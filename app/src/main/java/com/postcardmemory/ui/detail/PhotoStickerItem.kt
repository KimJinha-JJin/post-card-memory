package com.postcardmemory.ui.detail

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import java.util.UUID

data class PhotoStickerItem(
    val id: String = UUID.randomUUID().toString(),
    val originalUri: Uri,
    val displayedUri: Uri,
    @Suppress("SpellCheckingInspection") val removedBgUri: Uri? = null,
    val isBackgroundRemoved: Boolean = false,
    val offset: Offset? = null,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val edgeStyle: PhotoStickerEdgeStyle = PhotoStickerEdgeStyle.DEFAULT,
    /** 불규칙 오림 모양의 고정 seed. 0L = 아직 스타일을 적용한 적 없음. 복제 시 copy()로 그대로 따라간다. */
    val edgeSeed: Long = 0L
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
        scale.toString(),
        rotationDegrees.toString(),
        flipHorizontal.toString(),
        flipVertical.toString(),
        edgeStyle.storedName,
        edgeSeed.toString()
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
            scale = p[7].toFloat(),
            rotationDegrees =
                p.getOrNull(8)?.toFloatOrNull()
                    ?.let(::normalizeStickerRotation)
                    ?: 0f,
            flipHorizontal =
                p.getOrNull(9)?.toBoolean() ?: false,
            flipVertical =
                p.getOrNull(10)?.toBoolean() ?: false,
            edgeStyle =
                PhotoStickerEdgeStyle.fromStored(p.getOrNull(11)),
            edgeSeed =
                parsePhotoStickerEdgeSeed(p.getOrNull(12))
        )
    }.getOrNull()
}

fun normalizeStickerRotation(
    degrees: Float
): Float {
    var normalized = degrees % 360f
    if (normalized > 180f) {
        normalized -= 360f
    } else if (normalized < -180f) {
        normalized += 360f
    }
    return normalized
}
