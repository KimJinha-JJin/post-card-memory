package com.postcardmemory.ui.components

import com.postcardmemory.data.Postcard
import java.util.TimeZone
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Three short lines at the normal card width; never a second 500-character body.
const val BACK_POSTSCRIPT_MAX_LENGTH = 60

internal fun Postcard.withBackMessage(
    message: String,
    now: Long,
    offsetMinutes: Int
): Postcard {
    val startRecord = backWritingRecordEnabled && backWrittenAt == null && message.isNotBlank()
    return copy(
        backMessage = message,
        backWrittenAt = if (startRecord) now else backWrittenAt,
        backWrittenOffsetMinutes = if (startRecord) offsetMinutes else backWrittenOffsetMinutes
    )
}

internal fun seasonForMonth(month: Int): String = when (month) {
    in 3..5 -> "봄"
    in 6..8 -> "여름"
    in 9..11 -> "가을"
    else -> "겨울"
}

internal fun formatWritingRecord(writtenAt: Long?, offsetMinutes: Int?): String? {
    if (writtenAt == null || offsetMinutes == null) return null
    val zone = java.util.SimpleTimeZone(offsetMinutes * 60_000, "writing-time")
    val time = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = zone }
    val month = Calendar.getInstance(zone).apply { timeInMillis = writtenAt }.get(Calendar.MONTH) + 1
    return "${PostcardDateFormat.formatIso(writtenAt, zone)} ${time.format(Date(writtenAt))} · ${seasonForMonth(month)}"
}

internal fun writingOffsetMinutes(now: Long): Int = TimeZone.getDefault().getOffset(now) / 60_000
