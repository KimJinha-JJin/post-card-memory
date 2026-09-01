package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PostcardTempCleanupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun cleanup_deletesOnlyFilesAtLeastSevenDaysOld() {
        val filesDir = tempFolder.newFolder("files")
        val now = 2_000_000_000_000L
        val staleFile = tempFile(filesDir, "stale.jpg")
        val boundaryFile = tempFile(filesDir, "boundary.jpg")
        val recentFile = tempFile(filesDir, "recent.jpg")

        setModified(staleFile, now - POSTCARD_TEMP_FILE_MAX_AGE_MILLIS - 1L)
        setModified(boundaryFile, now - POSTCARD_TEMP_FILE_MAX_AGE_MILLIS)
        setModified(recentFile, now - POSTCARD_TEMP_FILE_MAX_AGE_MILLIS + 1L)

        val result = PostcardTempCleanup.cleanup(filesDir, now)

        assertEquals(
            setOf(staleFile.path, boundaryFile.path),
            result.deletedFiles.toSet()
        )
        assertTrue(result.failedFiles.isEmpty())
        assertFalse(staleFile.exists())
        assertFalse(boundaryFile.exists())
        assertTrue(recentFile.exists())
    }

    @Test
    fun cleanup_keepsCurrentActiveTempAndFilesOutsideTempDirectory() {
        val filesDir = tempFolder.newFolder("files")
        val now = 2_000_000_000_000L
        val activeTemp = tempFile(filesDir, "active.jpg")
        val postcard = File(filesDir, "postcards/postcard_1.jpg").apply {
            parentFile?.mkdirs()
            writeText("postcard")
        }

        setModified(activeTemp, now)
        setModified(postcard, now - POSTCARD_TEMP_FILE_MAX_AGE_MILLIS - 1L)

        val result = PostcardTempCleanup.cleanup(filesDir, now)

        assertTrue(result.deletedFiles.isEmpty())
        assertTrue(result.failedFiles.isEmpty())
        assertTrue(activeTemp.exists())
        assertTrue(postcard.exists())
    }

    @Test
    fun cleanup_deleteFailureIsReportedAndDoesNotThrow() {
        val filesDir = tempFolder.newFolder("files")
        val now = 2_000_000_000_000L
        val staleFile = tempFile(filesDir, "stale.jpg")
        setModified(staleFile, now - POSTCARD_TEMP_FILE_MAX_AGE_MILLIS)

        val result =
            PostcardTempCleanup.cleanup(
                filesDir = filesDir,
                nowMillis = now,
                deleteFile = { false }
            )

        assertTrue(result.deletedFiles.isEmpty())
        assertEquals(listOf(staleFile.path), result.failedFiles)
        assertTrue(staleFile.exists())
    }

    @Test
    fun cleanup_missingOrEmptyDirectoryReportsNothing() {
        val missingFilesDir = tempFolder.newFolder("missing")

        val missingResult =
            PostcardTempCleanup.cleanup(
                missingFilesDir,
                nowMillis = 2_000_000_000_000L
            )

        File(missingFilesDir, "postcards_temp").mkdirs()
        val emptyResult =
            PostcardTempCleanup.cleanup(
                missingFilesDir,
                nowMillis = 2_000_000_000_000L
            )

        assertTrue(missingResult.deletedFiles.isEmpty())
        assertTrue(missingResult.failedFiles.isEmpty())
        assertTrue(emptyResult.deletedFiles.isEmpty())
        assertTrue(emptyResult.failedFiles.isEmpty())
    }

    private fun tempFile(filesDir: File, name: String): File =
        File(filesDir, "postcards_temp/$name").apply {
            parentFile?.mkdirs()
            writeText("temp")
        }

    private fun setModified(file: File, timestamp: Long) {
        assertTrue(file.setLastModified(timestamp))
    }
}
