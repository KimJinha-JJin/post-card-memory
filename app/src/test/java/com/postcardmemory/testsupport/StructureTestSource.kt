package com.postcardmemory.testsupport

import java.io.File

/**
 * 화면 구조에 대한 요구사항 상당수를 production 소스 텍스트를 직접 읽어 고정하는
 * "구조 테스트"로 지킨다. 그 테스트들이 모두 똑같이 필요로 하는 파일 읽기만 여기 모아 둔다.
 *
 * **왜 행동 테스트가 아니라 구조 테스트인가** (78일차 호적 조사에서 실제 상태를 다시 확인함):
 * Compose UI 테스트 라이브러리(`androidx.compose.ui:ui-test-junit4`)는 이미 프로젝트에
 * 들어와 있고 `app/src/androidTest`에서 실제로 쓰인다(`PostcardBackRenderingTest`). 없는
 * 것은 **JVM unit test 쪽 하네스**다 — Robolectric이 없어서 `src/test`에서는 Composable을
 * 렌더링하거나 ViewModel을 만들 수 없다. androidTest로 옮기면 검증할 수 있지만 그건
 * instrumented 실행이 필요하고, 실사용 기기에서의 계측 실행은 `AGENTS.md` 5절로 금지돼
 * 있으며 emulator는 아직 준비돼 있지 않다. 그래서 "지금 당장 확인할 수 있는 유일한 수단"이
 * 소스 텍스트다.
 *
 * 따라서 구조 테스트는 **대체 수단이 없을 때만** 쓴다. 순수 함수·저장소·enum처럼 직접
 * 호출할 수 있는 대상은 구조 테스트가 아니라 행동 테스트로 검증한다(78일차에 그런 4건을
 * 실제 호출 테스트로 교체했다).
 *
 * [candidates]를 여러 개 받는 이유는 test runner의 작업 디렉터리가 모듈 루트(`app/`)일
 * 때와 저장소 루트일 때 모두 있었기 때문이다 — 먼저 실제로 존재하는 경로를 쓴다. 못 찾으면
 * 조용히 통과시키지 않고 현재 cwd와 후보 목록을 붙여 바로 실패시킨다(경로가 바뀐 것을
 * "검증할 게 없어서 통과"로 오인하지 않기 위해서다).
 */
fun readStructureTestSource(candidates: List<String>): String {
    val file = candidates
        .map { File(it) }
        .firstOrNull { it.exists() }
        ?: error(
            "소스 파일을 찾을 수 없음(cwd=${File(".").absolutePath}). " +
                "candidates=$candidates"
        )
    return file.readText()
}
