package com.postcardmemory.ui.detail

/**
 * 앱이 기본 제공하는 추천 템플릿 5종. 사용자 템플릿과 달리 파일로 저장되지
 * 않고 코드에 고정되어 있다 — 이름 변경·덮어쓰기·삭제 대상이 아니며
 * (source == TemplateSource.BUILT_IN), 앱 재실행 후에도 항상 동일하게
 * 제공된다. id는 UUID가 아니라 고정 문자열이라 앱 버전이 바뀌어도 같은
 * 템플릿을 가리킨다.
 */
object BuiltInTemplates {

    val all: List<PostcardTemplate> =
        listOf(
            oldLetter,
            summerRecord,
            travelPost,
            blackAndWhiteDiary,
            nightPostcard
        )

    /** 오래된 편지 — 앱의 대표 템플릿: 크림색 배경, 옅은 종이 질감, 작은 중앙 사진, 넓은 여백. */
    private val oldLetter: PostcardTemplate
        get() = PostcardTemplate(
            id = "builtin_old_letter",
            source = TemplateSource.BUILT_IN,
            name = "오래된 편지",
            style = PostcardTemplateStyle(
                layoutStyle = "STAMP",
                backgroundColorArgb = 0xFFF5EBDDL,
                backgroundPattern = "SPECKLE",
                backgroundPatternDensity = 0.8f,
                messageFont = "SERIF",
                dateFormat = "DOT",
                messageTextScale = 1f,
                dateTextScale = 1f,
                photoEdgeBlur = 0f,
                stampPhotoScale = 0.85f,
                stampPhotoOffsetX = 0f,
                stampPhotoOffsetY = 0f,
                stampPhotoZoom = 1f,
                polaroidPhotoScale = 1f,
                polaroidPhotoOffsetX = 0f,
                polaroidPhotoOffsetY = 0f,
                polaroidPhotoZoom = 1f,
                tapedFilmPhotoOffsetX = 0f,
                tapedFilmPhotoOffsetY = 0f,
                tapedFilmPhotoZoom = 1f
            ),
            seal = null,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )

    /** 여름날의 기록 — 민트빛 배경, 사진을 크게, 가벼운 테이프 필름 분위기. */
    private val summerRecord: PostcardTemplate
        get() = PostcardTemplate(
            id = "builtin_summer_record",
            source = TemplateSource.BUILT_IN,
            name = "여름날의 기록",
            style = PostcardTemplateStyle(
                layoutStyle = "TAPED_FILM",
                backgroundColorArgb = 0xFFDCF1EFL,
                backgroundPattern = "WAVES",
                backgroundPatternDensity = 1.1f,
                messageFont = "SANS_SERIF",
                dateFormat = "ENGLISH_SHORT",
                messageTextScale = 0.95f,
                dateTextScale = 1f,
                photoEdgeBlur = 0f,
                stampPhotoScale = 1f,
                stampPhotoOffsetX = 0f,
                stampPhotoOffsetY = 0f,
                stampPhotoZoom = 1.15f,
                polaroidPhotoScale = 1f,
                polaroidPhotoOffsetX = 0f,
                polaroidPhotoOffsetY = 0f,
                polaroidPhotoZoom = 1f,
                tapedFilmPhotoOffsetX = 0f,
                tapedFilmPhotoOffsetY = 0f,
                tapedFilmPhotoZoom = 1.15f
            ),
            seal = null,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )

    /** 여행 우편 — 폴라로이드 레이아웃, 베이지 배경, 사진 아래 문구 공간, 우편 소인 도장. */
    private val travelPost: PostcardTemplate
        get() = PostcardTemplate(
            id = "builtin_travel_post",
            source = TemplateSource.BUILT_IN,
            name = "여행 우편",
            style = PostcardTemplateStyle(
                layoutStyle = "POLAROID",
                backgroundColorArgb = 0xFFEFE6D6L,
                backgroundPattern = "NONE",
                backgroundPatternDensity = 1f,
                messageFont = "SERIF",
                dateFormat = "DOT",
                messageTextScale = 1f,
                dateTextScale = 1.1f,
                photoEdgeBlur = 0f,
                stampPhotoScale = 1f,
                stampPhotoOffsetX = 0f,
                stampPhotoOffsetY = 0f,
                stampPhotoZoom = 1f,
                polaroidPhotoScale = 0.9f,
                polaroidPhotoOffsetX = 0f,
                polaroidPhotoOffsetY = 0f,
                polaroidPhotoZoom = 1f,
                tapedFilmPhotoOffsetX = 0f,
                tapedFilmPhotoOffsetY = 0f,
                tapedFilmPhotoZoom = 1f
            ),
            seal = PostcardTemplateSeal(
                type = SealType.CIRCLE_POSTMARK,
                colorArgb = 0xFF7A3B2EL
            ),
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )

    /** 흑백 일기 — 저채도 회색 배경, 장식 최소화. 사진 필터 기능이 없어 사진 자체는 바꾸지 않는다. */
    private val blackAndWhiteDiary: PostcardTemplate
        get() = PostcardTemplate(
            id = "builtin_bw_diary",
            source = TemplateSource.BUILT_IN,
            name = "흑백 일기",
            style = PostcardTemplateStyle(
                layoutStyle = "STAMP",
                backgroundColorArgb = 0xFFE6E4E1L,
                backgroundPattern = "NONE",
                backgroundPatternDensity = 1f,
                messageFont = "MONOSPACE",
                dateFormat = "DOT",
                messageTextScale = 0.95f,
                dateTextScale = 0.95f,
                photoEdgeBlur = 0f,
                stampPhotoScale = 1f,
                stampPhotoOffsetX = 0f,
                stampPhotoOffsetY = 0f,
                stampPhotoZoom = 1f,
                polaroidPhotoScale = 1f,
                polaroidPhotoOffsetX = 0f,
                polaroidPhotoOffsetY = 0f,
                polaroidPhotoZoom = 1f,
                tapedFilmPhotoOffsetX = 0f,
                tapedFilmPhotoOffsetY = 0f,
                tapedFilmPhotoZoom = 1f
            ),
            seal = null,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )

    /** 밤의 엽서 — 짙은 남색 배경 + SPECKLE 패턴(어두운 배경에서 자동으로 흰 점이라 별처럼 보임), 작은 사진과 넓은 여백. */
    private val nightPostcard: PostcardTemplate
        get() = PostcardTemplate(
            id = "builtin_night_postcard",
            source = TemplateSource.BUILT_IN,
            name = "밤의 엽서",
            style = PostcardTemplateStyle(
                layoutStyle = "STAMP",
                backgroundColorArgb = 0xFF1B2340L,
                backgroundPattern = "SPECKLE",
                backgroundPatternDensity = 1.3f,
                messageFont = "SERIF",
                dateFormat = "ENGLISH_LONG",
                messageTextScale = 1f,
                dateTextScale = 1f,
                photoEdgeBlur = 0f,
                stampPhotoScale = 0.8f,
                stampPhotoOffsetX = 0f,
                stampPhotoOffsetY = 0f,
                stampPhotoZoom = 1f,
                polaroidPhotoScale = 1f,
                polaroidPhotoOffsetX = 0f,
                polaroidPhotoOffsetY = 0f,
                polaroidPhotoZoom = 1f,
                tapedFilmPhotoOffsetX = 0f,
                tapedFilmPhotoOffsetY = 0f,
                tapedFilmPhotoZoom = 1f
            ),
            seal = null,
            createdAtMillis = 0L,
            updatedAtMillis = 0L
        )
}
