package com.postcardmemory.utils

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * StickerAssetDiagnostics.scan()은 java.io.File만 다루는 internal 오버로드라
 * OrphanFileDiagnosticsTest와 같이 TemporaryFolder로 검증한다. 상태 파일·초안 줄은
 * PhotoStickerItem/PostcardEditDraft.serialize와 같은 탭 구분 원문으로 직접 만든다.
 * 진단은 파일을 지우지 않으므로 각 테스트에서 파일이 남아 있는지도 확인한다.
 */
class StickerAssetDiagnosticsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun fileIn(root: File, relativePath: String, content: String = "x"): File {
        val file = File(root, relativePath)
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file
    }

    private fun uri(file: File): String = file.toURI().toString()

    private fun stickerLine(
        id: String,
        original: File,
        cutout: File?,
        offsetX: String = "10.0"
    ): String =
        listOf(
            id,
            uri(original),
            uri(cutout ?: original),
            cutout?.let { uri(it) } ?: "~",
            (cutout != null).toString(),
            offsetX,
            "20.0",
            "1.0",
            "0.0",
            "false",
            "false",
            "default",
            "0"
        ).joinToString("\t")

    private fun draftText(
        postcardId: Long,
        stickerLines: List<String>,
        sealLines: List<String> = emptyList()
    ): String {
        val meta = listOf(
            "5", postcardId.toString(), "1", "2", "3", "~", "~",
            stickerLines.size.toString(), sealLines.size.toString(),
            "0", "0", "~", "0", "~", "0", "~"
        ).joinToString("\t")
        return (listOf("POSTCARD_DRAFT_V1", meta) + stickerLines + sealLines).joinToString("\n")
    }

    private fun scan(filesDir: File, cacheDir: File, ids: Set<Long>) =
        StickerAssetDiagnostics.scan(
            rootDirectory = filesDir,
            cacheDirectory = cacheDir,
            existingPostcardIds = ids
        )

    @Test
    fun confirmedBackground_droppedFromStateOfLivePostcard_isReported_keptOneIsNot() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = fileIn(filesDir, "sticker_originals/1/a.jpg")
        val kept = fileIn(filesDir, "sticker_bgs/1/kept.png", "kept")
        val dropped = fileIn(filesDir, "sticker_bgs/1/dropped.png", "dropped-bytes")
        fileIn(filesDir, "sticker_states/1.txt", stickerLine("kept", original, kept))

        val result = scan(filesDir, cacheDir, setOf(1L))

        val category = result.categories.first { it.type == "unreferencedConfirmedStickerBackground" }
        assertEquals(listOf(dropped.path), category.orphans.map { it.path })
        assertEquals(dropped.length(), category.totalSizeBytes)
        assertTrue(kept.exists())
        assertTrue(dropped.exists())
    }

    @Test
    fun confirmedBackground_ofMissingPostcard_isLeftToOrphanFileDiagnostics() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val other = fileIn(filesDir, "sticker_bgs/999/x.png")
        val stray = fileIn(filesDir, "sticker_bgs/not-a-number.png")

        val result = scan(filesDir, cacheDir, setOf(1L))

        val category = result.categories.first { it.type == "unreferencedConfirmedStickerBackground" }
        assertTrue(category.orphans.isEmpty())
        assertEquals(listOf(stray.path), result.unclassified)
        assertTrue(other.exists())
    }

    @Test
    fun draftBackground_referencedByDraft_isNotReported_withoutDraft_isReported() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = fileIn(filesDir, "sticker_originals/1/a.jpg")
        val usedByDraft = fileIn(filesDir, "draft_sticker_bgs/1/s1.png")
        val leftover = fileIn(filesDir, "draft_sticker_bgs/2/s2.png")
        fileIn(
            filesDir,
            "drafts/edit_state/1.draft.txt",
            draftText(1L, listOf(stickerLine("s1", original, usedByDraft)))
        )

        val result = scan(filesDir, cacheDir, setOf(1L, 2L))

        val category = result.categories.first { it.type == "unreferencedDraftStickerBackground" }
        assertEquals(listOf(leftover.path), category.orphans.map { it.path })
        assertTrue(usedByDraft.exists())
        assertTrue(leftover.exists())
    }

    @Test
    fun stickerCache_referencedBySavedState_isNotReported_otherCacheIs() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = fileIn(filesDir, "sticker_originals/1/a.jpg")
        val referencedCache = fileIn(cacheDir, "photo_stickers/live.png")
        val strayCache = fileIn(cacheDir, "photo_stickers/duplicated_x_1.png")
        fileIn(filesDir, "sticker_states/1.txt", stickerLine("s1", original, referencedCache))

        val result = scan(filesDir, cacheDir, setOf(1L))

        val category = result.categories.first { it.type == "unreferencedStickerCache" }
        assertEquals(listOf(strayCache.path), category.orphans.map { it.path })
        assertTrue(referencedCache.exists())
        assertTrue(strayCache.exists())
    }

    @Test
    fun draft_sameAsConfirmed_withCutoutCopiedToDraftFolder_isSameAsConfirmed() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = fileIn(filesDir, "sticker_originals/1/a.jpg")
        val confirmedCutout = fileIn(filesDir, "sticker_bgs/1/s1.png", "cutout")
        val draftCutout = fileIn(filesDir, "draft_sticker_bgs/1/s1.png", "cutout")
        fileIn(filesDir, "sticker_states/1.txt", stickerLine("s1", original, confirmedCutout))
        fileIn(filesDir, "seal_states/1.txt", "seal-line")
        val draft = fileIn(
            filesDir,
            "drafts/edit_state/1.draft.txt",
            draftText(1L, listOf(stickerLine("s1", original, draftCutout)), listOf("seal-line"))
        )

        val entry = scan(filesDir, cacheDir, setOf(1L)).drafts.single()

        assertEquals(DraftDiagnosis.SAME_AS_CONFIRMED, entry.diagnosis)
        assertEquals(draft.path, entry.path)
        assertEquals(draft.length() + draftCutout.length(), entry.sizeBytes)
        assertTrue(draft.exists())
        assertTrue(draftCutout.exists())
    }

    @Test
    fun draft_withMovedSticker_hasUnsavedChanges() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = fileIn(filesDir, "sticker_originals/1/a.jpg")
        fileIn(filesDir, "sticker_states/1.txt", stickerLine("s1", original, null))
        fileIn(
            filesDir,
            "drafts/edit_state/1.draft.txt",
            draftText(1L, listOf(stickerLine("s1", original, null, offsetX = "99.0")))
        )

        val entry = scan(filesDir, cacheDir, setOf(1L)).drafts.single()

        assertEquals(DraftDiagnosis.HAS_UNSAVED_CHANGES, entry.diagnosis)
    }

    @Test
    fun draft_withRedoneCutoutUnderSameName_hasUnsavedChanges() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = fileIn(filesDir, "sticker_originals/1/a.jpg")
        val confirmedCutout = fileIn(filesDir, "sticker_bgs/1/s1.png", "old-cutout")
        val draftCutout = fileIn(filesDir, "draft_sticker_bgs/1/s1.png", "new-cutout")
        fileIn(filesDir, "sticker_states/1.txt", stickerLine("s1", original, confirmedCutout))
        fileIn(
            filesDir,
            "drafts/edit_state/1.draft.txt",
            draftText(1L, listOf(stickerLine("s1", original, draftCutout)))
        )

        val entry = scan(filesDir, cacheDir, setOf(1L)).drafts.single()

        assertEquals(DraftDiagnosis.HAS_UNSAVED_CHANGES, entry.diagnosis)
    }

    @Test
    fun draft_withUnsavedSeal_hasUnsavedChanges() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        fileIn(
            filesDir,
            "drafts/edit_state/1.draft.txt",
            draftText(1L, emptyList(), listOf("new-seal"))
        )

        val entry = scan(filesDir, cacheDir, setOf(1L)).drafts.single()

        assertEquals(DraftDiagnosis.HAS_UNSAVED_CHANGES, entry.diagnosis)
    }

    @Test
    fun draft_corrupted_isUnreadable_andKept() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val draft = fileIn(filesDir, "drafts/edit_state/1.draft.txt", "garbage")

        val entry = scan(filesDir, cacheDir, setOf(1L)).drafts.single()

        assertEquals(DraftDiagnosis.UNREADABLE, entry.diagnosis)
        assertTrue(draft.exists())
    }

    // ---- 120일차: 앱 복원 규칙(id로 대신 찾는 누끼)·판정 보류·원본 진단 ----

    /** 누끼 칸과 배경제거 여부를 직접 정하는 상태 줄(옛 형식·손상 재현용). */
    private fun rawStickerLine(
        id: String,
        original: String,
        cutoutUri: String,
        isBackgroundRemoved: Boolean
    ): String =
        listOf(
            id, original, original, cutoutUri, isBackgroundRemoved.toString(),
            "10.0", "20.0", "1.0", "0.0", "false", "false", "default", "0"
        ).joinToString("\t")

    private fun category(result: StickerAssetScanResult, type: String) =
        result.categories.first { it.type == type }

    @Test
    fun confirmedBackground_withTildeOrUnreadableCutoutUri_isReferencedById() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = uri(fileIn(filesDir, "sticker_originals/1/a.jpg"))
        val byTilde = fileIn(filesDir, "sticker_bgs/1/tilde.png")
        val byMissing = fileIn(filesDir, "sticker_bgs/1/missing.png")
        val byContent = fileIn(filesDir, "sticker_bgs/1/content.png")
        fileIn(
            filesDir,
            "sticker_states/1.txt",
            listOf(
                rawStickerLine("tilde", original, "~", isBackgroundRemoved = true),
                rawStickerLine(
                    "missing", original,
                    uri(File(cacheDir, "photo_stickers/gone.png")), isBackgroundRemoved = true
                ),
                rawStickerLine("content", original, "content://media/x/1", isBackgroundRemoved = true)
            ).joinToString("\n")
        )

        val result = scan(filesDir, cacheDir, setOf(1L))

        assertTrue(category(result, "unreferencedConfirmedStickerBackground").orphans.isEmpty())
        assertTrue(result.pending.isEmpty())
        listOf(byTilde, byMissing, byContent).forEach { assertTrue(it.exists()) }
    }

    @Test
    fun confirmedBackground_whenCutoutUriPointsToOtherExistingFile_isPending() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = uri(fileIn(filesDir, "sticker_originals/1/a.jpg"))
        val liveCache = fileIn(cacheDir, "photo_stickers/live.png")
        val idBackground = fileIn(filesDir, "sticker_bgs/1/s1.png")
        fileIn(
            filesDir,
            "sticker_states/1.txt",
            rawStickerLine("s1", original, uri(liveCache), isBackgroundRemoved = true)
        )

        val result = scan(filesDir, cacheDir, setOf(1L))

        assertTrue(category(result, "unreferencedConfirmedStickerBackground").orphans.isEmpty())
        assertEquals(listOf(idBackground.path), result.pending.map { it.path })
        assertTrue(category(result, "unreferencedStickerCache").orphans.isEmpty())
        assertTrue(idBackground.exists())
    }

    @Test
    fun confirmedBackground_ofBackgroundRemovalOff_isPendingNotOrphan() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val original = uri(fileIn(filesDir, "sticker_originals/1/a.jpg"))
        val oldBackground = fileIn(filesDir, "sticker_bgs/1/s1.png")
        fileIn(
            filesDir,
            "sticker_states/1.txt",
            rawStickerLine("s1", original, "~", isBackgroundRemoved = false)
        )

        val result = scan(filesDir, cacheDir, setOf(1L))

        assertTrue(category(result, "unreferencedConfirmedStickerBackground").orphans.isEmpty())
        assertEquals(listOf(oldBackground.path), result.pending.map { it.path })
        assertTrue(oldBackground.exists())
    }

    @Test
    fun unreadableOrMalformedConfirmedState_keepsFilesPending_everywhereItCouldMatter() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        // 엽서 1: 상태 파일을 읽을 수 없음(디렉터리). 엽서 2: 앱이 읽지 못하는 줄.
        File(filesDir, "sticker_states/1.txt").mkdirs()
        fileIn(filesDir, "sticker_states/2.txt", "broken\tline")
        val background1 = fileIn(filesDir, "sticker_bgs/1/x.png")
        val background2 = fileIn(filesDir, "sticker_bgs/2/y.png")
        val original3 = fileIn(filesDir, "sticker_originals/3/z.jpg")
        val cache = fileIn(cacheDir, "photo_stickers/removed_background_1.png")

        val result = scan(filesDir, cacheDir, setOf(1L, 2L, 3L))

        result.categories.forEach { assertTrue("${it.type}: ${it.orphans}", it.orphans.isEmpty()) }
        assertEquals(
            listOf(background1, background2, original3, cache).map { it.path }.sorted(),
            result.pending.map { it.path }.sorted()
        )
        listOf(background1, background2, original3, cache).forEach { assertTrue(it.exists()) }
    }

    @Test
    fun unreadableDraft_keepsDraftBackgroundsPending() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        fileIn(filesDir, "drafts/edit_state/1.draft.txt", "garbage")
        val draftBackground = fileIn(filesDir, "draft_sticker_bgs/1/s1.png")

        val result = scan(filesDir, cacheDir, setOf(1L))

        assertTrue(category(result, "unreferencedDraftStickerBackground").orphans.isEmpty())
        assertEquals(listOf(draftBackground.path), result.pending.map { it.path })
        assertTrue(draftBackground.exists())
    }

    @Test
    fun stickerOriginal_referencedOrShared_isKept_unreferencedIsReported_missingPostcardLeftToOrphan() {
        val filesDir = tempFolder.newFolder("files")
        val cacheDir = tempFolder.newFolder("cache")
        val shared = fileIn(filesDir, "sticker_originals/1/shared.jpg")
        val usedByDraft = fileIn(filesDir, "sticker_originals/2/draft.jpg")
        val unreferenced = fileIn(filesDir, "sticker_originals/2/left.jpg", "left-bytes")
        val ofMissingPostcard = fileIn(filesDir, "sticker_originals/999/gone.jpg")
        fileIn(
            filesDir,
            "sticker_states/1.txt",
            listOf(
                stickerLine("a", shared, null),
                stickerLine("b", shared, null),
                rawStickerLine("c", "content://media/picker/0/x", "~", isBackgroundRemoved = false)
            ).joinToString("\n")
        )
        fileIn(
            filesDir,
            "drafts/edit_state/2.draft.txt",
            draftText(2L, listOf(stickerLine("d", usedByDraft, null)))
        )

        val result = scan(filesDir, cacheDir, setOf(1L, 2L))

        val originals = category(result, "unreferencedStickerOriginal")
        assertEquals(listOf(unreferenced.path), originals.orphans.map { it.path })
        assertEquals(unreferenced.length(), originals.totalSizeBytes)
        assertTrue(result.pending.isEmpty())
        listOf(shared, usedByDraft, unreferenced, ofMissingPostcard).forEach { assertTrue(it.exists()) }
    }
}
