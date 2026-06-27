package com.postcardmemory.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: PostcardRepository
) : ViewModel() {

    private val _postcard = MutableStateFlow<Postcard?>(null)
    val postcard: StateFlow<Postcard?> = _postcard

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    fun loadPostcard(id: Long) {
        viewModelScope.launch {
            _postcard.value = repository.getPostcardById(id)
        }
    }

    fun deletePostcard() {
        viewModelScope.launch {
            val pc = _postcard.value ?: return@launch
            val file = File(pc.imagePath)
            if (file.exists()) file.delete()
            repository.deletePostcard(pc)
            _deleted.value = true
        }
    }
}
