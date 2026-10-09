package com.postcardmemory.ui.detail

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 화면을 나갈 때(DetailScreen의 뒤로가기) `awaitPendingStyleSaves()`가 아직 끝나지
 * 않은 저장을 기다려 줘야 마지막 조작이 Room에 남는다. 기다리는 대상은 DetailViewModel의
 * `Job?` 필드를 손으로 나열한 목록이라, 새 저장 경로를 만들면서 Job 필드만 선언하고 이
 * 목록에 넣는 것을 잊으면 컴파일도 기존 테스트도 통과한 채 "나가기 직전 조작 유실"이
 * 되살아난다(DetailScreenExitSaveLossTest가 Fake로 재현했던 바로 그 문제).
 *
 * 그래서 숫자나 이름 목록을 테스트에 다시 적지 않고, production 소스에서 선언된
 * `Job?` 필드 전체를 뽑아 `awaitPendingStyleSaves()`의 join 목록과 대조한다. 새 Job
 * 필드는 자동으로 검사 대상이 되며, 의도적으로 목록 밖에서 다루는 Job만 아래
 * [handledSeparately]에 이유와 함께 적는다.
 *
 * ViewModel은 JVM에서 만들 수 없어(Robolectric 없음 — StructureTestSource 참고)
 * 행동 테스트 대신 소스 텍스트로 확인한다. Job 없이 바로 launch만 하는 저장
 * ("jobless" 저장)은 필드가 없으니 이 테스트가 잡지 못한다.
 */
class PendingStyleSaveCoverageStructureTest {

    private val viewModelText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"
            )
        )
    }

    /**
     * join 목록에 넣지 않는 것이 맞는 Job과, 그 대신 함수 안에 있어야 하는 처리.
     *
     * draftAutosaveJob: 초안 자동저장 debounce라 join하면 남은 debounce 시간만큼
     * 괜히 기다린다. 대신 cancel한 뒤 persistDraftNow()로 즉시 저장한다
     * (flushDraftNow()와 같은 방식).
     */
    private val handledSeparately: Map<String, List<String>> = mapOf(
        "draftAutosaveJob" to listOf("draftAutosaveJob?.cancel()", "persistDraftNow()")
    )

    private fun declaredJobFields(): Set<String> =
        Regex("""(?m)^\s*(?:private\s+|internal\s+)?var\s+(\w+)\s*:\s*Job\?""")
            .findAll(viewModelText)
            .map { it.groupValues[1] }
            .toSet()

    private fun awaitPendingStyleSavesBody(): String {
        val start = viewModelText.indexOf("suspend fun awaitPendingStyleSaves()")
        assertTrue("awaitPendingStyleSaves()를 찾지 못함", start >= 0)
        val bodyStart = viewModelText.indexOf('{', start)
        var depth = 0
        for (i in bodyStart until viewModelText.length) {
            when (viewModelText[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return viewModelText.substring(bodyStart, i + 1)
                }
            }
        }
        error("awaitPendingStyleSaves() 본문의 끝을 찾지 못함")
    }

    /** `listOfNotNull( ... )` 괄호 안에 나열된 이름들 — 실제로 joinAll 하는 대상. */
    private fun joinedJobNames(body: String): Set<String> {
        val listStart = body.indexOf("listOfNotNull(")
        assertTrue("awaitPendingStyleSaves()에 listOfNotNull( 목록이 없음", listStart >= 0)
        val open = listStart + "listOfNotNull".length
        var depth = 0
        for (i in open until body.length) {
            when (body[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) {
                        return body.substring(open + 1, i)
                            .split(',')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .toSet()
                    }
                }
            }
        }
        error("listOfNotNull( 목록의 끝을 찾지 못함")
    }

    @Test
    fun everyDeclaredSaveJob_isJoinedOnExit_orExplicitlyHandledSeparately() {
        val declared = declaredJobFields()
        val body = awaitPendingStyleSavesBody()
        val joined = joinedJobNames(body)

        // 예외 목록이 실제 선언과 어긋나면(이름 변경·삭제) 이 테스트 자체가 낡은 것이다.
        handledSeparately.keys.forEach { name ->
            assertTrue(
                "별도 처리 예외 [$name]가 DetailViewModel에 Job? 필드로 선언돼 있지 않음 — " +
                    "추출이 깨졌거나 예외 목록이 낡았다",
                name in declared
            )
        }

        val missing = declared - handledSeparately.keys - joined
        assertTrue(
            "다음 Job은 선언돼 있지만 awaitPendingStyleSaves()가 기다리지 않는다: $missing — " +
                "저장 Job이면 listOfNotNull 목록에 넣고, 의도적으로 따로 다루는 Job이면 " +
                "이 테스트의 handledSeparately에 이유와 처리 방식을 적는다",
            missing.isEmpty()
        )

        handledSeparately.forEach { (name, requiredHandling) ->
            assertTrue(
                "[$name]는 join 목록과 별도 처리 양쪽에 동시에 있으면 안 됨",
                name !in joined
            )
            requiredHandling.forEach { snippet ->
                assertTrue(
                    "[$name]를 join하지 않는 대신 awaitPendingStyleSaves()에 `$snippet`이 있어야 함",
                    body.contains(snippet)
                )
            }
        }
    }
}
