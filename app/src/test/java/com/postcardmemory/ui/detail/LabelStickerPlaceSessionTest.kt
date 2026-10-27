package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 87일차 후속: 라벨 스티커 "핀셋으로 톡" 붙이기의 상태 흐름과 핀셋이 집는 자리. */
class LabelStickerPlaceSessionTest {

    private val postcard = IntSize(1000, 800)
    private val labelSize = IntSize(200, 60)
    private val draft =
        LabelStickerItem(
            id = "label-1",
            text = "HELLO",
            style = LabelTapeStyle.BLACK
        )

    private fun aimingSession(): LabelStickerPlaceSession =
        LabelStickerPlaceSession().also {
            assertTrue(it.startAiming(draft, postcard, labelSize))
        }

    private fun aim(session: LabelStickerPlaceSession) =
        session.phase as LabelStickerPlacePhase.Aiming

    @Test
    fun startAiming_beginsAtPostcardCenterWithoutRotation() {
        val session = aimingSession()

        assertEquals(Offset(500f, 400f), aim(session).center)
        assertEquals(0f, aim(session).rotationDegrees, 0f)
    }

    @Test
    fun onePlacement_createsExactlyOneLabel_withAimedPositionAndRotation() {
        val session = aimingSession()
        session.moveAim(Offset(300f, 200f), postcard, labelSize)
        session.rotateAim(-15f)

        assertTrue(session.beginPlace())
        val first = session.takeContactLabel(labelSize)
        val second = session.takeContactLabel(labelSize)

        assertNotNull(first)
        assertNull(second)
        // 문구·종류·id는 그대로, 자리는 조준 중심이 라벨 중심이 되도록.
        assertEquals("label-1", first!!.id)
        assertEquals("HELLO", first.text)
        assertEquals(LabelTapeStyle.BLACK, first.style)
        assertEquals(Offset(200f, 170f), first.offset)
        assertEquals(-15f, first.rotationDegrees, 0.0001f)
        assertEquals(1f, first.scale, 0f)
        assertEquals("label-1", session.finish())
    }

    @Test
    fun repeatedPlaceTaps_startOnlyOneHand_andNothingCutsIn() {
        val session = aimingSession()

        assertTrue(session.beginPlace())
        assertFalse(session.beginPlace())
        assertFalse(session.cancelAiming())
        assertFalse(session.startAiming(draft.copy(id = "other"), postcard, labelSize))
        val before = session.phase
        session.moveAim(Offset.Zero, postcard, labelSize)
        session.rotateAim(40f)
        assertEquals(before, session.phase)
    }

    @Test
    fun cancelWhileAiming_createsNothing() {
        val session = aimingSession()

        assertTrue(session.cancelAiming())
        assertFalse(session.beginPlace())
        assertNull(session.takeContactLabel(labelSize))
        assertNull(session.finish())
    }

    @Test
    fun contactBeforePlace_createsNothing() {
        assertNull(aimingSession().takeContactLabel(labelSize))
    }

    @Test
    fun aimCenter_keepsWholeLabelInsidePostcard() {
        val session = aimingSession()

        session.moveAim(Offset(-100f, 5000f), postcard, labelSize)

        assertEquals(Offset(100f, 770f), aim(session).center)
    }

    @Test
    fun aimRotation_isNormalized() {
        val session = aimingSession()

        session.rotateAim(270f)

        assertEquals(-90f, aim(session).rotationDegrees, 0.0001f)
    }

    @Test
    fun rectGrip_upright_isBottomEdgeCenterInset() {
        val grip = rectStickerGripOffset(200f, 60f, rotationDegrees = 0f, gripInsetPx = 2f)

        assertEquals(0f, grip.x, 0.001f)
        assertEquals(28f, grip.y, 0.001f)
    }

    @Test
    fun rectGrip_rotatedSteeply_movesToTheSideEdgeThatFacesDown() {
        // 시계방향 90도: 오른쪽 변(+x)이 화면 아래를 향한다.
        val grip = rectStickerGripOffset(200f, 60f, rotationDegrees = 90f, gripInsetPx = 2f)

        assertEquals(0f, grip.x, 0.001f)
        assertEquals(98f, grip.y, 0.001f)
    }

    @Test
    fun rectGrip_slightTilt_staysOnBottomEdge() {
        val grip = rectStickerGripOffset(200f, 60f, rotationDegrees = 10f, gripInsetPx = 0f)

        // 아래 변 가운데 (0, 30)을 10도 돌린 자리.
        assertEquals(-30f * kotlin.math.sin(Math.toRadians(10.0)).toFloat(), grip.x, 0.001f)
        assertEquals(30f * kotlin.math.cos(Math.toRadians(10.0)).toFloat(), grip.y, 0.001f)
    }
}
