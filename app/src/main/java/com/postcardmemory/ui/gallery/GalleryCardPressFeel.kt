package com.postcardmemory.ui.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 130일차 B(사용자 선택 B안): 월별 그리드 엽서를 누르면 종이처럼 살짝 눌리고 누른 쪽이 낮아지며,
 * 탭하면 집어 드는 짧은 움직임 뒤에 상세로 간다. 목업: docs/mockups/paper-touch-feel-day130.html.
 *
 * 그리기(graphicsLayer)만 바꾼다 — 선택·길게 누르기·연타 처리·상세 화면/Page Curl·연못 모드는
 * 그대로다. 움직임 값은 draw 단계에서만 읽어 매 프레임 recomposition하지 않는다.
 * 시스템 "애니메이션 끄기"면 크기·이동·기울기·집어 듦을 끄고 눌린 동안 살짝 흐려지는 표시만 남긴다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun galleryCardPressFeel(
    liftOnOpen: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier {
    val reduceMotion = rememberSystemReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var pressPosition by remember { mutableStateOf(Offset.Unspecified) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Press) pressPosition = interaction.pressPosition
        }
    }
    val press = animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (pressed) GALLERY_CARD_PRESS_IN_MS else GALLERY_CARD_PRESS_OUT_MS,
            easing = PaperEaseOutCubic
        ),
        label = "galleryCardPress"
    )
    val pick = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var opening by remember { mutableStateOf(false) }
    val latestOnClick by rememberUpdatedState(onClick)
    val latestLiftOnOpen by rememberUpdatedState(liftOnOpen)

    return Modifier
        .graphicsLayer {
            val (nx, ny) = galleryCardPressDirection(
                pressX = pressPosition.x,
                pressY = pressPosition.y,
                width = size.width,
                height = size.height
            )
            val visual = galleryCardPressVisual(press = press.value, pick = pick.value, nx = nx, ny = ny)
            scaleX = visual.scale
            scaleY = visual.scale
            translationY = visual.translationYDp.dp.toPx()
            rotationX = visual.rotationXDeg
            rotationY = visual.rotationYDeg
            shadowElevation = visual.shadowElevationDp.dp.toPx()
            shape = GalleryCardPhotoShadowShape
            spotShadowColor = GalleryCardPickSpotShadow
            ambientShadowColor = GalleryCardPickAmbientShadow
            clip = false
            alpha = if (reduceMotion && pressed) GALLERY_CARD_REDUCED_PRESS_ALPHA else 1f
        }
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                if (!latestLiftOnOpen || reduceMotion) {
                    latestOnClick()
                } else if (!opening) {
                    opening = true
                    scope.launch {
                        try {
                            launch { pick.animateTo(1f, tween(GALLERY_CARD_PICK_MS, easing = PaperEaseOutCubic)) }
                            delay(GALLERY_CARD_OPEN_DELAY_MS)
                            latestOnClick()
                            pick.animateTo(0f, tween(GALLERY_CARD_PICK_MS, easing = PaperEaseOutCubic))
                        } finally {
                            opening = false
                        }
                    }
                }
            },
            onLongClick = onLongClick
        )
}

/** 누른 위치를 카드 중심 기준 -1..1로. 위치를 모르면(키보드·접근성 클릭) 기울이지 않는다. */
internal fun galleryCardPressDirection(
    pressX: Float,
    pressY: Float,
    width: Float,
    height: Float
): Pair<Float, Float> {
    if (!pressX.isFinite() || !pressY.isFinite() || width <= 0f || height <= 0f) return 0f to 0f
    val nx = (pressX / width * 2f - 1f).coerceIn(-1f, 1f)
    val ny = (pressY / height * 2f - 1f).coerceIn(-1f, 1f)
    return nx to ny
}

internal data class GalleryCardPressVisual(
    val scale: Float,
    val translationYDp: Float,
    val rotationXDeg: Float,
    val rotationYDeg: Float,
    val shadowElevationDp: Float
)

/**
 * [press] 0..1 눌림, [pick] 0..1 집어 듦. 누르면 0.97배·2dp 내려가며 누른 쪽이 최대 2.5° 낮아지고,
 * 집어 들면 1.03배·4dp 떠오르며 사진 아래 그림자가 생긴다. 쉬는 상태(0, 0)는 기존과 똑같다.
 */
internal fun galleryCardPressVisual(press: Float, pick: Float, nx: Float, ny: Float): GalleryCardPressVisual {
    val p = press.coerceIn(0f, 1f)
    val k = pick.coerceIn(0f, 1f)
    return GalleryCardPressVisual(
        scale = (1f - GALLERY_CARD_PRESS_SCALE * p) * (1f + GALLERY_CARD_PICK_SCALE * k),
        translationYDp = GALLERY_CARD_PRESS_SINK_DP * p - GALLERY_CARD_PICK_LIFT_DP * k,
        // 위를 누르면 윗변이, 오른쪽을 누르면 오른쪽 변이 화면 안쪽으로 내려간다.
        rotationXDeg = -ny * GALLERY_CARD_PRESS_TILT_DEG * p + 0f,
        rotationYDeg = nx * GALLERY_CARD_PRESS_TILT_DEG * p + 0f,
        shadowElevationDp = GALLERY_CARD_PICK_ELEVATION_DP * k
    )
}

/** 그림자는 날짜 줄이 아니라 위쪽 정사각형 사진에서만 떨어진다(볼록한 사각형이라 모든 API에서 그려짐). */
private object GalleryCardPhotoShadowShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, 0f, size.width, minOf(size.width, size.height)))
}

internal val PaperEaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

internal const val GALLERY_CARD_PRESS_IN_MS = 110
internal const val GALLERY_CARD_PRESS_OUT_MS = 160
internal const val GALLERY_CARD_PICK_MS = 160
internal const val GALLERY_CARD_OPEN_DELAY_MS = 120L
internal const val GALLERY_CARD_PRESS_SCALE = 0.03f
internal const val GALLERY_CARD_PRESS_SINK_DP = 2f
internal const val GALLERY_CARD_PRESS_TILT_DEG = 2.5f
internal const val GALLERY_CARD_PICK_SCALE = 0.03f
internal const val GALLERY_CARD_PICK_LIFT_DP = 4f
internal const val GALLERY_CARD_PICK_ELEVATION_DP = 6f
private const val GALLERY_CARD_REDUCED_PRESS_ALPHA = 0.85f

// 108일차 연못 집기 그림자 QA("너무 진해")에서 맞춘 옅은 색을 그대로 쓴다.
private val GalleryCardPickSpotShadow = Color.Black.copy(alpha = 0.4f)
private val GalleryCardPickAmbientShadow = Color.Black.copy(alpha = 0.2f)
