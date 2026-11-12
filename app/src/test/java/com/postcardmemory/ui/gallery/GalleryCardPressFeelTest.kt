package com.postcardmemory.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 130일차 B: 갤러리 엽서 눌림·집어 듦(B안) 순수 계산. 실제 그리기·터치 감각은 실기기 QA로 본다.
 */
class GalleryCardPressFeelTest {

    private val eps = 1e-5f

    @Test
    fun rest_isIdenticalToBefore() {
        // 쉬는 상태는 기존 그리드 그림과 같아야 한다(크기 1, 이동·기울기·그림자 없음).
        assertEquals(GalleryCardPressVisual(1f, 0f, 0f, 0f, 0f), galleryCardPressVisual(0f, 0f, 1f, -1f))
    }

    @Test
    fun fullPress_sinksAndShrinksByChosenAmounts() {
        val v = galleryCardPressVisual(press = 1f, pick = 0f, nx = 0f, ny = 0f)
        assertEquals(0.97f, v.scale, eps)
        assertEquals(2f, v.translationYDp, eps)
        assertEquals(0f, v.shadowElevationDp, eps)
    }

    @Test
    fun press_lowersTheCornerUnderTheFinger_upToTwoAndHalfDegrees() {
        // 오른쪽 위 모서리를 누르면 윗변(rotationX+)과 오른쪽 변(rotationY+)이 내려간다.
        val topRight = galleryCardPressVisual(press = 1f, pick = 0f, nx = 1f, ny = -1f)
        assertEquals(2.5f, topRight.rotationXDeg, eps)
        assertEquals(2.5f, topRight.rotationYDeg, eps)
        val bottomLeft = galleryCardPressVisual(press = 1f, pick = 0f, nx = -1f, ny = 1f)
        assertEquals(-2.5f, bottomLeft.rotationXDeg, eps)
        assertEquals(-2.5f, bottomLeft.rotationYDeg, eps)
        val half = galleryCardPressVisual(press = 0.5f, pick = 0f, nx = 1f, ny = 0f)
        assertEquals(1.25f, half.rotationYDeg, eps)
    }

    @Test
    fun pick_liftsWithShadow() {
        val v = galleryCardPressVisual(press = 0f, pick = 1f, nx = 0f, ny = 0f)
        assertEquals(1.03f, v.scale, eps)
        assertEquals(-4f, v.translationYDp, eps)
        assertEquals(6f, v.shadowElevationDp, eps)
    }

    @Test
    fun outOfRangeInputs_areClamped() {
        assertEquals(galleryCardPressVisual(1f, 1f, 0f, 0f), galleryCardPressVisual(3f, 9f, 0f, 0f))
        assertEquals(galleryCardPressVisual(0f, 0f, 0f, 0f), galleryCardPressVisual(-1f, -1f, 0f, 0f))
    }

    @Test
    fun pressDirection_isNormalizedFromCardCenter() {
        assertEquals(-1f to -1f, galleryCardPressDirection(0f, 0f, 100f, 120f))
        assertEquals(0f to 0f, galleryCardPressDirection(50f, 60f, 100f, 120f))
        assertEquals(1f to 1f, galleryCardPressDirection(100f, 120f, 100f, 120f))
        // 카드 밖 좌표는 가장자리로 묶는다.
        assertEquals(1f to -1f, galleryCardPressDirection(500f, -40f, 100f, 120f))
    }

    @Test
    fun pressDirection_unknownPositionOrEmptySize_doesNotTilt() {
        // 키보드·접근성 클릭은 위치가 없다(Offset.Unspecified = NaN).
        assertEquals(0f to 0f, galleryCardPressDirection(Float.NaN, Float.NaN, 100f, 100f))
        assertEquals(0f to 0f, galleryCardPressDirection(10f, 10f, 0f, 100f))
    }
}
