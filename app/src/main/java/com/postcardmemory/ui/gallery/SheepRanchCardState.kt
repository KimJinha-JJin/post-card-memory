package com.postcardmemory.ui.gallery

import kotlin.math.abs

data class SheepRanchCardSpec(
    val seed: Int,
    val startBias: Float,
    val startJitter: Float,
    val speedDpPerSecond: Float,
    val pauseMillis: Long,
    val walkMillis: Int,
    val jumpHeightDp: Float,
    val jumpEvery: Int,
    val baseRotation: Float,
    val startDelayMillis: Long,
    val direction: Int
)

fun createSheepRanchCardSpec(
    postcardId: Long,
    index: Int,
    count: Int
): SheepRanchCardSpec {
    val seed = abs((postcardId * 1103515245L + index * 12345L).hashCode())
    val countSafe = count.coerceAtLeast(1)
    val bandCenter = (index + 0.5f) / countSafe
    val jitter = (((seed / 7) % 21) - 10) / 100f

    return SheepRanchCardSpec(
        seed = seed,
        startBias = (bandCenter + jitter).coerceIn(0.08f, 0.92f),
        startJitter = (((seed / 11) % 100) / 100f),
        speedDpPerSecond = 14f + (seed % 21),
        pauseMillis = 500L + (seed % 1500),
        walkMillis = 1200 + (seed % 2800),
        jumpHeightDp = 6f + (seed % 9),
        jumpEvery = 2 + (seed % 3),
        baseRotation = (((seed / 13) % 50) - 25) / 10f,
        startDelayMillis = (seed % 900).toLong(),
        direction = if (seed % 2 == 0) 1 else -1
    )
}
