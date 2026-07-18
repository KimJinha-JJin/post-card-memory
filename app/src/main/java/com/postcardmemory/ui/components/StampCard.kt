package com.postcardmemory.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.gallery.PondController
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private const val FLOAT_BASE_PERIOD_MS = 2600
private const val FLOAT_PERIOD_VARIANCE_MS = 900
private val FLOAT_AMPLITUDE = 3.dp

private val SCATTER_DISTANCE = 34.dp
private const val SCATTER_ROTATION_DEGREES = 18f
private const val SCATTER_STAGGER_MAX_MS = 120
private const val SCATTER_HOLD_MS = 180L

// 〈엽서의 연못〉 새총 발사 튜닝값 — 전부 화면 한정 임시 상태에만 쓰인다.
private val POND_MAX_PULL = 200.dp
private val POND_MIN_LAUNCH = 16.dp
private const val POND_MAX_LAUNCH_SPEED_DP_PER_S = 1500f
private const val POND_MIN_SPEED_DP_PER_S = 60f
private const val POND_BOUNCE_DAMPING = 0.6f
private const val POND_MAX_BOUNCES = 2
private const val POND_MAX_FLIGHT_MS = 1800L
private val POND_PATH_RIPPLE_MIN_DISTANCE = 30.dp
private val POND_PATH_RIPPLE_RADIUS = 26.dp
private val POND_BOUNDARY_RIPPLE_RADIUS = 34.dp
private val POND_LAUNCH_RIPPLE_RADIUS = 38.dp
private val POND_NEIGHBOR_INFLUENCE_RADIUS = 130.dp
private val POND_NEIGHBOR_MAX_PUSH = 14.dp
private const val POND_CAPTURE_SCALE = 1.03f
private const val POND_ROTATION_FROM_VELOCITY = 0.006f

/**
 * 셔플(흔들기) 트리거가 바뀔 때마다 카드가 잠시 흩어졌다 원래 자리로 돌아오는
 * 애니메이션을 재생한다. shakeTrigger가 0(초기값)일 때는 재생하지 않는다.
 *
 * isPondModeOn이 true일 때만 평상시 부유 효과·흔들기 반응·새총 잡기/발사가 동작한다.
 * false면 기존(연못 모드 도입 이전) 갤러리와 완전히 동일하게 동작한다.
 */
@Composable
fun StampCard(
    postcard: Postcard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    shakeTrigger: Int = 0,
    isPondModeOn: Boolean = false,
    pondController: PondController? = null,
    onLongClick: () -> Unit = {}
) {
    val seed = remember(postcard.id) { abs(postcard.id.hashCode()) }

    val rotation = remember(seed) {
        (seed % 70 - 35) / 10f
    }

    // 평상시 아주 미세하게 둥둥 떠 있는 효과 — 연못 모드 ON일 때만, 카드마다
    // 위상·주기를 살짝 달리해 기계적으로 동시에 움직이는 느낌을 피한다.
    val floatPhase = remember(seed) { (seed % 1000) / 1000f }
    val floatPeriodMs = remember(seed) {
        FLOAT_BASE_PERIOD_MS + (seed % FLOAT_PERIOD_VARIANCE_MS)
    }

    val floatOffsetPx = if (isPondModeOn) {
        val infiniteTransition = rememberInfiniteTransition(label = "stampFloat")
        val floatAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = floatPeriodMs,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "stampFloatAngle"
        )
        val amplitudePx = with(LocalDensity.current) { FLOAT_AMPLITUDE.toPx() }
        sin(floatAngle + floatPhase * 2f * PI.toFloat()) * amplitudePx
    } else {
        0f
    }

    // 휴대폰을 흔들면 잠시 사방으로 흩어졌다가 원래 그리드 위치로 복귀한다.
    // (연못 모드 ON일 때만 반응 — 센서 등록 자체도 연못 모드 ON일 때만 이뤄진다.)
    val scatterOffsetX = remember { Animatable(0f) }
    val scatterOffsetY = remember { Animatable(0f) }
    val scatterRotation = remember { Animatable(0f) }
    val staggerDelayMs = remember(seed) { seed % SCATTER_STAGGER_MAX_MS }
    val scatterDistancePx = with(LocalDensity.current) { SCATTER_DISTANCE.toPx() }

    LaunchedEffect(shakeTrigger, isPondModeOn) {
        if (!isPondModeOn || shakeTrigger <= 0) return@LaunchedEffect

        delay(staggerDelayMs.milliseconds)

        val targetX = (Random.nextFloat() * 2f - 1f) * scatterDistancePx
        val targetY = (Random.nextFloat() * 2f - 1f) * scatterDistancePx
        val targetRotation =
            (Random.nextFloat() * 2f - 1f) * SCATTER_ROTATION_DEGREES

        coroutineScope {
            launch {
                scatterOffsetX.animateTo(
                    targetX,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scatterOffsetY.animateTo(
                    targetY,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scatterRotation.animateTo(
                    targetRotation,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }

        delay(SCATTER_HOLD_MS.milliseconds)

        coroutineScope {
            launch {
                scatterOffsetX.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scatterOffsetY.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                scatterRotation.animateTo(
                    0f,
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
        }
    }

    // 새총으로 붙잡아 당기고 발사하는 연못 모드 전용 임시 물리 상태.
    val pondOffsetX = remember { Animatable(0f) }
    val pondOffsetY = remember { Animatable(0f) }
    val pondExtraRotation = remember { Animatable(0f) }
    var isCaptured by remember { mutableStateOf(false) }
    var isLaunched by remember { mutableStateOf(false) }
    var launchJob by remember { mutableStateOf<Job?>(null) }
    var restCenterInWindow by remember { mutableStateOf(Offset.Zero) }
    var cardSizePx by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current
    val maxPullPx = with(density) { POND_MAX_PULL.toPx() }
    val minLaunchPx = with(density) { POND_MIN_LAUNCH.toPx() }
    val maxLaunchSpeedPx = with(density) { POND_MAX_LAUNCH_SPEED_DP_PER_S.dp.toPx() }
    val minSpeedPx = with(density) { POND_MIN_SPEED_DP_PER_S.dp.toPx() }
    val pathRippleMinDistancePx = with(density) { POND_PATH_RIPPLE_MIN_DISTANCE.toPx() }
    val pathRipplePx = with(density) { POND_PATH_RIPPLE_RADIUS.toPx() }
    val boundaryRipplePx = with(density) { POND_BOUNDARY_RIPPLE_RADIUS.toPx() }
    val launchRipplePx = with(density) { POND_LAUNCH_RIPPLE_RADIUS.toPx() }
    val neighborInfluenceRadiusPx = with(density) { POND_NEIGHBOR_INFLUENCE_RADIUS.toPx() }
    val neighborMaxPushPx = with(density) { POND_NEIGHBOR_MAX_PUSH.toPx() }

    val pondReturnSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    suspend fun springPondBackToOrigin() {
        coroutineScope {
            launch { pondOffsetX.animateTo(0f, pondReturnSpring) }
            launch { pondOffsetY.animateTo(0f, pondReturnSpring) }
            launch { pondExtraRotation.animateTo(0f, pondReturnSpring) }
        }
    }

    suspend fun runPondFlight(initialVelocity: Offset) {
        var velocity = initialVelocity
        var bounces = 0
        var lastRippleCenter = restCenterInWindow
        val startNanos = withFrameNanos { it }
        var lastFrameNanos = startNanos

        while (true) {
            val frameNanos = withFrameNanos { it }
            val dt = ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastFrameNanos = frameNanos

            var newX = pondOffsetX.value + velocity.x * dt
            var newY = pondOffsetY.value + velocity.y * dt

            val bounds = pondController?.gridBoundsInWindow
            if (bounds != null && bounds != Rect.Zero && cardSizePx != IntSize.Zero) {
                val halfW = cardSizePx.width / 2f
                val halfH = cardSizePx.height / 2f
                val centerX = restCenterInWindow.x + newX
                val centerY = restCenterInWindow.y + newY

                if (centerX - halfW < bounds.left && velocity.x < 0f) {
                    velocity = Offset(-velocity.x * POND_BOUNCE_DAMPING, velocity.y)
                    newX = (bounds.left + halfW) - restCenterInWindow.x
                    bounces++
                    pondController.addRipple(
                        Offset(bounds.left + halfW, centerY),
                        boundaryRipplePx
                    )
                } else if (centerX + halfW > bounds.right && velocity.x > 0f) {
                    velocity = Offset(-velocity.x * POND_BOUNCE_DAMPING, velocity.y)
                    newX = (bounds.right - halfW) - restCenterInWindow.x
                    bounces++
                    pondController.addRipple(
                        Offset(bounds.right - halfW, centerY),
                        boundaryRipplePx
                    )
                }

                if (centerY - halfH < bounds.top && velocity.y < 0f) {
                    velocity = Offset(velocity.x, -velocity.y * POND_BOUNCE_DAMPING)
                    newY = (bounds.top + halfH) - restCenterInWindow.y
                    bounces++
                    pondController.addRipple(
                        Offset(centerX, bounds.top + halfH),
                        boundaryRipplePx
                    )
                } else if (centerY + halfH > bounds.bottom && velocity.y > 0f) {
                    velocity = Offset(velocity.x, -velocity.y * POND_BOUNCE_DAMPING)
                    newY = (bounds.bottom - halfH) - restCenterInWindow.y
                    bounces++
                    pondController.addRipple(
                        Offset(centerX, bounds.bottom - halfH),
                        boundaryRipplePx
                    )
                }
            }

            pondOffsetX.snapTo(newX)
            pondOffsetY.snapTo(newY)
            pondExtraRotation.snapTo(
                (pondExtraRotation.value + velocity.x * dt * POND_ROTATION_FROM_VELOCITY)
                    .coerceIn(-25f, 25f)
            )

            val currentCenter = Offset(restCenterInWindow.x + newX, restCenterInWindow.y + newY)
            if ((currentCenter - lastRippleCenter).getDistance() > pathRippleMinDistancePx) {
                pondController?.addRipple(currentCenter, pathRipplePx, 500L)
                lastRippleCenter = currentCenter
            }

            val elapsedMs = (frameNanos - startNanos) / 1_000_000L
            if (
                velocity.getDistance() < minSpeedPx ||
                bounces > POND_MAX_BOUNCES ||
                elapsedMs > POND_MAX_FLIGHT_MS
            ) {
                break
            }
        }

        springPondBackToOrigin()
    }

    // 연못 모드를 끄거나 이 카드가 화면을 벗어나면 모든 임시 물리 상태를 정리한다.
    LaunchedEffect(isPondModeOn) {
        if (!isPondModeOn) {
            launchJob?.cancel()
            launchJob = null
            isCaptured = false
            isLaunched = false
            pondOffsetX.snapTo(0f)
            pondOffsetY.snapTo(0f)
            pondExtraRotation.snapTo(0f)
        }
    }

    LaunchedEffect(postcard.id, pondController) {
        if (pondController == null) return@LaunchedEffect

        snapshotFlow { pondController.lastImpulse }.collect { impulse ->
            if (impulse == null || impulse.sourceId == postcard.id) {
                return@collect
            }

            val myCenter = restCenterInWindow
            if (myCenter == Offset.Zero) {
                return@collect
            }

            val delta = myCenter - impulse.center
            val distance = delta.getDistance()

            if (distance in 0.01f..neighborInfluenceRadiusPx) {
                val strength = 1f - (distance / neighborInfluenceRadiusPx)
                val direction = delta / distance
                val push = direction * (neighborMaxPushPx * strength)

                coroutineScope {
                    launch {
                        pondOffsetX.animateTo(
                            push.x,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                        pondOffsetX.animateTo(0f, pondReturnSpring)
                    }
                    launch {
                        pondOffsetY.animateTo(
                            push.y,
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                        pondOffsetY.animateTo(0f, pondReturnSpring)
                    }
                }
            }
        }
    }

    val isPondActive = isCaptured || isLaunched

    Column(
        modifier = modifier
            .zIndex(if (isPondActive) 1f else 0f)
            .graphicsLayer {
                translationX = scatterOffsetX.value + pondOffsetX.value
                translationY = floatOffsetPx + scatterOffsetY.value + pondOffsetY.value
                rotationZ = rotation + scatterRotation.value + pondExtraRotation.value
                val scale = if (isPondActive) POND_CAPTURE_SCALE else 1f
                scaleX = scale
                scaleY = scale
                shadowElevation = if (isPondActive) 10f else 0f
                shape = RoundedCornerShape(12.dp)
                clip = false
            }
            .onGloballyPositioned { coordinates ->
                if (isPondModeOn && !isCaptured && !isLaunched) {
                    val topLeft = coordinates.positionInWindow()
                    val size = coordinates.size
                    restCenterInWindow =
                        topLeft + Offset(size.width / 2f, size.height / 2f)
                    cardSizePx = size
                }
            }
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaperSurface)
            .border(1.dp, PaperDivider, RoundedCornerShape(12.dp))
            .then(
                if (isPondModeOn) {
                    Modifier.pointerInput(postcard.id, pondController) {
                        coroutineScope {
                            val gestureScope = this

                            launch {
                                detectTapGestures(
                                    onTap = { onClick() }
                                )
                            }

                            launch {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        launchJob?.cancel()
                                        launchJob = null
                                        isCaptured = true
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val pulled = Offset(
                                            pondOffsetX.value + dragAmount.x,
                                            pondOffsetY.value + dragAmount.y
                                        )
                                        val distance = pulled.getDistance()
                                        val clamped = if (distance > maxPullPx && distance > 0f) {
                                            pulled * (maxPullPx / distance)
                                        } else {
                                            pulled
                                        }
                                        gestureScope.launch { pondOffsetX.snapTo(clamped.x) }
                                        gestureScope.launch { pondOffsetY.snapTo(clamped.y) }
                                    },
                                    onDragEnd = {
                                        isCaptured = false
                                        val pulled =
                                            Offset(pondOffsetX.value, pondOffsetY.value)
                                        val distance = pulled.getDistance()

                                        if (distance < minLaunchPx || distance <= 0f) {
                                            gestureScope.launch { springPondBackToOrigin() }
                                        } else {
                                            val speed =
                                                (distance / maxPullPx).coerceIn(0f, 1f) *
                                                        maxLaunchSpeedPx
                                            val unit = pulled / distance
                                            val velocity = Offset(-unit.x, -unit.y) * speed

                                            isLaunched = true

                                            pondController?.addRipple(
                                                restCenterInWindow,
                                                launchRipplePx,
                                                600L
                                            )
                                            pondController?.notifyLaunch(
                                                postcard.id,
                                                restCenterInWindow
                                            )

                                            launchJob = gestureScope.launch {
                                                runPondFlight(velocity)
                                                isLaunched = false
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isCaptured = false
                                        gestureScope.launch { springPondBackToOrigin() }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                }
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StampCardContent(
            postcard = postcard,
            isSelected = isSelected
        )
    }
}

@Composable
fun StampCardContent(
    postcard: Postcard,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    dateTextSizeSp: Int = 11
) {
    val dateFormatter = remember {
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            StampPhoto(
                imagePath = postcard.imagePath,
                contentDescription = postcard.title,
                modifier = Modifier.fillMaxWidth(),
                outlineColor = Color.White,
                outlineWidth = 3f
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(PinkingPhotoShape)
                        .background(
                            color = BrutalCoral.copy(alpha = 0.3f)
                        )
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(14.dp)
                        .background(
                            color = BrutalCoral,
                            shape = CircleShape
                        )
                )
            }
        }

        Text(
            text = dateFormatter.format(
                Date(postcard.capturedAt)
            ),
            fontSize = dateTextSizeSp.sp,
            fontWeight = FontWeight.Medium,
            color = GraphiteAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
