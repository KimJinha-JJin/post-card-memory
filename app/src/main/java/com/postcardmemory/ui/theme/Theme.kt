package com.postcardmemory.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrutalViolet,
    onPrimary = BrutalWhite,

    primaryContainer = BrutalLavender,
    onPrimaryContainer = BrutalDeepViolet,

    secondary = LavenderSurface,
    onSecondary = BrutalDeepViolet,

    secondaryContainer = LavenderSoft,
    onSecondaryContainer = BrutalDeepViolet,

    tertiary = BrutalCoral,
    onTertiary = BrutalBlack,

    background = LavenderBackground,
    onBackground = BrutalBlack,

    surface = LavenderSoft,
    onSurface = BrutalBlack,

    surfaceVariant = LavenderSurface,
    onSurfaceVariant = BrutalDeepViolet,

    error = BrutalCoral,
    onError = BrutalBlack,

    outline = BrutalDeepViolet
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