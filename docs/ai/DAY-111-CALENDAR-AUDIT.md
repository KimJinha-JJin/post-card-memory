# 111일차 2차 — 방문 달력 구조 감사 · 봉투형 비교 목업

작업일: 2026-10-22 / 담당: Codex / 기준 HEAD: `4b865df` / 수동 표준 모드.

**최신 상태: 봉투 UI 구현·로컬 검증·SM-S936N 데이터 보존 설치·앱 실행·사용자 QA 정상 확인 완료. 최종 Git 마감 승인 완료. 실제 commit/push/CI 결과는 Git/Actions와 최종 완료보고로 확인해.** 아래 `device-qa`가 현재 기기 기록, `approved-implementation`이 구현 결과야. 최초 구조 감사·시안·재설계 기록은 보존 이력이며, 당시의 '앱 무변경/미승인/미실행'은 현재 상태를 뜻하지 않아. [독립 목업](mockups/visit-calendar-envelope-mockup.html)은 승인된 모습 그대로 보존했어.

<a id="device-qa"></a>

## 111일차 실기기 QA — 설치·실행·달력/통·재사용 정상 확인 완료

- **시작 상태:** `feature/photo-sticker`, HEAD `4b865dfe55fb787e12625fdfb23ecee815fdc13c`. 이전 production/test/문서 변경과 신규 PNG/목업을 보존했고, 이번 QA 시작 때 코드 수정·테스트 재실행·재빌드는 하지 않았어. 같은 debug APK(53,619,675B, SHA-256 `bac760fe9466453fc318fe83f8b6a6713e8264f045f2f61af1641b2b49eb98fd`)와 기존992/992 XML 결과를 확인했어.
- **기기:** `R3KYB00HAYY`, 실제 모델 `SM-S936N`,1080×2340/450dpi, font_scale0.9,SDK37. 이 값은 이번 읽기 실측이야.
- **설치 조건:** 기존 `com.postcardmemory` debug 앱은 설치 전 실행 중이 아니었어. 기존 설치 APK를 기기에서 읽어 PC Temp에만 복사하고 새 APK와 apksigner 인증서를 비교해 SHA-256 `cec335400357d8165cc91246adfaa38a9342db90c7ef3f0369da3b73c7c875b9` 일치를 확인했어. 기기 안의 기존 APK는 삭제·정리하지 않았어.
- **설치:** 승인된 serial에 `install -r`만 실행해 `Success`. `lastUpdateTime=2026-10-22 20:26:36`. `firstInstallTime=2026-10-05 14:19:35`,UID10525 유지. uninstall/pm clear/다운로드 정리/instrumentation 실행 없음.
- **보존 근거:** 앱 실행 전후가 아니라 **설치 직전/설치 직후, 아직 앱을 열기 전**의 비교야. `files/databases/shared_prefs` 안의74개 파일,총30,771,932B에서 경로·크기·mtime 목록이 완전히 같고,DB 폴더5개 파일의 SHA-256도 모두 같았어. 사용자 사진 등 모든 파일 내용의 전체 hash를 조사한 것은 아니야. 원본 비교 기록은 PC Temp의 `pcm-day111-before-20261022-2025.json`/`pcm-day111-after-install-20261022-2025.json`에만 두었고 저장소에 사용자 파일 목록을 추가하지 않았어.
- **실행/로그:** 설치 직후에는 앱이 실행되지 않았고, 이후 사용자가 직접 열었어. 20:31 KST 관찰에서 PID13246·`com.postcardmemory/.MainActivity` foreground를 확인했어. 재사용 QA 뒤 20:33:39 마지막 관찰도 같은 PID가 유지됐고, 20:25:18 이후 crash buffer와 해당 PID 로그에서 FATAL/ANR/OOM/AndroidRuntime·Exception/Error에 해당하는 오류를 찾지 못했어. 이는 관찰 구간의 근거이며 모든 미래 조작의 무오류 보장은 아니야.
- **달력 사용자 확인:** 사용자가 **'달력 항목 모두 정상'**이라고 답했어.질문 범위는 연도·월4×4 봉투,상단 숫자 가독성과 기존 글꼴,청록 현재/진한 선택,선택창 원 제거,실제 날짜의 오늘 원·민트 출석,월·연도 이동이야.
- **스티커 통 사용자 확인:** 사용자가 **'스티커 통 항목 모두 정상'**이라고 답했어.이번 질문은 기존 조각을 삭제/저장하지 않는 범위로,월/연도 선택 중 진입·복귀,목록·원본/누끼 표시,선택/해제,이전 선택창 상태와 반복 복귀,흔들림 자연스러움이야.
- **재사용 확인:** 기존 엽서를 수정하지 않고 새 QA용 엽서 한 장에서 기존 통 조각 꺼내기→이동·크기·회전→저장·재진입을 사용자가 직접 확인했어. 답변은 **'새 QA 엽서에서 재사용·저장·재진입 정상'**이야. 기존 통 조각/엽서 삭제는 요청하거나 실행하지 않았어.
- **QA 판정:** 달력 외형/선택/이동, 통 표시/선택/복귀/흔들림, 새 QA 엽서 재사용까지 질문별 정상 응답을 받았어. 발견된 문제와 추가 production 수정은 없어. 이 사용자 확인은 Git commit/push 승인을 뜻하지 않아.
- **검증 상태:** 로컬 자동검증은 앞선 관련99/전체992/빌드/계측컴파일 성공 근거를 재확인했어. 코드 변경이 없어 재실행하지 않았어. 실기기 instrumentation·신규 CI·stage·commit·push 미실행. 60개 파일 채우기/대용량 시험은 하지 않았어.
- **남은 범위:** 이번 SM-S936N·450dpi·font_scale0.9와 실제 조작 범위의 결과야. 다른 기기/큰 글꼴·장시간 메모리·저장 실패/취소의 기기 재현은 별도 미검증이고, 삭제·60개 경계 시험은 이번 범위에서 제외했어. 데이터 보존의 파일/DB 비교는 설치 직후·앱 실행 전 시점이며, 이후 사용자가 새 QA 엽서를 저장한 정상 데이터 변경과 혼동하지 않아.
- **HANDOFF/Git 마감:** QA 중에는 이전 보존 지시에 따라 HANDOFF를 수정하지 않았어. 이후 사용자가 111일차 최종 마감에서 추가 갱신·commit·push·CI 전체를 명시 승인했어. 기존 HANDOFF 원문을 바이트 그대로 남기고 최종 구현/검증/QA/후속 과제를 앞에 추가했어. 이 기록을 포함한 commit과 해당 SHA의 CI 결과는 최종 완료보고에서 확인해.

---

<a id="approved-implementation"></a>

## 111일차 승인 구현 — 앱 반영과 로컬 검증 (2026-10-22)

### 구현한 것과 보존한 것

- `VisitCalendarDrawer.kt`: 연도/월 picker에 공통 `visit_calendar_envelope_base.png` 한 장을 재사용해. 원본 전체를 사방4dp 안의56×32dp 범위에 Fit하고, 선택은 RGB0.94/alpha유지 필터로 표현해. 화면 단계·월 전환 바깥에서 불러와 칸마다 decode하거나 bitmap을 구워내지 않아.
- 숫자는 별도 fontFamily 없이 기존 앱 글꼴·연도12sp/월13sp·보통/Medium 굵기를 유지해. 상단6dp/lineHeight14sp 배치로 기본 글꼴 배율에서 중심y≈13dp를 목표로 했어. 실제 Android 글자 metrics·큰 글꼴은 사용자 QA 대상이야.
- picker의 현재 숫자 뒤 원만 제거해. 기존 청록 현재·진한 선택과 선택 우선순위,4×4 배열·40dp 셀·전체 클릭 영역·callback·월/연도 이동·뒤로가기는 유지해. 봉투 열기/닫기 애니메이션은 추가하지 않았어.
- 실제 날짜의 오늘 펜 원·민트 출석·날짜 계산·방문 기록·장 넘김·보관함 진입/복귀는 수정하지 않았어. `VisitCalendarMonthGrid`, `VisitCalendarMonthPage`, `VisitCalendarDrawer`, 두 잉크 우선순위 함수는 HEAD와 본문 일치도 확인했어.
- 파우치는 승인된 A안 코드 그대로야. `StickerStashPouch.kt` 및 통 저장·재사용·삭제 production 무변경. 기존 문서·목업·보호 자산을 초기화하지 않았어. Room/schema/Migration·dependency·Gradle 설정 변경 없음.

### 봉투 PNG와 비용

- 승인 목업 내장 원본1659×948을 **252×144 RGBA nodpi,35,078B**로 축소했어. 전체 캔버스·7:4비율·원점·투명 여백 유지. 분리된 바깥 alpha 잔점8px만 제거했고,새 그림 생성·crop·trim·배경 합성은 하지 않았어.
- alpha0~255,바깥 네 변은 모두0. alpha>0 bbox `(4,9)..(247,134)`,투명5,544px/부분alpha29,870px. 원본 종이 내부의 약한 반투명성도 유지했어. 소스 PNG는 직접 이미지로 확인했고,APK 안에도252×144 RGBA 한 장/35,078B로 들어간 것을 확인했어.
- ARGB_8888 decode 추정 **145,152B,약0.138MiB**. 16개 칸·연도/월에 곱해서 별도 bitmap을 생성하지 않아. GPU·리소스 캐시·실제 프로세스 메모리는 미측정이야.
- 자산 SHA-256: `2e4f87aa899827039878e4adad2bdfe66758db23fcf5236e1309d547a852ae58`. 자세한 사용처는 IMAGE-ASSET-MAP의111일차 절에 기록했어.

### 자동 검증 결과

| 영역 | 결과 |
|---|---|
| 관련 JVM | 실행·통과. 달력55+구조13+통 wiring11+통 저장13+재사용 조준7 = **99건**, 실패/오류/skip0. |
| 전체 JVM | 실행·통과. **108 XML / 992 tests**, 실패/오류/skip0. 소스 `@Test`992개/107파일+helper1개. |
| debug APK | `assembleDebug` 성공. `app/build/outputs/apk/debug/app-debug.apk`,53,619,675B,2026-10-22 19:02:14 생성. |
| instrumentation | `assembleDebugAndroidTest` 성공.21건/8파일 **컴파일** 근거이며 실제 실행은0회. CONDITIONAL/FORBIDDEN은 실행 불가 유지. |
| 보호 의미 | 낡은 자투리/crop/원 테스트3건을 새 Fit3건+실제PNG alpha/여백/크기2건으로 교체(순증2). 구조13건 중1건을 공통자산·터치·기존글꼴·원제거·잉크·재생성방지로 갱신. 날짜 달력의 오늘/출석 검사는 그대로야. TEST-COVERAGE-MAP 갱신 완료. |
| 환경 | 로컬 Gradle9.4.1/JBR21. Wrapper 실행 파일 부재와 격리 환경의 기존 foojay 해석 실패는 사용자 환경 권한 실행으로 해소했어. 설정/dependency를 수정하지 않았어. |
| CI/Git | 신규 CI·stage·commit·push 미실행. 현재 branch `feature/photo-sticker`,HEAD `4b865df`. 실기기 사용자 확인 전 Git 마감하지 않아. |

APK SHA-256은 `bac760fe9466453fc318fe83f8b6a6713e8264f045f2f61af1641b2b49eb98fd`야. debug 서명 SHA-256은 `cec335400357d8165cc91246adfaa38a9342db90c7ef3f0369da3b73c7c875b9`. 이 기록은 기존 기기의 서명과 일치했다는 뜻이 아니야. 아직 연결이 없어 기존 설치를 읽지 못했어.

### 구현 마감 당시의 실기기 단계·QA 계획 — 설치 결과는 위 device-qa 참조

대상은 승인된 SM-S936N이야. 일반/사용자 환경 권한 실행의 `adb devices -l` 모두 목록이 비어 있어 **설치 미실행·기기 QA 대기**야. 연결 뒤 serial/model·기존 package·설치 시각·서명을 확인하고,조건이 맞을 때 승인된 `adb -s <serial> install -r <debug APK>`만 실행해. 서명/설치 충돌이면 그 상태에서 멈추고 앱 제거·초기화로 우회하지 않아. 기존 SAFE instrumentation을 자동 실행하지 않아.

사용자 화면 QA는 다음 순서야. 자동검증 통과와 별도로 실제 결과를 받아야 해.

1. 갤러리→사이드바→월/연도 선택: 봉투 실제 크기·상단 숫자·청록 현재·진한 선택·현재 원 제거·4×4·클릭·범위 이동 확인. 실제 날짜 달력은 기존 오늘 펜 원·민트 출석 그대로인지 확인.
2. 월/연도 선택 도중 파우치→보관함→복귀: 보던 월·선택창 연도/단계 유지,뒤로가기·사이드바 재열기 확인.
3. 파우치 최근3개·원본/누끼·가림·움직임·보관함 선택·스크롤 확인. 기존 조각은 표시·선택만 보고 삭제하지 않아.
4. 보관·다른 엽서 재사용·통에서 빼기는 사용자가 새로 만든 QA용 조각/엽서로만 확인해. 기존 엽서·스티커를 삭제하는 시험은 하지 않아. 삭제 승인이 불명확한 대상은 실행하지 않아.
5. 빈 통은 기존 통이 실제 비어 있을 때만 확인해. 빈 상태를 만들려고 기존 조각을 지우지 않아. 다수/작은 화면/큰 글꼴도 이번 실제 조건에서 관찰 가능한 범위만 확인하고 나머지는 미검증으로 남겨.

기기 설치·실사용 QA·현재 density/프로세스 메모리는 아직 검증 결과가 없어. 연결 후 진행할 안전한 단계만 남아 있고,구현을 더 바꾸거나 목업을 추가할 단계는 아니야. 기존 HANDOFF는 앞선 사용자 보존 지시에 따라 추가 수정하지 않았으며,이 절과 최종 보고가 이번 구현/검증의 현재 기록이야.

---

## UI/UX 사전 판단

연도·월 칸은 **달력 탐색을 위한 기간 선택**이야. 지금 쓰는 4×4 배열·선택/현재의 구분·짧은 계층 전환은 보존 대상이야. 종이 자투리 배경은 이번 사용자가 바꾸려는 표현이므로 신규 봉투의 바깥 배경으로 중첩하지 않아. 봉투 자체에 숫자를 인쇄한 것처럼 구성해. 새 계층·선택 방식·toolbar·애니메이션은 제안하지 않아.

현재 코드와 110일차 HANDOFF가 근거야. 이번 실기기 스크린샷은 없고 HTML의 '현재'는 코드 기준 재현이지 실제 앱 캡처가 아니야. 최신 DECISIONS에는 이 달력의 새로운 봉투 외형을 확정한 기록이 없어. 외형은 이번 목업의 승인 대상이야.

## 파일·함수와 선택 경로

아래 별도 표기가 없는 함수는 [VisitCalendarDrawer.kt](../../app/src/main/java/com/postcardmemory/ui/gallery/VisitCalendarDrawer.kt)에 있어.

| 영역 | 함수·상태 | 조사 결과 |
|---|---|---|
| 단계 | `VisitCalendarNavLevel`, `MonthlyVisitCalendar`, `VisitCalendarDrawer` | `CALENDAR → MONTH_PICKER → YEAR_PICKER`. 달력 제목 탭은 월 선택으로, 월 선택의 연도 제목 탭은 연도 선택으로 가. |
| 연도 목록 | `decadeStartFor`, `yearPickerGridYears`, `VisitCalendarYearPicker` | 해당 decade 앞 2년+10년+뒤 4년. 2020 decade면 2018~2033, 4열×4행. 10개만 또는 12개로 줄이지 않아. |
| 연도 선택 | `highlightedYearFor`, `visitCalendarYearCellEmphasisFor` | 선택 강조는 실제 `displayedMonth.year`가 decade 안에 있을 때만. 탭 → `pickerYear=year` → `MONTH_PICKER`. 연도만 고를 때 실제 달력 월은 바뀌지 않아. |
| 월 목록 | `monthPickerGridCells`, `VisitCalendarMonthPicker` | `pickerYear`의 1~12월+다음 해 1~4월, 16칸. 다음 해 칸도 실제 선택 가능해. |
| 월 선택 | `visitCalendarMonthCellEmphasisFor`, `onMonthSelected` | 연도와 월을 함께 전달 → `displayedMonth=YearMonth.of(year, month)` → `CALENDAR`. |
| 배열·터치 | 두 Picker의 `Box` | drawer 304dp−좌우 24dp=256dp 내용 폭. 셀 64×40dp, 셀 전체 clickable. 자투리는 사방 4dp 안쪽이므로 기본 56×32dp. 연도 12sp, 월 13sp. |
| 종이 자투리 | `visitCalendarPickerPaperScrap`, `visitCalendarPickerScrapCrop`, 각 `ScrapSeed` | 기존 달력 PNG의 아래 55%부터 seed별 crop, `ImageShader`와 손으로 자른 외곽·옅은 그림자를 각 칸 뒤에 그려. 별도 Card가 실제 생성되는 건 아니야. |
| 현재 기간 | `isCurrentMonthCell`, `isCurrentYearCell`, `visitCalendarShowsCurrentMarker`, `VisitCalendarCurrentPeriodMarker` | 선택과 실제 오늘은 별개. 현재는 진청록과 26dp 연한 원, 선택과 겹치면 원 생략. 월 선택에서 올해 다른 월은 연한 청록, 인접 연도는 연한 잉크. |
| 범위 이동 | `VisitCalendarPickerHeaderRow`, `rememberVisitCalendarPickerSwipeModifier`, `visitCalendarSwipeStepFor` | 월 선택의 ▲/▼와 세로 swipe는 ±1년, 연도 선택에서는 ±10년. 오늘이 창 밖이면 `VisitCalendarPickerTodayReturnLink`가 보기 창만 오늘 쪽으로 옮겨. |
| 상태 보존 | `VisitCalendarMonthSaver`, `VisitCalendarNavLevelSaver`, `rememberSaveable` | 실제 월 `displayedMonth`, 선택창 연도 `pickerYear`, 단계 `navLevel`을 분리. 초기 월에 remember key를 주지 않아 자정에 보던 월을 오늘로 끌어오지 않아. |
| 계층 전환 | `visitCalendarHierarchyTransition` | 기존 170ms fade+0.97 scale. 변경 대상 아님. |
| 선택창 범위 전환 | `visitCalendarPickerStepTransition` | 기존 200ms 수직 slide. 변경 대상 아님. |
| 실제 달 이동 | `visitCalendarPageTurnTransition`, `visitCalendarPageTurnModifier`, `VisitCalendarMonthPage` | 실제 달력 한 장의 기존 380ms 월 넘김. 봉투 접힘·펼침과 별개이며 변경 대상 아님. |
| 뒤로가기 | `visitCalendarNavLevelOnBack`, drawer `BackHandler` | 연도 선택→월 선택→달력→drawer 닫기. 보관함이 열려 있으면 보관함→이전 달력 장면을 먼저 처리해. |

## 방문 기록·오늘 표시 연결

1. [MainActivity.kt](../../app/src/main/java/com/postcardmemory/MainActivity.kt)의 시작 `LaunchedEffect`가 `VisitRecordStorage.recordTodayVisit` 결과를 전달하고 관측 날짜를 `VisitHistoryStorage.recordDate`로 기록한 뒤 현재 월 `loadMonth`를 읽어. 여기만 봐도 **달력을 여는 것/탐색하는 것과 방문 기록 생성은 다른 경로**야.
2. `MainActivity`의 `PostcardApp` → [GalleryScreen.kt](../../app/src/main/java/com/postcardmemory/ui/gallery/GalleryScreen.kt) `GalleryScreen` → `VisitCalendarDrawer` → `MonthlyVisitCalendar`로 `visitedEpochDays/totalVisitDays`가 전달돼.
3. `MonthlyVisitCalendar`는 현재 달 이외에는 `LaunchedEffect(displayedMonth, todayYearMonth)` → IO에서 [VisitHistoryStorage.kt](../../app/src/main/java/com/postcardmemory/utils/VisitHistoryStorage.kt) `loadMonth(filesDir, displayedMonth)`를 읽고 세션 내 `loadedVisitsByMonth`에 보관해. 읽기 전용이며 날짜를 새로 만들지 않아.
4. `visitedDaysForMonth`는 그려지는 장의 month에 맞는 집합만 골라 `VisitCalendarMonthPage` → `VisitCalendarMonthGrid`로 넘겨. 다른 달 기록을 대신 쓰지 않아.
5. `VisitCalendarMonthGrid`는 `toEpochDay()` 포함 여부로 방문 표시를 결정해. 방문일은 `visitDayGelPenMark`의 민트 `#16A7A1`, 실제 오늘 방문은 `#117E7A`, 오늘 숫자에는 방문 여부와 관계없이 `visitTodayPenCircle`을 써. 날짜 계산·주말색·카오모지·6주 높이는 이번 변경 범위 밖이야.
6. 오늘 기준은 [GalleryCurrentDate.kt](../../app/src/main/java/com/postcardmemory/ui/gallery/GalleryCurrentDate.kt)의 `rememberTodayDate`에서 받고, 저장·집계 기준은 [VisitRecord.kt](../../app/src/main/java/com/postcardmemory/utils/VisitRecord.kt)에 있어. 방문 총횟수와 달력에 실제 관측된 날짜 개수는 서로 다른 값일 수 있어. 이번 목업의 '111번'은 고정 샘플일 뿐이야.

## 스티커 통과의 상태 관계

| 상황 | 코드 근거·판단 | 이번 확인 수준 |
|---|---|---|
| 보관함 진입 전 선택 월 | `displayedMonth`는 Monthly 내부 saveable, `sceneStateHolder.SaveableStateProvider(key=stash)`로 장면별 보존 | 소스와 기존 구조 테스트 확인 |
| 보관함 종료 | `showStickerStash=false`만 변경. 월·pickerYear·navLevel을 직접 바꾸지 않아 | 소스 확인. 실제 반복 복귀 QA는 미실행 |
| 연도/월 선택 중 통 진입 | 파우치는 Monthly 아래에 항상 있어 선택창에서도 접근 가능. 돌아오면 해당 단계와 선택창 연도도 유지하는 구조야 | 소스상 경로. 이 조합의 실기기 결과는 미검증 |
| 사이드바 다시 열기 | `LaunchedEffect(drawerState.isOpen)`에서 `navLevel=CALENDAR`, `showStickerStash=false`. displayedMonth는 직접 초기화하지 않아 | 의도된 초기화. 월까지 초기화하는 코드 없음 |
| 상태 충돌 | `navLevel` enum과 `showStickerStash` Boolean이 분리. scene transition과 hierarchy transition도 별도 함수 | 직접 상태 덮어쓰기 충돌은 발견하지 못했어. runtime 무충돌 보장은 아님 |
| 방문 캐시·종이 | Monthly가 빠지면 일반 `remember` 방문 cache는 사라져 다른 달을 다시 읽을 수 있어. 종이도 재진입 decode 가능. 월 자체의 saveable 보존과는 별개야 | 코드와 전일 HANDOFF의 기존 위험. 수정 안 함 |
| 보관함 내부 선택 | `selectedId/confirmRemove`는 일반 `remember`라 재진입 보존 대상 아님 | 현재 동작. 임의로 saveable 변경 안 함 |

달력 외형 교체에는 `navLevel/showStickerStash/SaveableStateProvider`를 바꿀 이유가 없어. 새로운 상태·저장 계층을 추가하지 않는 범위를 권장해.

## 최초 목업 내용과 적용 후보 — 외형 반려, 이력 보존

- 파일: [visit-calendar-envelope-mockup.html](mockups/visit-calendar-envelope-mockup.html).
- 연도 선택 비교와 월 선택 비교를 각각 **왼쪽 현재 / 오른쪽 봉투안**으로 배치했어. 기본 바깥 폭304px, padding24px, 내용256px, 4×4·셀40px 높이는 동일해. 좁은 화면에서는 두 패널 비교 줄을 가로로 스크롤해 원래 비율을 보존해.
- 좁은 공간 280px 옵션은 공간 제약 관찰용 가정이야. 실제 drawer 폭304dp를280dp로 바꾸자는 제안이 아니야.
- 작은 봉투는 위쪽에 얕은 접힘선, 하단 양끝에 짧은 접힌 선만 둬서 숫자 주변의 넓은 전면을 비워. 그림은 56×32px, 중앙 숫자는 연도12px·월13px. 별도 라벨·버튼 판·두꺼운 외곽·추가 그림자는 없어.
- 선택은 기존 진한 숫자와 Medium weight, 현재 기간은 청록과 연한 원, 인접 기간은 연한 숫자. 현재/선택 중첩도 두 버튼으로 비교 가능해. 칸 탭은 양쪽 **시각적 선택만** 동기화하고 실제 navigation/date 계산은 하지 않아. 연도 비교와 월 비교는 각각 독립된 샘플 장면이며, 두 선택값이 실제 앱의 동일 세션을 뜻하지 않아.
- 앱의 `visit_calendar_paper.png`를 읽기 전용으로 참조하고 정적인 SVG 외곽/접힘을 써. **이 방향이면 추가 PNG가 필수는 아니야.** HTML을 폴더 밖으로 옮기면 상대 참조 PNG도 필요해. 왼쪽의 종이 crop·불규칙 외곽과 아래 리본은 간략 재현이므로 실제 앱 픽셀과 동일하다고 말할 수 없어.
- 실제 적용 후보: `VisitCalendarDrawer.kt`의 `visitCalendarPickerPaperScrap`를 대신하는 봉투용 cached draw modifier와 두 picker의 배경 연결만. 기존 종이 bitmap을 재사용하고 얕은 접힘선을 같은 draw pass에 그리는 방식이 최소안이야. 숫자·marker·clickable·목록·callback·전환은 보존해.
- 새 PNG 질감이 필요하다고 사용자가 판단하면 별도 명세·제작 프롬프트·적용 계획을 먼저 제출해. 이번에는 PNG를 생성하지 않았어.

## 관련 테스트와 검증 상태

- `VisitCalendarTest`: decade/list·month/list·선택/현재 우선순위·back 단계·swipe 방향·방문/오늘 색·종이 crop의 순수 함수 assertion을 확인했어. 실제 Compose gesture 테스트는 아니야.
- `VisitCalendarPaperPageStructureTest`: 종이 로딩·월 장면·오늘 circle·민트 mark·picker의 paper shader/40dp/marker/클릭·기존 전환 연결을 구조로 확인해.
- `StickerStashWiringStructureTest`: 장면별 saveable holder·보관함 back 우선·drawer 재열기·파우치 애니메이션 조건의 소스 구조를 검사해. 이 문자열 assertion이 runtime 복원 순서를 보장하지는 않아.
- **향후 적용 시 주의:** `pickerCellsSitOnCalendarPaperScrapsWithoutBecomingCards`는 modifier 이름과 종이 외곽을 직접 고정해. 봉투안 적용 때 테스트가 요구하는 '새 Card 없이 같은 종이를 재사용·칸 높이·현재표시·터치 보존' 의미를 유지하며 승인된 외형에 맞게 수정해야 해. 지금은 production/test 무변경이라 충돌하지 않아. 테스트를 무조건 통과시키는 약화는 하지 않아.

| 검증 영역 | 이번 상태 |
|---|---|
| 로컬 정적 검증 | HTML script 구문·내부 SVG 참조·상대 이미지/문서 경로·diff 검사 실행. 자세한 결과는 HANDOFF에 기록 |
| 브라우저 시각 검수 | 실행 불가. CUA inventory는 브라우저 없음, iab 열기는 `Browser is not available: iab`. 실제 렌더링·키보드 조작은 미검증 |
| JVM / debug APK / instrumentation 컴파일 | 불필요·미실행. 읽기 전용 감사와 독립 문서/목업만 변경 |
| 실사용 기기 instrumentation | 미실행 |
| 실기기 감각 QA | 미실행. 앱 적용 전이야 |
| 신규 GitHub Actions CI | 불필요·미실행. workflow는 JVM·APK·instrumentation **컴파일**이며 실제 instrumentation 실행은 없음 |
| TEST-COVERAGE-MAP | 변경 없음. 앱 테스트 수·파일·보호 의미 변화 없음 |

## 최초 작성 당시 승인 대기 — 후속 판단은 아래 참조

이번 산출물은 **구조 감사·목업 파일 작성 완료 / 브라우저 시각 검수 미완료 / 목업 사용자 검토 대기**야. 프로덕션 UI 수정, 기기 설치, Git 마감은 하지 않았어. 봉투 전면·숫자·기존 상태 표시가 괜찮은지 사용자 목업 승인을 받은 뒤 해당 시각 요소만 적용할 수 있어.

1차 자산 계획은 [별도 문서](DAY-111-STICKER-POUCH-ASSET-PLAN.md)에 있어. 두 차수의 승인 대기는 독립적이야.

<a id="followup-image-envelope"></a>

## 111일차 2차 후속 — 이미지 봉투와 상단 인장으로 재설계

### 반려 반영과 보존 범위

사용자는 최초 목업이 봉투보다 반복되는 네모 종이 칸처럼 보인다고 판단했어. **코드로 단순 외곽과 선을 그려 봉투를 대신하는 전제와 '추가 PNG 불필요' 권장은 폐기해.** 앱의 종이 자투리 구현 자체를 이번에 없애는 건 아니야. 이번에는 문서·비교 HTML만 바꿔.

기간 탐색이라는 역할, 4×4 배열, 선택/현재 판정, 인접 기간 선택 가능성, back 단계, 터치 영역과 기존 전환은 보존해. 새로 비교할 부분은 이미지 봉투 표면과 숫자의 인쇄 위치야. 새 외형은 아직 승인되지 않았고, 펼치기·열기·애니메이션·저장은 추가하지 않아. 최신 실기기 캡처 없이 최초 감사와 이번 사용자 피드백을 근거로 한 시안이야.

### 자산 수와 역할 — 권장 1장, 조건부 최대 2장

| 항목 | 권장 처리 | 수량·이유 |
|---|---|---|
| 연도 선택용 기본 봉투 | RGBA PNG | 공통 기본 1장 사용. 16개 연도별 파일을 만들지 않아. |
| 월 선택용 기본 봉투 | 같은 PNG 재사용 | 연도용과 공유하므로 추가 0장. 월 전용 그림·여백은 필요하지 않아. |
| 기본 상태 | 기본 이미지 + 보통 농도 숫자 | PNG 안에 숫자나 도장을 굽지 않아. |
| 선택 상태 | 같은 이미지의 미세 명도 조정 + 숫자 잉크 강화 | 기본안은 추가 0장. 불투명 종이에만 적용하고 별도 사각판을 깔지 않아. |
| 선택 전용 봉투 | 필요할 때만 대안 PNG | 기본안으로 충분히 구분되지 않거나 종이 결이 뭉개질 때 1장 추가. 총 2장. 기본과 원점·외곽·alpha 동일해야 해. |
| 오늘이 속한 연도/월 | 기존 청록 숫자 + 26dp 연한 원을 코드로 유지 | 추가 0장. 날짜 변경을 PNG 교체와 결합하지 않아. |
| 선택과 오늘 중첩 | 기존 우선순위 유지 | 진한 선택 숫자, 현재 원 생략. 오늘만의 새 배지·wax seal을 추가하지 않아. |

선택 PNG를 채택해도 숫자는 계속 코드야. 오늘 전용·월별·연도별·선택×오늘 조합별 PNG는 만들지 않아. 같은 그림의 반복감은 정교한 한 장의 인쇄 질감으로 먼저 평가하고, 랜덤 회전·여러 외곽 자산으로 확대하지 않아.

### 크기·파일 후보와 배치 제안

기존 감사에서 확인한 셀 64×40dp, 내부 그림 56×32dp를 출발점으로 써. 이하 값은 **권장 시안 값**이고 실기기 측정·승인값이 아니야.

| 항목 | 제안 |
|---|---|
| 기본 파일 후보 | `app/src/main/res/drawable-nodpi/visit_calendar_envelope_base.png` — 지금 추가하지 않아. |
| 선택 대안 후보 | 같은 폴더 `visit_calendar_envelope_selected.png` — 조건부 1장, 지금 추가하지 않아. |
| 제작 목표 | 각 **252×144px, 7:4, RGBA PNG**, 바깥 실제 투명 alpha. 56×32dp × 가정 3x × 여유 1.5야. |
| 해상도 근거 | 3x 표시168×96px 대비1.5배, 4x224×128px 대비1.125배. 최대 확대·export 없음. 4x 미세 결은 실제 자산으로 확인할 항목이야. |
| 캔버스·외곽 | 전체 캔버스를 56×32dp에 Fit. 종이는 대략 x=1~55, y=1~31dp, 얇은 투명 여백. 개별 bbox trim·자동 중심 보정 금지. |
| 정면과 플랩 | 닫힌 봉투 정면, 양쪽 위에서 아래 중앙으로 내려오는 얕은 삼각 플랩과 옆 접힘. 접힘은 읽혀야 하지만 두꺼운 선·그림자·입체 봉투는 피해. |
| 상단 숫자 anchor | 이미지 좌상단 기준 **(28, 9)dp**, 셀 기준 **(32, 13)dp**. PNG 좌표 중심 **(126, 40.5)px**. |
| 인쇄 안전 영역 | 이미지 x=9~47dp, y=2~16dp(38×14dp)를 조용하게 확보해. 플랩선과 진한 얼룩이 네 자리 연도를 가르지 않게 해. |
| 글자 | 기존 연도12sp·월13sp를 출발점으로 유지. 대략 line-height14dp의 상단 영역에 놓되 큰 글꼴 QA 뒤 조정. 과도한 자간·가짜 마모·숫자 누락은 금지. |
| 인장 느낌 | 숫자 자체를 작은 인쇄 표식으로 사용. 묵직한 세리프 계열은 HTML의 비교 가정이며 앱 폰트 교체 확정이 아니야. 별도 방패판·라벨·테두리를 만들지 않아. |
| 오늘 원 위치 | 숫자와 함께 anchor로 옮기는 후보. 지름26dp면 상단이 이미지 위로 약4dp 나올 수 있어, 셀 안에 두고 clip 금지. 기존 색·지름·중첩 우선순위는 유지해. |

선택은 이미지 brightness 약0.94와 더 진한 숫자로 비교해. 인접 기간의 숫자 농도는 기존 규칙대로 유지해. 이 수치는 목업 가정이며 Android 색 처리값을 확정한 게 아니야. 선택을 색 변화만으로 새로 정의하지 않고 기존 글자 굵기·선택 의미·접근성 상태를 유지해.

**메모리 추정:** 원본 ARGB_8888 decode는252×144×4=145,152B, 약0.138MiB/장, 선택까지2장은 약0.277MiB야. 16칸마다 원본을 새로 decode하는 구조는 권장하지 않아. 화면/리소스 단위로 공유하는 적용안을 검토하고 GPU·캐시·실제 APK 증가분은 자산 제공 뒤 실측해야 해. PNG 압축 바이트와 decode RAM은 달라.

### 비교 HTML의 역할

기존 자투리/이미지 봉투를 나란히 보고, 봉투 전면·상단 숫자·선택 농도·오늘 중첩을 비교해. 작은 4×4 실제 크기 비교와 확대 관찰을 분리해. 확대가 좋아 보여도 작은 크기 합격을 뜻하지 않아. HTML 속 봉투 이미지는 **검토용 시안**이며 최종 앱 자산 규격·alpha·인쇄 질감 합격본이 아니야. 숫자·현재 원·선택 명도는 별도 HTML/CSS 오버레이야. SVG 봉투 외곽과 접힘선은 제거해.

검토용 이미지는 built-in imagegen으로 한 장만 생성했고, 원본 **1659×948px RGBA,2,513,898B**를 리사이즈 없이 HTML의 data URI로 내장했어. 별도 앱 리소스나 추가 목업 파일은 없어. HTML은 약3.37MB야. 원본은 `C:/Users/estel/.codex/generated_images/01a162cf-de4b-7df2-8028-6a354cdecf53/exec-4fc709d5-a7fa-4b36-8857-075f13a8b34a.png`에 남겨 두었고 HTML은 이 경로에 의존하지 않아. 생성 때 사용한 내용은 '7:4에 가까운 단일 얇은 중성 회색 봉투,실제 투명 배경,정면의 삼각 플랩과 은근한 옆 접힘,상단 중앙 숫자 자리,글자·도장·배지·소품 없음'이야. 기본/선택은 같은 이미지를 사용해.

이미지 자체는 도구 출력으로 직접 확인했어. 플랩이 읽히지만 접힘 음영이 목표보다 강하고 바깥 잔점이 남아 있어. header는 RGBA이며 바깥 두 표본 alpha=0,종이 안쪽 표본 alpha=253이었어. 이는 전체 alpha 검수나 최종 불투명 종이 합격을 뜻하지 않아. 최종 제작 때 더 평평한 음영·잔점 정리·종이 내부 alpha를 확인해야 해. 이 목업에서 별도 이미지 편집이나 리사이즈는 하지 않았어.

브라우저 연결 inventory는 이번에도 비어 있어 실제 렌더링·클릭·키보드·글꼴·좁은 폭 시각 검수는 미완료야. 코드 기준 의도는 '봉투 이미지 위쪽에 표식처럼 찍힌 숫자'이고, 실제로 그렇게 보이는지는 사용자 목업 확인이 필요해. 연도/월 샘플 선택만 동기화하며 앱 navigation·현재 날짜 계산·방문 기록을 구현하지 않아.

### 제작 프롬프트 초안 1 — 기본 봉투

종이 배경·재질 부품으로 STANDARD 2.2 적용 제외를 사용해. 실사 물체 사진을 인쇄해 오린 스티커나 활자 조각 프리셋을 강제하지 않아. 아래 프롬프트는 **최종 앱 자산 제작용 초안**으로, 이번 비교 이미지 제작과 별개야.

```text
post-card-memory의 연도·월 선택에서 공통으로 쓸 닫힌 봉투 이미지 한 장을 만들어줘. 얇은 일상 인쇄 종이로 된 봉투 정면을 납작하게 보여줘. 중성 회색 중심의 낮은 채도, 밝고 조용한 종이 결, 가까이서만 보이는 미세하고 촘촘한 인쇄 망점·작은 잉크 농도 차이·약한 번짐을 써. 노랗고 강한 세피아의 골동품이나 깨끗한 벡터 아이콘은 피하고, 작은 화면에서도 봉투의 플랩·접힘이 알아보이게 해.

최종 도입 목표는252×144px,7:4 RGBA PNG야. 도구가 이 크기를 지원하지 않으면 같은7:4비율로 만들고 실제 출력 크기를 알려줘. 논리 캔버스는56×32, 환산배율4.5야. 종이는 대략 x=1~55,y=1~31 안에 두고 바깥은 실제 alpha=0으로 만들어. 체크무늬나 흰 바탕을 그리지 마. 캔버스와 원점을 유지하고 bbox crop·trim·자동 확대하지 마. 이번 요청에 리사이즈·앱 적용은 포함하지 않아.

전면이 넓게 보이고 양쪽 위 모서리에서 아래 중앙 쪽으로 내려오는 얕은 삼각 플랩과 짧은 옆 접힘이 느껴져야 해. 위쪽 중앙의 x=9~47,y=2~16 논리 영역은 숫자 인쇄를 위해 조용하게 비워줘. 숫자 중심은(28,9),최종px중심은(126,40.5)야. 진한 접힘선이나 얼룩이 이 영역을 가르지 않게 해. 외곽에는 아주 작은 가위질 흔들림만 허용해.

숫자·월·연도·문자·문장 마크·도장·오늘 원·배지는 이미지에 넣지 마. 앱이 별도로 인쇄하듯 얹을 거야. 숫자 뒤 네모판·라벨·버튼판·두꺼운 테두리·왁스 실링·손·소품·여러 겹 종이 공예·찢어진 가장자리·입체 두께·강한 바닥 그림자·광택·색 halo는 금지야. 단일 기본 봉투만 출력해.

검수: 실제 alpha,캔버스7:4,56×32dp에서 읽히는 봉투,상단 숫자 안전영역,밝고 낮은 대비의 종이,halo 없음. 출력 크기와 투명 여부를 알려주고 좌표 정합을 추측으로 보장하지 마.
```

### 제작 프롬프트 초안 2 — 선택 봉투, 필요할 때만

기본 1장과 코드 명도 차이로 충분하면 **이 이미지는 제작하지 않아.** 필요할 경우 기본 PNG를 edit target으로 제공하고 따로 생성한 새 외곽으로 대체하지 않아.

```text
제공한 기본 봉투 PNG를 편집해 post-card-memory의 선택 상태 봉투 한 장을 만들어줘. 기본 이미지가 없으면 별개 봉투를 새로 그리지 말고 기본 참조가 필요하다고 알려줘. 얇고 평평한 중성 회색 인쇄 종이,미세하고 촘촘한 망점·약한 번짐·낮은 채도는 유지해. 종이 전체를 아주 조금 눌린 톤으로 조정하되 밝은 질감과 플랩·접힘이 살아 있어야 해.

최종 목표252×144px,7:4 RGBA PNG야. 입력의 캔버스 크기·원점·실루엣·alpha·투명 여백·접힘 위치를 그대로 유지해. 원본이 목표 크기와 다르면 출력 크기를 알려주고 임의 crop·trim·확대·재배치를 하지 마.56×32논리 좌표의 상단x=9~47,y=2~16 안전영역과 숫자 중심(28,9)을 보존해. 리사이즈·앱 반영은 포함하지 않아.

숫자와 잉크 농도는 앱이 따로 그리므로 숫자·글자·문장·도장·오늘 원·선택 배지·테두리를 굽지 마. 색만 진한 새 직사각형 판을 추가하지 마. 새로운 종이 겹·왁스 실링·3D두께·그림자·노란 세피아·찢김·굵은 망점·halo·배경 체크무늬는 금지야. 기본과 정확히 겹쳐 비교할 수 있는 한 장만 출력해.

검수:기본과 같은 외곽·alpha·좌표,숫자 자리 보존,아주 미묘하지만 읽히는 종이 톤 차이,작게 표시해도 질감 유지. 실제 크기·alpha를 알려주고 정합은 별도 합성 검수가 필요하다고 표시해.
```

### 승인 뒤 적용 후보와 이번 마감

- 후보는 공통 PNG1장(조건부 선택1장), `VisitCalendarDrawer.kt`의 두 picker 배경 이미지 연결과 숫자/현재 원의 상단 정렬이야. 기존 제안의 '배경 modifier만 교체'보다 배치 변경이 추가돼. 셀 전체 클릭·선택 callback·목록·상태·전환·방문 저장·Room은 바꿀 이유가 없어.
- 기존 구조 테스트가 종이 modifier와 중앙 정렬을 직접 고정하는지 승인된 구현 단계에서 확인하고 보호 의미를 유지해야 해. 이번에는 production/test/테스트 지도 무변경이야.
- 검토할 항목은 이미지 봉투 인상,상단 네 자리 연도 가독성,현재 원과 접힘의 간섭,선택 톤 차이야. 공통1장+코드 오버레이를 권장하며 선택 PNG 추가는 관찰 근거가 생길 때 결정해.
- 이번에는 문서·비교 HTML만 보완해. 앱 로컬 빌드/JVM은 불필요·미실행, 실사용 기기 instrumentation과 감각 QA는 미실행, 신규 CI는 금지·미실행이야. HTML 정적 검증과 이미지 header 결과는 최종 보고에 남겨. HANDOFF는 앞선 사용자 지시에 따라 로컬 수정본을 보존하고 추가 수정·stage하지 않아.

**후속 정적 검증 결과:** Node로 script1개 구문 컴파일,HTML id9개 중복 없음,SVG use4개 참조 존재,내장 PNG의1659×948 RGBA header,연도16/월16 샘플,문서 링크10개와 HTML 상대경로,세 산출물 공백·충돌 표시 검사를 통과했어. `git diff --check`도 오류 없었어. DOM 실행·시각 렌더링 검증은 아니야. HANDOFF·보호 설정/PNG·기존 파우치 목업 총7개 경로의 SHA-256은 후속 시작 전과 동일해. 이번 stage·commit·push는 없어.
