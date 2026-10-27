package com.postcardmemory.ui.gallery

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 88일차: 갤러리 퀵 셀렉트(신문 오림 손 부채)의 단계 전이와 부채 geometry.
 * 애니메이션의 픽셀 위치가 아니라 입력 보호와 겹침·배치 규칙만 고정한다.
 */
class GalleryQuickSelectFanTest {

    // --- 단계 전이 ---

    @Test
    fun phase_openAndCloseRequestsFollowClosedOpeningOpenClosing() {
        var phase = GalleryQuickSelectPhase.CLOSED

        phase = phase.onRequest(open = true)
        assertEquals(GalleryQuickSelectPhase.OPENING, phase)
        phase = phase.onAnimationFinished()
        assertEquals(GalleryQuickSelectPhase.OPEN, phase)

        // 뒤로가기·바깥 탭 모두 부모의 닫기 요청으로 들어온다.
        phase = phase.onRequest(open = false)
        assertEquals(GalleryQuickSelectPhase.CLOSING, phase)
        phase = phase.onAnimationFinished()
        assertEquals(GalleryQuickSelectPhase.CLOSED, phase)
    }

    @Test
    fun phase_repeatedRequestsInSameDirectionDoNotRestart() {
        assertEquals(GalleryQuickSelectPhase.OPENING, GalleryQuickSelectPhase.OPENING.onRequest(open = true))
        assertEquals(GalleryQuickSelectPhase.OPEN, GalleryQuickSelectPhase.OPEN.onRequest(open = true))
        assertEquals(GalleryQuickSelectPhase.CLOSING, GalleryQuickSelectPhase.CLOSING.onRequest(open = false))
        assertEquals(GalleryQuickSelectPhase.CLOSED, GalleryQuickSelectPhase.CLOSED.onRequest(open = false))
    }

    @Test
    fun phase_reversingMidAnimationOnlyChangesDirection() {
        assertEquals(GalleryQuickSelectPhase.CLOSING, GalleryQuickSelectPhase.OPENING.onRequest(open = false))
        assertEquals(GalleryQuickSelectPhase.OPENING, GalleryQuickSelectPhase.CLOSING.onRequest(open = true))
    }

    @Test
    fun select_isIgnoredUnlessFullyOpen() {
        listOf(
            GalleryQuickSelectPhase.CLOSED,
            GalleryQuickSelectPhase.OPENING,
            GalleryQuickSelectPhase.CLOSING
        ).forEach { phase ->
            val (next, accepted) = phase.onSelect()
            assertFalse("$phase 에서 기능이 실행되면 안 됨", accepted)
            assertEquals(phase, next)
        }
    }

    @Test
    fun select_rapidTapsDispatchExactlyOnce() {
        var phase = GalleryQuickSelectPhase.OPEN
        var dispatched = 0

        repeat(3) {
            val (next, accepted) = phase.onSelect()
            phase = next
            if (accepted) dispatched++
        }

        assertEquals(1, dispatched)
        assertEquals(GalleryQuickSelectPhase.CLOSING, phase)
    }

    // --- 자산 매핑 ---

    @Test
    fun items_areFiveWithUniqueSlotsAndBlocksInsideTheSourceImage() {
        val items = GalleryQuickSelectItem.entries
        assertEquals(QUICK_SELECT_HAND_COUNT, items.size)
        assertEquals((0 until QUICK_SELECT_HAND_COUNT).toSet(), items.map { it.slotFromBottom }.toSet())
        items.forEach { item ->
            assertTrue(item.blockLeftPx >= 0f && item.blockRightPx <= QUICK_SELECT_HAND_SOURCE_PX)
            assertTrue(item.blockTopPx >= 0f && item.blockBottomPx <= QUICK_SELECT_HAND_SOURCE_PX)
            assertTrue(item.blockLeftPx < item.blockRightPx && item.blockTopPx < item.blockBottomPx)
        }
    }

    // --- 부채 geometry ---

    @Test
    fun spacing_isClampedForSmallAndTallScreens() {
        assertEquals(QUICK_SELECT_MAX_SPACING_DEG, quickSelectSpacingDegrees(900f), 0.0001f)
        assertEquals(QUICK_SELECT_MIN_SPACING_DEG, quickSelectSpacingDegrees(250f), 0.0001f)
        val mid = quickSelectSpacingDegrees(330f)
        assertTrue(mid in QUICK_SELECT_MIN_SPACING_DEG..QUICK_SELECT_MAX_SPACING_DEG)
    }

    @Test
    fun openFan_goesFromBottomToTopWithSmallRotation() {
        val spacing = QUICK_SELECT_MAX_SPACING_DEG
        val angles = (0 until QUICK_SELECT_HAND_COUNT).map { slot ->
            quickSelectHandAngle(slot, spacing, spread = 1f, tilt = 0f)
        }
        // 아래 → 위로 차례대로, 가운데 손은 수평.
        assertEquals(angles.sorted(), angles)
        assertEquals(0f, angles[2], 0.0001f)
        // 회전은 보조 수단 — 블록이 크게 기울지 않는다.
        assertTrue(angles.all { abs(it) <= 20f })
    }

    @Test
    fun openFan_bottomBlockSitsAboveContainerBottomAndBlocksStayOnScreen() {
        val width = 360f
        val height = 640f
        val spacing = quickSelectSpacingDegrees(height)
        val pivot = quickSelectPivot(width, height, spacing)

        val bottom = quickSelectBlockCenter(pivot, QUICK_SELECT_FAN_RADIUS_DP, quickSelectSlotAngle(0, spacing))
        assertEquals(height - QUICK_SELECT_BOTTOM_BLOCK_GAP_DP, bottom.y, 0.01f)

        (0 until QUICK_SELECT_HAND_COUNT).forEach { slot ->
            val center = quickSelectBlockCenter(pivot, QUICK_SELECT_FAN_RADIUS_DP, quickSelectSlotAngle(slot, spacing))
            assertTrue("slot $slot 블록이 화면 밖", center.x in 0f..width && center.y in 0f..height)
        }
    }

    @Test
    fun closedBundle_collapsesNearBottomSlotAndPeeksFromTheRightEdge() {
        val width = 360f
        val height = 640f
        val spacing = quickSelectSpacingDegrees(height)
        val pivot = quickSelectPivot(width, height, spacing)
        val bottomAngle = quickSelectSlotAngle(0, spacing)

        (0 until QUICK_SELECT_HAND_COUNT).forEach { slot ->
            val closedAngle = quickSelectHandAngle(slot, spacing, spread = 0f, tilt = 0f)
            assertTrue(abs(closedAngle - bottomAngle) <= 5f)
        }

        // 닫힌 묶음의 블록 중심은 오른쪽 끝 근처 — 대부분 화면 밖에 숨는다.
        val closed = quickSelectAnimatedBlockCenter(pivot, slide = 0f, angleDeg = bottomAngle)
        assertTrue(closed.x > width - 20f)
        // 88일차 QA: 닫힌 묶음만 더 내려앉고, 다 들어오면 열린 부채 자리와 정확히 같다.
        val closedWithoutDrop = quickSelectBlockCenter(pivot, quickSelectHandRadius(0f), bottomAngle)
        assertEquals(QUICK_SELECT_CLOSED_DROP_DP, closed.y - closedWithoutDrop.y, 0.001f)
        val arrived = quickSelectAnimatedBlockCenter(pivot, slide = 1f, angleDeg = bottomAngle)
        assertEquals(height - QUICK_SELECT_BOTTOM_BLOCK_GAP_DP, arrived.y, 0.01f)
        assertTrue("닫힌 손잡이가 컨테이너 안에 있어야 함", closed.y < height)
        assertEquals(QUICK_SELECT_CLOSED_SCALE, quickSelectHandScale(0f), 0.0001f)
        assertEquals(1f, quickSelectHandScale(1f), 0.0001f)
    }

    @Test
    fun openFan_neighbouringBlocksKeepQaApprovedGap() {
        // 88일차 실기기 QA: 8°·400dp(블록 중심 간격 약 56dp)는 손이 빡빡하게 겹쳐
        // 보여 넓혔다. 보통 폰 높이에서는 이웃 블록 중심이 64dp 이상 떨어진다.
        val width = 360f
        val height = 640f
        val spacing = quickSelectSpacingDegrees(height)
        val pivot = quickSelectPivot(width, height, spacing)
        val centers = (0 until QUICK_SELECT_HAND_COUNT).map { slot ->
            quickSelectBlockCenter(pivot, QUICK_SELECT_FAN_RADIUS_DP, quickSelectSlotAngle(slot, spacing))
        }
        centers.zipWithNext().forEach { (a, b) ->
            val gap = Math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble())
            assertTrue("이웃 블록 간격 부족: $gap", gap >= 64.0)
        }
    }

    @Test
    fun tilt_bendsTheBundleDownward() {
        val spacing = QUICK_SELECT_MAX_SPACING_DEG
        val upright = quickSelectHandAngle(0, spacing, spread = 0f, tilt = 0f)
        val tilted = quickSelectHandAngle(0, spacing, spread = 0f, tilt = 1f)
        assertEquals(-QUICK_SELECT_TILT_DEG, tilted - upright, 0.0001f)
    }

    @Test
    fun handSpread_staggerIsShortAndEveryHandFinishes() {
        (0 until QUICK_SELECT_HAND_COUNT).forEach { slot ->
            assertEquals(0f, quickSelectHandSpread(0f, slot), 0.0001f)
            assertEquals(1f, quickSelectHandSpread(1f, slot), 0.0001f)
        }
        // 맨 위 손도 전체 진행의 절반 안쪽에서 움직이기 시작한다.
        assertTrue(QUICK_SELECT_STAGGER * (QUICK_SELECT_HAND_COUNT - 1) < 0.5f)
    }

    @Test
    fun touchAreas_ofNeighbouringHandsNeverOverlapAndStayTappable() {
        listOf(250f, 330f, 640f, 900f).forEach { height ->
            val width = 360f
            val spacing = quickSelectSpacingDegrees(height)
            val pivot = quickSelectPivot(width, height, spacing)
            val halfHeight = quickSelectTouchHalfHeightDp(spacing)
            // 48dp 최소 터치 높이에 가깝게 유지된다.
            assertTrue("h=$height 터치 높이 부족: ${halfHeight * 2}", halfHeight * 2f >= 46f)

            val rects = GalleryQuickSelectItem.entries
                .sortedBy { it.slotFromBottom }
                .map { item ->
                    val angle = quickSelectSlotAngle(item.slotFromBottom, spacing)
                    val center = quickSelectBlockCenter(pivot, QUICK_SELECT_FAN_RADIUS_DP, angle)
                    rotatedRect(
                        center,
                        halfWidth = item.blockHalfWidthDp + QUICK_SELECT_TOUCH_PAD_X_DP,
                        halfHeight = halfHeight,
                        angleDeg = angle
                    )
                }
            rects.zipWithNext().forEachIndexed { index, (lower, upper) ->
                assertFalse("h=$height slot $index/${index + 1} 터치 영역 겹침", convexOverlap(lower, upper))
            }
        }
    }

    private fun rotatedRect(
        center: QuickSelectPoint,
        halfWidth: Float,
        halfHeight: Float,
        angleDeg: Float
    ): List<QuickSelectPoint> {
        val a = Math.toRadians(angleDeg.toDouble())
        val c = cos(a).toFloat()
        val s = sin(a).toFloat()
        return listOf(-1f to -1f, 1f to -1f, 1f to 1f, -1f to 1f).map { (sx, sy) ->
            val x = sx * halfWidth
            val y = sy * halfHeight
            QuickSelectPoint(center.x + x * c - y * s, center.y + x * s + y * c)
        }
    }

    /** 볼록 다각형 분리축 판정. 변이 맞닿는 정도는 겹침으로 보지 않는다. */
    private fun convexOverlap(a: List<QuickSelectPoint>, b: List<QuickSelectPoint>): Boolean {
        for (poly in listOf(a, b)) {
            for (i in poly.indices) {
                val p1 = poly[i]
                val p2 = poly[(i + 1) % poly.size]
                val nx = -(p2.y - p1.y)
                val ny = p2.x - p1.x
                val aProj = a.map { it.x * nx + it.y * ny }
                val bProj = b.map { it.x * nx + it.y * ny }
                if (aProj.max() <= bProj.min() + 1e-3f || bProj.max() <= aProj.min() + 1e-3f) {
                    return false
                }
            }
        }
        return true
    }
}
