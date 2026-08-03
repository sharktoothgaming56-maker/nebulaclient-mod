package com.nebulaclient.mod.client.gui;

import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps other mods' buttons working on our custom Title and Game Menu
 * screens.
 *
 * WHY THIS IS NEEDED: this mod is deliberately Mixin-free, so the only way
 * to show a custom starfield menu is to swap vanilla's screen for our own.
 * But mods like Mod Menu and Essential add their buttons to VANILLA's
 * screen — so discarding it discards them. This class carries them across:
 * before the vanilla screen is thrown away we collect the widgets vanilla
 * didn't create, then re-host those exact widget objects on our screen.
 * They keep their original click actions, so they do whatever their mod
 * intended; we only move and restyle them.
 *
 * HOW A MOD'S BUTTON IS RECOGNISED — two independent rules, so a button
 * only has to satisfy one:
 *   1. Its class isn't a {@code net.minecraft.*} one. Mods that ship custom
 *      widget classes (Essential's, Mod Menu's ModMenuButtonWidget) are
 *      caught here no matter what they're labelled — including icon-only
 *      buttons with no text at all, which the old label-only check dropped.
 *   2. Or it's a vanilla widget class carrying a label that isn't one of
 *      vanilla's own (matched against vanilla's translated labels, so it
 *      works in any language).
 *
 * WHERE WE LOOK: both Fabric's button list AND the screen's children. Those
 * two can genuinely disagree — Mod Menu, for instance, REPLACES the Realms
 * entry via {@code buttons.set(i, ...)}, which updates one and not
 * necessarily the other. Scanning both and de-duplicating means a button
 * has to hide from both lists to be missed.
 */
public final class ModCompat {
    private ModCompat() {}

    /**
     * Draw labelled mod buttons in the Nebula style instead of the vanilla
     * look. The real widget is kept exactly where the styled button is drawn
     * and still handles its own clicks — it's just made fully transparent,
     * so nothing about the mod's behaviour changes.
     *
     * If a future Minecraft build ever ignores widget alpha you'd see a
     * vanilla button drawn on top of a Nebula one. Flip this to false and
     * mod buttons go back to their plain vanilla look, still working.
     */
    private static final boolean RESTYLE = true;

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
        for (String key : keys) out.add(Text.translatable(key).getString().trim());
        return out;
    }

    /** Widgets on {@code screen} that vanilla didn't put there. */
    public static List<ClickableWidget> harvest(Screen screen, boolean pause) {
        // Identity-based set: the same widget can appear in both lists.
        LinkedHashSet<ClickableWidget> candidates = new LinkedHashSet<>();
        try {
            candidates.addAll(Screens.getButtons(screen));
        } catch (RuntimeException ignored) {
            // Never let a compatibility helper take the menu down with it.
        }
        for (Element element : screen.children()) {
            if (element instanceof ClickableWidget widget) candidates.add(widget);
        }

        Set<String> vanillaLabels = labelsOf(pause ? PAUSE_KEYS : TITLE_KEYS);
        List<ClickableWidget> extras = new ArrayList<>();
        for (ClickableWidget widget : candidates) {
            if (widget instanceof TextWidget) continue; // splash/version text, not a control

            boolean vanillaClass = widget.getClass().getName().startsWith("net.minecraft.");
            String label = widget.getMessage() == null ? "" : widget.getMessage().getString().trim();

            if (vanillaClass) {
                // A vanilla widget class: only a non-vanilla label marks it
                // as a mod's. Unlabelled vanilla widgets (the Realms
                // notifier, for one) are left alone so no ghost buttons
                // appear on our menu.
                if (label.isEmpty() || vanillaLabels.contains(label)) continue;
                if (label.contains("Mojang")) continue; // the copyright line is a button too
            }
            extras.add(widget);
        }
        return extras;
    }

    /**
     * Position harvested widgets down the left edge, clear of the centred
     * Nebula panel, and return Nebula-styled stand-ins to draw for the
     * labelled ones.
     *
     * Icon-only widgets keep their own size and appearance — they're
     * artwork, and forcing them into a text-button shape is what made them
     * look broken before.
     *
     * @param panelLeft left edge of our button panel, so we never overlap it
     */
    public static List<SpaceTheme.NebulaButton> install(List<ClickableWidget> extras, int panelLeft, int screenHeight) {
        List<SpaceTheme.NebulaButton> styled = new ArrayList<>();
        if (extras.isEmpty()) return styled;

        List<ClickableWidget> labelled = new ArrayList<>();
        List<ClickableWidget> icons = new ArrayList<>();
        for (ClickableWidget widget : extras) {
            boolean hasLabel = widget.getMessage() != null && !widget.getMessage().getString().trim().isEmpty();
            (hasLabel ? labelled : icons).add(widget);
        }

        int margin = 8;
        int width = Math.max(60, Math.min(110, panelLeft - margin * 2));
        int rowHeight = 20;
        int gap = 4;

        int total = labelled.size() * (rowHeight + gap);
        int y = Math.max(margin, (screenHeight - total) / 2);
        for (ClickableWidget widget : labelled) {
            widget.setDimensionsAndPosition(width, rowHeight, margin, y);
            if (RESTYLE) {
                // Invisible but still clickable: vanilla's dispatch hands the
                // click to this widget exactly as before, while the Nebula
                // button below is what the player actually sees. Note it must
                // stay visible=true — an invisible widget receives no input.
                widget.setAlpha(0f);
                SpaceTheme.NebulaButton stand = new SpaceTheme.NebulaButton(
                        margin, y, width, rowHeight,
                        widget.getMessage().getString(),
                        () -> {}); // the real widget under it does the work
                styled.add(stand);
            }
            y += rowHeight + gap;
        }

        // Icon buttons: a wrapped row underneath, at their natural size.
        int iconX = margin;
        int iconY = y + 4;
        int tallest = 0;
        for (ClickableWidget widget : icons) {
            int w = Math.max(1, widget.getWidth());
            int h = Math.max(1, widget.getHeight());
            if (iconX > margin && iconX + w > margin + width) { // wrap
                iconX = margin;
                iconY += tallest + gap;
                tallest = 0;
            }
            widget.setPosition(iconX, iconY);
            iconX += w + gap;
            tallest = Math.max(tallest, h);
        }
        return styled;
    }
}
