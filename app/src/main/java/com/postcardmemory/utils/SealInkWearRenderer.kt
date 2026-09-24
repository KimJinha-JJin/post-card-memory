package com.postcardmemory.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import com.postcardmemory.ui.detail.SealInkWear
import java.nio.ByteBuffer

/**
 * 도장 잉크 결손을 실제로 지우는 단 하나의 경로. 화면(SealPreviewContent)과
 * 저장 이미지(PostcardImageExporter)가 둘 다 [createEraseMask]로 같은 결손
 * 지도를 만들고 [erase]로 같은 방식(쌍선형 확대 + DST_OUT)으로 지우므로,
 * 편집 화면과 저장·공유 이미지의 질감이 갈라지지 않는다.
 *
 * 호출자는 이미 도장 도형을 별도 레이어(saveLayer / Offscreen)에 그려 둔
 * 상태여야 한다 — DST_OUT이 도장 잉크만 지우고 아래 엽서 사진은 건드리지
 * 않게 하기 위해서다.
 */
object SealInkWearRenderer {

    /** 결손 지도를 알파 전용 비트맵으로 만든다. 알파 = 지울 잉크의 양. */
    fun createEraseMask(wear: SealInkWear): Bitmap {
        val size = wear.resolution
        val bytes = ByteArray(size * size)
        for (index in bytes.indices) {
            bytes[index] =
                (wear.erase[index].coerceIn(0f, 1f) * 255f + 0.5f)
                    .toInt()
                    .toByte()
        }
        return Bitmap.createBitmap(size, size, Bitmap.Config.ALPHA_8).apply {
            copyPixelsFromBuffer(ByteBuffer.wrap(bytes))
        }
    }

    fun erase(
        canvas: Canvas,
        eraseMask: Bitmap,
        left: Float,
        top: Float,
        side: Float
    ) {
        if (side <= 0f || eraseMask.isRecycled) return

        val erasePaint =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                color = 0xFF000000.toInt()
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            }

        canvas.drawBitmap(
            eraseMask,
            null,
            RectF(left, top, left + side, top + side),
            erasePaint
        )
    }
}
