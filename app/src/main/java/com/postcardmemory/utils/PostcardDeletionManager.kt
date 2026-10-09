package com.postcardmemory.utils

import android.content.Context
import com.postcardmemory.data.Postcard
import com.postcardmemory.data.PostcardRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/** 파일/디렉터리 하나를 정리하려다 실패한 경우의 기록. */
data class AssetDeletionFailure(
    val assetName: String,
    val path: String,
    val reason: String
)

/**
 * 엽서 한 장을 삭제한 결과. Boolean 하나로 뭉뚱그리지 않고, DB 삭제 성공
 * 여부와 파일 정리 결과(정리됨/원래 없었음/실패함)를 각각 구분해 호출자가
 * 부분 실패를 조용히 숨기지 않게 한다.
 */
data class PostcardDeletionResult(
    val postcardId: Long,
    val databaseDeleted: Boolean,
    val deletedAssets: List<String>,
    val missingAssets: List<String>,
    val failedAssets: List<AssetDeletionFailure>
) {
    val isFullSuccess: Boolean
        get() = databaseDeleted && failedAssets.isEmpty()
}

/**
 * 엽서 한 장이 소유하는 자산(기본 이미지, 배경 이미지, 꾸미기 요소별 확정
 * 상태 파일, 편집 초안 및 초안 전용 누끼 디렉터리(2일차), 확정 누끼 디렉터리,
 * 카메라 스티커 원본 디렉터리)을 filesDir 기준으로 정리한다. Context 없이
 * java.io.File만으로 동작해 PostcardDraftStorage/ConfirmedEditStateStorage와
 * 같은 방식으로 순수 JUnit에서 TemporaryFolder로 검증할 수 있다.
 *
 * 이미 존재하지 않는 파일은 실패가 아니라 missingAssets로 기록한다(멱등
 * 재시도가 안전함). postcard.imagePath/backgroundImagePath처럼 DB에 저장된
 * 문자열 경로는 실제로 filesDir 하위인지 확인한 뒤에만 삭제를 시도하고,
 * 아니라면 failedAssets에 사유와 함께 남기고 건드리지 않는다.
 */
internal fun cleanupPostcardOwnedAssets(
    filesDir: File,
    postcard: Postcard
): PostcardDeletionResult {
    val deleted = mutableListOf<String>()
    val missing = mutableListOf<String>()
    val failed = mutableListOf<AssetDeletionFailure>()

    fun deleteFile(assetName: String, file: File) {
        if (!file.exists()) {
            missing += assetName
            return
        }
        val ok = runCatching { file.delete() }.getOrDefault(false)
        if (ok) {
            deleted += assetName
        } else {
            failed += AssetDeletionFailure(
                assetName = assetName,
                path = file.path,
                reason = "파일 삭제 실패"
            )
        }
    }

    fun deleteDir(assetName: String, dir: File) {
        if (!dir.exists()) {
            missing += assetName
            return
        }
        val ok = runCatching { dir.deleteRecursively() }.getOrDefault(false)
        if (ok) {
            deleted += assetName
        } else {
            failed += AssetDeletionFailure(
                assetName = assetName,
                path = dir.path,
                reason = "디렉터리 삭제 실패"
            )
        }
    }

    // 1. 기본 이미지 — DB에 저장된 경로라 실제로 앱 내부 경로인지 확인 후 삭제.
    val imageFile = File(postcard.imagePath)
    if (isInsideDirectory(filesDir, imageFile)) {
        deleteFile("image", imageFile)
    } else {
        failed += AssetDeletionFailure(
            assetName = "image",
            path = imageFile.path,
            reason = "앱 내부 경로가 아니라 건너뜀"
        )
    }

    // 2. 배경 이미지 — 없을 수 있음(색/패턴 배경만 쓰는 엽서).
    val backgroundPath = postcard.backgroundImagePath
    if (backgroundPath.isNullOrBlank()) {
        missing += "backgroundImage"
    } else {
        val backgroundFile = File(backgroundPath)
        if (isInsideDirectory(filesDir, backgroundFile)) {
            deleteFile("backgroundImage", backgroundFile)
        } else {
            failed += AssetDeletionFailure(
                assetName = "backgroundImage",
                path = backgroundFile.path,
                reason = "앱 내부 경로가 아니라 건너뜀"
            )
        }
    }

    // 3. 꾸미기 요소별 확정 상태 파일. DetailViewModel의 persist*EditState가
    // 요소마다 <디렉터리>/<postcardId>.txt 하나씩 쓰므로, 여기서도 삭제되는
    // 엽서의 id에 해당하는 파일만 지운다(다른 엽서 파일은 건드리지 않음).
    // 저장과 같은 DecorationStateFile 목록을 순회하므로 요소를 새로 추가하면
    // 여기에도 자동으로 포함된다(DecorationDirectoryContractTest가 보호).
    DecorationStateFile.entries.forEach { stateFile ->
        deleteFile(
            stateFile.assetName,
            stateFile.file(filesDir, postcard.id)
        )
    }

    // 4. 편집 초안 — PostcardDraftStorage.deleteDraft가 초안 텍스트와
    // 2일차에 추가된 초안 전용 누끼 디렉터리(draft_sticker_bgs/<id>/)를
    // 함께 정리한다(이미 검증된 경로 계산을 재사용, 경로 중복 정의 방지).
    val draftFile =
        PostcardDraftStorage.draftFile(filesDir, postcard.id)
    val draftBgDir =
        PostcardDraftStorage.draftStickerBackgroundDir(filesDir, postcard.id)
    val draftExistedBefore =
        draftFile.exists() || draftBgDir.exists()

    val draftCleanupOk =
        runCatching {
            PostcardDraftStorage.deleteDraft(filesDir, postcard.id)
        }.getOrDefault(false)

    when {
        !draftCleanupOk ->
            failed += AssetDeletionFailure(
                assetName = "draft",
                path = draftFile.path,
                reason = "초안 정리 실패"
            )

        draftExistedBefore ->
            deleted += "draft"

        else ->
            missing += "draft"
    }

    // 5~7. 엽서별 꾸미기 자산 디렉터리 — 확정 누끼(sticker_bgs/<id>/),
    // 스티커 원본(sticker_originals/<id>/: 카메라 촬영분과 57일차부터 Photo
    // Picker로 고른 원본까지, 라벨 "cameraStickerOriginals"는 기존 테스트 호환을
    // 위해 그대로 둔다), 마스킹테이프 사진 원본(masking_tape_photos/<id>/,
    // 57일차 URI 영속성 수정으로 신설). 모두 postcardId 전용 하위 디렉터리라
    // 재귀 삭제가 안전하다(각 루트 자체는 건드리지 않음).
    PostcardAssetDirectory.entries.forEach { assetDirectory ->
        deleteDir(
            assetDirectory.deletionAssetName,
            assetDirectory.directory(filesDir, postcard.id)
        )
    }

    return PostcardDeletionResult(
        postcardId = postcard.id,
        databaseDeleted = true,
        deletedAssets = deleted,
        missingAssets = missing,
        failedAssets = failed
    )
}

/**
 * DB-우선 삭제 gate. [deleteDatabaseRow]가 성공한 뒤에만 [cleanupOwnedAssets]를
 * 부르고, 일반 오류로 실패하면 파일을 하나도 건드리지 않은 실패 결과를 돌려준다.
 *
 * coroutine 취소는 실패 결과로 바꾸지 않고 그대로 다시 던진다 — 취소를 "DB 삭제
 * 실패"로 삼키면 이미 취소된 호출자가 남은 엽서까지 계속 삭제를 시도하게 된다.
 * 이때도 파일 정리 단계로는 넘어가지 않는다.
 *
 * Context 없이 순수 JUnit에서 이 gate만 검증할 수 있도록 Manager에서 떼어 둔다.
 */
internal suspend fun deletePostcardDatabaseFirst(
    postcard: Postcard,
    deleteDatabaseRow: suspend (Long) -> Unit,
    cleanupOwnedAssets: (Postcard) -> PostcardDeletionResult
): PostcardDeletionResult {
    val databaseDeleted =
        try {
            deleteDatabaseRow(postcard.id)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            false
        }

    if (!databaseDeleted) {
        return PostcardDeletionResult(
            postcardId = postcard.id,
            databaseDeleted = false,
            deletedAssets = emptyList(),
            missingAssets = emptyList(),
            failedAssets = emptyList()
        )
    }

    return cleanupOwnedAssets(postcard)
}

/**
 * 상세 화면 삭제와 갤러리 삭제가 동일한 정책을 쓰도록 모은 공통 계층.
 *
 * 정책: Room 삭제가 성공한 뒤에만 파일을 정리한다. 파일을 먼저 지우고
 * Room 삭제가 실패하면 이미지가 사라진 "깨진 엽서" 레코드가 남을 수
 * 있기 때문이다. Room 삭제 자체가 실패하면 어떤 파일도 건드리지 않고
 * 즉시 결과를 반환해 재시도가 안전하게 남는다.
 */
@Singleton
class PostcardDeletionManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: PostcardRepository
) {

    suspend fun deletePostcard(
        postcard: Postcard
    ): PostcardDeletionResult =
        deletePostcardDatabaseFirst(
            postcard = postcard,
            deleteDatabaseRow = { id -> repository.deletePostcardById(id) },
            cleanupOwnedAssets = { cleanupPostcardOwnedAssets(context.filesDir, it) }
        )

    suspend fun deletePostcards(
        postcards: List<Postcard>
    ): List<PostcardDeletionResult> =
        postcards.map { postcard -> deletePostcard(postcard) }
}
