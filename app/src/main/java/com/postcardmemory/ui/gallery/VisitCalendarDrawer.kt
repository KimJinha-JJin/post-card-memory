package com.postcardmemory.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.detail.labelStickerTextColorArgbFor
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.SealInkNavy
import com.postcardmemory.ui.theme.SealInkRed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

// 방문한 날짜마다 작게 찍히는 얼굴 도장. 짧은 후보만 써서 좁은 날짜 cell 폭을 넘기지 않는다.
// 공휴일 데이터가 없어(73일차 조사) 요일 기본색만 적용하고 공휴일 우선순위는 보류한다.
// "^_^"는 다른 후보와 선 느낌·폭이 달라 박스 안에서 혼자 따로 노는 인상이 있어 제거했다(4차 QA 피드백).
internal val VISIT_DAY_KAOMOJI = listOf("•ᴗ•", "˙ᵕ˙", "ᵔᴗᵔ", "ᵔ.ᵔ")

internal fun visitDayKaomoji(date: LocalDate): String =
    VISIT_DAY_KAOMOJI[Math.floorMod(date.toEpochDay(), VISIT_DAY_KAOMOJI.size.toLong()).toInt()]

// totalVisitDays를 그대로 읽어 보여주는 조용한 문구. streak/보상 의미는 담지 않는다.
internal fun visitCountLabel(totalVisitDays: Int?): String =
    totalVisitDays?.let { "오늘까지 ${it}번 만났어요~!" } ?: ""

// 차분한 웜톤 잉크 색을 재사용한다(SealInkNavy/SealInkRed) — 새 원색을 추가하지 않는다.
private val WeekendSaturday = SealInkNavy
private val WeekendSunday = SealInkRed
private val VisitCalendarOrnamentColor = InkSecondary.copy(alpha = 0.29f)
private val VisitCalendarBottomOrnamentColor = InkSecondary.copy(alpha = 0.39f)

/** 공휴일 데이터가 없어 요일 기본색만 적용한다. 대체공휴일 포함 지원은 STOP 상태다. */
internal fun visitDateColor(date: LocalDate): Color = when (date.dayOfWeek) {
    DayOfWeek.SATURDAY -> WeekendSaturday
    DayOfWeek.SUNDAY -> WeekendSunday
    else -> InkSecondary
}

// 방문일 채움 색. 지시된 정확한 값(#16A7A1)을 그대로 쓴다 — 요일색과 달리 무디게 낮추지 않는다.
internal val VisitFillColor = Color(0xFF16A7A1)
private const val VisitFillArgb = 0xFF16A7A1L

// 오늘 + 실제 방문 history가 있는 날만 쓰는 한 톤 진한 청록. 75일차 목업 3안(A/B/C) 중
// 사용자가 B(또렷하게)를 확정했다. 같은 hue family를 유지한 채 기존 #16A7A1보다 어둡게 낮췄다.
internal val VisitFillColorToday = Color(0xFF117E7A)
private const val VisitFillTodayArgb = 0xFF117E7AL

/**
 * 방문일 채움 위의 글자색. 새 대비 로직을 만들지 않고 텍스트 스티커의
 * "밝은 배경 → 어두운 글자 / 어두운 배경 → 밝은 글자" 판정
 * ([com.postcardmemory.ui.detail.labelStickerTextColorArgbFor])을 그대로 재사용한다.
 */
internal val VisitFillContrastColor = Color(labelStickerTextColorArgbFor(VisitFillArgb))

/** 오늘 진한 청록 위의 글자색도 같은 자동 대비 로직을 재사용한다. */
internal val VisitFillContrastColorToday = Color(labelStickerTextColorArgbFor(VisitFillTodayArgb))

/**
 * 오늘 + 실제 방문만 진한 청록, 그 외 방문일은 기존 색. 이 함수는 이미 `visited`인
 * 날짜에만 호출된다 — 미방문·미래 날짜는 호출부에서 라벨 자체를 그리지 않는다.
 */
internal fun visitDayFillColor(date: LocalDate, today: LocalDate): Color =
    if (date == today) VisitFillColorToday else VisitFillColor

internal fun visitDayFillContrastColor(date: LocalDate, today: LocalDate): Color =
    if (date == today) VisitFillContrastColorToday else VisitFillContrastColor

@Composable
private fun VisitCalendarTopOrnament() {
    Row(
        modifier = Modifier.fillMaxWidth().height(21.dp).clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = .5.dp,
            color = VisitCalendarOrnamentColor
        )
        Spacer(Modifier.width(6.dp))
        Text("✦", color = VisitCalendarOrnamentColor, fontSize = 9.sp)
        Spacer(Modifier.width(6.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = .5.dp,
            color = VisitCalendarOrnamentColor
        )
    }
}

// 150~250ms 검토 범위 중 다른 화면 전환보다 유독 느리지 않도록 중간값을 택했다.
private const val MONTH_TRANSITION_DURATION_MS = 200

private val VisitCalendarMonthSaver = Saver<YearMonth, String>(
    save = { it.toString() },
    restore = { saved -> runCatching { YearMonth.parse(saved) }.getOrDefault(YearMonth.now()) }
)

/**
 * 종이를 옆으로 미는 방향 = 시간이 이동하는 방향. state(월 값의 전후 비교)가 방향을 결정하고
 * animation은 그 결과를 표현만 한다 — 별도 "방향" state를 따로 두지 않는다.
 */
private fun AnimatedContentTransitionScope<YearMonth>.visitCalendarMonthTransition(): ContentTransform {
    val forward = targetState > initialState
    val offsetSpec = tween<IntOffset>(MONTH_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    val enter = slideInHorizontally(offsetSpec) { width -> if (forward) width else -width }
    val exit = slideOutHorizontally(offsetSpec) { width -> if (forward) -width else width }
    return (enter togetherWith exit).using(
        SizeTransform(sizeAnimationSpec = { _, _ -> tween(MONTH_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing) })
    )
}

@Composable
private fun VisitCalendarBottomOrnament() {
    Row(
        modifier = Modifier.fillMaxWidth().height(21.dp).clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("୨୧", color = VisitCalendarBottomOrnamentColor, fontSize = 9.sp)
        Spacer(Modifier.width(2.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = .5.dp,
            color = VisitCalendarBottomOrnamentColor
        )
        Spacer(Modifier.width(4.dp))
        Text("୨୧", color = VisitCalendarBottomOrnamentColor, fontSize = 9.sp)
    }
}

/** 예전 좌측 drawer의 위치·종이색을 재사용하되, 기능 메뉴는 복원하지 않는다. */
@Composable
internal fun VisitCalendarDrawer(
    drawerState: DrawerState,
    visitedEpochDays: Set<Long>,
    totalVisitDays: Int?,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        // 닫힌 상태의 가로 제스처는 기존 pager/엽서 제스처에 맡긴다.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(304.dp),
                drawerShape = RectangleShape,
                drawerContainerColor = PaperSurface,
                drawerTonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp)
                ) {
                    IconButton(
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Close, "방문 달력 닫기", tint = InkSecondary, modifier = Modifier.size(20.dp))
                    }
                    MonthlyVisitCalendar(visitedEpochDays, totalVisitDays)
                }
            }
        },
        content = content
    )
}

/** 한 달 분량의 날짜 grid만 그린다. AnimatedContent가 이 composable 전체를 슬라이드시킨다. */
@Composable
private fun VisitCalendarMonthGrid(month: YearMonth, visitedEpochDays: Set<Long>, today: LocalDate) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val weeks = calendarCellsFor(month).chunked(7)
        weeks.forEachIndexed { weekIndex, week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val visited = date != null && date.toEpochDay() in visitedEpochDays
                    Box(
                        modifier = Modifier.weight(1f).height(34.dp).clearAndSetSemantics {
                            if (date != null) {
                                contentDescription = "$date" + if (visited) ", 방문 기록 있음" else ""
                            }
                        }
                    ) {
                        if (date != null) {
                            // 방문 표시는 1차로 채움 색이 맡는다. 셀 전체를 꽉 채우지 않고
                            // 안쪽에 여백을 둬 습관 트래커의 딱딱한 사각형처럼 보이지 않게 한다.
                            if (visited) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .fillMaxSize()
                                        .padding(3.dp)
                                        .background(visitDayFillColor(date, today), RoundedCornerShape(2.dp))
                                )
                            }
                            val cellTextColor = if (visited) visitDayFillContrastColor(date, today) else visitDateColor(date)
                            Text(
                                date.dayOfMonth.toString(),
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp),
                                color = cellTextColor,
                                fontSize = 11.sp
                            )
                            if (visited) {
                                // 카오모지는 이제 방문 여부를 설명하는 주인공이 아니라, 채움 안에
                                // 붙는 작은 보조 스티커다.
                                Text(
                                    visitDayKaomoji(date),
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                                    color = cellTextColor,
                                    fontSize = 8.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            // 종이 달력의 행 구분을 흉내 낸 아주 옅은 가로선. 표/타임테이블처럼 보이지 않도록
            // 장식선과 함께 낮은 대비를 유지하고, 마지막 주 다음에는 넣지 않는다.
            if (weekIndex != weeks.lastIndex) {
                HorizontalDivider(
                    thickness = .5.dp,
                    color = PaperDivider.copy(alpha = 0.3f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
internal fun MonthlyVisitCalendar(
    visitedEpochDays: Set<Long>,
    totalVisitDays: Int? = null,
    today: LocalDate = remember { LocalDate.now() },
    initialMonth: YearMonth = YearMonth.from(today)
) {
    var displayedMonth by rememberSaveable(stateSaver = VisitCalendarMonthSaver) {
        mutableStateOf(initialMonth)
    }
    val isCurrentMonth = displayedMonth == YearMonth.from(today)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    "이전 달",
                    tint = InkSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            AnimatedContent(
                targetState = displayedMonth,
                transitionSpec = { visitCalendarMonthTransition() },
                modifier = Modifier.weight(1f).clipToBounds(),
                contentAlignment = Alignment.Center,
                label = "visitCalendarMonthTitle"
            ) { month ->
                Text(
                    text = "${month.year}년 ${month.monthValue}월",
                    color = InkPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { heading() }
                )
            }
            IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    "다음 달",
                    tint = InkSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "다녀간 날들",
                color = InkSecondary,
                fontSize = 9.sp,
                modifier = Modifier.weight(1f).padding(top = 1.dp)
            )
            if (!isCurrentMonth) {
                Text(
                    text = "오늘",
                    color = InkSecondary,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .clickable(onClick = { displayedMonth = YearMonth.from(today) })
                )
            }
        }
        VisitCalendarTopOrnament()
        Row(Modifier.fillMaxWidth()) {
            listOf(
                "일" to DayOfWeek.SUNDAY, "월" to DayOfWeek.MONDAY, "화" to DayOfWeek.TUESDAY,
                "수" to DayOfWeek.WEDNESDAY, "목" to DayOfWeek.THURSDAY, "금" to DayOfWeek.FRIDAY,
                "토" to DayOfWeek.SATURDAY
            ).forEach { (label, dow) ->
                Text(
                    label,
                    Modifier.weight(1f),
                    color = when (dow) {
                        DayOfWeek.SUNDAY -> WeekendSunday
                        DayOfWeek.SATURDAY -> WeekendSaturday
                        else -> InkSecondary
                    },
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = displayedMonth,
            transitionSpec = { visitCalendarMonthTransition() },
            modifier = Modifier.fillMaxWidth().clipToBounds(),
            label = "visitCalendarMonthGrid"
        ) { month ->
            VisitCalendarMonthGrid(month, visitedEpochDays, today)
        }
        VisitCalendarBottomOrnament()
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = visitCountLabel(totalVisitDays),
                modifier = Modifier.weight(1f),
                color = InkSecondary,
                fontSize = 10.sp
            )
            if (totalVisitDays != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription = "총 ${totalVisitDays}번 방문"
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = InkSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = " × $totalVisitDays",
                        color = InkSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
