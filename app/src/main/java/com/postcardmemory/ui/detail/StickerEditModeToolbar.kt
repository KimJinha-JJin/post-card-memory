package com.postcardmemory.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight

@Composable
internal fun StickerEditModeToolbar(
    sticker: PhotoStickerItem,
    editMode: StickerEditMode,
    onModeSelected: (StickerEditMode) -> Unit,
    isRemovingBackground: Boolean,
    onToggleBackgroundRemoval: () -> Unit,
    onToggleFlipHorizontal: () -> Unit,
    onToggleFlipVertical: () -> Unit,
    canMoveForward: Boolean,
    canMoveBackward: Boolean,
    onMoveForward: () -> Unit,
    onMoveBackward: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .background(
                color = NeutralLight,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StickerEditModeButton(
            label = "이동",
            selected = editMode == StickerEditMode.Move,
            enabled = enabled,
            onClick = {
                onModeSelected(StickerEditMode.Move)
            }
        )

        StickerEditModeButton(
            label = "크기",
            selected = editMode == StickerEditMode.Scale,
            enabled = enabled,
            onClick = {
                onModeSelected(
                    if (editMode == StickerEditMode.Scale) {
                        StickerEditMode.Move
                    } else {
                        StickerEditMode.Scale
                    }
                )
            }
        )

        StickerEditModeButton(
            label = "회전",
            selected = editMode == StickerEditMode.Rotate,
            enabled = enabled,
            onClick = {
                onModeSelected(
                    if (editMode == StickerEditMode.Rotate) {
                        StickerEditMode.Move
                    } else {
                        StickerEditMode.Rotate
                    }
                )
            }
        )

        StickerEditModeButton(
            label = "좌우대칭",
            selected = sticker.flipHorizontal,
            enabled = enabled,
            onClick = onToggleFlipHorizontal
        )

        StickerEditModeButton(
            label = "상하대칭",
            selected = sticker.flipVertical,
            enabled = enabled,
            onClick = onToggleFlipVertical
        )

        StickerEditModeButton(
            label =
                when {
                    isRemovingBackground -> "처리중..."
                    sticker.isBackgroundRemoved -> "원본복원"
                    else -> "배경제거"
                },
            selected = sticker.isBackgroundRemoved,
            enabled = enabled && !isRemovingBackground,
            onClick = onToggleBackgroundRemoval
        )

        StickerEditModeButton(
            label = "뒤로",
            selected = false,
            enabled = enabled && canMoveBackward,
            onClick = onMoveBackward
        )

        StickerEditModeButton(
            label = "앞으로",
            selected = false,
            enabled = enabled && canMoveForward,
            onClick = onMoveForward
        )
    }
}

@Composable
private fun StickerEditModeButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .background(
                color = if (selected) GraphiteAccent else BrutalWhite,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) BrutalWhite else BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
