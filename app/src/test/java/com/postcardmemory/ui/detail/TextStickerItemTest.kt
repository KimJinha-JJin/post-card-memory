package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * TextStickerItem은 Float/Long/String만 다루는 순수 데이터 클래스라
 * Robolectric 없이 검증할 수 있다. PostcardSealItemTest와 동일한 구성.
 */
class TextStickerItemTest {

    private fun sampleTextSticker(text: String = "오늘 최고!") =
        TextStickerItem(
            id = "text-sticker-1",
            text = text,
            offset = Offset(0.2f, -0.4f),
            scale = 0.6f,
            rotationDegrees = 15f,
            colorArgb = 0xFF112233L
        )

    @Test
    fun serialize_thenParse_roundTripsExactly() {
        val sticker = sampleTextSticker()

        val parsed = deserializeTextStickerItem(sticker.serialize())

        assertEquals(sticker, parsed)
    }

    @Test
    fun serialize_thenParse_roundTripsExactly_nullOffset() {
        val sticker = sampleTextSticker().copy(offset = null)

        val parsed = deserializeTextStickerItem(sticker.serialize())

        assertEquals(sticker, parsed)
    }

    // ---- 카오모지/Unicode 문자열이 저장·복원 과정에서 손상되지 않는다 ----

    @Test
    fun serialize_thenParse_preservesKaomoji() {
        val kaomojis = listOf(
            "( ˶ˆᗜˆ˵ )",
            "ᕕ( ᐛ )ᕗ",
            "ಠ_ಠ",
            "(╯°□°）╯︵ ┻━┻",
            "૮ ˶ᵔ ᵕ ᵔ˶ ა"
        )

        kaomojis.forEach { kaomoji ->
            val sticker = sampleTextSticker(text = kaomoji)

            val parsed = deserializeTextStickerItem(sticker.serialize())

            assertEquals(kaomoji, parsed?.text)
        }
    }

    @Test
    fun serialize_thenParse_preservesLeadingAndTrailingSpaces() {
        val sticker = sampleTextSticker(text = "  SUMMER!  ")

        val parsed = deserializeTextStickerItem(sticker.serialize())

        assertEquals("  SUMMER!  ", parsed?.text)
    }

    @Test
    fun parse_tooFewFields_returnsNull() {
        assertNull(deserializeTextStickerItem("only\tthree\tfields"))
    }

    @Test
    fun parse_nonNumericScale_returnsNull() {
        val corrupted =
            listOf("text-sticker-1", "SUMMER!", "~", "~", "not-a-float", "0", "16777215")
                .joinToString("\t")

        assertNull(deserializeTextStickerItem(corrupted))
    }

    // ---- 테두리색(outlineColorArgb) ----

    @Test
    fun serialize_thenParse_roundTripsOutlineColor() {
        val sticker = sampleTextSticker().copy(outlineColorArgb = 0xFFD9C7F5L)

        val parsed = deserializeTextStickerItem(sticker.serialize())

        assertEquals(0xFFD9C7F5L, parsed?.outlineColorArgb)
        assertEquals(sticker, parsed)
    }

    @Test
    fun parse_legacyLineWithoutOutlineColor_fallsBackToWhite() {
        // outlineColorArgb 필드가 없던 구버전 저장 라인(7개 필드)을 흉내낸다.
        val legacyLine =
            listOf("text-sticker-1", "SUMMER!", "~", "~", "1.0", "0.0", "4283782485")
                .joinToString("\t")

        val parsed = deserializeTextStickerItem(legacyLine)

        assertEquals(DEFAULT_TEXT_STICKER_OUTLINE_COLOR_ARGB, parsed?.outlineColorArgb)
    }
}
