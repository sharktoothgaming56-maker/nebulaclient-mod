package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows what the player is currently doing — flying, sneaking, sprinting.
 * Plain floating text with a shadow, no background panel — matches the
 * "no boxes, like Lunar" feedback.
 *
 * NOTE on held-vs-toggled: this was flipped from my first attempt based
 * on direct in-game testing — actually holding the key now shows
 * "(held)" and not physically holding it (double-tap/toggle-mode) shows
 * "(toggled)", the opposite of what the isPressed()-based logic
 * originally produced. Trusting the tester's live result over my theory.
 */
public class StatusHud {
    public static final String ID = "status";

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!HudStyle.shouldRenderHuds() || !NebulaConfig.get().isHudEnabled(ID)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;

        List<String> lines = new ArrayList<>();
        if (player.getAbilities().flying) lines.add("Flying (toggled)");
        if (player.isSneaking()) lines.add("Sneaking" + (client.options.sneakKey.isPressed() ? " (toggled)" : " (held)"));
        if (player.isSprinting()) lines.add("Sprinting" + (client.options.sprintKey.isPressed() ? " (toggled)" : " (held)"));
        if (lines.isEmpty()) return;

        int width = 120;
        int height = lines.size() * 10;
        int[] pos = NebulaConfig.get().getHudPosition(ID, client.getWindow().getScaledWidth() - width - 6, 6);

        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(client.textRenderer, lines.get(i), pos[0] + width - client.textRenderer.getWidth(lines.get(i)), pos[1] + i * 10, HudStyle.TEXT);
        }
    }
}
