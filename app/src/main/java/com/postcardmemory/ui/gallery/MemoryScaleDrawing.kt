package com.postcardmemory.ui.gallery

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
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
import kotlin.random.Random

// 145일차 B안 "오래된 우체국" 재질. 모양 좌표와 모션은 144일차 그대로이고 색·명암만 담는다.
private val ScaleEdge = Color(0xFF66513E)
private val ScaleLight = Color(0xFFF6EAD2)
private val ScaleWarmLight = Color(0xFFFFF6E4)
private val ScaleShade = Color(0xFF20140B)
private val ScaleSoot = Color(0xFF2A1D12)
private val ScaleBodyTop = Color(0xFFA8957A)
private val ScaleBodyBottom = Color(0xFF86735B)
private val ScaleTrayTop = Color(0xFFC8B899)
private val ScaleTrayFront = Color(0xFF938068)
private val ScaleTrayDark = Color(0xFF76644F)
private val ScaleFoot = Color(0xFF45362A)
private val ScaleDialPaper = Color(0xFFF5EBD7)
private val ScaleDialVignette = Color(0xFF5A3C1E)
private val ScaleCardFace = Color(0xFFF7EEDD)
private val ScaleCardTints = listOf(Color(0xFFF6ECD9), Color(0xFFEEE1C8), Color(0xFFF3E8D4), Color(0xFFE9DAC0))
private val ScaleStamp = Color(0xFFB5674C)
private val ScaleInk = Color(0xFF3D2C1D)
private val ScaleNeedle = Color(0xFF5F3A2A)
private const val SCALE_OUTLINE_ALPHA = .7f
private const val SCALE_BEVEL_ALPHA = .55f
private const val SCALE_SPECK_ALPHA = .55f
private const val SCALE_DIAL_TILE_ALPHA = .42f
private const val SCALE_CARD_TILE_ALPHA = .32f
private const val SCALE_SPECK_UNITS = 24f
private const val SCALE_SPECK_PX = 64

/**
 * 무광 도장 알갱이 한 칸. 고정 시드로 composition당 한 번만 그리고 ImageShader로 반복한다.
 * 프레임마다 만들지 않으며 사용자 사진·DB와 무관하다. 가장자리 점은 이웃 칸에도 그려 이음새를 없앤다.
 */
internal fun memoryScaleSpeckleTile(): ImageBitmap {
    val image = ImageBitmap(SCALE_SPECK_PX, SCALE_SPECK_PX)
    val canvas = Canvas(image)
    val unit = SCALE_SPECK_PX / SCALE_SPECK_UNITS
    val random = Random(145)
    val paint = androidx.compose.ui.graphics.Paint().apply { isAntiAlias = true }
    fun dots(n: Int, color: Color, radius: ClosedFloatingPointRange<Float>, alpha: ClosedFloatingPointRange<Float>) = repeat(n) {
        val x = random.nextFloat() * SCALE_SPECK_PX
        val y = random.nextFloat() * SCALE_SPECK_PX
        val r = (radius.start + random.nextFloat() * (radius.endInclusive - radius.start)) * unit
        paint.color = color.copy(alpha = alpha.start + random.nextFloat() * (alpha.endInclusive - alpha.start))
        for (dx in -1..1) for (dy in -1..1) canvas.drawCircle(Offset(x + dx * SCALE_SPECK_PX, y + dy * SCALE_SPECK_PX), r, paint)
    }
    dots(26, Color(0xFF2B1D12), .25f..0.6f, .25f..0.6f)
    dots(10, Color(0xFFFFF4DC), .3f..0.6f, .3f..0.6f)
    return image
}

/** 이미지를 그림 좌표에서 [units] 크기로, 가로·세로 같은 배율로만 반복한다. */
private fun repeatedBrush(image: ImageBitmap, units: Float): ShaderBrush {
    val shader = ImageShader(image, TileMode.Repeated, TileMode.Repeated)
    shader.setLocalMatrix(android.graphics.Matrix().apply { setScale(units / image.width, units / image.width) })
    return ShaderBrush(shader)
}

private class MemoryScaleTick(val inner: Offset, val outer: Offset, val labelAt: Offset, val major: Boolean, val label: String?)

/** 승인 HTML의 440×405 좌표계. 프레임 상태는 draw 단계에서만 읽는다. */
@Composable
internal fun MemoryScaleDrawing(
    count: Int,
    paperTile: ImageBitmap,
    elapsedMs: () -> Long,
    modifier: Modifier = Modifier
) {
    val speckle = remember { memoryScaleSpeckleTile() }
    // 본체 비율은 유지하고 정착 더미 위/발 아래의 빈 여백만 잘라낸다.
    Box(modifier.aspectRatio(440f / 325f).clipToBounds().clearAndSetSemantics { }.drawWithCache {
        val edge = ScaleEdge
        val paper = ScaleDialPaper
        val outline = Stroke(1f)
        val capacity = memoryScaleCapacity(count)
        val body = Path().apply {
            moveTo(159f, 175f); quadraticBezierTo(220f, 157f, 281f, 175f)
            lineTo(310f, 360f); lineTo(130f, 360f); close()
        }
        val bodyBevel = Path().apply { moveTo(159f, 177f); quadraticBezierTo(220f, 159f, 281f, 177f) }
        val tray = Path().apply {
            moveTo(101f, 120f); lineTo(339f, 120f); lineTo(329f, 132f); lineTo(111f, 132f); close()
        }
        val needle = Path().apply {
            moveTo(217f, 281f); lineTo(218f, 218f); lineTo(220f, 201f)
            lineTo(222f, 218f); lineTo(223f, 281f); close()
            moveTo(216f, 280f); lineTo(224f, 280f); lineTo(220f, 290f); close()
        }
        val needleGleam = Path().apply { moveTo(219.3f, 279f); lineTo(219.5f, 214f); lineTo(220f, 204f) }
        // 종이 결은 갤러리 배경과 같은 화면 크기(360dp)로 되돌린다. 그림 좌표 배율을 나눠 준다.
        val drawScale = size.width / 440f
        val paperBrush = repeatedBrush(paperTile, GALLERY_PAPER_TILE_SIZE.toPx() / drawScale)
        val speckBrush = repeatedBrush(speckle, SCALE_SPECK_UNITS)
        val bodyBrush = Brush.verticalGradient(listOf(ScaleBodyTop, ScaleBodyBottom), startY = 158f, endY = 360f)
        val sideBrush = Brush.horizontalGradient(
            0f to ScaleWarmLight.copy(alpha = .16f), .32f to ScaleWarmLight.copy(alpha = 0f),
            .68f to ScaleShade.copy(alpha = 0f), 1f to ScaleShade.copy(alpha = .17f), startX = 130f, endX = 310f
        )
        val plinthBrush = Brush.verticalGradient(listOf(ScaleBodyTop, ScaleTrayDark), startY = 359f, endY = 376f)
        // 피스톤은 가로 방향 명암만 쓴다 — 길이가 바뀌어도 무늬가 늘어나 보이지 않는다.
        val sleeveBrush = Brush.horizontalGradient(
            0f to Color(0xFF4F3F30), .35f to Color(0xFF8A775E), .5f to Color(0xFF9C8A70),
            .8f to Color(0xFF6E5C47), 1f to Color(0xFF45362A), startX = 205f, endX = 235f
        )
        val rodBrush = Brush.horizontalGradient(
            0f to Color(0xFF76644E), .3f to Color(0xFFB3A285), .42f to Color(0xFFC8B99B),
            .65f to Color(0xFF9C896E), 1f to Color(0xFF64523F), startX = 211f, endX = 229f
        )
        val collarBrush = Brush.verticalGradient(listOf(Color(0xFFB9A789), Color(0xFF5F4D3B)), startY = 163f, endY = 171f)
        val channelBrush = Brush.verticalGradient(listOf(ScaleShade.copy(alpha = .35f), ScaleShade.copy(alpha = 0f)), startY = 168f, endY = 192f)
        val trayBrush = Brush.verticalGradient(
            0f to ScaleTrayTop, .24f to ScaleTrayTop, .26f to ScaleTrayFront, 1f to ScaleTrayDark, startY = 120f, endY = 132f
        )
        val vignetteBrush = Brush.radialGradient(
            .62f to ScaleDialVignette.copy(alpha = 0f), 1f to ScaleDialVignette.copy(alpha = .10f),
            center = Offset(220f, 269f), radius = 85f
        )
        val bezelBrush = Brush.linearGradient(
            0f to Color(0xFFB09A78), .5f to Color(0xFF6C5641), 1f to Color(0xFF46362A),
            start = Offset(160f, 190f), end = Offset(280f, 350f)
        )
        val groundBrush = Brush.radialGradient(listOf(ScaleSoot.copy(alpha = .17f), ScaleSoot.copy(alpha = 0f)), center = Offset(220f, 381f), radius = 128f)
        val contactBrush = Brush.radialGradient(listOf(ScaleSoot.copy(alpha = .24f), ScaleSoot.copy(alpha = 0f)), center = Offset(220f, 120f), radius = 82f)
        val hubBrush = Brush.radialGradient(
            0f to Color(0xFFD6C3A0), .55f to Color(0xFF7A6450), 1f to Color(0xFF3F3025),
            center = Offset(217.6f, 266.6f), radius = 8.5f
        )
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(61, 44, 29)
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
            val label = if (!major || (capacity >= 1000 && i % 8 != 0)) null else {
                val value = capacity * i / 40
                when {
                    value >= 100_000_000L -> "${value / 100_000_000.0}억".replace(".0억", "억")
                    value >= 10_000L -> "${value / 10_000.0}만".replace(".0만", "만")
                    else -> String.format(Locale.KOREA, "%,d", value)
                }
            }
            MemoryScaleTick(point(if (major) 66f else 70f), point(76f), point(53f), major, label)
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
                    val top = layers - 1
                    val tx = 145f + (top % 3 - 1) * 1.8f
                    val ty = 120f - top * 3
                    drawRect(ScaleShade.copy(alpha = .10f), Offset(147.5f, ty - 2), Size(151f, 122f - ty))
                    repeat(layers) { i ->
                        val x = 145f + (i % 3 - 1) * 1.8f
                        val y = 120f - i * 3
                        drawRect(ScaleCardTints[i % ScaleCardTints.size], Offset(x, y - 3), Size(150f, 3f))
                        drawLine(edge.copy(alpha = .28f), Offset(x, y - .3f), Offset(x + 150, y - .3f), .6f)
                    }
                    drawPath(faces[layers], ScaleCardFace)
                    drawPath(faces[layers], paperBrush, alpha = SCALE_CARD_TILE_ALPHA)
                    drawPath(faces[layers], edge.copy(alpha = .45f), style = Stroke(.7f))
                    drawLine(edge.copy(alpha = .4f), Offset(tx + 76, ty - 18), Offset(tx + 76, ty - 7), .6f)
                    drawLine(edge.copy(alpha = .4f), Offset(tx + 88, ty - 12), Offset(tx + 119, ty - 12), .6f)
                    drawLine(edge.copy(alpha = .4f), Offset(tx + 88, ty - 8), Offset(tx + 112, ty - 8), .6f)
                    drawRect(ScaleStamp.copy(alpha = .16f), Offset(tx + 119, ty - 19), Size(13f, 8f))
                    drawRect(edge.copy(alpha = .5f), Offset(tx + 119, ty - 19), Size(13f, 8f), style = Stroke(.6f))
                }
                // 바닥: 가운데 진하고 바깥으로 사라지는 그림자 + 발 아래 접촉 그림자
                scale(1f, .07f, pivot = Offset(220f, 381f)) { drawCircle(groundBrush, 128f, Offset(220f, 381f)) }
                drawOval(ScaleSoot.copy(alpha = .12f), Offset(116f, 375.3f), Size(208f, 4.4f))
                // 본체: 위→아래 명암, 좌우 빛, 도장 알갱이, 윗곡선 반사·좌우 모서리, 얇은 외곽선
                drawPath(body, bodyBrush); drawPath(body, sideBrush)
                drawPath(body, speckBrush, alpha = SCALE_SPECK_ALPHA)
                clipPath(body) {
                    drawPath(bodyBevel, ScaleLight.copy(alpha = SCALE_BEVEL_ALPHA), style = Stroke(1.6f))
                    drawLine(ScaleLight.copy(alpha = .30f), Offset(160.6f, 178f), Offset(132f, 360f), 1.4f)
                    drawLine(ScaleShade.copy(alpha = .25f), Offset(279.4f, 178f), Offset(308f, 360f), 1.6f)
                    drawRect(channelBrush, Offset(201f, 166f), Size(38f, 26f))
                }
                drawPath(body, edge.copy(alpha = SCALE_OUTLINE_ALPHA), style = outline)
                drawRoundRect(plinthBrush, Offset(116f, 359f), Size(208f, 17f), CornerRadius(5f))
                drawRoundRect(speckBrush, Offset(116f, 359f), Size(208f, 17f), CornerRadius(5f), alpha = SCALE_SPECK_ALPHA)
                drawLine(ScaleLight.copy(alpha = SCALE_BEVEL_ALPHA), Offset(121f, 360.6f), Offset(319f, 360.6f), 1.1f)
                drawRoundRect(edge.copy(alpha = SCALE_OUTLINE_ALPHA), Offset(116f, 359f), Size(208f, 17f), CornerRadius(5f), style = outline)
                drawRoundRect(ScaleFoot, Offset(146f, 376f), Size(25f, 5f), CornerRadius(2f))
                drawRoundRect(ScaleFoot, Offset(269f, 376f), Size(25f, 5f), CornerRadius(2f))
                // 피스톤과 받침대는 같은 하중의 trayY를 사용한다.
                drawRect(sleeveBrush, Offset(205f, 127f + frame.trayY), Size(30f, 61f - frame.trayY))
                drawRect(rodBrush, Offset(211f, 127f + frame.trayY), Size(18f, 61f - frame.trayY))
                // 본체 입구 칼라는 고정 부품이다.
                drawRoundRect(collarBrush, Offset(200f, 163.5f), Size(40f, 7.5f), CornerRadius(1.5f))
                drawRoundRect(edge.copy(alpha = SCALE_OUTLINE_ALPHA), Offset(200f, 163.5f), Size(40f, 7.5f), CornerRadius(1.5f), style = Stroke(.8f))
                drawLine(ScaleLight.copy(alpha = SCALE_BEVEL_ALPHA), Offset(202f, 164.6f), Offset(238f, 164.6f), .8f)
                translate(top = frame.trayY) {
                    drawRect(ScaleShade.copy(alpha = .22f), Offset(205f, 132f), Size(30f, 3.2f))
                    drawPath(tray, trayBrush)
                    drawPath(tray, speckBrush, alpha = SCALE_SPECK_ALPHA)
                    drawLine(ScaleLight.copy(alpha = SCALE_BEVEL_ALPHA), Offset(103f, 120.6f), Offset(337f, 120.6f), 1f)
                    drawPath(tray, edge.copy(alpha = SCALE_OUTLINE_ALPHA), style = outline)
                    if (frame.layers > 0) scale(1f, .05f, pivot = Offset(220f, 120f)) { drawCircle(contactBrush, 82f, Offset(220f, 120f)) }
                    cards(frame.layers)
                }
                // 눈금판: 종이 + 결 + 가장자리 → 금속 링 → 안쪽 그늘
                drawCircle(paper, 85f, Offset(220f, 269f))
                drawCircle(paperBrush, 85f, Offset(220f, 269f), alpha = SCALE_DIAL_TILE_ALPHA)
                drawCircle(vignetteBrush, 85f, Offset(220f, 269f))
                drawCircle(bezelBrush, 82f, Offset(220f, 269f), style = Stroke(6f))
                drawCircle(Color(0xFF3A2716).copy(alpha = .18f), 78.4f, Offset(220f, 269f), style = Stroke(1.2f))
                drawCircle(Color(0xFF2B1D12).copy(alpha = .5f), 84.4f, Offset(220f, 269f), style = Stroke(.8f))
                ticks.forEach { tick ->
                    drawLine(ScaleInk, tick.inner, tick.outer, if (tick.major) 1.4f else .8f)
                    tick.label?.let { text(it, tick.labelAt.x, tick.labelAt.y + 3, 9f) }
                }
                text("엽서 · 장", 220f, 315f, 11f)
                // 빛은 고정이라 그림자는 화면 기준으로 비켜 두고 바늘과 같은 각도로 돈다.
                translate(1.5f, 2.2f) {
                    rotate(frame.needleDegrees, pivot = Offset(220f, 269f)) { drawPath(needle, ScaleShade.copy(alpha = .2f)) }
                }
                rotate(frame.needleDegrees, pivot = Offset(220f, 269f)) {
                    drawPath(needle, ScaleNeedle)
                    drawPath(needleGleam, Color(0xFFF3DCC6).copy(alpha = .35f), style = Stroke(.6f))
                }
                drawCircle(ScaleShade.copy(alpha = .22f), 7.4f, Offset(221.3f, 271f))
                drawCircle(hubBrush, 7f, Offset(220f, 269f))
                drawCircle(Color(0xFFE6D8BC), 2.6f, Offset(220f, 269f))
                drawLine(Color(0xFF5A4838), Offset(218.4f, 269.9f), Offset(221.6f, 268.1f), .7f)
                if (frame.falling) translate(top = frame.trayY + frame.dropY) { cards(count.coerceIn(0, 12)) }
              }
            }
        }
    })
}
