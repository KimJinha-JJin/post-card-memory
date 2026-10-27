package com.postcardmemory.ui.detail

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 화면 이탈 직전 마지막 저장을 시작하고, **대기만** 상한 시간으로 끊는다.
 *
 * [save]는 [saveScope]에서 실행되므로, 상한 시간이 지나 이 함수가 돌아간
 * 뒤에도(그리고 호출한 coroutine이 취소된 뒤에도) 저장은 계속 진행돼
 * 완료된다. `withTimeoutOrNull { save() }`처럼 저장을 대기 coroutine
 * 안에서 직접 실행하면 상한 시간이 저장 자체를 취소해 사용자의 마지막
 * 편집이 사라지는데, 그 결합을 끊는 것이 이 함수의 유일한 목적이다.
 *
 * @return 상한 시간 안에 저장이 끝났으면 true. false는 "저장 실패"가 아니라
 *   "아직 진행 중이라 기다리지 않고 돌아간다"는 뜻이다.
 */
internal suspend fun launchSaveAndAwaitWithUiTimeout(
    saveScope: CoroutineScope,
    timeoutMillis: Long,
    save: suspend () -> Unit
): Boolean {
    val saveJob = saveScope.launch { save() }

    return withTimeoutOrNull(timeoutMillis.milliseconds) {
        saveJob.join()
    } != null
}
