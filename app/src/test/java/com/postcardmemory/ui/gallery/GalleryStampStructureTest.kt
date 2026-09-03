package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 62일차 5차 우표 보기의 최소 구조와 기존 상호작용 재사용을 고정한다. */
class GalleryStampStructureTest {

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
    fun stampPage_usesTwoColumnLazyGrid_withoutDecorativeCard() {
        val body = functionBody("GalleryStampPage", "GalleryStampGridItem")

        assertTrue(body.contains("GridCells.Fixed(2)"))
        assertTrue(body.contains("LazyVerticalGrid("))
        assertFalse("우표들을 다시 Card로 감싸면 안 됨", body.contains("Card("))
    }

    @Test
    fun stampItem_isSinglePerforatedPaperObject_withExistingHandlers() {
        val start = sourceText.indexOf("private fun GalleryStampGridItem(")
        assertTrue("GalleryStampGridItem 선언을 찾지 못함", start >= 0)
        val end = sourceText.indexOf("internal data class GalleryMemoryDensityMonth", start)
        assertTrue("GalleryStampGridItem 끝을 찾지 못함", end > start)
        val body = sourceText.substring(start, end)

        assertTrue("기존 톱니 shape를 재사용해야 함", body.contains(".clip(PinkingPhotoShape)"))
        assertTrue("우표 종이는 기존 PaperSurface를 써야 함", body.contains(".background(PaperSurface)"))
        assertTrue("얇은 종이 여백이 있어야 함", body.contains(".padding(STAMP_PAPER_PADDING)"))
        assertTrue("기존 Coil thumbnail 경로를 유지해야 함", body.contains("AsyncImage("))
        assertTrue("원본 경로는 표시만 해야 함", body.contains("model = File(postcard.imagePath)"))
        assertTrue("기존 click 경로를 유지해야 함", body.contains("onClick = onClick"))
        assertTrue("기존 long-click 선택 경로를 유지해야 함", body.contains("onLongClick = onLongClick"))
        assertTrue("선택은 작은 기존 coral marker로 표시해야 함", body.contains("color = BrutalCoral"))
        assertFalse("우표 바깥에 Card를 추가하면 안 됨", body.contains("Card("))
        assertFalse("이중 톱니를 만드는 StampCardContent를 넣으면 안 됨", body.contains("StampCardContent("))
        assertFalse("둥근 panel을 추가하면 안 됨", body.contains("RoundedCornerShape"))
    }

    @Test
    fun stampPagerBranch_usesDedicatedState_andExistingCallbacks() {
        val branchStart = sourceText.indexOf("GalleryPageFormat.STAMP ->")
        assertTrue("STAMP pager 분기를 찾지 못함", branchStart >= 0)
        val branchEnd = sourceText.indexOf("GalleryPageFormat.CALENDAR ->", branchStart)
        assertTrue("STAMP pager 분기 끝을 찾지 못함", branchEnd > branchStart)
        val body = sourceText.substring(branchStart, branchEnd)

        assertTrue(body.contains("GalleryStampPage("))
        assertTrue(body.contains("gridState = stampGridState"))
        assertTrue(body.contains("onItemClick = ::handleItemClick"))
        assertTrue(body.contains("onItemLongClick = ::handleItemLongClick"))
    }

    @Test
    fun pageFormatOrder_keepsAllSixViewsInProductOrder() {
        assertTrue(
            GalleryPageFormat.entries == listOf(
                GalleryPageFormat.THREE_COLUMN,
                GalleryPageFormat.MONTHLY,
                GalleryPageFormat.TIMELINE,
                GalleryPageFormat.CALENDAR,
                GalleryPageFormat.STAMP,
                GalleryPageFormat.DENSITY
            )
        )
    }
}
