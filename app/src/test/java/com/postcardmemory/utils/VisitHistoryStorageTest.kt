package com.postcardmemory.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VisitHistoryStorageTest {
    @get:Rule val temp = TemporaryFolder()
    private val today = LocalDate.of(2026, 9, 14)
    private val month = YearMonth.from(today)

    @Test fun recordsOnlyObservedDatesAndFiltersMonth() {
        assertTrue(VisitHistoryStorage.loadMonth(temp.root, month).isEmpty())
        listOf(today, today.plusDays(3), today.minusMonths(1), today.plusMonths(1)).forEach {
            assertTrue(VisitHistoryStorage.recordDate(temp.root, it))
        }
        assertEquals(setOf(today.toEpochDay(), today.plusDays(3).toEpochDay()), VisitHistoryStorage.loadMonth(temp.root, month))
        assertEquals(0L, VisitHistoryStorage.marker(temp.root, today).length())
    }

    @Test fun sameDayNeverRewritesExistingMarkerOrLegacyRecord() {
        assertTrue(VisitHistoryStorage.recordDate(temp.root, today))
        val marker = VisitHistoryStorage.marker(temp.root, today)
        marker.writeText("existing bytes must survive")
        val timestamp = FileTime.fromMillis(1_600_000_000_000)
        Files.setLastModifiedTime(marker.toPath(), timestamp)
        val legacy = VisitRecordStorage.visitRecordFile(temp.root)
        legacy.writeText("1\t20600\t44\t9")
        Files.setLastModifiedTime(legacy.toPath(), timestamp)

        repeat(3) { assertTrue(VisitHistoryStorage.recordDate(temp.root, today)) }

        assertEquals("existing bytes must survive", marker.readText())
        assertEquals(timestamp, Files.getLastModifiedTime(marker.toPath()))
        assertEquals("1\t20600\t44\t9", legacy.readText())
        assertEquals(timestamp, Files.getLastModifiedTime(legacy.toPath()))
    }

    @Test fun upgradeOnAlreadyCountedDayDoesNotBackfillOrChangeSummary() {
        val record = VisitRecord(today.toEpochDay(), 44, 9)
        assertTrue(VisitRecordStorage.save(temp.root, record))
        val before = VisitRecordStorage.visitRecordFile(temp.root).readBytes()
        val instant = today.atTime(12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
        val result = VisitRecordStorage.recordTodayVisit(temp.root, instant, ZoneId.of("Asia/Seoul"))
        assertTrue(VisitHistoryStorage.recordDate(temp.root, today))
        assertFalse(result.isFirstVisitToday)
        assertEquals(record, result.record)
        assertArrayEquals(before, VisitRecordStorage.visitRecordFile(temp.root).readBytes())
        assertEquals(setOf(today.toEpochDay()), VisitHistoryStorage.loadMonth(temp.root, month))
    }

    @Test fun blockedHistoryDoesNotPreventNormalSummaryAndCanRetrySameDay() {
        val yesterday = VisitRecord(today.minusDays(1).toEpochDay(), 10, 2)
        assertTrue(VisitRecordStorage.save(temp.root, yesterday))
        val blocked = VisitHistoryStorage.directory(temp.root)
        blocked.writeText("obstruction")
        val instant = today.atTime(12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
        val result = VisitRecordStorage.recordTodayVisit(temp.root, instant, ZoneId.of("Asia/Seoul"))
        assertFalse(VisitHistoryStorage.recordDate(temp.root, today))
        assertEquals(11, result.record?.totalVisitDays)
        assertEquals(3, result.record?.currentStreakDays)
        assertEquals(result.record, VisitRecordStorage.load(temp.root))
        assertEquals("obstruction", blocked.readText())
        assertTrue(VisitHistoryStorage.loadMonth(temp.root, month).isEmpty())

        // JUnit 소유 임시 fixture만 제거. production에는 삭제 경로가 없다.
        assertTrue(blocked.delete())
        assertTrue(VisitHistoryStorage.recordDate(temp.root, today))
        assertEquals(result.record, VisitRecordStorage.load(temp.root))
    }

    @Test fun failedSummaryDoesNotPreventIndependentHistory() {
        val legacy = VisitRecordStorage.visitRecordFile(temp.root)
        assertTrue(legacy.mkdirs())
        File(legacy, "keep").writeText("fixture")
        assertFalse(VisitRecordStorage.save(temp.root, VisitRecord(today.toEpochDay(), 1, 1)))
        assertTrue(VisitHistoryStorage.recordDate(temp.root, today))
        assertEquals("fixture", File(legacy, "keep").readText())
        assertEquals(setOf(today.toEpochDay()), VisitHistoryStorage.loadMonth(temp.root, month))
    }

    @Test fun concurrentSameDateCreatesOnlyOneMarker() {
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val results = (1..8).map {
                pool.submit(Callable {
                    check(start.await(5, TimeUnit.SECONDS))
                    VisitHistoryStorage.recordDate(temp.root, today)
                })
            }
            start.countDown()
            results.forEach { assertTrue(it.get(10, TimeUnit.SECONDS)) }
            assertEquals(listOf("${today.toEpochDay()}.visit"), VisitHistoryStorage.directory(temp.root).list()!!.toList())
            assertEquals(0L, VisitHistoryStorage.marker(temp.root, today).length())
        } finally {
            pool.shutdownNow()
        }
    }

    @Test fun readIgnoresForeignFilesAndDirectoriesWithoutDeletingThem() {
        val dir = VisitHistoryStorage.directory(temp.root)
        assertTrue(dir.mkdirs())
        File(dir, "garbage.visit").writeText("keep")
        File(dir, "${today.toEpochDay()}.visit.tmp").writeText("keep")
        assertTrue(VisitHistoryStorage.marker(temp.root, today).mkdir())
        assertFalse(VisitHistoryStorage.recordDate(temp.root, today))
        assertTrue(VisitHistoryStorage.loadMonth(temp.root, month).isEmpty())
        assertEquals(3, dir.list()!!.size)
    }

    @Test fun failedLaterDatePreservesAllEarlierMarkers() {
        assertTrue(VisitHistoryStorage.recordDate(temp.root, today))
        val tomorrow = today.plusDays(1)
        assertTrue(VisitHistoryStorage.marker(temp.root, tomorrow).mkdir())
        assertFalse(VisitHistoryStorage.recordDate(temp.root, tomorrow))
        assertEquals(setOf(today.toEpochDay()), VisitHistoryStorage.loadMonth(temp.root, month))
        assertTrue(VisitHistoryStorage.marker(temp.root, today).isFile)
    }
}
