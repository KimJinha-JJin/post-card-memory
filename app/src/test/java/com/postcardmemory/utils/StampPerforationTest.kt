package com.postcardmemory.utils

import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure C2 geometry; Android Path rasterization and Compose clipping require device QA. */
class StampPerforationTest {
    private data class Point(val x: Double, val y: Double)
    private data class Arc(
        val center: Point, val radius: Double, val start: Double, val sweep: Double,
        val preceding: Point
    ) {
        fun point(angle: Double): Point {
            val radians = Math.toRadians(angle)
            return Point(center.x + radius * cos(radians), center.y + radius * sin(radians))
        }
        val end get() = point(start + sweep)
    }

    private class Recording : StampPerforationSink {
        lateinit var first: Point
        lateinit var current: Point
        val arcs = mutableListOf<Arc>()
        val lines = mutableListOf<Point>()
        var closed = false
        override fun moveTo(x: Double, y: Double) {
            first = Point(x, y)
            current = first
        }
        override fun lineTo(x: Double, y: Double) {
            current = Point(x, y)
            lines.add(current)
        }
        override fun arcTo(centerX: Double, centerY: Double, radius: Double, start: Double, sweep: Double) {
            val arc = Arc(Point(centerX, centerY), radius, start, sweep, current)
            arcs.add(arc)
            current = arc.end
        }
        override fun close() { closed = true }
    }

    private fun contour() = Recording().also { traceStampPerforation(it) }
    private fun assertPoint(expected: Point, actual: Point) {
        assertEquals(expected.x, actual.x, 1e-12)
        assertEquals(expected.y, actual.y, 1e-12)
    }

    @Test
    fun approvedHtmlC2FixtureMatchesAtFiveDisplaySizes() {
        val path = contour()
        // Independently calculated from HTML C2: p=side/(16-.234), R=.267p.
        assertPoint(Point(0.00780159837625270, 0.0), path.first)
        val first = path.arcs.first()
        assertPoint(Point(0.0560065964734238, -0.00742103260180135), first.center)
        assertEquals(0.0169351769630851, first.radius, 1e-14)
        for (side in listOf(93.333333, 106.666667, 146.666667, 60.0, 38.0)) {
            val midpoint = first.point(first.start + first.sweep / 2.0)
            assertEquals(side * 0.00951414436128378, midpoint.y * side, 1e-10)
        }
    }

    @Test
    fun everySideHasFifteenEqualHolesAndOneLatticeCorner() {
        val path = contour()
        assertEquals(64, path.arcs.size)
        assertEquals(64, path.lines.size)
        assertTrue(path.closed)
        val radius = path.arcs.first().radius
        path.arcs.forEach { assertEquals(radius, it.radius, 1e-14) }
        for (edge in 0 until 4) {
            val arcs = path.arcs.subList(edge * 16, (edge + 1) * 16)
            arcs.take(15).forEach { assertEquals(arcs.first().sweep, it.sweep, 1e-12) }
            assertEquals(90.0 + arcs.first().sweep, arcs.last().sweep, 1e-12)
        }
    }

    @Test
    fun allArcStartsAndFinalCornerConnectWithoutExtraPathSegments() {
        val path = contour()
        path.arcs.forEach { assertPoint(it.preceding, it.point(it.start)) }
        assertPoint(path.first, path.arcs.last().end)
        for (edge in 0 until 3) {
            val cornerEnd = path.arcs[edge * 16 + 15].end
            val nextEntry = path.arcs[edge * 16 + 16].preceding
            // The first straight bridge has the same length as an interior bridge.
            val length = kotlin.math.hypot(nextEntry.x - cornerEnd.x, nextEntry.y - cornerEnd.y)
            assertEquals(0.52 / 15.766, length, 1e-12)
        }
    }

    @Test
    fun fourSidesAreQuarterTurnCopiesIncludingCorners() {
        val arcs = contour().arcs
        for (index in 0 until 48) {
            val source = arcs[index]
            val rotated = arcs[index + 16]
            assertPoint(Point(1.0 - source.center.y, source.center.x), rotated.center)
            assertPoint(Point(1.0 - source.end.y, source.end.x), rotated.end)
        }
    }

    @Test
    fun mouthsAndBridgesRetainApprovedRatiosAtSmallSizesAndDensities() {
        val arcs = contour().arcs.take(15)
        for (side in listOf(38.0, 60.0, 106.666667)) {
            for (density in listOf(1.0, 1.5, 2.8125, 3.0)) {
                val scale = side * density
                val pitch = (arcs[1].center.x - arcs[0].center.x) * scale
                val mouth = (arcs[0].end.x - arcs[0].preceding.x) * scale
                assertEquals(0.48, mouth / pitch, 1e-12)
                assertEquals(0.52, (pitch - mouth) / pitch, 1e-12)
                assertTrue(pitch - mouth > 0.0)
            }
        }
    }

    @Test
    fun sampledContourIsFiniteAndStaysInsidePaperBounds() {
        contour().arcs.forEach { arc ->
            for (sample in 0..100) {
                val point = arc.point(arc.start + arc.sweep * sample / 100.0)
                assertTrue(point.x.isFinite() && point.y.isFinite())
                assertTrue(point.x in -1e-12..(1.0 + 1e-12))
                assertTrue(point.y in -1e-12..(1.0 + 1e-12))
            }
        }
    }
}
