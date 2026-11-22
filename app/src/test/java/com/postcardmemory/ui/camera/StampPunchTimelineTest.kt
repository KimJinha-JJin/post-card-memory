package com.postcardmemory.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StampPunchTimelineTest {

    @Test
    fun `C안 시간표는 138일차 채택값 그대로다`() {
        with(StampPunchTimeline) {
            assertEquals(1_900, TOTAL_MS)
            assertEquals(540, CONTACT_MS)
            assertEquals(560, CUT_MS)
            assertEquals(600, PRESS_END_MS)
            assertEquals(860, RISE_END_MS)
            assertEquals(1_060, LIFT_START_MS)
            assertEquals(1_360, LIFT_END_MS)
            assertEquals(1_660, SETTLE_END_MS)
            assertEquals(5f, LIFT_HEIGHT, 0f)
        }
    }

    @Test
    fun `절단 전에는 종이 한 장이고 조각이 움직이지 않는다`() {
        for (t in listOf(0f, 250f, 539f, 559f)) {
            val s = stampPunchStateAt(t)
            assertFalse("t=$t", s.cut)
            assertEquals("t=$t", 0f, s.lift, 0f)
            assertEquals("t=$t", 0f, s.seam, 0f)
        }
    }

    @Test
    fun `절단 순간부터 구멍과 조각이 생기고 들리기 전까지 조각은 제자리다`() {
        for (t in listOf(560f, 600f, 860f, 1_059f)) {
            val s = stampPunchStateAt(t)
            assertTrue("t=$t", s.cut)
            assertEquals("t=$t", 0f, s.lift, 0f)
            assertEquals("t=$t", 1f, s.seam, 0f)
        }
    }

    @Test
    fun `펀칭기는 접촉 때 종이 윗면에 닿고 눌린 뒤 상승 끝에 화면 밖으로 빠진다`() {
        assertEquals(StampPunchTimeline.DIE_START_HEIGHT, stampPunchStateAt(0f).dieBottom, 0f)
        assertEquals(StampPunchTimeline.PAPER_TOP, stampPunchStateAt(540f).dieBottom, 1e-4f)
        assertEquals(
            StampPunchTimeline.PAPER_TOP - StampPunchTimeline.BITE,
            stampPunchStateAt(599f).dieBottom,
            1e-3f
        )
        assertEquals(StampPunchTimeline.DIE_EXIT_HEIGHT, stampPunchStateAt(860f).dieBottom, 1e-4f)
    }

    @Test
    fun `조각은 들림 구간에서만 올라가고 정착 뒤 높이 5에 머문다`() {
        val mid = stampPunchStateAt(1_210f).lift
        assertTrue(mid > 0f && mid < StampPunchTimeline.LIFT_HEIGHT)
        assertEquals(StampPunchTimeline.LIFT_HEIGHT, stampPunchStateAt(1_360f).lift, 1e-4f)
        assertEquals(StampPunchTimeline.LIFT_HEIGHT, stampPunchStateAt(1_900f).lift, 1e-4f)
        assertEquals(0f, stampPunchStateAt(1_900f).seam, 0f)
        assertEquals(StampPunchPhase.END, stampPunchStateAt(1_900f).phase)
    }

    @Test
    fun `갤러리 복귀는 저장 성공에서만 그리고 같은 저장에 한 번만 허용된다`() {
        val gate = SuccessExitGate()
        assertFalse(gate.tryClaim(CaptureState.Saving))
        assertFalse(gate.tryClaim(CaptureState.Error("실패")))
        assertFalse(gate.tryClaim(CaptureState.Idle))

        val success = CaptureState.Success("/stamps/a.jpg")
        assertTrue(gate.tryClaim(success))
        assertFalse(gate.tryClaim(success))
        assertFalse(gate.tryClaim(CaptureState.Success("/stamps/a.jpg")))
    }
}
