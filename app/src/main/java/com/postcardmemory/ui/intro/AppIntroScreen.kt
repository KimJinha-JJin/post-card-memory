package com.postcardmemory.ui.intro

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.postcardmemory.ui.components.SEAL_POSTMARK_DATE_TEXT_RATIO
import com.postcardmemory.ui.components.SealPreviewContent
import com.postcardmemory.ui.detail.SealType
import com.postcardmemory.ui.theme.GalleryPaperWhite
import com.postcardmemory.ui.theme.InkSecondary
import com.postcardmemory.ui.theme.PaperDivider
import com.postcardmemory.utils.VisitRecord
import com.postcardmemory.utils.visitDayStartMillis
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val INTRO_FILL_DURATION_MS = 1800
private const val INTRO_SETTLE_DELAY_MS = 150L

/** 인트로 상단에 평소 보이는 조용한 문구 풀. */
internal val INTRO_GENERAL_MESSAGES = listOf(
    "작은 편지가 도착하고 있어",
    "오늘의 엽서함",
    "작은 기억 하나",
    "도착한 편지가 있어요",
    "다시 꺼내 보는 마음",
    "어제의 장면 하나",
    "아직 남아 있는 것들",
    "잠시, 엽서함 앞에서",
    "오늘 하루의 조각"
)

/** 아주 낮은 확률로만 등장하는 이스터에그 문구 — 일반 문구와 동일한 디자인으로 노출한다. */
internal val INTRO_SECRET_MESSAGES = listOf(
    "뚜뚜뚜두 막스 베르스타펜",
    "챗지피티야 고마워",
    "비개발자가 만들었어요"
)

private const val INTRO_SECRET_PROBABILITY = 0.03f

/** 누적 방문일이 이 값과 정확히 같으면 확률과 무관하게 막스 베르스타펜 문구를 확정한다. */
internal const val INTRO_MAX_MILESTONE_VISIT_DAY = 33

/** [roll]이 이스터에그 확률 구간(기본 3%) 안에 들어오는지. */
internal fun isSecretRoll(roll: Float): Boolean = roll < INTRO_SECRET_PROBABILITY

/**
 * 인트로 상단에 보여줄 문구 하나를 뽑는다. 순수 함수라 [random]을 고정 시드로
 * 넘기면 결과를 결정적으로 검증할 수 있다.
 *
 * [totalVisitDays]가 정확히 [INTRO_MAX_MILESTONE_VISIT_DAY]번째면 확률 롤 없이
 * [INTRO_SECRET_MESSAGES]의 막스 베르스타펜 문구를 확정으로 돌려준다. 그 외
 * 값(32/34번째 등)에는 이 규칙이 적용되지 않고 기존 3% 확률 롤을 그대로 탄다.
 */
internal fun selectIntroMessage(random: Random = Random, totalVisitDays: Int? = null): String {
    if (totalVisitDays == INTRO_MAX_MILESTONE_VISIT_DAY) {
        return INTRO_SECRET_MESSAGES[0]
    }

    return if (isSecretRoll(random.nextFloat())) {
        INTRO_SECRET_MESSAGES.random(random)
    } else {
        INTRO_GENERAL_MESSAGES.random(random)
    }
}

/** 인트로 아래쪽 방문 소인의 지름. 엽서 위 도장(90.dp)보다 작게 둬 인트로에서 튀지 않게 한다. */
private val INTRO_POSTMARK_SIZE = 76.dp

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

/**
 * 앱 시작 직후 아주 짧게 보이는 인트로. 실제 갤러리 데이터 로딩과 연결된
 * 진행률이 아니라(Room Flow 초기 방출이 거의 즉시 끝나 의미 있는 진행률을
 * 만들 수 없다), 고정 길이의 짧은 시각 애니메이션이다.
 *
 * [visitRecord]는 오늘 방문 기록이며, 아직 읽히지 않았으면 null이다. 인트로는
 * 이 값을 **기다리지 않는다** — null이어도 진행선은 그대로 흐르고, 값이
 * 도착하면 아래쪽 소인과 위쪽 문구가 조용히 나타난다(저장소 I/O 때문에
 * 인트로가 느려지거나 멈추지 않는다). 상단 문구는 누적 방문일이 정확히 33일
 * 때만 막스 베르스타펜 이스터에그를 확정으로 보여준다.
 *
 * [isFirstVisitToday]는 이번 실행이 오늘의 첫 방문인지다. 소인은 앱을 열
 * 때마다 찍히지만 도장이 닿는 진동은 이때만 울린다 — 같은 날 여러 번 열
 * 때마다 진동하면 과하다는 실기기 판단.
 */
@Composable
fun AppIntroScreen(
    visitRecord: VisitRecord? = null,
    isFirstVisitToday: Boolean = false,
    onFinished: () -> Unit
) {
    val progress = remember { Animatable(0f) }

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

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(INTRO_FILL_DURATION_MS, easing = LinearEasing)
        )
        delay(INTRO_SETTLE_DELAY_MS)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryPaperWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = introMessage.orEmpty(),
                fontSize = 12.sp,
                color = InkSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            AppIntroProgress(progress = progress.value)

            Spacer(modifier = Modifier.height(20.dp))

            AppIntroVisitPostmark(
                visitRecord = visitRecord,
                isFirstVisitToday = isFirstVisitToday
            )
        }
    }
}

/**
 * 오늘 들렀다는 흔적 하나. 엽서에 쓰는 원형 소인 렌더러
 * ([SealPreviewContent] + [SealType.CIRCLE_POSTMARK])를 그대로 재사용해
 * 날짜까지 앱 공통 서식(yyyy-MM-dd)으로 찍는다 — 인트로용 소인을 따로
 * 그리지 않는다. 잉크색만 도장 콘텐츠 색(sealInkColors)이 아니라 인트로
 * 다른 요소와 같은 UI 색 [InkSecondary]를 쓴다.
 *
 * 방문 기록이 도착하면 도장이 한 번 "통" 하고 내려앉는다: 크게 들려 있던
 * 상태에서 잉크가 배어들며 내려와, 목표 크기를 살짝 지나 눌렸다가 제자리에
 * 앉고 동시에 손으로 찍은 듯한 각도까지 돌아간다. 반복되지 않는 일회성
 * 움직임이고, 값을 [graphicsLayer] 안에서 읽으므로 프레임마다 recomposition이
 * 일어나지 않는다.
 *
 * 이 움직임은 앱을 열 때마다 보이지만, 닿는 순간의 진동은
 * [isFirstVisitToday]일 때만 울린다.
 *
 * 기록이 아직 없어도(null) 자리는 항상 잡아둔다 — 값이 늦게 도착해도 위쪽
 * 진행선이 다시 가운데로 밀려 흔들리지 않게 하려는 것이다.
 */
@Composable
private fun AppIntroVisitPostmark(
    visitRecord: VisitRecord?,
    isFirstVisitToday: Boolean
) {
    // 0 = 아직 들려 있음, 1 = 종이에 앉음. 탄성이 1을 살짝 넘는 구간이 "통"이다.
    val stampPress = remember { Animatable(0f) }
    val hasVisitRecord = visitRecord != null
    val context = LocalContext.current

    LaunchedEffect(hasVisitRecord, isFirstVisitToday) {
        if (!hasVisitRecord) return@LaunchedEffect

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

        stampPress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = INTRO_POSTMARK_PRESS_STIFFNESS
            )
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                alpha = (stampPress.value * INTRO_POSTMARK_INK_RAMP)
                    .coerceIn(0f, 1f)
            }
            .clearAndSetSemantics {
                contentDescription = "오늘 방문 소인"
            }
    ) {
        Box(
            modifier = Modifier
                .size(INTRO_POSTMARK_SIZE)
                .graphicsLayer {
                    val press = stampPress.value
                    val pressScale =
                        1f + INTRO_POSTMARK_DROP_SCALE * (1f - press)

                    scaleX = pressScale
                    scaleY = pressScale
                    rotationZ = INTRO_POSTMARK_TILT_DEGREES * press
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

        Text(
            // 연속 방문일은 일부러 보여주지 않는다 - 끊겼다는 표현은 압박이
            // 되므로, 절대 줄지 않는 총 방문일만 조용히 적는다.
            text = visitRecord?.let { "${it.totalVisitDays}번째 방문" }.orEmpty(),
            fontSize = 11.sp,
            color = InkSecondary,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun AppIntroProgress(progress: Float) {
    val percent = (progress * 100f).roundToInt().coerceIn(0, 100)

    Row(
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .clearAndSetSemantics {
                contentDescription = "엽서함을 여는 중"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
        ) {
            val density = LocalDensity.current
            var envelopeWidth by remember { mutableStateOf(0.dp) }
            val travel = (maxWidth - envelopeWidth).coerceAtLeast(0.dp)
            val envelopeOffset = travel * progress

            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerY = size.height * 0.82f
                val strokeWidthPx = 1.4.dp.toPx()
                drawLine(
                    color = PaperDivider,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = InkSecondary,
                    start = Offset(0f, centerY),
                    end = Offset(size.width * progress, centerY),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            Text(
                text = "✉︎",
                fontSize = 14.sp,
                color = InkSecondary,
                modifier = Modifier
                    .offset(x = envelopeOffset)
                    .onGloballyPositioned {
                        envelopeWidth = with(density) { it.size.width.toDp() }
                    }
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "$percent%",
            fontSize = 13.sp,
            color = InkSecondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(30.dp)
        )
    }
}
