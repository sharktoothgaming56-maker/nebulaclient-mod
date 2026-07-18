package com.nebulaclient.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen; // NOTE: same package caveat as NebulaGameMenuScreen — verify against your build.
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Scoped to the buttons that matter (Singleplayer / Multiplayer / Options
 * / Quit) rather than every vanilla title screen element (realms, mod
 * loading warnings, splash text, etc.) — same reasoning as
 * NebulaGameMenuScreen: this replaces a core vanilla screen, so keeping
 * the surface small keeps the risk small.
 */
public class NebulaTitleScreen extends Screen {
    public NebulaTitleScreen() {
        super(Text.literal("NebulaClient"));
    }

    @Override
    protected void init() {
        int y = this.height / 2 - 10;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Singleplayer"), b -> this.client.setScreen(new SelectWorldScreen(this)))
                .dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 24;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Multiplayer"), b -> this.client.setScreen(new MultiplayerScreen(this)))
                .dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 24;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Options..."), b -> this.client.setScreen(new OptionsScreen(this, this.client.options)))
                .dimensions(this.width / 2 - 100, y, 200, 20).build());
        y += 24;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Quit"), b -> this.client.scheduleStop())
                .dimensions(this.width / 2 - 100, y, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Deliberately NOT calling this.renderBackground(...) here — on the
        // title screen specifically, that triggers Minecraft's panorama
        // blur, which is already happening once this frame for the vanilla
        // screen this replaces. Calling it again crashes with
        // "Can only blur once per frame". A plain fill avoids that
        // entirely and doubles as our own branded background anyway.
        ctx.fillGradient(0, 0, this.width, this.height, 0xFF2a1440, 0xFF08050F);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "NebulaClient", this.width / 2, this.height / 4, 0xFF9D6BFF);
        super.render(ctx, mouseX, mouseY, delta);
    }
}
