# CI 자동검증 조사 — 79일차 마감

> **80일차 후속:** 이 문서의 "CI 없음" 결론은 79일차 당시 사실이야. 80일차에 실제 GitHub Actions CI를 도입했어 — 결과는 [CI-AUDIT-80.md](CI-AUDIT-80.md)를 봐. 아래 79일차 원문은 수정하지 않았어.

확인일: 2026-09-20 / 브랜치: `feature/photo-sticker` / HEAD: `7725ec5e82c7821b067b079dfe08f443b0da6674`

## 결론

**현재 이 브랜치에 push해도 테스트와 빌드를 자동 실행하는 CI는 없어.** CI는 코드를 올렸을 때 사람이 별도로 명령하지 않아도 시험과 빌드를 돌리는 장치야. 현재 테스트는 사람이 직접 명령해야 실행돼. 테스트의 존재와 자동 보호는 별개야.

평가: **매우 약함 — 현재 검증 CI 없음.**

| feature/photo-sticker에 push할 때 | 자동 실행 | 근거 |
|---|---|---|
| JVM unit test 750개 | 아니오 | 실행 workflow 없음 |
| assembleDebug: 앱 빌드 | 아니오 | 실행 workflow 없음 |
| assembleDebugAndroidTest: Android 테스트 APK 빌드 | 아니오 | 실행 workflow 없음 |
| instrumentation 10개: Android 환경에서 실제 시험 | 아니오 | 실행 workflow·emulator 구성 없음 |
| lint: 정적 오류 검사 | 아니오 | 실행 workflow 없음 |

테스트 APK 빌드는 시험지의 실행 가능 형태를 만드는 것이지, 실제 시험을 치르는 것이 아니야.

## 무엇을 확인했나

- 현재 작업트리에는 `.github/` 자체가 없어. 현재 HEAD와 로컬 `origin/main`의 Git 트리에도 `.github` 파일이 없어.
- Git 추적 파일에서 GitHub Actions, Jenkins, GitLab CI, CircleCI, Azure Pipelines, Bitrise, Buildkite, Travis, AppVeyor, TeamCity, Codemagic 등 이름의 설정과 YAML·shell·PowerShell·batch 자동화 파일을 검색했지만 찾지 못했어.
- [루트 Gradle](../../build.gradle.kts), [settings](../../settings.gradle.kts), [앱 Gradle](../../app/build.gradle.kts), [README](../../README.md)를 확인했어. 테스트 dependency와 Android test runner는 있지만 push를 계기로 실행하는 설정은 없어.
- 로컬 Git의 별도 hooksPath 설정은 없고, `.git/hooks`에도 sample 이외 실행 hook은 없어.
- GitHub API로 Actions workflow 목록, 실행 이력, 최신 HEAD의 check-runs·commit status, 현재 브랜치 보호 여부를 읽기 전용으로 확인했어. 원격 설정은 변경하지 않았어.

## GitHub에 남아 있는 과거 workflow — 현재 CI와 구분

GitHub API에는 `.github/workflows/apply-photo-drawer-fix.yml` 등록 1개가 `active`로 남아 있어. 따라서 “GitHub Actions 흔적이 한 번도 없었다”는 말은 틀려. 하지만 현재 브랜치와 기본 브랜치에는 이 파일이 없고, **이 작업은 테스트 CI도 아니었어.**

| 항목 | 과거 파일에서 확인한 내용 |
|---|---|
| 이름 | Apply photo drawer fix |
| 실행 조건 | push |
| 대상 브랜치 | feature/photo-sticker |
| 추가 조건 | workflow 파일 자체가 변경되는 push만 |
| PR / 수동 실행 | 설정 없음 |
| 하는 일 | Python으로 DetailScreen.kt 문자열 수정 후 git commit·push 시도 |
| 권한 | contents: write |
| JVM / 앱 빌드 / lint / Android 테스트 빌드 | 전부 없음 |
| instrumentation / emulator | 없음 |

과거 실제 파일은 [b22292d 시점 workflow](https://github.com/KimJinha-JJin/post-card-memory/blob/b22292dbef3d44a3b958db0e4ad36ed0ebe144d2/.github/workflows/apply-photo-drawer-fix.yml)를 읽었어. [삭제 커밋 7a5ed7d](https://github.com/KimJinha-JJin/post-card-memory/commit/7a5ed7d90234e3bd1941df54c19513bd74b62f02)은 2026-07-02에 이 파일 196줄을 삭제했어.

Actions 실행 이력은 총 4건이고 모두 이 과거 workflow의 push 실행, 결과는 failure야. 이 실패를 JVM 테스트 실패로 해석하면 안 돼. 테스트 명령이 없는 코드 수정 작업의 실패 기록이야. 실패 원인의 추가 감사는 이번 범위에서 하지 않았어. [Actions 기록](https://github.com/KimJinha-JJin/post-card-memory/actions)

## 최신 원격 상태와 실패 차단 여부

- `git ls-remote origin refs/heads/feature/photo-sticker`의 실제 원격 HEAD가 로컬 HEAD와 같아.
- 최근 3개 커밋 `d968f3c → 6e5e37f → 7725ec5`이 원격 브랜치 이력에도 있어. local/origin ahead-behind는 `0/0`이야.
- [최신 HEAD](https://github.com/KimJinha-JJin/post-card-memory/commit/7725ec5e82c7821b067b079dfe08f443b0da6674)의 check-runs는 0개, commit status도 0개야. API의 합성 상태 문자열 `pending`은 검사 0개인 이 응답에서 실행 중인 시험이 있다는 증거가 아니야.
- 현재 브랜치 API의 `protected`는 `false`야. 확인 범위에서 필수 테스트를 통과해야 하는 보호 설정은 발견하지 못했어.
- 현재는 자동 검사가 없어서 테스트 실패를 표시하거나 그것으로 push를 차단하는 경로도 없어. 과거 workflow는 push 뒤 실행되는 작업이었으므로, 그 실패는 이미 접수된 push 자체를 거절한 것이 아니야.

저장소 밖 외부 서비스, 조직 수준 정책, 모든 webhook 설정까지 전수 확인한 것은 아니야. 그 범위는 **미확인**으로 남겨. 다만 현재 저장소 설정과 최신 커밋 검사 기록에서 자동 테스트·빌드의 증거는 없어.

## 최소 비용 개선 후보 — 제안만, 미구현

향후 별도 승인 작업에서 push / pull request 대상 브랜치를 명시하고 아래 순서로 구성하면 돼.

1. `testDebugUnitTest`: 현재 JVM 테스트부터 자동 실행.
2. `assembleDebug`: 앱이 빌드되는지 자동 확인.
3. `assembleDebugAndroidTest`: Android 테스트 코드가 빌드되는지 확인. 같은 단계에 `git diff --check`에 해당하는 변경분 공백 검사도 포함하되 PR 비교 기준을 명시.

실제 instrumentation 실행은 별도 emulator 환경을 준비한 뒤 다음 단계로 분리할 수 있어. **위 3단계만으로 Android 실제 실행까지 보호된다고 표현하면 안 돼.**

환경 준비 시 저장소에 Gradle Wrapper 실행 스크립트와 jar가 없다는 점을 고려해야 해. 현재 [wrapper properties](../../gradle/wrapper/gradle-wrapper.properties)는 Gradle 9.4.1을 가리키고, 앱 설정은 JDK 17·compileSdk 37 기준이야. 단순히 `./gradlew`만 적으면 된다고 가정하지 말고, 기존 버전에 맞는 실행 환경 확보 방법부터 정해야 해. 이번에는 Gradle·SDK 설치나 dependency·workflow 변경을 하지 않았어.

## 오늘 실행하지 않은 것

테스트·빌드·instrumentation 재실행, emulator·SDK 설치, CI 파일 작성·수정, GitHub 설정 변경은 전부 하지 않았어. 기존 JVM 성공 기록과 instrumentation 컴파일 기록은 [테스트 보호지도](TEST-COVERAGE-MAP.md)에서 최신 실제 실행 여부와 나눠 설명했어.
