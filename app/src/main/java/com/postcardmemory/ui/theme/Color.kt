package com.postcardmemory.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * ── 웜 페이퍼 디자인 토큰 (역할 중심) ──
 *
 * 해질녘 노을빛이 밴 오래된 종이 세계관. 갤러리·상세 등 모든 화면이
 * 이 토큰을 공유한다. 신규/수정 코드는 아래 역할 토큰을 직접 사용한다.
 *
 * 명도 램프: PaperCanvas < PaperTray < PaperField < PaperSurface
 */
val PaperCanvas = Color(0xFFF4ECDE)  // 앱 전체 화면 배경 (갤러리·상세 공통)
val PaperTray = Color(0xFFE8DCC6)    // 편집 트레이·슬라이더 트랙·보조 표면
val PaperSurface = Color(0xFFFFFBF3) // 카드·칩·다이얼로그 표면 (종이 화이트)
val PaperField = Color(0xFFFAF4E9)   // 입력창·옵션 칩의 옅은 표면
val PaperDivider = Color(0xFFDBCBB0)  // 경계선·구분선 전용
val InkPrimary = Color(0xFF1A1324)   // 본문·주요 아이콘
val InkSecondary = Color(0xFF5B5046) // 보조 텍스트·비활성·선택 채움
val SunsetGold = Color(0xFF8C5F00)   // 선택 상태·활성 강조 (편집 UI)
val SunsetCoral = Color(0xFFFF7F8E)  // 콘텐츠 팔레트·장식 포인트
val DangerRed = Color(0xFFC0392B)    // 삭제·위험 행동

/*
 * 포스트카드 콘텐츠(엽서 배경)용 파스텔 — UI 토큰과 무관한 콘텐츠 색
 */
val BrutalMint = Color(0xFF8FFFDA)
val BrutalYellow = Color(0xFFFFE566)
val BrutalPink = Color(0xFFFFB3E1)
val BrutalBlue = Color(0xFFAAD4FF)

/*
 * 레거시 이름 호환 별칭
 *
 * 기존 참조가 그대로 컴파일되도록 역할 토큰에 연결한다.
 * (예전 냉회색 이름이 남아 있지만 값은 전부 위의 웜 토큰을 가리킨다)
 */
val BrutalBlack = InkPrimary
val BrutalWhite = PaperSurface
val NeutralLight = PaperTray
val GraphiteAccent = InkSecondary
val ScreenBackgroundGray = PaperCanvas
val SurfaceGray = PaperDivider
val SoftGray = PaperField
val GalleryPaperWhite = PaperCanvas
val BrutalCoral = SunsetCoral
val GalleryDangerRed = DangerRed

/*
 * 포스트카드 배경색 목록 (엽서 콘텐츠 전용)
 */
val pastelColors = listOf(
    BrutalMint,
    BrutalCoral,
    BrutalYellow,
    BrutalPink,
    BrutalBlue
)

/*
 * 도장(우편 소인) 잉크 색상
 *
 * UI 색상과 무관한 포스트카드 콘텐츠 전용 색이며
 * 실제 우편 잉크처럼 제한된 색만 제공한다.
 */
val SealInkBlack = Color(0xFF252525)
val SealInkRed = Color(0xFFB33A32)
val SealInkNavy = Color(0xFF30415F)
val SealInkSepia = Color(0xFF704E38)
val SealInkGreen = Color(0xFF365C4A)
val SealInkWhite = Color(0xFFFFFFFF)

val sealInkColors = listOf(
    SealInkBlack,
    SealInkRed,
    SealInkNavy,
    SealInkSepia,
    SealInkGreen,
    SealInkWhite
)

/*
 * 도장 색상 선택지 — 도장은 채움 없는 얇은 선(Stroke)으로만 그려져
 * 밝은 배경 위에서 흰 잉크가 사실상 안 보이므로 textStickerColors와
 * 같은 이유로 신규 선택지에서 흰색을 제외한다. sealInkColors 자체는
 * 그대로 두어 이미 저장된 흰 잉크 도장은 계속 SealInkWhite로 렌더된다.
 */
val sealSelectableInkColors = sealInkColors.filterNot { it == SealInkWhite }

/*
 * 텍스트 스티커 글자색 — 흰색 외곽선과 항상 대비돼야 하므로 흰색은
 * 제외한다. 콘텐츠 팔레트(pastelColors)와 잉크 톤을 섞어 "귀엽고
 * 사랑스러운" 느낌에 맞춘다.
 */
val textStickerColors = listOf(
    InkPrimary,
    SunsetCoral,
    SunsetGold,
    BrutalBlue,
    BrutalMint,
    BrutalPink
)

/*
 * 텍스트 스티커 테두리색 — 젤리롤펜/데코펜으로 한 번 더 두른 느낌의
 * 소수 팔레트. 기본값(흰색)을 항상 첫 번째로 둔다.
 */
val TextStickerOutlineWhite = Color(0xFFFFFFFF)
val TextStickerOutlineLavender = Color(0xFFD9C7F5)

val textStickerOutlineColors = listOf(
    TextStickerOutlineWhite,
    BrutalPink,
    TextStickerOutlineLavender,
    BrutalMint,
    BrutalBlue,
    BrutalYellow
)
