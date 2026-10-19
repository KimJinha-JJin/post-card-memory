package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 62일차 2차: 월별 보기가 단일 스크롤 컨테이너(`LazyVerticalGrid` +
 * `GridItemSpan`으로 전체 폭 헤더 삽입) 구조를 유지하고, 월 섹션마다
 * 둥근 카드/배경을 다시 씌우지 않는지(상자덮기식 UI 금지) 고정한다.
 * `src/test` JVM 환경에는 Robolectric이 없어 Composable을 직접 렌더링할 수
 * 없으므로(DialogPreviewFlatContainerStructureTest.kt 참고), 소스 텍스트
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
        // 89일차: 종이 배경은 pager 밖 한 레이어(GalleryPaperBackground)로
        // 옮겨져 grid 컨테이너도 배경을 칠하지 않는다. 월 섹션마다 카드처럼
        // 다시 감싸는 배경도 없어야 한다.
        assertEquals(
            "GalleryMonthlyGridPage 안에는 배경이 없어야 함(종이는 pager 밖 한 레이어)",
            0,
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

        // 연못 모드는 이 grid 안에서 그려진다 — 평상시엔 가벼운
        // StampCardContent를 그대로 쓰고, 연못 모드가 켜졌을 때만 물리 연출이
        // 붙은 StampCard로 바꿔 그린다.
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
            "평상시 날짜 표기는 월 헤더와 중복되지 않게 day-only override를 넘겨야 함",
            itemBody.contains("dateLabelOverride = dayLabel")
        )
        // 108일차: 연못은 월 헤더가 없어 "일"만으론 의미가 없으므로 날짜 줄을 빼야 한다.
        assertTrue(
            "연못 분기는 빈 문자열로 날짜 줄을 그리지 않아야 함",
            itemBody.contains("dateLabelOverride = \"\"")
        )
    }

    @Test
    fun pondMode_showsLatestPondPostcardsWithoutMonthHeaders() {
        // 108일차: 연못은 월별 묶음 대신 pondPostcardsFor로 고른 최신 10장을 넘겨받고,
        // 월 헤더(월·장수)는 만들지 않는다. 연못을 끄면 같은 페이지가 월별 묶음으로 돌아온다.
        assertTrue(
            "pager는 연못 목록을 pondPostcardsFor(전체 엽서)로 골라야 함",
            sourceText.contains("pondPostcardsFor(postcards)")
        )
        assertTrue(
            "연못일 때만 연못 목록, 아니면 검색·정렬된 월별 목록을 넘겨야 함",
            sourceText.contains("postcards = if (isPondModeOn) pondPostcards else displayedPostcards")
        )
        assertTrue(
            "연못일 때는 월 섹션(헤더)을 만들지 않아야 함",
            monthlyGridPageBody.contains("if (isPondModeOn) emptyList() else monthSectionsFor(postcards)")
        )
        assertTrue(
            "연못은 별도 스크롤 state를 써서 월별 보기의 스크롤 위치를 건드리지 않아야 함",
            monthlyGridPageBody.contains("state = if (isPondModeOn) pondGridState else gridState")
        )
    }

    @Test
    fun pondLeaf_isDecodedOncePerPageAndDrawnInsideTheCardLayer() {
        // 108일차: 연잎은 카드 10장이 공유하는 한 장 — 페이지에서 연못이 켜진 동안만 한 번 불러온다.
        assertTrue(
            monthlyGridPageBody.contains(
                "val pondLeaf = if (isPondModeOn) ImageBitmap.imageResource(R.drawable.pond_lily_pad) else null"
            )
        )
        assertTrue(monthlyGridPageBody.contains("pondLeaf = pondLeaf"))

        // 연잎은 StampCard의 graphicsLayer 뒤(안쪽)·터치 처리 앞에서 그려야 우표와 한 몸으로
        // 움직이고 기존 터치 영역도 그대로다.
        val stampCard = listOf(
            "src/main/java/com/postcardmemory/ui/components/StampCard.kt",
            "app/src/main/java/com/postcardmemory/ui/components/StampCard.kt"
        ).map { File(it) }.first { it.exists() }.readText()
        val layer = stampCard.indexOf(".graphicsLayer {")
        val leaf = stampCard.indexOf("drawImage(\n                                image = pondLeaf")
        val pointer = stampCard.indexOf("Modifier.pointerInput(postcard.id, pondController)")
        assertTrue("graphicsLayer를 찾지 못함", layer >= 0)
        assertTrue("연잎 drawImage를 찾지 못함", leaf >= 0)
        assertTrue("연잎은 graphicsLayer 안쪽에서 그려야 함", leaf > layer)
        assertTrue("연잎은 터치 처리 앞(바깥 받침 자리)에서 그려야 함", pointer > leaf)
    }

    @Test
    fun enteringPond_closesSearch() {
        // 108일차: 연못은 검색과 무관한 최신 엽서를 띄우므로, 연못을 켜면 검색을 닫는다.
        val start = sourceText.indexOf("onPlayModeSelected = { selectedMode ->")
        assertTrue("onPlayModeSelected 콜백을 찾지 못함", start >= 0)
        val end = sourceText.indexOf("modifier = Modifier.navigationBarsPadding()", start)
        assertTrue("onPlayModeSelected 콜백 끝을 찾지 못함", end > start)
        val body = sourceText.substring(start, end)

        assertTrue(body.contains("if (playMode == GalleryPlayMode.POND)"))
        assertTrue(body.contains("isSearchActive = false"))
        assertTrue(body.contains("searchQuery = \"\""))
    }

    @Test
    fun monthlyGridPage_reusesPondRippleOverlayMigratedFromThreeColumnPage() {
        val start = sourceText.indexOf("private fun GalleryMonthlyGridPage(")
        assertTrue("GalleryMonthlyGridPage 선언을 찾지 못함", start >= 0)

        val end = sourceText.indexOf("private fun GalleryMonthlyGridItem(", start)
        assertTrue("GalleryMonthlyGridItem 선언을 찾지 못함", end > start)

        val body = sourceText.substring(start, end)

        // 연못 물리 오버레이(탭/드래그 파문)와 검색 결과 없음 안내는 월별
        // 보기가 맡는다.
        assertTrue(body.contains("PondRippleOverlay("))
        assertTrue(body.contains("pondController.gridBoundsInWindow"))
        assertTrue(
            "검색 결과 없음 안내는 3단 보기가 맡던 역할을 이어받아야 함",
            body.contains("SearchEmptyState(")
        )
    }
}
