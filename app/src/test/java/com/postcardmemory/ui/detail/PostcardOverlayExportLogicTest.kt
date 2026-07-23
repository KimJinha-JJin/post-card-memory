package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 4일차: 미리보기와 Export의 도장 가장자리 정책 일치, 측정값 미준비 시
 * fallback 크기 사용을 검증한다. Offset/IntSize는 android.net.Uri와 달리
 * Android 스텁에 의존하지 않는 순수 JVM 클래스라 Robolectric 없이 검증
 * 가능하다(스티커 쪽 createStickerOverlayForExport는 Uri를 받아 이 프로젝트의
 * 순수 JUnit 환경에서 직접 호출할 수 없다 — PostcardEditDraftTest.kt 참고).
 */
class PostcardOverlayExportLogicTest {

    private val postcard = IntSize(1000, 1000)
    private val seal100 = IntSize(100, 100)
    private val minVisibleEdgePx = 24f

    // ---- correctSealOffsetForMinimumVisibility ----

    @Test
    fun correctSeal_centered_isUnchanged() {
        val centered = Offset(450f, 450f) // 100짜리 도장을 1000짜리 캔버스 중앙에

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = centered,
            sealSize = seal100,
            rotationDegrees = 0f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        assertEquals(centered, corrected)
    }

    @Test
    fun correctSeal_halfHangingRightEdge_alreadyWithinPolicy_isUnchanged() {
        // 중심이 오른쪽 가장자리에 걸쳐 절반만 보이는 배치 — 정책상 이미 허용 범위.
        val halfHangRight = Offset(950f, 450f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = halfHangRight,
            sealSize = seal100,
            rotationDegrees = 0f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        assertEquals(halfHangRight, corrected)
    }

    @Test
    fun correctSeal_draggedFullyOffRightEdge_pulledBackToMinimumVisibleFraction_notFullyInside() {
        // 완전히 캔버스 밖으로 나간 경우: 최소 가시 폭(30% = 30px)만 남기고
        // 보정돼야 한다 — clampStickerOffset처럼 완전히 안쪽으로 밀어넣으면 안 된다.
        val fullyOffRight = Offset(1000f, 450f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = fullyOffRight,
            sealSize = seal100,
            rotationDegrees = 0f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        // 최소 가시 폭 30px만 남기고 나머지는 밖으로 걸쳐 있어야 한다.
        assertEquals(970f, corrected.x, 0.01f)
        // clampStickerOffset이었다면 900f(완전히 안쪽)가 됐을 것 — 그렇지 않음을 확인.
        assertFalse(corrected.x == 900f)
    }

    @Test
    fun correctSeal_draggedFullyOffLeftEdge_producesNegativeOffset() {
        val fullyOffLeft = Offset(-100f, 450f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = fullyOffLeft,
            sealSize = seal100,
            rotationDegrees = 0f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        // 왼쪽으로 걸친 상태를 표현하려면 음수 offset이 나와야 한다.
        assertTrue(corrected.x < 0f)
        assertEquals(-70f, corrected.x, 0.01f) // minVisibleWidth(30) - rotatedWidth(100)
    }

    @Test
    fun correctSeal_draggedFullyOffTopEdge_producesNegativeYOffset() {
        val fullyOffTop = Offset(450f, -100f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = fullyOffTop,
            sealSize = seal100,
            rotationDegrees = 0f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        assertTrue(corrected.y < 0f)
    }

    @Test
    fun correctSeal_draggedFullyOffBottomEdge_pulledBackToMinimumVisibleFraction() {
        val fullyOffBottom = Offset(450f, 1000f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = fullyOffBottom,
            sealSize = seal100,
            rotationDegrees = 0f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        assertEquals(970f, corrected.y, 0.01f)
    }

    @Test
    fun correctSeal_positiveRotation_stillAllowsEdgeOverhang() {
        val fullyOffRight = Offset(1000f, 450f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = fullyOffRight,
            sealSize = seal100,
            rotationDegrees = 30f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        // 회전해도 완전히 안쪽으로 끌려오지 않고 여전히 가장자리에 걸쳐 있어야 한다.
        assertTrue(corrected.x > 900f)
    }

    @Test
    fun correctSeal_negativeRotation_stillAllowsEdgeOverhang() {
        val fullyOffRight = Offset(1000f, 450f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = fullyOffRight,
            sealSize = seal100,
            rotationDegrees = -30f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        assertTrue(corrected.x > 900f)
    }

    @Test
    fun correctSeal_miniStampSmallSize_stillCorrectsWithoutCrashing() {
        // 미니 하트/별 도장(defaultScale=0.55 근처)에 해당하는 작은 크기.
        val miniSeal = IntSize(50, 50)
        val fullyOffRight = Offset(1000f, 450f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = fullyOffRight,
            sealSize = miniSeal,
            rotationDegrees = 0f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        assertTrue(corrected.x in 900f..1000f)
    }

    @Test
    fun correctSeal_largeSize_stillCorrectsWithoutCrashing() {
        // 최대 scale(3배) 근처의 큰 도장.
        val largeSeal = IntSize(270, 270)
        val fullyOffRight = Offset(1000f, 450f)

        val corrected = correctSealOffsetForMinimumVisibility(
            offset = fullyOffRight,
            sealSize = largeSeal,
            rotationDegrees = 0f,
            postcardSize = postcard,
            minimumVisibleEdgePx = minVisibleEdgePx
        )

        assertTrue(corrected.x < 1000f)
    }

    @Test
    fun correctSeal_zeroSizes_returnsOffsetUnchanged_doesNotCrash() {
        val offset = Offset(100f, 100f)

        assertEquals(
            offset,
            correctSealOffsetForMinimumVisibility(
                offset = offset,
                sealSize = IntSize.Zero,
                rotationDegrees = 0f,
                postcardSize = postcard,
                minimumVisibleEdgePx = minVisibleEdgePx
            )
        )
        assertEquals(
            offset,
            correctSealOffsetForMinimumVisibility(
                offset = offset,
                sealSize = seal100,
                rotationDegrees = 0f,
                postcardSize = IntSize.Zero,
                minimumVisibleEdgePx = minVisibleEdgePx
            )
        )
    }

    // ---- computeFallbackOverlaySize ----

    @Test
    fun fallbackSize_matchesBasePxTimesScale() {
        val size = computeFallbackOverlaySize(basePx = 200f, scale = 0.55f)

        assertEquals(110, size.width)
        assertEquals(110, size.height)
    }

    @Test
    fun fallbackSize_isAlwaysSquare() {
        val size = computeFallbackOverlaySize(basePx = 333f, scale = 1.7f)

        assertEquals(size.width, size.height)
    }

    @Test
    fun fallbackSize_neverZeroOrNegative() {
        val size = computeFallbackOverlaySize(basePx = 100f, scale = 0f)

        assertTrue(size.width >= 1)
    }

    // ---- createSealOverlayForExport: 정책 일치 + fallback ----

    private fun seal(
        type: SealType = SealType.HEART,
        offset: Offset? = Offset(950f, 450f),
        scale: Float = 1f,
        rotationDegrees: Float = 0f
    ) = PostcardSealItem(
        type = type,
        offset = offset,
        scale = scale,
        rotationDegrees = rotationDegrees
    )

    @Test
    fun createSealOverlay_edgeHangingSeal_keepsOverhang_notPushedFullyInside() {
        // 미리보기에서 오른쪽 가장자리를 완전히 벗어난 상태로 드래그된 도장.
        val overlay = createSealOverlayForExport(
            type = SealType.HEART,
            colorArgb = 0xFF000000L,
            rotationDegrees = 0f,
            sealOffset = Offset(1000f, 450f),
            postcardSize = postcard,
            sealSize = seal100,
            minimumVisibleEdgePx = minVisibleEdgePx,
            capturedAtMillis = null
        )

        requireNotNull(overlay)
        // normalizedX가 1.0 미만으로 강제로 눌리지 않고, clampStickerOffset이라면
        // 나왔을 0.9(900/1000)보다 커야 한다 — 여전히 가장자리에 걸쳐 있어야 함.
        assertTrue(overlay.normalizedX > 0.9f)
    }

    @Test
    fun createSealOverlaysForExport_missingSizeEntry_includedViaFallback_notDropped() {
        val seals = listOf(
            seal().copy(id = "missing-size-seal")
        )

        val overlays = createSealOverlaysForExport(
            photoSeals = seals,
            postcardSize = postcard,
            sealSizes = emptyMap(), // 측정값이 하나도 없는 상태
            baseSealPx = 200f,
            minimumVisibleEdgePx = minVisibleEdgePx,
            capturedAtMillis = null
        )

        assertEquals(1, overlays.size)
    }

    @Test
    fun createSealOverlaysForExport_allMeasurementsPresent_allIncluded() {
        val sealA = seal().copy(id = "a")
        val sealB = seal(offset = Offset(50f, 50f)).copy(id = "b")

        val overlays = createSealOverlaysForExport(
            photoSeals = listOf(sealA, sealB),
            postcardSize = postcard,
            sealSizes = mapOf("a" to seal100, "b" to seal100),
            baseSealPx = 200f,
            minimumVisibleEdgePx = minVisibleEdgePx,
            capturedAtMillis = null
        )

        assertEquals(2, overlays.size)
    }

    @Test
    fun createSealOverlaysForExport_oneMissingOneMeasured_bothIncluded_noPartialDrop() {
        val measured = seal().copy(id = "measured")
        val missing = seal(offset = Offset(50f, 50f)).copy(id = "missing")

        val overlays = createSealOverlaysForExport(
            photoSeals = listOf(measured, missing),
            postcardSize = postcard,
            sealSizes = mapOf("measured" to seal100), // "missing"은 측정값 없음
            baseSealPx = 200f,
            minimumVisibleEdgePx = minVisibleEdgePx,
            capturedAtMillis = null
        )

        // 하나만 측정됐다고 나머지가 빠진 성공 결과를 반환하면 안 된다.
        assertEquals(2, overlays.size)
    }

    // ---- clampStickerOffset / centeredStickerOffset (일반 스티커 정책, 변경 없음 확인) ----

    @Test
    fun clampStickerOffset_keepsRegularStickerFullyInside_evenIfRequestedOffEdge() {
        val requestedOffOffset = Offset(1000f, 1000f)

        val clamped = clampStickerOffset(
            offset = requestedOffOffset,
            postcardSize = postcard,
            stickerSize = seal100
        )

        // 스티커는 도장과 달리 가장자리 걸침이 없다 — 완전히 안쪽에 있어야 한다.
        assertEquals(900f, clamped.x, 0.01f)
        assertEquals(900f, clamped.y, 0.01f)
    }

    @Test
    fun centeredStickerOffset_centersWithinPostcard() {
        val centered = centeredStickerOffset(
            postcardSize = postcard,
            stickerSize = seal100
        )

        assertEquals(450f, centered.x, 0.01f)
        assertEquals(450f, centered.y, 0.01f)
    }
}
