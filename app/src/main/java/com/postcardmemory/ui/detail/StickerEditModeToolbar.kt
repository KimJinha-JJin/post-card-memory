package com.postcardmemory.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.postcardmemory.ui.theme.PaperDivider

@Composable
internal fun StickerEditModeToolbar(
    sticker: PhotoStickerItem,
    isRemovingBackground: Boolean,
    onToggleBackgroundRemoval: () -> Unit,
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
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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

        StickerToolbarGroupDivider()

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

/**
 * 이미지처리(배경제거·원본복원)와 쌓임순서(뒤로·앞으로)가 성격이 다른
 * action이라는 걸 보여주는 얇은 구분선. gesture 로직과는 무관한 순수
 * 시각 구분이다. 이동/크기/회전/좌우·상하대칭은 pinch·twist 제스처가
 * 이미 처리하므로 툴바에서 뺐다.
 */
@Composable
private fun StickerToolbarGroupDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(color = PaperDivider)
    )
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
