package com.postcardmemory.testsupport

import java.io.File

/**
 * 이 프로젝트는 Compose UI 테스트 인프라를 쓰지 않아서, 화면 구조에 대한 요구사항 상당수를
 * production 소스 텍스트를 직접 읽어 고정하는 "구조 테스트"로 지킨다. 그 테스트들이 모두
 * 똑같이 필요로 하는 파일 읽기만 여기 모아 둔다.
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
