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

    @Override
    public void onInitializeClient() {
        NebulaKeybinds.init();
        registerHuds();
        registerMenuWordmark();

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
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
            if (screen instanceof TitleScreen) {
                client.setScreen(new NebulaTitleScreen());
                return;
            }
            if (screen instanceof GameMenuScreen) {
                com.nebulaclient.mod.client.gui.GameplayPreview.capture(client);
                client.setScreen(new NebulaGameMenuScreen());
                return;
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
                buttons.add(net.minecraft.client.gui.widget.ButtonWidget.builder(
                        Text.literal("Save Preset"),
                        b -> client.setScreen(new com.nebulaclient.mod.client.gui.PresetNameScreen(screen)))
                        .dimensions(cx - 64 - 96, 6, 96, 20).build());
                buttons.add(net.minecraft.client.gui.widget.ButtonWidget.builder(
                        Text.literal("Presets"),
                        b -> client.setScreen(new com.nebulaclient.mod.client.gui.KeybindPresetScreen(screen)))
                        .dimensions(cx + 64, 6, 96, 20).build());
            }

            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, delta) -> {
                var mc = MinecraftClient.getInstance();
                if (mc.world != null) return; // in-game menus are handled by the HUD path
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

    private void onClientTick(MinecraftClient client) {
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
