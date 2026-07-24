package com.postcardmemory.ui.gallery

import com.postcardmemory.data.Postcard
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 갤러리 검색 필터([filterPostcardsForSearch])가 문구·장소·날짜 기준으로
 * 정확히 좁혀지는지, 그리고 정렬에 영향을 주지 않고 입력 순서를 그대로
 * 보존하는지 검증한다. GalleryViewModel과 마찬가지로 GalleryScreen도
 * Context 의존 Compose 트리라 이 프로젝트의 순수 JUnit 환경(Robolectric
 * 미사용)에서 직접 렌더링할 수 없으므로, 필터링 로직만 분리해 검증한다
 * (DeletionMessageLogicTest.kt 참고).
 */
class GallerySearchFilterTest {

    private fun epochMillisOf(year: Int, month: Int, day: Int): Long =
        ZonedDateTime.of(year, month, day, 12, 0, 0, 0, ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private val busan = Postcard(
        id = 1,
        imagePath = "/a.jpg",
        title = "2026-07-01",
        capturedAt = epochMillisOf(2026, 7, 1),
        location = "부산광역시 해운대구",
        message = "부산 바다에서 보낸 여름"
    )

    private val seoulNoLocation = Postcard(
        id = 2,
        imagePath = "/b.jpg",
        title = "2025-01-15",
        capturedAt = epochMillisOf(2025, 1, 15),
        location = null,
        message = "겨울에 만난 찰떡이"
    )

    private val decemberEnglish = Postcard(
        id = 3,
        imagePath = "/c.jpg",
        title = "2026-12-31",
        capturedAt = epochMillisOf(2026, 12, 31),
        location = "",
        message = "Happy New Year Seoul"
    )

    private val allPostcards = listOf(busan, seoulNoLocation, decemberEnglish)

    @Test
    fun message_partialMatch_findsPostcard() {
        val result = filterPostcardsForSearch(allPostcards, "여름")

        assertEquals(listOf(busan), result)
    }

    @Test
    fun message_koreanSubstring_matchesAcrossWords() {
        val result = filterPostcardsForSearch(allPostcards, "찰떡이")

        assertEquals(listOf(seoulNoLocation), result)
    }

    @Test
    fun location_partialMatch_findsPostcard() {
        val result = filterPostcardsForSearch(allPostcards, "해운대")

        assertEquals(listOf(busan), result)
    }

    @Test
    fun location_null_isSkippedSafely_noCrash() {
        val result = filterPostcardsForSearch(allPostcards, "seoul")

        assertEquals(listOf(decemberEnglish), result)
    }

    @Test
    fun location_blank_isSkippedSafely_noMatch() {
        val result = filterPostcardsForSearch(allPostcards, "")

        assertEquals(allPostcards, result)
    }

    @Test
    fun date_yearOnly_matchesAllPostcardsInThatYear() {
        val result = filterPostcardsForSearch(allPostcards, "2026")

        assertEquals(listOf(busan, decemberEnglish), result)
    }

    @Test
    fun date_isoYearMonth_matchesOnlyThatMonth() {
        val result = filterPostcardsForSearch(allPostcards, "2026-07")

        assertEquals(listOf(busan), result)
    }

    @Test
    fun date_koreanYearMonth_matchesOnlyThatMonth() {
        val result = filterPostcardsForSearch(allPostcards, "2026년 7월")

        assertEquals(listOf(busan), result)
    }

    @Test
    fun query_blank_or_whitespaceOnly_returnsFullListUnfiltered() {
        assertEquals(allPostcards, filterPostcardsForSearch(allPostcards, ""))
        assertEquals(allPostcards, filterPostcardsForSearch(allPostcards, "   "))
    }

    @Test
    fun query_leadingAndTrailingWhitespace_isTrimmedBeforeMatching() {
        val result = filterPostcardsForSearch(allPostcards, "  부산  ")

        assertEquals(listOf(busan), result)
    }

    @Test
    fun query_mixedCaseEnglish_isCaseInsensitive() {
        val lower = filterPostcardsForSearch(allPostcards, "seoul")
        val upper = filterPostcardsForSearch(allPostcards, "SEOUL")
        val mixed = filterPostcardsForSearch(allPostcards, "SeOuL")

        assertEquals(listOf(decemberEnglish), lower)
        assertEquals(listOf(decemberEnglish), upper)
        assertEquals(listOf(decemberEnglish), mixed)
    }

    @Test
    fun noMatch_returnsEmptyList() {
        val result = filterPostcardsForSearch(allPostcards, "존재하지않는단어")

        assertTrue(result.isEmpty())
    }

    @Test
    fun filter_preservesOriginalOrder_soExistingSortStillApplies() {
        val reversed = allPostcards.reversed()

        val result = filterPostcardsForSearch(reversed, "2026")

        assertEquals(listOf(decemberEnglish, busan), result)
    }
}
