package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class DirectionHud {
    public static final String ID = "direction";
    private static final String[] COMPASS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!HudStyle.shouldRenderHuds() || !NebulaConfig.get().isHudEnabled(ID)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int[] pos = NebulaConfig.get().getHudPosition(ID, 6, 34);
        int index = Math.round(client.player.getYaw() / 45f) & 7;
        HudStyle.beginScale(ctx, pos[0], pos[1], NebulaConfig.get().getHudScale(ID));
        ctx.drawTextWithShadow(client.textRenderer, Text.literal("Facing: " + COMPASS[index]).formatted(Formatting.BOLD), 0, 0, NebulaConfig.get().getHudColor(ID, HudStyle.TEXT));
        HudStyle.endScale(ctx);
    }
}
