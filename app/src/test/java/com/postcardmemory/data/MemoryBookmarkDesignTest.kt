package com.postcardmemory.data

import org.junit.Assert.*
import org.junit.Test

class MemoryBookmarkDesignTest {
    @Test fun everyCombinationRoundTripsWithoutSharingPostcardIdentity() {
        val designs = BookmarkShape.entries.flatMap { shape ->
            BookmarkPaper.entries.flatMap { paper ->
                BookmarkTrace.entries.map { trace -> BookmarkDesign(shape, paper, trace) }
            }
        }
        assertEquals(
            BookmarkShape.entries.size * BookmarkPaper.entries.size * BookmarkTrace.entries.size,
            designs.toSet().size
        )
        designs.forEachIndexed { index, design ->
            val row = MemoryBookmark.from(index + 1L, design)
            assertEquals(index + 1L, row.postcardId)
            assertEquals(design, row.designOrNull())
        }
    }

    /** DB에 저장되는 이름과 승인된 HEX. 이미 저장된 이름이 바뀌면 기존 책갈피 색을 잃어. */
    @Test fun storedPaperNamesAndApprovedColorsStayFixed() {
        assertEquals(
            listOf(
                "IVORY" to 0xFFE8DCC6, "BROWN" to 0xFFBBA184, "MINT" to 0xFFBECFC1,
                "LAVENDER" to 0xFFC8BED5, "BLUE" to 0xFFB4C5CF, "APRICOT" to 0xFFDFC0A3,
                "ROSE" to 0xFFCFAFB2, "OLIVE" to 0xFFC4C5A2, "BUTTER" to 0xFFE5D6A5
            ),
            BookmarkPaper.entries.map { it.name to it.argb }
        )
        assertEquals(listOf("CLASSIC", "HAND_CUT", "MINI_TAG"), BookmarkShape.entries.map { it.name })
        assertEquals(listOf("NONE", "POSTMARK", "LINES"), BookmarkTrace.entries.map { it.name })
        assertEquals(
            BookmarkDesign(BookmarkShape.HAND_CUT, BookmarkPaper.IVORY, BookmarkTrace.POSTMARK),
            BookmarkDesign()
        )
    }

    /** 142일차 세 색으로 저장된 행은 색이 늘어난 뒤에도 같은 모습으로 읽혀. */
    @Test fun rowsSavedBeforePaletteExpansionKeepTheirDesign() {
        for (shape in BookmarkShape.entries) for (trace in BookmarkTrace.entries) {
            for (name in listOf("IVORY", "BROWN", "MINT")) {
                val design = MemoryBookmark(9, shape.name, name, trace.name, 1).designOrNull()!!
                assertEquals(BookmarkDesign(shape, BookmarkPaper.valueOf(name), trace), design)
            }
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
