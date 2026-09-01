package com.postcardmemory.ui.futuremail

import com.postcardmemory.data.FUTURE_MAIL_STATE_SENT
import com.postcardmemory.data.Postcard
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * 미래 엽서 기능의 날짜 판정을 전부 이 파일에 모은다. 앱 전체가 capturedAt과
 * 동일하게 epoch millis로 저장하고 표시 시점에 ZoneId.systemDefault()로
 * 변환하는 관례([PostcardDetailRow], [GalleryScreen])를 그대로 따른다.
 */

/** epochMillis가 속한 날짜의 자정(해당 타임존 기준) epoch millis. */
internal fun startOfDayMillis(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long =
    Instant.ofEpochMilli(epochMillis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

/**
 * 사용자가 고른 날짜를 발송 가능한 "미래" 날짜로 받아줄지 판정한다.
 * 오늘 포함 이전 날짜는 허용하지 않는다 — 오늘을 허용하면 보내자마자
 * 도착 판정이 나버려 "한 번 보내면 도착일까지 못 연다"는 약속이
 * 무의미해지기 때문에, 최소 하루는 실제로 떠나 있어야 한다.
 */
internal fun isSelectableFutureMailDate(
    selectedDateMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Boolean =
    startOfDayMillis(selectedDateMillis, zone) > startOfDayMillis(nowMillis, zone)

/** deliverAt 당일의 자정이 지났으면(=그 날짜가 되었으면) 도착으로 본다. */
internal fun isFutureMailArrived(
    deliverAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Boolean =
    startOfDayMillis(nowMillis, zone) >= startOfDayMillis(deliverAtMillis, zone)

/** 도착까지 남은 일수. 이미 도착했다면 0 이하 값이 나온다(음수 허용). */
internal fun daysUntilFutureMail(
    deliverAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long =
    ChronoUnit.DAYS.between(
        Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate(),
        Instant.ofEpochMilli(deliverAtMillis).atZone(zone).toLocalDate()
    )

/**
 * Material3 DatePicker(selectedDateMillis)는 선택한 날짜를 "그 날짜의 UTC
 * 자정" epoch millis로 표현한다. 이를 앱 전체 관례(capturedAt 등)와 동일한
 * "로컬 타임존 자정" epoch millis로 다시 해석한다.
 */
internal fun materialDatePickerUtcMillisToLocalStartOfDay(
    utcMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long =
    Instant.ofEpochMilli(utcMillis)
        .atZone(ZoneOffset.UTC)
        .toLocalDate()
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()

/** 위 변환의 역방향 — 로컬 자정 epoch millis를 DatePicker의 initialSelectedDateMillis 형식으로. */
internal fun localStartOfDayToMaterialDatePickerUtcMillis(
    localStartOfDayMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long =
    Instant.ofEpochMilli(localStartOfDayMillis)
        .atZone(zone)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

/** 우체통 화면에 표시할, 같은 도착일끼리 묶인 배송 그룹 하나. */
data class FutureMailGroup(
    val deliverAtMillis: Long,
    val arrived: Boolean,
    val progressPercent: Int,
    val postcardIds: List<Long>
) {
    val count: Int get() = postcardIds.size
}

fun futureMailProgressPercent(
    sentAtMillis: Long,
    deliverAtMillis: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Int {
    val sentDay = startOfDayMillis(sentAtMillis, zone)
    val deliverDay = startOfDayMillis(deliverAtMillis, zone)
    val nowDay = startOfDayMillis(nowMillis, zone)
    val totalMillis = deliverDay - sentDay

    if (totalMillis <= 0L) {
        return if (nowDay >= deliverDay) 100 else 0
    }

    val elapsedMillis = nowDay - sentDay
    return (elapsedMillis.toDouble() / totalMillis.toDouble() * 100.0)
        .roundToInt()
        .coerceIn(0, 100)
}

fun futureMailShipSlotIndex(progressPercent: Int): Int =
    (progressPercent.coerceIn(0, 100) / 100.0 * 10.0)
        .roundToInt()
        .coerceIn(0, 10)

/**
 * 배송 중인(SENT) 엽서 목록을 도착일(자정 기준) 단위로 묶어 날짜순으로
 * 정렬한다. 내용(사진/문구 등)은 postcardIds만 남기고 전혀 담지 않아,
 * 이 함수의 반환값만으로는 우체통 화면이 엽서 내용을 유추할 수 없다.
 */
fun buildFutureMailGroups(
    sentPostcards: List<Postcard>,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): List<FutureMailGroup> =
    sentPostcards
        .filter { it.futureMailState == FUTURE_MAIL_STATE_SENT && it.futureMailDeliverAt != null }
        .groupBy { startOfDayMillis(it.futureMailDeliverAt!!, zone) }
        .map { (deliverAtDay, group) ->
            val progressPercent = group
                .map {
                    futureMailProgressPercent(
                        sentAtMillis = it.capturedAt,
                        deliverAtMillis = deliverAtDay,
                        nowMillis = nowMillis,
                        zone = zone
                    )
                }
                .average()
                .roundToInt()
                .coerceIn(0, 100)

            FutureMailGroup(
                deliverAtMillis = deliverAtDay,
                arrived = isFutureMailArrived(deliverAtDay, nowMillis, zone),
                progressPercent = progressPercent,
                postcardIds = group.map { it.id }
            )
        }
        .sortedBy { it.deliverAtMillis }
