package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 62일차 추가 작업: Gallery 보기 관리가 우측 상단 한 곳에서만
 * [GalleryPageFormat] 활성 집합을 조작하고, 3단 페이지가 다시 legacy
 * 목록 보기로 바뀌지 않는 구조를 고정한다.
 */
class GalleryViewSelectionStructureTest {

    private val sourceText: String by lazy {
        val candidates = listOf(
            "src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt",
            "app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt"
        )
        candidates
            .map(::File)
            .firstOrNull(File::exists)
            ?.readText()
            ?: error("GalleryScreen.kt를 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    private fun section(startMarker: String, endMarker: String): String {
        val start = sourceText.indexOf(startMarker)
        assertTrue("시작 marker를 찾지 못함: $startMarker", start >= 0)

        val end = sourceText.indexOf(endMarker, start + startMarker.length)
        assertTrue("끝 marker를 찾지 못함: $endMarker", end > start)

        return sourceText.substring(start, end)
    }

    @Test
    fun topBarViewMenu_managesAllGalleryPageFormatsFromActiveSet() {
        val topBar = section("topBar = {", "floatingActionButton = {")

        assertTrue(topBar.contains("contentDescription = \"보기 형식 관리\""))
        assertTrue(topBar.contains("GalleryPageFormat.entries.forEach"))
        assertTrue(topBar.contains("checked = format in activePageFormats"))
        assertTrue(topBar.contains("toggleActivePageFormat(format)"))
        assertFalse(topBar.contains("3열 그리드 보기"))
        assertFalse(topBar.contains("세부 기록 보기"))
    }

    @Test
    fun featureDrawer_noLongerContainsGalleryPageFormatSettings() {
        val drawer = section(
            "private fun GalleryFeatureDrawer(",
            "private fun GalleryPageFormatMenuItem("
        )

        assertFalse(drawer.contains("activePageFormats"))
        assertFalse(drawer.contains("GalleryPageFormat.entries"))
        assertFalse(drawer.contains("보기 형식"))
        assertTrue(drawer.contains("특별한 갤러리"))
    }

    @Test
    fun threeColumnPage_alwaysRendersThreeColumnGrid() {
        val threeColumnPage = section(
            "private fun GalleryThreeColumnPage(",
            "private val monthlyGridDayLabelFormatter"
        )

        assertTrue(threeColumnPage.contains("GalleryGrid("))
        assertFalse(threeColumnPage.contains("viewMode"))
        assertFalse(threeColumnPage.contains("GalleryDetailList("))
        assertFalse(threeColumnPage.contains("detailListState"))
    }

    @Test
    fun legacyViewMode_hasNoGalleryScreenStateOrBranch() {
        assertFalse(sourceText.contains("ViewModeSaver"))
        assertFalse(sourceText.contains("var viewMode"))
        assertFalse(sourceText.contains("GalleryViewMode."))
    }

    @Test
    fun fixedThreeColumnEntry_staysCheckedAndCannotBeDisabled() {
        val menuItem = section(
            "private fun GalleryPageFormatMenuItem(",
            "private fun GalleryPlayModeDrawerItem("
        )

        assertTrue(menuItem.contains("if (checked)"))
        assertTrue(menuItem.contains("Icons.Default.Check"))
        assertTrue(menuItem.contains("enabled = !locked"))
    }
}
