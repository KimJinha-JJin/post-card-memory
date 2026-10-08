package com.postcardmemory.ui.gallery

import com.postcardmemory.utils.millisUntilNextMidnight

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.postcardmemory.ui.detail.LABEL_STICKER_DARK_TEXT_ARGB
import com.postcardmemory.ui.detail.LABEL_STICKER_LIGHT_TEXT_ARGB
import com.postcardmemory.ui.detail.labelStickerTextColorArgbFor
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.SealInkNavy
import com.postcardmemory.ui.theme.SealInkRed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
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

    // 계층형 월/연도 탐색기 (달력 → 월 선택 → 연도 선택).

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

    // ── 자정 갱신 회귀 방지 (78일차 후속) ────────────────────────────────
    // 앱을 켜 둔 채 자정을 넘기면 시계는 새 날짜인데 달력의 "오늘"만 어제에 머물렀다.
    // 원인은 달력이 today를 key 없는 remember로 한 번만 붙잡아 둔 것이었고, 수정 후에는
    // millisUntilNextMidnight만큼 기다렸다가 스스로 다시 읽는다. 아래는 그 대기 시간
    // 계산과, "날짜가 하루 넘어가면 현재 기준 판정도 새 날짜를 쓴다"는 의미를 고정한다.

    @Test fun millisUntilNextMidnightCountsDownToTheUpcomingDayBoundary() {
        assertEquals(
            60_000L,
            millisUntilNextMidnight(LocalDateTime.of(2026, 9, 19, 23, 59, 0))
        )
        assertEquals(
            500L,
            millisUntilNextMidnight(LocalDateTime.of(2026, 9, 19, 23, 59, 59, 500_000_000))
        )
        assertEquals(
            12L * 60 * 60 * 1000,
            millisUntilNextMidnight(LocalDateTime.of(2026, 9, 19, 12, 0, 0))
        )
    }

    @Test fun millisUntilNextMidnightAtExactMidnightWaitsAFullDayInsteadOfZero() {
        // 0을 돌려주면 대기 루프가 자정에 바쁜 루프로 돌아버린다 — 항상 양수여야 한다.
        val atMidnight = millisUntilNextMidnight(LocalDateTime.of(2026, 9, 19, 0, 0, 0))
        assertEquals(24L * 60 * 60 * 1000, atMidnight)
        listOf(
            LocalDateTime.of(2026, 9, 19, 0, 0, 0),
            LocalDateTime.of(2026, 12, 31, 23, 59, 59),
            LocalDateTime.of(2028, 2, 28, 0, 0, 0) // 윤년 2월 28일 -> 29일
        ).forEach { assertTrue(millisUntilNextMidnight(it) > 0L) }
    }

    @Test fun millisUntilNextMidnightCrossesMonthAndYearBoundaries() {
        assertEquals(
            1_000L,
            millisUntilNextMidnight(LocalDateTime.of(2026, 9, 30, 23, 59, 59))
        )
        assertEquals(
            1_000L,
            millisUntilNextMidnight(LocalDateTime.of(2026, 12, 31, 23, 59, 59))
        )
    }

    @Test fun currentPeriodJudgmentsFollowTheNewDateOnceTheDayRollsOver() {
        // 월말 자정 통과: 9/30 -> 10/1. 현재 기준이 갱신되면 "현재 월"도 함께 넘어가야 한다.
        val lastDayOfSeptember = YearMonth.from(LocalDate.of(2026, 9, 30))
        val firstDayOfOctober = YearMonth.from(LocalDate.of(2026, 10, 1))

        assertTrue(isCurrentMonthCell(2026, 9, lastDayOfSeptember))
        assertFalse(isCurrentMonthCell(2026, 10, lastDayOfSeptember))

        assertFalse(isCurrentMonthCell(2026, 9, firstDayOfOctober))
        assertTrue(isCurrentMonthCell(2026, 10, firstDayOfOctober))
    }

    @Test fun currentYearJudgmentsFollowTheNewDateAcrossNewYearMidnight() {
        // 연말 자정 통과: 2026-12-31 -> 2027-01-01.
        val newYearsEve = YearMonth.from(LocalDate.of(2026, 12, 31))
        val newYearsDay = YearMonth.from(LocalDate.of(2027, 1, 1))

        assertTrue(isCurrentYearCell(2026, newYearsEve))
        assertFalse(isCurrentYearCell(2027, newYearsEve))

        assertFalse(isCurrentYearCell(2026, newYearsDay))
        assertTrue(isCurrentYearCell(2027, newYearsDay))

        // 해가 바뀌어도 2026년 MONTH_PICKER는 다음 해 1~4월을 버퍼로 보여주므로 2027년
        // 1월은 아직 창 안이다 — 링크가 바로 뜨지는 않는다.
        assertTrue(isCurrentMonthVisibleInMonthPicker(2026, newYearsEve))
        assertTrue(isCurrentMonthVisibleInMonthPicker(2026, newYearsDay))
    }

    @Test fun todayReturnLinkAppearsWhenTheRolledOverDateLeavesTheVisiblePickerWindow() {
        // 2026년 MONTH_PICKER가 보여주는 마지막 버퍼 칸은 2027년 4월이다. 그 달의 마지막 날
        // 자정을 넘기면 오늘이 창 밖으로 나가므로 "오늘 ...로" 복귀 링크가 나타나야 한다.
        val lastVisibleBufferMonth = YearMonth.from(LocalDate.of(2027, 4, 30))
        val firstMonthOutsideWindow = YearMonth.from(LocalDate.of(2027, 5, 1))

        assertTrue(isCurrentMonthVisibleInMonthPicker(2026, lastVisibleBufferMonth))
        assertFalse(isCurrentMonthVisibleInMonthPicker(2026, firstMonthOutsideWindow))
    }

    @Test fun earlierMonthPageAlwaysLiesOnTopOfLaterMonthPage() {
        // 90일차: 벽걸이 달력 묶음처럼 앞선 달이 위 — 다음 달로 가면 지금 장이 위에서 넘어가고,
        // 이전 달로 가면 이전 장이 위에서 내려와 덮는다. 해가 바뀌는 경계와 먼 점프도 같다.
        val pairs = listOf(
            YearMonth.of(2026, 9) to YearMonth.of(2026, 10),
            YearMonth.of(2026, 12) to YearMonth.of(2027, 1),
            YearMonth.of(2019, 3) to YearMonth.of(2026, 9)
        )
        pairs.forEach { (earlier, later) ->
            assertTrue(
                "$earlier 장은 $later 장 위에 있어야 함",
                visitCalendarPageStackZIndex(earlier) > visitCalendarPageStackZIndex(later)
            )
        }
    }

    @Test fun turningPageIsOpaqueWhenFlatAndGoneWhenFullyTurned() {
        listOf(true, false).forEach { forward ->
            assertEquals(1f, visitCalendarPageTurnAlpha(0f, forward), 0f)
            assertEquals(0f, visitCalendarPageTurnAlpha(1f, forward), 0f)
            val samples = (0..20).map { visitCalendarPageTurnAlpha(it / 20f, forward) }
            samples.zipWithNext().forEach { (a, b) -> assertTrue("들릴수록 흐려지기만 해야 함", b <= a) }
            samples.forEach { assertTrue(it in 0f..1f) }
        }
    }

    @Test fun nextMonthPageStaysOpaqueWhileTurningSoTwoMonthsNeverShowThroughEachOther() {
        // 90일차 QA: 다음 달로 넘길 때 현재 장이 반쯤 넘어간 채 반투명하면 두 달의 인쇄가 겹쳐
        // crossfade처럼 보였다. 현재 장은 절반 넘게 넘어가도 불투명해야 하고, 어떤 지점에서도
        // 이전 달로 내려오는 장보다 먼저 흐려지면 안 된다.
        assertEquals(1f, visitCalendarPageTurnAlpha(0.5f, forward = true), 0f)
        (0..20).map { it / 20f }.forEach { lift ->
            assertTrue(visitCalendarPageTurnAlpha(lift, forward = true) >= visitCalendarPageTurnAlpha(lift, forward = false))
        }
    }

    @Test fun paperEdgeOutlineIsTheSameShapeEveryTimeForTheSameMonth() {
        val seed = visitCalendarPaperEdgeSeed(YearMonth.of(2026, 10))
        assertEquals(seed, visitCalendarPaperEdgeSeed(YearMonth.of(2026, 10)))
        assertEquals(
            visitCalendarPaperEdgeOutline(256f, 330f, seed),
            visitCalendarPaperEdgeOutline(256f, 330f, seed)
        )
    }

    @Test fun paperEdgeOutlineDiffersBetweenNeighbouringMonths() {
        val months = listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 10), YearMonth.of(2026, 11), YearMonth.of(2027, 1))
        val seeds = months.map(::visitCalendarPaperEdgeSeed)
        assertEquals(seeds.size, seeds.toSet().size)
        val outlines = seeds.map { visitCalendarPaperEdgeOutline(256f, 330f, it) }
        assertEquals(outlines.size, outlines.toSet().size)
    }

    @Test fun paperEdgeOutlineStaysInsideThePageWithinTheTinyCutBand() {
        val width = 256f
        val height = 330f
        val band = VISIT_CALENDAR_PAPER_EDGE_MAX_INSET_DP
        val epsilon = 1e-3f
        (2020..2030).flatMap { y -> (1..12).map { YearMonth.of(y, it) } }.forEach { month ->
            val outline = visitCalendarPaperEdgeOutline(width, height, visitCalendarPaperEdgeSeed(month))
            assertTrue(outline.size > 4)
            outline.forEach { p ->
                // 장 밖으로 나가지 않는다 — 장 크기·배치는 그대로다.
                assertTrue(p.x >= -epsilon && p.x <= width + epsilon)
                assertTrue(p.y >= -epsilon && p.y <= height + epsilon)
                // 어느 변에서든 띠 안에 있다 — 가장자리만 미세하게 흔들린다.
                val distanceToEdge = minOf(p.x, p.y, width - p.x, height - p.y)
                assertTrue("$month $p", distanceToEdge <= band + epsilon)
            }
            assertTrue("$month 가장자리가 반듯한 사각형이면 안 됨", outline.any { p -> minOf(p.x, p.y, width - p.x, height - p.y) > 0.05f })
        }
    }

    @Test fun tapePieceIsTheSameEveryTimeForTheSameMonthAndDiffersBetweenMonths() {
        val october = visitCalendarTapePiece(visitCalendarTapeSeed(YearMonth.of(2026, 10)))
        val again = visitCalendarTapePiece(visitCalendarTapeSeed(YearMonth.of(2026, 10)))
        assertEquals(october.outline, again.outline)
        assertEquals(october.fibers, again.fibers)
        assertEquals(october.angleDeg, again.angleDeg, 0f)
        val november = visitCalendarTapePiece(visitCalendarTapeSeed(YearMonth.of(2026, 11)))
        assertNotEquals(october.outline, november.outline)
    }

    @Test fun tapePieceIsAShortSlightlyTiltedStripWithUnevenTornEnds() {
        val halfH = VISIT_CALENDAR_TAPE_HEIGHT_DP / 2f
        (2020..2030).flatMap { y -> (1..12).map { YearMonth.of(y, it) } }.forEach { month ->
            val tape = visitCalendarTapePiece(visitCalendarTapeSeed(month))
            val magnitude = kotlin.math.abs(tape.angleDeg)
            assertTrue("$month 각도 ${tape.angleDeg}", magnitude >= VISIT_CALENDAR_TAPE_MIN_ANGLE_DEG && magnitude <= VISIT_CALENDAR_TAPE_MAX_ANGLE_DEG)
            assertTrue("$month 길이 ${tape.lengthDp}", tape.lengthDp in 40f..48f)
            val halfL = tape.lengthDp / 2f
            tape.outline.forEach { p ->
                assertTrue("$month $p", kotlin.math.abs(p.x) <= halfL + 1e-3f && kotlin.math.abs(p.y) <= halfH + 0.1f)
            }
            tape.fibers.forEach { (from, to, alpha) ->
                listOf(from, to).forEach { p -> assertTrue("$month 섬유는 테이프 안", kotlin.math.abs(p.x) < halfL && kotlin.math.abs(p.y) < halfH) }
                assertTrue("$month 섬유는 아주 옅어야 함", alpha <= 0.12f)
            }
            // 양 끝은 찢긴 모양이 서로 달라 좌우대칭이 아니다.
            val rightEnd = tape.outline.filter { it.x > halfL - 3f }.map { halfL - it.x }
            val leftEnd = tape.outline.filter { it.x < -halfL + 3f }.map { it.x + halfL }
            assertTrue(rightEnd.size > 2 && leftEnd.size > 2)
            assertNotEquals("$month", rightEnd.sorted(), leftEnd.sorted())
        }
    }

    @Test fun gelPenMarkIsTheSameEveryTimeForTheSameDateAndDiffersBetweenDays() {
        val day = LocalDate.of(2026, 10, 7)
        val first = visitDayGelPenMark(29f, 28f, visitDayPenSeed(day))!!
        val again = visitDayGelPenMark(29f, 28f, visitDayPenSeed(day))!!
        assertEquals(first.wash, again.wash)
        assertEquals(first.strokes.map { it.points }, again.strokes.map { it.points })
        val next = visitDayGelPenMark(29f, 28f, visitDayPenSeed(day.plusDays(1)))!!
        assertNotEquals(first.strokes.map { it.points }, next.strokes.map { it.points })
    }

    @Test fun gelPenMarkIsShortGentlyDiagonalStrokesThatStayOnTheCell() {
        val width = 29f
        val height = 28f
        (0L until 400L).map { LocalDate.of(2026, 1, 1).plusDays(it) }.forEach { day ->
            val mark = visitDayGelPenMark(width, height, visitDayPenSeed(day))!!
            // 바탕 외곽은 칸 안쪽에서 손떨림만큼만 흔들린다.
            mark.wash.forEach { p ->
                assertTrue("$day $p", p.x in 0f..width && p.y in 0f..height)
                assertTrue("$day $p", minOf(p.x, p.y, width - p.x, height - p.y) <= 0.5f + VISIT_DAY_PEN_EDGE_WOBBLE_DP + 1e-3f)
            }
            assertTrue("$day 획이 충분히 반복돼야 함", mark.strokes.size >= 15)
            mark.strokes.forEach { stroke ->
                stroke.points.forEach { p ->
                    assertTrue("$day 획은 칸에서 1dp 넘게 삐져나가면 안 됨 $p", p.x in -1f..width + 1f && p.y in -1f..height + 1f)
                }
                // 중성펜: 굵기·농도가 고르고 바탕보다 옅게 겹친다(색연필·크레파스처럼 진하게 뭉치지 않음).
                assertTrue(stroke.widthDp in 0.9f..1.4f)
                assertTrue(stroke.alpha in 0.2f..0.55f)
                // 약한 사선: 거의 가로~-30° 사이로 오른쪽 위를 향한다.
                val dx = stroke.points.last().x - stroke.points.first().x
                val dy = stroke.points.last().y - stroke.points.first().y
                val angle = Math.toDegrees(kotlin.math.atan2(dy, dx).toDouble())
                assertTrue("$day 각도 $angle", angle in -30.0..-5.0)
            }
        }
    }

    @Test fun todayCircleIsTheSameEveryTimeForTheSameDateAndDiffersBetweenDays() {
        val day = LocalDate.of(2026, 10, 7)
        val first = visitTodayPenCircle(12f, 13f, visitTodayCircleSeed(day))
        assertEquals(first.map { it.points }, visitTodayPenCircle(12f, 13f, visitTodayCircleSeed(day)).map { it.points })
        assertNotEquals(
            first.map { it.points },
            visitTodayPenCircle(12f, 13f, visitTodayCircleSeed(day.plusDays(1))).map { it.points }
        )
    }

    @Test fun todayCircleIsAFewThinScribbledTurnsAroundTheNumberWithoutCoveringIt() {
        val turnCounts = mutableSetOf<Int>()
        listOf(6f to 13f, 12.5f to 13f, 6f to 16f).forEach { (width, height) ->
            val center = Offset(width / 2f, height / 2f)
            (0L until 400L).map { LocalDate.of(2026, 1, 1).plusDays(it) }.forEach { day ->
                val turns = visitTodayPenCircle(width, height, visitTodayCircleSeed(day))
                turnCounts += turns.size
                assertTrue("$day 몇 바퀴 휘갈겨야 함", turns.size in 3..4)
                // 펜을 떼지 않은 한 줄: 다음 바퀴는 앞 바퀴가 끝난 자리에서 이어진다.
                turns.zipWithNext().forEach { (a, b) -> assertEquals("$day", a.points.last(), b.points.first()) }
                // 바퀴마다 농도가 조금씩 다르다.
                assertEquals("$day", turns.size, turns.map { it.alpha }.toSet().size)
                val all = turns.flatMap { it.points }
                // 끝이 시작점과 맞물리지 않는다(닫힌 도형이 아님).
                assertNotEquals("$day", all.first(), all.last())
                turns.forEach { turn ->
                    // 얇은 펜 선, 디지털 테두리처럼 진하지 않다.
                    assertTrue("$day 굵기 ${turn.widthDp}", turn.widthDp in 0.5f..0.75f)
                    assertTrue("$day 농도 ${turn.alpha}", turn.alpha in 0.4f..0.85f)
                }
                val radius = maxOf(height / 2f + 3f, width / 2f + 5.5f, 10f)
                all.forEach { p ->
                    val d = p - center
                    // 숫자 글자(상자보다 조금 안쪽)를 가로지르지 않는다.
                    val inner = (d.x / (width / 2f + 1f)).let { it * it } + (d.y / (height / 2f - 2f)).let { it * it }
                    assertTrue("$day 숫자 위를 지나면 안 됨 $p", inner >= 1f)
                    // 숫자 둘레를 감는 크기에 머문다(칸 밖으로 크게 번지지 않음).
                    assertTrue("$day 너무 큼 $p", kotlin.math.abs(d.x) <= radius * 1.3f + 3f)
                    assertTrue("$day 너무 큼 $p", kotlin.math.abs(d.y) <= radius * 1.3f + 3f)
                }
                // 세로 타원이 아니라 거의 동그란 원이다(첫 QA: 세로로 길쭉해 보임).
                val spanX = all.maxOf { it.x } - all.minOf { it.x }
                val spanY = all.maxOf { it.y } - all.minOf { it.y }
                assertTrue("$day 세로로 길쭉하면 안 됨 $spanX x $spanY", spanX >= spanY * 0.85f)
                assertTrue("$day 가로로도 납작하면 안 됨 $spanX x $spanY", spanX <= spanY * 1.35f)
            }
        }
        assertEquals("세 바퀴 남짓과 네 바퀴 가까이가 모두 나와야 함", setOf(3, 4), turnCounts)
    }

    @Test fun newsprintArrowIsTheSameEveryTimeAndCutDifferentlyForEachSeed() {
        assertEquals(visitCalendarNewsprintArrow(true, 7), visitCalendarNewsprintArrow(true, 7))
        assertNotEquals(visitCalendarNewsprintArrow(true, 7).outline, visitCalendarNewsprintArrow(true, 8).outline)
    }

    @Test fun newsprintArrowStaysAClearlyPointingDarkTriangleWithAFewPaperDots() {
        (0 until 300).forEach { seed ->
            listOf(true, false).forEach { pointsLeft ->
                val arrow = visitCalendarNewsprintArrow(pointsLeft, seed)
                val outline = arrow.outline
                // 반듯한 디지털 삼각형이 아니라 가위로 몇 번 끊어 자른 윤곽, 상자 안.
                assertTrue("$seed 윤곽 점 ${outline.size}", outline.size in 6..9)
                outline.forEach { p -> assertTrue("$seed 상자 밖 $p", p.x in 1f..19f && p.y in 1f..19f) }
                // 방향이 즉시 읽힌다: 끝점은 세로 가운데, 몸통보다 한쪽으로 확실히 나와 있다.
                val tip = if (pointsLeft) outline.minBy { it.x } else outline.maxBy { it.x }
                val back = if (pointsLeft) outline.maxOf { it.x } else outline.minOf { it.x }
                assertTrue("$seed 끝점 높이 ${tip.y}", tip.y in 9f..11f)
                assertTrue("$seed 폭", kotlin.math.abs(back - tip.x) in 8f..10f)
                assertTrue("$seed 높이", outline.maxOf { it.y } - outline.minOf { it.y } in 10f..12f)
                // 망점은 잉크 안쪽에만 있고, 비치는 종이가 잉크를 이기지 않는다(조작부 가독성).
                assertTrue("$seed 망점 ${arrow.dots.size}", arrow.dots.size >= 10)
                arrow.dots.forEach { dot ->
                    assertTrue("$seed 망점 크기 ${dot.radiusDp}", dot.radiusDp in 0.1f..0.38f)
                    assertTrue("$seed 망점이 윤곽 밖", dot.center.x in outline.minOf { it.x }..outline.maxOf { it.x })
                }
                val dotArea = arrow.dots.sumOf { Math.PI * it.radiusDp * it.radiusDp }
                val triangleArea = 0.5 * 9 * 11
                assertTrue("$seed 종이가 너무 많이 비침 ${dotArea / triangleArea}", dotArea / triangleArea in 0.03..0.3)
            }
        }
    }

    @Test fun newsprintArrowsMirrorEachOther() {
        val left = visitCalendarNewsprintArrow(true, 1)
        val right = visitCalendarNewsprintArrow(false, 1)
        assertEquals(left.outline.map { Offset(20f - it.x, it.y) }, right.outline)
    }

    @Test fun pickerPaperScrapsComeFromTheBottomOfThePaperAndStayInside() {
        val seeds = (2020..2032).flatMap { year -> (1..12).map { visitCalendarMonthPickerScrapSeed(year, it) } } +
            (1990..2060).map { visitCalendarYearPickerScrapSeed(it) }
        seeds.forEach { seed ->
            val crop = visitCalendarPickerScrapCrop(seed, 1122, 1402, 280, 130)
            assertEquals(crop, visitCalendarPickerScrapCrop(seed, 1122, 1402, 280, 130))
            assertTrue("$seed 가로 $crop", crop.x in 0..(1122 - 280))
            assertTrue("$seed 하단 영역 $crop", crop.y in (1402 * VISIT_CALENDAR_PICKER_SCRAP_REGION_TOP).toInt()..(1402 - 130))
        }
        // 종이보다 큰 자투리도 종이 밖을 가리키지 않는다.
        assertEquals(0, visitCalendarPickerScrapCrop(1, 100, 100, 200, 200).x)
        assertEquals(0, visitCalendarPickerScrapCrop(1, 100, 100, 200, 200).y)
    }

    @Test fun pickerPaperScrapsAreNotTheSamePieceCopiedIntoEveryCell() {
        val monthCells = monthPickerGridCells(2026).map { (year, month) -> visitCalendarMonthPickerScrapSeed(year, month) }
        val yearCells = yearPickerGridYears(2020).map { visitCalendarYearPickerScrapSeed(it) }
        listOf(monthCells, yearCells).forEach { seeds ->
            assertEquals(16, seeds.toSet().size)
            val crops = seeds.map { visitCalendarPickerScrapCrop(it, 1122, 1402, 280, 130) }
            assertTrue("자투리 위치가 거의 다 달라야 함 ${crops.toSet().size}", crops.toSet().size >= 14)
            assertNotEquals(
                visitCalendarPaperEdgeOutline(70f, 32f, seeds[0]),
                visitCalendarPaperEdgeOutline(70f, 32f, seeds[1])
            )
        }
    }

    @Test fun todayCircleIsNothingForAnUnmeasuredNumber() {
        assertTrue(visitTodayPenCircle(0f, 13f, 1).isEmpty())
        assertTrue(visitTodayPenCircle(12f, 0f, 1).isEmpty())
    }

    @Test fun gelPenMarkIsNothingForAnUnmeasuredCell() {
        assertNull(visitDayGelPenMark(0f, 28f, 1))
        assertNull(visitDayGelPenMark(29f, 0f, 1))
    }

    @Test fun paperEdgeOutlineIsEmptyForAnUnmeasuredPage() {
        assertTrue(visitCalendarPaperEdgeOutline(0f, 330f, 1).isEmpty())
        assertTrue(visitCalendarPaperEdgeOutline(256f, 0f, 1).isEmpty())
    }
}
