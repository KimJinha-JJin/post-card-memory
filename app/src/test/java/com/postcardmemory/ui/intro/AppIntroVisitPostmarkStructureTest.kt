package com.postcardmemory.ui.intro

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 70일차 방문 소인의 최소 구조와, 출석 UI로 변해버리지 않게 하는 금지선을
 * 고정한다. 인트로 자체(진행선/봉투/퍼센트)의 69일차 구조도 함께 지킨다.
 */
class AppIntroVisitPostmarkStructureTest {

    private fun sourceOf(relativePath: String): String {
        val candidates = listOf("src/$relativePath", "app/src/$relativePath")
        return candidates.map(::File).firstOrNull(File::exists)?.readText()
            ?: error("$relativePath 를 찾을 수 없음(cwd=${File(".").absolutePath})")
    }

    private val introSource: String by lazy {
        sourceOf("main/java/com/postcardmemory/ui/intro/AppIntroScreen.kt")
    }

    private val mainActivitySource: String by lazy {
        sourceOf("main/java/com/postcardmemory/MainActivity.kt")
    }

    // ---- 소인 렌더링: 새로 그리지 않고 엽서 소인 렌더러를 재사용 ----

    @Test
    fun postmark_reusesExistingCirclePostmarkRenderer() {
        assertTrue(
            "엽서 소인 렌더러를 재사용해야 함",
            introSource.contains("SealPreviewContent(")
        )
        assertTrue(
            "원형 소인 종류를 써야 함",
            introSource.contains("type = SealType.CIRCLE_POSTMARK")
        )
        assertTrue(
            "소인 안 날짜는 방문일에서 계산해야 함",
            introSource.contains("visitDayStartMillis(visitRecord.lastVisitEpochDay)")
        )
    }

    @Test
    fun postmark_usesIntroUiInkNotPostcardSealInk() {
        assertTrue(
            "인트로 다른 요소와 같은 UI 잉크색을 써야 함",
            introSource.contains("color = InkSecondary")
        )
        assertFalse(
            "엽서 콘텐츠 전용 도장 잉크색은 UI에 끌어오지 않음",
            introSource.contains("color = SealInk")
        )
    }

    // ---- 작은 소인에서 날짜가 잘리지 않게: 엽서 기본값은 건드리지 않는다 ----

    @Test
    fun postmark_passesSmallerDateTextRatioInsteadOfChangingThePostcardDefault() {
        assertTrue(
            "인트로는 자기 날짜 비율을 넘겨야 함",
            introSource.contains("dateTextRatio = INTRO_POSTMARK_DATE_TEXT_RATIO")
        )

        val introRatio = Regex("INTRO_POSTMARK_DATE_TEXT_RATIO = ([0-9.]+)f")
            .find(introSource)
            ?.groupValues
            ?.get(1)
            ?.toFloat()
            ?: error("INTRO_POSTMARK_DATE_TEXT_RATIO 값을 찾지 못함")

        assertTrue(
            "인트로 날짜는 엽서 도장 기본값(0.42)보다 작아야 함(introRatio=$introRatio)",
            introRatio < 0.42f
        )
    }

    @Test
    fun postcardSealDateRatio_staysUnchangedInPreviewAndExporter() {
        // 이 기본값이 바뀌면 이미 저장된 엽서의 소인과 내보낸 이미지가 함께
        // 달라진다. 화면 미리보기와 exporter가 같은 값을 쓰는지도 함께 고정한다.
        val sealShapes = sourceOf("main/java/com/postcardmemory/ui/components/SealShapes.kt")
        val exporter = sourceOf("main/java/com/postcardmemory/utils/PostcardImageExporter.kt")

        assertTrue(
            "엽서 도장 날짜 비율 기본값은 0.42f로 유지",
            sealShapes.contains("SEAL_POSTMARK_DATE_TEXT_RATIO = 0.42f")
        )
        assertTrue(
            "미리보기는 기본값을 그대로 기본 인자로 써야 함",
            sealShapes.contains("dateTextRatio: Float = SEAL_POSTMARK_DATE_TEXT_RATIO")
        )
        assertTrue(
            "저장본 exporter의 소인 날짜 계산식은 그대로 유지",
            exporter.contains("textSize = innerRadius * 0.42f")
        )
    }

    // ---- "통" 하고 찍히는 일회성 움직임 ----

    @Test
    fun postmark_pressesDownOnceWithSpringInsteadOfPlainFade() {
        assertTrue(
            "도장이 내려앉는 탄성 움직임이 있어야 함",
            introSource.contains("Spring.DampingRatioMediumBouncy")
        )
        assertTrue(
            "크게 들려 있다가 줄어들며 내려와야 함",
            introSource.contains("INTRO_POSTMARK_DROP_SCALE * (1f - press)")
        )
        assertTrue(
            "내려앉으면서 기울기까지 돌아가야 함",
            introSource.contains("rotationZ = INTRO_POSTMARK_TILT_DEGREES * press")
        )
        assertTrue(
            "방문 기록이 도착할 때 한 번만 실행되어야 함",
            introSource.contains("LaunchedEffect(hasVisitRecord, isFirstVisitToday)")
        )
        assertFalse(
            "반복 애니메이션을 두지 않음",
            introSource.contains("infiniteRepeatable") ||
                introSource.contains("rememberInfiniteTransition")
        )
    }

    // ---- 닿는 순간의 햅틱 ----

    @Test
    fun postmark_hapticUsesVibratorNotTheUnfeltHapticFeedbackApi() {
        // 68일차 갤러리 QA에서 LocalHapticFeedback이 실기기에서 전혀 느껴지지
        // 않는 것으로 확인됐다. 같은 함정으로 돌아가지 않게 고정한다.
        assertTrue(
            "갤러리와 같은 Vibrator + createOneShot을 써야 함",
            introSource.contains("VibrationEffect.createOneShot(")
        )
        assertTrue(introSource.contains("vibrator.hasVibrator()"))
        assertFalse(
            // 왜 그 API를 쓰지 않는지는 주석에 남아 있어야 하므로 import로 판정한다.
            "실기기에서 느껴지지 않는 LocalHapticFeedback으로 돌아가지 않음",
            introSource.contains("import androidx.compose.ui.platform.LocalHapticFeedback")
        )
    }

    @Test
    fun postmark_hapticOnlyOnTheFirstVisitOfTheDay() {
        // 소인은 앱을 열 때마다 찍히지만 진동은 그날 처음 찍힐 때만.
        assertTrue(
            "첫 방문 여부를 인트로가 받아야 함",
            introSource.contains("isFirstVisitToday: Boolean = false")
        )
        assertTrue(
            "진동은 첫 방문일 때만 예약해야 함",
            introSource.contains("if (isFirstVisitToday) {")
        )
        assertTrue(
            "도장 애니메이션 자체는 첫 방문 여부와 무관하게 실행되어야 함",
            introSource.contains("stampPress.animateTo(")
        )
        assertTrue(
            "MainActivity가 첫 방문 여부를 넘겨줘야 함",
            mainActivitySource.contains("todayVisit?.isFirstVisitToday == true")
        )
        assertTrue(
            mainActivitySource.contains("visitRecord = todayVisit?.record")
        )
    }

    @Test
    fun postmark_hapticFiresOnceAtVisualContact() {
        assertTrue(
            "애니메이션 종료가 아니라 접촉 시점에 울려야 함",
            introSource.contains("press >= INTRO_POSTMARK_CONTACT_PRESS")
        )
        assertTrue(
            "접촉 시점은 눌림 값을 처음 지나는 순간 한 번만",
            introSource.contains("snapshotFlow { stampPress.value }") &&
                introSource.contains(".first {")
        )
        assertEquals(
            "진동 호출은 한 곳뿐이어야 함",
            1,
            Regex("vibrateIntroPostmark\\(context\\)").findAll(introSource).count()
        )
        assertFalse(
            "고정 delay로 접촉 시점을 추측하지 않음",
            Regex("delay\\(INTRO_POSTMARK").containsMatchIn(introSource)
        )
    }

    @Test
    fun postmark_hapticIsLighterThanGalleryLongPress() {
        val gallery = sourceOf("main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt")

        fun longConstant(source: String, name: String): Long =
            Regex("$name = (\\d+)L").find(source)?.groupValues?.get(1)?.toLong()
                ?: error("$name 값을 찾지 못함")

        fun intConstant(source: String, name: String): Int =
            Regex("$name = (\\d+)\\b").find(source)?.groupValues?.get(1)?.toInt()
                ?: error("$name 값을 찾지 못함")

        val introDuration = longConstant(introSource, "INTRO_POSTMARK_HAPTIC_DURATION_MS")
        val introAmplitude = intConstant(introSource, "INTRO_POSTMARK_HAPTIC_AMPLITUDE")
        val longPressDuration = longConstant(gallery, "GalleryFabHapticLongPressDurationMs")
        val longPressAmplitude = intConstant(gallery, "GalleryFabHapticLongPressAmplitude")

        assertTrue(
            "앱 실행 직후 진동이라 롱프레스보다 짧아야 함($introDuration vs $longPressDuration)",
            introDuration < longPressDuration
        )
        assertTrue(
            "롱프레스보다 약해야 함($introAmplitude vs $longPressAmplitude)",
            introAmplitude < longPressAmplitude
        )
    }

    @Test
    fun postmark_animationValuesAreReadInsideGraphicsLayer() {
        // 애니메이션 값은 draw 단계(graphicsLayer)나 코루틴(snapshotFlow)에서만
        // 읽어야 프레임마다 recomposition이 일어나지 않는다. 컴포지션 본문에서
        // 직접 읽으면 소인이 내려앉는 동안 매 프레임 recompose 된다.
        val reads = Regex("stampPress\\.value").findAll(introSource).toList()

        assertTrue("애니메이션 값을 읽는 곳이 있어야 함", reads.isNotEmpty())

        reads.forEach { read ->
            val lookBehind = introSource.substring(
                maxOf(0, read.range.first - 200),
                read.range.first
            )

            assertTrue(
                "stampPress.value는 graphicsLayer나 snapshotFlow 안에서만 읽어야 함",
                lookBehind.contains(".graphicsLayer {") ||
                    lookBehind.contains("snapshotFlow {")
            )
        }

        assertFalse(
            "프레임마다 recompose 되는 animateFloatAsState로 돌아가지 않음",
            introSource.contains("animateFloatAsState")
        )
    }

    // ---- 인트로를 막지 않는다 ----

    @Test
    fun postmark_isOptionalAndDoesNotBlockIntro() {
        assertTrue(
            "방문 기록은 없을 수도 있는(null 허용) 입력이어야 함",
            introSource.contains("visitRecord: VisitRecord? = null")
        )
        assertFalse(
            "인트로가 방문 기록 I/O를 직접 기다리면 안 됨",
            introSource.contains("VisitRecordStorage")
        )
        assertFalse(
            "인트로 안에서 저장소를 읽으면 안 됨",
            introSource.contains("Dispatchers.IO")
        )
    }

    @Test
    fun intro_keepsProgressAnimationUntouched() {
        assertTrue(introSource.contains("INTRO_FILL_DURATION_MS = 1800"))
        assertTrue(introSource.contains("INTRO_SETTLE_DELAY_MS = 150L"))
        assertTrue("봉투 실제 너비 측정 보정 유지", introSource.contains("onGloballyPositioned"))
        assertTrue("진행선/퍼센트 구조 유지", introSource.contains("AppIntroProgress(progress = progress.value)"))
    }

    // ---- 출석 UI 문법 금지선 ----

    @Test
    fun postmark_showsTotalVisitsOnlyNeverStreakPressure() {
        assertTrue(
            "절대 줄지 않는 총 방문일만 노출해야 함",
            introSource.contains("it.totalVisitDays}번째 방문")
        )
        assertFalse(
            "연속 방문일은 이번 범위에서 화면에 노출하지 않음",
            introSource.contains("currentStreakDays}")
        )
    }

    @Test
    fun postmark_hasNoRewardOrPressureVocabulary() {
        // 주석에는 "연속 방문일을 왜 숨기는지" 같은 설명이 있어야 하므로,
        // 사용자에게 실제로 보이는 문자열 리터럴만 검사한다.
        val userFacingText = Regex("\"([^\"\\n]*)\"")
            .findAll(introSource)
            .map { it.groupValues[1] }
            .joinToString("\n")

        val forbidden = listOf(
            "연속", "출석", "보상", "획득", "달성", "놓쳤", "끊겼", "실패",
            "다시 시작", "STREAK", "REWARD", "CLAIM", "DAILY", "포인트", "🔥"
        )

        forbidden.forEach { word ->
            assertFalse(
                "인트로 화면 문구에 출석/보상/압박 문법이 들어가면 안 됨: $word",
                userFacingText.contains(word)
            )
        }
    }

    @Test
    fun postmark_hasNoButtonOrBadgeContainer() {
        assertFalse("소인은 누르는 것이 아님", introSource.contains("clickable"))
        assertFalse("배너/뱃지 컨테이너를 두지 않음", introSource.contains("Card("))
        assertFalse("강조 배경을 두지 않음", introSource.contains("BrutalYellow"))
    }

    // ---- 상단 문구: 방문 기록 도착 전엔 즉시 고르지 않음(33번째 milestone 대비) ----

    @Test
    fun introMessage_isGatedOnVisitRecordArrivalInsteadOfPickedImmediately() {
        assertTrue(
            "방문 기록 도착 여부로 게이팅해야 함(즉시 remember { selectIntroMessage() }로 돌아가지 않음)",
            introSource.contains("remember(hasVisitRecord) {")
        )
        assertFalse(
            "도착을 기다리지 않는 즉시 선택 방식으로 되돌아가면 안 됨",
            introSource.contains("remember { selectIntroMessage() }")
        )
        assertTrue(
            "방문 기록이 도착하면 누적 방문일을 문구 선택에 넘겨야 함",
            introSource.contains("selectIntroMessage(totalVisitDays = visitRecord.totalVisitDays)")
        )
        assertTrue(
            "문구도 소인과 같은 orEmpty() 관례로 도착 전엔 빈 자리로 둬야 함",
            introSource.contains("text = introMessage.orEmpty()")
        )
    }

    // ---- 방문 판정 위치: 프로세스당 1회, IO에서 ----

    @Test
    fun visitIsRecordedOncePerProcessOffTheMainThread() {
        assertTrue(
            "프로세스 수명 holder로 1회만 판정해야 함",
            mainActivitySource.contains("if (AppIntroState.todayVisit == null)")
        )
        assertTrue(
            "저장소 접근은 IO 디스패처에서",
            mainActivitySource.contains("withContext(Dispatchers.IO)")
        )
        assertTrue(
            "판정은 recomposition이 아니라 LaunchedEffect(Unit)에서 한 번",
            mainActivitySource.contains("LaunchedEffect(Unit)")
        )
        assertTrue(
            mainActivitySource.contains("VisitRecordStorage.recordTodayVisit(")
        )
        assertTrue(
            "인트로에 방문 기록을 넘겨줘야 함",
            mainActivitySource.contains("visitRecord = todayVisit?.record")
        )
    }

    @Test
    fun visitRecordingDoesNotTouchRoomOrNewPersistence() {
        assertFalse(
            "방문 기록 때문에 Room을 건드리지 않음",
            mainActivitySource.contains("Room")
        )
        assertFalse(
            "SharedPreferences/DataStore를 새로 들이지 않음",
            mainActivitySource.contains("getSharedPreferences") ||
                mainActivitySource.contains("DataStore")
        )
    }
}
