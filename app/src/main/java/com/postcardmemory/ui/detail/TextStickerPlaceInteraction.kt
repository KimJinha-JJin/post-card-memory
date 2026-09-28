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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.postcardmemory.ui.components.TEXT_STICKER_BASE_FONT_SIZE_SP
import com.postcardmemory.ui.components.TextStickerContent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 87일차 후속: 텍스트 스티커 "핀셋으로 톡" 붙이기. 사진·라벨 스티커처럼 자리·크기·각도를
 * 먼저 고른 뒤 `붙이기`를 누르면 핀셋 손이 외곽선 있는 글자 스티커를 집어 와 놓는다.
 * 화면에만 있는 상태라 저장·복원·export와 무관하다.
 */
internal sealed interface TextStickerPlacePhase {
    data object Idle : TextStickerPlacePhase

    data class Aiming(
        /** 붙일 텍스트 스티커 그대로(id·문구·색). 자리·크기·각도는 아래 값으로 채워진다. */
        val draft: TextStickerItem,
        /** 스티커 중심이 놓일 자리. 엽서 미리보기 좌상단 기준 px. */
        val center: Offset,
        val scale: Float,
        val rotationDegrees: Float
    ) : TextStickerPlacePhase

    data class Placing(
        val aim: Aiming,
        val placed: Boolean = false
    ) : TextStickerPlacePhase
}

internal class TextStickerPlaceSession {
    var phase: TextStickerPlacePhase by mutableStateOf(TextStickerPlacePhase.Idle)
        private set

    val isAiming: Boolean get() = phase is TextStickerPlacePhase.Aiming
    val isPlacing: Boolean get() = phase is TextStickerPlacePhase.Placing

    /** 손 연출 중이면 false. 조준 중이면 새 글자로 바꾼다. */
    fun startAiming(draft: TextStickerItem, postcardSize: IntSize): Boolean {
        if (isPlacing) return false
        phase =
            TextStickerPlacePhase.Aiming(
                draft = draft.copy(offset = null, scale = 1f, rotationDegrees = 0f),
                center = Offset(postcardSize.width / 2f, postcardSize.height / 2f),
                scale = 1f,
                rotationDegrees = 0f
            )
        return true
    }

    /** [textSize]는 지금 크기로 그려진 미리보기의 실제 크기(회전 전). 아직 모르면 Zero. */
    fun moveAim(center: Offset, postcardSize: IntSize, textSize: IntSize) {
        val current = phase as? TextStickerPlacePhase.Aiming ?: return
        phase = current.copy(center = clampStickerAimCenter(center, textSize, postcardSize))
    }

    /** 두 손가락 확대·축소·회전. 범위는 붙인 텍스트 스티커 편집과 같다(0.5~3배). */
    fun transformAim(
        zoom: Float,
        rotationChange: Float,
        postcardSize: IntSize,
        textSize: IntSize
    ) {
        val current = phase as? TextStickerPlacePhase.Aiming ?: return
        val scale =
            (current.scale * zoom).coerceIn(TEXT_STICKER_PLACE_MIN_SCALE, TEXT_STICKER_PLACE_MAX_SCALE)
        // 글자 크기는 배율에 비례하므로 새 배율의 크기로 어림해 다시 가둔다.
        val ratio = scale / current.scale
        val scaledSize =
            IntSize(
                (textSize.width * ratio).roundToInt(),
                (textSize.height * ratio).roundToInt()
            )
        phase =
            current.copy(
                scale = scale,
                rotationDegrees = normalizeStickerRotation(current.rotationDegrees + rotationChange),
                center = clampStickerAimCenter(current.center, scaledSize, postcardSize)
            )
    }

    fun cancelAiming(): Boolean {
        if (!isAiming) return false
        phase = TextStickerPlacePhase.Idle
        return true
    }

    /**
     * `붙이기`. 조준 중일 때 딱 한 번만 true — 연타해도 손은 하나만 나온다. 마지막으로 잰
     * 크기로 자리를 한 번 더 맞춰 고정해서, 손이 가는 자리 = 실제로 놓이는 자리다.
     */
    fun beginPlace(textSize: IntSize, postcardSize: IntSize): Boolean {
        val current = phase as? TextStickerPlacePhase.Aiming ?: return false
        phase =
            TextStickerPlacePhase.Placing(
                aim = current.copy(
                    center = clampStickerAimCenter(current.center, textSize, postcardSize)
                )
            )
        return true
    }

    /** 스티커가 종이에 닿은 순간 추가할 텍스트 스티커. 같은 붙이기에서 두 번째부터는 null. */
    fun takeContactText(textSize: IntSize): TextStickerItem? {
        val current = phase as? TextStickerPlacePhase.Placing ?: return null
        if (current.placed) return null

        val aim = current.aim
        val sticker =
            aim.draft.copy(
                offset = Offset(
                    aim.center.x - textSize.width / 2f,
                    aim.center.y - textSize.height / 2f
                ),
                scale = aim.scale,
                rotationDegrees = aim.rotationDegrees
            )
        phase = current.copy(placed = true)
        return sticker
    }

    fun finish(): String? {
        val current = phase as? TextStickerPlacePhase.Placing
        phase = TextStickerPlacePhase.Idle
        return current?.takeIf { it.placed }?.aim?.draft?.id
    }
}

/** 붙인 텍스트 스티커와 같은 모양(Box 통째 회전 + [TextStickerContent])으로 글자를 그린다. */
@Composable
private fun TextStickerPlacingContent(
    aim: TextStickerPlacePhase.Aiming,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = aim.rotationDegrees
            this.alpha = alpha
        }
    ) {
        TextStickerContent(
            text = aim.draft.text,
            colorArgb = aim.draft.colorArgb,
            fontSizeSp = TEXT_STICKER_BASE_FONT_SIZE_SP * aim.scale,
            outlineColorArgb = aim.draft.outlineColorArgb
        )
    }
}

/**
 * 텍스트 조준 중 엽서 위: 붙일 글자의 반투명 미리보기 + 작은 +.
 * 그려진 실제 크기를 [onMeasured]로 알려 조준·착지 계산에 쓰게 한다.
 * [interactive]가 false면(손이 오는 동안) 목표 자리 표시만 한다.
 */
@Composable
internal fun TextStickerAimLayer(
    aim: TextStickerPlacePhase.Aiming,
    textSize: IntSize,
    interactive: Boolean,
    onMeasured: (IntSize) -> Unit,
    onMoveAim: (Offset) -> Unit,
    onTransformAim: (zoom: Float, rotationChange: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnMoveAim by rememberUpdatedState(onMoveAim)
    val currentOnTransformAim by rememberUpdatedState(onTransformAim)
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
                                detectTransformGestures { _, pan, zoom, rotationChange ->
                                    currentOnMoveAim(currentCenter + pan)
                                    currentOnTransformAim(zoom, rotationChange)
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
    ) {
        TextStickerPlacingContent(
            aim = aim,
            alpha = TEXT_STICKER_PREVIEW_ALPHA,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (aim.center.x - textSize.width / 2f).roundToInt(),
                        (aim.center.y - textSize.height / 2f).roundToInt()
                    )
                }
                .onSizeChanged(onMeasured)
        )

        if (interactive) {
            StickerPlaceCrosshair(center = aim.center)
        }
    }
}

/**
 * 텍스트용 핀셋 손: 회전한 글자 영역에서 화면 아래를 향한 변의 가운데를 집어 들고 온다
 * (라벨과 같은 [rectStickerGripOffset]).
 */
@Composable
internal fun TextStickerTweezerHandOverlay(
    aim: TextStickerPlacePhase.Aiming,
    textSize: IntSize,
    textCenter: Offset,
    onContact: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val gripOffset =
        remember(textSize, aim.rotationDegrees, density) {
            rectStickerGripOffset(
                widthPx = textSize.width.toFloat(),
                heightPx = textSize.height.toFloat(),
                rotationDegrees = aim.rotationDegrees,
                gripInsetPx = with(density) { STICKER_TWEEZER_GRIP_INSET.toPx() }
            )
        }
    StickerTweezerHandOverlay(
        stickerCenter = textCenter,
        stickerWidth = with(density) { textSize.width.toDp() },
        stickerHeight = with(density) { textSize.height.toDp() },
        gripOffset = gripOffset,
        onContact = onContact,
        onFinished = onFinished,
        modifier = modifier
    ) {
        TextStickerPlacingContent(aim = aim)
    }
}

private const val TEXT_STICKER_PREVIEW_ALPHA = 0.45f
internal const val TEXT_STICKER_PLACE_MIN_SCALE = 0.5f
internal const val TEXT_STICKER_PLACE_MAX_SCALE = 3f
