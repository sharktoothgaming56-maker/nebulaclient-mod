package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.hud.HudInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Drag-to-move + click-to-resize HUD editor. Boxes are shown at each
 * HUD's actual scaled size; the [-]/[+] squares in the top-right corner
 * of each box shrink/grow it (0.5x–2.0x in 0.25 steps), the bottom strip
 * toggles it, anywhere else drags. Mouse handling stays on raw GLFW
 * polling (the Click-object Screen API this version introduced broke the
 * classic overrides — confirmed by a real build failure).
 */
public class HudEditorScreen extends Screen {
    private HudInfo dragging = null;
    private int dragOffsetX, dragOffsetY;
    private boolean wasMouseDown = true; // true so a held-over click can't instantly grab a box

    public HudEditorScreen() {
        super(Text.literal("Edit HUDs"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        SpaceTheme.paintDim(ctx, this.width, this.height);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Drag to move · +/- to resize · bottom strip toggles · Esc when done", this.width / 2, 12, 0xFFFFFFFF);

        NebulaConfig cfg = NebulaConfig.get();
        boolean mouseDown = GLFW.glfwGetMouseButton(this.client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean justPressed = mouseDown && !wasMouseDown;
        boolean justReleased = !mouseDown && wasMouseDown;

        for (HudInfo info : HudInfo.ALL) {
            float scale = cfg.getHudScale(info.id);
            int w = (int) (info.width * scale);
            int h = Math.max((int) (info.height * scale), 30); // keep small HUDs' boxes usable
            int[] pos = cfg.hudPosition.containsKey(info.id)
                    ? cfg.getHudPosition(info.id, 0, 0)
                    : info.defaultPos.apply(this.client, new int[]{w, h});
            boolean enabled = cfg.isHudEnabled(info.id);

            ctx.fill(pos[0], pos[1], pos[0] + w, pos[1] + h, enabled ? 0x559D6BFF : 0x55666666);
            drawBoxOutline(ctx, pos[0], pos[1], w, h, enabled ? 0xFF9D6BFF : 0xFF666666);
            ctx.drawTextWithShadow(this.textRenderer, info.label + "  " + scale + "x", pos[0] + 4, pos[1] + 4, 0xFFFFFFFF);
            ctx.drawTextWithShadow(this.textRenderer, enabled ? "click to hide" : "hidden — click to show",
                    pos[0] + 4, pos[1] + h - 12, 0xFFCCCCCC);

            // Resize controls: two 12px squares in the top-right corner.
            int plusX = pos[0] + w - 13, minusX = pos[0] + w - 27, ctlY = pos[1] + 2;
            ctx.fill(minusX, ctlY, minusX + 12, ctlY + 12, 0xC0241238);
            ctx.fill(plusX, ctlY, plusX + 12, ctlY + 12, 0xC0241238);
            ctx.drawTextWithShadow(this.textRenderer, "-", minusX + 4, ctlY + 2, 0xFFB98BFF);
            ctx.drawTextWithShadow(this.textRenderer, "+", plusX + 3, ctlY + 2, 0xFFB98BFF);

            boolean inside = mouseX >= pos[0] && mouseX <= pos[0] + w && mouseY >= pos[1] && mouseY <= pos[1] + h;
            if (justPressed && inside && dragging == null) {
                boolean onMinus = mouseX >= minusX && mouseX <= minusX + 12 && mouseY >= ctlY && mouseY <= ctlY + 12;
                boolean onPlus = mouseX >= plusX && mouseX <= plusX + 12 && mouseY >= ctlY && mouseY <= ctlY + 12;
                if (onMinus) {
                    cfg.setHudScale(info.id, scale - 0.25f);
                } else if (onPlus) {
                    cfg.setHudScale(info.id, scale + 0.25f);
                } else if (mouseY >= pos[1] + h - 14) {
                    cfg.setHudEnabled(info.id, !enabled);
                } else {
                    dragging = info;
                    dragOffsetX = mouseX - pos[0];
                    dragOffsetY = mouseY - pos[1];
                }
            }
        }

        if (dragging != null && mouseDown) {
            cfg.hudPosition.put(dragging.id, new int[]{mouseX - dragOffsetX, mouseY - dragOffsetY}); // save() only on release
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
