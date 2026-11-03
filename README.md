# Legion Zone Launcher (starter project)

A standalone Compose launcher + Game Zone + thermal guard + charging animation, built around the
Legion theme. Package: `com.legion.zone` · minSdk 30 · compileSdk 35 · Kotlin 2.0 · AGP 8.7 · JDK 17.

> NOT a Lawnchair fork. This is a clean starting project you can run on its own as a HOME app.
> The `perf/`, `thermal/`, `power/`, `assistant/` and `ui/ChargingScene.kt` packages are framework-agnostic,
> so you can copy them into a Lawnchair fork later. It was written without an Android SDK on hand,
> so it has NOT been compiled — expect to fix a few small errors on first sync.

## Run
1. Android Studio (latest stable) → Open → this folder. If it asks to upgrade AGP/Kotlin, accept.
2. If the Gradle wrapper jar is missing: Studio uses the properties file, or run `gradle wrapper --gradle-version 8.9`.
3. Run on the tablet, press Home, choose "Legion Zone" → Always.

## What is inside
| Area | Where |
|---|---|
| Pager: Feed (left) · Home · Game Zone | `MainActivity.kt` |
| Home (wallpaper, clock, battery, dock, swipe-up) | `ui/HomeScreen.kt` |
| App drawer + A–Z quick scroll (Latin + Arabic) | `ui/AppDrawer.kt` |
| Game Zone (English; Home / Library / Performance / Tools / Assistant) | `ui/GameZoneScreen.kt` |
| Charging scene (ring + lightning scaled by real watts) | `ui/ChargingScene.kt` |
| Charging screens | `ChargingActivity.kt` (plug-in / button) and `dream/ChargingDream.kt` (screen saver) |
| Thermal guard (headroom + battery temp, 4 levels, hysteresis) | `thermal/` |
| Perf backends (A = Settings, B = Shizuku stub) | `perf/PerfBackend.kt` |
| Assistant (tool schema, emotions, LLM hook) | `assistant/Assistant.kt` |
| Strings: English default + Arabic | `res/values`, `res/values-ar` |

## Permissions (Game Zone → Tools tab shows live status and opens each screen)
- Display over other apps, Modify system settings, Usage access, Do Not Disturb, Notifications.
- Secure settings (one time, from a PC): `adb shell pm grant com.legion.zone android.permission.WRITE_SECURE_SETTINGS`
- Android 13+ blocks some special permissions for sideloaded apps: App info → ⋮ → "Allow restricted settings".

## Charging screen
Settings → Display → Screen saver → "Legion Charging" → start "While charging". The plug-in popup
(`ChargingActivity`) needs the overlay permission and can be limited by newer Android versions.
It cannot replace the real lock screen without root / a custom ROM.

## Assets
- `drawable-nodpi/wallpaper.jpg` – clean wallpaper (already contains the character) → used on Home.
- `drawable-nodpi/girl.png` – transparent cutout → used on Game Zone and Charging.
- The white-background image you sent later was not used: it needs a transparent cutout first.

## Honest limits / next steps
1. Refresh-rate keys are AOSP names; Lenovo ZUI may ignore them. Find the real Legion Zone keys with the
   `settings list` diff method (before/after toggling a mode), then add them to `SettingsBackend`.
2. `ShizukuBackend` is a stub (delegates to level A). Commands to implement are listed in its comment.
3. Resolution scaling and per-process FPS need Shizuku/root.
4. Assistant runs on local rules until you set `AssistantEngine.llm` (use your own backend proxy, no key in the APK).
5. Character animation is breathing + glow. Face swaps by `Emotion` need the art split into layers.
6. Dock is a pill that opens the drawer; the expandable (multi-row) dock is the next feature.
7. Thermal thresholds in `ThermalGuard.tick()` are starting values. Never disable the system's throttling.
