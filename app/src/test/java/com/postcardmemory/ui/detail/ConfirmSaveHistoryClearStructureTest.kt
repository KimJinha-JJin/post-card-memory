package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 확정 저장이 성공하면 꾸미기 요소 여섯 종류의 undo/redo 이력이 모두
 * 비워져야 한다. 하나라도 빠지면 그 요소만 저장 후에도 이전 상태로
 * undo돼 저장된 결과와 화면이 어긋난다 — 실제로 도장(seal) 이력이
 * 한동안 이 목록에서 빠져 있었다.
 *
 * DetailViewModel은 Context/Uri/MLKit에 의존해 이 프로젝트의 순수 JUnit
 * 환경에서 인스턴스화할 수 없으므로(ConfirmSaveLogicTest 상단 주석 참고),
 * StickerPositionCalculationsStructureTest와 같은 방식으로 소스 텍스트에서
 * 호출 목록을 고정한다.
 */
class ConfirmSaveHistoryClearStructureTest {

    private val decorationHistoryClearCalls = listOf(
        "clearStickerHistory()",
        "clearSealHistory()",
        "clearDoodleHistory()",
        "clearTextStickerHistory()",
        "clearMaskingTapeHistory()",
        "clearLabelStickerHistory()"
    )

    private val detailViewModelText: String by lazy {
        val file = listOf(
            "src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt",
            "app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"
        )
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error("DetailViewModel.kt를 찾을 수 없음(cwd=${File(".").absolutePath})")

        file.readText()
    }

    /**
     * saveEditsAndClearDraft의 성공 경로만 잘라낸다. 초기 로드·"원래대로"
     * 경로에도 같은 호출들이 있어서, 파일 전체를 세면 이 함수에서 빠진
     * 호출을 잡아낼 수 없다.
     */
    private val confirmSaveSuccessBlock: String by lazy {
        val functionStart =
            detailViewModelText.indexOf("fun saveEditsAndClearDraft(")
        assertTrue("saveEditsAndClearDraft를 찾지 못했다", functionStart >= 0)

        val successBranchStart =
            detailViewModelText.indexOf("if (allSaved) {", functionStart)
        assertTrue("성공 분기를 찾지 못했다", successBranchStart >= 0)

        // 초안 삭제 분기와 이력 정리 분기 둘 다 "if (allSaved) {"로 시작하므로
        // 이력 정리 쪽(두 번째)을 기준으로 잡는다.
        val historyBranchStart =
            detailViewModelText.indexOf(
                "if (allSaved) {",
                successBranchStart + 1
            )
        assertTrue("이력 정리 분기를 찾지 못했다", historyBranchStart >= 0)

        val functionEnd =
            detailViewModelText.indexOf("_confirmSaveState.value =", historyBranchStart)
        assertTrue("성공 분기의 끝을 찾지 못했다", functionEnd >= 0)

        detailViewModelText.substring(historyBranchStart, functionEnd)
    }

    @Test
    fun confirmSaveSuccessPath_clearsEveryDecorationHistory() {
        decorationHistoryClearCalls.forEach { call ->
            assertTrue(
                "[$call] 확정 저장 성공 경로에서 호출되지 않는다 — " +
                    "이 요소만 저장 후에도 이전 상태로 undo된다",
                confirmSaveSuccessBlock.contains(call)
            )
        }
    }

    @Test
    fun confirmSaveSuccessPath_clearsEachHistoryExactlyOnce() {
        decorationHistoryClearCalls.forEach { call ->
            assertEquals(
                "[$call] 호출이 정확히 한 번이어야 한다",
                1,
                Regex(Regex.escape(call)).findAll(confirmSaveSuccessBlock).count()
            )
        }
    }

    /**
     * 확정 저장·초기 로드·"원래대로" 세 경로가 같은 여섯 개를 다뤄야 한다는
     * 대칭을 고정한다. 새 꾸미기 요소를 추가하면서 한 경로에만 넣는 실수를
     * 막는 것이 목적이다.
     */
    @Test
    fun everyDecorationHistoryHasClearFunctionAndIsUsedInAllThreePaths() {
        decorationHistoryClearCalls.forEach { call ->
            val declaration = "private fun ${call.removeSuffix("()")}() {"
            assertTrue(
                "[$call] 선언을 찾지 못했다",
                detailViewModelText.contains(declaration)
            )

            // 선언 1회 + 확정 저장/초기 로드/"원래대로" 3회 = 최소 4회 등장.
            assertTrue(
                "[$call] 이 세 경로(확정 저장·초기 로드·원래대로) 모두에서 쓰이지 않는다",
                Regex(Regex.escape(call)).findAll(detailViewModelText).count() >= 3
            )
        }
    }
}
