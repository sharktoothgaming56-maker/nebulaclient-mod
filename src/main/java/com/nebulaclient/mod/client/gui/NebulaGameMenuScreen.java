package com.nebulaclient.mod.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen; // NOTE: verify this exact package — try net.minecraft.client.gui.screen.OptionsScreen if it doesn't compile.
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class NebulaGameMenuScreen extends SpaceTheme.SpaceScreen {
    public NebulaGameMenuScreen() {
        super(Text.literal("Game Menu"));
    }

    @Override
    protected void init() {
        nebulaButtons.clear();
        int col = 98, full = 200;
        int y = this.height / 2 - 64;

        SpaceTheme.NebulaButton back = new SpaceTheme.NebulaButton(this.width / 2 - 100, y, full, 22, "Back to Game", () -> this.client.setScreen(null));
        back.bold = true;
        nebulaButtons.add(back);
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 - 100, y, col, 22, "Advancements",
                () -> this.client.setScreen(new AdvancementsScreen(this.client.player.networkHandler.getAdvancementHandler(), this))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 + 2, y, col, 22, "Statistics",
                () -> this.client.setScreen(new StatsScreen(this, this.client.player.getStatHandler()))));
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 - 100, y, full, 22, "Multiplayer",
                () -> this.client.setScreen(new MultiplayerScreen(this))));
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 - 100, y, col, 22, "Options...",
                () -> this.client.setScreen(new OptionsScreen(this, this.client.options))));
        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 + 2, y, col, 22, "Nebula Menu",
                () -> this.client.setScreen(new NebulaMenuScreen(this))));
        y += 26;

        nebulaButtons.add(new SpaceTheme.NebulaButton(this.width / 2 - 100, y, full, 22, "Disconnect",
                () -> this.client.disconnectWithSavingScreen()));
    }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("GAME MENU").formatted(Formatting.BOLD), this.width / 2, this.height / 2 - 92, 0xFFB98BFF);
    }
}
