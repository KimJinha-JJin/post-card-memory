package com.postcardmemory.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PostcardDateFormat(
    val label: String,
    val previewText: String,
    private val pattern: String,
    private val locale: Locale,
    private val uppercase: Boolean = false
) {
    DOT(
        label = "점 표기",
        previewText = "2026.07.01",
        pattern = "yyyy.MM.dd",
        locale = Locale.KOREAN
    ),
    KOREAN(
        label = "한글 표기",
        previewText = "2026년 7월 1일",
        pattern = "yyyy년 M월 d일",
        locale = Locale.KOREAN
    ),
    ENGLISH_LONG(
        label = "영문 긴 표기",
        previewText = "JULY 1, 2026",
        pattern = "MMMM d, yyyy",
        locale = Locale.ENGLISH,
        uppercase = true
    ),
    ENGLISH_SHORT(
        label = "영문 짧은 표기",
        previewText = "01 JUL 2026",
        pattern = "dd MMM yyyy",
        locale = Locale.ENGLISH,
        uppercase = true
    );

    fun format(
        capturedAt: Long
    ): String {
        val formatted =
            SimpleDateFormat(
                pattern,
                locale
            ).format(
                Date(capturedAt)
            )

        return if (uppercase) {
            formatted.uppercase(locale)
        } else {
            formatted
        }
    }
}
