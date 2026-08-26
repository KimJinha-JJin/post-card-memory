package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 53일차 제7단계: 스티커 탭의 사진/텍스트/라벨 subcategory navigation을
 * 스크롤 콘텐츠 안 pill형 EditorSegmentedTabRow에서, 스크롤 밖 고정 영역의
 * 평평한 EditorSubcategoryNavBar(EditorBottomTabBar.kt)로 옮겼다. 54일차부터는
 * 마스킹테이프 탭의 기본 디자인/커스텀/사진 생성 방식 선택도 같은 역할이라
 * 같은 컴포저블을 재사용한다 — 두 호출부 모두 각자의 페이지 조건 안에서만
 * 렌더돼야 한다.
 * Compose UI 테스트 인프라가 없는 프로젝트 관례([[StickerEditModeToolbarStructureTest]]
 * 참고)에 따라 소스 텍스트 기준으로 다음을 고정한다.
 */
class EditorSubcategoryNavBarStructureTest {

    private fun readSource(candidates: List<String>): String {
        val file = candidates
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error(
                "소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                    "candidates=$candidates"
            )
        return file.readText()
    }

    private val detailScreenText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
            )
        )
    }

    private val bottomTabBarText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/EditorBottomTabBar.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/EditorBottomTabBar.kt"
            )
        )
    }

    @Test
    fun detailScreen_noLongerUsesInlineSegmentedTabRowForSubcategorySelection() {
        assertFalse(
            "subcategory 선택은 더 이상 스크롤 콘텐츠 안 EditorSegmentedTabRow를 쓰면 안 됨",
            detailScreenText.contains("EditorSegmentedTabRow")
        )
    }

    @Test
    fun detailScreen_callsEditorSubcategoryNavBarForBothStickerAndMaskingTape() {
        assertEquals(
            "EditorSubcategoryNavBar 호출은 DetailScreen.kt에 정확히 2곳(스티커, 마스킹테이프)이어야 함",
            2,
            Regex("""EditorSubcategoryNavBar\(""")
                .findAll(detailScreenText)
                .count()
        )

        val stickerCallIndex = detailScreenText.indexOf("EditorSubcategoryNavBar(")
        val beforeSticker = detailScreenText.substring(0, stickerCallIndex)
        assertTrue(
            "첫 번째 EditorSubcategoryNavBar 호출은 STICKER_TAB_PAGE_INDEX 조건 안에 있어야 함",
            beforeSticker.trimEnd().endsWith(
                "if (customizationPagerState.currentPage == STICKER_TAB_PAGE_INDEX) {"
            )
        )

        val maskingTapeCallIndex =
            detailScreenText.indexOf("EditorSubcategoryNavBar(", stickerCallIndex + 1)
        assertTrue(
            "두 번째 EditorSubcategoryNavBar 호출을 찾지 못함",
            maskingTapeCallIndex > stickerCallIndex
        )
        val beforeMaskingTape = detailScreenText.substring(0, maskingTapeCallIndex)
        assertTrue(
            "두 번째 EditorSubcategoryNavBar 호출은 MASKING_TAPE_TAB_PAGE_INDEX 조건 안에 있어야 함",
            beforeMaskingTape.trimEnd().endsWith(
                "} else if (customizationPagerState.currentPage == MASKING_TAPE_TAB_PAGE_INDEX) {"
            )
        )

        val afterMaskingTape = detailScreenText.substring(maskingTapeCallIndex)
        assertTrue(
            "마지막 EditorSubcategoryNavBar 호출 직후 EditorBottomTabBar 호출이 이어져야 함(같은 고정 영역)",
            afterMaskingTape.substringBefore("EditorBottomTabBar(").length < 600
        )
    }

    @Test
    fun editorSubcategoryNavBar_usesFlatSolidFillWithoutPerItemRoundedShape() {
        val declarationStart =
            bottomTabBarText.indexOf("internal fun EditorSubcategoryNavBar(")
        assertTrue(
            "EditorSubcategoryNavBar 선언을 찾지 못함",
            declarationStart >= 0
        )
        val body = bottomTabBarText.substring(declarationStart)

        assertFalse(
            "항목별로 RoundedCornerShape를 적용하면 안 됨(둥근 선택 Box 제거가 이번 파일럿의 핵심)",
            body.contains("RoundedCornerShape")
        )
        assertTrue(
            "선택된 칸은 SunsetGold 단색으로 채워야 함",
            body.contains("if (selected) SunsetGold else PaperTray")
        )
        assertFalse(
            "이 컴포저블 안에서 SunsetGold를 alpha 워시로 쓰면 안 됨(단색 채움만 허용)",
            body.contains("SunsetGold.copy(alpha")
        )
    }

    @Test
    fun editorSubcategoryNavBar_keepsMinimumTouchTarget() {
        val declarationStart =
            bottomTabBarText.indexOf("internal fun EditorSubcategoryNavBar(")
        val body = bottomTabBarText.substring(declarationStart)
        assertTrue(
            "각 항목은 최소 44dp 터치 영역을 유지해야 함",
            body.contains("heightIn(min = 44.dp)")
        )
    }
}
