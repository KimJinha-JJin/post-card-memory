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
 * DetailViewModel의 개별 스타일 저장(예: saveStampPhotoScale)과 템플릿 일괄
 * 저장(persistTemplateStyle)은 Context/Room/Hilt에 묶여 있어 Robolectric·
 * mockk 없이 순수 JUnit에서 직접 호출할 수 없다(TemplateStyleSaveRollbackTest,
 * StickerBackgroundRemovalCancellationTest와 동일한 제약). 대신 두 종류의
 * 저장이 실제로 쓰는 것과 동일한 구조 — 공유 styleWriteMutex로 실제 DAO 쓰기
 * 구간을 직렬화하고, Mutex를 획득한 순간 "호출 당시 캡처한 값"이 아니라
 * "그 시점의 UI 상태"를 다시 읽어서 쓰는 구조 — 를 가짜 UI 상태(FakeUiState)와
 * 가짜 Room(FakeRoom)으로 재현해 검증한다.
 *
 * beforeWrite는 실제 코드의 "Dispatchers.IO로 디스패치되고 실제 DAO 쓰기가
 * 시작되기까지 걸리는 시간"에 해당한다 — 완료 순서를 뒤섞으려면 이 지연을
 * Mutex 진입 *전*에 둬야 한다. Mutex를 쥔 채로 오래 기다리게 하면(진짜 지연
 * DB 호출처럼) 그 사이 다른 저장은 Mutex 자체를 얻지 못해 그냥 대기할 뿐이며,
 * 이는 실제 구현에서도 동일한 동작이다(직렬화가 의도한 트레이드오프).
 */
class StyleSaveRaceTest {

    /** Postcard의 스타일 필드 중 경합 검증에 필요한 두 개(fieldA, fieldB)만 축약한 모델. */
    private class FakeUiState {
        var fieldA: Int = 0
        var fieldB: Int = 0
    }

    /** 실제 Room을 대신하는 가짜 저장소 — 최종적으로 무엇이 "저장"됐는지 확인한다. */
    private class FakeRoom {
        var fieldA: Int = 0
        var fieldB: Int = 0
        val writeLog = mutableListOf<String>()
    }

    private class FakeViewModel {
        val ui = FakeUiState()
        val room = FakeRoom()
        val errors = mutableListOf<String>()
        private val styleWriteMutex = Mutex()
        private var fieldASaveJob: Job? = null
        private var templateSaveJob: Job? = null

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
                        room.writeLog += "fieldA=$latest"
                        latest
                    }
                    ui.fieldA = written
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    ui.fieldA = previousValue
                    errors += "fieldA save failed"
                }
            }
            fieldASaveJob = job
            return job
        }

        /** persistTemplateStyle() + applyTemplate()과 동일한 형태(fieldA, fieldB 모두 다룸). */
        fun applyTemplate(
            scope: CoroutineScope,
            newA: Int,
            newB: Int,
            beforeWrite: suspend () -> Unit = {}
        ): Job {
            val previousA = ui.fieldA
            val previousB = ui.fieldB
            ui.fieldA = newA // 낙관적 갱신
            ui.fieldB = newB

            templateSaveJob?.cancel()
            val job = scope.launch {
                try {
                    beforeWrite()
                    styleWriteMutex.withLock {
                        val latestA = ui.fieldA // Mutex를 획득한 이 순간 다시 읽기
                        val latestB = ui.fieldB
                        room.fieldA = latestA
                        room.fieldB = latestB
                        room.writeLog += "template=($latestA,$latestB)"
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    ui.fieldA = previousA
                    ui.fieldB = previousB
                    errors += "template save failed"
                }
            }
            templateSaveJob = job
            return job
        }
    }

    // ---- 사례 A: 개별 저장이 늦게 완료돼도 템플릿을 덮어쓰면 안 된다 ----

    @Test
    fun individualStartsFirst_completesLate_templateStillWins() = runBlocking {
        val vm = FakeViewModel()
        val letIndividualProceed = CompletableDeferred<Unit>()

        // 1. 사진 크기 슬라이더 조절 시작 — 실제 DAO 쓰기 직전(Dispatchers.IO
        //    스케줄링 지연에 해당)까지만 진행하고 멈춰 있다.
        val individualJob = vm.saveFieldA(this, newValue = 8) {
            letIndividualProceed.await()
        }

        // 2. 곧바로 템플릿 적용 — 지연 없이 바로 Mutex를 얻어 즉시 커밋된다.
        val templateJob = vm.applyTemplate(this, newA = 100, newB = 200)
        templateJob.join()

        // 3. 뒤늦게 개별 저장이 실제 쓰기를 시도한다.
        letIndividualProceed.complete(Unit)
        individualJob.join()

        // Room 최종 상태는 템플릿이어야 한다 — 개별 저장이 Mutex를 늦게 얻어
        // 다시 읽은 값은 이미 템플릿이 반영된 100이므로 옛 값(8)으로 덮어쓰지 않는다.
        assertEquals(100, vm.room.fieldA)
        assertEquals(200, vm.room.fieldB)
        assertEquals(vm.ui.fieldA, vm.room.fieldA)
        assertEquals(vm.ui.fieldB, vm.room.fieldB)
        assertEquals(listOf("template=(100,200)", "fieldA=100"), vm.room.writeLog)
    }

    // ---- 사례 B: 템플릿 저장이 늦게 완료돼도 최신 개별 조작을 덮어쓰면 안 된다 ----

    @Test
    fun templateStartsFirst_completesLate_individualFieldPreserved() = runBlocking {
        val vm = FakeViewModel()
        val letTemplateProceed = CompletableDeferred<Unit>()

        // 1. 템플릿 적용 — 실제 DAO 쓰기 직전까지만 진행하고 멈춰 있다.
        val templateJob = vm.applyTemplate(this, newA = 100, newB = 200) {
            letTemplateProceed.await()
        }

        // 2. 즉시 fieldA(배경 강도 등)를 다시 조절 — 지연 없이 바로 커밋된다.
        val individualJob = vm.saveFieldA(this, newValue = 55)
        individualJob.join()

        // 3. 뒤늦게 템플릿 저장이 실제 쓰기를 시도한다.
        letTemplateProceed.complete(Unit)
        templateJob.join()

        // 최종 fieldA는 사용자가 마지막으로 조절한 값(55)이어야 하고,
        // fieldB는 템플릿 값(200)이 그대로 유지돼야 한다 — 템플릿이 Mutex를
        // 늦게 얻어 다시 읽은 값이 이미 55이므로 되돌리지 않는다.
        assertEquals(55, vm.room.fieldA)
        assertEquals(200, vm.room.fieldB)
        assertEquals(vm.ui.fieldA, vm.room.fieldA)
        assertEquals(vm.ui.fieldB, vm.room.fieldB)
        assertEquals(listOf("fieldA=55", "template=(55,200)"), vm.room.writeLog)
    }

    // ---- 사례 C: 여러 개별 저장 후 템플릿 적용 시 스타일 혼합이 없어야 한다 ----

    @Test
    fun multipleIndividualSaves_thenTemplate_noMixing() = runBlocking {
        val vm = FakeViewModel()
        val letFirstProceed = CompletableDeferred<Unit>()

        // fieldA를 두 번 연속 조절 — 두 번째가 첫 번째 Job을 취소한다.
        val firstJob = vm.saveFieldA(this, newValue = 1) {
            letFirstProceed.await()
        }
        val secondJob = vm.saveFieldA(this, newValue = 2)
        secondJob.join()

        val templateJob = vm.applyTemplate(this, newA = 100, newB = 200)
        templateJob.join()

        letFirstProceed.complete(Unit)
        firstJob.join()

        // 취소된 firstJob은 오류를 남기지 않고, 최종 상태는 순수하게 템플릿이어야 한다.
        assertTrue(vm.errors.isEmpty())
        assertEquals(100, vm.room.fieldA)
        assertEquals(200, vm.room.fieldB)
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
