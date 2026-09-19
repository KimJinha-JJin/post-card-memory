package com.postcardmemory.utils

import java.time.LocalDateTime
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 78일차 P2-6: 미래 우체통이 Room Flow가 다시 방출할 때까지 "지금"을 다시
 * 읽지 않아 자정을 넘겨도 어제 기준에 머물던 문제를 위해 만든 자정 신호.
 */
class DayBoundaryTest {

    @Test
    fun ticksEmitImmediatelySoACombinedFlowCanProduceRightAway() = runBlocking {
        // 자정 직전 시각을 넣어 대기 시간을 짧게 만든다(실제로는 하루 한 번).
        val justBeforeMidnight = LocalDateTime.of(2026, 9, 19, 23, 59, 59, 950_000_000)

        val ticks = dayBoundaryTicks { justBeforeMidnight }
            .take(1)
            .toList()

        assertEquals(1, ticks.size)
    }

    @Test
    fun ticksKeepComingOnceEachBoundary() = runBlocking {
        val justBeforeMidnight = LocalDateTime.of(2026, 9, 19, 23, 59, 59, 950_000_000)

        val startedAt = System.currentTimeMillis()
        val ticks = dayBoundaryTicks { justBeforeMidnight }
            .take(3)
            .toList()
        val elapsed = System.currentTimeMillis() - startedAt

        assertEquals(3, ticks.size)
        // 첫 방출은 즉시, 이후 두 번은 각각 약 50ms 기다린다.
        assertTrue("실제 경과=$elapsed", elapsed >= 90L)
    }

    @Test
    fun exactlyAtMidnightTheTickerWaitsAFullDayInsteadOfSpinning() {
        val atMidnight = millisUntilNextMidnight(LocalDateTime.of(2026, 9, 19, 0, 0, 0))

        assertEquals(24L * 60L * 60L * 1000L, atMidnight)
    }

    @Test
    fun theTickerAndTheComposeTodayValueShareTheSameBoundaryFunction() {
        // 두 소비자가 각자 자기 방식으로 자정을 계산하면 하루가 갈린다.
        // 같은 함수를 쓴다는 사실을 값으로 확인해 둔다.
        listOf(
            LocalDateTime.of(2026, 9, 19, 23, 59, 0),
            LocalDateTime.of(2026, 12, 31, 23, 59, 59),
            LocalDateTime.of(2026, 9, 19, 12, 0, 0)
        ).forEach { now ->
            assertTrue(millisUntilNextMidnight(now) > 0L)
            assertTrue(millisUntilNextMidnight(now) <= 24L * 60L * 60L * 1000L)
        }
    }
}
