# CI 자동검증 도입 — 80일차 후속

확인일: 2026-09-21 / 브랜치: `feature/photo-sticker` / 시작 HEAD: `82471674eb8c2f606c09f26036db7a10b0fb9b16` / 종료 HEAD: `57748c7770b1b849b7365dce8a6705dda8780340`

[79일차 CI 조사](CI-AUDIT-79.md)는 "**현재 CI 없음**"이 결론이었어. 80일차는 그 공백을 실제로 메운 결과야. 79일차 원문은 수정하지 않았고, 이 문서는 그 뒤에 일어난 일만 다뤄.

## 결론

**`feature/photo-sticker`에 push하거나 pull request를 열면 GitHub Actions가 JVM unit test·debug 빌드·Android 테스트 APK 컴파일을 자동으로 실행해.** 사람이 명령하지 않아도 실행된다는 뜻이야.

평가: 최소 CI 구축 완료 + 권장 성공(3단계 모두 자동 실행) 달성. **같은 날 이어서 안전한 테스트 전용 emulator(`PostcardMemory_Test`, API 37)를 만들어 instrumentation 10건을 실제로 처음 실행했고, 그 결과 real production Room migration 버그 하나를 실제로 찾아 최소 수정까지 마쳤어.** 자세한 내용은 아래 "instrumentation 실제 실행" 절.

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

## instrumentation 실제 실행 (같은 날, 로컬 emulator — GitHub Actions 아님)

CI 문서 갱신을 마친 뒤 사용자가 Android Studio Device Manager로 **테스트 전용 AVD `PostcardMemory_Test`(API 37, `sdk_gphone16k_x86_64`)를 직접 부팅**했어. 실사용 기기와 혼동하지 않도록 실행 전에 `adb devices -l`로 `emulator-5554` 하나만 연결돼 있는지 매번 재확인한 뒤 로컬 Gradle(`connectedDebugAndroidTest`, `ANDROID_SERIAL=emulator-5554` 고정)로 실행했어. GitHub Actions에는 여전히 emulator가 없어 — 이 실행은 CI가 아니라 로컬에서 사용자가 준비한 emulator야.

### 1차 실행 — 5/10 성공

```yaml
PostcardBackMigrationTest: 1/1 성공
PostcardBackSaveTest: 2/2 성공
PostcardBackgroundColorSaveRaceTest: 2/2 성공
PostcardBackRenderingTest: 0/3 성공 (3건 실패)
PostcardFullMigrationChainTest: 0/2 성공 (2건 실패)
```

- **PostcardBackRenderingTest 3건**: `NoSuchMethodException: android.hardware.input.InputManager.getInstance []` — Espresso가 `onIdle` 처리 중 이 메서드를 reflection으로 찾다 실패. API 37은 아주 최신 SDK라 현재 프로젝트가 쓰는 Espresso/androidx.test 버전이 이 플랫폼의 InputManager reflection 시그니처를 아직 지원하지 못하는 것으로 보여. **test infrastructure(emulator API ↔ Espresso 버전) 호환 문제로 분류. production 문제 아님.**
- **PostcardFullMigrationChainTest 2건**: `IllegalStateException: Migration didn't properly handle: postcards(...)`. 두 테스트가 정확히 같은 3개 컬럼에서 동일하게 실패해 재현성 있는 production 버그로 판단했어.

### Room migration 버그 — 원인 확정과 수정

`19.json` schema export와 `PostcardDatabase.kt`의 전체 migration chain을 대조해 원인을 특정했어. 세 컬럼 모두 **entity가 기대하는 default와 실제로 그 컬럼을 만든 migration SQL의 default 선언이 어긋나 있었고**, 그 컬럼을 다루는 다른 migration은 없어서(전체 재생성은 `MIGRATION_2_3` 단 한 번뿐) 원인이 각각 하나로 좁혀졌어.

| 컬럼 | expected (schema export) | found (실제 migration SQL 결과) | 원인 |
|---|---|---|---|
| `message` | `defaultValue=''` | `defaultValue=undefined`(default 없음) | `MIGRATION_1_2`는 `DEFAULT ''`로 정확히 추가했지만, 바로 다음 `MIGRATION_2_3`이 테이블 전체를 재생성(`CREATE TABLE postcards_new`)하면서 그 `DEFAULT ''`를 빠뜨림 |
| `futureMailDeliverAt` | `defaultValue=undefined`(선언 없음) | `defaultValue='NULL'`(명시적 리터럴) | `MIGRATION_15_16`이 `ADD COLUMN ... INTEGER DEFAULT NULL`로 **명시적** `DEFAULT NULL`을 씀. entity에는 default 선언이 없어 SQLite가 기록하는 "default 없음"과 달라짐 |
| `envelopeStyle` | `defaultValue=undefined`(선언 없음) | `defaultValue='NULL'`(명시적 리터럴) | `MIGRATION_16_17`도 동일하게 `ADD COLUMN ... TEXT DEFAULT NULL` — 같은 원인 |

**위험 판단:** migration은 기기가 그 버전 구간을 지나갈 때 한 번만 실행돼. 이미 v19에 도달한 기기는 이 코드를 다시 실행하지 않으므로, 이 수정은 **아직 v3/v16/v17 미만에서 올라오는 기기(새 설치 포함)의 미래 실행 경로만** 바꿔. 버전 증가나 새 migration 추가 없이 기존 3개 migration의 SQL 문자열만 고쳤고, 데이터 삭제나 destructive 요소는 없어 — STOP 대상이 아니라고 판단해 최소 수정을 진행했어.

**수정 (`app/src/main/java/com/postcardmemory/data/PostcardDatabase.kt`, 3줄):**

```diff
- message TEXT NOT NULL,
+ message TEXT NOT NULL DEFAULT '',
```
```diff
- ADD COLUMN futureMailDeliverAt INTEGER DEFAULT NULL
+ ADD COLUMN futureMailDeliverAt INTEGER
```
```diff
- ADD COLUMN envelopeStyle TEXT DEFAULT NULL
+ ADD COLUMN envelopeStyle TEXT
```

**별개로 남겨둔 의문(오늘 조사 범위 밖):** 이 검증은 Room이 실제 앱에서 DB를 열 때와 동일한 경로로 일어나. 즉 이미 v19에 있는 기존 실사용 기기도 과거에 이 정확한 스키마 상태를 지나며 한 번은 이 검증을 통과했어야 해. 지금까지 조용했다면 문제가 없었다는 뜻일 수도 있고 아직 드러나지 않은 것일 수도 있어 — 오늘은 더 조사하지 않고 후속으로 남겼어.

### 2차 실행 — Room migration만 재실행: 2/2 통과

수정 후 `adb devices -l`로 `emulator-5554` 단일 대상 재확인 → `connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.postcardmemory.PostcardFullMigrationChainTest`로 두 테스트만 재실행 → **2/2 통과**.

### 3차 실행 — instrumentation 전체 10건 재실행: 7/10 성공

```yaml
PostcardBackMigrationTest: 1/1 성공
PostcardBackSaveTest: 2/2 성공
PostcardBackgroundColorSaveRaceTest: 2/2 성공
PostcardFullMigrationChainTest: 2/2 성공 (수정 후 — 이전엔 0/2)
PostcardBackRenderingTest: 0/3 성공 (동일하게 실패 — API 37/Espresso 환경 문제, 손대지 않음)
합계: 7/10 성공, 3/10 실패(test infrastructure), 0 skipped
```

### 로컬 회귀검증 (production 수정 후)

```yaml
testDebugUnitTest: BUILD SUCCESSFUL, JVM @Test 750개, failures 0, errors 0 (수정 전과 동일 — 회귀 없음)
assembleDebug: BUILD SUCCESSFUL
assembleDebugAndroidTest: BUILD SUCCESSFUL
git diff --check: 통과
```

## 아직 하지 않은 것 — 정직하게 구분

- **instrumentation은 여전히 GitHub Actions CI에는 없어.** 오늘 실행한 10건은 사용자가 로컬 Android Studio에서 직접 부팅한 emulator 위에서, 로컬 Gradle로 실행한 거야. CI의 `assembleDebugAndroidTest`는 여전히 "컴파일된다"만 보장해.
- PostcardBackRenderingTest 3건은 여전히 실패 상태로 남아 있어. API 37/Espresso 호환 문제로 분류했고, 오늘은 이 3건 때문에 production을 건드리지 않았어.
- CI 로그에 Node.js 20 deprecation, `actions/setup-java@v4` deprecation 경고가 떴어. 오늘 실행 성공에는 영향 없는 정보성 경고라 고치지 않았어.
- Gradle wrapper 파일(`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`)은 여전히 저장소에 없어.
- 실사용 기기는 오늘 전 과정에서 한 번도 사용하지 않았어 — `adb devices -l`을 매 실행 직전 재확인해 `emulator-5554` 단일 대상임을 검증했어.

## 후속 후보

- Node.js/setup-java deprecation 경고 정리(`actions/setup-java@v5`).
- API 37 / Espresso `InputManager.getInstance()` 호환 문제 — androidx.test/espresso 버전 업 또는 API 37 emulator 자체의 알려진 이슈인지 확인 필요.
- CI에 lint, `git diff --check`류 공백 검사를 추가할지는 별도 승인 필요.
- (신규) postcard-memory workflow 후속 보강 필요: push 후 CI 결과 확인 절차, instrumentation은 emulator 전용이라는 원칙, 실행 전 `adb devices` 실측, 물리 기기 존재 시 STOP, 실패를 CI 환경/test infrastructure/production으로 분류하는 절차를 문서화. workflow 파일 자체는 오늘 수정하지 않았어.
