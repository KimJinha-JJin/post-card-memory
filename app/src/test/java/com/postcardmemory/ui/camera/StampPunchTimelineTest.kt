package com.postcardmemory.ui.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StampPunchTimelineTest {

    @Test
    fun `C안 시간표는 140일차 속도 조정값이고 순서는 그대로다`() {
        with(StampPunchTimeline) {
            assertEquals(2_800, TOTAL_MS)
            assertEquals(950, CONTACT_MS)
            assertEquals(1_050, CUT_MS)
            assertEquals(1_180, PRESS_END_MS)
            assertEquals(1_800, RISE_END_MS)
            assertEquals(1_900, LIFT_START_MS)
            assertEquals(2_350, LIFT_END_MS)
            assertEquals(2_800, SETTLE_END_MS)
            assertEquals(5f, LIFT_HEIGHT, 0f)
            val order = listOf(
                DESCENT_START_MS, CONTACT_MS, CUT_MS, PRESS_END_MS,
                RISE_END_MS, LIFT_START_MS, LIFT_END_MS
            )
            assertEquals(order.sorted(), order)
            assertTrue(SETTLE_END_MS <= TOTAL_MS)
        }
    }

    @Test
    fun `하강은 종이에 가까울수록 느려지고 상승은 천천히 떨어져 나간다`() {
        with(StampPunchTimeline) {
            // 접촉 직전 100ms 동안 내려오는 거리 < 하강 한가운데 100ms 동안 내려오는 거리
            val mid = (DESCENT_START_MS + CONTACT_MS) / 2f
            val midDrop = stampPunchStateAt(mid - 50f).dieBottom - stampPunchStateAt(mid + 50f).dieBottom
            val endDrop = stampPunchStateAt(CONTACT_MS - 100f).dieBottom - stampPunchStateAt(CONTACT_MS - 1f).dieBottom
            assertTrue("mid=$midDrop end=$endDrop", endDrop < midDrop * 0.5f)
            // 상승 첫 100ms 동안 오르는 거리 < 상승 한가운데 100ms 동안 오르는 거리
            val riseMid = (PRESS_END_MS + RISE_END_MS) / 2f
            val startRise = stampPunchStateAt(PRESS_END_MS + 100f).dieBottom - stampPunchStateAt(PRESS_END_MS.toFloat()).dieBottom
            val midRise = stampPunchStateAt(riseMid + 50f).dieBottom - stampPunchStateAt(riseMid - 50f).dieBottom
            assertTrue("start=$startRise mid=$midRise", startRise < midRise * 0.5f)
        }
    }

    @Test
    fun `절단 전에는 종이 한 장이고 조각이 움직이지 않는다`() {
        for (t in listOf(0f, 120f, 949f, 1_049f)) {
            val s = stampPunchStateAt(t)
            assertFalse("t=$t", s.cut)
            assertEquals("t=$t", 0f, s.lift, 0f)
            assertEquals("t=$t", 0f, s.seam, 0f)
        }
    }

    @Test
    fun `절단 순간부터 구멍과 조각이 생기고 들리기 전까지 조각은 제자리다`() {
        for (t in listOf(1_050f, 1_180f, 1_800f, 1_899f)) {
            val s = stampPunchStateAt(t)
            assertTrue("t=$t", s.cut)
            assertEquals("t=$t", 0f, s.lift, 0f)
            assertEquals("t=$t", 1f, s.seam, 0f)
        }
    }

    @Test
    fun `펀칭기는 접촉 때 종이 윗면에 닿고 눌린 뒤 상승 끝에 화면 밖으로 빠진다`() {
        assertEquals(StampPunchTimeline.DIE_START_HEIGHT, stampPunchStateAt(0f).dieBottom, 0f)
        assertEquals(StampPunchTimeline.PAPER_TOP, stampPunchStateAt(950f).dieBottom, 1e-4f)
        assertEquals(
            StampPunchTimeline.PAPER_TOP - StampPunchTimeline.BITE,
            stampPunchStateAt(1_179f).dieBottom,
            1e-3f
        )
        assertEquals(StampPunchTimeline.DIE_EXIT_HEIGHT, stampPunchStateAt(1_800f).dieBottom, 1e-4f)
    }

    @Test
    fun `조각은 들림 구간에서만 올라가고 정착 뒤 높이 5에 머문다`() {
        val mid = stampPunchStateAt(2_125f).lift
        assertTrue(mid > 0f && mid < StampPunchTimeline.LIFT_HEIGHT)
        assertEquals(StampPunchTimeline.LIFT_HEIGHT, stampPunchStateAt(2_350f).lift, 1e-4f)
        assertEquals(StampPunchTimeline.LIFT_HEIGHT, stampPunchStateAt(2_800f).lift, 1e-4f)
        assertEquals(0f, stampPunchStateAt(2_800f).seam, 0f)
        assertEquals(StampPunchPhase.END, stampPunchStateAt(2_800f).phase)
    }

    @Test
    fun `갤러리 복귀는 저장 성공에서만 그리고 같은 저장에 한 번만 허용된다`() {
        val gate = SuccessExitGate()
        assertFalse(gate.tryClaim(CaptureState.Saving))
        assertFalse(gate.tryClaim(CaptureState.Error("실패")))
        assertFalse(gate.tryClaim(CaptureState.Idle))

        val success = CaptureState.Success("/stamps/a.jpg", postcardId = 42L)
        assertTrue(gate.tryClaim(success))
        assertFalse(gate.tryClaim(success))
        assertFalse(gate.tryClaim(CaptureState.Success("/stamps/a.jpg", postcardId = 42L)))
    }

    @Test
    fun `펀칭 뒤 편집 화면은 방금 등록된 실제 엽서 id로만 열린다`() {
        assertEquals(42L, editablePostcardIdOrNull(42L))
        assertEquals(1L, editablePostcardIdOrNull(1L))
        // Room 자동 id는 1부터라 0·음수(비정상 insert 결과)면 편집 대신 갤러리로 돌아간다.
        assertEquals(null, editablePostcardIdOrNull(0L))
        assertEquals(null, editablePostcardIdOrNull(-1L))
    }
}
