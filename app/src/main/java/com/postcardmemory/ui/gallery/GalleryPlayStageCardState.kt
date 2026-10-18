package com.postcardmemory.ui.gallery

import kotlin.math.abs

data class PlayStageCardSpec(
    val seed: Long,
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

/**
 * 해시값을 0 이상의 seed로 바꾼다. Int로 abs를 하면 [Int.MIN_VALUE]만 음수로 남아(abs가
 * 자기 자신을 돌려줌) `jumpEvery`가 0이 되고 걷기 루프의 `% jumpEvery`에서 앱이 죽었다.
 * Long으로 넓혀 abs를 하면 그 값만 2^31이 되고, 나머지 모든 해시의 seed는 예전과 같다.
 */
internal fun playStageSeed(hash: Int): Long = abs(hash.toLong())

fun createPlayStageCardSpec(
    postcardId: Long,
    index: Int,
    count: Int
): PlayStageCardSpec {
    val seed = playStageSeed((postcardId * 1103515245L + index * 12345L).hashCode())
    val countSafe = count.coerceAtLeast(1)
    val bandCenter = (index + 0.5f) / countSafe
    val jitter = (((seed / 7) % 21) - 10) / 100f

    return PlayStageCardSpec(
        seed = seed,
        startBias = (bandCenter + jitter).coerceIn(0.08f, 0.92f),
        startJitter = (((seed / 11) % 100) / 100f),
        speedDpPerSecond = 14f + (seed % 21),
        pauseMillis = 500L + (seed % 1500),
        walkMillis = 1200 + (seed % 2800).toInt(),
        jumpHeightDp = 6f + (seed % 9),
        jumpEvery = 2 + (seed % 3).toInt(),
        baseRotation = (((seed / 13) % 50) - 25) / 10f,
        startDelayMillis = seed % 900,
        direction = if (seed % 2 == 0L) 1 else -1
    )
}
