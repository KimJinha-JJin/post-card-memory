package com.postcardmemory.ui.detail

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 124일차: 엽서를 지우는 동안과 지운 뒤에 그 엽서의 초안이 다시 생기지 않게 막는다.
 *
 * 초안 저장과 엽서 삭제가 같은 [draftSaveMutex]로 줄을 서므로, 이미 실행 중인 저장은
 * 끝난 뒤에 삭제가 그 결과까지 지우고, 늦게 도착한 저장은 닫힌 문을 보고 아무것도
 * 쓰지 않는다(폴더도 만들지 않는다). 900ms 자동저장, 화면 이탈(ON_STOP) flush,
 * exitSaveScope flush가 모두 같은 저장 함수를 지나므로 검사는 이 한 곳이면 된다.
 *
 * 문은 DB 삭제가 실패했을 때만 다시 열린다. 취소나 예상 밖 예외로 끝나면 행이
 * 지워졌는지 알 수 없어 닫아 둔다 — 이미 삭제한 엽서의 잔여 파일이 되살아나는 쪽을
 * 막는다. 다른 엽서의 저장은 막지 않는다.
 *
 * ViewModel은 JVM에서 만들 수 없어, 이 순서 규칙을 실제 파일 저장·삭제 함수와 함께
 * 실행해 검증할 수 있도록 따로 둔다(DraftDeletionGateTest).
 */
internal class DraftDeletionGate(
    private val draftSaveMutex: Mutex
) {
    @Volatile
    private var closedPostcardId: Long? = null

    /** 삭제 요청 즉시 부른다. 삭제가 잠금을 얻기 전에 도착한 저장도 여기서 막힌다. */
    fun close(postcardId: Long) {
        closedPostcardId = postcardId
    }

    /** 잠금 안에서 문이 열려 있을 때만 [write]를 실행하고, 닫혀 있으면 [whenClosed]를 돌려준다. */
    suspend fun <T> writeUnlessClosed(
        postcardId: Long,
        whenClosed: T,
        write: suspend () -> T
    ): T =
        draftSaveMutex.withLock {
            if (closedPostcardId == postcardId) whenClosed else write()
        }

    /**
     * 잠금을 쥔 채 [delete]를 실행한다. [rowDeleted]가 false면 엽서가 살아 있으므로
     * 잠금을 놓기 전에 문을 연다 — 기다리던 저장은 그대로 초안을 쓸 수 있다.
     */
    suspend fun <T> runDeletion(
        postcardId: Long,
        delete: suspend () -> T,
        rowDeleted: (T) -> Boolean
    ): T =
        draftSaveMutex.withLock {
            val result = delete()
            if (!rowDeleted(result) && closedPostcardId == postcardId) {
                closedPostcardId = null
            }
            result
        }
}
