package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Picks the one shared colour used by every NebulaButton across all menus
 * (borders, glow, wordmark, scrollbar). Clicking a swatch writes it to
 * config immediately, so the change is visible live — the buttons on this
 * very screen recolour as soon as you pick, which doubles as the preview.
 */
public class ButtonColorScreen extends SpaceTheme.SpaceScreen {
    private final Screen parent;

    public ButtonColorScreen(Screen parent) {
        super(Text.literal("Button Colour"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nebulaButtons.clear();
        SpaceTheme.NebulaButton back = new SpaceTheme.NebulaButton(
                panelX(), this.height - 32, panelWidth(), 22, "Back",
                () -> this.client.setScreen(parent));
        back.bold = true;
        nebulaButtons.add(back);
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int accent = 0xFF000000 | (NebulaConfig.get().getButtonColor() & 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("BUTTON COLOUR").formatted(Formatting.BOLD),
                this.width / 2, 24, accent);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Recolours every menu button", this.width / 2, 40, 0xFFA79FC4);

        // Swatch grid, centred, wrapping to the window width.
        int chip = 30, gap = 12;
        int perRow = Math.max(3, Math.min(NebulaColors.PALETTE.size(),
                (this.width - 40) / (chip + gap)));
        int gridW = perRow * chip + (perRow - 1) * gap;
        int startX = (this.width - gridW) / 2;
        int startY = 70;

        int current = NebulaConfig.get().getButtonColor();

        for (int i = 0; i < NebulaColors.PALETTE.size(); i++) {
            NebulaColors.Swatch sw = NebulaColors.PALETTE.get(i);
            int col = i % perRow, row = i / perRow;
            int cx = startX + col * (chip + gap);
            int cy = startY + row * (chip + gap + 12);

            boolean selected = sw.argb() == current;
            boolean hover = mouseX >= cx && mouseX <= cx + chip && mouseY >= cy && mouseY <= cy + chip;
            if (selected || hover) {
                ctx.fill(cx - 2, cy - 2, cx + chip + 2, cy + chip + 2, selected ? 0xFFFFFFFF : 0x80FFFFFF);
            }
            ctx.fill(cx, cy, cx + chip, cy + chip, sw.argb());
            ctx.drawCenteredTextWithShadow(this.textRenderer, sw.name(), cx + chip / 2, cy + chip + 2, 0xFFCCCCCC);

            if (justPressed && hover) {
                NebulaConfig.get().setButtonColor(sw.argb());
            }
        }
    }
}
