package com.postcardmemory.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 70일차 방문 기록의 날짜 판정을 실제 날짜가 바뀌기를 기다리지 않고 검증한다.
 * recordVisit은 순수 함수이고 "오늘"을 epochDay로 주입하므로, 월말/연말/
 * 자정 경계/시계 역행을 모두 결정적으로 재현할 수 있다.
 */
class VisitRecordTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    private fun day(text: String): Long = LocalDate.parse(text).toEpochDay()

    private fun millisAt(
        text: String,
        zone: ZoneId = seoul
    ): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    // ---- 1. 최초 방문 ----

    @Test
    fun recordVisit_firstEverVisit_startsAtOneDay() {
        val result = recordVisit(previous = null, todayEpochDay = day("2026-09-11"))

        assertEquals(day("2026-09-11"), result.lastVisitEpochDay)
        assertEquals(1, result.totalVisitDays)
        assertEquals(1, result.currentStreakDays)
    }

    // ---- 2. 같은 날 재실행 ----

    @Test
    fun recordVisit_sameDayAgain_returnsPreviousUnchanged() {
        val previous = VisitRecord(day("2026-09-11"), totalVisitDays = 1, currentStreakDays = 1)

        val result = recordVisit(previous, day("2026-09-11"))

        // 같은 인스턴스를 그대로 돌려줘야 저장 쪽에서 파일 쓰기를 건너뛸 수 있다.
        assertSame(previous, result)
    }

    @Test
    fun recordVisit_sameDayTenTimes_neverIncreasesAnything() {
        var record = recordVisit(previous = null, todayEpochDay = day("2026-09-11"))

        repeat(10) { record = recordVisit(record, day("2026-09-11")) }

        assertEquals(1, record.totalVisitDays)
        assertEquals(1, record.currentStreakDays)
        assertEquals(day("2026-09-11"), record.lastVisitEpochDay)
    }

    // ---- 3. 바로 다음 날 ----

    @Test
    fun recordVisit_nextDay_increasesTotalAndStreak() {
        val previous = VisitRecord(day("2026-09-11"), totalVisitDays = 1, currentStreakDays = 1)

        val result = recordVisit(previous, day("2026-09-12"))

        assertEquals(day("2026-09-12"), result.lastVisitEpochDay)
        assertEquals(2, result.totalVisitDays)
        assertEquals(2, result.currentStreakDays)
    }

    @Test
    fun recordVisit_sevenConsecutiveDays_streakFollowsTotal() {
        var record: VisitRecord? = null
        val firstDay = day("2026-09-05")

        repeat(7) { offset -> record = recordVisit(record, firstDay + offset) }

        assertEquals(7, record!!.totalVisitDays)
        assertEquals(7, record!!.currentStreakDays)
    }

    // ---- 4. 하루 이상 건너뜀 ----

    @Test
    fun recordVisit_afterGap_keepsTotalButRestartsStreak() {
        val previous = VisitRecord(day("2026-09-11"), totalVisitDays = 1, currentStreakDays = 1)

        val result = recordVisit(previous, day("2026-09-15"))

        assertEquals(day("2026-09-15"), result.lastVisitEpochDay)
        assertEquals(2, result.totalVisitDays)
        assertEquals(1, result.currentStreakDays)
    }

    @Test
    fun recordVisit_longGap_totalIsPreservedNotReset() {
        val previous = VisitRecord(day("2026-05-01"), totalVisitDays = 43, currentStreakDays = 12)

        val result = recordVisit(previous, day("2026-09-11"))

        assertEquals(44, result.totalVisitDays)
        assertEquals(1, result.currentStreakDays)
    }

    // ---- 5. 월말 to 다음 달 ----

    @Test
    fun recordVisit_acrossMonthEnd_countsAsConsecutive() {
        val previous = VisitRecord(day("2026-09-30"), totalVisitDays = 5, currentStreakDays = 5)

        val result = recordVisit(previous, day("2026-10-01"))

        assertEquals(6, result.totalVisitDays)
        assertEquals(6, result.currentStreakDays)
    }

    @Test
    fun recordVisit_acrossFebruaryEndInLeapYear_countsAsConsecutive() {
        val previous = VisitRecord(day("2028-02-29"), totalVisitDays = 2, currentStreakDays = 2)

        val result = recordVisit(previous, day("2028-03-01"))

        assertEquals(3, result.totalVisitDays)
        assertEquals(3, result.currentStreakDays)
    }

    // ---- 6. 연말 to 다음 해 ----

    @Test
    fun recordVisit_acrossYearEnd_countsAsConsecutive() {
        val previous = VisitRecord(day("2026-12-31"), totalVisitDays = 100, currentStreakDays = 9)

        val result = recordVisit(previous, day("2027-01-01"))

        assertEquals(101, result.totalVisitDays)
        assertEquals(10, result.currentStreakDays)
    }

    // ---- 7. 기기 날짜가 과거로 이동한 경우 ----

    @Test
    fun recordVisit_deviceClockMovedBackward_movesMarkerButKeepsCounts() {
        val previous = VisitRecord(day("2026-09-11"), totalVisitDays = 43, currentStreakDays = 12)

        val result = recordVisit(previous, day("2026-09-05"))

        assertEquals(day("2026-09-05"), result.lastVisitEpochDay)
        assertEquals(43, result.totalVisitDays)
        assertEquals(12, result.currentStreakDays)
    }

    @Test
    fun recordVisit_deviceClockMovedBackwardRepeatedly_doesNotInflateTotal() {
        var record = VisitRecord(day("2026-09-11"), totalVisitDays = 43, currentStreakDays = 12)

        repeat(20) { record = recordVisit(record, day("2026-01-01")) }

        assertEquals(43, record.totalVisitDays)
        assertEquals(12, record.currentStreakDays)
    }

    @Test
    fun recordVisit_afterBackwardClock_resumesNormallyNextDay() {
        val rolledBack = recordVisit(
            VisitRecord(day("2026-09-11"), totalVisitDays = 43, currentStreakDays = 12),
            day("2026-09-05")
        )

        val nextDay = recordVisit(rolledBack, day("2026-09-06"))

        assertEquals(44, nextDay.totalVisitDays)
        assertEquals(13, nextDay.currentStreakDays)
    }

    @Test
    fun recordVisit_neverProducesNonPositiveOrImpossibleCounts() {
        val days = listOf(
            day("2026-09-11"),
            day("2026-09-11"),
            day("2026-09-12"),
            day("2026-01-01"),
            day("2026-01-02"),
            day("2027-06-06")
        )
        var record: VisitRecord? = null

        days.forEach { today ->
            val updated = recordVisit(record, today)

            assertTrue("총 방문일은 항상 1 이상", updated.totalVisitDays >= 1)
            assertTrue("연속 방문일은 항상 1 이상", updated.currentStreakDays >= 1)
            assertTrue(
                "연속 방문일이 총 방문일을 넘을 수 없다",
                updated.currentStreakDays <= updated.totalVisitDays
            )

            record = updated
        }
    }

    // ---- 8. 현지 자정 경계 ----

    @Test
    fun visitEpochDay_justBeforeLocalMidnight_isStillToday() {
        assertEquals(
            day("2026-09-11"),
            visitEpochDay(millisAt("2026-09-11T23:59:59.999"), seoul)
        )
    }

    @Test
    fun visitEpochDay_atLocalMidnight_isAlreadyTomorrow() {
        assertEquals(
            day("2026-09-12"),
            visitEpochDay(millisAt("2026-09-12T00:00:00"), seoul)
        )
    }

    @Test
    fun visitEpochDay_usesLocalDateNotUtcDate() {
        // 한국 오전 8시는 UTC로는 아직 전날이다. 사용자가 느끼는 "오늘"을 써야 한다.
        val morningInSeoul = millisAt("2026-09-11T08:00:00")

        assertEquals(day("2026-09-11"), visitEpochDay(morningInSeoul, seoul))
        assertEquals(day("2026-09-10"), visitEpochDay(morningInSeoul, ZoneId.of("UTC")))
    }

    @Test
    fun visitDayStartMillis_roundTripsWithVisitEpochDay() {
        val epochDay = day("2026-09-11")
        val startMillis = visitDayStartMillis(epochDay, seoul)

        assertEquals(millisAt("2026-09-11T00:00:00"), startMillis)
        assertEquals(epochDay, visitEpochDay(startMillis, seoul))
    }

    // ---- 저장 형식 ----

    @Test
    fun serialize_thenParse_roundTrips() {
        val record = VisitRecord(day("2026-09-11"), totalVisitDays = 43, currentStreakDays = 12)

        assertEquals(record, parseVisitRecord(record.serialize()))
    }

    @Test
    fun serialize_isSingleTabSeparatedLine() {
        val text = VisitRecord(day("2026-09-11"), 43, 12).serialize()

        assertTrue("줄바꿈 없는 한 줄이어야 한다", !text.contains("\n"))
        assertEquals(4, text.split("\t").size)
    }

    @Test
    fun parseVisitRecord_toleratesTrailingNewline() {
        val record = VisitRecord(day("2026-09-11"), 43, 12)

        assertNotNull(parseVisitRecord(record.serialize() + "\n"))
    }

    @Test
    fun parseVisitRecord_rejectsGarbageAndImpossibleValues() {
        assertNull(parseVisitRecord(""))
        assertNull(parseVisitRecord("nonsense"))
        assertNull("숫자가 아닌 날짜", parseVisitRecord("1\tnot-a-number\t1\t1"))
        assertNull("항목이 부족한 줄", parseVisitRecord("1\t20000\t1"))
        assertNull("모르는 형식 버전", parseVisitRecord("99\t20000\t1\t1"))
        assertNull("총 방문일은 1 미만일 수 없다", parseVisitRecord("1\t20000\t0\t0"))
        assertNull("음수 방문일은 손상으로 본다", parseVisitRecord("1\t20000\t-5\t-5"))
        assertNull("연속일이 총 방문일보다 클 수 없다", parseVisitRecord("1\t20000\t3\t9"))
    }
}
