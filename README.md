# Rang (रंग)

A battery indicator for people who can't read. Rang replaces "what does that tiny icon mean" with a **color you can see from across the room**, plus spoken Hindi alerts.

Built for my mother's Redmi phone.

## Features

- **Always-on color bar.** A thick bar sits just below the status bar, over every app. It gets shorter as the battery drains:

  | Color | Battery |
  |---|---|
  | 🟢 Green | 60–100% |
  | 🟡 Yellow | 35–59% |
  | 🟠 Orange | 15–34% |
  | 🔴 Red, blinking | below 15% |

  While charging, the bar slowly pulses.
- **Big home-screen widget.** A large battery picture filled with the same color, with a ⚡ while charging. It has no numbers or text.
- **Sounds.** Urgent beeps and *"फ़ोन की बैटरी कम है। फ़ोन चार्ज कर दो।"* when the battery drops below 15%, 10% and 5%. The low alert uses the alarm stream, so it plays even when the phone is on silent. A chirp plays when the charger goes in, and *"फ़ोन पूरा चार्ज हो गया।"* plays at 100%.
- **Survives restarts.** The bar comes back after a reboot or an app update.
- **One-time setup screen** for whoever installs it. It shows a ✅ checklist, has buttons that jump straight to the right MIUI settings screens, and lets you pick a bar thickness and test the sounds.
- No ads, no internet permission, about 70 KB.

## Install

1. Download `rang-vX.Y.Z.apk` from [Releases](https://github.com/NikhilBhatt-15/rang/releases) and install it (allow "install unknown apps" if asked).
2. Open **Rang** and go through the setup list:
   1. **Show over other apps:** turn it on.
   2. **Redmi/MIUI:** in *Other permissions*, allow *Display pop-up windows while running in background*.
   3. **Notifications:** allow them (the ongoing notification keeps the bar alive).
   4. **Battery:** set *No restrictions*.
   5. **Redmi/MIUI:** turn on *Autostart*.
   6. Add the **Rang widget** to the home screen.
3. Redmi only: open Recent apps, long-press the Rang card and tap the 🔒 lock.
4. If the setup screen says no Hindi voice is installed, tap **Install Hindi voice** (Google Speech Services). Without it, only the beeps play.

## Build

```bash
./gradlew testDebugUnitTest assembleRelease
```

Release signing reads `keystore.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`), which stays out of git. Without it, the release APK is unsigned.

## How it works

- `BarService` is a foreground service (type `specialUse`). It listens to the sticky `ACTION_BATTERY_CHANGED` broadcast and adds a `TYPE_APPLICATION_OVERLAY` window. The window is not touchable and has opacity 0.8, because Android 12+ only passes touches through overlays at ≤ 80% opacity. The bar fits under the status-bar inset.
- `Battery.zone()` maps a level to a color. `Battery.newlyCrossed()` decides which low thresholds have fired since the last charge; the fired thresholds are stored in prefs, so each alert plays once per discharge.
- `BatteryWidget` is a `RemoteViews` widget whose image is a bitmap drawn by `BatteryArt`. The service redraws it only when the level or charging state changes.
- `Alerts` plays `ToneGenerator` beeps, then speaks Hindi through `TextToSpeech`.
- `BootReceiver` restarts the service on `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`.

## License

MIT
