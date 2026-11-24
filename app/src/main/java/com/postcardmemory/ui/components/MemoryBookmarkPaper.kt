package com.postcardmemory.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.postcardmemory.data.BookmarkDesign
import com.postcardmemory.data.BookmarkShape
import com.postcardmemory.data.BookmarkTrace
import kotlin.math.min

/** 목업의 세 가지 끝선을 비례 좌표로 보존해. 엽서 C2 윤곽과는 별개의 종이야. */
internal fun bookmarkOutline(shape: BookmarkShape, width: Float, height: Float): Path {
    val points = when (shape) {
        BookmarkShape.CLASSIC -> listOf(0f to 0f, 1f to 0f, 1f to 1f, .5f to .86f, 0f to 1f)
        BookmarkShape.HAND_CUT -> listOf(.04f to 0f, .98f to .01f, 1f to .28f, .97f to .58f,
            1f to .83f, .89f to .97f, .67f to .94f, .46f to 1f, .23f to .95f,
            0f to .97f, .03f to .63f, 0f to .25f)
        BookmarkShape.MINI_TAG -> listOf(0f to 0f, 1f to 0f, 1f to .89f, .85f to 1f,
            .15f to 1f, 0f to .89f)
    }
    return Path().apply {
        moveTo(points.first().first * width, points.first().second * height)
        points.drop(1).forEach { lineTo(it.first * width, it.second * height) }
        close()
    }
}

internal fun bookmarkHeightFraction(shape: BookmarkShape) = when (shape) {
    BookmarkShape.CLASSIC -> .60f
    BookmarkShape.HAND_CUT -> .52f
    BookmarkShape.MINI_TAG -> .36f
}

internal fun DrawScope.drawBookmarkPaper(design: BookmarkDesign, origin: Offset, paperSize: Size) {
    if (paperSize.width <= 0f || paperSize.height <= 0f) return
    translate(origin.x, origin.y) {
        val path = bookmarkOutline(design.shape, paperSize.width, paperSize.height)
        translate(.45.dp.toPx(), .45.dp.toPx()) { drawPath(path, Color(0x225B5046)) }
        drawPath(path, Color(design.paper.argb))
        clipPath(path) {
            repeat(14) { i ->
                val y = paperSize.height * (i + .5f) / 14f
                drawLine(Color(0x0C704E38), Offset(0f, y), Offset(paperSize.width, y - paperSize.width * .12f), .5.dp.toPx())
            }
            val ink = Color(0x99704E38)
            val center = Offset(paperSize.width * .5f, paperSize.height * .73f)
            when (design.trace) {
                BookmarkTrace.NONE -> Unit
                BookmarkTrace.POSTMARK -> {
                    val radius = paperSize.width * .25f
                    drawCircle(ink, radius, center, style = Stroke(.7.dp.toPx()))
                    drawCircle(ink.copy(alpha = .3f), radius * .75f, center, style = Stroke(.4.dp.toPx()))
                    drawLine(ink, center - Offset(radius * .35f, 0f), center + Offset(radius * .35f, 0f), .6.dp.toPx())
                    drawLine(ink, center - Offset(0f, radius * .35f), center + Offset(0f, radius * .35f), .6.dp.toPx())
                }
                BookmarkTrace.LINES -> repeat(3) { i ->
                    val y = center.y + (i - 1) * paperSize.width * .13f
                    drawLine(ink, Offset(paperSize.width * .24f, y), Offset(paperSize.width * .76f, y), .7.dp.toPx())
                }
            }
        }
    }
}

/** 배치 크기는 바꾸지 않고 사진 아래 날짜 전 6dp 여백 안에 끝만 내밀어. */
internal fun Modifier.memoryBookmarkBehind(design: BookmarkDesign?): Modifier = if (design == null) this else drawBehind {
    val edge = size.width
    val exposure = min(edge * .055f, 5.dp.toPx())
    val height = edge * bookmarkHeightFraction(design.shape)
    drawBookmarkPaper(design, Offset(edge * .72f, edge + exposure - height), Size(edge * .18f, height))
}
