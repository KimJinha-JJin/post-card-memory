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

/** 봉투 앞주머니(EnvelopeFrontPocket)가 시작되는 높이. */
const val ENVELOPE_POCKET_TOP_FRACTION = 0.40f

/**
 * 플랩이 완전히 닫혀 봉인된 것처럼 보일 때의 꼭짓점 높이. 실제 봉투에서 닫힌
 * 플랩은 앞면 절반 가까이를 덮는 큰 삼각형이므로 세 상태 중 가장 큰 값이어야
 * 한다 — 위로 젖혀 열수록(진행률 증가) 정면 투영이 작아지는 실제 접힘 방향을
 * 따른다.
 */
const val ENVELOPE_FLAP_CLOSED_PEAK_FRACTION = 0.42f

/**
 * 삽입 애니메이션 1단계에서 플랩이 위로 활짝 젖혀졌을 때의 꼭짓점 높이. 실제
 * 봉투는 플랩이 열릴수록 정면에서 보이는 부분이 작아지므로 세 상태 중 가장
 * 작은 값이다 — 입구를 최대한 넓게 드러낸다.
 */
const val ENVELOPE_FLAP_WIDE_OPEN_PEAK_FRACTION = 0.10f

/**
 * 엽서가 들어온 뒤 플랩이 살짝 내려와 정착하는 꼭짓점 높이. 완전히 닫히지
 * 않고 열린 상태를 유지하도록 [ENVELOPE_POCKET_TOP_FRACTION]보다 뚜렷하게
 * 작게 잡는다 — 정착 후에도 입구가 열려 있다는 인상을 남긴다.
 */
const val ENVELOPE_FLAP_RESTING_PEAK_FRACTION = 0.20f

/** 엽서 박스의 높이 비율(봉투 높이 기준) — 정사각형 엽서를 봉투 안에 넣을 때 쓰는 기준값. */
const val ENVELOPE_CARD_HEIGHT_FRACTION = 0.72f

/**
 * 삽입 애니메이션이 끝난 뒤 카드 상단이 위치하는 높이 비율(봉투 높이 기준).
 * [ENVELOPE_POCKET_TOP_FRACTION], [ENVELOPE_CARD_HEIGHT_FRACTION]과 함께 계산하면
 * 카드 높이의 약 60%가 앞주머니 뒤로 들어가고, 위쪽 약 40%만 남아 어떤 엽서인지
 * 알아볼 수 있다. 봉투/화면 크기와 무관하게 항상 같은 비율로 가려지도록 dp 고정값
 * 대신 이 비율을 쓴다.
 */
const val ENVELOPE_CARD_REST_TOP_FRACTION = 0.12f

/**
 * 삽입 애니메이션 시작 시 카드가 이동하는 거리를 봉투 높이 비율로 표현한 값.
 * 고정 dp 대신 이 비율을 쓰면 화면·봉투 크기가 달라져도 카드가 항상 비슷한
 * 체감 거리만큼 움직인다.
 */
const val ENVELOPE_CARD_ENTRY_TRAVEL_FRACTION = 0.62f

/** 에어메일 테두리 띠의 두께(가로폭 기준 비율). */
private const val ENVELOPE_BORDER_BAND_FRACTION = 0.03f

/**
 * 봉투 하단 좌우 모서리를 대각선으로 접어 깎은 봉인선 깊이(짧은 변 기준 비율).
 * 몸통([EnvelopeBack])과 앞주머니([EnvelopeFrontPocket])에 똑같이 적용해
 * 두 레이어가 둥근 카드 패널이 아니라 하나로 이어진 종이 봉투 실루엣으로
 * 보이게 한다.
 */
private const val ENVELOPE_BOTTOM_SEAM_CUT_FRACTION = 0.16f

/** 봉투 몸통/앞주머니의 각진 하단 봉인선 실루엣을 그린다. 위쪽 두 모서리만 살짝 둥글다. */
private fun envelopeBodyPath(
    width: Float,
    height: Float,
    topStartY: Float = 0f
): Path {
    val topCorner = minOf(width, height) * 0.04f
    val bottomCut = minOf(width, height) * ENVELOPE_BOTTOM_SEAM_CUT_FRACTION

    return Path().apply {
        if (topStartY <= 0f) {
            moveTo(topCorner, 0f)
            lineTo(width - topCorner, 0f)
            quadraticTo(width, 0f, width, topCorner)
            lineTo(width, height - bottomCut)
            lineTo(width - bottomCut, height)
            lineTo(bottomCut, height)
            lineTo(0f, height - bottomCut)
            lineTo(0f, topCorner)
            quadraticTo(0f, 0f, topCorner, 0f)
        } else {
            moveTo(0f, topStartY)
            lineTo(width, topStartY)
            lineTo(width, height - bottomCut)
            lineTo(width - bottomCut, height)
            lineTo(bottomCut, height)
            lineTo(0f, height - bottomCut)
        }
        close()
    }
}

/**
 * 봉투 뒷면(몸통)을 그린다. 엽서보다 먼저(아래 레이어로) 그린다 — 엽서
 * 양옆·위쪽 여백에 보이는 봉투 색이 이 레이어다. 하단 모서리를 대각선으로
 * 깎아 종이를 접어 봉인한 실제 봉투의 실루엣을 흉내 낸다.
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
        val bodyPath = envelopeBodyPath(width = w, height = h)

        drawPath(bodyPath, color = bodyColor)

        // 좌우 가장자리를 살짝 어둡게 해 안으로 오목한 주머니처럼 보이게 한다.
        drawPath(
            bodyPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.08f),
                    Color.Transparent,
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.08f)
                )
            )
        )

        if (style.hasAirmailBorder) {
            clipPath(bodyPath) {
                drawAirmailBorder(
                    width = w,
                    height = h,
                    bandWidth = w * ENVELOPE_BORDER_BAND_FRACTION
                )
            }
        }

        drawPath(
            bodyPath,
            color = accentColor.copy(alpha = 0.55f),
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
        val pocketPath = envelopeBodyPath(width = w, height = h, topStartY = top)

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
