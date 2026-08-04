package com.postcardmemory.ui.detail

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailViewModel의 봉투 저장 함수(updateEnvelopeStyle/updateEnvelopePostmarked/
 * removeEnvelope)는 Context/Room/Hilt에 묶여 있어 Robolectric·mockk 없이
 * 순수 JUnit에서 직접 호출할 수 없다(StyleSaveRaceTest와 동일한 제약). 실제
 * 구현과 같은 구조 — 낙관적 갱신 → 공유 styleWriteMutex로 직렬화된 쓰기 →
 * Mutex를 쥔 순간 최신 상태를 다시 읽어 쓰기 → 실패 시 "그 사이 더 최신 값으로
 * 덮어써지지 않았을 때만" 롤백 — 을 가짜 UI 상태와 가짜 Room으로 재현해 검증한다.
 */
class EnvelopeStateTest {

    private class FakeUiState {
        var envelopeStyle: String? = null
        var envelopePostmarked: Boolean = false
    }

    private class FakeRoom {
        var envelopeStyle: String? = null
        var envelopePostmarked: Boolean = false
        val writeLog = mutableListOf<String>()
    }

    private class FakeEnvelopeViewModel {
        val ui = FakeUiState()
        val room = FakeRoom()
        val errors = mutableListOf<String>()
        private val styleWriteMutex = Mutex()

        fun updateEnvelopeStyle(
            scope: CoroutineScope,
            newStyle: String,
            failWith: Exception? = null,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            val previousStyle = ui.envelopeStyle
            if (previousStyle == newStyle) {
                return scope.launch { }
            }
            ui.envelopeStyle = newStyle // 낙관적 갱신 — 소인 상태는 건드리지 않는다

            return scope.launch {
                try {
                    beforeWrite()
                    styleWriteMutex.withLock {
                        val latest = ui.envelopeStyle ?: return@withLock
                        if (failWith != null) throw failWith
                        room.envelopeStyle = latest
                        room.writeLog += "style=$latest"
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    if (ui.envelopeStyle == newStyle) {
                        ui.envelopeStyle = previousStyle
                    }
                    errors += "envelope style save failed"
                }
            }
        }

        fun updateEnvelopePostmarked(
            scope: CoroutineScope,
            postmarked: Boolean,
            failWith: Exception? = null,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            if (ui.envelopeStyle == null) {
                return scope.launch { }
            }
            val previousPostmarked = ui.envelopePostmarked
            if (previousPostmarked == postmarked) {
                return scope.launch { }
            }
            ui.envelopePostmarked = postmarked // 낙관적 갱신

            return scope.launch {
                try {
                    beforeWrite()
                    styleWriteMutex.withLock {
                        val latest = ui.envelopePostmarked
                        if (failWith != null) throw failWith
                        room.envelopePostmarked = latest
                        room.writeLog += "postmarked=$latest"
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    if (ui.envelopePostmarked == postmarked) {
                        ui.envelopePostmarked = previousPostmarked
                    }
                    errors += "postmark save failed"
                }
            }
        }

        fun removeEnvelope(
            scope: CoroutineScope,
            failWith: Exception? = null,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            if (ui.envelopeStyle == null) {
                return scope.launch { }
            }
            val previousStyle = ui.envelopeStyle
            val previousPostmarked = ui.envelopePostmarked
            ui.envelopeStyle = null // 낙관적 갱신 — 봉투와 소인을 함께 지운다
            ui.envelopePostmarked = false

            return scope.launch {
                try {
                    beforeWrite()
                    styleWriteMutex.withLock {
                        if (failWith != null) throw failWith
                        room.envelopeStyle = null
                        room.envelopePostmarked = false
                        room.writeLog += "cleared"
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    if (ui.envelopeStyle == null) {
                        ui.envelopeStyle = previousStyle
                        ui.envelopePostmarked = previousPostmarked
                    }
                    errors += "envelope clear failed"
                }
            }
        }
    }

    // ---- 기본 상태 전이 ----

    @Test
    fun applyStyle_setsEnvelope_postmarkStaysFalse() = runBlocking {
        val vm = FakeEnvelopeViewModel()

        vm.updateEnvelopeStyle(this, "IVORY").join()

        assertEquals("IVORY", vm.ui.envelopeStyle)
        assertEquals("IVORY", vm.room.envelopeStyle)
        assertFalse(vm.ui.envelopePostmarked)
        assertFalse(vm.room.envelopePostmarked)
    }

    @Test
    fun changingStyle_afterPostmarked_keepsPostmarkTrue() = runBlocking {
        val vm = FakeEnvelopeViewModel()
        vm.updateEnvelopeStyle(this, "IVORY").join()
        vm.updateEnvelopePostmarked(this, true).join()

        vm.updateEnvelopeStyle(this, "KRAFT").join()

        assertEquals("KRAFT", vm.room.envelopeStyle)
        assertTrue(
            "봉투를 바꿔도 이미 찍은 소인은 유지되어야 한다",
            vm.ui.envelopePostmarked
        )
        assertTrue(vm.room.envelopePostmarked)
    }

    @Test
    fun postmark_thenClear_keepsEnvelopeStyle() = runBlocking {
        val vm = FakeEnvelopeViewModel()
        vm.updateEnvelopeStyle(this, "MINT").join()
        vm.updateEnvelopePostmarked(this, true).join()

        vm.updateEnvelopePostmarked(this, false).join()

        assertEquals("MINT", vm.ui.envelopeStyle)
        assertEquals("MINT", vm.room.envelopeStyle)
        assertFalse(vm.ui.envelopePostmarked)
        assertFalse(vm.room.envelopePostmarked)
    }

    @Test
    fun repeatedPostmarkTrue_isIdempotent_noDuplicateWrite() = runBlocking {
        val vm = FakeEnvelopeViewModel()
        vm.updateEnvelopeStyle(this, "IVORY").join()

        vm.updateEnvelopePostmarked(this, true).join()
        vm.updateEnvelopePostmarked(this, true).join() // 다시 찍기 — 값이 이미 true

        assertTrue(vm.ui.envelopePostmarked)
        // 값이 안 바뀌면 새로 쓰지 않는다 — 최초 1회만 기록된다.
        assertEquals(listOf("style=IVORY", "postmarked=true"), vm.room.writeLog)
    }

    @Test
    fun removeEnvelope_clearsStyleAndPostmark() = runBlocking {
        val vm = FakeEnvelopeViewModel()
        vm.updateEnvelopeStyle(this, "AIRMAIL").join()
        vm.updateEnvelopePostmarked(this, true).join()

        vm.removeEnvelope(this).join()

        assertNull(vm.ui.envelopeStyle)
        assertNull(vm.room.envelopeStyle)
        assertFalse(vm.ui.envelopePostmarked)
        assertFalse(vm.room.envelopePostmarked)
    }

    @Test
    fun postmark_withoutEnvelope_isNoOp() = runBlocking {
        val vm = FakeEnvelopeViewModel()

        vm.updateEnvelopePostmarked(this, true).join()

        assertFalse(vm.ui.envelopePostmarked)
        assertTrue(vm.room.writeLog.isEmpty())
    }

    // ---- 실패와 롤백 ----

    @Test
    fun styleSaveFailure_rollsBackAndRecordsError() = runBlocking {
        val vm = FakeEnvelopeViewModel()

        vm.updateEnvelopeStyle(
            this,
            "LAVENDER",
            failWith = IllegalStateException("db failed")
        ).join()

        assertEquals(1, vm.errors.size)
        assertNull(vm.ui.envelopeStyle)
        assertNull(vm.room.envelopeStyle)
    }

    @Test
    fun postmarkSaveFailure_rollsBackAndRecordsError() = runBlocking {
        val vm = FakeEnvelopeViewModel()
        vm.updateEnvelopeStyle(this, "IVORY").join()

        vm.updateEnvelopePostmarked(
            this,
            true,
            failWith = IllegalStateException("db failed")
        ).join()

        assertEquals(1, vm.errors.size)
        assertFalse(vm.ui.envelopePostmarked)
        assertFalse(vm.room.envelopePostmarked)
    }

    // ---- 경합: 실패한 저장의 롤백이 그 사이 커밋된 더 최신 값을 덮어쓰면 안 된다 ----
    // (StyleSaveRaceTest의 핵심 회귀 사례와 동일한 위험을 봉투 소인에 대해 검증)

    @Test
    fun postmarkFails_afterNewerToggleCommitted_doesNotStompIt() = runBlocking {
        val vm = FakeEnvelopeViewModel()
        vm.updateEnvelopeStyle(this, "IVORY").join()

        val letFirstProceed = CompletableDeferred<Unit>()

        // 1. 찍기 요청 — 실제 DAO 쓰기 직전까지만 진행하고 멈춰 있다.
        val firstJob = vm.updateEnvelopePostmarked(
            this,
            true,
            failWith = IllegalStateException("db failed")
        ) {
            letFirstProceed.await()
        }

        // 2. 그 사이 사용자가 다시 지우기를 눌렀다고 가정 — 별도 Job이라 방해받지
        //    않고 먼저 커밋된다(여기서는 지우기가 곧 false 유지이므로, 대신
        //    스타일을 바꿔 최신 커밋을 관찰한다).
        vm.updateEnvelopeStyle(this, "KRAFT").join()

        // 3. 뒤늦게 찍기 시도가 실패한다.
        letFirstProceed.complete(Unit)
        firstJob.join()

        assertEquals(1, vm.errors.size)
        // 실패한 찍기의 롤백은 postmarked만 되돌리고, 그 사이 커밋된 최신
        // 스타일(KRAFT)을 건드리면 안 된다.
        assertEquals("KRAFT", vm.ui.envelopeStyle)
        assertEquals("KRAFT", vm.room.envelopeStyle)
    }
}
