package com.postcardmemory.utils

import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * 이미 작성이 끝난 임시 파일을 최종 대상 파일로 교체하는 책임만 담당한다.
 * 대상 파일이 이미 존재해도 안전하게 교체한다.
 *
 * `java.io.File.renameTo()`는 "대상 경로에 파일이 이미 있으면 성공하지
 * 못할 수 있다"는 것이 Javadoc에 명시된 플랫폼 종속 동작이며, 실제로
 * Windows에서는 대상이 이미 존재하면 실패하고 POSIX(Linux/Android)에서는
 * 성공한다. 이 헬퍼는 그 대신 `Files.move`로 원자적 교체를 우선 시도하고,
 * 파일시스템이 원자적 이동을 지원하지 않을 때만 REPLACE_EXISTING 단독
 * 방식으로 한 번 안전하게 재시도한다.
 */
object AtomicFileReplace {

    /**
     * [tempFile]을 [targetFile] 위치로 교체한다. 두 파일은 같은 디렉터리에
     * 있어야 한다. 성공하면 true를 반환하고 [tempFile]은 더 이상 그 경로에
     * 존재하지 않는다(이미 [targetFile]로 옮겨졌으므로). 실패하면 false를
     * 반환하며, [targetFile]은 먼저 지우지 않으므로 실패 이전 상태 그대로
     * 남는다. 실패 시 [tempFile] 정리를 시도하지만, 정리 성공 여부는
     * 반환값에 영향을 주지 않는다 - 이 함수의 반환값은 오직 "교체(저장)가
     * 성공했는가"만 의미한다.
     */
    fun replace(tempFile: File, targetFile: File): Boolean {
        val tempPath = tempFile.toPath()
        val targetPath = targetFile.toPath()

        val moved = try {
            Files.move(
                tempPath,
                targetPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
            true
        } catch (atomicNotSupported: AtomicMoveNotSupportedException) {
            try {
                Files.move(
                    tempPath,
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
                )
                true
            } catch (fallbackFailure: IOException) {
                false
            }
        } catch (moveFailure: IOException) {
            false
        }

        if (!moved) {
            tempFile.delete()
        }

        return moved
    }
}
