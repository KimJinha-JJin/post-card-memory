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

/**
 * 120일차: 참조가 있다고도 없다고도 단정할 수 없어 잔여 후보에서 뺀 파일. 앱이 다시 쓸 수
 * 있는 길이 남아 있거나(예: 배경제거를 다시 켬), 상태·초안을 읽지 못해 판단 근거가 없는 경우다.
 */
data class PendingStickerAsset(
    val path: String,
    val sizeBytes: Long,
    val reason: String
)

data class StickerAssetScanResult(
    val categories: List<OrphanCategoryResult>,
    val drafts: List<DraftDiagnosisEntry>,
    val pending: List<PendingStickerAsset>,
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
 *
 * 120일차: 확정 누끼는 경로뿐 아니라 앱 복원 규칙(DetailViewModel.restorePersistedStickerBackground
 * ·persistStickerBackground)도 따른다 — 배경제거가 켜진 줄의 누끼 URI가 없거나(`~`), file://이
 * 아니거나, 그 파일을 읽을 수 없으면 앱은 `sticker_bgs/<엽서>/<스티커id>.png`를 대신 쓰므로 참조로
 * 친다. 다시 쓰일 길이 남은 파일과 상태·초안을 읽지 못한 엽서의 파일은 [PendingStickerAsset]으로
 * 따로 보고한다. 살아 있는 엽서의 `sticker_originals/<엽서>/` 원본도 같은 기준으로 진단한다.
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
    private const val STICKER_MIN_FIELD_COUNT = 8

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
        val pending = mutableListOf<PendingStickerAsset>()
        val confirmedStickerFile = DecorationStateFile.PHOTO_STICKER

        // 엽서별로 확정 상태와 초안이 가리키는 파일 경로를 모은다.
        val confirmedStates = mutableMapOf<Long, ConfirmedStickerReferences>()
        val draftReferences = mutableMapOf<Long, Set<String>>()
        // 상태나 초안을 읽지 못해 참조를 다 알 수 없는 엽서.
        val unclearPostcardIds = mutableSetOf<Long>()
        val drafts = mutableListOf<DraftDiagnosisEntry>()

        for (postcardId in existingPostcardIds.sorted()) {
            val confirmed =
                confirmedStickerReferences(
                    lines = readNonBlankLines(confirmedStickerFile.file(rootDirectory, postcardId)),
                    backgroundDirectory =
                        PostcardAssetDirectory.CONFIRMED_STICKER_BACKGROUNDS
                            .directory(rootDirectory, postcardId)
                )
            confirmedStates[postcardId] = confirmed
            if (confirmed.unclear) unclearPostcardIds += postcardId

            val draftFile = PostcardDraftStorage.draftFile(rootDirectory, postcardId)
            if (!draftFile.isFile) continue

            val draftSections = readDraftSections(draftFile)
            val draftStickers = draftSections?.stickers.orEmpty().filter { it.isNotBlank() }
            draftReferences[postcardId] = stickerFileReferences(draftStickers)
            if (draftSections == null || draftStickers.any { !isRestorableStickerLine(it) }) {
                unclearPostcardIds += postcardId
            }
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
            (
                confirmedStates.values.flatMap { it.references } +
                    draftReferences.values.flatten()
                ).toSet()
        val unclearPostcardReason = "이 엽서의 사진 스티커 상태나 초안을 읽지 못해 참조를 판정할 수 없음"
        val unclearAnywhereReason =
            unclearPostcardIds.takeIf { it.isNotEmpty() }
                ?.let { ids -> "사진 스티커 상태나 초안을 읽지 못한 엽서가 있어 판정할 수 없음(엽서 ${ids.sorted()})" }

        val categories = listOf(
            scanPerPostcardFiles(
                type = "unreferencedConfirmedStickerBackground",
                directory = PostcardAssetDirectory.CONFIRMED_STICKER_BACKGROUNDS.root(rootDirectory),
                existingPostcardIds = existingPostcardIds,
                referencesFor = { id ->
                    confirmedStates[id]?.references.orEmpty() + draftReferences[id].orEmpty()
                },
                pendingReasonFor = { id, canonicalPath ->
                    confirmedStates[id]?.pendingReasons?.get(canonicalPath)
                        ?: unclearPostcardReason.takeIf { id in unclearPostcardIds }
                },
                unclassified = unclassified,
                pending = pending,
                reason = "엽서는 있지만 확정 상태(스티커 id로 대신 찾는 누끼 포함)·초안의 사진 스티커가 가리키지 않는 확정 누끼"
            ),
            scanPerPostcardFiles(
                type = "unreferencedDraftStickerBackground",
                directory = File(rootDirectory, PostcardDraftStorage.DRAFT_STICKER_BG_DIR_NAME),
                existingPostcardIds = existingPostcardIds,
                referencesFor = { id ->
                    draftReferences[id].orEmpty() + confirmedStates[id]?.references.orEmpty()
                },
                pendingReasonFor = { id, _ ->
                    unclearPostcardReason.takeIf { id in unclearPostcardIds }
                },
                unclassified = unclassified,
                pending = pending,
                reason = "엽서는 있지만 그 엽서의 초안이 가리키지 않는 초안 누끼(초안 파일 없음 포함)"
            ),
            scanPerPostcardFiles(
                type = "unreferencedStickerOriginal",
                directory = PostcardAssetDirectory.STICKER_ORIGINALS.root(rootDirectory),
                existingPostcardIds = existingPostcardIds,
                referencesFor = { allReferences },
                pendingReasonFor = { _, _ -> unclearAnywhereReason },
                unclassified = unclassified,
                pending = pending,
                reason = "엽서는 있지만 어느 엽서의 확정 상태·초안도 원본으로 가리키지 않는 스티커 원본" +
                    "(편집 중인 화면·undo가 쓰거나, 고른 뒤 붙이기 전에 화면을 떠난 사진일 수 있음)"
            ),
            scanCacheFiles(
                directory = File(cacheDirectory, STICKER_CACHE_DIR_NAME),
                references = allReferences,
                pendingReason = unclearAnywhereReason,
                pending = pending
            )
        )

        return StickerAssetScanResult(
            categories = categories,
            drafts = drafts.toList(),
            pending = pending.toList(),
            unclassified = unclassified.toList()
        )
    }

    /**
     * 확정 사진 스티커 상태 한 장이 가리키는 파일.
     * [references]는 앱이 실제로 쓰는 파일(정규 경로), [pendingReasons]는 지금은 안 쓰지만
     * 다시 쓰일 수 있는 id 누끼와 그 이유, [unclear]는 상태를 다 읽지 못했는지다.
     */
    private class ConfirmedStickerReferences(
        val references: Set<String>,
        val pendingReasons: Map<String, String>,
        val unclear: Boolean
    )

    /** 앱(deserializePhotoStickerItem)이 읽을 수 있는 줄 형식인지(최소 8칸, id 있음). */
    private fun isRestorableStickerLine(line: String): Boolean {
        val fields = line.split("\t")
        return fields.size >= STICKER_MIN_FIELD_COUNT && fields[0].isNotBlank()
    }

    private fun confirmedStickerReferences(
        lines: List<String>?,
        backgroundDirectory: File
    ): ConfirmedStickerReferences {
        if (lines == null) {
            return ConfirmedStickerReferences(emptySet(), emptyMap(), unclear = true)
        }

        val references = stickerFileReferences(lines).toMutableSet()
        val pendingReasons = mutableMapOf<String, String>()
        var unclear = false

        for (line in lines) {
            if (!isRestorableStickerLine(line)) {
                unclear = true
                continue
            }
            val fields = line.split("\t")
            val idBackground =
                runCatching { File(backgroundDirectory, fields[0] + ".png").canonicalPath }
                    .getOrNull()
                    ?: continue

            if (fields[STICKER_IS_BACKGROUND_REMOVED] != "true") {
                pendingReasons[idBackground] =
                    "배경제거를 끈 스티커의 확정 누끼 — 다시 켤 때 이 파일을 쓰는지 확인되지 않음"
                continue
            }

            // 복원·저장은 누끼 URI의 파일을 먼저 쓰고, 그 파일이 없을 때만 id 누끼로 돌아간다.
            val explicit =
                fileOf(fields[STICKER_REMOVED_BG_URI])
                    ?.takeIf { it.isFile && it.canRead() }
            val explicitPath = explicit?.let { runCatching { it.canonicalPath }.getOrNull() }
            when {
                explicit == null -> references += idBackground
                explicitPath != idBackground ->
                    pendingReasons[idBackground] =
                        "누끼 URI가 다른 파일을 가리켜 지금은 안 쓰지만, 그 파일이 사라지면 대신 쓰이고 " +
                            "다음 완료 저장 때 덮어쓰일 확정 누끼"
            }
        }

        references.forEach { pendingReasons.remove(it) }
        return ConfirmedStickerReferences(references, pendingReasons, unclear)
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
        pendingReasonFor: (postcardId: Long, canonicalPath: String) -> String?,
        unclassified: MutableList<String>,
        pending: MutableList<PendingStickerAsset>,
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
                if (canonical != null && canonical in references) continue

                val pendingReason = pendingReasonFor(postcardId, canonical ?: file.path)
                if (pendingReason != null) {
                    pending += PendingStickerAsset(path = file.path, sizeBytes = file.length(), reason = pendingReason)
                } else {
                    orphans += OrphanAsset(path = file.path, isDirectory = false, sizeBytes = file.length())
                }
            }
        }

        return OrphanCategoryResult(type = type, orphans = orphans, reason = reason)
    }

    private fun scanCacheFiles(
        directory: File,
        references: Set<String>,
        pendingReason: String?,
        pending: MutableList<PendingStickerAsset>
    ): OrphanCategoryResult {
        val unreferenced =
            directory.listFiles()
                ?.sortedBy { it.name }
                .orEmpty()
                .filter { it.isFile }
                .filter { file ->
                    val canonical = runCatching { file.canonicalPath }.getOrNull()
                    canonical == null || canonical !in references
                }
        val orphans =
            if (pendingReason != null) {
                unreferenced.forEach { file ->
                    pending += PendingStickerAsset(path = file.path, sizeBytes = file.length(), reason = pendingReason)
                }
                emptyList()
            } else {
                unreferenced.map { OrphanAsset(path = it.path, isDirectory = false, sizeBytes = it.length()) }
            }

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
