package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 87일차 사진 스티커 "핀셋으로 톡" 붙이기의 상태 흐름.
 * android.net.Uri는 순수 JUnit에서 만들 수 없어 원본은 문자열로 대신한다 —
 * 세션은 원본 값을 옮기기만 하고 해석하지 않는다.
 */
class PhotoStickerPlaceSessionTest {

    private val postcard = IntSize(1000, 800)
    private val base = 100f

    private fun aimingSession(source: String = "a"): PhotoStickerPlaceSession<String> =
        PhotoStickerPlaceSession<String>().also {
            it.startAiming(source, postcard, base)
        }

    private fun aim(session: PhotoStickerPlaceSession<String>) =
        session.phase as PhotoStickerPlacePhase.Aiming<String>

    @Test
    fun startAiming_beginsAtPostcardCenterWithDefaultScaleAndNoRotation() {
        val session = aimingSession()

        val aim = aim(session)
        assertEquals(Offset(500f, 400f), aim.center)
        assertEquals(1f, aim.scale, 0f)
        assertEquals(0f, aim.rotationDegrees, 0f)
    }

    @Test
    fun onePlacement_createsExactlyOneSticker_evenIfContactFiresTwice() {
        val session = aimingSession()
        assertTrue(session.beginPlace())

        val first = session.takeContactPlacement(base)
        val second = session.takeContactPlacement(base)

        assertNotNull(first)
        assertNull(second)
        assertEquals(first!!.stickerId, session.finish())
        assertTrue(session.phase is PhotoStickerPlacePhase.Idle)
    }

    @Test
    fun repeatedPlaceTaps_startOnlyOneHand_andNothingCutsIn() {
        val session = aimingSession()

        assertTrue(session.beginPlace())
        assertFalse(session.beginPlace())
        assertFalse(session.beginPlace())
        // 손이 움직이는 중에는 취소·새 조준·조작·배경제거가 끼어들지 못한다.
        assertEquals(emptyList<String>(), session.cancelAiming())
        assertNull(session.toggleAimBackground())
        assertEquals(listOf("b"), session.startAiming("b", postcard, base))
        val before = session.phase
        session.moveAim(Offset(10f, 10f), postcard, base)
        session.transformAim(2f, 30f, postcard, base)
        assertEquals(before, session.phase)
    }

    @Test
    fun contactBeforePlace_createsNothing() {
        val session = aimingSession()

        assertNull(session.takeContactPlacement(base))
        assertNull(PhotoStickerPlaceSession<String>().takeContactPlacement(base))
    }

    @Test
    fun cancelWhileAiming_returnsSource_andCreatesNothing() {
        val session = aimingSession("picked")

        assertEquals(listOf("picked"), session.cancelAiming())
        assertTrue(session.phase is PhotoStickerPlacePhase.Idle)
        assertFalse(session.beginPlace())
        assertNull(session.takeContactPlacement(base))
        assertNull(session.finish())
    }

    @Test
    fun placement_keepsAimedPositionScaleAndRotation() {
        val session = aimingSession("src")
        session.moveAim(Offset(300f, 250f), postcard, base)
        session.transformAim(zoom = 1.5f, rotationChange = -20f, postcard, base)
        val aimed = aim(session)

        session.beginPlace()
        val placement = session.takeContactPlacement(base)!!

        val side = base * 1.5f
        assertEquals("src", placement.source)
        assertEquals(aimed.stickerId, placement.stickerId)
        assertEquals(1.5f, placement.scale, 0.0001f)
        assertEquals(-20f, placement.rotationDegrees, 0.0001f)
        // offset은 스티커 사각형 좌상단 — 그 중심이 조준한 자리와 같다.
        assertEquals(300f, placement.offset.x + side / 2f, 0.0001f)
        assertEquals(250f, placement.offset.y + side / 2f, 0.0001f)
    }

    @Test
    fun aimCenter_staysInsidePostcard_likePlacedStickerClamp() {
        val session = aimingSession()

        session.moveAim(Offset(-500f, 5000f), postcard, base)
        assertEquals(Offset(50f, 750f), aim(session).center)

        // 모서리에서 키우면 커진 크기 기준으로 다시 안쪽으로 들어온다.
        session.transformAim(zoom = 2f, rotationChange = 0f, postcard, base)
        assertEquals(Offset(100f, 700f), aim(session).center)

        session.beginPlace()
        val placement = session.takeContactPlacement(base)!!
        val side = (base * 2f).toInt()
        assertEquals(
            placement.offset,
            clampStickerOffset(placement.offset, postcard, IntSize(side, side))
        )
    }

    @Test
    fun aimScale_isLimitedToPlacedStickerRange() {
        val session = aimingSession()

        session.transformAim(zoom = 100f, rotationChange = 0f, postcard, base)
        assertEquals(PHOTO_STICKER_PLACE_MAX_SCALE, aim(session).scale, 0f)

        session.transformAim(zoom = 0.0001f, rotationChange = 0f, postcard, base)
        assertEquals(PHOTO_STICKER_PLACE_MIN_SCALE, aim(session).scale, 0f)
    }

    @Test
    fun aimRotation_isNormalizedLikePlacedSticker() {
        val session = aimingSession()

        session.transformAim(zoom = 1f, rotationChange = 200f, postcard, base)

        assertEquals(-160f, aim(session).rotationDegrees, 0.0001f)
    }

    @Test
    fun newPhotoWhileAiming_replacesAim_andReturnsPreviousSource() {
        val session = aimingSession("first")
        val firstId = aim(session).stickerId

        val abandoned = session.startAiming("second", postcard, base)

        assertEquals(listOf("first"), abandoned)
        assertEquals("second", aim(session).source)
        assertFalse(firstId == aim(session).stickerId)
    }

    @Test
    fun abandon_returnsOnlyFilesThatNeverBecameStickers() {
        assertEquals(listOf("a"), aimingSession("a").abandon())

        val flying = aimingSession("b").also { it.beginPlace() }
        assertEquals(listOf("b"), flying.abandon())

        val cutFlying = aimingSession("b2").also { it.removeBackground("b2-cut") }
        cutFlying.beginPlace()
        assertEquals(listOf("b2", "b2-cut"), cutFlying.abandon())

        val placed = aimingSession("c").also {
            it.removeBackground("c-cut")
            it.beginPlace()
            it.takeContactPlacement(base)
        }
        assertEquals(emptyList<String>(), placed.abandon())
        assertTrue(placed.phase is PhotoStickerPlacePhase.Idle)
    }

    private val silhouette = listOf(Offset(0f, 0.2f))

    private fun PhotoStickerPlaceSession<String>.removeBackground(result: String) {
        val request = toggleAimBackground()!!
        assertNull(applyAimBackgroundRemoval(request.stickerId, result, silhouette))
    }

    @Test
    fun aimBackgroundRemoval_showsCutout_andPlacesAsBackgroundRemovedSticker() {
        val session = aimingSession("orig")

        val request = session.toggleAimBackground()
        assertEquals("orig", request!!.source)
        assertTrue(aim(session).isRemovingBackground)
        // 처리 중에는 다시 누르거나 붙일 수 없다.
        assertNull(session.toggleAimBackground())
        assertFalse(session.beginPlace())

        assertNull(session.applyAimBackgroundRemoval(request.stickerId, "cut", silhouette))
        assertEquals("cut", aim(session).displayedSource)
        assertEquals(silhouette, aim(session).activeCutoutSilhouette)

        assertTrue(session.beginPlace())
        val placement = session.takeContactPlacement(base)!!
        assertEquals("orig", placement.source)
        assertEquals("cut", placement.removedBgSource)
        assertTrue(placement.isBackgroundRemoved)
    }

    @Test
    fun aimBackgroundToggleOff_keepsResultForReuse_andPlacesOriginalLook() {
        val session = aimingSession("orig")
        session.removeBackground("cut")

        // 원본복원: 결과는 들고 있고 다시 켤 때 새로 돌리지 않는다.
        assertNull(session.toggleAimBackground())
        assertEquals("orig", aim(session).displayedSource)
        assertNull(aim(session).activeCutoutSilhouette)
        assertNull(session.toggleAimBackground())
        assertEquals("cut", aim(session).displayedSource)
        session.toggleAimBackground()

        session.beginPlace()
        val placement = session.takeContactPlacement(base)!!
        assertFalse(placement.isBackgroundRemoved)
        assertEquals("cut", placement.removedBgSource)
    }

    @Test
    fun lateBackgroundResult_afterCancelOrNewPhoto_isReturnedForCleanup() {
        val cancelled = aimingSession("a")
        val request = cancelled.toggleAimBackground()!!
        assertEquals(listOf("a"), cancelled.cancelAiming())
        assertEquals("a-cut", cancelled.applyAimBackgroundRemoval(request.stickerId, "a-cut", silhouette))

        val replaced = aimingSession("b")
        val oldRequest = replaced.toggleAimBackground()!!
        replaced.startAiming("c", postcard, base)
        assertEquals("b-cut", replaced.applyAimBackgroundRemoval(oldRequest.stickerId, "b-cut", silhouette))
        assertEquals("c", aim(replaced).source)
        assertFalse(aim(replaced).isBackgroundRemoved)
    }

    @Test
    fun cancelOrReplace_afterBackgroundRemoval_returnsOriginalAndCutout() {
        val cancelled = aimingSession("a").also { it.removeBackground("a-cut") }
        assertEquals(listOf("a", "a-cut"), cancelled.cancelAiming())

        val replaced = aimingSession("b").also { it.removeBackground("b-cut") }
        assertEquals(listOf("b", "b-cut"), replaced.startAiming("c", postcard, base))
    }

    @Test
    fun backgroundRemovalFailure_clearsProcessing_andAllowsRetryOrPlace() {
        val session = aimingSession("a")
        val request = session.toggleAimBackground()!!

        session.failAimBackgroundRemoval(request.stickerId)

        assertFalse(aim(session).isRemovingBackground)
        assertFalse(aim(session).isBackgroundRemoved)
        assertNotNull(session.toggleAimBackground())
        session.failAimBackgroundRemoval(request.stickerId)
        assertTrue(session.beginPlace())
        assertFalse(session.takeContactPlacement(base)!!.isBackgroundRemoved)
    }

    @Test
    fun cutoutSilhouette_mapsOpaqueCellsIntoFittedSquare() {
        // 가로로 긴 200×100 그림: 긴 변 기준으로 칸에 맞춰져 세로는 ±0.25 안에 든다.
        val points =
            photoStickerCutoutSilhouette(width = 200, height = 100, grid = 4) { x, y ->
                x >= 100 && y >= 50
            }

        assertEquals(4, points.size)
        points.forEach {
            assertTrue(it.x > 0f && it.x < 0.5f)
            assertTrue(it.y > 0f && it.y < 0.25f)
        }
        assertEquals(
            emptyList<Offset>(),
            photoStickerCutoutSilhouette(width = 0, height = 10) { _, _ -> true }
        )
    }

    @Test
    fun gripOffset_forCutout_isLowestOpaquePointAfterRotation() {
        val points = listOf(Offset(-0.3f, 0.1f), Offset(0.2f, 0.4f))

        val upright = photoStickerGripOffset(points, stickerSidePx = 100f, rotationDegrees = 0f, gripInsetPx = 2f)
        assertEquals(20f, upright.x, 0.001f)
        assertEquals(38f, upright.y, 0.001f)

        // 180도 돌리면 두 점이 (30, -10)·(-20, -40)이 되고, 더 아래인 (30, -10)을 집는다 —
        // 그림이 모두 중심 위에 있어도 허공(중심)이 아니라 그림 끝을 문다.
        val flipped = photoStickerGripOffset(points, stickerSidePx = 100f, rotationDegrees = 180f, gripInsetPx = 2f)
        assertEquals(30f, flipped.x, 0.001f)
        assertEquals(-12f, flipped.y, 0.001f)
    }

    @Test
    fun gripOffset_withoutCutout_isLowerEdgeCenter() {
        assertEquals(
            Offset(0f, 58f),
            photoStickerGripOffset(null, stickerSidePx = 120f, rotationDegrees = 35f, gripInsetPx = 2f)
        )
        assertEquals(
            Offset(0f, 58f),
            photoStickerGripOffset(emptyList(), stickerSidePx = 120f, rotationDegrees = 0f, gripInsetPx = 2f)
        )
    }

    @Test
    fun gripDistance_reachesLowerEdgeMinusInset_andNeverGoesNegative() {
        assertEquals(58f, photoStickerGripDistance(stickerSidePx = 120f, gripInsetPx = 2f), 0f)
        assertEquals(0f, photoStickerGripDistance(stickerSidePx = 2f, gripInsetPx = 5f), 0f)
    }
}
