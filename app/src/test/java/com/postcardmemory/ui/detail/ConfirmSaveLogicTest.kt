package com.postcardmemory.ui.detail

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 완료 버튼(확정 저장)의 성공/실패 판정과 중복 입력 방지 판정을 검증한다.
 * DetailViewModel은 Context/Uri/MLKit 등 Android 인프라에 의존해 이
 * 프로젝트의 순수 JUnit 환경에서 직접 인스턴스화할 수 없으므로(Robolectric
 * 미사용, PostcardEditDraftTest.kt 참고), saveEditsAndClearDraft가 실제로
 * 사용하는 순수 판정 함수만 분리해 검증한다.
 */
class ConfirmSaveLogicTest {

    @Test
    fun shouldConfirmSaveSucceed_whenStickersAndSealsBothSaved() {
        assertTrue(
            shouldConfirmSaveSucceed(stickersSaved = true, sealsSaved = true)
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenSealsSaveFails() {
        assertFalse(
            shouldConfirmSaveSucceed(stickersSaved = true, sealsSaved = false)
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenStickersSaveFails() {
        assertFalse(
            shouldConfirmSaveSucceed(stickersSaved = false, sealsSaved = true)
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenBothFail() {
        assertFalse(
            shouldConfirmSaveSucceed(stickersSaved = false, sealsSaved = false)
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_whenStickersSealsAndDoodlesAllSaved() {
        assertTrue(
            shouldConfirmSaveSucceed(
                stickersSaved = true,
                sealsSaved = true,
                doodlesSaved = true
            )
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenDoodlesSaveFailsEvenIfRestSucceed() {
        assertFalse(
            shouldConfirmSaveSucceed(
                stickersSaved = true,
                sealsSaved = true,
                doodlesSaved = false
            )
        )
    }

    @Test
    fun canStartConfirmSave_blocksWhileSaving() {
        assertFalse(canStartConfirmSave(ConfirmSaveState.Saving))
    }

    @Test
    fun canStartConfirmSave_allowsFromIdle() {
        assertTrue(canStartConfirmSave(ConfirmSaveState.Idle))
    }

    @Test
    fun canStartConfirmSave_allowsRetryAfterFailed() {
        assertTrue(canStartConfirmSave(ConfirmSaveState.Failed))
    }

    @Test
    fun canStartConfirmSave_allowsAfterPreviousSaveSucceeded() {
        assertTrue(canStartConfirmSave(ConfirmSaveState.Saved))
    }
}
