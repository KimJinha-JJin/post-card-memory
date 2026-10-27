package com.postcardmemory.ui.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 초안 복원 시 파일 유효성 검사에 쓰이는 순수 판정 함수를 검증한다.
 * PhotoStickerItem/Uri를 다루는 실제 복원 로직(DetailViewModel)은 Context와
 * android.net.Uri에 의존해 이 프로젝트의 순수 JUnit 환경에서 직접 검증할 수
 * 없으므로(Robolectric 미사용, PostcardEditDraftTest.kt 참고), 그 로직이
 * 실제로 사용하는 판정 함수만 분리해 검증한다.
 */
class DraftRestoreLogicTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun isReadableNonEmptyFile_trueForRealFileWithContent() {
        val file = tempFolder.newFile("sticker.png")
        file.writeText("fake-png-bytes")

        assertTrue(isReadableNonEmptyFile(file))
    }

    @Test
    fun isReadableNonEmptyFile_falseWhenFileDoesNotExist() {
        val missing = File(tempFolder.root, "does-not-exist.png")

        assertFalse(isReadableNonEmptyFile(missing))
    }

    @Test
    fun isReadableNonEmptyFile_falseWhenFileIsEmpty() {
        val emptyFile = tempFolder.newFile("empty.png")

        assertFalse(isReadableNonEmptyFile(emptyFile))
    }

    @Test
    fun shouldClearBackgroundRemoval_clearsWhenRemovedBgFileIsGone() {
        assertTrue(
            shouldClearBackgroundRemoval(
                isBackgroundRemoved = true,
                removedBgUsable = false
            )
        )
    }

    @Test
    fun shouldClearBackgroundRemoval_keepsWhenRemovedBgFileStillUsable() {
        assertFalse(
            shouldClearBackgroundRemoval(
                isBackgroundRemoved = true,
                removedBgUsable = true
            )
        )
    }

    @Test
    fun shouldClearBackgroundRemoval_noOpWhenBackgroundWasNeverRemoved() {
        // isBackgroundRemoved가 애초에 false면 removedBgUsable 값과 무관하게
        // 지울 이유가 없다(모순 상태가 아니므로).
        assertFalse(
            shouldClearBackgroundRemoval(
                isBackgroundRemoved = false,
                removedBgUsable = false
            )
        )
        assertFalse(
            shouldClearBackgroundRemoval(
                isBackgroundRemoved = false,
                removedBgUsable = true
            )
        )
    }
}
