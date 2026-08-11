package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MaskingTapeItem은 Float/Long/String만 다루는 순수 데이터 클래스라
 * Robolectric 없이 검증할 수 있다. PostcardSealItemTest/TextStickerItemTest와
 * 동일한 구성.
 */
class MaskingTapeItemTest {

    private fun sampleMaskingTape(
        style: MaskingTapeStyle = MaskingTapeStyle.MINT_CHECK
    ) = MaskingTapeItem(
        id = "masking-tape-1",
        style = style,
        offset = Offset(0.2f, -0.4f),
        scale = 0.8f,
        rotationDegrees = -12f
    )

    @Test
    fun serialize_thenParse_roundTripsExactly() {
        val tape = sampleMaskingTape()

        val parsed = deserializeMaskingTapeItem(tape.serialize())

        assertEquals(tape, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsExactly_nullOffset() {
        val tape = sampleMaskingTape().copy(offset = null)

        val parsed = deserializeMaskingTapeItem(tape.serialize())

        assertEquals(tape, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsEveryStyle() {
        MaskingTapeStyle.entries.forEach { style ->
            val tape = sampleMaskingTape(style = style)

            val parsed = deserializeMaskingTapeItem(tape.serialize())

            assertEquals(style, parsed?.style)
        }
    }

    @Test
    fun parse_tooFewFields_returnsNull() {
        assertNull(deserializeMaskingTapeItem("only\tthree\tfields"))
    }

    @Test
    fun parse_unknownStyleName_returnsNull() {
        // 향후 enum 값이 없어지거나 이름이 바뀌면(구버전 데이터) 임의의 다른
        // 디자인으로 조용히 대체하지 않고 이 항목만 버려야 한다.
        val corrupted =
            listOf("masking-tape-1", "NO_LONGER_EXISTS", "~", "~", "1.0", "0")
                .joinToString("\t")

        assertNull(deserializeMaskingTapeItem(corrupted))
    }

    @Test
    fun parse_nonNumericScale_returnsNull() {
        val corrupted =
            listOf("masking-tape-1", "MINT_CHECK", "~", "~", "not-a-float", "0")
                .joinToString("\t")

        assertNull(deserializeMaskingTapeItem(corrupted))
    }

    // ---- 44일차: 길이(lengthScale)/가장자리(edgeStyle)/커스텀 색상·패턴 필드 ----

    @Test
    fun serialize_thenParse_roundTripsLengthScaleAndEdgeStyle() {
        val tape = sampleMaskingTape().copy(
            lengthScale = 1.8f,
            edgeStyle = MaskingTapeEdgeStyle.JAGGED_BOTH
        )

        val parsed = deserializeMaskingTapeItem(tape.serialize())

        assertEquals(tape, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsCustomColorsAndPatternKind() {
        val tape = sampleMaskingTape(style = MaskingTapeStyle.CUSTOM).copy(
            customBaseColorArgb = 0xFF123456L,
            customPatternColorArgb = 0xFF654321L,
            customPatternKind = MaskingTapePatternKind.HEART
        )

        val parsed = deserializeMaskingTapeItem(tape.serialize())

        assertEquals(tape, parsed)
    }

    @Test
    fun deserialize_lineWithoutNewFields_treatedAsDefaults() {
        // 44일차(길이/가장자리/커스텀) 이전, 6개 필드만 있던 구버전 라인.
        val legacyLine =
            listOf("masking-tape-1", "MINT_CHECK", "~", "~", "1.0", "0")
                .joinToString("\t")

        val parsed = deserializeMaskingTapeItem(legacyLine)

        assertEquals(1f, parsed?.lengthScale)
        assertEquals(MaskingTapeEdgeStyle.NOTCHED_BOTH, parsed?.edgeStyle)
        assertNull(parsed?.customBaseColorArgb)
        assertNull(parsed?.customPatternColorArgb)
        assertNull(parsed?.customPatternKind)
        assertNull(parsed?.photoUri)
        assertEquals(1f, parsed?.thicknessScale)
    }

    // ---- 굵기(thicknessScale) 필드: 44일차 후속(핀치 정상화) 작업에서 추가 ----

    @Test
    fun serialize_thenParse_roundTripsThicknessScale() {
        val tape = sampleMaskingTape().copy(thicknessScale = 2.4f)

        val parsed = deserializeMaskingTapeItem(tape.serialize())

        assertEquals(tape, parsed)
    }

    @Test
    fun deserialize_lineWithoutThicknessScale_defaultsToOne() {
        // thicknessScale 추가 이전, 12개 필드(photoUri까지)만 있던 라인.
        val lineWithoutThickness =
            listOf(
                "masking-tape-1", "MINT_CHECK", "~", "~", "1.0", "0",
                "1.0", "NOTCHED_BOTH", "~", "~", "~", "~"
            ).joinToString("\t")

        val parsed = deserializeMaskingTapeItem(lineWithoutThickness)

        assertEquals(1f, parsed?.thicknessScale)
    }

    @Test
    fun effectiveColors_forPresetStyle_returnPresetValuesRegardlessOfCustomFields() {
        // style이 CUSTOM이 아니면 customBaseColorArgb 등이 값을 갖고 있어도 무시된다.
        val tape = sampleMaskingTape(style = MaskingTapeStyle.LAVENDER_DOT).copy(
            customBaseColorArgb = 0xFF000000L
        )

        assertEquals(MaskingTapeStyle.LAVENDER_DOT.baseColorArgb, tape.effectiveBaseColorArgb())
    }

    @Test
    fun effectiveColors_forCustomStyle_returnCustomValues() {
        val tape = sampleMaskingTape(style = MaskingTapeStyle.CUSTOM).copy(
            customBaseColorArgb = 0xFF111111L,
            customPatternColorArgb = 0xFF222222L,
            customPatternKind = MaskingTapePatternKind.STRIPE
        )

        assertEquals(0xFF111111L, tape.effectiveBaseColorArgb())
        assertEquals(0xFF222222L, tape.effectivePatternColorArgb())
        assertEquals(MaskingTapePatternKind.STRIPE, tape.effectivePatternKind())
    }

    @Test
    fun effectiveColors_forCustomStyleWithMissingFields_fallBackToPresetPlaceholder() {
        // 커스텀 필드가 비어 있어도(방어적 상황) 크래시 없이 CUSTOM 엔트리의
        // 폴백 값을 반환한다.
        val tape = sampleMaskingTape(style = MaskingTapeStyle.CUSTOM)

        assertEquals(MaskingTapeStyle.CUSTOM.baseColorArgb, tape.effectiveBaseColorArgb())
        assertEquals(MaskingTapeStyle.CUSTOM.patternColorArgb, tape.effectivePatternColorArgb())
        assertEquals(MaskingTapeStyle.CUSTOM.patternKind, tape.effectivePatternKind())
    }

    @Test
    fun presetMaskingTapeStyles_excludesCustomAndPhoto() {
        assertTrue(MaskingTapeStyle.CUSTOM !in presetMaskingTapeStyles)
        assertTrue(MaskingTapeStyle.PHOTO !in presetMaskingTapeStyles)
        assertEquals(MaskingTapeStyle.entries.size - 2, presetMaskingTapeStyles.size)
    }

    // ---- maskingTapeOutlinePoints: Compose·Canvas가 공유하는 순수 좌표 함수 ----

    @Test
    fun outlinePoints_straight_isPlainRectangle() {
        val points = maskingTapeOutlinePoints(100f, 40f, MaskingTapeEdgeStyle.STRAIGHT)

        assertEquals(
            listOf(0f to 0f, 100f to 0f, 100f to 40f, 0f to 40f),
            points
        )
    }

    @Test
    fun outlinePoints_notchedBoth_cutsTopLeftAndBottomRightCorners() {
        val points = maskingTapeOutlinePoints(100f, 40f, MaskingTapeEdgeStyle.NOTCHED_BOTH)

        // 첫 점은 top-left 모서리를 잘라낸 지점(x>0, y=0), 끝에서 두 번째는
        // bottom-right 모서리를 잘라낸 지점(x<width, y=height)이어야 한다.
        assertEquals(6, points.size)
        assertTrue(points[0].first > 0f && points[0].second == 0f)
        assertEquals(100f to 0f, points[1])
    }

    @Test
    fun outlinePoints_tornOneSide_leftEdgeStaysStraightAtZero() {
        val points = maskingTapeOutlinePoints(100f, 40f, MaskingTapeEdgeStyle.TORN_ONE_SIDE)

        // 처음(top-left)과 마지막(bottom-left) 점은 왼쪽 끝이 일자임을 보장한다.
        assertEquals(0f to 0f, points.first())
        assertEquals(0f to 40f, points.last())
        // 오른쪽 끝의 첫/끝 지점은 delta=0이라 정확히 (width,0)/(width,height)에 닿는다.
        assertEquals(100f to 0f, points[1])
        assertEquals(100f to 40f, points[points.size - 2])
    }

    @Test
    fun outlinePoints_jaggedBoth_bothEdgesMeetTopAndBottomCleanly() {
        val points = maskingTapeOutlinePoints(100f, 40f, MaskingTapeEdgeStyle.JAGGED_BOTH)

        // 오른쪽 지그재그의 첫 점(top)과 왼쪽 지그재그의 마지막 점(top)이
        // 이어져야 빈틈없는 폐곡선이 된다 — 양쪽 다 y=0에서 만난다.
        assertEquals(0f, points.first().second, 0.001f)
        assertEquals(0f, points.last().second, 0.001f)
    }

    @Test
    fun outlinePoints_zeroSize_returnsEmptyList() {
        assertEquals(emptyList<Pair<Float, Float>>(), maskingTapeOutlinePoints(0f, 40f, MaskingTapeEdgeStyle.STRAIGHT))
        assertEquals(emptyList<Pair<Float, Float>>(), maskingTapeOutlinePoints(100f, 0f, MaskingTapeEdgeStyle.NOTCHED_BOTH))
    }
}
