package com.postcardmemory.ui.detail

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailScreenExitSaveLossTest가 재현한 문제(개별 스타일 저장이 실제 DAO
 * 쓰기 전에 viewModelScope가 취소되면 마지막 값이 유실됨)에 대한 수정을
 * 검증한다. 실제 수정은 DetailViewModel.awaitPendingStyleSaves()를 추가하고,
 * DetailScreen의 뒤로 가기(아이콘 버튼·시스템 back 모두)를
 * "awaitPendingStyleSaves() 완료 → onNavigateBack()" 순서로 감싼 것이다 —
 * onNavigateBack()이 popBackStack()을 호출해야 비로소 NavBackStackEntry의
 * ViewModelStore가 clear()되어 viewModelScope가 취소되므로, await가 먼저
 * 끝나면 그 시점에는 이미 저장이 완료돼 있다.
 *
 * DetailViewModel을 Context/Room/Hilt 없이 직접 인스턴스화할 수 없으므로
 * (StyleSaveRaceTest, DetailScreenExitSaveLossTest와 동일한 제약), 동일한
 * FakeViewModel 구조에 awaitPendingStyleSaves()에 대응하는 함수를 추가해
 * "await 완료 후에만 scope를 취소한다"는 실제 코드의 순서를 재현한다.
 */
class DetailScreenExitSaveGuaranteeTest {

    private class FakeRoom {
        var fieldA: Int = 0
        var fieldC: Int = 0
        var draftField: Int = -1
    }

    private class FakeViewModel(parentJob: Job) {
        var uiFieldA: Int = 0
        var uiFieldC: Int = 0
        var uiDraftField: Int = -1
        val room = FakeRoom()
        val errors = mutableListOf<String>()
        private val scope = CoroutineScope(parentJob)
        private val styleWriteMutex = Mutex()
        private var fieldASaveJob: Job? = null
        private var fieldCSaveJob: Job? = null
        private var draftAutosaveJob: Job? = null

        fun saveFieldA(
            newValue: Int,
            failWith: Exception? = null,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            val previousValue = uiFieldA
            uiFieldA = newValue

            fieldASaveJob?.cancel()
            val job = scope.launch {
                try {
                    beforeWrite()
                    styleWriteMutex.withLock {
                        val latest = uiFieldA
                        if (failWith != null) throw failWith
                        room.fieldA = latest
                    }
                } catch (exception: kotlinx.coroutines.CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    uiFieldA = previousValue
                    errors += "fieldA save failed"
                }
            }
            fieldASaveJob = job
            return job
        }


        /**
         * DetailViewModel.updateBackgroundColor 등 5개 수정 후와 동일한 형태:
         * 이전 Job을 cancel()하지 않지만(재읽기+직렬화만으로 최종 상태에
         * 수렴하므로) 필드에는 보관해 awaitPendingStyleSaves()가 찾을 수 있다.
         */
        fun updateFieldC(
            newValue: Int,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            uiFieldC = newValue

            val job = scope.launch {
                beforeWrite()
                styleWriteMutex.withLock {
                    room.fieldC = uiFieldC
                }
            }
            fieldCSaveJob = job
            return job
        }

        /**
         * DetailViewModel.scheduleDraftAutosave()와 동일한 형태 — debounce
         * 뒤에야 실제 저장이 일어난다.
         */
        fun scheduleDraftAutosave(
            newValue: Int,
            debounceMillis: Long,
            beforeWrite: suspend () -> Unit = {}
        ) {
            uiDraftField = newValue

            draftAutosaveJob?.cancel()
            draftAutosaveJob = scope.launch {
                delay(debounceMillis)
                beforeWrite()
                persistDraftNow()
            }
        }

        private suspend fun persistDraftNow() {
            styleWriteMutex.withLock {
                room.draftField = uiDraftField
            }
        }

        /**
         * DetailViewModel.awaitPendingStyleSaves()와 동일한 형태 —
         * draftAutosaveJob은 join하지 않고, 아직 debounce 대기 중이면
         * cancel 후 persistDraftNow()를 직접 호출해 즉시 완료를 기다린다
         * (flushDraftNow()와 동일한 방식 — 57일차 제5차 수정).
         */
        suspend fun awaitPendingStyleSaves(timeoutMillis: Long = 2_000L) {
            val pendingJobs =
                listOfNotNull(fieldASaveJob, fieldCSaveJob)
                    .filter { it.isActive }

            if (pendingJobs.isNotEmpty()) {
                withTimeoutOrNull(timeoutMillis) {
                    pendingJobs.joinAll()
                }
            }

            if (draftAutosaveJob?.isActive == true) {
                draftAutosaveJob?.cancel()
                withTimeoutOrNull(timeoutMillis) {
                    persistDraftNow()
                }
            }
        }
    }

    /** DetailScreen.navigateBackAfterPendingStyleSaves와 동일한 순서. */
    private suspend fun exitScreen(vm: FakeViewModel, parentJob: Job) {
        vm.awaitPendingStyleSaves()
        parentJob.cancel() // onNavigateBack() -> popBackStack() -> ViewModelStore.clear()에 해당
    }

    // ---- 시나리오 A: 값 변경 직후 이탈 — await 덕분에 마지막 값이 저장된다 ----

    @Test
    fun awaitBeforeExit_lastValueIsPersisted() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        val saveJob = vm.saveFieldA(newValue = 42) {
            reachedDispatchPoint.await()
        }

        val exitJob = launch { exitScreen(vm, parentJob) }

        // 실제 Dispatchers.IO 디스패치가 뒤늦게 일어나는 상황을 흉내낸다 —
        // exitJob은 이미 awaitPendingStyleSaves()에서 join 대기 중이어야 한다.
        reachedDispatchPoint.complete(Unit)
        saveJob.join()
        exitJob.join()

        assertFalse(saveJob.isCancelled)
        assertEquals(42, vm.room.fieldA) // await가 취소보다 먼저 저장을 끝냈다
    }

    // ---- 시나리오 B: A -> B로 빠르게 변경 후 이탈 — 최신 B가 살아남는다 ----

    @Test
    fun rapidAThenB_awaitBeforeExit_latestBPersists() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        vm.saveFieldA(newValue = 1).join()
        assertEquals(1, vm.room.fieldA)

        val saveJobB = vm.saveFieldA(newValue = 2) {
            reachedDispatchPoint.await()
        }
        val exitJob = launch { exitScreen(vm, parentJob) }

        reachedDispatchPoint.complete(Unit)
        saveJobB.join()
        exitJob.join()

        assertFalse(saveJobB.isCancelled)
        assertEquals(2, vm.room.fieldA) // 더 이상 오래된 1로 되돌아가지 않는다
    }

    // ---- 저장 실패는 취소가 아니다 — await 도입 후에도 실패 롤백은 그대로 동작한다 ----

    @Test
    fun awaitDoesNotSuppressRealSaveFailureRollback() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)

        val job = vm.saveFieldA(
            newValue = 42,
            failWith = IllegalStateException("db failed")
        )
        vm.awaitPendingStyleSaves()

        job.join()
        assertEquals(1, vm.errors.size)
        assertEquals(0, vm.uiFieldA) // 실패 전 값으로 롤백
        assertEquals(0, vm.room.fieldA)
    }

    // ---- 비정상적으로 오래 걸리는 저장이 있어도 navigation이 무기한 멈추지 않는다 ----

    @Test
    fun awaitPendingStyleSaves_neverHangsForever_boundedByTimeout() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val neverCompletes = CompletableDeferred<Unit>()

        vm.saveFieldA(newValue = 1) {
            neverCompletes.await() // 절대 끝나지 않는 저장을 흉내낸다
        }

        // 실제 코드는 2_000ms를 쓰지만 테스트에서는 짧은 상한을 넣어 빠르게 검증한다.
        val elapsedJob = launch {
            vm.awaitPendingStyleSaves(timeoutMillis = 50L)
        }
        elapsedJob.join()

        assertTrue(elapsedJob.isCompleted)
        assertFalse(elapsedJob.isCancelled) // 타임아웃으로 정상 반환됐다(취소된 것이 아님)

        parentJob.cancel() // 영원히 끝나지 않을 저장을 정리한다(테스트 종료 후 누수 방지)
    }

    // ---- 시나리오 E: Job 추적이 추가된 배경색류 저장(fieldC)도 이제 보존된다 ----
    //
    // DetailScreenExitSaveLossTest.awaitPendingStyleSaves_doesNotCoverJoblessSave_fieldCStillLost가
    // 재현한 문제(awaitPendingStyleSaves()가 Job을 어디에도 보관하지 않는
    // 배경색/배경 패턴/폰트/레이아웃/날짜 형식 저장을 기다리지 못함)에 대한
    // 수정을 검증한다. 실제 수정은 DetailViewModel에 backgroundColorSaveJob 등
    // 5개 필드를 추가하고 각 저장 함수가 launch한 Job을 거기에 담아
    // awaitPendingStyleSaves()의 목록에 포함시킨 것이다.
    @Test
    fun awaitBeforeExit_backgroundColorLikeSaveIsNowPersisted() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        val fieldCJob = vm.updateFieldC(newValue = 7) {
            reachedDispatchPoint.await()
        }

        val exitJob = launch { exitScreen(vm, parentJob) }

        reachedDispatchPoint.complete(Unit)
        fieldCJob.join()
        exitJob.join()

        assertFalse(fieldCJob.isCancelled)
        assertEquals(7, vm.room.fieldC) // await가 취소보다 먼저 저장을 끝냈다
    }

    // ---- 57일차 저장·데이터 안전성 챕터 제5차: draftAutosaveJob(초안 자동저장
    // debounce)도 이제 화면 이탈 전에 보존된다 ----
    //
    // draftAutosaveJob은 style-save Job들과 달리 스스로 delay(debounce)를
    // 거친 뒤에야 실제 저장을 한다. 수정 전에는 awaitPendingStyleSaves()의
    // 추적 대상에 전혀 없어서, debounce가 끝나기 전에 화면을 나가면(흔한
    // 사용 패턴 — 편집 직후 바로 뒤로가기) 아직 시작도 안 한 초안 저장이
    // viewModelScope 취소로 그대로 유실됐다. 수정은 flushDraftNow()와 동일한
    // 방식으로 debounce를 기다리지 않고 즉시 cancel 후 persistDraftNow()를
    // 직접 호출해 완료를 기다리는 것이다.

    @Test
    fun awaitBeforeExit_pendingDraftAutosaveIsFlushedNotLostToDebounce() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)

        // 편집 직후(디바운스가 끝나기 한참 전) 바로 나가는 상황을 흉내낸다.
        vm.scheduleDraftAutosave(newValue = 99, debounceMillis = 10_000L)
        assertEquals(-1, vm.room.draftField) // 아직 debounce 중이라 저장 전

        vm.awaitPendingStyleSaves()

        // debounce(10초)를 기다리지 않고도 즉시 flush돼 저장됐어야 한다.
        assertEquals(99, vm.room.draftField)

        parentJob.cancel()
    }

    @Test
    fun awaitBeforeExit_pendingDraftAutosave_doesNotWaitFullDebounce() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)

        vm.scheduleDraftAutosave(newValue = 7, debounceMillis = 10_000L)

        val elapsedMillis = kotlin.system.measureTimeMillis {
            vm.awaitPendingStyleSaves()
        }

        // debounce(10초)를 그대로 기다렸다면 이 값이 훨씬 컸을 것이다 —
        // cancel 후 즉시 flush하므로 가상 시간이 아니어도 매우 빨라야 한다.
        assertTrue(elapsedMillis < 2_000L)
        assertEquals(7, vm.room.draftField)

        parentJob.cancel()
    }

    @Test
    fun awaitBeforeExit_noPendingDraftAutosave_doesNothing() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)

        // 예약된 초안 저장이 전혀 없는 상태 — 아무 것도 하지 않고 조용히 통과해야 한다.
        vm.awaitPendingStyleSaves()

        assertEquals(-1, vm.room.draftField)

        parentJob.cancel()
    }
}
