package com.postcardmemory.utils

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 133일차: 필름 인트로가 며칠 만에 왔는지 알 수 있도록 [TodayVisit.previousVisitEpochDay]에
 * 직전 방문일을 넘긴다. 이미 읽은 값을 메모리로만 전달할 뿐 저장 파일 내용·형식은 그대로인지 함께 본다.
 */
class VisitPreviousDayTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val seoul = ZoneId.of("Asia/Seoul")

    private fun filesDir(): File = tempFolder.root

    private fun day(text: String): Long = LocalDate.parse(text).toEpochDay()

    private fun recordAt(text: String): TodayVisit =
        VisitRecordStorage.recordTodayVisit(
            filesDir = filesDir(),
            nowMillis = LocalDateTime.parse(text).atZone(seoul).toInstant().toEpochMilli(),
            zone = seoul
        )

    @Test
    fun firstRunEver_hasNoPreviousDay() {
        assertNull(recordAt("2026-11-15T09:00").previousVisitEpochDay)
    }

    @Test
    fun nextDay_reportsYesterdayAsPrevious() {
        recordAt("2026-11-14T21:00")
        val today = recordAt("2026-11-15T09:00")
        assertEquals(day("2026-11-14"), today.previousVisitEpochDay)
        assertEquals(day("2026-11-15"), today.record?.lastVisitEpochDay)
    }

    @Test
    fun afterThirtyDayGap_reportsTheRealLastVisit() {
        recordAt("2026-10-16T09:00")
        val today = recordAt("2026-11-15T09:00")
        assertEquals(day("2026-10-16"), today.previousVisitEpochDay)
    }

    @Test
    fun sameDayAgain_previousIsToday() {
        recordAt("2026-11-15T09:00")
        val again = recordAt("2026-11-15T20:00")
        assertEquals(day("2026-11-15"), again.previousVisitEpochDay)
    }

    @Test
    fun storedFileFormatIsUnchanged_previousIsNeverWritten() {
        recordAt("2026-11-14T09:00")
        recordAt("2026-11-15T09:00")
        val stored = VisitRecordStorage.visitRecordFile(filesDir()).readText()
        assertEquals(
            "버전·마지막 방문일·총 방문일·연속일 네 칸 그대로",
            "1\t${day("2026-11-15")}\t2\t2",
            stored
        )
    }
}
