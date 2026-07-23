package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.hud.HudInfo;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * HUD editor, reworked around the pattern established client editors use
 * (select -> edit in a panel, drag with snapping) instead of tiny per-box
 * corner icons:
 *
 *  - CLICK a HUD box to select it (white outline). Click empty space to
 *    deselect.
 *  - DRAG a box to move it. While dragging, the box SNAPS to the screen's
 *    vertical/horizontal centre and to an 6px margin from each edge; cyan
 *    guide lines show when a snap is active. This makes tidy layouts easy,
 *    which was the main inefficiency before.
 *  - The BOTTOM BAR shows big controls for the selected HUD:
 *    [Shown/Hidden] [-] [1.0x] [+] [Colour] [Reset pos]. Large targets, no
 *    overlap with the box labels.
 *
 * Backdrop: the captured gameplay still (if opened from the pause menu),
 * else the dim theme. Mouse stays on raw GLFW polling — the Click-object
 * Screen API broke the classic overrides in this MC version (known issue
 * in this project).
 */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 6;      // snap distance in px
    private static final int MARGIN = 6;    // edge-margin snap target
    private static final int BAR_H = 30;

    private HudInfo dragging = null;
    private HudInfo selected = null;
    private int dragOffsetX, dragOffsetY;
    private boolean wasMouseDown = true; // true so a held-over click can't instantly grab
    private boolean snappedX = false, snappedY = false;
    private int snapLineX = 0, snapLineY = 0;

    public HudEditorScreen() {
        super(Text.literal("Edit HUDs"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (GameplayPreview.isAvailable()) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, GameplayPreview.TEXTURE_ID,
                    0, 0, 0f, 0f, this.width, this.height,
                    this.width, this.height, this.width, this.height, 0xFFFFFFFF);
            ctx.fill(0, 0, this.width, this.height, 0x66000000);
        } else {
            SpaceTheme.paintDim(ctx, this.width, this.height);
        }

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                "Click to select · drag to move (snaps to centre/edges) · Esc when done",
                this.width / 2, 8, 0xFFFFFFFF);

        NebulaConfig cfg = NebulaConfig.get();
        boolean mouseDown = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean justPressed = mouseDown && !wasMouseDown;
        boolean justReleased = !mouseDown && wasMouseDown;
        boolean clickedSomething = false;

        // ---- Bottom control bar for the selected HUD (drawn/handled first
        // so its clicks never fall through to boxes underneath) ----
        if (selected != null) {
            clickedSomething |= renderControlBar(ctx, cfg, mouseX, mouseY, justPressed);
        } else {
            ctx.fill(0, this.height - BAR_H, this.width, this.height, 0xC0140b22);
            ctx.drawCenteredTextWithShadow(this.textRenderer, "Select a HUD to edit it",
                    this.width / 2, this.height - BAR_H / 2 - 4, 0xFFA79FC4);
        }

        // ---- HUD boxes ----
        for (HudInfo info : HudInfo.ALL) {
            float scale = cfg.getHudScale(info.id);
            int w = (int) (info.width * scale);
            int h = Math.max((int) (info.height * scale), 24);
            int[] pos = cfg.hudPosition.containsKey(info.id)
                    ? cfg.getHudPosition(info.id, 0, 0)
                    : info.defaultPos.apply(this.client, new int[]{w, h});
            boolean enabled = cfg.isHudEnabled(info.id);
            boolean isSel = selected == info;

            int fill = enabled ? 0x559D6BFF : 0x40666666;
            ctx.fill(pos[0], pos[1], pos[0] + w, pos[1] + h, fill);
            int outline = isSel ? 0xFFFFFFFF : (enabled ? 0xFF9D6BFF : 0xFF666666);
            drawBoxOutline(ctx, pos[0], pos[1], w, h, outline);
            ctx.drawTextWithShadow(this.textRenderer, info.label, pos[0] + 4, pos[1] + 4,
                    enabled ? 0xFFFFFFFF : 0xFF999999);

            boolean inside = mouseX >= pos[0] && mouseX <= pos[0] + w && mouseY >= pos[1] && mouseY <= pos[1] + h
                    && mouseY < this.height - BAR_H; // bar area never selects boxes
            if (justPressed && inside && dragging == null && !clickedSomething) {
                selected = info;
                dragging = info;
                dragOffsetX = mouseX - pos[0];
                dragOffsetY = mouseY - pos[1];
                clickedSomething = true;
            }
        }

        // Click on empty space (not bar, not a box) deselects.
        if (justPressed && !clickedSomething && mouseY < this.height - BAR_H) {
            selected = null;
        }

        // ---- Dragging with snapping ----
        snappedX = snappedY = false;
        if (dragging != null && mouseDown) {
            float scale = cfg.getHudScale(dragging.id);
            int w = (int) (dragging.width * scale);
            int h = Math.max((int) (dragging.height * scale), 24);
            int nx = mouseX - dragOffsetX;
            int ny = mouseY - dragOffsetY;

            // Snap X: screen centre, left margin, right margin.
            int cx = this.width / 2 - w / 2;
            if (Math.abs(nx - cx) <= SNAP) { nx = cx; snappedX = true; snapLineX = this.width / 2; }
            else if (Math.abs(nx - MARGIN) <= SNAP) { nx = MARGIN; snappedX = true; snapLineX = MARGIN; }
            else if (Math.abs(nx - (this.width - MARGIN - w)) <= SNAP) { nx = this.width - MARGIN - w; snappedX = true; snapLineX = this.width - MARGIN; }

            // Snap Y: screen centre, top margin, bottom margin (above bar).
            int cy = this.height / 2 - h / 2;
            int bottomLimit = this.height - BAR_H;
            if (Math.abs(ny - cy) <= SNAP) { ny = cy; snappedY = true; snapLineY = this.height / 2; }
            else if (Math.abs(ny - MARGIN) <= SNAP) { ny = MARGIN; snappedY = true; snapLineY = MARGIN; }
            else if (Math.abs(ny - (bottomLimit - MARGIN - h)) <= SNAP) { ny = bottomLimit - MARGIN - h; snappedY = true; snapLineY = bottomLimit - MARGIN; }

            cfg.hudPosition.put(dragging.id, new int[]{nx, ny}); // save() on release

            if (snappedX) ctx.fill(snapLineX, 0, snapLineX + 1, this.height - BAR_H, 0xFF35D0E0);
            if (snappedY) ctx.fill(0, snapLineY, this.width, snapLineY + 1, 0xFF35D0E0);
        }
        if (justReleased && dragging != null) {
            cfg.save();
            dragging = null;
        }

        wasMouseDown = mouseDown;
        super.render(ctx, mouseX, mouseY, delta);
    }

    /** Draws + handles the bottom bar; returns true if a bar control was clicked. */
    private boolean renderControlBar(DrawContext ctx, NebulaConfig cfg, int mouseX, int mouseY, boolean justPressed) {
        int barY = this.height - BAR_H;
        ctx.fill(0, barY, this.width, this.height, 0xC0140b22);
        ctx.fill(0, barY, this.width, barY + 1, 0x80B98BFF);

        boolean enabled = cfg.isHudEnabled(selected.id);
        float scale = cfg.getHudScale(selected.id);
        int hudColor = cfg.getHudColor(selected.id, 0xFFEAE6F7);

        ctx.drawTextWithShadow(this.textRenderer, selected.label, 8, barY + 11, 0xFFFFFFFF);

        int x = Math.max(120, this.width / 2 - 150);
        boolean clicked = false;

        clicked |= barButton(ctx, x, barY + 5, 60, 20, enabled ? "Shown" : "Hidden", mouseX, mouseY, justPressed,
                () -> cfg.setHudEnabled(selected.id, !enabled));
        x += 64;
        clicked |= barButton(ctx, x, barY + 5, 20, 20, "-", mouseX, mouseY, justPressed,
                () -> cfg.setHudScale(selected.id, scale - 0.25f));
        x += 22;
        ctx.drawCenteredTextWithShadow(this.textRenderer, String.format("%.2fx", scale), x + 20, barY + 11, 0xFFEAE6F7);
        x += 42;
        clicked |= barButton(ctx, x, barY + 5, 20, 20, "+", mouseX, mouseY, justPressed,
                () -> cfg.setHudScale(selected.id, scale + 0.25f));
        x += 26;
        // Colour: swatch-styled button opening the full picker.
        boolean hovC = mouseX >= x && mouseX <= x + 56 && mouseY >= barY + 5 && mouseY <= barY + 25;
        ctx.fill(x, barY + 5, x + 56, barY + 25, hudColor);
        drawBoxOutline(ctx, x, barY + 5, 56, 20, hovC ? 0xFFFFFFFF : 0x80FFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Colour", x + 28, barY + 11, 0xFF140b22);
        if (justPressed && hovC) {
            this.client.setScreen(new HudColorScreen(this, selected.id, selected.label));
            clicked = true;
        }
        x += 60;
        clicked |= barButton(ctx, x, barY + 5, 70, 20, "Reset pos", mouseX, mouseY, justPressed, () -> {
            cfg.hudPosition.remove(selected.id);
            cfg.save();
        });
        return clicked;
    }

    /** Small flat bar button; runs action and returns true when clicked. */
    private boolean barButton(DrawContext ctx, int x, int y, int w, int h, String label,
                              int mouseX, int mouseY, boolean justPressed, Runnable action) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        ctx.fill(x, y, x + w, y + h, hover ? 0xF0432a6e : 0xE02a1845);
        drawBoxOutline(ctx, x, y, w, h, hover ? 0xFFB98BFF : 0x80B98BFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, label, x + w / 2, y + (h - 8) / 2, 0xFFEAE6F7);
        if (justPressed && hover) {
            action.run();
            return true;
        }
        return false;
    }

    private static void drawBoxOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
