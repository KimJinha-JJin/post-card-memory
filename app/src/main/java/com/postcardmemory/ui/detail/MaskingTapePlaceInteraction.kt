package com.postcardmemory.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.postcardmemory.R
import com.postcardmemory.ui.components.MaskingTapeContent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 87일차 후속: 마스킹테이프 "손으로 쓸어 붙이기". 스티커들과 같은 순서(조준 → 손 → 닿는
 * 순간 생성)를 따르고, 손은 테이프의 긴 축을 따라 한 번만 쓸고 빠진다.
 * 조준 중에는 자리(끌기)·각도(두 손가락) 외에 편집창으로 가장자리·길이·굵기·각도를
 * 미리 정할 수 있다. 화면에만 있는 상태라
 * 저장·복원·export와 무관하다.
 */
internal sealed interface MaskingTapePlacePhase {
    data object Idle : MaskingTapePlacePhase

    data class Aiming(
        /** 붙일 테이프 그대로(id·디자인·사진). 자리·각도는 아래 값으로 채워진다. */
        val draft: MaskingTapeItem,
        /** 테이프 중심이 놓일 자리. 엽서 미리보기 좌상단 기준 px. */
        val center: Offset,
        val rotationDegrees: Float
    ) : MaskingTapePlacePhase

    data class Placing(
        val aim: Aiming,
        val placed: Boolean = false
    ) : MaskingTapePlacePhase
}

internal class MaskingTapePlaceSession {
    var phase: MaskingTapePlacePhase by mutableStateOf(MaskingTapePlacePhase.Idle)
        private set

    val isAiming: Boolean get() = phase is MaskingTapePlacePhase.Aiming
    val isPlacing: Boolean get() = phase is MaskingTapePlacePhase.Placing

    /**
     * 새 테이프 조준을 시작한다. 더 이상 쓰이지 않을 초안을 돌려준다 — 조준 중이던
     * 이전 초안, 또는 손 연출 중이라 받지 못한 [draft] 자신. 사진 테이프면 호출부가
     * 그 사진 파일을 정리한다.
     */
    fun startAiming(
        draft: MaskingTapeItem,
        postcardSize: IntSize,
        tapeSize: IntSize
    ): List<MaskingTapeItem> {
        if (isPlacing) return listOf(draft)
        val leftover = (phase as? MaskingTapePlacePhase.Aiming)?.draft
        phase =
            MaskingTapePlacePhase.Aiming(
                draft = draft.copy(offset = null, rotationDegrees = 0f),
                center = clampStickerAimCenter(
                    center = Offset(postcardSize.width / 2f, postcardSize.height / 2f),
                    stickerSize = tapeSize,
                    postcardSize = postcardSize
                ),
                rotationDegrees = 0f
            )
        return listOfNotNull(leftover)
    }

    /** 붙인 테이프 끌기와 같은 규칙(회전 전 사각형이 엽서 안)으로 자리를 가둔다. */
    fun moveAim(center: Offset, postcardSize: IntSize, tapeSize: IntSize) {
        val current = phase as? MaskingTapePlacePhase.Aiming ?: return
        phase = current.copy(center = clampStickerAimCenter(center, tapeSize, postcardSize))
    }

    /** 두 손가락 회전. 범위는 테이프 편집의 회전(-180~180)과 같다. */
    fun rotateAim(rotationChange: Float) {
        val current = phase as? MaskingTapePlacePhase.Aiming ?: return
        phase =
            current.copy(
                rotationDegrees = normalizeStickerRotation(current.rotationDegrees + rotationChange)
            )
    }

    /**
     * 조준 중 편집창(`저장`)에서 고른 가장자리·길이·굵기·각도를 미리보기에 반영한다.
     * 길이·굵기가 바뀌면 새 크기([tapeSizeOf])로 자리를 다시 엽서 안에 가둔다.
     */
    fun editAim(
        edgeStyle: MaskingTapeEdgeStyle,
        lengthScale: Float,
        thicknessScale: Float,
        rotationDegrees: Float,
        postcardSize: IntSize,
        tapeSizeOf: (MaskingTapeItem) -> IntSize
    ) {
        val current = phase as? MaskingTapePlacePhase.Aiming ?: return
        val draft =
            current.draft.copy(
                edgeStyle = edgeStyle,
                lengthScale = lengthScale.coerceIn(
                    MASKING_TAPE_MIN_LENGTH_SCALE,
                    MASKING_TAPE_MAX_LENGTH_SCALE
                ),
                thicknessScale = thicknessScale.coerceIn(
                    MASKING_TAPE_MIN_THICKNESS_SCALE,
                    MASKING_TAPE_MAX_THICKNESS_SCALE
                )
            )
        phase =
            current.copy(
                draft = draft,
                center = clampStickerAimCenter(current.center, tapeSizeOf(draft), postcardSize),
                rotationDegrees = normalizeStickerRotation(rotationDegrees)
            )
    }

    /** 조준 중이면 접고 그 초안을 돌려준다. 손 연출 중이면 아무것도 하지 않는다. */
    fun cancelAiming(): List<MaskingTapeItem> {
        val current = phase as? MaskingTapePlacePhase.Aiming ?: return emptyList()
        phase = MaskingTapePlacePhase.Idle
        return listOf(current.draft)
    }

    /** `붙이기`. 조준 중일 때 딱 한 번만 true — 연타해도 손은 하나만 나온다. */
    fun beginPlace(): Boolean {
        val current = phase as? MaskingTapePlacePhase.Aiming ?: return false
        phase = MaskingTapePlacePhase.Placing(aim = current)
        return true
    }

    /** 손이 종이에 닿은 순간 추가할 테이프. 같은 붙이기에서 두 번째부터는 null. */
    fun takeContactTape(tapeSize: IntSize): MaskingTapeItem? {
        val current = phase as? MaskingTapePlacePhase.Placing ?: return null
        if (current.placed) return null

        val aim = current.aim
        val tape =
            aim.draft.copy(
                offset = Offset(
                    aim.center.x - tapeSize.width / 2f,
                    aim.center.y - tapeSize.height / 2f
                ),
                rotationDegrees = aim.rotationDegrees
            )
        phase = current.copy(placed = true)
        return tape
    }

    fun finish(): String? {
        val current = phase as? MaskingTapePlacePhase.Placing
        phase = MaskingTapePlacePhase.Idle
        return current?.takeIf { it.placed }?.aim?.draft?.id
    }

    /** 화면을 떠날 때: 아직 종이에 닿지 않은 초안을 돌려주고 접는다. */
    fun abandon(): List<MaskingTapeItem> {
        val leftover =
            when (val current = phase) {
                is MaskingTapePlacePhase.Aiming -> listOf(current.draft)
                is MaskingTapePlacePhase.Placing ->
                    if (current.placed) emptyList() else listOf(current.aim.draft)
                MaskingTapePlacePhase.Idle -> emptyList()
            }
        phase = MaskingTapePlacePhase.Idle
        return leftover
    }
}

/**
 * 손이 쓸고 가는 길. 손가락이 닿는 점이 [start]에서 [end]까지 테이프 긴 축 위를
 * 한 번 지나간다. 긴 축 방향은 테이프 각도에서 나오며(화면 가로가 아님), 손이
 * 뒤집히지 않도록 각도를 (-90, 90]으로 접어 늘 대체로 왼쪽→오른쪽(수직이면 위→아래)으로 쓴다.
 */
internal data class MaskingTapeSweep(
    val start: Offset,
    val end: Offset,
    /** 손 이미지를 돌릴 각도 = 접은 긴 축 각도. */
    val handRotationDegrees: Float,
    /** true면 손이 테이프 자기 좌표의 +x 반대 방향으로 쓴다(테이프 각도가 90°를 넘을 때). */
    val againstTapeX: Boolean,
    /** 양 끝에서 안쪽으로 들어와 손가락이 닿는 거리. */
    val insetPx: Float
)

internal fun maskingTapeSweep(
    center: Offset,
    lengthPx: Float,
    rotationDegrees: Float,
    insetPx: Float
): MaskingTapeSweep {
    val rotation = normalizeStickerRotation(rotationDegrees)
    val (axisDegrees, againstTapeX) =
        when {
            rotation > 90f -> (rotation - 180f) to true
            rotation <= -90f -> (rotation + 180f) to true
            else -> rotation to false
        }
    val inset = insetPx.coerceIn(0f, lengthPx / 2f)
    val half = lengthPx / 2f - inset
    // graphicsLayer rotationZ와 같은 방향(y 아래, 양수 = 시계방향).
    val radians = Math.toRadians(axisDegrees.toDouble())
    val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
    return MaskingTapeSweep(
        start = center - direction * half,
        end = center + direction * half,
        handRotationDegrees = axisDegrees,
        againstTapeX = againstTapeX,
        insetPx = inset
    )
}

/**
 * 손이 [progress](0~1)만큼 쓸었을 때 아직 눌리지 않은 앞쪽 구간(테이프 자기 좌표 x, 0~[lengthPx]).
 * 밀착 연출이 이 구간만 살짝 옅게 덮는다.
 */
internal fun maskingTapeUnpressedRange(
    sweep: MaskingTapeSweep,
    lengthPx: Float,
    progress: Float
): ClosedFloatingPointRange<Float> {
    val travelled =
        sweep.insetPx + (lengthPx - 2f * sweep.insetPx) * progress.coerceIn(0f, 1f)
    return if (sweep.againstTapeX) {
        0f..(lengthPx - travelled)
    } else {
        travelled..lengthPx
    }
}

internal fun maskingTapeSize(tape: MaskingTapeItem): Pair<Dp, Dp> =
    MASKING_TAPE_BASE_WIDTH * tape.scale * tape.lengthScale to
            MASKING_TAPE_BASE_HEIGHT * tape.scale * tape.thicknessScale

/** 붙인 테이프 Box(`.size(dp)`)와 같은 반올림으로 잰 테이프 크기(px, 회전 전). */
internal fun maskingTapeSizePx(tape: MaskingTapeItem, density: Density): IntSize {
    val (width, height) = maskingTapeSize(tape)
    return with(density) { IntSize(width.roundToPx(), height.roundToPx()) }
}

/** 붙인 테이프와 같은 순서(크기 → 회전 → [MaskingTapeContent])로 테이프를 그린다. */
@Composable
private fun MaskingTapePlacingContent(
    tape: MaskingTapeItem,
    center: Offset,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val (width, height) = maskingTapeSize(tape)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (center.x - width.toPx() / 2f).roundToInt(),
                    (center.y - height.toPx() / 2f).roundToInt()
                )
            }
            .size(width = width, height = height)
            .graphicsLayer {
                rotationZ = rotationDegrees
                this.alpha = alpha
            }
            .then(modifier)
    ) {
        MaskingTapeContent(tape = tape, modifier = Modifier.fillMaxSize())
    }
}

/**
 * 테이프 조준 중 엽서 위: 붙일 테이프의 반투명 미리보기 + 작은 +.
 * 탭·한 손가락 끌기로 자리를, 두 손가락으로 각도를 고른다. [interactive]가 false면
 * (손이 오는 동안) 목표 자리 표시만 한다.
 */
@Composable
internal fun MaskingTapeAimLayer(
    aim: MaskingTapePlacePhase.Aiming,
    interactive: Boolean,
    onMoveAim: (Offset) -> Unit,
    onRotateAim: (rotationChange: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnMoveAim by rememberUpdatedState(onMoveAim)
    val currentOnRotateAim by rememberUpdatedState(onRotateAim)
    val currentCenter by rememberUpdatedState(aim.center)

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (interactive) {
                    Modifier.pointerInput(Unit) {
                        coroutineScope {
                            launch {
                                detectTapGestures(
                                    onTap = { position -> currentOnMoveAim(position) }
                                )
                            }
                            launch {
                                detectTransformGestures { _, pan, _, rotationChange ->
                                    currentOnMoveAim(currentCenter + pan)
                                    currentOnRotateAim(rotationChange)
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        MaskingTapePlacingContent(
            tape = aim.draft,
            center = aim.center,
            rotationDegrees = aim.rotationDegrees,
            alpha = MASKING_TAPE_PREVIEW_ALPHA
        )

        if (interactive) {
            StickerPlaceCrosshair(center = aim.center)
        }
    }
}

/**
 * 신문지에서 오린 손이 화면 아래에서 올라와 테이프 한쪽 끝에 손가락을 대고, 긴 축을
 * 따라 반대쪽 끝까지 한 번 쓸어 붙인 뒤 빠지는 일시 overlay. 좌표는 이 overlay 좌상단
 * 기준 px. 왕복·문지르기·흔들림·반동 없이 한 방향으로 한 번만 지나간다.
 *
 * 손가락이 닿는 프레임에 [onContact] 한 번(실제 테이프 생성), 다 빠진 뒤 [onFinished]
 * 한 번 부른다. 쓸고 가는 동안 아직 눌리지 않은 앞쪽만 종이색으로 살짝 옅게 덮어
 * "지나간 자리부터 붙는" 느낌을 준다 — 이 overlay 안에서만 그리고 사라진다.
 * 연출 중에는 아래 화면 입력을 막는다.
 */
@Composable
internal fun MaskingTapePressHandOverlay(
    tape: MaskingTapeItem,
    tapeCenter: Offset,
    rotationDegrees: Float,
    onContact: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hand: ImageBitmap = ImageBitmap.imageResource(R.drawable.tape_press_hand)
    val currentOnContact by rememberUpdatedState(onContact)
    val currentOnFinished by rememberUpdatedState(onFinished)
    val density = LocalDensity.current

    val tapeSizePx = remember(tape, density) { maskingTapeSizePx(tape, density) }
    val lengthPx = tapeSizePx.width.toFloat()
    val sweep =
        remember(tapeCenter, tapeSizePx, rotationDegrees) {
            maskingTapeSweep(
                center = tapeCenter,
                lengthPx = lengthPx,
                rotationDegrees = rotationDegrees,
                insetPx = tapeSizePx.height * MASKING_TAPE_SWEEP_INSET_RATIO
            )
        }
    val sweepMs =
        remember(tapeSizePx, density) {
            val lengthDp = with(density) { lengthPx.toDp().value }
            (lengthDp * MASKING_TAPE_SWEEP_MS_PER_DP)
                .roundToInt()
                .coerceIn(MASKING_TAPE_SWEEP_MIN_MS, MASKING_TAPE_SWEEP_MAX_MS)
        }

    // 1 = 화면 아래(카메라 가까이), 0 = 종이에 닿음.
    val travel = remember { Animatable(1f) }
    // 0 = 닿기만 함, 1 = 살짝 눌러 댐(손만 아주 조금 작아진다).
    val press = remember { Animatable(0f) }
    // 0 = 시작 끝, 1 = 반대쪽 끝. 한 번만 앞으로 간다.
    val progress = remember { Animatable(0f) }
    // 아직 안 눌린 앞쪽을 덮는 옅은 막의 세기(0~1).
    val unpressedVeil = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        travel.animateTo(
            targetValue = 0f,
            animationSpec = tween(MASKING_TAPE_PLACE_ENTER_MS, easing = MaskingTapePlaceEnterEasing)
        )
        currentOnContact()
        unpressedVeil.snapTo(1f)
        press.animateTo(1f, tween(MASKING_TAPE_PLACE_PRESS_IN_MS, easing = LinearEasing))
        progress.animateTo(1f, tween(sweepMs, easing = FastOutSlowInEasing))
        coroutineScope {
            launch {
                unpressedVeil.animateTo(
                    0f,
                    tween(MASKING_TAPE_PLACE_PRESS_OUT_MS, easing = LinearEasing)
                )
            }
            press.animateTo(0f, tween(MASKING_TAPE_PLACE_PRESS_OUT_MS, easing = LinearEasing))
        }
        travel.animateTo(
            targetValue = 1f,
            animationSpec = tween(MASKING_TAPE_PLACE_EXIT_MS, easing = MaskingTapePlaceExitEasing)
        )
        currentOnFinished()
    }

    val handSidePx = with(density) { MASKING_TAPE_HAND_SIZE.toPx() }
    val anchorX = MASKING_TAPE_HAND_ANCHOR_X * handSidePx
    val anchorY = MASKING_TAPE_HAND_ANCHOR_Y * handSidePx
    val marginPx = with(density) { MASKING_TAPE_HAND_OFFSCREEN_MARGIN.toPx() }

    Box(
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
        // 밀착 연출: 종이 위에 고정된 채(손과 같이 움직이지 않음) 앞쪽만 옅게 덮는다.
        if (unpressedVeil.value > 0f) {
            MaskingTapePlacingContent(
                tape = tape,
                center = tapeCenter,
                rotationDegrees = rotationDegrees,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = MASKING_TAPE_UNPRESSED_VEIL_ALPHA * unpressedVeil.value
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        val range =
                            maskingTapeUnpressedRange(sweep, size.width, progress.value)
                        clipRect(left = range.start, right = range.endInclusive) {
                            this@drawWithContent.drawContent()
                            drawRect(color = MaskingTapeVeilColor, blendMode = BlendMode.SrcIn)
                        }
                    }
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = travel.value
                    // 들어올 때는 시작 끝, 나갈 때는 반대쪽 끝이 기준점이다.
                    val contact = lerpOffset(sweep.start, sweep.end, progress.value)
                    val approachScale = 1f + MASKING_TAPE_APPROACH_SCALE * t
                    val hiddenDistance =
                        (size.height - contact.y) +
                                handSidePx * (1f + MASKING_TAPE_APPROACH_SCALE) +
                                marginPx
                    transformOrigin =
                        TransformOrigin(
                            pivotFractionX = if (size.width > 0f) contact.x / size.width else 0f,
                            pivotFractionY = if (size.height > 0f) contact.y / size.height else 0f
                        )
                    scaleX = approachScale
                    scaleY = approachScale
                    rotationZ = MASKING_TAPE_HAND_TILT_DEGREES * t
                    translationX = MASKING_TAPE_HAND_DRIFT_X * handSidePx * t
                    translationY = hiddenDistance * t
                }
        ) {
            val contact = lerpOffset(sweep.start, sweep.end, progress.value)
            // 누르는 동안 손만 아주 조금 작아진다(종이 쪽으로 눌림). 기준은 손가락이 닿는 점.
            val pressScale = 1f - MASKING_TAPE_PRESS_SCALE * press.value
            val side = handSidePx * pressScale
            rotate(degrees = sweep.handRotationDegrees, pivot = contact) {
                drawImage(
                    image = hand,
                    dstOffset = IntOffset(
                        (contact.x - anchorX * pressScale).roundToInt(),
                        (contact.y - anchorY * pressScale).roundToInt()
                    ),
                    dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                    filterQuality = FilterQuality.Medium
                )
            }
        }
    }
}

private fun lerpOffset(start: Offset, end: Offset, fraction: Float): Offset =
    start + (end - start) * fraction

// 손 이미지(tape_press_hand, 1254×1254)에서 손가락 부분(y 90~500) 불투명 영역의
// 가운데(약 (500, 325)) — 손가락 마디가 테이프를 누르는 자리. 실기기 QA에서 보정한다.
internal const val MASKING_TAPE_HAND_ANCHOR_X = 0.399f
internal const val MASKING_TAPE_HAND_ANCHOR_Y = 0.259f

private val MASKING_TAPE_HAND_SIZE = 260.dp
private val MASKING_TAPE_HAND_OFFSCREEN_MARGIN = 24.dp
private const val MASKING_TAPE_APPROACH_SCALE = 0.12f
private const val MASKING_TAPE_PRESS_SCALE = 0.012f
private const val MASKING_TAPE_HAND_DRIFT_X = 0.16f
private const val MASKING_TAPE_HAND_TILT_DEGREES = 4f

// 손가락이 테이프 끝에서 안쪽으로 들어와 닿는 거리(테이프 굵기 대비).
internal const val MASKING_TAPE_SWEEP_INSET_RATIO = 0.5f

// 스티커 핀셋과 같은 진입·퇴장 리듬. 쓸기는 테이프 길이에 비례하되 너무 짧거나 길지 않게.
private const val MASKING_TAPE_PLACE_ENTER_MS = 320
private const val MASKING_TAPE_PLACE_PRESS_IN_MS = 60
private const val MASKING_TAPE_PLACE_PRESS_OUT_MS = 60
private const val MASKING_TAPE_PLACE_EXIT_MS = 220
private const val MASKING_TAPE_SWEEP_MS_PER_DP = 2.6f
private const val MASKING_TAPE_SWEEP_MIN_MS = 360
private const val MASKING_TAPE_SWEEP_MAX_MS = 760

private val MaskingTapePlaceEnterEasing = CubicBezierEasing(0.2f, 0.75f, 0.25f, 1f)
private val MaskingTapePlaceExitEasing = CubicBezierEasing(0.55f, 0f, 0.85f, 0.4f)

private const val MASKING_TAPE_PREVIEW_ALPHA = 0.45f
private const val MASKING_TAPE_UNPRESSED_VEIL_ALPHA = 0.32f
private val MaskingTapeVeilColor = Color(0xFFFFFDF7)
