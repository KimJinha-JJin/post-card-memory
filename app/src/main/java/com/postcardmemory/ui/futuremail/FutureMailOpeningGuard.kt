package com.postcardmemory.ui.futuremail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * "지금 개봉 중인 도착일" 집합을 들고 있는 가드.
 *
 * 연타로 같은 묶음이 두 번 열리는 것을 막는 것이 원래 목적인데, 표시를
 * 걸어두고 해제하는 코드가 서로 떨어져 있으면 중간에 예외가 났을 때 표시가
 * 남아 사용자가 **다시 시도할 수 없게 된다**. 그래서 해제를 호출부의
 * 성실함에 맡기지 않고 [releasingAfter]의 finally 안에 가둔다.
 *
 * 표시는 UI 스레드(클릭)에서 걸고 IO 스레드(개봉 완료)에서 푸는 양쪽
 * 접근이 있으므로 원자적으로 갱신되는 MutableStateFlow로 보관한다.
 */
internal class FutureMailOpeningGuard {

    private val opening = MutableStateFlow<Set<Long>>(emptySet())

    fun isOpening(key: Long): Boolean = key in opening.value

    /**
     * [key]를 개봉 중으로 표시한다. 이미 개봉 중이면 false를 돌려주며 이때는
     * 표시를 건드리지 않는다(= 호출부는 그냥 무시하면 된다).
     *
     * 연타 차단이 목적이므로 coroutine을 띄우기 **전에** 동기적으로 부른다.
     */
    fun beginOrSkip(key: Long): Boolean {
        var begun = false
        opening.update { current ->
            if (key in current) {
                current
            } else {
                begun = true
                current + key
            }
        }
        return begun
    }

    /**
     * [block]을 실행하고, 성공하든 예외로 끝나든 취소되든 [key]의 개봉 중
     * 표시를 반드시 해제한다. 예외는 그대로 다시 던져 호출부가 실패를
     * 삼키지 않게 한다 — 해제만 보장하고 실패는 숨기지 않는다.
     */
    suspend fun releasingAfter(key: Long, block: suspend () -> Unit) {
        try {
            block()
        } finally {
            opening.update { it - key }
        }
    }
}
