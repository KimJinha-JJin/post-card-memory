package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 87일차 후속 버그 수정: 누끼 스티커는 투명 여백이 아니라 보이는 부분 기준으로 엽서 안에
 * 가둔다(예전에는 원본 사진 칸 전체가 경계라 보이는 부분을 가장자리까지 못 옮겼다).
 */
class PhotoStickerCutoutBoundsTest {

    private val postcard = IntSize(1000, 800)
    private val side = 200
    private val sticker = IntSize(side, side)
    private val pad = side * sqrt(2f) * 0.5f / PHOTO_STICKER_CUTOUT_GRID

    // 칸 가운데 작은 피사체(한 변의 ±10%).
    private val smallCenterSubject =
        listOf(Offset(-0.1f, -0.1f), Offset(0.1f, -0.1f), Offset(-0.1f, 0.1f), Offset(0.1f, 0.1f))

    // 칸 왼쪽 위 구석에만 있는 피사체.
    private val cornerSubject =
        listOf(Offset(-0.45f, -0.45f), Offset(-0.3f, -0.45f), Offset(-0.45f, -0.3f))

    private fun tight(
        offset: Offset,
        silhouette: List<Offset>,
        rotation: Float = 0f,
        flipHorizontal: Boolean = false
    ) = clampPhotoStickerOffset(
        offset = offset,
        postcardSize = postcard,
        stickerSize = sticker,
        isBackgroundRemoved = true,
        cutoutSilhouette = silhouette,
        rotationDegrees = rotation,
        flipHorizontal = flipHorizontal
    )

    private fun loose(offset: Offset, rotation: Float = 0f) =
        clampPhotoStickerOffset(
            offset = offset,
            postcardSize = postcard,
            stickerSize = sticker,
            isBackgroundRemoved = true,
            cutoutSilhouette = null,
            rotationDegrees = rotation
        )

    @Test
    fun defaultSticker_keepsTheWholeSquareInside_asBefore() {
        val far = Offset(-500f, 5000f)

        assertEquals(
            clampStickerOffset(far, postcard, sticker),
            clampPhotoStickerOffset(far, postcard, sticker, false, smallCenterSubject, 30f)
        )
    }

    @Test
    fun cutout_canMoveUntilTheVisiblePartTouchesTheEdge_transparentMarginGoesOutside() {
        val topLeft = tight(Offset(-500f, -500f), smallCenterSubject)
        val bottomRight = tight(Offset(5000f, 5000f), smallCenterSubject)

        // 보이는 부분(중심 ±20px + 표본 여유)이 가장자리에 닿는 자리.
        val reach = 20f + pad
        assertEquals(-100f + reach, topLeft.x, 0.01f)
        assertEquals(-100f + reach, topLeft.y, 0.01f)
        assertEquals(1000f - 100f - reach, bottomRight.x, 0.01f)
        assertEquals(800f - 100f - reach, bottomRight.y, 0.01f)
        // 예전 규칙(칸 전체)이라면 0에서 멈췄을 자리보다 더 나간다.
        assertTrue(topLeft.x < 0f && bottomRight.x > 800f)
    }

    @Test
    fun cutout_insideRange_isNotMoved() {
        val offset = Offset(300f, 250f)

        assertEquals(offset, tight(offset, smallCenterSubject))
    }

    @Test
    fun cutout_rotatedAndOffCenter_neverPushesAVisiblePointOutside() {
        listOf(0f, 45f, -120f, 180f).forEach { rotation ->
            listOf(Offset(-900f, -900f), Offset(2000f, 2000f), Offset(-900f, 2000f)).forEach { drag ->
                val offset = tight(drag, cornerSubject, rotation)
                val center = offset + Offset(side / 2f, side / 2f)
                val radians = Math.toRadians(rotation.toDouble())
                val c = cos(radians).toFloat()
                val s = sin(radians).toFloat()
                cornerSubject.forEach { p ->
                    val x = center.x + (p.x * c - p.y * s) * side
                    val y = center.y + (p.x * s + p.y * c) * side
                    assertTrue("rotation=$rotation x=$x", x in 0f..1000f)
                    assertTrue("rotation=$rotation y=$y", y in 0f..800f)
                }
            }
        }
    }

    @Test
    fun cutout_flippedHorizontally_usesTheMirroredVisiblePart() {
        // 왼쪽 구석 피사체를 좌우 뒤집으면 오른쪽 구석에 보인다 → 왼쪽으로 더 멀리 갈 수 있다.
        val plain = tight(Offset(-900f, 300f), cornerSubject)
        val flipped = tight(Offset(-900f, 300f), cornerSubject, flipHorizontal = true)

        assertEquals(-100f + 90f + pad, plain.x, 0.01f)
        assertEquals(-100f - 60f + pad, flipped.x, 0.01f)
    }

    @Test
    fun looseRule_neverPullsBackAPlaceTheTightRuleAllowed() {
        listOf(smallCenterSubject, cornerSubject).forEach { subject ->
            listOf(0f, 45f, 135f, -90f).forEach { rotation ->
                listOf(
                    Offset(-900f, -900f),
                    Offset(2000f, 2000f),
                    Offset(-900f, 2000f),
                    Offset(2000f, -900f)
                ).forEach { drag ->
                    val placed = tight(drag, subject, rotation)
                    assertEquals(placed, loose(placed, rotation))
                }
            }
        }
    }

    @Test
    fun cutoutWithoutSamples_usesTheLooseRule() {
        val far = Offset(-5000f, 5000f)
        val reach = side * CUTOUT_STICKER_LOOSE_REACH

        val clamped = clampPhotoStickerOffset(far, postcard, sticker, true, emptyList(), 0f)

        assertEquals(-100f - reach, clamped.x, 0.01f)
        assertEquals(800f - 100f + reach, clamped.y, 0.01f)
        assertEquals(clamped, loose(far))
    }

    @Test
    fun visibleExtent_isNullWithoutSamples() {
        assertNull(cutoutStickerVisibleExtent(emptyList(), 200f, 0f))
    }

    @Test
    fun aimingACutout_usesTheSameVisiblePartRule_andRestoringTheOriginalPullsItBackIn() {
        val base = 100f
        val session = PhotoStickerPlaceSession<String>().also {
            it.startAiming("a", postcard, base)
        }
        val request = session.toggleAimBackground()!!
        session.applyAimBackgroundRemoval(request.stickerId, "a-cut", smallCenterSubject)

        session.moveAim(Offset(-500f, 400f), postcard, base)
        val cutoutCenter = (session.phase as PhotoStickerPlacePhase.Aiming<String>).center
        val halfPad = 100f * sqrt(2f) * 0.5f / PHOTO_STICKER_CUTOUT_GRID
        assertEquals(10f + halfPad, cutoutCenter.x, 0.01f)

        // 원본복원하면 칸 전체가 보이므로 칸 기준으로 다시 들어온다.
        session.toggleAimBackground(postcard, base)
        assertEquals(50f, (session.phase as PhotoStickerPlacePhase.Aiming<String>).center.x, 0.01f)
    }
}
