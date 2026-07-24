package com.postcardmemory.ui.detail

import com.postcardmemory.data.Postcard
import java.util.UUID

const val TEMPLATE_FORMAT_VERSION = 1

private const val TEMPLATE_HEADER = "POSTCARD_TEMPLATE_V1"

/**
 * 엽서를 "꾸미는 방식"만 담은 스타일 값. 사진 원본·문구·날짜·스티커 등
 * 개인 기록은 절대 포함하지 않는다 — Postcard.toTemplateStyle()/
 * applyTemplateStyle()이 이 경계를 지킨다.
 */
data class PostcardTemplateStyle(
    val layoutStyle: String,
    val backgroundColorArgb: Long,
    val backgroundPattern: String,
    val backgroundPatternDensity: Float,
    val messageFont: String,
    val dateFormat: String,
    val messageTextScale: Float,
    val dateTextScale: Float,
    val photoEdgeBlur: Float,
    val stampPhotoScale: Float,
    val stampPhotoOffsetX: Float,
    val stampPhotoOffsetY: Float,
    val stampPhotoZoom: Float,
    val polaroidPhotoScale: Float,
    val polaroidPhotoOffsetX: Float,
    val polaroidPhotoOffsetY: Float,
    val polaroidPhotoZoom: Float,
    val tapedFilmPhotoOffsetX: Float,
    val tapedFilmPhotoOffsetY: Float,
    val tapedFilmPhotoZoom: Float
)

/** 도장이 없는 엽서에만 적용되는 선택적 도장 스타일(위치는 신규 도장과 동일하게 자동 배치). */
data class PostcardTemplateSeal(
    val type: SealType,
    val colorArgb: Long
)

enum class TemplateSource {
    BUILT_IN,
    USER
}

data class PostcardTemplate(
    val id: String = UUID.randomUUID().toString(),
    val source: TemplateSource = TemplateSource.USER,
    val name: String,
    val style: PostcardTemplateStyle,
    val seal: PostcardTemplateSeal? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

/** 현재 엽서의 스타일 값만 뽑아낸다("현재 꾸밈 저장"/"덮어쓰기"에서 사용). */
fun Postcard.toTemplateStyle(): PostcardTemplateStyle =
    PostcardTemplateStyle(
        layoutStyle = layoutStyle,
        backgroundColorArgb = backgroundColorArgb,
        backgroundPattern = backgroundPattern,
        backgroundPatternDensity = backgroundPatternDensity,
        messageFont = messageFont,
        dateFormat = dateFormat,
        messageTextScale = messageTextScale,
        dateTextScale = dateTextScale,
        photoEdgeBlur = photoEdgeBlur,
        stampPhotoScale = stampPhotoScale,
        stampPhotoOffsetX = stampPhotoOffsetX,
        stampPhotoOffsetY = stampPhotoOffsetY,
        stampPhotoZoom = stampPhotoZoom,
        polaroidPhotoScale = polaroidPhotoScale,
        polaroidPhotoOffsetX = polaroidPhotoOffsetX,
        polaroidPhotoOffsetY = polaroidPhotoOffsetY,
        polaroidPhotoZoom = polaroidPhotoZoom,
        tapedFilmPhotoOffsetX = tapedFilmPhotoOffsetX,
        tapedFilmPhotoOffsetY = tapedFilmPhotoOffsetY,
        tapedFilmPhotoZoom = tapedFilmPhotoZoom
    )

/**
 * 스타일 값만 교체한 새 Postcard를 돌려준다. id/imagePath/title/message/
 * capturedAt/location/backgroundImagePath는 그대로 유지된다 — 템플릿은
 * "꾸미는 방식"만 바꾸고 사진·문구·날짜 같은 기록은 절대 건드리지 않는다.
 */
fun Postcard.applyTemplateStyle(
    style: PostcardTemplateStyle
): Postcard =
    copy(
        layoutStyle = style.layoutStyle,
        backgroundColorArgb = style.backgroundColorArgb,
        backgroundPattern = style.backgroundPattern,
        backgroundPatternDensity = style.backgroundPatternDensity,
        messageFont = style.messageFont,
        dateFormat = style.dateFormat,
        messageTextScale = style.messageTextScale,
        dateTextScale = style.dateTextScale,
        photoEdgeBlur = style.photoEdgeBlur,
        stampPhotoScale = style.stampPhotoScale,
        stampPhotoOffsetX = style.stampPhotoOffsetX,
        stampPhotoOffsetY = style.stampPhotoOffsetY,
        stampPhotoZoom = style.stampPhotoZoom,
        polaroidPhotoScale = style.polaroidPhotoScale,
        polaroidPhotoOffsetX = style.polaroidPhotoOffsetX,
        polaroidPhotoOffsetY = style.polaroidPhotoOffsetY,
        polaroidPhotoZoom = style.polaroidPhotoZoom,
        tapedFilmPhotoOffsetX = style.tapedFilmPhotoOffsetX,
        tapedFilmPhotoOffsetY = style.tapedFilmPhotoOffsetY,
        tapedFilmPhotoZoom = style.tapedFilmPhotoZoom
    )

private fun PostcardTemplateStyle.serialize(): String =
    listOf(
        layoutStyle,
        backgroundColorArgb.toString(),
        backgroundPattern,
        backgroundPatternDensity.toString(),
        messageFont,
        dateFormat,
        messageTextScale.toString(),
        dateTextScale.toString(),
        photoEdgeBlur.toString(),
        stampPhotoScale.toString(),
        stampPhotoOffsetX.toString(),
        stampPhotoOffsetY.toString(),
        stampPhotoZoom.toString(),
        polaroidPhotoScale.toString(),
        polaroidPhotoOffsetX.toString(),
        polaroidPhotoOffsetY.toString(),
        polaroidPhotoZoom.toString(),
        tapedFilmPhotoOffsetX.toString(),
        tapedFilmPhotoOffsetY.toString(),
        tapedFilmPhotoZoom.toString()
    ).joinToString("\t")

private fun parseTemplateStyle(line: String): PostcardTemplateStyle? {
    val p = line.split("\t")
    if (p.size < 20) return null

    return runCatching {
        PostcardTemplateStyle(
            layoutStyle = p[0],
            backgroundColorArgb = p[1].toLong(),
            backgroundPattern = p[2],
            backgroundPatternDensity = p[3].toFloat(),
            messageFont = p[4],
            dateFormat = p[5],
            messageTextScale = p[6].toFloat(),
            dateTextScale = p[7].toFloat(),
            photoEdgeBlur = p[8].toFloat(),
            stampPhotoScale = p[9].toFloat(),
            stampPhotoOffsetX = p[10].toFloat(),
            stampPhotoOffsetY = p[11].toFloat(),
            stampPhotoZoom = p[12].toFloat(),
            polaroidPhotoScale = p[13].toFloat(),
            polaroidPhotoOffsetX = p[14].toFloat(),
            polaroidPhotoOffsetY = p[15].toFloat(),
            polaroidPhotoZoom = p[16].toFloat(),
            tapedFilmPhotoOffsetX = p[17].toFloat(),
            tapedFilmPhotoOffsetY = p[18].toFloat(),
            tapedFilmPhotoZoom = p[19].toFloat()
        )
    }.getOrNull()
}

private fun PostcardTemplateSeal.serialize(): String =
    listOf(
        type.name,
        colorArgb.toString()
    ).joinToString("\t")

private fun parseTemplateSeal(line: String): PostcardTemplateSeal? {
    val p = line.split("\t")
    if (p.size < 2) return null

    return runCatching {
        val type =
            SealType.entries.firstOrNull { it.name == p[0] }
                ?: return null

        PostcardTemplateSeal(
            type = type,
            colorArgb = p[1].toLong()
        )
    }.getOrNull()
}

/**
 * 사용자 템플릿(내장 템플릿은 파일로 저장하지 않으므로 여기서 다루지
 * 않는다) 하나를 파일 하나로 직렬화한다. 이름에 탭·개행이 섞이면 형식이
 * 깨지므로 저장 전에 항상 제거한다.
 */
fun PostcardTemplate.serialize(): String {
    val safeName =
        name.replace("\t", " ").replace("\n", " ")
    val metaLine =
        listOf(
            id,
            safeName,
            createdAtMillis.toString(),
            updatedAtMillis.toString(),
            if (seal != null) "1" else "0"
        ).joinToString("\t")

    val lines =
        mutableListOf(
            TEMPLATE_HEADER,
            metaLine,
            style.serialize()
        )

    if (seal != null) {
        lines += seal.serialize()
    }

    return lines.joinToString("\n")
}

/**
 * 손상되었거나 형식이 맞지 않는 템플릿은 예외 없이 null을 반환한다 —
 * 호출부가 이 템플릿 하나만 목록에서 제외하고 나머지는 그대로 읽는다.
 */
fun parsePostcardTemplate(text: String): PostcardTemplate? {
    val lines = text.split("\n")
    if (lines.size < 3) return null
    if (lines[0].trim() != TEMPLATE_HEADER) return null

    val meta = lines[1].split("\t")
    if (meta.size < 5) return null

    return runCatching {
        val id = meta[0]
        val name = meta[1]
        val createdAtMillis = meta[2].toLong()
        val updatedAtMillis = meta[3].toLong()
        val hasSeal = meta[4] == "1"

        if (id.isBlank() || name.isBlank()) return null

        val style =
            parseTemplateStyle(lines[2]) ?: return null

        val seal =
            if (hasSeal) {
                val sealLine = lines.getOrNull(3) ?: return null
                parseTemplateSeal(sealLine) ?: return null
            } else {
                null
            }

        PostcardTemplate(
            id = id,
            source = TemplateSource.USER,
            name = name,
            style = style,
            seal = seal,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis
        )
    }.getOrNull()
}
