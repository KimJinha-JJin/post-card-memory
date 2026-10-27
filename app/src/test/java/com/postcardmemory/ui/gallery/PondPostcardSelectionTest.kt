package com.postcardmemory.ui.gallery

import com.postcardmemory.data.Postcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 108일차: 연못에 띄울 엽서 선정 규칙([pondPostcardsFor])을 고정한다 — 엽서 날짜
 * (capturedAt) 최신순, 전체에서 최대 [POND_MAX_POSTCARDS]장, 같은 날짜는 id 큰 쪽 먼저.
 */
class PondPostcardSelectionTest {

    private fun postcard(id: Long, capturedAt: Long) =
        Postcard(id = id, imagePath = "photo", title = "t$id", capturedAt = capturedAt)

    @Test
    fun picksNewestTenAcrossAllPostcards() {
        // 여러 달에 걸친 15장 — 달마다 10장이 아니라 전체에서 최신 10장이어야 한다.
        val postcards = (1L..15L).map { postcard(id = it, capturedAt = it * 1_000_000_000L) }

        val picked = pondPostcardsFor(postcards.shuffled(java.util.Random(7)))

        assertEquals(POND_MAX_POSTCARDS, picked.size)
        assertEquals((15L downTo 6L).toList(), picked.map { it.id })
    }

    @Test
    fun fewerThanTen_returnsOnlyExistingPostcards() {
        val postcards = listOf(postcard(1, 300), postcard(2, 100), postcard(3, 200))

        assertEquals(listOf(1L, 3L, 2L), pondPostcardsFor(postcards).map { it.id })
        assertTrue(pondPostcardsFor(emptyList()).isEmpty())
    }

    @Test
    fun sameDate_keepsStableOrderByNewerIdFirst() {
        val postcards = listOf(postcard(4, 500), postcard(9, 500), postcard(2, 500), postcard(7, 900))

        val forward = pondPostcardsFor(postcards).map { it.id }
        val reversed = pondPostcardsFor(postcards.reversed()).map { it.id }

        assertEquals(listOf(7L, 9L, 4L, 2L), forward)
        assertEquals("입력 순서(갤러리 정렬)와 무관해야 함", forward, reversed)
    }
}
