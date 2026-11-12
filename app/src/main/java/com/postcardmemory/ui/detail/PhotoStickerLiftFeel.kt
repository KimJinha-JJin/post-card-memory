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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postcardmemory.ui.gallery.rememberSystemReducedMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/*
 * 130일차 B(사용자 선택 B안): 사진 스티커를 잡으면 종이에서 살짝 들리고(1.05배·4dp·그림자),
 * 끄는 동안 가로 속도만큼 최대 ±3° 기울었다가 손이 멈추면 바로 서고, 놓으면 종이에 한 번
 * 살짝 눌리며 내려앉는다(반복 진동 없음). 목업: docs/mockups/paper-touch-feel-day130.html.
 *
 * 전부 "그리기"만 바꾼다. 들림·기울기는 위치 Box의 drawWithContent 변환이라 제스처 좌표계
 * (graphicsLayer 회전·대칭 + localStickerDeltaToParent)는 그대로이고, 저장되는 위치·크기·회전,
 * Undo/Redo 스냅샷, 공유·내보내기 결과에도 들어가지 않는다. 쉬는 상태에서는 기존과 똑같이
 * 그려지고(그림자 없음) 그림자 요소는 들린 동안에만 구성한다.
 */

@Stable
internal class PhotoStickerLiftState(
    private val scope: CoroutineScope,
    val reduceMotion: Boolean
) {
    /** 1 = 들림, 0 = 붙어 있음. 내려앉을 때 잠깐 음수(종이에 눌림)까지 간다. */
    val lift = Animatable(0f)
    val tilt = Animatable(0f)

    /** 손가락이 닿은 뒤 내려앉기가 끝날 때까지 true — 그림자 요소를 이 동안만 구성한다. */
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
                lift.animateTo(1f, tween(PHOTO_STICKER_LIFT_IN_MS, easing = PaperEaseOutQuint))
            }
        }
        scope.launch { tilt.stop() }
    }

    fun onDrag(deltaXDp: Float, deltaMillis: Long, multiTouch: Boolean) {
        if (reduceMotion) return
        smoothedVelocityDpPerMs = smoothPhotoStickerVelocity(smoothedVelocityDpPerMs, deltaXDp, deltaMillis)
        val target = if (multiTouch) 0f else photoStickerDragTiltTarget(smoothedVelocityDpPerMs)
        tiltJob?.cancel()
        tiltJob = scope.launch {
            tilt.animateTo(target, tween(PHOTO_STICKER_TILT_FOLLOW_MS, easing = LinearOutSlowInEasing))
            // 손이 멈추면(다음 움직임이 오지 않으면) 기울기는 0으로 돌아온다.
            delay(PHOTO_STICKER_TILT_IDLE_MS)
            smoothedVelocityDpPerMs = 0f
            tilt.animateTo(0f, tween(PHOTO_STICKER_TILT_RETURN_MS, easing = PaperEaseOutCubicDetail))
        }
    }

    fun onRelease() {
        tiltJob?.cancel()
        settleJob?.cancel()
        settleJob = scope.launch {
            launch {
                if (reduceMotion) tilt.snapTo(0f)
                else tilt.animateTo(0f, tween(PHOTO_STICKER_DROP_MS, easing = PaperEaseOutCubicDetail))
            }
            if (reduceMotion) {
                lift.snapTo(0f)
            } else {
                lift.animateTo(
                    0f,
                    keyframes {
                        durationMillis = PHOTO_STICKER_DROP_MS
                        PHOTO_STICKER_SETTLE_LIFT at PHOTO_STICKER_SETTLE_AT_MS using PaperEaseOutCubicDetail
                        0f at PHOTO_STICKER_DROP_MS using FastOutSlowInEasing
                    }
                )
            }
            isLifted = false
        }
    }
}

@Composable
internal fun rememberPhotoStickerLiftState(stickerId: Any): PhotoStickerLiftState {
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberSystemReducedMotion()
    return remember(stickerId, reduceMotion) { PhotoStickerLiftState(scope, reduceMotion) }
}

/**
 * 손가락이 닿고 떨어지는 것만 Initial pass에서 지켜본다(소비하지 않음 — 기존 탭·변형 제스처와
 * 그 결과는 그대로). [toParentDeltaX]는 스티커 로컬 이동량을 엽서 기준 가로 이동량으로 바꾼다.
 */
internal suspend fun PointerInputScope.trackPhotoStickerLift(
    state: PhotoStickerLiftState,
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
            // 손을 떼든, 취소되든(제스처 재시작·화면 이탈) 항상 내려앉는다.
            state.onRelease()
        }
    }
}

/** 위치 Box에 붙여 들림·기울기를 그리기 변환으로만 적용한다(터치 좌표는 바꾸지 않음). */
internal fun Modifier.photoStickerLiftTransform(state: PhotoStickerLiftState): Modifier =
    drawWithContent {
        val visual = photoStickerLiftVisual(state.lift.value, state.reduceMotion)
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
 * 들린 동안만 스티커 아래에 깔리는 그림자. 종이·일반 사진 스티커는 둥근 사각형 elevation 그림자,
 * 누끼는 실루엣을 흐려 쓴다(API 31+; 그 아래는 흐림을 쓸 수 없어 누끼 그림자를 생략).
 * 위치 Box의 그리기 변환으로 함께 들리므로, 들린 만큼 다시 내려 종이 위에 남게 한다.
 */
@Composable
internal fun PhotoStickerLiftShadow(
    state: PhotoStickerLiftState,
    model: Any?,
    isCutout: Boolean,
    cornerRadius: Dp,
    rotationDegrees: Float,
    flipHorizontal: Boolean,
    flipVertical: Boolean
) {
    if (!state.isLifted) return
    if (isCutout) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(PhotoStickerShadowInk, BlendMode.SrcIn),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val visual = photoStickerLiftVisual(state.lift.value, state.reduceMotion)
                    rotationZ = rotationDegrees
                    scaleX = if (flipHorizontal) -1f else 1f
                    scaleY = if (flipVertical) -1f else 1f
                    translationY = (visual.cutoutShadowOffsetDp + visual.liftDp).dp.toPx()
                    alpha = visual.cutoutShadowAlpha
                    val blur = visual.cutoutShadowBlurDp.dp.toPx()
                    renderEffect = BlurEffect(blur, blur, TileMode.Decal)
                }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val visual = photoStickerLiftVisual(state.lift.value, state.reduceMotion)
                    rotationZ = rotationDegrees
                    translationY = visual.liftDp.dp.toPx()
                    shadowElevation = visual.shadowElevationDp.dp.toPx()
                    shape = RoundedCornerShape(cornerRadius)
                    spotShadowColor = PhotoStickerSpotShadow
                    ambientShadowColor = PhotoStickerAmbientShadow
                    clip = false
                }
        )
    }
}

internal data class PhotoStickerLiftVisual(
    val scale: Float,
    val liftDp: Float,
    val shadowElevationDp: Float,
    val cutoutShadowOffsetDp: Float,
    val cutoutShadowBlurDp: Float,
    val cutoutShadowAlpha: Float
)

/** 들림 값 하나로 크기·높이·그림자를 정한다. 0이면 기존과 같은 쉬는 상태(크기 1, 그림자 없음). */
internal fun photoStickerLiftVisual(lift: Float, reduceMotion: Boolean = false): PhotoStickerLiftVisual {
    val l = if (lift.isFinite()) lift.coerceIn(PHOTO_STICKER_SETTLE_LIFT, 1f) else 0f
    val up = l.coerceAtLeast(0f)
    return PhotoStickerLiftVisual(
        scale = if (reduceMotion) 1f else 1f + PHOTO_STICKER_LIFT_SCALE * l,
        liftDp = if (reduceMotion) 0f else PHOTO_STICKER_LIFT_DP * up,
        shadowElevationDp = PHOTO_STICKER_LIFT_ELEVATION_DP * up,
        cutoutShadowOffsetDp = 1f + (PHOTO_STICKER_CUTOUT_SHADOW_OFFSET_DP - 1f) * up,
        cutoutShadowBlurDp = 1f + (PHOTO_STICKER_CUTOUT_SHADOW_BLUR_DP - 1f) * up,
        cutoutShadowAlpha = PHOTO_STICKER_CUTOUT_SHADOW_ALPHA * up
    )
}

/** 끄는 가로 속도(dp/ms)에 비례한 기울기, 최대 ±3°. */
internal fun photoStickerDragTiltTarget(velocityDpPerMs: Float): Float =
    if (!velocityDpPerMs.isFinite()) 0f
    else (velocityDpPerMs * PHOTO_STICKER_TILT_PER_SPEED).coerceIn(-PHOTO_STICKER_TILT_MAX_DEG, PHOTO_STICKER_TILT_MAX_DEG)

/** 순간 속도를 이전 값과 섞어(0.6/0.4) 튀지 않게 한다. 시간 간격이 없으면 이전 값을 유지한다. */
internal fun smoothPhotoStickerVelocity(previous: Float, deltaXDp: Float, deltaMillis: Long): Float {
    if (deltaMillis <= 0L || !deltaXDp.isFinite()) return previous
    return previous * 0.6f + (deltaXDp / deltaMillis) * 0.4f
}

private val PaperEaseOutQuint = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
private val PaperEaseOutCubicDetail = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

internal const val PHOTO_STICKER_LIFT_IN_MS = 140
internal const val PHOTO_STICKER_DROP_MS = 200
internal const val PHOTO_STICKER_SETTLE_AT_MS = 130
internal const val PHOTO_STICKER_SETTLE_LIFT = -0.15f
internal const val PHOTO_STICKER_LIFT_SCALE = 0.05f
internal const val PHOTO_STICKER_LIFT_DP = 4f
internal const val PHOTO_STICKER_LIFT_ELEVATION_DP = 10f
internal const val PHOTO_STICKER_CUTOUT_SHADOW_OFFSET_DP = 10f
internal const val PHOTO_STICKER_CUTOUT_SHADOW_BLUR_DP = 8f
internal const val PHOTO_STICKER_CUTOUT_SHADOW_ALPHA = 0.22f
internal const val PHOTO_STICKER_TILT_PER_SPEED = 2f
internal const val PHOTO_STICKER_TILT_MAX_DEG = 3f
private const val PHOTO_STICKER_TILT_FOLLOW_MS = 90
private const val PHOTO_STICKER_TILT_IDLE_MS = 60L
private const val PHOTO_STICKER_TILT_RETURN_MS = 220

private val PhotoStickerShadowInk = Color(0xFF3B3226)
// 108일차 연못 집기 그림자 QA("너무 진해")에서 맞춘 옅은 색과 같은 값.
private val PhotoStickerSpotShadow = Color.Black.copy(alpha = 0.4f)
private val PhotoStickerAmbientShadow = Color.Black.copy(alpha = 0.2f)
