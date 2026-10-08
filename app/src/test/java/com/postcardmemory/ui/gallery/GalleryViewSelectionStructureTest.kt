package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 갤러리의 보기 전환과 기능 진입 구조를 고정한다.
 *  - 좌측 패널(`GalleryFeatureDrawer`)은 없고, 그 진입 기능(미래 우체통, 특별한
 *    갤러리 3종)은 우측 하단 + 클러스터([GalleryFabCluster])가 맡는다.
 *  - 체크박스 기반 "보기 형식 관리" selector 없이, 월별/기억 밀도 2페이지가
 *    항상 활성화된 채 pager 좌우 스와이프로 전환된다.
 *  - + 클러스터는 신문 오림 손 다섯 장의 부채([GalleryQuickSelectHands])로
 *    그려지지만 기존 callback·노출 조건·접근성 이름은 그대로 유지된다.
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
    fun topBar_hasNoViewFormatSelectorMenu() {
        val topBar = section("topBar = {", ") { paddingValues ->")

        // 76일차: 체크박스 기반 "보기 형식 관리" 메뉴 자체가 사라졌다 —
        // 남은 두 보기는 항상 함께 활성화되어 있어 고를 필요가 없다.
        assertFalse(topBar.contains("contentDescription = \"보기 형식 관리\""))
        assertFalse(topBar.contains("GalleryPageFormat.entries.forEach"))
        assertFalse(topBar.contains("toggleActivePageFormat"))
        assertFalse(topBar.contains("viewMenuExpanded"))
        assertFalse(topBar.contains("3열 그리드 보기"))
        assertFalse(topBar.contains("세부 기록 보기"))
        assertFalse(topBar.contains("기능 메뉴 열기"))
    }

    @Test
    fun monthlyAndDensity_areAlwaysBothActiveWithNoToggleState() {
        // 76일차: activePageFormats는 더 이상 rememberSaveable Set이 아니라
        // 월별·기억 밀도 2개로 고정된 값이다.
        assertTrue(
            sourceText.contains(
                "setOf(GalleryPageFormat.MONTHLY, GalleryPageFormat.DENSITY)"
            )
        )
        assertFalse(sourceText.contains("ActivePageFormatsSaver"))
        assertFalse(sourceText.contains("fun toggleActivePageFormat"))
    }

    @Test
    fun legacyPageFormat_fallsBackToMonthly() {
        // 76일차: 3단/캘린더/우표/타임라인처럼 삭제된 이름이 저장값으로
        // 남아 있어도 안전 보기인 월별 보기로 되돌아가야 한다.
        val saver = section("private val PageFormatSaver", "private const val SHAKE_THRESHOLD_G")

        assertTrue(saver.contains(".getOrDefault(GalleryPageFormat.MONTHLY)"))
        assertFalse(saver.contains("THREE_COLUMN"))
    }

    @Test
    fun legacyViewMode_hasNoGalleryScreenStateOrBranch() {
        assertFalse(sourceText.contains("ViewModeSaver"))
        assertFalse(sourceText.contains("var viewMode"))
        assertFalse(sourceText.contains("GalleryViewMode."))
    }

    @Test
    fun visitDrawer_doesNotRestoreTheOldFeatureMenu() {
        assertFalse(sourceText.contains("GalleryFeatureDrawer("))
        assertFalse(sourceText.contains("NavigationDrawerItem"))
        assertFalse(sourceText.contains("GalleryPlayModeDrawerItem"))
        assertFalse(sourceText.contains("기능 메뉴 열기"))
        assertTrue(sourceText.contains("VisitCalendarDrawer(visitDrawerState, visitedEpochDays, totalVisitDays)"))
        assertTrue(sourceText.contains("contentDescription = \"방문 달력 열기\""))
    }

    @Test
    fun quickSelect_callSiteKeepsExistingExposureBackAndOutsideTap() {
        // 88일차: 표현만 손 부채로 바뀌고 노출 조건·뒤로가기·바깥 탭·기존 callback은 그대로다.
        assertTrue(sourceText.contains("GalleryQuickSelectHands("))
        assertFalse(sourceText.contains("GalleryFabCluster("))
        assertTrue(sourceText.contains("visible = !selectionMode,"))
        assertTrue(sourceText.contains("BackHandler(enabled = fabMenuExpanded) {"))
        assertTrue(sourceText.contains("visible = fabMenuExpanded,"))
        assertTrue(sourceText.contains("onNavigateToCamera()"))
        assertTrue(sourceText.contains("onNavigateToFutureMailbox()"))
        assertTrue(sourceText.contains("playMode = if (playMode == selectedMode) {"))
    }

    @Test
    fun quickSelect_fiveHandsReuseExistingCallbacks() {
        val hands = section(
            "private fun GalleryQuickSelectHands(",
            "private fun GalleryQuickSelectHand("
        )

        assertTrue(hands.contains("GalleryQuickSelectItem.CAMERA -> currentOnNavigateToCamera()"))
        assertTrue(hands.contains("GalleryQuickSelectItem.FUTURE_MAILBOX -> currentOnNavigateToFutureMailbox()"))
        assertTrue(hands.contains("GalleryQuickSelectItem.POND -> currentOnPlayModeSelected(GalleryPlayMode.POND)"))
        assertTrue(
            hands.contains(
                "GalleryQuickSelectItem.SHEEP_RANCH -> currentOnPlayModeSelected(GalleryPlayMode.SHEEP_RANCH)"
            )
        )
        assertTrue(hands.contains("GalleryQuickSelectItem.RACE -> currentOnPlayModeSelected(GalleryPlayMode.RACE)"))

        // 탭은 단계 guard를 거쳐 한 번만 실행된다.
        assertTrue(hands.contains("val (next, accepted) = phase.onSelect()"))
        assertTrue(hands.contains("dispatch(item)"))
    }

    @Test
    fun quickSelect_usesUserHandAssetsAndKeepsExistingAccessibilityNames() {
        // 사용자가 준비한 신문 오림 손 5장만 쓴다(기존 도장·핀셋·테이프 손 재사용 없음).
        listOf(
            "R.drawable.quick_select_camera_hand",
            "R.drawable.quick_select_letter_hand",
            "R.drawable.quick_select_river_hand",
            "R.drawable.quick_select_sheep_hand",
            "R.drawable.quick_select_checker_hand"
        ).forEach { res -> assertTrue("손 자산 누락: $res", sourceText.contains(res)) }
        assertFalse(sourceText.contains("R.drawable.seal_stamp_hand"))
        assertFalse(sourceText.contains("R.drawable.sticker_tweezer_hand"))
        assertFalse(sourceText.contains("R.drawable.tape_press_hand"))

        listOf("\"카메라\"", "\"미래 우체통\"", "\"엽서의 연못\"", "\"양떼목장\"", "\"엽서 쫑쫑컵\"").forEach { label ->
            assertTrue("접근성 이름 누락: $label", sourceText.contains(label))
        }
        assertTrue(sourceText.contains("\"바로가기 열기\""))
        assertTrue(sourceText.contains("\"바로가기 닫기\""))
    }

    @Test
    fun quickSelect_dropsSpecialGalleryGroupAndLongPressDragAsApproved() {
        // 88일차 사용자 승인: 다섯 손이 한 단계로 펼쳐지며 "특별한 갤러리" 묶음과
        // 68일차 롱프레스 드래그 바로 실행이 빠졌다.
        assertFalse(sourceText.contains("SPECIAL_GALLERY_TOGGLE"))
        assertFalse(sourceText.contains("contentDescription = \"특별한 갤러리\""))
        assertFalse(sourceText.contains("detectDragGesturesAfterLongPress("))
        assertFalse(sourceText.contains("GalleryFabHapticLongPressDurationMs"))
    }

    @Test
    fun quickSelect_hapticsUseDirectVibratorNotPerformHapticFeedback() {
        assertTrue(sourceText.contains("private fun vibrateGalleryFab("))
        assertTrue(sourceText.contains("VibrationEffect.createOneShot("))
        assertFalse(sourceText.contains("import androidx.compose.ui.platform.LocalHapticFeedback"))
        assertFalse(sourceText.contains("import androidx.compose.ui.hapticfeedback.HapticFeedbackType"))

        val hands = section(
            "private fun GalleryQuickSelectHands(",
            "private fun GalleryQuickSelectHand("
        )
        // 손잡이 탭은 가장 가벼운 톡, 기능 선택은 확정 톡.
        assertTrue(hands.contains("GalleryFabHapticAnchorTapDurationMs"))
        assertTrue(hands.contains("GalleryFabHapticConfirmDurationMs"))
    }

    @Test
    fun quickSelectHand_selectionIsGuardedAndHasNoBounceOrText() {
        val hand = section(
            "private fun GalleryQuickSelectHand(",
            "/** 보기 형식 dot indicator"
        )

        assertTrue(hand.contains("enabled = phase.acceptsSelection"))
        assertTrue(hand.contains("if (phase != GalleryQuickSelectPhase.CLOSED) {"))
        // 연출 값은 graphicsLayer 안에서만 읽는다.
        assertTrue(hand.contains(".graphicsLayer {"))
        assertTrue(hand.contains("filterQuality = FilterQuality.Medium"))
        assertFalse(hand.contains("spring("))
        assertFalse(hand.contains("Text("))
        listOf("📷", "⏳", "✨", "💌").forEach { emoji ->
            assertFalse("손 퀵 셀렉트에 emoji가 포함됨: $emoji", hand.contains(emoji))
        }
    }
}
