package com.postcardmemory.ui.gallery

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.utils.PostcardDeletionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

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