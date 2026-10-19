package com.postcardmemory.ui.gallery

import android.content.Context
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/** 연못 수면 도트 잉크 — 종이 위에 얇게 인쇄된 저채도 하늘색. */
internal val PondWaterInk = Color(0xFF8DB4C6)
private const val POND_WATER_INK_ALPHA = 0.55f

// 수면 높이가 한눈에 보이도록 윗선을 따라 긋는 푸른 선(도트보다 한 톤 진하게, 굵지 않게).
internal val PondWaterLineInk = Color(0xFF5C8FA8)
private const val WATER_LINE_ALPHA = 0.9f
private const val WATER_LINE_WIDTH_DP = 2f
private const val WATER_LINE_STEP_DP = 4f

// 하프톤 격자와 점 크기(dp 배수). 줄마다 반 칸씩 엇갈려 인쇄 망점처럼 보이게 한다.
private const val DOT_SPACING_DP = 6f
private const val DOT_SMALL_DP = 1.6f
private const val DOT_MEDIUM_DP = 2.2f
private const val DOT_LARGE_DP = 2.9f

// 물결 윗선: 긴 물결 + 짧은 물결이 서로 반대로 천천히 흐른다.
private const val WAVE_LONG_AMPLITUDE_DP = 4f
private const val WAVE_LONG_LENGTH_DP = 140f
private const val WAVE_LONG_PERIOD_S = 5.2f
private const val WAVE_SHORT_AMPLITUDE_DP = 2f
private const val WAVE_SHORT_LENGTH_DP = 61f
private const val WAVE_SHORT_PERIOD_S = 3.7f

// 차오르는 동안 윗선을 이만큼(배) 더 크게 출렁이다가 다 차면 1배로 잦아든다.
private const val RISE_EXTRA_AMPLITUDE = 1.5f
internal const val POND_WATER_RISE_MS = 900

// 수면 전체의 잔잔한 결 — 점이 아주 조금 떠밀리는 크기와 주기.
private const val IDLE_DRIFT_DP = 1.2f
private const val IDLE_DRIFT_PERIOD_S = 4.5f

// 윗선 바로 아래 띠는 점을 크게 찍어 물결 마루가 살짝 진하게 보이게 한다.
private const val CREST_BAND_DP = 10f

// 터치·발사·벽 충돌 물살: 퍼지는 속도, 물살 띠 두께, 감쇠, 수명.
// (108일차 실기기 QA "물살을 더 크게": 띠 18→24dp, 감쇠 0.6→0.8s, 세기 0.15→0.35, 진해짐 0.6→1.0)
internal const val IMPULSE_SPEED_DP_PER_S = 140f
internal const val IMPULSE_RING_WIDTH_DP = 24f
private const val IMPULSE_DECAY_S = 0.8f
private const val IMPULSE_SHADE_BOOST = 1.0f
// 물살은 파문 이벤트 원래 수명(자취 450~탭 700ms)의 2배 동안 남아 잔향이 보이게 한다.
internal const val POND_WATER_IMPULSE_LIFE_SCALE = 2f
// 파문 이벤트의 반경(maxRadiusPx)을 물살 세기(점이 밀리는 px)로 바꾸는 비율.
private const val IMPULSE_STRENGTH_PER_RADIUS = 0.35f
internal const val POND_WATER_MAX_IMPULSES = 20

// 휴대폰 기울기: 감도를 곱한 값이 0.6을 넘으면 같은 세기로 묶고, 센서 값은 저역 통과 뒤
// 살짝 덜 감쇠된 스프링(감쇠비 ≈ 0.55)으로 따라가 멈추면 한두 번 출렁이고 가라앉는다.
internal const val POND_TILT_MAX = 0.6f
private const val TILT_SENSOR_SMOOTHING = 0.15f
// (108일차 실기기 QA "더 크게" 3회: 높이 차 24→48→110→200dp, 쏠림 5→12→24→40dp,
// 출렁임 0.8→1.6·최대 1.5→2.5, 감도 ×1.5 — 약 23°에서 최대)
private const val TILT_GAIN = 1.5f
private const val TILT_SLOPE_DP = 200f
private const val TILT_SHIFT_DP = 40f
private const val TILT_SPRING_STIFFNESS = 30f
private const val TILT_SPRING_DAMPING = 6f
private const val TILT_SLOSH_SWELL = 1.6f
private const val TILT_SLOSH_MAX = 2.5f
private const val STANDARD_GRAVITY = 9.80665f

private const val TWO_PI = (2.0 * PI).toFloat()

/**
 * 중력(또는 가속도) 센서의 기기 x·y 값과 화면 회전([Surface.ROTATION_0] 등)으로 화면 기준 좌우
 * 기울기를 구한다. 오른쪽이 내려가면 +, 왼쪽이 내려가면 −, [POND_TILT_MAX]로 묶는다.
 */
internal fun screenTiltFromGravity(gx: Float, gy: Float, rotation: Int): Float {
    // 센서는 위를 향한 축에 +g가 걸린다 — 화면 x축 성분이 음수면 오른쪽이 내려간 것.
    val screenX = when (rotation) {
        Surface.ROTATION_90 -> -gy
        Surface.ROTATION_180 -> -gx
        Surface.ROTATION_270 -> gy
        else -> gx
    }
    return (-screenX / STANDARD_GRAVITY * TILT_GAIN).coerceIn(-POND_TILT_MAX, POND_TILT_MAX)
}

/**
 * 기울기 [tilt]에서 화면 가로 위치 [u](-1 왼쪽 끝 … 1 오른쪽 끝)의 수면이 쉬는 높이보다 올라간
 * 양(dp). 수면 윗선·도트와 연잎이 같은 값을 써 물과 연잎 높이가 어긋나지 않게 한다.
 */
internal fun pondWaterTiltRiseDp(tilt: Float, u: Float): Float = TILT_SLOPE_DP * tilt * u

/** 기울기 [tilt]에서 폭 [widthDp]인 수면 윗선이 화면에서 기운 각도(도, rotationZ 기준 시계 방향 +). */
internal fun pondWaterTiltAngleDegrees(tilt: Float, widthDp: Float): Float {
    if (widthDp <= 0f) return 0f
    return Math.toDegrees(atan(-2f * TILT_SLOPE_DP * tilt / widthDp).toDouble()).toFloat()
}

/** 기울기 목표를 부드럽게 따라가는 스프링. 프레임마다 [step]으로 한 칸씩 움직인다. */
internal class PondTiltSpring {
    var position = 0f
        private set
    var velocity = 0f
        private set

    fun step(target: Float, dtSec: Float) {
        val dt = dtSec.coerceIn(0f, 0.05f)
        velocity += ((target - position) * TILT_SPRING_STIFFNESS - velocity * TILT_SPRING_DAMPING) * dt
        position += velocity * dt
    }
}

/** 센서 콜백이 쓰고 프레임 루프가 읽는 저역 통과된 기울기 목표. */
internal class PondTiltInput {
    @Volatile
    var target = 0f

    fun onSample(rawTilt: Float) {
        target += (rawTilt - target) * TILT_SENSOR_SMOOTHING
    }
}

/** 이번 프레임에 크기별로 그릴 망점 좌표(x, y 쌍)와 각 배열에 채워진 float 수. */
internal class PondWaterFrame(dotCapacity: Int) {
    val small = FloatArray(dotCapacity * 2)
    val medium = FloatArray(dotCapacity * 2)
    val large = FloatArray(dotCapacity * 2)
    var smallCount = 0
    var mediumCount = 0
    var largeCount = 0

    val dotCount: Int get() = (smallCount + mediumCount + largeCount) / 2
}

/**
 * 하프톤 수면의 순수 계산부. 망점의 기준 격자·인쇄 얼룩은 크기가 정해질 때 한 번만 만들고,
 * 프레임마다 [render]가 시간·수위·물살을 반영한 위치와 크기 단계를 [PondWaterFrame]에 채운다.
 * 프레임 중에는 객체를 새로 만들지 않는다. 실제 유체 계산이 아니라 사인 물결과 감쇠하는
 * 원형 물살을 더한 근사다.
 */
internal class PondWaterField(
    private val width: Float,
    private val height: Float,
    private val surfaceTopPx: Float,
    private val unitPx: Float
) {
    private val spacing = DOT_SPACING_DP * unitPx
    private val columnXs: FloatArray
    private val baseX: FloatArray
    private val baseY: FloatArray
    private val columnOf: IntArray
    private val noise: FloatArray
    private val waterlineByColumn: FloatArray

    val dotCount: Int

    init {
        val xs = ArrayList<Float>()
        val dx = ArrayList<Float>()
        val dy = ArrayList<Float>()
        val dc = ArrayList<Int>()
        val dn = ArrayList<Float>()
        if (width > 0f && height > 0f && unitPx > 0f) {
            // 짝수 줄과 홀수 줄(반 칸 엇갈림)의 x를 한 목록에 모아, 윗선은 열마다 한 번만 계산한다.
            // 기울기 쏠림만큼 좌우 바깥까지, 위로는 화면 맨 위부터 깔아 둬야 기울어 올라간 물·
            // 옆으로 밀린 면에도 빈 곳 없이 도트가 찬다(물 밖 점은 render에서 건너뛴다).
            val sideMargin = TILT_SHIFT_DP * POND_TILT_MAX * unitPx + spacing
            var x = -sideMargin
            while (x < width + sideMargin) { xs.add(x); x += spacing }
            val evenColumns = xs.size
            x = -sideMargin + spacing / 2f
            while (x < width + sideMargin) { xs.add(x); x += spacing }

            var row = 0
            var y = 0f
            while (y < height) {
                val first = if (row % 2 == 0) 0 else evenColumns
                val last = if (row % 2 == 0) evenColumns else xs.size
                for (c in first until last) {
                    dx.add(xs[c]); dy.add(y); dc.add(c)
                    dn.add(printNoise(c, row))
                }
                row++
                y += spacing
            }
        }
        columnXs = xs.toFloatArray()
        baseX = dx.toFloatArray()
        baseY = dy.toFloatArray()
        columnOf = dc.toIntArray()
        noise = dn.toFloatArray()
        waterlineByColumn = FloatArray(columnXs.size)
        dotCount = baseX.size
    }

    /**
     * 수위 [level](0 = 화면 바닥, 1 = 다 참)과 시간 [timeSec]에서 x 위치의 수면 윗선 y(px).
     * [tilt]가 +(오른쪽이 내려감)면 오른쪽 물이 높아지고, 기울기가 빨리 변하는 동안([tiltVelocity])
     * 물결이 잠깐 커진다.
     */
    fun waterlineY(
        x: Float,
        timeSec: Float,
        level: Float,
        tilt: Float = 0f,
        tiltVelocity: Float = 0f
    ): Float {
        val restTop = height - (height - surfaceTopPx) * level.coerceIn(0f, 1f) -
            unitPx * pondWaterTiltRiseDp(tilt, 2f * x / width - 1f)
        val swell = 1f + RISE_EXTRA_AMPLITUDE * (1f - level.coerceIn(0f, 1f)) +
            (abs(tiltVelocity) * TILT_SLOSH_SWELL).coerceAtMost(TILT_SLOSH_MAX)
        val long = WAVE_LONG_AMPLITUDE_DP *
            sin(TWO_PI * x / (WAVE_LONG_LENGTH_DP * unitPx) - TWO_PI * timeSec / WAVE_LONG_PERIOD_S)
        val short = WAVE_SHORT_AMPLITUDE_DP *
            sin(TWO_PI * x / (WAVE_SHORT_LENGTH_DP * unitPx) + 1.3f + TWO_PI * timeSec / WAVE_SHORT_PERIOD_S)
        return restTop + unitPx * swell * (long + short)
    }

    /**
     * [impulses]는 (x, y, 나이 초, 세기 px) 4개씩 [impulseCount]개. 각 물살은 중심에서 원형으로
     * 퍼지며 지나가는 띠의 망점을 바깥쪽으로 밀었다 당기고, 띠 부분 점을 크게 찍는다.
     */
    fun render(
        timeSec: Float,
        level: Float,
        impulses: FloatArray,
        impulseCount: Int,
        out: PondWaterFrame,
        tilt: Float = 0f,
        tiltVelocity: Float = 0f
    ) {
        out.smallCount = 0
        out.mediumCount = 0
        out.largeCount = 0
        if (dotCount == 0) return

        // 도트는 기울기만큼 옆으로 쏠린 뒤 그려지므로, 물 안인지는 쏠린 실제 x의 윗선으로 판정한다
        // (윗선 푸른 선과 같은 식·같은 x — 둘이 어긋나지 않게).
        val tiltShift = unitPx * TILT_SHIFT_DP * tilt
        for (c in columnXs.indices) {
            waterlineByColumn[c] = waterlineY(columnXs[c] + tiltShift, timeSec, level, tilt, tiltVelocity)
        }
        val visibleMin = -spacing
        val visibleMax = width + spacing

        val crestBand = CREST_BAND_DP * unitPx
        val drift = IDLE_DRIFT_DP * unitPx
        val driftPhase = TWO_PI * timeSec / IDLE_DRIFT_PERIOD_S
        val ringWidth = IMPULSE_RING_WIDTH_DP * unitPx
        val reach = ringWidth * 2.5f
        val speed = IMPULSE_SPEED_DP_PER_S * unitPx

        for (i in 0 until dotCount) {
            val bx = baseX[i]
            val by = baseY[i]
            val waterline = waterlineByColumn[columnOf[i]]
            if (by < waterline) continue

            var px = bx + tiltShift + drift * 0.5f * sin(by / (55f * unitPx) - driftPhase)
            var py = by + drift * sin(bx / (40f * unitPx) + by / (70f * unitPx) - driftPhase)
            var boost = 0f

            for (k in 0 until impulseCount) {
                val base = k * 4
                val age = impulses[base + 2]
                val ddx = bx - impulses[base]
                val ddy = by - impulses[base + 1]
                val front = speed * age
                val distSq = ddx * ddx + ddy * ddy
                val outer = front + reach
                if (distSq > outer * outer) continue
                val dist = sqrt(distSq)
                val d = dist - front
                if (abs(d) > reach) continue
                val envelope = exp(-(d / ringWidth) * (d / ringWidth)) * exp(-age / IMPULSE_DECAY_S)
                val push = impulses[base + 3] * envelope * cos(d * PI.toFloat() / ringWidth)
                if (dist > 0.5f) {
                    px += ddx / dist * push
                    py += ddy / dist * push
                }
                boost += envelope
            }

            val depth = py - waterline
            if (depth < 0f || px < visibleMin || px > visibleMax) continue
            val level3 = if (depth < crestBand) {
                2
            } else {
                val shade = 0.5f +
                    0.3f * sin(bx / (48f * unitPx) + by / (90f * unitPx) - 0.9f * timeSec) +
                    0.2f * sin(by / (37f * unitPx) - bx / (120f * unitPx) + 0.6f * timeSec) +
                    (noise[i] - 0.5f) * 0.3f +
                    boost * IMPULSE_SHADE_BOOST
                when {
                    shade > 0.82f -> 2
                    shade > 0.62f -> 1
                    else -> 0
                }
            }
            when (level3) {
                2 -> { out.large[out.largeCount++] = px; out.large[out.largeCount++] = py }
                1 -> { out.medium[out.mediumCount++] = px; out.medium[out.mediumCount++] = py }
                else -> { out.small[out.smallCount++] = px; out.small[out.smallCount++] = py }
            }
        }
    }
}

/** 망점마다 고정된 0..1 인쇄 얼룩 값 — 같은 칸은 늘 같은 값이라 화면이 반짝이지 않는다. */
private fun printNoise(column: Int, row: Int): Float =
    ((column * 73_856_093) xor (row * 19_349_663)).and(0xFFFF) / 65_535f

/** 파문 이벤트 반경(px)을 물살 세기(px)로. */
internal fun pondWaterImpulseStrength(maxRadiusPx: Float): Float =
    maxRadiusPx * IMPULSE_STRENGTH_PER_RADIUS

/**
 * 연못 수면의 기울기 입력. 중력 센서(없으면 가속도계)를 갤러리 화면이 RESUMED일 때만 등록하고
 * ON_PAUSE·composable 이탈(연못 끄기) 때 해제한다 — 메인 갤러리 "흔들어서 한 장"과 같은 수명
 * 규칙. 연못 흔들기(가속도계 별도 리스너)와는 같은 센서 값을 따로 읽을 뿐 서로 막지 않는다.
 * 센서가 없는 기기에서는 기울기 0으로 남는다.
 */
@Composable
private fun rememberPondTiltInput(): PondTiltInput {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val input = remember { PondTiltInput() }

    DisposableEffect(lifecycleOwner) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || sensor == null) {
            return@DisposableEffect onDispose { }
        }

        var registered = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotation = view.display?.rotation ?: Surface.ROTATION_0
                input.onSample(screenTiltFromGravity(event.values[0], event.values[1], rotation))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun register() {
            if (registered) return
            registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        fun unregister() {
            if (!registered) return
            sensorManager.unregisterListener(listener)
            registered = false
            input.target = 0f
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE -> unregister()
                else -> Unit
            }
        }

        // 이미 RESUMED라면 addObserver가 ON_RESUME까지 따라잡아 바로 등록된다.
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unregister()
        }
    }

    return input
}

/**
 * 108일차: 연못 전체에 깔리는 하늘색 하프톤 수면. 켜지는 순간 화면 바닥에서 [POND_WATER_RISE_MS]
 * 동안 출렁이며 차오르고, 그 뒤엔 잔잔히 흐른다. 빈 곳 터치·자취·발사·비행·벽 충돌이
 * [PondController.addRipple]로 남긴 자리마다 도트 면이 원형으로 출렁인다(예전 청록 동심원 대체).
 * 휴대폰을 기울이면 낮아진 쪽 물이 높아지고 도트 면이 그쪽으로 쏠리며, 멈추면 출렁이다 가라앉는다.
 *
 * 연못이 켜진 동안만 구성되므로 끄면 프레임 루프·물살이 함께 사라진다. 프레임 시간만 draw
 * 단계에서 읽어 매 프레임 다시 그리기만 하고 recomposition은 일어나지 않는다.
 */
@Composable
internal fun PondWaterSurface(
    controller: PondController,
    originInWindow: Offset,
    surfaceTop: Dp,
    modifier: Modifier = Modifier
) {
    var frameNanos by remember { mutableLongStateOf(0L) }
    val level = remember { Animatable(0f) }
    val tiltInput = rememberPondTiltInput()
    val tiltSpring = remember { PondTiltSpring() }

    LaunchedEffect(Unit) {
        level.animateTo(1f, tween(POND_WATER_RISE_MS, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(controller) {
        val start = withFrameNanos { it }
        var last = start
        while (true) {
            withFrameNanos { now ->
                tiltSpring.step(tiltInput.target, (now - last) / 1_000_000_000f)
                last = now
                frameNanos = now - start
                // 연잎도 같은 기울기를 따라 미끄러진다(StampCard가 draw 단계에서 읽음).
                controller.waterTilt = tiltSpring.position
            }
            controller.pruneRipples(System.currentTimeMillis(), POND_WATER_IMPULSE_LIFE_SCALE)
        }
    }

    Spacer(
        modifier = modifier.drawWithCache {
            val field = PondWaterField(size.width, size.height, surfaceTop.toPx(), density)
            val frame = PondWaterFrame(field.dotCount)
            val impulses = FloatArray(POND_WATER_MAX_IMPULSES * 4)
            val ink = PondWaterInk.copy(alpha = POND_WATER_INK_ALPHA).toArgb()
            fun dotPaint(diameterDp: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ink
                strokeWidth = diameterDp * density
                strokeCap = Paint.Cap.ROUND
            }
            val smallPaint = dotPaint(DOT_SMALL_DP)
            val mediumPaint = dotPaint(DOT_MEDIUM_DP)
            val largePaint = dotPaint(DOT_LARGE_DP)
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PondWaterLineInk.copy(alpha = WATER_LINE_ALPHA).toArgb()
                style = Paint.Style.STROKE
                strokeWidth = WATER_LINE_WIDTH_DP * density
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            }
            val linePath = android.graphics.Path()
            val lineStep = WATER_LINE_STEP_DP * density

            onDrawBehind {
                val timeSec = frameNanos / 1_000_000_000f
                val nowMillis = System.currentTimeMillis()
                var count = 0
                for (ripple in controller.ripples) {
                    if (count >= POND_WATER_MAX_IMPULSES) break
                    val ageMs = nowMillis - ripple.bornAtMillis
                    if (ageMs < 0 || ageMs > ripple.lifespanMillis * POND_WATER_IMPULSE_LIFE_SCALE) continue
                    val base = count * 4
                    impulses[base] = ripple.center.x - originInWindow.x
                    impulses[base + 1] = ripple.center.y - originInWindow.y
                    impulses[base + 2] = ageMs / 1000f
                    impulses[base + 3] = pondWaterImpulseStrength(ripple.maxRadiusPx)
                    count++
                }

                field.render(
                    timeSec, level.value, impulses, count, frame,
                    tilt = tiltSpring.position,
                    tiltVelocity = tiltSpring.velocity
                )

                // 수면 윗선(도트 계산과 같은 식) — Path는 재사용하고 프레임마다 점만 다시 잇는다.
                linePath.reset()
                var x = 0f
                while (true) {
                    val y = field.waterlineY(x, timeSec, level.value, tiltSpring.position, tiltSpring.velocity)
                    if (x == 0f) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                    if (x >= size.width) break
                    x = (x + lineStep).coerceAtMost(size.width)
                }

                drawIntoCanvas { canvas ->
                    val native = canvas.nativeCanvas
                    native.drawPoints(frame.small, 0, frame.smallCount, smallPaint)
                    native.drawPoints(frame.medium, 0, frame.mediumCount, mediumPaint)
                    native.drawPoints(frame.large, 0, frame.largeCount, largePaint)
                    native.drawPath(linePath, linePaint)
                }
            }
        }
    )
}
