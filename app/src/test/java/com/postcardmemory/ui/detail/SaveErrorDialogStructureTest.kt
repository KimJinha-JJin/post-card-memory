package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 저장 에러(및 내보내기 성공) 안내 다이얼로그 7종의 현재 구조를 소스 텍스트 기준으로
 * 고정한다.
 *
 * 이 프로젝트는 Compose UI 테스트 인프라(androidx.compose.ui:ui-test)나
 * Robolectric을 쓰지 않는다(다른 *LogicTest.kt 상단 주석 참고 — DetailScreen/
 * DetailViewModel을 직접 인스턴스화할 수 없음). 따라서 다이얼로그를 실제로
 * 렌더링해 클릭 이벤트를 검증할 수 없다. 대신 소스를 텍스트로 읽어 구조적
 * 불변식을 고정한다.
 *
 * 제3차(2026-08-07)에서 7개 다이얼로그의 AlertDialog UI를 공통 Composable
 * `SaveResultAlertDialog`(SaveResultAlertDialog.kt)로 추출했다. 그 결과 이
 * 테스트가 고정하는 지점이 두 층으로 나뉜다:
 *  - DetailScreen.kt: 7개 호출부가 여전히 올바른 상태 조건에서, 올바른 제목·
 *    제목색·본문·reset 콜백으로 SaveResultAlertDialog를 호출하는지
 *  - SaveResultAlertDialog.kt: 공통 Composable 자체가 dismiss와 confirm 모두
 *    같은 onAcknowledge를 부르고, dismissButton이 없고, ViewModel을 직접
 *    참조하지 않는지
 *
 * 문구(제목/본문) 전체 텍스트는 이 테스트로 고정하지 않는다 — 정확한 한글
 * 문구는 제2차 감사 보고서 부록에 스냅샷으로 기록했다. 여기서는 각 다이얼로그를
 * 구분 짓는 최소 표식(고정 안내문, .message 참조 등)만 검증해 사소한 줄바꿈
 * 변경에 테스트가 깨지지 않도록 한다.
 */
class SaveErrorDialogStructureTest {

    private object CallSiteSource {
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

        private fun readSource(): String {
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
            val source = readSource()
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

    private object ComponentSource {
        val text: String by lazy { read() }

        private fun read(): String {
            val candidates = listOf(
                File("src/main/java/com/postcardmemory/ui/detail/SaveResultAlertDialog.kt"),
                File("app/src/main/java/com/postcardmemory/ui/detail/SaveResultAlertDialog.kt")
            )
            val file = candidates.firstOrNull { it.exists() }
                ?: error(
                    "SaveResultAlertDialog.kt를 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                        "제3차에서 분리한 공통 Composable 파일 경로가 바뀌었을 수 있음."
                )
            return file.readText()
        }
    }

    private fun assertSingleCallSite(label: String, block: String) {
        assertEquals(
            "[$label] SaveResultAlertDialog 호출 수",
            1,
            Regex("SaveResultAlertDialog\\(").findAll(block).count()
        )
    }

    private fun assertOnAcknowledgeCallsReset(label: String, block: String, expected: String) {
        val called = Regex("onAcknowledge\\s*=\\s*\\{\\s*viewModel\\s*\\.\\s*(\\w+)\\(\\)")
            .find(block)?.groupValues?.get(1)
        assertEquals("[$label] onAcknowledge가 호출하는 reset 함수", expected, called)
    }

    private fun assertTitle(label: String, block: String, expectedTitle: String) {
        assertTrue(
            "[$label] title이 예상(\"$expectedTitle\")과 다름",
            block.contains("title = \"$expectedTitle\"")
        )
    }

    private fun assertTitleColor(label: String, block: String, expectedColor: String) {
        assertTrue(
            "[$label] titleColor가 예상($expectedColor)과 다름",
            block.contains("titleColor = $expectedColor")
        )
    }

    @Test
    fun exactlySevenDialogCallSitesExistInThisSectionInExpectedOrder() {
        // CallSiteSource.blocks 초기화 자체가 7개 앵커 + 종료 앵커를 정확히 이
        // 순서로 찾아야 성공한다(하나라도 순서가 어긋나거나 사라지면 위
        // sliceBlocks에서 즉시 예외 발생). 이 테스트는 그 결과를 명시적으로
        // 재확인한다.
        assertEquals(7, CallSiteSource.blocks.size)
    }

    @Test
    fun exportSuccessDialog_confirmAndDismissBothResetExportState() {
        val block = CallSiteSource.blocks.getValue("exportSuccess")
        assertSingleCallSite("exportSuccess", block)
        assertOnAcknowledgeCallsReset("exportSuccess", block, "resetExportState")
        assertTitle("exportSuccess", block, "저장 완료!")
        assertTitleColor("exportSuccess", block, "BrutalBlack")
        assertTrue(
            "[exportSuccess] 성공 안내 문구(고정 문자열)가 있어야 함",
            block.contains("Pictures/PostcardMemory")
        )
    }

    @Test
    fun exportErrorDialog_confirmAndDismissBothResetExportState() {
        val block = CallSiteSource.blocks.getValue("exportError")
        assertSingleCallSite("exportError", block)
        assertOnAcknowledgeCallsReset("exportError", block, "resetExportState")
        assertTitle("exportError", block, "저장하지 못했어")
        assertTitleColor("exportError", block, "BrutalCoral")
        assertTrue(
            "[exportError] 본문이 exportError.message를 그대로 노출해야 함",
            block.contains("body = exportError.message")
        )
    }

    @Test
    fun backgroundErrorDialog_confirmAndDismissBothResetBackgroundUpdateState() {
        val block = CallSiteSource.blocks.getValue("backgroundError")
        assertSingleCallSite("backgroundError", block)
        assertOnAcknowledgeCallsReset("backgroundError", block, "resetBackgroundUpdateState")
        assertTitle("backgroundError", block, "배경을 저장하지 못했어")
        assertTitleColor("backgroundError", block, "BrutalCoral")
        assertTrue(
            "[backgroundError] 본문이 backgroundError.message를 그대로 노출해야 함",
            block.contains("body = backgroundError.message")
        )
    }

    @Test
    fun imageErrorDialog_addsExistingPhotoKeptReassuranceUnlikeOtherFive() {
        val block = CallSiteSource.blocks.getValue("imageError")
        assertSingleCallSite("imageError", block)
        assertOnAcknowledgeCallsReset("imageError", block, "resetImageUpdateState")
        assertTitle("imageError", block, "사진을 바꾸지 못했어")
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
        val block = CallSiteSource.blocks.getValue("fontError")
        assertSingleCallSite("fontError", block)
        assertOnAcknowledgeCallsReset("fontError", block, "resetFontUpdateState")
        assertTitle("fontError", block, "폰트를 저장하지 못했어")
        assertTitleColor("fontError", block, "BrutalCoral")
        assertTrue(
            "[fontError] 본문이 fontError.message를 그대로 노출해야 함",
            block.contains("body = fontError.message")
        )
    }

    @Test
    fun layoutErrorDialog_confirmAndDismissBothResetLayoutUpdateState() {
        val block = CallSiteSource.blocks.getValue("layoutError")
        assertSingleCallSite("layoutError", block)
        assertOnAcknowledgeCallsReset("layoutError", block, "resetLayoutUpdateState")
        assertTitle("layoutError", block, "레이아웃을 저장하지 못했어")
        assertTitleColor("layoutError", block, "BrutalCoral")
        assertTrue(
            "[layoutError] 본문이 layoutError.message를 그대로 노출해야 함",
            block.contains("body = layoutError.message")
        )
    }

    @Test
    fun dateFormatErrorDialog_confirmAndDismissBothResetDateFormatUpdateState() {
        val block = CallSiteSource.blocks.getValue("dateFormatError")
        assertSingleCallSite("dateFormatError", block)
        assertOnAcknowledgeCallsReset("dateFormatError", block, "resetDateFormatUpdateState")
        assertTitle("dateFormatError", block, "날짜 형식을 저장하지 못했어")
        assertTitleColor("dateFormatError", block, "BrutalCoral")
        assertTrue(
            "[dateFormatError] 본문이 dateFormatError.message를 그대로 노출해야 함",
            block.contains("body = dateFormatError.message")
        )
    }

    @Test
    fun commonComposable_rendersExactlyOneAlertDialogWithoutDismissButton() {
        val text = ComponentSource.text
        assertEquals(
            "SaveResultAlertDialog가 렌더링하는 Material3 AlertDialog 수",
            1,
            // "AlertDialog\(" 단순 검색은 함수 선언 자체(`internal fun
            // SaveResultAlertDialog(`)의 "AlertDialog(" 부분 문자열까지
            // 걸려 2로 오탐한다. 앞에 식별자 문자가 없는(= 더 긴 이름의
            // 일부가 아닌) "AlertDialog(" 호출만 세도록 lookbehind로 제외한다.
            Regex("""(?<![A-Za-z])AlertDialog\(""").findAll(text).count()
        )
        assertFalse(
            "SaveResultAlertDialog에 dismissButton(취소 버튼)이 없어야 함",
            text.contains("dismissButton")
        )
        assertFalse(
            "SaveResultAlertDialog에 DialogProperties 오버라이드가 없어야 함(기본 back/외부탭 dismiss 유지)",
            text.contains("properties =")
        )
    }

    @Test
    fun commonComposable_dismissAndConfirmBothUseOnAcknowledge() {
        val text = ComponentSource.text
        assertTrue(
            "onDismissRequest가 onAcknowledge를 그대로 사용해야 함",
            text.contains("onDismissRequest = onAcknowledge")
        )
        val confirmBlock = text.substringAfter("confirmButton = {")
        assertTrue(
            "confirmButton의 TextButton도 onAcknowledge를 그대로 사용해야 함",
            confirmBlock.substringBefore("}").contains("onClick = onAcknowledge")
        )
    }

    @Test
    fun commonComposable_doesNotReferenceViewModelOrRepository() {
        val text = ComponentSource.text
        assertFalse(
            "SaveResultAlertDialog는 ViewModel을 직접 참조하지 않아야 함",
            text.contains("ViewModel") || text.contains("viewModel")
        )
        assertFalse(
            "SaveResultAlertDialog는 Repository를 직접 참조하지 않아야 함",
            text.contains("Repository")
        )
    }
}
