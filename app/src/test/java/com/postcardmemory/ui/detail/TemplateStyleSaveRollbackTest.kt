package com.postcardmemory.ui.detail

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailViewModel의 persistTemplateStyle()/applyTemplate()/
 * undoTemplateStyleChange()/redoTemplateStyleChange()는 Context/Room/Hilt에
 * 묶여 있어 Robolectric·mockk 없이 순수 JUnit에서 직접 호출할 수 없다(이
 * 프로젝트는 새 테스트 프레임워크를 추가하지 않는 방침 —
 * StickerBackgroundRemovalCancellationTest와 동일한 제약). 대신 그 함수들이
 * 실제로 쓰는 것과 완전히 동일한 제어 흐름 — Job 취소가 새 저장 시작 시
 * 이전 저장을 밀어내고(templateStyleSaveJob?.cancel()), CancellationException은
 * rethrow해서 실패로 취급하지 않으며, 실제 실패 시에만 저장 직전에 캡처한
 * 스냅샷으로 스타일·Undo/Redo 스택·도장·선택 표시를 모두 되돌리는 구조 —
 * 를 순수 데이터로 재현해 검증한다. kotlinx-coroutines-test는 이 프로젝트의
 * 의존성에 없으므로 StandardTestDispatcher 대신 CompletableDeferred +
 * runBlocking + job.join()으로 실제 코루틴 완료를 기다린다.
 */
class TemplateStyleSaveRollbackTest {

    private data class Snapshot(
        val style: String,
        val seals: List<String>?
    )

    /** DetailViewModel의 템플릿 관련 상태를 그대로 옮겨온 가짜 컨테이너. */
    private class FakeTemplateState(initialStyle: String = "style-0") {
        var style: String = initialStyle
        var seals: List<String> = emptyList()
        var lastAppliedTemplateId: String? = null
        val undoStack = ArrayDeque<Snapshot>()
        val redoStack = ArrayDeque<Snapshot>()
        var canUndo = false
        var canRedo = false
        val errors = mutableListOf<String>()

        private var saveJob: Job? = null

        private fun updateAvailability() {
            canUndo = undoStack.isNotEmpty()
            canRedo = redoStack.isNotEmpty()
        }

        /** persistTemplateStyle()과 동일한 형태. */
        private fun persistStyle(
            scope: CoroutineScope,
            newStyle: String,
            previousStyle: String,
            save: suspend (String) -> Unit,
            onSaveFailed: () -> Unit
        ): Job {
            style = newStyle

            saveJob?.cancel()
            val job = scope.launch {
                try {
                    save(newStyle)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    style = previousStyle
                    onSaveFailed()
                    errors += "템플릿 스타일을 저장하지 못했어. 이전 상태로 되돌렸어."
                }
            }
            saveJob = job
            return job
        }

        /** applyTemplate()과 동일한 형태(historyLimit은 TEMPLATE_STYLE_HISTORY_LIMIT 대응). */
        fun applyTemplate(
            scope: CoroutineScope,
            templateId: String,
            newStyle: String,
            templateSeal: String?,
            historyLimit: Int = 50,
            save: suspend (String) -> Unit
        ): Job {
            val willAddSeal = templateSeal != null && seals.isEmpty()
            val previousStyle = style
            val previousSeals = seals
            val previousTemplateId = lastAppliedTemplateId
            val undoBefore = ArrayDeque(undoStack)
            val redoBefore = ArrayDeque(redoStack)

            undoStack.addLast(
                Snapshot(previousStyle, if (willAddSeal) previousSeals else null)
            )
            if (undoStack.size > historyLimit) {
                undoStack.removeFirst()
            }
            redoStack.clear()
            updateAvailability()

            lastAppliedTemplateId = templateId

            if (willAddSeal) {
                seals = listOf(templateSeal!!)
            }

            return persistStyle(
                scope = scope,
                newStyle = newStyle,
                previousStyle = previousStyle,
                save = save,
                onSaveFailed = {
                    undoStack.clear()
                    undoStack.addAll(undoBefore)
                    redoStack.clear()
                    redoStack.addAll(redoBefore)
                    updateAvailability()

                    lastAppliedTemplateId = previousTemplateId

                    if (willAddSeal) {
                        seals = previousSeals
                    }
                }
            )
        }

        /** undoTemplateStyleChange()와 동일한 형태. */
        fun undo(scope: CoroutineScope, save: suspend (String) -> Unit): Job? {
            if (undoStack.isEmpty()) return null
            val undoBefore = ArrayDeque(undoStack)
            val redoBefore = ArrayDeque(redoStack)
            val sealsBefore = seals
            val styleBeforeOp = style

            val previous = undoStack.removeLast()
            redoStack.addLast(
                Snapshot(styleBeforeOp, if (previous.seals != null) seals else null)
            )

            val job = persistStyle(
                scope = scope,
                newStyle = previous.style,
                previousStyle = styleBeforeOp,
                save = save,
                onSaveFailed = {
                    undoStack.clear()
                    undoStack.addAll(undoBefore)
                    redoStack.clear()
                    redoStack.addAll(redoBefore)
                    updateAvailability()

                    if (previous.seals != null) {
                        seals = sealsBefore
                    }
                }
            )

            if (previous.seals != null) {
                seals = previous.seals
            }
            updateAvailability()
            return job
        }

        /** redoTemplateStyleChange()와 동일한 형태. */
        fun redo(scope: CoroutineScope, save: suspend (String) -> Unit): Job? {
            if (redoStack.isEmpty()) return null
            val undoBefore = ArrayDeque(undoStack)
            val redoBefore = ArrayDeque(redoStack)
            val sealsBefore = seals
            val styleBeforeOp = style

            val next = redoStack.removeLast()
            undoStack.addLast(
                Snapshot(styleBeforeOp, if (next.seals != null) seals else null)
            )

            val job = persistStyle(
                scope = scope,
                newStyle = next.style,
                previousStyle = styleBeforeOp,
                save = save,
                onSaveFailed = {
                    undoStack.clear()
                    undoStack.addAll(undoBefore)
                    redoStack.clear()
                    redoStack.addAll(redoBefore)
                    updateAvailability()

                    if (next.seals != null) {
                        seals = sealsBefore
                    }
                }
            )

            if (next.seals != null) {
                seals = next.seals
            }
            updateAvailability()
            return job
        }
    }

    // ---- 저장 성공: 기존 동작 유지 ----

    @Test
    fun applyTemplate_saveSucceeds_keepsNewStyle_noError() = runBlocking {
        val state = FakeTemplateState()

        val job = state.applyTemplate(this, "A", "style-A", templateSeal = null) { }
        job.join()

        assertEquals("style-A", state.style)
        assertTrue(state.errors.isEmpty())
        assertTrue(state.canUndo)
        assertFalse(state.canRedo)
    }

    // ---- 적용 실패: 전체 상태 롤백 ----

    @Test
    fun applyTemplate_saveFails_rollsBackStyleStackSelectionAndSeal() = runBlocking {
        val state = FakeTemplateState()
        // 적용 전: 아무 템플릿도 선택되지 않았고 도장도 없는 상태.

        val job = state.applyTemplate(
            this, "B", "style-B", templateSeal = "seal-B"
        ) { throw IllegalStateException("db failed") }
        job.join()

        assertEquals("style-0", state.style)
        assertTrue(state.undoStack.toList().isEmpty())
        assertTrue(state.redoStack.toList().isEmpty())
        assertNull(state.lastAppliedTemplateId)
        assertTrue(state.seals.isEmpty())
        assertFalse(state.canUndo)
        assertFalse(state.canRedo)
        assertEquals(1, state.errors.size)
    }

    @Test
    fun applyTemplate_saveFails_withoutSeal_leavesExistingSealsUntouched() = runBlocking {
        val state = FakeTemplateState()
        state.seals = listOf("existing-seal")

        val job = state.applyTemplate(
            this, "B", "style-B", templateSeal = "seal-B" // 이미 도장이 있으므로 willAddSeal=false
        ) { throw IllegalStateException("db failed") }
        job.join()

        assertEquals(listOf("existing-seal"), state.seals)
    }

    @Test
    fun applyTemplate_saveFails_thenReapply_succeedsNormally() = runBlocking {
        val state = FakeTemplateState()

        state.applyTemplate(this, "B", "style-B", templateSeal = null) {
            throw IllegalStateException("db failed")
        }.join()
        assertEquals("style-0", state.style)

        val secondJob =
            state.applyTemplate(this, "C", "style-C", templateSeal = null) { }
        secondJob.join()

        assertEquals("style-C", state.style)
        assertEquals("C", state.lastAppliedTemplateId)
        assertTrue(state.canUndo)
    }

    @Test
    fun applyTemplate_saveFails_evictedHistoryEntryIsRestored() = runBlocking {
        val state = FakeTemplateState(initialStyle = "style-2")
        state.undoStack.addLast(Snapshot("s1", null))
        state.undoStack.addLast(Snapshot("s2", null))

        val job = state.applyTemplate(
            this, "X", "style-X", templateSeal = null, historyLimit = 2
        ) { throw IllegalStateException("db failed") }
        job.join()

        assertEquals("style-2", state.style)
        assertEquals(
            listOf(Snapshot("s1", null), Snapshot("s2", null)),
            state.undoStack.toList()
        )
    }

    // ---- 취소는 실패가 아니다 ----

    @Test
    fun applyTemplate_saveThrowsCancellation_doesNotRollbackOrRecordError() = runBlocking {
        val state = FakeTemplateState()

        val job = launch {
            state.applyTemplate(this, "A", "style-A", templateSeal = null) {
                throw CancellationException("left screen")
            }.join()
        }
        job.join()

        assertTrue(state.errors.isEmpty())
        assertEquals("style-A", state.style)
    }

    @Test
    fun applyTemplate_supersededByNewerApply_oldFailureDoesNotRollbackNewState() = runBlocking {
        val state = FakeTemplateState()
        val deferredA = CompletableDeferred<Unit>()

        // A 적용: 저장이 끝나지 않고 매달려 있는 상태를 흉내낸다.
        val jobA = state.applyTemplate(this, "A", "style-A", templateSeal = null) {
            deferredA.await()
        }

        // A의 저장이 끝나기 전에 B를 적용하면 templateStyleSaveJob?.cancel()이
        // A의 Job을 취소한다 — A는 자기 catch(Exception)에 도달하지 못해야 한다.
        val jobB = state.applyTemplate(this, "B", "style-B", templateSeal = null) {
            throw IllegalStateException("db failed")
        }

        jobA.join()
        jobB.join()

        assertTrue(jobA.isCancelled)
        // B 실패는 B 적용 직전 상태(A 적용 직후, style-A)로만 되돌아가야 한다.
        // A 적용 이전(style-0)까지 되돌아가면 오래된 작업이 최신 상태를
        // 롤백한 것이므로 회귀다.
        assertEquals("style-A", state.style)
        assertEquals(1, state.errors.size)
    }

    // ---- Undo 실패 ----

    @Test
    fun undo_saveFails_restoresPreUndoStateAndStacks() = runBlocking {
        val state = FakeTemplateState(initialStyle = "style-1")
        state.undoStack.addLast(Snapshot("style-0", null))

        val job = state.undo(this) { throw IllegalStateException("db failed") }
        requireNotNull(job).join()

        assertEquals("style-1", state.style)
        assertEquals(listOf(Snapshot("style-0", null)), state.undoStack.toList())
        assertTrue(state.redoStack.toList().isEmpty())
        assertEquals(1, state.errors.size)
    }

    @Test
    fun undo_saveFails_thenUndoAgain_succeedsNormally() = runBlocking {
        val state = FakeTemplateState(initialStyle = "style-1")
        state.undoStack.addLast(Snapshot("style-0", null))

        state.undo(this) { throw IllegalStateException("db failed") }
            ?.join()
        assertEquals("style-1", state.style)

        val secondJob = state.undo(this) { }
        requireNotNull(secondJob).join()

        assertEquals("style-0", state.style)
        assertTrue(state.undoStack.toList().isEmpty())
        assertTrue(state.canRedo)
    }

    // ---- Redo 실패 ----

    @Test
    fun redo_saveFails_restoresPreRedoStateAndStacks() = runBlocking {
        val state = FakeTemplateState(initialStyle = "style-0")
        state.redoStack.addLast(Snapshot("style-1", null))

        val job = state.redo(this) { throw IllegalStateException("db failed") }
        requireNotNull(job).join()

        assertEquals("style-0", state.style)
        assertEquals(listOf(Snapshot("style-1", null)), state.redoStack.toList())
        assertTrue(state.undoStack.toList().isEmpty())
        assertEquals(1, state.errors.size)
    }

    @Test
    fun redo_saveFails_sealRestoredAndDraftWouldBeRescheduled() = runBlocking {
        val state = FakeTemplateState(initialStyle = "style-0")
        state.seals = listOf("seal-before-redo")
        state.redoStack.addLast(Snapshot("style-1", listOf("seal-after-redo")))

        val job = state.redo(this) { throw IllegalStateException("db failed") }
        requireNotNull(job).join()

        assertEquals(listOf("seal-before-redo"), state.seals)
    }

    // ---- 선택 표시는 Undo/Redo가 건드리지 않는다(기존 동작 보존) ----

    @Test
    fun undo_doesNotTouchLastAppliedTemplateId_onSuccessOrFailure() = runBlocking {
        val successState = FakeTemplateState(initialStyle = "style-1")
        successState.lastAppliedTemplateId = "some-template"
        successState.undoStack.addLast(Snapshot("style-0", null))
        successState.undo(this) { }?.join()
        assertEquals("some-template", successState.lastAppliedTemplateId)

        val failureState = FakeTemplateState(initialStyle = "style-1")
        failureState.lastAppliedTemplateId = "some-template"
        failureState.undoStack.addLast(Snapshot("style-0", null))
        failureState.undo(this) { throw IllegalStateException("db failed") }?.join()
        assertEquals("some-template", failureState.lastAppliedTemplateId)
    }

    // ---- 여러 종류의 진입점이 같은 저장 job을 공유해도 취소 보호가 유지된다 ----

    @Test
    fun applyThenUndo_cancelsPendingApply_undoFailureDoesNotRollbackPastUndo() = runBlocking {
        val state = FakeTemplateState(initialStyle = "style-0")
        val deferredApply = CompletableDeferred<Unit>()

        val applyJob = state.applyTemplate(this, "A", "style-A", templateSeal = null) {
            deferredApply.await()
        }

        // apply의 저장이 끝나기 전에 undo를 실행하면 같은 templateStyleSaveJob을
        // 공유하므로 apply의 저장이 취소된다.
        state.undoStack.addLast(Snapshot("style-A", null))
        val undoJob = state.undo(this) { throw IllegalStateException("db failed") }

        applyJob.join()
        requireNotNull(undoJob).join()

        assertTrue(applyJob.isCancelled)
        // undo 실패는 undo 실행 직전 상태(style-A)로 돌아가야 한다.
        assertEquals("style-A", state.style)
    }
}
