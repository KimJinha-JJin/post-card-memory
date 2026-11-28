package com.postcardmemory.utils

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 146일차 B: 기본(DEFAULT) 사진 스티커는 편집 화면에서 둥근 모서리로 사진만 잘라 보여준다(7/4
 * `7f7e647`에서 검은 3dp 테두리 제거). 저장·공유 이미지에도 같은 모습이 나와야 한다 — 예전에는 저장
 * 쪽에만 검은 테두리가 남아 있었다. 실제 Android Canvas(Robolectric native graphics)로 그려 확인한다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, application = Application::class)
class PhotoStickerDefaultExportTest {

    private val background = Color.rgb(200, 220, 240)
    private val photo = Color.rgb(250, 240, 210)

    private fun renderDefaultSticker(): Bitmap {
        val output = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
        output.eraseColor(background)
        val sticker = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        sticker.eraseColor(photo)
        val bounds = RectF(20f, 20f, 220f, 220f)
        PostcardImageExporter.drawDefaultPhotoSticker(
            canvas = Canvas(output),
            bitmap = sticker,
            stickerBounds = bounds,
            cornerRadius = bounds.width() * 16f / 120f
        )
        sticker.recycle()
        return output
    }

    private fun assertNear(expected: Int, actual: Int, label: String) {
        val diff = maxOf(
            kotlin.math.abs(Color.red(expected) - Color.red(actual)),
            kotlin.math.abs(Color.green(expected) - Color.green(actual)),
            kotlin.math.abs(Color.blue(expected) - Color.blue(actual))
        )
        assertTrue("$label: 기대 #${Integer.toHexString(expected)} 실제 #${Integer.toHexString(actual)}", diff <= 4)
    }

    @Test
    fun defaultSticker_edgeShowsPhotoNotBlackBorder() {
        val out = renderDefaultSticker()
        // 테두리가 있었다면 굵기 200×3/120 = 5px 검은 띠가 놓였을 자리(네 변의 안쪽 1~4px).
        listOf(120 to 21, 120 to 23, 21 to 120, 23 to 120, 218 to 120, 120 to 218).forEach { (x, y) ->
            assertNear(photo, out.getPixel(x, y), "가장자리($x,$y)는 사진")
        }
        assertNear(photo, out.getPixel(120, 120), "가운데는 사진")
        out.recycle()
    }

    @Test
    fun defaultSticker_keepsRoundedCornerClipAndBounds() {
        val out = renderDefaultSticker()
        // 둥근 모서리(반경 약 26.7px) 바깥은 엽서 바탕 그대로.
        assertNear(background, out.getPixel(21, 21), "왼쪽 위 모서리 바깥")
        assertNear(background, out.getPixel(218, 218), "오른쪽 아래 모서리 바깥")
        // 칸 밖은 건드리지 않음.
        assertNear(background, out.getPixel(10, 120), "칸 왼쪽 바깥")
        assertNear(background, out.getPixel(230, 120), "칸 오른쪽 바깥")
        out.recycle()
    }

    @Test
    fun exporter_defaultStickerHasNoBorderStroke_matchingEditScreen() {
        val exporter = File(
            listOf("src/main", "app/src/main").first { File(it).exists() },
            "java/com/postcardmemory/utils/PostcardImageExporter.kt"
        ).readText()
        val fn = exporter.substringAfter("internal fun drawDefaultPhotoSticker(")
            .substringBefore("\n    }\n")
        assertFalse("기본 스티커 저장에 외곽선 없음", fn.contains("Paint.Style.STROKE"))
        assertFalse("검은 테두리 상수 제거", exporter.contains("STICKER_BORDER_WIDTH_RATIO"))
        assertEquals("모서리 비율은 화면 기준(16dp/120dp) 유지", true, exporter.contains("16f / 120f"))
    }
}
