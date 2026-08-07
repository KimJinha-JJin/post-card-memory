package com.postcardmemory.ui.detail

import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 제10차(2026-08-07)에서 `centeredStickerOffset`을 DetailScreen.kt에서
 * StickerPositionCalculations.kt로 분리했다. 대표값 테스트는 이미
 * `PostcardOverlayExportLogicTest.kt`의 `centeredStickerOffset_centersWithinPostcard`
 * 에 있고(같은 패키지라 이번 이동으로 수정 없이 그대로 통과) 여기서 중복
 * 작성하지 않는다. 이 파일은 그 테스트가 다루지 않던 경계값만 추가한다 —
 * 전부 기존 계산식(`(postcardSize - stickerSize) / 2f`를 `coerceAtLeast(0f)`로
 * 클램프)을 손으로 계산해 산정한 기대값이며, 새 구현을 보고 역산하지 않았다.
 */
class StickerPositionCalculationsTest {

    @Test
    fun sameSize_centersAtZero() {
        val centered = centeredStickerOffset(
            postcardSize = IntSize(400, 400),
            stickerSize = IntSize(400, 400)
        )

        assertEquals(0f, centered.x, 0.01f)
        assertEquals(0f, centered.y, 0.01f)
    }

    @Test
    fun stickerLargerThanPostcard_clampsToZero_notNegative() {
        // (500-800)/2f = -150, (500-600)/2f = -50 — coerceAtLeast(0f)가
        // 음수를 0으로 고정하는 기존 동작을 그대로 고정한다.
        val centered = centeredStickerOffset(
            postcardSize = IntSize(500, 500),
            stickerSize = IntSize(800, 600)
        )

        assertEquals(0f, centered.x, 0.01f)
        assertEquals(0f, centered.y, 0.01f)
    }

    @Test
    fun oneAxisSame_onlyOtherAxisOffsets() {
        val centered = centeredStickerOffset(
            postcardSize = IntSize(500, 300),
            stickerSize = IntSize(300, 300)
        )

        assertEquals(100f, centered.x, 0.01f)
        assertEquals(0f, centered.y, 0.01f)
    }

    @Test
    fun zeroSizeSticker_centersAtHalfPostcardSize() {
        val centered = centeredStickerOffset(
            postcardSize = IntSize(200, 150),
            stickerSize = IntSize(0, 0)
        )

        assertEquals(100f, centered.x, 0.01f)
        assertEquals(75f, centered.y, 0.01f)
    }
}
