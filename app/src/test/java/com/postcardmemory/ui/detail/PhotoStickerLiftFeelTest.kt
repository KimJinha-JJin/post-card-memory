package com.postcardmemory.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 130일차 B: 사진 스티커 들림·기울기·내려앉음(B안) 순수 계산. 실제 그리기·손맛은 실기기 QA로 본다.
 * 이 효과는 그리기 전용이라 저장·Undo·내보내기와 무관한 것은 [PhotoStickerLiftStructureTest]가 지킨다.
 */
class PhotoStickerLiftFeelTest {

    private val eps = 1e-5f

    @Test
    fun rest_drawsExactlyLikeBefore() {
        val v = photoStickerLiftVisual(0f)
        assertEquals(1f, v.scale, eps)
        assertEquals(0f, v.liftDp, eps)
        assertEquals(0f, v.shadowElevationDp, eps)
        assertEquals(0f, v.cutoutShadowAlpha, eps)
    }

    @Test
    fun fullLift_usesChosenAmounts() {
        val v = photoStickerLiftVisual(1f)
        assertEquals(1.05f, v.scale, eps)
        assertEquals(4f, v.liftDp, eps)
        assertEquals(10f, v.shadowElevationDp, eps)
        assertEquals(10f, v.cutoutShadowOffsetDp, eps)
        assertEquals(8f, v.cutoutShadowBlurDp, eps)
        assertEquals(0.22f, v.cutoutShadowAlpha, eps)
    }

    @Test
    fun settle_pressesIntoPaperOnceWithoutShadowOrLift() {
        // 내려앉을 때 한 번 눌림: 크기만 0.9925배, 들림·그림자는 없다.
        val v = photoStickerLiftVisual(PHOTO_STICKER_SETTLE_LIFT)
        assertEquals(0.9925f, v.scale, eps)
        assertEquals(0f, v.liftDp, eps)
        assertEquals(0f, v.shadowElevationDp, eps)
        // 더 깊이 눌리지 않게 묶는다.
        assertEquals(v, photoStickerLiftVisual(-5f))
    }

    @Test
    fun reducedMotion_keepsShadowCueButNoMovement() {
        val v = photoStickerLiftVisual(1f, reduceMotion = true)
        assertEquals(1f, v.scale, eps)
        assertEquals(0f, v.liftDp, eps)
        assertEquals(10f, v.shadowElevationDp, eps)
    }

    @Test
    fun invalidLift_fallsBackToRest() {
        assertEquals(photoStickerLiftVisual(0f), photoStickerLiftVisual(Float.NaN))
        assertEquals(photoStickerLiftVisual(0f), photoStickerLiftVisual(Float.POSITIVE_INFINITY))
        assertEquals(photoStickerLiftVisual(1f), photoStickerLiftVisual(7f))
    }

    @Test
    fun dragTilt_followsHorizontalSpeed_andCapsAtThreeDegrees() {
        assertEquals(0f, photoStickerDragTiltTarget(0f), eps)
        assertEquals(1f, photoStickerDragTiltTarget(0.5f), eps)
        assertEquals(-1f, photoStickerDragTiltTarget(-0.5f), eps)
        assertEquals(3f, photoStickerDragTiltTarget(4f), eps)
        assertEquals(-3f, photoStickerDragTiltTarget(-40f), eps)
        assertEquals(0f, photoStickerDragTiltTarget(Float.NaN), eps)
    }

    @Test
    fun velocitySmoothing_blendsAndIgnoresZeroTime() {
        assertEquals(0.4f, smoothPhotoStickerVelocity(0f, deltaXDp = 10f, deltaMillis = 10L), eps)
        assertEquals(0.6f + 0.4f, smoothPhotoStickerVelocity(1f, deltaXDp = 10f, deltaMillis = 10L), eps)
        assertEquals(0.7f, smoothPhotoStickerVelocity(0.7f, deltaXDp = 10f, deltaMillis = 0L), eps)
        assertEquals(0.7f, smoothPhotoStickerVelocity(0.7f, deltaXDp = Float.NaN, deltaMillis = 16L), eps)
    }

    @Test
    fun dropTiming_settlesInsideTheDrop() {
        assertTrue(PHOTO_STICKER_SETTLE_AT_MS in 1 until PHOTO_STICKER_DROP_MS)
        assertTrue(PHOTO_STICKER_SETTLE_LIFT < 0f && PHOTO_STICKER_SETTLE_LIFT > -0.5f)
    }
}
