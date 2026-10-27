package com.postcardmemory.ui.detail

import kotlin.random.Random

/**
 * 사진 스티커를 "어떤 종이로, 어떤 손길로 잘라 붙였는지"를 나타내는 오림 스타일.
 * 사진 자체에 필터를 씌우지 않고 가장자리·종이 여백만 바꾼다.
 *
 * 저장값은 [storedName]이다. 스타일 의미는 이 파일에서만 관리하고, 호출부는
 * 문자열을 직접 비교하지 않는다.
 */
enum class PhotoStickerEdgeStyle(
    val storedName: String,
    val label: String
) {
    /** 기존 사진 스티커 모습 그대로(둥근 모서리 사각형). 새 디자인이 아니다. */
    DEFAULT("DEFAULT", "기본"),
    POLAROID("POLAROID", "폴라로이드"),
    SCISSOR("SCISSOR", "가위 오림"),
    TORN("TORN", "찢은 종이"),
    MAGAZINE("MAGAZINE", "잡지 오림");

    companion object {
        /**
         * 필드가 없는 옛 데이터(null)와 알 수 없는 문자열은 예외 없이 DEFAULT로
         * 읽는다 — valueOf()처럼 던지면 deserializePhotoStickerItem의
         * runCatching 전체가 실패해 스티커가 통째로 사라진다.
         */
        fun fromStored(value: String?): PhotoStickerEdgeStyle =
            entries.firstOrNull { it.storedName == value } ?: DEFAULT
    }
}

/** 스티커 한 변을 1로 둔 정규화 사각형. */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    /**
     * 렌더러는 좌우·상하대칭을 레이어(뒤집힌 좌표계) 전체에 적용한다.
     * 종이는 뒤집지 않고 사진만 뒤집어야 하므로, 뒤집힌 좌표계 안에서
     * 쓸 사진 창 위치를 미리 거울상으로 돌려 둔다.
     */
    fun mirrored(
        flipHorizontal: Boolean,
        flipVertical: Boolean
    ): NormalizedRect =
        NormalizedRect(
            left = if (flipHorizontal) 1f - right else left,
            top = if (flipVertical) 1f - bottom else top,
            right = if (flipHorizontal) 1f - left else right,
            bottom = if (flipVertical) 1f - top else bottom
        )
}

/**
 * 종이 스타일의 canonical geometry. 모든 값은 스티커 한 변 대비 비율이라
 * 크기·회전과 무관하게 같은 모양이 나온다. 편집 미리보기(Compose)와 저장·공유
 * 이미지(android.graphics.Canvas)는 각자 이 값으로 도형을 만들어 그린다.
 */
data class PhotoStickerPaperSpec(
    /** 종이 위에서 사진이 보이는 창(종이 = 스티커 전체 정사각형). */
    val photoWindow: NormalizedRect,
    val paperCornerRadius: Float,
    /** 흰 종이가 밝은 엽서 배경에 묻히지 않게 하는 아주 옅은 가장자리 선. */
    val edgeLineWidth: Float,
    /**
     * 불규칙하게 오린 종이 외곽(시계 방향 닫힌 다각형). null이면 종이는
     * [paperCornerRadius]의 둥근 사각형이다.
     */
    val paperOutline: List<NormalizedPoint>? = null,
    /**
     * 사진 인쇄층이 찢겨 나간 모양(닫힌 다각형, 스티커 정규화 좌표). null이면
     * 사진은 [photoWindow] 사각형 그대로 보인다. 있으면 사진은 [photoWindow]에
     * 맞춰 채운 뒤 이 다각형으로 한 번 더 잘린다.
     */
    val photoClipOutline: List<NormalizedPoint>? = null,
    val paperArgb: Long = PHOTO_STICKER_PAPER_ARGB,
    /** 인쇄 망점 질감. null이면 없음. */
    val halftone: PhotoStickerHalftoneSpec? = null,
    /**
     * 인쇄된 종이를 칼로 잘랐을 때 외곽 안쪽에 드러나는 흰 종이 단면 폭.
     * 0이면 없음. 종이 외곽이 있을 때만 쓴다.
     */
    val cutCoreWidth: Float = 0f
)

/**
 * 잡지 오림의 인쇄 망점. 사진에 필터를 씌우는 게 아니라 "인쇄된 종이"를
 * 느끼게 하는 아주 낮은 강도의 질감이다. 여백에는 옆 페이지 인쇄 잔흔 같은
 * 컬러 망점, 사진 위에는 거의 보이지 않는 흑색 망점만 얹는다.
 */
data class PhotoStickerHalftoneSpec(
    /** 망점 간격(스티커 한 변 대비). */
    val pitch: Float,
    val marginInkArgb: Long,
    /** 여백 전체에 깔리는 옅은 인쇄색 바탕. */
    val marginTintAlpha: Float,
    val marginAlpha: Float,
    val marginAngleDegrees: Float,
    val photoInkArgb: Long,
    val photoAlpha: Float,
    val photoAngleDegrees: Float
)

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

/** [NormalizedRect.mirrored]와 같은 이유로, 뒤집힌 좌표계에서 쓸 거울상 다각형. */
fun List<NormalizedPoint>.mirrored(
    flipHorizontal: Boolean,
    flipVertical: Boolean
): List<NormalizedPoint> =
    if (!flipHorizontal && !flipVertical) {
        this
    } else {
        map {
            NormalizedPoint(
                x = if (flipHorizontal) 1f - it.x else it.x,
                y = if (flipVertical) 1f - it.y else it.y
            )
        }
    }

const val PHOTO_STICKER_PAPER_ARGB: Long = 0xFFFDFBF6L
const val PHOTO_STICKER_PAPER_EDGE_ARGB: Long = 0x24000000L
/** 인쇄된 종이의 칼 단면(속지) 색. */
const val PHOTO_STICKER_CUT_CORE_ARGB: Long = 0xFFFDFBF6L

/** 상·좌·우는 얇게, 하단만 조금 넓게. 자동 문구·날짜 없음. */
private val POLAROID_PAPER_SPEC =
    PhotoStickerPaperSpec(
        photoWindow = NormalizedRect(
            left = 0.045f,
            top = 0.045f,
            right = 1f - 0.045f,
            bottom = 1f - 0.13f
        ),
        paperCornerRadius = 0.012f,
        edgeLineWidth = 0.006f
    )

/*
 * 가위 오림: 사진 둘레에 얇은 흰 여백을 남기고, 네 변을 각각 몇 번의 곧은
 * 가위질로 잘라낸 모양. 꺾임은 작고 드물게 — 톱니·지그재그·구름 테두리가
 * 되지 않도록 한 변에 꺾임은 최대 2개, 변에서 벗어나는 폭은 한 변의 1% 미만.
 * 외곽은 [0,1] 안에 머물러 스티커 박스(미리보기 Box·저장 bounds) 밖으로 나가지 않는다.
 */
internal const val SCISSOR_OUTLINE_BASE_INSET = 0.016f
internal const val SCISSOR_CORNER_JITTER = 0.010f
internal const val SCISSOR_SIDE_DEVIATION = 0.009f
internal const val SCISSOR_MAX_BREAKS_PER_SIDE = 2
private const val SCISSOR_PHOTO_INSET = 0.068f

private val SCISSOR_PHOTO_WINDOW =
    NormalizedRect(
        left = SCISSOR_PHOTO_INSET,
        top = SCISSOR_PHOTO_INSET,
        right = 1f - SCISSOR_PHOTO_INSET,
        bottom = 1f - SCISSOR_PHOTO_INSET
    )

/**
 * DEFAULT는 null — 호출부는 기존 렌더링 경로를 그대로 탄다.
 * 불규칙 외곽은 [edgeSeed]만으로 결정되므로 이동·확대·회전·재구성·재실행·
 * 복제와 무관하게 같은 스티커는 늘 같은 모양이다. 호출부는 결과를
 * remember(style, seed)로 재사용해 제스처 중 다시 계산하지 않는다.
 */
fun photoStickerPaperSpec(
    style: PhotoStickerEdgeStyle,
    edgeSeed: Long
): PhotoStickerPaperSpec? =
    when (style) {
        PhotoStickerEdgeStyle.DEFAULT -> null
        PhotoStickerEdgeStyle.POLAROID -> POLAROID_PAPER_SPEC
        PhotoStickerEdgeStyle.SCISSOR ->
            PhotoStickerPaperSpec(
                photoWindow = SCISSOR_PHOTO_WINDOW,
                paperCornerRadius = 0f,
                edgeLineWidth = 0.006f,
                paperOutline = scissorPaperOutline(edgeSeed)
            )
        PhotoStickerEdgeStyle.TORN -> tornPaperSpec(edgeSeed)
        PhotoStickerEdgeStyle.MAGAZINE -> magazinePaperSpec(edgeSeed)
    }

internal fun scissorPaperOutline(
    edgeSeed: Long
): List<NormalizedPoint> =
    straightCutOutline(
        random = StableEdgeRandom(edgeSeed),
        baseInset = SCISSOR_OUTLINE_BASE_INSET,
        cornerJitter = SCISSOR_CORNER_JITTER,
        sideDeviation = SCISSOR_SIDE_DEVIATION,
        maxBreaksPerSide = SCISSOR_MAX_BREAKS_PER_SIDE
    )

/**
 * 네 변을 몇 번의 곧은 칼·가위질로 잘라낸 외곽. 난수를 뽑는 순서까지 저장된
 * 스티커 모양의 일부이므로 바꾸지 않는다(golden 테스트로 고정).
 */
private fun straightCutOutline(
    random: StableEdgeRandom,
    baseInset: Float,
    cornerJitter: Float,
    sideDeviation: Float,
    maxBreaksPerSide: Int
): List<NormalizedPoint> {
    val base = baseInset

    fun clampOutline(value: Float): Float =
        value.coerceIn(0.002f, 0.998f)

    fun jitteredCorner(x: Float, y: Float): NormalizedPoint =
        NormalizedPoint(
            x = clampOutline(
                x + random.nextFloat(-cornerJitter, cornerJitter)
            ),
            y = clampOutline(
                y + random.nextFloat(-cornerJitter, cornerJitter)
            )
        )

    val corners =
        listOf(
            jitteredCorner(base, base),
            jitteredCorner(1f - base, base),
            jitteredCorner(1f - base, 1f - base),
            jitteredCorner(base, 1f - base)
        )

    val outline = mutableListOf<NormalizedPoint>()
    corners.forEachIndexed { index, start ->
        val end = corners[(index + 1) % corners.size]
        // 윗변·아랫변은 y로, 왼변·오른변은 x로만 살짝 벗어난다.
        val horizontalSide = index % 2 == 0
        val breaks =
            1 + (random.nextFloat(0f, 1f) * maxBreaksPerSide).toInt()
                .coerceAtMost(maxBreaksPerSide - 1)

        outline += start
        for (k in 1..breaks) {
            val slot = 1f / (breaks + 1)
            val t = slot * k + random.nextFloat(-0.2f, 0.2f) * slot
            val deviation =
                random.nextFloat(-sideDeviation, sideDeviation)
            val x = start.x + (end.x - start.x) * t
            val y = start.y + (end.y - start.y) * t
            outline +=
                if (horizontalSide) {
                    NormalizedPoint(x, clampOutline(y + deviation))
                } else {
                    NormalizedPoint(clampOutline(x + deviation), y)
                }
        }
    }
    return outline
}

/*
 * 찢은 종이: 네 변 모두 손으로 찢은 모양. 종이 외곽이 완만하게 일렁이고,
 * 사진 인쇄층은 그보다 안쪽에서 얕게 찢겨 그 사이로 흰 종이 단면이 불규칙한
 * 띠로 드러난다. 변마다 일렁임의 주기·위상·잔결을 seed로 따로 정해 스티커마다
 * 모양이 다르다. 굴곡은 낮은 주파수 사인 둘 + 아주 작은 잔결이라 뾰족한
 * 돌기가 생기지 않는다.
 */
internal enum class TornSide { TOP, RIGHT, BOTTOM, LEFT }

internal const val TORN_POINTS_PER_SIDE = 28
internal const val TORN_PAPER_BASE_INSET = 0.018f
internal const val TORN_PHOTO_BASE_INSET = 0.056f
/** 흰 단면 띠의 최소 폭(종이 끝 ↔ 사진 끝). */
internal const val TORN_MIN_WHITE_BAND = 0.018f

/** 각 변의 스티커 가장자리로부터의 깊이(0..1 위치 t마다). */
internal data class TornEdgeProfile(
    val paperInsets: Map<TornSide, FloatArray>,
    val photoInsets: Map<TornSide, FloatArray>
)

internal fun tornEdgeProfile(
    edgeSeed: Long
): TornEdgeProfile {
    val random = StableEdgeRandom(edgeSeed)

    fun wave(amplitude1: Float, amplitude2: Float): (Float) -> Float {
        val frequency1 = random.nextFloat(1f, 2.2f)
        val frequency2 = random.nextFloat(3f, 5f)
        val phase1 = random.nextFloat(0f, 1f)
        val phase2 = random.nextFloat(0f, 1f)
        return { t ->
            amplitude1 * kotlin.math.sin(2f * kotlin.math.PI.toFloat() * (frequency1 * t + phase1)) +
                amplitude2 * kotlin.math.sin(2f * kotlin.math.PI.toFloat() * (frequency2 * t + phase2))
        }
    }

    val paperInsets = mutableMapOf<TornSide, FloatArray>()
    val photoInsets = mutableMapOf<TornSide, FloatArray>()
    TornSide.entries.forEach { side ->
        val paperWave = wave(0.006f, 0.003f)
        val photoWave = wave(0.006f, 0.0025f)
        val paper = FloatArray(TORN_POINTS_PER_SIDE)
        val photo = FloatArray(TORN_POINTS_PER_SIDE)
        for (i in 0 until TORN_POINTS_PER_SIDE) {
            val t = i / (TORN_POINTS_PER_SIDE - 1).toFloat()
            paper[i] =
                (TORN_PAPER_BASE_INSET + paperWave(t) +
                    random.nextFloat(-0.0025f, 0.0025f))
                    .coerceIn(0.002f, 0.034f)
            photo[i] =
                (TORN_PHOTO_BASE_INSET + 0.010f + photoWave(t) +
                    random.nextFloat(-0.002f, 0.002f))
                    .coerceAtLeast(paper[i] + TORN_MIN_WHITE_BAND)
                    .coerceAtMost(0.09f)
        }
        paperInsets[side] = paper
        photoInsets[side] = photo
    }
    return TornEdgeProfile(paperInsets, photoInsets)
}

/** 네 변의 깊이 배열을 시계 방향 닫힌 다각형으로 잇는다. */
private fun insetPolygon(
    insets: Map<TornSide, FloatArray>
): List<NormalizedPoint> {
    val top = insets.getValue(TornSide.TOP)
    val right = insets.getValue(TornSide.RIGHT)
    val bottom = insets.getValue(TornSide.BOTTOM)
    val left = insets.getValue(TornSide.LEFT)

    fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    fun tOf(i: Int, n: Int) = if (n <= 1) 0f else i / (n - 1).toFloat()

    val points = mutableListOf<NormalizedPoint>()
    // 윗변: 왼→오
    top.forEachIndexed { i, depth ->
        points += NormalizedPoint(lerp(left.first(), 1f - right.first(), tOf(i, top.size)), depth)
    }
    // 오른변: 위→아래
    right.forEachIndexed { i, depth ->
        points += NormalizedPoint(1f - depth, lerp(top.last(), 1f - bottom.last(), tOf(i, right.size)))
    }
    // 아랫변: 오→왼
    bottom.reversed().forEachIndexed { i, depth ->
        points += NormalizedPoint(lerp(1f - right.last(), left.last(), tOf(i, bottom.size)), 1f - depth)
    }
    // 왼변: 아래→위
    left.reversed().forEachIndexed { i, depth ->
        points += NormalizedPoint(depth, lerp(1f - bottom.first(), top.first(), tOf(i, left.size)))
    }
    return points
}

private fun tornPaperSpec(
    edgeSeed: Long
): PhotoStickerPaperSpec {
    val profile = tornEdgeProfile(edgeSeed)
    val photoOutline = insetPolygon(profile.photoInsets)

    return PhotoStickerPaperSpec(
        // 사진은 찢긴 인쇄층 다각형을 감싸는 사각형에 맞춰 채운 뒤 다각형으로 잘린다.
        photoWindow = NormalizedRect(
            left = photoOutline.minOf { it.x },
            top = photoOutline.minOf { it.y },
            right = photoOutline.maxOf { it.x },
            bottom = photoOutline.maxOf { it.y }
        ),
        paperCornerRadius = 0f,
        edgeLineWidth = 0.005f,
        paperOutline = insetPolygon(profile.paperInsets),
        photoClipOutline = photoOutline
    )
}

/*
 * 잡지 오림: 인쇄된 잡지 페이지에서 사진을 오려낸 모양. 가위 오림(흰 종이 여백)과
 * 달리 사진 둘레에 남는 건 그 페이지에 인쇄돼 있던 색 — 옅은 인쇄색 바탕 위
 * 망점 띠다. 흰색은 칼이 지나간 단면에 아주 얇게만 보인다. 외곽은 거의 곧다.
 * 사진 인쇄 끝은 아주 미세하게 거칠다.
 */
internal const val MAGAZINE_OUTLINE_BASE_INSET = 0.010f
internal const val MAGAZINE_CORNER_JITTER = 0.005f
internal const val MAGAZINE_SIDE_DEVIATION = 0.003f
internal const val MAGAZINE_MAX_BREAKS_PER_SIDE = 2
internal const val MAGAZINE_CUT_CORE_WIDTH = 0.007f
internal const val MAGAZINE_PHOTO_INSET = 0.055f
internal const val MAGAZINE_PRINT_EDGE_POINTS_PER_SIDE = 18
/** 인쇄 끝의 거칠기 — 사진 창 안쪽으로만 이만큼 들쭉날쭉하다. */
internal const val MAGAZINE_PRINT_EDGE_ROUGHNESS = 0.003f
internal const val MAGAZINE_HALFTONE_PITCH = 0.02f
internal const val MAGAZINE_MARGIN_TINT_ALPHA = 0.16f
internal const val MAGAZINE_MARGIN_HALFTONE_ALPHA = 0.5f
internal const val MAGAZINE_PHOTO_HALFTONE_ALPHA = 0.06f

/** 바랜 인쇄 잉크 몇 가지(시안·마젠타·옐로 계열·먹). seed가 하나를 고른다. */
internal val MAGAZINE_MARGIN_INKS: List<Long> =
    listOf(
        0xFF4F87A3L,
        0xFFB25A78L,
        0xFFC29A3EL,
        0xFF5E5A56L
    )

private const val MAGAZINE_PAPER_ARGB: Long = 0xFFF8F6F0L
private const val MAGAZINE_PHOTO_INK_ARGB: Long = 0xFF2E2A27L

private fun magazinePaperSpec(
    edgeSeed: Long
): PhotoStickerPaperSpec {
    val random = StableEdgeRandom(edgeSeed)
    val paperOutline =
        straightCutOutline(
            random = random,
            baseInset = MAGAZINE_OUTLINE_BASE_INSET,
            cornerJitter = MAGAZINE_CORNER_JITTER,
            sideDeviation = MAGAZINE_SIDE_DEVIATION,
            maxBreaksPerSide = MAGAZINE_MAX_BREAKS_PER_SIDE
        )

    val inset = MAGAZINE_PHOTO_INSET
    val printEdge = mutableListOf<NormalizedPoint>()
    val n = MAGAZINE_PRINT_EDGE_POINTS_PER_SIDE
    fun rough() = random.nextFloat(0f, MAGAZINE_PRINT_EDGE_ROUGHNESS)
    for (i in 0 until n) {
        val t = i / n.toFloat()
        printEdge += NormalizedPoint(inset + (1f - 2f * inset) * t, inset + rough())
    }
    for (i in 0 until n) {
        val t = i / n.toFloat()
        printEdge += NormalizedPoint(1f - inset - rough(), inset + (1f - 2f * inset) * t)
    }
    for (i in 0 until n) {
        val t = i / n.toFloat()
        printEdge += NormalizedPoint(1f - inset - (1f - 2f * inset) * t, 1f - inset - rough())
    }
    for (i in 0 until n) {
        val t = i / n.toFloat()
        printEdge += NormalizedPoint(inset + rough(), 1f - inset - (1f - 2f * inset) * t)
    }

    val inkIndex =
        (random.nextFloat(0f, 1f) * MAGAZINE_MARGIN_INKS.size).toInt()
            .coerceIn(0, MAGAZINE_MARGIN_INKS.lastIndex)

    return PhotoStickerPaperSpec(
        photoWindow = NormalizedRect(
            left = inset,
            top = inset,
            right = 1f - inset,
            bottom = 1f - inset
        ),
        paperCornerRadius = 0f,
        edgeLineWidth = 0.005f,
        paperOutline = paperOutline,
        photoClipOutline = printEdge,
        paperArgb = MAGAZINE_PAPER_ARGB,
        halftone = PhotoStickerHalftoneSpec(
            pitch = MAGAZINE_HALFTONE_PITCH,
            marginInkArgb = MAGAZINE_MARGIN_INKS[inkIndex],
            marginTintAlpha = MAGAZINE_MARGIN_TINT_ALPHA,
            marginAlpha = MAGAZINE_MARGIN_HALFTONE_ALPHA,
            // 인쇄 판마다 다른 스크린 각도(시안 15°, 먹 45°)
            marginAngleDegrees = 15f,
            photoInkArgb = MAGAZINE_PHOTO_INK_ARGB,
            photoAlpha = MAGAZINE_PHOTO_HALFTONE_ALPHA,
            photoAngleDegrees = 45f
        ),
        cutCoreWidth = MAGAZINE_CUT_CORE_WIDTH
    )
}

/**
 * 저장된 seed로 오림 모양을 재현하는 고정 알고리즘(SplitMix64). kotlin.random의
 * 내부 구현이 바뀌더라도 이미 저장된 스티커의 오림 모양이 달라지지 않도록
 * 직접 둔다 — 이 계산을 바꾸면 기존 스티커의 모양이 바뀐다.
 */
private class StableEdgeRandom(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** [min, max) 균등 분포. */
    fun nextFloat(min: Float, max: Float): Float {
        val unit = (nextLong() ushr 40).toFloat() / (1L shl 24).toFloat()
        return min + (max - min) * unit
    }
}

/**
 * 실제로 그릴 스타일. 누끼 상태에서는 저장된 스타일을 지우지 않고 렌더링에서만
 * 오림을 쉬게 한다 — 누끼를 끄면 저장된 스타일이 그대로 돌아온다.
 */
fun PhotoStickerItem.renderedEdgeStyle(): PhotoStickerEdgeStyle =
    if (isBackgroundRemoved) PhotoStickerEdgeStyle.DEFAULT else edgeStyle

/** 필드가 없거나 숫자가 아니면 예외 없이 0L(아직 정하지 않음). */
fun parsePhotoStickerEdgeSeed(value: String?): Long =
    value?.toLongOrNull() ?: 0L

/** 0L은 "아직 정하지 않음"이라 새 seed로는 쓰지 않는다. */
fun newPhotoStickerEdgeSeed(
    random: Random = Random.Default
): Long {
    var seed: Long
    do {
        seed = random.nextLong()
    } while (seed == 0L)
    return seed
}

/**
 * [stickerId] 스티커의 오림 스타일을 바꾼 새 목록. 바뀔 게 없으면(대상 없음,
 * 같은 스타일, 누끼 상태) null. seed는 처음 스타일을 적용할 때 한 번만 정하고
 * 이후 절대 다시 만들지 않는다.
 */
fun List<PhotoStickerItem>.withStickerEdgeStyle(
    stickerId: String,
    style: PhotoStickerEdgeStyle,
    newSeed: () -> Long = ::newPhotoStickerEdgeSeed
): List<PhotoStickerItem>? {
    val target = find { it.id == stickerId } ?: return null
    if (target.isBackgroundRemoved || target.edgeStyle == style) {
        return null
    }
    val seed =
        if (target.edgeSeed != 0L) target.edgeSeed else newSeed()

    return map {
        if (it.id == stickerId) {
            it.copy(edgeStyle = style, edgeSeed = seed)
        } else {
            it
        }
    }
}
