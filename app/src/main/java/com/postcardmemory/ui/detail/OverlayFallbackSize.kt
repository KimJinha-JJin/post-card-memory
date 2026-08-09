package com.postcardmemory.ui.detail

import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * stickerSizes[id]/sealSizes[id] 측정값이 아직 없을 때(예: 방금 추가해 첫
 * 컴포지션이 안 끝난 경우) 쓰는 fallback 크기. 스티커·도장 모두 실제 렌더
 * 크기가 `기준크기(dp) * item.scale`로 정확히 결정되므로(DetailScreen의
 * `.size(STICKER_BASE_SIZE * sticker.scale)`/`.size(SEAL_BASE_SIZE * seal.scale)`
 * 참고) 이 공식이 근사가 아니라 실제 측정값과 사실상 동일하다.
 */
internal fun computeFallbackOverlaySize(
    basePx: Float,
    scale: Float
): IntSize {
    val side =
        (basePx * scale)
            .roundToInt()
            .coerceAtLeast(1)
    return IntSize(side, side)
}

/** 마스킹테이프처럼 가로·세로 기준 크기가 다른(정사각형이 아닌) 오버레이용 오버로드. */
internal fun computeFallbackOverlaySize(
    basePxWidth: Float,
    basePxHeight: Float,
    scale: Float
): IntSize {
    val width =
        (basePxWidth * scale)
            .roundToInt()
            .coerceAtLeast(1)
    val height =
        (basePxHeight * scale)
            .roundToInt()
            .coerceAtLeast(1)
    return IntSize(width, height)
}
