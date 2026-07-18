package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Bottom-right HUD: the four armor slots, held item, and offhand item.
 * No big background panel — just a subtle square behind each icon,
 * matching the "no boxes, like Lunar" feedback. Each shows a durability
 * bar + small number when damaged, and a stack count when more than 1.
 */
public class ArmorHud {
    public static final String ID = "armor";
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!HudStyle.shouldRenderHuds() || !NebulaConfig.get().isHudEnabled(ID)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        var player = client.player;

        int slotSize = 20;
        int gap = 3;
        int slotCount = 6; // 4 armor + mainhand + offhand
        int width = slotCount * slotSize + (slotCount - 1) * gap;
        int[] pos = NebulaConfig.get().getHudPosition(
                ID,
                client.getWindow().getScaledWidth() - width - 6,
                client.getWindow().getScaledHeight() - slotSize - 24
        );

        int x = pos[0];
        int y = pos[1];
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            drawSlot(ctx, client, player.getEquippedStack(slot), x, y, slotSize);
            x += slotSize + gap;
        }
        drawSlot(ctx, client, player.getMainHandStack(), x, y, slotSize);
        x += slotSize + gap;
        drawSlot(ctx, client, player.getOffHandStack(), x, y, slotSize);
    }

    private static void drawSlot(DrawContext ctx, MinecraftClient client, ItemStack stack, int x, int y, int size) {
        ctx.fill(x, y, x + size, y + size, 0x30000000); // subtle backing, not a full panel
        if (stack.isEmpty()) return;

        boolean enchanted = !stack.getEnchantments().isEmpty();
        if (enchanted) {
            int glow = 0xFFB98BFF; // --nebula-bright — a thin border stands in for vanilla's shimmer shader
            ctx.fill(x - 1, y - 1, x + size + 1, y, glow);
            ctx.fill(x - 1, y + size, x + size + 1, y + size + 1, glow);
            ctx.fill(x - 1, y - 1, x, y + size + 1, glow);
            ctx.fill(x + size, y - 1, x + size + 1, y + size + 1, glow);
        }

        ctx.drawItem(stack, x + (size - 16) / 2, y + (size - 16) / 2);

        if (stack.getCount() > 1) {
            String countText = String.valueOf(stack.getCount());
            int tx = x + size - client.textRenderer.getWidth(countText);
            ctx.drawTextWithShadow(client.textRenderer, countText, tx, y + size - 8, HudStyle.TEXT);
        }

        if (stack.isDamageable() && stack.getDamage() > 0) {
            int remaining = stack.getMaxDamage() - stack.getDamage();
            float healthFrac = (float) remaining / stack.getMaxDamage();
            int barWidth = size - 2;
            int filled = Math.round(barWidth * healthFrac);
            int color = healthFrac > 0.5f ? 0xFF5EEB8F : (healthFrac > 0.2f ? 0xFFE3AC45 : 0xFFE0625A);
            ctx.fill(x + 1, y + size + 1, x + 1 + barWidth, y + size + 2, 0xFF000000);
            ctx.fill(x + 1, y + size + 1, x + 1 + filled, y + size + 2, color);

            // Small durability number below the bar, only when actually damaged.
            String durText = String.valueOf(remaining);
            int dtx = x + (size - client.textRenderer.getWidth(durText)) / 2;
            ctx.drawText(client.textRenderer, durText, dtx, y + size + 3, color, false); // no shadow — keeps it tiny/subtle
        }
    }
}
