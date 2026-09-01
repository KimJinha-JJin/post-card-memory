package com.postcardmemory.ui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCardContent
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface

private val RaceWheelDark = Color(0xFF3C332B)
private val RaceChassisShadow = Color(0xFF2E2A24)

/**
 * 쫑쫑컵 레이스카 한 대. 엽서가 자동차로 변신하는 게 아니라, 작은 차체 위에
 * 엽서가 얹혀 달리는 모습이다. `StampCardContent`를 그대로 재사용해 사진·날짜가
 * 계속 읽히게 하고, 차체 색은 보조 요소로만 쓴다.
 */
@Composable
fun PixelPostcardRaceCar(
    postcard: Postcard,
    bodyColor: Color,
    cardWidth: Dp,
    tiltDegrees: Float,
    bobOffsetPx: Float,
    launchScale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .graphicsLayer {
                translationY = bobOffsetPx
                rotationZ = tiltDegrees
                scaleX = launchScale
                scaleY = launchScale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(cardWidth)
                .background(PaperSurface, RoundedCornerShape(10.dp))
                .border(1.dp, PaperDivider, RoundedCornerShape(10.dp))
                .padding(5.dp)
        ) {
            StampCardContent(
                postcard = postcard,
                isSelected = false,
                dateTextSizeSp = 9
            )
        }

        Canvas(
            modifier = Modifier
                .width(cardWidth * 0.82f)
                .height(13.dp)
                .offset(y = (-4).dp)
        ) {
            val bodyHeight = size.height * 0.62f
            drawRoundRect(
                color = bodyColor,
                topLeft = Offset(0f, 0f),
                size = Size(size.width, bodyHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = bodyColor.copy(alpha = 0.55f),
                topLeft = Offset(size.width * 0.14f, bodyHeight * 0.28f),
                size = Size(size.width * 0.72f, bodyHeight * 0.3f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            val wheelRadius = size.height * 0.24f
            val wheelY = bodyHeight
            val wheelXs = floatArrayOf(size.width * 0.2f, size.width * 0.8f)
            for (wheelX in wheelXs) {
                drawCircle(
                    color = RaceChassisShadow,
                    radius = wheelRadius,
                    center = Offset(wheelX, wheelY)
                )
                drawCircle(
                    color = RaceWheelDark,
                    radius = wheelRadius * 0.72f,
                    center = Offset(wheelX, wheelY)
                )
            }
        }
    }
}
