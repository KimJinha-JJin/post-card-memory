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
 * cleanupPostcardOwnedAssets는 Context/Uri에 의존하지 않고 java.io.File과
 * Postcard(순수 데이터 클래스, Uri 없음)만 다루므로 Robolectric 없이도
 * TemporaryFolder로 실제 파일 I/O를 검증할 수 있다. PostcardDeletionManager의
 * Room 삭제 오케스트레이션(성공/실패 순서)은 PostcardRepository가 Room DAO를
 * 감싸고 있어 이 순수 JUnit 환경에서 실제 삭제 순서까지 재현하기는 어렵고,
 * 코드 리뷰로 확인했다(최종 보고 참고) — 이 파일은 그 정책이 실제로 딛고
 * 있는 파일 정리 단계만 검증한다.
 */
class PostcardDeletionManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun postcard(
        id: Long,
        imagePath: String,
        backgroundImagePath: String? = null
    ) = Postcard(
        id = id,
        imagePath = imagePath,
        title = "postcard-$id",
        backgroundImagePath = backgroundImagePath
    )

    private fun fileIn(filesDir: File, relativePath: String, content: String = "x"): File {
        val file = File(filesDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file
    }

    @Test
    fun cleanup_noOwnedFilesBesidesImage_reportsImageDeletedAndRestMissing() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = fileIn(filesDir, "postcards/postcard_1.jpg")

        val result = cleanupPostcardOwnedAssets(filesDir, postcard(1L, imageFile.path))

        assertTrue(result.databaseDeleted)
        assertTrue(result.isFullSuccess)
        assertFalse(imageFile.exists())
        assertTrue("image" in result.deletedAssets)
        assertTrue("backgroundImage" in result.missingAssets)
        assertTrue("stickerState" in result.missingAssets)
        assertTrue("sealState" in result.missingAssets)
        assertTrue("draft" in result.missingAssets)
        assertTrue("confirmedStickerBackgrounds" in result.missingAssets)
        assertTrue("cameraStickerOriginals" in result.missingAssets)
        assertTrue(result.failedAssets.isEmpty())
    }

    @Test
    fun cleanup_backgroundImagePresent_isDeleted() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = fileIn(filesDir, "postcards/postcard_2.jpg")
        val bgFile = fileIn(filesDir, "postcard_backgrounds/background_2.jpg")

        val result = cleanupPostcardOwnedAssets(
            filesDir,
            postcard(2L, imageFile.path, bgFile.path)
        )

        assertFalse(bgFile.exists())
        assertTrue("backgroundImage" in result.deletedAssets)
        assertTrue(result.isFullSuccess)
    }

    @Test
    fun cleanup_stickerAndSealStateFiles_bothDeleted() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = fileIn(filesDir, "postcards/postcard_3.jpg")
        val stickerState = fileIn(filesDir, "sticker_states/3.txt")
        val sealState = fileIn(filesDir, "seal_states/3.txt")

        val result = cleanupPostcardOwnedAssets(filesDir, postcard(3L, imageFile.path))

        assertFalse(stickerState.exists())
        assertFalse(sealState.exists())
        assertTrue("stickerState" in result.deletedAssets)
        assertTrue("sealState" in result.deletedAssets)
    }

    @Test
    fun cleanup_confirmedStickerBackgroundDir_deletedRecursively_otherPostcardDirUntouched() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = fileIn(filesDir, "postcards/postcard_4.jpg")
        val ownDir = File(filesDir, "sticker_bgs/4").apply { mkdirs() }
        File(ownDir, "sticker-a.png").writeText("png")
        val otherDir = File(filesDir, "sticker_bgs/999").apply { mkdirs() }
        File(otherDir, "sticker-b.png").writeText("png2")

        val result = cleanupPostcardOwnedAssets(filesDir, postcard(4L, imageFile.path))

        assertFalse(ownDir.exists())
        assertTrue("confirmedStickerBackgrounds" in result.deletedAssets)
        // 다른 엽서의 전용 디렉터리는 건드리지 않아야 한다.
        assertTrue(otherDir.exists())
        assertTrue(File(otherDir, "sticker-b.png").exists())
        // sticker_bgs/ 루트 자체는 삭제되지 않아야 한다(공용 루트).
        assertTrue(File(filesDir, "sticker_bgs").exists())
    }

    @Test
    fun cleanup_cameraStickerOriginalsDir_deletedRecursively_otherPostcardDirUntouched() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = fileIn(filesDir, "postcards/postcard_5.jpg")
        val ownDir = File(filesDir, "sticker_originals/5").apply { mkdirs() }
        File(ownDir, "sticker_abc.jpg").writeText("jpg")
        val otherDir = File(filesDir, "sticker_originals/6").apply { mkdirs() }
        File(otherDir, "sticker_def.jpg").writeText("jpg2")

        val result = cleanupPostcardOwnedAssets(filesDir, postcard(5L, imageFile.path))

        assertFalse(ownDir.exists())
        assertTrue("cameraStickerOriginals" in result.deletedAssets)
        assertTrue(otherDir.exists())
    }

    @Test
    fun cleanup_draftAndDraftStickerBackgroundDir_bothCleaned() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = fileIn(filesDir, "postcards/postcard_6.jpg")
        val draftFile = PostcardDraftStorage.draftFile(filesDir, 6L)
        draftFile.parentFile?.mkdirs()
        draftFile.writeText("POSTCARD_DRAFT_V1\n1\t6\t1\t1\t1\t~\t~\t0\t0")
        val draftBgDir = PostcardDraftStorage.draftStickerBackgroundDir(filesDir, 6L)
        draftBgDir.mkdirs()
        File(draftBgDir, "sticker-a.png").writeText("png")

        val result = cleanupPostcardOwnedAssets(filesDir, postcard(6L, imageFile.path))

        assertFalse(draftFile.exists())
        assertFalse(draftBgDir.exists())
        assertTrue("draft" in result.deletedAssets)
    }

    @Test
    fun cleanup_imagePathOutsideFilesDir_notDeleted_reportedAsFailed() {
        val filesDir = tempFolder.newFolder("files")
        val externalDir = tempFolder.newFolder("external")
        val externalImage = fileIn(externalDir, "outside.jpg")

        val result = cleanupPostcardOwnedAssets(filesDir, postcard(7L, externalImage.path))

        // 앱 내부 경로가 아니므로 삭제를 시도조차 하지 않는다.
        assertTrue(externalImage.exists())
        assertTrue(result.failedAssets.any { it.assetName == "image" })
        assertFalse(result.isFullSuccess)
    }

    @Test
    fun cleanup_stickerStateDeletionFails_othersStillProcessed_notHiddenAsSuccess() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = fileIn(filesDir, "postcards/postcard_8.jpg")

        // 실제 삭제 실패를 재현: 파일이어야 할 경로를 비어있지 않은
        // 디렉터리로 만들면 File.delete()가 false를 반환한다.
        val stickerStateAsDir = File(filesDir, "sticker_states/8.txt")
        stickerStateAsDir.mkdirs()
        File(stickerStateAsDir, "unexpected.txt").writeText("blocks delete")

        val sealState = fileIn(filesDir, "seal_states/8.txt")

        val result = cleanupPostcardOwnedAssets(filesDir, postcard(8L, imageFile.path))

        assertTrue(result.failedAssets.any { it.assetName == "stickerState" })
        assertFalse(result.isFullSuccess)
        // 하나 실패해도 나머지는 정상적으로 정리돼야 한다 — 전체 성공으로 숨기지 않되
        // 나머지 항목의 결과까지 잃지는 않는다.
        assertTrue("sealState" in result.deletedAssets)
        assertFalse(sealState.exists())
        assertTrue("image" in result.deletedAssets)
    }

    @Test
    fun cleanup_calledTwice_isIdempotent_secondCallReportsAllMissing() {
        val filesDir = tempFolder.newFolder("files")
        val imageFile = fileIn(filesDir, "postcards/postcard_9.jpg")
        fileIn(filesDir, "sticker_states/9.txt")

        val pc = postcard(9L, imageFile.path)

        val first = cleanupPostcardOwnedAssets(filesDir, pc)
        assertTrue(first.isFullSuccess)

        // 이미 삭제된 엽서를 다시 정리해도 실패로 취급하지 않는다(멱등).
        val second = cleanupPostcardOwnedAssets(filesDir, pc)

        assertTrue(second.isFullSuccess)
        assertTrue("image" in second.missingAssets)
        assertTrue("stickerState" in second.missingAssets)
    }
}
