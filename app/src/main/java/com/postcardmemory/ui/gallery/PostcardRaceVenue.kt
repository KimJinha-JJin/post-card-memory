package com.postcardmemory.ui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal const val RACE_TRACK_HEIGHT_FRACTION = 0.42f
internal const val RACE_TRACK_SIDE_INSET_FRACTION = 0.09f

/**
 * 트랙 맨 아래 차선(3번)이 화면 끝에 바짝 붙어 카드 하단 날짜 글씨가 잘리지 않도록
 * 트랙 바닥과 화면 끝 사이에 남겨두는 여백. Venue와 Overlay가 반드시 같은 값을 쓴다.
 */
internal const val RACE_TRACK_BOTTOM_MARGIN_DP = 30f

/**
 * 쫑쫑컵 모드에서 카드(패독·관중)가 돌아다닐 수 있는 세로 영역의 윗선.
 * 무대 기본 바닥 시작선(58~60%)보다 훨씬 위로 끌어올려서, 화면 하단에만
 * 몰려 보이지 않고 화면 대부분을 쓰는 작은 축제 공간처럼 보이게 한다.
 * [GalleryPlayStage]의 구역별 배회 범위 계산도 이 값을 그대로 쓴다.
 */
internal const val RACE_ZONE_TOP_FRACTION = 0.20f

private val RaceTrackBase = Color(0xFFB9A489)
private val RaceTrackLane = Color(0xFFEFE6D2)
private val RaceTrackCheckerDark = Color(0xFF8A7A63)
private val RaceLightHousing = Color(0xFF4A4038)
private val RaceLightDim = Color(0xFF8A8074)
private val RaceStartLightRed = Color(0xFFB8564A)
private const val RACE_START_LIGHT_ROWS = 3
private val RaceCurbCream = Color(0xFFF3E8D4)
private val RaceCurbBurgundy = Color(0xFF8C4A3E)
private val RaceFenceColor = Color(0xFF7C7266)
private val RaceGroundSage = Color(0xFFDDE8D5)

/** 관중석을 오려낸 바랜 신문지 색과, 그 위에 찍힌 낡은 먹 잉크 색. */
private val RaceNewsprintPaper = Color(0xFFEEE7D7)
private val RaceNewsprintInk = Color(0xFF4F463D)

/** 관중석 단의 벤치 판자와 단 앞면(바랜 인쇄 색). 차양은 연석과 같은 크림·버건디를 쓴다. */
private val RaceStandPlank = Color(0xFFA88D6C)
private val RaceStandRiser = Color(0xFFE4D9C3)
private const val RACE_STAND_ROWS = 2
private const val RACE_STAND_PAPER_SEED = 23

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
 * 무대 기본 잔디(58% 지점부터 시작)보다 훨씬 위쪽[RACE_ZONE_TOP_FRACTION]부터 이어지는
 * 쫑쫑컵 전용 바닥. 기존 [RaceGrassBackground]는 건드리지 않고 그 위에 자연스럽게 이어붙는
 * 별도 레이어라서, 동글이 쉼터 화면에는 전혀 영향이 없다. 위쪽은 옅게 시작해 잔디와
 * 만나는 지점에서 자연스럽게 진해지도록 그라데이션을 준다.
 */
@Composable
private fun RaceGroundExtension(
    stageWidthPx: Float,
    stageHeightPx: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val groundTop = stageHeightPx * RACE_ZONE_TOP_FRACTION
        val groundBottom = stageHeightPx * 0.58f
        if (groundBottom <= groundTop) return@Canvas
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    RaceGroundSage.copy(alpha = 0f),
                    RaceGroundSage.copy(alpha = 0.5f)
                ),
                startY = groundTop,
                endY = groundBottom
            ),
            topLeft = Offset(0f, groundTop),
            size = Size(stageWidthPx, groundBottom - groundTop)
        )
    }
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

/**
 * 관중 엽서가 앉는 자리 뒤의 작은 관중석. 관중 배치와 같은 [raceSpectatorRowTop]으로
 * 두 층 벤치를 그려서, 레이스 중 관중 사진이 각 층 벤치에 걸터앉아 보인다. 잡지에서
 * 오려낸 신문지 한 장([raceNewsprintClipping]) 위에 줄무늬 차양, 벤치 판자, 망점이 찍힌
 * 단 앞면만 그린다. 트랙처럼 평상시엔 옅게 있다가 레이스 때 선명해진다.
 */
@Composable
private fun RaceGrandstand(
    geometry: RaceTrackGeometry,
    cardWidthPx: Float,
    cardHeightPx: Float,
    intensity: Float
) {
    with(LocalDensity.current) {
        val inset = RaceSpectatorPhotoInset.toPx()
        val photo = cardWidthPx * RACE_SPECTATOR_CARD_SCALE - inset * 2f
        val awningHeight = 9.dp.toPx()
        val left = geometry.trackLeft - 6.dp.toPx()
        val right = geometry.trackRight + 6.dp.toPx()
        // 트랙 위 연석(5dp)과 펜스(10dp) 바로 위에서 끝난다.
        val bottom = geometry.trackTop - 15.dp.toPx()
        val top = raceSpectatorRowTop(RACE_STAND_ROWS - 1, geometry.trackTop, cardHeightPx) +
            inset - awningHeight - 6.dp.toPx()
        if (bottom <= top || right <= left) return
        // 각 층 벤치 윗면: 그 줄 관중 사진 아랫변보다 조금 위 — 사진이 벤치에 걸터앉은 모습.
        val benchTops = List(RACE_STAND_ROWS) { row ->
            raceSpectatorRowTop(row, geometry.trackTop, cardHeightPx) + inset + photo - 6.dp.toPx() - top
        }

        Spacer(
            modifier = Modifier
                .graphicsLayer {
                    translationX = left
                    translationY = top
                    alpha = intensity
                }
                .size(width = (right - left).toDp(), height = (bottom - top).toDp())
                .raceNewsprintClipping(RACE_STAND_PAPER_SEED)
                .drawWithCache {
                    val dp = density
                    val w = size.width
                    val h = size.height
                    val plankHeight = 3.5f * dp
                    val rng = Random(RACE_STAND_PAPER_SEED)

                    val dotStep = 3f * dp
                    val riserDots = buildList {
                        benchTops.forEachIndexed { row, benchTop ->
                            val riserTop = benchTop + plankHeight
                            val riserBottom = if (row == 0) h else benchTops[row - 1]
                            var y = riserTop + dotStep / 2f
                            var line = 0
                            while (y < riserBottom) {
                                var x = if (line % 2 == 0) dotStep / 2f else dotStep
                                while (x < w) {
                                    val depth = ((y - riserTop) / (riserBottom - riserTop)).coerceIn(0f, 1f)
                                    if (rng.nextFloat() < 0.15f + depth * 0.4f) add(Offset(x, y))
                                    x += dotStep
                                }
                                y += dotStep
                                line++
                            }
                        }
                    }

                    onDrawBehind {
                        // 차양: 크림·버건디 줄무늬 띠와 아래로 늘어진 둥근 술.
                        val stripe = 8f * dp
                        val bandHeight = awningHeight - 3f * dp
                        var x = 0f
                        var i = 0
                        while (x < w) {
                            val color = (if (i % 2 == 0) RaceCurbCream else RaceCurbBurgundy).copy(alpha = 0.8f)
                            val stripeWidth = minOf(stripe, w - x)
                            drawRect(color = color, topLeft = Offset(x, 0f), size = Size(stripeWidth, bandHeight))
                            drawCircle(
                                color = color,
                                radius = stripeWidth / 2f,
                                center = Offset(x + stripeWidth / 2f, bandHeight)
                            )
                            x += stripe
                            i++
                        }

                        // 양 끝 기둥.
                        listOf(2f * dp, w - 2f * dp).forEach { postX ->
                            drawLine(
                                color = RaceFenceColor.copy(alpha = 0.55f),
                                start = Offset(postX, bandHeight),
                                end = Offset(postX, h),
                                strokeWidth = 2f * dp
                            )
                        }

                        benchTops.forEachIndexed { row, benchTop ->
                            val riserTop = benchTop + plankHeight
                            val riserBottom = if (row == 0) h else benchTops[row - 1]
                            if (riserBottom > riserTop) {
                                drawRect(
                                    color = RaceStandRiser.copy(alpha = 0.85f),
                                    topLeft = Offset(4f * dp, riserTop),
                                    size = Size(w - 8f * dp, riserBottom - riserTop)
                                )
                            }
                            drawRect(
                                color = RaceStandPlank.copy(alpha = 0.9f),
                                topLeft = Offset(4f * dp, benchTop),
                                size = Size(w - 8f * dp, plankHeight)
                            )
                            // 판자 아래 그늘 한 줄 — 벤치가 단 앞면보다 튀어나와 보이게.
                            drawLine(
                                color = RaceNewsprintInk.copy(alpha = 0.22f),
                                start = Offset(4f * dp, riserTop + 0.5f * dp),
                                end = Offset(w - 4f * dp, riserTop + 0.5f * dp),
                                strokeWidth = 1f * dp
                            )
                        }
                        drawPoints(
                            points = riserDots,
                            pointMode = PointMode.Points,
                            color = RaceNewsprintInk.copy(alpha = 0.1f),
                            strokeWidth = 1f * dp,
                            cap = StrokeCap.Round
                        )
                    }
                }
        )
    }
}

/**
 * 잡지에서 가위로 오려낸 종이 한 장 바탕. 모서리와 가장자리가 자로 잰 듯하지 않게
 * 미세하게 흔들리고, 바랜 신문지 색 위에 아주 옅은 망점(한쪽 구석으로 갈수록 조금
 * 진해짐)과 종이 결만 얹는다. 같은 [seed]면 늘 같은 모양이라 프레임마다 떨리지 않는다.
 */
private fun Modifier.raceNewsprintClipping(seed: Int): Modifier = drawWithCache {
    val dp = density
    val w = size.width
    val h = size.height
    val rng = Random(seed)
    fun wobble(amount: Float) = (rng.nextFloat() - 0.5f) * amount * dp

    val corners = listOf(
        Offset(wobble(1.2f), wobble(1.2f)),
        Offset(w + wobble(1.2f), wobble(1.2f)),
        Offset(w + wobble(1.2f), h + wobble(1.2f)),
        Offset(wobble(1.2f), h + wobble(1.2f))
    )
    val outline = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        for (side in 0 until 4) {
            val from = corners[side]
            val to = corners[(side + 1) % 4]
            val length = (to - from).getDistance().coerceAtLeast(1f)
            val normalX = -(to.y - from.y) / length
            val normalY = (to.x - from.x) / length
            val segments = (length / (7f * dp)).toInt().coerceAtLeast(1)
            for (s in 1 until segments) {
                val t = s / segments.toFloat()
                val cut = wobble(0.9f)
                lineTo(
                    lerp(from.x, to.x, t) + normalX * cut,
                    lerp(from.y, to.y, t) + normalY * cut
                )
            }
            lineTo(to.x, to.y)
        }
        close()
    }

    val dotStep = 3.4f * dp
    val halftone = buildList {
        var row = 0
        var y = dotStep / 2f
        while (y < h) {
            var x = if (row % 2 == 0) dotStep / 2f else dotStep
            while (x < w) {
                val corner = ((x / w + y / h) / 2f).coerceIn(0f, 1f)
                if (rng.nextFloat() < 0.12f + corner * corner * 0.5f) add(Offset(x, y))
                x += dotStep
            }
            y += dotStep
            row++
        }
    }

    val fibers = Path().apply {
        repeat(((w * h) / (90f * dp * dp)).toInt().coerceIn(4, 40)) {
            val sx = rng.nextFloat() * w
            val sy = rng.nextFloat() * h
            val len = (2f + rng.nextFloat() * 4f) * dp
            val angle = (rng.nextFloat() - 0.5f) * 0.8f
            moveTo(sx, sy)
            lineTo(sx + cos(angle) * len, sy + sin(angle) * len)
        }
    }

    onDrawBehind {
        // 바닥에서 종이 한 장 두께만큼만 떠 보이게 하는 아주 옅은 자국.
        translate(top = 0.9f * dp) {
            drawPath(path = outline, color = RaceNewsprintInk.copy(alpha = 0.07f))
        }
        drawPath(path = outline, color = RaceNewsprintPaper)
        clipPath(outline) {
            drawPoints(
                points = halftone,
                pointMode = PointMode.Points,
                color = RaceNewsprintInk.copy(alpha = 0.09f),
                strokeWidth = 1f * dp,
                cap = StrokeCap.Round
            )
            drawPath(
                path = fibers,
                color = RaceNewsprintInk.copy(alpha = 0.05f),
                style = Stroke(width = 0.5f * dp, cap = StrokeCap.Round)
            )
        }
        // 오린 단면의 얇은 종이 결 — 테두리 선이 아니라 가장자리가 살짝 어두운 정도.
        drawPath(
            path = outline,
            color = RaceNewsprintInk.copy(alpha = 0.16f),
            style = Stroke(width = 0.6f * dp)
        )
    }
}

/**
 * F1 출발 신호등처럼 무대 상단에 매달린 5칸 × 3줄 신호등. 평상시엔 꺼진 채 자리만 지키고,
 * COUNTDOWN 동안 [litColumns]만큼 왼쪽부터 한 칸(세로 한 줄)씩 빨간 등이 켜진다.
 * 다섯 칸이 다 켜진 뒤 RACING이 되면 한꺼번에 꺼지며 출발한다.
 */
@Composable
private fun RaceStartLightGantry(
    litColumns: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(width = 85.dp, height = 46.dp)
    ) {
        val podWidth = 13.dp.toPx()
        val podGap = 5.dp.toPx()
        val beamHeight = 4.dp.toPx()
        val podTop = beamHeight + 2.dp.toPx()
        val podHeight = size.height - podTop
        val rowHeight = podHeight / RACE_START_LIGHT_ROWS
        val lampRadius = minOf(podWidth, rowHeight) * 0.34f

        // 무대 위쪽으로 이어지는 두 가닥 매달림 줄과 가로 들보.
        listOf(size.width * 0.15f, size.width * 0.85f).forEach { wireX ->
            drawLine(
                color = RaceFenceColor.copy(alpha = 0.6f),
                start = Offset(wireX, 0f),
                end = Offset(wireX, -size.height),
                strokeWidth = 1.2.dp.toPx()
            )
        }
        drawRect(
            color = RaceLightHousing,
            topLeft = Offset(-3.dp.toPx(), 0f),
            size = Size(size.width + 6.dp.toPx(), beamHeight)
        )

        for (column in 0 until RACE_START_LIGHT_COLUMNS) {
            val podLeft = column * (podWidth + podGap)
            drawRoundRect(
                color = RaceLightHousing,
                topLeft = Offset(podLeft, podTop),
                size = Size(podWidth, podHeight),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
            val isLit = column < litColumns
            for (row in 0 until RACE_START_LIGHT_ROWS) {
                drawCircle(
                    color = if (isLit) RaceStartLightRed else RaceLightDim.copy(alpha = 0.5f),
                    radius = lampRadius,
                    center = Offset(podLeft + podWidth / 2f, podTop + rowHeight * (row + 0.5f))
                )
            }
        }
    }
}

/**
 * 쫑쫑컵 모드에 들어온 순간부터 항상 떠 있는 작은 경기장 배경.
 * 레이스가 시작되지 않았어도(IDLE) 옅게 표시되고, 진행 단계에 따라 선명해진다.
 * 동글이 쉼터(raceEnabled = false)에서는 아예 호출되지 않는다.
 */
@Composable
fun PostcardRaceVenue(
    raceState: PostcardRaceState,
    racePhaseProgress: Float,
    raceProgress: Float,
    countdownStep: Int,
    stageWidthPx: Float,
    stageHeightPx: Float,
    cardWidthPx: Float,
    cardHeightPx: Float,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val geometry = remember(stageWidthPx, stageHeightPx, cardWidthPx) {
        computeRaceTrackGeometry(
            stageWidthPx = stageWidthPx,
            stageHeightPx = stageHeightPx,
            cardWidthPx = cardWidthPx,
            bottomMarginPx = with(density) { RACE_TRACK_BOTTOM_MARGIN_DP.dp.toPx() }
        )
    }

    val trackIntensity = when (raceState.phase) {
        PostcardRacePhase.IDLE -> 0.35f
        PostcardRacePhase.GATHERING -> lerp(0.35f, 1f, racePhaseProgress)
        PostcardRacePhase.COUNTDOWN,
        PostcardRacePhase.RACING,
        PostcardRacePhase.FINISHING -> 1f
        PostcardRacePhase.RETURNING -> lerp(1f, 0.35f, racePhaseProgress)
    }
    val dashScroll = if (raceState.phase == PostcardRacePhase.RACING) raceProgress else -1f
    val litStartColumns = if (raceState.phase == PostcardRacePhase.COUNTDOWN) countdownStep else 0
    // 결승·복귀 동안엔 같은 자리에 우승자 전광판이 걸리므로 숨었다가, 전광판이 꺼질 무렵 돌아온다.
    val lightAlpha = when (raceState.phase) {
        PostcardRacePhase.IDLE -> 0.5f
        PostcardRacePhase.FINISHING -> 0f
        PostcardRacePhase.RETURNING -> lerp(0f, 0.5f, ((racePhaseProgress - 0.7f) / 0.3f).coerceIn(0f, 1f))
        else -> 1f
    }

    Box(modifier = modifier) {
        RaceGroundExtension(
            stageWidthPx = stageWidthPx,
            stageHeightPx = stageHeightPx,
            modifier = Modifier.matchParentSize()
        )
        RaceGrandstand(
            geometry = geometry,
            cardWidthPx = cardWidthPx,
            cardHeightPx = cardHeightPx,
            intensity = trackIntensity
        )
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
        RaceStartLightGantry(
            litColumns = litStartColumns,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = stageHeightPx * RACE_RESULT_TOP_FRACTION
                    alpha = lightAlpha
                }
        )
    }
}
