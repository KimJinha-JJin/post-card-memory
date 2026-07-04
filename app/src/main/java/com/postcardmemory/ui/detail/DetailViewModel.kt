package com.postcardmemory.ui.detail

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.utils.BackgroundImageStorage
import com.postcardmemory.utils.PostcardImageExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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

sealed interface StickerBackgroundRemovalState {

    data object Idle : StickerBackgroundRemovalState

    data class Removing(
        val stickerId: String,
        val sourceUri: Uri
    ) : StickerBackgroundRemovalState

    data class Success(
        val stickerId: String,
        val sourceUri: Uri,
        val resultUri: Uri
    ) : StickerBackgroundRemovalState

    data class Error(
        val stickerId: String,
        val message: String
    ) : StickerBackgroundRemovalState
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

    private val _stickerBackgroundRemovalState =
        MutableStateFlow<StickerBackgroundRemovalState>(
            StickerBackgroundRemovalState.Idle
        )

    val stickerBackgroundRemovalState:
            StateFlow<StickerBackgroundRemovalState> =
        _stickerBackgroundRemovalState

    private val _photoStickers =
        MutableStateFlow(listOf<PhotoStickerItem>())

    val photoStickers: StateFlow<List<PhotoStickerItem>> =
        _photoStickers

    private val _selectedStickerId =
        MutableStateFlow<String?>(null)

    val selectedStickerId: StateFlow<String?> =
        _selectedStickerId

    fun setPhotoStickers(
        stickers: List<PhotoStickerItem>
    ) {
        _photoStickers.value = stickers
    }

    fun setSelectedStickerId(
        id: String?
    ) {
        _selectedStickerId.value = id
    }

    fun savePhotoStickersState(
        postcardId: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val stickerCacheDir =
                File(
                    context.cacheDir,
                    "photo_stickers"
                ).canonicalFile
            val persistDir =
                File(
                    context.filesDir,
                    "sticker_bgs/$postcardId"
                )
            if (
                !persistDir.exists() &&
                !persistDir.mkdirs()
            ) {
                return@launch
            }

            val updatedStickers =
                _photoStickers.value.map { sticker ->
                    persistStickerBackground(
                        sticker = sticker,
                        stickerCacheDir = stickerCacheDir,
                        persistDir = persistDir
                    )
                }

            if (updatedStickers != _photoStickers.value) {
                _photoStickers.value = updatedStickers
            }

            val stateDir =
                File(context.filesDir, "sticker_states")
            if (!stateDir.exists()) stateDir.mkdirs()
            File(stateDir, "$postcardId.txt").writeText(
                updatedStickers.joinToString("\n") {
                    it.serialize()
                }
            )
        }
    }

    fun loadPhotoStickersState(
        postcardId: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val file =
                File(
                    context.filesDir,
                    "sticker_states/$postcardId.txt"
                )
            _selectedStickerId.value = null
            if (!file.exists()) return@launch
            val persistDir =
                File(
                    context.filesDir,
                    "sticker_bgs/$postcardId"
                )
            val stickers =
                file.readLines()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        val item =
                            deserializePhotoStickerItem(
                                line
                            ) ?: return@mapNotNull null
                        restorePersistedStickerBackground(
                            sticker = item,
                            persistDir = persistDir
                        )
                    }
            if (stickers.isNotEmpty()) {
                _photoStickers.value = stickers
            }
        }
    }

    private var subjectSegmenter: SubjectSegmenter? =
        null

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

    private fun persistStickerBackground(
        sticker: PhotoStickerItem,
        stickerCacheDir: File,
        persistDir: File
    ): PhotoStickerItem {
        if (!sticker.isBackgroundRemoved) {
            return sticker
        }

        val removedUri =
            sticker.removedBgUri
        val destFile =
            File(
                persistDir,
                "${sticker.id}.png"
            ).canonicalFile

        if (removedUri == null) {
            val restoredFile =
                destFile.takeIf {
                    it.exists() && it.canRead()
                }
            val restoredUri =
                restoredFile?.let { Uri.fromFile(it) }
                    ?: return sticker.copy(
                        displayedUri = sticker.originalUri,
                        removedBgUri = null,
                        isBackgroundRemoved = false
                    )

            return sticker.copy(
                displayedUri = restoredUri,
                removedBgUri = restoredUri,
                isBackgroundRemoved = true
            )
        }

        if (removedUri.scheme != "file") {
            return sticker.copy(
                displayedUri = removedUri,
                removedBgUri = removedUri,
                isBackgroundRemoved = true
            )
        }

        val srcPath =
            removedUri.path
                ?: return sticker.copy(
                    displayedUri = sticker.originalUri,
                    removedBgUri = null,
                    isBackgroundRemoved = false
                )
        val srcFile =
            File(srcPath).canonicalFile

        val finalFile =
            if (
                srcFile.path.startsWith(
                    stickerCacheDir.path
                )
            ) {
                if (
                    srcFile.exists() &&
                    srcFile.canRead()
                ) {
                    srcFile.copyTo(
                        destFile,
                        overwrite = true
                    )
                    if (
                        destFile.exists() &&
                        destFile.canRead()
                    ) {
                        srcFile.delete()
                        destFile
                    } else {
                        srcFile
                    }
                } else if (
                    destFile.exists() &&
                    destFile.canRead()
                ) {
                    destFile
                } else {
                    null
                }
            } else {
                if (
                    srcFile.exists() &&
                    srcFile.canRead()
                ) {
                    srcFile
                } else if (
                    destFile.exists() &&
                    destFile.canRead()
                ) {
                    destFile
                } else {
                    null
                }
            }

        val finalUri =
            finalFile?.let { Uri.fromFile(it) }
                ?: return sticker.copy(
                    displayedUri = sticker.originalUri,
                    removedBgUri = null,
                    isBackgroundRemoved = false
                )

        return sticker.copy(
            displayedUri = finalUri,
            removedBgUri = finalUri,
            isBackgroundRemoved = true
        )
    }

    private fun restorePersistedStickerBackground(
        sticker: PhotoStickerItem,
        persistDir: File
    ): PhotoStickerItem {
        if (!sticker.isBackgroundRemoved) {
            return sticker.copy(
                displayedUri = sticker.originalUri,
                removedBgUri = null,
                isBackgroundRemoved = false
            )
        }

        val persistedFile =
            File(
                persistDir,
                "${sticker.id}.png"
            )
        val removedFile =
            sticker.removedBgUri
                ?.takeIf { it.scheme == "file" }
                ?.path
                ?.let { File(it) }
        val validFile =
            when {
                removedFile?.exists() == true &&
                        removedFile.canRead() ->
                    removedFile

                persistedFile.exists() &&
                        persistedFile.canRead() ->
                    persistedFile

                else -> null
            }

        val validUri =
            validFile?.let { Uri.fromFile(it) }
                ?: return sticker.copy(
                    displayedUri = sticker.originalUri,
                    removedBgUri = null,
                    isBackgroundRemoved = false
                )

        return sticker.copy(
            displayedUri = validUri,
            removedBgUri = validUri,
            isBackgroundRemoved = true
        )
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

    fun removeStickerBackground(
        stickerId: String,
        sourceUri: Uri
    ) {
        if (
            _stickerBackgroundRemovalState.value
                    is StickerBackgroundRemovalState.Removing
        ) {
            return
        }

        _stickerBackgroundRemovalState.value =
            StickerBackgroundRemovalState.Removing(
                stickerId = stickerId,
                sourceUri = sourceUri
            )

        viewModelScope.launch {
            val result =
                runCatching {
                    val inputImage =
                        withContext(Dispatchers.IO) {
                            InputImage.fromFilePath(
                                context,
                                sourceUri
                            )
                        }

                    val foregroundBitmap =
                        getSubjectSegmenter()
                            .process(inputImage)
                            .awaitResult()
                            .foregroundBitmap
                            ?: throw IllegalStateException(
                                "\uBC30\uACBD \uC81C\uAC70 \uACB0\uACFC\uB97C \uB9CC\uB4E4\uC9C0 \uBABB\uD588\uC5B4."
                            )

                    try {
                        withContext(Dispatchers.IO) {
                            saveStickerForegroundBitmap(
                                foregroundBitmap
                            )
                        }
                    } finally {
                        if (!foregroundBitmap.isRecycled) {
                            foregroundBitmap.recycle()
                        }
                    }
                }

            result.fold(
                onSuccess = { resultUri ->
                    _stickerBackgroundRemovalState.value =
                        StickerBackgroundRemovalState.Success(
                            stickerId = stickerId,
                            sourceUri = sourceUri,
                            resultUri = resultUri
                        )
                },
                onFailure = {
                    _stickerBackgroundRemovalState.value =
                        StickerBackgroundRemovalState.Error(
                            stickerId = stickerId,
                            message = "\uBC30\uACBD \uC81C\uAC70\uB97C \uC900\uBE44\uD558\uC9C0 \uBABB\uD588\uC5B4. \uC7A0\uC2DC \uB4A4 \uB2E4\uC2DC \uC2DC\uB3C4\uD574\uC918."
                        )
                }
            )
        }
    }

    fun resetStickerBackgroundRemovalState() {
        _stickerBackgroundRemovalState.value =
            StickerBackgroundRemovalState.Idle
    }

    fun deleteStickerCacheUri(
        uri: Uri?
    ) {
        val file =
            uri
                ?.takeIf { cachedUri ->
                    cachedUri.scheme == "file"
                }
                ?.path
                ?.let { path ->
                    File(path)
                }
                ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val stickerCacheDir =
                File(
                    context.cacheDir,
                    "photo_stickers"
                ).canonicalFile
            val stickerPersistDir =
                File(
                    context.filesDir,
                    "sticker_bgs"
                ).canonicalFile
            val targetFile =
                file.canonicalFile

            if (
                targetFile.path.startsWith(
                    stickerCacheDir.path
                ) ||
                targetFile.path.startsWith(
                    stickerPersistDir.path
                )
            ) {
                targetFile.delete()
            }
        }
    }

    fun duplicateSticker(
        stickerId: String
    ) {
        val original =
            _photoStickers.value.find {
                it.id == stickerId
            } ?: return

        val newId =
            UUID.randomUUID().toString()
        val duplicateOffset =
            original.offset?.plus(
                Offset(40f, 40f)
            )
        val needsFileCopy =
            original.isBackgroundRemoved &&
                    original.removedBgUri?.scheme == "file"

        if (!needsFileCopy) {
            val duplicate =
                original.copy(
                    id = newId,
                    offset = duplicateOffset
                )

            _photoStickers.value =
                _photoStickers.value + duplicate
            _selectedStickerId.value = newId
            return
        }

        val sourceUri = original.removedBgUri!!

        viewModelScope.launch {
            val duplicate =
                withContext(Dispatchers.IO) {
                    val copiedUri =
                        copyStickerBackgroundToCache(
                            sourceUri = sourceUri,
                            newId = newId
                        )

                    val fallbackUri =
                        copiedUri ?: sourceUri.takeIf {
                            isFileUriReadable(it)
                        }

                    if (fallbackUri != null) {
                        original.copy(
                            id = newId,
                            offset = duplicateOffset,
                            removedBgUri = fallbackUri,
                            displayedUri = fallbackUri
                        )
                    } else {
                        // 원본 누끼 파일이 복사 도중 사라진 경우 원본 사진으로 대체
                        original.copy(
                            id = newId,
                            offset = duplicateOffset,
                            removedBgUri = null,
                            displayedUri = original.originalUri,
                            isBackgroundRemoved = false
                        )
                    }
                }

            _photoStickers.value =
                _photoStickers.value + duplicate
            _selectedStickerId.value = newId
        }
    }

    private fun isFileUriReadable(
        uri: Uri
    ): Boolean {
        val path = uri.path ?: return false
        val file = File(path)
        return file.exists() && file.canRead()
    }

    private fun copyStickerBackgroundToCache(
        sourceUri: Uri,
        newId: String
    ): Uri? {
        val sourcePath =
            sourceUri.path ?: return null
        val sourceFile =
            File(sourcePath)

        if (!sourceFile.exists() || !sourceFile.canRead()) {
            return null
        }

        val stickerCacheDir =
            File(
                context.cacheDir,
                "photo_stickers"
            )

        if (
            !stickerCacheDir.exists() &&
            !stickerCacheDir.mkdirs()
        ) {
            return null
        }

        val destFile =
            File(
                stickerCacheDir,
                "duplicated_" +
                        newId +
                        "_" +
                        System.currentTimeMillis() +
                        ".png"
            )

        return runCatching {
            sourceFile.copyTo(
                destFile,
                overwrite = true
            )
            Uri.fromFile(destFile)
        }.getOrNull()
    }

    fun moveStickerForward(
        stickerId: String
    ) {
        val stickers = _photoStickers.value
        val index =
            stickers.indexOfFirst { it.id == stickerId }

        if (index == -1 || index == stickers.lastIndex) {
            return
        }

        _photoStickers.value =
            stickers.toMutableList().apply {
                val temp = this[index]
                this[index] = this[index + 1]
                this[index + 1] = temp
            }
    }

    fun moveStickerBackward(
        stickerId: String
    ) {
        val stickers = _photoStickers.value
        val index =
            stickers.indexOfFirst { it.id == stickerId }

        if (index <= 0) {
            return
        }

        _photoStickers.value =
            stickers.toMutableList().apply {
                val temp = this[index]
                this[index] = this[index - 1]
                this[index - 1] = temp
            }
    }

    fun exportPostcardToGallery(
        stickerOverlays: List<PostcardImageExporter.StickerOverlay> = emptyList()
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
                            stickerOverlays = stickerOverlays
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

                File(
                    context.filesDir,
                    "sticker_states/" +
                            "${currentPostcard.id}.txt"
                ).let { stickerFile ->
                    if (stickerFile.exists()) {
                        stickerFile.delete()
                    }
                }

                File(
                    context.filesDir,
                    "sticker_bgs/${currentPostcard.id}"
                ).let { bgDir ->
                    if (bgDir.exists()) {
                        bgDir.deleteRecursively()
                    }
                }

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
            "MAGAZINE",
            "POLAROID" -> layoutStyle

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

    private fun getSubjectSegmenter():
            SubjectSegmenter {
        val currentSegmenter =
            subjectSegmenter

        if (currentSegmenter != null) {
            return currentSegmenter
        }

        val options =
            SubjectSegmenterOptions.Builder()
                .enableForegroundBitmap()
                .build()
        val newSegmenter =
            SubjectSegmentation.getClient(
                options
            )

        subjectSegmenter =
            newSegmenter

        return newSegmenter
    }

    private fun saveStickerForegroundBitmap(
        bitmap: Bitmap
    ): Uri {
        val stickerCacheDir =
            File(
                context.cacheDir,
                "photo_stickers"
            )

        if (
            !stickerCacheDir.exists() &&
            !stickerCacheDir.mkdirs()
        ) {
            throw IllegalStateException(
                "\uC2A4\uD2F0\uCEE4 \uCE90\uC2DC \uD3F4\uB354\uB97C \uB9CC\uB4E4\uC9C0 \uBABB\uD588\uC5B4."
            )
        }

        val outputFile =
            File(
                stickerCacheDir,
                "removed_background_" +
                        System.currentTimeMillis() +
                        ".png"
            )

        val outputBitmap =
            if (
                bitmap.config ==
                Bitmap.Config.ARGB_8888
            ) {
                bitmap
            } else {
                bitmap.copy(
                    Bitmap.Config.ARGB_8888,
                    false
                )
            }

        FileOutputStream(outputFile).use { stream ->
            val saved =
                outputBitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    stream
                )

            if (!saved) {
                throw IllegalStateException(
                    "\uC2A4\uD2F0\uCEE4 PNG\uB97C \uC800\uC7A5\uD558\uC9C0 \uBABB\uD588\uC5B4."
                )
            }
        }

        if (outputBitmap != bitmap) {
            outputBitmap.recycle()
        }

        return Uri.fromFile(outputFile)
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            addOnFailureListener { exception ->
                if (continuation.isActive) {
                    continuation.resumeWithException(
                        exception
                    )
                }
            }
        }

    override fun onCleared() {
        subjectSegmenter?.close()
        subjectSegmenter = null
        val stickerCacheDir =
            File(
                context.cacheDir,
                "photo_stickers"
            ).canonicalFile
        _photoStickers.value.forEach { sticker ->
            sticker.removedBgUri
                ?.takeIf { it.scheme == "file" }
                ?.path
                ?.let { path ->
                    val f = File(path).canonicalFile
                    if (f.path.startsWith(stickerCacheDir.path)) {
                        f.delete()
                    }
                }
        }
        super.onCleared()
    }
}
