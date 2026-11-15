package com.postcardmemory.ui.intro

import com.postcardmemory.utils.VisitRecord
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 133일차 필름형 인트로의 순수 계산. 화면(Compose)과 분리해 JVM 테스트로 날짜 배열·이동 칸 수·
 * 시간표를 검증한다. 방문을 기록하거나 다시 판정하지 않는다 — [com.postcardmemory.MainActivity]가
 * 이미 끝낸 결과([VisitRecord], 그날 첫 방문 여부, 직전 방문일)를 받아 **보여줄 모습만** 고른다.
 *
 * 화면 배열은 시간 순서다: 왼쪽 = 지난 날, 가운데 기준 칸, 오른쪽 = 아직 오지 않은 날(빈 칸).
 * 그날 첫 방문이면 직전 방문 칸이 기준 위치에서 출발해 필름이 **왼쪽으로** 이송되고, 오늘 칸이
 * 오른쪽에서 들어와 기준 위치에 정착한다. 며칠 쉬었다면 쉰 날(도장 없는 날짜 칸)이 그 수만큼 지나간다
 * (사용자 확정: 경과 일수만큼 이동).
 *
 * 지난 날짜 칸의 소인은 **실제 방문 기록과 일치**해야 한다: 달력과 같은 방문 표식이 확인된 날,
 * 또는 visit_record.txt의 직전 방문일에만 찍고, 미방문·판별 불가 날짜는 빈 종이로 둔다
 * ([IntroFilmPastVisit]). 가상·장식용 소인을 만들지 않는다.
 */

/**
 * 지난 날짜 칸의 실제 방문 여부. 소인은 [VISITED]에만 찍힌다.
 * - [VISITED]: 달력과 같은 방문 표식(`visits/history/<epochDay>.visit`)이 있거나,
 *   visit_record.txt가 직전 방문일로 기록한 날.
 * - [NOT_VISITED]: 표식을 정상적으로 확인했는데 없는 날(쉰 날 포함). 표식 저장소가 생기기 전의 날도
 *   표식이 없으므로 여기에 들어간다 — 확인되지 않은 방문에 소인을 찍지 않는다.
 * - [UNKNOWN]: 표식을 읽지 못했거나 아직 받지 못해 판별할 수 없는 날. 소인 없음.
 */
internal enum class IntroFilmPastVisit { VISITED, NOT_VISITED, UNKNOWN }

/** 필름 한 칸. [epochDay]가 null이면 날짜가 없는 빈 칸(아직 오지 않은 날, 기록 이전). */
internal data class IntroFilmFrame(
    val epochDay: Long?,
    val isToday: Boolean = false,
    /** 지난 날짜 칸의 실제 방문 여부. 오늘 칸·빈 칸은 의미 없음(UNKNOWN). */
    val pastVisit: IntroFilmPastVisit = IntroFilmPastVisit.UNKNOWN
) {
    /** 지난 방문 칸에 이미 찍혀 있는 소인. 실제 방문이 확인된 지난 날짜에만 true. */
    val hasPastStamp: Boolean
        get() = !isToday && epochDay != null && pastVisit == IntroFilmPastVisit.VISITED
}

internal enum class IntroFilmStamp {
    /** 그날 첫 방문: 기존 손 도장이 들어와 꾹 찍는다. */
    HAND,

    /** 같은 날 재실행·날짜 되돌림·애니메이션 감소: 움직임 없이 이미 찍힌 모습. */
    INKED,

    /** 방문 기록을 읽지 못함(또는 대기 한도 초과): 날짜·소인·방문 수를 숨긴다. */
    NONE
}

internal data class IntroFilmPlan(
    val frames: List<IntroFilmFrame>,
    /** 시작할 때 기준 위치에 있는 칸. */
    val startIndex: Int,
    /** 오늘 칸(끝날 때 기준 위치에 정착). */
    val todayIndex: Int,
    val stamp: IntroFilmStamp,
    /** 필름 이송 시간(ms). 0이면 움직이지 않는다. */
    val moveDurationMs: Int,
    /** 움직임 없는 계획([IntroFilmStamp.INKED]/[IntroFilmStamp.NONE])에서 보여주는 시간(ms). */
    val holdMs: Long = INTRO_FILM_STATIC_HOLD_MS
) {
    val moves: Boolean get() = moveDurationMs > 0 && todayIndex != startIndex

    /** 이송으로 지나가는 칸 수. */
    val travelFrames: Int get() = todayIndex - startIndex
}

/** 방문 기록을 이 시간까지만 기다린다. 넘으면 기록 없이(날짜·소인 숨김) 지나간다. */
internal const val INTRO_FILM_RECORD_WAIT_MS = 700L

/** 기록이 도착한 뒤 직전 방문 칸을 잠깐 보여주고 이송을 시작한다. */
internal const val INTRO_FILM_MOVE_START_DELAY_MS = 280L

/** A안: 한 칸 이송 시간. 끝에서 길게 감속한다(CubicBezier 0.40, 0, 0.12, 1). */
internal const val INTRO_FILM_MOVE_MS = 720

/** 여러 칸이면 한 칸 늘 때마다 이송 시간이 이만큼(배) 늘어난다. */
internal const val INTRO_FILM_EXTRA_PER_FRAME = 0.45f

/** 이송 시간 상한(한 칸 시간의 배수). 30일 공백도 1.87초 안에 끝난다. 속도 조정은 실기기 QA 이후. */
internal const val INTRO_FILM_MAX_MOVE_FACTOR = 2.6f

/** 이송이 멈춘 뒤 손 도장이 들어오기까지. */
internal const val INTRO_FILM_STAMP_DELAY_MS = 140L

/** 손이 다 빠진 뒤 찍힌 모습을 보여주는 시간. */
internal const val INTRO_FILM_AFTER_HAND_HOLD_MS = 260L

/** 같은 날 재실행·기록 없음: 움직임 없이 보여주고 넘어가는 시간. */
internal const val INTRO_FILM_STATIC_HOLD_MS = 650L

/** 감소 모드 그날 첫 방문: 움직임 없이 찍힌 모습을 보여주는 시간. */
internal const val INTRO_FILM_REDUCED_HOLD_MS = 800L

/** 오늘 칸 오른쪽에 보이는 아직 오지 않은 날 칸 수. */
internal const val INTRO_FILM_FUTURE_FRAMES = 3

/** 직전 방문 칸 왼쪽에 보이는 지난 칸 수. */
internal const val INTRO_FILM_HISTORY_FRAMES = 4

/**
 * 한 번에 그리는 쉰 날 칸의 상한(안전장치). 이보다 오래 쉬면 가장 최근 쉰 날들만 칸으로 그리고
 * 그 앞의 날짜는 건너뛴다 — 날짜 순서는 그대로 오름차순이다. 수백 일 공백에서 칸 수백 개를 한
 * 번에 그리지 않으려는 것으로, 30일 공백에는 걸리지 않는다.
 */
internal const val INTRO_FILM_MAX_MISSED_FRAMES = 60

/** 손 도장 연출 길이(SealStampHandOverlay 실측 상수 합: 진입 300 + 누름 50 + 유지 90 + 뗌 60 + 퇴장 220). */
internal const val INTRO_FILM_HAND_CONTACT_MS = 300L
internal const val INTRO_FILM_HAND_TOTAL_MS = 720L

/** 갤러리로 넘어가는 MainActivity Crossfade 길이(tween(180)). */
internal const val INTRO_TO_GALLERY_CROSSFADE_MS = 180L

/**
 * 그날 첫 방문 연출(이송·손 도장)을 하는지. 직전 방문일이 오늘보다 앞일 때만(또는 최초 실행) 한다 —
 * 기기 날짜를 되돌린 경우(직전 방문일 > 오늘)는 필름을 거꾸로 돌리지 않고 정지한다.
 */
internal fun introFilmPlaysVisit(today: Long, isFirstVisitToday: Boolean, previous: Long?): Boolean =
    isFirstVisitToday && (previous == null || previous < today)

/**
 * 오늘 칸 왼쪽에 놓을 지난 칸의 날짜(왼쪽부터, null = 날짜 없는 빈 칸).
 * - 연출 없음(같은 날 재실행·날짜 되돌림): 오늘 바로 앞 [INTRO_FILM_HISTORY_FRAMES]일.
 * - 최초 실행: 빈 칸만.
 * - 그날 첫 방문: 직전 방문일 앞 [INTRO_FILM_HISTORY_FRAMES]일 · 직전 방문일 · 쉰 날(최근 순으로 최대
 *   [INTRO_FILM_MAX_MISSED_FRAMES]일).
 */
internal fun introFilmPastEpochDays(today: Long, previous: Long?, playsVisit: Boolean): List<Long?> = when {
    !playsVisit -> (INTRO_FILM_HISTORY_FRAMES downTo 1).map { today - it }
    previous == null -> List(INTRO_FILM_HISTORY_FRAMES + 1) { null }
    else -> {
        val missed = min((today - previous - 1).coerceAtLeast(0), INTRO_FILM_MAX_MISSED_FRAMES.toLong())
        (INTRO_FILM_HISTORY_FRAMES downTo 0).map { previous - it } + (missed downTo 1).map { today - it }
    }
}

/**
 * MainActivity가 방문 표식을 읽어 둘 날짜들(읽기 전용). 어떤 연출이 고를 수 있는 지난 칸이든 모두 포함한다
 * (최대 [INTRO_FILM_HISTORY_FRAMES] * 2 + 1 + [INTRO_FILM_MAX_MISSED_FRAMES]일).
 */
internal fun introFilmLookupEpochDays(today: Long, previous: Long?): Set<Long> =
    (introFilmPastEpochDays(today, previous, playsVisit = true) +
        introFilmPastEpochDays(today, previous, playsVisit = false))
        .filterNotNull()
        .filter { it < today }
        .toSet()

/**
 * 지난 날짜 [day]의 실제 방문 여부.
 * visit_record.txt가 직전 방문일로 기록한 날([previous] < [today])은 방문이 확인된 날이다.
 * 그 밖에는 달력 표식만 근거로 쓰고, 표식을 확인하지 못했으면([visitedDays] == null) 판별 불가다.
 */
internal fun introFilmPastVisit(day: Long, today: Long, previous: Long?, visitedDays: Set<Long>?): IntroFilmPastVisit =
    when {
        day >= today -> IntroFilmPastVisit.UNKNOWN
        previous != null && previous < today && day == previous -> IntroFilmPastVisit.VISITED
        visitedDays == null -> IntroFilmPastVisit.UNKNOWN
        day in visitedDays -> IntroFilmPastVisit.VISITED
        else -> IntroFilmPastVisit.NOT_VISITED
    }

internal fun introFilmMoveDurationMs(travelFrames: Int): Int {
    if (travelFrames <= 0) return 0
    val factor = min(1f + INTRO_FILM_EXTRA_PER_FRAME * (travelFrames - 1), INTRO_FILM_MAX_MOVE_FACTOR)
    // 720 × 2.6f가 부동소수점으로 1871.99…가 되므로 버리지 않고 반올림한다(목업과 같은 1872ms).
    return (INTRO_FILM_MOVE_MS * factor).roundToInt()
}

/**
 * 보여줄 필름을 정한다.
 *
 * - [record] == null(읽기 실패·대기 한도 초과): 움직임·소인 없음. 날짜는 [todayEpochDay]를 알 때 오늘 칸에만.
 * - 같은 날 재실행([isFirstVisitToday] == false): 오늘 칸이 처음부터 기준 위치, 소인은 이미 찍힌 모습.
 * - 기기 날짜를 되돌림(직전 방문일 > 오늘): 같은 날처럼 정지. 필름을 거꾸로 돌리지 않는다.
 * - 최초 실행([previousVisitEpochDay] == null): 빈 칸에서 오늘 칸으로 한 칸 이송.
 * - 그 밖의 첫 방문: 직전 방문 칸에서 오늘 칸까지, 쉰 날 수만큼 이송.
 * - [reducedMotion]: 이송 없이 오늘 칸이 기준 위치, 소인은 찍힌 모습.
 */
internal fun planIntroFilm(
    record: VisitRecord?,
    isFirstVisitToday: Boolean,
    previousVisitEpochDay: Long?,
    reducedMotion: Boolean,
    todayEpochDay: Long? = record?.lastVisitEpochDay,
    /**
     * 지난 날짜 칸 중 달력 방문 표식이 확인된 날([com.postcardmemory.utils.VisitHistoryStorage.loadDays]).
     * null이면 표식을 읽지 못했거나 아직 받지 못한 것 — 해당 칸은 판별 불가로 소인을 찍지 않는다.
     */
    pastVisitedDays: Set<Long>? = null
): IntroFilmPlan {
    val future = List(INTRO_FILM_FUTURE_FRAMES) { IntroFilmFrame(epochDay = null) }

    if (record == null) {
        val frames = List(INTRO_FILM_HISTORY_FRAMES + 1) { IntroFilmFrame(epochDay = null) } +
            IntroFilmFrame(epochDay = todayEpochDay, isToday = true) + future
        val today = INTRO_FILM_HISTORY_FRAMES + 1
        return IntroFilmPlan(frames, today, today, IntroFilmStamp.NONE, 0)
    }

    val today = record.lastVisitEpochDay
    val previous = previousVisitEpochDay
    val firstRun = previous == null
    val playsVisit = introFilmPlaysVisit(today, isFirstVisitToday, previous)

    val past = introFilmPastEpochDays(today, previous, playsVisit).map { day ->
        IntroFilmFrame(
            epochDay = day,
            pastVisit = if (day == null) {
                IntroFilmPastVisit.UNKNOWN
            } else {
                introFilmPastVisit(day, today, previous, pastVisitedDays)
            }
        )
    }

    val frames = past + IntroFilmFrame(epochDay = today, isToday = true) + future
    val todayIndex = past.size

    if (!playsVisit) {
        return IntroFilmPlan(frames, todayIndex, todayIndex, IntroFilmStamp.INKED, 0)
    }
    if (reducedMotion) {
        return IntroFilmPlan(
            frames, todayIndex, todayIndex, IntroFilmStamp.INKED, 0,
            holdMs = INTRO_FILM_REDUCED_HOLD_MS
        )
    }

    // 최초 실행은 마지막 빈 칸, 그 밖에는 직전 방문 칸에서 출발한다.
    val startIndex = if (firstRun) todayIndex - 1 else INTRO_FILM_HISTORY_FRAMES
    return IntroFilmPlan(
        frames = frames,
        startIndex = startIndex,
        todayIndex = todayIndex,
        stamp = IntroFilmStamp.HAND,
        moveDurationMs = introFilmMoveDurationMs(todayIndex - startIndex)
    )
}

/**
 * 필름이 보이기 시작한 순간(기록 도착 또는 대기 한도)부터 [onFinished]까지(ms).
 * 실제 화면 전환은 여기에 [INTRO_TO_GALLERY_CROSSFADE_MS]가 더해진다.
 */
internal fun introFilmTimelineMs(plan: IntroFilmPlan): Long = when (plan.stamp) {
    IntroFilmStamp.HAND ->
        INTRO_FILM_MOVE_START_DELAY_MS + plan.moveDurationMs + INTRO_FILM_STAMP_DELAY_MS +
            INTRO_FILM_HAND_TOTAL_MS + INTRO_FILM_AFTER_HAND_HOLD_MS
    else -> plan.holdMs
}
