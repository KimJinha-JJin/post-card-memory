package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 제6차(2026-08-07)에서 `StickerEditModeToolbar`(및 Toolbar 전용 helper인
 * `StickerEditModeButton`)를 DetailScreen.kt에서 StickerEditModeToolbar.kt로
 * 물리적으로 분리했다. 이 프로젝트는 Compose UI 테스트 인프라를 쓰지 않으므로
 * ([[SaveErrorDialogStructureTest]] 상단 주석 참고), 소스 텍스트 기준으로
 * 다음을 고정한다:
 *  - 분리된 파일에만 두 함수 정의가 존재하고(DetailScreen.kt에는 남지 않음)
 *    ViewModel/Repository/Context/gesture/저장/Undo를 참조하지 않음
 *  - `StickerEditModeButton`은 Toolbar 파일 안에서만 쓰이는 private helper로
 *    남아 DetailScreen.kt 어디에서도 더 이상 참조되지 않음
 *  - `StickerEditMode` enum은 접근 범위만 private→internal로 넓어졌을 뿐
 *    멤버(Move/Scale/Rotate)는 그대로임
 *  - DetailScreen.kt의 단일 호출부가 기존과 동일한 상태·콜백으로 연결됨
 *
 * 제3차에서 "AlertDialog(" 부분 문자열이 "SaveResultAlertDialog(" 안에도
 * 걸려 개수를 잘못 세었던 오탐을 반복하지 않기 위해 함수 선언 검사는 줄 시작
 * 앵커로 제한하고, 호출부 경계는 들여쓰기 공백 수 대신 괄호 깊이를 직접
 * 스캔해 잘라낸다.
 *
 * 이후 디자인 폴리시 작업(2026-08-20)에서 pinch/twist 제스처가 이동·크기·
 * 회전을 이미 전부 처리하게 되면서 Move/Scale/Rotate 모드 전환 버튼과
 * 좌우·상하대칭 버튼을 툴바에서 제거했다. `editMode`/`onModeSelected`/
 * `onToggleFlipHorizontal`/`onToggleFlipVertical` 파라미터가 그래서 빠졌다.
 * `StickerEditMode` enum 자체와 기존 flip 데이터·렌더링은 그대로다.
 *
 * 53일차(2026-08-25) 스티커 UI/UX 문법 파일럿에서 Property(배경제거·레이어순서)와
 * Object Action(복제·삭제)이 스크롤 본문의 서로 다른 위치에 떨어져 있던 걸
 * "선택한 스티커" 컨텍스트 하나로 모았다. 그 결과 `StickerEditModeToolbar` 호출부가
 * DetailScreen.kt에서 `PhotoStickerDetailScreen.kt`의 `PhotoStickerPickerPanel`
 * 안(복제·삭제 버튼과 같은 블록)으로 옮겨갔다. DetailScreen.kt는 이제 상태·콜백을
 * `PhotoStickerPickerPanel`에 파라미터로 전달만 하고 Toolbar를 직접 호출하지 않는다.
 */
class StickerEditModeToolbarStructureTest {

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
                "src/main/java/com/postcardmemory/ui/detail/StickerEditModeToolbar.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/StickerEditModeToolbar.kt"
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

    private val pickerPanelText: String by lazy {
        readSource(
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
    fun componentFile_declaresStickerEditModeButtonAsPrivateHelperOnly() {
        assertEquals(
            "StickerEditModeToolbar.kt에 Button 함수 선언이 정확히 1개 있어야 함",
            1,
            Regex("""(?m)^private fun StickerEditModeButton\(""")
                .findAll(componentText)
                .count()
        )
        assertFalse(
            "StickerEditModeButton은 DetailScreen.kt에서 더 이상 참조되면 안 됨(Toolbar 전용 helper)",
            detailScreenText.contains("StickerEditModeButton")
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
    fun stickerEditModeEnum_visibilityWidenedButMembersUnchanged() {
        assertTrue(
            "StickerEditMode enum은 internal로 가시성이 넓어져야 함(Toolbar 파일에서 참조 가능해야 함)",
            Regex("""(?m)^internal enum class StickerEditMode \{""")
                .containsMatchIn(detailScreenText)
        )
        assertFalse(
            "StickerEditMode enum이 다시 private으로 남아 있으면 안 됨",
            Regex("""(?m)^private enum class StickerEditMode \{""")
                .containsMatchIn(detailScreenText)
        )
        for (member in listOf("Move", "Scale", "Rotate")) {
            assertTrue(
                "[$member] StickerEditMode enum 멤버가 유지돼야 함",
                detailScreenText.contains(member)
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
