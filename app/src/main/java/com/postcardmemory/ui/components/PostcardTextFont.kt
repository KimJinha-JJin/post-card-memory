package com.postcardmemory.ui.components

import androidx.compose.ui.text.font.FontFamily

enum class PostcardTextFont(
    val label: String,
    val previewText: String,
    val fontFamily: FontFamily
) {
    DEFAULT(
        label = "기본",
        previewText = "가나다",
        fontFamily = FontFamily.Default
    ),
    SANS_SERIF(
        label = "고딕",
        previewText = "가나다",
        fontFamily = FontFamily.SansSerif
    ),
    SERIF(
        label = "명조",
        previewText = "가나다",
        fontFamily = FontFamily.Serif
    ),
    MONOSPACE(
        label = "타자기",
        previewText = "가나다",
        fontFamily = FontFamily.Monospace
    ),
    CURSIVE(
        label = "필기체",
        previewText = "Memory",
        fontFamily = FontFamily.Cursive
    )
}
