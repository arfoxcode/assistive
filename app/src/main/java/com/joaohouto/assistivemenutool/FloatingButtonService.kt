package com.joaohouto.assistivemenutool

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

class FloatingButtonService : Service() {

    // ── State ────────────────────────────────────────────────────────────────

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var settings: SettingsRepository

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private var menuOverlayView: View? = null
    private var favoritesOverlayView: View? = null
    private var systemPanelView: View? = null
    private var volumePanelView: View? = null
    private var isFlashlightOn = false
    private var isButtonHidden = false

    // ── Gesture tracking ─────────────────────────────────────────────────────
    private val gestureHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var lastTapTime   = 0L
    private var tapCount      = 0
    private var longPressRunnable: Runnable? = null
    private var isLongPress   = false

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            SettingsRepository.KEY_OPACITY       -> floatingView.alpha = settings.opacity
            SettingsRepository.KEY_BUTTON_SIZE,
            SettingsRepository.KEY_BUTTON_SHAPE,
            SettingsRepository.KEY_OUTER_COLOR,
            SettingsRepository.KEY_INNER_COLOR,
            SettingsRepository.KEY_BORDER_ENABLED,
            SettingsRepository.KEY_BORDER_COLOR,
            SettingsRepository.KEY_BORDER_WIDTH  -> rebuildButton()
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        settings = SettingsRepository(this)
        settings.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        rebuildButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        // Matikan senter jika masih nyala
        if (isFlashlightOn) try { toggleFlashlight() } catch (_: Exception) {}
        settings.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        // Cleanup semua overlay
        listOf(systemPanelView, volumePanelView, favoritesOverlayView, menuOverlayView).forEach {
            it?.let { v -> try { windowManager.removeView(v) } catch (_: Exception) {} }
        }
        systemPanelView = null; volumePanelView = null
        favoritesOverlayView = null; menuOverlayView = null
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
        super.onDestroy()
    }

    // ── Floating button ──────────────────────────────────────────────────────

    private fun rebuildButton() {
        val savedX = if (::params.isInitialized) params.x else 0
        val savedY = if (::params.isInitialized) params.y else dpToPx(200)

        if (::floatingView.isInitialized) {
            floatingView.setOnTouchListener(null)
            windowManager.removeView(floatingView)
        }

        val sizePx = dpToPx(settings.buttonSizeDp)
        floatingView = buildButtonView(sizePx)

        params = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        windowManager.addView(floatingView, params)
        floatingView.alpha = settings.opacity
        floatingView.setOnTouchListener(::onTouch)
    }

    private fun buildButtonView(sizePx: Int): View {
        val shape        = settings.buttonShape
        val outerColor   = settings.outerColor
        val innerColor   = settings.innerColor
        val borderEnabled = settings.borderEnabled
        val borderColor  = settings.borderColor
        val borderWidthPx = dpToPx(settings.borderWidthDp)

        val cornerRadius = when (shape) {
            SettingsRepository.SHAPE_CIRCLE       -> sizePx / 2f
            SettingsRepository.SHAPE_ROUNDED_RECT -> sizePx * 0.28f
            else                                   -> sizePx / 2f
        }

        val outerBg = GradientDrawable().apply {
            this.shape = GradientDrawable.RECTANGLE
            setColor(outerColor)
            setCornerRadius(cornerRadius)
            if (borderEnabled) setStroke(borderWidthPx, borderColor)
        }

        // Inner dot selalu lingkaran sempurna
        val innerBg = GradientDrawable().apply {
            this.shape = GradientDrawable.OVAL
            setColor(innerColor)
        }

        val innerSizePx = (sizePx * 0.38f).toInt()
        val innerView = View(this).apply {
            background = innerBg
            layoutParams = FrameLayout.LayoutParams(innerSizePx, innerSizePx, Gravity.CENTER)
        }

        return FrameLayout(this).apply {
            background = outerBg
            elevation = dpToPx(6).toFloat()
            addView(innerView)
        }
    }

    // ── Touch handling ───────────────────────────────────────────────────────

    private fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX      = params.x; initialY = params.y
                initialTouchX = event.rawX; initialTouchY = event.rawY
                isDragging    = false
                isLongPress   = false

                floatingView.animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).start()

                // Jadwalkan long press
                val lp = Runnable {
                    if (!isDragging) {
                        isLongPress = true
                        floatingView.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                        // Haptic feedback ringan lewat performHapticFeedback
                        floatingView.performHapticFeedback(
                            android.view.HapticFeedbackConstants.LONG_PRESS
                        )
                        onLongPress()
                    }
                }
                longPressRunnable = lp
                gestureHandler.postDelayed(lp, LONG_PRESS_MS)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                if (!isDragging && (kotlin.math.abs(dx) > DRAG_THRESHOLD ||
                                    kotlin.math.abs(dy) > DRAG_THRESHOLD)) {
                    isDragging = true
                    // Batalkan long press jika mulai drag
                    longPressRunnable?.let { gestureHandler.removeCallbacks(it) }
                    longPressRunnable = null
                }
                if (isDragging) {
                    params.x = initialX + dx; params.y = initialY + dy
                    windowManager.updateViewLayout(floatingView, params)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                floatingView.animate().scaleX(1f).scaleY(1f).setDuration(80).start()

                // Batalkan long press jika jari diangkat lebih cepat
                longPressRunnable?.let { gestureHandler.removeCallbacks(it) }
                longPressRunnable = null

                when {
                    isDragging    -> snapToEdge()
                    isLongPress   -> { /* sudah dihandle di Runnable */ }
                    else          -> onTap()
                }
            }
        }
        return true
    }

    // ── Gesture actions ───────────────────────────────────────────────────────

    private fun onTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < DOUBLE_TAP_MS) {
            // Double tap — batalkan pending single tap, lakukan double tap action
            tapCount = 0
            lastTapTime = 0L
            gestureHandler.removeCallbacksAndMessages(SINGLE_TAP_TOKEN)
            onDoubleTap()
        } else {
            // Mungkin single tap — tunggu sebentar untuk cek apakah ada tap kedua
            tapCount = 1
            lastTapTime = now
            gestureHandler.postAtTime({
                if (tapCount == 1) {
                    tapCount = 0
                    onSingleTap()
                }
            }, SINGLE_TAP_TOKEN, android.os.SystemClock.uptimeMillis() + DOUBLE_TAP_MS)
        }
    }

    private fun onSingleTap() {
        // Single tap → buka/tutup menu
        if (menuOverlayView != null) hideMenu() else showMenu()
    }

    private fun onDoubleTap() {
        // Double tap → lock screen
        if (menuOverlayView != null) hideMenu()
        AssistiveMenuAccessibilityService.instance?.execute(MenuAction.LOCK)
        // Animasi feedback
        floatingView.animate().scaleX(0.8f).scaleY(0.8f).setDuration(80).withEndAction {
            floatingView.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
        }.start()
    }

    private fun onLongPress() {
        // Long press → action yang dikonfigurasi user
        if (menuOverlayView != null) hideMenu()
        executeLongPressAction(settings.longPressAction)
    }

    private fun executeLongPressAction(action: MenuAction) {
        executeAction(action)
    }

    private fun snapToEdge() {
        val screenWidth  = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val sizePx   = dpToPx(settings.buttonSizeDp)
        val targetX  = if (params.x + sizePx / 2 < screenWidth / 2) 0 else screenWidth - sizePx
        val targetY  = params.y.coerceIn(0, screenHeight - sizePx)
        val startX   = params.x; val startY = params.y

        ValueAnimator.ofFloat(0f, 1f).apply {
            duration     = SNAP_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val f = anim.animatedValue as Float
                params.x = (startX + (targetX - startX) * f).toInt()
                params.y = (startY + (targetY - startY) * f).toInt()
                try { windowManager.updateViewLayout(floatingView, params) } catch (_: Exception) {}
            }
            start()
        }
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    private fun showMenu() {
        val overlay = buildMenuOverlay()
        windowManager.addView(overlay, WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ))
        menuOverlayView = overlay
        val frame = overlay as FrameLayout
        frame.getChildAt(0).also { it.alpha = 0f; it.animate().alpha(1f).setDuration(MENU_SHOW_MS).start() }
        frame.getChildAt(1).also {
            it.scaleX = 0.75f; it.scaleY = 0.75f; it.alpha = 0f
            it.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(MENU_SHOW_MS).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    private fun hideMenu() {
        val overlay = menuOverlayView ?: return
        dismissFavoritesImmediate()
        val frame = overlay as FrameLayout
        frame.getChildAt(1).animate()
            .scaleX(0.75f).scaleY(0.75f).alpha(0f).setDuration(MENU_HIDE_MS).start()
        frame.getChildAt(0).animate().alpha(0f).setDuration(MENU_HIDE_MS)
            .withEndAction {
                try { windowManager.removeView(overlay) } catch (_: Exception) {}
                menuOverlayView = null
            }.start()
    }

    private fun dismissMenuImmediate() {
        menuOverlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            menuOverlayView = null
        }
    }

    private fun buildMenuOverlay(): FrameLayout {
        val overlay = FrameLayout(this)
        val dim = View(this).apply {
            setBackgroundColor(Color.argb(110, 0, 0, 0))
            setOnClickListener { hideMenu() }
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        val panel = buildMenuPanel()
        panel.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
            .also { it.setMargins(dpToPx(32), 0, dpToPx(32), 0) }
        overlay.addView(dim)
        overlay.addView(panel)
        return overlay
    }

    // ── Menu panel — modern grid, slot kosong tidak ditampilkan ─────────────

    private fun buildMenuPanel(): View {
        // Hanya slot yang terisi
        val slots = settings.popupSlots.filter { !it.isEmpty }

        // Panel background: dark glass
        val bg = GradientDrawable().apply {
            shape        = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(28).toFloat()
            setColor(Color.argb(245, 22, 22, 26))
        }

        val pad = dpToPx(14)
        val container = FrameLayout(this).apply {
            background = bg
            setPadding(pad, pad, pad, pad)
            // Soft shadow via elevation
            elevation = dpToPx(12).toFloat()
        }

        val gridView = buildMixedGrid(slots)
        container.addView(gridView)
        return container
    }

    /**
     * Grid 3 kolom, slot kosong dilewati.
     * Tile 2×2 dan 3×3 span beberapa cell.
     * Gap antar tile sebesar GAP_DP.
     */
    private fun buildMixedGrid(slots: List<PopupSlot>): View {
        val cellPx  = dpToPx(MENU_ITEM_SIZE_DP)
        val gapPx   = dpToPx(GAP_DP)
        val cols    = 3
        val rows    = 3
        val totalPx = cellPx * cols + gapPx * (cols - 1)

        val grid = android.widget.RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(totalPx, totalPx)
        }

        // Posisi tile ditentukan oleh slotIndex — bukan sequential scan
        // slotIndex 0..8 → row = idx/3, col = idx%3
        for (slot in slots) {
            val idx  = slot.slotIndex.coerceIn(0, 8)
            val row  = idx / cols
            val col  = idx % cols

            // Span dibatasi supaya tidak keluar batas grid dari posisi ini
            val span = slot.tileSize.coerceIn(1, minOf(3, cols - col, rows - row))

            val tilePx   = cellPx * span + gapPx * (span - 1)
            val tileView = buildTileView(slot, tilePx)

            val lp = android.widget.RelativeLayout.LayoutParams(tilePx, tilePx).apply {
                leftMargin = col * (cellPx + gapPx)
                topMargin  = row * (cellPx + gapPx)
            }
            tileView.layoutParams = lp
            grid.addView(tileView)
        }
        return grid
    }

    private fun buildTileView(slot: PopupSlot, sizePx: Int): View {
        val isLarge  = sizePx > dpToPx(MENU_ITEM_SIZE_DP)
        val iconSize = (sizePx * 0.46f).toInt()
        val iconPad  = (sizePx * 0.13f).toInt()
        val corner   = dpToPx(18).toFloat()
        val margin   = dpToPx(3)

        // Tile background: frosted dark surface
        val tileBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = corner
            setColor(Color.argb(80, 255, 255, 255))
        }

        // Icon container: lebih besar, rounded square bukan circle
        val iconContainerBg = GradientDrawable().apply {
            shape        = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(12).toFloat()
            setColor(Color.argb(60, 255, 255, 255))
        }

        val icon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(iconPad, iconPad, iconPad, iconPad)
            background = iconContainerBg
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                .also { it.gravity = Gravity.CENTER_HORIZONTAL }

            when (slot.type) {
                PopupSlot.SlotType.ACTION -> slot.action?.let {
                    setImageResource(it.iconRes())
                    setColorFilter(Color.WHITE)
                }
                PopupSlot.SlotType.SHORTCUT -> loadShortcutIcon(this, slot.shortcut)
                else -> {}
            }
        }

        val labelText = when (slot.type) {
            PopupSlot.SlotType.ACTION   -> slot.action?.let { getString(it.labelRes()) } ?: ""
            PopupSlot.SlotType.SHORTCUT -> slot.shortcut?.label
                ?: slot.shortcut?.packageName?.let { getAppLabel(it) } ?: ""
            else -> ""
        }

        val label = TextView(this).apply {
            text      = labelText
            textSize  = if (isLarge) 13f else 11f
            setTextColor(Color.WHITE)
            gravity   = Gravity.CENTER
            maxLines  = 1
            // Sedikit bold supaya terbaca di atas background gelap
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                .also { it.gravity = Gravity.CENTER_HORIZONTAL; it.topMargin = dpToPx(6) }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            background  = tileBg
            setPadding(margin, margin, margin, margin)
            addView(icon)
            addView(label)
            setOnClickListener {
                // Animasi press — scale down + ripple feel
                animate().scaleX(0.92f).scaleY(0.92f).setDuration(80).withEndAction {
                    animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
                onSlotClick(slot)
            }
        }
    }

    private fun onSlotClick(slot: PopupSlot) {
        when (slot.type) {
            PopupSlot.SlotType.ACTION -> executeAction(slot.action ?: return)
            PopupSlot.SlotType.SHORTCUT -> {
                hideMenu()
                launchApp(slot.shortcut?.packageName)
            }
            else -> {}
        }
    }

    private fun executeAction(action: MenuAction) {
        when (action) {
            MenuAction.FAVORITES     -> showFavorites()
            MenuAction.SYSTEM_PANEL  -> { hideMenu(); showSystemPanel() }
            MenuAction.VOLUME_PANEL  -> { hideMenu(); showVolumePanel() }
            MenuAction.VOLUME_UP     -> { hideMenu(); adjustVolume(AudioManager.ADJUST_RAISE) }
            MenuAction.VOLUME_DOWN   -> { hideMenu(); adjustVolume(AudioManager.ADJUST_LOWER) }
            MenuAction.OPEN_APP      -> { hideMenu(); openApp() }
            MenuAction.HIDE_BUTTON   -> { hideMenu(); toggleHideButton() }
            MenuAction.FLASHLIGHT    -> { hideMenu(); toggleFlashlight() }
            MenuAction.WIFI          -> { hideMenu(); openWifiPanel() }
            MenuAction.CELLULAR      -> { hideMenu(); openCellularSettings() }
            MenuAction.BLUETOOTH     -> { hideMenu(); openBluetoothPanel() }
            MenuAction.SILENT_MODE   -> { hideMenu(); setSilentMode() }
            MenuAction.VIBRATE_MODE  -> { hideMenu(); setVibrateMode() }
            MenuAction.SCREENSHOT    -> {
                hideMenu()
                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                handler.postDelayed({
                    floatingView.visibility = View.INVISIBLE
                    handler.postDelayed({
                        AssistiveMenuAccessibilityService.instance?.execute(action)
                        handler.postDelayed({
                            floatingView.visibility = View.VISIBLE
                            floatingView.alpha = 0f
                            floatingView.animate()
                                .alpha(settings.opacity)
                                .setDuration(SCREENSHOT_RESTORE_MS)
                                .start()
                        }, SCREENSHOT_RESTORE_MS)
                    }, SCREENSHOT_CAPTURE_MS)
                }, SCREENSHOT_HIDE_MS)
            }
            else -> {
                hideMenu()
                AssistiveMenuAccessibilityService.instance?.execute(action)
            }
        }
    }

    // ── Favorites panel ──────────────────────────────────────────────────────

    private fun showFavorites() {
        if (favoritesOverlayView != null) return
        val panel = buildFavoritesPanel()
        val lp = WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(panel, lp)
        favoritesOverlayView = panel

        panel.alpha = 0f
        panel.scaleX = 0.85f; panel.scaleY = 0.85f
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(MENU_SHOW_MS).setInterpolator(DecelerateInterpolator()).start()
    }

    private fun hideFavorites() {
        val panel = favoritesOverlayView ?: return
        panel.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(MENU_HIDE_MS)
            .withEndAction {
                try { windowManager.removeView(panel) } catch (_: Exception) {}
                favoritesOverlayView = null
            }.start()
    }

    private fun dismissFavoritesImmediate() {
        favoritesOverlayView?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
            favoritesOverlayView = null
        }
    }

    private fun buildFavoritesPanel(): FrameLayout {
        val overlay = FrameLayout(this)

        val dim = View(this).apply {
            setBackgroundColor(Color.argb(110, 0, 0, 0))
            setOnClickListener { hideFavorites() }
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        val slots   = settings.favoriteSlots
        val bg      = GradientDrawable().apply {
            shape        = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(20).toFloat()
            setColor(Color.argb(235, 28, 28, 30))
        }

        val panel = FrameLayout(this).apply {
            background = bg
            val p = dpToPx(12)
            setPadding(p, p, p, p)
        }

        val grid = buildFavoritesGrid(slots)
        panel.addView(grid)

        // Tombol close di pojok kanan atas
        val closeBtn = TextView(this).apply {
            text      = "✕"
            textSize  = 16f
            setTextColor(Color.WHITE)
            gravity   = Gravity.CENTER
            val sz    = dpToPx(32)
            layoutParams = FrameLayout.LayoutParams(sz, sz, Gravity.TOP or Gravity.END)
                .also { it.setMargins(0, dpToPx(-4), dpToPx(-4), 0) }
            setOnClickListener { hideFavorites() }
        }

        panel.addView(closeBtn)

        panel.layoutParams = FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
            .also { it.setMargins(dpToPx(32), 0, dpToPx(32), 0) }

        overlay.addView(dim)
        overlay.addView(panel)
        return overlay
    }

    private fun buildFavoritesGrid(slots: List<AppShortcut>): View {
        val cellPx   = dpToPx(MENU_ITEM_SIZE_DP)
        val cols     = 3
        val rows     = 3
        val occupied = Array(rows) { BooleanArray(cols) }

        val grid = android.widget.RelativeLayout(this).apply {
            val size = cellPx * cols
            layoutParams = LinearLayout.LayoutParams(size, size)
        }

        var slotIdx = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (occupied[row][col]) continue
                val slot  = slots.getOrNull(slotIdx) ?: break
                slotIdx++

                val span = slot.tileSize.coerceIn(1, minOf(3, cols - col, rows - row))
                for (r in row until row + span)
                    for (c in col until col + span)
                        if (r < rows && c < cols) occupied[r][c] = true

                val tilePx   = cellPx * span
                val tileView = buildFavoriteTile(slot, tilePx)
                val lp = android.widget.RelativeLayout.LayoutParams(tilePx, tilePx).apply {
                    leftMargin = col * cellPx
                    topMargin  = row * cellPx
                }
                tileView.layoutParams = lp
                grid.addView(tileView)
            }
        }
        return grid
    }

    private fun buildFavoriteTile(shortcut: AppShortcut, sizePx: Int): View {
        val iconSize = (sizePx * 0.52f).toInt()
        val margin   = dpToPx(4)

        val icon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                .also { it.gravity = Gravity.CENTER_HORIZONTAL }
            if (!shortcut.isEmpty) {
                loadShortcutIcon(this, AppShortcut(
                    packageName = shortcut.packageName,
                    customIcon  = shortcut.customIcon
                ))
            } else {
                setImageResource(android.R.drawable.ic_menu_add)
                setColorFilter(Color.argb(120, 255, 255, 255))
            }
        }

        val label = TextView(this).apply {
            text     = if (!shortcut.isEmpty)
                shortcut.label ?: shortcut.packageName?.let { getAppLabel(it) } ?: ""
            else ""
            textSize = if (sizePx > dpToPx(MENU_ITEM_SIZE_DP)) 12f else 10f
            setTextColor(Color.WHITE)
            gravity  = Gravity.CENTER
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                .also { it.gravity = Gravity.CENTER_HORIZONTAL; it.topMargin = dpToPx(4) }
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            setPadding(margin, margin, margin, margin)
            addView(icon)
            addView(label)
            setOnClickListener {
                if (!shortcut.isEmpty) {
                    hideFavorites()
                    hideMenu()
                    launchApp(shortcut.packageName)
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun loadShortcutIcon(imageView: ImageView, shortcut: AppShortcut?) {
        if (shortcut == null) return
        try {
            if (!shortcut.customIcon.isNullOrEmpty()) {
                val bmp = AppShortcut.decodeBitmap(shortcut.customIcon)
                if (bmp != null) { imageView.setImageBitmap(bmp); return }
            }
            shortcut.packageName?.let { pkg ->
                val drawable = packageManager.getApplicationIcon(pkg)
                imageView.setImageDrawable(drawable)
            }
        } catch (_: Exception) {
            imageView.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (_: Exception) { packageName }
    }

    private fun launchApp(packageName: String?) {
        packageName ?: return
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED }
            if (intent != null) startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun adjustVolume(direction: Int) {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        audio.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun openApp() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    // ── New action helpers ────────────────────────────────────────────────────

    /** Sembunyikan/tampilkan floating button. Tap lagi untuk restore. */
    private fun toggleHideButton() {
        isButtonHidden = !isButtonHidden
        if (isButtonHidden) {
            floatingView.animate().alpha(0f).setDuration(300).withEndAction {
                floatingView.visibility = View.GONE
                // Restore otomatis setelah 5 detik supaya tidak terkunci
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isButtonHidden) {
                        isButtonHidden = false
                        floatingView.visibility = View.VISIBLE
                        floatingView.animate().alpha(settings.opacity).setDuration(300).start()
                    }
                }, HIDE_RESTORE_MS)
            }.start()
        } else {
            floatingView.visibility = View.VISIBLE
            floatingView.animate().alpha(settings.opacity).setDuration(300).start()
        }
    }

    /** Toggle senter on/off via CameraManager */
    private fun toggleFlashlight() {
        try {
            val cm = getSystemService(android.hardware.camera2.CameraManager::class.java)
            val cameraId = cm.cameraIdList.firstOrNull() ?: return
            isFlashlightOn = !isFlashlightOn
            cm.setTorchMode(cameraId, isFlashlightOn)
        } catch (_: Exception) {}
    }

    /** Buka WiFi panel (Settings.Panel — bekerja di API 29+) */
    private fun openWifiPanel() {
        try {
            val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Intent(android.provider.Settings.Panel.ACTION_WIFI)
            } else {
                Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (_: Exception) {}
    }

    /** Buka pengaturan data seluler */
    private fun openCellularSettings() {
        try {
            val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Intent(android.provider.Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
            } else {
                Intent(android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } catch (_: Exception) {}
    }

    /** Buka Bluetooth panel */
    private fun openBluetoothPanel() {
        try {
            // Pakai string literal supaya tidak bergantung pada konstanta SDK
            // "android.settings.panel.action.BLUETOOTH" = Settings.Panel.ACTION_BLUETOOTH (API 33+)
            val action = if (android.os.Build.VERSION.SDK_INT >= 33) {
                "android.settings.panel.action.BLUETOOTH"
            } else {
                android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            }
            startActivity(Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (_: Exception) {
            // Fallback ke settings biasa jika action tidak dikenali
            try {
                startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {}
        }
    }

    /** Set mode senyap (DND / silent) */
    private fun setSilentMode() {
        try {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            val nm    = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                audio.ringerMode = AudioManager.RINGER_MODE_SILENT
            } else {
                // Minta akses DND jika belum granted
                startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            }
        } catch (_: Exception) {}
    }

    /** Set mode getar */
    private fun setVibrateMode() {
        try {
            val audio = getSystemService(AUDIO_SERVICE) as AudioManager
            audio.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            // Haptic feedback konfirmasi
            val vibrator = getSystemService(android.os.Vibrator::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(80,
                    android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    // ── Volume Panel overlay ──────────────────────────────────────────────────

    private fun showVolumePanel() {
        if (volumePanelView != null) return
        val panel = buildVolumePanelView()
        val lp = WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(panel, lp)
        volumePanelView = panel
        panel.alpha = 0f; panel.scaleX = 0.9f; panel.scaleY = 0.9f
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun hideVolumePanel() {
        val panel = volumePanelView ?: return
        panel.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(150)
            .withEndAction {
                try { windowManager.removeView(panel) } catch (_: Exception) {}
                volumePanelView = null
            }.start()
    }

    private fun buildVolumePanelView(): FrameLayout {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val overlay = FrameLayout(this)

        val dim = View(this).apply {
            setBackgroundColor(Color.argb(110, 0, 0, 0))
            setOnClickListener { hideVolumePanel() }
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(24).toFloat()
            setColor(Color.argb(245, 22, 22, 26))
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background  = bg
            elevation   = dpToPx(12).toFloat()
            val p = dpToPx(20)
            setPadding(p, p, p, p)
            layoutParams = FrameLayout.LayoutParams(dpToPx(280), WRAP_CONTENT, Gravity.CENTER)
        }

        // Title
        val title = TextView(this).apply {
            text     = getString(R.string.title_volume_panel)
            textSize = 16f
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                .also { it.bottomMargin = dpToPx(16) }
        }
        panel.addView(title)

        // Volume streams
        val streams = listOf(
            AudioManager.STREAM_MUSIC    to getString(R.string.label_volume_media),
            AudioManager.STREAM_RING     to getString(R.string.label_volume_ring),
            AudioManager.STREAM_ALARM    to getString(R.string.label_volume_alarm),
            AudioManager.STREAM_NOTIFICATION to getString(R.string.label_volume_notif)
        )

        streams.forEach { (stream, label) ->
            val max     = audio.getStreamMaxVolume(stream)
            val current = audio.getStreamVolume(stream)

            val rowLabel = TextView(this).apply {
                text     = label
                textSize = 12f
                setTextColor(Color.argb(200, 255, 255, 255))
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    .also { it.topMargin = dpToPx(8) }
            }

            val seekBar = android.widget.SeekBar(this).apply {
                this.max      = max
                progress      = current
                progressDrawable?.setColorFilter(Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN)
                thumb?.setColorFilter(Color.WHITE,
                    android.graphics.PorterDuff.Mode.SRC_IN)
                layoutParams  = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                    .also { it.topMargin = dpToPx(4); it.bottomMargin = dpToPx(4) }
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                        if (fromUser) audio.setStreamVolume(stream, p, 0)
                    }
                    override fun onStartTrackingTouch(sb: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(sb: android.widget.SeekBar?) {}
                })
            }
            panel.addView(rowLabel)
            panel.addView(seekBar)
        }

        overlay.addView(dim)
        overlay.addView(panel)
        return overlay
    }

    // ── System Panel overlay (9 tile fixed) ───────────────────────────────────

    private fun showSystemPanel() {
        if (systemPanelView != null) return
        val panel = buildSystemPanelView()
        val lp = WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(panel, lp)
        systemPanelView = panel
        panel.alpha = 0f; panel.scaleX = 0.85f; panel.scaleY = 0.85f
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(200)
            .setInterpolator(DecelerateInterpolator()).start()
    }

    private fun hideSystemPanel() {
        val panel = systemPanelView ?: return
        panel.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f).setDuration(150)
            .withEndAction {
                try { windowManager.removeView(panel) } catch (_: Exception) {}
                systemPanelView = null
            }.start()
    }

    private fun buildSystemPanelView(): FrameLayout {
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        val overlay = FrameLayout(this)

        val dim = View(this).apply {
            setBackgroundColor(Color.argb(110, 0, 0, 0))
            setOnClickListener { hideSystemPanel() }
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(28).toFloat()
            setColor(Color.argb(245, 22, 22, 26))
        }

        val cellPx  = dpToPx(84)
        val gapPx   = dpToPx(8)
        val padPx   = dpToPx(14)
        val cols    = 3
        val totalPx = cellPx * cols + gapPx * (cols - 1)

        val panel = FrameLayout(this).apply {
            background = bg
            elevation  = dpToPx(12).toFloat()
            setPadding(padPx, padPx, padPx, padPx)
            layoutParams = FrameLayout.LayoutParams(
                totalPx + padPx * 2, WRAP_CONTENT, Gravity.CENTER
            )
        }

        // 9 tile fixed
        data class SysTile(val iconRes: Int, val label: String, val action: () -> Unit)
        val tiles = listOf(
            SysTile(R.drawable.ic_wifi,         getString(R.string.panel_wifi))    { hideSystemPanel(); openWifiPanel() },
            SysTile(R.drawable.ic_cellular,     getString(R.string.panel_data))    { hideSystemPanel(); openCellularSettings() },
            SysTile(R.drawable.ic_bluetooth,    getString(R.string.panel_bt))      { hideSystemPanel(); openBluetoothPanel() },
            SysTile(R.drawable.ic_silent,       getString(R.string.panel_silent))  { hideSystemPanel(); setSilentMode() },
            SysTile(R.drawable.ic_vibrate,      getString(R.string.panel_vibrate)) { hideSystemPanel(); setVibrateMode() },
            SysTile(R.drawable.ic_flashlight,   getString(R.string.panel_flash))   { toggleFlashlight() },
            SysTile(R.drawable.ic_volume_up,    getString(R.string.panel_vol_up))  { hideSystemPanel(); adjustVolume(AudioManager.ADJUST_RAISE) },
            SysTile(R.drawable.ic_volume_down,  getString(R.string.panel_vol_down)){ hideSystemPanel(); adjustVolume(AudioManager.ADJUST_LOWER) },
            SysTile(R.drawable.ic_open_app,     getString(R.string.panel_settings)){ hideSystemPanel(); openApp() }
        )

        val grid = android.widget.RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(totalPx, cellPx * 3 + gapPx * 2)
        }

        tiles.forEachIndexed { idx, tile ->
            val row = idx / cols
            val col = idx % cols

            val iconContBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.argb(70, 255, 255, 255))
            }
            val tileBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(18).toFloat()
                setColor(Color.argb(80, 255, 255, 255))
            }

            val iconSize = (cellPx * 0.46f).toInt()
            val iconPad  = (cellPx * 0.13f).toInt()

            val iconView = ImageView(this).apply {
                setImageResource(tile.iconRes)
                setColorFilter(Color.WHITE)
                scaleType  = ImageView.ScaleType.CENTER_INSIDE
                background = iconContBg
                setPadding(iconPad, iconPad, iconPad, iconPad)
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                    .also { it.gravity = Gravity.CENTER_HORIZONTAL }
            }
            val labelView = TextView(this).apply {
                text     = tile.label
                textSize = 10f
                setTextColor(Color.WHITE)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                gravity  = Gravity.CENTER
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                    .also { it.gravity = Gravity.CENTER_HORIZONTAL; it.topMargin = dpToPx(5) }
            }

            val tileView = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity     = Gravity.CENTER
                background  = tileBg
                addView(iconView)
                addView(labelView)
                setOnClickListener {
                    animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).withEndAction {
                        animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    }.start()
                    tile.action()
                }
            }

            val lp = android.widget.RelativeLayout.LayoutParams(
                cellPx - dpToPx(4), cellPx - dpToPx(4)
            ).apply {
                leftMargin = col * (cellPx + gapPx) + dpToPx(2)
                topMargin  = row * (cellPx + gapPx) + dpToPx(2)
            }
            tileView.layoutParams = lp
            grid.addView(tileView)
        }

        panel.addView(grid)
        overlay.addView(dim)
        overlay.addView(panel)
        return overlay
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingButtonService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_floating_button)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.action_stop), stopIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        var isRunning = false
            private set

        const val ACTION_STOP = "com.joaohouto.assistivemenutool.STOP"

        private const val NOTIFICATION_ID     = 1001
        private const val CHANNEL_ID          = "assistive_menu_tool"
        const val MENU_ITEM_SIZE_DP           = 86
        const val GAP_DP                      = 8
        private const val DRAG_THRESHOLD        = 8
        private const val SNAP_DURATION_MS      = 260L
        private const val MENU_SHOW_MS          = 200L
        private const val MENU_HIDE_MS          = 150L
        private const val SCREENSHOT_HIDE_MS    = 150L   // tunggu animasi menu tutup
        private const val SCREENSHOT_CAPTURE_MS = 100L   // jeda setelah button hilang
        private const val SCREENSHOT_RESTORE_MS = 600L   // delay sebelum button muncul kembali (diupdate dari 300)
        private const val LONG_PRESS_MS         = 500L   // durasi tahan untuk long press
        private const val DOUBLE_TAP_MS         = 280L   // jendela waktu double tap
        private val SINGLE_TAP_TOKEN            = Any()  // token untuk cancel pending single tap
        private const val HIDE_RESTORE_MS       = 5000L  // auto restore hide button setelah 5 detik
    }
}
