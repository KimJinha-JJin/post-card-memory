package com.postcardmemory.utils

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import androidx.core.graphics.createBitmap

/**
 * 잡지 오림 사진 스티커의 인쇄 망점을 그린다. 편집 미리보기
 * (Modifier.photoStickerPaper, nativeCanvas)와 저장·공유 이미지
 * (PostcardImageExporter.drawPaperStyledSticker)가 이 함수 하나를 같이 쓴다.
 *
 * 점 하나짜리 작은 알파 타일을 BitmapShader로 반복하고, 스티커 한 변에 비례한
 * 간격·각도·원점만 행렬로 바꾼다 — 제스처 중에도 bitmap을 새로 만들지 않는다.
 */
object PhotoStickerHalftoneRenderer {

    private const val TILE_SIZE = 64
    /** 망점 반지름(간격 대비). 희미한 망점이 되도록 작게. */
    private const val DOT_RADIUS_RATIO = 0.26f

    private val dotTile: Bitmap by lazy {
        createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ALPHA_8).also { tile ->
            Canvas(tile).drawCircle(
                TILE_SIZE / 2f,
                TILE_SIZE / 2f,
                TILE_SIZE * DOT_RADIUS_RATIO,
                Paint(Paint.ANTI_ALIAS_FLAG)
            )
        }
    }

    /**
     * [clip] 안에 망점을 깐다. 원점은 스티커 좌상단([originX], [originY])이고
     * 간격은 [pitch] × [side]라 크기가 바뀌어도 같은 무늬가 비례해서 그려진다.
     */
    fun draw(
        canvas: Canvas,
        clip: Path,
        originX: Float,
        originY: Float,
        side: Float,
        pitch: Float,
        angleDegrees: Float,
        inkArgb: Long,
        alpha: Float
    ) {
        val pitchPx = pitch * side
        // 너무 작아 뭉개질 크기에서는 그리지 않는다.
        if (pitchPx < 2f || alpha <= 0f) return

        val shader =
            BitmapShader(dotTile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT).apply {
                setLocalMatrix(
                    Matrix().apply {
                        setScale(pitchPx / TILE_SIZE, pitchPx / TILE_SIZE)
                        postRotate(angleDegrees)
                        postTranslate(originX, originY)
                    }
                )
            }
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.shader = shader
                color = inkArgb.toInt()
                this.alpha = (alpha.coerceIn(0f, 1f) * 255).toInt()
            }

        canvas.save()
        try {
            canvas.clipPath(clip)
            canvas.drawPaint(paint)
        } finally {
            canvas.restore()
        }
    }
}
