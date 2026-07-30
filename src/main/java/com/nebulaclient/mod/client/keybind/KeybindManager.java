package com.nebulaclient.mod.client.keybind;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

/**
 * Applies and captures {@link KeybindPreset}s against Minecraft's live
 * keybindings.
 *
 * Design notes:
 *  - "All keybinds" means every binding the game knows about —
 *    client.options.allKeys — so a preset can cover vanilla movement,
 *    attack/use, hotbar, AND NebulaClient's own keys in one shot. That's
 *    what makes a Builder-vs-PvP preset genuinely useful.
 *  - Applying resets every key to UNKNOWN first, then sets the ones the
 *    preset names. That way switching presets never leaves a stale bind
 *    from the previous preset hanging around.
 *  - After mutating binds we call the vanilla persistence + refresh path
 *    (options.write() + KeyBinding.updateKeysByCode()) so the change
 *    survives to options.txt and takes effect immediately, exactly like
 *    rebinding a key in the vanilla Controls screen does.
 */
public final class KeybindManager {
    private KeybindManager() {}

    /** All keybindings the game knows about (vanilla + every mod's). */
    public static KeyBinding[] allKeys() {
        return MinecraftClient.getInstance().options.allKeys;
    }

    /** Snapshot the current binds of every keybinding into the preset. */
    public static void captureInto(KeybindPreset preset) {
        preset.binds().clear();
        for (KeyBinding kb : allKeys()) {
            preset.binds().put(kb.getId(), kb.getBoundKeyTranslationKey());
        }
    }

    /**
     * Make the preset the live configuration: unbind everything, then set
     * exactly what the preset stored, then persist + refresh.
     */
    public static void apply(KeybindPreset preset) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (KeyBinding kb : allKeys()) {
            String stored = preset.binds().get(kb.getId());
            InputUtil.Key key = stored != null
                    ? InputUtil.fromTranslationKey(stored)
                    : InputUtil.UNKNOWN_KEY;
            kb.setBoundKey(key);
        }
        KeyBinding.updateKeysByCode();
        client.options.write();

        NebulaConfig cfg = NebulaConfig.get();
        cfg.activePreset = preset.name;
        cfg.save();
    }

    /** Create a preset from the current live binds and store it in config. */
    public static KeybindPreset createFromCurrent(String name, int color) {
        KeybindPreset preset = new KeybindPreset(name, color);
        captureInto(preset);
        NebulaConfig cfg = NebulaConfig.get();
        cfg.keybindPresets.add(preset);
        cfg.save();
        return preset;
    }

    public static void delete(KeybindPreset preset) {
        NebulaConfig cfg = NebulaConfig.get();
        cfg.keybindPresets.remove(preset);
        if (preset.name != null && preset.name.equals(cfg.activePreset)) cfg.activePreset = null;
        cfg.save();
    }

    /** Re-capture the current binds into an existing preset (an "update this preset" action). */
    public static void updateFromCurrent(KeybindPreset preset) {
        captureInto(preset);
        NebulaConfig.get().save();
    }
}
