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
import com.postcardmemory.ui.components.EditorEmptyHint
import com.postcardmemory.ui.components.EditorOutlineButton
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "낙서",
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "도구",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "색상",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
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
            fontWeight = FontWeight.SemiBold
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

        if (strokeCount == 0) {
            Spacer(modifier = Modifier.height(14.dp))

            EditorEmptyHint(
                text = "손가락으로 엽서 위에 낙서를 그려봐."
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
                    color = if (selected) SunsetGold.copy(alpha = 0.18f) else BrutalWhite,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) SunsetGold else NeutralLight,
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
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
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
                    color = if (selected) SunsetGold.copy(alpha = 0.18f) else BrutalWhite,
                    shape = RoundedCornerShape(10.dp)
                )
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) SunsetGold else NeutralLight,
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
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
