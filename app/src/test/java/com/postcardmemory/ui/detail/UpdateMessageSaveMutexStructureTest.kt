package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 57일차: `updateMessage()`가 다른 style 저장 함수(`updateBackMessage`,
 * `updateBackRecipientModifier` 등)와 같은 styleWriteMutex 저장 문법에서
 * 이탈해 있었다 — mutex 없이 호출 시점의 전체 Postcard snapshot을 잡아뒀다가
 * 비동기 저장이 끝난 뒤 그 오래된 snapshot으로 `_postcard.value`를 덮어써서,
 * 저장이 진행되는 동안 다른 style 값이 바뀌면 그 값이 과거로 되돌아갈 수
 * 있었다. Fake 기반 race 테스트(`StyleSaveRaceTest` 등)는 안전 패턴 자체가
 * 올바른지 검증하고, 이 테스트는 production `updateMessage()`가 실제로 그
 * 패턴을 계속 쓰는지만 소스 텍스트 기준으로 감시한다.
 */
class UpdateMessageSaveMutexStructureTest {

    private fun readSource(candidates: List<String>): String {
        val file = candidates
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?: error(
                "소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                    "candidates=$candidates"
            )
        return file.readText()
    }

    private val viewModelText: String by lazy {
        readSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt",
                "app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"
            )
        )
    }

    private val updateMessageBody: String by lazy {
        val start = viewModelText.indexOf("fun updateMessage(")
        assertTrue(
            "updateMessage() 선언을 찾지 못함",
            start >= 0
        )
        val nextFunStart =
            viewModelText.indexOf("\n    fun ", start + 1)
        assertTrue(
            "updateMessage() 다음 함수 경계를 찾지 못함",
            nextFunStart > start
        )
        viewModelText.substring(start, nextFunStart)
    }

    private val launchBlockStart: Int by
        lazy { updateMessageBody.indexOf("viewModelScope.launch") }

    @Test
    fun updateMessage_usesStyleWriteMutex() {
        assertTrue(
            "updateMessage()가 styleWriteMutex.withLock을 사용해야 함(다른 style 저장 함수와 동일한 직렬화 문법)",
            updateMessageBody.contains("styleWriteMutex.withLock")
        )
    }

    @Test
    fun updateMessage_reRedsLatestStateInsideMutex() {
        val lockIndex =
            updateMessageBody.indexOf("styleWriteMutex.withLock")
        assertTrue(
            "styleWriteMutex.withLock 블록을 찾지 못함",
            lockIndex >= 0
        )
        val afterLock = updateMessageBody.substring(lockIndex)
        assertTrue(
            "mutex 획득 후 최신 _postcard.value를 다시 읽어야 함(호출 시점의 오래된 snapshot을 그대로 쓰면 안 됨)",
            afterLock.contains("val latest =") &&
                afterLock.indexOf("_postcard.value") <
                    afterLock.indexOf("repository.updatePostcardMessage(")
        )
        val repoCallIndex =
            afterLock.indexOf("repository.updatePostcardMessage(")
        assertTrue(
            "repository.updatePostcardMessage() 호출을 찾지 못함",
            repoCallIndex >= 0
        )
        val repoCallArgs =
            afterLock.substring(repoCallIndex, afterLock.indexOf(")", repoCallIndex))
        assertTrue(
            "저장 시점에는 latest(mutex 안에서 다시 읽은 최신 state)의 message를 써야 함, 호출 시점 인자를 그대로 쓰면 안 됨",
            repoCallArgs.contains("latest.message")
        )
    }

    @Test
    fun updateMessage_doesNotRewriteStaleFullSnapshotAfterAsyncSave() {
        assertTrue(
            "viewModelScope.launch 블록 경계를 찾지 못함",
            launchBlockStart >= 0
        )
        val asyncBody = updateMessageBody.substring(launchBlockStart)
        assertFalse(
            "비동기 저장 완료 후 호출 시점의 전체 currentPostcard snapshot으로 _postcard.value를 다시 쓰면 안 됨" +
                "(다른 style 값이 그 사이 바뀌었다면 과거로 되돌아감) — message 필드만 반영해야 함",
            Regex("""_postcard\.value\s*=\s*\n?\s*currentPostcard\.copy\(""")
                .containsMatchIn(asyncBody)
        )
    }

    @Test
    fun updateMessage_optimisticSyncUpdateOnlyTouchesMessageField() {
        assertTrue(
            "launch 이전(동기 구간)에서 _postcard.value를 currentPostcard.copy(message = ...)로 낙관적 갱신해야 함(updateBackMessage와 동일 문법)",
            launchBlockStart >= 0
        )
        val syncBody = updateMessageBody.substring(0, launchBlockStart)
        assertTrue(
            "동기 구간에 _postcard.value = currentPostcard.copy(message = ...) 형태가 있어야 함",
            Regex("""_postcard\.value\s*=\s*\n\s*currentPostcard\.copy\(\s*\n\s*message = normalizedMessage""")
                .containsMatchIn(syncBody)
        )
    }
}
