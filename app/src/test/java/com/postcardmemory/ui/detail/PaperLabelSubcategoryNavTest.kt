package com.postcardmemory.ui.detail

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 101일차: 하단 세부 선택줄([EditorSubcategoryNavBar], 다섯 탭 공통)의 종이 라벨
 * 표현. 종이 띠는 칸(터치 영역) 안에 그림으로만 놓인다. 선택은 띠 색(월 라벨과 같은 바랜
 * 코랄)·굵은 글자·띠 위에 앞발을 걸친 빠끔 얼굴(탭하면 새 칸으로 건너감)로 나타내고, 라벨 둘레 펜 고리는 그리지
 * 않는다(실기기 QA에서 제거). 띠 색값·얼굴 위치 같은 미감 값 자체는 고정하지 않는다.
 */
class PaperLabelSubcategoryNavTest {

    private val paperLabelText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/PaperLabelSubcategoryNav.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/PaperLabelSubcategoryNav.kt"
            )
        )
    }

    @Test
    fun selectionIsShownByStripColorWithoutPenLoop() {
        assertTrue(
            "선택된 라벨은 월 라벨과 같은 코랄 필터, 미선택은 별도 연회색 필터로 칠해야 함",
            paperLabelText.contains("if (selected) GALLERY_DATE_STRIP_CORAL_FILTER else PAPER_LABEL_STRIP_IDLE_FILTER")
        )
        assertFalse(
            "라벨 둘레 펜 고리는 그리지 않음(실기기 QA에서 혼란스럽다고 제거)",
            paperLabelText.contains("drawPath(")
        )
    }

    @Test
    fun stripStaysInsideCellAndLowerThanTouchArea() {
        val sizes = listOf(80f to 44f, 110f to 44f, 140f to 52f)
        for ((width, height) in sizes) {
            val (labelLeft, labelTop, labelRight, labelBottom) = paperLabelBounds(width, height).toList()
            assertTrue(
                "종이 띠는 칸 안에 있어야 함",
                labelLeft >= 0f && labelTop >= 0f && labelRight <= width && labelBottom <= height
            )
            assertTrue("종이 띠는 칸 높이보다 낮게 보여야 함", labelBottom - labelTop < height)
        }
    }

    @Test
    fun peekSitsOnStripTopEdge_withinLabelWidthAndSpaceAboveRow() {
        // 선택줄 위 여백: DetailScreen의 Spacer(6dp) + 고정 영역 위쪽 padding(4dp).
        val spaceAboveRow = 10f
        val sizes = listOf(80f to 44f, 110f to 44f, 140f to 52f)
        for ((width, height) in sizes) {
            val (labelLeft, labelTop, labelRight, _) = paperLabelBounds(width, height).toList()
            val (peekLeft, peekTop, peekRight, peekBottom) = paperLabelPeekBounds(width, height).toList()
            assertTrue(
                "앞발 끝은 띠 위쪽 가장자리를 조금만 걸쳐야 함(글자 영역 침범 금지)",
                peekBottom > labelTop && peekBottom <= labelTop + 4f
            )
            assertTrue("얼굴은 띠 폭 안에 있어야 함", peekLeft >= labelLeft && peekRight <= labelRight)
            assertTrue("앉아 있는 얼굴은 선택줄 위 여백을 넘어가면 안 됨", peekTop >= -spaceAboveRow)
        }
    }

    @Test
    fun peekIsDrawnOncePerRow_andStartsSeatedOnSelectedCell() {
        assertTrue(
            "빠끔 얼굴은 칸마다가 아니라 줄 위에 하나만 그려야 함(건너가기)",
            paperLabelText.contains(".paperLabelPeek(") && !paperLabelText.contains("peek = peek, index")
        )
        assertTrue(
            "얼굴 위치는 선택된 칸에서 시작해 처음 나타날 때 움직이지 않아야 함",
            paperLabelText.contains("Animatable(selectedIndex.toFloat())")
        )
    }

    @Test
    fun peekHop_landsFlatOnBothEnds_andLeansTowardTravelDirection() {
        assertEquals("출발할 때는 띠 가장자리에 앉아 있음", 0f, paperLabelPeekHopLiftDp(0f), 0.001f)
        assertEquals("도착하면 띠 가장자리에 다시 앉음", 0f, paperLabelPeekHopLiftDp(1f), 0.001f)
        val peak = paperLabelPeekHopLiftDp(0.5f)
        assertTrue("가운데에서 떠올라야 함", peak > 0f)
        listOf(0.2f, 0.4f, 0.6f, 0.8f).forEach { t ->
            assertTrue("가운데가 가장 높아야 함", paperLabelPeekHopLiftDp(t) <= peak)
        }

        assertEquals("출발 시 기울지 않음", 0f, paperLabelPeekHopTiltDeg(0f, 1f), 0.001f)
        assertEquals("도착 시 똑바로 섬", 0f, paperLabelPeekHopTiltDeg(1f, -1f), 0.001f)
        assertTrue("오른쪽으로 갈 때 오른쪽으로 기움", paperLabelPeekHopTiltDeg(0.5f, 1f) > 0f)
        assertTrue("왼쪽으로 갈 때 왼쪽으로 기움", paperLabelPeekHopTiltDeg(0.5f, -1f) < 0f)
    }
}
