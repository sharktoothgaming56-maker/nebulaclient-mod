package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows what the player is currently doing — flying, sneaking, sprinting.
 * Plain floating text with a shadow, no background panel.
 *
 * ROOT CAUSE of the earlier backwards/broken labels: KeyBinding.isPressed()
 * returns true for a toggled-ON key even when nothing is physically held —
 * that's literally how toggle mode works internally — so it can never
 * distinguish held from toggled. The real differentiator is the player's
 * accessibility SETTING: if sneak/sprint is set to toggle mode, an active
 * state is "(toggled)"; in hold mode it's "(held)".
 */
public class StatusHud {
    public static final String ID = "status";

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!HudStyle.shouldRenderHuds() || !NebulaConfig.get().isHudEnabled(ID)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;

        // NOTE: if these two getters don't compile, search GameOptions for
        // "Toggled" — the sneak/sprint toggle-mode options exist, only the
        // getter naming has shifted between mappings versions.
        boolean sneakToggleMode = client.options.getSneakToggled().getValue();
        boolean sprintToggleMode = client.options.getSprintToggled().getValue();

        List<String> lines = new ArrayList<>();
        if (player.getAbilities().flying) lines.add("Flying (toggled)");
        if (player.isSneaking()) lines.add("Sneaking" + (sneakToggleMode ? " (toggled)" : " (held)"));
        if (player.isSprinting()) lines.add("Sprinting" + (sprintToggleMode ? " (toggled)" : " (held)"));
        if (lines.isEmpty()) return;

        int width = 120;
        int height = lines.size() * 10;
        int[] pos = NebulaConfig.get().getHudPosition(ID, client.getWindow().getScaledWidth() - width - 6, 6);

        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(client.textRenderer, lines.get(i), pos[0] + width - client.textRenderer.getWidth(lines.get(i)), pos[1] + i * 10, HudStyle.TEXT);
        }
    }
}
