package com.postcardmemory.ui.detail

import com.postcardmemory.utils.DoodleStroke
import com.postcardmemory.utils.deserializeDoodleStroke
import com.postcardmemory.utils.serialize as serializeDoodleStroke

const val DRAFT_FORMAT_VERSION = 2

private const val DRAFT_HEADER = "POSTCARD_DRAFT_V1"

/**
 * 완성 저장본과 분리된, 진행 중인 스티커·도장·낙서 편집 상태의 스냅샷.
 * 사진·배경·문구·날짜·폰트·레이아웃은 Room에 이미 실시간 저장되므로 여기 포함하지 않는다.
 */
data class PostcardEditDraft(
    val draftFormatVersion: Int = DRAFT_FORMAT_VERSION,
    val postcardId: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val revision: Long,
    val stickers: List<PhotoStickerItem>,
    val selectedStickerId: String?,
    val seals: List<PostcardSealItem>,
    val selectedSealId: String?,
    val doodleStrokes: List<DoodleStroke> = emptyList()
)

/** 오래된 저장 요청이 최신 상태를 덮어쓰지 않도록 하는 순수 판정 함수. */
internal fun shouldPersistDraftRevision(
    candidateRevision: Long,
    latestPersistedRevision: Long
): Boolean = candidateRevision >= latestPersistedRevision

fun PostcardEditDraft.serialize(): String {
    val metaLine = listOf(
        draftFormatVersion.toString(),
        postcardId.toString(),
        createdAtMillis.toString(),
        updatedAtMillis.toString(),
        revision.toString(),
        selectedStickerId ?: "~",
        selectedSealId ?: "~",
        stickers.size.toString(),
        seals.size.toString(),
        doodleStrokes.size.toString()
    ).joinToString("\t")

    val lines = mutableListOf(DRAFT_HEADER, metaLine)
    stickers.forEach { lines += it.serialize() }
    seals.forEach { lines += it.serialize() }
    doodleStrokes.forEach { lines += it.serializeDoodleStroke() }

    return lines.joinToString("\n")
}

/**
 * 손상되었거나 형식이 맞지 않는 초안은 예외를 던지지 않고 null을 반환한다.
 * 개별 스티커/도장/낙서 라인이 손상됐으면 해당 항목만 건너뛰고 나머지는 복구한다.
 *
 * meta 필드가 9개뿐인 구버전(DRAFT_FORMAT_VERSION=1) 초안은 낙서 개수 필드가
 * 없으므로 낙서 없는 초안으로 해석한다 — 낙서 도입 전에 저장된 초안도 그대로 복원된다.
 */
fun parsePostcardEditDraft(text: String): PostcardEditDraft? {
    val lines = text.split("\n")
    if (lines.size < 2) return null
    if (lines[0].trim() != DRAFT_HEADER) return null

    val meta = lines[1].split("\t")
    if (meta.size < 9) return null

    return runCatching {
        val formatVersion = meta[0].toInt()
        val postcardId = meta[1].toLong()
        val createdAtMillis = meta[2].toLong()
        val updatedAtMillis = meta[3].toLong()
        val revision = meta[4].toLong()
        val selectedStickerId = meta[5].takeIf { it != "~" }
        val selectedSealId = meta[6].takeIf { it != "~" }
        val stickerCount = meta[7].toInt()
        val sealCount = meta[8].toInt()
        val doodleCount = meta.getOrNull(9)?.toIntOrNull() ?: 0

        if (stickerCount < 0 || sealCount < 0 || doodleCount < 0) return null

        val bodyLines = lines.drop(2)
        if (bodyLines.size < stickerCount + sealCount + doodleCount) return null

        val stickers = bodyLines
            .subList(0, stickerCount)
            .mapNotNull { deserializePhotoStickerItem(it) }

        val seals = bodyLines
            .subList(stickerCount, stickerCount + sealCount)
            .mapNotNull { deserializePostcardSealItem(it) }

        val doodleStrokes = bodyLines
            .subList(
                stickerCount + sealCount,
                stickerCount + sealCount + doodleCount
            )
            .mapNotNull { deserializeDoodleStroke(it) }

        PostcardEditDraft(
            draftFormatVersion = formatVersion,
            postcardId = postcardId,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            revision = revision,
            stickers = stickers,
            selectedStickerId = selectedStickerId,
            seals = seals,
            selectedSealId = selectedSealId,
            doodleStrokes = doodleStrokes
        )
    }.getOrNull()
}
