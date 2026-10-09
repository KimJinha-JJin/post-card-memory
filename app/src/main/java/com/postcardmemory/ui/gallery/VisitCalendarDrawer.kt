package com.postcardmemory.ui.gallery

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.R
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
// 96일차 후속: 하단 양끝 ୨୧ 자리에 붙인 신문 오림 리본. 자산(1448×1086)은 둘레에 투명 여백이 있어
// 리본 자체는 가운데 약 65%×42%다 — 이미지 폭 24dp면 리본은 약 16×8dp로 9sp 기호와 비슷하다.
// 원본 비율·알파 그대로, 그림자 없음. 값은 실기기 QA로 다듬는 미감 값이다.
private val VISIT_CALENDAR_BOTTOM_RIBBON_WIDTH = 24.dp
private const val VISIT_CALENDAR_BOTTOM_RIBBON_ASPECT = 1448f / 1086f

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

// 90일차: 벽걸이 달력 한 장을 위로 넘기는 월 이동. 값은 실기기 QA로 다듬는 미감 값이다.
private const val PAGE_TURN_DURATION_MS = 380
// 원근을 약하게 둬 책장처럼 깊게 말리지 않고 얇은 종이가 살짝 들리는 정도로만 보이게 한다.
private const val PAGE_TURN_CAMERA_DISTANCE = 14f

// 이전 달: 이전 장이 위에서 내려와 덮는다. 실기기 QA에서 자연스럽다고 확인된 값이라 그대로 둔다.
// 들린 채(80°, 반투명) 시작해 FastOutSlowIn으로 빨리 불투명해지고 천천히 내려앉는다.
private const val PAGE_TURN_BACKWARD_MAX_ANGLE_DEG = 80f
private const val PAGE_TURN_BACKWARD_FADE_START = 0.45f

// 다음 달: 현재 장이 윗변을 축으로 위로 들려 넘어간다. 불투명한 채 천천히 들리다 빨라져
// 옆면(90°)에서 선처럼 사라지고, 흐려짐은 마지막에만 쓴다 — 오래 반투명하게 떠 있으면
// 두 달의 인쇄가 겹쳐 보여 종이가 아니라 화면 crossfade처럼 읽힌다(90일차 QA).
private const val PAGE_TURN_FORWARD_MAX_ANGLE_DEG = 90f
private const val PAGE_TURN_FORWARD_FADE_START = 0.9f

// 그림자의 최대 농도. 다음 달에서는 들리는 현재 장에, 이전 달에서는 덮이는 아래 장에 드리운다.
private const val PAGE_TURN_MAX_SHADE = 0.12f

/**
 * 달력 묶음에서 이 달의 장이 놓이는 높이. 실제 벽걸이 달력처럼 앞선 달이 뒤 달 위에 겹쳐
 * 있다 — 다음 달로 가면 지금 장이 위에서 들려 넘어가며 아래 장이 드러나고, 이전 달로 가면
 * 이전 장이 위에서 내려와 덮는다. 달 자체로 정해지므로 별도 순서 state가 없다.
 */
internal fun visitCalendarPageStackZIndex(month: YearMonth): Float =
    -(month.year * 12 + month.monthValue - 1).toFloat()

/**
 * 들린 정도(0 = 펼쳐짐, 1 = 다 넘어감)에 따른 움직이는 장의 불투명도. [forward]면 현재 장이
 * 거의 다 넘어갈 때까지 불투명하고, 이전 달로 내려오는 장은 더 일찍부터 흐려져 있다.
 */
internal fun visitCalendarPageTurnAlpha(lift: Float, forward: Boolean): Float {
    val fadeStart = if (forward) PAGE_TURN_FORWARD_FADE_START else PAGE_TURN_BACKWARD_FADE_START
    return if (lift <= fadeStart) {
        1f
    } else {
        (1f - (lift - fadeStart) / (1f - fadeStart)).coerceIn(0f, 1f)
    }
}

/**
 * 월 이동의 AnimatedContent 전환. 움직임 자체는 각 장의 [visitCalendarPageTurnModifier]가
 * 맡고, 여기서는 두 장을 끝까지 남겨 두고 달력 묶음 순서로 겹치게만 한다. 어느 달이든 장의
 * 높이가 같아 크기 전환이 없고, 들린 장이 옆으로 살짝 넓어져도 잘리지 않게 clip하지 않는다.
 */
private fun AnimatedContentTransitionScope<YearMonth>.visitCalendarPageTurnTransition(): ContentTransform =
    ContentTransform(
        targetContentEnter = EnterTransition.None,
        initialContentExit = ExitTransition.KeepUntilTransitionsFinished,
        targetContentZIndex = visitCalendarPageStackZIndex(targetState),
        sizeTransform = SizeTransform(clip = false)
    )

/**
 * 달력 한 장의 넘김 표현. [forward](다음 달로 이동)이면 위에 놓인 현재 장이 불투명한 채 들려
 * 넘어가며 조금 어두워지고, 다음 장은 그 아래에 움직이지 않고 놓여 있다가 드러난다. 이전 달로
 * 이동하면 이전 장이 위에서 내려와 덮고, 덮이는 장은 그 아래에서 그림자가 짙어진다. 두 방향은
 * 대칭일 필요가 없어 값을 따로 둔다. 장 안의 종이·글자·방문 표시가 한 몸으로 움직인다.
 */
@Composable
private fun AnimatedVisibilityScope.visitCalendarPageTurnModifier(forward: Boolean): Modifier {
    // 움직이는 장은 방향마다 하나뿐이다 — 다음 달은 나가는 장(PostExit), 이전 달은 들어오는 장(PreEnter).
    val easing = if (forward) FastOutLinearInEasing else FastOutSlowInEasing
    val lift by transition.animateFloat(
        transitionSpec = { tween(PAGE_TURN_DURATION_MS, easing = easing) },
        label = "visitCalendarPageLift"
    ) { state ->
        when (state) {
            EnterExitState.PreEnter -> if (forward) 0f else 1f
            EnterExitState.Visible -> 0f
            EnterExitState.PostExit -> if (forward) 1f else 0f
        }
    }
    // 그림자는 두 방향 모두 나가는 장에만 진다 — 다음 달에서는 들려 넘어가는 현재 장,
    // 이전 달에서는 내려오는 장에 덮이는 아래 장. 다음 장이 그림자에서 밝아지며 나타나면
    // fade-in처럼 보여서 들어오는 장에는 그림자를 두지 않는다.
    val shade by transition.animateFloat(
        transitionSpec = { tween(PAGE_TURN_DURATION_MS, easing = easing) },
        label = "visitCalendarPageShade"
    ) { state ->
        when (state) {
            EnterExitState.PreEnter -> 0f
            EnterExitState.Visible -> 0f
            EnterExitState.PostExit -> 1f
        }
    }
    val maxAngle = if (forward) PAGE_TURN_FORWARD_MAX_ANGLE_DEG else PAGE_TURN_BACKWARD_MAX_ANGLE_DEG
    return Modifier
        .graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 0f)
            rotationX = lift * maxAngle
            cameraDistance = PAGE_TURN_CAMERA_DISTANCE * density
            alpha = visitCalendarPageTurnAlpha(lift, forward)
        }
        .drawWithContent {
            drawContent()
            if (shade > 0f) drawRect(Color.Black, alpha = shade * PAGE_TURN_MAX_SHADE)
        }
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

// 월/연도 고르기 칸마다 글자 뒤에 달력 종이의 자투리 한 조각. 새 자산 없이
// 이미 불러온 visit_calendar_paper를 칸마다 다른 하단 위치에서 잘라 써 종이결이 복붙되지 않고,
// 가장자리는 달력 장과 같은 손으로 자른 윤곽·같은 아주 옅은 접촉 그림자를 칸마다 다른 seed로 쓴다.
// 칸 크기·글자·선택 표시·현재 표시 동그라미·터치는 그대로다. 값은 실기기 QA로 다듬는 미감 값이다.
private val VISIT_CALENDAR_PICKER_SCRAP_INSET_X = 4.dp
private val VISIT_CALENDAR_PICKER_SCRAP_INSET_Y = 4.dp
private const val VISIT_CALENDAR_PICKER_SCRAP_SEED = 96_600
// 자투리는 종이 아래쪽 이 비율부터 끝까지에서만 고른다.
internal const val VISIT_CALENDAR_PICKER_SCRAP_REGION_TOP = 0.55f

internal fun visitCalendarMonthPickerScrapSeed(year: Int, month: Int): Int =
    VISIT_CALENDAR_PICKER_SCRAP_SEED + year * 13 + month

internal fun visitCalendarYearPickerScrapSeed(year: Int): Int = VISIT_CALENDAR_PICKER_SCRAP_SEED + 50_000 + year

/**
 * [paperWidth]×[paperHeight] 종이에서 [cropWidth]×[cropHeight] 자투리를 잘라 낼 왼쪽 위(px).
 * 종이 하단([VISIT_CALENDAR_PICKER_SCRAP_REGION_TOP]~끝) 안에서 seed마다 다르게 고르고, 같은 seed면 같다.
 */
internal fun visitCalendarPickerScrapCrop(
    seed: Int,
    paperWidth: Int,
    paperHeight: Int,
    cropWidth: Int,
    cropHeight: Int
): IntOffset {
    val random = kotlin.random.Random(seed)
    val maxX = (paperWidth - cropWidth).coerceAtLeast(0)
    val top = (paperHeight * VISIT_CALENDAR_PICKER_SCRAP_REGION_TOP).toInt()
    val maxY = (paperHeight - cropHeight).coerceAtLeast(0)
    val minY = top.coerceAtMost(maxY)
    return IntOffset(
        if (maxX == 0) 0 else random.nextInt(maxX + 1),
        if (maxY == minY) minY else minY + random.nextInt(maxY - minY + 1)
    )
}

/** 칸 뒤에 [visitCalendarPickerScrapCrop] 자리의 종이 자투리를 손으로 자른 윤곽으로 깐다. */
private fun Modifier.visitCalendarPickerPaperScrap(paper: ImageBitmap, seed: Int, gridWidthCells: Int = 4): Modifier =
    drawWithCache {
        val insetX = VISIT_CALENDAR_PICKER_SCRAP_INSET_X.toPx()
        val insetY = VISIT_CALENDAR_PICKER_SCRAP_INSET_Y.toPx()
        val pieceWidth = (size.width - insetX * 2).coerceAtLeast(1f)
        val pieceHeight = (size.height - insetY * 2).coerceAtLeast(1f)
        // 종이결 크기를 달력 장과 맞춘다 — 종이 한 장의 폭이 grid 한 줄 폭에 놓이는 배율.
        val scale = size.width * gridWidthCells / paper.width
        val crop = visitCalendarPickerScrapCrop(
            seed = seed,
            paperWidth = paper.width,
            paperHeight = paper.height,
            cropWidth = (pieceWidth / scale).toInt().coerceAtLeast(1),
            cropHeight = (pieceHeight / scale).toInt().coerceAtLeast(1)
        )
        val brush = ShaderBrush(
            ImageShader(paper).apply {
                setLocalMatrix(
                    android.graphics.Matrix().apply {
                        setTranslate(-crop.x.toFloat(), -crop.y.toFloat())
                        postScale(scale, scale)
                        postTranslate(insetX, insetY)
                    }
                )
            }
        )
        val outline = Path().apply {
            visitCalendarPaperEdgeOutline(pieceWidth / density, pieceHeight / density, seed).forEachIndexed { index, point ->
                val x = insetX + point.x * density
                val y = insetY + point.y * density
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        onDrawBehind {
            VISIT_CALENDAR_PAPER_SHADOW_LAYERS.forEach { (dx, dy, alpha) ->
                translate(left = dx * density, top = dy * density) {
                    drawPath(outline, color = VisitCalendarPaperShadowColor, alpha = alpha)
                }
            }
            drawPath(outline, brush = brush)
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
    paper: ImageBitmap,
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
                            .visitCalendarPickerPaperScrap(paper, visitCalendarMonthPickerScrapSeed(year, month))
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
    paper: ImageBitmap,
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
                            .visitCalendarPickerPaperScrap(paper, visitCalendarYearPickerScrapSeed(year))
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

// 96일차 추가: 월 이동 ◀ ▶을 오래된 신문에 인쇄된 작은 삼각형을 가위로 오려 붙인 듯한 표시로.
// 버튼(IconButton 48dp 터치 영역·클릭·접근성 설명)은 그대로 두고 보이는 화살표만 바꾼다.
// 가위로 몇 번 끊어 자른 삼각형에 잉크를 칠하고, 45° 망점으로 종이가 비쳐 보이게 한다(하프톤).
// 흰 테두리·그림자 없음. 조작부라 장식 리본보다 또렷하게 InkPrimary 계열 잉크를 쓴다.
// 값은 실기기 QA로 다듬는 미감 값이다.
private val VISIT_CALENDAR_ARROW_SIZE = 20.dp
private const val VISIT_CALENDAR_ARROW_BOX_DP = 20f
private const val VISIT_CALENDAR_ARROW_SEED_PREVIOUS = 96_500
private const val VISIT_CALENDAR_ARROW_SEED_NEXT = 96_501
private val VisitCalendarArrowInk = InkPrimary.copy(alpha = 0.82f)
private val VisitCalendarArrowNewsprint = Color(0xFFE9E1D3)

/** 망점 하나(dp, 화살표 상자 기준). 잉크 사이로 비치는 종이다. */
internal data class VisitCalendarHalftoneDot(val center: Offset, val radiusDp: Float)

/** 오려 낸 삼각형 화살표의 윤곽과 망점(dp, [VISIT_CALENDAR_ARROW_BOX_DP] 정사각 상자 기준). */
internal data class VisitCalendarNewsprintArrow(val outline: List<Offset>, val dots: List<VisitCalendarHalftoneDot>)

/** [pointsLeft]면 ◀, 아니면 ▶. 같은 seed면 항상 같은 모양이다. */
internal fun visitCalendarNewsprintArrow(pointsLeft: Boolean, seed: Int): VisitCalendarNewsprintArrow {
    val random = kotlin.random.Random(seed)
    fun jitter(amount: Float) = (random.nextFloat() - 0.5f) * 2f * amount
    // ◀ 기준: 끝 (5.5, 10), 뒤 위 (14.5, 4.5), 뒤 아래 (14.5, 15.5) — 폭 9dp·높이 11dp.
    val corners = listOf(
        Offset(5.5f + jitter(0.3f), 10f + jitter(0.3f)),
        Offset(14.5f + jitter(0.3f), 4.5f + jitter(0.3f)),
        Offset(14.5f + jitter(0.3f), 15.5f + jitter(0.3f))
    )
    // 변마다 가위를 1~2번 고쳐 잡은 듯 중간에서 살짝 꺾인다.
    val cut = mutableListOf<Offset>()
    corners.indices.forEach { i ->
        val a = corners[i]
        val b = corners[(i + 1) % corners.size]
        cut += a
        val edge = b - a
        val normal = Offset(-edge.y, edge.x) / edge.getDistance()
        val bends = 1 + random.nextInt(2)
        (1..bends).forEach { k ->
            val t = k.toFloat() / (bends + 1) + jitter(0.08f)
            cut += a + edge * t + normal * jitter(0.25f)
        }
    }
    val outline = if (pointsLeft) cut else cut.map { Offset(VISIT_CALENDAR_ARROW_BOX_DP - it.x, it.y) }
    // 45° 망점. 잉크 농도가 조금씩 일렁여 비치는 종이 점 크기가 다르고, 가장자리는 잉크가 번져 점이 없다.
    val pitch = 1.15f
    val phaseA = random.nextFloat() * 6.28f
    val phaseB = random.nextFloat() * 6.28f
    val dots = mutableListOf<VisitCalendarHalftoneDot>()
    val steps = (VISIT_CALENDAR_ARROW_BOX_DP * 1.5f / pitch).toInt()
    val diagonal = Offset(0.7071f, 0.7071f)
    val antiDiagonal = Offset(-0.7071f, 0.7071f)
    val origin = Offset(VISIT_CALENDAR_ARROW_BOX_DP / 2f, VISIT_CALENDAR_ARROW_BOX_DP / 2f)
    for (i in -steps..steps) for (j in -steps..steps) {
        val center = origin + diagonal * (i * pitch) + antiDiagonal * (j * pitch)
        val tone = 0.5f + 0.3f * kotlin.math.sin(center.x * 0.55f + phaseA) * kotlin.math.cos(center.y * 0.45f + phaseB) +
            jitter(0.2f)
        val radius = (0.12f + 0.24f * tone).coerceIn(0.1f, 0.38f)
        if (visitCalendarPointInPolygon(center, outline) &&
            visitCalendarDistanceToOutline(center, outline) >= radius + 0.5f
        ) {
            dots += VisitCalendarHalftoneDot(center, radius)
        }
    }
    return VisitCalendarNewsprintArrow(outline, dots)
}

private fun visitCalendarPointInPolygon(p: Offset, polygon: List<Offset>): Boolean {
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[j]
        if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) inside = !inside
        j = i
    }
    return inside
}

private fun visitCalendarDistanceToOutline(p: Offset, polygon: List<Offset>): Float =
    polygon.indices.minOf { i ->
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        val ab = b - a
        val t = (((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / (ab.x * ab.x + ab.y * ab.y)).coerceIn(0f, 1f)
        (p - (a + ab * t)).getDistance()
    }

/** [visitCalendarNewsprintArrow]을 그린다: 아주 옅은 잉크 번짐 → 잉크 → 비치는 종이 망점. */
private fun Modifier.visitCalendarNewsprintArrowMark(pointsLeft: Boolean, seed: Int): Modifier = drawWithCache {
    val arrow = visitCalendarNewsprintArrow(pointsLeft, seed)
    val unit = size.minDimension / VISIT_CALENDAR_ARROW_BOX_DP
    val path = Path().apply {
        arrow.outline.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x * unit, point.y * unit) else lineTo(point.x * unit, point.y * unit)
        }
        close()
    }
    val bleed = Stroke(width = 0.5f * unit, join = StrokeJoin.Round)
    onDrawBehind {
        drawPath(path, color = VisitCalendarArrowInk, alpha = 0.18f, style = bleed)
        drawPath(path, color = VisitCalendarArrowInk)
        arrow.dots.forEach { dot ->
            drawCircle(VisitCalendarArrowNewsprint, radius = dot.radiusDp * unit, center = dot.center * unit)
        }
    }
}

@Composable
private fun VisitCalendarBottomOrnament() {
    // 두 자리가 한 번만 decode한 같은 bitmap을 함께 쓴다.
    val ribbon = ImageBitmap.imageResource(R.drawable.visit_calendar_newspaper_ribbon)
    val ribbonModifier = Modifier
        .width(VISIT_CALENDAR_BOTTOM_RIBBON_WIDTH)
        .aspectRatio(VISIT_CALENDAR_BOTTOM_RIBBON_ASPECT)
    Row(
        modifier = Modifier.fillMaxWidth().height(21.dp).clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(bitmap = ribbon, contentDescription = null, modifier = ribbonModifier)
        // 90일차: 폭 전체 가로선은 달력 종이 밑변과 이중 경계를 만들어 사이드바 구획선처럼
        // 보여(실기기 QA) 빼고, 양끝 표시만 남긴다. 96일차 후속: ୨୧ 기호를 신문 오림 리본으로 교체.
        Spacer(Modifier.weight(1f))
        Image(bitmap = ribbon, contentDescription = null, modifier = ribbonModifier)
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

// 96일차 3단계: 방문일 민트 표시를 매끈한 상자 대신 민트 중성펜으로 슥슥 칠한 자국으로 그린다.
// 색(#16A7A1 / 오늘 #117E7A)은 그대로고 질감만 바뀐다. 옅은 잉크 바탕 위에 짧은 사선 획을 겹쳐,
// 획이 지나간 곳·겹친 곳이 조금 더 진하고 획 사이로 종이가 비친다. 첫 QA에서 바탕 α0.75는
// "팔레트에 묻은 물감" 같다고 해 α0.5로 옅게 했다. 칸 글자색은 여전히 단색 기준 자동 대비라
// 가독성은 실기기로 확인한다. 값은 실기기 QA로 다듬는 미감 값이다.
private const val VISIT_DAY_PEN_SEED = 96_300
private const val VISIT_DAY_PEN_WASH_ALPHA = 0.5f
internal const val VISIT_DAY_PEN_EDGE_WOBBLE_DP = 0.35f

/** 칠한 자국의 한 획(dp). */
internal class VisitDayPenStroke(
    val points: List<Offset>,
    val widthDp: Float,
    val alpha: Float
)

/** 칠한 자국 하나: 잉크가 고르게 먹은 바탕 [wash](손떨림 있는 외곽) + 그 위의 [strokes]. */
internal class VisitDayPenMark(
    val wash: List<Offset>,
    val strokes: List<VisitDayPenStroke>
)

internal fun visitDayPenSeed(date: LocalDate): Int = VISIT_DAY_PEN_SEED + date.toEpochDay().toInt()

/** 손으로 그은 살짝 휘는 한 획. 중성펜이라 획 안의 농도는 거의 일정하다. */
private fun visitDayFreehandStroke(
    from: Offset,
    to: Offset,
    widthDp: Float,
    alpha: Float,
    random: kotlin.random.Random
): VisitDayPenStroke {
    val length = (to - from).getDistance()
    val dir = if (length > 0f) (to - from) / length else Offset.Zero
    val normal = Offset(-dir.y, dir.x)
    val segments = maxOf(2, kotlin.math.ceil(length / 2f).toInt())
    val bow = (random.nextFloat() - 0.5f) * 0.4f // 획 전체가 살짝 휘는 정도
    val phase = random.nextFloat() * 6.28f
    val points = (0..segments).map { k ->
        val t = k.toFloat() / segments
        val offset = bow * kotlin.math.sin(t * Math.PI.toFloat()) + 0.06f * kotlin.math.sin(t * 9f + phase)
        from + dir * (length * t) + normal * offset
    }
    return VisitDayPenStroke(points, widthDp, alpha)
}

/**
 * 방문일 칸([widthDp]×[heightDp]) 하나를 민트 중성펜으로 칠한 자국. 같은 날짜(seed)면 항상 같은
 * 자국이다 — 재구성돼도 획이 흔들리지 않는다. 외곽은 칸 경계에서 1dp 남짓 안쪽·바깥쪽으로만
 * 흔들려 날짜 칸 배치는 그대로다.
 */
internal fun visitDayGelPenMark(widthDp: Float, heightDp: Float, seed: Int): VisitDayPenMark? {
    if (widthDp <= 0f || heightDp <= 0f) return null
    val random = kotlin.random.Random(seed)

    // 바탕: 칸보다 0.5dp 안쪽 사각형을 3dp 간격 점으로 돌며 손떨림만큼 흔든다.
    val inset = 0.5f
    val corners = listOf(
        Offset(inset, inset), Offset(widthDp - inset, inset),
        Offset(widthDp - inset, heightDp - inset), Offset(inset, heightDp - inset)
    )
    val wash = corners.indices.flatMap { i ->
        val from = corners[i]
        val to = corners[(i + 1) % corners.size]
        val length = (to - from).getDistance()
        val dir = (to - from) / length
        val normal = Offset(-dir.y, dir.x)
        val steps = maxOf(2, kotlin.math.ceil(length / 3f).toInt())
        (0 until steps).map { k ->
            from + dir * (length * k / steps) + normal * ((random.nextFloat() * 2f - 1f) * VISIT_DAY_PEN_EDGE_WOBBLE_DP)
        }
    }

    // 획: 약한 사선(-20° 안팎)으로 칸을 가로지르며 1.3dp 남짓 간격으로 반복. 끝은 칸 경계에서
    // 조금 모자라거나 살짝 삐져나가고, 가끔 한 줄을 두 번에 나눠 그어 이음매가 겹쳐 진해진다.
    val strokes = mutableListOf<VisitDayPenStroke>()
    fun hatch(angleDeg: Float, spacing: Float, widthDp0: Float, alpha0: Float, keep: Float) {
        val angle = Math.toRadians(angleDeg.toDouble()).toFloat()
        val dir = Offset(kotlin.math.cos(angle), kotlin.math.sin(angle))
        val normal = Offset(-dir.y, dir.x)
        val projections = listOf(
            Offset(0f, 0f), Offset(widthDp, 0f), Offset(0f, heightDp), Offset(widthDp, heightDp)
        ).map { it.x * normal.x + it.y * normal.y }
        var c = projections.min() + spacing * random.nextFloat()
        while (c < projections.max()) {
            val base = normal * c
            var t0 = -Float.MAX_VALUE
            var t1 = Float.MAX_VALUE
            fun clip(p: Float, d: Float, lo: Float, hi: Float): Boolean {
                if (kotlin.math.abs(d) < 1e-6f) return p in lo..hi
                val a = (lo - p) / d
                val b = (hi - p) / d
                t0 = maxOf(t0, minOf(a, b))
                t1 = minOf(t1, maxOf(a, b))
                return t0 <= t1
            }
            if (random.nextFloat() < keep &&
                clip(base.x, dir.x, 0.6f, widthDp - 0.6f) && clip(base.y, dir.y, 0.6f, heightDp - 0.6f)
            ) {
                val start = t0 + (random.nextFloat() * 1.2f - 0.5f)
                val end = t1 - (random.nextFloat() * 1.2f - 0.5f)
                if (end - start > 1.5f) {
                    val strokeWidth = widthDp0 * (0.92f + random.nextFloat() * 0.16f)
                    val strokeAlpha = alpha0 * (0.88f + random.nextFloat() * 0.24f)
                    if (end - start > 14f && random.nextFloat() < 0.3f) {
                        // 두 번에 나눠 그은 줄: 이음매가 1dp 남짓 겹친다.
                        val mid = start + (end - start) * (0.35f + random.nextFloat() * 0.3f)
                        strokes += visitDayFreehandStroke(base + dir * start, base + dir * (mid + 0.6f), strokeWidth, strokeAlpha, random)
                        strokes += visitDayFreehandStroke(base + dir * (mid - 0.6f), base + dir * end, strokeWidth, strokeAlpha, random)
                    } else {
                        strokes += visitDayFreehandStroke(base + dir * start, base + dir * end, strokeWidth, strokeAlpha, random)
                    }
                }
            }
            c += spacing * (0.8f + 0.4f * random.nextFloat())
        }
    }
    // 첫 번째로 슥슥 칠하고, 각도를 조금 바꿔 듬성듬성 한 번 더 지나간다.
    hatch(angleDeg = -20f + (random.nextFloat() - 0.5f) * 6f, spacing = 1.3f, widthDp0 = 1.15f, alpha0 = 0.4f, keep = 1f)
    hatch(angleDeg = -12f + (random.nextFloat() - 0.5f) * 6f, spacing = 2.6f, widthDp0 = 1.05f, alpha0 = 0.28f, keep = 0.55f)
    return VisitDayPenMark(wash, strokes)
}

/** [visitDayGelPenMark]를 [color] 잉크로 그린다. 칸 크기가 정해질 때만 path를 만든다. */
private fun Modifier.visitDayGelPenMark(color: Color, seed: Int): Modifier = drawWithCache {
    val mark = visitDayGelPenMark(size.width / density, size.height / density, seed)
    fun pathOf(points: List<Offset>, closed: Boolean) = Path().apply {
        points.forEachIndexed { index, point ->
            if (index == 0) moveTo(point.x * density, point.y * density)
            else lineTo(point.x * density, point.y * density)
        }
        if (closed) close()
    }
    val washPath = mark?.let { pathOf(it.wash, closed = true) }
    val strokes = mark?.strokes.orEmpty().map { stroke ->
        Triple(
            pathOf(stroke.points, closed = false),
            Stroke(width = stroke.widthDp * density, cap = StrokeCap.Round),
            stroke.alpha
        )
    }
    onDrawBehind {
        washPath?.let { drawPath(it, color = color, alpha = VISIT_DAY_PEN_WASH_ALPHA) }
        strokes.forEach { (path, style, alpha) -> drawPath(path, color = color, alpha = alpha, style = style) }
    }
}

// 96일차 후속: 오늘 날짜 숫자 둘레를 얇은 펜으로 몇 번 휘갈긴 동그라미. 방문 여부와 별개라
// 방문하지 않은 오늘에도 보이고, 방문한 오늘의 민트 칠 위에서도 구분되도록 민트와 겹치지 않는
// 일요일 빨강 잉크(SealInkRed)를 쓴다 — 새 색을 추가하지 않는다. 바퀴끼리 조금씩 어긋나고
// 시작점과 끝점이 맞물리지 않으며 농도도 조금씩 다르다. 숫자 뒤에 그려 글자를 덮지 않는다.
// 값은 실기기 QA로 다듬는 미감 값이다.
private const val VISIT_TODAY_CIRCLE_SEED = 96_400
private val VisitTodayCircleInk = SealInkRed

internal fun visitTodayCircleSeed(date: LocalDate): Int = VISIT_TODAY_CIRCLE_SEED + date.toEpochDay().toInt()

/**
 * 날짜 숫자 글자 상자([widthDp]×[heightDp]) 둘레를 감는 손그림 동그라미 바퀴들(dp, 글자 상자 기준).
 * 같은 날짜(seed)면 항상 같은 모양이다.
 */
internal fun visitTodayPenCircle(widthDp: Float, heightDp: Float, seed: Int): List<VisitDayPenStroke> {
    if (widthDp <= 0f || heightDp <= 0f) return emptyList()
    val random = kotlin.random.Random(seed)
    // 둘째 QA: 숫자보다 살짝 아래로 내려 감싼다.
    val center = Offset(widthDp / 2f, heightDp / 2f + 1.3f)
    // 숫자 글자 상자는 세로로 길어 상자에 맞추면 세로 타원이 된다(첫 QA). 높이·너비 중 큰 쪽에
    // 맞춘 거의 동그란 원으로, 가로를 아주 조금만 더 넓게 둔다.
    val radius = maxOf(heightDp / 2f + 3f, widthDp / 2f + 5.5f, 10f)
    val radiusX = radius * 1.05f
    val radiusY = radius
    // 셋째 QA: 바퀴마다 따로 그은 원 대신, 얇은 펜으로 펜을 떼지 않고 2.4~3.2바퀴 빙빙 휘갈긴
    // 한 줄. 도는 동안 중심이 한쪽으로 밀리고 반지름이 커지거나 작아져 바퀴끼리 벌어지며,
    // 모양도 완전한 타원이 아니라 조금 울퉁불퉁하다.
    val turns = 2.4f + random.nextFloat() * 0.8f
    val start = random.nextFloat() * 2f * Math.PI.toFloat()
    val tilt = Math.toRadians(((random.nextFloat() - 0.5f) * 20f).toDouble()).toFloat()
    val c0 = center + Offset((random.nextFloat() - 0.5f) * 0.8f, (random.nextFloat() - 0.5f) * 0.6f)
    val shift = Offset((random.nextFloat() - 0.5f) * 2.4f, (random.nextFloat() - 0.5f) * 1.8f)
    val scale0 = 0.94f + random.nextFloat() * 0.06f
    val scale1 = 1f + random.nextFloat() * 0.08f
    val (scaleFrom, scaleTo) = if (random.nextBoolean()) scale0 to scale1 else scale1 to scale0
    val lump2 = random.nextFloat() * 6.28f
    val lump3 = random.nextFloat() * 6.28f
    val phase = random.nextFloat() * 6.28f
    val pointsPerTurn = 40
    val total = kotlin.math.ceil(turns * pointsPerTurn).toInt()
    val points = (0..total).map { k ->
        val t = k.toFloat() / total
        val a = start + turns * 2f * Math.PI.toFloat() * t
        val lumpy = 1f + 0.05f * kotlin.math.sin(2f * a + lump2 + 0.6f * t) + 0.025f * kotlin.math.sin(3f * a + lump3)
        val scale = (scaleFrom + (scaleTo - scaleFrom) * t) * lumpy
        val wobble = 0.12f * kotlin.math.sin(t * 17f + phase)
        val x = (radiusX * scale + wobble) * kotlin.math.cos(a)
        val y = (radiusY * scale + wobble) * kotlin.math.sin(a)
        c0 + shift * t + Offset(
            x * kotlin.math.cos(tilt) - y * kotlin.math.sin(tilt),
            x * kotlin.math.sin(tilt) + y * kotlin.math.cos(tilt)
        )
    }
    // 한 줄이지만 바퀴마다 손 힘이 달라 농도가 조금씩 다르다 — 바퀴 단위로 끊어 이어 그린다.
    val width = 0.6f + random.nextFloat() * 0.1f
    return points.indices.step(pointsPerTurn).map { from ->
        VisitDayPenStroke(
            points = points.subList(from, minOf(from + pointsPerTurn + 1, points.size)),
            widthDp = width,
            alpha = 0.5f + random.nextFloat() * 0.3f
        )
    }.filter { it.points.size > 1 }
}

/** 오늘 날짜 숫자에 붙여 [visitTodayPenCircle]을 글자 뒤에 그린다. */
private fun Modifier.visitTodayPenCircle(seed: Int): Modifier = drawWithCache {
    val strokes = visitTodayPenCircle(size.width / density, size.height / density, seed).map { stroke ->
        Triple(
            Path().apply {
                stroke.points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x * density, point.y * density)
                    else lineTo(point.x * density, point.y * density)
                }
            },
            Stroke(width = stroke.widthDp * density, cap = StrokeCap.Round),
            stroke.alpha
        )
    }
    onDrawBehind {
        strokes.forEach { (path, style, alpha) -> drawPath(path, color = VisitTodayCircleInk, alpha = alpha, style = style) }
    }
}

/** 한 달 분량의 날짜 grid만 그린다. [VisitCalendarMonthPage]의 종이 위에 함께 인쇄돼 움직인다. */
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
                                contentDescription = "$date" + (if (date == today) ", 오늘" else "") +
                                    if (visited) ", 방문 기록 있음" else ""
                            }
                        }
                    ) {
                        if (date != null) {
                            // 방문 표시는 1차로 채움 색이 맡는다. 셀 전체를 꽉 채우지 않고
                            // 안쪽에 여백을 둬 습관 트래커의 딱딱한 사각형처럼 보이지 않게 한다.
                            // 96일차: 채움은 민트 중성펜으로 칠한 자국이다(색은 그대로).
                            if (visited) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .fillMaxSize()
                                        .padding(3.dp)
                                        .visitDayGelPenMark(visitDayFillColor(date, today), visitDayPenSeed(date))
                                )
                            }
                            val cellTextColor = if (visited) visitDayFillContrastColor(date, today) else visitDateColor(date)
                            // 오늘 동그라미는 방문 여부와 상관없이 오늘 숫자에만 붙는다.
                            Text(
                                date.dayOfMonth.toString(),
                                modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
                                    .then(if (date == today) Modifier.visitTodayPenCircle(visitTodayCircleSeed(date)) else Modifier),
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
            } else if (weekIndex < weeks.size - 1) {
                // 90일차: 선을 긋지 않는 자리도 같은 높이를 비워 둬 어느 달이든 달력 한 장의
                // 높이가 같다 — 넘길 때 아래 장이 더 짧아 삐져나오거나 장 크기가 흔들리지 않는다.
                Spacer(Modifier.height(4.5.dp))
            }
        }
    }
}

// 90일차: 달력 한 장. 기존 제목 줄(IconButton 48dp) 높이를 그대로 둬, 위에 겹쳐 고정한 ◀ ▶와 줄이 맞는다.
private val VISIT_CALENDAR_PAGE_TITLE_HEIGHT = 48.dp
private val VISIT_CALENDAR_PAGE_TOP_GAP = 6.dp
private val VISIT_CALENDAR_PAGE_HORIZONTAL_PADDING = 6.dp
private val VISIT_CALENDAR_PAGE_BOTTOM_PADDING = 8.dp

// 장의 경계는 윤곽선 없이 종이 자체의 색·질감과 drawer(PaperSurface)의 색 차로만 읽힌다.
// 윤곽선은 카드형 박스처럼 보여 90일차 QA에서 제거했다.

// 96일차: 장 가장자리를 가위로 손질한 종이처럼 아주 미세하게 흔든다. 값은 실기기 QA로 다듬는 미감 값이다.
internal const val VISIT_CALENDAR_PAPER_EDGE_MAX_INSET_DP = 0.5f
private const val VISIT_CALENDAR_PAPER_EDGE_STEP_DP = 4f
private const val VISIT_CALENDAR_PAPER_EDGE_SEED = 96_000

// 종이 밑에 깔리는 접촉 그림자. 장 아래·오른쪽으로 1dp 남짓만 비쳐 거의 느껴지지 않게 둔다.
private val VisitCalendarPaperShadowColor = Color(0xFF3B3226)
private val VISIT_CALENDAR_PAPER_SHADOW_LAYERS = listOf(
    Triple(0.3f, 0.8f, 0.06f), // dx dp, dy dp, alpha
    Triple(0.5f, 1.4f, 0.03f)
)

/** 달마다 다른, 같은 달이면 언제나 같은 가장자리 모양을 고르는 seed. */
internal fun visitCalendarPaperEdgeSeed(month: YearMonth): Int =
    VISIT_CALENDAR_PAPER_EDGE_SEED + month.year * 12 + month.monthValue

/**
 * 손으로 자른 종이 장의 윤곽(dp, 위→오른쪽→아래→왼쪽 순서의 다각형 꼭짓점). 모든 점은 장
 * 사각형 안쪽 0~[maxInsetDp] 띠 안에만 놓여 장의 크기·배치는 그대로고, 종이만 그 안에서 살짝
 * 덜 잘려 나간다. 변마다 긴 물결(가위질 한 번) + 짧은 물결 + 아주 작은 떨림을 섞는다.
 * 같은 seed면 항상 같은 모양이다 — 재구성돼도 가장자리가 흔들리지 않는다.
 */
internal fun visitCalendarPaperEdgeOutline(
    widthDp: Float,
    heightDp: Float,
    seed: Int,
    maxInsetDp: Float = VISIT_CALENDAR_PAPER_EDGE_MAX_INSET_DP
): List<Offset> {
    if (widthDp <= 0f || heightDp <= 0f) return emptyList()
    val random = kotlin.random.Random(seed)
    val edges = listOf(
        Offset(0f, 0f) to Offset(widthDp, 0f),
        Offset(widthDp, 0f) to Offset(widthDp, heightDp),
        Offset(widthDp, heightDp) to Offset(0f, heightDp),
        Offset(0f, heightDp) to Offset(0f, 0f)
    )
    val points = mutableListOf<Offset>()
    edges.forEach { (from, to) ->
        val length = (to - from).getDistance()
        val dir = (to - from) / length
        // 시계 방향으로 돌기 때문에 진행 방향의 오른쪽이 장 안쪽이다.
        val inward = Offset(-dir.y, dir.x)
        val phase1 = random.nextFloat() * 6.28f
        val phase2 = random.nextFloat() * 6.28f
        val freq1 = 0.05f + random.nextFloat() * 0.05f
        val freq2 = 0.25f + random.nextFloat() * 0.15f
        val segments = maxOf(2, kotlin.math.ceil(length / VISIT_CALENDAR_PAPER_EDGE_STEP_DP).toInt())
        for (k in 0 until segments) {
            val d = length * k / segments
            val wave = 0.5f + 0.5f * (0.65f * kotlin.math.sin(d * freq1 + phase1) + 0.35f * kotlin.math.sin(d * freq2 + phase2))
            val jitter = random.nextFloat() * 0.15f
            val inset = (maxInsetDp * (wave * 0.85f + jitter)).coerceIn(0f, maxInsetDp)
            points += from + dir * d + inward * inset
        }
    }
    return points
}

// 96일차 2단계: 장 윗변 가운데에 반쯤 걸쳐 붙인 짧은 마스킹테이프 한 조각. 출첵 민트와 겨루지
// 않게 아이보리~연베이지를 반투명하게 쓴다 — 베이지 종이(평균 약 #E8D4BA) 위에서는 살짝 밝은 띠로,
// drawer 바탕(PaperSurface) 위에서는 살짝 탁한 띠로 읽힌다. 값은 실기기 QA로 다듬는 미감 값이다.
private val VisitCalendarTapeColor = Color(0xFFF6EEDC)
private const val VISIT_CALENDAR_TAPE_ALPHA = 0.62f
private val VisitCalendarTapeFiberColor = Color(0xFFB8A27E)
private const val VISIT_CALENDAR_TAPE_FIBER_WIDTH_DP = 0.35f
private const val VISIT_CALENDAR_TAPE_SEED = 96_200
internal const val VISIT_CALENDAR_TAPE_HEIGHT_DP = 11f
internal const val VISIT_CALENDAR_TAPE_MIN_ANGLE_DEG = 1f
internal const val VISIT_CALENDAR_TAPE_MAX_ANGLE_DEG = 2f
// 테이프 중심은 장 윗변보다 조금 아래 — 위쪽 절반은 장 위 6dp 틈에, 아래쪽은 제목 글자 위 여백에 놓인다.
private const val VISIT_CALENDAR_TAPE_CENTER_Y_DP = 1.5f

/**
 * 테이프 한 조각의 모양(dp). [outline]과 [fibers]는 테이프 중심이 (0, 0)인 자기 좌표이고,
 * 그릴 때 장 윗변 가운데 + [centerOffsetXDp] 위치로 옮겨 [angleDeg]만큼 돌린다.
 * [fibers]는 (시작, 끝, alpha) — 길이 방향으로 지나가는 아주 옅은 종이 섬유.
 */
internal class VisitCalendarTapePiece(
    val outline: List<Offset>,
    val fibers: List<Triple<Offset, Offset, Float>>,
    val lengthDp: Float,
    val angleDeg: Float,
    val centerOffsetXDp: Float
)

internal fun visitCalendarTapeSeed(month: YearMonth): Int =
    VISIT_CALENDAR_TAPE_SEED + month.year * 12 + month.monthValue

/**
 * 손으로 뜯어 붙인 테이프 한 조각. 긴 변은 공장에서 잘린 그대로 거의 곧고, 양 끝만 손으로
 * 찢어 들쭉날쭉하다 — 두 끝은 서로 다른 난수와 기울기를 써서 좌우대칭이 되지 않는다.
 * 길이·위치·각도(±1~2°)는 달마다 조금씩 다르고, 같은 seed면 항상 같은 조각이다.
 */
internal fun visitCalendarTapePiece(seed: Int): VisitCalendarTapePiece {
    val random = kotlin.random.Random(seed)
    val length = 40f + random.nextFloat() * 8f
    val halfL = length / 2f
    val halfH = VISIT_CALENDAR_TAPE_HEIGHT_DP / 2f
    val angleSign = if (random.nextBoolean()) 1f else -1f
    val angle = angleSign * (VISIT_CALENDAR_TAPE_MIN_ANGLE_DEG +
        random.nextFloat() * (VISIT_CALENDAR_TAPE_MAX_ANGLE_DEG - VISIT_CALENDAR_TAPE_MIN_ANGLE_DEG))
    val centerOffsetX = (random.nextFloat() - 0.5f) * 6f

    fun straightEdge(fromX: Float, toX: Float, y: Float): List<Offset> {
        val steps = maxOf(2, kotlin.math.ceil(kotlin.math.abs(toX - fromX) / 4f).toInt())
        return (0 until steps).map { k ->
            Offset(fromX + (toX - fromX) * k / steps, y + (random.nextFloat() - 0.5f) * 0.16f)
        }
    }

    // 찢긴 끝: 위→아래(또는 아래→위)로 1dp 남짓 간격의 톱니. inwardSign은 테이프 안쪽 방향.
    fun tornEnd(x: Float, fromY: Float, toY: Float, inwardSign: Float): List<Offset> {
        val slant = (random.nextFloat() - 0.5f) * 2f // 끝 전체가 비스듬히 찢긴 정도(dp)
        val steps = 8 + random.nextInt(4)
        return (0 until steps).map { k ->
            val t = k.toFloat() / steps
            val y = fromY + (toY - fromY) * t
            val tooth = random.nextFloat() * 1.4f
            Offset(x + inwardSign * (tooth + (slant * (t - 0.5f)).coerceAtLeast(-0.6f) + 0.6f), y)
        }
    }

    val outline = buildList {
        addAll(straightEdge(-halfL, halfL, -halfH))
        addAll(tornEnd(halfL, -halfH, halfH, inwardSign = -1f))
        addAll(straightEdge(halfL, -halfL, halfH))
        addAll(tornEnd(-halfL, halfH, -halfH, inwardSign = 1f))
    }

    val fibers = List(5 + random.nextInt(4)) {
        val y = -halfH + 1.2f + random.nextFloat() * (VISIT_CALENDAR_TAPE_HEIGHT_DP - 2.4f)
        val fiberLength = 6f + random.nextFloat() * 14f
        val startX = -halfL + 3f + random.nextFloat() * (length - 6f - fiberLength).coerceAtLeast(0f)
        val tilt = (random.nextFloat() - 0.5f) * 0.8f
        Triple(
            Offset(startX, y),
            Offset(startX + fiberLength, (y + tilt).coerceIn(-halfH + 0.8f, halfH - 0.8f)),
            0.05f + random.nextFloat() * 0.06f
        )
    }
    return VisitCalendarTapePiece(outline, fibers, length, angle, centerOffsetX)
}

/** [visitCalendarTapePiece]를 px path로. drawWithCache에서 장 크기가 정해질 때만 만든다. */
private fun visitCalendarTapePath(tape: VisitCalendarTapePiece, density: Float): Path = Path().apply {
    tape.outline.forEachIndexed { index, point ->
        if (index == 0) moveTo(point.x * density, point.y * density)
        else lineTo(point.x * density, point.y * density)
    }
    close()
}

/** 장 윗변 가운데에 테이프를 붙인다. 장 위쪽 틈까지 걸쳐 그리며, 장과 함께 넘어간다. */
private fun DrawScope.drawVisitCalendarTape(tape: VisitCalendarTapePiece, tapePath: Path) {
    val centerX = size.width / 2f + tape.centerOffsetXDp * density
    val centerY = VISIT_CALENDAR_TAPE_CENTER_Y_DP * density
    withTransform({
        translate(left = centerX, top = centerY)
        rotate(degrees = tape.angleDeg, pivot = Offset.Zero)
    }) {
        drawPath(tapePath, color = VisitCalendarTapeColor, alpha = VISIT_CALENDAR_TAPE_ALPHA)
        tape.fibers.forEach { (from, to, alpha) ->
            drawLine(
                color = VisitCalendarTapeFiberColor,
                start = from * density,
                end = to * density,
                strokeWidth = VISIT_CALENDAR_TAPE_FIBER_WIDTH_DP * density,
                cap = StrokeCap.Round,
                alpha = alpha
            )
        }
    }
}

/**
 * 종이 한 장에 인쇄된 것처럼 함께 움직이는 달력 한 달. 종이 bitmap은 호출부가 한 번만
 * 불러와 넘기고, 여기서는 늘리지 않고 비율을 유지한 채 장을 꽉 채우도록 가운데를 잘라 그린다.
 * 96일차: 종이는 [visitCalendarPaperEdgeOutline] 모양으로 오려 그리고(경계 안티에일리어싱을 위해
 * clip 대신 종이 무늬를 채운 path), 그 밑에 아주 옅은 접촉 그림자를 깐다.
 * 이 composable 안에서는 파라미터로 받은 [month]만 읽는다.
 */
@Composable
private fun VisitCalendarMonthPage(
    month: YearMonth,
    paper: ImageBitmap,
    visitedEpochDays: Set<Long>,
    today: LocalDate,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawWithCache {
                val dstWidth = size.width.toInt()
                val dstHeight = size.height.toInt()
                val scale = maxOf(dstWidth.toFloat() / paper.width, dstHeight.toFloat() / paper.height)
                val srcWidth = (dstWidth / scale).toInt().coerceIn(1, paper.width)
                val srcHeight = (dstHeight / scale).toInt().coerceIn(1, paper.height)
                val srcOffset = IntOffset((paper.width - srcWidth) / 2, (paper.height - srcHeight) / 2)
                // 가운데를 잘라 장에 맞추던 기존 계산을 그대로 종이 무늬의 위치·배율로 옮긴다.
                val paperShader = ImageShader(paper).apply {
                    setLocalMatrix(
                        android.graphics.Matrix().apply {
                            setTranslate(-srcOffset.x.toFloat(), -srcOffset.y.toFloat())
                            postScale(dstWidth.toFloat() / srcWidth, dstHeight.toFloat() / srcHeight)
                        }
                    )
                }
                val paperBrush = ShaderBrush(paperShader)
                val outline = Path().apply {
                    visitCalendarPaperEdgeOutline(
                        widthDp = size.width / density,
                        heightDp = size.height / density,
                        seed = visitCalendarPaperEdgeSeed(month)
                    ).forEachIndexed { index, point ->
                        if (index == 0) moveTo(point.x * density, point.y * density)
                        else lineTo(point.x * density, point.y * density)
                    }
                    close()
                }
                val tape = visitCalendarTapePiece(visitCalendarTapeSeed(month))
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
            .padding(
                start = VISIT_CALENDAR_PAGE_HORIZONTAL_PADDING,
                end = VISIT_CALENDAR_PAGE_HORIZONTAL_PADDING,
                bottom = VISIT_CALENDAR_PAGE_BOTTOM_PADDING
            )
    ) {
        Text(
            text = "${month.year}년 ${month.monthValue}월",
            color = InkPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            // 근거리는 좌우 화살표, 원거리는 이 제목을 눌러 월/연도 grid로.
            modifier = Modifier
                .fillMaxWidth()
                .height(VISIT_CALENDAR_PAGE_TITLE_HEIGHT)
                .wrapContentHeight(Alignment.CenterVertically)
                .clickable(onClick = onTitleClick)
                .semantics { heading() }
        )
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
        VisitCalendarMonthGrid(month = month, visitedEpochDays = visitedEpochDays, today = today)
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

    // 달력 종이는 여기서 한 번만 불러와 모든 장이 같은 bitmap을 나눠 쓴다. 월 이동
    // AnimatedContent 안에서 불러오면 달이 바뀔 때마다 새 장이 다시 decode한다.
    val calendarPaper = ImageBitmap.imageResource(R.drawable.visit_calendar_paper)

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
                        // 90일차: "다녀간 날들 / 오늘" 줄은 종이에 인쇄된 것이 아니라 종이 밖의
                        // 고정 안내·조작부라, 달력 한 장 위로 올려 두고 월 이동과 함께 움직이지 않는다.
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
                        Spacer(Modifier.height(VISIT_CALENDAR_PAGE_TOP_GAP))
                        // 90일차: 종이·제목·장식·요일·날짜 grid를 달력 한 장(VisitCalendarMonthPage)으로
                        // 묶어 월 이동 때 한 몸으로 움직인다. ◀ ▶는 인쇄물이 아닌 조작부라 종이 위에
                        // 겹쳐 고정해 두고, 가운데 빈 곳의 터치는 아래 제목으로 그대로 내려간다.
                        Box(Modifier.fillMaxWidth()) {
                            val monthTransition = updateTransition(displayedMonth, label = "visitCalendarMonthPage")
                            monthTransition.AnimatedContent(
                                transitionSpec = { visitCalendarPageTurnTransition() },
                                modifier = Modifier.fillMaxWidth()
                            ) { month ->
                                // 넘기는 도중에만 두 상태가 다르다. 다음 달로 가는지는 월 값 비교가
                                // 정하고, animation은 그 결과를 표현만 한다.
                                val forward = monthTransition.targetState > monthTransition.currentState
                                // 나가는 장은 자기 [month]만 읽는다 — displayedMonth를 여기서 읽으면
                                // 넘어가는 도중 다음 달 내용으로 바뀐다.
                                VisitCalendarMonthPage(
                                    month = month,
                                    modifier = visitCalendarPageTurnModifier(forward),
                                    paper = calendarPaper,
                                    visitedEpochDays = visitedDaysForMonth(
                                        month = month,
                                        currentMonth = todayYearMonth,
                                        currentMonthVisitedDays = visitedEpochDays,
                                        loadedByMonth = loadedVisitsByMonth
                                    ),
                                    today = today,
                                    onTitleClick = {
                                        pickerYear = displayedMonth.year
                                        onNavLevelChange(VisitCalendarNavLevel.MONTH_PICKER)
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().height(VISIT_CALENDAR_PAGE_TITLE_HEIGHT),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 96일차 추가: 터치 영역·클릭·접근성 설명은 그대로, 보이는 화살표만 신문 오림.
                                IconButton(onClick = { displayedMonth = displayedMonth.minusMonths(1) }) {
                                    Box(
                                        Modifier.size(VISIT_CALENDAR_ARROW_SIZE)
                                            .semantics { contentDescription = "이전 달" }
                                            .visitCalendarNewsprintArrowMark(pointsLeft = true, seed = VISIT_CALENDAR_ARROW_SEED_PREVIOUS)
                                    )
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { displayedMonth = displayedMonth.plusMonths(1) }) {
                                    Box(
                                        Modifier.size(VISIT_CALENDAR_ARROW_SIZE)
                                            .semantics { contentDescription = "다음 달" }
                                            .visitCalendarNewsprintArrowMark(pointsLeft = false, seed = VISIT_CALENDAR_ARROW_SEED_NEXT)
                                    )
                                }
                            }
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
                                paper = calendarPaper,
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
                                paper = calendarPaper,
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
