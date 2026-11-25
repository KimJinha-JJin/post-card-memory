package com.postcardmemory.ui.bookmark

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postcardmemory.data.MemoryBookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 엽서 편집 화면 위에 뜨는 책갈피 제작 화면 전용. 편집 화면의 DetailViewModel과 상태를 나누지 않아. */
@HiltViewModel
class MemoryBookmarkViewModel @Inject constructor(
    private val repository: MemoryBookmarkRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    internal val session = MemoryBookmarkSession(viewModelScope, repository, savedState)

    /** 더보기 메뉴 문구용: 이 엽서에 책갈피가 있는지. */
    fun hasBookmark(postcardId: Long): Flow<Boolean> =
        repository.observe(postcardId).map { it != null }
}
