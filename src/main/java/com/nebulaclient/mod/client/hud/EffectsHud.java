package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.List;

public class EffectsHud {
    public static final String ID = "effects";

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!HudStyle.shouldRenderHuds() || !NebulaConfig.get().isHudEnabled(ID)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;

        List<StatusEffectInstance> effects = player.getStatusEffects().stream().toList();
        if (effects.isEmpty()) return;

        int[] pos = NebulaConfig.get().getHudPosition(ID, 6, client.getWindow().getScaledHeight() / 2 - (effects.size() * 10) / 2);
        int y = pos[1];
        for (StatusEffectInstance effect : effects) {
            String name = effect.getEffectType().value().getName().getString();
            String amplifier = effect.getAmplifier() > 0 ? " " + toRoman(effect.getAmplifier() + 1) : "";
            String time = formatDuration(effect.getDuration());
            ctx.drawTextWithShadow(client.textRenderer, name + amplifier + " " + time, pos[0], y, HudStyle.TEXT);
            y += 10;
        }
    }

    private static String formatDuration(int ticks) {
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private static String toRoman(int n) {
        String[] romans = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= 10 ? romans[n - 1] : String.valueOf(n);
    }
}
