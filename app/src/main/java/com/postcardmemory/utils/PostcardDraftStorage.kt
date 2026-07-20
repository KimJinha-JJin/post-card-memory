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

    internal fun draftFileNameFor(postcardId: Long): String =
        "$postcardId.draft.txt"

    private fun draftDir(filesDir: File): File =
        File(filesDir, DRAFT_DIR_NAME)

    private fun draftFile(filesDir: File, postcardId: Long): File =
        File(draftDir(filesDir), draftFileNameFor(postcardId))

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
            return null
        }

        val parsed = parsePostcardEditDraft(text)

        if (parsed == null) {
            // 손상된 초안은 무한 복구 팝업을 막기 위해 즉시 격리(삭제)한다.
            file.delete()
        }

        return parsed
    }

    fun deleteDraft(context: Context, postcardId: Long) {
        deleteDraft(context.filesDir, postcardId)
    }

    internal fun deleteDraft(filesDir: File, postcardId: Long) {
        val file = draftFile(filesDir, postcardId)
        if (file.exists()) {
            file.delete()
        }
    }
}
