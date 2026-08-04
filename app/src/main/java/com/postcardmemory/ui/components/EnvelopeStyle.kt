package com.postcardmemory.ui.components

/**
 * 엽서를 넣는 봉투의 디자인. 값과 무관하게 실제 색상·질감은
 * [EnvelopeFrame]에서 그린다. 저장은 이 이름 그대로 Postcard.envelopeStyle에
 * 들어가므로, 이후 항목을 추가하더라도 기존 이름은 바꾸지 않는다.
 */
enum class EnvelopeStyle(
    val label: String,
    val baseColorArgb: Long,
    val flapColorArgb: Long,
    val accentColorArgb: Long,
    val hasAirmailBorder: Boolean = false
) {
    IVORY(
        label = "아이보리 기본 봉투",
        baseColorArgb = 0xFFF7F1E4L,
        flapColorArgb = 0xFFEDE3CEL,
        accentColorArgb = 0xFFC9BFA0L
    ),
    KRAFT(
        label = "크라프트 종이 봉투",
        baseColorArgb = 0xFFCBA876L,
        flapColorArgb = 0xFFB79564L,
        accentColorArgb = 0xFF7C5C34L
    ),
    MINT(
        label = "민트 포인트 봉투",
        baseColorArgb = 0xFFEAF7F0L,
        flapColorArgb = 0xFFD3EEE0L,
        accentColorArgb = 0xFF4FAE85L
    ),
    LAVENDER(
        label = "라벤더 포인트 봉투",
        baseColorArgb = 0xFFF2EEFBL,
        flapColorArgb = 0xFFE1D8F5L,
        accentColorArgb = 0xFF9B7FD9L
    ),
    AIRMAIL(
        label = "에어메일 봉투",
        baseColorArgb = 0xFFFDFBF5L,
        flapColorArgb = 0xFFF0EDE3L,
        accentColorArgb = 0xFF3B5FCCL,
        hasAirmailBorder = true
    );

    companion object {
        /** 저장된 값이 null이거나 인식할 수 없으면 "봉투 없음"으로 안전하게 취급한다. */
        fun fromStoredValue(
            value: String?
        ): EnvelopeStyle? =
            value?.let { stored ->
                entries.firstOrNull { it.name == stored }
            }
    }
}
