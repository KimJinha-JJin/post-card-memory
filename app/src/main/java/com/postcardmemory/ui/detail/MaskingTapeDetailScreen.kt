package com.postcardmemory.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.EditorEmptyHint
import com.postcardmemory.ui.components.EditorOutlineButton
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.MaskingTapeContent
import com.postcardmemory.ui.components.PostcardCustomColorPicker
import com.postcardmemory.ui.components.postcardBackgroundPalette
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.SunsetGold

/** 커스텀 편집기에서 무늬·색상 컨트롤 영역이 차지할 수 있는 최대 높이. 이 값으로
 *  묶어 자체 스크롤시키면, 팔레트를 아무리 펼쳐도 위의 미리보기와 아래의 추가
 *  버튼은 항상 화면에 남는다.
 */
private val MASKING_TAPE_CUSTOM_EDITOR_CONTROLS_MAX_HEIGHT = 280.dp

@Composable
internal fun MaskingTapePickerPanel(
    photoMaskingTapes: List<MaskingTapeItem>,
    selectedMaskingTapeId: String?,
    onSelectMaskingTape: (String) -> Unit,
    onAddMaskingTape: (MaskingTapeStyle) -> Unit,
    onAddCustomMaskingTape: (
        baseColorArgb: Long,
        patternColorArgb: Long,
        patternKind: MaskingTapePatternKind
    ) -> Unit,
    onAddPhotoMaskingTape: (Uri) -> Unit,
    onDeleteMaskingTape: (String) -> Unit,
    onDuplicateMaskingTape: (String) -> Unit,
    onEdgeStyleSelected: (String, MaskingTapeEdgeStyle) -> Unit,
    editMode: StickerEditMode,
    onEditModeSelected: (StickerEditMode) -> Unit,
    onLengthScaleSelected: (String, Float) -> Unit,
    onThicknessScaleSelected: (String, Float) -> Unit,
    onUndoMaskingTape: () -> Unit,
    onRedoMaskingTape: () -> Unit,
    canUndoMaskingTape: Boolean,
    canRedoMaskingTape: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedTape =
        photoMaskingTapes.find { it.id == selectedMaskingTapeId }

    var isAddPanelExpanded by remember {
        mutableStateOf(photoMaskingTapes.isEmpty())
    }
    var isCustomEditorExpanded by remember {
        mutableStateOf(false)
    }

    val photoPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }
                onAddPhotoMaskingTape(uri)
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
                text = "마스킹테이프",
                color = BrutalBlack,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            EditorUndoRedoButtons(
                canUndo = canUndoMaskingTape,
                canRedo = canRedoMaskingTape,
                onUndo = onUndoMaskingTape,
                onRedo = onRedoMaskingTape,
                enabled = enabled,
                undoContentDescription = "실행 취소",
                redoContentDescription = "다시 실행"
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = { isAddPanelExpanded = !isAddPanelExpanded },
            enabled = enabled,
            colors = ButtonDefaults.textButtonColors(contentColor = BrutalBlack)
        ) {
            Text(
                text = if (isAddPanelExpanded) "디자인 선택 닫기" else "새 마스킹테이프 추가",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        AnimatedVisibility(
            visible = isAddPanelExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "직접 만들기",
                    color = BrutalBlack,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MaskingTapeCreateTile(
                        enabled = enabled,
                        label = "커스텀",
                        icon = Icons.Default.Palette,
                        onClick = { isCustomEditorExpanded = !isCustomEditorExpanded },
                        modifier = Modifier.weight(1f)
                    )

                    MaskingTapeCreateTile(
                        enabled = enabled,
                        label = "사진",
                        icon = Icons.Default.Photo,
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "기본 디자인",
                    color = BrutalBlack,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetMaskingTapeStyles.forEach { style ->
                        MaskingTapeDesignTile(
                            enabled = enabled,
                            label = style.label,
                            onClick = { onAddMaskingTape(style) }
                        ) {
                            MaskingTapeContent(
                                style = style,
                                modifier = Modifier.size(width = 64.dp, height = 26.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isCustomEditorExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    MaskingTapeCustomEditor(
                        enabled = enabled,
                        onConfirm = { base, pattern, kind ->
                            onAddCustomMaskingTape(base, pattern, kind)
                            isCustomEditorExpanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }
            }
        }

        if (photoMaskingTapes.isEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorEmptyHint(
                text = "아직 붙인 마스킹테이프가 없어."
            )

            return@Column
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "붙인 마스킹테이프",
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
            photoMaskingTapes.forEach { tape ->
                val isSelected = tape.id == selectedMaskingTapeId

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = enabled) {
                            onSelectMaskingTape(tape.id)
                        }
                        .padding(6.dp)
                ) {
                    MaskingTapeContent(
                        tape = tape,
                        modifier = Modifier.size(width = 56.dp, height = 24.dp)
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
        }

        if (selectedTape == null) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorEmptyHint(
                text = "편집할 마스킹테이프를 선택해."
            )

            return@Column
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "가장자리 모양",
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
            MaskingTapeEdgeStyle.entries.forEach { edgeStyle ->
                val isEdgeSelected = selectedTape.edgeStyle == edgeStyle

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            color = if (isEdgeSelected) SunsetGold else Color(0xFFF4ECDE)
                        )
                        .clickable(enabled = enabled) {
                            onEdgeStyleSelected(selectedTape.id, edgeStyle)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = edgeStyle.label,
                        color = BrutalBlack,
                        fontSize = 12.sp,
                        fontWeight = if (isEdgeSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "손가락 조작",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "이동은 언제나 가능해. 두 손가락 제스처가 크기/회전 중 뭘 바꿀지만 골라줘.",
            color = BrutalBlack,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                StickerEditMode.Move to "이동만",
                StickerEditMode.Scale to "크기",
                StickerEditMode.Rotate to "회전"
            ).forEach { (mode, label) ->
                val isModeSelected = editMode == mode

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            color = if (isModeSelected) SunsetGold else Color(0xFFF4ECDE)
                        )
                        .clickable(enabled = enabled) {
                            onEditModeSelected(mode)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = BrutalBlack,
                        fontSize = 12.sp,
                        fontWeight = if (isModeSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "길이",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MASKING_TAPE_LENGTH_SCALE_STEPS.zip(
                listOf("짧게", "보통", "길게", "아주 길게")
            ).forEach { (step, label) ->
                val isStepSelected = selectedTape.lengthScale == step

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            color = if (isStepSelected) SunsetGold else Color(0xFFF4ECDE)
                        )
                        .clickable(enabled = enabled) {
                            onLengthScaleSelected(selectedTape.id, step)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = BrutalBlack,
                        fontSize = 12.sp,
                        fontWeight = if (isStepSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "굵기",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MASKING_TAPE_THICKNESS_SCALE_STEPS.zip(
                listOf("얇게", "보통", "굵게", "아주 굵게")
            ).forEach { (step, label) ->
                val isStepSelected = selectedTape.thicknessScale == step

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            color = if (isStepSelected) SunsetGold else Color(0xFFF4ECDE)
                        )
                        .clickable(enabled = enabled) {
                            onThicknessScaleSelected(selectedTape.id, step)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = BrutalBlack,
                        fontSize = 12.sp,
                        fontWeight = if (isStepSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EditorOutlineButton(
                text = "복제",
                icon = Icons.Default.ContentCopy,
                onClick = { onDuplicateMaskingTape(selectedTape.id) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )

            EditorOutlineButton(
                text = "삭제",
                icon = Icons.Default.Delete,
                onClick = { onDeleteMaskingTape(selectedTape.id) },
                enabled = enabled,
                contentColor = GalleryDangerRed,
                borderColor = GalleryDangerRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MaskingTapeCreateTile(
    enabled: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color = SunsetGold.copy(alpha = 0.16f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BrutalBlack,
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = label,
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MaskingTapeDesignTile(
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 64.dp, height = 26.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = BrutalBlack,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private enum class MaskingTapeCustomColorTarget {
    BASE,
    PATTERN
}

/**
 * 배경 커스텀 색상 편집(PostcardCustomColorPicker)과 같은 방식으로
 * 베이스·패턴 색상을 자유롭게 고르고, 무늬 종류를 선택해 새 마스킹테이프를
 * 만든다. 사용자에게 opacity 조절은 제공하지 않는다(고정 alpha 정책 유지).
 */
@Composable
private fun MaskingTapeCustomEditor(
    enabled: Boolean,
    onConfirm: (
        baseColorArgb: Long,
        patternColorArgb: Long,
        patternKind: MaskingTapePatternKind
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var baseColorArgb by remember {
        mutableStateOf(MaskingTapeStyle.CUSTOM.baseColorArgb)
    }
    var patternColorArgb by remember {
        mutableStateOf(MaskingTapeStyle.CUSTOM.patternColorArgb)
    }
    var patternKind by remember {
        mutableStateOf(MaskingTapePatternKind.DOT)
    }
    var colorTarget by remember {
        mutableStateOf(MaskingTapeCustomColorTarget.BASE)
    }

    Column(
        modifier = modifier
            .background(color = Color(0xFFFFFBF3), shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "커스텀 마스킹테이프 만들기",
            color = BrutalBlack,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(10.dp))

        MaskingTapeContent(
            tape = MaskingTapeItem(
                style = MaskingTapeStyle.CUSTOM,
                customBaseColorArgb = baseColorArgb,
                customPatternColorArgb = patternColorArgb,
                customPatternKind = patternKind
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 무늬 선택부터 색상판까지는 세로로 길어질 수 있어 자체 스크롤
        // 영역으로 묶는다. 위의 미리보기와 아래의 "추가" 버튼은 이
        // 영역 밖에 있어 색을 고르는 동안에도 계속 보인다.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = MASKING_TAPE_CUSTOM_EDITOR_CONTROLS_MAX_HEIGHT)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "무늬",
                color = BrutalBlack,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaskingTapePatternKind.entries.forEach { kind ->
                    val isKindSelected = patternKind == kind

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = if (isKindSelected) SunsetGold else Color(0xFFF4ECDE)
                            )
                            .clickable(enabled = enabled) { patternKind = kind }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = maskingTapePatternKindLabel(kind),
                            color = BrutalBlack,
                            fontSize = 11.sp,
                            fontWeight = if (isKindSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MaskingTapeColorTargetToggle(
                    label = "베이스 색상",
                    selected = colorTarget == MaskingTapeCustomColorTarget.BASE,
                    swatchColorArgb = baseColorArgb,
                    enabled = enabled,
                    onClick = { colorTarget = MaskingTapeCustomColorTarget.BASE },
                    modifier = Modifier.weight(1f)
                )
                MaskingTapeColorTargetToggle(
                    label = "패턴 색상",
                    selected = colorTarget == MaskingTapeCustomColorTarget.PATTERN,
                    swatchColorArgb = patternColorArgb,
                    enabled = enabled,
                    onClick = { colorTarget = MaskingTapeCustomColorTarget.PATTERN },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                postcardBackgroundPalette.forEach { swatchArgb ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = Color(swatchArgb))
                            .clickable(enabled = enabled) {
                                when (colorTarget) {
                                    MaskingTapeCustomColorTarget.BASE ->
                                        baseColorArgb = swatchArgb
                                    MaskingTapeCustomColorTarget.PATTERN ->
                                        patternColorArgb = swatchArgb
                                }
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PostcardCustomColorPicker(
                selectedColorArgb = when (colorTarget) {
                    MaskingTapeCustomColorTarget.BASE -> baseColorArgb
                    MaskingTapeCustomColorTarget.PATTERN -> patternColorArgb
                },
                enabled = enabled,
                onColorSelected = { colorArgb ->
                    when (colorTarget) {
                        MaskingTapeCustomColorTarget.BASE -> baseColorArgb = colorArgb
                        MaskingTapeCustomColorTarget.PATTERN -> patternColorArgb = colorArgb
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        EditorOutlineButton(
            text = "이 디자인으로 추가",
            icon = Icons.Default.Add,
            onClick = { onConfirm(baseColorArgb, patternColorArgb, patternKind) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MaskingTapeColorTargetToggle(
    label: String,
    selected: Boolean,
    swatchColorArgb: Long,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color = if (selected) SunsetGold.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color = Color(swatchColorArgb))
        )
        Text(
            text = label,
            color = BrutalBlack,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun maskingTapePatternKindLabel(kind: MaskingTapePatternKind): String =
    when (kind) {
        MaskingTapePatternKind.GRID -> "격자"
        MaskingTapePatternKind.DOT -> "도트"
        MaskingTapePatternKind.STAR -> "별"
        MaskingTapePatternKind.HEART -> "하트"
        MaskingTapePatternKind.STRIPE -> "사선"
        MaskingTapePatternKind.PLAIN -> "무늬없음"
    }
