package com.postcardmemory.ui.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.random.Random

private val CrownGold = Color(0xFFE8B94A)
private val CrownGoldOutline = Color(0xFF8C5F00)

/** 레이스카 차체 색 풀 — 차선이 아니라 엽서 id로 골라서, 카드마다 자기만의 색을 갖는다. */
private val RaceCarColors = listOf(
    Color(0xFFB1543F), // 버건디 노을빛 빨강
    Color(0xFF7E9C7A), // 세이지 민트
    Color(0xFF6E7FA0), // 따뜻한 파랑 회색
    Color(0xFFD9A441), // 골드
    Color(0xFFC97B84), // 더스티 로즈
    Color(0xFFA6704A), // 테라코타
    Color(0xFF8E8FBF), // 라벤더 그레이
    Color(0xFF9CAF6B)  // 올리브
)

private fun raceCarColorFor(postcardId: Long): Color {
    val rawIndex = (postcardId % RaceCarColors.size).toInt()
    val index = if (rawIndex < 0) rawIndex + RaceCarColors.size else rawIndex
    return RaceCarColors[index]
}

/** 컨페티가 고를 수 있는 전체 색 풀 — 우승 카드마다 이 중 일부만 뽑아 자기만의 색 조합을 갖는다. */
private val ConfettiPalette = listOf(
    Color(0xFFB1543F), // 버건디
    Color(0xFF7E9C7A), // 세이지 민트
    Color(0xFF6E7FA0), // 파랑 회색
    Color(0xFFD9A441), // 골드
    Color(0xFFEFE6D2), // 크림
    Color(0xFFC97B84), // 더스티 로즈
    Color(0xFFA6704A), // 테라코타
    Color(0xFF8E8FBF), // 라벤더 그레이
    Color(0xFF9CAF6B), // 올리브
    Color(0xFFE8B94A)  // 웜 옐로
)
private const val CONFETTI_THEME_SIZE = 4

private const val AUDIENCE_ROW_SIZE = 4
private const val WAVE_CYCLE_MILLIS = 1500f
private const val WAVE_SPECTATOR_DELAY_MILLIS = 190f
private const val WAVE_BOUNCE_DP = 6f
private const val WINNER_JUMP_STAGES = 3
private const val WINNER_JUMP_RISE_MILLIS = 130
private const val WINNER_JUMP_FALL_MILLIS = 150
private const val WINNER_JUMP_BASE_DP = 11f
private const val CONFETTI_COUNT = 18
private const val CONFETTI_DURATION_MILLIS = 850

/** 컨페티 한 조각의 고정된 낙하 궤적 — 레이스 시작 시 한 번만 뽑히고 프레임마다 재생성되지 않는다. */
private data class ConfettiSpec(
    val xFraction: Float,
    val color: Color,
    val sizeDp: Float,
    val fallDp: Float,
    val swayDp: Float,
    val turns: Float,
    val delayFraction: Float
)

/**
 * 우승한 엽서의 id를 시드로 써서, 카드마다 컨페티 색 조합이 달라지게 한다.
 * 같은 엽서가 다시 우승해도 늘 같은 조합이 나오는 건 우연이 아니라 "그 카드의 색"이라
 * 의도한 동작이다.
 */
private fun buildConfetti(cardSeed: Long): List<ConfettiSpec> {
    val theme = ConfettiPalette.shuffled(Random(cardSeed)).take(CONFETTI_THEME_SIZE)
    val random = Random(cardSeed * 31L + 7L)
    return List(CONFETTI_COUNT) {
        ConfettiSpec(
            xFraction = random.nextFloat(),
            color = theme[random.nextInt(theme.size)],
            sizeDp = 3f + random.nextFloat() * 3f,
            fallDp = 44f + random.nextFloat() * 38f,
            swayDp = 5f + random.nextFloat() * 9f,
            turns = 0.6f + random.nextFloat() * 1.3f,
            delayFraction = random.nextFloat() * 0.35f
        )
    }
}

/** 차선별 진행 곡선 — 결승 직전 순간이동이나 역행 없이, 서로 다른 페이스로 앞서거니 뒤서거니 한다. */
private fun laneBaseProgress(laneIndex: Int, t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return when (laneIndex) {
        0 -> clamped.pow(0.6f) * 0.97f
        1 -> clamped * 0.995f
        else -> clamped.pow(1.6f)
    }
}

/**
 * 마지막 30% 구간에서만 우승 차량을 밀어준다. 시작·끝 지점에서 속도(미분)가 0이 되는
 * smoothstep 곡선을 써서, 갑자기 툭 튀어나오지 않고 서서히 붙었다가 서서히 빠지게 한다.
 */
private fun laneWinnerBoost(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    val ramp = ((clamped - 0.7f) / 0.3f).coerceIn(0f, 1f)
    val smoothRamp = ramp * ramp * (3f - 2f * ramp)
    return smoothRamp * 0.06f
}

private fun laneProgress(laneIndex: Int, isWinner: Boolean, t: Float): Float {
    val base = laneBaseProgress(laneIndex, t)
    val boost = if (isWinner) laneWinnerBoost(t) else 0f
    return (base + boost).coerceIn(0f, 1.08f)
}

private fun spectatorSlot(
    index: Int,
    count: Int,
    trackLeft: Float,
    trackRight: Float,
    trackTop: Float,
    cardHeightPx: Float,
    jitter: Offset
): Offset {
    val row = index / AUDIENCE_ROW_SIZE
    val col = index % AUDIENCE_ROW_SIZE
    val colsInRow = if (row == 0) {
        minOf(count, AUDIENCE_ROW_SIZE)
    } else {
        (count - AUDIENCE_ROW_SIZE).coerceAtLeast(1)
    }
    val fraction = (col + 0.5f) / colsInRow.coerceAtLeast(1)
    val x = lerp(trackLeft, trackRight, fraction) + jitter.x
    val rowSpacing = cardHeightPx * 0.6f
    val y = (trackTop - cardHeightPx * 0.75f - row * rowSpacing + jitter.y).coerceAtLeast(8f)
    return Offset(x, y)
}

/** 관중 자리별 흔들림 — 레이스 시작 시 한 번만 뽑혀 매번 자리 배치가 조금씩 달라 보이게 한다. */
private fun buildSpectatorJitter(seed: Int, count: Int, cardHeightPx: Float): List<Offset> {
    val random = Random(seed)
    val rangeX = cardHeightPx * 0.5f
    val rangeY = cardHeightPx * 0.28f
    return List(count) {
        Offset(
            (random.nextFloat() - 0.5f) * rangeX,
            (random.nextFloat() - 0.5f) * rangeY
        )
    }
}

/**
 * 〈엽서 양떼목장 — 쫑쫑컵〉 레이스 오버레이.
 *
 * 출전자·관중 카드와 결승 연출을 그리는 동적 렌더러다. 상시 트랙·연석·신호등처럼
 * IDLE 상태에도 떠 있는 배경 소품은 [SheepRanchRaceVenue]가 맡고, 이 오버레이는
 * [SheepRanchRaceVenue]와 같은 [computeRaceTrackGeometry] 좌표를 공유해 정렬이
 * 어긋나지 않게 한다. 실제 단계 전환과 타이밍은 [SheepRanchStage]의 단일 코루틴이
 * 관리하고, 이 컴포저블은 그 상태(raceState, raceProgress, racePhaseProgress)를
 * 읽어 매 프레임 위치만 계산한다. 기존 SheepRanchCard의 낙하·stretch 상태는 전혀
 * 건드리지 않는다.
 */
@Composable
fun SheepRanchRaceOverlay(
    raceState: SheepRanchRaceState,
    raceProgress: Float,
    racePhaseProgress: Float,
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
    val cardHeightDp = with(density) { cardHeightPx.toDp() }
    val spectatorCardWidthPx = cardWidthPx * 0.88f
    val spectatorCardWidthDp = cardWidthDp * 0.88f

    val geometry = remember(stageWidthPx, stageHeightPx, cardWidthPx) {
        computeRaceTrackGeometry(
            stageWidthPx = stageWidthPx,
            stageHeightPx = stageHeightPx,
            cardWidthPx = cardWidthPx,
            bottomMarginPx = with(density) { RACE_TRACK_BOTTOM_MARGIN_DP.dp.toPx() }
        )
    }
    val trackTop = geometry.trackTop
    val trackLeft = geometry.trackLeft
    val trackRight = geometry.trackRight
    val startLineX = geometry.startLineX
    val finishLineX = geometry.finishLineX

    fun laneCenterY(laneIndex: Int): Float = geometry.laneCenterY(laneIndex)

    val waveActive = raceState.phase == SheepRanchRacePhase.RACING ||
        raceState.phase == SheepRanchRacePhase.FINISHING
    var waveClockMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(waveActive) {
        if (!waveActive) return@LaunchedEffect
        while (isActive) {
            withFrameMillis { waveClockMillis = it }
        }
    }

    val spectatorJitter = remember(raceState.sessionId, spectators.size) {
        buildSpectatorJitter(raceState.sessionId, spectators.size, cardHeightPx)
    }

    Box(modifier = modifier) {
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
                cardHeightPx,
                spectatorJitter.getOrElse(index) { Offset.Zero }
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

            val confettiSpecs = remember(postcard.id) { buildConfetti(postcard.id) }
            val confettiProgress = remember(postcard.id) { Animatable(0f) }
            LaunchedEffect(raceState.phase, raceState.sessionId) {
                if (isWinner && raceState.phase == SheepRanchRacePhase.FINISHING) {
                    confettiProgress.snapTo(0f)
                    confettiProgress.animateTo(
                        1f,
                        tween(durationMillis = CONFETTI_DURATION_MILLIS, easing = LinearEasing)
                    )
                } else {
                    confettiProgress.snapTo(0f)
                }
            }
            val crownScale by animateFloatAsState(
                targetValue = if (isWinner && raceState.phase == SheepRanchRacePhase.FINISHING) {
                    1f
                } else {
                    0f
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "raceCrownScale"
            )

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
                    .width(cardWidthDp)
                    .zIndex(if (isWinner && raceState.phase == SheepRanchRacePhase.FINISHING) 3f else 2f)
                    .graphicsLayer {
                        translationX = x.roundToInt().toFloat()
                        translationY = y.roundToInt().toFloat()
                    }
            ) {
                PixelPostcardRaceCar(
                    postcard = postcard,
                    bodyColor = raceCarColorFor(postcard.id),
                    cardWidth = cardWidthDp,
                    tiltDegrees = tiltDegrees,
                    bobOffsetPx = bobPx,
                    launchScale = launchScale
                )

                if (isWinner && confettiProgress.value > 0f) {
                    RaceConfetti(
                        specs = confettiSpecs,
                        progress = confettiProgress.value,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = -cardHeightDp * 0.5f)
                            .size(width = cardWidthDp * 2.4f, height = cardHeightDp * 1.7f)
                    )
                }
                if (isWinner && crownScale > 0.01f) {
                    PixelCrown(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = -cardHeightDp * 0.4f)
                            .graphicsLayer {
                                scaleX = crownScale
                                scaleY = crownScale
                            }
                    )
                }
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

/** 우승 엽서 위에 잠깐 뜨는 작은 픽셀 왕관 — 스프링 스케일로 통통 튀며 나타난다. */
@Composable
private fun PixelCrown(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 22.dp, height = 14.dp)) {
        val blockPx = size.width / 5f
        val spikeHeight = size.height * 0.34f
        val bandHeight = size.height - spikeHeight
        listOf(0, 2, 4).forEach { col ->
            drawRect(
                color = CrownGold,
                topLeft = Offset(col * blockPx, 0f),
                size = Size(blockPx, spikeHeight)
            )
        }
        drawRect(
            color = CrownGold,
            topLeft = Offset(0f, spikeHeight),
            size = Size(size.width, bandHeight * 0.55f)
        )
        drawRect(
            color = CrownGoldOutline,
            topLeft = Offset(0f, spikeHeight + bandHeight * 0.55f),
            size = Size(size.width, bandHeight * 0.45f)
        )
    }
}

/** 우승 순간에만 짧게 터지는 컨페티 — 조각별 궤적은 레이스 시작 시 한 번만 뽑히고, 프레임마다는 진행률만 읽는다. */
@Composable
private fun RaceConfetti(
    specs: List<ConfettiSpec>,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        specs.forEach { spec ->
            val localT = ((progress - spec.delayFraction) / (1f - spec.delayFraction))
                .coerceIn(0f, 1f)
            if (localT <= 0f) return@forEach

            val alpha = if (localT > 0.7f) {
                (1f - (localT - 0.7f) / 0.3f).coerceIn(0f, 1f)
            } else {
                1f
            }
            if (alpha <= 0f) return@forEach

            val fallPx = spec.fallDp.dp.toPx() * localT
            val swayPx = sin(localT * PI.toFloat() * spec.turns * 2f) * spec.swayDp.dp.toPx()
            val cx = size.width * spec.xFraction + swayPx
            val cy = fallPx
            val half = spec.sizeDp.dp.toPx() / 2f

            rotate(degrees = localT * spec.turns * 360f, pivot = Offset(cx, cy)) {
                drawRect(
                    color = spec.color.copy(alpha = alpha),
                    topLeft = Offset(cx - half, cy - half),
                    size = Size(half * 2f, half * 2f)
                )
            }
        }
    }
}
