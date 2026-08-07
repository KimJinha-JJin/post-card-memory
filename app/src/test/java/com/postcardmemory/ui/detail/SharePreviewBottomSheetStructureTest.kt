package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 제8차(2026-08-07)에서 `SharePreviewBottomSheet`를 DetailScreen.kt에서
 * SharePreviewBottomSheet.kt로 분리했다. 이 프로젝트는 Compose UI 테스트
 * 인프라를 쓰지 않으므로([[SaveErrorDialogStructureTest]] 상단 주석 참고),
 * 소스 텍스트 기준으로 다음을 고정한다:
 *  - 분리된 파일에는 미리보기·버튼만 그리는 순수 렌더링 함수만 존재하고,
 *    Context/Intent/FileProvider/Toast/공유 실행/ViewModel/Repository는
 *    전혀 참조하지 않음
 *  - 공유 실행(Context 획득, Intent 생성, FileProvider, chooser 실행, Toast)과
 *    `isLaunchingShareChooser` 가드는 DetailScreen.kt 호출부에 그대로 남음
 *  - `sheetState`(ModalBottomSheet 상태)는 호출부에서 hoist되어 전달됨 —
 *    공유 버튼 클릭 후의 "애니메이션과 함께 닫기"는 실행 결과에 달려 있어
 *    호출부가 계속 제어해야 하기 때문
 *  - DetailScreen.kt의 단일 호출부가 계산된 값·콜백을 그대로 전달함
 *
 * 제3차에서 "AlertDialog(" 부분 문자열이 "SaveResultAlertDialog(" 안에도
 * 걸려 개수를 잘못 세었던 오탐을 반복하지 않기 위해 함수 선언 검사는 줄 시작
 * 앵커로 제한하고, 호출부 경계는 괄호 깊이를 직접 스캔해 잘라낸다.
 */
class SharePreviewBottomSheetStructureTest {

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
                "src/main/java/com/postcardmemory/ui/detail/SharePreviewBottomSheet.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/SharePreviewBottomSheet.kt"
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
    fun componentFile_declaresSharePreviewBottomSheetExactlyOnce() {
        assertEquals(
            "SharePreviewBottomSheet.kt에 함수 선언이 정확히 1개 있어야 함",
            1,
            Regex("""(?m)^internal fun SharePreviewBottomSheet\(""")
                .findAll(componentText)
                .count()
        )
    }

    @Test
    fun detailScreen_noLongerDeclaresSharePreviewBottomSheet() {
        assertFalse(
            "DetailScreen.kt에 SharePreviewBottomSheet 정의가 남아 있으면 안 됨",
            Regex("""(?m)^(private |internal )?fun SharePreviewBottomSheet\(""")
                .containsMatchIn(detailScreenText)
        )
    }

    @Test
    fun componentFile_takesOnlyDisplayValuesAndCallbacks() {
        val expectedParams = listOf(
            "file: File",
            "enabled: Boolean",
            "sheetState: SheetState",
            "onDismissed: () -> Unit",
            "onShare: () -> Unit"
        )
        for (param in expectedParams) {
            assertTrue(
                "[$param] 파라미터가 SharePreviewBottomSheet 선언에 있어야 함",
                componentText.contains(param)
            )
        }
    }

    @Test
    fun componentFile_doesNotReferencePlatformShareExecutionLogic() {
        val forbiddenTokens = listOf(
            "LocalContext",
            "Context",
            "Intent(",
            "ACTION_SEND",
            "createChooser",
            "startActivity",
            "FileProvider",
            "Toast",
            "isLaunchingShareChooser",
            "rememberCoroutineScope",
            "ViewModel",
            "viewModel",
            "Repository"
        )
        for (token in forbiddenTokens) {
            assertFalse(
                "[$token] SharePreviewBottomSheet는 이 토큰을 직접 참조하지 않아야 함",
                componentText.contains(token)
            )
        }
    }

    @Test
    fun detailScreen_stillOwnsShareExecutionAndFileProviderLogic() {
        assertTrue(
            "Intent(Intent.ACTION_SEND) 생성이 DetailScreen.kt에 남아 있어야 함",
            detailScreenText.contains("Intent(Intent.ACTION_SEND)")
        )
        assertTrue(
            "FileProvider.getUriForFile 호출이 DetailScreen.kt에 남아 있어야 함",
            detailScreenText.contains("FileProvider.getUriForFile(")
        )
        assertTrue(
            "context.startActivity 호출이 DetailScreen.kt에 남아 있어야 함",
            detailScreenText.contains("context.startActivity(")
        )
        assertTrue(
            "Intent.createChooser 호출이 DetailScreen.kt에 남아 있어야 함",
            detailScreenText.contains("Intent.createChooser(")
        )
    }

    @Test
    fun detailScreen_hasExactlyOneCallSiteWiringHoistedStateAndCallbacks() {
        val callStarts = Regex("""(?m)^\s*SharePreviewBottomSheet\(""")
            .findAll(detailScreenText)
            .map { it.range.first + it.value.indexOf("SharePreviewBottomSheet") }
            .toList()

        assertEquals(
            "DetailScreen.kt의 SharePreviewBottomSheet 호출은 정확히 1곳이어야 함",
            1,
            callStarts.size
        )

        val block = extractBalancedCall(detailScreenText, callStarts.single())

        assertTrue("file = 전달", block.contains("file = readyShareState.file"))
        assertTrue(
            "enabled = 기존 가드(!isLaunchingShareChooser) 전달",
            block.contains("enabled = !isLaunchingShareChooser")
        )
        assertTrue(
            "sheetState = hoist된 상태 전달",
            block.contains("sheetState = shareSheetState")
        )
        assertTrue(
            "onDismissed = 기존 콜백 그대로 전달",
            block.contains("onDismissed = { viewModel.resetShareState() }")
        )
        assertTrue(
            "onShare = 기존 공유 실행 함수 그대로 전달",
            block.contains("onShare = { launchShareChooser() }")
        )
    }
}
