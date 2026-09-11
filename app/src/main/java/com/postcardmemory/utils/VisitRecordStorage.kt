package com.postcardmemory.utils

import android.content.Context
import java.io.File
import java.time.ZoneId

/**
 * 방문 기록 전용 독립 저장소. 엽서 데이터(Room)와 완전히 분리된
 * `filesDir/visits/visit_record.txt` 한 파일만 쓴다 — 이 앱에는
 * SharedPreferences나 DataStore 선례가 아예 없고, 작은 앱 전역 상태는
 * [PostcardDraftStorage]·[PostcardTemplateStorage]처럼 filesDir 하위 전용
 * 폴더에 원자적으로 쓰는 것이 기존 방식이다. 방문 기록 하나 때문에 Room
 * 스키마나 Migration을 건드리지 않는다.
 *
 * 이 파일이 손상되거나 사라져도 엽서·스티커·도장·초안에는 아무 영향이 없다
 * (반대 방향도 마찬가지 — 엽서 삭제 경로는 엽서 id별 디렉터리만 지운다).
 *
 * 공개 API는 Context를 받고 실제 파일 로직은 filesDir를 직접 받는 internal
 * 오버로드에 위임한다 — Context 없이 순수 JUnit에서 검증할 수 있다.
 */
object VisitRecordStorage {

    private const val VISIT_DIR_NAME = "visits"
    private const val VISIT_FILE_NAME = "visit_record.txt"

    internal fun visitRecordFile(filesDir: File): File =
        File(File(filesDir, VISIT_DIR_NAME), VISIT_FILE_NAME)

    /**
     * 오늘 방문을 기록하고 그 결과를 돌려준다. 파일을 읽고 쓰므로 **반드시
     * IO 디스패처에서** 호출한다(호출부: [com.postcardmemory.MainActivity]).
     */
    fun recordTodayVisit(
        context: Context,
        nowMillis: Long = System.currentTimeMillis()
    ): TodayVisit = recordTodayVisit(context.filesDir, nowMillis)

    internal fun recordTodayVisit(
        filesDir: File,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): TodayVisit {
        val previous = load(filesDir)
        val updated = recordVisit(previous, visitEpochDay(nowMillis, zone))

        // 같은 날 다시 열었으면 recordVisit이 previous를 그대로 돌려준다.
        // 그때는 파일도 다시 쓰지 않는다 — 하루에 여러 번 실행해도 디스크
        // 쓰기는 0회이고, 이 판정이 곧 "오늘의 첫 방문인가"다.
        val isFirstVisitToday = updated != previous

        if (isFirstVisitToday) {
            save(filesDir, updated)
        }

        return TodayVisit(
            record = updated,
            isFirstVisitToday = isFirstVisitToday
        )
    }

    /** 읽을 수 없거나 형식이 깨졌으면 null. 이 경우 오늘이 첫 방문으로 다시 시작된다. */
    internal fun load(filesDir: File): VisitRecord? {
        val text =
            runCatching {
                visitRecordFile(filesDir).readText(Charsets.UTF_8)
            }.getOrNull() ?: return null

        return parseVisitRecord(text)
    }

    /**
     * 임시 파일에 먼저 쓰고 rename하는 검증된 원자적 저장
     * ([ConfirmedEditStateStorage])을 그대로 재사용한다 — 쓰다가 실패해도
     * 기존 방문 기록이 손상되지 않고 그대로 남는다.
     *
     * 저장에 실패해도 이번 실행의 소인은 그대로 보여준다(화면에서 실패를
     * 알리지 않는다). 다음 실행에서 같은 날짜를 다시 기록하게 될 뿐이다.
     */
    internal fun save(filesDir: File, record: VisitRecord): Boolean =
        ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = visitRecordFile(filesDir),
            content = record.serialize()
        )
}
