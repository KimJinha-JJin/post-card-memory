package com.postcardmemory.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.SealInkWhite
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.sealInkColors

@Composable
fun SealPickerPanel(
    photoSeals: List<PostcardSealItem>,
    selectedSealId: String?,
    onSelectSeal: (String) -> Unit,
    onAddSeal: (SealType) -> Unit,
    onDeleteSeal: (String) -> Unit,
    onColorSelected: (String, Long) -> Unit,
    isSelectedSealOutOfBounds: Boolean,
    onRestoreSealPosition: () -> Unit,
    onUndoSeal: () -> Unit,
    onRedoSeal: () -> Unit,
    canUndoSeal: Boolean,
    canRedoSeal: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val selectedSeal =
        photoSeals.find { it.id == selectedSealId }

    var isAddListExpanded by remember {
        mutableStateOf(photoSeals.size < MAX_SEAL_COUNT)
    }

    LaunchedEffect(photoSeals.size) {
        if (photoSeals.size >= MAX_SEAL_COUNT) {
            isAddListExpanded = false
        }
    }

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
                    text = "${photoSeals.size} / ${MAX_SEAL_COUNT}개",
                    color = GraphiteAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = {
                isAddListExpanded = !isAddListExpanded
            },
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(
                contentColor = BrutalBlack
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.size(6.dp))

            Text(
                text =
                    if (isAddListExpanded) {
                        "새 도장 목록 닫기"
                    } else {
                        "새 도장 추가"
                    },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        AnimatedVisibility(
            visible = isAddListExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                SealTypeTileRow(
                    types = SealType.entries,
                    enabled = enabled,
                    onAddSeal = onAddSeal
                )
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
            verticalAlignment = Alignment.Top
        ) {
            photoSeals.forEach { seal ->
                val isSelected = seal.id == selectedSealId
                val isWhiteInk =
                    (seal.colorArgb == (SealInkWhite.toArgb().toLong() and 0xFFFFFFFFL))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = if (isWhiteInk) NeutralLight else BrutalWhite,
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

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .width(24.dp)
                            .background(
                                color = if (isSelected) SunsetGold else Color.Transparent,
                                shape = RoundedCornerShape(1.dp)
                            )
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
            sealInkColors.forEach { inkColor ->
                val inkArgb =
                    inkColor.toArgb().toLong() and 0xFFFFFFFFL
                val isColorSelected =
                    selectedSeal.colorArgb == inkArgb

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(enabled = enabled) {
                        onColorSelected(selectedSeal.id, inkArgb)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                color = inkColor,
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

        if (isSelectedSealOutOfBounds) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorOutlineButton(
                text = "위치 되돌리기",
                onClick = onRestoreSealPosition,
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

@Composable
private fun SealTypeTileRow(
    types: List<SealType>,
    enabled: Boolean,
    onAddSeal: (SealType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        types.forEach { type ->
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
}
