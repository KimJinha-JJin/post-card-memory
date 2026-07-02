package com.postcardmemory.ui.detail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.utils.BackgroundImageStorage
import com.postcardmemory.utils.PostcardImageExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ExportState {

    data object Idle : ExportState

    data object Exporting : ExportState

    data class Success(
        val uri: Uri
    ) : ExportState

    data class Error(
        val message: String
    ) : ExportState
}

sealed interface BackgroundUpdateState {

    data object Idle : BackgroundUpdateState

    data object Saving : BackgroundUpdateState

    data object Success : BackgroundUpdateState

    data class Error(
        val message: String
    ) : BackgroundUpdateState
}

sealed interface FontUpdateState {

    data object Idle : FontUpdateState

    data object Saving : FontUpdateState

    data object Success : FontUpdateState

    data class Error(
        val message: String
    ) : FontUpdateState
}

sealed interface LayoutUpdateState {

    data object Idle : LayoutUpdateState

    data object Saving : LayoutUpdateState

    data object Success : LayoutUpdateState

    data class Error(
        val message: String
    ) : LayoutUpdateState
}

sealed interface DateFormatUpdateState {

    data object Idle : DateFormatUpdateState

    data object Saving : DateFormatUpdateState

    data object Success : DateFormatUpdateState

    data class Error(
        val message: String
    ) : DateFormatUpdateState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: PostcardRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _postcard =
        MutableStateFlow<Postcard?>(null)

    val postcard: StateFlow<Postcard?> =
        _postcard

    private val _deleted =
        MutableStateFlow(false)

    val deleted: StateFlow<Boolean> =
        _deleted

    private val _exportState =
        MutableStateFlow<ExportState>(
            ExportState.Idle
        )

    val exportState: StateFlow<ExportState> =
        _exportState

    private val _backgroundUpdateState =
        MutableStateFlow<BackgroundUpdateState>(
            BackgroundUpdateState.Idle
        )

    val backgroundUpdateState:
            StateFlow<BackgroundUpdateState> =
        _backgroundUpdateState

    private val _fontUpdateState =
        MutableStateFlow<FontUpdateState>(
            FontUpdateState.Idle
        )

    val fontUpdateState:
            StateFlow<FontUpdateState> =
        _fontUpdateState

    private val _layoutUpdateState =
        MutableStateFlow<LayoutUpdateState>(
            LayoutUpdateState.Idle
        )

    val layoutUpdateState:
            StateFlow<LayoutUpdateState> =
        _layoutUpdateState

    private val _dateFormatUpdateState =
        MutableStateFlow<DateFormatUpdateState>(
            DateFormatUpdateState.Idle
        )

    val dateFormatUpdateState:
            StateFlow<DateFormatUpdateState> =
        _dateFormatUpdateState

    fun loadPostcard(
        postcardId: Long
    ) {
        viewModelScope.launch {
            val loadedPostcard =
                withContext(Dispatchers.IO) {
                    repository.getPostcardById(
                        postcardId
                    )
                }

            _postcard.value =
                loadedPostcard
        }
    }

    fun updateMessage(
        message: String
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val normalizedMessage =
            message.take(120)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updatePostcardMessage(
                    id = currentPostcard.id,
                    message = normalizedMessage
                )
            }

            _postcard.value =
                currentPostcard.copy(
                    message = normalizedMessage
                )
        }
    }

    fun updateMessageFont(
        messageFont: String
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val normalizedFont =
            normalizeMessageFont(
                messageFont
            )

        if (
            normalizedFont ==
            currentPostcard.messageFont
        ) {
            return
        }

        _fontUpdateState.value =
            FontUpdateState.Saving

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository
                        .updatePostcardMessageFont(
                            id = currentPostcard.id,
                            messageFont =
                                normalizedFont
                        )
                }

                _postcard.value =
                    currentPostcard.copy(
                        messageFont =
                            normalizedFont
                    )

                _fontUpdateState.value =
                    FontUpdateState.Success
            } catch (exception: Exception) {
                _fontUpdateState.value =
                    FontUpdateState.Error(
                        exception.message
                            ?: "글귀 폰트를 저장하지 못했습니다."
                    )
            }
        }
    }

    fun updateLayoutStyle(
        layoutStyle: String
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val normalizedLayoutStyle =
            normalizeLayoutStyle(
                layoutStyle
            )

        if (
            normalizedLayoutStyle ==
            currentPostcard.layoutStyle
        ) {
            return
        }

        _layoutUpdateState.value =
            LayoutUpdateState.Saving

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository
                        .updatePostcardLayoutStyle(
                            id = currentPostcard.id,
                            layoutStyle =
                                normalizedLayoutStyle
                        )
                }

                _postcard.value =
                    currentPostcard.copy(
                        layoutStyle =
                            normalizedLayoutStyle
                    )

                _layoutUpdateState.value =
                    LayoutUpdateState.Success
            } catch (exception: Exception) {
                _layoutUpdateState.value =
                    LayoutUpdateState.Error(
                        exception.message
                            ?: "레이아웃을 저장하지 못했습니다."
                    )
            }
        }
    }

    fun updateDateFormat(
        dateFormat: String
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val normalizedDateFormat =
            normalizeDateFormat(
                dateFormat
            )

        if (
            normalizedDateFormat ==
            currentPostcard.dateFormat
        ) {
            return
        }

        _dateFormatUpdateState.value =
            DateFormatUpdateState.Saving

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository
                        .updatePostcardDateFormat(
                            id = currentPostcard.id,
                            dateFormat =
                                normalizedDateFormat
                        )
                }

                _postcard.value =
                    currentPostcard.copy(
                        dateFormat =
                            normalizedDateFormat
                    )

                _dateFormatUpdateState.value =
                    DateFormatUpdateState.Success
            } catch (exception: Exception) {
                _dateFormatUpdateState.value =
                    DateFormatUpdateState.Error(
                        exception.message
                            ?: "날짜 형식을 저장하지 못했습니다."
                    )
            }
        }
    }

    fun updateBackgroundColor(
        backgroundColorArgb: Long
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val previousBackgroundPath =
            currentPostcard.backgroundImagePath

        _backgroundUpdateState.value =
            BackgroundUpdateState.Saving

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updatePostcardBackground(
                        id = currentPostcard.id,
                        backgroundColorArgb =
                            backgroundColorArgb,
                        backgroundImagePath = null
                    )

                    BackgroundImageStorage
                        .deleteBackgroundImage(
                            previousBackgroundPath
                        )
                }

                _postcard.value =
                    currentPostcard.copy(
                        backgroundColorArgb =
                            backgroundColorArgb,
                        backgroundImagePath = null
                    )

                _backgroundUpdateState.value =
                    BackgroundUpdateState.Success
            } catch (exception: Exception) {
                _backgroundUpdateState.value =
                    BackgroundUpdateState.Error(
                        exception.message
                            ?: "배경색을 저장하지 못했습니다."
                    )
            }
        }
    }

    fun updateBackgroundPattern(
        backgroundPattern: String
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val normalizedPattern =
            normalizeBackgroundPattern(
                backgroundPattern
            )

        if (
            normalizedPattern ==
            currentPostcard.backgroundPattern
        ) {
            return
        }

        _backgroundUpdateState.value =
            BackgroundUpdateState.Saving

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository
                        .updatePostcardBackgroundPattern(
                            id = currentPostcard.id,
                            backgroundPattern =
                                normalizedPattern
                        )
                }

                _postcard.value =
                    currentPostcard.copy(
                        backgroundPattern =
                            normalizedPattern
                    )

                _backgroundUpdateState.value =
                    BackgroundUpdateState.Success
            } catch (exception: Exception) {
                _backgroundUpdateState.value =
                    BackgroundUpdateState.Error(
                        exception.message
                            ?: "배경 패턴을 저장하지 못했습니다."
                    )
            }
        }
    }

    fun updateBackgroundImage(
        sourceUri: Uri
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val previousBackgroundPath =
            currentPostcard.backgroundImagePath

        _backgroundUpdateState.value =
            BackgroundUpdateState.Saving

        viewModelScope.launch {
            var newBackgroundPath: String? =
                null

            try {
                newBackgroundPath =
                    withContext(Dispatchers.IO) {
                        BackgroundImageStorage
                            .copyToAppStorage(
                                context = context,
                                sourceUri = sourceUri
                            )
                    }

                withContext(Dispatchers.IO) {
                    repository.updatePostcardBackground(
                        id = currentPostcard.id,
                        backgroundColorArgb =
                            currentPostcard
                                .backgroundColorArgb,
                        backgroundImagePath =
                            newBackgroundPath
                    )

                    if (
                        previousBackgroundPath !=
                        newBackgroundPath
                    ) {
                        BackgroundImageStorage
                            .deleteBackgroundImage(
                                previousBackgroundPath
                            )
                    }
                }

                _postcard.value =
                    currentPostcard.copy(
                        backgroundImagePath =
                            newBackgroundPath
                    )

                _backgroundUpdateState.value =
                    BackgroundUpdateState.Success
            } catch (exception: Exception) {
                withContext(Dispatchers.IO) {
                    BackgroundImageStorage
                        .deleteBackgroundImage(
                            newBackgroundPath
                        )
                }

                _backgroundUpdateState.value =
                    BackgroundUpdateState.Error(
                        exception.message
                            ?: "배경사진을 저장하지 못했습니다."
                    )
            }
        }
    }

    fun removeBackgroundImage() {
        val currentPostcard =
            _postcard.value
                ?: return

        val previousBackgroundPath =
            currentPostcard.backgroundImagePath

        _backgroundUpdateState.value =
            BackgroundUpdateState.Saving

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updatePostcardBackground(
                        id = currentPostcard.id,
                        backgroundColorArgb =
                            currentPostcard
                                .backgroundColorArgb,
                        backgroundImagePath = null
                    )

                    BackgroundImageStorage
                        .deleteBackgroundImage(
                            previousBackgroundPath
                        )
                }

                _postcard.value =
                    currentPostcard.copy(
                        backgroundImagePath = null
                    )

                _backgroundUpdateState.value =
                    BackgroundUpdateState.Success
            } catch (exception: Exception) {
                _backgroundUpdateState.value =
                    BackgroundUpdateState.Error(
                        exception.message
                            ?: "배경사진을 제거하지 못했습니다."
                    )
            }
        }
    }

    fun resetBackgroundUpdateState() {
        _backgroundUpdateState.value =
            BackgroundUpdateState.Idle
    }

    fun resetFontUpdateState() {
        _fontUpdateState.value =
            FontUpdateState.Idle
    }

    fun resetLayoutUpdateState() {
        _layoutUpdateState.value =
            LayoutUpdateState.Idle
    }

    fun resetDateFormatUpdateState() {
        _dateFormatUpdateState.value =
            DateFormatUpdateState.Idle
    }

    fun exportPostcardToGallery(
        stickerOverlay: PostcardImageExporter.StickerOverlay? = null
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        if (
            _exportState.value is
                    ExportState.Exporting
        ) {
            return
        }

        _exportState.value =
            ExportState.Exporting

        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    PostcardImageExporter
                        .exportToGallery(
                            context = context,
                            postcard = currentPostcard,
                            stickerOverlay = stickerOverlay
                        )
                }

            result.fold(
                onSuccess = { uri ->
                    _exportState.value =
                        ExportState.Success(uri)
                },
                onFailure = { exception ->
                    _exportState.value =
                        ExportState.Error(
                            exception.message
                                ?: "이미지를 저장하지 못했습니다."
                        )
                }
            )
        }
    }

    fun resetExportState() {
        _exportState.value =
            ExportState.Idle
    }

    fun deletePostcard() {
        val currentPostcard =
            _postcard.value
                ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                File(
                    currentPostcard.imagePath
                ).let { imageFile ->
                    if (imageFile.exists()) {
                        imageFile.delete()
                    }
                }

                BackgroundImageStorage
                    .deleteBackgroundImage(
                        currentPostcard
                            .backgroundImagePath
                    )

                repository.deletePostcard(
                    currentPostcard
                )
            }

            _deleted.value = true
        }
    }

    private fun normalizeBackgroundPattern(
        backgroundPattern: String
    ): String {
        return when (backgroundPattern) {
            "DOTS",
            "STARS",
            "HEARTS",
            "CHECKER",
            "CHERRY_BLOSSOMS",
            "TRIANGLES",
            "SQUARES" -> backgroundPattern

            else -> "NONE"
        }
    }

    private fun normalizeMessageFont(
        messageFont: String
    ): String {
        return when (messageFont) {
            "DEFAULT",
            "SANS_SERIF",
            "SERIF",
            "MONOSPACE",
            "CURSIVE" -> messageFont

            else -> "SERIF"
        }
    }

    private fun normalizeLayoutStyle(
        layoutStyle: String
    ): String {
        return when (layoutStyle) {
            "STANDARD",
            "PHOTO_FOCUS",
            "AIRY",
            "MAGAZINE" -> layoutStyle

            else -> "STANDARD"
        }
    }


    private fun normalizeDateFormat(
        dateFormat: String
    ): String {
        return when (dateFormat) {
            "DOT",
            "KOREAN",
            "ENGLISH_LONG",
            "ENGLISH_SHORT" -> dateFormat

            else -> "DOT"
        }
    }
}
