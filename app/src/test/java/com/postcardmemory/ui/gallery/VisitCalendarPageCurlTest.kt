package com.postcardmemory.ui.gallery

import java.time.YearMonth
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VisitCalendarPageCurlTest {
    private val october = YearMonth.of(2026, 10)

    @Test fun repeatedStepsFinishTheActiveSheetThenJumpOnceToTheFinalRequest() {
        var state = VisitCalendarPagingState(october).step(1).startNext()
        val active = state.activeTurn!!
        repeat(100) { state = state.step(1) }
        assertSame(active, state.activeTurn)
        assertEquals(october.plusMonths(1), active.to)
        val finalRequest = october.plusMonths(101)
        assertEquals(finalRequest, state.requestedMonth)
        state = state.complete().startNext()
        assertEquals(VisitCalendarPageTurn(active.to, finalRequest), state.activeTurn)
        state = state.complete()
        assertSame(state, state.startNext())
        assertEquals(finalRequest, state.settledMonth)
    }

    @Test fun reversedInputCannotReverseTheActiveSheetButChangesTheNextOne() {
        var state = VisitCalendarPagingState(october).step(1).startNext()
        val active = state.activeTurn!!
        state = state.step(-1).step(-1)
        assertSame(active, state.startNext().activeTurn)
        assertTrue(active.forward)
        state = state.complete().startNext()
        assertEquals(VisitCalendarPageTurn(october.plusMonths(1), october.minusMonths(1)), state.activeTurn)
        assertFalse(state.activeTurn!!.forward)
    }

    @Test fun requestsThatReturnToTheDestinationDoNotAddAnotherTransition() {
        var state = VisitCalendarPagingState(october).step(1).startNext()
        state = state.step(1).step(-1).complete()
        assertEquals(october.plusMonths(1), state.settledMonth)
        assertSame(state, state.startNext())
        assertEquals(null, state.activeTurn)
    }

    @Test fun absolutePickerAndTodayRequestsReplacePendingStepsAcrossYearBoundaries() {
        val december = YearMonth.of(2026, 12)
        var state = VisitCalendarPagingState(december).step(1).startNext()
        assertEquals(YearMonth.of(2027, 1), state.activeTurn!!.to)
        state = state.step(1).request(YearMonth.of(2020, 4)).request(december)
        state = state.complete().startNext()
        assertEquals(VisitCalendarPageTurn(YearMonth.of(2027, 1), december), state.activeTurn)
        state = state.complete().step(-1).startNext()
        assertEquals(YearMonth.of(2026, 11), state.activeTurn!!.to)
    }

    @Test fun clippedAreasCoverTheSheetWithoutLosingOrDuplicatingPrint() {
        val fold = VisitCalendarPageFold()
        val buffer = FloatArray(12)
        fun area(count: Int): Float {
            var sum = 0f
            for (i in 0 until count) {
                val next = (i + 1) % count
                sum += buffer[i * 2] * buffer[next * 2 + 1] - buffer[next * 2] * buffer[i * 2 + 1]
                assertTrue(buffer[i * 2].isFinite() && buffer[i * 2 + 1].isFinite())
                assertTrue(buffer[i * 2] in -.001f..256.001f)
                assertTrue(buffer[i * 2 + 1] in -.001f..302.001f)
            }
            return abs(sum) / 2f
        }
        listOf(true, false).forEach { forward ->
            for (step in 0..100) {
                fold.update(256f, 302f, step / 100f, forward, 1f)
                val front = area(fold.writePolygon(buffer, folded = false))
                val back = area(fold.writePolygon(buffer, folded = true))
                assertEquals("접힘 선 양쪽의 인쇄 영역 합은 한 장이어야 함", 256f * 302f, front + back, .04f)
            }
        }
    }

    @Test fun foldSupportsDensityScalingAndUnmeasuredPages() {
        val normal = VisitCalendarPageFold()
        val scaled = VisitCalendarPageFold()
        listOf(true, false).forEach { forward ->
            normal.update(256f, 302f, .5f, forward, 1f)
            scaled.update(768f, 906f, .5f, forward, 3f)
            assertEquals(normal.reflectedX(200f, 250f) * 3, scaled.reflectedX(600f, 750f), .001f)
            assertEquals(normal.reflectedY(200f, 250f) * 3, scaled.reflectedY(600f, 750f), .001f)
        }
        normal.update(0f, 0f, .5f, true, 1f)
        assertEquals(0, normal.writePolygon(FloatArray(12), folded = true))
        assertEquals(0, normal.writePolygon(FloatArray(12), folded = false))
    }
}
