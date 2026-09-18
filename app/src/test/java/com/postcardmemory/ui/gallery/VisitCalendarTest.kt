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

    // 75일차 추가 수정지시서: 계층형 월/연도 탐색기 (달력 → 월 선택 → 연도 선택).

    @Test fun decadeStartForComputesTheTenYearBucketAcrossBoundaries() {
        assertEquals(1990, decadeStartFor(1999))
        assertEquals(2000, decadeStartFor(2000))
        assertEquals(2000, decadeStartFor(2009))
        assertEquals(2010, decadeStartFor(2010))
        assertEquals(2020, decadeStartFor(2026))
        assertEquals(2020, decadeStartFor(2029))
        assertEquals(2030, decadeStartFor(2030))
    }

    @Test fun visitCalendarNavLevelOnBackStepsDownOneLevelAtATimeAndStaysOnCalendar() {
        assertEquals(VisitCalendarNavLevel.MONTH_PICKER, visitCalendarNavLevelOnBack(VisitCalendarNavLevel.YEAR_PICKER))
        assertEquals(VisitCalendarNavLevel.CALENDAR, visitCalendarNavLevelOnBack(VisitCalendarNavLevel.MONTH_PICKER))
        // CALENDAR에서는 이 함수가 더 내려갈 단계가 없다고만 알려준다 — drawer를 닫는 건 호출부(BackHandler) 책임.
        assertEquals(VisitCalendarNavLevel.CALENDAR, visitCalendarNavLevelOnBack(VisitCalendarNavLevel.CALENDAR))
    }

    @Test fun highlightedYearForOnlyMarksAYearWhenItFallsInsideTheShownDecade() {
        val displayedMonth = YearMonth.of(2026, 9)
        assertEquals(2026, highlightedYearFor(decadeStart = 2020, displayedMonth = displayedMonth))
        assertNull(highlightedYearFor(decadeStart = 2030, displayedMonth = displayedMonth))
        assertNull(highlightedYearFor(decadeStart = 2010, displayedMonth = displayedMonth))
        // decade 경계값도 포함 관계를 정확히 판단해야 한다.
        assertEquals(2029, highlightedYearFor(decadeStart = 2020, displayedMonth = YearMonth.of(2029, 12)))
        assertNull(highlightedYearFor(decadeStart = 2020, displayedMonth = YearMonth.of(2030, 1)))
    }

    // 실기기 QA 후속 지시: 월 grid 4×4(다음 해 4칸), 연도 grid 4×4(앞 2년+뒤 4년), 달력 6주 고정.

    @Test fun isYearWithinDecadeMatchesTheSameBoundaryAsHighlightedYearFor() {
        assertTrue(isYearWithinDecade(2020, decadeStart = 2020))
        assertTrue(isYearWithinDecade(2029, decadeStart = 2020))
        assertFalse(isYearWithinDecade(2019, decadeStart = 2020))
        assertFalse(isYearWithinDecade(2030, decadeStart = 2020))
    }

    @Test fun yearPickerGridYearsSpansTwoYearsBeforeAndFourYearsAfterTheDecade() {
        val years = yearPickerGridYears(decadeStart = 2020)
        assertEquals(16, years.size)
        assertEquals((2018..2033).toList(), years)
        assertEquals((2020..2029).toList(), years.filter { isYearWithinDecade(it, 2020) })
    }

    @Test fun monthPickerGridCellsHasTwelveMonthsOfPickerYearThenFourMonthsOfTheNextYear() {
        val cells = monthPickerGridCells(pickerYear = 2026)
        assertEquals(16, cells.size)
        assertEquals((1..12).map { 2026 to it }, cells.take(12))
        assertEquals((1..4).map { 2027 to it }, cells.drop(12))
    }

    // 77일차: MONTH_PICKER/YEAR_PICKER "지금 여기" 현재 위치 marker (선택 상태와 독립).

    @Test fun isCurrentMonthCellMatchesOnlyTheExactYearAndMonthOfToday() {
        val today = YearMonth.of(2026, 9)
        assertTrue(isCurrentMonthCell(2026, 9, today))
        assertFalse(isCurrentMonthCell(2026, 8, today))
        assertFalse(isCurrentMonthCell(2025, 9, today))
        assertFalse(isCurrentMonthCell(2027, 1, today))
    }

    @Test fun isCurrentYearCellMatchesOnlyTheExactYearOfToday() {
        val today = YearMonth.of(2026, 9)
        assertTrue(isCurrentYearCell(2026, today))
        assertFalse(isCurrentYearCell(2025, today))
        assertFalse(isCurrentYearCell(2027, today))
    }

    @Test fun currentMarkerNeverStacksWithSelectionHighlightOnTheSameCell() {
        assertTrue(visitCalendarShowsCurrentMarker(isCurrentPeriod = true, isSelected = false))
        assertFalse(visitCalendarShowsCurrentMarker(isCurrentPeriod = true, isSelected = true))
        assertFalse(visitCalendarShowsCurrentMarker(isCurrentPeriod = false, isSelected = false))
        assertFalse(visitCalendarShowsCurrentMarker(isCurrentPeriod = false, isSelected = true))
    }

    // 77일차: MONTH_PICKER/YEAR_PICKER 공용 수직 swipe 방향 판정 — ▲/▼ 버튼과 같은 결과를 내야 한다.

    @Test fun visitCalendarSwipeStepForTreatsUpwardSwipeAsNextAndDownwardAsPrevious() {
        val threshold = 24f
        // 위로 밀기(누적 drag가 음수) -> 다음 범위 -> onStepUp과 같은 방향(NEXT).
        assertEquals(VisitCalendarSwipeStep.NEXT, visitCalendarSwipeStepFor(-40f, threshold))
        assertEquals(VisitCalendarSwipeStep.NEXT, visitCalendarSwipeStepFor(-threshold, threshold))
        // 아래로 당기기(누적 drag가 양수) -> 이전 범위 -> onStepDown과 같은 방향(PREVIOUS).
        assertEquals(VisitCalendarSwipeStep.PREVIOUS, visitCalendarSwipeStepFor(40f, threshold))
        assertEquals(VisitCalendarSwipeStep.PREVIOUS, visitCalendarSwipeStepFor(threshold, threshold))
        // threshold 미만의 아주 작은 움직임은 아직 swipe로 인정하지 않는다.
        assertEquals(VisitCalendarSwipeStep.NONE, visitCalendarSwipeStepFor(10f, threshold))
        assertEquals(VisitCalendarSwipeStep.NONE, visitCalendarSwipeStepFor(-10f, threshold))
        assertEquals(VisitCalendarSwipeStep.NONE, visitCalendarSwipeStepFor(0f, threshold))
    }

    // 77일차 후속: 다른 월/연도를 탐색 중이라 오늘이 4×4 창 밖에 있을 때 헤더에 복귀 링크를 띄우는 판정.

    @Test fun isCurrentMonthVisibleInMonthPickerMatchesTheActualGridWindow() {
        val today = YearMonth.of(2026, 9)
        // pickerYear가 오늘의 해와 같으면(1~12월 전부 보임) 항상 보임.
        assertTrue(isCurrentMonthVisibleInMonthPicker(pickerYear = 2026, today = today))
        // 전년도를 보고 있으면 grid는 pickerYear의 1~12월 + 다음 해 1~4월만 보여준다 ->
        // pickerYear=2025면 2026년 1~4월만 보이고 9월은 안 보임.
        assertFalse(isCurrentMonthVisibleInMonthPicker(pickerYear = 2025, today = today))
        val earlyMonthToday = YearMonth.of(2026, 3)
        assertTrue(isCurrentMonthVisibleInMonthPicker(pickerYear = 2025, today = earlyMonthToday))
        assertFalse(isCurrentMonthVisibleInMonthPicker(pickerYear = 2024, today = earlyMonthToday))
        // 완전히 먼 연도는 당연히 안 보임.
        assertFalse(isCurrentMonthVisibleInMonthPicker(pickerYear = 2030, today = today))
    }

    @Test fun isCurrentYearVisibleInYearPickerMatchesTheActualGridWindow() {
        val today = YearMonth.of(2026, 9)
        // yearPickerGridYears(2020) = 2018~2033 -> 2026 포함.
        assertTrue(isCurrentYearVisibleInYearPicker(decadeStart = 2020, today = today))
        // 경계값: yearPickerGridYears(2013) = 2011~2026 -> 2026이 마지막 칸으로 포함.
        assertTrue(isCurrentYearVisibleInYearPicker(decadeStart = 2013, today = today))
        // 다음 decade(2030)의 창은 2028~2043이라 2026이 빠진다.
        assertFalse(isCurrentYearVisibleInYearPicker(decadeStart = 2030, today = today))
        // 완전히 먼 decade는 당연히 안 보임.
        assertFalse(isCurrentYearVisibleInYearPicker(decadeStart = 1990, today = today))
        assertFalse(isCurrentYearVisibleInYearPicker(decadeStart = 2050, today = today))
    }

    // 77일차 추가: 연한 원(marker)="정확한 위치", 색상="현재 연도에 속한 시간대" — 서로 다른
    // 의미의 색상 우선순위(선택 > 실제 현재 월 > 현재 연도 소속 월 > 다음 해 > 일반).

    @Test fun monthCellEmphasisPrioritizesSelectedOverEverythingElse() {
        assertEquals(
            VisitCalendarMonthCellEmphasis.SELECTED,
            visitCalendarMonthCellEmphasisFor(
                isSelected = true,
                isCurrentMonth = true,
                isAdjacentYearCell = true,
                isCurrentYearBeingViewed = true
            )
        )
    }

    @Test fun monthCellEmphasisPrioritizesActualCurrentMonthOverAdjacentYearDimming() {
        // 다음 해 버퍼 칸(pickerYear+1의 1~4월)에 우연히 오늘이 걸리는 경우에도
        // "실제 현재 월"이 "다음 해라 연하게" 규칙보다 우선해야 한다.
        assertEquals(
            VisitCalendarMonthCellEmphasis.CURRENT_MONTH,
            visitCalendarMonthCellEmphasisFor(
                isSelected = false,
                isCurrentMonth = true,
                isAdjacentYearCell = true,
                isCurrentYearBeingViewed = false
            )
        )
    }

    @Test fun monthCellEmphasisFallsBackThroughAdjacentYearThenCurrentYearThenNormal() {
        assertEquals(
            VisitCalendarMonthCellEmphasis.ADJACENT_YEAR,
            visitCalendarMonthCellEmphasisFor(false, false, isAdjacentYearCell = true, isCurrentYearBeingViewed = true)
        )
        assertEquals(
            VisitCalendarMonthCellEmphasis.CURRENT_YEAR,
            visitCalendarMonthCellEmphasisFor(false, false, isAdjacentYearCell = false, isCurrentYearBeingViewed = true)
        )
        assertEquals(
            VisitCalendarMonthCellEmphasis.NORMAL,
            visitCalendarMonthCellEmphasisFor(false, false, isAdjacentYearCell = false, isCurrentYearBeingViewed = false)
        )
    }

    @Test fun yearCellEmphasisPrioritizesSelectedThenCurrentYearThenDecadeMembership() {
        assertEquals(
            VisitCalendarYearCellEmphasis.SELECTED,
            visitCalendarYearCellEmphasisFor(isSelected = true, isCurrentYear = true, isInDecade = true)
        )
        assertEquals(
            VisitCalendarYearCellEmphasis.CURRENT_YEAR,
            visitCalendarYearCellEmphasisFor(isSelected = false, isCurrentYear = true, isInDecade = false)
        )
        assertEquals(
            VisitCalendarYearCellEmphasis.IN_DECADE,
            visitCalendarYearCellEmphasisFor(isSelected = false, isCurrentYear = false, isInDecade = true)
        )
        assertEquals(
            VisitCalendarYearCellEmphasis.OUT_OF_DECADE,
            visitCalendarYearCellEmphasisFor(isSelected = false, isCurrentYear = false, isInDecade = false)
        )
    }

    @Test fun visitCalendarPaddedCellsAlwaysFillsSixFullWeeksWithoutInventingRealDates() {
        listOf(
            YearMonth.of(2026, 9), // 평범한 5주
            YearMonth.of(2026, 2), // 짧은 2월, 4주
            YearMonth.of(2027, 1), // 31일, 6주가 필요할 수 있는 경우 포함해 연도 경계 확인
            YearMonth.of(2028, 2) // 윤년 2월
        ).forEach { month ->
            val real = calendarCellsFor(month)
            val padded = visitCalendarPaddedCells(month)
            assertEquals(42, padded.size)
            assertEquals(real, padded.take(real.size))
            // 채운 칸은 전부 빈 칸이어야 한다 — 실제 날짜를 만들어내지 않는다.
            padded.drop(real.size).forEach { assertNull(it) }
        }
    }
}
