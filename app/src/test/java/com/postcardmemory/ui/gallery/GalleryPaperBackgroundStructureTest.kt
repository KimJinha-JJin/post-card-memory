package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 89일차: 메인 갤러리 종이 배경의 구조를 고정한다 — 종이는 구분선 아래
 * pager 밖 한 레이어에서만 그려지고(스크롤·보기 전환에 따라 움직이지 않음),
 * 늘리지 않고 타일로 반복되며, 시계·커피 header는 불투명하다. 농도·색 같은
 * 미감 값은 실기기 QA 대상이라 여기서 고정하지 않는다. `src/test` JVM에는
 * Robolectric이 없어 소스 텍스트 기준으로 검사한다.
 */
class GalleryPaperBackgroundStructureTest {

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

    private fun functionBody(name: String): String {
        val start = sourceText.indexOf("private fun $name(")
        assertTrue("$name 선언을 찾지 못함", start >= 0)
        val end = sourceText.indexOf("\n@Composable", start).let { if (it < 0) sourceText.length else it }
        return sourceText.substring(start, end)
    }

    @Test
    fun paperTile_isLoadedOnlyInsideGalleryPaperBackground() {
        assertEquals(
            "종이 타일 drawable은 GalleryScreen.kt에서 한 번만 참조해야 함(목록 item마다 decode 금지)",
            1,
            Regex("""R\.drawable\.gallery_paper_tile""").findAll(sourceText).count()
        )
        assertTrue(
            "종이 타일은 GalleryPaperBackground 안에서만 불러와야 함",
            functionBody("GalleryPaperBackground").contains("R.drawable.gallery_paper_tile")
        )
    }

    @Test
    fun paperTile_repeatsWithoutStretching() {
        val body = functionBody("GalleryPaperBackground")
        assertTrue(
            "종이는 가로·세로 모두 TileMode.Repeated로 반복해야 함",
            body.contains("ImageShader(tile, TileMode.Repeated, TileMode.Repeated)")
        )
        assertTrue(
            "타일은 가로·세로 같은 배율로만 조정해야 함(늘림 금지)",
            body.contains("setScale(scale, scale)")
        )
        assertFalse("FillBounds로 늘리면 안 됨", body.contains("FillBounds"))
    }

    @Test
    fun paperBackground_sitsOutsidePagerBelowHeader() {
        val pagerBoxStart = sourceText.indexOf("GalleryPaperBackground(\n")
        val pagerStart = sourceText.indexOf("HorizontalPager(\n")
        assertTrue("pager 바깥 종이 레이어를 찾지 못함", pagerBoxStart >= 0 && pagerStart > 0)
        assertTrue("종이 레이어는 pager보다 먼저(뒤쪽 z-order에) 있어야 함", pagerBoxStart < pagerStart)
        assertTrue(
            "종이 레이어는 header 높이만큼 아래에서 시작해야 함",
            sourceText.substring(pagerBoxStart, pagerStart)
                .contains(".padding(top = paddingValues.calculateTopPadding())")
        )
    }

    @Test
    fun galleryPages_doNotPaintTheirOwnFullScreenBackground() {
        listOf("GalleryMonthlyGridPage", "GalleryDensityPage", "SearchEmptyState").forEach { name ->
            assertFalse(
                "$name 는 GalleryPaperWhite 전체 배경을 칠하면 안 됨(종이를 가림)",
                functionBody(name).contains(".background(GalleryPaperWhite)")
            )
            assertFalse(
                "$name 안에서 종이 레이어를 다시 그리면 안 됨(스크롤과 함께 움직임)",
                functionBody(name).contains("GalleryPaperBackground(")
            )
        }
    }

    @Test
    fun clockHeader_isOpaque() {
        val clockCall = sourceText.indexOf("GalleryRetroClock(\n")
        assertTrue("GalleryRetroClock 호출을 찾지 못함", clockCall >= 0)
        val headerColumn = sourceText.lastIndexOf("Column(", clockCall)
        assertTrue(
            "시계·커피가 있는 header Column은 불투명 배경을 가져야 함",
            sourceText.substring(headerColumn, headerColumn + 80)
                .startsWith("Column(modifier = Modifier.background(GalleryPaperWhite))")
        )
    }
}
