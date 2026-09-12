package com.postcardmemory.ui.intro

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIntroMessageLogicTest {

    // ---- isSecretRoll ----

    @Test
    fun isSecretRoll_zero_isSecret() {
        assertTrue(isSecretRoll(0f))
    }

    @Test
    fun isSecretRoll_justBelowThreshold_isSecret() {
        assertTrue(isSecretRoll(0.0299f))
    }

    @Test
    fun isSecretRoll_atThreshold_isNotSecret() {
        assertFalse(isSecretRoll(0.03f))
    }

    @Test
    fun isSecretRoll_high_isNotSecret() {
        assertFalse(isSecretRoll(0.9999f))
    }

    // ---- message pools ----

    @Test
    fun secretMessages_matchFixedSpec() {
        assertEquals(
            listOf(
                "뚜뚜뚜두 막스 베르스타펜",
                "챗지피티야 고마워",
                "비개발자가 만들었어요"
            ),
            INTRO_SECRET_MESSAGES
        )
    }

    @Test
    fun generalMessages_poolSizeIsInRecommendedRange() {
        assertTrue(INTRO_GENERAL_MESSAGES.size in 8..15)
    }

    @Test
    fun generalMessages_noBlankOrDuplicateEntries() {
        assertTrue(INTRO_GENERAL_MESSAGES.all { it.isNotBlank() })
        assertEquals(INTRO_GENERAL_MESSAGES.size, INTRO_GENERAL_MESSAGES.toSet().size)
    }

    @Test
    fun generalAndSecretPools_haveNoOverlap() {
        assertTrue(INTRO_GENERAL_MESSAGES.none { it in INTRO_SECRET_MESSAGES })
    }

    // ---- selectIntroMessage distribution (fixed seed, deterministic) ----

    @Test
    fun selectIntroMessage_withFixedSeed_secretShareIsRareButPresent() {
        val random = Random(12345)
        val trials = 20_000
        val secretCount = (0 until trials).count { selectIntroMessage(random) in INTRO_SECRET_MESSAGES }

        // 3% of 20,000 = 600; generous band so this stays non-flaky.
        assertTrue("secretCount=$secretCount", secretCount in 400..900)
    }

    @Test
    fun selectIntroMessage_withFixedSeed_allThreeSecretsEventuallyAppear() {
        val random = Random(777)
        val seen = mutableSetOf<String>()
        repeat(50_000) {
            val message = selectIntroMessage(random)
            if (message in INTRO_SECRET_MESSAGES) seen += message
        }

        assertEquals(INTRO_SECRET_MESSAGES.toSet(), seen)
    }

    // ---- 33번째 방문 milestone ----

    @Test
    fun selectIntroMessage_at33rdVisit_alwaysReturnsMaxVerstappenMessage() {
        val random = Random(1)
        repeat(1_000) {
            assertEquals(
                "뚜뚜뚜두 막스 베르스타펜",
                selectIntroMessage(random, totalVisitDays = INTRO_MAX_MILESTONE_VISIT_DAY)
            )
        }
    }

    @Test
    fun selectIntroMessage_32ndVisit_milestoneDoesNotApply() {
        // 32번째는 milestone 분기를 타지 않고 totalVisitDays 없을 때와 완전히
        // 같은 난수 소비·결과 분포를 가져야 한다(같은 시드로 결과 동일성 확인).
        val withoutContext = Random(555)
        val at32 = Random(555)

        repeat(500) {
            assertEquals(
                selectIntroMessage(withoutContext),
                selectIntroMessage(at32, totalVisitDays = 32)
            )
        }
    }

    @Test
    fun selectIntroMessage_34thVisit_milestoneDoesNotApply() {
        val withoutContext = Random(999)
        val at34 = Random(999)

        repeat(500) {
            assertEquals(
                selectIntroMessage(withoutContext),
                selectIntroMessage(at34, totalVisitDays = 34)
            )
        }
    }

    // ---- 선수 등번호 milestone (3/7/16/44/63번째 방문) ----

    @Test
    fun milestoneMessages_matchFixedSpec() {
        assertEquals(
            mapOf(
                3 to "피에르으으으으으으으 가슬리이이이이이이이이이이",
                7 to "럭키데이",
                16 to "그것은 물이다",
                33 to "뚜뚜뚜두 막스 베르스타펜",
                44 to "Hey, man",
                63 to "Here comes the DIVA"
            ),
            INTRO_MILESTONE_MESSAGES
        )
    }

    @Test
    fun selectIntroMessage_atEachDriverNumberMilestone_alwaysReturnsMappedMessage() {
        INTRO_MILESTONE_MESSAGES.forEach { (visitDay, expectedMessage) ->
            val random = Random(visitDay)
            repeat(200) {
                assertEquals(
                    "visitDay=$visitDay",
                    expectedMessage,
                    selectIntroMessage(random, totalVisitDays = visitDay)
                )
            }
        }
    }

    @Test
    fun selectIntroMessage_daysAdjacentToDriverNumberMilestones_doNotForceMessage() {
        // milestone 키 바로 옆 방문일들은 milestone 분기를 타지 않고
        // totalVisitDays 없을 때와 완전히 같은 결과·난수 소비를 가져야 한다.
        val adjacentDays = INTRO_MILESTONE_MESSAGES.keys
            .flatMap { listOf(it - 1, it + 1) }
            .filter { it !in INTRO_MILESTONE_MESSAGES }

        adjacentDays.forEach { day ->
            val withoutContext = Random(day)
            val atDay = Random(day)

            repeat(100) {
                assertEquals(
                    "day=$day",
                    selectIntroMessage(withoutContext),
                    selectIntroMessage(atDay, totalVisitDays = day)
                )
            }
        }
    }
}
