package com.postcardmemory.utils

import android.content.Context
import com.postcardmemory.data.Postcard
import java.io.File

/** 고아로 판정된 파일 또는 디렉터리 하나. */
data class OrphanAsset(
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long
)

/** 진단 유형 하나(중심 사진, 배경사진, 확정 누끼 등)의 스캔 결과. */
data class OrphanCategoryResult(
    val type: String,
    val orphans: List<OrphanAsset>,
    val reason: String
) {
    val count: Int
        get() = orphans.size

    val totalSizeBytes: Long
        get() = orphans.sumOf { it.sizeBytes }
}

/** 전체 스캔 결과. 유형별로 묶은 고아 목록과, 이름/ID 파싱에 실패해 고아인지 판정할 수 없었던 항목을 분리해 담는다. */
data class OrphanScanResult(
    val categories: List<OrphanCategoryResult>,
    val unclassified: List<String>
) {
    val totalOrphanCount: Int
        get() = categories.sumOf { it.count }

    val totalOrphanSizeBytes: Long
        get() = categories.sumOf { it.totalSizeBytes }
}

/**
 * Room에 더 이상 연결되지 않은 파일을 찾아 보고만 하는 순수 진단 도구.
 * 파일을 지우거나 옮기지 않는다 — 삭제는 이 도구가 하지 않는 별도 작업이다.
 *
 * PostcardDeletionManager와 같은 filesDir 하위 경로 규칙(꾸미기 요소별
 * <디렉터리>/<id>.txt — sticker_states/, seal_states/, doodle_states/,
 * text_sticker_states/, masking_tape_states/, label_sticker_states/ —
 * 그리고 sticker_bgs/<id>/, sticker_originals/<id>/, draft_sticker_bgs/<id>/,
 * drafts/edit_state/<id>.draft.txt)을 그대로 따르되, 그 파일들을 지우는
 * 대신 목록만 만든다. 삭제 쪽에 새 디렉터리가 추가되면 여기에도 함께
 * 넣어야 그 요소의 고아 파일을 찾을 수 있다.
 */
object OrphanFileDiagnostics {

    fun scan(
        context: Context,
        postcards: List<Postcard>
    ): OrphanScanResult {
        val existingPostcardIds =
            postcards.map { postcard -> postcard.id }.toSet()
        val referencedImagePaths =
            postcards.map { postcard -> postcard.imagePath }.toSet()
        val referencedBackgroundPaths =
            postcards
                .mapNotNull { postcard -> postcard.backgroundImagePath }
                .toSet()

        return scan(
            rootDirectory = context.filesDir,
            existingPostcardIds = existingPostcardIds,
            referencedImagePaths = referencedImagePaths,
            referencedBackgroundPaths = referencedBackgroundPaths
        )
    }

    internal fun scan(
        rootDirectory: File,
        existingPostcardIds: Set<Long>,
        referencedImagePaths: Set<String>,
        referencedBackgroundPaths: Set<String>
    ): OrphanScanResult {
        val unclassified = mutableListOf<String>()

        val categories = listOf(
            scanFlatFileDirectory(
                type = "centralImage",
                directory = File(rootDirectory, "postcards"),
                referencedPaths = canonicalize(referencedImagePaths),
                reason = "Room의 imagePath에서 참조하지 않음"
            ),
            scanFlatFileDirectory(
                type = "backgroundImage",
                directory = File(rootDirectory, "postcard_backgrounds"),
                referencedPaths = canonicalize(referencedBackgroundPaths),
                reason = "Room의 backgroundImagePath에서 참조하지 않음"
            ),
            scanPostcardIdDirectories(
                type = "confirmedStickerBackground",
                directory = File(rootDirectory, "sticker_bgs"),
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId 디렉터리"
            ),
            scanPostcardIdDirectories(
                type = "cameraStickerOriginal",
                directory = File(rootDirectory, "sticker_originals"),
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId 디렉터리"
            ),
            scanPostcardIdDirectories(
                type = "draftStickerBackground",
                directory = File(rootDirectory, "draft_sticker_bgs"),
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId 디렉터리"
            ),
            scanPostcardIdFiles(
                type = "stickerState",
                directory = File(rootDirectory, "sticker_states"),
                suffix = ".txt",
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId의 스티커 상태 파일"
            ),
            scanPostcardIdFiles(
                type = "sealState",
                directory = File(rootDirectory, "seal_states"),
                suffix = ".txt",
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId의 도장 상태 파일"
            ),
            scanPostcardIdFiles(
                type = "doodleState",
                directory = File(rootDirectory, "doodle_states"),
                suffix = ".txt",
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId의 낙서 상태 파일"
            ),
            scanPostcardIdFiles(
                type = "textStickerState",
                directory = File(rootDirectory, "text_sticker_states"),
                suffix = ".txt",
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId의 텍스트 스티커 상태 파일"
            ),
            scanPostcardIdFiles(
                type = "maskingTapeState",
                directory = File(rootDirectory, "masking_tape_states"),
                suffix = ".txt",
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId의 마스킹테이프 상태 파일"
            ),
            scanPostcardIdFiles(
                type = "labelStickerState",
                directory = File(rootDirectory, "label_sticker_states"),
                suffix = ".txt",
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId의 라벨 스티커 상태 파일"
            ),
            scanPostcardIdFiles(
                type = "editDraft",
                directory = File(rootDirectory, "drafts/edit_state"),
                suffix = ".draft.txt",
                existingPostcardIds = existingPostcardIds,
                unclassified = unclassified,
                reason = "Room에 존재하지 않는 postcardId의 편집 초안 파일"
            )
        )

        return OrphanScanResult(
            categories = categories,
            unclassified = unclassified.toList()
        )
    }

    private fun canonicalize(paths: Set<String>): Set<String> =
        paths
            .mapNotNull { path ->
                runCatching { File(path).canonicalPath }.getOrNull()
            }
            .toSet()

    private fun scanFlatFileDirectory(
        type: String,
        directory: File,
        referencedPaths: Set<String>,
        reason: String
    ): OrphanCategoryResult {
        val entries = directory.listFiles()?.toList().orEmpty()

        val orphans = entries
            .filter { entry -> entry.isFile }
            .filter { file ->
                val canonical =
                    runCatching { file.canonicalPath }.getOrNull()
                canonical == null || canonical !in referencedPaths
            }
            .map { file ->
                OrphanAsset(
                    path = file.path,
                    isDirectory = false,
                    sizeBytes = file.length()
                )
            }

        return OrphanCategoryResult(
            type = type,
            orphans = orphans,
            reason = reason
        )
    }

    private fun scanPostcardIdDirectories(
        type: String,
        directory: File,
        existingPostcardIds: Set<Long>,
        unclassified: MutableList<String>,
        reason: String
    ): OrphanCategoryResult {
        val entries = directory.listFiles()?.toList().orEmpty()
        val orphans = mutableListOf<OrphanAsset>()

        for (entry in entries) {
            val postcardId = entry.name.toLongOrNull()

            if (!entry.isDirectory || postcardId == null) {
                unclassified += entry.path
                continue
            }

            if (postcardId !in existingPostcardIds) {
                orphans += OrphanAsset(
                    path = entry.path,
                    isDirectory = true,
                    sizeBytes = directorySize(entry)
                )
            }
        }

        return OrphanCategoryResult(
            type = type,
            orphans = orphans,
            reason = reason
        )
    }

    private fun scanPostcardIdFiles(
        type: String,
        directory: File,
        suffix: String,
        existingPostcardIds: Set<Long>,
        unclassified: MutableList<String>,
        reason: String
    ): OrphanCategoryResult {
        val entries = directory.listFiles()?.toList().orEmpty()
        val orphans = mutableListOf<OrphanAsset>()

        for (entry in entries) {
            val postcardId =
                entry.name
                    .takeIf { name -> name.endsWith(suffix) }
                    ?.removeSuffix(suffix)
                    ?.toLongOrNull()

            if (!entry.isFile || postcardId == null) {
                unclassified += entry.path
                continue
            }

            if (postcardId !in existingPostcardIds) {
                orphans += OrphanAsset(
                    path = entry.path,
                    isDirectory = false,
                    sizeBytes = entry.length()
                )
            }
        }

        return OrphanCategoryResult(
            type = type,
            orphans = orphans,
            reason = reason
        )
    }

    private fun directorySize(directory: File): Long =
        directory
            .walkTopDown()
            .filter { file -> file.isFile }
            .sumOf { file -> file.length() }
}
