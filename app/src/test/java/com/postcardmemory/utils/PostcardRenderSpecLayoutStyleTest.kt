package com.postcardmemory.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * resolveLayoutStyle()은 문자열→enum 매핑만 하는 순수 함수라 Robolectric
 * 없이 검증 가능하다(layoutFor()는 android.graphics.RectF를 생성해 이
 * 프로젝트의 unmocked 단위 테스트 스텁에서는 호출할 수 없다 — 여기서는
 * 다루지 않는다).
 */
class PostcardRenderSpecLayoutStyleTest {

    @Test
    fun resolveLayoutStyle_letter_mapsToLetter() {
        assertEquals(
            PostcardRenderSpec.LayoutStyle.LETTER,
            PostcardRenderSpec.resolveLayoutStyle("LETTER")
        )
    }

    @Test
    fun resolveLayoutStyle_existingValues_unchanged() {
        assertEquals(
            PostcardRenderSpec.LayoutStyle.STAMP,
            PostcardRenderSpec.resolveLayoutStyle("STAMP")
        )
        assertEquals(
            PostcardRenderSpec.LayoutStyle.POLAROID,
            PostcardRenderSpec.resolveLayoutStyle("POLAROID")
        )
        assertEquals(
            PostcardRenderSpec.LayoutStyle.TAPED_FILM,
            PostcardRenderSpec.resolveLayoutStyle("TAPED_FILM")
        )
    }

    @Test
    fun resolveLayoutStyle_unknownValue_fallsBackToStamp() {
        assertEquals(
            PostcardRenderSpec.LayoutStyle.STAMP,
            PostcardRenderSpec.resolveLayoutStyle("SOMETHING_FROM_THE_FUTURE")
        )
    }
}
