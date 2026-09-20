package com.postcardmemory.utils

import java.io.File
import java.io.IOException
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

    private fun TodayVisit.knownRecord(): VisitRecord = requireNotNull(record)

    @Test
    fun load_withNoFileYet_isNull() {
        assertNull(VisitRecordStorage.load(filesDir()))
    }

    @Test
    fun recordTodayVisit_firstRun_writesFirstVisitAndPersistsIt() {
        val recorded = recordAt("2026-09-11T09:00:00")

        assertEquals(day("2026-09-11"), recorded.knownRecord().lastVisitEpochDay)
        assertEquals(1, recorded.knownRecord().totalVisitDays)
        assertEquals(1, recorded.knownRecord().currentStreakDays)
        assertEquals(recorded.knownRecord(), VisitRecordStorage.load(filesDir()))
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

        assertEquals(1, second.knownRecord().totalVisitDays)
        assertEquals(1, second.knownRecord().currentStreakDays)
        assertEquals(second.knownRecord(), VisitRecordStorage.load(filesDir()))
    }

    @Test
    fun recordTodayVisit_acrossConsecutiveDays_persistsGrowingStreak() {
        recordAt("2026-09-11T09:00:00")
        recordAt("2026-09-12T09:00:00")
        val third = recordAt("2026-09-13T09:00:00")

        assertEquals(3, third.knownRecord().totalVisitDays)
        assertEquals(3, third.knownRecord().currentStreakDays)
        assertEquals(third.knownRecord(), VisitRecordStorage.load(filesDir()))
    }

    @Test
    fun recordTodayVisit_afterGap_persistsTotalButRestartedStreak() {
        recordAt("2026-09-11T09:00:00")
        recordAt("2026-09-12T09:00:00")
        val afterGap = recordAt("2026-09-20T09:00:00")

        assertEquals(3, afterGap.knownRecord().totalVisitDays)
        assertEquals(1, afterGap.knownRecord().currentStreakDays)
        assertEquals(afterGap.knownRecord(), VisitRecordStorage.load(filesDir()))
    }

    @Test
    fun recordTodayVisit_justBeforeAndAfterLocalMidnight_countsAsTwoDays() {
        val lateNight = recordAt("2026-09-11T23:59:00")
        val justAfterMidnight = recordAt("2026-09-12T00:00:30")

        assertEquals(1, lateNight.knownRecord().totalVisitDays)
        assertEquals(2, justAfterMidnight.knownRecord().totalVisitDays)
        assertEquals(2, justAfterMidnight.knownRecord().currentStreakDays)
        assertTrue(justAfterMidnight.isFirstVisitToday)
    }

    @Test
    fun recordTodayVisit_corruptFile_startsOverWithoutCrashing() {
        VisitRecordStorage.visitRecordFile(filesDir()).also { file ->
            file.parentFile?.mkdirs()
            file.writeText("깨진 내용")
        }

        val recorded = recordAt("2026-09-11T09:00:00")

        assertEquals(1, recorded.knownRecord().totalVisitDays)
        assertEquals(recorded.knownRecord(), VisitRecordStorage.load(filesDir()))
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

    // ---- 읽기 실패는 손상이 아니다(누적 방문일 보존) ----
    //
    // 읽기 실패를 "기록 없음"과 같이 취급하면 recordVisit(null, today)가
    // 새 기록(총 1일)을 만들고 그대로 저장돼, 잠깐의 I/O 실패 한 번으로
    // 사용자의 누적 방문일이 영구히 사라진다. 아래 테스트들은 실제
    // 예외를 주입해 그 경로를 재현한다(항상 참인 fake 상태가 아니다).

    private fun recordAtFailingRead(text: String): TodayVisit =
        VisitRecordStorage.recordTodayVisit(
            filesDir = filesDir(),
            nowMillis = millisAt(text),
            zone = seoul,
            readRecordText = { throw IOException("일시적 읽기 실패") }
        )

    @Test
    fun recordTodayVisit_readFailure_keepsTheStoredRecordUntouched() {
        recordAt("2026-09-11T09:00:00")
        recordAt("2026-09-12T09:00:00")
        recordAt("2026-09-13T09:00:00")
        val before = VisitRecordStorage.visitRecordFile(filesDir()).readText()

        recordAtFailingRead("2026-09-14T09:00:00")

        assertEquals(before, VisitRecordStorage.visitRecordFile(filesDir()).readText())
        assertEquals(3, VisitRecordStorage.load(filesDir())?.totalVisitDays)
    }

    @Test
    fun recordTodayVisit_readFailure_isNotReportedAsTheFirstVisitOfTheDay() {
        recordAt("2026-09-11T09:00:00")

        val unreadable = recordAtFailingRead("2026-09-12T09:00:00")

        assertFalse(unreadable.isFirstVisitToday)
        assertNull("알 수 없는 누적값을 신규 방문 1회로 만들면 안 됨", unreadable.record)
    }

    @Test
    fun recordTodayVisit_afterATransientReadFailure_theNextRunResumesFromTheRealCount() {
        recordAt("2026-09-11T09:00:00")
        recordAt("2026-09-12T09:00:00")

        recordAtFailingRead("2026-09-13T09:00:00")
        val recovered = recordAt("2026-09-13T21:00:00")

        assertEquals(3, recovered.knownRecord().totalVisitDays)
        assertEquals(3, recovered.knownRecord().currentStreakDays)
        assertTrue(recovered.isFirstVisitToday)
    }

    @Test
    fun recordTodayVisit_readFailureOnARealUnreadableFile_stillKeepsTheRecord() {
        // 이음매 없이 production 기본 경로만 쓰는 검증: 기록 파일 자리를
        // 디렉터리로 만들어 readText가 실제로 실패하게 한다.
        recordAt("2026-09-11T09:00:00")
        val file = VisitRecordStorage.visitRecordFile(filesDir())
        file.delete()
        assertTrue(file.mkdirs())

        val recorded = VisitRecordStorage.recordTodayVisit(
            filesDir = filesDir(),
            nowMillis = millisAt("2026-09-12T09:00:00"),
            zone = seoul
        )

        assertFalse(recorded.isFirstVisitToday)
        assertNull(recorded.record)
        assertTrue(file.isDirectory)
    }

    @Test
    fun readStoredRecord_separatesMissingFromUnreadableFromCorrupt() {
        assertEquals(
            VisitRecordStorage.StoredVisitRecord.Absent,
            VisitRecordStorage.readStoredRecord(filesDir())
        )

        recordAt("2026-09-11T09:00:00")
        assertEquals(
            VisitRecordStorage.StoredVisitRecord.Unreadable,
            VisitRecordStorage.readStoredRecord(filesDir()) { throw IOException("boom") }
        )

        VisitRecordStorage.visitRecordFile(filesDir()).writeText("깨진 내용")
        assertEquals(
            VisitRecordStorage.StoredVisitRecord.Present(null),
            VisitRecordStorage.readStoredRecord(filesDir())
        )
    }

    @Test
    fun recordTodayVisit_corruptFile_stillStartsOver_soTheFixDidNotWidenPreservation() {
        // 읽기 실패만 보존 대상이다. 내용을 읽었는데 해석할 수 없으면
        // 기존 정책대로 오늘이 첫 방문으로 다시 시작한다.
        recordAt("2026-09-11T09:00:00")
        VisitRecordStorage.visitRecordFile(filesDir()).writeText("깨진 내용")

        val recorded = recordAt("2026-09-12T09:00:00")

        assertTrue(recorded.isFirstVisitToday)
        assertEquals(1, recorded.knownRecord().totalVisitDays)
    }
}
