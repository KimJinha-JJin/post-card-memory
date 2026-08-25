package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 53일차 제5단계: 텍스트/라벨 스티커의 글자색·테두리색·테이프 스타일
 * Property를 하단 상시 패널에서 "선택 → 편집창 → 저장" Dialog로 옮겼다.
 * 이어서 생성(Add/Create) 다이얼로그에도 기타(커스텀) 색상을 열어, 수정
 * 창에서만 가능하던 커스텀 색상을 최초 추가 시점에도 쓸 수 있게 했다.
 * 그 뒤 텍스트 스티커의 글자 채움색은 사용자가 직접 고르지 않고, 라벨의
 * 테이프→문자색과 동일한 규칙(labelStickerTextColorArgbFor)으로 테두리색의
 * 밝기에서 자동으로 정하도록 단순화했다 — 사용자가 고르는 색은 테두리색
 * 하나뿐이다.
 * Compose UI 테스트 인프라가 없는 프로젝트 관례([[StickerEditModeToolbarStructureTest]]
 * 참고)에 따라 소스 텍스트 기준으로 다음을 고정한다:
 *  - 두 패널의 공개 시그니처에 개별 색상/스타일 콜백이 더 이상 없음
 *  - 색상/스타일 편집 UI(TextStickerColorPickerSection/LabelTapeStyleRow)는
 *    선언 1곳 + Add/Create·Edit Dialog 호출로만 존재함
 *  - 텍스트 Add/Edit 다이얼로그는 테두리색만 고르고, 글자색은
 *    labelStickerTextColorArgbFor로 자동 계산해 onConfirm에 실어보냄
 *  - 라벨 Add/Create 다이얼로그는 커스텀 색상까지 confirm에 실어보냄
 *  - 라벨의 완료 Action 문구가 "뽑기"가 아니라 "저장"임
 */
class TextLabelStickerPropertyEditStructureTest {

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

    private val textStickerText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/TextStickerDetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/TextStickerDetailScreen.kt"
            )
        )
    }

    private val labelStickerText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/LabelStickerDetailScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/LabelStickerDetailScreen.kt"
            )
        )
    }

    @Test
    fun textStickerPickerPanel_noLongerTakesPerColorCallbacks() {
        // TextStickerColorPickerSection(선언은 그대로 유지)의 자체 파라미터인
        // onCustomColorSelected: (Long) -> Unit과 겹치지 않도록, 예전 패널
        // 시그니처에만 있던 (id: String, ...) 꼴 전체를 확인한다.
        val removedParams = listOf(
            "onColorSelected: (id: String, colorArgb: Long) -> Unit",
            "onOutlineColorSelected: (id: String, outlineColorArgb: Long) -> Unit",
            "onEnterCustomColor: () -> Unit",
            "onCustomColorSelected: (id: String, colorArgb: Long) -> Unit",
            "onEnterCustomOutlineColor: () -> Unit",
            "onCustomOutlineColorSelected: (id: String, outlineColorArgb: Long) -> Unit"
        )
        for (param in removedParams) {
            assertFalse(
                "[$param]는 TextStickerPickerPanel에서 제거됐어야 함(EditDialog로 이동)",
                textStickerText.contains(param)
            )
        }
    }

    @Test
    fun textStickerColorPickerSection_existsOnlyAsDeclarationAndOutlineDialogCalls() {
        // 선언 1개 + AddDialog 테두리색 호출 1개 + EditDialog 테두리색 호출 1개 = 3.
        // 글자색은 더 이상 이 섹션으로 고르지 않으므로(자동 계산) 하단 상시
        // 패널이나 별도 글자색 호출은 존재하지 않는다.
        assertEquals(
            "TextStickerColorPickerSection은 선언 1개 + Add/Edit Dialog 테두리색 호출 2개만 있어야 함",
            3,
            Regex("""TextStickerColorPickerSection\(""")
                .findAll(textStickerText)
                .count()
        )
    }

    @Test
    fun textStickerEditDialog_noLongerTakesColorDraftParameter() {
        assertFalse(
            "글자색은 자동 계산되므로 TextStickerEditDialog에 initialColorArgb 파라미터가 없어야 함",
            textStickerText.contains("initialColorArgb: Long")
        )
        assertTrue(
            "initialOutlineColorArgb 파라미터는 그대로 있어야 함",
            textStickerText.contains("initialOutlineColorArgb: Long")
        )
    }

    @Test
    fun textStickerDialogs_deriveGlyphColorFromOutlineLuminance() {
        // Add/Edit 두 다이얼로그의 onConfirm 호출부 모두 라벨과 동일한
        // labelStickerTextColorArgbFor(outlineColorArgbDraft)로 글자색을
        // 계산해 넘겨야 한다 — 사용자가 직접 고른 값을 쓰지 않는다.
        assertEquals(
            "labelStickerTextColorArgbFor(outlineColorArgbDraft) 호출이 Add/Edit 두 곳에 있어야 함",
            2,
            Regex("""labelStickerTextColorArgbFor\(outlineColorArgbDraft\)""")
                .findAll(textStickerText)
                .count()
        )
        assertFalse(
            "글자색 draft(colorArgbDraft)는 더 이상 존재하면 안 됨 — 자동 계산만 씀",
            textStickerText.contains("colorArgbDraft")
        )
    }

    @Test
    fun labelStickerPickerPanel_noLongerTakesPerTapeCallbacks() {
        val removedParams = listOf(
            "onTapeStyleSelected:",
            "onEnterCustomTapeColor:",
            "onCustomTapeColorSelected:"
        )
        for (param in removedParams) {
            assertFalse(
                "[$param] 파라미터는 LabelStickerPickerPanel에서 제거됐어야 함(EditDialog로 이동)",
                labelStickerText.contains(param)
            )
        }
    }

    @Test
    fun labelTapeStyleRow_existsOnlyAsDeclarationAndDialogCalls() {
        // 선언 1개 + CreateDialog 호출 1개 + EditDialog 호출 1개 = 3.
        // 둘 다 이제 커스텀 색상 파라미터까지 넘긴다(프리셋 전용이 아님).
        assertEquals(
            "LabelTapeStyleRow는 선언 1개 + Create/Edit Dialog 호출 2개만 있어야 함",
            3,
            Regex("""LabelTapeStyleRow\(""")
                .findAll(labelStickerText)
                .count()
        )
    }

    @Test
    fun labelStickerCreateDialog_supportsCustomTapeColor() {
        assertTrue(
            "LabelStickerCreateDialog의 onConfirm은 customTapeColorArgb까지 넘겨야 함",
            Regex(
                """onConfirm:\s*\(\s*text: String,\s*style: LabelTapeStyle,\s*customTapeColorArgb: Long\?\s*\)\s*->\s*Unit"""
            ).containsMatchIn(labelStickerText)
        )
        assertTrue(
            "LabelStickerCreateDialog는 customTapeColorArgb draft를 local state로 들고 있어야 함",
            labelStickerText.contains("var customTapeColorArgb by remember")
        )
    }

    @Test
    fun labelStickerEditDialog_takesStyleAndCustomColorDraftParameters() {
        val expectedParams = listOf(
            "initialStyle: LabelTapeStyle",
            "initialCustomTapeColorArgb: Long?"
        )
        for (param in expectedParams) {
            assertTrue(
                "[$param] 파라미터가 LabelStickerEditDialog 선언에 있어야 함",
                labelStickerText.contains(param)
            )
        }
    }

    @Test
    fun labelStickerCreateDialog_confirmActionIsSaveNotPick() {
        assertEquals(
            "라벨 흐름의 완료 Action(\"저장\") 문구는 생성/수정 다이얼로그 2곳에만 있어야 함",
            2,
            Regex(""""저장",""").findAll(labelStickerText).count()
        )
        assertFalse(
            "라벨 생성 확인 버튼에 \"뽑기\" 문구가 더 이상 남아 있으면 안 됨",
            labelStickerText.contains("text = \"뽑기\"")
        )
    }
}
