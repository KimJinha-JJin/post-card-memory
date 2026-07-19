package com.postcardmemory.ui.gallery

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.postcardmemory.ui.theme.GraphiteAccent
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface

internal const val RACE_TRACK_HEIGHT_FRACTION = 0.30f
internal const val RACE_TRACK_SIDE_INSET_FRACTION = 0.09f

private val RaceTrackBase = Color(0xFFB9A489)
private val RaceTrackLane = Color(0xFFEFE6D2)
private val RaceTrackCheckerDark = Color(0xFF8A7A63)
private val RaceLightHousing = Color(0xFF4A4038)
private val RaceLightDim = Color(0xFF8A8074)
private val RaceCurbCream = Color(0xFFF3E8D4)
private val RaceCurbBurgundy = Color(0xFF8C4A3E)
private val RaceFenceColor = Color(0xFF7C7266)

/** 쫑쫑컵 트랙의 좌표 — Venue(상시 표시)와 Overlay(레이스 진행)가 반드시 같은 값을 공유한다. */
internal data class RaceTrackGeometry(
    val trackTop: Float,
    val trackBottom: Float,
    val trackLeft: Float,
    val trackRight: Float,
    val laneHeight: Float,
    val startLineX: Float,
    val finishLineX: Float
) {
    fun laneCenterY(laneIndex: Int): Float = trackTop + laneHeight * (laneIndex + 0.5f)
}

internal fun computeRaceTrackGeometry(
    stageWidthPx: Float,
    stageHeightPx: Float,
    cardWidthPx: Float,
    bottomMarginPx: Float
): RaceTrackGeometry {
    val trackTop = stageHeightPx * (1f - RACE_TRACK_HEIGHT_FRACTION)
    val trackBottom = stageHeightPx - bottomMarginPx
    val trackLeft = stageWidthPx * RACE_TRACK_SIDE_INSET_FRACTION
    val trackRight = stageWidthPx * (1f - RACE_TRACK_SIDE_INSET_FRACTION)
    val laneHeight = (trackBottom - trackTop) / 3f
    val startLineX = trackLeft + cardWidthPx * 0.55f
    val finishLineX = trackRight - cardWidthPx * 0.55f
    return RaceTrackGeometry(
        trackTop = trackTop,
        trackBottom = trackBottom,
        trackLeft = trackLeft,
        trackRight = trackRight,
        laneHeight = laneHeight,
        startLineX = startLineX,
        finishLineX = finishLineX
    )
}

/**
 * 쫑쫑컵 트랙 바닥 — 하나의 렌더러가 `intensity`(0=졸린 평상시, 1=레이스 진행)만 다르게
 * 그린다. IDLE용과 레이스용 트랙을 별도로 그리지 않는다.
 * `dashScrollFraction`이 0 이상이면(RACING 중 raceProgress) 차선에 흐르는 점선을 더해
 * 가로 속도감을 낸다 — 별도 프레임 클록 없이 기존 raceProgress를 재사용한다.
 */
@Composable
private fun RaceTrackCanvas(
    geometry: RaceTrackGeometry,
    intensity: Float,
    dashScrollFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawRect(
            color = RaceTrackBase.copy(alpha = 0.55f * intensity),
            topLeft = Offset(geometry.trackLeft, geometry.trackTop),
            size = Size(geometry.trackRight - geometry.trackLeft, geometry.trackBottom - geometry.trackTop)
        )

        val dashLength = 10.dp.toPx()
        val dashGap = 8.dp.toPx()
        val dashPeriod = dashLength + dashGap

        for (lane in 1 until 3) {
            val laneY = geometry.trackTop + geometry.laneHeight * lane
            drawLine(
                color = RaceTrackLane.copy(alpha = 0.5f * intensity),
                start = Offset(geometry.trackLeft, laneY),
                end = Offset(geometry.trackRight, laneY),
                strokeWidth = 1.5.dp.toPx()
            )

            if (dashScrollFraction >= 0f) {
                val scrollOffset = (dashScrollFraction * dashPeriod * 6f) % dashPeriod
                var dashX = geometry.trackLeft - scrollOffset
                while (dashX < geometry.trackRight) {
                    val segStart = dashX.coerceAtLeast(geometry.trackLeft)
                    val segEnd = (dashX + dashLength).coerceAtMost(geometry.trackRight)
                    if (segEnd > segStart) {
                        drawLine(
                            color = RaceTrackLane.copy(alpha = 0.85f * intensity),
                            start = Offset(segStart, laneY),
                            end = Offset(segEnd, laneY),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                    dashX += dashPeriod
                }
            }
        }

        val checkerBlock = 5.dp.toPx()
        val checkerAlpha = 0.65f * intensity
        val checkerLines = listOf(
            geometry.startLineX to true,
            geometry.finishLineX to (intensity > 0.9f)
        )
        checkerLines.forEach { (lineX, visible) ->
            if (!visible) return@forEach
            var blockY = geometry.trackTop
            var row = 0
            while (blockY < geometry.trackBottom) {
                val color = if (row % 2 == 0) RaceTrackLane else RaceTrackCheckerDark
                drawRect(
                    color = color.copy(alpha = checkerAlpha),
                    topLeft = Offset(lineX - checkerBlock / 2f, blockY),
                    size = Size(checkerBlock, checkerBlock)
                )
                blockY += checkerBlock
                row++
            }
        }
    }
}

/** 트랙 위쪽 가장자리의 종이 연석과 낮은 펜스. */
@Composable
private fun RaceCurbAndFence(
    geometry: RaceTrackGeometry,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val curbHeight = 5.dp.toPx()
        val curbBlock = 8.dp.toPx()
        val curbColors = listOf(RaceCurbCream, RaceCurbBurgundy)
        var curbX = geometry.trackLeft
        var curbIndex = 0
        while (curbX < geometry.trackRight) {
            drawRect(
                color = curbColors[curbIndex % 2].copy(alpha = 0.7f * intensity),
                topLeft = Offset(curbX, geometry.trackTop - curbHeight),
                size = Size(curbBlock, curbHeight)
            )
            curbX += curbBlock
            curbIndex++
        }

        val fenceY = geometry.trackTop - curbHeight - 10.dp.toPx()
        drawLine(
            color = RaceFenceColor.copy(alpha = 0.5f * intensity),
            start = Offset(geometry.trackLeft, fenceY),
            end = Offset(geometry.trackRight, fenceY),
            strokeWidth = 2.dp.toPx()
        )
        val postGap = 26.dp.toPx()
        var postX = geometry.trackLeft
        while (postX <= geometry.trackRight) {
            drawLine(
                color = RaceFenceColor.copy(alpha = 0.5f * intensity),
                start = Offset(postX, fenceY),
                end = Offset(postX, fenceY + 10.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            postX += postGap
        }
    }
}

/** 출발선 옆 1·2·3 그리드 번호 — 레이스 시작 위치와 같은 [RaceTrackGeometry]를 사용한다. */
@Composable
private fun RaceStartGrid(
    geometry: RaceTrackGeometry,
    intensity: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        for (lane in 0 until 3) {
            Text(
                text = (lane + 1).toString(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GraphiteAccent.copy(alpha = 0.5f + 0.5f * intensity),
                modifier = Modifier.graphicsLayer {
                    translationX = geometry.startLineX - 26.dp.toPx()
                    translationY = geometry.laneCenterY(lane) - 8.dp.toPx()
                }
            )
        }
    }
}

/** "쫑쫑컵" 팻말 — 세션이 새로 시작될 때 살짝 흔들리고, 결승 순간 한 번 통 튄다. */
@Composable
private fun RaceBanner(
    wobbleTrigger: Int,
    thumpTrigger: Boolean,
    modifier: Modifier = Modifier
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(wobbleTrigger) {
        if (wobbleTrigger > 0) {
            rotation.snapTo(-7f)
            rotation.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
    }
    val thump = remember { Animatable(1f) }
    LaunchedEffect(thumpTrigger) {
        if (thumpTrigger) {
            thump.snapTo(0.9f)
            thump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = rotation.value
            scaleX = thump.value
            scaleY = thump.value
        }
    ) {
        Box(
            modifier = Modifier
                .background(PaperSurface, RoundedCornerShape(8.dp))
                .border(1.dp, PaperDivider, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = "쫑쫑컵",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GraphiteAccent
            )
        }
    }
}

/** 출발선 옆 픽셀 신호등 — 평상시엔 꺼진 채 자리만 지키다가 COUNTDOWN에만 점등한다. */
@Composable
internal fun RaceTrafficLight(
    countdownStep: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(width = 18.dp, height = 48.dp)
    ) {
        val lightColors = listOf(
            Color(0xFFB8564A),
            Color(0xFFD9A441),
            Color(0xFF7E9C7A)
        )
        drawRoundRect(
            color = RaceLightHousing,
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        val slotHeight = size.height / 3f
        lightColors.forEachIndexed { index, color ->
            val isLit = index == countdownStep
            drawCircle(
                color = if (isLit) color else RaceLightDim.copy(alpha = 0.5f),
                radius = slotHeight * 0.32f,
                center = Offset(size.width / 2f, slotHeight * (index + 0.5f))
            )
        }
    }
}

/**
 * 쫑쫑컵 모드에 들어온 순간부터 항상 떠 있는 작은 경기장 배경.
 * 레이스가 시작되지 않았어도(IDLE) 옅게 표시되고, 진행 단계에 따라 선명해진다.
 * 양떼목장(raceEnabled = false)에서는 아예 호출되지 않는다.
 */
@Composable
fun SheepRanchRaceVenue(
    raceState: SheepRanchRaceState,
    racePhaseProgress: Float,
    raceProgress: Float,
    countdownStep: Int,
    stageWidthPx: Float,
    stageHeightPx: Float,
    cardWidthPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val geometry = remember(stageWidthPx, stageHeightPx, cardWidthPx) {
        computeRaceTrackGeometry(
            stageWidthPx = stageWidthPx,
            stageHeightPx = stageHeightPx,
            cardWidthPx = cardWidthPx,
            bottomMarginPx = with(density) { 8.dp.toPx() }
        )
    }

    val trackIntensity = when (raceState.phase) {
        SheepRanchRacePhase.IDLE -> 0.35f
        SheepRanchRacePhase.GATHERING -> lerp(0.35f, 1f, racePhaseProgress)
        SheepRanchRacePhase.COUNTDOWN,
        SheepRanchRacePhase.RACING,
        SheepRanchRacePhase.FINISHING -> 1f
        SheepRanchRacePhase.RETURNING -> lerp(1f, 0.35f, racePhaseProgress)
    }
    val dashScroll = if (raceState.phase == SheepRanchRacePhase.RACING) raceProgress else -1f
    val lightCountdownStep = if (raceState.phase == SheepRanchRacePhase.COUNTDOWN) countdownStep else -1
    val lightAlpha = if (raceState.phase == SheepRanchRacePhase.IDLE) 0.5f else 1f

    Box(modifier = modifier) {
        RaceTrackCanvas(
            geometry = geometry,
            intensity = trackIntensity,
            dashScrollFraction = dashScroll,
            modifier = Modifier.matchParentSize()
        )
        RaceCurbAndFence(
            geometry = geometry,
            intensity = trackIntensity,
            modifier = Modifier.matchParentSize()
        )
        RaceStartGrid(
            geometry = geometry,
            intensity = trackIntensity,
            modifier = Modifier.matchParentSize()
        )
        RaceTrafficLight(
            countdownStep = lightCountdownStep,
            modifier = Modifier.graphicsLayer {
                translationX = geometry.startLineX - 20.dp.toPx()
                translationY = geometry.trackTop - 46.dp.toPx()
                alpha = lightAlpha
            }
        )
        RaceBanner(
            wobbleTrigger = raceState.sessionId,
            thumpTrigger = raceState.phase == SheepRanchRacePhase.FINISHING,
            modifier = Modifier.graphicsLayer {
                translationX = geometry.trackRight - 78.dp.toPx()
                translationY = geometry.trackTop - 44.dp.toPx()
            }
        )
    }
}
