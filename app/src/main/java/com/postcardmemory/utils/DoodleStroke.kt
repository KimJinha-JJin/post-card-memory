package com.postcardmemory.utils

import java.util.UUID

/**
 * 낙서 굵기 3단계. PostcardRenderSpec.LOGICAL_SIZE(2048) 기준 절대값이라
 * 미리보기·내보내기 등 실제 캔버스 크기가 달라도 같은 비율로 렌더된다.
 */
enum class DoodleStrokeWidth(val logicalWidth: Float) {
    THIN(16f),
    MEDIUM(30f),
    THICK(50f)
}

/** 엽서 기준 정규화 좌표. (0,0)=좌상단, (1,1)=우하단. */
data class DoodlePoint(val x: Float, val y: Float)

data class DoodleStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<DoodlePoint>,
    val colorArgb: Long,
    val width: DoodleStrokeWidth
)

private fun sanitizedNormalizedValue(value: Float): Float {
    if (value.isNaN() || value.isInfinite()) return 0.5f
    return value.coerceIn(0f, 1f)
}

/** 손상되거나 범위를 벗어난 좌표가 렌더를 중단시키지 않도록 항상 유효한 값으로 보정한다. */
fun sanitizedDoodlePoint(x: Float, y: Float): DoodlePoint =
    DoodlePoint(
        x = sanitizedNormalizedValue(x),
        y = sanitizedNormalizedValue(y)
    )

fun DoodleStroke.serialize(): String {
    val pointsEncoded =
        points.joinToString(";") { "${it.x},${it.y}" }

    return listOf(
        id,
        colorArgb.toString(),
        width.name,
        pointsEncoded
    ).joinToString("\t")
}

/**
 * 손상된 획은 예외를 던지지 않고 null을 반환한다(deserializePostcardSealItem,
 * deserializePhotoStickerItem과 동일한 정책). 점 목록 중 개별 좌표가 파싱되지
 * 않으면 그 점만 건너뛰고, 남은 점이 하나도 없으면 이 획 전체를 버린다.
 */
fun deserializeDoodleStroke(line: String): DoodleStroke? {
    val p = line.split("\t")
    if (p.size < 4) return null

    return runCatching {
        val width =
            DoodleStrokeWidth.entries.firstOrNull { it.name == p[2] }
                ?: return null

        val points = p[3]
            .split(";")
            .filter { it.isNotBlank() }
            .mapNotNull { encoded ->
                val xy = encoded.split(",")
                if (xy.size != 2) return@mapNotNull null
                val x = xy[0].toFloatOrNull() ?: return@mapNotNull null
                val y = xy[1].toFloatOrNull() ?: return@mapNotNull null
                sanitizedDoodlePoint(x, y)
            }

        if (points.isEmpty()) return null

        DoodleStroke(
            id = p[0],
            points = points,
            colorArgb = p[1].toLong(),
            width = width
        )
    }.getOrNull()
}
