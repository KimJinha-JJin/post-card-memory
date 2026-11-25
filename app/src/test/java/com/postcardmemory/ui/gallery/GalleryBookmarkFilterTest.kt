package com.postcardmemory.ui.gallery

import com.postcardmemory.data.BookmarkDesign
import com.postcardmemory.data.Postcard
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryBookmarkFilterTest {

    private fun postcard(id: Long, message: String = "") =
        Postcard(id = id, imagePath = "p$id", title = "t", capturedAt = 1000L * id, message = message)

    private val cards = listOf(postcard(3, "바다"), postcard(1, "산"), postcard(2, "바다"))
    private val bookmarks = mapOf<Long, BookmarkDesign?>(2L to BookmarkDesign(), 3L to null)

    @Test fun offKeepsTheSameListAndOrder() {
        assertEquals(cards, filterPostcardsForBookmark(cards, bookmarks, false))
        assertEquals(cards, filterPostcardsForBookmark(cards, null, false))
    }

    /** 그릴 수 없는 미래 형식(값 null)도 책갈피가 있는 엽서야. 정렬 순서는 그대로 둬. */
    @Test fun onKeepsOnlyBookmarkedPostcardsInTheGivenOrder() {
        assertEquals(listOf(3L, 2L), filterPostcardsForBookmark(cards, bookmarks, true).map { it.id })
        assertEquals(emptyList<Long>(), filterPostcardsForBookmark(cards, emptyMap(), true).map { it.id })
        assertEquals(emptyList<Long>(), filterPostcardsForBookmark(cards, null, true).map { it.id })
    }

    @Test fun combinesWithSearchAndFollowsRemoval() {
        val searched = filterPostcardsForSearch(cards, "바다")
        assertEquals(listOf(3L, 2L), filterPostcardsForBookmark(searched, bookmarks, true).map { it.id })
        assertEquals(listOf(3L), filterPostcardsForBookmark(searched, bookmarks - 2L, true).map { it.id })
    }
}
