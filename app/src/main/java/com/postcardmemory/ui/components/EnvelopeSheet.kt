package com.postcardmemory.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.ScreenBackgroundGray
import com.postcardmemory.ui.theme.SunsetGold
import kotlinx.coroutines.launch

/**
 * 봉투 고르기/바꾸기와, 봉투가 있을 때의 소인 찍기·다시 찍기·지우기·꺼내기를
 * 한 바텀시트에 묶는다(작업지시서: 메뉴가 길어지면 봉투 관련 기능을 하나의
 * BottomSheet로 묶는다). [currentStyle]이 null이면 스타일 선택만 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeSheet(
    currentStyle: EnvelopeStyle?,
    postmarked: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onStyleSelected: (EnvelopeStyle) -> Unit,
    onPostmark: () -> Unit,
    onClearPostmark: () -> Unit,
    onRemoveEnvelope: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    fun hideThenRun(action: () -> Unit) {
        scope.launch {
            sheetState.hide()
            action()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ScreenBackgroundGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (currentStyle == null) "봉투 고르기" else "봉투 바꾸기",
                color = BrutalBlack,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "완성된 엽서를 봉투에 넣어볼까?",
                color = GraphiteAccent,
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EnvelopeStyle.entries.forEach { style ->
                    val selected = style == currentStyle

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(enabled = enabled) {
                                hideThenRun { onStyleSelected(style) }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .width(88.dp)
                                .aspectRatio(1.3f)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) SunsetGold else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(3.dp)
                        ) {
                            EnvelopeBack(
                                style = style,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            EnvelopeFrontPocket(
                                style = style,
                                topFraction = ENVELOPE_POCKET_TOP_FRACTION,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            EnvelopeFlap(
                                style = style,
                                peakHeightFraction = ENVELOPE_FLAP_RESTING_PEAK_FRACTION,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = style.label,
                            color = BrutalBlack,
                            fontSize = 11.sp,
                            fontWeight =
                                if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            if (currentStyle != null) {
                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = PaperDivider)
                Spacer(modifier = Modifier.height(12.dp))

                EnvelopeSheetActionRow(
                    icon = if (postmarked) Icons.Default.Refresh else Icons.Default.Check,
                    label = if (postmarked) "소인 다시 찍기" else "소인 찍기",
                    onClick = { hideThenRun(onPostmark) },
                    enabled = enabled
                )

                if (postmarked) {
                    Spacer(modifier = Modifier.height(10.dp))

                    EnvelopeSheetActionRow(
                        icon = Icons.Default.Close,
                        label = "소인 지우기",
                        onClick = { hideThenRun(onClearPostmark) },
                        enabled = enabled
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                EnvelopeSheetActionRow(
                    icon = Icons.Default.Unarchive,
                    label = "봉투에서 꺼내기",
                    onClick = { hideThenRun(onRemoveEnvelope) },
                    enabled = enabled,
                    emphasized = false,
                    tint = GalleryDangerRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EnvelopeSheetActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    emphasized: Boolean = true,
    tint: Color = GraphiteAccent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (emphasized) NeutralLight else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            color = BrutalBlack,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
