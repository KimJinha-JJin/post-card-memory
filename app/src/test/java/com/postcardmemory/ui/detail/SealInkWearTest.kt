package com.postcardmemory.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 도장 잉크 결손 지도는 새 저장 필드 없이 도장 id만으로 계산된다. 미감은
 * 실기기에서 판단하고, 여기서는 "같은 도장은 언제 다시 그려도 같은 질감"과
 * "잘 눌림/덜 눌림이 실제로 갈린다"는 구조적 성질만 확인한다.
 */
class SealInkWearTest {

    // 성질 확인용으로는 거친 해상도로 충분하다(실제 렌더는 SEAL_INK_WEAR_RESOLUTION).
    private val testResolution = 48

    private fun wearOf(id: String) =
        sealInkWear(sealInkSeed(id), testResolution)

    private fun SealInkWear.erasedShare() = erase.average()

    @Test
    fun sameId_alwaysProducesSameWear() {
        val id = "3f2b6c1e-8a55-4d1e-9f0a-1c2d3e4f5a6b"

        val first = wearOf(id)
        val second = wearOf(id)

        assertEquals(sealInkSeed(id), sealInkSeed(id))
        assertEquals(first.pressure, second.pressure)
        assertTrue(first.erase.contentEquals(second.erase))
    }

    @Test
    fun seedSurvivesSerializationRoundTrip_soReopenedSealKeepsItsTexture() {
        val seal = PostcardSealItem(type = SealType.AIR_MAIL)

        val reopened = requireNotNull(deserializePostcardSealItem(seal.serialize()))

        assertEquals(sealInkSeed(seal.id), sealInkSeed(reopened.id))
    }

    @Test
    fun differentIds_produceDifferentWear() {
        assertFalse(
            wearOf("seal-a").erase.contentEquals(wearOf("seal-b").erase)
        )
    }

    @Test
    fun eraseValues_stayWithinZeroToOne() {
        (0 until 20).forEach { index ->
            val wear = wearOf("seal-$index")
            assertEquals(testResolution * testResolution, wear.erase.size)
            wear.erase.forEach { assertTrue(it in 0f..1f) }
        }
    }

    @Test
    fun wellPressedSeals_stayMostlyInked_andLightlyPressedOnesLoseMore() {
        val byPressure =
            (0 until 80)
                .map { wearOf("seal-$it") }
                .groupBy { it.pressure }

        // 잘 눌린·중간·덜 눌린 도장이 모두 실제로 나온다.
        SealInkPressure.entries.forEach { pressure ->
            assertTrue(byPressure[pressure].orEmpty().isNotEmpty())
        }

        // 잘 눌린 도장은 거의 온전하다(실루엣·글자가 그대로 읽히도록).
        byPressure.getValue(SealInkPressure.WELL).forEach { wear ->
            assertTrue(wear.erasedShare() < 0.05)
        }

        val wellMean = byPressure.getValue(SealInkPressure.WELL).map { it.erasedShare() }.average()
        val mediumMean = byPressure.getValue(SealInkPressure.MEDIUM).map { it.erasedShare() }.average()
        val lightMean = byPressure.getValue(SealInkPressure.LIGHT).map { it.erasedShare() }.average()
        assertTrue(wellMean < mediumMean)
        assertTrue(mediumMean < lightMean)
    }
}
