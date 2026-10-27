package com.postcardmemory.ui.components

import com.postcardmemory.data.Postcard
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class PostcardWritingRecordTest {
    private fun postcard() = Postcard(imagePath = "photo", title = "test", capturedAt = 100)

    @Test fun firstNonblankBodyRecordsOnceAndSurvivesEditsAndDeletion() {
        val blank = postcard().withBackMessage(" \n", 1000, 540)
        assertNull(blank.backWrittenAt)
        val started = blank.withBackMessage("첫 편지", 2000, 540)
        val changed = started.withBackMessage("새 내용", 9000, -300).withBackMessage("", 10000, 0)
        assertEquals(2000L, changed.backWrittenAt)
        assertEquals(540, changed.backWrittenOffsetMinutes)
        assertEquals(100L, changed.capturedAt)
    }

    @Test fun legacyPostcardNeverReceivesInventedWritingTime() {
        val old = postcard().copy(backWritingRecordEnabled = false, backMessage = "오래된 편지")
        val edited = old.withBackMessage("수정한 편지", 9000, 540)
        assertNull(edited.backWrittenAt)
        assertNull(edited.backWrittenOffsetMinutes)
    }

    @Test fun postscriptAloneIsBackContentButNotBodyWriting() {
        val ps = postcard().copy(backPostscript = "한마디 더")
        assertNull(ps.backWrittenAt)
        assertTrue(postcardHasBackContent("", "", ps.backPostscript))
        assertFalse(postcardHasBackContent("", "", " \n"))
        assertFalse(postcardHasBackContent("", "", null))
    }

    @Test fun allMonthBoundariesUseKoreanSeasons() {
        assertEquals(listOf("겨울", "겨울", "봄", "봄", "봄", "여름", "여름", "여름", "가을", "가을", "가을", "겨울"),
            (1..12).map(::seasonForMonth))
    }

    @Test fun recordUsesSavedOffsetEvenAfterDeviceZoneChanges() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"))
            val first = formatWritingRecord(0, 540)
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/London"))
            assertEquals("1970-01-01 09:00 · 겨울", first)
            assertEquals(first, formatWritingRecord(0, 540))
            assertEquals("1969-12-31 19:00 · 겨울", formatWritingRecord(0, -300))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test fun missingHistoricalRecordIsHidden() {
        assertNull(formatWritingRecord(null, null))
        assertNull(formatWritingRecord(100, null))
    }
}
