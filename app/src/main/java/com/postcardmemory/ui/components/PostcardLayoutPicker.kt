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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.BrutalYellow
import com.postcardmemory.ui.theme.SoftGray

enum class PostcardLayoutStyle(
    val label: String,
    val description: String
) {
    STAMP(
        label = "우표",
        description = "핑킹 가위 사진 · 크기 조절 가능"
    ),
    POLAROID(
        label = "폴라로이드",
        description = "하단 여백이 넓은 즉석사진"
    ),
    TAPED_FILM(
        label = "테이프 필름",
        description = "필름 조각을 테이프로 붙인 느낌"
    )
}

@Composable
fun PostcardLayoutPicker(
    selectedLayout: PostcardLayoutStyle,
    onLayoutSelected: (PostcardLayoutStyle) -> Unit,
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
            .padding(
                horizontal = 16.dp,
                vertical = 16.dp
            )
    ) {
        Text(
            text = "엽서 레이아웃",
            color = BrutalBlack,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "배치를 누르면 위 엽서에서 바로 확인할 수 있어.",
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
            PostcardLayoutStyle.entries
                .forEach { layout ->
                    LayoutOption(
                        layout = layout,
                        selected =
                            layout == selectedLayout,
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
    val cardShape =
        RoundedCornerShape(14.dp)

    Column(
        modifier = Modifier
            .width(126.dp)
            .graphicsLayer {
                scaleX = if (selected) 1.02f else 1f
                scaleY = if (selected) 1.02f else 1f
            }
            .background(
                color =
                    if (selected) {
                        BrutalYellow
                    } else {
                        BrutalWhite
                    },
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
        LayoutMiniPreview(
            layout = layout
        )

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
            color = BrutalBlack,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            lineHeight = 13.sp
        )
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
        contentAlignment =
            Alignment.Center
    ) {
        when (layout) {
            PostcardLayoutStyle.STAMP -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(34.dp)
                            .background(
                                color = NeutralLight,
                                shape =
                                    RoundedCornerShape(
                                        4.dp
                                    )
                            )
                            .border(
                                width = 1.dp,
                                color = BrutalBlack,
                                shape =
                                    RoundedCornerShape(
                                        4.dp
                                    )
                            )
                    )

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(8.dp)
                            .background(
                                color = BrutalBlack,
                                shape =
                                    RoundedCornerShape(
                                        4.dp
                                    )
                            )
                    )
                }
            }

            PostcardLayoutStyle.POLAROID -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(50.dp)
                            .offset(
                                x = 3.dp,
                                y = 3.dp
                            )
                            .background(
                                color =
                                    BrutalBlack.copy(
                                        alpha = 0.25f
                                    ),
                                shape =
                                    RoundedCornerShape(
                                        4.dp
                                    )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .width(50.dp)
                            .height(50.dp)
                            .background(
                                color = BrutalWhite,
                                shape =
                                    RoundedCornerShape(
                                        4.dp
                                    )
                            )
                            .border(
                                width = 1.dp,
                                color = BrutalBlack,
                                shape =
                                    RoundedCornerShape(
                                        4.dp
                                    )
                            )
                            .padding(
                                top = 4.dp,
                                start = 4.dp,
                                end = 4.dp,
                                bottom = 14.dp
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(
                                    color = NeutralLight,
                                    shape =
                                        RoundedCornerShape(
                                            3.dp
                                        )
                                )
                                .border(
                                    width = 1.dp,
                                    color = BrutalBlack,
                                    shape =
                                        RoundedCornerShape(
                                            3.dp
                                        )
                                )
                        )
                    }
                }
            }

            PostcardLayoutStyle.TAPED_FILM -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .background(
                                color = BrutalBlack,
                                shape = RoundedCornerShape(4.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 3.dp
                                ),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(
                                            color = BrutalWhite,
                                            shape =
                                                RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.8f)
                                .height(18.dp)
                                .background(
                                    color = NeutralLight,
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(
                                    horizontal = 6.dp,
                                    vertical = 3.dp
                                ),
                            horizontalArrangement =
                                Arrangement.SpaceBetween
                        ) {
                            repeat(4) {
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(
                                            color = BrutalWhite,
                                            shape =
                                                RoundedCornerShape(1.dp)
                                        )
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-2).dp, y = (-4).dp)
                                .size(width = 16.dp, height = 9.dp)
                                .background(
                                    color = SoftGray.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-4).dp)
                                .size(width = 16.dp, height = 9.dp)
                                .background(
                                    color = SoftGray.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
        }
    }
}
