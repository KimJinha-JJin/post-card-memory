package com.postcardmemory.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = GraphiteAccent,
    onPrimary = BrutalWhite,

    primaryContainer = NeutralLight,
    onPrimaryContainer = BrutalBlack,

    secondary = SurfaceGray,
    onSecondary = BrutalBlack,

    secondaryContainer = SoftGray,
    onSecondaryContainer = BrutalBlack,

    tertiary = BrutalCoral,
    onTertiary = BrutalBlack,

    background = PaperCanvas,
    onBackground = InkPrimary,

    surface = PaperSurface,
    onSurface = InkPrimary,

    surfaceVariant = SurfaceGray,
    onSurfaceVariant = BrutalBlack,

    // 알림창(Snackbar) 등 inverse 색을 쓰는 컴포넌트. 비워 두면 Material 기본 보라색이
    // 나온다(101일차 실기기 QA). 앱에서는 진한 반전 대신 종이 알림으로 쓴다.
    inverseSurface = PaperSurface,
    inverseOnSurface = InkPrimary,
    inversePrimary = SunsetGold,

    error = BrutalCoral,
    onError = BrutalBlack,

    outline = BrutalBlack
)

@Composable
fun PostCardMemoryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}