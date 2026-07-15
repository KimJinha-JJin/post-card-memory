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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.EditorEmptyHint
import com.postcardmemory.ui.components.EditorOutlineButton
import com.postcardmemory.ui.components.EditorSlider
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.SealInkWhite
import com.postcardmemory.ui.theme.SoftGray
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.sealInkColors
import kotlin.math.roundToInt

@Composable
fun SealPickerPanel(
    photoSeals: List<PostcardSealItem>,
    selectedSealId: String?,
    onSelectSeal: (String) -> Unit,
    onAddSeal: (SealType) -> Unit,
    onDeleteSeal: (String) -> Unit,
    onScaleChanged: (String, Float) -> Unit,
    onScaleChangeFinished: () -> Unit,
    onRotateBy: (String, Float) -> Unit,
    onColorSelected: (String, Long) -> Unit,
    onUndoSeal: () -> Unit,
    onRedoSeal: () -> Unit,
    canUndoSeal: Boolean,
    canRedoSeal: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedSeal =
        photoSeals.find { it.id == selectedSealId }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "도장",
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
            ) {
                EditorUndoRedoButtons(
                    canUndo = canUndoSeal,
                    canRedo = canRedoSeal,
                    onUndo = onUndoSeal,
                    onRedo = onRedoSeal,
                    enabled = enabled,
                    undoContentDescription = "실행 취소",
                    redoContentDescription = "다시 실행"
                )

                Text(
                    text = "${photoSeals.size}개",
                    color = GraphiteAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "새 도장",
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
            SealType.entries.forEach { type ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = enabled) {
                            onAddSeal(type)
                        }
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = BrutalWhite,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(6.dp)
                    ) {
                        SealPreviewContent(
                            type = type,
                            color = BrutalBlack,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = type.label,
                        color = BrutalBlack,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (photoSeals.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorEmptyHint(
                text = "아직 추가한 도장이 없어."
            )

            return@Column
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "추가한 도장",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            photoSeals.forEach { seal ->
                val isSelected = seal.id == selectedSealId
                val isWhiteInk =
                    (seal.colorArgb == (SealInkWhite.toArgb().toLong() and 0xFFFFFFFFL))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = if (isWhiteInk) NeutralLight else BrutalWhite,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 0.dp,
                            color = if (isSelected) SunsetGold else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) {
                            onSelectSeal(seal.id)
                        }
                        .padding(6.dp)
                ) {
                    SealPreviewContent(
                        type = seal.type,
                        color = Color(seal.colorArgb),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (selectedSeal == null) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorEmptyHint(
                text = "편집할 도장을 선택해."
            )

            return@Column
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "선택한 도장",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "크기",
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${(selectedSeal.scale * 100f).roundToInt()}%",
                color = GraphiteAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        EditorSlider(
            value = selectedSeal.scale,
            onValueChange = { newValue ->
                onScaleChanged(selectedSeal.id, newValue)
            },
            onValueChangeFinished = onScaleChangeFinished,
            valueRange = 0.5f..3f,
            enabled = enabled,
            inactiveTrackColor = SoftGray,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "색상",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sealInkColors.forEach { inkColor ->
                val inkArgb =
                    inkColor.toArgb().toLong() and 0xFFFFFFFFL
                val isColorSelected =
                    selectedSeal.colorArgb == inkArgb

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            color = inkColor,
                            shape = CircleShape
                        )
                        .border(
                            width = if (isColorSelected) 2.dp else 1.5.dp,
                            color = if (isColorSelected) {
                                SunsetGold
                            } else {
                                BrutalBlack.copy(alpha = 0.35f)
                            },
                            shape = CircleShape
                        )
                        .clickable(enabled = enabled) {
                            onColorSelected(selectedSeal.id, inkArgb)
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "회전",
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${selectedSeal.rotationDegrees.roundToInt()}°",
                color = GraphiteAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorOutlineButton(
                text = "−15°",
                onClick = { onRotateBy(selectedSeal.id, -15f) },
                enabled = enabled
            )

            EditorOutlineButton(
                text = "+15°",
                onClick = { onRotateBy(selectedSeal.id, 15f) },
                enabled = enabled
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        EditorOutlineButton(
            text = "삭제",
            icon = Icons.Default.Delete,
            onClick = { onDeleteSeal(selectedSeal.id) },
            enabled = enabled,
            contentColor = GalleryDangerRed,
            borderColor = GalleryDangerRed
        )
    }
}
