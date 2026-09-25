package com.postcardmemory.ui.gallery

import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 84일차: 메인 갤러리 "흔들어서 한 장"의 흔들림 판정 순수 로직.
 *
 * 센서 callback은 sample을 넘겨주기만 하고, 판정은 모두 여기서 한다 — JVM
 * 테스트로 threshold/cooldown 회귀를 잡기 위해서다.
 *
 * 판정 방식:
 * 1. 중력 제거 — 시간 상수 기반 low-pass로 중력 성분을 추정해 빼고, 남은
 *    선형 가속도의 크기만 본다. 폰을 가만히 들고 있거나 방향만 바꾸는 건
 *    여기서 대부분 사라진다.
 * 2. 방향 반전 — 선형 가속도가 [thresholdMs2]를 넘는 "강한 sample"끼리
 *    방향(내적 부호)이 뒤집힐 때만 한 번으로 센다. 한 번 왕복해 흔들면
 *    +, −, −, + 로 두 번 뒤집힌다. 들어올리기·내려놓기처럼 한쪽으로
 *    밀었다 멈추는 동작은 많아야 한 번이고, 책상에 닿는 충격의 고주파
 *    떨림은 [minReversalGapMillis]보다 촘촘해 세지 않는다.
 * 3. 창 — [windowMillis] 안에 반전이 [requiredReversals]번 쌓이면 흔들기로
 *    인정한다. 강한 sample이 창보다 오래 끊기면 연쇄를 처음부터 다시 센다.
 * 4. cooldown — 인정 직후 [cooldownMillis] 동안은 sample을 쌓지 않는다.
 *    한 번의 물리적 흔들기에서 sample이 여러 번 들어와도 이벤트는 1번이다.
 *
 * 숫자는 초기값이고 실기기 QA가 최종 기준이다.
 */
internal class GalleryShakeDetector(
    private val thresholdMs2: Float = GALLERY_SHAKE_THRESHOLD_MS2,
    private val requiredReversals: Int = GALLERY_SHAKE_REQUIRED_REVERSALS,
    private val windowMillis: Long = GALLERY_SHAKE_WINDOW_MS,
    private val minReversalGapMillis: Long = GALLERY_SHAKE_MIN_REVERSAL_GAP_MS,
    private val cooldownMillis: Long = GALLERY_SHAKE_COOLDOWN_MS,
    private val gravityTimeConstantMillis: Float = GALLERY_SHAKE_GRAVITY_TIME_CONSTANT_MS
) {
    private val gravity = FloatArray(3)
    private var hasGravity = false
    private var lastSampleAtMillis = 0L

    private var lastStrong: FloatArray? = null
    private var lastStrongAtMillis = 0L
    private var lastReversalAtMillis = 0L
    private val reversalTimes = ArrayDeque<Long>()

    private var lastTriggerAtMillis: Long? = null

    /**
     * 가속도계 sample 하나(m/s², 중력 포함)를 넣는다. 이 sample로 흔들기가
     * 인정되면 true — 호출자는 true일 때만 반응하면 된다.
     */
    fun onSample(timestampMillis: Long, x: Float, y: Float, z: Float): Boolean {
        updateGravity(timestampMillis, x, y, z)

        val lx = x - gravity[0]
        val ly = y - gravity[1]
        val lz = z - gravity[2]

        val lastTrigger = lastTriggerAtMillis
        if (lastTrigger != null && timestampMillis - lastTrigger < cooldownMillis) {
            return false
        }

        val magnitude = sqrt(lx * lx + ly * ly + lz * lz)
        if (magnitude < thresholdMs2) {
            return false
        }

        val previous = lastStrong
        if (previous == null || timestampMillis - lastStrongAtMillis > windowMillis) {
            resetChain()
        } else {
            val dot = previous[0] * lx + previous[1] * ly + previous[2] * lz
            if (dot < 0f && timestampMillis - lastReversalAtMillis >= minReversalGapMillis) {
                reversalTimes.addLast(timestampMillis)
                lastReversalAtMillis = timestampMillis
            }
        }

        lastStrong = floatArrayOf(lx, ly, lz)
        lastStrongAtMillis = timestampMillis

        while (reversalTimes.isNotEmpty() &&
            timestampMillis - reversalTimes.first() > windowMillis
        ) {
            reversalTimes.removeFirst()
        }

        if (reversalTimes.size >= requiredReversals) {
            lastTriggerAtMillis = timestampMillis
            resetChain()
            return true
        }

        return false
    }

    private fun resetChain() {
        lastStrong = null
        lastReversalAtMillis = 0L
        reversalTimes.clear()
    }

    private fun updateGravity(timestampMillis: Long, x: Float, y: Float, z: Float) {
        if (!hasGravity) {
            gravity[0] = x
            gravity[1] = y
            gravity[2] = z
            hasGravity = true
            lastSampleAtMillis = timestampMillis
            return
        }

        val dt = (timestampMillis - lastSampleAtMillis).coerceAtLeast(0L).toFloat()
        lastSampleAtMillis = timestampMillis

        val alpha = gravityTimeConstantMillis / (gravityTimeConstantMillis + dt)
        gravity[0] = alpha * gravity[0] + (1f - alpha) * x
        gravity[1] = alpha * gravity[1] + (1f - alpha) * y
        gravity[2] = alpha * gravity[2] + (1f - alpha) * z
    }
}

/**
 * 흔들기 후보 중 균등 랜덤으로 한 장의 id를 고른다. 후보가 없으면 null —
 * 호출자는 navigation을 하지 않는다. 가중치(최근/오래된/덜 본)는 두지 않는다.
 */
internal fun pickRandomPostcardId(
    candidateIds: List<Long>,
    random: Random = Random.Default
): Long? {
    if (candidateIds.isEmpty()) {
        return null
    }

    return candidateIds[random.nextInt(candidateIds.size)]
}

// 선형 가속도(중력 제거 후) 약 1.1g. 가볍게 한 번 왕복해도 넘고, 걷기·들어올리기
// (대개 2~5 m/s²)는 넘지 않는 선에서 시작한다.
internal const val GALLERY_SHAKE_THRESHOLD_MS2 = 11f
internal const val GALLERY_SHAKE_REQUIRED_REVERSALS = 2
internal const val GALLERY_SHAKE_WINDOW_MS = 800L
internal const val GALLERY_SHAKE_MIN_REVERSAL_GAP_MS = 70L
internal const val GALLERY_SHAKE_COOLDOWN_MS = 1500L
internal const val GALLERY_SHAKE_GRAVITY_TIME_CONSTANT_MS = 100f
