package com.postcardmemory.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.postcardmemory.ui.theme.BrutalYellow

@Composable
fun StampOverlay(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.size(240.dp, 320.dp)
    ) {
        val strokePx = 3.dp.toPx()
        val perfSizePx = 10.dp.toPx()
        val halfPerf = perfSizePx / 2f
        val inset = halfPerf + strokePx

        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset

        val path = Path()

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
            color = BrutalYellow,
            style = Stroke(width = strokePx)
        )
    }
}
