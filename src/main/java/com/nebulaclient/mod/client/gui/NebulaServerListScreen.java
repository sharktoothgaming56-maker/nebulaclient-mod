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
        int third = (panelWidth() - 8) / 3;
        int fy = this.height - 54;
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), fy, third, 20, "Join", this::joinSelected));
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX() + third + 4, fy, third, 20, "Add",
                () -> this.client.setScreen(new AddServerScreen(this, Text.literal("Add Server"), this::onServerAdded,
                        new ServerInfo("Minecraft Server", "", ServerInfo.ServerType.OTHER)))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX() + (third + 4) * 2, fy, third, 20, "Direct",
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

    private void joinSelected() {
        if (selected == null) return;
        ConnectScreen.connect(this, this.client, ServerAddress.parse(selected.address), selected, false, null);
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

                ctx.drawTextWithShadow(this.textRenderer, Text.literal(server.name).formatted(Formatting.BOLD),
                        rowX + 4 + STAR_W + 4, drawY + 6, 0xFFEAE6F7);
                ctx.drawTextWithShadow(this.textRenderer, server.address,
                        rowX + 4 + STAR_W + 4, drawY + 18, 0xFFA79FC4);

                if (pressedNow && hovered) {
                    if (overStar) {
                        cfg.toggleFavoriteServer(server.address);
                    } else {
                        selected = server;
                        draggingServer = server;
                        dragStartY = mouseY;
                        dragCurrentY = mouseY;
                    }
                }
            }
            if (!isDragged) y += ROW_H;
        }

        // Finish a drag: work out the drop index from the pointer position.
        if (releasedNow) {
            if (dragActive && draggingServer != null) {
                int dropIndex = Math.max(0, (dragCurrentY - (top - scroll)) / ROW_H);
                ServerInfo dropAfter = null;
                if (dropIndex < visible.size()) dropAfter = visible.get(Math.min(dropIndex, visible.size() - 1));
                int fullIndex = dropAfter == null ? Integer.MAX_VALUE : ServerOrdering.sorted(allServers()).indexOf(dropAfter);
                if (fullIndex == Integer.MAX_VALUE) fullIndex = allServers().size() - 1;
                ServerOrdering.applyDrag(allServers(), draggingServer, fullIndex);
            }
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
