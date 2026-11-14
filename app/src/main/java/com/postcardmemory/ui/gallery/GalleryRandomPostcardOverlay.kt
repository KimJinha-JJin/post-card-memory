package com.postcardmemory.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.components.PinkingPhotoShape
import com.postcardmemory.ui.components.StampPhoto
import com.postcardmemory.ui.theme.PaperSurface
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.launch

/** Approved day-132 B: one clock drives the grid, its detached photo, dim and date. */
@Composable
internal fun GalleryRandomPostcardOverlay(
    postcard: Postcard,
    state: GalleryRandomRevealState,
    reducedMotion: Boolean,
    prepareSource: suspend () -> RandomRevealSource?,
    onOpen: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val session = state.session ?: return
    val settled by remember(state, session.generation) {
        derivedStateOf { state.elapsedMs >= RANDOM_REVEAL_SETTLED_MS }
    }
    val currentOnDismissed by rememberUpdatedState(onDismissed)
    val scope = rememberCoroutineScope()
    val exit = remember(session.generation) { Animatable(1f) }
    var dismissing by remember(session.generation) { mutableStateOf(false) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var labelHeight by remember { mutableIntStateOf(0) }

    LaunchedEffect(session.generation, reducedMotion, dismissing) {
        if (!dismissing) {
            if (state.elapsedMs == 0f) state.updateSource(session.generation, prepareSource())
            if (reducedMotion) state.advance(session.generation, RANDOM_REVEAL_TOTAL_MS)
            else animate(
                initialValue = state.elapsedMs,
                targetValue = RANDOM_REVEAL_TOTAL_MS,
                animationSpec = tween(
                    (RANDOM_REVEAL_TOTAL_MS - state.elapsedMs).roundToInt(),
                    easing = LinearEasing
                )
            ) { value, _ -> state.advance(session.generation, value) }
        }
    }

    fun dismiss() {
        if (dismissing) return
        dismissing = true
        if (reducedMotion || state.elapsedMs < RANDOM_REVEAL_SETTLED_MS) {
            currentOnDismissed()
        } else scope.launch {
            exit.animateTo(0f, tween(220))
            currentOnDismissed()
        }
    }
    BackHandler { dismiss() }
    val label = remember(postcard.capturedAt) { randomPostcardStoryLabel(postcard.capturedAt) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .semantics { paneTitle = "랜덤 엽서" }
    ) {
        val density = LocalDensity.current
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val layout = randomOverlayLayout(width, height, density.density, labelHeight.toFloat())
        val source = if (reducedMotion) null else session.source?.card?.translate(-origin)
        val viewport = session.source?.viewport?.translate(-origin)
        // Measure/load at the final size, then scale down for the handoff. A thumbnail-sized
        // AsyncImage enlarged only by graphicsLayer would remain unnecessarily soft at the result.
        val baseSide = layout.cardSizePx.coerceAtLeast(density.density)
        fun frame() = randomCardFlight(
            postcard.id, source, layout, width, height, density.density,
            if (reducedMotion) RANDOM_REVEAL_TOTAL_MS else state.elapsedMs
        )

        // Transparent from the first frame: all input belongs to this result, never the grid beneath it.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    alpha = .45f * randomRevealProgress(state.elapsedMs, RANDOM_REVEAL_DETACH_MS, 1120f) * exit.value
                }
                .background(Color.Black)
                .semantics { contentDescription = "랜덤 엽서 닫기" }
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = ::dismiss)
        )

        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = PaperSurface.copy(alpha = .88f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .onSizeChanged { labelHeight = it.height }
                .then(if (!settled) Modifier.clearAndSetSemantics { } else Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                })
                .offset { IntOffset(0, (layout.cardTopPx - labelHeight - layout.labelGapPx).roundToInt()) }
                .graphicsLayer {
                    alpha = randomRevealProgress(state.elapsedMs, RANDOM_REVEAL_SETTLED_MS, RANDOM_REVEAL_TOTAL_MS) * exit.value
                }
        )

        // A partly clipped source is revealed gradually as it leaves the grid viewport.
        Box(Modifier.fillMaxSize().drawWithContent {
            val reveal = randomRevealProgress(state.elapsedMs, RANDOM_REVEAL_DETACH_MS, 700f)
            if (viewport != null && reveal < 1f && !reducedMotion) {
                val top = viewport.top.coerceAtLeast(0f) * (1f - reveal)
                val bottom = viewport.bottom.coerceAtMost(size.height) +
                    (size.height - viewport.bottom.coerceAtMost(size.height)) * reveal
                clipRect(top = top, bottom = bottom) { this@drawWithContent.drawContent() }
            } else drawContent()
        }) {
            Box(
                Modifier.size(with(density) { baseSide.toDp() })
                    .graphicsLayer {
                        val pose = frame()
                        translationX = pose.centerX - baseSide / 2f
                        translationY = pose.centerY - baseSide / 2f
                        rotationZ = pose.rotation
                        scaleX = pose.side / baseSide
                        scaleY = pose.side / baseSide
                        alpha = if (reducedMotion || state.elapsedMs >= RANDOM_REVEAL_DETACH_MS) exit.value else 0f
                        shadowElevation = 10f * pose.progress * density.density
                        shape = PinkingPhotoShape
                        clip = false
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = settled && !dismissing,
                        onClick = {
                            if (state.elapsedMs >= RANDOM_REVEAL_SETTLED_MS && !dismissing) onOpen()
                        }
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
        }
    }
}

internal data class RandomOverlayLayout(
    val cardSizePx: Float,
    val cardLeftPx: Float,
    val cardTopPx: Float,
    val labelGapPx: Float,
    val hiddenTravelPx: Float
) {
    val cardBottomPx: Float get() = cardTopPx + cardSizePx
}

/** Keep the approved .62 width/.40 height/.46 center, reserving measured space for large text. */
internal fun randomOverlayLayout(
    widthPx: Float,
    heightPx: Float,
    density: Float,
    labelHeightPx: Float = 18f * density
): RandomOverlayLayout {
    val labelGap = 14f * density
    val labelRoom = labelHeightPx + labelGap + 8f * density
    val side = minOf(widthPx * .62f, heightPx * .40f, (heightPx - labelRoom - 16f * density).coerceAtLeast(0f))
    val top = (heightPx * .46f - side / 2f)
        .coerceIn(labelRoom.coerceAtMost(heightPx - side), (heightPx - side).coerceAtLeast(0f))
    return RandomOverlayLayout(side, (widthPx - side) / 2f, top, labelGap, heightPx - top + 24f * density)
}

internal fun randomPostcardStoryLabel(capturedAt: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
    val date = Instant.ofEpochMilli(capturedAt).atZone(zoneId).toLocalDate()
    return "${date.monthValue}월 ${date.dayOfMonth}일의 이야기예요~!"
}

internal fun nextShakeOverlayPostcardId(
    currentOverlayPostcardId: Long?, candidateIds: List<Long>, random: Random = Random.Default
): Long? = currentOverlayPostcardId ?: pickRandomPostcardId(candidateIds, random)
