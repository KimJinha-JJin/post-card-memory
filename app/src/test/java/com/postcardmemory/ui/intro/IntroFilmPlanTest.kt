package com.postcardmemory.ui.intro

import com.postcardmemory.utils.VisitRecord
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 133일차 필름 인트로 계산: 날짜 칸이 시간 순서인지, 오늘 칸이 기준 위치에 정착하는지,
 * 이송 칸 수·시간과 화면 전환 시점(그날 첫 방문 약 2.3초, 같은 날 약 0.83초)을 검증한다.
 */
class IntroFilmPlanTest {

    private val today = LocalDate.of(2026, 11, 15).toEpochDay()

    private fun record(total: Int = 133) = VisitRecord(
        lastVisitEpochDay = today,
        totalVisitDays = total,
        currentStreakDays = 1
    )

    private fun IntroFilmPlan.datedDays(): List<Long> = frames.mapNotNull { it.epochDay }

    private fun IntroFilmPlan.assertChronological() {
        val days = datedDays()
        assertEquals("날짜 칸은 왼쪽부터 오름차순", days.sorted(), days)
        assertEquals("같은 날짜 칸은 한 번만", days.distinct().size, days.size)
    }

    private fun IntroFilmPlan.assertTodayAtIndexAndFutureOnTheRight() {
        assertTrue(frames[todayIndex].isToday)
        assertEquals(1, frames.count { it.isToday })
        val right = frames.drop(todayIndex + 1)
        assertEquals(INTRO_FILM_FUTURE_FRAMES, right.size)
        assertTrue("오늘 오른쪽은 아직 오지 않은 빈 칸", right.all { it.epochDay == null })
    }

    @Test
    fun nextDay_movesOneFrameLeftAndHandStampsToday() {
        val plan = planIntroFilm(record(), true, today - 1, reducedMotion = false)

        plan.assertChronological()
        plan.assertTodayAtIndexAndFutureOnTheRight()
        assertEquals(today - 1, plan.frames[plan.startIndex].epochDay)
        assertTrue(plan.frames[plan.startIndex].hasPastStamp)
        assertEquals("필름은 정확히 한 칸 이송", 1, plan.travelFrames)
        assertEquals(INTRO_FILM_MOVE_MS, plan.moveDurationMs)
        assertEquals(IntroFilmStamp.HAND, plan.stamp)
        assertFalse("오늘 칸은 손 도장으로 찍힘(미리 찍혀 있지 않음)", plan.frames[plan.todayIndex].hasPastStamp)
    }

    @Test
    fun firstVisitOfTheDay_reachesTheGalleryInAbout2_3Seconds() {
        val plan = planIntroFilm(record(), true, today - 1, reducedMotion = false)
        val toGallery = introFilmTimelineMs(plan) + INTRO_TO_GALLERY_CROSSFADE_MS
        // 280 대기 + 720 이송 + 140 + 손 720 + 260 + 전환 180 = 2300ms (+ 기록 도착 수십 ms)
        assertEquals(2300L, toGallery)
    }

    @Test
    fun sameDayAgain_staysStillAndReachesTheGalleryInAbout0_83Seconds() {
        val plan = planIntroFilm(record(), false, today, reducedMotion = false)

        plan.assertChronological()
        plan.assertTodayAtIndexAndFutureOnTheRight()
        assertFalse(plan.moves)
        assertEquals(plan.todayIndex, plan.startIndex)
        assertEquals(IntroFilmStamp.INKED, plan.stamp)
        assertEquals(830L, introFilmTimelineMs(plan) + INTRO_TO_GALLERY_CROSSFADE_MS)
    }

    @Test
    fun thirtyDayGap_travelsEveryMissedDayInOrder() {
        val plan = planIntroFilm(record(), true, today - 30, reducedMotion = false)

        plan.assertChronological()
        plan.assertTodayAtIndexAndFutureOnTheRight()
        assertEquals(today - 30, plan.frames[plan.startIndex].epochDay)
        assertEquals("쉰 29일 + 오늘 = 30칸 이송", 30, plan.travelFrames)

        val missed = plan.frames.subList(plan.startIndex + 1, plan.todayIndex)
        assertEquals(29, missed.size)
        assertEquals(((today - 29)..(today - 1)).toList(), missed.map { it.epochDay })
        assertTrue("쉰 날에는 소인이 없음", missed.none { it.hasPastStamp || it.isToday })

        assertEquals("상한 2.6배 = 1872ms", 1872, plan.moveDurationMs)
        assertEquals(IntroFilmStamp.HAND, plan.stamp)
    }

    @Test
    fun moveDuration_growsWithDaysAndIsCapped() {
        assertEquals(0, introFilmMoveDurationMs(0))
        assertEquals(720, introFilmMoveDurationMs(1))
        assertEquals(1044, introFilmMoveDurationMs(2))
        var last = 0
        for (n in 1..400) {
            val d = introFilmMoveDurationMs(n)
            assertTrue(d >= last)
            assertTrue(d <= 1872)
            last = d
        }
    }

    @Test
    fun veryLongGap_drawsAtMostTheSafetyCapStillInOrder() {
        val plan = planIntroFilm(record(), true, today - 400, reducedMotion = false)

        plan.assertChronological()
        plan.assertTodayAtIndexAndFutureOnTheRight()
        assertEquals(INTRO_FILM_MAX_MISSED_FRAMES + 1, plan.travelFrames)
        assertEquals(today - 400, plan.frames[plan.startIndex].epochDay)
        assertEquals(today - 1, plan.frames[plan.todayIndex - 1].epochDay)
    }

    @Test
    fun firstRunEver_movesFromABlankFrame() {
        val plan = planIntroFilm(record(total = 1), true, null, reducedMotion = false)

        plan.assertTodayAtIndexAndFutureOnTheRight()
        assertEquals(listOf(today), plan.datedDays())
        assertNull(plan.frames[plan.startIndex].epochDay)
        assertEquals(1, plan.travelFrames)
        assertEquals(IntroFilmStamp.HAND, plan.stamp)
    }

    @Test
    fun clockMovedBackwards_doesNotRunTheFilmBackwards() {
        val plan = planIntroFilm(record(), true, today + 2, reducedMotion = false)

        plan.assertChronological()
        assertFalse(plan.moves)
        assertEquals(IntroFilmStamp.INKED, plan.stamp)
        assertTrue("미래 날짜 칸을 만들지 않음", plan.datedDays().all { it <= today })
    }

    @Test
    fun reducedMotion_firstVisit_noTravelAndStampAlreadyInked() {
        val plan = planIntroFilm(record(), true, today - 5, reducedMotion = true)

        plan.assertChronological()
        plan.assertTodayAtIndexAndFutureOnTheRight()
        assertFalse(plan.moves)
        assertEquals(IntroFilmStamp.INKED, plan.stamp)
        assertEquals(INTRO_FILM_REDUCED_HOLD_MS, introFilmTimelineMs(plan))
    }

    @Test
    fun unreadableRecord_hidesDatesAndStampsExceptTodayIfKnown() {
        val plan = planIntroFilm(null, false, null, reducedMotion = false, todayEpochDay = today)

        assertFalse(plan.moves)
        assertEquals(IntroFilmStamp.NONE, plan.stamp)
        assertEquals(listOf(today), plan.datedDays())
        assertTrue(plan.frames.none { it.hasPastStamp })
        assertEquals(INTRO_FILM_STATIC_HOLD_MS, introFilmTimelineMs(plan))

        val unknownDay = planIntroFilm(null, false, null, reducedMotion = false)
        assertTrue(unknownDay.datedDays().isEmpty())
    }

    // ---- 지난 칸 소인 = 실제 방문 기록(달력 표식 + visit_record 직전 방문일) ----

    private fun IntroFilmPlan.stampedDays(): Set<Long> =
        frames.filter { it.hasPastStamp }.mapNotNull { it.epochDay }.toSet()

    private fun IntroFilmPlan.visitOf(day: Long): IntroFilmPastVisit =
        frames.first { it.epochDay == day && !it.isToday }.pastVisit

    @Test
    fun consecutiveVisits_everyMarkedDayIsStamped() {
        val marked = ((today - 5)..(today - 1)).toSet()
        val plan = planIntroFilm(record(), true, today - 1, false, pastVisitedDays = marked)

        assertEquals(((today - 5)..(today - 1)).toSet(), plan.stampedDays())
    }

    @Test
    fun skippedDaysInTheMiddle_stayBlankPaper() {
        val marked = setOf(today - 5, today - 3, today - 1)
        val plan = planIntroFilm(record(), true, today - 1, false, pastVisitedDays = marked)

        assertEquals(marked, plan.stampedDays())
        assertEquals(IntroFilmPastVisit.NOT_VISITED, plan.visitOf(today - 4))
        assertEquals(IntroFilmPastVisit.NOT_VISITED, plan.visitOf(today - 2))
    }

    @Test
    fun gapOf5_10_30Days_onlyRealVisitsAreStampedAndMissedDaysNever() {
        for (gap in listOf(5L, 10L, 30L)) {
            val last = today - gap
            val marked = setOf(last, last - 2) // 직전 방문 + 그 이틀 전만 실제 방문
            val plan = planIntroFilm(record(), true, last, false, pastVisitedDays = marked)

            plan.assertChronological()
            assertEquals("gap=$gap", gap.toInt(), plan.travelFrames)
            assertEquals("gap=$gap", marked, plan.stampedDays())
            val missed = plan.frames.subList(plan.startIndex + 1, plan.todayIndex)
            assertTrue("gap=$gap 쉰 날에는 소인 없음", missed.none { it.hasPastStamp })
            assertTrue(missed.all { it.pastVisit == IntroFilmPastVisit.NOT_VISITED })
        }
    }

    @Test
    fun previousVisitFromVisitRecord_isStampedEvenWithoutACalendarMarker() {
        // 달력 표식 저장소 이전부터 쓰던 기기: visit_record.txt가 직전 방문일을 확인해 준다.
        val plan = planIntroFilm(record(), true, today - 3, false, pastVisitedDays = emptySet())

        assertEquals(setOf(today - 3), plan.stampedDays())
        assertTrue(plan.frames.filter { !it.isToday && it.epochDay != null && it.epochDay != today - 3 }
            .all { it.pastVisit == IntroFilmPastVisit.NOT_VISITED })
    }

    @Test
    fun firstRunEver_hasNoPastStampsEvenIfMarkersAreGiven() {
        val plan = planIntroFilm(record(total = 1), true, null, false, pastVisitedDays = setOf(today - 1))

        assertTrue(plan.stampedDays().isEmpty())
    }

    @Test
    fun sameDayAgain_pastFourDaysFollowMarkersOnly() {
        val plan = planIntroFilm(record(), false, today, false, pastVisitedDays = setOf(today - 1, today - 4))

        assertEquals(setOf(today - 1, today - 4), plan.stampedDays())
        assertEquals(IntroFilmPastVisit.NOT_VISITED, plan.visitOf(today - 2))
    }

    @Test
    fun unreadableMarkers_areUnknownAndNeverStamped() {
        val sameDay = planIntroFilm(record(), false, today, false, pastVisitedDays = null)
        assertTrue(sameDay.stampedDays().isEmpty())
        assertTrue(sameDay.frames.filter { !it.isToday && it.epochDay != null }
            .all { it.pastVisit == IntroFilmPastVisit.UNKNOWN })

        // 그날 첫 방문: 표식은 못 읽었어도 visit_record.txt의 직전 방문일만은 확인된 방문이다.
        val nextDay = planIntroFilm(record(), true, today - 1, false, pastVisitedDays = null)
        assertEquals(setOf(today - 1), nextDay.stampedDays())
        assertEquals(IntroFilmPastVisit.UNKNOWN, nextDay.visitOf(today - 3))
    }

    @Test
    fun recordNotArrivedOrFailed_showsNoPastStampsAtAll() {
        val plan = planIntroFilm(null, false, null, false, todayEpochDay = today, pastVisitedDays = setOf(today - 1))
        assertTrue(plan.stampedDays().isEmpty())
    }

    @Test
    fun clockMovedBackwards_futurePreviousIsNotTreatedAsAVisit() {
        val plan = planIntroFilm(record(), true, today + 2, false, pastVisitedDays = setOf(today - 2))

        assertEquals(setOf(today - 2), plan.stampedDays())
        assertTrue(plan.stampedDays().all { it < today })
    }

    @Test
    fun noPastFrameIsEverStampedWithoutARealRecord() {
        val random = kotlin.random.Random(133)
        repeat(300) {
            val gap = random.nextLong(1, 40)
            val previous = if (random.nextInt(5) == 0) null else today - gap
            val first = random.nextBoolean()
            val marked = if (random.nextInt(6) == 0) {
                null
            } else {
                ((today - 80) until today).filter { random.nextInt(3) == 0 }.toSet()
            }
            val plan = planIntroFilm(record(), first, if (first) previous else today, random.nextBoolean(), pastVisitedDays = marked)
            val prevConfirmed = if (first && previous != null) setOf(previous) else emptySet()
            val allowed = (marked ?: emptySet()) + prevConfirmed
            assertTrue("기록 없는 소인 발견", allowed.containsAll(plan.stampedDays()))
        }
    }

    @Test
    fun lookupDays_coverEveryDatedPastFrame() {
        for (previous in listOf(null, today - 1, today - 5, today - 30, today - 400, today, today + 2)) {
            val lookup = introFilmLookupEpochDays(today, previous)
            for (first in listOf(true, false)) {
                val plan = planIntroFilm(record(), first, previous, false, pastVisitedDays = emptySet())
                val dated = plan.frames.filter { !it.isToday }.mapNotNull { it.epochDay }
                assertTrue("previous=$previous first=$first", lookup.containsAll(dated))
            }
            assertTrue(lookup.all { it < today })
        }
    }

    @Test
    fun handTimingMatchesTheEditorStampHandConstants() {
        // SealStampInteraction: 진입 300 + 누름 50 + 유지 90 + 뗌 60 + 퇴장 220
        assertEquals(300L, INTRO_FILM_HAND_CONTACT_MS)
        assertEquals(300L + 50 + 90 + 60 + 220, INTRO_FILM_HAND_TOTAL_MS)
    }
}
