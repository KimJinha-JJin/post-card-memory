package com.postcardmemory.ui.detail

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
