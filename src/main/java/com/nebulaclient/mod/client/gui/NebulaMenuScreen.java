package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.hud.HudInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class NebulaMenuScreen extends Screen {
    private final Screen parent; // null when opened via keybind rather than the pause menu

    public NebulaMenuScreen(Screen parent) {
        super(Text.literal("Nebula Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        NebulaConfig cfg = NebulaConfig.get();
        int y = 34;
        for (HudInfo info : HudInfo.ALL) {
            boolean enabled = cfg.isHudEnabled(info.id);
            this.addDrawableChild(ButtonWidget.builder(
                    Text.literal(info.label + ": " + (enabled ? "ON" : "OFF")),
                    b -> {
                        cfg.setHudEnabled(info.id, !cfg.isHudEnabled(info.id));
                        this.client.setScreen(new NebulaMenuScreen(parent)); // refresh labels
                    }
            ).dimensions(this.width / 2 - 100, y, 200, 20).build());
            y += 24;
        }

        y += 6;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD Positions"), b -> this.client.setScreen(new HudEditorScreen()))
                .dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 24;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Waypoints"), b -> this.client.setScreen(new WaypointMenuScreen(this)))
                .dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 24;

        String closeLabel = parent != null ? "Back" : "Close";
        this.addDrawableChild(ButtonWidget.builder(Text.literal(closeLabel), b -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // NOT calling this.renderBackground(...) — that's what actually
        // crashed (confirmed by your crash report). fillGradient is a
        // long-stable DrawContext method, safe to use here, and gives a
        // space-themed purple-to-black backdrop instead of vanilla's blur.
        ctx.fillGradient(0, 0, this.width, this.height, 0xE0241033, 0xF008050F);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Nebula Menu", this.width / 2, 12, 0xFFB98BFF);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return parent == null; // pause when opened standalone in-world; the pause-menu variant is already paused
    }
}
