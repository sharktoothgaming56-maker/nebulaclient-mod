package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The small "NebulaClient" wordmark, pinned to the BOTTOM-LEFT of the
 * screen. Per the brief it always stays bottom-left and is sized by the
 * "Logo Size" setting (Nebula Menu), defaulting small — it is intentionally
 * NOT draggable, unlike the other HUDs.
 *
 * It shows during normal gameplay, and also stays put while a non-Nebula
 * screen (inventory, vanilla menus, chat) is open, so the mark is always
 * present. Our own menus draw their own wordmark, so it skips those to
 * avoid drawing it twice.
 */
public class LogoHud {
    public static final String ID = "logo";

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        // Respect the on/off toggle. Hide only while F3 is up (keeps the
        // debug overlay clean); otherwise it stays, screen open or not.
        if (!NebulaConfig.get().isHudEnabled(ID)) return;
        if (client.currentScreen == null && client.getDebugHud().shouldShowDebugHud()) return;
        // Skip our own space menus — they render their own wordmark.
        if (client.currentScreen instanceof com.nebulaclient.mod.client.gui.SpaceTheme.SpaceScreen) return;

        Text text = Text.literal("NEBULACLIENT").formatted(Formatting.BOLD);
        int color = NebulaConfig.get().getHudColor(ID, 0xFF000000 | (NebulaConfig.get().getButtonColor() & 0xFFFFFF));
        float scale = NebulaConfig.get().getLogoScale();

        int height = client.getWindow().getScaledHeight();
        int x = 4;                                   // pinned left
        int y = height - (int) (10 * scale) - 4;     // pinned bottom

        HudStyle.beginScale(ctx, x, y, scale);
        ctx.drawTextWithShadow(client.textRenderer, text, 0, 0, color);
        HudStyle.endScale(ctx);
    }
}
