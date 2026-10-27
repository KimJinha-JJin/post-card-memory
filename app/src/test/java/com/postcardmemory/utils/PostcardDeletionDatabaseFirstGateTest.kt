package com.postcardmemory.utils

import com.postcardmemory.data.Postcard
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * deletePostcardDatabaseFirst(DB-우선 삭제 gate)를 순수 JUnit으로 검증한다.
 *
 * 같은 gate를 실제 Room으로 확인하는 PostcardDeletionOrchestrationTest는 실제
 * filesDir에 써서 실사용 기기에서 실행할 수 없는 FORBIDDEN 등급이라, 이 파일이
 * 실행 가능한 보호선이다. 파일 정리는 실제 cleanupPostcardOwnedAssets를
 * TemporaryFolder에 대고 부른다.
 */
class PostcardDeletionDatabaseFirstGateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun ownedImage(filesDir: File): File =
        File(filesDir, "postcard_images/7.jpg").apply {
            parentFile?.mkdirs()
            writeText("image")
        }

    @Test
    fun cancellationDuringDatabaseDelete_isRethrownAndSkipsFileCleanup() {
        val filesDir = tempFolder.root
        val image = ownedImage(filesDir)
        val postcard = Postcard(id = 7L, imagePath = image.path, title = "postcard-7")
        var cleanupCalled = false

        try {
            runBlocking {
                deletePostcardDatabaseFirst(
                    postcard = postcard,
                    deleteDatabaseRow = { throw CancellationException("cancelled during delete") },
                    cleanupOwnedAssets = {
                        cleanupCalled = true
                        cleanupPostcardOwnedAssets(filesDir, it)
                    }
                )
            }
            fail("취소가 실패 결과로 삼켜지면 안 된다")
        } catch (cancellation: CancellationException) {
            assertEquals("cancelled during delete", cancellation.message)
        }

        assertFalse(cleanupCalled)
        assertTrue(image.exists())
    }

    @Test
    fun ordinaryDatabaseError_returnsFailureAndDeletesNoFiles() = runBlocking {
        val filesDir = tempFolder.root
        val image = ownedImage(filesDir)
        val postcard = Postcard(id = 7L, imagePath = image.path, title = "postcard-7")
        var cleanupCalled = false

        val result = deletePostcardDatabaseFirst(
            postcard = postcard,
            deleteDatabaseRow = { throw IOException("injected delete failure") },
            cleanupOwnedAssets = {
                cleanupCalled = true
                cleanupPostcardOwnedAssets(filesDir, it)
            }
        )

        assertFalse(result.databaseDeleted)
        assertFalse(result.isFullSuccess)
        assertTrue(result.deletedAssets.isEmpty())
        assertTrue(result.failedAssets.isEmpty())
        assertFalse(cleanupCalled)
        assertTrue(image.exists())
    }
}
