package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ConfirmedStickerBackgroundCleanupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val keptId = "11111111-1111-1111-1111-111111111111"
    private val droppedId = "22222222-2222-2222-2222-222222222222"
    private val undoId = "33333333-3333-3333-3333-333333333333"
    private val legacyLeftoverId = "44444444-4444-4444-4444-444444444444"

    private fun png(directory: File, id: String): File =
        File(directory, "$id.png").apply {
            parentFile?.mkdirs()
            writeText("png-$id")
        }

    @Test
    fun readStickerIds_missingFile_isEmpty_presentFile_readsFirstColumn() {
        val stateFile = File(tempFolder.root, "sticker_states/1.txt")
        assertEquals(emptySet<String>(), ConfirmedStickerBackgroundCleanup.readStickerIds(stateFile))

        stateFile.parentFile?.mkdirs()
        stateFile.writeText("$keptId\tfile:///a\tx\n\n$droppedId\tfile:///b\ty")
        assertEquals(
            setOf(keptId, droppedId),
            ConfirmedStickerBackgroundCleanup.readStickerIds(stateFile)
        )
    }

    @Test
    fun readStickerIds_unreadable_isNull() {
        // 디렉터리를 파일처럼 읽으면 실패한다 — 그때는 아무것도 지우지 않도록 null.
        val directoryInsteadOfFile = tempFolder.newFolder("sticker_states", "1.txt")
        assertNull(ConfirmedStickerBackgroundCleanup.readStickerIds(directoryInsteadOfFile))
    }

    @Test
    fun deleteDropped_removesOnlyPreviouslyConfirmedIdsThatAreNotKept() {
        val directory = tempFolder.newFolder("sticker_bgs", "1")
        val kept = png(directory, keptId)
        val dropped = png(directory, droppedId)
        val heldByUndo = png(directory, undoId)
        val legacyLeftover = png(directory, legacyLeftoverId)
        val unrelated = File(directory, "note.txt").apply { writeText("x") }

        val deleted = ConfirmedStickerBackgroundCleanup.deleteDroppedBackgrounds(
            directory = directory,
            previousStickerIds = setOf(keptId, droppedId, undoId),
            keepStickerIds = setOf(keptId, undoId)
        )

        assertEquals(listOf(dropped.name), deleted.map { it.name })
        assertFalse(dropped.exists())
        assertTrue(kept.exists())
        assertTrue(heldByUndo.exists())
        // 이전 확정 목록에 없던 잔여물은 소급해서 지우지 않는다.
        assertTrue(legacyLeftover.exists())
        assertTrue(unrelated.exists())
    }

    @Test
    fun deleteDropped_ignoresMalformedIdsAndOtherPostcards() {
        val directory = tempFolder.newFolder("sticker_bgs", "1")
        val otherPostcard = png(tempFolder.newFolder("sticker_bgs", "2"), droppedId)
        val sameName = png(directory, droppedId)

        val deleted = ConfirmedStickerBackgroundCleanup.deleteDroppedBackgrounds(
            directory = directory,
            previousStickerIds = setOf(droppedId, "../2/$droppedId", "not-a-uuid"),
            keepStickerIds = emptySet()
        )

        assertEquals(listOf(sameName.name), deleted.map { it.name })
        assertTrue(otherPostcard.exists())
    }

    @Test
    fun deleteDropped_missingFileOrDirectory_deletesNothing() {
        val missingDirectory = File(tempFolder.root, "sticker_bgs/9")

        val deleted = ConfirmedStickerBackgroundCleanup.deleteDroppedBackgrounds(
            directory = missingDirectory,
            previousStickerIds = setOf(droppedId),
            keepStickerIds = emptySet()
        )

        assertTrue(deleted.isEmpty())
        assertFalse(missingDirectory.exists())
    }
}
