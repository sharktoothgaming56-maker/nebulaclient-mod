package com.nebulaclient.mod.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps other mods' buttons working on our custom Title and Game Menu
 * screens.
 *
 * THE PROBLEM: mods like Mod Menu and Essential add their buttons to
 * VANILLA's TitleScreen / GameMenuScreen from their own screen-init
 * handlers. We used to throw that vanilla screen away and show ours
 * instead, so every one of those buttons vanished — and because handler
 * order between mods isn't defined, it wasn't even consistent about it.
 *
 * THE FIX: before discarding the vanilla screen, collect the widgets that
 * vanilla itself didn't create and re-host them on our screen. They're the
 * same widget objects with their original click actions, so they keep
 * working exactly as their mod intended; we only move them. The swap is
 * also deferred by one frame (see NebulaClientModClient) so every other
 * mod has had its turn to add things before we look.
 *
 * HOW "not vanilla" is decided: vanilla's own buttons are matched by their
 * translated labels, so it works in any language. Anything else is treated
 * as a mod's. That's a heuristic, not magic — a mod that labels its button
 * exactly "Options..." would be mistaken for vanilla's and dropped. If a
 * button ever goes missing, that's the first thing to check.
 */
public final class ModCompat {
    private ModCompat() {}

    /** Vanilla title screen button labels. */
    private static final String[] TITLE_KEYS = {
            "menu.singleplayer", "menu.multiplayer", "menu.online", "menu.options", "menu.quit",
            "menu.playdemo", "menu.resetdemo", "narrator.button.language", "narrator.button.accessibility",
            "title.credits",
    };

    /** Vanilla pause screen button labels. */
    private static final String[] PAUSE_KEYS = {
            "menu.returnToGame", "gui.advancements", "gui.stats", "menu.sendFeedback", "menu.reportBugs",
            "menu.options", "menu.shareToLan", "menu.playerReporting", "menu.returnToMenu",
            "menu.disconnect", "menu.server_links", "menu.quit", "menu.paused", "menu.game",
    };

    private static Set<String> labelsOf(String[] keys) {
        Set<String> out = new HashSet<>();
        for (String key : keys) {
            out.add(Text.translatable(key).getString().trim());
        }
        return out;
    }

    /**
     * Widgets on {@code screen} that vanilla didn't put there.
     *
     * @param pause true for the pause menu, false for the title screen
     */
    public static List<ClickableWidget> harvest(Screen screen, boolean pause) {
        List<ClickableWidget> extras = new ArrayList<>();
        Set<String> vanilla = labelsOf(pause ? PAUSE_KEYS : TITLE_KEYS);
        for (ClickableWidget widget : Screens.getButtons(screen)) {
            // Plain labels (splash text, version strings) aren't controls.
            if (widget instanceof TextWidget) continue;

            String label = widget.getMessage() == null ? "" : widget.getMessage().getString().trim();

            // Icon-only buttons have no label to match on, so they'd be
            // indistinguishable from vanilla's; skipping them keeps ghost
            // buttons off the menu. (Mods using icon-only buttons — some of
            // Essential's — won't be picked up. Known limitation.)
            if (label.isEmpty()) continue;
            if (vanilla.contains(label)) continue;
            // Vanilla's copyright line is a button, but not a menu control.
            if (label.contains("Mojang")) continue;

            extras.add(widget);
        }
        return extras;
    }

    /**
     * Lay harvested buttons out as a column down the left edge, clear of the
     * centred Nebula panel. Returns them ready to be added as children.
     *
     * @param panelLeft left edge of our button panel, so we never overlap it
     */
    public static void layout(List<ClickableWidget> extras, int panelLeft, int screenHeight) {
        if (extras.isEmpty()) return;
        int margin = 8;
        int width = Math.max(50, Math.min(110, panelLeft - margin * 2));
        int height = 20;
        int gap = 4;
        int total = extras.size() * height + (extras.size() - 1) * gap;
        // Centred vertically against the panel, but never off the top.
        int y = Math.max(margin, (screenHeight - total) / 2);
        for (ClickableWidget widget : extras) {
            widget.setDimensionsAndPosition(width, height, margin, y);
            y += height + gap;
        }
    }
}
