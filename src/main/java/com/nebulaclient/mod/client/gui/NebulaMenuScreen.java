package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.hud.HudInfo;
import com.nebulaclient.mod.client.hud.Waypoint3dHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NebulaMenuScreen extends SpaceTheme.SpaceScreen {
    private final Screen parent; // null when opened via keybind rather than the pause menu

    public NebulaMenuScreen(Screen parent) {
        super(Text.literal("Nebula Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nebulaButtons.clear();
        NebulaConfig cfg = NebulaConfig.get();
        int y = 46;

        for (HudInfo info : HudInfo.ALL) {
            y = addToggle(cfg, info.id, info.label, y);
        }
        y = addToggle(cfg, Waypoint3dHud.ID, "3D Waypoints", y);

        y += 8;
        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 - 100, y, 200, 22, "Edit HUD Positions",
                () -> this.client.setScreen(new HudEditorScreen())));
        y += 26;
        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 - 100, y, 200, 22, "Waypoints",
                () -> this.client.setScreen(new WaypointMenuScreen(this))));

        SpaceTheme.NebulaButton back = new SpaceTheme.NebulaButton(
                this.width / 2 - 100, this.height - 32, 200, 22,
                parent != null ? "Back" : "Close",
                () -> this.client.setScreen(parent));
        back.bold = true;
        nebulaButtons.add(back);
    }

    /** Adds an ON/OFF toggle whose label flips in place — no screen rebuild needed. */
    private int addToggle(NebulaConfig cfg, String hudId, String label, int y) {
        SpaceTheme.NebulaButton btn = new SpaceTheme.NebulaButton(
                this.width / 2 - 100, y, 200, 22,
                label + ": " + (cfg.isHudEnabled(hudId) ? "ON" : "OFF"), null);
        btn.setAction(() -> {
            boolean now = !cfg.isHudEnabled(hudId);
            cfg.setHudEnabled(hudId, now);
            btn.setLabel(label + ": " + (now ? "ON" : "OFF"));
        });
        nebulaButtons.add(btn);
        return y + 26;
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("NEBULA MENU").formatted(Formatting.BOLD), this.width / 2, 20, 0xFFB98BFF);
    }

    @Override
    public boolean shouldPause() {
        return parent == null;
    }
}
