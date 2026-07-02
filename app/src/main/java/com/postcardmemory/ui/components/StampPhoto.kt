package com.postcardmemory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import kotlin.math.max
import kotlin.math.min

private fun createPinkingPath(
    size: Size,
    inset: Float = 0f
): Path {
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset

    val width =
        (right - left).coerceAtLeast(1f)

    val height =
        (bottom - top).coerceAtLeast(1f)

    val shortestSide =
        min(width, height)

    val toothDepth =
        (shortestSide * 0.032f)
            .coerceAtLeast(2f)

    val cornerCut =
        toothDepth * 1.5f

    val horizontalLength =
        (width - cornerCut * 2f)
            .coerceAtLeast(1f)

    val verticalLength =
        (height - cornerCut * 2f)
            .coerceAtLeast(1f)

    val horizontalTeeth =
        max(
            8,
            (
                    horizontalLength /
                            (toothDepth * 2.3f)
                    ).toInt()
        )

    val verticalTeeth =
        max(
            8,
            (
                    verticalLength /
                            (toothDepth * 2.3f)
                    ).toInt()
        )

    val horizontalStep =
        horizontalLength /
                horizontalTeeth

    val verticalStep =
        verticalLength /
                verticalTeeth

    val path = Path()

    path.moveTo(
        left + cornerCut,
        top
    )

    repeat(horizontalTeeth) { index ->
        val startX =
            left +
                    cornerCut +
                    index * horizontalStep

        val middleX =
            startX +
                    horizontalStep / 2f

        val endX =
            startX +
                    horizontalStep

        path.lineTo(
            middleX,
            top + toothDepth
        )

        path.lineTo(
            endX,
            top
        )
    }

    path.lineTo(
        right,
        top + cornerCut
    )

    repeat(verticalTeeth) { index ->
        val startY =
            top +
                    cornerCut +
                    index * verticalStep

        val middleY =
            startY +
                    verticalStep / 2f

        val endY =
            startY +
                    verticalStep

        path.lineTo(
            right - toothDepth,
            middleY
        )

        path.lineTo(
            right,
            endY
        )
    }

    path.lineTo(
        right - cornerCut,
        bottom
    )

    repeat(horizontalTeeth) { index ->
        val startX =
            right -
                    cornerCut -
                    index * horizontalStep

        val middleX =
            startX -
                    horizontalStep / 2f

        val endX =
            startX -
                    horizontalStep

        path.lineTo(
            middleX,
            bottom - toothDepth
        )

        path.lineTo(
            endX,
            bottom
        )
    }

    path.lineTo(
        left,
        bottom - cornerCut
    )

    repeat(verticalTeeth) { index ->
        val startY =
            bottom -
                    cornerCut -
                    index * verticalStep

        val middleY =
            startY -
                    verticalStep / 2f

        val endY =
            startY -
                    verticalStep

        path.lineTo(
            left + toothDepth,
            middleY
        )

        path.lineTo(
            left,
            endY
        )
    }

    path.close()

    return path
}

object PinkingPhotoShape : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            createPinkingPath(size)
        )
    }
}

@Composable
fun StampPhoto(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 2f
) {
    val whiteOutlineColor =
        outlineColor.copy(
            red = 1f,
            green = 1f,
            blue = 1f
        )

    Box(
        modifier =
            modifier.aspectRatio(1f)
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription =
                contentDescription,
            contentScale =
                ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(PinkingPhotoShape)
        )

        Canvas(
            modifier =
                Modifier.matchParentSize()
        ) {
            val strokeWidthPx =
                outlineWidth.dp.toPx()

            val borderPath =
                createPinkingPath(
                    size = size,
                    inset =
                        strokeWidthPx / 2f
                )

            drawPath(
                path = borderPath,
                color =
                    whiteOutlineColor,
                style = Stroke(
                    width =
                        strokeWidthPx,
                    join =
                        StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun PinkingPhotoGuide(
    modifier: Modifier = Modifier,
    outlineColor: Color = Color.White,
    outlineWidth: Float = 4f,
    haloColor: Color = Color.White,
    haloWidth: Float = 8f
) {
    val whiteOutlineColor =
        outlineColor.copy(
            red = 1f,
            green = 1f,
            blue = 1f
        )

    Canvas(
        modifier = modifier
    ) {
        val outlineWidthPx =
            outlineWidth.dp.toPx()

        val haloWidthPx =
            haloWidth.dp.toPx()

        val inset =
            max(
                outlineWidthPx,
                haloWidthPx
            ) / 2f

        val guidePath =
            createPinkingPath(
                size = size,
                inset = inset
            )

        drawPath(
            path = guidePath,
            color =
                haloColor.copy(
                    alpha = 0.85f
                ),
            style = Stroke(
                width =
                    haloWidthPx,
                join =
                    StrokeJoin.Round
            )
        )

        drawPath(
            path = guidePath,
            color =
                whiteOutlineColor,
            style = Stroke(
                width =
                    outlineWidthPx,
                join =
                    StrokeJoin.Round
            )
        )
    }
}