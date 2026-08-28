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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
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
import com.postcardmemory.ui.theme.SunsetGold

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
    SPECKLE("은은한 입자", "░"),
    HEISEI("잔꽃무늬", "❀")
}

/**
 * 배경 색상 스와치 하나(프리셋·사진 추출색 공용). 원형 미리보기 + 아래
 * 선택 점만으로 선택 상태를 전달한다 — 카드 배경 없음.
 */
@Composable
fun BackgroundColorSwatch(
    colorArgb: Long,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    color = Color(colorArgb),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = BrutalBlack.copy(alpha = 0.35f),
                    shape = CircleShape
                )
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Box(
            modifier = Modifier
                .size(5.dp)
                .background(
                    color =
                        if (selected) {
                            SunsetGold
                        } else {
                            Color.Transparent
                        },
                    shape = CircleShape
                )
        )
    }
}

@Composable
fun PostcardBackgroundColorPicker(
    selectedColorArgb: Long,
    enabled: Boolean = true,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "배경 색상",
            color = BrutalBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
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
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Top
        ) {
            postcardBackgroundPalette.forEach { colorArgb ->
                BackgroundColorSwatch(
                    colorArgb = colorArgb,
                    selected = selectedColorArgb == colorArgb,
                    enabled = enabled,
                    onClick = {
                        onColorSelected(colorArgb)
                    }
                )
            }
        }
    }
}

/**
 * 배경 패턴 선택. 56일차 카드 제거 개편: 패턴 기호를 감싸던 카드형
 * `DecorationPresetTile` 대신 평면형 `EditorFlatPresetTile`(스티커/텍스트/
 * 라벨 목록과 같은 문법)을 써서 기호+이름+선택 밑줄만 남긴다. 선택된
 * 패턴은 기호 색을 SunsetGold로 강조해 카드 배경(색상+패턴 조합 미리보기)이
 * 없어도 어떤 패턴인지, 무엇이 선택됐는지 구분되게 한다.
 */
@Composable
fun PostcardBackgroundPatternPicker(
    selectedPattern: PostcardBackgroundPattern = PostcardBackgroundPattern.NONE,
    enabled: Boolean = true,
    onPatternSelected: (PostcardBackgroundPattern) -> Unit,
    modifier: Modifier = Modifier
) {
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

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "배경 패턴",
            color = BrutalBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            PostcardBackgroundPattern.entries.forEach { pattern ->
                val selected =
                    visibleSelectedPattern == pattern

                EditorFlatPresetTile(
                    onClick = {
                        localSelectedPatternName = pattern.name
                        onPatternSelected(pattern)
                    },
                    enabled = enabled,
                    previewModifier = Modifier.size(40.dp),
                    label = pattern.label,
                    selected = selected
                ) {
                    Text(
                        text = pattern.symbol,
                        color =
                            if (selected) {
                                SunsetGold
                            } else {
                                BrutalBlack
                            },
                        fontSize =
                            if (pattern == PostcardBackgroundPattern.NONE) {
                                25.sp
                            } else {
                                22.sp
                            },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * selectedColorArgb가 바뀔 때마다 색상판/색상계열 HSV 상태를 무조건
 * 다시 만들면, 그 상태를 읽고 쓰는 pointerInput 제스처는 캔버스 크기가
 * 바뀔 때만 재시작되므로 명도 조절과 색상 선택이 서로 다른 세대의
 * HSV 상태를 가리키게 된다. 이 색이 이 컴포저블 자신이 방금 emit한
 * 값과 같다면(자기 반향) 재동기화하지 말고, 프리셋 스와치처럼 외부에서
 * 바뀐 값일 때만 재동기화해야 두 상태가 항상 같은 것을 가리킨다.
 */
internal fun shouldResyncCustomColorHsv(
    externalColorArgb: Long,
    lastEmittedColorArgb: Long
): Boolean =
    externalColorArgb != lastEmittedColorArgb

/**
 * HSVToColor는 RGB를 0~255 정수로 반올림하므로, 드래그 중 인접한 여러
 * 포인터 이동이 같은 ARGB로 수렴하는 경우가 흔하다. 실제로 색이 바뀌지
 * 않았다면 ViewModel 저장·미리보기 재렌더로 이어지는 onColorSelected
 * 호출을 생략해도 사용자가 보는 최종 색은 달라지지 않는다.
 */
internal fun shouldEmitCustomColor(
    newColorArgb: Long,
    lastEmittedColorArgb: Long
): Boolean =
    newColorArgb != lastEmittedColorArgb

private fun colorArgbToHsv(
    colorArgb: Long
): FloatArray =
    FloatArray(3).also { hsv ->
        AndroidColor.colorToHSV(
            colorArgb.toInt(),
            hsv
        )
    }

@Composable
fun PostcardCustomColorPicker(
    selectedColorArgb: Long,
    enabled: Boolean = true,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val initialHsv =
        remember {
            colorArgbToHsv(selectedColorArgb)
        }

    var hue by remember {
        mutableStateOf(initialHsv[0])
    }

    var saturation by remember {
        mutableStateOf(initialHsv[1])
    }

    var value by remember {
        mutableStateOf(initialHsv[2])
    }

    var lastEmittedColorArgb by remember {
        mutableStateOf(selectedColorArgb)
    }

    LaunchedEffect(selectedColorArgb) {
        if (shouldResyncCustomColorHsv(selectedColorArgb, lastEmittedColorArgb)) {
            val hsv = colorArgbToHsv(selectedColorArgb)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            lastEmittedColorArgb = selectedColorArgb
        }
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

    val previewColorInt =
        AndroidColor.HSVToColor(
            floatArrayOf(
                hue,
                saturation,
                value
            )
        )

    val previewColorArgb =
        previewColorInt.toLong() and 0xFFFFFFFFL

    val previewColor =
        Color(previewColorArgb)

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

    fun emitColor() {
        val colorInt =
            AndroidColor.HSVToColor(
                floatArrayOf(hue, saturation, value)
            )

        val argb = colorInt.toLong() and 0xFFFFFFFFL

        if (!shouldEmitCustomColor(argb, lastEmittedColorArgb)) {
            return
        }

        lastEmittedColorArgb = argb

        onColorSelected(argb)
    }

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

        emitColor()
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

        emitColor()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "기타 색상",
            color = BrutalBlack,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "색상판을 눌러서 원하는 색을 골라봐.",
            color = BrutalBlack,
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
            color = BrutalBlack,
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
                        color = previewColor,
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
                    color = BrutalBlack,
                    fontSize = 12.sp
                )

                Text(
                    text =
                        "#" +
                                previewColorArgb
                                    .toString(16)
                                    .uppercase()
                                    .takeLast(6),
                    color = BrutalBlack,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
