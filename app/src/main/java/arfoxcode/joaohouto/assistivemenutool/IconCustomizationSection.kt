package arfoxcode.joaohouto.assistivemenutool

/**
 * IconCustomizationSection.kt
 *
 * Berisi semua Composable untuk section kustomisasi floating icon:
 *   1. GradientSection      — toggle + pilih tipe + warna kedua
 *   2. IconModeSection      — tab Dot / Preset / Custom
 *   3. PresetIconGrid       — 15 icon yang bisa dipilih
 *   4. AnimationSection     — pilih gaya animasi idle
 *
 * Dipanggil dari MainActivity sebagai item terpisah dalam LazyColumn.
 */

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream

// ─────────────────────────────────────────────────────────────────────────────
// 1. Gradient Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GradientSection(settings: SettingsRepository, onColorPickRequest: (target: String) -> Unit) {
    var enabled      by remember { mutableStateOf(settings.gradientEnabled) }
    var gradientType by remember { mutableStateOf(settings.gradientType) }
    var color1       by remember { mutableStateOf(Color(settings.outerColor)) }
    var color2       by remember { mutableStateOf(Color(settings.gradientColor2)) }

    // Sinkronisasi ulang jika outerColor berubah dari section lain
    LaunchedEffect(settings.outerColor) { color1 = Color(settings.outerColor) }
    LaunchedEffect(settings.gradientColor2) { color2 = Color(settings.gradientColor2) }

    ICSurface {
        // Toggle gradient
        Row(
            Modifier.fillMaxWidth(),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.label_gradient_enabled),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    stringResource(R.string.label_gradient_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked         = enabled,
                onCheckedChange = {
                    enabled = it
                    settings.gradientEnabled = it
                }
            )
        }

        AnimatedVisibility(
            visible = enabled,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                ICDivider()
                Spacer(Modifier.height(16.dp))

                // Preview gradient live
                GradientPreview(
                    color1       = color1,
                    color2       = color2,
                    gradientType = gradientType
                )

                Spacer(Modifier.height(16.dp))

                // Warna 1 (pakai outerColor yang sudah ada)
                ICColorChip(
                    label   = stringResource(R.string.label_gradient_color1),
                    color   = color1,
                    onClick = { onColorPickRequest("outer") }
                )
                Spacer(Modifier.height(8.dp))

                // Warna 2 — baru
                ICColorChip(
                    label   = stringResource(R.string.label_gradient_color2),
                    color   = color2,
                    onClick = { onColorPickRequest("gradient2") }
                )

                Spacer(Modifier.height(16.dp))
                ICDivider()
                Spacer(Modifier.height(12.dp))

                Text(
                    stringResource(R.string.label_gradient_type),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                // 5 pilihan tipe gradient
                val types = listOf(
                    SettingsRepository.GRADIENT_LINEAR_TB    to stringResource(R.string.gradient_tb),
                    SettingsRepository.GRADIENT_LINEAR_LR    to stringResource(R.string.gradient_lr),
                    SettingsRepository.GRADIENT_LINEAR_DIAG  to stringResource(R.string.gradient_diag),
                    SettingsRepository.GRADIENT_LINEAR_DIAG2 to stringResource(R.string.gradient_diag2),
                    SettingsRepository.GRADIENT_RADIAL       to stringResource(R.string.gradient_radial)
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    types.forEach { (typeVal, typeLabel) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    gradientType = typeVal
                                    settings.gradientType = typeVal
                                }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Mini preview
                                GradientTypeDot(typeVal, color1, color2)
                                Text(
                                    typeLabel,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            RadioButton(
                                selected = gradientType == typeVal,
                                onClick  = {
                                    gradientType = typeVal
                                    settings.gradientType = typeVal
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GradientPreview(color1: Color, color2: Color, gradientType: String) {
    val brush: Brush = when (gradientType) {
        SettingsRepository.GRADIENT_LINEAR_LR   -> Brush.horizontalGradient(listOf(color1, color2))
        SettingsRepository.GRADIENT_LINEAR_DIAG -> Brush.linearGradient(listOf(color1, color2))
        SettingsRepository.GRADIENT_LINEAR_DIAG2-> Brush.linearGradient(
            listOf(color1, color2),
            start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
            end   = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
        )
        SettingsRepository.GRADIENT_RADIAL      -> Brush.radialGradient(listOf(color1, color2))
        else                                     -> Brush.verticalGradient(listOf(color1, color2))
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
    )
}

@Composable
private fun GradientTypeDot(type: String, color1: Color, color2: Color) {
    val brush: Brush = when (type) {
        SettingsRepository.GRADIENT_LINEAR_LR    -> Brush.horizontalGradient(listOf(color1, color2))
        SettingsRepository.GRADIENT_LINEAR_DIAG  -> Brush.linearGradient(listOf(color1, color2))
        SettingsRepository.GRADIENT_LINEAR_DIAG2 -> Brush.linearGradient(
            listOf(color1, color2),
            start = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, 0f),
            end   = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
        )
        SettingsRepository.GRADIENT_RADIAL       -> Brush.radialGradient(listOf(color1, color2))
        else                                      -> Brush.verticalGradient(listOf(color1, color2))
    }
    Box(
        Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Icon Mode Section — Tab: Dot / Preset / Custom
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IconModeSection(settings: SettingsRepository) {
    var iconMode   by remember { mutableStateOf(settings.iconMode) }
    var iconPreset by remember { mutableStateOf(settings.iconPreset) }
    var innerColor by remember { mutableStateOf(Color(settings.innerColor)) }
    var customPath by remember { mutableStateOf(settings.iconCustomPath) }

    LaunchedEffect(settings.innerColor) { innerColor = Color(settings.innerColor) }

    val context = LocalContext.current

    // Launcher untuk pick gambar dari galeri
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = processAndSaveCustomIcon(context, uri)
            if (path != null) {
                customPath             = path
                settings.iconCustomPath = path
                settings.iconMode       = SettingsRepository.ICON_MODE_CUSTOM
                iconMode               = SettingsRepository.ICON_MODE_CUSTOM
            }
        }
    }

    ICSurface {
        Text(
            stringResource(R.string.label_icon_mode),
            style      = MaterialTheme.typography.labelMedium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        // Tab selector: Dot / Preset / Custom
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(
                SettingsRepository.ICON_MODE_DOT    to stringResource(R.string.icon_mode_dot),
                SettingsRepository.ICON_MODE_PRESET to stringResource(R.string.icon_mode_preset),
                SettingsRepository.ICON_MODE_CUSTOM to stringResource(R.string.icon_mode_custom)
            ).forEachIndexed { i, (modeVal, modeLabel) ->
                SegmentedButton(
                    shape    = SegmentedButtonDefaults.itemShape(i, 3),
                    selected = iconMode == modeVal,
                    onClick  = {
                        iconMode          = modeVal
                        settings.iconMode = modeVal
                    }
                ) {
                    Text(modeLabel, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Panel konten sesuai mode
        AnimatedVisibility(
            visible = iconMode == SettingsRepository.ICON_MODE_PRESET,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                ICDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.label_icon_color),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                // Warna icon dipakai dari innerColor yang sudah ada
                Text(
                    stringResource(R.string.label_icon_color_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                PresetIconGrid(
                    selectedId    = iconPreset,
                    iconColor     = innerColor,
                    onSelect      = { preset ->
                        iconPreset          = preset.id
                        settings.iconPreset = preset.id
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = iconMode == SettingsRepository.ICON_MODE_CUSTOM,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically()
        ) {
            Column {
                Spacer(Modifier.height(16.dp))
                ICDivider()
                Spacer(Modifier.height(12.dp))

                // Preview thumbnail jika sudah ada
                if (customPath.isNotEmpty() && File(customPath).exists()) {
                    CustomIconPreview(customPath)
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    stringResource(R.string.label_custom_icon_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick  = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter           = painterResource(R.drawable.ic_open_app),
                        contentDescription = null,
                        modifier          = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (customPath.isEmpty()) stringResource(R.string.action_pick_image)
                        else stringResource(R.string.action_change_image)
                    )
                }

                if (customPath.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    TextButton(
                        onClick  = {
                            // Hapus file dan reset ke dot
                            try { File(customPath).delete() } catch (_: Exception) {}
                            customPath              = ""
                            settings.iconCustomPath = ""
                            settings.iconMode       = SettingsRepository.ICON_MODE_DOT
                            iconMode                = SettingsRepository.ICON_MODE_DOT
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.action_remove_custom_icon))
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetIconGrid(
    selectedId: String,
    iconColor:  Color,
    onSelect:   (SettingsRepository.PresetIcon) -> Unit
) {
    // 5 kolom, tinggi = auto (3 baris untuk 15 icon)
    LazyVerticalGrid(
        columns             = GridCells.Fixed(5),
        modifier            = Modifier.height(200.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled   = false
    ) {
        items(SettingsRepository.PRESET_ICONS) { preset ->
            val isSelected = preset.id == selectedId
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(preset) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter           = painterResource(preset.drawableRes),
                    contentDescription = stringResource(preset.labelRes),
                    modifier          = Modifier.size(26.dp),
                    tint              = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                        else iconColor
                )
            }
        }
    }
}

@Composable
private fun CustomIconPreview(path: String) {
    // Decode thumbnail 64×64 — bukan load penuh 256×256 ke UI
    val bmp = remember(path) {
        try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(path, opts)
        } catch (_: Exception) { null }
    }
    if (bmp != null) {
        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                bitmap             = bmp.asImageBitmapSafe(),
                contentDescription = null,
                modifier           = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Animation Section
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnimationSection(settings: SettingsRepository) {
    var animStyle by remember { mutableStateOf(settings.animationStyle) }

    val options = listOf(
        SettingsRepository.ANIM_NONE      to stringResource(R.string.anim_none),
        SettingsRepository.ANIM_PULSE     to stringResource(R.string.anim_pulse),
        SettingsRepository.ANIM_BREATHING to stringResource(R.string.anim_breathing),
        SettingsRepository.ANIM_BOUNCE    to stringResource(R.string.anim_bounce),
        SettingsRepository.ANIM_GLOW      to stringResource(R.string.anim_glow)
    )

    ICSurface {
        Text(
            stringResource(R.string.label_animation),
            style      = MaterialTheme.typography.labelMedium,
            color      = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEach { (styleVal, styleLabel) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            animStyle             = styleVal
                            settings.animationStyle = styleVal
                        }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Dot indikator gaya animasi
                        AnimationIndicator(styleVal)
                        Column {
                            Text(
                                styleLabel,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                animDescriptionFor(styleVal),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RadioButton(
                        selected = animStyle == styleVal,
                        onClick  = {
                            animStyle             = styleVal
                            settings.animationStyle = styleVal
                        }
                    )
                }
            }
        }

        // Peringatan konsumsi baterai jika animasi aktif
        if (animStyle != SettingsRepository.ANIM_NONE) {
            Spacer(Modifier.height(8.dp))
            ICDivider()
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter           = painterResource(R.drawable.ic_notifications),
                    contentDescription = null,
                    modifier          = Modifier.size(14.dp),
                    tint              = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    stringResource(R.string.anim_battery_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun AnimationIndicator(style: String) {
    val color = when (style) {
        SettingsRepository.ANIM_PULSE     -> Color(0xFF4CAF50)
        SettingsRepository.ANIM_BREATHING -> Color(0xFF2196F3)
        SettingsRepository.ANIM_BOUNCE    -> Color(0xFFFF9800)
        SettingsRepository.ANIM_GLOW      -> Color(0xFF9C27B0)
        else                               -> MaterialTheme.colorScheme.outlineVariant
    }
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun animDescriptionFor(style: String): String = when (style) {
    SettingsRepository.ANIM_PULSE     -> stringResource(R.string.anim_pulse_desc)
    SettingsRepository.ANIM_BREATHING -> stringResource(R.string.anim_breathing_desc)
    SettingsRepository.ANIM_BOUNCE    -> stringResource(R.string.anim_bounce_desc)
    SettingsRepository.ANIM_GLOW      -> stringResource(R.string.anim_glow_desc)
    else                               -> stringResource(R.string.anim_none_desc)
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared UI helpers internal
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ICSurface(content: @Composable ColumnScope.() -> Unit) {
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
private fun ICDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
fun ICColorChip(label: String, color: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "#%06X".format(color.toArgb() and 0xFFFFFF),
                style      = MaterialTheme.typography.labelMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Image processing — resize ke 256×256, simpan ke filesDir
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Buka Uri gambar, resize ke 256×256 (crop center), simpan sebagai PNG
 * ke filesDir/custom_button_icon.png.
 *
 * Return: path absolut ke file yang tersimpan, atau null jika gagal.
 */
fun processAndSaveCustomIcon(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original    = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null

        // Crop ke square, lalu resize ke 256×256
        val size     = minOf(original.width, original.height)
        val startX   = (original.width  - size) / 2
        val startY   = (original.height - size) / 2
        val cropped  = Bitmap.createBitmap(original, startX, startY, size, size)
        val resized  = Bitmap.createScaledBitmap(cropped, 256, 256, true)

        // Simpan ke filesDir — bukan SharedPreferences
        val outFile  = File(context.filesDir, "custom_button_icon.png")
        val fos      = FileOutputStream(outFile)
        resized.compress(Bitmap.CompressFormat.PNG, 90, fos)
        fos.flush(); fos.close()

        // Recycle intermediate bitmaps
        if (cropped !== original) cropped.recycle()
        original.recycle()

        outFile.absolutePath
    } catch (_: Exception) { null }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension — Bitmap → ImageBitmap (safe)
// ─────────────────────────────────────────────────────────────────────────────

private fun Bitmap.asImageBitmapSafe(): androidx.compose.ui.graphics.ImageBitmap =
    androidx.compose.ui.graphics.asImageBitmap(this)
