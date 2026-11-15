package com.postcardmemory.utils

import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 133일차 필름 인트로가 지난 칸 소인을 정할 때 쓰는 읽기 전용 조회. 달력과 같은 방문 표식만 근거로
 * 하며, 표식을 만들거나 폴더를 만들지 않고, 없음(빈 집합)과 판별 불가(null)를 구분한다.
 */
class VisitHistoryLoadDaysTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun filesDir(): File = tempFolder.root

    private val today = LocalDate.of(2026, 11, 15)

    @Test
    fun returnsOnlyDaysWithAMarker() {
        VisitHistoryStorage.recordDate(filesDir(), today.minusDays(1))
        VisitHistoryStorage.recordDate(filesDir(), today.minusDays(3))

        val asked = (1L..5L).map { today.minusDays(it).toEpochDay() }
        assertEquals(
            setOf(today.minusDays(1).toEpochDay(), today.minusDays(3).toEpochDay()),
            VisitHistoryStorage.loadDays(filesDir(), asked)
        )
    }

    @Test
    fun noHistoryFolderYet_isEmptyNotUnknown_andCreatesNothing() {
        val result = VisitHistoryStorage.loadDays(filesDir(), listOf(today.toEpochDay() - 1))

        assertEquals(emptySet<Long>(), result)
        assertFalse("읽기만 하고 폴더를 만들지 않음", VisitHistoryStorage.directory(filesDir()).exists())
    }

    @Test
    fun historyPathIsNotADirectory_isUnknown() {
        val path = VisitHistoryStorage.directory(filesDir())
        path.parentFile!!.mkdirs()
        path.writeText("not a directory")

        assertNull(VisitHistoryStorage.loadDays(filesDir(), listOf(today.toEpochDay() - 1)))
    }

    @Test
    fun readingNeverWritesOrChangesMarkers() {
        VisitHistoryStorage.recordDate(filesDir(), today.minusDays(2))
        val before = VisitHistoryStorage.directory(filesDir()).listFiles()!!.map { it.name to it.length() }.toSet()

        VisitHistoryStorage.loadDays(filesDir(), (0L..70L).map { today.toEpochDay() - it })

        val after = VisitHistoryStorage.directory(filesDir()).listFiles()!!.map { it.name to it.length() }.toSet()
        assertEquals(before, after)
        assertTrue(after.size == 1)
    }

    @Test
    fun aMarkerNamedDirectoryIsNotCountedAsAVisit() {
        val day = today.minusDays(1)
        VisitHistoryStorage.marker(filesDir(), day).mkdirs()

        assertEquals(emptySet<Long>(), VisitHistoryStorage.loadDays(filesDir(), listOf(day.toEpochDay())))
    }
}
