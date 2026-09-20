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
        zone: ZoneId = ZoneId.systemDefault(),
        readRecordText: (File) -> String = { it.readText(Charsets.UTF_8) }
    ): TodayVisit {
        val stored = readStoredRecord(filesDir, readRecordText)
        if (stored is StoredVisitRecord.Unreadable) {
            return TodayVisit(record = null, isFirstVisitToday = false)
        }

        val previous = (stored as? StoredVisitRecord.Present)?.record
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

    /**
     * 저장된 방문 기록을 읽은 결과. "기록이 없다"와 "읽지 못했다"를 구분하기
     * 위해서만 존재한다 — 둘 다 null로 뭉뚱그리면 일시적인 읽기 실패가
     * 새 사용자와 똑같이 취급돼 기존 누적 방문일이 1로 덮어써진다
     * ([com.postcardmemory.utils.PostcardDraftStorage.loadDraft]에서 초안에
     * 적용한 것과 같은 구분).
     */
    internal sealed interface StoredVisitRecord {
        /** 아직 방문 기록 파일이 없다. 오늘이 진짜 첫 방문이다. */
        object Absent : StoredVisitRecord

        /** 파일은 읽었다. [record]가 null이면 내용이 손상돼 해석할 수 없었다는 뜻. */
        data class Present(val record: VisitRecord?) : StoredVisitRecord

        /** 파일은 있는데 읽지 못했다(파일 잠금, 저장소 일시 오류 등). */
        object Unreadable : StoredVisitRecord
    }

    /**
     * 세 경우를 구분한다.
     *
     * - 파일 없음 → [StoredVisitRecord.Absent]. 첫 실행이다.
     * - 읽기 실패 → [StoredVisitRecord.Unreadable]. 내용을 한 글자도 보지
     *   못했으므로 손상 여부조차 판정할 수 없다. 기존 파일을 그대로 두고
     *   이번 실행만 기록을 건너뛴다. 다음 실행에서 정상적으로 읽히면
     *   원래 숫자가 그대로 돌아온다.
     * - 읽었지만 형식이 깨짐 → [StoredVisitRecord.Present]`(null)`. 기존
     *   정책 그대로 오늘이 첫 방문으로 다시 시작한다.
     *
     * [readRecordText]는 순수 JUnit에서 일시적 읽기 실패를 실제로 주입하기
     * 위한 이음매이며, production은 항상 기본값을 쓴다.
     */
    internal fun readStoredRecord(
        filesDir: File,
        readRecordText: (File) -> String = { it.readText(Charsets.UTF_8) }
    ): StoredVisitRecord {
        val file = visitRecordFile(filesDir)
        if (!file.exists()) return StoredVisitRecord.Absent

        val text = runCatching { readRecordText(file) }.getOrNull()
            ?: return StoredVisitRecord.Unreadable

        return StoredVisitRecord.Present(parseVisitRecord(text))
    }

    /** 읽을 수 없거나 형식이 깨졌으면 null. 읽기 실패와 손상을 구분해야 하면 [readStoredRecord]를 쓴다. */
    internal fun load(filesDir: File): VisitRecord? =
        (readStoredRecord(filesDir) as? StoredVisitRecord.Present)?.record

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
