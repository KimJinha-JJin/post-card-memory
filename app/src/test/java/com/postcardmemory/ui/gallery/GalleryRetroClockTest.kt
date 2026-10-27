package com.postcardmemory.ui.gallery

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryRetroClockTest {

    @Test fun retroClockTimeTextFormatsAfternoonTimeWithZeroPaddedParts() {
        val result = retroClockTimeTextFor(LocalTime.of(17, 49, 32))
        assertEquals("05:49", result.hourMinute)
        assertEquals("32", result.second)
        assertEquals("PM", result.meridiem)
    }

    @Test fun retroClockTimeTextFormatsMorningTime() {
        val result = retroClockTimeTextFor(LocalTime.of(9, 3, 7))
        assertEquals("09:03", result.hourMinute)
        assertEquals("07", result.second)
        assertEquals("AM", result.meridiem)
    }

    @Test fun retroClockTimeTextHandlesMidnightAs12AM() {
        val result = retroClockTimeTextFor(LocalTime.of(0, 5, 9))
        assertEquals("12:05", result.hourMinute)
        assertEquals("AM", result.meridiem)
    }

    @Test fun retroClockTimeTextHandlesNoonAs12PM() {
        val result = retroClockTimeTextFor(LocalTime.of(12, 0, 0))
        assertEquals("12:00", result.hourMinute)
        assertEquals("PM", result.meridiem)
    }

    @Test fun retroClockTimeTextHandlesJustAfterNoonAndJustBeforeMidnightBoundaries() {
        assertEquals("PM", retroClockTimeTextFor(LocalTime.of(12, 1, 0)).meridiem)
        assertEquals("01:00", retroClockTimeTextFor(LocalTime.of(13, 0, 0)).hourMinute)
        assertEquals("AM", retroClockTimeTextFor(LocalTime.of(11, 59, 59)).meridiem)
        assertEquals("11:59", retroClockTimeTextFor(LocalTime.of(11, 59, 59)).hourMinute)
    }

    @Test fun retroClockDateTextPutsYearThenMonthAbbreviationThenDayThenWeekdayAbbreviation() {
        // 2026-09-18은 금요일이다.
        assertEquals("2026 SEP 18 FRI", retroClockDateTextFor(LocalDate.of(2026, 9, 18)))
    }

    @Test fun retroClockDateTextZeroPadsSingleDigitDay() {
        assertEquals("2026 JAN 05 MON", retroClockDateTextFor(LocalDate.of(2026, 1, 5)))
    }

    @Test fun retroClockDateTextCoversAllTwelveMonthAbbreviationsAndAllSevenWeekdayAbbreviations() {
        val expectedMonths = listOf(
            "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
        )
        (1..12).forEach { month ->
            val text = retroClockDateTextFor(LocalDate.of(2026, month, 1))
            assertEquals(expectedMonths[month - 1], text.split(" ")[1])
        }
        val expectedDows = setOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
        val seenDows = (0L..6L).map { offset ->
            retroClockDateTextFor(LocalDate.of(2026, 9, 14).plusDays(offset)).split(" ")[3]
        }.toSet()
        assertEquals(expectedDows, seenDows)
    }

    @Test fun retroClockAccessibilityDescriptionIsNaturalKoreanRegardlessOfOnScreenAbbreviations() {
        val description = retroClockAccessibilityDescriptionFor(
            LocalDateTime.of(2026, 9, 18, 17, 49, 32)
        )
        assertEquals(
            "현재 시간 오후 5시 49분 32초, 2026년 9월 18일 금요일",
            description
        )
    }

    // 77일차 추가 v2: 7세그먼트 on/off 패턴은 순수 데이터라 Canvas/Path 없이도 검증 가능하다.
    // Path 렌더링 자체(육각형 좌표)는 목업 HTML을 Chrome headless로 캡처해 0~9 전체를
    // 직접 확인했고(막대기 조립품처럼 보이지 않음), 여기서는 어떤 세그먼트가 켜지는지만 검증한다.

    @Test fun sevenSegmentPatternsCoverAllTenDigitsWithNoUnknownSegmentNames() {
        assertEquals((0..9).map { it.toString()[0] }.toSet(), SEVEN_SEGMENT_PATTERNS.keys)
        SEVEN_SEGMENT_PATTERNS.values.forEach { segments ->
            assertTrue(segments.isNotEmpty())
            assertTrue(segments.all { it in setOf('a', 'b', 'c', 'd', 'e', 'f', 'g') })
        }
    }

    @Test fun sevenSegmentPatternsMatchStandardDigitalClockGlyphs() {
        assertEquals(setOf('a', 'b', 'c', 'd', 'e', 'f'), SEVEN_SEGMENT_PATTERNS.getValue('0'))
        assertEquals(setOf('b', 'c'), SEVEN_SEGMENT_PATTERNS.getValue('1'))
        assertEquals(setOf('a', 'b', 'd', 'e', 'g'), SEVEN_SEGMENT_PATTERNS.getValue('2'))
        assertEquals(setOf('a', 'b', 'c', 'd', 'g'), SEVEN_SEGMENT_PATTERNS.getValue('3'))
        assertEquals(setOf('b', 'c', 'f', 'g'), SEVEN_SEGMENT_PATTERNS.getValue('4'))
        assertEquals(setOf('a', 'c', 'd', 'f', 'g'), SEVEN_SEGMENT_PATTERNS.getValue('5'))
        assertEquals(setOf('a', 'c', 'd', 'e', 'f', 'g'), SEVEN_SEGMENT_PATTERNS.getValue('6'))
        assertEquals(setOf('a', 'b', 'c'), SEVEN_SEGMENT_PATTERNS.getValue('7'))
        assertEquals(setOf('a', 'b', 'c', 'd', 'e', 'f', 'g'), SEVEN_SEGMENT_PATTERNS.getValue('8'))
        assertEquals(setOf('a', 'b', 'c', 'd', 'f', 'g'), SEVEN_SEGMENT_PATTERNS.getValue('9'))
    }

    @Test fun sevenSegmentEightIsTheOnlyDigitUsingAllSevenSegments() {
        assertEquals(
            listOf('8'),
            SEVEN_SEGMENT_PATTERNS.filterValues { it.size == 7 }.keys.toList()
        )
    }

    @Test fun retroClockAccessibilityDescriptionHandlesMidnightAndNoonInKorean() {
        assertEquals(
            "현재 시간 오전 12시 0분 0초, 2026년 1월 1일 목요일",
            retroClockAccessibilityDescriptionFor(LocalDateTime.of(2026, 1, 1, 0, 0, 0))
        )
        assertEquals(
            "현재 시간 오후 12시 0분 0초, 2026년 1월 1일 목요일",
            retroClockAccessibilityDescriptionFor(LocalDateTime.of(2026, 1, 1, 12, 0, 0))
        )
    }
}
