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

        // Keybinds — straight to the vanilla Key Binds page (which now has
        // the Save Preset / Presets buttons in its header).
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, full, 22, "Keybinds",
                () -> this.client.setScreen(new net.minecraft.client.gui.screen.option.KeybindsScreen(this, this.client.options))));
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, full, 22, "Disconnect",
                () -> {
                    // The disconnect* calls below only START an asynchronous
                    // teardown and show their own "Saving world" progress
                    // screen — none of them decide where you land afterwards,
                    // and setting a screen on the next line just races the
                    // teardown (that's the bug where it sat on the progress
                    // screen forever). So we arm a watchdog that waits for
                    // the world to actually be gone and then routes to the
                    // menu, standing down if vanilla gets there first.
                    boolean singleplayer = this.client.isInSingleplayer();
                    com.nebulaclient.mod.client.NebulaClientModClient.beginDisconnect(!singleplayer);
                    if (singleplayer) {
                        this.client.disconnectWithSavingScreen();
                    } else {
                        this.client.disconnectWithProgressScreen();
                    }
                }));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Supplied GAME MENU logo, centred and placed nicely above the
        // buttons (roughly the same distance the text title used to sit at).
        int cx = this.width / 2;
        int logoW = (int) (this.width * 0.34f);
        int maxByHeight = (int) (this.height * 0.16f * NebulaLogos.GAME_MENU_W / (float) NebulaLogos.GAME_MENU_H);
        logoW = Math.max(40, Math.min(logoW, maxByHeight));
        int logoH = (int) ((long) logoW * NebulaLogos.GAME_MENU_H / NebulaLogos.GAME_MENU_W);
        int logoTop = this.height / 2 - 76 - logoH - 6; // sit just above the "Back to Game" button
        if (logoTop < 4) logoTop = 4;
        NebulaLogos.drawCentered(ctx, NebulaLogos.GAME_MENU, NebulaLogos.GAME_MENU_W, NebulaLogos.GAME_MENU_H, cx, logoTop, logoW);
    }
}
