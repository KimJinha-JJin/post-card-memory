package com.postcardmemory.utils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 스티커·도장 확정 상태 파일(sticker_states/, seal_states/)을 임시파일에
 * 먼저 쓰고 rename하는 방식으로 저장한다. PostcardDraftStorage의 검증된
 * 원자적 저장 패턴과 동일한 방식이며, 쓰기 도중 실패해도 대상 파일이
 * 손상되지 않고 기존 확정 파일이 그대로 유지된다.
 */
object ConfirmedEditStateStorage {

    fun writeTextAtomically(
        targetFile: File,
        content: String
    ): Boolean {
        val dir = targetFile.parentFile ?: return false

        if (!dir.exists() && !dir.mkdirs()) {
            return false
        }

        val tempFile = File(dir, "${targetFile.name}.tmp")

        return try {
            FileOutputStream(tempFile).use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }

            tempFile.renameTo(targetFile)
        } catch (exception: IOException) {
            tempFile.delete()
            false
        }
    }
}
