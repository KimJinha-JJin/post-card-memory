package com.postcardmemory.ui.gallery

import java.time.DayOfWeek
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 캘린더 보기(작업지시서 24절)의 날짜 칸 배치를 계산하는
 * [calendarCellsFor]를 검증한다. 일요일 시작 기준으로 앞뒤 빈 칸을
 * 채우고, 항상 7의 배수(온전한 주)로 끝나는지 확인한다.
 */
class GalleryCalendarCellsTest {

    @Test
    fun cellCount_isAlwaysMultipleOfSeven() {
        // 2026년 1월~12월 전부 검사해 어떤 달이 걸려도 항상 7의 배수인지 확인.
        (1..12).forEach { month ->
            val yearMonth = YearMonth.of(2026, month)
            val cells = calendarCellsFor(yearMonth)

            assertTrue(
                "${yearMonth}: 셀 개수(${cells.size})는 7의 배수여야 함",
                cells.size % 7 == 0
            )
        }
    }

    @Test
    fun firstOfMonth_isPlacedUnderCorrectWeekday_sundayStart() {
        // 2026년 9월 1일은 화요일 → 일요일(0), 월요일(1) 다음, index 2.
        val yearMonth = YearMonth.of(2026, 9)
        assertEquals(DayOfWeek.TUESDAY, yearMonth.atDay(1).dayOfWeek)

        val cells = calendarCellsFor(yearMonth)

        assertNull("1일 앞은 빈 칸이어야 함(일)", cells[0])
        assertNull("1일 앞은 빈 칸이어야 함(월)", cells[1])
        assertEquals(yearMonth.atDay(1), cells[2])
    }

    @Test
    fun sundayStartingMonth_hasNoLeadingBlanks() {
        // 2026년 3월 1일은 일요일 → 앞에 빈 칸이 없어야 함.
        val yearMonth = YearMonth.of(2026, 3)
        assertEquals(DayOfWeek.SUNDAY, yearMonth.atDay(1).dayOfWeek)

        val cells = calendarCellsFor(yearMonth)

        assertEquals(yearMonth.atDay(1), cells[0])
    }

    @Test
    fun allDaysOfMonth_arePresentExactlyOnceInOrder() {
        val yearMonth = YearMonth.of(2026, 2)
        val cells = calendarCellsFor(yearMonth)

        val nonNullDates = cells.filterNotNull()

        assertEquals(yearMonth.lengthOfMonth(), nonNullDates.size)
        assertEquals(
            (1..yearMonth.lengthOfMonth()).map { yearMonth.atDay(it) },
            nonNullDates
        )
    }

    @Test
    fun trailingCells_afterLastDayOfMonth_areBlank() {
        val yearMonth = YearMonth.of(2026, 9)
        val cells = calendarCellsFor(yearMonth)

        val lastDayIndex = cells.indexOf(yearMonth.atDay(yearMonth.lengthOfMonth()))
        val trailing = cells.subList(lastDayIndex + 1, cells.size)

        assertTrue(
            "마지막 날짜 뒤는 전부 빈 칸이어야 함",
            trailing.all { it == null }
        )
    }
}
