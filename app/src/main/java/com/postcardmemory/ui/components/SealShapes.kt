package com.postcardmemory.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.postcardmemory.R
import com.postcardmemory.ui.detail.SealType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 미니 스탬프(고양이 발바닥 등)는 벡터/Path 대신 res/drawable 이미지를 틴트해서 그린다. */
private fun sealImageRes(type: SealType): Int? =
    when (type) {
        SealType.DOG_PAW -> R.drawable.seal_dog_paw
        SealType.PIGEON_TRACK -> R.drawable.seal_pigeon_track
        SealType.HEART -> R.drawable.seal_heart
        SealType.STAR_STAMP -> R.drawable.seal_star
        else -> null
    }

/** 도장 종류에 맞는 모양을 정사각형 영역 안에 그린다. 미리보기 전용(저장본은 PostcardImageExporter에서 별도로 그림) */
@Composable
fun SealPreviewContent(
    type: SealType,
    color: Color,
    capturedAtMillis: Long? = null,
    modifier: Modifier = Modifier
) {
    val imageRes = sealImageRes(type)

    if (imageRes != null) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color),
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
        return
    }

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.035f

        when (type) {
            SealType.CIRCLE_POSTMARK ->
                drawCirclePostmark(color, strokeWidth, capturedAtMillis)

            SealType.WAVE_CANCEL ->
                drawWaveCancel(color, strokeWidth)

            SealType.AIR_MAIL ->
                drawAirMail(color, strokeWidth)

            SealType.STAR ->
                drawSealStar(color, strokeWidth)

            else -> Unit
        }
    }
}

private fun DrawScope.drawCirclePostmark(
    color: Color,
    strokeWidth: Float,
    capturedAtMillis: Long?
) {
    val outerRadius = size.minDimension / 2f - strokeWidth
    val innerRadius = outerRadius * 0.72f
    val center = Offset(size.width / 2f, size.height / 2f)

    drawCircle(
        color = color,
        radius = outerRadius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    drawCircle(
        color = color,
        radius = innerRadius,
        center = center,
        style = Stroke(width = strokeWidth * 0.7f)
    )

    val tickCount = 16
    for (i in 0 until tickCount) {
        val angle = (2 * PI * i / tickCount).toFloat()
        val fromRadius = outerRadius * 0.86f
        val toRadius = outerRadius * 0.96f

        drawLine(
            color = color,
            start = Offset(
                center.x + fromRadius * cos(angle),
                center.y + fromRadius * sin(angle)
            ),
            end = Offset(
                center.x + toRadius * cos(angle),
                center.y + toRadius * sin(angle)
            ),
            strokeWidth = strokeWidth * 0.6f
        )
    }

    if (capturedAtMillis != null) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                isAntiAlias = true
                this.color = color.toArgbInt()
                typeface = Typeface.create(
                    Typeface.SANS_SERIF,
                    Typeface.BOLD
                )
                textSize = innerRadius * 0.42f
                textAlign = Paint.Align.CENTER
            }

            val dateText =
                PostcardDateFormat.formatIso(capturedAtMillis)

            val textY =
                center.y -
                        (paint.descent() + paint.ascent()) / 2f

            canvas.nativeCanvas.drawText(
                dateText,
                center.x,
                textY,
                paint
            )
        }
    }
}

private fun DrawScope.drawWaveCancel(
    color: Color,
    strokeWidth: Float
) {
    val lineCount = 4
    val waveHeight = size.height * 0.08f
    val waveWidth = size.width * 0.22f
    val spacing = size.height / (lineCount + 1)

    for (lineIndex in 1..lineCount) {
        val y = spacing * lineIndex
        val path = Path()
        path.moveTo(0f, y)

        var x = 0f
        var up = true
        while (x < size.width) {
            val nextX = (x + waveWidth).coerceAtMost(size.width)
            path.quadraticBezierTo(
                x + waveWidth / 2f,
                y + if (up) -waveHeight else waveHeight,
                nextX,
                y
            )
            x = nextX
            up = !up
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth)
        )
    }
}

private fun DrawScope.drawAirMail(
    color: Color,
    strokeWidth: Float
) {
    val inset = strokeWidth
    val cornerRadius = size.minDimension * 0.12f

    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(
            size.width - inset * 2,
            size.height - inset * 2
        ),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = strokeWidth)
    )

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            isAntiAlias = true
            this.color = color.toArgbInt()
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                Typeface.BOLD
            )
            textSize = size.height * 0.24f
            textAlign = Paint.Align.CENTER
        }

        val textY =
            size.height / 2f -
                    (paint.descent() + paint.ascent()) / 2f

        canvas.nativeCanvas.drawText(
            "AIR MAIL",
            size.width / 2f,
            textY,
            paint
        )
    }
}

private fun DrawScope.drawSealStar(
    color: Color,
    strokeWidth: Float
) {
    val outerRadius = size.minDimension / 2f - strokeWidth
    val center = Offset(size.width / 2f, size.height / 2f)

    drawCircle(
        color = color,
        radius = outerRadius,
        center = center,
        style = Stroke(width = strokeWidth)
    )

    val starOuterRadius = outerRadius * 0.72f
    val starInnerRadius = starOuterRadius * 0.42f
    val points = 5
    val path = Path()

    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) starOuterRadius else starInnerRadius
        val angle = (PI / points * i - PI / 2).toFloat()
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)

        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth * 0.8f)
    )
}

private fun Color.toArgbInt(): Int {
    return android.graphics.Color.argb(
        (alpha * 255f).toInt(),
        (red * 255f).toInt(),
        (green * 255f).toInt(),
        (blue * 255f).toInt()
    )
}
