package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 120일차: 완료 저장된 사진 스티커의 누끼·원본은 다음 완료 저장이 성공하기 전까지 정리되지
 * 않아야 한다.
 *
 * 판정은 [isStickerFileReferenced] 순수 함수로 검증한다. DetailViewModel은 Context/Uri에
 * 의존해 순수 JUnit에서 만들 수 없으므로(ConfirmSaveHistoryClearStructureTest 참고), undo 상한과
 * sweep 순서는 아래 [EditorHistory]가 recordStickerSnapshotForUndo/undo/redo/clearStickerHistory와
 * 같은 규칙으로 흉내 내고, ViewModel이 실제로 그 함수에 확정 기준선을 넘기는지는 소스 구조로
 * 고정한다. 실제 파일 삭제·화면 복원은 실기기 QA 몫이다.
 */
class ConfirmedStickerAssetProtectionTest {

    private data class Sticker(val id: String, val original: String, val cutout: String?)

    /** DetailViewModel의 스티커 이력·정리 후보 규칙을 그대로 옮긴 모형. */
    private class EditorHistory(confirmed: List<Sticker>) {
        var current: List<Sticker> = confirmed
        var baseline: List<Sticker> = confirmed
        private val undo = ArrayDeque<List<Sticker>>()
        private val redo = ArrayDeque<List<Sticker>>()
        private val candidates = mutableSetOf<String>()
        val deleted = mutableListOf<String>()

        fun isReferenced(uri: String): Boolean =
            isStickerFileReferenced(
                uri = uri,
                currentStickers = current,
                confirmedBaseline = baseline,
                historyStickers = undo.flatten() + redo.flatten(),
                fileUrisOf = { listOf(it.original, it.cutout) }
            )

        private fun sweep() {
            candidates.toList().forEach { uri ->
                if (!isReferenced(uri)) {
                    candidates.remove(uri)
                    deleted += uri
                }
            }
        }

        private fun record() {
            undo.addLast(current)
            if (undo.size > STICKER_HISTORY_LIMIT) {
                undo.removeFirst()
                sweep()
            }
            redo.clear()
        }

        /** DetailScreen.onDeleteSticker: 이력 기록 → 목록에서 빼기 → 누끼·원본 정리 요청. */
        fun delete(id: String) {
            val sticker = current.first { it.id == id }
            record()
            current = current.filter { it.id != id }
            listOfNotNull(sticker.cutout, sticker.original).forEach { uri ->
                if (isReferenced(uri)) candidates += uri else deleted += uri
            }
        }

        fun move(id: String) {
            record()
            current = current.map { if (it.id == id) it.copy() else it }
        }

        fun undo() {
            val previous = undo.removeLastOrNull() ?: return
            redo.addLast(current)
            current = previous
            sweep()
        }

        fun redo() {
            val next = redo.removeLastOrNull() ?: return
            undo.addLast(current)
            current = next
            sweep()
        }

        /** saveEditsAndClearDraft 성공: 기준선을 옮긴 뒤 clearStickerHistory(정리 포함). */
        fun confirmSaveSucceeded() {
            baseline = current
            undo.clear()
            redo.clear()
            sweep()
        }

        /** 저장 6종 중 하나라도 실패: 기준선·이력을 그대로 둔다. */
        fun confirmSaveFailed() = Unit
    }

    private val x = Sticker("x", "sticker_originals/1/x.jpg", "sticker_bgs/1/x.png")
    private val y = Sticker("y", "sticker_originals/1/y.jpg", "sticker_bgs/1/y.png")

    @Test
    fun deleteThenUndo_keepsConfirmedCutoutAndOriginal() {
        val editor = EditorHistory(listOf(x, y))

        editor.delete("x")
        editor.undo()

        assertTrue(editor.deleted.isEmpty())
        assertTrue(editor.isReferenced(x.cutout!!))
    }

    @Test
    fun deleteThenUndoHistoryOverflow_keepsConfirmedFilesUntilNextConfirmSave() {
        val editor = EditorHistory(listOf(x, y))

        editor.delete("x")
        repeat(STICKER_HISTORY_LIMIT + 1) { editor.move("y") }

        assertTrue("확정본이 가리키는 파일이 지워졌다: ${editor.deleted}", editor.deleted.isEmpty())
        assertTrue(editor.isReferenced(x.cutout!!))
        assertTrue(editor.isReferenced(x.original))
    }

    @Test
    fun historyOverflow_withoutConfirmedBaseline_wouldDeleteConfirmedFiles() {
        // 기준선을 빼면(119일차 이전 판정) 같은 조작에서 확정 파일이 지워진다 — 위 테스트가 실제 위험을 잡는지 확인.
        val editor = EditorHistory(listOf(x, y))
        editor.baseline = emptyList()

        editor.delete("x")
        repeat(STICKER_HISTORY_LIMIT + 1) { editor.move("y") }

        assertTrue(editor.deleted.containsAll(listOf(x.cutout!!, x.original)))
    }

    @Test
    fun leaveWithoutSave_thenRestoredDraftSession_stillProtectsConfirmedFiles() {
        // 다음 진입: 초안(x 없음)이 복원돼 이력은 비고, 확정 기준선만 x를 가리킨다.
        val editor = EditorHistory(listOf(x, y))
        editor.current = listOf(y)

        assertTrue(editor.isReferenced(x.cutout!!))
        assertTrue(editor.isReferenced(x.original))
    }

    @Test
    fun deleteThenConfirmSave_releasesDroppedFilesForCleanup() {
        val editor = EditorHistory(listOf(x, y))

        editor.delete("x")
        editor.confirmSaveSucceeded()

        assertTrue(editor.deleted.containsAll(listOf(x.cutout!!, x.original)))
        assertFalse(editor.deleted.contains(y.cutout))
        assertFalse(editor.deleted.contains(y.original))
    }

    @Test
    fun deleteThenFailedConfirmSave_keepsPreviousConfirmedFiles() {
        val editor = EditorHistory(listOf(x, y))

        editor.delete("x")
        repeat(STICKER_HISTORY_LIMIT + 1) { editor.move("y") }
        editor.confirmSaveFailed()

        assertTrue(editor.deleted.isEmpty())
    }

    @Test
    fun deleteRedoUndoCycle_keepsFilesWhileAnyStateUsesThem() {
        val editor = EditorHistory(listOf(x, y))

        editor.delete("x")
        editor.undo()
        editor.redo()

        assertTrue(editor.deleted.isEmpty())
    }

    @Test
    fun sharedOriginal_staysWhileAnotherStickerUsesIt() {
        val duplicate = Sticker("x2", x.original, "cache/photo_stickers/duplicated_x2.png")
        val editor = EditorHistory(listOf(x, duplicate))

        editor.delete("x")
        editor.confirmSaveSucceeded()

        assertTrue(editor.deleted.contains(x.cutout))
        assertFalse(editor.deleted.contains(x.original))

        editor.delete("x2")
        editor.confirmSaveSucceeded()

        assertTrue(editor.deleted.contains(x.original))
    }

    // ---- DetailViewModel이 위 판정에 확정 기준선을 실제로 넘기는지(소스 구조) ----

    private val source: String by lazy {
        listOf(
            "src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt",
            "app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"
        )
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("DetailViewModel.kt를 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    /** 시그니처부터 다음 멤버 선언 직전까지(식 본문 함수도 포함). */
    private fun declaration(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("$signature 를 찾지 못했다", start >= 0)
        val next =
            Regex("\n    (private |internal |override |suspend )*(fun|val|var) ")
                .find(source, start + signature.length)
                ?.range?.first
                ?: source.length
        return source.substring(start, next)
    }

    private fun assertOrdered(body: String, vararg markers: String) {
        var previous = -1
        markers.forEach { marker ->
            val index = body.indexOf(marker, previous + 1)
            assertTrue("[$marker]가 앞 단계 뒤에 없다", index > previous)
            previous = index
        }
    }

    @Test
    fun sweepReferenceCheck_includesConfirmedBaseline() {
        val body = declaration("private fun isStickerFileStillReferenced(")
        assertTrue(body.contains("isStickerFileReferenced("))
        assertTrue(body.contains("confirmedBaseline = confirmedStickersBaseline"))
        assertTrue(body.contains("stickerUndoStack.flatMap"))
        assertTrue(body.contains("stickerRedoStack.flatMap"))
    }

    @Test
    fun originalCleanupPaths_keepConfirmedBaselineOriginals() {
        assertTrue(
            declaration("fun deleteStickerOriginalIfUnreferenced(")
                .contains("confirmedStickersBaseline +")
        )
        assertTrue(
            declaration("private suspend fun awaitStickerCleanupSweep(")
                .contains("_photoStickers.value + confirmedStickersBaseline")
        )
    }

    @Test
    fun baseline_isFilledBeforeStickersAppear_andMovesOnlyAfterSuccessfulSave() {
        assertOrdered(
            declaration("fun loadStickerSealStateAndAutoRestoreDraft("),
            "confirmedStickersBaseline = confirmedStickers",
            "_photoStickers.value = confirmedStickers"
        )
        assertOrdered(
            declaration("fun saveEditsAndClearDraft("),
            "withContext(Dispatchers.Main) {",
            "if (allSaved) {",
            "confirmedStickersBaseline = _photoStickers.value",
            "clearStickerHistory()"
        )
    }
}
