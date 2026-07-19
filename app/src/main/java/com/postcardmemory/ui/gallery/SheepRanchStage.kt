package com.postcardmemory.ui.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampCardContent
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private val RanchSage = Color(0xFFDDE8D5)
private val RanchGrass = Color(0xFF8BA37C)
private val RanchShadow = Color(0xFF40382F)
private val RanchStagePadding = 18.dp
private val RanchBottomReserved = 92.dp
private val RanchCardCorner = 12.dp
private const val RANCH_MAX_LIFT_FRACTION = 0.62f
private const val RANCH_DRAG_SCALE = 1.04f
private const val RANCH_MAX_DEPTH_SCALE = 0.86f
private const val RANCH_MAX_ROTATION_X = 6f
private const val RANCH_MAX_ROTATION_Y = 3f
private const val RANCH_SHADOW_MIN_SCALE = 0.42f
private const val RANCH_SHADOW_MIN_ALPHA = 0.055f
private const val RANCH_SHADOW_PARALLAX = 0.88f
private const val RANCH_TAP_PRESS_SCALE = 0.97f
private const val RANCH_TAP_BOUNCE_SCALE = 1.035f
private const val RANCH_MAX_STRETCH_X = 1.4f
private const val RANCH_MAX_STRETCH_Y = 1.32f
private const val RANCH_STRETCH_RESISTANCE = 1.55f
private const val RANCH_STRETCH_CENTER_FOLLOW = 0.28f
private const val RACE_MIN_POSTCARDS = 3
private const val RACE_MAX_RACERS = 3
private const val RACE_GATHER_MILLIS = 850
private const val RACE_COUNTDOWN_STEP_MILLIS = 500
private const val RACE_COUNTDOWN_GO_MILLIS = 350
private const val RACE_DURATION_MILLIS = 4200
private const val RACE_FINISH_HOLD_MILLIS = 900
private const val RACE_RETURN_MILLIS = 800
private const val RACE_NOT_ENOUGH_HINT_MILLIS = 1800L

/**
 * 쫑쫑컵 모드의 IDLE 상태에서 카드가 평소 돌아다니는 구역.
 * 양떼목장(raceEnabled = false)에서는 항상 [FULL_RANCH]로, 기존 자유 배회와 동일하다.
 */
private enum class RanchMovementZone {
    FULL_RANCH,
    RACE_PADDOCK,
    RACE_SPECTATOR
}

private fun movementZoneFor(index: Int, raceEnabled: Boolean): RanchMovementZone {
    if (!raceEnabled) return RanchMovementZone.FULL_RANCH
    return if (index < RACE_MAX_RACERS) {
        RanchMovementZone.RACE_PADDOCK
    } else {
        RanchMovementZone.RACE_SPECTATOR
    }
}

/** 구역별 좌우 배회 범위 — 패독은 출발선 쪽 절반으로, 관중은 기존 폭 그대로 쓴다. */
private fun zoneBoundsX(
    zone: RanchMovementZone,
    stagePaddingPx: Float,
    maxX: Float
): Pair<Float, Float> {
    return when (zone) {
        RanchMovementZone.FULL_RANCH -> stagePaddingPx to maxX
        RanchMovementZone.RACE_PADDOCK -> stagePaddingPx to lerp(stagePaddingPx, maxX, 0.5f)
        RanchMovementZone.RACE_SPECTATOR -> stagePaddingPx to maxX
    }
}

private fun ranchFloorY(
    spec: SheepRanchCardSpec,
    stageHeightPx: Float,
    cardHeightPx: Float,
    bottomReservedPx: Float,
    stagePaddingPx: Float,
    zone: RanchMovementZone
): Float {
    val fullMin = (stageHeightPx * 0.60f).coerceAtLeast(stagePaddingPx)
    val fullMax = (stageHeightPx - bottomReservedPx - cardHeightPx)
        .coerceAtLeast(fullMin)
    val (bandMin, bandMax) = when (zone) {
        RanchMovementZone.FULL_RANCH -> fullMin to fullMax
        // 패독: 트랙·그리드와 같은 하단부에 머물도록 아래쪽으로 붙인다.
        RanchMovementZone.RACE_PADDOCK -> lerp(fullMin, fullMax, 0.62f) to fullMax
        // 관중: 트랙 위 중단부(펜스 쪽)에 머물도록 위쪽으로 붙인다.
        RanchMovementZone.RACE_SPECTATOR -> fullMin to lerp(fullMin, fullMax, 0.55f)
    }
    return lerp(bandMin, bandMax, spec.startJitter)
}

@Composable
fun SheepRanchStage(
    postcards: List<Postcard>,
    paddingValues: PaddingValues,
    onPostcardClick: (Long) -> Unit,
    raceEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val positions = remember(postcards.map { it.id }) {
        mutableStateMapOf<Long, Float>()
    }
    val stretchingCardId = remember {
        mutableStateOf<Long?>(null)
    }
    val busyCardId = remember {
        mutableStateOf<Long?>(null)
    }

    var raceState by remember { mutableStateOf(SheepRanchRaceState()) }
    val raceProgress = remember { Animatable(0f) }
    val racePhaseProgress = remember { Animatable(0f) }
    var raceCountdownStep by remember { mutableIntStateOf(0) }
    var showNotEnoughHint by remember { mutableStateOf(false) }
    val raceActive = raceState.phase != SheepRanchRacePhase.IDLE

    LaunchedEffect(showNotEnoughHint) {
        if (showNotEnoughHint) {
            delay(RACE_NOT_ENOUGH_HINT_MILLIS)
            showNotEnoughHint = false
        }
    }

    LaunchedEffect(raceState.sessionId) {
        if (raceState.phase != SheepRanchRacePhase.GATHERING) {
            return@LaunchedEffect
        }
        try {
            racePhaseProgress.snapTo(0f)
            racePhaseProgress.animateTo(
                1f,
                tween(durationMillis = RACE_GATHER_MILLIS, easing = FastOutSlowInEasing)
            )

            raceState = raceState.copy(phase = SheepRanchRacePhase.COUNTDOWN)
            raceCountdownStep = 0
            delay(RACE_COUNTDOWN_STEP_MILLIS.milliseconds)
            raceCountdownStep = 1
            delay(RACE_COUNTDOWN_STEP_MILLIS.milliseconds)
            raceCountdownStep = 2
            delay(RACE_COUNTDOWN_GO_MILLIS.milliseconds)

            raceState = raceState.copy(phase = SheepRanchRacePhase.RACING)
            raceProgress.snapTo(0f)
            raceProgress.animateTo(
                1f,
                tween(durationMillis = RACE_DURATION_MILLIS, easing = LinearEasing)
            )

            raceState = raceState.copy(phase = SheepRanchRacePhase.FINISHING)
            delay(RACE_FINISH_HOLD_MILLIS.milliseconds)

            raceState = raceState.copy(phase = SheepRanchRacePhase.RETURNING)
            racePhaseProgress.snapTo(0f)
            racePhaseProgress.animateTo(
                1f,
                tween(durationMillis = RACE_RETURN_MILLIS, easing = FastOutSlowInEasing)
            )
        } finally {
            raceState = SheepRanchRaceState(sessionId = raceState.sessionId)
            raceCountdownStep = 0
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
            .padding(paddingValues)
    ) {
        val density = LocalDensity.current
        val stageWidthPx = constraints.maxWidth.toFloat()
        val stageHeightPx = constraints.maxHeight.toFloat()
        val cardWidth = when {
            postcards.size <= 3 -> 92.dp
            postcards.size <= 6 -> 84.dp
            else -> 76.dp
        }
        val cardWidthPx = with(density) { cardWidth.toPx() }
        val cardHeightPx = with(density) { (cardWidth + 36.dp).toPx() }
        val stagePaddingPx = with(density) { RanchStagePadding.toPx() }
        val bottomReservedPx = with(density) { RanchBottomReserved.toPx() }
        val stageMaxX = (stageWidthPx - cardWidthPx - stagePaddingPx).coerceAtLeast(stagePaddingPx)

        fun isPointOverAnyCard(point: Offset): Boolean {
            val marginPx = with(density) { 14.dp.toPx() }
            postcards.forEachIndexed { index, pc ->
                val spec = createSheepRanchCardSpec(pc.id, index, postcards.size)
                val zone = movementZoneFor(index, raceEnabled)
                val (zoneMinX, zoneMaxX) = zoneBoundsX(zone, stagePaddingPx, stageMaxX)
                val left = positions[pc.id] ?: lerp(zoneMinX, zoneMaxX, spec.startBias)
                val top = ranchFloorY(spec, stageHeightPx, cardHeightPx, bottomReservedPx, stagePaddingPx, zone)
                val rect = Rect(
                    left - marginPx,
                    top - marginPx,
                    left + cardWidthPx + marginPx,
                    top + cardHeightPx + marginPx
                )
                if (rect.contains(point)) return true
            }
            return false
        }

        fun attemptStartRace(tapOffset: Offset) {
            if (!raceEnabled) return
            if (raceState.phase != SheepRanchRacePhase.IDLE) return
            if (postcards.size < RACE_MIN_POSTCARDS) {
                showNotEnoughHint = true
                return
            }
            if (stretchingCardId.value != null || busyCardId.value != null) return
            if (isPointOverAnyCard(tapOffset)) return

            val racers = postcards.take(RACE_MAX_RACERS)
            val spectators = postcards.drop(RACE_MAX_RACERS)
            val snapshots = buildMap {
                postcards.forEachIndexed { index, pc ->
                    val spec = createSheepRanchCardSpec(pc.id, index, postcards.size)
                    val zone = movementZoneFor(index, raceEnabled)
                    val (zoneMinX, zoneMaxX) = zoneBoundsX(zone, stagePaddingPx, stageMaxX)
                    val x = positions[pc.id] ?: lerp(zoneMinX, zoneMaxX, spec.startBias)
                    val y = ranchFloorY(spec, stageHeightPx, cardHeightPx, bottomReservedPx, stagePaddingPx, zone)
                    put(pc.id, SheepRanchCardSnapshot(pc.id, x, y, spec.baseRotation))
                }
            }
            val nextSessionId = raceState.sessionId + 1
            val racerIds = racers.map { it.id }

            raceState = SheepRanchRaceState(
                phase = SheepRanchRacePhase.GATHERING,
                racerIds = racerIds,
                spectatorIds = spectators.map { it.id },
                winnerId = racerIds[Random.nextInt(racerIds.size)],
                sessionId = nextSessionId,
                snapshots = snapshots
            )
        }

        SheepRanchCloudLayer(
            modifier = Modifier.matchParentSize()
        )

        RanchBackground(
            modifier = Modifier.matchParentSize()
        )

        if (raceEnabled) {
            SheepRanchRaceVenue(
                raceState = raceState,
                racePhaseProgress = racePhaseProgress.value,
                raceProgress = raceProgress.value,
                countdownStep = raceCountdownStep,
                stageWidthPx = stageWidthPx,
                stageHeightPx = stageHeightPx,
                cardWidthPx = cardWidthPx,
                modifier = Modifier.matchParentSize()
            )
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(postcards.map { it.id }, raceState.phase) {
                    detectTapGestures(
                        onDoubleTap = { offset -> attemptStartRace(offset) }
                    )
                }
        ) {}

        if (postcards.isEmpty()) {
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "새 엽서를 만들면 이곳을 뛰어다녀요.",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GraphiteAccent,
                    textAlign = TextAlign.Center
                )
            }
        }

        postcards.forEachIndexed { index, postcard ->
            SheepRanchCard(
                postcard = postcard,
                index = index,
                count = postcards.size,
                cardWidthPx = cardWidthPx,
                cardHeightPx = cardHeightPx,
                stageWidthPx = stageWidthPx,
                stageHeightPx = stageHeightPx,
                stagePaddingPx = stagePaddingPx,
                bottomReservedPx = bottomReservedPx,
                positions = positions,
                stretchingCardId = stretchingCardId,
                busyCardId = busyCardId,
                raceActive = raceActive,
                zone = movementZoneFor(index, raceEnabled),
                onPostcardClick = onPostcardClick,
                modifier = Modifier.size(width = cardWidth, height = cardWidth + 36.dp)
            )
        }

        if (raceActive) {
            val racerPostcards = remember(raceState.racerIds, postcards) {
                raceState.racerIds.mapNotNull { id -> postcards.find { it.id == id } }
            }
            val spectatorPostcards = remember(raceState.spectatorIds, postcards) {
                raceState.spectatorIds.mapNotNull { id -> postcards.find { it.id == id } }
            }

            SheepRanchRaceOverlay(
                raceState = raceState,
                raceProgress = raceProgress.value,
                racePhaseProgress = racePhaseProgress.value,
                racers = racerPostcards,
                spectators = spectatorPostcards,
                stageWidthPx = stageWidthPx,
                stageHeightPx = stageHeightPx,
                cardWidthPx = cardWidthPx,
                cardHeightPx = cardHeightPx,
                modifier = Modifier.matchParentSize()
            )
        }

        if (showNotEnoughHint) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(PaperSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, PaperDivider, RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "엽서가 3장 모이면 쫑쫑컵을 열 수 있어요.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GraphiteAccent
                    )
                }
            }
        }
    }
}

@Composable
private fun RanchBackground(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val floorTop = size.height * 0.58f
        drawRect(
            color = RanchSage.copy(alpha = 0.58f),
            topLeft = Offset(0f, floorTop),
            size = Size(size.width, size.height - floorTop)
        )
        drawLine(
            color = RanchGrass.copy(alpha = 0.42f),
            start = Offset(0f, floorTop),
            end = Offset(size.width, floorTop),
            strokeWidth = 2.dp.toPx()
        )

        repeat(26) { index ->
            val seed = index * 37 + 11
            val x = ((seed % 100) / 100f) * size.width
            val y = floorTop + 16.dp.toPx() + ((seed * 17 % 100) / 100f) *
                    (size.height - floorTop - 26.dp.toPx()).coerceAtLeast(1f)
            val blade = 4.dp.toPx() + (seed % 4).dp.toPx()
            drawLine(
                color = RanchGrass.copy(alpha = 0.34f),
                start = Offset(x, y),
                end = Offset(x + (if (seed % 2 == 0) 2.dp.toPx() else -2.dp.toPx()), y - blade),
                strokeWidth = 1.2.dp.toPx()
            )
        }
    }
}

@Composable
private fun SheepRanchCard(
    postcard: Postcard,
    index: Int,
    count: Int,
    cardWidthPx: Float,
    cardHeightPx: Float,
    stageWidthPx: Float,
    stageHeightPx: Float,
    stagePaddingPx: Float,
    bottomReservedPx: Float,
    positions: MutableMap<Long, Float>,
    stretchingCardId: MutableState<Long?>,
    busyCardId: MutableState<Long?>,
    raceActive: Boolean,
    zone: RanchMovementZone,
    onPostcardClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val spec = remember(postcard.id, index, count) {
        createSheepRanchCardSpec(postcard.id, index, count)
    }
    val x = remember(postcard.id) { Animatable(0f) }
    val y = remember(postcard.id) { Animatable(0f) }
    val jump = remember(postcard.id) { Animatable(0f) }
    val landingSquash = remember(postcard.id) { Animatable(0f) }
    val landingScaleX = remember(postcard.id) { Animatable(1f) }
    val landingScaleY = remember(postcard.id) { Animatable(1f) }
    val depthOvershootScale = remember(postcard.id) { Animatable(1f) }
    val tapScale = remember(postcard.id) { Animatable(1f) }
    val landingWobbleRotation = remember(postcard.id) { Animatable(0f) }
    var stretchScaleX by remember(postcard.id) { mutableFloatStateOf(1f) }
    var stretchScaleY by remember(postcard.id) { mutableFloatStateOf(1f) }
    var stretchOffsetX by remember(postcard.id) { mutableFloatStateOf(0f) }
    var stretchOffsetY by remember(postcard.id) { mutableFloatStateOf(0f) }
    var stretchRotation by remember(postcard.id) { mutableFloatStateOf(0f) }
    var direction by remember(postcard.id) { mutableIntStateOf(spec.direction) }
    var walkCount by remember(postcard.id) { mutableIntStateOf(0) }
    var isGrabbed by remember(postcard.id) { mutableStateOf(false) }
    var isDropping by remember(postcard.id) { mutableStateOf(false) }
    var isStretching by remember(postcard.id) { mutableStateOf(false) }
    var suppressTap by remember(postcard.id) { mutableStateOf(false) }
    var dragLean by remember(postcard.id) { mutableFloatStateOf(0f) }
    var dragTiltY by remember(postcard.id) { mutableFloatStateOf(0f) }
    var resistanceShakeX by remember(postcard.id) { mutableFloatStateOf(0f) }
    var resistanceRotation by remember(postcard.id) { mutableFloatStateOf(0f) }

    val maxX = (stageWidthPx - cardWidthPx - stagePaddingPx).coerceAtLeast(stagePaddingPx)
    val (wanderMinX, wanderMaxX) = zoneBoundsX(zone, stagePaddingPx, maxX)
    val maxLiftHeight = (stageHeightPx * RANCH_MAX_LIFT_FRACTION)
        .coerceAtLeast(cardHeightPx)
    val floorY = ranchFloorY(spec, stageHeightPx, cardHeightPx, bottomReservedPx, stagePaddingPx, zone)
    val liftHeight = (floorY - y.value + jump.value).coerceAtLeast(0f)
    val depthProgress = (liftHeight / maxLiftHeight).coerceIn(0f, 1f)
    val latestDepthProgress = rememberUpdatedState(depthProgress)
    val depthScale = lerp(1f, RANCH_MAX_DEPTH_SCALE, depthProgress)
    val shadowScale = lerp(1f, RANCH_SHADOW_MIN_SCALE, depthProgress)
    val shadowAlpha = lerp(0.22f, RANCH_SHADOW_MIN_ALPHA, depthProgress)
    val cardScale by animateFloatAsState(
        targetValue = if (isGrabbed) RANCH_DRAG_SCALE else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ranchCardScale"
    )

    LaunchedEffect(stageWidthPx, stageHeightPx, cardWidthPx, postcard.id, zone) {
        val startX = lerp(wanderMinX, wanderMaxX, spec.startBias)
        x.snapTo(startX)
        y.snapTo(floorY)
        jump.snapTo(0f)
        landingSquash.snapTo(0f)
        landingScaleX.snapTo(1f)
        landingScaleY.snapTo(1f)
        depthOvershootScale.snapTo(1f)
        tapScale.snapTo(1f)
        landingWobbleRotation.snapTo(0f)
        stretchScaleX = 1f
        stretchScaleY = 1f
        stretchOffsetX = 0f
        stretchOffsetY = 0f
        stretchRotation = 0f
        positions[postcard.id] = startX
    }

    LaunchedEffect(isGrabbed, isDropping, postcard.id) {
        if (isGrabbed || isDropping) {
            busyCardId.value = postcard.id
        } else if (busyCardId.value == postcard.id) {
            busyCardId.value = null
        }
    }

    LaunchedEffect(
        postcard.id,
        stageWidthPx,
        stageHeightPx,
        isGrabbed,
        isDropping,
        isStretching,
        raceActive,
        zone
    ) {
        if (
            isGrabbed || isDropping || isStretching || raceActive ||
            stageWidthPx <= 0f || stageHeightPx <= 0f
        ) {
            return@LaunchedEffect
        }

        delay(spec.startDelayMillis.milliseconds)

        while (isActive) {
            delay(spec.pauseMillis.milliseconds)
            if (isGrabbed || isDropping || isStretching) break

            val distancePx = with(density) {
                (spec.speedDpPerSecond * spec.walkMillis / 1000f).dp.toPx()
            }
            val nearEdge =
                (direction < 0 && x.value <= wanderMinX + cardWidthPx * 0.3f) ||
                        (direction > 0 && x.value >= wanderMaxX - cardWidthPx * 0.3f)
            if (nearEdge) {
                direction *= -1
            }

            var targetX = (x.value + distancePx * direction).coerceIn(wanderMinX, wanderMaxX)
            val tooClose = positions
                .filterKeys { it != postcard.id }
                .values
                .any { otherX -> abs(otherX - targetX) < cardWidthPx * 0.72f }
            if (tooClose) {
                targetX = (x.value - distancePx * direction * 0.75f).coerceIn(wanderMinX, wanderMaxX)
                direction *= -1
            }

            x.animateTo(
                targetX,
                animationSpec = tween(
                    durationMillis = spec.walkMillis,
                    easing = LinearEasing
                )
            )
            positions[postcard.id] = targetX
            walkCount++

            if (walkCount % spec.jumpEvery == 0 && !isGrabbed && !isDropping && !isStretching) {
                val jumpHeightPx = with(density) { spec.jumpHeightDp.dp.toPx() }
                jump.animateTo(
                    jumpHeightPx,
                    animationSpec = tween(durationMillis = 130)
                )
                jump.animateTo(
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }

            if (targetX <= wanderMinX + 1f || targetX >= wanderMaxX - 1f) {
                direction *= -1
            }
        }
    }

    LaunchedEffect(isGrabbed, postcard.id) {
        if (!isGrabbed) {
            dragLean = 0f
            dragTiltY = 0f
            resistanceShakeX = 0f
            resistanceRotation = 0f
            return@LaunchedEffect
        }

        while (isActive) {
            val now = withFrameMillis { it }
            val amplitudePx = with(density) {
                lerp(0.6f, 2.4f, latestDepthProgress.value).dp.toPx()
            }
            val rotationAmplitude = lerp(0.5f, 1.8f, latestDepthProgress.value)
            val phase = (now % 120L) / 120f * 2f * PI.toFloat()
            resistanceShakeX = sin(phase) * amplitudePx
            resistanceRotation = sin(phase * 1.35f) * rotationAmplitude
        }
    }

    suspend fun dropToFloor(impactStrength: Float) {
        val impact = impactStrength.coerceIn(0f, 1f)
        isDropping = true
        val firstBounceDp = lerp(7f, 20f, impact)
        val secondBounceDp = firstBounceDp * lerp(0.42f, 0.54f, impact)
        val thirdBounceDp = firstBounceDp * lerp(0.16f, 0.24f, impact)
        val firstBounce = with(density) { firstBounceDp.dp.toPx() }
        val secondBounce = with(density) { secondBounceDp.dp.toPx() }
        val thirdBounce = with(density) { thirdBounceDp.dp.toPx() }
        val dropMillis = lerp(220f, 280f, impact).roundToInt()
        val firstRiseMillis = lerp(105f, 135f, impact).roundToInt()
        val firstLandMillis = lerp(90f, 110f, impact).roundToInt()
        val secondRiseMillis = lerp(70f, 90f, impact).roundToInt()
        val secondLandMillis = lerp(60f, 80f, impact).roundToInt()
        val thirdRiseMillis = lerp(45f, 65f, impact).roundToInt()
        val finalLandMillis = lerp(50f, 60f, impact).roundToInt()
        val restMillis = lerp(150f, 230f, impact).roundToInt()
        val firstSquashX = lerp(1.035f, 1.075f, impact)
        val firstSquashY = lerp(0.955f, 0.925f, impact)
        val stretchX = lerp(0.99f, 0.975f, impact)
        val stretchY = lerp(1.025f, 1.065f, impact)
        val secondSquashX = lerp(1.018f, 1.045f, impact)
        val secondSquashY = lerp(0.985f, 0.955f, impact)
        val finalStretchY = lerp(1.01f, 1.02f, impact)
        val wobbleDegrees = lerp(0.35f, 0.95f, impact)

        y.animateTo(
            floorY,
            animationSpec = tween(durationMillis = dropMillis)
        )
        coroutineScope {
            launch { depthOvershootScale.animateTo(1.02f, tween(durationMillis = 45)) }
            launch { landingSquash.animateTo(1f, tween(durationMillis = 45)) }
            launch { landingScaleX.animateTo(firstSquashX, tween(durationMillis = 45)) }
            launch { landingScaleY.animateTo(firstSquashY, tween(durationMillis = 45)) }
        }
        coroutineScope {
            launch { landingSquash.animateTo(0.35f, tween(durationMillis = 90)) }
            launch { landingScaleX.animateTo(stretchX, tween(durationMillis = firstRiseMillis)) }
            launch { landingScaleY.animateTo(stretchY, tween(durationMillis = firstRiseMillis)) }
            launch { y.animateTo(floorY - firstBounce, tween(durationMillis = firstRiseMillis)) }
        }
        coroutineScope {
            launch { y.animateTo(floorY, tween(durationMillis = firstLandMillis)) }
            launch { depthOvershootScale.animateTo(1f, tween(durationMillis = firstLandMillis)) }
            launch { landingSquash.animateTo(0.48f, tween(durationMillis = firstLandMillis)) }
            launch { landingScaleX.animateTo(secondSquashX, tween(durationMillis = firstLandMillis)) }
            launch { landingScaleY.animateTo(secondSquashY, tween(durationMillis = firstLandMillis)) }
        }
        coroutineScope {
            launch { y.animateTo(floorY - secondBounce, tween(durationMillis = secondRiseMillis)) }
            launch { landingSquash.animateTo(0.16f, tween(durationMillis = secondRiseMillis)) }
            launch { landingScaleX.animateTo(0.99f, tween(durationMillis = secondRiseMillis)) }
            launch { landingScaleY.animateTo(finalStretchY, tween(durationMillis = secondRiseMillis)) }
        }
        coroutineScope {
            launch { y.animateTo(floorY, tween(durationMillis = secondLandMillis)) }
            launch { landingSquash.animateTo(0.24f, tween(durationMillis = secondLandMillis)) }
            launch { landingScaleX.animateTo(1.025f, tween(durationMillis = secondLandMillis)) }
            launch { landingScaleY.animateTo(0.985f, tween(durationMillis = secondLandMillis)) }
        }
        coroutineScope {
            launch { y.animateTo(floorY - thirdBounce, tween(durationMillis = thirdRiseMillis)) }
            launch { landingSquash.animateTo(0.08f, tween(durationMillis = thirdRiseMillis)) }
            launch { landingScaleX.animateTo(0.995f, tween(durationMillis = thirdRiseMillis)) }
            launch { landingScaleY.animateTo(finalStretchY, tween(durationMillis = thirdRiseMillis)) }
        }
        coroutineScope {
            launch { y.animateTo(floorY, tween(durationMillis = finalLandMillis)) }
            launch { landingSquash.animateTo(0.12f, tween(durationMillis = finalLandMillis)) }
            launch { landingScaleX.animateTo(1.012f, tween(durationMillis = finalLandMillis)) }
            launch { landingScaleY.animateTo(0.992f, tween(durationMillis = finalLandMillis)) }
        }
        coroutineScope {
            launch { landingWobbleRotation.animateTo(wobbleDegrees * direction, tween(durationMillis = 70)) }
            launch { landingScaleX.animateTo(1f, tween(durationMillis = 70)) }
            launch { landingScaleY.animateTo(1f, tween(durationMillis = 70)) }
            launch { landingSquash.animateTo(0f, tween(durationMillis = 70)) }
        }
        landingWobbleRotation.animateTo(-wobbleDegrees * 0.65f * direction, tween(durationMillis = 75))
        landingWobbleRotation.animateTo(0f, tween(durationMillis = 95))
        if (restMillis > 0) {
            delay(restMillis.milliseconds)
        }
        coroutineScope {
            launch {
                dragTiltY = 0f
                dragLean = 0f
                resistanceShakeX = 0f
                resistanceRotation = 0f
                tapScale.snapTo(1f)
                jump.snapTo(0f)
                landingScaleX.snapTo(1f)
                landingScaleY.snapTo(1f)
                landingSquash.snapTo(0f)
                depthOvershootScale.snapTo(1f)
                landingWobbleRotation.snapTo(0f)
                suppressTap = false
            }
        }
        isDropping = false
        positions[postcard.id] = x.value
    }

    suspend fun releaseStretch() {
        suspend fun animateStretchValue(
            from: Float,
            to: Float,
            durationMillis: Int,
            setter: (Float) -> Unit
        ) {
            animate(
                initialValue = from,
                targetValue = to,
                animationSpec = tween(durationMillis = durationMillis)
            ) { value, _ ->
                setter(value)
            }
        }

        val wasHorizontal = stretchScaleX >= stretchScaleY
        val contractX = if (wasHorizontal) 0.94f else 1.03f
        val contractY = if (wasHorizontal) 1.04f else 0.95f

        coroutineScope {
            launch { animateStretchValue(stretchScaleX, contractX, 120) { stretchScaleX = it } }
            launch { animateStretchValue(stretchScaleY, contractY, 120) { stretchScaleY = it } }
            launch { animateStretchValue(stretchOffsetX, 0f, 150) { stretchOffsetX = it } }
            launch { animateStretchValue(stretchOffsetY, 0f, 150) { stretchOffsetY = it } }
            launch {
                animateStretchValue(
                    stretchRotation,
                    -stretchRotation * 0.45f,
                    120
                ) { stretchRotation = it }
            }
        }
        coroutineScope {
            launch {
                animateStretchValue(
                    stretchScaleX,
                    if (wasHorizontal) 1.025f else 0.99f,
                    115
                ) { stretchScaleX = it }
            }
            launch {
                animateStretchValue(
                    stretchScaleY,
                    if (wasHorizontal) 0.985f else 1.02f,
                    115
                ) { stretchScaleY = it }
            }
            launch {
                animateStretchValue(
                    stretchRotation,
                    stretchRotation * 0.35f,
                    90
                ) { stretchRotation = it }
            }
        }
        coroutineScope {
            launch { animateStretchValue(stretchScaleX, 1f, 140) { stretchScaleX = it } }
            launch { animateStretchValue(stretchScaleY, 1f, 140) { stretchScaleY = it } }
            launch { animateStretchValue(stretchRotation, 0f, 120) { stretchRotation = it } }
        }
        delay(90.milliseconds)
        stretchScaleX = 1f
        stretchScaleY = 1f
        stretchOffsetX = 0f
        stretchOffsetY = 0f
        stretchRotation = 0f
        isStretching = false
    }

    val walkPhase = ((System.currentTimeMillis() + spec.seed) % 720L) / 720f
    val walkStep = sin(walkPhase * 2f * PI.toFloat())
    val walkBounce = if (!isGrabbed && !isDropping && !isStretching) {
        walkStep * 1.4f
    } else {
        0f
    }
    val footfall = if (!isGrabbed && !isDropping && !isStretching && walkStep < -0.72f) {
        ((abs(walkStep) - 0.72f) / 0.28f).coerceIn(0f, 1f)
    } else {
        0f
    }
    val jumpProgress = (jump.value / with(density) { spec.jumpHeightDp.dp.toPx() })
        .coerceIn(0f, 1f)
    val walkScaleX = 1f + footfall * 0.01f
    val walkScaleY = 1f - footfall * 0.015f
    val jumpScale = lerp(1f, 0.99f, jumpProgress)
    val finalScaleX =
        depthScale *
                cardScale *
                tapScale.value *
                walkScaleX *
                jumpScale *
                landingScaleX.value *
                depthOvershootScale.value *
                stretchScaleX
    val finalScaleY =
        depthScale *
                cardScale *
                tapScale.value *
                walkScaleY *
                jumpScale *
                landingScaleY.value *
                depthOvershootScale.value *
                stretchScaleY
    val shadowParallaxX = lerp(x.value, x.value * RANCH_SHADOW_PARALLAX, depthProgress)
    val stretchShadowX = lerp(1f, 1.22f, ((stretchScaleX - 1f) / 0.4f).coerceIn(0f, 1f))
    val stretchShadowY = lerp(1f, 0.94f, ((stretchScaleY - 1f) / 0.32f).coerceIn(0f, 1f))
    val shadowAlphaWithStretch =
        (shadowAlpha - ((stretchScaleY - 1f).coerceAtLeast(0f) * 0.06f))
            .coerceAtLeast(0.045f)
    val shadowWidthScale = shadowScale * lerp(1f, 1.14f, depthProgress) * stretchShadowX
    val shadowHeightScale = shadowScale * lerp(1f, 0.72f, depthProgress) * stretchShadowY

    Box(
        modifier = Modifier
            .zIndex(if (isGrabbed || isDropping || isStretching) 4f else 1f + index / 100f)
            .offsetInStage(
                shadowParallaxX,
                floorY + cardHeightPx - with(density) { 11.dp.toPx() }
            )
            .graphicsLayer {
                scaleX = shadowWidthScale * (1f + landingSquash.value * 0.18f)
                scaleY = shadowHeightScale * (1f - landingSquash.value * 0.08f)
                alpha = if (raceActive) 0f else shadowAlphaWithStretch
            }
    ) {
        Canvas(
            modifier = Modifier.size(
                width = with(density) { (cardWidthPx * 0.82f).toDp() },
                height = 14.dp
            )
        ) {
            drawOval(
                color = RanchShadow,
                size = size
            )
        }
    }

    Box(
        modifier = modifier
            .zIndex(if (isGrabbed || isDropping || isStretching) 5f else 2f + index / 100f)
            .offsetInStage(x.value, y.value - jump.value)
            .graphicsLayer {
                translationY += walkBounce
                rotationZ = spec.baseRotation +
                        (if (isGrabbed) {
                            dragLean + resistanceRotation
                        } else {
                            direction * 1.8f
                        }) -
                        landingSquash.value * direction * 2f +
                        landingWobbleRotation.value +
                        stretchRotation
                translationX += if (isGrabbed) resistanceShakeX else 0f
                translationX += stretchOffsetX
                translationY += stretchOffsetY
                rotationX = if (isGrabbed || isDropping) {
                    -RANCH_MAX_ROTATION_X * depthProgress
                } else {
                    0f
                }
                rotationY = if (isGrabbed || isDropping) {
                    dragTiltY + direction * RANCH_MAX_ROTATION_Y * depthProgress * 0.35f
                } else {
                    0f
                }
                cameraDistance = 32f * density.density
                scaleX = finalScaleX
                scaleY = finalScaleY
                shadowElevation = if (isGrabbed) 12f else 3f
                shape = RoundedCornerShape(RanchCardCorner)
                clip = false
                alpha = if (raceActive) 0f else 1f
            }
            .then(
                if (raceActive) {
                    Modifier
                } else {
                    Modifier.pointerInput(postcard.id, floorY, stageWidthPx, stageHeightPx) {
                        coroutineScope {
                    val gestureScope = this
                    launch {
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            var stretchStarted = false

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedChanges = event.changes.filter { it.pressed }

                                if (pressedChanges.isEmpty()) {
                                    if (stretchStarted && stretchingCardId.value == postcard.id) {
                                        stretchingCardId.value = null
                                        suppressTap = false
                                    }
                                    break
                                }

                                if (
                                    !stretchStarted &&
                                    pressedChanges.size >= 2 &&
                                    !isGrabbed &&
                                    !isDropping &&
                                    !isStretching &&
                                    jump.value <= 1f &&
                                    (stretchingCardId.value == null ||
                                            stretchingCardId.value == postcard.id)
                                ) {
                                    val first = pressedChanges.firstOrNull {
                                        it.id == firstDown.id
                                    } ?: pressedChanges[0]
                                    val second = pressedChanges.firstOrNull {
                                        it.id != first.id
                                    } ?: pressedChanges[1]
                                    val startFirst = first.position
                                    val startSecond = second.position
                                    val startVector = startSecond - startFirst
                                    val startMidpoint = (startFirst + startSecond) / 2f
                                    val minAxisPx = with(density) { 24.dp.toPx() }

                                    stretchStarted = true
                                    isStretching = true
                                    suppressTap = true
                                    stretchingCardId.value = postcard.id
                                    gestureScope.launch {
                                        x.stop()
                                        y.stop()
                                        jump.stop()
                                        jump.snapTo(0f)
                                        tapScale.snapTo(1f)
                                        landingSquash.snapTo(0f)
                                        landingWobbleRotation.snapTo(0f)
                                    }
                                    first.consume()
                                    second.consume()

                                    while (true) {
                                        val stretchEvent = awaitPointerEvent()
                                        val activeChanges = stretchEvent.changes.filter { it.pressed }
                                        val currentFirst = activeChanges.firstOrNull {
                                            it.id == first.id
                                        }
                                        val currentSecond = activeChanges.firstOrNull {
                                            it.id == second.id
                                        }

                                        if (currentFirst == null || currentSecond == null) {
                                            gestureScope.launch {
                                                releaseStretch()
                                            }
                                            stretchEvent.changes.forEach { it.consume() }
                                            if (activeChanges.isEmpty() &&
                                                stretchingCardId.value == postcard.id
                                            ) {
                                                stretchingCardId.value = null
                                                suppressTap = false
                                            }
                                            break
                                        }

                                        val currentVector =
                                            currentSecond.position - currentFirst.position
                                        val currentMidpoint =
                                            (currentFirst.position + currentSecond.position) / 2f
                                        val startAbsX = abs(startVector.x).coerceAtLeast(minAxisPx)
                                        val startAbsY = abs(startVector.y).coerceAtLeast(minAxisPx)
                                        val pullX =
                                            ((abs(currentVector.x) - startAbsX) /
                                                    (cardWidthPx * 0.45f))
                                                .coerceAtLeast(0f)
                                        val pullY =
                                            ((abs(currentVector.y) - startAbsY) /
                                                    (cardHeightPx * 0.45f))
                                                .coerceAtLeast(0f)
                                        val resistedX =
                                            1f - exp(-pullX * RANCH_STRETCH_RESISTANCE)
                                        val resistedY =
                                            1f - exp(-pullY * RANCH_STRETCH_RESISTANCE)
                                        val targetStretchX =
                                            (1f + resistedX * 0.4f - resistedY * 0.045f)
                                                .coerceIn(0.96f, RANCH_MAX_STRETCH_X)
                                        val targetStretchY =
                                            (1f + resistedY * 0.32f - resistedX * 0.055f)
                                                .coerceIn(0.94f, RANCH_MAX_STRETCH_Y)
                                        val midpointDelta =
                                            (currentMidpoint - startMidpoint) *
                                                    RANCH_STRETCH_CENTER_FOLLOW
                                        val targetOffsetX =
                                            midpointDelta.x.coerceIn(
                                                stagePaddingPx - x.value,
                                                maxX - x.value
                                            )
                                        val targetOffsetY =
                                            midpointDelta.y.coerceIn(
                                                stagePaddingPx - y.value,
                                                floorY - y.value
                                            )
                                        val tiltFromHands =
                                            ((currentFirst.position.y - currentSecond.position.y) /
                                                    cardHeightPx * 2.2f)
                                                .coerceIn(-2f, 2f)
                                        val stretchAmount =
                                            ((targetStretchX - 1f).coerceAtLeast(0f) / 0.4f +
                                                    (targetStretchY - 1f).coerceAtLeast(0f) / 0.32f)
                                                .coerceIn(0f, 1f)
                                        val jitter =
                                            if (stretchAmount > 0.82f) {
                                                sin(System.currentTimeMillis() / 42f) *
                                                        (stretchAmount - 0.82f) *
                                                        1.6f
                                            } else {
                                                0f
                                            }

                                        stretchScaleX = targetStretchX
                                        stretchScaleY = targetStretchY
                                        stretchOffsetX = targetOffsetX
                                        stretchOffsetY = targetOffsetY
                                        stretchRotation = tiltFromHands + jitter
                                        stretchEvent.changes.forEach { it.consume() }
                                    }
                                } else if (stretchStarted) {
                                    event.changes.forEach { it.consume() }
                                }
                            }

                        }
                    }
                    launch {
                        detectTapGestures(
                            onPress = {
                                if (!isGrabbed && !isDropping && !isStretching) {
                                    suppressTap = false
                                    gestureScope.launch {
                                        tapScale.animateTo(
                                            RANCH_TAP_PRESS_SCALE,
                                            tween(durationMillis = 45)
                                        )
                                    }
                                    val released = tryAwaitRelease()
                                    if (
                                        released &&
                                        !suppressTap &&
                                        !isGrabbed &&
                                        !isDropping &&
                                        !isStretching
                                    ) {
                                        gestureScope.launch {
                                            tapScale.animateTo(
                                                RANCH_TAP_BOUNCE_SCALE,
                                                tween(durationMillis = 50)
                                            )
                                            onPostcardClick(postcard.id)
                                            tapScale.animateTo(1f, tween(durationMillis = 70))
                                        }
                                    } else {
                                        gestureScope.launch {
                                            tapScale.animateTo(1f, tween(durationMillis = 70))
                                        }
                                    }
                                    suppressTap = false
                                }
                            }
                        )
                    }
                    launch {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                if (!isStretching && stretchingCardId.value == null) {
                                    isDropping = false
                                    isGrabbed = true
                                    suppressTap = true
                                    dragLean = 0f
                                    dragTiltY = 0f
                                    launch { tapScale.snapTo(1f) }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                if (isGrabbed && !isStretching) {
                                    change.consume()
                                    val nextX = (x.value + dragAmount.x).coerceIn(stagePaddingPx, maxX)
                                    val nextY = (y.value + dragAmount.y)
                                        .coerceIn(
                                            (floorY - maxLiftHeight).coerceAtLeast(stagePaddingPx),
                                            floorY
                                        )
                                    dragLean = (dragAmount.x / 18f).coerceIn(-2f, 2f)
                                    dragTiltY = (dragTiltY * 0.7f +
                                            (dragAmount.x / 22f).coerceIn(
                                                -RANCH_MAX_ROTATION_Y,
                                                RANCH_MAX_ROTATION_Y
                                            ) * 0.3f)
                                    launch {
                                        x.snapTo(nextX)
                                        y.snapTo(nextY)
                                    }
                                }
                            },
                            onDragEnd = {
                                if (isGrabbed) {
                                    val impactStrength = depthProgress
                                    isGrabbed = false
                                    launch {
                                        dropToFloor(impactStrength)
                                    }
                                }
                            },
                            onDragCancel = {
                                if (isGrabbed) {
                                    val impactStrength = depthProgress
                                    isGrabbed = false
                                    launch {
                                        dropToFloor(impactStrength)
                                    }
                                }
                            }
                        )
                    }
                }
                    }
                }
            )
            .background(PaperSurface, RoundedCornerShape(RanchCardCorner))
            .border(1.dp, PaperDivider, RoundedCornerShape(RanchCardCorner))
            .padding(7.dp),
        contentAlignment = Alignment.Center
    ) {
        StampCardContent(
            postcard = postcard,
            isSelected = false,
            dateTextSizeSp = 10
        )
    }
}

private fun Modifier.offsetInStage(
    x: Float,
    y: Float
): Modifier {
    return this.then(
        Modifier.graphicsLayer {
            translationX = x.roundToInt().toFloat()
            translationY = y.roundToInt().toFloat()
        }
    )
}
