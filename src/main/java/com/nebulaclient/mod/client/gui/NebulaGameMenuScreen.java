package com.nebulaclient.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen; // NOTE: verify this exact package — try net.minecraft.client.gui.screen.OptionsScreen if it doesn't compile.
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Custom pause menu. Buttons size off the responsive panel helpers so they
 * grow/shrink with the window instead of a fixed 200px, matching the main
 * menu's look. Adds a Keybinds button that jumps straight to the vanilla
 * Key Binds screen (returning here on Done), next to the Nebula Menu.
 */
public class NebulaGameMenuScreen extends SpaceTheme.SpaceScreen {
    public NebulaGameMenuScreen() {
        super(Text.literal("Game Menu"));
    }

    @Override
    protected void init() {
        nebulaButtons.clear();
        int full = panelWidth();
        int y = this.height / 2 - 76;

        SpaceTheme.NebulaButton back = new SpaceTheme.NebulaButton(panelX(), y, full, 22, "Back to Game", () -> this.client.setScreen(null));
        back.bold = true;
        nebulaButtons.add(back);
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(leftColX(), y, halfWidth(), 22, "Advancements",
                () -> this.client.setScreen(new AdvancementsScreen(this.client.player.networkHandler.getAdvancementHandler(), this))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(rightColX(), y, halfWidth(), 22, "Statistics",
                () -> this.client.setScreen(new StatsScreen(this, this.client.player.getStatHandler()))));
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, full, 22, "Multiplayer",
                () -> this.client.setScreen(new MultiplayerScreen(this))));
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(leftColX(), y, halfWidth(), 22, "Options...",
                () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(rightColX(), y, halfWidth(), 22, "Nebula Menu",
                () -> this.client.setScreen(new NebulaMenuScreen(this))));
        y += 26;

        // Keybinds shortcut — straight to vanilla's Key Binds screen, comes
        // back here on Done. Sits next to a quick jump to Keybind Presets.
        nebulaButtons.add(new SpaceTheme.NebulaButton(leftColX(), y, halfWidth(), 22, "Keybinds",
                () -> this.client.setScreen(new KeybindsScreen(this, this.client.options))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(rightColX(), y, halfWidth(), 22, "Keybind Presets",
                () -> this.client.setScreen(new KeybindPresetScreen(this))));
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, full, 22, "Disconnect",
                () -> this.client.disconnectWithSavingScreen()));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int accent = 0xFF000000 | (com.nebulaclient.mod.client.config.NebulaConfig.get().getButtonColor() & 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("GAME MENU").formatted(Formatting.BOLD),
                this.width / 2, this.height / 2 - 104, accent);
    }
}
