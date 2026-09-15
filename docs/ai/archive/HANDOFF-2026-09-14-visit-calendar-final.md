# HANDOFF — 73일차 현재 상태

확인일: 2026-09-14. 수동 표준 모드, Codex / 패코 작업이야. 공용 작업판은 활성화하지 않았어.

## 현재 목표·승인·상태

기존 방문 집계를 유지하면서 적용 버전 실행일부터 실제 방문 날짜를 추가로 기록하고, 좌측 사이드바에 현재 월 달력을 보여주는 작업이야. (최초 승인 당시엔 방문 표시가 손그린 X 5종·월별 카오모지였지만, 아래 13·14번 후속 지시서로 X는 완전히 제거되고 방문 날짜마다 짧은 카오모지 스탬프로 대체됐어 — 최신 상태는 이 문단이 아니라 상태 표와 UI/UX 절을 기준으로 봐.)

- 날짜별 history가 없어 첫 조사에서 STOP했지만 사용자가 **A — 적용 이후 실제 방문 날짜부터 쌓기**를 승인했어.
- 사용자가 기존 `visit_record.txt`의 형식·위치·저장 코드는 유지하고 history를 옆에 추가하며 기존 history는 재작성·삭제하지 않도록 명시했어.
- 위치 질문에는 **예전에 제거한 좌측 사이드바로 분리**하라고 지시했어. 최초 작업지시서의 navigation 변경 비범위 중 이 최소 사이드바 복원만 새로 승인된 범위야. NavHost destination은 추가하지 않았어.
- 기존 카메라·미래 우체통·특별한 갤러리 햅틱 퀵 메뉴는 유지해. 달력 목록 하단 배치는 채택하지 않았어.
- **1차 실기기 QA 결과 반영(2차 지시서)**: 달력 전체 크기·위치, 월/요일/날짜 구성, 카오모지, drawer 비율은 만족 확인돼 더 손대지 않았어. 승인 범위는 ① 방문일 X 미감 개선(크기·굵기·비대칭), ② 하단에 `totalVisitDays` 기반 방문 횟수 문구 추가였어.
- **2차 실기기 QA 결과 반영(3차 지시서, "2차 수정 지시서"라는 이름으로 받음)**: 달력 전체 크기·drawer 폭·여백·월 구조·시각 문법·history 저장·방문 횟수 기능은 만족 확인돼 골격은 재설계하지 않았어. 승인 범위는 "출석체크"보다 "다녀간 날들의 작은 흔적"처럼 보이도록 표현만 바꾸는 것 — ① 상단에 "다녀간 날들" 부제 추가, ② 손그린 X 완전 제거, ③ 방문 날짜 아래 짧은 카오모지 스탬프(날짜 기반 deterministic selector), ④ 주말 색상(토=muted navy, 일=muted red, 공휴일은 데이터 없어 STOP), ⑤ 주차별 아주 옅은 가로선, ⑥ 하단 standalone 카오모지 제거, ⑦ 하단 문구를 "오늘까지 N번 만났어요~!"로 교체, ⑧ 하단 우측에 기존 봉투 아이콘(`Icons.Default.MailOutline`) 재사용한 `[봉투] × N` 카운터 추가.
- **3차 실기기 QA 결과 반영(4차 지시서, "3차 수정 지시서"라는 이름으로 받음)**: 달력 전체 크기·위치·drawer 비율, 기본 디자인은 만족 확인돼 골격을 유지했어. "X/카오모지 중심 표현은 방문일 강조력이 약하다"는 피드백에 따라 승인 범위는 ① 방문한 날짜를 `#16A7A1` 색으로 채워 1차 강조 수단으로 삼기(셀 전체를 채우는 무거운 사각형이 아니라 안쪽 여백이 있는 작은 라벨 블록), ② 채움 위 날짜 숫자·카오모지 글자색은 기존 텍스트 스티커의 명도 대비 판정(`labelStickerTextColorArgbFor`)을 재사용해 자동 결정, ③ 카오모지는 "방문했음을 설명하는 주인공"에서 "채움 안의 보조 스티커"로 역할 축소(표시 방식 자체는 유지), ④ 우선순위를 "방문일 채움+자동대비 > 비방문일 요일색"으로 재정의. 상단 부제·주말 색상·주차 구분선·하단 문구/봉투 카운터는 3차 그대로 유지했고, 공휴일 색상은 여전히 데이터 소스가 없어 STOP을 유지했어.
- **실기기 QA 전 미감 확인(4.5차)**: 사용자가 실기기 확인 전에 9/14~18 연속 방문 상태를 미리 보고 싶다고 해서, 앱 코드·persistence를 전혀 건드리지 않는 별도 HTML 정적 목업(Artifact, 리포지토리 바깥)으로 실제 색·크기·selector 값을 그대로 옮겨 보여줬어. 그 결과 `"^_^"`가 다른 후보와 선 느낌·폭이 달라 박스 안에서 혼자 따로 노는 인상이라는 피드백을 받아, 카오모지 후보를 5종→4종(`"^_^"` 제거)으로 줄였어. 채움 색·하단 문구·봉투 카운터·전체 레이아웃은 그대로 유지.

| 구분 | 상태 |
|---|---|
| 구현 | 완료 — history·사이드바·달력·주말 색상·주차 구분선·방문일 채움(`#16A7A1`)+자동 대비·카오모지 보조 스탬프(4종)·방문 횟수 문구·봉투 카운터. 공휴일 색상은 데이터 없어 STOP(§공휴일 STOP 참고) |
| 자동 검증 | 통과 — compile 및 관련 unit test, 전체 unit test(641건, 실패·에러 0), `git diff --check` (1~4차 + 카오모지 4종 조정까지 모두 재실행) |
| 사용자 QA | **완료** — 실기기 최종 확인 통과, 사용자가 commit/push 승인 |
| commit | **완료** — `02574f1` "Add a visit calendar sidebar with a date-stamped kaomoji and highlight fill" (10 files changed, 839 insertions(+), 43 deletions(-)) |
| push | **완료** — `feature/photo-sticker`에 push, 원격 `94c4ea4..02574f1` |

이번 73일차 작업은 사용자 QA·commit·push까지 전부 끝났어. 다음 세션은 새 작업으로 시작하면 돼.

## 저장 구조·파일 보호·실패 처리

- 기존 [VisitRecord.kt](../../app/src/main/java/com/postcardmemory/utils/VisitRecord.kt)의 `lastVisitEpochDay / totalVisitDays / currentStreakDays`, 버전 1 탭 구분 직렬화는 수정하지 않았어.
- 기존 [VisitRecordStorage.kt](../../app/src/main/java/com/postcardmemory/utils/VisitRecordStorage.kt)와 `ConfirmedEditStateStorage`, `AtomicFileReplace`도 수정하지 않았어. `visits/visit_record.txt`에 대한 정상 새날 집계는 기존 코드가 계속 수행해. history가 이 파일을 읽어 이관하거나 재작성·이동·삭제하지 않아.
- 새 [VisitHistoryStorage.kt](../../app/src/main/java/com/postcardmemory/utils/VisitHistoryStorage.kt)는 `filesDir/visits/history/<epochDay>.visit`에 내용 없는 marker 하나를 추가해. 날짜 자체는 파일명에 있고 파일 존재가 기록이야. epochDay는 기존과 같은 로컬 날짜 단위야.
- `Files.createFile`의 배타적 신규 생성만 사용해. 이미 존재하면 정규 파일인지 확인할 뿐 열어서 쓰지 않아. 같은 날짜 동시 생성도 기존 marker를 덮어쓰지 않아. 임시 파일·rename·덮어쓰기·삭제 경로가 없어.
- `loadMonth`는 요청 월의 28~31개 날짜에 해당하는 정규 marker 존재만 조회해. 임의 파일명·임시 파일·디렉터리는 방문일로 취급하지 않고 삭제도 하지 않아. 심볼릭 링크 자체는 정규 marker로 취급하지 않아.
- IO/권한 오류의 저장 실패는 false, 조회 권한 오류는 빈 집합으로 처리해. history 실패 때문에 기존 집계를 되돌리거나 앱 실행을 중단하지 않아. CancellationException을 일반 저장 실패로 잡지 않아.
- [MainActivity.kt](../../app/src/main/java/com/postcardmemory/MainActivity.kt)에서 기존 집계와 Intro 결과를 먼저 전달한 뒤 IO에서 history를 기록·조회해. 실제 이번 프로세스 실행 시각으로 관측한 날짜만 메모리에 두며, 기존 파일의 과거 날짜나 total/streak으로 소급 생성하지 않아.
- 같은 날 이미 total을 올린 사용자가 업데이트해도 history만 새로 생기고 total/streak은 다시 증가하지 않아. Activity 재생성 때는 기존 집계를 반복하지 않고 중단된 history를 재시도할 수 있어.
- Room/Entity/DAO/Migration, DataStore, 새 dependency, JSON/list 직렬화는 추가·변경하지 않았어.
- 실제 기기의 `visit_record.txt`나 DB는 조회·수정하지 않았어. 파일 보존 자동 검증은 JUnit 소유 임시 디렉터리 기준이고, 실기기 전체 데이터 복구·백업 보장을 뜻하지 않아.

## UI/UX 문법과 구현

- 역할: 행동·보상이 없는 방문 흔적 표시. 진입만 과거 좌측 메뉴 아이콘과 drawer 문법을 재사용해.
- 근거: `cacbc4c` 직전 Gallery의 `ModalNavigationDrawer`, 폭 304dp, `PaperSurface` 패널을 확인했어. 이번 사용자 지시로 방문 달력 전용 복원이 승인됐어. 옛 기능 목록·NavigationDrawerItem은 복원하지 않아.
- 신규 [VisitCalendarDrawer.kt](../../app/src/main/java/com/postcardmemory/ui/gallery/VisitCalendarDrawer.kt): 좌측 패널 안에 현재 월만 표시. 닫힌 상태에서는 drawer 가로 제스처를 꺼 기존 pager와 drag를 우선해. 닫기 아이콘·바깥 영역·뒤로 가기 및 열린 상태의 닫기 swipe를 사용해.
- 기존 `PaperSurface`, `InkPrimary`, `InkSecondary`, `PaperDivider`와 기본 폰트를 사용해. 각진 평면 패널, 얇은 선과 작은 숫자이며 새 테마·강한 그림자·큰 카드·오늘 badge·보상 문구가 없어.
- 기존 `calendarCellsFor(YearMonth)`를 재사용해. 기존 촬영일 캘린더의 엽서 데이터·월 이동·선택 동작은 가져오지 않아.
- **(폐기) 1차 QA 후 X 폴리시**: 손그린 X(Canvas drawPath, 5개 variant, `visitXVariant`)는 2차 QA 지시서로 완전히 제거했어. 관련 `Canvas`/`Path`/`Stroke`/`StrokeCap` import, `MarkerStroke`, `markerVariants`, `visitXVariant`도 파일에서 삭제했어.
- **방문 날짜 카오모지 스탬프(3차 신규 → 4차에서 역할 축소 → 4.5차에서 후보 4종으로 정리)**: X 대신 방문한 날짜 숫자 아래에 짧은 카오모지 1개를 표시해. 후보는 `VISIT_DAY_KAOMOJI = ["•ᴗ•", "˙ᵕ˙", "ᵔᴗᵔ", "ᵔ.ᵔ"]`(원래 5종 중 `"^_^"`는 다른 후보와 선 느낌·폭이 달라 박스 안에서 혼자 따로 노는 인상이라는 미감 확인 피드백으로 제거, 전부 4자 이하 기본 유니코드 문자). `visitDayKaomoji(date)`가 `floorMod(epochDay, VISIT_DAY_KAOMOJI.size)`(현재 4)로 날짜마다 고정된 후보를 골라(recomposition/재진입/재실행에도 항상 동일), 날짜 Box `Alignment.BottomCenter`에 8sp·`maxLines=1`·`softWrap=false`로 그려 옆 셀을 침범하지 않게 했어. 저장하는 variant 필드는 없고, 기존 `floorMod` selector 개념만 재사용했을 뿐 X 관련 코드와 무관해. **4차 지시서로 역할이 바뀌어**, 이제 방문 표시의 1차 수단은 아래 채움 색이고 카오모지는 채움 안에 붙는 보조 스티커야(표시 위치는 그대로).
- **방문일 채움 색(신규, 4차)**: 방문한 날짜 Box 안쪽에 `Modifier.fillMaxSize().padding(3.dp).background(VisitFillColor, RoundedCornerShape(6.dp))`로 여백이 있는 작은 라벨 블록을 그려. `VisitFillColor = Color(0xFF16A7A1)`(지시된 값 그대로, 무디게 낮추지 않음). 셀을 꽉 채우는 습관 트래커형 사각형이 아니라 3dp 여백을 둔 둥근 블록이라 존재감은 있지만 무겁지 않아. 실제 history에 기록된 날짜(`visitedEpochDays`)에만 그려지고, 과거 방문일 추정/생성은 하지 않아.
- **방문일 자동 대비 글자색(신규, 4차)**: 새 대비 로직을 만들지 않고 텍스트 스티커의 `labelStickerTextColorArgbFor(tapeColorArgb: Long)`([LabelStickerItem.kt](../../app/src/main/java/com/postcardmemory/ui/detail/LabelStickerItem.kt))를 그대로 import해 재사용했어 — "밝은 배경 → 어두운 글자 / 어두운 배경 → 밝은 글자" 판정(`labelStickerLuminance`, 임계값 0.6). `VisitFillContrastColor = Color(labelStickerTextColorArgbFor(0xFF16A7A1L))`을 방문일의 날짜 숫자·카오모지 색으로 함께 사용해. 우선순위는 "방문일 채움+자동 대비 > 비방문일 요일색" — 방문하지 않은 날짜는 기존 `visitDateColor(date)`(요일색) 규칙 그대로야.
- 미방문 날짜는 숫자만 표시하고 별도 placeholder를 두지 않아.
- **하단 standalone 카오모지(구 `visitKaomoji(month)`, `VISIT_KAOMOJI`)는 3차 지시서로 제거**했어. 카오모지 역할이 날짜별 스탬프로 옮겨갔기 때문이야.
- **상단 부제(신규, 3차)**: 월 제목(`2026년 9월`, 14sp) 아래 `"다녀간 날들"`(9sp, InkSecondary)을 한 줄 더 넣었어. badge/pill/배경 없이 텍스트만이고, header 높이가 과하게 커지지 않도록 이어지는 Spacer를 14dp→10dp로 줄였어.
- **주말 색상(3차 신규, 4차에서 우선순위 정리)**: 요일 헤더 글자색과, 방문하지 않은 날짜 숫자 색에 `visitDateColor(date)`를 적용해 토요일은 `SealInkNavy`, 일요일은 `SealInkRed`로 칠하고 평일은 기존 `InkSecondary`를 유지해. 새 원색을 추가하지 않고 이미 존재하던 도장 잉크 팔레트(`SealInkNavy`/`SealInkRed`, 웜톤 세계관에 맞는 차분한 톤)를 재사용했어. **4차 지시서로 우선순위가 명시**돼, 방문한 날짜는 요일색 대신 위 자동 대비 색을 쓰고(`if (visited) VisitFillContrastColor else visitDateColor(date)`), 요일 헤더 자체는 방문 여부와 무관하므로 그대로야.
- **공휴일 색상 — STOP**: 착수 전 `holiday`/`공휴일` 키워드로 `app/src/main` 전체를 조사했지만 기존 공휴일 판정 로직이나 데이터 소스가 없었어(신규 조사, 이번 세션). 공휴일/대체공휴일 지원에는 새 데이터 구조나 외부 API/라이브러리가 필요하므로 AGENTS.md 6절에 따라 이 부분만 STOP했고, 임의로 불완전한 국내 공휴일 목록을 만들어 "전체 지원"으로 간주하지 않았어. 나머지(주말 색상, 달력 미감)는 안전해서 계속 진행했어. 재개하려면 데이터 소스 선택(정적 연도별 목록 내장 vs 새 dependency vs 외부 API)에 대한 사용자 판단이 필요해.
- **주차별 옅은 가로선(신규, 3차)**: 각 주(week) `Row` 다음에(마지막 주 제외) `HorizontalDivider(thickness = .5.dp, color = PaperDivider.copy(alpha = 0.3f))`를 넣어 표/타임테이블처럼 보이지 않을 만큼 연하게 뒀어. 세로선·셀 테두리는 추가하지 않았고, 너비는 날짜 grid와 같은 폭(fillMaxWidth)이야.
- **방문 횟수 문구(3차 갱신)**: 하단 구분선 아래 왼쪽에 `"오늘까지 N번 만났어요~!"`(10sp/InkSecondary)를 표시해. `visitCountLabel(totalVisitDays: Int?)` 함수가 순수 문자열 포매팅만 하고 streak/보상 의미가 없도록 금지 문구(연속/출석/성공/streak/reward/achievement/축하)를 테스트로 확인해. `totalVisitDays`가 null이면 빈 문자열로 숨겨 "0번" 오표시를 막아. (2차 지시서의 `"N번째 방문이에요~!"` 문구는 3차 지시서로 이 문구로 교체됐어.) 새 persistence는 없고 기존 `TodayVisit.record.totalVisitDays`를 MainActivity → MainNavHost → GalleryScreen → VisitCalendarDrawer로 그대로 전달만 해.
- **하단 우측 봉투 카운터(신규, 3차)**: 같은 줄 오른쪽에 기존 `Icons.Default.MailOutline`(Gallery의 "미래 우체통" FAB에 이미 쓰이는 선형 벡터 아이콘, OS emoji 아님)과 `" × N"` 텍스트를 12dp/10sp로 작게 배치했어. `totalVisitDays`가 null이면 표시하지 않고, 아이콘+숫자 Row 전체에 `"총 N번 방문"` 접근성 설명 하나만 둬.
- 날짜 접근성 설명은 실제 기록이 있는 날짜에만 `방문 기록 있음`을 더해. 과거 빈칸을 미방문으로 단정하는 설명은 없어.
- Gallery 변경은 drawer wrapper·메뉴 아이콘·history 전달·`totalVisitDays` 전달뿐이야. 3열 기본, 사진 진입·검색·정렬·놀이모드·햅틱 퀵 메뉴 코드는 유지해.
- `GalleryViewSelectionStructureTest`의 과거 “drawer 완전 제거” 조건은 새 승인에 맞게 방문 drawer 존재와 옛 기능 메뉴 부재를 확인하도록 대체했어. `VisitCalendarDrawer(...)` 호출 문자열 assertion(`totalVisitDays` 인자 포함)은 3차에서도 시그니처가 그대로라 추가 변경이 없었어. 기존 퀵 메뉴 구조·햅틱·drag 회귀 검사는 유지해.

## 변경 파일

| 파일 | 이유 |
|---|---|
| `app/src/main/java/com/postcardmemory/utils/VisitHistoryStorage.kt` | 독립적인 날짜 marker 저장·월 조회 |
| `app/src/main/java/com/postcardmemory/MainActivity.kt` | 기존 방문 결과 뒤 history 처리·Gallery에 날짜 전달 |
| `app/src/main/java/com/postcardmemory/ui/gallery/VisitCalendarDrawer.kt` | 사이드바·현재 월 달력·부제·주말 색상·주차 구분선·방문일 채움 색(`#16A7A1`)+자동 대비·카오모지 보조 스탬프·방문 횟수 문구·봉투 카운터(X와 월별 standalone 카오모지는 3차에서 제거) |
| `app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt` | 좌측 달력 진입과 drawer 연결 |
| `app/src/test/java/com/postcardmemory/utils/VisitHistoryStorageTest.kt` | 기존 bytes/mtime 보존·동시 생성·부분 실패·업데이트 당일 검증 |
| `app/src/test/java/com/postcardmemory/ui/gallery/VisitCalendarTest.kt` | 윤년·월 길이·연도 경계, 방문 횟수 문구 금지어 검증, 카오모지 스탬프 selector 안정성·셀 폭 보호, 주말 색상, 방문일 채움 색·자동 대비 재사용 검증 |
| `app/src/test/java/com/postcardmemory/ui/gallery/GalleryViewSelectionStructureTest.kt` | 새 승인과 일치하는 사이드바 회귀 조건 |
| `docs/ai/HANDOFF.md` | 현재 상태와 QA·위험 기록 |
| `docs/ai/archive/HANDOFF-2026-09-13-harness.md` | 직전 하네스 인수인계 원문 보존 |
| `docs/ai/archive/HANDOFF-2026-09-14-history-investigation.md` | A 승인 전 history 부재 조사·STOP 원문 보존 |

## 자동 검증과 실행환경

- 로컬 Gradle 9.4.1과 Android Studio JBR 21.0.10 사용. 저장소에는 gradlew.bat가 없어 기존 로컬 distribution의 gradle.bat로 실행했어.
- **1차 구현(history/drawer/달력/X/카오모지) 검증**: `:app:compileDebugKotlin` BUILD SUCCESSFUL, 관련 필터 65개 테스트 전부 통과, 전체 unit test 636개 전부 통과, `git diff --check` 공백 오류 없음.
- **2차 폴리시(X 크기·두께·비대칭, 방문 횟수 문구 `"N번째 방문이에요~!"`) 검증**: `:app:compileDebugKotlin` BUILD SUCCESSFUL. 전체 `:app:testDebugUnitTest` 637개 전부 통과(1차 636 + 신규 1), `git diff --check` 공백 오류 없음.
- **3차 폴리시(달력 재구성 — 부제/X 제거/카오모지 스탬프/주말 색상/주차 구분선/하단 문구 교체/봉투 카운터) 검증**: `:app:compileDebugKotlin` BUILD SUCCESSFUL(기존 경고 외 신규 오류 없음). 관련 필터(VisitCalendarTest 5건, GalleryViewSelectionStructureTest 12건, VisitHistoryStorageTest 8건, GalleryCalendarCellsTest 5건) 30건 전부 통과. 전체 `:app:testDebugUnitTest` 638개 전부 통과(2차 637 − X/월별카오모지 테스트 2건 제거 + 카오모지스탬프·셀폭·주말색상 테스트 3건 추가 = 638), 실패·에러 0 — 회귀 없음. `git diff --check` 공백 오류 없음.
- **4차 폴리시(방문일 채움 `#16A7A1` + 텍스트 스티커 대비 재사용, 카오모지 보조화) 검증**: `:app:compileDebugKotlin` BUILD SUCCESSFUL(기존 경고 외 신규 오류 없음). 관련 필터(VisitCalendarTest 7건 — 채움 색·대비 재사용 테스트 2건 추가, GalleryViewSelectionStructureTest 12건, VisitHistoryStorageTest 8건, GalleryCalendarCellsTest 5건) 32건 전부 통과. 전체 `:app:testDebugUnitTest` 640개 전부 통과(3차 638 + 신규 2), 실패·에러 0 — 회귀 없음. `git diff --check` 공백 오류 없음.
- **4.5차(카오모지 후보 5종→4종, `"^_^"` 제거) 검증**: 실기기 확인 전 별도 HTML 목업(Artifact, 리포지토리 밖)으로 미감을 먼저 확인한 뒤 받은 피드백 반영. `VISIT_DAY_KAOMOJI` 리스트만 수정했고 `visitDayKaomoji`의 `floorMod` selector는 `VISIT_DAY_KAOMOJI.size`를 그대로 참조해 자동으로 mod 4가 되므로 다른 코드 변경은 없었어. `:app:compileDebugKotlin` BUILD SUCCESSFUL. 관련 필터(VisitCalendarTest **8건** — 후보 4종·`"^_^"` 부재 확인 테스트 1건 추가) 전부 통과. 전체 `:app:testDebugUnitTest` **641개** 전부 통과(4차 640 + 신규 1), 실패·에러 0 — 회귀 없음. `git diff --check`(신규 파일 `git add -N`로 포함 후 실행, 확인 뒤 즉시 `git reset`으로 staging 원복) 공백 오류 없음, LF→CRLF 변환 경고만 존재(기존 저장소 autocrlf 설정, 무해).
- 4차 신규 테스트는 ① `VisitFillColor`가 지시된 `Color(0xFF16A7A1)`과 정확히 같고 완전 불투명(`alpha == 1f`)인지, ② `VisitFillContrastColor`가 `labelStickerTextColorArgbFor(0xFF16A7A1L)`을 실제로 호출한 결과와 같은지(새 로직을 따로 만들지 않았는지) 확인해. 4.5차 신규 테스트는 후보가 정확히 4개이고 `"^_^"`가 더 이상 없는지, 40일 범위에서 4종 전부가 실제로 등장하는지 확인해.
- `visitCountLabel(totalVisitDays: Int?)` 테스트는 금지 문구(연속/출석/성공/streak/reward/achievement/축하) 미포함도 함께 확인해. `VISIT_DAY_KAOMOJI` 후보는 전부 4자 이하·줄바꿈 없음을 테스트로 고정해 좁은 날짜 cell 폭 보호를 자동 검증 수준에서 뒷받침해(실제 렌더 폭은 실기기 QA 대상).
- 새 저장 테스트의 삭제·손상 fixture는 JUnit TemporaryFolder 안에서만 조작해. production history에는 삭제가 없고, 기기·사용자 저장 파일에 테스트하지 않아.
- connected/계측/test APK lifecycle/설치·제거/데이터 초기화/기기 시계 변경은 실행하지 않았어.
- 과거 625 tests는 baseline 기록이며 이번 641 결과가 최신 전체 unit test 기준이야.

## 실기기 QA — 구현 빌드 확인 후 사용자 수동 확인

대상: `feature/photo-sticker`, `94c4ea4` 위 이번 미커밋 작업트리(1~4차 + 카오모지 4종 조정까지 모두 포함)의 일반 앱 빌드. AI는 기기에 설치·실행하지 않았어. 기존 앱을 지우거나 데이터를 초기화하지 않고 평소 일반 업데이트 방식으로 확인해.

**1~3차 QA 결과(완료, 재확인 불필요)**: 달력 전체 크기·위치·drawer 폭/비율·여백, 기본 디자인, 현재 월/요일/날짜 구성, history 저장, 방문 횟수 기능은 만족 확인됨. (X는 3차에서 완전히 제거됐고, 3차의 "X/카오모지 중심 표현이 방문일 강조력이 약하다"는 피드백을 받아 4차에서 채움 색으로 바꿨으니 재확인 대상이 아니야.)

**미감 확인(실기기 아님)**: 실기기 QA 전에 사용자가 별도 HTML 목업으로 9/14~18 연속 방문 상태의 채움+카오모지 조합을 먼저 봤고, 방향은 유지하되 `"^_^"`만 제거하기로 확정했어. 이 목업은 리포지토리 밖의 정적 페이지라 앱 코드·검증 결과와는 무관하고, 실기기 QA를 대체하지 않아.

**4차 QA — 이번에 새로 확인할 항목**:

1. 방문한 날짜가 `#16A7A1` 색으로 한눈에 잘 보이는지
2. 색 채움이 과하게 두껍거나 습관 트래커/출석앱처럼 보이지 않는지
3. 방문일 내부 날짜 숫자와 카오모지가 충분히 읽히는지(자동 대비 색이 실제로 잘 작동하는지)
4. 카오모지가 방문일 안에서 "보조 스티커"처럼 자연스럽게 보이는지(더 이상 주인공처럼 보이지 않아도 되는지)
5. 토요일 muted navy / 일요일 muted red가 전체 톤과 어울리는지(비방문일 기준)
6. (공휴일 색상은 데이터 소스 없어 이번 범위에서도 STOP — 확인 대상 아님)
7. 주차 가로선이 너무 진하지 않은지
8. 여러 방문일이 있을 때 화면이 복잡해 보이지 않는지
9. "오늘까지 N번 만났어요~!"와 우측 [봉투] × N이 여전히 자연스러운지
10. 전체적으로 여전히 심플하고 조용한 느낌인지(출석 보상 UI처럼 보이지 않는지)

추가로 달력을 닫았다 다시 열거나 같은 날 앱을 재실행해도 같은 날짜의 채움·카오모지·하단 숫자가 유지되는지, 닫기 아이콘·바깥 탭·뒤로 가기로 패널이 닫히고 이후 기존 갤러리 swipe·사진 탭·퀵 메뉴·카메라/미래 우체통/특별한 갤러리 진입이 정상인지도 함께 봐줘.

자동 JVM 테스트는 Android 실제 화면·폰트·제스처·실기기 파일시스템을 대신하지 못해. 채움 색·자동 대비·카오모지의 실제 여러 날짜 모습과 다음 날 추가 기록은 자연스럽게 해당 날짜에 확인해. 테스트를 위해 날짜·방문 기록을 조작하지 않아. 실제로 보인 국소 문제만 다음 폴리시 대상으로 삼아.

## 남은 위험·미검증

- history와 기존 요약은 하나의 트랜잭션이 아니야. history 실패 시 total은 정상 증가해도 그날의 카오모지 스탬프는 없을 수 있고 같은 날 재실행/Activity 재생성에서 재시도해. 다음 날에는 실패했던 과거 방문일을 추정해 채우지 않아.
- 앱 종료·전원 손실·디스크 고장까지 포함한 최신 marker의 영구 보존을 보장하지 않아. 기존 파일을 덮어쓰지 않는 구조와 별개야. 파일 삭제 API나 자동 복구는 추가하지 않았어.
- 방문 관측 시점은 기존처럼 프로세스 최초 방문 판정이야. 앱 프로세스를 자정 너머 계속 유지한 경우 foreground 복귀마다 새 날짜를 집계하도록 확대하지 않았어. 날짜가 바뀌는 자연 실행 조건에서 확인해야 해.
- 실기기 시각/상호작용 QA는 아직 없어. 기존 Intro 33번째 회전·나머지 milestone 자연 도달 QA도 미검증을 유지해.
- 새 dependency·Room migration·기존 데이터 migration·사용자 데이터 조작·추정 backfill은 없어.
- **공휴일/대체공휴일 색상 STOP**: 프로젝트에 공휴일 판정 로직·데이터가 없어 3차·4차 모두 이 범위에서 제외했어. 재개하려면 정적 연도별 데이터 내장/새 dependency/외부 API 중 방식을 사용자가 먼저 선택해야 해.
- 짧은 카오모지 후보(`VISIT_DAY_KAOMOJI`)와 `#16A7A1` 채움 위 자동 대비 글자색의 실제 Android 폰트·화면 렌더링은 자동 테스트로 확인 불가 — 4차 실기기 QA 항목 1·2·3·4에서 확인 대상.
- `VisitFillColor`/`VisitFillContrastColor`는 [LabelStickerItem.kt](../../app/src/main/java/com/postcardmemory/ui/detail/LabelStickerItem.kt)의 텍스트 스티커 대비 상수·함수에 의존해. 그 파일의 임계값(`LABEL_STICKER_LIGHT_TAPE_LUMINANCE_THRESHOLD`)이나 텍스트 색 상수가 향후 바뀌면 방문 달력의 대비 색도 같이 바뀐다 — 의도된 재사용이지만 독립적인 색상 시스템이 아니라는 점은 기록해 둬.

## Git·기존 위험·다음 행동

- 시작 branch/HEAD: `feature/photo-sticker` / `94c4ea428d2e15b472ec97a14aefa3994278d55b`. 종료 HEAD: `02574f1`(원격 push 완료, 로컬=원격 동기화).
- 이번 작업으로 stage·commit한 파일: `MainActivity.kt`, `GalleryScreen.kt`, `GalleryViewSelectionStructureTest.kt`, `HANDOFF.md`(수정) + `VisitCalendarDrawer.kt`, `VisitHistoryStorage.kt`, `VisitCalendarTest.kt`, `VisitHistoryStorageTest.kt`, `archive/HANDOFF-2026-09-13-harness.md`, `archive/HANDOFF-2026-09-14-history-investigation.md`(신규). 기존 untracked `.codex-config.candidate.toml`, `.kotlin/`는 이번 커밋에 포함하지 않고 그대로 보존했어.
- 최근 commit: `02574f1` 이번 방문 달력 기능(신규), `94c4ea4` checkpoint 설명, `1f1ddc2` 하네스 정리, `c5959d8` milestone 5종/33번째 회전.
- 기존 사고·복구 원문은 [2026-09-12까지 이력](archive/HANDOFF-through-2026-09-12.md), 직전 상태는 [2026-09-13 원문](archive/HANDOFF-2026-09-13-harness.md), 이번 최초 STOP은 [73일차 조사 원문](archive/HANDOFF-2026-09-14-history-investigation.md)에 있어. archive 상대 링크는 당시 HANDOFF 위치 기준이야. 사고 후 엽서 1개 보존을 사고 전 전체 복구로 해석하지 않아.
- 전용 단계 추적 도구가 노출되지 않아 진행 메시지와 이 문서로 단계를 기록했어. Goal 대체·위임 agent는 사용하지 않았어.
- 다음 후보(미승인, 사용자 판단 필요): 공휴일/대체공휴일 색상은 데이터 소스가 없어 STOP 상태로 남아 있어 — 정적 연도별 데이터 내장/새 dependency/외부 API 중 방식을 먼저 정해야 재개할 수 있어. 그 외 새로운 기능·추측 폴리시는 실행하지 않아.
