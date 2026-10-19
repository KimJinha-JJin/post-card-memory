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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
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
import com.postcardmemory.ui.gallery.PondContactTuning
import com.postcardmemory.ui.gallery.PondController
import com.postcardmemory.ui.gallery.PondLeafBody
import com.postcardmemory.ui.gallery.pondWaterTiltAngleDegrees
import com.postcardmemory.ui.gallery.pondWaterTiltRiseDp
import com.postcardmemory.ui.theme.BrutalCoral
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SunsetGold
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
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
// 108일차 연잎 접촉 충돌(예전 발사 지점 130dp 근접 밀림을 대체): 충돌 원 반경은 칸 폭의 0.45배
// (연잎 그림 지름 ≈ 칸 −8dp), 이보다 약한 충격은 상대에게 넘기지 않고, 전달 속도는 최대 발사
// 속도의 0.85배로 묶는다. 충돌 물살 반경은 세기에 따라 24~48dp.
private const val POND_LEAF_CONTACT_RADIUS_RATIO = 0.45f
private const val POND_MIN_TRANSFER_SPEED_DP_PER_S = 150f
private const val POND_MAX_TRANSFER_RATIO = 0.85f
private val POND_CONTACT_RIPPLE_MIN = 24.dp
private val POND_CONTACT_RIPPLE_MAX = 48.dp
private const val POND_CAPTURE_SCALE = 1.03f
private const val POND_ACTIVE_SHADOW_ELEVATION = 6f
private val PondActiveSpotShadow = Color.Black.copy(alpha = 0.4f)
private val PondActiveAmbientShadow = Color.Black.copy(alpha = 0.2f)
private const val POND_ROTATION_FROM_VELOCITY = 0.006f

// 연잎(칸 폭 −8dp 정사각) 가장자리에서 우표까지의 여백. 칸 폭 약 115dp(384dp 화면) 기준
// 우표가 연잎 지름의 약 59%(≈63dp)가 되어 우표 둘레로 잎이 넉넉히 보인다
// (108일차 실기기 QA: 74%에선 우표가 잎을 거의 가렸다).
private val POND_LEAF_STAMP_INSET = 22.dp

// 기울기 1(실제 최대 0.6)일 때 연잎이 옆으로 미끄러지는 기준 거리 — 최대 0.6에서 약 20dp(×0.5~1.5).
private val POND_TILT_DRIFT = 34.dp
// 연잎이 수면 기울기 각도를 따라 기우는 비율(우표가 읽히도록 절반만).
private const val POND_LEAF_SLOPE_FOLLOW = 0.5f

/**
 * 연잎 한 장이 기울기 목표를 따라가는 스프링. [stiffness](1/s²)와 [dampingRatio]가 카드마다
 * 달라 같은 기울기에도 출발·출렁임·멈춤이 제각각이다. 프레임이 길게 끊겨도 한 번에 튀지 않게
 * 한 칸 시간을 0.05초로 묶는다.
 */
internal class PondLeafTiltSpring(
    private val stiffness: Float,
    dampingRatio: Float
) {
    private val damping = 2f * dampingRatio * kotlin.math.sqrt(stiffness)

    var position = 0f
        private set
    var velocity = 0f
        private set

    fun step(target: Float, dtSec: Float) {
        val dt = dtSec.coerceIn(0f, 0.05f)
        velocity += ((target - position) * stiffness - velocity * damping) * dt
        position += velocity * dt
    }

    fun reset() {
        position = 0f
        velocity = 0f
    }
}

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
    // 76일차: 월별 보기에 연못 모드가 이식되며, 월 헤더와 날짜가 중복되지
    // 않도록 day-only 표기를 넘길 수 있게 함(GalleryMonthlyGridItem 참고).
    // null이면(기본값) 기존과 동일하게 전체 날짜를 보여준다.
    dateLabelOverride: String? = null,
    // 108일차: 연못에서 둥근 종이 받침 대신 깔 연잎. 같은 graphicsLayer 안에서
    // 그리므로 부유·흩어짐·새총·밀림을 우표와 한 몸으로 따라간다. null이면
    // 기존 종이 받침 그대로.
    pondLeaf: ImageBitmap? = null,
    onLongClick: () -> Unit = {}
) {
    val seed = remember(postcard.id) { abs(postcard.id.hashCode()) }

    val rotation = remember(seed) {
        (seed % 70 - 35) / 10f
    }

    // 연잎 한 장을 카드마다 다른 각도로 돌려 써서 같은 그림이 반복돼 보이지 않게 한다.
    val leafRotation = remember(seed) { (seed % 360).toFloat() }

    // 108일차: 휴대폰을 기울이면 연잎이 수면을 탄다 — 자기 x 위치의 물 높이만큼 오르내리고
    // (pondWaterTiltRiseDp, 수면 윗선·도트와 같은 식), 수면 기울기의 절반만큼 같이 기울며, 낮아진
    // 쪽으로 살짝 미끄러진다. 카드마다 탄성·출렁임·미끄러짐(0.5~1.5배)이 다른 스프링을 따로 둬
    // 출발과 멈춤이 제각각이 되게 한다.
    val tiltDriftFactor = remember(seed) { 0.5f + (seed % 101) / 100f }
    val tiltStiffness = remember(seed) { 6f + (seed % 13) }
    val tiltDampingRatio = remember(seed) { 0.35f + (seed / 13 % 7) * 0.05f }
    val driftSpring = remember(seed) { PondLeafTiltSpring(tiltStiffness, tiltDampingRatio) }
    val riseSpring = remember(seed) { PondLeafTiltSpring(tiltStiffness, tiltDampingRatio) }
    val angleSpring = remember(seed) { PondLeafTiltSpring(tiltStiffness, tiltDampingRatio) }
    val tiltDriftPx = with(LocalDensity.current) { POND_TILT_DRIFT.toPx() }
    val densityScale = LocalDensity.current.density
    var tiltDrift by remember { mutableFloatStateOf(0f) }
    var tiltRise by remember { mutableFloatStateOf(0f) }
    var tiltLean by remember { mutableFloatStateOf(0f) }

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
    var hitFlightToken by remember { mutableIntStateOf(0) }
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
    val contactTuning = remember(density) {
        with(density) {
            PondContactTuning(
                minTransferSpeedPx = POND_MIN_TRANSFER_SPEED_DP_PER_S.dp.toPx(),
                maxTransferSpeedPx = POND_MAX_LAUNCH_SPEED_DP_PER_S.dp.toPx() * POND_MAX_TRANSFER_RATIO,
                minRipplePx = POND_CONTACT_RIPPLE_MIN.toPx(),
                maxRipplePx = POND_CONTACT_RIPPLE_MAX.toPx()
            )
        }
    }

    // 108일차: 다른 연잎이 이 연잎의 실제 위치를 알 수 있게 공유하는 원형 몸체. 연못이 켜진
    // 동안만 등록되고, 위치는 아래 "수면을 타는 연잎" 프레임 루프가 매 프레임 갱신한다.
    val leafBody = remember(postcard.id) { PondLeafBody(postcard.id) }
    DisposableEffect(pondController, isPondModeOn, leafBody) {
        if (pondController == null || !isPondModeOn) return@DisposableEffect onDispose { }
        pondController.registerLeaf(leafBody)
        onDispose {
            pondController.unregisterLeaf(leafBody)
            leafBody.isFlying = false
            leafBody.isHeld = false
        }
    }

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

    // 공유 몸체를 화면에 실제로 보이는 중심(새총 오프셋 + 흔들기 흩어짐 + 기울기 이동)과 연잎
    // 원 반경으로 맞춘다. 아직 위치를 재지 못했으면 반경 0(충돌 대상에서 빠짐).
    fun syncLeafBody(offsetX: Float, offsetY: Float) {
        if (restCenterInWindow == Offset.Zero || cardSizePx == IntSize.Zero) {
            leafBody.radius = 0f
            return
        }
        leafBody.centerX = restCenterInWindow.x + offsetX + scatterOffsetX.value + tiltDrift
        leafBody.centerY = restCenterInWindow.y + offsetY + scatterOffsetY.value + tiltRise
        leafBody.radius = minOf(cardSizePx.width, cardSizePx.height) * POND_LEAF_CONTACT_RADIUS_RATIO
    }

    // [generation]: 사용자가 직접 쏜 비행 0, 그 연잎에 맞아 날아가는 비행 1, … (연쇄 단계).
    suspend fun runPondFlight(initialVelocity: Offset, generation: Int = 0) {
        var velocity = initialVelocity
        var bounces = 0
        var lastRippleCenter = restCenterInWindow
        val startNanos = withFrameNanos { it }
        var lastFrameNanos = startNanos
        pondController?.takePendingVelocity(postcard.id)
        leafBody.isFlying = true

        try {
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

                // 108일차: 화면에 보이는 위치(기울기 이동 포함)로 몸체를 갱신하고, 실제로 닿은 연잎에만
                // 충격을 나눈다. 날아가는 중에 다른 연잎에게 맞아 쌓인 속도도 여기서 더한다.
                if (pondController != null) {
                    syncLeafBody(newX, newY)
                    velocity = pondController.resolveContacts(
                        sourceId = postcard.id,
                        centerX = leafBody.centerX,
                        centerY = leafBody.centerY,
                        velocity = velocity,
                        generation = generation,
                        nowMillis = System.currentTimeMillis(),
                        tuning = contactTuning
                    ) + pondController.takePendingVelocity(postcard.id)
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
        } finally {
            leafBody.isFlying = false
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

    // 108일차: 다른 연잎에 실제로 맞으면(PondController.resolveContacts → hits) 받은 속도로
    // 기존 새총 비행을 그대로 시작한다 — 벽 반사·물살·복귀가 같이 따라오고, 그 비행도 다른
    // 연잎과 부딪히면 다음 단계로 충격을 넘긴다. 붙잡힌 연잎에는 hits가 오지 않는다.
    LaunchedEffect(postcard.id, pondController, isPondModeOn) {
        if (pondController == null || !isPondModeOn) return@LaunchedEffect

        coroutineScope {
            val hitScope = this
            pondController.hits.collect { hit ->
                if (hit.targetId != postcard.id || isCaptured) return@collect
                launchJob?.cancel()
                isLaunched = true
                // 취소된 이전 비행의 finally가 늦게 돌아도 새 비행의 상태를 끄지 않게 번호로 구분한다.
                val token = ++hitFlightToken
                launchJob = hitScope.launch {
                    try {
                        runPondFlight(hit.velocity, hit.generation)
                    } finally {
                        if (hitFlightToken == token) isLaunched = false
                    }
                }
            }
        }
    }

    // 수면을 타는 연잎(위 tilt* 스프링): 매 프레임 수면 기울기를 읽어 카드별 스프링을 한 칸씩 움직인다.
    LaunchedEffect(pondController, isPondModeOn) {
        driftSpring.reset()
        riseSpring.reset()
        angleSpring.reset()
        tiltDrift = 0f
        tiltRise = 0f
        tiltLean = 0f
        if (pondController == null || !isPondModeOn) return@LaunchedEffect

        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = (now - last) / 1_000_000_000f
                last = now
                val tilt = pondController.waterTilt
                val bounds = pondController.gridBoundsInWindow
                // 이 연잎이 놓인 가로 위치(-1 왼쪽 끝 … 1 오른쪽 끝).
                val u = if (bounds.width > 0f && restCenterInWindow != Offset.Zero) {
                    ((restCenterInWindow.x - bounds.left) / bounds.width * 2f - 1f).coerceIn(-1f, 1f)
                } else {
                    0f
                }
                driftSpring.step(tilt * tiltDriftPx * tiltDriftFactor, dt)
                riseSpring.step(-pondWaterTiltRiseDp(tilt, u) * densityScale, dt)
                angleSpring.step(
                    pondWaterTiltAngleDegrees(tilt, bounds.width / densityScale) * POND_LEAF_SLOPE_FOLLOW,
                    dt
                )
                tiltDrift = driftSpring.position
                tiltRise = riseSpring.position
                tiltLean = angleSpring.position

                // 접촉 충돌용 공유 몸체 — 날아가는 동안은 비행 루프가 직접 갱신한다.
                leafBody.isHeld = isCaptured
                if (!leafBody.isFlying) syncLeafBody(pondOffsetX.value, pondOffsetY.value)
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
                // 108일차 실기기 QA "꾹 눌렀을 때 그림자가 너무 진해": 높이 10→6, 그림자색을 옅은
                // 반투명으로(직접 40%·주변 20%). API 28 미만은 색 지정이 무시되고 높이만 낮아진다.
                shadowElevation = if (isPondActive) POND_ACTIVE_SHADOW_ELEVATION else 0f
                spotShadowColor = PondActiveSpotShadow
                ambientShadowColor = PondActiveAmbientShadow
                shape = if (pondLeaf != null) CircleShape else RoundedCornerShape(12.dp)
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
            // 108일차: 휴대폰을 기울이면 연잎이 낮아진 쪽으로 미끄러진다. 위치 측정(위
            // onGloballyPositioned) 안쪽의 별도 레이어라 새총·반사 계산의 기준 위치는 그대로다.
            .graphicsLayer {
                translationX = tiltDrift
                translationY = tiltRise
                rotationZ = tiltLean
            }
            .padding(4.dp)
            .then(
                if (pondLeaf != null) {
                    Modifier.drawBehind {
                        rotate(leafRotation) {
                            drawImage(
                                image = pondLeaf,
                                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                            )
                        }
                    }
                } else {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(PaperSurface)
                        .border(1.dp, PaperDivider, RoundedCornerShape(12.dp))
                }
            )
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
                                            // 108일차: 발사 지점 근접 밀림은 없앴다 — 실제로 닿은
                                            // 연잎만 비행 중 접촉 판정으로 반응한다.
                                            hitFlightToken++
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
            .padding(if (pondLeaf != null) POND_LEAF_STAMP_INSET else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StampCardContent(
            postcard = postcard,
            isSelected = isSelected,
            dateLabelOverride = dateLabelOverride
        )
    }
}

@Composable
fun StampCardContent(
    postcard: Postcard,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    dateTextSizeSp: Int = 11,
    // 62일차 2차(월별 보기): 월 헤더가 이미 연/월을 보여주므로, 그 grid에서는
    // 전체 날짜 대신 일(day)만 넘겨 중복 표기를 피한다. null이면(기본값)
    // 기존 3단 그리드와 동일하게 전체 날짜를 그대로 보여준다 — 기존 호출부
    // 무변경.
    dateLabelOverride: String? = null
) {
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

            if (postcardHasBackContent(postcard.backRecipientModifier, postcard.backMessage, postcard.backPostscript)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(16.dp)
                        .background(
                            color = PaperSurface,
                            shape = CircleShape
                        )
                        .border(1.dp, PaperDivider, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MailOutline,
                        contentDescription = "뒷면 편지 있음",
                        tint = SunsetGold,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        val dateLabel = dateLabelOverride ?: PostcardDateFormat.formatIso(postcard.capturedAt)

        // 빈 문자열을 넘기면(예: 타임라인 보기처럼 날짜를 이미 다른 곳에
        // 표시하는 호출부) 날짜 줄 자체를 그리지 않는다 — 빈 Text가 불필요한
        // 여백만 남기지 않게.
        if (dateLabel.isNotEmpty()) {
            Text(
                text = dateLabel,
                fontSize = dateTextSizeSp.sp,
                fontWeight = FontWeight.Medium,
                color = GraphiteAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
