package com.nebulaclient.mod.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nebulaclient.mod.client.NebulaClientModClient;
import com.nebulaclient.mod.client.keybind.KeybindPreset;
import com.nebulaclient.mod.client.waypoint.Waypoint;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything NebulaClient remembers between sessions: which HUDs are on,
 * where you dragged them to, each HUD's colour, your waypoints, the shared
 * button colour, and your saved keybind presets. One JSON file at
 * config/nebulaclient.json — simple on purpose, no need for a database for
 * data this small.
 */
public class NebulaConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("nebulaclient.json");

    /** Default accent used everywhere the player hasn't picked something else (the "nebula" purple). */
    public static final int DEFAULT_ACCENT = 0xFFB98BFF;
    /** Default HUD text colour (the launcher's --text). */
    public static final int DEFAULT_HUD_TEXT = 0xFFEAE6F7;

    public Map<String, Boolean> hudEnabled = new HashMap<>();
    public Map<String, int[]> hudPosition = new HashMap<>(); // [x, y] offset from the HUD's default anchor corner
    public Map<String, Float> hudScale = new HashMap<>();    // per-HUD resize factor, 1.0 = default
    public Map<String, Integer> hudColor = new HashMap<>();  // per-HUD ARGB text/accent colour
    public List<Waypoint> waypoints = new ArrayList<>();
    public boolean deathWaypointsEnabled = false; // "auto off" per the brief — starts disabled, you opt in

    /** Fill/gradient tint for every NebulaButton across all menus. 0 = "unset", falls back to DEFAULT_ACCENT. */
    public int buttonColor = 0;
    /** Stroke (border/rounding) colour for every NebulaButton. 0 = "unset", falls back to DEFAULT_ACCENT. */
    public int strokeColor = 0;

    /** Bottom-left in-game wordmark scale (0 = unset -> default 0.5). */
    public float logoScale = 0f;

    /**
     * The player's chosen vanilla GUI Scale, mirrored here so NebulaClient
     * can restore it locally if it ever gets reset between launches.
     * -1 = "not captured yet" -> adopt whatever vanilla currently has.
     */
    public int guiScale = -1;

    /** Saved keybind sets (each a full snapshot of the player's binds, named + coloured). */
    public List<KeybindPreset> keybindPresets = new ArrayList<>();
    /** Name of the set currently applied, or null if none. */
    public String activePreset = null;

    /**
     * Server list customisation, keyed by server address (not index) so it
     * survives the player editing, adding or removing entries in vanilla's
     * own list.
     */
    public List<String> favoriteServers = new ArrayList<>();
    /** Addresses in the player's hand-dragged order; anything unlisted falls back to vanilla order. */
    public List<String> serverOrder = new ArrayList<>();
    /** Whether the server list is filtered to favourites only (persists between launches). */
    public boolean showFavoritesOnly = false;

    public boolean isFavoriteServer(String address) {
        return address != null && favoriteServers.contains(address);
    }

    public void toggleFavoriteServer(String address) {
        if (address == null) return;
        if (!favoriteServers.remove(address)) favoriteServers.add(address);
        save();
    }

    private static NebulaConfig instance;

    public static NebulaConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static NebulaConfig load() {
        if (Files.exists(FILE)) {
            try {
                NebulaConfig cfg = GSON.fromJson(Files.readString(FILE), NebulaConfig.class);
                if (cfg != null) {
                    cfg.repairNulls();
                    return cfg;
                }
            } catch (Exception e) {
                NebulaClientModClient.LOGGER.warn("Couldn't read nebulaclient.json, starting fresh: {}", e.getMessage());
            }
        }
        return new NebulaConfig();
    }

    /** Gson leaves omitted collections null when loading an older file — put empties back so nothing NPEs. */
    private void repairNulls() {
        if (hudEnabled == null) hudEnabled = new HashMap<>();
        if (hudPosition == null) hudPosition = new HashMap<>();
        if (hudScale == null) hudScale = new HashMap<>();
        if (hudColor == null) hudColor = new HashMap<>();
        if (waypoints == null) waypoints = new ArrayList<>();
        if (keybindPresets == null) keybindPresets = new ArrayList<>();
    }

    public void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this));
        } catch (IOException e) {
            NebulaClientModClient.LOGGER.warn("Couldn't save nebulaclient.json: {}", e.getMessage());
        }
    }

    public boolean isHudEnabled(String hudId) {
        return hudEnabled.getOrDefault(hudId, true); // every HUD defaults to visible
    }

    public void setHudEnabled(String hudId, boolean enabled) {
        hudEnabled.put(hudId, enabled);
        save();
    }

    public int[] getHudPosition(String hudId, int defaultX, int defaultY) {
        return hudPosition.getOrDefault(hudId, new int[]{defaultX, defaultY});
    }

    public void setHudPosition(String hudId, int x, int y) {
        hudPosition.put(hudId, new int[]{x, y});
        save();
    }

    public float getHudScale(String hudId) {
        return hudScale.getOrDefault(hudId, 1.0f);
    }

    public void setHudScale(String hudId, float scale) {
        hudScale.put(hudId, Math.max(0.5f, Math.min(2.0f, scale)));
        save();
    }

    /** Per-HUD colour, falling back to the given default when the player hasn't set one. */
    public int getHudColor(String hudId, int fallback) {
        return hudColor.getOrDefault(hudId, fallback);
    }

    public void setHudColor(String hudId, int argb) {
        hudColor.put(hudId, argb);
        save();
    }

    /** The shared button colour, or DEFAULT_ACCENT when unset. */
    public int getButtonColor() {
        return buttonColor == 0 ? DEFAULT_ACCENT : buttonColor;
    }

    public void setButtonColor(int argb) {
        buttonColor = argb;
        save();
    }

    /** The shared stroke (border) colour, or DEFAULT_ACCENT when unset. */
    public int getStrokeColor() {
        return strokeColor == 0 ? DEFAULT_ACCENT : strokeColor;
    }

    public void setStrokeColor(int argb) {
        strokeColor = argb;
        save();
    }

    /** In-game wordmark scale, clamped to a small readable range; 0.5 default. */
    public float getLogoScale() {
        return logoScale <= 0f ? 0.5f : logoScale;
    }

    public void setLogoScale(float scale) {
        logoScale = Math.max(0.25f, Math.min(1.5f, scale));
        save();
    }

    /** Remembered vanilla GUI Scale, or -1 if we haven't captured one yet. */
    public int getGuiScale() {
        return guiScale;
    }

    /** Persist the vanilla GUI Scale locally so it survives across launches. */
    public void setGuiScale(int scale) {
        if (scale != guiScale) {
            guiScale = scale;
            save();
        }
    }
}
