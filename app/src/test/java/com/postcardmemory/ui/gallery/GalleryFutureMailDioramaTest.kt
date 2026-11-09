package com.postcardmemory.ui.gallery

import com.postcardmemory.ui.futuremail.FutureMailGroup
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 127일차: 기억 밀도 아래 미래 우체통 종이(B안)의 순수 로직 — 상태 선택(도착 우선·엽서 수),
 * 날짜 문구, 문구 모델, 짧은 화면에서 숨기는 높이 판정. 그림·눌림·실제 배치는 JVM으로
 * 보장하지 않는다(실기기 QA).
 */
class GalleryFutureMailDioramaTest {

    private val zone = ZoneId.of("Asia/Seoul")

    private fun millis(date: String): Long =
        LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun group(
        date: String,
        arrived: Boolean,
        daysLeft: Long = 0,
        ids: List<Long> = listOf(1L),
        progress: Int = 50
    ) = FutureMailGroup(
        deliverAtMillis = millis(date),
        arrived = arrived,
        progressPercent = progress,
        daysLeft = daysLeft,
        postcardIds = ids
    )

    @Test
    fun noGroups_isEmpty() {
        assertEquals(FutureMailDioramaState.Empty, futureMailDioramaStateFor(emptyList()))
    }

    @Test
    fun travelingOnly_picksNearestDeliveryAndCountsOtherPostcards() {
        val state = futureMailDioramaStateFor(
            listOf(
                group("2027-03-01", arrived = false, daysLeft = 112, ids = listOf(5L)),
                group("2026-12-24", arrived = false, daysLeft = 45, ids = listOf(1L, 2L))
            )
        )
        assertEquals(
            FutureMailDioramaState.Traveling(
                deliverAtMillis = millis("2026-12-24"),
                daysLeft = 45,
                otherTravelingCount = 2
            ),
            state
        )
    }

    @Test
    fun singleTravelingPostcard_hasNoOthers() {
        val state = futureMailDioramaStateFor(listOf(group("2026-12-24", arrived = false, daysLeft = 45)))
        assertEquals(0, (state as FutureMailDioramaState.Traveling).otherTravelingCount)
    }

    @Test
    fun arrivedWinsOverTraveling_withLatestArrivedDateAndTravelingPostcardCount() {
        val state = futureMailDioramaStateFor(
            listOf(
                group("2026-10-01", arrived = true),
                group("2026-11-01", arrived = true, ids = listOf(3L, 4L)),
                group("2026-12-24", arrived = false, daysLeft = 45, ids = listOf(7L, 8L))
            )
        )
        assertEquals(
            FutureMailDioramaState.Arrived(deliverAtMillis = millis("2026-11-01"), travelingCount = 2),
            state
        )
    }

    @Test
    fun stateIgnoresProgressPercent() {
        val a = futureMailDioramaStateFor(listOf(group("2026-12-24", arrived = false, daysLeft = 45, progress = 0)))
        val b = futureMailDioramaStateFor(listOf(group("2026-12-24", arrived = false, daysLeft = 45, progress = 99)))
        assertEquals(a, b)
    }

    @Test
    fun dateText_omitsYearOnlyWithinSameYear() {
        val now = millis("2026-11-09")
        assertEquals("12월 24일", futureMailDioramaDateText(millis("2026-12-24"), now, zone))
        assertEquals("2027년 1월 3일", futureMailDioramaDateText(millis("2027-01-03"), now, zone))
    }

    @Test
    fun travelingTexts_showDateAndDdayFromGroup() {
        val now = millis("2026-11-09")
        val texts = futureMailDioramaTexts(
            FutureMailDioramaState.Traveling(millis("2026-12-24"), daysLeft = 45, otherTravelingCount = 0),
            now,
            zone
        )
        assertEquals("12월 24일에 도착해", texts.primary)
        assertEquals("D-45", texts.accent)
        assertNull(texts.secondary)
        assertTrue(texts.accessibility.contains("45일 남았어"))

        val withOthers = futureMailDioramaTexts(
            FutureMailDioramaState.Traveling(millis("2026-12-24"), daysLeft = 45, otherTravelingCount = 2),
            now,
            zone
        )
        assertEquals("외 2통", withOthers.secondary)
    }

    @Test
    fun arrivedTexts_mentionTravelingSecondarilyOrOpenHint() {
        val now = millis("2026-11-09")
        val mixed = futureMailDioramaTexts(FutureMailDioramaState.Arrived(millis("2026-11-01"), 2), now, zone)
        assertEquals("편지가 도착했어", mixed.primary)
        assertNull(mixed.accent)
        assertEquals("2통은 아직 여행 중", mixed.secondary)

        val only = futureMailDioramaTexts(FutureMailDioramaState.Arrived(millis("2026-11-01"), 0), now, zone)
        assertEquals("눌러서 열어보기", only.secondary)
    }

    @Test
    fun emptyTexts_giveShortDiscoveryHint() {
        val texts = futureMailDioramaTexts(FutureMailDioramaState.Empty, millis("2026-11-09"), zone)
        assertEquals("미래의 나에게 보내 볼까?", texts.primary)
        assertNull(texts.accent)
        assertEquals("엽서 편집 ⋮ › 미래 우체통", texts.secondary)
    }

    @Test
    fun closedHandleTop_followsRealQuickSelectGeometry() {
        val top = quickSelectClosedHandleTopFromBottomDp()
        assertTrue("닫힌 손잡이 윗변=$top", top in 90f..100f)
        // 펼침 간격이 좁을수록 맨 아래 손이 덜 올라가 손잡이도 낮다 — 최대 간격이 가장 보수적.
        assertTrue(quickSelectClosedHandleTopFromBottomDp(QUICK_SELECT_MIN_SPACING_DEG) < top)
        assertEquals(top - 24f + 4f, futureMailDioramaBottomReserveDp(), 0.001f)
    }

    @Test
    fun fits_onlyWhenGapSheetAndHandleReserveAllFit() {
        val reserve = futureMailDioramaBottomReserveDp()
        val needed = FUTURE_MAIL_DIORAMA_TOP_GAP.value + 92f + reserve
        assertTrue(futureMailDioramaFits(needed, 92f))
        assertFalse(futureMailDioramaFits(needed - 0.5f, 92f))
        // 큰 글꼴로 종이가 높아지면 같은 칸에서도 숨긴다(줄이지 않는다).
        assertFalse(futureMailDioramaFits(needed, 110f))
    }
}
