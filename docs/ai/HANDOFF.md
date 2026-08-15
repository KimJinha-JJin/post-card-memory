# HANDOFF

## 2026-08-15 — AGENTS.md + docs/ai 구조 도입

**변경 파일**

- `AGENTS.md` (신규)
- `CLAUDE.md` (재작성 — 안전 규칙은 `AGENTS.md`로 이동, Claude Code 전용 규칙만 남김)
- `docs/ai/CURRENT_TASK.md` (신규)
- `docs/ai/WORK_CONTEXT.md` (신규)
- `docs/ai/STATUS.md` (신규)
- `docs/ai/HANDOFF.md` (신규, 이 문서)
- `docs/ai/DECISIONS.md` (신규)

**검증**

- 문서 변경만 있고 앱 코드/Room/Gradle 변경 없음 → 빌드·테스트 실행 대상 없음.
- 기존 `CLAUDE.md`의 안전 규칙(데이터 안전, Git 안전, 테스트/검증, 진행-승인 경계, Android Studio/CLI 잠금, commit/push 승인)이 `AGENTS.md`와 `CLAUDE.md` 두 문서에 모두 남아 있는지 항목별로 대조 확인함 — 누락 없음.

**Git 상태**

- 신규 파일 7개(`AGENTS.md`, `docs/ai/*.md` 5개, 그리고 재작성된 `CLAUDE.md`는 기존 추적 파일).
- staged/commit 없음 — 사용자 명시 요청 전까지 보류.

**남은 위험 / 미확인**

- ~~공용 작업판 모드를 통한 실제 작업 사이클은 아직 실행해보지 않음~~ → 바로 아래 "엽서 배경 구조 조사" 항목이 이 구조의 첫 시험 운전이었고, `CURRENT_TASK.md` 등록 → 조사 → `WORK_CONTEXT.md`/`STATUS.md`/`HANDOFF.md` 갱신까지 실제로 잘 동작함을 확인함 — 해소됨.

## 2026-08-15 — 엽서 배경 구조 조사 (공용 작업판 모드 시험 운전)

**목표**: `CURRENT_TASK.md`에 등록한 대로 엽서 배경 관련 구조 조사(코드 수정 없음).

**조사 결과**

- 배경 데이터: `Postcard.kt`(Room)의 `backgroundColorArgb`(Long), `backgroundPattern`(String), `backgroundPatternDensity`(Float), `backgroundImagePath`(String?, nullable) 4개 컬럼.
- 배경 렌더링: `PostcardRenderSpec.drawBackground()`/`drawBackgroundPattern()` 한 곳에서만 단색 채우기 + 8종 패턴(CHECKER/DOTS/STRIPES/WAVES/GRID/CROSSHATCH/SPECKLE/HEISEI) + 안쪽 흰 테두리를 그림.
- 호출 경로(4곳, 모두 `drawBaseContent()` 경유, 모두 같은 값 전달): 화면 미리보기(`DetailScreen.kt`), 저장용 미리보기 비트맵(`DetailViewModel.kt`), 최종 저장/공유 이미지(`PostcardImageExporter.kt`), 템플릿 목록 썸네일(`PostcardTemplateRow.kt`) — 화면과 exporter가 갈라질 여지 없음(AGENTS.md 13장 불변값과 일치).
- 배경 색·패턴 선택 UI 정의: `PostcardBackgroundPicker.kt`(팔레트 12색, `PostcardBackgroundPattern` enum).
- 특이사항: `backgroundImagePath`는 컬럼·삭제 방어(`PostcardDeletionManager`, `OrphanFileDiagnostics`)·저장 경합 테스트(`BackgroundColorSaveRaceTest`)까지 갖춰져 있지만, `drawBaseContent()`가 이 값을 파라미터로 받지 않아 실제로 배경에 이미지를 그리는 코드는 없음. 테스트 주석에도 "현재 앱에서 backgroundImagePath를 non-null로 만드는 UI 경로는 없다"고 명시됨 — 미사용 필드지만 삭제 방어 로직이 이미 갖춰져 있으므로 그대로 둠(수정 안 함).

**변경 파일**: 없음 (`docs/ai/CURRENT_TASK.md`, `WORK_CONTEXT.md`, `STATUS.md`, `HANDOFF.md`만 갱신, 앱 코드·Room·Gradle 미변경).

**검증**: 코드 읽기와 grep 기반 조사만 수행, 빌드/테스트 실행 대상 없음.

**Git 상태**: `git status --short` 기준 — `CLAUDE.md`(M, 이전 작업분), `AGENTS.md`·`docs/ai/`(??, 이전 작업분), `.kotlin/`(??, 무관). 이번 조사로 새로 변경된 추적 파일은 `docs/ai/*.md` 갱신뿐이며 앱 코드 변경 없음. staged/commit/push 없음.

**남은 위험 / 미확인**: `backgroundImagePath`를 실제 기능으로 쓸지, 죽은 컬럼으로 정리할지는 이번 조사 범위 밖 — 사용자 판단 필요 시 별도 작업으로 진행.
