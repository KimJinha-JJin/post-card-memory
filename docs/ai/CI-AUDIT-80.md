# CI 자동검증 도입 — 80일차 후속

확인일: 2026-09-21 / 브랜치: `feature/photo-sticker` / 시작 HEAD: `82471674eb8c2f606c09f26036db7a10b0fb9b16` / 종료 HEAD: `57748c7770b1b849b7365dce8a6705dda8780340`

[79일차 CI 조사](CI-AUDIT-79.md)는 "**현재 CI 없음**"이 결론이었어. 80일차는 그 공백을 실제로 메운 결과야. 79일차 원문은 수정하지 않았고, 이 문서는 그 뒤에 일어난 일만 다뤄.

## 결론

**`feature/photo-sticker`에 push하거나 pull request를 열면 GitHub Actions가 JVM unit test·debug 빌드·Android 테스트 APK 컴파일을 자동으로 실행해.** 사람이 명령하지 않아도 실행된다는 뜻이야.

평가: 최소 CI 구축 완료 + 권장 성공(3단계 모두 자동 실행) 달성. instrumentation의 실제 emulator 실행은 별도 단계로, 이 문서 하단에 있는 그대로 미확인이야.

## 무엇을 만들었나

- 신규 파일: `.github/workflows/android-ci.yml`.
- trigger: `push`, `pull_request` (특정 브랜치로 제한하지 않음 — 현재 개발이 feature 브랜치 직접 push 중심이라 하드코딩할 근거가 없었음).
- 3단계: `testDebugUnitTest` → `assembleDebug` → `assembleDebugAndroidTest`. 각각 별도 step이라 실패 지점이 로그에서 구분돼.
- 저장소에 `gradlew`/wrapper jar가 없어서(기존부터 그런 상태) wrapper 대신 `gradle/actions/setup-gradle@v4`로 Gradle 9.4.1을 설치해 `gradle` 명령을 직접 사용해. wrapper 파일은 오늘 추가하지 않았어 — 그건 이번 승인 범위 밖의 별도 판단이라 후속으로 남겨.
- production 코드, compileSdk, Gradle 구조는 변경하지 않았어.

## 실제 GitHub Actions 실행 — 두 번

### 1차 실행 (commit e2a0fad, run 35562870949) — 실패

- 실패 step: SDK 설치 단계에서 `sdkmanager "platforms;android-37" "build-tools;37.0.0"`를 원격 저장소에서 새로 받으려 시도.
- 실패 로그: `Warning: Failed to find package 'platforms;android-37'` → exit code 1.
- 원인 분류: **Android SDK**. API 37 자체가 없는 게 아니라, sdkmanager 표준 패키지 id 표기와 실제 존재 여부를 확인하지 않고 곧바로 설치를 시도한 게 문제였어.
- production 문제 여부: 아니오. Gradle·테스트 단계에 도달하기 전에 멈췄어.

### 2차 실행 (commit 57748c7, run 35563162247) — 성공

실패 후 바로 고치지 않고, 먼저 이 CI가 실제로 사용한 runner 이미지(`ubuntu24/20260907.300`)의 [공식 소프트웨어 목록](https://github.com/actions/runner-images/releases/tag/ubuntu24%2F20260907.300)을 확인했어. 그 이미지에는 이미 다음이 기본 내장돼 있었어.

```yaml
ANDROID_SDK_ROOT: /usr/local/lib/android/sdk
Android SDK Platforms: android-37.2-beta3 / beta2 / beta1 / android-37.2 / android-37.1 / android-37.0 (rev 2) / android-36.1 / ... / android-34
Android SDK Build-tools: 37.0.0 / 36.0.0 / 36.1.0 / 35.0.0 / 35.0.1 / 34.0.0
```

즉 **필요한 SDK는 이미 runner에 다 있었고, 새로 받을 필요가 없었어.** 그래서 워크플로를 다음처럼 최소 수정했어.

- 삭제: `sdkmanager "platforms;android-37" "build-tools;37.0.0"` (존재하지 않는 패키지 id로 불필요한 원격 설치를 시도하던 명령).
- 추가: `ls "$ANDROID_HOME/platforms"`, `ls "$ANDROID_HOME/build-tools"` — runner에서 직접 실측해 로그로 남김.
- 유지: `sdkmanager --licenses` 라이선스 수락만.

이 수정된 workflow가 실제 runner에서 다음을 실측으로 확인했어(로그 원문).

```text
ANDROID_HOME=/usr/local/lib/android/sdk
...
android-37.0
...
37.0.0
```

그 뒤 3단계 전부 성공했어.

```yaml
testDebugUnitTest: BUILD SUCCESSFUL in 3m 13s (32 actionable tasks: 32 executed)
assembleDebug: BUILD SUCCESSFUL in 1m 47s
assembleDebugAndroidTest: BUILD SUCCESSFUL in 11s
```

compileSdk를 낮추지 않았고(37 유지), targetSdk도 건드리지 않았고, Gradle wrapper도 추가하지 않았어. 두 커밋 다 `.github/workflows/android-ci.yml` 한 파일만 변경했어.

## 로컬 검증 (같은 코드 기준)

CI workflow를 처음 작성한 직후, push 전에 로컬에서 실행해 확인했어(JAVA_HOME은 Android Studio 번들 JBR, Gradle은 로컬 캐시된 9.4.1 배포판 사용).

```yaml
testDebugUnitTest: BUILD SUCCESSFUL, JVM @Test 750개, failures 0, errors 0
assembleDebug: BUILD SUCCESSFUL
assembleDebugAndroidTest: BUILD SUCCESSFUL
```

두 번째 workflow 수정(SDK 설치 step 제거)은 CI YAML만 바뀐 것이라 Gradle 결과에 영향이 없어 로컬 재실행은 생략했어.

## 아직 하지 않은 것 — 정직하게 구분

- **instrumentation 10건의 실제 emulator 실행은 하지 않았어.** 오늘 CI가 확인한 건 `assembleDebugAndroidTest`, 즉 계측 테스트 코드가 최신 소스 기준으로 **컴파일**된다는 것뿐이야. `connectedDebugAndroidTest`를 CI에 넣지 않았고 GitHub Actions에는 emulator도 없어.
- CI 로그에 Node.js 20 deprecation, `actions/setup-java@v4` deprecation 경고가 떴어. 오늘 실행 성공에는 영향 없는 정보성 경고라 고치지 않았어. 후속 후보로만 남겨.
- Gradle wrapper 파일(`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`)은 여전히 저장소에 없어. 오늘은 `setup-gradle` action으로 우회했을 뿐 이 상태 자체를 바꾸지 않았어.

## 후속 후보

- Node.js/setup-java deprecation 경고 정리(`actions/setup-java@v5`).
- 안전한 테스트 전용 emulator 구축과 instrumentation 10건 실제 실행(이 문서와 별도 판단, 80일차 세션에서 이어서 조사).
- CI에 lint, `git diff --check`류 공백 검사를 추가할지는 별도 승인 필요.
