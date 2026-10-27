package com.postcardmemory.utils

import androidx.compose.ui.geometry.Offset
import com.postcardmemory.ui.detail.LabelStickerItem
import com.postcardmemory.ui.detail.LabelTapeStyle
import com.postcardmemory.ui.detail.MaskingTapeEdgeStyle
import com.postcardmemory.ui.detail.MaskingTapeItem
import com.postcardmemory.ui.detail.MaskingTapePatternKind
import com.postcardmemory.ui.detail.MaskingTapeStyle
import com.postcardmemory.ui.detail.PostcardEditDraft
import com.postcardmemory.ui.detail.PostcardSealItem
import com.postcardmemory.ui.detail.SealType
import com.postcardmemory.ui.detail.TextStickerItem
import java.io.File
import java.io.IOException
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

    /**
     * 위 왕복은 빈 초안만 다루고, PostcardEditDraftTest는 직렬화 문자열 단위로
     * 요소 종류를 하나씩 확인한다. 여기서는 실제 앱의 자동저장·복원 경로
     * (saveDraftAtomically → 파일 → loadDraft)로, 여러 꾸미기 요소를 함께 가진
     * 초안 하나가 의미 그대로 돌아오는지 본다 — 한 요소의 줄 수·순서가 어긋나면
     * 뒤따르는 요소까지 밀려 깨지므로, 요소별 테스트로는 잡히지 않는 회귀다.
     *
     * 포함하지 않은 것: 사진 스티커(PhotoStickerItem)와 사진 마스킹테이프
     * (style == PHOTO)는 android.net.Uri를 만들어야 하는데 JVM에는 Robolectric이
     * 없어 Uri.parse가 동작하지 않는다. 사진 스티커 한 줄의 왕복은 androidTest의
     * PhotoStickerEdgeStyleInstrumentedTest가 따로 다룬다.
     *
     * 모든 값은 저장 형식이 정규화하지 않는 범위(회전 -180~180 등)로 골라서,
     * 객체 전체 equals가 곧 "저장 계약에 있는 모든 필드가 보존됐다"는 뜻이 된다.
     */
    @Test
    fun saveDraftAtomically_thenLoadDraft_draftWithEveryNonPhotoDecorationKeepsAllFields() {
        val filesDir = tempFolder.newFolder("files")
        val original = PostcardEditDraft(
            postcardId = 77L,
            createdAtMillis = 1_700_000_000_000L,
            updatedAtMillis = 1_700_000_123_456L,
            revision = 12L,
            stickers = emptyList(),
            selectedStickerId = null,
            seals = listOf(
                PostcardSealItem(
                    id = "seal-postmark",
                    type = SealType.CIRCLE_POSTMARK,
                    offset = Offset(0.25f, -0.5f),
                    scale = 1.3f,
                    rotationDegrees = -15f,
                    colorArgb = 0xFF8A2F2FL
                ),
                PostcardSealItem(
                    // 아직 자리를 잡지 않은 도장(offset 없음) + 미니 도장 기본 배율
                    id = "seal-paw",
                    type = SealType.DOG_PAW,
                    offset = null,
                    scale = SealType.DOG_PAW.defaultScale,
                    rotationDegrees = 30f
                )
            ),
            selectedSealId = "seal-paw",
            doodleStrokes = listOf(
                DoodleStroke(
                    id = "stroke-pen",
                    points = listOf(DoodlePoint(0f, 0f), DoodlePoint(0.4f, 0.6f)),
                    colorArgb = 0xFF252525L,
                    width = DoodleStrokeWidth.THIN
                ),
                DoodleStroke(
                    id = "stroke-highlighter",
                    points = listOf(
                        DoodlePoint(0.1f, 0.9f),
                        DoodlePoint(0.5f, 0.85f),
                        DoodlePoint(0.9f, 0.8f)
                    ),
                    colorArgb = 0xFFFFE066L,
                    width = DoodleStrokeWidth.THICK,
                    tool = DoodleTool.HIGHLIGHTER
                )
            ),
            textStickers = listOf(
                TextStickerItem(
                    id = "text-1",
                    text = "여름 바다 ( ˶ˆᗜˆ˵ )",
                    offset = Offset(-0.2f, 0.35f),
                    scale = 1.15f,
                    rotationDegrees = 7.5f,
                    colorArgb = 0xFF334455L,
                    outlineColorArgb = 0xFFFAF7F0L
                )
            ),
            selectedTextStickerId = "text-1",
            maskingTapes = listOf(
                MaskingTapeItem(
                    id = "tape-preset",
                    style = MaskingTapeStyle.LAVENDER_DOT,
                    offset = Offset(0.3f, -0.1f),
                    scale = 0.9f,
                    rotationDegrees = 8f
                ),
                MaskingTapeItem(
                    id = "tape-custom",
                    style = MaskingTapeStyle.CUSTOM,
                    offset = Offset(-0.45f, 0.05f),
                    scale = 1.1f,
                    rotationDegrees = -22f,
                    lengthScale = 1.6f,
                    thicknessScale = 0.7f,
                    edgeStyle = MaskingTapeEdgeStyle.STRAIGHT,
                    customBaseColorArgb = 0xFFB8E0D2L,
                    customPatternColorArgb = 0xFF2E5E4EL,
                    customPatternKind = MaskingTapePatternKind.STAR
                )
            ),
            selectedMaskingTapeId = null,
            labelStickers = listOf(
                LabelStickerItem(
                    id = "label-preset",
                    text = "SUMMER 2026",
                    style = LabelTapeStyle.RED,
                    offset = Offset(0.4f, -0.2f),
                    rotationDegrees = -6f
                ),
                LabelStickerItem(
                    id = "label-custom",
                    text = "민트 라벨",
                    style = LabelTapeStyle.CUSTOM,
                    offset = Offset(-0.35f, 0.44f),
                    scale = 1.25f,
                    rotationDegrees = 12f,
                    customTapeColorArgb = 0xFF7FD4C1L
                )
            ),
            selectedLabelStickerId = "label-custom"
        )

        assertTrue(PostcardDraftStorage.saveDraftAtomically(filesDir, original))
        val loaded = PostcardDraftStorage.loadDraft(filesDir, 77L)

        assertEquals(original, loaded)
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

    /**
     * 57일차 저장·데이터 안전성 챕터 제4차: `deleteDraft()`의 `File.delete()`가
     * 드문 파일시스템 오류로 실패하는 상황을 플랫폼 독립적으로 재현하기
     * 어려워, fallback인 `invalidateDraftFile()` 자체와 `loadDraft()`의
     * 상호작용을 직접 검증한다 — 핵심 계약: fallback이 남긴 내용을
     * `loadDraft()`가 항상 "복원 대상 없음"으로 안전하게 처리해야 한다.
     */
    @Test
    fun invalidateDraftFile_succeedsAndLeavesFileThatLoadDraftTreatsAsAbsent() {
        val filesDir = tempFolder.newFolder("files")

        PostcardDraftStorage.saveDraftAtomically(
            filesDir,
            draft(postcardId = 50L, revision = 4L)
        )
        val draftFile = File(filesDir, "drafts/edit_state/50.draft.txt")
        assertTrue(draftFile.exists())

        val invalidated =
            PostcardDraftStorage.invalidateDraftFile(filesDir, draftFile, 50L)
        assertTrue(invalidated)

        // 파일 자체는 여전히 존재하지만(delete가 아니라 replace이므로),
        // loadDraft()는 형식 오류로 판정해 복원하지 않고 스스로 지운다.
        assertTrue(draftFile.exists())
        val loaded = PostcardDraftStorage.loadDraft(filesDir, 50L)
        assertNull(loaded)
        assertFalse(draftFile.exists())
    }

    @Test
    fun invalidateDraftFile_leavesNoLeftoverTempFile() {
        val filesDir = tempFolder.newFolder("files")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 51L))
        val draftFile = File(filesDir, "drafts/edit_state/51.draft.txt")

        PostcardDraftStorage.invalidateDraftFile(filesDir, draftFile, 51L)

        val draftDir = File(filesDir, "drafts/edit_state")
        val leftoverTemp = File(draftDir, "51.draft.invalidate.tmp")
        assertFalse(leftoverTemp.exists())
    }

    @Test
    fun invalidateDraftFile_doesNotAffectOtherPostcardIds() {
        val filesDir = tempFolder.newFolder("files")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 52L))
        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 53L))
        val draftFile52 = File(filesDir, "drafts/edit_state/52.draft.txt")

        PostcardDraftStorage.invalidateDraftFile(filesDir, draftFile52, 52L)

        assertNull(PostcardDraftStorage.loadDraft(filesDir, 52L))
        assertNotNull(PostcardDraftStorage.loadDraft(filesDir, 53L))
    }

    @Test
    fun invalidateDraftFile_doesNotTouchConfirmedStateFiles() {
        val filesDir = tempFolder.newFolder("files")

        val stickerStateFile = File(filesDir, "sticker_states/54.txt")
        stickerStateFile.parentFile?.mkdirs()
        stickerStateFile.writeText("confirmed-sticker-state")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 54L))
        val draftFile = File(filesDir, "drafts/edit_state/54.draft.txt")

        PostcardDraftStorage.invalidateDraftFile(filesDir, draftFile, 54L)

        assertTrue(stickerStateFile.exists())
        assertEquals("confirmed-sticker-state", stickerStateFile.readText())
    }

    /**
     * deleteDraft()의 정상 삭제 경로(File.delete() 성공)는 이번 fallback
     * 추가로 영향받지 않아야 한다 — fallback은 delete 실패 시에만 실행된다.
     */
    @Test
    fun deleteDraft_normalDeleteSucceeds_fallbackNeverInvoked() {
        val filesDir = tempFolder.newFolder("files")

        PostcardDraftStorage.saveDraftAtomically(filesDir, draft(postcardId = 55L))
        val deleted = PostcardDraftStorage.deleteDraft(filesDir, 55L)

        assertTrue(deleted)
        val draftFile = File(filesDir, "drafts/edit_state/55.draft.txt")
        assertFalse(draftFile.exists())
        val leftoverTemp =
            File(filesDir, "drafts/edit_state/55.draft.invalidate.tmp")
        assertFalse(leftoverTemp.exists())
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

    // ── 78일차 P0-1: 읽기 실패를 손상으로 오인해 초안을 지우던 위험 ──
    // 예전 구현은 readText가 던지기만 하면 초안 파일과 초안 소유 누끼
    // 폴더를 지웠다. 아래 테스트들은 그 구현으로 되돌리면 전부 실패한다.

    @Test
    fun loadDraft_transientReadFailure_keepsDraftFileAndReturnsNull() {
        val filesDir = tempFolder.newFolder("files")
        assertTrue(PostcardDraftStorage.saveDraftAtomically(filesDir, draft(31L)))

        val draftFile = PostcardDraftStorage.draftFile(filesDir, 31L)
        val before = draftFile.readText()

        val loaded = PostcardDraftStorage.loadDraft(filesDir, 31L) {
            throw IOException("일시적 읽기 실패")
        }

        assertNull(loaded)
        assertTrue(draftFile.exists())
        assertEquals(before, draftFile.readText())
    }

    @Test
    fun loadDraft_transientReadFailure_keepsDraftOwnedStickerBackgroundDir() {
        val filesDir = tempFolder.newFolder("files")
        assertTrue(PostcardDraftStorage.saveDraftAtomically(filesDir, draft(32L)))

        val draftBgDir = PostcardDraftStorage.draftStickerBackgroundDir(filesDir, 32L)
        draftBgDir.mkdirs()
        val ownedFile = File(draftBgDir, "sticker-1.png")
        ownedFile.writeText("fake-png-bytes")

        val loaded = PostcardDraftStorage.loadDraft(filesDir, 32L) {
            throw IOException("일시적 읽기 실패")
        }

        assertNull(loaded)
        assertTrue(draftBgDir.exists())
        assertTrue(ownedFile.exists())
        assertEquals("fake-png-bytes", ownedFile.readText())
    }

    @Test
    fun loadDraft_afterTransientReadFailure_nextSuccessfulReadStillRestoresTheDraft() {
        val filesDir = tempFolder.newFolder("files")
        assertTrue(PostcardDraftStorage.saveDraftAtomically(filesDir, draft(33L, revision = 7L)))

        assertNull(
            PostcardDraftStorage.loadDraft(filesDir, 33L) {
                throw IOException("일시적 읽기 실패")
            }
        )

        // 지우지 않았으므로 다음 진입에서 초안이 그대로 살아나야 한다.
        val recovered = PostcardDraftStorage.loadDraft(filesDir, 33L)

        assertNotNull(recovered)
        assertEquals(33L, recovered!!.postcardId)
        assertEquals(7L, recovered.revision)
    }

    @Test
    fun loadDraft_readFailureFromRealFilesystem_keepsDraftOwnedStickerBackgroundDir() {
        // seam 없이 production 기본 경로만으로도 같은 보호가 걸리는지 확인한다.
        // 초안 경로가 디렉터리면 exists()는 true지만 readText()는 실패한다.
        val filesDir = tempFolder.newFolder("files")
        val draftDir = File(filesDir, "drafts/edit_state")
        draftDir.mkdirs()

        val unreadable = File(draftDir, "34.draft.txt")
        assertTrue(unreadable.mkdir())

        val draftBgDir = PostcardDraftStorage.draftStickerBackgroundDir(filesDir, 34L)
        draftBgDir.mkdirs()
        val ownedFile = File(draftBgDir, "sticker-1.png")
        ownedFile.writeText("fake-png-bytes")

        assertNull(PostcardDraftStorage.loadDraft(filesDir, 34L))

        assertTrue(draftBgDir.exists())
        assertTrue(ownedFile.exists())
    }

    @Test
    fun loadDraft_corruptedContentPolicyIsUnchangedByTheReadFailureFix() {
        // 실제 손상(내용을 읽었는데 파싱 불가)은 기존대로 격리된다.
        val filesDir = tempFolder.newFolder("files")
        val draftDir = File(filesDir, "drafts/edit_state")
        draftDir.mkdirs()
        val corruptFile = File(draftDir, "35.draft.txt")
        corruptFile.writeText("garbage")

        val draftBgDir = PostcardDraftStorage.draftStickerBackgroundDir(filesDir, 35L)
        draftBgDir.mkdirs()
        File(draftBgDir, "sticker-1.png").writeText("fake-png-bytes")

        assertNull(PostcardDraftStorage.loadDraft(filesDir, 35L))

        assertFalse(corruptFile.exists())
        assertFalse(draftBgDir.exists())
    }
}
