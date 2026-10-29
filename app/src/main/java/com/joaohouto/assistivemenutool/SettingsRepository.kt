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

    // Custom size dp — slider bebas 32..96
    var buttonSizeDp: Int
        get() = prefs.getInt(KEY_BUTTON_SIZE, DEFAULT_SIZE_DP)
        set(value) = prefs.edit().putInt(KEY_BUTTON_SIZE, value.coerceIn(MIN_SIZE_DP, MAX_SIZE_DP)).apply()

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

    var buttonShape: String
        get() = prefs.getString(KEY_BUTTON_SHAPE, SHAPE_CIRCLE) ?: SHAPE_CIRCLE
        set(value) = prefs.edit().putString(KEY_BUTTON_SHAPE, value).apply()

    var outerColor: Int
        get() = prefs.getInt(KEY_OUTER_COLOR, DEFAULT_OUTER_COLOR)
        set(value) = prefs.edit().putInt(KEY_OUTER_COLOR, value).apply()

    var innerColor: Int
        get() = prefs.getInt(KEY_INNER_COLOR, DEFAULT_INNER_COLOR)
        set(value) = prefs.edit().putInt(KEY_INNER_COLOR, value).apply()

    var borderEnabled: Boolean
        get() = prefs.getBoolean(KEY_BORDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BORDER_ENABLED, value).apply()

    var borderColor: Int
        get() = prefs.getInt(KEY_BORDER_COLOR, DEFAULT_BORDER_COLOR)
        set(value) = prefs.edit().putInt(KEY_BORDER_COLOR, value).apply()

    var borderWidthDp: Int
        get() = prefs.getInt(KEY_BORDER_WIDTH, DEFAULT_BORDER_WIDTH_DP)
        set(value) = prefs.edit().putInt(KEY_BORDER_WIDTH, value).apply()

    // ── Popup slots ──────────────────────────────────────────────────────────

    var popupSlots: List<PopupSlot>
        get() {
            val stored = prefs.getString(KEY_POPUP_SLOTS, null)
            if (stored.isNullOrEmpty()) return defaultPopupSlots()
            return stored.split(SLOT_SEPARATOR)
                .mapIndexed { i, raw -> PopupSlot.deserialize(raw) ?: PopupSlot.empty(i) }
                .let { list -> List(9) { i -> list.getOrElse(i) { PopupSlot.empty(i) } } }
        }
        set(value) = prefs.edit()
            .putString(KEY_POPUP_SLOTS, value.take(9).joinToString(SLOT_SEPARATOR) { it.serialize() })
            .apply()

    // ── Favorite slots ───────────────────────────────────────────────────────

    var favoriteSlots: List<AppShortcut>
        get() {
            val stored = prefs.getString(KEY_FAVORITE_SLOTS, null)
            if (stored.isNullOrEmpty()) return List(9) { AppShortcut.empty(it) }
            return stored.split(SLOT_SEPARATOR)
                .mapIndexed { i, raw -> AppShortcut.deserialize(raw) ?: AppShortcut.empty(i) }
                .let { list -> List(9) { i -> list.getOrElse(i) { AppShortcut.empty(i) } } }
        }
        set(value) = prefs.edit()
            .putString(KEY_FAVORITE_SLOTS, value.take(9).joinToString(SLOT_SEPARATOR) { it.serialize() })
            .apply()

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun defaultPopupSlots(): List<PopupSlot> {
        val defaults = listOf(MenuAction.HOME, MenuAction.BACK, MenuAction.RECENTS, MenuAction.LOCK)
        return List(9) { i ->
            if (i < defaults.size) PopupSlot.fromAction(defaults[i], i)
            else PopupSlot.empty(i)
        }
    }

    companion object {
        private const val PREFS_NAME     = "at_settings"
        private const val SLOT_SEPARATOR = "^^^"

        const val KEY_OPACITY        = "opacity"
        const val KEY_BUTTON_SIZE    = "button_size"
        const val KEY_MENU_ACTIONS   = "menu_actions"
        const val KEY_BUTTON_SHAPE   = "button_shape"
        const val KEY_OUTER_COLOR    = "outer_color"
        const val KEY_INNER_COLOR    = "inner_color"
        const val KEY_BORDER_ENABLED = "border_enabled"
        const val KEY_BORDER_COLOR   = "border_color"
        const val KEY_BORDER_WIDTH   = "border_width"
        const val KEY_POPUP_SLOTS    = "popup_slots"
        const val KEY_FAVORITE_SLOTS = "favorite_slots"

        const val SHAPE_CIRCLE       = "circle"
        const val SHAPE_ROUNDED_RECT = "rounded_rect"

        const val DEFAULT_OPACITY         = 0.85f
        const val DEFAULT_SIZE_DP         = 56
        const val MIN_SIZE_DP             = 32
        const val MAX_SIZE_DP             = 96
        val DEFAULT_OUTER_COLOR           = Color.argb(204, 20, 20, 20)
        val DEFAULT_INNER_COLOR           = Color.WHITE
        val DEFAULT_BORDER_COLOR          = Color.WHITE
        const val DEFAULT_BORDER_WIDTH_DP = 2

        val DEFAULT_ACTIONS = listOf(
            MenuAction.HOME, MenuAction.BACK,
            MenuAction.RECENTS, MenuAction.LOCK
        )
    }
}
