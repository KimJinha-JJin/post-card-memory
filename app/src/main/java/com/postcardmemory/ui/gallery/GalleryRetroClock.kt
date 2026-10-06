package com.postcardmemory.ui.gallery

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.R
import com.postcardmemory.ui.theme.InkPrimary
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin

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

// 큰 자리(hh:mm)와 작은 자리(ss) 크기. 목업에서 확인한 비율을 그대로 옮겼다.
private val RetroClockLargeDigitWidth = 15.dp
private val RetroClockLargeDigitHeight = 26.dp
private val RetroClockLargeDigitThickness = 3.4.dp
private val RetroClockSmallDigitWidth = 10.dp
private val RetroClockSmallDigitHeight = 17.dp
private val RetroClockSmallDigitThickness = 2.4.dp

/** 시계 화면(hh:mm / ss / AM·PM 한 줄 + 날짜 한 줄)만 그린다. 폭 제약을 걸지 않아
 * 내부 글자 폭에 맞춰 스스로 닫힌다(v3, "판넬처럼 늘어나 보임" 피드백 반영).
 * 95일차: 바디·LCD 패널 바탕은 오린 시계 이미지([R.drawable.home_clock_collage_body])의
 * 숫자창이 맡으므로 여기서는 바탕색·여백 없이 글자만 그린다. */
@Composable
private fun GalleryRetroClockFace(timeText: RetroClockTimeText, dateText: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Column(
            // IntrinsicSize.Min: 안쪽 HorizontalDivider의 기본 fillMaxWidth()가 상위에서
            // 내려온 화면 전체 폭까지 다시 늘어나 버리는 것을 막는다 — 이 폭 계산이 없으면
            // 바디 폭 제약을 없앤 의미가 사라지고 v2와 같은 배너 폭으로 되돌아간다.
            modifier = Modifier.width(IntrinsicSize.Min),
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
                // 기본 본문 줄높이(24sp)를 그대로 두면 날짜 한 줄이 글자보다 훨씬 큰 칸을
                // 차지해 시계 이미지 숫자창(높이 약 44dp)을 넘친다. 글자 크기·색은 그대로다.
                lineHeight = 10.sp,
                color = InkSecondary,
                letterSpacing = 0.6.sp
            )
        }
    }
}

// ══════════════ 95일차 잡지 오림 장면 ══════════════
// "한 손엔 커피, 다른 손은 알람시계 스누즈 버튼 위"인 아침을 정적 오림 이미지 세 장으로
// 붙인다. 손과 컵은 정적이고, 컵 위의 옅은 김만 움직인다. 클릭·눌림 상태는 없다.
// 아래 좌표는 모두 drawable-nodpi 원본의 실측 픽셀을 화면 dp로 환산한 값이라, 이미지를
// 바꾸면 다시 재야 한다.

// 시계 몸체 이미지(1695×928px)의 화면 크기.
private val RETRO_CLOCK_BODY_WIDTH = 168.dp
private val RETRO_CLOCK_BODY_HEIGHT = 92.dp

// 몸체 이미지 안의 밝은 숫자창(원본 x 145~1567px, y 290~730px). 7세그 숫자와 날짜를
// 이 칸 한가운데에 둔다.
private val RETRO_CLOCK_WINDOW_START = 14.5.dp
private val RETRO_CLOCK_WINDOW_TOP = 28.6.dp
private val RETRO_CLOCK_WINDOW_WIDTH = 141.dp
private val RETRO_CLOCK_WINDOW_HEIGHT = 44.dp

// 교체된 왼손(1448×1086px)의 원본 비율을 유지한다.
private val RETRO_CLOCK_HAND_WIDTH = 112.dp
private val RETRO_CLOCK_HAND_HEIGHT = 84.dp
private val RETRO_CLOCK_HAND_X = (-8).dp
private val RETRO_CLOCK_HAND_Y = (-5).dp
private val RETRO_CLOCK_SCENE_CLOCK_INSET = 16.dp

// 손이 몸체보다 왼쪽·위로 나와 있는 만큼을 시계 묶음 안에 미리 비워 둔다 — Row 밖으로
// 삐져나가면 Scaffold가 topBar를 본문 위에 그려 아래 목록이나 위 메뉴 버튼을 덮는다.
// 시계 위치는 유지하고 손만 화면 경계에서 자른다. 검지 끝은 화면 약 (99.5,38.3)dp로
// 스누즈 버튼(x90~124dp, y39~48dp) 위에 닿는다. 숫자창은 x84.5dp, y58.6dp부터다.
private val RETRO_CLOCK_HAND_LEAD = 54.dp
private val RETRO_CLOCK_HAND_RISE = 30.dp

// 교체된 오른손+컵(1086×1448px)을 확대하고 원본 비율을 유지한다.
private val RETRO_CLOCK_CUP_WIDTH = 105.28.dp
private val RETRO_CLOCK_CUP_HEIGHT = 140.373.dp

// 이미지의 손목 끝을 그대로 노출하면 공중에 떠 보인다. 하단 약 32dp를 장면 안에서
// 잘라 손목이 바로 아래 선으로 이어지게 한다. 잘린 부분은 Row 밖에 그리지 않는다.
private val RETRO_CLOCK_CUP_VIEWPORT_HEIGHT = 108.dp

/** 이미지 위쪽 투명 여백 안에서만 천천히 올라가는 두 줄의 김. 상태는 draw에서 읽는다. */
@Composable
private fun GalleryCoffeeSteam(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "coffeeSteam")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "coffeeSteamRise"
    )
    Canvas(modifier) {
        repeat(2) { index ->
            val progress = (phase.value + index * 0.5f) % 1f
            val alpha = sin(PI * progress).toFloat() * 0.13f
            val sway = sin(2 * PI * progress).toFloat() * 0.7.dp.toPx()
            val x = (if (index == 0) 25.dp else 43.dp).toPx()
            val bottom = 23.dp.toPx() - progress * 8.dp.toPx()
            val path = Path().apply {
                moveTo(x, bottom)
                quadraticBezierTo(
                    x + sway, bottom - 5.dp.toPx(),
                    x + sway * 0.5f, bottom - 11.dp.toPx()
                )
            }
            drawPath(
                path = path,
                color = InkSecondary.copy(alpha = alpha),
                style = Stroke(width = 0.65.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

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
 * 95일차: 크림색 바디·LCD 패널·선 아이콘 커피잔·흔들리는 김을 걷어내고, 시계 몸체·스누즈
 * 버튼 위 왼손·커피잔을 든 오른손을 잡지 오림 이미지로 바꿨다. 숫자·날짜·시간 갱신은 그대로다.
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
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(RETRO_CLOCK_HAND_RISE + RETRO_CLOCK_BODY_HEIGHT)
                .clipToBounds()
        ) {
            // 몸체 → 숫자 → 왼손 순으로 겹친다. 손은 숫자창 위쪽 띠에만 얹혀 숫자를 가리지 않는다.
            Box(
                modifier = Modifier.align(Alignment.BottomStart)
                    .padding(start = RETRO_CLOCK_SCENE_CLOCK_INSET)
            ) {
                Image(
                    painter = painterResource(R.drawable.home_clock_collage_body),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = RETRO_CLOCK_HAND_LEAD, top = RETRO_CLOCK_HAND_RISE)
                        .size(RETRO_CLOCK_BODY_WIDTH, RETRO_CLOCK_BODY_HEIGHT)
                )
                Box(
                    modifier = Modifier
                        .padding(
                            start = RETRO_CLOCK_HAND_LEAD + RETRO_CLOCK_WINDOW_START,
                            top = RETRO_CLOCK_HAND_RISE + RETRO_CLOCK_WINDOW_TOP
                        )
                        .size(RETRO_CLOCK_WINDOW_WIDTH, RETRO_CLOCK_WINDOW_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    // unbounded: 큰 글꼴 설정에서 날짜 줄이 커져도 숫자창 높이에 눌려
                    // 잘리지 않고 가운데 기준으로만 넘치게 한다.
                    GalleryRetroClockFace(
                        timeText = timeText,
                        dateText = dateText,
                        modifier = Modifier.wrapContentSize(unbounded = true)
                    )
                }
            }
            // 손목 쪽을 화면 끝에서 자르고, 위쪽도 장면의 clip 안에만 그린다.
            Image(
                painter = painterResource(R.drawable.home_clock_left_snooze_hand),
                contentDescription = null,
                modifier = Modifier
                    .offset(x = RETRO_CLOCK_HAND_X, y = RETRO_CLOCK_HAND_Y)
                    .size(RETRO_CLOCK_HAND_WIDTH, RETRO_CLOCK_HAND_HEIGHT)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(RETRO_CLOCK_CUP_WIDTH, RETRO_CLOCK_CUP_VIEWPORT_HEIGHT)
                    .clipToBounds()
            ) {
                Image(
                    painter = painterResource(R.drawable.home_right_hand_coffee_cup),
                    contentDescription = null,
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(RETRO_CLOCK_CUP_WIDTH, RETRO_CLOCK_CUP_HEIGHT)
                )
                GalleryCoffeeSteam(modifier = Modifier.fillMaxSize())
            }
        }
        // "선반 위 물건" 느낌만 주는, 존재감을 최소화한 얇은 공유 선반선.
        HorizontalDivider(thickness = 0.5.dp, color = PaperDivider.copy(alpha = 0.6f))
    }
}
