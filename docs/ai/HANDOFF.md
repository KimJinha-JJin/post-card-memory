# HANDOFF — 80일차: GitHub Actions CI 도입 완료

확인일: 2026-09-21. 수동 표준 모드. 이번 승인 범위는 최소 GitHub Actions CI 구축, 로컬 검증, 실제 GitHub 실행 확인, CI 관련 문서 갱신이었어. **emulator/instrumentation 실제 실행은 이 HANDOFF 이후 별도로 조사·판단 중이야.**

## 현재 결과

- 신규: [`.github/workflows/android-ci.yml`](../../.github/workflows/android-ci.yml). push/PR 시 `testDebugUnitTest` → `assembleDebug` → `assembleDebugAndroidTest`를 자동 실행해.
- 79일차 결론("CI 없음")은 [CI-AUDIT-79.md](CI-AUDIT-79.md)에 그대로 보존했고, 80일차 도입 과정과 실제 실행 로그는 [CI-AUDIT-80.md](CI-AUDIT-80.md)에 새로 기록했어.
- [TEST-COVERAGE-MAP.md](TEST-COVERAGE-MAP.md)의 "자동 실행 여부" 절만 80일차 상태로 갱신했어. 나머지 16개 기능 지도는 79일차 원문 그대로야.
- 79일차 HANDOFF 원문은 [archive/HANDOFF-through-2026-09-20.md](archive/HANDOFF-through-2026-09-20.md)에 보존했어. Git blob hash가 이번 HEAD의 이전 HANDOFF.md와 동일한 `c9d412aa195038537d2d160519d73af6f5cd6dd1`임을 확인해 원문 전체 보존을 검증했어.

## 핵심 판단

- CI 자체는 두 번의 실제 push로 검증했어. 1차(commit `e2a0fad`)는 SDK 설치 step이 존재하지 않는 패키지 id(`platforms;android-37`)를 원격에서 받으려다 실패했어 — Android SDK 분류, production과 무관.
- 실패 직후 GitHub 공식 [runner-images 소프트웨어 목록](https://github.com/actions/runner-images/releases/tag/ubuntu24%2F20260907.300)을 확인해, 이 CI가 쓰는 runner 이미지에 `android-37.0`/`build-tools 37.0.0`이 이미 내장돼 있음을 먼저 확인했어. 그 뒤 2차(commit `57748c7`)에서 불필요한 설치 명령을 제거하고 실측(`ls`) 확인으로 바꿔서 push했더니 runner 로그에서도 실제로 확인됐고, 3단계 모두 성공했어.
- compileSdk 37은 오타나 잘못된 값이 아니라 실재하는 SDK야. 낮추지 않았어.
- CI는 여전히 instrumentation 10건의 실제 emulator 실행은 하지 않아. `assembleDebugAndroidTest` 성공은 "테스트 코드가 최신 소스 기준으로 컴파일된다"는 것만 증명해.

## 검증·사용자 환경

- 구현: `.github/workflows/android-ci.yml` 신규 작성 2회 커밋(SDK step 최소 수정 1회 포함). production 코드, compileSdk, targetSdk, Gradle wrapper는 변경하지 않았어.
- 로컬 자동 검증(작성 직후, push 전): `testDebugUnitTest` 성공(JVM `@Test` 750개, failures 0, errors 0), `assembleDebug` 성공, `assembleDebugAndroidTest` 성공. 두 번째 workflow 수정은 CI YAML만 바뀐 것이라 로컬 재실행은 생략했어.
- **GitHub Actions 실제 실행: 확인 완료.** 1차 run [35562870949](https://github.com/KimJinha-JJin/post-card-memory/actions/runs/35562870949) 실패(SDK), 2차 run [35563162247](https://github.com/KimJinha-JJin/post-card-memory/actions/runs/35563162247) 성공(3단계 전부 `BUILD SUCCESSFUL`).
- 사용자 QA: 이번 CI-only 변경은 실기기 불필요 — 사용자 앱 설치·데이터에 영향 없음.
- 기기 접근·설치·삭제·데이터 변경·SDK/emulator 구축은 이번 범위에서 하지 않았어(문서 갱신 이후 별도 조사 예정).

## Git 스냅샷

- 브랜치: feature/photo-sticker
- 이번 세션 시작 HEAD: `82471674eb8c2f606c09f26036db7a10b0fb9b16` (79일차 종료 지점과 동일, 예상값과 실측 일치)
- 이 HANDOFF 작성 시점 HEAD: `57748c7770b1b849b7365dce8a6705dda8780340` (commit `e2a0fad` → `57748c7`, 둘 다 push 완료·origin과 동기화)
- local/origin ahead-behind: 0/0
- 시작 시 기존 untracked: `.codex-config.candidate.toml`, `.kotlin/` — 이번 세션에서 그대로 보존, 수정·stage·commit 없음.
- 이번 문서 갱신(`CI-AUDIT-80.md` 신규, `CI-AUDIT-79.md`/`TEST-COVERAGE-MAP.md`/`HANDOFF.md` 수정, 이전 HANDOFF archive 이동)은 아직 stage·commit하지 않았어.
- commit: workflow 2건은 완료(`e2a0fad`, `57748c7`), 이번 문서 갱신은 미승인·미실행. push: workflow 2건은 완료, 문서는 미승인·미실행.

## 다음 행동

문서 갱신 diff 검토 후 사용자 확인을 받아 문서만 별도 commit/push할지 판단해. 그 뒤 emulator 환경 실측(Android SDK 위치, emulator binary, sdkmanager/avdmanager, 설치된 system image, 기존 AVD, Android Studio Device Manager로 테스트 전용 AVD 생성 가능 여부)을 진행해. 안전하게 준비되면 instrumentation 10건(`connectedDebugAndroidTest`) 실행까지 이어가고, 대공사가 필요하면 여기서 종료해도 80일차는 이미 성공이야. 실사용 기기는 절대 사용하지 않아.
