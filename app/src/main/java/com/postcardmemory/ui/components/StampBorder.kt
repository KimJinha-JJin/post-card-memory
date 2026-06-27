package com.postcardmemory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.postcardmemory.ui.theme.BrutalBlack

@Composable
fun StampBorderCanvas(
    modifier: Modifier = Modifier,
    borderColor: Color = BrutalBlack,
    backgroundColor: Color = Color.White,
    strokeWidth: Dp = 3.dp,
    perfSize: Dp = 8.dp
) {
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val perfSizePx = perfSize.toPx()
        val halfPerf = perfSizePx / 2f
        val inset = halfPerf + strokePx

        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset

        // Draw background
        if (backgroundColor != Color.Transparent) {
            drawRect(
                color = backgroundColor,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top)
            )
        }

        // Build perforated path
        val path = Path()

        // Top edge: left to right with semicircle notches on top
        val topEdgeWidth = right - left
        val topSteps = (topEdgeWidth / perfSizePx).toInt()
        val topSpacing = topEdgeWidth / topSteps

        path.moveTo(left, top)
        for (i in 0 until topSteps) {
            val cx = left + i * topSpacing + topSpacing / 2f
            path.lineTo(cx - halfPerf, top)
            path.arcTo(
                rect = Rect(cx - halfPerf, top - perfSizePx, cx + halfPerf, top),
                startAngleDegrees = 180f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
        path.lineTo(right, top)

        // Right edge: top to bottom with semicircle notches on right
        val rightEdgeHeight = bottom - top
        val rightSteps = (rightEdgeHeight / perfSizePx).toInt()
        val rightSpacing = rightEdgeHeight / rightSteps

        for (i in 0 until rightSteps) {
            val cy = top + i * rightSpacing + rightSpacing / 2f
            path.lineTo(right, cy - halfPerf)
            path.arcTo(
                rect = Rect(right, cy - halfPerf, right + perfSizePx, cy + halfPerf),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
        path.lineTo(right, bottom)

        // Bottom edge: right to left with semicircle notches on bottom
        val bottomEdgeWidth = right - left
        val bottomSteps = (bottomEdgeWidth / perfSizePx).toInt()
        val bottomSpacing = bottomEdgeWidth / bottomSteps

        for (i in 0 until bottomSteps) {
            val cx = right - i * bottomSpacing - bottomSpacing / 2f
            path.lineTo(cx + halfPerf, bottom)
            path.arcTo(
                rect = Rect(cx - halfPerf, bottom, cx + halfPerf, bottom + perfSizePx),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
        path.lineTo(left, bottom)

        // Left edge: bottom to top with semicircle notches on left
        val leftEdgeHeight = bottom - top
        val leftSteps = (leftEdgeHeight / perfSizePx).toInt()
        val leftSpacing = leftEdgeHeight / leftSteps

        for (i in 0 until leftSteps) {
            val cy = bottom - i * leftSpacing - leftSpacing / 2f
            path.lineTo(left, cy + halfPerf)
            path.arcTo(
                rect = Rect(left - perfSizePx, cy - halfPerf, left, cy + halfPerf),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
        }
        path.lineTo(left, top)
        path.close()

        drawPath(
            path = path,
            color = borderColor,
            style = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
