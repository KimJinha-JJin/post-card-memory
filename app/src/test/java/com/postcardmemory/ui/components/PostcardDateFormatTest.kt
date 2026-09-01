package com.postcardmemory.ui.components

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PostcardDateFormat은 값과 무관하게 항상 yyyy-MM-dd를 반환해야 한다
 * (엽서 날짜 스타일 선택 기능은 제거되었고, ENGLISH_SHORT/ENGLISH_LONG 같은
 * 값은 기존 저장 데이터 파싱 호환을 위해서만 남아 있음).
 */
class PostcardDateFormatTest {

    private fun millisFor(year: Int, month: Int, day: Int): Long {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.clear()
        calendar.set(year, month - 1, day, 12, 0, 0)
        return calendar.timeInMillis
    }

    @Test
    fun formatIso_padsSingleDigitMonthAndDay() {
        assertEquals("2026-01-01", PostcardDateFormat.formatIso(millisFor(2026, 1, 1)))
    }

    @Test
    fun formatIso_regularDate() {
        assertEquals("2026-08-02", PostcardDateFormat.formatIso(millisFor(2026, 8, 2)))
    }

    @Test
    fun formatIso_yearEndDoesNotRollYear() {
        assertEquals("2026-12-31", PostcardDateFormat.formatIso(millisFor(2026, 12, 31)))
    }

    @Test
    fun formatIso_leapDayFormatsCorrectly() {
        assertEquals("2024-02-29", PostcardDateFormat.formatIso(millisFor(2024, 2, 29)))
    }

    @Test
    fun formatIso_neverContainsEnglishMonthAbbreviation() {
        val august = PostcardDateFormat.formatIso(millisFor(2026, 8, 2))
        assertFalse(august.contains("AUG", ignoreCase = true))
        assertTrue(august.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
    }

    @Test
    fun format_allEnumValuesProduceIdenticalOutput() {
        val capturedAt = millisFor(2026, 8, 2)

        val results = PostcardDateFormat.entries.map { it.format(capturedAt) }.toSet()

        assertEquals(setOf("2026-08-02"), results)
    }
}
