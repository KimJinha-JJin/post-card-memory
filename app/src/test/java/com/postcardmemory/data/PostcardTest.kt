package com.postcardmemory.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 뒷면 편지 필드(backRecipientModifier/backMessage)는 기존 엽서(신규 필드
 * 없이 만들어진 데이터)를 로드했을 때도 안전한 기본값으로 채워져야
 * 뒤집었을 때 빈 편지 상태로 정상 진입한다. 실제 Room migration은
 * Robolectric/androidTest 없이 이 프로젝트의 순수 JUnit 환경에서 검증할
 * 수 없으므로(BackgroundColorSaveRaceTest와 동일한 제약), 이 테스트는
 * data class 기본값만 확인한다.
 */
class PostcardTest {

    @Test
    fun backLetterFields_defaultToEmptyString() {
        val postcard =
            Postcard(
                imagePath = "/data/photo.jpg",
                title = "제목"
            )

        assertEquals("", postcard.backRecipientModifier)
        assertEquals("", postcard.backMessage)
    }
}
