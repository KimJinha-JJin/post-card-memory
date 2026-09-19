package com.postcardmemory.utils

import java.io.File
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * "파일을 먼저 만들고, 그 경로를 DB에 커밋한다"는 흐름에서 파일의 소유권이
 * 언제 넘어가는지를 한 곳에 못박는다.
 *
 * - [produce]가 만든 파일은 그 순간에는 아직 **임시 소유**다. 어떤 저장본도
 *   이 경로를 가리키지 않기 때문이다.
 * - [commit]이 정상적으로 끝나야 소유권이 저장본(DB)으로 넘어간다. 이후에는
 *   이 함수가 파일을 건드리지 않는다.
 * - [commit]이 실패하거나 취소되면 그 파일을 참조할 주체가 영원히 생기지
 *   않으므로 지운다. 지우지 않으면 어느 화면에도 안 보이면서 저장공간만
 *   차지하는 고아 파일이 남는다.
 *
 * 정리는 [NonCancellable]에서 한다 — 취소 때문에 여기 왔는데 정리까지 다시
 * 취소되면 애초에 정리가 되지 않는다.
 *
 * 사용자가 고른 원본(외부 갤러리 URI 등)에는 쓰지 않는다. 이 함수가 지우는
 * 것은 오직 [produce]가 이번 호출에서 **새로 만든** 앱 소유 파일뿐이다.
 */
internal suspend fun <T> withProvisionalFile(
    produce: suspend () -> File,
    commit: suspend (File) -> T
): T {
    val file = produce()
    var committed = false

    try {
        val result = commit(file)
        committed = true
        return result
    } finally {
        if (!committed) {
            withContext(NonCancellable) {
                file.delete()
            }
        }
    }
}
