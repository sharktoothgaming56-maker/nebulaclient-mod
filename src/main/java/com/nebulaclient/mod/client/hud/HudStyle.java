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

    /**
     * True whenever no screen is open AND the F3 debug overlay is closed —
     * the only time HUDs should draw, so they never sit on top of F3.
     */
    public static boolean shouldRenderHuds() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.player == null) return false;
        // NOTE: if getDebugHud()/shouldShowDebugHud() doesn't compile, the
        // older form was the field `client.options.debugEnabled` — swap this
        // line for `if (client.options.debugEnabled) return false;`
        if (client.getDebugHud().shouldShowDebugHud()) return false;
        return true;
    }

    /**
     * Wraps a HUD's drawing in a translate+scale so every HUD can be resized
     * from the editor. Draw everything relative to (0,0) between begin/end.
     *
     * NOTE: this uses the post-1.21.6 Matrix3x2fStack-style DrawContext
     * matrix API (pushMatrix/translate(x,y)/scale(x,y)/popMatrix). If these
     * names don't compile, the older MatrixStack style was:
     *   ctx.getMatrices().push(); ...translate(x, y, 0); ...scale(s, s, 1);
     * and endScale() -> ctx.getMatrices().pop();
     */
    public static void beginScale(DrawContext ctx, int x, int y, float scale) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
    }

    public static void endScale(DrawContext ctx) {
        ctx.getMatrices().popMatrix();
    }
}
