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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.SoftGray
import com.postcardmemory.ui.theme.SunsetCoral

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
                tint = if (canUndo) BrutalBlack else SoftGray
            )
        }

        IconButton(
            onClick = onRedo,
            enabled = enabled && canRedo
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Redo,
                contentDescription = redoContentDescription,
                tint = if (canRedo) BrutalBlack else SoftGray
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
 * 오래된 종이 톤에 맞춰 얇은 트랙(코랄 활성 / PaperDivider 비활성)과
 * 작은 코랄 원형 손잡이로 통일한다. value·valueRange·steps·콜백은
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
    enabled: Boolean = true
) {
    val span = valueRange.endInclusive - valueRange.start
    val fraction =
        if (span > 0f) {
            ((value - valueRange.start) / span).coerceIn(0f, 1f)
        } else {
            0f
        }
    val accentColor = if (enabled) SunsetCoral else PaperDivider

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
                        .background(PaperDivider)
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
