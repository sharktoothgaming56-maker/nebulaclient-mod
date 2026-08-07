package com.nebulaclient.mod.client.hud;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.social.NebulaLink;
import com.nebulaclient.mod.client.social.PartyState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * THE SIDE PANEL — your party and friends down the whole right edge,
 * in game.
 *
 * This is not a button that opens a thing. It IS the thing. It runs the
 * full height of the screen so the information is simply present while
 * you play, the way a party list is present in a modern launcher rather
 * than buried a click deep.
 *
 * Drawn as a HUD element (so it can be toggled and moved like every
 * other one) but laid out against the window edge rather than a free
 * position, because a full-height rail only makes sense flush.
 *
 * WHAT IT SHOWS, in priority order — anything waiting on YOU sits above
 * anything that is merely informational:
 *   1. Invites you have not answered
 *   2. Your party, with ready state and the crown
 *   3. Friends who are online
 *   4. Friends who are offline, dimmed
 */
public class PartySidePanel {
    public static final String ID = "party_panel";

    private static final int WIDTH = 132;
    private static final int PAD = 7;
    private static final int ROW_H = 21;
    private static final int HEAD_H = 15;

    private static final int READY = 0xFF5EEB8F;
    private static final int WAITING = 0xFFE3AC45;
    private static final int DIM = 0xFF736B96;
    private static final int CROWN = 0xFFFFD76A;
    private static final int TEXT = 0xFFEAE6F7;
    private static final int ACCENT = 0xFFB98BFF;

    public static void render(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tick) {
        if (!HudStyle.shouldRenderHuds()) return;
        if (!NebulaConfig.get().isHudEnabled(ID)) return;

        MinecraftClient client = MinecraftClient.getInstance();
        PartyState state = PartyState.get();

        // Nothing connected and nothing to say — draw nothing at all
        // rather than an empty rail taking up a third of the screen.
        if (!NebulaLink.get().isConnected()) return;
        if (!state.inParty() && state.friends().isEmpty() && state.invites().isEmpty()) return;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int x = screenW - WIDTH - 4;
        int y = 4;
        int bottom = screenH - 4;

        // One translucent rail behind everything, so the sections read as
        // one panel with divisions rather than three stacked boxes.
        HudStyle.panel(ctx, x, y, WIDTH, bottom - y);

        int cy = y + PAD;

        // ---- invites: the only thing here that is waiting on you ------
        for (PartyState.Invite inv : state.invites()) {
            if (cy + 30 > bottom) break;
            ctx.fill(x + 3, cy - 2, x + WIDTH - 3, cy + 28, 0x38B98BFF);
            ctx.fill(x + 3, cy - 2, x + 4, cy + 28, ACCENT); // accent spine
            ctx.drawTextWithShadow(client.textRenderer,
                    Text.literal(clip(client, inv.fromName, WIDTH - 20)).formatted(Formatting.BOLD),
                    x + PAD, cy + 1, TEXT);
            ctx.drawTextWithShadow(client.textRenderer,
                    Text.literal("invited you"), x + PAD, cy + 11, DIM);
            ctx.drawTextWithShadow(client.textRenderer,
                    Text.literal(inv.instantLaunch ? "No ready-up!" : "Open party menu"),
                    x + PAD, cy + 20, inv.instantLaunch ? WAITING : ACCENT);
            cy += 34;
        }

        // ---- party ----------------------------------------------------
        if (state.inParty() && !state.members().isEmpty()) {
            cy = section(ctx, client, x, cy, "PARTY · " + state.members().size());
            for (PartyState.Member m : state.members()) {
                if (cy + ROW_H > bottom) break;
                int dot = m.leader ? CROWN
                        : m.sittingOut ? DIM
                        : m.ready ? READY : WAITING;
                ctx.fill(x + PAD, cy + 4, x + PAD + 3, cy + 12, dot);

                String name = clip(client, m.name == null ? "Player" : m.name, WIDTH - 26);
                Text label = m.self
                        ? Text.literal(name).formatted(Formatting.BOLD)
                        : Text.literal(name);
                ctx.drawTextWithShadow(client.textRenderer, label,
                        x + PAD + 7, cy + 1, m.sittingOut ? DIM : TEXT);
                if (m.leader) {
                    ctx.drawTextWithShadow(client.textRenderer, Text.literal("♛"),
                            x + WIDTH - PAD - 6, cy + 1, CROWN);
                }
                String sub = m.sittingOut ? "Sitting out"
                        : m.leader ? "Leader"
                        : m.ready ? "Ready" : "Not ready";
                ctx.drawTextWithShadow(client.textRenderer, Text.literal(sub),
                        x + PAD + 7, cy + 11, m.ready ? READY : DIM);
                cy += ROW_H;
            }
            if (state.instantLaunch() && cy + 10 < bottom) {
                ctx.drawTextWithShadow(client.textRenderer,
                        Text.literal("Instant launch"), x + PAD, cy, WAITING);
                cy += 12;
            }
            cy += 4;
        }

        // ---- friends ---------------------------------------------------
        if (!state.friends().isEmpty() && cy + HEAD_H < bottom) {
            long online = state.friends().stream().filter(f -> f.online).count();
            cy = section(ctx, client, x, cy, "FRIENDS · " + online + "/" + state.friends().size());

            for (PartyState.Friend f : state.friends()) {
                if (cy + ROW_H > bottom) {
                    // Say how many did not fit rather than silently cutting
                    // the list off at whatever the window height allows.
                    int shown = countShown(state, bottom, cy);
                    if (shown > 0) {
                        ctx.drawTextWithShadow(client.textRenderer,
                                Text.literal("+" + shown + " more"), x + PAD, bottom - 12, DIM);
                    }
                    break;
                }
                ctx.fill(x + PAD, cy + 4, x + PAD + 3, cy + 12,
                        f.online ? READY : 0x40FFFFFF);
                ctx.drawTextWithShadow(client.textRenderer,
                        Text.literal(clip(client, f.name, WIDTH - 20)),
                        x + PAD + 7, cy + 1, f.online ? TEXT : DIM);
                String sub = !f.online ? "Offline"
                        : f.activity != null ? f.activity : "Online";
                ctx.drawTextWithShadow(client.textRenderer,
                        Text.literal(clip(client, sub, WIDTH - 20)),
                        x + PAD + 7, cy + 11, DIM);
                cy += ROW_H;
            }
        }
    }

    /** Section header with a hairline under it. Returns the new cursor. */
    private static int section(DrawContext ctx, MinecraftClient client, int x, int y, String label) {
        ctx.drawTextWithShadow(client.textRenderer,
                Text.literal(label).formatted(Formatting.BOLD), x + PAD, y, ACCENT);
        ctx.fill(x + PAD, y + 10, x + WIDTH - PAD, y + 11, 0x22FFFFFF);
        return y + HEAD_H;
    }

    private static int countShown(PartyState state, int bottom, int cy) {
        int fits = Math.max(0, (bottom - cy) / ROW_H);
        return Math.max(0, state.friends().size() - fits);
    }

    /** Truncate to fit the rail rather than letting text run off it. */
    private static String clip(MinecraftClient client, String text, int maxWidth) {
        if (text == null) return "";
        String out = text;
        while (client.textRenderer.getWidth(out) > maxWidth && out.length() > 2) {
            out = out.substring(0, out.length() - 2) + "…";
        }
        return out;
    }
}
