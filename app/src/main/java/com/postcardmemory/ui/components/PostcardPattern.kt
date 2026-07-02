package com.postcardmemory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class PostcardPattern(
    val label: String
) {
    NONE("없음"),
    DOTS("땡땡이"),
    STARS("별"),
    HEARTS("하트"),
    CHECKER("체크"),
    CHERRY_BLOSSOMS("벚꽃"),
    TRIANGLES("세모"),
    SQUARES("사각형")
}

@Composable
fun PostcardPatternOverlay(
    pattern: PostcardPattern,
    modifier: Modifier = Modifier,
    patternColor: Color = Color.White.copy(alpha = 0.28f),
    patternScale: Float = 1f
) {
    if (pattern == PostcardPattern.NONE) {
        return
    }

    Canvas(modifier = modifier) {
        drawPostcardPattern(
            pattern = pattern,
            color = patternColor,
            scale = patternScale
        )
    }
}

private fun DrawScope.drawPostcardPattern(
    pattern: PostcardPattern,
    color: Color,
    scale: Float
) {
    val safeScale = scale.coerceIn(0.6f, 2f)

    if (pattern == PostcardPattern.CHECKER) {
        drawCheckerPattern(
            color = color,
            scale = safeScale
        )
        return
    }

    val cellSize = 46f * safeScale
    val horizontalCount = (size.width / cellSize).toInt() + 3
    val verticalCount = (size.height / cellSize).toInt() + 3

    for (row in -1..verticalCount) {
        val staggerOffset = if (row % 2 == 0) {
            0f
        } else {
            cellSize / 2f
        }

        for (column in -1..horizontalCount) {
            val center = Offset(
                x = column * cellSize + staggerOffset,
                y = row * cellSize
            )

            when (pattern) {
                PostcardPattern.NONE -> Unit

                PostcardPattern.DOTS -> {
                    val radius = if ((row + column) % 2 == 0) {
                        5.5f * safeScale
                    } else {
                        3.8f * safeScale
                    }

                    drawCircle(
                        color = color,
                        radius = radius,
                        center = center
                    )
                }

                PostcardPattern.STARS -> {
                    drawStar(
                        center = center,
                        outerRadius = 10f * safeScale,
                        innerRadius = 4.5f * safeScale,
                        color = color
                    )
                }

                PostcardPattern.HEARTS -> {
                    drawHeart(
                        center = center,
                        radius = 9f * safeScale,
                        color = color
                    )
                }

                PostcardPattern.CHERRY_BLOSSOMS -> {
                    drawCherryBlossom(
                        center = center,
                        radius = 9f * safeScale,
                        color = color
                    )
                }

                PostcardPattern.TRIANGLES -> {
                    drawTriangle(
                        center = center,
                        radius = 10f * safeScale,
                        color = color
                    )
                }

                PostcardPattern.SQUARES -> {
                    drawSquarePattern(
                        center = center,
                        radius = 9f * safeScale,
                        color = color,
                        rotated = (row + column) % 2 != 0
                    )
                }

                PostcardPattern.CHECKER -> Unit
            }
        }
    }
}

private fun DrawScope.drawCheckerPattern(
    color: Color,
    scale: Float
) {
    val tileSize = 28f * scale
    val horizontalCount = (size.width / tileSize).toInt() + 2
    val verticalCount = (size.height / tileSize).toInt() + 2

    for (row in 0..verticalCount) {
        for (column in 0..horizontalCount) {
            if ((row + column) % 2 == 0) {
                drawRect(
                    color = color.copy(
                        alpha = color.alpha * 0.72f
                    ),
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

private fun DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()

    repeat(10) { index ->
        val radius = if (index % 2 == 0) {
            outerRadius
        } else {
            innerRadius
        }

        val angle = -PI / 2.0 + index * PI / 5.0

        val point = Offset(
            x = center.x + cos(angle).toFloat() * radius,
            y = center.y + sin(angle).toFloat() * radius
        )

        if (index == 0) {
            path.moveTo(point.x, point.y)
        } else {
            path.lineTo(point.x, point.y)
        }
    }

    path.close()

    drawPath(
        path = path,
        color = color
    )
}

private fun DrawScope.drawHeart(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()

    path.moveTo(
        center.x,
        center.y + radius
    )

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

    drawPath(
        path = path,
        color = color
    )
}

private fun DrawScope.drawCherryBlossom(
    center: Offset,
    radius: Float,
    color: Color
) {
    repeat(5) { index ->
        val angle = index * 72f

        rotate(
            degrees = angle,
            pivot = center
        ) {
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
            alpha = max(
                color.alpha * 0.65f,
                0.08f
            )
        ),
        radius = radius * 0.27f,
        center = center
    )
}

private fun DrawScope.drawTriangle(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()

    path.moveTo(
        center.x,
        center.y - radius
    )

    path.lineTo(
        center.x - radius * 0.9f,
        center.y + radius * 0.75f
    )

    path.lineTo(
        center.x + radius * 0.9f,
        center.y + radius * 0.75f
    )

    path.close()

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = max(2f, radius * 0.18f),
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawSquarePattern(
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