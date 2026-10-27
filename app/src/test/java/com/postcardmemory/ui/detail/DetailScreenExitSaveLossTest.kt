package com.postcardmemory.ui.detail

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailViewModel의 슬라이더 계열 저장(saveStampPhotoScale, saveBackgroundPatternDensity,
 * saveMessageTextScale 등)은 viewModelScope.launch로 실행된다.
 * DetailScreen.kt의 controlsEnabled는 backgroundUpdateState/fontUpdateState/
 * layoutUpdateState/dateFormatUpdateState/imageUpdateState만 확인할 뿐, 이
 * 슬라이더 저장군에는 대응하는 Saving 상태가 없어 저장이 끝나기 전에도
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
        var fieldC: Int = 0
    }

    /** viewModelScope에 해당 — ViewModelStore.clear()가 parentJob을 취소하면 함께 취소된다. */
    private class FakeViewModel(parentJob: Job) {
        var uiFieldA: Int = 0
        var uiFieldC: Int = 0
        val room = FakeRoom()
        private val scope = CoroutineScope(parentJob)
        private val styleWriteMutex = Mutex()
        private var fieldASaveJob: Job? = null

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


        /**
         * DetailViewModel.awaitPendingStyleSaves()와 동일한 형태 — 단, 실제
         * 코드처럼 fieldASaveJob만 기다리고 아래
         * updateFieldC(색/패턴/폰트/레이아웃/날짜형식처럼 Job 추적이 없는
         * styleWriteMutex 저장군을 대표)는 목록에 없다.
         */
        suspend fun awaitPendingStyleSaves() {
            val pendingJobs =
                listOfNotNull(fieldASaveJob)
                    .filter { it.isActive }

            if (pendingJobs.isEmpty()) {
                return
            }

            withTimeoutOrNull(2_000L) {
                pendingJobs.joinAll()
            }
        }

        /**
         * DetailViewModel.updateBackgroundColor/updateBackgroundPattern/
         * updateMessageFont/updateLayoutStyle/updateDateFormat과 동일한 형태:
         * styleWriteMutex로 직렬화되지만 Job을 클래스 필드에 보관하지 않는
         * "jobless" 저장. awaitPendingStyleSaves()가 이 저장을 기다릴 방법이
         * 없다는 것을 보이기 위한 대역이다. 실제 코드처럼 DetailScreen이
         * 이 Job을 사용할 방법은 없으므로, 아래 반환값은 테스트가 cancel 이후
         * 코루틴이 실제로 취소 처리를 마쳤는지 결정론적으로 join하기 위한
         * 것일 뿐 production 경로를 대표하지 않는다.
         */
        fun updateFieldC(
            newValue: Int,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            uiFieldC = newValue

            return scope.launch {
                beforeWrite()
                styleWriteMutex.withLock {
                    room.fieldC = uiFieldC
                }
            }
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

    // ---- 시나리오 E: awaitPendingStyleSaves()를 기다려도 Job 추적이 없는
    //      저장(배경색/배경 패턴/폰트/레이아웃/날짜 형식과 동일한 형태)은
    //      여전히 유실된다 ----
    //
    // DetailScreen.navigateBackAfterPendingStyleSaves()는 뒤로 가기 전
    // viewModel.awaitPendingStyleSaves()를 기다린 뒤에만 onNavigateBack()을
    // 호출한다(5c17c2d). 하지만 DetailViewModel.updateBackgroundColor()/
    // updateBackgroundPattern()/updateMessageFont()/updateLayoutStyle()/
    // updateDateFormat()은 styleWriteMutex로 직렬화될 뿐 Job을 어디에도
    // 보관하지 않아 awaitPendingStyleSaves()의 listOfNotNull(...)에 애초에
    // 들어가지 않는다. 그 결과 이 저장군은 await가 끝나자마자(할 일이
    // 없다고 판단하고) 즉시 진행되는 navigation에 의해 여전히 취소될 수
    // 있다 — 아이콘 뒤로 가기 버튼은 controlsEnabled(backgroundUpdateState
    // 등 Saving 플래그)로 막히지만, 시스템 back(BackHandler)은 그 플래그를
    // 전혀 확인하지 않으므로 이 경로에서 특히 그렇다.
    @Test
    fun awaitPendingStyleSaves_doesNotCoverJoblessSave_fieldCStillLost() = runBlocking {
        val parentJob = SupervisorJob()
        val vm = FakeViewModel(parentJob)
        val reachedDispatchPoint = CompletableDeferred<Unit>()

        val fieldCJob = vm.updateFieldC(newValue = 7) {
            reachedDispatchPoint.await()
        }

        // 실제 DetailScreen처럼 이탈 전 awaitPendingStyleSaves()를 기다린다.
        // fieldASaveJob이 비어 있으므로 즉시 반환된다.
        vm.awaitPendingStyleSaves()

        // await가 fieldC 저장을 애초에 몰랐으므로, 아직 Dispatchers.IO
        // 디스패치 지점(beforeWrite)에 멈춰 있는 채로 scope가 취소된다.
        parentJob.cancel()
        fieldCJob.join()

        assertTrue(fieldCJob.isCancelled)
        assertEquals(7, vm.uiFieldC) // 화면에는 마지막 값이 반영돼 있었지만
        assertEquals(0, vm.room.fieldC) // await까지 기다렸는데도 Room에는 끝내 쓰이지 못했다
    }
}
