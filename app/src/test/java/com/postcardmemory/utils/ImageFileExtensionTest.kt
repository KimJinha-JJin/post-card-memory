package com.postcardmemory.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 사진 스티커·사진 테이프 원본을 앱 폴더로 복사할 때 붙이는 확장자. 파일 이름이 아니라
 * MIME 타입으로만 정한다(파일 이름의 점·대소문자는 결과에 영향이 없다).
 */
class ImageFileExtensionTest {

    @Test
    fun knownImageTypesKeepTheirOwnExtension() {
        assertEquals(".png", imageFileExtensionForMimeType("image/png"))
        assertEquals(".webp", imageFileExtensionForMimeType("image/webp"))
        assertEquals(".heic", imageFileExtensionForMimeType("image/heic"))
        assertEquals(".heic", imageFileExtensionForMimeType("image/heif"))
        assertEquals(".jpg", imageFileExtensionForMimeType("image/jpeg"))
    }

    @Test
    fun mimeTypeCaseDoesNotMatter() {
        assertEquals(".png", imageFileExtensionForMimeType("IMAGE/PNG"))
        assertEquals(".heic", imageFileExtensionForMimeType("Image/HEIF"))
    }

    @Test
    fun unknownEmptyOrMissingTypesFallBackToJpg() {
        listOf(null, "", "image/gif", "application/octet-stream", "image/png; charset=binary", "png")
            .forEach { mimeType ->
                assertEquals("mimeType=$mimeType", ".jpg", imageFileExtensionForMimeType(mimeType))
            }
    }
}
