package com.postcardmemory.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [GalleryPlayStage]는 동글이 쉼터(raceEnabled = false)와 엽서 쫑쫑컵(raceEnabled = true)이
 * 함께 쓰는 무대다. 화면 자체는 JVM에서 그릴 수 없으므로, 두 모드가 갈라지는 순수 계산
 * (카드 구역, 좌우 배회 범위, 세로 바닥선, 카드별 움직임 사양)을 직접 호출해 고정한다.
 */
class GalleryPlayStageZoneTest {

    private val stageHeight = 1000f
    private val stagePadding = 20f
    private val bottomReserved = 100f
    private val cardHeight = 150f

    @Test
    fun shelter_everyCardWandersTheFullStage() {
        (0 until 10).forEach { index ->
            assertEquals(
                "쉼터의 $index 번째 카드",
                StageMovementZone.FULL_STAGE,
                movementZoneFor(index, raceEnabled = false)
            )
        }
    }

    @Test
    fun race_firstRacersStayInPaddock_restAreSpectators() {
        (0 until 10).forEach { index ->
            val expected = if (index < RACE_MAX_RACERS) {
                StageMovementZone.RACE_PADDOCK
            } else {
                StageMovementZone.RACE_SPECTATOR
            }
            assertEquals("쫑쫑컵의 $index 번째 카드", expected, movementZoneFor(index, raceEnabled = true))
        }
    }

    @Test
    fun zoneBoundsX_onlyPaddockIsLimitedToTheStartLineHalf() {
        val maxX = 420f
        assertEquals(stagePadding to maxX, zoneBoundsX(StageMovementZone.FULL_STAGE, stagePadding, maxX))
        assertEquals(stagePadding to maxX, zoneBoundsX(StageMovementZone.RACE_SPECTATOR, stagePadding, maxX))
        assertEquals(stagePadding to 220f, zoneBoundsX(StageMovementZone.RACE_PADDOCK, stagePadding, maxX))
    }

    @Test
    fun stageFloorY_shelterUsesFloorFromSixtyPercentDown() {
        // 쉼터: 위쪽 끝 = 높이 60%, 아래쪽 끝 = 높이 - 하단 여백 - 카드 높이(쫑쫑컵 전용 추가 여백 없음).
        assertEquals(600f, floorY(StageMovementZone.FULL_STAGE, jitter = 0f), 0.001f)
        assertEquals(750f, floorY(StageMovementZone.FULL_STAGE, jitter = 1f), 0.001f)
    }

    @Test
    fun stageFloorY_raceSplitsSpectatorsAbovePaddock() {
        val top = stageHeight * RACE_ZONE_TOP_FRACTION
        val bottom = stageHeight - bottomReserved - cardHeight - stageHeight * 0.05f
        val mid = (top + bottom) / 2f

        assertEquals(top, floorY(StageMovementZone.RACE_SPECTATOR, jitter = 0f), 0.001f)
        assertEquals(mid, floorY(StageMovementZone.RACE_SPECTATOR, jitter = 1f), 0.001f)
        assertEquals(mid, floorY(StageMovementZone.RACE_PADDOCK, jitter = 0f), 0.001f)
        assertEquals(bottom, floorY(StageMovementZone.RACE_PADDOCK, jitter = 1f), 0.001f)
    }

    @Test
    fun createPlayStageCardSpec_isDeterministicPerPostcardAndSlot() {
        assertEquals(
            createPlayStageCardSpec(postcardId = 42L, index = 3, count = 10),
            createPlayStageCardSpec(postcardId = 42L, index = 3, count = 10)
        )
    }

    @Test
    fun createPlayStageCardSpec_staysInsideItsTuningRanges() {
        // count = 0은 빈 무대 계산 중에도 나눗셈이 터지지 않아야 하는 경계값이다.
        (0..10).forEach { count ->
            (1L..300L).forEach { postcardId ->
                val index = (postcardId % (count.coerceAtLeast(1))).toInt()
                assertInTuningRanges(
                    createPlayStageCardSpec(postcardId, index, count),
                    "id=$postcardId index=$index count=$count"
                )
            }
        }
    }

    @Test
    fun playStageSeed_isNonNegativeForEveryHashBoundary() {
        assertEquals(0L, playStageSeed(0))
        assertEquals(12345L, playStageSeed(12345))
        assertEquals(12345L, playStageSeed(-12345))
        assertEquals(Int.MAX_VALUE.toLong(), playStageSeed(Int.MAX_VALUE))
        // Int abs는 Int.MIN_VALUE를 그대로(음수) 돌려준다 — Long으로 넓혀 2^31이 되어야 한다.
        assertEquals(2_147_483_648L, playStageSeed(Int.MIN_VALUE))
    }

    @Test
    fun createPlayStageCardSpec_minValueHashNoLongerBreaksTheWalkLoop() {
        // 엽서 id 108,841,867이 10번째 자리(index 9)에 오면 해시가 정확히 Int.MIN_VALUE다.
        val postcardId = 108_841_867L
        val index = 9
        assertEquals(Int.MIN_VALUE, (postcardId * 1103515245L + index * 12345L).hashCode())

        // 예전 계산은 jumpEvery = 0(걷기 루프의 `% jumpEvery`에서 ArithmeticException), walkMillis 음수였다.
        val legacy = legacyCardSpec(postcardId, index, count = 10)
        assertEquals(0, legacy.jumpEvery)
        assertTrue(legacy.walkMillis < 0)

        assertInTuningRanges(createPlayStageCardSpec(postcardId, index, count = 10), "Int.MIN_VALUE 해시")
    }

    @Test
    fun createPlayStageCardSpec_keepsEveryNormalHashResultUnchanged() {
        // 수정은 Int.MIN_VALUE 해시 하나만 바꿔야 한다 — 그 밖의 입력은 예전 계산과 필드 하나까지 같아야 한다.
        (1..10).forEach { count ->
            (0 until count).forEach { index ->
                (1L..2000L).forEach { postcardId ->
                    val legacy = legacyCardSpec(postcardId, index, count)
                    val current = createPlayStageCardSpec(postcardId, index, count)
                    assertEquals("id=$postcardId index=$index count=$count", legacy.toCurrent(), current)
                }
            }
        }
        // 해시가 음수로 나오는 큰 id까지 고정 시드 표본으로 넓혀 본다(Int.MIN_VALUE 해시만 제외).
        val random = kotlin.random.Random(107)
        repeat(20_000) {
            val postcardId = random.nextLong(1L, Long.MAX_VALUE / 1103515245L)
            val index = random.nextInt(10)
            if ((postcardId * 1103515245L + index * 12345L).hashCode() == Int.MIN_VALUE) return@repeat
            assertEquals(
                "id=$postcardId index=$index",
                legacyCardSpec(postcardId, index, count = 10).toCurrent(),
                createPlayStageCardSpec(postcardId, index, count = 10)
            )
        }
    }

    private fun assertInTuningRanges(spec: PlayStageCardSpec, label: String) {
        assertTrue(label, spec.seed >= 0)
        assertTrue(label, spec.startBias in 0.08f..0.92f)
        assertTrue(label, spec.startJitter in 0f..1f)
        assertTrue(label, spec.speedDpPerSecond in 14f..34f)
        assertTrue(label, spec.pauseMillis in 500L..1999L)
        assertTrue(label, spec.walkMillis in 1200..3999)
        assertTrue(label, spec.jumpHeightDp in 6f..14f)
        assertTrue(label, spec.jumpEvery in 2..4)
        assertTrue(label, spec.baseRotation in -2.5f..2.4f)
        assertTrue(label, spec.startDelayMillis in 0L..899L)
        assertTrue(label, spec.direction == 1 || spec.direction == -1)
    }

    /** 107일차 4차 수정 전 계산(Int abs) 그대로 — 전후 비교와 결함 재현에만 쓴다. */
    private data class LegacyCardSpec(
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
    ) {
        fun toCurrent() = PlayStageCardSpec(
            seed.toLong(), startBias, startJitter, speedDpPerSecond, pauseMillis, walkMillis,
            jumpHeightDp, jumpEvery, baseRotation, startDelayMillis, direction
        )
    }

    private fun legacyCardSpec(postcardId: Long, index: Int, count: Int): LegacyCardSpec {
        val seed = kotlin.math.abs((postcardId * 1103515245L + index * 12345L).hashCode())
        val countSafe = count.coerceAtLeast(1)
        val bandCenter = (index + 0.5f) / countSafe
        val jitter = (((seed / 7) % 21) - 10) / 100f
        return LegacyCardSpec(
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

    private fun floorY(zone: StageMovementZone, jitter: Float): Float =
        stageFloorY(
            spec = createPlayStageCardSpec(postcardId = 1L, index = 0, count = 1).copy(startJitter = jitter),
            stageHeightPx = stageHeight,
            cardHeightPx = cardHeight,
            bottomReservedPx = bottomReserved,
            stagePaddingPx = stagePadding,
            zone = zone
        )
}
