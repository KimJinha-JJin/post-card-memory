package com.postcardmemory.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = BrutalBlack,
    onPrimary = BrutalWhite,
    secondary = BrutalMint,
    onSecondary = BrutalBlack,
    tertiary = BrutalCoral,
    background = BrutalWhite,
    onBackground = BrutalBlack,
    surface = BrutalWhite,
    onSurface = BrutalBlack
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
