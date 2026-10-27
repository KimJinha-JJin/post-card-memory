package com.postcardmemory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.detail.LabelTapeStyle
import com.postcardmemory.ui.detail.labelTapePalette
import com.postcardmemory.utils.LabelStickerRenderer

/**
 * 라벨 스티커 기본(scale=1.0) 글자 크기. 라벨 폭·두께가 전부 여기서
 * 파생되므로, 최대 문구 길이(LABEL_STICKER_MAX_LENGTH)와 짝을 이뤄
 * 가장 긴 한글 문구도 미리보기 엽서 안에 들어오도록 정해진 값이다.
 */
val LABEL_STICKER_BASE_FONT_SIZE_SP = 15f

/**
 * 라벨프린터에서 뽑은 플라스틱 테이프 한 조각의 미리보기. 실제 그리기는
 * 저장 이미지와 완전히 같은 [LabelStickerRenderer]에 맡기고, 여기서는
 * 그 렌더러가 알려준 크기에 컴포저블 레이아웃을 맞추는 일만 한다 —
 * 이 컴포저블의 레이아웃 크기가 곧 보이는 라벨 크기라서 제스처 히트
 * 영역·중앙 정렬 계산이 여백 없이 그대로 맞아떨어진다.
 */
@Composable
fun LabelStickerContent(
    text: String,
    style: LabelTapeStyle,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
    customTapeColorArgb: Long? = null
) {
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSizeSp.sp.toPx() }

    // 저장 이미지 쪽(PostcardImageExporter)도 같은 labelTapePalette를 부른다 —
    // 테이프색·문자색 결정 로직이 화면 전용/export 전용으로 갈라지지 않는다.
    val palette = remember(style, customTapeColorArgb) {
        labelTapePalette(
            style = style,
            customTapeColorArgb = customTapeColorArgb
        )
    }

    val textPaint = remember(fontSizePx) {
        LabelStickerRenderer.createTextPaint(fontSizePx)
    }

    val labelWidthPx = remember(text, fontSizePx, textPaint) {
        LabelStickerRenderer.labelWidthPx(
            text = text,
            fontSizePx = fontSizePx,
            textPaint = textPaint
        )
    }
    val labelHeightPx = LabelStickerRenderer.labelHeightPx(fontSizePx)

    val labelWidthDp = with(density) { labelWidthPx.toDp() }
    val labelHeightDp = with(density) { labelHeightPx.toDp() }

    Canvas(
        modifier = modifier.size(labelWidthDp, labelHeightDp)
    ) {
        drawIntoCanvas { canvas ->
            LabelStickerRenderer.draw(
                canvas = canvas.nativeCanvas,
                text = text,
                palette = palette,
                fontSizePx = fontSizePx,
                left = 0f,
                top = 0f,
                textPaint = textPaint
            )
        }
    }
}
