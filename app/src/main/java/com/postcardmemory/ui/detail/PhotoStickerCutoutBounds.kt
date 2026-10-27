package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 누끼(배경제거) 스티커에서 실제로 보이는 부분이 칸 중심 기준으로 차지하는 범위(px,
 * 화면 좌표 — 회전·뒤집기 반영). 투명한 여백은 들어가지 않는다.
 */
internal data class StickerVisibleExtent(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/**
 * [cutoutSilhouette]([photoStickerCutoutSilhouette]의 불투명 표본점)로 보이는 범위를 구한다.
 * 붙인 스티커의 graphicsLayer처럼 뒤집기 → 회전 순서로 옮긴다. 표본점은 격자 칸의
 * 가운데라, 칸 반 대각선만큼 넓혀 보이는 가장자리가 엽서 밖으로 잘리지 않게 한다.
 * 표본점이 없으면(완전히 투명·아직 모름) null.
 */
internal fun cutoutStickerVisibleExtent(
    cutoutSilhouette: List<Offset>,
    stickerSidePx: Float,
    rotationDegrees: Float,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false,
    grid: Int = PHOTO_STICKER_CUTOUT_GRID
): StickerVisibleExtent? {
    if (cutoutSilhouette.isEmpty() || stickerSidePx <= 0f) return null
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY
    cutoutSilhouette.forEach { p ->
        val x = if (flipHorizontal) -p.x else p.x
        val y = if (flipVertical) -p.y else p.y
        val rx = (x * c - y * s) * stickerSidePx
        val ry = (x * s + y * c) * stickerSidePx
        minX = min(minX, rx)
        minY = min(minY, ry)
        maxX = max(maxX, rx)
        maxY = max(maxY, ry)
    }
    val pad = stickerSidePx * sqrt(2f) * 0.5f / max(1, grid)
    return StickerVisibleExtent(minX - pad, minY - pad, maxX + pad, maxY + pad)
}

/**
 * 사진 스티커 자리(offset = 회전 전 칸 좌상단 px)를 엽서 안에 가둔다.
 *
 * - 기본 모양: 칸 전체가 엽서 안([clampStickerOffset], 예전 그대로).
 * - 누끼 + 표본점 있음: 보이는 부분만 엽서 안 — 투명 여백은 밖으로 나가도 되고,
 *   보이는 테두리가 가장자리에 닿을 때까지 옮길 수 있다(사용자가 끌거나 키우거나 돌릴 때).
 * - 누끼 + 표본점 없음([cutoutSilhouette] null): 칸 중심이 엽서에서 칸 반 대각선
 *   ([CUTOUT_STICKER_LOOSE_REACH]배) 이상 벗어나지 않게만. 저장된 자리를 다시 맞추거나
 *   저장·공유 이미지를 만들 때 쓰는 느슨한 규칙이다. 보이는 부분은 늘 회전한 칸 안에
 *   있으므로 위 규칙으로 놓은 자리는 모두 이 안에 들어와, 끌어서 놓은 자리가 다시
 *   당겨지지 않는다(화면 = 저장본).
 */
internal fun clampPhotoStickerOffset(
    offset: Offset,
    postcardSize: IntSize,
    stickerSize: IntSize,
    isBackgroundRemoved: Boolean,
    cutoutSilhouette: List<Offset>?,
    rotationDegrees: Float,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false
): Offset {
    if (!isBackgroundRemoved) {
        return clampStickerOffset(offset, postcardSize, stickerSize)
    }
    val halfWidth = stickerSize.width / 2f
    val halfHeight = stickerSize.height / 2f
    // 칸 중심이 움직일 수 있는 범위를 "중심 + extent가 엽서 안"으로 나타낸다.
    val reach = stickerSize.width * CUTOUT_STICKER_LOOSE_REACH
    val extent =
        cutoutSilhouette?.let {
            cutoutStickerVisibleExtent(
                cutoutSilhouette = it,
                stickerSidePx = stickerSize.width.toFloat(),
                rotationDegrees = rotationDegrees,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical
            )
        } ?: StickerVisibleExtent(left = reach, top = reach, right = -reach, bottom = -reach)
    return Offset(
        x = clampStickerAxis(
            value = offset.x,
            min = -halfWidth - extent.left,
            max = postcardSize.width - halfWidth - extent.right
        ),
        y = clampStickerAxis(
            value = offset.y,
            min = -halfHeight - extent.top,
            max = postcardSize.height - halfHeight - extent.bottom
        )
    )
}

// 칸 중심에서 가장 먼 보이는 점까지의 거리 상한(한 변 대비): 반 대각선 + 표본 여유.
internal const val CUTOUT_STICKER_LOOSE_REACH = 0.75f

// 보이는 부분이 엽서보다 크면 가운데에 둔다.
private fun clampStickerAxis(value: Float, min: Float, max: Float): Float =
    if (min <= max) value.coerceIn(min, max) else (min + max) / 2f
