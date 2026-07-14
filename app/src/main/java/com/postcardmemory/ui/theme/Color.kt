package com.postcardmemory.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 기본 글자와 외곽선
 *
 * BrutalWhite는 카드·칩 표면과 루트 바탕에 쓰는 종이 화이트라
 * 순백 대신 아주 옅은 웜 페이퍼 톤을 머금는다.
 */
val BrutalBlack = Color(0xFF1A1324)
val BrutalWhite = Color(0xFFFFFBF3)

/*
 * 포스트카드에 사용하는 파스텔 색상
 */
val BrutalMint = Color(0xFF8FFFDA)
val BrutalCoral = Color(0xFFFF7F8E)
val BrutalYellow = Color(0xFFFFE566)
val BrutalPink = Color(0xFFFFB3E1)
val BrutalBlue = Color(0xFFAAD4FF)

/*
 * 앱 전체 중성 톤 (선택/강조 상태와 보조 표면)
 *
 * 원래 냉회색이었으나 갤러리의 종이 감성과 한 세계관으로 묶기 위해
 * 해질녘 노을빛이 밴 웜 페이퍼 계열로 조정했다. (val 이름은 호환을 위해 유지)
 *
 * NeutralLight  : 편집 패널 트레이·슬라이더 트랙 등 배경 위 웜 베이지 표면
 * GraphiteAccent: 선택 채움과 보조 텍스트·비활성 아이콘에 쓰는 따뜻한 잉크 회갈색
 */
val NeutralLight = Color(0xFFE8DCC6)
val GraphiteAccent = Color(0xFF5B5046)

/*
 * 화면 배경과 카드 표면 (웜 페이퍼 램프: 배경 < 표면 < 종이 화이트)
 *
 * ScreenBackgroundGray: 편지를 펼쳐두는 따뜻한 작업대 크림 배경
 * SurfaceGray         : 회색 대신 옅은 로즈 베이지 경계선·디바이더
 * SoftGray            : 칩·입력창에 쓰는 웜 오프화이트
 */
val ScreenBackgroundGray = Color(0xFFF4ECDE)
val SurfaceGray = Color(0xFFDBCBB0)
val SoftGray = Color(0xFFFAF4E9)

/*
 * 갤러리 크롬 전용 색상
 *
 * GalleryPaperWhite: 우표를 올려두는 따뜻한 종이 배경.
 * GalleryDangerRed: 삭제 전용 색. 코랄은 카메라 등
 * 주요 행동에 이미 쓰이므로 위험 행동과 구분하기 위해 별도로 둔다.
 */
val GalleryPaperWhite = Color(0xFFFAF3E8)
val GalleryDangerRed = Color(0xFFC0392B)

/*
 * 포스트카드 배경색 목록
 *
 * 앱 전체가 흑백·회색조 테마이므로
 * 카드 색상 목록에서는 무채색을 제외한다.
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