package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.keybind.KeybindManager;
import com.nebulaclient.mod.client.keybind.KeybindPreset;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * The saved keybind sets ("Presets"). Reached from the Presets button on the
 * Key Binds screen (top-right) or the Keybinds hub.
 *
 * SELECT = APPLY: clicking a preset row immediately swaps every keybinding
 * (vanilla + this mod's) to that set and marks it active — no separate
 * Apply button, exactly per the brief. Each row also has a small ✕ to
 * delete. Everything is stored locally in config/nebulaclient.json.
 */
public class KeybindPresetScreen extends SpaceTheme.SpaceScreen {
    private final Screen parent;
    private int scroll = 0;

    private static final int ROW_TOP = 80;
    private static final int ROW_H = 26;

    public KeybindPresetScreen(Screen parent) {
        super(Text.literal("Key Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nebulaButtons.clear();

        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), 46, panelWidth(), 22, "Save Current as New Set",
                () -> this.client.setScreen(new PresetNameScreen(this))));

        SpaceTheme.NebulaButton back = new SpaceTheme.NebulaButton(
                panelX(), this.height - 32, panelWidth(), 22, "Back",
                () -> this.client.setScreen(parent));
        back.bold = true;
        nebulaButtons.add(back);

        rebuildRows();
    }

    /** Rebuilds the per-preset row buttons (apply-on-click + delete). */
    private void rebuildRows() {
        while (nebulaButtons.size() > 2) nebulaButtons.remove(nebulaButtons.size() - 1); // keep Save + Back

        NebulaConfig cfg = NebulaConfig.get();
        List<KeybindPreset> presets = cfg.keybindPresets;
        int listBottom = this.height - 40;

        int y = ROW_TOP - scroll;
        for (KeybindPreset preset : presets) {
            if (y + 22 >= ROW_TOP && y <= listBottom) {
                boolean active = preset.name != null && preset.name.equals(cfg.activePreset);
                // Main row button: chip-width inset on the left, ✕ on the right.
                int rowX = panelX() + 20;
                int rowW = panelWidth() - 20 - 28;
                SpaceTheme.NebulaButton apply = new SpaceTheme.NebulaButton(rowX, y, rowW, 22,
                        preset.name + (active ? "  (active)" : ""), () -> {
                    KeybindManager.apply(preset); // select = apply, instantly
                    this.client.setScreen(new KeybindPresetScreen(parent));
                });
                apply.bold = active;
                SpaceTheme.NebulaButton del = new SpaceTheme.NebulaButton(panelX() + panelWidth() - 24, y, 24, 22, "✕", () -> {
                    KeybindManager.delete(preset);
                    this.client.setScreen(new KeybindPresetScreen(parent));
                });
                nebulaButtons.add(apply);
                nebulaButtons.add(del);
            }
            y += ROW_H;
        }
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NebulaConfig cfg = NebulaConfig.get();
        int accent = 0xFF000000 | (cfg.getStrokeColor() & 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("KEY PRESETS").formatted(Formatting.BOLD),
                this.width / 2, 22, accent);

        List<KeybindPreset> presets = cfg.keybindPresets;
        int listBottom = this.height - 40;

        if (presets.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    "No saved sets yet — set your keys, then \"Save Preset\".", this.width / 2, ROW_TOP + 10, 0xFFA79FC4);
            return;
        }

        ctx.drawCenteredTextWithShadow(this.textRenderer, "Click a set to apply it", this.width / 2, 68, 0xFFA79FC4);

        // Colour chips beside each visible row (chips only; the row itself
        // is a real button drawn by the base class).
        ctx.enableScissor(0, ROW_TOP, this.width, listBottom);
        int y = ROW_TOP - scroll;
        for (KeybindPreset preset : presets) {
            if (y + 22 >= ROW_TOP && y <= listBottom) {
                int chip = 14;
                ctx.fill(panelX(), y + 4, panelX() + chip, y + 4 + chip, 0xFF000000 | (preset.color & 0xFFFFFF));
            }
            y += ROW_H;
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int viewport = (this.height - 40) - ROW_TOP;
        int contentHeight = NebulaConfig.get().keybindPresets.size() * ROW_H;
        int maxScroll = Math.max(0, contentHeight - viewport);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (verticalAmount * 18)));
        rebuildRows();
        return true;
    }
}
