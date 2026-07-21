package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.server.ServerOrdering;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList; // client.option, not client.network — verified against the 1.21.11 Yarn javadoc
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * NebulaClient's server list.
 *
 * What it adds over vanilla:
 *  - a star on every row; click it to favourite, and favourites sort to
 *    the top automatically
 *  - an All / Favourites filter tab, remembered between launches
 *  - click-and-drag a row to reorder the list by hand; the order is saved
 *  - no vanilla up/down arrows (that's what the transparent move_up /
 *    move_down sprite overrides in resources are for)
 *
 * Input follows the same split the rest of this codebase settled on:
 * clicks and drags via raw GLFW polling (this version's Click-object
 * Screen API broke the classic overrides), and mouseScrolled as a normal
 * override since that one still works.
 */
public class NebulaServerListScreen extends SpaceTheme.SpaceScreen {
    private final Screen parent;
    private ServerList serverList;
    private int scroll = 0;
    private ServerInfo selected = null;

    // Drag state
    private ServerInfo draggingServer = null;
    private int dragStartY = 0;
    private int dragCurrentY = 0;
    private boolean dragActive = false; // only true once the pointer has actually moved
    private boolean wasMouseDown = true; // start true so a click carried in from the last screen can't grab a row
    private ServerInfo clickedRowServer = null; // set on press, becomes a join on release unless it turned into a drag

    private static final int ROW_H = 32;
    private static final int STAR_W = 20;
    private static final int DRAG_THRESHOLD = 4; // px before a click becomes a drag

    public NebulaServerListScreen(Screen parent) {
        super(Text.literal("Servers"));
        this.parent = parent;
    }

    private int listTop() { return 74; }
    private int listBottom() { return this.height - 62; }

    @Override
    protected void init() {
        nebulaButtons.clear();
        if (serverList == null) {
            serverList = new ServerList(this.client);
            serverList.loadFile();
        }

        NebulaConfig cfg = NebulaConfig.get();

        // Filter tabs.
        int tabW = (panelWidth() - 4) / 2;
        SpaceTheme.NebulaButton allTab = new SpaceTheme.NebulaButton(panelX(), 46, tabW, 20, "All", null);
        allTab.setAction(() -> {
            cfg.showFavoritesOnly = false;
            cfg.save();
            this.client.setScreen(new NebulaServerListScreen(parent));
        });
        allTab.bold = !cfg.showFavoritesOnly;
        nebulaButtons.add(allTab);

        SpaceTheme.NebulaButton favTab = new SpaceTheme.NebulaButton(panelX() + tabW + 4, 46, tabW, 20, "★ Favourites", null);
        favTab.setAction(() -> {
            cfg.showFavoritesOnly = true;
            cfg.save();
            this.client.setScreen(new NebulaServerListScreen(parent));
        });
        favTab.bold = cfg.showFavoritesOnly;
        nebulaButtons.add(favTab);

        // Footer actions.
        // No Join button — clicking a row joins it directly.
        int half = (panelWidth() - 4) / 2;
        int fy = this.height - 54;
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), fy, half, 20, "Add",
                () -> this.client.setScreen(new AddServerScreen(this, Text.literal("Add Server"), this::onServerAdded,
                        new ServerInfo("Minecraft Server", "", ServerInfo.ServerType.OTHER)))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX() + half + 4, fy, half, 20, "Direct",
                () -> this.client.setScreen(new DirectConnectScreen(this, this::onDirectConnect, new ServerInfo("Minecraft Server", "", ServerInfo.ServerType.OTHER)))));

        SpaceTheme.NebulaButton back = new SpaceTheme.NebulaButton(panelX(), this.height - 30, panelWidth(), 20, "Back",
                () -> this.client.setScreen(parent));
        back.bold = true;
        nebulaButtons.add(back);
    }

    private void onServerAdded(boolean confirmed) {
        if (confirmed) {
            // The AddServerScreen callback hands back confirmation; vanilla's
            // own list file is the source of truth, so just reload it.
            serverList.loadFile();
        }
        this.client.setScreen(this);
    }

    private void onDirectConnect(boolean confirmed) {
        this.client.setScreen(this);
    }

    private List<ServerInfo> allServers() {
        List<ServerInfo> out = new ArrayList<>();
        for (int i = 0; i < serverList.size(); i++) out.add(serverList.get(i));
        return out;
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NebulaConfig cfg = NebulaConfig.get();
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("SERVERS").formatted(Formatting.BOLD), this.width / 2, 22, cfg.getButtonColor());

        List<ServerInfo> visible = ServerOrdering.visible(allServers(), cfg.showFavoritesOnly);

        boolean mouseDown = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean pressedNow = mouseDown && !wasMouseDown;
        boolean releasedNow = !mouseDown && wasMouseDown;
        wasMouseDown = mouseDown;

        int top = listTop();
        int bottom = listBottom();
        int rowX = panelX();
        int rowW = panelWidth();

        if (visible.isEmpty()) {
            String msg = cfg.showFavoritesOnly ? "No favourite servers yet — star one in All." : "No servers yet — use Add below.";
            ctx.drawCenteredTextWithShadow(this.textRenderer, msg, this.width / 2, top + 20, 0xFFA79FC4);
        }

        // Track a drag once the pointer has moved past the threshold.
        if (draggingServer != null && mouseDown) {
            dragCurrentY = mouseY;
            if (Math.abs(dragCurrentY - dragStartY) > DRAG_THRESHOLD) dragActive = true;
        }

        int y = top - scroll;
        for (int i = 0; i < visible.size(); i++) {
            ServerInfo server = visible.get(i);
            boolean isDragged = dragActive && server == draggingServer;
            int drawY = isDragged ? dragCurrentY - ROW_H / 2 : y;

            if (drawY + ROW_H >= top && drawY <= bottom) {
                boolean hovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= drawY && mouseY <= drawY + ROW_H && !dragActive;
                boolean isSelected = server == selected;

                int bg = isDragged ? 0xD0432a6e : (isSelected ? 0xB0361f5c : (hovered ? 0x902a1845 : 0x70180d2b));
                ctx.fill(rowX, drawY, rowX + rowW, drawY + ROW_H - 2, bg);
                if (isSelected || isDragged) {
                    int border = cfg.getButtonColor();
                    ctx.fill(rowX, drawY, rowX + rowW, drawY + 1, border);
                    ctx.fill(rowX, drawY + ROW_H - 3, rowX + rowW, drawY + ROW_H - 2, border);
                    ctx.fill(rowX, drawY, rowX + 1, drawY + ROW_H - 2, border);
                    ctx.fill(rowX + rowW - 1, drawY, rowX + rowW, drawY + ROW_H - 2, border);
                }

                // Star: filled gold when favourited, hollow grey otherwise.
                boolean fav = cfg.isFavoriteServer(server.address);
                boolean overStar = mouseX >= rowX + 4 && mouseX <= rowX + 4 + STAR_W && mouseY >= drawY && mouseY <= drawY + ROW_H;
                int starColor = fav ? 0xFFFFC85E : (overStar ? 0xFFEAE6F7 : 0xFF6F6690);
                ctx.drawTextWithShadow(this.textRenderer, Text.literal(fav ? "★" : "☆").formatted(Formatting.BOLD),
                        rowX + 7, drawY + ROW_H / 2 - 6, starColor);

                // Server icon slot. NOTE: ServerInfo.getFavicon() hands back
                // raw PNG bytes, not a drawable texture — turning those into
                // one needs NativeImage decoding + per-server texture
                // registration and cleanup, which I couldn't verify well
                // enough to ship blind. For now this draws a placeholder tile
                // in the exact spot the real icon belongs, so wiring it up
                // later is a drop-in change rather than a re-layout.
                int iconSize = ROW_H - 8;
                int iconX = rowX + 4 + STAR_W + 2;
                int iconY = drawY + 4;
                ctx.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0xFF2a1845);
                ctx.drawCenteredTextWithShadow(this.textRenderer,
                        server.name.isEmpty() ? "?" : server.name.substring(0, 1).toUpperCase(),
                        iconX + iconSize / 2, iconY + iconSize / 2 - 4, 0xFFB98BFF);

                int textX = iconX + iconSize + 6;
                ctx.drawTextWithShadow(this.textRenderer, Text.literal(server.name).formatted(Formatting.BOLD),
                        textX, drawY + 6, 0xFFEAE6F7);
                ctx.drawTextWithShadow(this.textRenderer, server.address,
                        textX, drawY + 18, 0xFFA79FC4);

                if (hovered && !overStar) {
                    ctx.drawTextWithShadow(this.textRenderer, "click to join",
                            rowX + rowW - this.textRenderer.getWidth("click to join") - 6, drawY + 12, 0xFF6F6690);
                }

                if (pressedNow && hovered) {
                    if (overStar) {
                        cfg.toggleFavoriteServer(server.address);
                    } else {
                        selected = server;
                        draggingServer = server;
                        dragStartY = mouseY;
                        dragCurrentY = mouseY;
                        clickedRowServer = server; // may become a join on release, if it wasn't a drag
                    }
                }
            }
            if (!isDragged) y += ROW_H;
        }

        // Finish the interaction: a drag reorders, a plain click joins.
        if (releasedNow) {
            if (dragActive && draggingServer != null) {
                int dropIndex = Math.max(0, (dragCurrentY - (top - scroll)) / ROW_H);
                ServerInfo dropAfter = null;
                if (dropIndex < visible.size()) dropAfter = visible.get(Math.min(dropIndex, visible.size() - 1));
                int fullIndex = dropAfter == null ? Integer.MAX_VALUE : ServerOrdering.sorted(allServers()).indexOf(dropAfter);
                if (fullIndex == Integer.MAX_VALUE) fullIndex = allServers().size() - 1;
                ServerOrdering.applyDrag(allServers(), draggingServer, fullIndex);
            } else if (clickedRowServer != null) {
                // Never moved past the drag threshold, so treat it as a
                // click: join straight away instead of making the player
                // select the row and then press a separate Join button.
                ServerInfo toJoin = clickedRowServer;
                clickedRowServer = null;
                draggingServer = null;
                dragActive = false;
                ConnectScreen.connect(this, this.client, ServerAddress.parse(toJoin.address), toJoin, false, null);
                return;
            }
            clickedRowServer = null;
            draggingServer = null;
            dragActive = false;
        }

        // Scrollbar.
        int viewport = bottom - top;
        int contentH = visible.size() * ROW_H;
        if (contentH > viewport) {
            int maxScroll = contentH - viewport;
            scroll = Math.max(0, Math.min(scroll, maxScroll));
            int barH = Math.max(20, viewport * viewport / contentH);
            int barY = top + (maxScroll == 0 ? 0 : (viewport - barH) * scroll / maxScroll);
            ctx.fill(rowX + rowW + 4, top, rowX + rowW + 7, bottom, 0x40FFFFFF);
            ctx.fill(rowX + rowW + 4, barY, rowX + rowW + 7, barY + barH, cfg.getButtonColor());
        } else {
            scroll = 0;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        NebulaConfig cfg = NebulaConfig.get();
        int contentH = ServerOrdering.visible(allServers(), cfg.showFavoritesOnly).size() * ROW_H;
        int maxScroll = Math.max(0, contentH - (listBottom() - listTop()));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (verticalAmount * 18)));
        return true;
    }
}
