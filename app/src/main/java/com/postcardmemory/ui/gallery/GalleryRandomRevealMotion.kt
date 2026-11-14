package com.postcardmemory.ui.gallery

import androidx.compose.runtime.Stable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.PI
import kotlin.math.sin

internal const val RANDOM_REVEAL_TOTAL_MS = 2200f
internal const val RANDOM_REVEAL_DETACH_MS = 520f
internal const val RANDOM_REVEAL_SETTLED_MS = 1900f

/** Re-read on resume so returning from Android's animation settings takes effect without restart. */
@Composable
internal fun rememberRandomRevealReducedMotion(): Boolean {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    fun read() = runCatching {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)
    var reduced by remember(context) { mutableStateOf(read()) }
    DisposableEffect(lifecycle, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reduced = read()
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    return reduced
}

/** Window pixels captured before any shake, never persisted or used to change gallery order. */
internal data class RandomRevealSource(val card: Rect, val viewport: Rect)

internal class GalleryRandomCardRegistry {
    private val cards = mutableMapOf<Long, Rect>()
    var viewport: Rect = Rect.Zero
    fun update(id: Long, bounds: Rect) { cards[id] = bounds }
    fun remove(id: Long) { cards.remove(id) }
    fun source(id: Long, isVisible: Boolean = true): RandomRevealSource? = cards[id]?.takeIf {
        isVisible && it.width > 0f && it.height > 0f && it.overlaps(viewport)
    }?.let { RandomRevealSource(it, viewport) }
}

internal data class RandomRevealSession(
    val generation: Long,
    val postcardId: Long,
    val source: RandomRevealSource?
)

/** One active result; generation checks reject late frames/cleanup from a cancelled result. */
@Stable
internal class GalleryRandomRevealState {
    var session by mutableStateOf<RandomRevealSession?>(null)
        private set
    var elapsedMs by mutableFloatStateOf(0f)
        private set
    private var generation = 0L

    fun start(postcardId: Long, source: RandomRevealSource?): Boolean {
        if (session != null) return false
        elapsedMs = 0f
        session = RandomRevealSession(++generation, postcardId, source)
        return true
    }

    fun advance(token: Long, elapsed: Float) {
        if (session?.generation == token && elapsed.isFinite()) {
            elapsedMs = elapsed.coerceIn(0f, RANDOM_REVEAL_TOTAL_MS)
        }
    }

    fun clear(token: Long? = session?.generation) {
        if (session?.generation != token) return
        session = null
        elapsedMs = 0f
    }

    fun updateSource(token: Long, source: RandomRevealSource?) {
        session?.takeIf { it.generation == token }?.let { session = it.copy(source = source) }
    }
}

internal data class RandomCardShake(
    val xDp: Float = 0f,
    val yDp: Float = 0f,
    val rotation: Float = 0f,
    val scale: Float = 1f
)

/** Same B envelope/amplitudes as the approved HTML; the stable ID replaces the sample index. */
internal fun randomCardShake(id: Long, elapsedMs: Float): RandomCardShake {
    if (!elapsedMs.isFinite() || elapsedMs <= 0f || elapsedMs >= 900f) return RandomCardShake()
    val seed = Math.floorMod(id xor (id ushr 32), 997L).toInt()
    val u = elapsedMs / 900f
    val envelope = sin(PI * u).toFloat().let { it * it }
    val phase = seed * 1.73
    val frequency = 2.2 + seed % 3 * .21
    return RandomCardShake(
        xDp = 10f * envelope * sin(u * PI * 2 * frequency + phase).toFloat(),
        yDp = 6f * envelope * sin(u * PI * 2 * (frequency - .3) + phase + 1).toFloat() -
            4f * envelope * if (seed % 2 == 1) .4f else 1f,
        rotation = 4f * envelope * sin(u * PI * 2 * frequency + phase + .4).toFloat(),
        scale = 1f + .022f * envelope * (seed % 3 + 1) / 3f
    )
}

internal data class RandomCardFlight(
    val centerX: Float,
    val centerY: Float,
    val side: Float,
    val rotation: Float,
    val progress: Float
)

internal fun randomRevealProgress(elapsed: Float, start: Float, end: Float): Float =
    ((elapsed - start) / (end - start)).coerceIn(0f, 1f)

internal fun randomRevealSmooth(value: Float): Float = value * value * (3f - 2f * value)

/** Source and target are in the same overlay-local pixel space; scaling pivots at photo center. */
internal fun randomCardFlight(
    id: Long,
    source: Rect?,
    layout: RandomOverlayLayout,
    widthPx: Float,
    heightPx: Float,
    density: Float,
    elapsedMs: Float
): RandomCardFlight {
    val q = randomRevealProgress(elapsedMs, RANDOM_REVEAL_DETACH_MS, RANDOM_REVEAL_SETTLED_MS)
    if (q >= 1f) return RandomCardFlight(
        layout.cardLeftPx + layout.cardSizePx / 2f,
        layout.cardTopPx + layout.cardSizePx / 2f,
        layout.cardSizePx, 0f, 1f
    )
    val p = randomRevealSmooth(q)
    val shake = if (source == null) RandomCardShake(rotation = -4f) else randomCardShake(id, RANDOM_REVEAL_DETACH_MS)
    val initialSide = source?.width ?: ((widthPx - 40f * density) / 3f).coerceAtLeast(density)
    val startX = (source?.center?.x ?: (widthPx / 2f)) + shake.xDp * density
    val startY = (source?.center?.y ?: (heightPx + 20f * density + initialSide / 2f)) + shake.yDp * density
    fun mix(a: Float, b: Float) = a + (b - a) * p
    return RandomCardFlight(
        centerX = mix(startX, layout.cardLeftPx + layout.cardSizePx / 2f),
        centerY = mix(startY, layout.cardTopPx + layout.cardSizePx / 2f) - sin(PI * q).toFloat() * 12f * density,
        side = mix(initialSide * shake.scale, layout.cardSizePx),
        rotation = mix(shake.rotation, 0f) - sin(PI * q).toFloat() * 4f,
        progress = p
    )
}

internal fun Modifier.randomPostcardShake(
    id: Long,
    state: GalleryRandomRevealState?,
    reducedMotion: Boolean
): Modifier = graphicsLayer {
    val session = state?.session
    val elapsed = state?.elapsedMs ?: 0f
    val shake = if (session == null || reducedMotion) RandomCardShake() else randomCardShake(id, elapsed)
    translationX = shake.xDp * density
    translationY = shake.yDp * density
    rotationZ = shake.rotation
    scaleX = shake.scale
    scaleY = shake.scale
    // The date line belongs to the cell, but the pivot belongs to its square photo.
    transformOrigin = TransformOrigin(.5f, if (size.height > 0f) size.width / (2f * size.height) else .5f)
    alpha = if (!reducedMotion && session?.postcardId == id && session.source != null &&
        elapsed >= RANDOM_REVEAL_DETACH_MS) 0f else 1f
}
