package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 제4차(2026-08-07)에서 `EditorBottomTabBar`를 DetailScreen.kt에서
 * EditorBottomTabBar.kt로 물리적으로 분리했다. 이 프로젝트는 Compose UI
 * 테스트 인프라를 쓰지 않으므로([[SaveErrorDialogStructureTest]] 상단 주석
 * 참고), 소스 텍스트 기준으로 다음을 고정한다:
 *  - 분리된 파일에 순수 UI 함수가 존재하고 ViewModel/Repository를 참조하지 않음
 *  - DetailScreen.kt의 단일 호출부가 기존과 동일한 상태·콜백을 전달함
 *
 * "AlertDialog(" 부분 문자열이 "SaveResultAlertDialog(" 안에도 걸려 개수를
 * 잘못 세었던 제3차의 오탐을 반복하지 않기 위해, 여기서는 검색 대상 식별자
 * ("EditorBottomTabBar")가 이 저장소 어디에도 다른 식별자의 접미사로
 * 등장하지 않음을 전제로 하되, 선언 검사는 "fun EditorBottomTabBar("처럼
 * 앞뒤 문맥을 포함한 리터럴로 앵커링해 우연한 매치를 피한다.
 */
class EditorBottomTabBarStructureTest {

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
                "src/main/java/com/postcardmemory/ui/detail/EditorBottomTabBar.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/EditorBottomTabBar.kt"
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

    @Test
    fun componentFile_declaresEditorBottomTabBarExactlyOnce() {
        assertEquals(
            "EditorBottomTabBar.kt에 함수 선언이 정확히 1개 있어야 함",
            1,
            Regex("""(?m)^internal fun EditorBottomTabBar\(""")
                .findAll(componentText)
                .count()
        )
    }

    @Test
    fun componentFile_takesOnlyPlainStateAndACallback() {
        val expectedParams = listOf(
            "selectedPage: Int",
            "labels: List<String>",
            "icons: List<ImageVector>",
            "enabled: Boolean",
            "onTabSelected: (Int) -> Unit"
        )
        for (param in expectedParams) {
            assertTrue(
                "[$param] 파라미터가 EditorBottomTabBar 선언에 있어야 함",
                componentText.contains(param)
            )
        }
    }

    @Test
    fun componentFile_doesNotReferenceViewModelOrRepository() {
        assertFalse(
            "EditorBottomTabBar는 ViewModel을 직접 참조하지 않아야 함",
            componentText.contains("ViewModel") || componentText.contains("viewModel")
        )
        assertFalse(
            "EditorBottomTabBar는 Repository를 직접 참조하지 않아야 함",
            componentText.contains("Repository")
        )
    }

    @Test
    fun detailScreen_hasExactlyOneCallSiteWithExistingStateAndCallback() {
        val callSiteStart = detailScreenText.indexOf("EditorBottomTabBar(")
        assertTrue(
            "DetailScreen.kt에 EditorBottomTabBar 호출부가 있어야 함",
            callSiteStart >= 0
        )
        assertEquals(
            "DetailScreen.kt의 EditorBottomTabBar 호출은 정확히 1곳이어야 함",
            1,
            Regex("""(?m)^\s*EditorBottomTabBar\(""")
                .findAll(detailScreenText)
                .count()
        )

        // 다음 형제 구문(SnackbarHost) 시작 전까지를 호출부 블록으로 본다.
        // 들여쓰기 공백 수에 의존하는 경계 탐색은 사소한 포맷 변경에도
        // 깨지기 쉬워, 저장소에 유일하게 등장하는 리터럴을 종료 앵커로 쓴다.
        val terminalAnchor = "SnackbarHost("
        val terminalIdx = detailScreenText.indexOf(terminalAnchor, callSiteStart)
        assertTrue(
            "종료 앵커($terminalAnchor)를 찾지 못함",
            terminalIdx >= 0
        )
        val callSiteBlock = detailScreenText.substring(callSiteStart, terminalIdx)

        assertTrue(
            "selectedPage가 기존 pager 상태를 그대로 전달해야 함",
            callSiteBlock.contains("selectedPage = customizationPagerState.currentPage")
        )
        assertTrue(
            "labels가 기존 라벨 목록을 그대로 전달해야 함",
            callSiteBlock.contains("labels = customizationPageLabels")
        )
        assertTrue(
            "icons가 기존 아이콘 목록을 그대로 전달해야 함",
            callSiteBlock.contains("icons = customizationPageIcons")
        )
        assertTrue(
            "enabled가 기존 controlsEnabled를 그대로 전달해야 함",
            callSiteBlock.contains("enabled = controlsEnabled")
        )
        assertTrue(
            "onTabSelected가 기존 pager 스크롤 콜백을 그대로 호출해야 함",
            callSiteBlock.contains("customizationPagerState") &&
                callSiteBlock.contains(".animateScrollToPage(pageIndex)")
        )
    }
}
