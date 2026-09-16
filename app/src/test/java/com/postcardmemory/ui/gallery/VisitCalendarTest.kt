package com.postcardmemory.ui.gallery

import androidx.compose.ui.graphics.Color
import com.postcardmemory.ui.detail.LABEL_STICKER_DARK_TEXT_ARGB
import com.postcardmemory.ui.detail.LABEL_STICKER_LIGHT_TEXT_ARGB
import com.postcardmemory.ui.detail.labelStickerTextColorArgbFor
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.SealInkNavy
import com.postcardmemory.ui.theme.SealInkRed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class VisitCalendarTest {
    @Test fun visitDayKaomojiIsStableAndWithinCandidateSet() {
        val dates = (0L..400L).map { LocalDate.of(2026, 9, 14).plusDays(it) } +
            listOf(LocalDate.of(1960, 1, 1), LocalDate.MIN, LocalDate.MAX)
        val first = dates.associateWith(::visitDayKaomoji)
        dates.reversed().forEach { date -> assertEquals(first[date], visitDayKaomoji(date)) }
        dates.forEach { date -> assertTrue(visitDayKaomoji(date) in VISIT_DAY_KAOMOJI) }
    }

    @Test fun visitDayKaomojiHasExactlyFourCandidatesAndNoLongerIncludesTheOddOneOut() {
        // "^_^"는 다른 후보와 선 느낌·폭이 달라 박스 안에서 혼자 따로 노는 인상이라 제거됐다(4차 QA 피드백).
        assertEquals(4, VISIT_DAY_KAOMOJI.size)
        assertFalse("^_^" in VISIT_DAY_KAOMOJI)
        val dates = (0L..40L).map { LocalDate.of(2026, 9, 14).plusDays(it) }
        assertEquals(VISIT_DAY_KAOMOJI.toSet(), dates.map(::visitDayKaomoji).toSet())
    }

    @Test fun visitDayKaomojiCandidatesStayShortEnoughForANarrowDateCell() {
        VISIT_DAY_KAOMOJI.forEach { candidate ->
            assertTrue("\"$candidate\" is ${candidate.length} chars", candidate.length <= 4)
            assertFalse(candidate.contains("\n"))
        }
    }

    @Test fun visitCountLabelReflectsTotalVisitDaysWithoutStreakOrRewardWording() {
        assertEquals("오늘까지 73번 만났어요~!", visitCountLabel(73))
        assertEquals("오늘까지 1번 만났어요~!", visitCountLabel(1))
        assertEquals("", visitCountLabel(null))
        val forbidden = listOf("연속", "출석", "성공", "streak", "reward", "achievement", "축하")
        forbidden.forEach { word -> assertFalse(visitCountLabel(73).contains(word)) }
    }

    @Test fun visitDateColorAppliesMutedWeekendColorsAndKeepsWeekdaysDefault() {
        // 2026-09-14는 월요일이다.
        val monday = LocalDate.of(2026, 9, 14)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(InkSecondary, visitDateColor(monday))
        assertEquals(InkSecondary, visitDateColor(monday.plusDays(3))) // 목요일

        val saturday = monday.plusDays(5)
        assertEquals(DayOfWeek.SATURDAY, saturday.dayOfWeek)
        assertEquals(SealInkNavy, visitDateColor(saturday))

        val sunday = monday.plusDays(6)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(SealInkRed, visitDateColor(sunday))
    }

    @Test fun visitFillColorMatchesTheExactColorFromTheInstructionsAndIsFullyOpaque() {
        assertEquals(Color(0xFF16A7A1), VisitFillColor)
        assertEquals(1f, VisitFillColor.alpha)
    }

    @Test fun visitFillContrastColorReusesLabelStickerContrastLogicInsteadOfANewOne() {
        // 텍스트 스티커의 밝은/어두운 배경 판정을 그대로 재사용했는지 직접 재계산해 확인한다.
        val expectedArgb = labelStickerTextColorArgbFor(0xFF16A7A1L)
        assertEquals(Color(expectedArgb), VisitFillContrastColor)
        // #16A7A1은 두 후보(밝은 텍스트/어두운 텍스트) 중 하나와 정확히 일치해야 한다.
        assertTrue(
            VisitFillContrastColor == Color(LABEL_STICKER_LIGHT_TEXT_ARGB) ||
                VisitFillContrastColor == Color(LABEL_STICKER_DARK_TEXT_ARGB)
        )
    }

    @Test fun sharedCalendarHandlesLeapYearsMonthLengthsAndYearBoundary() {
        listOf(YearMonth.of(2027, 2), YearMonth.of(2028, 2), YearMonth.of(2026, 4),
            YearMonth.of(2026, 12), YearMonth.of(2027, 1)).forEach { month ->
            val cells = calendarCellsFor(month)
            assertEquals(0, cells.size % 7)
            assertEquals((1..month.lengthOfMonth()).map(month::atDay), cells.filterNotNull())
            assertEquals(month.atDay(1).dayOfWeek.value % 7, cells.indexOf(month.atDay(1)))
        }
    }

    // 75일차: 월 이동 자체는 java.time의 YearMonth.minusMonths/plusMonths에 맡기고(자체 계산 없음)
    // 연도 경계 포함 정확성은 이미 sharedCalendarHandlesLeapYearsMonthLengthsAndYearBoundary가
    // calendarCellsFor로 확인한다. 여기서는 오늘 방문 색 결정 로직만 따로 검증한다.

    @Test fun visitDayFillColorIsDarkerOnlyForTodayAndKeepsTheOriginalTealOtherwise() {
        val today = LocalDate.of(2026, 9, 16)
        assertEquals(VisitFillColorToday, visitDayFillColor(today, today))
        assertEquals(VisitFillColor, visitDayFillColor(today.minusDays(1), today))
        assertEquals(VisitFillColor, visitDayFillColor(today.plusDays(1), today))
        assertNotEquals(VisitFillColor, VisitFillColorToday)
    }

    @Test fun visitDayFillContrastColorFollowsTheSameAutomaticContrastLogicAsTheOriginal() {
        val today = LocalDate.of(2026, 9, 16)
        assertEquals(VisitFillContrastColorToday, visitDayFillContrastColor(today, today))
        assertEquals(VisitFillContrastColor, visitDayFillContrastColor(today.minusDays(1), today))
        // 새 대비 로직을 만들지 않았다면 오늘 대비색도 기존 두 후보(밝은/어두운 텍스트) 중 하나와 일치해야 한다.
        assertTrue(
            VisitFillContrastColorToday == Color(LABEL_STICKER_LIGHT_TEXT_ARGB) ||
                VisitFillContrastColorToday == Color(LABEL_STICKER_DARK_TEXT_ARGB)
        )
    }

    @Test fun todayVisitColorAppliesOnlyWhenTodayItselfHasAnActualVisitHistoryEntry() {
        val today = LocalDate.of(2026, 9, 16)
        val visitedEpochDays = setOf(
            LocalDate.of(2026, 9, 10).toEpochDay(),
            LocalDate.of(2026, 9, 14).toEpochDay(),
            today.toEpochDay()
        )

        // 오늘 + 실제 방문 history 있음 -> 진한 청록이 적용될 조건을 만족한다.
        assertTrue(today.toEpochDay() in visitedEpochDays)
        assertEquals(VisitFillColorToday, visitDayFillColor(today, today))

        // 과거 방문 -> 기존 색 그대로.
        val pastVisited = LocalDate.of(2026, 9, 10)
        assertTrue(pastVisited.toEpochDay() in visitedEpochDays)
        assertEquals(VisitFillColor, visitDayFillColor(pastVisited, today))

        // 오늘 + history 없음 -> visited 자체가 false라 호출부가 라벨을 그리지 않는다(기존 gate 유지).
        val emptyHistory = emptySet<Long>()
        assertFalse(today.toEpochDay() in emptyHistory)

        // 미래 -> history에 없으니 라벨 없음.
        val future = today.plusDays(5)
        assertFalse(future.toEpochDay() in visitedEpochDays)
    }
}
