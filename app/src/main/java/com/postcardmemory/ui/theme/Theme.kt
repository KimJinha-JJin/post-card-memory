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

    background = ScreenBackgroundGray,
    onBackground = BrutalBlack,

    surface = SoftGray,
    onSurface = BrutalBlack,

    surfaceVariant = SurfaceGray,
    onSurfaceVariant = BrutalBlack,

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