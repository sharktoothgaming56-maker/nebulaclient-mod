package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The small "NebulaClient" wordmark, now a normal HUD like the others:
 * movable, scalable and colourable from the HUD editor (default anchor is
 * bottom-left). It renders during gameplay and also stays put while a
 * non-Nebula screen is open, EXCEPT:
 *   - it hides while F3 is up (keeps the debug overlay clean),
 *   - it hides while the chat/command box is open, so it never overlaps
 *     what you're typing (reappears when you close chat),
 *   - our own space menus draw their own wordmark, so it skips those.
 */
public class LogoHud {
    public static final String ID = "logo";

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!NebulaConfig.get().isHudEnabled(ID)) return;

        // Hide while typing in chat/commands.
        if (client.currentScreen instanceof ChatScreen) return;
        // Hide under F3.
        if (client.currentScreen == null && client.getDebugHud().shouldShowDebugHud()) return;
        // Our menus render their own wordmark.
        if (client.currentScreen instanceof com.nebulaclient.mod.client.gui.SpaceTheme.SpaceScreen) return;
        // Respect the global "hide HUDs while TAB is held" gate.
        if (HudStyle.isPlayerListOpen()) return;

        Text text = Text.literal("NEBULACLIENT").formatted(Formatting.BOLD);
        int color = NebulaConfig.get().getHudColor(ID, 0xFF000000 | (NebulaConfig.get().getButtonColor() & 0xFFFFFF));
        float scale = NebulaConfig.get().getHudScale(ID);

        int height = client.getWindow().getScaledHeight();
        int defX = 4;
        int defY = height - (int) (10 * scale) - 4;
        int[] pos = NebulaConfig.get().getHudPosition(ID, defX, defY);

        HudStyle.beginScale(ctx, pos[0], pos[1], scale);
        ctx.drawTextWithShadow(client.textRenderer, text, 0, 0, color);
        HudStyle.endScale(ctx);
    }
}
