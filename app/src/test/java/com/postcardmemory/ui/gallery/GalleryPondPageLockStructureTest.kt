package com.postcardmemory.ui.gallery

import com.postcardmemory.testsupport.extractBalancedCall
import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 108일차: 연못이 켜져 있는 동안 갤러리 pager가 기억 밀도 페이지로 넘어가지 않는지 고정한다.
 * 연못은 월별 페이지 안에서만 그려지므로 스와이프를 잠그고, 다른 페이지에서 연못을 켜면
 * 월별 페이지로 옮기며, 넘길 페이지가 없음을 알리게 점만 감춘다(자리는 유지). 126일차부터
 * 선택 모드에도 같은 잠금을 적용하며 둘 다 종료하면 복원한다. 쉼터·쫑쫑컵은 pager 밖 별도
 * 분기라 이 조건과 무관하다.
 */
class GalleryPondPageLockStructureTest {

    private val sourceText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt"
            )
        )
    }

    @Test
    fun pager_disablesUserSwipeWhilePondOrSelectionIsOn() {
        val start = sourceText.indexOf("HorizontalPager(\n")
        assertTrue("HorizontalPager를 찾지 못함", start >= 0)
        val call = extractBalancedCall(sourceText, start)

        assertTrue(
            "연못 또는 선택 중이면 잠그고 둘 다 꺼지면 스와이프를 복원해야 함",
            call.contains("userScrollEnabled = !isPondModeOn && !selectionMode")
        )
    }

    @Test
    fun turningPondOn_movesPagerToMonthlyPage() {
        val start = sourceText.indexOf("LaunchedEffect(isPondModeOn) {\n                if (!isPondModeOn) return@LaunchedEffect")
        assertTrue("연못이 켜질 때 페이지를 맞추는 effect를 찾지 못함", start >= 0)
        val body = sourceText.substring(start, sourceText.indexOf("val showPageIndicator", start))

        assertTrue(body.contains("orderedActiveFormats.indexOf(GalleryPageFormat.MONTHLY)"))
        assertTrue(body.contains("pagerState.currentPage != monthlyIndex"))
        assertTrue(body.contains("pagerState.animateScrollToPage(monthlyIndex)"))
    }

    @Test
    fun pageIndicatorDotsHideDuringPond_butReservedSpaceStays() {
        assertTrue(
            "연못 또는 선택 중에는 페이지 점을 그리지 않아야 함",
            sourceText.contains("if (showPageIndicator && !isPondModeOn && !selectionMode) {\n                    GalleryPageIndicator(")
        )
        // 자리(상단 여백)는 연못 여부와 무관하게 showPageIndicator만 따라야 grid가 튀지 않는다.
        assertTrue(
            sourceText.contains(
                "val pageContentPaddingValues = if (showPageIndicator) {"
            )
        )
    }
}
