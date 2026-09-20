package com.postcardmemory.ui.gallery

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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
import com.postcardmemory.utils.VisitHistoryStorage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

/**
 * 요일 하나에 대한 기본 글자색. 날짜 칸([visitDateColor])과 상단 요일 머리글이 주말색을
 * 따로 판정하다 어긋나지 않도록 한 곳에서만 계산한다.
 */
internal fun visitDayOfWeekColor(dayOfWeek: DayOfWeek): Color = when (dayOfWeek) {
    DayOfWeek.SATURDAY -> WeekendSaturday
    DayOfWeek.SUNDAY -> WeekendSunday
    else -> InkSecondary
}

/** 공휴일 데이터가 없어 요일 기본색만 적용한다. 대체공휴일 포함 지원은 STOP 상태다. */
internal fun visitDateColor(date: LocalDate): Color = visitDayOfWeekColor(date.dayOfWeek)

// 일요일 시작 7칸 머리글. 매 recomposition마다 새로 만들 이유가 없어 파일 상수로 둔다.
private val VISIT_CALENDAR_WEEKDAY_HEADERS = listOf(
    "일" to DayOfWeek.SUNDAY, "월" to DayOfWeek.MONDAY, "화" to DayOfWeek.TUESDAY,
    "수" to DayOfWeek.WEDNESDAY, "목" to DayOfWeek.THURSDAY, "금" to DayOfWeek.FRIDAY,
    "토" to DayOfWeek.SATURDAY
)

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

/**
 * MONTH_PICKER/YEAR_PICKER 안에서 ▲▼로 연도·decade를 한 칸씩 옮길 때 쓰는 수직 슬라이드.
 * 월 이동의 가로 슬라이드와 같은 원리(state 비교가 방향을 정하고 animation은 표현만) —
 * ▲(다음)는 위로 스와이프하듯, ▼(이전)는 아래로 스와이프하듯 움직인다(실기기 QA 피드백).
 */
private fun <T : Comparable<T>> AnimatedContentTransitionScope<T>.visitCalendarPickerStepTransition(): ContentTransform {
    val forward = targetState > initialState
    val offsetSpec = tween<IntOffset>(MONTH_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    val enter = slideInVertically(offsetSpec) { height -> if (forward) height else -height }
    val exit = slideOutVertically(offsetSpec) { height -> if (forward) -height else height }
    return (enter togetherWith exit).using(
        SizeTransform(sizeAnimationSpec = { _, _ -> tween(MONTH_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing) })
    )
}

/** 일반 달력 ↔ 월 선택 ↔ 연도 선택. 날짜 선택기가 아니라 달력 탐색기 단계만 나타낸다. */
internal enum class VisitCalendarNavLevel { CALENDAR, MONTH_PICKER, YEAR_PICKER }

private val VisitCalendarNavLevelSaver = Saver<VisitCalendarNavLevel, String>(
    save = { it.name },
    restore = { saved ->
        runCatching { VisitCalendarNavLevel.valueOf(saved) }.getOrDefault(VisitCalendarNavLevel.CALENDAR)
    }
)

/** back 한 번에 한 단계씩만 내려온다. 이미 CALENDAR면 더 내려갈 단계가 없다(호출부가 drawer를 닫는다). */
internal fun visitCalendarNavLevelOnBack(current: VisitCalendarNavLevel): VisitCalendarNavLevel = when (current) {
    VisitCalendarNavLevel.YEAR_PICKER -> VisitCalendarNavLevel.MONTH_PICKER
    VisitCalendarNavLevel.MONTH_PICKER -> VisitCalendarNavLevel.CALENDAR
    VisitCalendarNavLevel.CALENDAR -> VisitCalendarNavLevel.CALENDAR
}

/** 2026 -> 2020, 1999 -> 1990처럼 10년 단위 시작 연도를 계산한다. */
internal fun decadeStartFor(year: Int): Int = (year / 10) * 10

/** 이 연도가 지금 보는 10년 단위 decade 안에 있는지. YEAR_PICKER 4×4 grid에서 진하게/연하게를 가른다. */
internal fun isYearWithinDecade(year: Int, decadeStart: Int): Boolean = year in decadeStart..decadeStart + 9

/** YEAR_PICKER에서 약하게 구분할 연도. 표시 월의 연도가 지금 보는 decade 밖이면 아무 해도 강조하지 않는다. */
internal fun highlightedYearFor(decadeStart: Int, displayedMonth: YearMonth): Int? =
    if (isYearWithinDecade(displayedMonth.year, decadeStart)) displayedMonth.year else null

/**
 * MONTH_PICKER 칸이 "실제 오늘이 포함된 현재 월"인지. `displayedMonth`(선택/표시 중인 월)와는
 * 완전히 독립적인 판정이다 — 다른 달을 보고 있어도 오늘이 속한 달은 항상 같은 결과를 낸다.
 */
internal fun isCurrentMonthCell(year: Int, month: Int, today: YearMonth): Boolean =
    year == today.year && month == today.monthValue

/** YEAR_PICKER 칸이 실제 현재 연도인지. */
internal fun isCurrentYearCell(year: Int, today: YearMonth): Boolean = year == today.year

/**
 * 현재 위치 표시(아주 연한 원)를 그릴지. 선택 강조와 같은 칸이면 그리지 않는다 —
 * "선택 상태와 현재 상태는 다른 의미"이지만 같은 칸일 때 두 강조를 겹겹이 쌓지 않는다.
 */
internal fun visitCalendarShowsCurrentMarker(isCurrentPeriod: Boolean, isSelected: Boolean): Boolean =
    isCurrentPeriod && !isSelected

/**
 * MONTH_PICKER 한 칸의 색상 우선순위: 선택 > 실제 현재 월 > 현재 연도 소속 월(다음 해 칸은
 * 제외) > 다음 해(연한 구분) > 일반. 다음 해 버퍼 칸이 우연히 오늘의 달과 겹치는 경우(예:
 * pickerYear가 작년이라 다음 해 1~4월 칸에 오늘이 있을 때)에도 "실제 현재 월"이 "다음 해라 연하게"
 * 규칙보다 우선한다 — 다른 해를 보여준다는 시각적 구분보다 "오늘이 여기 있다"는 사실이 더 중요하다.
 */
internal enum class VisitCalendarMonthCellEmphasis { SELECTED, CURRENT_MONTH, CURRENT_YEAR, ADJACENT_YEAR, NORMAL }

internal fun visitCalendarMonthCellEmphasisFor(
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    isAdjacentYearCell: Boolean,
    isCurrentYearBeingViewed: Boolean
): VisitCalendarMonthCellEmphasis = when {
    isSelected -> VisitCalendarMonthCellEmphasis.SELECTED
    isCurrentMonth -> VisitCalendarMonthCellEmphasis.CURRENT_MONTH
    isAdjacentYearCell -> VisitCalendarMonthCellEmphasis.ADJACENT_YEAR
    isCurrentYearBeingViewed -> VisitCalendarMonthCellEmphasis.CURRENT_YEAR
    else -> VisitCalendarMonthCellEmphasis.NORMAL
}

/** YEAR_PICKER 한 칸의 색상 우선순위: 선택 > 실제 현재 연도 > decade 안 > decade 밖(연한 구분). */
internal enum class VisitCalendarYearCellEmphasis { SELECTED, CURRENT_YEAR, IN_DECADE, OUT_OF_DECADE }

internal fun visitCalendarYearCellEmphasisFor(
    isSelected: Boolean,
    isCurrentYear: Boolean,
    isInDecade: Boolean
): VisitCalendarYearCellEmphasis = when {
    isSelected -> VisitCalendarYearCellEmphasis.SELECTED
    isCurrentYear -> VisitCalendarYearCellEmphasis.CURRENT_YEAR
    isInDecade -> VisitCalendarYearCellEmphasis.IN_DECADE
    else -> VisitCalendarYearCellEmphasis.OUT_OF_DECADE
}

/** MONTH_PICKER/YEAR_PICKER 수직 swipe가 어느 방향으로 이동할지 결정하는 순수 판정. */
internal enum class VisitCalendarSwipeStep { NEXT, PREVIOUS, NONE }

/**
 * 위로 밀면(누적 drag가 음수 방향으로 threshold를 넘으면) NEXT(▲와 같은 방향),
 * 아래로 당기면 PREVIOUS(▼와 같은 방향). threshold 미만은 NONE(아직 swipe로 인정 안 함).
 */
internal fun visitCalendarSwipeStepFor(accumulatedDrag: Float, thresholdPx: Float): VisitCalendarSwipeStep = when {
    accumulatedDrag <= -thresholdPx -> VisitCalendarSwipeStep.NEXT
    accumulatedDrag >= thresholdPx -> VisitCalendarSwipeStep.PREVIOUS
    else -> VisitCalendarSwipeStep.NONE
}

/**
 * YEAR_PICKER 4×4 grid: decade 앞 2년 + 실제 decade 10년 + 뒤 4년, 총 16개.
 * 예: decadeStart=2020 -> 2018~2033. 윈도우 작업표시줄 캘린더의 연도 grid와 같은 배치.
 */
internal fun yearPickerGridYears(decadeStart: Int): List<Int> = (decadeStart - 2 until decadeStart + 14).toList()

/**
 * MONTH_PICKER 4×4 grid: pickerYear의 1~12월 + 다음 해 1~4월, 총 16개.
 * YEAR_PICKER와 같은 4×4 리듬을 맞추기 위해 다음 해 앞부분만 살짝 보여준다.
 */
internal fun monthPickerGridCells(pickerYear: Int): List<Pair<Int, Int>> =
    (1..16).map { index -> if (index <= 12) pickerYear to index else pickerYear + 1 to (index - 12) }

/**
 * 지금 MONTH_PICKER가 보여주는 4×4 창 안에 오늘이 포함되는지. 포함돼 있으면 칸 안의 연한
 * 원(marker)만으로 충분하고, 포함이 안 되면(다른 연도를 탐색 중이면) 헤더 아래 "오늘 ...로
 * 돌아가기" 링크를 따로 보여준다.
 */
internal fun isCurrentMonthVisibleInMonthPicker(pickerYear: Int, today: YearMonth): Boolean =
    today.year == pickerYear || (today.year == pickerYear + 1 && today.monthValue <= 4)

/** 지금 YEAR_PICKER가 보여주는 4×4 창(decade ± 여백) 안에 오늘 연도가 포함되는지. */
internal fun isCurrentYearVisibleInYearPicker(decadeStart: Int, today: YearMonth): Boolean =
    today.year in yearPickerGridYears(decadeStart)

// 기존 월 슬라이드(200ms)와 성격이 다른 계층 이동이라는 걸 구분하기 위해 살짝 더 짧게 뒀다.
private const val HIERARCHY_TRANSITION_DURATION_MS = 170

/**
 * CALENDAR/MONTH_PICKER/YEAR_PICKER 사이는 옆으로 미는 게 아니라 짧은 fade + 아주 작은 scale로
 * "단계가 바뀐다"는 느낌만 준다. 월 슬라이드의 가로 이동과 시각적으로 혼동되지 않도록 방향성 없이
 * 대칭적으로 처리한다(과한 zoom 금지 지시에 맞춰 최소 후보만 선택).
 */
private fun AnimatedContentTransitionScope<VisitCalendarNavLevel>.visitCalendarHierarchyTransition(): ContentTransform {
    val spec = tween<Float>(HIERARCHY_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing)
    val enter = fadeIn(spec) + scaleIn(spec, initialScale = 0.97f)
    val exit = fadeOut(spec) + scaleOut(spec, targetScale = 0.97f)
    return (enter togetherWith exit).using(
        SizeTransform(sizeAnimationSpec = { _, _ -> tween(HIERARCHY_TRANSITION_DURATION_MS, easing = FastOutSlowInEasing) })
    )
}

/**
 * MONTH_PICKER("2026년")·YEAR_PICKER("2020 - 2029") 공용 상단 줄. 왼쪽 라벨은(있다면) 한 단계
 * 위로 올라가는 진입점, 오른쪽 ▲▼는 같은 단계 안에서 연도/decade를 하나씩 옮긴다.
 */
@Composable
private fun VisitCalendarPickerHeaderRow(
    label: String,
    onLabelClick: (() -> Unit)?,
    stepUpDescription: String,
    stepDownDescription: String,
    onStepUp: () -> Unit,
    onStepDown: () -> Unit
) {
    Row(
        // 높이를 임의로 고정하지 않고 아래 ▲▼ 두 아이콘이 필요한 만큼 그대로 차지하게 둔다
        // (24dp로 고정했다가 36dp가 필요한 아이콘 둘이 잘려서 ▼가 작게 보였던 문제 수정, QA 피드백).
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = InkPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .weight(1f)
                .then(if (onLabelClick != null) Modifier.clickable(onClick = onLabelClick) else Modifier)
                .semantics { heading() }
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                stepUpDescription,
                tint = InkSecondary,
                modifier = Modifier.size(20.dp).clickable(onClick = onStepUp)
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                stepDownDescription,
                tint = InkSecondary,
                modifier = Modifier.size(20.dp).clickable(onClick = onStepDown)
            )
        }
    }
}

/**
 * picker 창 밖에 오늘이 있을 때만 헤더 아래에 뜨는 조용한 복귀 링크. 탭하면 보기 창만
 * 오늘이 포함되게 옮길 뿐 navLevel도 실제 선택(`displayedMonth`)도 바꾸지 않는다.
 * CALENDAR 레벨의 "오늘"(표시 월 자체를 되돌리고 햅틱까지 울림)과는 의미가 다른 동작이라,
 * 생김새가 같아도 두 handler를 하나로 합치지 않는다.
 */
@Composable
private fun VisitCalendarPickerTodayReturnLink(label: String, onReturnToToday: () -> Unit) {
    Text(
        text = label,
        color = InkSecondary,
        fontSize = 9.sp,
        modifier = Modifier
            .padding(top = 1.dp)
            .clickable(onClick = onReturnToToday)
    )
}

// 다음 해로 넘어가는 4칸을 구분하는 정도의 낮은 대비. 새 회색 토큰을 추가하지 않고 기존
// InkSecondary를 더 낮은 alpha로 재사용한다(장식선과 같은 방식).
private val VisitCalendarAdjacentPeriodColor = InkSecondary.copy(alpha = 0.4f)

// "지금 여기"라는 조용한 위치 안내일 뿐 성취가 아니므로, 선택 강조(InkPrimary)보다 훨씬
// 연한 alpha만 쓴다. 새 색을 만들지 않고 기존 InkSecondary를 한 번 더 낮춰 재사용한다.
private val VisitCalendarCurrentPeriodMarkerColor = InkSecondary.copy(alpha = 0.16f)
private val VisitCalendarCurrentPeriodMarkerSize = 26.dp

// 연한 원(marker)은 "정확한 위치", 색상은 "현재 연도에 속한 시간대"라는 다른 의미를 맡는다.
// 실제 현재 월/연도에는 방문일 marker와 같은 계열(VisitFillColorToday, "오늘" 테마)을 그대로
// 재사용해 또렷하게, MONTH_PICKER에서 "올해 전체"를 은근하게 알릴 때는 같은 색을 훨씬 낮은
// alpha로만 써서 실제 현재 월보다 한 단계 약하게 만든다. 새 색상 팔레트를 추가하지 않는다.
private val VisitCalendarCurrentYearMonthTintColor = VisitFillColorToday.copy(alpha = 0.5f)

@Composable
private fun VisitCalendarCurrentPeriodMarker() {
    Box(
        modifier = Modifier
            .size(VisitCalendarCurrentPeriodMarkerSize)
            .background(VisitCalendarCurrentPeriodMarkerColor, CircleShape)
    )
}

/**
 * 다른 달에서 오늘로 돌아왔을 때 딱 한 번 울리는 가벼운 "톡". 인트로 방문 소인이 종이에
 * 닿는 "통"(24ms/175, [com.postcardmemory.ui.intro.AppIntroScreen])보다 가볍게,
 * 갤러리 최소 탭(10ms/90)보다는 살짝 무겁게 잡았다. 이 "오늘" 버튼 자체가 이미
 * `!isCurrentMonth`일 때만 보이므로(이미 현재 월이면 버튼이 없음) 반복 탭에 대한 별도
 * 방지 로직 없이도 실제로 이동했을 때만 울린다. `LocalHapticFeedback`이 실기기에서
 * 무반응이라 확인된 전력이 있어(갤러리/인트로와 동일 판단) `Vibrator.vibrate`를 직접 쓴다.
 */
private const val VISIT_CALENDAR_TODAY_RETURN_HAPTIC_DURATION_MS = 14L
private const val VISIT_CALENDAR_TODAY_RETURN_HAPTIC_AMPLITUDE = 120

private fun vibrateVisitCalendarTodayReturn(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(
        VibrationEffect.createOneShot(
            VISIT_CALENDAR_TODAY_RETURN_HAPTIC_DURATION_MS,
            VISIT_CALENDAR_TODAY_RETURN_HAPTIC_AMPLITUDE
        )
    )
}

/**
 * Android가 "페이지 넘기기" 제스처에 표준으로 쓰는 scaledPagingTouchSlop을 그대로 쓴다.
 * 일반 touchSlop(탭/드래그 구분용)보다 커서 아주 작은 움직임에는 반응하지 않고, 새로
 * 임의의 px 값을 만들지 않는다.
 */
@Composable
private fun rememberVisitCalendarPagingTouchSlopPx(): Float {
    val context = LocalContext.current
    return remember(context) {
        android.view.ViewConfiguration.get(context).scaledPagingTouchSlop.toFloat()
    }
}

/**
 * MONTH_PICKER/YEAR_PICKER 공용 수직 swipe. 위로 밀면 [onStepUp](▲와 동일), 아래로 당기면
 * [onStepDown](▼와 동일) — 헤더의 ▲▼ 버튼과 똑같은 handler를 그대로 받아 두 입력 경로가
 * 분리되지 않게 한다. 방향 판정은 [visitCalendarSwipeStepFor] 순수 함수로 뺐다.
 */
@Composable
private fun rememberVisitCalendarPickerSwipeModifier(
    onStepUp: () -> Unit,
    onStepDown: () -> Unit
): Modifier {
    val pagingTouchSlop = rememberVisitCalendarPagingTouchSlopPx()
    val latestStepUp by rememberUpdatedState(onStepUp)
    val latestStepDown by rememberUpdatedState(onStepDown)
    return Modifier.pointerInput(pagingTouchSlop) {
        var accumulatedDrag = 0f
        detectVerticalDragGestures(
            onDragStart = { accumulatedDrag = 0f },
            onDragCancel = { accumulatedDrag = 0f },
            onDragEnd = {
                when (visitCalendarSwipeStepFor(accumulatedDrag, pagingTouchSlop)) {
                    VisitCalendarSwipeStep.NEXT -> latestStepUp()
                    VisitCalendarSwipeStep.PREVIOUS -> latestStepDown()
                    VisitCalendarSwipeStep.NONE -> Unit
                }
                accumulatedDrag = 0f
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                accumulatedDrag += dragAmount
            }
        )
    }
}

/**
 * pickerYear의 1~12월 + 다음 해 1~4월을 4열×4행으로. 다음 해 4칸은 연하게 구분해
 * YEAR_PICKER의 4×4 리듬과 맞춘다. 카드·pill·border 없이 텍스트 중심 grid만 쓴다.
 */
@Composable
private fun VisitCalendarMonthPicker(
    pickerYear: Int,
    displayedMonth: YearMonth,
    today: YearMonth,
    onMonthSelected: (year: Int, month: Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        monthPickerGridCells(pickerYear).chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (year, month) ->
                    val isNextYear = year != pickerYear
                    val isHighlighted = year == displayedMonth.year && month == displayedMonth.monthValue
                    val isCurrent = isCurrentMonthCell(year, month, today)
                    val emphasis = visitCalendarMonthCellEmphasisFor(
                        isSelected = isHighlighted,
                        isCurrentMonth = isCurrent,
                        isAdjacentYearCell = isNextYear,
                        isCurrentYearBeingViewed = pickerYear == today.year
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable(onClick = { onMonthSelected(year, month) }),
                        contentAlignment = Alignment.Center
                    ) {
                        if (visitCalendarShowsCurrentMarker(isCurrent, isHighlighted)) {
                            VisitCalendarCurrentPeriodMarker()
                        }
                        Text(
                            text = month.toString(),
                            textAlign = TextAlign.Center,
                            color = when (emphasis) {
                                VisitCalendarMonthCellEmphasis.SELECTED -> InkPrimary
                                VisitCalendarMonthCellEmphasis.CURRENT_MONTH -> VisitFillColorToday
                                VisitCalendarMonthCellEmphasis.CURRENT_YEAR -> VisitCalendarCurrentYearMonthTintColor
                                VisitCalendarMonthCellEmphasis.ADJACENT_YEAR -> VisitCalendarAdjacentPeriodColor
                                VisitCalendarMonthCellEmphasis.NORMAL -> InkSecondary
                            },
                            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * decade 앞 2년 + 실제 10년 + 뒤 4년을 4열×4행으로(윈도우 작업표시줄 캘린더와 같은 배치).
 * 앞/뒤 6칸은 연하게 구분해 지금 보는 decade가 어디까지인지 알 수 있게 한다.
 */
@Composable
private fun VisitCalendarYearPicker(
    decadeStart: Int,
    highlightYear: Int?,
    today: YearMonth,
    onYearSelected: (Int) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        yearPickerGridYears(decadeStart).chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { year ->
                    val inDecade = isYearWithinDecade(year, decadeStart)
                    val isHighlighted = year == highlightYear
                    val isCurrent = isCurrentYearCell(year, today)
                    val emphasis = visitCalendarYearCellEmphasisFor(
                        isSelected = isHighlighted,
                        isCurrentYear = isCurrent,
                        isInDecade = inDecade
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clickable(onClick = { onYearSelected(year) }),
                        contentAlignment = Alignment.Center
                    ) {
                        if (visitCalendarShowsCurrentMarker(isCurrent, isHighlighted)) {
                            VisitCalendarCurrentPeriodMarker()
                        }
                        Text(
                            text = year.toString(),
                            textAlign = TextAlign.Center,
                            color = when (emphasis) {
                                VisitCalendarYearCellEmphasis.SELECTED -> InkPrimary
                                VisitCalendarYearCellEmphasis.CURRENT_YEAR -> VisitFillColorToday
                                VisitCalendarYearCellEmphasis.IN_DECADE -> InkSecondary
                                VisitCalendarYearCellEmphasis.OUT_OF_DECADE -> VisitCalendarAdjacentPeriodColor
                            },
                            fontWeight = if (isHighlighted) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
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
    var navLevel by rememberSaveable(stateSaver = VisitCalendarNavLevelSaver) {
        mutableStateOf(VisitCalendarNavLevel.CALENDAR)
    }

    // drawer를 다시 열 때는 항상 CALENDAR로 연다 — YEAR_PICKER 등에 머문 채로 사용자를 놀라게
    // 하지 않는다. displayedMonth(표시 중이던 월)는 기존 동작을 그대로 유지하므로 건드리지 않는다.
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) navLevel = VisitCalendarNavLevel.CALENDAR
    }

    BackHandler(enabled = drawerState.isOpen) {
        if (navLevel == VisitCalendarNavLevel.CALENDAR) {
            scope.launch { drawerState.close() }
        } else {
            navLevel = visitCalendarNavLevelOnBack(navLevel)
        }
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
                    MonthlyVisitCalendar(
                        visitedEpochDays = visitedEpochDays,
                        totalVisitDays = totalVisitDays,
                        navLevel = navLevel,
                        onNavLevelChange = { navLevel = it }
                    )
                }
            }
        },
        content = content
    )
}

// 실제 달은 4~6주(28~37칸을 7의 배수로 채움)로 흔들린다. 6주로 고정해야 어떤 달이든
// 절대 모자라지 않는다 — 5주로 고정하면 31일이 금/토에 시작하는 달(6주 필요)이 그대로 넘친다.
private const val VISIT_CALENDAR_FIXED_ROW_COUNT = 6

/**
 * 실제 달 칸 뒤에 빈 칸을 채워 항상 [VISIT_CALENDAR_FIXED_ROW_COUNT]주(6주=42칸) 길이로
 * 맞춘다. 채우는 칸은 전부 null이라 존재하지 않는 날짜를 만들어내지 않는다 — 오직 표시 높이만
 * 맞추는 패딩이다.
 */
internal fun visitCalendarPaddedCells(month: YearMonth): List<LocalDate?> {
    val cells = calendarCellsFor(month)
    val targetSize = VISIT_CALENDAR_FIXED_ROW_COUNT * 7
    return if (cells.size >= targetSize) cells else cells + List(targetSize - cells.size) { null }
}

/**
 * 지금 그리는 [month]에 찍을 방문 표시를 고른다.
 *
 * 앱을 켤 때 읽어 둔 집합([currentMonthVisitedDays])은 **그 달의 것**이므로
 * 다른 달을 그릴 때 그대로 쓰면 안 된다. 다른 달은 그 달을 실제로 읽어온
 * 결과([loadedByMonth])만 쓰고, 아직 못 읽었으면 빈 집합을 준다 — 여기서
 * 다른 달 집합을 흘려보내면 없는 방문이 찍히거나 있는 방문이 사라진다.
 *
 * epochDay는 절대 날짜라 다른 달 값이 섞여도 이 달 칸과는 애초에 매칭되지
 * 않지만, "왜 안 섞이는지"를 우연에 맡기지 않으려고 규칙을 명시해 둔다.
 */
internal fun visitedDaysForMonth(
    month: YearMonth,
    currentMonth: YearMonth,
    currentMonthVisitedDays: Set<Long>,
    loadedByMonth: Map<YearMonth, Set<Long>>
): Set<Long> =
    if (month == currentMonth) {
        currentMonthVisitedDays
    } else {
        loadedByMonth[month].orEmpty()
    }

/** 한 달 분량의 날짜 grid만 그린다. AnimatedContent가 이 composable 전체를 슬라이드시킨다. */
@Composable
private fun VisitCalendarMonthGrid(month: YearMonth, visitedEpochDays: Set<Long>, today: LocalDate) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val realWeekCount = calendarCellsFor(month).chunked(7).size
        val weeks = visitCalendarPaddedCells(month).chunked(7)
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
            // 장식선과 함께 낮은 대비를 유지하고, 실제 마지막 주 다음이나 높이를 맞추는
            // 빈 패딩 행 사이에는 넣지 않는다.
            if (weekIndex < realWeekCount - 1) {
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
    today: LocalDate = rememberTodayDate(),
    initialMonth: YearMonth = YearMonth.from(today),
    navLevel: VisitCalendarNavLevel = VisitCalendarNavLevel.CALENDAR,
    onNavLevelChange: (VisitCalendarNavLevel) -> Unit = {}
) {
    // [initialMonth]는 이름 그대로 "처음 열 때의 월"이다. rememberSaveable에 key를 주지
    // 않는 것이 의도인데, 자정을 넘겨 [today]가 바뀌어도 사용자가 보고 있던 월이 현재
    // 월로 끌려가면 안 되기 때문이다 — 자정에 갱신되는 건 "현재 날짜 기준"(marker·색·
    // 복귀 링크)뿐이고 탐색 위치는 그대로 둔다. key를 추가하면 그 원칙이 깨진다.
    var displayedMonth by rememberSaveable(stateSaver = VisitCalendarMonthSaver) {
        mutableStateOf(initialMonth)
    }
    // MONTH_PICKER/YEAR_PICKER가 지금 보여주는 연도. 실제 달력의 displayedMonth와는 분리해서,
    // picker 안에서 연도만 훑어보다가 선택 없이 뒤로 가도 실제 표시 월을 건드리지 않는다.
    var pickerYear by rememberSaveable { mutableStateOf(initialMonth.year) }
    val todayYearMonth = YearMonth.from(today)
    val isCurrentMonth = displayedMonth == todayYearMonth
    val decadeStart = decadeStartFor(pickerYear)
    val context = LocalContext.current

    // [visitedEpochDays]는 앱을 켠 달 하나만 담고 있다. 달력은 어느 달로든
    // 이동할 수 있으므로, 지금 보고 있는 달의 방문 기록을 그때 읽어온다.
    // 읽기 전용이다 — 달력을 넘기는 것으로 방문이 생기지는 않는다
    // (방문을 만드는 규칙은 [com.postcardmemory.utils.VisitRecord] 참고).
    // 한 달은 marker 파일 최대 31개 stat이라 이동할 때마다 읽어도 싸고,
    // 같은 drawer 세션에서 왔다 갔다 할 때 표시가 깜빡이지 않도록 읽어온
    // 달만 기억해 둔다(세션 한정 memo이지 영구 캐시가 아니다).
    val loadedVisitsByMonth = remember { mutableStateMapOf<YearMonth, Set<Long>>() }

    LaunchedEffect(displayedMonth, todayYearMonth) {
        if (displayedMonth == todayYearMonth || loadedVisitsByMonth.containsKey(displayedMonth)) {
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            VisitHistoryStorage.loadMonth(context.filesDir, displayedMonth)
        }
        loadedVisitsByMonth[displayedMonth] = loaded
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(
            targetState = navLevel,
            transitionSpec = { visitCalendarHierarchyTransition() },
            modifier = Modifier.fillMaxWidth(),
            label = "visitCalendarNavLevel"
        ) { level ->
            Column(Modifier.fillMaxWidth()) {
                when (level) {
                    VisitCalendarNavLevel.CALENDAR -> {
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
                                    // 근거리는 좌우 화살표, 원거리는 이 제목을 눌러 월/연도 grid로.
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(onClick = {
                                            pickerYear = displayedMonth.year
                                            onNavLevelChange(VisitCalendarNavLevel.MONTH_PICKER)
                                        })
                                        .semantics { heading() }
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
                                        .clickable(onClick = {
                                            displayedMonth = todayYearMonth
                                            vibrateVisitCalendarTodayReturn(context)
                                        })
                                )
                            }
                        }
                        VisitCalendarTopOrnament()
                        Row(Modifier.fillMaxWidth()) {
                            VISIT_CALENDAR_WEEKDAY_HEADERS.forEach { (label, dow) ->
                                Text(
                                    label,
                                    Modifier.weight(1f),
                                    color = visitDayOfWeekColor(dow),
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
                            VisitCalendarMonthGrid(
                                month = month,
                                visitedEpochDays = visitedDaysForMonth(
                                    month = month,
                                    currentMonth = todayYearMonth,
                                    currentMonthVisitedDays = visitedEpochDays,
                                    loadedByMonth = loadedVisitsByMonth
                                ),
                                today = today
                            )
                        }
                    }
                    VisitCalendarNavLevel.MONTH_PICKER -> {
                        // ▲▼ 버튼과 swipe가 완전히 같은 결과를 내도록 handler를 한 번만 만들어
                        // 헤더 버튼과 아래 swipe modifier 양쪽에 그대로 넘긴다.
                        val onStepUp: () -> Unit = { pickerYear++ }
                        val onStepDown: () -> Unit = { pickerYear-- }
                        VisitCalendarPickerHeaderRow(
                            label = "${pickerYear}년",
                            onLabelClick = { onNavLevelChange(VisitCalendarNavLevel.YEAR_PICKER) },
                            stepUpDescription = "다음 연도",
                            stepDownDescription = "이전 연도",
                            onStepUp = onStepUp,
                            onStepDown = onStepDown
                        )
                        // 지금 보는 4×4 창에 오늘이 없으면(다른 연도를 탐색 중이면) 헤더 아래
                        // 조용한 복귀 링크를 보여준다 — 오늘이 이미 보이면(칸 안 marker로 충분)
                        // 굳이 중복 표시하지 않는다.
                        if (!isCurrentMonthVisibleInMonthPicker(pickerYear, todayYearMonth)) {
                            VisitCalendarPickerTodayReturnLink(
                                label = "오늘 ${todayYearMonth.year}년 ${todayYearMonth.monthValue}월 →",
                                onReturnToToday = { pickerYear = todayYearMonth.year }
                            )
                        }
                        VisitCalendarTopOrnament()
                        AnimatedContent(
                            targetState = pickerYear,
                            transitionSpec = { visitCalendarPickerStepTransition() },
                            modifier = Modifier.fillMaxWidth().clipToBounds()
                                .then(rememberVisitCalendarPickerSwipeModifier(onStepUp, onStepDown)),
                            label = "visitCalendarMonthPickerYear"
                        ) { year ->
                            VisitCalendarMonthPicker(
                                pickerYear = year,
                                displayedMonth = displayedMonth,
                                today = todayYearMonth,
                                onMonthSelected = { selectedYear, month ->
                                    displayedMonth = YearMonth.of(selectedYear, month)
                                    onNavLevelChange(VisitCalendarNavLevel.CALENDAR)
                                }
                            )
                        }
                    }
                    VisitCalendarNavLevel.YEAR_PICKER -> {
                        val onStepUp: () -> Unit = { pickerYear += 10 }
                        val onStepDown: () -> Unit = { pickerYear -= 10 }
                        VisitCalendarPickerHeaderRow(
                            label = "$decadeStart - ${decadeStart + 9}",
                            onLabelClick = null,
                            stepUpDescription = "다음 10년",
                            stepDownDescription = "이전 10년",
                            onStepUp = onStepUp,
                            onStepDown = onStepDown
                        )
                        if (!isCurrentYearVisibleInYearPicker(decadeStart, todayYearMonth)) {
                            VisitCalendarPickerTodayReturnLink(
                                label = "오늘 ${todayYearMonth.year}년 →",
                                onReturnToToday = { pickerYear = todayYearMonth.year }
                            )
                        }
                        VisitCalendarTopOrnament()
                        AnimatedContent(
                            targetState = decadeStart,
                            transitionSpec = { visitCalendarPickerStepTransition() },
                            modifier = Modifier.fillMaxWidth().clipToBounds()
                                .then(rememberVisitCalendarPickerSwipeModifier(onStepUp, onStepDown)),
                            label = "visitCalendarYearPickerDecade"
                        ) { start ->
                            VisitCalendarYearPicker(
                                decadeStart = start,
                                highlightYear = highlightedYearFor(start, displayedMonth),
                                today = todayYearMonth,
                                onYearSelected = { year ->
                                    pickerYear = year
                                    onNavLevelChange(VisitCalendarNavLevel.MONTH_PICKER)
                                }
                            )
                        }
                    }
                }
            }
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
