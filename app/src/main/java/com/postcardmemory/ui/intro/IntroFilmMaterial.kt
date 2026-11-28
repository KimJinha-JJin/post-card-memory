package com.postcardmemory.ui.intro

import android.graphics.Bitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import kotlin.math.floor

/**
 * 146일차 B안(아날로그 사진 필름) 재질. 사용자가 고른 HTML 목업
 * (docs/ai/mockups/intro-film-texture-day146.html의 paintFilmBase·paintB)의 색·투명도·비율을 그대로 옮긴다.
 *
 * - 모션과 분리: 이 파일은 한 칸의 **정지된 모습**만 그린다. 이송은 기존 Row의 graphicsLayer
 *   translationX가 칸째로 옮기므로 그레인·노출 흔적·먼지가 필름과 같은 좌표로 함께 움직인다.
 * - 캐시: 그레인 타일은 고정 시드로 composition당 한 번([rememberIntroFilmGrain]), 칸의 Path·Brush·먼지
 *   위치는 drawWithCache에서 크기가 바뀔 때만 만든다. 프레임마다 난수·텍스처를 만들지 않는다.
 * - 천공: 바탕 Path에서 원을 짝홀 규칙으로 빼서 실제로 뚫는다(뒤의 화면 배경이 보인다).
 * - 새 이미지 자원·사용자 사진·DB와 무관하다.
 */

/** 목업 mulberry32와 같은 결정적 난수. 같은 시드면 HTML과 같은 순서의 값을 낸다. */
internal class IntroFilmRandom(seed: Int) {
    private var state = seed

    fun next(): Float {
        state += 0x6D2B79F5
        var t = state
        t = (t xor (t ushr 15)) * (1 or t)
        t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
        return ((t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0).toFloat()
    }
}

/** 목업 grainTile(96px, 따뜻한 밝은 점/짙은 점, 알파 = amp × 편차²). */
internal const val INTRO_FILM_GRAIN_TILE_PX = 96
internal const val INTRO_FILM_BASE_GRAIN_AMP = 0.16f
internal const val INTRO_FILM_BASE_GRAIN_SEED = 23
internal const val INTRO_FILM_CELL_GRAIN_AMP = 0.08f
internal const val INTRO_FILM_CELL_GRAIN_SEED = 29

internal fun introFilmGrainTile(amp: Float, seed: Int): ImageBitmap {
    val n = INTRO_FILM_GRAIN_TILE_PX
    val random = IntroFilmRandom(seed)
    val pixels = IntArray(n * n) {
        val v = random.next()
        val dark = v < 0.5f
        val a = kotlin.math.abs(v - 0.5f) * 2f
        val alpha = Math.round(255f * amp * a * a)
        if (dark) {
            (alpha shl 24) or (70 shl 16) or (44 shl 8) or 18
        } else {
            (alpha shl 24) or (255 shl 16) or (244 shl 8) or 226
        }
    }
    return Bitmap.createBitmap(pixels, n, n, Bitmap.Config.ARGB_8888).asImageBitmap()
}

internal class IntroFilmGrain(val base: ImageBitmap, val cell: ImageBitmap)

/** 필름 바탕·칸 안 그레인 타일 두 장. 화면에서 `remember { introFilmGrain() }`로 한 번만 만든다. */
internal fun introFilmGrain(): IntroFilmGrain = IntroFilmGrain(
    base = introFilmGrainTile(INTRO_FILM_BASE_GRAIN_AMP, INTRO_FILM_BASE_GRAIN_SEED),
    cell = introFilmGrainTile(INTRO_FILM_CELL_GRAIN_AMP, INTRO_FILM_CELL_GRAIN_SEED)
)

// ---- 목업 B안 색·투명도(사용자 승인값, 임의 변경 금지) ----
private val FilmBaseEdge = Color(150, 82, 30).copy(alpha = 0.58f)
private val FilmBaseBand = Color(162, 92, 36).copy(alpha = 0.52f)
private val FilmBaseMiddle = Color(170, 100, 42).copy(alpha = 0.44f)
private val FilmHoleShade = Color(60, 32, 10).copy(alpha = 0.55f)
private val FilmHoleLight = Color(255, 236, 206).copy(alpha = 0.55f)
private val FilmEdgeDark = Color(78, 42, 14).copy(alpha = 0.62f)
private val FilmEdgeLight = Color(255, 226, 186).copy(alpha = 0.35f)
private val FilmCellDated = Color(255, 236, 206).copy(alpha = 0.26f)
private val FilmCellBlank = Color(255, 238, 210).copy(alpha = 0.30f)
private val FilmExposure = Color(255, 240, 214)
private const val FILM_EXPOSURE_ALPHA = 0.22f
private val FilmVignette = Color(120, 64, 20)
private const val FILM_VIGNETTE_ALPHA = 0.16f
private val FilmDustDark = Color(80, 44, 16).copy(alpha = 0.16f)
private val FilmDustLight = Color(255, 244, 222).copy(alpha = 0.45f)

/** 목업 두께 값(dp). */
private const val FILM_HOLE_EDGE_DP = 0.8f
private const val FILM_EDGE_DARK_DP = 0.75f
private const val FILM_EDGE_LIGHT_DP = 0.6f
private const val FILM_CELL_RADIUS_DP = 2f
private const val FILM_CELL_EDGE_INSET_DP = 0.5f

/** 목업 blur(0.6px) 대신 칸 노출 면 가장자리를 이 폭의 옅은 테로 부드럽게 한다(blur는 레이어 효과라 쓰지 않음). */
private const val FILM_CELL_SOFT_EDGE_DP = 1.2f

/** 칸마다 다른 노출 흔적·먼지 시드(목업 mulberry32(1000 + i × 37)). */
internal fun introFilmFrameSeed(index: Int): Int = 1000 + index * 37

/**
 * 필름 한 칸의 B안 재질. [index]는 계획의 칸 번호(노출 흔적·먼지의 고정 시드, 그레인 위치 기준),
 * [blank]는 날짜가 없는 칸. 칸 크기·띠·천공 간격은 기존 값 그대로 받는다.
 */
internal fun Modifier.introFilmFrameMaterial(
    index: Int,
    blank: Boolean,
    grain: IntroFilmGrain,
    band: Dp,
    holeSpacing: Dp,
    holeRadius: Dp,
    cellInset: Dp
): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val dp = density
    val bandPx = band.toPx()
    val spacing = holeSpacing.toPx()
    val radius = holeRadius.toPx()

    // 바탕 = 칸 사각형에서 천공 원을 뺀 모양(짝홀 규칙: 원끼리 겹치지 않고 사각형 안에 있다).
    val holeCenters = buildList {
        var x = spacing / 2f
        while (x < w) {
            add(Offset(x, bandPx / 2f))
            add(Offset(x, h - bandPx / 2f))
            x += spacing
        }
    }
    val basePath = Path().apply {
        fillType = PathFillType.EvenOdd
        addRect(Rect(0f, 0f, w, h))
        holeCenters.forEach { addOval(Rect(it, radius)) }
    }
    val baseBrush = Brush.verticalGradient(
        0f to FilmBaseEdge,
        0.08f to FilmBaseBand,
        0.5f to FilmBaseMiddle,
        0.92f to FilmBaseBand,
        1f to FilmBaseEdge,
        startY = 0f,
        endY = h
    )
    // 그레인은 필름 좌표 기준: 타일 1픽셀 = 1dp, 칸 번호만큼 밀어 칸 경계에서도 같은 결이 이어진다.
    fun grainBrush(tile: ImageBitmap): ShaderBrush {
        val shader = ImageShader(tile, TileMode.Repeated, TileMode.Repeated)
        shader.setLocalMatrix(android.graphics.Matrix().apply {
            setScale(dp, dp)
            postTranslate(-index * w, 0f)
        })
        return ShaderBrush(shader)
    }
    val baseGrain = grainBrush(grain.base)
    val cellGrain = grainBrush(grain.cell)

    val holeEdge = FILM_HOLE_EDGE_DP * dp
    val holeArcRadius = radius - holeEdge / 2f
    val holeStroke = Stroke(width = holeEdge)

    // 칸(노출 면)
    val inset = cellInset.toPx()
    val cell = Rect(inset, bandPx, w - inset, h - bandPx)
    val edgeInset = FILM_CELL_EDGE_INSET_DP * dp
    val cellFace = Rect(cell.left + edgeInset, cell.top + edgeInset, cell.right - edgeInset, cell.bottom - edgeInset)
    val corner = CornerRadius(FILM_CELL_RADIUS_DP * dp)
    val cellPath = Path().apply { addRoundRect(RoundRect(cellFace, corner)) }
    val cellColor = if (blank) FilmCellBlank else FilmCellDated
    val softEdge = Stroke(width = FILM_CELL_SOFT_EDGE_DP * dp)

    val random = IntroFilmRandom(introFilmFrameSeed(index))
    val exposureCenter = Offset(
        cell.left + cell.width * (0.3f + random.next() * 0.4f),
        cell.top + cell.height * (0.25f + random.next() * 0.35f)
    )
    val exposureRadius = cell.width * (0.55f + random.next() * 0.2f)
    val exposureBrush = Brush.radialGradient(
        (2f * dp / exposureRadius).coerceIn(0f, 1f) to FilmExposure.copy(alpha = FILM_EXPOSURE_ALPHA),
        1f to FilmExposure.copy(alpha = 0f),
        center = exposureCenter,
        radius = exposureRadius
    )
    val vignetteInner = cell.width * 0.3f
    val vignetteOuter = cell.height * 0.7f
    val vignetteBrush = Brush.radialGradient(
        (vignetteInner / vignetteOuter) to FilmVignette.copy(alpha = 0f),
        1f to FilmVignette.copy(alpha = FILM_VIGNETTE_ALPHA),
        center = cell.center,
        radius = vignetteOuter
    )
    val dustCount = 2 + floor(random.next() * 3f).toInt()
    val dust = List(dustCount) {
        val color = if (random.next() < 0.5f) FilmDustDark else FilmDustLight
        val center = Offset(cell.left + random.next() * cell.width, cell.top + random.next() * cell.height)
        val r = (0.35f + random.next() * 0.5f) * dp
        Triple(color, center, r)
    }

    val edgeDark = FILM_EDGE_DARK_DP * dp
    val edgeLight = FILM_EDGE_LIGHT_DP * dp

    onDrawBehind {
        // 1) 반투명 필름 바탕 + 그레인(천공은 비어 있다)
        drawPath(basePath, baseBrush)
        drawPath(basePath, baseGrain)

        // 2) 천공 단면: 위쪽 그늘, 아래쪽 밝은 선
        holeCenters.forEach { c ->
            val topLeft = Offset(c.x - holeArcRadius, c.y - holeArcRadius)
            val arcSize = Size(holeArcRadius * 2f, holeArcRadius * 2f)
            drawArc(FilmHoleShade, 189f, 162f, false, topLeft, arcSize, style = holeStroke)
            drawArc(FilmHoleLight, 18f, 144f, false, topLeft, arcSize, style = holeStroke)
        }

        // 3) 얇은 필름 가장자리(짙은 단면 + 안쪽 밝은 선)
        drawRect(FilmEdgeDark, Offset.Zero, Size(w, edgeDark))
        drawRect(FilmEdgeDark, Offset(0f, h - edgeDark), Size(w, edgeDark))
        drawRect(FilmEdgeLight, Offset(0f, edgeDark), Size(w, edgeLight))
        drawRect(FilmEdgeLight, Offset(0f, h - edgeDark - edgeLight), Size(w, edgeLight))

        // 4) 칸 노출 면: 옅은 면 + 부드러운 가장자리
        drawPath(cellPath, cellColor)
        drawPath(cellPath, cellColor.copy(alpha = cellColor.alpha * 0.5f), style = softEdge)

        // 5) 칸 안: 노출 흔적, 비네트, 옅은 그레인, 먼지
        clipPath(cellPath) {
            drawRect(exposureBrush, cell.topLeft, cell.size)
            drawRect(vignetteBrush, cell.topLeft, cell.size)
            drawRect(cellGrain, cell.topLeft, cell.size)
            dust.forEach { (color, center, r) -> drawCircle(color, r, center) }
        }
    }
}
