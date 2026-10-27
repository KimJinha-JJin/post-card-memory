package com.postcardmemory.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 갤러리의 PlayModeSaver는 [GalleryPlayMode]의 `name`을 저장 상태에 넣고 `valueOf`로 되읽는다.
 * 그래서 enum 이름은 화면 이름과 달라도 바꾸면 안 되는 호환성 계약이다 — 특히 동글이 쉼터는
 * 예전 "양떼목장" 시절 이름 `SHEEP_RANCH`로 남아 있다.
 */
class GalleryPlayModeTest {

    @Test
    fun persistedNames_matchTheSavedStateContract() {
        assertEquals(
            listOf("NONE", "POND", "SHEEP_RANCH", "RACE"),
            GalleryPlayMode.entries.map { it.name }
        )
    }

    @Test
    fun legacyShelterName_stillRestores() {
        assertEquals(GalleryPlayMode.SHEEP_RANCH, GalleryPlayMode.valueOf("SHEEP_RANCH"))
    }
}
