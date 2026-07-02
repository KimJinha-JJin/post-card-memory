package com.postcardmemory.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val repository: PostcardRepository
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
     * 선택된 포스트카드들의 사진 파일과
     * 데이터베이스 기록을 함께 삭제한다.
     */
    fun deletePostcards(ids: Set<Long>) {
        if (ids.isEmpty()) {
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val selectedPostcards =
                postcards.value.filter { postcard ->
                    postcard.id in ids
                }

            selectedPostcards.forEach { postcard ->
                val imageFile = File(postcard.imagePath)

                if (imageFile.exists()) {
                    imageFile.delete()
                }
            }

            repository.deletePostcardsByIds(
                ids.toList()
            )
        }
    }
}