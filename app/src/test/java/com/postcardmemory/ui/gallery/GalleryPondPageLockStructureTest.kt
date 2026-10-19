package com.postcardmemory.ui.gallery

import com.postcardmemory.testsupport.extractBalancedCall
import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 108일차: 연못이 켜져 있는 동안 갤러리 pager가 기억 밀도 페이지로 넘어가지 않는지 고정한다.
 * 연못은 월별 페이지 안에서만 그려지므로 스와이프를 잠그고, 다른 페이지에서 연못을 켜면
 * 월별 페이지로 옮기며, 넘길 페이지가 없음을 알리게 점만 감춘다(자리는 유지). 연못을 끄면
 * 같은 조건이 풀려 기존 두 페이지 구조가 그대로 돌아온다. 쉼터·쫑쫑컵은 pager 밖 별도
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
    fun pager_disablesUserSwipeOnlyWhilePondIsOn() {
        val start = sourceText.indexOf("HorizontalPager(\n")
        assertTrue("HorizontalPager를 찾지 못함", start >= 0)
        val call = extractBalancedCall(sourceText, start)

        assertTrue(
            "연못일 때만 사용자 스와이프를 잠가야 함",
            call.contains("userScrollEnabled = !isPondModeOn")
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
            "연못 중에는 페이지 점을 그리지 않아야 함",
            sourceText.contains("if (showPageIndicator && !isPondModeOn) {\n                    GalleryPageIndicator(")
        )
        // 자리(상단 여백)는 연못 여부와 무관하게 showPageIndicator만 따라야 grid가 튀지 않는다.
        assertTrue(
            sourceText.contains(
                "val pageContentPaddingValues = if (showPageIndicator) {"
            )
        )
    }
}
