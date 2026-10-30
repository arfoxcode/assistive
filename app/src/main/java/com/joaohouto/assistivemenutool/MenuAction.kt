package com.joaohouto.assistivemenutool

enum class MenuAction {
    // System navigation
    HOME, BACK, RECENTS, LOCK,

    // Volume
    VOLUME_UP, VOLUME_DOWN,

    // Media
    SCREENSHOT, NOTIFICATIONS, QUICK_SETTINGS,

    // App
    OPEN_APP, FAVORITES,

    // NEW: Toggle actions
    HIDE_BUTTON,    // sembunyikan floating button sementara
    FLASHLIGHT,     // senter on/off
    WIFI,           // buka wifi panel
    CELLULAR,       // buka cellular settings
    BLUETOOTH,      // buka bluetooth panel
    SILENT_MODE,    // mode senyap (DND)
    VIBRATE_MODE,   // mode getar
    VOLUME_PANEL,   // buka volume mixer lengkap
    SYSTEM_PANEL;   // panel quick controls (9 tile fix)

    fun iconRes(): Int = when (this) {
        HOME           -> R.drawable.ic_home
        BACK           -> R.drawable.ic_back
        RECENTS        -> R.drawable.ic_recents
        LOCK           -> R.drawable.ic_lock
        VOLUME_UP      -> R.drawable.ic_volume_up
        VOLUME_DOWN    -> R.drawable.ic_volume_down
        SCREENSHOT     -> R.drawable.ic_screenshot
        NOTIFICATIONS  -> R.drawable.ic_notifications
        QUICK_SETTINGS -> R.drawable.ic_quick_settings
        OPEN_APP       -> R.drawable.ic_open_app
        FAVORITES      -> R.drawable.ic_favorites
        HIDE_BUTTON    -> R.drawable.ic_hide
        FLASHLIGHT     -> R.drawable.ic_flashlight
        WIFI           -> R.drawable.ic_wifi
        CELLULAR       -> R.drawable.ic_cellular
        BLUETOOTH      -> R.drawable.ic_bluetooth
        SILENT_MODE    -> R.drawable.ic_silent
        VIBRATE_MODE   -> R.drawable.ic_vibrate
        VOLUME_PANEL   -> R.drawable.ic_volume_panel
        SYSTEM_PANEL   -> R.drawable.ic_system_panel
    }

    fun labelRes(): Int = when (this) {
        HOME           -> R.string.action_home
        BACK           -> R.string.action_back
        RECENTS        -> R.string.action_recents
        LOCK           -> R.string.action_lock
        VOLUME_UP      -> R.string.action_volume_up
        VOLUME_DOWN    -> R.string.action_volume_down
        SCREENSHOT     -> R.string.action_screenshot
        NOTIFICATIONS  -> R.string.action_notifications
        QUICK_SETTINGS -> R.string.action_quick_settings
        OPEN_APP       -> R.string.action_open_app
        FAVORITES      -> R.string.action_favorites
        HIDE_BUTTON    -> R.string.action_hide_button
        FLASHLIGHT     -> R.string.action_flashlight
        WIFI           -> R.string.action_wifi
        CELLULAR       -> R.string.action_cellular
        BLUETOOTH      -> R.string.action_bluetooth
        SILENT_MODE    -> R.string.action_silent_mode
        VIBRATE_MODE   -> R.string.action_vibrate_mode
        VOLUME_PANEL   -> R.string.action_volume_panel
        SYSTEM_PANEL   -> R.string.action_system_panel
    }
}
