package com.postcardmemory.ui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.postcardmemory.utils.millisUntilNextMidnight
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * 자정을 넘기면 스스로 새 날짜가 되는 "오늘". 앱을 켜 둔 채 날짜가 바뀌었을 때 화면마다
 * 현재 날짜 기준이 갈라지는 것을 막는다 — 레트로 시계([GalleryRetroClock])는 매초 다시
 * 읽어 이미 새 날짜인데, 이 값을 쓰지 않는 화면만 어제에 머무는 상황을 없앤다.
 *
 * 쓰는 곳은 방문 달력(오늘 marker·채움색·picker 현재 월/연도 판정·오늘 복귀 링크),
 * 기억밀도 화면(기준 연도), 그리고 봉인된 미래 엽서 상세 화면(도착·D-day 판정)이다.
 * ViewModel 쪽에서 같은 경계가 필요하면 Compose state 대신
 * [com.postcardmemory.utils.dayBoundaryTicks]를 쓴다 — 둘 다 같은
 * [millisUntilNextMidnight]를 쓰므로 기준이 갈라지지 않는다. **바뀌는 건 "지금이 며칠인가"뿐이고 사용자가 보고 있던
 * 위치는 건드리지 않는다** — 달력의 `displayedMonth`/`pickerYear`가 key 없는
 * `rememberSaveable`로 남아 있는 것이 그 보장이다.
 *
 * 호출할 때마다 각자의 state를 새로 가지므로(공유 singleton이 아니다) 화면끼리 상태가
 * 얽히지 않는다. 시계처럼 매초 깨우지 않고 다음 자정까지 한 번만 기다린다 — 하루 한 번이면
 * 충분한 갱신이라, 주기가 다른 시계의 1초 ticker와 상태를 억지로 합치지 않고 각자 두었다.
 * [LaunchedEffect]는 Activity의 lifecycle-aware recomposer 위에서 돌아 별도 lifecycle
 * 처리가 필요 없다.
 */
@Composable
internal fun rememberTodayDate(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(millisUntilNextMidnight(LocalDateTime.now()))
            today = LocalDate.now()
        }
    }
    return today
}
