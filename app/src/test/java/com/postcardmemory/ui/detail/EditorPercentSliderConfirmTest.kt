package com.postcardmemory.ui.detail

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 슬라이더를 놓을 때 저장할 값을 고르는 규칙을 검증한다. 판정 함수는 production 코드를
 * 직접 부르고, 슬라이더 연결은 소스 구조로 고정한다.
 *
 * 한계: Compose UI 테스트가 없어 실제 터치와 Material3의 콜백 순서는 실행하지 않는다.
 * "값이 그대로면 onValueChange 없이 onValueChangeFinished만 불린다"는 전제는 Material3
 * 1.4.0 소스(sliderTapModifier, dispatchRawDelta)를 읽어 확인한 내용이다.
 */
class EditorPercentSliderConfirmTest {

    @Test
    fun tapWithoutValueChange_afterUndo_confirmsCurrentValue_notOldDragValue() {
        // 120%로 드래그 → 되돌리기로 100% → 썸 근처 탭(값 변화 없음)
        assertEquals(
            100,
            resolveConfirmedPercent(
                valueChanged = false,
                draggingPercent = 120,
                currentPercent = 100
            )
        )
    }

    @Test
    fun dragThatChangedValue_confirmsDraggedValue() {
        assertEquals(
            125,
            resolveConfirmedPercent(
                valueChanged = true,
                draggingPercent = 125,
                currentPercent = 100
            )
        )
    }

    @Test
    fun dragReturningToStartingValue_confirmsThatValue() {
        assertEquals(
            100,
            resolveConfirmedPercent(
                valueChanged = true,
                draggingPercent = 100,
                currentPercent = 120
            )
        )
    }

    @Test
    fun structure_finishCallback_usesResolver_notRawDragValue() {
        val source = readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/EditorPercentSlider.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/EditorPercentSlider.kt"
            )
        )
        val callback = source.substringAfter("onValueChangeFinished = {").substringBefore("valueRange =")

        assertFalse(callback.contains("onPercentConfirmed(draggingPercent)"))
        assertTrue(callback.contains("resolveConfirmedPercent("))
        assertTrue(callback.contains("valueChanged = isDragging"))
        assertTrue(callback.contains("currentPercent = percent"))
        assertTrue(
            "판정은 isDragging을 내리기 전에 해야 함",
            callback.indexOf("resolveConfirmedPercent(") < callback.indexOf("isDragging = false")
        )
    }
}
