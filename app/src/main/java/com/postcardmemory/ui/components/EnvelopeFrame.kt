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
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.postcardmemory.ui.detail.SealType

/** 플랩 삼각형의 밑변이 좌우에서 얼마나 안쪽에서 시작하는지. */
const val ENVELOPE_FLAP_BASE_INSET_FRACTION = 0.10f

/**
 * 앞주머니([EnvelopeFrontPocket])·내부 안감([EnvelopeInterior]) 입구선의 좌우 끝
 * 높이이자 [EnvelopePostmark] 위치 기준. 완전한 수평선이 아니라 중앙이
 * [ENVELOPE_POCKET_CENTER_DIP_FRACTION]만큼 살짝 더 내려가는 완만한 곡선을
 * 이룬다 — 수평 벽이 아니라 종이 앞면처럼 보이게 하기 위함이다.
 */
const val ENVELOPE_POCKET_TOP_FRACTION = 0.34f

/**
 * 입구선 중앙이 좌우 끝보다 얼마나 더 내려가는지(봉투 높이 비율). 벽처럼
 * 보이면 이 값부터 줄인다 — 깊은 V자를 만들지 않는다.
 */
private const val ENVELOPE_POCKET_CENTER_DIP_FRACTION = 0.03f

/**
 * 플랩이 완전히 닫혀 봉인된 것처럼 보일 때의 꼭짓점 높이 — [EnvelopeFlap]의
 * 정적인 기본 삼각형 모양을 결정하는 값이다. 열림 정도는 이 도형을 세로로
 * 축소(scaleY)해 표현하므로, 이 값 자체는 애니메이션 중 좌표를 다시 계산하는
 * 데 쓰이지 않는다.
 */
const val ENVELOPE_FLAP_CLOSED_PEAK_FRACTION = 0.42f

/**
 * 플랩이 위로 활짝 젖혀졌을 때 [ENVELOPE_FLAP_CLOSED_PEAK_FRACTION] 대비 세로
 * 축소 비율(scaleY)을 정하는 값. 실제 봉투는 플랩이 열릴수록 정면 투영이
 * 작아지므로 세 상태 중 가장 작다.
 */
const val ENVELOPE_FLAP_WIDE_OPEN_PEAK_FRACTION = 0.10f

/**
 * 엽서가 들어온 뒤 플랩이 살짝 내려와 정착할 때의 값. 완전히 닫히지 않고
 * 열린 상태를 유지하도록 [ENVELOPE_POCKET_TOP_FRACTION]보다 뚜렷하게 작게
 * 잡는다 — 정착 후에도 입구가 열려 있다는 인상을 남긴다.
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

/** 봉투 몸통 모서리 둥글기(짧은 변 기준 비율). 각지지도, UI 카드처럼 과하게 둥글지도 않게 작게 유지한다. */
private const val ENVELOPE_BODY_CORNER_FRACTION = 0.035f

/** 에어메일 테두리 띠의 두께(가로폭 기준 비율). */
private const val ENVELOPE_BORDER_BAND_FRACTION = 0.03f

/**
 * 앞주머니·내부 안감이 공유하는 입구선 곡선(왼쪽 끝 → 중앙 → 오른쪽 끝)만
 * 그린 열린 [Path]를 반환한다. 호출한 쪽에서 위 또는 아래로 마저 닫아
 * 채우는 도형을 완성한다. 좌우 끝은 [topFraction], 중앙은 그보다
 * [ENVELOPE_POCKET_CENTER_DIP_FRACTION]만큼 살짝 더 내려간 완만한 곡선이다 —
 * 수평 벽이 아니라 종이 앞면처럼 보이게 하려는 의도이므로 깊은 V자로
 * 바꾸지 않는다.
 */
private fun pocketTopCurve(
    width: Float,
    height: Float,
    topFraction: Float
): Path {
    val edgeY = height * topFraction
    val centerY = height * (topFraction + ENVELOPE_POCKET_CENTER_DIP_FRACTION)
    return Path().apply {
        moveTo(0f, edgeY)
        quadraticTo(width * 0.25f, centerY, width * 0.5f, centerY)
        quadraticTo(width * 0.75f, centerY, width, edgeY)
    }
}

/**
 * 봉투 뒷면(몸통)을 그린다. 엽서보다 먼저(아래 레이어로) 그린다 — 엽서
 * 양옆·위쪽 여백에 보이는 봉투 색이 이 레이어다. 살짝 둥근 모서리의 단순한
 * 사각형이다. 하단을 대각선으로 크게 잘라내면 방패·문장 모양처럼 보이므로
 * 그렇게 하지 않는다 — 접힘 자국은 [EnvelopeFoldLines]가 얇은 선으로만
 * 별도 표현한다.
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
        val corner = minOf(w, h) * ENVELOPE_BODY_CORNER_FRACTION
        val cornerRadius = CornerRadius(corner, corner)

        drawRoundRect(color = bodyColor, cornerRadius = cornerRadius)

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
            val roundedClip = Path().apply {
                addRoundRect(RoundRect(Rect(Offset.Zero, Size(w, h)), cornerRadius))
            }
            clipPath(roundedClip) {
                drawAirmailBorder(
                    width = w,
                    height = h,
                    bandWidth = w * ENVELOPE_BORDER_BAND_FRACTION
                )
            }
        }

        drawRoundRect(
            color = accentColor.copy(alpha = 0.55f),
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

/**
 * 플랩이 열렸을 때 보이는 봉투 내부 안감. [EnvelopeBack] 다음, 완성 엽서보다
 * 먼저(더 아래 레이어로) 그린다 — "봉투 안이 비어 있지 않다"는 인상을 주는
 * 용도라 별도 진행도 없이도 [EnvelopeFlap]이 닫히면 자연히 가려진다(닫힌
 * 플랩 꼭짓점이 이 레이어의 아래쪽 경계보다 깊다).
 */
@Composable
fun EnvelopeInterior(
    style: EnvelopeStyle,
    modifier: Modifier = Modifier
) {
    val liningColor = Color(style.baseColorArgb)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val interiorPath = pocketTopCurve(w, h, ENVELOPE_POCKET_TOP_FRACTION).apply {
            lineTo(w, 0f)
            lineTo(0f, 0f)
            close()
        }

        drawPath(interiorPath, color = liningColor)

        // 위쪽(플랩 힌지 쪽)을 살짝 어둡게 해 오목한 안쪽 공간처럼 보이게 한다.
        drawPath(
            interiorPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
                endY = h * (ENVELOPE_POCKET_TOP_FRACTION + ENVELOPE_POCKET_CENTER_DIP_FRACTION)
            )
        )
    }
}

/**
 * 봉투 앞주머니 — 입구선([ENVELOPE_POCKET_TOP_FRACTION] 부근) 아래쪽만 덮는
 * 불투명 층이다. [EnvelopeBack]의 외곽 path를 재사용하지 않는다 — 재사용하면
 * 두 레이어가 같은 대각선 절단을 공유해 방패 모양이 된다. 하단 모서리는 이
 * 레이어를 담는 바깥 Box의 라운드 클립에 맡기고 여기서는 사각형으로만
 * 그린다. **반드시 엽서보다 나중에(위 레이어로)** 그려야 한다 — 그래야
 * 엽서가 아래로 내려와 이 층과 겹치는 부분이 실제로 가려져 "봉투 속으로
 * 들어갔다"는 착시가 생긴다.
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
        val edgeY = h * topFraction
        val pocketPath = pocketTopCurve(w, h, topFraction).apply {
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(pocketPath, color = bodyColor)

        // 입구 경계 바로 아래에 옅은 그림자를 둬 장식이 아니라 깊이(입구)를 표현한다.
        drawPath(
            pocketPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                startY = edgeY,
                endY = edgeY + (h - edgeY) * 0.22f
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

        drawPath(
            pocketTopCurve(w, h, topFraction),
            color = accentColor.copy(alpha = 0.5f),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

/**
 * 봉투 하단 좌우에서 중앙 쪽으로 모이는 접힘선. 옆면을 안으로 접어 붙인
 * 실제 봉투의 봉인 자국을 얇은 선으로만 표현한다 — 외곽을 잘라내면 방패
 * 모양이 되므로 반드시 선으로만 그린다. 앞주머니보다 나중에(위 레이어로)
 * 그려야 주머니 위에 겹쳐 보인다.
 */
@Composable
fun EnvelopeFoldLines(
    style: EnvelopeStyle,
    modifier: Modifier = Modifier
) {
    val lineColor = Color(style.accentColorArgb).copy(alpha = 0.22f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.dp.toPx()
        val centerX = w * 0.5f
        val centerY = h * 0.62f

        drawLine(
            color = lineColor,
            start = Offset(0f, h),
            end = Offset(centerX, centerY),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = lineColor,
            start = Offset(w, h),
            end = Offset(centerX, centerY),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * 봉투 플랩을 그린다. 기본 삼각형 모양은 닫힌 상태([ENVELOPE_FLAP_CLOSED_PEAK_FRACTION])
 * 좌표로 고정하고, 열리는 정도([peakHeightFraction])는 힌지(윗변, y=0)를
 * 기준으로 세로 축소(`graphicsLayer scaleY`)해서만 표현한다 — 좌표와
 * scaleY를 동시에 바꾸면 삼각형이 눌려 찌그러진 것처럼 보이므로 하나만
 * 쓴다. 열릴수록 앞면 색에서 옅은 안감 색으로 섞여, 뒤로 젖혀지며 반대쪽
 * 면이 보이는 느낌을 낸다.
 */
@Composable
fun EnvelopeFlap(
    style: EnvelopeStyle,
    peakHeightFraction: Float,
    modifier: Modifier = Modifier
) {
    val flapColor = Color(style.flapColorArgb)
    val accentColor = Color(style.accentColorArgb)
    val liningColor = lerp(flapColor, Color.White, 0.55f)

    val scaleY = (peakHeightFraction / ENVELOPE_FLAP_CLOSED_PEAK_FRACTION)
        .coerceIn(0.05f, 1.2f)
    val openness = (
        (ENVELOPE_FLAP_CLOSED_PEAK_FRACTION - peakHeightFraction) /
            (ENVELOPE_FLAP_CLOSED_PEAK_FRACTION - ENVELOPE_FLAP_WIDE_OPEN_PEAK_FRACTION)
        ).coerceIn(0f, 1f)
    val displayColor = lerp(flapColor, liningColor, openness)

    Canvas(
        modifier = modifier.graphicsLayer {
            this.scaleY = scaleY
            transformOrigin = TransformOrigin(0.5f, 0f)
        }
    ) {
        val w = size.width
        val h = size.height
        val flapBaseInset = w * ENVELOPE_FLAP_BASE_INSET_FRACTION
        val flapPeakY = h * ENVELOPE_FLAP_CLOSED_PEAK_FRACTION

        val flapPath = Path().apply {
            moveTo(flapBaseInset, 0f)
            lineTo(w - flapBaseInset, 0f)
            lineTo(w / 2f, flapPeakY)
            close()
        }

        drawPath(flapPath, color = displayColor)

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
