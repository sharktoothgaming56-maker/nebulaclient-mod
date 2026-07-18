# NebulaClient Mod

Fabric mod for Minecraft 1.21.11. This is what the launcher auto-downloads
into Fabric/Quilt instances.

## What's built

- **HUDs** (Options > Nebula Menu, or the keybinds below): status
  (flying/sneaking/sprinting, held-vs-toggled), armor + offhand/mainhand
  with durability bars and an enchant glow, FPS/coords/facing, active
  potion effects with time remaining, and a waypoint compass. All hide
  themselves the instant any screen (inventory, menus, chat) opens, and a
  small "NebulaClient" mark shows bottom-left in their place — matching
  the brief.
- **Drag-to-reposition HUD editor** — real dragging via mouse events, not
  a preset-position picker. Click a HUD's bottom edge to toggle it
  on/off, drag anywhere else on it to move it.
- **Waypoints** — Create Waypoint keybind drops one at your position;
  Waypoint Menu lists/deletes them; death waypoints are off by default
  (per the brief) and toggle on from the same menu. Rendering is a 2D
  compass-style HUD (arrow + distance), not 3D beams in the world — see
  "What's deliberately scoped down" below for why.
- **Nebula Menu** — reachable via the Mod Menu keybind, and as a real
  button next to Options on the pause menu.
- **Custom main menu and pause menu** — swapped in for vanilla's via a
  safe tick-based detection, no Mixin required.
- All 10 keybinds now default to **unbound**, as requested.

## What's deliberately scoped down, and why

- **3D world-rendered waypoints** (beams/text floating in the world) —
  needs camera/projection math, one of the most version-fragile parts of
  Minecraft's renderer. The 2D compass HUD gets you the practical
  "which way, how far" info without that risk.
- **Pause menu buttons** — only Back to Game / Options / Nebula Menu /
  Disconnect. Advancements, Statistics, Multiplayer, and Player Reporting
  each need their own exact class name verified; adding them later is
  straightforward once the core screen swap is confirmed working.
- **F3 username line, repositioning the vanilla boss bar, reskinning
  vanilla buttons black** — all three need Mixins (directly patching
  Minecraft's internal rendering methods). That's the highest-risk category
  of modding, and I have no way to test a Mixin target from here — a wrong
  one means the mod fails to load at all, not just a broken feature. Worth
  doing as a focused follow-up once everything above is confirmed stable.
- **In-game account switching, "who else uses NebulaClient" tab icons** —
  both need something outside the mod itself (the launcher's stored
  credentials, and a backend server, respectively).

## Building it

```
./gradlew build
```

See the Phase 1 notes below for first-time wrapper setup if you haven't
already got this working from before.

### If the build fails — check these first

I verified most of this against Minecraft's real 1.21.11 API docs, but a
few calls I could only find reasonable evidence for, not a 100%-confirmed
signature (no way to compile-test from here). If the build errors, these
are the first places to look — each has a NOTE comment in the source
pointing at the alternative:

- `NebulaClientModClient.java` — the chat-visibility toggle
  (`client.options.getChatVisibility()`)
- `NebulaGameMenuScreen.java` / `NebulaTitleScreen.java` — the
  `OptionsScreen` import path
- `ArmorHud.java` — `stack.getEnchantments().isEmpty()` for the enchant glow

Everything else (HUD registration, KeyBinding, waypoint math, screen
mouse handling) is checked against confirmed 1.21.11 documentation or
uses APIs that have been stable for many versions.

## First-time wrapper setup (from Phase 1)
This repo doesn't include the Gradle wrapper jar. Before your first
build: `gradle wrapper --gradle-version 9.5` (or open in IntelliJ, which
generates it automatically).

## Publishing a release the launcher can download
```
git tag v0.2.0
git push --tags
```
CI builds and attaches the jar to a GitHub Release automatically. The
launcher always grabs whatever the newest release is.

