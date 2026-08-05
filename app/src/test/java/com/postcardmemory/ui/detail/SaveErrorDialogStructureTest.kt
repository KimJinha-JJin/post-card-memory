package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 저장 에러(및 내보내기 성공) AlertDialog 7종의 현재 구조를 소스 텍스트 기준으로
 * 고정한다.
 *
 * 이 프로젝트는 Compose UI 테스트 인프라(androidx.compose.ui:ui-test)나
 * Robolectric을 쓰지 않는다(다른 *LogicTest.kt 상단 주석 참고 — DetailScreen/
 * DetailViewModel을 직접 인스턴스화할 수 없음). 따라서 다이얼로그를 실제로
 * 렌더링해 클릭 이벤트를 검증할 수 없다. 대신 DetailScreen.kt의 해당 구간을
 * 텍스트로 읽어 "다이얼로그 개수·순서·버튼 구성·dismiss와 confirm이 동일한
 * reset 함수를 호출하는지"라는 구조적 불변식을 고정한다.
 *
 * 제3차에서 이 다이얼로그들을 별도 파일로 옮길 때, 이동한 코드가 여전히 이
 * 텍스트 패턴을 만족하는지(또는 이 테스트를 의도적으로 갱신했는지)가
 * "동작 변경 없음"의 최소 증거가 된다.
 *
 * 문구(제목/본문) 전체 텍스트는 이 테스트로 고정하지 않는다 — 정확한 한글
 * 문구는 제2차 감사 보고서 부록에 스냅샷으로 기록했다. 여기서는 각 다이얼로그를
 * 구분 짓는 최소 표식(고정 안내문, .message 참조 등)만 검증해 사소한 줄바꿈
 * 변경에 테스트가 깨지지 않도록 한다.
 */
class SaveErrorDialogStructureTest {

    private object Source {
        private val ANCHORS_IN_ORDER = listOf(
            "exportSuccess" to "if (exportState is ExportState.Success)",
            "exportError" to "(exportState as? ExportState.Error)?.let",
            "backgroundError" to "as? BackgroundUpdateState.Error",
            "imageError" to "as? ImageUpdateState.Error",
            "fontError" to "as? FontUpdateState.Error",
            "layoutError" to "as? LayoutUpdateState.Error",
            "dateFormatError" to "as? DateFormatUpdateState.Error"
        )
        private const val TERMINAL_ANCHOR = "if (postcard != null && !isFocusPreviewMode)"

        val blocks: Map<String, String> by lazy { sliceBlocks() }

        private fun readDetailScreenSource(): String {
            val candidates = listOf(
                File("src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"),
                File("app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt")
            )
            val file = candidates.firstOrNull { it.exists() }
                ?: error(
                    "DetailScreen.kt를 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                        "정적 구조 테스트가 소스 파일 위치를 못 찾음 - candidates 경로를 갱신해야 함."
                )
            return file.readText()
        }

        // 7개 앵커 + 종료 앵커를 코드에 실제로 등장하는 순서대로 찾아 각 구간을
        // "이 앵커부터 다음 앵커 직전까지"로 잘라낸다. 앵커 하나라도 예상 순서로
        // 발견되지 않으면(다이얼로그가 삭제/이동/재배치됨) 여기서 즉시 실패한다.
        private fun sliceBlocks(): Map<String, String> {
            val source = readDetailScreenSource()
            var searchFrom = 0
            val starts = mutableListOf<Int>()
            for ((name, anchor) in ANCHORS_IN_ORDER) {
                val idx = source.indexOf(anchor, startIndex = searchFrom)
                check(idx >= 0) {
                    "앵커를 찾지 못함(다이얼로그가 삭제·이동·변형됐을 가능성): [$name] $anchor"
                }
                starts += idx
                searchFrom = idx + anchor.length
            }
            val terminalIdx = source.indexOf(TERMINAL_ANCHOR, startIndex = searchFrom)
            check(terminalIdx >= 0) { "종료 앵커를 찾지 못함: $TERMINAL_ANCHOR" }

            val ends = starts.drop(1) + terminalIdx
            return ANCHORS_IN_ORDER.map { it.first }
                .zip(starts.zip(ends).map { (s, e) -> source.substring(s, e) })
                .toMap()
        }
    }

    private fun assertSingleDialogWithConfirmOnly(label: String, block: String) {
        assertEquals(
            "[$label] AlertDialog 블록 수",
            1,
            Regex("AlertDialog\\(").findAll(block).count()
        )
        assertEquals(
            "[$label] confirmButton 블록 수",
            1,
            Regex("confirmButton = \\{").findAll(block).count()
        )
        assertEquals(
            "[$label] dismissButton(취소 버튼)은 없어야 함",
            0,
            Regex("dismissButton = \\{").findAll(block).count()
        )
        assertFalse(
            "[$label] DialogProperties 오버라이드가 없어야 함(기본 back/외부탭 dismiss 유지)",
            block.contains("properties =")
        )
    }

    private fun assertResetFunction(label: String, block: String, expected: String) {
        val onDismiss = Regex("onDismissRequest = \\{[\\s\\S]*?viewModel\\s*\\.\\s*(\\w+)\\(\\)")
            .find(block)?.groupValues?.get(1)
        assertEquals("[$label] onDismissRequest가 호출하는 함수", expected, onDismiss)

        val onConfirm = Regex("confirmButton = \\{[\\s\\S]*?viewModel\\s*\\.\\s*(\\w+)\\(\\)")
            .find(block)?.groupValues?.get(1)
        assertEquals("[$label] confirmButton이 호출하는 함수", expected, onConfirm)
    }

    private fun assertTitleColor(label: String, block: String, expectedColor: String) {
        val titleSection = block.substringBefore("text = {")
        assertTrue(
            "[$label] title 색상이 예상($expectedColor)과 다름",
            titleSection.contains("color = $expectedColor")
        )
    }

    @Test
    fun exactlySevenDialogsExistInThisSectionInExpectedOrder() {
        // Source.blocks 초기화 자체가 7개 앵커 + 종료 앵커를 정확히 이 순서로
        // 찾아야 성공한다(하나라도 순서가 어긋나거나 사라지면 위 sliceBlocks에서
        // 즉시 예외 발생). 이 테스트는 그 결과를 명시적으로 재확인한다.
        assertEquals(7, Source.blocks.size)
    }

    @Test
    fun exportSuccessDialog_confirmAndDismissBothResetExportState() {
        val block = Source.blocks.getValue("exportSuccess")
        assertSingleDialogWithConfirmOnly("exportSuccess", block)
        assertResetFunction("exportSuccess", block, "resetExportState")
        assertTitleColor("exportSuccess", block, "BrutalBlack")
        assertTrue(
            "[exportSuccess] 성공 안내 문구(고정 문자열)가 있어야 함",
            block.contains("Pictures/PostcardMemory")
        )
    }

    @Test
    fun exportErrorDialog_confirmAndDismissBothResetExportState() {
        val block = Source.blocks.getValue("exportError")
        assertSingleDialogWithConfirmOnly("exportError", block)
        assertResetFunction("exportError", block, "resetExportState")
        assertTitleColor("exportError", block, "BrutalCoral")
        assertTrue(
            "[exportError] 본문이 exportError.message를 그대로 노출해야 함",
            block.contains("text = exportError.message")
        )
    }

    @Test
    fun backgroundErrorDialog_confirmAndDismissBothResetBackgroundUpdateState() {
        val block = Source.blocks.getValue("backgroundError")
        assertSingleDialogWithConfirmOnly("backgroundError", block)
        assertResetFunction("backgroundError", block, "resetBackgroundUpdateState")
        assertTitleColor("backgroundError", block, "BrutalCoral")
        assertTrue(
            "[backgroundError] 본문이 backgroundError.message를 그대로 노출해야 함",
            block.contains("text = backgroundError.message")
        )
    }

    @Test
    fun imageErrorDialog_addsExistingPhotoKeptReassuranceUnlikeOtherFive() {
        val block = Source.blocks.getValue("imageError")
        assertSingleDialogWithConfirmOnly("imageError", block)
        assertResetFunction("imageError", block, "resetImageUpdateState")
        assertTitleColor("imageError", block, "BrutalCoral")
        // 6개 에러 다이얼로그 중 이것만 유일하게 메시지 앞에 고정 안내문을 붙인다
        // (제1차/제2차 조사에서 확인된 유일한 개별 예외).
        assertTrue(
            "[imageError] '기존 사진은 그대로 유지했어' 고정 안내문이 있어야 함",
            block.contains("기존 사진은 그대로 유지했어")
        )
        assertTrue(
            "[imageError] 안내문 뒤에 imageError.message가 이어붙어야 함",
            block.contains("imageError.message")
        )
    }

    @Test
    fun fontErrorDialog_confirmAndDismissBothResetFontUpdateState() {
        val block = Source.blocks.getValue("fontError")
        assertSingleDialogWithConfirmOnly("fontError", block)
        assertResetFunction("fontError", block, "resetFontUpdateState")
        assertTitleColor("fontError", block, "BrutalCoral")
        assertTrue(
            "[fontError] 본문이 fontError.message를 그대로 노출해야 함",
            block.contains("text = fontError.message")
        )
    }

    @Test
    fun layoutErrorDialog_confirmAndDismissBothResetLayoutUpdateState() {
        val block = Source.blocks.getValue("layoutError")
        assertSingleDialogWithConfirmOnly("layoutError", block)
        assertResetFunction("layoutError", block, "resetLayoutUpdateState")
        assertTitleColor("layoutError", block, "BrutalCoral")
        assertTrue(
            "[layoutError] 본문이 layoutError.message를 그대로 노출해야 함",
            block.contains("text = layoutError.message")
        )
    }

    @Test
    fun dateFormatErrorDialog_confirmAndDismissBothResetDateFormatUpdateState() {
        val block = Source.blocks.getValue("dateFormatError")
        assertSingleDialogWithConfirmOnly("dateFormatError", block)
        assertResetFunction("dateFormatError", block, "resetDateFormatUpdateState")
        assertTitleColor("dateFormatError", block, "BrutalCoral")
        assertTrue(
            "[dateFormatError] 본문이 dateFormatError.message를 그대로 노출해야 함",
            block.contains("text = dateFormatError.message")
        )
    }
}
