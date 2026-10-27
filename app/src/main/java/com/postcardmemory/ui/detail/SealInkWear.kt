package com.postcardmemory.ui.detail

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * 고무도장이 종이에 얼마나 고르게 눌렸는지. 새 저장 필드가 아니라 도장의
 * 기존 id에서 결정론적으로 계산되므로 Room/seal_states 형식과 무관하다.
 */
enum class SealInkPressure {
    WELL,
    MEDIUM,
    LIGHT
}

/**
 * 도장 정사각형을 [resolution]×[resolution] 칸으로 나눈 "잉크가 빠진 정도"
 * 지도. 값 0 = 잉크 그대로, 1 = 잉크가 전혀 묻지 않음. 좌표가 도장 한 변
 * 대비라 화면과 export 해상도가 달라도 같은 모양으로 늘려 그린다.
 */
class SealInkWear(
    val pressure: SealInkPressure,
    val resolution: Int,
    val erase: FloatArray
)

/** 결손 지도 해상도. 도장 크기와 무관하게 고정이라 화면·export가 같은 지도를 쓴다. */
const val SEAL_INK_WEAR_RESOLUTION = 160

/**
 * 도장 id → seed. String.hashCode 대신 FNV-1a 64비트를 직접 계산해, JVM
 * 구현이나 저장 시점과 상관없이 같은 id는 항상 같은 seed가 된다.
 */
fun sealInkSeed(sealId: String): Long {
    var hash = -0x340d631b7bdddcdbL // FNV offset basis
    for (ch in sealId) {
        hash = hash xor ch.code.toLong()
        hash *= 0x100000001b3L
    }
    return hash
}

/**
 * 고무면이 종이에 닿은 결과를 계산한다.
 *
 * 접촉 = 고무면 압력 + 종이 결.
 * - 압력: 고무면이 한쪽으로 살짝 기울어 눌린 기울기 + 고무면의 완만한
 *   울퉁불퉁함. 덜 눌린 쪽에서만 접촉이 모자라 결손이 그쪽에 몰린다.
 * - 종이 결: 여러 굵기의 섬유 요철. 압력이 충분한 곳에서는 거의 영향이
 *   없고, 압력이 모자란 곳에서만 잉크가 점점이·결 따라 빠진다.
 * 접촉이 부족한 칸만 잉크를 지우므로 표면 전체에 고른 노이즈가 깔리지
 * 않고, 선을 지나면 획이 끊기고 면 위에서는 내부가 빈다.
 *
 * 같은 seed는 항상 같은 지도를 돌려준다(렌더 때마다 새 랜덤을 만들지 않음).
 */
fun sealInkWear(
    seed: Long,
    resolution: Int = SEAL_INK_WEAR_RESOLUTION
): SealInkWear {
    var state = seed
    fun nextFloat(): Float {
        state += -0x61c8864680b583ebL
        return ((mix64(state) ushr 40).toFloat() / (1L shl 24).toFloat())
    }

    val pressureRoll = nextFloat()
    val pressure =
        when {
            pressureRoll < 0.45f -> SealInkPressure.WELL
            pressureRoll < 0.80f -> SealInkPressure.MEDIUM
            else -> SealInkPressure.LIGHT
        }

    // 눌림 정도마다 결손의 "양"뿐 아니라 "모양"이 다르다.
    // - 잘 눌림: 기울기가 거의 없어 전체에 고르게 작은 핀홀만 생긴다(잔결 위주).
    // - 중간: 한쪽으로 기울어 얼룩진 결손이 한쪽에 몰린다.
    // - 덜 눌림: 크게 기울어 한쪽은 진하고 반대쪽은 잉크가 뚝 떨어진다.
    val spec =
        when (pressure) {
            SealInkPressure.WELL -> PressureSpec(
                basePressure = 0.46f, tilt = 0.10f, bumpiness = 0.10f,
                coarseGrain = 0.30f, midGrain = 0.12f, fineGrain = 0.38f
            )
            SealInkPressure.MEDIUM -> PressureSpec(
                basePressure = 0.42f, tilt = 0.34f, bumpiness = 0.22f,
                coarseGrain = 0.50f, midGrain = 0.24f, fineGrain = 0.18f
            )
            SealInkPressure.LIGHT -> PressureSpec(
                basePressure = 0.40f, tilt = 0.66f, bumpiness = 0.26f,
                coarseGrain = 0.50f, midGrain = 0.30f, fineGrain = 0.14f
            )
        }

    // 덜 눌린 방향. 이 방향으로 갈수록 압력이 약해진다.
    val tiltAngle = nextFloat() * 2f * PI.toFloat()
    val tiltX = cos(tiltAngle)
    val tiltY = sin(tiltAngle)

    val erase = FloatArray(resolution * resolution)
    for (row in 0 until resolution) {
        for (col in 0 until resolution) {
            val x = (col + 0.5f) / resolution
            val y = (row + 0.5f) / resolution

            val press =
                spec.basePressure -
                        spec.tilt * ((x - 0.5f) * tiltX + (y - 0.5f) * tiltY) * 2f +
                        spec.bumpiness * valueNoise(seed, 1, x, y, 3f)

            val grain =
                spec.coarseGrain * valueNoise(seed, 2, x, y, 31f) +
                        spec.midGrain * valueNoise(seed, 3, x, y, 12f) +
                        spec.fineGrain * valueNoise(seed, 4, x, y, 67f)

            val contact = press + grain

            // 접촉 경계를 아주 좁게 부드럽게 해서, 결손 가장자리가 계단처럼
            // 보이지 않고 잉크가 묻다 만 느낌이 나게 한다.
            val t = ((contact + 0.06f) / 0.12f).coerceIn(0f, 1f)
            val ink = t * t * (3f - 2f * t)
            erase[row * resolution + col] = 1f - ink
        }
    }

    return SealInkWear(
        pressure = pressure,
        resolution = resolution,
        erase = erase
    )
}

/** 눌림 정도별 압력(기본값·기울기·고무면 울퉁불퉁함)과 종이 결 굵기별 세기. */
private class PressureSpec(
    val basePressure: Float,
    val tilt: Float,
    val bumpiness: Float,
    val coarseGrain: Float,
    val midGrain: Float,
    val fineGrain: Float
)

// 옥타브마다 격자 방향을 다르게 돌려, 결손이 가로·세로로 각지게 정렬되지 않게 한다.
private val NOISE_LAYER_ROTATION = floatArrayOf(0f, 0f, 0.61f, 1.37f, 2.2f)

/** [-1, 1] 값 노이즈. 칸 꼭짓점 값을 seed로 해시해 부드럽게 보간한다. */
private fun valueNoise(
    seed: Long,
    layer: Int,
    x: Float,
    y: Float,
    cells: Float
): Float {
    val angle = NOISE_LAYER_ROTATION[layer]
    val c = cos(angle)
    val s = sin(angle)
    val fx = (x * c - y * s) * cells + 17.3f * layer
    val fy = (x * s + y * c) * cells + 5.1f * layer

    val ix = floor(fx).toLong()
    val iy = floor(fy).toLong()
    val tx = fx - ix
    val ty = fy - iy
    val sx = tx * tx * (3f - 2f * tx)
    val sy = ty * ty * (3f - 2f * ty)

    val a = latticeValue(seed, layer, ix, iy)
    val b = latticeValue(seed, layer, ix + 1, iy)
    val d = latticeValue(seed, layer, ix, iy + 1)
    val e = latticeValue(seed, layer, ix + 1, iy + 1)

    val top = a + (b - a) * sx
    val bottom = d + (e - d) * sx
    return top + (bottom - top) * sy
}

private fun latticeValue(seed: Long, layer: Int, ix: Long, iy: Long): Float {
    val h =
        mix64(
            seed xor
                    (layer.toLong() * -0x61c8864680b583ebL) xor
                    (ix * -0x3d4d51c2d82b14b1L) xor
                    (iy * 0x165667b19e3779f9L)
        )
    return (h ushr 40).toFloat() / (1L shl 24).toFloat() * 2f - 1f
}

/**
 * SplitMix64 섞기 함수. java.util.Random 등 플랫폼 구현에 기대지 않고
 * 계산식을 여기 고정해, 화면·export·재실행 어디서든 같은 결과를 낸다.
 */
private fun mix64(value: Long): Long {
    var z = value
    z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
    z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
    return z xor (z ushr 31)
}
