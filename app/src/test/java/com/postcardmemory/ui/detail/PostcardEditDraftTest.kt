package com.postcardmemory.ui.detail

import androidx.compose.ui.geometry.Offset
import com.postcardmemory.utils.DoodlePoint
import com.postcardmemory.utils.DoodleStroke
import com.postcardmemory.utils.DoodleStrokeWidth
import com.postcardmemory.utils.serialize as serializeDoodleStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PhotoStickerItem/PostcardSealItem은 android.net.Uri를 직접 다루는데
 * 이 프로젝트엔 Robolectric이나 unitTests.returnDefaultValues 설정이
 * 없어 순수 JUnit에서 Uri 인스턴스 생성 자체가 실패한다. 그래서 아래
 * 테스트는 스티커/도장을 포함하지 않는 시나리오(메타데이터, 손상 처리,
 * revision 가드)만 다루고, 스티커/도장 필드가 포함된 완전한 왕복은
 * 실기기/계측 테스트 영역으로 남겨둔다.
 */
class PostcardEditDraftTest {

    private fun emptyDraft(
        postcardId: Long = 42L,
        revision: Long = 3L
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
    fun serialize_thenParse_roundTripsMetadataWithEmptyLists() {
        val original = emptyDraft()

        val parsed = parsePostcardEditDraft(original.serialize())

        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun serialize_thenParse_preservesPostcardIdAndRevision() {
        val original = emptyDraft(postcardId = 999L, revision = 17L)

        val parsed = parsePostcardEditDraft(original.serialize())

        assertNotNull(parsed)
        assertEquals(999L, parsed!!.postcardId)
        assertEquals(17L, parsed.revision)
    }

    @Test
    fun parsePostcardEditDraft_returnsNullForWrongHeader() {
        val text = "NOT_A_DRAFT_HEADER\n1\t1\t1\t1\t1\t~\t~\t0\t0"

        assertNull(parsePostcardEditDraft(text))
    }

    @Test
    fun parsePostcardEditDraft_returnsNullForTruncatedMetadata() {
        val text = "POSTCARD_DRAFT_V1\n1\t1\t1"

        assertNull(parsePostcardEditDraft(text))
    }

    @Test
    fun parsePostcardEditDraft_returnsNullForNonNumericField() {
        val text = "POSTCARD_DRAFT_V1\n1\tnot-a-long\t1\t1\t1\t~\t~\t0\t0"

        assertNull(parsePostcardEditDraft(text))
    }

    @Test
    fun parsePostcardEditDraft_returnsNullWhenDeclaredItemCountExceedsBodyLines() {
        // stickerCount=5 라고 선언했지만 실제로는 스티커 라인이 하나도 없음
        val text = "POSTCARD_DRAFT_V1\n1\t1\t1\t1\t1\t~\t~\t5\t0"

        assertNull(parsePostcardEditDraft(text))
    }

    @Test
    fun parsePostcardEditDraft_returnsNullForEmptyText() {
        assertNull(parsePostcardEditDraft(""))
    }

    @Test
    fun parsePostcardEditDraft_doesNotThrowOnGarbageInput() {
        val garbage = " random\tgarbage\nwith\nnewlines\t\t\t"

        // 예외를 던지지 않고 null을 반환하는지가 핵심
        val result = parsePostcardEditDraft(garbage)

        assertNull(result)
    }

    // ---- 낙서(DoodleStroke)는 Uri를 다루지 않으므로 스티커/도장과 달리 전체 왕복을 검증할 수 있다 ----

    private fun sampleDoodleStroke(id: String = "stroke-1") =
        DoodleStroke(
            id = id,
            points = listOf(
                DoodlePoint(0f, 0f),
                DoodlePoint(0.5f, 0.5f),
                DoodlePoint(1f, 1f)
            ),
            colorArgb = 0xFF252525L,
            width = DoodleStrokeWidth.THICK
        )

    @Test
    fun serialize_thenParse_roundTripsDoodleStrokes() {
        val original = emptyDraft().copy(
            doodleStrokes = listOf(sampleDoodleStroke("a"), sampleDoodleStroke("b"))
        )

        val parsed = parsePostcardEditDraft(original.serialize())

        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun serialize_thenParse_preservesDoodleAndStickerSealCountsTogether() {
        val original = emptyDraft().copy(
            doodleStrokes = listOf(sampleDoodleStroke())
        )

        val parsed = parsePostcardEditDraft(original.serialize())

        assertNotNull(parsed)
        assertEquals(1, parsed!!.doodleStrokes.size)
        assertTrue(parsed.stickers.isEmpty())
        assertTrue(parsed.seals.isEmpty())
    }

    @Test
    fun parsePostcardEditDraft_missingDoodleCountField_treatedAsNoDoodles() {
        // DRAFT_FORMAT_VERSION=1 시절(낙서 도입 전)에 저장된 초안: meta가 9개 필드뿐.
        val text = "POSTCARD_DRAFT_V1\n1\t1\t1\t1\t1\t~\t~\t0\t0"

        val parsed = parsePostcardEditDraft(text)

        assertNotNull(parsed)
        assertTrue(parsed!!.doodleStrokes.isEmpty())
    }

    @Test
    fun parsePostcardEditDraft_returnsNullWhenDeclaredDoodleCountExceedsBodyLines() {
        val text = "POSTCARD_DRAFT_V1\n1\t1\t1\t1\t1\t~\t~\t0\t0\t3"

        assertNull(parsePostcardEditDraft(text))
    }

    @Test
    fun parsePostcardEditDraft_skipsCorruptedDoodleLineButKeepsRest() {
        val goodStroke = sampleDoodleStroke("good")
        val text = listOf(
            "POSTCARD_DRAFT_V1",
            "1\t1\t1\t1\t1\t~\t~\t0\t0\t2",
            "corrupted-doodle-line",
            goodStroke.serializeDoodleStroke()
        ).joinToString("\n")

        val parsed = parsePostcardEditDraft(text)

        assertNotNull(parsed)
        assertEquals(listOf(goodStroke), parsed!!.doodleStrokes)
    }

    // ---- 텍스트 스티커도 Uri를 다루지 않으므로 낙서와 동일하게 전체 왕복을 검증할 수 있다 ----

    private fun sampleTextSticker(id: String = "text-1") =
        TextStickerItem(
            id = id,
            text = "( ˶ˆᗜˆ˵ )",
            offset = Offset(0.1f, 0.2f),
            scale = 1.2f,
            rotationDegrees = -10f,
            colorArgb = 0xFF334455L
        )

    @Test
    fun serialize_thenParse_roundTripsTextStickers() {
        val original = emptyDraft().copy(
            textStickers = listOf(sampleTextSticker("a"), sampleTextSticker("b")),
            selectedTextStickerId = "a"
        )

        val parsed = parsePostcardEditDraft(original.serialize())

        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun parsePostcardEditDraft_missingTextStickerFields_treatedAsNoTextStickers() {
        // DRAFT_FORMAT_VERSION=2 시절(텍스트 스티커 도입 전)에 저장된 초안:
        // meta에 낙서 개수(9)까지만 있고 10~11번(텍스트 스티커 개수/선택 id)이 없다.
        val text = "POSTCARD_DRAFT_V1\n2\t1\t1\t1\t1\t~\t~\t0\t0\t0"

        val parsed = parsePostcardEditDraft(text)

        assertNotNull(parsed)
        assertTrue(parsed!!.textStickers.isEmpty())
        assertNull(parsed.selectedTextStickerId)
    }

    @Test
    fun parsePostcardEditDraft_returnsNullWhenDeclaredTextStickerCountExceedsBodyLines() {
        val text = "POSTCARD_DRAFT_V1\n3\t1\t1\t1\t1\t~\t~\t0\t0\t0\t3\t~"

        assertNull(parsePostcardEditDraft(text))
    }

    @Test
    fun parsePostcardEditDraft_skipsCorruptedTextStickerLineButKeepsRest() {
        val goodSticker = sampleTextSticker("good")
        val text = listOf(
            "POSTCARD_DRAFT_V1",
            "3\t1\t1\t1\t1\t~\t~\t0\t0\t0\t2\t~",
            "corrupted\tline",
            goodSticker.serialize()
        ).joinToString("\n")

        val parsed = parsePostcardEditDraft(text)

        assertNotNull(parsed)
        assertEquals(listOf(goodSticker), parsed!!.textStickers)
    }

    // ---- 마스킹테이프도 Uri를 다루지 않으므로 낙서·텍스트 스티커와 동일하게 전체 왕복을 검증할 수 있다 ----

    private fun sampleMaskingTape(id: String = "tape-1") =
        MaskingTapeItem(
            id = id,
            style = MaskingTapeStyle.LAVENDER_DOT,
            offset = Offset(0.3f, -0.1f),
            scale = 0.9f,
            rotationDegrees = 8f
        )

    @Test
    fun serialize_thenParse_roundTripsMaskingTapes() {
        val original = emptyDraft().copy(
            maskingTapes = listOf(sampleMaskingTape("a"), sampleMaskingTape("b")),
            selectedMaskingTapeId = "a"
        )

        val parsed = parsePostcardEditDraft(original.serialize())

        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun parsePostcardEditDraft_missingMaskingTapeFields_treatedAsNoMaskingTapes() {
        // DRAFT_FORMAT_VERSION=3 시절(마스킹테이프 도입 전)에 저장된 초안: meta에
        // 텍스트 스티커 필드(11)까지만 있고 12~13번(마스킹테이프 개수/선택 id)이 없다.
        val text = "POSTCARD_DRAFT_V1\n3\t1\t1\t1\t1\t~\t~\t0\t0\t0\t0\t~"

        val parsed = parsePostcardEditDraft(text)

        assertNotNull(parsed)
        assertTrue(parsed!!.maskingTapes.isEmpty())
        assertNull(parsed.selectedMaskingTapeId)
    }

    @Test
    fun parsePostcardEditDraft_returnsNullWhenDeclaredMaskingTapeCountExceedsBodyLines() {
        val text = "POSTCARD_DRAFT_V1\n4\t1\t1\t1\t1\t~\t~\t0\t0\t0\t0\t~\t3\t~"

        assertNull(parsePostcardEditDraft(text))
    }

    @Test
    fun parsePostcardEditDraft_skipsCorruptedMaskingTapeLineButKeepsRest() {
        val goodTape = sampleMaskingTape("good")
        val text = listOf(
            "POSTCARD_DRAFT_V1",
            "4\t1\t1\t1\t1\t~\t~\t0\t0\t0\t0\t~\t2\t~",
            "corrupted\tline",
            goodTape.serialize()
        ).joinToString("\n")

        val parsed = parsePostcardEditDraft(text)

        assertNotNull(parsed)
        assertEquals(listOf(goodTape), parsed!!.maskingTapes)
    }

    @Test
    fun shouldPersistDraftRevision_allowsNewerOrEqualRevision() {
        assertTrue(shouldPersistDraftRevision(candidateRevision = 5L, latestPersistedRevision = 4L))
        assertTrue(shouldPersistDraftRevision(candidateRevision = 5L, latestPersistedRevision = 5L))
    }

    @Test
    fun shouldPersistDraftRevision_rejectsStaleRevision() {
        // 오래된 저장 요청(revision 3)이 이미 반영된 최신 상태(revision 7)를 덮으면 안 된다.
        assertFalse(shouldPersistDraftRevision(candidateRevision = 3L, latestPersistedRevision = 7L))
    }

    @Test
    fun shouldPersistDraftRevision_confirmedSaveClearCannotBeResurrectedByStaleAutosave() {
        // saveEditsAndClearDraft가 삭제 시점에 revision을 올려두면(latest=10),
        // 그보다 먼저 예약됐던 자동저장(candidate=6)은 더 이상 반영되면 안 된다.
        val latestAfterClear = 10L
        val staleAutosaveCandidate = 6L

        assertFalse(
            shouldPersistDraftRevision(
                candidateRevision = staleAutosaveCandidate,
                latestPersistedRevision = latestAfterClear
            )
        )
    }
}
