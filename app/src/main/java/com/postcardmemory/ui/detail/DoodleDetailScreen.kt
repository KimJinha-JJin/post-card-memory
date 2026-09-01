package com.postcardmemory.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.EditorSlider
import com.postcardmemory.ui.components.EditorTextAction
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.sealInkColors
import com.postcardmemory.utils.DoodleStrokeWidth
import kotlin.math.roundToInt

/**
 * 낙서는 스티커·마스킹테이프·도장 같은 객체 추가형 기능이 아니라 "펜을 꺼내
 * 캔버스에 바로 그리는" 도구형 기능이다. 도구(펜/형광펜/점선/지우개) 선택은
 * 이 패널이 아니라 화면 하단 고정 영역의 EditorSubcategoryNavBar가 맡는다
 * (DetailScreen.kt, 스티커·마스킹테이프 하위 탭과 같은 자리·같은 문법) —
 * 속성 영역을 스크롤해도 도구 선택줄은 항상 같은 자리에 남는다. 이 패널은
 * 색상·굵기 같은 속성과 전체 지우기만 다룬다.
 */
@Composable
fun DoodlePanel(
    doodleColorArgb: Long,
    onColorSelected: (Long) -> Unit,
    doodleWidth: DoodleStrokeWidth,
    onWidthSelected: (DoodleStrokeWidth) -> Unit,
    strokeCount: Int,
    onClearAll: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((-4).dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorUndoRedoButtons(
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                enabled = enabled,
                undoContentDescription = "실행 취소",
                redoContentDescription = "다시 실행"
            )

            Text(
                text = "${strokeCount}개",
                color = GraphiteAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "색상",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            sealInkColors.forEach { swatchColor ->
                val swatchArgb =
                    swatchColor.toArgb().toLong() and 0xFFFFFFFFL
                val isColorSelected = doodleColorArgb == swatchArgb

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                        .clickable(enabled = enabled) {
                            onColorSelected(swatchArgb)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                color = swatchColor,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = BrutalBlack.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(
                                color =
                                    if (isColorSelected) {
                                        SunsetGold
                                    } else {
                                        Color.Transparent
                                    },
                                shape = CircleShape
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 굵기 3단계(THIN/MEDIUM/THICK)는 값 자체를 바꾸지 않고 표현만
        // 여러 개의 선택 타일 대신 하나의 슬라이더로 바꾼다 — 0/1/2 정수
        // 위치만 갖는 슬라이더(steps=1)라 중간값이 생기지 않는다.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "굵기",
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.width(12.dp))

            EditorSlider(
                value = doodleWidth.sliderPosition,
                onValueChange = { position ->
                    onWidthSelected(doodleStrokeWidthForSliderPosition(position))
                },
                valueRange = 0f..2f,
                steps = 1,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        EditorTextAction(
            text = "전체 지우기",
            onClick = onClearAll,
            enabled = enabled && strokeCount > 0,
            contentColor = GalleryDangerRed
        )
    }
}

private val DoodleStrokeWidth.sliderPosition: Float
    get() = when (this) {
        DoodleStrokeWidth.THIN -> 0f
        DoodleStrokeWidth.MEDIUM -> 1f
        DoodleStrokeWidth.THICK -> 2f
    }

private fun doodleStrokeWidthForSliderPosition(position: Float): DoodleStrokeWidth =
    when (position.roundToInt()) {
        0 -> DoodleStrokeWidth.THIN
        1 -> DoodleStrokeWidth.MEDIUM
        else -> DoodleStrokeWidth.THICK
    }
