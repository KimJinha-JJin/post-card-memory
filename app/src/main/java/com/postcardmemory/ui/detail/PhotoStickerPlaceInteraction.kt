package com.postcardmemory.ui.detail

import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.postcardmemory.R
import com.postcardmemory.ui.theme.GraphiteAccent
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 87일차: 사진 스티커 "핀셋으로 톡" 붙이기의 진행 상태. 도장 찍기([SealStampSession])와
 * 같은 순서(조준 → 손 → 닿는 순간 생성)를 따르지만 코드는 공유하지 않는다.
 * 화면에만 잠깐 존재하는 상태라 저장·복원·export 어디에도 들어가지 않는다.
 */
internal sealed interface PhotoStickerPlacePhase<out S : Any> {
    data object Idle : PhotoStickerPlacePhase<Nothing>

    /**
     * 사진을 고른 뒤 엽서 위 십자(+)와 반투명 미리보기로 붙일 자리·크기·각도·배경제거를
     * 고르는 중. [source]는 이미 앱 저장소로 복사된(또는 SAF로 고른) 원본이다
     * (앱에서는 Uri, 순수 JUnit에서는 Uri를 만들 수 없어 다른 값).
     */
    data class Aiming<S : Any>(
        val stickerId: String,
        val source: S,
        /** 스티커 중심이 닿을 자리. 엽서 미리보기 좌상단 기준 px. */
        val center: Offset,
        val scale: Float,
        val rotationDegrees: Float,
        /** 조준 중에 만든 배경제거 결과. 한 번 만들면 원본복원 후에도 다시 쓰려고 들고 있다. */
        val removedBgSource: S? = null,
        val isBackgroundRemoved: Boolean = false,
        val isRemovingBackground: Boolean = false,
        /** 배경제거 결과의 불투명 표본점([photoStickerCutoutSilhouette]). 핀셋이 집을 자리를 정한다. */
        val cutoutSilhouette: List<Offset>? = null
    ) : PhotoStickerPlacePhase<S> {
        /** 지금 미리보기·실제 스티커에 쓰일 그림. */
        val displayedSource: S
            get() = if (isBackgroundRemoved && removedBgSource != null) removedBgSource else source

        /** 핀셋이 집을 자리 계산에 쓸 표본점. 배경제거를 끈 상태면 null(사각형 아래 변). */
        val activeCutoutSilhouette: List<Offset>?
            get() = cutoutSilhouette.takeIf { isBackgroundRemoved }
    }

    /** `붙이기`를 누른 뒤 핀셋 손이 스티커를 들고 와 놓고 빠지는 동안. 조준 값은 고정된다. */
    data class Placing<S : Any>(
        val aim: Aiming<S>,
        /** 스티커가 종이에 닿아 실제 스티커가 만들어졌는지. */
        val placed: Boolean = false
    ) : PhotoStickerPlacePhase<S>
}

/** 핀셋이 닿는 순간 확정된 새 스티커의 모습. [toPhotoStickerItem]으로 실제 스티커가 된다. */
internal data class PhotoStickerPlacement<S : Any>(
    val stickerId: String,
    val source: S,
    val removedBgSource: S?,
    val isBackgroundRemoved: Boolean,
    /** 엽서 미리보기 좌상단 기준, 스티커 사각형 좌상단 px([PhotoStickerItem.offset]과 같은 의미). */
    val offset: Offset,
    val scale: Float,
    val rotationDegrees: Float
)

/**
 * 배경제거 필드는 기존 `배경제거` 버튼이 만드는 스티커와 같은 모양으로 채운다:
 * 결과가 있으면 removedBgUri에 두고, 켜져 있을 때만 displayedUri가 결과를 가리킨다.
 */
internal fun PhotoStickerPlacement<Uri>.toPhotoStickerItem(): PhotoStickerItem =
    PhotoStickerItem(
        id = stickerId,
        originalUri = source,
        displayedUri = if (isBackgroundRemoved && removedBgSource != null) removedBgSource else source,
        removedBgUri = removedBgSource,
        isBackgroundRemoved = isBackgroundRemoved && removedBgSource != null,
        offset = offset,
        scale = scale,
        rotationDegrees = rotationDegrees
    )

/** 조준 중 배경제거를 새로 돌려야 할 때 넘기는 작업 단위. */
internal data class PhotoStickerAimBackgroundRequest<S : Any>(
    val stickerId: String,
    val source: S
)

/**
 * 조준 → 붙이기 → 끝의 순서를 한곳에서 지킨다. 연타·중복 호출이 와도 스티커는
 * 한 번 붙이기에 정확히 하나만 만들어진다.
 *
 * 조준에 쓰이다 버려진 파일(원본·배경제거 결과: 조준 취소, 새 사진으로 교체, 늦게 도착한
 * 배경제거 결과, 손 연출 중이라 거절)은 반환값으로 돌려준다. 호출자가 참조 여부를
 * 확인해 정리한다 — 이 클래스는 파일을 직접 지우지 않는다.
 */
internal class PhotoStickerPlaceSession<S : Any> {
    var phase: PhotoStickerPlacePhase<S> by mutableStateOf(PhotoStickerPlacePhase.Idle)
        private set

    val isAiming: Boolean get() = phase is PhotoStickerPlacePhase.Aiming
    val isPlacing: Boolean get() = phase is PhotoStickerPlacePhase.Placing

    /**
     * 새 사진으로 조준을 시작한다. 이미 조준 중이면 새 사진으로 바꾼다.
     * 더 이상 쓰이지 않게 된 파일 목록을 돌려준다(손 연출 중이면 새 원본 자체).
     */
    fun startAiming(
        source: S,
        postcardSize: IntSize,
        baseStickerSidePx: Float
    ): List<S> {
        if (isPlacing) return listOf(source)
        val replaced =
            (phase as? PhotoStickerPlacePhase.Aiming<S>)
                ?.leftovers()
                ?.filter { it != source }
                .orEmpty()
        val scale = 1f
        phase =
            PhotoStickerPlacePhase.Aiming(
                stickerId = UUID.randomUUID().toString(),
                source = source,
                center = clampPhotoStickerAimCenter(
                    center = Offset(postcardSize.width / 2f, postcardSize.height / 2f),
                    stickerSidePx = baseStickerSidePx * scale,
                    postcardSize = postcardSize
                ),
                scale = scale,
                rotationDegrees = 0f
            )
        return replaced
    }

    fun moveAim(center: Offset, postcardSize: IntSize, baseStickerSidePx: Float) {
        val current = phase as? PhotoStickerPlacePhase.Aiming<S> ?: return
        phase =
            current.copy(
                center = clampPhotoStickerAimCenter(
                    center = center,
                    stickerSidePx = baseStickerSidePx * current.scale,
                    postcardSize = postcardSize
                )
            )
    }

    /** 두 손가락 확대·축소·회전. 범위는 붙인 스티커 편집과 같다(0.5~2.5배). */
    fun transformAim(
        zoom: Float,
        rotationChange: Float,
        postcardSize: IntSize,
        baseStickerSidePx: Float
    ) {
        val current = phase as? PhotoStickerPlacePhase.Aiming<S> ?: return
        val scale =
            (current.scale * zoom).coerceIn(PHOTO_STICKER_PLACE_MIN_SCALE, PHOTO_STICKER_PLACE_MAX_SCALE)
        phase =
            current.copy(
                scale = scale,
                rotationDegrees = normalizeStickerRotation(current.rotationDegrees + rotationChange),
                // 커진 스티커가 엽서 밖으로 밀려나지 않게 중심을 다시 맞춘다 —
                // 붙인 스티커의 clampStickerOffset과 같은 기준이라 조준 = 착지.
                center = clampPhotoStickerAimCenter(
                    center = current.center,
                    stickerSidePx = baseStickerSidePx * scale,
                    postcardSize = postcardSize
                )
            )
    }

    /**
     * 조준 중 `배경제거`/`원본복원`. 이미 만든 결과가 있으면 켜고 끄기만 하고 null,
     * 새로 돌려야 하면 처리 중으로 표시하고 작업 단위를 돌려준다. 처리 중에는 무시한다.
     */
    fun toggleAimBackground(): PhotoStickerAimBackgroundRequest<S>? {
        val current = phase as? PhotoStickerPlacePhase.Aiming<S> ?: return null
        if (current.isRemovingBackground) return null
        return when {
            current.isBackgroundRemoved -> {
                phase = current.copy(isBackgroundRemoved = false)
                null
            }
            current.removedBgSource != null -> {
                phase = current.copy(isBackgroundRemoved = true)
                null
            }
            else -> {
                phase = current.copy(isRemovingBackground = true)
                PhotoStickerAimBackgroundRequest(current.stickerId, current.source)
            }
        }
    }

    /**
     * 배경제거 결과 도착. 같은 조준이 아직 처리 중이면 켜진 상태로 적용하고 null,
     * 그 사이 조준이 취소·교체·출발했으면 결과를 버릴 파일로 돌려준다.
     */
    fun applyAimBackgroundRemoval(
        stickerId: String,
        removedBgSource: S,
        cutoutSilhouette: List<Offset>
    ): S? {
        val current = phase as? PhotoStickerPlacePhase.Aiming<S>
        if (current == null || current.stickerId != stickerId || !current.isRemovingBackground) {
            return removedBgSource
        }
        phase =
            current.copy(
                removedBgSource = removedBgSource,
                isBackgroundRemoved = true,
                isRemovingBackground = false,
                cutoutSilhouette = cutoutSilhouette
            )
        return null
    }

    /** 배경제거 실패. 같은 조준이면 처리 중 표시만 푼다. */
    fun failAimBackgroundRemoval(stickerId: String) {
        val current = phase as? PhotoStickerPlacePhase.Aiming<S> ?: return
        if (current.stickerId != stickerId) return
        phase = current.copy(isRemovingBackground = false)
    }

    /** 조준 중일 때만 취소된다. 버려진 파일(원본·배경제거 결과)을 돌려준다. 손이 출발했으면 빈 목록. */
    fun cancelAiming(): List<S> {
        val current = phase as? PhotoStickerPlacePhase.Aiming<S> ?: return emptyList()
        phase = PhotoStickerPlacePhase.Idle
        return current.leftovers()
    }

    /** `붙이기`. 조준 중이고 배경제거 처리 중이 아닐 때 딱 한 번만 true — 연타해도 손은 하나만 나온다. */
    fun beginPlace(): Boolean {
        val current = phase as? PhotoStickerPlacePhase.Aiming<S> ?: return false
        if (current.isRemovingBackground) return false
        phase = PhotoStickerPlacePhase.Placing(aim = current)
        return true
    }

    /**
     * 스티커가 종이에 닿은 순간 추가할 스티커 모습을 돌려준다. 같은 붙이기에서 두 번째
     * 호출부터는 null이라 스티커·undo 기록이 중복되지 않는다.
     */
    fun takeContactPlacement(baseStickerSidePx: Float): PhotoStickerPlacement<S>? {
        val current = phase as? PhotoStickerPlacePhase.Placing<S> ?: return null
        if (current.placed) return null

        val aim = current.aim
        val side = baseStickerSidePx * aim.scale
        val placement =
            PhotoStickerPlacement(
                stickerId = aim.stickerId,
                source = aim.source,
                removedBgSource = aim.removedBgSource,
                isBackgroundRemoved = aim.isBackgroundRemoved && aim.removedBgSource != null,
                offset = Offset(aim.center.x - side / 2f, aim.center.y - side / 2f),
                scale = aim.scale,
                rotationDegrees = aim.rotationDegrees
            )
        phase = current.copy(placed = true)
        return placement
    }

    /**
     * 화면이 사라질 때. 어떤 단계든 상태를 비우고, 아직 스티커가 되지 못한 파일이
     * 있으면 돌려준다(이미 붙은 스티커의 파일은 돌려주지 않는다).
     */
    fun abandon(): List<S> {
        val unused =
            when (val current = phase) {
                is PhotoStickerPlacePhase.Aiming -> current.leftovers()
                is PhotoStickerPlacePhase.Placing ->
                    if (current.placed) emptyList() else current.aim.leftovers()
                PhotoStickerPlacePhase.Idle -> emptyList()
            }
        phase = PhotoStickerPlacePhase.Idle
        return unused
    }

    /** 손이 다 빠졌을 때. 붙인 스티커 id(선택 표시용)를 돌려주고 상태를 비운다. */
    fun finish(): String? {
        val current = phase as? PhotoStickerPlacePhase.Placing<S>
        phase = PhotoStickerPlacePhase.Idle
        return current?.takeIf { it.placed }?.aim?.stickerId
    }

    private fun PhotoStickerPlacePhase.Aiming<S>.leftovers(): List<S> =
        listOfNotNull(source, removedBgSource)
}

/**
 * 조준 중심을 붙인 스티커와 같은 규칙([clampStickerOffset]: 회전 전 사각형이
 * 엽서 안)으로 가둔다. 그래서 조준한 자리와 실제로 놓이는 자리가 어긋나지 않는다.
 */
internal fun clampPhotoStickerAimCenter(
    center: Offset,
    stickerSidePx: Float,
    postcardSize: IntSize
): Offset {
    val side = stickerSidePx.roundToInt()
    return clampStickerAimCenter(center, IntSize(side, side), postcardSize)
}

/** [clampPhotoStickerAimCenter]의 직사각형판(라벨 스티커처럼 가로·세로가 다른 스티커). */
internal fun clampStickerAimCenter(
    center: Offset,
    stickerSize: IntSize,
    postcardSize: IntSize
): Offset {
    val halfWidth = stickerSize.width / 2f
    val halfHeight = stickerSize.height / 2f
    val topLeft =
        clampStickerOffset(
            offset = Offset(center.x - halfWidth, center.y - halfHeight),
            postcardSize = postcardSize,
            stickerSize = stickerSize
        )
    return Offset(topLeft.x + halfWidth, topLeft.y + halfHeight)
}

/**
 * 통째로 회전하는 직사각형 스티커(라벨)에서 핀셋 끝이 집는 자리(중심 기준 px, 화면 좌표).
 * 네 변의 가운데 중 회전 뒤 화면에서 가장 아래에 있는 변을, 중심 쪽으로 [gripInsetPx]만큼
 * 들어가 문다 — 기울기가 조금 바뀌어도 집는 자리가 모서리 사이를 튀지 않는다.
 */
internal fun rectStickerGripOffset(
    widthPx: Float,
    heightPx: Float,
    rotationDegrees: Float,
    gripInsetPx: Float
): Offset {
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    val halfWidth = widthPx / 2f
    val halfHeight = heightPx / 2f
    val (edgeMid, halfDepth) =
        listOf(
            Offset(0f, halfHeight) to halfHeight,
            Offset(0f, -halfHeight) to halfHeight,
            Offset(halfWidth, 0f) to halfWidth,
            Offset(-halfWidth, 0f) to halfWidth
        )
            .map { (p, depth) -> Offset(p.x * c - p.y * s, p.x * s + p.y * c) to depth }
            .maxBy { (p, _) -> p.y }
    if (halfDepth <= 0f) return edgeMid
    val keep = max(0f, halfDepth - gripInsetPx) / halfDepth
    return Offset(edgeMid.x * keep, edgeMid.y * keep)
}

/**
 * 기본 모양 스티커에서 핀셋 끝이 집는 거리: 스티커 중심에서 화면 아래쪽으로.
 * 기본 모양은 둥근 사각 틀이 회전하지 않으므로(틀 clip이 회전 바깥에 걸림)
 * 회전과 무관하게 아래 변의 가운데를 집는다.
 */
internal fun photoStickerGripDistance(stickerSidePx: Float, gripInsetPx: Float): Float =
    max(0f, stickerSidePx / 2f - gripInsetPx)

/**
 * 배경제거 그림(width×height)의 불투명 표본점. 붙인 누끼 스티커처럼 정사각형 칸에
 * ContentScale.Fit으로 가운데 놓였을 때의 자리를, 칸 한 변 기준(중심 0, 범위 ±0.5)으로 준다.
 * [grid]×[grid] 격자 칸 가운데만 본다.
 */
internal fun photoStickerCutoutSilhouette(
    width: Int,
    height: Int,
    grid: Int = PHOTO_STICKER_CUTOUT_GRID,
    isOpaque: (x: Int, y: Int) -> Boolean
): List<Offset> {
    if (width <= 0 || height <= 0 || grid <= 0) return emptyList()
    val longSide = max(width, height).toFloat()
    val points = ArrayList<Offset>()
    for (gy in 0 until grid) {
        for (gx in 0 until grid) {
            val px = ((gx + 0.5f) / grid * width).toInt().coerceIn(0, width - 1)
            val py = ((gy + 0.5f) / grid * height).toInt().coerceIn(0, height - 1)
            if (isOpaque(px, py)) {
                points +=
                    Offset(
                        (px + 0.5f - width / 2f) / longSide,
                        (py + 0.5f - height / 2f) / longSide
                    )
            }
        }
    }
    return points
}

/**
 * 핀셋 끝이 스티커를 집는 자리(스티커 중심 기준 px, 화면 좌표).
 * 누끼면 회전까지 반영한 그림의 가장 아래 불투명 점에서 [gripInsetPx]만큼 위,
 * 표본점이 없거나 기본 모양이면 사각형 아래 변의 가운데.
 */
internal fun photoStickerGripOffset(
    cutoutSilhouette: List<Offset>?,
    stickerSidePx: Float,
    rotationDegrees: Float,
    gripInsetPx: Float
): Offset {
    if (cutoutSilhouette.isNullOrEmpty()) {
        return Offset(0f, photoStickerGripDistance(stickerSidePx, gripInsetPx))
    }
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val c = cos(radians).toFloat()
    val s = sin(radians).toFloat()
    // graphicsLayer rotationZ와 같은 방향(y 아래, 양수 = 시계방향).
    val lowest =
        cutoutSilhouette
            .map { p ->
                Offset(
                    (p.x * c - p.y * s) * stickerSidePx,
                    (p.x * s + p.y * c) * stickerSidePx
                )
            }
            .maxBy { it.y }
    return Offset(lowest.x, lowest.y - gripInsetPx)
}

/**
 * 조준 중 엽서 위: 붙일 스티커의 반투명 미리보기 + 작은 +.
 * 탭하면 그 자리로, 한 손가락으로 끌면 따라오고, 두 손가락으로 돌리거나 크기를 바꾼다.
 * [interactive]가 false면(손이 오는 동안) 목표 자리 표시만 하고 입력은 받지 않는다.
 */
@Composable
internal fun PhotoStickerAimLayer(
    aim: PhotoStickerPlacePhase.Aiming<Uri>,
    stickerBaseSize: Dp,
    interactive: Boolean,
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
        val stickerSide = stickerBaseSize * aim.scale
        Box(
            modifier = Modifier
                .offset {
                    val halfPx = stickerSide.toPx() / 2f
                    IntOffset(
                        (aim.center.x - halfPx).roundToInt(),
                        (aim.center.y - halfPx).roundToInt()
                    )
                }
                .size(stickerSide)
                .graphicsLayer { alpha = PHOTO_STICKER_PREVIEW_ALPHA }
        ) {
            PhotoStickerPlacingImage(
                uri = aim.displayedSource,
                isBackgroundRemoved = aim.isBackgroundRemoved,
                rotationDegrees = aim.rotationDegrees
            )
        }

        if (interactive) {
            StickerPlaceCrosshair(center = aim.center)
        }
    }
}

/** 조준 십자. 도장 조준과 같은 모양: 종이색 바탕선 위에 흑연색 선. */
@Composable
internal fun StickerPlaceCrosshair(center: Offset) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val arm = PHOTO_STICKER_CROSSHAIR_ARM.toPx()
        listOf(
            Color(0xCCFFFDF7) to PHOTO_STICKER_CROSSHAIR_UNDERLAY_WIDTH.toPx(),
            GraphiteAccent to PHOTO_STICKER_CROSSHAIR_WIDTH.toPx()
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

/**
 * 새 스티커를 엽서에 붙은 것과 같은 modifier 순서로 그린다(DetailScreen의 스티커 그리기):
 * 기본 모양 = fillMaxSize → clip(16dp) → 회전, Crop / 누끼 = fillMaxSize → 회전, Fit.
 */
@Composable
private fun PhotoStickerPlacingImage(
    uri: Uri,
    isBackgroundRemoved: Boolean,
    rotationDegrees: Float
) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = if (isBackgroundRemoved) ContentScale.Fit else ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isBackgroundRemoved) {
                    Modifier
                } else {
                    Modifier.clip(RoundedCornerShape(16.dp))
                }
            )
            .graphicsLayer { rotationZ = rotationDegrees }
    )
}

/** 사진 스티커용 핀셋 손: 집는 자리를 정하고 들고 갈 그림을 [StickerTweezerHandOverlay]에 넘긴다. */
@Composable
internal fun PhotoStickerTweezerHandOverlay(
    stickerUri: Uri,
    isBackgroundRemoved: Boolean,
    cutoutSilhouette: List<Offset>?,
    stickerCenter: Offset,
    stickerSide: Dp,
    stickerRotationDegrees: Float,
    onContact: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val stickerSidePx = with(density) { stickerSide.toPx() }
    val gripOffset =
        remember(cutoutSilhouette, stickerSidePx, stickerRotationDegrees, density) {
            photoStickerGripOffset(
                cutoutSilhouette = cutoutSilhouette,
                stickerSidePx = stickerSidePx,
                rotationDegrees = stickerRotationDegrees,
                gripInsetPx = with(density) { STICKER_TWEEZER_GRIP_INSET.toPx() }
            )
        }
    StickerTweezerHandOverlay(
        stickerCenter = stickerCenter,
        stickerWidth = stickerSide,
        stickerHeight = stickerSide,
        gripOffset = gripOffset,
        onContact = onContact,
        onFinished = onFinished,
        modifier = modifier
    ) {
        PhotoStickerPlacingImage(
            uri = stickerUri,
            isBackgroundRemoved = isBackgroundRemoved,
            rotationDegrees = stickerRotationDegrees
        )
    }
}

/**
 * 신문지에서 오린 손이 핀셋으로 스티커를 집어 화면 아래에서 들고 올라와
 * [stickerCenter]에 톡 놓고 빠지는 일시 overlay. 좌표는 이 overlay 좌상단 기준 px.
 * 사진·라벨 스티커가 함께 쓴다 — 들고 가는 그림([carriedContent], 스티커 크기 칸 안에
 * 붙은 스티커와 같은 순서로 그림)과 집는 자리([gripOffset], 중심 기준)만 다르다.
 *
 * 손과 스티커는 한 덩어리로 같은 변환(이동·기울기·원근 크기)을 받는다. 변환 기준점은
 * 핀셋 끝([PHOTO_STICKER_TWEEZER_ANCHOR_X]/[PHOTO_STICKER_TWEEZER_ANCHOR_Y])이
 * 스티커를 집는 점이라, 진행값 0에서 스티커는 조준한 자리·크기·각도와 정확히
 * 같다. 연출 중에는 아래 화면 입력을 막는다.
 *
 * 닿는 프레임에 [onContact] 한 번, 다 빠진 뒤 [onFinished] 한 번 부른다.
 */
@Composable
internal fun StickerTweezerHandOverlay(
    stickerCenter: Offset,
    stickerWidth: Dp,
    stickerHeight: Dp,
    gripOffset: Offset,
    onContact: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    carriedContent: @Composable () -> Unit
) {
    val hand: ImageBitmap = ImageBitmap.imageResource(R.drawable.sticker_tweezer_hand)
    val currentOnContact by rememberUpdatedState(onContact)
    val currentOnFinished by rememberUpdatedState(onFinished)

    // 1 = 화면 아래(카메라 가까이), 0 = 종이에 닿음.
    val travel = remember { Animatable(1f) }
    // 0 = 닿기만 함, 1 = 살짝 눌러 붙임(손만 — 스티커 크기는 그대로).
    val press = remember { Animatable(0f) }
    // 닿은 뒤 실제 스티커가 확실히 그려질 때까지 들고 온 스티커를 같은 자리에 겹쳐 둔다.
    var carrying by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        // 쑤욱: 빠르게 올라와 종이 가까이에서 길게 감속한다(넘침·반동 없음).
        travel.animateTo(
            targetValue = 0f,
            animationSpec = tween(PHOTO_STICKER_PLACE_ENTER_MS, easing = PhotoStickerPlaceEnterEasing)
        )
        currentOnContact()
        // 톡: 반동 없이 아주 짧게 눌렀다가 놓는다.
        press.animateTo(1f, tween(PHOTO_STICKER_PLACE_PRESS_IN_MS, easing = LinearEasing))
        delay(PHOTO_STICKER_PLACE_HOLD_MS)
        carrying = false
        press.animateTo(0f, tween(PHOTO_STICKER_PLACE_PRESS_OUT_MS, easing = LinearEasing))
        travel.animateTo(
            targetValue = 1f,
            animationSpec = tween(PHOTO_STICKER_PLACE_EXIT_MS, easing = PhotoStickerPlaceExitEasing)
        )
        currentOnFinished()
    }

    val density = LocalDensity.current
    val handSidePx = with(density) { PHOTO_STICKER_TWEEZER_HAND_SIZE.toPx() }
    val stickerWidthPx = with(density) { stickerWidth.toPx() }
    val stickerHeightPx = with(density) { stickerHeight.toPx() }
    val grip = stickerCenter + gripOffset
    val anchorX = PHOTO_STICKER_TWEEZER_ANCHOR_X * handSidePx
    val anchorY = PHOTO_STICKER_TWEEZER_ANCHOR_Y * handSidePx
    val marginPx = with(density) { PHOTO_STICKER_HAND_OFFSCREEN_MARGIN.toPx() }

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
        // 손 + 들고 있는 스티커 한 덩어리. 핀셋 끝(grip)을 기준으로 기울고 커진 뒤 이동한다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val t = travel.value
                    val approachScale = 1f + PHOTO_STICKER_APPROACH_SCALE * t
                    // 출발점: 덩어리의 가장 위(스티커 윗부분 또는 핀셋 끝)까지 화면 아래로 숨는 거리.
                    // 스티커는 회전해도 중심에서 긴 변의 0.75배 안에 들어온다.
                    val extentAboveGrip =
                        max(gripOffset.y + max(stickerWidthPx, stickerHeightPx) * 0.75f, anchorY)
                    val hiddenDistance =
                        (size.height - grip.y) +
                                extentAboveGrip * (1f + PHOTO_STICKER_APPROACH_SCALE) +
                                marginPx
                    transformOrigin =
                        TransformOrigin(
                            pivotFractionX = if (size.width > 0f) grip.x / size.width else 0f,
                            pivotFractionY = if (size.height > 0f) grip.y / size.height else 0f
                        )
                    scaleX = approachScale
                    scaleY = approachScale
                    rotationZ = PHOTO_STICKER_HAND_TILT_DEGREES * t
                    translationX = PHOTO_STICKER_HAND_DRIFT_X * handSidePx * t
                    translationY = hiddenDistance * t
                }
        ) {
            if (carrying) {
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (stickerCenter.x - stickerWidthPx / 2f).roundToInt(),
                                (stickerCenter.y - stickerHeightPx / 2f).roundToInt()
                            )
                        }
                        .size(stickerWidth, stickerHeight)
                ) {
                    carriedContent()
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                // 누르는 동안 손만 아주 조금 작아진다(종이 쪽으로 파고듦). 기준은 핀셋 끝.
                val pressScale = 1f - PHOTO_STICKER_PRESS_SCALE * press.value
                val side = handSidePx * pressScale
                drawImage(
                    image = hand,
                    dstOffset = IntOffset(
                        (grip.x - anchorX * pressScale).roundToInt(),
                        (grip.y - anchorY * pressScale).roundToInt()
                    ),
                    dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                    filterQuality = FilterQuality.Medium
                )
            }
        }
    }
}

/** 스티커가 종이에 닿는 순간의 가벼운 "톡" 한 번. 도장("꾸욱")보다 짧고 약하다. */
internal fun vibratePhotoStickerContact(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(
        VibrationEffect.createOneShot(
            PHOTO_STICKER_HAPTIC_DURATION_MS,
            PHOTO_STICKER_HAPTIC_AMPLITUDE
        )
    )
}

// 손 이미지(sticker_tweezer_hand, 1254×1254)에서 벌어진 두 집게 끝(약 (345, 49)·(384, 31))
// 바로 아래, 두 집게 사이에 종이 가장자리가 끼워질 지점(372, 56)을 한 변 기준으로
// 정규화. 실기기 QA에서 보정한다.
internal const val PHOTO_STICKER_TWEEZER_ANCHOR_X = 0.297f
internal const val PHOTO_STICKER_TWEEZER_ANCHOR_Y = 0.045f

// 집게 끝이 스티커 가장자리에서 안쪽으로 물고 들어가는 정도.
internal val STICKER_TWEEZER_GRIP_INSET = 2.dp
private val PHOTO_STICKER_TWEEZER_HAND_SIZE = 300.dp
private val PHOTO_STICKER_HAND_OFFSCREEN_MARGIN = 24.dp
private const val PHOTO_STICKER_APPROACH_SCALE = 0.12f
private const val PHOTO_STICKER_PRESS_SCALE = 0.012f
private const val PHOTO_STICKER_HAND_DRIFT_X = 0.16f
private const val PHOTO_STICKER_HAND_TILT_DEGREES = 4f

// 도장(300/50/90/60/220ms)과 같은 리듬. 스티커를 들고 오는 만큼 진입만 조금 길고,
// 누름은 "꾸욱"이 아니라 "톡"이라 더 짧다.
private const val PHOTO_STICKER_PLACE_ENTER_MS = 320
private const val PHOTO_STICKER_PLACE_PRESS_IN_MS = 40
private const val PHOTO_STICKER_PLACE_HOLD_MS = 70L
private const val PHOTO_STICKER_PLACE_PRESS_OUT_MS = 50
private const val PHOTO_STICKER_PLACE_EXIT_MS = 220

private val PhotoStickerPlaceEnterEasing = CubicBezierEasing(0.2f, 0.75f, 0.25f, 1f)
private val PhotoStickerPlaceExitEasing = CubicBezierEasing(0.55f, 0f, 0.85f, 0.4f)

private const val PHOTO_STICKER_HAPTIC_DURATION_MS = 14L
private const val PHOTO_STICKER_HAPTIC_AMPLITUDE = 120

private const val PHOTO_STICKER_PREVIEW_ALPHA = 0.45f
internal const val PHOTO_STICKER_PLACE_MIN_SCALE = 0.5f
internal const val PHOTO_STICKER_PLACE_MAX_SCALE = 2.5f

// 누끼 표본 격자(한 변 칸 수)와, 불투명으로 볼 alpha 기준.
internal const val PHOTO_STICKER_CUTOUT_GRID = 48
internal const val PHOTO_STICKER_CUTOUT_ALPHA_THRESHOLD = 128

private val PHOTO_STICKER_CROSSHAIR_ARM = 7.dp
private val PHOTO_STICKER_CROSSHAIR_WIDTH = 1.5.dp
private val PHOTO_STICKER_CROSSHAIR_UNDERLAY_WIDTH = 3.5.dp
