package com.postcardmemory.utils

import com.postcardmemory.ui.detail.PostcardTemplate
import com.postcardmemory.ui.detail.PostcardTemplateStyle
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * internal filesDir 오버로드만 검증한다(bitmap을 다루는 previewFile/
 * savePreviewAtomically의 Context 버전은 android.graphics.Bitmap에 의존해
 * Robolectric 없는 이 프로젝트의 순수 JUnit 환경에서 호출할 수 없다 —
 * PostcardDraftStorageTest와 같은 방식).
 */
class PostcardTemplateStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun style() =
        PostcardTemplateStyle(
            layoutStyle = "STAMP",
            backgroundColorArgb = 0xFFFFFBF7L,
            backgroundPattern = "NONE",
            backgroundPatternDensity = 1f,
            messageFont = "SERIF",
            dateFormat = "DOT",
            messageTextScale = 1f,
            dateTextScale = 1f,
            photoEdgeBlur = 0f,
            stampPhotoScale = 1f,
            stampPhotoOffsetX = 0f,
            stampPhotoOffsetY = 0f,
            stampPhotoZoom = 1f,
            polaroidPhotoScale = 1f,
            polaroidPhotoOffsetX = 0f,
            polaroidPhotoOffsetY = 0f,
            polaroidPhotoZoom = 1f,
            tapedFilmPhotoOffsetX = 0f,
            tapedFilmPhotoOffsetY = 0f,
            tapedFilmPhotoZoom = 1f
        )

    @Test
    fun saveThenLoad_roundTrips() {
        val filesDir = tempFolder.newFolder("files")
        val template = PostcardTemplate(name = "나의 템플릿 1", style = style())

        val saved = PostcardTemplateStorage.saveTemplateAtomically(filesDir, template)
        val loaded = PostcardTemplateStorage.loadAllTemplates(filesDir)

        assertTrue(saved)
        assertEquals(1, loaded.size)
        assertEquals(template.id, loaded.single().id)
        assertEquals(template.name, loaded.single().name)
    }

    @Test
    fun save_leavesNoTempFileBehind() {
        val filesDir = tempFolder.newFolder("files")
        val template = PostcardTemplate(name = "템플릿", style = style())

        PostcardTemplateStorage.saveTemplateAtomically(filesDir, template)

        val templatesDir = PostcardTemplateStorage.templateFile(filesDir, template.id).parentFile!!
        val tmpFiles = templatesDir.listFiles { file -> file.name.endsWith(".tmp") }

        assertTrue(tmpFiles == null || tmpFiles.isEmpty())
    }

    @Test
    fun overwrite_sameId_replacesContent_otherTemplatesUntouched() {
        val filesDir = tempFolder.newFolder("files")
        val original = PostcardTemplate(id = "fixed-id", name = "원래 이름", style = style())
        val other = PostcardTemplate(id = "other-id", name = "다른 템플릿", style = style())

        PostcardTemplateStorage.saveTemplateAtomically(filesDir, original)
        PostcardTemplateStorage.saveTemplateAtomically(filesDir, other)

        val renamed = original.copy(name = "바뀐 이름", updatedAtMillis = original.updatedAtMillis + 1)
        PostcardTemplateStorage.saveTemplateAtomically(filesDir, renamed)

        val loaded = PostcardTemplateStorage.loadAllTemplates(filesDir)

        assertEquals(2, loaded.size)
        assertEquals("바뀐 이름", loaded.first { it.id == "fixed-id" }.name)
        assertEquals("다른 템플릿", loaded.first { it.id == "other-id" }.name)
    }

    @Test
    fun corruptedTemplateFile_isSkipped_othersStillLoad() {
        val filesDir = tempFolder.newFolder("files")
        val good = PostcardTemplate(id = "good-id", name = "정상 템플릿", style = style())
        PostcardTemplateStorage.saveTemplateAtomically(filesDir, good)

        val corruptedFile = PostcardTemplateStorage.templateFile(filesDir, "bad-id")
        corruptedFile.parentFile?.mkdirs()
        corruptedFile.writeText("완전히 깨진 내용")

        val loaded = PostcardTemplateStorage.loadAllTemplates(filesDir)

        assertEquals(1, loaded.size)
        assertEquals("good-id", loaded.single().id)
    }

    @Test
    fun loadAllTemplates_missingDirectory_returnsEmptyList_doesNotThrow() {
        val filesDir = tempFolder.newFolder("files")

        val loaded = PostcardTemplateStorage.loadAllTemplates(filesDir)

        assertTrue(loaded.isEmpty())
    }

    @Test
    fun deleteTemplate_removesDataAndPreview_otherTemplateFilesUntouched() {
        val filesDir = tempFolder.newFolder("files")
        val target = PostcardTemplate(id = "to-delete", name = "지울 템플릿", style = style())
        val other = PostcardTemplate(id = "keep", name = "남길 템플릿", style = style())

        PostcardTemplateStorage.saveTemplateAtomically(filesDir, target)
        PostcardTemplateStorage.saveTemplateAtomically(filesDir, other)

        val previewFile = PostcardTemplateStorage.previewFile(filesDir, target.id)
        previewFile.parentFile?.mkdirs()
        previewFile.writeText("fake-png-bytes")

        val otherPreview = PostcardTemplateStorage.previewFile(filesDir, other.id)
        otherPreview.parentFile?.mkdirs()
        otherPreview.writeText("other-fake-png-bytes")

        PostcardTemplateStorage.deleteTemplate(filesDir, target.id)

        assertFalse(PostcardTemplateStorage.templateFile(filesDir, target.id).exists())
        assertFalse(previewFile.exists())

        assertTrue(PostcardTemplateStorage.templateFile(filesDir, other.id).exists())
        assertTrue(otherPreview.exists())
    }

    @Test
    fun deleteTemplate_alreadyMissing_doesNotThrow() {
        val filesDir = tempFolder.newFolder("files")

        PostcardTemplateStorage.deleteTemplate(filesDir, "never-existed")
    }
}
