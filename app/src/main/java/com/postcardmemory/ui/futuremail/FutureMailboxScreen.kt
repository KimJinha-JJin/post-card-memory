package com.postcardmemory.ui.futuremail

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.postcardmemory.R
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.detail.SealType
import com.postcardmemory.ui.gallery.PeekingPostcard
import com.postcardmemory.ui.gallery.VisitFillColor
import com.postcardmemory.ui.gallery.VisitFillColorToday
import com.postcardmemory.ui.gallery.calendarPaperSheet
import com.postcardmemory.ui.gallery.futureMailDioramaDateText
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SurfaceGray
import java.time.Instant
import java.time.ZoneId

/**
 * 미래 우체통. 갤러리와 나란한 세 번째 "보기 방식"이 아니라, 갤러리 밖의
 * 별도 화면이다 — 배송 중인 엽서의 날짜·건수만 보여주고, 사진·문구·스티커·
 * 도장 등 내용은 절대 노출하지 않는다(그룹 데이터 자체가 postcardId만
 * 들고 있어 이 화면이 실수로라도 내용을 그릴 방법이 없다).
 *
 * 129일차(A안 · 우편 서류함): 127일차 디오라마와 같은 달력 종이·가위 가장자리·테이프 위에
 * "도착했어 / 여행 중" 장을 한 장씩 두고, 도착일 묶음 하나를 장부의 한 줄로 정돈한다.
 * 봉투 뒤 엽서 그림은 실제 엽서가 아닌 추상 색면이다. 진행률은 실제 발송일이 아니라 엽서를
 * 만든 날 기준이라 숫자(%)로 보여주지 않고, 점선 위 점 하나와 소인 진하기로만 은근히 둔다.
 * 목업: docs/mockups/future-mailbox-inside-day129.html (A안).
 */
@Composable
fun FutureMailboxScreen(
    onNavigateBack: () -> Unit,
    viewModel: FutureMailboxViewModel = hiltViewModel()
) {
    val groups by viewModel.groups.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.openedMessages.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 종이 한 장이 화면 폭 정도라 원본(1122×1402)의 절반이면 충분하다(약 1.6MB, 원본 decode는
    // 약 6.3MB). 이 화면이 구성돼 있는 동안만 들고 있는다.
    val resources = context.resources
    val paper = remember(resources) { loadFutureMailboxPaper(resources) }

    Scaffold(
        containerColor = GalleryPaperWhite,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GalleryPaperWhite)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = InkPrimary
                        )
                    }

                    Text(
                        text = "미래 우체통",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                HorizontalDivider(color = SurfaceGray, thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        val arrived = groups.filter { it.arrived }
        val traveling = groups.filterNot { it.arrived }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp)
        ) {
            item(key = "summary") {
                FutureMailboxSummary(arrived = arrived, traveling = traveling)
            }

            if (groups.isEmpty()) {
                item(key = "empty") {
                    FutureMailboxEmptySheet(paper = paper)
                }
            }

            if (arrived.isNotEmpty()) {
                item(key = "arrived") {
                    FutureMailboxSection(
                        label = "도착했어",
                        paper = paper,
                        edgeSeed = FUTURE_MAILBOX_ARRIVED_EDGE_SEED,
                        tapeSeed = FUTURE_MAILBOX_ARRIVED_TAPE_SEED
                    ) {
                        arrived.forEachIndexed { index, group ->
                            FutureMailRow(
                                group = group,
                                showDivider = index > 0,
                                onOpen = { viewModel.openArrivedGroup(group) }
                            )
                        }
                    }
                }
            }

            if (traveling.isNotEmpty()) {
                item(key = "traveling") {
                    FutureMailboxSection(
                        label = "여행 중",
                        paper = paper,
                        edgeSeed = FUTURE_MAILBOX_TRAVELING_EDGE_SEED,
                        tapeSeed = FUTURE_MAILBOX_TRAVELING_TAPE_SEED
                    ) {
                        traveling.forEachIndexed { index, group ->
                            FutureMailRow(
                                group = group,
                                showDivider = index > 0,
                                onOpen = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 원본 종이를 절반으로 줄여 불러온다. 이 화면만 쓰고, 화면을 떠나면 함께 놓는다. */
private fun loadFutureMailboxPaper(resources: Resources): ImageBitmap =
    BitmapFactory.decodeResource(
        resources,
        R.drawable.visit_calendar_paper,
        BitmapFactory.Options().apply { inSampleSize = FUTURE_MAILBOX_PAPER_SAMPLE }
    ).asImageBitmap()

/** "도착 2통 · 여행 중 4통" — 묶음 수가 아니라 엽서(통) 수다. */
@Composable
private fun FutureMailboxSummary(
    arrived: List<FutureMailGroup>,
    traveling: List<FutureMailGroup>
) {
    val arrivedCount = arrived.sumOf { it.count }
    val travelingCount = traveling.sumOf { it.count }
    val text = buildAnnotatedString {
        if (arrivedCount == 0 && travelingCount == 0) {
            append("편지를 미래로 보내면 여기 보관돼.")
        } else {
            val strong = SpanStyle(color = InkPrimary, fontWeight = FontWeight.SemiBold)
            if (arrivedCount > 0) {
                append("도착 ")
                withStyle(strong) { append("${arrivedCount}통") }
            }
            if (arrivedCount > 0 && travelingCount > 0) append(" · ")
            if (travelingCount > 0) {
                append("여행 중 ")
                withStyle(strong) { append("${travelingCount}통") }
            }
        }
    }
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = InkSecondary,
        modifier = Modifier.padding(top = 2.dp, bottom = 18.dp)
    )
}

/** 섹션 이름 + 가는 선, 그 아래 종이 한 장(테이프·가위 가장자리·옅은 그림자). */
@Composable
private fun FutureMailboxSection(
    label: String,
    paper: ImageBitmap,
    edgeSeed: Int,
    tapeSeed: Int,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 26.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = InkSecondary
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(PaperDivider)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .calendarPaperSheet(paper, edgeSeed = edgeSeed, tapeSeed = tapeSeed)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            content()
        }
    }
}

/**
 * 장부의 한 줄: 봉투 · 날짜(+ 보조 문구) · 오른쪽 열(도착이면 열어보기, 여행 중이면 D-day).
 * 글꼴이 크거나 화면이 좁아 한 줄에 다 못 들어가면 오른쪽 열을 아랫줄 오른쪽으로 내린다.
 */
@Composable
private fun FutureMailRow(
    group: FutureMailGroup,
    showDivider: Boolean,
    onOpen: () -> Unit
) {
    val dateText = remember(group.deliverAtMillis, group.daysLeft) {
        futureMailboxDateText(group)
    }
    // 화면에서 시각을 다시 읽지 않는다 — 도착 여부·진행률과 같은 기준으로
    // 그룹이 만들어질 때 함께 계산된 값을 그대로 쓴다(자정 경계 일치).
    val daysLeft = group.daysLeft
    val count = group.count

    val title: String
    val subtitle: String
    if (group.arrived) {
        title = if (daysLeft == 0L) "오늘 도착" else "$dateText 도착"
        subtitle = if (count > 1) "${count}통 · 같은 날 도착" else "${count}통"
    } else {
        title = dateText
        subtitle = if (count > 1) "도착 예정 · ${count}통 함께" else "도착 예정"
    }

    val rowSemantics = if (group.arrived) {
        Modifier.semantics(mergeDescendants = true) {}
    } else {
        Modifier.clearAndSetSemantics {
            contentDescription = "$dateText 도착 예정, 도착까지 ${daysLeft}일, ${count}통"
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showDivider) Modifier.dashedTopDivider() else Modifier)
            .then(rowSemantics)
            .heightIn(min = 72.dp)
            .padding(vertical = 12.dp)
    ) {
        // 목업 A안과 같은 기준: 날짜 칸이 최소 118sp, 오른쪽 열이 약 84sp는 있어야 한 줄로 둔다.
        val stacked = with(LocalDensity.current) {
            maxWidth < FutureMailArtWidth + 24.dp + 118.sp.toDp() + 84.sp.toDp()
        }
        val trailing: @Composable () -> Unit = {
            if (group.arrived) {
                FutureMailInkButton(
                    text = "열어보기",
                    accessibilityLabel = "$dateText 도착 편지 ${count}통 열어보기",
                    onClick = onOpen
                )
            } else {
                FutureMailDaysLeft(daysLeft = daysLeft, count = count)
            }
        }

        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FutureMailEnvelopeArt(group = group)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary,
                        style = PhraseBreakStyle
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = InkSecondary,
                        style = PhraseBreakStyle,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (!group.arrived) {
                        FutureMailRouteLine(
                            progressPercent = group.progressPercent,
                            modifier = Modifier.padding(top = 7.dp)
                        )
                    }
                }
                if (!stacked) {
                    Spacer(Modifier.width(12.dp))
                    trailing()
                }
            }
            if (stacked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    trailing()
                }
            }
        }
    }
}

/** 오른쪽 열 — 민트 D-day가 주인공, 그 아래 통 수. */
@Composable
private fun FutureMailDaysLeft(daysLeft: Long, count: Int) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = "D-$daysLeft",
            fontSize = 20.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            color = VisitFillColorToday,
            maxLines = 1
        )
        Text(
            text = "${count}통",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = InkSecondary,
            maxLines = 1
        )
    }
}

/** 종이 위 얇은 잉크 테두리 버튼(금색 채움 Material 버튼 대신). 터치 영역은 48dp 이상. */
@Composable
private fun FutureMailInkButton(
    text: String,
    accessibilityLabel: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(FutureMailButtonCorner)
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .drawBehind {
                // 종이 위에 놓인 듯한 1dp 아래 그림자 한 줄
                drawRoundRect(
                    color = FutureMailButtonShadow,
                    topLeft = Offset(0f, 1.dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(FutureMailButtonCorner.toPx())
                )
            }
            .background(PaperSurface, shape)
            .border(1.dp, InkPrimary.copy(alpha = 0.55f), shape)
            .semantics { contentDescription = accessibilityLabel }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary,
            maxLines = 1
        )
    }
}

/**
 * 봉투 그림(58×48dp): 같은 날 묶음이면 뒤로 봉투를 최대 2장 더 겹치고, 도착이면 봉인 대신
 * 봉투 뒤로 엽서가 고개를 내민다. 원형 소인은 도착이면 진하게, 여행 중이면 흐리게.
 */
@Composable
private fun FutureMailEnvelopeArt(group: FutureMailGroup) {
    val arrived = group.arrived
    val layers = group.count.coerceIn(1, 3)
    val postmarkAlpha = if (arrived) {
        POSTMARK_ALPHA_ARRIVED
    } else {
        POSTMARK_ALPHA_TRAVELING_MIN +
            POSTMARK_ALPHA_TRAVELING_RANGE * group.progressPercent.coerceIn(0, 100) / 100f
    }

    Box(modifier = Modifier.size(FutureMailArtWidth, FutureMailArtHeight)) {
        for (layer in layers - 1 downTo 1) {
            FutureMailEnvelope(
                sealed = !arrived,
                modifier = Modifier
                    .offset(x = EnvelopeLeft + StackStep * layer, y = EnvelopeTop - StackStep * layer)
                    .rotate(-3f + 3f * layer)
                    .alpha(0.6f + 0.15f * (2 - layer))
            )
        }
        if (arrived) {
            PeekingPostcard(
                modifier = Modifier
                    .offset(x = EnvelopeLeft + EnvelopeWidth * 0.42f, y = EnvelopeTop - EnvelopeWidth * 0.24f)
                    .size(EnvelopeWidth * 0.5f, EnvelopeWidth * 0.33f)
                    .rotate(-9f)
            )
        }
        FutureMailEnvelope(
            sealed = !arrived,
            modifier = Modifier
                .offset(x = EnvelopeLeft, y = EnvelopeTop)
                .rotate(-3f)
        )
        SealPreviewContent(
            type = SealType.CIRCLE_POSTMARK,
            color = InkSecondary,
            modifier = Modifier
                .offset(
                    x = EnvelopeLeft + EnvelopeWidth - 2.dp - PostmarkSize / 2,
                    y = PostmarkCenterY - PostmarkSize / 2
                )
                .size(PostmarkSize)
                .rotate(-12f)
                .alpha(postmarkAlpha)
        )
    }
}

/** 달력 연월 봉투 그림 한 장(+ 여행 중이면 덮개 꼭짓점의 민트 봉인). */
@Composable
private fun FutureMailEnvelope(sealed: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(EnvelopeWidth, EnvelopeHeight)) {
        Image(
            painter = painterResource(R.drawable.visit_calendar_envelope_base),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().fillMaxHeight()
        )
        if (sealed) {
            Canvas(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                val center = Offset(size.width * 0.5f, size.height * 0.61f)
                drawCircle(VisitFillColor.copy(alpha = 0.92f), radius = 3.dp.toPx(), center = center)
                drawCircle(
                    PaperSurface.copy(alpha = 0.7f),
                    radius = 1.9.dp.toPx(),
                    center = center,
                    style = Stroke(width = 0.5.dp.toPx())
                )
            }
        }
    }
}

/** 점선 위 민트 점 하나 + 끝의 빈 동그라미. 숫자 없이 "어디쯤"만 보여준다. */
@Composable
private fun FutureMailRouteLine(progressPercent: Int, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .widthIn(max = 150.dp)
            .fillMaxWidth()
            .height(10.dp)
    ) {
        val y = size.height / 2f
        val endRadius = 4.5.dp.toPx()
        val endCenterX = size.width - endRadius - 0.5.dp.toPx()
        drawLine(
            color = InkSecondary.copy(alpha = 0.45f),
            start = Offset(0f, y),
            end = Offset(endCenterX - endRadius, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 2.dp.toPx()))
        )
        drawCircle(
            color = InkSecondary.copy(alpha = 0.55f),
            radius = endRadius,
            center = Offset(endCenterX, y),
            style = Stroke(width = 1.dp.toPx())
        )
        // 도착 동그라미와 겹치지 않도록 92%에서 멈춘다(목업과 같음).
        val x = size.width * (progressPercent.coerceIn(0, ROUTE_DOT_MAX_PERCENT) / 100f)
        drawCircle(PaperSurface.copy(alpha = 0.8f), radius = 5.5.dp.toPx(), center = Offset(x, y))
        drawCircle(VisitFillColor, radius = 4.dp.toPx(), center = Offset(x, y))
    }
}

/** 빈 우체통: 종이 위 흐린 봉투와 빈 소인, 보내는 길 안내(버튼 없음). */
@Composable
private fun FutureMailboxEmptySheet(paper: ImageBitmap) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .calendarPaperSheet(
                paper,
                edgeSeed = FUTURE_MAILBOX_EMPTY_EDGE_SEED,
                tapeSeed = FUTURE_MAILBOX_EMPTY_TAPE_SEED
            )
            .padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 24.dp)
    ) {
        Box(modifier = Modifier.size(96.dp, 70.dp)) {
            Image(
                painter = painterResource(R.drawable.visit_calendar_envelope_base),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = 18.dp, y = 20.dp)
                    .size(56.dp, 32.dp)
                    .rotate(-3f)
                    .alpha(0.45f)
            )
            SealPreviewContent(
                type = SealType.CIRCLE_POSTMARK,
                color = InkSecondary,
                modifier = Modifier
                    .offset(x = 59.dp, y = 9.dp)
                    .size(30.dp)
                    .rotate(-12f)
                    .alpha(0.3f)
            )
        }
        Text(
            text = "아직 떠난 엽서가 없어",
            fontSize = 15.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = InkPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = "엽서 편집 ⋮ › 미래 우체통에서 보낼 수 있어",
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = InkSecondary,
            textAlign = TextAlign.Center,
            style = PhraseBreakStyle,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** 장부 줄 사이의 얇은 점선(윗변). */
private fun Modifier.dashedTopDivider(): Modifier = drawBehind {
    drawLine(
        color = InkSecondary.copy(alpha = 0.32f),
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
    )
}

/**
 * 디오라마와 같은 날짜 문구("12월 24일" / 다른 해면 "2027년 1월 1일"). 올해 판정은 시계를
 * 다시 읽지 않고, 그룹을 만들 때의 오늘(도착일 − D-day)로 한다.
 */
internal fun futureMailboxDateText(
    group: FutureMailGroup,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val deliverDate = Instant.ofEpochMilli(group.deliverAtMillis).atZone(zone).toLocalDate()
    val todayMillis = deliverDate
        .minusDays(group.daysLeft)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
    return futureMailDioramaDateText(group.deliverAtMillis, todayMillis, zone)
}

/** 한국어를 글자 단위가 아니라 어절 단위로 줄바꿈("11월 9일 / 도착", "11월 9 / 일" 방지). */
private val PhraseBreakStyle = TextStyle(lineBreak = LineBreak.Heading)

private const val FUTURE_MAILBOX_PAPER_SAMPLE = 2

private const val FUTURE_MAILBOX_ARRIVED_EDGE_SEED = 129_000
private const val FUTURE_MAILBOX_ARRIVED_TAPE_SEED = 129_100
private const val FUTURE_MAILBOX_TRAVELING_EDGE_SEED = 129_010
private const val FUTURE_MAILBOX_TRAVELING_TAPE_SEED = 129_110
private const val FUTURE_MAILBOX_EMPTY_EDGE_SEED = 129_020
private const val FUTURE_MAILBOX_EMPTY_TAPE_SEED = 129_120

private val FutureMailArtWidth = 58.dp
private val FutureMailArtHeight = 48.dp
private val EnvelopeWidth = 40.dp
private val EnvelopeHeight = 23.dp // 원본 252×144 비율
private val EnvelopeLeft = 4.dp
private val EnvelopeTop = 16.dp
private val StackStep = 4.dp
private val PostmarkSize = 20.dp
private val PostmarkCenterY = 18.dp
private val FutureMailButtonCorner = 4.dp
private val FutureMailButtonShadow = Color(0x2E3B3226)
private const val ROUTE_DOT_MAX_PERCENT = 92

/** 소인 진하기 — 도착은 진하게, 여행 중은 흐리게 시작해 도착이 가까울수록 조금씩 진해진다. */
private const val POSTMARK_ALPHA_ARRIVED = 0.9f
private const val POSTMARK_ALPHA_TRAVELING_MIN = 0.25f
private const val POSTMARK_ALPHA_TRAVELING_RANGE = 0.45f
