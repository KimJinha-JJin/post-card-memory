package com.postcardmemory

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.postcardmemory.utils.rotateBitmapForExifOrientation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 107일차에 하나로 모은 EXIF 회전이 예전 세 벌(ImageUtils·PostcardImageExporter·
 * PostcardRenderSpec에 똑같이 있던 행렬)과 픽셀까지 같은 결과를 내는지 실제 Bitmap으로 비교한다.
 * 메모리 안의 작은 비트맵만 쓰고 파일·DB·앱 데이터를 건드리지 않는 테스트다.
 */
class ExifOrientationInstrumentedTest {

    @Test
    fun sharedRotationMatchesTheLegacyMatrixForEveryOrientation() {
        (-1..9).forEach { orientation ->
            val source = markedBitmap()
            val expected = legacyRotate(source, orientation)
            val actual = rotateBitmapForExifOrientation(source, orientation)

            if (expected === source) {
                assertSame("orientation=$orientation 은 원본을 그대로 돌려줘야 함", source, actual)
            } else {
                assertEquals("orientation=$orientation width", expected.width, actual.width)
                assertEquals("orientation=$orientation height", expected.height, actual.height)
                assertTrue("orientation=$orientation 픽셀", expected.sameAs(actual))
                // 원본 recycle은 호출부 몫이다 — 공용 함수는 원본을 건드리지 않는다.
                assertTrue("orientation=$orientation 원본 유지", !source.isRecycled)
            }
        }
    }

    /** 위치마다 색이 다른 3×2 비트맵 — 회전·반전이 틀리면 픽셀 비교가 바로 달라진다. */
    private fun markedBitmap(): Bitmap =
        Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.RED)
            setPixel(1, 0, Color.GREEN)
            setPixel(2, 0, Color.BLUE)
            setPixel(0, 1, Color.YELLOW)
            setPixel(1, 1, Color.CYAN)
            setPixel(2, 1, Color.MAGENTA)
        }

    /** 107일차 이전 세 helper의 행렬 계산 그대로(recycle 제외). */
    private fun legacyRotate(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
