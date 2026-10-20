package com.postcardmemory.ui.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCardContent
import kotlin.random.Random

private val RaceWheelDark = Color(0xFF3C332B)
private val RaceCarInk = Color(0xFF3A322B)
private val RaceCarPaper = Color(0xFFF1EADB)

/** 차체 길이는 엽서 폭보다 조금 길다 — 앞코와 뒷날개만 엽서 양옆으로 빠져나온다. */
private const val RACE_CAR_LENGTH_RATIO = 1.34f

/** 차 그림 전체 높이와, 그중 엽서 아래로 드러나는 높이(나머지는 엽서 뒤에 숨는 운전석). */
private val RaceCarHeight = 26.dp
private val RaceCarBelowCard = 15.dp

/**
 * 쫑쫑컵 카드 사진의 안쪽 여백. 대기 중 무대 카드([GalleryPlayStage]의 쫑쫑컵 분기,
 * 7dp·아래 정렬)와 같은 배치를 써서, 레이스가 시작돼 오버레이로 넘어가는 순간 사진이 튀지 않는다.
 */
internal val RaceCardPhotoInset = 7.dp

/** 카드 상자(폭 [cardWidth], 높이 [cardHeight]) 안에서 아래 정렬된 사진의 윗변 위치. */
internal fun raceCardPhotoTop(cardWidth: Dp, cardHeight: Dp): Dp =
    cardHeight - RaceCardPhotoInset - (cardWidth - RaceCardPhotoInset * 2)

/**
 * 쫑쫑컵 레이스카 한 대. 엽서가 자동차로 변신하는 게 아니라, 옆에서 본 작은 빈티지
 * 오픈휠 경주차 운전석에 엽서가 올라타 달리는 모습이다. 차는 엽서 뒤에 그려서
 * 사진·날짜를 가리지 않고, 엽서 아래로 차체와 앞뒤 바퀴, 오른쪽(진행 방향)으로
 * 길게 뻗은 앞코, 왼쪽으로 작은 뒷날개가 드러난다. `StampCardContent`를 날짜 없이
 * 재사용하고, 차체 색은 보조 요소로만 쓴다.
 */
@Composable
fun PixelPostcardRaceCar(
    postcard: Postcard,
    bodyColor: Color,
    cardWidth: Dp,
    cardHeight: Dp,
    tiltDegrees: Float,
    bobOffsetPx: Float,
    launchScale: Float,
    modifier: Modifier = Modifier
) {
    val photoBottom = cardHeight - RaceCardPhotoInset
    Box(
        modifier = modifier
            .size(width = cardWidth, height = cardHeight)
            .graphicsLayer {
                translationY = bobOffsetPx
                rotationZ = tiltDegrees
                scaleX = launchScale
                scaleY = launchScale
            }
    ) {
        Spacer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = photoBottom - (RaceCarHeight - RaceCarBelowCard))
                .requiredWidth(cardWidth * RACE_CAR_LENGTH_RATIO)
                .height(RaceCarHeight)
                .sideRaceCarCutout(bodyColor = bodyColor, seed = postcard.id)
        )

        // 둥근 카드 상자와 날짜 없이 사진만 차에 올라탄다.
        StampCardContent(
            postcard = postcard,
            isSelected = false,
            dateLabelOverride = "",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(RaceCardPhotoInset)
                .fillMaxWidth()
        )
    }
}

/**
 * 자동차 경주 잡지에 인쇄된 측면 경주차 그림을 오려 붙인 모습. 오른쪽이 앞이다.
 * 실루엣 둘레에 얇은 종이 여백을 남겨 오린 느낌을 주고, 차체에는 바랜 잉크 얼룩과
 * 아래쪽으로 조금 짙어지는 망점만 얹는다. 모양은 [seed](엽서 id)로 한 번만 정해져
 * 주행 중에도 떨리지 않는다.
 */
private fun Modifier.sideRaceCarCutout(bodyColor: Color, seed: Long): Modifier = drawWithCache {
    val dp = density
    val l = size.width
    val h = size.height
    val rng = Random(seed)

    val wheelRadius = 6.5f * dp
    val wheelY = h - wheelRadius - 0.5f * dp
    val rearWheelX = l * 0.17f
    val frontWheelX = l * 0.80f
    val floorY = wheelY + 1f * dp
    val bodyTopY = 9f * dp

    // 낮고 긴 시가형 차체: 뒤는 살짝 좁아지고, 앞은 엽서 밖으로 길게 뻗어 둥근 코로 끝난다.
    val body = Path().apply {
        moveTo(l * 0.09f, floorY)
        lineTo(l * 0.07f, bodyTopY + 1.5f * dp)
        quadraticTo(l * 0.075f, bodyTopY, l * 0.11f, bodyTopY)
        lineTo(l * 0.86f, bodyTopY + 0.5f * dp)
        quadraticTo(l * 0.95f, bodyTopY + 2.5f * dp, l - 1.5f * dp, floorY - 4.5f * dp)
        quadraticTo(l, floorY - 2.5f * dp, l * 0.97f, floorY)
        close()
    }
    // 엽서 앞에 살짝 보이는 작은 바람막이 — 운전석이 여기 있다는 표시.
    val windscreen = Path().apply {
        moveTo(l * 0.868f, bodyTopY + 0.6f * dp)
        lineTo(l * 0.876f, bodyTopY - 3.6f * dp)
        quadraticTo(l * 0.89f, bodyTopY - 3.4f * dp, l * 0.905f, bodyTopY + 1f * dp)
        close()
    }
    val rearWing = Rect(0.5f * dp, 2.5f * dp, l * 0.105f, 5f * dp)
    val rearWingStrut = Rect(l * 0.045f, 5f * dp, l * 0.045f + 1.6f * dp, bodyTopY + 1f * dp)
    val frontWing = Rect(l * 0.9f, floorY - 1.2f * dp, l - 0.5f * dp, floorY + 0.6f * dp)
    val wheelCenters = listOf(Offset(rearWheelX, wheelY), Offset(frontWheelX, wheelY))

    val trim = lerp(bodyColor, RaceCarInk, 0.35f)

    val dotStep = 2.4f * dp
    val halftone = buildList {
        var row = 0
        var y = bodyTopY
        while (y < floorY) {
            var x = if (row % 2 == 0) 0f else dotStep / 2f
            while (x < l) {
                val depth = ((y - bodyTopY) / (floorY - bodyTopY)).coerceIn(0f, 1f)
                if (rng.nextFloat() < 0.1f + depth * 0.45f) add(Offset(x, y))
                x += dotStep
            }
            y += dotStep
            row++
        }
    }
    // 고르게 찍히지 않은 인쇄 잉크: 차체 군데군데가 살짝 바래 있다.
    val wornPatches = List(3) {
        val pw = (6f + rng.nextFloat() * 10f) * dp
        val ph = (2f + rng.nextFloat() * 3f) * dp
        val cx = l * (0.1f + rng.nextFloat() * 0.85f)
        val cy = bodyTopY + rng.nextFloat() * (floorY - bodyTopY)
        Offset(cx - pw / 2f, cy - ph / 2f) to Size(pw, ph)
    }

    onDrawBehind {
        // 바닥에 닿은 자리만 아주 옅게 — 차가 떠 보이지 않을 정도의 잉크 자국.
        drawOval(
            color = RaceCarInk.copy(alpha = 0.1f),
            topLeft = Offset(l * 0.12f, h - 1.8f * dp),
            size = Size(l * 0.76f, 1.8f * dp)
        )

        // 오려낸 종이 여백: 실루엣 둘레를 종이색으로 한 번 넓게 칠해 둔다.
        val margin = Stroke(width = 2.4f * dp, join = StrokeJoin.Round)
        drawPath(path = body, color = RaceCarPaper, style = margin)
        drawRect(color = RaceCarPaper, topLeft = rearWing.topLeft, size = rearWing.size, style = margin)
        drawRect(color = RaceCarPaper, topLeft = frontWing.topLeft, size = frontWing.size, style = margin)
        wheelCenters.forEach { center ->
            drawCircle(color = RaceCarPaper, radius = wheelRadius + 1.2f * dp, center = center)
        }

        drawRect(color = trim, topLeft = rearWingStrut.topLeft, size = rearWingStrut.size)
        drawRect(color = trim, topLeft = rearWing.topLeft, size = rearWing.size)
        drawRect(color = trim, topLeft = frontWing.topLeft, size = frontWing.size)

        drawPath(path = body, color = bodyColor.copy(alpha = 0.94f))
        clipPath(body) {
            wornPatches.forEach { (topLeft, patchSize) ->
                drawOval(color = RaceCarPaper.copy(alpha = 0.16f), topLeft = topLeft, size = patchSize)
            }
            // 차체 옆구리를 길게 가로지르는 크림색 줄무늬 — 차가 길고 낮아 보이게 한다.
            drawLine(
                color = RaceCarPaper.copy(alpha = 0.7f),
                start = Offset(l * 0.06f, floorY - 5f * dp),
                end = Offset(l, floorY - 5f * dp),
                strokeWidth = 1.2f * dp
            )
            drawPoints(
                points = halftone,
                pointMode = PointMode.Points,
                color = RaceCarInk.copy(alpha = 0.16f),
                strokeWidth = 0.9f * dp,
                cap = StrokeCap.Round
            )
        }
        drawPath(
            path = body,
            color = RaceCarInk.copy(alpha = 0.5f),
            style = Stroke(width = 0.8f * dp, join = StrokeJoin.Round)
        )

        // 앞코의 번호판 동그라미.
        drawCircle(
            color = RaceCarPaper.copy(alpha = 0.85f),
            radius = 2.4f * dp,
            center = Offset(l * 0.925f, floorY - 5f * dp)
        )

        drawPath(path = windscreen, color = RaceCarPaper.copy(alpha = 0.6f))
        drawPath(
            path = windscreen,
            color = RaceCarInk.copy(alpha = 0.45f),
            style = Stroke(width = 0.6f * dp, join = StrokeJoin.Round)
        )

        // 차체 밖으로 드러난 앞뒤 바퀴: 진한 타이어, 크림색 휠, 가운데 허브.
        wheelCenters.forEach { center ->
            drawCircle(color = RaceWheelDark, radius = wheelRadius, center = center)
            drawCircle(color = RaceCarPaper.copy(alpha = 0.8f), radius = wheelRadius * 0.42f, center = center)
            drawCircle(color = RaceWheelDark, radius = wheelRadius * 0.15f, center = center)
        }
    }
}
