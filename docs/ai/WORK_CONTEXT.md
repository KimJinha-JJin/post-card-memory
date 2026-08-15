# WORK_CONTEXT

## 2026-08-15 — AGENTS.md + docs/ai 구조 도입

- 결정된 방향: 기존 실제 저장소 `CLAUDE.md`(Git/Room/저장 안전성/테스트/commit-push 승인 등 안전 규칙)를 삭제·약화하지 않고, 공통 규칙은 `AGENTS.md`로, Claude Code 전용 실행·세션 규칙은 `CLAUDE.md`로 분리한다.
- 새로 확인한 사실: 저장소 루트에는 원래 `AGENTS.md`와 `docs/ai/`가 없었고, 별도 검토용 복제본(`~/.codex/.chatgpt-projects/.../post-card-memory-workflow`)에만 초안이 존재했다. 이번 작업으로 그 초안을 실제 저장소 규칙과 병합해 도입했다.
- 내린 결정: 기존 "작업지시서" 개념은 `CURRENT_TASK.md`(공용 모드) 또는 세션 제공 작업지시서(수동 모드)로, "인수인계서" 개념은 `HANDOFF.md` + `STATUS.md` 조합으로 대응시킨다. 어느 한쪽 문서가 없어도 작업이 막히지 않도록 수동 비상 모드를 항상 유효한 대안으로 유지한다.
- 보류한 부분: 공용 작업판 모드를 통한 정식 작업 사이클은 아직 시작하지 않음. 다음 기능 작업부터 `CURRENT_TASK.md`에 목표를 채워 넣을지, 계속 수동 방식으로 진행할지는 사용자 선택.
- 사용자 또는 ChatGPT 판단 필요 항목: 없음 (이번 구조 도입 자체는 사용자가 승인함).

## 2026-08-15 — 엽서 배경 구조 조사 (공용 작업판 모드 시험 운전)

- 새로 확인한 사실: 배경 렌더링(단색 채우기 + 8종 패턴 + 안쪽 흰 테두리)은 `PostcardRenderSpec.drawBackground()`/`drawBackgroundPattern()` 하나로만 이뤄지고, 이 함수는 `drawBaseContent()`를 통해 화면 미리보기(`DetailScreen.kt`), 저장/공유 exporter(`PostcardImageExporter.kt`), 저장 시 미리보기 비트맵 생성(`DetailViewModel.kt`), 템플릿 목록 썸네일(`PostcardTemplateRow.kt`) 네 곳에서 동일하게 호출됨. 네 호출부 모두 `backgroundColorArgb`/`backgroundPattern`/`backgroundPatternDensity`를 같은 출처(Postcard 엔티티 또는 템플릿 스타일)에서 그대로 넘김 — 화면·저장 경로가 갈라져 있지 않음.
- 배경 데이터 정의 위치: `Postcard.kt`(Room Entity)에 `backgroundColorArgb: Long`, `backgroundPattern: String`, `backgroundPatternDensity: Float`, `backgroundImagePath: String?` 4개 컬럼. 색상 팔레트/패턴 enum 자체(라벨, 기호)는 `PostcardBackgroundPicker.kt`에 정의되어 있고, 값은 문자열(`"NONE"`, `"DOTS"` 등)로 저장되어 `PostcardRenderSpec`의 `when` 분기와 이름으로 매칭됨.
- 기존 예상과 달랐던 점: `backgroundImagePath` 컬럼이 존재하고 삭제 방어(`PostcardDeletionManager`, `OrphanFileDiagnostics`)와 저장 경합 테스트(`BackgroundColorSaveRaceTest`)까지 갖춰져 있지만, 실제 렌더링 함수(`drawBaseContent`)는 이 값을 파라미터로 받지 않는다 — 즉 "이미지 배경"을 그리는 코드는 없음. `BackgroundColorSaveRaceTest.kt:38` 주석에도 "현재 앱에서 backgroundImagePath를 non-null로 만드는 UI 경로는 없다"고 명시되어 있어, 미래 확장을 위해 미리 마련해둔 컬럼으로 보임(과거 데이터 호환성 필드 사례).
- 결정한 것 없음 — 조사만 수행, 코드 변경 없음.
- 보류한 부분: `backgroundImagePath`를 실제로 쓸지, 죽은 컬럼으로 정리할지는 이번 조사 범위 밖이라 다루지 않음.
- 사용자 판단 필요 항목: 없음(순수 조사 결과 보고).
