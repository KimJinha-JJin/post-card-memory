package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 131일차: 텍스트·라벨·테이프 손맛이 "그리기 전용"으로 남는지 소스 기준으로 고정한다
 * ([PhotoStickerLiftStructureTest]와 같은 방식 — JVM에선 Composable을 렌더링할 수 없음).
 * - 들림 변환이 graphicsLayer가 되거나 회전 layer 안쪽에 걸리면 터치 좌표·들림 방향이 어긋난다.
 * - 추적기가 이벤트를 소비하면 기존 탭·이동·Undo 기록 시작점이 바뀐다.
 * - 상태가 ViewModel·저장·내보내기에 닿으면 저장 결과에 섞일 수 있다.
 */
class DecorationLiftStructureTest {

    private fun read(path: String): String {
        val file = listOf("src/main/java/com/postcardmemory/$path", "app/src/main/java/com/postcardmemory/$path")
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error("소스 파일을 찾을 수 없음: $path (cwd=${File(".").absolutePath})")
        return file.readText()
    }

    private val liftSource by lazy { read("ui/detail/DecorationLiftFeel.kt") }
    private val detail by lazy { read("ui/detail/DetailScreen.kt") }

    private fun section(source: String, startMarker: String, endMarker: String): String {
        val start = source.indexOf(startMarker)
        val end = source.indexOf(endMarker, start + 1)
        assertTrue("$startMarker 구간을 찾지 못함", start >= 0 && end > start)
        return source.substring(start, end)
    }

    @Test
    fun liftTransformAndShadow_areDrawOnly() {
        val transform = section(liftSource, "fun Modifier.decorationLiftTransform(", "fun Modifier.decorationLiftShadow(")
        assertTrue(transform.contains("drawWithContent"))
        assertFalse(
            "들림 변환에 graphicsLayer를 쓰면 터치 좌표가 바뀜",
            Regex("""graphicsLayer\s*[({]""").containsMatchIn(transform)
        )
        val shadow = section(liftSource, "fun Modifier.decorationLiftShadow(", "internal data class DecorationLiftVisual")
        assertTrue(shadow.contains("drawWithContent"))
        assertFalse(shadow.contains("Modifier.graphicsLayer"))
        assertFalse("그림자가 레이아웃 크기를 바꾸면 안 됨", Regex("""\.(size|padding|offset)\(""").containsMatchIn(shadow))
        assertTrue("쉬는 상태는 기존과 똑같이 그려야 함", shadow.contains("if (!state.isLifted)"))
    }

    @Test
    fun liftTracker_onlyObserves_neverConsumes_andAlwaysSettles() {
        val body = section(liftSource, "suspend fun PointerInputScope.trackDecorationLift(", "fun Modifier.decorationLiftTransform(")
        assertTrue(body.contains("PointerEventPass.Initial"))
        assertTrue(body.contains("requireUnconsumed = false"))
        assertFalse("기존 탭·이동 제스처를 막으면 안 됨", Regex("""\.consume\w*\(""").containsMatchIn(body))
        assertTrue("취소돼도 항상 내려앉아야 함", body.contains("finally") && body.contains("state.onRelease()"))
    }

    @Test
    fun liftFile_doesNotWriteEditorOrSaveState() {
        for (forbidden in listOf(
            "viewModel", "setTextStickers", "setLabelStickers", "setPhotoMaskingTapes",
            "SnapshotForUndo", "Repository", "Exporter", "MaskingTapePhotoDecoder"
        )) {
            assertFalse("손맛 효과 파일이 $forbidden 를 건드리면 안 됨", liftSource.contains(forbidden))
        }
    }

    @Test
    fun photoStickerLift_isLeftAlone() {
        // 130일차에 실기기 검증이 끝난 사진 스티커 손맛은 새 helper로 갈아끼우지 않는다.
        assertTrue(detail.contains(".photoStickerLiftTransform(stickerLift)"))
        assertTrue(detail.contains("trackPhotoStickerLift(stickerLift)"))
        assertFalse(read("ui/detail/PhotoStickerLiftFeel.kt").contains("DecorationLift"))
    }

    @Test
    fun exporterAndRenderersAndModels_doNotKnowAboutLift() {
        for (path in listOf(
            "utils/PostcardImageExporter.kt",
            "utils/LabelStickerRenderer.kt",
            "ui/components/TextStickerContent.kt",
            "ui/components/LabelStickerContent.kt",
            "ui/components/MaskingTapeShapes.kt",
            "ui/detail/MaskingTapeItem.kt",
            "ui/detail/MaskingTapePlaceInteraction.kt",
            "ui/detail/DetailViewModel.kt"
        )) {
            assertFalse("$path 에 손맛 효과가 섞이면 안 됨", read(path).contains("DecorationLift"))
        }
    }

    @Test
    fun detailScreen_wiresEachMaterial_liftOutsideRotation_shadowOnContent() {
        val cases = listOf(
            Triple("tapeLift", "MaskingTapeLiftSpec", "maskingTapePositionModifier"),
            Triple("textStickerLift", "TextStickerLiftSpec", "textStickerPositionModifier"),
            Triple("labelStickerLift", "LabelStickerLiftSpec", "labelStickerPositionModifier")
        )
        for ((state, spec, position) in cases) {
            assertTrue(detail.contains("rememberDecorationLiftState(") && detail.contains(spec))
            assertTrue("$state 추적기 연결", detail.contains("trackDecorationLift($state)"))
            assertTrue("$state 그림자 연결", Regex("""decorationLiftShadow\(\s*$state,""").containsMatchIn(detail))
            // 들림 변환은 같은 Box의 회전 graphicsLayer보다 앞(바깥)에 있어야 화면 기준으로 들린다.
            val boxStart = detail.indexOf("modifier = $position")
            assertTrue("$position Box를 찾지 못함", boxStart >= 0)
            val transformAt = detail.indexOf(".decorationLiftTransform($state)", boxStart)
            val rotationAt = detail.indexOf(".graphicsLayer {", boxStart)
            assertTrue("$state 변환이 회전보다 앞이어야 함", transformAt in boxStart until rotationAt)
            assertEquals(1, Regex("""\.decorationLiftTransform\($state\)""").findAll(detail).count())
        }
    }
}
