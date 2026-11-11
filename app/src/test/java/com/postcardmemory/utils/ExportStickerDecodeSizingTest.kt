package com.postcardmemory.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class ExportStickerDecodeSizingTest {
    @Test fun squareStickerKeepsTwiceTheOutputResolution() {
        assertEquals(2, sample(2992, 2992, 512f))
        assertEquals(4, sample(2992, 2992, 204.8f))
    }

    @Test fun largeStickerDoesNotLoseSourceResolution() {
        assertEquals(1, sample(2992, 2992, 2048f))
        assertEquals(1, sample(2992, 2992, 819.2f))
    }

    @Test fun cropUsesShortSideSoPanoramaDoesNotBecomeBlurry() {
        assertEquals(1, sample(6000, 1000, 512f))
        assertEquals(1, sample(1000, 6000, 512f))
    }

    @Test fun cutoutFitUsesLongSide() {
        assertEquals(4, sample(6000, 1000, 512f, true))
        assertEquals(4, sample(1000, 6000, 512f, true))
    }

    @Test fun failedCutoutFallbackNeedsCropResolution() {
        assertEquals(4, sample(6000, 1000, 512f, true))
        assertEquals(1, sample(6000, 1000, 512f, false))
    }

    @Test fun smallOriginalIsNeverUpscaledByDecoder() {
        assertEquals(1, sample(120, 80, 512f))
        assertEquals(1, sample(120, 80, 512f, true))
    }

    @Test fun boundaryIsConservativeWithOddPixelsAndFractionalSide() {
        assertEquals(2, sample(2048, 2048, 512f))
        assertEquals(1, sample(2047, 2047, 512f))
        assertEquals(1, sample(2048, 2048, 512.01f))
    }

    @Test fun exifAxisSwapDoesNotChangeSquareBoxRequirements() {
        for (fit in listOf(false, true)) {
            assertEquals(sample(4033, 3025, 400f, fit), sample(3025, 4033, 400f, fit))
        }
    }

    @Test fun malformedBoundsOrSideRetainsFullDecode() {
        for (side in listOf(Float.NaN, Float.POSITIVE_INFINITY, 0f, -1f)) {
            assertEquals(1, sample(2992, 2992, side))
        }
        assertEquals(1, sample(-1, 2992, 512f))
        assertEquals(1, sample(2992, 0, 512f))
    }

    @Test fun extremelyThinFitImageNeverRequestsZeroPixelAxis() {
        assertEquals(1, sample(1, 100000, 1f, true))
        assertEquals(2, sample(2, 100000, 1f, true))
    }

    @Test fun extremeBoundsDoNotOverflowOrLoop() {
        assertEquals(1 shl 29, sample(Int.MAX_VALUE, Int.MAX_VALUE, 1f))
        assertEquals(1, sample(Int.MAX_VALUE, Int.MAX_VALUE, Float.MAX_VALUE))
    }

    @Test fun sampledDimensionsKeepQualityMarginForManyAspectRatios() {
        for (w in listOf(1, 17, 511, 1024, 2992, 4033, 12000)) {
            for (h in listOf(1, 127, 1023, 2048, 2992, 6001)) {
                for (side in listOf(1f, 57.3f, 204.8f, 512f, 1024f, 2048f)) {
                    for (fit in listOf(false, true)) {
                        val s = sample(w, h, side, fit)
                        assertTrue(s > 0 && s and (s - 1) == 0)
                        assertTrue(w / s >= 1 && h / s >= 1)
                        val limiting = if (fit) max(w, h) else min(w, h)
                        if (s > 1) assertTrue(limiting / s >= ceil(side * 2.0))
                        // 불필요하게 전체 원본을 읽는 회귀도 잡는다.
                        val next = s * 2L
                        assertTrue(w / next < 1 || h / next < 1 || limiting / next < ceil(side * 2.0))
                    }
                }
            }
        }
    }

    private fun sample(w: Int, h: Int, side: Float, fit: Boolean = false) =
        exportStickerSampleSize(w, h, side, fit)
}
