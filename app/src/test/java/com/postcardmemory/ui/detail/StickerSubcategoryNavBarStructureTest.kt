package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 53일차 제7단계: 스티커 탭의 사진/텍스트/라벨 subcategory navigation을
 * 스크롤 콘텐츠 안 pill형 EditorSegmentedTabRow에서, 스크롤 밖 고정 영역의
 * 평평한 StickerSubcategoryNavBar(EditorBottomTabBar.kt)로 옮겼다. 항목별
 * rounded Box/카드 없이 칸 자체의 배경색(선택 시 SunsetGold 단색, 미선택
 * 시 PaperTray)만으로 선택 상태를 나타낸다.
 * Compose UI 테스트 인프라가 없는 프로젝트 관례([[StickerEditModeToolbarStructureTest]]
 * 참고)에 따라 소스 텍스트 기준으로 다음을 고정한다.
 */
class StickerSubcategoryNavBarStructureTest {

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
    fun detailScreen_noLongerUsesInlineSegmentedTabRowForStickerSubcategory() {
        assertFalse(
            "스티커 탭의 사진/텍스트/라벨 선택은 더 이상 스크롤 콘텐츠 안 EditorSegmentedTabRow를 쓰면 안 됨",
            detailScreenText.contains("EditorSegmentedTabRow")
        )
    }

    @Test
    fun detailScreen_callsStickerSubcategoryNavBarExactlyOnceInsideFixedArea() {
        assertEquals(
            "StickerSubcategoryNavBar 호출은 DetailScreen.kt에 정확히 1곳이어야 함",
            1,
            Regex("""StickerSubcategoryNavBar\(""")
                .findAll(detailScreenText)
                .count()
        )
        // 호출부 바로 앞에 스티커 탭에서만 보이도록 하는 조건문이 있어야
        // 하고(다른 탭에서는 렌더하지 않음), 그 뒤로 곧 EditorBottomTabBar
        // 호출이 이어져야 한다(같은 고정 Box 안에서 위아래로 쌓임).
        val callIndex = detailScreenText.indexOf("StickerSubcategoryNavBar(")
        val before = detailScreenText.substring(0, callIndex)
        val after = detailScreenText.substring(callIndex)
        assertTrue(
            "StickerSubcategoryNavBar는 STICKER_TAB_PAGE_INDEX 조건 안에서만 렌더돼야 함",
            before.trimEnd().endsWith(
                "if (customizationPagerState.currentPage == STICKER_TAB_PAGE_INDEX) {"
            )
        )
        assertTrue(
            "StickerSubcategoryNavBar 호출 직후 EditorBottomTabBar 호출이 이어져야 함(같은 고정 영역)",
            after.substringBefore("EditorBottomTabBar(").length < 600
        )
    }

    @Test
    fun stickerSubcategoryNavBar_usesFlatSolidFillWithoutPerItemRoundedShape() {
        val declarationStart =
            bottomTabBarText.indexOf("internal fun StickerSubcategoryNavBar(")
        assertTrue(
            "StickerSubcategoryNavBar 선언을 찾지 못함",
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
    fun stickerSubcategoryNavBar_keepsMinimumTouchTarget() {
        val declarationStart =
            bottomTabBarText.indexOf("internal fun StickerSubcategoryNavBar(")
        val body = bottomTabBarText.substring(declarationStart)
        assertTrue(
            "각 항목은 최소 44dp 터치 영역을 유지해야 함",
            body.contains("heightIn(min = 44.dp)")
        )
    }
}
