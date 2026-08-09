package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 제9차(2026-08-07)에서 `computeFallbackOverlaySize`를 DetailScreen.kt에서
 * OverlayFallbackSize.kt로 분리했다 — 이 대작업에서 첫 순수 계산 로직 분리다.
 *
 * 계산 결과 자체의 대표값·불변식·경계값 검증은 이미
 * `PostcardOverlayExportLogicTest.kt`(fallbackSize_matchesBasePxTimesScale,
 * fallbackSize_isAlwaysSquare, fallbackSize_neverZeroOrNegative)에 있고
 * 같은 패키지라 이번 이동으로 수정 없이 그대로 통과해야 한다. 이 파일은
 * 그 계산 결과가 아니라 "분리 경계" 자체 — 정의 위치, 시그니처, 순수성,
 * 호출부 유지 — 를 소스 텍스트 기준으로 고정한다([[SaveErrorDialogStructureTest]]
 * 상단 주석 참고 — 이 프로젝트는 Compose UI 테스트 인프라가 없다).
 *
 * 제3차의 "AlertDialog(" 부분 문자열 오탐을 반복하지 않기 위해 함수 선언
 * 검사는 줄 시작 앵커로 제한한다.
 */
class OverlayFallbackSizeStructureTest {

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
                "src/main/java/com/postcardmemory/ui/detail/OverlayFallbackSize.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/OverlayFallbackSize.kt"
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
    fun componentFile_declaresComputeFallbackOverlaySizeExactlyTwice() {
        // 43일차(마스킹테이프)에서 정사각형이 아닌 오버레이(가로·세로 기준
        // 크기가 다른 마스킹테이프)를 위한 오버로드를 추가해 1개 → 2개가 됐다.
        assertEquals(
            "OverlayFallbackSize.kt에 computeFallbackOverlaySize 선언이 정확히 2개(정사각형용 + 가로세로용) 있어야 함",
            2,
            Regex("""(?m)^internal fun computeFallbackOverlaySize\(""")
                .findAll(componentText)
                .count()
        )
    }

    @Test
    fun detailScreen_noLongerDeclaresComputeFallbackOverlaySize() {
        assertFalse(
            "DetailScreen.kt에 computeFallbackOverlaySize 정의가 남아 있으면 안 됨",
            Regex("""(?m)^(private |internal )?fun computeFallbackOverlaySize\(""")
                .containsMatchIn(detailScreenText)
        )
    }

    @Test
    fun componentFile_keepsExactSignatureAndTypes() {
        assertTrue(
            "basePx: Float 파라미터가 그대로 있어야 함",
            componentText.contains("basePx: Float")
        )
        assertTrue(
            "scale: Float 파라미터가 그대로 있어야 함",
            componentText.contains("scale: Float")
        )
        assertTrue(
            "반환 타입이 IntSize 그대로여야 함",
            componentText.contains("): IntSize {")
        )
    }

    @Test
    fun componentFile_isPureWithNoViewModelContextOrGestureState() {
        val forbiddenTokens = listOf(
            "ViewModel",
            "viewModel",
            "Repository",
            "Context",
            "pointerInput",
            "remember",
            "MutableState",
            "StateFlow",
            "Intent",
            "System.currentTimeMillis",
            "LocalDate",
            "Random"
        )
        for (token in forbiddenTokens) {
            assertFalse(
                "[$token] computeFallbackOverlaySize는 이 토큰을 참조하지 않아야 함",
                componentText.contains(token)
            )
        }
    }

    @Test
    fun detailScreen_hasExactlyThreeCallSitesUnchanged() {
        assertEquals(
            "DetailScreen.kt의 computeFallbackOverlaySize 호출은 정확히 3곳(스티커·도장·마스킹테이프 export)이어야 함",
            3,
            Regex("""computeFallbackOverlaySize\(""")
                .findAll(detailScreenText)
                .count()
        )
    }
}
