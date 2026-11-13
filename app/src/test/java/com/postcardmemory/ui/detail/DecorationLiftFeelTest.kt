package com.postcardmemory.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 131일차: 텍스트·라벨·테이프 들림·기울기·밀착 순수 계산. 실제 그리기·손맛은 실기기 QA로 본다.
 * 그리기 전용(저장·Undo·내보내기 무관)인 것은 [DecorationLiftStructureTest]가 지킨다.
 */
class DecorationLiftFeelTest {

    private val eps = 1e-5f
    private val specs = listOf(TextStickerLiftSpec, LabelStickerLiftSpec, MaskingTapeLiftSpec)

    @Test
    fun rest_drawsExactlyLikeBefore_forEveryMaterial() {
        for (spec in specs) {
            val v = decorationLiftVisual(0f, spec)
            assertEquals(1f, v.scale, eps)
            assertEquals(0f, v.liftDp, eps)
            assertEquals(0f, v.shadowAlpha, eps)
            assertEquals(0f, v.shadowBlurDp, eps)
        }
    }

    @Test
    fun fullLift_usesChosenAmounts() {
        decorationLiftVisual(1f, TextStickerLiftSpec).let {
            assertEquals(1.03f, it.scale, eps)
            assertEquals(3f, it.liftDp, eps)
            assertEquals(6f, it.shadowOffsetDp, eps)
            assertEquals(6f, it.shadowBlurDp, eps)
            assertEquals(0.20f, it.shadowAlpha, eps)
        }
        decorationLiftVisual(1f, LabelStickerLiftSpec).let {
            assertEquals(1.025f, it.scale, eps)
            assertEquals(3f, it.liftDp, eps)
            assertEquals(4f, it.shadowOffsetDp, eps)
            assertEquals(3f, it.shadowBlurDp, eps)
            assertEquals(0.28f, it.shadowAlpha, eps)
        }
        decorationLiftVisual(1f, MaskingTapeLiftSpec).let {
            assertEquals(1.015f, it.scale, eps)
            assertEquals(1.5f, it.liftDp, eps)
            assertEquals(2.5f, it.shadowOffsetDp, eps)
            assertEquals(2.5f, it.shadowBlurDp, eps)
            assertEquals(0.14f, it.shadowAlpha, eps)
        }
    }

    @Test
    fun materials_keepTheirRelativeCharacter() {
        // 테이프는 가장 낮게·옅게, 라벨은 단단해서 그림자가 짧고 또렷, 글자는 가장 부드럽게 퍼진다.
        assertTrue(MaskingTapeLiftSpec.liftDp < TextStickerLiftSpec.liftDp)
        assertTrue(MaskingTapeLiftSpec.shadowAlpha < TextStickerLiftSpec.shadowAlpha)
        assertTrue(MaskingTapeLiftSpec.tiltMaxDeg < LabelStickerLiftSpec.tiltMaxDeg)
        assertTrue(LabelStickerLiftSpec.shadowBlurDp < TextStickerLiftSpec.shadowBlurDp)
        assertTrue(LabelStickerLiftSpec.settleLift < TextStickerLiftSpec.settleLift)
        // 사진 스티커(4dp·±3°)보다 모두 가볍다.
        for (spec in specs) {
            assertTrue(spec.liftDp < PHOTO_STICKER_LIFT_DP)
            assertTrue(spec.tiltMaxDeg < PHOTO_STICKER_TILT_MAX_DEG)
            assertTrue(spec.settleAtMs < spec.dropMs)
        }
    }

    @Test
    fun settle_pressesOnceWithoutShadowOrLift_andIsBounded() {
        for (spec in specs) {
            val v = decorationLiftVisual(spec.settleLift, spec)
            assertEquals(1f + spec.liftScale * spec.settleLift, v.scale, eps)
            assertTrue(v.scale < 1f)
            assertEquals(0f, v.liftDp, eps)
            assertEquals(0f, v.shadowAlpha, eps)
            assertEquals(v, decorationLiftVisual(-5f, spec))
        }
    }

    @Test
    fun reducedMotion_keepsShadowCueButNoMovement() {
        for (spec in specs) {
            val v = decorationLiftVisual(1f, spec, reduceMotion = true)
            assertEquals(1f, v.scale, eps)
            assertEquals(0f, v.liftDp, eps)
            assertEquals(spec.shadowAlpha, v.shadowAlpha, eps)
        }
    }

    @Test
    fun invalidLift_fallsBackToRest() {
        for (spec in specs) {
            assertEquals(decorationLiftVisual(0f, spec), decorationLiftVisual(Float.NaN, spec))
            assertEquals(decorationLiftVisual(1f, spec), decorationLiftVisual(9f, spec))
        }
    }

    @Test
    fun dragTilt_isProportionalAndCappedPerMaterial() {
        assertEquals(0.75f, decorationDragTiltTarget(0.5f, TextStickerLiftSpec), eps)
        assertEquals(2f, decorationDragTiltTarget(10f, TextStickerLiftSpec), eps)
        assertEquals(-2f, decorationDragTiltTarget(-10f, LabelStickerLiftSpec), eps)
        assertEquals(1.2f, decorationDragTiltTarget(10f, MaskingTapeLiftSpec), eps)
        assertEquals(0f, decorationDragTiltTarget(Float.NaN, MaskingTapeLiftSpec), eps)
    }

    @Test
    fun shadowDrop_staysScreenDownWhateverTheRotation() {
        screenDownOffsetInRotatedSpace(5f, 0f).let {
            assertEquals(0f, it.x, eps); assertEquals(5f, it.y, eps)
        }
        // 시계 방향 90° 돌아간 내용에서 화면 아래 = 로컬 +x.
        screenDownOffsetInRotatedSpace(5f, 90f).let {
            assertEquals(5f, it.x, 1e-4f); assertEquals(0f, it.y, 1e-4f)
        }
        screenDownOffsetInRotatedSpace(5f, 180f).let {
            assertEquals(0f, it.x, 1e-4f); assertEquals(-5f, it.y, 1e-4f)
        }
        // 로컬 → 화면으로 다시 돌리면 (0, d)가 된다(DetailScreen.localStickerDeltaToParent와 같은 회전식).
        for (deg in listOf(-135f, -30f, 17f, 75f, 200f)) {
            val local = screenDownOffsetInRotatedSpace(7f, deg)
            val r = Math.toRadians(deg.toDouble())
            val screenX = (local.x * kotlin.math.cos(r) - local.y * kotlin.math.sin(r)).toFloat()
            val screenY = (local.x * kotlin.math.sin(r) + local.y * kotlin.math.cos(r)).toFloat()
            assertEquals(0f, screenX, 1e-3f)
            assertEquals(7f, screenY, 1e-3f)
        }
        assertEquals(0f, screenDownOffsetInRotatedSpace(Float.NaN, 10f).y, eps)
    }
}
