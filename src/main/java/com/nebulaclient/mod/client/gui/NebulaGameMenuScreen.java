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
    /** Other mods' pause-menu buttons, re-hosted here (see ModCompat). */
    private final java.util.List<ModCompat.ModButton> modExtras;

    /** Top of the button stack, so the logo can sit above it as it grows. */
    private int stackTop;

    public NebulaGameMenuScreen() {
        this(java.util.List.of());
    }

    public NebulaGameMenuScreen(java.util.List<ModCompat.ModButton> modExtras) {
        super(Text.literal("Game Menu"));
        this.modExtras = modExtras;
    }

    @Override
    protected void init() {
        nebulaButtons.clear();
        int full = panelWidth();
        // Same treatment as the title screen: mod buttons take a row in the
        // stack and the stack grows both ways so it stays centred.
        java.util.List<ModCompat.ModButton> column = ModCompat.columnButtons(modExtras);
        int y = this.height / 2 - 76 - (column.size() * 26) / 2;
        stackTop = y;

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

        for (ModCompat.ModButton mb : column) {
            nebulaButtons.add(ModCompat.fold(mb, panelX(), y, full, 22));
            y += 26;
        }

        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, full, 22, "Disconnect",
                () -> {
                    // This mirrors what vanilla's own Disconnect button does,
                    // in vanilla's order. The step we were missing is the
                    // FIRST one: closing the world's connection. Without it
                    // the teardown stalls, which is why the progress box sat
                    // there with the world still rendering behind it.
                    //
                    // The argument those disconnect* methods take is named
                    // "disconnectionScreen" in the mappings — it's what shows
                    // WHILE tearing down, not where you end up. Choosing the
                    // destination has always been the caller's job, so we set
                    // it right after, exactly like vanilla.
                    boolean singleplayer = this.client.isInSingleplayer();
                    if (this.client.world != null) {
                        this.client.world.disconnect(Text.translatable("menu.returnToMenu"));
                    }
                    com.nebulaclient.mod.client.NebulaClientModClient.beginDisconnect(singleplayer);
                    if (singleplayer) {
                        this.client.disconnectWithSavingScreen();
                    } else {
                        this.client.disconnectWithProgressScreen();
                    }
                    this.client.setScreen(
                            com.nebulaclient.mod.client.NebulaClientModClient.disconnectDestination(singleplayer));
                }));

        // Icon buttons and side panels keep the spot their mod chose.
        ModCompat.placeFree(modExtras, this.width, this.height);
        for (ModCompat.ModButton mb : modExtras) {
            this.addDrawableChild(mb.widget());
        }
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
        int logoTop = stackTop - logoH - 6; // sit just above the "Back to Game" button, following the stack
        if (logoTop < 4) logoTop = 4;
        NebulaLogos.drawCentered(ctx, NebulaLogos.GAME_MENU, NebulaLogos.GAME_MENU_W, NebulaLogos.GAME_MENU_H, cx, logoTop, logoW);
    }
}
