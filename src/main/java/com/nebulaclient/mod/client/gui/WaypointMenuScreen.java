package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.waypoint.Waypoint;
import com.nebulaclient.mod.client.waypoint.WaypointColors;
import com.nebulaclient.mod.client.waypoint.WaypointManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class WaypointMenuScreen extends Screen {
    private final Screen parent;

    public WaypointMenuScreen(Screen parent) {
        super(Text.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        NebulaConfig cfg = NebulaConfig.get();

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Death Waypoints: " + (cfg.deathWaypointsEnabled ? "ON" : "OFF")),
                b -> {
                    cfg.deathWaypointsEnabled = !cfg.deathWaypointsEnabled;
                    cfg.save();
                    this.client.setScreen(new WaypointMenuScreen(parent)); // refresh label
                }
        ).dimensions(this.width / 2 - 100, 30, 200, 20).build());

        List<Waypoint> waypoints = WaypointManager.all();
        int y = 60;
        for (Waypoint wp : waypoints) {
            String label = (wp.isDeath ? "☠ " : "") + wp.name + String.format(" (%.0f, %.0f, %.0f)", wp.x, wp.y, wp.z);
            this.addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {})
                    .dimensions(this.width / 2 - 150, y, 210, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal(wp.color), b -> {
                wp.color = WaypointColors.next(wp.color);
                NebulaConfig.get().save();
                this.client.setScreen(new WaypointMenuScreen(parent)); // refresh label
            }).dimensions(this.width / 2 + 62, y, 52, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> {
                WaypointManager.delete(wp);
                this.client.setScreen(new WaypointMenuScreen(parent));
            }).dimensions(this.width / 2 + 116, y, 40, 20).build());
            y += 24;
            if (y > this.height - 40) break; // simple cutoff rather than a scroll list, keeps this low-risk
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> this.client.setScreen(parent))
                .dimensions(this.width / 2 - 100, this.height - 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0xE0241033, 0xF008050F);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Waypoints", this.width / 2, 12, 0xFFB98BFF);
        if (WaypointManager.all().isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer, "No waypoints yet — use Create Waypoint in-game.", this.width / 2, 60, 0xFFAAAAAA);
        }
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
