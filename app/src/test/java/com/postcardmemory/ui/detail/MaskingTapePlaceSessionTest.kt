package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/** 87일차 후속: 마스킹테이프 "손으로 쓸어 붙이기"의 상태 흐름과 쓸고 가는 길. */
class MaskingTapePlaceSessionTest {

    private val postcard = IntSize(1000, 800)
    private val tapeSize = IntSize(264, 80)
    private val draft =
        MaskingTapeItem(
            id = "tape-1",
            style = MaskingTapeStyle.CUSTOM,
            lengthScale = 2f,
            thicknessScale = 2f,
            customBaseColorArgb = 0xFF112233L
        )

    private fun aimingSession(): MaskingTapePlaceSession =
        MaskingTapePlaceSession().also {
            assertTrue(it.startAiming(draft, postcard, tapeSize).isEmpty())
        }

    private fun aim(session: MaskingTapePlaceSession) =
        session.phase as MaskingTapePlacePhase.Aiming

    private fun assertOffsetEquals(expected: Offset, actual: Offset) {
        assertEquals(expected.x, actual.x, 0.001f)
        assertEquals(expected.y, actual.y, 0.001f)
    }

    @Test
    fun startAiming_beginsAtPostcardCenterWithNoRotation() {
        val aim = aim(aimingSession())

        assertEquals(Offset(500f, 400f), aim.center)
        assertEquals(0f, aim.rotationDegrees, 0f)
    }

    @Test
    fun onePlacement_createsExactlyOneTape_withAimedCenterAndRotation_keepingDesign() {
        val session = aimingSession()
        session.moveAim(Offset(300f, 200f), postcard, tapeSize)
        session.rotateAim(30f)

        assertTrue(session.beginPlace())
        val first = session.takeContactTape(tapeSize)
        val second = session.takeContactTape(tapeSize)

        assertNotNull(first)
        assertNull(second)
        assertEquals("tape-1", first!!.id)
        assertEquals(MaskingTapeStyle.CUSTOM, first.style)
        assertEquals(0xFF112233L, first.customBaseColorArgb)
        assertEquals(2f, first.lengthScale, 0f)
        assertEquals(2f, first.thicknessScale, 0f)
        assertEquals(30f, first.rotationDegrees, 0.0001f)
        // offset은 회전 전 사각형 좌상단 — 그 중심이 조준 중심.
        assertEquals(Offset(168f, 160f), first.offset)
        assertEquals("tape-1", session.finish())
    }

    @Test
    fun repeatedPlaceTaps_startOnlyOneHand_andNothingCutsIn() {
        val session = aimingSession()

        assertTrue(session.beginPlace())
        assertFalse(session.beginPlace())
        assertTrue(session.cancelAiming().isEmpty())
        val other = draft.copy(id = "other")
        assertEquals(listOf(other), session.startAiming(other, postcard, tapeSize))
        val before = session.phase
        session.moveAim(Offset.Zero, postcard, tapeSize)
        session.rotateAim(45f)
        assertEquals(before, session.phase)
    }

    @Test
    fun cancelOrContactBeforePlace_createsNothing_andHandsBackTheDraft() {
        val session = aimingSession()
        assertNull(session.takeContactTape(tapeSize))

        assertEquals(listOf("tape-1"), session.cancelAiming().map { it.id })
        assertFalse(session.beginPlace())
        assertNull(session.takeContactTape(tapeSize))
        assertNull(session.finish())
    }

    @Test
    fun startingAnotherAim_handsBackThePreviousDraft() {
        val session = aimingSession()
        val next = draft.copy(id = "tape-2")

        val leftover = session.startAiming(next, postcard, tapeSize)

        assertEquals(listOf("tape-1"), leftover.map { it.id })
        assertEquals("tape-2", aim(session).draft.id)
    }

    @Test
    fun abandon_handsBackOnlyTapesThatNeverTouchedPaper() {
        val aiming = aimingSession()
        assertEquals(listOf("tape-1"), aiming.abandon().map { it.id })
        assertEquals(MaskingTapePlacePhase.Idle, aiming.phase)

        val placingBeforeContact = aimingSession().also { it.beginPlace() }
        assertEquals(listOf("tape-1"), placingBeforeContact.abandon().map { it.id })

        val placed = aimingSession().also {
            it.beginPlace()
            it.takeContactTape(tapeSize)
        }
        assertTrue(placed.abandon().isEmpty())
    }

    @Test
    fun aimCenter_keepsWholeTapeInsidePostcard() {
        val session = aimingSession()

        session.moveAim(Offset(5000f, -50f), postcard, tapeSize)

        assertEquals(Offset(868f, 40f), aim(session).center)
        val placedOffset =
            session.run {
                beginPlace()
                takeContactTape(tapeSize)!!.offset!!
            }
        assertEquals(placedOffset, clampStickerOffset(placedOffset, postcard, tapeSize))
    }

    /** 테스트용 크기 규칙: 길이·굵기 배율에 비례(1배 = 132×40). */
    private fun sizeOf(tape: MaskingTapeItem) =
        IntSize((132 * tape.lengthScale).toInt(), (40 * tape.thicknessScale).toInt())

    @Test
    fun editAim_appliesPopupValues_andTheyReachThePlacedTape() {
        val session = aimingSession()
        session.moveAim(Offset(300f, 200f), postcard, tapeSize)

        session.editAim(
            edgeStyle = MaskingTapeEdgeStyle.STRAIGHT,
            lengthScale = 1.5f,
            thicknessScale = 0.5f,
            rotationDegrees = -45f,
            postcardSize = postcard,
            tapeSizeOf = ::sizeOf
        )
        val edited = aim(session)
        assertEquals(MaskingTapeEdgeStyle.STRAIGHT, edited.draft.edgeStyle)
        assertEquals(1.5f, edited.draft.lengthScale, 0f)
        assertEquals(0.5f, edited.draft.thicknessScale, 0f)
        assertEquals(-45f, edited.rotationDegrees, 0f)
        assertEquals(Offset(300f, 200f), edited.center)

        session.beginPlace()
        val placed = session.takeContactTape(sizeOf(edited.draft))!!
        assertEquals(MaskingTapeEdgeStyle.STRAIGHT, placed.edgeStyle)
        assertEquals(1.5f, placed.lengthScale, 0f)
        assertEquals(0.5f, placed.thicknessScale, 0f)
        assertEquals(-45f, placed.rotationDegrees, 0f)
        assertEquals(Offset(201f, 190f), placed.offset)
    }

    @Test
    fun editAim_longerTape_isPulledBackInsidePostcard_andScalesStayInEditRange() {
        val session = aimingSession()
        session.moveAim(Offset(860f, 400f), postcard, tapeSize)

        session.editAim(
            edgeStyle = MaskingTapeEdgeStyle.NOTCHED_BOTH,
            lengthScale = 10f,
            thicknessScale = 0.01f,
            rotationDegrees = 190f,
            postcardSize = postcard,
            tapeSizeOf = ::sizeOf
        )

        val edited = aim(session)
        assertEquals(MASKING_TAPE_MAX_LENGTH_SCALE, edited.draft.lengthScale, 0f)
        assertEquals(MASKING_TAPE_MIN_THICKNESS_SCALE, edited.draft.thicknessScale, 0f)
        assertEquals(-170f, edited.rotationDegrees, 0.0001f)
        // 3배 = 396px → 중심은 1000 - 198 = 802 안쪽으로.
        assertEquals(Offset(802f, 400f), edited.center)
    }

    @Test
    fun editAim_isIgnoredOnceTheHandHasStarted() {
        val session = aimingSession()
        session.beginPlace()
        val before = session.phase

        session.editAim(
            MaskingTapeEdgeStyle.STRAIGHT, 2f, 2f, 30f, postcard, ::sizeOf
        )

        assertEquals(before, session.phase)
    }

    @Test
    fun rotateAim_staysInTapeEditRange() {
        val session = aimingSession()

        session.rotateAim(170f)
        session.rotateAim(30f)

        assertEquals(-160f, aim(session).rotationDegrees, 0.0001f)
    }

    @Test
    fun sweep_followsTapeLongAxis_notTheScreenX() {
        val center = Offset(400f, 300f)
        val sweep = maskingTapeSweep(center, lengthPx = 200f, rotationDegrees = 30f, insetPx = 20f)

        val radians = Math.toRadians(30.0)
        val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
        assertOffsetEquals(center - direction * 80f, sweep.start)
        assertOffsetEquals(center + direction * 80f, sweep.end)
        assertEquals(30f, sweep.handRotationDegrees, 0.0001f)
        assertFalse(sweep.againstTapeX)
    }

    @Test
    fun sweep_unrotatedTape_goesLeftToRightOnce() {
        val sweep =
            maskingTapeSweep(Offset(400f, 300f), lengthPx = 200f, rotationDegrees = 0f, insetPx = 20f)

        assertOffsetEquals(Offset(320f, 300f), sweep.start)
        assertOffsetEquals(Offset(480f, 300f), sweep.end)
    }

    @Test
    fun sweep_pastNinetyDegrees_staysOnTheSameAxis_withoutFlippingTheHand() {
        val center = Offset(400f, 300f)
        val tilted = maskingTapeSweep(center, 200f, rotationDegrees = 150f, insetPx = 20f)
        val same = maskingTapeSweep(center, 200f, rotationDegrees = -30f, insetPx = 20f)

        assertEquals(-30f, tilted.handRotationDegrees, 0.0001f)
        assertTrue(tilted.againstTapeX)
        assertFalse(same.againstTapeX)
        assertOffsetEquals(same.start, tilted.start)
        assertOffsetEquals(same.end, tilted.end)

        // 세로 테이프는 어느 쪽으로 돌렸든 위→아래로 쓴다.
        val down = maskingTapeSweep(center, 200f, rotationDegrees = 90f, insetPx = 20f)
        val up = maskingTapeSweep(center, 200f, rotationDegrees = -90f, insetPx = 20f)
        assertOffsetEquals(Offset(400f, 220f), down.start)
        assertOffsetEquals(Offset(400f, 220f), up.start)
        assertOffsetEquals(Offset(400f, 380f), up.end)
    }

    @Test
    fun sweep_onVeryShortTape_neverRunsBackwards() {
        val sweep = maskingTapeSweep(Offset(50f, 50f), lengthPx = 10f, rotationDegrees = 0f, insetPx = 40f)

        assertEquals(5f, sweep.insetPx, 0f)
        assertOffsetEquals(Offset(50f, 50f), sweep.start)
        assertOffsetEquals(Offset(50f, 50f), sweep.end)
    }

    @Test
    fun unpressedRange_shrinksAheadOfTheHand_inTapeLocalX() {
        val forward = maskingTapeSweep(Offset.Zero, 200f, rotationDegrees = 10f, insetPx = 20f)
        assertEquals(20f..200f, maskingTapeUnpressedRange(forward, 200f, 0f))
        assertEquals(100f..200f, maskingTapeUnpressedRange(forward, 200f, 0.5f))
        assertEquals(180f..200f, maskingTapeUnpressedRange(forward, 200f, 1f))

        // 손이 테이프 +x 반대로 쓰면 앞쪽은 0 쪽이다.
        val backward = maskingTapeSweep(Offset.Zero, 200f, rotationDegrees = 170f, insetPx = 20f)
        assertEquals(0f..180f, maskingTapeUnpressedRange(backward, 200f, 0f))
        assertEquals(0f..20f, maskingTapeUnpressedRange(backward, 200f, 1f))
    }
}
