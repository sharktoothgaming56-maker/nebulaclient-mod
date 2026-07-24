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
        long time = System.currentTimeMillis();

        // Soft halo that breathes slowly, then the supplied NEBULA CLIENT
        // logo drifting on a gentle 2px float, like it is hanging in orbit.
        // Logo width scales with the window but is clamped so it never
        // dominates small screens.
        float pulse = 0.16f + 0.06f * (float) Math.sin(time / 1600.0);
        SpaceTheme.drawGlowDisc(ctx, cx, ty + 6, 40, 157, 107, 255, pulse);
        int logoW = Math.max(180, Math.min(360, (int) (this.width * 0.42f)));
        int logoH = (int) ((long) logoW * NebulaLogos.TITLE_H / NebulaLogos.TITLE_W);
        int bob = (int) Math.round(2.0 * Math.sin(time / 900.0));
        int logoTop = ty - logoH / 2 + bob;
        NebulaLogos.drawCentered(ctx, NebulaLogos.TITLE, NebulaLogos.TITLE_W, NebulaLogos.TITLE_H, cx, logoTop, logoW);

        // Signature accent rule between the logo and the buttons.
        int accent = com.nebulaclient.mod.client.config.NebulaConfig.get().getStrokeColor();
        SpaceTheme.drawHeaderAccent(ctx, this.textRenderer, cx, logoTop + logoH + 8, logoW / 3, accent);

        // Small version line, bottom-left, out of the way.
        ctx.drawTextWithShadow(this.textRenderer, "Minecraft 1.21.11", 4, this.height - 22, 0x80EAE6F7);
        ctx.drawTextWithShadow(this.textRenderer, "NebulaClient", 4, this.height - 12, 0x80EAE6F7);
    }
}
