package com.postcardmemory.ui.gallery

import com.postcardmemory.utils.VisitHistoryStorage
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 78일차 P1-4: 앱을 켠 달의 방문 기록만 들고 있어서, 달력에서 다른 달로
 * 이동하면 디스크에 남아 있는 방문이 빈 칸으로 보이던 문제.
 *
 * 화면 조립은 Compose 테스트 인프라가 없어 직접 돌릴 수 없지만, "어느 달에
 * 어떤 집합을 쓰는가"라는 판정은 [visitedDaysForMonth] 하나에 모여 있고
 * 실제 디스크 읽기는 [VisitHistoryStorage.loadMonth]가 담당한다. 둘 다
 * production 함수 그대로 호출한다.
 */
class VisitCalendarMonthLoadingTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val september = YearMonth.of(2026, 9)
    private val august = YearMonth.of(2026, 8)

    private fun epochDay(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).toEpochDay()

    // ── 어느 집합을 쓸지 고르는 규칙 ──

    @Test
    fun currentMonth_usesTheSetLoadedAtAppStart() {
        val startupDays = setOf(epochDay(2026, 9, 3), epochDay(2026, 9, 19))

        assertEquals(
            startupDays,
            visitedDaysForMonth(
                month = september,
                currentMonth = september,
                currentMonthVisitedDays = startupDays,
                loadedByMonth = emptyMap()
            )
        )
    }

    @Test
    fun otherMonth_usesTheSetLoadedForThatMonth_notTheStartupSet() {
        val startupDays = setOf(epochDay(2026, 9, 3))
        val augustDays = setOf(epochDay(2026, 8, 11), epochDay(2026, 8, 12))

        assertEquals(
            augustDays,
            visitedDaysForMonth(
                month = august,
                currentMonth = september,
                currentMonthVisitedDays = startupDays,
                loadedByMonth = mapOf(august to augustDays)
            )
        )
    }

    @Test
    fun otherMonthNotLoadedYet_showsNothingRatherThanAnotherMonthsMarkers() {
        val startupDays = setOf(epochDay(2026, 9, 3))

        assertEquals(
            emptySet<Long>(),
            visitedDaysForMonth(
                month = august,
                currentMonth = september,
                currentMonthVisitedDays = startupDays,
                loadedByMonth = emptyMap()
            )
        )
    }

    @Test
    fun goingBackToTheCurrentMonth_restoresTheStartupSet() {
        val startupDays = setOf(epochDay(2026, 9, 3))
        val augustDays = setOf(epochDay(2026, 8, 11))
        val loaded = mapOf(august to augustDays)

        assertEquals(augustDays, visitedDaysForMonth(august, september, startupDays, loaded))
        assertEquals(startupDays, visitedDaysForMonth(september, september, startupDays, loaded))
    }

    // ── 실제 marker 파일을 읽어 붙였을 때의 결과 ──

    @Test
    fun browsingAcrossMonths_showsEachMonthsOwnMarkersFromDisk() {
        val filesDir = tempFolder.newFolder("files")
        assertTrue(VisitHistoryStorage.recordDate(filesDir, LocalDate.of(2026, 8, 11)))
        assertTrue(VisitHistoryStorage.recordDate(filesDir, LocalDate.of(2026, 8, 12)))
        assertTrue(VisitHistoryStorage.recordDate(filesDir, LocalDate.of(2026, 9, 3)))

        val septemberFromDisk = VisitHistoryStorage.loadMonth(filesDir, september)
        val augustFromDisk = VisitHistoryStorage.loadMonth(filesDir, august)

        // 9월을 보고 있을 때: 9월 것만.
        assertEquals(
            setOf(epochDay(2026, 9, 3)),
            visitedDaysForMonth(september, september, septemberFromDisk, emptyMap())
        )

        // 8월로 이동: 디스크에 있던 8월 방문이 실제로 보인다(수정 전에는 빈 달).
        assertEquals(
            setOf(epochDay(2026, 8, 11), epochDay(2026, 8, 12)),
            visitedDaysForMonth(
                month = august,
                currentMonth = september,
                currentMonthVisitedDays = septemberFromDisk,
                loadedByMonth = mapOf(august to augustFromDisk)
            )
        )

        // 다시 9월: 8월 값이 새어들지 않는다.
        assertEquals(
            setOf(epochDay(2026, 9, 3)),
            visitedDaysForMonth(
                month = september,
                currentMonth = september,
                currentMonthVisitedDays = septemberFromDisk,
                loadedByMonth = mapOf(august to augustFromDisk)
            )
        )
    }

    @Test
    fun monthWithNoRecordsStaysEmptyEvenWhenOtherMonthsHaveMarkers() {
        val filesDir = tempFolder.newFolder("files")
        assertTrue(VisitHistoryStorage.recordDate(filesDir, LocalDate.of(2026, 9, 3)))

        val july = YearMonth.of(2026, 7)
        val julyFromDisk = VisitHistoryStorage.loadMonth(filesDir, july)

        assertEquals(emptySet<Long>(), julyFromDisk)
        assertEquals(
            emptySet<Long>(),
            visitedDaysForMonth(july, september, setOf(epochDay(2026, 9, 3)), mapOf(july to julyFromDisk))
        )
    }

    // ── 오늘 표시와 방문 표시가 섞이지 않는다 ──

    @Test
    fun pastMonthGridNeverContainsToday_soTheTodayHighlightCannotLeakIntoIt() {
        val today = LocalDate.of(2026, 9, 19)

        assertFalse(visitCalendarPaddedCells(august).contains(today))
        assertTrue(visitCalendarPaddedCells(september).contains(today))
    }

    @Test
    fun aVisitMarkerInAPastMonthIsNotTheTodayMarker() {
        val today = LocalDate.of(2026, 9, 19)
        val augustDays = setOf(epochDay(2026, 8, 11))

        val augustCells = visitCalendarPaddedCells(august).filterNotNull()
        val visitedCells = augustCells.filter { it.toEpochDay() in augustDays }

        assertEquals(listOf(LocalDate.of(2026, 8, 11)), visitedCells)
        assertTrue(visitedCells.none { it == today })
    }
}
