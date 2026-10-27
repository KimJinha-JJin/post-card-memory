package com.postcardmemory.utils

import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 지금부터 다음 자정까지 남은 밀리초. 정확히 자정이어도 0이 아니라 하루치를 돌려주므로
 * 이 값을 기다리는 쪽이 바쁜 루프에 빠지지 않는다. 한국은 서머타임이 없어 LocalDateTime
 * 기준 계산으로 충분하고, 호출부가 깰 때마다 실제 현재 시각으로 다시 계산하기 때문에
 * 기기 절전 등으로 타이머가 늦게 깨도 다음 계산에서 스스로 맞춰진다.
 *
 * 화면(Compose)과 ViewModel(Flow) 양쪽이 같은 날짜 경계를 쓰도록 여기 한 곳에 둔다.
 */
internal fun millisUntilNextMidnight(now: LocalDateTime): Long =
    Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMillis()

/**
 * 자정을 넘길 때마다 한 번씩 방출하는 신호.
 *
 * 구독 즉시 한 번 방출하고, 그 뒤로는 다음 자정까지 기다렸다가 방출한다 — 첫 방출이
 * 있어야 이 Flow와 combine한 쪽이 구독하자마자 값을 낼 수 있다. 하루 한 번만 깨므로
 * 초 단위 polling이 아니다.
 *
 * "지금이 며칠인가"에만 관여한다. 미래 우체통의 도착/D-day/진행률은 전부 자정 기준
 * 날짜 비교([isFutureMailArrived] 등)라 이 주기로 충분하고, 시:분까지 보는 판정은
 * 프로젝트에 없다.
 *
 * [now]는 테스트에서 경계 직전 시각을 넣어 짧은 주기로 검증하기 위한 seam이다.
 */
internal fun dayBoundaryTicks(
    now: () -> LocalDateTime = { LocalDateTime.now() }
): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(millisUntilNextMidnight(now()))
    }
}
