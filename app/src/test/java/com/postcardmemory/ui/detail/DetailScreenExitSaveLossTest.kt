package com.postcardmemory.ui.detail

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailViewModel의 슬라이더 계열 저장(saveStampPhotoScale, saveBackgroundPatternDensity,
 * saveMessageTextScale 등)과 persistTemplateStyle(템플릿 적용)은 viewModelScope.launch로
 * 실행된다. DetailScreen.kt의 controlsEnabled는 backgroundUpdateState/fontUpdateState/
 * layoutUpdateState/dateFormatUpdateState/imageUpdateState만 확인할 뿐, 이
 * 슬라이더·템플릿 저장군에는 대응하는 Saving 상태가 없어 저장이 끝나기 전에도
 * 뒤로 가기 버튼이 계속 활성 상태다. MainActivity.kt의 composable("detail/{id}")는
 * 커스텀 전환 애니메이션이 없어 popBackStack() 직후 거의 바로 NavBackStackEntry의
 * ViewModelStore가 clear()되고, onCleared() -> viewModelScope.cancel()이 뒤따른다.
 *
 * Context/Room/Hilt에 묶여 있어 순수 JUnit에서 DetailViewModel을 직접 인스턴스화할
 * 수 없으므로(StyleSaveRaceTest와 동일한 제약), 실제 구조 — 공유 Mutex로 DAO 쓰기
 * 구간을 직렬화하고 Mutex 진입 시점에 최신 UI 상태를 다시 읽는 구조 — 를 FakeRoom /
 * FakeViewModel로 재현한다. 다른 점은 viewModelScope 자체를 화면 이탈 시 취소되는
 * 별도 Job으로 만들어, "저장이 실제 DAO 쓰기(Mutex)에 닿기 전에 뒤로 가기가
 * 온다"는 상황을 CompletableDeferred 게이트로 결정적으로 재현한다는 것이다.
 */
class DetailScreenExitSaveLossTest {

    private class FakeRoom {
        var fieldA: Int = 0
        var fieldB: Int = 0
    }

    /** viewModelScope에 해당 — ViewModelStore.clear()가 parentJob을 취소하면 함께 취소된다. */
    private class FakeViewModel(parentJob: Job) {
        var uiFieldA: Int = 0
        var uiFieldB: Int = 0
        val room = FakeRoom()
        private val scope = CoroutineScope(parentJob)
        private val styleWriteMutex = Mutex()
        private var fieldASaveJob: Job? = null
        private var templateSaveJob: Job? = null

        /** saveStampPhotoScale() 등 슬라이더 저장 함수와 동일한 형태. */
        fun saveFieldA(
            newValue: Int,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            uiFieldA = newValue // 낙관적 갱신 — 화면에는 즉시 반영된다.

            fieldASaveJob?.cancel()
            val job = scope.launch {
                beforeWrite() // Dispatchers.IO로 디스패치되기까지의 구간에 해당
                styleWriteMutex.withLock {
                    room.fieldA = uiFieldA // Mutex 획득 시점의 최신값을 다시 읽어서 쓴다
                }
            }
            fieldASaveJob = job
            return job
        }

        /** persistTemplateStyle()과 동일한 형태(두 필드 일괄 저장). */
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
    }

    // ---- 시나리오 A: 값 변경 직후 화면 이탈 — 저장이 DAO 쓰기 전에 취소된다 ----

    @Test
    fun scopeCancelledBeforeWrite_lastValueNeverReachesRoom() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        val job = vm.saveFieldA(newValue = 42) {
            reachedDispatchPoint.await()
        }

        // 사용자가 저장이 끝나기 전에 뒤로 가기 -> ViewModelStore.clear() -> onCleared()
        // -> viewModelScope.cancel()에 해당.
        parentJob.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(42, vm.uiFieldA) // 화면에는 마지막 값이 반영돼 있었지만
        assertEquals(0, vm.room.fieldA) // Room에는 끝내 쓰이지 못했다 -> 재진입 시 예전 값을 보게 된다
    }

    // ---- 시나리오 B: A -> B로 빠르게 변경 후 이탈 — B가 유실되고 오래된 A만 남는다 ----

    @Test
    fun rapidAThenB_scopeCancelledDuringB_onlyStaleAPersists() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        vm.saveFieldA(newValue = 1).join() // A는 정상적으로 저장 완료
        assertEquals(1, vm.room.fieldA)

        val jobB = vm.saveFieldA(newValue = 2) {
            reachedDispatchPoint.await()
        }
        parentJob.cancel() // B가 쓰기 전에 화면 이탈
        jobB.join()

        assertTrue(jobB.isCancelled)
        assertEquals(2, vm.uiFieldA) // 화면(=이탈 직전 마지막 상태)은 2였지만
        assertEquals(1, vm.room.fieldA) // Room에는 오래된 1만 남는다 -> 재진입 시 2가 사라져 있다
    }

    // ---- 시나리오 C: 템플릿 적용 직후 화면 이탈 — 템플릿 자체가 저장되지 못한다 ----

    @Test
    fun templateApplied_scopeCancelledBeforeWrite_templateNeverPersists() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        val job = vm.applyTemplate(newA = 100, newB = 200) {
            reachedDispatchPoint.await()
        }

        parentJob.cancel()
        job.join()

        assertTrue(job.isCancelled)
        assertEquals(100, vm.uiFieldA)
        assertEquals(200, vm.uiFieldB)
        assertEquals(0, vm.room.fieldA) // 템플릿을 적용한 화면을 보고 나갔지만 Room은 그대로다
        assertEquals(0, vm.room.fieldB)
    }

    // ---- 시나리오 D: 템플릿 적용 후 개별 조작 직후 이탈 — 마지막 개별 조작이 유실된다 ----

    @Test
    fun templateAppliedThenIndividualEdit_scopeCancelledBeforeWrite_individualEditLost() =
        runBlocking {
            val parentJob = SupervisorJob()
            val vm = FakeViewModel(parentJob)
            val reachedDispatchPoint = CompletableDeferred<Unit>()

            vm.applyTemplate(newA = 100, newB = 200).join() // 템플릿은 정상적으로 저장 완료
            assertEquals(100, vm.room.fieldA)
            assertEquals(200, vm.room.fieldB)

            val job = vm.saveFieldA(newValue = 55) {
                reachedDispatchPoint.await()
            }
            parentJob.cancel() // 개별 조작이 쓰기 전에 화면 이탈
            job.join()

            assertTrue(job.isCancelled)
            assertEquals(55, vm.uiFieldA) // 마지막으로 조작한 값은 55였지만
            assertEquals(100, vm.room.fieldA) // Room에는 템플릿의 100만 남는다 -> 마지막 조작이 사라져 있다
            assertEquals(200, vm.room.fieldB) // 템플릿의 나머지 값은 그대로 유지된다
        }
}
