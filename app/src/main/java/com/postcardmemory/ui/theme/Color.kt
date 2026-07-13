package com.postcardmemory.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 기본 글자와 외곽선
 */
val BrutalBlack = Color(0xFF1A1324)
val BrutalWhite = Color(0xFFFFFBFF)

/*
 * 포스트카드에 사용하는 파스텔 색상
 */
val BrutalMint = Color(0xFF8FFFDA)
val BrutalCoral = Color(0xFFFF7F8E)
val BrutalYellow = Color(0xFFFFE566)
val BrutalPink = Color(0xFFFFB3E1)
val BrutalBlue = Color(0xFFAAD4FF)

/*
 * 앱 전체 흑백·회색조 테마
 * (선택/강조 상태와 보조 배경)
 */
val NeutralLight = Color(0xFFCFCED6)
val GraphiteAccent = Color(0xFF57545C)

/*
 * 화면 배경과 카드 표면
 */
val ScreenBackgroundGray = Color(0xFFEDEBF2)
val SurfaceGray = Color(0xFFE0DFE5)
val SoftGray = Color(0xFFF5F4F7)

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