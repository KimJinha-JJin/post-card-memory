package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import java.util.UUID

private const val DEFAULT_TEXT_STICKER_COLOR_ARGB = 0xFF252525L
const val DEFAULT_TEXT_STICKER_OUTLINE_COLOR_ARGB = 0xFFFFFFFFL

/**
 * 사용자가 입력한 문자열을 그대로 붙이는 텍스트 스티커. PostcardSealItem과
 * 같은 형태(id/offset/scale/rotationDegrees/colorArgb)를 따르되, type
 * 대신 text를 갖는다는 점에서 근본적으로 다른 모델이라 seal/sticker
 * 모델을 확장하지 않고 독립된 타입으로 둔다.
 */
data class TextStickerItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val offset: Offset? = null,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    val colorArgb: Long = DEFAULT_TEXT_STICKER_COLOR_ARGB,
    val outlineColorArgb: Long = DEFAULT_TEXT_STICKER_OUTLINE_COLOR_ARGB
)

/**
 * 탭으로 필드를 구분하는 기존 sticker/seal 직렬화 관례를 그대로 따른다.
 * text 자체에는 개행을 허용하지 않는다(입력 UI에서 막는다) — 줄 단위로
 * 읽는 저장 파일 형식과 충돌하지 않기 위함이며, 문자열 내용을 임의로
 * 정리하는 것과는 다르다.
 */
fun TextStickerItem.serialize(): String =
    listOf(
        id,
        text,
        offset?.x?.toString() ?: "~",
        offset?.y?.toString() ?: "~",
        scale.toString(),
        rotationDegrees.toString(),
        colorArgb.toString(),
        outlineColorArgb.toString()
    ).joinToString("\t")

fun deserializeTextStickerItem(
    line: String
): TextStickerItem? {
    val p = line.split("\t")
    if (p.size < 7) return null
    return runCatching {
        val ox = p[2].takeIf { it != "~" }?.toFloat()
        val oy = p[3].takeIf { it != "~" }?.toFloat()

        TextStickerItem(
            id = p[0],
            text = p[1],
            offset = if (ox != null && oy != null) {
                Offset(ox, oy)
            } else {
                null
            },
            scale = p[4].toFloat(),
            rotationDegrees = p[5].toFloat(),
            colorArgb = p[6].toLong(),
            outlineColorArgb = p.getOrNull(7)?.toLongOrNull()
                ?: DEFAULT_TEXT_STICKER_OUTLINE_COLOR_ARGB
        )
    }.getOrNull()
}
