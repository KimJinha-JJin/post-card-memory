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
 * 해의 1월→12월 그래프"로 재정의되며, 그 월별 집계([memoryDensityMonthsForYear])를
 * 검증한다. 93일차에 줄기·하트·얼굴을 걷어내고 중성펜 막대(비례형, 20장 = 꽉 참)로
 * 바뀌어, 높이 비율([memoryDensityHeightFraction]), 20장 초과 표시
 * ([memoryDensityHasOverflow]), 펜 획 계산([memoryDensityGelPenBarStrokes])을 검증한다.
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

    // ── 93일차: 비례형 높이(20장 = 꽉 참) ──────────────────────────────

    @Test
    fun heightFraction_growsOnePostcardAtATimeUpToTwenty() {
        assertEquals(0f, memoryDensityHeightFraction(0), 0.0001f)
        assertEquals(0.05f, memoryDensityHeightFraction(1), 0.0001f)
        assertEquals(0.5f, memoryDensityHeightFraction(10), 0.0001f)
        assertEquals(1f, memoryDensityHeightFraction(20), 0.0001f)
        val fractions = (0..20).map { memoryDensityHeightFraction(it) }
        for (i in 1 until fractions.size) {
            assertTrue("${i}장이 ${i - 1}장보다 높아야 함(계단 없이 1장마다 자람)", fractions[i] > fractions[i - 1])
        }
    }

    @Test
    fun heightFraction_staysFullAboveTwentyAndZeroForNegative() {
        assertEquals(1f, memoryDensityHeightFraction(21), 0.0001f)
        assertEquals(1f, memoryDensityHeightFraction(100), 0.0001f)
        assertEquals(0f, memoryDensityHeightFraction(-3), 0.0001f)
    }

    @Test
    fun overflow_onlyTrueAboveTwentyPostcards() {
        assertFalse(memoryDensityHasOverflow(20))
        assertTrue(memoryDensityHasOverflow(21))
        assertTrue(memoryDensityHasOverflow(100))
    }

    // ── 93일차: 중성펜 막대 획 ────────────────────────────────────────

    @Test
    fun gelPenBar_sameSeedDrawsSameStrokes() {
        val first = memoryDensityGelPenBarStrokes(4f, 30f, 14f, 90f, seed = 7)
        val second = memoryDensityGelPenBarStrokes(4f, 30f, 14f, 90f, seed = 7)

        assertEquals(first.size, second.size)
        first.zip(second).forEach { (a, b) ->
            assertEquals(a.points, b.points)
            assertEquals(a.alpha, b.alpha, 0f)
        }
    }

    @Test
    fun gelPenBar_staysInsideBarSilhouetteWithinOneAndAHalfDp() {
        // 손맛은 표면에만: 반복선·외곽선 어느 점도 막대 사각형에서 1.5dp 넘게 벗어나지 않는다.
        val left = 4f
        val top = 30f
        val width = 14f
        val height = 90f
        (1..20).forEach { seed ->
            val strokes = memoryDensityGelPenBarStrokes(left, top, width, height, seed)
            assertTrue("seed=$seed: 획이 있어야 함", strokes.isNotEmpty())
            strokes.flatMap { it.points }.forEach { p ->
                assertTrue("seed=$seed x=${p.x}", p.x >= left - 1.5f && p.x <= left + width + 1.5f)
                assertTrue("seed=$seed y=${p.y}", p.y >= top - 1.5f && p.y <= top + height + 1.5f)
            }
        }
    }

    @Test
    fun gelPenBar_emptyBarDrawsNothing() {
        assertTrue(memoryDensityGelPenBarStrokes(4f, 30f, 14f, 0f, seed = 1).isEmpty())
    }
}
