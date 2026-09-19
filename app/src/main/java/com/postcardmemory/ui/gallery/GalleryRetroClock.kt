package com.postcardmemory.ui.gallery

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperTray
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val RETRO_CLOCK_MONTH_ABBR = listOf(
    "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
    "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
)

private val RETRO_CLOCK_DOW_ABBR = mapOf(
    DayOfWeek.MONDAY to "MON", DayOfWeek.TUESDAY to "TUE", DayOfWeek.WEDNESDAY to "WED",
    DayOfWeek.THURSDAY to "THU", DayOfWeek.FRIDAY to "FRI", DayOfWeek.SATURDAY to "SAT",
    DayOfWeek.SUNDAY to "SUN"
)

private val RETRO_CLOCK_DOW_KOREAN = mapOf(
    DayOfWeek.MONDAY to "월", DayOfWeek.TUESDAY to "화", DayOfWeek.WEDNESDAY to "수",
    DayOfWeek.THURSDAY to "목", DayOfWeek.FRIDAY to "금", DayOfWeek.SATURDAY to "토",
    DayOfWeek.SUNDAY to "일"
)

/** hh:mm(12시간제, zero-padded) / ss / AM·PM으로 쪼갠 시간 표시 텍스트. 한 가로선에 정렬하되
 * 셋의 글자 크기는 서로 다르게 쓰기 위해 미리 분리해 둔다. */
internal data class RetroClockTimeText(val hourMinute: String, val second: String, val meridiem: String)

/**
 * 24시 → 12시 변환. 자정(0시)=12, 정오(12시)=12로 접히는 경계가 핵심이라, 화면 표시
 * ([retroClockTimeTextFor])와 접근성 설명([retroClockAccessibilityDescriptionFor])이
 * 서로 다른 값을 말하는 일이 없도록 한 곳에서만 계산한다.
 */
private fun retroClockHour12(hour: Int): Int = when (val h = hour % 12) {
    0 -> 12
    else -> h
}

/** 12시간제 변환. 자정(0시)=12AM, 정오(12시)=12PM 경계를 포함해 순수 함수로 검증 가능하다. */
internal fun retroClockTimeTextFor(time: LocalTime): RetroClockTimeText {
    val hour12 = retroClockHour12(time.hour)
    val hourMinute = "${hour12.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    val second = time.second.toString().padStart(2, '0')
    val meridiem = if (time.hour < 12) "AM" else "PM"
    return RetroClockTimeText(hourMinute, second, meridiem)
}

/** "2026 SEP 18 FRI" 형식의 날짜 한 줄. 연도 → 월(영문 약어) → 일 → 요일(영문 약어) 순서다. */
internal fun retroClockDateTextFor(date: LocalDate): String {
    val month = RETRO_CLOCK_MONTH_ABBR[date.monthValue - 1]
    val day = date.dayOfMonth.toString().padStart(2, '0')
    val dow = RETRO_CLOCK_DOW_ABBR.getValue(date.dayOfWeek)
    return "${date.year} $month $day $dow"
}

/** 화면에는 영어 약어를 쓰지만 접근성 설명은 자연스러운 한국어 한 문장으로 제공한다. */
internal fun retroClockAccessibilityDescriptionFor(dateTime: LocalDateTime): String {
    val hour12 = retroClockHour12(dateTime.hour)
    val meridiemKo = if (dateTime.hour < 12) "오전" else "오후"
    val dowKo = RETRO_CLOCK_DOW_KOREAN.getValue(dateTime.dayOfWeek)
    return "현재 시간 ${meridiemKo} ${hour12}시 ${dateTime.minute}분 ${dateTime.second}초, " +
        "${dateTime.year}년 ${dateTime.monthValue}월 ${dateTime.dayOfMonth}일 ${dowKo}요일"
}

// ══════════════ 7세그먼트 숫자 렌더러 ══════════════
// 77일차 추가 v2: "막대기 DIY 숫자" 금지 지시에 맞춰, 각 세그먼트 끝을 뾰족하게 다듬은
// 육각형(hexagon) Path로 그린다 — 단순 직사각형 7개를 붙인 조립품처럼 안 보이게 하는
// 핵심 장치다. `docs/ai/mockups/gallery-retro-clock-mockup.html`에서 같은 비율의
// CSS clip-path로 0~9 전체를 직접 캡처해 확인한 모양을 그대로 Path로 옮겼다.

/** 7세그먼트 on 상태 세그먼트 집합(a~g). Path/Canvas 없이도 검증 가능한 순수 데이터다. */
internal val SEVEN_SEGMENT_PATTERNS: Map<Char, Set<Char>> = mapOf(
    '0' to setOf('a', 'b', 'c', 'd', 'e', 'f'),
    '1' to setOf('b', 'c'),
    '2' to setOf('a', 'b', 'd', 'e', 'g'),
    '3' to setOf('a', 'b', 'c', 'd', 'g'),
    '4' to setOf('b', 'c', 'f', 'g'),
    '5' to setOf('a', 'c', 'd', 'f', 'g'),
    '6' to setOf('a', 'c', 'd', 'e', 'f', 'g'),
    '7' to setOf('a', 'b', 'c'),
    '8' to setOf('a', 'b', 'c', 'd', 'e', 'f', 'g'),
    '9' to setOf('a', 'b', 'c', 'd', 'f', 'g')
)

/**
 * a(위)/g(중간)/d(아래) 가로 세그먼트와 b·c·e·f 세로 세그먼트를, 끝이 뾰족한 육각형으로
 * 그릴 좌표를 계산한다. 단순 사각 막대가 아니라 실제 LCD 글리프처럼 보이게 하는 부분이다.
 */
private fun sevenSegmentPaths(width: Float, height: Float, thickness: Float): Map<Char, Path> {
    val horizWidth = width - thickness * 0.9f
    val horizInsetX = thickness * 0.45f
    fun horizontalPath(y: Float): Path = Path().apply {
        moveTo(horizInsetX, y + thickness * 0.5f)
        lineTo(horizInsetX + horizWidth * 0.22f, y)
        lineTo(horizInsetX + horizWidth * 0.78f, y)
        lineTo(horizInsetX + horizWidth, y + thickness * 0.5f)
        lineTo(horizInsetX + horizWidth * 0.78f, y + thickness)
        lineTo(horizInsetX + horizWidth * 0.22f, y + thickness)
        close()
    }
    val vertHeight = height / 2f - thickness * 0.68f
    fun verticalPath(x: Float, y: Float): Path = Path().apply {
        moveTo(x + thickness * 0.5f, y)
        lineTo(x + thickness, y + vertHeight * 0.22f)
        lineTo(x + thickness, y + vertHeight * 0.78f)
        lineTo(x + thickness * 0.5f, y + vertHeight)
        lineTo(x, y + vertHeight * 0.78f)
        lineTo(x, y + vertHeight * 0.22f)
        close()
    }
    return mapOf(
        'a' to horizontalPath(0f),
        'g' to horizontalPath(height / 2f - thickness / 2f),
        'd' to horizontalPath(height - thickness),
        'f' to verticalPath(0f, thickness * 0.34f),
        'b' to verticalPath(width - thickness, thickness * 0.34f),
        'e' to verticalPath(0f, height / 2f + thickness * 0.34f),
        'c' to verticalPath(width - thickness, height / 2f + thickness * 0.34f)
    )
}

// 꺼진 세그먼트도 아주 흐리게 남겨서(진짜 LCD의 "유령상") 숫자 하나가 완성된 글리프처럼
// 읽히게 한다 — 발광이 아니라 구조감을 가져오는 장치다(지시서 14절: 발광 복제가 목표가 아님).
private val RetroClockSegmentOffColor = InkPrimary.copy(alpha = 0.09f)

@Composable
private fun SevenSegmentDigit(char: Char, width: Dp, height: Dp, thickness: Dp, onColor: Color) {
    Canvas(modifier = Modifier.size(width, height)) {
        val onSegments = SEVEN_SEGMENT_PATTERNS[char] ?: SEVEN_SEGMENT_PATTERNS.getValue('8')
        sevenSegmentPaths(width.toPx(), height.toPx(), thickness.toPx()).forEach { (name, path) ->
            drawPath(path, color = if (name in onSegments) onColor else RetroClockSegmentOffColor)
        }
    }
}

@Composable
private fun SevenSegmentColon(height: Dp, dotSize: Dp, gap: Dp, color: Color) {
    Column(
        modifier = Modifier.height(height).width(dotSize),
        verticalArrangement = Arrangement.spacedBy(gap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(dotSize).background(color, CircleShape))
        Box(Modifier.size(dotSize).background(color, CircleShape))
    }
}

// 숫자가 얹히는 저채도 LCD 패널. 새 색을 만들지 않고 InkSecondary를 아주 낮은 alpha로
// 깔아 크림색 바디([PaperTray])와 "화면" 영역만 구분한다.
private val RetroClockPanelColor = InkSecondary.copy(alpha = 0.10f)

// 큰 자리(hh:mm)와 작은 자리(ss) 크기. 목업에서 확인한 비율을 그대로 옮겼다.
private val RetroClockLargeDigitWidth = 15.dp
private val RetroClockLargeDigitHeight = 26.dp
private val RetroClockLargeDigitThickness = 3.4.dp
private val RetroClockSmallDigitWidth = 10.dp
private val RetroClockSmallDigitHeight = 17.dp
private val RetroClockSmallDigitThickness = 2.4.dp

/** 시계 화면(hh:mm / ss / AM·PM 한 줄 + 날짜 한 줄)만 그린다. 폭 제약을 걸지 않아
 * 내부 글자 폭에 맞춰 스스로 닫힌다(v3, "판넬처럼 늘어나 보임" 피드백 반영). */
@Composable
private fun GalleryRetroClockFace(timeText: RetroClockTimeText, dateText: String, modifier: Modifier = Modifier) {
    // 콘텐츠가 위로 치우쳐 보인다는 피드백으로, 바디 전체 높이(프레임+패널 상하 여백의 합)는
    // 그대로 두고 위/아래 여백 배분만 8dp만큼 아래로 옮긴다 — 프레임 4/4dp→6/2dp, 패널
    // 7/7dp→13/1dp(각각 2dp+6dp=8dp). 요소 사이 Spacer(4dp/3dp)와 가로 정렬은 손대지 않는다.
    Box(
        modifier = modifier
            .background(PaperTray, RoundedCornerShape(14.dp))
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp)
    ) {
        Column(
            // IntrinsicSize.Min: 안쪽 HorizontalDivider의 기본 fillMaxWidth()가 상위에서
            // 내려온 화면 전체 폭까지 다시 늘어나 버리는 것을 막는다 — 이 폭 계산이 없으면
            // 바디 폭 제약을 없앤 의미가 사라지고 v2와 같은 배너 폭으로 되돌아간다.
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .background(RetroClockPanelColor, RoundedCornerShape(10.dp))
                .padding(start = 10.dp, end = 10.dp, top = 13.dp, bottom = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    timeText.hourMinute.filter { it != ':' }.forEachIndexed { index, digit ->
                        if (index == 2) {
                            SevenSegmentColon(
                                height = RetroClockLargeDigitHeight,
                                dotSize = 3.dp,
                                gap = 5.dp,
                                color = InkPrimary
                            )
                        }
                        SevenSegmentDigit(
                            char = digit,
                            width = RetroClockLargeDigitWidth,
                            height = RetroClockLargeDigitHeight,
                            thickness = RetroClockLargeDigitThickness,
                            onColor = InkPrimary
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    timeText.second.forEach { digit ->
                        SevenSegmentDigit(
                            char = digit,
                            width = RetroClockSmallDigitWidth,
                            height = RetroClockSmallDigitHeight,
                            thickness = RetroClockSmallDigitThickness,
                            onColor = InkSecondary
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                // 지시서 17절: PM은 "절제된 보조 텍스트" 방향을 택함 — 메인 시간보다 튀지
                // 않게 일반 텍스트로 유지하고, 우측 끝에 붙이지 않고 ss 바로 옆 고정 간격에
                // 둬 hh:mm/ss/PM이 한 시간 정보 묶음으로 읽히게 한다. 바디 자체는 이 묶음
                // 폭에 맞춰 닫히므로(v3) PM 뒤에 별도로 남겨두는 여백은 없다.
                Text(
                    text = timeText.meridiem,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = InkSecondary,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(thickness = 0.5.dp, color = PaperDivider)
            Spacer(Modifier.height(3.dp))
            Text(
                text = dateText,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = InkSecondary,
                letterSpacing = 0.6.sp
            )
        }
    }
}

// 시계 오른쪽 여백을 정리해주는 작은 동반자. 컵 몸체 + 손잡이 + 김 두 줄만 — 라떼아트·
// 표정·반짝임 등 장식은 넣지 않는다. 다른 갤러리 아이콘([PondDrawerIcon] 등)과 같은
// stroke 기반 ImageVector.Builder 패턴을 그대로 따른다.
private val RetroClockCoffeeCupIcon: ImageVector =
    ImageVector.Builder(
        name = "RetroClockCoffeeCupIcon",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 컵 몸체
            moveTo(4f, 9f)
            lineTo(17f, 9f)
            lineTo(17f, 15f)
            quadTo(17f, 20f, 12f, 20f)
            lineTo(9f, 20f)
            quadTo(4f, 20f, 4f, 15f)
            close()
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // 손잡이
            moveTo(17f, 10.5f)
            lineTo(19.2f, 10.5f)
            quadTo(21.5f, 10.5f, 21.5f, 12.8f)
            quadTo(21.5f, 15.1f, 19.2f, 15.1f)
            lineTo(17f, 15.1f)
        }
        path(
            fill = SolidColor(Color.Transparent),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            strokeAlpha = 0.55f
        ) {
            // 김 두 줄 (최소한의 표현만)
            moveTo(8.3f, 8.6f)
            quadTo(9.7f, 7.0f, 8.3f, 5.6f)
            quadTo(6.9f, 4.2f, 8.3f, 2.6f)

            moveTo(12f, 8.6f)
            quadTo(13.4f, 7.0f, 12f, 5.6f)
            quadTo(10.6f, 4.2f, 12f, 2.6f)
        }
    }.build()

// 시계 바디 옆 커피잔 크기. 바디보다 확실히 작아 "동반자"로 읽히는 선.
private val RETRO_CLOCK_CUP_SIZE = 22.dp

/**
 * 메인 갤러리 상단의 작은 레트로 디지털 탁상시계 + 커피잔. 네온·유광·그림자·badge·장식문구
 * 없이, "hh:mm은 크게, ss·AM/PM은 작지만 같은 가로선, 날짜는 가장 작게 아래 줄"이라는 정보
 * 위계와 "작은 기계" 비율만으로 옛날 탁상시계의 문법을 옮긴다.
 *
 * 77일차 추가 v2: 시계 폭이 화면을 거의 다 먹어 배너처럼 보인다는 피드백을 받아, 원인(내부
 * `Spacer(Modifier.weight(1f))`가 상위 `Row(fillMaxWidth())`의 화면 전체 너비를 그대로
 * 상속)을 고치고 시계 폭을 화면 폭의 고정 비율로 한 번 제한했다. 숫자는 Text가 아니라 직접
 * 그린 7세그먼트 [Path]로 바꿔 "진짜 디지털 시계" 인상을 냈다.
 *
 * 77일차 추가 v3: 고정 비율(화면 폭의 58%)조차 실제 숫자 폭과 무관하게 바디를 늘려 "억지로
 * 당긴 판넬"처럼 보인다는 피드백을 받아, [GalleryRetroClockFace]에서 폭 제약을 완전히
 * 제거했다 — 바디는 이제 내부 hh:mm/ss/PM 묶음과 날짜 줄 중 더 넓은 쪽 글자 폭에만 맞춰
 * 감싸듯 닫힌다(`Box`/`Column` 기본 wrap-content). 두 줄은 [Alignment.CenterHorizontally]로
 * 가운데 정렬해 폭이 다른 두 줄이 한 몸체 안에 자연스럽게 들어앉게 했다.
 * (`docs/ai/mockups/gallery-retro-clock-mockup.html`에서 Chrome headless로 캡처해
 * 사용자 확인을 받은 디자인을 그대로 옮김.)
 *
 * 숫자는 새 폰트를 추가하지 않고 Canvas로 직접 그린다(1순위였던 "프로젝트에 이미 있는
 * 7세그 폰트/자산"은 조사 결과 없었고, 새 폰트 리소스 추가도 하지 않음 — 13·16절).
 * 색은 바디(크림 [PaperTray])와 화면(`InkSecondary`를 낮은 alpha로 얹은 저채도 패널)만
 * 구분하고 새 색상을 만들지 않는다.
 *
 * 시간 상태는 이 composable 안에서만 `remember`+`LaunchedEffect`로 매초(정확히는 다음 초
 * 경계까지 delay) 갱신한다 — 연못 파문([PondRippleOverlay])과 같은 "로컬 상태·로컬 루프"
 * 패턴이라 갤러리 화면 전체가 매초 다시 그려지지 않는다. `LaunchedEffect`는 Activity의
 * lifecycle-aware recomposer 위에서 돌아 백그라운드 진입 시 자동으로 멈추므로 별도 lifecycle
 * 처리를 추가하지 않았다.
 */
@Composable
internal fun GalleryRetroClock(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            now = LocalDateTime.now()
            val millisIntoSecond = now.nano / 1_000_000
            delay((1000 - millisIntoSecond).toLong())
        }
    }

    val timeText = retroClockTimeTextFor(now.toLocalTime())
    val dateText = retroClockDateTextFor(now.toLocalDate())

    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = retroClockAccessibilityDescriptionFor(now)
        }
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GalleryRetroClockFace(
                timeText = timeText,
                dateText = dateText
            )
            Icon(
                imageVector = RetroClockCoffeeCupIcon,
                contentDescription = null,
                tint = InkSecondary,
                modifier = Modifier.size(RETRO_CLOCK_CUP_SIZE)
            )
        }
        Spacer(Modifier.height(6.dp))
        // "선반 위 물건" 느낌만 주는, 존재감을 최소화한 얇은 공유 선반선.
        HorizontalDivider(thickness = 0.5.dp, color = PaperDivider.copy(alpha = 0.6f))
    }
}
