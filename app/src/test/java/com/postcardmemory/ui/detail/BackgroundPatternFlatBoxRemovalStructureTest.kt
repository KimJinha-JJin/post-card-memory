package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 56일차 배경 UI 2차 정돈: 배경 패턴 선택 타일을 감싸던 둥근 Box
 * (DecorationPresetTile — 배경색+패턴 조합 미리보기 카드)를 없애고, 스티커
 * 목록과 같은 평면 버전(EditorFlatPresetTile)으로 옮겼다. 같은 차수에서
 * "직접 고르기"의 인라인 펼침(AnimatedVisibility + 색상 탭 하단 삽입)도
 * 제거하고 별도 Dialog로 옮겼다([[StickerItemFlatBoxRemovalStructureTest]]
 * 참고 — 그 시점엔 배경 패턴이 계속 DecorationPresetTile을 쓴다고
 * 전제했었다). Compose UI 테스트 인프라가 없는 프로젝트 관례에 따라
 * 소스 텍스트 기준으로 다음을 고정한다.
 */
class BackgroundPatternFlatBoxRemovalStructureTest {

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

    private val backgroundPickerText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/components/PostcardBackgroundPicker.kt",
                "app/src/main/java/com/postcardmemory/ui/components/PostcardBackgroundPicker.kt"
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
    fun backgroundPatternPicker_noLongerCallsDecorationPresetTile() {
        assertFalse(
            "PostcardBackgroundPicker.kt는 더 이상 DecorationPresetTile을 호출하면 안 됨(EditorFlatPresetTile로 대체) " +
                "— 과거 참고용 주석에서 이름을 언급하는 것은 허용하므로 실제 호출 형태(여는 괄호 포함)만 검사한다",
            backgroundPickerText.contains("DecorationPresetTile(")
        )
        assertTrue(
            "배경 패턴 선택에 EditorFlatPresetTile을 써야 함",
            backgroundPickerText.contains("EditorFlatPresetTile(")
        )
    }

    @Test
    fun backgroundPatternPicker_stillShowsAllNinePatternsWithLabel() {
        assertTrue(
            "패턴 9종(없음 포함) enum이 그대로 있어야 함",
            Regex("""NONE\([^)]*\),\s*DOTS\([^)]*\),\s*CHECKER\([^)]*\)""")
                .containsMatchIn(backgroundPickerText)
        )
        val declarationStart =
            backgroundPickerText.indexOf("fun PostcardBackgroundPatternPicker(")
        assertTrue(
            "PostcardBackgroundPatternPicker 선언을 찾지 못함",
            declarationStart >= 0
        )
        val body = backgroundPickerText.substring(declarationStart)
        assertTrue(
            "패턴 이름(label)을 EditorFlatPresetTile에 전달해야 함(기호 아래 이름 표시)",
            body.contains("label = pattern.label")
        )
    }

    @Test
    fun colorTab_noLongerInlinesCustomColorPickerWithAnimatedVisibility() {
        val colorPickerCallIndex =
            detailScreenText.indexOf("PostcardBackgroundColorPicker(")
        val patternPickerCallIndex =
            detailScreenText.indexOf("PostcardBackgroundPatternPicker(")
        assertTrue(
            "배경 색상/패턴 호출부를 모두 찾지 못함",
            colorPickerCallIndex >= 0 && patternPickerCallIndex > colorPickerCallIndex
        )
        val colorPanelText =
            detailScreenText.substring(colorPickerCallIndex, patternPickerCallIndex)

        assertFalse(
            "색상 탭 본문에서 PostcardCustomColorPicker를 직접 펼치면 안 됨(Dialog로 이동)",
            colorPanelText.contains("PostcardCustomColorPicker(")
        )
        assertFalse(
            "색상 탭 본문에 더 이상 AnimatedVisibility 인라인 펼침이 없어야 함",
            colorPanelText.contains("AnimatedVisibility")
        )
    }

    @Test
    fun customColorDialog_hostsPickerInsideAlertDialog() {
        val dialogGateIndex =
            detailScreenText.indexOf("if (showCustomColorDialog) {")
        assertTrue(
            "showCustomColorDialog 게이트를 찾지 못함",
            dialogGateIndex >= 0
        )
        val pickerCallIndex =
            detailScreenText.indexOf("PostcardCustomColorPicker(", dialogGateIndex)
        assertTrue(
            "showCustomColorDialog 게이트 안에서 PostcardCustomColorPicker 호출을 찾지 못함",
            pickerCallIndex > dialogGateIndex
        )
        val betweenGateAndPicker =
            detailScreenText.substring(dialogGateIndex, pickerCallIndex)
        assertTrue(
            "게이트와 picker 호출 사이에 AlertDialog가 있어야 함(별도 Dialog로 이동)",
            betweenGateAndPicker.contains("AlertDialog(")
        )
    }
}
