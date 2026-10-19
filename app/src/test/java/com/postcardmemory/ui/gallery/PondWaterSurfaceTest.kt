package com.postcardmemory.ui.gallery

import androidx.compose.ui.geometry.Offset
import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 108일차 하프톤 수면: 차오름(수위)·잔잔한 흐름·원형 물살의 순수 계산([PondWaterField])과
 * 파문 수명 정리, 연못 전용·grid 뒤 배치를 고정한다. 실제 색감·움직임의 느낌·프레임 성능은
 * 실기기 확인 몫이다.
 */
class PondWaterSurfaceTest {

    // SM-S936N 450dpi 근사: 1dp = 2.8125px, 화면 1080×2200px.
    private val unit = 2.8125f
    private val width = 1080f
    private val height = 2200f
    private val surfaceTop = 120f
    private val noImpulses = FloatArray(POND_WATER_MAX_IMPULSES * 4)

    private fun field() = PondWaterField(width, height, surfaceTop, unit)

    private fun dotsOf(frame: PondWaterFrame): List<Offset> {
        val dots = ArrayList<Offset>()
        listOf(
            frame.small to frame.smallCount,
            frame.medium to frame.mediumCount,
            frame.large to frame.largeCount
        ).forEach { (array, count) ->
            for (i in 0 until count step 2) dots.add(Offset(array[i], array[i + 1]))
        }
        return dots
    }

    private fun render(
        f: PondWaterField,
        timeSec: Float,
        level: Float,
        impulses: FloatArray = noImpulses,
        count: Int = 0
    ): PondWaterFrame = PondWaterFrame(f.dotCount).also { f.render(timeSec, level, impulses, count, it) }

    @Test
    fun waterRisesFromTheBottomToTheSurfaceLine() {
        val f = field()
        val maxWave = (4f + 2f) * unit

        // 수위 0: 윗선이 화면 바닥 근처(차오르는 동안은 물결이 2.5배까지 커진다).
        assertTrue(f.waterlineY(300f, 0f, 0f) > height - maxWave * 2.5f - 1f)
        // 수위 1: 윗선이 수면 시작선 ± 물결.
        val top = f.waterlineY(300f, 0f, 1f)
        assertTrue(top in (surfaceTop - maxWave - 1f)..(surfaceTop + maxWave + 1f))

        val empty = render(f, 0f, 0f).dotCount
        val half = render(f, 0f, 0.5f).dotCount
        val full = render(f, 0f, 1f).dotCount
        assertTrue("차오를수록 물(망점)이 늘어나야 함: $empty < $half < $full", empty < half && half < full)
    }

    @Test
    fun fullWater_staysBelowSurfaceAndInsideCanvas() {
        val f = field()
        val lowestAllowed = surfaceTop - (4f + 2f) * unit - 1f
        val spacing = 6f * unit
        dotsOf(render(f, 3.3f, 1f)).forEach { dot ->
            assertTrue("수면 위로 올라간 점: $dot", dot.y >= lowestAllowed)
            assertTrue("화면 밖 점: $dot", dot.x in -spacing - 1f..(width + spacing + 1f) && dot.y < height + 10f)
        }
    }

    @Test
    fun idleSurface_movesOverTime_butIsDeterministic() {
        val f = field()
        val a = dotsOf(render(f, 1.0f, 1f))
        val again = dotsOf(render(f, 1.0f, 1f))
        val later = dotsOf(render(f, 2.0f, 1f))

        assertEquals("같은 시각이면 같은 그림", a, again)
        assertNotEquals("시간이 흐르면 수면이 움직여야 함(정지 배경 아님)", a, later)
    }

    @Test
    fun impulse_ripplesDotsAroundItsCenterOnly() {
        val f = field()
        val impulses = FloatArray(POND_WATER_MAX_IMPULSES * 4)
        // 왼쪽 아래(200, 1900)에 0.2초 된 물살 — 퍼진 띠는 중심에서 (속도×나이 + 띠×2.5) 안.
        impulses[0] = 200f; impulses[1] = 1900f; impulses[2] = 0.2f
        impulses[3] = pondWaterImpulseStrength(34f * unit)

        val calm = dotsOf(render(f, 1f, 1f))
        val stirred = dotsOf(render(f, 1f, 1f, impulses, 1))

        // 밀림 최대치(세기)만큼 점이 띠 밖으로 나갈 수 있어 그만큼 더 여유를 둔다.
        val reach = (IMPULSE_SPEED_DP_PER_S * 0.2f + IMPULSE_RING_WIDTH_DP * 2.5f + 10f) * unit +
            pondWaterImpulseStrength(34f * unit)
        fun far(dot: Offset) = (dot - Offset(200f, 1900f)).getDistance() > reach + 20f
        assertEquals("물살에서 먼 망점은 그대로여야 함", calm.filter(::far).toSet(), stirred.filter(::far).toSet())
        assertNotEquals("물살 근처 망점은 움직여야 함", calm.filterNot(::far).toSet(), stirred.filterNot(::far).toSet())
    }

    @Test
    fun dotCountStaysBoundedByTheGrid() {
        val f = field()
        val spacing = 6f * unit
        // 기울기 쏠림 여유(좌우 각 24dp + 한 칸)까지 포함한 격자 상한.
        val sideMargin = 40f * POND_TILT_MAX * unit + spacing
        val upperBound = (((width + 2 * sideMargin) / spacing) + 2) * ((height / spacing) + 2)
        assertTrue("망점 ${f.dotCount}개가 6dp 격자 상한을 넘음", f.dotCount <= upperBound)
        assertEquals(0, PondWaterField(0f, 0f, surfaceTop, unit).dotCount)
    }

    @Test
    fun pruneRipples_keepsWaterImpulsesForTheScaledLifespan() {
        val controller = PondController()
        controller.addRipple(Offset(10f, 10f), 30f, lifespanMillis = 500L)
        val born = controller.ripples.single().bornAtMillis

        controller.pruneRipples(born + 900L, POND_WATER_IMPULSE_LIFE_SCALE)
        assertEquals("원래 수명(500ms)을 넘어도 2배(1000ms) 안이면 남아야 함", 1, controller.ripples.size)

        controller.pruneRipples(born + 1_100L, POND_WATER_IMPULSE_LIFE_SCALE)
        assertTrue("2배 수명이 지나면 지워져야 함", controller.ripples.isEmpty())
    }

    @Test
    fun gravity_mapsToScreenTilt_forEveryRotation_andIsClamped() {
        val g = 9.80665f
        // 세로(0°): 오른쪽이 내려가면 기기 x축이 아래를 향해 x 값이 음수 → +기울기(감도 ×1.5).
        assertEquals(0.3f, screenTiltFromGravity(-0.2f * g, 0.9f * g, 0), 1e-4f)
        assertEquals(-0.3f, screenTiltFromGravity(0.2f * g, 0.9f * g, 0), 1e-4f)
        // 가로·뒤집힘에서는 화면 x축이 기기의 다른 축이 된다.
        assertEquals(0.3f, screenTiltFromGravity(0f, 0.2f * g, 1), 1e-4f)
        assertEquals(0.3f, screenTiltFromGravity(0.2f * g, 0f, 2), 1e-4f)
        assertEquals(0.3f, screenTiltFromGravity(0f, -0.2f * g, 3), 1e-4f)
        // 90°로 세워도 묶인 값 이상은 안 나온다.
        assertEquals(POND_TILT_MAX, screenTiltFromGravity(-g, 0f, 0), 1e-4f)
    }

    @Test
    fun tilt_raisesWaterOnTheLoweredSide_andShiftsDots() {
        val f = field()
        val left = 0.1f * width
        val right = 0.9f * width
        val tilt = 0.5f

        // 오른쪽이 내려가면(+) 오른쪽 윗선은 위로(y 감소), 왼쪽은 아래로.
        assertTrue(f.waterlineY(right, 1f, 1f, tilt) < f.waterlineY(right, 1f, 1f))
        assertTrue(f.waterlineY(left, 1f, 1f, tilt) > f.waterlineY(left, 1f, 1f))

        val flat = dotsOf(render(f, 1f, 1f)).map { it.x }.average()
        val tilted = dotsOf(PondWaterFrame(f.dotCount).also {
            f.render(1f, 1f, noImpulses, 0, it, tilt = tilt)
        }).map { it.x }.average()
        assertTrue("도트 면이 낮아진 오른쪽으로 쏠려야 함: $flat → $tilted", tilted > flat)
    }

    @Test
    fun tiltedHalftone_fillsRightUpToTheBlueLine() {
        // 수면을 내려 둔 화면에서 최대로 기울여도, 도트는 푸른 선 아래에만 있고 선 바로 밑이 비지 않아야 한다.
        val deepTop = 900f
        val f = PondWaterField(width, height, deepTop, unit)
        val tilt = POND_TILT_MAX
        val frame = PondWaterFrame(f.dotCount).also { f.render(2f, 1f, noImpulses, 0, it, tilt = tilt) }
        val dots = dotsOf(frame)
        val drift = (1.2f + 1f) * unit

        dots.forEach { dot ->
            assertTrue(
                "푸른 선 위로 삐져나온 도트: $dot",
                dot.y >= f.waterlineY(dot.x, 2f, 1f, tilt) - drift
            )
        }
        val spacing = 6f * unit
        var x = spacing
        while (x < width - spacing) {
            val line = f.waterlineY(x, 2f, 1f, tilt)
            assertTrue(
                "x=$x 에서 선 바로 아래(16dp 안)에 도트가 없음 — 하프톤이 선을 못 따라감",
                dots.any { kotlin.math.abs(it.x - x) < spacing && it.y - line in -drift..(16f * unit) }
            )
            x += 30f * unit
        }
    }

    @Test
    fun leafRide_matchesTheWaterline() {
        val f = field()
        val tilt = 0.4f
        listOf(0.1f, 0.5f, 0.9f).forEach { fraction ->
            val x = width * fraction
            val u = 2f * fraction - 1f
            val delta = f.waterlineY(x, 1f, 1f, tilt) - f.waterlineY(x, 1f, 1f)
            assertEquals("연잎이 타는 높이와 수면 윗선 변화가 같아야 함", -unit * pondWaterTiltRiseDp(tilt, u), delta, 0.01f)
        }
        val angle = pondWaterTiltAngleDegrees(tilt, width / unit)
        assertTrue("오른쪽이 내려가면 수면은 오른쪽이 올라가 반시계(−)로 기운다: $angle", angle < 0f)
        assertEquals(0f, pondWaterTiltAngleDegrees(0f, width / unit), 0f)
    }

    @Test
    fun tiltSpring_settlesSmoothly_withoutWildOvershoot() {
        val spring = PondTiltSpring()
        var peak = 0f
        repeat(180) { // 60fps × 3초
            spring.step(0.5f, 1f / 60f)
            peak = maxOf(peak, spring.position)
        }
        assertEquals("3초 뒤엔 목표에 가라앉아야 함", 0.5f, spring.position, 0.01f)
        assertTrue("출렁여도 목표의 1.3배를 넘지 않아야 함: $peak", peak < 0.65f)

        // 프레임이 길게 끊겨도(1초) 한 번에 튀지 않는다.
        val jump = PondTiltSpring().also { it.step(0.5f, 1f) }
        assertTrue(jump.position < 0.05f)
    }

    @Test
    fun tiltInput_isLowPassed() {
        val input = PondTiltInput()
        input.onSample(0.6f)
        assertTrue("센서 한 번에 목표가 통째로 바뀌면 안 됨", input.target in 0.01f..0.2f)
        repeat(60) { input.onSample(0.6f) }
        assertEquals(0.6f, input.target, 0.01f)
    }

    @Test
    fun tiltSensor_followsScreenLifecycle_andIsReleased() {
        val source = readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/gallery/PondWaterSurface.kt",
                "app/src/main/java/com/postcardmemory/ui/gallery/PondWaterSurface.kt"
            )
        )
        val start = source.indexOf("private fun rememberPondTiltInput()")
        val body = source.substring(start, source.indexOf("\n/**", start))

        assertTrue(body.contains("Sensor.TYPE_GRAVITY"))
        assertTrue("중력 센서가 없으면 가속도계로", body.contains("?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)"))
        assertTrue(body.contains("Lifecycle.Event.ON_RESUME -> register()"))
        assertTrue(body.contains("Lifecycle.Event.ON_PAUSE -> unregister()"))
        assertTrue("중복 등록 방지", body.contains("if (registered) return"))
        val dispose = body.substring(body.indexOf("onDispose {\n            lifecycleOwner"))
        assertTrue("연못을 끄면 해제", dispose.contains("removeObserver(observer)") && dispose.contains("unregister()"))
    }

    @Test
    fun leavesSlideWithTilt_outsideTheMeasuredRestPosition() {
        val stampCard = readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/components/StampCard.kt",
                "app/src/main/java/com/postcardmemory/ui/components/StampCard.kt"
            )
        )
        val measured = stampCard.indexOf(".onGloballyPositioned { coordinates ->")
        val tiltLayer = stampCard.indexOf(
            "translationX = tiltDrift\n                translationY = tiltRise\n                rotationZ = tiltLean"
        )
        val leaf = stampCard.indexOf("image = pondLeaf")

        assertTrue("연잎 기울기 이동을 찾지 못함", tiltLayer >= 0)
        assertTrue("기울기 이동은 위치 측정 안쪽이어야 새총·반사 기준 위치가 오염되지 않음", tiltLayer > measured)
        assertTrue("연잎·우표가 같이 움직이도록 연잎 그리기보다 바깥이어야 함", leaf > tiltLayer)
    }

    @Test
    fun leafSprings_withDifferentSeeds_moveOutOfStep_butBothSettle() {
        // 카드마다 다른 탄성·감쇠: 같은 기울기를 받아도 0.3초 뒤 위치가 서로 달라야 덩어리처럼 안 보인다.
        val soft = com.postcardmemory.ui.components.PondLeafTiltSpring(stiffness = 6f, dampingRatio = 0.65f)
        val snappy = com.postcardmemory.ui.components.PondLeafTiltSpring(stiffness = 18f, dampingRatio = 0.35f)
        repeat(18) { soft.step(50f, 1f / 60f); snappy.step(50f, 1f / 60f) }
        assertTrue("0.3초 뒤 두 연잎 위치 차가 커야 함: ${soft.position} vs ${snappy.position}",
            kotlin.math.abs(soft.position - snappy.position) > 5f)

        repeat(600) { soft.step(50f, 1f / 60f); snappy.step(50f, 1f / 60f) }
        assertEquals(50f, soft.position, 0.5f)
        assertEquals(50f, snappy.position, 0.5f)

        soft.reset()
        assertEquals(0f, soft.position, 0f)
        assertEquals(0f, soft.velocity, 0f)
    }

    @Test
    fun controllerTilt_isFedByWater_andClearedOnReset() {
        val controller = PondController()
        controller.waterTilt = 0.4f
        controller.reset()
        assertEquals(0f, controller.waterTilt, 0f)

        val water = readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/gallery/PondWaterSurface.kt",
                "app/src/main/java/com/postcardmemory/ui/gallery/PondWaterSurface.kt"
            )
        )
        assertTrue("수면이 매 프레임 기울기를 넘겨야 함", water.contains("controller.waterTilt = tiltSpring.position"))
        assertTrue("수면 윗선 푸른 선을 그려야 함", water.contains("native.drawPath(linePath, linePaint)"))
    }

    @Test
    fun waterIsDrawnOnlyInPond_behindTheGrid_andTealRingsAreGone() {
        val source = readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt",
                "app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt"
            )
        )
        val water = source.indexOf("PondWaterSurface(\n")
        val grid = source.indexOf("LazyVerticalGrid(", water)

        assertTrue("수면을 찾지 못함", water >= 0)
        assertTrue("수면은 연못 분기 안에 있어야 함", source.lastIndexOf("if (isPondModeOn) {", water) in (water - 300)..water)
        assertTrue("수면은 grid보다 먼저(뒤쪽) 그려야 함", grid > water)
        assertTrue("수면은 파문 이벤트를 받아야 함", source.substring(water, grid).contains("controller = pondController"))
        assertFalse("청록 동심원 오버레이는 수면 반응으로 대체됐음", source.contains("PondRippleOverlay("))
    }
}
