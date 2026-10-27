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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailViewModel의 개별 스타일 저장(예: saveStampPhotoScale)은
 * Context/Room/Hilt에 묶여 있어 순수 JUnit에서 직접 호출할 수 없다.
 * 대신 실제 저장이 쓰는 공유 Mutex, 최신 UI 상태 재읽기, 취소와 실패
 * 롤백 흐름을 가짜 UI 상태와 저장소로 재현해 검증한다.
 *
 * beforeWrite는 실제 코드의 "Dispatchers.IO로 디스패치되고 실제 DAO 쓰기가
 * 시작되기까지 걸리는 시간"에 해당한다 — 완료 순서를 뒤섞으려면 이 지연을
 * Mutex 진입 *전*에 둬야 한다. Mutex를 쥔 채로 오래 기다리게 하면(진짜 지연
 * DB 호출처럼) 그 사이 다른 저장은 Mutex 자체를 얻지 못해 그냥 대기할 뿐이며,
 * 이는 실제 구현에서도 동일한 동작이다(직렬화가 의도한 트레이드오프).
 */
class StyleSaveRaceTest {

    /** Postcard의 스타일 필드 중 경합 검증에 필요한 fieldA만 축약한 모델. */
    private class FakeUiState {
        var fieldA: Int = 0
    }

    /** 실제 Room을 대신하는 가짜 저장소 — 최종적으로 무엇이 "저장"됐는지 확인한다. */
    private class FakeRoom {
        var fieldA: Int = 0
    }

    private class FakeViewModel {
        val ui = FakeUiState()
        val room = FakeRoom()
        val errors = mutableListOf<String>()
        private val styleWriteMutex = Mutex()
        private var fieldASaveJob: Job? = null

        /** saveStampPhotoScale() 등 개별 필드 저장 함수와 동일한 형태(fieldA만 다룸). */
        fun saveFieldA(
            scope: CoroutineScope,
            newValue: Int,
            failWith: Exception? = null,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            val previousValue = ui.fieldA
            ui.fieldA = newValue // 낙관적 갱신

            fieldASaveJob?.cancel()
            val job = scope.launch {
                try {
                    beforeWrite()
                    val written = styleWriteMutex.withLock {
                        val latest = ui.fieldA // Mutex를 획득한 이 순간 다시 읽기
                        if (failWith != null) throw failWith
                        room.fieldA = latest
                        latest
                    }
                    ui.fieldA = written
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    // 이미 더 최신 조작이 반영됐다면(fieldA가 더 이상 이 저장이
                    // 낙관적으로 썼던 값이 아니라면) 되돌리지 않는다 — 그렇지
                    // 않으면 늦게 실패를 확인한 옛 저장이 그 사이 성공적으로
                    // 커밋된 최신 값을 덮어쓴다.
                    if (ui.fieldA == newValue) {
                        ui.fieldA = previousValue
                    }
                    errors += "fieldA save failed"
                }
            }
            fieldASaveJob = job
            return job
        }

    }

    // ---- 취소는 실패가 아니다 ----

    @Test
    fun cancelledIndividualSave_doesNotRecordError_doesNotRollbackNewerValue() = runBlocking {
        val vm = FakeViewModel()
        val neverCompletes = CompletableDeferred<Unit>()

        val staleJob = vm.saveFieldA(this, newValue = 1) {
            neverCompletes.await() // 곧 취소될 것이므로 실제로 기다리지 않는다
        }
        val freshJob = vm.saveFieldA(this, newValue = 2) // staleJob을 취소시킴
        freshJob.join()
        staleJob.join()

        assertTrue(staleJob.isCancelled)
        assertTrue(vm.errors.isEmpty())
        assertEquals(2, vm.ui.fieldA)
        assertEquals(2, vm.room.fieldA)
    }

    // ---- 실제 저장 실패는 여전히 롤백된다(경합 방지가 실패 롤백을 없애면 안 됨) ----

    @Test
    fun realSaveFailure_stillRollsBackAndRecordsError() = runBlocking {
        val vm = FakeViewModel()

        val job = vm.saveFieldA(
            this,
            newValue = 42,
            failWith = IllegalStateException("db failed")
        )
        job.join()

        assertEquals(1, vm.errors.size)
        assertEquals(0, vm.ui.fieldA) // 실패 전(previousValue)으로 복원
        assertEquals(0, vm.room.fieldA) // Room에는 애초에 쓰기 자체가 실패했으므로 반영 안 됨
    }

    // ---- 실패 후에도 다음 정상 저장은 가능해야 한다 ----

    @Test
    fun afterFailure_nextSaveSucceedsNormally() = runBlocking {
        val vm = FakeViewModel()

        vm.saveFieldA(this, newValue = 42, failWith = IllegalStateException("db failed")).join()
        assertEquals(0, vm.ui.fieldA)

        vm.saveFieldA(this, newValue = 7).join()

        assertEquals(7, vm.ui.fieldA)
        assertEquals(7, vm.room.fieldA)
    }

}
