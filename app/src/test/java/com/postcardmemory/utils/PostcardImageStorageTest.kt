package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 5일차: 중심 사진 교체 성공 후 이전 파일을 정리하는 deleteIfOwnedByApp을
 * 검증한다. imagePath는 항상 filesDir 내부 경로이지만, 외부 갤러리 원본이나
 * 다른 위치를 실수로 지우지 않는지가 핵심이라 그 경계 검사를 직접 확인한다.
 */
class PostcardImageStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun deleteIfOwnedByApp_deletesFileInsideFilesDir() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = File(filesDir, "postcards/postcard_old.jpg")
        imageFile.parentFile?.mkdirs()
        imageFile.writeText("old-image-bytes")

        PostcardImageStorage.deleteIfOwnedByApp(filesDir, imageFile.path)

        assertFalse(imageFile.exists())
    }

    @Test
    fun deleteIfOwnedByApp_doesNotDeleteFileOutsideFilesDir() {
        val filesDir = tempFolder.newFolder("files")
        val externalDir = tempFolder.newFolder("external_gallery")
        val externalImage = File(externalDir, "user_original.jpg")
        externalImage.writeText("user-original-bytes")

        PostcardImageStorage.deleteIfOwnedByApp(filesDir, externalImage.path)

        // 사용자의 갤러리 원본(앱 filesDir 밖)은 절대 지우면 안 된다.
        assertTrue(externalImage.exists())
    }

    @Test
    fun deleteIfOwnedByApp_nullPath_doesNotThrow() {
        val filesDir = tempFolder.newFolder("files")

        PostcardImageStorage.deleteIfOwnedByApp(filesDir, null)
    }

    @Test
    fun deleteIfOwnedByApp_blankPath_doesNotThrow() {
        val filesDir = tempFolder.newFolder("files")

        PostcardImageStorage.deleteIfOwnedByApp(filesDir, "")
    }

    @Test
    fun deleteIfOwnedByApp_alreadyMissingFile_doesNotThrow() {
        val filesDir = tempFolder.newFolder("files")
        val missingFile = File(filesDir, "postcards/already_gone.jpg")

        // 이미 없는 파일을 다시 지우려 해도(멱등) 예외가 나지 않아야 한다.
        PostcardImageStorage.deleteIfOwnedByApp(filesDir, missingFile.path)
    }

    @Test
    fun deleteIfOwnedByApp_pathEqualToPreviousPath_stillSafeToCallButHasNoUnintendedEffect() {
        // 호출부(DetailViewModel)가 previousImagePath == newImagePath일 때
        // 아예 호출하지 않지만, 혹시 호출되더라도 그 파일이 여전히 앱 내부
        // 경로면 정상적으로 지워지는 것이 맞다(스스로 자기 자신 삭제) —
        // "동일 경로면 호출하지 않는다"는 결정은 호출부 책임이다.
        val filesDir = tempFolder.newFolder("files")
        val samePathFile = File(filesDir, "postcards/same.jpg")
        samePathFile.parentFile?.mkdirs()
        samePathFile.writeText("bytes")

        PostcardImageStorage.deleteIfOwnedByApp(filesDir, samePathFile.path)

        assertFalse(samePathFile.exists())
    }
}
