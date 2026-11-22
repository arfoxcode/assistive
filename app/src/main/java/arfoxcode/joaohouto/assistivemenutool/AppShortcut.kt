package arfoxcode.joaohouto.assistivemenutool

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

data class AppShortcut(
    val packageName: String? = null,
    val label: String? = null,
    val customIcon: String? = null,
    val tileSize: Int = 1,
    val slotIndex: Int = 0
) {
    val isEmpty get() = packageName == null

    fun serialize(): String {
        return listOf(
            packageName ?: "",
            label ?: "",
            customIcon ?: "",
            tileSize.toString(),
            slotIndex.toString()
        ).joinToString(DELIMITER)
    }

    companion object {
        private const val DELIMITER = "|||"

        fun deserialize(raw: String): AppShortcut? {
            return try {
                val parts = raw.split(DELIMITER)
                if (parts.size < 5) return null
                AppShortcut(
                    packageName = parts[0].ifEmpty { null },
                    label       = parts[1].ifEmpty { null },
                    customIcon  = parts[2].ifEmpty { null },
                    tileSize    = parts[3].toIntOrNull()?.coerceIn(1, 3) ?: 1,
                    slotIndex   = parts[4].toIntOrNull()?.coerceIn(0, 8) ?: 0
                )
            } catch (_: Exception) { null }
        }

        fun empty(slotIndex: Int) = AppShortcut(slotIndex = slotIndex)

        fun encodeBitmap(bitmap: Bitmap): String {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }

        fun decodeBitmap(base64: String): Bitmap? {
            return try {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        }
    }
}

data class PopupSlot(
    val type: SlotType = SlotType.EMPTY,
    val action: MenuAction? = null,
    val shortcut: AppShortcut? = null,
    val tileSize: Int = 1,
    val slotIndex: Int = 0
) {
    enum class SlotType { EMPTY, ACTION, SHORTCUT }

    val isEmpty get() = type == SlotType.EMPTY

    fun serialize(): String {
        return listOf(
            type.name,
            action?.name ?: "",
            shortcut?.packageName ?: "",
            shortcut?.label ?: "",
            shortcut?.customIcon ?: "",
            tileSize.toString(),
            slotIndex.toString()
        ).joinToString("|||")
    }

    companion object {
        fun deserialize(raw: String): PopupSlot? {
            return try {
                val p = raw.split("|||")
                if (p.size < 7) return null
                val type = SlotType.entries.find { it.name == p[0] } ?: SlotType.EMPTY
                PopupSlot(
                    type      = type,
                    action    = MenuAction.entries.find { it.name == p[1] },
                    shortcut  = if (p[2].isNotEmpty()) AppShortcut(
                        packageName = p[2],
                        label       = p[3].ifEmpty { null },
                        customIcon  = p[4].ifEmpty { null }
                    ) else null,
                    tileSize  = p[5].toIntOrNull()?.coerceIn(1, 3) ?: 1,
                    slotIndex = p[6].toIntOrNull()?.coerceIn(0, 8) ?: 0
                )
            } catch (_: Exception) { null }
        }

        fun empty(slotIndex: Int) = PopupSlot(slotIndex = slotIndex)

        fun fromAction(action: MenuAction, slotIndex: Int, tileSize: Int = 1) = PopupSlot(
            type = SlotType.ACTION, action = action, tileSize = tileSize, slotIndex = slotIndex
        )

        fun fromShortcut(shortcut: AppShortcut, slotIndex: Int, tileSize: Int = 1) = PopupSlot(
            type = SlotType.SHORTCUT, shortcut = shortcut, tileSize = tileSize, slotIndex = slotIndex
        )
    }
}
