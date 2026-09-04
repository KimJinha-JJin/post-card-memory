package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 62일차 추가 작업: Gallery 보기 관리가 우측 상단 한 곳에서만
 * [GalleryPageFormat] 활성 집합을 조작하고, 3단 페이지가 다시 legacy
 * 목록 보기로 바뀌지 않는 구조를 고정한다.
 *
 * 63일차 추가 구현: 좌측 패널([GalleryFeatureDrawer])이 완전히 제거되고
 * 그 진입 기능(미래 우체통, 특별한 갤러리 3종)이 우측 하단 + 클러스터
 * ([GalleryFabCluster])로 흡수된 구조를 고정한다.
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
        val topBar = section("topBar = {", ") { paddingValues ->")

        assertTrue(topBar.contains("contentDescription = \"보기 형식 관리\""))
        assertTrue(topBar.contains("GalleryPageFormat.entries.forEach"))
        assertTrue(topBar.contains("checked = format in activePageFormats"))
        assertTrue(topBar.contains("toggleActivePageFormat(format)"))
        assertFalse(topBar.contains("3열 그리드 보기"))
        assertFalse(topBar.contains("세부 기록 보기"))
        assertFalse(topBar.contains("기능 메뉴 열기"))
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
            "private val PondDrawerIcon"
        )

        assertTrue(menuItem.contains("if (checked)"))
        assertTrue(menuItem.contains("Icons.Default.Check"))
        assertTrue(menuItem.contains("enabled = !locked"))
    }

    @Test
    fun leftPanelDrawer_isCompletelyRemoved() {
        assertFalse(sourceText.contains("GalleryFeatureDrawer("))
        assertFalse(sourceText.contains("ModalNavigationDrawer"))
        assertFalse(sourceText.contains("ModalDrawerSheet"))
        assertFalse(sourceText.contains("NavigationDrawerItem"))
        assertFalse(sourceText.contains("GalleryPlayModeDrawerItem"))
        assertFalse(sourceText.contains("rememberDrawerState"))
        assertFalse(sourceText.contains("기능 메뉴 열기"))
    }

    @Test
    fun fabCluster_reusesExistingIconsForCameraFutureMailboxAndPlayModes() {
        val cluster = section(
            "private fun GalleryFabCluster(",
            "private fun BoxScope.GalleryFabShortcut("
        )

        // 엽서 생성 — 기존 카메라 FAB 문법(같은 drawable) 그대로 재사용
        assertTrue(cluster.contains("R.drawable.ic_camera_button"))
        assertTrue(cluster.contains("contentDescription = \"카메라\""))

        // 미래 우체통 — 기존 아이콘과 콜백 재사용, 새 destination 없음
        assertTrue(cluster.contains("Icons.Default.MailOutline"))
        assertTrue(cluster.contains("contentDescription = \"미래 우체통\""))
        assertTrue(cluster.contains("onClick = onNavigateToFutureMailbox"))

        // 특별한 갤러리 3종 — 대표 아이콘으로 한 번 더 감싸지 않고 개별 진입
        assertTrue(cluster.contains("PondDrawerIcon"))
        assertTrue(cluster.contains("SheepDrawerIcon"))
        assertTrue(cluster.contains("CheckFlagDrawerIcon"))
        assertTrue(cluster.contains("onPlayModeSelected(GalleryPlayMode.POND)"))
        assertTrue(cluster.contains("onPlayModeSelected(GalleryPlayMode.SHEEP_RANCH)"))
        assertTrue(cluster.contains("onPlayModeSelected(GalleryPlayMode.RACE)"))
    }

    @Test
    fun fabCluster_hasNoEmojiOrTextLabelDecoration() {
        val cluster = section(
            "private fun GalleryFabCluster(",
            "private fun BoxScope.GalleryFabShortcut("
        )

        listOf("📷", "⏳", "✨", "💌").forEach { emoji ->
            assertFalse("클러스터에 emoji가 포함됨: $emoji", cluster.contains(emoji))
        }

        // 안내 카드/tooltip성 텍스트 없이 아이콘 + contentDescription만 사용한다.
        assertFalse(cluster.contains("Text("))
    }
}
