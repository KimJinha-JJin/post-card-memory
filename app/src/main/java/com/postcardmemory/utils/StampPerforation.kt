package com.postcardmemory.utils

import kotlin.math.acos

/** A unit-square contour, shared by the Android Path adapter and JVM geometry checks. */
internal interface StampPerforationSink {
    fun moveTo(x: Double, y: Double)
    fun lineTo(x: Double, y: Double)
    fun arcTo(centerX: Double, centerY: Double, radius: Double, start: Double, sweep: Double)
    fun close()
}

/** Day 135 HTML shallowAligned(C2): 16 intervals, mouth 48%, depth 15% of pitch. */
internal fun traceStampPerforation(sink: StampPerforationSink) {
    val halfMouthRatio = 0.48 / 2.0
    val depthRatio = 0.15
    val radiusRatio = (halfMouthRatio * halfMouthRatio + depthRatio * depthRatio) /
        (2.0 * depthRatio)
    val outsideRatio = radiusRatio - depthRatio
    val pitch = 1.0 / (16 - 2.0 * outsideRatio)
    val halfMouth = halfMouthRatio * pitch
    val radius = radiusRatio * pitch
    val outside = outsideRatio * pitch
    val corner = halfMouth - outside
    val halfSweep = Math.toDegrees(acos(outside / radius))

    fun x(edge: Int, x: Double, y: Double) = when (edge) {
        0 -> x
        1 -> 1.0 - y
        2 -> 1.0 - x
        else -> y
    }
    fun y(edge: Int, x: Double, y: Double) = when (edge) {
        0 -> y
        1 -> x
        2 -> 1.0 - y
        else -> 1.0 - x
    }

    sink.moveTo(corner, 0.0)
    repeat(4) { edge ->
        fun line(localX: Double, localY: Double) =
            sink.lineTo(x(edge, localX, localY), y(edge, localX, localY))
        fun arc(centerX: Double, centerY: Double, sweep: Double) = sink.arcTo(
            x(edge, centerX, centerY), y(edge, centerX, centerY), radius,
            90.0 + halfSweep + edge * 90.0, sweep
        )

        for (index in 1 until 16) {
            val center = index * pitch - outside
            line(center - halfMouth, 0.0)
            arc(center, -outside, -2.0 * halfSweep)
        }
        line(1.0 - corner, 0.0)
        // The corner uses the same outside circle lattice, not a separate rounded corner.
        arc(1.0 + outside, -outside, 90.0 - 2.0 * halfSweep)
    }
    sink.close()
}
