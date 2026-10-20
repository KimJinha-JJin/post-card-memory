package com.postcardmemory.ui.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCardContent
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

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
private const val CONFETTI_COUNT = 18
private const val CONFETTI_DURATION_MILLIS = 850

/** 레이스 중 레이스카에 탄 엽서(차 포함)는 대기 우표보다 이만큼 작게 달린다. */
private const val RACE_RACER_SCALE = 0.75f

/**
 * 출전 엽서+레이스카 한 덩어리가 차선 한가운데에 오도록 그림만 위로 올리는 양.
 * 차선 좌표·주행 경로는 그대로이고, 모이기·복귀 보간도 이 높이를 기준으로 한다.
 */
private const val RACE_RACER_LIFT_DP = 24f

/**
 * 우승자 전광판의 세로 위치 — 경기장·관중석보다 훨씬 위, 무대 상단 쪽에 걸린다.
 * 출발 신호등도 같은 자리에 걸리고, 전광판이 내려오는 동안에는 숨는다.
 */
internal const val RACE_RESULT_TOP_FRACTION = 0.08f

/** 전광판: 바랜 먹색 판, 크림색 글씨, 둘레를 따라 차례로 켜지는 전구. */
private val ScoreboardPanel = Color(0xFF3B342E)
private val ScoreboardText = Color(0xFFF3E8D4)
private val ScoreboardBulbOn = Color(0xFFE8C66A)
private val ScoreboardBulbOff = Color(0xFF6B5D4A)
private val ScoreboardWire = Color(0xFF7C7266)
private const val SCOREBOARD_CHASE_STEP_MILLIS = 120L
private const val SCOREBOARD_DROP_MILLIS = 260

/** 관중 엽서는 출전 카드보다 조금 작게, 사진만 보인다. 관중석 그림도 같은 값을 쓴다. */
internal const val RACE_SPECTATOR_CARD_SCALE = 0.88f
internal val RaceSpectatorPhotoInset = 5.dp

/** 관중 [row]번째 줄 엽서 윗변의 기준 높이(흔들림 전). 관중석 그림과 관중 배치가 함께 쓴다. */
internal fun raceSpectatorRowTop(row: Int, trackTop: Float, cardHeightPx: Float): Float =
    trackTop - cardHeightPx * 0.75f - row * cardHeightPx * 0.6f

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
    val y = (raceSpectatorRowTop(row, trackTop, cardHeightPx) + jitter.y).coerceAtLeast(8f)
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
 * 〈엽서 쫑쫑컵〉 레이스 오버레이.
 *
 * 출전자·관중 카드와 결승 연출을 그리는 동적 렌더러다. 상시 트랙·연석·신호등처럼
 * IDLE 상태에도 떠 있는 배경 소품은 [PostcardRaceVenue]가 맡고, 이 오버레이는
 * [PostcardRaceVenue]와 같은 [computeRaceTrackGeometry] 좌표를 공유해 정렬이
 * 어긋나지 않게 한다. 실제 단계 전환과 타이밍은 [GalleryPlayStage]의 단일 코루틴이
 * 관리하고, 이 컴포저블은 그 상태(raceState, raceProgress, racePhaseProgress)를
 * 읽어 매 프레임 위치만 계산한다. 기존 GalleryPlayStageCard의 낙하·stretch 상태는 전혀
 * 건드리지 않는다.
 */
@Composable
fun PostcardRaceOverlay(
    raceState: PostcardRaceState,
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
    val spectatorCardWidthPx = cardWidthPx * RACE_SPECTATOR_CARD_SCALE
    val spectatorCardWidthDp = cardWidthDp * RACE_SPECTATOR_CARD_SCALE
    val photoTopDp = raceCardPhotoTop(cardWidthDp, cardHeightDp)
    val photoInsetPx = with(density) { RaceCardPhotoInset.toPx() }
    val idlePhotoPx = cardWidthPx - photoInsetPx * 2f
    // 관중 사진(정사각 상자 안 여백 5dp)은 대기 우표의 바닥 중심에서, 대기 우표 크기로 출발한다.
    val spectatorInsetPx = with(density) { RaceSpectatorPhotoInset.toPx() }
    val spectatorPhotoPx = spectatorCardWidthPx - spectatorInsetPx * 2f
    val spectatorStartShiftX = cardWidthPx / 2f - spectatorCardWidthPx / 2f
    val spectatorStartShiftY = (cardHeightPx - photoInsetPx) - (spectatorCardWidthPx - spectatorInsetPx)
    val spectatorPivotY = (spectatorCardWidthPx - spectatorInsetPx) / spectatorCardWidthPx
    // 출전 엽서는 사진 중심을 축으로 줄어들어, 크기가 바뀌어도 차선 위 자리가 그대로다.
    val racerPivotY = (cardHeightPx - photoInsetPx - idlePhotoPx / 2f) / cardHeightPx

    /** 모이는 동안 대기 우표 크기([idleScale])에서 레이스 크기(1)로 바뀌고, 복귀하며 되돌아간다. */
    fun entryScale(idleScale: Float, raceScale: Float): Float = when (raceState.phase) {
        PostcardRacePhase.GATHERING -> lerp(idleScale, raceScale, racePhaseProgress)
        PostcardRacePhase.RETURNING -> lerp(raceScale, idleScale, racePhaseProgress)
        PostcardRacePhase.IDLE -> idleScale
        else -> raceScale
    }
    val spectatorScale = entryScale(idleScale = idlePhotoPx / spectatorPhotoPx, raceScale = 1f)
    val racerScale = entryScale(idleScale = 1f, raceScale = RACE_RACER_SCALE)

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

    val waveActive = raceState.phase == PostcardRacePhase.RACING ||
        raceState.phase == PostcardRacePhase.FINISHING
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
            val startX = snapshot?.let { it.x + spectatorStartShiftX } ?: 0f
            val startY = snapshot?.let { it.y + spectatorStartShiftY } ?: 0f
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
                PostcardRacePhase.GATHERING ->
                    lerp(startX, slotLeftX, racePhaseProgress) to
                        lerp(startY, slotCenter.y, racePhaseProgress)
                PostcardRacePhase.RETURNING ->
                    lerp(slotLeftX, startX, racePhaseProgress) to
                        lerp(slotCenter.y, startY, racePhaseProgress)
                PostcardRacePhase.IDLE -> startX to startY
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
                        transformOrigin = TransformOrigin(0.5f, spectatorPivotY)
                        scaleX = spectatorScale
                        scaleY = spectatorScale
                    }
            ) {
                StampCardContent(
                    postcard = postcard,
                    isSelected = false,
                    dateLabelOverride = "",
                    modifier = Modifier
                        .width(spectatorCardWidthDp)
                        .padding(RaceSpectatorPhotoInset)
                )
            }
        }

        racers.forEachIndexed { laneIndex, postcard ->
            val snapshot = raceState.snapshots[postcard.id]
            val startX = snapshot?.x ?: 0f
            val startY = snapshot?.y ?: 0f
            val gridX = startLineX - cardWidthPx * 0.5f
            val gridY = laneCenterY(laneIndex) - cardHeightPx * 0.5f -
                with(density) { RACE_RACER_LIFT_DP.dp.toPx() }
            val isWinner = raceState.winnerId == postcard.id

            val (x, y) = when (raceState.phase) {
                PostcardRacePhase.GATHERING ->
                    lerp(startX, gridX, racePhaseProgress) to
                        lerp(startY, gridY, racePhaseProgress)
                PostcardRacePhase.COUNTDOWN -> gridX to gridY
                PostcardRacePhase.RACING -> {
                    val progress = laneProgress(laneIndex, isWinner, raceProgress)
                    (lerp(startLineX, finishLineX, progress) - cardWidthPx * 0.5f) to gridY
                }
                PostcardRacePhase.FINISHING -> {
                    val progress = laneProgress(laneIndex, isWinner, 1f)
                    (lerp(startLineX, finishLineX, progress) - cardWidthPx * 0.5f) to gridY
                }
                PostcardRacePhase.RETURNING -> {
                    val finishProgress = laneProgress(laneIndex, isWinner, 1f)
                    val finishX = lerp(startLineX, finishLineX, finishProgress) - cardWidthPx * 0.5f
                    lerp(finishX, startX, racePhaseProgress) to
                        lerp(gridY, startY, racePhaseProgress)
                }
                PostcardRacePhase.IDLE -> startX to startY
            }

            val launchScale by animateFloatAsState(
                targetValue = if (raceState.phase == PostcardRacePhase.COUNTDOWN) 0.93f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "raceCarLaunchScale"
            )

            val confettiSpecs = remember(postcard.id) { buildConfetti(postcard.id) }
            val confettiProgress = remember(postcard.id) { Animatable(0f) }
            LaunchedEffect(raceState.phase, raceState.sessionId) {
                if (isWinner && raceState.phase == PostcardRacePhase.FINISHING) {
                    confettiProgress.snapTo(0f)
                    confettiProgress.animateTo(
                        1f,
                        tween(durationMillis = CONFETTI_DURATION_MILLIS, easing = LinearEasing)
                    )
                } else {
                    confettiProgress.snapTo(0f)
                }
            }
            val bobPx = when {
                raceState.phase == PostcardRacePhase.RACING ->
                    with(density) { (sin(raceProgress * 46f + laneIndex * 2f) * 1.4f).dp.toPx() }
                else -> 0f
            }
            val tiltDegrees = when (raceState.phase) {
                PostcardRacePhase.RACING -> -1.6f
                PostcardRacePhase.FINISHING -> 1f
                else -> 0f
            }

            Box(
                modifier = Modifier
                    .width(cardWidthDp)
                    .zIndex(if (isWinner && raceState.phase == PostcardRacePhase.FINISHING) 3f else 2f)
                    .graphicsLayer {
                        translationX = x.roundToInt().toFloat()
                        translationY = y.roundToInt().toFloat()
                        transformOrigin = TransformOrigin(0.5f, racerPivotY)
                        scaleX = racerScale
                        scaleY = racerScale
                    }
            ) {
                PixelPostcardRaceCar(
                    postcard = postcard,
                    bodyColor = raceCarColorFor(postcard.id),
                    cardWidth = cardWidthDp,
                    cardHeight = cardHeightDp,
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
                            .offset(y = photoTopDp - cardHeightDp * 0.5f)
                            .size(width = cardWidthDp * 2.4f, height = cardHeightDp * 1.7f)
                    )
                }
            }
        }

        val winner = racers.find { it.id == raceState.winnerId }
        val scoreboardVisible = raceState.phase == PostcardRacePhase.FINISHING ||
            raceState.phase == PostcardRacePhase.RETURNING
        if (winner != null && scoreboardVisible) {
            // 결승 순간 위에서 내려와 복귀하는 동안 걸려 있다가, 복귀 끝 무렵 꺼진다.
            // 가로 위치는 무대 상단 가운데 정렬에 맡겨서 글꼴 크기가 바뀌어도 가운데에 온다.
            val fadeOut = if (raceState.phase == PostcardRacePhase.RETURNING) {
                (1f - (racePhaseProgress - 0.7f) / 0.3f).coerceIn(0f, 1f)
            } else {
                1f
            }
            RaceWinnerScoreboard(
                winner = winner,
                sessionId = raceState.sessionId,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(3f)
                    .graphicsLayer {
                        translationY = stageHeightPx * RACE_RESULT_TOP_FRACTION
                        alpha = fadeOut
                    }
            )
        }
    }
}

/**
 * 무대 상단에 매달린 "오늘의 우승자" 전광판. 우승 엽서 사진을 가운데 띄우고, 판 둘레의
 * 전구가 차례로 켜졌다 꺼지며 돈다. 처음 나타날 때 위에서 툭 내려온다. 왕관·점프 대신
 * 이 전광판 하나로 우승을 알린다.
 */
@Composable
private fun RaceWinnerScoreboard(
    winner: Postcard,
    sessionId: Int,
    modifier: Modifier = Modifier
) {
    val drop = remember(sessionId) { Animatable(0f) }
    LaunchedEffect(sessionId) {
        drop.animateTo(1f, tween(durationMillis = SCOREBOARD_DROP_MILLIS, easing = FastOutSlowInEasing))
    }
    var chaseStep by remember { mutableLongStateOf(0L) }
    LaunchedEffect(sessionId) {
        val start = withFrameMillis { it }
        while (isActive) {
            withFrameMillis { chaseStep = (it - start) / SCOREBOARD_CHASE_STEP_MILLIS }
        }
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                translationY = -(1f - drop.value) * 40.dp.toPx()
            }
            .drawBehind {
                val dp = density
                // 무대 위쪽으로 이어지는 두 가닥 매달림 줄.
                listOf(size.width * 0.18f, size.width * 0.82f).forEach { wireX ->
                    drawLine(
                        color = ScoreboardWire.copy(alpha = 0.6f),
                        start = Offset(wireX, 0f),
                        end = Offset(wireX, -size.height),
                        strokeWidth = 1.2f * dp
                    )
                }
                drawRect(color = ScoreboardPanel)
                val bulbStep = 7f * dp
                val bulbRadius = 1.6f * dp
                val edge = 4f * dp
                val w = size.width
                val h = size.height
                val bulbs = buildList {
                    var x = edge
                    while (x <= w - edge) {
                        add(Offset(x, edge))
                        add(Offset(w - x, h - edge))
                        x += bulbStep
                    }
                    var y = edge + bulbStep
                    while (y <= h - edge - bulbStep) {
                        add(Offset(w - edge, y))
                        add(Offset(edge, h - y))
                        y += bulbStep
                    }
                }
                bulbs.forEachIndexed { index, center ->
                    val lit = (index / 2 + chaseStep) % 3 == 0L
                    drawCircle(
                        color = if (lit) ScoreboardBulbOn else ScoreboardBulbOff.copy(alpha = 0.8f),
                        radius = bulbRadius,
                        center = center
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "오늘의 우승자",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = ScoreboardText
            )
            StampCardContent(
                postcard = winner,
                isSelected = false,
                dateLabelOverride = "",
                modifier = Modifier
                    .padding(top = 6.dp)
                    .width(60.dp)
            )
        }
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
