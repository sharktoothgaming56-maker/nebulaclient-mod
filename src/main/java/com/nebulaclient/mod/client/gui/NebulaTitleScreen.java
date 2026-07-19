package com.nebulaclient.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen; // NOTE: same package caveat as NebulaGameMenuScreen — verify against your build.
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NebulaTitleScreen extends SpaceTheme.SpaceScreen {
    public NebulaTitleScreen() {
        super(Text.literal("NebulaClient"));
    }

    @Override
    protected boolean opaqueBackground() {
        return true; // full starfield — nothing behind the title screen to show through
    }

    @Override
    protected void init() {
        nebulaButtons.clear();
        int y = this.height / 2 - 12;

        SpaceTheme.NebulaButton single = new SpaceTheme.NebulaButton(this.width / 2 - 100, y, 200, 22, "Singleplayer",
                () -> this.client.setScreen(new SelectWorldScreen(this)));
        single.bold = true;
        nebulaButtons.add(single);
        y += 26;
        SpaceTheme.NebulaButton multi = new SpaceTheme.NebulaButton(this.width / 2 - 100, y, 200, 22, "Multiplayer",
                () -> this.client.setScreen(new MultiplayerScreen(this)));
        multi.bold = true;
        nebulaButtons.add(multi);
        y += 26;
        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 - 100, y, 200, 22, "Options...",
                () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        y += 26;
        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 - 100, y, 200, 22, "Quit",
                () -> this.client.scheduleStop()));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Big glowing wordmark: soft halo disc behind bold text.
        int cx = this.width / 2;
        int ty = this.height / 4;
        SpaceTheme.drawGlowDisc(ctx, cx, ty + 4, 26, 157, 107, 255, 0.18f);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("NEBULACLIENT").formatted(Formatting.BOLD), cx, ty, 0xFFB98BFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "beyond the stars", cx, ty + 14, 0xFF6F6690);
    }
}
