package com.postcardmemory.ui.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCardContent
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private val RaceTrackBase = Color(0xFFB9A489)
private val RaceTrackLane = Color(0xFFEFE6D2)
private val RaceTrackCheckerDark = Color(0xFF8A7A63)
private val RaceLightHousing = Color(0xFF4A4038)
private val RaceLightDim = Color(0xFF8A8074)

private val RaceCarColors = listOf(
    Color(0xFFB1543F), // 1번 차량 — 버건디 노을빛 빨강
    Color(0xFF7E9C7A), // 2번 차량 — 세이지 민트
    Color(0xFF6E7FA0)  // 3번 차량 — 따뜻한 파랑 회색
)

private const val TRACK_HEIGHT_FRACTION = 0.30f
private const val TRACK_SIDE_INSET_FRACTION = 0.09f
private const val AUDIENCE_ROW_SIZE = 4
private const val WAVE_CYCLE_MILLIS = 1500f
private const val WAVE_SPECTATOR_DELAY_MILLIS = 190f
private const val WAVE_BOUNCE_DP = 6f
private const val WINNER_JUMP_STAGES = 3
private const val WINNER_JUMP_RISE_MILLIS = 130
private const val WINNER_JUMP_FALL_MILLIS = 150
private const val WINNER_JUMP_BASE_DP = 11f

/** 차선별 진행 곡선 — 결승 직전 순간이동이나 역행 없이, 서로 다른 페이스로 앞서거니 뒤서거니 한다. */
private fun laneBaseProgress(laneIndex: Int, t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return when (laneIndex) {
        0 -> clamped.pow(0.6f) * 0.97f
        1 -> clamped * 0.995f
        else -> clamped.pow(1.6f)
    }
}

/** 마지막 15~25% 구간에서만 우승 차량을 완만하게 밀어준다 — 단조증가라 역행하지 않는다. */
private fun laneWinnerBoost(t: Float): Float {
    val ramp = ((t.coerceIn(0f, 1f) - 0.75f) / 0.25f).coerceIn(0f, 1f)
    return ramp * 0.09f
}

private fun laneProgress(laneIndex: Int, isWinner: Boolean, t: Float): Float {
    val base = laneBaseProgress(laneIndex, t)
    val boost = if (isWinner) laneWinnerBoost(t) else 0f
    return (base + boost).coerceIn(0f, 1.05f)
}

private fun spectatorSlot(
    index: Int,
    count: Int,
    trackLeft: Float,
    trackRight: Float,
    trackTop: Float,
    cardHeightPx: Float
): Offset {
    val row = index / AUDIENCE_ROW_SIZE
    val col = index % AUDIENCE_ROW_SIZE
    val colsInRow = if (row == 0) {
        minOf(count, AUDIENCE_ROW_SIZE)
    } else {
        (count - AUDIENCE_ROW_SIZE).coerceAtLeast(1)
    }
    val fraction = (col + 0.5f) / colsInRow.coerceAtLeast(1)
    val x = lerp(trackLeft, trackRight, fraction)
    val rowSpacing = cardHeightPx * 0.6f
    val y = (trackTop - cardHeightPx * 0.75f - row * rowSpacing).coerceAtLeast(8f)
    return Offset(x, y)
}

/**
 * 〈엽서 양떼목장 — 쫑쫑컵〉 레이스 오버레이.
 *
 * 트랙·신호등·관중·레이스카를 그리는 순수 렌더러다. 실제 단계 전환과 타이밍은
 * [SheepRanchStage]의 단일 코루틴이 관리하고, 이 컴포저블은 그 상태(raceState,
 * raceProgress, racePhaseProgress, countdownStep)를 읽어 매 프레임 위치만 계산한다.
 * 기존 SheepRanchCard의 낙하·stretch 상태는 전혀 건드리지 않는다.
 */
@Composable
fun SheepRanchRaceOverlay(
    raceState: SheepRanchRaceState,
    raceProgress: Float,
    racePhaseProgress: Float,
    countdownStep: Int,
    racers: List<Postcard>,
    spectators: List<Postcard>,
    stageWidthPx: Float,
    stageHeightPx: Float,
    cardWidthPx: Float,
    cardHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val cardWidthDp = with(density) { cardWidthPx.toDp() }
    val spectatorCardWidthPx = cardWidthPx * 0.88f
    val spectatorCardWidthDp = cardWidthDp * 0.88f

    val trackTop = stageHeightPx * (1f - TRACK_HEIGHT_FRACTION)
    val trackBottom = stageHeightPx - with(density) { 8.dp.toPx() }
    val trackLeft = stageWidthPx * TRACK_SIDE_INSET_FRACTION
    val trackRight = stageWidthPx * (1f - TRACK_SIDE_INSET_FRACTION)
    val laneHeight = (trackBottom - trackTop) / 3f
    val startLineX = trackLeft + cardWidthPx * 0.55f
    val finishLineX = trackRight - cardWidthPx * 0.55f

    fun laneCenterY(laneIndex: Int): Float = trackTop + laneHeight * (laneIndex + 0.5f)

    val waveActive = raceState.phase == SheepRanchRacePhase.RACING ||
        raceState.phase == SheepRanchRacePhase.FINISHING
    var waveClockMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(waveActive) {
        if (!waveActive) return@LaunchedEffect
        while (isActive) {
            withFrameMillis { waveClockMillis = it }
        }
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRect(
                color = RaceTrackBase.copy(alpha = 0.55f),
                topLeft = Offset(trackLeft, trackTop),
                size = Size(trackRight - trackLeft, trackBottom - trackTop)
            )
            for (lane in 1 until 3) {
                val y = trackTop + laneHeight * lane
                drawLine(
                    color = RaceTrackLane.copy(alpha = 0.5f),
                    start = Offset(trackLeft, y),
                    end = Offset(trackRight, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            val checkerBlock = 5.dp.toPx()
            listOf(startLineX, finishLineX).forEach { lineX ->
                var blockY = trackTop
                var row = 0
                while (blockY < trackBottom) {
                    val color = if (row % 2 == 0) RaceTrackLane else RaceTrackCheckerDark
                    drawRect(
                        color = color.copy(alpha = 0.65f),
                        topLeft = Offset(lineX - checkerBlock / 2f, blockY),
                        size = Size(checkerBlock, checkerBlock)
                    )
                    blockY += checkerBlock
                    row++
                }
            }
        }

        if (raceState.phase == SheepRanchRacePhase.GATHERING ||
            raceState.phase == SheepRanchRacePhase.COUNTDOWN
        ) {
            RaceTrafficLight(
                countdownStep = if (raceState.phase == SheepRanchRacePhase.COUNTDOWN) {
                    countdownStep
                } else {
                    -1
                },
                modifier = Modifier.graphicsLayer {
                    translationX = startLineX - 20.dp.toPx()
                    translationY = trackTop - 46.dp.toPx()
                }
            )
        }

        spectators.forEachIndexed { index, postcard ->
            val snapshot = raceState.snapshots[postcard.id]
            val startX = snapshot?.x ?: 0f
            val startY = snapshot?.y ?: 0f
            val slotCenter = spectatorSlot(
                index,
                spectators.size,
                trackLeft,
                trackRight,
                trackTop,
                cardHeightPx
            )
            val slotLeftX = slotCenter.x - spectatorCardWidthPx / 2f
            val (x, y) = when (raceState.phase) {
                SheepRanchRacePhase.GATHERING ->
                    lerp(startX, slotLeftX, racePhaseProgress) to
                        lerp(startY, slotCenter.y, racePhaseProgress)
                SheepRanchRacePhase.RETURNING ->
                    lerp(slotLeftX, startX, racePhaseProgress) to
                        lerp(slotCenter.y, startY, racePhaseProgress)
                SheepRanchRacePhase.IDLE -> startX to startY
                else -> slotLeftX to slotCenter.y
            }
            val rotation = snapshot?.rotationDegrees ?: 0f
            val waveBobPx = if (waveActive) {
                val localPhase = ((waveClockMillis + index * WAVE_SPECTATOR_DELAY_MILLIS) %
                    WAVE_CYCLE_MILLIS) / WAVE_CYCLE_MILLIS
                val lift = sin(localPhase * 2f * PI.toFloat()).coerceAtLeast(0f)
                with(density) { -(lift * WAVE_BOUNCE_DP).dp.toPx() }
            } else {
                0f
            }

            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .graphicsLayer {
                        translationX = x.roundToInt().toFloat()
                        translationY = (y + waveBobPx).roundToInt().toFloat()
                        rotationZ = rotation
                    }
            ) {
                Box(
                    modifier = Modifier
                        .width(spectatorCardWidthDp)
                        .background(PaperSurface, RoundedCornerShape(10.dp))
                        .border(1.dp, PaperDivider, RoundedCornerShape(10.dp))
                        .padding(5.dp)
                ) {
                    StampCardContent(
                        postcard = postcard,
                        isSelected = false,
                        dateTextSizeSp = 9
                    )
                }
            }
        }

        racers.forEachIndexed { laneIndex, postcard ->
            val snapshot = raceState.snapshots[postcard.id]
            val startX = snapshot?.x ?: 0f
            val startY = snapshot?.y ?: 0f
            val gridX = startLineX - cardWidthPx * 0.5f
            val gridY = laneCenterY(laneIndex) - cardHeightPx * 0.5f
            val isWinner = raceState.winnerId == postcard.id

            val (x, y) = when (raceState.phase) {
                SheepRanchRacePhase.GATHERING ->
                    lerp(startX, gridX, racePhaseProgress) to
                        lerp(startY, gridY, racePhaseProgress)
                SheepRanchRacePhase.COUNTDOWN -> gridX to gridY
                SheepRanchRacePhase.RACING -> {
                    val progress = laneProgress(laneIndex, isWinner, raceProgress)
                    (lerp(startLineX, finishLineX, progress) - cardWidthPx * 0.5f) to gridY
                }
                SheepRanchRacePhase.FINISHING -> {
                    val progress = laneProgress(laneIndex, isWinner, 1f)
                    (lerp(startLineX, finishLineX, progress) - cardWidthPx * 0.5f) to gridY
                }
                SheepRanchRacePhase.RETURNING -> {
                    val finishProgress = laneProgress(laneIndex, isWinner, 1f)
                    val finishX = lerp(startLineX, finishLineX, finishProgress) - cardWidthPx * 0.5f
                    lerp(finishX, startX, racePhaseProgress) to
                        lerp(gridY, startY, racePhaseProgress)
                }
                SheepRanchRacePhase.IDLE -> startX to startY
            }

            val launchScale by animateFloatAsState(
                targetValue = if (raceState.phase == SheepRanchRacePhase.COUNTDOWN) 0.93f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "raceCarLaunchScale"
            )

            val winnerJumpOffset = remember(postcard.id) { Animatable(0f) }
            LaunchedEffect(raceState.phase, raceState.sessionId) {
                if (isWinner && raceState.phase == SheepRanchRacePhase.FINISHING) {
                    val hop = with(density) { WINNER_JUMP_BASE_DP.dp.toPx() }
                    repeat(WINNER_JUMP_STAGES) { stage ->
                        val height = hop * (1f - stage * 0.28f)
                        winnerJumpOffset.animateTo(
                            -height,
                            tween(durationMillis = WINNER_JUMP_RISE_MILLIS, easing = FastOutSlowInEasing)
                        )
                        winnerJumpOffset.animateTo(
                            0f,
                            tween(durationMillis = WINNER_JUMP_FALL_MILLIS, easing = FastOutSlowInEasing)
                        )
                    }
                } else {
                    winnerJumpOffset.snapTo(0f)
                }
            }

            val bobPx = when {
                raceState.phase == SheepRanchRacePhase.RACING ->
                    with(density) { (sin(raceProgress * 46f + laneIndex * 2f) * 1.4f).dp.toPx() }
                raceState.phase == SheepRanchRacePhase.FINISHING && isWinner ->
                    winnerJumpOffset.value
                else -> 0f
            }
            val tiltDegrees = when (raceState.phase) {
                SheepRanchRacePhase.RACING -> -1.6f
                SheepRanchRacePhase.FINISHING -> 1f
                else -> 0f
            }

            Box(
                modifier = Modifier
                    .zIndex(if (isWinner && raceState.phase == SheepRanchRacePhase.FINISHING) 3f else 2f)
                    .graphicsLayer {
                        translationX = x.roundToInt().toFloat()
                        translationY = y.roundToInt().toFloat()
                    }
            ) {
                PixelPostcardRaceCar(
                    postcard = postcard,
                    bodyColor = RaceCarColors[laneIndex % RaceCarColors.size],
                    cardWidth = cardWidthDp,
                    tiltDegrees = tiltDegrees,
                    bobOffsetPx = bobPx,
                    launchScale = launchScale
                )
            }
        }

        if (raceState.phase == SheepRanchRacePhase.FINISHING) {
            Box(
                modifier = Modifier
                    .zIndex(3f)
                    .graphicsLayer {
                        translationX = (stageWidthPx / 2f) - 90.dp.toPx()
                        translationY = trackTop - 78.dp.toPx()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .background(PaperSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, PaperDivider, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "오늘의 쫑쫑 우승!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GraphiteAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun RaceTrafficLight(
    countdownStep: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(width = 18.dp, height = 48.dp)
    ) {
        val lightColors = listOf(
            Color(0xFFB8564A),
            Color(0xFFD9A441),
            Color(0xFF7E9C7A)
        )
        drawRoundRect(
            color = RaceLightHousing,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        val slotHeight = size.height / 3f
        lightColors.forEachIndexed { index, color ->
            val isLit = index == countdownStep
            drawCircle(
                color = if (isLit) color else RaceLightDim.copy(alpha = 0.5f),
                radius = slotHeight * 0.32f,
                center = Offset(size.width / 2f, slotHeight * (index + 0.5f))
            )
        }
    }
}
