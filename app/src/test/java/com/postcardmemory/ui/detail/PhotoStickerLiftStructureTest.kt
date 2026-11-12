package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 130일차 B: 스티커 들림·갤러리 눌림이 "그리기 전용"으로 남는지 소스 기준으로 고정한다.
 * (JVM 환경엔 Robolectric이 없어 Composable을 렌더링할 수 없다 — 다른 구조 테스트와 같은 방식.)
 * 들림 변환이 graphicsLayer로 바뀌면 제스처 좌표가 1.05배·기울기만큼 어긋나고,
 * 상태를 ViewModel에 쓰면 저장·Undo·내보내기에 섞일 수 있다.
 */
class PhotoStickerLiftStructureTest {

    private fun read(path: String): String {
        val file = listOf("src/main/java/com/postcardmemory/$path", "app/src/main/java/com/postcardmemory/$path")
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error("소스 파일을 찾을 수 없음: $path (cwd=${File(".").absolutePath})")
        return file.readText()
    }

    private val liftSource by lazy { read("ui/detail/PhotoStickerLiftFeel.kt") }
    private val gallerySource by lazy { read("ui/gallery/GalleryCardPressFeel.kt") }

    @Test
    fun liftTransform_isDrawOnly_soGestureCoordinatesStayTheSame() {
        val start = liftSource.indexOf("fun Modifier.photoStickerLiftTransform(")
        val end = liftSource.indexOf("fun PhotoStickerLiftShadow(", start)
        assertTrue("photoStickerLiftTransform을 찾지 못함", start >= 0 && end > start)
        val body = liftSource.substring(start, end)
        assertTrue("들림은 drawWithContent 변환이어야 함", body.contains("drawWithContent"))
        assertFalse("들림 변환에 graphicsLayer를 쓰면 터치 좌표가 바뀜", body.contains("graphicsLayer"))
    }

    @Test
    fun liftTracker_onlyObserves_neverConsumes() {
        val start = liftSource.indexOf("suspend fun PointerInputScope.trackPhotoStickerLift(")
        val end = liftSource.indexOf("fun Modifier.photoStickerLiftTransform(", start)
        assertTrue("trackPhotoStickerLift를 찾지 못함", start >= 0 && end > start)
        val body = liftSource.substring(start, end)
        assertTrue(body.contains("PointerEventPass.Initial"))
        assertFalse("기존 탭·이동 제스처를 막으면 안 됨", Regex("""\.consume\w*\(""").containsMatchIn(body))
        assertTrue("취소돼도 항상 내려앉아야 함", body.contains("finally") && body.contains("state.onRelease()"))
    }

    @Test
    fun touchFeelFiles_doNotWriteEditorOrSaveState() {
        for (source in listOf(liftSource, gallerySource)) {
            for (forbidden in listOf("viewModel", "setPhotoStickers", "recordStickerSnapshotForUndo", "Repository", "Exporter")) {
                assertFalse("손맛 효과 파일이 $forbidden 를 건드리면 안 됨", source.contains(forbidden))
            }
        }
    }

    @Test
    fun exporterAndSavedModel_doNotKnowAboutLift() {
        val exporter = read("utils/PostcardImageExporter.kt")
        val item = read("ui/detail/PhotoStickerItem.kt")
        for (source in listOf(exporter, item)) {
            assertFalse(source.contains("PhotoStickerLift"))
            assertFalse(source.contains("galleryCardPress"))
        }
    }

    @Test
    fun detailScreen_wiresLiftAroundExistingStickerBox() {
        val detail = read("ui/detail/DetailScreen.kt")
        assertTrue(detail.contains(".photoStickerLiftTransform(stickerLift)"))
        assertTrue(detail.contains("trackPhotoStickerLift(stickerLift)"))
        assertTrue(detail.contains("PhotoStickerLiftShadow("))
    }
}
