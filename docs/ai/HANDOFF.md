# HANDOFF — 2026-11-18 136일차: C2 실제 앱 이식·미세 그림자 / PR·병합 마감

패코(Codex), 수동 표준 모드. 사용자 136일차 구현 지시와 C2 최종 확정, 후속 상세 우표 그림자 조정 요청을 수행했어. [상세 검증·시각 비교](../qa/day136/README.md), [직전 135일차 HANDOFF 원문](archive/HANDOFF-before-day136-2026-11-18.md)을 확인해. 이전 원문은 바이트 그대로 보존했어.

## 현재 결과와 사용자 확인

- 구현 **완료**: C2 단일 윤곽을 공통 Android Path로 이식했어. C1/C3는 비교 참고용 목업으로 그대로 남겼고 섞지 않았어. C2는 HTML `shallowAligned(C2)`의 16분할·입구48%·깊이15%·연결부52%·동일 원 격자 모서리가 기준이야.
- 공통 `traceStampPerforation` → `PostcardRenderSpec.createPinkingPath` → 기존 Compose wrapper와 앞면 renderer. 사진과 흰 여백이 같은 외곽을 쓰고, 두 배 중심 stroke를 clip해서 기존 명목 안쪽 여백을 남겨. 사진 비율·crop·zoom/offset·우표 배치를 바꾸지 않았어.
- 사용자 실기기 QA **완료**: 갤러리·카메라·상세/편집·공유·내보내기 동작 모두 정상 확인. 마지막 그림자 변경 뒤에는 **상세와 공유/내보내기 모두 괜찮아**라고 재확인했어.
- 후속 제품 결정: 사용자는 정지 갤러리가 아니라 **상세/편집 우표 뒤 회색 그림자**를 조정 대상으로 지정했어. 현재/약화/미세 엠보싱 비교 후 **미세 엠보싱안** 선택. 기존 그림자 색·inset9·blur0은 유지하고, 알파85→18/255·논리 offset22/26→11/13만 변경했어. 공통 앞면 공유/내보내기에도 동일하게 적용돼. 새 emboss filter·하이라이트·갤러리 정지 shadow·전환 효과는 추가하지 않았어.
- 필요한 사용자 QA를 마쳤으므로 원 지시의 commit/push·별도 구현 PR·CI 확인으로 진행할 수 있어. **main 병합은 여전히 미승인**이야.

## 검증을 구분해서 읽어

- **로컬 자동검증 — 실행/통과:** 최종 Gradle9.8.2/Zulu17 `testDebugUnitTest assembleDebug assembleDebugAndroidTest` 성공. JVM **1295/1295**, XML138개, 실패·오류·skip0. source `@Test`1295 / test 파일137 / helper1. 신규 `StampPerforationTest`6건은 production 순수 계산을 실행해 HTML 독립 fixture·다섯 크기·입구/깊이/연결부·모서리 원호 연결/폐합·회전 대칭·좌표 범위를 검사해. TEST-COVERAGE-MAP **갱신 완료**. instrumentation21건/8파일 무변경, 컴파일만 확인했어.
- **PC 시각 비교 — 실행:** 기존 프로젝트 카메라 사진과 실제 빌드된 Kotlin 윤곽을 Java2D로 재현. 93.33/106.67/146.67/60/38dp, 네 모서리6배, 그림자 세 강도를 확인했어. 테두리 변경 전후의 사진 내부(가장자리12dp 제외)는 다섯 크기 모두 차이0픽셀. Android rasterization/Compose golden이 아니라 대체 renderer 근거야. 기존 golden은 갱신하지 않았어.
- **실사용 기기 instrumentation — 미실행:** SAFE 테스트도 별도 승인·실행하지 않았어. CONDITIONAL/FORBIDDEN은 격리 환경이 없어 **실행 불가**. emulator·connected task·uninstall·초기화는 실행0회야.
- **실기기 감각 QA — 실행/사용자 정상 확인:** 사용자 연결·설치 승인 후 SM-S936N 한 대, 실측1080×2340/450dpi를 확인. signing certificate SHA-256 일치 확인 후 같은 기기에 `adb install -r`만 사용했고 최초 설치 시각 유지 확인. 사용자 직접 앱 실행·편집·공유/내보내기로 QA했어. 에이전트가 사용자 엽서를 자동으로 수정·저장·공유/내보내기하지 않았어. 개인 화면 캡처와 기존 설치 APK는 PC 임시 폴더에만 있고 이 PR에는 없어. 최초 설치 시각 유지는 DB·전체 파일의 byte 보존 검사는 아니야.
- **GitHub Actions CI — 작성 시점 미실행:** 구현 commit/push/PR 이전 기록이야. 최종 PR·head/base SHA·CI 실제 step 결과는 대화 완료보고에 남겨. 135일차 PR#30의 CI(run164)는 JVM·앱APK·테스트APK 컴파일 모두 실제 성공했지만 이번 구현의 CI 근거로 대신하지 않아.

## 시행착오·남은 한계

- 첫 sandbox/offline Gradle은 플러그인 캐시 해석 실패로 코드 검사 전 중단. 실행환경 문제로 분류했고 dependency·Gradle·설정 변경 없이 기존 Gradle/SDK 접근으로 해결했어. Java2D 도구도 빌드 클래스 접근이 가능한 실행 환경에서 재현했어.
- 첫 C2 구현에서 그림자 inset9까지 없앤 회귀가 있었어. 사용자가 상세 우표 뒤 이중 윤곽이 어색하다고 보고했고 캡처에서도 관찰했어. **그림자 보정1회차:** 원래 inset9 복원·JVM/앱 재검증·같은 승인 범위 데이터 보존 설치. 이후 사용자가 미세 종이 느낌으로 강도/범위 비교를 명시 요청했어. 비교 뒤 선택한 알파18·offset11/13 적용을 **2회차 최종 보정**으로 기록해. 최종 실기기 재확인 정상이며 같은 문제에 추가 production 보정은 하지 않았어.
- 38dp의 C2 깊이는 약.362dp,60dp는 약.571dp라 작으면 거의 사각형처럼 읽힐 수 있어. 승인된 C2를 그대로 유지했고 깊이를 임의로 키우지 않았어. 실제38/60dp Android pixel 계측·다른 기기/밀도·성능 측정은 미실행이야. 현재 정사각형 호출부가 기준이며 다른 종횡비의 RectF는 단위 윤곽 축별 확대라 원호가 타원이 돼.
- 정지 월별 갤러리는 기존에도 shadow가 없고 집기 순간 shadow는 별도 경로야. 그 경로·갤러리 이동·펀칭 애니메이션·효과음은 수정하지 않았어. 후속 후보로 자동 확대하지 않아.

## 보호 상태와 Git 출발점

- Room Entity/schema/DAO/Migration·저장/삭제 로직·dependency·Gradle/CI·운영 이미지 자원은 무변경. 원본 프로젝트의 개인정보 보류/SAFE 보호 기준 `protected-baseline.json` **38개 모두 SHA-256 일치**, 변경0·누락0을 작업 중 재확인했어. 이 기준 밖의 모든 보호/백업 위치까지 전수 검증했다고 확대하지 않아. 원본 폴더 파일에는 쓰지 않았고 공유 Git 메타데이터의 승인된 브랜치 작업만 했어.
- 최종 설치 APK SHA-256: `facc2c8f397af8c10e2af47695e4125748265b9d97ed070bcde55aa4b37085e9`.
- 시작 worktree는 개발용 `post-card-memory-next`, 시작 브랜치 `design/day135-stamp-edge-study`, HEAD=추적ref=실제 PR#30 head **860bc728e478c0ad63b519ce30e55793742a08e9**. 추적·미추적 변경 없이 깨끗했어. local main=origin/main=GitHub main **3665af30b5a3f6d5568cc94fd857c86d19807b4b**. 별도 136일차 파일은 찾지 못해 기존 135일차 HANDOFF·조사 보고·목업·PR을 대조했고 최종 후보는 사용자 보완 지시로 확정했어.
- 새 구현 브랜치 `feat/day136-c2-stamp-perforation`를 860bc72에서 만들었어. 기존 조사 PR#30과 브랜치는 수정·삭제하지 않았어. 구현 PR은 **design/day135-stamp-edge-study를 base로 하는 별도 stacked PR**로 만들 예정이라 135일차 목업 diff와 구현 diff가 분리돼. 현재 HEAD는 새 commit 전860bc72이며 commit·push는 작성 시점 미실행이야. 최종 Git 결과는 대화 완료보고 기준이야.
- 훅 실제 실행 점검: pre-commit/pre-push 이미 활성, 로컬 설정 변경 없음. 명시한12파일만 stage하고 훅 검사 뒤 commit/push → 구현 PR → 필수CI → 결과 보고. 최종 성공만 기록하려고 추가 HANDOFF commit을 만들지 않아.
- 재개/병합: main 병합 승인 전에는 어떤 PR도 병합하지 않아. 이후 사용자가 검토한 정확한 PR·head/base·CI에 대해 승인하면 조사PR#30 선행 관계와 구현PR base를 다시 확인해야 해. 기존 브랜치를 삭제하거나 자동 병합하지 않아.
