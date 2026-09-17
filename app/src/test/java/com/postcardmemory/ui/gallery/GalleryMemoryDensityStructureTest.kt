package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 76일차: 기억밀도가 "한 해 동안 어느 달에 기억을 많이 남겼는지 조용히
 * 바라보는" 1월→12월 ASCII 막대그래프로 재정의된 구조를 고정한다 —
 * dashboard·통계 카드가 아니라 막대 + 카오모지만 있는 화면이어야 한다.
 */
class GalleryMemoryDensityStructureTest {

    private val sourceText: String by lazy {
        val candidates = listOf(
            "src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt",
            "app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt"
        )
        candidates.map(::File).firstOrNull(File::exists)?.readText()
            ?: error("GalleryScreen.kt를 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    private fun functionBody(functionName: String, nextFunctionName: String): String {
        val start = sourceText.indexOf("private fun $functionName(")
        assertTrue("$functionName 선언을 찾지 못함", start >= 0)
        val end = sourceText.indexOf("private fun $nextFunctionName(", start)
        assertTrue("$nextFunctionName 선언을 찾지 못함", end > start)
        return sourceText.substring(start, end)
    }

    @Test
    fun densityPage_hasNoYearScrollingOrCardDashboard() {
        val body = functionBody("GalleryDensityPage", "GalleryMemoryDensityBar")

        assertTrue(body.contains("memoryDensityMonthsForYear("))
        assertFalse(
            "연도를 이어붙여 스크롤하던 옛 구조(LazyColumn)가 없어야 함",
            body.contains("LazyColumn")
        )
        assertFalse("통계 Card나 graph panel을 추가하면 안 됨", body.contains("Card("))
        assertFalse("새 modal이나 sheet를 추가하면 안 됨", body.contains("Dialog(") || body.contains("ModalBottomSheet("))
        assertFalse(
            "오늘 범위가 아닌 사진 클릭/탭 상세 진입을 추가하면 안 됨",
            body.contains("onItemClick") || body.contains("onItemLongClick")
        )
    }

    @Test
    fun densityBar_usesBarLevelAndKaomojiNotOldDot() {
        val start = sourceText.indexOf("private fun GalleryMemoryDensityBar(")
        assertTrue("GalleryMemoryDensityBar 선언을 찾지 못함", start >= 0)
        val end = sourceText.indexOf("private fun GalleryMonthlyGridPage(", start)
        assertTrue("GalleryMonthlyGridPage 선언을 찾지 못함", end > start)
        val body = sourceText.substring(start, end)

        assertTrue(body.contains("memoryDensityBarLevel("))
        assertTrue(body.contains("memoryDensityHasOverflow("))
        assertTrue(body.contains("memoryDensityKaomoji("))
        assertFalse(
            "옛 원형 점(intensity 기반 크기·투명도) 문법을 재사용하면 안 됨",
            body.contains("memoryDensityIntensity(")
        )
        assertFalse("RoundedCornerShape 카드 패널을 추가하면 안 됨", body.contains("RoundedCornerShape"))
    }

    @Test
    fun densityPagerBranch_passesOnlyPostcardsAndPadding() {
        val start = sourceText.indexOf("GalleryPageFormat.DENSITY ->")
        assertTrue("DENSITY pager 분기를 찾지 못함", start >= 0)
        val end = sourceText.indexOf("GalleryPageFormat.MONTHLY", start)
        assertTrue("DENSITY pager 분기 끝을 찾지 못함", end > start)
        val body = sourceText.substring(start, end)

        assertTrue(body.contains("GalleryDensityPage("))
        assertTrue(body.contains("postcards = displayedPostcards"))
        assertFalse(
            "76일차부터 기억밀도는 선택 상태나 클릭 콜백을 받지 않아야 함",
            body.contains("selectedIds") || body.contains("onItemClick") || body.contains("onItemLongClick")
        )
    }
}
