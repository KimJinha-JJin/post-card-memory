package com.postcardmemory.ui.bookmark

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.postcardmemory.data.BookmarkDesign
import com.postcardmemory.data.BookmarkPaper
import com.postcardmemory.data.BookmarkShape
import com.postcardmemory.data.BookmarkTrace
import com.postcardmemory.ui.components.BOOKMARK_EXPOSURE_FRACTION
import com.postcardmemory.ui.components.PinkingPhotoShape
import com.postcardmemory.ui.components.StampPhoto
import com.postcardmemory.ui.components.bookmarkHeightFraction
import com.postcardmemory.ui.components.drawBookmarkPaper
import com.postcardmemory.ui.components.drawPostcardShadeOnBookmark
import com.postcardmemory.ui.components.insertedBookmarkBounds
import com.postcardmemory.ui.gallery.rememberSystemReducedMotion
import com.postcardmemory.ui.theme.DangerRed
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperCanvas
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.PaperTray

private val StudioCorner = RoundedCornerShape(2.dp)

internal val BookmarkShape.label: String
    get() = when (this) {
        BookmarkShape.CLASSIC -> "클래식"
        BookmarkShape.HAND_CUT -> "손오림"
        BookmarkShape.MINI_TAG -> "미니 태그"
    }

internal val BookmarkPaper.label: String
    get() = when (this) {
        BookmarkPaper.IVORY -> "아이보리"
        BookmarkPaper.BROWN -> "브라운"
        BookmarkPaper.MINT -> "연민트"
        BookmarkPaper.LAVENDER -> "더스티 라벤더"
        BookmarkPaper.BLUE -> "빈티지 블루"
        BookmarkPaper.APRICOT -> "살구빛 종이"
        BookmarkPaper.ROSE -> "말린 장미"
        BookmarkPaper.OLIVE -> "올리브 베이지"
        BookmarkPaper.BUTTER -> "버터 옐로"
    }

internal val BookmarkTrace.label: String
    get() = when (this) {
        BookmarkTrace.NONE -> "없이"
        BookmarkTrace.POSTMARK -> "작은 소인"
        BookmarkTrace.LINES -> "세 줄 무늬"
    }

/**
 * 143일차: 기억 책갈피를 만드는 종이 공방. 별도 창(Dialog)이라 아래 편집 화면의 composition과
 * ViewModel은 그대로 살아 있고, 닫으면 편집하던 자리로 돌아가. 편집 화면의 저장·초안·되돌리기는
 * 부르지 않아.
 */
@Composable
internal fun MemoryBookmarkStudio(
    session: MemoryBookmarkSession,
    state: BookmarkStudioState,
    imagePath: String
) {
    Dialog(
        onDismissRequest = session::close,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
        BookmarkStudioContent(session, state, imagePath)
    }
}

@Composable
private fun BookmarkStudioContent(
    session: MemoryBookmarkSession,
    state: BookmarkStudioState,
    imagePath: String
) {
    val reduceMotion = rememberSystemReducedMotion()
    var peeking by rememberSaveable { mutableStateOf(false) }
    var showRemoveConfirm by rememberSaveable { mutableStateOf(false) }
    // 이미 재생을 시작한 연출 번호. 화면이 다시 만들어져도 같은 끼우기를 두 번 보여주지 않아.
    var playedToken by rememberSaveable { mutableIntStateOf(0) }
    val elapsed = remember {
        Animatable(
            if (state.step == BookmarkStep.DONE || peeking) BOOKMARK_INSERT_TOTAL_MS.toFloat() else 0f
        )
    }

    // 다 끼운 뒤에만 엽서를 작업대 가운데로 모아(끼우는 동안에는 엽서 자리가 움직이지 않아).
    val centering = remember { Animatable(if (state.step == BookmarkStep.DONE) 1f else 0f) }
    LaunchedEffect(state.step == BookmarkStep.DONE) {
        val target = if (state.step == BookmarkStep.DONE) 1f else 0f
        if (reduceMotion) centering.snapTo(target) else centering.animateTo(target, tween(PEEK_OUT_MS))
    }

    LaunchedEffect(state.step, state.insertToken, peeking) {
        val end = BOOKMARK_INSERT_TOTAL_MS.toFloat()
        when (state.step) {
            BookmarkStep.INSERTING -> {
                if (reduceMotion || playedToken == state.insertToken) {
                    elapsed.snapTo(end)
                } else {
                    playedToken = state.insertToken
                    // 미리보기로 끼워 둔 상태였다면 종이를 먼저 엽서 옆으로 되돌린 다음 끼워.
                    if (elapsed.value > 0f) elapsed.animateTo(0f, tween(PEEK_OUT_MS, easing = FastOutSlowInEasing))
                    elapsed.animateTo(end, tween(BOOKMARK_INSERT_TOTAL_MS, easing = LinearEasing))
                }
                peeking = false
                session.onInsertionShown()
            }

            BookmarkStep.DONE -> elapsed.snapTo(end)

            BookmarkStep.EDITING -> {
                val target = if (peeking) end else 0f
                when {
                    elapsed.value == target -> Unit
                    reduceMotion -> elapsed.snapTo(target)
                    peeking -> elapsed.animateTo(
                        target,
                        tween((BOOKMARK_INSERT_TOTAL_MS * (1f - elapsed.value / end)).toInt(), easing = LinearEasing)
                    )
                    else -> elapsed.animateTo(target, tween(PEEK_OUT_MS, easing = FastOutSlowInEasing))
                }
            }

            BookmarkStep.SAVING, BookmarkStep.REMOVING -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = session::close, enabled = !state.busy) {
                Icon(Icons.Default.Close, contentDescription = "책갈피 만들기 닫기", tint = InkPrimary)
            }
            Text(
                text = "기억 책갈피",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .semantics { heading() }
            )
            if (state.hadBookmark && state.step == BookmarkStep.EDITING) {
                TextButton(onClick = { showRemoveConfirm = true }, shape = StudioCorner) {
                    Text("책갈피 빼기", color = InkSecondary, fontSize = 14.sp)
                }
            }
        }
        HorizontalDivider(color = PaperDivider, thickness = 1.dp)

        BoxWithConstraints(Modifier.weight(1f)) {
            // 남는 높이를 작업대에 주되, 작은 화면에서는 줄이고 전체를 스크롤해서 모든 옵션을 쓸 수 있게 해.
            val stageHeight = (maxHeight - OPTIONS_HEIGHT).coerceIn(STAGE_MIN_HEIGHT, STAGE_MAX_HEIGHT)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                BookmarkStage(
                    design = state.draft,
                    imagePath = imagePath,
                    elapsedMs = { elapsed.value },
                    centering = { centering.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(stageHeight)
                )
                Text(
                    text = state.error ?: when (state.step) {
                        BookmarkStep.EDITING ->
                            if (peeking) "미리보기야. 아직 저장하지 않았어." else "종이를 골라서 엽서 뒤에 끼워 봐."
                        BookmarkStep.SAVING -> "책갈피를 저장하고 있어."
                        BookmarkStep.INSERTING -> "엽서를 살짝 들어 종이를 끼우는 중이야."
                        BookmarkStep.DONE -> "엽서 뒤에 끼워 뒀어. 갤러리에서는 종이 끝만 살짝 보이고, 공유한 엽서에는 나오지 않아."
                        BookmarkStep.REMOVING -> "책갈피를 빼고 있어."
                    },
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = if (state.error != null) DangerRed else InkSecondary,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (state.step != BookmarkStep.DONE && state.step != BookmarkStep.INSERTING) {
                    BookmarkOptions(
                        design = state.draft,
                        enabled = state.step == BookmarkStep.EDITING,
                        onChange = session::changeDraft
                    )
                }
            }
        }

        HorizontalDivider(color = PaperDivider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.step == BookmarkStep.DONE || state.step == BookmarkStep.INSERTING) {
                StudioOutlinedButton("다시 꾸미기", Modifier.weight(1f), state.step == BookmarkStep.DONE) {
                    peeking = false
                    session.editAgain()
                }
                StudioFilledButton("다 됐어", Modifier.weight(1f), true, session::close)
            } else {
                val enabled = state.step == BookmarkStep.EDITING
                StudioOutlinedButton(
                    if (peeking) "옆에 두기" else "끼운 모습 보기",
                    Modifier.weight(1f),
                    enabled
                ) { peeking = !peeking }
                StudioFilledButton("이 엽서에 끼우기", Modifier.weight(1f), enabled, session::insert)
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            shape = RoundedCornerShape(4.dp),
            containerColor = PaperSurface,
            titleContentColor = InkPrimary,
            textContentColor = InkPrimary,
            title = { Text("책갈피만 뺄까?", fontSize = 19.sp, fontWeight = FontWeight.SemiBold) },
            text = { Text("끼워 둔 종이만 사라져. 엽서와 사진, 꾸미기는 그대로 남아 있어.") },
            confirmButton = {
                StudioFilledButton("빼기", Modifier, true) {
                    showRemoveConfirm = false
                    session.remove()
                }
            },
            dismissButton = {
                StudioOutlinedButton("취소", Modifier, true) { showRemoveConfirm = false }
            }
        )
    }
}

/** 작업대 위의 엽서와 책갈피. 책갈피는 항상 엽서보다 먼저 그려서 뒤로 들어가. */
@Composable
private fun BookmarkStage(
    design: BookmarkDesign,
    imagePath: String,
    elapsedMs: () -> Float,
    centering: () -> Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.background(PaperTray.copy(alpha = .45f)),
        contentAlignment = Alignment.Center
    ) {
        val edge: Dp = min(maxWidth * .62f, (maxHeight - 36.dp) / (1f + BOOKMARK_EXPOSURE_FRACTION))
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = edge.toPx() * (SCENE_WIDTH_FRACTION - 1f) / 2f * centering() }
                .size(width = edge * SCENE_WIDTH_FRACTION, height = edge * (1f + BOOKMARK_EXPOSURE_FRACTION))
                .semantics {
                    contentDescription =
                        "엽서와 ${design.paper.label} ${design.shape.label} 책갈피, ${design.trace.label}"
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val edgePx = edge.toPx()
                val frame = bookmarkInsertionFrame(elapsedMs())
                val inserted = insertedBookmarkBounds(design.shape, edgePx)
                val away = 1f - frame.slide
                val bounds = Rect(
                    offset = inserted.topLeft + Offset(
                        edgePx * SLIDE_DISTANCE_FRACTION * away,
                        -edgePx * BOOKMARK_EXPOSURE_FRACTION * away
                    ),
                    size = inserted.size
                )
                val tilt = (if (design.shape == BookmarkShape.HAND_CUT) -3f else 3f) * away
                rotate(tilt, pivot = bounds.center) {
                    drawBookmarkPaper(design, bounds.topLeft, bounds.size)
                }
                // 다 끼워질 무렵에만 엽서 그림자가 종이 끝에 스며들어.
                drawPostcardShadeOnBookmark(design, bounds, edgePx, strength = ((frame.slide - .8f) / .2f).coerceIn(0f, 1f))
            }
            StampPhoto(
                imagePath = imagePath,
                contentDescription = null,
                modifier = Modifier
                    .size(edge)
                    .graphicsLayer {
                        val lift = bookmarkInsertionFrame(elapsedMs()).lift
                        translationY = -LIFT_DP.dp.toPx() * lift
                        rotationX = LIFT_TILT_DEGREES * lift
                        cameraDistance = 14f * density
                        shadowElevation = (1.dp + 5.dp * lift).toPx()
                        shape = PinkingPhotoShape
                        clip = false
                    }
                    // 사진이 아직 없을 때도 뒤의 책갈피가 비치지 않게 해.
                    .background(PaperSurface, PinkingPhotoShape),
                outlineWidth = 4f
            )
        }
    }
}

@Composable
private fun ColumnScope.BookmarkOptions(
    design: BookmarkDesign,
    enabled: Boolean,
    onChange: ((BookmarkDesign) -> BookmarkDesign) -> Unit
) {
    OptionTitle("종이 고르기", design.shape.label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BookmarkShape.entries.forEach { shape ->
            PaperChoice(
                design = design.copy(shape = shape),
                label = shape.label,
                selected = shape == design.shape,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) { onChange { it.copy(shape = shape) } }
        }
    }

    OptionTitle("색상 고르기", design.paper.label)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .selectableGroup()
    ) {
        BookmarkPaper.entries.chunked(SWATCHES_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { paper ->
                    PaperSwatch(
                        paper = paper,
                        selected = paper == design.paper,
                        enabled = enabled,
                        modifier = Modifier.weight(1f)
                    ) { onChange { it.copy(paper = paper) } }
                }
                repeat(SWATCHES_PER_ROW - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }

    OptionTitle("작은 흔적", design.trace.label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BookmarkTrace.entries.forEach { trace ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(
                        selected = trace == design.trace,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onChange { it.copy(trace = trace) } }
                    )
                    .choiceBox(trace == design.trace),
                contentAlignment = Alignment.Center
            ) { ChoiceLabel(trace.label, trace == design.trace) }
        }
    }
}

@Composable
private fun OptionTitle(title: String, value: String) {
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkPrimary)
        Text(value, fontSize = 13.sp, color = InkSecondary)
    }
}

/** 실제 종이 윤곽을 그대로 작게 그린 형태 선택 칸. */
@Composable
private fun PaperChoice(
    design: BookmarkDesign,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .heightIn(min = 60.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .choiceBox(selected)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(width = 16.dp, height = 48.dp)) {
            val paperHeight = size.height * bookmarkHeightFraction(design.shape) / .6f
            drawBookmarkPaper(design, Offset(0f, (size.height - paperHeight) / 2f), Size(size.width * .9f, paperHeight))
        }
        ChoiceLabel(label, selected)
    }
}

/** 동그란 칩 대신 잘라 둔 종이 조각. 선택은 테두리와 체크로도 보여줘서 색에만 기대지 않아. */
@Composable
private fun PaperSwatch(
    paper: BookmarkPaper,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = paper.label },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 36.dp)
                .border(BorderStroke(if (selected) 2.dp else 0.dp, if (selected) InkSecondary else PaperCanvas), StudioCorner)
                .padding(4.dp)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawBookmarkPaper(
                    BookmarkDesign(BookmarkShape.MINI_TAG, paper, BookmarkTrace.NONE), Offset.Zero, size
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = InkSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun ChoiceLabel(text: String, selected: Boolean) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = if (selected) InkPrimary else InkSecondary
    )
}

private fun Modifier.choiceBox(selected: Boolean): Modifier = this
    .background(PaperSurface, StudioCorner)
    .border(if (selected) 2.dp else 1.dp, if (selected) InkSecondary else PaperDivider, StudioCorner)

@Composable
private fun StudioOutlinedButton(text: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = StudioCorner,
        border = BorderStroke(1.dp, PaperDivider),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkPrimary),
        modifier = modifier.heightIn(min = 48.dp)
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun StudioFilledButton(text: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = StudioCorner,
        colors = ButtonDefaults.buttonColors(containerColor = InkSecondary, contentColor = PaperSurface),
        modifier = modifier.heightIn(min = 48.dp)
    ) { Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
}

private const val PEEK_OUT_MS = 260
private const val SWATCHES_PER_ROW = 5
private const val LIFT_DP = 5
private const val LIFT_TILT_DEGREES = 6f
private const val SCENE_WIDTH_FRACTION = 1.42f
private const val SLIDE_DISTANCE_FRACTION = .44f
private val OPTIONS_HEIGHT = 372.dp
private val STAGE_MIN_HEIGHT = 200.dp
private val STAGE_MAX_HEIGHT = 340.dp
