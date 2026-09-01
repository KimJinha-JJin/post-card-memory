package com.postcardmemory.ui.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 갤러리 다중 삭제 결과 요약 메시지를 검증한다. GalleryViewModel 자체는
 * PostcardRepository/PostcardDeletionManager(Context 의존)를 필요로 해
 * 이 프로젝트의 순수 JUnit 환경에서 직접 인스턴스화할 수 없으므로
 * (Robolectric 미사용, PostcardEditDraftTest.kt 참고), 그 결과 집계 로직만
 * 분리해 검증한다.
 */
class DeletionMessageLogicTest {

    @Test
    fun buildDeletionResultMessage_allSucceeded() {
        assertEquals(
            "3개를 삭제했어.",
            buildDeletionResultMessage(succeededCount = 3, failedCount = 0)
        )
    }

    @Test
    fun buildDeletionResultMessage_allFailed() {
        assertEquals(
            "삭제하지 못했어. 다시 시도해줘.",
            buildDeletionResultMessage(succeededCount = 0, failedCount = 2)
        )
    }

    @Test
    fun buildDeletionResultMessage_partialFailure_mentionsBothCounts() {
        val message = buildDeletionResultMessage(succeededCount = 2, failedCount = 1)

        assertEquals(
            "2개 삭제했고, 1개는 삭제하지 못했어.",
            message
        )
    }
}
