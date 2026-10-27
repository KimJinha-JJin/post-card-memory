package com.postcardmemory.ui.gallery

/**
 * 메인 갤러리에서 좌우로 넘겨볼 수 있는 보기 형식. 선언 순서가 기본 표시
 * 순서다 — 항상 [MONTHLY]가 먼저, [DENSITY]가 나중이다(76일차 보기 체계
 * 축소).
 *
 * [MONTHLY]는 기억을 찾고 관리하는 기본 갤러리이며, 최소 하나의 보기를
 * 보장하는 안전 보기로서 항상 활성 상태다 — 사용자가 끌 수 없다.
 *
 * [sortAffectsOrder]는 정렬 방식(최신순/오래된순)이 해당 보기의 실제
 * 표시 순서에 반영되는지를 나타낸다. 월별은 그룹핑 헬퍼가 입력 순서를
 * 그대로 보존해 정렬값을 물려받지만, 기억 밀도는 항상 1월→12월로 고정돼
 * 정렬과 무관하다.
 */
enum class GalleryPageFormat(
    val sortAffectsOrder: Boolean = false
) {
    MONTHLY(sortAffectsOrder = true),
    DENSITY
}
