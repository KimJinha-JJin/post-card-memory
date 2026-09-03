package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 62일차 4차: 캘린더 보기가 날짜 칸 안에 사진을 억지로 넣지 않고(작업지시서
 * 24절 — 날짜·thumbnail·선택 상태가 경쟁하지 않게), 새 modal/bottom sheet
 * 없이 기존 클릭 경로로 상세 화면과 연결되는지 고정한다. Compose UI 테스트
 * 인프라가 없는 프로젝트 관례에 따라 소스 텍스트 기준으로 검사한다.
 */
class GalleryCalendarStructureTest {

    private val sourceText: String by lazy {
        val candidates = listOf(
            "src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt",
            "app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt"
        )
        val file = candidates
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error(
                "소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                    "candidates=$candidates"
            )
        file.readText()
    }

    private fun functionBody(functionName: String, nextFunctionName: String): String {
        val start = sourceText.indexOf("private fun $functionName(")
        assertTrue("$functionName 선언을 찾지 못함", start >= 0)

        val end = sourceText.indexOf("private fun $nextFunctionName(", start)
        assertTrue("$nextFunctionName 선언을 찾지 못함", end > start)

        return sourceText.substring(start, end)
    }

    @Test
    fun calendarDayCell_hasNoThumbnailOrCardWrapping() {
        val body = functionBody("GalleryCalendarDayCell", "GalleryGrid")

        assertFalse(
            "날짜 칸 안에는 사진(StampCardContent/StampPhoto)을 넣지 않아야 함",
            body.contains("StampCardContent(") || body.contains("StampPhoto(")
        )
        assertFalse(
            "날짜 칸을 감싸는 Card를 새로 만들면 안 됨",
            body.contains("Card(")
        )
    }

    @Test
    fun calendarPage_usesModalessSelectionFlow_reusingExistingItemClick() {
        val body = functionBody("GalleryCalendarPage", "GalleryCalendarDayCell")

        assertFalse(
            "새 modal/bottom sheet(Dialog/ModalBottomSheet)를 추가하면 안 됨",
            body.contains("Dialog(") || body.contains("ModalBottomSheet(")
        )
        assertTrue(
            "선택한 날짜의 사진 클릭은 기존 onItemClick 경로를 그대로 써야 함",
            body.contains("onClick = { onItemClick(postcard.id) }")
        )
        assertTrue(
            "월 grid는 Lazy 없이 고정 높이 Column으로 그려야 함",
            body.contains("cells.chunked(7)")
        )
    }
}
