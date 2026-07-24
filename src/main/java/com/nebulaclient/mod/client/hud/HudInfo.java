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
            new HudInfo(FpsHud.ID, "FPS", 60, 12, (c, wh) -> new int[]{6, 6}),
            new HudInfo(CoordsHud.ID, "Coordinates", 130, 12, (c, wh) -> new int[]{6, 20}),
            new HudInfo(DirectionHud.ID, "Direction", 80, 12, (c, wh) -> new int[]{6, 34}),
            new HudInfo(CpsHud.ID, "CPS", 80, 12, (c, wh) -> new int[]{6, 48}),
            new HudInfo(StatusHud.ID, "Status", 120, 34, (c, wh) -> new int[]{c.getWindow().getScaledWidth() - wh[0] - 6, 6}),
            new HudInfo(ArmorHud.ID, "Armor & Held Items", 60, 120, (c, wh) -> new int[]{c.getWindow().getScaledWidth() - wh[0] - 6, c.getWindow().getScaledHeight() / 2 - wh[1] / 2}),
            new HudInfo(EffectsHud.ID, "Potion Effects", 150, 46, (c, wh) -> new int[]{6, c.getWindow().getScaledHeight() / 2 - wh[1] / 2})
    );
}
