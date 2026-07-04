package com.postcardmemory.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalDeepViolet
import com.postcardmemory.ui.theme.BrutalViolet
import com.postcardmemory.ui.theme.BrutalWhite

val postcardBackgroundPalette =
    listOf(
        0xFFFFFBF7L,
        0xFFFFE8E8L,
        0xFFFFE8D6L,
        0xFFFFF1C7L,
        0xFFE9F4D8L,
        0xFFDDF5EEL,
        0xFFDDEFFFL,
        0xFFE8E4FFL,
        0xFFF3E1FFL,
        0xFFFFE2F2L,
        0xFFE4E4E4L,
        0xFF2E2638L
    )

/** 엽서 배경 위에 반복해서 표시할 패턴 종류 */
enum class PostcardBackgroundPattern(
    val label: String,
    val symbol: String
) {
    NONE("없음", "×"),
    DOTS("땡땡이", "●"),
    CHECKER("체크", "▦"),
    STRIPES("사선무늬", "╱"),
    WAVES("물결무늬", "〜"),
    GRID("격자무늬", "#"),
    CROSSHATCH("교차무늬", "▨"),
    SPECKLE("은은한 입자", "░")
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun PostcardBackgroundPicker(
    selectedColorArgb: Long,
    hasBackgroundImage: Boolean,
    enabled: Boolean = true,
    onColorSelected: (Long) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    modifier: Modifier = Modifier,
    selectedPattern: PostcardBackgroundPattern = PostcardBackgroundPattern.NONE,
    onPatternSelected: (PostcardBackgroundPattern) -> Unit = {}
) {
    var showCustomColorPicker by rememberSaveable {
        mutableStateOf(false)
    }

    var localSelectedPatternName by rememberSaveable {
        mutableStateOf(selectedPattern.name)
    }

    LaunchedEffect(selectedPattern) {
        localSelectedPatternName = selectedPattern.name
    }

    val visibleSelectedPattern =
        PostcardBackgroundPattern.entries.firstOrNull { pattern ->
            pattern.name == localSelectedPatternName
        } ?: PostcardBackgroundPattern.NONE

    val selectedIsCustomColor =
        postcardBackgroundPalette.none { colorArgb ->
            colorArgb == selectedColorArgb
        }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = BrutalWhite,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = BrutalBlack,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text = "엽서 배경 꾸미기",
            color = BrutalDeepViolet,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "기본 색상이나 기타 색상에서 원하는 배경색을 골라봐.",
            color = BrutalDeepViolet,
            fontSize = 13.sp
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            postcardBackgroundPalette.forEach { colorArgb ->
                val selected =
                    selectedColorArgb == colorArgb

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color(colorArgb),
                            shape = CircleShape
                        )
                        .border(
                            width =
                                if (selected) {
                                    4.dp
                                } else {
                                    2.dp
                                },
                            color =
                                if (selected) {
                                    BrutalViolet
                                } else {
                                    BrutalBlack
                                },
                            shape = CircleShape
                        )
                        .clickable(
                            enabled = enabled
                        ) {
                            onColorSelected(colorArgb)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    color = BrutalWhite,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = BrutalBlack,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(86.dp)
                    .height(48.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red,
                                Color.Yellow,
                                Color.Green,
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta,
                                Color.Red
                            )
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width =
                            if (selectedIsCustomColor) {
                                4.dp
                            } else {
                                2.dp
                            },
                        color =
                            if (selectedIsCustomColor) {
                                BrutalViolet
                            } else {
                                BrutalBlack
                            },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(
                        enabled = enabled
                    ) {
                        showCustomColorPicker = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "기타 색상",
                    modifier = Modifier
                        .background(
                            color = BrutalWhite.copy(alpha = 0.88f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(
                            horizontal = 8.dp,
                            vertical = 5.dp
                        ),
                    color = BrutalDeepViolet,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = "배경 패턴",
            color = BrutalDeepViolet,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = "색상 위에 얹을 무늬를 골라봐. 패턴 없음도 언제든 선택할 수 있어.",
            color = BrutalDeepViolet,
            fontSize = 12.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(
                    rememberScrollState()
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            PostcardBackgroundPattern.entries.forEach { pattern ->
                val selected =
                    visibleSelectedPattern == pattern

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color =
                                    if (selected) {
                                        Color(selectedColorArgb)
                                    } else {
                                        Color(0xFFF7F2FF)
                                    },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .border(
                                width =
                                    if (selected) {
                                        4.dp
                                    } else {
                                        2.dp
                                    },
                                color =
                                    if (selected) {
                                        BrutalViolet
                                    } else {
                                        BrutalBlack
                                    },
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                enabled = enabled
                            ) {
                                localSelectedPatternName = pattern.name
                                onPatternSelected(pattern)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pattern.symbol,
                            color =
                                if (selected) {
                                    BrutalWhite
                                } else {
                                    BrutalDeepViolet
                                },
                            fontSize =
                                if (pattern == PostcardBackgroundPattern.NONE) {
                                    25.sp
                                } else {
                                    22.sp
                                },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(5.dp)
                    )

                    Text(
                        text = pattern.label,
                        color = BrutalDeepViolet,
                        fontSize = 11.sp,
                        fontWeight =
                            if (selected) {
                                FontWeight.ExtraBold
                            } else {
                                FontWeight.Medium
                            }
                    )
                }
            }
        }
    }

    if (showCustomColorPicker) {
        CustomColorPickerDialog(
            initialColorArgb = selectedColorArgb,
            onDismiss = {
                showCustomColorPicker = false
            },
            onColorConfirmed = { colorArgb ->
                showCustomColorPicker = false
                onColorSelected(colorArgb)
            }
        )
    }
}

@Composable
private fun CustomColorPickerDialog(
    initialColorArgb: Long,
    onDismiss: () -> Unit,
    onColorConfirmed: (Long) -> Unit
) {
    val initialHsv =
        remember(initialColorArgb) {
            FloatArray(3).also { hsv ->
                AndroidColor.colorToHSV(
                    initialColorArgb.toInt(),
                    hsv
                )
            }
        }

    var hue by remember(initialColorArgb) {
        mutableStateOf(initialHsv[0])
    }

    var saturation by remember(initialColorArgb) {
        mutableStateOf(initialHsv[1])
    }

    var value by remember(initialColorArgb) {
        mutableStateOf(initialHsv[2])
    }

    var colorAreaWidth by remember {
        mutableIntStateOf(1)
    }

    var colorAreaHeight by remember {
        mutableIntStateOf(1)
    }

    var hueBarWidth by remember {
        mutableIntStateOf(1)
    }

    val selectedColorInt =
        AndroidColor.HSVToColor(
            floatArrayOf(
                hue,
                saturation,
                value
            )
        )

    val selectedColorArgb =
        selectedColorInt.toLong() and 0xFFFFFFFFL

    val selectedColor =
        Color(selectedColorArgb)

    val hueColor =
        Color(
            AndroidColor.HSVToColor(
                floatArrayOf(
                    hue,
                    1f,
                    1f
                )
            )
        )

    fun updateSaturationAndValue(
        offset: Offset
    ) {
        saturation =
            (offset.x / colorAreaWidth.toFloat())
                .coerceIn(0f, 1f)

        value =
            1f -
                    (offset.y / colorAreaHeight.toFloat())
                        .coerceIn(0f, 1f)
    }

    fun updateHue(
        offset: Offset
    ) {
        hue =
            (
                    offset.x /
                            hueBarWidth.toFloat() *
                            360f
                    ).coerceIn(0f, 359.999f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "기타 색상 선택",
                color = BrutalDeepViolet,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "큰 색상판을 눌러 원하는 색의 밝기와 선명도를 골라봐.",
                    color = BrutalDeepViolet,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.35f)
                        .onSizeChanged { size ->
                            colorAreaWidth =
                                size.width.coerceAtLeast(1)

                            colorAreaHeight =
                                size.height.coerceAtLeast(1)
                        }
                        .pointerInput(
                            colorAreaWidth,
                            colorAreaHeight
                        ) {
                            detectTapGestures { offset ->
                                updateSaturationAndValue(
                                    offset
                                )
                            }
                        }
                        .pointerInput(
                            colorAreaWidth,
                            colorAreaHeight
                        ) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    updateSaturationAndValue(
                                        offset
                                    )
                                },
                                onDrag = { change, _ ->
                                    updateSaturationAndValue(
                                        change.position
                                    )
                                }
                            )
                        }
                ) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White,
                                hueColor
                            )
                        )
                    )

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            )
                        )
                    )

                    val indicatorCenter =
                        Offset(
                            x =
                                saturation *
                                        size.width,
                            y =
                                (1f - value) *
                                        size.height
                        )

                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = indicatorCenter,
                        style = Stroke(
                            width = 3.dp.toPx()
                        )
                    )

                    drawCircle(
                        color = Color.Black,
                        radius = 13.dp.toPx(),
                        center = indicatorCenter,
                        style = Stroke(
                            width = 2.dp.toPx()
                        )
                    )
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Text(
                    text = "색상 계열",
                    color = BrutalDeepViolet,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .onSizeChanged { size ->
                            hueBarWidth =
                                size.width.coerceAtLeast(1)
                        }
                        .pointerInput(hueBarWidth) {
                            detectTapGestures { offset ->
                                updateHue(offset)
                            }
                        }
                        .pointerInput(hueBarWidth) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    updateHue(offset)
                                },
                                onDrag = { change, _ ->
                                    updateHue(
                                        change.position
                                    )
                                }
                            )
                        }
                ) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Red,
                                Color.Yellow,
                                Color.Green,
                                Color.Cyan,
                                Color.Blue,
                                Color.Magenta,
                                Color.Red
                            )
                        ),
                        cornerRadius =
                            androidx.compose.ui.geometry.CornerRadius(
                                12.dp.toPx(),
                                12.dp.toPx()
                            )
                    )

                    val indicatorX =
                        hue / 360f * size.width

                    drawLine(
                        color = Color.White,
                        start = Offset(
                            indicatorX,
                            0f
                        ),
                        end = Offset(
                            indicatorX,
                            size.height
                        ),
                        strokeWidth = 5.dp.toPx()
                    )

                    drawLine(
                        color = Color.Black,
                        start = Offset(
                            indicatorX,
                            0f
                        ),
                        end = Offset(
                            indicatorX,
                            size.height
                        ),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = selectedColor,
                                shape = CircleShape
                            )
                            .border(
                                width = 2.dp,
                                color = BrutalBlack,
                                shape = CircleShape
                            )
                    )

                    Column {
                        Text(
                            text = "선택한 색상",
                            color = BrutalDeepViolet,
                            fontSize = 12.sp
                        )

                        Text(
                            text =
                                "#" +
                                        selectedColorArgb
                                            .toString(16)
                                            .uppercase()
                                            .padStart(8, '0'),
                            color = BrutalDeepViolet,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onColorConfirmed(
                        selectedColorArgb
                    )
                }
            ) {
                Text(
                    text = "적용",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "취소",
                    color = BrutalDeepViolet,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}