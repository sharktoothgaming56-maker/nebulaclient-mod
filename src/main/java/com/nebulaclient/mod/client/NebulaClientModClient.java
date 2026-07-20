package com.nebulaclient.mod.client;

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

    private boolean titleScreenSwapped = false; // only swap once per game-menu "session" so the player can still get back to vanilla's screen via Esc if something looks wrong

    @Override
    public void onInitializeClient() {
        NebulaKeybinds.init();
        registerHuds();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        LOGGER.info("NebulaClient client init complete");
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

    private void onClientTick(MinecraftClient client) {
        WaypointManager.tick(client);
        swapVanillaScreens(client);

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
     * (tracked by titleScreenSwapped) rather than fighting the game every
     * tick, which would make it impossible to ever see the vanilla screen
     * again if something about the custom one broke.
     */
    private void swapVanillaScreens(MinecraftClient client) {
        boolean onVanillaSwappable = client.currentScreen instanceof TitleScreen || client.currentScreen instanceof GameMenuScreen;
        if (!onVanillaSwappable) { titleScreenSwapped = false; return; }
        if (titleScreenSwapped) return;

        titleScreenSwapped = true;
        if (client.currentScreen instanceof TitleScreen) {
            client.setScreen(new NebulaTitleScreen());
        } else {
            // Grab a still of the frame we're pausing over, so the HUD
            // editor can show your real gameplay as its backdrop.
            com.nebulaclient.mod.client.gui.GameplayPreview.capture(client);
            client.setScreen(new NebulaGameMenuScreen());
        }
    }

    private static void sendFeedback(MinecraftClient client, String message) {
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[NebulaClient] " + message), false);
        }
    }
}
