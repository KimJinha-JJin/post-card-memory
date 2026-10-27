package com.postcardmemory.ui.detail

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 78일차 P0-2: 화면 이탈 상한 시간이 "대기"가 아니라 "저장"까지 끊던 문제.
 *
 * DetailViewModel 자체는 Room/Context 의존 때문에 순수 JUnit에서 만들 수
 * 없지만, 상한 시간과 저장 생명주기를 분리하는 규칙은
 * [launchSaveAndAwaitWithUiTimeout]이라는 실제 production 함수 하나에
 * 모여 있다. 아래 테스트는 그 production 함수를 직접 호출한다(replica 아님).
 *
 * [oldShape_savingInsideTheTimeout_getsCancelled_thisIsWhatWeFixed]는 수정
 * 전 구조를 그대로 재현해 두어, 언제든 예전 방식으로 되돌리면 어떤 손실이
 * 생기는지 실행 가능한 형태로 못박는다.
 */
class ExitSaveTimeoutTest {

    private fun saveScope() =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Test
    fun saveFinishingWithinTheTimeout_reportsCompleted() = runBlocking {
        val scope = saveScope()
        val saved = AtomicBoolean(false)

        val completed = launchSaveAndAwaitWithUiTimeout(
            saveScope = scope,
            timeoutMillis = 2_000L
        ) {
            delay(20.milliseconds)
            saved.set(true)
        }

        assertTrue(completed)
        assertTrue(saved.get())
        scope.cancel()
    }

    @Test
    fun saveSlowerThanTheTimeout_stopsTheWaitButStillCompletesTheSave() = runBlocking {
        val scope = saveScope()
        val saved = AtomicBoolean(false)

        val completed = launchSaveAndAwaitWithUiTimeout(
            saveScope = scope,
            timeoutMillis = 50L
        ) {
            delay(400.milliseconds)
            saved.set(true)
        }

        // 화면은 붙잡지 않는다.
        assertFalse(completed)
        // 그러나 저장은 취소되지 않았다 — 끝까지 진행된다.
        assertFalse(saved.get())
        scope.coroutineContext.job.children.forEach { it.join() }
        assertTrue(saved.get())
        scope.cancel()
    }

    @Test
    fun saveSurvivesEvenWhenTheExitCoroutineIsCancelledRightAfterTheTimeout() = runBlocking {
        // 실제 화면 이탈: 대기하던 coroutine(= rememberCoroutineScope)이
        // navigation 직후 사라진다. 저장은 그것과 무관하게 끝나야 한다.
        val scope = saveScope()
        val saved = AtomicBoolean(false)
        val exitScope = saveScope()

        val exitJob = exitScope.launch {
            launchSaveAndAwaitWithUiTimeout(
                saveScope = scope,
                timeoutMillis = 50L
            ) {
                delay(400.milliseconds)
                saved.set(true)
            }
        }

        exitJob.join()
        exitScope.cancel()

        assertFalse(saved.get())
        scope.coroutineContext.job.children.forEach { it.join() }
        assertTrue(saved.get())
        scope.cancel()
    }

    @Test
    fun oldShape_savingInsideTheTimeout_getsCancelled_thisIsWhatWeFixed() = runBlocking {
        // 수정 전 구조: withTimeoutOrNull 안에서 저장을 직접 실행.
        val saved = AtomicBoolean(false)

        val result = withTimeoutOrNull(50.milliseconds) {
            delay(400.milliseconds)
            saved.set(true)
        }

        assertTrue(result == null)
        // 상한 시간이 저장 자체를 취소했다 — 재시도 주체도 없으므로 영구 유실.
        delay(500.milliseconds)
        assertFalse(saved.get())
    }
}
