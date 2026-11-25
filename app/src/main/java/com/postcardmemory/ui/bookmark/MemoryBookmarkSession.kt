package com.postcardmemory.ui.bookmark

import androidx.lifecycle.SavedStateHandle
import com.postcardmemory.data.BookmarkDesign
import com.postcardmemory.data.BookmarkPaper
import com.postcardmemory.data.BookmarkShape
import com.postcardmemory.data.BookmarkTrace
import com.postcardmemory.data.MemoryBookmarkRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** 저장이 성공해야만 [INSERTING]으로 넘어가서, 끼우는 연출과 완료 화면은 실제 저장 뒤에만 나와. */
internal enum class BookmarkStep { EDITING, SAVING, INSERTING, DONE, REMOVING }

internal data class BookmarkStudioState(
    val postcardId: Long,
    val draft: BookmarkDesign,
    val hadBookmark: Boolean,
    val step: BookmarkStep = BookmarkStep.EDITING,
    val error: String? = null,
    /** 끼우기에 성공할 때마다 1씩 늘어. 화면이 같은 연출을 두 번 재생하지 않게 구분하는 번호야. */
    val insertToken: Int = 0
) {
    val busy: Boolean get() = step == BookmarkStep.SAVING || step == BookmarkStep.REMOVING
}

/**
 * 책갈피 제작 한 번의 상태. 엽서 편집 상태·초안·확정 저장과 완전히 분리되어 memory_bookmarks
 * 테이블만 써. 고르는 중의 값은 DB가 아니라 [savedState]에만 있어서 화면이 다시 만들어져도 남고,
 * 취소하면 그대로 버려져.
 */
internal class MemoryBookmarkSession(
    private val scope: CoroutineScope,
    private val repository: MemoryBookmarkRepository,
    private val savedState: SavedStateHandle
) {
    private val _state = MutableStateFlow(restore())
    val state: StateFlow<BookmarkStudioState?> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    private var opening = false

    fun open(postcardId: Long) {
        if (postcardId <= 0 || opening || _state.value != null) return
        opening = true
        scope.launch {
            try {
                val row = repository.get(postcardId)
                val design = row?.designOrNull()
                if (row != null && design == null) {
                    _messages.trySend("새 버전에서 만든 책갈피라 여기서는 바꿀 수 없어.")
                } else {
                    publish(
                        BookmarkStudioState(
                            postcardId = postcardId,
                            draft = design ?: BookmarkDesign(),
                            hadBookmark = row != null
                        )
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _messages.trySend("책갈피를 불러오지 못했어. 다시 시도해 줘.")
            } finally {
                opening = false
            }
        }
    }

    fun changeDraft(change: (BookmarkDesign) -> BookmarkDesign) {
        val current = _state.value ?: return
        if (current.step != BookmarkStep.EDITING) return
        publish(current.copy(draft = change(current.draft), error = null))
    }

    fun insert() {
        val current = _state.value ?: return
        if (current.step != BookmarkStep.EDITING) return
        publish(current.copy(step = BookmarkStep.SAVING, error = null))
        scope.launch {
            try {
                repository.save(current.postcardId, current.draft)
                updateIf(BookmarkStep.SAVING) {
                    it.copy(
                        step = BookmarkStep.INSERTING,
                        hadBookmark = true,
                        insertToken = it.insertToken + 1
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateIf(BookmarkStep.SAVING) {
                    it.copy(
                        step = BookmarkStep.EDITING,
                        error = "책갈피를 저장하지 못했어. 고른 종이는 그대로니까 다시 시도해 줘."
                    )
                }
            }
        }
    }

    fun onInsertionShown() = updateIf(BookmarkStep.INSERTING) { it.copy(step = BookmarkStep.DONE) }

    fun editAgain() = updateIf(BookmarkStep.DONE) { it.copy(step = BookmarkStep.EDITING) }

    /** 책갈피 행만 지워. 엽서·사진·꾸미기에는 쓰지 않아. */
    fun remove() {
        val current = _state.value ?: return
        if (current.step != BookmarkStep.EDITING || !current.hadBookmark) return
        publish(current.copy(step = BookmarkStep.REMOVING, error = null))
        scope.launch {
            try {
                repository.remove(current.postcardId)
                publish(null)
                _messages.trySend("책갈피를 뺐어. 엽서는 그대로야.")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateIf(BookmarkStep.REMOVING) {
                    it.copy(step = BookmarkStep.EDITING, error = "책갈피를 빼지 못했어. 다시 시도해 줘.")
                }
            }
        }
    }

    /** 저장·제거가 끝나기 전에는 닫지 않아(결과를 모르는 채 사라지지 않게). */
    fun close() {
        if (_state.value?.busy == true) return
        publish(null)
    }

    private fun updateIf(step: BookmarkStep, change: (BookmarkStudioState) -> BookmarkStudioState) {
        val current = _state.value ?: return
        if (current.step == step) publish(change(current))
    }

    private fun publish(next: BookmarkStudioState?) {
        _state.value = next
        savedState[KEY_POSTCARD] = next?.postcardId
        savedState[KEY_SHAPE] = next?.draft?.shape?.name
        savedState[KEY_PAPER] = next?.draft?.paper?.name
        savedState[KEY_TRACE] = next?.draft?.trace?.name
        savedState[KEY_HAD] = next?.hadBookmark
        savedState[KEY_TOKEN] = next?.insertToken
        savedState[KEY_DONE] =
            next?.step == BookmarkStep.INSERTING || next?.step == BookmarkStep.DONE
    }

    /** 프로세스가 되살아났을 때: 저장이 끝났던 것만 완료로, 그 밖에는 고르던 화면으로 돌아가. */
    private fun restore(): BookmarkStudioState? {
        val id = savedState.get<Long>(KEY_POSTCARD) ?: return null
        val fallback = BookmarkDesign()
        val shape = savedState.get<String>(KEY_SHAPE)
        val paper = savedState.get<String>(KEY_PAPER)
        val trace = savedState.get<String>(KEY_TRACE)
        return BookmarkStudioState(
            postcardId = id,
            draft = BookmarkDesign(
                shape = BookmarkShape.entries.firstOrNull { it.name == shape } ?: fallback.shape,
                paper = BookmarkPaper.entries.firstOrNull { it.name == paper } ?: fallback.paper,
                trace = BookmarkTrace.entries.firstOrNull { it.name == trace } ?: fallback.trace
            ),
            hadBookmark = savedState.get<Boolean>(KEY_HAD) == true,
            insertToken = savedState.get<Int>(KEY_TOKEN) ?: 0,
            step = if (savedState.get<Boolean>(KEY_DONE) == true) BookmarkStep.DONE else BookmarkStep.EDITING
        )
    }

    private companion object {
        const val KEY_POSTCARD = "bookmark_studio_postcard"
        const val KEY_SHAPE = "bookmark_studio_shape"
        const val KEY_PAPER = "bookmark_studio_paper"
        const val KEY_TRACE = "bookmark_studio_trace"
        const val KEY_HAD = "bookmark_studio_had"
        const val KEY_TOKEN = "bookmark_studio_token"
        const val KEY_DONE = "bookmark_studio_done"
    }
}
