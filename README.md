<!-- SEO Meta Keywords (GitHub renders ini sebagai komentar, tapi crawler tetap baca) -->
<!-- assistive touch android, floating button android, accessibility button android, assistive menu android apk, tombol bantu android, floating accessibility button, android assistive touch alternative, system actions floating button -->

# Assistive Menu Tool

**A powerful floating accessibility button for Android** — drag it anywhere on screen, tap to open a fully customizable menu of system actions. Inspired by the AssistiveTouch feature on iOS, built natively for Android.

> Available on **F-Droid** · Open Source · No ads · No tracking · MIT License

<p align="center">
  <img src="docs/feature_graphic.jpg" width="100%" alt="Assistive Menu Tool - Floating Accessibility Button for Android" />
</p>

<p align="center">
  <a href="https://f-droid.org/packages/arfoxcode.joaohouto.assistivemenutool/" target="_blank">
    <img src="https://img.shields.io/badge/F--Droid-Available-1976D2?style=for-the-badge&logo=f-droid&logoColor=white" alt="Download on F-Droid" />
  </a>
  <a href="https://github.com/arfoxcode/assistive/releases/latest" target="_blank">
    <img src="https://img.shields.io/github/v/release/arfoxcode/assistive?style=for-the-badge&label=Latest%20Release" alt="Latest Release" />
  </a>
  <a href="https://github.com/arfoxcode/assistive/blob/main/LICENSE.md" target="_blank">
    <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="MIT License" />
  </a>
</p>

---

## What is Assistive Menu Tool?

Assistive Menu Tool is a free, open-source Android app that adds a **draggable floating button** over any screen. When tapped, it opens a quick-access menu of system actions — no hardware buttons needed.

Perfect for:
- Devices with broken or hard-to-reach hardware buttons
- One-handed phone use
- Accessibility needs
- Power users who want faster system controls

---

## Features

### Core
- 🔘 **Draggable floating button** — place it anywhere on screen
- 🧲 **Auto edge snap** — snaps to nearest edge when released
- ✨ **Smooth animations** — expand from button position with spring effect
- 🎨 **Full customization** — shape, color, border, size, opacity

### Menu Actions (21 actions available)
| Category | Actions |
|---|---|
| Navigation | Home, Back, Recents, Lock Screen, Screenshot |
| Volume | Volume Up, Volume Down, Volume Panel (full mixer) |
| Sound & Mode | Silent Mode, Vibrate Mode |
| Connectivity | Wi-Fi, Mobile Data, Bluetooth, Airplane Mode |
| Tools | Flashlight, Notifications, Quick Settings, System Panel |
| App | Favorites, Open Settings, Hide Button |

### Gestures
| Gesture | Action |
|---|---|
| Single Tap | Open menu |
| Double Tap | Lock screen |
| Long Press | Configurable (Favorites, Flashlight, etc.) |

### Customization
- **Button appearance** — circle or rounded rectangle shape, custom colors with HSV picker, border on/off
- **Button size** — free slider from 32dp to 96dp + quick presets (S/M/L/XL)
- **Menu theme** — Dark, Light, Follow System, or Custom color
- **Popup slots** — 9 configurable slots, each can be a system action or app shortcut
- **Favorite Apps** — 9 slots for quick app launch, with tile sizes 1×1, 2×2, 3×3
- **System Panel** — 9 fixed quick-control tiles (Wi-Fi, Data, BT, Airplane, Silent, Vibrate, Torch, Vol+/−)

### Reliability (v2.0+)
- 🛡️ **Device Administrator** support — prevents force stop
- 🔋 **Battery optimization exclusion** — prevents system from killing service
- 🔄 **Auto-restart** — restarts after system kill via AlarmManager
- 🚀 **Boot receiver** — auto-starts after device reboot
- 👁️ **Watchdog** — keeps service alive in background

---

## Screenshots

> _Coming soon — contributions welcome_

---

## Requirements

| Item | Value |
|---|---|
| Minimum Android | 7.0 (API 24) |
| Target Android | 15 (API 36) |
| Kotlin | 2.2.10 |
| UI | Jetpack Compose + Material 3 |

---

## Required Permissions

| Permission | Purpose |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Show floating button over other apps |
| `BIND_ACCESSIBILITY_SERVICE` | Perform system actions (Home, Back, Lock, etc.) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Keep service alive in background |
| `BIND_DEVICE_ADMIN` | Prevent force stop (optional but recommended) |
| `CAMERA` | Flashlight toggle |
| `BLUETOOTH_CONNECT` | Bluetooth panel (Android 13+) |
| `ACCESS_NOTIFICATION_POLICY` | Silent / DND mode |
| `RECEIVE_BOOT_COMPLETED` | Auto-start after reboot |
| `SCHEDULE_EXACT_ALARM` | Service restart via AlarmManager |

> **Privacy note:** The accessibility service is configured with `canRetrieveWindowContent="false"` and `eventTypes="typeWindowStateChanged"` only. It does **not** read screen content, monitor apps, or collect any data. Compatible with most banking apps.

---

## Architecture

```
app/src/main/java/arfoxcode/joaohouto/assistivemenutool/
├── MainActivity.kt                      # Main UI (Jetpack Compose)
├── FloatingButtonService.kt             # Foreground service — overlay + menu
├── AssistiveMenuAccessibilityService.kt # Accessibility service — global actions
├── MenuAction.kt                        # Enum — all 21 available actions
├── SettingsRepository.kt                # SharedPreferences wrapper
├── AppShortcut.kt                       # Data class for app shortcuts
├── BootReceiver.kt                      # Auto-start after reboot
├── ServiceRestartReceiver.kt            # AlarmManager restart receiver
├── AssistiveDeviceAdminReceiver.kt      # Device admin for protection
└── ui/theme/                            # Material 3 theme
```

---

## Build & Release

### Debug

```bash
./gradlew assembleDebug
```

### Release via GitHub Actions

Push a version tag to trigger the automated build, sign, and release workflow:

```bash
git tag v2.5
git push origin v2.5
```

The workflow will:
1. Build signed APK and AAB
2. Create a GitHub Release with both files attached
3. Calculate SHA-256 for verification

### Manual Release

1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore release.jks -alias amt \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Set GitHub Secrets: `KEYSTORE_BASE64`, `KEY_ALIAS`, `STORE_PASSWORD`, `KEY_PASSWORD`

3. Run:
   ```bash
   ./gradlew assembleRelease   # APK
   ./gradlew bundleRelease     # AAB for Google Play
   ```

---

## Contributing

Contributions are welcome. Please open an issue first to discuss what you'd like to change.

---

## Credits

Based on the original [Assistive Menu Tool](https://github.com/joaohouto/assistive) by João Couto.
Extended and maintained by [arfoxcode](https://github.com/arfoxcode).

---

## License

MIT License — see [LICENSE.md](LICENSE.md)

```
Copyright (c) 2026 João Couto
Copyright (c) 2026 arfoxcode
```
