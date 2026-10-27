package com.postcardmemory.utils

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.postcardmemory.ui.detail.LABEL_STICKER_CORNER_RADIUS_RATIO
import com.postcardmemory.ui.detail.LABEL_STICKER_CUT_BAND_RATIO
import com.postcardmemory.ui.detail.LABEL_STICKER_EMBOSS_HIGHLIGHT_ARGB
import com.postcardmemory.ui.detail.LABEL_STICKER_EMBOSS_OFFSET_RATIO
import com.postcardmemory.ui.detail.LABEL_STICKER_EMBOSS_SHADOW_ARGB
import com.postcardmemory.ui.detail.LABEL_STICKER_LETTER_SPACING_EM
import com.postcardmemory.ui.detail.LABEL_STICKER_SHEEN_Y_RATIO
import com.postcardmemory.ui.detail.LabelTapePalette
import com.postcardmemory.ui.detail.labelStickerHeightPx
import com.postcardmemory.ui.detail.labelStickerWidthPx
import com.postcardmemory.ui.detail.scaleLabelTapeRgb

/**
 * 라벨 스티커를 실제로 그리는 단 하나의 경로. 미리보기(Compose의
 * LabelStickerContent)와 저장 이미지(PostcardImageExporter)가 둘 다 이
 * object의 [labelWidthPx]/[draw]를 호출하므로, 마스킹테이프·텍스트
 * 스티커처럼 "같은 절차를 두 군데에 각각 옮겨 적는" 방식이 아니라 계산식
 * 자체가 하나뿐이다 — 화면과 공유 결과가 어긋날 여지가 구조적으로 없다.
 *
 * android.graphics만 쓰고 Compose에 의존하지 않아 두 호출자 모두에서
 * 그대로 쓸 수 있다.
 */
object LabelStickerRenderer {

    /**
     * 기계식 라벨 느낌을 내는 고정 스타일. 새 폰트 파일을 들여오지 않고
     * 시스템 MONOSPACE BOLD + 벌어진 자간만으로, 세리프 계열인 텍스트
     * 스티커(TextStickerContent)와 분명히 다른 글자꼴을 만든다.
     */
    fun createTextPaint(fontSizePx: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = fontSizePx
            letterSpacing = LABEL_STICKER_LETTER_SPACING_EM
            textAlign = Paint.Align.LEFT
            style = Paint.Style.FILL
        }

    /** 문구 길이에 따라 자동으로 정해지는 테이프 가로 길이. */
    fun labelWidthPx(
        text: String,
        fontSizePx: Float,
        textPaint: Paint = createTextPaint(fontSizePx)
    ): Float =
        labelStickerWidthPx(
            measuredTextWidthPx = textPaint.measureText(text),
            fontSizePx = fontSizePx
        )

    /** 테이프 세로 두께. 문구와 무관하게 고정이라 Paint가 필요 없다. */
    fun labelHeightPx(fontSizePx: Float): Float =
        labelStickerHeightPx(fontSizePx)

    /**
     * (left, top)을 좌상단으로 라벨 한 조각을 그린다. 회전은 호출자가
     * canvas.rotate로 감싸서 처리한다(오브젝트마다 회전 중심 기준이 달라
     * 여기서 정하지 않는다).
     */
    fun draw(
        canvas: Canvas,
        text: String,
        palette: LabelTapePalette,
        fontSizePx: Float,
        left: Float,
        top: Float,
        textPaint: Paint = createTextPaint(fontSizePx)
    ) {
        if (fontSizePx <= 0f) return

        val width = labelWidthPx(text, fontSizePx, textPaint)
        val height = labelHeightPx(fontSizePx)
        val right = left + width
        val bottom = top + height
        val bounds = RectF(left, top, right, bottom)
        val radius = fontSizePx * LABEL_STICKER_CORNER_RADIUS_RATIO

        drawTapeSurface(canvas, bounds, radius, palette, fontSizePx)
        drawEmbossedText(canvas, bounds, text, palette, fontSizePx, textPaint)
    }

    /**
     * 플라스틱 테이프의 물성 — 가운데가 살짝 밝은 세로 그라데이션, 잘린
     * 양 끝의 단면 음영, 표면을 스치는 얇은 하이라이트. 전부 고정 비율이라
     * 매번 같은 모양이 재현된다(랜덤 요소 없음).
     *
     * 라벨을 둘러싸는 명시적인 외곽선은 두지 않는다 — 테두리로 형태를
     * 설명하면 버튼·chip처럼 읽혀서, 빛과 표면 차이만으로 형태가 드러나게
     * 한다. 형태가 흐려 보인다면 새 테두리나 그림자를 더하는 대신 여기
     * 그라데이션·단면 음영의 강도를 조정한다.
     */
    private fun drawTapeSurface(
        canvas: Canvas,
        bounds: RectF,
        radius: Float,
        palette: LabelTapePalette,
        fontSizePx: Float
    ) {
        val surfacePaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    bounds.left,
                    bounds.top,
                    bounds.left,
                    bounds.bottom,
                    intArrayOf(
                        scaleRgb(palette.baseColorArgb, 0.88f),
                        scaleRgb(palette.baseColorArgb, 1.14f),
                        palette.baseColorArgb.toInt(),
                        scaleRgb(palette.baseColorArgb, 0.82f)
                    ),
                    floatArrayOf(0f, 0.3f, 0.62f, 1f),
                    Shader.TileMode.CLAMP
                )
            }

        canvas.drawRoundRect(bounds, radius, radius, surfacePaint)

        val clipPath = Path().apply {
            addRoundRect(bounds, radius, radius, Path.Direction.CW)
        }

        canvas.save()
        canvas.clipPath(clipPath)

        drawCutEndBands(canvas, bounds, palette, fontSizePx)

        // 표면을 스치는 아주 얇은 반사. 디지털 UI의 균일한 면이 아니라
        // 빛을 받는 플라스틱처럼 보이게 하는 최소 장치다.
        val sheenPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x24FFFFFF
            }
        val sheenY = bounds.top + bounds.height() * LABEL_STICKER_SHEEN_Y_RATIO
        val sheenInset = fontSizePx * 0.35f
        canvas.drawRect(
            bounds.left + sheenInset,
            sheenY,
            bounds.right - sheenInset,
            sheenY + fontSizePx * 0.055f,
            sheenPaint
        )

        canvas.restore()
    }

    /**
     * 양 끝에 안쪽으로 흐려지는 음영대를 깔아 "잘라낸 테이프 단면"처럼
     * 보이게 한다. 딱 떨어지는 선 하나를 긋는 것보다 두께가 있는 물건처럼
     * 읽히고, path 계산을 복잡하게 만들지 않는다.
     */
    private fun drawCutEndBands(
        canvas: Canvas,
        bounds: RectF,
        palette: LabelTapePalette,
        fontSizePx: Float
    ) {
        val bandWidth = fontSizePx * LABEL_STICKER_CUT_BAND_RATIO
        if (bandWidth <= 0f) return

        val bandColor = withAlpha(palette.edgeColorArgb, 0x5C)
        val transparent = withAlpha(palette.edgeColorArgb, 0x00)

        val leftPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    bounds.left,
                    bounds.top,
                    bounds.left + bandWidth,
                    bounds.top,
                    bandColor,
                    transparent,
                    Shader.TileMode.CLAMP
                )
            }
        canvas.drawRect(
            bounds.left,
            bounds.top,
            bounds.left + bandWidth,
            bounds.bottom,
            leftPaint
        )

        val rightPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    bounds.right,
                    bounds.top,
                    bounds.right - bandWidth,
                    bounds.top,
                    bandColor,
                    transparent,
                    Shader.TileMode.CLAMP
                )
            }
        canvas.drawRect(
            bounds.right - bandWidth,
            bounds.top,
            bounds.right,
            bounds.bottom,
            rightPaint
        )
    }

    /**
     * 눌려 올라온 문자. 밝은 면 → 반대쪽 음영 → 본 색 순서로 세 번만
     * 그린다(다중 blur나 필터를 쓰지 않는다). 세로 위치는 글리프 경계가
     * 아니라 폰트 metrics로 잡아, 영문 대문자든 한글이든 같은 자리에
     * 놓이고 위아래가 잘리지 않는다.
     */
    private fun drawEmbossedText(
        canvas: Canvas,
        bounds: RectF,
        text: String,
        palette: LabelTapePalette,
        fontSizePx: Float,
        textPaint: Paint
    ) {
        if (text.isEmpty()) return

        val textWidth = textPaint.measureText(text)
        val originX = bounds.left + (bounds.width() - textWidth) / 2f

        val metrics = textPaint.fontMetrics
        val baselineY =
            bounds.top +
                bounds.height() / 2f -
                (metrics.ascent + metrics.descent) / 2f

        val offset = fontSizePx * LABEL_STICKER_EMBOSS_OFFSET_RATIO

        textPaint.color = LABEL_STICKER_EMBOSS_HIGHLIGHT_ARGB.toInt()
        canvas.drawText(text, originX - offset, baselineY - offset, textPaint)

        textPaint.color = LABEL_STICKER_EMBOSS_SHADOW_ARGB.toInt()
        canvas.drawText(text, originX + offset, baselineY + offset, textPaint)

        textPaint.color = palette.textColorArgb.toInt()
        canvas.drawText(text, originX, baselineY, textPaint)
    }

    /** 기타 색상 테이프의 단면색을 만들 때와 같은 식을 쓴다(scaleLabelTapeRgb 한 곳에만 존재). */
    private fun scaleRgb(colorArgb: Long, factor: Float): Int =
        scaleLabelTapeRgb(colorArgb, factor).toInt()

    private fun withAlpha(colorArgb: Long, alpha: Int): Int =
        ((alpha and 0xFF) shl 24) or (colorArgb.toInt() and 0x00FFFFFF)
}
