package com.postcardmemory.ui.detail

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DetailViewModel.removeStickerBackground()는 Context/ML Kit 세그먼터에
 * 묶여 있어 Robolectric·mockk 없이 순수 JUnit에서 직접 호출할 수 없다(이
 * 프로젝트는 오늘 새 테스트 프레임워크를 추가하지 않는 방침). 대신 그
 * 함수가 실제로 쓰는 것과 동일한 취소 처리 형태
 * (runCatching → result.fold, onFailure에서 CancellationException만
 * 재전파)를 그대로 재현해, 화면 이탈로 인한 정상 취소가 Error 상태로
 * 잘못 바뀌지 않는지를 코루틴 수준에서 검증한다.
 */
class StickerBackgroundRemovalCancellationTest {

    private sealed class State {
        data class Success(val uri: String) : State()
        data class Error(val message: String) : State()
    }

    /** DetailViewModel.kt의 removeStickerBackground() 내부 처리와 동일한 형태. */
    private suspend fun runRemoval(
        outcome: () -> String,
        onState: (State) -> Unit
    ) {
        val result = runCatching { outcome() }

        result.fold(
            onSuccess = { uri -> onState(State.Success(uri)) },
            onFailure = { exception ->
                if (exception is CancellationException) {
                    throw exception
                }
                onState(State.Error("배경 제거를 준비하지 못했어. 잠시 뒤 다시 시도해줘."))
            }
        )
    }

    @Test
    fun success_updatesSuccessState() = runBlocking {
        var lastState: State? = null

        runRemoval(outcome = { "content://result" }) { lastState = it }

        assertTrue(lastState is State.Success)
    }

    @Test
    fun normalFailure_updatesErrorState_doesNotRethrow() = runBlocking {
        var lastState: State? = null

        runRemoval(outcome = { throw IllegalStateException("ml kit failed") }) {
            lastState = it
        }

        assertTrue(lastState is State.Error)
    }

    @Test
    fun cancellation_doesNotUpdateErrorState_andCoroutineEndsUpCancelled() = runBlocking {
        var errorStateSetCount = 0

        val job = launch {
            runRemoval(outcome = { throw CancellationException("screen left") }) { state ->
                if (state is State.Error) {
                    errorStateSetCount++
                }
            }
        }

        job.join()

        assertTrue(job.isCancelled)
        assertEquals(0, errorStateSetCount)
    }
}
