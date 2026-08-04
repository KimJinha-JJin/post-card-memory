package com.postcardmemory.ui.components

import com.postcardmemory.data.Postcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * EnvelopeStyle.fromStoredValue와 Postcard의 봉투 필드 기본값을 검증한다.
 * 둘 다 순수 Kotlin/데이터 클래스만 다루므로 Robolectric 없이 JVM에서
 * 바로 검증 가능하다(PostcardDateFormatTest와 동일한 전제).
 */
class EnvelopeStyleTest {

    @Test
    fun fromStoredValue_null_meansNoEnvelope() {
        assertNull(EnvelopeStyle.fromStoredValue(null))
    }

    @Test
    fun fromStoredValue_unknownName_fallsBackToNoEnvelope() {
        // 향후 스타일이 추가·삭제되거나 이름이 바뀌어도, 인식 못하는 저장값
        // 때문에 크래시하거나 엉뚱한 스타일로 대체하지 않고 "봉투 없음"으로 본다.
        assertNull(EnvelopeStyle.fromStoredValue("UNKNOWN_FUTURE_STYLE"))
    }

    @Test
    fun fromStoredValue_knownName_resolvesExactEntry() {
        EnvelopeStyle.entries.forEach { style ->
            assertEquals(style, EnvelopeStyle.fromStoredValue(style.name))
        }
    }

    @Test
    fun atLeastFourStylesAvailable() {
        assertTrue(EnvelopeStyle.entries.size >= 4)
    }

    @Test
    fun onlyAirmailHasStripedBorder() {
        assertEquals(
            listOf(EnvelopeStyle.AIRMAIL),
            EnvelopeStyle.entries.filter { it.hasAirmailBorder }
        )
    }

    @Test
    fun newPostcard_hasNoEnvelopeByDefault() {
        val postcard = Postcard(
            id = 1L,
            imagePath = "x",
            title = "t"
        )

        assertNull(postcard.envelopeStyle)
        assertEquals(false, postcard.envelopePostmarked)
    }
}
