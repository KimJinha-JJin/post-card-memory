package com.postcardmemory.ui.detail

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.postcardmemory.R
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.utils.SealInkWearRenderer
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 도장 "찍기" 인터랙션의 진행 상태. 도장 자체는 여전히 [PostcardSealItem] 하나이고,
 * 이 상태는 화면에만 잠깐 존재하는 조준·손 연출용이다 — 저장·복원·export 어디에도
 * 들어가지 않는다.
 */
internal sealed interface SealStampPhase {
    data object Idle : SealStampPhase

    /**
     * 새 도장 종류·색을 고른 뒤, 엽서 위 십자(+)와 반투명 미리보기로 찍을 자리·
     * 크기·각도를 고르는 중. [sealId]를 미리 정해 두어 미리보기의 잉크 결손이
     * 실제로 찍힐 도장과 똑같다(결손은 id에서 결정된다).
     */
    data class Aiming(
        val sealId: String,
        val type: SealType,
        val colorArgb: Long,
        /** 도장 중심이 닿을 자리. 엽서 미리보기 좌상단 기준 px. */
        val center: Offset,
        val scale: Float,
        val rotationDegrees: Float
    ) : SealStampPhase

    /** `도장 찍기`를 누른 뒤 손이 올라와 찍고 빠지는 동안. 조준 값은 여기서 고정된다. */
    data class Stamping(
        val aim: Aiming,
        /** 손이 종이에 닿아 실제 도장이 만들어졌는지. */
        val stamped: Boolean = false
    ) : SealStampPhase
}

/**
 * 조준 → 찍기 → 끝의 순서를 한곳에서 지킨다. 연타·중복 호출이 와도 도장은
 * 한 번 찍기에 정확히 하나만 만들어지도록, 각 전이는 허용된 단계에서만 일어난다.
 */
internal class SealStampSession {
    var phase: SealStampPhase by mutableStateOf(SealStampPhase.Idle)
        private set

    val isAiming: Boolean get() = phase is SealStampPhase.Aiming
    val isStamping: Boolean get() = phase is SealStampPhase.Stamping

    /** 손 연출 중에는 새 조준을 시작하지 않는다. 조준 중이면 새 종류·색으로 다시 시작한다. */
    fun startAiming(type: SealType, colorArgb: Long, center: Offset): Boolean {
        if (isStamping) return false
        phase =
            SealStampPhase.Aiming(
                sealId = UUID.randomUUID().toString(),
                type = type,
                colorArgb = colorArgb,
                center = center,
                scale = type.defaultScale,
                rotationDegrees = 0f
            )
        return true
    }

    fun moveAim(center: Offset) {
        val current = phase as? SealStampPhase.Aiming ?: return
        phase = current.copy(center = center)
    }

    /** 두 손가락 확대·축소·회전. 범위는 찍힌 도장 편집과 같다(0.5~3배). */
    fun transformAim(zoom: Float, rotationChange: Float) {
        val current = phase as? SealStampPhase.Aiming ?: return
        phase =
            current.copy(
                scale = (current.scale * zoom).coerceIn(SEAL_STAMP_MIN_SCALE, SEAL_STAMP_MAX_SCALE),
                rotationDegrees = normalizeStickerRotation(current.rotationDegrees + rotationChange)
            )
    }

    /** 조준 중일 때만 취소된다. 손이 이미 출발했으면 무시한다(중간에 끊으면 유령 상태가 남는다). */
    fun cancelAiming(): Boolean {
        if (!isAiming) return false
        phase = SealStampPhase.Idle
        return true
    }

    /** `도장 찍기`. 조준 중일 때 딱 한 번만 true — 연타해도 손은 하나만 나온다. */
    fun beginStamp(): Boolean {
        val current = phase as? SealStampPhase.Aiming ?: return false
        phase = SealStampPhase.Stamping(aim = current)
        return true
    }

    /**
     * 도장 바닥이 종이에 닿은 순간 추가할 도장을 돌려준다. 같은 찍기에서 두 번째
     * 호출부터는 null이라 도장·undo 기록이 중복되지 않는다.
     */
    fun takeContactSeal(baseSealSidePx: Float): PostcardSealItem? {
        val current = phase as? SealStampPhase.Stamping ?: return null
        if (current.stamped) return null

        val aim = current.aim
        val seal =
            PostcardSealItem(
                id = aim.sealId,
                type = aim.type,
                offset = sealOffsetForStampCenter(aim.center, baseSealSidePx * aim.scale),
                scale = aim.scale,
                rotationDegrees = aim.rotationDegrees,
                colorArgb = aim.colorArgb
            )
        phase = current.copy(stamped = true)
        return seal
    }

    /** 손이 다 빠졌을 때. 찍힌 도장 id(선택 표시용)를 돌려주고 상태를 비운다. */
    fun finish(): String? {
        val current = phase as? SealStampPhase.Stamping
        phase = SealStampPhase.Idle
        return current?.takeIf { it.stamped }?.aim?.sealId
    }
}

/** 도장 중심 좌표 → [PostcardSealItem.offset](도장 시각 영역의 좌상단). */
internal fun sealOffsetForStampCenter(center: Offset, sealSidePx: Float): Offset =
    Offset(center.x - sealSidePx / 2f, center.y - sealSidePx / 2f)

/** 십자는 엽서 밖으로 나가지 않는다. */
internal fun clampStampCenter(center: Offset, postcardSize: IntSize): Offset =
    Offset(
        center.x.coerceIn(0f, postcardSize.width.toFloat()),
        center.y.coerceIn(0f, postcardSize.height.toFloat())
    )

/**
 * 조준 중 엽서 위: 찍힐 도장의 반투명 미리보기 + 작은 +.
 * 탭하면 그 자리로, 한 손가락으로 끌면 따라오고, 두 손가락으로 돌리거나
 * 크기를 바꾼다. 이 동안 엽서의 다른 제스처는 막힌다.
 */
@Composable
internal fun SealStampAimLayer(
    aim: SealStampPhase.Aiming,
    capturedAtMillis: Long?,
    sealBaseSize: Dp,
    postcardSize: IntSize,
    onMoveAim: (Offset) -> Unit,
    onTransformAim: (zoom: Float, rotationChange: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnMoveAim by rememberUpdatedState(onMoveAim)
    val currentOnTransformAim by rememberUpdatedState(onTransformAim)
    val currentPostcardSize by rememberUpdatedState(postcardSize)
    val currentCenter by rememberUpdatedState(aim.center)

    // 실제로 찍힐 도장과 같은 id → 같은 잉크 결손을 미리 보여준다.
    val inkEraseMask =
        remember(aim.sealId) {
            SealInkWearRenderer.createEraseMask(sealInkWear(sealInkSeed(aim.sealId)))
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                coroutineScope {
                    launch {
                        detectTapGestures(
                            onTap = { position ->
                                currentOnMoveAim(
                                    clampStampCenter(position, currentPostcardSize)
                                )
                            }
                        )
                    }
                    launch {
                        detectTransformGestures { _, pan, zoom, rotationChange ->
                            currentOnMoveAim(
                                clampStampCenter(currentCenter + pan, currentPostcardSize)
                            )
                            currentOnTransformAim(zoom, rotationChange)
                        }
                    }
                }
            }
    ) {
        val sealSide = sealBaseSize * aim.scale
        Box(
            modifier = Modifier
                .offset {
                    val halfPx = sealSide.toPx() / 2f
                    IntOffset(
                        (aim.center.x - halfPx).roundToInt(),
                        (aim.center.y - halfPx).roundToInt()
                    )
                }
                .size(sealSide)
                .graphicsLayer {
                    rotationZ = aim.rotationDegrees
                    alpha = SEAL_STAMP_PREVIEW_ALPHA
                }
        ) {
            SealPreviewContent(
                type = aim.type,
                color = Color(aim.colorArgb),
                capturedAtMillis = capturedAtMillis,
                modifier = Modifier.fillMaxSize(),
                inkEraseMask = inkEraseMask
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = aim.center
            val arm = SEAL_STAMP_CROSSHAIR_ARM.toPx()
            // 사진이 어두운 곳에서도 보이게 얇은 종이색 바탕선을 먼저 깔고
            // 그 위에 흑연색 선을 긋는다(발광·원형 조준경 없음).
            listOf(
                Color(0xCCFFFDF7) to SEAL_STAMP_CROSSHAIR_UNDERLAY_WIDTH.toPx(),
                GraphiteAccent to SEAL_STAMP_CROSSHAIR_WIDTH.toPx()
            ).forEach { (lineColor, width) ->
                drawLine(
                    color = lineColor,
                    start = Offset(center.x - arm, center.y),
                    end = Offset(center.x + arm, center.y),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = lineColor,
                    start = Offset(center.x, center.y - arm),
                    end = Offset(center.x, center.y + arm),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * 신문지에서 오린 손이 화면 아래에서 올라와 [target]에 도장을 콩 찍고 빠지는
 * 일시 overlay. [target]은 이 overlay 좌상단 기준 px이며, 손 이미지의 중심이
 * 아니라 도장 바닥 중심([SEAL_STAMP_HAND_ANCHOR_X]/[SEAL_STAMP_HAND_ANCHOR_Y])이
 * 정확히 그 점에 닿는다. 연출 중에는 아래 화면 입력을 막는다.
 *
 * 손이 닿는 프레임에 [onContact] 한 번, 다 빠진 뒤 [onFinished] 한 번 부른다.
 */
@Composable
internal fun SealStampHandOverlay(
    target: Offset,
    onContact: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hand: ImageBitmap = ImageBitmap.imageResource(R.drawable.seal_stamp_hand)
    val currentOnContact by rememberUpdatedState(onContact)
    val currentOnFinished by rememberUpdatedState(onFinished)

    // 1 = 화면 아래(카메라 가까이), 0 = 종이에 닿음.
    val travel = remember { Animatable(1f) }
    // 0 = 닿기만 함, 1 = 꾹 눌림.
    val press = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 쑤욱: 빠르게 올라와 종이 가까이에서 길게 감속한다(넘침·반동 없음).
        travel.animateTo(
            targetValue = 0f,
            animationSpec = tween(SEAL_STAMP_ENTER_MS, easing = SealStampEnterEasing)
        )
        currentOnContact()
        // 콩: 반동 없이 짧게 눌렀다가 그대로 뗀다.
        press.animateTo(1f, tween(SEAL_STAMP_PRESS_IN_MS, easing = LinearEasing))
        delay(SEAL_STAMP_HOLD_MS)
        press.animateTo(0f, tween(SEAL_STAMP_PRESS_OUT_MS, easing = LinearEasing))
        travel.animateTo(
            targetValue = 1f,
            animationSpec = tween(SEAL_STAMP_EXIT_MS, easing = SealStampExitEasing)
        )
        currentOnFinished()
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        val side = SEAL_STAMP_HAND_SIZE.toPx()
        val t = travel.value
        val anchorX = SEAL_STAMP_HAND_ANCHOR_X * side
        val anchorY = SEAL_STAMP_HAND_ANCHOR_Y * side

        // 손이 카메라에 가까울수록 크다 → 종이로 내려가며 작아지고, 누르는 동안
        // 아주 조금 더 작아진다(종이 쪽으로 파고듦).
        val handScale =
            1f + SEAL_STAMP_APPROACH_SCALE * t - SEAL_STAMP_PRESS_SCALE * press.value

        // 출발점: 손 이미지 윗부분까지 화면 아래로 완전히 숨는 거리.
        val hiddenDistance =
            (size.height - target.y) + anchorY * (1f + SEAL_STAMP_APPROACH_SCALE) +
                    SEAL_STAMP_HAND_OFFSCREEN_MARGIN.toPx()
        val dx = SEAL_STAMP_HAND_DRIFT_X * side * t
        val dy = hiddenDistance * t
        // 빳빳한 종이 한 장처럼 아주 작은 기울기만.
        val tilt = SEAL_STAMP_HAND_TILT_DEGREES * t

        translate(left = target.x + dx, top = target.y + dy) {
            rotate(degrees = tilt, pivot = Offset.Zero) {
                scale(scale = handScale, pivot = Offset.Zero) {
                    drawImage(
                        image = hand,
                        dstOffset = IntOffset(-anchorX.roundToInt(), -anchorY.roundToInt()),
                        dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                        filterQuality = FilterQuality.Medium
                    )
                }
            }
        }
    }
}

/** 도장 바닥이 종이에 닿는 순간의 "콩" 한 번. 기존 화면들과 같은 Vibrator 방식. */
internal fun vibrateSealStampContact(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(
        VibrationEffect.createOneShot(
            SEAL_STAMP_HAPTIC_DURATION_MS,
            SEAL_STAMP_HAPTIC_AMPLITUDE
        )
    )
}

// 손 이미지(seal_stamp_hand, 1254×1254)에서 도장 바닥 중심 — 손잡이가 주먹으로
// 들어가는 지점(575, 540)을 이미지 한 변 기준으로 정규화. 실기기 QA에서 보정한다.
internal const val SEAL_STAMP_HAND_ANCHOR_X = 0.459f
internal const val SEAL_STAMP_HAND_ANCHOR_Y = 0.431f

private val SEAL_STAMP_HAND_SIZE = 300.dp
private val SEAL_STAMP_HAND_OFFSCREEN_MARGIN = 24.dp
private const val SEAL_STAMP_APPROACH_SCALE = 0.12f
private const val SEAL_STAMP_PRESS_SCALE = 0.015f
private const val SEAL_STAMP_HAND_DRIFT_X = 0.12f
private const val SEAL_STAMP_HAND_TILT_DEGREES = 4f

private const val SEAL_STAMP_ENTER_MS = 300
private const val SEAL_STAMP_PRESS_IN_MS = 50
private const val SEAL_STAMP_HOLD_MS = 90L
private const val SEAL_STAMP_PRESS_OUT_MS = 60
private const val SEAL_STAMP_EXIT_MS = 220

private val SealStampEnterEasing = CubicBezierEasing(0.2f, 0.75f, 0.25f, 1f)
private val SealStampExitEasing = CubicBezierEasing(0.55f, 0f, 0.85f, 0.4f)

private const val SEAL_STAMP_HAPTIC_DURATION_MS = 28L
private const val SEAL_STAMP_HAPTIC_AMPLITUDE = 200

private const val SEAL_STAMP_PREVIEW_ALPHA = 0.45f
private const val SEAL_STAMP_MIN_SCALE = 0.5f
private const val SEAL_STAMP_MAX_SCALE = 3f

private val SEAL_STAMP_CROSSHAIR_ARM = 7.dp
private val SEAL_STAMP_CROSSHAIR_WIDTH = 1.5.dp
private val SEAL_STAMP_CROSSHAIR_UNDERLAY_WIDTH = 3.5.dp
