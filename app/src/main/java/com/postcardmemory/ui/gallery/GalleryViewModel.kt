package com.postcardmemory.ui.gallery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.ui.futuremail.FutureMailGroup
import com.postcardmemory.ui.futuremail.buildFutureMailGroups
import com.postcardmemory.utils.dayBoundaryTicks
import com.postcardmemory.utils.PostcardDeletionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "GalleryViewModel"

/** 다중 삭제 결과 집계를 사용자 메시지로 표현하는 순수 함수(테스트 가능). */
internal fun buildDeletionResultMessage(
    succeededCount: Int,
    failedCount: Int
): String = when {
    failedCount == 0 ->
        "${succeededCount}개를 삭제했어."

    succeededCount == 0 ->
        "삭제하지 못했어. 다시 시도해줘."

    else ->
        "${succeededCount}개 삭제했고, " +
                "${failedCount}개는 삭제하지 못했어."
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: PostcardRepository,
    private val deletionManager: PostcardDeletionManager
) : ViewModel() {

    val postcards: StateFlow<List<Postcard>> =
        repository
            .getAllPostcards()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    /*
     * 127일차: 기억 밀도 아래 미래 우체통 종이가 읽는 묶음(읽기 전용). 우체통 화면
     * (FutureMailboxViewModel.groups)과 같은 계산 — 보낸 엽서 Flow + 자정 신호 — 이라
     * 도착 여부·D-day가 두 화면에서 갈라지지 않는다. 첫 값이 오기 전은 null로 두어
     * 엽서가 있는데도 "빈 우체통"이 잠깐 비치지 않게 한다.
     */
    val futureMailGroups: StateFlow<List<FutureMailGroup>?> =
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
                initialValue = null
            )

    private val _isDeleting = MutableStateFlow(false)

    /** 삭제 결과 요약 메시지(성공/부분 실패/전체 실패)를 한 번만 전달하는 이벤트. */
    private val _deletionMessages = Channel<String>(Channel.BUFFERED)
    val deletionMessages: Flow<String> = _deletionMessages.receiveAsFlow()

    /*
     * 선택된 포스트카드들의 소유 자산과 데이터베이스 기록을 함께 삭제한다.
     * 상세 화면 삭제(DetailViewModel.deletePostcard)와 동일한
     * PostcardDeletionManager를 사용해 두 경로가 같은 자산 목록을 정리하게
     * 한다. 한 장이 실패해도 나머지 결과를 잃지 않도록 각 엽서의 결과를
     * 독립적으로 집계한다.
     */
    fun deletePostcards(ids: Set<Long>) {
        if (ids.isEmpty() || _isDeleting.value) {
            return
        }

        _isDeleting.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val selectedPostcards =
                postcards.value.filter { postcard ->
                    postcard.id in ids
                }

            val results = deletionManager.deletePostcards(selectedPostcards)

            results
                .filter { it.failedAssets.isNotEmpty() || !it.databaseDeleted }
                .forEach { result ->
                    Log.w(
                        TAG,
                        "엽서 삭제: postcardId=${result.postcardId} " +
                                "db=${result.databaseDeleted} " +
                                "failedAssets=${result.failedAssets}"
                    )
                }

            val succeededCount = results.count { it.databaseDeleted }
            val failedCount = results.size - succeededCount

            _deletionMessages.trySend(
                buildDeletionResultMessage(succeededCount, failedCount)
            )
            _isDeleting.value = false
        }
    }
}