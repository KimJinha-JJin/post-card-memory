package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.SunsetGold

enum class PostcardLayoutStyle(
    val label: String
) {
    STAMP("우표"),
    POLAROID("폴라로이드"),
    TAPED_FILM("테이프 필름"),
    LETTER("편지지")
}

/**
 * 4개 레이아웃 중 하나를 고르는 상호배타 선택지다. 가로 탭/세그먼트/pill이
 * 아니라 각 항목이 한 행을 차지하는 세로 목록으로 두고, 왼쪽 사각 체크
 * 표시로만 선택 상태를 나타낸다 — 라디오버튼(원형)은 쓰지 않는다.
 */
@Composable
fun PostcardLayoutPicker(
    selectedLayout: PostcardLayoutStyle,
    onLayoutSelected: (PostcardLayoutStyle) -> Unit,
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
        PostcardLayoutStyle.entries.forEach { layout ->
            LayoutRow(
                layout = layout,
                selected = layout == selectedLayout,
                enabled = enabled,
                onClick = {
                    onLayoutSelected(layout)
                }
            )
        }
    }
}

@Composable
private fun LayoutRow(
    layout: PostcardLayoutStyle,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val checkShape = RoundedCornerShape(4.dp)

        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (selected) SunsetGold else Color.Transparent,
                    shape = checkShape
                )
                .border(
                    width = 1.5.dp,
                    color = if (selected) SunsetGold else PaperDivider,
                    shape = checkShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = layout.label,
            color = BrutalBlack,
            fontSize = 15.sp,
            fontWeight =
                if (selected) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
        )
    }
}
