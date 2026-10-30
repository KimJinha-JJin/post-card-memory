package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 119일차 사진 스티커 저장 생애주기 안정화의 연결 구조를 고정한다.
 *
 * DetailViewModel은 Context/Uri/MLKit에 의존해 순수 JUnit에서 만들 수 없으므로
 * (ConfirmSaveHistoryClearStructureTest 참고) 소스 텍스트에서 호출 위치와 순서를 확인한다.
 * 판정 로직 자체는 hasUnpersistedDraftEdits(아래 순수 테스트)와
 * ConfirmedStickerBackgroundCleanupTest가 검증한다. 실제 이탈 순서·파일 수는 실기기 QA 몫이다.
 */
class StickerStorageLifecycleStructureTest {

    private val source: String by lazy {
        listOf(
            "src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt",
            "app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt"
        )
            .map { File(it) }
            .firstOrNull { it.exists() }
            ?.readText()
            ?: error("DetailViewModel.kt를 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    private fun functionBody(signature: String): String {
        val start = source.indexOf(signature)
        assertTrue("$signature 를 찾지 못했다", start >= 0)
        val open = source.indexOf('{', start)
        var depth = 0
        for (index in open until source.length) {
            when (source[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(open, index + 1)
                }
            }
        }
        error("$signature 의 끝을 찾지 못했다")
    }

    private fun assertOrdered(body: String, vararg markers: String) {
        var previous = -1
        markers.forEach { marker ->
            val index = body.indexOf(marker, previous + 1)
            assertTrue("[$marker]가 앞 단계 뒤에 없다", index > previous)
            previous = index
        }
    }

    @Test
    fun hasUnpersistedDraftEdits_onlyWhenEditsAreNewerThanLastPersist() {
        assertFalse(hasUnpersistedDraftEdits(editGeneration = 0L, persistedGeneration = 0L))
        assertFalse(hasUnpersistedDraftEdits(editGeneration = 4L, persistedGeneration = 4L))
        assertTrue(hasUnpersistedDraftEdits(editGeneration = 5L, persistedGeneration = 4L))
    }

    @Test
    fun everyDraftAutosaveSchedule_bumpsTheEditGeneration() {
        assertTrue(
            functionBody("private fun scheduleDraftAutosave()")
                .contains("draftEditGeneration.incrementAndGet()")
        )
    }

    @Test
    fun flushDraftNow_skipsWhenNothingChangedSinceLastPersist() {
        assertOrdered(
            functionBody("fun flushDraftNow()"),
            "hasUnpersistedDraftEdits(",
            "return",
            "persistDraftNow()"
        )
    }

    @Test
    fun persistDraftNow_marksGenerationPersistedOnlyWhenDraftWasWritten() {
        val body = functionBody("private suspend fun persistDraftNow()")
        assertOrdered(
            body,
            "val snapshotEditGeneration = draftEditGeneration.get()",
            "if (promoted != null) {",
            "draftPersistedGeneration.accumulateAndGet(snapshotEditGeneration"
        )
        assertOrdered(
            body,
            "_photoStickers.value = promoted",
            "rememberPromotedStickerCacheSources("
        )
    }

    @Test
    fun loadAndRevert_resetPersistedGeneration() {
        assertTrue(
            functionBody("fun loadStickerSealStateAndAutoRestoreDraft(")
                .contains("draftPersistedGeneration.set(0L)")
        )
        assertTrue(
            functionBody("fun revertToConfirmedState()")
                .contains("draftPersistedGeneration.set(draftEditGeneration.get())")
        )
    }

    @Test
    fun confirmSave_success_movesBaseline_handsCacheToSweep_thenPrunesDroppedBackgrounds() {
        val body = functionBody("fun saveEditsAndClearDraft(")
        assertOrdered(
            body,
            "val confirmEditGeneration = draftEditGeneration.get()",
            "ConfirmedStickerBackgroundCleanup.readStickerIds(",
            "persistStickerEditState(postcardId)",
            "PostcardDraftStorage.deleteDraft(context, postcardId)",
            "confirmedStickersBaseline = _photoStickers.value",
            "draftPersistedGeneration.accumulateAndGet(confirmEditGeneration",
            "stickerCleanupCandidates.addAll(promotedCacheSources)",
            "clearStickerHistory()",
            "if (allSaved && previouslyConfirmedStickerIds != null) {",
            "confirmedStickersBaseline +",
            "stickerUndoStack.flatMap",
            "ConfirmedStickerBackgroundCleanup.deleteDroppedBackgrounds("
        )
    }

    @Test
    fun droppedBackgroundCleanup_isOnlyCalledFromConfirmSave() {
        assertTrue(
            Regex(Regex.escape("ConfirmedStickerBackgroundCleanup.deleteDroppedBackgrounds("))
                .findAll(source).count() == 1
        )
    }
}
