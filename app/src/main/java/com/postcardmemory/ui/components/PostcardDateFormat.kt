package com.postcardmemory.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 엽서에 표시되는 캡처 날짜의 서식. 값과 무관하게 항상 yyyy-MM-dd로
 * 표시한다. enum 값 자체는 기존에 저장된 Postcard.dateFormat 문자열
 * (예: "ENGLISH_SHORT")과의 파싱 호환을 위해 유지한다.
 */
enum class PostcardDateFormat {
    DOT,
    KOREAN,
    ENGLISH_LONG,
    ENGLISH_SHORT;

    fun format(
        capturedAt: Long
    ): String = formatIso(capturedAt)

    companion object {
        fun formatIso(
            capturedAt: Long
        ): String =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).format(
                Date(capturedAt)
            )
    }
}
