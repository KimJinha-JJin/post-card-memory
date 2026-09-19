package com.postcardmemory.ui.futuremail

import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.utils.dayBoundaryTicks
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FutureMailboxViewModel @Inject constructor(
    private val repository: PostcardRepository
) : ViewModel() {

    /**
     * 도착일별로 묶인 그룹만 노출한다(내용 없음).
     *
     * Room Flow **하나만** 보고 있으면 저장된 내용이 바뀔 때만 다시 계산되므로,
     * 앱을 켜 둔 채 자정을 넘겨도 도착 버튼·D-day·진행률·봉인 상태가 어제에
     * 머문다. 그래서 자정 신호([dayBoundaryTicks])와 합쳐, DB가 그대로여도
     * 날짜가 바뀌면 다시 판정한다. 하루 한 번 깨는 신호라 polling이 아니고,
     * `WhileSubscribed` 안에 있어 화면을 안 보면 같이 멈춘다.
     */
    val groups: StateFlow<List<FutureMailGroup>> =
        combine(
            repository.getFutureMailPostcards(),
            dayBoundaryTicks()
        ) { sentPostcards, _ ->
            buildFutureMailGroups(
                sentPostcards = sentPostcards,
                nowMillis = System.currentTimeMillis()
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    private val _openedMessages = Channel<String>(Channel.BUFFERED)
    val openedMessages: Flow<String> = _openedMessages.receiveAsFlow()

    /** 개봉 중인 도착일(deliverAtMillis) 집합. 연타로 같은 그룹이 두 번 열리는 것만 막는다. */
    private val openingGuard = FutureMailOpeningGuard()

    /**
     * 도착한 그룹을 개봉한다 — 상태만 되돌릴 뿐 파일이나 사진 등 어떤 자산도
     * 건드리지 않는다. 개봉 즉시 getAllPostcards()에 다시 잡혀 일반
     * 갤러리로 돌아간다(추가 네비게이션 없이 화면 이동만으로 확인 가능).
     * 개봉 자체는 멱등이라 연타해도 데이터는 안전하지만, 가드가 없으면
     * "엽서가 갤러리로 돌아왔어요" 메시지가 중복으로 뜬다.
     *
     * 묶음은 repository.openFutureMailGroup으로 **한 번에** 연다. 예전처럼
     * id마다 따로 열면 중간에 실패했을 때 앞의 몇 장만 열린 채로 남는다.
     * 그리고 개봉 중 표시는 [FutureMailOpeningGuard.releasingAfter]의
     * finally에서 해제하므로, 예외가 나도 표시가 남아 재시도가 영구히
     * 막히는 일이 없다.
     */
    fun openArrivedGroup(group: FutureMailGroup) {
        if (!group.arrived || group.postcardIds.isEmpty()) {
            return
        }

        if (!openingGuard.beginOrSkip(group.deliverAtMillis)) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            openingGuard.releasingAfter(group.deliverAtMillis) {
                repository.openFutureMailGroup(group.postcardIds)
                _openedMessages.trySend("엽서가 갤러리로 돌아왔어요")
            }
        }
    }
}
