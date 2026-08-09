package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.postcardmemory.utils.DoodlePoint
import com.postcardmemory.utils.DoodleStroke
import com.postcardmemory.utils.DoodleStrokeWidth
import com.postcardmemory.utils.DoodleTool
import com.postcardmemory.utils.PostcardRenderSpec
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

    // ---- normalizedDoodlePoint ----

    @Test
    fun normalizedDoodlePoint_topLeftCorner_isZeroZero() {
        val point = normalizedDoodlePoint(Offset(0f, 0f), postcard)

        assertEquals(0f, point!!.x, 0.001f)
        assertEquals(0f, point.y, 0.001f)
    }

    @Test
    fun normalizedDoodlePoint_bottomRightCorner_isOneOne() {
        val point = normalizedDoodlePoint(Offset(1000f, 1000f), postcard)

        assertEquals(1f, point!!.x, 0.001f)
        assertEquals(1f, point.y, 0.001f)
    }

    @Test
    fun normalizedDoodlePoint_center_isHalfHalf() {
        val point = normalizedDoodlePoint(Offset(500f, 500f), postcard)

        assertEquals(0.5f, point!!.x, 0.001f)
        assertEquals(0.5f, point.y, 0.001f)
    }

    @Test
    fun normalizedDoodlePoint_differentCanvasSize_producesSameNormalizedPosition() {
        val smallCanvas = IntSize(400, 400)

        val onThousandCanvas = normalizedDoodlePoint(Offset(250f, 250f), postcard)
        val onSmallCanvas = normalizedDoodlePoint(Offset(100f, 100f), smallCanvas)

        assertEquals(onThousandCanvas!!.x, onSmallCanvas!!.x, 0.001f)
        assertEquals(onThousandCanvas.y, onSmallCanvas.y, 0.001f)
    }

    @Test
    fun normalizedDoodlePoint_outsideBounds_isClampedNotDropped() {
        // 그리는 도중 손가락이 엽서 경계를 벗어나도 입력을 끊지 않고
        // 좌표를 경계로 고정한다(§10: 저장 좌표를 유효 범위로 제한).
        val point = normalizedDoodlePoint(Offset(-50f, 1200f), postcard)

        assertEquals(0f, point!!.x, 0.001f)
        assertEquals(1f, point.y, 0.001f)
    }

    @Test
    fun normalizedDoodlePoint_returnsNullWhenPostcardSizeNotMeasuredYet() {
        assertEquals(null, normalizedDoodlePoint(Offset(10f, 10f), IntSize(0, 0)))
    }

    // ---- doodleEraserHitsStroke ----

    private fun horizontalStroke(
        width: DoodleStrokeWidth = DoodleStrokeWidth.MEDIUM
    ) = DoodleStroke(
        points = listOf(DoodlePoint(0.2f, 0.5f), DoodlePoint(0.8f, 0.5f)),
        colorArgb = 0xFF000000L,
        width = width
    )

    @Test
    fun doodleEraserHitsStroke_touchDirectlyOnLine_hits() {
        val hit = doodleEraserHitsStroke(
            touchPoint = DoodlePoint(0.5f, 0.5f),
            stroke = horizontalStroke(),
            eraserWidth = DoodleStrokeWidth.MEDIUM
        )

        assertTrue(hit)
    }

    @Test
    fun doodleEraserHitsStroke_touchFarFromLine_misses() {
        val hit = doodleEraserHitsStroke(
            touchPoint = DoodlePoint(0.5f, 0.9f),
            stroke = horizontalStroke(),
            eraserWidth = DoodleStrokeWidth.MEDIUM
        )

        assertFalse(hit)
    }

    @Test
    fun doodleEraserHitsStroke_touchOutsideSegmentRange_misses() {
        // x=0.2~0.8 구간 밖(x=0.95)은 아무리 y가 맞아도 스칠 수 없다.
        val hit = doodleEraserHitsStroke(
            touchPoint = DoodlePoint(0.95f, 0.5f),
            stroke = horizontalStroke(),
            eraserWidth = DoodleStrokeWidth.MEDIUM
        )

        assertFalse(hit)
    }

    @Test
    fun doodleEraserHitsStroke_thickerStrokeIsEasierToHit() {
        val nearMissPoint = DoodlePoint(0.5f, 0.514f)

        assertFalse(
            doodleEraserHitsStroke(
                nearMissPoint,
                horizontalStroke(width = DoodleStrokeWidth.THIN),
                DoodleStrokeWidth.THIN
            )
        )
        assertTrue(
            doodleEraserHitsStroke(
                nearMissPoint,
                horizontalStroke(width = DoodleStrokeWidth.THICK),
                DoodleStrokeWidth.THIN
            )
        )
    }

    @Test
    fun doodleEraserHitsStroke_singlePointDot_hitsWithinRadius() {
        val dot = DoodleStroke(
            points = listOf(DoodlePoint(0.5f, 0.5f)),
            colorArgb = 0xFF000000L,
            width = DoodleStrokeWidth.MEDIUM
        )

        assertTrue(
            doodleEraserHitsStroke(DoodlePoint(0.5f, 0.5f), dot, DoodleStrokeWidth.MEDIUM)
        )
        assertFalse(
            doodleEraserHitsStroke(DoodlePoint(0.9f, 0.9f), dot, DoodleStrokeWidth.MEDIUM)
        )
    }

    @Test
    fun doodleEraserHitsStroke_emptyStroke_neverHits() {
        val empty = DoodleStroke(
            points = emptyList(),
            colorArgb = 0xFF000000L,
            width = DoodleStrokeWidth.MEDIUM
        )

        assertFalse(
            doodleEraserHitsStroke(DoodlePoint(0.5f, 0.5f), empty, DoodleStrokeWidth.THICK)
        )
    }

    @Test
    fun doodleEraserHitsStroke_highlighterErasesAtItsWiderVisibleWidth() {
        // 형광펜은 같은 굵기 단계의 펜보다 넓게 그려진다. 펜이라면 빗나갈
        // 거리에서도 형광펜은 눈에 보이는 만큼 지워져야 한다.
        val penHitRadius =
            DoodleStrokeWidth.MEDIUM.logicalWidth /
                    PostcardRenderSpec.LOGICAL_SIZE
        val justOutsidePen = 0.5f + penHitRadius * 1.2f

        val touch = DoodlePoint(0.5f, justOutsidePen)

        assertFalse(
            doodleEraserHitsStroke(
                touch,
                horizontalStroke().copy(tool = DoodleTool.PEN),
                DoodleStrokeWidth.MEDIUM
            )
        )
        assertTrue(
            doodleEraserHitsStroke(
                touch,
                horizontalStroke().copy(tool = DoodleTool.HIGHLIGHTER),
                DoodleStrokeWidth.MEDIUM
            )
        )
    }

    // ---- createTextStickerOverlayForExport/createTextStickerOverlaysForExport ----
    // TextStickerItem은 Uri를 다루지 않아 도장과 마찬가지로 순수 JUnit에서 검증 가능하다.

    private val textSticker100 = IntSize(100, 100)

    private fun textSticker(
        text: String = "SUMMER",
        offset: Offset? = Offset(450f, 450f),
        scale: Float = 1f,
        rotationDegrees: Float = 0f,
        colorArgb: Long = 0xFF112233L
    ) = TextStickerItem(
        text = text,
        offset = offset,
        scale = scale,
        rotationDegrees = rotationDegrees,
        colorArgb = colorArgb
    )

    @Test
    fun createTextStickerOverlay_computesNormalizedPositionFromOffset() {
        val overlay = createTextStickerOverlayForExport(
            text = "SUMMER",
            colorArgb = 0xFF112233L,
            rotationDegrees = 0f,
            textStickerOffset = Offset(250f, 500f),
            postcardSize = postcard,
            textStickerSize = textSticker100,
            fontSizePx = 64f
        )

        requireNotNull(overlay)
        assertEquals(0.25f, overlay.normalizedX, 0.001f)
        assertEquals(0.5f, overlay.normalizedY, 0.001f)
    }

    @Test
    fun createTextStickerOverlay_fontSizeRatioIsFontSizeDividedByPostcardWidth() {
        val overlay = createTextStickerOverlayForExport(
            text = "SUMMER",
            colorArgb = 0xFF112233L,
            rotationDegrees = 0f,
            textStickerOffset = Offset(0f, 0f),
            postcardSize = postcard, // width = 1000
            textStickerSize = textSticker100,
            fontSizePx = 40f
        )

        requireNotNull(overlay)
        assertEquals(0.04f, overlay.fontSizeRatio, 0.0001f)
    }

    @Test
    fun createTextStickerOverlay_zeroFontSize_returnsNull() {
        val overlay = createTextStickerOverlayForExport(
            text = "SUMMER",
            colorArgb = 0xFF112233L,
            rotationDegrees = 0f,
            textStickerOffset = Offset(0f, 0f),
            postcardSize = postcard,
            textStickerSize = textSticker100,
            fontSizePx = 0f
        )

        assertEquals(null, overlay)
    }

    @Test
    fun createTextStickerOverlaysForExport_missingSizeEntry_isDropped() {
        // 사진/도장은 fallback 크기로 대체하지만, 텍스트 스티커는 문자열마다 폭이
        // 전혀 달라 고정 fallback을 쓸 수 없으므로 측정값이 없으면 건너뛴다.
        val stickers = listOf(
            textSticker().copy(id = "missing-size")
        )

        val overlays = createTextStickerOverlaysForExport(
            textStickers = stickers,
            postcardSize = postcard,
            textStickerSizes = emptyMap(),
            baseFontSizePx = 64f
        )

        assertTrue(overlays.isEmpty())
    }

    @Test
    fun createTextStickerOverlaysForExport_allMeasurementsPresent_allIncluded() {
        val a = textSticker().copy(id = "a")
        val b = textSticker(offset = Offset(50f, 50f)).copy(id = "b")

        val overlays = createTextStickerOverlaysForExport(
            textStickers = listOf(a, b),
            postcardSize = postcard,
            textStickerSizes = mapOf("a" to textSticker100, "b" to textSticker100),
            baseFontSizePx = 64f
        )

        assertEquals(2, overlays.size)
    }

    @Test
    fun createTextStickerOverlaysForExport_oneMissingOneMeasured_onlyMeasuredIncluded() {
        val measured = textSticker().copy(id = "measured")
        val missing = textSticker(offset = Offset(50f, 50f)).copy(id = "missing")

        val overlays = createTextStickerOverlaysForExport(
            textStickers = listOf(measured, missing),
            postcardSize = postcard,
            textStickerSizes = mapOf("measured" to textSticker100),
            baseFontSizePx = 64f
        )

        assertEquals(1, overlays.size)
        assertEquals("SUMMER", overlays.first().text)
    }
}
