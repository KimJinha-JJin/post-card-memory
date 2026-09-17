package com.postcardmemory.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 활성 보기 목록이 바뀐 뒤 pager가 이동해야 할 새 페이지 index를 계산하는
 * [resolveGalleryPagerTargetIndex]를 검증한다. GalleryScreen 자체는 Context
 * 의존 Compose 트리라 이 프로젝트의 순수 JUnit 환경에서 직접 렌더링할 수
 * 없으므로, 이 순수 함수만 분리해 검증한다(GallerySearchFilterTest.kt와
 * 동일한 방식).
 *
 * 76일차: 3단/캘린더/우표/타임라인 보기가 삭제되며 [GalleryPageFormat]에는
 * 월별·기억 밀도 2개만 남았고, 두 보기는 항상 함께 활성화된 채 pager로
 * 스와이프된다(설정에서 껐다 켜는 UI 자체가 없음). 아래 함수들은 여전히
 * 일반적인 순수 함수라 방어적 경로까지 그대로 검증한다.
 */
class GalleryPagerTargetIndexTest {

    @Test
    fun orderedFormats_alwaysPutsMonthlyBeforeDensity() {
        val monthly = GalleryPageFormat.MONTHLY
        val density = GalleryPageFormat.DENSITY

        assertEquals(listOf(monthly), orderedGalleryPageFormats(setOf(monthly)))
        assertEquals(
            listOf(monthly, density),
            orderedGalleryPageFormats(setOf(density, monthly))
        )
        assertEquals(
            GalleryPageFormat.entries,
            orderedGalleryPageFormats(GalleryPageFormat.entries.toSet())
        )
    }

    @Test
    fun currentFormatStillActive_keepsShowingIt() {
        val target = resolveGalleryPagerTargetIndex(
            activeFormats = listOf(GalleryPageFormat.MONTHLY, GalleryPageFormat.DENSITY),
            lastKnownFormat = GalleryPageFormat.DENSITY,
            currentIndex = 1
        )

        assertEquals(1, target)
    }

    @Test
    fun lastKnownFormatNoLongerActive_clampsToNearestValidIndex() {
        // 실제 production에서는 두 보기가 항상 함께 활성화되어 발생하지
        // 않지만, 함수 자체의 방어적 경로는 계속 유효해야 한다.
        val target = resolveGalleryPagerTargetIndex(
            activeFormats = listOf(GalleryPageFormat.MONTHLY),
            lastKnownFormat = GalleryPageFormat.DENSITY,
            currentIndex = 1
        )

        assertEquals(0, target)
    }

    @Test
    fun emptyActiveFormats_fallsBackToZero_defensivePathOnly() {
        val target = resolveGalleryPagerTargetIndex(
            activeFormats = emptyList(),
            lastKnownFormat = GalleryPageFormat.MONTHLY,
            currentIndex = 0
        )

        assertEquals(0, target)
    }

    @Test
    fun onlyMonthlyActive_staysAtZero() {
        val target = resolveGalleryPagerTargetIndex(
            activeFormats = listOf(GalleryPageFormat.MONTHLY),
            lastKnownFormat = GalleryPageFormat.MONTHLY,
            currentIndex = 0
        )

        assertEquals(0, target)
    }
}
