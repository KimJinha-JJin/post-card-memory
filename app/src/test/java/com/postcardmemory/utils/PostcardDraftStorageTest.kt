package com.postcardmemory.utils

import com.postcardmemory.ui.detail.PostcardEditDraft
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Context 없이 java.io.File만으로 동작하는 internal 오버로드를 직접
 * 검증한다(PostcardDraftStorage 참고). TemporaryFolder는 표준 JUnit4
 * 규칙이라 Robolectric 없이도 실제 파일 I/O를 검증할 수 있다.
 */
class PostcardDraftStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun draft(
        postcardId: Long,
        revision: Long = 1L
    ) = PostcardEditDraft(
        postcardId = postcardId,
        createdAtMillis = 1_000L,
        updatedAtMillis = 2_000L,
        revision = revision,
        stickers = emptyList(),
        selectedStickerId = null,
        seals = emptyList(),
        selectedSealId = null
    )

    @Test
    fun draftFileNameFor_usesPostcardIdAndExtension() {
        assertEquals(
            "42.draft.txt",
            PostcardDraftStorage.draftFileNameFor(42L)
        )
    }

    @Test
    fun draftFileNameFor_isStableForSameId() {
        val first = PostcardDraftStorage.draftFileNameFor(7L)
        val second = PostcardDraftStorage.draftFileNameFor(7L)

        assertEquals(first, second)
    }

    @Test
    fun draftFileNameFor_differsAcrossIds() {
        val first = PostcardDraftStorage.draftFileNameFor(1L)
        val second = PostcardDraftStorage.draftFileNameFor(2L)

        assertFalse(first == second)
    }

    @Test
    fun draftFileNameFor_containsNoPathTraversalOrUserText() {
        val fileName = PostcardDraftStorage.draftFileNameFor(123L)

        assertTrue(
            fileName.matches(Regex("[0-9]+\\.draft\\.txt"))
        )
        assertFalse(fileName.contains(".."))
        assertFalse(fileName.contains("/"))
        assertFalse(fileName.contains("\\"))
    }

    @Test
    fun saveDraftAtomically_thenLoadDraft_roundTrips() {
        val filesDir = tempFolder.newFolder("files")

        val saved = PostcardDraftStorage.saveDraftAtomically(
            filesDir,
            draft(postcardId = 5L, revision = 3L)
        )
        assertTrue(saved)

        val loaded = PostcardDraftStorage.loadDraft(filesDir, 5L)
        assertNotNull(loaded)
        assertEquals(5L, loaded!!.postcardId)
        assertEquals(3L, loaded.revision)
    }

    @Test
    fun saveDraftAtomically_leavesNoLeftoverTempFile() {
        val filesDir = tempFolder.newFolder("files")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 9L))

        val draftDir = File(filesDir, "drafts/edit_state")
        val leftoverTemp = File(draftDir, "9.draft.tmp")

        assertFalse(leftoverTemp.exists())
    }

    /**
     * 자동저장은 같은 postcardId로 반복 실행되며 매번 기존 초안 파일을
     * 덮어쓴다. renameTo() 기반 구현이 Windows에서 이 경로를 실패시켰던
     * 것과 동일한 시나리오다(저장 덮어쓰기 테스트 원인 조사 보고서 참고).
     */
    @Test
    fun saveDraftAtomically_sameIdSavedTwice_secondSaveOverwritesFirst() {
        val filesDir = tempFolder.newFolder("files")

        val firstSaved = PostcardDraftStorage.saveDraftAtomically(
            filesDir,
            draft(postcardId = 21L, revision = 1L)
        )
        assertTrue(firstSaved)

        val secondSaved = PostcardDraftStorage.saveDraftAtomically(
            filesDir,
            draft(postcardId = 21L, revision = 2L)
        )
        assertTrue(secondSaved)

        val loaded = PostcardDraftStorage.loadDraft(filesDir, 21L)
        assertNotNull(loaded)
        assertEquals(2L, loaded!!.revision)

        val draftDir = File(filesDir, "drafts/edit_state")
        val leftoverTemp = File(draftDir, "21.draft.tmp")
        assertFalse(leftoverTemp.exists())
    }

    @Test
    fun saveDraftAtomically_overwritingOnePostcard_otherPostcardDraftUntouched() {
        val filesDir = tempFolder.newFolder("files")

        PostcardDraftStorage.saveDraftAtomically(
            filesDir,
            draft(postcardId = 22L, revision = 1L)
        )
        PostcardDraftStorage.saveDraftAtomically(
            filesDir,
            draft(postcardId = 23L, revision = 1L)
        )

        val secondSaved = PostcardDraftStorage.saveDraftAtomically(
            filesDir,
            draft(postcardId = 22L, revision = 2L)
        )
        assertTrue(secondSaved)

        val untouched = PostcardDraftStorage.loadDraft(filesDir, 23L)
        assertNotNull(untouched)
        assertEquals(1L, untouched!!.revision)
    }

    @Test
    fun draftStoragePath_isSeparateFromConfirmedStickerAndSealStatePaths() {
        val filesDir = tempFolder.newFolder("files")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 11L))

        val draftFile = File(filesDir, "drafts/edit_state/11.draft.txt")
        val stickerStateFile = File(filesDir, "sticker_states/11.txt")
        val sealStateFile = File(filesDir, "seal_states/11.txt")
        val stickerBgsDir = File(filesDir, "sticker_bgs/11")

        assertTrue(draftFile.exists())
        // 초안 저장이 확정 상태 파일 경로를 건드리지 않았는지 확인
        assertFalse(stickerStateFile.exists())
        assertFalse(sealStateFile.exists())
        assertFalse(stickerBgsDir.exists())
    }

    @Test
    fun deleteDraft_onlyRemovesDraftFile_confirmedStateFilesUntouched() {
        val filesDir = tempFolder.newFolder("files")

        // 확정 상태 파일이 이미 있는 상황을 흉내낸다.
        val stickerStateFile = File(filesDir, "sticker_states/13.txt")
        stickerStateFile.parentFile?.mkdirs()
        stickerStateFile.writeText("confirmed-sticker-state")

        val sealStateFile = File(filesDir, "seal_states/13.txt")
        sealStateFile.parentFile?.mkdirs()
        sealStateFile.writeText("confirmed-seal-state")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 13L))
        PostcardDraftStorage.deleteDraft(filesDir, 13L)

        val draftFile = File(filesDir, "drafts/edit_state/13.draft.txt")
        assertFalse(draftFile.exists())
        assertTrue(stickerStateFile.exists())
        assertTrue(sealStateFile.exists())
        assertEquals("confirmed-sticker-state", stickerStateFile.readText())
        assertEquals("confirmed-seal-state", sealStateFile.readText())
    }

    @Test
    fun deleteDraft_doesNotAffectOtherPostcardIds() {
        val filesDir = tempFolder.newFolder("files")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 1L))
        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 2L))

        PostcardDraftStorage.deleteDraft(filesDir, 1L)

        assertNull(PostcardDraftStorage.loadDraft(filesDir, 1L))
        assertNotNull(PostcardDraftStorage.loadDraft(filesDir, 2L))
    }

    @Test
    fun loadDraft_deletesCorruptedFileAndReturnsNull() {
        val filesDir = tempFolder.newFolder("files")
        val draftDir = File(filesDir, "drafts/edit_state")
        draftDir.mkdirs()

        val corruptFile = File(draftDir, "21.draft.txt")
        corruptFile.writeText("this is not a valid draft file at all")

        val loaded = PostcardDraftStorage.loadDraft(filesDir, 21L)

        assertNull(loaded)
        // 손상 파일은 다음 진입에서 또 실패하지 않도록 즉시 격리(삭제)돼야 한다.
        assertFalse(corruptFile.exists())
    }

    @Test
    fun loadDraft_returnsNullWhenNoFileExists() {
        val filesDir = tempFolder.newFolder("files")

        assertNull(PostcardDraftStorage.loadDraft(filesDir, 999L))
    }

    @Test
    fun deleteDraft_isSafeWhenFileDoesNotExist() {
        val filesDir = tempFolder.newFolder("files")

        // 예외 없이 조용히 넘어가야 한다.
        PostcardDraftStorage.deleteDraft(filesDir, 555L)
    }

    @Test
    fun draftStickerBackgroundDir_isSeparateFromConfirmedStickerBgsDir() {
        val filesDir = tempFolder.newFolder("files")

        val draftBgDir = PostcardDraftStorage.draftStickerBackgroundDir(filesDir, 30L)

        assertEquals(
            File(filesDir, "draft_sticker_bgs/30").canonicalPath,
            draftBgDir.canonicalPath
        )
        assertFalse(
            draftBgDir.canonicalPath ==
                File(filesDir, "sticker_bgs/30").canonicalPath
        )
    }

    @Test
    fun deleteDraft_alsoRemovesDraftOwnedStickerBackgroundDir() {
        val filesDir = tempFolder.newFolder("files")

        val draftBgDir = PostcardDraftStorage.draftStickerBackgroundDir(filesDir, 40L)
        draftBgDir.mkdirs()
        val ownedFile = File(draftBgDir, "sticker-1.png")
        ownedFile.writeText("fake-png-bytes")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 40L))
        PostcardDraftStorage.deleteDraft(filesDir, 40L)

        assertFalse(ownedFile.exists())
        assertFalse(draftBgDir.exists())
    }

    @Test
    fun deleteDraft_doesNotTouchConfirmedStickerBgsDir() {
        val filesDir = tempFolder.newFolder("files")

        val confirmedBgDir = File(filesDir, "sticker_bgs/41")
        confirmedBgDir.mkdirs()
        val confirmedFile = File(confirmedBgDir, "sticker-1.png")
        confirmedFile.writeText("confirmed-png-bytes")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 41L))
        PostcardDraftStorage.deleteDraft(filesDir, 41L)

        // 확정 저장용 sticker_bgs는 draft_sticker_bgs와 이름만 비슷할 뿐
        // 전혀 다른 디렉터리이므로 초안 삭제에 영향받지 않아야 한다.
        assertTrue(confirmedFile.exists())
        assertEquals("confirmed-png-bytes", confirmedFile.readText())
    }

    @Test
    fun deleteDraft_isSafeWhenStickerBackgroundDirDoesNotExist() {
        val filesDir = tempFolder.newFolder("files")

        // 예외 없이 조용히 넘어가야 한다(디렉터리 자체가 없는 경우).
        PostcardDraftStorage.deleteDraft(filesDir, 999L)
    }

    @Test
    fun loadDraft_corruptedFile_alsoRemovesDraftOwnedStickerBackgroundDir() {
        val filesDir = tempFolder.newFolder("files")
        val draftDir = File(filesDir, "drafts/edit_state")
        draftDir.mkdirs()

        val corruptFile = File(draftDir, "22.draft.txt")
        corruptFile.writeText("this is not a valid draft file at all")

        val draftBgDir = PostcardDraftStorage.draftStickerBackgroundDir(filesDir, 22L)
        draftBgDir.mkdirs()
        val ownedFile = File(draftBgDir, "sticker-1.png")
        ownedFile.writeText("fake-png-bytes")

        val loaded = PostcardDraftStorage.loadDraft(filesDir, 22L)

        assertNull(loaded)
        assertFalse(ownedFile.exists())
        assertFalse(draftBgDir.exists())
    }
}
