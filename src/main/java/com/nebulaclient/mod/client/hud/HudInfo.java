package com.nebulaclient.mod.client.hud;

import net.minecraft.client.MinecraftClient;

import java.util.List;
import java.util.function.BiFunction;

/** One entry per HUD, so the editor/toggle screens don't need to hardcode each one by hand. */
public class HudInfo {
    public final String id;
    public final String label;
    public final int width;
    public final int height;
    public final BiFunction<MinecraftClient, int[], int[]> defaultPos; // (client, [w,h]) -> [x, y]

    public HudInfo(String id, String label, int width, int height, BiFunction<MinecraftClient, int[], int[]> defaultPos) {
        this.id = id;
        this.label = label;
        this.width = width;
        this.height = height;
        this.defaultPos = defaultPos;
    }

    public static final List<HudInfo> ALL = List.of(
            new HudInfo(StatusHud.ID, "Status", 120, 30,
                    (c, wh) -> new int[]{c.getWindow().getScaledWidth() - wh[0] - 6, 6}),
            new HudInfo(ArmorHud.ID, "Armor & Held Items", 138, 24,
                    (c, wh) -> new int[]{c.getWindow().getScaledWidth() - wh[0] - 6, c.getWindow().getScaledHeight() - wh[1] - 24}),
            new HudInfo(InfoHud.ID, "FPS / Coords / Facing", 130, 30,
                    (c, wh) -> new int[]{6, 6}),
            new HudInfo(EffectsHud.ID, "Potion Effects", 150, 30,
                    (c, wh) -> new int[]{6, c.getWindow().getScaledHeight() / 2 - wh[1] / 2})
    );
}
