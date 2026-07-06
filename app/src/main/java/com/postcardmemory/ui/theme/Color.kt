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