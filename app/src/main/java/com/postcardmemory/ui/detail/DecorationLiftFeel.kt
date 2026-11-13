package com.postcardmemory.ui.detail

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.postcardmemory.ui.gallery.rememberSystemReducedMotion
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * 131일차: 텍스트·라벨·테이프를 잡으면 재질에 맞게 살짝 들리고, 끄는 속도만큼 약하게
 * 기울었다가, 놓으면 종이에 짧게 눌리며 붙는다. 130일차 사진 스티커 B안
 * (PhotoStickerLiftFeel.kt — 검증 끝난 코드라 손대지 않음)과 같은 흐름이지만, 재질마다
 * 수치와 그림자 모양만 [DecorationLiftSpec]으로 따로 둔다.
 *
 * 전부 "그리기"만 바꾼다.
 * - 들림·기울기: 위치 Box에서 회전 graphicsLayer "바깥"(앞)에 거는 drawWithContent 변환이라
 *   화면 기준 위로 들리고, 제스처 좌표계(graphicsLayer 회전 + localStickerDeltaToParent)는 그대로다.
 * - 그림자: 내용(글자·라벨·테이프) 자체를 기록해 어둡게 물들이고 흐린 실루엣이라
 *   사각형 판이 아니라 글자 획·라벨 윤곽·테이프 가장자리(반투명도까지)를 그대로 따른다.
 *   선택 테두리는 내용 밖이라 그림자에 들어가지 않는다.
 * - 저장되는 위치·크기·회전, Undo/Redo 스냅샷, 공유·내보내기 결과에 들어가지 않는다.
 *   쉬는 상태에서는 기존과 똑같이 그려진다(기록·그림자 없음).
 */

/** 재질별 손맛 수치. 들림 값(lift) 하나에서 크기·높이·그림자가 파생된다. */
internal data class DecorationLiftSpec(
    val liftScale: Float,
    val liftDp: Float,
    val settleLift: Float,
    val liftInMs: Int,
    val dropMs: Int,
    val settleAtMs: Int,
    val tiltPerSpeed: Float,
    val tiltMaxDeg: Float,
    val shadowRestOffsetDp: Float,
    val shadowLiftedOffsetDp: Float,
    val shadowBlurDp: Float,
    val shadowAlpha: Float
)

/** 글자 다이컷 스티커: 가볍게 들리되 글자가 흔들려 읽기 어렵지 않게 기울기는 작게. */
internal val TextStickerLiftSpec = DecorationLiftSpec(
    liftScale = 0.03f,
    liftDp = 3f,
    settleLift = -0.12f,
    liftInMs = 140,
    dropMs = 200,
    settleAtMs = 130,
    tiltPerSpeed = 1.5f,
    tiltMaxDeg = 2f,
    shadowRestOffsetDp = 1f,
    shadowLiftedOffsetDp = 6f,
    shadowBlurDp = 6f,
    shadowAlpha = 0.20f
)

/** 단단한 플라스틱 엠보싱 라벨: 한 덩어리로 들리고, 또렷한 짧은 그림자, 놓으면 탁 눌려 붙는다. */
internal val LabelStickerLiftSpec = DecorationLiftSpec(
    liftScale = 0.025f,
    liftDp = 3f,
    settleLift = -0.2f,
    liftInMs = 130,
    dropMs = 180,
    settleAtMs = 110,
    tiltPerSpeed = 1.5f,
    tiltMaxDeg = 2f,
    shadowRestOffsetDp = 1f,
    shadowLiftedOffsetDp = 4f,
    shadowBlurDp = 3f,
    shadowAlpha = 0.28f
)

/** 얇은 종이 테이프: 거의 떠오르지 않고, 옅은 그림자, 놓으면 가볍게 밀착된다. */
internal val MaskingTapeLiftSpec = DecorationLiftSpec(
    liftScale = 0.015f,
    liftDp = 1.5f,
    settleLift = -0.1f,
    liftInMs = 120,
    dropMs = 160,
    settleAtMs = 100,
    tiltPerSpeed = 1f,
    tiltMaxDeg = 1.2f,
    shadowRestOffsetDp = 0.5f,
    shadowLiftedOffsetDp = 2.5f,
    shadowBlurDp = 2.5f,
    shadowAlpha = 0.14f
)

@Stable
internal class DecorationLiftState(
    private val scope: CoroutineScope,
    val reduceMotion: Boolean,
    val spec: DecorationLiftSpec,
    internal val shadowLayer: GraphicsLayer
) {
    /** 1 = 들림, 0 = 붙어 있음. 내려앉을 때 잠깐 음수(종이에 눌림)까지 간다. */
    val lift = Animatable(0f)
    val tilt = Animatable(0f)

    /** 손가락이 닿은 뒤 내려앉기가 끝날 때까지 true — 그림자는 이 동안만 그린다. */
    var isLifted by mutableStateOf(false)
        private set

    private var smoothedVelocityDpPerMs = 0f
    private var tiltJob: Job? = null
    private var settleJob: Job? = null

    fun onPress() {
        settleJob?.cancel()
        tiltJob?.cancel()
        isLifted = true
        smoothedVelocityDpPerMs = 0f
        scope.launch {
            if (reduceMotion) {
                lift.snapTo(1f)
            } else {
                lift.animateTo(1f, tween(spec.liftInMs, easing = DecorationEaseOutQuint))
            }
        }
        scope.launch { tilt.stop() }
    }

    fun onDrag(deltaXDp: Float, deltaMillis: Long, multiTouch: Boolean) {
        if (reduceMotion) return
        smoothedVelocityDpPerMs = smoothPhotoStickerVelocity(smoothedVelocityDpPerMs, deltaXDp, deltaMillis)
        val target = if (multiTouch) 0f else decorationDragTiltTarget(smoothedVelocityDpPerMs, spec)
        tiltJob?.cancel()
        tiltJob = scope.launch {
            tilt.animateTo(target, tween(DECORATION_TILT_FOLLOW_MS, easing = LinearOutSlowInEasing))
            // 손이 멈추면(다음 움직임이 오지 않으면) 기울기는 0으로 돌아온다.
            delay(DECORATION_TILT_IDLE_MS)
            smoothedVelocityDpPerMs = 0f
            tilt.animateTo(0f, tween(DECORATION_TILT_RETURN_MS, easing = DecorationEaseOutCubic))
        }
    }

    fun onRelease() {
        tiltJob?.cancel()
        settleJob?.cancel()
        settleJob = scope.launch {
            launch {
                if (reduceMotion) tilt.snapTo(0f)
                else tilt.animateTo(0f, tween(spec.dropMs, easing = DecorationEaseOutCubic))
            }
            if (reduceMotion) {
                lift.snapTo(0f)
            } else {
                lift.animateTo(
                    0f,
                    keyframes {
                        durationMillis = spec.dropMs
                        spec.settleLift at spec.settleAtMs using DecorationEaseOutCubic
                        0f at spec.dropMs using FastOutSlowInEasing
                    }
                )
            }
            isLifted = false
        }
    }
}

@Composable
internal fun rememberDecorationLiftState(itemId: Any, spec: DecorationLiftSpec): DecorationLiftState {
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberSystemReducedMotion()
    val shadowLayer = rememberGraphicsLayer()
    return remember(itemId, spec, reduceMotion, shadowLayer) {
        DecorationLiftState(scope, reduceMotion, spec, shadowLayer)
    }
}

/**
 * 손가락이 닿고 떨어지는 것만 Initial pass에서 지켜본다(소비하지 않음 — 기존 탭·변형 제스처와
 * 그 결과·Undo 기록 시점은 그대로). [toParentDeltaX]는 로컬 이동량을 엽서 기준 가로 이동량으로 바꾼다.
 */
internal suspend fun PointerInputScope.trackDecorationLift(
    state: DecorationLiftState,
    toParentDeltaX: (Offset) -> Float
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        state.onPress()
        try {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val deltaMillis = change.uptimeMillis - change.previousUptimeMillis
                val deltaXDp = toParentDeltaX(change.position - change.previousPosition).toDp().value
                state.onDrag(
                    deltaXDp = deltaXDp,
                    deltaMillis = deltaMillis,
                    multiTouch = event.changes.count { it.pressed } >= 2
                )
            }
        } finally {
            // 손을 떼든, 취소되든(제스처 재시작·탭 전환·화면 이탈) 항상 내려앉는다.
            state.onRelease()
        }
    }
}

/**
 * 위치 Box에서 회전 graphicsLayer보다 "앞"에 붙여, 들림·기울기를 화면 기준 그리기 변환으로만
 * 적용한다(터치 좌표·레이아웃 크기는 바꾸지 않음).
 */
internal fun Modifier.decorationLiftTransform(state: DecorationLiftState): Modifier =
    drawWithContent {
        val visual = decorationLiftVisual(state.lift.value, state.spec, state.reduceMotion)
        val tiltDeg = state.tilt.value
        if (visual.scale == 1f && visual.liftDp == 0f && tiltDeg == 0f) {
            drawContent()
        } else {
            val liftPx = visual.liftDp.dp.toPx()
            withTransform({
                translate(top = -liftPx)
                rotate(tiltDeg, pivot = center)
                scale(visual.scale, visual.scale, pivot = center)
            }) {
                this@drawWithContent.drawContent()
            }
        }
    }

/**
 * 내용(글자·라벨·테이프 그리기)에 붙이는 실루엣 그림자. 들린 동안만 내용을 기록해 어둡게 물들이고
 * (API 31+에선 흐림까지) 종이 쪽으로 내려 깐 뒤, 그 위에 원래 내용을 그대로 그린다.
 * 내용은 회전 graphicsLayer 안쪽이므로 "화면 아래" 방향을 로컬 좌표로 돌려 쓰고,
 * 바깥 들림 높이만큼 더 내려 그림자가 종이 위에 남게 한다.
 */
internal fun Modifier.decorationLiftShadow(
    state: DecorationLiftState,
    rotationDegrees: Float
): Modifier =
    drawWithContent {
        if (!state.isLifted) {
            drawContent()
            return@drawWithContent
        }
        val visual = decorationLiftVisual(state.lift.value, state.spec, state.reduceMotion)
        if (visual.shadowAlpha > 0f && size.width > 0f && size.height > 0f) {
            val blurPx = visual.shadowBlurDp.dp.toPx()
            val dropPx = (visual.shadowOffsetDp + visual.liftDp).dp.toPx()
            // 흐림·이동이 기록 영역 경계에서 잘리지 않도록 여백을 두고 기록한다.
            val pad = ceil(blurPx * 3f + dropPx).toInt().coerceAtLeast(1)
            val layer = state.shadowLayer
            layer.record(
                size = IntSize(
                    ceil(size.width).toInt() + pad * 2,
                    ceil(size.height).toInt() + pad * 2
                )
            ) {
                translate(pad.toFloat(), pad.toFloat()) {
                    this@drawWithContent.drawContent()
                }
            }
            layer.colorFilter = ColorFilter.tint(DecorationShadowInk, BlendMode.SrcIn)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0f) {
                layer.alpha = visual.shadowAlpha
                layer.renderEffect = BlurEffect(blurPx, blurPx, TileMode.Decal)
            } else {
                // 흐림을 쓸 수 없는 기기에선 또렷한 실루엣이 진해 보이지 않게 더 옅게.
                layer.alpha = visual.shadowAlpha * 0.5f
                layer.renderEffect = null
            }
            val drop = screenDownOffsetInRotatedSpace(dropPx, rotationDegrees)
            translate(drop.x - pad, drop.y - pad) {
                drawLayer(layer)
            }
        }
        drawContent()
    }

internal data class DecorationLiftVisual(
    val scale: Float,
    val liftDp: Float,
    val shadowOffsetDp: Float,
    val shadowBlurDp: Float,
    val shadowAlpha: Float
)

/** 들림 값 하나로 크기·높이·그림자를 정한다. 0이면 기존과 같은 쉬는 상태(크기 1, 그림자 없음). */
internal fun decorationLiftVisual(
    lift: Float,
    spec: DecorationLiftSpec,
    reduceMotion: Boolean = false
): DecorationLiftVisual {
    val l = if (lift.isFinite()) lift.coerceIn(spec.settleLift, 1f) else 0f
    val up = l.coerceAtLeast(0f)
    return DecorationLiftVisual(
        scale = if (reduceMotion) 1f else 1f + spec.liftScale * l,
        liftDp = if (reduceMotion) 0f else spec.liftDp * up,
        shadowOffsetDp = spec.shadowRestOffsetDp + (spec.shadowLiftedOffsetDp - spec.shadowRestOffsetDp) * up,
        shadowBlurDp = spec.shadowBlurDp * up,
        shadowAlpha = spec.shadowAlpha * up
    )
}

/** 끄는 가로 속도(dp/ms)에 비례한 기울기, 재질별 최대각까지. */
internal fun decorationDragTiltTarget(velocityDpPerMs: Float, spec: DecorationLiftSpec): Float =
    if (!velocityDpPerMs.isFinite()) 0f
    else (velocityDpPerMs * spec.tiltPerSpeed).coerceIn(-spec.tiltMaxDeg, spec.tiltMaxDeg)

/**
 * 화면 기준 아래로 [distance]만큼의 이동을, rotationZ = [rotationDegrees]로 돌아간 로컬 좌표로
 * 바꾼다(로컬 → 화면 회전의 역회전). 회전 0이면 (0, distance).
 */
internal fun screenDownOffsetInRotatedSpace(distance: Float, rotationDegrees: Float): Offset {
    if (!distance.isFinite() || !rotationDegrees.isFinite()) return Offset.Zero
    val rad = Math.toRadians(rotationDegrees.toDouble())
    return Offset(
        x = (distance * sin(rad)).toFloat(),
        y = (distance * cos(rad)).toFloat()
    )
}

private val DecorationEaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val DecorationEaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

private const val DECORATION_TILT_FOLLOW_MS = 90
private const val DECORATION_TILT_IDLE_MS = 60L
private const val DECORATION_TILT_RETURN_MS = 220

// 사진 스티커 누끼 그림자와 같은 따뜻한 먹색.
private val DecorationShadowInk = Color(0xFF3B3226)
