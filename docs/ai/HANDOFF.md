# HANDOFF — 80일차 마감: GitHub Actions CI + emulator instrumentation 최초 실행 + Room migration 버그 수정

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
