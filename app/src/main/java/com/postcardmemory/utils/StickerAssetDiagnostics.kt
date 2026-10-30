package com.postcardmemory.utils

import android.content.Context
import com.postcardmemory.data.Postcard
import java.io.File
import java.net.URI

/** 초안 한 장을 확정 상태와 비교한 결과. */
enum class DraftDiagnosis {
    /** 꾸미기 내용(선택 상태 제외)이 확정 상태와 같다 — 버려도 편집이 사라지지 않을 잔여 후보. */
    SAME_AS_CONFIRMED,

    /** 확정 상태와 다른 내용이 있다 — 아직 완료 저장하지 않은 편집일 수 있는 정상 초안. */
    HAS_UNSAVED_CHANGES,

    /** 초안 형식을 읽지 못했다 — 판정하지 않는다. */
    UNREADABLE
}

data class DraftDiagnosisEntry(
    val postcardId: Long,
    val path: String,
    val diagnosis: DraftDiagnosis,
    /** 초안 텍스트 + draft_sticker_bgs/<id>/ 전체 크기. */
    val sizeBytes: Long
)

data class StickerAssetScanResult(
    val categories: List<OrphanCategoryResult>,
    val drafts: List<DraftDiagnosisEntry>,
    val unclassified: List<String>
)

/**
 * 119일차: 살아 있는 엽서 안의 사진 스티커 파일을 스티커 단위로 진단한다.
 * [OrphanFileDiagnostics]는 "Room에 없는 엽서 id의 폴더/파일"만 찾으므로, 엽서는 있는데
 * 그 안에서 아무 상태도 가리키지 않는 누끼·캐시 파일(예: 스티커를 지우고 완료 저장한 뒤
 * 남은 sticker_bgs/<id>/<스티커id>.png)은 보지 못한다. 이 진단이 그 빈칸을 채운다.
 *
 * [OrphanFileDiagnostics]와 같이 보고만 하고 파일을 지우거나 옮기지 않는다. 결과의
 * "참조 없음"은 저장된 확정 상태와 초안 기준이라, 지금 편집 중인 화면의 메모리·undo가
 * 쓰는 캐시 파일도 후보로 보일 수 있다 — 삭제 승인이 아니라 판단 근거다.
 *
 * 상태 파일과 초안은 Uri 없이 원문 그대로 읽는다(순수 JVM에서 검증하기 위함). 줄 형식은
 * PhotoStickerItem.serialize(탭 구분: 0 id, 1 원본, 2 표시, 3 누끼, 4 배경제거 여부, ...)와
 * PostcardEditDraft.serialize(머리 줄, 메타 줄, 요소 줄)를 따른다.
 */
object StickerAssetDiagnostics {

    internal const val STICKER_CACHE_DIR_NAME = "photo_stickers"

    private const val DRAFT_HEADER = "POSTCARD_DRAFT_V1"
    private const val DRAFT_FILE_SUFFIX = ".draft.txt"

    // 메타 줄에서 요소 개수 자리(PostcardEditDraft.serialize 순서). 없는 자리는 0개(구버전 초안).
    private const val META_STICKER_COUNT = 7
    private const val META_SEAL_COUNT = 8
    private const val META_DOODLE_COUNT = 9
    private const val META_TEXT_STICKER_COUNT = 10
    private const val META_MASKING_TAPE_COUNT = 12
    private const val META_LABEL_STICKER_COUNT = 14

    // 사진 스티커 줄에서 파일 자리와 비교에서 뺄 자리(같은 그림이어도 초안·확정 폴더가 달라 경로가 다르다).
    private const val STICKER_ORIGINAL_URI = 1
    private const val STICKER_DISPLAYED_URI = 2
    private const val STICKER_REMOVED_BG_URI = 3
    private const val STICKER_IS_BACKGROUND_REMOVED = 4

    fun scan(
        context: Context,
        postcards: List<Postcard>
    ): StickerAssetScanResult =
        scan(
            rootDirectory = context.filesDir,
            cacheDirectory = context.cacheDir,
            existingPostcardIds = postcards.map { it.id }.toSet()
        )

    internal fun scan(
        rootDirectory: File,
        cacheDirectory: File,
        existingPostcardIds: Set<Long>
    ): StickerAssetScanResult {
        val unclassified = mutableListOf<String>()
        val confirmedStickerFile = DecorationStateFile.PHOTO_STICKER

        // 엽서별로 확정 상태와 초안이 가리키는 파일 경로를 모은다.
        val confirmedReferences = mutableMapOf<Long, Set<String>>()
        val draftReferences = mutableMapOf<Long, Set<String>>()
        val drafts = mutableListOf<DraftDiagnosisEntry>()

        for (postcardId in existingPostcardIds.sorted()) {
            val confirmedLines =
                readNonBlankLines(confirmedStickerFile.file(rootDirectory, postcardId))
            confirmedReferences[postcardId] =
                stickerFileReferences(confirmedLines.orEmpty())

            val draftFile = PostcardDraftStorage.draftFile(rootDirectory, postcardId)
            if (!draftFile.isFile) continue

            val draftSections = readDraftSections(draftFile)
            draftReferences[postcardId] =
                stickerFileReferences(draftSections?.stickers.orEmpty())
            drafts += DraftDiagnosisEntry(
                postcardId = postcardId,
                path = draftFile.path,
                diagnosis = diagnoseDraft(rootDirectory, postcardId, draftSections),
                sizeBytes = draftFile.length() +
                    directorySize(
                        PostcardDraftStorage.draftStickerBackgroundDir(rootDirectory, postcardId)
                    )
            )
        }

        val allReferences =
            (confirmedReferences.values + draftReferences.values).flatten().toSet()

        val categories = listOf(
            scanPerPostcardFiles(
                type = "unreferencedConfirmedStickerBackground",
                directory = PostcardAssetDirectory.CONFIRMED_STICKER_BACKGROUNDS.root(rootDirectory),
                existingPostcardIds = existingPostcardIds,
                referencesFor = { id ->
                    confirmedReferences[id].orEmpty() + draftReferences[id].orEmpty()
                },
                unclassified = unclassified,
                reason = "엽서는 있지만 확정 상태·초안의 사진 스티커가 가리키지 않는 확정 누끼"
            ),
            scanPerPostcardFiles(
                type = "unreferencedDraftStickerBackground",
                directory = File(rootDirectory, PostcardDraftStorage.DRAFT_STICKER_BG_DIR_NAME),
                existingPostcardIds = existingPostcardIds,
                referencesFor = { id -> draftReferences[id].orEmpty() },
                unclassified = unclassified,
                reason = "엽서는 있지만 그 엽서의 초안이 가리키지 않는 초안 누끼(초안 파일 없음 포함)"
            ),
            scanCacheFiles(
                directory = File(cacheDirectory, STICKER_CACHE_DIR_NAME),
                references = allReferences
            )
        )

        return StickerAssetScanResult(
            categories = categories,
            drafts = drafts.toList(),
            unclassified = unclassified.toList()
        )
    }

    private class DraftSections(
        val stickers: List<String>,
        val seals: List<String>,
        val doodles: List<String>,
        val textStickers: List<String>,
        val maskingTapes: List<String>,
        val labelStickers: List<String>
    )

    private fun readDraftSections(draftFile: File): DraftSections? {
        val lines =
            runCatching { draftFile.readText(Charsets.UTF_8).split("\n") }.getOrNull()
                ?: return null
        if (lines.size < 2 || lines[0].trim() != DRAFT_HEADER) return null

        val meta = lines[1].split("\t")
        if (meta.size <= META_SEAL_COUNT) return null

        fun count(index: Int): Int? =
            if (index < meta.size) meta[index].toIntOrNull()?.takeIf { it >= 0 } else 0

        val counts = listOf(
            count(META_STICKER_COUNT),
            count(META_SEAL_COUNT),
            count(META_DOODLE_COUNT),
            count(META_TEXT_STICKER_COUNT),
            count(META_MASKING_TAPE_COUNT),
            count(META_LABEL_STICKER_COUNT)
        )
        if (counts.any { it == null }) return null

        val body = lines.drop(2)
        if (body.size < counts.sumOf { it!! }) return null

        var start = 0
        val sections = counts.map { size ->
            body.subList(start, start + size!!).also { start += size }
        }
        return DraftSections(
            stickers = sections[0],
            seals = sections[1],
            doodles = sections[2],
            textStickers = sections[3],
            maskingTapes = sections[4],
            labelStickers = sections[5]
        )
    }

    /**
     * 선택 상태는 빼고 꾸미기 여섯 종의 줄을 확정 상태 파일과 비교한다. 사진 스티커는 누끼
     * 경로 대신 누끼 파일 내용을 비교한다. 확정 쪽을 읽지 못하거나 누끼 파일을 열 수 없으면
     * 같다고 단정하지 않는다(정상 초안 쪽으로 판정).
     */
    private fun diagnoseDraft(
        rootDirectory: File,
        postcardId: Long,
        draft: DraftSections?
    ): DraftDiagnosis {
        if (draft == null) return DraftDiagnosis.UNREADABLE

        fun confirmed(stateFile: DecorationStateFile): List<String>? =
            readNonBlankLines(stateFile.file(rootDirectory, postcardId))

        val sameStickers =
            confirmed(DecorationStateFile.PHOTO_STICKER)
                ?.let { stickerLinesMatch(draft.stickers, it) } == true
        val sameOthers = listOf(
            draft.seals to DecorationStateFile.SEAL,
            draft.doodles to DecorationStateFile.DOODLE,
            draft.textStickers to DecorationStateFile.TEXT_STICKER,
            draft.maskingTapes to DecorationStateFile.MASKING_TAPE,
            draft.labelStickers to DecorationStateFile.LABEL_STICKER
        ).all { (draftLines, stateFile) ->
            confirmed(stateFile) == draftLines.filter { it.isNotBlank() }
        }

        return if (sameStickers && sameOthers) {
            DraftDiagnosis.SAME_AS_CONFIRMED
        } else {
            DraftDiagnosis.HAS_UNSAVED_CHANGES
        }
    }

    private fun stickerLinesMatch(draftLines: List<String>, confirmedLines: List<String>): Boolean {
        val draftStickers = draftLines.filter { it.isNotBlank() }
        if (draftStickers.size != confirmedLines.size) return false

        return draftStickers.zip(confirmedLines).all { (draftLine, confirmedLine) ->
            val draft = draftLine.split("\t")
            val confirmed = confirmedLine.split("\t")
            if (draft.size != confirmed.size || draft.size <= STICKER_IS_BACKGROUND_REMOVED) {
                return@all false
            }
            val sameFields = draft.indices.all { index ->
                index == STICKER_DISPLAYED_URI ||
                    index == STICKER_REMOVED_BG_URI ||
                    draft[index] == confirmed[index]
            }
            sameFields &&
                (
                    draft[STICKER_IS_BACKGROUND_REMOVED] != "true" ||
                        sameCutout(draft[STICKER_REMOVED_BG_URI], confirmed[STICKER_REMOVED_BG_URI])
                    )
        }
    }

    private fun sameCutout(draftUri: String, confirmedUri: String): Boolean {
        if (draftUri == confirmedUri) return true
        val draftFile = fileOf(draftUri) ?: return false
        val confirmedFile = fileOf(confirmedUri) ?: return false
        if (!draftFile.isFile || !confirmedFile.isFile) return false
        if (draftFile.length() != confirmedFile.length()) return false
        return runCatching { draftFile.readBytes().contentEquals(confirmedFile.readBytes()) }
            .getOrDefault(false)
    }

    /** 사진 스티커 줄들이 가리키는 file:// 경로(원본·표시·누끼)를 정규 경로로 모은다. */
    private fun stickerFileReferences(lines: List<String>): Set<String> =
        lines
            .flatMap { line ->
                val fields = line.split("\t")
                listOf(STICKER_ORIGINAL_URI, STICKER_DISPLAYED_URI, STICKER_REMOVED_BG_URI)
                    .mapNotNull { fields.getOrNull(it) }
            }
            .mapNotNull { fileOf(it) }
            .mapNotNull { runCatching { it.canonicalPath }.getOrNull() }
            .toSet()

    private fun fileOf(uriText: String): File? {
        if (!uriText.startsWith("file:")) return null
        return runCatching { URI(uriText).path }.getOrNull()?.let { File(it) }
    }

    /** 파일이 없으면 빈 목록, 읽기 실패면 null. */
    private fun readNonBlankLines(file: File): List<String>? {
        if (!file.exists()) return emptyList()
        return runCatching { file.readLines(Charsets.UTF_8).filter { it.isNotBlank() } }.getOrNull()
    }

    private fun scanPerPostcardFiles(
        type: String,
        directory: File,
        existingPostcardIds: Set<Long>,
        referencesFor: (Long) -> Set<String>,
        unclassified: MutableList<String>,
        reason: String
    ): OrphanCategoryResult {
        val orphans = mutableListOf<OrphanAsset>()

        for (entry in directory.listFiles()?.sortedBy { it.name }.orEmpty()) {
            val postcardId = entry.name.toLongOrNull()
            if (!entry.isDirectory || postcardId == null) {
                unclassified += entry.path
                continue
            }
            // Room에 없는 엽서의 폴더는 OrphanFileDiagnostics가 폴더째 보고한다.
            if (postcardId !in existingPostcardIds) continue

            val references = referencesFor(postcardId)
            for (file in entry.listFiles()?.sortedBy { it.name }.orEmpty()) {
                if (!file.isFile) {
                    unclassified += file.path
                    continue
                }
                val canonical = runCatching { file.canonicalPath }.getOrNull()
                if (canonical == null || canonical !in references) {
                    orphans += OrphanAsset(path = file.path, isDirectory = false, sizeBytes = file.length())
                }
            }
        }

        return OrphanCategoryResult(type = type, orphans = orphans, reason = reason)
    }

    private fun scanCacheFiles(
        directory: File,
        references: Set<String>
    ): OrphanCategoryResult {
        val orphans =
            directory.listFiles()
                ?.sortedBy { it.name }
                .orEmpty()
                .filter { it.isFile }
                .filter { file ->
                    val canonical = runCatching { file.canonicalPath }.getOrNull()
                    canonical == null || canonical !in references
                }
                .map { OrphanAsset(path = it.path, isDirectory = false, sizeBytes = it.length()) }

        return OrphanCategoryResult(
            type = "unreferencedStickerCache",
            orphans = orphans,
            reason = "저장된 확정 상태·초안이 가리키지 않는 스티커 캐시" +
                "(편집 중인 화면의 메모리·undo가 쓰는 파일일 수 있음, OS가 회수할 수 있는 영역)"
        )
    }

    private fun directorySize(directory: File): Long =
        directory
            .walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
}
