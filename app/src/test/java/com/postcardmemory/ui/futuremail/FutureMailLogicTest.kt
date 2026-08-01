package com.postcardmemory.ui.futuremail

import com.postcardmemory.data.FUTURE_MAIL_STATE_NONE
import com.postcardmemory.data.FUTURE_MAIL_STATE_SENT
import com.postcardmemory.data.Postcard
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 미래 엽서 날짜 판정/그룹핑 순수 함수를 검증한다. Postcard.futureMailState는
 * DAO/Repository(Context 의존)를 거쳐야 실제로 반영되므로, 이 프로젝트의
 * 순수 JUnit 환경(Robolectric 미사용)에서는 로직만 분리해 검증한다
 * (GallerySearchFilterTest.kt 참고).
 */
class FutureMailLogicTest {

    private val zone = ZoneId.of("Asia/Seoul")

    private fun epochMillisOf(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        ZonedDateTime.of(year, month, day, hour, 0, 0, 0, zone)
            .toInstant()
            .toEpochMilli()

    // ---- isSelectableFutureMailDate ----

    @Test
    fun selectableDate_yesterday_isRejected() {
        val today = epochMillisOf(2026, 7, 31)
        val yesterday = epochMillisOf(2026, 7, 30)

        assertFalse(isSelectableFutureMailDate(yesterday, today, zone))
    }

    @Test
    fun selectableDate_today_isRejected() {
        val today = epochMillisOf(2026, 7, 31, hour = 9)
        val alsoToday = epochMillisOf(2026, 7, 31, hour = 23)

        assertFalse(isSelectableFutureMailDate(alsoToday, today, zone))
    }

    @Test
    fun selectableDate_tomorrow_isAccepted() {
        val today = epochMillisOf(2026, 7, 31)
        val tomorrow = epochMillisOf(2026, 8, 1)

        assertTrue(isSelectableFutureMailDate(tomorrow, today, zone))
    }

    @Test
    fun selectableDate_farFuture_isAccepted() {
        val today = epochMillisOf(2026, 7, 31)
        val nextYear = epochMillisOf(2027, 7, 31)

        assertTrue(isSelectableFutureMailDate(nextYear, today, zone))
    }

    // ---- isFutureMailArrived ----

    @Test
    fun arrived_beforeDeliveryDay_isFalse() {
        val deliverAt = epochMillisOf(2026, 12, 25)
        val now = epochMillisOf(2026, 12, 24, hour = 23)

        assertFalse(isFutureMailArrived(deliverAt, now, zone))
    }

    @Test
    fun arrived_exactlyOnDeliveryDay_isTrue() {
        val deliverAt = epochMillisOf(2026, 12, 25, hour = 15)
        val now = epochMillisOf(2026, 12, 25, hour = 0)

        assertTrue(isFutureMailArrived(deliverAt, now, zone))
    }

    @Test
    fun arrived_longAfterDeliveryDay_isTrue() {
        val deliverAt = epochMillisOf(2026, 12, 25)
        val now = epochMillisOf(2027, 3, 1)

        assertTrue(isFutureMailArrived(deliverAt, now, zone))
    }

    // ---- daysUntilFutureMail ----

    @Test
    fun daysUntil_tomorrow_isOne() {
        val today = epochMillisOf(2026, 7, 31)
        val tomorrow = epochMillisOf(2026, 8, 1)

        assertEquals(1L, daysUntilFutureMail(tomorrow, today, zone))
    }

    @Test
    fun daysUntil_afterArrival_isZeroOrNegative() {
        val deliverAt = epochMillisOf(2026, 7, 31)
        val now = epochMillisOf(2026, 8, 5)

        assertTrue(daysUntilFutureMail(deliverAt, now, zone) <= 0)
    }

    // ---- futureMailProgressPercent / futureMailShipSlotIndex ----

    @Test
    fun progressPercent_startMiddleArrival_areClampedToExpectedBounds() {
        val sentAt = epochMillisOf(2026, 1, 1)
        val deliverAt = epochMillisOf(2026, 1, 11)

        assertEquals(0, futureMailProgressPercent(sentAt, deliverAt, epochMillisOf(2025, 12, 31), zone))
        assertEquals(0, futureMailProgressPercent(sentAt, deliverAt, sentAt, zone))
        assertEquals(50, futureMailProgressPercent(sentAt, deliverAt, epochMillisOf(2026, 1, 6), zone))
        assertEquals(100, futureMailProgressPercent(sentAt, deliverAt, deliverAt, zone))
        assertEquals(100, futureMailProgressPercent(sentAt, deliverAt, epochMillisOf(2026, 1, 20), zone))
    }

    @Test
    fun shipSlotIndex_mapsProgressBoundariesToElevenSlots() {
        val cases = mapOf(
            -10 to 0,
            0 to 0,
            1 to 0,
            24 to 2,
            25 to 3,
            49 to 5,
            50 to 5,
            51 to 5,
            75 to 8,
            99 to 10,
            100 to 10,
            140 to 10
        )

        cases.forEach { (progress, expectedSlot) ->
            assertEquals(expectedSlot, futureMailShipSlotIndex(progress))
        }
    }

    @Test
    fun shipSlotIndex_staysInRangeAndNeverMovesBackwardAsProgressIncreases() {
        var previousSlot = 0

        for (progress in -20..120) {
            val slot = futureMailShipSlotIndex(progress)

            assertTrue(slot in 0..10)
            if (progress >= 0) {
                assertTrue(slot >= previousSlot)
                previousSlot = slot
            }
        }
    }

    // ---- Material3 DatePicker <-> local start-of-day conversion ----

    @Test
    fun materialDatePickerConversion_roundTrips_toSameLocalDate() {
        val localStartOfDay = epochMillisOf(2027, 7, 31, hour = 0)

        val utcMillis = localStartOfDayToMaterialDatePickerUtcMillis(localStartOfDay, zone)
        val roundTripped = materialDatePickerUtcMillisToLocalStartOfDay(utcMillis, zone)

        assertEquals(localStartOfDay, roundTripped)
    }

    @Test
    fun materialDatePickerConversion_negativeUtcOffsetZone_keepsCalendarDate() {
        // UTC-8 존에서도 같은 날짜 정체성을 유지하는지 확인(양의 KST 존만으로는
        // UTC 자정 재해석 버그를 못 잡을 수 있어 반대 부호 존으로도 검증).
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val localStartOfDay = ZonedDateTime.of(2027, 1, 1, 0, 0, 0, 0, losAngeles)
            .toInstant()
            .toEpochMilli()

        val utcMillis = localStartOfDayToMaterialDatePickerUtcMillis(localStartOfDay, losAngeles)
        val roundTripped = materialDatePickerUtcMillisToLocalStartOfDay(utcMillis, losAngeles)

        assertEquals(localStartOfDay, roundTripped)
    }

    // ---- buildFutureMailGroups ----

    private fun sentPostcard(id: Long, deliverAt: Long) = Postcard(
        id = id,
        imagePath = "/$id.jpg",
        title = "postcard-$id",
        capturedAt = epochMillisOf(2026, 7, 31),
        futureMailState = FUTURE_MAIL_STATE_SENT,
        futureMailDeliverAt = deliverAt
    )

    @Test
    fun groups_sameDeliveryDate_areCountedTogether() {
        val christmas = epochMillisOf(2026, 12, 25, hour = 3)
        val christmasLater = epochMillisOf(2026, 12, 25, hour = 21)
        val now = epochMillisOf(2026, 7, 31)

        val groups = buildFutureMailGroups(
            listOf(
                sentPostcard(1, christmas),
                sentPostcard(2, christmasLater)
            ),
            now,
            zone
        )

        assertEquals(1, groups.size)
        assertEquals(2, groups[0].count)
        assertEquals(setOf(1L, 2L), groups[0].postcardIds.toSet())
    }

    @Test
    fun groups_areSortedByDeliveryDateAscending() {
        val now = epochMillisOf(2026, 1, 1)
        val far = epochMillisOf(2028, 7, 27)
        val near = epochMillisOf(2026, 12, 25)

        val groups = buildFutureMailGroups(
            listOf(sentPostcard(1, far), sentPostcard(2, near)),
            now,
            zone
        )

        assertEquals(listOf(near, far).map { startOfDayMillis(it, zone) }, groups.map { it.deliverAtMillis })
    }

    @Test
    fun groups_markArrivedFlagPerGroup() {
        val now = epochMillisOf(2026, 12, 25)
        val alreadyArrived = epochMillisOf(2026, 12, 20)
        val stillTraveling = epochMillisOf(2027, 1, 1)

        val groups = buildFutureMailGroups(
            listOf(sentPostcard(1, alreadyArrived), sentPostcard(2, stillTraveling)),
            now,
            zone
        )

        val arrivedGroup = groups.first { it.postcardIds == listOf(1L) }
        val travelingGroup = groups.first { it.postcardIds == listOf(2L) }

        assertTrue(arrivedGroup.arrived)
        assertFalse(travelingGroup.arrived)
    }

    @Test
    fun groups_ignoreNonSentPostcards() {
        val now = epochMillisOf(2026, 7, 31)
        val normalPostcard = Postcard(
            id = 99,
            imagePath = "/99.jpg",
            title = "normal",
            futureMailState = FUTURE_MAIL_STATE_NONE,
            futureMailDeliverAt = null
        )

        val groups = buildFutureMailGroups(
            listOf(normalPostcard),
            now,
            zone
        )

        assertTrue(groups.isEmpty())
    }

    @Test
    fun groups_emptyInput_returnsEmptyList() {
        assertTrue(buildFutureMailGroups(emptyList(), epochMillisOf(2026, 7, 31), zone).isEmpty())
    }
}
