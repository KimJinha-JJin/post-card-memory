package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 62일차 2차: 월별 보기가 단일 스크롤 컨테이너(`LazyVerticalGrid` +
 * `GridItemSpan`으로 전체 폭 헤더 삽입) 구조를 유지하고, 월 섹션마다
 * 둥근 카드/배경을 다시 씌우지 않는지(작업지시서 22절 — 상자덮기식 UI
 * 금지) 고정한다. Compose UI 테스트 인프라가 없는 프로젝트 관례
 * (DialogPreviewFlatContainerStructureTest.kt 참고)에 따라 소스 텍스트
 * 기준으로 검사한다.
 */
class GalleryMonthlyGridStructureTest {

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

    private val monthlyGridPageBody: String by lazy {
        val start = sourceText.indexOf("private fun GalleryMonthlyGridPage(")
        assertTrue("GalleryMonthlyGridPage 선언을 찾지 못함", start >= 0)

        val nextFunctionStart = sourceText.indexOf(
            "private fun GalleryMonthlyGridItem(",
            start
        )
        assertTrue("GalleryMonthlyGridItem 선언을 찾지 못함", nextFunctionStart > start)

        sourceText.substring(start, nextFunctionStart)
    }

    @Test
    fun monthlyGridPage_usesSingleLazyVerticalGridWithFullWidthHeaderSpan() {
        assertTrue(
            "월별 보기는 LazyVerticalGrid 하나로 헤더와 썸네일을 함께 그려야 함",
            monthlyGridPageBody.contains("LazyVerticalGrid(")
        )
        assertTrue(
            "월 헤더는 GridItemSpan(maxLineSpan)으로 전체 폭을 차지해야 함",
            monthlyGridPageBody.contains("span = { GridItemSpan(maxLineSpan) }")
        )
        assertFalse(
            "LazyColumn을 중첩하면 안 됨(스크롤 컨테이너는 하나여야 함)",
            monthlyGridPageBody.contains("LazyColumn")
        )
        assertTrue(
            "기존 GalleryMonthHeader를 그대로 재사용해야 함(새 헤더 컴포넌트 금지)",
            monthlyGridPageBody.contains("GalleryMonthHeader(")
        )
    }

    @Test
    fun monthlyGridPage_hasNoDecorativeCardWrappingPerMonthSection() {
        // 그리드 컨테이너 자신의 배경(.background(GalleryPaperWhite)) 하나만
        // 있어야 하고, 월 섹션마다 카드처럼 다시 감싸는 배경이 없어야 한다.
        assertEquals(
            "GalleryMonthlyGridPage 안에는 grid 컨테이너 자체의 배경 1개만 있어야 함",
            1,
            Regex("""\.background\(""").findAll(monthlyGridPageBody).count()
        )
        assertFalse(
            "월 섹션을 감싸는 Card를 새로 만들면 안 됨",
            monthlyGridPageBody.contains("Card(")
        )
        assertFalse(
            "월 섹션을 감싸는 RoundedCornerShape 배경을 새로 만들면 안 됨",
            monthlyGridPageBody.contains("RoundedCornerShape")
        )
    }

    @Test
    fun monthlyGridItem_usesLightContentNormallyAndHeavyStampCardOnlyInPondMode() {
        val itemStart = sourceText.indexOf("private fun GalleryMonthlyGridItem(")
        assertTrue("GalleryMonthlyGridItem 선언을 찾지 못함", itemStart >= 0)

        val itemEnd = sourceText.indexOf("\n@Composable", itemStart)
        assertTrue("GalleryMonthlyGridItem 끝을 찾지 못함", itemEnd > itemStart)

        val itemBody = sourceText.substring(itemStart, itemEnd)

        // 76일차: 3단 보기가 삭제되며 그 페이지 전용이던 연못 모드가 이
        // grid로 이식됐다 — 평상시엔 가벼운 StampCardContent를 그대로 쓰고,
        // 연못 모드가 켜졌을 때만 물리 연출이 붙은 StampCard로 바꿔 그린다.
        assertTrue(
            "평상시(연못 모드 꺼짐)에는 가벼운 StampCardContent를 그대로 써야 함",
            itemBody.contains("StampCardContent(")
        )
        assertTrue(
            "연못 모드가 켜졌을 때만 물리 연출이 붙은 StampCard를 써야 함",
            itemBody.contains("StampCard(")
        )
        assertTrue(
            "두 렌더링은 isPondModeOn 분기로 갈려야 함",
            itemBody.contains("if (isPondModeOn)")
        )
        assertTrue(
            "날짜 표기는 월 헤더와 중복되지 않게 두 분기 모두 day-only override를 넘겨야 함",
            itemBody.contains("dateLabelOverride = dayLabel")
        )
    }

    @Test
    fun monthlyGridPage_reusesPondRippleOverlayMigratedFromThreeColumnPage() {
        val start = sourceText.indexOf("private fun GalleryMonthlyGridPage(")
        assertTrue("GalleryMonthlyGridPage 선언을 찾지 못함", start >= 0)

        val end = sourceText.indexOf("private fun GalleryMonthlyGridItem(", start)
        assertTrue("GalleryMonthlyGridItem 선언을 찾지 못함", end > start)

        val body = sourceText.substring(start, end)

        // 76일차: 3단 보기가 삭제되며 그 페이지의 연못 물리 오버레이(탭/드래그
        // 파문)가 월별 보기로 그대로 옮겨졌다.
        assertTrue(body.contains("PondRippleOverlay("))
        assertTrue(body.contains("pondController.gridBoundsInWindow"))
        assertTrue(
            "검색 결과 없음 안내는 3단 보기가 맡던 역할을 이어받아야 함",
            body.contains("SearchEmptyState(")
        )
    }
}
