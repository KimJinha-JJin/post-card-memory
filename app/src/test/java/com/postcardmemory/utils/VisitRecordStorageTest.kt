package com.postcardmemory.utils

import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 방문 기록 파일 저장소를 실제 파일로 검증한다. Context나 Room 없이
 * filesDir만 받는 internal 오버로드를 쓰므로 순수 JUnit에서 돌아간다
 * (ConfirmedEditStateStorageTest, PostcardTemplateStorageTest와 같은 방식).
 */
class VisitRecordStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val seoul = ZoneId.of("Asia/Seoul")

    private fun filesDir(): File = tempFolder.root

    private fun day(text: String): Long = LocalDate.parse(text).toEpochDay()

    private fun millisAt(text: String): Long =
        LocalDateTime.parse(text).atZone(seoul).toInstant().toEpochMilli()

    private fun recordAt(text: String): TodayVisit =
        VisitRecordStorage.recordTodayVisit(
            filesDir = filesDir(),
            nowMillis = millisAt(text),
            zone = seoul
        )

    @Test
    fun load_withNoFileYet_isNull() {
        assertNull(VisitRecordStorage.load(filesDir()))
    }

    @Test
    fun recordTodayVisit_firstRun_writesFirstVisitAndPersistsIt() {
        val recorded = recordAt("2026-09-11T09:00:00")

        assertEquals(day("2026-09-11"), recorded.record.lastVisitEpochDay)
        assertEquals(1, recorded.record.totalVisitDays)
        assertEquals(1, recorded.record.currentStreakDays)
        assertEquals(recorded.record, VisitRecordStorage.load(filesDir()))
    }

    // ---- 오늘의 첫 방문인지(닿는 순간 진동을 울릴지) ----

    @Test
    fun recordTodayVisit_firstRunEver_isFirstVisitToday() {
        assertTrue(recordAt("2026-09-11T09:00:00").isFirstVisitToday)
    }

    @Test
    fun recordTodayVisit_sameDayAgain_isNotFirstVisitToday() {
        recordAt("2026-09-11T09:00:00")

        assertFalse(recordAt("2026-09-11T21:30:00").isFirstVisitToday)
        assertFalse(recordAt("2026-09-11T23:59:00").isFirstVisitToday)
    }

    @Test
    fun recordTodayVisit_nextDay_isFirstVisitTodayAgain() {
        recordAt("2026-09-11T09:00:00")
        recordAt("2026-09-11T20:00:00")

        assertTrue(recordAt("2026-09-12T08:00:00").isFirstVisitToday)
        assertFalse(recordAt("2026-09-12T22:00:00").isFirstVisitToday)
    }

    @Test
    fun recordTodayVisit_afterGap_isFirstVisitToday() {
        recordAt("2026-09-11T09:00:00")

        assertTrue(recordAt("2026-09-20T09:00:00").isFirstVisitToday)
    }

    @Test
    fun recordTodayVisit_firstVisitTodayMatchesWhetherTheFileWasWritten() {
        // "오늘의 첫 방문"과 "파일을 새로 썼다"는 같은 판정이어야 한다.
        val first = recordAt("2026-09-11T09:00:00")
        val file = VisitRecordStorage.visitRecordFile(filesDir())
        val pinnedModifiedAt = millisAt("2020-01-01T00:00:00")

        assertTrue(first.isFirstVisitToday)
        assertTrue("테스트 전제: 수정 시각을 고정할 수 있어야 함", file.setLastModified(pinnedModifiedAt))

        val sameDay = recordAt("2026-09-11T23:00:00")

        assertFalse(sameDay.isFirstVisitToday)
        assertTrue(
            "첫 방문이 아니면 파일도 다시 쓰지 않아야 함(lastModified=${file.lastModified()})",
            file.lastModified() < millisAt("2021-01-01T00:00:00")
        )
    }

    // ---- 기록 내용 영속 ----

    @Test
    fun recordTodayVisit_sameDayAgain_doesNotChangeStoredRecord() {
        recordAt("2026-09-11T09:00:00")
        val second = recordAt("2026-09-11T21:30:00")

        assertEquals(1, second.record.totalVisitDays)
        assertEquals(1, second.record.currentStreakDays)
        assertEquals(second.record, VisitRecordStorage.load(filesDir()))
    }

    @Test
    fun recordTodayVisit_acrossConsecutiveDays_persistsGrowingStreak() {
        recordAt("2026-09-11T09:00:00")
        recordAt("2026-09-12T09:00:00")
        val third = recordAt("2026-09-13T09:00:00")

        assertEquals(3, third.record.totalVisitDays)
        assertEquals(3, third.record.currentStreakDays)
        assertEquals(third.record, VisitRecordStorage.load(filesDir()))
    }

    @Test
    fun recordTodayVisit_afterGap_persistsTotalButRestartedStreak() {
        recordAt("2026-09-11T09:00:00")
        recordAt("2026-09-12T09:00:00")
        val afterGap = recordAt("2026-09-20T09:00:00")

        assertEquals(3, afterGap.record.totalVisitDays)
        assertEquals(1, afterGap.record.currentStreakDays)
        assertEquals(afterGap.record, VisitRecordStorage.load(filesDir()))
    }

    @Test
    fun recordTodayVisit_justBeforeAndAfterLocalMidnight_countsAsTwoDays() {
        val lateNight = recordAt("2026-09-11T23:59:00")
        val justAfterMidnight = recordAt("2026-09-12T00:00:30")

        assertEquals(1, lateNight.record.totalVisitDays)
        assertEquals(2, justAfterMidnight.record.totalVisitDays)
        assertEquals(2, justAfterMidnight.record.currentStreakDays)
        assertTrue(justAfterMidnight.isFirstVisitToday)
    }

    @Test
    fun recordTodayVisit_corruptFile_startsOverWithoutCrashing() {
        VisitRecordStorage.visitRecordFile(filesDir()).also { file ->
            file.parentFile?.mkdirs()
            file.writeText("깨진 내용")
        }

        val recorded = recordAt("2026-09-11T09:00:00")

        assertEquals(1, recorded.record.totalVisitDays)
        assertEquals(recorded.record, VisitRecordStorage.load(filesDir()))
    }

    // ---- 다른 데이터를 건드리지 않는다 ----

    @Test
    fun visitRecordFile_livesInItsOwnFolderUnderFilesDir() {
        val file = VisitRecordStorage.visitRecordFile(filesDir())

        assertEquals("visit_record.txt", file.name)
        assertEquals("visits", file.parentFile?.name)
        assertEquals(filesDir(), file.parentFile?.parentFile)
    }

    @Test
    fun recordTodayVisit_touchesOnlyItsOwnFolder() {
        // 엽서/스티커/초안 쪽 파일을 미리 놓아두고, 방문 기록이 그 파일들을
        // 건드리지 않는지 확인한다.
        val postcardImage = File(tempFolder.newFolder("postcards"), "1.jpg")
        postcardImage.writeText("postcard-bytes")
        val sealState = File(tempFolder.newFolder("seal_states"), "1.txt")
        sealState.writeText("seal-state")

        recordAt("2026-09-11T09:00:00")

        assertEquals("postcard-bytes", postcardImage.readText())
        assertEquals("seal-state", sealState.readText())
    }

    @Test
    fun recordTodayVisit_leavesNoLeftoverTempFile() {
        recordAt("2026-09-11T09:00:00")

        val file = VisitRecordStorage.visitRecordFile(filesDir())
        val leftoverTemp = File(file.parentFile, "${file.name}.tmp")

        assertTrue(file.exists())
        assertFalse(leftoverTemp.exists())
    }
}
