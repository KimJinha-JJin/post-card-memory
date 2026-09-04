package com.postcardmemory.ui.gallery

/**
 * 메인 갤러리에서 좌우로 넘겨볼 수 있는 보기 형식. 선언 순서가 기본 표시
 * 순서다(작업지시서 27절) — 활성 형식 목록은 항상 이 선언 순서로 걸러
 * 쓰므로 별도 정렬 로직이 필요 없다.
 *
 * [THREE_COLUMN]은 기존 3단 갤러리를 그대로 편입한 기본 보기이며, 최소
 * 하나의 보기를 보장하는 안전 보기로서 항상 활성 상태다(45절) — 사용자가
 * 끌 수 없다.
 *
 * [sortAffectsOrder]는 정렬 방식(최신순/오래된순)이 해당 보기의 실제
 * 표시 순서에 반영되는지를 나타낸다. 월별/타임라인/우표는 그룹핑 헬퍼가
 * 입력 순서를 그대로 보존해 정렬값을 물려받지만, 캘린더는 날짜 grid라
 * 순서 자체가 무의미하고 기억 밀도는 항상 과거→현재로 고정돼 정렬과
 * 무관하다(63일차 우측 상단 전수조사).
 */
enum class GalleryPageFormat(
    val label: String,
    val sortAffectsOrder: Boolean = false
) {
    THREE_COLUMN("3단 보기", sortAffectsOrder = true),
    MONTHLY("월별 보기", sortAffectsOrder = true),
    TIMELINE("타임라인 보기", sortAffectsOrder = true),
    CALENDAR("캘린더 보기"),
    STAMP("우표 보기", sortAffectsOrder = true),
    DENSITY("기억 밀도 보기")
}
