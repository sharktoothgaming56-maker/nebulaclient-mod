package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The small "NebulaClient" wordmark, PINNED to the bottom-left. It is not
 * part of the HUD editor and cannot be moved or resized — the only control
 * is the "NebulaClient Logo" ON/OFF toggle in the Nebula Menu, per the
 * brief. It still hides itself while chat/commands are open, while F3 is
 * up, while TAB (player list) is held, and on NebulaClient's own menus
 * (which draw their own wordmark).
 */
public class LogoHud {
    public static final String ID = "logo";
    private static final float SCALE = 1.0f;

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (!NebulaConfig.get().isHudEnabled(ID)) return;
        if (client.currentScreen instanceof ChatScreen) return;
        if (client.currentScreen == null && client.getDebugHud().shouldShowDebugHud()) return;
        if (client.currentScreen instanceof com.nebulaclient.mod.client.gui.SpaceTheme.SpaceScreen) return;
        if (HudStyle.isPlayerListOpen()) return;

        Text text = Text.literal("NEBULACLIENT").formatted(Formatting.BOLD);
        int color = NebulaConfig.get().getHudColor(ID, 0xFF000000 | (NebulaConfig.get().getButtonColor() & 0xFFFFFF));
        int height = client.getWindow().getScaledHeight();
        HudStyle.beginScale(ctx, 4, height - (int) (10 * SCALE) - 4, SCALE);
        ctx.drawTextWithShadow(client.textRenderer, text, 0, 0, color);
        HudStyle.endScale(ctx);
    }
}
