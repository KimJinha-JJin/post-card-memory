package com.postcardmemory.ui.gallery

/** 〈엽서 양떼목장 — 쫑쫑컵〉 레이스 이벤트의 진행 단계. */
enum class SheepRanchRacePhase {
    IDLE,
    GATHERING,
    COUNTDOWN,
    RACING,
    FINISHING,
    RETURNING
}

/** 레이스가 시작되는 순간의 카드 위치·회전 스냅샷 — 레이스 종료 후 이 자리로 되돌아간다. */
data class SheepRanchCardSnapshot(
    val postcardId: Long,
    val x: Float,
    val y: Float,
    val rotationDegrees: Float
)

/**
 * 화면 한정(레이스 결과·우승 횟수는 저장하지 않음) 레이스 세션 상태.
 * `sessionId`는 레이스가 새로 시작될 때마다 증가해 애니메이션 재시작 키로 쓰인다.
 * `winnerId`는 레이스 시작 시 한 번만 무작위로 뽑혀 진행 중에는 값이 바뀌지 않는다.
 */
data class SheepRanchRaceState(
    val phase: SheepRanchRacePhase = SheepRanchRacePhase.IDLE,
    val racerIds: List<Long> = emptyList(),
    val spectatorIds: List<Long> = emptyList(),
    val winnerId: Long? = null,
    val sessionId: Int = 0,
    val snapshots: Map<Long, SheepRanchCardSnapshot> = emptyMap()
)
