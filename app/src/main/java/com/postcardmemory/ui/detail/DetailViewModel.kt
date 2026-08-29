package com.postcardmemory.ui.detail

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import com.postcardmemory.ui.components.BACK_MESSAGE_MAX_LENGTH
import com.postcardmemory.ui.components.BACK_RECIPIENT_MODIFIER_MAX_LENGTH
import com.postcardmemory.utils.ConfirmedEditStateStorage
import com.postcardmemory.utils.DoodleStroke
import com.postcardmemory.utils.PhotoColorExtractor
import com.postcardmemory.utils.PhotoStickerImageStorage
import com.postcardmemory.utils.PostcardDeletionManager
import com.postcardmemory.utils.PostcardDraftStorage
import com.postcardmemory.utils.PostcardImageExporter
import com.postcardmemory.utils.deserializeDoodleStroke
import com.postcardmemory.utils.serialize as serializeDoodleStroke
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
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "DetailViewModel"
private const val SEAL_HISTORY_LIMIT = 50
private const val STICKER_HISTORY_LIMIT = 30
private const val DOODLE_HISTORY_LIMIT = 50
private const val TEXT_STICKER_HISTORY_LIMIT = 50
private const val MASKING_TAPE_HISTORY_LIMIT = 50
private const val LABEL_STICKER_HISTORY_LIMIT = 50
private const val PHOTO_TRANSFORM_HISTORY_LIMIT = 50
private const val DRAFT_AUTOSAVE_DEBOUNCE_MS = 900L
private const val PENDING_STYLE_SAVE_TIMEOUT_MS = 2_000L

sealed interface DraftSaveStatus {

    data object Idle : DraftSaveStatus

    data object PendingChanges : DraftSaveStatus

    data object Saving : DraftSaveStatus

    data object Saved : DraftSaveStatus

    data object Failed : DraftSaveStatus
}

/** 완료 버튼(확정 저장)의 상태. 스티커·도장 저장을 이 상태로만 판단한다. */
sealed interface ConfirmSaveState {

    data object Idle : ConfirmSaveState

    data object Saving : ConfirmSaveState

    data object Saved : ConfirmSaveState

    data object Failed : ConfirmSaveState
}

/** 스티커·도장·낙서 저장이 모두 성공했을 때만 확정 저장 전체를 성공으로 본다. */
internal fun shouldConfirmSaveSucceed(
    stickersSaved: Boolean,
    sealsSaved: Boolean,
    doodlesSaved: Boolean = true,
    textStickersSaved: Boolean = true,
    maskingTapesSaved: Boolean = true,
    labelStickersSaved: Boolean = true
): Boolean =
    stickersSaved &&
            sealsSaved &&
            doodlesSaved &&
            textStickersSaved &&
            maskingTapesSaved &&
            labelStickersSaved

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

/** "미래의 나에게 보내기" 발송 상태. */
sealed interface FutureMailSendState {

    data object Idle : FutureMailSendState

    data object Sending : FutureMailSendState

    data object Sent : FutureMailSendState

    data class Error(
        val message: String
    ) : FutureMailSendState
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
    @param:ApplicationContext private val context: Context
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

    private val _futureMailSendState =
        MutableStateFlow<FutureMailSendState>(FutureMailSendState.Idle)

    val futureMailSendState: StateFlow<FutureMailSendState> =
        _futureMailSendState

    fun acknowledgeFutureMailSendError() {
        if (_futureMailSendState.value is FutureMailSendState.Error) {
            _futureMailSendState.value = FutureMailSendState.Idle
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

    private var backgroundColorSaveJob: Job? = null

    private var backgroundPatternSaveJob: Job? = null

    private var messageFontSaveJob: Job? = null

    private var layoutStyleSaveJob: Job? = null

    private var dateFormatSaveJob: Job? = null

    private var messageUpdateJob: Job? = null

    private var backRecipientModifierSaveJob: Job? = null

    private var backMessageSaveJob: Job? = null

    private var confirmSaveJob: Job? = null

    /**
     * 개별 스타일 저장(위 Job들)의 실제 DAO 쓰기 구간을 직렬화한다.
     * 각 저장은 이 Mutex를 획득한 시점에
     * _postcard.value에서 자신이 쓸 값을 다시 읽어서 DAO에 넘기므로(호출
     * 시점에 캡처해둔 값이 아니라), 완료 순서가 뒤바뀌어도 가장 나중에
     * 커밋하는 저장이 항상 그 순간의 실제 화면 상태를 그대로 쓰게 된다 —
     * 서로 다른 개별 저장 중 어느 쪽이 사용자의 시간상 마지막
     * 조작이었는지와 무관하게, "다시 읽기 + 직렬화"만으로 항상 최신 조작이
     * 최종 Room 상태가 된다. Mutex 없이 다시 읽기만 하면 읽기와 커밋 사이의
     * 시간차 때문에 오래된 읽기가 나중에 커밋되며 다시 역전될 수 있어 두
     * 가지를 함께 써야 한다.
     *
     * backgroundColorSaveJob 등 5개는 다른 것과 달리 새 저장이 이전 Job을
     * cancel()하지 않는다 — 재읽기+직렬화만으로 이미 최종 상태로 수렴하므로
     * cancel 없이도 안전하며(da80596), 여러 개가 겹쳐 있어도 그중 어느
     * 것이든 완료되면 그 시점의 화면 상태가 커밋된다. awaitPendingStyleSaves()가
     * 필드에 보관된(=가장 나중에 launch된) Job 하나만 join해도 충분한 이유가
     * 이것이다.
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
    private var confirmedDoodlesBaseline: List<DoodleStroke> = emptyList()
    private var confirmedTextStickersBaseline: List<TextStickerItem> = emptyList()
    private var confirmedMaskingTapesBaseline: List<MaskingTapeItem> = emptyList()
    private var confirmedLabelStickersBaseline: List<LabelStickerItem> = emptyList()

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
        confirmedDoodlesBaseline = emptyList()
        confirmedTextStickersBaseline = emptyList()
        confirmedMaskingTapesBaseline = emptyList()
        confirmedLabelStickersBaseline = emptyList()

        clearStickerHistory()
        clearSealHistory()
        clearDoodleHistory()
        clearTextStickerHistory()
        clearMaskingTapeHistory()
        clearLabelStickerHistory()

        viewModelScope.launch(Dispatchers.IO) {
            val confirmedStickers = readConfirmedStickerState(postcardId)
            val confirmedSeals = readConfirmedSealState(postcardId)
            val confirmedDoodles = readConfirmedDoodleState(postcardId)
            val confirmedTextStickers = readConfirmedTextStickerState(postcardId)
            val confirmedMaskingTapes = readConfirmedMaskingTapeState(postcardId)
            val confirmedLabelStickers = readConfirmedLabelStickerState(postcardId)

            withContext(Dispatchers.Main) {
                _photoStickers.value = confirmedStickers
                _selectedStickerId.value = null
                _photoSeals.value = confirmedSeals
                _selectedSealId.value = null
                _doodleStrokes.value = confirmedDoodles
                _textStickers.value = confirmedTextStickers
                _selectedTextStickerId.value = null
                _photoMaskingTapes.value = confirmedMaskingTapes
                _selectedMaskingTapeId.value = null
                _labelStickers.value = confirmedLabelStickers
                _selectedLabelStickerId.value = null
            }

            confirmedStickersBaseline = confirmedStickers
            confirmedSealsBaseline = confirmedSeals
            confirmedDoodlesBaseline = confirmedDoodles
            confirmedTextStickersBaseline = confirmedTextStickers
            confirmedMaskingTapesBaseline = confirmedMaskingTapes
            confirmedLabelStickersBaseline = confirmedLabelStickers

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
                    _doodleStrokes.value = existingDraft.doodleStrokes
                    _textStickers.value = existingDraft.textStickers
                    _selectedTextStickerId.value = existingDraft.selectedTextStickerId
                    _photoMaskingTapes.value = existingDraft.maskingTapes
                    _selectedMaskingTapeId.value = existingDraft.selectedMaskingTapeId
                    _labelStickers.value = existingDraft.labelStickers
                    _selectedLabelStickerId.value = existingDraft.selectedLabelStickerId
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
        _doodleStrokes.value = confirmedDoodlesBaseline
        _textStickers.value = confirmedTextStickersBaseline
        _selectedTextStickerId.value = null
        _photoMaskingTapes.value = confirmedMaskingTapesBaseline
        _selectedMaskingTapeId.value = null
        _labelStickers.value = confirmedLabelStickersBaseline
        _selectedLabelStickerId.value = null

        clearStickerHistory()
        clearSealHistory()
        clearDoodleHistory()
        clearTextStickerHistory()
        clearMaskingTapeHistory()
        clearLabelStickerHistory()

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
            delay(DRAFT_AUTOSAVE_DEBOUNCE_MS.milliseconds)
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
        val snapshotDoodleStrokes = _doodleStrokes.value
        val snapshotTextStickers = _textStickers.value
        val snapshotSelectedTextStickerId = _selectedTextStickerId.value
        val snapshotMaskingTapes = _photoMaskingTapes.value
        val snapshotSelectedMaskingTapeId = _selectedMaskingTapeId.value
        val snapshotLabelStickers = _labelStickers.value
        val snapshotSelectedLabelStickerId = _selectedLabelStickerId.value

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
                    selectedSealId = snapshotSelectedSealId,
                    doodleStrokes = snapshotDoodleStrokes,
                    textStickers = snapshotTextStickers,
                    selectedTextStickerId = snapshotSelectedTextStickerId,
                    maskingTapes = snapshotMaskingTapes,
                    selectedMaskingTapeId = snapshotSelectedMaskingTapeId,
                    labelStickers = snapshotLabelStickers,
                    selectedLabelStickerId = snapshotSelectedLabelStickerId
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

        confirmSaveJob = viewModelScope.launch(Dispatchers.IO) {
            val stickersSaved = persistStickerEditState(postcardId)
            val sealsSaved = persistSealEditState(postcardId)
            val doodlesSaved = persistDoodleEditState(postcardId)
            val textStickersSaved = persistTextStickerEditState(postcardId)
            val maskingTapesSaved = persistMaskingTapeEditState(postcardId)
            val labelStickersSaved = persistLabelStickerEditState(postcardId)
            val allSaved =
                shouldConfirmSaveSucceed(
                    stickersSaved,
                    sealsSaved,
                    doodlesSaved,
                    textStickersSaved,
                    maskingTapesSaved,
                    labelStickersSaved
                )

            if (allSaved) {
                draftSaveMutex.withLock {
                    latestPersistedDraftRevision =
                        draftRevisionCounter.incrementAndGet()
                    PostcardDraftStorage.deleteDraft(context, postcardId)
                }
            }

            withContext(Dispatchers.Main) {
                // 스티커 저장 자체는 성공해도 도장·낙서 저장이 실패하면 전체 결과는
                // Failed이므로, 이 시점(allSaved 확정 후)에야 스티커 undo/redo
                // 이력을 지운다. persistStickerEditState 안에서 자신의 성공만
                // 보고 바로 지우면, 도장·낙서 저장 실패로 사용자가 재시도해야 하는
                // 상황에서 스티커 되돌리기 능력만 먼저 사라지는 비대칭이 생긴다.
                //
                // 여기서 지우는 목록은 초기 로드(loadStickerSealStateAndAutoRestoreDraft)
                // 및 "원래대로"(revertToConfirmedState)와 같은 여섯 개여야 한다 —
                // 하나라도 빠지면 그 요소만 확정 저장 후에도 이전 상태로 undo돼
                // 저장된 결과와 화면이 어긋난다.
                if (allSaved) {
                    clearStickerHistory()
                    clearSealHistory()
                    clearDoodleHistory()
                    clearTextStickerHistory()
                    clearMaskingTapeHistory()
                    clearLabelStickerHistory()
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

    /**
     * 스티커 확정 상태를 저장한다. 누끼 파일 승격이나 상태 파일 쓰기 중
     * 하나라도 실패하면 false를 반환하며, 이 경우 StateFlow와 undo/redo
     * 이력, 기존 확정 파일 모두 건드리지 않아 재시도가 안전하다.
     */
    private fun persistStickerEditState(
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

        return true
    }

    /** 확정 저장된 스티커 상태만 읽어 반환한다(StateFlow는 건드리지 않음). */
    private fun readConfirmedStickerState(
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

    /** 도장 확정 상태를 원자적으로 저장한다. 실패 시 기존 확정 파일은 그대로 유지된다. */
    private fun persistSealEditState(
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
    private fun readConfirmedSealState(
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

    // ---- 텍스트 스티커 상태 ----

    private val _textStickers =
        MutableStateFlow(listOf<TextStickerItem>())

    val textStickers: StateFlow<List<TextStickerItem>> =
        _textStickers

    private val _selectedTextStickerId =
        MutableStateFlow<String?>(null)

    val selectedTextStickerId: StateFlow<String?> =
        _selectedTextStickerId

    fun setTextStickers(
        textStickers: List<TextStickerItem>
    ) {
        _textStickers.value = textStickers
        scheduleDraftAutosave()
    }

    fun setSelectedTextStickerId(
        id: String?
    ) {
        _selectedTextStickerId.value = id
    }

    /** 텍스트 스티커 확정 상태를 원자적으로 저장한다. 실패 시 기존 확정 파일은 그대로 유지된다. */
    private fun persistTextStickerEditState(
        postcardId: Long
    ): Boolean {
        val stateFile =
            File(context.filesDir, "text_sticker_states/$postcardId.txt")

        return ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = stateFile,
            content = _textStickers.value.joinToString("\n") {
                it.serialize()
            }
        )
    }

    /** 확정 저장된 텍스트 스티커 상태만 읽어 반환한다(StateFlow는 건드리지 않음). */
    private fun readConfirmedTextStickerState(
        postcardId: Long
    ): List<TextStickerItem> {
        val file =
            File(
                context.filesDir,
                "text_sticker_states/$postcardId.txt"
            )
        if (!file.exists()) return emptyList()

        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                deserializeTextStickerItem(line)
            }
    }

    // ---- 마스킹테이프 상태 ----

    private val _photoMaskingTapes =
        MutableStateFlow(listOf<MaskingTapeItem>())

    val photoMaskingTapes: StateFlow<List<MaskingTapeItem>> =
        _photoMaskingTapes

    private val _selectedMaskingTapeId =
        MutableStateFlow<String?>(null)

    val selectedMaskingTapeId: StateFlow<String?> =
        _selectedMaskingTapeId

    fun setPhotoMaskingTapes(
        maskingTapes: List<MaskingTapeItem>
    ) {
        _photoMaskingTapes.value = maskingTapes
        scheduleDraftAutosave()
    }

    fun setSelectedMaskingTapeId(
        id: String?
    ) {
        _selectedMaskingTapeId.value = id
    }

    /**
     * duplicateSticker와 동일한 정책 — 살짝 어긋난 위치(40,40 px)에 같은
     * 디자인의 새 테이프를 추가한다. photoUri는 파일을 복사하지 않고
     * 그대로 공유한다(사진 스티커의 originalUri와 달리 배경제거 캐시
     * 파일이 아니라 persistable 권한을 받은 갤러리 Uri라 복사가 필요 없다).
     */
    fun duplicateMaskingTape(
        maskingTapeId: String
    ) {
        val original =
            _photoMaskingTapes.value.find {
                it.id == maskingTapeId
            } ?: return

        recordMaskingTapeSnapshotForUndo()

        val duplicate =
            original.copy(
                id = UUID.randomUUID().toString(),
                offset = original.offset?.plus(Offset(40f, 40f))
            )

        _photoMaskingTapes.value += duplicate
        _selectedMaskingTapeId.value = duplicate.id
        scheduleDraftAutosave()
    }

    /** 마스킹테이프 확정 상태를 원자적으로 저장한다. 실패 시 기존 확정 파일은 그대로 유지된다. */
    private fun persistMaskingTapeEditState(
        postcardId: Long
    ): Boolean {
        val stateFile =
            File(context.filesDir, "masking_tape_states/$postcardId.txt")

        return ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = stateFile,
            content = _photoMaskingTapes.value.joinToString("\n") {
                it.serialize()
            }
        )
    }

    /** 확정 저장된 마스킹테이프 상태만 읽어 반환한다(StateFlow는 건드리지 않음). */
    private fun readConfirmedMaskingTapeState(
        postcardId: Long
    ): List<MaskingTapeItem> {
        val file =
            File(
                context.filesDir,
                "masking_tape_states/$postcardId.txt"
            )
        if (!file.exists()) return emptyList()

        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                deserializeMaskingTapeItem(line)
            }
    }

    // ---- 라벨 스티커 상태 ----

    private val _labelStickers =
        MutableStateFlow(listOf<LabelStickerItem>())

    val labelStickers: StateFlow<List<LabelStickerItem>> =
        _labelStickers

    private val _selectedLabelStickerId =
        MutableStateFlow<String?>(null)

    val selectedLabelStickerId: StateFlow<String?> =
        _selectedLabelStickerId

    fun setLabelStickers(
        labelStickers: List<LabelStickerItem>
    ) {
        _labelStickers.value = labelStickers
        scheduleDraftAutosave()
    }

    fun setSelectedLabelStickerId(
        id: String?
    ) {
        _selectedLabelStickerId.value = id
    }

    /** 라벨 스티커 확정 상태를 원자적으로 저장한다. 실패 시 기존 확정 파일은 그대로 유지된다. */
    private fun persistLabelStickerEditState(
        postcardId: Long
    ): Boolean {
        val stateFile =
            File(context.filesDir, "label_sticker_states/$postcardId.txt")

        return ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = stateFile,
            content = _labelStickers.value.joinToString("\n") {
                it.serialize()
            }
        )
    }

    /** 확정 저장된 라벨 스티커 상태만 읽어 반환한다(StateFlow는 건드리지 않음). */
    private fun readConfirmedLabelStickerState(
        postcardId: Long
    ): List<LabelStickerItem> {
        val file =
            File(
                context.filesDir,
                "label_sticker_states/$postcardId.txt"
            )
        if (!file.exists()) return emptyList()

        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                deserializeLabelStickerItem(line)
            }
    }

    // ---- 낙서 상태 ----

    private val _doodleStrokes =
        MutableStateFlow(listOf<DoodleStroke>())

    val doodleStrokes: StateFlow<List<DoodleStroke>> =
        _doodleStrokes

    fun setDoodleStrokes(
        strokes: List<DoodleStroke>
    ) {
        _doodleStrokes.value = strokes
        scheduleDraftAutosave()
    }

    /** 낙서 확정 상태를 원자적으로 저장한다. 실패 시 기존 확정 파일은 그대로 유지된다. */
    private fun persistDoodleEditState(
        postcardId: Long
    ): Boolean {
        val stateFile =
            File(context.filesDir, "doodle_states/$postcardId.txt")

        return ConfirmedEditStateStorage.writeTextAtomically(
            targetFile = stateFile,
            content = _doodleStrokes.value.joinToString("\n") {
                it.serializeDoodleStroke()
            }
        )
    }

    /** 확정 저장된 낙서 상태만 읽어 반환한다(StateFlow는 건드리지 않음). */
    private fun readConfirmedDoodleState(
        postcardId: Long
    ): List<DoodleStroke> {
        val file =
            File(
                context.filesDir,
                "doodle_states/$postcardId.txt"
            )
        if (!file.exists()) return emptyList()

        return file.readLines()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                deserializeDoodleStroke(line)
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

    private data class TextStickerSnapshot(
        val textStickers: List<TextStickerItem>,
        val selectedTextStickerId: String?
    )

    private val textStickerUndoStack =
        ArrayDeque<TextStickerSnapshot>()

    private val textStickerRedoStack =
        ArrayDeque<TextStickerSnapshot>()

    private val _canUndoTextSticker =
        MutableStateFlow(false)

    val canUndoTextSticker: StateFlow<Boolean> =
        _canUndoTextSticker

    private val _canRedoTextSticker =
        MutableStateFlow(false)

    val canRedoTextSticker: StateFlow<Boolean> =
        _canRedoTextSticker

    private fun updateTextStickerHistoryAvailability() {
        _canUndoTextSticker.value = textStickerUndoStack.isNotEmpty()
        _canRedoTextSticker.value = textStickerRedoStack.isNotEmpty()
    }

    private fun clearTextStickerHistory() {
        textStickerUndoStack.clear()
        textStickerRedoStack.clear()
        updateTextStickerHistoryAvailability()
    }

    fun recordTextStickerSnapshotForUndo() {
        textStickerUndoStack.addLast(
            TextStickerSnapshot(
                textStickers = _textStickers.value,
                selectedTextStickerId = _selectedTextStickerId.value
            )
        )
        if (textStickerUndoStack.size > TEXT_STICKER_HISTORY_LIMIT) {
            textStickerUndoStack.removeFirst()
        }
        textStickerRedoStack.clear()
        updateTextStickerHistoryAvailability()
    }

    fun undoTextStickerChange() {
        val previous =
            textStickerUndoStack.removeLastOrNull() ?: return

        textStickerRedoStack.addLast(
            TextStickerSnapshot(
                textStickers = _textStickers.value,
                selectedTextStickerId = _selectedTextStickerId.value
            )
        )
        _textStickers.value = previous.textStickers
        _selectedTextStickerId.value = previous.selectedTextStickerId
        updateTextStickerHistoryAvailability()
        scheduleDraftAutosave()
    }

    fun redoTextStickerChange() {
        val next =
            textStickerRedoStack.removeLastOrNull() ?: return

        textStickerUndoStack.addLast(
            TextStickerSnapshot(
                textStickers = _textStickers.value,
                selectedTextStickerId = _selectedTextStickerId.value
            )
        )
        _textStickers.value = next.textStickers
        _selectedTextStickerId.value = next.selectedTextStickerId
        updateTextStickerHistoryAvailability()
        scheduleDraftAutosave()
    }

    private data class MaskingTapeSnapshot(
        val maskingTapes: List<MaskingTapeItem>,
        val selectedMaskingTapeId: String?
    )

    private val maskingTapeUndoStack =
        ArrayDeque<MaskingTapeSnapshot>()

    private val maskingTapeRedoStack =
        ArrayDeque<MaskingTapeSnapshot>()

    private val _canUndoMaskingTape =
        MutableStateFlow(false)

    val canUndoMaskingTape: StateFlow<Boolean> =
        _canUndoMaskingTape

    private val _canRedoMaskingTape =
        MutableStateFlow(false)

    val canRedoMaskingTape: StateFlow<Boolean> =
        _canRedoMaskingTape

    private fun updateMaskingTapeHistoryAvailability() {
        _canUndoMaskingTape.value = maskingTapeUndoStack.isNotEmpty()
        _canRedoMaskingTape.value = maskingTapeRedoStack.isNotEmpty()
    }

    private fun clearMaskingTapeHistory() {
        maskingTapeUndoStack.clear()
        maskingTapeRedoStack.clear()
        updateMaskingTapeHistoryAvailability()
    }

    fun recordMaskingTapeSnapshotForUndo() {
        maskingTapeUndoStack.addLast(
            MaskingTapeSnapshot(
                maskingTapes = _photoMaskingTapes.value,
                selectedMaskingTapeId = _selectedMaskingTapeId.value
            )
        )
        if (maskingTapeUndoStack.size > MASKING_TAPE_HISTORY_LIMIT) {
            maskingTapeUndoStack.removeFirst()
        }
        maskingTapeRedoStack.clear()
        updateMaskingTapeHistoryAvailability()
    }

    fun undoMaskingTapeChange() {
        val previous =
            maskingTapeUndoStack.removeLastOrNull() ?: return

        maskingTapeRedoStack.addLast(
            MaskingTapeSnapshot(
                maskingTapes = _photoMaskingTapes.value,
                selectedMaskingTapeId = _selectedMaskingTapeId.value
            )
        )
        _photoMaskingTapes.value = previous.maskingTapes
        _selectedMaskingTapeId.value = previous.selectedMaskingTapeId
        updateMaskingTapeHistoryAvailability()
        scheduleDraftAutosave()
    }

    fun redoMaskingTapeChange() {
        val next =
            maskingTapeRedoStack.removeLastOrNull() ?: return

        maskingTapeUndoStack.addLast(
            MaskingTapeSnapshot(
                maskingTapes = _photoMaskingTapes.value,
                selectedMaskingTapeId = _selectedMaskingTapeId.value
            )
        )
        _photoMaskingTapes.value = next.maskingTapes
        _selectedMaskingTapeId.value = next.selectedMaskingTapeId
        updateMaskingTapeHistoryAvailability()
        scheduleDraftAutosave()
    }

    private data class LabelStickerSnapshot(
        val labelStickers: List<LabelStickerItem>,
        val selectedLabelStickerId: String?
    )

    private val labelStickerUndoStack =
        ArrayDeque<LabelStickerSnapshot>()

    private val labelStickerRedoStack =
        ArrayDeque<LabelStickerSnapshot>()

    private val _canUndoLabelSticker =
        MutableStateFlow(false)

    val canUndoLabelSticker: StateFlow<Boolean> =
        _canUndoLabelSticker

    private val _canRedoLabelSticker =
        MutableStateFlow(false)

    val canRedoLabelSticker: StateFlow<Boolean> =
        _canRedoLabelSticker

    private fun updateLabelStickerHistoryAvailability() {
        _canUndoLabelSticker.value = labelStickerUndoStack.isNotEmpty()
        _canRedoLabelSticker.value = labelStickerRedoStack.isNotEmpty()
    }

    private fun clearLabelStickerHistory() {
        labelStickerUndoStack.clear()
        labelStickerRedoStack.clear()
        updateLabelStickerHistoryAvailability()
    }

    fun recordLabelStickerSnapshotForUndo() {
        labelStickerUndoStack.addLast(
            LabelStickerSnapshot(
                labelStickers = _labelStickers.value,
                selectedLabelStickerId = _selectedLabelStickerId.value
            )
        )
        if (labelStickerUndoStack.size > LABEL_STICKER_HISTORY_LIMIT) {
            labelStickerUndoStack.removeFirst()
        }
        labelStickerRedoStack.clear()
        updateLabelStickerHistoryAvailability()
    }

    fun undoLabelStickerChange() {
        val previous =
            labelStickerUndoStack.removeLastOrNull() ?: return

        labelStickerRedoStack.addLast(
            LabelStickerSnapshot(
                labelStickers = _labelStickers.value,
                selectedLabelStickerId = _selectedLabelStickerId.value
            )
        )
        _labelStickers.value = previous.labelStickers
        _selectedLabelStickerId.value = previous.selectedLabelStickerId
        updateLabelStickerHistoryAvailability()
        scheduleDraftAutosave()
    }

    fun redoLabelStickerChange() {
        val next =
            labelStickerRedoStack.removeLastOrNull() ?: return

        labelStickerUndoStack.addLast(
            LabelStickerSnapshot(
                labelStickers = _labelStickers.value,
                selectedLabelStickerId = _selectedLabelStickerId.value
            )
        )
        _labelStickers.value = next.labelStickers
        _selectedLabelStickerId.value = next.selectedLabelStickerId
        updateLabelStickerHistoryAvailability()
        scheduleDraftAutosave()
    }

    private data class DoodleSnapshot(
        val strokes: List<DoodleStroke>
    )

    private val doodleUndoStack =
        ArrayDeque<DoodleSnapshot>()

    private val doodleRedoStack =
        ArrayDeque<DoodleSnapshot>()

    private val _canUndoDoodle =
        MutableStateFlow(false)

    val canUndoDoodle: StateFlow<Boolean> =
        _canUndoDoodle

    private val _canRedoDoodle =
        MutableStateFlow(false)

    val canRedoDoodle: StateFlow<Boolean> =
        _canRedoDoodle

    private fun updateDoodleHistoryAvailability() {
        _canUndoDoodle.value = doodleUndoStack.isNotEmpty()
        _canRedoDoodle.value = doodleRedoStack.isNotEmpty()
    }

    private fun clearDoodleHistory() {
        doodleUndoStack.clear()
        doodleRedoStack.clear()
        updateDoodleHistoryAvailability()
    }

    /**
     * 낙서 한 획(펜 확정, 지우개 동작, 전체 지우기)을 시작하기 직전에 호출한다.
     * sealUndoStack/stickerUndoStack과 동일하게 변경 직전의 전체 목록을
     * 스냅샷으로 남기는 방식이라, 별도 재설계 없이 기존 Undo/Redo 체계에
     * 그대로 편입된다.
     */
    fun recordDoodleSnapshotForUndo() {
        doodleUndoStack.addLast(
            DoodleSnapshot(strokes = _doodleStrokes.value)
        )
        if (doodleUndoStack.size > DOODLE_HISTORY_LIMIT) {
            doodleUndoStack.removeFirst()
        }
        doodleRedoStack.clear()
        updateDoodleHistoryAvailability()
    }

    fun undoDoodleChange() {
        val previous =
            doodleUndoStack.removeLastOrNull() ?: return

        doodleRedoStack.addLast(
            DoodleSnapshot(strokes = _doodleStrokes.value)
        )
        _doodleStrokes.value = previous.strokes
        updateDoodleHistoryAvailability()
        scheduleDraftAutosave()
    }

    fun redoDoodleChange() {
        val next =
            doodleRedoStack.removeLastOrNull() ?: return

        doodleUndoStack.addLast(
            DoodleSnapshot(strokes = _doodleStrokes.value)
        )
        _doodleStrokes.value = next.strokes
        updateDoodleHistoryAvailability()
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

    /**
     * sweepStickerCleanupCandidates()와 같은 판정 로직을 쓰지만, 화면을
     * 완전히 나가기 직전(awaitPendingStyleSaves) 호출용으로 실제 파일 삭제를
     * viewModelScope에 새 Job으로 던지지 않고 이 자리에서 끝까지 기다린다.
     * onCleared() 시점에는 viewModelScope가 이미 취소된 뒤라 그 안에서
     * launch한 정리 작업은 시작도 못 하고 취소되므로, sticker_originals/
     * 아래 아직 정리되지 않은 원본 파일이 영구적으로 남는다 — 그래서
     * onCleared()가 아니라 scope가 살아있는 이 시점에 직접 기다려야 한다.
     */
    private suspend fun awaitStickerCleanupSweep() {
        val stillPending =
            stickerCleanupCandidates.toList()

        stillPending.forEach { uri ->
            if (!isStickerFileStillReferenced(uri)) {
                stickerCleanupCandidates.remove(uri)

                withContext(Dispatchers.IO) {
                    uriToLocalStickerFile(uri)
                        ?.let { file -> deleteStickerCacheFile(file) }

                    PhotoStickerImageStorage
                        .deleteOriginalIfUnreferenced(
                            context = context,
                            deletedUri = uri,
                            remainingStickers = _photoStickers.value
                        )
                }
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
        val tapedFilmPhotoZoom: Float,
        val photoEdgeBlur: Float
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
            tapedFilmPhotoZoom = current.tapedFilmPhotoZoom,
            photoEdgeBlur = current.photoEdgeBlur
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
        if (current.photoEdgeBlur != snapshot.photoEdgeBlur) {
            savePhotoEdgeBlur(snapshot.photoEdgeBlur)
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

    /** updateBackRecipientModifier와 동일한 이유로 styleWriteMutex를 재사용한다. */
    fun updateMessage(
        message: String
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val normalizedMessage =
            message.take(120)

        val previous =
            currentPostcard.message

        _postcard.value =
            currentPostcard.copy(
                message = normalizedMessage
            )

        messageUpdateJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    styleWriteMutex.withLock {
                        val latest =
                            _postcard.value
                                ?: return@withLock
                        repository.updatePostcardMessage(
                            id = currentPostcard.id,
                            message = latest.message
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (_postcard.value?.message == normalizedMessage) {
                    _postcard.value =
                        _postcard.value?.copy(
                            message = previous
                        )
                }

                Log.w(
                    TAG,
                    "글귀 저장 실패: ${exception.message}"
                )
            }
        }
    }

    /**
     * 뒷면 편지는 타이핑 중 계속 저장을 트리거하므로 updateMessage(다이얼로그
     * "저장" 클릭 시 1회만 호출)와 달리 짧은 시간에 여러 번 겹쳐 호출될 수
     * 있다. Mutex 없이 매번 저장하면 완료 순서가 뒤바뀌어 나중에 입력한
     * 내용이 먼저 입력한 내용에 덮여 사라질 수 있으므로, updateBackgroundColor와
     * 동일하게 styleWriteMutex로 직렬화하고 획득 시점에 _postcard.value를
     * 다시 읽어서 쓴다.
     */
    fun updateBackRecipientModifier(
        backRecipientModifier: String
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val normalized =
            backRecipientModifier.take(
                BACK_RECIPIENT_MODIFIER_MAX_LENGTH
            )

        val previous =
            currentPostcard.backRecipientModifier

        _postcard.value =
            currentPostcard.copy(
                backRecipientModifier = normalized
            )

        backRecipientModifierSaveJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    styleWriteMutex.withLock {
                        val latest =
                            _postcard.value
                                ?: return@withLock
                        repository.updatePostcardBackRecipientModifier(
                            id = currentPostcard.id,
                            backRecipientModifier =
                                latest.backRecipientModifier
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (
                    _postcard.value?.backRecipientModifier ==
                    normalized
                ) {
                    _postcard.value =
                        _postcard.value?.copy(
                            backRecipientModifier = previous
                        )
                }

                Log.w(
                    TAG,
                    "받는 사람 수식언 저장 실패: ${exception.message}"
                )
            }
        }
    }

    /** updateBackRecipientModifier와 동일한 이유로 styleWriteMutex를 재사용한다. */
    fun updateBackMessage(
        backMessage: String
    ) {
        val currentPostcard =
            _postcard.value
                ?: return

        val normalized =
            backMessage.take(
                BACK_MESSAGE_MAX_LENGTH
            )

        val previous =
            currentPostcard.backMessage

        _postcard.value =
            currentPostcard.copy(
                backMessage = normalized
            )

        backMessageSaveJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    styleWriteMutex.withLock {
                        val latest =
                            _postcard.value
                                ?: return@withLock
                        repository.updatePostcardBackMessage(
                            id = currentPostcard.id,
                            backMessage = latest.backMessage
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (_postcard.value?.backMessage == normalized) {
                    _postcard.value =
                        _postcard.value?.copy(
                            backMessage = previous
                        )
                }

                Log.w(
                    TAG,
                    "뒷면 편지 저장 실패: ${exception.message}"
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

        layoutStyleSaveJob = viewModelScope.launch {
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
                if (_postcard.value?.layoutStyle == normalizedLayoutStyle) {
                    _postcard.value =
                        _postcard.value?.copy(
                            layoutStyle = previousLayoutStyle
                        )
                }
                _layoutUpdateState.value =
                    LayoutUpdateState.Error(
                        exception.message
                            ?: "레이아웃을 저장하지 못했습니다."
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
                                // 다른 스타일 저장과의 경합 방지: 이 순간
                                // _postcard.value에 남아있는 값을 다시 읽어서
                                // 쓴다(호출 당시 캡처한 normalizedScale이
                                // 아니라) — 그 사이 더 최신 조작이 반영됐다면
                                // 그 값을 그대로 유지하고, 재확인(reconfirm)도
                                // 이 값 기준으로 해야 최신 값을 되돌리지 않는다.
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
                } catch (_: Exception) {
                    if (_postcard.value?.messageTextScale == normalizedScale) {
                        _postcard.value =
                            _postcard.value?.copy(
                                messageTextScale = previousScale
                            )
                    }
                    _textScaleSaveErrors.trySend(
                        "글귀 크기를 저장하지 못했어."
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
                } catch (_: Exception) {
                    if (_postcard.value?.backgroundPatternDensity == normalizedDensity) {
                        _postcard.value =
                            _postcard.value?.copy(
                                backgroundPatternDensity =
                                    previousDensity
                            )
                    }
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
                } catch (_: Exception) {
                    if (_postcard.value?.stampPhotoScale == normalizedScale) {
                        _postcard.value =
                            _postcard.value?.copy(
                                stampPhotoScale = previousScale
                            )
                    }
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
                } catch (_: Exception) {
                    if (_postcard.value?.polaroidPhotoScale == normalizedScale) {
                        _postcard.value =
                            _postcard.value?.copy(
                                polaroidPhotoScale = previousScale
                            )
                    }
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
                } catch (_: Exception) {
                    if (_postcard.value?.photoEdgeBlur == normalizedEdgeBlur) {
                        _postcard.value =
                            _postcard.value?.copy(
                                photoEdgeBlur = previousEdgeBlur
                            )
                    }
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
                } catch (_: Exception) {
                    if (
                        _postcard.value?.stampPhotoOffsetX == normalizedOffsetX &&
                        _postcard.value?.stampPhotoOffsetY == normalizedOffsetY
                    ) {
                        _postcard.value =
                            _postcard.value?.copy(
                                stampPhotoOffsetX = previousOffsetX,
                                stampPhotoOffsetY = previousOffsetY
                            )
                    }
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
                } catch (_: Exception) {
                    if (
                        _postcard.value?.polaroidPhotoOffsetX == normalizedOffsetX &&
                        _postcard.value?.polaroidPhotoOffsetY == normalizedOffsetY
                    ) {
                        _postcard.value =
                            _postcard.value?.copy(
                                polaroidPhotoOffsetX = previousOffsetX,
                                polaroidPhotoOffsetY = previousOffsetY
                            )
                    }
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
                } catch (_: Exception) {
                    if (
                        _postcard.value?.tapedFilmPhotoOffsetX == normalizedOffsetX &&
                        _postcard.value?.tapedFilmPhotoOffsetY == normalizedOffsetY
                    ) {
                        _postcard.value =
                            _postcard.value?.copy(
                                tapedFilmPhotoOffsetX = previousOffsetX,
                                tapedFilmPhotoOffsetY = previousOffsetY
                            )
                    }
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
                } catch (_: Exception) {
                    if (_postcard.value?.stampPhotoZoom == normalizedZoom) {
                        _postcard.value =
                            _postcard.value?.copy(
                                stampPhotoZoom = previousZoom
                            )
                    }
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
                } catch (_: Exception) {
                    if (_postcard.value?.polaroidPhotoZoom == normalizedZoom) {
                        _postcard.value =
                            _postcard.value?.copy(
                                polaroidPhotoZoom = previousZoom
                            )
                    }
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
                } catch (_: Exception) {
                    if (_postcard.value?.tapedFilmPhotoZoom == normalizedZoom) {
                        _postcard.value =
                            _postcard.value?.copy(
                                tapedFilmPhotoZoom = previousZoom
                            )
                    }
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

        backgroundColorSaveJob = viewModelScope.launch {
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

        backgroundPatternSaveJob = viewModelScope.launch {
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
                if (_postcard.value?.backgroundPattern == normalizedPattern) {
                    _postcard.value =
                        _postcard.value?.copy(
                            backgroundPattern = previousPattern
                        )
                }
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
     * 슬라이더 계열 저장(saveStampPhotoScale 등), 배경색·배경 패턴·폰트·
     * 레이아웃·날짜 형식 저장(updateBackgroundColor 등), 글귀 저장
     * (updateMessage), 뒷면 편지 저장(updateBackRecipientModifier,
     * updateBackMessage), 스티커·도장 확정 저장(saveEditsAndClearDraft)은
     * DetailScreen의 controlsEnabled가 확인하는 Saving 상태만으로는 뒤로
     * 가기를 막지 못한다 — 아이콘 뒤로 가기 버튼은 enabled=controlsEnabled로
     * 저장 중 클릭을 막지만, 시스템 back(BackHandler)은 이 플래그를 전혀
     * 확인하지 않아 저장이 실제 DAO/파일 쓰기에 닿기 전에도 화면을 나갈 수
     * 있다. 화면 이탈 직전 이 함수로 아직 끝나지 않은 저장들이 완료되기를
     * 기다린 뒤 navigation을 진행해야, ViewModelStore가 clear()되어
     * viewModelScope가 취소되기 전에 마지막 값이 Room/파일에 반영된다.
     * 각 Job은 실패를 자체적으로 롤백하고 CancellationException을 rethrow하므로
     * 여기서는 완료 여부만 기다리면 된다(join은 예외를 전파하지 않는다). 혹시
     * 모를 비정상적 지연으로 navigation이 무기한 멈추지 않도록 상한 시간을 둔다.
     *
     * 마지막으로 stickerCleanupCandidates도 여기서 함께 정리한다(
     * awaitStickerCleanupSweep) — onCleared()는 viewModelScope가 이미
     * 취소된 뒤 호출되므로 그 안에서 정리를 시도하면 아무 파일도 지워지지
     * 않는다.
     *
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
                backgroundColorSaveJob,
                backgroundPatternSaveJob,
                messageFontSaveJob,
                layoutStyleSaveJob,
                dateFormatSaveJob,
                messageUpdateJob,
                backRecipientModifierSaveJob,
                backMessageSaveJob,
                confirmSaveJob
            ).filter { it.isActive }

        if (pendingJobs.isNotEmpty()) {
            withTimeoutOrNull(PENDING_STYLE_SAVE_TIMEOUT_MS.milliseconds) {
                pendingJobs.joinAll()
            }
        }

        awaitStickerCleanupSweep()
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

    private fun uriToLocalStickerFile(
        uri: Uri
    ): File? =
        uri
            .takeIf { cachedUri ->
                cachedUri.scheme == "file"
            }
            ?.path
            ?.let { path ->
                File(path)
            }

    private fun deleteStickerCacheFile(
        file: File
    ) {
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

    fun deleteStickerCacheUri(
        uri: Uri?
    ) {
        val file =
            uri
                ?.let(::uriToLocalStickerFile)
                ?: return

        if (isStickerFileStillReferenced(uri)) {
            stickerCleanupCandidates.add(uri)
            return
        }

        stickerCleanupCandidates.remove(uri)

        viewModelScope.launch(Dispatchers.IO) {
            deleteStickerCacheFile(file)
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
                    } catch (_: Exception) {
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

                _photoStickers.value += newSticker
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

            _photoStickers.value += duplicate
            _selectedStickerId.value = newId
            scheduleDraftAutosave()
            return
        }

        val sourceUri = original.removedBgUri

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

            _photoStickers.value += duplicate
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
        sealOverlays: List<PostcardImageExporter.SealOverlay> = emptyList(),
        textStickerOverlays: List<PostcardImageExporter.TextStickerOverlay> = emptyList(),
        maskingTapeOverlays: List<PostcardImageExporter.MaskingTapeOverlay> = emptyList(),
        labelStickerOverlays: List<PostcardImageExporter.LabelStickerOverlay> = emptyList()
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
                            sealOverlays = sealOverlays,
                            doodleStrokes = _doodleStrokes.value,
                            textStickerOverlays = textStickerOverlays,
                            maskingTapeOverlays = maskingTapeOverlays,
                            labelStickerOverlays = labelStickerOverlays
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
        sealOverlays: List<PostcardImageExporter.SealOverlay> = emptyList(),
        textStickerOverlays: List<PostcardImageExporter.TextStickerOverlay> = emptyList(),
        maskingTapeOverlays: List<PostcardImageExporter.MaskingTapeOverlay> = emptyList(),
        labelStickerOverlays: List<PostcardImageExporter.LabelStickerOverlay> = emptyList()
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
                            sealOverlays = sealOverlays,
                            doodleStrokes = _doodleStrokes.value,
                            textStickerOverlays = textStickerOverlays,
                            maskingTapeOverlays = maskingTapeOverlays,
                            labelStickerOverlays = labelStickerOverlays
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

    /**
     * 완성된 엽서를 미래 날짜로 봉인해 보낸다. 삭제와 달리 어떤 파일도
     * 지우지 않는다 — futureMailState/futureMailDeliverAt만 바뀌어 갤러리
     * 조회에서 제외될 뿐, 사진·스티커·도장 등 실제 자산은 그대로 보존된다.
     * deletionManager를 재사용하지 않는 이유가 바로 이것이다.
     */
    fun sendToFuture(deliverAtMillis: Long) {
        val currentPostcard =
            _postcard.value
                ?: return

        if (_futureMailSendState.value is FutureMailSendState.Sending) {
            return
        }

        _futureMailSendState.value = FutureMailSendState.Sending

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.sendToFutureMailbox(
                        id = currentPostcard.id,
                        deliverAt = deliverAtMillis
                    )
                }

                _futureMailSendState.value = FutureMailSendState.Sent
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _futureMailSendState.value =
                    FutureMailSendState.Error(
                        "보내지 못했어. 다시 시도해줘."
                    )
            }
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
            "SPECKLE",
            "HEISEI" -> backgroundPattern

            else -> "NONE"
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
