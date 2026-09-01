# DECISIONS

## 2026-08-15 — AGENTS.md + docs/ai 공용 작업판 구조 도입

- 결정: 공통 안전 규칙(Room/Git/테스트/저장 안전성/commit-push 승인)은 `AGENTS.md`에 두고, Claude Code 전용 실행 규칙(Task 운영, 보고 포맷, `/compact`·`/clear`)은 `CLAUDE.md`에 둔다.
- 이유: ChatGPT/Codex와 Claude Code가 같은 안전 규칙을 서로 다른 문서에 중복 유지하다가 어긋나는 것을 막기 위함.
- 영향: 앞으로 안전 규칙을 바꿀 때는 `AGENTS.md` 한 곳만 수정하면 되고, `CLAUDE.md`는 Claude Code 도구 사용법이 바뀔 때만 수정한다. 프로젝트 불변값(3열 그리드, 날짜 형식 등)은 `AGENTS.md` 13장에서 관리한다.

## 2026-08-31 — 엽서 export는 항상 앞면을 출력한다

- 결정: 저장/공유/파일 내보내기 exporter는 현재 화면이 뒷면이어도 항상 엽서 앞면을 출력한다. 뒷면 export, 현재 보고 있는 면 export, 앞/뒤 선택 UI는 추가하지 않는다.
- 이유: 58일차 조사(`docs/ai/HANDOFF.md` 2026-08-28 "export 앞/뒷면" 항목)에서 화면 면과 export 결과가 다를 수 있다는 사실만 코드로 확인됐고, 어느 쪽이 맞는 제품 의미인지는 사용자 판단이 필요해 STOP으로 남겨뒀다. 이번 59일차 후속 작업지시서에서 사용자가 "항상 앞면"으로 확정했다.
- 영향: 현재 exporter 동작(`PostcardImageExporter`가 항상 앞면 렌더)은 버그가 아니라 확정된 정책이므로 앞으로 이 동작을 근거 없이 수정 대상으로 보고하지 않는다. 뒷면을 보고 export했을 때 앞면이 나오는 것에 대한 사용자 혼동을 줄이려면 별도 문구/UX 결정이 필요할 수 있으나 이는 이번 결정의 범위 밖이다.
