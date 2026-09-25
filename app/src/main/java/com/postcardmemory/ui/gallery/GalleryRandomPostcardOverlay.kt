package com.postcardmemory.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.R
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.StampPhoto
import com.postcardmemory.ui.theme.PaperSurface
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.launch

/**
 * 84일차 후속: "흔들어서 한 장"이 뽑은 엽서를 갤러리 위에 꺼내 보여주는 overlay.
 *
 * 별도의 팝업 상자 없이 엽서 자체가 떠 있는 물체다. 레이어는 아래부터
 * dim → 날짜 문구 → 엽서 → 신문 오림 손. 손과 엽서는 같은 진행값 하나로
 * 함께 아래에서 올라와, 손이 엽서 아랫변을 집어 끌어올리는 것처럼 보인다.
 *
 * - 엽서 탭 → [onOpen] (기존 상세 화면 navigation은 호출자가 한다)
 * - dim(엽서 바깥) 탭·뒤로가기 → 짧게 내려가며 사라진 뒤 [onDismissed]
 * - 손은 pointer input이 없는 장식이라 탭을 가로채지 않는다.
 */
@Composable
internal fun GalleryRandomPostcardOverlay(
    postcard: Postcard,
    onOpen: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hand: ImageBitmap = ImageBitmap.imageResource(R.drawable.gallery_pick_hand)
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val scope = rememberCoroutineScope()

    // 0 = 손·엽서가 화면 아래로 숨음(dim 없음), 1 = 제자리.
    val rise = remember { Animatable(0f) }
    val labelAlpha = remember { Animatable(0f) }
    var dismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 쑤욱 — 통: 빠르게 올라와 제자리 근처에서 길게 감속, 넘침·반동 없음.
        rise.animateTo(
            targetValue = 1f,
            animationSpec = tween(RANDOM_OVERLAY_ENTER_MS, easing = RandomOverlayEnterEasing)
        )
        labelAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(RANDOM_OVERLAY_LABEL_FADE_MS, easing = LinearEasing)
        )
    }

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            launch { labelAlpha.snapTo(0f) }
            rise.animateTo(
                targetValue = 0f,
                animationSpec = tween(RANDOM_OVERLAY_EXIT_MS, easing = RandomOverlayExitEasing)
            )
            currentOnDismissed()
        }
    }

    BackHandler { dismiss() }

    val label = remember(postcard.capturedAt) {
        randomPostcardStoryLabel(postcard.capturedAt)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val layout = remember(constraints.maxWidth, constraints.maxHeight, density) {
            randomOverlayLayout(
                widthPx = constraints.maxWidth.toFloat(),
                heightPx = constraints.maxHeight.toFloat(),
                density = density.density
            )
        }
        val cardSizeDp = with(density) { layout.cardSizePx.toDp() }

        // dim: 갤러리가 무엇인지는 알아볼 수 있을 정도로만 어둡게.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = rise.value }
                .background(Color.Black.copy(alpha = RANDOM_OVERLAY_DIM_ALPHA))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::dismiss
                )
        )

        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = PaperSurface.copy(alpha = RANDOM_OVERLAY_LABEL_ALPHA),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(0, (layout.cardTopPx - layout.labelGapPx).roundToInt())
                }
                .graphicsLayer { alpha = labelAlpha.value }
        )

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        layout.cardLeftPx.roundToInt(),
                        (layout.cardTopPx + layout.hiddenTravelPx * (1f - rise.value))
                            .roundToInt()
                    )
                }
                .size(cardSizeDp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !dismissing,
                    onClick = onOpen
                )
        ) {
            StampPhoto(
                imagePath = postcard.imagePath,
                contentDescription = postcard.title,
                modifier = Modifier.fillMaxSize(),
                outlineColor = Color.White,
                outlineWidth = 3f
            )
        }

        // 손: 엽서와 같은 이동량으로 움직여 엽서 아랫변을 집고 있는 것처럼 보인다.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = layout.handSidePx
            val travel = layout.hiddenTravelPx * (1f - rise.value)
            val left = layout.handAnchorXPx - RANDOM_OVERLAY_HAND_ANCHOR_X * side
            val top = layout.cardBottomPx + travel - RANDOM_OVERLAY_HAND_ANCHOR_Y * side

            drawImage(
                image = hand,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                filterQuality = FilterQuality.Medium
            )
        }
    }
}

/** overlay 한 화면의 배치(px). 화면 크기에서만 계산되는 순수 값. */
internal data class RandomOverlayLayout(
    val cardSizePx: Float,
    val cardLeftPx: Float,
    val cardTopPx: Float,
    val handSidePx: Float,
    val handAnchorXPx: Float,
    val labelGapPx: Float,
    val hiddenTravelPx: Float
) {
    val cardBottomPx: Float get() = cardTopPx + cardSizePx
}

/**
 * 엽서는 가로 폭과 세로 높이 중 좁은 쪽 기준으로 크기를 정하고, 중앙보다
 * 살짝 위에 둔다(위 날짜 문구·아래 손 공간).
 *
 * 손 이미지는 손목이 그림 아래 가장자리에서 잘려 있어, 화면 아래까지 닿지
 * 않으면 잘린 팔이 공중에 뜬다 — 그래서 손 크기는 "집는 지점이 엽서 아랫변에
 * 오고 손목이 화면 아래 밖으로 나가는" 크기로 정하되, 엽서보다 과하게
 * 커지지 않게 상한을 둔다.
 */
internal fun randomOverlayLayout(
    widthPx: Float,
    heightPx: Float,
    density: Float
): RandomOverlayLayout {
    val cardSize = minOf(
        widthPx * RANDOM_OVERLAY_CARD_WIDTH_FRACTION,
        heightPx * RANDOM_OVERLAY_CARD_HEIGHT_FRACTION
    )
    val cardCenterY = heightPx * RANDOM_OVERLAY_CARD_CENTER_Y_FRACTION
    val cardTop = cardCenterY - cardSize / 2f
    val cardBottom = cardTop + cardSize

    val handToScreenBottom = heightPx - cardBottom + RANDOM_OVERLAY_HAND_BOTTOM_BLEED_DP * density
    val handSide = (handToScreenBottom / (1f - RANDOM_OVERLAY_HAND_ANCHOR_Y))
        .coerceIn(
            cardSize * RANDOM_OVERLAY_HAND_MIN_SCALE,
            cardSize * RANDOM_OVERLAY_HAND_MAX_SCALE
        )

    // 실기기 QA: 가운데 아래를 집으면 어색해서 왼쪽 아래 귀퉁이를 집는다.
    // 손 몸통은 이미지 안에서 집는 지점보다 왼쪽에 있어, 팔이 화면 왼쪽
    // 아래에서 비스듬히 들어오는 모양이 된다(일부가 화면 밖으로 나가도 된다).
    val cardLeft = (widthPx - cardSize) / 2f
    val handAnchorX = cardLeft + RANDOM_OVERLAY_HAND_CORNER_INSET * cardSize

    // 숨은 위치: 엽서 윗변까지 화면 아래로 완전히 내려간 거리.
    val hiddenTravel = heightPx - cardTop + RANDOM_OVERLAY_OFFSCREEN_MARGIN_DP * density

    return RandomOverlayLayout(
        cardSizePx = cardSize,
        cardLeftPx = cardLeft,
        cardTopPx = cardTop,
        handSidePx = handSide,
        handAnchorXPx = handAnchorX,
        labelGapPx = RANDOM_OVERLAY_LABEL_GAP_DP * density,
        hiddenTravelPx = hiddenTravel
    )
}

/** "9월 25일의 이야기예요~!" — 갤러리 날짜 표기와 같은 [Postcard.capturedAt] 기준. */
internal fun randomPostcardStoryLabel(
    capturedAt: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val date = Instant.ofEpochMilli(capturedAt).atZone(zoneId).toLocalDate()
    return "${date.monthValue}월 ${date.dayOfMonth}일의 이야기예요~!"
}

/**
 * 흔들기가 인정됐을 때 overlay에 띄울 엽서 id. overlay가 이미 열려 있으면
 * 그 엽서를 그대로 유지한다(흔들 때마다 교체·중첩 금지). 후보가 없으면 null.
 */
internal fun nextShakeOverlayPostcardId(
    currentOverlayPostcardId: Long?,
    candidateIds: List<Long>,
    random: Random = Random.Default
): Long? = currentOverlayPostcardId ?: pickRandomPostcardId(candidateIds, random)

// gallery_pick_hand.png(1254×1254)에서 검지 끝과 엄지 끝 사이, 엽서 아랫변이
// 끼워질 지점(840, 280)을 한 변 기준으로 정규화.
internal const val RANDOM_OVERLAY_HAND_ANCHOR_X = 0.670f
internal const val RANDOM_OVERLAY_HAND_ANCHOR_Y = 0.223f
// 집는 지점이 엽서 왼쪽 모서리에서 안쪽으로 들어간 정도(엽서 한 변 기준).
private const val RANDOM_OVERLAY_HAND_CORNER_INSET = 0.08f

private const val RANDOM_OVERLAY_CARD_WIDTH_FRACTION = 0.62f
private const val RANDOM_OVERLAY_CARD_HEIGHT_FRACTION = 0.40f
private const val RANDOM_OVERLAY_CARD_CENTER_Y_FRACTION = 0.46f
private const val RANDOM_OVERLAY_HAND_MIN_SCALE = 1.0f
private const val RANDOM_OVERLAY_HAND_MAX_SCALE = 1.6f
private const val RANDOM_OVERLAY_HAND_BOTTOM_BLEED_DP = 8f
private const val RANDOM_OVERLAY_OFFSCREEN_MARGIN_DP = 24f
private const val RANDOM_OVERLAY_LABEL_GAP_DP = 40f

private const val RANDOM_OVERLAY_DIM_ALPHA = 0.45f
private const val RANDOM_OVERLAY_LABEL_ALPHA = 0.88f

private const val RANDOM_OVERLAY_ENTER_MS = 480
private const val RANDOM_OVERLAY_LABEL_FADE_MS = 180
private const val RANDOM_OVERLAY_EXIT_MS = 220

private val RandomOverlayEnterEasing = CubicBezierEasing(0.2f, 0.75f, 0.25f, 1f)
private val RandomOverlayExitEasing = CubicBezierEasing(0.55f, 0f, 0.85f, 0.4f)
