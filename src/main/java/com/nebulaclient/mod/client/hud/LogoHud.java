package com.nebulaclient.mod.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * All the other HUDs hide themselves whenever a screen is open (inventory,
 * menus, chat, etc — see HudStyle.shouldRenderHuds()). This one does the
 * opposite on purpose: it only shows while a screen IS open — any screen,
 * not just inventory, since the check below is just "is a screen open at
 * all" — so there's always a "NebulaClient" mark instead of the other
 * HUDs just vanishing with nothing in their place.
 */
public class LogoHud {
    public static final String ID = "logo";
    private static final float SCALE = 1.8f;

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null || client.player == null) return;
        // Our own menus already draw the wordmark themselves — skip so it
        // doesn't render twice in the same corner.
        if (client.currentScreen instanceof com.nebulaclient.mod.client.gui.SpaceTheme.SpaceScreen) return;

        Text text = Text.literal("NEBULACLIENT").formatted(Formatting.BOLD);
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int x = width - (int) (client.textRenderer.getWidth(text) * SCALE) - 8;
        int y = height - (int) (12 * SCALE) - 4;
        HudStyle.beginScale(ctx, x, y, SCALE);
        ctx.drawTextWithShadow(client.textRenderer, text, 0, 0, 0xFFB98BFF);
        HudStyle.endScale(ctx);
    }
}
