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
    public NebulaTitleScreen() {
        super(Text.literal("NebulaClient"));
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
        int y = this.height / 2 - 25;

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
        nebulaButtons.add(new SpaceTheme.NebulaButton(leftColX(), y, halfWidth(), 22, "Options...",
                () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(rightColX(), y, halfWidth(), 22, "Nebula Menu",
                () -> this.client.setScreen(new NebulaMenuScreen(this))));
        y += 26;
        nebulaButtons.add(new SpaceTheme.NebulaButton(panelX(), y, w, 22, "Quit",
                () -> this.client.scheduleStop()));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2;
        int ty = this.height / 4;
        // Soft halo, then the supplied NEBULA CLIENT logo image. Logo width
        // scales with the window but is clamped so it never dominates small
        // screens.
        SpaceTheme.drawGlowDisc(ctx, cx, ty + 10, 34, 157, 107, 255, 0.18f);
        int logoW = Math.max(180, Math.min(360, (int) (this.width * 0.42f)));
        int logoH = (int) ((long) logoW * NebulaLogos.TITLE_H / NebulaLogos.TITLE_W);
        int logoTop = ty - logoH / 2;
        NebulaLogos.drawCentered(ctx, NebulaLogos.TITLE, NebulaLogos.TITLE_W, NebulaLogos.TITLE_H, cx, logoTop, logoW);
    }
}
