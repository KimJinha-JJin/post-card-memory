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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.EditorActionDivider
import com.postcardmemory.ui.components.EditorFlatPresetTile
import com.postcardmemory.ui.components.EditorQuietHint
import com.postcardmemory.ui.components.EditorTextAction
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.LABEL_STICKER_BASE_FONT_SIZE_SP
import com.postcardmemory.ui.components.LabelStickerContent
import com.postcardmemory.ui.components.PostcardCustomColorPicker
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SunsetGold

/** 목록 칩에서 쓰는 축소 글자 크기. 실제로 붙는 라벨은 항상 기본 크기로 만들어진다. */
private const val LABEL_STICKER_CHIP_FONT_SIZE_SP = 11f

/** 문구를 아직 한 글자도 넣지 않았을 때 미리보기에 띄우는 예시. 실제로 저장되지는 않는다. */
private const val LABEL_STICKER_PREVIEW_PLACEHOLDER = "LABEL"

/**
 * "스티커" 탭 안에 있는 라벨 스티커 전용 섹션. 사진 스티커·텍스트 스티커
 * 패널 아래에 이어 붙는 세 번째 섹션으로, 하단 탭을 새로 늘리지 않는다는
 * 기존 방침(TextStickerPickerPanel과 동일)을 따른다.
 *
 * 텍스트 스티커 패널과 달리 글자색·테두리색·자유색 진입점이 없다.
 * 사용자가 정하는 것은 문구와 테이프 종류 두 가지뿐이고, 나머지는 라벨
 * 렌더 규칙이 결정한다 — 자유도를 줄이는 것이 이 기능의 정체성이다.
 */
@Composable
fun LabelStickerPickerPanel(
    labelStickers: List<LabelStickerItem>,
    selectedLabelStickerId: String?,
    onSelectLabelSticker: (String) -> Unit,
    onAddLabelSticker: (
        text: String,
        style: LabelTapeStyle,
        customTapeColorArgb: Long?
    ) -> Unit,
    onEditLabelSticker: (
        id: String,
        text: String,
        style: LabelTapeStyle,
        customTapeColorArgb: Long?
    ) -> Unit,
    onDeleteLabelSticker: (String) -> Unit,
    onUndoLabelSticker: () -> Unit,
    onRedoLabelSticker: () -> Unit,
    canUndoLabelSticker: Boolean,
    canRedoLabelSticker: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val selectedLabelSticker =
        labelStickers.find { it.id == selectedLabelStickerId }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((-4).dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorUndoRedoButtons(
                canUndo = canUndoLabelSticker,
                canRedo = canRedoLabelSticker,
                onUndo = onUndoLabelSticker,
                onRedo = onRedoLabelSticker,
                enabled = enabled,
                undoContentDescription = "실행 취소",
                redoContentDescription = "다시 실행"
            )

            Text(
                text = "${labelStickers.size}개",
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            labelStickers.forEach { labelSticker ->
                val isSelected = labelSticker.id == selectedLabelStickerId

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 목록 칩 자체를 진짜 라벨로 그린다 — 글자만 나열하는
                    // 텍스트 스티커 목록과 한눈에 구분된다.
                    Box(
                        modifier = Modifier
                            .clickable(enabled = enabled) {
                                onSelectLabelSticker(labelSticker.id)
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        LabelStickerContent(
                            text = labelSticker.text,
                            style = labelSticker.style,
                            fontSizeSp = LABEL_STICKER_CHIP_FONT_SIZE_SP,
                            customTapeColorArgb = labelSticker.customTapeColorArgb
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

            EditorFlatPresetTile(
                onClick = { showCreateDialog = true },
                enabled = enabled,
                previewModifier = Modifier.size(56.dp)
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

        if (selectedLabelSticker != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorTextAction(
                    text = "수정",
                    onClick = { showEditDialog = true },
                    enabled = enabled
                )

                EditorActionDivider()

                EditorTextAction(
                    text = "삭제",
                    onClick = { onDeleteLabelSticker(selectedLabelSticker.id) },
                    enabled = enabled,
                    contentColor = GalleryDangerRed
                )
            }
        } else if (labelStickers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorQuietHint(
                text = "편집할 라벨을 선택해."
            )
        }
    }

    if (showCreateDialog) {
        LabelStickerCreateDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { text, style, customTapeColorArgb ->
                onAddLabelSticker(text, style, customTapeColorArgb)
                showCreateDialog = false
            }
        )
    }

    if (showEditDialog && selectedLabelSticker != null) {
        LabelStickerEditDialog(
            initialText = selectedLabelSticker.text,
            initialStyle = selectedLabelSticker.style,
            initialCustomTapeColorArgb = selectedLabelSticker.customTapeColorArgb,
            enabled = enabled,
            onDismiss = { showEditDialog = false },
            onConfirm = { text, style, customTapeColorArgb ->
                onEditLabelSticker(
                    selectedLabelSticker.id,
                    text,
                    style,
                    customTapeColorArgb
                )
                showEditDialog = false
            }
        )
    }
}

/**
 * 테이프 종류 선택 줄. 색상환이 아니라 실제 테이프 조각을 늘어놓은 모양이라,
 * "색을 고른다"기보다 "기계에 넣을 테이프를 고른다"에 가깝게 읽힌다.
 *
 * 맨 끝의 "기타" 칸은 기존 배경색·텍스트 스티커 자유색에서 쓰던
 * PostcardCustomColorPicker를 그대로 펼친다(새 피커를 만들지 않는다).
 * onToggleCustomPicker가 null이면 기타 색상 없이 프리셋만 보여주는
 * 모드로, 생성 다이얼로그처럼 좁은 곳에서 쓴다.
 */
@Composable
private fun LabelTapeStyleRow(
    selectedStyle: LabelTapeStyle,
    enabled: Boolean,
    onStyleSelected: (LabelTapeStyle) -> Unit,
    customTapeColorArgb: Long? = null,
    customPickerExpanded: Boolean = false,
    onToggleCustomPicker: (() -> Unit)? = null,
    onCustomColorSelected: ((Long) -> Unit)? = null
) {
    val isCustomSelected = selectedStyle == LabelTapeStyle.CUSTOM

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presetLabelTapeStyles.forEach { style ->
            LabelTapeSwatch(
                label = style.label,
                tapeColor = Color(style.baseColorArgb),
                edgeColor = Color(style.edgeColorArgb),
                isSelected = !isCustomSelected && style == selectedStyle,
                enabled = enabled,
                onClick = { onStyleSelected(style) }
            )
        }

        if (onToggleCustomPicker != null) {
            val customPalette =
                labelTapePalette(
                    style = LabelTapeStyle.CUSTOM,
                    customTapeColorArgb = customTapeColorArgb
                )

            LabelTapeSwatch(
                label = "🎨 기타",
                // 이미 기타 색상이 적용 중이면 진입 칸 자체가 그 색을 보여줘서
                // 지금 어떤 색이 쓰이는지 알 수 있게 한다.
                tapeColor =
                    if (isCustomSelected) {
                        Color(customPalette.baseColorArgb)
                    } else {
                        PaperField
                    },
                edgeColor =
                    if (isCustomSelected) {
                        Color(customPalette.edgeColorArgb)
                    } else {
                        PaperDivider
                    },
                isSelected = isCustomSelected,
                enabled = enabled,
                onClick = onToggleCustomPicker
            )
        }
    }

    // AlertDialog 안에서는 Dialog 자체가 별도 Window라 AnimatedVisibility로
    // 높이를 애니메이션하면 매 프레임 Window relayout이 걸려 버벅인다(실기기
    // 확인됨). 이 줄은 이제 Create/Edit Dialog 안에서만 쓰이므로 애니메이션
    // 없이 즉시 표시/숨김으로 바꾼다.
    if (onToggleCustomPicker != null && onCustomColorSelected != null) {
        if (customPickerExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                PostcardCustomColorPicker(
                    // 아직 기타 색상으로 바꾸기 전이라면 지금 붙어 있는
                    // 프리셋 색에서 출발한다 — 피커를 열자마자 엉뚱한 색이
                    // 잡혀 있지 않게.
                    selectedColorArgb =
                        labelTapePalette(
                            style = selectedStyle,
                            customTapeColorArgb = customTapeColorArgb
                        ).baseColorArgb,
                    enabled = enabled,
                    onColorSelected = onCustomColorSelected,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** 테이프 조각 하나를 나타내는 작은 스와치. 프리셋과 "기타" 진입 칸이 같은 모양을 쓴다. */
@Composable
private fun LabelTapeSwatch(
    label: String,
    tapeColor: Color,
    edgeColor: Color,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(18.dp)
                .background(
                    color = tapeColor,
                    shape = RoundedCornerShape(2.dp)
                )
                .border(
                    width = 1.dp,
                    color = edgeColor,
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = BrutalBlack,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

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

/**
 * "라벨 뽑기" 진입 시 뜨는 생성 다이얼로그. 입력창부터 띄우는 텍스트 스티커
 * 다이얼로그와 달리, 맨 위에 지금 뽑히는 라벨을 실물 크기 감각으로 먼저
 * 보여주고 그 아래에서 문구와 테이프를 정한다 — 글을 쓰는 것이 아니라
 * 물건 하나를 만들어 내는 흐름이다. 테이프 선택은 EditDialog와 동일하게
 * LabelTapeStyleRow를 재사용해 프리셋뿐 아니라 기타(커스텀) 색상도 생성
 * 시점에 바로 고를 수 있다.
 *
 * 입력값은 저장 여부 판단(isBlank)에만 쓰고 실제 저장 문자열은 trim하지
 * 않는다(텍스트 스티커와 동일 정책). 다만 개행과 탭은 줄·필드 단위로 읽는
 * 저장 형식과 충돌하므로 입력 자체를 받지 않는다.
 */
@Composable
private fun LabelStickerCreateDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        text: String,
        style: LabelTapeStyle,
        customTapeColorArgb: Long?
    ) -> Unit
) {
    var textDraft by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf(LabelTapeStyle.BLACK) }
    var customTapeColorArgb by remember { mutableStateOf<Long?>(null) }
    var customTapeColorExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        titleContentColor = InkPrimary,
        textContentColor = InkPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "라벨 뽑기",
                color = InkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                // 긴 문구를 넣어도 미리보기가 잘리지 않도록 가로 스크롤을 둔다 —
                // 라벨 폭이 문구에 따라 늘어나는 것 자체를 여기서 확인해야 한다.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LabelStickerContent(
                        text = textDraft.ifBlank { LABEL_STICKER_PREVIEW_PLACEHOLDER },
                        style = selectedStyle,
                        // 축소하지 않고 실제로 붙을 크기 그대로 보여준다.
                        fontSizeSp = LABEL_STICKER_BASE_FONT_SIZE_SP,
                        customTapeColorArgb = customTapeColorArgb
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { newValue ->
                        if (
                            newValue.length <= LABEL_STICKER_MAX_LENGTH &&
                            !newValue.contains('\n') &&
                            !newValue.contains('\t')
                        ) {
                            textDraft = newValue
                        }
                    },
                    placeholder = {
                        Text("SUMMER")
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

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${textDraft.length} / $LABEL_STICKER_MAX_LENGTH · 짧은 한 줄만 들어가.",
                    color = InkSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "테이프 색",
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LabelTapeStyleRow(
                    selectedStyle = selectedStyle,
                    customTapeColorArgb = customTapeColorArgb,
                    enabled = true,
                    customPickerExpanded = customTapeColorExpanded,
                    onStyleSelected = { style ->
                        customTapeColorExpanded = false
                        selectedStyle = style
                    },
                    onToggleCustomPicker = {
                        customTapeColorExpanded = !customTapeColorExpanded
                    },
                    onCustomColorSelected = { colorArgb ->
                        selectedStyle = LabelTapeStyle.CUSTOM
                        customTapeColorArgb = colorArgb
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = textDraft.isNotBlank(),
                onClick = {
                    onConfirm(textDraft, selectedStyle, customTapeColorArgb)
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

/**
 * 이미 뽑은 라벨의 문구·테이프 색/스타일을 한 창에서 고쳐 쓰는 Dialog.
 * 위치·회전은 여기서 건드리지 않고, 저장을 눌렀을 때만 호출부가 동일 id에
 * text/style/customTapeColorArgb를 한 번에 반영한다. 테이프 선택 UI는
 * 기존 하단 Property가 쓰던 LabelTapeStyleRow를 그대로 재사용하되, 값의
 * 출처만 ViewModel에서 이 Dialog의 local state로 바꿨다.
 */
@Composable
private fun LabelStickerEditDialog(
    initialText: String,
    initialStyle: LabelTapeStyle,
    initialCustomTapeColorArgb: Long?,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        text: String,
        style: LabelTapeStyle,
        customTapeColorArgb: Long?
    ) -> Unit
) {
    var textDraft by remember { mutableStateOf(initialText) }
    var styleDraft by remember { mutableStateOf(initialStyle) }
    var customTapeColorArgbDraft by remember {
        mutableStateOf(initialCustomTapeColorArgb)
    }
    var customTapeColorExpanded by remember { mutableStateOf(false) }

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LabelStickerContent(
                        text = textDraft.ifBlank { LABEL_STICKER_PREVIEW_PLACEHOLDER },
                        style = styleDraft,
                        fontSizeSp = LABEL_STICKER_BASE_FONT_SIZE_SP,
                        customTapeColorArgb = customTapeColorArgbDraft
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = textDraft,
                    onValueChange = { newValue ->
                        if (
                            newValue.length <= LABEL_STICKER_MAX_LENGTH &&
                            !newValue.contains('\n') &&
                            !newValue.contains('\t')
                        ) {
                            textDraft = newValue
                        }
                    },
                    placeholder = {
                        Text("SUMMER")
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

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "${textDraft.length} / $LABEL_STICKER_MAX_LENGTH · 짧은 한 줄만 들어가.",
                    color = InkSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "테이프 색",
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LabelTapeStyleRow(
                    selectedStyle = styleDraft,
                    customTapeColorArgb = customTapeColorArgbDraft,
                    enabled = enabled,
                    customPickerExpanded = customTapeColorExpanded,
                    onStyleSelected = { style ->
                        customTapeColorExpanded = false
                        styleDraft = style
                    },
                    onToggleCustomPicker = {
                        customTapeColorExpanded = !customTapeColorExpanded
                    },
                    onCustomColorSelected = { colorArgb ->
                        styleDraft = LabelTapeStyle.CUSTOM
                        customTapeColorArgbDraft = colorArgb
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = textDraft.isNotBlank(),
                onClick = {
                    onConfirm(textDraft, styleDraft, customTapeColorArgbDraft)
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
