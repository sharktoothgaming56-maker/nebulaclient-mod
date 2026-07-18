package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.NebulaClientModClient;
import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.waypoint.Waypoint;
import com.nebulaclient.mod.client.waypoint.WaypointColors;
import com.nebulaclient.mod.client.waypoint.WaypointManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.Comparator;
import java.util.List;

/**
 * Deliberately a 2D compass-style HUD (arrow + distance) rather than 3D
 * beams rendered in the world — that needs vertex-buffer/render-layer/
 * camera-transform APIs I could only partially verify, and I don't want
 * to gamble a crash on the riskiest possible piece. This is the
 * interim/practical version; true 3D markers (circle + beam) are a
 * planned follow-up once done as its own careful, isolated change.
 */
public class WaypointCompassHud {
    public static final String ID = "waypoint_compass";
    private static final int MAX_SHOWN = 4;

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!HudStyle.shouldRenderHuds() || !NebulaConfig.get().isHudEnabled(ID)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (!NebulaClientModClient.waypointsVisible) return;

        String currentDim = client.world.getRegistryKey().getValue().toString();
        List<Waypoint> nearby = WaypointManager.all().stream()
                .filter(w -> w.dimension.equals(currentDim))
                .sorted(Comparator.comparingDouble(w -> WaypointManager.distanceTo(client, w)))
                .limit(MAX_SHOWN)
                .toList();
        if (nearby.isEmpty()) return;

        int[] pos = NebulaConfig.get().getHudPosition(ID, 6, 40);
        int y = pos[1];
        for (Waypoint wp : nearby) {
            float bearing = WaypointManager.bearingTo(client, wp);
            String arrow = arrowFor(bearing);
            double distance = WaypointManager.distanceTo(client, wp);
            String line = arrow + " " + wp.name + " (" + Math.round(distance) + "m)";
            ctx.drawTextWithShadow(client.textRenderer, line, pos[0], y, WaypointColors.argb(wp.color));
            y += 10;
        }
    }

    private static String arrowFor(float bearingDegrees) {
        String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        int index = Math.round(bearingDegrees / 45f);
        index = ((index % 8) + 8) % 8;
        return arrows[index];
    }
}
