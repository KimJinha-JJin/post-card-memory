package com.postcardmemory.utils

import com.postcardmemory.testsupport.readStructureTestSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * canonical 정의([VisitRecord])의 세 번째 항목만 따로 고정한다:
 * **앱을 켜 둔 채 자정을 넘기는 것만으로는 새 방문이 생기지 않는다.**
 *
 * 이 보장이 어디서 오는지 먼저 분명히 해 둔다. 저장소 자체는 자정을 넘겨
 * 다시 호출되면 당연히 새 방문을 만든다 —
 * `VisitRecordStorageTest.recordTodayVisit_justBeforeAndAfterLocalMidnight_countsAsTwoDays`
 * 가 그 동작을 이미 고정하고 있고, 그게 맞는 동작이다. 따라서 "자정만
 * 통과했을 때 새 방문이 없다"는 보장은 **자정 신호를 받는 쪽이 방문 기록
 * API를 아예 부르지 않는다**는 사실에서만 나온다. 아래 첫 두 테스트가
 * 그 사실을 실제 production 소스에서 확인한다(구조 검사인 이유: 화면을
 * 띄운 채 자정을 넘기는 것은 JVM unit test로 재현할 수 없고, 실기기 날짜
 * 변경은 AGENTS.md 5절로 금지돼 있다).
 *
 * 마지막 테스트는 순수 함수 쪽 짝을 이룬다 — 날짜 경계 계산이 바뀌어도
 * 같은 날 두 번째 호출은 기록을 늘리지 않는다는 것.
 */
class VisitDayBoundaryDefinitionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val seoul = ZoneId.of("Asia/Seoul")

    private fun sourceOf(relativePath: String): String =
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/$relativePath",
                "app/src/main/java/com/postcardmemory/$relativePath"
            )
        )

    /** 자정 신호([dayBoundaryTicks] / `rememberTodayDate`)를 실제로 소비하는 production 파일들. */
    private val dayBoundaryConsumers = listOf(
        "ui/futuremail/FutureMailboxViewModel.kt",
        "ui/detail/DetailScreen.kt",
        "ui/gallery/GalleryScreen.kt",
        "ui/gallery/VisitCalendarDrawer.kt"
    )

    @Test
    fun theListOfDayBoundaryConsumersIsStillComplete() {
        // 새 소비자가 생겼는데 이 목록에 없으면 아래 금지 검사가 조용히
        // 비어버린다. 목록이 실제 소스와 맞는지 먼저 확인한다.
        for (path in dayBoundaryConsumers) {
            val text = sourceOf(path)
            assertTrue(
                "$path 가 더 이상 자정 신호를 쓰지 않는다면 이 목록에서 빼야 함",
                text.contains("dayBoundaryTicks(") || text.contains("rememberTodayDate()")
            )
        }
    }

    @Test
    fun noDayBoundaryConsumerRecordsAVisit() {
        for (path in dayBoundaryConsumers) {
            val text = sourceOf(path)

            assertFalse(
                "$path: 자정 신호를 받는 화면이 방문을 기록하면, 앱을 켜 둔 채 " +
                    "자정을 넘기는 것만으로 없던 방문일이 생긴다",
                text.contains("recordTodayVisit(")
            )
            assertFalse(
                "$path: 자정 신호를 받는 화면이 방문 달력 표식을 새로 만들면 안 됨 " +
                    "(표시와 기록 생성은 다른 일이다)",
                text.contains("VisitHistoryStorage.recordDate(")
            )
        }
    }

    @Test
    fun theOnlyPlaceThatRecordsAVisitDoesNotUseTheDayBoundarySignal() {
        val mainActivity = sourceOf("MainActivity.kt")

        assertTrue(
            "방문 기록 진입점은 MainActivity 하나여야 함",
            mainActivity.contains("VisitRecordStorage.recordTodayVisit(")
        )
        assertFalse(
            "방문 기록이 자정 신호에 연결되면 켜 둔 채 자정을 넘길 때 방문이 생긴다",
            mainActivity.contains("dayBoundaryTicks(") || mainActivity.contains("rememberTodayDate()")
        )
        assertTrue(
            "판정은 프로세스당 한 번 — 날짜가 아니라 holder가 null인지로 가른다",
            mainActivity.contains("if (AppIntroState.todayVisit == null)")
        )
    }

    @Test
    fun recordingTwiceOnTheSameDayNeverGrowsTheRecord_evenLateAtNight() {
        // 자정 직전에 다시 열어도 그날의 방문은 여전히 1회다.
        val morning = record("2026-09-19T08:00:00")
        val justBeforeMidnight = record("2026-09-19T23:59:59")

        assertEquals(1, morning.record?.totalVisitDays)
        assertEquals(morning.record, justBeforeMidnight.record)
        assertFalse(justBeforeMidnight.isFirstVisitToday)
        assertEquals(
            LocalDate.parse("2026-09-19").toEpochDay(),
            justBeforeMidnight.record?.lastVisitEpochDay
        )
    }

    private fun record(text: String): TodayVisit =
        VisitRecordStorage.recordTodayVisit(
            filesDir = tempFolder.root,
            nowMillis = LocalDateTime.parse(text).atZone(seoul).toInstant().toEpochMilli(),
            zone = seoul
        )
}
