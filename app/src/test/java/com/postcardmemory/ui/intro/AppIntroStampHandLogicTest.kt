package com.postcardmemory.ui.intro

import com.postcardmemory.utils.VisitRecord
import com.postcardmemory.utils.VisitRecordStorage
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 100일차: 인트로 손 도장은 "오늘 첫 방문으로 기록이 새로 남은 실행"에서만 재생한다.
 * 판정은 기존 방문 기록 결과([com.postcardmemory.utils.TodayVisit])만 쓰고 새 저장 상태가 없다.
 */
class AppIntroStampHandLogicTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val zone: ZoneId = ZoneId.of("Asia/Seoul")

    private fun millisAt(text: String): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private val someRecord = VisitRecord(
        lastVisitEpochDay = 20_000L,
        totalVisitDays = 100,
        currentStreakDays = 3
    )

    @Test
    fun firstVisitOfTheDay_playsTheStampHand() {
        assertTrue(shouldPlayIntroStampHand(someRecord, isFirstVisitToday = true))
    }

    @Test
    fun revisitOnTheSameDay_doesNotPlayTheStampHand() {
        assertFalse(shouldPlayIntroStampHand(someRecord, isFirstVisitToday = false))
    }

    @Test
    fun unknownRecord_doesNotPlayTheStampHand() {
        // 기록을 읽지 못한 실행은 소인 자체를 숨기므로 손도 들어오지 않는다.
        assertFalse(shouldPlayIntroStampHand(null, isFirstVisitToday = true))
        assertFalse(shouldPlayIntroStampHand(null, isFirstVisitToday = false))
    }

    @Test
    fun realStorage_playsOnceOnTheFirstRunOfTheDay_andNotOnTheSameDayRerun() {
        val filesDir = tempFolder.root

        val first = VisitRecordStorage.recordTodayVisit(
            filesDir, nowMillis = millisAt("2026-10-11T08:00:00"), zone = zone
        )
        val again = VisitRecordStorage.recordTodayVisit(
            filesDir, nowMillis = millisAt("2026-10-11T20:00:00"), zone = zone
        )

        assertTrue(shouldPlayIntroStampHand(first.record, first.isFirstVisitToday))
        assertFalse(shouldPlayIntroStampHand(again.record, again.isFirstVisitToday))
        // 연출 판정과 무관하게 방문은 하루 1회 그대로다.
        assertEquals(1, again.record?.totalVisitDays)
    }
}
