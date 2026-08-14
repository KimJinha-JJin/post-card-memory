package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import java.util.UUID

/**
 * 라벨 문구의 최대 길이. 라벨프린터로 뽑는 짧은 한 조각이라는 성격을
 * 유지하기 위한 제한이며, TEXT_STICKER_MAX_LENGTH(50)보다 의도적으로
 * 훨씬 짧다 — 라벨은 문장을 적는 물건이 아니다.
 *
 * 값이 14인 이유는 한글 폭 때문이다. 미리보기 엽서는 화면 너비의 0.8배
 * (clip됨)라 360dp 기기 기준 약 288dp인데, 전각인 한글은 한 자당
 * 글자크기 x 1.14(자간 포함)를 먹는다. 기본 크기 15sp에서 14자면
 * 약 258dp라 여유 있게 들어가지만, 20자면 약 360dp가 되어 오른쪽 끝이
 * 잘린다. 영문 기준으로는 14자도 "SUMMER 2026"·"MY FAVORITE" 같은
 * 실제 사용 예시를 모두 담고 남는다.
 */
const val LABEL_STICKER_MAX_LENGTH = 14

/**
 * 라벨 테이프 렌더 규칙. 전부 "글자 크기(px) 대비 비율"이라 미리보기
 * (화면 density)와 저장 이미지(2048px 캔버스) 어느 해상도에서도 같은
 * 모양·같은 폭이 나온다. 실제 그리기는 두 경로가 공통으로 호출하는
 * utils/LabelStickerRenderer.kt 하나뿐이라 계산식이 갈라지지 않는다.
 */
/** 테이프 세로 두께. 글자 종류(영문/한글)와 무관하게 고정 — 실제 라벨테이프도 폭이 기계로 정해져 있다. */
const val LABEL_STICKER_HEIGHT_RATIO = 1.85f

/** 문자열 좌우에 항상 남는 여백. */
const val LABEL_STICKER_SIDE_PADDING_RATIO = 0.62f

/** "A" 한 글자짜리 라벨도 정사각형 칩이 아니라 짧은 테이프 조각으로 보이게 하는 최소 폭. */
const val LABEL_STICKER_MIN_WIDTH_RATIO = 2.4f

/** 거의 각진 끝. pill/chip처럼 보이지 않을 만큼만 둥글린다. */
const val LABEL_STICKER_CORNER_RADIUS_RATIO = 0.10f

/** 기계식 라벨 특유의 벌어진 자간(em 단위). */
const val LABEL_STICKER_LETTER_SPACING_EM = 0.14f

/** 눌려 올라온 문자 표현에 쓰는 위/아래 렌더 offset. */
const val LABEL_STICKER_EMBOSS_OFFSET_RATIO = 0.036f

/** 잘라낸 테이프 단면처럼 보이게 하는 양 끝 음영대의 폭. */
const val LABEL_STICKER_CUT_BAND_RATIO = 0.10f

/** 표면 하이라이트 선이 놓이는 세로 위치(테이프 높이 대비). */
const val LABEL_STICKER_SHEEN_Y_RATIO = 0.2f

/** 눌린 문자의 밝은 면·반대쪽 음영. 테이프 색과 무관한 고정값이라 모든 테이프가 같은 물성으로 보인다. */
const val LABEL_STICKER_EMBOSS_HIGHLIGHT_ARGB = 0x73FFFFFFL
const val LABEL_STICKER_EMBOSS_SHADOW_ARGB = 0x59000000L

/**
 * 기타 색상 테이프에서 자동으로 고르는 문자색 두 가지. 프리셋이 쓰는
 * 밝은/어두운 문자색과 같은 톤이라, 프리셋과 커스텀 라벨이 나란히
 * 있어도 문자 느낌이 튀지 않는다.
 */
const val LABEL_STICKER_LIGHT_TEXT_ARGB = 0xFFF3F1EAL
const val LABEL_STICKER_DARK_TEXT_ARGB = 0xFF2B2825L

/**
 * 이 값보다 밝은 테이프에는 어두운 문자를, 어두운 테이프에는 밝은 문자를
 * 올린다. 6종 프리셋을 이 식에 넣으면 전부 프리셋에 손으로 지정해 둔
 * 문자색과 같은 쪽이 나오도록 맞춘 경계값이다(LabelStickerItemTest에서 고정).
 */
const val LABEL_STICKER_LIGHT_TAPE_LUMINANCE_THRESHOLD = 0.6f

/** 기타 색상 테이프의 잘린 단면 음영을 바탕색에서 만들 때 쓰는 어둡게 하는 비율. */
private const val LABEL_STICKER_CUSTOM_EDGE_DARKEN_FACTOR = 0.55f

/**
 * 라벨프린터에 넣는 플라스틱 테이프 한 종류. 사용자가 글자색을 따로
 * 고르는 UI는 만들지 않고(작업지시서 12절) 테이프마다 문자색을 여기
 * 고정해 둔다 — 실제로도 테이프 색이 정해지면 눌려 나오는 글자색은
 * 함께 정해진다.
 *
 * edgeColorArgb는 잘린 단면 음영에 쓰는, 바탕보다 어두운 짝색이다.
 *
 * CUSTOM은 프리셋 그리드에 노출하지 않는 마커 엔트리다
 * ([presetLabelTapeStyles] 참고). 실제 색은
 * [LabelStickerItem.customTapeColorArgb]로 대체되고, 여기 적힌 값은 그
 * 필드가 비어 있을 때만 쓰는 안전한 폴백이다 — MaskingTapeStyle.CUSTOM과
 * 같은 방식.
 */
enum class LabelTapeStyle(
    val label: String,
    val baseColorArgb: Long,
    val edgeColorArgb: Long,
    val textColorArgb: Long
) {
    BLACK("블랙", 0xFF262626L, 0xFF0D0D0DL, 0xFFF4F2EDL),
    RED("레드", 0xFFA8362BL, 0xFF6C1E16L, 0xFFF9EFEBL),
    BLUE("블루", 0xFF21507CL, 0xFF12304EL, 0xFFEFF4F9L),
    GREEN("그린", 0xFF2C6B4EL, 0xFF17442FL, 0xFFEFF6F1L),
    YELLOW("옐로", 0xFFE3BE33L, 0xFF9A7A10L, 0xFF33290BL),
    WHITE("화이트", 0xFFF1EDE2L, 0xFFB9B2A0L, 0xFF2B2825L),
    CUSTOM("기타", 0xFFB9B2A0L, 0xFF6B675CL, 0xFF2B2825L)
}

/** 테이프 선택 줄에 늘어놓을 프리셋들. 기타 색상은 별도 진입점으로만 만든다. */
val presetLabelTapeStyles: List<LabelTapeStyle> =
    LabelTapeStyle.entries.filterNot { it == LabelTapeStyle.CUSTOM }

/**
 * 실제로 그릴 때 쓰는 테이프 색 3종. 프리셋이면 enum에 손으로 지정해 둔
 * 값을 그대로 돌려주므로(기타 색상 도입 때문에 기존 프리셋 외형이 바뀌지
 * 않는다), 자동 계산은 CUSTOM에서만 일어난다.
 */
data class LabelTapePalette(
    val baseColorArgb: Long,
    val edgeColorArgb: Long,
    val textColorArgb: Long
)

/**
 * 미리보기와 exporter가 공유하는 유일한 색 결정 지점. 화면 쪽과 저장
 * 이미지 쪽이 각자 색을 고르지 않도록, 두 경로 모두 이 함수 하나만 부른다.
 */
fun labelTapePalette(
    style: LabelTapeStyle,
    customTapeColorArgb: Long?
): LabelTapePalette {
    if (style != LabelTapeStyle.CUSTOM) {
        return LabelTapePalette(
            baseColorArgb = style.baseColorArgb,
            edgeColorArgb = style.edgeColorArgb,
            textColorArgb = style.textColorArgb
        )
    }

    val baseColorArgb = customTapeColorArgb ?: style.baseColorArgb

    return LabelTapePalette(
        baseColorArgb = baseColorArgb,
        edgeColorArgb =
            scaleLabelTapeRgb(
                baseColorArgb,
                LABEL_STICKER_CUSTOM_EDGE_DARKEN_FACTOR
            ),
        textColorArgb = labelStickerTextColorArgbFor(baseColorArgb)
    )
}

/** 테이프가 밝으면 어두운 문자를, 어두우면 밝은 문자를 돌려준다. */
fun labelStickerTextColorArgbFor(tapeColorArgb: Long): Long =
    if (
        labelStickerLuminance(tapeColorArgb) >
        LABEL_STICKER_LIGHT_TAPE_LUMINANCE_THRESHOLD
    ) {
        LABEL_STICKER_DARK_TEXT_ARGB
    } else {
        LABEL_STICKER_LIGHT_TEXT_ARGB
    }

/**
 * 사람 눈이 느끼는 밝기(0~1). 색 시스템을 새로 만들지 않고 문자 대비
 * 판단에만 쓰는 최소 계산이라, 표준 sRGB 가중치만 적용한다.
 */
fun labelStickerLuminance(colorArgb: Long): Float {
    val c = colorArgb.toInt()
    val r = (c ushr 16) and 0xFF
    val g = (c ushr 8) and 0xFF
    val b = c and 0xFF
    return (0.299f * r + 0.587f * g + 0.114f * b) / 255f
}

/** alpha는 유지한 채 RGB만 밝기 조절한다. android.graphics.Color 정적 호출 없이 순수 비트 연산. */
internal fun scaleLabelTapeRgb(colorArgb: Long, factor: Float): Long {
    val c = colorArgb.toInt()
    val a = (c ushr 24) and 0xFF
    val r = (((c ushr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
    val g = (((c ushr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
    val b = ((c and 0xFF) * factor).toInt().coerceIn(0, 255)
    return (
        (a.toLong() shl 24) or
            (r.toLong() shl 16) or
            (g.toLong() shl 8) or
            b.toLong()
        )
}

/**
 * 라벨프린터로 뽑아 엽서에 붙이는 테이프 한 조각. 텍스트 스티커와 달리
 * 글자색·테두리색·크기를 따로 갖지 않는다 — 문구와 테이프 종류만 정하면
 * 나머지는 [LabelTapeStyle]과 고정 렌더 규칙으로 전부 결정된다.
 *
 * 폭을 필드로 저장하지 않는 것이 이 모델의 핵심이다. text + fontSizePx만
 * 있으면 화면·재진입·exporter 어디서든 같은 폭이 계산되므로, 저장해 두면
 * 오히려 두 값이 어긋날 여지만 생긴다.
 *
 * scale은 사용자에게 크기 조절 UI를 제공하지 않으므로 항상 1f지만,
 * 다른 꾸미기 오브젝트와 같은 형태를 유지하고 저장 형식을 나중에 바꾸지
 * 않아도 되도록 필드와 직렬화 자리를 남겨 둔다.
 */
data class LabelStickerItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val style: LabelTapeStyle = LabelTapeStyle.BLACK,
    val offset: Offset? = null,
    val scale: Float = 1f,
    val rotationDegrees: Float = 0f,
    /** style == CUSTOM일 때만 쓰는 사용자 지정 테이프 바탕색. 문자색은 여기서 자동으로 파생된다. */
    val customTapeColorArgb: Long? = null
)

/** 이 라벨을 그릴 때 실제로 쓰는 색 3종. 프리셋이면 고정값, 기타 색상이면 바탕색에서 파생된다. */
fun LabelStickerItem.tapePalette(): LabelTapePalette =
    labelTapePalette(
        style = style,
        customTapeColorArgb = customTapeColorArgb
    )

/** 테이프 세로 두께. 문자열과 무관하게 글자 크기만으로 정해진다. */
fun labelStickerHeightPx(fontSizePx: Float): Float =
    (fontSizePx * LABEL_STICKER_HEIGHT_RATIO).coerceAtLeast(1f)

/**
 * 측정된 문자열 폭에 좌우 여백을 더한 테이프 가로 길이. 문자열이 아주
 * 짧아도 최소 폭 아래로는 줄지 않는다. Paint에 의존하지 않는 순수 계산이라
 * JUnit에서 길이별 결과를 그대로 검증할 수 있다.
 */
fun labelStickerWidthPx(
    measuredTextWidthPx: Float,
    fontSizePx: Float
): Float {
    val padded =
        measuredTextWidthPx +
            fontSizePx * LABEL_STICKER_SIDE_PADDING_RATIO * 2f

    return padded
        .coerceAtLeast(fontSizePx * LABEL_STICKER_MIN_WIDTH_RATIO)
        .coerceAtLeast(1f)
}

/**
 * 탭으로 필드를 구분하는 기존 sticker/seal/텍스트 스티커 직렬화 관례를
 * 그대로 따른다. text에는 개행과 탭을 허용하지 않는다(입력 UI에서 막는다).
 */
fun LabelStickerItem.serialize(): String =
    listOf(
        id,
        text,
        style.name,
        offset?.x?.toString() ?: "~",
        offset?.y?.toString() ?: "~",
        scale.toString(),
        rotationDegrees.toString(),
        customTapeColorArgb?.toString() ?: "~"
    ).joinToString("\t")

/**
 * 8번째(customTapeColorArgb)는 기타 색상 도입 전에 저장된 라인에는 없다.
 * MaskingTapeItem의 뒤늦게 붙은 필드들과 동일하게 getOrNull + 기본값으로
 * 구버전 라인도 그대로 복원되게 한다 — 값이 없으면 프리셋 색을 쓰므로
 * 기존 라벨의 외형이 달라지지 않는다.
 */
fun deserializeLabelStickerItem(
    line: String
): LabelStickerItem? {
    val p = line.split("\t")
    if (p.size < 7) return null
    return runCatching {
        // 인식하지 못하는 LabelTapeStyle이면 임의의 다른 테이프로 조용히
        // 대체하지 않고 이 항목만 버린다 — deserializeMaskingTapeItem과 동일한 정책.
        val style =
            LabelTapeStyle.entries
                .firstOrNull { it.name == p[2] }
                ?: return null

        val ox = p[3].takeIf { it != "~" }?.toFloat()
        val oy = p[4].takeIf { it != "~" }?.toFloat()

        LabelStickerItem(
            id = p[0],
            text = p[1],
            style = style,
            offset = if (ox != null && oy != null) {
                Offset(ox, oy)
            } else {
                null
            },
            scale = p[5].toFloat(),
            rotationDegrees = p[6].toFloat(),
            customTapeColorArgb =
                p.getOrNull(7)?.takeIf { it != "~" }?.toLongOrNull()
        )
    }.getOrNull()
}
