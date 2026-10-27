package com.postcardmemory.ui.theme

import com.postcardmemory.testsupport.readStructureTestSource
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 101일차: 테마에 inverse 색을 비워 두면 알림창(Snackbar)이 Material 기본 보라색
 * 배경·연보라 버튼으로 나온다(실기기 QA에서 발견). 앱 팔레트로 채워 둔 상태를
 * 소스 텍스트 기준으로 고정한다. 색값 자체는 미감 값이라 고정하지 않는다.
 */
class ThemeInverseColorsStructureTest {

    private val themeText: String by lazy {
        readStructureTestSource(
            listOf(
                "src/main/java/com/postcardmemory/ui/theme/Theme.kt",
                "app/src/main/java/com/postcardmemory/ui/theme/Theme.kt"
            )
        )
    }

    @Test
    fun lightColorScheme_definesInverseColors() {
        listOf("inverseSurface =", "inverseOnSurface =", "inversePrimary =").forEach { slot ->
            assertTrue(
                "테마에 $slot 가 없으면 알림창이 Material 기본 보라색으로 나옴",
                themeText.contains(slot)
            )
        }
    }
}
