package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The small "NebulaClient" wordmark.
 *
 * Two jobs in one:
 *   1. During normal gameplay (no screen open) it shows as a proper HUD —
 *      togglable, movable and recolourable from the Nebula Menu / HUD
 *      editor like every other HUD, so the client's mark sits on your main
 *      screen (small, per the brief).
 *   2. While a NON-Nebula screen is open (inventory, vanilla menus, chat),
 *      it still draws in the bottom-right as the stand-in for the HUDs that
 *      hide themselves — so something's always there in their place.
 *
 * Our own menus draw their own wordmark, so it skips those to avoid drawing
 * twice.
 */
public class LogoHud {
    public static final String ID = "logo";
    private static final float SCALE = 1.0f; // small on purpose (was 1.8f)

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Text text = Text.literal("NEBULACLIENT").formatted(Formatting.BOLD);
        int color = 0xFF000000 | (NebulaConfig.get().getButtonColor() & 0xFFFFFF);
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        if (client.currentScreen == null) {
            // Gameplay HUD mode — respect the toggle, position and F3 rules
            // like the other HUDs. Default anchor: bottom-right corner.
            if (!HudStyle.shouldRenderHuds() || !NebulaConfig.get().isHudEnabled(ID)) return;
            float scale = NebulaConfig.get().getHudScale(ID);
            int defX = width - (int) (client.textRenderer.getWidth(text) * scale) - 6;
            int defY = height - (int) (10 * scale) - 4;
            int[] pos = NebulaConfig.get().getHudPosition(ID, defX, defY);
            int logoColor = NebulaConfig.get().getHudColor(ID, color);
            HudStyle.beginScale(ctx, pos[0], pos[1], scale);
            ctx.drawTextWithShadow(client.textRenderer, text, 0, 0, logoColor);
            HudStyle.endScale(ctx);
            return;
        }

        // Screen-open stand-in mode — skip our own menus (they draw it).
        if (client.currentScreen instanceof com.nebulaclient.mod.client.gui.SpaceTheme.SpaceScreen) return;
        int x = width - (int) (client.textRenderer.getWidth(text) * SCALE) - 6;
        int y = height - (int) (10 * SCALE) - 4;
        HudStyle.beginScale(ctx, x, y, SCALE);
        ctx.drawTextWithShadow(client.textRenderer, text, 0, 0, color);
        HudStyle.endScale(ctx);
    }
}
