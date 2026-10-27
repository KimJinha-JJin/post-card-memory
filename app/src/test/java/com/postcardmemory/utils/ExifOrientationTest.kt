package com.postcardmemory.utils

import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 촬영 사진 자르기·스티커 내보내기·API 28 미만 렌더링이 함께 쓰는 EXIF 방향표.
 * 실제 비트맵 회전은 Android 런타임이 필요해 `ExifOrientationInstrumentedTest`(계측)가
 * 예전 행렬과 픽셀 단위로 비교하고, 여기서는 표 자체를 고정한다.
 */
class ExifOrientationTest {

    @Test
    fun everyRotatingOrFlippingOrientationMapsToItsTransform() {
        val expected = mapOf(
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to ExifOrientationTransform(0f, true),
            ExifInterface.ORIENTATION_ROTATE_180 to ExifOrientationTransform(180f, false),
            ExifInterface.ORIENTATION_FLIP_VERTICAL to ExifOrientationTransform(180f, true),
            ExifInterface.ORIENTATION_TRANSPOSE to ExifOrientationTransform(90f, true),
            ExifInterface.ORIENTATION_ROTATE_90 to ExifOrientationTransform(90f, false),
            ExifInterface.ORIENTATION_TRANSVERSE to ExifOrientationTransform(270f, true),
            ExifInterface.ORIENTATION_ROTATE_270 to ExifOrientationTransform(270f, false)
        )
        expected.forEach { (orientation, transform) ->
            assertEquals("orientation=$orientation", transform, exifOrientationTransform(orientation))
        }
    }

    @Test
    fun normalMissingAndInvalidOrientationsLeaveThePhotoUntouched() {
        // EXIF가 없으면 호출부가 ORIENTATION_NORMAL(1)을 기본값으로 읽는다.
        listOf(ExifInterface.ORIENTATION_NORMAL, ExifInterface.ORIENTATION_UNDEFINED, -1, 9, 100)
            .forEach { orientation ->
                assertNull("orientation=$orientation", exifOrientationTransform(orientation))
            }
    }

    @Test
    fun onlyQuarterTurnsSwapWidthAndHeight() {
        // ImageUtils의 원본 크기 계산이 쓰던 목록(ROTATE_90/270, TRANSPOSE, TRANSVERSE)과 같아야 한다.
        val swapping = setOf(
            ExifInterface.ORIENTATION_ROTATE_90,
            ExifInterface.ORIENTATION_ROTATE_270,
            ExifInterface.ORIENTATION_TRANSPOSE,
            ExifInterface.ORIENTATION_TRANSVERSE
        )
        (-1..9).forEach { orientation ->
            val swaps = exifOrientationTransform(orientation)?.swapsWidthAndHeight == true
            if (orientation in swapping) {
                assertTrue("orientation=$orientation", swaps)
            } else {
                assertFalse("orientation=$orientation", swaps)
            }
        }
    }
}
