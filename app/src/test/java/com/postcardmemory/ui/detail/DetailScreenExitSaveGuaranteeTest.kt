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
 * DetailScreenExitSaveLossTest가 재현한 문제(슬라이더·템플릿 저장이 실제 DAO
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
        var fieldB: Int = 0
        var fieldC: Int = 0
    }

    private class FakeViewModel(parentJob: Job) {
        var uiFieldA: Int = 0
        var uiFieldB: Int = 0
        var uiFieldC: Int = 0
        val room = FakeRoom()
        val errors = mutableListOf<String>()
        private val scope = CoroutineScope(parentJob)
        private val styleWriteMutex = Mutex()
        private var fieldASaveJob: Job? = null
        private var templateSaveJob: Job? = null
        private var fieldCSaveJob: Job? = null

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

        fun applyTemplate(
            newA: Int,
            newB: Int,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            uiFieldA = newA
            uiFieldB = newB

            templateSaveJob?.cancel()
            val job = scope.launch {
                beforeWrite()
                styleWriteMutex.withLock {
                    room.fieldA = uiFieldA
                    room.fieldB = uiFieldB
                }
            }
            templateSaveJob = job
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

        /** DetailViewModel.awaitPendingStyleSaves()와 동일한 형태. */
        suspend fun awaitPendingStyleSaves(timeoutMillis: Long = 2_000L) {
            val pendingJobs =
                listOfNotNull(fieldASaveJob, templateSaveJob, fieldCSaveJob)
                    .filter { it.isActive }

            if (pendingJobs.isEmpty()) {
                return
            }

            withTimeoutOrNull(timeoutMillis) {
                pendingJobs.joinAll()
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

    // ---- 시나리오 C: 템플릿 적용 직후 이탈 — 템플릿이 온전히 저장된다 ----

    @Test
    fun templateApplied_awaitBeforeExit_templatePersists() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        val templateJob = vm.applyTemplate(newA = 100, newB = 200) {
            reachedDispatchPoint.await()
        }
        val exitJob = launch { exitScreen(vm, parentJob) }

        reachedDispatchPoint.complete(Unit)
        templateJob.join()
        exitJob.join()

        assertFalse(templateJob.isCancelled)
        assertEquals(100, vm.room.fieldA)
        assertEquals(200, vm.room.fieldB)
    }

    // ---- 시나리오 D: 템플릿 적용 후 개별 조작 직후 이탈 — 둘 다 보존된다 ----

    @Test
    fun templateAppliedThenIndividualEdit_awaitBeforeExit_bothPersist() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        vm.applyTemplate(newA = 100, newB = 200).join()
        assertEquals(100, vm.room.fieldA)
        assertEquals(200, vm.room.fieldB)

        val editJob = vm.saveFieldA(newValue = 55) {
            reachedDispatchPoint.await()
        }
        val exitJob = launch { exitScreen(vm, parentJob) }

        reachedDispatchPoint.complete(Unit)
        editJob.join()
        exitJob.join()

        assertFalse(editJob.isCancelled)
        assertEquals(55, vm.room.fieldA) // 마지막 개별 조작이 살아남고
        assertEquals(200, vm.room.fieldB) // 템플릿의 나머지 값도 그대로 유지된다
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
}
