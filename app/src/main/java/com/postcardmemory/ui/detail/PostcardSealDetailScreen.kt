package com.postcardmemory.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.DecorationPresetTile
import com.postcardmemory.ui.components.EditorActionDivider
import com.postcardmemory.ui.components.EditorFlatPresetTile
import com.postcardmemory.ui.components.EditorQuietHint
import com.postcardmemory.ui.components.EditorTextAction
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.NeutralLight
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SealInkWhite
import com.postcardmemory.ui.theme.SunsetGold
import com.postcardmemory.ui.theme.sealSelectableInkColors

@Composable
fun SealPickerPanel(
    photoSeals: List<PostcardSealItem>,
    selectedSealId: String?,
    onSelectSeal: (String) -> Unit,
    onAddSeal: (SealType, Long) -> Unit,
    onDeleteSeal: (String) -> Unit,
    onEditSeal: (String, SealType, Long) -> Unit,
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

    val canAddSeal = photoSeals.size < MAX_SEAL_COUNT

    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy((-4).dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
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

        Spacer(modifier = Modifier.height(12.dp))

        // 붙인 도장 목록과 `+ 추가`를 한 줄에 둔다(라벨 스티커·마스킹테이프와
        // 같은 문법). 목록이 비어 있어도 이 줄과 `+ 추가`의 자리는 그대로라서
        // "아직 없다"는 별도 안내 상자 없이도 무엇을 눌러야 할지 드러난다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            photoSeals.forEach { seal ->
                val isSelected = seal.id == selectedSealId
                // SealPreviewContent는 도장을 채움 없는 얇은 선(Stroke)으로만
                // 그린다. 흰 잉크는 밝은 배경 위에서 선 자체가 안 보이므로,
                // 신규 선택지에서는 제외해도(sealSelectableInkColors) 이미
                // 저장된 흰 잉크 도장은 계속 보여야 해서 이 타일만은
                // 배경(NeutralLight)을 남겨 legacy 데이터를 보호한다.
                val isWhiteInk =
                    (seal.colorArgb == (SealInkWhite.toArgb().toLong() and 0xFFFFFFFFL))

                DecorationPresetTile(
                    onClick = { onSelectSeal(seal.id) },
                    enabled = enabled,
                    previewModifier = Modifier.size(56.dp),
                    backgroundColor = if (isWhiteInk) NeutralLight else Color.Transparent,
                    selected = isSelected
                ) {
                    SealPreviewContent(
                        type = seal.type,
                        color = Color(seal.colorArgb),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            EditorFlatPresetTile(
                onClick = {
                    if (canAddSeal) {
                        showCreateDialog = true
                    }
                },
                enabled = enabled && canAddSeal,
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

        if (!canAddSeal) {
            Spacer(modifier = Modifier.height(6.dp))

            EditorQuietHint(
                text = "도장은 최대 ${MAX_SEAL_COUNT}개까지 추가할 수 있어."
            )
        }

        if (selectedSeal != null) {
            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorTextAction(
                    text = "편집",
                    onClick = { showEditDialog = true },
                    enabled = enabled
                )

                EditorActionDivider()

                EditorTextAction(
                    text = "삭제",
                    onClick = { onDeleteSeal(selectedSeal.id) },
                    enabled = enabled,
                    contentColor = GalleryDangerRed
                )
            }
        } else if (photoSeals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorQuietHint(
                text = "편집할 도장을 선택해."
            )
        }
    }

    if (showCreateDialog) {
        SealDesignDialog(
            title = "새 도장 추가",
            confirmText = "추가",
            initialType = SealType.entries.first(),
            initialColorArgb = sealSelectableInkColors.first().toArgb().toLong() and 0xFFFFFFFFL,
            isOutOfBounds = false,
            enabled = enabled,
            onDismiss = { showCreateDialog = false },
            onRestorePosition = null,
            onConfirm = { type, colorArgb ->
                onAddSeal(type, colorArgb)
            }
        )
    }

    if (showEditDialog && selectedSeal != null) {
        SealDesignDialog(
            title = "도장 편집",
            confirmText = "저장",
            initialType = selectedSeal.type,
            initialColorArgb = selectedSeal.colorArgb,
            isOutOfBounds = isSelectedSealOutOfBounds,
            enabled = enabled,
            onDismiss = { showEditDialog = false },
            onRestorePosition = onRestoreSealPosition,
            onConfirm = { type, colorArgb ->
                onEditSeal(selectedSeal.id, type, colorArgb)
            }
        )
    }
}

/**
 * `+ 추가`와 `편집`이 함께 쓰는 도장 설정창. 두 진입점을 서로 다른 화면처럼
 * 보이지 않게, 종류·색상을 같은 위치·같은 문법으로 보여주는 하나의 Dialog로
 * 만들고 제목·확인 버튼 문구·초기값·"위치 되돌리기" 노출 여부만 호출부에서
 * 다르게 준다. 구조를 복제하는 대신 create/edit 모두 이 컴포저블 하나를 쓴다.
 *
 * 종류·색상 모두 이 창 안의 local draft이고, 확인 버튼을 눌러야 실제 도장에
 * 반영된다 — 취소/Back/바깥 dismiss는 아무것도 바꾸지 않는다. 편집에서는
 * 종류·색상 변경이 저장 1회에 함께 반영되니 Undo도 저장 1회당 1단계로
 * 남는다.
 *
 * 이미 저장된 흰 잉크(SealInkWhite) 도장을 편집할 때는 draft 색상이 흰색으로
 * 시작하지만 신규 선택지(sealSelectableInkColors)에는 흰색이 없어 어떤
 * 스와치도 선택 표시되지 않는다 — 저장을 누르지 않는 한 색상은 그대로
 * 남고, 미리보기 배경만 NeutralLight로 바꿔 흰 잉크가 실제로 보이게 한다.
 *
 * 범위 밖으로 나간 도장의 "위치 되돌리기"는 도장 디자인 property가 아니라
 * 하단 contextual action을 늘리지 않기 위해 얹은 보조 action이라, 생성
 * 모드에서는 아예 노출하지 않는다(onRestorePosition == null). 위치 복원은
 * draft가 아니라 즉시 적용되는 교정 동작이라 탭하면 바로 실행하고 창을
 * 닫는다.
 */
@Composable
private fun SealDesignDialog(
    title: String,
    confirmText: String,
    initialType: SealType,
    initialColorArgb: Long,
    isOutOfBounds: Boolean,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onRestorePosition: (() -> Unit)?,
    onConfirm: (type: SealType, colorArgb: Long) -> Unit
) {
    var typeDraft by remember { mutableStateOf(initialType) }
    var colorArgbDraft by remember { mutableStateOf(initialColorArgb) }

    val isDraftWhiteInk =
        colorArgbDraft == (SealInkWhite.toArgb().toLong() and 0xFFFFFFFFL)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        titleContentColor = InkPrimary,
        textContentColor = InkPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = title,
                color = InkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (isDraftWhiteInk) NeutralLight else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SealPreviewContent(
                        type = typeDraft,
                        color = Color(colorArgbDraft),
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "도장 종류",
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                SealTypeTileRow(
                    types = SealType.entries,
                    selectedType = typeDraft,
                    enabled = enabled,
                    onTypeSelected = { typeDraft = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "잉크 색상",
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
                    sealSelectableInkColors.forEach { inkColor ->
                        val inkArgb =
                            inkColor.toArgb().toLong() and 0xFFFFFFFFL
                        val isColorSelected =
                            colorArgbDraft == inkArgb

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                .clickable(enabled = enabled) {
                                    colorArgbDraft = inkArgb
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

                if (onRestorePosition != null && isOutOfBounds) {
                    Spacer(modifier = Modifier.height(16.dp))

                    EditorTextAction(
                        text = "위치 되돌리기",
                        onClick = {
                            onRestorePosition()
                            onDismiss()
                        },
                        enabled = enabled
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(typeDraft, colorArgbDraft)
                    onDismiss()
                },
                enabled = enabled
            ) {
                Text(
                    text = confirmText,
                    color = SunsetGold,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "취소",
                    color = BrutalBlack,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun SealTypeTileRow(
    types: List<SealType>,
    selectedType: SealType,
    enabled: Boolean,
    onTypeSelected: (SealType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        types.forEach { type ->
            DecorationPresetTile(
                onClick = { onTypeSelected(type) },
                enabled = enabled,
                previewModifier = Modifier.size(52.dp),
                backgroundColor = Color.Transparent,
                label = type.label,
                selected = type == selectedType
            ) {
                SealPreviewContent(
                    type = type,
                    color = BrutalBlack,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
