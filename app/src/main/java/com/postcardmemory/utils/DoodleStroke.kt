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

/**
 * 낙서 탭에서 선택 가능한 도구.
 *
 * ERASER는 획을 만들지 않고 기존 획을 지우기만 하므로 저장된 획의 tool로는
 * 쓰이지 않는다(역직렬화에서 PEN으로 되돌린다).
 */
enum class DoodleTool {
    PEN,
    HIGHLIGHTER,
    DOTTED,
    ERASER
}

/**
 * 형광펜 알파(0~255). 사진·배경을 완전히 가리지 않을 만큼만 덮는다.
 * 저장하지 않고 렌더 시점에만 적용하므로, 이 값을 바꾸면 예전에 그린
 * 형광펜 획도 함께 새 농도로 보인다.
 */
const val HIGHLIGHTER_ALPHA = 96

/** 형광펜이 같은 굵기 단계의 펜보다 넓어 보이는 배율. */
const val HIGHLIGHTER_WIDTH_MULTIPLIER = 1.9f

/** 엽서 기준 정규화 좌표. (0,0)=좌상단, (1,1)=우하단. */
data class DoodlePoint(val x: Float, val y: Float)

data class DoodleStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<DoodlePoint>,
    val colorArgb: Long,
    val width: DoodleStrokeWidth,
    val tool: DoodleTool = DoodleTool.PEN
)

/**
 * 도구 배율까지 반영한 실제 렌더 굵기(LOGICAL_SIZE 기준).
 *
 * 렌더러와 지우개 판정이 반드시 이 값 하나를 함께 써야 "보이는 굵기"와
 * "지워지는 굵기"가 어긋나지 않는다.
 */
val DoodleStroke.renderWidth: Float
    get() = when (tool) {
        DoodleTool.HIGHLIGHTER ->
            width.logicalWidth * HIGHLIGHTER_WIDTH_MULTIPLIER
        else -> width.logicalWidth
    }

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
        pointsEncoded,
        tool.name
    ).joinToString("\t")
}

/**
 * 저장된 tool 문자열을 도구로 되돌린다.
 *
 * 도구 필드는 낙서 기능보다 나중에 추가됐으므로, 필드가 없거나(4필드 구버전)
 * 비어 있거나 알 수 없는 값이어도 획을 버리지 않고 PEN으로 복원한다 —
 * 도구를 몰라도 좌표·색·굵기만 있으면 그 획을 그릴 수 있기 때문이다.
 * ERASER는 획으로 저장되지 않는 도구라 저장값으로 들어오면 PEN으로 본다.
 */
private fun parseStoredDoodleTool(raw: String?): DoodleTool {
    val parsed =
        DoodleTool.entries.firstOrNull { it.name == raw?.trim() }
            ?: return DoodleTool.PEN

    return if (parsed == DoodleTool.ERASER) DoodleTool.PEN else parsed
}

/**
 * 손상된 획은 예외를 던지지 않고 null을 반환한다(deserializePostcardSealItem,
 * deserializePhotoStickerItem과 동일한 정책). 점 목록 중 개별 좌표가 파싱되지
 * 않으면 그 점만 건너뛰고, 남은 점이 하나도 없으면 이 획 전체를 버린다.
 *
 * 다만 5번째 tool 필드는 예외다 — 도구를 몰라도 좌표·색·굵기만 있으면 그릴 수
 * 있으므로, 없거나 알 수 없는 값이면 획을 버리지 않고 PEN으로 복원한다
 * (parseStoredDoodleTool). 도구 필드가 없던 4필드 구버전 낙서도 그대로 열린다.
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
            width = width,
            tool = parseStoredDoodleTool(p.getOrNull(4))
        )
    }.getOrNull()
}
