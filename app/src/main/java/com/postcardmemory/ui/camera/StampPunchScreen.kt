package com.postcardmemory.ui.camera

import android.graphics.RectF
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.postcardmemory.ui.gallery.rememberSystemReducedMotion
import com.postcardmemory.ui.theme.ScreenBackgroundGray
import com.postcardmemory.utils.PostcardRenderSpec
import java.io.File
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/*
 * 138일차 C안 목업(`docs/ai/mockups/stamp-punch-motion-mockup.html`)의 장면 기하.
 * 목업은 CSS 3D(rotateX 40°)였고 여기서는 같은 기울기를 정사영 2D로 근사한다:
 * 평면 y는 cos(40°)로 눌리고, 높이 z는 화면 위쪽으로 sin(40°)만큼 올라가며
 * 원근(목업 perspective 420)은 높이에 따른 작은 확대로만 남긴다.
 */
private const val VIEW_W = 140f
private const val PLANE_H = 110f
private const val STAGE_H = 104f
private const val SHEET_X = 20f
private const val SHEET_Y = 19f
private const val SHEET_W = 100f
private const val SHEET_H = 72f
private const val STAMP_X = 50f
private const val STAMP_Y = 35f
private const val STAMP_S = 40f
private const val PLATE_PAD = 7f
private const val RIM = STAMP_S * 18f / 1000f // exporter STAMP_BORDER_WIDTH 18 / 우표 1000
private const val PERSPECTIVE = 420f
private val TILT_COS = cos(Math.toRadians(40.0)).toFloat()
private val TILT_SIN = sin(Math.toRadians(40.0)).toFloat()
private val PLANE_TOP = (STAGE_H - PLANE_H * TILT_COS) / 2f + STAGE_H * 0.03f

/*
 * 140일차 후속(공간감): 매트는 책상 위에 놓인 두께 있는 판이다. 평면 안쪽으로 들여 그려
 * 둘레에 책상이 보이게 하고, 앞쪽 두께 면과 책상 위 접지 그림자를 둔다. 장면 전체에는
 * 먼 쪽이 아주 조금 좁아지는 원근만 건다. C2 윤곽·시간표·장면 구성은 그대로다.
 */
private const val MAT_INSET = 5f
private const val MAT_THICK = 2.2f
private const val MAT_RADIUS = 1.6f
private const val FAR_EDGE_INSET = 0.035f // 먼 쪽(위) 가장자리가 한쪽당 폭의 3.5% 좁아짐

private val DeskLight = ScreenBackgroundGray
private val DeskShade = Color(0xFFE6DAC6)
private val DeskShadow = Color(0xFF3A3024)
private val MatEdge = Color(0xFF515B4E)
private val MatColor = Color(0xFF6D7869)
private val MatLine = Color(0xFF626D5F)
private val MatMinor = Color(0xFF687365)
private val PaperColor = Color(0xFFF3EEE3)
private val PaperWall = Color(0xFFD3C9B6)
private val PaperWall2 = Color(0xFFC5BAA5)
private val StampPlaceholder = Color(0xFFC8D4CF)

/** 장면에 쓰는 경로. 구멍·조각·펀칭날·펀칭기 창이 모두 같은 C2 윤곽 하나에서 나온다. */
private class PunchPaths {
    val stamp: Path = PostcardRenderSpec
        .createPinkingPath(RectF(STAMP_X, STAMP_Y, STAMP_X + STAMP_S, STAMP_Y + STAMP_S))
        .asComposePath()
    val sheet: Path = Path().apply {
        addRect(Rect(SHEET_X, SHEET_Y, SHEET_X + SHEET_W, SHEET_Y + SHEET_H))
    }
    val sheetHole: Path = Path.combine(PathOperation.Difference, sheet, stamp)
    val plate: Path = Path.combine(
        PathOperation.Difference,
        Path().apply {
            addRect(
                Rect(
                    STAMP_X - PLATE_PAD, STAMP_Y - PLATE_PAD,
                    STAMP_X + STAMP_S + PLATE_PAD, STAMP_Y + STAMP_S + PLATE_PAD
                )
            )
        },
        stamp
    )
}

/**
 * 저장 성공 뒤 갤러리로 돌아가기 전에 보여 주는 C안 우표 펀칭.
 * 고정된 종이 위에 방금 저장한 우표 사진이 인쇄돼 있고, 펀칭기가 내려와 C2 천공선으로
 * 잘라 낸 뒤 빠지면 잘린 조각만 살짝 들려 정착한다. 끝나면 [onFinished]를 한 번 부른다.
 * 진행 시각은 저장 가능한 상태라 화면이 다시 만들어져도 처음부터 다시 재생하지 않는다.
 */
@Composable
internal fun StampPunchScreen(
    imagePath: String,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val reducedMotion = rememberSystemReducedMotion()
    val latestOnFinished by rememberUpdatedState(onFinished)

    // 우표 면은 화면에서 한 변 약 40/140 폭이라 그 크기로만 decode한다.
    val stampPx = with(density) {
        (LocalConfiguration.current.screenWidthDp.dp.toPx() * STAMP_S / VIEW_W).toInt().coerceAtLeast(64)
    }
    val painter = rememberAsyncImagePainter(
        model = remember(imagePath, stampPx) {
            ImageRequest.Builder(context)
                .data(File(imagePath))
                .size(stampPx)
                .build()
        }
    )
    val paths = remember { PunchPaths() }
    var elapsedMs by rememberSaveable(imagePath) { mutableFloatStateOf(0f) }

    LaunchedEffect(imagePath, reducedMotion) {
        // 우표 사진이 준비되기 전에 자르기 시작하지 않는다. 로컬 파일이라 보통 즉시 끝나고,
        // 실패·지연이어도 저장은 이미 끝났으니 잠깐만 기다리고 빈 우표 면으로 진행한다.
        withTimeoutOrNull(400) {
            snapshotFlow { painter.state }.first {
                it is AsyncImagePainter.State.Success || it is AsyncImagePainter.State.Error
            }
        }
        val total = StampPunchTimeline.TOTAL_MS.toFloat()
        if (reducedMotion) {
            // 움직임 줄이기: 중간 보간 없이 잘려 들린 끝 장면만 짧게 보여 주고 바로 복귀한다.
            elapsedMs = total
            delay(StampPunchTimeline.REDUCED_MOTION_HOLD_MS.toLong())
        } else if (elapsedMs < total) {
            val clock = Animatable(elapsedMs)
            clock.animateTo(
                targetValue = total,
                animationSpec = tween(
                    durationMillis = (total - elapsedMs).toInt().coerceAtLeast(1),
                    easing = LinearEasing
                )
            ) {
                elapsedMs = value
            }
        }
        latestOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 책상 표면: 기존 종이 배경색에서 가장자리로 아주 조금 어두워지는 은은한 면.
            .background(
                Brush.radialGradient(
                    colors = listOf(DeskLight, DeskLight, DeskShade),
                    radius = LocalConfiguration.current.screenHeightDp.dp.value *
                        density.density * 0.62f
                )
            )
            .semantics { contentDescription = "우표를 잘라 편집 화면으로 넘기는 중" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .aspectRatio(VIEW_W / STAGE_H)
        ) {
            drawPunchStage(
                state = stampPunchStateAt(elapsedMs),
                paths = paths,
                photo = painter.takeIf { it.state is AsyncImagePainter.State.Success }
            )
        }
    }
}

/** 높이 [z]의 평평한 층을 화면에 놓는 변환 안에서 [block]을 그린다(좌표는 목업 뷰박스 단위). */
private inline fun DrawScope.atHeight(z: Float, block: DrawScope.() -> Unit) {
    val u = size.width / VIEW_W
    val zoom = PERSPECTIVE / (PERSPECTIVE - z * TILT_COS)
    val pivot = Offset(STAMP_X + STAMP_S / 2f, (STAMP_Y + STAMP_S / 2f) * TILT_COS)
    withTransform({
        scale(u, u, pivot = Offset.Zero)
        translate(0f, PLANE_TOP - z * TILT_SIN)
        scale(zoom, zoom, pivot = pivot)
        scale(1f, TILT_COS, pivot = Offset.Zero)
    }) { block() }
}

/** 흐림 필터 대신 얇은 층을 겹쳐 가장자리를 부드럽게 한 그림자(minSdk 26 하드웨어 캔버스 호환). */
private fun DrawScope.softShadow(path: Path, color: Color, alpha: Float, blur: Float) {
    if (alpha <= 0.001f) return
    drawPath(path, color.copy(alpha = alpha * 0.7f))
    drawPath(path, color.copy(alpha = alpha * 0.18f), style = Stroke(width = blur * 2f, join = StrokeJoin.Round))
    drawPath(path, color.copy(alpha = alpha * 0.12f), style = Stroke(width = blur * 3.6f, join = StrokeJoin.Round))
}

/** 우표 인쇄면: C2로 클립한 사진 + 같은 윤곽 두 배 굵기 선을 안쪽만 남긴 테두리. */
private fun DrawScope.drawStampFace(paths: PunchPaths, photo: Painter?) {
    clipPath(paths.stamp) {
        if (photo != null) {
            translate(STAMP_X, STAMP_Y) {
                with(photo) { draw(Size(STAMP_S, STAMP_S)) }
            }
        } else {
            drawRect(StampPlaceholder, Offset(STAMP_X, STAMP_Y), Size(STAMP_S, STAMP_S))
        }
        drawPath(paths.stamp, PaperColor, style = Stroke(width = RIM * 2f, join = StrokeJoin.Round))
    }
}

private fun clamp01(v: Float) = if (v < 0f) 0f else if (v > 1f) 1f else v

/** 먼 쪽(위) 가장자리를 살짝 좁히는 원근. 장면 전체(매트·종이·펀칭기·그림자)에 같이 걸린다. */
private fun DrawScope.farEdgePerspective(): android.graphics.Matrix {
    val w = size.width
    val h = size.height
    val inset = w * FAR_EDGE_INSET
    return android.graphics.Matrix().apply {
        setPolyToPoly(
            floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h), 0,
            floatArrayOf(inset, 0f, w - inset, 0f, w, h, 0f, h), 0,
            4
        )
    }
}

private fun DrawScope.drawPunchStage(state: StampPunchState, paths: PunchPaths, photo: Painter?) {
    val canvas = drawContext.canvas.nativeCanvas
    canvas.save()
    canvas.concat(farEdgePerspective())
    try {
        drawPunchScene(state, paths, photo)
    } finally {
        canvas.restore()
    }
}

private fun DrawScope.drawPunchScene(state: StampPunchState, paths: PunchPaths, photo: Painter?) {
    val paperTop = StampPunchTimeline.PAPER_TOP
    val matRect = Rect(MAT_INSET, MAT_INSET, VIEW_W - MAT_INSET, PLANE_H - MAT_INSET)
    val matPath = Path().apply { addRoundRect(RoundRect(matRect, CornerRadius(MAT_RADIUS))) }

    // 책상 위 접지 그림자와 매트 앞쪽 두께 면: 매트 바닥은 책상(높이 -MAT_THICK)에 놓여 있다.
    atHeight(-MAT_THICK) {
        translate(1.2f, 2.4f) { softShadow(matPath, DeskShadow, 0.26f, 3f) }
        translate(.3f, .6f) { softShadow(matPath, DeskShadow, 0.22f, .6f) }
        drawPath(matPath, MatEdge)
    }

    // 바닥판(커팅 매트)과 구멍 속 바닥에 드리운 절단면 그늘.
    atHeight(0f) {
        drawPath(matPath, MatColor)
        clipPath(matPath) {
            var i = 0
            while (i <= VIEW_W.toInt()) {
                val major = i % 10 == 0
                drawLine(if (major) MatLine else MatMinor, Offset(i.toFloat(), 0f), Offset(i.toFloat(), PLANE_H), if (major) .3f else .18f)
                i += 5
            }
            var j = 0
            while (j <= PLANE_H.toInt()) {
                val major = j % 10 == 0
                drawLine(if (major) MatLine else MatMinor, Offset(0f, j.toFloat()), Offset(VIEW_W, j.toFloat()), if (major) .3f else .18f)
                j += 5
            }
        }
        if (state.cut) {
            clipPath(paths.stamp) {
                translate(.5f, .7f) {
                    drawPath(paths.stamp, Color(0x6B141612), style = Stroke(width = 2.2f, join = StrokeJoin.Round))
                    drawPath(paths.stamp, Color(0x24141612), style = Stroke(width = 3.4f, join = StrokeJoin.Round))
                }
            }
        }
    }

    // 들린 조각의 그림자 중 구멍 속 바닥에 떨어지는 부분.
    val h = state.lift
    val hm = h + paperTop
    if (h > 0f) {
        atHeight(0.05f) {
            translate(hm * .16f, hm * .26f) {
                softShadow(paths.stamp, Color(0xFF141611), 0.3f * clamp01(h / 2.5f) * (1f - hm / 60f), .25f + hm * .11f)
            }
        }
    }

    // 종이가 매트 위에 얹힌 높이 차이: 종이 바깥으로 짧게 번지는 접촉 그림자.
    atHeight(0.02f) {
        translate(.35f, .6f) { softShadow(paths.sheet, Color(0xFF2A2116), 0.2f, .9f) }
    }

    // 고정된 종이: 두께(벽 두 장) + 윗면. 절단 전엔 한 장, 절단 후엔 같은 C2 윤곽 구멍.
    val sheetPath = if (state.cut) paths.sheetHole else paths.sheet
    atHeight(paperTop * .33f) { drawPath(sheetPath, PaperWall2) }
    atHeight(paperTop * .66f) { drawPath(sheetPath, PaperWall) }
    atHeight(paperTop) {
        drawPath(sheetPath, PaperColor)
        if (!state.cut) drawStampFace(paths, photo)
    }

    // 종이 위: 들린 조각 그림자, 펀칭기 그림자, 절단선.
    val db = state.dieBottom
    val dieVisible = db < StampPunchTimeline.DIE_EXIT_HEIGHT - 1f
    atHeight(paperTop + 0.04f) {
        if (h > 0f) {
            clipPath(paths.sheetHole) {
                translate(h * .16f, h * .26f) {
                    softShadow(paths.stamp, Color(0xFF2A2116), 0.24f * clamp01(h / 2.5f) * (1f - h / 60f), .25f + h * .11f)
                }
            }
        }
        if (dieVisible) {
            // 펀칭기가 종이에 가까울수록 그림자가 짙고 또렷해지고, 날이 닿을 때는 날 아래에
            // 눌린 그늘이 생긴다(멀 때는 옅고 넓게 퍼짐).
            val above = max(0f, db + 3.6f - paperTop)
            val near = 1f - clamp01(above / 40f)
            translate(above * .16f, above * .26f) {
                softShadow(paths.plate, Color(0xFF1D1A15), 0.36f * near * sqrt(near), .2f + above * .12f)
            }
            val bladeGap = max(0f, db - paperTop + StampPunchTimeline.BITE)
            val contact = 1f - clamp01(bladeGap / 4f)
            if (contact > 0f) {
                drawPath(
                    paths.stamp,
                    Color(0xFF1D1A15).copy(alpha = 0.22f * contact),
                    style = Stroke(width = .5f + (1f - contact) * 1.2f, join = StrokeJoin.Round)
                )
            }
        }
        if (state.seam > 0f) {
            drawPath(paths.stamp, Color(0xFF3A4237).copy(alpha = state.seam * .85f), style = Stroke(width = .6f))
        }
    }

    // 잘린 조각: 종이와 같은 두께, 같은 윤곽. 절단 전에는 존재하지 않는다.
    if (state.cut) {
        atHeight(paperTop * .33f + h) { drawPath(paths.stamp, PaperWall2) }
        atHeight(paperTop * .66f + h) { drawPath(paths.stamp, PaperWall) }
        atHeight(paperTop + h) {
            drawPath(paths.stamp, PaperColor)
            drawStampFace(paths, photo)
        }
    }

    // 펀칭기: C2 날(아래→위 6겹) + C2 창이 뚫린 판(4겹).
    if (dieVisible) {
        for (i in 0 until 6) {
            val col = when (i) {
                0 -> Color(0xFFDCDEDF)
                1 -> Color(0xFFB5BABD)
                else -> Color(0xFF8E9497)
            }
            atHeight(db + i * .6f) {
                drawPath(paths.stamp, col, style = Stroke(width = .7f, join = StrokeJoin.Round))
            }
        }
        for (i in 0 until 4) {
            atHeight(db + 3.6f + i * .6f) {
                drawPath(paths.plate, if (i < 3) Color(0xFF474C4F) else Color(0xFF6F7579))
                if (i == 3) drawPath(paths.plate, Color(0xFF8A9094), style = Stroke(width = .35f))
            }
        }
    }
}
