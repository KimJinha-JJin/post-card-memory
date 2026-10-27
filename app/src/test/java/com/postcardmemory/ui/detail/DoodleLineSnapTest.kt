package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 낙서 직선의 수평·수직 자동 정렬 판정은 화면 raw 좌표 기준 각도로
 * 이루어진다(정규화 좌표는 엽서 가로세로 비율이 1:1이 아니면 각도가
 * 왜곡되므로 사용하지 않음). Offset은 Robolectric 없이 순수 JVM에서
 * 검증 가능하다(PostcardOverlayExportLogicTest와 동일한 전제).
 */
class DoodleLineSnapTest {

    private val minDistancePx = 8f

    /** 시작점(0,0) 기준으로 [angleDeg]도, 길이 [length]인 지점의 dx/dy. */
    private fun deltaForAngle(angleDeg: Float, length: Float = 100f): Pair<Float, Float> {
        val radians = Math.toRadians(angleDeg.toDouble())
        return (length * cos(radians)).toFloat() to (length * sin(radians)).toFloat()
    }

    // ---- 일반 각도: 정렬되지 않아야 한다 ----

    @Test
    fun angle44Degrees_fromNone_staysFree() {
        val (dx, dy) = deltaForAngle(44f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.NONE, result)
    }

    @Test
    fun angle46Degrees_fromNone_staysFree() {
        val (dx, dy) = deltaForAngle(46f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.NONE, result)
    }

    // ---- 수평: 좌우 양방향 ----

    @Test
    fun rightwardNearHorizontal_snapsHorizontal() {
        val (dx, dy) = deltaForAngle(3f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.HORIZONTAL, result)
    }

    @Test
    fun leftwardNearHorizontal_snapsHorizontal() {
        val (dx, dy) = deltaForAngle(180f - 3f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.HORIZONTAL, result)
    }

    // ---- 수직: 상하 양방향 ----

    @Test
    fun downwardNearVertical_snapsVertical() {
        val (dx, dy) = deltaForAngle(90f - 3f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.VERTICAL, result)
    }

    @Test
    fun upwardNearVertical_snapsVertical() {
        val (dx, dy) = deltaForAngle(-90f + 3f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.VERTICAL, result)
    }

    // ---- 진입 임계값 경계 ----

    @Test
    fun withinEntryThreshold_fromNone_snaps() {
        val (dx, dy) = deltaForAngle(11f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.HORIZONTAL, result)
    }

    @Test
    fun outsideEntryThreshold_fromNone_staysFree() {
        val (dx, dy) = deltaForAngle(13f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.NONE, result)
    }

    // ---- 해제 임계값과 흔들림 방지 ----

    @Test
    fun lockedHorizontal_withinExitThreshold_staysLocked() {
        // 진입 임계값(12도)은 넘었지만 해제 임계값(18도) 안쪽이라 유지되어야 한다.
        val (dx, dy) = deltaForAngle(15f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.HORIZONTAL,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.HORIZONTAL, result)
    }

    @Test
    fun lockedHorizontal_beyondExitThreshold_releasesToFree() {
        val (dx, dy) = deltaForAngle(30f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.HORIZONTAL,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.NONE, result)
    }

    @Test
    fun lockedVertical_withinExitThreshold_staysLocked() {
        val (dx, dy) = deltaForAngle(90f - 15f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.VERTICAL,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.VERTICAL, result)
    }

    @Test
    fun lockedVertical_beyondExitThreshold_releasesToFree() {
        val (dx, dy) = deltaForAngle(90f - 30f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.VERTICAL,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.NONE, result)
    }

    // ---- 상태 전환: 수평 <-> 수직 ----

    @Test
    fun lockedHorizontal_movesNearVertical_switchesDirectlyToVertical() {
        val (dx, dy) = deltaForAngle(90f - 5f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.HORIZONTAL,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.VERTICAL, result)
    }

    @Test
    fun lockedVertical_movesNearHorizontal_switchesDirectlyToHorizontal() {
        val (dx, dy) = deltaForAngle(5f)
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.VERTICAL,
            dx = dx,
            dy = dy,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.HORIZONTAL, result)
    }

    // ---- 짧은 거리 보호 ----

    @Test
    fun distanceBelowMinimum_keepsCurrentState_doesNotThrow() {
        val resultFromNone = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = 1f,
            dy = 1f,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.NONE, resultFromNone)

        val resultFromHorizontal = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.HORIZONTAL,
            dx = 1f,
            dy = 1f,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.HORIZONTAL, resultFromHorizontal)
    }

    @Test
    fun startEqualsEnd_doesNotThrowOrProduceNaN() {
        val result = resolveDoodleLineSnapDirection(
            current = DoodleLineSnapDirection.NONE,
            dx = 0f,
            dy = 0f,
            minDistancePx = minDistancePx
        )
        assertEquals(DoodleLineSnapDirection.NONE, result)
    }

    // ---- 끝점 보정 ----

    @Test
    fun snappedEndpoint_horizontal_usesStartYAndCurrentX() {
        val start = Offset(10f, 20f)
        val current = Offset(90f, 45f)

        val snapped = snappedDoodleLineEndpoint(
            direction = DoodleLineSnapDirection.HORIZONTAL,
            start = start,
            current = current
        )

        assertEquals(Offset(90f, 20f), snapped)
    }

    @Test
    fun snappedEndpoint_vertical_usesStartXAndCurrentY() {
        val start = Offset(10f, 20f)
        val current = Offset(90f, 45f)

        val snapped = snappedDoodleLineEndpoint(
            direction = DoodleLineSnapDirection.VERTICAL,
            start = start,
            current = current
        )

        assertEquals(Offset(10f, 45f), snapped)
    }

    @Test
    fun snappedEndpoint_none_returnsCurrentUnchanged() {
        val start = Offset(10f, 20f)
        val current = Offset(90f, 45f)

        val snapped = snappedDoodleLineEndpoint(
            direction = DoodleLineSnapDirection.NONE,
            start = start,
            current = current
        )

        assertEquals(current, snapped)
    }
}
