package com.postcardmemory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.postcardmemory.ui.detail.SealType

/** 플랩 삼각형의 밑변이 좌우에서 얼마나 안쪽에서 시작하는지. */
const val ENVELOPE_FLAP_BASE_INSET_FRACTION = 0.10f

/** 봉투 앞주머니(EnvelopeFrontPocket)가 시작되는 높이 — 플랩이 평상시 접히는 위치와 맞춘다. */
const val ENVELOPE_POCKET_TOP_FRACTION = 0.40f

/** 플랩이 평상시(엽서가 들어와 정착한 뒤) 접혀 있는 꼭짓점 높이. 주머니 시작선과 맞춰 이어져 보이게 한다. */
const val ENVELOPE_FLAP_RESTING_PEAK_FRACTION = ENVELOPE_POCKET_TOP_FRACTION

/** 삽입 애니메이션 1단계에서 플랩이 활짝 벌어질 때의 꼭짓점 높이. */
const val ENVELOPE_FLAP_WIDE_OPEN_PEAK_FRACTION = 0.58f

/** 플랩이 완전히 닫혀 있을 때(애니메이션 시작 지점) 꼭짓점 높이. */
const val ENVELOPE_FLAP_CLOSED_PEAK_FRACTION = 0.05f

/** 엽서 박스의 높이 비율(봉투 높이 기준) — 정사각형 엽서를 봉투 안에 넣을 때 쓰는 기준값. */
const val ENVELOPE_CARD_HEIGHT_FRACTION = 0.72f

/** 에어메일 테두리 띠의 두께(가로폭 기준 비율). */
private const val ENVELOPE_BORDER_BAND_FRACTION = 0.03f

/**
 * 봉투 뒷면(몸통)을 그린다. 엽서보다 먼저(아래 레이어로) 그린다 — 엽서
 * 양옆·위쪽 여백에 보이는 봉투 색이 이 레이어다.
 */
@Composable
fun EnvelopeBack(
    style: EnvelopeStyle,
    modifier: Modifier = Modifier
) {
    val bodyColor = Color(style.baseColorArgb)
    val accentColor = Color(style.accentColorArgb)

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "${style.label} 봉투"
        }
    ) {
        val w = size.width
        val h = size.height
        val corner = minOf(w, h) * 0.05f
        val cornerRadius = CornerRadius(corner, corner)

        drawRoundRect(
            color = bodyColor,
            cornerRadius = cornerRadius
        )

        // 좌우 가장자리를 살짝 어둡게 해 안으로 오목한 주머니처럼 보이게 한다.
        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.08f),
                    Color.Transparent,
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.08f)
                )
            ),
            cornerRadius = cornerRadius
        )

        if (style.hasAirmailBorder) {
            drawAirmailBorder(
                width = w,
                height = h,
                bandWidth = w * ENVELOPE_BORDER_BAND_FRACTION
            )
        }

        drawRoundRect(
            color = accentColor.copy(alpha = 0.55f),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * 봉투 앞주머니 — [topFraction]보다 아래쪽만 덮는 불투명 층이다. **반드시
 * 엽서보다 나중에(위 레이어로)** 그려야 한다. 그래야 엽서가 아래로
 * 내려와 이 층과 겹치는 부분이 실제로 가려져 "봉투 속으로 들어갔다"는
 * 착시가 생긴다 — 이게 없으면 엽서는 봉투 위에 그냥 얹힌 것처럼 보인다.
 */
@Composable
fun EnvelopeFrontPocket(
    style: EnvelopeStyle,
    topFraction: Float,
    modifier: Modifier = Modifier
) {
    val bodyColor = Color(style.baseColorArgb)
    val accentColor = Color(style.accentColorArgb)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val top = h * topFraction
        val corner = minOf(w, h) * 0.05f

        val pocketPath = Path().apply {
            moveTo(0f, top)
            lineTo(w, top)
            lineTo(w, h - corner)
            quadraticTo(w, h, w - corner, h)
            lineTo(corner, h)
            quadraticTo(0f, h, 0f, h - corner)
            close()
        }

        drawPath(pocketPath, color = bodyColor)

        // 엽서가 파고드는 윗선에 옅은 그림자를 둬 "여기서부터 안쪽"이라는 단차를 준다.
        drawPath(
            pocketPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.16f),
                    Color.Transparent
                ),
                startY = top,
                endY = top + (h - top) * 0.25f
            )
        )

        if (style.hasAirmailBorder) {
            clipPath(pocketPath) {
                drawAirmailBorder(
                    width = w,
                    height = h,
                    bandWidth = w * ENVELOPE_BORDER_BAND_FRACTION
                )
            }
        }

        drawLine(
            color = accentColor.copy(alpha = 0.6f),
            start = Offset(0f, top),
            end = Offset(w, top),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

/**
 * 봉투 플랩만 그린다. 열린 정도([peakHeightFraction])를 애니메이션해 입구가
 * 벌어지는 느낌을 낸다(닫힘 [ENVELOPE_FLAP_CLOSED_PEAK_FRACTION] → 활짝
 * [ENVELOPE_FLAP_WIDE_OPEN_PEAK_FRACTION] → 평상시 [ENVELOPE_FLAP_RESTING_PEAK_FRACTION]).
 */
@Composable
fun EnvelopeFlap(
    style: EnvelopeStyle,
    peakHeightFraction: Float,
    modifier: Modifier = Modifier
) {
    val flapColor = Color(style.flapColorArgb)
    val accentColor = Color(style.accentColorArgb)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val flapBaseInset = w * ENVELOPE_FLAP_BASE_INSET_FRACTION
        val flapPeakY = h * peakHeightFraction

        val flapPath = Path().apply {
            moveTo(flapBaseInset, 0f)
            lineTo(w - flapBaseInset, 0f)
            lineTo(w / 2f, flapPeakY)
            close()
        }

        drawPath(flapPath, color = flapColor)

        // 플랩 안쪽(꼭짓점 쪽)을 살짝 어둡게 — 접힌 종이의 그림자.
        drawPath(
            flapPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                endY = flapPeakY.coerceAtLeast(1f)
            )
        )

        drawPath(
            flapPath,
            color = accentColor.copy(alpha = 0.7f),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/** 봉투 소인. 앞주머니 시작선([ENVELOPE_POCKET_TOP_FRACTION]) 아래, 엽서 오른쪽 여백에 고정 배치한다. */
@Composable
fun EnvelopePostmark(
    postcardCapturedAt: Long,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = maxHeight * (ENVELOPE_POCKET_TOP_FRACTION + 0.06f),
                    end = maxWidth * 0.04f
                )
                .size(minOf(maxWidth, maxHeight) * 0.2f)
        ) {
            SealPreviewContent(
                type = SealType.CIRCLE_POSTMARK,
                color = Color(0xFF252525L),
                capturedAtMillis = postcardCapturedAt,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** 대각선 빨강·파랑 줄무늬로 에어메일 특유의 테두리를 그린다 — 색상만으로 스타일을 구분하지 않기 위함. */
private fun DrawScope.drawAirmailBorder(
    width: Float,
    height: Float,
    bandWidth: Float
) {
    val outer = Path().apply {
        addRect(Rect(Offset.Zero, Size(width, height)))
    }
    val inner = Path().apply {
        addRect(
            Rect(
                Offset(bandWidth, bandWidth),
                Size(width - bandWidth * 2f, height - bandWidth * 2f)
            )
        )
    }
    val frame = Path().apply {
        op(outer, inner, PathOperation.Difference)
    }

    clipPath(frame) {
        val stripeColors = listOf(
            Color(0xFFD64545L).copy(alpha = 0.85f),
            Color.Transparent,
            Color(0xFF3B5FCCL).copy(alpha = 0.85f),
            Color.Transparent
        )
        val stripeWidth = bandWidth * 1.6f
        var x = -height
        var colorIndex = 0
        while (x < width + height) {
            drawLine(
                color = stripeColors[colorIndex % stripeColors.size],
                start = Offset(x, height),
                end = Offset(x + height, 0f),
                strokeWidth = stripeWidth
            )
            x += stripeWidth
            colorIndex++
        }
    }
}
