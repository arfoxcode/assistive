package com.joaohouto.assistivemenutool

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
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
                    settings = settings,
                    hasOverlayPermission = { Settings.canDrawOverlays(this) },
                    hasAccessibilityService = { isAccessibilityServiceEnabled() },
                    requestOverlayPermission = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    },
                    openAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    isServiceRunning = { FloatingButtonService.isRunning },
                    toggleService = {
                        val svc = Intent(this, FloatingButtonService::class.java)
                        if (FloatingButtonService.isRunning) stopService(svc)
                        else startForegroundService(svc)
                    }
                )
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName, ignoreCase = true)
    }
}

// ── Main screen ───────────────────────────────────────────────────────────────

@Composable
private fun MainScreen(
    settings: SettingsRepository,
    hasOverlayPermission: () -> Boolean,
    hasAccessibilityService: () -> Boolean,
    requestOverlayPermission: () -> Unit,
    openAccessibilitySettings: () -> Unit,
    isServiceRunning: () -> Boolean,
    toggleService: () -> Unit
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
            Text(
                stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            // ── Permissions ──────────────────────────────────────────────
            PermissionRow(
                label   = stringResource(R.string.permission_overlay),
                granted = overlayGranted,
                onRequest = requestOverlayPermission
            )
            Spacer(Modifier.height(8.dp))
            PermissionRow(
                label   = stringResource(R.string.permission_accessibility),
                granted = accessibilityEnabled,
                onRequest = { showAccessibilityDisclosure = true }
            )

            Spacer(Modifier.height(16.dp))

            // ── Toggle ───────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
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
                        checked        = serviceRunning && allGranted,
                        onCheckedChange = { if (allGranted) toggleService() },
                        enabled        = allGranted
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Appearance ───────────────────────────────────────────────
            AppearanceSection(settings)

            Spacer(Modifier.height(16.dp))

            // ── Settings ─────────────────────────────────────────────────
            SettingsSection(settings)

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Appearance section ────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(settings: SettingsRepository) {
    // Local state — sync ke SharedPreferences on change
    var shape         by remember { mutableStateOf(settings.buttonShape) }
    var outerColor    by remember { mutableStateOf(Color(settings.outerColor)) }
    var innerColor    by remember { mutableStateOf(Color(settings.innerColor)) }
    var borderEnabled by remember { mutableStateOf(settings.borderEnabled) }
    var borderColor   by remember { mutableStateOf(Color(settings.borderColor)) }
    var borderWidth   by remember { mutableIntStateOf(settings.borderWidthDp) }

    // Dialog state
    var colorPickerTarget by remember { mutableStateOf<String?>(null) } // "outer" | "inner" | "border"

    // Color picker dialog
    colorPickerTarget?.let { target ->
        val currentColor = when (target) {
            "outer"  -> outerColor
            "inner"  -> innerColor
            "border" -> borderColor
            else     -> outerColor
        }
        ColorPickerDialog(
            initial = currentColor,
            onDismiss = { colorPickerTarget = null },
            onConfirm = { picked ->
                when (target) {
                    "outer"  -> { outerColor = picked; settings.outerColor = picked.toArgb() }
                    "inner"  -> { innerColor = picked; settings.innerColor = picked.toArgb() }
                    "border" -> { borderColor = picked; settings.borderColor = picked.toArgb() }
                }
                colorPickerTarget = null
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.title_appearance), style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(16.dp))

            // ── Live Preview ─────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ButtonPreview(
                    shape         = shape,
                    outerColor    = outerColor,
                    innerColor    = innerColor,
                    borderEnabled = borderEnabled,
                    borderColor   = borderColor,
                    borderWidthDp = borderWidth
                )
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Shape ────────────────────────────────────────────────────
            Text(stringResource(R.string.label_shape), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    SettingsRepository.SHAPE_CIRCLE       to stringResource(R.string.shape_circle),
                    SettingsRepository.SHAPE_ROUNDED_RECT to stringResource(R.string.shape_rounded)
                ).forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        onClick = { shape = value; settings.buttonShape = value },
                        selected = shape == value
                    ) { Text(label) }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Colors ───────────────────────────────────────────────────
            Text(stringResource(R.string.label_colors), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))

            ColorPickerRow(
                label  = stringResource(R.string.label_outer_color),
                color  = outerColor,
                onClick = { colorPickerTarget = "outer" }
            )
            Spacer(Modifier.height(8.dp))
            ColorPickerRow(
                label  = stringResource(R.string.label_inner_color),
                color  = innerColor,
                onClick = { colorPickerTarget = "inner" }
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // ── Border ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_border), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked        = borderEnabled,
                    onCheckedChange = { borderEnabled = it; settings.borderEnabled = it }
                )
            }

            AnimatedVisibility(visible = borderEnabled) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    ColorPickerRow(
                        label  = stringResource(R.string.label_border_color),
                        color  = borderColor,
                        onClick = { colorPickerTarget = "border" }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.label_border_width), style = MaterialTheme.typography.bodyMedium)
                        Text("${borderWidth} dp", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value           = borderWidth.toFloat(),
                        onValueChange   = { borderWidth = it.roundToInt() },
                        onValueChangeFinished = { settings.borderWidthDp = borderWidth },
                        valueRange      = 1f..8f,
                        steps           = 6,
                        modifier        = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ── Button Preview ────────────────────────────────────────────────────────────

@Composable
private fun ButtonPreview(
    shape: String,
    outerColor: Color,
    innerColor: Color,
    borderEnabled: Boolean,
    borderColor: Color,
    borderWidthDp: Int
) {
    val buttonSize = 72.dp
    val innerSize  = (72 * 0.38f).dp
    val cornerRadius: Dp = when (shape) {
        SettingsRepository.SHAPE_CIRCLE       -> 36.dp
        SettingsRepository.SHAPE_ROUNDED_RECT -> (72 * 0.28f).dp
        else -> 36.dp
    }
    val innerCorner: Dp = when (shape) {
        SettingsRepository.SHAPE_CIRCLE       -> (72 * 0.38f / 2f).dp
        SettingsRepository.SHAPE_ROUNDED_RECT -> (72 * 0.38f * 0.28f).dp
        else -> (72 * 0.38f / 2f).dp
    }

    val outerShape = RoundedCornerShape(cornerRadius)
    val innerShape = RoundedCornerShape(innerCorner)

    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(outerShape)
            .background(outerColor, outerShape)
            .then(
                if (borderEnabled)
                    Modifier.border(borderWidthDp.dp, borderColor, outerShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(innerSize)
                .clip(innerShape)
                .background(innerColor, innerShape)
        )
    }
}

// ── Color Picker Row ──────────────────────────────────────────────────────────

@Composable
private fun ColorPickerRow(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val hexStr = "#%06X".format(color.toArgb() and 0xFFFFFF)
            Text(hexStr, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

// ── Color Picker Dialog ───────────────────────────────────────────────────────

@Composable
private fun ColorPickerDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onConfirm: (Color) -> Unit
) {
    // Convert initial color to HSV
    val initHsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initial.toArgb(), initHsv)

    var hue        by remember { mutableFloatStateOf(initHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initHsv[1]) }
    var brightness by remember { mutableFloatStateOf(initHsv[2]) }
    var alpha      by remember { mutableFloatStateOf(initial.alpha) }
    var hexInput   by remember { mutableStateOf("%08X".format(initial.toArgb())) }
    var hexError   by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Build current color from HSV + alpha
    val currentColor by remember(hue, saturation, brightness, alpha) {
        derivedStateOf {
            val argb = android.graphics.Color.HSVToColor(
                (alpha * 255).roundToInt(),
                floatArrayOf(hue, saturation, brightness)
            )
            Color(argb)
        }
    }

    // Sync hex when sliders change
    LaunchedEffect(currentColor) {
        hexInput = "%08X".format(currentColor.toArgb())
        hexError = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.title_color_picker)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color.White, currentColor))
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                )

                // Hue slider
                SliderRow(
                    label     = stringResource(R.string.label_hue),
                    value     = hue,
                    range     = 0f..360f,
                    valueText = "${hue.roundToInt()}°",
                    trackBrush = Brush.horizontalGradient(
                        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                    ),
                    onValueChange = { hue = it }
                )

                // Saturation slider
                SliderRow(
                    label     = stringResource(R.string.label_saturation),
                    value     = saturation,
                    range     = 0f..1f,
                    valueText = "${(saturation * 100).roundToInt()}%",
                    trackBrush = Brush.horizontalGradient(
                        listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, brightness))),
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, brightness)))
                        )
                    ),
                    onValueChange = { saturation = it }
                )

                // Brightness slider
                SliderRow(
                    label     = stringResource(R.string.label_brightness),
                    value     = brightness,
                    range     = 0f..1f,
                    valueText = "${(brightness * 100).roundToInt()}%",
                    trackBrush = Brush.horizontalGradient(
                        listOf(
                            Color.Black,
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
                        )
                    ),
                    onValueChange = { brightness = it }
                )

                // Alpha slider
                SliderRow(
                    label     = stringResource(R.string.label_alpha),
                    value     = alpha,
                    range     = 0f..1f,
                    valueText = "${(alpha * 100).roundToInt()}%",
                    trackBrush = Brush.horizontalGradient(
                        listOf(Color.Transparent, currentColor.copy(alpha = 1f))
                    ),
                    onValueChange = { alpha = it }
                )

                HorizontalDivider()

                // HEX input
                OutlinedTextField(
                    value        = hexInput,
                    onValueChange = { raw ->
                        val clean = raw.uppercase().filter { it.isLetterOrDigit() }.take(8)
                        hexInput = clean
                        if (clean.length == 6 || clean.length == 8) {
                            try {
                                val parsed = android.graphics.Color.parseColor("#$clean")
                                val hsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsed, hsv)
                                hue        = hsv[0]
                                saturation = hsv[1]
                                brightness = hsv[2]
                                alpha      = if (clean.length == 8) (parsed ushr 24) / 255f else 1f
                                hexError   = false
                            } catch (_: Exception) {
                                hexError = true
                            }
                        } else {
                            hexError = clean.isNotEmpty()
                        }
                    },
                    label       = { Text("HEX (RRGGBB / AARRGGBB)") },
                    isError     = hexError,
                    singleLine  = true,
                    modifier    = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction      = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    prefix = { Text("#") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentColor) }) {
                Text(stringResource(R.string.action_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

// ── Slider Row (dengan colored track brush) ───────────────────────────────────

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    trackBrush: Brush,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueText, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // Colored track background behind the slider
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(trackBrush)
            )
            Slider(
                value          = value,
                onValueChange  = onValueChange,
                valueRange     = range,
                modifier       = Modifier.fillMaxWidth(),
                colors         = SliderDefaults.colors(
                    activeTrackColor   = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

// ── Settings section ──────────────────────────────────────────────────────────

@Composable
private fun SettingsSection(settings: SettingsRepository) {
    var opacity by remember { mutableFloatStateOf(settings.opacity) }
    var sizeDp  by remember { mutableIntStateOf(settings.buttonSizeDp) }
    var actions by remember { mutableStateOf(settings.menuActions.toSet()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.title_settings), style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Opacity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_opacity), style = MaterialTheme.typography.bodyMedium)
                Text("${(opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(
                value                = opacity,
                onValueChange        = { opacity = it },
                onValueChangeFinished = { settings.opacity = opacity },
                valueRange           = 0.3f..1.0f,
                modifier             = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Size
            Text(stringResource(R.string.label_size), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SettingsRepository.SIZE_OPTIONS.forEachIndexed { index, dp ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = SettingsRepository.SIZE_OPTIONS.size
                        ),
                        onClick  = { sizeDp = dp; settings.buttonSizeDp = dp },
                        selected = sizeDp == dp
                    ) {
                        Text(stringResource(SettingsRepository.SIZE_LABEL_RES[index]))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Actions
            Text(stringResource(R.string.label_menu_actions), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))

            MenuAction.entries.forEach { action ->
                val checked = action in actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked        = checked,
                        onCheckedChange = { on ->
                            actions = if (on) actions + action else actions - action
                            settings.menuActions = actions.toList()
                        }
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(action.labelRes()), style = MaterialTheme.typography.bodyMedium)
                }
            }

            AnimatedVisibility(visible = actions.isEmpty()) {
                Text(
                    stringResource(R.string.error_select_at_least_one_action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// ── Permission row ────────────────────────────────────────────────────────────

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(
                        if (granted) R.drawable.ic_check_circle else R.drawable.ic_circle_outline
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (granted) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(12.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium)
            }
            if (!granted) {
                OutlinedButton(
                    onClick = onRequest,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text(stringResource(R.string.action_activate), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
