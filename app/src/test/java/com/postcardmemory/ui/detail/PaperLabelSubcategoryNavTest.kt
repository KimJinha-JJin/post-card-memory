package com.postcardmemory.ui.detail

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 101일차: 하단 세부 선택줄([EditorSubcategoryNavBar], 다섯 탭 공통)의 종이 라벨
 * 표현. 종이 띠는 칸(터치 영역) 안에 그림으로만 놓인다. 선택은 띠 색(월 라벨과 같은 바랜
 * 코랄)·굵은 글자·띠 위에 앞발을 걸친 빠끔 얼굴로 나타내고, 라벨 둘레 펜 고리는 그리지
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
            assertTrue("얼굴은 선택줄 위 여백을 넘어가면 안 됨", peekTop >= -spaceAboveRow)
        }
        assertTrue(
            "얼굴은 선택된 칸에만 그려야 함",
            paperLabelText.substringAfter("if (selected) {").startsWith("\n") &&
                paperLabelText.substringAfter("if (selected) {").substringBefore("}").contains("image = peek")
        )
    }
}
