package com.nebulaclient.mod.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Small shared drawing helpers so every HUD panel looks consistent. */
public final class HudStyle {
    public static final int PANEL_BG = 0xA0120E24;   // matches the launcher's --space-900, ~63% opaque
    public static final int PANEL_BORDER = 0x60332A54; // --line
    public static final int ACCENT = 0xFF9D6BFF;      // --nebula
    public static final int TEXT = 0xFFEAE6F7;        // --text
    public static final int TEXT_DIM = 0xFFA79FC4;    // --text-dim

    private HudStyle() {}

    public static void panel(DrawContext ctx, int x, int y, int width, int height) {
        ctx.fill(x, y, x + width, y + height, PANEL_BG);
        ctx.fill(x, y, x + width, y + 1, PANEL_BORDER);
        ctx.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER);
        ctx.fill(x, y, x + 1, y + height, PANEL_BORDER);
        ctx.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
    }

    /** True whenever no screen (inventory, menu, chat, etc.) is open — the only time HUDs should draw. */
    public static boolean shouldRenderHuds() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.currentScreen == null && client.player != null;
    }
}
