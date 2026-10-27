# HANDOFF — 73일차 현재 상태

확인일: 2026-09-14. 수동 표준 모드, Codex / 패코의 새 기능 작업이야. 이번 세션의 73일차 작업지시서가 승인 근거이며 공용 작업판은 활성화하지 않았어.

## 현재 결과와 재개 조건

목표는 실제 방문 날짜만 현재 월 달력에 표시하고, 날짜별 고정 손그린 X 5종과 월별 고정 작은 카오모지를 두는 거야.

**방문 persistence 조사 완료 / 날짜별 history 없음 / 앱 구현 미착수 / 사용자 정책 결정 대기**야. 작업지시서 8·9·40절에 따라 달력과 새 persistence 관련 mutation을 멈췄어. 앱 코드와 사용자 데이터는 변경하지 않았어. 다음 후보는 승인된 실행이 아니야.

| 구분 | 현재 상태 |
|---|---|
| 조사 | 저장 모델·직렬화·파일 쓰기·호출부·관련 테스트 소스 확인 완료 |
| 구현 | 미착수 — history 부재로 달력·X·카오모지·selector 구현 중단 |
| 자동 검증 | 문서 diff·로컬 링크·보존 원문 대조 확인. unit test와 compile은 미실행 — 앱/테스트 변경 없음 |
| 사용자 QA | 이번 조사·문서-only로 실기기 불필요. 달력 시각 QA는 구현 전이라 미착수 |
| commit | 미승인·미실행 |
| push | 미승인·미실행 |

## 실제 저장 구조와 근거

- [VisitRecord.kt](../../app/src/main/java/com/postcardmemory/utils/VisitRecord.kt): `lastVisitEpochDay: Long`, `totalVisitDays: Int`, `currentStreakDays: Int` 세 값이야. 날짜는 로컬 `LocalDate.toEpochDay()` 기준이야.
- 저장 형식은 버전 1의 `1<TAB>lastVisitEpochDay<TAB>totalVisitDays<TAB>currentStreakDays` 한 줄이야. 지시서의 lastVisitDate/currentStreak에 대응하는 실제 필드명은 위와 같아.
- [VisitRecordStorage.kt](../../app/src/main/java/com/postcardmemory/utils/VisitRecordStorage.kt): `filesDir/visits/visit_record.txt` 한 파일을 읽고 갱신해. 새 날짜마다 이전 상태를 교체하며 날짜별 목록이나 로그를 누적하지 않아. Room·DataStore에 별도 방문 history를 쓰는 경로도 소스 검색에서 발견하지 못했어.
- 하루 첫 방문은 `recordVisit(previous, today)`의 결과가 previous와 다른지로 판정해. 같은 날짜면 동일 record를 반환하고 파일 쓰기와 첫 방문 표시를 생략해. 다음 날짜면 total +1, 바로 다음 날이면 streak +1, 날짜 간격이 있으면 streak 1이야.
- 시계가 과거로 움직이면 마지막 날짜만 바꾸고 total/streak은 유지해. 그래서 streak에서 과거 방문일을 역산하는 것도 신뢰할 수 없어.
- [MainActivity.kt](../../app/src/main/java/com/postcardmemory/MainActivity.kt): 프로세스 시작에 IO에서 한 번 판정하고 `AppIntroState.todayVisit`에 보관해. Activity 재생성·recomposition에서 다시 기록하지 않아. 자정 이후 살아 있는 프로세스의 기록 정책 확대는 이번에 하지 않았어.
- 저장은 기존 임시 파일/교체 helper를 사용해. `save`의 Boolean 실패 결과를 호출부가 반영하지 않고 이번 실행의 소인은 계속 보여주는 기존 동작이 있어. 이를 수정하지 않았으며 새 history 연결 시 부분 저장 실패와 재시도 설계에서 고려해야 해.
- 관련 `VisitRecordTest`, `VisitRecordStorageTest`의 소스를 읽었어. 실행 결과를 확인한 것은 아니야.
- 실제 기기 파일의 숫자나 날짜는 조회하지 않았어. 위 내용은 실제 저장 **구조**이며 사용자 기기의 현재 저장 **값**을 뜻하지 않아.

총 방문 수는 빠진 날짜를 알려주지 않고 마지막 날짜는 과거 전체 목록이 아니야. 엽서 촬영일도 앱 방문일의 증거가 아니므로 대체하지 않아. 현재 월의 정확한 방문 날짜 목록은 현재 persistence로 얻을 수 없어.

## 사용자 정책 선택지와 최소 확장 후보

### A — 적용 이후 실제 방문 날짜만 새로 기록

- 기존 `visit_record.txt`의 버전·내용·total/streak 계산 의미는 유지해. 과거 날짜를 소급 생성하거나 기존 마지막 날짜를 자동 이관하지 않아.
- 최소 후보는 같은 visits 영역에 날짜당 작은 marker 파일을 하나씩 두는 방식이야(예: `visits/history/<epochDay>.txt`). 이미 쓰는 파일 저장 방식으로 구현 가능성을 검토할 수 있고 JSON/list 직렬화, 새 DB table, Room migration, DataStore, dependency 추가가 필요하지 않은 방향이야. 아직 구현·검증·승인된 설계는 아니야.
- 적용된 버전을 실제 실행한 날부터 기록해. 같은 날 이미 total이 증가했어도 그날 history가 없다면 실제 실행 날짜만 보완하며 total은 다시 올리지 않는 방향이야. 이후에도 기존 방문 판정 호출 시점에 관측한 날짜를 기록해.
- 달력에는 적용 이후 성공적으로 저장된 날짜만 X가 생겨. 과거 빈칸은 미방문 확정이 아니라 기록 부재야. 기존 total과 달력의 X 개수가 달라도 정상인 정책이야.
- 요약과 history 두 파일의 저장은 하나의 트랜잭션이 아니야. 한쪽만 실패할 때 기존 요약을 되돌리거나 초기화하지 않고, 중복·경합·취소·실패 후 재시도와 실제 저장 성공 여부를 검증해야 해. 실패한 과거 날짜를 나중에 추정해 채우지 않아.
- 기존 사용자 데이터 migration은 하지 않는 후보야. 신규 날짜 저장 자체는 현재 승인 밖이므로 A 정책과 이 최소 확장 범위에 대한 승인 후에만 수정해.

### B — 달력 보류, history 설계를 별도 작업으로 분리

- 현재 앱과 방문 저장 구조를 그대로 유지해. 신규 history도 아직 쌓이지 않아.
- 별도 작업에서 저장 정책과 실패 처리 범위를 확정한 뒤 달력 작업을 재개해.

둘 중 임의 선택하지 않았어. 현재 멈춘 대상은 앱 UI와 persistence 수정이고, 독립적인 읽기 전용 조사는 가능해. 달력 위치·typography·X 좌표·카오모지 glyph 확인은 데이터 선행 조건이 충족되지 않아 진행하지 않았어. UI 문법 재사용·variant·예외는 해당 없음, 신규 UI 변경도 없어.

## Git과 변경 파일

시작·종료 확인 기준:

- branch: `feature/photo-sticker`
- HEAD: `94c4ea428d2e15b472ec97a14aefa3994278d55b`
- 로컬 `origin/feature/photo-sticker` 추적 참조와 ahead/behind `0/0`. 이번에 fetch하지 않았으므로 서버 최신 상태를 새로 확인한 것은 아니야.
- 시작 tracked/staged diff 없음. 종료 tracked 변경은 이 HANDOFF 하나, 신규 문서는 아래 archive 하나야. staged 변경 없음.
- 기존 untracked `.claude/`, `.codex-config.candidate.toml`, `.kotlin/`를 보존했어.
- 최근 커밋: `94c4ea4` Git checkpoint 설명, `1f1ddc2` 공용 하네스 정리, `c5959d8` milestone 5종과 33번째 회전.

변경 파일:

1. `docs/ai/HANDOFF.md`: 73일차 조사 결과·STOP·정책 후보·검증 상태로 최신화.
2. [2026-09-13 인수인계 원문](archive/HANDOFF-2026-09-13-harness.md): 직전 기록을 수정 없이 보존. 이 파일의 상대 링크는 당시 HANDOFF 위치 기준이므로 역사 원문으로 읽어.

검증: 전체 tracked diff와 staged diff 확인, `git diff --check`, 새 HANDOFF 상대 링크 존재와 archive 원문 동일성 검사. 앱 compile·관련/전체 unit test·계측 테스트는 미실행이야. 625 tests baseline은 과거 보고이며 이번 통과 결과가 아니야. `rg`가 없어 `git grep`과 PowerShell로 조사했어. Git 전역 ignore 파일 접근 권한 경고가 있었지만 status/diff 명령은 완료됐어. 전용 단계 추적 도구가 노출되지 않아 진행 메시지와 이 문서로 단계를 기록했고 Goal 도구로 대체하지 않았어. 위임 agent는 사용하지 않았고 핵심 production 수정 시도는 0회야.

## 기존 미검증과 데이터 보호

- 33번째 방문 소인 회전·착지와 나머지 milestone 문구 실기기 표현은 직전 HANDOFF의 미검증을 유지해. 자연 도달할 때 확인하며 방문 데이터나 기기 날짜를 조작하지 않아.
- 70일차의 다음 날 첫 실행·숫자·햅틱·Gallery 전환은 과거 사용자 확인 기록이 있고, 이번에 재검증한 것은 아니야.
- 2026-09-07 계측 테스트 후 앱 제거 사고의 원문과 후속 기록은 [2026-09-12까지의 이력](archive/HANDOFF-through-2026-09-12.md)에 보존돼 있어. 사고 후 엽서 1개 보존 기록을 사고 전 전체 복구로 해석하지 않아.
- 이번에 기기·DB·설치·백업 상태를 조회하거나 변경하지 않았어. 앱·Intro·Gallery·저장 로직 변경이 없으며 실기기 현재 데이터 상태를 새로 보장하는 보고는 아니야.
- 기존 설정 감사 후보와 하네스 완료 이력은 직전 인수인계 원문에 남겼어. 현재 작업 범위로 확대하지 않아.

다음 행동 하나: 사용자가 A 또는 B 정책을 결정하면 그 승인 범위를 기준으로 이어가. A라도 달력 위치가 불명확하면 후보와 근거를 보고하고, commit/push는 각각 별도 승인을 받아야 해.
