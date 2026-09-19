package com.postcardmemory.ui.futuremail

import com.postcardmemory.data.FUTURE_MAIL_STATE_SENT
import com.postcardmemory.data.Postcard
import com.postcardmemory.testsupport.readStructureTestSource
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 78일차 P2-6: 앱을 켜 둔 채 자정을 넘기면 미래 우체통의 도착 버튼·D-day·
 * 진행률·봉인 상태가 어제 기준에 머물던 문제.
 *
 * `deliverAtMillis`의 의미는 **로컬 타임존 자정**이고(발송 시
 * materialDatePickerUtcMillisToLocalStartOfDay로 변환, 그룹화 시
 * startOfDayMillis로 재정규화) 도착·D-day·진행률 판정이 전부 날짜 단위라,
 * 갱신 주기는 "자정마다 한 번"이면 충분하다. 아래 첫 번째 테스트가 그
 * 전제(시:분은 판정에 영향이 없다)를 직접 못박는다.
 */
class FutureMailTimeBoundaryTest {

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    private fun millisOf(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long =
        ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    private fun sent(id: Long, deliverAt: Long, capturedAt: Long) = Postcard(
        id = id,
        imagePath = "/$id.jpg",
        title = "postcard-$id",
        capturedAt = capturedAt,
        futureMailState = FUTURE_MAIL_STATE_SENT,
        futureMailDeliverAt = deliverAt
    )

    // ── deliverAt의 의미: 시각이 아니라 날짜 ──

    @Test
    fun withinTheSameDay_theTimeOfDayChangesNothing() {
        val deliverAt = millisOf(2026, 12, 25)
        val sentAt = millisOf(2026, 12, 20)

        val atDawn = buildFutureMailGroups(
            listOf(sent(1, deliverAt, sentAt)), millisOf(2026, 12, 24, hour = 0, minute = 1), zone
        ).single()
        val atNight = buildFutureMailGroups(
            listOf(sent(1, deliverAt, sentAt)), millisOf(2026, 12, 24, hour = 23, minute = 59), zone
        ).single()

        assertEquals(atDawn, atNight)
    }

    // ── 자정 경계: DB가 그대로여도 판정이 바뀐다 ──

    @Test
    fun crossingMidnightFlipsArrivedEvenThoughTheStoredDataIsIdentical() {
        val deliverAt = millisOf(2026, 12, 25)
        val stored = listOf(sent(1, deliverAt, millisOf(2026, 12, 20)))

        val beforeMidnight =
            buildFutureMailGroups(stored, millisOf(2026, 12, 24, hour = 23, minute = 59), zone).single()
        val afterMidnight =
            buildFutureMailGroups(stored, millisOf(2026, 12, 25, hour = 0, minute = 1), zone).single()

        assertFalse(beforeMidnight.arrived)
        assertTrue(afterMidnight.arrived)
    }

    @Test
    fun crossingMidnightDecrementsDaysLeftAndAdvancesProgress() {
        val deliverAt = millisOf(2026, 12, 25)
        val stored = listOf(sent(1, deliverAt, millisOf(2026, 12, 15)))

        val before =
            buildFutureMailGroups(stored, millisOf(2026, 12, 23, hour = 23, minute = 59), zone).single()
        val after =
            buildFutureMailGroups(stored, millisOf(2026, 12, 24, hour = 0, minute = 1), zone).single()

        assertEquals(2L, before.daysLeft)
        assertEquals(1L, after.daysLeft)
        assertTrue(after.progressPercent > before.progressPercent)
    }

    // ── D-day와 진행률이 같은 기준을 쓴다 ──

    @Test
    fun daysLeftAndProgressAreComputedFromTheSameNow() {
        val deliverAt = millisOf(2026, 12, 25)
        val now = millisOf(2026, 12, 20, hour = 10)
        val group = buildFutureMailGroups(
            listOf(sent(1, deliverAt, millisOf(2026, 12, 15))), now, zone
        ).single()

        assertEquals(daysUntilFutureMail(group.deliverAtMillis, now, zone), group.daysLeft)
        assertEquals(isFutureMailArrived(group.deliverAtMillis, now, zone), group.arrived)
    }

    @Test
    fun arrivedGroupReportsZeroOrNegativeDaysLeft() {
        val deliverAt = millisOf(2026, 12, 25)
        val group = buildFutureMailGroups(
            listOf(sent(1, deliverAt, millisOf(2026, 12, 15))),
            millisOf(2026, 12, 26, hour = 9),
            zone
        ).single()

        assertTrue(group.arrived)
        assertTrue(group.daysLeft <= 0L)
        assertEquals(100, group.progressPercent)
    }

    // ── 화면·ViewModel이 실제로 자정 신호를 쓰는가 ──

    private fun source(name: String): String =
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/$name",
                "app/src/main/java/com/postcardmemory/$name"
            )
        )

    @Test
    fun theMailboxViewModelRecomputesOnEveryDayBoundary_notOnlyOnRoomEmissions() {
        val vm = source("ui/futuremail/FutureMailboxViewModel.kt")

        assertTrue(vm.contains("dayBoundaryTicks()"))
        assertTrue(vm.contains("combine("))
    }

    @Test
    fun theMailboxCardReadsDaysLeftFromTheGroup_insteadOfReadingTheClockAgain() {
        val screen = source("ui/futuremail/FutureMailboxScreen.kt")

        assertTrue(screen.contains("val daysLeft = group.daysLeft"))
        assertFalse(screen.contains("daysUntilFutureMail("))
        assertFalse(screen.contains("System.currentTimeMillis()"))
    }

    @Test
    fun theSealedDetailScreenRebindsItsNowWhenTheDateChanges() {
        val detail = source("ui/detail/DetailScreen.kt")

        assertTrue(detail.contains("val now = remember(today) { System.currentTimeMillis() }"))
        assertFalse(detail.contains("val now = remember { System.currentTimeMillis() }"))
    }
}
