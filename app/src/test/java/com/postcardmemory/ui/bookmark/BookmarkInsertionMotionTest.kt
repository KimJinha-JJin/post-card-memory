package com.postcardmemory.ui.bookmark

import com.postcardmemory.data.BookmarkShape
import com.postcardmemory.ui.components.BOOKMARK_LEFT_FRACTION
import com.postcardmemory.ui.components.BOOKMARK_WIDTH_FRACTION
import com.postcardmemory.ui.components.insertedBookmarkBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkInsertionMotionTest {

    private fun frame(ms: Int) = bookmarkInsertionFrame(ms.toFloat())

    @Test fun startsBesideAndEndsFullyInsertedWithThePostcardDown() {
        assertEquals(BookmarkInsertionFrame(0f, 0f), frame(0))
        assertEquals(BookmarkInsertionFrame(0f, 1f), frame(BOOKMARK_INSERT_TOTAL_MS))
        assertEquals(frame(0), frame(-50))
        assertEquals(frame(BOOKMARK_INSERT_TOTAL_MS), frame(5000))
    }

    @Test fun postcardLiftsBeforePaperMovesAndSettlesWhileItArrives() {
        assertEquals(1f, frame(180).lift, 1e-4f)
        assertEquals(0f, frame(180).slide, 1e-4f)
        assertEquals(1f, frame(500).lift, 1e-4f)
        assertTrue(frame(710).slide > .9f)
        assertEquals(1f, frame(880).slide, 1e-4f)
        assertTrue(frame(800).lift < 1f)
    }

    @Test fun paperNeverSlidesBackOut() {
        val slides = (0..BOOKMARK_INSERT_TOTAL_MS step 5).map { frame(it).slide }
        assertTrue(slides.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(slides.all { it in 0f..1f })
    }

    /** 내미는 길이는 QA에서 정한 9~10%이고, 클래식 V자 파임보다 길며, 가운데 날짜와 카드 오른쪽 밖을 침범하지 않아. */
    @Test fun exposureStaysWithinApprovedRangeForEveryShape() {
        for (shape in BookmarkShape.entries) for (edge in listOf(96f, 114.7f, 240f)) {
            val bounds = insertedBookmarkBounds(shape, edge)
            val exposure = (bounds.bottom - edge) / edge
            assertTrue("$shape $exposure", exposure in .09f..0.10f)
            assertTrue(bounds.right <= edge)
            assertTrue(bounds.top > 0f)
            assertTrue(BOOKMARK_LEFT_FRACTION > .6f)
            assertEquals(edge * BOOKMARK_WIDTH_FRACTION, bounds.width, 1e-3f)
        }
    }
}
