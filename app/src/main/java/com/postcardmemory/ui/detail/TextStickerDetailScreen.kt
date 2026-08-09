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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.EditorEmptyHint
import com.postcardmemory.ui.components.EditorOutlineButton
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.BrutalWhite
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.textStickerColors

/** 텍스트 스티커 문구의 최대 길이. surrogate pair를 자르지 않도록 잘라내지 않고, 초과분은 아예 반영하지 않는다. */
const val TEXT_STICKER_MAX_LENGTH = 50

/**
 * "스티커" 탭 안에 있는 텍스트 스티커 전용 섹션. 기존 사진 스티커 패널과
 * 나란히 놓이되 완전히 독립된 목록/선택 상태를 다룬다. 새 하단 탭을
 * 만들지 않는다는 요구에 따라, PhotoStickerPickerPanel 밑에 이어 붙이는
 * 두 번째 섹션으로 둔다.
 */
@Composable
fun TextStickerPickerPanel(
    textStickers: List<TextStickerItem>,
    selectedTextStickerId: String?,
    onSelectTextSticker: (String) -> Unit,
    onAddTextSticker: (text: String, colorArgb: Long) -> Unit,
    onColorSelected: (id: String, colorArgb: Long) -> Unit,
    onDeleteTextSticker: (String) -> Unit,
    onUndoTextSticker: () -> Unit,
    onRedoTextSticker: () -> Unit,
    canUndoTextSticker: Boolean,
    canRedoTextSticker: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    val selectedTextSticker =
        textStickers.find { it.id == selectedTextStickerId }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "텍스트 스티커",
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((-4).dp)
            ) {
                EditorUndoRedoButtons(
                    canUndo = canUndoTextSticker,
                    canRedo = canRedoTextSticker,
                    onUndo = onUndoTextSticker,
                    onRedo = onRedoTextSticker,
                    enabled = enabled,
                    undoContentDescription = "실행 취소",
                    redoContentDescription = "다시 실행"
                )

                Text(
                    text = "${textStickers.size}개",
                    color = GraphiteAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            textStickers.forEach { sticker ->
                val isSelected = sticker.id == selectedTextStickerId

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 56.dp)
                            .widthIn(min = 56.dp, max = 96.dp)
                            .background(
                                color = BrutalWhite,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = enabled) {
                                onSelectTextSticker(sticker.id)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sticker.text,
                            color = Color(sticker.colorArgb),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Box(
                        modifier = Modifier
                            .height(2.dp)
                            .widthIn(min = 24.dp)
                            .background(
                                color = if (isSelected) SunsetGold else Color.Transparent,
                                shape = RoundedCornerShape(1.dp)
                            )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        color = BrutalWhite,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(enabled = enabled) {
                        showAddDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = GraphiteAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Aa 텍스트",
                        color = BrutalBlack,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (selectedTextSticker != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "선택한 텍스트 스티커",
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "글자색",
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                textStickerColors.forEach { color ->
                    val colorArgb = color.toArgb().toLong() and 0xFFFFFFFFL
                    val isColorSelected = selectedTextSticker.colorArgb == colorArgb

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(enabled = enabled) {
                            onColorSelected(selectedTextSticker.id, colorArgb)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(
                                    color = color,
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
                                    color = if (isColorSelected) SunsetGold else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            EditorOutlineButton(
                text = "삭제",
                icon = Icons.Default.Delete,
                onClick = { onDeleteTextSticker(selectedTextSticker.id) },
                enabled = enabled,
                contentColor = GalleryDangerRed,
                borderColor = GalleryDangerRed
            )
        } else if (textStickers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorEmptyHint(
                text = "편집할 텍스트 스티커를 선택해."
            )
        }

        if (textStickers.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorEmptyHint(
                text = "아직 추가한 텍스트 스티커가 없어."
            )
        }
    }

    if (showAddDialog) {
        TextStickerAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { text, colorArgb ->
                onAddTextSticker(text, colorArgb)
                showAddDialog = false
            }
        )
    }
}

/**
 * "Aa 텍스트" 진입 시 뜨는 입력 다이얼로그. 기존 "글귀 남기기" AlertDialog와
 * 동일한 스타일(PaperSurface/PaperField/SunsetGold)을 따르고, 색상은
 * 도장 잉크색 스와치와 동일한 형태의 고정 팔레트를 재사용한다.
 *
 * 입력값은 저장 여부 판단(isBlank)에만 쓰이고, 실제로 저장하는 문자열은
 * trim하지 않는다 — 앞뒤 공백도 사용자가 입력한 표현의 일부일 수 있다.
 */
@Composable
private fun TextStickerAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, colorArgb: Long) -> Unit
) {
    var textDraft by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(textStickerColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        titleContentColor = InkPrimary,
        textContentColor = InkPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "텍스트 스티커",
                color = InkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "붙이고 싶은 말을 적어봐.",
                    color = InkSecondary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { newValue ->
                        if (
                            newValue.length <= TEXT_STICKER_MAX_LENGTH &&
                            !newValue.contains('\n')
                        ) {
                            textDraft = newValue
                        }
                    },
                    placeholder = {
                        Text("( ˶ˆᗜˆ˵ )")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = PaperField,
                        unfocusedContainerColor = PaperField,
                        focusedBorderColor = SunsetGold,
                        unfocusedBorderColor = PaperDivider,
                        focusedLabelColor = SunsetGold,
                        unfocusedLabelColor = InkSecondary,
                        focusedTextColor = InkPrimary,
                        unfocusedTextColor = InkPrimary,
                        focusedPlaceholderColor = InkSecondary,
                        unfocusedPlaceholderColor = InkSecondary,
                        cursorColor = SunsetGold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "글자색",
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    textStickerColors.forEach { color ->
                        val isColorSelected = selectedColor == color

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedColor = color
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(
                                        color = color,
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
                                        color = if (isColorSelected) SunsetGold else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = textDraft.isNotBlank(),
                onClick = {
                    onConfirm(
                        textDraft,
                        selectedColor.toArgb().toLong() and 0xFFFFFFFFL
                    )
                }
            ) {
                Text(
                    text = "추가",
                    color = if (textDraft.isNotBlank()) SunsetGold else InkSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "취소",
                    color = InkSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
