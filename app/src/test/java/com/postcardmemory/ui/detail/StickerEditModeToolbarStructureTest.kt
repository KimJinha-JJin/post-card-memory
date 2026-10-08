package com.postcardmemory.ui.detail

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 선택한 사진 스티커의 Property(배경제거·레이어순서) 툴바인
 * `StickerEditModeToolbar`가 StickerEditModeToolbar.kt에 떨어진 순수 UI 조각으로
 * 남는 구조를 고정한다. `src/test` JVM 환경에는 Robolectric이 없어 Composable을
 * 직접 렌더링할 수 없으므로([[SaveErrorDialogStructureTest]] 상단 주석 참고),
 * 소스 텍스트 기준으로 다음을 고정한다:
 *  - Toolbar 정의는 분리된 파일에 정확히 하나만 있고 DetailScreen.kt에는 남지 않음
 *  - 항목은 공용 `EditorTextAction` 평면 텍스트 Action이며, 예전 filled Box인
 *    `StickerEditModeButton`이나 툴바를 감싸는 NeutralLight 둥근 배경이 다시
 *    생기지 않음
 *  - ViewModel/Repository/Context/gesture/저장/Undo를 직접 참조하지 않음
 *  - 호출부는 DetailScreen.kt가 아니라 `PhotoStickerPickerPanel` 안(복제·삭제와
 *    같은 "선택한 스티커" 블록) 한 곳이고, 상태·콜백이 그대로 연결됨
 *
 * "AlertDialog("가 "SaveResultAlertDialog(" 안에도 걸리는 것 같은 부분 문자열
 * 오탐을 피하려고, 함수 선언 검사는 줄 시작 앵커로 제한하고 호출부 경계는
 * 들여쓰기 공백 수 대신 괄호 깊이를 직접 스캔해 잘라낸다.
 */
class StickerEditModeToolbarStructureTest {

    private val componentText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/StickerEditModeToolbar.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/StickerEditModeToolbar.kt"
            )
        )
    }

    private val detailScreenText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
            )
        )
    }

    private val pickerPanelText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/PhotoStickerDetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/PhotoStickerDetailScreen.kt"
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
    fun componentFile_declaresStickerEditModeToolbarExactlyOnce() {
        assertEquals(
            "StickerEditModeToolbar.kt에 Toolbar 함수 선언이 정확히 1개 있어야 함",
            1,
            Regex("""(?m)^internal fun StickerEditModeToolbar\(""")
                .findAll(componentText)
                .count()
        )
    }

    @Test
    fun componentFile_noLongerDeclaresStickerEditModeButton() {
        // 53일차 제8차: 개별 항목을 감싸던 filled Box(StickerEditModeButton)를
        // 완전히 삭제하고 공용 EditorTextAction으로 대체했다.
        assertFalse(
            "StickerEditModeButton 선언이 더 이상 남아 있으면 안 됨(EditorTextAction으로 대체)",
            componentText.contains("StickerEditModeButton")
        )
        assertFalse(
            "StickerEditModeButton은 DetailScreen.kt에서도 참조되면 안 됨",
            detailScreenText.contains("StickerEditModeButton")
        )
        assertTrue(
            "StickerEditModeToolbar.kt는 공용 EditorTextAction을 써야 함",
            componentText.contains("EditorTextAction")
        )
    }

    @Test
    fun componentFile_noLongerWrapsToolbarInRoundedBackground() {
        // NeutralLight 둥근 배경(기존 툴바 chrome)이 완전히 사라졌는지 확인.
        assertFalse(
            "StickerEditModeToolbar.kt에 NeutralLight 배경이 남아 있으면 안 됨",
            componentText.contains("NeutralLight")
        )
        assertFalse(
            "StickerEditModeToolbar.kt에 RoundedCornerShape가 남아 있으면 안 됨(개별/전체 Box 제거)",
            componentText.contains("RoundedCornerShape")
        )
    }

    @Test
    fun detailScreen_noLongerDeclaresToolbarOrButton() {
        assertFalse(
            "DetailScreen.kt에 StickerEditModeToolbar 정의가 남아 있으면 안 됨",
            Regex("""(?m)^(private |internal )?fun StickerEditModeToolbar\(""")
                .containsMatchIn(detailScreenText)
        )
        assertFalse(
            "DetailScreen.kt에 StickerEditModeButton 정의가 남아 있으면 안 됨",
            Regex("""(?m)^(private |internal )?fun StickerEditModeButton\(""")
                .containsMatchIn(detailScreenText)
        )
    }

    @Test
    fun componentFile_takesExpectedCoreParameters() {
        val expectedParams = listOf(
            "sticker: PhotoStickerItem",
            "isRemovingBackground: Boolean",
            "onToggleBackgroundRemoval: () -> Unit",
            "canMoveForward: Boolean",
            "canMoveBackward: Boolean",
            "onMoveForward: () -> Unit",
            "onMoveBackward: () -> Unit",
            "enabled: Boolean"
        )
        for (param in expectedParams) {
            assertTrue(
                "[$param] 파라미터가 StickerEditModeToolbar 선언에 있어야 함",
                componentText.contains(param)
            )
        }
    }

    @Test
    fun componentFile_doesNotReferenceViewModelRepositoryContextOrSaveLogic() {
        val forbiddenTokens = listOf(
            "ViewModel",
            "viewModel",
            "Repository",
            "Context",
            "pointerInput",
            "CoroutineScope",
            "recordStickerSnapshotForUndo",
            "setPhotoStickers",
            "moveStickerForward",
            "moveStickerBackward",
            "removeStickerBackground"
        )
        for (token in forbiddenTokens) {
            assertFalse(
                "[$token] StickerEditModeToolbar/Button은 이 토큰을 직접 참조하지 않아야 함",
                componentText.contains(token)
            )
        }
    }

    @Test
    fun detailScreen_noLongerCallsToolbarDirectly() {
        assertFalse(
            "DetailScreen.kt는 더 이상 StickerEditModeToolbar를 직접 호출하면 안 됨" +
                "(PhotoStickerPickerPanel 안으로 옮겨감)",
            Regex("""(?m)^\s*StickerEditModeToolbar\(""")
                .containsMatchIn(detailScreenText)
        )
    }

    @Test
    fun pickerPanel_hasExactlyOneCallSiteWithExistingStateAndCallbacksWired() {
        val callStarts = Regex("""(?m)^\s*StickerEditModeToolbar\(""")
            .findAll(pickerPanelText)
            .map { it.range.first + it.value.indexOf("StickerEditModeToolbar") }
            .toList()

        assertEquals(
            "PhotoStickerDetailScreen.kt의 StickerEditModeToolbar 호출은 정확히 1곳이어야 함",
            1,
            callStarts.size
        )

        val block = extractBalancedCall(pickerPanelText, callStarts.single())

        assertTrue("sticker = 전달", block.contains("sticker = selectedSticker"))
        assertTrue(
            "isRemovingBackground = 전달",
            block.contains("isRemovingBackground = isRemovingBackground")
        )
        assertTrue(
            "onToggleBackgroundRemoval 콜백 전달",
            block.contains("onToggleBackgroundRemoval = onToggleBackgroundRemoval")
        )
        assertTrue(
            "canMoveForward = 전달",
            block.contains("canMoveForward = canMoveForward")
        )
        assertTrue(
            "canMoveBackward = 전달",
            block.contains("canMoveBackward = canMoveBackward")
        )
        assertTrue(
            "onMoveForward 콜백 전달",
            block.contains("onMoveForward = onMoveForward")
        )
        assertTrue(
            "onMoveBackward 콜백 전달",
            block.contains("onMoveBackward = onMoveBackward")
        )
        assertTrue("enabled = 전달", block.contains("enabled = enabled"))
    }

    @Test
    fun pickerPanel_declaresToolbarRelayParameters() {
        val expectedParams = listOf(
            "isRemovingBackground: Boolean",
            "onToggleBackgroundRemoval: () -> Unit",
            "canMoveForward: Boolean",
            "canMoveBackward: Boolean",
            "onMoveForward: () -> Unit",
            "onMoveBackward: () -> Unit"
        )
        for (param in expectedParams) {
            assertTrue(
                "[$param] 파라미터가 PhotoStickerPickerPanel 선언에 있어야 함",
                pickerPanelText.contains(param)
            )
        }
    }
}
