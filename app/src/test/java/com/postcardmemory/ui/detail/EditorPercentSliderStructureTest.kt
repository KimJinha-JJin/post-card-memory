package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 제5차(2026-08-07)에서 `EditorPercentSlider`를 DetailScreen.kt에서
 * EditorPercentSlider.kt로 물리적으로 분리했다. 이 프로젝트는 Compose UI
 * 테스트 인프라를 쓰지 않으므로([[SaveErrorDialogStructureTest]] 상단 주석
 * 참고), 소스 텍스트 기준으로 다음을 고정한다:
 *  - 분리된 파일에만 함수 정의가 존재하고(DetailScreen.kt에는 남지 않음)
 *    ViewModel/Repository/Context를 참조하지 않음
 *  - DetailScreen.kt의 5개 호출부가 기존과 동일한 파라미터 구조로 연결됨
 *
 * 제3차에서 "AlertDialog(" 부분 문자열이 "SaveResultAlertDialog(" 안에도
 * 걸려 개수를 잘못 세었던 오탐을 반복하지 않기 위해, 함수 선언 검사는
 * 줄 시작 앵커(`^internal fun EditorPercentSlider\(`)로 제한한다. 호출부
 * 경계는 들여쓰기 공백 수에 기대지 않고, 여는 괄호부터 괄호 깊이가 0으로
 * 돌아오는 지점까지 직접 스캔해 잘라낸다 — 5개 호출부마다 콜백 본문
 * 길이가 달라 고정 앵커 문자열을 쓸 수 없기 때문이다.
 */
class EditorPercentSliderStructureTest {

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

    private val componentText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/EditorPercentSlider.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/EditorPercentSlider.kt"
            )
        )
    }

    private val detailScreenText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
            )
        )
    }

    /** 호출 시작 위치(식별자 첫 글자)부터 여는 괄호와 짝이 맞는 닫는 괄호까지 잘라낸다. */
    private fun extractBalancedCall(source: String, callStart: Int): String {
        val openParenIndex = source.indexOf('(', callStart)
        check(openParenIndex >= 0) { "여는 괄호를 찾지 못함: index=$callStart" }

        var depth = 0
        var i = openParenIndex
        while (i < source.length) {
            when (source[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return source.substring(callStart, i + 1)
                }
            }
            i++
        }
        error("괄호 짝이 맞지 않음: index=$callStart")
    }

    @Test
    fun componentFile_declaresEditorPercentSliderExactlyOnce() {
        assertEquals(
            "EditorPercentSlider.kt에 함수 선언이 정확히 1개 있어야 함",
            1,
            Regex("""(?m)^internal fun EditorPercentSlider\(""")
                .findAll(componentText)
                .count()
        )
    }

    @Test
    fun detailScreen_noLongerDeclaresEditorPercentSlider() {
        assertFalse(
            "DetailScreen.kt에 EditorPercentSlider 정의가 남아 있으면 안 됨",
            Regex("""(?m)^(private |internal )?fun EditorPercentSlider\(""")
                .containsMatchIn(detailScreenText)
        )
    }

    @Test
    fun componentFile_takesExpectedCoreParameters() {
        val expectedParams = listOf(
            "label: String",
            "percent: Int",
            "minPercent: Int",
            "maxPercent: Int",
            "enabled: Boolean",
            "onPreviewPercentChanged: (Int) -> Unit",
            "onPercentConfirmed: (Int) -> Unit"
        )
        for (param in expectedParams) {
            assertTrue(
                "[$param] 파라미터가 EditorPercentSlider 선언에 있어야 함",
                componentText.contains(param)
            )
        }
    }

    @Test
    fun componentFile_doesNotReferenceViewModelRepositoryOrContext() {
        assertFalse(
            "EditorPercentSlider는 ViewModel을 직접 참조하지 않아야 함",
            componentText.contains("ViewModel") || componentText.contains("viewModel")
        )
        assertFalse(
            "EditorPercentSlider는 Repository를 직접 참조하지 않아야 함",
            componentText.contains("Repository")
        )
        assertFalse(
            "EditorPercentSlider는 Context를 직접 참조하지 않아야 함",
            componentText.contains("Context")
        )
    }

    @Test
    fun componentFile_stillDelegatesToSharedEditorSlider() {
        assertTrue(
            "공용 EditorSlider(Material Slider 래퍼)를 그대로 사용해야 함",
            componentText.contains("EditorSlider(")
        )
    }

    @Test
    fun detailScreen_hasExactlyFiveCallSitesWithCoreParametersWired() {
        val callStarts = Regex("""(?m)^\s*EditorPercentSlider\(""")
            .findAll(detailScreenText)
            .map { it.range.first + it.value.indexOf("EditorPercentSlider") }
            .toList()

        assertEquals(
            "DetailScreen.kt의 EditorPercentSlider 호출은 정확히 5곳이어야 함",
            5,
            callStarts.size
        )

        callStarts.forEachIndexed { index, callStart ->
            val block = extractBalancedCall(detailScreenText, callStart)
            val label = "callSite#$index"

            assertTrue("[$label] label = 전달", block.contains("label ="))
            assertTrue("[$label] percent = 전달", block.contains("percent ="))
            assertTrue("[$label] minPercent = 전달", block.contains("minPercent ="))
            assertTrue("[$label] maxPercent = 전달", block.contains("maxPercent ="))
            assertTrue("[$label] enabled = 전달", block.contains("enabled ="))
            assertTrue(
                "[$label] onPreviewPercentChanged 콜백 전달",
                block.contains("onPreviewPercentChanged = {")
            )
            assertTrue(
                "[$label] onPercentConfirmed 콜백 전달",
                block.contains("onPercentConfirmed = {")
            )
        }
    }
}
