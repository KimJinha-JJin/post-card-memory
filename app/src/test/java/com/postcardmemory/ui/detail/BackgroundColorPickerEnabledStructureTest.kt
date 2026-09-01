package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 57일차 저장·데이터 안전성 챕터 제3차 실기기 회귀 수정: `updateBackgroundColor()`는
 * HSV 드래그 프레임마다 즉시 호출돼 `backgroundUpdateState`를 Saving→Success로
 * 매우 빠르게 반복 전환한다. `PostcardCustomColorPicker`의 `enabled`에
 * `controlsEnabled`(= backgroundUpdateState 포함)를 그대로 넘기면, 피커
 * 자신의 저장 상태가 자신의 입력을 계속 막았다 풀었다 하는 자기참조
 * 피드백 루프가 생겨 드래그 중 화면이 빠르게 깜빡였다. `backgroundColorPickerEnabled`는
 * 이 루프를 끊기 위해 `backgroundUpdateState` 조건만 뺀 별도 값이어야 한다.
 * 소스 텍스트 기준으로 이 분리가 유지되는지 고정한다.
 */
class BackgroundColorPickerEnabledStructureTest {

    private fun readSource(candidates: List<String>): String {
        val file = candidates
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error(
                "소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                    "candidates=$candidates"
            )
        return file.readText()
    }

    private val detailScreenText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
            )
        )
    }

    @Test
    fun backgroundColorPickerEnabled_excludesBackgroundUpdateState() {
        val declStart =
            detailScreenText.indexOf("val backgroundColorPickerEnabled =")
        assertTrue(
            "backgroundColorPickerEnabled 선언을 찾지 못함",
            declStart >= 0
        )
        val declEnd =
            detailScreenText.indexOf("\n\n", declStart)
        assertTrue(
            "backgroundColorPickerEnabled 선언 끝을 찾지 못함",
            declEnd > declStart
        )
        val declBody =
            detailScreenText.substring(declStart, declEnd)

        assertFalse(
            "backgroundColorPickerEnabled는 backgroundUpdateState를 조건에 포함하면 안 됨" +
                "(포함하면 HSV 드래그 중 자기참조 피드백 루프로 화면이 깜빡이는 회귀가 재발함)",
            declBody.contains("backgroundUpdateState")
        )
        assertTrue(
            "다른 진짜 차단 상태(export)는 여전히 막아야 함",
            declBody.contains("exportState")
        )
        assertTrue(
            "다른 진짜 차단 상태(확정 저장)는 여전히 막아야 함",
            declBody.contains("confirmSaveState")
        )
    }

    @Test
    fun backgroundColorPicker_callSiteUsesDedicatedEnabledValue() {
        val callIndex =
            detailScreenText.indexOf("PostcardCustomColorPicker(")
        assertTrue(
            "PostcardCustomColorPicker 호출부를 찾지 못함",
            callIndex >= 0
        )
        val callArgsEnd =
            detailScreenText.indexOf("onColorSelected", callIndex)
        assertTrue(
            "PostcardCustomColorPicker 호출부의 onColorSelected를 찾지 못함",
            callArgsEnd > callIndex
        )
        val callArgs =
            detailScreenText.substring(callIndex, callArgsEnd)

        assertTrue(
            "배경색 다이얼로그의 PostcardCustomColorPicker는 backgroundColorPickerEnabled를 써야 함",
            callArgs.contains("enabled = backgroundColorPickerEnabled")
        )
        assertFalse(
            "controlsEnabled(backgroundUpdateState 포함)를 다시 직접 넘기면 안 됨",
            Regex("""enabled\s*=\s*controlsEnabled\s*,""").containsMatchIn(callArgs)
        )
    }
}
