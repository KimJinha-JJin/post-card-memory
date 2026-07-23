package com.postcardmemory.utils

import android.content.Context
import com.postcardmemory.ui.detail.PostcardEditDraft
import com.postcardmemory.ui.detail.parsePostcardEditDraft
import com.postcardmemory.ui.detail.serialize
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 완성 저장본(Room, sticker_states, seal_states)과 완전히 분리된
 * 스티커·도장 편집 초안 저장소. filesDir 하위 전용 폴더만 사용하며
 * 완성본이 쓰는 파일은 절대 건드리지 않는다.
 *
 * 공개 API는 Context를 받고, 실제 파일 로직은 filesDir를 직접 받는
 * internal 오버로드에 위임한다 — 이 internal 함수들은 Context 없이
 * java.io.File만으로 순수 JUnit에서 검증할 수 있다.
 */
object PostcardDraftStorage {

    private const val DRAFT_DIR_NAME = "drafts/edit_state"
    private const val DRAFT_STICKER_BG_DIR_NAME = "draft_sticker_bgs"

    internal fun draftFileNameFor(postcardId: Long): String =
        "$postcardId.draft.txt"

    private fun draftDir(filesDir: File): File =
        File(filesDir, DRAFT_DIR_NAME)

    /** internal: PostcardDeletionManager가 삭제 전 존재 여부 확인에 재사용한다. */
    internal fun draftFile(filesDir: File, postcardId: Long): File =
        File(draftDir(filesDir), draftFileNameFor(postcardId))

    /**
     * 초안이 참조하는 누끼 PNG를 보관하는 초안 전용 영구 디렉터리(postcardId별).
     * confirm-save 전용 sticker_bgs/<postcardId>/와 완전히 분리되어 있어,
     * 초안이 폐기/승격/손상될 때 이 디렉터리째로 지워도 확정 저장본에는
     * 영향이 없다.
     */
    fun draftStickerBackgroundDir(
        context: Context,
        postcardId: Long
    ): File = draftStickerBackgroundDir(context.filesDir, postcardId)

    internal fun draftStickerBackgroundDir(
        filesDir: File,
        postcardId: Long
    ): File = File(
        File(filesDir, DRAFT_STICKER_BG_DIR_NAME),
        postcardId.toString()
    )

    fun saveDraftAtomically(
        context: Context,
        draft: PostcardEditDraft
    ): Boolean = saveDraftAtomically(context.filesDir, draft)

    internal fun saveDraftAtomically(
        filesDir: File,
        draft: PostcardEditDraft
    ): Boolean {
        val dir = draftDir(filesDir)

        if (!dir.exists() && !dir.mkdirs()) {
            return false
        }

        val target = draftFile(filesDir, draft.postcardId)
        val tempFile = File(dir, "${draft.postcardId}.draft.tmp")

        return try {
            FileOutputStream(tempFile).use { outputStream ->
                outputStream.write(
                    draft.serialize().toByteArray(Charsets.UTF_8)
                )
                outputStream.flush()
            }

            tempFile.renameTo(target)
        } catch (exception: IOException) {
            tempFile.delete()
            false
        }
    }

    fun loadDraft(
        context: Context,
        postcardId: Long
    ): PostcardEditDraft? = loadDraft(context.filesDir, postcardId)

    internal fun loadDraft(
        filesDir: File,
        postcardId: Long
    ): PostcardEditDraft? {
        val file = draftFile(filesDir, postcardId)

        if (!file.exists()) {
            return null
        }

        val text = runCatching {
            file.readText(Charsets.UTF_8)
        }.getOrNull()

        if (text == null) {
            file.delete()
            draftStickerBackgroundDir(filesDir, postcardId).deleteRecursively()
            return null
        }

        val parsed = parsePostcardEditDraft(text)

        if (parsed == null) {
            // 손상된 초안은 무한 복구 팝업을 막기 위해 즉시 격리(삭제)한다.
            // 파싱이 실패해 어떤 파일을 참조했는지 알 수 없으므로, 이
            // postcardId가 단독 소유하는 초안 전용 폴더를 통째로 정리한다.
            file.delete()
            draftStickerBackgroundDir(filesDir, postcardId).deleteRecursively()
        }

        return parsed
    }

    fun deleteDraft(context: Context, postcardId: Long) {
        deleteDraft(context.filesDir, postcardId)
    }

    /**
     * 초안 텍스트 파일과 초안 소유 누끼 디렉터리를 함께 지운다 — 초안을
     * 지우는 모든 호출부(원래대로/완료 저장 성공/엽서 삭제)가 이 함수 하나만
     * 부르면 초안이 소유한 파일까지 자동으로 정리되도록 한 곳에 모았다.
     */
    internal fun deleteDraft(filesDir: File, postcardId: Long) {
        val file = draftFile(filesDir, postcardId)
        if (file.exists()) {
            file.delete()
        }
        draftStickerBackgroundDir(filesDir, postcardId).deleteRecursively()
    }
}
