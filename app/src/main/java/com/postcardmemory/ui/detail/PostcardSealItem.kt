package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import java.util.UUID

enum class SealType(
    val label: String,
    val isMiniStamp: Boolean = false,
    val defaultScale: Float = 1f
) {
    CIRCLE_POSTMARK("원형 소인"),
    WAVE_CANCEL("물결 취소선"),
    AIR_MAIL("AIR MAIL"),
    STAR("별 도장"),
    DOG_PAW("강아지", isMiniStamp = true, defaultScale = 0.55f),
    PIGEON_TRACK("비둘기", isMiniStamp = true, defaultScale = 0.55f),
    HEART("하트", isMiniStamp = true, defaultScale = 0.55f),
    STAR_STAMP("별", isMiniStamp = true, defaultScale = 0.55f)
}

private const val DEFAULT_SEAL_INK_ARGB = 0xFF252525L

data class PostcardSealItem(
    val id: String = UUID.randomUUID().toString(),
    val type: SealType,
    val offset: Offset? = null,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val colorArgb: Long = DEFAULT_SEAL_INK_ARGB
)

fun PostcardSealItem.serialize(): String =
    listOf(
        id,
        type.name,
        offset?.x?.toString() ?: "~",
        offset?.y?.toString() ?: "~",
        scale.toString(),
        rotationDegrees.toString(),
        colorArgb.toString()
    ).joinToString("\t")

fun deserializePostcardSealItem(
    line: String
): PostcardSealItem? {
    val p = line.split("\t")
    if (p.size < 7) return null
    return runCatching {
        val ox = p[2].takeIf { it != "~" }?.toFloat()
        val oy = p[3].takeIf { it != "~" }?.toFloat()
        // 인식하지 못하는 SealType이면(향후 enum 값이 없어지거나 이름이
        // 바뀌면) 임의의 다른 도장으로 조용히 대체하지 않고 이 항목만
        // 버린다 — deserializePhotoStickerItem, PostcardTemplate.
        // parseTemplateSeal과 동일한 정책(손상된 개별 항목은 건너뛴다).
        val type =
            SealType.entries
                .firstOrNull { it.name == p[1] }
                ?: return null

        PostcardSealItem(
            id = p[0],
            type = type,
            offset = if (ox != null && oy != null) {
                Offset(ox, oy)
            } else {
                null
            },
            scale = p[4].toFloat(),
            rotationDegrees = p[5].toFloat(),
            colorArgb = p[6].toLong()
        )
    }.getOrNull()
}
