package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 62일차 3차: 타임라인 보기가 단일 스크롤 컨테이너(`LazyColumn`) 구조를
 * 유지하고, 항목마다 카드를 다시 씌우지 않으며(작업지시서 23절), 마지막
 * 항목 아래로는 연결선을 그리지 않는지 고정한다. Compose UI 테스트
 * 인프라가 없는 프로젝트 관례(GalleryMonthlyGridStructureTest.kt 참고)에
 * 따라 소스 텍스트 기준으로 검사한다.
 */
class GalleryTimelineStructureTest {

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

    private fun functionBody(functionName: String, nextFunctionName: String): String {
        val start = sourceText.indexOf("private fun $functionName(")
        assertTrue("$functionName 선언을 찾지 못함", start >= 0)

        val end = sourceText.indexOf("private fun $nextFunctionName(", start)
        assertTrue("$nextFunctionName 선언을 찾지 못함", end > start)

        return sourceText.substring(start, end)
    }

    @Test
    fun timelinePage_usesSingleLazyColumn_noNestedGrid() {
        val body = functionBody("GalleryTimelinePage", "GalleryTimelineEntry")

        assertTrue(
            "타임라인은 LazyColumn 하나로 항목을 그려야 함",
            body.contains("LazyColumn(")
        )
        assertFalse(
            "LazyVerticalGrid를 중첩하면 안 됨(스크롤 컨테이너는 하나여야 함)",
            body.contains("LazyVerticalGrid")
        )
        assertTrue(
            "같은 날짜 여러 postcard는 daySectionsFor로 자연스럽게 묶어야 함",
            body.contains("daySectionsFor(")
        )
    }

    @Test
    fun timelineEntry_hasNoDecorativeCardWrapping_andSkipsLineAfterLastItem() {
        val body = functionBody("GalleryTimelineEntry", "GalleryGrid")

        assertFalse(
            "타임라인 항목을 감싸는 Card를 새로 만들면 안 됨",
            body.contains("Card(")
        )
        assertTrue(
            "마지막 항목 아래로는 연결선을 그리지 않아야 함",
            body.contains("if (!isLast)")
        )
        assertTrue(
            "연결선은 weight(1f)로 dot이 쓰고 남은 높이만 채워야 함(fillMaxHeight는" +
                " Column의 비-weight 자식 예산을 무시해 dot 높이만큼 밖으로 넘침)",
            body.contains(".weight(1f)")
        )
        assertFalse(
            "연결선에 fillMaxHeight를 다시 쓰면 dot 높이만큼 넘치는 버그가 재발함",
            body.contains(".fillMaxHeight()")
        )
        assertTrue(
            "사진 렌더링은 다른 보기와 같은 StampCardContent를 공유해야 함",
            body.contains("StampCardContent(")
        )
        assertFalse(
            "연못 모드 물리 연출이 붙은 무거운 StampCard를 재사용하면 안 됨",
            body.contains("StampCard(")
        )
    }

    @Test
    fun daySectionsFor_groupsSameDayPostcardsWithoutNewDbField() {
        val start = sourceText.indexOf("private fun daySectionsFor(")
        assertTrue("daySectionsFor 선언을 찾지 못함", start >= 0)

        val end = sourceText.indexOf("private val searchDateFormatters", start)
        assertTrue("daySectionsFor 끝을 찾지 못함", end > start)

        val body = sourceText.substring(start, end)

        assertTrue(
            "capturedAt에서 계산한 LocalDate로만 묶어야 함(새 DB 필드 없음)",
            body.contains("toLocalDate()")
        )
        assertTrue(
            "그룹은 LinkedHashMap으로 입력 순서를 보존해야 함(monthSectionsFor와 동일 원칙)",
            body.contains("LinkedHashMap<LocalDate")
        )
    }
}
