package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds two things to the vanilla Multiplayer screen, purely additively (no
 * Mixins, nothing vanilla is removed or re-routed):
 *
 *  1. FAVOURITES. Every server row gets a star in its bottom-right corner;
 *     click it to star/unstar (stored in nebulaclient.json). A button in
 *     the top-left toggles between "All Servers" (the untouched vanilla
 *     list) and a "Favorites" view. The Favorites view is drawn as an
 *     overlay panel ON TOP of the vanilla list widget: the widget itself
 *     is never filtered or mutated, so toggling back always restores the
 *     exact vanilla list, pings and all. Clicking a favourite connects to
 *     it; clicking its star unstars it.
 *
 *  2. DRAG TO REORDER (All Servers view). Press a row and drag vertically
 *     to move that server up/down the list; the order is written through
 *     ServerList.swapEntries — the same call the vanilla move arrows use —
 *     so it persists to servers.dat. The click zones vanilla already owns
 *     (the join arrow over the icon and the tiny move-up/move-down arrows,
 *     relative x < 32) are left completely alone: pressing there does
 *     exactly what it did before, and drags only arm outside those zones
 *     and outside the star.
 *
 * Click interception uses Fabric's allow-events for the star and the
 * Favorites panel (so a star click can't double-fire vanilla's
 * double-click-to-join), while drag tracking uses the same GLFW
 * edge-polling technique the rest of NebulaClient's screens run on.
 */
public final class ServerListEnhancer {
    /** Which category is showing. Session-wide so it survives screen re-inits. */
    private static boolean favoritesView = false;

    // --- geometry constants ---
    private static final int STAR_BOX = 15;    // clickable star square, bottom-right of each row
    private static final int JOIN_ZONE = 32;   // vanilla's join + move-arrow strip (relative x), never touched
    private static final int DRAG_THRESHOLD = 4;
    private static final int ROW_STRIDE = 36;  // vanilla server list item height
    private static final int FAV_ROW_H = 26;

    private final MinecraftClient client;
    private final MultiplayerScreen screen;
    private MultiplayerServerListWidget widget;

    // Frame-tracked mouse (screen coords) so click events can hit-test
    // without depending on unmapped accessors of the new Click object.
    private int lastMouseX = -1, lastMouseY = -1;

    // GLFW edge tracking (starts true so a held-over click can't insta-fire).
    private boolean wasDown = true;

    // Drag state (All Servers view only).
    private MultiplayerServerListWidget.ServerEntry pressedEntry;
    private int pressY;
    private boolean dragging;
    private boolean movedAny;

    // Favorites panel scroll offset.
    private int favScroll = 0;

    private ServerListEnhancer(MinecraftClient client, MultiplayerScreen screen) {
        this.client = client;
        this.screen = screen;
    }

    /** Hook everything onto a freshly-initialised Multiplayer screen. */
    public static void attach(MinecraftClient client, MultiplayerScreen screen) {
        ServerListEnhancer e = new ServerListEnhancer(client, screen);
        e.widget = e.findWidget();

        // Category toggle, top-left, using the same themed vanilla button
        // texture as every other button. Label shows the CURRENT category.
        ButtonWidget toggle = ButtonWidget.builder(e.toggleLabel(), b -> {
            favoritesView = !favoritesView;
            b.setMessage(e.toggleLabel());
            e.favScroll = 0;
            e.cancelDrag();
            if (favoritesView && e.widget != null) {
                e.widget.setSelected(null); // nothing hidden stays selected/joinable
            }
        }).dimensions(6, 6, 115, 20).build();
        Screens.getButtons(screen).add(toggle);

        ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, delta) -> e.onFrame(ctx, mouseX, mouseY));

        ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> e.onAllowClick(click.button()));

        ScreenMouseEvents.allowMouseScroll(screen).register((s, mx, my, hAmt, vAmt) -> e.onAllowScroll(mx, my, vAmt));
    }

    private Text toggleLabel() {
        return favoritesView
                ? Text.literal("\u2605 Favorites")
                : Text.literal("All Servers");
    }

    private MultiplayerServerListWidget findWidget() {
        for (var el : screen.children()) {
            if (el instanceof MultiplayerServerListWidget w) return w;
        }
        return null;
    }

    /** Server entries in list order (they sit at the head of children()). */
    private List<MultiplayerServerListWidget.ServerEntry> serverRows() {
        List<MultiplayerServerListWidget.ServerEntry> rows = new ArrayList<>();
        if (widget == null) return rows;
        for (var entry : widget.children()) {
            if (entry instanceof MultiplayerServerListWidget.ServerEntry se) {
                rows.add(se);
            } else {
                break; // scanning/LAN entries follow the servers; stop at the first
            }
        }
        return rows;
    }

    private static String keyOf(ServerInfo info) {
        return NebulaConfig.serverFavoriteKey(info.name, info.address);
    }

    // =====================================================================
    // Per-frame: draw stars / favourites panel, and run the drag machine.
    // =====================================================================
    private void onFrame(DrawContext ctx, int mouseX, int mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (widget == null) widget = findWidget();
        if (widget == null) return;

        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean pressedEdge = down && !wasDown;
        boolean releasedEdge = !down && wasDown;
        wasDown = down;

        if (favoritesView) {
            cancelDrag();
            renderFavoritesPanel(ctx, mouseX, mouseY);
            return;
        }

        // ---------- All Servers view: stars + drag ----------
        int wx = widget.getX(), wy = widget.getY(), ww = widget.getWidth(), wh = widget.getHeight();
        List<MultiplayerServerListWidget.ServerEntry> rows = serverRows();
        NebulaConfig cfg = NebulaConfig.get();

        ctx.enableScissor(wx, wy, wx + ww, wy + wh);
        for (MultiplayerServerListWidget.ServerEntry row : rows) {
            int x = row.getContentX(), y = row.getContentY();
            int w = row.getContentWidth(), h = row.getContentHeight();
            if (y + h < wy || y > wy + wh) continue; // fully scrolled out

            boolean fav = cfg.isFavoriteServer(keyOf(row.getServer()));
            int sx = starX(row), sy = starY(row);
            boolean hover = mouseX >= sx && mouseX < sx + STAR_BOX && mouseY >= sy && mouseY < sy + STAR_BOX;
            if (hover) ctx.fill(sx, sy, sx + STAR_BOX, sy + STAR_BOX, 0x28FFFFFF);
            Text glyph = Text.literal(fav ? "\u2605" : "\u2606");
            int color = fav ? 0xFFFFD75E : (hover ? 0xFFEAE6F7 : 0x90C9C2DE);
            ctx.drawTextWithShadow(client.textRenderer, glyph,
                    sx + (STAR_BOX - client.textRenderer.getWidth(glyph)) / 2, sy + 3, color);
        }

        // Drag highlight, drawn over the row being carried.
        if (dragging && pressedEntry != null) {
            int x = pressedEntry.getContentX(), y = pressedEntry.getContentY();
            int w = pressedEntry.getContentWidth(), h = pressedEntry.getContentHeight();
            int accent = 0xFF000000 | (cfg.getStrokeColor() & 0xFFFFFF);
            ctx.fill(x - 2, y - 2, x + w + 2, y - 1, accent);
            ctx.fill(x - 2, y + h + 1, x + w + 2, y + h + 2, accent);
            ctx.fill(x - 2, y - 2, x - 1, y + h + 2, accent);
            ctx.fill(x + w + 1, y - 2, x + w + 2, y + h + 2, accent);
            ctx.fill(x, y, x + w, y + h, 0x14FFFFFF);
        }
        ctx.disableScissor();

        // ---------- drag state machine (GLFW edge polling) ----------
        boolean mouseInList = mouseX >= wx && mouseX < wx + ww && mouseY >= wy && mouseY < wy + wh;

        if (pressedEdge && mouseInList) {
            for (MultiplayerServerListWidget.ServerEntry row : rows) {
                int x = row.getContentX(), y = row.getContentY();
                int w = row.getContentWidth(), h = row.getContentHeight();
                boolean inRow = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
                if (!inRow) continue;
                int relX = mouseX - x;
                boolean inStar = mouseX >= starX(row) && mouseX < starX(row) + STAR_BOX
                        && mouseY >= starY(row) && mouseY < starY(row) + STAR_BOX;
                // Leave vanilla's join + move-arrow strip and the star alone.
                if (relX >= JOIN_ZONE && !inStar) {
                    pressedEntry = row;
                    pressY = mouseY;
                    dragging = false;
                    movedAny = false;
                }
                break;
            }
        }

        if (down && pressedEntry != null && !dragging && Math.abs(mouseY - pressY) > DRAG_THRESHOLD) {
            dragging = true;
            widget.setSelected(pressedEntry); // vanilla highlight follows the carried row
        }

        if (dragging && pressedEntry != null) {
            List<MultiplayerServerListWidget.ServerEntry> live = serverRows();
            int cur = live.indexOf(pressedEntry);
            if (cur < 0) {
                cancelDrag(); // list was rebuilt (refresh/LAN tick) mid-drag
            } else {
                int target = slotUnderMouse(live, mouseY);
                ServerList list = screen.getServerList();
                // Walk one swap at a time so widget + saved list move in
                // lockstep; guard against any size mismatch just in case.
                while (cur != target && target >= 0 && target < live.size()
                        && cur < list.size() && target < list.size()) {
                    int next = cur + (target > cur ? 1 : -1);
                    list.swapEntries(cur, next);                 // persists order (same call the arrows use)
                    widget.swapEntriesOnPositions(cur, next);    // moves the rows on screen
                    movedAny = true;
                    cur = next;
                }
            }
        }

        if (releasedEdge) {
            if (movedAny) {
                try {
                    screen.getServerList().saveFile(); // belt-and-braces persist
                } catch (Exception ignored) {}
            }
            cancelDrag();
        }
    }

    private int starX(MultiplayerServerListWidget.ServerEntry row) {
        return row.getContentX() + row.getContentWidth() - STAR_BOX - 2;
    }

    private int starY(MultiplayerServerListWidget.ServerEntry row) {
        return row.getContentY() + row.getContentHeight() - STAR_BOX - 1;
    }

    /** Which slot index the mouse is over, clamped to the ends of the list. */
    private int slotUnderMouse(List<MultiplayerServerListWidget.ServerEntry> rows, int mouseY) {
        if (rows.isEmpty()) return -1;
        if (mouseY < rows.get(0).getContentY()) return 0;
        for (int i = 0; i < rows.size(); i++) {
            int top = rows.get(i).getContentY();
            if (mouseY >= top && mouseY < top + ROW_STRIDE) return i;
        }
        return rows.size() - 1;
    }

    private void cancelDrag() {
        pressedEntry = null;
        dragging = false;
        movedAny = false;
    }

    // =====================================================================
    // Favourites overlay panel (vanilla widget stays untouched underneath).
    // =====================================================================
    private record FavRow(ServerInfo info, int index) {}

    private List<FavRow> favRows() {
        List<FavRow> rows = new ArrayList<>();
        NebulaConfig cfg = NebulaConfig.get();
        ServerList list = screen.getServerList();
        for (int i = 0; i < list.size(); i++) {
            ServerInfo info = list.get(i);
            if (cfg.isFavoriteServer(keyOf(info))) rows.add(new FavRow(info, i));
        }
        return rows;
    }

    private void renderFavoritesPanel(DrawContext ctx, int mouseX, int mouseY) {
        int wx = widget.getX(), wy = widget.getY(), ww = widget.getWidth(), wh = widget.getHeight();
        NebulaConfig cfg = NebulaConfig.get();
        int accent = 0xFF000000 | (cfg.getStrokeColor() & 0xFFFFFF);

        // Opaque space-tinted panel fully covering the vanilla list.
        ctx.fillGradient(wx, wy, wx + ww, wy + wh, 0xFF160D26, 0xFF090512);
        ctx.fill(wx, wy, wx + ww, wy + 1, accent);
        ctx.fill(wx, wy + wh - 1, wx + ww, wy + wh, accent);

        int cx = wx + ww / 2;
        ctx.drawCenteredTextWithShadow(client.textRenderer,
                Text.literal("\u2605 FAVORITES").formatted(Formatting.BOLD), cx, wy + 6, 0xFFFFD75E);

        List<FavRow> rows = favRows();
        if (rows.isEmpty()) {
            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    "No favorite servers yet", cx, wy + wh / 2 - 10, 0xFFEAE6F7);
            ctx.drawCenteredTextWithShadow(client.textRenderer,
                    "Switch to All Servers and click the \u2606 on a server", cx, wy + wh / 2 + 4, 0xFFA79FC4);
            return;
        }

        int rowW = Math.min(340, ww - 40);
        int rx = cx - rowW / 2;
        int top = wy + 20;
        int viewH = wh - 26;
        int contentH = rows.size() * FAV_ROW_H;
        int maxScroll = Math.max(0, contentH - viewH);
        favScroll = Math.max(0, Math.min(favScroll, maxScroll));

        ctx.enableScissor(wx, top, wx + ww, wy + wh - 4);
        for (int i = 0; i < rows.size(); i++) {
            FavRow row = rows.get(i);
            int ry = top + i * FAV_ROW_H - favScroll;
            if (ry + FAV_ROW_H < top || ry > wy + wh) continue;

            boolean hoverRow = mouseX >= rx && mouseX < rx + rowW && mouseY >= ry && mouseY < ry + FAV_ROW_H - 2;
            ctx.fill(rx, ry, rx + rowW, ry + FAV_ROW_H - 2, hoverRow ? 0x33B98BFF : 0x1AFFFFFF);
            if (hoverRow) {
                ctx.fill(rx, ry, rx + rowW, ry + 1, accent);
                ctx.fill(rx, ry + FAV_ROW_H - 3, rx + rowW, ry + FAV_ROW_H - 2, accent);
            }

            ctx.drawTextWithShadow(client.textRenderer,
                    Text.literal(row.info().name).formatted(Formatting.BOLD), rx + 6, ry + 4, 0xFFFFFFFF);
            ctx.drawTextWithShadow(client.textRenderer,
                    row.info().address, rx + 6, ry + 14, 0xFF9A93B8);

            int sx = rx + rowW - STAR_BOX - 4;
            int sy = ry + (FAV_ROW_H - 2 - STAR_BOX) / 2;
            boolean hoverStar = mouseX >= sx && mouseX < sx + STAR_BOX && mouseY >= sy && mouseY < sy + STAR_BOX;
            if (hoverStar) ctx.fill(sx, sy, sx + STAR_BOX, sy + STAR_BOX, 0x28FFFFFF);
            Text glyph = Text.literal("\u2605");
            ctx.drawTextWithShadow(client.textRenderer, glyph,
                    sx + (STAR_BOX - client.textRenderer.getWidth(glyph)) / 2, sy + 3, 0xFFFFD75E);

            if (hoverRow && !hoverStar) {
                Text join = Text.literal("Join \u25B6");
                ctx.drawTextWithShadow(client.textRenderer, join,
                        sx - client.textRenderer.getWidth(join) - 8, ry + 9, 0xFFB98BFF);
            }
        }
        ctx.disableScissor();

        if (maxScroll > 0) {
            int trackX = rx + rowW + 6;
            int barH = Math.max(16, viewH * viewH / contentH);
            int barY = top + (viewH - barH) * favScroll / maxScroll;
            ctx.fill(trackX, top, trackX + 3, top + viewH, 0x30FFFFFF);
            ctx.fill(trackX, barY, trackX + 3, barY + barH, accent);
        }
    }

    // =====================================================================
    // Click / scroll interception
    // =====================================================================
    /** Return false to consume the click before vanilla sees it. */
    private boolean onAllowClick(int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        if (widget == null || lastMouseX < 0) return true;
        int mx = lastMouseX, my = lastMouseY;
        int wx = widget.getX(), wy = widget.getY(), ww = widget.getWidth(), wh = widget.getHeight();
        boolean inWidget = mx >= wx && mx < wx + ww && my >= wy && my < wy + wh;

        if (favoritesView) {
            if (!inWidget) return true;
            // Everything over the panel is ours; the hidden vanilla list
            // (its entries, arrows and scrollbar) must not react.
            List<FavRow> rows = favRows();
            int rowW = Math.min(340, ww - 40);
            int rx = wx + ww / 2 - rowW / 2;
            int top = wy + 20;
            for (int i = 0; i < rows.size(); i++) {
                int ry = top + i * FAV_ROW_H - favScroll;
                if (ry + FAV_ROW_H < top || ry > wy + wh) continue;
                boolean inRow = mx >= rx && mx < rx + rowW && my >= ry && my < ry + FAV_ROW_H - 2;
                if (!inRow) continue;
                int sx = rx + rowW - STAR_BOX - 4;
                int sy = ry + (FAV_ROW_H - 2 - STAR_BOX) / 2;
                boolean inStar = mx >= sx && mx < sx + STAR_BOX && my >= sy && my < sy + STAR_BOX;
                if (inStar) {
                    NebulaConfig.get().toggleFavoriteServer(keyOf(rows.get(i).info()));
                } else {
                    screen.connect(rows.get(i).info());
                }
                return false;
            }
            return false; // clicked panel background — still consume
        }

        // All Servers view: only the star squares are intercepted, so a
        // star click can't count toward vanilla's double-click-to-join.
        for (MultiplayerServerListWidget.ServerEntry row : serverRows()) {
            int sx = starX(row), sy = starY(row);
            if (mx >= sx && mx < sx + STAR_BOX && my >= sy && my < sy + STAR_BOX
                    && my >= wy && my < wy + wh) {
                NebulaConfig.get().toggleFavoriteServer(keyOf(row.getServer()));
                return false;
            }
        }
        return true;
    }

    /** Consume wheel over the favourites panel so the hidden list stays put. */
    private boolean onAllowScroll(double mouseX, double mouseY, double vAmount) {
        if (!favoritesView || widget == null) return true;
        int wx = widget.getX(), wy = widget.getY(), ww = widget.getWidth(), wh = widget.getHeight();
        if (mouseX < wx || mouseX >= wx + ww || mouseY < wy || mouseY >= wy + wh) return true;
        int maxScroll = Math.max(0, favRows().size() * FAV_ROW_H - (wh - 26));
        favScroll = Math.max(0, Math.min(maxScroll, favScroll - (int) (vAmount * 14)));
        return false;
    }
}
