package com.postcardmemory.ui.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlin.math.roundToInt
import kotlin.math.sin
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

@Composable
fun SheepRanchStage(
    postcards: List<Postcard>,
    paddingValues: PaddingValues,
    onPostcardClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val positions = remember(postcards.map { it.id }) {
        mutableStateMapOf<Long, Float>()
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

        RanchBackground(
            modifier = Modifier.matchParentSize()
        )

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
                onPostcardClick = onPostcardClick,
                modifier = Modifier.size(width = cardWidth, height = cardWidth + 36.dp)
            )
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
    var direction by remember(postcard.id) { mutableIntStateOf(spec.direction) }
    var walkCount by remember(postcard.id) { mutableIntStateOf(0) }
    var isGrabbed by remember(postcard.id) { mutableStateOf(false) }
    var isDropping by remember(postcard.id) { mutableStateOf(false) }
    var suppressTap by remember(postcard.id) { mutableStateOf(false) }
    var dragLean by remember(postcard.id) { mutableStateOf(0f) }
    var dragTiltY by remember(postcard.id) { mutableStateOf(0f) }
    var resistanceShakeX by remember(postcard.id) { mutableStateOf(0f) }
    var resistanceRotation by remember(postcard.id) { mutableStateOf(0f) }

    val minX = stagePaddingPx
    val maxX = (stageWidthPx - cardWidthPx - stagePaddingPx).coerceAtLeast(minX)
    val maxLiftHeight = (stageHeightPx * RANCH_MAX_LIFT_FRACTION)
        .coerceAtLeast(cardHeightPx)
    val minY = stagePaddingPx
    val floorTopMin = (stageHeightPx * 0.60f).coerceAtLeast(stagePaddingPx)
    val floorTopMax = (stageHeightPx - bottomReservedPx - cardHeightPx)
        .coerceAtLeast(floorTopMin)
    val floorY = lerp(floorTopMin, floorTopMax, spec.startJitter)
    val liftHeight = (floorY - y.value + jump.value).coerceAtLeast(0f)
    val depthProgress = (liftHeight / maxLiftHeight).coerceIn(0f, 1f)
    val currentDepthProgress by rememberUpdatedState(depthProgress)
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

    LaunchedEffect(stageWidthPx, stageHeightPx, cardWidthPx, postcard.id) {
        val startX = lerp(minX, maxX, spec.startBias)
        x.snapTo(startX)
        y.snapTo(floorY)
        jump.snapTo(0f)
        landingSquash.snapTo(0f)
        landingScaleX.snapTo(1f)
        landingScaleY.snapTo(1f)
        depthOvershootScale.snapTo(1f)
        tapScale.snapTo(1f)
        positions[postcard.id] = startX
    }

    LaunchedEffect(postcard.id, stageWidthPx, stageHeightPx, isGrabbed, isDropping) {
        if (isGrabbed || isDropping || stageWidthPx <= 0f || stageHeightPx <= 0f) {
            return@LaunchedEffect
        }

        delay(spec.startDelayMillis.milliseconds)

        while (isActive) {
            delay(spec.pauseMillis.milliseconds)
            if (isGrabbed || isDropping) break

            val distancePx = with(density) {
                (spec.speedDpPerSecond * spec.walkMillis / 1000f).dp.toPx()
            }
            val nearEdge =
                (direction < 0 && x.value <= minX + cardWidthPx * 0.3f) ||
                        (direction > 0 && x.value >= maxX - cardWidthPx * 0.3f)
            if (nearEdge) {
                direction *= -1
            }

            var targetX = (x.value + distancePx * direction).coerceIn(minX, maxX)
            val tooClose = positions
                .filterKeys { it != postcard.id }
                .values
                .any { otherX -> abs(otherX - targetX) < cardWidthPx * 0.72f }
            if (tooClose) {
                targetX = (x.value - distancePx * direction * 0.75f).coerceIn(minX, maxX)
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

            if (walkCount % spec.jumpEvery == 0 && !isGrabbed && !isDropping) {
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

            if (targetX <= minX + 1f || targetX >= maxX - 1f) {
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
                lerp(0.6f, 2.4f, currentDepthProgress).dp.toPx()
            }
            val rotationAmplitude = lerp(0.5f, 1.8f, currentDepthProgress)
            val phase = (now % 120L) / 120f * 2f * PI.toFloat()
            resistanceShakeX = sin(phase) * amplitudePx
            resistanceRotation = sin(phase * 1.35f) * rotationAmplitude
        }
    }

    suspend fun dropToFloor() {
        isDropping = true
        val firstBounce = with(density) { (6f + spec.seed % 7).dp.toPx() }
        val secondBounce = with(density) { (2f + spec.seed % 4).dp.toPx() }
        y.animateTo(
            floorY,
            animationSpec = tween(durationMillis = 260)
        )
        coroutineScope {
            launch { depthOvershootScale.animateTo(1.035f, tween(durationMillis = 55)) }
            launch { landingSquash.animateTo(1f, tween(durationMillis = 55)) }
            launch { landingScaleX.animateTo(1.025f, tween(durationMillis = 55)) }
            launch { landingScaleY.animateTo(0.93f, tween(durationMillis = 55)) }
        }
        coroutineScope {
            launch { landingSquash.animateTo(0.35f, tween(durationMillis = 90)) }
            launch { landingScaleX.animateTo(0.985f, tween(durationMillis = 90)) }
            launch { landingScaleY.animateTo(1.035f, tween(durationMillis = 90)) }
            launch { y.animateTo(floorY - firstBounce, tween(durationMillis = 120)) }
        }
        coroutineScope {
            launch {
                y.animateTo(
                    floorY,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch { depthOvershootScale.animateTo(1f, tween(durationMillis = 120)) }
            launch { landingScaleX.animateTo(1.015f, tween(durationMillis = 90)) }
            launch { landingScaleY.animateTo(0.99f, tween(durationMillis = 90)) }
        }
        coroutineScope {
            launch { y.animateTo(floorY - secondBounce, tween(durationMillis = 90)) }
            launch { landingScaleX.animateTo(1f, tween(durationMillis = 90)) }
            launch { landingScaleY.animateTo(1.015f, tween(durationMillis = 90)) }
        }
        coroutineScope {
            launch { y.animateTo(floorY, tween(durationMillis = 110)) }
            launch { landingSquash.animateTo(0f, tween(durationMillis = 110)) }
            launch { landingScaleX.animateTo(1f, tween(durationMillis = 110)) }
            launch { landingScaleY.animateTo(1f, tween(durationMillis = 110)) }
            launch { depthOvershootScale.animateTo(1f, tween(durationMillis = 110)) }
        }
        coroutineScope {
            launch {
                dragTiltY = 0f
                dragLean = 0f
                resistanceShakeX = 0f
                resistanceRotation = 0f
                tapScale.snapTo(1f)
                jump.snapTo(0f)
                suppressTap = false
            }
        }
        isDropping = false
        positions[postcard.id] = x.value
    }

    val walkPhase = ((System.currentTimeMillis() + spec.seed) % 720L) / 720f
    val walkStep = sin(walkPhase * 2f * PI.toFloat())
    val walkBounce = if (!isGrabbed && !isDropping) {
        walkStep * 1.4f
    } else {
        0f
    }
    val footfall = if (!isGrabbed && !isDropping && walkStep < -0.72f) {
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
                depthOvershootScale.value
    val finalScaleY =
        depthScale *
                cardScale *
                tapScale.value *
                walkScaleY *
                jumpScale *
                landingScaleY.value *
                depthOvershootScale.value
    val shadowParallaxX = lerp(x.value, x.value * RANCH_SHADOW_PARALLAX, depthProgress)
    val shadowWidthScale = shadowScale * lerp(1f, 1.14f, depthProgress)
    val shadowHeightScale = shadowScale * lerp(1f, 0.72f, depthProgress)

    Box(
        modifier = Modifier
            .zIndex(if (isGrabbed || isDropping) 4f else 1f + index / 100f)
            .offsetInStage(
                shadowParallaxX,
                floorY + cardHeightPx - with(density) { 11.dp.toPx() }
            )
            .graphicsLayer {
                scaleX = shadowWidthScale * (1f + landingSquash.value * 0.18f)
                scaleY = shadowHeightScale * (1f - landingSquash.value * 0.08f)
                alpha = shadowAlpha
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
            .zIndex(if (isGrabbed || isDropping) 5f else 2f + index / 100f)
            .offsetInStage(x.value, y.value - jump.value)
            .graphicsLayer {
                translationY += walkBounce
                rotationZ = spec.baseRotation +
                        (if (isGrabbed) {
                            dragLean + resistanceRotation
                        } else {
                            direction * 1.8f
                        }) -
                        landingSquash.value * direction * 2f
                translationX += if (isGrabbed) resistanceShakeX else 0f
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
            }
            .pointerInput(postcard.id, floorY, stageWidthPx, stageHeightPx) {
                coroutineScope {
                    launch {
                        detectTapGestures(
                            onPress = {
                                if (!isGrabbed && !isDropping) {
                                    suppressTap = false
                                    tapScale.animateTo(
                                        RANCH_TAP_PRESS_SCALE,
                                        tween(durationMillis = 45)
                                    )
                                    val released = tryAwaitRelease()
                                    if (
                                        released &&
                                        !suppressTap &&
                                        !isGrabbed &&
                                        !isDropping
                                    ) {
                                        tapScale.animateTo(
                                            RANCH_TAP_BOUNCE_SCALE,
                                            tween(durationMillis = 50)
                                        )
                                        onPostcardClick(postcard.id)
                                    }
                                    tapScale.animateTo(1f, tween(durationMillis = 70))
                                    suppressTap = false
                                }
                            }
                        )
                    }
                    launch {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                isDropping = false
                                isGrabbed = true
                                suppressTap = true
                                dragLean = 0f
                                dragTiltY = 0f
                                launch { tapScale.snapTo(1f) }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val nextX = (x.value + dragAmount.x).coerceIn(minX, maxX)
                                val nextY = (y.value + dragAmount.y)
                                    .coerceIn((floorY - maxLiftHeight).coerceAtLeast(minY), floorY)
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
                            },
                            onDragEnd = {
                                isGrabbed = false
                                launch {
                                    dropToFloor()
                                }
                            },
                            onDragCancel = {
                                isGrabbed = false
                                launch {
                                    dropToFloor()
                                }
                            }
                        )
                    }
                }
            }
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
