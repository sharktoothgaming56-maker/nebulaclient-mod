package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.keybind.KeybindManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Name + colour picker for a new keybind preset. Mirrors WaypointNameScreen
 * (a known-good pattern in this project): a vanilla TextFieldWidget added
 * via addDrawableChild so vanilla routes typing/clicks to it, plus a swatch
 * grid for the colour tag. On save it snapshots the CURRENT keybinds into
 * the new preset, so "set your keys, then name+save" is the whole flow.
 */
public class PresetNameScreen extends SpaceTheme.SpaceScreen {
    private final Screen parent;
    private TextFieldWidget nameField;
    private int selectedColor = com.nebulaclient.mod.client.config.NebulaConfig.DEFAULT_ACCENT;

    public PresetNameScreen(Screen parent) {
        super(Text.literal("New Preset"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nebulaButtons.clear();

        nameField = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 70, 200, 20, Text.literal("Preset name"));
        nameField.setText("Preset " + (com.nebulaclient.mod.client.config.NebulaConfig.get().keybindPresets.size() + 1));
        this.addDrawableChild(nameField);
        this.setInitialFocus(nameField);

        SpaceTheme.NebulaButton save = new SpaceTheme.NebulaButton(this.width / 2 - 100, 170, 98, 22, "Save", null);
        save.bold = true;
        save.setAction(() -> {
            String name = nameField.getText().isBlank()
                    ? "Preset " + (com.nebulaclient.mod.client.config.NebulaConfig.get().keybindPresets.size() + 1)
                    : nameField.getText().trim();
            KeybindManager.createFromCurrent(name, selectedColor);
            this.client.setScreen(new KeybindPresetScreen(parent));
        });
        nebulaButtons.add(save);
        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 + 2, 170, 98, 22, "Cancel",
                () -> this.client.setScreen(new KeybindPresetScreen(parent))));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int accent = 0xFF000000 | (com.nebulaclient.mod.client.config.NebulaConfig.get().getButtonColor() & 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("NEW PRESET").formatted(Formatting.BOLD),
                this.width / 2, 40, accent);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Saves your current key binds under this name", this.width / 2, 100, 0xFFA79FC4);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Colour", this.width / 2, 116, 0xFFA79FC4);

        // Swatch row (wraps if the window is narrow).
        int chip = 16, gap = 8;
        int perRow = Math.max(4, Math.min(NebulaColors.PALETTE.size(), (this.width - 40) / (chip + gap)));
        int gridW = perRow * chip + (perRow - 1) * gap;
        int startX = (this.width - gridW) / 2;
        int startY = 130;
        for (int i = 0; i < NebulaColors.PALETTE.size(); i++) {
            NebulaColors.Swatch sw = NebulaColors.PALETTE.get(i);
            int col = i % perRow, row = i / perRow;
            int cx = startX + col * (chip + gap);
            int cy = startY + row * (chip + gap);
            boolean selected = sw.argb() == selectedColor;
            boolean hover = mouseX >= cx && mouseX <= cx + chip && mouseY >= cy && mouseY <= cy + chip;
            if (selected || hover) {
                ctx.fill(cx - 2, cy - 2, cx + chip + 2, cy + chip + 2, selected ? 0xFFFFFFFF : 0x80FFFFFF);
            }
            ctx.fill(cx, cy, cx + chip, cy + chip, sw.argb());
            if (justPressed && hover) selectedColor = sw.argb();
        }
    }
}
