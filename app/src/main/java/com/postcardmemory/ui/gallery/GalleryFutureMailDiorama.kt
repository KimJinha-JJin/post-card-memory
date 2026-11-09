package com.postcardmemory.ui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.R
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.detail.SealType
import com.postcardmemory.ui.futuremail.FutureMailGroup
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * 127일차: 기억 밀도 그래프 아래 빈자리에 놓는 미래 우체통 종이 한 장(목업 B안).
 * 방문 달력 장과 같은 종이·가위 가장자리·접촉 그림자·테이프 위에 봉투와 원형 소인만
 * 둔다. 숫자 현황판이 아니라 그림이 상태를 먼저 말한다 — 소인의 진하기, 봉인 여부,
 * 봉투 뒤로 고개를 내민 엽서. 우체통 데이터(도착일·D-day·도착 여부)는 기존
 * [com.postcardmemory.ui.futuremail.buildFutureMailGroups] 결과만 읽는다. 진행률은
 * 실제 발송일이 아닌 capturedAt 기준이라(127일차 조사) 여기서는 쓰지 않는다.
 * 목업: docs/mockups/future-mailbox-diorama-day127.html (B안).
 */

/** 종이 한 장이 보여줄 상태. 우체통 묶음 목록에서만 만든다(새 저장 상태 없음). */
internal sealed interface FutureMailDioramaState {
    /** 보낸 엽서가 하나도 없다. */
    data object Empty : FutureMailDioramaState

    /** 도착한 편지는 없고, 가장 먼저 도착할 묶음을 보여준다. */
    data class Traveling(
        val deliverAtMillis: Long,
        val daysLeft: Long,
        /** 그림에 담긴 한 통 외에 함께 여행 중인 엽서 수. */
        val otherTravelingCount: Int
    ) : FutureMailDioramaState

    /** 도착한 편지가 있다 — 여행 중인 편지가 함께 있어도 도착이 먼저다. */
    data class Arrived(
        val deliverAtMillis: Long,
        val travelingCount: Int
    ) : FutureMailDioramaState
}

/**
 * 도착 우선 규칙. 도착한 묶음이 있으면 그중 가장 최근 도착일(소인에 찍을 날짜)을,
 * 없으면 가장 가까운 도착 예정 묶음을 고른다. 개수는 묶음 수가 아니라 엽서 수다.
 */
internal fun futureMailDioramaStateFor(groups: List<FutureMailGroup>): FutureMailDioramaState {
    val travelingGroups = groups.filterNot { it.arrived }
    val travelingCount = travelingGroups.sumOf { it.count }
    val latestArrived = groups.filter { it.arrived }.maxByOrNull { it.deliverAtMillis }
    if (latestArrived != null) {
        return FutureMailDioramaState.Arrived(
            deliverAtMillis = latestArrived.deliverAtMillis,
            travelingCount = travelingCount
        )
    }
    val next = travelingGroups.minByOrNull { it.deliverAtMillis }
        ?: return FutureMailDioramaState.Empty
    return FutureMailDioramaState.Traveling(
        deliverAtMillis = next.deliverAtMillis,
        daysLeft = next.daysLeft,
        otherTravelingCount = (travelingCount - 1).coerceAtLeast(0)
    )
}

/** 도착 예정일 문구 — 올해면 "12월 24일", 다른 해면 "2027년 1월 3일". */
internal fun futureMailDioramaDateText(
    deliverAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val date = Instant.ofEpochMilli(deliverAtMillis).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val pattern = if (date.year == today.year) "M월 d일" else "yyyy년 M월 d일"
    return DateTimeFormatter.ofPattern(pattern, Locale.KOREA).format(date)
}

/** 종이 위 세 줄과 화면 읽기용 설명. [accent]는 민트 D-day, [secondary]는 흐린 보조 문구. */
internal data class FutureMailDioramaTexts(
    val primary: String,
    val accent: String?,
    val secondary: String?,
    val accessibility: String
)

internal fun futureMailDioramaTexts(
    state: FutureMailDioramaState,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): FutureMailDioramaTexts = when (state) {
    FutureMailDioramaState.Empty -> FutureMailDioramaTexts(
        primary = "미래의 나에게 보내 볼까?",
        accent = null,
        secondary = "엽서 편집 ⋮ › 미래 우체통",
        accessibility = "미래 우체통. 아직 보낸 엽서가 없어. " +
            "엽서 편집 화면의 더보기 메뉴에서 미래로 보낼 수 있어."
    )

    is FutureMailDioramaState.Traveling -> {
        val date = futureMailDioramaDateText(state.deliverAtMillis, nowMillis, zone)
        val others = state.otherTravelingCount
        FutureMailDioramaTexts(
            primary = "${date}에 도착해",
            accent = "D-${state.daysLeft}",
            secondary = if (others > 0) "외 ${others}통" else null,
            accessibility = "미래 우체통. ${date}에 도착하는 편지가 여행 중이야. " +
                "${state.daysLeft}일 남았어." +
                if (others > 0) " 다른 ${others}통도 여행 중이야." else ""
        )
    }

    is FutureMailDioramaState.Arrived -> {
        val traveling = state.travelingCount
        FutureMailDioramaTexts(
            primary = "편지가 도착했어",
            accent = null,
            secondary = if (traveling > 0) "${traveling}통은 아직 여행 중" else "눌러서 열어보기",
            accessibility = "미래 우체통. 편지가 도착했어." +
                if (traveling > 0) " ${traveling}통은 아직 여행 중이야." else ""
        )
    }
}

/** 그래프 아래 끝과 종이 사이 틈. */
internal val FUTURE_MAIL_DIORAMA_TOP_GAP = 24.dp

/** 기본 글꼴에서의 종이 높이(목업 B안). 큰 글꼴이면 [futureMailDioramaSheetHeight]가 늘린다. */
internal val FUTURE_MAIL_DIORAMA_MIN_SHEET_HEIGHT = 92.dp

/** 종이 위 글 묶음의 줄 높이 합(sp) — 라벨 1줄 + 본문 최대 2줄 + D-day/보조 1줄. */
private const val FUTURE_MAIL_DIORAMA_TEXT_BLOCK_SP = 15f + 18f * 2f + 16f
private val FUTURE_MAIL_DIORAMA_TEXT_VERTICAL_PADDING = 12.dp

/** 닫힌 손잡이 위에 더 두는 여유. 손 그림이 손잡이 칸보다 조금 위로 비칠 수 있다. */
private const val FUTURE_MAIL_DIORAMA_HANDLE_CLEARANCE_DP = 4f

/**
 * 갤러리 페이지 본문의 바닥은 Scaffold 아래 inset 위로 24dp 더 올라와 있고([GalleryDensityPage]
 * bottom padding), 퀵셀렉트 손 컨테이너의 바닥은 그 inset 바로 위다. 둘의 차이.
 */
private const val GALLERY_PAGE_BOTTOM_PADDING_DP = 24f

/**
 * 닫힌 퀵셀렉트 손잡이의 윗변이 손 컨테이너 바닥에서 몇 dp 위에 있는지. 손잡이를
 * 실제로 놓는 계산([quickSelectPivot]·[quickSelectAnimatedBlockCenter])을 그대로 쓴다.
 * 펼침 간격이 클수록 손잡이가 높이 오므로 기본값은 최대 간격(가장 보수적)이다.
 */
internal fun quickSelectClosedHandleTopFromBottomDp(
    spacingDeg: Float = QUICK_SELECT_MAX_SPACING_DEG,
    handleHeightDp: Float = GalleryQuickSelectHandleHeight.value
): Float {
    // 바닥 기준 거리는 컨테이너 크기와 무관하다 — 계산용 임의 크기를 넣고 바닥에서 뺀다.
    val containerWidth = 400f
    val containerHeight = 1000f
    val pivot = quickSelectPivot(containerWidth, containerHeight, spacingDeg)
    val center = quickSelectAnimatedBlockCenter(
        pivot,
        slide = 0f,
        angleDeg = quickSelectHandAngle(0, spacingDeg, spread = 0f, tilt = 0f)
    )
    return containerHeight - center.y + handleHeightDp / 2f
}

/** 페이지 본문 바닥에서 손잡이를 피해 비워 둘 높이. */
internal fun futureMailDioramaBottomReserveDp(): Float =
    (quickSelectClosedHandleTopFromBottomDp() - GALLERY_PAGE_BOTTOM_PADDING_DP)
        .coerceAtLeast(0f) + FUTURE_MAIL_DIORAMA_HANDLE_CLEARANCE_DP

/**
 * 그래프 아래 남은 높이 [availableHeightDp] 안에 틈 + 종이 + 손잡이 여유가 다 들어가는지.
 * 들어가지 않으면 종이를 아예 그리지 않는다 — 줄이거나 그래프를 밀어 올리지 않는다.
 */
internal fun futureMailDioramaFits(
    availableHeightDp: Float,
    sheetHeightDp: Float,
    bottomReserveDp: Float = futureMailDioramaBottomReserveDp()
): Boolean =
    availableHeightDp >= FUTURE_MAIL_DIORAMA_TOP_GAP.value + sheetHeightDp + bottomReserveDp

@Composable
private fun futureMailDioramaSheetHeight(): Dp {
    val textBlock = with(LocalDensity.current) { FUTURE_MAIL_DIORAMA_TEXT_BLOCK_SP.sp.toDp() }
    return maxOf(FUTURE_MAIL_DIORAMA_MIN_SHEET_HEIGHT, textBlock + FUTURE_MAIL_DIORAMA_TEXT_VERTICAL_PADDING * 2)
}

/**
 * 기억 밀도 페이지에서 그래프 아래 남은 칸 전체를 받는다([modifier]에 weight). 칸 높이가
 * 모자라면 아무것도 구성하지 않아 접근성 트리에도 남지 않는다.
 */
@Composable
internal fun GalleryFutureMailDioramaSlot(
    groups: List<FutureMailGroup>,
    onOpenFutureMailbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetHeight = futureMailDioramaSheetHeight()
    BoxWithConstraints(modifier = modifier) {
        if (!futureMailDioramaFits(maxHeight.value, sheetHeight.value)) return@BoxWithConstraints
        // 도착·D-day는 groups를 만들 때 이미 같은 자정 기준으로 계산됐다. 여기서 읽는 시각은
        // "올해인지"만 가르는 데 쓰므로 groups가 바뀔 때(자정 신호 포함) 다시 읽으면 충분하다.
        val nowMillis = remember(groups) { System.currentTimeMillis() }
        val state = remember(groups) { futureMailDioramaStateFor(groups) }
        GalleryFutureMailDiorama(
            state = state,
            texts = remember(state, nowMillis) { futureMailDioramaTexts(state, nowMillis) },
            sheetHeight = sheetHeight,
            onClick = onOpenFutureMailbox,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = FUTURE_MAIL_DIORAMA_TOP_GAP)
        )
    }
}

private const val FUTURE_MAIL_DIORAMA_PAPER_EDGE_SEED = 127_000
private const val FUTURE_MAIL_DIORAMA_TAPE_SEED = 127_200

private val DioramaEnvelopeWidth = 84.dp
private val DioramaEnvelopeHeight = 48.dp // 원본 252×144 비율
private val DioramaArtStart = 16.dp
private val DioramaTextStart = 124.dp
private val DioramaPostmarkSize = 44.dp
private const val DIORAMA_POSTMARK_DATE_TEXT_RATIO = 0.32f

/** 소인 진하기 — 여행 중은 아직 찍히기 전처럼 흐리게, 도착은 진하게, 빈 우체통은 자리만. */
private const val POSTMARK_ALPHA_TRAVELING = 0.5f
private const val POSTMARK_ALPHA_ARRIVED = 0.9f
private const val POSTMARK_ALPHA_EMPTY = 0.3f
private const val EMPTY_ENVELOPE_ALPHA = 0.45f

private val PeekPhotoColors = listOf(Color(0xFF9FC7C0), Color(0xFFD9C9A6), Color(0xFFB98F6A))

@Composable
private fun GalleryFutureMailDiorama(
    state: FutureMailDioramaState,
    texts: FutureMailDioramaTexts,
    sheetHeight: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 달력 종이 bitmap. 기억 밀도 페이지가 구성돼 있는 동안만 들고 있는다.
    val paper = ImageBitmap.imageResource(R.drawable.visit_calendar_paper)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clearAndSetSemantics {
                contentDescription = texts.accessibility
                role = Role.Button
                onClick(label = "미래 우체통 열기") { onClick(); true }
            }
            .graphicsLayer {
                val press = if (pressed) 1f else 0f
                scaleX = 1f - 0.012f * press
                scaleY = 1f - 0.012f * press
                translationY = 1.dp.toPx() * press
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .height(sheetHeight)
            .calendarPaperSheet(paper)
    ) {
        DioramaArt(
            state = state,
            modifier = Modifier
                .padding(start = DioramaArtStart)
                .align(Alignment.CenterStart)
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = DioramaTextStart, end = 28.dp)
        ) {
            Text(
                text = "미래 우체통",
                fontSize = 11.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = texts.primary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (texts.accent != null || texts.secondary != null) {
                Row {
                    if (texts.accent != null) {
                        Text(
                            text = texts.accent,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VisitFillColorToday,
                            maxLines = 1
                        )
                    }
                    if (texts.secondary != null) {
                        Text(
                            text = if (texts.accent != null) " · ${texts.secondary}" else texts.secondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = InkSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Text(
            text = "›",
            fontSize = 18.sp,
            color = InkSecondary,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
        )
    }
}

/**
 * 봉투 + 원형 소인(+ 도착이면 뒤로 고개를 내민 엽서). 봉투는 달력 연월 봉투 그림,
 * 소인은 엽서·인트로와 같은 원형 소인 렌더러다.
 */
@Composable
private fun DioramaArt(
    state: FutureMailDioramaState,
    modifier: Modifier = Modifier
) {
    val arrived = state is FutureMailDioramaState.Arrived
    val empty = state is FutureMailDioramaState.Empty
    val postmarkDate = when (state) {
        is FutureMailDioramaState.Arrived -> state.deliverAtMillis
        is FutureMailDioramaState.Traveling -> state.deliverAtMillis
        FutureMailDioramaState.Empty -> null
    }
    val postmarkAlpha = when {
        arrived -> POSTMARK_ALPHA_ARRIVED
        empty -> POSTMARK_ALPHA_EMPTY
        else -> POSTMARK_ALPHA_TRAVELING
    }

    // 봉투 위 9dp(엽서)와 소인이 봉투 밖으로 비어져 나오는 만큼을 미리 잡아 둔다.
    Box(modifier = modifier.size(DioramaEnvelopeWidth + 22.dp, DioramaEnvelopeHeight + 26.dp)) {
        if (arrived) {
            PeekingPostcard(
                modifier = Modifier
                    .offset(x = DioramaEnvelopeWidth * 0.46f, y = 2.dp)
                    .size(40.dp, 27.dp)
                    .rotate(-9f)
            )
        }

        Box(
            modifier = Modifier
                .offset(y = 13.dp)
                .size(DioramaEnvelopeWidth, DioramaEnvelopeHeight)
                .rotate(-3f)
                .graphicsLayer { alpha = if (empty) EMPTY_ENVELOPE_ALPHA else 1f }
        ) {
            Image(
                painter = painterResource(R.drawable.visit_calendar_envelope_base),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            )
            if (state is FutureMailDioramaState.Traveling) {
                // 봉인: 덮개 꼭짓점(원본 약 50%, 61%)에 민트 봉인 하나.
                Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                    val center = Offset(size.width * 0.5f, size.height * 0.61f)
                    drawCircle(VisitFillColor.copy(alpha = 0.92f), radius = 4.6.dp.toPx(), center = center)
                    drawCircle(
                        PaperSurface.copy(alpha = 0.7f),
                        radius = 2.9.dp.toPx(),
                        center = center,
                        style = Stroke(width = 0.5.dp.toPx())
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset(
                    x = DioramaEnvelopeWidth - 4.dp - DioramaPostmarkSize / 2,
                    y = 13.dp + 6.dp - DioramaPostmarkSize / 2
                )
                .size(DioramaPostmarkSize)
                .rotate(-12f)
                .graphicsLayer { alpha = postmarkAlpha }
        ) {
            SealPreviewContent(
                type = SealType.CIRCLE_POSTMARK,
                color = InkSecondary,
                capturedAtMillis = postmarkDate,
                dateTextRatio = DIORAMA_POSTMARK_DATE_TEXT_RATIO,
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            )
        }
    }
}

/** 봉투 뒤로 고개를 내민 엽서 한 장 — "열 수 있어"의 그림 신호. 사진은 실제 엽서가 아니다. */
@Composable
private fun PeekingPostcard(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(PaperSurface)
        drawRect(InkPrimary.copy(alpha = 0.35f), style = Stroke(width = 0.6.dp.toPx()))
        val inset = size.width * 0.08f
        drawRect(
            brush = Brush.linearGradient(
                colors = PeekPhotoColors,
                start = Offset(inset, inset),
                end = Offset(size.width - inset, size.height - inset)
            ),
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2, size.height - inset * 2)
        )
    }
}

/**
 * 방문 달력 장([VisitCalendarMonthPage])과 같은 종이 한 장: cover-crop 종이, 가위 가장자리,
 * 아주 옅은 접촉 그림자, 윗변 가운데 아이보리 테이프. seed는 이 종이 전용으로 고정한다.
 */
private fun Modifier.calendarPaperSheet(paper: ImageBitmap): Modifier = drawWithCache {
    val dstWidth = size.width.toInt().coerceAtLeast(1)
    val dstHeight = size.height.toInt().coerceAtLeast(1)
    val scale = maxOf(dstWidth.toFloat() / paper.width, dstHeight.toFloat() / paper.height)
    val srcWidth = (dstWidth / scale).toInt().coerceIn(1, paper.width)
    val srcHeight = (dstHeight / scale).toInt().coerceIn(1, paper.height)
    val srcOffset = IntOffset((paper.width - srcWidth) / 2, (paper.height - srcHeight) / 2)
    val paperBrush = ShaderBrush(
        ImageShader(paper).apply {
            setLocalMatrix(
                android.graphics.Matrix().apply {
                    setTranslate(-srcOffset.x.toFloat(), -srcOffset.y.toFloat())
                    postScale(dstWidth.toFloat() / srcWidth, dstHeight.toFloat() / srcHeight)
                }
            )
        }
    )
    val outline = Path().apply {
        visitCalendarPaperEdgeOutline(
            widthDp = size.width / density,
            heightDp = size.height / density,
            seed = FUTURE_MAIL_DIORAMA_PAPER_EDGE_SEED
        ).forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x * density, point.y * density)
            else lineTo(point.x * density, point.y * density)
        }
        close()
    }
    val tape = visitCalendarTapePiece(FUTURE_MAIL_DIORAMA_TAPE_SEED)
    val tapePath = visitCalendarTapePath(tape, density)
    onDrawBehind {
        VISIT_CALENDAR_PAPER_SHADOW_LAYERS.forEach { (dx, dy, alpha) ->
            translate(left = dx * density, top = dy * density) {
                drawPath(outline, color = VisitCalendarPaperShadowColor, alpha = alpha)
            }
        }
        drawPath(outline, brush = paperBrush)
        drawVisitCalendarTape(tape, tapePath)
    }
}
