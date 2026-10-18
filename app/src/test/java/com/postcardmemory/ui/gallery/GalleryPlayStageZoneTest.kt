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
                val spec = createPlayStageCardSpec(postcardId, index, count)
                val label = "id=$postcardId index=$index count=$count"

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
        }
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
