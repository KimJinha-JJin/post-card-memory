package com.postcardmemory.ui.detail

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.LABEL_STICKER_BASE_FONT_SIZE_SP
import com.postcardmemory.ui.components.LabelStickerContent
import com.postcardmemory.utils.LabelStickerRenderer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 87일차 후속: 라벨 스티커 "핀셋으로 톡" 붙이기의 진행 상태. 사진 스티커
 * ([PhotoStickerPlaceSession])와 같은 순서(조준 → 손 → 닿는 순간 생성)를 따른다.
 * 라벨은 붙인 뒤 편집에서도 크기를 바꾸지 않으므로 조준도 자리·각도만 고른다.
 * 파일이 없어서 버릴 것을 돌려줄 필요도 없다.
 */
internal sealed interface LabelStickerPlacePhase {
    data object Idle : LabelStickerPlacePhase

    /** [draft]는 붙일 라벨 그대로(id·문구·테이프 종류). 자리·각도는 아직 비어 있다. */
    data class Aiming(
        val draft: LabelStickerItem,
        /** 라벨 중심이 닿을 자리. 엽서 미리보기 좌상단 기준 px. */
        val center: Offset,
        val rotationDegrees: Float
    ) : LabelStickerPlacePhase

    data class Placing(
        val aim: Aiming,
        val placed: Boolean = false
    ) : LabelStickerPlacePhase
}

internal class LabelStickerPlaceSession {
    var phase: LabelStickerPlacePhase by mutableStateOf(LabelStickerPlacePhase.Idle)
        private set

    val isAiming: Boolean get() = phase is LabelStickerPlacePhase.Aiming
    val isPlacing: Boolean get() = phase is LabelStickerPlacePhase.Placing

    /** 손 연출 중이면 false. 조준 중이면 새 라벨로 바꾼다. */
    fun startAiming(
        draft: LabelStickerItem,
        postcardSize: IntSize,
        labelSize: IntSize
    ): Boolean {
        if (isPlacing) return false
        phase =
            LabelStickerPlacePhase.Aiming(
                draft = draft.copy(offset = null, rotationDegrees = 0f),
                center = clampStickerAimCenter(
                    center = Offset(postcardSize.width / 2f, postcardSize.height / 2f),
                    stickerSize = labelSize,
                    postcardSize = postcardSize
                ),
                rotationDegrees = 0f
            )
        return true
    }

    fun moveAim(center: Offset, postcardSize: IntSize, labelSize: IntSize) {
        val current = phase as? LabelStickerPlacePhase.Aiming ?: return
        phase =
            current.copy(
                center = clampStickerAimCenter(center, labelSize, postcardSize)
            )
    }

    /** 두 손가락 회전. 붙인 라벨 편집처럼 크기는 바꾸지 않는다. */
    fun rotateAim(rotationChange: Float) {
        val current = phase as? LabelStickerPlacePhase.Aiming ?: return
        phase =
            current.copy(
                rotationDegrees = normalizeStickerRotation(current.rotationDegrees + rotationChange)
            )
    }

    fun cancelAiming(): Boolean {
        if (!isAiming) return false
        phase = LabelStickerPlacePhase.Idle
        return true
    }

    fun beginPlace(): Boolean {
        val current = phase as? LabelStickerPlacePhase.Aiming ?: return false
        phase = LabelStickerPlacePhase.Placing(aim = current)
        return true
    }

    /** 라벨이 종이에 닿은 순간 추가할 라벨. 같은 붙이기에서 두 번째부터는 null. */
    fun takeContactLabel(labelSize: IntSize): LabelStickerItem? {
        val current = phase as? LabelStickerPlacePhase.Placing ?: return null
        if (current.placed) return null

        val aim = current.aim
        val label =
            aim.draft.copy(
                offset = Offset(
                    aim.center.x - labelSize.width / 2f,
                    aim.center.y - labelSize.height / 2f
                ),
                rotationDegrees = aim.rotationDegrees
            )
        phase = current.copy(placed = true)
        return label
    }

    fun finish(): String? {
        val current = phase as? LabelStickerPlacePhase.Placing
        phase = LabelStickerPlacePhase.Idle
        return current?.takeIf { it.placed }?.aim?.draft?.id
    }
}

/**
 * 화면에 그려질 라벨 크기(px). [LabelStickerContent]가 같은 렌더러로 제 크기를 정하므로
 * 조준 미리보기·착지 계산이 붙은 라벨과 같은 크기를 쓴다.
 */
internal fun labelStickerSizePx(label: LabelStickerItem, density: Density): IntSize {
    val fontSizePx =
        with(density) { (LABEL_STICKER_BASE_FONT_SIZE_SP * label.scale).sp.toPx() }
    val textPaint = LabelStickerRenderer.createTextPaint(fontSizePx)
    return IntSize(
        LabelStickerRenderer
            .labelWidthPx(text = label.text, fontSizePx = fontSizePx, textPaint = textPaint)
            .roundToInt(),
        LabelStickerRenderer.labelHeightPx(fontSizePx).roundToInt()
    )
}

@Composable
internal fun rememberLabelStickerSizePx(label: LabelStickerItem): IntSize {
    val density = LocalDensity.current
    return remember(label.text, label.scale, density) {
        labelStickerSizePx(label, density)
    }
}

/** 붙인 라벨과 같은 모양(Box 통째 회전 + [LabelStickerContent])으로 라벨을 그린다. */
@Composable
private fun LabelStickerPlacingContent(
    label: LabelStickerItem,
    rotationDegrees: Float,
    alpha: Float = 1f
) {
    Box(
        modifier = Modifier.graphicsLayer {
            rotationZ = rotationDegrees
            this.alpha = alpha
        }
    ) {
        LabelStickerContent(
            text = label.text,
            style = label.style,
            fontSizeSp = LABEL_STICKER_BASE_FONT_SIZE_SP * label.scale,
            customTapeColorArgb = label.customTapeColorArgb
        )
    }
}

/**
 * 라벨 조준 중 엽서 위: 붙일 라벨의 반투명 미리보기 + 작은 +.
 * 탭·한 손가락 끌기로 자리를, 두 손가락으로 각도를 고른다. [interactive]가 false면
 * (손이 오는 동안) 목표 자리 표시만 한다.
 */
@Composable
internal fun LabelStickerAimLayer(
    aim: LabelStickerPlacePhase.Aiming,
    labelSize: IntSize,
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
        Box(
            modifier = Modifier.offset {
                IntOffset(
                    (aim.center.x - labelSize.width / 2f).roundToInt(),
                    (aim.center.y - labelSize.height / 2f).roundToInt()
                )
            }
        ) {
            LabelStickerPlacingContent(
                label = aim.draft,
                rotationDegrees = aim.rotationDegrees,
                alpha = LABEL_STICKER_PREVIEW_ALPHA
            )
        }

        if (interactive) {
            StickerPlaceCrosshair(center = aim.center)
        }
    }
}

/** 라벨용 핀셋 손: 회전한 라벨의 가장 아래 변 가운데를 집어 들고 온다. */
@Composable
internal fun LabelStickerTweezerHandOverlay(
    label: LabelStickerItem,
    labelSize: IntSize,
    labelCenter: Offset,
    labelRotationDegrees: Float,
    onContact: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gripOffset =
        remember(labelSize, labelRotationDegrees, density) {
            rectStickerGripOffset(
                widthPx = labelSize.width.toFloat(),
                heightPx = labelSize.height.toFloat(),
                rotationDegrees = labelRotationDegrees,
                gripInsetPx = with(density) { STICKER_TWEEZER_GRIP_INSET.toPx() }
            )
        }
    StickerTweezerHandOverlay(
        stickerCenter = labelCenter,
        stickerWidth = with(density) { labelSize.width.toDp() },
        stickerHeight = with(density) { labelSize.height.toDp() },
        gripOffset = gripOffset,
        onContact = onContact,
        onFinished = onFinished,
        modifier = modifier
    ) {
        LabelStickerPlacingContent(
            label = label,
            rotationDegrees = labelRotationDegrees
        )
    }
}

private const val LABEL_STICKER_PREVIEW_ALPHA = 0.45f
