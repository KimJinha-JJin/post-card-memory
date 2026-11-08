package com.postcardmemory.ui.gallery

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 선택 중 진행 중인 스와이프·프로그램 이동도 월별로 돌리는 배선을 검사한다.
 * 실제 제스처와 선택 개수 보존은 실기기 QA 대상이며 이 검사는 소스 구조만 보장한다. */
class GallerySelectionPageLockStructureTest {
    private val source by lazy {
        readStructureTestSource(listOf(
            "src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt",
            "app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt"
        ))
    }

    private val effect by lazy {
        val start = source.indexOf("LaunchedEffect(selectionMode, pagerState.currentPage) {")
        assertTrue("선택 변화와 페이지 이동을 모두 감시해야 함", start >= 0)
        val end = source.indexOf("val showPageIndicator", start)
        assertTrue("선택 복귀 effect의 끝을 찾지 못함", end > start)
        source.substring(start, end)
    }

    @Test
    fun selectionAndPageChanges_returnImmediatelyToMonthlyOnlyDuringSelection() {
        assertTrue(effect.contains("if (!selectionMode) return@LaunchedEffect"))
        assertTrue(effect.contains("orderedActiveFormats.indexOf(GalleryPageFormat.MONTHLY)"))
        assertTrue(effect.contains("monthlyIndex >= 0 && pagerState.currentPage != monthlyIndex"))
        assertTrue(effect.contains("pagerState.scrollToPage(monthlyIndex)"))
    }

    @Test
    fun returningToMonthly_doesNotClearOrReplaceSelectedIds() {
        assertFalse("복귀하면서 선택 ID를 비우거나 바꾸면 안 됨", effect.contains("selectedIds"))
        assertTrue(source.contains("val selectionMode = selectedIds.isNotEmpty()"))
        assertTrue(source.contains("text = \"\${selectedIds.size}개 선택\""))
    }
}
