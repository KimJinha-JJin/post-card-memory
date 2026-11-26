package com.postcardmemory.ui.gallery

import com.postcardmemory.data.Postcard
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 그래프 제거 후에도 유지하는 capturedAt·시간대 기준 12개월 집계 검사. */
class GalleryPostcardMonthTest {

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

    // ── 월별 grouping ──────────────────────────────────────────

    @Test
    fun emptyPostcards_stillReturnsAllTwelveMonthsAtZero() {
        val result = postcardMonthsForYear(emptyList(), 2026, zone)

        assertEquals(12, result.size)
        assertTrue(result.all { it.count == 0 })
    }

    @Test
    fun sameMonthPostcards_areAggregatedTogether() {
        val result = postcardMonthsForYear(
            listOf(postcard(1, 2026, 9, 1), postcard(2, 2026, 9, 30)),
            2026,
            zone
        )

        assertEquals(2, result.single { it.yearMonth.monthValue == 9 }.count)
    }

    @Test
    fun januaryPostcard_isOnlyCountedInJanuary() {
        val result = postcardMonthsForYear(listOf(postcard(1, 2026, 1, 15)), 2026, zone)

        assertEquals(1, result.single { it.yearMonth.monthValue == 1 }.count)
        assertEquals(0, result.filter { it.yearMonth.monthValue != 1 }.sumOf { it.count })
    }

    @Test
    fun decemberPostcard_isOnlyCountedInDecember() {
        val result = postcardMonthsForYear(listOf(postcard(1, 2026, 12, 25)), 2026, zone)

        assertEquals(1, result.single { it.yearMonth.monthValue == 12 }.count)
        assertEquals(0, result.filter { it.yearMonth.monthValue != 12 }.sumOf { it.count })
    }

    @Test
    fun previousYearPostcard_isNotIncludedInCurrentYearCount() {
        val result = postcardMonthsForYear(listOf(postcard(1, 2025, 12, 31)), 2026, zone)

        assertEquals(0, result.sumOf { it.count })
    }

    @Test
    fun nextYearPostcard_isNotIncludedInCurrentYearCount() {
        val result = postcardMonthsForYear(listOf(postcard(1, 2027, 1, 1)), 2026, zone)

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

        val result = postcardMonthsForYear(listOf(input), 2026, zone)

        assertEquals(1, result.single { it.yearMonth.monthValue == 9 }.count)
        assertEquals(0, result.single { it.yearMonth.monthValue == 8 }.count)
    }

    @Test
    fun monthOrder_isAlwaysJanuaryToDecember() {
        val result = postcardMonthsForYear(emptyList(), 2026, zone)

        assertEquals((1..12).toList(), result.map { it.yearMonth.monthValue })
    }

}
