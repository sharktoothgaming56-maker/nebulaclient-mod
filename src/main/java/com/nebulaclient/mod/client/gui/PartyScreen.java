package com.nebulaclient.mod.client.gui;

import com.nebulaclient.mod.client.social.NebulaLink;
import com.nebulaclient.mod.client.social.PartyState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * PARTY & FRIENDS, in game.
 *
 * Two columns: your party on the left, your friends on the right. That
 * split exists because the two answer different questions — "who am I
 * going in with" and "who could I add" — and putting them side by side
 * means inviting someone never costs you sight of the party you are
 * building.
 *
 * Sizes off SpaceScreen's responsive helpers rather than fixed pixels,
 * so it reflows when the window is resized exactly like every other
 * NebulaClient screen.
 *
 * If the launcher is not running there is nothing truthful to show, so
 * the screen says so plainly instead of rendering empty columns.
 */
public class PartyScreen extends SpaceTheme.SpaceScreen {
    private final Screen parent;

    private static final int ROW_H = 20;
    private static final int READY = 0xFF5EEB8F;
    private static final int WAITING = 0xFFE3AC45;
    private static final int DIM = 0xFF736B96;
    private static final int CROWN = 0xFFFFD76A;
    private static final int TEXT = 0xFFEAE6F7;

    /** Column geometry, recomputed in init() so resizing reflows it. */
    private int colW, leftX, rightX, listTop, listBottom;

    public PartyScreen(Screen parent) {
        super(Text.literal("Party"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        nebulaButtons.clear();

        // Two columns inside a panel that is a little wider than the
        // single-column menus, because two lists need the room.
        int total = Math.max(280, Math.min(460, (int) (this.width * 0.72f)));
        colW = (total - 12) / 2;
        leftX = (this.width - total) / 2;
        rightX = leftX + colW + 12;
        listTop = 56;
        listBottom = this.height - 60;

        PartyState state = PartyState.get();
        boolean linked = NebulaLink.get().isConnected();

        // ---- party actions, bottom of the left column ----------------
        int by = this.height - 52;
        if (linked && state.inParty()) {
            PartyState.Member me = self(state);

            if (me != null && !me.leader) {
                boolean ready = me.ready;
                SpaceTheme.NebulaButton readyBtn = new SpaceTheme.NebulaButton(
                        leftX, by, colW, 20,
                        ready ? "Ready ✓" : "Ready up",
                        () -> { NebulaLink.get().setReady(!ready); });
                readyBtn.bold = !ready;
                nebulaButtons.add(readyBtn);
                by += 24;
            }

            if (me != null) {
                boolean out = me.sittingOut;
                nebulaButtons.add(new SpaceTheme.NebulaButton(
                        leftX, by, (colW - 4) / 2, 20,
                        out ? "Join in" : "Sit out",
                        () -> NebulaLink.get().setSittingOut(!out)));
                nebulaButtons.add(new SpaceTheme.NebulaButton(
                        leftX + (colW - 4) / 2 + 4, by, (colW - 4) / 2, 20,
                        "Leave party",
                        () -> { NebulaLink.get().leaveParty(); this.rebuild(); }));
            }
        }

        // ---- invites: answering one is the most urgent thing here -----
        int iy = listTop;
        for (PartyState.Invite inv : state.invites()) {
            nebulaButtons.add(new SpaceTheme.NebulaButton(
                    rightX, iy + 22, (colW - 4) / 2, 18, "Join",
                    () -> { NebulaLink.get().respondToInvite(inv.id, true); this.rebuild(); }));
            nebulaButtons.add(new SpaceTheme.NebulaButton(
                    rightX + (colW - 4) / 2 + 4, iy + 22, (colW - 4) / 2, 18, "Decline",
                    () -> { NebulaLink.get().respondToInvite(inv.id, false); this.rebuild(); }));
            iy += 46;
        }

        // ---- friend rows: invite / promote -----------------------------
        int fy = iy;
        for (PartyState.Friend f : state.friends()) {
            if (fy + ROW_H > listBottom) break;
            boolean alreadyIn = state.members().stream().anyMatch(m -> m.id.equals(f.id));
            boolean canInvite = state.isLeader() || state.anyoneCanInvite() || !state.inParty();
            if (!alreadyIn && canInvite && f.online) {
                nebulaButtons.add(new SpaceTheme.NebulaButton(
                        rightX + colW - 46, fy + 2, 44, 15, "Invite",
                        () -> NebulaLink.get().invite(f.id)));
            }
            fy += ROW_H;
        }

        nebulaButtons.add(new SpaceTheme.NebulaButton(
                (this.width - 120) / 2, this.height - 26, 120, 20, "Done",
                () -> this.client.setScreen(parent)));
    }

    /** Re-run init so buttons match the state that just changed. */
    private void rebuild() {
        this.clearAndInit();
    }

    private static PartyState.Member self(PartyState state) {
        return state.members().stream().filter(m -> m.self).findFirst().orElse(null);
    }

    @Override
    protected boolean showWordmark() { return false; }

    @Override
    protected void renderContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        PartyState state = PartyState.get();
        boolean linked = NebulaLink.get().isConnected();

        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("PARTY & FRIENDS").formatted(Formatting.BOLD),
                this.width / 2, 26, 0xFFB98BFF);

        if (!linked) {
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("NebulaClient launcher isn't running."),
                    this.width / 2, this.height / 2 - 10, TEXT);
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Start the launcher to see your party and friends."),
                    this.width / 2, this.height / 2 + 4, DIM);
            return;
        }

        // ---- left column: the party ----------------------------------
        header(ctx, leftX, listTop - 12, state.inParty()
                ? "YOUR PARTY · " + state.members().size()
                : "YOUR PARTY");

        if (!state.inParty()) {
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("Not in a party."), leftX, listTop + 4, DIM);
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("Invite a friend to start one."), leftX, listTop + 16, DIM);
        } else {
            int y = listTop;
            for (PartyState.Member m : state.members()) {
                if (y + ROW_H > listBottom) break;
                int dot = m.leader ? CROWN : m.sittingOut ? DIM : m.ready ? READY : WAITING;
                ctx.fill(leftX, y + 4, leftX + 3, y + 11, dot);

                Text name = m.self
                        ? Text.literal(m.name + " (you)").formatted(Formatting.BOLD)
                        : Text.literal(m.name == null ? "Player" : m.name);
                ctx.drawTextWithShadow(this.textRenderer, name, leftX + 8, y + 2,
                        m.sittingOut ? DIM : TEXT);
                if (m.leader) {
                    ctx.drawTextWithShadow(this.textRenderer, Text.literal("♛"),
                            leftX + 10 + this.textRenderer.getWidth(name), y + 2, CROWN);
                }

                String sub = m.sittingOut ? "Sitting out"
                        : m.leader ? "Party leader"
                        : m.ready ? "Ready"
                        : "Not ready";
                ctx.drawTextWithShadow(this.textRenderer, Text.literal(sub), leftX + 8, y + 11,
                        m.ready ? READY : DIM);
                y += ROW_H;
            }

            if (state.instantLaunch()) {
                ctx.drawTextWithShadow(this.textRenderer,
                        Text.literal("Launches without ready-up"),
                        leftX, listBottom - 10, WAITING);
            }
        }

        // ---- right column: invites, then friends ---------------------
        int y = listTop;
        for (PartyState.Invite inv : state.invites()) {
            ctx.fill(rightX - 2, y - 2, rightX + colW, y + 40, 0x40B98BFF);
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal(inv.fromName + " invited you").formatted(Formatting.BOLD),
                    rightX + 2, y + 2, TEXT);
            String note = inv.instantLaunch
                    ? "No ready-up — you go when they go"
                    : inv.target != null ? "Heading to " + inv.target : "Party invite";
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(note),
                    rightX + 2, y + 12, inv.instantLaunch ? WAITING : DIM);
            y += 46;
        }

        header(ctx, rightX, y - 12, "FRIENDS");
        if (state.friends().isEmpty()) {
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("No friends yet."), rightX, y + 4, DIM);
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal("Follow people in the launcher."), rightX, y + 16, DIM);
        }
        for (PartyState.Friend f : state.friends()) {
            if (y + ROW_H > listBottom) break;
            ctx.fill(rightX, y + 4, rightX + 3, y + 11, f.online ? READY : 0x40FFFFFF);
            ctx.drawTextWithShadow(this.textRenderer,
                    Text.literal(f.name), rightX + 8, y + 2, f.online ? TEXT : DIM);
            String sub = !f.online ? "Offline"
                    : f.activity != null ? f.activity
                    : "Online";
            // Truncate rather than overlap the Invite button.
            int room = colW - 56;
            while (this.textRenderer.getWidth(sub) > room && sub.length() > 2) {
                sub = sub.substring(0, sub.length() - 2) + "…";
            }
            ctx.drawTextWithShadow(this.textRenderer, Text.literal(sub), rightX + 8, y + 11, DIM);
            y += ROW_H;
        }
    }

    private void header(DrawContext ctx, int x, int y, String label) {
        ctx.drawTextWithShadow(this.textRenderer,
                Text.literal(label).formatted(Formatting.BOLD), x, y, 0xFFA79FC4);
    }
}
