package com.postcardmemory.utils

import java.io.File

/**
 * 119일차: 완료 저장에서 목록에서 빠진 사진 스티커의 확정 누끼
 * (`sticker_bgs/<postcardId>/<stickerId>.png`)를 정리한다.
 *
 * 예전에는 이 파일을 "지금 화면이 가리키는 누끼 URI"로만 지웠다. 그런데 초안을 복원한 화면은
 * draft_sticker_bgs/ 사본을 가리키므로, 거기서 스티커를 지우고 완료 저장하면 확정 누끼는 아무도
 * 가리키지 않은 채 남았다. 그래서 파일 경로가 아니라 "이번 저장 직전 확정 목록에 있던 id 중
 * 저장 뒤 남길 id가 아닌 것"으로 대상을 정한다.
 *
 * - 호출자는 확정 상태 파일 쓰기를 포함한 저장 전체가 성공한 뒤에만 부른다.
 * - 이전 확정 목록에 없던 파일(이 기능 이전부터 남아 있던 잔여물 등)은 대상이 아니다.
 * - 스티커 id 형식(UUID)과 파일 위치가 맞지 않으면 건너뛴다.
 *
 * 순수 JVM 코드로 두어 JUnit 임시 폴더로 검증한다.
 */
internal object ConfirmedStickerBackgroundCleanup {

    private val STICKER_ID_PATTERN = Regex("^[0-9a-fA-F-]{36}$")
    private const val BACKGROUND_EXTENSION = ".png"

    /** 확정 사진 스티커 상태 파일의 스티커 id. 파일이 없으면 빈 집합, 읽기 실패면 null. */
    fun readStickerIds(stateFile: File): Set<String>? {
        if (!stateFile.exists()) return emptySet()
        return runCatching {
            stateFile.readLines(Charsets.UTF_8)
                .filter { it.isNotBlank() }
                .map { it.substringBefore('\t') }
                .toSet()
        }.getOrNull()
    }

    /** 지울 대상만 계산한다(파일은 건드리지 않음). */
    fun droppedBackgroundFiles(
        directory: File,
        previousStickerIds: Set<String>,
        keepStickerIds: Set<String>
    ): List<File> {
        val canonicalDirectory =
            runCatching { directory.canonicalFile }.getOrNull() ?: return emptyList()

        return (previousStickerIds - keepStickerIds)
            .filter { STICKER_ID_PATTERN.matches(it) }
            .sorted()
            .map { File(canonicalDirectory, it + BACKGROUND_EXTENSION) }
            .filter { file ->
                file.isFile &&
                    runCatching { file.canonicalFile.parentFile }.getOrNull() == canonicalDirectory
            }
    }

    /** [droppedBackgroundFiles]를 지우고 실제로 지운 파일을 돌려준다. 하나가 실패해도 나머지는 계속한다. */
    fun deleteDroppedBackgrounds(
        directory: File,
        previousStickerIds: Set<String>,
        keepStickerIds: Set<String>
    ): List<File> =
        droppedBackgroundFiles(directory, previousStickerIds, keepStickerIds)
            .filter { file -> runCatching { file.delete() }.getOrDefault(false) }
}
