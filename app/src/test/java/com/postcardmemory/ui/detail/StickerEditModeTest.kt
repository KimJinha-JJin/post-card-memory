package com.postcardmemory.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 스티커 편집 모드 enum의 구성원과 순서를 **실제 값으로** 고정한다.
 *
 * 78일차 호적 조사에서 이 자리를 지키던 구조 테스트
 * (`StickerEditModeToolbarStructureTest.stickerEditModeEnum_visibilityWidenedButMembersUnchanged`)
 * 를 대체한다. 그 테스트는 DetailScreen.kt 전체 텍스트에 "Move"/"Scale"/
 * "Rotate"라는 흔한 단어가 들어 있는지만 봐서 어떤 변경으로도 깨지지 않는
 * 사실상 항상 참인 검사였다. 여기서는 enum을 직접 읽으므로
 *
 * - 멤버를 지우거나 이름을 바꾸면 컴파일이 깨지고,
 * - 순서를 바꾸거나 멤버를 추가하면 이 테스트가 실패하며,
 * - enum을 다시 `private`으로 좁히면 이 파일이 컴파일되지 않는다
 *   (테스트 소스는 `internal`까지만 볼 수 있다).
 *
 * 현재 상태(98일차 확인): 선택한 사진 스티커의 툴바(StickerEditModeToolbar)는
 * 이 enum을 쓰지 않고 "배경제거/원본복원 · 뒤로 · 앞으로" 세 액션만 보여 준다.
 * DetailScreen의 `stickerEditMode`는 Move로 초기화된 뒤 다시 대입되는 곳이 없어,
 * 제스처 분기의 Scale/Rotate 쪽은 지금 사용자가 도달할 수 없다(production 정리
 * 후보로만 기록 — 이 테스트는 enum의 현재 구성만 고정한다).
 */
class StickerEditModeTest {

    @Test
    fun hasExactlyMoveScaleRotate_inThatOrder() {
        assertEquals(
            listOf(StickerEditMode.Move, StickerEditMode.Scale, StickerEditMode.Rotate),
            StickerEditMode.entries.toList()
        )
    }
}
