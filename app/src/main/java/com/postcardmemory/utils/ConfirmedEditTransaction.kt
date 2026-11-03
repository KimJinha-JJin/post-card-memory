package com.postcardmemory.utils

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 121일차: 완료 저장(꾸미기 여섯 종 + 확정 누끼)을 "준비 → 교체" 두 단계로 나눈다.
 *
 * 이전에는 종류마다 [ConfirmedEditStateStorage.writeTextAtomically]로 곧바로 확정 파일을
 * 교체했다. 파일 하나하나는 원자적이었지만, 세 번째 종류에서 실패하면 앞의 두 종류는 이미
 * 새 내용으로 바뀌어 갤러리·공유·"원래대로" 기준이 서로 다른 시점의 상태를 보게 됐다. 또
 * 확정 누끼(`sticker_bgs/<엽서>/<id>.png`)는 상태 파일을 쓰기 전에 제자리 덮어쓰기로 복사돼,
 * 이후 실패해도 기존 확정본의 이미지 내용이 이미 바뀌어 있었다.
 *
 * 준비 단계([stageText], [stageCopy])는 대상 옆의 `<이름>.tmp`에만 쓴다. 준비가 하나라도
 * 실패하면 [discard]로 임시 파일만 지우고 **확정 파일은 하나도 바뀌지 않는다**. 모두 준비된
 * 뒤에야 [commit]이 같은 디렉터리 안의 이름 바꾸기(rename)만으로 차례로 교체한다 — 새 데이터를
 * 쓰는 작업은 이 시점에 남아 있지 않다.
 *
 * 남는 한계: [commit] 도중 rename 자체가 실패하면(같은 디렉터리 안 rename이라 드묾) 앞서
 * 교체된 파일은 되돌리지 않는다. 이 경우 [CommitResult.PARTIALLY_COMMITTED]를 돌려 호출자가
 * 성공으로 처리하지 않게(초안 보존) 한다. 프로세스가 교체 도중 강제 종료되는 경우도 같다.
 * 되돌리려면 영속 복구 기록이 필요해 이번 범위에서 다루지 않는다.
 *
 * 임시 파일 이름은 기존 원자 저장과 같은 `<이름>.tmp`다(새 종류의 잔여 파일을 만들지 않음).
 * 확정 파일 형식·경로는 바꾸지 않는다.
 */
internal class ConfirmedEditTransaction(
    private val replace: (File, File) -> Boolean = AtomicFileReplace::replace
) {

    enum class CommitResult {
        /** 준비한 파일이 모두 교체됐다. */
        COMMITTED,

        /** 첫 교체부터 실패해 확정 파일이 하나도 바뀌지 않았다. */
        NOTHING_COMMITTED,

        /** 일부만 교체됐다(드문 rename 실패). 성공으로 다루면 안 된다. */
        PARTIALLY_COMMITTED
    }

    private class StagedReplacement(
        val stagedFile: File,
        val targetFile: File
    )

    private val staged = mutableListOf<StagedReplacement>()

    /** 준비된 교체 수(테스트·진단용). */
    val stagedCount: Int
        get() = staged.size

    /** [targetFile]에 넣을 [content]를 `<이름>.tmp`에 써 둔다. 확정 파일은 건드리지 않는다. */
    fun stageText(targetFile: File, content: String): Boolean =
        stage(targetFile) { stagedFile ->
            FileOutputStream(stagedFile).use { outputStream ->
                outputStream.write(content.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }
        }

    /**
     * [sourceFile] 내용을 [targetFile] 옆 `<이름>.tmp`로 복사해 둔다. [targetFile]이 이미
     * 있어도(같은 스티커 id의 확정 누끼) 이 단계에서는 바뀌지 않는다.
     */
    fun stageCopy(sourceFile: File, targetFile: File): Boolean =
        stage(targetFile) { stagedFile ->
            sourceFile.inputStream().use { input ->
                FileOutputStream(stagedFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
        }

    private fun stage(
        targetFile: File,
        write: (File) -> Unit
    ): Boolean {
        val dir = targetFile.parentFile ?: return false
        if (!dir.exists() && !dir.mkdirs()) {
            return false
        }

        val stagedFile = File(dir, "${targetFile.name}.tmp")
        // 같은 대상을 두 번 준비하면 앞의 것을 버리고 마지막 내용만 교체한다.
        staged.removeAll { it.targetFile == targetFile }

        return try {
            write(stagedFile)
            if (!stagedFile.isFile) {
                false
            } else {
                staged.add(StagedReplacement(stagedFile, targetFile))
                true
            }
        } catch (_: IOException) {
            stagedFile.delete()
            false
        }
    }

    /** 준비한 순서대로 교체한다. 실패하면 그 뒤의 임시 파일은 지우고 멈춘다. */
    fun commit(): CommitResult {
        var committedCount = 0
        val pending = staged.toList()
        staged.clear()

        for ((index, replacement) in pending.withIndex()) {
            val replaced =
                runCatching {
                    replace(replacement.stagedFile, replacement.targetFile)
                }.getOrDefault(false)

            if (!replaced) {
                replacement.stagedFile.delete()
                pending.drop(index + 1).forEach { it.stagedFile.delete() }
                return if (committedCount == 0) {
                    CommitResult.NOTHING_COMMITTED
                } else {
                    CommitResult.PARTIALLY_COMMITTED
                }
            }
            committedCount++
        }

        return CommitResult.COMMITTED
    }

    /** 준비만 하고 교체하지 않은 임시 파일을 모두 지운다. 확정 파일은 그대로다. */
    fun discard() {
        staged.forEach { it.stagedFile.delete() }
        staged.clear()
    }
}
