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
 *
 * shouldConfirmSaveSucceed는 기본값 없이 여섯 종 결과를 모두 받는다. 각 테스트도
 * 여섯 값을 전부 명시해, 검증 대상이 아닌 종류가 기본값에 숨지 않게 한다.
 */
class ConfirmSaveLogicTest {

    @Test
    fun shouldConfirmSaveSucceed_whenStickersAndSealsBothSaved() {
        assertTrue(
            shouldConfirmSaveSucceed(
                stickersSaved = true,
                sealsSaved = true,
                doodlesSaved = true,
                textStickersSaved = true,
                maskingTapesSaved = true,
                labelStickersSaved = true
            )
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenSealsSaveFails() {
        assertFalse(
            shouldConfirmSaveSucceed(
                stickersSaved = true,
                sealsSaved = false,
                doodlesSaved = true,
                textStickersSaved = true,
                maskingTapesSaved = true,
                labelStickersSaved = true
            )
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenStickersSaveFails() {
        assertFalse(
            shouldConfirmSaveSucceed(
                stickersSaved = false,
                sealsSaved = true,
                doodlesSaved = true,
                textStickersSaved = true,
                maskingTapesSaved = true,
                labelStickersSaved = true
            )
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenBothFail() {
        assertFalse(
            shouldConfirmSaveSucceed(
                stickersSaved = false,
                sealsSaved = false,
                doodlesSaved = true,
                textStickersSaved = true,
                maskingTapesSaved = true,
                labelStickersSaved = true
            )
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenDoodlesSaveFailsEvenIfRestSucceed() {
        assertFalse(
            shouldConfirmSaveSucceed(
                stickersSaved = true,
                sealsSaved = true,
                doodlesSaved = false,
                textStickersSaved = true,
                maskingTapesSaved = true,
                labelStickersSaved = true
            )
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenTextStickersSaveFailsEvenIfRestSucceed() {
        assertFalse(
            shouldConfirmSaveSucceed(
                stickersSaved = true,
                sealsSaved = true,
                doodlesSaved = true,
                textStickersSaved = false,
                maskingTapesSaved = true,
                labelStickersSaved = true
            )
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenMaskingTapesSaveFailsEvenIfRestSucceed() {
        assertFalse(
            shouldConfirmSaveSucceed(
                stickersSaved = true,
                sealsSaved = true,
                doodlesSaved = true,
                textStickersSaved = true,
                maskingTapesSaved = false,
                labelStickersSaved = true
            )
        )
    }

    @Test
    fun shouldConfirmSaveSucceed_failsWhenLabelStickersSaveFailsEvenIfRestSucceed() {
        assertFalse(
            shouldConfirmSaveSucceed(
                stickersSaved = true,
                sealsSaved = true,
                doodlesSaved = true,
                textStickersSaved = true,
                maskingTapesSaved = true,
                labelStickersSaved = false
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
