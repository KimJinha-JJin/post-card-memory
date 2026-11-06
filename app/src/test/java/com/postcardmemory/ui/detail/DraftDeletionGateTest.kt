package com.postcardmemory.ui.detail

import com.postcardmemory.data.Postcard
import com.postcardmemory.testsupport.readStructureTestSource
import com.postcardmemory.utils.PostcardDeletionResult
import com.postcardmemory.utils.PostcardDraftStorage
import com.postcardmemory.utils.cleanupPostcardOwnedAssets
import com.postcardmemory.utils.deletePostcardDatabaseFirst
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 삭제와 초안 저장의 순서를 검증한다. 게이트, 초안 저장(saveDraftAtomically), DB-우선 삭제
 * gate, 파일 정리는 모두 production 코드이고 TemporaryFolder에서 실제 파일로 실행한다. DB 행
 * 삭제만 람다로 대신한다.
 *
 * 한계: ViewModel은 JVM에서 만들 수 없어(Robolectric 없음) 900ms 자동저장·ON_STOP의 실제
 * 타이밍과 ViewModel 연결은 실행하지 않는다. 연결은 파일 아래쪽 구조 검사로만 고정한다.
 */
class DraftDeletionGateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val filesDir: File get() = tempFolder.root
    private val gate = DraftDeletionGate(Mutex())

    private fun draftFile(id: Long = DELETED_ID) = PostcardDraftStorage.draftFile(filesDir, id)

    private fun draftBgDir(id: Long = DELETED_ID) =
        PostcardDraftStorage.draftStickerBackgroundDir(filesDir, id)

    /** persistDraftNow의 파일 순서와 같다: 누끼 폴더를 만들고 복사한 뒤 초안을 쓴다. */
    private fun writeDraftFiles(id: Long = DELETED_ID, revision: Long = 1L): Boolean {
        val dir = draftBgDir(id)
        dir.mkdirs()
        File(dir, "sticker.png").writeText("cutout")
        return PostcardDraftStorage.saveDraftAtomically(
            filesDir,
            PostcardEditDraft(
                postcardId = id,
                createdAtMillis = 1L,
                updatedAtMillis = 1L,
                revision = revision,
                stickers = emptyList(),
                selectedStickerId = null,
                seals = emptyList(),
                selectedSealId = null
            )
        )
    }

    private fun postcard(id: Long = DELETED_ID): Postcard {
        val image = File(filesDir, "postcard_images/$id.jpg").apply {
            parentFile?.mkdirs()
            writeText("image")
        }
        return Postcard(id = id, imagePath = image.path, title = "postcard-$id")
    }

    private suspend fun deleteForReal(target: Postcard): PostcardDeletionResult =
        deletePostcardDatabaseFirst(
            postcard = target,
            deleteDatabaseRow = {},
            cleanupOwnedAssets = { cleanupPostcardOwnedAssets(filesDir, it) }
        )

    private suspend fun failDatabaseDelete(target: Postcard): PostcardDeletionResult =
        deletePostcardDatabaseFirst(
            postcard = target,
            deleteDatabaseRow = { throw IOException("injected delete failure") },
            cleanupOwnedAssets = { cleanupPostcardOwnedAssets(filesDir, it) }
        )

    private suspend fun gatedDelete(
        delete: suspend () -> PostcardDeletionResult
    ): PostcardDeletionResult =
        gate.runDeletion(DELETED_ID, delete, { it.databaseDeleted })

    private suspend fun attemptWrite(id: Long = DELETED_ID, revision: Long = 1L): Boolean =
        gate.writeUnlessClosed(id, whenClosed = false) { writeDraftFiles(id, revision) }

    private fun assertNoDraftFiles(id: Long = DELETED_ID) {
        assertFalse("초안 파일이 남으면 안 됨", draftFile(id).exists())
        assertFalse("초안 누끼 폴더가 남으면 안 됨", draftBgDir(id).exists())
    }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runBlocking {
        withTimeout(TIMEOUT_MS, block)
    }

    // ---- 위험 재현: 게이트가 없으면 저장소에는 삭제 후 쓰기를 막는 장치가 없다 ----

    @Test
    fun withoutGate_lateDraftWriteRecreatesDeletedPostcardsFiles() = test {
        val target = postcard()
        writeDraftFiles()

        assertTrue(deleteForReal(target).databaseDeleted)
        assertNoDraftFiles()

        assertTrue(writeDraftFiles())
        assertTrue("게이트 없는 늦은 쓰기는 초안을 되살린다(위험 기록)", draftFile().exists())
        assertTrue(draftBgDir().exists())
    }

    // ---- 삭제 성공 경로 ----

    @Test
    fun deleteWithNoUnsavedEdits_leavesNoDraftFiles() = test {
        val target = postcard()

        gate.close(DELETED_ID)
        val result = gatedDelete { deleteForReal(target) }

        assertTrue(result.databaseDeleted)
        assertNoDraftFiles()
    }

    @Test
    fun deleteRightAfterAutosave_removesTheDraftAlreadyWritten() = test {
        val target = postcard()
        assertTrue(attemptWrite())
        assertTrue(draftFile().exists())

        gate.close(DELETED_ID)
        gatedDelete { deleteForReal(target) }

        assertNoDraftFiles()
    }

    @Test
    fun pendingAutosaveFiringBeforeDeletionTakesLock_writesNothing() = test {
        val target = postcard()

        gate.close(DELETED_ID)
        assertFalse("닫힌 뒤 도착한 저장은 쓰지 않음", attemptWrite())
        assertNoDraftFiles()

        gatedDelete { deleteForReal(target) }
        assertNoDraftFiles()
    }

    @Test
    fun saveAlreadyRunning_finishesFirst_thenDeletionRemovesItsFiles() = test {
        val target = postcard()
        val writeStarted = CompletableDeferred<Unit>()
        val releaseWrite = CompletableDeferred<Unit>()
        var deleteStarted = false

        val writer = launch {
            gate.writeUnlessClosed(DELETED_ID, whenClosed = false) {
                writeStarted.complete(Unit)
                releaseWrite.await()
                writeDraftFiles()
            }
        }
        writeStarted.await()

        gate.close(DELETED_ID)
        val deleter = launch {
            gatedDelete {
                deleteStarted = true
                deleteForReal(target)
            }
        }
        repeat(5) { yield() }
        assertFalse("실행 중인 저장이 끝나기 전에 삭제가 시작되면 안 됨", deleteStarted)

        releaseWrite.complete(Unit)
        writer.join()
        deleter.join()

        assertTrue(deleteStarted)
        assertNoDraftFiles()
    }

    @Test
    fun saveArrivingDuringDeletion_waitsThenWritesNothing() = test {
        val target = postcard()
        val deleteEntered = CompletableDeferred<Unit>()
        val releaseDelete = CompletableDeferred<Unit>()
        var writeResult: Boolean? = null

        gate.close(DELETED_ID)
        val deleter = launch {
            gatedDelete {
                deleteEntered.complete(Unit)
                releaseDelete.await()
                deleteForReal(target)
            }
        }
        deleteEntered.await()

        val writer = launch { writeResult = attemptWrite() }
        repeat(5) { yield() }
        assertNull("삭제가 끝날 때까지 저장은 기다림", writeResult)

        releaseDelete.complete(Unit)
        deleter.join()
        writer.join()

        assertEquals(false, writeResult)
        assertNoDraftFiles()
    }

    @Test
    fun flushAfterDeletionFinished_writesNothing_noFolderEither() = test {
        val target = postcard()
        writeDraftFiles()

        gate.close(DELETED_ID)
        gatedDelete { deleteForReal(target) }

        assertFalse(attemptWrite())
        assertFalse(attemptWrite(revision = 2L))
        assertNoDraftFiles()
    }

    // ---- 삭제 실패·취소: 살아 있는 엽서의 초안을 잃으면 안 된다 ----

    @Test
    fun databaseFailure_keepsExistingDraft_andReopensForLaterSaves() = test {
        val target = postcard()
        writeDraftFiles(revision = 3L)

        gate.close(DELETED_ID)
        val result = gatedDelete { failDatabaseDelete(target) }

        assertFalse(result.databaseDeleted)
        assertEquals(3L, PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision)

        assertTrue("삭제 실패 뒤에는 다시 저장돼야 함", attemptWrite(revision = 4L))
        assertEquals(4L, PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision)
    }

    @Test
    fun saveWaitingDuringFailedDeletion_writesAfterReopen() = test {
        val target = postcard()
        val deleteEntered = CompletableDeferred<Unit>()
        val releaseDelete = CompletableDeferred<Unit>()
        var writeResult: Boolean? = null

        gate.close(DELETED_ID)
        val deleter = launch {
            gatedDelete {
                deleteEntered.complete(Unit)
                releaseDelete.await()
                failDatabaseDelete(target)
            }
        }
        deleteEntered.await()
        val writer = launch { writeResult = attemptWrite(revision = 9L) }
        repeat(5) { yield() }

        releaseDelete.complete(Unit)
        deleter.join()
        writer.join()

        assertEquals(true, writeResult)
        assertEquals(9L, PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision)
    }

    @Test
    fun cancelledDeletion_staysClosed_releasesLock_keepsFiles() = test {
        val target = postcard()
        writeDraftFiles()
        val deleteEntered = CompletableDeferred<Unit>()

        gate.close(DELETED_ID)
        val deleter = launch(start = CoroutineStart.UNDISPATCHED) {
            gatedDelete {
                deleteEntered.complete(Unit)
                CompletableDeferred<Unit>().await()
                deleteForReal(target)
            }
        }
        deleteEntered.await()
        deleter.cancel()
        deleter.join()

        assertTrue("취소는 파일을 지우지 않음", draftFile().exists())
        assertFalse("행이 지워졌는지 모르니 닫아 둔다", attemptWrite(revision = 2L))
        assertEquals(1L, PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision)
        assertTrue("잠금이 풀려 다른 엽서는 저장됨", attemptWrite(id = OTHER_ID))
    }

    @Test
    fun unexpectedDeletionError_isRethrown_staysClosed_releasesLock() = test {
        gate.close(DELETED_ID)

        try {
            gatedDelete { throw IllegalStateException("boom") }
            fail("예상 밖 오류를 삼키면 안 된다")
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: IllegalStateException) {
            assertEquals("boom", error.message)
        }

        assertFalse(attemptWrite())
        assertNoDraftFiles()
        assertTrue(attemptWrite(id = OTHER_ID))
    }

    // ---- 정상 저장 회귀: 삭제와 무관한 저장은 그대로다 ----

    @Test
    fun withoutDeletion_consecutiveSavesAllWrite_andLatestIsRestored() = test {
        assertTrue(attemptWrite(revision = 1L))
        assertTrue(attemptWrite(revision = 2L))
        assertTrue(attemptWrite(revision = 3L))

        assertEquals(3L, PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision)
    }

    @Test
    fun otherPostcardsDraft_isSavedAndRestored_acrossDeletion() = test {
        val target = postcard()
        writeDraftFiles(id = OTHER_ID, revision = 5L)

        gate.close(DELETED_ID)
        assertTrue("다른 엽서 저장은 막히지 않음", attemptWrite(id = OTHER_ID, revision = 6L))

        gatedDelete { deleteForReal(target) }

        assertNoDraftFiles(DELETED_ID)
        val restored = PostcardDraftStorage.loadDraft(filesDir, OTHER_ID)
        assertNotNull(restored)
        assertEquals(6L, restored?.revision)
        assertTrue(File(draftBgDir(OTHER_ID), "sticker.png").exists())
        assertTrue("삭제 뒤에도 저장됨", attemptWrite(id = OTHER_ID, revision = 7L))
    }

    // ---- ViewModel 연결(구조 검사: 소스 텍스트만 본다) ----

    private val viewModelText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"
            )
        )
    }

    private fun functionBody(signature: String): String {
        val start = viewModelText.indexOf(signature)
        assertTrue("$signature 를 찾지 못함", start >= 0)
        val bodyStart = viewModelText.indexOf('{', start)
        var depth = 0
        for (i in bodyStart until viewModelText.length) {
            when (viewModelText[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return viewModelText.substring(bodyStart, i + 1)
                }
            }
        }
        error("$signature 본문의 끝을 찾지 못함")
    }

    @Test
    fun structure_gateSharesTheDraftSaveMutex() {
        assertTrue(viewModelText.contains("DraftDeletionGate(draftSaveMutex)"))
    }

    @Test
    fun structure_persistDraftNow_checksGateBeforeTouchingAnyFile() {
        val body = functionBody("private suspend fun persistDraftNow()")
        val gateIndex = body.indexOf("draftDeletionGate.writeUnlessClosed(")

        assertTrue("저장이 게이트를 지나야 함", gateIndex >= 0)
        assertFalse("게이트를 우회한 직접 잠금이 있으면 안 됨", body.contains("draftSaveMutex.withLock"))
        assertTrue(
            "누끼 폴더 생성이 게이트 안에 있어야 함",
            body.indexOf("promoteDraftStickerBackgrounds(") > gateIndex
        )
        assertTrue(
            "초안 쓰기가 게이트 안에 있어야 함",
            body.indexOf("PostcardDraftStorage.saveDraftAtomically(") > gateIndex
        )
    }

    @Test
    fun structure_deletePostcard_closesBeforeDeleting_andRestoresAutosaveOnlyOnFailure() {
        val body = functionBody("fun deletePostcard()")
        val close = body.indexOf("draftDeletionGate.close(currentPostcard.id)")
        val cancel = body.indexOf("draftAutosaveJob?.cancel()")
        val launch = body.indexOf("postcardDeleteJob = viewModelScope.launch")
        val run = body.indexOf("draftDeletionGate.runDeletion(")
        val manager = body.indexOf("deletionManager.deletePostcard(currentPostcard)")
        val failure = body.indexOf("if (!result.databaseDeleted)")
        val reschedule = body.indexOf("scheduleDraftAutosave()")
        val failureEnd = body.indexOf("return@launch")

        assertTrue("닫기가 삭제 시작보다 먼저", close in 0 until launch)
        assertTrue("예약 취소가 삭제 시작보다 먼저", cancel in 0 until launch)
        assertTrue("삭제가 게이트 안에서 실행됨", run in (launch + 1) until manager)
        assertTrue("다시 예약은 실패 분기 안에서만", reschedule in (failure + 1) until failureEnd)
        assertTrue(
            "디스크에 없는 편집이 있을 때만 다시 예약",
            body.indexOf("hasUnpersistedDraftEdits(") in (failure + 1) until reschedule
        )
    }

    private companion object {
        const val DELETED_ID = 7L
        const val OTHER_ID = 8L
        const val TIMEOUT_MS = 10_000L
    }
}
