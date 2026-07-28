package com.postcardmemory.ui.detail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
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
import com.postcardmemory.utils.ConfirmedEditStateStorage
import com.postcardmemory.utils.PhotoColorExtractor
import com.postcardmemory.utils.PhotoStickerImageStorage
import com.postcardmemory.utils.PostcardDeletionManager
import com.postcardmemory.utils.PostcardDraftStorage
import com.postcardmemory.utils.PostcardImageExporter
import com.postcardmemory.utils.PostcardImageStorage
import com.postcardmemory.utils.PostcardRenderSpec
import com.postcardmemory.utils.PostcardTemplateStorage
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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "DetailViewModel"
private const val SEAL_HISTORY_LIMIT = 50
private const val STICKER_HISTORY_LIMIT = 30
private const val PHOTO_TRANSFORM_HISTORY_LIMIT = 50
private const val TEMPLATE_STYLE_HISTORY_LIMIT = 50
private const val MAX_TEMPLATE_NAME_LENGTH = 20
private const val TEMPLATE_PREVIEW_SIZE = 320
private const val DRAFT_AUTOSAVE_DEBOUNCE_MS = 900L
private const val PENDING_STYLE_SAVE_TIMEOUT_MS = 2_000L

sealed interface DraftSaveStatus {

    data object Idle : DraftSaveStatus

    data object PendingChanges : DraftSaveStatus

    data object Saving : DraftSaveStatus

    data object Saved : DraftSaveStatus

    data object Failed : DraftSaveStatus
}

/** "현재 꾸밈 저장"(새 사용자 템플릿 생성)의 상태. */
sealed interface TemplateSaveState {

    data object Idle : TemplateSaveState

    data object Saving : TemplateSaveState

    data object Saved : TemplateSaveState

    data class Error(val message: String) : TemplateSaveState
}

/** 이름 변경·덮어쓰기·삭제 등 기존 사용자 템플릿 관리 동작의 상태. */
sealed interface TemplateManageState {

    data object Idle : TemplateManageState

    data object InProgress : TemplateManageState

    data object Success : TemplateManageState

    data class Error(val message: String) : TemplateManageState
}

/** 완료 버튼(확정 저장)의 상태. 스티커·도장 저장을 이 상태로만 판단한다. */
sealed interface ConfirmSaveState {

    data object Idle : ConfirmSaveState

    data object Saving : ConfirmSaveState

    data object Saved : ConfirmSaveState

    data object Failed : ConfirmSaveState
}

/** 스티커·도장 저장이 모두 성공했을 때만 확정 저장 전체를 성공으로 본다. */
internal fun shouldConfirmSaveSucceed(
    stickersSaved: Boolean,
    sealsSaved: Boolean
): Boolean = stickersSaved && sealsSaved

/** 이미 확정 저장이 진행 중이면 완료 버튼 연타로 새 저장을 또 시작하지 않는다. */
internal fun canStartConfirmSave(
    currentState: ConfirmSaveState
): Boolean = currentState !is ConfirmSaveState.Saving

/** 실제로 읽을 수 있는 파일인지(존재+읽기가능+빈 파일 아님) 확인하는 순수 판정. */
internal fun isReadableNonEmptyFile(file: File): Boolean =
    file.exists() && file.canRead() && file.length() > 0L

/**
 * 초안 복원 시 배경제거 상태를 유지할지 판정한다. isBackgroundRemoved=true인데
 * removedBgUri를 실제로 열 수 없다면(파일 유실) 배경제거 상태를 지워야
 * displayedUri/removedBgUri/isBackgroundRemoved가 서로 모순되지 않는다.
 */
internal fun shouldClearBackgroundRemoval(
    isBackgroundRemoved: Boolean,
    removedBgUsable: Boolean
): Boolean = isBackgroundRemoved && !removedBgUsable

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

/** 상세 화면 엽서 삭제(3일차 공통 삭제 정책) 상태. */
sealed interface PostcardDeleteState {

    data object Idle : PostcardDeleteState

    data object Deleting : PostcardDeleteState

    data object Deleted : PostcardDeleteState

    data class Error(
        val message: String
    ) : PostcardDeleteState
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
    private val deletionManager: PostcardDeletionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _postcard =
        MutableStateFlow<Postcard?>(null)

    val postcard: StateFlow<Postcard?> =
        _postcard

    private val _deleteState =
        MutableStateFlow<PostcardDeleteState>(PostcardDeleteState.Idle)

    val deleteState: StateFlow<PostcardDeleteState> =
        _deleteState

    /** 실패 Toast 등을 보여준 뒤 다시 삭제를 시도할 수 있도록 Idle로 되돌린다. */
    fun acknowledgeDeleteError() {
        if (_deleteState.value is PostcardDeleteState.Error) {
            _deleteState.value = PostcardDeleteState.Idle
        }
    }

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

    /**
     * 개별 스타일 저장(위 12개 Job + Job이 없는 layoutStyle/backgroundColorArgb/
     * backgroundPattern/messageFont/dateFormat)과 템플릿 일괄 저장
     * (persistTemplateStyle)의 실제 DAO 쓰기 구간을 직렬화한다. 각 저장은 이
     * Mutex를 획득한 시점에 _postcard.value에서 자신이 쓸 값을 다시 읽어서
     * DAO에 넘기므로(호출 시점에 캡처해둔 값이 아니라), 완료 순서가 뒤바뀌어도
     * 가장 나중에 커밋하는 저장이 항상 그 순간의 실제 화면 상태를 그대로
     * 쓰게 된다 — 개별 저장과 템플릿 일괄 저장 중 어느 쪽이 사용자의 시간상
     * 마지막 조작이었는지와 무관하게, "다시 읽기 + 직렬화"만으로 항상 최신
     * 조작이 최종 Room 상태가 된다. Mutex 없이 다시 읽기만 하면 읽기와
     * 커밋 사이의 시간차 때문에 오래된 읽기가 나중에 커밋되며 다시 역전될
     * 수 있어 두 가지를 함께 써야 한다.
     */
    private val styleWriteMutex = Mutex()

    // ---- 편집 초안(스티커·도장) 자동저장 ----

    private val _draftSaveStatus =
        MutableStateFlow<DraftSaveStatus>(DraftSaveStatus.Idle)

    val draftSaveStatus: StateFlow<DraftSaveStatus> =
        _draftSaveStatus

    private val _confirmSaveState =
        MutableStateFlow<ConfirmSaveState>(ConfirmSaveState.Idle)

    val confirmSaveState: StateFlow<ConfirmSaveState> =
        _confirmSaveState

    /** 완료 버튼 결과(성공/실패)를 화면이 확인 처리한 뒤 Idle로 되돌린다. */
    fun acknowledgeConfirmSaveResult() {
        _confirmSaveState.value = ConfirmSaveState.Idle
    }

    /** 임시저장 상태가 자동 복원된 순간에만 한 번 발행되는 이벤트(Snackbar 트리거용). */
    private val _draftAutoRestoredEvents =
        Channel<Unit>(Channel.BUFFERED)

    val draftAutoRestoredEvents: Flow<Unit> =
        _draftAutoRestoredEvents.receiveAsFlow()

    private var currentDraftPostcardId: Long = 0L
    private var draftCreatedAtMillis: Long = 0L
    private val draftRevisionCounter = AtomicLong(0L)
    private var latestPersistedDraftRevision: Long = 0L
    private val draftSaveMutex = Mutex()
    private var draftAutosaveJob: Job? = null

    /** "원래대로"를 눌렀을 때 되돌아갈 확정 저장 상태(임시저장 적용 직전 스냅샷). */
    private var confirmedStickersBaseline: List<PhotoStickerItem> = emptyList()
    private var confirmedSealsBaseline: List<PostcardSealItem> = emptyList()

    /**
     * 새 postcardId로 상세 화면에 진입할 때 한 번만 호출한다.
     * 확정 스티커·도장 상태를 먼저 로드한 뒤, 저장하지 않은 임시저장이
     * 있으면 그 위에 자동으로 덮어 적용한다. 사용자 확인은 요구하지 않는다.
     *
     * 이미 같은 postcardId로 초기화된 세션이면 아무 것도 하지 않는다 —
     * 화면 회전 등으로 Activity가 재생성돼 LaunchedEffect가 다시 실행돼도
     * (ViewModel 자체는 살아남으므로) 확정 상태를 다시 덮어써 진행 중인
     * 편집을 잃거나 복원 Snackbar가 중복 표시되는 일이 없게 한다.
     */
    fun loadStickerSealStateAndAutoRestoreDraft(postcardId: Long) {
        if (currentDraftPostcardId == postcardId) {
            return
        }

        currentDraftPostcardId = postcardId
        draftAutosaveJob?.cancel()
        draftRevisionCounter.set(0L)
        latestPersistedDraftRevision = 0L
        _draftSaveStatus.value = DraftSaveStatus.Idle
        confirmedStickersBaseline = emptyList()
        confirmedSealsBaseline = emptyList()

        clearStickerHistory()
        clearSealHistory()

        viewModelScope.launch(Dispatchers.IO) {
            val confirmedStickers = readConfirmedStickerState(postcardId)
            val confirmedSeals = readConfirmedSealState(postcardId)

            withContext(Dispatchers.Main) {
                _photoStickers.value = confirmedStickers
                _selectedStickerId.value = null
                _photoSeals.value = confirmedSeals
                _selectedSealId.value = null
            }

            confirmedStickersBaseline = confirmedStickers
            confirmedSealsBaseline = confirmedSeals

            val existingDraft =
                PostcardDraftStorage.loadDraft(context, postcardId)

            if (existingDraft != null && existingDraft.postcardId == postcardId) {
                draftCreatedAtMillis = existingDraft.createdAtMillis
                draftRevisionCounter.set(existingDraft.revision)
                latestPersistedDraftRevision = existingDraft.revision

                val restoredStickers =
                    validateAndRepairRestoredStickers(existingDraft.stickers)

                withContext(Dispatchers.Main) {
                    _photoStickers.value = restoredStickers
                    _selectedStickerId.value = existingDraft.selectedStickerId
                    _photoSeals.value = existingDraft.seals
                    _selectedSealId.value = existingDraft.selectedSealId
                }

                _draftAutoRestoredEvents.trySend(Unit)
            } else {
                draftCreatedAtMillis = System.currentTimeMillis()
            }
        }
    }

    /**
     * 초안에 기록된 URI가 실제로 열 수 있는 파일/콘텐츠를 가리키는지 검증하고
     * 모순된 상태를 복구한다. 원본(originalUri)까지 열 수 없는 스티커는
     * 복원 목록에서 제외한다(깨진 이미지를 화면에 남기지 않기 위함) — 손상된
     * 스티커 하나 때문에 나머지 스티커의 복원이 실패하지는 않는다.
     */
    private fun validateAndRepairRestoredStickers(
        stickers: List<PhotoStickerItem>
    ): List<PhotoStickerItem> {
        return stickers.mapNotNull { sticker ->
            if (!isUriUsable(sticker.originalUri)) {
                Log.w(
                    TAG,
                    "초안 복원 제외(원본 유실): stickerId=${sticker.id}"
                )
                return@mapNotNull null
            }

            val removedBgUsable =
                sticker.removedBgUri?.let { isUriUsable(it) } ?: false

            if (
                shouldClearBackgroundRemoval(
                    isBackgroundRemoved = sticker.isBackgroundRemoved,
                    removedBgUsable = removedBgUsable
                )
            ) {
                Log.w(
                    TAG,
                    "초안 복원 중 누끼 파일 유실, 원본으로 복구: " +
                            "stickerId=${sticker.id}"
                )
                sticker.copy(
                    displayedUri = sticker.originalUri,
                    removedBgUri = null,
                    isBackgroundRemoved = false
                )
            } else {
                sticker
            }
        }
    }

    private fun isUriUsable(uri: Uri): Boolean {
        if (uri.scheme == "file") {
            val path = uri.path ?: return false
            return isReadableNonEmptyFile(File(path))
        }

        return runCatching {
            context.contentResolver
                .openInputStream(uri)
                ?.use { true }
                ?: false
        }.getOrDefault(false)
    }

    /**
     * Snackbar의 "원래대로"를 눌렀을 때 호출한다. 임시저장을 폐기하고 확정
     * 상태로 되돌리며, 삭제도 revision을 올려 진행 중이던 자동저장이
     * 되살리지 못하게 막는다(saveEditsAndClearDraft와 동일한 보호 방식).
     */
    fun revertToConfirmedState() {
        val postcardId = currentDraftPostcardId
        if (postcardId <= 0L) {
            return
        }

        draftAutosaveJob?.cancel()

        _photoStickers.value = confirmedStickersBaseline
        _selectedStickerId.value = null
        _photoSeals.value = confirmedSealsBaseline
        _selectedSealId.value = null

        clearStickerHistory()
        clearSealHistory()

        viewModelScope.launch(Dispatchers.IO) {
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

    /**
     * 초안을 저장하기 전에 배경제거된 스티커의 누끼 파일을 캐시에서
     * draft_sticker_bgs/<postcardId>/로 승격한다. onCleared()가 화면
     * 이탈 시 cacheDir/photo_stickers를 정리하므로, 이 승격이 없으면
     * 완료 버튼을 누르지 않고 나간 초안이 참조하는 누끼 파일이 다음 진입
     * 전에 사라질 수 있다. 승격에 실패한 스티커가 하나라도 있으면 null을
     * 반환해 이번 초안 저장 전체를 실패로 처리하고, 이미 저장된 초안은
     * 손상시키지 않는다.
     */
    private fun promoteDraftStickerBackgrounds(
        postcardId: Long,
        stickers: List<PhotoStickerItem>
    ): List<PhotoStickerItem>? {
        val stickerCacheDir =
            File(context.cacheDir, "photo_stickers").canonicalFile
        val draftPersistDir =
            PostcardDraftStorage.draftStickerBackgroundDir(context, postcardId)

        if (
            !draftPersistDir.exists() &&
            !draftPersistDir.mkdirs()
        ) {
            return null
        }

        val promoted = mutableListOf<PhotoStickerItem>()
        for (sticker in stickers) {
            val persisted =
                persistStickerBackground(
                    sticker = sticker,
                    stickerCacheDir = stickerCacheDir,
                    persistDir = draftPersistDir,
                    // undo/redo 스택이 아직 캐시 경로를 참조 중일 수 있으니
                    // 초안 자동저장에서는 캐시 원본을 지우지 않는다.
                    deleteCacheSourceAfterCopy = false
                ) ?: return null
            promoted.add(persisted)
        }
        return promoted
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

        var promotedStickers: List<PhotoStickerItem>? = null

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

                val promoted =
                    promoteDraftStickerBackgrounds(
                        postcardId = postcardId,
                        stickers = snapshotStickers
                    ) ?: return@withLock false

                val draft = PostcardEditDraft(
                    postcardId = postcardId,
                    createdAtMillis = draftCreatedAtMillis,
                    updatedAtMillis = System.currentTimeMillis(),
                    revision = candidateRevision,
                    stickers = promoted,
                    selectedStickerId = snapshotSelectedStickerId,
                    seals = snapshotSeals,
                    selectedSealId = snapshotSelectedSealId
                )

                val saved =
                    PostcardDraftStorage.saveDraftAtomically(context, draft)

                if (saved) {
                    latestPersistedDraftRevision = candidateRevision
                    promotedStickers = promoted
                }

                saved
            }
        }

        // 승격된 URI를 실제 화면 상태에도 반영한다. 단, 승격이 진행되는 동안
        // 사용자가 스티커를 더 편집했다면(스냅샷과 현재 값이 달라졌다면)
        // 되돌아온 결과가 그 편집을 덮어쓰지 않도록 건너뛴다 — 다음 자동저장
        // 사이클이 최신 상태로 다시 승격을 시도한다.
        val promoted = promotedStickers
        if (
            success &&
            promoted != null &&
            _photoStickers.value == snapshotStickers
        ) {
            _photoStickers.value = promoted
        }

        _draftSaveStatus.value =
            if (success) DraftSaveStatus.Saved else DraftSaveStatus.Failed
    }

    /**
     * 확정 저장(완료 버튼)이 스티커·도장 모두 실제로 성공한 뒤에만 초안을
     * 지운다. 하나라도 실패하면 초안을 유지해 사용자가 다시 시도할 수 있게
     * 한다. 이미 저장이 진행 중이면 새 요청은 무시해 완료 버튼 연타로
     * 같은 파일에 저장이 중복 실행되는 것을 막는다.
     *
     * 초안 삭제 자체의 실패(드문 파일시스템 오류)는 정리 실패로만 취급하고
     * 전체 결과는 성공으로 본다 — 이 시점엔 스티커·도장 확정 상태가 이미
     * 안전하게 저장됐고, deleteDraft는 예외를 던지지 않으므로 재시도 시
     * 자동저장이 다음 임시저장에서 초안 파일을 다시 갱신해 자연히 해소된다.
     */
    fun saveEditsAndClearDraft(postcardId: Long) {
        if (!canStartConfirmSave(_confirmSaveState.value)) {
            return
        }

        draftAutosaveJob?.cancel()
        _confirmSaveState.value = ConfirmSaveState.Saving

        viewModelScope.launch(Dispatchers.IO) {
            val stickersSaved = persistStickerEditState(postcardId)
            val sealsSaved = persistSealEditState(postcardId)
            val allSaved = shouldConfirmSaveSucceed(stickersSaved, sealsSaved)

            if (allSaved) {
                draftSaveMutex.withLock {
                    latestPersistedDraftRevision =
                        draftRevisionCounter.incrementAndGet()
                    PostcardDraftStorage.deleteDraft(context, postcardId)
                }
            }

            withContext(Dispatchers.Main) {
                if (allSaved) {
                    _draftSaveStatus.value = DraftSaveStatus.Idle
                }
                _confirmSaveState.value =
                    if (allSaved) {
                        ConfirmSaveState.Saved
                    } else {
                        ConfirmSaveState.Failed
                    }
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

    /**
     * 스티커 확정 상태를 저장한다. 누끼 파일 승격이나 상태 파일 쓰기 중
     * 하나라도 실패하면 false를 반환하며, 이 경우 StateFlow와 undo/redo
     * 이력, 기존 확정 파일 모두 건드리지 않아 재시도가 안전하다.
     */
    private suspend fun persistStickerEditState(
        postcardId: Long
    ): Boolean {
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
            return false
        }

        val updatedStickers = mutableListOf<PhotoStickerItem>()
        for (sticker in _photoStickers.value) {
            val persisted =
                persistStickerBackground(
                    sticker = sticker,
                    stickerCacheDir = stickerCacheDir,
                    persistDir = persistDir
                ) ?: return false
            updatedStickers.add(persisted)
        }

        val stateFile =
            File(context.filesDir, "sticker_states/$postcardId.txt")
        val saved =
            ConfirmedEditStateStorage.writeTextAtomically(
                targetFile = stateFile,
                content = updatedStickers.joinToString("\n") {
                    it.serialize()
                }
            )

        if (!saved) {
            return false
        }

        if (updatedStickers != _photoStickers.value) {
            _photoStickers.value = updatedStickers
        }
        clearStickerHistory()

        return true
    }

    /** 확정 저장된 스티커 상태만 읽어 반환한다(StateFlow는 건드리지 않음). */
    private suspend fun readConfirmedStickerState(
        postcardId: Long
    ): List<PhotoStickerItem> {
        val file =
            File(
                context.filesDir,
                "sticker_states/$postcardId.txt"
            )
        if (!file.exists()) return emptyList()

        val persistDir =
            File(
                context.filesDir,
                "sticker_bgs/$postcardId"
            )

        return file.readLines()
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

    /** 도장 확정 상태를 원자적으로 저장한다. 실패 시 기존 확정 파일은 그대로 유지된다. */
    private suspend fun persistSealEditState(
        postcardId: Long
    ): Boolean {
        val stateFile =
            File(context.filesDir, "seal_states/$postcardId.txt")

        return ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = stateFile,
            content = _photoSeals.value.joinToString("\n") {
                it.serialize()
            }
        )
    }

    /** 확정 저장된 도장 상태만 읽어 반환한다(StateFlow는 건드리지 않음). */
    private suspend fun readConfirmedSealState(
        postcardId: Long
    ): List<PostcardSealItem> {
        val file =
            File(
                context.filesDir,
                "seal_states/$postcardId.txt"
            )
        if (!file.exists()) return emptyList()

        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                deserializePostcardSealItem(line)
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

    /**
     * 템플릿 적용 한 번을 되돌릴 수 있는 스냅샷. seals가 null이면 이 템플릿
     * 적용이 도장을 건드리지 않았다는 뜻이라 되돌릴 때도 도장은 그대로 둔다
     * (기존 sealUndoStack과는 완전히 독립적 — 스티커·도장 자체 Undo는 건드리지 않는다).
     */
    private data class TemplateApplicationSnapshot(
        val style: PostcardTemplateStyle,
        val seals: List<PostcardSealItem>?
    )

    private val templateStyleUndoStack =
        ArrayDeque<TemplateApplicationSnapshot>()

    private val templateStyleRedoStack =
        ArrayDeque<TemplateApplicationSnapshot>()

    private val _canUndoTemplateStyle =
        MutableStateFlow(false)

    val canUndoTemplateStyle: StateFlow<Boolean> =
        _canUndoTemplateStyle

    private val _canRedoTemplateStyle =
        MutableStateFlow(false)

    val canRedoTemplateStyle: StateFlow<Boolean> =
        _canRedoTemplateStyle

    private fun updateTemplateStyleHistoryAvailability() {
        _canUndoTemplateStyle.value =
            templateStyleUndoStack.isNotEmpty()
        _canRedoTemplateStyle.value =
            templateStyleRedoStack.isNotEmpty()
    }

    private fun clearTemplateStyleHistory() {
        templateStyleUndoStack.clear()
        templateStyleRedoStack.clear()
        updateTemplateStyleHistoryAvailability()
    }

    /**
     * 현재 "선택됨"으로 표시할 템플릿 id. DetailScreen의 Compose 로컬 상태가
     * 아니라 여기서 관리하는 이유: 저장 실패 롤백 시 스타일·Undo/Redo 스택과
     * 같은 트랜잭션(같은 onSaveFailed 콜백) 안에서 함께 되돌려야, 실패한
     * 적용이 선택된 것처럼 보이거나 오래된 실패가 그 사이에 사용자가 고른
     * 최신 선택을 덮어쓰는 일이 없다.
     */
    private val _lastAppliedTemplateId =
        MutableStateFlow<String?>(null)

    val lastAppliedTemplateId: StateFlow<String?> =
        _lastAppliedTemplateId

    private fun applyTemplateApplicationSnapshot(
        snapshot: TemplateApplicationSnapshot,
        onSaveFailed: () -> Unit
    ) {
        val currentPostcard =
            _postcard.value ?: return
        val previousStyle =
            currentPostcard.toTemplateStyle()

        persistTemplateStyle(
            postcardId = currentPostcard.id,
            style = snapshot.style,
            previousStyle = previousStyle,
            onSaveFailed = onSaveFailed
        )

        if (snapshot.seals != null) {
            // setPhotoSeals()/undoSealChange()와 동일하게 초안 자동저장도
            // 함께 트리거해야 한다 — 그렇지 않으면 템플릿 Undo/Redo로 되돌린
            // 도장이 인메모리·화면에서는 사라졌는데 크래시 복구용 초안
            // 파일(PostcardDraftStorage)에는 예전 도장이 남아, "완료" 버튼을
            // 누르기 전에 앱이 종료되면 재실행 시 되돌렸던 도장이 복구
            // 제안으로 다시 나타날 수 있다.
            _photoSeals.value = snapshot.seals
            scheduleDraftAutosave()
        }
    }

    fun undoTemplateStyleChange() {
        if (templateStyleUndoStack.isEmpty()) return
        val currentPostcard =
            _postcard.value ?: return

        val undoStackBeforeExecution =
            ArrayDeque(templateStyleUndoStack)
        val redoStackBeforeExecution =
            ArrayDeque(templateStyleRedoStack)
        val sealsBeforeExecution =
            _photoSeals.value

        val previous =
            templateStyleUndoStack.removeLastOrNull() ?: return

        templateStyleRedoStack.addLast(
            TemplateApplicationSnapshot(
                style = currentPostcard.toTemplateStyle(),
                seals =
                    if (previous.seals != null) {
                        _photoSeals.value
                    } else {
                        null
                    }
            )
        )

        applyTemplateApplicationSnapshot(
            snapshot = previous,
            onSaveFailed = {
                templateStyleUndoStack.clear()
                templateStyleUndoStack.addAll(undoStackBeforeExecution)
                templateStyleRedoStack.clear()
                templateStyleRedoStack.addAll(redoStackBeforeExecution)
                updateTemplateStyleHistoryAvailability()

                if (previous.seals != null) {
                    _photoSeals.value = sealsBeforeExecution
                    scheduleDraftAutosave()
                }
            }
        )
        updateTemplateStyleHistoryAvailability()
    }

    fun redoTemplateStyleChange() {
        if (templateStyleRedoStack.isEmpty()) return
        val currentPostcard =
            _postcard.value ?: return

        val undoStackBeforeExecution =
            ArrayDeque(templateStyleUndoStack)
        val redoStackBeforeExecution =
            ArrayDeque(templateStyleRedoStack)
        val sealsBeforeExecution =
            _photoSeals.value

        val next =
            templateStyleRedoStack.removeLastOrNull() ?: return

        templateStyleUndoStack.addLast(
            TemplateApplicationSnapshot(
                style = currentPostcard.toTemplateStyle(),
                seals =
                    if (next.seals != null) {
                        _photoSeals.value
                    } else {
                        null
                    }
            )
        )

        applyTemplateApplicationSnapshot(
            snapshot = next,
            onSaveFailed = {
                templateStyleUndoStack.clear()
                templateStyleUndoStack.addAll(undoStackBeforeExecution)
                templateStyleRedoStack.clear()
                templateStyleRedoStack.addAll(redoStackBeforeExecution)
                updateTemplateStyleHistoryAvailability()

                if (next.seals != null) {
                    _photoSeals.value = sealsBeforeExecution
                    scheduleDraftAutosave()
                }
            }
        )
        updateTemplateStyleHistoryAvailability()
    }

    /**
     * 템플릿을 적용한다. 사진·문구·날짜·스티커·기존 도장은 그대로 두고
     * 스타일 값만 바꾼다. 도장은 현재 엽서에 도장이 하나도 없을 때만 템플릿
     * 도장을 추가하고(기존 도장이 있으면 템플릿 도장은 무시, 최대 개수 위반
     * 불가능), 적용 전 상태를 스냅샷 하나로 남겨 Undo 한 번으로 스타일과
     * (추가됐다면) 도장까지 함께 되돌릴 수 있다.
     */
    fun applyTemplate(
        template: PostcardTemplate
    ) {
        val currentPostcard =
            _postcard.value ?: return

        val willAddSeal =
            template.seal != null && _photoSeals.value.isEmpty()
        val previousStyle =
            currentPostcard.toTemplateStyle()
        val previousSeals =
            _photoSeals.value
        val previousTemplateId =
            _lastAppliedTemplateId.value
        val undoStackBeforeExecution =
            ArrayDeque(templateStyleUndoStack)
        val redoStackBeforeExecution =
            ArrayDeque(templateStyleRedoStack)

        templateStyleUndoStack.addLast(
            TemplateApplicationSnapshot(
                style = previousStyle,
                seals = if (willAddSeal) previousSeals else null
            )
        )
        if (templateStyleUndoStack.size > TEMPLATE_STYLE_HISTORY_LIMIT) {
            templateStyleUndoStack.removeFirst()
        }
        templateStyleRedoStack.clear()
        updateTemplateStyleHistoryAvailability()

        _lastAppliedTemplateId.value = template.id

        if (willAddSeal) {
            val templateSeal = template.seal!!
            setPhotoSeals(
                listOf(
                    PostcardSealItem(
                        type = templateSeal.type,
                        scale = templateSeal.type.defaultScale,
                        colorArgb = templateSeal.colorArgb
                    )
                )
            )
        }

        persistTemplateStyle(
            postcardId = currentPostcard.id,
            style = template.style,
            previousStyle = previousStyle,
            onSaveFailed = {
                templateStyleUndoStack.clear()
                templateStyleUndoStack.addAll(undoStackBeforeExecution)
                templateStyleRedoStack.clear()
                templateStyleRedoStack.addAll(redoStackBeforeExecution)
                updateTemplateStyleHistoryAvailability()

                _lastAppliedTemplateId.value = previousTemplateId

                if (willAddSeal) {
                    _photoSeals.value = previousSeals
                    scheduleDraftAutosave()
                }
            }
        )
    }

    /**
     * 템플릿 스타일 저장 전용 job. 슬라이더 저장 함수들(saveStampPhotoScale
     * 등)과 동일하게, 새 저장을 시작하기 전 이전 저장을 취소한다 — 그렇지
     * 않으면 템플릿을 빠르게 연속 적용(A→B→C)했을 때 Dispatchers.IO의 실행
     * 순서가 보장되지 않아 나중에 적용한 스타일이 먼저 완료된 이전 스타일에
     * 덮어써질 수 있다. _postcard.value는 항상 동기적으로 최신값이므로
     * 화면에는 영향이 없지만, Room에 저장되는 값이 화면과 달라져 앱을 다시
     * 켰을 때 이전 템플릿으로 되돌아가 보일 수 있었다.
     *
     * 취소(templateStyleSaveJob?.cancel())는 슬라이더 저장 함수들과 동일하게
     * CancellationException을 먼저 rethrow해서 실패로 취급하지 않는다 — 이
     * 덕분에 A→B→C 연속 적용에서 취소된 이전 저장은 자기 catch(Exception)
     * 블록에 도달하지 못해 최신 상태를 롤백할 수 없다(오래된 작업의 롤백
     * 방지는 별도 세대 번호 없이 이 취소·rethrow 구조만으로 보장된다).
     */
    private var templateStyleSaveJob: Job? = null

    private fun persistTemplateStyle(
        postcardId: Long,
        style: PostcardTemplateStyle,
        previousStyle: PostcardTemplateStyle,
        onSaveFailed: () -> Unit
    ) {
        _postcard.value =
            _postcard.value?.applyTemplateStyle(style)

        templateStyleSaveJob?.cancel()
        templateStyleSaveJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    styleWriteMutex.withLock {
                        // 개별 스타일 저장과의 경합 방지: 캡처해둔 style이
                        // 아니라 Mutex를 획득한 이 순간의 _postcard.value를
                        // 다시 읽어서 쓴다 — 그 사이 개별 슬라이더 조작이
                        // 끼어들었다면 그 값이 이미 반영돼 있으므로 함께
                        // 저장되고, 반대로 이 템플릿 저장이 늦게 커밋되더라도
                        // 자신이 밀려난 옛 style로 최신 조작을 덮어쓰지 않는다.
                        val latestStyle =
                            _postcard.value?.toTemplateStyle()
                                ?: return@withLock

                        repository.updatePostcardTemplateStyle(
                            id = postcardId,
                            layoutStyle = latestStyle.layoutStyle,
                            backgroundColorArgb = latestStyle.backgroundColorArgb,
                            backgroundPattern = latestStyle.backgroundPattern,
                            backgroundPatternDensity = latestStyle.backgroundPatternDensity,
                            messageFont = latestStyle.messageFont,
                            dateFormat = latestStyle.dateFormat,
                            messageTextScale = latestStyle.messageTextScale,
                            dateTextScale = latestStyle.dateTextScale,
                            photoEdgeBlur = latestStyle.photoEdgeBlur,
                            stampPhotoScale = latestStyle.stampPhotoScale,
                            stampPhotoOffsetX = latestStyle.stampPhotoOffsetX,
                            stampPhotoOffsetY = latestStyle.stampPhotoOffsetY,
                            stampPhotoZoom = latestStyle.stampPhotoZoom,
                            polaroidPhotoScale = latestStyle.polaroidPhotoScale,
                            polaroidPhotoOffsetX = latestStyle.polaroidPhotoOffsetX,
                            polaroidPhotoOffsetY = latestStyle.polaroidPhotoOffsetY,
                            polaroidPhotoZoom = latestStyle.polaroidPhotoZoom,
                            tapedFilmPhotoOffsetX = latestStyle.tapedFilmPhotoOffsetX,
                            tapedFilmPhotoOffsetY = latestStyle.tapedFilmPhotoOffsetY,
                            tapedFilmPhotoZoom = latestStyle.tapedFilmPhotoZoom
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _postcard.value =
                    _postcard.value?.applyTemplateStyle(previousStyle)
                onSaveFailed()
                _textScaleSaveErrors.trySend(
                    "템플릿 스타일을 저장하지 못했어. 이전 상태로 되돌렸어."
                )
            }
        }
    }

    // ---- 내 템플릿(사용자 템플릿) ----

    private val _userTemplates =
        MutableStateFlow<List<PostcardTemplate>>(emptyList())

    val userTemplates: StateFlow<List<PostcardTemplate>> =
        _userTemplates

    private val _templateSaveState =
        MutableStateFlow<TemplateSaveState>(TemplateSaveState.Idle)

    val templateSaveState: StateFlow<TemplateSaveState> =
        _templateSaveState

    private val _templateManageState =
        MutableStateFlow<TemplateManageState>(TemplateManageState.Idle)

    val templateManageState: StateFlow<TemplateManageState> =
        _templateManageState

    /** 저장된 사용자 템플릿을 filesDir에서 읽어온다. 손상된 파일은 storage가 이미 걸러낸 뒤다. */
    fun loadUserTemplates() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded =
                PostcardTemplateStorage.loadAllTemplates(context)
                    .sortedByDescending { it.updatedAtMillis }

            withContext(Dispatchers.Main) {
                _userTemplates.value = loaded
            }
        }
    }

    /** "나의 템플릿 N" 형태의 기본 이름을 제안한다. 이미 쓰이는 이름과 겹치지 않는 번호를 고른다. */
    fun suggestNewTemplateName(): String {
        val existingNames =
            _userTemplates.value.map { it.name }.toSet()
        var index = _userTemplates.value.size + 1
        var candidate = "나의 템플릿 $index"

        while (candidate in existingNames) {
            index += 1
            candidate = "나의 템플릿 $index"
        }

        return candidate
    }

    fun isTemplateNameDuplicate(name: String): Boolean =
        _userTemplates.value.any {
            it.name == name.trim()
        }

    fun resetTemplateSaveState() {
        _templateSaveState.value = TemplateSaveState.Idle
    }

    fun resetTemplateManageState() {
        _templateManageState.value = TemplateManageState.Idle
    }

    /**
     * 현재 엽서의 스타일 값(+도장이 정확히 하나면 그 도장도)을 새 사용자
     * 템플릿으로 저장한다. 사진 원본·문구·날짜·스티커는 절대 포함하지 않는다.
     * 저장 실패 시 기존에 있던 정상 템플릿 목록은 손대지 않고, 부분적으로
     * 생성됐을 수 있는 파일을 정리한다.
     */
    fun saveCurrentStyleAsNewTemplate(
        name: String
    ) {
        val currentPostcard =
            _postcard.value ?: return

        val trimmedName =
            name.trim().take(MAX_TEMPLATE_NAME_LENGTH)

        if (trimmedName.isBlank()) {
            _templateSaveState.value =
                TemplateSaveState.Error("템플릿 이름을 입력해줘.")
            return
        }

        _templateSaveState.value = TemplateSaveState.Saving

        val style = currentPostcard.toTemplateStyle()
        val currentSeals = _photoSeals.value
        val seal =
            if (currentSeals.size == 1) {
                PostcardTemplateSeal(
                    type = currentSeals[0].type,
                    colorArgb = currentSeals[0].colorArgb
                )
            } else {
                null
            }

        val template =
            PostcardTemplate(
                name = trimmedName,
                style = style,
                seal = seal
            )

        viewModelScope.launch(Dispatchers.IO) {
            val previewBitmap =
                renderTemplatePreviewBitmap(
                    imagePath = currentPostcard.imagePath,
                    style = style
                )

            val templateSaved =
                PostcardTemplateStorage.saveTemplateAtomically(
                    context = context,
                    template = template
                )

            if (!templateSaved) {
                previewBitmap?.recycle()
                withContext(Dispatchers.Main) {
                    _templateSaveState.value =
                        TemplateSaveState.Error(
                            "템플릿을 저장하지 못했어. 잠시 뒤 다시 시도해줘."
                        )
                }
                return@launch
            }

            // 미리보기 생성/저장 실패는 템플릿 자체 저장 실패로 취급하지 않는다 —
            // 카드에서는 항상 현재 엽서 사진으로 실시간 미리보기를 그리므로
            // 저장된 미리보기 파일은 향후를 위한 보조 자산일 뿐이다.
            if (previewBitmap != null) {
                PostcardTemplateStorage.savePreviewAtomically(
                    context = context,
                    templateId = template.id,
                    bitmap = previewBitmap
                )
                previewBitmap.recycle()
            }

            withContext(Dispatchers.Main) {
                _userTemplates.value =
                    (_userTemplates.value + template)
                        .sortedByDescending { it.updatedAtMillis }
                _templateSaveState.value =
                    TemplateSaveState.Saved
            }
        }
    }

    /** 작은 미리보기 전용 렌더. 실패해도 예외를 던지지 않고 null을 반환한다. */
    private fun renderTemplatePreviewBitmap(
        imagePath: String,
        style: PostcardTemplateStyle
    ): Bitmap? =
        runCatching {
            val sourceBitmap =
                PostcardRenderSpec.decodeSourceBitmap(File(imagePath))

            try {
                val previewBitmap =
                    Bitmap.createBitmap(
                        TEMPLATE_PREVIEW_SIZE,
                        TEMPLATE_PREVIEW_SIZE,
                        Bitmap.Config.ARGB_8888
                    )

                PostcardRenderSpec.drawBaseContent(
                    canvas = Canvas(previewBitmap),
                    sourceBitmap = sourceBitmap,
                    backgroundColorArgb = style.backgroundColorArgb,
                    backgroundPattern = style.backgroundPattern,
                    message = "",
                    messageFont = style.messageFont,
                    layoutStyle = style.layoutStyle,
                    capturedAt = System.currentTimeMillis(),
                    dateFormat = style.dateFormat,
                    targetSize = TEMPLATE_PREVIEW_SIZE.toFloat(),
                    messageTextScale = style.messageTextScale,
                    dateTextScale = style.dateTextScale,
                    backgroundPatternDensity = style.backgroundPatternDensity,
                    stampPhotoScale = style.stampPhotoScale,
                    polaroidPhotoScale = style.polaroidPhotoScale,
                    photoEdgeBlur = style.photoEdgeBlur,
                    stampPhotoOffsetX = style.stampPhotoOffsetX,
                    stampPhotoOffsetY = style.stampPhotoOffsetY,
                    polaroidPhotoOffsetX = style.polaroidPhotoOffsetX,
                    polaroidPhotoOffsetY = style.polaroidPhotoOffsetY,
                    tapedFilmPhotoOffsetX = style.tapedFilmPhotoOffsetX,
                    tapedFilmPhotoOffsetY = style.tapedFilmPhotoOffsetY,
                    stampPhotoZoom = style.stampPhotoZoom,
                    polaroidPhotoZoom = style.polaroidPhotoZoom,
                    tapedFilmPhotoZoom = style.tapedFilmPhotoZoom
                )

                previewBitmap
            } finally {
                if (!sourceBitmap.isRecycled) {
                    sourceBitmap.recycle()
                }
            }
        }.getOrNull()

    /**
     * 이름 변경. id·스타일·미리보기·생성 시각은 그대로 두고 이름과 수정
     * 시각만 바꾼다. 내장 템플릿은 _userTemplates에 없으므로 애초에
     * 대상이 될 수 없다.
     */
    fun renameUserTemplate(
        templateId: String,
        newName: String
    ) {
        val target =
            _userTemplates.value.firstOrNull { it.id == templateId }
                ?: run {
                    _templateManageState.value =
                        TemplateManageState.Error(
                            "이미 지워진 템플릿이야. 목록을 새로고침해줘."
                        )
                    return
                }

        val trimmedName =
            newName.trim().take(MAX_TEMPLATE_NAME_LENGTH)

        if (trimmedName.isBlank()) {
            _templateManageState.value =
                TemplateManageState.Error("템플릿 이름을 입력해줘.")
            return
        }

        _templateManageState.value = TemplateManageState.InProgress

        val renamed =
            target.copy(
                name = trimmedName,
                updatedAtMillis = System.currentTimeMillis()
            )

        viewModelScope.launch(Dispatchers.IO) {
            val saved =
                PostcardTemplateStorage.saveTemplateAtomically(
                    context = context,
                    template = renamed
                )

            withContext(Dispatchers.Main) {
                if (saved) {
                    _userTemplates.value =
                        _userTemplates.value
                            .map { if (it.id == templateId) renamed else it }
                            .sortedByDescending { it.updatedAtMillis }
                    _templateManageState.value =
                        TemplateManageState.Success
                } else {
                    _templateManageState.value =
                        TemplateManageState.Error(
                            "이름을 바꾸지 못했어. 기존 템플릿은 그대로야."
                        )
                }
            }
        }
    }

    /**
     * 현재 엽서의 꾸밈으로 덮어쓴다. id·이름·생성 시각은 유지하고 스타일·
     * (있다면)도장·미리보기·수정 시각만 갱신한다. 저장에 실패하면 기존
     * 템플릿 파일은 그대로 남는다(temp+rename 원자적 저장이라 반쯤 쓰인
     * 파일이 기존 파일을 대체하지 않음).
     */
    fun overwriteUserTemplateWithCurrentStyle(
        templateId: String
    ) {
        val target =
            _userTemplates.value.firstOrNull { it.id == templateId }
                ?: run {
                    _templateManageState.value =
                        TemplateManageState.Error(
                            "이미 지워진 템플릿이야. 목록을 새로고침해줘."
                        )
                    return
                }
        val currentPostcard =
            _postcard.value ?: return

        _templateManageState.value = TemplateManageState.InProgress

        val style = currentPostcard.toTemplateStyle()
        val currentSeals = _photoSeals.value
        val seal =
            if (currentSeals.size == 1) {
                PostcardTemplateSeal(
                    type = currentSeals[0].type,
                    colorArgb = currentSeals[0].colorArgb
                )
            } else {
                null
            }

        val updated =
            target.copy(
                style = style,
                seal = seal,
                updatedAtMillis = System.currentTimeMillis()
            )

        viewModelScope.launch(Dispatchers.IO) {
            val previewBitmap =
                renderTemplatePreviewBitmap(
                    imagePath = currentPostcard.imagePath,
                    style = style
                )

            val saved =
                PostcardTemplateStorage.saveTemplateAtomically(
                    context = context,
                    template = updated
                )

            if (!saved) {
                previewBitmap?.recycle()
                withContext(Dispatchers.Main) {
                    _templateManageState.value =
                        TemplateManageState.Error(
                            "템플릿을 덮어쓰지 못했어. 기존 템플릿은 그대로야."
                        )
                }
                return@launch
            }

            if (previewBitmap != null) {
                PostcardTemplateStorage.savePreviewAtomically(
                    context = context,
                    templateId = updated.id,
                    bitmap = previewBitmap
                )
                previewBitmap.recycle()
            }

            withContext(Dispatchers.Main) {
                _userTemplates.value =
                    _userTemplates.value
                        .map { if (it.id == templateId) updated else it }
                        .sortedByDescending { it.updatedAtMillis }
                _templateManageState.value =
                    TemplateManageState.Success
            }
        }
    }

    /**
     * 사용자 템플릿과 그 미리보기 파일만 지운다(PostcardTemplateStorage.
     * deleteTemplate이 둘 다 정리). 이 템플릿을 과거에 적용했던 엽서들은
     * 이미 스타일 값이 각자 Room에 복사되어 있으므로 전혀 영향받지 않는다.
     */
    fun deleteUserTemplate(
        templateId: String
    ) {
        val target =
            _userTemplates.value.firstOrNull { it.id == templateId }
                ?: return

        _templateManageState.value = TemplateManageState.InProgress

        viewModelScope.launch(Dispatchers.IO) {
            PostcardTemplateStorage.deleteTemplate(
                context = context,
                templateId = target.id
            )

            withContext(Dispatchers.Main) {
                _userTemplates.value =
                    _userTemplates.value.filter { it.id != templateId }
                _templateManageState.value =
                    TemplateManageState.Success
            }
        }
    }

    private var subjectSegmenter: SubjectSegmenter? =
        null

    fun loadPostcard(
        postcardId: Long
    ) {
        clearPhotoTransformHistory()
        clearTemplateStyleHistory()
        _lastAppliedTemplateId.value = null
        loadUserTemplates()

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

    /**
     * 스티커의 누끼(배경제거) 이미지를 persistDir로 승격한다. 초안 저장
     * (draft_sticker_bgs/<postcardId>)과 확정 저장(sticker_bgs/<postcardId>)
     * 양쪽에서 재사용하므로, 원본 위치가 캐시든 다른 영구 디렉터리(예: 초안
     * 전용 폴더)든 모두 처리할 수 있어야 한다. 승격에 필요한 원본 파일을
     * 끝내 찾지 못하면 null을 반환해 저장 전체를 실패로 처리하게 한다 —
     * 이전에는 이 경우 조용히 isBackgroundRemoved=false로 격하시켜 사용자가
     * 모르는 새 누끼 편집 내용이 사라졌다.
     *
     * deleteCacheSourceAfterCopy: 확정 저장(persistStickerEditState)은 성공
     * 직후 clearStickerHistory()로 undo/redo를 비우므로 캐시 원본을 바로
     * 지워도 안전하다(true, 기본값). 반면 초안 자동저장(persistDraftNow)은
     * undo/redo 이력을 그대로 유지해야 하므로, 그 스택이 여전히 옛 캐시
     * 경로를 참조 중일 수 있어 false로 호출해 캐시 원본을 지우지 않는다 —
     * cacheDir는 OS가 회수 가능한 영역이라 남겨둬도 안전하다.
     */
    private fun persistStickerBackground(
        sticker: PhotoStickerItem,
        stickerCacheDir: File,
        persistDir: File,
        deleteCacheSourceAfterCopy: Boolean = true
    ): PhotoStickerItem? {
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
                    ?: return null

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
                ?: return null
        val srcFile =
            File(srcPath).canonicalFile

        // 이미 이 목적지 파일 자체를 가리키고 있다면(같은 postcardId로
        // 같은 종류의 디렉터리에 재저장하는 경우) 복사가 필요 없다.
        val finalFile =
            if (srcFile == destFile) {
                srcFile.takeIf { it.exists() && it.canRead() }
                    ?: destFile.takeIf { it.exists() && it.canRead() }
            } else if (
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
                    // 캐시 원본만, 그것도 호출자가 안전하다고 표시했을 때만
                    // 정리한다. 초안 전용 디렉터리 등 다른 영구 디렉터리에서
                    // 승격해온 경우(예: 확정 저장이 초안이 이미 옮겨둔 파일을
                    // 다시 sticker_bgs로 승격) 그 디렉터리는 별도 소유자(초안
                    // 폐기/성공 시점)가 정리하므로 여기서 지우지 않는다.
                    if (
                        deleteCacheSourceAfterCopy &&
                        srcFile.path.startsWith(
                            stickerCacheDir.path
                        )
                    ) {
                        srcFile.delete()
                    }
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

        val finalUri =
            finalFile?.let { Uri.fromFile(it) }
                ?: return null

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

        val previousFont =
            currentPostcard.messageFont

        _postcard.value =
            currentPostcard.copy(
                messageFont = normalizedFont
            )

        _fontUpdateState.value =
            FontUpdateState.Saving

        viewModelScope.launch {
            try {
                val writtenFont =
                    withContext(Dispatchers.IO) {
                        styleWriteMutex.withLock {
                            // 템플릿 일괄 저장과의 경합 방지: Mutex를 획득한
                            // 이 순간 _postcard.value를 다시 읽어서 쓴다.
                            val latestFont =
                                _postcard.value?.messageFont
                                    ?: return@withLock null
                            repository
                                .updatePostcardMessageFont(
                                    id = currentPostcard.id,
                                    messageFont = latestFont
                                )
                            latestFont
                        }
                    }

                if (writtenFont != null) {
                    _postcard.value =
                        _postcard.value?.copy(
                            messageFont = writtenFont
                        )
                }

                _fontUpdateState.value =
                    FontUpdateState.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _postcard.value =
                    _postcard.value?.copy(
                        messageFont = previousFont
                    )
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

        val previousLayoutStyle =
            currentPostcard.layoutStyle

        _postcard.value =
            currentPostcard.copy(
                layoutStyle = normalizedLayoutStyle
            )

        _layoutUpdateState.value =
            LayoutUpdateState.Saving

        viewModelScope.launch {
            try {
                val writtenLayoutStyle =
                    withContext(Dispatchers.IO) {
                        styleWriteMutex.withLock {
                            val latestLayoutStyle =
                                _postcard.value?.layoutStyle
                                    ?: return@withLock null
                            repository
                                .updatePostcardLayoutStyle(
                                    id = currentPostcard.id,
                                    layoutStyle = latestLayoutStyle
                                )
                            latestLayoutStyle
                        }
                    }

                if (writtenLayoutStyle != null) {
                    _postcard.value =
                        _postcard.value?.copy(
                            layoutStyle = writtenLayoutStyle
                        )
                }

                _layoutUpdateState.value =
                    LayoutUpdateState.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _postcard.value =
                    _postcard.value?.copy(
                        layoutStyle = previousLayoutStyle
                    )
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

        val previousDateFormat =
            currentPostcard.dateFormat

        _postcard.value =
            currentPostcard.copy(
                dateFormat = normalizedDateFormat
            )

        _dateFormatUpdateState.value =
            DateFormatUpdateState.Saving

        viewModelScope.launch {
            try {
                val writtenDateFormat =
                    withContext(Dispatchers.IO) {
                        styleWriteMutex.withLock {
                            val latestDateFormat =
                                _postcard.value?.dateFormat
                                    ?: return@withLock null
                            repository
                                .updatePostcardDateFormat(
                                    id = currentPostcard.id,
                                    dateFormat = latestDateFormat
                                )
                            latestDateFormat
                        }
                    }

                if (writtenDateFormat != null) {
                    _postcard.value =
                        _postcard.value?.copy(
                            dateFormat = writtenDateFormat
                        )
                }

                _dateFormatUpdateState.value =
                    DateFormatUpdateState.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _postcard.value =
                    _postcard.value?.copy(
                        dateFormat = previousDateFormat
                    )
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
                    val writtenScale =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                // 템플릿 일괄 저장과의 경합 방지: 이 순간
                                // _postcard.value에 남아있는 값을 다시 읽어서
                                // 쓴다(호출 당시 캡처한 normalizedScale이
                                // 아니라) — 그 사이 템플릿이 적용됐다면 그
                                // 값을 그대로 유지하고, 재확인(reconfirm)도
                                // 이 값 기준으로 해야 템플릿 값을 되돌리지
                                // 않는다.
                                val latestScale =
                                    _postcard.value?.messageTextScale
                                        ?: return@withLock null
                                repository
                                    .updatePostcardMessageTextScale(
                                        id = currentPostcard.id,
                                        messageTextScale =
                                            latestScale
                                    )
                                latestScale
                            }
                        }

                    if (writtenScale != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                messageTextScale = writtenScale
                            )
                    }
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
                    val writtenScale =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latestScale =
                                    _postcard.value?.dateTextScale
                                        ?: return@withLock null
                                repository
                                    .updatePostcardDateTextScale(
                                        id = currentPostcard.id,
                                        dateTextScale =
                                            latestScale
                                    )
                                latestScale
                            }
                        }

                    if (writtenScale != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                dateTextScale = writtenScale
                            )
                    }
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
                    val writtenDensity =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latestDensity =
                                    _postcard.value?.backgroundPatternDensity
                                        ?: return@withLock null
                                repository
                                    .updatePostcardBackgroundPatternDensity(
                                        id = currentPostcard.id,
                                        backgroundPatternDensity =
                                            latestDensity
                                    )
                                latestDensity
                            }
                        }

                    if (writtenDensity != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                backgroundPatternDensity =
                                    writtenDensity
                            )
                    }
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
                    val writtenScale =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latestScale =
                                    _postcard.value?.stampPhotoScale
                                        ?: return@withLock null
                                repository
                                    .updatePostcardStampPhotoScale(
                                        id = currentPostcard.id,
                                        stampPhotoScale =
                                            latestScale
                                    )
                                latestScale
                            }
                        }

                    if (writtenScale != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                stampPhotoScale = writtenScale
                            )
                    }
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
                    val writtenScale =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latestScale =
                                    _postcard.value?.polaroidPhotoScale
                                        ?: return@withLock null
                                repository
                                    .updatePostcardPolaroidPhotoScale(
                                        id = currentPostcard.id,
                                        polaroidPhotoScale =
                                            latestScale
                                    )
                                latestScale
                            }
                        }

                    if (writtenScale != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                polaroidPhotoScale = writtenScale
                            )
                    }
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
                    val writtenEdgeBlur =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latestEdgeBlur =
                                    _postcard.value?.photoEdgeBlur
                                        ?: return@withLock null
                                repository
                                    .updatePostcardPhotoEdgeBlur(
                                        id = currentPostcard.id,
                                        photoEdgeBlur =
                                            latestEdgeBlur
                                    )
                                latestEdgeBlur
                            }
                        }

                    if (writtenEdgeBlur != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                photoEdgeBlur = writtenEdgeBlur
                            )
                    }
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
                    val written =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latest =
                                    _postcard.value
                                        ?: return@withLock null
                                repository
                                    .updatePostcardStampPhotoOffset(
                                        id = currentPostcard.id,
                                        stampPhotoOffsetX = latest.stampPhotoOffsetX,
                                        stampPhotoOffsetY = latest.stampPhotoOffsetY
                                    )
                                latest.stampPhotoOffsetX to latest.stampPhotoOffsetY
                            }
                        }

                    if (written != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                stampPhotoOffsetX = written.first,
                                stampPhotoOffsetY = written.second
                            )
                    }
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
                    val written =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latest =
                                    _postcard.value
                                        ?: return@withLock null
                                repository
                                    .updatePostcardPolaroidPhotoOffset(
                                        id = currentPostcard.id,
                                        polaroidPhotoOffsetX = latest.polaroidPhotoOffsetX,
                                        polaroidPhotoOffsetY = latest.polaroidPhotoOffsetY
                                    )
                                latest.polaroidPhotoOffsetX to latest.polaroidPhotoOffsetY
                            }
                        }

                    if (written != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                polaroidPhotoOffsetX = written.first,
                                polaroidPhotoOffsetY = written.second
                            )
                    }
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
                    val written =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latest =
                                    _postcard.value
                                        ?: return@withLock null
                                repository
                                    .updatePostcardTapedFilmPhotoOffset(
                                        id = currentPostcard.id,
                                        tapedFilmPhotoOffsetX = latest.tapedFilmPhotoOffsetX,
                                        tapedFilmPhotoOffsetY = latest.tapedFilmPhotoOffsetY
                                    )
                                latest.tapedFilmPhotoOffsetX to latest.tapedFilmPhotoOffsetY
                            }
                        }

                    if (written != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                tapedFilmPhotoOffsetX = written.first,
                                tapedFilmPhotoOffsetY = written.second
                            )
                    }
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
                    val writtenZoom =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latestZoom =
                                    _postcard.value?.stampPhotoZoom
                                        ?: return@withLock null
                                repository
                                    .updatePostcardStampPhotoZoom(
                                        id = currentPostcard.id,
                                        stampPhotoZoom = latestZoom
                                    )
                                latestZoom
                            }
                        }

                    if (writtenZoom != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                stampPhotoZoom = writtenZoom
                            )
                    }
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
                    val writtenZoom =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latestZoom =
                                    _postcard.value?.polaroidPhotoZoom
                                        ?: return@withLock null
                                repository
                                    .updatePostcardPolaroidPhotoZoom(
                                        id = currentPostcard.id,
                                        polaroidPhotoZoom = latestZoom
                                    )
                                latestZoom
                            }
                        }

                    if (writtenZoom != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                polaroidPhotoZoom = writtenZoom
                            )
                    }
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
                    val writtenZoom =
                        withContext(Dispatchers.IO) {
                            styleWriteMutex.withLock {
                                val latestZoom =
                                    _postcard.value?.tapedFilmPhotoZoom
                                        ?: return@withLock null
                                repository
                                    .updatePostcardTapedFilmPhotoZoom(
                                        id = currentPostcard.id,
                                        tapedFilmPhotoZoom = latestZoom
                                    )
                                latestZoom
                            }
                        }

                    if (writtenZoom != null) {
                        _postcard.value =
                            _postcard.value?.copy(
                                tapedFilmPhotoZoom = writtenZoom
                            )
                    }
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

        val previousBackgroundColorArgb =
            currentPostcard.backgroundColorArgb

        _postcard.value =
            currentPostcard.copy(
                backgroundColorArgb = backgroundColorArgb
            )

        _backgroundUpdateState.value =
            BackgroundUpdateState.Saving

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    styleWriteMutex.withLock {
                        // updatePostcardBackground는 색과 이미지 경로를 한
                        // 쿼리로 함께 UPDATE하므로, 이 저장이 바꾸려는 값이
                        // 색뿐이더라도 경로까지 이 순간의 _postcard.value에서
                        // 다시 읽어 그대로 되써야 한다. 호출 당시 캡처한 값이나
                        // 고정값(null)을 쓰면 그 사이에 반영된 더 최신 경로를
                        // 오래된 저장이 덮어쓰게 된다.
                        val latest =
                            _postcard.value
                                ?: return@withLock
                        repository.updatePostcardBackground(
                            id = currentPostcard.id,
                            backgroundColorArgb =
                                latest.backgroundColorArgb,
                            backgroundImagePath =
                                latest.backgroundImagePath
                        )
                    }
                }

                // 커밋한 값은 정의상 커밋 시점의 화면 값이라 여기서 화면에
                // 되쓸 것이 없다. 그 사이 화면이 달라졌다면 그건 더 최신
                // 조작이므로 오래된 저장이 되돌리면 안 된다.
                //
                // 배경 이미지 파일은 이 함수가 삭제하지 않는다 — 배경색 변경은
                // 이미지 파일의 수명주기를 소유하지 않으며, 호출 당시 캡처한
                // 경로만 보고 지우면 그 사이 다시 참조된 파일을 지울 수 있다.
                _backgroundUpdateState.value =
                    BackgroundUpdateState.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                // 실패한 저장이 되돌릴 수 있는 건 자기가 낙관적으로 쓴 배경색뿐이다.
                // 이미 더 최신 배경색 조작이 반영됐다면 그 상태는 건드리지 않고,
                // backgroundImagePath도 이 저장의 소관이 아니므로 손대지 않는다.
                if (
                    _postcard.value?.backgroundColorArgb ==
                    backgroundColorArgb
                ) {
                    _postcard.value =
                        _postcard.value?.copy(
                            backgroundColorArgb =
                                previousBackgroundColorArgb
                        )
                }

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

        val previousPattern =
            currentPostcard.backgroundPattern

        _postcard.value =
            currentPostcard.copy(
                backgroundPattern = normalizedPattern
            )

        _backgroundUpdateState.value =
            BackgroundUpdateState.Saving

        viewModelScope.launch {
            try {
                val writtenPattern =
                    withContext(Dispatchers.IO) {
                        styleWriteMutex.withLock {
                            val latestPattern =
                                _postcard.value?.backgroundPattern
                                    ?: return@withLock null
                            repository
                                .updatePostcardBackgroundPattern(
                                    id = currentPostcard.id,
                                    backgroundPattern = latestPattern
                                )
                            latestPattern
                        }
                    }

                if (writtenPattern != null) {
                    _postcard.value =
                        _postcard.value?.copy(
                            backgroundPattern = writtenPattern
                        )
                }

                _backgroundUpdateState.value =
                    BackgroundUpdateState.Success
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _postcard.value =
                    _postcard.value?.copy(
                        backgroundPattern = previousPattern
                    )
                _backgroundUpdateState.value =
                    BackgroundUpdateState.Error(
                        exception.message
                            ?: "배경 패턴을 저장하지 못했습니다."
                    )
            }
        }
    }

    fun resetBackgroundUpdateState() {
        _backgroundUpdateState.value =
            BackgroundUpdateState.Idle
    }

    /**
     * 슬라이더 계열 저장(saveStampPhotoScale 등)과 템플릿 적용(persistTemplateStyle)은
     * DetailScreen의 controlsEnabled가 확인하는 Saving 상태가 없어, 저장이 실제
     * DAO 쓰기에 닿기 전에도 뒤로 가기가 가능하다. 화면 이탈 직전 이 함수로
     * 아직 끝나지 않은 저장들이 완료되기를 기다린 뒤 navigation을 진행해야,
     * ViewModelStore가 clear()되어 viewModelScope가 취소되기 전에 마지막 값이
     * Room에 반영된다. 각 Job은 실패를 자체적으로 롤백하고 CancellationException을
     * rethrow하므로 여기서는 완료 여부만 기다리면 된다(join은 예외를 전파하지
     * 않는다). 혹시 모를 비정상적 지연으로 navigation이 무기한 멈추지 않도록
     * 상한 시간을 둔다.
     */
    suspend fun awaitPendingStyleSaves() {
        val pendingJobs =
            listOfNotNull(
                messageTextScaleSaveJob,
                dateTextScaleSaveJob,
                backgroundPatternDensitySaveJob,
                stampPhotoScaleSaveJob,
                polaroidPhotoScaleSaveJob,
                photoEdgeBlurSaveJob,
                stampPhotoOffsetSaveJob,
                polaroidPhotoOffsetSaveJob,
                tapedFilmPhotoOffsetSaveJob,
                stampPhotoZoomSaveJob,
                polaroidPhotoZoomSaveJob,
                tapedFilmPhotoZoomSaveJob,
                templateStyleSaveJob
            ).filter { it.isActive }

        if (pendingJobs.isEmpty()) {
            return
        }

        withTimeoutOrNull(PENDING_STYLE_SAVE_TIMEOUT_MS) {
            pendingJobs.joinAll()
        }
    }

    /**
     * 중심 사진 교체. 새 파일 복사 → Room 갱신 → 화면 갱신까지 모두 성공한
     * 뒤에만 이전 사진 파일을 지운다 — 예전에는 이 성공 경로에서 이전
     * 파일을 전혀 정리하지 않아 교체할 때마다 filesDir/postcards/에 과거
     * 사진이 계속 누적됐다. Room 갱신 실패 시에는 이전 그대로 새로 만든
     * 파일만 지운다(기존 동작 유지). previousImagePath와 newImagePath가
     * 같은 경우(이론상 UUID 기반이라 발생하지 않지만 방어적으로)는 이전
     * 파일을 지우지 않는다.
     */
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

        val previousImagePath =
            currentPostcard.imagePath

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

                if (previousImagePath != newImagePath) {
                    withContext(Dispatchers.IO) {
                        PostcardImageStorage
                            .deleteIfOwnedByApp(
                                context = context,
                                path = previousImagePath
                            )
                    }
                }
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
                onFailure = { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

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
            _shareState.value !is
                    ShareState.Idle
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

    /**
     * 상세 화면 삭제. 갤러리 삭제(GalleryViewModel.deletePostcards)와 동일한
     * PostcardDeletionManager를 사용해 두 경로가 같은 자산을 정리하게 한다.
     * Room 삭제 자체가 실패하면 화면을 종료하지 않고 재시도할 수 있게 두며,
     * Room은 삭제됐지만 일부 파일 정리만 실패했다면(고아 파일) 화면은
     * 정상 종료하되 그 사실을 로그로 남긴다.
     */
    fun deletePostcard() {
        val currentPostcard =
            _postcard.value
                ?: return

        if (_deleteState.value is PostcardDeleteState.Deleting) {
            return
        }

        _deleteState.value = PostcardDeleteState.Deleting

        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    deletionManager.deletePostcard(currentPostcard)
                }

            if (!result.databaseDeleted) {
                _deleteState.value =
                    PostcardDeleteState.Error(
                        "엽서를 삭제하지 못했어. 다시 시도해줘."
                    )
                return@launch
            }

            if (result.failedAssets.isNotEmpty()) {
                Log.w(
                    TAG,
                    "엽서 삭제: DB는 지워졌지만 일부 파일 정리 실패 " +
                            "postcardId=${result.postcardId} " +
                            "failed=${result.failedAssets}"
                )
            }

            _deleteState.value = PostcardDeleteState.Deleted
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
            "TAPED_FILM",
            "LETTER" -> layoutStyle

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

    /**
     * cacheDir/photo_stickers를 여기서 더 이상 정리하지 않는다. 예전에는
     * 화면을 나갈 때(완료 버튼을 안 눌러도) 현재 스티커가 참조하는 캐시
     * 파일을 무조건 지웠는데, 이러면 막 배경을 제거하고 나간 초안이
     * 참조하는 누끼 파일까지 함께 사라졌다. 이제 배경제거 파일은
     * persistDraftNow()가 자동저장 시점에 draft_sticker_bgs/<postcardId>/
     * 로 승격해 초안이 그 durable 경로를 가리키게 하므로, 캐시에 남는
     * 파일은 승격이 끝난 뒤의 여분 사본이거나 애초에 아무 저장도 참조하지
     * 않는 파일뿐이다 — cacheDir는 OS가 알아서 회수할 수 있는 임시 영역이라
     * 여기서 강제로 지우지 않아도 안전하다.
     */
    override fun onCleared() {
        subjectSegmenter?.close()
        subjectSegmenter = null
        super.onCleared()
    }
}
