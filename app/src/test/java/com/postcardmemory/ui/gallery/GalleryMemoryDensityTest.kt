package com.postcardmemory.ui.gallery

import com.postcardmemory.data.Postcard
import java.time.ZoneId
import java.time.YearMonth
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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

    @Test
    fun emptyPostcards_returnsNoMonths() {
        assertTrue(memoryDensityMonthsFor(emptyList(), zone).isEmpty())
    }

    @Test
    fun sameMonthPostcards_areAggregatedTogether() {
        val result = memoryDensityMonthsFor(
            listOf(postcard(1, 2026, 9, 1), postcard(2, 2026, 9, 30)),
            zone
        )

        assertEquals(2, result.single { it.yearMonth.monthValue == 9 }.count)
    }

    @Test
    fun everyYearContainsAllTwelveMonths_includingEmptyPeriods() {
        val result = memoryDensityMonthsFor(
            listOf(postcard(1, 2025, 12, 31), postcard(2, 2026, 1, 1)),
            zone
        )

        assertEquals(24, result.size)
        assertEquals(12, result.count { it.yearMonth.year == 2025 })
        assertEquals(12, result.count { it.yearMonth.year == 2026 })
        assertEquals(0, result.single { it.yearMonth.year == 2025 && it.yearMonth.monthValue == 1 }.count)
        assertEquals(1, result.single { it.yearMonth.year == 2025 && it.yearMonth.monthValue == 12 }.count)
        assertEquals(1, result.single { it.yearMonth.year == 2026 && it.yearMonth.monthValue == 1 }.count)
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

        val result = memoryDensityMonthsFor(listOf(input), zone)

        assertEquals(1, result.single { it.yearMonth.monthValue == 9 }.count)
        assertEquals(0, result.single { it.yearMonth.monthValue == 8 }.count)
    }

    @Test
    fun timeAxis_isAlwaysPastToPresent_regardlessOfInputOrder() {
        val result = memoryDensityMonthsFor(
            listOf(postcard(2, 2026, 1, 1), postcard(1, 2025, 12, 31)),
            zone
        )

        assertEquals(2025, result.first().yearMonth.year)
        assertEquals(1, result.first().yearMonth.monthValue)
        assertEquals(2026, result.last().yearMonth.year)
        assertEquals(12, result.last().yearMonth.monthValue)
    }

    @Test
    fun densityIntensity_handlesEmptyMaximumAndIntermediateCounts() {
        assertEquals(0f, memoryDensityIntensity(count = 0, maxCount = 0))
        assertEquals(1f, memoryDensityIntensity(count = 4, maxCount = 4))
        assertEquals(0.5f, memoryDensityIntensity(count = 2, maxCount = 4))
        assertEquals(1f, memoryDensityIntensity(count = 5, maxCount = 4))
    }

    @Test
    fun selectedMonth_isClearedWhenItsLastPostcardDisappears() {
        val months = memoryDensityMonthsFor(listOf(postcard(1, 2026, 9, 1)), zone)

        assertEquals(
            YearMonth.of(2026, 9),
            selectedMemoryDensityMonth(months, YearMonth.of(2026, 9))?.yearMonth
        )
        assertEquals(
            null,
            selectedMemoryDensityMonth(months, YearMonth.of(2026, 8))
        )
    }
}
