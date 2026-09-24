package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SealStampSessionTest {

    private val ink = 0xFF2B4C7EL

    @Test
    fun oneStamp_createsExactlyOneSeal_evenIfContactFiresTwice() {
        val session = SealStampSession()
        session.startAiming(SealType.HEART, ink, Offset(100f, 120f))
        assertTrue(session.beginStamp())

        val first = session.takeContactSeal(baseSealSidePx = 50f)
        val second = session.takeContactSeal(baseSealSidePx = 50f)

        assertNotNull(first)
        assertNull(second)
        assertEquals(first!!.id, session.finish())
    }

    @Test
    fun repeatedStampTaps_startOnlyOneHand() {
        val session = SealStampSession()
        session.startAiming(SealType.STAR, ink, Offset(10f, 10f))

        assertTrue(session.beginStamp())
        assertFalse(session.beginStamp())
        assertFalse(session.beginStamp())
        // 손이 움직이는 중에는 새 조준도, 취소도 끼어들지 못한다.
        assertFalse(session.startAiming(SealType.HEART, ink, Offset.Zero))
        assertFalse(session.cancelAiming())
        assertTrue(session.isStamping)
    }

    @Test
    fun cancelWhileAiming_createsNothing() {
        val session = SealStampSession()
        session.startAiming(SealType.AIR_MAIL, ink, Offset(40f, 40f))

        assertTrue(session.cancelAiming())

        assertEquals(SealStampPhase.Idle, session.phase)
        assertNull(session.takeContactSeal(baseSealSidePx = 90f))
        assertFalse(session.beginStamp())
    }

    @Test
    fun finish_clearsTransientState() {
        val session = SealStampSession()
        session.startAiming(SealType.CIRCLE_POSTMARK, ink, Offset(0f, 0f))
        session.beginStamp()
        session.takeContactSeal(baseSealSidePx = 90f)

        session.finish()

        assertEquals(SealStampPhase.Idle, session.phase)
        assertFalse(session.isAiming)
        assertFalse(session.isStamping)
    }

    @Test
    fun stampedSeal_isCenteredOnAimPoint_withChosenTypeColorAndDefaultScale() {
        val session = SealStampSession()
        session.startAiming(SealType.CIRCLE_POSTMARK, ink, Offset(0f, 0f))
        session.moveAim(Offset(200f, 150f))
        session.beginStamp()
        // 손이 출발한 뒤의 조준 이동·회전은 무시된다(찍히는 자리가 흔들리지 않음).
        session.moveAim(Offset(5f, 5f))
        session.transformAim(zoom = 2f, rotationChange = 30f)

        val seal = session.takeContactSeal(baseSealSidePx = 60f)!!

        assertEquals(Offset(170f, 120f), seal.offset)
        assertEquals(SealType.CIRCLE_POSTMARK, seal.type)
        assertEquals(ink, seal.colorArgb)
        assertEquals(SealType.CIRCLE_POSTMARK.defaultScale, seal.scale)
        assertEquals(0f, seal.rotationDegrees)
    }

    @Test
    fun aimedScaleAndRotation_carryIntoStampedSeal_andCenterStaysOnAimPoint() {
        val session = SealStampSession()
        session.startAiming(SealType.DOG_PAW, ink, Offset(200f, 150f))
        session.transformAim(zoom = 2f, rotationChange = 25f)
        session.transformAim(zoom = 1f, rotationChange = 200f)
        session.beginStamp()

        val seal = session.takeContactSeal(baseSealSidePx = 100f)!!

        val expectedScale = SealType.DOG_PAW.defaultScale * 2f
        assertEquals(expectedScale, seal.scale, 0.0001f)
        // 25 + 200 = 225° → 같은 방향의 -135°로 정규화.
        assertEquals(-135f, seal.rotationDegrees, 0.0001f)
        val side = 100f * expectedScale
        assertEquals(200f - side / 2f, seal.offset!!.x, 0.001f)
        assertEquals(150f - side / 2f, seal.offset!!.y, 0.001f)
    }

    @Test
    fun aimScale_isClampedToSealEditRange() {
        val session = SealStampSession()
        session.startAiming(SealType.CIRCLE_POSTMARK, ink, Offset.Zero)

        session.transformAim(zoom = 100f, rotationChange = 0f)
        assertEquals(3f, (session.phase as SealStampPhase.Aiming).scale)

        session.transformAim(zoom = 0.001f, rotationChange = 0f)
        assertEquals(0.5f, (session.phase as SealStampPhase.Aiming).scale)
    }

    @Test
    fun previewSealId_isTheStampedSealId_soInkWearPreviewMatches() {
        val session = SealStampSession()
        session.startAiming(SealType.HEART, ink, Offset(50f, 50f))
        val previewId = (session.phase as SealStampPhase.Aiming).sealId
        session.beginStamp()

        val seal = session.takeContactSeal(baseSealSidePx = 90f)!!

        assertEquals(previewId, seal.id)
    }

    @Test
    fun stampedSeal_serializesLikeAnyOtherSeal_noTransientFields() {
        val session = SealStampSession()
        session.startAiming(SealType.WAVE_CANCEL, ink, Offset(80f, 80f))
        session.beginStamp()
        val seal = session.takeContactSeal(baseSealSidePx = 90f)!!

        val restored = deserializePostcardSealItem(seal.serialize())

        assertEquals(seal, restored)
    }

    @Test
    fun aimPoint_isClampedInsidePostcard() {
        val size = IntSize(300, 300)

        assertEquals(Offset(0f, 300f), clampStampCenter(Offset(-20f, 999f), size))
        assertEquals(Offset(150f, 10f), clampStampCenter(Offset(150f, 10f), size))
    }
}
