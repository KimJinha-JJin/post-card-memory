package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 54일차: 마스킹테이프의 세 생성 방식(기본 디자인/커스텀/사진)이 모두 같은
 * 자리의 `+ 추가` 하나로 시작하고, 실제 테이프는 각 생성창에서 `저장`을
 * 눌렀을 때만 만들어지는 구조를 고정한다.
 *
 * Compose UI 테스트 인프라가 없는 프로젝트 관례([[EditorSubcategoryNavBarStructureTest]]
 * 참고)에 따라 소스 텍스트 기준으로 검사한다.
 */
class MaskingTapeCreationGrammarStructureTest {

    private val panelText: String by lazy {
        val candidates = listOf(
            "src/main/java/com/postcardmemory/ui/detail/MaskingTapeDetailScreen.kt",
            "app/src/main/java/com/postcardmemory/ui/detail/MaskingTapeDetailScreen.kt"
        )
        val file = candidates
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error(
                "소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                    "candidates=$candidates"
            )
        file.readText()
    }

    @Test
    fun addEntryPoint_isRenderedOnceAndSharedByAllThreeCreationTabs() {
        assertEquals(
            "`+ 추가` 진입점(EditorFlatPresetTile)은 패널에 정확히 1곳만 있어야 함 — 탭마다 따로 두지 않는다",
            1,
            Regex("""EditorFlatPresetTile\(""").findAll(panelText).count()
        )

        // 탭 분기는 타일을 "그릴지 말지"가 아니라 "눌렀을 때 어디로 갈지"에만
        // 쓰여야 한다. 그래야 탭을 바꿔도 `+ 추가`의 위치가 움직이지 않는다.
        val tileIndex = panelText.indexOf("EditorFlatPresetTile(")
        val afterTile = panelText.substring(tileIndex)
        val onClickBody = afterTile.substringAfter("onClick = {").substringBefore("enabled =")
        assertTrue(
            "creationTabIndex 분기는 `+ 추가`의 onClick 안에 있어야 함",
            onClickBody.contains("when (creationTabIndex)")
        )
        assertTrue(
            "기본 디자인 탭은 프리셋 생성 Dialog를 열어야 함",
            onClickBody.contains("0 -> showPresetCreateDialog = true")
        )
        assertTrue(
            "커스텀 탭은 커스텀 생성 Dialog를 열어야 함",
            onClickBody.contains("1 -> showCustomCreateDialog = true")
        )
        assertTrue(
            "사진 탭은 시스템 사진 피커를 열어야 함",
            onClickBody.contains("photoPicker.launch(")
        )
    }

    @Test
    fun creationTabIndex_doesNotGateWhichContentIsRendered() {
        // creationTabIndex를 읽는 곳은 `+ 추가`의 onClick 하나뿐이어야 한다.
        // (파라미터 선언과 KDoc 언급은 제외하고 본문 사용처만 센다.)
        val bodyUsages =
            Regex("""when \(creationTabIndex\)""").findAll(panelText).count()
        assertEquals(
            "creationTabIndex로 패널 콘텐츠 자체를 갈아끼우면 안 됨(서랍형 부활 방지)",
            1,
            bodyUsages
        )
    }

    @Test
    fun emptyStateHasNoHintBox_becauseAddTileAlwaysShowsInTheList() {
        // `+ 추가`가 썸네일 줄 안에 항상 있으므로, 비어 있을 때만 뜨던
        // 흰 안내 상자는 같은 말을 두 번 하는 셈이라 두지 않는다.
        assertFalse(
            "빈 상태 안내 상자(EditorEmptyHint)는 쓰지 않는다",
            panelText.contains("EditorEmptyHint")
        )
    }

    @Test
    fun inlineCustomEditorIsGone() {
        assertFalse(
            "인라인 커스텀 편집기는 제거되고 생성 Dialog로 옮겨져야 함",
            panelText.contains("MaskingTapeCustomEditor")
        )
        assertFalse(
            "인라인 편집기 전용 높이 제한 상수도 남아 있으면 안 됨",
            panelText.contains("MASKING_TAPE_CUSTOM_EDITOR_CONTROLS_MAX_HEIGHT")
        )
    }

    @Test
    fun createDialogsCommitOnlyOnConfirm() {
        listOf(
            "MaskingTapePresetCreateDialog",
            "MaskingTapeCustomCreateDialog"
        ).forEach { dialogName ->
            assertTrue(
                "$dialogName 선언이 있어야 함",
                panelText.contains("private fun $dialogName(")
            )
        }

        // 생성 콜백은 각 Dialog의 onConfirm 안에서만 호출돼야 한다 —
        // 프리셋을 탭하거나 색을 바꾸는 것만으로 테이프가 생기면 안 된다.
        assertEquals(
            "onAddMaskingTape 호출은 프리셋 Dialog의 onConfirm 1곳뿐이어야 함",
            1,
            Regex("""onAddMaskingTape\(""").findAll(panelText).count()
        )
        assertEquals(
            "onAddCustomMaskingTape 호출은 커스텀 Dialog의 onConfirm 1곳뿐이어야 함",
            1,
            Regex("""onAddCustomMaskingTape\(""").findAll(panelText).count()
        )

        val presetTileBlock =
            panelText.substringAfter("private fun MaskingTapePresetCreateDialog(")
        assertTrue(
            "프리셋을 탭하면 생성이 아니라 local draft(selectedStyleDraft)만 바뀌어야 함",
            presetTileBlock.contains("onClick = { selectedStyleDraft = style }")
        )
    }

    @Test
    fun selectedTapeActionsStayFlatTextActions() {
        // 53~54일차에 확정된 평면 contextual action을 되돌리지 않는다.
        listOf("\"편집\"", "\"복제\"", "\"삭제\"").forEach { label ->
            assertTrue(
                "$label 액션이 있어야 함",
                panelText.contains(label)
            )
        }
        assertFalse(
            "선택된 테이프 액션을 다시 외곽선 버튼으로 되돌리면 안 됨",
            panelText.contains("EditorOutlineButton")
        )
    }
}
