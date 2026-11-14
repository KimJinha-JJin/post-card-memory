package com.postcardmemory.ui.gallery

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

/** Executes production math/session/registry; does not run Compose rendering or physical sensors. */
class GalleryRandomRevealMotionTest {
    private val source = Rect(140f, 420f, 240f, 520f)
    private val viewport = Rect(0f, 200f, 384f, 760f)
    private fun pose(id: Long, t: Float, bounds: Rect? = source, density: Float = 1f) =
        randomCardFlight(id, bounds, randomOverlayLayout(384f, 760f, density), 384f, 760f, density, t)

    @Test fun shakeReturnsExactlyToIdentityAtBothEnds() {
        for (id in listOf(1L, 42L, Long.MIN_VALUE, Long.MAX_VALUE)) {
            for (t in listOf(-1f, 0f, 900f, 2200f, Float.NaN)) {
                assertEquals(RandomCardShake(), randomCardShake(id, t))
            }
        }
    }

    @Test fun bShakeStaysInsideApprovedBounds() {
        for (id in 1L..20L) for (t in 0..900 step 7) {
            val s = randomCardShake(id, t.toFloat())
            assertTrue(kotlin.math.abs(s.xDp) <= 10.001f)
            assertTrue(s.yDp in -10.001f..6.001f)
            assertTrue(kotlin.math.abs(s.rotation) <= 4.001f)
            assertTrue(s.scale in 1f..1.02201f)
        }
    }

    @Test fun idDeterminesStableDifferentPhases() {
        val samples = (1L..8L).map { randomCardShake(it, 350f) }
        assertEquals(samples, (1L..8L).map { randomCardShake(it, 350f) })
        assertTrue(samples.map { it.xDp }.distinct().size > 4)
        assertTrue(samples.any { it.xDp > 0f } && samples.any { it.xDp < 0f })
    }

    @Test fun visiblePhotoHandoffPreservesCenterRotationAndScale() {
        for (id in listOf(1L, 42L, 99L)) {
            val shake = randomCardShake(id, RANDOM_REVEAL_DETACH_MS)
            val flight = pose(id, RANDOM_REVEAL_DETACH_MS)
            assertEquals(source.center.x + shake.xDp, flight.centerX, .001f)
            assertEquals(source.center.y + shake.yDp, flight.centerY, .001f)
            assertEquals(source.width * shake.scale, flight.side, .001f)
            assertEquals(shake.rotation, flight.rotation, .001f)
        }
    }

    @Test fun flightIsContinuousAroundDetachmentAndSettlement() {
        for (boundary in listOf(520f, 1900f)) {
            val a = pose(42, boundary - .001f)
            val b = pose(42, boundary + .001f)
            assertEquals(a.centerX, b.centerX, .01f)
            assertEquals(a.centerY, b.centerY, .01f)
            assertEquals(a.side, b.side, .01f)
            assertEquals(a.rotation, b.rotation, .01f)
        }
    }

    @Test fun offscreenResultStartsBelowViewportAndEndsLikeVisibleResult() {
        val initial = pose(42, 520f, null)
        assertTrue(initial.centerY - initial.side / 2f >= 760f)
        assertEquals(pose(42, 2200f), pose(42, 2200f, null))
    }

    @Test fun allIdsAndSourcesSettleToIdenticalFrontFacingLayout() {
        val expected = pose(1, 2200f)
        for (id in 1L..20L) {
            assertEquals(expected, pose(id, 1900f))
            assertEquals(expected, pose(id, 2200f, null))
        }
        assertEquals(0f, expected.rotation, .001f)
    }

    @Test fun densityScalesTranslationsWithoutChangingRotation() {
        val density = 2.8125f
        val small = pose(42, 520f)
        val big = randomCardFlight(42, Rect(140f*density, 420f*density, 240f*density, 520f*density),
            randomOverlayLayout(384f*density, 760f*density, density),
            384f*density, 760f*density, density, 520f)
        assertEquals(small.centerX*density, big.centerX, .001f)
        assertEquals(small.centerY*density, big.centerY, .001f)
        assertEquals(small.side*density, big.side, .001f)
        assertEquals(small.rotation, big.rotation, .001f)
    }

    @Test fun narrowShortAndLargeTextLayoutsKeepPhotoAndLabelInside() {
        for (w in listOf(320f, 360f, 384f, 412f)) for (h in listOf(380f, 460f, 640f, 760f)) {
            val layout = randomOverlayLayout(w, h, 1f, labelHeightPx = 90f)
            assertTrue(layout.cardLeftPx >= 0f)
            assertTrue(layout.cardLeftPx + layout.cardSizePx <= w)
            assertTrue(layout.cardTopPx - 90f - layout.labelGapPx >= 0f)
            assertTrue(layout.cardBottomPx <= h)
        }
    }

    @Test fun registryOnlyReturnsRealVisibleId() {
        val registry = GalleryRandomCardRegistry()
        registry.viewport = viewport
        registry.update(1, source)
        registry.update(2, Rect(0f, 900f, 100f, 1000f))
        assertEquals(source, registry.source(1)?.card)
        assertNull(registry.source(1, isVisible = false)) // retained/precomposed LazyGrid item
        assertNull(registry.source(2))
        assertNull(registry.source(999))
    }

    @Test fun partiallyVisiblePhotoRetainsItsActualBounds() {
        val registry = GalleryRandomCardRegistry()
        registry.viewport = viewport
        val partial = Rect(10f, 150f, 110f, 250f)
        registry.update(5, partial)
        assertEquals(RandomRevealSource(partial, viewport), registry.source(5))
    }

    @Test fun disposedCardCannotBeUsedAsAVisibleOrigin() {
        val registry = GalleryRandomCardRegistry()
        registry.viewport = viewport
        registry.update(1, source)
        registry.remove(1)
        assertNull(registry.source(1))
    }

    @Test fun duplicateStartCannotReplaceResultOrResetItsClock() {
        val state = GalleryRandomRevealState()
        assertTrue(state.start(42, RandomRevealSource(source, viewport)))
        val first = state.session!!
        state.advance(first.generation, 650f)
        assertFalse(state.start(99, null))
        assertEquals(first, state.session)
        assertEquals(650f, state.elapsedMs, 0f)
    }

    @Test fun cancelClearsAllTransientMotionAndAllowsSameIdAgain() {
        val state = GalleryRandomRevealState()
        state.start(42, null)
        state.advance(state.session!!.generation, 710f)
        state.clear()
        assertNull(state.session)
        assertEquals(0f, state.elapsedMs, 0f)
        assertTrue(state.start(42, null))
        assertEquals(0f, state.elapsedMs, 0f)
    }

    @Test fun staleFramesAndCleanupCannotAffectNextResult() {
        val state = GalleryRandomRevealState()
        state.start(42, null)
        val old = state.session!!.generation
        state.clear(old)
        state.start(99, null)
        state.advance(old, 1900f)
        state.updateSource(old, RandomRevealSource(source, viewport))
        state.clear(old)
        assertEquals(99L, state.session!!.postcardId)
        assertNull(state.session!!.source)
        assertEquals(0f, state.elapsedMs, 0f)
    }

    @Test fun reducedMotionCanJumpStraightToResultWithoutIntermediateFrames() {
        val state = GalleryRandomRevealState()
        state.start(42, null)
        state.advance(state.session!!.generation, RANDOM_REVEAL_TOTAL_MS)
        assertEquals(pose(42, 2200f), pose(42, state.elapsedMs, null))
        assertEquals(2200f, state.elapsedMs, 0f)
    }

    @Test fun invalidOrOutOfRangeFramesCannotPoisonState() {
        val state = GalleryRandomRevealState()
        state.start(42, null)
        val token = state.session!!.generation
        state.advance(token, Float.NaN)
        assertEquals(0f, state.elapsedMs, 0f)
        state.advance(token, 99999f)
        assertEquals(2200f, state.elapsedMs, 0f)
        state.advance(token, -1f)
        assertEquals(0f, state.elapsedMs, 0f)
    }
}
