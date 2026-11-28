package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * 146일차 B: 편집 화면 저장 실패 창 제목은 예전 코랄 핑크(#FF7F8E, 종이 위 대비 약 2.3:1) 대신 앱의
 * 위험·오류 색(DangerRed #C0392B)을 쓴다. 테마 error 색과 카메라 화면은 이 작업 범위 밖이라 그대로 둔다.
 */
class SaveResultDialogColorStructureTest {

    private val detail: String by lazy {
        File(
            listOf("src/main", "app/src/main").first { File(it).exists() },
            "java/com/postcardmemory/ui/detail/DetailScreen.kt"
        ).readText()
    }

    @Test
    fun saveFailureTitles_useDangerRed_notLegacyCoral() {
        assertFalse("편집 화면에 코랄 제목이 남지 않음", detail.contains("titleColor = BrutalCoral"))
        assertEquals("저장 실패 창 3곳(내보내기·배경·레이아웃)", 3, Regex("titleColor = GalleryDangerRed,").findAll(detail).count())
        assertEquals("완료 창 제목은 잉크색 유지", 1, Regex("titleColor = BrutalBlack,").findAll(detail).count())
    }
}
