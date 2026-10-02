package com.postcardmemory.utils

import com.postcardmemory.data.Postcard
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 꾸미기 상태 파일·엽서별 자산 디렉터리의 저장 위치, 삭제, 고아 파일 진단이 서로
 * 어긋나지 않는지 실제 파일 I/O로 확인한다.
 *
 * 저장 쪽(DetailViewModel)은 DecorationStateFile.file / PostcardAssetDirectory.directory로
 * 경로를 만든다. 이 테스트는 같은 함수로 파일을 만들어 두고, production 삭제
 * (cleanupPostcardOwnedAssets)와 진단(OrphanFileDiagnostics.scan)이 그 파일을 각각 실제로
 * 지우고 찾아내는지 본다 — 목록에 새 종류가 추가됐는데 어느 한쪽이 그 위치를 다르게
 * 계산하면 여기서 실패한다.
 *
 * 디렉터리 문자열 자체는 production 상수를 다시 참조하지 않고 아래 리터럴로 독립 고정한다.
 * 기존 사용자 기기에 이미 저장된 위치이므로, 상수가 바뀌면 저장·삭제·진단이 함께 바뀌어
 * 서로는 일치해도 기존 파일을 잃는다 — 그 경우를 이 리터럴이 잡는다.
 */
class DecorationDirectoryContractTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun postcard(id: Long, imagePath: String) = Postcard(
        id = id,
        imagePath = imagePath,
        title = "postcard-$id"
    )

    private fun createAllDecorationFiles(filesDir: File, postcardId: Long) {
        DecorationStateFile.entries.forEach { stateFile ->
            val file = stateFile.file(filesDir, postcardId)
            file.parentFile?.mkdirs()
            file.writeText("state")
        }
        PostcardAssetDirectory.entries.forEach { assetDirectory ->
            val dir = assetDirectory.directory(filesDir, postcardId)
            dir.mkdirs()
            File(dir, "asset.png").writeText("png")
        }
    }

    @Test
    fun directoryNames_keepExistingUserFileLocations() {
        assertEquals(
            listOf(
                "sticker_states",
                "seal_states",
                "doodle_states",
                "text_sticker_states",
                "masking_tape_states",
                "label_sticker_states"
            ),
            DecorationStateFile.entries.map { it.directoryName }
        )
        assertEquals(
            listOf("sticker_bgs", "sticker_originals", "masking_tape_photos"),
            PostcardAssetDirectory.entries.map { it.directoryName }
        )

        val filesDir = File("files")
        assertEquals(
            File(filesDir, "label_sticker_states/42.txt"),
            DecorationStateFile.LABEL_STICKER.file(filesDir, 42L)
        )
        assertEquals(
            File(filesDir, "sticker_bgs/42"),
            PostcardAssetDirectory.CONFIRMED_STICKER_BACKGROUNDS.directory(filesDir, 42L)
        )
    }

    @Test
    fun cleanup_removesEverySavedDecorationLocation_andKeepsOtherPostcards() {
        val filesDir = tempFolder.newFolder("files")
        val image = File(filesDir, "postcards/postcard_7.jpg").apply {
            parentFile?.mkdirs()
            writeText("img")
        }
        createAllDecorationFiles(filesDir, 7L)
        createAllDecorationFiles(filesDir, 8L)

        val result = cleanupPostcardOwnedAssets(filesDir, postcard(7L, image.path))

        assertTrue(result.isFullSuccess)
        DecorationStateFile.entries.forEach { stateFile ->
            assertFalse(stateFile.name, stateFile.file(filesDir, 7L).exists())
            assertTrue(stateFile.name, stateFile.assetName in result.deletedAssets)
            assertTrue(stateFile.name, stateFile.file(filesDir, 8L).exists())
        }
        PostcardAssetDirectory.entries.forEach { assetDirectory ->
            assertFalse(assetDirectory.name, assetDirectory.directory(filesDir, 7L).exists())
            assertTrue(
                assetDirectory.name,
                assetDirectory.deletionAssetName in result.deletedAssets
            )
            assertTrue(assetDirectory.name, assetDirectory.directory(filesDir, 8L).exists())
        }
    }

    @Test
    fun orphanScan_findsEverySavedDecorationLocation_onlyWhenPostcardIsGone() {
        val filesDir = tempFolder.newFolder("files")
        createAllDecorationFiles(filesDir, 7L)

        val withoutPostcard = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = emptySet(),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = emptySet()
        )
        DecorationStateFile.entries.forEach { stateFile ->
            val category = withoutPostcard.categories.single { it.type == stateFile.assetName }
            assertEquals(
                stateFile.name,
                stateFile.file(filesDir, 7L).path,
                category.orphans.single().path
            )
        }
        PostcardAssetDirectory.entries.forEach { assetDirectory ->
            val category =
                withoutPostcard.categories.single { it.type == assetDirectory.orphanType }
            assertEquals(
                assetDirectory.name,
                assetDirectory.directory(filesDir, 7L).path,
                category.orphans.single().path
            )
        }
        assertTrue(withoutPostcard.unclassified.isEmpty())

        val withPostcard = OrphanFileDiagnostics.scan(
            rootDirectory = filesDir,
            existingPostcardIds = setOf(7L),
            referencedImagePaths = emptySet(),
            referencedBackgroundPaths = emptySet()
        )
        assertEquals(0, withPostcard.totalOrphanCount)

        // 진단은 지우지 않는다.
        DecorationStateFile.entries.forEach { stateFile ->
            assertTrue(stateFile.name, stateFile.file(filesDir, 7L).exists())
        }
    }
}
