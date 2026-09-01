package com.postcardmemory.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * shouldResyncCustomColorHsv는 기타 색상 선택기에서 "마지막으로 선택한 색"과
 * "명도 조절 기준 HSV"가 갈라지던 버그의 핵심 판단 로직이다. 색이 바뀔 때마다
 * 무조건 HSV를 다시 만들면 명도 조절이 붙잡고 있는 pointerInput 제스처가
 * 오래된 색을 기준으로 계산하게 되므로, 이 컴포저블 자신이 emit한 값과
 * 같은 반향(echo)이면 재동기화하지 않고, 프리셋 스와치처럼 외부에서 바뀐
 * 값일 때만 재동기화해야 한다.
 */
class PostcardCustomColorPickerTest {

    private val colorA = 0xFF2196F3L // 파랑
    private val colorB = 0xFFFFEB3BL // 노랑
    private val colorC = 0xFF4CAF50L // 초록

    // ---- 자기 반향(이 컴포넌트가 emit한 값)이면 재동기화하지 않는다 ----

    @Test
    fun selfEcho_doesNotResync() {
        assertFalse(
            shouldResyncCustomColorHsv(
                externalColorArgb = colorB,
                lastEmittedColorArgb = colorB
            )
        )
    }

    // ---- A 선택 -> B 선택: B가 자기 반향으로 돌아와도 A로 되돌아가지 않는다 ----

    @Test
    fun sequentialSelfEmits_neverResyncBackToEarlierColor() {
        // A를 고른 뒤 A가 반향으로 돌아옴
        assertFalse(
            shouldResyncCustomColorHsv(
                externalColorArgb = colorA,
                lastEmittedColorArgb = colorA
            )
        )

        // 곧이어 B를 고른 뒤 B가 반향으로 돌아옴 (A가 아니라 B와 비교해야 한다)
        assertFalse(
            shouldResyncCustomColorHsv(
                externalColorArgb = colorB,
                lastEmittedColorArgb = colorB
            )
        )
    }

    // ---- 외부(프리셋 스와치 등)에서 값이 바뀌면 재동기화한다 ----

    @Test
    fun externalChange_resyncs() {
        assertTrue(
            shouldResyncCustomColorHsv(
                externalColorArgb = colorC,
                lastEmittedColorArgb = colorB
            )
        )
    }

    // ==== shouldEmitCustomColor: 드래그 중 동일 ARGB 재계산 시 알림 생략 ====

    // ---- 1. 현재색 A -> A: 알림 생략(no-op) ----

    @Test
    fun sameColorAsLastEmitted_doesNotEmit() {
        assertFalse(
            shouldEmitCustomColor(
                newColorArgb = colorA,
                lastEmittedColorArgb = colorA
            )
        )
    }

    // ---- 2. 현재색 A -> B: 정상 알림 ----

    @Test
    fun differentColorFromLastEmitted_emits() {
        assertTrue(
            shouldEmitCustomColor(
                newColorArgb = colorB,
                lastEmittedColorArgb = colorA
            )
        )
    }

    // ---- 3. A -> A -> A 반복: 매번 알림 생략 ----

    @Test
    fun repeatedSameColor_neverEmits() {
        repeat(3) {
            assertFalse(
                shouldEmitCustomColor(
                    newColorArgb = colorA,
                    lastEmittedColorArgb = colorA
                )
            )
        }
    }

    // ---- 4. B로 실제 변경 후에는 다시 정상 알림 ----

    @Test
    fun actualChangeAfterRepeatedSameColor_emits() {
        // A가 반복되는 동안은 생략되다가
        assertFalse(
            shouldEmitCustomColor(
                newColorArgb = colorA,
                lastEmittedColorArgb = colorA
            )
        )

        // 실제로 B로 바뀌면 다시 알림이 나가야 한다
        assertTrue(
            shouldEmitCustomColor(
                newColorArgb = colorB,
                lastEmittedColorArgb = colorA
            )
        )
    }
}
