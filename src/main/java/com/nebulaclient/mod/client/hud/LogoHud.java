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

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen == null || client.player == null) return;

        Text text = Text.literal("NEBULACLIENT").formatted(Formatting.BOLD); // vanilla's built-in bold style, not a manual scale hack
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        int x = width - client.textRenderer.getWidth(text) - 8;
        int y = height - 16;
        ctx.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFB98BFF);
    }
}
