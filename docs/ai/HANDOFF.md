# HANDOFF — 81일차 1·2단계 완료 / 3단계 workflow 안전 규칙 보강

확인일: 2026-09-22. 수동 표준 모드. 81일차 1단계 Espresso/API 37 호환 개선과 2단계 삭제 오케스트레이션 보호 테스트는 commit·push·CI까지 완료됐어. 3단계는 그 결과를 프로젝트 공통 규칙과 workflow canonical source에 반영하고 설치 cache를 동기화한 문서-only 작업이며, **이번 3단계 commit/push는 사용자 승인 대기 상태야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`
- 현재 HEAD와 origin: `da78619d384dd22a0ed2dcaf9ae213332ca1d909` / fetch 후 ahead·behind `0/0`
- 시작 작업트리: tracked 변경 없음. 보호 untracked `.claude/`, `.codex-config.candidate.toml`, `.kotlin/`만 존재했고 모두 보존했어.
- 80일차 migration 수정은 `61d8e54`로 이미 commit/push 완료됐어.
- 81일차 1·2단계는 `da78619`(`Fix Espresso/API 37 test infra and add deletion orchestration guard tests`)로 commit/push 완료됐어.
- 3단계 저장소 변경: `AGENTS.md`, `docs/ai/HANDOFF.md`. production·test·dependency·CI YAML 변경은 0건이야.
- workflow canonical source는 7개 문서와 설치용 version marker를 갱신했고, 설치 cache `0.1.0+codex.20260922080652`에 정상 반영했어. source/cache 전체 16개 파일은 바이트 단위로 일치해.

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 1·2단계 완료 / 3단계 문서 보강 완료 | 실제 Git diff·commit과 canonical/cache 대조 |
| 로컬 자동검증 | 실행 — 750/750 통과 | 피코의 1·2단계 완료 결과. 3단계는 docs-only라 재실행하지 않음 |
| emulator instrumentation | 실행 — 13/13 통과 | 피코의 검증 전용 API 37 emulator 실행 결과 |
| GitHub Actions CI | 실행 — 성공 | run `35701039918`, HEAD `da78619`; JVM unit·debug APK·instrumentation test compile 세 step 직접 재확인 |
| 실기기 감각 QA | 불필요 | 1·2단계는 테스트 인프라·보호 테스트, 3단계는 문서-only. 실사용 기기 자동 검증 없음 |
| commit | 1·2단계 완료 / 3단계 미실행 | 3단계 사용자 승인 대기 |
| push | 1·2단계 완료 / 3단계 미실행 | 3단계 사용자 승인 대기 |

## 81일차 1단계 — Espresso/API 37 호환 개선

- 기존 `espresso-core 3.5.1`이 API 37에서 hidden API `InputManager.getInstance()`를 reflection으로 호출해 `NoSuchMethodException`이 발생했고, `PostcardBackRenderingTest` 3건이 assertion 전에 실패했어.
- `espresso-core 3.7.0`, `androidx.test.ext:junit 1.3.0`으로 두 테스트 dependency만 최소 갱신했고 production/test 코드는 바꾸지 않았어.
- hidden API 오류가 제거돼 rendering 3건이 실제 실행됐어. emulator가 `mWakefulness=Asleep`이면 draw pass가 없어 테스트의 `withTimeout(5_000)`이 발동할 수 있었고, 화면을 깨운 뒤 3/3 통과했어. 이 실패는 production이 아니라 `emulator / OS environment`로 분류해.
- 장기 규칙은 특정 버전 숫자가 아니라 Compose·androidx.test·Espresso 세대, 대상 API 호환성, resolved classpath를 함께 확인하는 방식으로 남겼어.

## 81일차 2단계 — 삭제 오케스트레이션 보호 검증

- production `PostcardDeletionManager`는 DB 삭제가 성공한 뒤에만 파일 정리를 호출하고, DB 삭제 실패·예외 시 파일 정리 함수 자체를 호출하지 않아. production 버그와 production 변경은 없었어.
- 신규 instrumentation `PostcardDeletionOrchestrationTest` 3건으로 `DB 실패 → 파일 삭제 0건·DB 행 유지`, `DB 성공 → 관련 파일 삭제`, `동일 삭제 2회 → 안전한 멱등성`을 실제 Room과 test 전용 filesDir에서 검증했고 3/3 통과했어.
- 실패 주입은 `Room.inMemoryDatabaseBuilder`와 `object : PostcardDao by realDao { override ... throw ... }` 조합을 사용했어. production 구조를 테스트 편의로 바꾸지 않고 실제 Room 동작을 유지하면서 특정 DAO 메서드만 실패시킨 재사용 가능한 권장 패턴이야.

### 삭제 위험 구분

- 금지 위험: 사용자 파일은 삭제됐지만 DB 행이 남아 깨진 엽서가 되는 상태.
- 설계상 수용한 약한 위험: DB 삭제 성공 뒤 파일 정리 전에 프로세스가 종료돼 DB 행은 없고 파일만 남는 고아 파일. 더 위험한 반대 상태를 피하기 위한 trade-off이며 읽기 전용 `OrphanFileDiagnostics`로 진단할 수 있어.
- 이 구분은 고아 파일 자동 삭제 승인이나 실사용 기기 파괴 테스트 허용을 뜻하지 않아.

## 81일차 3단계 — 공식 workflow로 승격한 규칙

- 검증을 `로컬 자동검증`, `emulator instrumentation`, `GitHub Actions CI`, `실기기 감각 QA`로 분리하고 각각 실행·미실행·불필요·실행 불가를 기록해.
- instrumentation 전 `adb devices -l`로 검증 전용 `emulator-*`만 존재하는지 확인하고, 물리 기기나 불명확한 대상이 보이면 자동 실행하지 않아.
- draw·capture 계열은 필요 시 `adb shell dumpsys power`와 `mWakefulness`를 확인해 잠든 emulator의 draw 미발생을 production timeout과 구분해.
- 실패를 test infrastructure, emulator/OS environment, fixture, timing/race, DB/migration, assertion, production으로 분류해.
- 삭제·초기화 검증은 Fake, temporary directory, in-memory DB, 검증 전용 emulator를 우선하고 실사용 DB·엽서·사진·`filesDir`, `pm clear`, uninstall은 건드리지 않아.
- push 뒤에는 실제 CI workflow step과 결과를 확인하고 실패를 skip, `continue-on-error`, 실패 step 제거로 숨기지 않아.

## 3단계 변경·검증 상태

- 저장소: `AGENTS.md`, `docs/ai/HANDOFF.md`만 변경. 앱 production/test/dependency/CI YAML 변경 없음.
- canonical source: `SKILL.md`, `work-order-template.md`, `handoff-template.md`, `claude-code-execution-rules.md`, `codex-execution-rules.md`, `write-project-handoff/SKILL.md`, `write-project-handoff/references/handoff-template.md`, manifest version marker.
- 설치 cache: `0.1.0+codex.20260922080652`, installed/enabled 확인.
- source/cache: 전체 16개 파일 목록과 바이트 내용 일치.
- 공식 skill/plugin validator: 검사 스크립트 자체는 실행했지만 현재 bundled Python에 `yaml` 모듈이 없어 validation 본문 진입 전 `ModuleNotFoundError`로 실행 불가. 별도 dependency 설치로 범위를 넓히지 않았어.
- `git diff --check`와 최종 Git 상태는 3단계 완료보고에서 다시 확인해.

---

## 80일차 상세 기록 — GitHub Actions CI + emulator instrumentation 최초 실행 + Room migration 버그 수정

확인일: 2026-09-21. 수동 표준 모드. 이번 승인 범위는 최소 GitHub Actions CI 구축, 로컬 검증, 실제 GitHub 실행 확인, CI 관련 문서 갱신, 그리고 사용자가 직접 준비한 테스트 전용 emulator에서의 instrumentation 10건 최초 실제 실행과 그 과정에서 발견된 Room migration 버그의 최소 수정이었어. **80일차는 여기서 종료야. 다음 작업은 승인되지 않았어.**

## 현재 결과

- 신규: [`.github/workflows/android-ci.yml`](../../.github/workflows/android-ci.yml). push/PR 시 `testDebugUnitTest` → `assembleDebug` → `assembleDebugAndroidTest`를 자동 실행하고, **실제 GitHub Actions에서 3단계 모두 성공을 확인**했어.
- 79일차 결론("CI 없음")은 [CI-AUDIT-79.md](CI-AUDIT-79.md)에 그대로 보존했고, 80일차 CI 도입 + emulator instrumentation 실행 + migration 버그 수정 전 과정은 [CI-AUDIT-80.md](CI-AUDIT-80.md)에 기록했어.
- [TEST-COVERAGE-MAP.md](TEST-COVERAGE-MAP.md)는 "자동 실행 여부" 절, "Room database / migration" 절, "실제 Compose interaction" 절, 마지막 요약의 공백 목록을 80일차 실제 결과로 갱신했어. 나머지 기능 지도는 79일차 원문 그대로야.
- 79일차 HANDOFF 원문은 [archive/HANDOFF-through-2026-09-20.md](archive/HANDOFF-through-2026-09-20.md)에 보존했어(blob hash 대조로 원문 전체 보존 확인 완료, 79일차 HANDOFF 작성 시점에 남긴 기록).
- **production 변경 1건:** [`app/src/main/java/com/postcardmemory/data/PostcardDatabase.kt`](../../app/src/main/java/com/postcardmemory/data/PostcardDatabase.kt)의 기존 migration 3개(SQL 3줄) — Room migration 스키마 불일치 버그 수정. 아래 "Room migration 버그" 절 참고.

## 핵심 판단

### CI (GitHub Actions)

- 두 번의 실제 push로 검증했어. 1차(commit `e2a0fad`)는 SDK 설치 step이 존재하지 않는 패키지 id(`platforms;android-37`)를 원격에서 받으려다 실패 — Android SDK 분류, production과 무관.
- 실패 직후 GitHub 공식 [runner-images 소프트웨어 목록](https://github.com/actions/runner-images/releases/tag/ubuntu24%2F20260907.300)을 확인해, 이 CI가 쓰는 runner 이미지에 `android-37.0`/`build-tools 37.0.0`이 이미 내장돼 있음을 먼저 확인했어. 2차(commit `57748c7`)에서 불필요한 설치 명령을 제거하고 실측(`ls`) 확인으로 바꿔서 push했더니 실제로 확인됐고, 3단계 모두 성공했어.
- compileSdk 37은 오타나 잘못된 값이 아니라 실재하는 SDK야. 낮추지 않았어.
- CI 자체에는 여전히 instrumentation 실행이 없어. `assembleDebugAndroidTest` 성공은 "테스트 코드가 최신 소스 기준으로 컴파일된다"는 것만 증명해.

### emulator + instrumentation 최초 실행 (로컬, CI 아님)

- 사용자가 Android Studio Device Manager로 테스트 전용 AVD `PostcardMemory_Test`(API 37, `sdk_gphone16k_x86_64`)를 직접 부팅했어. 매 실행 직전 `adb devices -l`로 실사용 기기 없이 `emulator-5554` 하나만 연결된 것을 재확인한 뒤에만 진행했어.
- **1차 실행: 5/10 성공.** `PostcardBackMigrationTest`(1/1)·`PostcardBackSaveTest`(2/2)·`PostcardBackgroundColorSaveRaceTest`(2/2) 성공. `PostcardBackRenderingTest`(0/3)·`PostcardFullMigrationChainTest`(0/2) 실패.
- `PostcardBackRenderingTest` 3건 실패 원인: `NoSuchMethodException: android.hardware.input.InputManager.getInstance` — API 37이 최신 SDK라 현재 Espresso/androidx.test 버전과의 test infrastructure 호환 문제로 분류. **production 문제 아님, 오늘 손대지 않았어.**

### Room migration 버그 — 발견·원인·수정

- `PostcardFullMigrationChainTest` 2건이 정확히 같은 3개 컬럼(`message`, `futureMailDeliverAt`, `envelopeStyle`)에서 동일하게 실패해 재현 가능한 production 버그로 판단했어.
- `19.json` schema export와 `PostcardDatabase.kt`의 migration chain 전체를 대조해 원인을 확정: 세 컬럼 모두 entity가 기대하는 default와 실제로 그 컬럼을 만든 migration SQL의 DEFAULT 선언이 어긋나 있었어(`MIGRATION_2_3`의 테이블 재생성이 `message`의 `DEFAULT ''`를 빠뜨림, `MIGRATION_15_16`/`MIGRATION_16_17`이 nullable 컬럼에 불필요한 명시적 `DEFAULT NULL`을 씀). 각 컬럼을 다루는 migration은 하나뿐이라 원인이 명확히 좁혀졌어.
- **위험 판단:** migration은 버전 전환 시 한 번만 실행되고 이미 v19인 기기는 재실행하지 않아 — 이 수정은 v3/v16/v17 미만에서 올라오는 기기(새 설치 포함)의 미래 실행 경로만 바꿔. 버전 증가·새 migration·데이터 삭제 없이 기존 migration SQL 3줄만 최소 수정 → STOP 대상 아님, 진행.
- **수정:** `message TEXT NOT NULL,` → `... DEFAULT '',` / `futureMailDeliverAt INTEGER DEFAULT NULL` → `... INTEGER`(DEFAULT 절 제거) / `envelopeStyle TEXT DEFAULT NULL` → `... TEXT`(DEFAULT 절 제거).
- **재검증:** `PostcardFullMigrationChainTest` 2건만 재실행 → 2/2 통과. instrumentation 전체 10건 재실행 → **7/10 성공**(Room migration 2건 포함 성공, Rendering 3건은 동일하게 실패 — 예상된 결과).
- **별개로 남겨둔 의문(오늘 조사 범위 밖):** 이 검증은 Room이 실제 앱에서 DB를 열 때와 같은 경로로 일어나. 이미 v19인 기존 실사용 기기도 과거 이 스키마 상태를 지나며 한 번은 이 검증을 통과했어야 해. 지금까지 조용했다면 문제 없었다는 뜻일 수도, 아직 안 드러난 것일 수도 있어 — 오늘은 더 파지 않았어.

## 검증·사용자 환경

- 구현: CI workflow 2회 커밋(`e2a0fad`, `57748c7`) + production migration 수정 1건(미커밋, 아래 Git 스냅샷 참고).
- 로컬 자동 검증(수정 전): `testDebugUnitTest`/`assembleDebug`/`assembleDebugAndroidTest` 전부 성공(JVM 750개, 0 failure).
- **로컬 회귀검증(migration 수정 후):** `testDebugUnitTest` 성공(JVM 750개 그대로 0 failure, 0 error — 회귀 없음), `assembleDebug` 성공, `assembleDebugAndroidTest` 성공, `git diff --check` 통과.
- GitHub Actions 실제 실행: 확인 완료. 1차 run [35562870949](https://github.com/KimJinha-JJin/post-card-memory/actions/runs/35562870949) 실패(SDK), 2차 run [35563162247](https://github.com/KimJinha-JJin/post-card-memory/actions/runs/35563162247) 성공.
- emulator instrumentation 실제 실행: 확인 완료(위 "핵심 판단" 참고). 실사용 기기는 전 과정에서 한 번도 사용하지 않았어 — 매 실행 직전 `adb devices -l` 재확인.
- 사용자 QA: CI workflow 변경은 실기기 불필요. **migration 수정은 Room database 변경이라 사용자 확인이 필요한 영역이지만, 오늘 이미 emulator instrumentation으로 실제 마이그레이션 경로(v1→v19 전체 체인 포함)를 자동 실행해 통과를 확인했어.** 추가로 실사용 기기에서의 실제 앱 업데이트 체감 확인은 사용자가 원하면 별도로 할 수 있어(강제하지 않음).

## Git 스냅샷

- 브랜치: feature/photo-sticker
- 이번 세션 시작 HEAD: `82471674eb8c2f606c09f26036db7a10b0fb9b16`
- 이 HANDOFF 작성 시점 HEAD: `5e61041ecfa59c06c3de6012def1a554c3fef755` (CI workflow 2건 + 이전 문서 갱신 커밋까지 push 완료, origin과 동기화 0/0)
- **이번 세션에서 새로 생긴 미커밋 변경(아직 stage 안 함):** `app/src/main/java/com/postcardmemory/data/PostcardDatabase.kt`(migration 3줄 수정), `docs/ai/CI-AUDIT-80.md`/`docs/ai/TEST-COVERAGE-MAP.md`/`docs/ai/HANDOFF.md`(이번 결과 반영).
- 시작 시 기존 untracked: `.codex-config.candidate.toml`, `.kotlin/` — 계속 보존, 수정·stage·commit 없음.
- commit: CI workflow 2건 완료(`e2a0fad`, `57748c7`), 이전 CI 문서 갱신 1건 완료(`5e61041`). **이번 migration 수정 + 최신 문서 갱신은 미승인·미실행.** push: 위와 동일 — CI workflow·이전 문서는 완료, 이번 변경은 미승인·미실행.

## 후속

- Node.js/setup-java deprecation 경고 정리(`actions/setup-java@v5`).
- API 37 / Espresso `InputManager.getInstance()` 호환 문제 — androidx.test/espresso 버전 업 또는 API 37 emulator 자체의 알려진 이슈인지 확인 필요. Rendering 3건이 여기 걸려 있어.
- 이미 v19인 기존 실사용 기기가 과거에 이 migration 검증을 이미 통과했는지, 조용히 통과한 이유가 무엇인지는 오늘 조사하지 않았어 — 필요하면 별도로 확인.
- **postcard-memory workflow 후속 보강 필요** (workflow 파일 자체는 오늘 수정하지 않음, 기록만 남김):
  - push 후 CI 결과 확인 절차
  - instrumentation은 emulator 전용이라는 원칙
  - 실행 전 `adb devices` 실측
  - 물리 기기 존재 시 STOP
  - 실패를 CI 환경 / test infrastructure / production으로 분류하는 절차
  - API 37 Espresso 호환 문제 후속

## 다음 행동

migration 수정 + 문서 갱신 diff를 검토한 뒤 사용자 확인을 받아 commit/push할지 판단해. 그 전까지는 미커밋 상태를 유지해.
