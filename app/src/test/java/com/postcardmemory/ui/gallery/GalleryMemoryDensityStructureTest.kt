package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun densityPage_usesMonthlyDotsAndExistingInlinePostcardFlow() {
        val body = functionBody("GalleryDensityPage", "GalleryMemoryDensityDot")

        assertTrue(body.contains("memoryDensityMonthsFor(postcards)"))
        assertTrue(body.contains("months.chunked(6)"))
        assertTrue(body.contains("StampCardContent("))
        assertTrue(body.contains("onClick = { onItemClick(postcard.id) }"))
        assertTrue(body.contains("onLongClick = { onItemLongClick(postcard.id) }"))
        assertFalse("통계 Card나 graph panel을 추가하면 안 됨", body.contains("Card("))
        assertFalse("새 modal이나 sheet를 추가하면 안 됨", body.contains("Dialog(") || body.contains("ModalBottomSheet("))
    }

    @Test
    fun densityDot_isWarmCircle_notGithubGreenSquare() {
        val body = functionBody("GalleryMemoryDensityDot", "GalleryThreeColumnPage")

        assertTrue(body.contains("shape = CircleShape"))
        assertTrue(body.contains("SunsetGold"))
        assertTrue(body.contains("PaperDivider"))
        assertFalse(body.contains("Color.Green"))
        assertFalse(body.contains("RoundedCornerShape"))
    }

    @Test
    fun densityPagerBranch_usesDedicatedStateAndExistingCallbacks() {
        val start = sourceText.indexOf("GalleryPageFormat.DENSITY ->")
        assertTrue("DENSITY pager 분기를 찾지 못함", start >= 0)
        val end = sourceText.indexOf("GalleryPageFormat.STAMP ->", start)
        assertTrue("DENSITY pager 분기 끝을 찾지 못함", end > start)
        val body = sourceText.substring(start, end)

        assertTrue(body.contains("GalleryDensityPage("))
        assertTrue(body.contains("listState = densityListState"))
        assertTrue(body.contains("selectedMonth = densitySelectedMonthKey"))
        assertTrue(body.contains("onItemClick = ::handleItemClick"))
        assertTrue(body.contains("onItemLongClick = ::handleItemLongClick"))
        assertFalse(body.contains("GalleryComingSoonPage("))
    }
}
