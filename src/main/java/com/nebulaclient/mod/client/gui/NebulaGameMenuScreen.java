package com.nebulaclient.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen; // NOTE: verify this exact package — try net.minecraft.client.gui.screen.OptionsScreen if it doesn't compile.
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Stands in for vanilla's pause menu with the full vanilla button set
 * (per feedback — the earlier scoped-down 4-button version looked too
 * bare) plus a Nebula Menu button next to Options.
 */
public class NebulaGameMenuScreen extends Screen {
    public NebulaGameMenuScreen() {
        super(Text.literal("Game Menu"));
    }

    @Override
    protected void init() {
        int colWidth = 98;
        int fullWidth = 200;
        int y = this.height / 2 - 64;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back to Game"), b -> this.client.setScreen(null))
                .dimensions(this.width / 2 - 100, y, fullWidth, 20).build());
        y += 24;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Advancements"),
                b -> this.client.setScreen(new AdvancementsScreen(this.client.player.networkHandler.getAdvancementHandler(), this)))
                .dimensions(this.width / 2 - 100, y, colWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Statistics"),
                b -> this.client.setScreen(new StatsScreen(this, this.client.player.getStatHandler())))
                .dimensions(this.width / 2 + 2, y, colWidth, 20).build());
        y += 24;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Multiplayer"), b -> this.client.setScreen(new MultiplayerScreen(this)))
                .dimensions(this.width / 2 - 100, y, fullWidth, 20).build());
        y += 24;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Options..."), b -> this.client.setScreen(new OptionsScreen(this, this.client.options)))
                .dimensions(this.width / 2 - 100, y, colWidth, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Nebula Menu"), b -> this.client.setScreen(new NebulaMenuScreen(this)))
                .dimensions(this.width / 2 + 2, y, colWidth, 20).build());
        y += 24;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Disconnect"), b -> this.client.disconnectWithSavingScreen())
                .dimensions(this.width / 2 - 100, y, fullWidth, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0xE02a1440, 0xF008050F);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "Game Menu", this.width / 2, this.height / 2 - 90, 0xFFB98BFF);
        super.render(ctx, mouseX, mouseY, delta);
    }
}
