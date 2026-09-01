package com.postcardmemory.utils

import android.content.Context
import android.graphics.Bitmap
import com.postcardmemory.ui.detail.PostcardTemplate
import com.postcardmemory.ui.detail.parsePostcardTemplate
import com.postcardmemory.ui.detail.serialize
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 사용자 템플릿 전용 독립 저장소. Room을 전혀 쓰지 않고 filesDir 하위
 * 전용 폴더만 사용한다(PostcardDraftStorage와 동일한 방식) — 템플릿
 * 하나가 손상돼도 나머지 템플릿이나 엽서 데이터에는 영향이 없다.
 *
 * 내장 템플릿은 여기서 다루지 않는다(코드에 고정으로 정의되어 파일로
 * 저장되지 않음). 이 저장소는 오직 사용자가 저장한 템플릿만 다룬다.
 */
object PostcardTemplateStorage {

    private const val ROOT_DIR_NAME = "postcard_templates"
    private const val TEMPLATES_DIR_NAME = "templates"
    private const val PREVIEWS_DIR_NAME = "previews"
    private const val TEMPLATE_FILE_EXTENSION = ".txt"
    private const val PREVIEW_FILE_EXTENSION = ".png"

    private fun templatesDir(filesDir: File): File =
        File(File(filesDir, ROOT_DIR_NAME), TEMPLATES_DIR_NAME)

    private fun previewsDir(filesDir: File): File =
        File(File(filesDir, ROOT_DIR_NAME), PREVIEWS_DIR_NAME)

    internal fun templateFile(filesDir: File, templateId: String): File =
        File(templatesDir(filesDir), "$templateId$TEMPLATE_FILE_EXTENSION")

    fun previewFile(context: Context, templateId: String): File =
        previewFile(context.filesDir, templateId)

    internal fun previewFile(filesDir: File, templateId: String): File =
        File(previewsDir(filesDir), "$templateId$PREVIEW_FILE_EXTENSION")

    fun saveTemplateAtomically(
        context: Context,
        template: PostcardTemplate
    ): Boolean = saveTemplateAtomically(context.filesDir, template)

    /** temp 파일에 먼저 쓰고 rename하는 원자적 저장 — 쓰다가 중단돼도 기존 정상 템플릿은 그대로 남는다. */
    internal fun saveTemplateAtomically(
        filesDir: File,
        template: PostcardTemplate
    ): Boolean {
        val dir = templatesDir(filesDir)

        if (!dir.exists() && !dir.mkdirs()) {
            return false
        }

        val target = templateFile(filesDir, template.id)
        val tempFile = File(dir, "${template.id}.tmp")

        return try {
            FileOutputStream(tempFile).use { outputStream ->
                outputStream.write(
                    template.serialize().toByteArray(Charsets.UTF_8)
                )
                outputStream.flush()
            }

            AtomicFileReplace.replace(tempFile, target)
        } catch (_: IOException) {
            tempFile.delete()
            false
        }
    }

    fun savePreviewAtomically(
        context: Context,
        templateId: String,
        bitmap: Bitmap
    ): Boolean = savePreviewAtomically(context.filesDir, templateId, bitmap)

    internal fun savePreviewAtomically(
        filesDir: File,
        templateId: String,
        bitmap: Bitmap
    ): Boolean {
        val dir = previewsDir(filesDir)

        if (!dir.exists() && !dir.mkdirs()) {
            return false
        }

        val target = previewFile(filesDir, templateId)
        val tempFile = File(dir, "$templateId.tmp")

        return try {
            FileOutputStream(tempFile).use { outputStream ->
                val saved =
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()

                if (!saved) {
                    throw IOException("템플릿 미리보기 압축에 실패했습니다.")
                }
            }

            AtomicFileReplace.replace(tempFile, target)
        } catch (_: IOException) {
            tempFile.delete()
            false
        }
    }

    fun loadAllTemplates(context: Context): List<PostcardTemplate> =
        loadAllTemplates(context.filesDir)

    /**
     * templates/ 아래 각 파일을 독립적으로 파싱한다. 손상된 파일 하나는
     * 조용히 건너뛰고(null) 나머지는 정상적으로 반환한다 — 한 템플릿의
     * 손상이 전체 목록을 막지 않는다.
     */
    internal fun loadAllTemplates(filesDir: File): List<PostcardTemplate> {
        val dir = templatesDir(filesDir)
        val files =
            dir.listFiles { file ->
                file.isFile && file.name.endsWith(TEMPLATE_FILE_EXTENSION)
            } ?: return emptyList()

        return files.mapNotNull { file ->
            val text =
                runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
                    ?: return@mapNotNull null

            parsePostcardTemplate(text)
        }
    }

    fun deleteTemplate(context: Context, templateId: String): Boolean =
        deleteTemplate(context.filesDir, templateId)

    /** 템플릿 데이터 파일과 미리보기 파일만 지운다. 다른 템플릿·엽서 파일은 건드리지 않는다. */
    internal fun deleteTemplate(filesDir: File, templateId: String): Boolean {
        val file = templateFile(filesDir, templateId)
        val fileDeleted = !file.exists() || file.delete()

        val preview = previewFile(filesDir, templateId)
        val previewDeleted = !preview.exists() || preview.delete()

        return fileDeleted && previewDeleted
    }
}
