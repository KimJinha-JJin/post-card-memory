package com.postcardmemory.ui.components

import android.graphics.Paint as AndroidPaint
import android.graphics.Rect as AndroidRect
import android.graphics.Typeface as AndroidTypeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.detail.DEFAULT_TEXT_STICKER_OUTLINE_COLOR_ARGB

/** 텍스트 스티커 흰색 외곽선 두께 = 글자 크기(px)의 이 비율. */
private const val TEXT_STICKER_OUTLINE_WIDTH_RATIO = 0.14f

/** 텍스트 스티커 기본(scale=1.0) 글자 크기. */
val TEXT_STICKER_BASE_FONT_SIZE_SP = 32f

/**
 * 사용자가 입력한 문자열을, 각 글자 획을 따라 색 있는 외곽선이 둘러싼
 * 다이컷 스티커처럼 그린다. 사각형 배경이 아니라 글자 모양 자체를
 * 따르는 외곽선이어야 하므로, Compose Text 대신 android.graphics.Paint를
 * 외곽선색 STROKE로 먼저 그리고 그 위에 컬러 FILL로 다시 그리는 표준
 * 기법을 쓴다. PostcardImageExporter의 공유/내보내기 렌더도 동일한 두 겹
 * 그리기 기법을 쓰므로, 화면과 공유 결과의 외곽선이 같은 방식으로 만들어진다.
 *
 * Canvas의 크기 자체를 측정된 글자 경계 + 외곽선 두께에 정확히 맞춰서,
 * 이 컴포저블의 레이아웃 크기가 곧 실제 보이는 스티커 크기가 되게 한다
 * (제스처 히트 영역·중앙 정렬 계산이 별도 여백 없이 그대로 맞아떨어진다).
 */
@Composable
fun TextStickerContent(
    text: String,
    colorArgb: Long,
    fontSizeSp: Float,
    outlineColorArgb: Long = DEFAULT_TEXT_STICKER_OUTLINE_COLOR_ARGB,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }

    val fillPaint = remember(colorArgb, fontSizePx) {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.FILL
            color = colorArgb.toInt()
            textSize = fontSizePx
            typeface = AndroidTypeface.create(AndroidTypeface.SERIF, AndroidTypeface.NORMAL)
        }
    }

    val strokeWidthPx = fontSizePx * TEXT_STICKER_OUTLINE_WIDTH_RATIO

    val textBounds = remember(text, fillPaint) {
        AndroidRect().also {
            fillPaint.getTextBounds(text, 0, text.length, it)
        }
    }

    val contentWidthPx = (textBounds.width() + strokeWidthPx).coerceAtLeast(1f)
    val contentHeightPx = (textBounds.height() + strokeWidthPx).coerceAtLeast(1f)
    val originX = -textBounds.left + strokeWidthPx / 2f
    val originY = -textBounds.top + strokeWidthPx / 2f

    val contentWidthDp = with(density) { contentWidthPx.toDp() }
    val contentHeightDp = with(density) { contentHeightPx.toDp() }

    Canvas(
        modifier = modifier.size(contentWidthDp, contentHeightDp)
    ) {
        drawIntoCanvas { canvas ->
            val strokePaint = AndroidPaint(fillPaint).apply {
                style = AndroidPaint.Style.STROKE
                strokeWidth = strokeWidthPx
                strokeJoin = AndroidPaint.Join.ROUND
                strokeMiter = 2f
                color = outlineColorArgb.toInt()
            }

            canvas.nativeCanvas.drawText(text, originX, originY, strokePaint)
            canvas.nativeCanvas.drawText(text, originX, originY, fillPaint)
        }
    }
}
