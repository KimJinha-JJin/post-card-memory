package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * "저장 덮어쓰기 테스트 2건 실패 원인 조사" 진단 테스트.
 *
 * ConfirmedEditStateStorageTest.writeTextAtomically_overwritesExistingConfirmedFile_onSuccess와
 * PostcardTemplateStorageTest.overwrite_sameId_replacesContent_otherTemplatesUntouched는
 * 둘 다 "이미 존재하는 대상 파일 위에 임시 파일을 rename"하는 지점에서 실패한다.
 * 이 파일은 그 원인을 저장소 클래스 로직과 완전히 분리해, 순수 `java.io.File
 * .renameTo()`만으로 재현한다 — 실패가 저장 클래스의 다른 로직(직렬화, 목록
 * 관리 등)이 아니라 JVM의 rename 구현 자체에 있음을 직접 확인하기 위함이다.
 *
 * 이 테스트는 플랫폼에 관계없이 항상 통과하도록 설계했다(데이터 손상 여부만
 * 불변식으로 검증) — renameTo가 성공하든 실패하든 target 파일이 old/new
 * 콘텐츠 중 하나여야 하며 부분 손상은 없어야 한다는 것만 assert한다. 대신
 * 실제 renameTo 반환값과 OS 이름을 콘솔에 출력해, 이 테스트를 실행한 사람이
 * "이 플랫폼에서 기존 파일 위에 renameTo가 실제로 성공하는지"를 직접 눈으로
 * 확인할 수 있게 한다.
 */
class FileRenameToOverwriteDiagnosticTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun rawFileRenameTo_overExistingTarget_neverPartiallyCorruptsButMayFailToOverwrite() {
        val dir = tempFolder.newFolder("raw-rename-diagnostic")
        val target = File(dir, "target.txt")
        target.writeText("old-content")

        val temp = File(dir, "target.txt.tmp")
        temp.writeText("new-content")

        val renamed = temp.renameTo(target)

        println(
            "[FileRenameToOverwriteDiagnosticTest] os.name=" +
                System.getProperty("os.name") +
                ", tempFile.renameTo(existingTarget)=" + renamed +
                ", target 내용=" + target.readText() +
                ", temp 파일 잔존=" + temp.exists()
        )

        // 플랫폼에 관계없이 항상 성립해야 하는 안전 불변식: rename이 실패하면
        // target은 손상되지 않은 old-content 그대로여야 하고, 성공하면
        // new-content로 정확히 바뀌어 있어야 한다. 부분 손상(빈 내용, 잘린
        // 내용 등)은 데이터 손상이므로 이 자체가 치명적 결함 신호가 된다.
        val expectedContent = if (renamed) "new-content" else "old-content"
        assertEquals(
            "renameTo=$renamed (os.name=${System.getProperty("os.name")})인데 " +
                "target 내용이 old/new 어느 쪽도 아님 - 부분 손상 의심",
            expectedContent,
            target.readText()
        )

        if (!renamed) {
            // renameTo가 false를 반환한 경우, 프로덕션 코드(ConfirmedEditStateStorage/
            // PostcardDraftStorage/PostcardTemplateStorage)는 이 경로에서 temp 파일을
            // 정리하지 않는다(IOException catch 블록에서만 delete함) - 이 사실도
            // 함께 기록한다.
            assertTrue(
                "renameTo 실패 후에도 temp 파일이 남아있어야 함(프로덕션 코드가 " +
                    "이 경우를 정리하지 않는다는 조사 보고서 §12 근거)",
                temp.exists()
            )
        }
    }
}
