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
import com.postcardmemory.utils.PhotoColorExtractor
import com.postcardmemory.utils.PhotoStickerImageStorage
import com.postcardmemory.utils.PostcardDraftStorage
import com.postcardmemory.utils.PostcardImageExporter
import com.postcardmemory.utils.PostcardImageStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val SEAL_HISTORY_LIMIT = 50
private const val STICKER_HISTORY_LIMIT = 30
private const val PHOTO_TRANSFORM_HISTORY_LIMIT = 50
private const val DRAFT_AUTOSAVE_DEBOUNCE_MS = 900L

sealed interface DraftSaveStatus {

    data object Idle : DraftSaveStatus

    data object PendingChanges : DraftSaveStatus

    data object Saving : DraftSaveStatus

    data object Saved : DraftSaveStatus

    data object Failed : DraftSaveStatus
}

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

sealed interface ShareState {

    data object Idle : ShareState

    data object Preparing : ShareState

    data class Ready(
        val file: File
    ) : ShareState

    data class Error(
        val message: String
    ) : ShareState
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

sealed interface ImageUpdateState {

    data object Idle : ImageUpdateState

    data object Saving : ImageUpdateState

    data object Success : ImageUpdateState

    data class Error(
        val message: String
    ) : ImageUpdateState
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

sealed interface PhotoColorExtractionState {

    data object Idle : PhotoColorExtractionState

    data object Extracting : PhotoColorExtractionState

    data class Success(
        val colors: List<PhotoColorExtractor.ExtractedColor>
    ) : PhotoColorExtractionState

    data class Error(
        val message: String
    ) : PhotoColorExtractionState
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

    private val _shareState =
        MutableStateFlow<ShareState>(
            ShareState.Idle
        )

    val shareState: StateFlow<ShareState> =
        _shareState

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

    private val _imageUpdateState =
        MutableStateFlow<ImageUpdateState>(
            ImageUpdateState.Idle
        )

    val imageUpdateState:
            StateFlow<ImageUpdateState> =
        _imageUpdateState

    private val _stickerBackgroundRemovalState =
        MutableStateFlow<StickerBackgroundRemovalState>(
            StickerBackgroundRemovalState.Idle
        )

    val stickerBackgroundRemovalState:
            StateFlow<StickerBackgroundRemovalState> =
        _stickerBackgroundRemovalState

    private val _photoColorExtractionState =
        MutableStateFlow<PhotoColorExtractionState>(
            PhotoColorExtractionState.Idle
        )

    val photoColorExtractionState:
            StateFlow<PhotoColorExtractionState> =
        _photoColorExtractionState

    private val _textScaleSaveErrors =
        Channel<String>(Channel.BUFFERED)

    val textScaleSaveErrors: Flow<String> =
        _textScaleSaveErrors.receiveAsFlow()

    private var messageTextScaleSaveJob: Job? = null

    private var dateTextScaleSaveJob: Job? = null

    private var backgroundPatternDensitySaveJob: Job? = null

    private var stampPhotoScaleSaveJob: Job? = null

    private var polaroidPhotoScaleSaveJob: Job? = null

    private var photoEdgeBlurSaveJob: Job? = null

    private var stampPhotoOffsetSaveJob: Job? = null

    private var polaroidPhotoOffsetSaveJob: Job? = null

    private var tapedFilmPhotoOffsetSaveJob: Job? = null

    private var stampPhotoZoomSaveJob: Job? = null

    private var polaroidPhotoZoomSaveJob: Job? = null

    private var tapedFilmPhotoZoomSaveJob: Job? = null

    // ---- 편집 초안(스티커·도장) 자동저장 ----

    private val _draftSaveStatus =
        MutableStateFlow<DraftSaveStatus>(DraftSaveStatus.Idle)

    val draftSaveStatus: StateFlow<DraftSaveStatus> =
        _draftSaveStatus

    private val _draftRecovery =
        MutableStateFlow<PostcardEditDraft?>(null)

    val draftRecovery: StateFlow<PostcardEditDraft?> =
        _draftRecovery

    private var currentDraftPostcardId: Long = 0L
    private var draftCreatedAtMillis: Long = 0L
    private val draftRevisionCounter = AtomicLong(0L)
    private var latestPersistedDraftRevision: Long = 0L
    private val draftSaveMutex = Mutex()
    private var draftAutosaveJob: Job? = null

    /**
     * 새 postcardId로 상세 화면에 진입할 때 한 번 호출한다.
     * 기존 미저장 초안이 있으면 draftRecovery에 채워 UI가 복구 여부를 묻게 한다.
     * 여기서는 아직 photoStickers/photoSeals를 바꾸지 않는다(완성 저장본을 임의로 덮지 않기 위함).
     */
    fun initializeDraftSession(postcardId: Long) {
        currentDraftPostcardId = postcardId
        draftAutosaveJob?.cancel()
        draftRevisionCounter.set(0L)
        latestPersistedDraftRevision = 0L
        _draftSaveStatus.value = DraftSaveStatus.Idle
        _draftRecovery.value = null

        viewModelScope.launch(Dispatchers.IO) {
            val existingDraft =
                PostcardDraftStorage.loadDraft(context, postcardId)

            if (existingDraft != null && existingDraft.postcardId == postcardId) {
                draftCreatedAtMillis = existingDraft.createdAtMillis
                draftRevisionCounter.set(existingDraft.revision)
                latestPersistedDraftRevision = existingDraft.revision
                _draftRecovery.value = existingDraft
            } else {
                draftCreatedAtMillis = System.currentTimeMillis()
            }
        }
    }

    fun resumeDraftRecovery() {
        val draft = _draftRecovery.value ?: return

        _photoStickers.value = draft.stickers
        _selectedStickerId.value = draft.selectedStickerId
        _photoSeals.value = draft.seals
        _selectedSealId.value = draft.selectedSealId

        clearStickerHistory()
        clearSealHistory()

        draftCreatedAtMillis = draft.createdAtMillis
        draftRevisionCounter.set(draft.revision)
        latestPersistedDraftRevision = draft.revision

        _draftRecovery.value = null
    }

    fun discardDraftRecovery() {
        val draft = _draftRecovery.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            PostcardDraftStorage.deleteDraft(context, draft.postcardId)
        }

        _draftRecovery.value = null
    }

    private fun scheduleDraftAutosave() {
        if (currentDraftPostcardId <= 0L) {
            return
        }

        _draftSaveStatus.value = DraftSaveStatus.PendingChanges

        draftAutosaveJob?.cancel()
        draftAutosaveJob = viewModelScope.launch {
            delay(DRAFT_AUTOSAVE_DEBOUNCE_MS)
            persistDraftNow()
        }
    }

    /** 디바운스를 건너뛰고 즉시 flush한다. 화면 이탈·백그라운드 전환 시 사용한다. */
    fun flushDraftNow() {
        if (currentDraftPostcardId <= 0L) {
            return
        }

        draftAutosaveJob?.cancel()
        viewModelScope.launch {
            persistDraftNow()
        }
    }

    private suspend fun persistDraftNow() {
        val postcardId = currentDraftPostcardId
        if (postcardId <= 0L) {
            return
        }

        val candidateRevision = draftRevisionCounter.incrementAndGet()
        val snapshotStickers = _photoStickers.value
        val snapshotSelectedStickerId = _selectedStickerId.value
        val snapshotSeals = _photoSeals.value
        val snapshotSelectedSealId = _selectedSealId.value

        _draftSaveStatus.value = DraftSaveStatus.Saving

        val success = withContext(Dispatchers.IO) {
            draftSaveMutex.withLock {
                if (
                    !shouldPersistDraftRevision(
                        candidateRevision = candidateRevision,
                        latestPersistedRevision = latestPersistedDraftRevision
                    )
                ) {
                    return@withLock true
                }

                val draft = PostcardEditDraft(
                    postcardId = postcardId,
                    createdAtMillis = draftCreatedAtMillis,
                    updatedAtMillis = System.currentTimeMillis(),
                    revision = candidateRevision,
                    stickers = snapshotStickers,
                    selectedStickerId = snapshotSelectedStickerId,
                    seals = snapshotSeals,
                    selectedSealId = snapshotSelectedSealId
                )

                val saved =
                    PostcardDraftStorage.saveDraftAtomically(context, draft)

                if (saved) {
                    latestPersistedDraftRevision = candidateRevision
                }

                saved
            }
        }

        _draftSaveStatus.value =
            if (success) DraftSaveStatus.Saved else DraftSaveStatus.Failed
    }

    /**
     * 확정 저장(팔레트 버튼)이 실제로 성공한 뒤에만 초안을 지운다.
     * 삭제도 draftSaveMutex 안에서 revision을 함께 올려, 이미 진행 중이던
     * 오래된 자동저장이 삭제 직후에 도착해 초안 파일을 되살리지 못하게 한다.
     */
    fun saveEditsAndClearDraft(postcardId: Long) {
        draftAutosaveJob?.cancel()

        viewModelScope.launch(Dispatchers.IO) {
            persistStickerEditState(postcardId)
            persistSealEditState(postcardId)

            draftSaveMutex.withLock {
                latestPersistedDraftRevision =
                    draftRevisionCounter.incrementAndGet()
                PostcardDraftStorage.deleteDraft(context, postcardId)
            }

            withContext(Dispatchers.Main) {
                _draftSaveStatus.value = DraftSaveStatus.Idle
            }
        }
    }

    // ---- 스티커/도장 상태 ----

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
        scheduleDraftAutosave()
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
            persistStickerEditState(postcardId)
        }
    }

    private suspend fun persistStickerEditState(
        postcardId: Long
    ) {
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
            return
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

        clearStickerHistory()

        val stateDir =
            File(context.filesDir, "sticker_states")
        if (!stateDir.exists()) stateDir.mkdirs()
        File(stateDir, "$postcardId.txt").writeText(
            updatedStickers.joinToString("\n") {
                it.serialize()
            }
        )
    }

    fun loadPhotoStickersState(
        postcardId: Long
    ) {
        clearStickerHistory()

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

    private val _photoSeals =
        MutableStateFlow(listOf<PostcardSealItem>())

    val photoSeals: StateFlow<List<PostcardSealItem>> =
        _photoSeals

    private val _selectedSealId =
        MutableStateFlow<String?>(null)

    val selectedSealId: StateFlow<String?> =
        _selectedSealId

    fun setPhotoSeals(
        seals: List<PostcardSealItem>
    ) {
        _photoSeals.value = seals
        scheduleDraftAutosave()
    }

    fun setSelectedSealId(
        id: String?
    ) {
        _selectedSealId.value = id
    }

    fun savePhotoSealsState(
        postcardId: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            persistSealEditState(postcardId)
        }
    }

    private suspend fun persistSealEditState(
        postcardId: Long
    ) {
        val stateDir =
            File(context.filesDir, "seal_states")
        if (!stateDir.exists()) stateDir.mkdirs()
        File(stateDir, "$postcardId.txt").writeText(
            _photoSeals.value.joinToString("\n") {
                it.serialize()
            }
        )
    }

    fun loadPhotoSealsState(
        postcardId: Long
    ) {
        clearSealHistory()

        viewModelScope.launch(Dispatchers.IO) {
            val file =
                File(
                    context.filesDir,
                    "seal_states/$postcardId.txt"
                )
            _selectedSealId.value = null
            if (!file.exists()) return@launch

            val seals =
                file.readLines()
                    .filter { it.isNotBlank() }
                    .mapNotNull { line ->
                        deserializePostcardSealItem(line)
                    }
            if (seals.isNotEmpty()) {
                _photoSeals.value = seals
            }
        }
    }

    private data class SealSnapshot(
        val seals: List<PostcardSealItem>,
        val selectedSealId: String?
    )

    private val sealUndoStack =
        ArrayDeque<SealSnapshot>()

    private val sealRedoStack =
        ArrayDeque<SealSnapshot>()

    private val _canUndoSeal =
        MutableStateFlow(false)

    val canUndoSeal: StateFlow<Boolean> =
        _canUndoSeal

    private val _canRedoSeal =
        MutableStateFlow(false)

    val canRedoSeal: StateFlow<Boolean> =
        _canRedoSeal

    private fun updateSealHistoryAvailability() {
        _canUndoSeal.value = sealUndoStack.isNotEmpty()
        _canRedoSeal.value = sealRedoStack.isNotEmpty()
    }

    private fun clearSealHistory() {
        sealUndoStack.clear()
        sealRedoStack.clear()
        updateSealHistoryAvailability()
    }

    fun recordSealSnapshotForUndo() {
        sealUndoStack.addLast(
            SealSnapshot(
                seals = _photoSeals.value,
                selectedSealId = _selectedSealId.value
            )
        )
        if (sealUndoStack.size > SEAL_HISTORY_LIMIT) {
            sealUndoStack.removeFirst()
        }
        sealRedoStack.clear()
        updateSealHistoryAvailability()
    }

    fun undoSealChange() {
        val previous =
            sealUndoStack.removeLastOrNull() ?: return

        sealRedoStack.addLast(
            SealSnapshot(
                seals = _photoSeals.value,
                selectedSealId = _selectedSealId.value
            )
        )
        _photoSeals.value = previous.seals
        _selectedSealId.value = previous.selectedSealId
        updateSealHistoryAvailability()
        scheduleDraftAutosave()
    }

    fun redoSealChange() {
        val next =
            sealRedoStack.removeLastOrNull() ?: return

        sealUndoStack.addLast(
            SealSnapshot(
                seals = _photoSeals.value,
                selectedSealId = _selectedSealId.value
            )
        )
        _photoSeals.value = next.seals
        _selectedSealId.value = next.selectedSealId
        updateSealHistoryAvailability()
        scheduleDraftAutosave()
    }

    private data class StickerSnapshot(
        val stickers: List<PhotoStickerItem>,
        val selectedStickerId: String?
    )

    private val stickerUndoStack =
        ArrayDeque<StickerSnapshot>()

    private val stickerRedoStack =
        ArrayDeque<StickerSnapshot>()

    private val _canUndoSticker =
        MutableStateFlow(false)

    val canUndoSticker: StateFlow<Boolean> =
        _canUndoSticker

    private val _canRedoSticker =
        MutableStateFlow(false)

    val canRedoSticker: StateFlow<Boolean> =
        _canRedoSticker

    /*
     * 삭제/배경제거 재실행으로 파일이 삭제될 뻔했지만
     * undo/redo 스택이 아직 참조 중이라 미룬 파일들.
     * 스택에서 완전히 밀려나야 실제로 지운다.
     */
    private val stickerCleanupCandidates =
        mutableSetOf<Uri>()

    private fun updateStickerHistoryAvailability() {
        _canUndoSticker.value = stickerUndoStack.isNotEmpty()
        _canRedoSticker.value = stickerRedoStack.isNotEmpty()
    }

    private fun isStickerFileStillReferenced(
        uri: Uri
    ): Boolean {
        val allReachableStickers =
            _photoStickers.value +
                    stickerUndoStack.flatMap { it.stickers } +
                    stickerRedoStack.flatMap { it.stickers }

        return allReachableStickers.any {
            it.originalUri == uri || it.removedBgUri == uri
        }
    }

    private fun sweepStickerCleanupCandidates() {
        val stillPending =
            stickerCleanupCandidates.toList()

        stillPending.forEach { uri ->
            if (!isStickerFileStillReferenced(uri)) {
                stickerCleanupCandidates.remove(uri)
                deleteStickerCacheUri(uri)
                deleteStickerOriginalIfUnreferenced(
                    uri = uri,
                    remainingStickers = _photoStickers.value
                )
            }
        }
    }

    private fun clearStickerHistory() {
        stickerUndoStack.clear()
        stickerRedoStack.clear()
        updateStickerHistoryAvailability()
        sweepStickerCleanupCandidates()
    }

    fun recordStickerSnapshotForUndo() {
        stickerUndoStack.addLast(
            StickerSnapshot(
                stickers = _photoStickers.value,
                selectedStickerId = _selectedStickerId.value
            )
        )
        if (stickerUndoStack.size > STICKER_HISTORY_LIMIT) {
            stickerUndoStack.removeFirst()
            sweepStickerCleanupCandidates()
        }
        stickerRedoStack.clear()
        updateStickerHistoryAvailability()
    }

    fun undoStickerChange() {
        val previous =
            stickerUndoStack.removeLastOrNull() ?: return

        stickerRedoStack.addLast(
            StickerSnapshot(
                stickers = _photoStickers.value,
                selectedStickerId = _selectedStickerId.value
            )
        )
        _photoStickers.value = previous.stickers
        _selectedStickerId.value = previous.selectedStickerId
        updateStickerHistoryAvailability()
        sweepStickerCleanupCandidates()
        scheduleDraftAutosave()
    }

    fun redoStickerChange() {
        val next =
            stickerRedoStack.removeLastOrNull() ?: return

        stickerUndoStack.addLast(
            StickerSnapshot(
                stickers = _photoStickers.value,
                selectedStickerId = _selectedStickerId.value
            )
        )
        _photoStickers.value = next.stickers
        _selectedStickerId.value = next.selectedStickerId
        updateStickerHistoryAvailability()
        sweepStickerCleanupCandidates()
        scheduleDraftAutosave()
    }

    private data class PhotoTransformSnapshot(
        val stampPhotoScale: Float,
        val polaroidPhotoScale: Float,
        val stampPhotoOffsetX: Float,
        val stampPhotoOffsetY: Float,
        val polaroidPhotoOffsetX: Float,
        val polaroidPhotoOffsetY: Float,
        val tapedFilmPhotoOffsetX: Float,
        val tapedFilmPhotoOffsetY: Float,
        val stampPhotoZoom: Float,
        val polaroidPhotoZoom: Float,
        val tapedFilmPhotoZoom: Float
    )

    private val photoTransformUndoStack =
        ArrayDeque<PhotoTransformSnapshot>()

    private val photoTransformRedoStack =
        ArrayDeque<PhotoTransformSnapshot>()

    private val _canUndoPhotoTransform =
        MutableStateFlow(false)

    val canUndoPhotoTransform: StateFlow<Boolean> =
        _canUndoPhotoTransform

    private val _canRedoPhotoTransform =
        MutableStateFlow(false)

    val canRedoPhotoTransform: StateFlow<Boolean> =
        _canRedoPhotoTransform

    private fun currentPhotoTransformSnapshot(): PhotoTransformSnapshot? {
        val current =
            _postcard.value ?: return null

        return PhotoTransformSnapshot(
            stampPhotoScale = current.stampPhotoScale,
            polaroidPhotoScale = current.polaroidPhotoScale,
            stampPhotoOffsetX = current.stampPhotoOffsetX,
            stampPhotoOffsetY = current.stampPhotoOffsetY,
            polaroidPhotoOffsetX = current.polaroidPhotoOffsetX,
            polaroidPhotoOffsetY = current.polaroidPhotoOffsetY,
            tapedFilmPhotoOffsetX = current.tapedFilmPhotoOffsetX,
            tapedFilmPhotoOffsetY = current.tapedFilmPhotoOffsetY,
            stampPhotoZoom = current.stampPhotoZoom,
            polaroidPhotoZoom = current.polaroidPhotoZoom,
            tapedFilmPhotoZoom = current.tapedFilmPhotoZoom
        )
    }

    private fun updatePhotoTransformHistoryAvailability() {
        _canUndoPhotoTransform.value =
            photoTransformUndoStack.isNotEmpty()
        _canRedoPhotoTransform.value =
            photoTransformRedoStack.isNotEmpty()
    }

    private fun clearPhotoTransformHistory() {
        photoTransformUndoStack.clear()
        photoTransformRedoStack.clear()
        updatePhotoTransformHistoryAvailability()
    }

    fun recordPhotoTransformSnapshotForUndo() {
        val snapshot =
            currentPhotoTransformSnapshot() ?: return

        photoTransformUndoStack.addLast(snapshot)
        if (photoTransformUndoStack.size > PHOTO_TRANSFORM_HISTORY_LIMIT) {
            photoTransformUndoStack.removeFirst()
        }
        photoTransformRedoStack.clear()
        updatePhotoTransformHistoryAvailability()
    }

    private fun applyPhotoTransformSnapshot(
        snapshot: PhotoTransformSnapshot
    ) {
        val current =
            _postcard.value ?: return

        if (current.stampPhotoScale != snapshot.stampPhotoScale) {
            saveStampPhotoScale(snapshot.stampPhotoScale)
        }
        if (current.polaroidPhotoScale != snapshot.polaroidPhotoScale) {
            savePolaroidPhotoScale(snapshot.polaroidPhotoScale)
        }
        if (
            current.stampPhotoOffsetX != snapshot.stampPhotoOffsetX ||
            current.stampPhotoOffsetY != snapshot.stampPhotoOffsetY
        ) {
            saveStampPhotoOffset(
                snapshot.stampPhotoOffsetX,
                snapshot.stampPhotoOffsetY
            )
        }
        if (
            current.polaroidPhotoOffsetX != snapshot.polaroidPhotoOffsetX ||
            current.polaroidPhotoOffsetY != snapshot.polaroidPhotoOffsetY
        ) {
            savePolaroidPhotoOffset(
                snapshot.polaroidPhotoOffsetX,
                snapshot.polaroidPhotoOffsetY
            )
        }
        if (
            current.tapedFilmPhotoOffsetX != snapshot.tapedFilmPhotoOffsetX ||
            current.tapedFilmPhotoOffsetY != snapshot.tapedFilmPhotoOffsetY
        ) {
            saveTapedFilmPhotoOffset(
                snapshot.tapedFilmPhotoOffsetX,
                snapshot.tapedFilmPhotoOffsetY
            )
        }
        if (current.stampPhotoZoom != snapshot.stampPhotoZoom) {
            saveStampPhotoZoom(snapshot.stampPhotoZoom)
        }
        if (current.polaroidPhotoZoom != snapshot.polaroidPhotoZoom) {
            savePolaroidPhotoZoom(snapshot.polaroidPhotoZoom)
        }
        if (current.tapedFilmPhotoZoom != snapshot.tapedFilmPhotoZoom) {
            saveTapedFilmPhotoZoom(snapshot.tapedFilmPhotoZoom)
        }
    }

    fun undoPhotoTransformChange() {
        val previous =
            photoTransformUndoStack.removeLastOrNull() ?: return
        val current =
            currentPhotoTransformSnapshot()

        if (current != null) {
            photoTransformRedoStack.addLast(current)
        }

        applyPhotoTransformSnapshot(previous)
        updatePhotoTransformHistoryAvailability()
    }

    fun redoPhotoTransformChange() {
        val next =
            photoTransformRedoStack.removeLastOrNull() ?: return
        val current =
            currentPhotoTransformSnapshot()

        if (current != null) {
            photoTransformUndoStack.addLast(current)
        }

        applyPhotoTransformSnapshot(next)
        updatePhotoTransformHistoryAvailability()
    }

    private var subjectSegmenter: SubjectSegmenter? =
        null

    fun loadPostcard(
        postcardId: Long
    ) {
        clearPhotoTransformHistory()

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

    fun setMessageTextScalePreview(
        scale: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                messageTextScale =
                    scale.coerceIn(0.6f, 1.4f)
            )
    }

    fun saveMessageTextScale(
        scale: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousScale =
            currentPostcard.messageTextScale
        val normalizedScale =
            scale.coerceIn(0.6f, 1.4f)

        _postcard.value =
            currentPostcard.copy(
                messageTextScale = normalizedScale
            )

        messageTextScaleSaveJob?.cancel()
        messageTextScaleSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardMessageTextScale(
                                id = currentPostcard.id,
                                messageTextScale =
                                    normalizedScale
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            messageTextScale = normalizedScale
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            messageTextScale = previousScale
                        )
                    _textScaleSaveErrors.trySend(
                        "글귀 크기를 저장하지 못했어."
                    )
                }
            }
    }

    fun setDateTextScalePreview(
        scale: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                dateTextScale =
                    scale.coerceIn(0.6f, 1.8f)
            )
    }

    fun saveDateTextScale(
        scale: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousScale =
            currentPostcard.dateTextScale
        val normalizedScale =
            scale.coerceIn(0.6f, 1.8f)

        _postcard.value =
            currentPostcard.copy(
                dateTextScale = normalizedScale
            )

        dateTextScaleSaveJob?.cancel()
        dateTextScaleSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardDateTextScale(
                                id = currentPostcard.id,
                                dateTextScale =
                                    normalizedScale
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            dateTextScale = normalizedScale
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            dateTextScale = previousScale
                        )
                    _textScaleSaveErrors.trySend(
                        "날짜 크기를 저장하지 못했어."
                    )
                }
            }
    }

    fun setBackgroundPatternDensityPreview(
        density: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                backgroundPatternDensity =
                    density.coerceIn(0.7f, 1.5f)
            )
    }

    fun saveBackgroundPatternDensity(
        density: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousDensity =
            currentPostcard.backgroundPatternDensity
        val normalizedDensity =
            density.coerceIn(0.7f, 1.5f)

        _postcard.value =
            currentPostcard.copy(
                backgroundPatternDensity = normalizedDensity
            )

        backgroundPatternDensitySaveJob?.cancel()
        backgroundPatternDensitySaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardBackgroundPatternDensity(
                                id = currentPostcard.id,
                                backgroundPatternDensity =
                                    normalizedDensity
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            backgroundPatternDensity =
                                normalizedDensity
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            backgroundPatternDensity =
                                previousDensity
                        )
                    _textScaleSaveErrors.trySend(
                        "무늬 세기를 저장하지 못했어."
                    )
                }
            }
    }

    fun setStampPhotoScalePreview(
        scale: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                stampPhotoScale =
                    scale.coerceIn(0.7f, 1.3f)
            )
    }

    fun saveStampPhotoScale(
        scale: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousScale =
            currentPostcard.stampPhotoScale
        val normalizedScale =
            scale.coerceIn(0.7f, 1.3f)

        _postcard.value =
            currentPostcard.copy(
                stampPhotoScale = normalizedScale
            )

        stampPhotoScaleSaveJob?.cancel()
        stampPhotoScaleSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardStampPhotoScale(
                                id = currentPostcard.id,
                                stampPhotoScale =
                                    normalizedScale
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            stampPhotoScale = normalizedScale
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            stampPhotoScale = previousScale
                        )
                    _textScaleSaveErrors.trySend(
                        "사진 크기를 저장하지 못했어."
                    )
                }
            }
    }

    fun setPolaroidPhotoScalePreview(
        scale: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                polaroidPhotoScale =
                    scale.coerceIn(0.75f, 1.05f)
            )
    }

    fun savePolaroidPhotoScale(
        scale: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousScale =
            currentPostcard.polaroidPhotoScale
        val normalizedScale =
            scale.coerceIn(0.75f, 1.05f)

        _postcard.value =
            currentPostcard.copy(
                polaroidPhotoScale = normalizedScale
            )

        polaroidPhotoScaleSaveJob?.cancel()
        polaroidPhotoScaleSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardPolaroidPhotoScale(
                                id = currentPostcard.id,
                                polaroidPhotoScale =
                                    normalizedScale
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            polaroidPhotoScale = normalizedScale
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            polaroidPhotoScale = previousScale
                        )
                    _textScaleSaveErrors.trySend(
                        "사진 크기를 저장하지 못했어."
                    )
                }
            }
    }

    fun setPhotoEdgeBlurPreview(
        edgeBlur: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                photoEdgeBlur =
                    edgeBlur.coerceIn(0f, 1f)
            )
    }

    fun savePhotoEdgeBlur(
        edgeBlur: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousEdgeBlur =
            currentPostcard.photoEdgeBlur
        val normalizedEdgeBlur =
            edgeBlur.coerceIn(0f, 1f)

        _postcard.value =
            currentPostcard.copy(
                photoEdgeBlur = normalizedEdgeBlur
            )

        photoEdgeBlurSaveJob?.cancel()
        photoEdgeBlurSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardPhotoEdgeBlur(
                                id = currentPostcard.id,
                                photoEdgeBlur =
                                    normalizedEdgeBlur
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            photoEdgeBlur = normalizedEdgeBlur
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            photoEdgeBlur = previousEdgeBlur
                        )
                    _textScaleSaveErrors.trySend(
                        "가장자리 블러를 저장하지 못했어."
                    )
                }
            }
    }

    fun setStampPhotoOffsetPreview(
        offsetX: Float,
        offsetY: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                stampPhotoOffsetX = offsetX.coerceIn(-1f, 1f),
                stampPhotoOffsetY = offsetY.coerceIn(-1f, 1f)
            )
    }

    fun saveStampPhotoOffset(
        offsetX: Float,
        offsetY: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousOffsetX =
            currentPostcard.stampPhotoOffsetX
        val previousOffsetY =
            currentPostcard.stampPhotoOffsetY
        val normalizedOffsetX =
            offsetX.coerceIn(-1f, 1f)
        val normalizedOffsetY =
            offsetY.coerceIn(-1f, 1f)

        _postcard.value =
            currentPostcard.copy(
                stampPhotoOffsetX = normalizedOffsetX,
                stampPhotoOffsetY = normalizedOffsetY
            )

        stampPhotoOffsetSaveJob?.cancel()
        stampPhotoOffsetSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardStampPhotoOffset(
                                id = currentPostcard.id,
                                stampPhotoOffsetX = normalizedOffsetX,
                                stampPhotoOffsetY = normalizedOffsetY
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            stampPhotoOffsetX = normalizedOffsetX,
                            stampPhotoOffsetY = normalizedOffsetY
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            stampPhotoOffsetX = previousOffsetX,
                            stampPhotoOffsetY = previousOffsetY
                        )
                    _textScaleSaveErrors.trySend(
                        "사진 위치를 저장하지 못했어."
                    )
                }
            }
    }

    fun setPolaroidPhotoOffsetPreview(
        offsetX: Float,
        offsetY: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                polaroidPhotoOffsetX = offsetX.coerceIn(-1f, 1f),
                polaroidPhotoOffsetY = offsetY.coerceIn(-1f, 1f)
            )
    }

    fun savePolaroidPhotoOffset(
        offsetX: Float,
        offsetY: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousOffsetX =
            currentPostcard.polaroidPhotoOffsetX
        val previousOffsetY =
            currentPostcard.polaroidPhotoOffsetY
        val normalizedOffsetX =
            offsetX.coerceIn(-1f, 1f)
        val normalizedOffsetY =
            offsetY.coerceIn(-1f, 1f)

        _postcard.value =
            currentPostcard.copy(
                polaroidPhotoOffsetX = normalizedOffsetX,
                polaroidPhotoOffsetY = normalizedOffsetY
            )

        polaroidPhotoOffsetSaveJob?.cancel()
        polaroidPhotoOffsetSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardPolaroidPhotoOffset(
                                id = currentPostcard.id,
                                polaroidPhotoOffsetX = normalizedOffsetX,
                                polaroidPhotoOffsetY = normalizedOffsetY
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            polaroidPhotoOffsetX = normalizedOffsetX,
                            polaroidPhotoOffsetY = normalizedOffsetY
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            polaroidPhotoOffsetX = previousOffsetX,
                            polaroidPhotoOffsetY = previousOffsetY
                        )
                    _textScaleSaveErrors.trySend(
                        "사진 위치를 저장하지 못했어."
                    )
                }
            }
    }

    fun setTapedFilmPhotoOffsetPreview(
        offsetX: Float,
        offsetY: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                tapedFilmPhotoOffsetX = offsetX.coerceIn(-1f, 1f),
                tapedFilmPhotoOffsetY = offsetY.coerceIn(-1f, 1f)
            )
    }

    fun saveTapedFilmPhotoOffset(
        offsetX: Float,
        offsetY: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousOffsetX =
            currentPostcard.tapedFilmPhotoOffsetX
        val previousOffsetY =
            currentPostcard.tapedFilmPhotoOffsetY
        val normalizedOffsetX =
            offsetX.coerceIn(-1f, 1f)
        val normalizedOffsetY =
            offsetY.coerceIn(-1f, 1f)

        _postcard.value =
            currentPostcard.copy(
                tapedFilmPhotoOffsetX = normalizedOffsetX,
                tapedFilmPhotoOffsetY = normalizedOffsetY
            )

        tapedFilmPhotoOffsetSaveJob?.cancel()
        tapedFilmPhotoOffsetSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardTapedFilmPhotoOffset(
                                id = currentPostcard.id,
                                tapedFilmPhotoOffsetX = normalizedOffsetX,
                                tapedFilmPhotoOffsetY = normalizedOffsetY
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            tapedFilmPhotoOffsetX = normalizedOffsetX,
                            tapedFilmPhotoOffsetY = normalizedOffsetY
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            tapedFilmPhotoOffsetX = previousOffsetX,
                            tapedFilmPhotoOffsetY = previousOffsetY
                        )
                    _textScaleSaveErrors.trySend(
                        "사진 위치를 저장하지 못했어."
                    )
                }
            }
    }

    fun setStampPhotoZoomPreview(
        zoom: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                stampPhotoZoom = zoom.coerceIn(1f, 3f)
            )
    }

    fun saveStampPhotoZoom(
        zoom: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousZoom =
            currentPostcard.stampPhotoZoom
        val normalizedZoom =
            zoom.coerceIn(1f, 3f)

        _postcard.value =
            currentPostcard.copy(
                stampPhotoZoom = normalizedZoom
            )

        stampPhotoZoomSaveJob?.cancel()
        stampPhotoZoomSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardStampPhotoZoom(
                                id = currentPostcard.id,
                                stampPhotoZoom = normalizedZoom
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            stampPhotoZoom = normalizedZoom
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            stampPhotoZoom = previousZoom
                        )
                    _textScaleSaveErrors.trySend(
                        "사진 확대 배율을 저장하지 못했어."
                    )
                }
            }
    }

    fun setPolaroidPhotoZoomPreview(
        zoom: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                polaroidPhotoZoom = zoom.coerceIn(1f, 3f)
            )
    }

    fun savePolaroidPhotoZoom(
        zoom: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousZoom =
            currentPostcard.polaroidPhotoZoom
        val normalizedZoom =
            zoom.coerceIn(1f, 3f)

        _postcard.value =
            currentPostcard.copy(
                polaroidPhotoZoom = normalizedZoom
            )

        polaroidPhotoZoomSaveJob?.cancel()
        polaroidPhotoZoomSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardPolaroidPhotoZoom(
                                id = currentPostcard.id,
                                polaroidPhotoZoom = normalizedZoom
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            polaroidPhotoZoom = normalizedZoom
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            polaroidPhotoZoom = previousZoom
                        )
                    _textScaleSaveErrors.trySend(
                        "사진 확대 배율을 저장하지 못했어."
                    )
                }
            }
    }

    fun setTapedFilmPhotoZoomPreview(
        zoom: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        _postcard.value =
            currentPostcard.copy(
                tapedFilmPhotoZoom = zoom.coerceIn(1f, 3f)
            )
    }

    fun saveTapedFilmPhotoZoom(
        zoom: Float
    ) {
        val currentPostcard =
            _postcard.value
                ?: return
        val previousZoom =
            currentPostcard.tapedFilmPhotoZoom
        val normalizedZoom =
            zoom.coerceIn(1f, 3f)

        _postcard.value =
            currentPostcard.copy(
                tapedFilmPhotoZoom = normalizedZoom
            )

        tapedFilmPhotoZoomSaveJob?.cancel()
        tapedFilmPhotoZoomSaveJob =
            viewModelScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repository
                            .updatePostcardTapedFilmPhotoZoom(
                                id = currentPostcard.id,
                                tapedFilmPhotoZoom = normalizedZoom
                            )
                    }

                    _postcard.value =
                        _postcard.value?.copy(
                            tapedFilmPhotoZoom = normalizedZoom
                        )
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _postcard.value =
                        _postcard.value?.copy(
                            tapedFilmPhotoZoom = previousZoom
                        )
                    _textScaleSaveErrors.trySend(
                        "사진 확대 배율을 저장하지 못했어."
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

    fun extractBackgroundColorsFromPhoto() {
        val currentPostcard =
            _postcard.value
                ?: return

        _photoColorExtractionState.value =
            PhotoColorExtractionState.Extracting

        viewModelScope.launch {
            try {
                val colors = withContext(Dispatchers.IO) {
                    PhotoColorExtractor.extractColors(
                        currentPostcard.imagePath
                    )
                }

                _photoColorExtractionState.value =
                    if (colors.isEmpty()) {
                        PhotoColorExtractionState.Error(
                            "사진에서 색을 찾지 못했어."
                        )
                    } else {
                        PhotoColorExtractionState.Success(
                            colors
                        )
                    }
            } catch (exception: Exception) {
                _photoColorExtractionState.value =
                    PhotoColorExtractionState.Error(
                        exception.message
                            ?: "색을 추출하지 못했어."
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

    fun updatePostcardImage(
        sourceUri: Uri
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        if (
            _imageUpdateState.value is
                    ImageUpdateState.Saving
        ) {
            return
        }

        _imageUpdateState.value =
            ImageUpdateState.Saving

        viewModelScope.launch {
            var newImagePath: String? =
                null

            try {
                newImagePath =
                    withContext(Dispatchers.IO) {
                        PostcardImageStorage
                            .copyToAppStorage(
                                context = context,
                                sourceUri = sourceUri
                            )
                    }

                withContext(Dispatchers.IO) {
                    repository.updatePostcardImagePath(
                        id = currentPostcard.id,
                        imagePath = newImagePath
                    )
                }

                _postcard.value =
                    currentPostcard.copy(
                        imagePath = newImagePath
                    )

                _imageUpdateState.value =
                    ImageUpdateState.Success
            } catch (exception: Exception) {
                withContext(Dispatchers.IO) {
                    newImagePath
                        ?.let { path ->
                            val newFile =
                                File(path)

                            if (newFile.exists()) {
                                newFile.delete()
                            }
                        }
                }

                _imageUpdateState.value =
                    ImageUpdateState.Error(
                        exception.message
                            ?: "사진을 바꾸지 못했습니다."
                    )
            }
        }
    }

    fun resetImageUpdateState() {
        _imageUpdateState.value =
            ImageUpdateState.Idle
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

        recordStickerSnapshotForUndo()

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

        if (isStickerFileStillReferenced(uri)) {
            stickerCleanupCandidates.add(uri)
            return
        }

        stickerCleanupCandidates.remove(uri)

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

    fun deleteStickerOriginalIfUnreferenced(
        uri: Uri?,
        remainingStickers: List<PhotoStickerItem>
    ) {
        if (uri != null && isStickerFileStillReferenced(uri)) {
            stickerCleanupCandidates.add(uri)
            return
        }

        if (uri != null) {
            stickerCleanupCandidates.remove(uri)
        }

        val reachableStickers =
            (
                remainingStickers +
                        stickerUndoStack.flatMap { it.stickers } +
                        stickerRedoStack.flatMap { it.stickers }
                ).distinctBy { it.id }

        viewModelScope.launch(Dispatchers.IO) {
            PhotoStickerImageStorage
                .deleteOriginalIfUnreferenced(
                    context = context,
                    deletedUri = uri,
                    remainingStickers = reachableStickers
                )
        }
    }

    fun addCameraPhotoSticker(
        postcardId: Long,
        captureFile: File
    ) {
        recordStickerSnapshotForUndo()

        viewModelScope.launch {
            val originalUri =
                withContext(Dispatchers.IO) {
                    try {
                        PhotoStickerImageStorage
                            .copyToStickerOriginalStorage(
                                context = context,
                                postcardId = postcardId,
                                sourceFile = captureFile
                            )
                    } catch (exception: Exception) {
                        null
                    } finally {
                        if (captureFile.exists()) {
                            captureFile.delete()
                        }
                    }
                }

            if (originalUri != null) {
                val newSticker =
                    PhotoStickerItem(
                        originalUri = originalUri,
                        displayedUri = originalUri
                    )

                _photoStickers.value =
                    _photoStickers.value + newSticker
                _selectedStickerId.value =
                    newSticker.id
                scheduleDraftAutosave()
            } else {
                _textScaleSaveErrors.trySend(
                    "스티커 사진을 저장하지 못했어."
                )
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

        recordStickerSnapshotForUndo()

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
            scheduleDraftAutosave()
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
            scheduleDraftAutosave()
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

        recordStickerSnapshotForUndo()

        _photoStickers.value =
            stickers.toMutableList().apply {
                val temp = this[index]
                this[index] = this[index + 1]
                this[index + 1] = temp
            }
        scheduleDraftAutosave()
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

        recordStickerSnapshotForUndo()

        _photoStickers.value =
            stickers.toMutableList().apply {
                val temp = this[index]
                this[index] = this[index - 1]
                this[index - 1] = temp
            }
        scheduleDraftAutosave()
    }

    fun exportPostcardToGallery(
        stickerOverlays: List<PostcardImageExporter.StickerOverlay> = emptyList(),
        sealOverlays: List<PostcardImageExporter.SealOverlay> = emptyList()
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
                            stickerOverlays = stickerOverlays,
                            sealOverlays = sealOverlays
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

    fun sharePostcard(
        stickerOverlays: List<PostcardImageExporter.StickerOverlay> = emptyList(),
        sealOverlays: List<PostcardImageExporter.SealOverlay> = emptyList()
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        if (
            _shareState.value is
                    ShareState.Preparing
        ) {
            return
        }

        _shareState.value =
            ShareState.Preparing

        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    PostcardImageExporter
                        .exportForSharing(
                            context = context,
                            postcard = currentPostcard,
                            stickerOverlays = stickerOverlays,
                            sealOverlays = sealOverlays
                        )
                }

            result.fold(
                onSuccess = { file ->
                    _shareState.value =
                        ShareState.Ready(file)
                },
                onFailure = { exception ->
                    _shareState.value =
                        ShareState.Error(
                            exception.message
                                ?: "엽서 이미지를 준비하지 못했어요."
                        )
                }
            )
        }
    }

    fun resetShareState() {
        _shareState.value =
            ShareState.Idle
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
                    "seal_states/" +
                            "${currentPostcard.id}.txt"
                ).let { sealFile ->
                    if (sealFile.exists()) {
                        sealFile.delete()
                    }
                }

                PostcardDraftStorage.deleteDraft(
                    context,
                    currentPostcard.id
                )

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
            "CHECKER",
            "STRIPES",
            "WAVES",
            "GRID",
            "CROSSHATCH",
            "SPECKLE" -> backgroundPattern

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
            "STAMP",
            "POLAROID",
            "TAPED_FILM" -> layoutStyle

            else -> "STAMP"
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
