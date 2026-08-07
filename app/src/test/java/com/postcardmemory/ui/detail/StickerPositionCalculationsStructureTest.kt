package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 제10차(2026-08-07)에서 `centeredStickerOffset`을 DetailScreen.kt에서
 * StickerPositionCalculations.kt로 분리했다. 이 프로젝트는 Compose UI 테스트
 * 인프라를 쓰지 않으므로([[SaveErrorDialogStructureTest]] 상단 주석 참고),
 * 소스 텍스트 기준으로 다음을 고정한다:
 *  - 분리된 파일에만 함수 정의가 존재하고(DetailScreen.kt에는 남지 않음)
 *    ViewModel/Repository/Context/gesture/저장/Undo는 전혀 참조하지 않음
 *  - 기존 시그니처(파라미터·반환 타입)가 그대로 유지됨
 *  - DetailScreen.kt의 9개 호출부(제스처 핸들러 5곳 포함)가 전부 그대로 남아
 *    있음 — 이 계산은 gesture 코드와 물리적으로 분리됐을 뿐, 호출부의 gesture
 *    로직 자체는 이번에 전혀 이동하지 않았다
 *
 * 제3차의 "AlertDialog(" 부분 문자열 오탐을 반복하지 않기 위해 함수 선언
 * 검사는 줄 시작 앵커로 제한한다. "centeredStickerOffset"은 이 저장소 어디
 * 에도 다른 식별자의 접미사로 등장하지 않으므로 호출부 개수는 앵커 없이
 * 세어도 안전하다(사전에 grep으로 확인).
 */
class StickerPositionCalculationsStructureTest {

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
                "src/main/java/com/postcardmemory/ui/detail/StickerPositionCalculations.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/StickerPositionCalculations.kt"
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
    fun componentFile_declaresCenteredStickerOffsetExactlyOnce() {
        assertEquals(
            "StickerPositionCalculations.kt에 함수 선언이 정확히 1개 있어야 함",
            1,
            Regex("""(?m)^internal fun centeredStickerOffset\(""")
                .findAll(componentText)
                .count()
        )
    }

    @Test
    fun detailScreen_noLongerDeclaresCenteredStickerOffset() {
        assertFalse(
            "DetailScreen.kt에 centeredStickerOffset 정의가 남아 있으면 안 됨",
            Regex("""(?m)^(private |internal )?fun centeredStickerOffset\(""")
                .containsMatchIn(detailScreenText)
        )
    }

    @Test
    fun componentFile_keepsExactSignatureAndTypes() {
        assertTrue(
            "postcardSize: IntSize 파라미터가 그대로 있어야 함",
            componentText.contains("postcardSize: IntSize")
        )
        assertTrue(
            "stickerSize: IntSize 파라미터가 그대로 있어야 함",
            componentText.contains("stickerSize: IntSize")
        )
        assertTrue(
            "반환 타입이 Offset 그대로여야 함",
            componentText.contains("): Offset =")
        )
        assertTrue(
            "coerceAtLeast(0f) 클램프가 그대로 있어야 함(음수 미허용 동작 유지)",
            Regex("""coerceAtLeast\(0f\)""").findAll(componentText).count() == 2
        )
    }

    @Test
    fun componentFile_isPureWithNoViewModelContextGestureOrSaveLogic() {
        val forbiddenTokens = listOf(
            "ViewModel",
            "viewModel",
            "Repository",
            "Context",
            "pointerInput",
            "detectDragGestures",
            "detectTransformGestures",
            "CoroutineScope",
            "remember",
            "recordSealSnapshotForUndo",
            "recordStickerSnapshotForUndo",
            "setPhotoSeals",
            "setPhotoStickers"
        )
        for (token in forbiddenTokens) {
            assertFalse(
                "[$token] centeredStickerOffset는 이 토큰을 참조하지 않아야 함",
                componentText.contains(token)
            )
        }
    }

    @Test
    fun detailScreen_hasExactlyNineCallSitesUnchanged() {
        assertEquals(
            "DetailScreen.kt의 centeredStickerOffset 호출은 정확히 9곳이어야 함" +
                "(export 2 + LaunchedEffect 1 + 스티커/도장 제스처 5 + 복원 버튼 콜백 1)",
            9,
            Regex("""centeredStickerOffset\(""")
                .findAll(detailScreenText)
                .count()
        )
    }
}
