package com.nebulaclient.mod.client.social;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nebulaclient.mod.client.NebulaClientModClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NEBULA LINK — the mod's end of the launcher bridge.
 *
 * The launcher runs a tiny HTTP server bound to 127.0.0.1 on an
 * OS-assigned port, and drops a handshake file with the port and a
 * per-session token into its own user-data directory. This class finds
 * that file, then long-polls {@code /events} on a daemon thread so party
 * changes show up in-game within a frame or two instead of on a timer.
 *
 * WHY HTTP AND NOT A SOCKET LIBRARY: {@code java.net.http.HttpClient} has
 * shipped with the JDK since 11, and this mod already requires Java 21.
 * So this whole file adds zero dependencies to build.gradle. Same story
 * on the launcher side, which uses Node's built-in http module.
 *
 * EVERYTHING HERE IS BEST-EFFORT. If the launcher is not running — the
 * player started Minecraft some other way, or closed the launcher after
 * launching — every method quietly no-ops and {@link #isConnected()}
 * stays false. Nothing in the game is allowed to break because a party
 * feature could not reach a launcher that may not exist.
 */
public final class NebulaLink {
    private static final Gson GSON = new Gson();
    private static final NebulaLink INSTANCE = new NebulaLink();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile int port = -1;
    private volatile String token = null;
    private volatile boolean connected = false;
    private volatile long revision = 0;
    private Thread pollThread;

    private NebulaLink() {}

    public static NebulaLink get() { return INSTANCE; }

    public boolean isConnected() { return connected; }

    // -----------------------------------------------------------------
    // Handshake discovery
    // -----------------------------------------------------------------

    /**
     * Where Electron's {@code app.getPath('userData')} lands, per OS.
     * Electron uses the productName, which this launcher sets to
     * "NebulaClient" in package.json — so the folder name is stable and
     * we don't have to guess.
     */
    private static Path handshakePath() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            Path base = appData != null ? Paths.get(appData) : Paths.get(home, "AppData", "Roaming");
            return base.resolve("NebulaClient").resolve("nebula-bridge.json");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return Paths.get(home, "Library", "Application Support", "NebulaClient", "nebula-bridge.json");
        }
        String xdg = System.getenv("XDG_CONFIG_HOME");
        Path base = xdg != null ? Paths.get(xdg) : Paths.get(home, ".config");
        return base.resolve("NebulaClient").resolve("nebula-bridge.json");
    }

    /** Reads the handshake file. Returns false if the launcher isn't up. */
    private boolean readHandshake() {
        try {
            Path p = handshakePath();
            if (!Files.exists(p)) return false;
            JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            int newPort = o.get("port").getAsInt();
            String newToken = o.get("token").getAsString();
            if (newPort != port || !newToken.equals(token)) {
                // The launcher restarted and picked a new port — reset the
                // revision, or we'd sit waiting on an event number from a
                // server that no longer exists.
                revision = 0;
            }
            port = newPort;
            token = newToken;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------
    public void start() {
        if (!running.compareAndSet(false, true)) return;
        pollThread = new Thread(this::pollLoop, "NebulaLink");
        pollThread.setDaemon(true); // never hold the game open on shutdown
        pollThread.start();
    }

    public void stop() {
        running.set(false);
        if (pollThread != null) pollThread.interrupt();
    }

    private void pollLoop() {
        int backoffMs = 1000;
        while (running.get()) {
            try {
                if (!readHandshake()) {
                    connected = false;
                    PartyState.get().clear();
                    // No launcher. Check again on a slow cadence — this is
                    // the normal state for anyone not using the launcher,
                    // so it must cost effectively nothing.
                    Thread.sleep(5000);
                    continue;
                }

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/events?since=" + revision))
                        .header("authorization", "Bearer " + token)
                        .timeout(Duration.ofSeconds(35)) // server parks for 25s
                        .GET().build();

                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() != 200) {
                    connected = false;
                    Thread.sleep(backoffMs);
                    backoffMs = Math.min(backoffMs * 2, 15000);
                    continue;
                }

                connected = true;
                backoffMs = 1000;
                JsonObject body = JsonParser.parseString(res.body()).getAsJsonObject();
                if (body.has("revision")) revision = body.get("revision").getAsLong();
                if (body.has("state") && !body.get("state").isJsonNull()) {
                    PartyState.get().apply(body.getAsJsonObject("state"));
                }
                if (body.has("notice") && !body.get("notice").isJsonNull()) {
                    PartyState.get().onNotice(body.getAsJsonObject("notice"));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // Timeouts are expected and routine — the long poll simply
                // had nothing to report. Only log the interesting ones.
                connected = false;
                try { Thread.sleep(backoffMs); } catch (InterruptedException ie) { return; }
                backoffMs = Math.min(backoffMs * 2, 15000);
            }
        }
    }

    // -----------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------

    /**
     * Fire-and-forget action. Runs off the render thread so a slow or
     * dead launcher can never stall a frame — clicking "Leave party"
     * must feel instant whether or not anything is listening.
     */
    public void action(String action, JsonObject payload) {
        if (!connected || token == null) return;
        Thread.ofVirtual().start(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("action", action);
                body.add("payload", payload == null ? new JsonObject() : payload);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://127.0.0.1:" + port + "/action"))
                        .header("authorization", "Bearer " + token)
                        .header("content-type", "application/json")
                        .timeout(Duration.ofSeconds(5))
                        .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                NebulaClientModClient.LOGGER.debug("NebulaLink action {} failed: {}", action, e.toString());
            }
        });
    }

    public void leaveParty() { action("party:leave", null); }

    public void setReady(boolean ready) {
        JsonObject o = new JsonObject();
        o.addProperty("ready", ready);
        action("party:setReady", o);
    }

    public void setSittingOut(boolean sittingOut) {
        JsonObject o = new JsonObject();
        o.addProperty("sittingOut", sittingOut);
        action("party:setSittingOut", o);
    }

    public void invite(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        action("party:invite", o);
    }

    public void transferLeader(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        action("party:transferLeader", o);
    }

    public void kick(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        action("party:kick", o);
    }

    public void respondToInvite(String inviteId, boolean accept) {
        JsonObject o = new JsonObject();
        o.addProperty("inviteId", inviteId);
        o.addProperty("accept", accept);
        action("party:respondToInvite", o);
    }

    /**
     * Tell the launcher where the player actually is. The mod knows this
     * far better than the launcher does — it can read the live server
     * connection rather than inferring it from launch arguments, so a
     * player who joined a second server mid-session still shows up
     * correctly to their friends.
     */
    public void reportActivity(String kind, String server, String label, String detail) {
        JsonObject activity = new JsonObject();
        activity.addProperty("kind", kind);
        if (server != null) activity.addProperty("server", server);
        if (label != null) activity.addProperty("label", label);
        if (detail != null) activity.addProperty("detail", detail);
        JsonObject o = new JsonObject();
        o.add("activity", activity);
        action("presence:update", o);
    }

    public void reportIdle() {
        JsonObject o = new JsonObject();
        o.add("activity", null);
        action("presence:update", o);
    }
}
