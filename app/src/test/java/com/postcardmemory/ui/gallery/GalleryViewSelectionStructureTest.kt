package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertEquals
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

        // 주요 3개 + 하위 3개 + anchor 1개. 부모를 더해도 기존 진입을 대체하지 않는다.
        assertEquals(6, Regex("GalleryFabShortcut\\(").findAll(cluster).count())
        assertTrue(cluster.contains("imageVector = Icons.Default.Add"))

        // 엽서 생성 — 앱의 사진 가져오기 메뉴와 동일한 단색 카메라 아이콘
        assertTrue(cluster.contains("Icons.Default.CameraAlt"))
        assertFalse(cluster.contains("R.drawable.ic_camera_button"))
        assertTrue(cluster.contains("contentDescription = \"카메라\""))
        assertTrue(cluster.contains("onClick = { dispatchDragTarget(GalleryFabDragTarget.CAMERA) }"))
        assertTrue(cluster.contains("GalleryFabDragTarget.CAMERA -> currentOnNavigateToCamera()"))

        // 미래 우체통 — 기존 아이콘과 콜백 재사용, 새 destination 없음
        assertTrue(cluster.contains("Icons.Default.MailOutline"))
        assertTrue(cluster.contains("contentDescription = \"미래 우체통\""))
        assertTrue(cluster.contains("onClick = { dispatchDragTarget(GalleryFabDragTarget.FUTURE_MAILBOX) }"))
        assertTrue(cluster.contains("GalleryFabDragTarget.FUTURE_MAILBOX -> currentOnNavigateToFutureMailbox()"))

        // 부모는 하위 표시만 제어하고, 3종의 기존 진입은 유지한다.
        assertTrue(cluster.contains("Icons.Default.PhotoLibrary"))
        assertTrue(cluster.contains("contentDescription = \"특별한 갤러리\""))
        assertTrue(cluster.contains("onClick = { dispatchDragTarget(GalleryFabDragTarget.SPECIAL_GALLERY_TOGGLE) }"))
        assertTrue(cluster.contains("GalleryFabDragTarget.SPECIAL_GALLERY_TOGGLE -> childrenExpanded = !childrenExpanded"))
        assertTrue(cluster.contains("PondDrawerIcon"))
        assertTrue(cluster.contains("SheepDrawerIcon"))
        assertTrue(cluster.contains("CheckFlagDrawerIcon"))
        assertTrue(cluster.contains("GalleryFabDragTarget.POND -> currentOnPlayModeSelected(GalleryPlayMode.POND)"))
        assertTrue(
            cluster.contains(
                "GalleryFabDragTarget.SHEEP_RANCH -> currentOnPlayModeSelected(GalleryPlayMode.SHEEP_RANCH)"
            )
        )
        assertTrue(cluster.contains("GalleryFabDragTarget.RACE -> currentOnPlayModeSelected(GalleryPlayMode.RACE)"))
    }

    @Test
    fun fabCluster_longPressDragReusesSameDispatchAsTap() {
        val cluster = section(
            "private fun GalleryFabCluster(",
            "private fun BoxScope.GalleryFabShortcut("
        )

        // 68일차 추가: 기존 짧은 탭(펼침/접힘)과 새 롱프레스+드래그가 앵커 하나에
        // 각자의 pointerInput으로 공존하고, 드래그 release도 tap과 동일한
        // dispatchDragTarget(...)만 호출해 실행 경로가 갈라지지 않는다.
        assertEquals(2, Regex("\\.pointerInput\\(Unit\\)").findAll(cluster).count())
        assertTrue(cluster.contains("detectTapGestures("))
        assertTrue(cluster.contains("detectDragGesturesAfterLongPress("))
        assertTrue(cluster.contains("currentOnToggle()"))
        assertTrue(cluster.contains("dispatchDragTarget(finalCandidate)"))

        // 모든 6개 shortcut이 drag hit-test 대상으로 등록된다(조준 게임 방지용 slop 포함).
        assertEquals(6, Regex("dragTarget = GalleryFabDragTarget\\.").findAll(cluster).count())
        assertTrue(cluster.contains("bounds.inflate(slopPx)"))

        // 68일차 1차 후속: LocalHapticFeedback.performHapticFeedback()이 실기기에서
        // 느껴지지 않아 Vibrator.vibrate(VibrationEffect)로 교체됨 — 톡/또잉/퐁
        // 세 단계가 서로 다른 duration/amplitude 상수로 구분된다.
        assertFalse(cluster.contains("HapticFeedbackType."))
        assertTrue(cluster.contains("GalleryFabHapticLongPressDurationMs"))
        assertTrue(cluster.contains("GalleryFabHapticSegmentTickDurationMs"))
        assertTrue(cluster.contains("GalleryFabHapticConfirmDurationMs"))
        assertTrue(cluster.contains("longPressPunchTrigger++"))
    }

    @Test
    fun fabCluster_hapticsUseDirectVibratorNotPerformHapticFeedback() {
        // vibrateGalleryFab(...)가 GalleryFabCluster 바깥(파일 상단, private
        // top-level fun)에 정의되므로 cluster 구간이 아니라 전체 소스에서 확인한다.
        assertTrue(sourceText.contains("private fun vibrateGalleryFab("))
        assertTrue(sourceText.contains("VibrationEffect.createOneShot("))
        assertFalse(sourceText.contains("import androidx.compose.ui.platform.LocalHapticFeedback"))
        assertFalse(sourceText.contains("import androidx.compose.ui.hapticfeedback.HapticFeedbackType"))
    }

    @Test
    fun fabShortcut_usesOneShotPunchNotRepeatingSpring() {
        val shortcut = section(
            "private fun BoxScope.GalleryFabShortcut(",
            "private fun GalleryPageFormatMenuItem("
        )

        // 선택 유지 중 지속되는 heldScale과, 진입/탭 순간 한 번만 튕기는
        // punchScale(keyframes 오버슈트→정착)을 곱해서 "통!" 감각을 만든다.
        assertTrue(shortcut.contains("val heldScale by animateFloatAsState("))
        assertTrue(shortcut.contains("val punchScale = remember { Animatable(1f) }"))
        assertTrue(shortcut.contains("scaleX = heldScale * punchScale.value"))
        assertTrue(shortcut.contains("animationSpec = keyframes {"))
        // 반복 bounce를 만드는 spring()은 쓰지 않는다.
        assertFalse(shortcut.contains("spring("))
        assertTrue(shortcut.contains("vibrateGalleryFab("))
    }

    @Test
    fun fabCluster_anchorShortTapAlsoGetsHapticAndLightPunch() {
        val cluster = section(
            "private fun GalleryFabCluster(",
            "private fun BoxScope.GalleryFabShortcut("
        )

        // 68일차 2차 후속: + 짧은 탭도 롱프레스보다 가벼운 별도 상수로
        // 진동을 주고, 같은 anchorPunch를 더 작은 peak/duration으로 재사용한다.
        assertTrue(cluster.contains("tapPunchTrigger++"))
        assertTrue(cluster.contains("GalleryFabHapticAnchorTapDurationMs"))
        assertTrue(cluster.contains("GalleryFabHapticAnchorTapAmplitude"))
    }

    @Test
    fun fabShortcut_hasSelectionRingDistinctFromRipplePulse() {
        val shortcut = section(
            "private fun BoxScope.GalleryFabShortcut(",
            "private fun GalleryPageFormatMenuItem("
        )

        // 68일차 2차 후속: "선택되었다"는 확신을 주는 고정 반경 선택 링(탭이면
        // 짧게, 드래그 후보 유지 중이면 지속)과, 중심에서 퍼지는 물방울 pulse를
        // 색과 동작 모두로 구분한다.
        assertTrue(shortcut.contains("val dragRingAlpha by animateFloatAsState("))
        assertTrue(shortcut.contains("val tapRingAlpha = remember { Animatable(0f) }"))
        assertTrue(shortcut.contains("val ringAlpha = maxOf(dragRingAlpha, tapRingAlpha.value)"))
        assertTrue(shortcut.contains("color = GalleryFabSelectionRingColor"))
        assertTrue(shortcut.contains("color = GalleryFabPulseColor"))
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
