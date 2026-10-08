package com.postcardmemory.ui.detail

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 편집 탭의 subcategory 선택은 스크롤 콘텐츠 안 pill형 EditorSegmentedTabRow가
 * 아니라, 스크롤 밖 고정 영역의 평평한 EditorSubcategoryNavBar
 * (EditorBottomTabBar.kt)가 맡는다. 사진(레이아웃/사진 편집), 배경(색상/패턴),
 * 스티커(사진/텍스트/라벨), 마스킹테이프(기본 디자인/커스텀/사진), 낙서
 * (펜/형광펜/점선/지우개 도구) 선택이 같은 역할이라 같은 컴포저블을
 * 재사용한다 — 다섯 호출부 모두 각자의 페이지 조건 안에서만 렌더돼야 한다.
 * `src/test` JVM 환경에는 Robolectric이 없어 Composable을 직접 렌더링할 수
 * 없으므로([[StickerEditModeToolbarStructureTest]] 참고) 소스 텍스트 기준으로
 * 다음을 고정한다.
 */
class EditorSubcategoryNavBarStructureTest {

    private val detailScreenText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
            )
        )
    }

    private val bottomTabBarText: String by lazy {
        readStructureTestSource(
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
    fun detailScreen_callsEditorSubcategoryNavBarForPhotoBackgroundStickerMaskingTapeAndDoodle() {
        assertEquals(
            "EditorSubcategoryNavBar 호출은 DetailScreen.kt에 정확히 5곳(사진, 배경, 스티커, 마스킹테이프, 낙서)이어야 함",
            5,
            Regex("""EditorSubcategoryNavBar\(""")
                .findAll(detailScreenText)
                .count()
        )

        val photoCallIndex = detailScreenText.indexOf("EditorSubcategoryNavBar(")
        val beforePhoto = detailScreenText.substring(0, photoCallIndex)
        assertTrue(
            "첫 번째 EditorSubcategoryNavBar 호출은 PHOTO_TAB_PAGE_INDEX 조건 안에 있어야 함",
            beforePhoto.trimEnd().endsWith(
                "if (customizationPagerState.currentPage == PHOTO_TAB_PAGE_INDEX) {"
            )
        )

        val backgroundCallIndex =
            detailScreenText.indexOf("EditorSubcategoryNavBar(", photoCallIndex + 1)
        assertTrue(
            "두 번째 EditorSubcategoryNavBar 호출을 찾지 못함",
            backgroundCallIndex > photoCallIndex
        )
        val beforeBackground = detailScreenText.substring(0, backgroundCallIndex)
        assertTrue(
            "두 번째 EditorSubcategoryNavBar 호출은 BACKGROUND_TAB_PAGE_INDEX 조건 안에 있어야 함",
            beforeBackground.trimEnd().endsWith(
                "} else if (customizationPagerState.currentPage == BACKGROUND_TAB_PAGE_INDEX) {"
            )
        )

        val stickerCallIndex =
            detailScreenText.indexOf("EditorSubcategoryNavBar(", backgroundCallIndex + 1)
        assertTrue(
            "세 번째 EditorSubcategoryNavBar 호출을 찾지 못함",
            stickerCallIndex > backgroundCallIndex
        )
        val beforeSticker = detailScreenText.substring(0, stickerCallIndex)
        assertTrue(
            "세 번째 EditorSubcategoryNavBar 호출은 STICKER_TAB_PAGE_INDEX 조건 안에 있어야 함",
            beforeSticker.trimEnd().endsWith(
                "} else if (customizationPagerState.currentPage == STICKER_TAB_PAGE_INDEX) {"
            )
        )

        val maskingTapeCallIndex =
            detailScreenText.indexOf("EditorSubcategoryNavBar(", stickerCallIndex + 1)
        assertTrue(
            "네 번째 EditorSubcategoryNavBar 호출을 찾지 못함",
            maskingTapeCallIndex > stickerCallIndex
        )
        val beforeMaskingTape = detailScreenText.substring(0, maskingTapeCallIndex)
        assertTrue(
            "네 번째 EditorSubcategoryNavBar 호출은 MASKING_TAPE_TAB_PAGE_INDEX 조건 안에 있어야 함",
            beforeMaskingTape.trimEnd().endsWith(
                "} else if (customizationPagerState.currentPage == MASKING_TAPE_TAB_PAGE_INDEX) {"
            )
        )

        val doodleCallIndex =
            detailScreenText.indexOf("EditorSubcategoryNavBar(", maskingTapeCallIndex + 1)
        assertTrue(
            "다섯 번째 EditorSubcategoryNavBar 호출을 찾지 못함",
            doodleCallIndex > maskingTapeCallIndex
        )
        val beforeDoodle = detailScreenText.substring(0, doodleCallIndex)
        assertTrue(
            "다섯 번째 EditorSubcategoryNavBar 호출은 DOODLE_TAB_PAGE_INDEX 조건 안에 있어야 함",
            beforeDoodle.trimEnd().endsWith(
                "} else if (customizationPagerState.currentPage == DOODLE_TAB_PAGE_INDEX) {"
            )
        )

        val afterDoodle = detailScreenText.substring(doodleCallIndex)
        assertTrue(
            "마지막 EditorSubcategoryNavBar 호출 직후 EditorBottomTabBar 호출이 이어져야 함(같은 고정 영역)",
            afterDoodle.substringBefore("EditorBottomTabBar(").length < 600
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
