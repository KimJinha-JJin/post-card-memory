package com.postcardmemory.ui.gallery

import com.postcardmemory.data.Postcard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class MemoryScaleModelTest {
    private val utc = ZoneId.of("UTC")
    private val date = LocalDate.of(2026, 11, 1)
    private fun card(id: Long, captured: String, state: String = "NONE") =
        Postcard(id = id, imagePath = "/fake/$id", title = "test", capturedAt = Instant.parse(captured).toEpochMilli(), futureMailState = state)
    private val cards = listOf(
        card(1, "2025-12-31T15:30:00Z"), card(2, "2026-10-31T15:30:00Z"),
        card(3, "2026-11-30T23:30:00Z"), card(4, "2026-11-01T12:00:00Z", "SENT")
    )

    @Test fun allMeasuresOnlyOrdinaryPostcardsWithoutDateLimit() {
        assertEquals(listOf(1L, 2L, 3L), memoryScalePostcards(cards, MemoryScalePeriod.ALL, date, utc).map { it.id })
    }
    @Test fun yearAndMonthUseCapturedAtInSelectedZone() {
        assertEquals(listOf(2L, 3L), memoryScalePostcards(cards, MemoryScalePeriod.YEAR, date, utc).map { it.id })
        assertEquals(listOf(3L), memoryScalePostcards(cards, MemoryScalePeriod.MONTH, date, utc).map { it.id })
        val seoul = ZoneId.of("Asia/Seoul")
        assertEquals(listOf(1L, 2L, 3L), memoryScalePostcards(cards, MemoryScalePeriod.YEAR, date, seoul).map { it.id })
        assertEquals(listOf(2L), memoryScalePostcards(cards, MemoryScalePeriod.MONTH, date, seoul).map { it.id })
    }
    @Test fun periodSwitchesAndEmptyPeriodsDoNotRetainPreviousCount() {
        assertEquals(listOf(3, 1, 2, 0, 3), listOf(
            memoryScalePostcards(cards, MemoryScalePeriod.ALL, date, utc).size,
            memoryScalePostcards(cards, MemoryScalePeriod.MONTH, date, utc).size,
            memoryScalePostcards(cards, MemoryScalePeriod.YEAR, date, utc).size,
            memoryScalePostcards(cards, MemoryScalePeriod.MONTH, date.withMonth(9), utc).size,
            memoryScalePostcards(cards, MemoryScalePeriod.ALL, date, utc).size))
    }
    @Test fun capacityBoundariesExtendWithoutTenThousandLimit() {
        val expected = mapOf(0 to 60L, 1 to 60L, 11 to 60L, 30 to 60L, 60 to 60L, 61 to 100L,
            100 to 100L, 101 to 200L, 500 to 500L, 501 to 1000L, 1000 to 1000L,
            10000 to 10000L, 10001 to 20000L, Int.MAX_VALUE to 5000000000L)
        expected.forEach { (count, capacity) -> assertEquals("count=$count", capacity, memoryScaleCapacity(count)) }
    }
    @Test fun finalNeedleAndTrayMatchActualMeasurementForAllRequiredCounts() {
        for (count in listOf(0, 1, 11, 30, 60, 61, 100, 101, 500, 1000, 10000, 10001, Int.MAX_VALUE)) {
            val ratio = count.toDouble() / memoryScaleCapacity(count)
            val frame = memoryScaleFrame(count, 2400)
            assertTrue(frame.finished); assertFalse(frame.falling)
            assertEquals((-120 + 240 * ratio).toFloat(), frame.needleDegrees, .00001f)
            assertEquals((16 * ratio).toFloat(), frame.trayY, .00001f)
            assertEquals(count.coerceAtMost(12), frame.layers)
        }
    }
    @Test fun beforeContactNoLoadThenPileTouchesTrayAtFourHundred() {
        val before = memoryScaleFrame(11, 399)
        assertTrue(before.falling); assertEquals(0, before.layers)
        assertEquals(0f, before.trayY, 0f); assertEquals(-120f, before.needleDegrees, 0f)
        assertTrue(before.dropY < 0f)
        val contact = memoryScaleFrame(11, 400)
        assertFalse(contact.falling); assertEquals(11, contact.layers)
        assertEquals(0f, contact.dropY, 0f); assertEquals(0f, contact.trayY, 0f)
        assertTrue(memoryScaleFrame(11, 450).trayY > 0f)
    }
    @Test fun independentMockupFixturesKeepApprovedBResponse() {
        val frame = memoryScaleFrame(11, 500)
        // 승인 HTML의 sample(500,{count:11,motion:'B',capacity:60}) 독립 표본.
        assertEquals(2.8073871214f, frame.trayY, .00001f)
        assertEquals(-84.9135215579f, frame.needleDegrees, .00001f)
    }
    @Test fun bHasSmallDampedOvershootButNeverLeavesDialOrPistonLimits() {
        val target = memoryScaleFrame(30, 2400)
        var maxNeedle = -120f
        for (t in 0L..2400L step 5) {
            val frame = memoryScaleFrame(30, t)
            maxNeedle = maxOf(maxNeedle, frame.needleDegrees)
            val full = memoryScaleFrame(60, t)
            assertTrue(full.needleDegrees in -120f..120f)
            assertTrue(full.trayY in 0f..16f)
        }
        assertTrue(maxNeedle > target.needleDegrees)
        assertTrue(maxNeedle < target.needleDegrees + 8f)
        assertEquals(target.needleDegrees, memoryScaleFrame(30, 2399).needleDegrees, .0001f)
    }
    @Test fun reducedMotionImmediatelyUsesExactFinalPoseAtAnyElapsedTime() {
        for (count in listOf(0, 1, 11, 61, 10001)) {
            assertEquals(memoryScaleFrame(count, 2400), memoryScaleFrame(count, 0, reducedMotion = true))
        }
    }
    @Test fun zeroAndNegativeInputsHaveNoFallingPileOrNeedleMovement() {
        for (count in listOf(0, -1)) for (t in listOf(0L, 60L, 400L, 1200L, 2400L)) {
            val frame = memoryScaleFrame(count, t)
            assertTrue(frame.finished); assertFalse(frame.falling); assertEquals(0, frame.layers)
            assertEquals(-120f, frame.needleDegrees, 0f); assertEquals(0f, frame.trayY, 0f)
        }
    }
    @Test fun risingAndFallingCountsHaveNoHistoryDependentResult() {
        val counts = listOf(1, 10000, 11, 0, 101, 60, 1)
        val frames = counts.map { memoryScaleFrame(it, 2400) }
        assertEquals(frames.first(), frames.last())
        assertEquals(-120f, frames[3].needleDegrees, 0f)
        assertEquals(12, frames[1].layers)
    }
}
