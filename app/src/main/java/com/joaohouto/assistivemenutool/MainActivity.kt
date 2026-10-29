package com.joaohouto.assistivemenutool

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
                    settings               = settings,
                    hasOverlayPermission   = { Settings.canDrawOverlays(this) },
                    hasAccessibilityService = { isAccessibilityServiceEnabled() },
                    requestOverlayPermission = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")))
                    },
                    openAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    isServiceRunning = { FloatingButtonService.isRunning },
                    toggleService = {
                        val svc = Intent(this, FloatingButtonService::class.java)
                        if (FloatingButtonService.isRunning) stopService(svc)
                        else startForegroundService(svc)
                    },
                    getInstalledApps = { getInstalledApps() }
                )
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(packageName, ignoreCase = true)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
                      pm.getLaunchIntentForPackage(it.packageName) != null }
            .mapNotNull { info ->
                val intent = pm.getLaunchIntentForPackage(info.packageName) ?: return@mapNotNull null
                AppInfo(
                    packageName = info.packageName,
                    label       = pm.getApplicationLabel(info).toString(),
                    icon        = try {
                        val d = pm.getApplicationIcon(info.packageName)
                        val bmp = Bitmap.createBitmap(d.intrinsicWidth.coerceAtLeast(1),
                            d.intrinsicHeight.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        d.setBounds(0, 0, bmp.width, bmp.height)
                        d.draw(Canvas(bmp))
                        bmp
                    } catch (_: Exception) { null }
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}

data class AppInfo(val packageName: String, val label: String, val icon: Bitmap?)

// ── Main screen ───────────────────────────────────────────────────────────────

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

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))

            PermissionRow(stringResource(R.string.permission_overlay), overlayGranted, requestOverlayPermission)
            Spacer(Modifier.height(8.dp))
            PermissionRow(stringResource(R.string.permission_accessibility), accessibilityEnabled) {
                showAccessibilityDisclosure = true
            }
            Spacer(Modifier.height(16.dp))

            // Toggle service
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.size(2.dp))
                        Text(
                            text = when {
                                !allGranted    -> stringResource(R.string.status_permissions_required)
                                serviceRunning -> stringResource(R.string.status_active)
                                else           -> stringResource(R.string.status_inactive)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                !allGranted    -> MaterialTheme.colorScheme.error
                                serviceRunning -> MaterialTheme.colorScheme.primary
                                else           -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Switch(
                        checked = serviceRunning && allGranted,
                        onCheckedChange = { if (allGranted) toggleService() },
                        enabled = allGranted
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            AppearanceSection(settings)
            Spacer(Modifier.height(16.dp))
            PopupSlotsSection(settings, getInstalledApps)
            Spacer(Modifier.height(16.dp))
            FavoritesSection(settings, getInstalledApps)
            Spacer(Modifier.height(16.dp))
            OpacitySizeSection(settings)
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Popup Slots Section ───────────────────────────────────────────────────────

@Composable
private fun PopupSlotsSection(
    settings: SettingsRepository,
    getInstalledApps: () -> List<AppInfo>
) {
    var slots by remember { mutableStateOf(settings.popupSlots) }
    var editingSlot by remember { mutableStateOf<Int?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(showAppPicker) {
        if (showAppPicker && apps.isEmpty()) apps = getInstalledApps()
    }

    if (showAppPicker) {
        val idx = editingSlot ?: return@PopupSlotsSection
        AppPickerDialog(
            apps = apps,
            onDismiss = { showAppPicker = false; editingSlot = null },
            onPick = { app ->
                val updated = slots.toMutableList()
                updated[idx] = PopupSlot.fromShortcut(
                    AppShortcut(packageName = app.packageName, label = app.label),
                    slotIndex = idx,
                    tileSize  = slots[idx].tileSize
                )
                slots = updated
                settings.popupSlots = updated
                showAppPicker = false; editingSlot = null
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.title_popup_slots), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("${slots.filter { !it.isEmpty }.size}/9 slots used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            slots.forEachIndexed { idx, slot ->
                SlotRow(
                    index = idx,
                    slot  = slot,
                    onSetAction = { action ->
                        val updated = slots.toMutableList()
                        updated[idx] = PopupSlot.fromAction(action, idx, slot.tileSize)
                        slots = updated; settings.popupSlots = updated
                    },
                    onSetShortcut = { editingSlot = idx; showAppPicker = true },
                    onClear = {
                        val updated = slots.toMutableList()
                        updated[idx] = PopupSlot.empty(idx)
                        slots = updated; settings.popupSlots = updated
                    },
                    onSizeChange = { size ->
                        val updated = slots.toMutableList()
                        updated[idx] = slot.copy(tileSize = size)
                        slots = updated; settings.popupSlots = updated
                    }
                )
                if (idx < 8) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun SlotRow(
    index: Int,
    slot: PopupSlot,
    onSetAction: (MenuAction) -> Unit,
    onSetShortcut: () -> Unit,
    onClear: () -> Unit,
    onSizeChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showActionPicker by remember { mutableStateOf(false) }

    if (showActionPicker) {
        ActionPickerDialog(
            onDismiss = { showActionPicker = false },
            onPick    = { action -> onSetAction(action); showActionPicker = false }
        )
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot number badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (slot.type) {
                        PopupSlot.SlotType.ACTION   -> slot.action?.let {
                            // Ambil nama action dari resource string tidak bisa di sini,
                            // pakai nama enum saja
                            it.name.replace('_', ' ').lowercase()
                                .replaceFirstChar { c -> c.uppercase() }
                        } ?: stringResource(R.string.slot_empty)
                        PopupSlot.SlotType.SHORTCUT -> slot.shortcut?.label
                            ?: slot.shortcut?.packageName ?: stringResource(R.string.slot_empty)
                        else -> stringResource(R.string.slot_empty)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!slot.isEmpty) {
                    Text(
                        text = when (slot.type) {
                            PopupSlot.SlotType.ACTION   -> stringResource(R.string.slot_type_action)
                            PopupSlot.SlotType.SHORTCUT -> stringResource(R.string.slot_type_shortcut)
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.ic_volume_up else R.drawable.ic_quick_settings
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 40.dp, bottom = 8.dp)) {
                // Tile size
                Text(stringResource(R.string.label_tile_size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(1 to R.string.tile_size_1x1, 2 to R.string.tile_size_2x2, 3 to R.string.tile_size_3x3)
                        .forEachIndexed { i, (size, res) ->
                            SegmentedButton(
                                shape    = SegmentedButtonDefaults.itemShape(i, 3),
                                onClick  = { onSizeChange(size) },
                                selected = slot.tileSize == size
                            ) { Text(stringResource(res)) }
                        }
                }
                Spacer(Modifier.height(8.dp))
                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showActionPicker = true },
                        modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.slot_type_action),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = onSetShortcut,
                        modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_choose_app),
                            style = MaterialTheme.typography.labelSmall)
                    }
                    if (!slot.isEmpty) {
                        OutlinedButton(onClick = { onClear(); expanded = false }) {
                            Text(stringResource(R.string.action_clear_slot),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// ── Favorites Section ─────────────────────────────────────────────────────────

@Composable
private fun FavoritesSection(
    settings: SettingsRepository,
    getInstalledApps: () -> List<AppInfo>
) {
    var slots by remember { mutableStateOf(settings.favoriteSlots) }
    var editingSlot by remember { mutableStateOf<Int?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showLabelEditor by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    LaunchedEffect(showAppPicker) {
        if (showAppPicker && apps.isEmpty()) apps = getInstalledApps()
    }

    if (showAppPicker) {
        val idx = editingSlot ?: return@FavoritesSection
        AppPickerDialog(
            apps = apps,
            onDismiss = { showAppPicker = false; editingSlot = null },
            onPick = { app ->
                val updated = slots.toMutableList()
                updated[idx] = AppShortcut(
                    packageName = app.packageName,
                    label       = app.label,
                    tileSize    = slots[idx].tileSize,
                    slotIndex   = idx
                )
                slots = updated; settings.favoriteSlots = updated
                showAppPicker = false; editingSlot = null
            }
        )
    }

    if (showLabelEditor) {
        val idx = editingSlot ?: return@FavoritesSection
        val current = slots[idx]
        LabelEditorDialog(
            initial   = current.label ?: "",
            onDismiss = { showLabelEditor = false; editingSlot = null },
            onConfirm = { label ->
                val updated = slots.toMutableList()
                updated[idx] = current.copy(label = label.ifEmpty { null })
                slots = updated; settings.favoriteSlots = updated
                showLabelEditor = false; editingSlot = null
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.title_favorites), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("${slots.filter { !it.isEmpty }.size}/9 apps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // Grid preview 3×3
            FavoritesGrid(
                slots = slots,
                onSlotClick = { idx ->
                    editingSlot = idx
                    showAppPicker = true
                },
                onSlotLongClick = { idx ->
                    editingSlot = idx
                    showLabelEditor = true
                },
                onClear = { idx ->
                    val updated = slots.toMutableList()
                    updated[idx] = AppShortcut.empty(idx)
                    slots = updated; settings.favoriteSlots = updated
                },
                onSizeChange = { idx, size ->
                    val updated = slots.toMutableList()
                    updated[idx] = slots[idx].copy(tileSize = size)
                    slots = updated; settings.favoriteSlots = updated
                }
            )
        }
    }
}

@Composable
private fun FavoritesGrid(
    slots: List<AppShortcut>,
    onSlotClick: (Int) -> Unit,
    onSlotLongClick: (Int) -> Unit,
    onClear: (Int) -> Unit,
    onSizeChange: (Int, Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        slots.chunked(3).forEachIndexed { rowIdx, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { colIdx, slot ->
                    val idx = rowIdx * 3 + colIdx
                    FavoriteSlotCard(
                        slot         = slot,
                        index        = idx,
                        modifier     = Modifier.weight(1f),
                        onClick      = { onSlotClick(idx) },
                        onLongClick  = { if (!slot.isEmpty) onSlotLongClick(idx) },
                        onClear      = { onClear(idx) },
                        onSizeChange = { size -> onSizeChange(idx, size) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteSlotCard(
    slot: AppShortcut,
    index: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onClear: () -> Unit,
    onSizeChange: (Int) -> Unit
) {
    var showOptions by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { if (slot.isEmpty) onClick() else showOptions = !showOptions },
        colors = CardDefaults.cardColors(
            containerColor = if (slot.isEmpty)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (slot.isEmpty) {
                Text("+", style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(4.dp)) {
                    // Icon placeholder — bitmap dari packageManager tidak bisa langsung
                    // di Compose tanpa Coil, jadi tampilkan initial letter
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text  = (slot.label ?: slot.packageName ?: "?")
                                .firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.height(4.dp))
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

    if (showOptions && !slot.isEmpty) {
        AlertDialog(
            onDismissRequest = { showOptions = false },
            title = { Text(slot.label ?: slot.packageName ?: "") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.label_tile_size),
                        style = MaterialTheme.typography.labelMedium)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(1 to R.string.tile_size_1x1, 2 to R.string.tile_size_2x2, 3 to R.string.tile_size_3x3)
                            .forEachIndexed { i, (size, res) ->
                                SegmentedButton(
                                    shape    = SegmentedButtonDefaults.itemShape(i, 3),
                                    onClick  = { onSizeChange(size) },
                                    selected = slot.tileSize == size
                                ) { Text(stringResource(res)) }
                            }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onLongClick(); showOptions = false }) {
                    Text(stringResource(R.string.action_edit_label))
                }
            },
            dismissButton = {
                TextButton(onClick = { onClear(); showOptions = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)) {
                    Text(stringResource(R.string.action_clear_slot))
                }
            }
        )
    }
}

// ── App Picker Dialog ─────────────────────────────────────────────────────────

@Composable
private fun AppPickerDialog(
    apps: List<AppInfo>,
    onDismiss: () -> Unit,
    onPick: (AppInfo) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        if (query.isEmpty()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp).heightIn(max = 480.dp)) {
                Text(stringResource(R.string.action_choose_app),
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    placeholder   = { Text("Search…") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filtered, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPick(app) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            app.icon?.let { bmp ->
                                Image(bitmap = bmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)))
                            } ?: Box(modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center) {
                                Text(app.label.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium)
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

// ── Action Picker Dialog ──────────────────────────────────────────────────────

@Composable
private fun ActionPickerDialog(onDismiss: () -> Unit, onPick: (MenuAction) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(stringResource(R.string.slot_type_action),
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                MenuAction.entries.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(action) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painter = painterResource(action.iconRes()),
                            contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(action.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// ── Label Editor Dialog ───────────────────────────────────────────────────────

@Composable
private fun LabelEditorDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_edit_label)) },
        text = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                placeholder   = { Text(stringResource(R.string.hint_custom_label)) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

// ── Appearance Section (sama seperti sebelumnya) ──────────────────────────────

@Composable
private fun AppearanceSection(settings: SettingsRepository) {
    var shape         by remember { mutableStateOf(settings.buttonShape) }
    var outerColor    by remember { mutableStateOf(Color(settings.outerColor)) }
    var innerColor    by remember { mutableStateOf(Color(settings.innerColor)) }
    var borderEnabled by remember { mutableStateOf(settings.borderEnabled) }
    var borderColor   by remember { mutableStateOf(Color(settings.borderColor)) }
    var borderWidth   by remember { mutableIntStateOf(settings.borderWidthDp) }
    var colorPickerTarget by remember { mutableStateOf<String?>(null) }

    colorPickerTarget?.let { target ->
        val currentColor = when (target) {
            "outer" -> outerColor; "inner" -> innerColor; else -> borderColor
        }
        ColorPickerDialog(initial = currentColor, onDismiss = { colorPickerTarget = null },
            onConfirm = { picked ->
                when (target) {
                    "outer"  -> { outerColor = picked; settings.outerColor = picked.toArgb() }
                    "inner"  -> { innerColor = picked; settings.innerColor = picked.toArgb() }
                    "border" -> { borderColor = picked; settings.borderColor = picked.toArgb() }
                }
                colorPickerTarget = null
            })
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.title_appearance), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ButtonPreview(shape, outerColor, innerColor, borderEnabled, borderColor, borderWidth)
            }
            Spacer(Modifier.height(20.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.label_shape), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(SettingsRepository.SHAPE_CIRCLE to stringResource(R.string.shape_circle),
                    SettingsRepository.SHAPE_ROUNDED_RECT to stringResource(R.string.shape_rounded))
                    .forEachIndexed { i, (value, label) ->
                        SegmentedButton(shape = SegmentedButtonDefaults.itemShape(i, 2),
                            onClick = { shape = value; settings.buttonShape = value },
                            selected = shape == value) { Text(label) }
                    }
            }
            Spacer(Modifier.height(20.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.label_colors), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            ColorPickerRow(stringResource(R.string.label_outer_color), outerColor) { colorPickerTarget = "outer" }
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(stringResource(R.string.label_inner_color), innerColor) { colorPickerTarget = "inner" }
            Spacer(Modifier.height(20.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_border), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = borderEnabled,
                    onCheckedChange = { borderEnabled = it; settings.borderEnabled = it })
            }
            AnimatedVisibility(visible = borderEnabled) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    ColorPickerRow(stringResource(R.string.label_border_color), borderColor) { colorPickerTarget = "border" }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.label_border_width), style = MaterialTheme.typography.bodyMedium)
                        Text("$borderWidth dp", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(value = borderWidth.toFloat(), onValueChange = { borderWidth = it.roundToInt() },
                        onValueChangeFinished = { settings.borderWidthDp = borderWidth },
                        valueRange = 1f..8f, steps = 6, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ── Opacity & Size Section ────────────────────────────────────────────────────

@Composable
private fun OpacitySizeSection(settings: SettingsRepository) {
    var opacity by remember { mutableFloatStateOf(settings.opacity) }
    var sizeDp  by remember { mutableIntStateOf(settings.buttonSizeDp) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.title_settings), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.label_opacity), style = MaterialTheme.typography.bodyMedium)
                Text("${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(value = opacity, onValueChange = { opacity = it },
                onValueChangeFinished = { settings.opacity = opacity },
                valueRange = 0.3f..1.0f, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.label_size), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SettingsRepository.SIZE_OPTIONS.forEachIndexed { i, dp ->
                    SegmentedButton(
                        shape    = SegmentedButtonDefaults.itemShape(i, SettingsRepository.SIZE_OPTIONS.size),
                        onClick  = { sizeDp = dp; settings.buttonSizeDp = dp },
                        selected = sizeDp == dp
                    ) { Text(stringResource(SettingsRepository.SIZE_LABEL_RES[i])) }
                }
            }
        }
    }
}

// ── Reusable composables (ButtonPreview, ColorPickerRow, ColorPickerDialog, SliderRow, PermissionRow) ──

@Composable
private fun ButtonPreview(shape: String, outerColor: Color, innerColor: Color,
    borderEnabled: Boolean, borderColor: Color, borderWidthDp: Int) {
    val buttonSize = 72.dp
    val innerSize  = (72 * 0.38f).dp
    val cornerRadius: Dp = when (shape) {
        SettingsRepository.SHAPE_CIRCLE       -> 36.dp
        SettingsRepository.SHAPE_ROUNDED_RECT -> (72 * 0.28f).dp
        else -> 36.dp
    }
    val outerShape = RoundedCornerShape(cornerRadius)
    Box(modifier = Modifier.size(buttonSize).clip(outerShape).background(outerColor, outerShape)
        .then(if (borderEnabled) Modifier.border(borderWidthDp.dp, borderColor, outerShape) else Modifier),
        contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.size(innerSize).clip(CircleShape).background(innerColor, CircleShape))
    }
}

@Composable
private fun ColorPickerRow(label: String, color: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text("#%06X".format(color.toArgb() and 0xFFFFFF),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}

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
    val focusManager = LocalFocusManager.current

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
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color.White, currentColor)))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
                SliderRow(stringResource(R.string.label_hue), hue, 0f..360f, "${hue.roundToInt()}°",
                    Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))) { hue = it }
                SliderRow(stringResource(R.string.label_saturation), saturation, 0f..1f, "${(saturation * 100).roundToInt()}%",
                    Brush.horizontalGradient(listOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, brightness))),
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, brightness)))))) { saturation = it }
                SliderRow(stringResource(R.string.label_brightness), brightness, 0f..1f, "${(brightness * 100).roundToInt()}%",
                    Brush.horizontalGradient(listOf(Color.Black,
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))))) { brightness = it }
                SliderRow(stringResource(R.string.label_alpha), alpha, 0f..1f, "${(alpha * 100).roundToInt()}%",
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
                }, label = { Text("HEX (RRGGBB / AARRGGBB)") }, isError = hexError,
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center)
                .padding(horizontal = 10.dp).clip(RoundedCornerShape(2.dp)).background(trackBrush))
            Slider(value = value, onValueChange = onValueChange, valueRange = range,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent))
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (granted)
            MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(if (granted) R.drawable.ic_check_circle else R.drawable.ic_circle_outline),
                    contentDescription = null, modifier = Modifier.size(20.dp),
                    tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(12.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            if (!granted) {
                OutlinedButton(onClick = onRequest, modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                    Text(stringResource(R.string.action_activate), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
