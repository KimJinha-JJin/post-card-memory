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
}
