package com.nebulaclient.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.text.Text;

/**
 * NebulaClient's keybinds landing screen — the Mixin-free hub the brief
 * describes. Three buttons:
 *   - LEFT  "Save Current Keybinds": snapshots your current binds → opens a
 *            name + colour screen → saves the whole set to disk.
 *   - RIGHT "Key Presets": opens the list of every saved set; click one to
 *            load/apply it.
 *   - "Edit Key Binds": opens vanilla's real Key Binds screen so you set
 *            keys the normal way, then come back and save them as a set.
 *
 * Reached from the pause menu's "Keybinds" button. Left/right layout mirrors
 * the wording in the brief ("saved keybinds on the left ... key presets is
 * the button beside it on the right").
 */
public class KeybindsHomeScreen extends SpaceTheme.SpaceScreen {
    private final Screen parent;

    public KeybindsHomeScreen(Screen parent) {
        super(Text.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nebulaButtons.clear();
        int y = this.height / 2 - 26;

        // Left: save current binds as a set. Right: browse saved sets.
        nebulaButtons.add(new SpaceTheme.NebulaButton(leftColX(), y, halfWidth(), 22, "Save Current Keybinds",
                () -> this.client.setScreen(new PresetNameScreen(this))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(rightColX(), y, halfWidth(), 22, "Key Presets",
                () -> this.client.setScreen(new KeybindPresetScreen(this))));
        y += 30;

        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, panelWidth(), 22, "Edit Key Binds",
                () -> this.client.setScreen(new KeybindsScreen(this, this.client.options))));

        SpaceTheme.NebulaButton back = new SpaceTheme.NebulaButton(panelX(), this.height - 32, panelWidth(), 22, "Back",
                () -> this.client.setScreen(parent));
        back.bold = true;
        nebulaButtons.add(back);
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Supplied KEY BINDS artwork as the heading, with the helper line
        // beneath it.
        int cx = this.width / 2;
        int logoW = Math.max(120, Math.min(220, (int) (this.width * 0.28f)));
        int logoH = (int) ((long) logoW * NebulaLogos.KEYBINDS_H / NebulaLogos.KEYBINDS_W);
        int logoTop = this.height / 2 - 58 - logoH;
        if (logoTop < 4) logoTop = 4;
        NebulaLogos.drawCentered(ctx, NebulaLogos.KEYBINDS, NebulaLogos.KEYBINDS_W, NebulaLogos.KEYBINDS_H, cx, logoTop, logoW);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                "Set your keys, then save them as a named set", cx, this.height / 2 - 44, 0xFFA79FC4);
    }
}
