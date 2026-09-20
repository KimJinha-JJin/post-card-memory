package com.postcardmemory.utils

import java.io.File
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.time.LocalDate
import java.time.YearMonth

/**
 * 방문 달력이 "어느 날에 들렀는지"를 칠하기 위한 날짜별 표식 저장소.
 * [VisitRecord]의 canonical 정의(그 날짜에 앱을 새로 연 기록)를 그대로
 * 따르지만, **[VisitRecord.totalVisitDays]와 세는 대상이 다르다** — 저쪽은
 * 이 저장소가 생기기 전부터 누적된 총 방문일이고, 여기에는 이 저장소가
 * 적용된 뒤 실제로 관측한 날짜만 있다. 그래서 달력에 칠해진 칸 수가
 * "오늘까지 N번 만났어요"의 N보다 적을 수 있고, 그건 버그가 아니다.
 *
 * 적용 이후 실제로 관측한 날짜만 추가한다. 기존 visit_record.txt와 독립적이다.
 * 빈 파일의 존재 자체가 기록이므로 내용 쓰기, 임시 파일, 교체, 삭제가 없다.
 * CREATE_NEW의 배타적 생성으로 같은 날짜의 경합에서도 기존 파일을 열지 않는다.
 */
object VisitHistoryStorage {
    internal fun directory(filesDir: File): File = File(filesDir, "visits/history")

    internal fun marker(filesDir: File, date: LocalDate): File =
        File(directory(filesDir), "${date.toEpochDay()}.visit")

    /** false여도 기존 방문 집계 결과는 그대로 사용한다. IO dispatcher에서 호출한다. */
    fun recordDate(filesDir: File, date: LocalDate): Boolean = try {
        val dir = directory(filesDir).toPath()
        Files.createDirectories(dir)
        if (!Files.isDirectory(dir, NOFOLLOW_LINKS)) {
            false
        } else {
            val path = marker(filesDir, date).toPath()
            try {
                Files.createFile(path)
                true
            } catch (_: FileAlreadyExistsException) {
                Files.isRegularFile(path, NOFOLLOW_LINKS)
            }
        }
    } catch (_: IOException) {
        false
    } catch (_: SecurityException) {
        false
    }

    /** 해당 월의 정규 marker만 확인한다. lastVisit/streak에서 날짜를 만들지 않는다. */
    fun loadMonth(filesDir: File, month: YearMonth): Set<Long> = try {
        if (!Files.isDirectory(directory(filesDir).toPath(), NOFOLLOW_LINKS)) {
            emptySet()
        } else {
            (1..month.lengthOfMonth()).mapNotNull { day ->
                val date = month.atDay(day)
                date.toEpochDay().takeIf {
                    Files.isRegularFile(marker(filesDir, date).toPath(), NOFOLLOW_LINKS)
                }
            }.toSet()
        }
    } catch (_: SecurityException) {
        emptySet()
    }
}
