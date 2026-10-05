package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 76일차: 기억밀도가 "한 해 동안 어느 달에 기억을 많이 남겼는지 조용히
 * 바라보는" 1월→12월 그래프로 재정의된 구조를 고정한다 — dashboard·통계
 * 카드가 아니어야 한다. 76일차 후속(새싹형)으로 표정 3단계 카오모지는
 * 폐기되고, 고정 얼굴 + 줄기 길이 + 하트로 바뀐 구조를 고정한다.
 * 76일차 후속 실기기 QA 반영: 물뿌리개 아이콘 제거, 달마다 끊기던
 * 구분선을 전체 폭 하나로 통합. 얼굴 색에 하트 진하기를 공유하는 시도는
 * 사용자 요청으로 되돌려 항상 고정된 InkSecondary를 쓴다.
 * 93일차: 얼굴·하트를 걷어내고 중성펜 막대(비례형) + 월 숫자만 남긴 구조를 고정한다.
 */
class GalleryMemoryDensityStructureTest {

    private val sourceText: String by lazy {
        val candidates = listOf(
            "src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt",
            "app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt"
        )
        candidates.map(::File).firstOrNull(File::exists)?.readText()
            ?: error("GalleryScreen.kt를 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    private fun functionBody(functionName: String, nextFunctionName: String): String {
        val start = sourceText.indexOf("private fun $functionName(")
        assertTrue("$functionName 선언을 찾지 못함", start >= 0)
        val end = sourceText.indexOf("private fun $nextFunctionName(", start)
        assertTrue("$nextFunctionName 선언을 찾지 못함", end > start)
        return sourceText.substring(start, end)
    }

    @Test
    fun densityPage_hasNoYearScrollingOrCardDashboard() {
        val body = functionBody("GalleryDensityPage", "GalleryMemoryDensityBar")

        assertTrue(body.contains("memoryDensityMonthsForYear("))
        assertFalse(
            "연도를 이어붙여 스크롤하던 옛 구조(LazyColumn)가 없어야 함",
            body.contains("LazyColumn")
        )
        assertFalse("통계 Card나 graph panel을 추가하면 안 됨", body.contains("Card("))
        assertFalse("새 modal이나 sheet를 추가하면 안 됨", body.contains("Dialog(") || body.contains("ModalBottomSheet("))
        assertFalse(
            "오늘 범위가 아닌 사진 클릭/탭 상세 진입을 추가하면 안 됨",
            body.contains("onItemClick") || body.contains("onItemLongClick")
        )
    }

    @Test
    fun densityPage_hasNoWateringCanIcon() {
        // 76일차 후속 실기기 QA: 물뿌리개 아이콘은 실기기 확인 후 제거됨.
        val body = functionBody("GalleryDensityPage", "GalleryMemoryDensityBar")
        assertFalse("물뿌리개 아이콘은 제거되어야 함", body.contains("WateringCanIcon"))
        assertFalse("연도 라벨 옆에 별도 Icon을 추가하면 안 됨", body.contains("Icon("))
        assertFalse("WateringCanIcon 정의 자체가 남아있으면 안 됨", sourceText.contains("WateringCanIcon"))
    }

    @Test
    fun densityPage_hasExactlyOneFullWidthGroundLine() {
        // 76일차 후속 실기기 QA: 달마다 짧게 끊기던 구분선을 전체 폭 하나로
        // 통합 — GalleryDensityPage에 정확히 하나만 있어야 하고, Bar/Foot
        // 각각의 per-column 구분선은 남아있으면 안 된다.
        val pageBody = functionBody("GalleryDensityPage", "GalleryMemoryDensityBar")
        assertEquals(1, Regex("HorizontalDivider\\(").findAll(pageBody).count())
        assertTrue(pageBody.contains("Modifier.fillMaxWidth(),\n            color = PaperDivider"))

        val barBody = functionBody("GalleryMemoryDensityBar", "GalleryMemoryDensityFoot")
        assertFalse("막대 쪽에 개별 구분선이 남아있으면 안 됨", barBody.contains("HorizontalDivider("))
    }

    @Test
    fun densityFoot_showsOnlyMonthNumberWithoutFace() {
        // 93일차: 하단에는 월 숫자만 남긴다 — 고정 얼굴 "•_•"도 걷어냈다.
        val start = sourceText.indexOf("private fun GalleryMemoryDensityFoot(")
        assertTrue("GalleryMemoryDensityFoot 선언을 찾지 못함", start >= 0)
        val end = sourceText.indexOf("private fun GalleryMonthlyGridPage(", start)
        assertTrue("GalleryMonthlyGridPage 선언을 찾지 못함", end > start)
        val body = sourceText.substring(start, end)

        assertFalse("월 숫자 쪽 구분선이 남아있으면 안 됨(전체 폭 구분선으로 통합됨)", body.contains("HorizontalDivider("))
        assertTrue("월 숫자를 보여줘야 함", body.contains("month.yearMonth.monthValue.toString()"))
        assertEquals("Text는 월 숫자 하나뿐이어야 함", 1, Regex("Text\\(").findAll(body).count())
        assertFalse("고정 얼굴 \"•_•\"은 93일차에 제거됨", body.contains("•_•"))
        assertFalse("표정 3단계 카오모지 로직을 재사용하면 안 됨", body.contains("memoryDensityKaomoji("))
    }

    @Test
    fun densityBar_drawsProportionalGelPenBarWithoutDecorations() {
        val body = functionBody("GalleryMemoryDensityBar", "GalleryMemoryDensityFoot")

        assertTrue("비례형 높이 계산을 써야 함", body.contains("memoryDensityHeightFraction("))
        assertTrue(body.contains("memoryDensityHasOverflow("))
        assertTrue("막대는 중성펜 획으로 그려야 함", body.contains("memoryDensityGelPenBarStrokes("))
        assertTrue("20장 초과 표시도 같은 펜으로 그려야 함", body.contains("memoryDensityGelPenPlusStrokes("))
        assertFalse("막대 위 하트는 93일차에 제거됨", body.contains("♥"))
        assertFalse("하트 진하기 계산은 93일차에 제거됨", sourceText.contains("memoryDensityHeartAlpha("))
        assertFalse("계단식 칸 계산은 비례형으로 대체됨", sourceText.contains("memoryDensityBarLevel("))
        assertFalse("표정 3단계 카오모지 로직을 재사용하면 안 됨", body.contains("memoryDensityKaomoji("))
        assertFalse(
            "옛 원형 점(intensity 기반 크기·투명도) 문법을 재사용하면 안 됨",
            body.contains("memoryDensityIntensity(")
        )
        assertFalse("RoundedCornerShape 카드 패널을 추가하면 안 됨", body.contains("RoundedCornerShape"))
        assertTrue(
            "접근성 설명(월·기억 개수)은 막대 쪽에 남아 있어야 함",
            body.contains("contentDescription = \"\${month.yearMonth.monthValue}월, 기억 \${month.count}개\"")
        )
    }

    @Test
    fun kaomojiFunction_noLongerExistsInSource() {
        assertFalse(
            "76일차 후속(새싹형)에서 표정 3단계 카오모지 함수 자체를 삭제해야 함",
            sourceText.contains("fun memoryDensityKaomoji(")
        )
    }

    @Test
    fun densityPagerBranch_passesOnlyPostcardsAndPadding() {
        val start = sourceText.indexOf("GalleryPageFormat.DENSITY ->")
        assertTrue("DENSITY pager 분기를 찾지 못함", start >= 0)
        val end = sourceText.indexOf("GalleryPageFormat.MONTHLY", start)
        assertTrue("DENSITY pager 분기 끝을 찾지 못함", end > start)
        val body = sourceText.substring(start, end)

        assertTrue(body.contains("GalleryDensityPage("))
        assertTrue(body.contains("postcards = displayedPostcards"))
        assertFalse(
            "76일차부터 기억밀도는 선택 상태나 클릭 콜백을 받지 않아야 함",
            body.contains("selectedIds") || body.contains("onItemClick") || body.contains("onItemLongClick")
        )
    }
}
