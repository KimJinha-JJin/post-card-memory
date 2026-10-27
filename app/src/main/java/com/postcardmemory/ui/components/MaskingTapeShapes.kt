package com.postcardmemory.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import com.postcardmemory.ui.detail.MASKING_TAPE_DOT_RADIUS_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_GRID_LINE_WIDTH_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_HEART_SIZE_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_PATTERN_ALPHA
import com.postcardmemory.ui.detail.MASKING_TAPE_PATTERN_PITCH_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_PHOTO_ALPHA
import com.postcardmemory.ui.detail.MASKING_TAPE_PLAIN_FIBER_ALPHA
import com.postcardmemory.ui.detail.MASKING_TAPE_STAR_SIZE_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_STRIPE_PITCH_RATIO
import com.postcardmemory.ui.detail.MASKING_TAPE_STRIPE_WIDTH_RATIO
import com.postcardmemory.ui.detail.MaskingTapeEdgeStyle
import com.postcardmemory.ui.detail.MaskingTapeItem
import com.postcardmemory.ui.detail.MaskingTapePatternKind
import com.postcardmemory.ui.detail.MaskingTapeStyle
import com.postcardmemory.ui.detail.effectiveBaseColorArgb
import com.postcardmemory.ui.detail.effectivePatternColorArgb
import com.postcardmemory.ui.detail.effectivePatternKind
import com.postcardmemory.ui.detail.maskingTapeOutlinePoints
import com.postcardmemory.utils.MaskingTapePhotoDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 종이 테이프 한 조각을 붙인 듯한 모양(가장자리는 [MaskingTapeItem.edgeStyle]
 * 에 따라 다름)에 디자인별 패턴 또는 사용자 사진을 그린다. 미리보기 전용
 * (저장본은 PostcardImageExporter의 drawMaskingTapeOverlay가 같은 좌표
 * 함수·비율 상수로 별도로 그림).
 */
@Composable
fun MaskingTapeContent(
    tape: MaskingTapeItem,
    modifier: Modifier = Modifier
) {
    val photoBitmap =
        if (tape.style == MaskingTapeStyle.PHOTO && tape.photoUri != null) {
            rememberMaskingTapePhotoBitmap(tape.photoUri)
        } else {
            null
        }

    Canvas(modifier = modifier) {
        val outline = maskingTapeClipPath(size.width, size.height, tape.edgeStyle)

        clipPath(outline) {
            if (tape.style == MaskingTapeStyle.PHOTO) {
                if (photoBitmap != null) {
                    drawTiledPhoto(photoBitmap)
                } else {
                    // 디코딩 전(첫 프레임)이나 실패했을 때도 빈 채로 두지 않는다.
                    drawRect(
                        color = Color(MaskingTapeStyle.PHOTO.baseColorArgb)
                            .copy(alpha = MASKING_TAPE_PHOTO_ALPHA)
                    )
                }
            } else {
                drawRect(
                    color = Color(tape.effectiveBaseColorArgb()).copy(alpha = tape.style.alpha)
                )
                drawMaskingTapePattern(tape)
            }
        }
    }
}

/**
 * 프리셋 디자인 미리보기(선택 그리드의 작은 스와치)를 위한 오버로드.
 * 실제로 붙인 테이프가 아니므로 edgeStyle/길이 등은 기본값을 쓴다.
 */
@Composable
fun MaskingTapeContent(
    style: MaskingTapeStyle,
    modifier: Modifier = Modifier
) {
    MaskingTapeContent(tape = MaskingTapeItem(style = style), modifier = modifier)
}

@Composable
private fun rememberMaskingTapePhotoBitmap(uri: android.net.Uri): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(uri) {
        bitmap = withContext(Dispatchers.IO) {
            MaskingTapePhotoDecoder.decodeSampledBitmap(context, uri)
        }
    }

    return bitmap
}

private fun DrawScope.drawTiledPhoto(bitmap: Bitmap) {
    // 타일 한 칸이 테이프 두께와 비슷한 정사각형이 되게 해 "질감처럼 반복"되는
    // 느낌을 준다 — 사진 한 장을 통째로 늘려 붙인 모습과는 다르다.
    val tileSize = size.height

    if (tileSize <= 0f || bitmap.width <= 0 || bitmap.height <= 0) return

    drawIntoCanvas { canvas ->
        val matrix = Matrix().apply {
            setScale(
                tileSize / bitmap.width,
                tileSize / bitmap.height
            )
        }

        val shader = BitmapShader(
            bitmap,
            Shader.TileMode.REPEAT,
            Shader.TileMode.REPEAT
        ).apply { setLocalMatrix(matrix) }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.shader = shader
            alpha = (MASKING_TAPE_PHOTO_ALPHA * 255).toInt()
        }

        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
    }
}

private fun DrawScope.maskingTapeClipPath(
    width: Float,
    height: Float,
    edgeStyle: MaskingTapeEdgeStyle
): Path {
    val points = maskingTapeOutlinePoints(width, height, edgeStyle)
    if (points.isEmpty()) return Path()

    return Path().apply {
        moveTo(points[0].first, points[0].second)
        for (i in 1 until points.size) {
            lineTo(points[i].first, points[i].second)
        }
        close()
    }
}

private fun DrawScope.drawMaskingTapePattern(tape: MaskingTapeItem) {
    val patternColor = Color(tape.effectivePatternColorArgb())

    when (tape.effectivePatternKind()) {
        MaskingTapePatternKind.GRID -> drawTapeGrid(patternColor)
        MaskingTapePatternKind.DOT -> drawTapeDots(patternColor)
        MaskingTapePatternKind.STAR -> drawTapeStars(patternColor)
        MaskingTapePatternKind.HEART -> drawTapeHearts(patternColor)
        MaskingTapePatternKind.STRIPE -> drawTapeStripes(patternColor)
        MaskingTapePatternKind.PLAIN -> drawTapeFiber(patternColor)
    }
}

private fun DrawScope.drawTapeGrid(color: Color) {
    val pitch = size.height * MASKING_TAPE_PATTERN_PITCH_RATIO
    val lineWidth = size.height * MASKING_TAPE_GRID_LINE_WIDTH_RATIO
    val lineColor = color.copy(alpha = MASKING_TAPE_PATTERN_ALPHA)

    var x = pitch / 2f
    while (x < size.width) {
        drawLine(
            color = lineColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = lineWidth
        )
        x += pitch
    }

    var y = pitch / 2f
    while (y < size.height) {
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = lineWidth
        )
        y += pitch
    }
}

private fun DrawScope.drawTapeDots(color: Color) {
    val pitch = size.height * MASKING_TAPE_PATTERN_PITCH_RATIO
    val radius = size.height * MASKING_TAPE_DOT_RADIUS_RATIO
    val dotColor = color.copy(alpha = MASKING_TAPE_PATTERN_ALPHA)

    forEachTapeGridPoint(pitch) { x, y ->
        drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
    }
}

private fun DrawScope.drawTapeStars(color: Color) {
    val pitch = size.height * MASKING_TAPE_PATTERN_PITCH_RATIO
    val starSize = size.height * MASKING_TAPE_STAR_SIZE_RATIO
    val starColor = color.copy(alpha = MASKING_TAPE_PATTERN_ALPHA)
    val strokeWidth = starSize * 0.16f
    val half = starSize / 2f
    val diag = half * 0.6f

    forEachTapeGridPoint(pitch) { x, y ->
        drawLine(
            color = starColor,
            start = Offset(x - half, y),
            end = Offset(x + half, y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = starColor,
            start = Offset(x, y - half),
            end = Offset(x, y + half),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = starColor,
            start = Offset(x - diag, y - diag),
            end = Offset(x + diag, y + diag),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = starColor,
            start = Offset(x - diag, y + diag),
            end = Offset(x + diag, y - diag),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawTapeHearts(color: Color) {
    val pitch = size.height * MASKING_TAPE_PATTERN_PITCH_RATIO
    val heartSize = size.height * MASKING_TAPE_HEART_SIZE_RATIO
    val heartColor = color.copy(alpha = MASKING_TAPE_PATTERN_ALPHA)

    forEachTapeGridPoint(pitch) { x, y ->
        drawPath(
            path = tapeHeartPath(centerX = x, centerY = y, heartSize = heartSize),
            color = heartColor
        )
    }
}

private fun tapeHeartPath(
    centerX: Float,
    centerY: Float,
    heartSize: Float
): Path {
    val half = heartSize / 2f
    val top = centerY - half * 0.5f

    return Path().apply {
        moveTo(centerX, centerY + half)
        cubicTo(
            centerX - half * 1.3f, centerY,
            centerX - half * 0.6f, top - half * 0.6f,
            centerX, top
        )
        cubicTo(
            centerX + half * 0.6f, top - half * 0.6f,
            centerX + half * 1.3f, centerY,
            centerX, centerY + half
        )
        close()
    }
}

private fun DrawScope.drawTapeStripes(color: Color) {
    val pitch = size.height * MASKING_TAPE_STRIPE_PITCH_RATIO
    val strokeWidth = size.height * MASKING_TAPE_STRIPE_WIDTH_RATIO
    val stripeColor = color.copy(alpha = MASKING_TAPE_PATTERN_ALPHA)
    val diagonalSpan = size.width + size.height

    var offset = -size.height
    while (offset < diagonalSpan) {
        drawLine(
            color = stripeColor,
            start = Offset(offset, 0f),
            end = Offset(offset + size.height, size.height),
            strokeWidth = strokeWidth
        )
        offset += pitch
    }
}

/** 반투명 아이보리·커스텀(무늬 없음)처럼 도안 없이 은은한 종이 결만 살짝 비치게 한다. */
private fun DrawScope.drawTapeFiber(color: Color) {
    val fiberColor = color.copy(alpha = MASKING_TAPE_PLAIN_FIBER_ALPHA)
    val lineWidth = size.height * 0.03f

    listOf(0.32f, 0.68f).forEach { yRatio ->
        drawLine(
            color = fiberColor,
            start = Offset(size.width * 0.08f, size.height * yRatio),
            end = Offset(size.width * 0.92f, size.height * yRatio),
            strokeWidth = lineWidth
        )
    }
}

/** 테이프 영역 안에서 격자 형태로 촘촘히 엇갈린 도안 반복 위치를 순회한다(체크무늬 배치). */
private inline fun DrawScope.forEachTapeGridPoint(
    pitch: Float,
    action: (x: Float, y: Float) -> Unit
) {
    var row = 0
    var y = pitch / 2f
    while (y < size.height) {
        val rowOffset = if (row % 2 == 0) 0f else pitch / 2f
        var x = pitch / 2f + rowOffset
        while (x < size.width) {
            action(x, y)
            x += pitch
        }
        y += pitch
        row++
    }
}
