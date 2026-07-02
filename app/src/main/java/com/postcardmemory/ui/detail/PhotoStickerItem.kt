package com.postcardmemory.ui.detail

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import java.util.UUID

data class PhotoStickerItem(
    val id: String = UUID.randomUUID().toString(),
    val originalUri: Uri,
    val displayedUri: Uri,
    val removedBgUri: Uri? = null,
    val isBackgroundRemoved: Boolean = false,
    val offset: Offset? = null,
    val scale: Float = 1f
)

fun PhotoStickerItem.serialize(): String =
    listOf(
        id,
        originalUri.toString(),
        displayedUri.toString(),
        removedBgUri?.toString() ?: "~",
        isBackgroundRemoved.toString(),
        offset?.x?.toString() ?: "~",
        offset?.y?.toString() ?: "~",
        scale.toString()
    ).joinToString("\t")

fun deserializePhotoStickerItem(
    line: String
): PhotoStickerItem? {
    val p = line.split("\t")
    if (p.size < 8) return null
    return runCatching {
        val ox = p[5].takeIf { it != "~" }?.toFloat()
        val oy = p[6].takeIf { it != "~" }?.toFloat()
        PhotoStickerItem(
            id = p[0],
            originalUri = Uri.parse(p[1]),
            displayedUri = Uri.parse(p[2]),
            removedBgUri = p[3].takeIf { it != "~" }
                ?.let { Uri.parse(it) },
            isBackgroundRemoved = p[4].toBoolean(),
            offset = if (ox != null && oy != null) {
                Offset(ox, oy)
            } else {
                null
            },
            scale = p[7].toFloat()
        )
    }.getOrNull()
}
