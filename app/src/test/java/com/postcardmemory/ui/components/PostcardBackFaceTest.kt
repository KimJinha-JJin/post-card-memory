package com.postcardmemory.ui.components

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 뒷면 To./From. 한 줄 포맷 규칙. 실제 레이아웃(Row 3분할, 한 줄 유지)은
 * Compose UI 테스트 없이는 검증할 수 없으므로, 이중 공백 방지 같은
 * 순수 포맷 로직만 여기서 확인한다.
 */
class PostcardBackFaceTest {

    // ---- To. 수식언이 비어 있으면 "To.  나"처럼 이중 공백이 생기지 않는다 ----

    @Test
    fun backRecipientSuffix_blankModifier_noLeadingSpace() {
        assertEquals("나", backRecipientSuffix(""))
        assertEquals("나", backRecipientSuffix("   "))
    }

    @Test
    fun backRecipientSuffix_nonBlankModifier_hasLeadingSpace() {
        assertEquals(
            " 나",
            backRecipientSuffix("귀엽고 사랑스러운")
        )
    }

    // ---- From. 은 capturedAt 기준 yyyy-MM-dd + 고정 문구 ----

    @Test
    fun formatBackFromLine_usesIsoDateAndFixedWording() {
        val capturedAt =
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .parse("2026-08-08")!!
                .time

        assertEquals(
            "From. 2026-08-08의 나",
            formatBackFromLine(capturedAt)
        )
    }

    @Test
    fun formatBackFromLine_padsSingleDigitMonthAndDay() {
        val capturedAt =
            SimpleDateFormat("yyyy-MM-dd", Locale.US)
                .parse("2026-01-05")!!
                .time

        assertEquals(
            "From. 2026-01-05의 나",
            formatBackFromLine(capturedAt)
        )
    }

    // ---- 갤러리 "뒷면 편지 있음" 배지 판정 ----

    @Test
    fun postcardHasBackContent_bothEmpty_isFalse() {
        assertEquals(
            false,
            postcardHasBackContent("", "")
        )
    }

    @Test
    fun postcardHasBackContent_onlyRecipientModifier_isTrue() {
        assertEquals(
            true,
            postcardHasBackContent("귀여운", "")
        )
    }

    @Test
    fun postcardHasBackContent_onlyMessage_isTrue() {
        assertEquals(
            true,
            postcardHasBackContent("", "오늘도 고생했어.")
        )
    }

    @Test
    fun postcardHasBackContent_bothPresent_isTrue() {
        assertEquals(
            true,
            postcardHasBackContent("귀여운", "오늘도 고생했어.")
        )
    }

    @Test
    fun postcardHasBackContent_onlyWhitespace_isFalse() {
        assertEquals(
            false,
            postcardHasBackContent("   ", "\n  ")
        )
    }
}
