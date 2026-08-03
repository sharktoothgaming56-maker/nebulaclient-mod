package com.nebulaclient.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen; // NOTE: same package caveat as NebulaGameMenuScreen — verify against your build.
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

/**
 * Custom main menu. Buttons size off the responsive panel helpers so they
 * grow/shrink with the window, matching the pause menu. Adds a Nebula Menu
 * button so the client's settings are reachable straight from the title
 * screen (opened with parent = this title screen so Back returns here).
 */
public class NebulaTitleScreen extends SpaceTheme.SpaceScreen {
    /** Other mods' title-screen buttons, re-hosted here (see ModCompat). */
    private final java.util.List<ModCompat.ModButton> modExtras;

    public NebulaTitleScreen() {
        this(java.util.List.of());
    }

    public NebulaTitleScreen(java.util.List<ModCompat.ModButton> modExtras) {
        super(Text.literal("NebulaClient"));
        this.modExtras = modExtras;
    }

    @Override
    protected boolean opaqueBackground() {
        return true; // full starfield — nothing behind the title screen to show through
    }

    @Override
    protected boolean showWordmark() {
        return false; // the big logo IS the branding here, no corner mark needed
    }

    @Override
    protected void init() {
        nebulaButtons.clear();
        int w = panelWidth();

        // Mod buttons meant for the menu column (Mod Menu's "Mods" is the
        // classic case) get a row of their own here, in the same slot
        // vanilla gives them: straight after Multiplayer, where Realms sits.
        // The whole stack then grows upward as well as downward, so it stays
        // centred instead of sliding down the screen — the same courtesy
        // vanilla extends when Mod Menu pushes its buttons apart.
        java.util.List<ModCompat.ModButton> column = ModCompat.columnButtons(modExtras);
        int y = this.height / 2 - 25 - (column.size() * 26) / 2;

        SpaceTheme.NebulaButton single = new SpaceTheme.NebulaButton(panelX(), y, w, 22, "Singleplayer",
                () -> this.client.setScreen(new SelectWorldScreen(this)));
        single.bold = true;
        nebulaButtons.add(single);
        y += 26;
        SpaceTheme.NebulaButton multi = new SpaceTheme.NebulaButton(panelX(), y, w, 22, "Multiplayer",
                () -> this.client.setScreen(new MultiplayerScreen(this)));
        multi.bold = true;
        nebulaButtons.add(multi);
        y += 26;

        for (ModCompat.ModButton mb : column) {
            nebulaButtons.add(ModCompat.fold(mb, panelX(), y, w, 22));
            y += 26;
        }

        nebulaButtons.add(new SpaceTheme.NebulaButton(leftColX(), y, halfWidth(), 22, "Options...",
                () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(rightColX(), y, halfWidth(), 22, "Nebula Menu",
                () -> this.client.setScreen(new NebulaMenuScreen(this))));
        y += 26;
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, w, 22, "Quit",
                () -> this.client.scheduleStop()));

        // Icon buttons and side panels keep the spot their mod chose.
        ModCompat.placeFree(modExtras, this.width, this.height);
        for (ModCompat.ModButton mb : modExtras) {
            this.addDrawableChild(mb.widget());
        }
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        int ty = this.height / 4;
        // Soft halo, then the supplied NEBULA CLIENT logo image. Pure
        // percentage sizing (capped only by a height share) so the layout
        // keeps the same proportions whether the window is fullscreen,
        // half the desktop, or a small floating box — everything just
        // scales together. The halo scales with the logo for the same
        // reason.
        int logoW = (int) (this.width * 0.42f);
        int maxByHeight = (int) (this.height * 0.24f * NebulaLogos.TITLE_W / (float) NebulaLogos.TITLE_H);
        logoW = Math.max(40, Math.min(logoW, maxByHeight));
        SpaceTheme.drawGlowDisc(ctx, cx, ty + 10, Math.max(16, (int) (logoW * 0.12f)), 157, 107, 255, 0.18f);
        int logoH = (int) ((long) logoW * NebulaLogos.TITLE_H / NebulaLogos.TITLE_W);
        int logoTop = ty - logoH / 2;
        NebulaLogos.drawCentered(ctx, NebulaLogos.TITLE, NebulaLogos.TITLE_W, NebulaLogos.TITLE_H, cx, logoTop, logoW);
    }
}
