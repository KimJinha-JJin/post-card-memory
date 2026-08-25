package com.postcardmemory.ui.detail

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Palette
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
import com.postcardmemory.ui.components.EditorActionDivider
import com.postcardmemory.ui.components.EditorQuietHint
import com.postcardmemory.ui.components.EditorTextAction
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
    onAddTextSticker: (text: String, colorArgb: Long, outlineColorArgb: Long) -> Unit,
    onEditTextSticker: (
        id: String,
        text: String,
        colorArgb: Long,
        outlineColorArgb: Long
    ) -> Unit,
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
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = GraphiteAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "추가",
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorTextAction(
                    text = "수정",
                    onClick = { showEditDialog = true },
                    enabled = enabled
                )

                EditorActionDivider()

                EditorTextAction(
                    text = "삭제",
                    onClick = { onDeleteTextSticker(selectedTextSticker.id) },
                    enabled = enabled,
                    contentColor = GalleryDangerRed
                )
            }
        } else if (textStickers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorQuietHint(
                text = "편집할 텍스트 스티커를 선택해."
            )
        }
    }

    if (showAddDialog) {
        TextStickerAddDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { text, colorArgb, outlineColorArgb ->
                onAddTextSticker(text, colorArgb, outlineColorArgb)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && selectedTextSticker != null) {
        TextStickerEditDialog(
            initialText = selectedTextSticker.text,
            initialOutlineColorArgb = selectedTextSticker.outlineColorArgb,
            enabled = enabled,
            onDismiss = { showEditDialog = false },
            onConfirm = { text, colorArgb, outlineColorArgb ->
                onEditTextSticker(
                    selectedTextSticker.id,
                    text,
                    colorArgb,
                    outlineColorArgb
                )
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

    // AlertDialog 안에서는 Dialog 자체가 별도 Window라 AnimatedVisibility로
    // 높이를 애니메이션하면 매 프레임 Window relayout이 걸려 버벅인다(실기기
    // 확인됨). 이 섹션은 이제 EditDialog 안에서만 쓰이므로 애니메이션 없이
    // 즉시 표시/숨김으로 바꾼다.
    if (customPickerExpanded) {
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
 * "추가" 타일 진입 시 뜨는 입력 다이얼로그. 기존 "글귀 남기기" AlertDialog와
 * 동일한 스타일(PaperSurface/PaperField/SunsetGold)을 따른다. 사용자는
 * 테두리색만 고르고(프리셋+기타, TextStickerColorPickerSection 재사용), 글자
 * 채움색은 라벨의 테이프→문자색과 동일한 규칙(labelStickerTextColorArgbFor)으로
 * 테두리색의 밝기에서 자동으로 골라 항상 잘 읽히게 한다 — 글자색을 따로
 * 고르게 하지 않는다.
 *
 * 입력값은 저장 여부 판단(isBlank)에만 쓰이고, 실제로 저장하는 문자열은
 * trim하지 않는다 — 앞뒤 공백도 사용자가 입력한 표현의 일부일 수 있다.
 */
@Composable
private fun TextStickerAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, colorArgb: Long, outlineColorArgb: Long) -> Unit
) {
    var textDraft by remember { mutableStateOf("") }
    var outlineColorArgbDraft by remember {
        mutableStateOf(
            textStickerOutlineColors.first().toArgb().toLong() and 0xFFFFFFFFL
        )
    }
    var outlineColorCustomExpanded by remember { mutableStateOf(false) }

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

                TextStickerColorPickerSection(
                    title = "테두리색",
                    presetColors = textStickerOutlineColors,
                    selectedColorArgb = outlineColorArgbDraft,
                    enabled = true,
                    customPickerExpanded = outlineColorCustomExpanded,
                    onPresetSelected = { outlineColorArgb ->
                        outlineColorArgbDraft = outlineColorArgb
                    },
                    onToggleCustomPicker = {
                        outlineColorCustomExpanded = !outlineColorCustomExpanded
                    },
                    onCustomColorSelected = { outlineColorArgb ->
                        outlineColorArgbDraft = outlineColorArgb
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = textDraft.isNotBlank(),
                onClick = {
                    onConfirm(
                        textDraft,
                        labelStickerTextColorArgbFor(outlineColorArgbDraft),
                        outlineColorArgbDraft
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
 * 이미 붙여놓은 텍스트 스티커의 문구·테두리색을 한 창에서 고쳐 쓰는 Dialog.
 * 위치·크기·회전은 여기서 건드리지 않고, 저장을 눌렀을 때만 호출부가 동일
 * id에 text/colorArgb/outlineColorArgb를 한 번에 반영한다. 글자 채움색은
 * 사용자가 고르지 않고 라벨의 테이프→문자색과 동일한 규칙
 * (labelStickerTextColorArgbFor)으로 테두리색의 밝기에서 자동으로 정해진다.
 * 색상 선택 UI는 기존 하단 Property가 쓰던 TextStickerColorPickerSection을
 * 그대로 재사용하되, 값의 출처만 ViewModel에서 이 Dialog의 local state로
 * 바꿨다 — PostcardCustomColorPicker는 selectedColorArgb/onColorSelected만
 * 지키면 되는 controlled component라 그대로 동작한다.
 */
@Composable
private fun TextStickerEditDialog(
    initialText: String,
    initialOutlineColorArgb: Long,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (text: String, colorArgb: Long, outlineColorArgb: Long) -> Unit
) {
    var textDraft by remember { mutableStateOf(initialText) }
    var outlineColorArgbDraft by remember { mutableStateOf(initialOutlineColorArgb) }
    var outlineColorCustomExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        titleContentColor = InkPrimary,
        textContentColor = InkPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "수정",
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

                Spacer(modifier = Modifier.height(16.dp))

                TextStickerColorPickerSection(
                    title = "테두리색",
                    presetColors = textStickerOutlineColors,
                    selectedColorArgb = outlineColorArgbDraft,
                    enabled = enabled,
                    customPickerExpanded = outlineColorCustomExpanded,
                    onPresetSelected = { outlineColorArgb ->
                        outlineColorArgbDraft = outlineColorArgb
                    },
                    onToggleCustomPicker = {
                        outlineColorCustomExpanded = !outlineColorCustomExpanded
                    },
                    onCustomColorSelected = { outlineColorArgb ->
                        outlineColorArgbDraft = outlineColorArgb
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = textDraft.isNotBlank(),
                onClick = {
                    onConfirm(
                        textDraft,
                        labelStickerTextColorArgbFor(outlineColorArgbDraft),
                        outlineColorArgbDraft
                    )
                }
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
