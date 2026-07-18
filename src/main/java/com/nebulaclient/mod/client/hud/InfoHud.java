package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class InfoHud {
    public static final String ID = "info";
    private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!HudStyle.shouldRenderHuds() || !NebulaConfig.get().isHudEnabled(ID)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;

        String fps = "FPS: " + client.getCurrentFps();
        String coords = String.format("XYZ: %.0f, %.0f, %.0f", player.getX(), player.getY(), player.getZ());
        String facing = "Facing: " + compassDirection(player.getYaw());

        int[] pos = NebulaConfig.get().getHudPosition(ID, 6, 6);
        ctx.drawTextWithShadow(client.textRenderer, fps, pos[0], pos[1], HudStyle.TEXT);
        ctx.drawTextWithShadow(client.textRenderer, coords, pos[0], pos[1] + 10, HudStyle.TEXT);
        ctx.drawTextWithShadow(client.textRenderer, facing, pos[0], pos[1] + 20, HudStyle.TEXT);
    }

    private static String compassDirection(float yaw) {
        int index = Math.round(yaw / 45f) & 7;
        return COMPASS[index];
    }
}
