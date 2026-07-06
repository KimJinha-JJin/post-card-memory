package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.BrutalYellow
import com.postcardmemory.ui.theme.SoftGray

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

@Composable
fun PostcardFontPicker(
    selectedFont: PostcardTextFont,
    onFontSelected: (PostcardTextFont) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .alpha(
                if (enabled) {
                    1f
                } else {
                    0.55f
                }
            )
            .background(
                color = SoftGray,
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 2.dp,
                color = BrutalBlack,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp
            )
    ) {
        Text(
            text = "글귀 폰트",
            color = BrutalBlack,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "글씨체를 누르면 엽서에 저장돼.",
            color = BrutalBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                )
                .padding(
                    end = 4.dp,
                    bottom = 4.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            PostcardTextFont.entries.forEach { font ->
                FontOption(
                    font = font,
                    selected = font == selectedFont,
                    enabled = enabled,
                    onClick = {
                        onFontSelected(font)
                    }
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = "화면을 나갔다 돌아와도 선택한 폰트가 유지돼.",
            color = BrutalBlack,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = NeutralLight,
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 8.dp
                )
        )
    }
}

@Composable
private fun FontOption(
    font: PostcardTextFont,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val cardShape =
        RoundedCornerShape(14.dp)

    Box {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(
                    x = 4.dp,
                    y = 4.dp
                )
                .background(
                    color = BrutalBlack,
                    shape = cardShape
                )
        )

        Column(
            modifier = Modifier
                .width(100.dp)
                .background(
                    color =
                        if (selected) {
                            BrutalYellow
                        } else {
                            BrutalWhite
                        },
                    shape = cardShape
                )
                .border(
                    width =
                        if (selected) {
                            3.dp
                        } else {
                            2.dp
                        },
                    color = BrutalBlack,
                    shape = cardShape
                )
                .clickable(
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 12.dp
                ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text = font.previewText,
                color = BrutalBlack,
                fontFamily = font.fontFamily,
                fontSize = 19.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = font.label,
                color = BrutalBlack,
                fontSize = 12.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.ExtraBold
                    } else {
                        FontWeight.Bold
                    },
                textAlign = TextAlign.Center
            )
        }
    }
}
