package com.postcardmemory.ui.detail

import android.net.Uri
import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * 마스킹테이프 모양·패턴을 그릴 때 쓰는 비율 상수. 미리보기(Compose,
 * MaskingTapeShapes.kt)와 저장 이미지(Canvas, PostcardImageExporter.kt)
 * 양쪽이 같은 값을 참조해야 두 렌더 결과가 어긋나지 않는다 — 화면과
 * 공유 결과가 달라지는 요소를 만들지 않는다는 정책의 핵심 근거.
 */
const val MASKING_TAPE_CORNER_NOTCH_RATIO_A = 0.42f
const val MASKING_TAPE_CORNER_NOTCH_RATIO_B = 0.20f
const val MASKING_TAPE_PATTERN_PITCH_RATIO = 0.42f
const val MASKING_TAPE_DOT_RADIUS_RATIO = 0.09f
const val MASKING_TAPE_GRID_LINE_WIDTH_RATIO = 0.045f
const val MASKING_TAPE_STAR_SIZE_RATIO = 0.22f
const val MASKING_TAPE_HEART_SIZE_RATIO = 0.30f
const val MASKING_TAPE_STRIPE_PITCH_RATIO = 0.38f
const val MASKING_TAPE_STRIPE_WIDTH_RATIO = 0.10f
const val MASKING_TAPE_PATTERN_ALPHA = 0.55f
const val MASKING_TAPE_PLAIN_FIBER_ALPHA = 0.18f

/** 커스텀 디자인의 기본 alpha. opacity 조절 UI는 제공하지 않으므로(프리셋과 동일 정책) 고정값. */
const val MASKING_TAPE_CUSTOM_ALPHA = 0.78f

/** 사진 마스킹테이프의 기본 alpha. 사진이 좀 더 또렷이 보이도록 프리셋보다 살짝 높다. */
const val MASKING_TAPE_PHOTO_ALPHA = 0.85f

/** 길이(가로) 배율의 허용 범위. 폭(scale)과 별개로 가로로만 늘이거나 줄인다. */
const val MASKING_TAPE_MIN_LENGTH_SCALE = 0.5f
const val MASKING_TAPE_MAX_LENGTH_SCALE = 3f

/** 굵기(세로) 배율의 허용 범위. scale과 별개로 세로 두께만 늘이거나 줄인다. */
const val MASKING_TAPE_MIN_THICKNESS_SCALE = 0.5f
const val MASKING_TAPE_MAX_THICKNESS_SCALE = 3f

/**
 * 회전 slider의 허용 범위. 기존 두 손가락 회전 제스처가 정규화하던 범위
 * (normalizeStickerRotation의 (-180, 180])와 동일해, 패널로 옮긴 뒤에도
 * 저장값의 의미가 달라지지 않는다.
 */
const val MASKING_TAPE_MIN_ROTATION_DEGREES = -180f
const val MASKING_TAPE_MAX_ROTATION_DEGREES = 180f

/**
 * 찢긴/삐뚤빼뚤한 가장자리를 표현할 때 쓰는 고정 좌표. 매 프레임 또는
 * 인스턴스마다 달라지는 무작위 값이 아니라, 모든 마스킹테이프가 같은
 * 모양을 재현하도록 고정한다(작업지시서 16절: 화면·공유 결과가 달라지는
 * 랜덤 요소 금지). yFraction은 세로 위치(0~1), xOffsetFraction은 테이프
 * 높이 대비 가로 삐침 정도다.
 */
internal val MASKING_TAPE_TORN_EDGE_Y_FRACTIONS =
    listOf(0f, 0.18f, 0.36f, 0.52f, 0.7f, 0.86f, 1f)
internal val MASKING_TAPE_TORN_EDGE_X_OFFSET_FRACTIONS =
    listOf(0f, -0.22f, 0.16f, -0.28f, 0.20f, -0.12f, 0f)

/** 마스킹테이프 패턴을 코드로 그릴 때 쓰는 기본 도안 종류. */
enum class MaskingTapePatternKind {
    GRID,
    DOT,
    STAR,
    HEART,
    STRIPE,
    PLAIN
}

/**
 * 마스킹테이프 가장자리 모양. 실제 다꾸 테이프의 다양한 절단면을
 * 반영한다 — 하나의 절단 방식만 있으면 전부 같은 UI 버튼처럼 보인다.
 */
enum class MaskingTapeEdgeStyle(val label: String) {
    /** 기존 기본값: 양쪽 모서리를 살짝 잘라낸 형태. */
    NOTCHED_BOTH("모서리컷"),

    /** 재단기로 자른 것처럼 일자로 각진 사각형. */
    STRAIGHT("일자"),

    /** 한쪽 끝만 손으로 찢은 듯 삐뚤빼뚤하고 반대쪽은 일자. */
    TORN_ONE_SIDE("한쪽찢김"),

    /** 양쪽 끝 모두 손끝으로 뜯어낸 듯 삐뚤빼뚤. */
    JAGGED_BOTH("양쪽찢김")
}

/**
 * 마스킹테이프 디자인 한 종류. 사용자에게 opacity 조절 UI를 제공하지
 * 않으므로 디자인별 alpha·색상을 여기 고정값으로 둔다.
 *
 * CUSTOM/PHOTO는 프리셋 그리드에 노출하지 않는 마커 엔트리다 —
 * [presetMaskingTapeStyles] 참고. 실제 색상·패턴은 CUSTOM일 때
 * [MaskingTapeItem.customBaseColorArgb] 등 커스텀 필드로, PHOTO일 때
 * [MaskingTapeItem.photoUri]로 대체된다. 여기 적힌 값은 그 필드가
 * 비정상적으로 비어 있을 때만 쓰는 안전한 폴백이다.
 */
enum class MaskingTapeStyle(
    val label: String,
    val baseColorArgb: Long,
    val patternColorArgb: Long,
    val patternKind: MaskingTapePatternKind,
    val alpha: Float
) {
    MINT_CHECK(
        label = "민트 체크",
        baseColorArgb = 0xFFDDF5EEL,
        patternColorArgb = 0xFF6FBFA0L,
        patternKind = MaskingTapePatternKind.GRID,
        alpha = 0.78f
    ),
    LAVENDER_DOT(
        label = "라벤더 도트",
        baseColorArgb = 0xFFE8E4FFL,
        patternColorArgb = 0xFF9285D9L,
        patternKind = MaskingTapePatternKind.DOT,
        alpha = 0.78f
    ),
    PALE_YELLOW_STAR(
        label = "연노랑 별",
        baseColorArgb = 0xFFFFF1C7L,
        patternColorArgb = 0xFFDDA82CL,
        patternKind = MaskingTapePatternKind.STAR,
        alpha = 0.78f
    ),
    PINK_HEART(
        label = "연분홍 하트",
        baseColorArgb = 0xFFFFE2F2L,
        patternColorArgb = 0xFFE0709FL,
        patternKind = MaskingTapePatternKind.HEART,
        alpha = 0.78f
    ),
    SKY_BLUE_GRID(
        label = "하늘색 격자",
        baseColorArgb = 0xFFDDEFFFL,
        patternColorArgb = 0xFF5E9BD9L,
        patternKind = MaskingTapePatternKind.GRID,
        alpha = 0.78f
    ),
    KRAFT_STRIPE(
        label = "크라프트 종이",
        baseColorArgb = 0xFFE0CDAEL,
        patternColorArgb = 0xFF9C7B4FL,
        patternKind = MaskingTapePatternKind.STRIPE,
        alpha = 0.82f
    ),
    IVORY_PLAIN(
        label = "반투명 아이보리",
        baseColorArgb = 0xFFFFFBF3L,
        patternColorArgb = 0xFFD8CBB0L,
        patternKind = MaskingTapePatternKind.PLAIN,
        alpha = 0.7f
    ),
    CUSTOM(
        label = "커스텀",
        baseColorArgb = 0xFFF4ECDEL,
        patternColorArgb = 0xFF8C5F00L,
        patternKind = MaskingTapePatternKind.PLAIN,
        alpha = MASKING_TAPE_CUSTOM_ALPHA
    ),
    PHOTO(
        label = "사진",
        baseColorArgb = 0xFFFFFBF3L,
        patternColorArgb = 0xFFD8CBB0L,
        patternKind = MaskingTapePatternKind.PLAIN,
        alpha = MASKING_TAPE_PHOTO_ALPHA
    )
}

/** 프리셋 디자인 선택 그리드에 표시할 스타일들. 커스텀/사진은 별도 진입점으로만 만든다. */
val presetMaskingTapeStyles: List<MaskingTapeStyle> =
    MaskingTapeStyle.entries.filterNot {
        it == MaskingTapeStyle.CUSTOM || it == MaskingTapeStyle.PHOTO
    }

data class MaskingTapeItem(
    val id: String = UUID.randomUUID().toString(),
    val style: MaskingTapeStyle,
    val offset: Offset? = null,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    /** 폭(scale)과 별개로 가로 길이만 늘이거나 줄이는 배율. */
    val lengthScale: Float = 1f,
    /** 폭(scale)과 별개로 세로 굵기만 늘이거나 줄이는 배율. */
    val thicknessScale: Float = 1f,
    val edgeStyle: MaskingTapeEdgeStyle = MaskingTapeEdgeStyle.NOTCHED_BOTH,
    val customBaseColorArgb: Long? = null,
    val customPatternColorArgb: Long? = null,
    val customPatternKind: MaskingTapePatternKind? = null,
    /** style == PHOTO일 때만 쓰는, 영구 저장소에 복사된 사용자 사진. */
    val photoUri: Uri? = null
)

/** style==CUSTOM이면 커스텀 색상을, 아니면(비어 있어도) 프리셋 고정값을 안전하게 반환한다. */
fun MaskingTapeItem.effectiveBaseColorArgb(): Long =
    if (style == MaskingTapeStyle.CUSTOM) {
        customBaseColorArgb ?: style.baseColorArgb
    } else {
        style.baseColorArgb
    }

fun MaskingTapeItem.effectivePatternColorArgb(): Long =
    if (style == MaskingTapeStyle.CUSTOM) {
        customPatternColorArgb ?: style.patternColorArgb
    } else {
        style.patternColorArgb
    }

fun MaskingTapeItem.effectivePatternKind(): MaskingTapePatternKind =
    if (style == MaskingTapeStyle.CUSTOM) {
        customPatternKind ?: style.patternKind
    } else {
        style.patternKind
    }

/**
 * 마스킹테이프 외곽선을 이루는 꼭짓점 목록(테이프 로컬 좌표계, 시계
 * 방향). Compose(MaskingTapeShapes.kt)와 Canvas(PostcardImageExporter.kt)
 * 양쪽이 이 좌표를 그대로 각자의 Path로 옮겨 그리므로 두 렌더 결과가
 * 어긋나지 않는다 — 순수 함수라 JUnit에서 좌표 자체를 검증할 수 있다.
 */
internal fun maskingTapeOutlinePoints(
    width: Float,
    height: Float,
    edgeStyle: MaskingTapeEdgeStyle
): List<Pair<Float, Float>> {
    if (width <= 0f || height <= 0f) return emptyList()

    return when (edgeStyle) {
        MaskingTapeEdgeStyle.STRAIGHT ->
            listOf(
                0f to 0f,
                width to 0f,
                width to height,
                0f to height
            )

        MaskingTapeEdgeStyle.NOTCHED_BOTH -> {
            val notchA = height * MASKING_TAPE_CORNER_NOTCH_RATIO_A
            val notchB = height * MASKING_TAPE_CORNER_NOTCH_RATIO_B
            listOf(
                notchA to 0f,
                width to 0f,
                width to (height - notchB),
                (width - notchB) to height,
                0f to height,
                0f to notchA
            )
        }

        MaskingTapeEdgeStyle.TORN_ONE_SIDE -> {
            val rightEdge = jaggedEdgePoints(baseX = width, height = height, mirrored = false)
            listOf(0f to 0f) + rightEdge + listOf(0f to height)
        }

        MaskingTapeEdgeStyle.JAGGED_BOTH -> {
            val rightEdge = jaggedEdgePoints(baseX = width, height = height, mirrored = false)
            val leftEdge =
                jaggedEdgePoints(baseX = 0f, height = height, mirrored = true).reversed()
            rightEdge + leftEdge
        }
    }
}

/** [MASKING_TAPE_TORN_EDGE_Y_FRACTIONS]/[MASKING_TAPE_TORN_EDGE_X_OFFSET_FRACTIONS] 고정 패턴을 baseX 기준 좌표로 변환한다. */
private fun jaggedEdgePoints(
    baseX: Float,
    height: Float,
    mirrored: Boolean
): List<Pair<Float, Float>> {
    val sign = if (mirrored) -1f else 1f
    return MASKING_TAPE_TORN_EDGE_Y_FRACTIONS.indices.map { i ->
        val yFraction = MASKING_TAPE_TORN_EDGE_Y_FRACTIONS[i]
        val xOffsetFraction = MASKING_TAPE_TORN_EDGE_X_OFFSET_FRACTIONS[i]
        (baseX + sign * xOffsetFraction * height) to (yFraction * height)
    }
}

fun MaskingTapeItem.serialize(): String =
    listOf(
        id,
        style.name,
        offset?.x?.toString() ?: "~",
        offset?.y?.toString() ?: "~",
        scale.toString(),
        rotationDegrees.toString(),
        lengthScale.toString(),
        edgeStyle.name,
        customBaseColorArgb?.toString() ?: "~",
        customPatternColorArgb?.toString() ?: "~",
        customPatternKind?.name ?: "~",
        photoUri?.toString() ?: "~",
        thicknessScale.toString()
    ).joinToString("\t")

/**
 * 7번째(lengthScale)부터는 43일차(길이 조절·복제·커스텀·사진) 이전에
 * 저장된 라인에는 없던 필드고, 13번째(thicknessScale)는 이번(굵기 조절)에
 * 새로 추가됐다. PhotoStickerItem의 rotationDegrees/flip 필드 추가 때와
 * 동일하게 getOrNull + 기본값으로 구버전 라인도 그대로 복원되게 한다 —
 * draft/확정 저장 파일 포맷(버전 표시) 자체는 건드리지 않는다.
 */
fun deserializeMaskingTapeItem(
    line: String
): MaskingTapeItem? {
    val p = line.split("\t")
    if (p.size < 6) return null
    return runCatching {
        val ox = p[2].takeIf { it != "~" }?.toFloat()
        val oy = p[3].takeIf { it != "~" }?.toFloat()

        // 인식하지 못하는 MaskingTapeStyle이면(향후 enum 값이 없어지거나
        // 이름이 바뀌면) 임의의 다른 디자인으로 조용히 대체하지 않고 이
        // 항목만 버린다 — deserializePostcardSealItem과 동일한 정책.
        val style =
            MaskingTapeStyle.entries
                .firstOrNull { it.name == p[1] }
                ?: return null

        val lengthScale = p.getOrNull(6)?.toFloatOrNull() ?: 1f
        val edgeStyle =
            p.getOrNull(7)
                ?.let { name -> MaskingTapeEdgeStyle.entries.firstOrNull { it.name == name } }
                ?: MaskingTapeEdgeStyle.NOTCHED_BOTH
        val customBaseColorArgb =
            p.getOrNull(8)?.takeIf { it != "~" }?.toLongOrNull()
        val customPatternColorArgb =
            p.getOrNull(9)?.takeIf { it != "~" }?.toLongOrNull()
        val customPatternKind =
            p.getOrNull(10)?.takeIf { it != "~" }
                ?.let { name -> MaskingTapePatternKind.entries.firstOrNull { it.name == name } }
        val photoUri =
            p.getOrNull(11)?.takeIf { it != "~" }?.let { Uri.parse(it) }
        val thicknessScale = p.getOrNull(12)?.toFloatOrNull() ?: 1f

        MaskingTapeItem(
            id = p[0],
            style = style,
            offset = if (ox != null && oy != null) {
                Offset(ox, oy)
            } else {
                null
            },
            scale = p[4].toFloat(),
            rotationDegrees = p[5].toFloat(),
            lengthScale = lengthScale,
            thicknessScale = thicknessScale,
            edgeStyle = edgeStyle,
            customBaseColorArgb = customBaseColorArgb,
            customPatternColorArgb = customPatternColorArgb,
            customPatternKind = customPatternKind,
            photoUri = photoUri
        )
    }.getOrNull()
}
