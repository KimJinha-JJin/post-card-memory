package com.postcardmemory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.postcardmemory.ui.theme.BrutalDeepViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 엽서 미리보기와 갤러리 내보내기에서 함께 사용할 배경 패턴 레이어.
 * 패턴 그리기 코드를 한곳에 두어 화면과 저장 이미지의 모양이 달라지는 일을 막는다.
 */
@Composable
fun PostcardPatternOverlay(
    pattern: PostcardBackgroundPattern,
    backgroundColorArgb: Long,
    modifier: Modifier = Modifier
) {
    if (pattern == PostcardBackgroundPattern.NONE) return

    val patternColor = remember(backgroundColorArgb) {
        patternColorFor(backgroundColorArgb)
    }

    Canvas(modifier = modifier) {
        drawPostcardPattern(
            pattern = pattern,
            color = patternColor
        )
    }
}

private fun patternColorFor(backgroundColorArgb: Long): Color {
    val color = backgroundColorArgb.toInt()
    val red = (color shr 16) and 0xFF
    val green = (color shr 8) and 0xFF
    val blue = color and 0xFF

    val brightness = (
            red * 0.299f +
                    green * 0.587f +
                    blue * 0.114f
            ) / 255f

    return if (brightness < 0.48f) {
        Color.White.copy(alpha = 0.34f)
    } else {
        BrutalDeepViolet.copy(alpha = 0.18f)
    }
}

private fun DrawScope.drawPostcardPattern(
    pattern: PostcardBackgroundPattern,
    color: Color
) {
    if (pattern == PostcardBackgroundPattern.CHECKER) {
        drawCheckerPattern(color)
        return
    }

    val cellSize = 48.dp.toPx()
    val horizontalCount = (size.width / cellSize).toInt() + 3
    val verticalCount = (size.height / cellSize).toInt() + 3

    for (row in -1..verticalCount) {
        val staggerOffset = if (row % 2 == 0) 0f else cellSize / 2f

        for (column in -1..horizontalCount) {
            val center = Offset(
                x = column * cellSize + staggerOffset,
                y = row * cellSize
            )

            when (pattern) {
                PostcardBackgroundPattern.NONE -> Unit

                PostcardBackgroundPattern.DOTS -> {
                    drawCircle(
                        color = color,
                        radius = if ((row + column) % 2 == 0) {
                            5.5.dp.toPx()
                        } else {
                            3.5.dp.toPx()
                        },
                        center = center
                    )
                }

                PostcardBackgroundPattern.STARS -> {
                    drawPatternStar(
                        center = center,
                        outerRadius = 10.dp.toPx(),
                        innerRadius = 4.4.dp.toPx(),
                        color = color
                    )
                }

                PostcardBackgroundPattern.HEARTS -> {
                    drawPatternHeart(
                        center = center,
                        radius = 9.dp.toPx(),
                        color = color
                    )
                }

                PostcardBackgroundPattern.CHERRY_BLOSSOMS -> {
                    drawCherryBlossom(
                        center = center,
                        radius = 8.5.dp.toPx(),
                        color = color
                    )
                }

                PostcardBackgroundPattern.TRIANGLES -> {
                    drawPatternTriangle(
                        center = center,
                        radius = 9.dp.toPx(),
                        color = color
                    )
                }

                PostcardBackgroundPattern.SQUARES -> {
                    drawPatternSquare(
                        center = center,
                        radius = 8.dp.toPx(),
                        color = color,
                        rotated = (row + column) % 2 != 0
                    )
                }

                PostcardBackgroundPattern.CHECKER -> Unit
            }
        }
    }
}

private fun DrawScope.drawCheckerPattern(color: Color) {
    val tileSize = 28.dp.toPx()
    val horizontalCount = (size.width / tileSize).toInt() + 2
    val verticalCount = (size.height / tileSize).toInt() + 2

    for (row in 0..verticalCount) {
        for (column in 0..horizontalCount) {
            if ((row + column) % 2 == 0) {
                drawRect(
                    color = color.copy(alpha = color.alpha * 0.78f),
                    topLeft = Offset(
                        x = column * tileSize,
                        y = row * tileSize
                    ),
                    size = Size(
                        width = tileSize,
                        height = tileSize
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawPatternStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()

    repeat(10) { index ->
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2.0 + index * PI / 5.0
        val point = Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius
        )

        if (index == 0) path.moveTo(point.x, point.y)
        else path.lineTo(point.x, point.y)
    }

    path.close()
    drawPath(path = path, color = color)
}

private fun DrawScope.drawPatternHeart(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()

    path.moveTo(center.x, center.y + radius)
    path.cubicTo(
        center.x - radius * 1.35f,
        center.y + radius * 0.2f,
        center.x - radius,
        center.y - radius * 0.95f,
        center.x,
        center.y - radius * 0.28f
    )
    path.cubicTo(
        center.x + radius,
        center.y - radius * 0.95f,
        center.x + radius * 1.35f,
        center.y + radius * 0.2f,
        center.x,
        center.y + radius
    )
    path.close()

    drawPath(path = path, color = color)
}

private fun DrawScope.drawCherryBlossom(
    center: Offset,
    radius: Float,
    color: Color
) {
    repeat(5) { index ->
        rotate(degrees = index * 72f, pivot = center) {
            drawOval(
                color = color,
                topLeft = Offset(
                    x = center.x - radius * 0.42f,
                    y = center.y - radius * 1.08f
                ),
                size = Size(
                    width = radius * 0.84f,
                    height = radius * 1.2f
                )
            )
        }
    }

    drawCircle(
        color = color.copy(
            alpha = max(color.alpha * 0.7f, 0.08f)
        ),
        radius = radius * 0.27f,
        center = center
    )
}

private fun DrawScope.drawPatternTriangle(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(
            center.x - radius * 0.9f,
            center.y + radius * 0.75f
        )
        lineTo(
            center.x + radius * 0.9f,
            center.y + radius * 0.75f
        )
        close()
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = max(2f, radius * 0.18f),
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawPatternSquare(
    center: Offset,
    radius: Float,
    color: Color,
    rotated: Boolean
) {
    rotate(
        degrees = if (rotated) 45f else 0f,
        pivot = center
    ) {
        drawRoundRect(
            color = color,
            topLeft = Offset(
                x = center.x - radius,
                y = center.y - radius
            ),
            size = Size(
                width = radius * 2f,
                height = radius * 2f
            ),
            cornerRadius = CornerRadius(
                x = radius * 0.24f,
                y = radius * 0.24f
            ),
            style = Stroke(
                width = max(2f, radius * 0.18f),
                join = StrokeJoin.Round
            )
        )
    }
}
