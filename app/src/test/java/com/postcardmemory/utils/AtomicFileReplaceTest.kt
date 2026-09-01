package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 공통 파일 교체 helper의 회귀 테스트.
 *
 * 이전에는 `FileRenameToOverwriteDiagnosticTest`가 `java.io.File.renameTo()`의
 * 원시 플랫폼 종속 동작(대상 파일 존재 시 Windows에서 실패)을 직접
 * 관찰하는 용도였다. 그 원인 조사를 근거로 4곳의 저장 구현을
 * `AtomicFileReplace`로 교체한 지금은, 원시 `renameTo()` 동작을 진단할
 * 필요가 없다 — 대신 이 교체 helper 자체가 대상 파일 존재 여부와
 * 관계없이(플랫폼과 무관하게) 항상 올바르게 동작하는지를 검증하는 것이
 * 더 의미 있는 회귀 테스트다. `Files.move`의 `REPLACE_EXISTING`은
 * Windows·Linux·Android 어디서나 대상을 교체하도록 보장되므로, 이
 * 테스트는 어떤 플랫폼에서 실행되든 같은 결과를 기대한다(저장 덮어쓰기
 * 테스트 실패 원인 조사 보고서 및 원자적 저장 덮어쓰기 안정화 보고서 참고).
 */
class AtomicFileReplaceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun replace_targetDoesNotExist_movesTempContentToTarget() {
        val dir = tempFolder.newFolder("replace-new")
        val target = File(dir, "target.txt")
        val temp = File(dir, "target.txt.tmp")
        temp.writeText("new-content")

        val replaced = AtomicFileReplace.replace(temp, target)

        assertTrue(replaced)
        assertEquals("new-content", target.readText())
        assertFalse(temp.exists())
    }

    @Test
    fun replace_targetAlreadyExists_overwritesWithNewContent() {
        val dir = tempFolder.newFolder("replace-overwrite")
        val target = File(dir, "target.txt")
        target.writeText("old-content")
        val temp = File(dir, "target.txt.tmp")
        temp.writeText("new-content")

        val replaced = AtomicFileReplace.replace(temp, target)

        assertTrue(replaced)
        assertEquals("new-content", target.readText())
    }

    @Test
    fun replace_success_leavesNoTempFileBehind() {
        val dir = tempFolder.newFolder("replace-cleanup")
        val target = File(dir, "target.txt")
        target.writeText("old-content")
        val temp = File(dir, "target.txt.tmp")
        temp.writeText("new-content")

        AtomicFileReplace.replace(temp, target)

        assertFalse(temp.exists())
    }

    @Test
    fun replace_missingTempFile_returnsFalse_andExistingTargetUntouched() {
        val dir = tempFolder.newFolder("replace-missing-temp")
        val target = File(dir, "target.txt")
        target.writeText("existing-content")
        val missingTemp = File(dir, "target.txt.tmp")

        val replaced = AtomicFileReplace.replace(missingTemp, target)

        assertFalse(replaced)
        // 실패해도 기존 대상 파일은 먼저 지우지 않으므로 그대로 남아 있어야 한다.
        assertEquals("existing-content", target.readText())
    }
}
