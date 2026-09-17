package com.postcardmemory.ui.gallery

import com.postcardmemory.data.Postcard
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 76일차: 기억밀도가 "여러 연도를 이어붙인 원형 점 grid"에서 "지정한 한
 * 해의 1월→12월 줄기 그래프"로 재정의되며, 그 계산 로직
 * ([memoryDensityMonthsForYear], [memoryDensityBarLevel],
 * [memoryDensityHasOverflow])을 검증한다. 76일차 후속(새싹형)으로 표정
 * 3단계는 폐기되고, 줄기 위 하트의 진하기([memoryDensityHeartAlpha])만
 * 줄기 단계에 비례해 검증한다.
 */
class GalleryMemoryDensityTest {

    private val zone = ZoneId.of("Asia/Seoul")

    private fun postcard(id: Long, year: Int, month: Int, day: Int): Postcard =
        Postcard(
            id = id,
            imagePath = "/$id.jpg",
            title = "memory-$id",
            capturedAt = ZonedDateTime.of(year, month, day, 12, 0, 0, 0, zone)
                .toInstant()
                .toEpochMilli()
        )

    // ── 33절: 월별 grouping ──────────────────────────────────────────

    @Test
    fun emptyPostcards_stillReturnsAllTwelveMonthsAtZero() {
        val result = memoryDensityMonthsForYear(emptyList(), 2026, zone)

        assertEquals(12, result.size)
        assertTrue(result.all { it.count == 0 })
    }

    @Test
    fun sameMonthPostcards_areAggregatedTogether() {
        val result = memoryDensityMonthsForYear(
            listOf(postcard(1, 2026, 9, 1), postcard(2, 2026, 9, 30)),
            2026,
            zone
        )

        assertEquals(2, result.single { it.yearMonth.monthValue == 9 }.count)
    }

    @Test
    fun januaryPostcard_isOnlyCountedInJanuary() {
        val result = memoryDensityMonthsForYear(listOf(postcard(1, 2026, 1, 15)), 2026, zone)

        assertEquals(1, result.single { it.yearMonth.monthValue == 1 }.count)
        assertEquals(0, result.filter { it.yearMonth.monthValue != 1 }.sumOf { it.count })
    }

    @Test
    fun decemberPostcard_isOnlyCountedInDecember() {
        val result = memoryDensityMonthsForYear(listOf(postcard(1, 2026, 12, 25)), 2026, zone)

        assertEquals(1, result.single { it.yearMonth.monthValue == 12 }.count)
        assertEquals(0, result.filter { it.yearMonth.monthValue != 12 }.sumOf { it.count })
    }

    @Test
    fun previousYearPostcard_isNotIncludedInCurrentYearCount() {
        val result = memoryDensityMonthsForYear(listOf(postcard(1, 2025, 12, 31)), 2026, zone)

        assertEquals(0, result.sumOf { it.count })
    }

    @Test
    fun nextYearPostcard_isNotIncludedInCurrentYearCount() {
        val result = memoryDensityMonthsForYear(listOf(postcard(1, 2027, 1, 1)), 2026, zone)

        assertEquals(0, result.sumOf { it.count })
    }

    @Test
    fun aggregationUsesCapturedAtInProvidedZone_atMonthBoundary() {
        val instantNearBoundary = ZonedDateTime.of(2026, 9, 1, 0, 30, 0, 0, zone)
            .toInstant()
            .toEpochMilli()
        val input = Postcard(
            id = 1,
            imagePath = "/1.jpg",
            title = "boundary",
            capturedAt = instantNearBoundary
        )

        val result = memoryDensityMonthsForYear(listOf(input), 2026, zone)

        assertEquals(1, result.single { it.yearMonth.monthValue == 9 }.count)
        assertEquals(0, result.single { it.yearMonth.monthValue == 8 }.count)
    }

    @Test
    fun monthOrder_isAlwaysJanuaryToDecember() {
        val result = memoryDensityMonthsForYear(emptyList(), 2026, zone)

        assertEquals((1..12).toList(), result.map { it.yearMonth.monthValue })
    }

    // ── 31절: 막대 단위(1칸 = 엽서 2장, 최대 6칸) ──────────────────────

    @Test
    fun barLevel_matchesTwoPostcardsPerUnitUpToSixUnits() {
        val expected = mapOf(
            0 to 0, 1 to 1, 2 to 1, 3 to 2, 4 to 2, 5 to 3, 6 to 3,
            7 to 4, 8 to 4, 9 to 5, 10 to 5, 11 to 6, 12 to 6, 13 to 6
        )

        expected.forEach { (count, level) ->
            assertEquals("count=$count", level, memoryDensityBarLevel(count))
        }
    }

    @Test
    fun barLevel_largeValueStaysCappedAtSixUnits() {
        assertEquals(6, memoryDensityBarLevel(100))
    }

    @Test
    fun overflow_onlyTrueAboveTwelvePostcards() {
        assertFalse(memoryDensityHasOverflow(12))
        assertTrue(memoryDensityHasOverflow(13))
        assertTrue(memoryDensityHasOverflow(100))
    }

    // ── 76일차 후속(새싹형): 하트 진하기는 줄기 단계에 비례 ──────────────

    @Test
    fun heartAlpha_isMinimumAtLevelOneAndFullAtMaxLevel() {
        assertEquals(0.5f, memoryDensityHeartAlpha(1), 0.001f)
        assertEquals(1.0f, memoryDensityHeartAlpha(6), 0.001f)
    }

    @Test
    fun heartAlpha_increasesMonotonicallyWithLevel() {
        val alphas = (1..6).map { memoryDensityHeartAlpha(it) }
        for (i in 1 until alphas.size) {
            assertTrue("level ${i + 1} 하트가 이전 단계보다 진해야 함", alphas[i] > alphas[i - 1])
        }
    }
}
