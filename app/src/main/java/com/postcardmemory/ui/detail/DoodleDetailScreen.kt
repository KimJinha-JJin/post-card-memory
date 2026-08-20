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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.EditorOutlineButton
import com.postcardmemory.ui.components.EditorQuietHint
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.sealInkColors
import com.postcardmemory.utils.DoodleStrokeWidth
import com.postcardmemory.utils.DoodleTool

@Composable
fun DoodlePanel(
    doodleTool: DoodleTool,
    onToolSelected: (DoodleTool) -> Unit,
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
            text = "도구",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 도구가 4개라 좁은 화면이나 큰 글자 설정에서는 한 줄을 넘길 수 있다.
        // 잘려서 안 보이는 대신 가로로 밀어 볼 수 있게 한다(굵기 줄도 동일).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DoodleToolTile(
                label = "펜",
                icon = Icons.Default.Draw,
                selected = doodleTool == DoodleTool.PEN,
                enabled = enabled,
                onClick = { onToolSelected(DoodleTool.PEN) }
            )

            DoodleToolTile(
                label = "형광펜",
                icon = Icons.Default.BorderColor,
                selected = doodleTool == DoodleTool.HIGHLIGHTER,
                enabled = enabled,
                onClick = { onToolSelected(DoodleTool.HIGHLIGHTER) }
            )

            DoodleToolTile(
                label = "점선",
                icon = Icons.Default.MoreHoriz,
                selected = doodleTool == DoodleTool.DOTTED,
                enabled = enabled,
                onClick = { onToolSelected(DoodleTool.DOTTED) }
            )

            DoodleToolTile(
                label = "지우개",
                icon = Icons.AutoMirrored.Filled.Backspace,
                selected = doodleTool == DoodleTool.ERASER,
                enabled = enabled,
                onClick = { onToolSelected(DoodleTool.ERASER) }
            )
        }

        // 직선은 전용 버튼이 없는 손가락 동작이라 안내가 없으면 찾을 수 없다.
        // 빈 상태 안내에만 넣으면 이미 낙서가 있는 사람은 못 보므로 획 개수와
        // 무관하게 띄우고, 직선이 적용되지 않는 지우개일 때만 감춘다.
        if (doodleTool != DoodleTool.ERASER) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorQuietHint(
                text = "길게 누른 채 끌면 직선을 그릴 수 있어."
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "색상",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            sealInkColors.forEach { swatchColor ->
                val swatchArgb =
                    swatchColor.toArgb().toLong() and 0xFFFFFFFFL
                val isColorSelected = doodleColorArgb == swatchArgb

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(enabled = enabled) {
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

        Text(
            text = "굵기",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DoodleWidthTile(
                label = "가는 선",
                dotSize = 8.dp,
                selected = doodleWidth == DoodleStrokeWidth.THIN,
                enabled = enabled,
                onClick = { onWidthSelected(DoodleStrokeWidth.THIN) }
            )

            DoodleWidthTile(
                label = "보통 선",
                dotSize = 14.dp,
                selected = doodleWidth == DoodleStrokeWidth.MEDIUM,
                enabled = enabled,
                onClick = { onWidthSelected(DoodleStrokeWidth.MEDIUM) }
            )

            DoodleWidthTile(
                label = "굵은 선",
                dotSize = 20.dp,
                selected = doodleWidth == DoodleStrokeWidth.THICK,
                enabled = enabled,
                onClick = { onWidthSelected(DoodleStrokeWidth.THICK) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        EditorOutlineButton(
            text = "전체 지우기",
            icon = Icons.Default.Delete,
            onClick = onClearAll,
            enabled = enabled && strokeCount > 0,
            contentColor = GalleryDangerRed,
            borderColor = GalleryDangerRed
        )
    }
}

@Composable
private fun DoodleToolTile(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = BrutalWhite,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) SunsetGold else BrutalBlack,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = BrutalBlack,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .height(2.dp)
                .width(24.dp)
                .background(
                    color = if (selected) SunsetGold else Color.Transparent,
                    shape = RoundedCornerShape(1.dp)
                )
        )
    }
}

@Composable
private fun DoodleWidthTile(
    label: String,
    dotSize: Dp,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = BrutalWhite,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        color = if (selected) SunsetGold else BrutalBlack,
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = BrutalBlack,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .height(2.dp)
                .width(24.dp)
                .background(
                    color = if (selected) SunsetGold else Color.Transparent,
                    shape = RoundedCornerShape(1.dp)
                )
        )
    }
}
