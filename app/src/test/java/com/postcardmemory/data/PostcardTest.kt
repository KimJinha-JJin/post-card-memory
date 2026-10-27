package com.postcardmemory.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 뒷면 편지 필드(backRecipientModifier/backMessage)는 기존 엽서(신규 필드
 * 없이 만들어진 데이터)를 로드했을 때도 안전한 기본값으로 채워져야
 * 뒤집었을 때 빈 편지 상태로 정상 진입한다. 이 JVM 테스트는 data class
 * 기본값만 확인한다. 실제 Room migration으로 옛 행이 두 필드를 빈 문자열로
 * 받는지는 androidTest의 PostcardFullMigrationChainTest(1→현재 버전)가 확인하지만,
 * 그 테스트는 실사용 기기 기준 CONDITIONAL 등급이라 지금은 실행하지 않는다
 * (TEST-COVERAGE-MAP 참고).
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
