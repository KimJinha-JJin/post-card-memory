# DECISIONS

## 2026-08-15 — AGENTS.md + docs/ai 공용 작업판 구조 도입

- 결정: 공통 안전 규칙(Room/Git/테스트/저장 안전성/commit-push 승인)은 `AGENTS.md`에 두고, Claude Code 전용 실행 규칙(Task 운영, 보고 포맷, `/compact`·`/clear`)은 `CLAUDE.md`에 둔다.
- 이유: ChatGPT/Codex와 Claude Code가 같은 안전 규칙을 서로 다른 문서에 중복 유지하다가 어긋나는 것을 막기 위함.
- 영향: 앞으로 안전 규칙을 바꿀 때는 `AGENTS.md` 한 곳만 수정하면 되고, `CLAUDE.md`는 Claude Code 도구 사용법이 바뀔 때만 수정한다. 프로젝트 불변값(3열 그리드, 날짜 형식 등)은 `AGENTS.md` 13장에서 관리한다.
