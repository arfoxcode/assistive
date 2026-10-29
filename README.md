# Assistive Menu Tool

A floating accessibility button for Android. It displays a draggable button over any screen that opens a menu of quick system actions when tapped, similar to accessibility features available on other mobile operating systems.

---

<p align="center">
  <img src="docs/feature_graphic.jpg" width="100%" alt="Assistive Menu Tool Banner" />
</p>

<p align="center">
  <a href="https://f-droid.org/packages/com.joaohouto.assistivemenutool/" target="_blank">
    <img src="https://img.shields.io/badge/F--Droid-Available-1976D2?style=for-the-badge&logo=f-droid&logoColor=white" alt="F-Droid" />
  </a>
  <a href="https://joaohouto.github.io/assistive/" target="_blank">
    <img src="https://img.shields.io/badge/Official%20Website-Visit-black?style=for-the-badge" alt="Official Website" />
  </a>
</p>

---

## Features

- **Floating Button** - Draggable across the screen.
- **Automatic Edge Snapping** - Automatically snaps to the nearest screen edge when released.
- **Expandable Menu** - Smooth scale and fade animations.
- **10 Available Actions** - Configurable by the user.

| Action | Description |
|---|---|
| Home | Returns to the home screen. |
| Back | Performs the Back action. |
| Recent Apps | Opens the recent apps screen. |
| Lock Screen | Locks the screen (Android 9+). |
| Volume Up | Increases the volume. |
| Volume Down | Decreases the volume. |
| Screenshot | Captures the screen (Android 9+). |
| Notifications | Opens the notification shade. |
| Quick Settings | Opens the system quick settings panel. |
| Settings | Opens the app's settings screen. |

### Customization

- **Button Opacity** - Adjust the floating button's opacity from 30% to 100%.
- **Button Size** - Choose between Small, Medium, and Large.
- **Menu Actions** - Select which actions appear in the menu.
- **Custom App Logo** - Customize the app's logo to personalize its appearance.
- **Favorite Apps** - Add favorite apps for quick access directly from the assistive menu.

Settings are stored persistently using `SharedPreferences`.

---

## Requirements

| Item | Value |
|---|---|
| Minimum Android Version | Android 7.0 (API 24) |
| Target Android Version | Android 15 (API 36) |
| Kotlin | 2.2.10 |
| UI Framework | Jetpack Compose + Material 3 |

---

## Required Permissions

The app requires two special permissions that must be granted manually.

### 1. Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)

Allows the floating button to appear over other applications.

Path: **Settings → Apps → Assistive Menu Tool → Display over other apps**

### 2. Accessibility Service (`BIND_ACCESSIBILITY_SERVICE`)

Allows the app to perform global system actions such as Home, Back, Recent Apps, Lock Screen, and Screenshot.

Path: **Settings → Accessibility → Assistive Menu Tool → Enable**

> The app guides users through both permission settings from the main screen.

---

## Architecture

```text
app/src/main/java/com/joaohouto/assistivemenutool/
├── MainActivity.kt
├── FloatingButtonService.kt
├── AssistiveMenuAccessibilityService.kt
├── MenuAction.kt
├── SettingsRepository.kt
└── ui/theme/
```

### Main Components

**`FloatingButtonService`**

- Adds the floating button to `WindowManager` using `TYPE_APPLICATION_OVERLAY`.
- Handles dragging, releasing, and animated snapping to the nearest screen edge.
- Opens and closes the menu with scale animations.
- Listens for settings changes in real time using `OnSharedPreferenceChangeListener`.
- Handles volume actions through `AudioManager` and screenshot actions with a 400 ms delay.

**`AssistiveMenuAccessibilityService`**

- Uses `performGlobalAction()` to execute system actions, including Home, Back, Recent Apps, Lock Screen, Screenshot, Notifications, and Quick Settings.
- Exposes an `instance` through a companion object for communication with `FloatingButtonService`.

**`SettingsRepository`**

- Wraps `SharedPreferences` with typed properties such as `opacity`, `buttonSizeDp`, and `menuActions`.
- Serializes `menuActions` as a CSV string containing enum names.

---

## Build & Release

### Debug Build

Open the project in Android Studio and click **Run** to build and launch the debug version.

Alternatively, run:

```bash
./gradlew assembleDebug
```

### Release Build

1. Generate a signing keystore:

   ```bash
   keytool -genkey -v -keystore release.jks -alias amt -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Configure signing in `app/build.gradle.kts`:

   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("release.jks")
           storePassword = "..."
           keyAlias = "amt"
           keyPassword = "..."
       }
   }
   ```

3. Generate the APK or AAB:

   ```bash
   ./gradlew assembleRelease
   ./gradlew bundleRelease
   ```

- `assembleRelease` generates a release APK.
- `bundleRelease` generates an Android App Bundle (AAB) for Google Play.

> The release build enables R8 code shrinking (`isMinifyEnabled = true`) and unused resource removal (`isShrinkResources = true`).

---

## Credits

This project is based on the original Assistive Menu Tool project by João Couto.

The original project and its contributors retain their respective copyrights. Modifications, additional features, and other contributions should be attributed to their respective authors.

Original repository: https://github.com/joaohouto/assistive

---

## License

This project uses the MIT License. The original copyright and license notices must be retained in accordance with the license terms.

```text
MIT License

Copyright (c) 2026 João Couto

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
