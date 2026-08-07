package com.nebulaclient.mod.client;

import com.nebulaclient.mod.client.config.NebulaConfig;
import com.nebulaclient.mod.client.gui.NebulaGameMenuScreen;
import com.nebulaclient.mod.client.gui.NebulaMenuScreen;
import com.nebulaclient.mod.client.gui.NebulaTitleScreen;
import com.nebulaclient.mod.client.gui.WaypointMenuScreen;
import com.nebulaclient.mod.client.hud.ArmorHud;
import com.nebulaclient.mod.client.hud.CoordsHud;
import com.nebulaclient.mod.client.hud.CpsHud;
import com.nebulaclient.mod.client.hud.DirectionHud;
import com.nebulaclient.mod.client.hud.EffectsHud;
import com.nebulaclient.mod.client.hud.FpsHud;
import com.nebulaclient.mod.client.hud.LogoHud;
import com.nebulaclient.mod.client.hud.StatusHud;
import com.nebulaclient.mod.client.gui.WaypointNameScreen;
import com.nebulaclient.mod.client.hud.Waypoint3dHud;
import com.nebulaclient.mod.client.waypoint.WaypointManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NebulaClientModClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("NebulaClient");
    private static final String NAMESPACE = "nebulaclient";

    // Some keys (Freelook, Zoom) are meant to be held rather than pressed —
    // this tracks which are currently held down so later work (camera
    // handling) has something to read.
    public static boolean freelookHeld = false;
    public static boolean zoomHeld = false;
    public static boolean chatHidden = false;
    public static boolean waypointsVisible = true;

    // GUI-scale persistence: restore the saved scale once on startup, then
    // follow whatever the player sets afterwards, re-saving it locally.
    private boolean guiScaleRestored = false;
    private int lastKnownGuiScale = Integer.MIN_VALUE;

    // --- Disconnect watchdog ---------------------------------------------
    // Every MinecraftClient.disconnect* overload takes a parameter yarn
    // names "disconnectionScreen": the screen shown WHILE tearing down. None
    // of them choose where you end up afterwards — the caller always did
    // that. The teardown is also asynchronous (see the client's
    // `disconnecting` flag and onDisconnected()), so setting the
    // destination on the very next line races it and gets overwritten,
    // which is exactly how we ended up parked on "Saving world" forever.
    //
    // So instead of guessing at the timing: start the disconnect, then
    // watch the real state. Once client.world is actually null the teardown
    // is genuinely finished, and if we're still sat on the progress screen
    // we route to the menu ourselves. If vanilla gets there first, we see
    // that and stand down. Driven from BOTH the tick loop and the render
    // hook, because ticks stop while the game is paused.
    private static boolean disconnectPending = false;
    private static boolean disconnectSingleplayer = false;
    private static boolean disconnectEscalated = false;
    private static int disconnectFrames = 0;
    /** Frames to let vanilla route itself before we take over. */
    private static final int SETTLE_FRAMES = 10;
    /** Frames to wait on a stalled teardown before kicking it once (~3s). */
    private static final int ESCALATE_AFTER = 180;

    /**
     * Where Disconnect should land you: the world list when leaving a
     * singleplayer world, the server list when leaving a server — each with
     * our title screen behind it, so Back goes somewhere sensible.
     */
    public static net.minecraft.client.gui.screen.Screen disconnectDestination(boolean singleplayer) {
        NebulaTitleScreen title = new NebulaTitleScreen();
        return singleplayer
                ? new net.minecraft.client.gui.screen.world.SelectWorldScreen(title)
                : new net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen(title);
    }

    /** Called by the pause menu's Disconnect button just before it tears down. */
    public static void beginDisconnect(boolean singleplayer) {
        disconnectPending = true;
        disconnectSingleplayer = singleplayer;
        disconnectEscalated = false;
        disconnectFrames = 0;
    }

    /** Idempotent; safe to call every tick and every frame. */
    public static void pumpDisconnect(MinecraftClient client) {
        if (!disconnectPending) return;

        disconnectFrames++;

        // World still loaded => teardown genuinely still running (or never
        // started). Wait for it; forcing a screen change here would strand a
        // loaded world in the background.
        if (client.world != null) {
            // ...unless it has been sitting there far too long. A dropped
            // teardown leaves the world rendering behind the progress screen
            // forever, so kick it exactly once rather than hanging.
            if (disconnectFrames == ESCALATE_AFTER && !disconnectEscalated) {
                disconnectEscalated = true;
                LOGGER.warn("Disconnect watchdog: world still loaded after {} frames — retrying teardown", ESCALATE_AFTER);
                try {
                    if (client.world != null) {
                        client.world.disconnect(net.minecraft.text.Text.translatable("menu.returnToMenu"));
                    }
                    if (client.isInSingleplayer()) {
                        client.disconnectWithSavingScreen();
                    } else {
                        client.disconnectWithProgressScreen();
                    }
                } catch (RuntimeException e) {
                    LOGGER.warn("Disconnect watchdog: retry threw {}", e.toString());
                }
            }
            return;
        }

        var current = client.currentScreen;
        if (current instanceof NebulaTitleScreen
                || current instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
                || current instanceof net.minecraft.client.gui.screen.world.SelectWorldScreen) {
            reset(); // we (or vanilla) already landed somewhere sensible
            return;
        }

        // Give vanilla a few frames to land somewhere on its own before we
        // step in, so we don't fight it in the normal case.
        if (disconnectFrames < SETTLE_FRAMES) return;

        LOGGER.info("Disconnect watchdog: world is down but screen was still '{}' — routing to the menu",
                current == null ? "none" : current.getClass().getSimpleName());
        boolean singleplayer = disconnectSingleplayer;
        reset();
        client.setScreen(disconnectDestination(singleplayer));
    }

    private static void reset() {
        disconnectPending = false;
        disconnectEscalated = false;
        disconnectFrames = 0;
    }

    @Override
    public void onInitializeClient() {
        NebulaKeybinds.init();
        registerHuds();
        registerMenuWordmark();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Connect to the launcher. Best-effort and entirely optional —
        // if no launcher is running this thread parks on a slow retry and
        // every party feature simply reports "not connected".
        com.nebulaclient.mod.client.social.NebulaLink.get().start();

        LOGGER.info("NebulaClient client init complete");
    }

    /**
     * Draws the small "NEBULACLIENT" wordmark in the bottom-right of vanilla
     * menu screens (Multiplayer, Options, etc.) so the client's mark shows
     * there too — our own SpaceScreens already draw their own wordmark, and
     * in-game the LogoHud handles it, so we skip both of those here.
     */
    private void registerMenuWordmark() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof com.nebulaclient.mod.client.gui.SpaceTheme.SpaceScreen) return;

            // Swap vanilla's title/pause menu for ours HERE rather than in
            // the tick loop. Opening the pause menu in singleplayer PAUSES
            // the game, so END_CLIENT_TICK stops firing and the tick-based
            // swap never ran — that's why the vanilla menu kept showing.
            // AFTER_INIT fires whenever any screen opens, paused or not.
            // Swapping vanilla's title/pause menu for ours is DEFERRED by a
            // frame rather than done right here. Other mods (Mod Menu,
            // Essential, ...) add their buttons from their own init handlers,
            // and the order mods run in isn't defined — swapping immediately
            // threw away every button added by a mod that happened to run
            // after us. Deferring means all of them have finished before we
            // look, so we can carry their buttons across (see ModCompat).
            // Turned off in the Nebula Menu when the player needs vanilla's
            // screens for an overlay-drawing mod like Essential.
            boolean customMenus = com.nebulaclient.mod.client.config.NebulaConfig.get().customMenus;

            if (screen instanceof TitleScreen && customMenus) {
                client.execute(() -> {
                    if (client.currentScreen != screen) return; // something else took over
                    var extras = com.nebulaclient.mod.client.gui.ModCompat.harvest(screen, false);
                    client.setScreen(new NebulaTitleScreen(extras));
                });
                return;
            }
            if (screen instanceof GameMenuScreen && customMenus) {
                com.nebulaclient.mod.client.gui.GameplayPreview.capture(client);
                client.execute(() -> {
                    if (client.currentScreen != screen) return;
                    var extras = com.nebulaclient.mod.client.gui.ModCompat.harvest(screen, true);
                    client.setScreen(new NebulaGameMenuScreen(extras));
                });
                return;
            }

            // Multiplayer screen: favourites category + star toggles + drag
            // reordering, added on top of the vanilla list (see
            // ServerListEnhancer for the full story). Nothing is removed:
            // the join arrow and move arrows behave exactly as before.
            if (screen instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen multiplayerScreen) {
                com.nebulaclient.mod.client.gui.ServerListEnhancer.attach(client, multiplayerScreen);

                // MULTIPLAYER artwork in the header, same treatment as the
                // Key Binds screen but smaller: hide the "Play Multiplayer"
                // title text and draw the logo centred, vertically centred
                // on the category button row (y=6..26). Sized to clear the
                // top-left button symmetrically; on very narrow windows
                // where it would have to shrink below readable, the vanilla
                // text is kept instead. Resizes re-run AFTER_INIT, so these
                // numbers stay correct at any window size.
                int mpBtnRight = 6 + com.nebulaclient.mod.client.gui.ServerListEnhancer.toggleWidth(screen.width);
                int mpHalfAvail = screen.width / 2 - mpBtnRight - 8;
                int mpLogoW = Math.min(96, mpHalfAvail * 2);
                if (mpLogoW >= 70) {
                    for (var wdg : net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen)) {
                        if (wdg instanceof net.minecraft.client.gui.widget.TextWidget tw
                                && tw.getMessage().getString().equals(screen.getTitle().getString())) {
                            tw.visible = false;
                        }
                    }
                    final int logoW = mpLogoW;
                    final int logoH = Math.max(1, Math.round(logoW * (float) com.nebulaclient.mod.client.gui.NebulaLogos.MULTIPLAYER_H
                            / com.nebulaclient.mod.client.gui.NebulaLogos.MULTIPLAYER_W));
                    final int top = 6 + (20 - logoH) / 2;
                    net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(screen).register((s2, ctx, mx, my, dt) -> {
                        com.nebulaclient.mod.client.gui.NebulaLogos.drawCentered(ctx,
                                com.nebulaclient.mod.client.gui.NebulaLogos.MULTIPLAYER,
                                com.nebulaclient.mod.client.gui.NebulaLogos.MULTIPLAYER_W,
                                com.nebulaclient.mod.client.gui.NebulaLogos.MULTIPLAYER_H,
                                s2.width / 2, top, logoW);
                    });
                }
            }

            // Key Binds screen: preset controls in the header — "Save Preset"
            // top-LEFT (opens the name + colour screen, then saves the whole
            // current bind set locally), "Presets" top-RIGHT (opens the list;
            // selecting one applies it immediately). Added via Fabric's
            // Screens API, so no Mixin is needed.
            if (screen instanceof net.minecraft.client.gui.screen.option.KeybindsScreen) {
                var buttons = net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen);
                // Flank the centred "Key Binds" title: Save Preset just to
                // its left, Presets just to its right — close in, so it
                // reads as one header instead of buttons floating at the
                // screen edges.
                int cx = screen.width / 2;
                // Full 96px next to the 128px logo gap on normal windows;
                // on narrow ones the buttons shrink instead of running off
                // the screen edges, keeping the header row even.
                int kbBtnW = Math.max(60, Math.min(96, screen.width / 2 - 64 - 10));
                buttons.add(net.minecraft.client.gui.widget.ButtonWidget.builder(
                        Text.literal("Save Preset"),
                        b -> client.setScreen(new com.nebulaclient.mod.client.gui.PresetNameScreen(screen)))
                        .dimensions(cx - 64 - kbBtnW, 6, kbBtnW, 20).build());
                buttons.add(net.minecraft.client.gui.widget.ButtonWidget.builder(
                        Text.literal("Presets"),
                        b -> client.setScreen(new com.nebulaclient.mod.client.gui.KeybindPresetScreen(screen)))
                        .dimensions(cx + 64, 6, kbBtnW, 20).build());

                // Swap the plain "Key Binds" title text for the supplied
                // KEY BINDS artwork: hide the header's title TextWidget,
                // then draw the logo centred in the 128px gap between the
                // two preset buttons, vertically centred on them (buttons
                // are y=6..26, logo ~17px tall at y=7) so the header reads
                // as one even row. If the TextWidget isn't found the logo
                // simply draws over the text, so nothing can break.
                for (var wdg : buttons) {
                    if (wdg instanceof net.minecraft.client.gui.widget.TextWidget tw
                            && tw.getMessage().getString().equals(screen.getTitle().getString())) {
                        tw.visible = false;
                    }
                }
                net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(screen).register((s2, ctx, mx, my, dt) -> {
                    int c = s2.width / 2;
                    int logoW = 104; // fits the 128px gap between the header buttons with even margins
                    int logoH = Math.max(1, Math.round(logoW * (float) com.nebulaclient.mod.client.gui.NebulaLogos.KEYBINDS_H
                            / com.nebulaclient.mod.client.gui.NebulaLogos.KEYBINDS_W));
                    int top = 6 + (20 - logoH) / 2;
                    com.nebulaclient.mod.client.gui.NebulaLogos.drawCentered(ctx,
                            com.nebulaclient.mod.client.gui.NebulaLogos.KEYBINDS,
                            com.nebulaclient.mod.client.gui.NebulaLogos.KEYBINDS_W,
                            com.nebulaclient.mod.client.gui.NebulaLogos.KEYBINDS_H,
                            c, top, logoW);
                });
            }

            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, delta) -> {
                var mc = MinecraftClient.getInstance();
                pumpDisconnect(mc); // progress screens render even when ticks are paused
                if (mc.world != null) return; // in-game menus are handled by the HUD path
                // The Multiplayer screen now carries its own MULTIPLAYER
                // header artwork — and at small window sizes the corner
                // wordmark used to sit on top of Add Server / Back.
                if (s instanceof net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen) return;
                Text mark = Text.literal("NEBULACLIENT").formatted(net.minecraft.util.Formatting.BOLD);
                int color = 0xFF000000 | (com.nebulaclient.mod.client.config.NebulaConfig.get().getButtonColor() & 0xFFFFFF);
                int tx = s.width - mc.textRenderer.getWidth(mark) - 6;
                int ty = s.height - 12;
                ctx.drawTextWithShadow(mc.textRenderer, mark, tx, ty, color);
            });
        });
    }

    private void registerHuds() {
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "status"), StatusHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "armor"), ArmorHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "fps"), FpsHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "coords"), CoordsHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "direction"), DirectionHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "cps"), CpsHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "effects"), EffectsHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "waypoints_3d"), Waypoint3dHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "party"), com.nebulaclient.mod.client.hud.PartyHud::render);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "party_panel"), com.nebulaclient.mod.client.hud.PartySidePanel::render);

        // Hide vanilla's top-right potion effect icons so only our side list
        // shows, per the brief. removeElement(Identifier) is confirmed API.
        // NOTE: the field name STATUS_EFFECTS is the one thing I couldn't
        // verify — if this exact line doesn't compile, delete it (plus the
        // VanillaHudElements import) and everything else still works; you'd
        // just see vanilla's icons alongside ours until we find the right
        // field name from the compile error's suggestions.
        HudElementRegistry.removeElement(VanillaHudElements.STATUS_EFFECTS);
        HudElementRegistry.addLast(Identifier.of(NAMESPACE, "logo"), LogoHud::render);
    }

    /**
     * Keeps the vanilla GUI Scale persisted locally in nebulaclient.json so
     * it survives across launches (some setups reset it otherwise).
     *
     * Behaviour:
     *  - Once, on the first tick where options exist: if we have a saved
     *    scale, push it into vanilla and write options.txt so it sticks;
     *    otherwise adopt vanilla's current value as our baseline.
     *  - Every tick after: if the live scale changed (the player picked a
     *    new one in Video Settings), remember and persist the new value.
     *    This way we restore your choice at startup but never fight you when
     *    you deliberately change it.
     */
    private void syncGuiScale(MinecraftClient client) {
        if (client.options == null) return;
        try {
            int current = client.options.getGuiScale().getValue();

            if (!guiScaleRestored) {
                NebulaConfig cfg = NebulaConfig.get();
                int saved = cfg.getGuiScale();
                if (saved >= 0 && saved != current) {
                    client.options.getGuiScale().setValue(saved);
                    client.options.write(); // persist to options.txt immediately
                    current = saved;
                } else if (saved < 0) {
                    cfg.setGuiScale(current); // first run — capture what's there
                }
                lastKnownGuiScale = current;
                guiScaleRestored = true;
                return;
            }

            if (current != lastKnownGuiScale) {
                lastKnownGuiScale = current;
                NebulaConfig.get().setGuiScale(current); // remember the player's new choice
            }
        } catch (Exception e) {
            // Never let GUI-scale bookkeeping break the tick loop.
        }
    }

    // --- Social tick -----------------------------------------------------
    // Two jobs, both cheap enough to run every tick:
    //   1. Surface any pending notice from the launcher as a chat line.
    //      A toast overlay would fight the game's own; chat is where
    //      Minecraft players already look for "something happened".
    //   2. Report where the player actually IS. The mod can read the live
    //      server connection, which is more truthful than the launcher's
    //      guess from launch arguments — someone who joined a different
    //      server mid-session still shows correctly to their friends.
    private String lastReportedActivity = null;

    private void pumpSocial(MinecraftClient client) {
        com.nebulaclient.mod.client.social.PartyState state =
                com.nebulaclient.mod.client.social.PartyState.get();

        String[] toast = state.takeToast();
        if (toast != null && client.player != null) {
            client.player.sendMessage(
                    net.minecraft.text.Text.literal("[Nebula] ")
                            .formatted(net.minecraft.util.Formatting.LIGHT_PURPLE)
                            .append(net.minecraft.text.Text.literal(toast[0] + " — " + toast[1])
                                    .formatted(net.minecraft.util.Formatting.WHITE)),
                    false);
        }

        if (!com.nebulaclient.mod.client.social.NebulaLink.get().isConnected()) return;

        // Only send when it CHANGES. Reporting every tick would be 20
        // HTTP requests a second for information that moves once a session.
        String kind, server = null, label, detail = null;
        if (client.world == null) {
            kind = "menu"; label = "In the menus";
        } else if (client.isInSingleplayer()) {
            kind = "world"; label = "Singleplayer";
            detail = client.getServer() != null ? client.getServer().getSaveProperties().getLevelName() : null;
        } else {
            net.minecraft.client.network.ServerInfo info = client.getCurrentServerEntry();
            kind = "server";
            server = info != null ? info.address : null;
            label = info != null && info.name != null ? info.name : server;
        }

        String signature = kind + "|" + server + "|" + label + "|" + detail;
        if (signature.equals(lastReportedActivity)) return;
        lastReportedActivity = signature;
        com.nebulaclient.mod.client.social.NebulaLink.get().reportActivity(kind, server, label, detail);
    }

    private void onClientTick(MinecraftClient client) {
        pumpDisconnect(client);
        syncGuiScale(client);
        WaypointManager.tick(client);

        // Held-style keys
        freelookHeld = NebulaKeybinds.FREELOOK.isPressed();
        zoomHeld = NebulaKeybinds.ZOOM.isPressed();

        // Press-style keys (wasPressed() drains one "click" per call, so this
        // is safe to poll every tick without double-firing)
        while (NebulaKeybinds.CREATE_WAYPOINT.wasPressed()) {
            if (client.player != null) {
                int count = WaypointManager.forCurrentWorld(client).size() + 1;
                client.setScreen(new WaypointNameScreen("Waypoint " + count));
            }
        }
        while (NebulaKeybinds.MOD_MENU.wasPressed()) {
            client.setScreen(new NebulaMenuScreen(client.currentScreen));
        }
        while (NebulaKeybinds.PARTY_MENU.wasPressed()) {
            client.setScreen(new com.nebulaclient.mod.client.gui.PartyScreen(client.currentScreen));
        }

        pumpSocial(client);
        while (NebulaKeybinds.RESET_COUNTS.wasPressed()) {
            sendFeedback(client, "Counts reset (placeholder — no counters implemented yet).");
        }
        while (NebulaKeybinds.SEND_COORDINATES.wasPressed()) {
            if (client.player != null) {
                String coords = String.format("My coordinates: %.0f, %.0f, %.0f",
                        client.player.getX(), client.player.getY(), client.player.getZ());
                client.player.networkHandler.sendChatMessage(coords);
            }
        }
        while (NebulaKeybinds.TOGGLE_CHAT_VISIBILITY.wasPressed()) {
            chatHidden = !chatHidden;
            // NOTE: this only tracks the toggle internally for now — the
            // actual vanilla chat-visibility option turned out not to be at
            // the path I guessed (net.minecraft.client.option.ChatVisibility),
            // and I don't want to guess a second time blind. The HUD/other
            // systems can read NebulaClientModClient.chatHidden if you want
            // to actually hide chat rendering yourself later.
            sendFeedback(client, "Chat visibility: " + (chatHidden ? "hidden" : "shown"));
        }
        while (NebulaKeybinds.TOGGLE_WAYPOINT_DISPLAY.wasPressed()) {
            waypointsVisible = !waypointsVisible;
            sendFeedback(client, "Waypoint display: " + (waypointsVisible ? "on" : "off"));
        }
        while (NebulaKeybinds.WAYPOINT_MENU.wasPressed()) {
            client.setScreen(new WaypointMenuScreen(client.currentScreen));
        }
        while (NebulaKeybinds.FORWARD_VIEW.wasPressed()) {
            sendFeedback(client, "Forward View (placeholder — camera behavior not implemented yet).");
        }
    }

    /**
     * Swaps vanilla's TitleScreen/GameMenuScreen for our own the moment
     * they open — a deliberately Mixin-free way to get custom versions of
     * core screens. Guarded so it only swaps once per screen "opening"
     * unconditionally rather than fighting the game every
     * tick, which would make it impossible to ever see the vanilla screen
     * again if something about the custom one broke.
     */
    private static void sendFeedback(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[NebulaClient] " + message), false);
        }
    }
}
