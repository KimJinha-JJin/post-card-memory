package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight

@Composable
fun EditorUndoRedoButtons(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    enabled: Boolean,
    undoContentDescription: String,
    redoContentDescription: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-4).dp)
    ) {
        IconButton(
            onClick = onUndo,
            enabled = enabled && canUndo
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Undo,
                contentDescription = undoContentDescription,
                tint = if (canUndo) BrutalBlack else GraphiteAccent,
                modifier = Modifier.size(18.dp)
            )
        }

        IconButton(
            onClick = onRedo,
            enabled = enabled && canRedo
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = redoContentDescription,
                tint = if (canRedo) BrutalBlack else GraphiteAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun EditorEmptyHint(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = BrutalBlack,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = BrutalWhite,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    )
}

/**
 * 편집 패널 공통 슬라이더.
 *
 * 얇은 4dp 트랙과 작은 16dp 원형 손잡이로 통일한다. 색상은 개편 전
 * 기존 값을 유지한다(활성·손잡이 = BrutalBlack, 비활성 = 호출부가 넘기는
 * inactiveTrackColor, 기본 NeutralLight). value·valueRange·steps·콜백은
 * 호출부에서 그대로 전달받아 기능·저장 동작은 바뀌지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    steps: Int = 0,
    enabled: Boolean = true,
    inactiveTrackColor: Color = NeutralLight
) {
    val span = valueRange.endInclusive - valueRange.start
    val fraction =
        if (span > 0f) {
            ((value - valueRange.start) / span).coerceIn(0f, 1f)
        } else {
            0f
        }
    val accentColor = if (enabled) BrutalBlack else inactiveTrackColor

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = accentColor,
                        shape = CircleShape
                    )
            )
        },
        track = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(inactiveTrackColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accentColor)
                )
            }
        }
    )
}
