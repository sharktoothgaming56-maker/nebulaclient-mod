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
     * OFF by design. Restyling worked by setting the mod's widget to zero
     * alpha and drawing a Nebula button in its place — but widgets that draw
     * themselves with custom code ignore alpha, so Mod Menu's update dot
     * still painted over the Nebula button while its label vanished. Letting
     * each mod render its own button keeps every icon, badge and texture
     * intact, which matters more than matching our styling. The Nebula frame
     * is drawn BEHIND them instead (see install), so they still sit in a
     * consistent panel.
     */
    private static final boolean RESTYLE = false;

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

    /**
     * True for labels that are internal identifiers rather than text meant
     * for a player: "&lt;essential_social&gt;", "modid.button.thing", and so
     * on. Mods that draw their own UI (Essential) register hidden marker
     * widgets like these purely as anchors for their overlay — they were
     * never meant to be seen, and showing them produced a column of
     * nonsense entries that did nothing when clicked.
     */
    private static boolean looksLikePlaceholder(String label) {
        if (label.length() < 2) return true;
        if (label.startsWith("<") && label.endsWith(">")) return true;
        // No spaces plus only identifier characters = a key, not a label.
        return label.matches("[a-z0-9]+([._-][a-z0-9]+)+");
    }

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

            // Anything the mod itself hid or disabled is scenery or an
            // anchor for its own overlay — never a button for us to show.
            if (!widget.visible || !widget.active) continue;

            boolean vanillaClass = widget.getClass().getName().startsWith("net.minecraft.");
            String label = widget.getMessage() == null ? "" : widget.getMessage().getString().trim();

            // Identifier-style text is an internal marker, not a real label,
            // whoever registered it.
            if (!label.isEmpty() && looksLikePlaceholder(label)) continue;

            // With markers filtered out, an unlabelled widget is an icon
            // button. Keep it: it draws its own texture.
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
     * Nebula panel. Each widget renders itself, so its own textures, icons
     * and badges all survive; only its position changes.
     *
     * Icon-only widgets keep their natural size — they're artwork, and
     * forcing them into a text-button shape is what mangled them before.
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
                widget.setAlpha(0f);
                styled.add(new SpaceTheme.NebulaButton(margin, y, width, rowHeight,
                        widget.getMessage().getString(), () -> {}));
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
