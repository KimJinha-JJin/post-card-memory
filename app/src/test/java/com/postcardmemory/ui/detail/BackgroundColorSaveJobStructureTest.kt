package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HSV drag 중 매 입력마다 새 Room 쓰기가 줄지어 쌓이지 않도록
 * updateBackgroundColor가 이전 저장 Job을 취소한 뒤 최신 Job을 보관하는지
 * 고정한다. 실제 값의 경합 안전성은 BackgroundColorSaveRaceTest가 맡는다.
 */
class BackgroundColorSaveJobStructureTest {

    private val viewModelText: String by lazy {
        val file =
            listOf(
                File("src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"),
                File("app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt")
            ).firstOrNull(File::exists)
                ?: error("DetailViewModel.kt를 찾을 수 없음(cwd=${File(".").absolutePath})")

        file.readText()
    }

    @Test
    fun updateBackgroundColor_cancelsPreviousJobBeforeLaunchingLatest() {
        val functionStart =
            viewModelText.indexOf("fun updateBackgroundColor(")
        assertTrue("updateBackgroundColor를 찾지 못함", functionStart >= 0)

        val functionEnd =
            viewModelText.indexOf(
                "\n    fun extractBackgroundColorsFromPhoto()",
                functionStart
            )
        assertTrue("updateBackgroundColor 끝을 찾지 못함", functionEnd > functionStart)

        val functionBody =
            viewModelText.substring(functionStart, functionEnd)
        val cancelIndex =
            functionBody.indexOf("backgroundColorSaveJob?.cancel()")
        val launchIndex =
            functionBody.indexOf("backgroundColorSaveJob = viewModelScope.launch")
        val latestReadIndex =
            functionBody.indexOf("val latest =")

        assertTrue("이전 배경색 저장 Job을 취소해야 함", cancelIndex >= 0)
        assertTrue("최신 배경색 저장 Job을 필드에 보관해야 함", launchIndex > cancelIndex)
        assertTrue("Mutex 안에서 최신 화면 상태를 다시 읽어야 함", latestReadIndex > launchIndex)
    }
}
