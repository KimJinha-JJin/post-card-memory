package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 87일차 후속: 텍스트 스티커 "핀셋으로 톡" 붙이기의 상태 흐름. */
class TextStickerPlaceSessionTest {

    private val postcard = IntSize(1000, 800)
    private val textSize = IntSize(160, 50)
    private val draft = TextStickerItem(id = "text-1", text = "안녕")

    private fun aimingSession(): TextStickerPlaceSession =
        TextStickerPlaceSession().also { assertTrue(it.startAiming(draft, postcard)) }

    private fun aim(session: TextStickerPlaceSession) =
        session.phase as TextStickerPlacePhase.Aiming

    @Test
    fun startAiming_beginsAtPostcardCenterWithDefaultScaleAndNoRotation() {
        val aim = aim(aimingSession())

        assertEquals(Offset(500f, 400f), aim.center)
        assertEquals(1f, aim.scale, 0f)
        assertEquals(0f, aim.rotationDegrees, 0f)
    }

    @Test
    fun onePlacement_createsExactlyOneSticker_withAimedCenterScaleAndRotation() {
        val session = aimingSession()
        session.moveAim(Offset(300f, 200f), postcard, textSize)
        session.transformAim(zoom = 1.5f, rotationChange = 25f, postcard, textSize)

        val placedSize = IntSize(240, 75)
        assertTrue(session.beginPlace(placedSize, postcard))
        val first = session.takeContactText(placedSize)
        val second = session.takeContactText(placedSize)

        assertNotNull(first)
        assertNull(second)
        assertEquals("text-1", first!!.id)
        assertEquals("안녕", first.text)
        assertEquals(1.5f, first.scale, 0.0001f)
        assertEquals(25f, first.rotationDegrees, 0.0001f)
        // offset은 회전 전 사각형 좌상단 — 그 중심이 조준 중심.
        assertEquals(Offset(180f, 162.5f), first.offset)
        assertEquals("text-1", session.finish())
    }

    @Test
    fun repeatedPlaceTaps_startOnlyOneHand_andNothingCutsIn() {
        val session = aimingSession()

        assertTrue(session.beginPlace(textSize, postcard))
        assertFalse(session.beginPlace(textSize, postcard))
        assertFalse(session.cancelAiming())
        assertFalse(session.startAiming(draft.copy(id = "other"), postcard))
        val before = session.phase
        session.moveAim(Offset.Zero, postcard, textSize)
        session.transformAim(2f, 30f, postcard, textSize)
        assertEquals(before, session.phase)
    }

    @Test
    fun cancelOrContactBeforePlace_createsNothing() {
        val session = aimingSession()
        assertNull(session.takeContactText(textSize))

        assertTrue(session.cancelAiming())
        assertFalse(session.beginPlace(textSize, postcard))
        assertNull(session.takeContactText(textSize))
        assertNull(session.finish())
    }

    @Test
    fun aimCenter_keepsWholeTextInsidePostcard_evenAfterGrowing() {
        val session = aimingSession()

        session.moveAim(Offset(5000f, -50f), postcard, textSize)
        assertEquals(Offset(920f, 25f), aim(session).center)

        // 2배로 키우면 (320×100) 기준으로 다시 안쪽으로 들어온다.
        session.transformAim(zoom = 2f, rotationChange = 0f, postcard, textSize)
        assertEquals(Offset(840f, 50f), aim(session).center)
    }

    @Test
    fun beginPlace_reclampsWithLatestMeasuredSize_soHandTargetEqualsLanding() {
        val session = aimingSession()
        session.moveAim(Offset(990f, 400f), postcard, IntSize.Zero)

        session.beginPlace(textSize, postcard)
        val target = (session.phase as TextStickerPlacePhase.Placing).aim.center
        val placed = session.takeContactText(textSize)!!

        assertEquals(Offset(920f, 400f), target)
        assertEquals(Offset(840f, 375f), placed.offset)
        assertEquals(
            placed.offset,
            clampStickerOffset(placed.offset!!, postcard, textSize)
        )
    }

    @Test
    fun aimScale_isLimitedToPlacedTextStickerRange() {
        val session = aimingSession()

        session.transformAim(zoom = 100f, rotationChange = 0f, postcard, textSize)
        assertEquals(TEXT_STICKER_PLACE_MAX_SCALE, aim(session).scale, 0f)

        session.transformAim(zoom = 0.0001f, rotationChange = 0f, postcard, textSize)
        assertEquals(TEXT_STICKER_PLACE_MIN_SCALE, aim(session).scale, 0f)
    }
}
