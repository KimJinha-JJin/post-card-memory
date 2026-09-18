# HANDOFF — 77일차: 방문 달력 폴리싱 + Intro 막스 33일차 조건 복구

확인일: 2026-09-18. 수동 표준 모드. 사용자 제공 77일차 지시서에 따라 피코(Claude Code)가 조사→구현→자동 검증→실기기 QA 순으로 진행했어. 지시서 범위(달력 현재 위치 표시·swipe 손맛, Intro 막스 문구 버그)를 끝낸 뒤, 같은 세션에서 사용자가 실기기로 확인하다가 작은 후속 요청("다른 월/연도 탐색 중엔 오늘 marker가 안 보이니 상단에 복귀 링크를 달아달라")을 추가로 반영했어. **실기기 QA 전부 통과. commit·push는 아직 요청받지 않아 미실행.**

## 시작 상태

branch `feature/photo-sticker`, 시작 HEAD `7e7b77f`(local==origin, tracked clean, 기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 존재 — 76일차 종료 참고값과 정확히 일치). 76일차(갤러리 축소+기억밀도 새싹형)는 이번 작업에서 전혀 건드리지 않았어.

## 방문 달력 — 현재 월/연도 marker

- 75일차에 이미 있던 `displayedMonth`(선택/표시 중인 월) 강조와 완전히 분리된 "실제 오늘" 판정을 추가: `isCurrentMonthCell(year, month, today)`/`isCurrentYearCell(year, today)` 순수 함수.
- 표시: `VisitCalendarCurrentPeriodMarker`(26dp 원, `InkSecondary.copy(alpha=0.16f)`) — 새 색 안 만들고 기존 색을 한 번 더 낮춰 재사용. 성취처럼 안 보이게 아주 연하게.
- 겹침 방지: `visitCalendarShowsCurrentMarker(isCurrentPeriod, isSelected)` — 선택된 칸과 같으면 marker를 그리지 않음(강조 이중 적용 금지 지시 반영).
- `VisitCalendarMonthPicker`/`VisitCalendarYearPicker`가 `today: YearMonth` 파라미터를 새로 받음, `MonthlyVisitCalendar`에서 `todayYearMonth`로 계산해 전달.

## 방문 달력 — 후속: 오늘이 화면 밖일 때 헤더 복귀 링크 (사용자 실기기 QA 중 추가 요청)

- 문제: 위 marker는 오늘이 지금 보이는 4×4 창 안에 있을 때만 보임. 다른 연도로 탐색해 오늘이 창 밖으로 나가면 "오늘이 어딘지" 알 방법이 없었음 — 사용자가 실기기로 직접 확인하다 발견.
- 사용자에게 "안내 문구만" vs "탭하면 오늘로 복귀" 두 방식을 물어봤고, 후자로 확정.
- 순수 판정 함수 추가: `isCurrentMonthVisibleInMonthPicker(pickerYear, today)`, `isCurrentYearVisibleInYearPicker(decadeStart, today)` — 지금 보이는 4×4 grid 범위 안에 오늘이 있는지.
- 창 밖일 때만 헤더(`VisitCalendarPickerHeaderRow`) 바로 아래에 "오늘 2026년 9월 →" / "오늘 2026년 →" 같은 조용한 텍스트 링크를 추가로 보여줌. 탭하면 `pickerYear = todayYearMonth.year`로 이동 — **보기 창만 오늘이 포함되게 재배치**할 뿐, `navLevel`이나 실제 선택(`displayedMonth`)은 바꾸지 않음(CALENDAR 레벨의 "오늘" 버튼과 다른 동작이라 헷갈리지 않게 의도적으로 구분).
- 오늘이 이미 창 안에 보이면(예: 그냥 열었을 때 흔한 케이스) 이 링크는 안 뜨고 기존 marker만으로 충분 — 중복 표시 없음.

## 방문 달력 — MONTH_PICKER/YEAR_PICKER 수직 swipe

- 새 gesture 엔진 없이 표준 `detectVerticalDragGestures` + `android.view.ViewConfiguration.get(context).scaledPagingTouchSlop`(Android가 "페이지 넘기기"에 표준으로 쓰는 threshold, 임의 px 값 아님) 사용.
- 방향 판정은 순수 함수 `visitCalendarSwipeStepFor(accumulatedDrag, thresholdPx)`로 분리(위=NEXT, 아래=PREVIOUS, 미만은 NONE) — 단위 테스트로 직접 검증 가능.
- **버튼과 완전히 같은 handler 공유**: MONTH_PICKER/YEAR_PICKER 분기에서 `onStepUp`/`onStepDown` 람다를 한 번만 만들어 `VisitCalendarPickerHeaderRow`(▲▼ 버튼)와 `rememberVisitCalendarPickerSwipeModifier`(swipe) 양쪽에 그대로 전달 — 별도 swipe 전용 경로 없음.
- **애니메이션도 완전히 재사용**: swipe가 바꾸는 state(`pickerYear`/`decadeStart`)가 기존 `AnimatedContent`+`visitCalendarPickerStepTransition()`(75일차부터 있던 수직 슬라이드) 그대로를 트리거 — 새 transition 안 만듦.
- gesture 범위는 MONTH_PICKER/YEAR_PICKER의 `AnimatedContent` 영역에만 한정(CALENDAR 레벨은 그대로 버튼만). drawer 전체를 감싸는 `verticalScroll`과의 충돌 가능성을 조사 단계에서 짚었고, `change.consume()`으로 소비해 우선권을 가져가도록 구현 — 실기기 QA로 충돌 없음 확인.

## 방문 달력 — 오늘 복귀 햅틱

- `vibrateVisitCalendarTodayReturn`(14ms/amplitude 120) 추가. 프로젝트 표준 패턴(`Vibrator.vibrate(VibrationEffect.createOneShot)`, `LocalHapticFeedback` 무반응 확인 전력 재사용 — 갤러리 `vibrateGalleryFab`, 인트로 `vibrateIntroPostmark`와 동일 판단).
- 강도는 인트로 방문 소인의 "통"(24ms/175)보다 가볍고 갤러리 최소 탭(10ms/90)보다 살짝 무겁게 — "톡" 목표.
- 반복 탭 방지 로직 불필요: "오늘" 텍스트 자체가 기존부터 `!isCurrentMonth`일 때만 렌더링되므로(이미 76일차 이전부터 있던 gate), 클릭이 발생하는 시점은 항상 실제 이동일 때뿐.

## Intro — 막스 베르스타펜 33일차 버그 원인·수정

- **원인**: `INTRO_MILESTONE_MESSAGES[33]` 확정 조건 자체는 이미 정확했지만, 같은 문자열("뚜뚜뚜두 막스 베르스타펜")이 무작위 이스터에그 풀 `INTRO_SECRET_MESSAGES`(3% 확률)에도 동시에 들어있었음 — 33일차가 아닌 날에도 약 1%(3%×1/3) 확률로 같은 문구가 새어나왔음(실제 발견 당시 6일차에 노출).
- **수정**: `INTRO_MAX_MILESTONE_MESSAGE` 상수로 분리해 milestone map에서만 참조하고, `INTRO_SECRET_MESSAGES`에서는 완전히 제거(남은 무작위 풀은 "챗지피티야 고마워"/"비개발자가 만들었어요" 2개). 방문 데이터·Room·streak 계산은 전혀 안 건드림 — 순수 문구 풀 구성만 수정.
- 기존에 이 버그를 놓쳤던 이유: `AppIntroMessageLogicTest.secretMessages_matchFixedSpec`가 막스 문구가 풀 안에 있는 걸 "정상"으로 고정 검증하고 있었음 — 이번에 그 테스트도 함께 수정.

## 0/6/32/33/34 경계 테스트

`selectIntroMessage_atNonMilestoneVisitDays_neverReturnsMaxVerstappenMessageEvenByRandomRoll`(0/6/32/34, 각 2만 회 롤) + `selectIntroMessage_at33rdVisit_alwaysReturnsExactlyTheMaxMilestoneMessageConstant`(1천 회) 신규 추가, 기존 32/33/34 milestone 비적용 테스트와 함께 전부 통과.

## 자동 검증

- `compileDebugKotlin`: BUILD SUCCESSFUL(2회 재실행, marker/swipe 라운드 + 복귀 링크 라운드), 신규 경고 없음(기존 Migration/deprecated 경고만).
- `VisitCalendarTest` 24건(marker 판정 2개, 겹침 방지 1개, swipe 방향 1개, 창 안/밖 판정 2개 = 신규 6건), `AppIntroMessageLogicTest` 18건(신규 2건) 전부 통과.
- 전체 unit test: 660건 전부 통과, 실패·에러 0건(test-results XML 직접 집계로 재확인).
- `git diff --check`: 통과(기존 LF→CRLF 경고만).
- **미검증(Compose UI 테스트 하네스 없음, 75일차와 동일한 이유)**: 실제 swipe 제스처 손맛, drawer scroll과의 실제 충돌 여부, marker·복귀 링크 실기기 가독성, 햅틱 강도 — 전부 실기기 QA로 확인함(아래).

## 실기기 QA

2라운드 전부 통과. 1라운드(marker+swipe+햅틱+Intro): "월/연도 marker 잘 보이고 swipe도 자연스러워", "햅틱은 톡 정도로 가볍고, 막스 문구도 안 떴어". 2라운드(오늘 복귀 링크 후속): "잘 돌아가". MONTH_PICKER/YEAR_PICKER marker 가독성, swipe 방향·손맛, 기존 ▲▼ 버튼과의 일관성, 오늘 복귀 햅틱 강도, 33일 아닌 날 막스 문구 미노출, 창 밖일 때 복귀 링크 동작까지 전부 확인됨.

## 자연 QA로 남긴 항목

실제 총 방문일이 33일에 도달하는 날 막스 문구가 실제로 뜨는지는 자동 테스트(33일차 강제 파라미터 통과)로만 확인했고, 실사용 33일차 자연 노출은 아직 미확인 — 방문일이 자연스럽게 33에 도달할 때까지 향후 확인 대상으로 남김(지시서 20절에 명시된 정책과 동일).

## 다음 행동

1. 실기기 QA 통과 — 사용자의 commit·push 승인 대기 중.
2. 승인 후 commit·push, push 후 HEAD/local-origin/git status 재확인해 이 HANDOFF에 반영 예정.

---

# HANDOFF — 76일차 후속: 기억밀도 카오모지 → 새싹형 전환

확인일: 2026-09-17. 수동 표준 모드. 76일차 본작업(commit `7d2f476`/`515ada5`) 이후 같은 세션에서 이어진 후속 지시서("카오모지형 → 새싹형 전환")를 피코(Claude Code)가 직접 구현했어. HTML 목업 먼저 만들어 확인받고(사용자 요청으로 편자→♥ 모양 수정, 하트 진하기, 물뿌리개 아이콘 추가까지 목업 단계에서 반복), 그 다음 production에 반영 → 실기기 QA → QA 피드백 2건(물뿌리개 제거, 구분선 통합, 얼굴 색 되돌림) 반영까지 끝냈어. **실기기 QA 통과, 사용자가 "커밋하고 푸시 부탁해"로 명시적으로 요청 — 이 HANDOFF 커밋에 포함. 사용자가 이 작업으로 76일차 전체를 마무리한다고 확인함(추가 예정 작업 없음).**

## 최종 상태

기억밀도(`GalleryDensityPage`)의 월별 표현이 카오모지 3단계(•_•/˙ᵕ˙/ᵔᴗᵔ)에서 "새싹형"으로 완전히 바뀌었어.

- **폐기 이유**: 카오모지 글리프마다 시각적 무게중심이 달라서 같은 기준선에 정렬해도 얼굴이 위/아래로 떠 보이는 문제가 있었고, "위로 갈수록 높은 수치"라는 그래프 문법과 표정이 충돌할 수 있다는 게 사용자 판단이었어.
- **새 구조(위→아래)**: overflow "+" 표시 → 수치만큼 자란 얇은 줄기(`MEMORY_DENSITY_STEM_WIDTH` 3dp, 기존 막대 14dp보다 훨씬 얇음) → 줄기 위 고정 모양 하트(♥, 처음엔 SVG 잎 두 장이었으나 사용자가 실기기에서 "말굽 편자 같다"고 해서 ♥ 텍스트로 교체) → **전체 폭으로 이어진 구분선 하나**(원래 달마다 22dp 개별 구분선이었는데 실기기 QA에서 "___" 끊김이 지적돼 통합) → 고정된 무표정 얼굴 "•_•"(색은 항상 `InkSecondary` 고정 — 하트 진하기와 공유해보자는 사용자 요청이 있었지만 실제로 보고 "원래대로 돌려달라"고 되돌림) → 월 숫자.
- **하트 진하기**: `memoryDensityHeartAlpha(level) = 0.4 + (level/6)*0.6`. 줄기가 높을수록(레벨 1→6) 하트가 옅음→진한 sunset-gold로 진해짐. 이 로직은 하트에만 남아있고 얼굴에는 적용 안 됨(사용자가 명시적으로 되돌림).
- **막대 단위 규칙은 그대로 유지**: 1칸=엽서 2장, 최대 6칸(12장), 13장부터 6칸 유지+"+".
- **물뿌리개 아이콘**: "2026년" 옆에 커스텀 `ImageVector`(`WateringCanIcon`, `PondDrawerIcon`과 같은 `ImageVector.Builder` 패턴)로 한 번 추가했으나, 실기기 QA에서 "빼줘"라는 요청으로 완전히 제거함(정의 자체도 삭제, dead code 안 남김).
- **함수 분리**: 기존 `GalleryMemoryDensityBar` 하나였던 걸 `GalleryMemoryDensityStem`(overflow+줄기+하트, 접근성 contentDescription 유지)과 `GalleryMemoryDensityFoot`(얼굴+월 숫자, `clearAndSetSemantics`로 중복 안내 방지)으로 나눴어 — 구분선을 전체 폭 하나로 통합하려면 Row 두 개로 쪼개야 했기 때문.
- **삭제한 것**: `memoryDensityKaomoji` 함수 완전 삭제(소스에 흔적 없음), 관련 카오모지 경계값 테스트 삭제.
- **기억밀도 데이터 기준·연도 처리·터치 상호작용 없음 등 76일차 본작업 원칙은 전혀 안 바뀜.**

## 목업 반복 과정(참고용, `docs/ai/mockups/memory-density-mockup.html`)

1차: 카오모지 폐기 + 하단 고정 얼굴 + 줄기 + 새싹(SVG 잎 두 장) → 사용자가 "말굽 편자 같다"고 지적 → SVG를 걷어내고 텍스트 "♥"로 교체 → 승인.
2차 요청: 하트 진하기를 높이에 비례하게, "2026년" 옆에 물뿌리개 아이콘 추가 → 둘 다 구현(물뿌리개는 초반에 자물쇠처럼 보여 물방울 2개를 추가해 재작성).
production 반영 후 실기기 QA → 물뿌리개 제거, 구분선 통합, 얼굴 색 되돌림 3가지 피드백 → production·테스트·목업 모두 동일하게 반영하고 재검증.

목업은 Chrome headless(`--headless=new`)로 매 단계 직접 캡처해서 확인했고, 검증용 스크린샷·임시 프로필은 매번 삭제해 저장소에 남기지 않았어. (세션 중 사용자가 "임시 파일은 저장소 내부 안전한 경로를 쓰고 작업 후 지워라"고 요청해 이후로는 `/tmp` 대신 `docs/ai/mockups/` 안에 밑줄 접두사 임시 파일을 만들었다가 확인 후 즉시 삭제하는 방식으로 전환했어.)

## 자동 검증

- `compileDebugKotlin`: 매 반영 라운드마다 재실행, 최종 `BUILD SUCCESSFUL`, 신규 경고 없음.
- gallery 패키지 관련 테스트: 최종 `GalleryMemoryDensityStructureTest` 7건, `GalleryMemoryDensityTest` 13건 전부 통과(카오모지 테스트 삭제, 하트 진하기 경계값·단조증가·물뿌리개 부재·구분선 단일화·얼굴 고정색 확인 테스트로 교체).
- 전체 unit test: 652건 전부 통과, 실패·에러 0건(XML 결과 직접 집계로 재확인, fork 보고를 그대로 신뢰하지 않음).
- `git diff --check`: 통과(기존 LF→CRLF 경고만).

## 실기기 QA

2라운드 진행. 1라운드: 전체적으로 만족("아주 완벽해 내가 원했던 거 그대로야" — 이건 76일차 본작업 ViewMode 축소 건). 새싹형 자체는 이번 후속 작업 QA에서 3가지 피드백(물뿌리개 제거/구분선 통합/얼굴 색 되돌림)을 받았고 전부 반영 후 사용자가 "좋아 커밋하고 푸시 부탁해"로 최종 승인.

## Git

commit·push는 사용자가 명시적으로 요청("좋아 커밋하고 푸시 부탁해~"). commit `06af4fe`(5 files changed), push 완료. 종료 HEAD `06af4fe`, local == origin/feature/photo-sticker, tracked working tree clean(기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 남음).

## 다음 행동

**없음.** 사용자가 이 작업으로 76일차(본작업+후속 새싹형 전환)를 완전히 마무리한다고 명시했어. 77일차 예약 범위(방문 달력 폴리싱, Intro 총 방문 33일차 조건 복구)는 여전히 미착수 상태로 남아있고, 다음 세션은 새 지시서로 시작하면 돼.

---

# HANDOFF — 76일차: 갤러리 보기 체계 축소 + 기억밀도 재정의

확인일: 2026-09-17. 수동 표준 모드. 사용자 제공 76일차 지시서에 따라 피코(Claude Code)가 조사→production 구현(fork 위임 후 직접 재검증)→실기기 QA→commit·push 순으로 진행했어. 공용 작업판은 활성화하지 않았어. **실기기 QA 통과("아주 완벽해 내가 원했던 거 그대로야"), 사용자가 commit·push를 명시적으로 요청 — 이 HANDOFF 커밋에 포함.**

## 시작 상태

branch `feature/photo-sticker`, 시작 HEAD `e6659da`(local==origin, tracked clean, 기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 존재 — 실측이 인수인계 참고값과 일치). 직전 HANDOFF는 75일차 방문 달력 작업으로 오늘 작업과 무관했어.

## 기존 ViewMode 구조(조사 결과)

- enum은 `GalleryPageFormat`(GalleryPageFormat.kt) — THREE_COLUMN/MONTHLY/TIMELINE/CALENDAR/STAMP/DENSITY 6종, default THREE_COLUMN. (참고: `GalleryViewMode.kt`의 COMPACT_GRID/DETAIL_LIST는 어디서도 참조 안 되는 완전 무관 dead code라 이번에도 건드리지 않았어.)
- 저장 방식은 SharedPreferences/DataStore가 아니라 `GalleryScreen.kt` 내부 `rememberSaveable`+커스텀 Saver뿐 — 앱 완전 재시작 시 항상 기본값으로 리셋됨(회전 등 프로세스 재생성에만 살아남음).
- 조사 중 지시서에 없던 위험 2가지를 발견해 사용자에게 확인받았어: ①"퐁(연못) 놀이 모드"가 3단 보기에만 렌더링 경로가 있었음(→월별 보기로 이식 승인), ②기억밀도로 들어가는 별도 진입점이 없었음(→기존 pager+점 인디케이터를 그대로 재사용해 월별/기억밀도 2페이지 좌우 스와이프로 승인).

## 삭제한 보기 / 월별 보기 default화

- 3단 보기, 캘린더 보기(갤러리 ViewMode만 — 방문 달력과 무관), 우표 보기, 타임라인 보기 삭제. enum에서 4개 항목 제거, 전용 composable(`GalleryThreeColumnPage`, `GalleryCalendarPage`, `GalleryStampPage`+`GalleryStampGridItem`, `GalleryTimelinePage`+`GalleryTimelineEntry`) 삭제.
- 공용 자산은 보존: `PinkingPhotoShape`/`StampCardContent`/`StampCard`(다른 화면에서도 재사용), `calendarCellsFor`(방문 달력 `VisitCalendarDrawer.kt`가 재사용 중 — 삭제했으면 방문 달력이 깨졌을 부분, diff에서 직접 확인).
- `activePageFormats`를 토글 가능한 `rememberSaveable Set`에서 `setOf(MONTHLY, DENSITY)` 고정값으로 단순화(더 이상 아무도 켤 수 없는 토글 로직을 남기지 않음). `currentPageFormat` 기본값 MONTHLY.
- 우측 상단 체크박스 기반 "보기 형식 관리" 드롭다운 UI 전체 제거(`GalleryPageFormatMenuItem` 포함). 기존 `HorizontalPager`+`GalleryPageIndicator`(2페이지 이상일 때 자동 표시)가 그대로 월별↔기억밀도 스와이프를 보여줌 — 새 UI 문법 추가 없음.
- 검색 아이콘 노출 조건과 검색 강제 종료 `LaunchedEffect`를 THREE_COLUMN→MONTHLY로 이관(정렬은 원래 MONTHLY도 `sortAffectsOrder=true`라 이관 불필요).
- 퐁 모드: `isPondModeOn`/`pondController`와 탭/드래그 파문 제스처를 `GalleryMonthlyGridPage`로 이식. 평소엔 가벼운 `StampCardContent`, 퐁 모드 켜졌을 때만 물리 연출 붙은 `StampCard`로 전환(`StampCard.kt`에 `dateLabelOverride` 파라미터 추가해 월 헤더와 날짜 중복 안 되게 "일"만 표기).
- legacy fallback: `PageFormatSaver`가 삭제된 이름(THREE_COLUMN/CALENDAR/STAMP/TIMELINE)이나 알 수 없는 값을 만나면 MONTHLY로 자동 복귀(`getOrDefault(GalleryPageFormat.MONTHLY)`). SharedPreferences/DataStore 자체가 없어 별도 마이그레이션 불필요.

## 기억밀도 재정의

- 데이터 source: `postcard.capturedAt`(방문 기록 아님), 지정 연도(기본 현재 연도) 1~12월만 — 연도 이동 picker는 기존에 없던 기능이라 새로 안 만듦.
- 막대 단위: 1칸=엽서 2장, 최대 6칸(12장), 13장부터 6칸 유지+위에 "+" 표시(`memoryDensityMonthsForYear`/`memoryDensityBarLevel`/`memoryDensityHasOverflow`).
- 카오모지 3단계: 0~4장 •_•, 5~8장 ˙ᵕ˙, 9장 이상 ᵔᴗᵔ(`memoryDensityKaomoji`) — 슬픈/우는 표정 없음.
- 레이아웃: 12개월을 `weight(1f)` 균등폭 Row로 한 화면 배치, 가로 스크롤 없음 — 실기기 QA로 뭉개짐 없이 확인됨. 월 표기는 숫자만("1"~"12").
- 기존에 있던 "점 탭하면 그 달 사진이 아래 펼쳐지는" 기능은 제거함 — "dashboard 아닌 순수 조회 화면" 재정의와 새 막대그래프 디자인이 양립하지 않아서 없앴고, 실기기 QA 통과로 사실상 승인됨(사용자에게 명시적으로 짚었고 이견 없었음).

## 자동 검증

- 내가 직접 재실행(fork의 보고를 그대로 믿지 않고 diff·테스트 결과를 재검증): `:app:testDebugUnitTest`(gallery 패키지 9개 파일, VisitCalendarTest 18건 포함 전부 통과) + `:app:compileDebugKotlin` `BUILD SUCCESSFUL`.
- 전체 unit test 재실행: `BUILD SUCCESSFUL`, test-results XML 직접 집계 **647건 전부 통과, 실패·에러 0건**.
- `git diff --check` 통과(기존 LF→CRLF 경고만).
- 기억밀도 막대/카오모지 경계값(0~13+ 전 구간), legacy ViewMode fallback 최소 테스트 신규 추가.
- 삭제한 ViewMode 전용 테스트 3개 파일(`GalleryCalendarStructureTest`/`GalleryStampStructureTest`/`GalleryTimelineStructureTest`) 삭제, `GalleryViewSelectionStructureTest`는 새 selector 없는 구조에 맞게 전면 재작성.

## 실기기 QA

통과. 사용자 확인: "아주 완벽해 내가 원했던 거 그대로야". 12개월 배치·월 숫자 표기·카오모지 가독성·퐁 모드 이관 모두 문제 없음.

## 추가 산출물 — 기억밀도 목업

며칠치 실데이터만으로는 밀도 체감이 어렵다는 사용자 요청으로 `docs/ai/mockups/memory-density-mockup.html` 추가(74~75일차와 같은 패턴 — 앱 밖 독립 HTML, production 미반영). 실제 앱과 동일한 막대/overflow/카오모지 공식을 그대로 재현했고, 월별 숫자를 직접 입력해 실시간으로 바꿔보거나 프리셋(조용한 해/보통인 해/풍성한 해/폭발적인 달 포함/기복이 큰 해)으로 비교 가능. Chrome headless로 직접 캡처해 12개월 전체 배치·overflow 표시·카오모지 단계를 확인했고, 그 과정에서 flex 자식 최소 너비 문제(입력창 때문에 12번째 달까지 안 잘리는지 확인 안 되던 버그)를 발견해 `min-width: 0` 추가로 수정했어. 검증용 스크린샷과 임시 Chrome 프로필은 작업 후 삭제.

## 남은 위험 / 다음 행동

- 지시서 26절(기억밀도 월 탭→월별 보기 이동)은 오늘 범위 아님, 77일차 이후 후보.
- 77일차 예약 범위(방문 달력 폴리싱, Intro 총 방문 33일차 조건 복구)는 이번 작업에서 건드리지 않음.
- 실사용 데이터가 쌓이면 12개월 배치·카오모지 가독성을 다시 한 번 확인하면 좋음(현재는 QA 시점 데이터량 기준 확인).

## Git

commit·push는 사용자가 목업 확인 후 명시적으로 요청("이대로 커밋 푸시하자")했어. commit `7d2f476`(14 files changed), push 완료. 종료 HEAD `7d2f476`, local == origin/feature/photo-sticker, tracked working tree clean(기존 무관 untracked `.codex-config.candidate.toml`/`.kotlin/`만 남음).

---

# HANDOFF — 75일차 추가: 방문 달력 계층형 월/연도 탐색

확인일: 2026-09-16. 수동 표준 모드. 75일차 기본 작업(월 이동/오늘 복귀/애니메이션/오늘 색) commit·push(`f687145`) 이후, 사용자가 챗지피티에게 받은 "75일차 추가 수정지시서"(계층형 월/연도 탐색)를 피코(Claude Code)가 직접 구현했어. 이어서 실기기 QA 중 나온 사용자 추가 요청(▼ 버튼 가시성, ▲▼ 애니메이션, 4×4 grid, 달력 6주 고정)까지 같은 흐름에서 반영했어. 공용 작업판은 활성화하지 않았어. **실기기 QA 통과, 사용자가 commit·push를 명시적으로 요청 — 이 HANDOFF 커밋에 포함.**

## 계층형 탐색 최종 상태

시작 HEAD `f687145`(clean, 기존 untracked 2개만). CALENDAR → MONTH_PICKER → YEAR_PICKER 3단 탐색기 + 실기기 QA 2라운드 반영까지 구현·자동 검증 완료.

- navigation level: `internal enum class VisitCalendarNavLevel { CALENDAR, MONTH_PICKER, YEAR_PICKER }`. `VisitCalendarDrawer`(부모)에서 `rememberSaveable`로 보유(hoist) — `BackHandler`와 drawer 재오픈 리셋이 이 값을 봐야 해서 `MonthlyVisitCalendar` 밖으로 끌어올렸어. `MonthlyVisitCalendar`는 `navLevel`/`onNavLevelChange`를 파라미터로만 받음.
- 표시 연도 state: `pickerYear: Int`를 `displayedMonth`와 분리된 별도 `rememberSaveable`로 둠. MONTH_PICKER/YEAR_PICKER 안에서 연도만 훑어보다가 선택 없이 뒤로 가도 실제 달력(`displayedMonth`)은 건드리지 않음 — 월/연도를 실제로 선택(탭)하는 순간에만 `displayedMonth`에 반영.
- **MONTH_PICKER grid 구조(QA 후 확정): 4열×4행(16칸)** — `monthPickerGridCells(pickerYear)`가 pickerYear의 1~12월(라벨 "1"~"12", "월" 접미사 제거) + 다음 해 1~4월을 반환. 다음 해 4칸은 `VisitCalendarAdjacentPeriodColor`(`InkSecondary` alpha 0.4, 새 회색 토큰 추가 안 함)로 낮춰 구분.
- **YEAR_PICKER grid 구조(QA 후 확정): 4열×4행(16칸)** — `yearPickerGridYears(decadeStart)`가 decade 앞 2년 + 실제 10년 + 뒤 4년을 반환(예: decadeStart=2020 → 2018~2033). 앞/뒤 6칸은 같은 낮춘 색으로 구분. 사용자가 확인한 Windows 작업표시줄 캘린더의 연도 grid 배치와 동일. (최초엔 5×2였다가 이 요청으로 4×4로 교체.)
- 연도 ▲▼ 동작: MONTH_PICKER 헤더 우측 ▲=`pickerYear++`(다음 연도), ▼=`pickerYear--`(이전 연도).
- decade ▲▼ 동작: YEAR_PICKER 헤더 우측 ▲=`pickerYear += 10`, ▼=`pickerYear -= 10`. decade 자체 state 없이 `pickerYear`에서 매번 계산.
- decade 계산 방식: `internal fun decadeStartFor(year: Int) = (year / 10) * 10`. 순수 함수, 경계값(1999/2000/2009/2010/2029/2030) 테스트 완료.
- 월 선택 동작: MONTH_PICKER에서 월(다음 해 4칸 포함) 탭 → `displayedMonth = YearMonth.of(선택한 year, month)`, `navLevel = CALENDAR`로 즉시 복귀. history marker·visit count·streak 변경 없음(순수 표시 state만).
- 연도 선택 동작: YEAR_PICKER에서 연도(앞뒤 6칸 포함) 탭 → `pickerYear = year`, `navLevel = MONTH_PICKER`로 한 단계 내려옴.
- 현재 선택값 표시: 강조 기준은 "오늘"이 아니라 "지금 표시 중인 연·월"(실제 `displayedMonth`와 정확히 일치하는 칸만). 강조 방식은 텍스트 weight(Medium)+색(InkPrimary) 하나만 사용 — 방문 라벨보다 약함.
- 계층 전환 animation(CALENDAR↔MONTH_PICKER↔YEAR_PICKER): `visitCalendarHierarchyTransition()` — `fadeIn + scaleIn(0.97f)` / `fadeOut + scaleOut(0.97f)`, 방향성 없이 대칭 처리, 170ms. `SizeTransform`도 같은 spec으로 덮어써 기본 spring 제거.
- **▲▼ 스텝 애니메이션(실기기 QA 추가 요청, 최초엔 애니메이션 없이 즉시 전환이라 "밑밑하다"는 피드백):** `visitCalendarPickerStepTransition()` 추가 — 월 이동과 같은 원리(state 비교가 방향 결정)로 `slideInVertically`/`slideOutVertically` 사용. ▲(다음 연도/decade)는 위로, ▼(이전)는 아래로 스르륵. MONTH_PICKER는 `pickerYear`로, YEAR_PICKER는 `decadeStart`로 각각 `AnimatedContent` 트리거. `MONTH_TRANSITION_DURATION_MS`(200ms) 재사용 — 월 이동과 같은 느낌을 원한 요청이라 새 duration을 만들지 않음.
- 계층 전환 duration: 170ms. 스텝(▲▼) 전환 duration: 200ms(월 이동과 동일).
- **CALENDAR 날짜 grid 높이(실기기 QA 추가 요청): 항상 6주(42칸) 고정.** `visitCalendarPaddedCells(month)`가 실제 달 뒤에 빈 칸(null)만 채워 42칸으로 맞춤 — 실제 날짜를 만들어내지 않음. 지시서 원문은 "5주 기준"이라고 했지만, 31일짜리 달이 금/토에 시작하면 실제로 6주가 필요해 5주로 고정하면 그 달만 여전히 튀어나온다고 판단해 6주로 구현하고 사용자에게 설명함(승인됨, 실기기 QA 통과). 주차 구분선은 실제 주(real week)까지만 그리고 패딩 행 사이에는 넣지 않음(`realWeekCount` 계산).
- drawer 재오픈 시 mode: `LaunchedEffect(drawerState.isOpen)`으로 drawer가 열릴 때마다 `navLevel = CALENDAR`로 리셋. `displayedMonth`는 기존 74·75일차 동작 그대로 유지.
- 오늘 복귀 시 mode: "오늘" 버튼은 CALENDAR 레벨에서만 보임(변경 없음) — 이미 CALENDAR로 돌아온 상태에서만 누를 수 있음.
- back 동작: `VisitCalendarDrawer`의 `BackHandler` — `navLevel == CALENDAR`면 drawer를 닫고, 아니면 `visitCalendarNavLevelOnBack(navLevel)`로 한 단계만 내려옴(YEAR_PICKER→MONTH_PICKER→CALENDAR).
- 미래 연도 탐색 정책: 75일차 기본 작업의 "제한 없음" 정책 그대로.
- 방문 셀 스타일(2dp, `#16A7A1`/`#117E7A`, 자동 대비)은 이번 작업에서 전혀 건드리지 않음.
- **QA 라운드 1 수정**: picker 헤더 Row에 `height(24.dp)`로 고정했다가 ▲▼ 아이콘 두 개(18+18=36dp)가 그 안에 다 못 들어가 ▼가 잘려서 작게 보이던 버그. 고정 높이를 제거하고 아이콘을 20dp로, 둘 사이 2dp 간격을 추가해 수정.
- UI 미감: Material DatePicker·card·pill·border·shadow·gradient 없음. 텍스트 중심 grid + 기존 `clickable` ripple만 사용. ▲▼는 20dp 아이콘(월/연도 이동 화살표와 동일 크기로 QA 후 통일).

### 자동 검증

- `:app:testDebugUnitTest --tests VisitCalendarTest :app:compileDebugKotlin`: 매 수정 라운드마다 재실행, 최종 `BUILD SUCCESSFUL`. `VisitCalendarTest` 11→18건(decade 계산·back 단계 이동·강조 로직·4×4 grid 생성·6주 패딩 신규 7건) 전부 통과. `compileDebugKotlin`은 기존 경고만, 신규 경고 없음.
- `git diff --check` 통과(기존 CRLF 경고만).
- **미검증**: 실제 탭 → navLevel 전환, ▲▼ 빠른 반복 입력, 각 애니메이션 자체, drawer 재오픈 시 리셋은 Compose UI 테스트 하네스가 이 저장소에 없어(새 테스트 의존성 추가는 미승인 범위) JVM 단위 테스트로 확인 불가 — **실기기 QA 2라운드로 확인 완료**(제목 터치, 4×4 월/연도 grid, ▲▼ 방향·가시성, 계층 전환, drawer 재오픈, back, ▲▼ 슬라이드 애니메이션, 6주 고정 높이 모두 통과).

### 사용자 승인

2026-09-16, 실기기 QA 2라운드(▼ 가시성 수정 확인, ▲▼ 애니메이션·4×4 grid·6주 높이 확인) 통과 후 사용자가 "커밋하고 푸시하자"로 commit·push를 명시적으로 요청함.

### 다음 행동

1. 실기기 QA 통과, 사용자 승인 완료 — commit·push 진행.
2. 다음 작업은 없음. 필요하면 사용자가 새 지시서로 시작.

---

# HANDOFF — 75일차 방문 달력 월 탐색·오늘 복귀·전환 애니메이션·오늘 방문 색

확인일: 2026-09-16. 수동 표준 모드. 사용자 제공 75일차 지시서에 따라 피코(Claude Code)가 직접 구현했어. 공용 작업판은 활성화하지 않았어.

## 75일차 현재 상태

시작 HEAD는 `86baedf`(74일차 장식선 production 반영까지 포함, 지시서 참고값 `a113dd7`보다 한 커밋 앞섰음 — 실제 상태로 확인 후 진행). 이전/다음 달 이동, 오늘 복귀, 월 전환 슬라이드 애니메이션은 구현·자동 검증까지 끝났어. **오늘 방문 색상만 사용자 목업 승인 대기**야 — 승인 전에는 production에 최종 반영하지 않아.

- 표시 월 state: `MonthlyVisitCalendar` 안에 `displayedMonth`를 `rememberSaveable`로 둬(기존 `GalleryScreen.kt`의 `visibleMonth` 패턴과 동일한 `YearMonth` 문자열 Saver 재사용, 이 파일 안에 따로 둠). 이전/다음 달은 `YearMonth.minusMonths(1)`/`plusMonths(1)`만 호출 — `visitedEpochDays`/`totalVisitDays`는 절대 만지지 않음.
- 이전/다음 달 UI: 이 드로어에 이미 있던 닫기 버튼과 같은 문법(`IconButton` + `InkSecondary` 20dp 아이콘) 재사용. `Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right` — `GalleryScreen.kt`의 `GalleryCalendarPage` 월 이동과 같은 아이콘·색상 선례.
- 미래 달 정책: **제한 없음**. `GalleryCalendarPage`도 미래 달 제한이 없는 기존 선례라 그대로 따름 — 빈 달만 보일 뿐 데이터 위험이 없고, 제한을 넣으면 state만 복잡해짐.
- 오늘 복귀: "다녀간 날들" 캡션 줄 끝에 작은 텍스트 `오늘`, 현재 월일 때는 숨김(`isCurrentMonth` 조건). 방문 데이터에는 손대지 않고 `displayedMonth = YearMonth.from(today)`만 실행.
- 애니메이션: Compose `AnimatedContent` 2곳(월 제목 텍스트, 날짜 grid) — 둘 다 같은 `displayedMonth`로 동시에 트리거되어 함께 슬라이드. 방향은 `targetState > initialState`(YearMonth 비교)로만 결정 — 별도 "방향" state 없음(state가 진짜 값, animation은 표현). `slideInHorizontally`/`slideOutHorizontally` + `tween(200ms, FastOutSlowInEasing)`, `SizeTransform`도 같은 duration으로 덮어써 기본 spring bounce를 제거. `clipToBounds()`로 슬라이드 중 드로어 폭 밖으로 새는 것 방지.
- animation duration: **200ms** (150~250ms 검토 범위 중 다른 화면 전환보다 유독 느리지 않은 중간값).
- 오늘 방문 색: 함수 `visitDayFillColor(date, today)` / `visitDayFillContrastColor(date, today)` 추가. `visited`가 이미 true인 날짜에만 호출되므로(기존 gate 유지) 오늘+미방문·미래는 애초에 라벨 자체가 없음. 오늘+방문만 `VisitFillColorToday`, 그 외 방문은 기존 `VisitFillColor(#16A7A1)` 그대로. 대비도 같은 `labelStickerTextColorArgbFor` 재사용(`VisitFillContrastColorToday`).
- **오늘 방문 색 최종 HEX: `#117E7A`(후보 B, "또렷하게") 확정.** 아티팩트(https://claude.ai/artifact/5v44EFArEciGTaB8FacmCE)와 로컬 목업(`docs/ai/mockups/today-color-artifact.html`, `docs/ai/mockups/visit-calendar-today-color.html`)으로 A(#13908B)/B(#117E7A)/C(#0E6C68) 세 후보를 제시했고 사용자가 B를 선택했어(2026-09-16). `VisitFillColorToday`/`VisitFillTodayArgb`에 반영 후 재검증 완료. 세 후보 모두 luminance 계산상 기존과 같은 "밝은 글자" 쪽으로 나와 자동 대비 로직이 뒤집히지 않음을 Node로 확인했음.
- 2dp 라벨 모서리, 3dp 내부 여백, 카오모지 4종, deterministic selector, 토요일/일요일 무디게 낮춘 잉크색, 주차 구분선, 상하단 장식선, "다녀간 날들"/"오늘까지 N번 만났어요~!"/봉투 카운터, drawer 구조, 달력 크기 모두 그대로 유지.
- 공휴일 STOP 유지, 회귀 보호(방문 기록/Intro/Gallery) 모두 준수. 실제 `filesDir`·`visit_record.txt`·history marker·total/streak 접근 없음.

### 자동 검증

- `:app:testDebugUnitTest --tests VisitCalendarTest --tests VisitHistoryStorageTest :app:compileDebugKotlin`: `BUILD SUCCESSFUL`. `VisitCalendarTest` 8→11건(오늘 방문 색 결정 로직 신규 3건 포함) 전부 통과, `VisitHistoryStorageTest` 8건 통과. `compileDebugKotlin`은 기존 경고(Migration `db` 파라미터명, deprecated API)만 있고 신규 경고 없음. B 색상 확정 후 동일 테스트·compile 재실행해 재통과 확인(11건·8건 그대로 통과).
- `git diff --check` 통과(기존 LF→CRLF 경고만, 신규 오류 없음).
- 전체 unit test는 국소 UI 변경에 비례해 실행하지 않았어(기존 74일차 관례와 동일).
- 실기기 설치·실행·삭제·초기화·계측 테스트·사용자 데이터 조작 없음.
- 월 이동 자체의 연도 경계(12월↔1월)는 java.time `YearMonth.minusMonths/plusMonths`에 위임하고 자체 계산을 하지 않음 — 기존 `sharedCalendarHandlesLeapYearsMonthLengthsAndYearBoundary` 테스트가 `calendarCellsFor`로 이미 확인.
- **미검증**: 실제 클릭 → `displayedMonth` state 전환·애니메이션 방향·빠른 연속 클릭·drawer 재오픈 후 유지는 Compose UI 테스트 하네스(Robolectric 등)가 이 저장소에 없어 JVM 단위 테스트로 확인 불가 — 실기기 QA로만 확인 가능. 새 테스트 의존성 추가는 미승인 범위라 시도하지 않았어.

### 다음 행동

1. 실기기 QA(75일차 지시서 28절: 월 이동, 오늘 복귀, 애니메이션, 오늘 방문 색 `#117E7A`) — 대기 중.
2. QA 통과 후 사용자 승인 시에만 commit, 이후 별도 승인 시에만 push.

---

# HANDOFF — 74일차 방문 달력 폴리싱

확인일: 2026-09-15. 수동 표준 모드. 사용자 제공 74일차 지시서에 따라 Codex가 직접 구현했어. 공용 작업판은 활성화하지 않았어.

## 현재 상태

방문 라벨 모서리는 **6dp → 2dp**로 바꿨고 실기기 QA와 이전 commit·push까지 끝났어. 사용자가 승인한 전체 폭 장식선을 실제 방문 달력에도 최소 반영했어. 청록색·라벨 크기·3dp 여백·날짜·카오모지 위치·대비와 방문 저장 구조는 그대로야. 공휴일은 지원 연도 결정 지점에서 STOP했어.

| 구분 | 상태 |
|---|---|
| 구현 | 2dp 완료. 장식선 목업 승인 및 production 최소 반영 완료. 공휴일 미구현·정책 판단 대기 |
| 자동 검증 | 관련 테스트 16건·Kotlin compile·diff check 통과 |
| 사용자 QA | 2dp 실기기 완료. 장식선 목업 승인 완료, production 실기기 QA 대기 |
| commit | 2dp 작업 완료 — `49654c2`, HANDOFF `a113dd7`. 현재 장식선 변경은 미커밋 |
| push | 2dp 작업 완료 — 원격 `a113dd7`. 현재 장식선 변경은 미푸시 |
| 전체 완료 | 2dp 폴리싱 완료. 장식선은 production 반영 후 실기기 QA 대기. 공휴일 구현은 사용자 정책 판단 대기 |

73일차 원문은 [방문 달력 최종 인수인계](archive/HANDOFF-2026-09-14-visit-calendar-final.md)에 byte 동일하게 보존했어(SHA256 일치 확인). 과거 사고·실패·폐기 접근도 원문 그대로야. archive 안 상대 링크는 원래 HANDOFF 위치 기준이야. 원문에 QA 완료 표와 과거 QA 대기 문장이 혼재해 있어. 73일차 QA 완료는 현재 사용자 지시서와 원문 최종 표를 기준으로 봐. 이번 QA와 혼동하지 않아.

## Git 시작·종료 상태

- 브랜치: `feature/photo-sticker`.
- 시작 HEAD: `141d6cba7a25db88ed80678efbc8b1c896a87814`. 2dp·목업 commit: `49654c2` (`Polish visit calendar label corners`).
- `49654c2` push 후 local과 origin의 `feature/photo-sticker`가 동일한 것을 확인했어. 이 HANDOFF 결과 갱신은 별도 기록 commit으로 이어서 반영해.
- 시작 tracked 변경 없음. 기존 untracked `.claude/`, `.codex-config.candidate.toml`, `.kotlin/` 모두 보존. 지시서보다 `.claude/`가 추가로 있었어.
- `49654c2` 포함 파일: `VisitCalendarDrawer.kt`, `docs/ai/archive/HANDOFF-2026-09-14-visit-calendar-final.md`, `docs/ai/mockups/visit-calendar-2dp.html`, `docs/ai/mockups/visit-calendar-2dp.png`. 현재 HANDOFF 갱신은 별도 기록 commit 대상이야.
- 최근 관련 커밋: `141d6cb` 인수인계, `02574f1` 방문 달력 본체.

## UI/UX 문법·수정 범위

- 역할: 행동 없는 방문 기록 표시. 선택·속성 조절·객체 행동·위험 행동·완료/저장 UI는 해당 없어.
- 근거: `VisitCalendarDrawer.kt`의 현재 월 종이 달력, 73일차 QA와 현재 지시서에서 승인·유지 확인.
- 진입: Gallery 좌측 drawer 그대로. 토큰 `PaperSurface`, `InkSecondary`, `SealInkNavy`, `SealInkRed` 재사용 유지.
- 기존 variant 범위: 역할·정보 계층·상호작용·container·위치는 같고 명시 승인된 반경만 변경. 신규 문법·외부 디자인 레퍼런스·전역 shape 변경은 없어.
- 실제 production diff는 `.background(VisitFillColor, RoundedCornerShape(6.dp))`의 `6`을 `2`로 바꾼 한 줄이야.
- `#16A7A1`, 자동 대비, 카오모지 4종과 날짜 기반 selector, 현재 월만 표시, 크기·spacing은 그대로야.
- 경미한 조정 1회. 핵심 해결 실패·해결책 역전·범위 확대 없음.
- 이번 세션에 최신 실기기 스크린샷은 제공되지 않았어. 새 미감을 실제로 봤다고 주장하지 않아.

## 공휴일 조사와 STOP

`app/src`와 `gradle`의 holiday/공휴일/대체공휴일/KoreanCalendar/lunar 검색에서 기존 STOP 주석만 발견했어. 날짜 helper·방문 기록은 날짜 포맷·요일·epochDay를 다루며 국내 공휴일 데이터가 없어. build 파일과 version catalog에도 공휴일 제공 의존성이 없어.

| 방식 | 정확도·지원 범위 | 대체·임시공휴일 | 새 dependency·네트워크 | 유지보수·복잡도 |
|---|---|---|---|---|
| 공식 자료 기반 정적 연도별 목록 | 대조한 연도와 발표 시점까지. 2026 월력요항 확인 | 공식 대체공휴일 포함 가능. 이후 임시 지정은 데이터 업데이트 필요 | 둘 다 불필요 | 구현 단순, 매년 및 변경 발표 때 목록 대조 필요 |
| 기존 dependency / 플랫폼 API | 한국 공휴일 판정 지원 확인 못함. 날짜/역법 계산과 휴일 정책은 별개 | 자체 계산으로 임시 지정까지 보장 불가 | 현재 구성만으로 정확한 정책 데이터 확보 불가 | 자체 음력·대체 규칙 계산은 복잡하고 유지 비용 큼 |
| 한국천문연구원 특일 정보 API | 공식 날짜·공공기관 휴일 여부·명칭 제공. 전체 지원 연도는 페이지에 미명시 | 실제 응답의 수록·최신성 검증 필요 | 인증키·앱 네트워크 필요, 새 라이브러리 필요 여부와 별개로 API 연결은 STOP 대상 | 장애·캐시·키·갱신 관리 추가 |

출처(2026-09-15 조회):

- [우주항공청 2026년 월력요항](https://www.kasa.go.kr/prog/bbsArticle/BBSMSTR_000000000010/view.do?bbsId=BBSMSTR_000000000010&nttId=B000000001860Pe2zT3): 매년 달력 제작 기준 발표, 대체공휴일 포함. 2025-06-30 발표라 이후 정책 변경까지 검증됐다는 뜻은 아니야.
- [한국천문연구원 특일 정보](https://www.data.go.kr/data/15012690/openapi.do): REST/XML, 인증키 필요. 공공기관 휴일 여부·명칭 제공, 무료·이용허락범위 제한 없음 표시. 실제 API 호출이나 키 발급은 하지 않았어.
- [Android ICU Calendar](https://developer.android.com/reference/android/icu/util/Calendar): 날짜 필드와 역법 계산 문서. 한국 공휴일 정책을 정확히 반환하는 내장 기능은 확인하지 못했어.

**권장:** 공식 자료를 대조한 정적 연도별 목록. 단 몇 년까지 지원할지와 연도별 갱신 부담은 제품 선택이므로, 사용자 지시서 13절의 지원 연도 STOP 경계에 따라 공휴일 구현만 멈췄어. 2026 우선 지원 또는 다음 해 포함 등은 미승인 후보야. 지원 연도 미확정, 현재 앱의 공휴일·대체공휴일·임시공휴일 지원 없음. 임시공휴일 추정, 새 dependency·API·권한·Room 변경 없음.

재개 조건: 지원 연도와 유지 방침 결정 → 해당 연도 최신 공식 목록 및 이후 변경 발표 대조 → 최소 날짜 판정과 색상 분리 구현. 정책을 자체 계산하거나 미래 정확성을 약속하지 않아.

현재 색상은 방문 채움+자동 대비가 요일색보다 우선하고 토요일 navy·일요일 red·평일 기본색이야. 추후 구현은 `방문 채움+자동 대비 > 공휴일/대체공휴일 red > 토요일 navy > 평일 기본색`과 일요일 red를 지켜. 토요일 공휴일·방문 공휴일·12월→1월 테스트가 필요해. 이번 공휴일 테스트는 로직 미구현으로 미실행이야.

## 귀여운 요소 검토

- 방문이 드문 달은 기존 문구와 카오모지로 충분한 여백을 유지해.
- 5~7일 연속은 라벨·얼굴이 이미 반복되므로 추가 반복 장식 필요성이 낮아.
- 방문이 많은 달은 청록 면적이 커져 여백 보존이 중요해.
- 공휴일 red가 늘어나는 상황은 navy/red/teal 정보가 늘어 작은 낙서도 시선을 분산할 수 있어.
- 현재 판단은 **추가 없음 권장**. 작은 소인·월별 낙서도 해결할 문제가 뚜렷하지 않아 후보로 채택하지 않았어.
- 최초에는 목업을 만들지 않았고, 이후 사용자 추가 지시로 아래 비교 목업을 제작했어. production 장식·애니메이션 추가는 없어.

### 74일차 추가 지시 — 비교 목업 완료

- 사용자 확인(2026-09-15): 목업과 실기기에서 2dp 디자인이 마음에 든다고 확인했고, 이어서 commit·push를 명시적으로 요청했어. **2dp 실기기 QA 통과와 Git 반영 승인 완료**로 기록해. 공휴일 방식·지원 연도나 추가 장식 승인으로 확대하지 않아.

- 방식: 앱 밖 독립 [HTML 목업](mockups/visit-calendar-2dp.html), [브라우저 캡처](mockups/visit-calendar-2dp.png). 로컬 HTML을 브라우저로 열면 돼. 앱 route·debug screen·dependency는 추가하지 않았어.
- 2026년 10월 한 달 전체를 3개 나란히 비교해. A는 10/1~7 연속 7일(주말 포함), B는 2·9·21일 3회, C는 16일 방문 샘플이야. 하단 횟수도 샘플 수치야.
- 기본 2dp, 이전 6dp로 비교 가능. 공휴일 가정을 끄면 현재 앱처럼 주말색만 보여. 목업에서만 10/3 개천절(토), 10/5 대체공휴일(월), 10/9 한글날(금)을 사용해. 위 공식 월력요항에서 확인한 샘플이며 연간 공휴일 production 구현은 아니야.
- B에서 토요일+공휴일 red와 대체공휴일 red, 방문한 공휴일 teal을 동시에 확인할 수 있어. A에서는 주말·공휴일에 방문 라벨이 우선해.
- 앱 토큰 색상, 304px 패널/256px 달력, 34px 셀, 3px 안쪽 여백, 글자 크기를 dp/sp 수치에 대응했어. 날짜 기반 epochDay mod 4와 4종 카오모지를 재현했어. Android 폰트·실제 dp 밀도와 브라우저 CSS 렌더링 차이는 남아 있어.
- 자동 점검: Node에서 실제 HTML script 실행해 3개 달력·총 26개 라벨·공휴일 전환 시 방문 라벨 수 유지·2/6px 전환 통과. 최초 점검 명령의 PowerShell 인용 오류는 stdin 전달로 수정했어(앱 코드 오류 아님).
- Chrome headless 캡처를 직접 열어 3개 월 전체, 라벨·색상·텍스트 배치를 확인했어. 첫 제한 환경 실행의 GPU 실패 후 독립 임시 프로필과 허용된 권한으로 캡처 성공. 사용자 브라우저 프로필은 사용하지 않았어.
- 내 시각 판단: A 연속 7일이 작은 종이 흔적이라는 의도를 가장 잘 보여줘. 라벨 사이 여백이 유지돼 한 덩어리로 붙지 않고, C의 16일도 뭉치지 않아 보여. 이는 브라우저 목업 의견이며 사용자 QA 승인은 아니야.
- 실제 filesDir·visit_record·방문 횟수·streak 접근 없음. 이번 추가 작업에서 production 수정 없음. 기존 2dp 한 줄 변경만 그대로 남아 있어.
- 추가 산출물은 `docs/ai/mockups/`의 HTML·PNG와 이 기록이야. HTML 정적 점검 및 `git diff --check` 통과. 앱 코드 변경이 없어 Gradle 재실행은 불필요, 직전 관련 16건·compile 통과 기록 유지. 실기기 2dp QA와 `49654c2` commit·push까지 완료했어.

### 74일차 추가 장식선 목업 승인 및 production 최소 구현

- 사용자 지정 조합을 같은 HTML 목업에 추가했어: 상단 `────── ✦ ──────`, 하단 `⋆｡────────｡⋆`.
- 첫 목업 확인 후 사용자가 양옆 선이 중간에서 끊겨 보인다고 피드백했어. 상·하단 선을 주차 구분선과 같은 달력 전체 폭으로 늘리고, 중앙 `✦`와 양끝 `⋆｡`·`｡⋆`만 작은 문자로 남겼어.
- 두 장식선은 기존 상·하단 구분선 자리를 대신해 달력 높이를 거의 늘리지 않아. 선 두께는 주차 구분선과 같은 0.5px이고, 본문 잉크보다 훨씬 낮은 대비(`InkSecondary` 계열을 약 29%로 표현)와 9px 장식 문자 크기를 사용했어.
- `장식선 보기`를 켜고 끌 수 있어 같은 3종 전체 달력에서 기존 가로선과 직접 비교 가능해. 2dp·공휴일 가정 비교 기능도 유지했어.
- Chrome headless로 선 길이 수정 후 PNG를 다시 만들고 직접 확인했어. 양쪽 여백까지 선이 이어져 기존보다 덜 끊겨 보이며, 상단 장식은 요일·날짜보다 뒤로 물러나고 하단 장식은 방문 횟수 문구보다도 조용하게 보여.
- HTML script 실행 점검: 상단 장식 3개, 하단 장식 3개, 전체 폭 선 조각 9개, 끄면 기존 구분선 6개, 방문 라벨 26개, 2px 반경 유지 모두 통과. `git diff --check` 통과.
- 사용자 확인(2026-09-15): 늘린 선 길이가 정확히 원한 지점이고 완벽하다고 명시했어. 이 승인으로 정적 프로토타입 단계를 통과했어.
- production은 `VisitCalendarDrawer.kt` 안에서 기존 상·하단 구분선 자리를 전용 장식 composable로 교체했어. 상단은 전체 폭 선 사이 중앙 `✦`, 하단은 양끝 `⋆｡`·`｡⋆` 사이 전체 폭 선이야. 선은 0.5dp, `InkSecondary` 29% 색, 문자는 9sp로 목업과 맞췄어.
- 장식 Row를 기존 구분선과 여백의 총높이에 맞추고 footer 위 여백을 조정해 달력 전체 밀도와 위치를 유지했어. 장식은 `clearAndSetSemantics`로 접근성 읽기에서 제외했어.
- 실제 방문 데이터·filesDir·visit_record·streak 접근 없음. route·dependency·저장 구조 변경 없음. 현재 변경은 `VisitCalendarDrawer.kt`, `docs/ai/mockups/visit-calendar-2dp.html`, 갱신된 PNG와 이 HANDOFF야.
- 상태: 목업 승인과 production 최소 구현 완료, production 실기기 QA 대기. 현재 변경은 commit·push하지 않았어.

### 74일차 하단 장식선 대비·문자 후보 목업

- 사용자 추가 지시(2026-09-15): 현재 `⋆｡────────｡⋆` 문자열은 유지하되 색 대비를 소폭 높인 안을 먼저 확인하고, 존재감이 부족하면 `୨୧────────୨୧`와 `୨୧ ──────── ୨୧` 후보를 비교하기로 했어. 대비를 높여도 별무늬 가독성이 약해 사용자가 `୨୧ ──────── ୨୧`를 선택했어.
- 같은 HTML 목업에 `하단 장식` 선택을 추가했어. 기본 선택은 기존 문자열 + 대비 상향(약 39%)이고, 기존 대비(약 29%)와 두 `୨୧` 후보를 같은 2026년 10월 A/B/C 전체 달력에서 비교할 수 있어.
- 상단 `✦`와 선 길이, 달력 크기, 2dp 라벨, 공휴일 가정은 유지했어. 실제 앱 코드·route·dependency·filesDir·visit_record·streak는 건드리지 않았어.
- Chrome headless로 기본 대비 상향 상태의 PNG를 갱신하고 3개 달력 전체를 직접 확인했어. 문자열은 양끝에 남고 선은 기존 승인 길이를 유지하며, 대비 상향안도 방문 라벨보다 먼저 시선을 끌 정도는 아니었어. 후보 선택별 최종 채택은 아직 사용자 확인 대기야.
- 사용자가 선택한 `୨୧ ──────── ୨୧`를 production 하단 장식에 반영했고, 하단만 대비를 39%로 올렸어. 상단 `✦`는 기존 29% 대비를 유지해 시선 우선순위를 보존했어. 목업 기본 선택도 선택안으로 맞췄어.
- 자동 검증: 단일 Gradle 프로세스로 `VisitCalendarTest`·`VisitHistoryStorageTest` 관련 테스트 통과, `:app:compileDebugKotlin` 성공. 병렬 실행 시 Kotlin incremental cache 잠금 환경 오류가 있었지만 중단 후 단일 실행으로 재검증했어.
- 상태: 선택안 production 반영 및 자동 검증 완료, 실기기 QA 대기. 현재 변경은 commit·push하지 않았어.

## 자동 검증·실행 환경

- 기존 로컬 Gradle 9.4.1 및 Android Studio JBR 사용. 저장소에 실행 wrapper가 없어 기존 로컬 `gradle.bat` 사용.
- 첫 offline 실행은 `settings.gradle.kts:8`의 기존 `foojay-resolver-convention:0.10.0`을 찾지 못해 설정 단계에서 실패. 앱 코드 오류가 아니야. build 설정을 수정하지 않았어.
- 기존 캐시·네트워크 접근 권한으로 재실행. `:app:testDebugUnitTest --tests com.postcardmemory.ui.gallery.VisitCalendarTest --tests com.postcardmemory.utils.VisitHistoryStorageTest :app:compileDebugKotlin --console=plain`.
- 장식선 production 반영 후 재검증 결과: `BUILD SUCCESSFUL in 2m 32s`. `VisitCalendarTest` 8건, `VisitHistoryStorageTest` 8건, 실패·에러 0을 XML 결과로 확인했고 `compileDebugKotlin`도 성공했어. 기존 코드의 deprecated API 등 warning만 있었어.
- 최종 `git diff --check` 통과. production diff는 장식선 전용 composable과 기존 구분선 교체 및 밀도 보정으로 한정했어. 기존 LF→CRLF 경고는 있어.
- 전체 unit test는 국소 시각 변경에 비례해 실행하지 않았어. 새 장식을 그대로 되풀이하는 테스트도 추가하지 않았어.
- 실기기 설치·실행·삭제·초기화·계측 테스트·사용자 데이터 조작은 실행하지 않았어.

## 실기기 QA·남은 위험·다음 행동

**2dp 실기기 QA는 완료했고, 새 장식선 production 실기기 QA는 대기 중이야.** 사용자가 목업의 전체 폭 선 길이와 인상을 승인한 뒤 실제 코드에 반영했어.

- 확인 대상은 `141d6cb` 위 2dp 작업트리였고, 확인된 결과를 `49654c2`에 commit했어.
- Gallery 좌측 메뉴 → 방문 달력. 여러 방문 라벨을 함께 보고 종이 조각 느낌인지, 너무 직각이거나 답답한지 확인해. 닫고 다시 열어 날짜·카오모지·횟수 유지도 확인해.
- 5~7일 연속 기록이 없다면 기록·기기 날짜를 조작하지 않고 자연적으로 쌓인 뒤 확인해. 그 조건은 미검증으로 남겨.
- 장식선 QA는 Gallery 좌측 메뉴에서 방문 달력을 열어 상·하단 선이 목업처럼 양옆까지 충분히 이어지는지, 본문·방문 라벨보다 먼저 보이지 않는지, 전체 높이와 footer 간격이 자연스러운지 확인하면 돼. 공휴일 QA는 미구현이라 해당 없어.
- history marker와 `visit_record.txt`, total/streak·하루 중복 방지, Intro·Gallery 기타 기능, Room·Migration·저장·공유 경로는 변경하지 않았어.
- 기존 위험 유지: history와 요약 저장은 단일 트랜잭션이 아니며 history 실패 시 그날 라벨이 없을 수 있어. 자정 너머 프로세스를 유지할 때 방문 집계 범위도 기존 그대로야. 이번에 이를 해결하거나 실기기에서 검증했다고 주장하지 않아.
- 과거 데이터 사고·복구 범위와 Intro 자연 도달 미검증은 archive 기록을 유지해. 코드 커밋을 사용자 데이터 백업으로 해석하지 않아.
- 다음 행동: 사용자가 장식선 production 실기기 화면을 확인해. 통과 후 별도 명시 요청이 있으면 현재 4개 tracked 파일만 commit·push할 수 있어. 공휴일은 지원 연도·유지 방침 결정 후 별도 재개 가능해.
- 전용 task checklist 도구가 없어 진행 메시지와 이 문서로 단계를 기록했어. Goal 대체나 하위 agent 위임 없음.
