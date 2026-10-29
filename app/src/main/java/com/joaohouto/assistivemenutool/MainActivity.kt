package com.joaohouto.assistivemenutool

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.joaohouto.assistivemenutool.ui.theme.AssistiveMenuToolTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(this)
        enableEdgeToEdge()
        setContent {
            AssistiveMenuToolTheme {
                MainScreen(
                    settings                  = settings,
                    hasOverlayPermission      = { Settings.canDrawOverlays(this) },
                    hasAccessibilityService   = { isAccessibilityServiceEnabled() },
                    requestOverlayPermission  = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")))
                    },
                    openAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    isServiceRunning          = { FloatingButtonService.isRunning },
                    toggleService             = {
                        val svc = Intent(this, FloatingButtonService::class.java)
                        if (FloatingButtonService.isRunning) stopService(svc)
                        else startForegroundService(svc)
                    },
                    getInstalledApps          = { getInstalledApps() }
                )
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(packageName, ignoreCase = true)
    }

    // Gunakan queryIntentActivities — lebih reliable untuk semua app launcher di API 30+
    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).also {
            it.addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val flag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M)
            PackageManager.MATCH_ALL else 0

        return pm.queryIntentActivities(launcherIntent, flag)
            .mapNotNull { resolveInfo ->
                val ai = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                val pkg = ai.packageName
                // Skip package sendiri
                if (pkg == packageName) return@mapNotNull null
                AppInfo(
<<<<<<< HEAD
                    packageName = pkg.packageName,
                    // SESUDAH — aman, skip package yang applicationInfo-nya null
                    val appInfo = pkg.applicationInfo ?: return@mapNotNull null
                    label = pm.getApplicationLabel(appInfo).toString(),
=======
                    packageName = pkg,
                    label       = pm.getApplicationLabel(ai).toString(),
>>>>>>> 1459a69 (fixed favourite button)
                    icon        = try {
                        val d   = pm.getApplicationIcon(pkg)
                        val w   = d.intrinsicWidth.coerceAtLeast(1)
                        val h   = d.intrinsicHeight.coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        d.setBounds(0, 0, w, h); d.draw(Canvas(bmp)); bmp
                    } catch (_: Exception) { null }
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}

data class AppInfo(val packageName: String, val label: String, val icon: Bitmap?)

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MainScreen(
    settings: SettingsRepository,
    hasOverlayPermission: () -> Boolean,
    hasAccessibilityService: () -> Boolean,
    requestOverlayPermission: () -> Unit,
    openAccessibilitySettings: () -> Unit,
    isServiceRunning: () -> Boolean,
    toggleService: () -> Unit,
    getInstalledApps: () -> List<AppInfo>
) {
    var overlayGranted       by remember { mutableStateOf(hasOverlayPermission()) }
    var accessibilityEnabled by remember { mutableStateOf(hasAccessibilityService()) }
    var serviceRunning       by remember { mutableStateOf(isServiceRunning()) }

    LaunchedEffect(Unit) {
        while (true) {
            overlayGranted       = hasOverlayPermission()
            accessibilityEnabled = hasAccessibilityService()
            serviceRunning       = isServiceRunning()
            delay(500)
        }
    }

    val allGranted = overlayGranted && accessibilityEnabled
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    if (showAccessibilityDisclosure) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDisclosure = false },
            title = { Text(stringResource(R.string.accessibility_disclosure_title)) },
            text  = { Text(stringResource(R.string.accessibility_disclosure_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showAccessibilityDisclosure = false
                    openAccessibilitySettings()
                }) { Text(stringResource(R.string.action_agree_and_continue)) }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDisclosure = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Hero header ────────────────────────────────────────────────
            item {
                HeroHeader(
                    serviceRunning = serviceRunning,
                    allGranted     = allGranted,
                    onToggle       = { if (allGranted) toggleService() }
                )
            }

            // ── Permissions ────────────────────────────────────────────────
            item {
                SectionLabel("Permissions")
                Column(
                    modifier            = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModernPermissionRow(
                        label   = stringResource(R.string.permission_overlay),
                        granted = overlayGranted,
                        onRequest = requestOverlayPermission
                    )
                    ModernPermissionRow(
                        label   = stringResource(R.string.permission_accessibility),
                        granted = accessibilityEnabled,
                        onRequest = { showAccessibilityDisclosure = true }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Button appearance ──────────────────────────────────────────
            item {
                SectionLabel(stringResource(R.string.title_appearance))
                AppearanceSection(settings)
                Spacer(Modifier.height(24.dp))
            }

            // ── Button size & opacity ──────────────────────────────────────
            item {
                SectionLabel(stringResource(R.string.title_settings))
                SizeOpacitySection(settings)
                Spacer(Modifier.height(24.dp))
            }

            // ── Popup slots ────────────────────────────────────────────────
            item {
                SectionLabel(stringResource(R.string.title_popup_slots))
                PopupSlotsSection(settings, getInstalledApps)
                Spacer(Modifier.height(24.dp))
            }

            // ── Favorites ──────────────────────────────────────────────────
            item {
                SectionLabel(stringResource(R.string.title_favorites))
                FavoritesSection(settings, getInstalledApps)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hero Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HeroHeader(serviceRunning: Boolean, allGranted: Boolean, onToggle: () -> Unit) {
    val activeColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 32.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter           = painterResource(R.drawable.ic_floating_button),
                        contentDescription = null,
                        tint              = MaterialTheme.colorScheme.onPrimary,
                        modifier          = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text       = stringResource(R.string.app_name),
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // Status + toggle card
            Surface(
                shape  = RoundedCornerShape(20.dp),
                color  = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier            = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        !allGranted    -> MaterialTheme.colorScheme.error
                                        serviceRunning -> Color(0xFF34C759) // green
                                        else           -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    }
                                )
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    !allGranted    -> stringResource(R.string.status_permissions_required)
                                    serviceRunning -> stringResource(R.string.status_active)
                                    else           -> stringResource(R.string.status_inactive)
                                },
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (allGranted) {
                                Text(
                                    text  = if (serviceRunning) "Tap to stop" else "Tap to start",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Switch(
                        checked         = serviceRunning && allGranted,
                        onCheckedChange = { onToggle() },
                        enabled         = allGranted
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String) {
    Text(
        text     = title.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .padding(horizontal = 28.dp)
            .padding(bottom = 10.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Modern Permission Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernPermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    Surface(
        shape           = RoundedCornerShape(16.dp),
        color           = if (granted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter           = painterResource(
                        if (granted) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
                    ),
                    contentDescription = null,
                    modifier          = Modifier.size(22.dp),
                    tint              = if (granted) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            if (!granted) {
                FilledTonalButton(
                    onClick          = onRequest,
                    contentPadding   = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier         = Modifier.height(34.dp)
                ) {
                    Text(stringResource(R.string.action_activate), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Appearance Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(settings: SettingsRepository) {
    var shape         by remember { mutableStateOf(settings.buttonShape) }
    var outerColor    by remember { mutableStateOf(Color(settings.outerColor)) }
    var innerColor    by remember { mutableStateOf(Color(settings.innerColor)) }
    var borderEnabled by remember { mutableStateOf(settings.borderEnabled) }
    var borderColor   by remember { mutableStateOf(Color(settings.borderColor)) }
    var borderWidth   by remember { mutableIntStateOf(settings.borderWidthDp) }
    var colorTarget   by remember { mutableStateOf<String?>(null) }

    colorTarget?.let { target ->
        val cur = when (target) { "outer" -> outerColor; "inner" -> innerColor; else -> borderColor }
        ColorPickerDialog(initial = cur, onDismiss = { colorTarget = null }, onConfirm = { picked ->
            when (target) {
                "outer"  -> { outerColor = picked; settings.outerColor = picked.toArgb() }
                "inner"  -> { innerColor = picked; settings.innerColor = picked.toArgb() }
                "border" -> { borderColor = picked; settings.borderColor = picked.toArgb() }
            }
            colorTarget = null
        })
    }

    ModernCard {
        // Live preview
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            ButtonPreview(shape, outerColor, innerColor, borderEnabled, borderColor, borderWidth)
        }

        Spacer(Modifier.height(24.dp))

        // Shape
        SettingRow(label = stringResource(R.string.label_shape)) {
            SingleChoiceSegmentedButtonRow {
                listOf(SettingsRepository.SHAPE_CIRCLE to stringResource(R.string.shape_circle),
                    SettingsRepository.SHAPE_ROUNDED_RECT to stringResource(R.string.shape_rounded))
                    .forEachIndexed { i, (v, l) ->
                        SegmentedButton(
                            shape    = SegmentedButtonDefaults.itemShape(i, 2),
                            onClick  = { shape = v; settings.buttonShape = v },
                            selected = shape == v
                        ) { Text(l, style = MaterialTheme.typography.labelMedium) }
                    }
            }
        }

        Spacer(Modifier.height(16.dp))
        ModernDivider()
        Spacer(Modifier.height(16.dp))

        // Colors
        Text(stringResource(R.string.label_colors),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))

        ColorChip(label = stringResource(R.string.label_outer_color), color = outerColor) { colorTarget = "outer" }
        Spacer(Modifier.height(8.dp))
        ColorChip(label = stringResource(R.string.label_inner_color), color = innerColor) { colorTarget = "inner" }

        Spacer(Modifier.height(16.dp))
        ModernDivider()
        Spacer(Modifier.height(16.dp))

        // Border
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.label_border), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Switch(checked = borderEnabled, onCheckedChange = { borderEnabled = it; settings.borderEnabled = it })
        }

        AnimatedVisibility(
            visible = borderEnabled,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(12.dp))
                ColorChip(label = stringResource(R.string.label_border_color), color = borderColor) { colorTarget = "border" }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_border_width), style = MaterialTheme.typography.bodyMedium)
                    Text("${borderWidth}dp", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                Slider(value = borderWidth.toFloat(), onValueChange = { borderWidth = it.roundToInt() },
                    onValueChangeFinished = { settings.borderWidthDp = borderWidth },
                    valueRange = 1f..8f, steps = 6, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Size & Opacity Section  (slider bebas untuk size)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SizeOpacitySection(settings: SettingsRepository) {
    var opacity by remember { mutableFloatStateOf(settings.opacity) }
    var sizeDp  by remember { mutableIntStateOf(settings.buttonSizeDp) }

    ModernCard {
        // Opacity
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(stringResource(R.string.label_opacity), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = opacity, onValueChange = { opacity = it },
            onValueChangeFinished = { settings.opacity = opacity },
            valueRange = 0.3f..1.0f, modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(8.dp))
        ModernDivider()
        Spacer(Modifier.height(16.dp))

        // Size — slider bebas
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.label_size), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${SettingsRepository.MIN_SIZE_DP}dp – ${SettingsRepository.MAX_SIZE_DP}dp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text("${sizeDp}dp",
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style      = MaterialTheme.typography.labelLarge,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Slider(
            value                = sizeDp.toFloat(),
            onValueChange        = { sizeDp = it.roundToInt() },
            onValueChangeFinished = { settings.buttonSizeDp = sizeDp },
            valueRange           = SettingsRepository.MIN_SIZE_DP.toFloat()..SettingsRepository.MAX_SIZE_DP.toFloat(),
            modifier             = Modifier.fillMaxWidth()
        )

        // Quick presets
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
            listOf(40 to "S", 56 to "M", 72 to "L", 88 to "XL").forEach { (dp, label) ->
                FilterChip(
                    selected = sizeDp == dp,
                    onClick  = { sizeDp = dp; settings.buttonSizeDp = dp },
                    label    = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Popup Slots Section — grid visual seperti favorit
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PopupSlotsSection(settings: SettingsRepository, getInstalledApps: () -> List<AppInfo>) {
    var slots         by remember { mutableStateOf(settings.popupSlots) }
    var editingSlot   by remember { mutableStateOf<Int?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }
    var apps          by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(showAppPicker) {
        if (showAppPicker && apps.isEmpty()) apps = getInstalledApps()
    }

    if (showAppPicker) {
        val idx = editingSlot ?: return@PopupSlotsSection
        AppPickerDialog(apps = apps, onDismiss = { showAppPicker = false; editingSlot = null },
            onPick = { app ->
                val u = slots.toMutableList()
                u[idx] = PopupSlot.fromShortcut(
                    AppShortcut(packageName = app.packageName, label = app.label),
                    slotIndex = idx, tileSize = slots[idx].tileSize)
                slots = u; settings.popupSlots = u
                showAppPicker = false; editingSlot = null
            })
    }

    if (showActionPicker) {
        val idx = editingSlot ?: return@PopupSlotsSection
        ActionPickerDialog(onDismiss = { showActionPicker = false; editingSlot = null },
            onPick = { action ->
                val u = slots.toMutableList()
                u[idx] = PopupSlot.fromAction(action, idx, slots[idx].tileSize)
                slots = u; settings.popupSlots = u
                showActionPicker = false; editingSlot = null
            })
    }

    ModernCard {
        Text("${slots.filter { !it.isEmpty }.size} / 9",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        // Grid 3×3 visual — sama seperti favorites
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slots.chunked(3).forEachIndexed { rowIdx, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { colIdx, slot ->
                        val idx = rowIdx * 3 + colIdx
                        PopupSlotCard(
                            slot         = slot,
                            modifier     = Modifier.weight(1f),
                            onTap        = { editingSlot = idx },
                            onSetAction  = { editingSlot = idx; showActionPicker = true },
                            onSetApp     = { editingSlot = idx; showAppPicker = true },
                            onClear      = {
                                val u = slots.toMutableList()
                                u[idx] = PopupSlot.empty(idx)
                                slots = u; settings.popupSlots = u
                            },
                            onSizeChange = { size ->
                                val u = slots.toMutableList()
                                u[idx] = slot.copy(tileSize = size)
                                slots = u; settings.popupSlots = u
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupSlotCard(
    slot: PopupSlot,
    modifier: Modifier = Modifier,
    onTap: () -> Unit,
    onSetAction: () -> Unit,
    onSetApp: () -> Unit,
    onClear: () -> Unit,
    onSizeChange: (Int) -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    if (showOptions) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = {
                Text(
                    text = when (slot.type) {
                        PopupSlot.SlotType.ACTION   -> slot.action?.name
                            ?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() }
                            ?: stringResource(R.string.slot_empty)
                        PopupSlot.SlotType.SHORTCUT -> slot.shortcut?.label ?: stringResource(R.string.slot_empty)
                        else -> "Slot ${stringResource(R.string.slot_empty)}"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Tile size
                    Text(stringResource(R.string.label_tile_size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(1 to R.string.tile_size_1x1, 2 to R.string.tile_size_2x2, 3 to R.string.tile_size_3x3)
                            .forEachIndexed { i, (size, res) ->
                                SegmentedButton(shape = SegmentedButtonDefaults.itemShape(i, 3),
                                    onClick = { onSizeChange(size) }, selected = slot.tileSize == size) {
                                    Text(stringResource(res), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                    }
                    // Change content
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onSetAction(); showOptions = false },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)) {
                            Text(stringResource(R.string.slot_type_action),
                                style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(onClick = { onSetApp(); showOptions = false },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)) {
                            Text(stringResource(R.string.action_choose_app),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptions = false }) {
                    Text(stringResource(R.string.action_apply))
                }
            },
            dismissButton = {
                if (!slot.isEmpty) {
                    TextButton(onClick = { onClear(); showOptions = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error)) {
                        Text(stringResource(R.string.action_clear_slot))
                    }
                }
            }
        )
    }

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = if (slot.isEmpty) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                   else MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp))
            .clickable { if (slot.isEmpty) onSetAction() else showOptions = true }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (slot.isEmpty) {
                // Slot kosong — tampil subtle tanpa tombol +
                Icon(painter = painterResource(R.drawable.ic_circle_outline),
                    contentDescription = null, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)) {
                    // Icon
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center) {
                        when (slot.type) {
                            PopupSlot.SlotType.ACTION -> slot.action?.let {
                                Icon(painter = painterResource(it.iconRes()),
                                    contentDescription = null, modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary)
                            }
                            PopupSlot.SlotType.SHORTCUT -> {
                                Text(
                                    text = (slot.shortcut?.label ?: slot.shortcut?.packageName ?: "?")
                                        .firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            else -> {}
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    // Label
                    Text(
                        text = when (slot.type) {
                            PopupSlot.SlotType.ACTION -> slot.action?.name
                                ?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: ""
                            PopupSlot.SlotType.SHORTCUT -> slot.shortcut?.label
                                ?: slot.shortcut?.packageName?.substringAfterLast('.') ?: ""
                            else -> ""
                        },
                        style    = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    // Tile size badge
                    if (slot.tileSize > 1) {
                        Text("${slot.tileSize}×${slot.tileSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Favorites Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FavoritesSection(settings: SettingsRepository, getInstalledApps: () -> List<AppInfo>) {
    var slots        by remember { mutableStateOf(settings.favoriteSlots) }
    var editingSlot  by remember { mutableStateOf<Int?>(null) }
    var showAppPicker  by remember { mutableStateOf(false) }
    var showLabelEditor by remember { mutableStateOf(false) }
    var apps         by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(showAppPicker) {
        if (showAppPicker && apps.isEmpty()) apps = getInstalledApps()
    }

    if (showAppPicker) {
        val idx = editingSlot ?: return@FavoritesSection
        AppPickerDialog(apps = apps, onDismiss = { showAppPicker = false; editingSlot = null },
            onPick = { app ->
                val u = slots.toMutableList()
                u[idx] = AppShortcut(packageName = app.packageName, label = app.label,
                    tileSize = slots[idx].tileSize, slotIndex = idx)
                slots = u; settings.favoriteSlots = u
                showAppPicker = false; editingSlot = null
            })
    }

    if (showLabelEditor) {
        val idx = editingSlot ?: return@FavoritesSection
        LabelEditorDialog(initial = slots[idx].label ?: "",
            onDismiss = { showLabelEditor = false; editingSlot = null },
            onConfirm = { label ->
                val u = slots.toMutableList()
                u[idx] = slots[idx].copy(label = label.ifEmpty { null })
                slots = u; settings.favoriteSlots = u
                showLabelEditor = false; editingSlot = null
            })
    }

    ModernCard {
        Text("${slots.filter { !it.isEmpty }.size} / 9 apps",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        // Grid 3×3
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slots.chunked(3).forEachIndexed { rowIdx, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { colIdx, slot ->
                        val idx = rowIdx * 3 + colIdx
                        FavoriteSlotCard(
                            slot         = slot,
                            modifier     = Modifier.weight(1f),
                            onClick      = { editingSlot = idx; showAppPicker = true },
                            onEditLabel  = { editingSlot = idx; showLabelEditor = true },
                            onClear      = {
                                val u = slots.toMutableList()
                                u[idx] = AppShortcut.empty(idx)
                                slots = u; settings.favoriteSlots = u
                            },
                            onSizeChange = { size ->
                                val u = slots.toMutableList()
                                u[idx] = slots[idx].copy(tileSize = size)
                                slots = u; settings.favoriteSlots = u
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteSlotCard(
    slot: AppShortcut, modifier: Modifier = Modifier,
    onClick: () -> Unit, onEditLabel: () -> Unit,
    onClear: () -> Unit, onSizeChange: (Int) -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    if (showOptions && !slot.isEmpty) {
        AlertDialog(onDismissRequest = { showOptions = false },
            title = { Text(slot.label ?: slot.packageName?.substringAfterLast('.') ?: "") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.label_tile_size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(1 to R.string.tile_size_1x1, 2 to R.string.tile_size_2x2, 3 to R.string.tile_size_3x3)
                            .forEachIndexed { i, (size, res) ->
                                SegmentedButton(shape = SegmentedButtonDefaults.itemShape(i, 3),
                                    onClick = { onSizeChange(size) }, selected = slot.tileSize == size) {
                                    Text(stringResource(res), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onEditLabel(); showOptions = false }) {
                    Text(stringResource(R.string.action_edit_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { onClear(); showOptions = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.action_clear_slot))
                }
            }
        )
    }

    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = if (slot.isEmpty) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                   else MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp))
            .clickable { if (slot.isEmpty) onClick() else showOptions = true }
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (slot.isEmpty) {
                Icon(painter = painterResource(R.drawable.ic_home),
                    contentDescription = null, modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center) {
                        Text(
                            text  = (slot.label ?: slot.packageName ?: "?")
                                .firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text     = slot.label ?: slot.packageName?.substringAfterLast('.') ?: "",
                        style    = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dialogs
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppPickerDialog(apps: List<AppInfo>, onDismiss: () -> Unit, onPick: (AppInfo) -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        if (query.isEmpty()) apps else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp).heightIn(max = 520.dp)) {
                Text(stringResource(R.string.action_choose_app),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = query, onValueChange = { query = it },
                    placeholder = { Text("Search…") }, singleLine = true,
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))

                if (apps.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(120.dp), Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onPick(app) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                if (app.icon != null) {
                                    Image(bitmap = app.icon.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                                } else {
                                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center) {
                                        Text(app.label.firstOrNull()?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(app.label, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text(app.packageName, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPickerDialog(onDismiss: () -> Unit, onPick: (MenuAction) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.slot_type_action),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                MenuAction.entries.forEach { action ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .clickable { onPick(action) }.padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center) {
                            Icon(painter = painterResource(action.iconRes()), contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(action.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelEditorDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_edit_label)) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.hint_custom_label)) },
                singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.action_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Color Picker Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ColorPickerDialog(initial: Color, onDismiss: () -> Unit, onConfirm: (Color) -> Unit) {
    val initHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initial.toArgb(), initHsv)
    var hue        by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initHsv[2]) }
    var alpha      by remember { mutableFloatStateOf(initial.alpha) }
    var hexInput   by remember { mutableStateOf("%08X".format(initial.toArgb())) }
    var hexError   by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val currentColor by remember(hue, saturation, brightness, alpha) {
        derivedStateOf {
            Color(android.graphics.Color.HSVToColor((alpha * 255).roundToInt(),
                floatArrayOf(hue, saturation, brightness)))
        }
    }
    LaunchedEffect(currentColor) { hexInput = "%08X".format(currentColor.toArgb()); hexError = false }

    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_color_picker)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Preview
                Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(Color.White, currentColor)))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)))

                SliderRow(stringResource(R.string.label_hue), hue, 0f..360f, "${hue.roundToInt()}°",
                    Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))) { hue = it }
                SliderRow(stringResource(R.string.label_saturation), saturation, 0f..1f, "${(saturation*100).roundToInt()}%",
                    Brush.horizontalGradient(listOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, brightness))),
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, brightness)))))) { saturation = it }
                SliderRow(stringResource(R.string.label_brightness), brightness, 0f..1f, "${(brightness*100).roundToInt()}%",
                    Brush.horizontalGradient(listOf(Color.Black,
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))))) { brightness = it }
                SliderRow(stringResource(R.string.label_alpha), alpha, 0f..1f, "${(alpha*100).roundToInt()}%",
                    Brush.horizontalGradient(listOf(Color.Transparent, currentColor.copy(alpha = 1f)))) { alpha = it }

                HorizontalDivider()
                OutlinedTextField(value = hexInput, onValueChange = { raw ->
                    val clean = raw.uppercase().filter { it.isLetterOrDigit() }.take(8)
                    hexInput = clean
                    if (clean.length == 6 || clean.length == 8) {
                        try {
                            val parsed = android.graphics.Color.parseColor("#$clean")
                            val hsv = FloatArray(3); android.graphics.Color.colorToHSV(parsed, hsv)
                            hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
                            alpha = if (clean.length == 8) (parsed ushr 24) / 255f else 1f
                            hexError = false
                        } catch (_: Exception) { hexError = true }
                    } else { hexError = clean.isNotEmpty() }
                }, label = { Text("HEX") }, isError = hexError, singleLine = true,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                    prefix = { Text("#") })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(currentColor) }) { Text(stringResource(R.string.action_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    valueText: String, trackBrush: Brush, onValueChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center)
                .padding(horizontal = 10.dp).clip(RoundedCornerShape(2.dp)).background(trackBrush))
            Slider(value = value, onValueChange = onValueChange, valueRange = range,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ModernCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape           = RoundedCornerShape(20.dp),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier        = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun ModernDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun ColorChip(label: String, color: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 8.dp),
        Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("#%06X".format(color.toArgb() and 0xFFFFFF),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Box(Modifier.size(28.dp).clip(CircleShape).background(color)
                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

@Composable
private fun ButtonPreview(shape: String, outerColor: Color, innerColor: Color,
    borderEnabled: Boolean, borderColor: Color, borderWidthDp: Int) {
    val size   = 72.dp
    val inner  = (72 * 0.38f).dp
    val corner : Dp = when (shape) {
        SettingsRepository.SHAPE_CIRCLE       -> 36.dp
        SettingsRepository.SHAPE_ROUNDED_RECT -> (72 * 0.28f).dp
        else -> 36.dp
    }
    val s = RoundedCornerShape(corner)
    Box(Modifier.size(size).shadow(8.dp, s).clip(s).background(outerColor, s)
        .then(if (borderEnabled) Modifier.border(borderWidthDp.dp, borderColor, s) else Modifier),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(inner).clip(CircleShape).background(innerColor, CircleShape))
    }
}
