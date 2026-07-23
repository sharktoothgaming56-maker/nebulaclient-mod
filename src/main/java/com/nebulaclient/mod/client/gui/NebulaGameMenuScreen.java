package com.nebulaclient.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen; // NOTE: verify this exact package — try net.minecraft.client.gui.screen.OptionsScreen if it doesn't compile.
import net.minecraft.text.Text;

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

        // Keybinds hub — save/load keybind sets + jump to vanilla Key Binds.
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, full, 22, "Keybinds",
                () -> this.client.setScreen(new KeybindsHomeScreen(this))));
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, full, 22, "Disconnect",
                () -> {
                    // Singleplayer really is saving a world; a server is not —
                    // show "Leaving server" there instead of the misleading
                    // "Saving world" the default saving-screen shows.
                    if (this.client.isInSingleplayer()) {
                        this.client.disconnectWithSavingScreen();
                    } else {
                        this.client.disconnect(new net.minecraft.client.gui.screen.MessageScreen(
                                net.minecraft.text.Text.literal("Leaving server")), false);
                    }
                }));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Supplied GAME MENU logo, centred and placed nicely above the
        // buttons (roughly the same distance the text title used to sit at).
        int cx = this.width / 2;
        int logoW = Math.max(150, Math.min(300, (int) (this.width * 0.34f)));
        int logoH = (int) ((long) logoW * NebulaLogos.GAME_MENU_H / NebulaLogos.GAME_MENU_W);
        int logoTop = this.height / 2 - 76 - logoH - 6; // sit just above the "Back to Game" button
        if (logoTop < 4) logoTop = 4;
        NebulaLogos.drawCentered(ctx, NebulaLogos.GAME_MENU, NebulaLogos.GAME_MENU_W, NebulaLogos.GAME_MENU_H, cx, logoTop, logoW);
    }
}
