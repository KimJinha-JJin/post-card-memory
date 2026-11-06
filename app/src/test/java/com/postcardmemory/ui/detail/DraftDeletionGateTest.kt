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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    // ---- 삭제 취소 회귀: 화면(ViewModel)이 닫혀도 DB 행과 초안이 어긋나면 안 된다 ----

    /** 화면 범위와 앱 범위를 분리해 흉내낸다. 실제 ViewModel과 Android 생명주기는 아니다. */
    private inner class DeletionScenario(
        private val failDatabase: Boolean = false
    ) {
        val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val target = postcard()
        var rowExists = true
        var deleteAttempts = 0
        var recoverCalls = 0
        var outcome: PostcardDeletionOutcome? = null

        suspend fun delete(): PostcardDeletionResult =
            deletePostcardDatabaseFirst(
                postcard = target,
                deleteDatabaseRow = {
                    deleteAttempts++
                    if (failDatabase) throw IOException("injected delete failure")
                    rowExists = false
                },
                cleanupOwnedAssets = { cleanupPostcardOwnedAssets(filesDir, it) }
            )

        /** 예약이 취소돼 디스크에 없던 마지막 편집(UNSAVED_REVISION)을 저장한다. */
        suspend fun recoverLastEdit() {
            recoverCalls++
            attemptWrite(revision = UNSAVED_REVISION)
        }

        fun launchDeletionIn(scope: CoroutineScope): Job =
            scope.launch {
                outcome =
                    gate.deleteThenRecoverDraft(
                        postcardId = DELETED_ID,
                        delete = { delete() },
                        recoverDraft = { recoverLastEdit() }
                    )
            }

        /** 앞선 저장이 잠금을 쥔 동안 삭제를 시작해, 화면이 닫힐 때 삭제가 잠금을 기다리게 한다. */
        suspend fun closeScreenWhileDeletionWaitsForLock(scope: CoroutineScope): Job {
            val lockHeld = CompletableDeferred<Unit>()
            val releaseLock = CompletableDeferred<Unit>()
            val earlierSave = appScope.launch {
                gate.writeUnlessClosed(DELETED_ID, whenClosed = false) {
                    lockHeld.complete(Unit)
                    releaseLock.await()
                    true
                }
            }
            lockHeld.await()

            gate.close(DELETED_ID)
            val deletion = launchDeletionIn(scope)
            repeat(5) { yield() }
            viewModelScope.cancel()

            releaseLock.complete(Unit)
            earlierSave.join()
            return deletion
        }
    }

    @Test
    fun oldArrangement_deletionInViewModelScope_keepsRow_butLosesLastEdit() = test {
        // 회귀 재현: 삭제를 화면 범위에서 실행하던 907fd2f의 구조.
        writeDraftFiles(revision = SAVED_REVISION)
        val scenario = DeletionScenario()

        scenario.closeScreenWhileDeletionWaitsForLock(scenario.viewModelScope).join()

        assertEquals("삭제가 실행되지 않음", 0, scenario.deleteAttempts)
        assertTrue("엽서는 남음", scenario.rowExists)
        assertEquals(0, scenario.recoverCalls)
        assertEquals(
            "마지막 편집이 디스크에 없음(위험 기록)",
            SAVED_REVISION,
            PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision
        )
    }

    @Test
    fun screenClosedBeforeDatabaseWrite_deletionStillFinishes_withNoDraftLeft() = test {
        writeDraftFiles(revision = SAVED_REVISION)
        val scenario = DeletionScenario()

        scenario.closeScreenWhileDeletionWaitsForLock(scenario.appScope).join()

        assertFalse("행이 지워짐", scenario.rowExists)
        assertEquals(0, scenario.recoverCalls)
        assertTrue(scenario.outcome is PostcardDeletionOutcome.RowDeleted)
        assertNoDraftFiles()
        assertFalse("삭제된 엽서에 늦은 저장은 쓰지 않음", attemptWrite(revision = 99L))
        assertNoDraftFiles()
    }

    @Test
    fun screenClosedThenDatabaseFails_lastEditIsRecovered_withRowKept() = test {
        writeDraftFiles(revision = SAVED_REVISION)
        val scenario = DeletionScenario(failDatabase = true)

        scenario.closeScreenWhileDeletionWaitsForLock(scenario.appScope).join()

        assertEquals(1, scenario.deleteAttempts)
        assertTrue("엽서는 남음", scenario.rowExists)
        assertEquals(1, scenario.recoverCalls)
        assertEquals(
            "화면이 닫혔어도 마지막 편집이 남음",
            UNSAVED_REVISION,
            PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision
        )
        assertEquals(PostcardDeletionOutcome.RowKept(null), scenario.outcome)
    }

    @Test
    fun rowDeleted_neverRunsRecovery_evenWithUnsavedEdits() = test {
        val scenario = DeletionScenario()

        gate.close(DELETED_ID)
        scenario.launchDeletionIn(this).join()

        assertEquals(0, scenario.recoverCalls)
        assertNoDraftFiles()
    }

    @Test
    fun unexpectedErrorAfterRowDelete_doesNotRewriteDraft_staysClosed() = test {
        val scenario = DeletionScenario()
        writeDraftFiles(revision = SAVED_REVISION)
        var recoverCalls = 0

        gate.close(DELETED_ID)
        val outcome =
            gate.deleteThenRecoverDraft(
                postcardId = DELETED_ID,
                delete = {
                    deletePostcardDatabaseFirst(
                        postcard = scenario.target,
                        deleteDatabaseRow = {},
                        cleanupOwnedAssets = { throw IOException("cleanup threw") }
                    )
                },
                recoverDraft = { recoverCalls++ }
            )

        assertTrue(outcome is PostcardDeletionOutcome.Uncertain)
        assertEquals("불확실하면 복구하지 않음", 0, recoverCalls)
        assertFalse("불확실하면 문을 닫아 둠", attemptWrite(revision = UNSAVED_REVISION))
        assertEquals(
            SAVED_REVISION,
            PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision
        )
    }

    @Test
    fun retryAfterUncertainResult_finishesCleanup() = test {
        val scenario = DeletionScenario()
        writeDraftFiles(revision = SAVED_REVISION)

        gate.close(DELETED_ID)
        gate.deleteThenRecoverDraft(
            postcardId = DELETED_ID,
            delete = { throw IOException("cleanup threw") },
            recoverDraft = {}
        )

        gate.close(DELETED_ID)
        val retry =
            gate.deleteThenRecoverDraft(
                postcardId = DELETED_ID,
                delete = { scenario.delete() },
                recoverDraft = { fail("성공한 재시도에서 복구하면 안 됨") }
            )

        assertTrue(retry is PostcardDeletionOutcome.RowDeleted)
        assertNoDraftFiles()
    }

    @Test
    fun internalCancellationInLiveCoroutine_isUncertain_notStuckDeleting() = test {
        val internal = CancellationException("internal")

        gate.close(DELETED_ID)
        val outcome =
            gate.deleteThenRecoverDraft(
                postcardId = DELETED_ID,
                delete = { throw internal },
                recoverDraft = { fail("불확실하면 복구하면 안 됨") }
            )

        assertEquals(PostcardDeletionOutcome.Uncertain(internal), outcome)
        assertFalse(attemptWrite())
    }

    @Test
    fun cancelledCallerDuringDeletion_propagates_withoutRecovery() = test {
        val deleteEntered = CompletableDeferred<Unit>()
        var recoverCalls = 0
        var finished = false

        gate.close(DELETED_ID)
        val job = launch {
            gate.deleteThenRecoverDraft(
                postcardId = DELETED_ID,
                delete = {
                    deleteEntered.complete(Unit)
                    CompletableDeferred<PostcardDeletionResult>().await()
                },
                recoverDraft = { recoverCalls++ }
            )
            finished = true
        }
        deleteEntered.await()
        job.cancel()
        job.join()

        assertFalse("취소된 호출자는 이어서 실행되지 않음", finished)
        assertEquals(0, recoverCalls)
        assertFalse(attemptWrite())
    }

    @Test
    fun recoveryFailure_isReported_releasesLock_andRetryStillWorks() = test {
        val scenario = DeletionScenario()
        val failure = IOException("disk full")

        gate.close(DELETED_ID)
        val outcome =
            gate.deleteThenRecoverDraft(
                postcardId = DELETED_ID,
                delete = { failDatabaseDelete(scenario.target) },
                recoverDraft = { throw failure }
            )

        assertEquals(PostcardDeletionOutcome.RowKept(failure), outcome)
        assertTrue("실패 뒤 문이 열려 저장 가능", attemptWrite(revision = UNSAVED_REVISION))

        gate.close(DELETED_ID)
        val retry =
            gate.deleteThenRecoverDraft(
                postcardId = DELETED_ID,
                delete = { scenario.delete() },
                recoverDraft = {}
            )
        assertTrue(retry is PostcardDeletionOutcome.RowDeleted)
        assertNoDraftFiles()
    }

    @Test
    fun internalCancellationDuringRecovery_isReported_notStuckDeleting() = test {
        val scenario = DeletionScenario(failDatabase = true)
        val internal = CancellationException("internal")

        gate.close(DELETED_ID)
        val outcome =
            gate.deleteThenRecoverDraft(
                postcardId = DELETED_ID,
                delete = { scenario.delete() },
                recoverDraft = { throw internal }
            )

        assertEquals(PostcardDeletionOutcome.RowKept(internal), outcome)
        assertTrue("엽서가 살아 있으므로 문은 열림", attemptWrite(revision = UNSAVED_REVISION))
    }

    @Test
    fun recoveryRunsOutsideTheLock_andAfterReopen() = test {
        val scenario = DeletionScenario(failDatabase = true)
        var recoveredWrite: Boolean? = null

        gate.close(DELETED_ID)
        gate.deleteThenRecoverDraft(
            postcardId = DELETED_ID,
            delete = { scenario.delete() },
            // persistDraftNow처럼 게이트로 다시 잠금을 얻는다. 잠금 안에서 부르면 여기서 멈춘다.
            recoverDraft = { recoveredWrite = attemptWrite(revision = UNSAVED_REVISION) }
        )

        assertEquals(true, recoveredWrite)
        assertEquals(
            UNSAVED_REVISION,
            PostcardDraftStorage.loadDraft(filesDir, DELETED_ID)?.revision
        )
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
    fun structure_deletePostcard_runsInAppScope_onMain_withGuardedRecovery() {
        val body = functionBody("fun deletePostcard()")
        val close = body.indexOf("draftDeletionGate.close(currentPostcard.id)")
        val cancel = body.indexOf("draftAutosaveJob?.cancel()")
        val launch =
            body.indexOf("postcardDeleteJob = exitSaveScope.launch(Dispatchers.Main.immediate)")
        val run = body.indexOf("draftDeletionGate.deleteThenRecoverDraft(")
        val manager = body.indexOf("deletionManager.deletePostcard(currentPostcard)")
        val recover = body.indexOf("recoverDraft = {")
        val guard = body.indexOf("hasUnpersistedDraftEdits(")
        val persist = body.indexOf("persistDraftNow()")

        assertTrue("닫기가 삭제 시작보다 먼저", close in 0 until launch)
        assertTrue("예약 취소가 삭제 시작보다 먼저", cancel in 0 until launch)
        assertTrue("삭제가 게이트 안에서 실행됨", run in (launch + 1) until manager)
        assertTrue("복구는 디스크에 없는 편집이 있을 때만", guard in (recover + 1) until persist)
        assertFalse("화면 범위에서 삭제하면 안 됨", body.contains("viewModelScope"))
        assertFalse("복구를 화면 범위 예약에 맡기면 안 됨", body.contains("scheduleDraftAutosave()"))
    }

    @Test
    fun structure_systemBack_isIgnoredWhileDeleting() {
        val screenText =
            readStructureTestSource(
                listOf(
                    "src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt",
                    "app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt"
                )
            )
        val start = screenText.indexOf("val navigateBackAfterPendingStyleSaves: () -> Unit = {")
        val guard = screenText.indexOf("if (deleteState !is PostcardDeleteState.Deleting)", start)
        val exit = screenText.indexOf("exitScope.launch", start)

        assertTrue(start >= 0)
        assertTrue("대기와 복귀가 삭제 중 검사 안에 있어야 함", guard in (start + 1) until exit)
    }

    private companion object {
        const val DELETED_ID = 7L
        const val OTHER_ID = 8L
        const val SAVED_REVISION = 1L
        const val UNSAVED_REVISION = 2L
        const val TIMEOUT_MS = 10_000L
    }
}
