package com.postcardmemory.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 활성 보기 목록이 바뀐 뒤 pager가 이동해야 할 새 페이지 index를 계산하는
 * [resolveGalleryPagerTargetIndex]를 검증한다(작업지시서 44절 — 보기
 * 비활성화 edge case). GalleryScreen 자체는 Context 의존 Compose 트리라
 * 이 프로젝트의 순수 JUnit 환경에서 직접 렌더링할 수 없으므로, 이 순수
 * 함수만 분리해 검증한다(GallerySearchFilterTest.kt와 동일한 방식).
 */
class GalleryPagerTargetIndexTest {

    @Test
    fun requestedActivationCombinations_keepExpectedPageCountAndOrder() {
        val three = GalleryPageFormat.THREE_COLUMN
        val monthly = GalleryPageFormat.MONTHLY
        val stamp = GalleryPageFormat.STAMP

        assertEquals(listOf(three), orderedGalleryPageFormats(setOf(three)))
        assertEquals(
            listOf(three, monthly),
            orderedGalleryPageFormats(setOf(monthly, three))
        )
        assertEquals(
            listOf(three, monthly, stamp),
            orderedGalleryPageFormats(setOf(stamp, three, monthly))
        )
        assertEquals(
            GalleryPageFormat.entries,
            orderedGalleryPageFormats(GalleryPageFormat.entries.toSet())
        )
    }

    @Test
    fun stillActive_currentFormatIsMiddlePage_keepsShowingIt_evenAfterEarlierPageRemoved() {
        // 3단/월별/타임라인/우표 4페이지 중 타임라인(2번 index)을 보던 중
        // 월별(1번 index, 타임라인보다 앞)이 꺼지면 남은 목록은
        // 3단/타임라인/우표가 되고 타임라인은 1번 index로 당겨진다.
        // 단순 index clamp(2 -> min(2, 2)=2)로는 우표가 걸려 틀리므로,
        // 반드시 보기 자체를 기준으로 찾아야 한다.
        val remainingFormats = listOf(
            GalleryPageFormat.THREE_COLUMN,
            GalleryPageFormat.TIMELINE,
            GalleryPageFormat.STAMP
        )

        val target = resolveGalleryPagerTargetIndex(
            activeFormats = remainingFormats,
            lastKnownFormat = GalleryPageFormat.TIMELINE,
            currentIndex = 2
        )

        assertEquals(1, target)
    }

    @Test
    fun currentFormatTurnedOff_clampsToNearestValidIndex() {
        // 우표(2번 index)를 보던 중 우표 자체를 껐다면 남은 목록의 마지막
        // 유효 index로 이동한다.
        val remainingFormats = listOf(
            GalleryPageFormat.THREE_COLUMN,
            GalleryPageFormat.MONTHLY
        )

        val target = resolveGalleryPagerTargetIndex(
            activeFormats = remainingFormats,
            lastKnownFormat = GalleryPageFormat.STAMP,
            currentIndex = 2
        )

        assertEquals(1, target)
    }

    @Test
    fun formatAddedAfterCurrentPage_indexUnaffected() {
        // 3단(0)을 보던 중 월별을 새로 켜서 목록이 3단/월별이 돼도 계속
        // 3단(0)에 머문다.
        val newFormats = listOf(
            GalleryPageFormat.THREE_COLUMN,
            GalleryPageFormat.MONTHLY
        )

        val target = resolveGalleryPagerTargetIndex(
            activeFormats = newFormats,
            lastKnownFormat = GalleryPageFormat.THREE_COLUMN,
            currentIndex = 0
        )

        assertEquals(0, target)
    }

    @Test
    fun emptyActiveFormats_fallsBackToZero_defensivePathOnly() {
        // 45절에 따라 실제로는 발생하지 않아야 하는 방어적 경로.
        val target = resolveGalleryPagerTargetIndex(
            activeFormats = emptyList(),
            lastKnownFormat = GalleryPageFormat.THREE_COLUMN,
            currentIndex = 0
        )

        assertEquals(0, target)
    }

    @Test
    fun onlyThreeColumnActive_staysAtZero() {
        val target = resolveGalleryPagerTargetIndex(
            activeFormats = listOf(GalleryPageFormat.THREE_COLUMN),
            lastKnownFormat = GalleryPageFormat.THREE_COLUMN,
            currentIndex = 0
        )

        assertEquals(0, target)
    }
}
