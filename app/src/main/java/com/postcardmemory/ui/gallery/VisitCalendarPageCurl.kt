package com.postcardmemory.ui.gallery

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import java.time.YearMonth

internal const val VISIT_CALENDAR_PAGE_TURN_DURATION_MS = 650

/** An active sheet never changes destination. Only the final request is replaced. */
internal data class VisitCalendarPageTurn(val from: YearMonth, val to: YearMonth) {
    val forward: Boolean get() = to > from
}

internal data class VisitCalendarPagingState(
    val settledMonth: YearMonth,
    val requestedMonth: YearMonth = settledMonth,
    val activeTurn: VisitCalendarPageTurn? = null
) {
    fun request(month: YearMonth): VisitCalendarPagingState = copy(requestedMonth = month)

    fun step(months: Long): VisitCalendarPagingState = request(requestedMonth.plusMonths(months))

    fun startNext(): VisitCalendarPagingState = when {
        activeTurn != null || settledMonth == requestedMonth -> this
        else -> copy(activeTurn = VisitCalendarPageTurn(settledMonth, requestedMonth))
    }

    fun complete(): VisitCalendarPagingState = activeTurn?.let {
        copy(settledMonth = it.to, activeTurn = null)
    } ?: this
}

/** Reused drawing geometry; no bitmap capture, polygon list or matrix allocation per frame. */
internal class VisitCalendarPageFold {
    var normalX = .76f
        private set
    val normalY = .6499231f
    var crease = 0f
        private set
    private var width = 0f
    private var height = 0f

    fun update(width: Float, height: Float, progress: Float, forward: Boolean, density: Float) {
        this.width = width.coerceAtLeast(0f)
        this.height = height.coerceAtLeast(0f)
        normalX = if (forward) .76f else -.76f
        val high = (if (forward) normalX * this.width else 0f) + normalY * this.height
        val low = if (forward) 0f else normalX * this.width
        // Same diagonal and travel as the approved A mockup (logical px -> dp).
        crease = high + density - progress.coerceIn(0f, 1f) * (high - low + 78.407075f * density)
    }

    fun signedDistance(x: Float, y: Float): Float = x * normalX + y * normalY - crease

    fun reflectedX(x: Float, y: Float): Float = x - 2f * signedDistance(x, y) * normalX

    fun reflectedY(x: Float, y: Float): Float = y - 2f * signedDistance(x, y) * normalY

    /** Clip the sheet rectangle against either side of the fold into a caller-owned buffer. */
    fun writePolygon(points: FloatArray, folded: Boolean): Int {
        require(points.size >= 12)
        if (width <= 0f || height <= 0f) return 0
        var count = 0
        var previousX = 0f
        var previousY = height
        var previousDistance = signedDistance(previousX, previousY)
        var previousInside = if (folded) previousDistance >= 0f else previousDistance <= 0f
        for (corner in 0..3) {
            val x = if (corner == 1 || corner == 2) width else 0f
            val y = if (corner >= 2) height else 0f
            val distance = signedDistance(x, y)
            val inside = if (folded) distance >= 0f else distance <= 0f
            if (inside != previousInside) {
                val fraction = previousDistance / (previousDistance - distance)
                points[count * 2] = previousX + fraction * (x - previousX)
                points[count * 2 + 1] = previousY + fraction * (y - previousY)
                count++
            }
            if (inside) {
                points[count * 2] = x
                points[count * 2 + 1] = y
                count++
            }
            previousX = x
            previousY = y
            previousDistance = distance
            previousInside = inside
        }
        return count
    }
}

private fun Path.setPolygon(points: FloatArray, count: Int) {
    reset()
    if (count == 0) return
    moveTo(points[0], points[1])
    for (index in 1 until count) lineTo(points[index * 2], points[index * 2 + 1])
    close()
}

/** The live printed subtree is replayed in both clips, so late visit markers are never frozen. */
internal fun Modifier.visitCalendarPageCurl(
    progress: () -> Float,
    forward: Boolean,
    month: YearMonth
): Modifier = drawWithCache {
    val fold = VisitCalendarPageFold()
    val points = FloatArray(12)
    val front = Path()
    val back = Path()
    val reflection = Matrix()
    val backColor = Color(0xFFF0EDE4).copy(alpha = .955f)
    val paperOutline = Path().apply {
        visitCalendarPaperEdgeOutline(
            widthDp = size.width / density,
            heightDp = size.height / density,
            seed = visitCalendarPaperEdgeSeed(month)
        ).forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x * density, point.y * density)
            else lineTo(point.x * density, point.y * density)
        }
        close()
    }
    onDrawWithContent {
        val amount = progress().coerceIn(0f, 1f)
        if (amount <= 0f) {
            drawContent()
        } else if (amount < 1f && size.width > 0f && size.height > 0f) {
            fold.update(size.width, size.height, amount, forward, density)
            front.setPolygon(points, fold.writePolygon(points, folded = false))
            back.setPolygon(points, fold.writePolygon(points, folded = true))
            clipPath(front) { this@onDrawWithContent.drawContent() }

            // Narrow layered strokes soften the fold shadow without creating a blur bitmap.
            val center = Offset(fold.normalX * fold.crease, fold.normalY * fold.crease)
            val tangent = Offset(-fold.normalY, fold.normalX) * (size.width + size.height)
            clipPath(back) {
                drawLine(Color.Black.copy(alpha = .025f), center - tangent, center + tangent, 10f * density)
                drawLine(Color.Black.copy(alpha = .035f), center - tangent, center + tangent, 5f * density)
                drawLine(Color.Black.copy(alpha = .035f), center - tangent, center + tangent, 2f * density)
            }

            reflection.reset()
            reflection[0, 0] = 1f - 2f * fold.normalX * fold.normalX
            reflection[0, 1] = -2f * fold.normalX * fold.normalY
            reflection[1, 0] = -2f * fold.normalX * fold.normalY
            reflection[1, 1] = 1f - 2f * fold.normalY * fold.normalY
            reflection[3, 0] = 2f * fold.crease * fold.normalX
            reflection[3, 1] = 2f * fold.crease * fold.normalY
            val overflow = 8f * density
            clipRect(-overflow, -overflow, size.width + overflow, size.height + overflow) {
                withTransform({ transform(reflection) }) {
                    clipPath(back) {
                        clipPath(paperOutline) {
                            this@onDrawWithContent.drawContent()
                            // Keep the existing hand-cut edge on the reverse side as well.
                            drawRect(backColor)
                        }
                    }
                }
            }
        }
    }
}
