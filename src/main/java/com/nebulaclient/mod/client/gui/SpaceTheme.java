package com.nebulaclient.mod.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole space look, shared by every NebulaClient screen:
 *  - deterministic twinkling starfield (positions seeded per-index so the
 *    sky doesn't reshuffle every frame; only brightness animates)
 *  - two soft glowing planets (layered translucent discs)
 *  - a shooting star every few seconds (time-derived, no state needed)
 *  - NebulaButton: a glowing purple gradient button drawn by hand
 *
 * Everything here uses only ctx.fill / fillGradient / text drawing —
 * the APIs already proven working in this project — deliberately
 * avoiding ButtonWidget rendering overrides and the reworked
 * mouseClicked(Click) API that broke earlier. Click handling uses raw
 * GLFW polling, the same technique HudEditorScreen already runs on.
 */
public final class SpaceTheme {
    private SpaceTheme() {}

    /** Fully opaque variant (title screen). */
    public static void paintOpaque(DrawContext ctx, int w, int h) {
        ctx.fillGradient(0, 0, w, h, 0xFF2a1440, 0xFF08050F);
        paintSky(ctx, w, h, 1f);
    }

    /** Translucent variant (in-game menus — world stays faintly visible). */
    public static void paintOverlay(DrawContext ctx, int w, int h) {
        ctx.fillGradient(0, 0, w, h, 0xE0241033, 0xF008050F);
        paintSky(ctx, w, h, 0.8f);
    }

    /** Dim variant (HUD editor — needs to stay out of the way). */
    public static void paintDim(DrawContext ctx, int w, int h) {
        ctx.fillGradient(0, 0, w, h, 0xB01a0e28, 0xC008050F);
        paintSky(ctx, w, h, 0.4f);
    }

    private static void paintSky(DrawContext ctx, int w, int h, float intensity) {
        long time = System.currentTimeMillis();

        // Planets: a big warm one top-right, a small purple one mid-left.
        drawGlowDisc(ctx, (int) (w * 0.86), (int) (h * 0.12), 34, 255, 190, 120, 0.35f * intensity);
        drawGlowDisc(ctx, (int) (w * 0.10), (int) (h * 0.68), 13, 157, 107, 255, 0.45f * intensity);

        // Stars — ~1 per 4000 px², deterministic positions, twinkling alpha.
        int count = Math.max(30, (w * h) / 4000);
        for (int i = 0; i < count; i++) {
            long hash = (i * 2654435761L) ^ 0x9E3779B97F4A7C15L;
            int x = (int) (Math.abs(hash) % w);
            int y = (int) (Math.abs(hash >> 20) % h);
            int size = (Math.abs((int) (hash >> 40)) % 5 == 0) ? 2 : 1;
            float twinkle = (float) (0.55 + 0.45 * Math.sin(time / 400.0 + i * 1.7));
            int alpha = (int) (200 * twinkle * intensity);
            if (alpha <= 8) continue;
            ctx.fill(x, y, x + size, y + size, (alpha << 24) | 0xE6E1F7);
        }

        // Shooting star: one every ~3.5s, streaking for the first 0.7s of
        // its cycle. Derived purely from the clock, so no state to manage.
        long period = 3500;
        long cycle = time / period;
        float progress = (time % period) / 700f;
        if (progress < 1f) {
            long seed = cycle * 6364136223846793005L + 1442695040888963407L;
            int startX = (int) (Math.abs(seed) % (w * 3 / 4));
            int startY = (int) (Math.abs(seed >> 24) % (h / 3));
            float px = startX + progress * (w * 0.35f);
            float py = startY + progress * (h * 0.22f);
            for (int t = 0; t < 10; t++) {
                int alpha = (int) (220 * (1f - progress) * (1f - t / 10f) * intensity);
                if (alpha <= 8) break;
                int sx = (int) (px - t * 3.2f);
                int sy = (int) (py - t * 2.0f);
                ctx.fill(sx, sy, sx + 2, sy + 2, (alpha << 24) | 0xFFFFFF);
            }
        }
    }

    /** A soft glowing disc: concentric translucent filled circles, drawn as row strips. */
    public static void drawGlowDisc(DrawContext ctx, int cx, int cy, int radius, int r, int g, int b, float coreAlpha) {
        for (int layer = 3; layer >= 0; layer--) {
            int lr = radius + layer * (radius / 3 + 2);
            int alpha = (int) (255 * coreAlpha / (1 + layer * 2));
            if (alpha <= 4) continue;
            fillDisc(ctx, cx, cy, lr, (alpha << 24) | (r << 16) | (g << 8) | b);
        }
    }

    /** Filled circle via horizontal strips — no circle primitive needed. */
    public static void fillDisc(DrawContext ctx, int cx, int cy, int radius, int argb) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.round(Math.sqrt((double) radius * radius - (double) dy * dy));
            ctx.fill(cx - half, cy + dy, cx + half, cy + dy + 1, argb);
        }
    }

    // =================================================================
    // NebulaButton — hand-drawn glowing button
    // =================================================================
    public static class NebulaButton {
        public int x, y, width, height;
        private String label;
        private Runnable action;
        public boolean bold;

        public NebulaButton(int x, int y, int width, int height, String label, Runnable action) {
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.label = label; this.action = action;
        }

        public void setLabel(String label) { this.label = label; }
        public void setAction(Runnable action) { this.action = action; }

        public boolean isHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        public void render(DrawContext ctx, MinecraftClient client, int mouseX, int mouseY) {
            boolean hover = isHovered(mouseX, mouseY);
            int top = hover ? 0xF0432a6e : 0xE02a1845;
            int bottom = hover ? 0xF0241238 : 0xE0180d2b;
            ctx.fillGradient(x, y, x + width, y + height, top, bottom);

            int border = hover ? 0xFFB98BFF : 0x809D6BFF;
            ctx.fill(x, y, x + width, y + 1, border);
            ctx.fill(x, y + height - 1, x + width, y + height, border);
            ctx.fill(x, y, x + 1, y + height, border);
            ctx.fill(x + width - 1, y, x + width, y + height, border);
            if (hover) { // outer glow line
                int glow = 0x309D6BFF;
                ctx.fill(x - 1, y - 1, x + width + 1, y, glow);
                ctx.fill(x - 1, y + height, x + width + 1, y + height + 1, glow);
                ctx.fill(x - 1, y, x, y + height, glow);
                ctx.fill(x + width, y, x + width + 1, y + height, glow);
            }

            Text text = bold ? Text.literal(label).formatted(Formatting.BOLD) : Text.literal(label);
            int tx = x + (width - client.textRenderer.getWidth(text)) / 2;
            int ty = y + (height - 8) / 2;
            ctx.drawTextWithShadow(client.textRenderer, text, tx, ty, hover ? 0xFFFFFFFF : 0xFFEAE6F7);
        }

        public void click() { if (action != null) action.run(); }
    }

    // =================================================================
    // SpaceScreen — base for all NebulaClient screens: paints the theme,
    // owns a NebulaButton list, and does GLFW edge-detected clicking.
    // =================================================================
    public abstract static class SpaceScreen extends Screen {
        protected final List<NebulaButton> nebulaButtons = new ArrayList<>();
        // Starts true so a click held over from the previous screen can't
        // instantly trigger a button sitting under the cursor.
        private boolean wasMouseDown = true;
        protected boolean justPressed = false; // exposed for subclasses with extra custom hit areas

        protected SpaceScreen(Text title) { super(title); }

        /** true = opaque space background (title screen); false = translucent overlay. */
        protected boolean opaqueBackground() { return false; }

        protected abstract void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta);

        @Override
        public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
            if (opaqueBackground()) SpaceTheme.paintOpaque(ctx, this.width, this.height);
            else SpaceTheme.paintOverlay(ctx, this.width, this.height);

            boolean mouseDown = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            justPressed = mouseDown && !wasMouseDown;
            wasMouseDown = mouseDown;

            for (NebulaButton b : nebulaButtons) {
                b.render(ctx, this.client, mouseX, mouseY);
                if (justPressed && b.isHovered(mouseX, mouseY)) b.click();
            }

            renderContent(ctx, mouseX, mouseY, delta);

            // Big NEBULACLIENT wordmark, bottom-right of every menu.
            Text mark = Text.literal("NEBULACLIENT").formatted(Formatting.BOLD);
            float markScale = 1.8f;
            int mx = this.width - (int) (this.client.textRenderer.getWidth(mark) * markScale) - 8;
            int my = this.height - (int) (12 * markScale) - 4;
            com.nebulaclient.mod.client.hud.HudStyle.beginScale(ctx, mx, my, markScale);
            ctx.drawTextWithShadow(this.client.textRenderer, mark, 0, 0, 0xFFB98BFF);
            com.nebulaclient.mod.client.hud.HudStyle.endScale(ctx);

            super.render(ctx, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldPause() { return false; }
    }
}
