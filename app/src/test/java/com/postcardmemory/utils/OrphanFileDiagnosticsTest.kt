package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * OrphanFileDiagnostics.scan()은 java.io.File만 다루는 internal 오버로드라
 * PostcardDeletionManagerTest와 같은 방식으로 Context 없이 TemporaryFolder로
 * 검증한다. 이 진단 도구는 파일을 지우지 않으므로, 스캔 후에도 만들어 둔
 * 파일이 그대로 남아 있는지를 각 테스트에서 함께 확인한다.
 */
class OrphanFileDiagnosticsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun fileIn(filesDir: File, relativePath: String, content: String = "x"): File {
        val file = File(filesDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file
    }

    @Test
    fun centralImage_referenced_isNotOrphan_unreferenced_isOrphan() {
        val filesDir = tempFolder.newFolder("files")
        val referencedImage = fileIn(filesDir, "postcards/postcard_1.jpg")
        val orphanImage = fileIn(filesDir, "postcards/postcard_orphan.jpg", "orphan-bytes")

        val result = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = setOf(1L),
            referencedImagePaths = setOf(referencedImage.path),
            referencedBackgroundPaths = emptySet()
        )

        val category = result.categories.first { it.type == "centralImage" }
        assertEquals(1, category.count)
        assertEquals(orphanImage.path, category.orphans.single().path)
        assertEquals(orphanImage.length(), category.totalSizeBytes)

        // 진단은 삭제하지 않는다.
        assertTrue(referencedImage.exists())
        assertTrue(orphanImage.exists())
    }

    @Test
    fun backgroundImage_referenced_isNotOrphan_unreferenced_isOrphan() {
        val filesDir = tempFolder.newFolder("files")
        val referencedBg = fileIn(filesDir, "postcard_backgrounds/background_1.jpg")
        val orphanBg = fileIn(filesDir, "postcard_backgrounds/background_orphan.jpg")

        val result = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = setOf(1L),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = setOf(referencedBg.path)
        )

        val category = result.categories.first { it.type == "backgroundImage" }
        assertEquals(1, category.count)
        assertEquals(orphanBg.path, category.orphans.single().path)

        assertTrue(referencedBg.exists())
        assertTrue(orphanBg.exists())
    }

    @Test
    fun postcardIdDirectory_existingId_isNotOrphan_missingId_isOrphan() {
        val filesDir = tempFolder.newFolder("files")
        val ownedDir = File(filesDir, "sticker_bgs/1").apply { mkdirs() }
        File(ownedDir, "a.png").writeText("png")
        val orphanDir = File(filesDir, "sticker_bgs/999").apply { mkdirs() }
        File(orphanDir, "b.png").writeText("orphan-png")

        val result = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = setOf(1L),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = emptySet()
        )

        val category = result.categories.first { it.type == "confirmedStickerBackground" }
        assertEquals(1, category.count)
        assertEquals(orphanDir.path, category.orphans.single().path)
        assertTrue(category.orphans.single().isDirectory)
        assertEquals("orphan-png".length.toLong(), category.totalSizeBytes)

        assertTrue(ownedDir.exists())
        assertTrue(orphanDir.exists())
    }

    @Test
    fun cameraStickerOriginals_and_draftStickerBackgrounds_followSameRule() {
        val filesDir = tempFolder.newFolder("files")
        File(filesDir, "sticker_originals/1").mkdirs()
        val orphanOriginals = File(filesDir, "sticker_originals/2").apply { mkdirs() }
        File(filesDir, "draft_sticker_bgs/1").mkdirs()
        val orphanDraftBg = File(filesDir, "draft_sticker_bgs/3").apply { mkdirs() }

        val result = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = setOf(1L),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = emptySet()
        )

        val originalsCategory = result.categories.first { it.type == "cameraStickerOriginal" }
        val draftBgCategory = result.categories.first { it.type == "draftStickerBackground" }

        assertEquals(listOf(orphanOriginals.path), originalsCategory.orphans.map { it.path })
        assertEquals(listOf(orphanDraftBg.path), draftBgCategory.orphans.map { it.path })
    }

    @Test
    fun stickerState_and_sealState_and_editDraft_followPostcardIdFileRule() {
        val filesDir = tempFolder.newFolder("files")
        fileIn(filesDir, "sticker_states/1.txt")
        val orphanStickerState = fileIn(filesDir, "sticker_states/2.txt")
        fileIn(filesDir, "seal_states/1.txt")
        val orphanSealState = fileIn(filesDir, "seal_states/2.txt")
        fileIn(filesDir, "drafts/edit_state/1.draft.txt")
        val orphanDraft = fileIn(filesDir, "drafts/edit_state/2.draft.txt")

        val result = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = setOf(1L),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = emptySet()
        )

        assertEquals(
            listOf(orphanStickerState.path),
            result.categories.first { it.type == "stickerState" }.orphans.map { it.path }
        )
        assertEquals(
            listOf(orphanSealState.path),
            result.categories.first { it.type == "sealState" }.orphans.map { it.path }
        )
        assertEquals(
            listOf(orphanDraft.path),
            result.categories.first { it.type == "editDraft" }.orphans.map { it.path }
        )
    }

    @Test
    fun malformedNames_areUnclassified_notTreatedAsOrphan() {
        val filesDir = tempFolder.newFolder("files")
        // postcardId로 파싱할 수 없는 디렉터리/파일 이름들.
        val badStickerBgDir = File(filesDir, "sticker_bgs/not-a-number").apply { mkdirs() }
        val strayFile = fileIn(filesDir, "sticker_bgs/loose-file.txt")
        val tmpDraft = fileIn(filesDir, "drafts/edit_state/1.draft.tmp")

        val result = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = setOf(1L),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = emptySet()
        )

        val stickerBgCategory = result.categories.first { it.type == "confirmedStickerBackground" }
        assertTrue(stickerBgCategory.orphans.isEmpty())

        assertTrue(badStickerBgDir.path in result.unclassified)
        assertTrue(strayFile.path in result.unclassified)
        assertTrue(tmpDraft.path in result.unclassified)

        // 미분류 항목도 삭제되지 않는다.
        assertTrue(badStickerBgDir.exists())
        assertTrue(strayFile.exists())
        assertTrue(tmpDraft.exists())
    }

    @Test
    fun totalCounts_and_sizes_aggregateAcrossCategories() {
        val filesDir = tempFolder.newFolder("files")
        val orphanImage = fileIn(filesDir, "postcards/orphan.jpg", "12345")
        val orphanBg = fileIn(filesDir, "postcard_backgrounds/orphan.jpg", "1234567890")

        val result = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = emptySet(),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = emptySet()
        )

        assertEquals(2, result.totalOrphanCount)
        assertEquals(orphanImage.length() + orphanBg.length(), result.totalOrphanSizeBytes)
    }

    @Test
    fun missingRootDirectories_doNotThrow_andReportNoOrphans() {
        val filesDir = tempFolder.newFolder("files")

        val result = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = emptySet(),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = emptySet()
        )

        assertEquals(0, result.totalOrphanCount)
        assertTrue(result.unclassified.isEmpty())
    }
}
