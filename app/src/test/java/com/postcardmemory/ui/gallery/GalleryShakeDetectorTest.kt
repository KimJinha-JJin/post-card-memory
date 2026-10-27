package com.postcardmemory.ui.gallery

import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 84일차 "흔들어서 한 장"의 판정([GalleryShakeDetector])과 랜덤 선택
 * ([pickRandomPostcardId]) 검증. 센서 하드웨어가 아니라, 센서가 넘겨줄
 * sample 시퀀스(m/s², 중력 포함, 50Hz)를 합성해 넣는다.
 */
class GalleryShakeDetectorTest {

    private val gravity = 9.81f
    private val stepMillis = 20L

    /** 폰을 세워 든 상태(중력이 y축)에서 [durationMillis] 동안 가만히 둔다. */
    private fun GalleryShakeDetector.feedStill(
        startMillis: Long,
        durationMillis: Long
    ): Int = feed(startMillis, durationMillis) { 0f }

    /**
     * x축으로 [frequencyHz], 진폭 [amplitudeMs2]의 사인 가속도를 더해 흔든다.
     * 반환값은 그 구간에서 true가 나온 횟수.
     */
    private fun GalleryShakeDetector.feedShake(
        startMillis: Long,
        durationMillis: Long,
        amplitudeMs2: Float,
        frequencyHz: Float = 3f
    ): Int = feed(startMillis, durationMillis) { tMillis ->
        amplitudeMs2 * sin(2.0 * PI * frequencyHz * tMillis / 1000.0).toFloat()
    }

    private fun GalleryShakeDetector.feed(
        startMillis: Long,
        durationMillis: Long,
        xAt: (Long) -> Float
    ): Int {
        var triggers = 0
        var t = 0L
        while (t < durationMillis) {
            if (onSample(startMillis + t, xAt(t), gravity, 0f)) {
                triggers++
            }
            t += stepMillis
        }
        return triggers
    }

    @Test
    fun stillPhone_neverTriggers() {
        val detector = GalleryShakeDetector()

        assertEquals(0, detector.feedStill(0L, 5_000L))
    }

    @Test
    fun gentleSwaying_belowThreshold_neverTriggers() {
        // 걷기·손목 흔들림 수준(선형 가속도 약 4 m/s², 2Hz)을 오래 넣어도 반응 없음.
        val detector = GalleryShakeDetector()

        assertEquals(
            0,
            detector.feedShake(0L, 5_000L, amplitudeMs2 = 4f, frequencyHz = 2f)
        )
    }

    @Test
    fun singleStrongPush_withoutReversal_doesNotTrigger() {
        // 들어올리기/내려놓기 충격처럼 한쪽으로 강하게 한 번 밀린 뒤 멈춤.
        val detector = GalleryShakeDetector()
        detector.feedStill(0L, 1_000L)

        val triggers = detector.feed(1_000L, 200L) { 25f } +
            detector.feedStill(1_200L, 2_000L)

        assertEquals(0, triggers)
    }

    @Test
    fun rotatingPhone_gravityShift_doesNotTrigger() {
        // 화면 방향 바꾸기: 중력이 y축에서 x축으로 약 300ms에 걸쳐 옮겨 간다.
        val detector = GalleryShakeDetector()
        detector.feedStill(0L, 1_000L)

        var triggers = 0
        var t = 0L
        while (t <= 300L) {
            val angle = (PI / 2.0) * t / 300.0
            val x = (gravity * sin(angle)).toFloat()
            val y = (gravity * kotlin.math.cos(angle)).toFloat()
            if (detector.onSample(1_000L + t, x, y, 0f)) triggers++
            t += stepMillis
        }
        repeat(100) { i ->
            if (detector.onSample(1_320L + i * stepMillis, gravity, 0f, 0f)) triggers++
        }

        assertEquals(0, triggers)
    }

    @Test
    fun clearShake_triggersExactlyOnceForOneSequence() {
        // 보통 세기(약 25 m/s², 3Hz)로 0.8초 흔드는 한 번의 동작은 1번만 인정된다.
        val detector = GalleryShakeDetector()
        detector.feedStill(0L, 1_000L)

        assertEquals(1, detector.feedShake(1_000L, 800L, amplitudeMs2 = 25f))
    }

    @Test
    fun lightButClearShake_stillTriggers() {
        // 너무 세게 휘두르지 않아도(약 15 m/s²) 한 번 왕복 흔들면 인식된다.
        val detector = GalleryShakeDetector()
        detector.feedStill(0L, 1_000L)

        assertEquals(1, detector.feedShake(1_000L, 700L, amplitudeMs2 = 15f))
    }

    @Test
    fun shakeDuringCooldown_isIgnored_andTriggersAgainAfterCooldown() {
        val detector = GalleryShakeDetector()
        detector.feedStill(0L, 1_000L)

        assertEquals(1, detector.feedShake(1_000L, 600L, amplitudeMs2 = 25f))

        // 첫 인정 직후 이어서 흔들어도 cooldown(1.5초) 안에서는 추가 이벤트 없음.
        assertEquals(0, detector.feedShake(1_600L, 700L, amplitudeMs2 = 25f))

        // cooldown이 끝난 뒤 새로 흔들면 다시 인정된다.
        detector.feedStill(2_300L, 1_000L)
        assertEquals(1, detector.feedShake(3_300L, 800L, amplitudeMs2 = 25f))
    }

    @Test
    fun continuousLongShake_triggersAtMostOncePerCooldown() {
        val detector = GalleryShakeDetector()
        detector.feedStill(0L, 1_000L)

        val triggers = detector.feedShake(1_000L, 3_000L, amplitudeMs2 = 30f)

        // 3초 연속 흔들기 → cooldown 1.5초 기준 최대 2번.
        assertTrue("triggers=$triggers", triggers in 1..2)
    }

    @Test
    fun pickRandom_emptyCandidates_returnsNull() {
        assertNull(pickRandomPostcardId(emptyList()))
    }

    @Test
    fun pickRandom_singleCandidate_returnsIt() {
        repeat(20) { seed ->
            assertEquals(42L, pickRandomPostcardId(listOf(42L), Random(seed)))
        }
    }

    @Test
    fun pickRandom_alwaysWithinCandidates_andReachesEveryCandidate() {
        val candidates = listOf(3L, 7L, 11L, 19L)
        val random = Random(84)

        val picked = (0 until 400).map { pickRandomPostcardId(candidates, random) }

        assertTrue(picked.all { it in candidates })
        assertEquals(candidates.toSet(), picked.toSet())
    }
}
