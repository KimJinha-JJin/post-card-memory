package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.detail.PhotoStickerPickerPanel
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalDeepViolet
import com.postcardmemory.ui.theme.BrutalLavender
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.BrutalYellow
import com.postcardmemory.ui.theme.LavenderSoft

enum class PostcardLayoutStyle(
    val label: String,
    val description: String
) {
    STANDARD(
        label = "기본형",
        description = "사진 위 · 글귀 아래"
    ),
    PHOTO_FOCUS(
        label = "사진 강조",
        description = "사진을 더 크게"
    ),
    AIRY(
        label = "여백형",
        description = "작은 사진 · 넓은 여백"
    ),
    MAGAZINE(
        label = "잡지형",
        description = "글귀를 사진 위에"
    )
}

@Composable
fun PostcardLayoutPicker(
    selectedLayout: PostcardLayoutStyle,
    onLayoutSelected: (PostcardLayoutStyle) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val drawerScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .alpha(
                if (enabled) {
                    1f
                } else {
                    0.55f
                }
            )
    ) {
        Text(
            text = "사진 꾸미기",
            color = BrutalDeepViolet,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "첫 번째 칸은 레이아웃, 오른쪽으로 밀면 두 번째 칸에서 스티커 사진을 고를 수 있어.",
            color = BrutalDeepViolet,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val pageWidth = maxWidth

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(drawerScrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LayoutDrawerPage(
                    selectedLayout = selectedLayout,
                    onLayoutSelected = onLayoutSelected,
                    enabled = enabled,
                    modifier = Modifier.width(pageWidth)
                )

                PhotoStickerPickerPanel(
                    enabled = enabled,
                    modifier = Modifier.width(pageWidth)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = "1 / 2  →  오른쪽으로 밀어서 스티커 사진 열기",
            color = BrutalDeepViolet,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BrutalLavender,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = 2.dp,
                    color = BrutalBlack,
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
private fun LayoutDrawerPage(
    selectedLayout: PostcardLayoutStyle,
    onLayoutSelected: (PostcardLayoutStyle) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = LavenderSoft,
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "엽서 레이아웃",
                color = BrutalDeepViolet,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "1 / 2",
                color = BrutalDeepViolet,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = BrutalBlack,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 5.dp
                    )
            )
        }

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "배치를 누르면 위 엽서에서 바로 확인할 수 있어.",
            color = BrutalDeepViolet,
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
            PostcardLayoutStyle.entries.forEach { layout ->
                LayoutOption(
                    layout = layout,
                    selected = layout == selectedLayout,
                    enabled = enabled,
                    onClick = {
                        onLayoutSelected(layout)
                    }
                )
            }
        }
    }
}

@Composable
private fun LayoutOption(
    layout: PostcardLayoutStyle,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(14.dp)

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
                .width(126.dp)
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LayoutMiniPreview(layout)

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = layout.label,
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight =
                    if (selected) {
                        FontWeight.ExtraBold
                    } else {
                        FontWeight.Bold
                    },
                textAlign = TextAlign.Center
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = layout.description,
                color = BrutalDeepViolet,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                minLines = 2,
                maxLines = 2,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun LayoutMiniPreview(
    layout: PostcardLayoutStyle
) {
    Box(
        modifier = Modifier
            .size(
                width = 76.dp,
                height = 66.dp
            )
            .background(
                color = BrutalWhite,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = BrutalBlack,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        when (layout) {
            PostcardLayoutStyle.STANDARD -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PreviewPhoto(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(34.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    PreviewText(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(8.dp)
                    )
                }
            }

            PostcardLayoutStyle.PHOTO_FOCUS -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PreviewPhoto(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    PreviewText(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(6.dp)
                    )
                }
            }

            PostcardLayoutStyle.AIRY -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    PreviewPhoto(
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    PreviewText(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(6.dp)
                    )
                }
            }

            PostcardLayoutStyle.MAGAZINE -> {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    PreviewPhoto(
                        modifier = Modifier.fillMaxSize()
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(15.dp)
                            .background(
                                color = BrutalBlack.copy(
                                    alpha = 0.74f
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPhoto(
    modifier: Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = BrutalLavender,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = BrutalBlack,
                shape = RoundedCornerShape(4.dp)
            )
    )
}

@Composable
private fun PreviewText(
    modifier: Modifier
) {
    Box(
        modifier = modifier.background(
            color = BrutalDeepViolet,
            shape = RoundedCornerShape(4.dp)
        )
    )
}
