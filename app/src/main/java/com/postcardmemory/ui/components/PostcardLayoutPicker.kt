package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.SunsetGold

enum class PostcardLayoutStyle(
    val label: String
) {
    STAMP("우표"),
    POLAROID("폴라로이드"),
    TAPED_FILM("테이프 필름"),
    LETTER("편지지")
}

@Composable
fun PostcardLayoutPicker(
    selectedLayout: PostcardLayoutStyle,
    onLayoutSelected: (PostcardLayoutStyle) -> Unit,
    onUndoPhotoTransform: () -> Unit,
    onRedoPhotoTransform: () -> Unit,
    canUndoPhotoTransform: Boolean,
    canRedoPhotoTransform: Boolean,
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
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "엽서 레이아웃",
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            EditorUndoRedoButtons(
                canUndo = canUndoPhotoTransform,
                canRedo = canRedoPhotoTransform,
                onUndo = onUndoPhotoTransform,
                onRedo = onRedoPhotoTransform,
                enabled = enabled,
                undoContentDescription = "실행 취소",
                redoContentDescription = "다시 실행"
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PostcardLayoutStyle.entries.forEach { layout ->
                LayoutOption(
                    layout = layout,
                    selected = layout == selectedLayout,
                    enabled = enabled,
                    onClick = {
                        onLayoutSelected(layout)
                    },
                    modifier = Modifier.weight(1f)
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(
                color =
                    if (selected) {
                        SunsetGold.copy(alpha = 0.16f)
                    } else {
                        PaperField
                    },
                shape = cardShape
            )
            .border(
                width = 1.dp,
                color = PaperDivider,
                shape = cardShape
            )
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(
                horizontal = 6.dp,
                vertical = 10.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = layout.label,
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                },
            textAlign = TextAlign.Center
        )
    }
}
