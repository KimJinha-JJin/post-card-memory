package com.postcardmemory.ui.gallery

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.postcardmemory.ui.theme.BrutalBlack
import com.postcardmemory.ui.theme.GalleryDangerRed
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.utils.StashedPhotoSticker
import kotlin.math.roundToInt
import kotlin.random.Random

/*
 * 110일차: 스티커 통 — 달력 사이드바 아래의 신문지 파우치와, 파우치를 누르면 달력 대신 보이는
 * 보관함 장. 110일차 목업 A·B-2·C를 Compose로 옮긴 1차 구현이다.
 *
 * 파우치·보관함 종이는 [임시 코드 그림]이다. 최종 그림은 IMAGE-ASSET-STANDARD에 따른 별도
 * 자산(뒷판·앞판, 필요하면 열린 파우치)으로 사용자 승인 뒤 교체한다. 조각은 앞판과 뒷판
 * 사이에 끼워 그려야 하므로 교체 후에도 "뒷판 → 조각 → 앞판" 층 구조는 그대로 둔다.
 */

/** 파우치 그림의 설계 좌표(목업과 같은 132×88). 화면에서는 [STICKER_POUCH_WIDTH_DP]에 맞춰 늘인다. */
private const val POUCH_DESIGN_WIDTH = 132f
private const val POUCH_DESIGN_HEIGHT = 88f
internal const val STICKER_POUCH_WIDTH_DP = 132
internal const val STICKER_POUCH_HEIGHT_DP = 88

/** 입구 위로 보일 조각 수. 많이 모여도 최근 것 셋만 보이고 나머지는 숫자로만 알린다. */
internal const val STICKER_POUCH_VISIBLE_PIECES = 3

private val PouchBackPaper = Color(0xFFDFD6C6)
private val PouchFrontPaper = Color(0xFFEBE3D5)
private val PouchInk = Color(0xFF3A332B)
private val PouchShadow = Color(0xFF3B3226)
private val StashSheetPaper = Color(0xFFF2EADB)

/** 조각 하나의 입구 자리(설계 좌표, 좌상단)와 기본 기울기, 흔들림. 목업 값 그대로. */
private class PouchPieceSlot(
    val x: Float,
    val y: Float,
    val tiltDeg: Float,
    val periodMs: Int,
    val phaseMs: Int,
    val liftDp: Float,
    val swayDp: Float,
    val swingDeg: Float
)

private val POUCH_PIECE_SLOTS = listOf(
    PouchPieceSlot(x = 20f, y = 3f, tiltDeg = -3f, periodMs = 3_100, phaseMs = 400, liftDp = 2f, swayDp = 1f, swingDeg = 2f),
    PouchPieceSlot(x = 52f, y = 7f, tiltDeg = 2f, periodMs = 3_700, phaseMs = 1_700, liftDp = 2.5f, swayDp = -1f, swingDeg = -1.5f),
    PouchPieceSlot(x = 84f, y = 2f, tiltDeg = -1.5f, periodMs = 4_300, phaseMs = 2_600, liftDp = 1.5f, swayDp = 0.8f, swingDeg = 1.8f)
)
private const val POUCH_PIECE_SIZE_DP = 30

/** 시스템 "애니메이션 끄기"(애니메이터 배율 0)면 조각을 멈춘다. */
@Composable
internal fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * 닫힌 파우치. 파우치 전체가 하나의 터치 영역이다. [animate]가 false면(사이드바가 닫혀 있거나
 * 보관함이 열렸거나 움직임 끄기) 무한 애니메이션을 아예 만들지 않는다. 움직임은 graphicsLayer
 * 안에서만 읽어 다시 구성 없이 그리기만 갱신한다.
 */
@Composable
internal fun StickerStashPouch(
    stickers: List<StashedPhotoSticker>,
    animate: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = stickers.take(STICKER_POUCH_VISIBLE_PIECES)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(STICKER_POUCH_WIDTH_DP.dp, STICKER_POUCH_HEIGHT_DP.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpen
                )
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription =
                        if (stickers.isEmpty()) "스티커 통 열기, 비어 있음" else "스티커 통 열기, ${stickers.size}조각"
                }
        ) {
            Box(Modifier.fillMaxSize().stickerPouchBack(empty = visible.isEmpty()))
            if (animate && visible.isNotEmpty()) {
                AnimatedPouchPieces(visible)
            } else {
                visible.forEachIndexed { index, sticker ->
                    PouchPiece(sticker, POUCH_PIECE_SLOTS[index], Modifier)
                }
            }
            Box(Modifier.fillMaxSize().stickerPouchFront())
            Text(
                text = "스티커 통",
                color = PouchInk.copy(alpha = 0.78f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.offset(x = 12.dp, y = 39.dp)
            )
            if (stickers.isNotEmpty()) {
                Text(
                    text = "${stickers.size}조각",
                    color = PouchInk.copy(alpha = 0.65f),
                    fontSize = 8.sp,
                    modifier = Modifier.offset(x = 12.dp, y = 71.dp)
                )
            }
        }
        Text(
            text =
                if (stickers.isEmpty()) {
                    "아직 비어 있어.\n엽서를 꾸미다가\n'통에 넣기'로 담아 봐."
                } else {
                    "최근 담은\n조각 ${visible.size}개"
                },
            color = InkSecondary,
            fontSize = 9.sp,
            lineHeight = 13.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun AnimatedPouchPieces(visible: List<StashedPhotoSticker>) {
    val transition = rememberInfiniteTransition(label = "stickerPouchPieces")
    visible.forEachIndexed { index, sticker ->
        val slot = POUCH_PIECE_SLOTS[index]
        val t by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = slot.periodMs / 2, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = StartOffset(slot.phaseMs)
            ),
            label = "stickerPouchPiece$index"
        )
        PouchPiece(
            sticker = sticker,
            slot = slot,
            motion = Modifier.graphicsLayer {
                // 0 → 1 → 0을 오가며 살짝 떠올랐다 제자리로. 입구 밖으로 나가지 않는 작은 폭.
                translationY = -slot.liftDp.dp.toPx() * t
                translationX = slot.swayDp.dp.toPx() * (t - 0.5f)
                rotationZ = slot.swingDeg * (t - 0.5f)
            }
        )
    }
}

@Composable
private fun PouchPiece(
    sticker: StashedPhotoSticker,
    slot: PouchPieceSlot,
    motion: Modifier
) {
    Box(
        modifier = Modifier
            .offset(x = slot.x.dp, y = slot.y.dp)
            .size(POUCH_PIECE_SIZE_DP.dp)
            .then(motion)
            .graphicsLayer { rotationZ = slot.tiltDeg }
    ) {
        StashPieceImage(sticker, Modifier.fillMaxSize())
    }
}

@Composable
private fun StashPieceImage(sticker: StashedPhotoSticker, modifier: Modifier) {
    AsyncImage(
        model = sticker.displayedFile,
        contentDescription = null,
        contentScale = if (sticker.isBackgroundRemoved) ContentScale.Fit else ContentScale.Crop,
        modifier = modifier
            .graphicsLayer {
                scaleX = if (sticker.flipHorizontal) -1f else 1f
                scaleY = if (sticker.flipVertical) -1f else 1f
            }
            .then(if (sticker.isBackgroundRemoved) Modifier else Modifier.clip(RoundedCornerShape(2.dp)))
    )
}

/**
 * 파우치를 누르면 달력 대신 보이는 보관함 장. 조각은 움직이지 않는다(고르기 쉽게).
 * 조각을 누르면 고르고, 아래 `통에서 빼기`는 확인을 한 번 더 받는다. 엽서 파일은 건드리지 않는다.
 */
@Composable
internal fun StickerStashPage(
    stickers: List<StashedPhotoSticker>,
    onBackToCalendar: () -> Unit,
    onRemove: (StashedPhotoSticker) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf<StashedPhotoSticker?>(null) }
    val selected = stickers.firstOrNull { it.id == selectedId }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← 달력",
                color = InkSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onBackToCalendar)
                    .padding(horizontal = 4.dp, vertical = 15.dp)
                    .semantics { contentDescription = "달력으로 돌아가기" }
            )
            Text(
                text = "스티커 통",
                color = InkPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (stickers.isEmpty()) "비어 있음" else "${stickers.size}조각",
                color = InkSecondary,
                fontSize = 10.sp
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(STICKER_POUCH_WIDTH_DP.dp * 0.9f, STICKER_POUCH_HEIGHT_DP.dp * 0.9f)
                .align(Alignment.CenterHorizontally)
                .clearAndSetSemantics { }
        ) {
            Box(Modifier.fillMaxSize().stickerPouchBack(empty = stickers.isEmpty(), open = true))
            Box(Modifier.fillMaxSize().stickerPouchFront(open = true))
        }

        Spacer(Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .stickerStashSheet()
                .padding(horizontal = 8.dp, vertical = 14.dp)
        ) {
            if (stickers.isEmpty()) {
                StickerStashEmptySheet()
            } else {
                stickers.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { sticker ->
                            StashCell(
                                sticker = sticker,
                                isSelected = sticker.id == selectedId,
                                onClick = {
                                    selectedId = if (selectedId == sticker.id) null else sticker.id
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected != null) {
                Text(
                    text = "고른 조각",
                    color = InkSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "통에서 빼기",
                    color = GalleryDangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clickable { confirmRemove = selected }
                        .padding(horizontal = 4.dp, vertical = 15.dp)
                )
            } else {
                Text(
                    text =
                        if (stickers.isEmpty()) {
                            "빈 통도 늘 여기 있어."
                        } else {
                            "조각을 누르면 고를 수 있어. 엽서에 붙이기는 엽서 편집 › 스티커 › 스티커 통에서 해."
                        },
                    color = InkSecondary,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }

    confirmRemove?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            containerColor = PaperSurface,
            titleContentColor = BrutalBlack,
            textContentColor = BrutalBlack,
            title = { Text("이 조각을 통에서 뺄까요?", fontWeight = FontWeight.SemiBold) },
            text = { Text("엽서에 이미 붙인 스티커는 그대로 남아요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = null
                        selectedId = null
                        onRemove(target)
                    }
                ) {
                    Text("빼기", color = GalleryDangerRed, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) {
                    Text("취소", color = GraphiteAccent, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun StashCell(
    sticker: StashedPhotoSticker,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    // 조각마다 고정된 작은 기울기(종이 위에 흩어 둔 느낌). id로 정해 다시 그려도 같다.
    val tilt = remember(sticker.id) { (Random(sticker.id.hashCode()).nextFloat() * 2f - 1f) * 5f }
    Box(
        modifier = modifier
            .height(76.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .semantics {
                role = Role.Button
                selected = isSelected
                contentDescription = "보관한 조각"
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(Modifier.fillMaxSize().padding(2.dp).stashPenLoop(sticker.id.hashCode()))
        }
        StashPieceImage(
            sticker = sticker,
            modifier = Modifier
                .size(50.dp)
                .graphicsLayer {
                    rotationZ = tilt
                    translationY = if (isSelected) -3.dp.toPx() else 0f
                }
        )
    }
}

@Composable
private fun StickerStashEmptySheet() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(150.dp, 44.dp)
                .stashScissorLine(),
            contentAlignment = Alignment.Center
        ) {
            Text("✂", color = InkSecondary.copy(alpha = 0.6f), fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "아직 담은 조각이 없어.\n엽서 편집 › 스티커에서 조각을 고르고\n'통에 넣기'를 누르면 여기 모여.",
            color = InkSecondary,
            fontSize = 10.sp,
            lineHeight = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ---- 임시 코드 그림 -------------------------------------------------------------------

/** 가위로 오린 외곽: 꼭짓점 사이를 [step] 간격으로 쪼개 [jitter]만큼 흔든 닫힌 경로(설계 좌표). */
private fun pouchCutOutline(points: List<Offset>, seed: Int, step: Float = 3f, jitter: Float = 0.5f): List<Offset> {
    val random = Random(seed)
    val out = mutableListOf<Offset>()
    points.indices.forEach { i ->
        val a = points[i]
        val b = points[(i + 1) % points.size]
        val d = b - a
        val length = d.getDistance()
        val n = maxOf(1, (length / step).toInt())
        val normal = Offset(-d.y / length, d.x / length)
        repeat(n) { k ->
            out += a + d * (k.toFloat() / n) + normal * ((random.nextFloat() * 2f - 1f) * jitter)
        }
    }
    return out
}

private fun List<Offset>.toScaledPath(sx: Float, sy: Float): Path =
    Path().apply {
        forEachIndexed { index, p ->
            if (index == 0) moveTo(p.x * sx, p.y * sy) else lineTo(p.x * sx, p.y * sy)
        }
        close()
    }

/** 인쇄 글줄 흉내(설계 좌표 사각형 목록). 같은 seed면 늘 같은 줄. */
private fun pouchPrintedLines(x: Float, y: Float, width: Float, count: Int, seed: Int): List<Pair<Offset, Float>> {
    val random = Random(seed)
    return List(count) { i -> Offset(x, y + i * 3.4f) to width * (0.55f + 0.45f * random.nextFloat()) }
}

private fun pouchHalftone(): List<Offset> {
    val points = mutableListOf<Offset>()
    var y = 1f
    var row = 0
    while (y < POUCH_DESIGN_HEIGHT) {
        var x = if (row % 2 == 0) 1f else 2.1f
        while (x < POUCH_DESIGN_WIDTH) {
            points += Offset(x, y)
            x += 2.2f
        }
        y += 1.9f
        row++
    }
    return points
}

private fun DrawScope.drawPrintedPaper(
    outline: Path,
    paper: Color,
    /** 이미 화면 px로 바꾼 망점. */
    halftone: List<Offset>,
    lines: List<Pair<Offset, Float>>,
    sx: Float,
    sy: Float,
    extra: DrawScope.() -> Unit = {}
) {
    translate(left = 0.8f * sx, top = 1.4f * sy) { drawPath(outline, PouchShadow, alpha = 0.12f) }
    drawPath(outline, paper)
    clipPath(outline) {
        drawPoints(
            points = halftone,
            pointMode = PointMode.Points,
            color = PouchInk,
            strokeWidth = 0.84f * sx,
            cap = StrokeCap.Round,
            alpha = 0.1f
        )
        lines.forEach { (topLeft, width) ->
            drawRect(
                color = PouchInk,
                topLeft = Offset(topLeft.x * sx, topLeft.y * sy),
                size = androidx.compose.ui.geometry.Size(width * sx, 1.1f * sy),
                alpha = 0.2f
            )
        }
        extra()
    }
}

private fun Modifier.stickerPouchBack(empty: Boolean, open: Boolean = false): Modifier = drawWithCache {
    val sx = size.width / POUCH_DESIGN_WIDTH
    val sy = size.height / POUCH_DESIGN_HEIGHT
    val top = if (open) 4f else 12f
    val outline = pouchCutOutline(
        listOf(Offset(4f, top), Offset(128f, top - 1f), Offset(127f, 84f), Offset(5f, 85f)),
        seed = 101
    ).toScaledPath(sx, sy)
    val halftone = pouchHalftone().map { Offset(it.x * sx, it.y * sy) }
    val lines = pouchPrintedLines(10f, 16f, 50f, 4, 5) + pouchPrintedLines(70f, 16f, 50f, 4, 6)
    val frontTop = if (open) 44f else 30f
    val baked = bakePouchLayer {
        drawPrintedPaper(outline, PouchBackPaper, halftone, lines, sx, sy) {
            // 입구 안쪽 그늘. 비어 있으면 조금 더 깊어 보이게.
            drawRect(
                color = PouchShadow,
                topLeft = Offset(0f, (frontTop - 8f) * sy),
                size = androidx.compose.ui.geometry.Size(size.width, 14f * sy),
                alpha = if (empty) 0.16f else 0.1f
            )
        }
    }
    onDrawBehind { drawImage(baked) }
}

private fun Modifier.stickerPouchFront(open: Boolean = false): Modifier = drawWithCache {
    val sx = size.width / POUCH_DESIGN_WIDTH
    val sy = size.height / POUCH_DESIGN_HEIGHT
    val t = if (open) 44f else 30f
    // 앞판 윗변 가운데의 얕은 엄지 홈이 입구를 알려 준다.
    val outline = pouchCutOutline(
        listOf(
            Offset(3f, t), Offset(46f, t - 1f), Offset(56f, t + 5f), Offset(66f, t + 8f),
            Offset(76f, t + 5f), Offset(86f, t - 1f), Offset(129f, t + 1f), Offset(127f, 86f), Offset(5f, 87f)
        ),
        seed = 202,
        jitter = 0.55f
    ).toScaledPath(sx, sy)
    val halftone = pouchHalftone().map { Offset(it.x * sx, it.y * sy) }
    val lines = pouchPrintedLines(12f, t + 28f, 46f, if (open) 3 else 5, 9) +
        pouchPrintedLines(72f, t + 12f, 46f, if (open) 6 else 10, 10)
    val crease = if (open) 72f else 66f
    val baked = bakePouchLayer {
        drawPrintedPaper(outline, PouchFrontPaper, halftone, lines, sx, sy) {
            // 접힌 자국: 밝은 선 + 바로 아래 옅은 그늘.
            drawRect(Color.White, Offset(0f, crease * sy), androidx.compose.ui.geometry.Size(size.width, 0.8f * sy), alpha = 0.55f)
            drawRect(PouchShadow, Offset(0f, (crease + 0.8f) * sy), androidx.compose.ui.geometry.Size(size.width, 0.7f * sy), alpha = 0.12f)
        }
    }
    onDrawBehind { drawImage(baked) }
}

/**
 * 110일차 QA(사이드바 열 때 버벅임, 프레임 통계에서 그리기 명령 지연): 망점 수천 개와 오린 외곽
 * clip을 사이드바가 움직이는 매 프레임 다시 그리지 않도록, 크기가 정해질 때 한 번만 비트맵으로
 * 구워 두고 이후엔 그림 한 장만 그린다. 크기가 바뀔 때만 다시 굽는다(drawWithCache).
 */
private fun CacheDrawScope.bakePouchLayer(block: DrawScope.() -> Unit): ImageBitmap {
    val bitmap = ImageBitmap(maxOf(1, size.width.roundToInt()), maxOf(1, size.height.roundToInt()))
    CanvasDrawScope().draw(this, layoutDirection, Canvas(bitmap), size, block)
    return bitmap
}

/** 보관함 장의 종이: 달력 장과 같은 손으로 자른 가장자리 + 옅은 그림자(임시 단색 종이). */
private fun Modifier.stickerStashSheet(): Modifier = drawWithCache {
    val outline = Path().apply {
        visitCalendarPaperEdgeOutline(
            widthDp = size.width / density,
            heightDp = size.height / density,
            seed = 110_000
        ).forEachIndexed { index, p ->
            if (index == 0) moveTo(p.x * density, p.y * density) else lineTo(p.x * density, p.y * density)
        }
        close()
    }
    onDrawBehind {
        translate(left = 0.3f * density, top = 0.8f * density) { drawPath(outline, PouchShadow, alpha = 0.06f) }
        translate(left = 0.5f * density, top = 1.4f * density) { drawPath(outline, PouchShadow, alpha = 0.03f) }
        drawPath(outline, StashSheetPaper)
    }
}

/** 고른 조각 둘레의 펜 고리(달력 오늘 표시와 같은 손그림 문법, 임시 값). */
private fun Modifier.stashPenLoop(seed: Int): Modifier = drawWithCache {
    val random = Random(seed)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rx = size.width * 0.42f
    val ry = size.height * 0.44f
    val path = Path()
    val steps = 44
    for (i in 0..steps + 4) {
        val a = i.toFloat() / steps * (2f * Math.PI.toFloat()) - 1f
        val wobble = 1f + (random.nextFloat() - 0.5f) * 0.04f
        val p = Offset(cx + kotlin.math.cos(a) * rx * wobble, cy + kotlin.math.sin(a) * ry * wobble)
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    onDrawBehind {
        drawPath(path, InkSecondary, alpha = 0.75f, style = Stroke(width = 1.1f * density, cap = StrokeCap.Round))
    }
}

private fun Modifier.stashScissorLine(): Modifier = drawBehind {
    val dash = 4f * density
    val gap = 3f * density
    val stroke = 1.2f * density
    fun dashed(from: Offset, to: Offset) {
        val d = to - from
        val length = d.getDistance()
        val dir = d / length
        var s = 0f
        while (s < length) {
            val e = minOf(s + dash, length)
            drawLine(InkSecondary, from + dir * s, from + dir * e, strokeWidth = stroke, alpha = 0.55f)
            s += dash + gap
        }
    }
    dashed(Offset(0f, 0f), Offset(size.width, 0f))
    dashed(Offset(size.width, 0f), Offset(size.width, size.height))
    dashed(Offset(size.width, size.height), Offset(0f, size.height))
    dashed(Offset(0f, size.height), Offset(0f, 0f))
}
