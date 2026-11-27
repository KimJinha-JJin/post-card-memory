package com.postcardmemory.ui.gallery

import com.postcardmemory.data.Postcard
import com.postcardmemory.data.FUTURE_MAIL_STATE_NONE
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

internal enum class MemoryScalePeriod { ALL, YEAR, MONTH }

internal const val MEMORY_SCALE_DURATION_MS = 2_400L
internal const val MEMORY_SCALE_CONTACT_MS = 400L

internal data class GalleryPostcardMonth(val yearMonth: YearMonth, val postcards: List<Postcard>) {
    val count: Int get() = postcards.size
}

/** 기존 월별 집계 의미와 빈 달·1~12월 순서를 유지한다. 그래프 렌더링과는 독립이다. */
internal fun postcardMonthsForYear(
    postcards: List<Postcard>, year: Int, zoneId: ZoneId = ZoneId.systemDefault()
): List<GalleryPostcardMonth> {
    val grouped = postcards.groupBy { YearMonth.from(Instant.ofEpochMilli(it.capturedAt).atZone(zoneId)) }
    return (1..12).map { month ->
        val yearMonth = YearMonth.of(year, month)
        GalleryPostcardMonth(yearMonth, grouped[yearMonth].orEmpty())
    }
}

/** 저장 통계 없이 기존 일반 보관 목록을 읽는다. 날짜 의미는 갤러리와 같다. */
internal fun memoryScalePostcards(
    postcards: List<Postcard>,
    period: MemoryScalePeriod,
    date: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): List<Postcard> {
    val ordinary = postcards.filter { it.futureMailState == FUTURE_MAIL_STATE_NONE }
    if (period == MemoryScalePeriod.ALL) return ordinary
    val months = postcardMonthsForYear(ordinary, date.year, zone)
    return if (period == MemoryScalePeriod.YEAR) months.flatMap { it.postcards }
        else months[date.monthValue - 1].postcards
}

private val MemoryScaleCapacityFactors = longArrayOf(1, 2, 5)

/** Int 크기의 목록 전체를 담는다. HTML의 시험 입력 상한은 앱 제한이 아니다. */
internal fun memoryScaleCapacity(count: Int): Long {
    val required = count.coerceAtLeast(0).toLong()
    if (required <= 60L) return 60L
    var magnitude = 100L
    while (true) {
        for (factor in MemoryScaleCapacityFactors) {
            val capacity = magnitude * factor
            if (required <= capacity) return capacity
        }
        magnitude *= 10
    }
}

internal data class MemoryScaleFrame(
    val trayY: Float,
    val needleDegrees: Float,
    val layers: Int,
    val falling: Boolean,
    val dropY: Float,
    val finished: Boolean
)

private fun memoryScaleResponse(elapsedMs: Long, w: Double, d: Double): Double {
    if (elapsedMs <= 0) return 0.0
    val seconds = elapsedMs / 1000.0
    return 1 - exp(-d * seconds) * (cos(w * seconds) + d / w * sin(w * seconds))
}

/** 승인 B: 하나의 접촉 시각/하중을 받침대와 바늘의 감쇠 응답에 전달한다. */
internal fun memoryScaleFrame(count: Int, elapsedMs: Long, reducedMotion: Boolean = false): MemoryScaleFrame {
    val safeCount = count.coerceAtLeast(0)
    val finished = reducedMotion || safeCount == 0 || elapsedMs >= MEMORY_SCALE_DURATION_MS
    val time = elapsedMs.coerceAtLeast(0)
    val ratio = safeCount.toDouble() / memoryScaleCapacity(safeCount)
    val loadTime = time - MEMORY_SCALE_CONTACT_MS
    val trayLoad = if (finished) 1.0 else memoryScaleResponse(loadTime, 21.0, 20.0)
    val needleLoad = if (finished) 1.0 else memoryScaleResponse(loadTime, 16.0, 15.0)
    val falling = safeCount > 0 && !finished && time in 60 until MEMORY_SCALE_CONTACT_MS
    val dropProgress = ((time - 60) / 340.0).coerceIn(0.0, 1.0)
    return MemoryScaleFrame(
        trayY = (16 * (ratio * trayLoad).coerceIn(0.0, 1.0)).toFloat(),
        needleDegrees = (-120 + 240 * (ratio * needleLoad).coerceIn(0.0, 1.0)).toFloat(),
        layers = if (finished || time >= MEMORY_SCALE_CONTACT_MS) safeCount.coerceAtMost(12) else 0,
        falling = falling,
        dropY = if (finished) 0f else (-68 * (1 - dropProgress * dropProgress)).toFloat(),
        finished = finished
    )
}
