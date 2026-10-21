package com.postcardmemory.utils

import com.postcardmemory.data.Postcard
import com.postcardmemory.ui.detail.PhotoStickerEdgeStyle
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 110일차 스티커 통 저장 구조. 엽서 파일과의 독립성, 쓰기 실패 뒤 목록·파일 정합성,
 * 재시작(새로 읽기) 후 복원을 실제 파일 입출력으로 확인한다.
 */
class StickerStashStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val filesDir: File get() = tempFolder.root
    private val anyImage: (File) -> Boolean = { true }

    private fun bytes(text: String): () -> InputStream = { ByteArrayInputStream(text.toByteArray()) }

    private fun source(
        original: String = "photo",
        cutout: String? = null,
        edgeStyle: PhotoStickerEdgeStyle = PhotoStickerEdgeStyle.DEFAULT,
        edgeSeed: Long = 0L,
        flipHorizontal: Boolean = false,
        extension: String = ".jpg"
    ) = PhotoStickerStashSource(
        openOriginal = bytes(original),
        originalExtension = extension,
        openCutout = cutout?.let(::bytes),
        isBackgroundRemoved = cutout != null,
        edgeStyle = edgeStyle,
        edgeSeed = edgeSeed,
        flipHorizontal = flipHorizontal,
        flipVertical = false
    )

    private fun stashed(result: StickerStashResult): StashedPhotoSticker =
        (result as StickerStashResult.Stashed).sticker

    @Test
    fun stashedSticker_survivesReloadWithShapeInfo() {
        val styles = PhotoStickerEdgeStyle.entries
        val style = styles.last()
        val sticker = stashed(
            StickerStashStorage.stash(
                filesDir,
                source(original = "photo", cutout = "cutout", edgeStyle = style, edgeSeed = 42L, flipHorizontal = true),
                anyImage,
                nowMillis = 1_000L
            )
        )

        // 새 프로세스처럼 목록 파일을 처음부터 다시 읽는다.
        val reloaded = StickerStashStorage.list(filesDir).single()
        assertEquals(sticker.id, reloaded.id)
        assertEquals("photo", reloaded.originalFile.readText())
        assertEquals("cutout", reloaded.cutoutFile?.readText())
        assertTrue(reloaded.isBackgroundRemoved)
        assertEquals(reloaded.cutoutFile, reloaded.displayedFile)
        assertEquals(style, reloaded.edgeStyle)
        assertEquals(42L, reloaded.edgeSeed)
        assertTrue(reloaded.flipHorizontal)
        assertTrue(isInsideDirectory(StickerStashStorage.root(filesDir), reloaded.originalFile))
    }

    @Test
    fun list_isNewestFirst() {
        val first = stashed(StickerStashStorage.stash(filesDir, source(original = "a"), anyImage, nowMillis = 1L))
        val second = stashed(StickerStashStorage.stash(filesDir, source(original = "b"), anyImage, nowMillis = 2L))

        assertEquals(listOf(second.id, first.id), StickerStashStorage.list(filesDir).map { it.id })
    }

    @Test
    fun deletingTheSourcePostcard_keepsTheStashedCopy() {
        val postcardOriginal =
            File(PostcardAssetDirectory.STICKER_ORIGINALS.directory(filesDir, 7L), "sticker_a.jpg").apply {
                parentFile?.mkdirs()
                writeText("postcard-photo")
            }
        val image = File(filesDir, "postcard_images/7.jpg").apply {
            parentFile?.mkdirs()
            writeText("image")
        }
        StickerStashStorage.stash(
            filesDir,
            source().let {
                PhotoStickerStashSource(
                    openOriginal = { postcardOriginal.inputStream() },
                    originalExtension = ".jpg",
                    openCutout = null,
                    isBackgroundRemoved = false,
                    edgeStyle = it.edgeStyle,
                    edgeSeed = 0L,
                    flipHorizontal = false,
                    flipVertical = false
                )
            },
            anyImage
        )

        cleanupPostcardOwnedAssets(filesDir, Postcard(id = 7L, imagePath = image.path, title = "postcard-7"))

        assertFalse(postcardOriginal.exists())
        val kept = StickerStashStorage.list(filesDir).single()
        assertEquals("postcard-photo", kept.originalFile.readText())
    }

    @Test
    fun removingFromStash_keepsPostcardFiles() {
        val postcardOriginal =
            File(PostcardAssetDirectory.STICKER_ORIGINALS.directory(filesDir, 3L), "sticker_b.jpg").apply {
                parentFile?.mkdirs()
                writeText("postcard-photo")
            }
        val sticker = stashed(
            StickerStashStorage.stash(
                filesDir,
                PhotoStickerStashSource(
                    openOriginal = { postcardOriginal.inputStream() },
                    originalExtension = ".jpg",
                    openCutout = null,
                    isBackgroundRemoved = false,
                    edgeStyle = PhotoStickerEdgeStyle.DEFAULT,
                    edgeSeed = 0L,
                    flipHorizontal = false,
                    flipVertical = false
                ),
                anyImage
            )
        )

        assertTrue(StickerStashStorage.remove(filesDir, sticker.id))

        assertTrue(StickerStashStorage.list(filesDir).isEmpty())
        assertFalse(sticker.originalFile.parentFile!!.exists())
        assertEquals("postcard-photo", postcardOriginal.readText())
    }

    @Test
    fun failedIndexWrite_leavesNoEntryAndNoFiles() {
        val kept = stashed(StickerStashStorage.stash(filesDir, source(original = "kept"), anyImage))

        val result = StickerStashStorage.stash(
            filesDir,
            source(original = "new"),
            anyImage,
            writeIndex = { _, _ -> false }
        )

        assertEquals(StickerStashResult.Failed, result)
        assertEquals(listOf(kept.id), StickerStashStorage.list(filesDir).map { it.id })
        val itemDirs = File(StickerStashStorage.root(filesDir), "items").listFiles().orEmpty().map { it.name }
        assertEquals(listOf(kept.id), itemDirs)
    }

    @Test
    fun unreadableOriginal_isNotRegistered() {
        val throwing = PhotoStickerStashSource(
            openOriginal = { throw SecurityException("permission revoked") },
            originalExtension = ".jpg",
            openCutout = null,
            isBackgroundRemoved = false,
            edgeStyle = PhotoStickerEdgeStyle.DEFAULT,
            edgeSeed = 0L,
            flipHorizontal = false,
            flipVertical = false
        )

        assertEquals(StickerStashResult.Failed, StickerStashStorage.stash(filesDir, throwing, anyImage))
        assertEquals(StickerStashResult.Failed, StickerStashStorage.stash(filesDir, source(original = ""), anyImage))
        assertEquals(StickerStashResult.Failed, StickerStashStorage.stash(filesDir, source(), isReadableImage = { false }))
        assertTrue(StickerStashStorage.list(filesDir).isEmpty())
        assertTrue(File(StickerStashStorage.root(filesDir), "items").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun failedCutoutCopy_cleansUpTheCopiedOriginal() {
        val brokenCutout = PhotoStickerStashSource(
            openOriginal = bytes("photo"),
            originalExtension = ".jpg",
            openCutout = { object : InputStream() { override fun read(): Int = throw IOException("disk") } },
            isBackgroundRemoved = true,
            edgeStyle = PhotoStickerEdgeStyle.DEFAULT,
            edgeSeed = 0L,
            flipHorizontal = false,
            flipVertical = false
        )

        assertEquals(StickerStashResult.Failed, StickerStashStorage.stash(filesDir, brokenCutout, anyImage))
        assertTrue(File(StickerStashStorage.root(filesDir), "items").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun sameImageAndShape_isStashedOnce() {
        stashed(StickerStashStorage.stash(filesDir, source(original = "same"), anyImage))

        assertEquals(StickerStashResult.AlreadyStashed, StickerStashStorage.stash(filesDir, source(original = "same"), anyImage))
        // 모양이 다르면 다른 조각이다.
        assertNotNull(
            (StickerStashStorage.stash(filesDir, source(original = "same", flipHorizontal = true), anyImage)
                as? StickerStashResult.Stashed)
        )
        assertEquals(2, StickerStashStorage.list(filesDir).size)
        assertEquals(2, File(StickerStashStorage.root(filesDir), "items").listFiles().orEmpty().size)
    }

    @Test
    fun stashStopsAtTheLimit() {
        repeat(StickerStashStorage.MAX_STASHED_STICKERS) { index ->
            stashed(StickerStashStorage.stash(filesDir, source(original = "p$index"), anyImage))
        }

        assertEquals(StickerStashResult.Full, StickerStashStorage.stash(filesDir, source(original = "over"), anyImage))
        assertEquals(StickerStashStorage.MAX_STASHED_STICKERS, StickerStashStorage.list(filesDir).size)
    }

    @Test
    fun failedRemoveWrite_keepsEntryAndFiles() {
        val sticker = stashed(StickerStashStorage.stash(filesDir, source(), anyImage))

        assertFalse(StickerStashStorage.remove(filesDir, sticker.id, writeIndex = { _, _ -> false }))

        assertEquals(listOf(sticker.id), StickerStashStorage.list(filesDir).map { it.id })
        assertTrue(sticker.originalFile.exists())
    }

    @Test
    fun removeRejectsPathLikeIds() {
        val outside = File(filesDir, "sticker_originals").apply { mkdirs() }

        assertFalse(StickerStashStorage.remove(filesDir, "../../sticker_originals"))
        assertTrue(outside.exists())
    }

    @Test
    fun entryWithMissingFiles_isHiddenButUnknownLinesArePreserved() {
        val gone = stashed(StickerStashStorage.stash(filesDir, source(original = "gone"), anyImage, nowMillis = 1L))
        gone.originalFile.delete()
        val index = File(StickerStashStorage.root(filesDir), "index.txt")
        index.appendText("future-format-line\n")

        assertTrue(StickerStashStorage.list(filesDir).isEmpty())

        stashed(StickerStashStorage.stash(filesDir, source(original = "next"), anyImage, nowMillis = 2L))
        val lines = index.readLines().filter { it.isNotBlank() }
        assertTrue(lines.any { it == "future-format-line" })
        assertTrue(lines.any { it.startsWith(gone.id) })
    }

    @Test
    fun unsafeExtension_fallsBackToJpg() {
        val sticker = stashed(StickerStashStorage.stash(filesDir, source(extension = "/../x"), anyImage))

        assertEquals("original.jpg", sticker.originalFile.name)
        assertNull(sticker.cutoutFile)
    }
}
