package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.hud.HudInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Drag-to-reposition HUD editor.
 *
 * NOTE ON APPROACH: Screen's mouseClicked/mouseDragged/mouseReleased were
 * reworked into a Click-object-based API in a recent version (confirmed
 * via a real build error — they used to take plain double x/y + int
 * button). Rather than guess at that new record's exact fields with no
 * way to test it, this does drag tracking entirely inside render() using
 * its plain int mouseX/mouseY (confirmed unchanged) plus a raw LWJGL
 * mouse-button poll — GLFW itself isn't touched by any Minecraft mapping
 * change, so this sidesteps the whole issue.
 */
public class HudEditorScreen extends Screen {
    private HudInfo dragging = null;
    private int dragOffsetX, dragOffsetY;
    private boolean wasMouseDown = false;

    public HudEditorScreen() {
        super(Text.literal("Edit HUDs"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // NOT calling this.renderBackground(...) — confirmed by a real crash
        // report that this can trip "Can only blur once per frame" on ANY
        // custom screen, not just the title screen, depending on what else
        // is rendering blur that frame. A plain fill is the safe version.
        ctx.fill(0, 0, this.width, this.height, 0xC0080510);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Drag boxes to reposition — Esc when done", this.width / 2, 12, 0xFFFFFFFF);

        NebulaConfig cfg = NebulaConfig.get();
        boolean mouseDown = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean justPressed = mouseDown && !wasMouseDown;
        boolean justReleased = !mouseDown && wasMouseDown;

        for (HudInfo info : HudInfo.ALL) {
            int[] pos = cfg.getHudPosition(info.id, 0, 0);
            boolean hasCustomPos = cfg.hudPosition.containsKey(info.id);
            if (!hasCustomPos) {
                pos = info.defaultPos.apply(this.client, new int[]{info.width, info.height});
            }
            boolean enabled = cfg.isHudEnabled(info.id);

            int color = enabled ? 0x559D6BFF : 0x55666666;
            ctx.fill(pos[0], pos[1], pos[0] + info.width, pos[1] + info.height, color);
            drawBoxOutline(ctx, pos[0], pos[1], info.width, info.height, enabled ? 0xFF9D6BFF : 0xFF666666);
            ctx.drawTextWithShadow(this.textRenderer, info.label, pos[0] + 4, pos[1] + 4, 0xFFFFFFFF);
            ctx.drawTextWithShadow(this.textRenderer, enabled ? "click to hide" : "hidden — click to show",
                    pos[0] + 4, pos[1] + info.height - 12, 0xFFCCCCCC);

            boolean inside = mouseX >= pos[0] && mouseX <= pos[0] + info.width && mouseY >= pos[1] && mouseY <= pos[1] + info.height;
            if (justPressed && inside && dragging == null) {
                if (mouseY >= pos[1] + info.height - 14) {
                    cfg.setHudEnabled(info.id, !enabled);
                } else {
                    dragging = info;
                    dragOffsetX = mouseX - pos[0];
                    dragOffsetY = mouseY - pos[1];
                }
            }
        }

        if (dragging != null && mouseDown) {
            cfg.hudPosition.put(dragging.id, new int[]{mouseX - dragOffsetX, mouseY - dragOffsetY}); // save() only on release, avoids spamming disk writes mid-drag
        }
        if (justReleased && dragging != null) {
            cfg.save();
            dragging = null;
        }

        wasMouseDown = mouseDown;
        super.render(ctx, mouseX, mouseY, delta);
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
