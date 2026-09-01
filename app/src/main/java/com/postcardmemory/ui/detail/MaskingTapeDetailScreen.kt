package com.postcardmemory.ui.detail

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.DecorationPresetTile
import com.postcardmemory.ui.components.EditorActionDivider
import com.postcardmemory.ui.components.EditorFlatPresetTile
import com.postcardmemory.ui.components.EditorQuietHint
import com.postcardmemory.ui.components.EditorSlider
import com.postcardmemory.ui.components.EditorTextAction
import com.postcardmemory.ui.components.EditorUndoRedoButtons
import com.postcardmemory.ui.components.MaskingTapeContent
import com.postcardmemory.ui.components.PostcardCustomColorPicker
import com.postcardmemory.ui.components.postcardBackgroundPalette
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SunsetGold
import kotlin.math.roundToInt

/**
 * 마스킹테이프 탭의 생성 방식(기본 디자인/커스텀/사진) 선택은 54일차부터
 * 스티커 탭의 사진/텍스트/라벨 선택과 같은 문법을 쓴다 — 패널 내부의
 * 서랍(구 isAddPanelExpanded/isCustomEditorExpanded)이 아니라, 호출부
 * (DetailScreen.kt)가 고정 하단 영역에서 [EditorSubcategoryNavBar]로
 * 선택하고 그 결과만 [creationTabIndex]로 받는다. 같은 이유로 가장자리
 * 모양·길이·굵기·회전 Property는 이 패널에 항상 펼쳐두지 않고 [편집] 액션이
 * 여는 [MaskingTapeEditDialog]의 local draft로 옮겼다 — 라벨 스티커
 * 편집(LabelStickerEditDialog)과 동일한 "저장 시점에만 실제 객체에 반영"
 * 구조라, Undo도 저장 1회당 1단계로 남는다.
 */
@Composable
internal fun MaskingTapePickerPanel(
    photoMaskingTapes: List<MaskingTapeItem>,
    selectedMaskingTapeId: String?,
    creationTabIndex: Int,
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
    onEditMaskingTapeProperties: (
        id: String,
        edgeStyle: MaskingTapeEdgeStyle,
        lengthScale: Float,
        thicknessScale: Float,
        rotationDegrees: Float
    ) -> Unit,
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

    var showEditDialog by remember { mutableStateOf(false) }
    var showPresetCreateDialog by remember { mutableStateOf(false) }
    var showCustomCreateDialog by remember { mutableStateOf(false) }

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
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // 붙인 테이프 목록과 `+ 추가`를 한 줄에 둔다(라벨 스티커 패널과 같은
        // 문법). 목록이 비어 있어도 이 줄과 `+ 추가`의 자리는 그대로라서
        // "아직 없다"는 별도 안내 상자 없이도 무엇을 눌러야 할지 드러난다.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            photoMaskingTapes.forEach { tape ->
                val isSelected = tape.id == selectedMaskingTapeId

                DecorationPresetTile(
                    onClick = { onSelectMaskingTape(tape.id) },
                    enabled = enabled,
                    backgroundColor = Color.Transparent,
                    selected = isSelected
                ) {
                    MaskingTapeContent(
                        tape = tape,
                        modifier = Modifier.size(width = 56.dp, height = 24.dp)
                    )
                }
            }

            // 세 생성 방식(기본 디자인/커스텀/사진)은 모두 "새 마스킹테이프를
            // 추가한다"는 같은 역할이므로 진입점도 같은 자리·같은 문법을 쓴다.
            // 어떤 방식인지는 고정 하단 EditorSubcategoryNavBar가 정하고,
            // 여기서는 creationTabIndex에 따라 목적지만 달라진다 — 탭을 바꿔도
            // 이 타일의 위치는 움직이지 않는다. 세 목적지 모두 modal이라 패널
            // 높이도 고정이다.
            EditorFlatPresetTile(
                onClick = {
                    when (creationTabIndex) {
                        0 -> showPresetCreateDialog = true
                        1 -> showCustomCreateDialog = true
                        2 -> photoPicker.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                },
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

        if (selectedTape != null) {
            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                EditorTextAction(
                    text = "편집",
                    onClick = { showEditDialog = true },
                    enabled = enabled
                )

                EditorActionDivider()

                EditorTextAction(
                    text = "복제",
                    onClick = { onDuplicateMaskingTape(selectedTape.id) },
                    enabled = enabled
                )

                EditorActionDivider()

                EditorTextAction(
                    text = "삭제",
                    onClick = { onDeleteMaskingTape(selectedTape.id) },
                    enabled = enabled,
                    contentColor = GalleryDangerRed
                )
            }
        } else if (photoMaskingTapes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            EditorQuietHint(
                text = "편집할 마스킹테이프를 선택해."
            )
        }
    }

    if (showEditDialog && selectedTape != null) {
        MaskingTapeEditDialog(
            tape = selectedTape,
            enabled = enabled,
            onDismiss = { showEditDialog = false },
            onConfirm = { edgeStyle, lengthScale, thicknessScale, rotationDegrees ->
                onEditMaskingTapeProperties(
                    selectedTape.id,
                    edgeStyle,
                    lengthScale,
                    thicknessScale,
                    rotationDegrees
                )
                showEditDialog = false
            }
        )
    }

    if (showPresetCreateDialog) {
        MaskingTapePresetCreateDialog(
            enabled = enabled,
            onDismiss = { showPresetCreateDialog = false },
            onConfirm = { style ->
                onAddMaskingTape(style)
                showPresetCreateDialog = false
            }
        )
    }

    if (showCustomCreateDialog) {
        MaskingTapeCustomCreateDialog(
            enabled = enabled,
            onDismiss = { showCustomCreateDialog = false },
            onConfirm = { base, pattern, kind ->
                onAddCustomMaskingTape(base, pattern, kind)
                showCustomCreateDialog = false
            }
        )
    }
}

/**
 * 기본 디자인 탭의 `+ 추가`가 여는 생성창. 프리셋을 탭하는 것은 아직 선택일
 * 뿐이고(local draft), 실제 테이프는 `저장`을 눌러야 만들어진다 — 취소/Back/
 * 바깥 dismiss로 닫으면 아무것도 생성되지 않는다. 커스텀 생성창과 같은 골격을
 * 써서 두 생성 방식이 같은 문법으로 읽히게 한다. 프리셋 데이터·색상·모양은
 * 그대로 두고 고르는 자리만 옮긴 것이다.
 */
@Composable
private fun MaskingTapePresetCreateDialog(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (MaskingTapeStyle) -> Unit
) {
    var selectedStyleDraft by remember {
        mutableStateOf(presetMaskingTapeStyles.first())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        titleContentColor = InkPrimary,
        textContentColor = InkPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "기본 디자인으로 만들기",
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
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MaskingTapeContent(
                        style = selectedStyleDraft,
                        modifier = Modifier.size(width = 160.dp, height = 40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "디자인",
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    presetMaskingTapeStyles.forEach { style ->
                        DecorationPresetTile(
                            onClick = { selectedStyleDraft = style },
                            enabled = enabled,
                            backgroundColor = Color.Transparent,
                            label = style.label,
                            selected = style == selectedStyleDraft
                        ) {
                            MaskingTapeContent(
                                style = style,
                                modifier = Modifier.size(width = 64.dp, height = 26.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedStyleDraft) }
            ) {
                Text(
                    text = "저장",
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

private enum class MaskingTapeCustomColorTarget {
    BASE,
    PATTERN
}

/**
 * 커스텀 탭의 `+ 추가`가 여는 생성창. 배경 커스텀 색상 편집
 * (PostcardCustomColorPicker)과 같은 방식으로 베이스·패턴 색상을 자유롭게
 * 고르고, 무늬 종류를 선택해 새 마스킹테이프를 만든다. 사용자에게 opacity
 * 조절은 제공하지 않는다(고정 alpha 정책 유지).
 *
 * 무늬·색상·색상 타깃은 전부 이 창 안의 local draft라, 여러 번 바꿔도 실제
 * 테이프는 생기지 않는다 — `저장`을 눌러야 한 번 생성된다. 원래 패널 안에
 * 인라인으로 펼쳐져 있던 편집기를 그대로 옮겨온 것이라 기능은 동일하고,
 * 자체 스크롤 높이 제한(구 CONTROLS_MAX_HEIGHT)만 Dialog 스크롤로 대체됐다.
 */
@Composable
private fun MaskingTapeCustomCreateDialog(
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        baseColorArgb: Long,
        patternColorArgb: Long,
        patternKind: MaskingTapePatternKind
    ) -> Unit
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        titleContentColor = InkPrimary,
        textContentColor = InkPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "커스텀 마스킹테이프 만들기",
                color = InkPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
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

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "무늬",
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaskingTapePatternKind.entries.forEach { kind ->
                        val isKindSelected = patternKind == kind
                        val patternChipShape = RoundedCornerShape(8.dp)

                        Box(
                            modifier = Modifier
                                .clip(patternChipShape)
                                .background(
                                    color = if (isKindSelected) SunsetGold.copy(alpha = 0.16f) else PaperField
                                )
                                .border(width = 1.dp, color = PaperDivider, shape = patternChipShape)
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

                Spacer(modifier = Modifier.height(16.dp))

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
                                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                                .clickable(enabled = enabled) {
                                    when (colorTarget) {
                                        MaskingTapeCustomColorTarget.BASE ->
                                            baseColorArgb = swatchArgb
                                        MaskingTapeCustomColorTarget.PATTERN ->
                                            patternColorArgb = swatchArgb
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color = Color(swatchArgb))
                                    .border(
                                        width = 1.dp,
                                        color = BrutalBlack.copy(alpha = 0.35f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }
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
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(baseColorArgb, patternColorArgb, patternKind) }
            ) {
                Text(
                    text = "저장",
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

private val MASKING_TAPE_EDIT_PREVIEW_BASE_WIDTH = 96.dp
private val MASKING_TAPE_EDIT_PREVIEW_BASE_HEIGHT = 28.dp
private val MASKING_TAPE_EDIT_PREVIEW_MAX_WIDTH = 220.dp
private val MASKING_TAPE_EDIT_PREVIEW_MAX_HEIGHT = 84.dp

/**
 * 기존 메인 패널에 항상 펼쳐져 있던 가장자리 모양·길이·굵기·회전을 옮겨온
 * 상세 편집창. `LabelStickerEditDialog`와 동일하게 local draft로만 값을
 * 들고 있다가 `저장`을 눌렀을 때만 [onConfirm]으로 한 번에 넘긴다 —
 * 취소/바깥 dismiss는 [tape]를 전혀 바꾸지 않는다. 미리보기는 실제 캔버스
 * 크기 상수(DetailScreen.kt의 MASKING_TAPE_BASE_WIDTH/HEIGHT, private)를
 * 가져오지 않고, 이 Dialog 전용 기준값을 draft 배율만큼 키워 보여준다 —
 * 저장·Export 렌더 계산식과는 무관한, 편집 중 형태 확인용 미리보기다.
 */
@Composable
private fun MaskingTapeEditDialog(
    tape: MaskingTapeItem,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        edgeStyle: MaskingTapeEdgeStyle,
        lengthScale: Float,
        thicknessScale: Float,
        rotationDegrees: Float
    ) -> Unit
) {
    var edgeStyleDraft by remember { mutableStateOf(tape.edgeStyle) }
    var lengthScaleDraft by remember { mutableStateOf(tape.lengthScale) }
    var thicknessScaleDraft by remember { mutableStateOf(tape.thicknessScale) }
    var rotationDegreesDraft by remember { mutableStateOf(tape.rotationDegrees) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PaperSurface,
        titleContentColor = InkPrimary,
        textContentColor = InkPrimary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "마스킹테이프 편집",
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
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MaskingTapeContent(
                        tape = tape.copy(edgeStyle = edgeStyleDraft),
                        modifier = Modifier
                            .size(
                                width = (MASKING_TAPE_EDIT_PREVIEW_BASE_WIDTH * lengthScaleDraft)
                                    .coerceAtMost(MASKING_TAPE_EDIT_PREVIEW_MAX_WIDTH),
                                height = (MASKING_TAPE_EDIT_PREVIEW_BASE_HEIGHT * thicknessScaleDraft)
                                    .coerceAtMost(MASKING_TAPE_EDIT_PREVIEW_MAX_HEIGHT)
                            )
                            .graphicsLayer {
                                rotationZ = rotationDegreesDraft
                            }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "가장자리 모양",
                    color = BrutalBlack,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaskingTapeEdgeStyle.entries.forEach { edgeStyle ->
                        val isEdgeSelected = edgeStyleDraft == edgeStyle
                        val edgeChipShape = RoundedCornerShape(10.dp)

                        Box(
                            modifier = Modifier
                                .clip(edgeChipShape)
                                .background(
                                    color = if (isEdgeSelected) SunsetGold.copy(alpha = 0.16f) else PaperField
                                )
                                .border(width = 1.dp, color = PaperDivider, shape = edgeChipShape)
                                .clickable(enabled = enabled) {
                                    edgeStyleDraft = edgeStyle
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

                MaskingTapeAdjustSlider(
                    label = "길이",
                    value = lengthScaleDraft,
                    valueRange = MASKING_TAPE_MIN_LENGTH_SCALE..MASKING_TAPE_MAX_LENGTH_SCALE,
                    enabled = enabled,
                    onValueChange = { lengthScaleDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                MaskingTapeAdjustSlider(
                    label = "굵기",
                    value = thicknessScaleDraft,
                    valueRange = MASKING_TAPE_MIN_THICKNESS_SCALE..MASKING_TAPE_MAX_THICKNESS_SCALE,
                    enabled = enabled,
                    onValueChange = { thicknessScaleDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                MaskingTapeAdjustSlider(
                    label = "회전",
                    value = rotationDegreesDraft,
                    valueRange = MASKING_TAPE_MIN_ROTATION_DEGREES..MASKING_TAPE_MAX_ROTATION_DEGREES,
                    valueLabel = "${rotationDegreesDraft.roundToInt()}°",
                    enabled = enabled,
                    onValueChange = { rotationDegreesDraft = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        edgeStyleDraft,
                        lengthScaleDraft,
                        thicknessScaleDraft,
                        rotationDegreesDraft
                    )
                }
            ) {
                Text(
                    text = "저장",
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

/**
 * 길이·굵기·회전이 공유하는 편집 패널 slider 한 줄. 라벨과(있다면) 보조
 * 값 표시를 위에 두고 그 아래 공용 EditorSlider를 그린다. 숫자 입력창은
 * 만들지 않는다 — 사용자는 결과를 보고 조절한다(작업지시서 14/19절).
 */
@Composable
private fun MaskingTapeAdjustSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: () -> Unit = {},
    valueLabel: String? = null
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = BrutalBlack,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            if (valueLabel != null) {
                Text(
                    text = valueLabel,
                    color = BrutalBlack,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        EditorSlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
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
