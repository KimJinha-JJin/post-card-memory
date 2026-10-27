package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 110일차: 스티커 통에서 꺼낸 조각처럼 누끼가 이미 있는 사진으로 조준을 시작할 때.
 * 배경제거를 다시 돌리지 않고, 붙이면 그 누끼가 스티커에 그대로 들어가며, 버린 조준은
 * 원본과 누끼 복사본을 둘 다 정리 대상으로 돌려준다.
 */
class PhotoStickerPlaceSessionStashTest {

    private val postcard = IntSize(1000, 800)
    private val base = 100f
    private val silhouette = listOf(Offset(0.5f, 0.9f))

    private fun presetSession(isBackgroundRemoved: Boolean = true) =
        PhotoStickerPlaceSession<String>().also {
            it.startAiming(
                source = "orig",
                postcardSize = postcard,
                baseStickerSidePx = base,
                removedBgSource = "cut",
                isBackgroundRemoved = isBackgroundRemoved,
                cutoutSilhouette = silhouette
            )
        }

    private fun aim(session: PhotoStickerPlaceSession<String>) =
        session.phase as PhotoStickerPlacePhase.Aiming<String>

    @Test
    fun presetCutout_isShownAndPlacedWithoutNewBackgroundRemoval() {
        val session = presetSession()

        assertEquals("cut", aim(session).displayedSource)
        assertEquals(silhouette, aim(session).activeCutoutSilhouette)
        assertTrue(session.beginPlace())
        val placement = session.takeContactPlacement(base)!!
        assertEquals("orig", placement.source)
        assertEquals("cut", placement.removedBgSource)
        assertTrue(placement.isBackgroundRemoved)
    }

    @Test
    fun presetCutout_togglesWithoutRequestingRemoval() {
        val session = presetSession()

        assertNull(session.toggleAimBackground(postcard, base))
        assertEquals("orig", aim(session).displayedSource)
        assertNull(session.toggleAimBackground(postcard, base))
        assertEquals("cut", aim(session).displayedSource)
    }

    @Test
    fun presetCutoutKeptOff_startsWithOriginal() {
        val session = presetSession(isBackgroundRemoved = false)

        assertEquals("orig", aim(session).displayedSource)
        assertFalse(aim(session).isBackgroundRemoved)
    }

    @Test
    fun flagWithoutCutout_isIgnored() {
        val session = PhotoStickerPlaceSession<String>()
        session.startAiming("orig", postcard, base, removedBgSource = null, isBackgroundRemoved = true, cutoutSilhouette = silhouette)

        assertFalse(aim(session).isBackgroundRemoved)
        assertNull(aim(session).cutoutSilhouette)
    }

    @Test
    fun cancelledPresetAim_returnsBothCopiesForCleanup() {
        val session = presetSession()

        assertEquals(listOf("orig", "cut"), session.cancelAiming())
    }

    @Test
    fun presetDuringPlacing_isRejectedWithBothCopies() {
        val session = presetSession()
        session.beginPlace()

        val rejected = session.startAiming("orig2", postcard, base, removedBgSource = "cut2", isBackgroundRemoved = true)

        assertEquals(listOf("orig2", "cut2"), rejected)
    }

    @Test
    fun replacingAPresetAim_returnsThePreviousCopies() {
        val session = presetSession()

        val replaced = session.startAiming("next", postcard, base)

        assertEquals(listOf("orig", "cut"), replaced)
    }
}
