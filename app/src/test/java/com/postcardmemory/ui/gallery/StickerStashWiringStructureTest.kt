package com.postcardmemory.ui.gallery

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 110일차 스티커 통의 화면 연결. Compose UI JVM 테스트 인프라가 없어 사이드바 장면·뒤로가기·
 * 애니메이션 조건, 에디터의 꺼내기 경로, 엽서 삭제 경로와의 분리를 소스 구조로 고정한다.
 * 실제 화면 동작은 실기기 QA 대상이다.
 */
class StickerStashWiringStructureTest {

    private fun source(path: String): String =
        readStructureTestSource(listOf("src/main/java/com/postcardmemory/$path", "app/src/main/java/com/postcardmemory/$path"))

    private val drawer by lazy { source("ui/gallery/VisitCalendarDrawer.kt") }
    private val pouch by lazy { source("ui/gallery/StickerStashPouch.kt") }
    private val detail by lazy { source("ui/detail/DetailScreen.kt") }
    private val viewModel by lazy { source("ui/detail/DetailViewModel.kt") }

    @Test
    fun back_returnsFromStashToCalendarBeforeClosingDrawer() {
        val start = drawer.indexOf("BackHandler(enabled = drawerState.isOpen)")
        assertTrue(start >= 0)
        val body = drawer.substring(start, drawer.indexOf("ModalNavigationDrawer(", start))
        val stashBranch = body.indexOf("if (showStickerStash)")
        val closeBranch = body.indexOf("drawerState.close()")
        assertTrue("보관함에서 뒤로가기는 달력으로", stashBranch in 0 until closeBranch)
        assertTrue(body.contains("showStickerStash = false"))
    }

    @Test
    fun reopeningDrawer_startsAtCalendarAndRereadsStash() {
        val start = drawer.indexOf("LaunchedEffect(drawerState.isOpen)")
        val body = drawer.substring(start, drawer.indexOf("BackHandler(", start))
        assertTrue(body.contains("showStickerStash = false"))
        assertTrue(body.contains("StickerStashStorage.list(context.filesDir)"))
        assertTrue(body.contains("Dispatchers.IO"))
    }

    @Test
    fun calendarMonthSurvivesStashScene() {
        assertTrue(drawer.contains("rememberSaveableStateHolder()"))
        assertTrue(drawer.contains("sceneStateHolder.SaveableStateProvider(key = stash)"))
    }

    @Test
    fun pouchAnimatesOnlyWhileDrawerIsOpenAndMotionAllowed() {
        assertTrue(drawer.contains("animate = drawerState.isOpen && !reducedMotion"))
        assertTrue(pouch.contains("if (animate && visible.isNotEmpty()) {\n                AnimatedPouchPieces(visible)"))
        // 흔들림은 graphicsLayer 안에서만 읽는다.
        val pieces = pouch.substring(pouch.indexOf("private fun AnimatedPouchPieces"), pouch.indexOf("private fun PouchPiece("))
        assertTrue(pieces.contains("motion = Modifier.graphicsLayer {"))
    }

    @Test
    fun pouchPaper_isBakedOnceInsteadOfRedrawnEveryFrame() {
        listOf("private fun Modifier.stickerPouchBack(", "private fun Modifier.stickerPouchFront(").forEach { head ->
            val start = pouch.indexOf(head)
            val body = pouch.substring(start, pouch.indexOf("\n}\n", start))
            assertTrue(head, body.contains("val baked = bakePouchLayer {"))
            assertTrue(head, body.contains("onDrawBehind { drawImage(baked) }"))
        }
    }

    @Test
    fun stashMessages_showAboveTheBottomMenuNotOverThePostcard() {
        assertTrue(viewModel.contains("_stickerStashMessages.trySend("))
        val stash = viewModel.substring(viewModel.indexOf("fun stashPhotoSticker("), viewModel.indexOf("private fun stashExtensionFor("))
        assertFalse(stash.contains("_textScaleSaveErrors"))
        val host = detail.indexOf("hostState = stickerStashSnackbarHostState")
        assertTrue(host >= 0)
        val hostCall = detail.substring(host, detail.indexOf(")\n", detail.indexOf(".padding(bottom", host)) + 1)
        assertTrue(hostCall.contains(".align(Alignment.BottomCenter)"))
        assertTrue(hostCall.contains("editorDockHeightPx.toDp()"))
    }

    @Test
    fun stashRemoval_asksForConfirmation() {
        val page = pouch.substring(pouch.indexOf("internal fun StickerStashPage("), pouch.indexOf("private fun StashCell("))
        assertTrue(page.contains(".clickable { confirmRemove = selected }"))
        assertTrue(page.contains("AlertDialog("))
        val confirm = page.substring(page.indexOf("confirmButton"))
        assertTrue(confirm.contains("onRemove(target)"))
    }

    @Test
    fun editor_offersStashTabAndPicksIntoPhotoAim() {
        assertTrue(detail.contains("options = listOf(\"사진\", \"텍스트\", \"라벨\", \"스티커 통\")"))
        val start = detail.indexOf("StickerStashPickerPanel(")
        val call = detail.substring(start, detail.indexOf("} else {", start))
        val switchToPhoto = call.indexOf("stickerSubTabIndex = 0")
        val importCall = call.indexOf("viewModel.importStashedPhotoSticker(")
        assertTrue("조준은 사진 칸에서만 열리므로 먼저 사진 칸으로", switchToPhoto in 0 until importCall)
        assertTrue(call.contains("startStashedPhotoStickerAim("))
    }

    @Test
    fun placedStashedSticker_getsItsShapeAtContact() {
        val start = detail.indexOf("PhotoStickerTweezerHandOverlay(")
        val body = detail.substring(start, detail.indexOf("onFinished", start))
        assertTrue(body.contains("pendingStashedStickerShape?.applyTo(placed) ?: placed"))
    }

    @Test
    fun reuse_copiesIntoPostcardOwnedFilesAndNeverTouchesStashFiles() {
        val start = viewModel.indexOf("internal fun importStashedPhotoSticker(")
        val body = viewModel.substring(start, viewModel.indexOf("\n    }\n", start))
        assertTrue(body.contains("PhotoStickerImageStorage.copyToStickerOriginalStorage("))
        assertTrue(body.contains("copyStickerBackgroundToCache("))
        assertFalse(body.contains("StickerStashStorage"))
        assertFalse(body.contains(".delete()"))
    }

    @Test
    fun postcardDeletionAndOrphanScan_doNotKnowTheStash() {
        listOf(
            "utils/PostcardDeletionManager.kt",
            "utils/OrphanFileDiagnostics.kt",
            "utils/PostcardOwnedFileLayout.kt"
        ).forEach { path ->
            val text = source(path)
            assertFalse(path, text.contains("sticker_stash"))
            assertFalse(path, text.contains("StickerStashStorage"))
        }
    }
}
