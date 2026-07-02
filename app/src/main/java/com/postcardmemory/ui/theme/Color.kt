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
 * 앱 전체 라벤더·바이올렛 테마
 */
val BrutalLavender = Color(0xFFD4B8FF)
val BrutalViolet = Color(0xFF8055C7)
val BrutalDeepViolet = Color(0xFF432665)

/*
 * 화면 배경과 카드 표면
 */
val LavenderBackground = Color(0xFFF1E8FF)
val LavenderSurface = Color(0xFFE3D1FF)
val LavenderSoft = Color(0xFFFAF6FF)

/*
 * 포스트카드 배경색 목록
 *
 * 앱 전체가 라벤더 테마이므로
 * 카드 색상 목록에서는 라벤더를 제외한다.
 */
val pastelColors = listOf(
    BrutalMint,
    BrutalCoral,
    BrutalYellow,
    BrutalPink,
    BrutalBlue
)