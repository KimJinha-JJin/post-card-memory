package com.postcardmemory.ui.gallery

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 90일차: 방문 달력을 종이 한 장(VisitCalendarMonthPage) 단위로 묶은 구조를 고정한다 —
 * 종이·제목·장식·요일·날짜 grid가 한 장 안에서 함께 움직이고, 종이 bitmap은 월 이동
 * 바깥에서 한 번만 불러오며, "다녀간 날들 / 오늘" 줄과 월/연도 선택 단계 전환은 종이
 * 밖에 그대로 남는다. 질감 농도·윤곽 색 같은 미감 값은 실기기 QA 대상이라 고정하지 않는다.
 * `src/test` JVM에는 Robolectric이 없어 소스 텍스트 기준으로 검사한다.
 */
class VisitCalendarPaperPageStructureTest {

    private val sourceText: String by lazy {
        val candidates = listOf(
            "src/main/java/com/postcardmemory/ui/gallery/VisitCalendarDrawer.kt",
            "app/src/main/java/com/postcardmemory/ui/gallery/VisitCalendarDrawer.kt"
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

    private fun functionBody(declaration: String): String {
        val start = sourceText.indexOf(declaration)
        assertTrue("$declaration 선언을 찾지 못함", start >= 0)
        val end = sourceText.indexOf("\n@Composable", start).let { if (it < 0) sourceText.length else it }
        return sourceText.substring(start, end)
    }

    private val pageBody: String by lazy { functionBody("private fun VisitCalendarMonthPage(") }
    private val calendarBody: String by lazy { functionBody("internal fun MonthlyVisitCalendar(") }

    // 월 이동(달력 한 장 넘김)이 시작되는 지점.
    private val monthTransitionMarker = "updateTransition(displayedMonth"
    private val monthTransition: Int by lazy {
        calendarBody.indexOf(monthTransitionMarker).also {
            assertTrue("월 이동 transition을 찾지 못함", it >= 0)
        }
    }

    @Test
    fun paper_isLoadedOnceOutsideTheMonthTransition() {
        assertEquals(
            "달력 종이 drawable은 한 번만 참조해야 함(장마다 decode 금지)",
            1,
            Regex("""R\.drawable\.visit_calendar_paper""").findAll(sourceText).count()
        )
        val load = calendarBody.indexOf("R.drawable.visit_calendar_paper")
        assertTrue("종이는 MonthlyVisitCalendar 안에서 불러와야 함", load >= 0)
        assertTrue("종이는 월 이동 AnimatedContent보다 먼저(바깥에서) 불러와야 함", load < monthTransition)
        assertFalse("달력 장 안에서 종이를 다시 불러오면 안 됨", pageBody.contains("imageResource"))
    }

    @Test
    fun monthTransition_turnsOneWholePage() {
        assertEquals(
            "월 이동 transition은 하나여야 함(제목·grid가 따로 움직이면 안 됨)",
            1,
            Regex(Regex.escape(monthTransitionMarker)).findAll(sourceText).count()
        )
        assertFalse(
            "displayedMonth로 도는 AnimatedContent가 따로 남아 있으면 안 됨",
            sourceText.contains("targetState = displayedMonth")
        )
        val animatedContent = calendarBody.indexOf("monthTransition.AnimatedContent(", monthTransition)
        val pageCall = calendarBody.indexOf("VisitCalendarMonthPage(", animatedContent)
        assertTrue("월 이동 AnimatedContent가 달력 한 장을 감싸야 함", animatedContent > monthTransition && pageCall > animatedContent)
        assertTrue(
            "넘김 표현은 달력 한 장 전체(modifier)에 걸려야 함",
            calendarBody.substring(pageCall, minOf(calendarBody.length, pageCall + 200))
                .contains("modifier = visitCalendarPageTurnModifier(")
        )
        assertTrue(calendarBody.contains("transitionSpec = { visitCalendarPageTurnTransition() }"))
    }

    @Test
    fun pageTurn_hingesOnTheTopEdgeAndStacksByMonth() {
        val turn = functionBody("private fun AnimatedVisibilityScope.visitCalendarPageTurnModifier(")
        assertTrue("윗변을 축으로 위로 넘겨야 함(벽걸이 달력)", turn.contains("TransformOrigin(0.5f, 0f)"))
        assertTrue("위로 넘김은 rotationX로 표현해야 함", turn.contains("rotationX ="))
        assertFalse("옆으로 넘기거나 뒤집으면 안 됨", turn.contains("rotationY") || turn.contains("rotationZ"))
        val spec = functionBody("private fun AnimatedContentTransitionScope<YearMonth>.visitCalendarPageTurnTransition(")
        assertTrue("겹침 순서는 달력 묶음 순서를 따라야 함",
            spec.contains("targetContentZIndex = visitCalendarPageStackZIndex(targetState)"))
        assertTrue("넘어가는 장은 넘김이 끝날 때까지 남아야 함", spec.contains("ExitTransition.KeepUntilTransitionsFinished"))
        assertTrue("들린 장이 잘리지 않게 clip하지 않아야 함", spec.contains("SizeTransform(clip = false)"))
    }

    @Test
    fun page_containsEverythingPrintedOnThePaper() {
        listOf(
            "ImageShader(paper)",
            "\${month.year}년 \${month.monthValue}월",
            "VisitCalendarTopOrnament()",
            "VISIT_CALENDAR_WEEKDAY_HEADERS",
            "VisitCalendarMonthGrid("
        ).forEach { printed ->
            assertTrue("달력 한 장 안에 $printed 가 있어야 함", pageBody.contains(printed))
        }
        assertFalse(
            "달력 한 장은 자기 month만 읽어야 함(나가는 장이 다음 달로 바뀌지 않게)",
            pageBody.contains("displayedMonth")
        )
    }

    @Test
    fun paper_isCutAlongThisMonthsOwnHandCutEdge() {
        assertTrue(
            "종이는 이 장의 month로 고른 가장자리 모양으로 오려 그려야 함",
            pageBody.contains("seed = visitCalendarPaperEdgeSeed(month)")
        )
        assertTrue("오린 종이는 path로 채워 그려야 함", pageBody.contains("drawPath(outline, brush = paperBrush)"))
        assertFalse("가장자리는 안티에일리어싱 없는 clip이 아니라 채운 path여야 함", pageBody.contains("clipPath("))
    }

    @Test
    fun visitedDay_isMarkedWithTheGelPenInTheUnchangedFillColor() {
        val grid = functionBody("private fun VisitCalendarMonthGrid(")
        assertTrue(
            "방문일은 기존 채움 색 그대로 그 날짜의 중성펜 자국으로 칠해야 함",
            grid.contains(".visitDayGelPenMark(visitDayFillColor(date, today), visitDayPenSeed(date))")
        )
        assertFalse("매끈한 단색 상자로 돌아가면 안 됨", grid.contains(".background("))
    }

    @Test
    fun tape_isStuckOnThisMonthsPageAndTurnsWithIt() {
        assertTrue(
            "테이프는 이 장의 month로 고른 조각이어야 함",
            pageBody.contains("visitCalendarTapePiece(visitCalendarTapeSeed(month))")
        )
        val paper = pageBody.indexOf("drawPath(outline, brush = paperBrush)")
        val tape = pageBody.indexOf("drawVisitCalendarTape(")
        assertTrue("테이프는 장 안에서 종이 위에 붙어야 함", paper >= 0 && tape > paper)
        assertFalse("테이프는 자산 이미지가 아니라 코드 그림이어야 함", sourceText.contains("gallery_date_paper_strip"))
    }

    @Test
    fun paper_isCroppedWithoutStretching() {
        assertTrue("종이는 장을 꽉 채우는 큰 쪽 배율 하나로만 맞춰야 함", pageBody.contains("maxOf("))
        assertFalse("FillBounds로 늘리면 안 됨", pageBody.contains("FillBounds"))
        assertFalse("타일 반복이 아니라 한 장이어야 함", pageBody.contains("TileMode"))
        assertFalse(
            "장 둘레에 윤곽선을 그리면 카드형 박스처럼 보임(90일차 QA에서 제거)",
            pageBody.contains("Stroke(") || pageBody.contains(".border(")
        )
    }

    @Test
    fun visitedDaysLineAndArrowsStayOutsideThePaper() {
        assertFalse("\"다녀간 날들\" 줄은 종이 밖 고정이어야 함", pageBody.contains("다녀간 날들"))
        assertFalse("◀ ▶ 버튼은 종이 밖 고정이어야 함", pageBody.contains("IconButton("))
        val visitedLine = calendarBody.indexOf("\"다녀간 날들\"")
        assertTrue("\"다녀간 날들\" 줄은 달력 한 장 위에 있어야 함", visitedLine in 0 until monthTransition)
        listOf("\"이전 달\"", "\"다음 달\"").forEach { arrow ->
            assertTrue("$arrow 버튼은 월 이동 AnimatedContent 뒤(위 z-order)에 있어야 함",
                calendarBody.indexOf(arrow) > monthTransition)
        }
    }

    @Test
    fun hierarchyTransitionAndDrawerSurfaceAreUnchanged() {
        val hierarchy = functionBody("private fun AnimatedContentTransitionScope<VisitCalendarNavLevel>.visitCalendarHierarchyTransition(")
        assertTrue("단계 전환은 fade + scale 그대로여야 함", hierarchy.contains("fadeIn(") && hierarchy.contains("scaleIn("))
        assertFalse("단계 전환에 가로 슬라이드가 섞이면 안 됨", hierarchy.contains("slideIn"))
        assertTrue(calendarBody.contains("transitionSpec = { visitCalendarHierarchyTransition() }"))

        val drawer = functionBody("internal fun VisitCalendarDrawer(")
        assertTrue("drawer 전체 배경은 종이가 아니라 PaperSurface 그대로여야 함",
            drawer.contains("drawerContainerColor = PaperSurface"))
        assertFalse("종이는 drawer 전체가 아니라 달력 한 장에만 깔려야 함", drawer.contains("visit_calendar_paper"))
    }
}
