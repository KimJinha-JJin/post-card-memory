package com.postcardmemory.ui.components

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 57일차 저장·데이터 안전성 챕터 제3차: `PostcardCustomColorPicker`는
 * `enabled` 파라미터를 받았지만 실제 pointerInput 제스처(색상판 드래그·색상
 * 계열 바 드래그)를 전혀 막지 않았다 — 저장 중(controlsEnabled=false)에도
 * HSV를 계속 조작해 저장을 계속 트리거할 수 있었다. `updateSaturationAndValue`/
 * `updateHue`가 `rememberUpdatedState`로 감싼 최신 `enabled` 값을 확인해
 * 조작을 실제로 막는지 소스 텍스트 기준으로 고정한다.
 */
class PostcardCustomColorPickerEnabledStructureTest {

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

    private val pickerBody: String by lazy {
        val text = readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/components/PostcardBackgroundPicker.kt",
                "app/src/main/java/com/postcardmemory/ui/components/PostcardBackgroundPicker.kt"
            )
        )
        val start = text.indexOf("fun PostcardCustomColorPicker(")
        assertTrue(
            "PostcardCustomColorPicker 선언을 찾지 못함",
            start >= 0
        )
        text.substring(start)
    }

    @Test
    fun tracksLatestEnabledViaRememberUpdatedState() {
        assertTrue(
            "enabled 변경이 이미 실행 중인 pointerInput 제스처 코루틴에도 반영되려면 " +
                "rememberUpdatedState로 최신값을 참조해야 함(DetailScreen.kt의 " +
                "latestControlsEnabled와 동일한 이유)",
            pickerBody.contains("rememberUpdatedState(enabled)")
        )
    }

    @Test
    fun updateSaturationAndValue_returnsEarlyWhenDisabled() {
        val declarationStart =
            pickerBody.indexOf("fun updateSaturationAndValue(")
        assertTrue(
            "updateSaturationAndValue 선언을 찾지 못함",
            declarationStart >= 0
        )
        val nextFunStart =
            pickerBody.indexOf("fun ", declarationStart + 1)
        val body =
            if (nextFunStart > declarationStart) {
                pickerBody.substring(declarationStart, nextFunStart)
            } else {
                pickerBody.substring(declarationStart)
            }
        assertTrue(
            "색상판 드래그/탭이 disabled 상태에서는 saturation/value를 바꾸면 안 됨",
            body.contains("!latestEnabled")
        )
    }

    @Test
    fun updateHue_returnsEarlyWhenDisabled() {
        val declarationStart =
            pickerBody.indexOf("fun updateHue(")
        assertTrue(
            "updateHue 선언을 찾지 못함",
            declarationStart >= 0
        )
        val nextFunStart =
            pickerBody.indexOf("fun ", declarationStart + 1)
        val body =
            if (nextFunStart > declarationStart) {
                pickerBody.substring(declarationStart, nextFunStart)
            } else {
                pickerBody.substring(declarationStart)
            }
        assertTrue(
            "색상 계열 바 드래그/탭이 disabled 상태에서는 hue를 바꾸면 안 됨",
            body.contains("!latestEnabled")
        )
    }
}
