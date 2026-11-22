package arfoxcode.joaohouto.assistivemenutool

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

    // ── Button appearance — shape & solid color ──────────────────────────────

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

    // ── Gradient ─────────────────────────────────────────────────────────────

    /**
     * Apakah gradient aktif. Jika true, outerColor tetap dipakai sebagai
     * warna pertama; gradientColor2 adalah warna kedua.
     */
    var gradientEnabled: Boolean
        get() = prefs.getBoolean(KEY_GRADIENT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_GRADIENT_ENABLED, value).apply()

    var gradientColor2: Int
        get() = prefs.getInt(KEY_GRADIENT_COLOR2, DEFAULT_GRADIENT_COLOR2)
        set(value) = prefs.edit().putInt(KEY_GRADIENT_COLOR2, value).apply()

    /**
     * Tipe gradient: "linear_tb" | "linear_lr" | "linear_diag" |
     *                "linear_diag2" | "radial"
     */
    var gradientType: String
        get() = prefs.getString(KEY_GRADIENT_TYPE, GRADIENT_LINEAR_TB) ?: GRADIENT_LINEAR_TB
        set(value) = prefs.edit().putString(KEY_GRADIENT_TYPE, value).apply()

    // ── Icon mode ─────────────────────────────────────────────────────────────

    /**
     * Mode icon tengah tombol:
     * "dot"     → titik bulat (perilaku lama)
     * "preset"  → vector icon dari bundled presets
     * "custom"  → gambar yang diupload user dari galeri
     */
    var iconMode: String
        get() = prefs.getString(KEY_ICON_MODE, ICON_MODE_DOT) ?: ICON_MODE_DOT
        set(value) = prefs.edit().putString(KEY_ICON_MODE, value).apply()

    /**
     * Nama preset icon yang dipilih. Harus cocok dengan salah satu
     * nilai di [PRESET_ICONS].
     */
    var iconPreset: String
        get() = prefs.getString(KEY_ICON_PRESET, PRESET_ICONS.first().id) ?: PRESET_ICONS.first().id
        set(value) = prefs.edit().putString(KEY_ICON_PRESET, value).apply()

    /**
     * Path file absolut ke gambar custom yang sudah diproses (256×256 PNG)
     * di filesDir. Kosong jika belum pernah upload.
     */
    var iconCustomPath: String
        get() = prefs.getString(KEY_ICON_CUSTOM_PATH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ICON_CUSTOM_PATH, value).apply()

    // ── Idle animation ────────────────────────────────────────────────────────

    /**
     * Gaya animasi idle tombol:
     * "none" | "pulse" | "breathing" | "bounce" | "glow"
     */
    var animationStyle: String
        get() = prefs.getString(KEY_ANIMATION_STYLE, ANIM_NONE) ?: ANIM_NONE
        set(value) = prefs.edit().putString(KEY_ANIMATION_STYLE, value).apply()

    // ── Menu theme ───────────────────────────────────────────────────────────

    var menuTheme: String
        get() = prefs.getString(KEY_MENU_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_MENU_THEME, value).apply()

    var menuCustomColor: Int
        get() = prefs.getInt(KEY_MENU_CUSTOM_COLOR, Color.argb(245, 30, 30, 35))
        set(value) = prefs.edit().putInt(KEY_MENU_CUSTOM_COLOR, value).apply()

    // ── Gesture settings ─────────────────────────────────────────────────────

    var longPressAction: MenuAction
        get() {
            val stored = prefs.getString(KEY_LONG_PRESS_ACTION, null)
            return MenuAction.entries.find { it.name == stored } ?: DEFAULT_LONG_PRESS_ACTION
        }
        set(value) = prefs.edit().putString(KEY_LONG_PRESS_ACTION, value.name).apply()

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

    // ── Preset icon catalog ───────────────────────────────────────────────────

    data class PresetIcon(val id: String, val labelRes: Int, val drawableRes: Int)

    companion object {
        private const val PREFS_NAME     = "at_settings"
        private const val SLOT_SEPARATOR = "^^^"

        // Existing keys
        const val KEY_OPACITY        = "opacity"
        const val KEY_BUTTON_SIZE    = "button_size"
        const val KEY_MENU_ACTIONS   = "menu_actions"
        const val KEY_BUTTON_SHAPE   = "button_shape"
        const val KEY_OUTER_COLOR    = "outer_color"
        const val KEY_INNER_COLOR    = "inner_color"
        const val KEY_BORDER_ENABLED = "border_enabled"
        const val KEY_BORDER_COLOR   = "border_color"
        const val KEY_BORDER_WIDTH   = "border_width"
        const val KEY_POPUP_SLOTS       = "popup_slots"
        const val KEY_FAVORITE_SLOTS    = "favorite_slots"
        const val KEY_LONG_PRESS_ACTION = "long_press_action"
        const val KEY_MENU_THEME        = "menu_theme"
        const val KEY_MENU_CUSTOM_COLOR = "menu_custom_color"

        // New keys — gradient
        const val KEY_GRADIENT_ENABLED = "gradient_enabled"
        const val KEY_GRADIENT_COLOR2  = "gradient_color2"
        const val KEY_GRADIENT_TYPE    = "gradient_type"

        // New keys — icon
        const val KEY_ICON_MODE        = "icon_mode"
        const val KEY_ICON_PRESET      = "icon_preset"
        const val KEY_ICON_CUSTOM_PATH = "icon_custom_path"

        // New keys — animation
        const val KEY_ANIMATION_STYLE  = "animation_style"

        // Gradient types
        const val GRADIENT_LINEAR_TB   = "linear_tb"    // top → bottom
        const val GRADIENT_LINEAR_LR   = "linear_lr"    // left → right
        const val GRADIENT_LINEAR_DIAG = "linear_diag"  // top-left → bottom-right
        const val GRADIENT_LINEAR_DIAG2= "linear_diag2" // top-right → bottom-left
        const val GRADIENT_RADIAL      = "radial"       // center → edge

        // Icon modes
        const val ICON_MODE_DOT    = "dot"
        const val ICON_MODE_PRESET = "preset"
        const val ICON_MODE_CUSTOM = "custom"

        // Animation styles
        const val ANIM_NONE      = "none"
        const val ANIM_PULSE     = "pulse"
        const val ANIM_BREATHING = "breathing"
        const val ANIM_BOUNCE    = "bounce"
        const val ANIM_GLOW      = "glow"

        // Theme
        const val THEME_DARK   = "dark"
        const val THEME_LIGHT  = "light"
        const val THEME_SYSTEM = "system"
        const val THEME_CUSTOM = "custom"

        // Shape
        const val SHAPE_CIRCLE       = "circle"
        const val SHAPE_ROUNDED_RECT = "rounded_rect"

        // Defaults
        const val DEFAULT_OPACITY         = 0.85f
        const val DEFAULT_SIZE_DP         = 56
        const val MIN_SIZE_DP             = 32
        const val MAX_SIZE_DP             = 96
        val DEFAULT_OUTER_COLOR           = Color.argb(204, 20, 20, 20)
        val DEFAULT_INNER_COLOR           = Color.WHITE
        val DEFAULT_BORDER_COLOR          = Color.WHITE
        const val DEFAULT_BORDER_WIDTH_DP = 2
        val DEFAULT_GRADIENT_COLOR2       = Color.argb(204, 60, 60, 180)
        val DEFAULT_LONG_PRESS_ACTION     = MenuAction.FAVORITES

        val DEFAULT_ACTIONS = listOf(
            MenuAction.HOME, MenuAction.BACK,
            MenuAction.RECENTS, MenuAction.LOCK
        )

        /**
         * Daftar 15 preset icon yang di-bundle sebagai vector XML.
         * drawableRes harus sesuai dengan file di res/drawable/.
         */
        val PRESET_ICONS: List<PresetIcon> by lazy {
            listOf(
                PresetIcon("ic_preset_star",       R.string.preset_icon_star,       R.drawable.ic_preset_star),
                PresetIcon("ic_preset_bolt",       R.string.preset_icon_bolt,       R.drawable.ic_preset_bolt),
                PresetIcon("ic_preset_hand",       R.string.preset_icon_hand,       R.drawable.ic_preset_hand),
                PresetIcon("ic_preset_touch",      R.string.preset_icon_touch,      R.drawable.ic_preset_touch),
                PresetIcon("ic_preset_menu",       R.string.preset_icon_menu,       R.drawable.ic_preset_menu),
                PresetIcon("ic_preset_plus",       R.string.preset_icon_plus,       R.drawable.ic_preset_plus),
                PresetIcon("ic_preset_circle",     R.string.preset_icon_circle,     R.drawable.ic_preset_circle),
                PresetIcon("ic_preset_diamond",    R.string.preset_icon_diamond,    R.drawable.ic_preset_diamond),
                PresetIcon("ic_preset_heart",      R.string.preset_icon_heart,      R.drawable.ic_preset_heart),
                PresetIcon("ic_preset_leaf",       R.string.preset_icon_leaf,       R.drawable.ic_preset_leaf),
                PresetIcon("ic_preset_fire",       R.string.preset_icon_fire,       R.drawable.ic_preset_fire),
                PresetIcon("ic_preset_shield",     R.string.preset_icon_shield,     R.drawable.ic_preset_shield),
                PresetIcon("ic_preset_rocket",     R.string.preset_icon_rocket,     R.drawable.ic_preset_rocket),
                PresetIcon("ic_preset_wave",       R.string.preset_icon_wave,       R.drawable.ic_preset_wave),
                PresetIcon("ic_preset_infinity",   R.string.preset_icon_infinity,   R.drawable.ic_preset_infinity)
            )
        }
    }
}
