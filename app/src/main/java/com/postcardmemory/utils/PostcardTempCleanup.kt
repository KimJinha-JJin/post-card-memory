package com.postcardmemory.utils

import java.io.File

internal const val POSTCARD_TEMP_FILE_MAX_AGE_MILLIS =
    7L * 24L * 60L * 60L * 1_000L

internal data class PostcardTempCleanupResult(
    val deletedFiles: List<String>,
    val failedFiles: List<String>
)

/**
 * 이전 프로세스가 남긴 오래된 카메라 원본만 앱 시작 시 정리한다.
 * Application.onCreate()에서 카메라 화면보다 먼저 한 번 호출되므로 현재
 * 프로세스에서 사용 중인 crop 원본과 경합하지 않는다.
 */
internal object PostcardTempCleanup {

    fun cleanup(
        filesDir: File,
        nowMillis: Long = System.currentTimeMillis()
    ): PostcardTempCleanupResult =
        cleanup(
            filesDir = filesDir,
            nowMillis = nowMillis,
            deleteFile = { file -> file.delete() }
        )

    internal fun cleanup(
        filesDir: File,
        nowMillis: Long,
        deleteFile: (File) -> Boolean
    ): PostcardTempCleanupResult {
        val directory = File(filesDir, "postcards_temp")
        val entries =
            runCatching {
                directory.listFiles()?.toList().orEmpty()
            }.getOrDefault(emptyList())
        val staleCutoff =
            nowMillis - POSTCARD_TEMP_FILE_MAX_AGE_MILLIS
        val deletedFiles = mutableListOf<String>()
        val failedFiles = mutableListOf<String>()

        entries
            .filter { entry -> entry.isFile }
            .filter { file ->
                runCatching { file.lastModified() }
                    .getOrDefault(nowMillis) <= staleCutoff
            }
            .forEach { file ->
                val deleted =
                    runCatching { deleteFile(file) }
                        .getOrDefault(false)

                if (deleted) {
                    deletedFiles += file.path
                } else {
                    failedFiles += file.path
                }
            }

        return PostcardTempCleanupResult(
            deletedFiles = deletedFiles,
            failedFiles = failedFiles
        )
    }
}
