package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 제7차(2026-08-07)에서 `FutureMailSealedContent`를 DetailScreen.kt에서
 * FutureMailSealedContent.kt로 분리했다. 이 프로젝트는 Compose UI 테스트
 * 인프라를 쓰지 않으므로([[SaveErrorDialogStructureTest]] 상단 주석 참고),
 * 소스 텍스트 기준으로 다음을 고정한다:
 *  - 분리된 파일에는 이미 계산된 값(arrived/daysLeft/formattedDate)만 받는
 *    순수 렌더링 함수만 존재하고, 날짜 계산·상태 판단·ViewModel/Repository/
 *    Context는 전혀 참조하지 않음
 *  - 날짜 계산(`isFutureMailArrived`/`daysUntilFutureMail`/도착일 포맷)은
 *    DetailScreen.kt의 호출부에 그대로 남아 있고, 도착일 포맷 문자열
 *    ("yyyy년 M월 d일")도 변경되지 않음
 *  - DetailScreen.kt의 단일 호출부가 계산된 값을 그대로 전달함
 *
 * 제3차에서 "AlertDialog(" 부분 문자열이 "SaveResultAlertDialog(" 안에도
 * 걸려 개수를 잘못 세었던 오탐을 반복하지 않기 위해 함수 선언 검사는 줄 시작
 * 앵커로 제한하고, 호출부 경계는 괄호 깊이를 직접 스캔해 잘라낸다.
 */
class FutureMailSealedContentStructureTest {

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
                "src/main/java/com/postcardmemory/ui/detail/FutureMailSealedContent.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/FutureMailSealedContent.kt"
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
    fun componentFile_declaresFutureMailSealedContentExactlyOnce() {
        assertEquals(
            "FutureMailSealedContent.kt에 함수 선언이 정확히 1개 있어야 함",
            1,
            Regex("""(?m)^internal fun FutureMailSealedContent\(""")
                .findAll(componentText)
                .count()
        )
    }

    @Test
    fun detailScreen_noLongerDeclaresFutureMailSealedContent() {
        assertFalse(
            "DetailScreen.kt에 FutureMailSealedContent 정의가 남아 있으면 안 됨",
            Regex("""(?m)^(private |internal )?fun FutureMailSealedContent\(""")
                .containsMatchIn(detailScreenText)
        )
    }

    @Test
    fun componentFile_takesOnlyPrecomputedDisplayValues() {
        val expectedParams = listOf(
            "arrived: Boolean",
            "daysLeft: Long?",
            "formattedDate: String?",
            "onNavigateBack: () -> Unit"
        )
        for (param in expectedParams) {
            assertTrue(
                "[$param] 파라미터가 FutureMailSealedContent 선언에 있어야 함",
                componentText.contains(param)
            )
        }
        assertFalse(
            "deliverAtMillis(원시 타임스탬프)를 더 이상 직접 받지 않아야 함",
            componentText.contains("deliverAtMillis")
        )
    }

    @Test
    fun componentFile_doesNotComputeDatesOrReferenceViewModelRepositoryContext() {
        val forbiddenTokens = listOf(
            "DateTimeFormatter",
            "Locale",
            "isFutureMailArrived",
            "daysUntilFutureMail",
            "Instant",
            "ZoneId",
            "System.currentTimeMillis",
            "ViewModel",
            "viewModel",
            "Repository",
            "Context"
        )
        for (token in forbiddenTokens) {
            assertFalse(
                "[$token] FutureMailSealedContent는 이 토큰을 직접 참조하지 않아야 함",
                componentText.contains(token)
            )
        }
    }

    @Test
    fun detailScreen_stillOwnsDateComputationWithUnchangedFormat() {
        assertTrue(
            "도착 여부 계산(isFutureMailArrived)이 DetailScreen.kt에 남아 있어야 함",
            detailScreenText.contains("isFutureMailArrived(deliverAtMillis, now)")
        )
        assertTrue(
            "남은 일수 계산(daysUntilFutureMail)이 DetailScreen.kt에 남아 있어야 함",
            detailScreenText.contains("daysUntilFutureMail(it, now)")
        )
        assertTrue(
            "도착일 포맷 문자열(\"yyyy년 M월 d일\")이 변경되지 않아야 함",
            detailScreenText.contains("""ofPattern("yyyy년 M월 d일", Locale.KOREA)""")
        )
    }

    @Test
    fun detailScreen_hasExactlyOneCallSitePassingPrecomputedValues() {
        val callStarts = Regex("""(?m)^\s*FutureMailSealedContent\(""")
            .findAll(detailScreenText)
            .map { it.range.first + it.value.indexOf("FutureMailSealedContent") }
            .toList()

        assertEquals(
            "DetailScreen.kt의 FutureMailSealedContent 호출은 정확히 1곳이어야 함",
            1,
            callStarts.size
        )

        val block = extractBalancedCall(detailScreenText, callStarts.single())

        assertTrue("arrived = 전달", block.contains("arrived = arrived"))
        assertTrue("daysLeft = 전달", block.contains("daysLeft = daysLeft"))
        assertTrue("formattedDate = 전달", block.contains("formattedDate = formattedDate"))
        assertTrue(
            "onNavigateBack = 기존 콜백 그대로 전달",
            block.contains("onNavigateBack = navigateBackAfterPendingStyleSaves")
        )
    }
}
