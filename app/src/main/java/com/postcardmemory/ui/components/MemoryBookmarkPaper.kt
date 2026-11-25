package com.postcardmemory.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
        // 143일차 승인: 아이보리는 갤러리 선택 종이와 같은 색이라 가는 선으로 종이 끝을 구분해.
        drawPath(path, BookmarkEdgeInk, style = Stroke(.6.dp.toPx()))
    }
}

private val BookmarkEdgeInk = Color(0x6B5B5046)

/**
 * 갤러리에서 엽서 아래로 내미는 길이(엽서 한 변 대비). 날짜는 가운데에 있어 겹치지 않아.
 * 143일차 실기기 QA에서 "조금 더 잘 보이게" 요청으로 5.5%에서 늘렸어. 클래식 V자 파임보다 길어야
 * 종이가 이어져 보이고, 날짜 줄(6dp + 글자) 안에 머물러서 아래 카드에는 닿지 않아.
 */
internal const val BOOKMARK_EXPOSURE_FRACTION = .095f
internal const val BOOKMARK_WIDTH_FRACTION = .18f
internal const val BOOKMARK_LEFT_FRACTION = .72f

/** 엽서 한 변이 [edge]일 때 다 끼운 책갈피의 자리. 제작 화면과 갤러리가 같은 값을 써. */
internal fun insertedBookmarkBounds(shape: BookmarkShape, edge: Float): Rect {
    val height = edge * bookmarkHeightFraction(shape)
    return Rect(
        offset = Offset(edge * BOOKMARK_LEFT_FRACTION, edge * (1f + BOOKMARK_EXPOSURE_FRACTION) - height),
        size = Size(edge * BOOKMARK_WIDTH_FRACTION, height)
    )
}

/**
 * 엽서가 책갈피 위에 드리우는 옅은 그림자. 종이 윤곽 안에서 엽서 아래 변 바로 밑에만 깔려서
 * 사진에 붙인 스티커가 아니라 뒤에 끼운 종이로 보이게 해.
 */
internal fun DrawScope.drawPostcardShadeOnBookmark(
    design: BookmarkDesign,
    bounds: Rect,
    edge: Float,
    strength: Float = 1f
) {
    if (edge <= 0f || strength <= 0f) return
    val top = edge * .985f - bounds.top
    val depth = edge * .035f
    translate(bounds.left, bounds.top) {
        clipPath(bookmarkOutline(design.shape, bounds.width, bounds.height)) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0x4030251A).copy(alpha = .25f * strength), Color(0x0030251A)), startY = top, endY = top + depth
                ),
                topLeft = Offset(0f, top),
                size = Size(bounds.width, depth)
            )
        }
    }
}

/** 카드 배치 크기는 바꾸지 않고, 사진 뒤에서 종이 끝만 아래로 내밀어. */
internal fun Modifier.memoryBookmarkBehind(design: BookmarkDesign?): Modifier = if (design == null) this else drawBehind {
    val edge = size.width
    val bounds = insertedBookmarkBounds(design.shape, edge)
    drawBookmarkPaper(design, bounds.topLeft, bounds.size)
    drawPostcardShadeOnBookmark(design, bounds, edge)
}
