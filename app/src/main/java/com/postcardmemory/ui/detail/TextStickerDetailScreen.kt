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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
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
import com.postcardmemory.ui.components.DecorationPresetTile
import com.postcardmemory.ui.components.EditorEmptyHint
import com.postcardmemory.ui.components.EditorOutlineButton
import com.postcardmemory.ui.components.EditorQuietHint
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.PostcardCustomColorPicker
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
import com.postcardmemory.ui.theme.textStickerOutlineColors

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
    onEditTextSticker: (id: String, text: String) -> Unit,
    onColorSelected: (id: String, colorArgb: Long) -> Unit,
    onOutlineColorSelected: (id: String, outlineColorArgb: Long) -> Unit,
    onEnterCustomColor: () -> Unit,
    onCustomColorSelected: (id: String, colorArgb: Long) -> Unit,
    onEnterCustomOutlineColor: () -> Unit,
    onCustomOutlineColorSelected: (id: String, outlineColorArgb: Long) -> Unit,
    onDeleteTextSticker: (String) -> Unit,
    onUndoTextSticker: () -> Unit,
    onRedoTextSticker: () -> Unit,
    canUndoTextSticker: Boolean,
    canRedoTextSticker: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var fillColorCustomExpanded by remember { mutableStateOf(false) }
    var outlineColorCustomExpanded by remember { mutableStateOf(false) }

    val selectedTextSticker =
        textStickers.find { it.id == selectedTextStickerId }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((-4).dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
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

                DecorationPresetTile(
                    onClick = { onSelectTextSticker(sticker.id) },
                    enabled = enabled,
                    previewModifier = Modifier
                        .heightIn(min = 56.dp)
                        .widthIn(min = 56.dp, max = 96.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    selected = isSelected
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

            EditorOutlineButton(
                text = "문구 수정",
                icon = Icons.Default.Edit,
                onClick = { showEditDialog = true },
                enabled = enabled
            )

            Spacer(modifier = Modifier.height(14.dp))

            TextStickerColorPickerSection(
                title = "글자색",
                presetColors = textStickerColors,
                selectedColorArgb = selectedTextSticker.colorArgb,
                enabled = enabled,
                customPickerExpanded = fillColorCustomExpanded,
                onPresetSelected = { colorArgb ->
                    onColorSelected(selectedTextSticker.id, colorArgb)
                },
                onToggleCustomPicker = {
                    if (!fillColorCustomExpanded) {
                        onEnterCustomColor()
                    }
                    fillColorCustomExpanded = !fillColorCustomExpanded
                },
                onCustomColorSelected = { colorArgb ->
                    onCustomColorSelected(selectedTextSticker.id, colorArgb)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            TextStickerColorPickerSection(
                title = "테두리색",
                presetColors = textStickerOutlineColors,
                selectedColorArgb = selectedTextSticker.outlineColorArgb,
                enabled = enabled,
                customPickerExpanded = outlineColorCustomExpanded,
                onPresetSelected = { outlineColorArgb ->
                    onOutlineColorSelected(selectedTextSticker.id, outlineColorArgb)
                },
                onToggleCustomPicker = {
                    if (!outlineColorCustomExpanded) {
                        onEnterCustomOutlineColor()
                    }
                    outlineColorCustomExpanded = !outlineColorCustomExpanded
                },
                onCustomColorSelected = { outlineColorArgb ->
                    onCustomOutlineColorSelected(selectedTextSticker.id, outlineColorArgb)
                }
            )

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

            EditorQuietHint(
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

    if (showEditDialog && selectedTextSticker != null) {
        TextStickerEditDialog(
            initialText = selectedTextSticker.text,
            onDismiss = { showEditDialog = false },
            onConfirm = { text ->
                onEditTextSticker(selectedTextSticker.id, text)
                showEditDialog = false
            }
        )
    }
}

/**
 * 프리셋 스와치 옆에 "기타 색상" 진입점을 붙이고, 눌렀을 때 기존 배경색
 * 편집에서 쓰던 PostcardCustomColorPicker(hue/채도-명도)를 그대로 펼쳐서
 * 보여준다. 글자색·테두리색 두 곳에서 동일한 구조를 쓰므로 하나로 묶는다.
 *
 * 프리셋에 없는 색이 선택된 상태라면 진입점 스와치 자체가 그 색을 보여줘서
 * 사용자가 "지금 기타 색상이 적용 중"임을 알 수 있게 한다.
 */
@Composable
private fun TextStickerColorPickerSection(
    title: String,
    presetColors: List<Color>,
    selectedColorArgb: Long,
    enabled: Boolean,
    customPickerExpanded: Boolean,
    onPresetSelected: (Long) -> Unit,
    onToggleCustomPicker: () -> Unit,
    onCustomColorSelected: (Long) -> Unit
) {
    val isCustomColorActive =
        presetColors.none { preset ->
            (preset.toArgb().toLong() and 0xFFFFFFFFL) == selectedColorArgb
        }

    Text(
        text = title,
        color = BrutalBlack,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presetColors.forEach { color ->
            val colorArgb = color.toArgb().toLong() and 0xFFFFFFFFL
            val isColorSelected = selectedColorArgb == colorArgb

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    .clickable(enabled = enabled) {
                        onPresetSelected(colorArgb)
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                .clickable(enabled = enabled) {
                    onToggleCustomPicker()
                }
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        color = if (isCustomColorActive) Color(selectedColorArgb) else PaperField,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = BrutalBlack.copy(alpha = 0.35f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!isCustomColorActive) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "기타 색상",
                        tint = GraphiteAccent,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(
                        color = if (isCustomColorActive) SunsetGold else Color.Transparent,
                        shape = CircleShape
                    )
            )
        }
    }

    AnimatedVisibility(
        visible = customPickerExpanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            PostcardCustomColorPicker(
                selectedColorArgb = selectedColorArgb,
                enabled = enabled,
                onColorSelected = onCustomColorSelected,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    textStickerColors.forEach { color ->
                        val isColorSelected = selectedColor == color

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                .clickable {
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

/**
 * 이미 붙여놓은 텍스트 스티커의 문구만 고쳐 쓰는 Dialog. 위치·크기·회전·
 * 색상은 여기서 건드리지 않고 호출부에서 동일 id의 text 필드만 갱신한다.
 * 색상 선택 UI는 이미 편집 패널에 있으므로 여기서는 다시 만들지 않는다.
 */
@Composable
private fun TextStickerEditDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (text: String) -> Unit
) {
    var textDraft by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        titleContentColor = InkPrimary,
        textContentColor = InkPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "문구 수정",
                color = InkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "문구를 다시 적어봐.",
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = textDraft.isNotBlank(),
                onClick = { onConfirm(textDraft) }
            ) {
                Text(
                    text = "저장",
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
