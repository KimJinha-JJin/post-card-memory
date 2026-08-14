package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LabelStickerItem과 라벨 폭·높이 계산은 Float/Long/String만 다루는 순수
 * 코드라 Robolectric 없이 검증할 수 있다. 실제 문자열 측정(Paint)만
 * 실기기 영역이고, 측정값을 받은 뒤의 규칙은 전부 여기서 확인한다.
 */
class LabelStickerItemTest {

    private fun sampleLabelSticker(
        text: String = "SUMMER 2026",
        style: LabelTapeStyle = LabelTapeStyle.BLUE
    ) = LabelStickerItem(
        id = "label-sticker-1",
        text = text,
        style = style,
        offset = Offset(12.5f, -30.25f),
        scale = 1f,
        rotationDegrees = -7.5f
    )

    @Test
    fun serialize_thenParse_roundTripsExactly() {
        val label = sampleLabelSticker()

        val parsed = deserializeLabelStickerItem(label.serialize())

        assertEquals(label, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsEveryTapeStyle() {
        LabelTapeStyle.entries.forEach { style ->
            val label = sampleLabelSticker(style = style)

            val parsed = deserializeLabelStickerItem(label.serialize())

            assertEquals(label, parsed)
        }
    }

    @Test
    fun serialize_thenParse_roundTripsKoreanText() {
        val label = sampleLabelSticker(text = "오늘의 기록")

        val parsed = deserializeLabelStickerItem(label.serialize())

        assertEquals(label, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsNullOffset() {
        val label = sampleLabelSticker().copy(offset = null)

        val parsed = deserializeLabelStickerItem(label.serialize())

        assertNotNull(parsed)
        assertNull(parsed!!.offset)
    }

    @Test
    fun deserializeLabelStickerItem_returnsNullForTruncatedLine() {
        assertNull(deserializeLabelStickerItem("label-1\tSEOUL\tBLACK"))
    }

    @Test
    fun deserializeLabelStickerItem_returnsNullForUnknownTapeStyle() {
        // 인식하지 못하는 테이프는 임의의 다른 색으로 대체하지 않고 이 항목만 버린다.
        val line = "label-1\tSEOUL\tNEON_PINK\t0.0\t0.0\t1.0\t0.0"

        assertNull(deserializeLabelStickerItem(line))
    }

    @Test
    fun deserializeLabelStickerItem_returnsNullForNonNumericField() {
        val line = "label-1\tSEOUL\tBLACK\t0.0\t0.0\tnot-a-float\t0.0"

        assertNull(deserializeLabelStickerItem(line))
    }

    // ---- 자동 폭 계산 ----

    @Test
    fun labelStickerWidthPx_addsSidePaddingToBothEnds() {
        val fontSizePx = 40f
        val measured = 200f

        val width = labelStickerWidthPx(measured, fontSizePx)

        assertEquals(
            measured + fontSizePx * LABEL_STICKER_SIDE_PADDING_RATIO * 2f,
            width,
            0.001f
        )
    }

    @Test
    fun labelStickerWidthPx_growsWithLongerText() {
        val fontSizePx = 40f

        val shortLabel = labelStickerWidthPx(30f, fontSizePx)
        val mediumLabel = labelStickerWidthPx(150f, fontSizePx)
        val longLabel = labelStickerWidthPx(420f, fontSizePx)

        assertTrue(shortLabel < mediumLabel)
        assertTrue(mediumLabel < longLabel)
    }

    @Test
    fun labelStickerWidthPx_keepsMinimumWidthForVeryShortText() {
        val fontSizePx = 40f
        // "A" 한 글자처럼 아주 짧은 문자열이라 여백을 더해도 최소 폭에 못 미친다.
        val width = labelStickerWidthPx(4f, fontSizePx)

        assertEquals(
            fontSizePx * LABEL_STICKER_MIN_WIDTH_RATIO,
            width,
            0.001f
        )
    }

    @Test
    fun labelStickerWidthPx_scalesWithFontSize() {
        // 같은 문자열이라도 글자가 커지면(=export 해상도) 폭이 같은 비율로 커진다.
        val small = labelStickerWidthPx(100f, 20f)
        val large = labelStickerWidthPx(200f, 40f)

        assertEquals(small * 2f, large, 0.001f)
    }

    @Test
    fun labelStickerHeightPx_isIndependentOfText() {
        // 테이프 두께는 문구 종류(영문/한글)와 무관하게 글자 크기만으로 정해진다.
        assertEquals(
            30f * LABEL_STICKER_HEIGHT_RATIO,
            labelStickerHeightPx(30f),
            0.001f
        )
    }

    // ---- 기타 색상(커스텀 테이프) ----

    @Test
    fun serialize_thenParse_roundTripsCustomTapeColor() {
        val label = sampleLabelSticker().copy(
            style = LabelTapeStyle.CUSTOM,
            customTapeColorArgb = 0xFF7FD4C1L
        )

        val parsed = deserializeLabelStickerItem(label.serialize())

        assertEquals(label, parsed)
    }

    @Test
    fun deserializeLabelStickerItem_legacyLineWithoutCustomColor_fallsBackToPreset() {
        // 기타 색상 도입 전에 저장된 7필드 라인. 그대로 복원되고 커스텀 색은 없다.
        val line = "label-1\tSEOUL\tGREEN\t10.0\t20.0\t1.0\t0.0"

        val parsed = deserializeLabelStickerItem(line)

        assertNotNull(parsed)
        assertNull(parsed!!.customTapeColorArgb)
        assertEquals(LabelTapeStyle.GREEN, parsed.style)

        // 프리셋 외형은 기타 색상 추가 전과 완전히 동일해야 한다.
        val palette = parsed.tapePalette()
        assertEquals(LabelTapeStyle.GREEN.baseColorArgb, palette.baseColorArgb)
        assertEquals(LabelTapeStyle.GREEN.edgeColorArgb, palette.edgeColorArgb)
        assertEquals(LabelTapeStyle.GREEN.textColorArgb, palette.textColorArgb)
    }

    @Test
    fun labelTapePalette_returnsFixedPresetColorsUnchanged() {
        // 기타 색상이 생겼다고 기존 6종 프리셋 색·문자색이 달라지면 안 된다.
        presetLabelTapeStyles.forEach { style ->
            val palette = labelTapePalette(style, customTapeColorArgb = null)

            assertEquals(style.baseColorArgb, palette.baseColorArgb)
            assertEquals(style.edgeColorArgb, palette.edgeColorArgb)
            assertEquals(style.textColorArgb, palette.textColorArgb)
        }
    }

    @Test
    fun labelTapePalette_ignoresCustomColorOnPresetStyle() {
        // 프리셋을 다시 고르면 남아 있는 커스텀 색이 새어 나오지 않아야 한다.
        val palette =
            labelTapePalette(
                style = LabelTapeStyle.RED,
                customTapeColorArgb = 0xFF00FF00L
            )

        assertEquals(LabelTapeStyle.RED.baseColorArgb, palette.baseColorArgb)
    }

    @Test
    fun labelTapePalette_customUsesChosenBaseColorAndDarkerEdge() {
        val chosen = 0xFF4488CCL

        val palette =
            labelTapePalette(
                style = LabelTapeStyle.CUSTOM,
                customTapeColorArgb = chosen
            )

        assertEquals(chosen, palette.baseColorArgb)
        assertTrue(
            "단면 음영은 바탕색보다 어두워야 한다",
            labelStickerLuminance(palette.edgeColorArgb) <
                labelStickerLuminance(chosen)
        )
    }

    @Test
    fun labelTapePalette_customWithoutColorFallsBackToStyleDefault() {
        val palette =
            labelTapePalette(
                style = LabelTapeStyle.CUSTOM,
                customTapeColorArgb = null
            )

        assertEquals(LabelTapeStyle.CUSTOM.baseColorArgb, palette.baseColorArgb)
    }

    // ---- 자동 문자색 ----

    @Test
    fun labelStickerTextColorArgbFor_picksDarkTextOnBrightTape() {
        listOf(
            0xFFFFF176L, // 밝은 노랑
            0xFFFFC1D9L, // 연분홍
            0xFFB2F0E3L, // 민트
            0xFFFFFFFFL
        ).forEach { tape ->
            assertEquals(
                LABEL_STICKER_DARK_TEXT_ARGB,
                labelStickerTextColorArgbFor(tape)
            )
        }
    }

    @Test
    fun labelStickerTextColorArgbFor_picksLightTextOnDarkTape() {
        listOf(
            0xFF12233FL, // 짙은 네이비
            0xFF5C1A4EL, // 짙은 자주
            0xFF555555L, // 중간 회색
            0xFF000000L
        ).forEach { tape ->
            assertEquals(
                LABEL_STICKER_LIGHT_TEXT_ARGB,
                labelStickerTextColorArgbFor(tape)
            )
        }
    }

    /**
     * 자동 대비 계산이 6종 프리셋에 손으로 지정해 둔 문자색과 같은 쪽(밝은/어두운)을
     * 고르는지 고정한다 — 임계값을 건드리면 커스텀 라벨만 프리셋과 따로 놀게 된다.
     */
    @Test
    fun labelStickerTextColorArgbFor_agreesWithEveryPresetChoice() {
        presetLabelTapeStyles.forEach { style ->
            val presetIsDarkText =
                labelStickerLuminance(style.textColorArgb) <
                    labelStickerLuminance(style.baseColorArgb)

            val autoIsDarkText =
                labelStickerTextColorArgbFor(style.baseColorArgb) ==
                    LABEL_STICKER_DARK_TEXT_ARGB

            assertEquals(
                "${style.name}: 프리셋 문자색과 자동 계산 결과의 명암 방향이 달라졌다",
                presetIsDarkText,
                autoIsDarkText
            )
        }
    }

    @Test
    fun presetLabelTapeStyles_excludesCustomMarkerEntry() {
        assertFalse(presetLabelTapeStyles.contains(LabelTapeStyle.CUSTOM))
        assertEquals(LabelTapeStyle.entries.size - 1, presetLabelTapeStyles.size)
    }
}
