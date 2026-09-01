package com.postcardmemory.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DoodleStroke는 Float/Long/String만 다루는 순수 데이터 클래스라
 * Robolectric 없이 검증할 수 있다(PostcardSealItemTest와 동일한 전제).
 */
class DoodleStrokeTest {

    private fun sampleStroke() =
        DoodleStroke(
            id = "stroke-1",
            points = listOf(
                DoodlePoint(0f, 0f),
                DoodlePoint(0.25f, 0.5f),
                DoodlePoint(1f, 1f)
            ),
            colorArgb = 0xFF252525L,
            width = DoodleStrokeWidth.MEDIUM
        )

    @Test
    fun serialize_thenParse_roundTripsExactly() {
        val stroke = sampleStroke()

        val parsed = deserializeDoodleStroke(stroke.serialize())

        assertEquals(stroke, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsSinglePointDot() {
        val stroke = sampleStroke().copy(
            points = listOf(DoodlePoint(0.4f, 0.6f))
        )

        val parsed = deserializeDoodleStroke(stroke.serialize())

        assertEquals(stroke, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsAllWidths() {
        DoodleStrokeWidth.entries.forEach { width ->
            val stroke = sampleStroke().copy(width = width)
            assertEquals(stroke, deserializeDoodleStroke(stroke.serialize()))
        }
    }

    @Test
    fun parse_tooFewFields_returnsNull() {
        assertNull(deserializeDoodleStroke("only\tthree\tfields"))
    }

    @Test
    fun parse_unknownWidth_returnsNull_doesNotSubstituteDefault() {
        val line =
            listOf("stroke-1", "16777215", "UNKNOWN_FUTURE_WIDTH", "0.1,0.1")
                .joinToString("\t")

        assertNull(deserializeDoodleStroke(line))
    }

    @Test
    fun parse_emptyPointsList_returnsNull() {
        val line =
            listOf("stroke-1", "16777215", "MEDIUM", "")
                .joinToString("\t")

        assertNull(deserializeDoodleStroke(line))
    }

    @Test
    fun parse_nonNumericColor_returnsNull() {
        val line =
            listOf("stroke-1", "not-a-long", "MEDIUM", "0.1,0.1")
                .joinToString("\t")

        assertNull(deserializeDoodleStroke(line))
    }

    @Test
    fun parse_skipsSingleCorruptedPointButKeepsRestOfStroke() {
        // 두 번째 점만 깨진 경우, 그 점만 버리고 나머지 점으로 획을 복구한다.
        val line =
            listOf(
                "stroke-1",
                "16777215",
                "MEDIUM",
                "0.1,0.1;garbage;0.9,0.9"
            ).joinToString("\t")

        val parsed = deserializeDoodleStroke(line)

        assertEquals(
            listOf(DoodlePoint(0.1f, 0.1f), DoodlePoint(0.9f, 0.9f)),
            parsed?.points
        )
    }

    @Test
    fun parse_doesNotThrowOnGarbageInput() {
        val result = deserializeDoodleStroke(" random\tgarbage\twith\ttabs\t\t")
        // 예외를 던지지 않는지가 핵심 — null이든 아니든 크래시하지 않으면 통과.
        assertTrue(result == null || result.points.isNotEmpty())
    }

    // ---- tool 필드 (형광펜·점선 도입) ----

    @Test
    fun serialize_thenParse_roundTripsAllStorableTools() {
        // ERASER는 획으로 저장되지 않으므로 왕복 대상이 아니다.
        listOf(DoodleTool.PEN, DoodleTool.HIGHLIGHTER, DoodleTool.DOTTED)
            .forEach { tool ->
                val stroke = sampleStroke().copy(tool = tool)
                assertEquals(stroke, deserializeDoodleStroke(stroke.serialize()))
            }
    }

    @Test
    fun parse_legacyFourFieldLine_restoresAsPen_doesNotDiscardStroke() {
        // 도구 필드가 없던 시절에 저장된 낙서. 반드시 살아남아야 한다.
        val line =
            listOf("stroke-1", "16777215", "MEDIUM", "0.1,0.1;0.9,0.9")
                .joinToString("\t")

        val parsed = deserializeDoodleStroke(line)

        assertEquals(DoodleTool.PEN, parsed?.tool)
        assertEquals(
            listOf(DoodlePoint(0.1f, 0.1f), DoodlePoint(0.9f, 0.9f)),
            parsed?.points
        )
    }

    @Test
    fun parse_unknownTool_restoresAsPen_doesNotDiscardStroke() {
        val line =
            listOf(
                "stroke-1",
                "16777215",
                "MEDIUM",
                "0.1,0.1",
                "UNKNOWN_FUTURE_TOOL"
            ).joinToString("\t")

        val parsed = deserializeDoodleStroke(line)

        assertEquals(DoodleTool.PEN, parsed?.tool)
        assertEquals(listOf(DoodlePoint(0.1f, 0.1f)), parsed?.points)
    }

    @Test
    fun parse_blankTool_restoresAsPen() {
        val line =
            listOf("stroke-1", "16777215", "MEDIUM", "0.1,0.1", "")
                .joinToString("\t")

        assertEquals(DoodleTool.PEN, deserializeDoodleStroke(line)?.tool)
    }

    @Test
    fun parse_storedEraserTool_restoresAsPen() {
        // 지우개는 획을 남기지 않는 도구라 저장값으로 들어오면 펜으로 본다.
        val line =
            listOf("stroke-1", "16777215", "MEDIUM", "0.1,0.1", "ERASER")
                .joinToString("\t")

        assertEquals(DoodleTool.PEN, deserializeDoodleStroke(line)?.tool)
    }

    // ---- renderWidth (렌더러와 지우개 판정이 공유하는 값) ----

    @Test
    fun renderWidth_highlighterIsWiderThanSameWidthPen() {
        val pen = sampleStroke().copy(tool = DoodleTool.PEN)
        val highlighter = sampleStroke().copy(tool = DoodleTool.HIGHLIGHTER)

        assertTrue(highlighter.renderWidth > pen.renderWidth)
        assertEquals(
            DoodleStrokeWidth.MEDIUM.logicalWidth * HIGHLIGHTER_WIDTH_MULTIPLIER,
            highlighter.renderWidth
        )
    }

    @Test
    fun renderWidth_penAndDottedUseStoredWidthUnchanged() {
        listOf(DoodleTool.PEN, DoodleTool.DOTTED).forEach { tool ->
            DoodleStrokeWidth.entries.forEach { width ->
                val stroke = sampleStroke().copy(tool = tool, width = width)
                assertEquals(width.logicalWidth, stroke.renderWidth)
            }
        }
    }

    @Test
    fun sanitizedDoodlePoint_clampsOutOfRangeValues() {
        val point = sanitizedDoodlePoint(1.5f, -0.5f)

        assertEquals(1f, point.x)
        assertEquals(0f, point.y)
    }

    @Test
    fun sanitizedDoodlePoint_replacesNaNAndInfiniteWithCenter() {
        assertEquals(DoodlePoint(0.5f, 0.5f), sanitizedDoodlePoint(Float.NaN, Float.NaN))
        assertEquals(
            DoodlePoint(0.5f, 0.5f),
            sanitizedDoodlePoint(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)
        )
    }

    @Test
    fun deserializeDoodleStroke_sanitizesOutOfRangeStoredCoordinates() {
        val line =
            listOf("stroke-1", "16777215", "MEDIUM", "2.0,-3.0")
                .joinToString("\t")

        val parsed = deserializeDoodleStroke(line)

        assertEquals(listOf(DoodlePoint(1f, 0f)), parsed?.points)
    }
}
