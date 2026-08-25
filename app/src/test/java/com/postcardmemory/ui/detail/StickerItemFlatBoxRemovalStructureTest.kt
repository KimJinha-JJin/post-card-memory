package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 53일차 제9단계: 사진/텍스트/라벨 스티커의 `+ 추가` 타일과 목록 항목을
 * 감싸던 둥근 Box(DecorationPresetTile — 배경·clip·shape)를 없애고, 같은
 * 클릭 영역·선택 밑줄만 유지하는 평면 버전(EditorFlatPresetTile)으로
 * 옮겼다. DecorationPresetTile 자체는 배경 패턴·마스킹테이프·도장이 계속
 * 쓰므로 건드리지 않았다 — 이번 파일럿 3개 파일에서만 새 컴포저블을
 * 쓰는지 소스 텍스트 기준으로 고정한다. 라벨 목록 항목은 애초에
 * DecorationPresetTile을 쓴 적이 없어(이미 평면) 이번 단계에서 `+ 추가`
 * 타일만 바뀌었다.
 */
class StickerItemFlatBoxRemovalStructureTest {

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

    private fun readDetailSource(fileName: String): String =
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/$fileName",
                "app/src/main/java/com/postcardmemory/ui/detail/$fileName"
            )
        )

    private val photoStickerText: String by lazy {
        readDetailSource("PhotoStickerDetailScreen.kt")
    }

    private val textStickerText: String by lazy {
        readDetailSource("TextStickerDetailScreen.kt")
    }

    private val labelStickerText: String by lazy {
        readDetailSource("LabelStickerDetailScreen.kt")
    }

    private val sharedControlsText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/components/EditorSharedControls.kt",
                "app/src/main/java/com/postcardmemory/ui/components/EditorSharedControls.kt"
            )
        )
    }

    @Test
    fun editorFlatPresetTile_isDeclaredAndKeepsSelectionUnderline() {
        assertTrue(
            "EditorSharedControls.kt에 EditorFlatPresetTile 선언이 있어야 함",
            sharedControlsText.contains("fun EditorFlatPresetTile(")
        )
        val declarationStart =
            sharedControlsText.indexOf("fun EditorFlatPresetTile(")
        val body = sharedControlsText.substring(declarationStart)

        assertFalse(
            "EditorFlatPresetTile은 배경색을 칠하면 안 됨(Box 철거가 핵심)",
            body.substringBefore("preview: @Composable BoxScope.() -> Unit")
                .contains("backgroundColor")
        )
        assertTrue(
            "EditorFlatPresetTile도 DecorationPresetTile과 동일한 선택 밑줄(SunsetGold)을 유지해야 함",
            body.contains("if (selected) SunsetGold else Color.Transparent")
        )
    }

    @Test
    fun photoStickerScreen_noLongerUsesDecorationPresetTile() {
        assertFalse(
            "PhotoStickerDetailScreen.kt는 더 이상 DecorationPresetTile을 쓰면 안 됨(EditorFlatPresetTile로 대체)",
            photoStickerText.contains("DecorationPresetTile")
        )
        assertTrue(
            "PhotoStickerDetailScreen.kt는 EditorFlatPresetTile을 스티커 목록과 + 추가 타일 둘 다에 써야 함(선언 없이 호출 2곳)",
            Regex("""EditorFlatPresetTile\(""").findAll(photoStickerText).count() == 2
        )
    }

    @Test
    fun textStickerScreen_noLongerUsesDecorationPresetTile() {
        assertFalse(
            "TextStickerDetailScreen.kt는 더 이상 DecorationPresetTile을 쓰면 안 됨(EditorFlatPresetTile로 대체)",
            textStickerText.contains("DecorationPresetTile")
        )
        assertTrue(
            "TextStickerDetailScreen.kt는 EditorFlatPresetTile을 스티커 목록과 + 추가 타일 둘 다에 써야 함(호출 2곳)",
            Regex("""EditorFlatPresetTile\(""").findAll(textStickerText).count() == 2
        )
    }

    @Test
    fun labelStickerScreen_addTileNoLongerHasRoundedBackground() {
        // 라벨 목록 항목은 원래도 DecorationPresetTile을 쓴 적이 없었다 —
        // 이번 단계에서 바뀐 건 + 추가 타일뿐이다.
        assertTrue(
            "LabelStickerDetailScreen.kt는 + 추가 타일에 EditorFlatPresetTile을 써야 함(호출 1곳)",
            Regex("""EditorFlatPresetTile\(""").findAll(labelStickerText).count() == 1
        )
        assertFalse(
            "라벨 + 추가 타일에 더 이상 둥근 배경(RoundedCornerShape)이 남아 있으면 안 됨",
            labelStickerText.contains("RoundedCornerShape(10.dp)")
        )
    }
}
