package com.postcardmemory.utils

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * 출력용 사진 스티커만의 보수적인 디코딩 배율.
 * Crop은 짧은 변, 누끼 Fit은 긴 변에 출력 박스 한 변의 2배 이상을 남긴다.
 * 종이 스타일의 사진 창은 박스 안에 있으므로 박스 전체 기준이면 여유가 더 생긴다.
 * 정사각 박스라 EXIF의 가로/세로 교환과 회전·반전은 이 배율에 영향을 주지 않는다.
 * 2배 여유는 재표본화·정수 Crop 경계용이며 시각적 품질 검증을 대신하지 않는다.
 */
internal fun exportStickerSampleSize(
    sourceWidth: Int,
    sourceHeight: Int,
    stickerSide: Float,
    fitInside: Boolean
): Int {
    if (sourceWidth <= 0 || sourceHeight <= 0 ||
        !stickerSide.isFinite() || stickerSide <= 0f
    ) return 1

    val required = ceil(stickerSide.toDouble() * 2.0)
    val limitingSide = if (fitInside) max(sourceWidth, sourceHeight) else min(sourceWidth, sourceHeight)
    var sampleSize = 1
    while (true) {
        val next = sampleSize * 2L
        // 가느다란 Fit 이미지도 어느 축이든 0px이 되도록 요청하지 않는다.
        if (sourceWidth / next < 1 || sourceHeight / next < 1 ||
            limitingSide / next < required
        ) return sampleSize
        sampleSize = next.toInt()
    }
}
