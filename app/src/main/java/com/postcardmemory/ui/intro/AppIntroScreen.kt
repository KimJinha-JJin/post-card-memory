package com.postcardmemory.ui.intro

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.PostcardDateFormat
import com.postcardmemory.ui.components.SEAL_POSTMARK_DATE_TEXT_RATIO
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.detail.SealStampHandOverlay
import com.postcardmemory.ui.detail.SealType
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.ui.theme.PaperField
import com.postcardmemory.ui.theme.PaperSurface
import com.postcardmemory.ui.theme.PaperTray
import com.postcardmemory.utils.VisitRecord
import com.postcardmemory.utils.visitDayStartMillis
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 인트로 상단에 평소 보이는 조용한 문구 풀. */
internal val INTRO_GENERAL_MESSAGES = listOf(
    "작은 편지가 도착하고 있어",
    "오늘의 엽서함",
    "작은 기억 하나",
    "도착한 편지가 있어",
    "다시 꺼내 보는 마음",
    "어제의 장면 하나",
    "아직 남아 있는 것들",
    "잠시, 엽서함 앞에서",
    "오늘 하루의 조각"
)

/**
 * 아주 낮은 확률로만 등장하는 이스터에그 문구 — 일반 문구와 동일한 디자인으로 노출한다.
 * 막스 베르스타펜 문구는 여기 넣지 않는다 — [INTRO_MAX_MILESTONE_VISIT_DAY](33)에서만
 * 확정으로 뜨는 전용 문구인데 이 무작위 풀에 같이 있으면 33일차가 아닌 날에도(예:
 * 6일차) 낮은 확률로 새어나온다(77일차에서 발견해 수정한 버그).
 */
internal val INTRO_SECRET_MESSAGES = listOf(
    "챗지피티야 고마워",
    "비개발자가 만들었어요"
)

/** 33일차 전용 확정 문구. [INTRO_SECRET_MESSAGES] 무작위 풀과 분리해서 관리한다. */
internal const val INTRO_MAX_MILESTONE_MESSAGE = "뚜뚜뚜두 막스 베르스타펜"

private const val INTRO_SECRET_PROBABILITY = 0.03f

/**
 * 누적 방문일이 이 값과 정확히 같으면 확률과 무관하게 막스 베르스타펜 문구를
 * 확정하고, 소인이 착지할 때 한 바퀴 돈다 — [INTRO_MILESTONE_MESSAGES]의
 * 다른 방문일에는 문구만 확정될 뿐 회전은 이 값에만 붙는 특권이다.
 */
internal const val INTRO_MAX_MILESTONE_VISIT_DAY = 33

/**
 * 특정 누적 방문일에 확률과 무관하게 확정으로 뜨는 문구들(선수 등번호
 * 이스터에그). 키는 방문일, 값은 그날 확정될 문구다. 디자인은
 * [INTRO_MAX_MILESTONE_VISIT_DAY](33)의 소인 회전 하나를 빼면 전부 일반
 * 문구와 완전히 동일하다.
 */
internal val INTRO_MILESTONE_MESSAGES: Map<Int, String> = mapOf(
    3 to "피에르으으으으으으으 가슬리이이이이이이이이이이",
    7 to "럭키데이",
    16 to "그것은 물이다",
    INTRO_MAX_MILESTONE_VISIT_DAY to INTRO_MAX_MILESTONE_MESSAGE,
    44 to "Hey, man",
    63 to "Here comes the DIVA"
)

/**
 * 100일차: 오늘 첫 방문으로 기록이 실제로 새로 남은 실행에서만, 편집 화면의 도장 찍기
 * 손([SealStampHandOverlay])이 들어와 방문 소인을 직접 찍어 준다. 재방문이나 기록을 읽지
 * 못한 실행(record == null)에서는 손 없이 기존 소인 움직임 그대로다.
 *
 * 방문 저장은 [com.postcardmemory.MainActivity]가 이 화면과 무관하게 먼저 끝낸다 —
 * 이 판정은 이미 정해진 결과로 연출 여부만 고르며, 손이 중간에 끊겨도 기록은 그대로다.
 */
internal fun shouldPlayIntroStampHand(
    visitRecord: VisitRecord?,
    isFirstVisitToday: Boolean
): Boolean = visitRecord != null && isFirstVisitToday

/** [roll]이 이스터에그 확률 구간(기본 3%) 안에 들어오는지. */
internal fun isSecretRoll(roll: Float): Boolean = roll < INTRO_SECRET_PROBABILITY

/**
 * 인트로 상단에 보여줄 문구 하나를 뽑는다. 순수 함수라 [random]을 고정 시드로
 * 넘기면 결과를 결정적으로 검증할 수 있다.
 *
 * [totalVisitDays]가 [INTRO_MILESTONE_MESSAGES]의 키와 정확히 같으면 확률 롤
 * 없이 그 문구를 확정으로 돌려준다. 그 외 값(2/4/32/34번째 등)에는 이 규칙이
 * 적용되지 않고 기존 3% 확률 롤을 그대로 탄다.
 */
internal fun selectIntroMessage(random: Random = Random, totalVisitDays: Int? = null): String {
    INTRO_MILESTONE_MESSAGES[totalVisitDays]?.let { return it }

    return if (isSecretRoll(random.nextFloat())) {
        INTRO_SECRET_MESSAGES.random(random)
    } else {
        INTRO_GENERAL_MESSAGES.random(random)
    }
}

/** 손으로 찍은 듯한 기울기. 도장이 내려앉으면서 이 각도까지 돌아간다. */
private const val INTRO_POSTMARK_TILT_DEGREES = -7f

/**
 * 인트로 소인 안 날짜 글자 크기 비율. 엽서 도장의 기본값
 * ([SEAL_POSTMARK_DATE_TEXT_RATIO] = 0.42)은 90dp 도장 기준이라 이 작은
 * 소인에서는 날짜가 내부 링을 넘어 잘려 보인다. 기본값은 그대로 두고(이미
 * 저장된 엽서의 소인 모양이 달라지면 안 된다) 인트로에서만 줄여 쓴다.
 */
private const val INTRO_POSTMARK_DATE_TEXT_RATIO = 0.32f

/** 도장이 종이에 닿기 전 얼마나 크게 들려 있는지(1 + 이 값 배). */
private const val INTRO_POSTMARK_DROP_SCALE = 0.45f

/** 잉크가 배어나오는 속도 — 내려오는 중반쯤 이미 진해진다. */
private const val INTRO_POSTMARK_INK_RAMP = 2.2f

/** "통" 하고 한 번 눌렸다 앉는 탄성. 반복되지 않는 일회성 움직임이다. */
private const val INTRO_POSTMARK_PRESS_STIFFNESS = 2600f

/**
 * [INTRO_MAX_MILESTONE_VISIT_DAY]에서만 착지 직전 한 바퀴(360도) 더 돌고
 * 내려앉는다. 기존 [stampPress]([Animatable]) 하나로 계산하는 값이라 새
 * 애니메이션 시스템이 필요 없고, 다른 milestone/일반 문구에는 이 항이
 * 그대로 0이라 회전이 붙지 않는다.
 */
private const val INTRO_POSTMARK_MILESTONE_SPIN_DEGREES = 360f

/**
 * 도장이 종이에 "닿았다"고 볼 눌림 정도. 애니메이션이 완전히 멈춘 뒤가 아니라
 * 이 값을 처음 지나는 순간에 진동을 울려서 손끝과 눈이 같은 시점에 반응한다.
 * 탄성 상수를 바꿔도 접촉 시점이 자동으로 따라온다.
 */
private const val INTRO_POSTMARK_CONTACT_PRESS = 0.94f

/**
 * 도장이 닿는 순간의 짧은 진동. 갤러리 `+` 롱프레스(35ms/190)보다 가볍고
 * 단순 탭(10ms/90)보다는 묵직한, 한 번 "통" 하고 눌리는 무게로 잡았다.
 */
private const val INTRO_POSTMARK_HAPTIC_DURATION_MS = 24L
private const val INTRO_POSTMARK_HAPTIC_AMPLITUDE = 175

/**
 * 68일차 갤러리 실기기 QA에서 `LocalHapticFeedback.performHapticFeedback()`이
 * 손끝에 전혀 느껴지지 않는 것으로 확인됐다(기기의 터치 피드백 설정이나
 * 최신 상수의 API 지원 여부에 따라 조용히 무반응일 수 있음). 그래서 갤러리와
 * 똑같이 앱 코드에서 통제 가능한 `Vibrator.vibrate(VibrationEffect.createOneShot)`
 * 를 쓴다(minSdk 26부터 사용 가능, 새 dependency·framework 없음. VIBRATE 권한은
 * 이미 manifest에 있다).
 *
 * `GalleryScreen`의 같은 역할 헬퍼(`vibrateGalleryFab`)는 그 파일의 private
 * 함수라 여기서 호출할 수 없어 같은 최소 형태를 이 화면에도 둔다 — 공용 햅틱
 * 헬퍼 추출은 검증이 끝난 갤러리 코드를 건드리는 일이라 이번 범위 밖 후속
 * 후보로 남긴다.
 */
private fun vibrateIntroPostmark(context: Context) {
    val vibrator = context.getSystemService(Vibrator::class.java) ?: return

    if (!vibrator.hasVibrator()) return

    vibrator.vibrate(
        VibrationEffect.createOneShot(
            INTRO_POSTMARK_HAPTIC_DURATION_MS,
            INTRO_POSTMARK_HAPTIC_AMPLITUDE
        )
    )
}

/** A안 필름 이송 곡선: 짧게 출발해 끝에서 길게 감속한다(목업 cubic-bezier(.40,0,.12,1)). */
private val IntroFilmMoveEasing = CubicBezierEasing(0.40f, 0f, 0.12f, 1f)

/** 필름 한 칸 폭 = 화면 폭의 40%(112~170dp). 칸 높이는 폭의 1.18배, 위아래 구멍 띠 14dp. */
private const val INTRO_FILM_FRAME_WIDTH_RATIO = 0.40f
private val INTRO_FILM_FRAME_MIN_WIDTH = 112.dp
private val INTRO_FILM_FRAME_MAX_WIDTH = 170.dp
private const val INTRO_FILM_CELL_ASPECT = 1.18f
private val INTRO_FILM_BAND = 14.dp
private val INTRO_FILM_HOLE_SPACING = 14.dp
private val INTRO_FILM_HOLE_RADIUS = 2.4.dp
private val INTRO_FILM_CELL_INSET = 4.dp

/** 칸 아래 정중앙 소인 지름 = 칸 폭의 44%, 최대 66dp. */
private const val INTRO_FILM_STAMP_RATIO = 0.44f
private val INTRO_FILM_STAMP_MAX = 66.dp

/** 필름이 처음 나타날 때의 짧은 fade. */
private const val INTRO_FILM_APPEAR_MS = 150

/** 지난 방문 칸에 이미 찍혀 있는 소인의 옅기. */
private const val INTRO_FILM_PAST_STAMP_ALPHA = 0.9f

/** 아직 오지 않은 날·기록 이전 빈 칸의 종이색(조금 더 밝은 미사용 종이). */
private val IntroFilmBlankPaper = Color(0xFFFCF8F0)

/** 시스템 애니메이션 배율이 0이면 필름 이송·도장 탄성 없이 찍힌 모습만 보여준다(시작 시 1회 읽음). */
private fun readIntroReducedMotion(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}.getOrDefault(false)

/**
 * 앱 시작 직후 보이는 인트로. 133일차부터 69일차의 진행선·퍼센트·봉투 대신 **날짜별 종이 필름**을
 * 보여준다. 진행선은 실제 로딩과 연결되지 않은 고정 길이 연출이었고(Room Flow 초기 방출이 거의 즉시
 * 끝나 의미 있는 진행률을 만들 수 없다), 필름도 로딩 진행률이 아니라 **하루의 경과**만 표현한다.
 * 인트로 종료는 예전처럼 이 화면의 시간표가 끝나면 [onFinished]를 부르는 것뿐이고, 갤러리 데이터
 * 로딩은 인트로와 별개로 진행된다.
 *
 * [visitRecord]는 오늘 방문 기록이며, 아직 읽히지 않았으면 null이다. [visitArrived]는 기록 판정이
 * 끝났는지(읽기 실패로 record가 null인 경우 포함)다. 필름은 판정 결과를 최대
 * [INTRO_FILM_RECORD_WAIT_MS]까지만 기다리고, 넘으면 날짜·소인 없이 짧게 지나간다(저장소 I/O 때문에
 * 인트로가 멈추지 않는다). 상단 문구는 누적 방문일이 정확히 33일 때만 막스 베르스타펜 이스터에그를
 * 확정으로 보여준다.
 *
 * [isFirstVisitToday]는 이번 실행이 오늘의 첫 방문인지다. 그날 첫 방문이면 직전 방문 칸에서 오늘 칸까지
 * 필름이 이송되고([previousVisitEpochDay]부터 쉰 날 수만큼), 기존 손 도장이 들어와 오늘 칸에 꾹 찍는다.
 * 도장이 닿는 진동도 이때만 울린다. 같은 날 다시 열면 이동·도장 연출 없이 찍힌 모습만 짧게 보여준다.
 */
@Composable
fun AppIntroScreen(
    visitRecord: VisitRecord? = null,
    isFirstVisitToday: Boolean = false,
    visitArrived: Boolean = visitRecord != null,
    previousVisitEpochDay: Long? = null,
    todayEpochDay: Long? = null,
    /** 지난 날짜 칸 중 달력 방문 표식이 확인된 날. null이면 판별 불가로 지난 칸에 소인을 찍지 않는다. */
    pastVisitedDays: Set<Long>? = null,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val reducedMotion = remember(context) { readIntroReducedMotion(context) }

    // 방문 기록이 도착하기 전엔(null) 33번째 milestone 여부를 알 수 없다.
    // 즉시 아무 문구나 골랐다가 데이터가 도착한 뒤 다시 고르면 "문구 A→B"로
    // 바뀌는 모습이 보이므로, 아래 방문 소인과 같은 hasVisitRecord 게이팅으로
    // 도착 전엔 자리만 비워두고 데이터가 오면 그 순간 한 번만 확정한다.
    val hasVisitRecord = visitRecord != null
    val introMessage = remember(hasVisitRecord) {
        if (hasVisitRecord) {
            selectIntroMessage(totalVisitDays = visitRecord.totalVisitDays)
        } else {
            null
        }
    }

    val playStampHand = shouldPlayIntroStampHand(visitRecord, isFirstVisitToday)
    // 손이 들어올 차례인지, 도장이 종이에 닿았는지, 다 빠졌는지. 인트로는 프로세스당
    // 한 번만 보이므로(AppIntroState) 한 번의 재생만 다루면 된다.
    var stampHandStarted by remember { mutableStateOf(false) }
    var stampHandContacted by remember { mutableStateOf(false) }
    var stampHandFinished by remember { mutableStateOf(false) }
    var introRootOrigin by remember { mutableStateOf(Offset.Zero) }
    var postmarkCenterInRoot by remember { mutableStateOf<Offset?>(null) }
    var filmCenterXInRoot by remember { mutableStateOf<Float?>(null) }

    // 기록 판정 결과로 한 번 정한 필름. 정해지기 전(최대 0.7초)엔 필름 자리만 비워 둔다.
    var plan by remember { mutableStateOf<IntroFilmPlan?>(null) }
    val filmProgress = remember { Animatable(0f) }
    val filmAlpha = remember { Animatable(0f) }

    val currentArrived by rememberUpdatedState(visitArrived)
    val currentRecord by rememberUpdatedState(visitRecord)
    val currentFirstVisit by rememberUpdatedState(isFirstVisitToday)
    val currentPrevious by rememberUpdatedState(previousVisitEpochDay)
    val currentToday by rememberUpdatedState(todayEpochDay)
    val currentPastVisited by rememberUpdatedState(pastVisitedDays)
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        val arrived = withTimeoutOrNull(INTRO_FILM_RECORD_WAIT_MS) {
            snapshotFlow { currentArrived }.first { it }
        } != null

        val chosen = planIntroFilm(
            record = if (arrived) currentRecord else null,
            isFirstVisitToday = arrived && currentFirstVisit,
            previousVisitEpochDay = if (arrived) currentPrevious else null,
            reducedMotion = reducedMotion,
            todayEpochDay = currentToday,
            pastVisitedDays = if (arrived) currentPastVisited else null
        )
        plan = chosen
        launch { filmAlpha.animateTo(1f, tween(INTRO_FILM_APPEAR_MS)) }

        if (chosen.stamp == IntroFilmStamp.HAND) {
            if (chosen.moves) {
                delay(INTRO_FILM_MOVE_START_DELAY_MS)
                filmProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(chosen.moveDurationMs, easing = IntroFilmMoveEasing)
                )
            } else {
                filmProgress.snapTo(1f)
            }
            delay(INTRO_FILM_STAMP_DELAY_MS)
            stampHandStarted = true
            // 손이 어떤 이유로 시작되지 못해도 인트로가 멈추지 않도록 상한을 둔다.
            withTimeoutOrNull(INTRO_FILM_HAND_TOTAL_MS * 2) {
                snapshotFlow { stampHandFinished }.first { it }
            }
            delay(INTRO_FILM_AFTER_HAND_HOLD_MS)
        } else {
            filmProgress.snapTo(1f)
            delay(chosen.holdMs)
        }
        currentOnFinished()
    }

    val currentPlan = plan
    val stampMode = currentPlan?.stamp ?: IntroFilmStamp.NONE
    // 날짜를 되돌린 기기처럼 기록은 첫 방문이어도 필름은 정지하는 경우, 손은 오지 않는다.
    val handOnFilm = playStampHand && stampMode == IntroFilmStamp.HAND
    val showVisit = currentPlan != null && stampMode != IntroFilmStamp.NONE
    val showCount = showVisit && (stampMode != IntroFilmStamp.HAND || stampHandContacted)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite)
            .onGloballyPositioned { introRootOrigin = it.positionInRoot() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = introMessage.orEmpty(),
                fontSize = 12.sp,
                color = InkSecondary,
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .alpha(if (showVisit) 1f else 0f)
            )

            IntroFilmStrip(
                plan = currentPlan,
                progress = filmProgress,
                appearAlpha = filmAlpha,
                visitRecord = visitRecord,
                onFilmCenterXInRoot = { filmCenterXInRoot = it }
            ) { stampSize ->
                AppIntroVisitPostmark(
                    visitRecord = visitRecord,
                    isFirstVisitToday = isFirstVisitToday,
                    stampMode = stampMode,
                    stampSize = stampSize,
                    waitsForStampHand = handOnFilm,
                    stampHandContacted = stampHandContacted,
                    onPostmarkCenterInRoot = { postmarkCenterInRoot = it }
                )
            }

            Text(
                // 연속 방문일은 일부러 보여주지 않는다 - 끊겼다는 표현은 압박이
                // 되므로, 절대 줄지 않는 총 방문일만 조용히 적는다.
                text = visitRecord?.let { "${it.totalVisitDays}번째 방문" }.orEmpty(),
                fontSize = 11.sp,
                color = InkSecondary,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .alpha(if (showCount) 1f else 0f)
            )
        }

        // 손이 오는 시점엔 오늘 칸이 필름 가운데에 정착해 있다. 칸 좌표는 이송(graphicsLayer) 전
        // 배치 기준일 수 있어 가로는 필름 가운데, 세로는 소인 중심을 쓴다.
        val postmarkCenter = postmarkCenterInRoot
        val filmCenterX = filmCenterXInRoot
        val stampTarget = if (postmarkCenter != null && filmCenterX != null) {
            Offset(filmCenterX, postmarkCenter.y)
        } else {
            null
        }
        if (handOnFilm && stampHandStarted && !stampHandFinished && stampTarget != null) {
            SealStampHandOverlay(
                target = stampTarget - introRootOrigin,
                onContact = { stampHandContacted = true },
                onFinished = { stampHandFinished = true }
            )
        }
    }
}

/**
 * 날짜별 종이 필름. 칸은 시간 순서로 왼쪽(지난 날)부터 오른쪽(아직 오지 않은 날)까지 놓이고,
 * [progress] 0→1 동안 시작 칸에서 오늘 칸이 가운데 기준 위치로 오도록 필름 전체를 왼쪽으로 옮긴다.
 * 이동은 [graphicsLayer] 안에서만 읽어 프레임마다 recomposition이 일어나지 않는다. 칸은 사각형·원과
 * 기존 원형 소인 렌더러만 그린다(새 이미지·엽서 사진 디코딩 없음).
 */
@Composable
private fun IntroFilmStrip(
    plan: IntroFilmPlan?,
    progress: Animatable<Float, AnimationVector1D>,
    appearAlpha: Animatable<Float, AnimationVector1D>,
    visitRecord: VisitRecord?,
    onFilmCenterXInRoot: (Float) -> Unit,
    todayStamp: @Composable (Dp) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val frameWidth = (maxWidth * INTRO_FILM_FRAME_WIDTH_RATIO)
            .coerceIn(INTRO_FILM_FRAME_MIN_WIDTH, INTRO_FILM_FRAME_MAX_WIDTH)
        val cellHeight = frameWidth * INTRO_FILM_CELL_ASPECT
        val stripHeight = cellHeight + INTRO_FILM_BAND * 2
        val stampSize = minOf(frameWidth * INTRO_FILM_STAMP_RATIO, INTRO_FILM_STAMP_MAX)
        val windowWidth = maxWidth

        val description = if (plan != null && plan.stamp != IntroFilmStamp.NONE && visitRecord != null) {
            "오늘 방문 소인, " + PostcardDateFormat.formatIso(visitDayStartMillis(visitRecord.lastVisitEpochDay))
        } else {
            "엽서함을 여는 중"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stripHeight)
                .clipToBounds()
                .onGloballyPositioned { coordinates ->
                    onFilmCenterXInRoot(coordinates.positionInRoot().x + coordinates.size.width / 2f)
                }
                .graphicsLayer { alpha = appearAlpha.value }
                .clearAndSetSemantics { contentDescription = description }
        ) {
            if (plan != null) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .height(stripHeight)
                        .graphicsLayer {
                            val frame = frameWidth.toPx()
                            val position = plan.startIndex +
                                (plan.todayIndex - plan.startIndex) * progress.value
                            translationX = windowWidth.toPx() / 2f - frame * (position + 0.5f)
                        }
                        .drawBehind {
                            val line = 1.dp.toPx()
                            drawRect(PaperDivider, size = size.copy(height = line))
                            drawRect(
                                PaperDivider,
                                topLeft = Offset(0f, size.height - line),
                                size = size.copy(height = line)
                            )
                        }
                ) {
                    plan.frames.forEach { frame ->
                        IntroFilmFrameCell(
                            frame = frame,
                            frameWidth = frameWidth,
                            stampSize = stampSize,
                            todayStamp = todayStamp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroFilmFrameCell(
    frame: IntroFilmFrame,
    frameWidth: Dp,
    stampSize: Dp,
    todayStamp: @Composable (Dp) -> Unit
) {
    Box(
        modifier = Modifier
            .width(frameWidth)
            .fillMaxHeight()
            .background(PaperSurface)
            .drawBehind {
                // 위아래 띠에 같은 간격으로 뚫린 둥근 구멍.
                val spacing = INTRO_FILM_HOLE_SPACING.toPx()
                val radius = INTRO_FILM_HOLE_RADIUS.toPx()
                val bandCenter = INTRO_FILM_BAND.toPx() / 2f
                var x = spacing / 2f
                while (x < size.width) {
                    drawCircle(PaperTray, radius, Offset(x, bandCenter))
                    drawCircle(PaperTray, radius, Offset(x, size.height - bandCenter))
                    x += spacing
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = INTRO_FILM_CELL_INSET, vertical = INTRO_FILM_BAND)
                .background(
                    if (frame.epochDay == null) IntroFilmBlankPaper else PaperField,
                    RoundedCornerShape(2.dp)
                )
                .border(1.dp, PaperDivider, RoundedCornerShape(2.dp))
        ) {
            val epochDay = frame.epochDay
            if (epochDay != null) {
                Text(
                    text = PostcardDateFormat.formatIso(visitDayStartMillis(epochDay)),
                    fontSize = 9.5.sp,
                    color = InkSecondary,
                    // 쉰 날(도장 없는 지난 날짜)은 한 톤 옅게.
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 7.dp, top = 6.dp)
                        .alpha(if (frame.isToday || frame.hasPastStamp) 1f else 0.5f)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 9.dp)
                    .size(stampSize)
            ) {
                when {
                    frame.isToday -> todayStamp(stampSize)
                    frame.hasPastStamp && epochDay != null -> SealPreviewContent(
                        type = SealType.CIRCLE_POSTMARK,
                        color = InkSecondary,
                        capturedAtMillis = visitDayStartMillis(epochDay),
                        dateTextRatio = INTRO_POSTMARK_DATE_TEXT_RATIO,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = INTRO_POSTMARK_TILT_DEGREES
                                alpha = INTRO_FILM_PAST_STAMP_ALPHA
                            }
                    )
                }
            }
        }
    }
}

/**
 * 오늘 들렀다는 흔적 하나. 엽서에 쓰는 원형 소인 렌더러
 * ([SealPreviewContent] + [SealType.CIRCLE_POSTMARK])를 그대로 재사용해
 * 날짜까지 앱 공통 서식(yyyy-MM-dd)으로 찍는다 — 인트로용 소인을 따로
 * 그리지 않는다. 잉크색만 도장 콘텐츠 색(sealInkColors)이 아니라 인트로
 * 다른 요소와 같은 UI 색 [InkSecondary]를 쓴다. 133일차부터는 필름의 오늘 칸 아래 가운데에 놓인다.
 *
 * 그날 첫 방문([stampMode] == [IntroFilmStamp.HAND])에는 손 도장이 닿을 때까지 비워 뒀다가, 닿은
 * 자리에서 잉크가 묻고 "통" 하고 한 번 눌렸다 앉으며 손으로 찍은 듯한 각도까지 돌아간다. 반복되지
 * 않는 일회성 움직임이고, 값을 [graphicsLayer] 안에서 읽으므로 프레임마다 recomposition이 일어나지
 * 않는다. [INTRO_MAX_MILESTONE_VISIT_DAY]번째 방문이면 착지하며 마지막 회전을 더하고 같은 자리에
 * 앉는다 — 다른 방문일에는 이 회전이 붙지 않는다.
 *
 * 같은 날 재실행·애니메이션 감소([IntroFilmStamp.INKED])에는 움직임 없이 이미 찍힌 모습이다.
 * 닿는 순간의 진동은 [isFirstVisitToday]일 때만 울린다.
 */
@Composable
private fun AppIntroVisitPostmark(
    visitRecord: VisitRecord?,
    isFirstVisitToday: Boolean,
    stampMode: IntroFilmStamp,
    stampSize: Dp,
    waitsForStampHand: Boolean = false,
    stampHandContacted: Boolean = false,
    onPostmarkCenterInRoot: (Offset) -> Unit = {}
) {
    // 0 = 아직 들려 있음, 1 = 종이에 앉음. 탄성이 1을 살짝 넘는 구간이 "통"이다.
    val stampPress = remember { Animatable(0f) }
    val hasVisitRecord = visitRecord != null
    val isMaxMilestone = visitRecord?.totalVisitDays == INTRO_MAX_MILESTONE_VISIT_DAY
    val context = LocalContext.current

    LaunchedEffect(hasVisitRecord, isFirstVisitToday, stampHandContacted) {
        if (!hasVisitRecord) return@LaunchedEffect
        if (stampMode == IntroFilmStamp.NONE) return@LaunchedEffect
        // 손이 찍어 줄 날에는 손 도장이 종이에 닿을 때까지 소인을 비워 둔다.
        if (waitsForStampHand && !stampHandContacted) return@LaunchedEffect

        // 오늘 처음 찍히는 소인일 때만, 도장이 종이에 처음 닿는 순간에 딱 한 번
        // 진동한다. 애니메이션이 끝나기를 기다리지 않고(끝은 튕김이 잦아든
        // 뒤라 늦다) 눌림 값이 접촉 지점을 처음 지날 때 울린다.
        if (isFirstVisitToday) {
            launch {
                snapshotFlow { stampPress.value }
                    .first { press -> press >= INTRO_POSTMARK_CONTACT_PRESS }

                vibrateIntroPostmark(context)
            }
        }

        // 같은 날 재실행·감소 모드: 움직임 없이 이미 찍힌 모습.
        if (stampMode == IntroFilmStamp.INKED) {
            stampPress.snapTo(1f)
            return@LaunchedEffect
        }

        // 손이 찍은 소인은 위에서 떨어지지 않는다 — 닿은 자리에서 잉크가 바로 묻고
        // 살짝 눌렸다 앉기만 한다(진동도 이 접촉 값에서 같은 한 곳이 울린다).
        if (waitsForStampHand) {
            stampPress.snapTo(INTRO_POSTMARK_CONTACT_PRESS)
        }

        stampPress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = INTRO_POSTMARK_PRESS_STIFFNESS
            )
        )
    }

    Box(
        modifier = Modifier
            .size(stampSize)
            .onGloballyPositioned { coordinates ->
                val origin = coordinates.positionInRoot()
                onPostmarkCenterInRoot(
                    Offset(
                        origin.x + coordinates.size.width / 2f,
                        origin.y + coordinates.size.height / 2f
                    )
                )
            }
            .graphicsLayer {
                alpha = (stampPress.value * INTRO_POSTMARK_INK_RAMP)
                    .coerceIn(0f, 1f)
            }
            .graphicsLayer {
                val press = stampPress.value
                val pressScale =
                    1f + INTRO_POSTMARK_DROP_SCALE * (1f - press)

                scaleX = pressScale
                scaleY = pressScale
                rotationZ = INTRO_POSTMARK_TILT_DEGREES * press +
                    if (isMaxMilestone) {
                        INTRO_POSTMARK_MILESTONE_SPIN_DEGREES * (1f - press)
                    } else {
                        0f
                    }
            }
    ) {
        if (visitRecord != null) {
            SealPreviewContent(
                type = SealType.CIRCLE_POSTMARK,
                color = InkSecondary,
                capturedAtMillis =
                    visitDayStartMillis(visitRecord.lastVisitEpochDay),
                dateTextRatio = INTRO_POSTMARK_DATE_TEXT_RATIO,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
