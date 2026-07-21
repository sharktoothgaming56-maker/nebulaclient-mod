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
 * The keybind preset manager. Each preset is a named, colour-tagged set of
 * key binds — e.g. a "Builder" preset holding your building keys and a
 * "PvP" preset holding your combat keys — and applying one swaps every
 * keybinding to match in a single click.
 *
 * Flow:
 *  - "Edit Key Binds" opens vanilla's Controls/Key Binds screen so you set
 *    keys the normal way, then come back and save them as a preset.
 *  - "New Preset from current binds" snapshots whatever keys are set right
 *    now into a new named+coloured preset.
 *  - Each preset row: Apply (make it live), Save (re-capture current binds
 *    into it), Delete. The active preset is marked.
 *
 * Rows scroll if there are more presets than fit, same viewport approach
 * as the Nebula Menu, so the layout never squishes.
 */
public class KeybindPresetScreen extends SpaceTheme.SpaceScreen {
    private final Screen parent;
    private int scroll = 0;

    public KeybindPresetScreen(Screen parent) {
        super(Text.literal("Key Presets"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nebulaButtons.clear();

        // Top actions.
        // Top action: save the current binds as a new set.
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), 46, panelWidth(), 22, "Save Current as New Set",
                () -> this.client.setScreen(new PresetNameScreen(this))));

        // Footer.
        SpaceTheme.NebulaButton back = new SpaceTheme.NebulaButton(
                panelX(), this.height - 32, panelWidth(), 22, "Back",
                () -> this.client.setScreen(parent));
        back.bold = true;
        nebulaButtons.add(back);

        rebuildRows();
    }

    /** Rebuilds the per-preset Apply/Save/Delete buttons (called on init and after any change). */
    private void rebuildRows() {
        // Remove any old row buttons: keep only the 3 fixed ones (2 top + back).
        while (nebulaButtons.size() > 2) nebulaButtons.remove(nebulaButtons.size() - 1); // keep the 2 fixed (Save + Back)

        NebulaConfig cfg = NebulaConfig.get();
        List<KeybindPreset> presets = cfg.keybindPresets;

        int rowTop = 80;
        int rowH = 26;
        int listBottom = this.height - 40;

        int y = rowTop - scroll;
        for (KeybindPreset preset : presets) {
            if (y + rowH >= rowTop && y <= listBottom) {
                int bw = Math.max(40, (panelWidth() - 8) / 3 - 40);
                int applyX = panelX() + 120;
                int saveX = applyX + bw + 4;
                int delX = saveX + bw + 4;
                final int fy = y;
                SpaceTheme.NebulaButton apply = new SpaceTheme.NebulaButton(applyX, fy, bw, 20, "Apply", () -> {
                    KeybindManager.apply(preset);
                    this.client.setScreen(new KeybindPresetScreen(parent));
                });
                SpaceTheme.NebulaButton save = new SpaceTheme.NebulaButton(saveX, fy, bw, 20, "Save", () -> {
                    KeybindManager.updateFromCurrent(preset);
                    this.client.setScreen(new KeybindPresetScreen(parent));
                });
                SpaceTheme.NebulaButton del = new SpaceTheme.NebulaButton(delX, fy, bw, 20, "Delete", () -> {
                    KeybindManager.delete(preset);
                    this.client.setScreen(new KeybindPresetScreen(parent));
                });
                nebulaButtons.add(apply);
                nebulaButtons.add(save);
                nebulaButtons.add(del);
            }
            y += rowH;
        }
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int accent = 0xFF000000 | (NebulaConfig.get().getStrokeColor() & 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("KEY PRESETS").formatted(Formatting.BOLD),
                this.width / 2, 22, accent);

        NebulaConfig cfg = NebulaConfig.get();
        List<KeybindPreset> presets = cfg.keybindPresets;
        int rowTop = 80;
        int rowH = 26;
        int listBottom = this.height - 40;

        if (presets.isEmpty()) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    "No saved sets yet — save your keybinds from the Keybinds screen.", this.width / 2, rowTop + 10, 0xFFA79FC4);
            return;
        }

        ctx.enableScissor(0, rowTop, this.width, listBottom);
        int y = rowTop - scroll;
        for (KeybindPreset preset : presets) {
            if (y + rowH >= rowTop && y <= listBottom) {
                // Colour chip + name + active marker on the left of each row.
                int chip = 14;
                int chipX = panelX();
                int chipY = y + 3;
                ctx.fill(chipX, chipY, chipX + chip, chipY + chip, 0xFF000000 | (preset.color & 0xFFFFFF));
                boolean active = preset.name != null && preset.name.equals(cfg.activePreset);
                String label = preset.name + (active ? "  (active)" : "");
                ctx.drawTextWithShadow(this.textRenderer,
                        Text.literal(label).formatted(Formatting.BOLD),
                        chipX + chip + 6, y + 6, active ? accent : 0xFFEAE6F7);
            }
            y += rowH;
        }
        ctx.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int rowTop = 80, rowH = 26;
        int listBottom = this.height - 40;
        int viewport = listBottom - rowTop;
        int contentHeight = NebulaConfig.get().keybindPresets.size() * rowH;
        int maxScroll = Math.max(0, contentHeight - viewport);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) (verticalAmount * 18)));
        rebuildRows();
        return true;
    }
}
