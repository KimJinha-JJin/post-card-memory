package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * sticker_states/seal_states 확정 파일에 쓰이는 원자적 저장 로직을
 * 검증한다. Uri나 Context가 필요 없는 순수 File I/O라 Robolectric 없이도
 * 실제 파일로 검증할 수 있다.
 */
class ConfirmedEditStateStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun writeTextAtomically_thenRead_roundTrips() {
        val stateFile = File(tempFolder.newFolder("sticker_states"), "1.txt")

        val saved = ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = stateFile,
            content = "line-one\nline-two"
        )

        assertTrue(saved)
        assertEquals("line-one\nline-two", stateFile.readText())
    }

    @Test
    fun writeTextAtomically_leavesNoLeftoverTempFile() {
        val stateFile = File(tempFolder.newFolder("seal_states"), "2.txt")

        ConfirmedEditStateStorage.writeTextAtomically(stateFile, "content")

        val leftoverTemp = File(stateFile.parentFile, "2.txt.tmp")
        assertFalse(leftoverTemp.exists())
    }

    @Test
    fun writeTextAtomically_overwritesExistingConfirmedFile_onSuccess() {
        val stateFile = File(tempFolder.newFolder("sticker_states"), "3.txt")
        stateFile.writeText("old-confirmed-content")

        val saved = ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = stateFile,
            content = "new-confirmed-content"
        )

        assertTrue(saved)
        assertEquals("new-confirmed-content", stateFile.readText())

        val leftoverTemp = File(stateFile.parentFile, "3.txt.tmp")
        assertFalse(leftoverTemp.exists())
    }

    @Test
    fun writeTextAtomically_preservesExistingConfirmedFile_whenWriteFails() {
        val dir = tempFolder.newFolder("sticker_states")
        val stateFile = File(dir, "4.txt")
        stateFile.writeText("existing-confirmed-content")

        // 쓰기 실패를 강제로 재현: 원자적 저장이 사용할 임시파일 경로에
        // 이미 디렉터리를 만들어 두면 FileOutputStream이 IOException을 던진다.
        val tempFileCollisionDir = File(dir, "4.txt.tmp")
        tempFileCollisionDir.mkdirs()

        val saved = ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = stateFile,
            content = "attempted-new-content"
        )

        assertFalse(saved)
        // 기존 확정 파일은 손상되지 않고 그대로 남아 있어야 한다.
        assertEquals("existing-confirmed-content", stateFile.readText())
        // 충돌용 임시 디렉터리는 비어 있으므로 정리(삭제)됐어야 한다.
        assertFalse(tempFileCollisionDir.exists())
    }

    @Test
    fun writeTextAtomically_createsParentDirectoryWhenMissing() {
        val dir = File(tempFolder.root, "sticker_states")
        val stateFile = File(dir, "5.txt")

        val saved = ConfirmedEditStateStorage.writeTextAtomically(stateFile, "content")

        assertTrue(saved)
        assertTrue(stateFile.exists())
    }
}
