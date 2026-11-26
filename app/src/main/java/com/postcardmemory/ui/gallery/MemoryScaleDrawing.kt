package com.postcardmemory.ui.gallery

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.semantics.clearAndSetSemantics
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** 승인 HTML의 440×405 좌표계. 프레임 상태는 draw 단계에서만 읽는다. */
@Composable
internal fun MemoryScaleDrawing(
    count: Int,
    paperTile: ImageBitmap,
    elapsedMs: () -> Long,
    modifier: Modifier = Modifier
) {
    // 본체 비율은 유지하고 정착 더미 위/발 아래의 빈 여백만 잘라낸다.
    Box(modifier.aspectRatio(440f / 325f).clipToBounds().clearAndSetSemantics { }.drawWithCache {
        val metal = Color(0xFFA18E74)
        val edge = Color(0xFF66513E)
        val paper = Color(0xFFF6ECD9)
        val ink = Color(0xFF493725)
        val needleInk = Color(0xFF704634)
        val outline = Stroke(2f)
        val thin = Stroke(1f)
        val capacity = memoryScaleCapacity(count)
        val body = Path().apply {
            moveTo(159f, 175f); quadraticBezierTo(220f, 157f, 281f, 175f)
            lineTo(310f, 360f); lineTo(130f, 360f); close()
        }
        val tray = Path().apply {
            moveTo(101f, 120f); lineTo(339f, 120f); lineTo(329f, 132f); lineTo(111f, 132f); close()
        }
        val needle = Path().apply {
            moveTo(217f, 281f); lineTo(218f, 218f); lineTo(220f, 201f)
            lineTo(222f, 218f); lineTo(223f, 281f); close()
            moveTo(216f, 280f); lineTo(224f, 280f); lineTo(220f, 290f); close()
        }
        val dialClip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(140f, 189f, 300f, 349f)) }
        val paperBrush = ShaderBrush(ImageShader(paperTile, TileMode.Repeated, TileMode.Repeated))
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(73, 55, 37)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        val faces = (0..12).map { layers ->
            val i = (layers - 1).coerceAtLeast(0)
            val x = 145f + (i % 3 - 1) * 1.8f
            val y = 120f - i * 3
            Path().apply {
                moveTo(x, y - 3); lineTo(x + 10, y - 22); lineTo(x + 143, y - 22)
                lineTo(x + 150, y - 3); close()
            }
        }
        val ticks = (0..40).map { i ->
            val radians = Math.toRadians(-120.0 + i * 6)
            val major = i % 4 == 0
            fun point(r: Float) = Offset(220 + sin(radians).toFloat() * r, 269 - cos(radians).toFloat() * r)
            Triple(point(if (major) 66f else 70f), point(76f), point(53f))
        }
        val labels = (0..40).map { i ->
            if (i % 4 != 0 || (capacity >= 1000 && i % 8 != 0)) null
            else {
                val value = capacity * i / 40
                when {
                    value >= 100_000_000L -> "${value / 100_000_000.0}억".replace(".0억", "억")
                    value >= 10_000L -> "${value / 10_000.0}만".replace(".0만", "만")
                    else -> String.format(Locale.KOREA, "%,d", value)
                }
            }
        }
        onDrawBehind {
            val frame = memoryScaleFrame(count, elapsedMs())
            scale(size.width / 440f, pivot = Offset.Zero) {
              translate(top = -65f) {
                fun text(value: String, x: Float, y: Float, fontSize: Float) {
                    textPaint.textSize = fontSize
                    drawIntoCanvas { it.nativeCanvas.drawText(value, x, y, textPaint) }
                }
                fun cards(layers: Int) {
                    if (layers == 0) return
                    repeat(layers) { i ->
                        val x = 145f + (i % 3 - 1) * 1.8f
                        val y = 120f - i * 3
                        drawRect(paper, Offset(x, y - 3), Size(150f, 3f))
                        drawRect(edge, Offset(x, y - 3), Size(150f, 3f), style = thin)
                    }
                    drawPath(faces[layers], paper)
                    drawPath(faces[layers], edge, style = thin)
                    drawPath(faces[layers], paperBrush, alpha = .08f)
                    val i = layers - 1
                    val x = 145f + (i % 3 - 1) * 1.8f
                    val y = 120f - i * 3
                    drawLine(edge.copy(alpha = .4f), Offset(x + 76, y - 18), Offset(x + 76, y - 7), .6f)
                    drawLine(edge.copy(alpha = .4f), Offset(x + 88, y - 12), Offset(x + 119, y - 12), .6f)
                    drawLine(edge.copy(alpha = .4f), Offset(x + 88, y - 8), Offset(x + 112, y - 8), .6f)
                    drawRect(edge.copy(alpha = .5f), Offset(x + 119, y - 19), Size(13f, 8f), style = thin)
                }
                drawOval(Color.Black.copy(alpha = .09f), Offset(95f, 374f), Size(250f, 14f))
                drawPath(body, metal); drawPath(body, edge, style = outline)
                clipPath(body) {
                    repeat(28) { i ->
                        val y = 176f + i * 7
                        drawLine(edge.copy(alpha = .08f), Offset(130f, y), Offset(310f, y - 10), .45f)
                    }
                }
                drawRoundRect(metal, Offset(116f, 359f), Size(208f, 17f), CornerRadius(5f))
                drawRoundRect(edge, Offset(116f, 359f), Size(208f, 17f), CornerRadius(5f), style = outline)
                drawRoundRect(edge, Offset(146f, 376f), Size(25f, 5f), CornerRadius(2f))
                drawRoundRect(edge, Offset(269f, 376f), Size(25f, 5f), CornerRadius(2f))
                // 피스톤과 받침대는 같은 하중의 trayY를 사용한다.
                drawRect(edge, Offset(205f, 127f + frame.trayY), Size(30f, 61f - frame.trayY))
                drawRect(metal, Offset(211f, 127f + frame.trayY), Size(18f, 61f - frame.trayY))
                translate(top = frame.trayY) {
                    drawPath(tray, metal); drawPath(tray, edge, style = outline)
                    drawLine(paper.copy(alpha = .65f), Offset(112f, 123f), Offset(328f, 123f), 1f)
                    if (frame.layers > 0) drawOval(edge.copy(alpha = .18f), Offset(128f, 117f), Size(184f, 4f))
                    cards(frame.layers)
                }
                drawCircle(paper, 85f, Offset(220f, 269f))
                drawCircle(edge, 85f, Offset(220f, 269f), style = Stroke(3f))
                clipPath(dialClip) { drawRect(paperBrush, Offset(140f, 189f), Size(160f, 160f), alpha = .06f) }
                ticks.forEachIndexed { i, tick ->
                    drawLine(ink, tick.first, tick.second, 1f)
                    labels[i]?.let { text(it, tick.third.x, tick.third.y + 3, 9f) }
                }
                text("엽서 · 장", 220f, 315f, 11f)
                rotate(frame.needleDegrees, pivot = Offset(220f, 269f)) { drawPath(needle, needleInk) }
                drawCircle(edge, 7f, Offset(220f, 269f)); drawCircle(paper, 3f, Offset(220f, 269f))
                text("M E M O R Y  P O S T  O F F I C E", 220f, 348f, 8f)
                if (frame.falling) translate(top = frame.trayY + frame.dropY) { cards(count.coerceIn(0, 12)) }
              }
            }
        }
    })
}
