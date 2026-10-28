package com.joaohouto.assistivemenutool

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class SettingsRepository(context: Context) {

    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Existing settings ────────────────────────────────────────────────────

    var opacity: Float
        get() = prefs.getFloat(KEY_OPACITY, DEFAULT_OPACITY)
        set(value) = prefs.edit().putFloat(KEY_OPACITY, value).apply()

    var buttonSizeDp: Int
        get() = prefs.getInt(KEY_BUTTON_SIZE, DEFAULT_SIZE_DP)
        set(value) = prefs.edit().putInt(KEY_BUTTON_SIZE, value).apply()

    var menuActions: List<MenuAction>
        get() {
            val stored = prefs.getString(KEY_MENU_ACTIONS, null) ?: return DEFAULT_ACTIONS
            return stored.split(",")
                .mapNotNull { name -> MenuAction.entries.find { it.name == name } }
                .ifEmpty { DEFAULT_ACTIONS }
        }
        set(value) = prefs.edit()
            .putString(KEY_MENU_ACTIONS, value.joinToString(",") { it.name })
            .apply()

    // ── Button appearance ────────────────────────────────────────────────────

    /** ButtonShape: "circle" atau "rounded_rect" */
    var buttonShape: String
        get() = prefs.getString(KEY_BUTTON_SHAPE, SHAPE_CIRCLE) ?: SHAPE_CIRCLE
        set(value) = prefs.edit().putString(KEY_BUTTON_SHAPE, value).apply()

    /** Warna outer button, disimpan sebagai ARGB Int */
    var outerColor: Int
        get() = prefs.getInt(KEY_OUTER_COLOR, DEFAULT_OUTER_COLOR)
        set(value) = prefs.edit().putInt(KEY_OUTER_COLOR, value).apply()

    /** Warna inner dot, disimpan sebagai ARGB Int */
    var innerColor: Int
        get() = prefs.getInt(KEY_INNER_COLOR, DEFAULT_INNER_COLOR)
        set(value) = prefs.edit().putInt(KEY_INNER_COLOR, value).apply()

    /** Border aktif atau tidak */
    var borderEnabled: Boolean
        get() = prefs.getBoolean(KEY_BORDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BORDER_ENABLED, value).apply()

    /** Warna border */
    var borderColor: Int
        get() = prefs.getInt(KEY_BORDER_COLOR, DEFAULT_BORDER_COLOR)
        set(value) = prefs.edit().putInt(KEY_BORDER_COLOR, value).apply()

    /** Ketebalan border dalam dp */
    var borderWidthDp: Int
        get() = prefs.getInt(KEY_BORDER_WIDTH, DEFAULT_BORDER_WIDTH_DP)
        set(value) = prefs.edit().putInt(KEY_BORDER_WIDTH, value).apply()

    companion object {
        private const val PREFS_NAME = "at_settings"

        // Existing keys
        const val KEY_OPACITY      = "opacity"
        const val KEY_BUTTON_SIZE  = "button_size"
        const val KEY_MENU_ACTIONS = "menu_actions"

        // New appearance keys
        const val KEY_BUTTON_SHAPE   = "button_shape"
        const val KEY_OUTER_COLOR    = "outer_color"
        const val KEY_INNER_COLOR    = "inner_color"
        const val KEY_BORDER_ENABLED = "border_enabled"
        const val KEY_BORDER_COLOR   = "border_color"
        const val KEY_BORDER_WIDTH   = "border_width"

        // Shape constants
        const val SHAPE_CIRCLE       = "circle"
        const val SHAPE_ROUNDED_RECT = "rounded_rect"

        // Defaults
        const val DEFAULT_OPACITY        = 0.85f
        const val DEFAULT_SIZE_DP        = 56
        val DEFAULT_OUTER_COLOR          = Color.argb(204, 20, 20, 20)
        val DEFAULT_INNER_COLOR          = Color.WHITE
        val DEFAULT_BORDER_COLOR         = Color.WHITE
        const val DEFAULT_BORDER_WIDTH_DP = 2

        val SIZE_OPTIONS = listOf(48, 56, 68)
        val SIZE_LABEL_RES = listOf(
            R.string.size_small,
            R.string.size_medium,
            R.string.size_large
        )
        val DEFAULT_ACTIONS = listOf(
            MenuAction.HOME, MenuAction.BACK,
            MenuAction.RECENTS, MenuAction.LOCK
        )
    }
}
