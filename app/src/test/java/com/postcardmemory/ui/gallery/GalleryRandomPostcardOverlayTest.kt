package com.postcardmemory.ui.gallery

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 84일차 후속 "흔들어서 한 장" overlay의 순수 로직 검증 — 날짜 문구, overlay
 * 중 재흔들기 처리, 화면 배치. Compose 애니메이션 자체는 실기기 QA로 본다.
 */
class GalleryRandomPostcardOverlayTest {

    private val seoul = ZoneId.of("Asia/Seoul")

    @Test
    fun storyLabel_usesCapturedAtMonthAndDayWithoutZeroPadding() {
        val capturedAt = ZonedDateTime.of(2026, 9, 5, 21, 0, 0, 0, seoul)
            .toInstant()
            .toEpochMilli()

        assertEquals("9월 5일의 이야기예요~!", randomPostcardStoryLabel(capturedAt, seoul))
    }

    @Test
    fun storyLabel_followsGivenZoneAcrossMidnight() {
        // 서울 9월 25일 00:30 = UTC 9월 24일 15:30 — 표시 zone의 날짜를 따른다.
        val capturedAt = ZonedDateTime.of(2026, 9, 25, 0, 30, 0, 0, seoul)
            .toInstant()
            .toEpochMilli()

        assertEquals("9월 25일의 이야기예요~!", randomPostcardStoryLabel(capturedAt, seoul))
        assertEquals(
            "9월 24일의 이야기예요~!",
            randomPostcardStoryLabel(capturedAt, ZoneId.of("UTC"))
        )
    }

    @Test
    fun shakeWhileOverlayOpen_keepsCurrentPostcard() {
        val candidates = listOf(1L, 2L, 3L, 4L)

        repeat(20) { seed ->
            assertEquals(3L, nextShakeOverlayPostcardId(3L, candidates, Random(seed)))
        }
    }

    @Test
    fun shakeWithNoCandidates_opensNothing() {
        assertNull(nextShakeOverlayPostcardId(null, emptyList()))
    }

    @Test
    fun shakeWithOverlayClosed_picksFromCandidates() {
        val candidates = listOf(10L, 20L, 30L)
        val random = Random(84)

        repeat(50) {
            val picked = nextShakeOverlayPostcardId(null, candidates, random)
            assertTrue("picked=$picked", picked in candidates)
        }
    }

    @Test
    fun layout_keepsCardInsideScreen_andLeavesRoomForLabel() {
        val density = 2.75f
        // (가로 dp, 세로 dp): 작은 폰, 보통 폰, 큰 폰, 가로로 넓은 화면.
        val screens = listOf(320f to 520f, 392f to 760f, 430f to 880f, 800f to 400f)

        screens.forEach { (wDp, hDp) ->
            val w = wDp * density
            val h = hDp * density
            val layout = randomOverlayLayout(w, h, density)

            assertTrue("left $wDp×$hDp", layout.cardLeftPx >= 0f)
            assertTrue("right $wDp×$hDp", layout.cardLeftPx + layout.cardSizePx <= w)
            assertTrue(
                "label room $wDp×$hDp",
                layout.cardTopPx - layout.labelGapPx >= 0f
            )
            assertTrue("bottom $wDp×$hDp", layout.cardBottomPx <= h)
            // 숨은 위치에서는 엽서 윗변까지 화면 아래로 내려가 있다.
            assertTrue("hidden $wDp×$hDp", layout.cardTopPx + layout.hiddenTravelPx >= h)
        }
    }

    @Test
    fun layout_handReachesScreenBottom_onTypicalPhone_andStaysBelowCap() {
        val density = 2.75f
        val w = 392f * density
        val h = 760f * density
        val layout = randomOverlayLayout(w, h, density)

        // 잘린 손목이 공중에 뜨지 않도록 손 그림 아랫변이 화면 아래에 닿는다.
        val handBottom =
            layout.cardBottomPx + (1f - RANDOM_OVERLAY_HAND_ANCHOR_Y) * layout.handSidePx
        assertTrue("handBottom=$handBottom h=$h", handBottom >= h)

        // 손은 엽서 왼쪽 아래 귀퉁이를 집는다(가운데가 아니라 왼쪽 1/5 안).
        assertTrue(layout.handAnchorXPx >= layout.cardLeftPx)
        assertTrue(layout.handAnchorXPx <= layout.cardLeftPx + layout.cardSizePx * 0.2f)

        // 손은 엽서의 1.6배를 넘지 않는다.
        assertTrue(layout.handSidePx <= layout.cardSizePx * 1.6f + 0.01f)
    }
}
