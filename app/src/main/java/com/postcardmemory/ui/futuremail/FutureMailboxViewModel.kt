package com.postcardmemory.ui.futuremail

import com.postcardmemory.data.PostcardRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FutureMailboxViewModel @Inject constructor(
    private val repository: PostcardRepository
) : ViewModel() {

    /**
     * 도착일별로 묶인 그룹만 노출한다(내용 없음). "now"는 map이 다시 평가될
     * 때마다(=화면 재구독으로 Room Flow가 다시 쿼리될 때마다) 새로 읽으므로
     * 앱을 다시 열 때마다 최신 날짜로 도착 여부가 재판정된다.
     */
    val groups: StateFlow<List<FutureMailGroup>> =
        repository
            .getFutureMailPostcards()
            .map { sentPostcards ->
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
    private val _openingDeliverAtMillis = MutableStateFlow<Set<Long>>(emptySet())

    /**
     * 도착한 그룹을 개봉한다 — 상태만 되돌릴 뿐 파일이나 사진 등 어떤 자산도
     * 건드리지 않는다. 개봉 즉시 getAllPostcards()에 다시 잡혀 일반
     * 갤러리로 돌아간다(추가 네비게이션 없이 화면 이동만으로 확인 가능).
     * openFutureMail 자체는 멱등이라 연타해도 데이터는 안전하지만, 가드가
     * 없으면 "엽서가 갤러리로 돌아왔어요" 메시지가 중복으로 뜬다.
     */
    fun openArrivedGroup(group: FutureMailGroup) {
        if (!group.arrived || group.postcardIds.isEmpty()) {
            return
        }

        if (group.deliverAtMillis in _openingDeliverAtMillis.value) {
            return
        }

        _openingDeliverAtMillis.value += group.deliverAtMillis

        viewModelScope.launch(Dispatchers.IO) {
            group.postcardIds.forEach { id ->
                repository.openFutureMail(id)
            }

            _openedMessages.trySend("엽서가 갤러리로 돌아왔어요")
            _openingDeliverAtMillis.value -= group.deliverAtMillis
        }
    }
}
