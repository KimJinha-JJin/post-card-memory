package com.postcardmemory.data

import org.junit.Assert.*
import org.junit.Test

class MemoryBookmarkDesignTest {
    @Test fun all27CombinationsRoundTripWithoutSharingPostcardIdentity() {
        val designs = BookmarkShape.entries.flatMap { shape ->
            BookmarkPaper.entries.flatMap { paper ->
                BookmarkTrace.entries.map { trace -> BookmarkDesign(shape, paper, trace) }
            }
        }
        assertEquals(27, designs.toSet().size)
        designs.forEachIndexed { index, design ->
            val row = MemoryBookmark.from(index + 1L, design)
            assertEquals(index + 1L, row.postcardId)
            assertEquals(design, row.designOrNull())
        }
    }

    @Test fun invalidCurrentValuesFallBackWithoutChangingStoredRow() {
        val row = MemoryBookmark(4, "unknown-shape", "unknown-paper", "unknown-trace")
        assertEquals(BookmarkDesign(BookmarkShape.HAND_CUT, BookmarkPaper.IVORY, BookmarkTrace.NONE), row.designOrNull())
        assertEquals("unknown-shape", row.shapeId)
    }

    @Test fun futureFormatIsPreservedAndNotInterpretedAsCurrentDesign() {
        val future = MemoryBookmark(1, formatVersion = 2)
        assertNull(future.designOrNull())
        assertEquals(2, future.formatVersion)
    }

    @Test fun invalidPostcardIdentityCannotBeSaved() {
        for (id in listOf(0L, -1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                MemoryBookmark.from(id, BookmarkDesign())
            }
        }
    }
}
