package com.postcardmemory.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 앱을 하루 처음 열었다는 흔적 하나. 출석 점수나 보상이 아니라 "그날
 * 엽서함에 들렀다"는 기록 자체가 목적이므로, 담는 값은 세 개뿐이다.
 *
 * [lastVisitEpochDay]는 epoch millis가 아니라 **로컬 날짜 그대로**([LocalDate.toEpochDay])
 * 저장한다. 방문은 "사용자가 생활에서 느끼는 오늘"의 문제라서, millis로
 * 저장한 뒤 매번 타임존으로 되돌리면 자정 근처나 타임존 이동에서 하루가
 * 어긋날 수 있기 때문이다(엽서 [com.postcardmemory.data.Postcard.capturedAt]은
 * "그 사진을 찍은 순간"이라 millis가 맞지만, 방문일은 순간이 아니라 날짜다).
 *
 * [currentStreakDays]는 이번 작업에서 저장만 하고 화면에 노출하지 않는다 —
 * 연속이 끊겼다는 표현은 사용자를 압박하므로, 인트로에는 절대 줄지 않는
 * [totalVisitDays]만 보여준다.
 */
data class VisitRecord(
    val lastVisitEpochDay: Long,
    val totalVisitDays: Int,
    val currentStreakDays: Int
)

/**
 * 오늘 방문을 기록한 결과. [record]는 기록 뒤의 상태이고,
 * [isFirstVisitToday]는 **이번 실행이 오늘의 첫 방문이어서 기록이 실제로 새로
 * 남았는지**를 뜻한다 — 같은 날 두 번째 실행부터는 false다.
 *
 * 파일에 저장되는 값이 아니라 "이번 실행"에 대한 정보다. 소인은 앱을 열
 * 때마다 찍히지만 도장이 닿는 진동은 그날 처음 찍힐 때만 울리는 데 쓴다.
 */
data class TodayVisit(
    val record: VisitRecord,
    val isFirstVisitToday: Boolean
)

/** 저장 형식 버전. 파싱 실패 시 기록이 초기화되므로 형식은 쉽게 바꾸지 않는다. */
private const val VISIT_RECORD_FORMAT_VERSION = "1"

/** 엽서 꾸미기 요소들과 동일한 탭 구분 한 줄 직렬화(PostcardSealItem.serialize와 같은 방식). */
internal fun VisitRecord.serialize(): String =
    listOf(
        VISIT_RECORD_FORMAT_VERSION,
        lastVisitEpochDay.toString(),
        totalVisitDays.toString(),
        currentStreakDays.toString()
    ).joinToString("\t")

/**
 * 저장된 한 줄을 되돌린다. 형식이 다르거나 값이 말이 안 되면(음수, 총
 * 방문일보다 큰 연속일 등) 조용히 null을 준다 — 손상된 값을 그대로 믿고
 * 계산을 이어가면 총 방문일이 폭증하는 쪽이 더 위험하다.
 */
internal fun parseVisitRecord(text: String): VisitRecord? {
    val parts = text.trim().split("\t")
    if (parts.size < 4) return null
    if (parts[0] != VISIT_RECORD_FORMAT_VERSION) return null

    val lastVisitEpochDay = parts[1].toLongOrNull() ?: return null
    val totalVisitDays = parts[2].toIntOrNull() ?: return null
    val currentStreakDays = parts[3].toIntOrNull() ?: return null

    if (totalVisitDays < 1 || currentStreakDays < 1) return null
    if (currentStreakDays > totalVisitDays) return null

    return VisitRecord(
        lastVisitEpochDay = lastVisitEpochDay,
        totalVisitDays = totalVisitDays,
        currentStreakDays = currentStreakDays
    )
}

/**
 * [nowMillis]가 속한 로컬 날짜. [com.postcardmemory.ui.futuremail.startOfDayMillis]와
 * 같은 관례(표시·판정 시점에 [ZoneId.systemDefault]로 변환)를 따르되, 방문은
 * 날짜 단위이므로 자정 millis 대신 epochDay까지 줄인다.
 */
internal fun visitEpochDay(
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long =
    Instant.ofEpochMilli(nowMillis)
        .atZone(zone)
        .toLocalDate()
        .toEpochDay()

/**
 * epochDay를 그 날 자정의 epoch millis로 되돌린다. 소인에 날짜를 찍는
 * [com.postcardmemory.ui.components.SealPreviewContent]가 앱 공통 서식
 * ([com.postcardmemory.ui.components.PostcardDateFormat.formatIso])으로
 * millis를 받기 때문에, 표시 직전에만 이 변환을 쓴다.
 */
internal fun visitDayStartMillis(
    epochDay: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long =
    LocalDate.ofEpochDay(epochDay)
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

/**
 * 오늘 방문을 기록한 결과를 만든다. 순수 함수라 실제 날짜가 바뀌기를
 * 기다리지 않고 검증할 수 있다.
 *
 * - 기록이 없으면 오늘이 첫 방문이다(총 1일, 연속 1일).
 * - 같은 날 다시 열면 [previous]를 **그대로** 돌려준다 — 하루에 여러 번
 *   열어도 총 방문일과 연속 방문일은 늘지 않는다. 반환값이 입력과 같으므로
 *   저장 쪽에서 "바뀌었는지"만 보고 불필요한 파일 쓰기를 건너뛸 수 있다.
 * - 어제에 이어 열면 총 방문일 +1, 연속 방문일 +1.
 * - 며칠 쉬었다 돌아오면 총 방문일 +1, 연속 방문일은 1로 다시 시작한다.
 *   실패로 취급하지 않는다.
 * - 기기 시계가 과거로 움직여 마지막 방문일보다 이전 날짜가 오면, 기록
 *   위치([VisitRecord.lastVisitEpochDay])만 오늘로 되돌리고 숫자는 건드리지
 *   않는다. 잘못 맞춰진 시계 때문에 총 방문일이 늘어나지도 않고, 반대로
 *   실제 날짜가 따라잡을 때까지 기록이 영구히 멈추지도 않는다. 서버 시간
 *   검증이나 조작 감지는 하지 않는다 — 보안 기능이 아니다.
 */
internal fun recordVisit(
    previous: VisitRecord?,
    todayEpochDay: Long
): VisitRecord {
    if (previous == null) {
        return VisitRecord(
            lastVisitEpochDay = todayEpochDay,
            totalVisitDays = 1,
            currentStreakDays = 1
        )
    }

    val lastVisitEpochDay = previous.lastVisitEpochDay

    return when {
        lastVisitEpochDay == todayEpochDay -> previous

        lastVisitEpochDay > todayEpochDay ->
            previous.copy(lastVisitEpochDay = todayEpochDay)

        lastVisitEpochDay == todayEpochDay - 1L ->
            VisitRecord(
                lastVisitEpochDay = todayEpochDay,
                totalVisitDays = previous.totalVisitDays + 1,
                currentStreakDays = previous.currentStreakDays + 1
            )

        else ->
            VisitRecord(
                lastVisitEpochDay = todayEpochDay,
                totalVisitDays = previous.totalVisitDays + 1,
                currentStreakDays = 1
            )
    }
}
