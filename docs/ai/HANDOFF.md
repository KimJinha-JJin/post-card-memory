# HANDOFF — 98일차: 코드 클린 day (읽기 전용 dead code 감사 → 미사용 import 정리 + 삭제 gate 취소 전파 수정, 미커밋)

확인일: 2026-10-09. 수동 표준 모드(98일차 1·2단계 작업지시서), 담당 Claude Code.

- **시작 상태:** `feature/photo-sticker`, HEAD `4225a04`, local/origin 0/0, tracked clean. 보호 untracked 3종(`.codex-config.candidate.toml`, `.kotlin/`, `postcard_paper_fiber_tile.png`) — 미수정·미stage.
- **1단계(읽기 전용 감사) 결론:** A등급(삭제 가치 높음) — `PostcardRepository`의 호출부 0 래퍼 7개(`deletePostcard`, `updatePostcardMessageFont/DateFormat/DateTextScale`, `updatePostcardEnvelopeStyle/EnvelopePostmarked`, `clearPostcardEnvelope`), `EditorEmptyHint`, `EditorSegmentedTabRow`, `GalleryViewMode`, `PostcardImageStorage`(+`PostcardImageStorageTest` 6건). B — `updatePostcardTemplateStyle`(휴면 템플릿 쓰기 경로), `presetLabelTapeStyles`/`tapePalette()`(test 전용), 중복 helper(`rotateBitmapUsingExif` 3벌, `createPinkingPath` 화면/exporter 2벌, `getFileExtension` 2벌, exporter 스티커용 `drawCenterCroppedBitmap`). C(유지) — legacy enum 항목, 봉투 컬럼, 템플릿 subsystem, `OrphanFileDiagnostics`, `PondController.sequence`, test seam, Hilt/framework 진입점. **1단계 보고의 "미사용 import production 22개"는 오집계 — 실제 21개.**
- **2단계 변경 (미커밋):**
  - 미사용 import 27줄 삭제(main 7파일 21줄, test 5파일 6줄). 파일마다 이름이 주석까지 포함해 다른 곳에 0회임을 확인. KDoc 링크용 import 2개(`GalleryScreen` LazyColumn, `AppIntroScreen` SEAL_POSTMARK_DATE_TEXT_RATIO)는 유지.
  - `PostcardDeletionManager.kt` — DB-우선 gate를 `internal suspend fun deletePostcardDatabaseFirst`로 떼고 Manager는 그대로 위임. `runCatching`(취소까지 "DB 삭제 실패"로 흡수) → `try/catch`로 `CancellationException`은 재던지고 나머지 `Throwable`은 기존과 같은 실패 결과. DB 삭제 성공 → 파일 정리 순서, 실패 시 파일 0건, public API 불변.
  - 신규 `PostcardDeletionDatabaseFirstGateTest`(JVM 2건: 취소 전파·파일 정리 0건 / 일반 DB 오류 → 실패 결과·파일 0건). gate를 `runCatching`으로 되돌리는 일시 변형에서 취소 테스트 실패 확인 후 원복.
  - `TEST-COVERAGE-MAP.md` 갱신(JVM 930→932, 테스트 파일 94→95, XML 95→96, 파일 삭제 섹션에 gate JVM 보호 추가).
- **검증:** 로컬 `testDebugUnitTest` 932/932(XML 96, 실패·오류·skip 0), `assembleDebug`·`assembleDebugAndroidTest` 성공. 실사용 기기 instrumentation 미실행(불필요), CI 미실행(미push). 실기기 QA 불필요 — 취소는 사실상 ViewModel 종료(`viewModelScope` 취소) 때만 일어나 사용자에게 보이는 결과가 없음. 기존 경고 `VisitRecordTest.kt:84` 불필요한 `!!`는 이번 변경과 무관(미수정).
- **봉투·템플릿 legacy 추가 조사(읽기 전용, 사용자 사실: 봉투 사용 엽서 0, 템플릿 적용 엽서 0):**
  - 봉투: 쓰기는 사용자가 봉투를 고를 때만(`65fe77b` 08-04 ~ `7fe0047` 08-05), 자동 기록 경로 없음 → 실제 행은 전부 NULL/0으로 봐도 됨. 컬럼 제거는 DB 19→20 + 새 `MIGRATION_19_20` 필요(minSdk 26이라 `DROP COLUMN` 불가 → 테이블 재생성·전체 행 복사), 기존 migration 수정은 불필요하지만 Entity 필드도 같이 지워야 함. 검증할 migration 계측(`PostcardFullMigrationChainTest`, CONDITIONAL)은 실사용 기기에서 실행 불가. 이득은 2컬럼뿐 → **컬럼 유지, Repository 래퍼 + DAO 쿼리 3개만 제거 추천**(schema 무영향).
  - 템플릿: `Postcard`에 템플릿 전용 컬럼 없음(DAO `updatePostcardTemplateStyle`은 기존 스타일 컬럼 갱신일 뿐). 파일(`filesDir/postcard_templates/`)은 사용자가 "내 템플릿 저장/이름 변경/덮어쓰기"를 직접 눌렀을 때만 생성(07-24~08-28). "적용 0"은 "저장 0"과 다름. `allowBackup=true`(규칙 없음) + 10-05 런처 삭제 후 7~8월 클라우드 백업 자동 복원 → 그 시기 템플릿 파일이 지금 기기에 있을 수 있음. subsystem을 지워도 파일은 안 지워짐(삭제 코드 없음, `PostcardDeletionManager`·`OrphanFileDiagnostics` 무관) — 읽는 코드만 사라짐. **파일 존재 실측 전까지 판단 보류.**
- **Git:** commit·push 미승인·미실행. 보호 untracked 3종 그대로.
- **다음 후보(실행 승인 아님):** ① A등급 정리(Repository 래퍼 7개 → 봉투 DAO 3개 포함 여부 결정, `EditorEmptyHint`/`EditorSegmentedTabRow`/`GalleryViewMode`, `PostcardImageStorage`+테스트) ② 템플릿 파일 존재 실측(사용자가 기억 확인 또는 승인된 읽기 전용 `adb shell run-as` 목록 조회 — debug 빌드일 때만 가능) ③ `PostcardDeletionManagerTest` KDoc의 "순수 JUnit으로 순서 재현 불가" 문장은 이제 일부 낡음(gate는 JVM으로 재현됨).

---

# HANDOFF — 97일차: 주석 감사 + 낡은 주석·끊어진 지시서 참조 정리 (주석-only, commit·push·CI 성공)

확인일: 2026-10-08. 수동 표준 모드(97일차 1·2단계 작업지시서), 담당 Claude Code.

- **시작 상태:** `feature/photo-sticker`, HEAD `4ce281e`, local/origin 0/0, tracked clean. 보호 untracked 3종(`.codex-config.candidate.toml`, `.kotlin/`, `postcard_paper_fiber_tile.png`) 존재 — 미수정·미stage.
- **1단계(읽기 전용 감사) 결과:** 조사 221파일(main 116, test 95, androidTest 7, gradle `.kts` 3 별도), 주석 포함 204파일, 주석 줄 5,885(`//` 1,564줄, KDoc 839블록/4,098줄, `/* */` 46블록/223줄) — 문자열·template을 구분한 자체 lexer 기준 **근사치**. TODO/FIXME 0, STOP 1(`VisitCalendarDrawer` 대체공휴일, 현재 코드와 일치), 작업일차 주석 187개(78파일; `AppIntroScreen`의 "33일차"는 누적 방문일 도메인 용어라 작업일 아님), QA 42·실기기 41, 저장소 밖 "작업지시서 N절" 인용 20곳(13파일). 좋은 주석 예: `PostcardTemplate.kt` 휴면 사유, `PostcardDatabase` MIGRATION_14_15, `DetailViewModel` 배경 이미지 파일 비삭제 이유, `PhotoStickerEdgeStyle` 난수 순서 계약.
- **2단계 승인 범위:** 1순위(낡은 주석 갱신) + 2순위(작업지시서 참조 정리)만.
- **변경 (`32f871f`, 13파일 +47/−64, 주석만):**
  - `GalleryRetroClock.kt` — v2/v3 폭 변천사를 걷어내고 현재 구조(몸체 = 고정 크기 오림 이미지, 글자 폭에 맞춰 닫히는 것은 `GalleryRetroClockFace`뿐)로 갱신. Face 쪽 "(v3)"·"v2 배너 폭" 근거도 현재화.
  - `GalleryMemoryDensityStructureTest.kt` — KDoc이 제거된 얼굴·하트 구조를 "고정한다"던 모순을 현재 계약(비례형 중성펜 막대 + 전체 폭 구분선 하나 + 월 숫자, 걷어낸 요소 재유입 방지)으로 갱신. 테스트 본문·assertion·이름 불변.
  - `VisitCalendarDrawer.kt` — 자투리 주석의 낡은 "96일차 추가(실험)" 머리말 제거(새 날짜 태그로 대체하지 않음), 102줄 공휴일 중복 주석 삭제(133줄 KDoc·STOP 계약 유지).
  - 작업지시서 참조 19곳 정리(main: `DetailScreen`, `LabelStickerItem`, `MaskingTapeDetailScreen`, `MaskingTapeItem`, `GalleryRetroClock`, `GalleryScreen` / test: `LabelStickerLayerOrderStructureTest`, `MaskingTapeItemTest`, `GalleryMemoryDensityTest`, `GalleryMonthlyGridStructureTest`, `VisitCalendarTest`). 문서 좌표만 제거하고 설계 이유는 보존. `AGENTS.md 5절` 참조 2곳은 실존 문서라 유지.
- **코드 비변경 확인:** 13파일 각각 HEAD와 작업본의 주석 제거 코드가 동일(`ALL CODE IDENTICAL`), diff의 비주석 +/− 줄 0. `git diff --check` 통과.
- **검증:** 주석-only라 로컬 테스트 미실행(위 기계 대조로 대체). GitHub Actions CI run `37738657682` 성공(JVM unit test·assembleDebug·assembleDebugAndroidTest). 실기기 QA 불필요(실행 동작 변경 없음). TEST-COVERAGE-MAP 변경 없음(테스트 수·의미 불변).
- **Git:** `32f871f` commit·push, local/origin 0/0. 보호 untracked 3종 그대로.
- **보류(실행 승인 아님):** 연대기형 테스트 KDoc 축약(`StickerEditModeToolbarStructureTest`, `SaveErrorDialogStructureTest`, `EditorSubcategoryNavBarStructureTest`, `GalleryViewSelectionStructureTest`, `GalleryScreen` 63→88일차 클러스터 설명 등), 복제 설명 정본화(haptic 이유 4곳, 3단 보기→연못 이식 5곳), `Postcard.kt:23` layoutStyle SQL 기본값 `'STANDARD'` 계약 주석 추가, `DatabaseModule.kt` 주석 추가, `PostcardRepository.kt` 주석 추가.
- **새로 발견한 후속 후보:** `GalleryScreen.kt` 빈 상태 주석의 "앞으로 추가될 보기(월별/타임라인/캘린더/우표/기억 밀도)"는 76일차에 삭제된 보기를 미래형으로 나열하는 낡은 주석. "실기기 QA에서 보정한다"(도장·사진·테이프 손 좌표 3곳)는 보정 완료 여부 기록 미확인.
- **기존 미결정 유지:** `postcard_paper_fiber_tile.png` 사용·삭제 여부, 95일차 HANDOFF CI 성공 기록 보정 — 오늘 건드리지 않음.

## 97일차 추가 작업: 주석 감사 후속 정리 (주석-only, commit·push·CI 성공)

위 보류·후속 후보를 97일차 추가 작업지시서 범위에서 이어 처리했어. 시작 HEAD `f8aa232`, local/origin 0/0, tracked clean.

- **변경 (`0652fd7`, 10파일 +56/−80, 주석만):**
  - `GalleryScreen.kt` — 빈 상태 주석의 "앞으로 추가될 보기" 목록 제거(이유만 남김). pager 주석·`GalleryMonthlyGridPage`/`Item` KDoc에서 "76일차 3단 보기 삭제" 경위를 빼고 현재 구조만 남김.
  - `Postcard.kt` — `layoutStyle` SQL 기본값 `'STANDARD'`와 Kotlin `"STAMP"`가 일부러 다르다는 계약 주석 추가(맞추면 schema 검증·Migration 영향, 옛 값은 MIGRATION_14_15가 정규화).
  - `DatabaseModule.kt` — destructive fallback을 쓰지 않고 Migration 누락 시 실패해야 한다는 주석 추가. 등록 구조 테스트는 fallback 부재와 이유를 잠그지 않아 추가함(주석은 `.addMigrations(` 밖이라 테스트 파싱 무영향).
  - `PostcardRepository.kt` — 두 삭제 메서드는 Room 행만 지우며 사용자 삭제는 `PostcardDeletionManager`를 거친다는 주석 추가. 근거: `deletePostcardById` 호출부는 Manager 한 곳, 상세·갤러리 삭제 모두 Manager 경유.
  - 연대기형 테스트 KDoc 축약: `StickerEditModeToolbarStructureTest`, `SaveErrorDialogStructureTest`, `EditorSubcategoryNavBarStructureTest`, `GalleryViewSelectionStructureTest` — 현재 테스트가 막는 계약만 남김.
  - 복제 설명: haptic 이유는 production 3곳 유지, 테스트(`AppIntroVisitPostmarkStructureTest`) 1곳만 축약. 3단 보기→연못 설명은 main 3곳·test 2곳(`GalleryMonthlyGridStructureTest`)에서 경위 제거.
- **조사 후 수정하지 않음:** 손 좌표 "실기기 QA에서 보정한다" 3곳(`SealStampInteraction`, `PhotoStickerPlaceInteraction`, `MaskingTapePlaceInteraction`) — 값은 첫 commit(`0a19187`·`c8a2cf5`·`b9cd53b`) 이후 불변, 기능 QA는 통과했지만 anchor를 항목별로 확인·보정한 기록이 없어 **확인 불가로 유지**. 사용자가 확정으로 판단하면 현재형으로 바꿀 수 있음.
- **코드 비변경 확인:** 10파일 모두 주석 제거 코드가 HEAD와 동일(`ALL CODE IDENTICAL`), 새 주석에 구조 테스트 위험 토큰 없음, `git diff --check` 통과.
- **검증:** 로컬 테스트 미실행(주석-only, 기계 대조로 대체). CI run `37742063755` 성공(JVM unit test·assembleDebug·assembleDebugAndroidTest). 실기기 QA 불필요. TEST-COVERAGE-MAP 변경 없음.
- **남은 후속 후보(실행 승인 아님):** `PostcardRepository.deletePostcard(postcard)`는 production 호출부 0인 dead 메서드(정리는 코드 변경이라 별도 승인). `SaveErrorDialogStructureTest` KDoc의 저장소 밖 문서 참조("제2차 감사 보고서 부록") 남음. 손 좌표 QA 주석 3곳 확정 여부는 사용자 판단 대기.

---

# HANDOFF — 96일차 추가: 월/연도 고르기 칸 뒤 종이 자투리 (구현·자동검증·실기기 QA 완료)

확인일: 2026-10-07. 수동 표준 모드(96일차 추가 작업지시서), 담당 Claude Code. 사용자 선택으로 승인된 화살표를 먼저 `dfac9c6`로 commit·push한 뒤 시작(HEAD = origin). **실험 단계 — 다음 후보는 실행 승인이 아니다.**

- **조사 사실:** MONTH_PICKER(`VisitCalendarMonthPicker`)·YEAR_PICKER(`VisitCalendarYearPicker`) 모두 4열×4행, 칸 = weight(1f)×40dp 가운데 정렬 글자(월 13sp·연도 12sp). 선택은 글자색 InkPrimary+Medium, 현재 월/연도는 26dp 옅은 원. 카드·테두리·배경 없음. `visit_calendar_paper.png`(1122×1402)는 화면 위에서 이미 1회 로드돼 picker 호출부에서 그대로 전달 가능. 종이 자산은 전체가 고른 결이라 하단도 깨끗.
- **변경(미커밋):** `VisitCalendarDrawer.kt` — 두 picker에 `paper` 파라미터(이미 로드한 `calendarPaper` 전달), 칸마다 글자 뒤 `visitCalendarPickerPaperScrap`: 칸에서 4dp씩 안쪽(높이 32dp) 자투리를 손으로 자른 윤곽(`visitCalendarPaperEdgeOutline` 재사용)으로 채우고 달력 장과 같은 아주 옅은 접촉 그림자. 자투리 위치는 순수 함수 `visitCalendarPickerScrapCrop`이 종이 하단 55%~끝에서 칸 seed(월: 96_600+year×13+month, 연도: 146_600+year)로 골라 칸마다 결·가장자리가 다름. 종이결 배율은 종이 폭 = grid 한 줄 폭. 새 자산·테이프·리본·펜 없음. 칸 크기·글자·선택 표시·현재 원·클릭·swipe 그대로.
- **테스트:** `VisitCalendarTest` +2(자투리 위치 결정성·하단 영역·종이 안 / 한 화면 16칸 seed 모두 다르고 위치 14곳 이상·윤곽 다름), `VisitCalendarPaperPageStructureTest` +1(두 picker 칸 40dp·자투리→clickable 순서·현재 원 유지·카드/배경/테두리/그림자 modifier 금지·같은 종이 bitmap·새 자산 금지·호출부 `paper = calendarPaper`). 자동검증: gallery 187/187(16 suite), assembleDebug 성공, diff --check 통과.
- **실기기 감각 QA: 사용자 승인(2026-10-07, 첫 시도 값 그대로 "좋아, 이대로 승인")** — 자연스러움, 메인 달력과 같은 세계관, 카드 UI 아님, 복붙 느낌, 장식 없이 충분한지 기준.
- **최종 자동검증:** 전체 `testDebugUnitTest` 930/930(XML 95, 실패·오류·skip 0), `@Test` 930개. TEST-COVERAGE-MAP 갱신 완료(927→930, 구조 164→165).
- **Git:** 사용자 승인으로 `VisitCalendarDrawer.kt`, `VisitCalendarTest.kt`, `VisitCalendarPaperPageStructureTest.kt`, `docs/ai/TEST-COVERAGE-MAP.md`, 이 HANDOFF를 commit·push. CI 결과는 완료보고에서 확인(성공 기록만을 위한 docs-only commit은 만들지 않음). 제외 유지: `postcard_paper_fiber_tile.png`, `.codex-config.candidate.toml`, `.kotlin/`.
- 화살표 `dfac9c6` CI run `37574403863` 성공.
- 다음 후보(실행 승인 아님): 종이 질감 타일 PNG 사용 여부 결정, 95일차 HANDOFF CI 성공 기록 보정.

---

# HANDOFF — 96일차 추가: 방문 달력 월 이동 화살표 신문 오림 (구현·자동검증·실기기 QA 완료)

확인일: 2026-10-07. 수동 표준 모드(96일차 추가 작업지시서), 담당 Claude Code. 시작 HEAD `33a2ce9`(origin과 같음), tracked clean. **다음 후보는 실행 승인이 아니다.**

- **조사 사실:** ◀ ▶는 달력 장 위에 겹쳐 고정된 `IconButton`(기본 48dp 터치) 안의 Material `KeyboardArrowLeft/Right`(꺾쇠, 20dp, InkSecondary). disabled 상태 없음, 접근성 설명 "이전 달"/"다음 달", 넘김은 `displayedMonth` 변경으로만 시작.
- **변경(미커밋):** `VisitCalendarDrawer.kt` — Icon만 20dp `Box`(+`semantics { contentDescription }`)로 바꾸고 코드로 그린 신문 오림 삼각형(`visitCalendarNewsprintArrow`, 순수 함수, 고정 seed 96_500/96_501, ◀▶ 윤곽은 서로 거울상)을 그림. 폭 9·높이 11dp 삼각형, 변마다 1~2번 꺾인 가위 윤곽(±0.25dp), 잉크 InkPrimary α0.82 + 0.5dp 옅은 번짐(α0.18) + 45° 망점(간격 1.15dp, 반지름 0.1~0.38dp, 종이색 #E9E1D3, 가장자리 0.5dp 안쪽만). IconButton·onClick·위치·터치 영역 그대로, 배경·테두리·그림자 없음. 새 이미지 자산 없음. 쓰지 않게 된 auto-mirrored 화살표 import 삭제.
- **테스트:** `VisitCalendarTest` +3(결정성·seed별 다른 윤곽 / 300 seed×양방향: 윤곽 6~9점·상자 안·끝점 세로 가운데·폭 8~10·높이 10~12·망점 10개 이상·종이 비침 3~30% / 좌우 거울상), `VisitCalendarPaperPageStructureTest` +1(버튼·onClick·접근성 설명·크기 유지, `Icons.` 복귀 금지, 배경·테두리·그림자 금지). 자동검증: gallery 184/184(16 suite), assembleDebug 성공, diff --check 통과. 핵심 해결 시도 외: 함수 이름 충돌 컴파일 오류 1회(Modifier를 `visitCalendarNewsprintArrowMark`로 개명).
- **실기기 감각 QA: 사용자 승인(2026-10-07, 첫 시도 값 그대로 "좋아, 이대로 승인")** — 이전/다음 인식, 신문 세계관, 튀는 정도, 누르기 편의 기준.
- **최종 자동검증:** 전체 `testDebugUnitTest` 927/927(XML 95, 실패·오류·skip 0), `@Test` 927개. TEST-COVERAGE-MAP 갱신 완료(923→927, 구조 163→164).
- **Git:** 사용자 승인(다음 실험 전에 먼저 마감)으로 `dfac9c6` commit·push, CI run `37574403863` 성공. 대상: `VisitCalendarDrawer.kt`, `VisitCalendarTest.kt`, `VisitCalendarPaperPageStructureTest.kt`, `docs/ai/TEST-COVERAGE-MAP.md`, 이 HANDOFF. 제외 유지: `postcard_paper_fiber_tile.png`, `.codex-config.candidate.toml`, `.kotlin/`. 직전 `33a2ce9`(리본) CI run `37572520232` 성공.

---

# HANDOFF — 96일차 추가: 방문 달력 하단 신문 오림 리본 (구현·자동검증·실기기 QA 완료)

확인일: 2026-10-07. 수동 표준 모드(96일차 추가 작업지시서 + 사용자 방향 3번 선택), 담당 Claude Code. 시작 HEAD `63fddb3`(origin과 같음), tracked clean. **다음 후보는 실행 승인이 아니다.**

- **사실:** 기존 하단 장식 `VisitCalendarBottomOrnament`는 달력 장 **밖**(종이 아래, "다녀간 날들" 줄 위)의 21dp 줄 양끝 `୨୧` 2개(9sp, InkSecondary α0.39)였고, 장과 함께 넘어가지 않으며 MONTH/YEAR 고르기 화면에서도 보인다. 사용자가 이 구조 유지 + 양끝 기호만 교체(3번)를 선택.
- **변경(미커밋):** `VisitCalendarDrawer.kt` — 양끝 `୨୧` Text를 사용자 제공 자산 `drawable-nodpi/visit_calendar_newspaper_ribbon.png`(1448×1086 RGBA, 원본 무가공, untracked)의 `Image` 2개로 교체. 폭 24dp·원본 비율(높이 18dp, 21dp 줄 안). 자산 둘레 투명 여백 때문에 실제 리본은 약 16×8dp이고 양끝에서 약 4dp 안쪽에 보임. bitmap 1회 decode 후 공유, 알파 원본, 그림자·회전 없음, `clearAndSetSemantics { }` 유지. 쓰이지 않게 된 `VisitCalendarBottomOrnamentColor` 삭제. 장 높이·장 내부·넘김·가운데 장식 변경 없음.
- **자동검증:** `testDebugUnitTest --tests com.postcardmemory.ui.gallery.*` 180/180(16 suite), assembleDebug 성공, diff --check 통과. 테스트 변경 없음 → TEST-COVERAGE-MAP 변경 없음. instrumentation 불필요.
- **실기기 감각 QA: 사용자 승인(2026-10-07, 첫 시도 값 그대로 "좋아, 이대로 승인")** — 크기, 축소 후 형태, 검은 덩어리 여부, 튀는 정도, 손 자산과 같은 신문 하프톤 세계관, MONTH/YEAR 화면 확인 항목 기준.
- **Git:** 사용자 승인으로 `VisitCalendarDrawer.kt`, `visit_calendar_newspaper_ribbon.png`, 이 HANDOFF를 commit·push. CI 결과는 완료보고에서 확인(성공 기록만을 위한 docs-only commit은 만들지 않음). 제외 유지: `postcard_paper_fiber_tile.png`, `.codex-config.candidate.toml`, `.kotlin/`.
- 위험: 1448×1086 ARGB 원본 decode(약 6MiB, 기존 손 자산과 같은 방식).

---

# HANDOFF — 96일차 후속: 방문 달력 오늘 날짜 펜 동그라미 (구현·QA 완료)

확인일: 2026-10-07. 수동 표준 모드, 담당 Claude Code. 시작 HEAD `8d4f084`(origin과 같음). **아래 다음 후보는 실행 승인이 아니다.**

- **변경:** `VisitCalendarDrawer.kt` — 오늘 날짜 숫자 둘레에 기존 `SealInkRed`로 얇은 펜(0.6~0.7dp)을 떼지 않고 2.4~3.2바퀴 휘갈긴 동그라미(`visitTodayPenCircle`, 순수 함수, seed=`visitTodayCircleSeed(date)`). 숫자 Text의 drawBehind라 글자를 덮지 않고, 방문 여부와 무관. 접근성 설명에 ", 오늘" 추가. 새 색·자산·데이터 변경 없음. 실기기 QA 세 번(세로 타원→거의 원, 살짝 아래로, 휘갈긴 한 줄)으로 다듬음.
- **실기기 감각 QA:** 사용자 승인(2026-10-07, "동그라미 크기 딱 마음에 들어").
- **자동검증:** 전체 `testDebugUnitTest` 923/923(XML 95, 실패·오류·skip 0), `@Test` 923개/파일 94개(+helper 1), assembleDebug 성공, diff --check 통과. instrumentation 미실행(불필요). TEST-COVERAGE-MAP 갱신 완료(919→923, 구조 162→163).
- **Git:** 사용자 승인으로 위 코드 1개·테스트 2개·TEST-COVERAGE-MAP·이 HANDOFF를 commit·push. `63fddb3`으로 push, CI run `37570495418` 성공. 직전 `8d4f084`의 CI run `37566877002` 성공 확인. 제외 유지: `postcard_paper_fiber_tile.png`(사용자 결정 대기), `.codex-config.candidate.toml`, `.kotlin/`.
- 다음 후보(실행 승인 아님): 펜 자국·동그라미 위 날짜 글자 가독성 장기 관찰, 95일차 HANDOFF의 CI 성공 기록 보정.

---

# HANDOFF — 96일차: 방문 달력 질감 보강 (구현·QA 완료, `8d4f084`로 commit·push·CI 성공)

- **3단계·전체 실기기 QA: 사용자 승인(2026-10-07, 옅게 조정 후 "귀여워 너무 마음에 들어").** 첫 QA에서 "팔레트에 묻은 물감 느낌"이라 바탕 α0.75→0.5, 획 α0.42→0.4·0.3→0.28로 옅게 조정. 날짜 글자색 규칙은 바꾸지 않음.
- **최종 자동검증:** 전체 `testDebugUnitTest` 919/919(XML 95, 실패·오류·skip 0), `@Test` 919개/테스트 파일 94개(+helper 1), assembleDebug 성공, diff --check 통과. TEST-COVERAGE-MAP 갱신 완료(907→919, 구조 159→162, 96일차 항목 추가).
- **Git 마감 대상:** `VisitCalendarDrawer.kt`, `VisitCalendarTest.kt`, `VisitCalendarPaperPageStructureTest.kt`, `docs/ai/TEST-COVERAGE-MAP.md`, 이 HANDOFF. 제외: `postcard_paper_fiber_tile.png`(사용자 결정 대기), `.codex-config.candidate.toml`, `.kotlin/`. → 이후 `8d4f084`로 commit·push, CI run `37566877002` 성공.
- 다음 후보(실행 승인 아님): 펜 자국 위 날짜 글자 가독성은 장기 사용 중 관찰, 95일차 HANDOFF의 CI 성공 기록 보정(10-06 run 성공 확인됨).

- **2단계 실기기 QA: 사용자 승인(2026-10-07, "이 테이프 때문에 더 질감이 잘 살았어").**
- **3단계 구현(미커밋):** `VisitCalendarMonthGrid` 방문일 표시를 `.background(색, RoundedCornerShape(2.dp))` → `Modifier.visitDayGelPenMark(visitDayFillColor(date, today), visitDayPenSeed(date))`. 색 상수·오늘 진한 색·글자 자동 대비·카오모지·셀 크기 미변경. 자국 = 손떨림 외곽(±0.35dp)의 잉크 바탕 α0.5 + 약한 사선(-20°±3) 획 1.15dp·간격 1.3dp·α0.4 + 각도 바꾼 두 번째 듬성 패스(-12°±3, α0.28). 첫 QA("귀여운데 팔레트에 묻은 물감 같음")로 바탕 α0.75·획 0.42·0.3에서 옅게 조정, 가끔 한 줄을 두 번에 나눠 이음매가 겹침. seed=날짜라 같은 날은 항상 같은 자국. 순수 생성 함수 `visitDayGelPenMark`(기억밀도 코드와 공유 안 함 — 수치만 같은 문구류 느낌으로 맞춤).
- 테스트 추가: `VisitCalendarTest` +3(같은 날 같은 자국·다른 날 다른 자국 / 400일 동안 바탕이 칸 안 손떨림 띠·획 15개 이상·칸 밖 1dp 이내·굵기 0.9~1.4·α0.2~0.55·각도 -30~-5° / 측정 전 null), `VisitCalendarPaperPageStructureTest` +1(채움 색 그대로 중성펜 자국, `.background(` 복귀 금지). 자동검증: gallery 176/176(16 suite), assembleDebug 성공, diff --check 통과. **3단계·전체 실기기 QA 사용자 확인 대기.** 위험: 글자색은 단색 기준 자동 대비(크림 글자)라 칠한 농도가 옅어 보이면 날짜 가독성 확인 필요.
- **1단계 실기기 QA: 사용자 승인("좋아 적당해", 2026-10-07).**
- **2단계 구현(미커밋):** 장 윗변 가운데에 코드로 그린 마스킹테이프 한 조각(`visitCalendarTapePiece`, 순수 함수, seed=`visitCalendarTapeSeed(month)`). 아이보리 #F6EEDC α0.62, 높이 11dp, 길이 40~48dp, 중심 x ±3dp, 각도 ±1~2°, 긴 변은 곧고 양 끝만 서로 다른 톱니(최대 안쪽 2.5dp), 길이 방향 섬유 5~8가닥(#B8A27E α0.05~0.11, 0.35dp). 중심 y=+1.5dp라 위쪽은 장 위 6dp 틈에, 아래는 제목 위 여백에 걸침. 종이 위에 그려 장과 함께 넘어감. 갤러리 민트 테이프 자산은 쓰지 않음.
- 테스트 추가: `VisitCalendarTest` +2(같은 달 같은 조각·이웃 달 다른 조각 / 2020~2030 전 월 각도 1~2°·길이·테이프 안 섬유·좌우 끝 비대칭), `VisitCalendarPaperPageStructureTest` +1(이 달의 테이프를 종이 다음에 그림, 자산 미사용). 자동검증: gallery 172/172(16 suite), assembleDebug 성공, diff --check 통과. **2단계 실기기 QA 사용자 확인 대기.**

## 1단계 기록

확인일: 2026-10-07. 수동 표준 모드(96일차 작업지시서, 담당 Claude Code). 시작 HEAD `71ef5e3`, origin 0/0, tracked clean. **이 문서의 다음 후보는 실행 승인이 아니야.**

- 사용자 확정 방향: 새 종이를 얹지 않고 현재 달력 장(`VisitCalendarMonthPage`) 자체를 손으로 자른 종이처럼(가장자리 약 0.5dp, 달마다 다르고 같은 달은 고정, 그림자 거의 없음). 2단계 테이프는 아이보리~연베이지 반투명·각도 ±1~2°(민트 아님). 3단계 민트 중성펜 출첵. 단계마다 실기기 확인 후 다음 단계.
- **1단계 구현(미커밋):** `VisitCalendarDrawer.kt` — `visitCalendarPaperEdgeOutline`(순수 함수, dp 다각형, 장 안쪽 0~0.5dp 띠, seed=`visitCalendarPaperEdgeSeed(month)`) 모양으로 종이 bitmap을 `ImageShader`+`ShaderBrush`로 채운 path로 그림(clipPath는 경계 계단 현상 우려로 미사용). 기존 가운데 자르기 배율 계산은 shader matrix로 그대로 옮김. 밑에 접촉 그림자 2겹(0.3/0.8dp α0.06, 0.5/1.4dp α0.03, 색 #3B3226). 장 크기·배치·넘김 애니메이션·날짜 grid·방문 표시·색·월/연도 선택 화면 미변경. 테이프·중성펜 미착수.
- 테스트: `VisitCalendarTest` +4(같은 달 같은 모양, 이웃 달 다른 모양, 2020~2030 전 월 장 밖 이탈 없음·0.5dp 띠 안·반듯한 사각형 아님, 측정 전 빈 윤곽). `VisitCalendarPaperPageStructureTest`: 장 안 종이 그리기 확인 문자열을 `drawImage(` → `ImageShader(paper)`로 바꾸고(의미 동일: 종이가 장 안에서 그려짐), +1(이 장의 month seed로 오린 path로 그림, clipPath 없음). 윤곽선 금지(`Stroke(`/`.border(`)·늘리기 금지 검사는 그대로 통과.

| 구분 | 상태 |
|---|---|
| 구현 | 1·2·3단계 완료 (3단계 옅게 조정 포함) |
| 로컬 자동검증 | 실행: `testDebugUnitTest --tests com.postcardmemory.ui.gallery.*` 176/176(16 suite) → 최종 전체 919/919(XML 95), assembleDebug 성공, diff --check 통과 |
| 실사용 기기 instrumentation | 미실행(불필요·금지) |
| 실기기 감각 QA | 1·2·3단계·전체 사용자 승인 |
| TEST-COVERAGE-MAP | 갱신 완료(907→919, 구조 159→162) |
| commit / push / CI | 미승인·미실행 (사용자 승인 대기) |

- 미커밋 유지: `postcard_paper_fiber_tile.png`(사용자 결정 대기), 보호 untracked 2개.

---

# HANDOFF — 95일차: 메인 상단 시계·손·커피잔 배치 후속 수정

## 마감 승인

2026-10-06 사용자가 현재 배치의 실기기 확인 완료를 알리고 이 상태의 commit·push를 명시 요청함. 실기기 감각 QA는 사용자 확인 완료. 로컬 빌드는 기존 Gradle 플러그인 해석 오류로 실행 불가 상태를 유지. 이번 코드 2개·이미지 3개와 이 HANDOFF만 Git 마감 대상이며, 종이 질감 이미지·설정 후보·.claude/·.kotlin/는 제외. GitHub Actions는 push 후 JVM 테스트·debug APK 빌드·instrumentation APK 컴파일 결과를 확인할 예정이며 실제 instrumentation 실행은 아님.

## 최신 후속 변경 — 왼손의 화면 밖 진입

2026-10-06 사용자 직접 요청으로 왼손을 100×75dp → 112×84dp(12% 확대), 장면 기준 x=-8dp·y=-5dp로 이동. 손을 시계 묶음 밖의 장면 레이어로 옮기되 기존 장면 `clipToBounds` 안에서만 그림. 호출부의 왼쪽 16dp 여백을 제거하고 시계 묶음 내부에 같은 16dp 여백을 옮겨 시계·숫자창의 화면 위치를 유지. 오른손·컵·김 설정 유지. 손목 왼쪽 8dp와 상단 5dp는 화면/장면 경계에서 잘림. 검지 끝은 화면 약 (99.5,38.3)dp, 스누즈 버튼 영역 약 x90~124dp·y39~48dp에 맞춤. 숫자창 시작 x84.5dp·y58.6dp 및 메뉴가 있는 별도 상단 줄을 유지. 실제 접촉감·손가락과 숫자창 간격은 실기기 확인 대기.

검증: diff 공백 검사 통과. assembleDebug 오프라인 실행은 이전과 같은 foojay 0.10.0 플러그인 해석 실패로 컴파일 전 실행 불가. 기존 테스트의 입력·의미와 시계 로직 변경 없음; 테스트 재실행 없음. 실기기 감각 QA·instrumentation·CI·설치·commit·push 미실행. 아래 기록의 왼손 크기·배치는 이전 단계 기록이야.

## 최신 후속 변경 — 오른쪽 진입 장면과 옅은 김

2026-10-06 사용자 직접 요청으로 오른손+컵을 12% 확대(105.28×140.373dp). viewport 높이는 108dp로 유지해 손목 하단 약 32dp를 자름. Row 대신 높이 122dp의 clipped Box 안에서 시계는 BottomStart, 컵은 BottomEnd 고정. `GalleryScreen.kt`의 해당 호출부 오른쪽 16dp 여백만 제거해 컵·손목이 화면 오른쪽 끝에 닿음. 왼쪽·상하 여백 유지. 자산·시계 숫자·날짜·갱신 로직 미변경.

사용자가 재구현을 허용한 김만 `GalleryCoffeeSteam`으로 추가: 2줄, 두께 0.65dp, 최대 alpha 0.13, 4.8초 주기, 상승 8dp, 좌우 흔들림 최대 0.7dp, 선 길이 11dp. 두 줄의 위상을 반 주기 분리하고 시작·끝 opacity 0으로 반복 경계가 튀지 않게 함. 컵 이미지 상단 투명 영역 안에 그림. 애니메이션 상태는 Canvas draw에서 읽으며 손·컵 자체는 정적.

검증: `git diff --check` 통과. `assembleDebug` 및 `GalleryRetroClockTest`·`GalleryPaperBackgroundStructureTest` 실행 시도는 기존 foojay 0.10.0 플러그인 해석 실패로 컴파일·테스트 본문 진입 전 실행 불가. 실기기 감각 QA 미실행(오른쪽 진입 인상·손목 경계·김 강도·좁은 화면 겹침 확인 대기). instrumentation 미실행(불필요), CI 미실행, 설치·commit·push 미실행. 테스트 수·보호 범위 변경 없음. 아래 배치 수치는 이전 단계 기록이야.

확인일: 2026-10-06, 담당 Codex. 사용자 직접 구현 요청으로 기존 미커밋 작업을 이어받음. 브랜치 `feature/photo-sticker`, HEAD와 로컬 origin ref 모두 `cf009c41d90fb57c5f3a7e69e95712f78081fc42`(원격 서버 조회 미실행). 아래 이전 구현 보고의 자산 크기·배치·검증 결과는 현재 후속 수정의 검증 결과가 아님.

- 실제 교체 자산: 몸체 1695×928, 왼손 1448×1086, 오른손+컵 1086×1448. 자산 자체는 이번 작업에서 수정하지 않음.
- `GalleryRetroClock.kt` 배치만 후속 수정: 왼손 100×75dp, 몸체 앞 여백 54dp·위 여백 30dp. 검지 끝 약 (96,39)dp를 스누즈 버튼 영역에 맞춤. 오른손+컵 94×125.333dp를 94×108dp 영역에 위 정렬하고 하단 약 17dp를 clip. 선반선 앞 6dp 간격 제거로 손목이 선에서 잘리도록 배치. Row 간격 8dp, 장면 높이 122dp·가로 합계 324dp(외부 좌우 여백 제외). 좁은 폭에서 잘릴 가능성은 실기기 확인 필요.
- Row와 컵 영역에 `clipToBounds()` 적용. 큰 컵 이미지는 viewport 안에서만 그려 목록 위로 overflow하지 않음. 숫자·날짜 계산·7세그 렌더러·1초 갱신 루프는 이번 후속 수정에서 변경하지 않음. 기존 커피잔 코드·김 애니메이션 제거 상태 유지.
- 로컬 자동검증: `git diff --check` 실행·통과. `assembleDebug`와 `GalleryRetroClockTest` 실행 시도는 기존 settings 플러그인 `org.gradle.toolchains.foojay-resolver-convention:0.10.0` 해석 실패로 실행 불가(오프라인·일반 실행 동일). 컴파일·테스트 본문에 도달하지 못함. 기존 XML 결과는 이번 성공 근거로 사용하지 않음. Gradle·dependency 수정 없음.
- 실사용 기기 instrumentation 미실행(이번 배치 확인에 불필요), 실기기 감각 QA 미실행·사용자 확인 대기, GitHub Actions CI 미실행. APK 설치·기기 상태 변경 없음. 테스트 수·의미·보호 범위 변경 없어 TEST-COVERAGE-MAP 수정 없음.
- 기존 사용자 변경과 보호 untracked 유지. commit·push 미실행. 남은 확인: 빌드 환경에서 컴파일 확인, 실제 화면에서 손끝 접점·손목 절단선·숫자 가독성·좁은 화면 가로 배치 확인.

---

# HANDOFF — 95일차: 메인 상단 시계·커피잔을 잡지 오림 정적 장면으로 교체

확인일: 2026-10-06. 수동 표준 모드(95일차 작업지시서, 담당 Claude Code). 시작 HEAD `cf009c4`, origin 0/0, tracked clean. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 확인된 상태 (상단 시계)

- 대상: `ui/gallery/GalleryRetroClock.kt`의 `GalleryRetroClock`·`GalleryRetroClockFace`(호출부 `GalleryScreen.kt` topBar, 미변경).
- 자산(사용자 제작, `drawable-nodpi`, untracked → commit 대상): `home_clock_collage_body.png` 1695×928, `home_clock_left_snooze_hand.png` 1683×935, `home_right_hand_coffee_cup.png` 1165×1350. 모두 RGBA 실제 투명 배경, 비율이 사전 규격(168×92 / 72×40 / 76×88dp)과 일치.
- 변경: 크림 `PaperTray` 바디·LCD 패널 바탕/여백 제거 → 몸체 이미지 숫자창(원본 x145~1567, y290~730px = 14.5/28.6dp, 141×44dp) 가운데에 기존 7세그 숫자·AM/PM·구분선·날짜를 그대로 배치(`wrapContentSize(unbounded)`). 날짜 Text에 `lineHeight = 10.sp`만 추가(기본 24sp 줄칸이 숫자창을 넘침; 글자 크기·색·간격 유지). 왼손은 시계 묶음 Box 안 왼쪽 위(몸체를 26dp 오른쪽·10dp 아래로 둠) — 손끝이 스누즈 버튼 위, 손 아래 끝은 숫자창 테두리보다 위. 오른손+컵은 Row 옆, bottom 8dp 들어 시계 다리와 같은 높이.
- 삭제: 선 아이콘 커피잔(`RetroClockCoffeeCupIcon`), 김 애니메이션(`RetroClockCoffeeSteam`, infinite transition), 관련 상수·import, `RetroClockPanelColor`. 시간 갱신·접근성 설명·선반선·`clearAndSetSemantics` 유지. 클릭·애니메이션 없음.
- 모든 이미지는 Row 측정 범위 안(offset/graphicsLayer/zIndex 없음) → 아래 목록은 덮지 않고 헤더가 커져 밀려 내려감. 시계 묶음 높이 약 79.5dp → 102dp(약 +22.5dp, 사용자 사전 허용). 가로 합계 약 314dp(16+194+12+76+16).
- 위험: 이미지 3장 원본 해상도 decode(각 약 4~6MiB ARGB, 기존 손 자산과 같은 방식). 숫자창 상하 여백이 거의 0(내용 43.5dp / 창 43.6dp)이라 빡빡해 보일 수 있음 — 실기기 판단. 시스템 글꼴 크게 설정 시 날짜 줄이 창 테두리로 넘칠 수 있음(미확인).

| 구분 | 상태 |
|---|---|
| 구현 | 완료 |
| 로컬 자동검증 | 실행: assembleDebug 성공, testDebugUnitTest `com.postcardmemory.ui.gallery.*` 164/164(16 suite, 실패·skip 0, `GalleryRetroClockTest` 13·`GalleryPaperBackgroundStructureTest` 5 포함), diff --check 통과. 전체 suite는 미실행(변경이 이 파일 private UI에 한정) |
| 실사용 기기 instrumentation | 미실행(불필요·금지) |
| 실기기 감각 QA | **사용자 확인 대기** |
| TEST-COVERAGE-MAP | 변경 없음(테스트 변화 없음) |
| commit / push / CI | 미승인·미실행(QA 후 사용자 승인 필요) |

- 미커밋 유지: `postcard_paper_fiber_tile.png`(사용자 결정 대기), 보호 untracked 2개.

## 다음 후보 (실행 승인 아님)

- QA 결과에 따른 손·컵 위치/크기 미세조정, 헤더 높이 축소 여부, 이미지 해상도 축소(메모리).

---

# HANDOFF — 95일차: 갤러리 월 라벨 뒤 마스킹 테이프 띠

확인일: 2026-10-05. 수동 표준 모드(95일차 추가 작업지시서, 담당 Claude Code). 시작 HEAD `7547e2c`, origin 0/0. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 확인된 상태 (월 라벨)

- 대상: `GalleryScreen.kt`의 `GalleryMonthHeader()`(월별 3열 보기 `GalleryMonthlyGridPage` 안 full-span item). 날짜 그룹핑 `monthSectionsFor`, 정렬, `yyyy년 M월` 형식, 스크롤·클릭, 검색 날짜 문자열 미변경.
- 변경: 헤더의 화면 폭 `PaperField` 단색 배경 제거(뒤의 `GalleryPaperBackground`가 보임). "2026년 8월" Text만 Box로 감싸 `drawBehind`로 `gallery_date_paper_strip.png`를 깔고, 투명 여백을 뺀 영역(srcOffset 89,172 / srcSize 2011×379)만 상자 크기에 맞춰 그림. 안쪽 여백 좌우 18dp·위아래 5dp, 최소 폭 136dp, 글자 가운데. Row 여백 end 16dp·위아래 6dp(이전 가로 16·세로 10) — 띠 왼쪽 끝이 그리드 첫 칸과 맞음. "N장"(12sp, InkSecondary)과 하단 1dp `PaperDivider` 구분선 유지. 텍스트 내용·14sp·SemiBold·InkPrimary 유지.
- 자산: 사용자 제작 `drawable-nodpi/gallery_date_paper_strip.png`(2172×724 RGBA, 띠 밖 투명, 띠 안 alpha≈253). 같은 이름으로 3번 교체됨 — 크림 종이(배경과 같은 색이라 거의 안 보임) → 얼룩 갈색 종이 → **현재 민트 마스킹 테이프**(사용자 최종 선택). 잘라내기 상수는 자산을 바꿀 때마다 다시 재야 함(KDoc에 명시).
- 그림은 페이지에서 `ImageBitmap.imageResource`로 한 번만 decode(item별 decode 없음). 2172×724 ARGB라 갤러리 체류 중 약 6MiB — 자산 축소는 후속 후보(사용자 결정).
- 목업: `docs/ai/mockups/gallery-date-paper-strip-mockup.html`(1dp=1px, 실제 갤러리 타일·띠 자산 상대 경로, 새 모양/이전 모양/구분선 토글). 웹 근사라 실기기 인상과 다를 수 있음.

| 구분 | 상태 |
|---|---|
| 구현 | 완료 |
| 로컬 자동검증 | 실행: testDebugUnitTest 907/907(95 suite, 실패·skip 0), assembleDebug 성공, diff --check 통과. `GalleryMonthlyGridStructureTest`(헤더 재사용)·`GalleryPaperBackgroundStructureTest`(페이지가 종이 가리지 않음) 통과 |
| 실사용 기기 instrumentation | 미실행(불필요·금지) |
| 실기기 감각 QA | 실행: 사용자 Android Studio 설치(lastUpdateTime 17:10:12, firstInstallTime 14:19:35 유지, DB·엽서 파일 6개 확인) 후 승인. 구분선 유지(사용자가 제거 요청 안 함) |
| TEST-COVERAGE-MAP | 변경 없음(테스트 변화 없음) |
| commit / push / CI | 사용자 승인으로 진행 — 결과는 Git·CI와 완료보고로 확인 |

- **94일차 실기기 데이터 사고 — 복구 종료(2026-10-05 사용자 결정):** 사용자가 백업 복구를 더 진행하지 않기로 했어. 현재 기기 데이터(7·8월 클라우드 복원본 + 이후 사용분)를 기준 상태로 보존·관리해. 복구 시도·백업 확인 요청은 하지 않아. 실사용 기기 보호 규칙(uninstall·`pm clear`·데이터 초기화 금지, `install -r`은 승인 후, 설치 후 firstInstallTime 확인)은 그대로 적용해. 사고 경위 원문은 아래 94일차 절에 보존.
- 미커밋 유지: `postcard_paper_fiber_tile.png`(사용자 결정 대기), 보호 untracked 2개.

## 다음 후보 (실행 승인 아님)

- 구분선 제거 여부 재검토, 띠 자산 해상도 축소(메모리), 미사용 `ic_launcher_foreground.png`·`postcard_paper_fiber_tile.png` 정리.

---

# HANDOFF — 94일차 추가: 앱 아이콘 손그림(손바닥 + 봉투)으로 교체

확인일: 2026-10-05. 수동 표준 모드(94일차 추가 작업지시서, 담당 Claude Code). 시작 HEAD `ad75b68`, origin 0/0. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 확인된 상태 (아이콘)

- 원본: 사용자 제작 `app/src/main/res/drawable-nodpi/postcard_app_icon_hand_envelope.png`(1254×1254 PNG, RGB 불투명, 아이보리 종이 배경 전면, 약 2.0MB). 크롭·리사이즈·보정 없이 그대로 사용.
- 구조: minSdk 26이라 adaptive icon(`mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, 내용 동일)만 사용, legacy mipmap PNG·monochrome 없음. 기존 구조·리소스 이름 유지.
- 변경: 두 adaptive XML의 foreground를 `@drawable/postcard_app_icon_hand_envelope`로 연결, `drawable/ic_launcher_background.xml` 배경 `#FFFFFF` → 그림 종이색 `#F9EEDF`(foreground가 전면을 덮어 평소엔 안 보이고, 런처 애니메이션 가장자리 흰 틈 방지용).
- 안전 영역: 그림 bbox x 262–982, y 428–890(1254 기준). 가장 먼 점은 손목 왼쪽 선 끝 33.7dp/108 — 66dp 보장 영역(33dp)보다 0.7dp 밖, 72dp 표시 원(36dp) 안. 손가락·봉투는 전부 안쪽.
- 기존 `drawable/ic_launcher_foreground.png`는 미참조가 됐지만 삭제하지 않음(정리 여부 후속 후보).

| 구분 | 상태 |
|---|---|
| 구현 | 완료 |
| 로컬 자동검증 | 실행: assembleDebug 성공(processDebugResources 포함, APK 안 icon/roundIcon 참조와 새 PNG 확인), testDebugUnitTest 907/907, diff --check 통과. lint 별도 미실행 |
| 실사용 기기 instrumentation | 미실행(불필요·금지) |
| 실기기 감각 QA | 실행: 사용자 Android Studio 설치(lastUpdateTime 15:21:28, firstInstallTime 14:19:35 유지 = 데이터 보존, DB·엽서 파일 6개 확인) 후 "내가 원하던 느낌 그대로" 승인 |
| TEST-COVERAGE-MAP | 변경 없음(테스트 변화 없음) |
| commit / push / CI | 사용자 요청으로 진행 — 결과는 Git·CI와 완료보고로 확인 |

- 아래 "94일차 후반부" 절의 **실기기 데이터 사고(9월 이후 데이터 소실, 사용자 백업 확인 대기)** 위험은 그대로 유효해.
- 미커밋 유지: `postcard_paper_fiber_tile.png`(사용자 결정 대기), 보호 untracked 2개.

---

# HANDOFF — 94일차 후반부: 엽서 꾸미기 화면 앱 배경 종이 (정전 복구 후 방향 정정) + 실기기 데이터 사고 기록

확인일: 2026-10-05. 수동 표준 모드(94일차 정전 복구 작업지시서, 담당 Claude Code). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 확인된 상태

- 브랜치 `feature/photo-sticker`, 시작 HEAD `2f7e667`(기억밀도), `git fetch` 후 origin 대비 `0/0`.
- **현재 코드 변경은 하나:** `ui/detail/DetailScreen.kt` 루트 Box 배경 `.background(ScreenBackgroundGray)` → `editorPaperBackground(gallery_paper_tile)`. 갤러리 `GalleryPaperBackground`와 같은 문법(같은 밑색 `PaperCanvas` → `ImageShader` Repeated, 360.dp, 가로·세로 같은 배율). 엽서 꾸미기 화면 **엽서 바깥 앱 배경**에만 보이고, 엽서 캔버스·저장·공유 이미지·뒷면·좌표·gesture·Room·OUTPUT_SIZE는 미변경. 하단 도구 dock은 불투명 단색 유지(갤러리 시계 header와 같은 역할).
- 엽서 캔버스에 넣었던 종이 결(`PostcardPaperTexture`, `drawBaseContent` 인자, exporter 반영)은 사용자 방향 정정 후 **전부 되돌렸어**(경과는 아래 이전 기록 절). 현재 코드에 흔적 0건.
- `drawable-nodpi/postcard_paper_fiber_tile.png`(사용자 제작 거친 종이)는 **미사용·untracked로 보존**하고 commit에 넣지 않았어. 쓸지·지울지는 사용자 결정.
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 수정·stage 없음.

## 독립 상태

| 구분 | 상태 | 근거 |
|---|---|---|
| 구현 | 완료(꾸미기 화면 앱 배경만) | 위 현재 상태 |
| 로컬 자동검증 | 실행: testDebugUnitTest 907/907(95 suite, 실패·skip 0), assembleDebug 성공, `git diff --check` 통과 | 2026-10-05 14:26 빌드 |
| 실사용 기기 instrumentation | 미실행 | 지시서 금지 |
| 실기기 감각 QA | 실행: 사용자 "종이 배경 일단 나는 만족" | Android Studio 14:30 overlay 설치본으로 사용자 확인. 항목별 관찰 상세는 미확인 |
| TEST-COVERAGE-MAP | 변경 없음 | 테스트 수·보호 범위 변화 없음. `GalleryPaperBackgroundStructureTest`는 GalleryScreen.kt 안 참조만 셈 |
| 사용자 데이터 | **사고 발생 — 아래 절** | 9월 이후 기기 엽서 데이터 소실, 7·8월 클라우드 백업으로 복원된 상태 |
| commit / push / CI | 사용자 승인(작업지시서 12절, 실기기 만족)으로 진행 — 결과는 Git·CI와 완료보고로 확인 | |

## 🚨 실기기 데이터 사고 (2026-10-05) — 현재 위험

- 경위·근거는 아래 이전 기록 절의 "실기기 데이터 사고" 항목 원문 참고. 요약: 14:16:40 기기 홈 런처에서 앱 제거(앱 데이터 삭제) → 14:19:35 Android Studio Run이 새 설치 → Google 자동 백업 복원으로 7·8월 시점 데이터가 들어옴. Claude의 adb 명령에는 제거가 없었고(`install -r` 2회 모두 Retain data), Gradle `connected*`·uninstall·`pm clear` 미실행.
- 9월 이후(사용자 말로 샘플 위주) 엽서 데이터의 복구 수단은 확인되지 않음. 갤러리 내보내기 이미지 0장. 사용자 측 후보: 구글 포토(완성 이미지), 삼성 클라우드·Smart Switch 백업 — **사용자 확인 대기.**
- 재개 조건: 복구 시도는 백업 존재 확인 → 계획 설명 → 별도 승인 후에만. 그 전엔 기기 쓰기·재설치·데이터 조작 금지.
- 교훈(운영): 설치가 꼬일 때는 사용자에게 "앱 삭제 금지"를 먼저 명시하고, 승인된 `adb install -r` 경로를 우선한다.

## 다음 후보 (실행 승인 아님)

- 사용자 백업 확인 결과에 따른 복구 계획 수립.
- 앱 내 "엽서 전체 백업 내보내기" 기능 검토(데이터 사고 재발 방지) — 별도 작업지시 필요.
- 종이 배경을 다른 편집 화면으로 넓힐지, 사용자 제작 거친 종이를 쓸지 — 제품 판단.

---

# 이전 기록 — 94일차 전반부·후반부 경과
# HANDOFF — 94일차 전반부: 기억밀도 실기기 만족 확인, Git 마감

확인일: 2026-10-05. 수동 표준 모드, 94일차 작업지시서에 따라 Codex가 93일차 미커밋 작업을 인수했어.

## 현재 확인된 상태

- 시작 브랜치 `feature/photo-sticker`, HEAD `efd5d133155b230a06d4b7cd60fa9fe60a2f64eb`. 원격 실조회 HEAD도 동일했어.
- 93일차 production 1개, 테스트 2개, 문서 2개, 목업 3개의 변경을 이어받았어. 기존 변경을 삭제하거나 재구현하지 않았어.
- 사용자 실기기 감각 QA: **2026-10-05 사용자가 “실기기 확인했어 아주 만족스러워!”라고 확인했어.** 현 디자인을 승인한 것으로 반영하고 수치 조정 없이 유지해.
- 중성펜 외곽선·사선 칠·미세한 손떨림, 비례형 1장당 6dp, 20장 최대 120dp, 초과는 작은 `+`, 얼굴·하트 없음, 월 숫자, 월별 고정 seed를 유지해.
- 낮은 값 1~2장·20장·21장 이상을 각각 관찰했는지와 전환 성능·획 안정성의 항목별 상세 소견은 사용자 메시지에 없어. 전체 만족 보고와 개별 항목의 증거를 구분해.
- 코드상 비례·상한·초과 조건과 획 결정성은 관련 자동 테스트가 보호해. 이 근거를 개별 실제 기기 관찰로 확대하지 않아.
- 94일차 production/test 수정 없음. 93일차 XML 95개를 재집계해 **907개, 실패·오류·skip 0**을 확인했어(10월 4일 실행 결과, 오늘 재실행 아님).
- `.codex-config.candidate.toml`, `.kotlin/`, `.claude/settings.local.json`은 수정·stage·commit하지 않아. 전역 Git ignore 접근 제한으로 `.claude/`도 untracked로 보였어.
- 93일차 HANDOFF에서 92일차 이력 표가 교체된 오류를 HEAD 원문으로 보존했어. 기억밀도 검증 결과는 이 현재 절과 93일차 내용으로 구분해.

## 독립 상태와 마감 조건

| 구분 | 상태 | 근거 |
|---|---|---|
| 기억밀도 구현 | 완료 | 93일차 변경 유지, 오늘 미세조정 없음 |
| 로컬 자동검증 | 실행(93일차): 907/907 | 기존 XML 재집계, 관련 클래스 14+7 |
| 로컬 build | 미실행(94일차) | production/test 추가 수정 없음, 최종 커밋 CI에서 확인 |
| 실기기 감각 QA | 실행: 사용자 전체 만족 확인 | 항목별 관찰 상세는 미확인 |
| 실사용 기기 instrumentation | 미실행 | 오늘 지시에서 금지 |
| 사용자 데이터 | Codex 설치·삭제·데이터 쓰기 없음 | 기기 확인 명령만 실행, 설치 없음 |
| commit / push / CI | 완료: `2f7e667`, push 성공, CI `37259538200` success | JVM 테스트·assembleDebug·assembleDebugAndroidTest 모두 success |
| 후반부 종이 질감 | (당시 기록) 엽서 캔버스 1차 구현 — 이후 방향 정정으로 되돌림, 현재 상태는 문서 맨 위 | 아래 "종이 질감 1차 구현" 절 |

## 후반부 조사와 멈춘 범위

- `DetailScreen.kt`의 정사각형 앞면 Box가 실제 엽서 영역이야. `postcardPreviewSize`와 root 위치를 측정하고 사진 pan/zoom 및 꾸미기 객체 좌표에 사용해. 종이 작업으로 이 측정·gesture·좌표는 바꾸지 않아.
- 앞면은 `PostcardPreviewContent` → `PostcardRenderSpec.drawBaseContent`로 사진·배경·패턴·글귀를 그려. 그 뒤 마스킹테이프 → 사진 스티커 → 도장 등 개별 객체 레이어가 같은 Box에 올라가. 도장은 `SealPreviewContent`를 사용해.
- `PostcardImageExporter.createPostcardBitmap`와 `PostcardTemplateRow`도 같은 `drawBaseContent`를 사용해. 배경은 `drawBackground`에서 `canvas.drawColor`로 불투명하게 칠해. 외부 Box 아래에 종이를 추가하면 가려지고, 최상단에 덮으면 사진까지 물들이므로 둘 다 적절하지 않아.
- 기존 asset 2개를 파일·크기·사용처·이미지로 확인했어. `gallery_paper_tile.png`: 1254×1254, 2,668,364 bytes, RGBA 약 6.0MiB. `visit_calendar_paper.png`: 1122×1402, 2,431,538 bytes, RGBA 약 6.0MiB. 갤러리는 ImageBitmap과 Repeated shader로 같은 배율의 타일을 사용하고, 달력은 월 애니메이션 밖에서 로드해 장들이 공유해.
- 갤러리 타일은 기존 seamless 근거가 있어 재사용 후보야. 현재 자산은 섬유와 누런 기운이 편집 목표보다 강하므로 원본 그대로 적용은 권장하지 않아. 달력 자산은 한 장용이고 seamless 근거가 없어서 반복 타일로 임의 사용하지 않아.
- 자산 파일 재사용은 새로운 bitmap 할당이 0이라는 뜻이 아니야. editor의 decode 수명·cache 소유와 기존 갤러리와의 공유 여부는 구현안 확정 후 검증해야 해. 현재 새 asset·bitmap·상태를 추가하지 않았어.
- **제품 판단 대기:** 화면 전용 종이인지 완성 이미지 포함인지 사용자에게 선택을 요청했어. 화면 전용이면 export는 기존 배경을 유지해. 공통 렌더러에 기본 종이를 적용하면 기존 엽서의 표시·재내보내기 인상도 달라져. 작업지시서 18·24절에 따라 이 의미를 임의로 결정하지 않고 production 수정을 멈췄어.
- 권장 출발점은 저장·export 계약을 유지하는 화면 전용 최소 정적 확인이야. 적용 위치는 사진·글귀 아래의 배경 단계, 범위는 실제 엽서 캔버스만이고 툴바·저장 버튼·navigation은 제외해. 아직 구현이나 실기기 QA는 하지 않았어.
- UI 역할은 Advanced / Custom Editor의 캔버스 물성 보강과 Canvas Object Selection의 기존 조작 유지야. 진입·선택·객체 행동·Global Save는 현행 유지, 신규 toolbar/container/state/schema 없음. 저장 포함 여부 확정 후 구현 전 문법 보고를 마쳐야 해.
- TEST-COVERAGE-MAP은 후반부 테스트 수·보호 범위 변화가 없어 추가 수정하지 않았어. CI에 나온 action/runner deprecation 안내는 범위 밖이고 workflow를 변경하지 않았어.

## 종이 질감 1차 구현 (94일차 후반, Claude Code, 미커밋)

- **사용자 결정(94일차 구현 지시):** 화면 전용이 아니라 저장·공유에도 포함(A안), 기존 엽서 전체 적용 허용, 엽서별 ON/OFF 없음, 새 asset 없이 `gallery_paper_tile.png` 재사용, 앞면만. 위 39행의 "화면 전용 권장"은 Codex 조사 시점 의견이고 이 결정으로 대체됐어.
- 새 파일 `utils/PostcardPaperTexture.kt`: 타일을 한 번 decode해 평균 휘도보다 어두운 곳/밝은 곳을 ALPHA_8 마스크 2장으로 만들고 원본 bitmap은 즉시 recycle. 검정(최대 alpha 18/255)·흰색(최대 8/255)을 SRC_OVER로 엽서 한 장(LOGICAL_SIZE)에 타일 한 장으로 그려. 색은 버려서 배경 hue를 유지해. `PorterDuff.Mode.MULTIPLY`는 alpha까지 곱하는 modulate라 쓰지 않았어.
- `PostcardRenderSpec.drawBackground`: `drawColor` → **종이** → 패턴 → 안쪽 테두리 → (drawBaseContent) 사진·글귀·날짜. `drawBaseContent`에 기본값 없는 `paperTexture` 인자 추가.
- 호출부: `DetailScreen`은 앞↔뒤 전환 밖(Column 안, `postcard?.let` 앞)에서 `remember`로 한 번 load, 실패 시 종이 없이 그림. `PostcardImageExporter.createPostcardBitmap`은 IO에서 load→사용→`release`, 실패 시 내보내기 실패(화면과 다른 결과를 조용히 내보내지 않음). 휴면 `PostcardTemplateRow`는 인자만 관통(현재 호출부 없음).
- 미변경: 뒷면·PaperSurface, `postcardPreviewSize`/좌표/gesture/hit/누끼, OUTPUT_SIZE, Room/schema/migration/metadata.
- 자동검증(2026-10-05 로컬): compileDebugKotlin, testDebugUnitTest **907/907, 95 suite, 실패·skip 0**, assembleDebug, assembleDebugAndroidTest 성공. 테스트 추가·변경 없음 → TEST-COVERAGE-MAP 변경 없음. `GalleryPaperBackgroundStructureTest`는 GalleryScreen.kt 안 참조만 세므로 영향 없음.
- **정전 후 재개(2026-10-05):** 작업트리 생존 확인 결과 위 구현 전부 그대로 남음(재작성 0줄). 1차 강도는 실측 평균 검정 약 2.4/255(0.9%)·흰색 1.3/255라 사용자 육안으로 보이지 않음 → 사용자 선택 "중간"으로 상수만 상향: SHADE 4.0/최대 64, LIFT 2.0/최대 24(타일 기준 평균 검정 약 9.7/255≈3.8%, 흰색 약 5.1/255). 구조·호출부 불변. 재검증 testDebugUnitTest 907/907(95 suite, 실패·skip 0), assembleDebug 성공. 첫 Android Studio 설치는 기기에 반영되지 않았음(lastUpdateTime 2026-09-30 그대로) — QA 전 기기 업데이트 시각 확인 필요.
- **실기기 결과(2026-10-05 14:0x):** `adb -s R3KYB00HAYY install -r` 사용자 승인 후 성공(lastUpdateTime 14:01:50, firstInstallTime 9/7 유지, DB·files 디렉터리 존재 확인). 꾸미기 화면 캡처를 3배 확대해야만 결이 보이고 **사용자 맨눈으로는 1·2차 강도 모두 보이지 않음.** 원인 분석: 타일 자체 휘도 편차가 작음(std 6.9/255), 엽서 전체에 늘린 뒤 화면 축소로 섬유가 평균화됨, 진한 배경에서 검정 overlay 효과 미미, 격자 패턴이 시선 점유. 같은 가설(강도 상향) 2회 실패 → 관련 production 수정 STOP.
- **재개 조건:** 사용자가 더 거친 질감의 새 이미지 자원을 직접 제작해 경로를 전달 예정(정사각형 약 1024², 색 무관, 잔결 위주, 비네팅·투명 없음 안내함). 도착하면 `drawable-nodpi`에 새 이름으로 추가(갤러리 타일은 GalleryScreen 전용 유지) → 휘도 편차 실측 → 강도 재산정 → 빌드·JVM → 승인 후 install -r → 사용자 맨눈 판정. 2026-10-05 당일 강도 상수 4.0/64·2.0/24는 미커밋 작업트리에 남아 있고 새 자원 기준으로 다시 정할 값.
- **새 자원 적용(2026-10-05 14:15):** 사용자 제작 `drawable-nodpi/postcard_paper_fiber_tile.png`(1254², RGB 회색, 2,997,641 bytes, 평균 휘도 197.9·std 11.73, 4분면 평균 차 0.4 이내=비네팅 없음). `PostcardPaperTexture`만 새 자원을 load하고 갤러리는 `gallery_paper_tile` 유지. PC 합성 비교(청록·분홍·크림 × 강도 4종) 후 사용자 선택 "3열": SHADE 6.0/최대 96, LIFT 3.0/최대 40. testDebugUnitTest 907/907(95 suite), assembleDebug 성공, 승인된 install -r 성공(lastUpdateTime 14:15:48, firstInstallTime 유지, DB 존재). 사용자 맨눈 판정 대기.
- **🚨 실기기 데이터 사고(2026-10-05, READ-ONLY 조사로 확인):** 사용자 보고 "9월 샘플 엽서가 사라지고 7·8월 엽서로 바뀜". 기기 logcat·Android Studio idea.log 근거 타임라인: 14:15:48 Claude `adb install -r` 성공(Retain data, firstInstallTime 9/7 유지 확인) → 14:16:40 **기기 홈 런처(uid 10161 com.sec.android.app.launcher)에서 ACTION_DELETE 제거 화면 실행, 14:16:41 `deletePackageX` result 1(앱·앱 데이터 삭제)** — adb 경로 아님 → 14:19:35 Android Studio Run이 패키지 없음(`DUMP_UNKNOWN_PACKAGE`)으로 새 설치 → `BackupManagerService restoreAtInstall`이 Google 클라우드 백업(restoreSet 3835d7b9334e27ea)을 복원해 7·8월 시점 DB·파일이 들어옴(새 uid u0_a525, firstInstallTime 14:19:35). `Pictures/PostcardMemory` 갤러리 내보내기 이미지는 MediaStore 조회 결과 0장. 세션 PC에는 앱 데이터 사본 없음(받아둔 건 설치 APK뿐). 9월 데이터의 기기 내 복구 수단은 현재 확인되지 않음. 사고 후 기기 쓰기·재시도·복구 시도 없음. 복구 후보(삼성 클라우드·Smart Switch 등 사용자 측 백업) 확인 전 앱 사용·추가 설치·백업 동작 보류 권고.
- **⚠ 방향 정정(2026-10-05 14:2x, 사용자):** 사용자가 원한 "종이 질감"은 엽서 캔버스 배경이 아니라 **엽서 꾸미기 화면의 앱 배경(엽서 바깥, `ScreenBackgroundGray`)**이었음. 작업지시서가 앞면·exporter 기준으로 적혀 있었고, 1차 "안 보인다" 때 위치를 되묻지 않고 강도만 올린 것이 원인. 엽서 배경에 결이 들어간 빌드(14:01·14:15 설치) 동안 갤러리 저장·공유 0장 확인(MediaStore 읽기 조회). Room·저장 데이터 영향 없음(렌더 시점 overlay).
- **사용자 승인으로 되돌림:** `DetailScreen`·`PostcardTemplateRow`·`PostcardImageExporter`·`PostcardRenderSpec` 4파일을 `git restore`(변경분 전부가 이 작업 것임을 diff로 확인, 백업 diff는 세션 scratchpad), `utils/PostcardPaperTexture.kt` 삭제. 위의 "종이 질감 1차 구현"·"새 자원 적용" 기록은 역사로만 남기며 **현재 코드 상태가 아님.** `drawable-nodpi/postcard_paper_fiber_tile.png`(사용자 제작)는 현재 미사용·untracked로 보존 — commit 포함 여부 사용자 결정 필요.
- **현재 구현(미커밋):** `DetailScreen` 루트 Box의 `.background(ScreenBackgroundGray)`를 `editorPaperBackground(gallery_paper_tile)`로 교체 — 갤러리 `GalleryPaperBackground`와 같은 문법(같은 밑색 `PaperCanvas` → `ImageShader` Repeated, 360.dp 같은 배율). 사용자 선택: 꾸미기 화면만, 갤러리와 같은 종이. 하단 도구 dock(`ScreenBackgroundGray` 불투명)은 갤러리 시계 header처럼 단색 유지. 엽서 캔버스·exporter·뒷면·좌표·Room 미변경. testDebugUnitTest 907/907(95 suite), assembleDebug 성공, `git diff --check` 깨끗. `GalleryPaperBackgroundStructureTest`는 GalleryScreen.kt 안 참조만 세므로 영향 없음. 테스트 수·보호 범위 변화 없음 → TEST-COVERAGE-MAP 변경 없음. 실기기 설치·맨눈 QA 대기.
- (이하 이전 엽서 배경 기준 QA 메모 — 현재 무효) 실기기 QA 대기: 밝은 파스텔·중간색·`0xFF2E2638`·커스텀 색에서 누렇게/탁해짐 여부, 결 강도, 사진·텍스트·도장·스티커·패턴과의 조화, 내보낸 이미지와 화면 일치. 진한 배경에서는 결이 거의 안 보일 수 있어(시뮬레이션 기준). QA 전 강도 상향·새 asset·뒷면 확대·commit 금지.

## 다음 출발점

기억밀도 Git 마감과 CI 성공을 확인한 뒤 편집화면·export 구조와 기존 종이 자산을 조사해. 저장 의미나 좌표·schema 변경이 필요하면 구현을 멈추고 보고해. 종이 적용은 별도 변경·별도 commit으로 유지해.

---

# 이전 기록 — 93일차
# HANDOFF — 93일차: 기억밀도 중성펜 막대(비례형) — 목업 비교 → production 적용, 실기기 QA 대기

확인일: 2026-10-04. 수동 표준 모드(93일차 작업지시서, 담당 Claude Code). 목업으로 재료와 높이 규칙을 정한 뒤, 사용자가 같은 날 production 적용까지 승인했어. **실기기 감각 QA는 사용자 결정으로 94일차로 미뤘어. 그래서 commit·push는 하지 않았어**(AGENTS 9절: 필요한 QA가 남으면 commit 금지). Room, 저장 구조, 월 집계는 변경 없음. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 시작 상태

- 브랜치 `feature/photo-sticker`, HEAD `efd5d13`, `git fetch` 후 origin 대비 `0/0`, 추적 작업트리 clean, `git diff --check` 통과. 92일차 종료 참고값과 일치해.
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존(수정·stage 없음).

## 확정된 제품 판단 (사용자 선택, 2026-10-04)

- **방향:** 정보 구조는 반듯하게, 표면은 사람 손으로. 기억밀도에서 얼굴(•_•)·하트·별 같은 감정 장식을 빼고, 막대 아래에는 월 숫자만 남겨. 온기는 필압·선 겹침·작은 삐져나옴 같은 손의 흔적으로 만들어.
- **재료: 중성펜.** 연필·색연필·크레파스·볼펜·중성펜 5종을 같은 데이터·배치·종이 결로 비교한 뒤 사용자가 골랐어. 특징은 자 대고 그은 외곽선, 사선 반복선 메움, 일정한 잉크, 뭉침 없음, 미세한 손떨림.
- **높이 규칙: 비례형, 20장 = 꽉 찬 높이.** 5·7·10단계(1칸 = 4·3·2장)와 비교한 뒤 골랐어. 1장마다 조금씩 자라.
- **20장 초과:** 높이는 꽉 찬 그대로 두고, 막대 위에 같은 중성펜으로 그은 작은 "+"를 붙여.
- **진행:** 오늘 production 적용, 실기기 QA는 94일차.

## 목업 파일

- `docs/ai/mockups/memory-density-mockup.html`: 76일차 새싹형 목업을 1차 질감 비교판(데스크톱, 2배 확대 토글)으로 교체. 옛 새싹형은 git 기록(`06af4fe`)에 있어.
- `docs/ai/mockups/memory-density-mobile-mockup.html`(신규): 5개 재료를 휴대폰 세로 한 화면에서 비교.
- `docs/ai/mockups/memory-density-steps-mockup.html`(신규): 중성펜으로 높이 규칙 4종 비교, 10월 엽서 수 슬라이더.
- 세 파일 모두 외부 의존성 없는 canvas 렌더링이고, seed가 고정돼 있어서 매번 같은 그림이 나와. production 코드나 테스트와 공유하는 코드는 없어(검색 확인). 두 세로형은 claude.ai 비공개 artifact로도 게시했어(사용자 계정).

## production 변경

- `ui/gallery/GalleryScreen.kt` 기억밀도 부분만 바꿨어.
  - **지운 것:**
    - 계산 함수와 상수: `memoryDensityBarLevel`(1칸 = 2장, 6칸), `memoryDensityHeartAlpha`, 줄기 상수
    - 화면: 줄기·하트(`GalleryMemoryDensityStem`), 하단 `•_•` 얼굴
  - **새로 넣은 것:**
    - `memoryDensityHeightFraction(count)`: `count/20`을 0~1로 자른 비율
    - `memoryDensityHasOverflow`: 기준을 12장 초과에서 20장 초과로 변경
    - `memoryDensityGelPenBarStrokes`·`memoryDensityGelPenPlusStrokes`: 순수 함수, dp 좌표, seed 고정
    - `GalleryMemoryDensityBar`: `drawWithCache`로 획을 Path로 만들어 `InkPrimary` × 0.9 농도로 그려
    - `GalleryMemoryDensityFoot`: 월 숫자만 남김
  - **크기:**
    - 막대 영역 120dp(1장 = 6dp) + 위쪽 "+" 자리 14dp
    - 막대 폭은 월 칸의 55%
    - seed = 50500 + 월
  - **유지한 것:** 접근성 설명(`N월, 기억 M개`), 전체 폭 바닥선 1개, `memoryDensityMonthsForYear`
- 목업의 종이 결 반응은 중성펜에서 거의 0(strength 0.06)이라, production에서는 픽셀 합성 없이 획만 그려. 바탕은 갤러리의 기존 종이 배경이야.
- 테스트:
  - `GalleryMemoryDensityTest` 13→14건: 계단·하트 4건 삭제, 비례 높이·20장 초과·펜 획 결정성·실루엣 1.5dp 이내·빈 막대 5건 추가
  - `GalleryMemoryDensityStructureTest` 7건 유지: 얼굴 고정 검사 → "월 숫자만", 줄기·하트 검사 → "비례형 중성펜·장식 없음·접근성 유지"
- `TEST-COVERAGE-MAP.md`: JVM 906→907, 갤러리 보호 범위, 93일차 확인 메모를 갱신했어.

## 남은 위험과 후속 후보(승인된 작업 아님)

- **94일차 실기기 QA(필수, 재개 조건):** 사용자가 Android Studio로 debug 빌드를 실행해서 확인해. 기기 데이터를 지우는 설치 방식은 금지야. 확인할 것:
  - 갤러리 기억밀도 페이지에서 막대가 중성펜으로 그은 듯 보이는지(선 굵기·농도·손떨림)
  - 1~2장인 달의 낮은 막대가 읽히는지
  - 20장 이상인 달은 꽉 찬 높이인지, 21장 이상이면 "+"가 붙는지
  - 얼굴·하트가 없고 월 숫자만 남았는지
  - 페이지 전환과 스크롤이 버벅이지 않는지
- QA에서 조정이 나오면 같은 문법 안에서 값만 바꿔: 막대 영역 120dp, 막대 폭 55%, 선 굵기 0.75/0.95dp, 농도 0.9. QA 통과 뒤에 commit·push하고 CI를 확인해.
- 목업의 웹 canvas 질감과 Compose 실기기 화면의 인상이 같은지는 아직 미검증이야.
- 92일차 후보(SaveTest 격리 조사, 원격 CI instrumentation, Codex 캐시 동기화, 메모리 실측, `GalleryViewMode` dead code 판단)는 그대로 유지돼.

---

# 이전 기록 — 92일차: 검증 환경 안전체계 전환 + 주석·README 최신화

확인일: 2026-10-03. 수동 표준 모드(92일차 수정지시서 3건, 담당 Claude Code). **92일차는 원래 계획이던 "메모리 실측 DAY"가 아니야.** 메모리 실측은 하지 않고 보류했어. 앱 기능·production 로직·test 로직 변경 없이 ① 로컬 emulator 폐기와 실기기 instrumentation 안전 등급 도입(`f8b79e5`), ② AGENTS·workflow 정책 보강(`4c24a3a`), ③ 오래된 주석과 README를 현재 사실에 맞게 정리(이 HANDOFF를 포함한 커밋)했어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 시작 상태

- 브랜치 `feature/photo-sticker`, 92일차 시작 HEAD `4a60314eb596cdd918a65585f207a8c66eda435c`("Make a forgotten decoration save result or directory fail loudly"), `git fetch` 후 origin 대비 `0/0`.
- 기존 미커밋 변경 `app/src/main/java/com/postcardmemory/utils/PostcardDeletionManager.kt` 1건: 정리 단계 주석 번호 `5.`→`4.`, `6~8.`→`5~7.` 2줄. ①②에서는 범위 밖이라 보존했고, ③ 주석 클린업에서 실제 단계(1~4, 5~7)와 맞는 주석-only 변경임을 확인한 뒤 포함했어.
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/`와 ignore된 `.claude/settings.local.json` 보존.

## 현재 정책 (2026-10-03 사용자 결정)

- **로컬 Android Emulator 운영 폐기.** 사용자가 emulator SDK, hypervisor driver, `PostcardMemory_Test` AVD를 제거했고 Android SDK Platform-Tools(`adb`)는 유지했어. 저장소와 GitHub Actions 어디에도 emulator를 자동 생성·실행하는 스크립트는 없어(조사 확인). AVD 생성·부팅, `emulator -avd`, `avdmanager`, emulator 구성요소 재설치, emulator 기반 설치·instrumentation·메모리 측정·QA는 하지 않아. 사용자가 해당 작업에서 별도로 명시 승인할 때만 예외야.
- **로컬 검증 기본 환경 = 사용자가 연결한 실사용 기기.** 실제 엽서가 있는 보호 대상이라 읽기·관찰·비파괴 검증(`adb devices -l`, `dumpsys`, `meminfo`, log, 수동 QA)을 우선해.
- **instrumentation 20건 안전 등급**(현재 코드 기준 읽기 조사, 상세는 `TEST-COVERAGE-MAP.md`):
  - SAFE 7: `PhotoStickerEdgeStyleInstrumentedTest` 7
  - CONDITIONAL 6: `PostcardBackMigrationTest` 1, `PostcardFullMigrationChainTest` 2, `PostcardBackRenderingTest` 3
  - FORBIDDEN 7: `PostcardDeletionOrchestrationTest` 3, `PostcardBackSaveTest` 2, `PostcardBackgroundColorSaveRaceTest` 2
- SAFE 7건만 매번 serial 확인 → 클래스·데이터 영향 없음 명시 → 사용자 승인을 거쳐 `adb -s <serial> shell am instrument -w -r -e class ... com.postcardmemory.test/androidx.test.runner.AndroidJUnitRunner`로 실행할 수 있어. 나머지 13건은 실사용 기기에서 실행하지 않고 `실행 불가`로 기록해.
- Gradle `connected*` task(실행 후 uninstall 위험), `adb uninstall`, `pm clear`, 앱 데이터 초기화, 앱 삭제 후 재설치 우회는 금지야. `adb install -r`도 승인 후에만 하고 서명 충돌로 실패하면 STOP해.
- 원격 CI emulator는 현재 없고 후속 후보야.
- **이전 기록 읽는 법:** 아래 91일차 이전 섹션의 "검증 전용 emulator에서 실행" 같은 후속 지시와 emulator 실행 결과는 당시 기준 기록이야. 현재 실행 지시가 아니고, 위 정책이 우선해.

## 변경 내용

- 저장소 `AGENTS.md` 5절: 실기기 절대 금지 목록에 `connected*` 전체·갤러리·`filesDir` 삭제·재설치 우회를 명시, `adb install -r` 승인·서명 충돌 STOP 추가, emulator 우선·자동 실행·화면 깨우기·삭제 검증 emulator 우선 규칙을 제거하고 새 하위 절 "로컬 검증 환경과 instrumentation 등급"(emulator 폐기, 실기기 기본, SAFE/CONDITIONAL/FORBIDDEN, SAFE 실행 절차, 원격 CI 후보)을 추가. 8절: 검증 영역 `emulator instrumentation` → `실사용 기기 instrumentation(승인된 SAFE 테스트에 한함)`, 실패 분류 `emulator / OS environment` → `device / OS environment`.
- 저장소 `docs/ai/TEST-COVERAGE-MAP.md`: 안전 등급 절과 표 추가, migration·Compose 항목과 마지막 요약의 "다시 emulator에서 확인/실행 필요" 지시형 문구를 당시 기록 + 현재 정책으로 정리. 80~91일차 실행 기록은 그대로 보존. 테스트 수 변화 없음.
- 저장소 밖 workflow canonical source `~/plugins/post-card-memory-workflow`(Git 저장소 아님, 이번 commit에 포함되지 않음): `SKILL.md` #29~32, `references/work-order-template.md`, `references/claude-code-execution-rules.md`, `references/codex-execution-rules.md`, `references/handoff-template.md`, `write-project-handoff/SKILL.md`, `write-project-handoff/references/handoff-template.md`를 같은 정책으로 수정. 템플릿 검증 표의 `emulator instrumentation`/`AVD·API` 칸을 실사용 기기 SAFE 칸으로 바꿨어.
- Claude 메모리(저장소 밖): 91일차 메모리의 emulator 부팅~종료 절차를 지우고 폐기 사실만 남겼어. 새 정책 메모리를 추가하고 색인을 갱신했어.
- ② 정책 보강(`4c24a3a`): `AGENTS.md` 5절에 두 가지를 넣었어. 검증 미실행·실행 불가의 대안으로 emulator 생성·부팅·복구·재설치를 제안하지 않고, 과거 emulator 기록을 현재 실행 근거로 쓰지 않아. SAFE 판정은 helper·setup/teardown·runner·호출되는 앱 동작·실패/취소 후 cleanup까지 보고, 영향이 미확인이면 STOP해. workflow에서는 같은 의미가 빠져 있던 `SKILL.md` #30, `codex-execution-rules.md`, `work-order-template.md`만 최소로 보완했어.

## ③ 주석 클린업 + README 최신화

`app/src` 전체를 TODO/FIXME/임시/emulator/개수·구조 표현으로 검색하고, 걸린 항목을 실제 코드와 대조해 틀렸다고 확정된 것만 고쳤어. 로직·이름·포맷 변경은 없어.

- **현재 사실과 달라 수정:**
  - `ui/gallery/GalleryScreen.kt`의 pager 주석은 76일차에 삭제된 "3단 보기" 페이지가 아직 있는 것처럼 설명하고 있었어. 지금은 월별(MONTHLY)·기억 밀도(DENSITY) 두 페이지가 고정 순서로 있고, 연못 모드는 월별 grid 안에서 그려지고, 월별을 벗어나면 검색이 비워진다는 사실로 고쳤어. 바로 아래 보정 effect의 "(설정에서 보기를 켜거나 끔)"은 토글 UI가 없어진 지금 기준으로 고쳤어.
  - `data/PostcardDao.kt` `getAllPostcards` 주석: "3열 그리드/세부 기록 보기" → "월별 3열 그리드/기억 밀도 보기"(호출처 `GalleryViewModel` 확인).
- **단순 단계 번호 수정:** `utils/PostcardDeletionManager.kt` 정리 단계 `5.`→`4.`, `6~8.`→`5~7.`(기존 미커밋분 포함).
- **emulator 정책 때문에 수정:** `testsupport/StructureTestSource.kt` 주석을 바꿨어. "90일차부터 검증 전용 emulator에서 실행"이던 문장을, 로컬 emulator 폐기(과거 결과는 당시 기록)와 실사용 기기 SAFE만 승인 후 실행, `connected*` 금지, CI는 컴파일만 한다는 내용으로 고쳤어.
- **README:** 문서 상태 날짜를 92일차로 바꿨어. 레이아웃에 누락돼 있던 `편지지`(LETTER, `PostcardLayoutPicker`가 enum 전체를 노출함)를 추가했어. 퀵셀렉트는 옛 "특별한 갤러리" 묶음 표현 대신 실제 손 5장 이름(카메라·미래 우체통·엽서의 연못·양떼목장·엽서 쫑쫑컵)으로 바꿨어. 검증 절은 계측 20건 등급, 실기기 기본, 로컬 emulator 폐기, `connected*`·uninstall·`pm clear` 금지, 원격 CI 후보를 담도록 다시 썼어. 요구 사항의 "기기 또는 에뮬레이터"도 고쳤어. 기능·버전(`libs.versions.toml`)·schema 19·wrapper 없음·권한은 코드와 일치해서 유지했어.
- **애매해서 보존:** `GalleryScreen.kt`의 "특별한 갤러리 3종" 퀵셀렉트 이력 주석(88일차 변경을 함께 적은 이력), `StampCard.kt` 546행의 "기존 3단 그리드와 동일하게"(과거 비교 맥락), "76일차: 3단 보기가 삭제되며…" 계열 이력 주석. `GalleryViewMode`(`COMPACT_GRID`/`DETAIL_LIST`)는 선언 외 참조가 없어 보여도 저장 상태 호환을 확인하지 않아 손대지 않았어.

## 독립 상태

| 구분 | 상태 | 근거 |
|---|---|---|
| 구현(문서·규칙·주석·README) | 완료 | 위 ①②③ |
| production / test 로직 | 변경 없음 | diff는 주석 줄만 |
| 로컬 자동검증(JVM) | 대상 실행: 수정한 소스를 읽는 구조 테스트 포함 7개 suite 49/49 통과 | 구조 테스트가 소스 텍스트를 읽어서, 주석 변경이 문자열 assertion을 깨지 않는지 확인. 전체 JVM은 불필요 |
| 실사용 기기 instrumentation | 미실행 | 범위 밖 |
| 로컬 emulator | 미사용 | 폐기 |
| 메모리 실측 | 미실행(보류) | 이번 날은 안전체계 전환으로 성격이 바뀜 |
| 실기기 감각 QA | 불필요 | 앱 동작 변화 없음 |
| 정합성 감사 | 수정 후 검색 재감사로 확인 | 남은 emulator 언급은 금지 문장·과거 기록·원격 CI 후보 |
| TEST-COVERAGE-MAP | ①에서 갱신, ③은 변경 없음 | ③은 테스트 수·보호 범위 변화 없음 |
| 실사용 기기 | 미접촉 | ADB·설치·실행 없음 |
| repository HANDOFF | 최신화 | 이 섹션 |
| commit / push / CI | ①`f8b79e5` CI 성공, ②`4c24a3a` CI 성공, ③은 이 HANDOFF를 포함한 커밋으로 진행 | ③ 결과는 최종 완료보고에서 확인 |

## 남은 위험과 후속 후보(승인된 작업 아님)

- `PostcardBackSaveTest`·`PostcardBackgroundColorSaveRaceTest`가 실제 기기에서 같은 id의 실제 엽서 초안·확정 상태 파일을 건드리는지 읽기 전용 조사로 확정하고, 필요하면 테스트 데이터 격리(별도 승인 필요, test 코드 변경).
- 원격 CI에서 격리된 instrumentation 환경 도입 검토(CONDITIONAL·FORBIDDEN 13건의 Android 실제 실행 경로).
- Codex 플러그인 캐시 사본 `~/.codex/plugins/cache/personal/post-card-memory-workflow/...`는 canonical source가 아니라서 수정하지 않았어. Codex가 이 캐시를 쓰면 옛 emulator 규칙이 보일 수 있으니 플러그인 재설치·동기화 여부를 확인해야 해.
- 메모리 실측은 실제 증상(버벅임·메모리 부족 종료 등)이 확인될 때 다시 검토하는 후보야. 하게 되면 실사용 기기의 읽기 전용 `meminfo`/profiler 기준으로 설계해.
- `GalleryViewMode` enum은 선언 외 참조가 없어 보여. 저장된 화면 상태 복원 호환까지 확인한 뒤 dead code 여부를 판단할 후보야(91일차 후보 유지).

---

# 이전 기록 — 91일차: 꾸미기 저장 누락 방지 보강 + 주석·README 최신화

확인일: 2026-10-02. 수동 표준 모드(91일차 작업지시서, 담당 Claude Code). 새 기능·UI·미감 변경 없이, 꾸미기 저장 결과나 디렉터리 하나를 빠뜨려도 조용히 지나가지 않도록 작은 안전장치를 넣고 오래된 주석과 README를 현재 사실에 맞췄어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 91일차 시작 상태

- 브랜치 `feature/photo-sticker`, 시작 HEAD `3b01f18adde0e76d6c0421c91a6cec06396e2955`("Record the 90-day structure and tech-debt audit"), `git fetch` 후 origin 대비 `0/0`, 추적 작업트리 clean, `git diff --check` 통과.
- Codex 실패 시도 뒤 저장소 상태: 추적 파일 변경 없음. untracked는 기존 보호 대상 `.codex-config.candidate.toml`, `.kotlin/`(errors log 2개)뿐이었고, `.claude/settings.local.json`은 전역 ignore로 존재. Codex가 남긴 예상 밖 변경은 발견되지 않았어.
- 90일차 HANDOFF 게이트: 90일차 구조·기술부채 감사 기록은 이미 `3b01f18`로 커밋·푸시돼 있어 별도 docs-only commit을 하지 않았어.

## 변경 내용

- **저장 성공 확인 누락 방지:** `shouldConfirmSaveSucceed`(`ui/detail/DetailViewModel.kt`)의 `doodlesSaved`·`textStickersSaved`·`maskingTapesSaved`·`labelStickersSaved` 기본값 `= true` 4개를 제거했어. production 호출부는 여섯 결과를 이름 있는 인자로 넘기고, `ConfirmSaveLogicTest` 11개 호출도 여섯 값을 모두 명시해. 판정은 여전히 여섯 개 AND라 저장 성공 조건·실패 처리·초안 삭제/보존 조건은 그대로야. 이제 결과 하나를 빠뜨리면 컴파일이 실패해.
- **꾸미기 디렉터리 기준 단일화:** 새 `utils/PostcardOwnedFileLayout.kt`에 `DecorationStateFile`(상태 파일 6종: `sticker_states`, `seal_states`, `doodle_states`, `text_sticker_states`, `masking_tape_states`, `label_sticker_states`)과 `PostcardAssetDirectory`(엽서별 자산 3종: `sticker_bgs`, `sticker_originals`, `masking_tape_photos`)를 두었어.
  - 저장·복원: `DetailViewModel`의 상태 파일 쓰기/읽기 12곳과 `sticker_bgs` 3곳이 enum 경로를 써.
  - 삭제: `cleanupPostcardOwnedAssets`가 두 enum을 순회해(삭제 순서·결과 이름 `stickerState`·`confirmedStickerBackgrounds` 등 불변).
  - 고아 진단: `OrphanFileDiagnostics.scan`이 같은 enum을 순회하고, 초안 경로는 `PostcardDraftStorage`의 상수(`private` → `internal`, 값 불변)를 써. category 순서·type·reason 문자열 불변.
  - `PhotoStickerImageStorage`·`MaskingTapePhotoStorage`의 private 디렉터리 상수도 enum 값을 참조해.
  - **기존 경로 문자열은 한 글자도 바꾸지 않았어.** 경로 계산 결과(`File(filesDir, "<dir>/<id>.txt")`, `File(filesDir, "<dir>/<id>")`)도 기존과 같아. 파일 이동·rename·데이터 migration 없음.
- **새 보호 테스트:** `DecorationDirectoryContractTest` 3건 — 디렉터리 이름을 production 상수가 아닌 리터럴로 독립 고정, 저장 경로 함수로 만든 파일을 production 삭제가 전부 지우고 다른 엽서는 보존하는지, production 진단이 전부 찾고 엽서가 있으면 0건인지 실제 파일 I/O로 확인. 삭제 목록에서 마지막 종류를 빼는 일시 변형을 넣었을 때 이 테스트가 실패하는 것을 확인한 뒤 원복했어.
- **주석 건강검진:** `app/src` 전체에서 TODO/FIXME/HACK·emulator·schema/version 숫자·개수 표현을 검색해 현재 코드와 대조했어. TODO/FIXME는 0건. 고친 오래된 주석:
  - `testsupport/StructureTestSource.kt`: "emulator는 아직 준비돼 있지 않다" → 90일차부터 검증 전용 emulator 수동 실행이 가능하지만 CI는 계측 테스트를 컴파일만 한다는 현재 사실.
  - `ui/detail/SaveResultAlertDialog.kt`: "다이얼로그 7종" → 현재 DetailScreen 호출부 4곳(56·58일차에 줄어든 사실 반영).
  - `DetailViewModel.kt`: `ConfirmSaveState`·`saveEditsAndClearDraft` 설명의 "스티커·도장" 2종 표현 → 꾸미기 여섯 종. `shouldConfirmSaveSucceed`에는 기본값을 두지 않는 이유를 적었어.
  - `PostcardDeletionManager.kt`·`OrphanFileDiagnostics.kt`: "새 디렉터리 추가 시 여기도 넣어야" 경고 → 공용 목록을 순회하고 계약 테스트가 보호한다는 설명. 저장 실패·초안 보존·`ExitSaveScope`·`onCleared`·삭제 순서·파일 소유권 주석은 건드리지 않았어.
  - 갤러리 퀵셀렉트의 "특별한 갤러리 3종" 주석은 88일차 변경을 함께 적은 이력 주석이라 유지했어.
- **README:** "과거 설명 보존" 상태였던 앱 소개를 현재 코드 기준으로 다시 썼어 — 2열 그리드·5페이지 편집·Neo-Brutalism 색표 같은 옛 설명을 걷어내고, 앞면/뒷면 편지·작성 기록, 편집 탭 7개와 꾸미기 종류(사진 스티커 오림 5종 포함), 초안·확정 저장·공유·내보내기, 월별(3열)/기억 밀도 갤러리, 흔들어서 한 장, 퀵셀렉트, 방문 기록·달력, 미래 우체통, 기술 스택(Room schema 19, migration 1→19), 검증 표(CI는 `assembleDebugAndroidTest`로 계측 APK를 컴파일만 하고 실행하지 않음을 구분), 권한(CAMERA, VIBRATE)을 정리했어.

## 최종 검증과 독립 상태

| 구분 | 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | 위 변경 내용 |
| 관련 JVM 우선 실행 | 86/86 통과 | `ConfirmSaveLogicTest` 15, `ConfirmSaveHistoryClearStructureTest` 3, `DecorationDirectoryContractTest` 3, `PostcardDeletionManagerTest` 12, `OrphanFileDiagnosticsTest` 9, `PostcardDraftStorageTest` 29, `ConfirmedEditStateStorageTest` 5, `AppFileOwnershipTest` 10 |
| 로컬 JVM 전체 | **906/906 통과** | XML 95개, 실패·오류·skip 0. `@Test` 906개, 테스트 파일 94 + helper 1 |
| assembleDebug | 성공 | 앱 APK 빌드 |
| assembleDebugAndroidTest | 성공 | 계측 테스트 APK **컴파일**일 뿐 실행 아님 |
| emulator instrumentation | `PostcardDeletionOrchestrationTest` 3/3, 전체 **20/20** 통과 | 검증 전용 `PostcardMemory_Test`(`emulator-5554`, sdk_gphone16k_x86_64)만 연결 확인 후 APK 설치·실행. boot 직후 첫 시도 2회는 emulator lowmemorykiller가 test process를 죽여 status 없이 종료 → 환경 문제로 분류, test emulator만 재부팅 후 같은 APK로 통과. 실행 후 emulator 종료 |
| 실기기 QA | 불필요 | UI·미감·사용자 동작 변화 없음, 저장 판정 논리·경로 문자열 불변, 경로 일치는 자동 테스트와 계측으로 확인 |
| TEST-COVERAGE-MAP | 갱신 완료 | 테스트 3건 추가, 디렉터리 계약 보호 범위·한계 기록 |
| Room schema / migration / serialization / 꾸미기 파일 형식 / 디렉터리 이름 / 사용자 데이터 | 변경 없음 | diff 확인 |
| 실사용 기기 | 미접촉 | 연결·ADB·설치·계측 없음 |
| repository HANDOFF | 최신화 | 이 섹션 |
| commit / push / CI | 이 HANDOFF를 포함한 91일차 커밋으로 진행 | 결과는 최종 완료보고에서 확인 |

## 남은 위험과 후속 후보(승인된 작업 아님)

- 디렉터리 계약 테스트는 DetailViewModel이 실제로 enum 경로로 저장한다는 사실까지는 증명하지 못해(Robolectric 없음). 새 꾸미기 종류가 enum을 거치지 않고 다른 경로에 저장되면 잡지 못해 — 7번째 종류 기획 시 공통 틀과 함께 검토.
- `DetailViewModel`의 초기 로드·"원래대로"·확정 저장 후 이력 초기화의 여섯 종 평행 목록은 여전히 기억 의존이야(구조 테스트 `ConfirmSaveHistoryClearStructureTest`만 존재).
- `ConfirmSaveLogicTest`의 "…AllSaved" 4건은 기본값 제거 후 모두 여섯 값 true로 같은 입력이 됐어. 이름은 여전히 사실이지만 중복 정리는 후속 후보.
- `GalleryViewMode.DETAIL_LIST`는 enum 선언 외 참조가 없어 보여(README에서 "자세히 보기" 설명을 뺀 근거). 저장된 화면 상태 복원(`valueOf`) 호환까지 확인한 뒤 dead code 여부를 판단할 후보.
- `PostcardDeletionManager.kt` 정리 단계 주석 번호가 3 → 5로 건너뛰는 오래된 표기는 의미 영향이 없어 그대로 뒀어.
- 90일차 후보(백업 정책, release 로그, FileProvider 여분 root, quick select 손 해상도, 실제 heap 측정)는 그대로 남아 있고, 92일차 메모리 실측과 섞지 않았어. 90일차 "코드 클린데이 후보" 1~3번은 이번에 처리했어.

---

# 이전 기록 — 90일차 건강검진: instrumentation 청산 + 첫 보안 감사 + bitmap 메모리 감사

확인일: 2026-10-01. 수동 표준 모드(90일차 장기 작업지시서). 신규 기능과 앱 동작 변경 없이, 미실행 instrumentation을 검증 전용 emulator에서 실제로 실행하고 release/debug 보안 표면과 bitmap 상주 구조를 감사했어. production·test·이미지 자산은 수정하지 않았고 이 HANDOFF와 TEST-COVERAGE-MAP만 실제 결과로 갱신했어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 90일차 시작 상태

- 브랜치 `feature/photo-sticker`, 시작 HEAD `5e1abc861b88fc5a365acb16a3b222b663a3f3bf`, origin 대비 `0/0`, 추적 작업트리 clean.
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/`을 수정·삭제·이동·stage하지 않았어. 기존 `.claude/settings.local.json`도 보존했어.
- JVM 903개/소스 테스트 파일 93개(+helper 1개), 기존 결과 903/903. instrumentation 20개/7파일. 최근 CI run `36713844171` 성공.
- Room schema 19, migration 1→19 연속 등록, custom text serialization과 `filesDir/visits/visit_record.txt` + `visits/history/<epochDay>.visit` 방문 기록 형식, dependency 구조를 확인했어.

## 1단계 — instrumentation 실제 실행

- 검증 전용 `PostcardMemory_Test` AVD만 연결했어: `emulator-5554`, API 37, x86_64, 16KB page system image, 1080×2400, 420dpi. 실사용 기기는 연결하거나 조작하지 않았어.
- `assembleDebug`와 `assembleDebugAndroidTest` 성공 후 main/test APK를 이 emulator에만 설치했어.
- `PhotoStickerEdgeStyleInstrumentedTest` 7건을 명시 실행해 **7/7 통과**, 이어 전체 instrumentation을 **20/20 통과**했어.
- boot 직후 첫 대상 실행은 test process가 test status 전 종료됐어. 같은 시각 emulator에서 Play Store install session 충돌과 UWB HAL 재시작이 반복돼 환경 문제로 분류했고, test emulator만 재부팅해 session을 비운 뒤 같은 APK·테스트를 재실행하자 전부 통과했어. production/test 수정은 없었어.

## 2~3단계 — 첫 보안 감사와 수정 판단

- 앱 소스·설정 126개와 생성된 release/debug manifest를 정적 감사했어. manifest component/exported/permission, 내부 저장소와 cache, FileProvider/URI, 경로 입력, backup, release log, secret, 네트워크, 외부 Intent와 존재하는 선택 기능을 추적했어.
- 결과: 높음 0, 중간 1, 낮음 2, 정보성 3.
- **중간 — 기본 백업 범위 미지정:** `allowBackup=true`이고 `dataExtractionRules`/`fullBackupContent`가 없어 내부 Room·사진·초안·방문 기록이 플랫폼 기본 백업/기기 이전 대상이 될 수 있어. 앱 샌드박스와 계정 보호는 있지만 백업 계정·복원 환경 노출 시 개인 데이터가 함께 노출될 가능성이 있어. 제외하면 복원·기기 이전에서 데이터가 사라질 수 있으므로 제품 정책 결정 전 수정 STOP.
- **낮음 — release 진단 로그:** `PostCardMemoryApp`, `DetailViewModel`, `GalleryViewModel`의 `Log.w` 9곳에 내부 경로·ID·예외 메시지가 일부 남을 수 있어. 일반 앱이 logcat을 읽을 수 있는 구조는 아니고 사용자 본문을 직접 기록하지 않아 즉시 수정하지 않았어.
- **낮음 — FileProvider 여분 root:** provider는 `exported=false`이고 개별 URI grant가 필요하지만, 현재 호출 경로가 쓰지 않는 `files-path/postcards/`가 URI 생성 범위에 포함돼 있어. 현재 외부 grant 호출은 없고 과거 URI 호환을 깨뜨릴 가능성이 있어 보류했어.
- release의 필수 외부 진입은 launcher `MainActivity`뿐이야. provider/service는 비공개이고, 외부 표시된 profile installer receiver는 시스템급 `DUMP` permission으로 보호돼. `root-path`·`external-path`·전체 저장소 권한·deep link·WebView·동적 코드·secret/credential은 없었어.
- CAMERA는 카메라 기능, VIBRATE는 햅틱에 사용해. INTERNET·ACCESS_NETWORK_STATE는 ML Kit 계열 전이 dependency가 병합하지만 앱 소스에는 endpoint/HTTP 호출이 없고 release cleartext HTTP는 플랫폼 기본값으로 차단돼.
- production/test 수정과 보안 테스트 추가는 없음. 근거 없는 수정이나 테스트 수 늘리기를 피했어.

## 4단계 — bitmap 메모리 건강검진

- quick select 손 5장은 각각 1254×1254 `drawable-nodpi`이고 ARGB_8888 예상 decode 합계가 31,450,320B(약 30.0MiB)야. 표시 크기는 180dp, 검증 emulator 420dpi에서는 약 473px인데 메뉴를 닫아도 손 묶음이 composition에 남아 5장 모두 상주할 수 있어.
- `gallery_paper_tile.png`는 1254×1254, 예상 6,290,064B(약 6.0MiB). 360dp 반복 타일로 갤러리 화면 수명 동안 한 장만 유지되고 빈 화면/일반 pager 분기는 서로 배타적이야.
- `visit_calendar_paper.png`는 1122×1402, 예상 6,292,176B(약 6.0MiB). 월 이동 장들이 하나를 공유하지만 drawer가 닫혀도 drawer content composition에 남을 수 있어.
- 세 자산군 동시 상주 예상은 44,032,560B(약 42.0MiB)야. 실제 heap profiler 측정이 아니라 픽셀×4 상한 추정이야.
- 가장 큰 후보는 quick select야. 640px 가정 시 약 22.2MiB(74%), 720px 가정 시 약 20.1MiB(67%)를 줄일 수 있지만 손 디테일·알파 경계 비교 QA가 필요해. 이미지 파일과 두 종이의 해상도·밝기·질감은 수정하지 않았고, 달력 조건부 composition도 첫 열림/넘김 감각을 바꿀 수 있어 보류했어.

## 최종 검증과 구조 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| production/test 구현 | 변경 없음 | 감사 결과상 즉시 수리보다 정책·호환 판단이 먼저 |
| 로컬 JVM | 903/903 통과(94 XML, 실패·오류·skip 0) | 최종 task 성공, 코드 불변으로 test task는 up-to-date |
| assembleDebug / assembleDebugAndroidTest | 성공 | 최종 task 성공, 산출물 up-to-date |
| emulator instrumentation | 대상 7/7, 전체 20/20, 최종 대상 7/7 통과 | 검증 전용 emulator만 사용 |
| 실기기 QA | 불필요 | 앱 코드·UI·이미지·동작 변경 없음 |
| TEST-COVERAGE-MAP | 갱신 완료 | 테스트 수 변화 없음, instrumentation 실제 실행 상태 변경 |
| Room / migration / serialization / visit history / draft / photo format / dependency | 변경 없음 | 읽기 전용 감사 |
| 실사용 기기 | 미조작 | 설치·실행·ADB·instrumentation 없음 |

## 남은 위험과 91일차 후보(승인된 작업 아님)

- 백업 보존 정책을 사용자와 먼저 결정한 뒤 민감 경로 제외/백업 유지/전체 비활성 중 하나를 별도 작업으로 설계.
- release 로그 최소화와 미사용 FileProvider root 제거는 과거 호환·진단 필요성을 먼저 확인한 뒤 방어 심화 작업으로 검토.
- quick select 손 640px/720px 후보를 별도 파일로 비교해 실기기 화질 승인을 받은 뒤에만 원본 교체 검토.
- Android Studio profiler로 갤러리 진입·drawer 열기/닫기 전후 실제 Java/native/graphics heap을 측정해 42.0MiB 추정을 검증.
- CI는 instrumentation을 실행하지 않으므로 원격 CI에서 격리된 instrumentation 환경 도입 검토(2026-10-03 갱신: 로컬 emulator는 폐기됐으니 로컬 emulator 재구성이 아니라 원격 CI 후보로 읽어).

---

# 이전 기록 — 90일차 추가 감사 — 구조·기술부채 / 장기 유지보수

확인일: 2026-10-01. 수동 표준 모드. 위 90일차 건강검진을 기록한 HEAD `7fe29bf874461c46a3313d3667c0c1132abcfc06` "Record the 90-day health and security audit" 상태를 대상으로 한 **읽기 전용 후속 감사**야. 보안·bitmap 메모리는 위 건강검진에서 이미 봤으므로 반복하지 않고 장기 유지보수 관점만 봤어. production·test·Gradle/dependency·이미지 자산·Room/migration/serialization은 수정하지 않았고, 이 HANDOFF 섹션만 추가했어. 아래 수치는 감사 시점 `wc -l`·`grep` 실측이며, 상태 개수 등은 근사값이야. **이 섹션의 모든 후보는 승인된 작업이 아니라 관찰·후속 후보야.**

## 전체 판단

- 현재 구조 상태: **관리 필요**. 다만 **구조적 한계 접근 중은 아니야.**
- 강한 영역: 저장 안전성, Room schema/migration(schema 19 JSON 보존, 1→19 연결 계측 테스트), 원자 저장(`AtomicFileReplace`), 삭제 흐름(DB 삭제 성공 → 앱 소유 파일 정리, `isInsideDirectory` guard), 초안 파일 기반 프로세스 종료 복구.
- 현재 구조 비용은 주로 **편집 화면(`DetailScreen`/`DetailViewModel`)과 꾸미기 종류 확장 경로**에 집중돼 있어. 데이터 손상 위험보다 변경 시 수고 형태의 비용이야.
- 지금은 대규모 리팩터링보다 관찰과 작은 예방 조치가 적절해.

## 주요 hotspot

### DetailScreen

- `ui/detail/DetailScreen.kt`: 약 7,173줄(2026-08-08 기준 5,666줄 → 제3~9차 UI 조각 분리 이후에도 약 1,500줄 증가).
- `DetailScreen()` 단일 composable(`:1407` ~ 파일 끝): 약 5,770줄.
- 함수 안 `remember` 상태 약 78개, Effect(`LaunchedEffect`/`DisposableEffect`/`SideEffect`) 약 21개, `viewModel.` 참조 약 234개, gesture/`pointerInput` 계열 약 44곳.
- 2026-09-01 이후 이 파일을 건드린 커밋 13개로 변경 빈도가 가장 높은 파일이야. 새 상호작용은 별도 파일(`*PlaceInteraction.kt`, `SealStampInteraction.kt`)로 빼고 있지만 연결 코드는 계속 이 함수에 쌓여.
- 지금 대규모 분리는 회귀 위험(제스처·저장 연결, 소스 텍스트 구조 테스트 동반 수정)이 더 크므로 즉시 리팩터링하지 않아.
- 새 편집 탭이나 새 꾸미기 기능을 추가할 때 그 탭 단위로 작은 분리를 검토해.

### DetailViewModel

- `ui/detail/DetailViewModel.kt`: 약 4,718줄.
- 꾸미기 6종(사진 스티커·도장·낙서·텍스트·마스킹테이프·라벨)마다 목록·선택 id·canUndo/canRedo·확정 baseline·`persist*EditState`·`clear*History`·`filesDir/<종류>_states/<id>.txt` 경로가 평행 구조로 존재해. 여기에 Room 필드 즉시 저장 Job 19개, 초안 자동저장, 배경 제거, export/share가 같은 클래스에 있어.
- 7번째 꾸미기 종류 추가 시 이 파일만 최소 8~10군데를 고쳐야 해 유지보수 비용이 크게 증가할 가능성이 있어. 확정 저장 코드 주석도 "여섯 개여야 한다 — 하나라도 빠지면 …"이라고 기억 의존 계약을 경고해.
- 현재는 공통화하지 않아. 7번째 종류가 실제로 기획될 때 공통 틀을 검토해.

### 화면 / exporter 이중 렌더러

- 마스킹테이프 무늬와 도장 모양 일부가 Compose 화면(`ui/components/MaskingTapeShapes.kt`, `SealShapes.kt`, DrawScope)과 Canvas exporter(`utils/PostcardImageExporter.kt`의 `drawTape*Overlay`, `drawCirclePostmarkOverlay` 등)에 별도 구현돼 있어. 상수만 공유하고, 예를 들어 하트 path cubic 수식은 두 파일에 복사돼 있어. 사진·라벨·도장 잉크처럼 `PostcardRenderSpec`/`LabelStickerRenderer`/`SealInkWearRenderer`로 이미 공통화된 부분도 있어.
- 한쪽만 고치면 화면과 저장·공유 결과가 달라질 잠재 위험이 있고(`AGENTS.md` 13절 불변값), 좌표 테스트는 있지만 그림 일치 테스트는 없어.
- 지금 전체 통합하지 않아. 해당 모양을 실제 수정하는 시점에 그 종류만 공통화를 검토해.

## 숨은 결합

1. **꾸미기 상태 파일 디렉터리 목록 3중 정의:** 같은 `<종류>_states`·`sticker_bgs`·`sticker_originals`·`masking_tape_photos` 등 디렉터리 목록이 `DetailViewModel`(쓰기), `PostcardDeletionManager`(삭제), `OrphanFileDiagnostics`(진단) 세 곳에 별도 문자열 목록으로 존재해. 세 곳 모두 "새 디렉터리 추가 시 여기도 넣어야" 주석이 있고 목록별 테스트는 있지만, 세 목록이 서로 같은지 확인하는 테스트는 없어. 삭제 쪽 누락은 컴파일·테스트를 통과한 채 삭제할 때마다 고아 파일을 남길 수 있어.
2. **`shouldConfirmSaveSucceed`(`DetailViewModel.kt:101`):** 6종 중 4종 인자의 기본값이 `= true`야. 새 꾸미기 종류 추가 시 이 판정에 넘기는 것을 잊어도 컴파일되고, 그 종류의 실제 저장 실패가 전체 성공으로 처리돼 초안이 삭제될 수 있어. 미래 데이터 손실 함정 후보야(현재 6종은 모두 명시적으로 전달돼 현재 결함은 아님).
3. **초안 형식(`PostcardEditDraft.kt`):** 위치 기반 meta index(`meta[14]` 등)와 "앞 종류 개수 합" offset 구조야. v1~v5 하위 호환 테스트는 강하지만 종류 추가마다 offset 계산이 길어지고, 순서 실수 시 `parsePostcardEditDraft`가 null을 반환해 초안이 통째로 버려질 수 있어.
4. **꾸미기 종류 간 겹침 순서:** 종류 사이의 layer 순서는 데이터에 저장되지 않고 화면 코드와 exporter 코드의 그리기 순서에 각각 암묵적으로 존재해. 한쪽만 바뀌면 미리보기와 저장 이미지의 layer 순서가 달라질 수 있어(코드 구조로 판단, 실제 불일치 재현은 미실행).

참고(기록용): 레이아웃 × 사진 배치 컬럼(`stamp/polaroid/tapedFilm × offset/zoom/scale`)도 새 레이아웃 추가 시 Entity·Migration·DAO·Repository·저장 Job·화면 분기(`DetailScreen` 약 9곳)를 함께 늘리는 곱셈 구조야. 경로 자체는 잘 보호돼 있어 아래 안정 영역으로 분류해.

## 안정적인 영역 — 현재 건드리지 않는 편이 좋음

- Room schema / migration chain
- 평면 `Postcard` Entity와 DAO/Repository 1:1 경로
- 기존 텍스트 serialization 형식(바꾸면 기존 사용자 파일 호환 위험이 이득보다 큼)
- 필드별 저장 Job 구조(장황하지만 경합 테스트가 붙어 있음)
- `ExitSaveScope` / 초안 승격 / `onCleared` 저장 흐름
- 삭제 순서와 앱 소유 파일 guard
- 갤러리 계열 기능 분리 구조(`GalleryScreen.kt` 1,545 → 3,267 → 2,182줄로 실제 분리된 이력)
- 방문 기록 저장 구조

## 테스트 관점

- JVM 903개 / instrumentation 20개라는 숫자보다 무엇을 보호하는지가 중요해.
- migration, 직렬화, 초안, 원자 저장, 삭제, 순수 계산(좌표·오림 모양·달력·상호작용 session)은 production 직접 검증으로 강해.
- 구조 테스트 159개는 실제 동작이 아니라 소스 형태를 보호하는 한계가 있고, `DetailScreen` 분리 시 테스트 동반 수정 비용을 만들어.
- replica 테스트(핵심 DetailViewModel replica 25개)는 실제 production ViewModel 직접 검증이 아닌 영역이 존재해(`TEST-COVERAGE-MAP.md`에 이미 명시).
- 화면과 exporter의 시각 결과 일치 검증은 현재 공백이야.
- `app/src/test/.../testsupport/StructureTestSource.kt`의 "emulator는 아직 준비돼 있지 않다" 취지 주석은 90일차 검증 전용 emulator 20/20 실제 실행 결과 기준으로 이제 오래된 설명이야.

## 다음 코드 클린데이 후보(승인된 작업 아님, 실행하지 않음)

1. 쓰기·삭제·진단의 꾸미기 디렉터리 목록이 서로 동일한지 확인하는 보호 테스트 1건.
2. `shouldConfirmSaveSucceed`의 위험한 `= true` 기본값 제거 검토.
3. `StructureTestSource`의 emulator 관련 오래된 주석 갱신.

## 장기 후보(승인된 작업 아님 — 100일 이후 또는 실제 기능 확장 시점의 관찰·후속 후보)

- `DetailScreen` 탭 단위 점진적 분리
- 꾸미기 공통 상태/저장 틀
- 화면/exporter renderer 공통화(그림 비교 검증 마련 선행)
- 초안 위치 기반 형식의 장기 확장 전략

## 최종 평가

> 90일 동안 기능이 지속적으로 추가된 것에 비해 프로젝트는 구조적으로 잘 버티고 있다. 현재 가장 큰 비용은 데이터 손상 위험보다는 편집 기능을 변경할 때 발생하는 유지보수 비용이다. 지금은 대규모 리팩터링보다 다음 기능 확장 시점에 작은 단위로 분리하는 편이 안전하다.

## 이 감사의 독립 상태

| 구분 | 상태 | 근거 |
|---|---|---|
| production/test/Gradle/자산/Room/serialization | 변경 없음 | 읽기 전용 감사 |
| 로컬 JVM / instrumentation / CI | 미실행 | 코드 불변, 위 건강검진 결과(903/903, 20/20) 유지 |
| 실기기 QA | 불필요 | 앱 동작 변경 없음 |
| TEST-COVERAGE-MAP | 변경 없음 | 테스트 수·범위 변화 없음 |
| repository HANDOFF | 이 섹션 추가 | 문서-only |
| commit / push | 미실행(사용자 승인 대기) | |

---

# 이전 기록 — 90일차(89일차 추가 작업): 방문 달력 종이 한 장 + 위로 넘기는 월 이동

확인일: 2026-09-30. 수동 표준 모드(89일차 추가 작업지시서: 구조 조사 → 자산 규격 → 자산 게이트 → 정적 형태 → 넘김 → QA 보정). 사이드바 방문 달력의 제목·장식·요일·날짜 grid·방문 표시·카오모지를 사용자 제공 종이 한 장 위에 묶고, 이전/다음 달 이동을 벽걸이 달력처럼 윗변을 축으로 위로 넘기는 넘김으로 바꿨어. 로컬 자동검증과 사용자 실기기 QA(정적 형태 → 넘김 → 다음 달 반투명 겹침 보정 → 하단 가로선 제거, 전부 통과)를 마쳤어. **commit·push는 사용자 승인 대기**야. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, 시작·현재 HEAD `926b280` "Lay the main gallery on a cardboard paper sheet"(CI run `36698372064` 성공), origin `0/0`
- 미커밋 변경: `ui/gallery/VisitCalendarDrawer.kt` 수정, `VisitCalendarTest.kt` 수정 / 새 파일 `res/drawable-nodpi/visit_calendar_paper.png`, `VisitCalendarPaperPageStructureTest.kt` / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 자산

- `app/src/main/res/drawable-nodpi/visit_calendar_paper.png` — 사용자가 생성형 이미지로 만든 한 장 종이(타일 아님), 수정 없이 사용. 1122×1402(정확히 4:5) PNG 8bit RGB, 알파·ICC 없음(→ sRGB), 2,431,538 B, `caBX` 청크 포함. decode 약 6.3MB.
- 실측: 평균 #E9D4BA(L\* 85.9, b\* 15.7), 밝기 표준편차 5.06, 잔결 4.65, 가장 어두운 8×8 영역 201, 날짜색 #5B5046 대비 평균 약 5.4:1·최저 약 4.7:1, 5px 이상 섬유 점 81개, 3×4 구역 균일(±1), 비네팅·격자 성분 없음. 산출했던 규격(평균 #F4EDE1, L\* 92~95, 섬유 점 15개 이하)보다 어둡고 누렇지만 **사용자가 의도적으로 선택**했어.
- 경과: 1차(이 파일) 게이트 불통과 판정 → 2차(평균 #F6EFE6, 편차 2.03)는 폰에서 거의 무지로 보임 → 사용자가 "뒷배경이 너무 연하다"며 1차를 다시 채택. 2차는 저장소에 없음.

## 기존 구조(바꾸기 전)

- `VisitCalendarDrawer.kt` 한 파일: `ModalNavigationDrawer` → `ModalDrawerSheet`(304dp, `PaperSurface`) → `Column(verticalScroll, padding 24dp)` → `MonthlyVisitCalendar`. 월별↔선택판은 `AnimatedContent(navLevel)`(CALENDAR/MONTH_PICKER/YEAR_PICKER, fade+scale 170ms).
- 월 이동은 제목과 날짜 grid가 **각자 다른** `AnimatedContent(displayedMonth)`로 가로 슬라이드(200ms)했고, 요일·상단 장식·"다녀간 날들" 줄은 고정. 달력 자체 배경은 없었어(drawer 단색). 월 이동 gesture는 없음(◀ ▶·"오늘"·월 선택기만).

## 앱에 달라진 점과 구조

- `VisitCalendarMonthPage(month, …)`: 종이(`drawWithCache` + `drawImage`, 큰 쪽 배율 하나로 가운데 crop, 늘림·타일 없음) 위에 제목(48dp, 탭 → 월 선택)·상단 장식·요일·`VisitCalendarMonthGrid`. 안에서는 파라미터 `month`만 읽어 나가는 장이 다음 달로 바뀌지 않아. 윤곽선은 QA에서 "카드처럼 보임"으로 제거.
- 종이 bitmap은 `MonthlyVisitCalendar`에서 한 번만 로드해 모든 장이 공유(달이 바뀔 때마다 decode 없음).
- "다녀간 날들 / 오늘" 줄은 종이 위(밖)로 옮겨 고정, ◀ ▶는 종이 제목 줄 위에 겹쳐 고정(가운데 빈 곳 터치는 제목으로 내려감).
- 날짜 grid는 구분선이 없는 주 자리에도 같은 높이(4.5dp)를 비워 어느 달이든 장 높이가 같아(넘길 때 아래 장이 삐져나오지 않음). 4·5주 달의 빈 패딩 행 아래 위치가 최대 9dp 내려감.
- 월 이동: `updateTransition(displayedMonth)` + `Transition.AnimatedContent`, `visitCalendarPageTurnTransition`(Enter None, Exit `KeepUntilTransitionsFinished`, `targetContentZIndex = visitCalendarPageStackZIndex(month)` = 앞선 달이 항상 위, `SizeTransform(clip = false)`). 표현은 장 전체에 거는 `visitCalendarPageTurnModifier(forward)`(윗변 `TransformOrigin(0.5f, 0f)` + `rotationX`, cameraDistance 14, 380ms).
  - 다음 달: 위의 현재 장이 FastOutLinearIn으로 불투명한 채 들리다 빨라져 90°에서 사라지고(흐려짐은 마지막 10%), 들리는 장에만 옅은 그림자(최대 0.12). 아래 다음 장은 움직이지 않음. 처음엔 두 방향이 FastOutSlowIn·흐려짐 시작 0.45를 공유해 현재 장이 약 70% 시간 반투명하게 떠 있어 "두 달이 crossfade"처럼 보였고(QA), 다음 달 값만 분리해 해결.
  - 이전 달(QA에서 자연스럽다고 한 기준점, 보정 없이 유지): 이전 장이 80° 들린 채 FastOutSlowIn으로 내려와 덮고(흐려짐 시작 0.45), 덮이는 아래 장에 그림자.
- 하단 `VisitCalendarBottomOrnament`의 폭 전체 가로선 제거(종이 밑변과 이중 경계로 사이드바 구획선처럼 보임), 양끝 `୨୧`만 유지. 월/연도 선택판 하단도 같이 바뀜(사용자 승인).
- 월/연도 선택 단계 전환(fade+scale)·선택판 세로 슬라이드·방문 데이터 읽기(`visitedDaysForMonth`)·카오모지·오늘 표시·햅틱·drawer 폭/색/닫기·뒤로가기 변경 없음. Room·migration·serialization·dependency·방문 기록 형식 변경 없음.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 903/903 (94 XML) | 신규 `VisitCalendarPaperPageStructureTest` 7건, `VisitCalendarTest` +3건 |
| assembleDebug / assembleDebugAndroidTest | 성공 | 최종 코드로 재실행 |
| emulator instrumentation | 미실행 | androidTest 변경 없음, 기존 미실행 7건 유지 |
| 실기기 QA | 완료 | 정적 형태(윤곽선 제거 후) 통과, 다음 달 넘김 보정 후 "좋아졌어", 이전 달·연타·오늘 복귀·선택 단계 전환·방문 표시 정상, 하단 가로선 제거 "좋아" |
| TEST-COVERAGE-MAP | 갱신 완료 | 893→903, 파일 92→93, 구조 152→159 |
| git diff --check / LF | 깨끗 / LF 유지 | |
| commit / push / CI | 미실행(사용자 승인 대기) | |

## 남은 위험

- 종이가 산출 규격보다 어둡고 섬유 점이 많아(최저 대비 약 4.7:1) 밝기가 낮은 기기·야외에서 날짜 가독성이 떨어질 수 있음 — 사용자 선택이라 유지.
- 상주 메모리: 달력 종이 약 6.3MB 추가(drawer 내용은 닫혀 있어도 composition에 있음). 메인 종이 6.3MB + 퀵 셀렉트 약 31MB와 합산.
- 넘김 중 ◀ ▶ 아주 빠른 연타는 방향이 섞여 보일 수 있음(QA 연타는 정상). 들어오는 달 방문 기록을 아직 못 읽었으면 넘긴 뒤 민트가 늦게 찍힐 수 있음(기존과 같은 구조).
- 가로 화면·작은 화면·다른 밀도 기기의 넘김 원근·종이 crop 미확인.

## 다음 후보(승인된 작업 아님)

- 이 변경의 commit·push·CI 확인(사용자 승인 필요)
- `PhotoStickerEdgeStyleInstrumentedTest` 7건 검증 전용 emulator 실행, 종이·퀵 셀렉트 PNG 메모리 최적화

---

# 이전 기록 — 89일차: 메인 갤러리 카드보드 종이 배경 + 불투명 시계 header

확인일: 2026-09-30. 수동 표준 모드(89일차 작업지시서). 새 기능 없이, 메인 갤러리의 구분선 아래 본문에만 사용자가 만든 종이 질감 타일을 깔고 시계·커피 header를 불투명하게 만들었어. 로컬 자동검증과 사용자 실기기 QA(이미지 교체 1회 후 "대만족", 이음매 선 없음·기존 기능 전부 정상)를 마쳤고, 작업지시서 27절에 따라 commit·push·CI 확인까지 진행해(실제 commit hash·CI는 완료보고·`git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, 시작 HEAD `e4045ab` "Record the quick select long-press removal as a product decision"(CI run `36549115333` 성공), origin `0/0`, 추적 파일 clean
- 커밋 대상: `ui/gallery/GalleryScreen.kt` 수정 / 새 파일 `res/drawable-nodpi/gallery_paper_tile.png`, `GalleryPaperBackgroundStructureTest.kt` / `GalleryMonthlyGridStructureTest.kt` 수정 / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 자산

- `app/src/main/res/drawable-nodpi/gallery_paper_tile.png` — 사용자가 생성형 이미지로 만든 seamless 타일, 수정 없이 사용. 1254×1254 PNG 8bit RGB(알파 없음, ICC 없음 → sRGB), 2,668,364 B, `caBX`(C2PA 출처 메타데이터 추정) 청크 포함.
- 실측(최종본): 평균 #EFE1CD(Hue 35°, 채도 14%), 밝기 표준편차 6.9/255, 156px 블록 평균 차 2.4, 내부 반복 없음, 좌우 이음매 연속. 위아래 이음매는 맨 아래 줄(224.5)과 맨 위 줄(229.9)이 전체 줄 중 가장 어둡고/밝은 1px 밝기 단차가 있었지만 실기기에서 선이 보이지 않음(QA 확인).
- 1차 이미지(표준편차 4.5, 평균 #F1E7D5)는 폰에서 거의 안 보여 사용자가 같은 파일명으로 교체함(1차는 커밋되지 않음).

## 기존 구조(바꾸기 전)

- `GalleryScreen` = `VisitCalendarDrawer` → `Box` → `Scaffold(containerColor = GalleryPaperWhite)`. 상태바는 `MainNavHost`의 `safeDrawingPadding`이 처리해 화면 밖.
- 기본 topBar `Column`에는 배경이 없고 제목 `Row`만 `GalleryPaperWhite` → **시계·커피 줄은 실제로 투명**했어. M3 Scaffold는 본문 다음에 topBar를 그리고, 월별 `LazyVerticalGrid`가 화면 전체 크기(contentPadding으로 header만큼 비움)라 위로 스크롤한 엽서가 시계 줄 뒤로 비치는 구조였어. 같은 색이라 눈에 안 띄었던 것.
- 월별·기억 밀도·검색 빈 상태·엽서 0장 화면이 각자 전체 화면 `GalleryPaperWhite`를 칠했어. 검색·선택 topBar는 Row 배경과 1dp 구분선이 이미 불투명.

## 앱에 달라진 점과 구조

- 기본 topBar `Column(modifier = Modifier.background(GalleryPaperWhite))` — header 전체 불투명. 스크롤한 엽서·글씨·그림자는 header 뒤로 가려짐(z-order로 해결, 별도 clip 없음). 구분선·시계·커피·김 animation은 그대로.
- `GalleryPaperBackground`(private composable): `ImageBitmap.imageResource` 1회 → `drawWithCache`에서 `ImageShader(Repeated, Repeated)` + `setLocalMatrix(setScale(s, s))`로 타일 한 장을 `GALLERY_PAPER_TILE_SIZE = 360.dp`로 같은 배율 축소 반복. 먼저 `GalleryPaperWhite`를 칠하고 타일을 `GALLERY_PAPER_TILE_ALPHA = 1f`로 겹침(0.5에서는 안 보여 QA 뒤 1로 올림). 늘림·crop 없음.
- 배치: pager `Box` 첫 자식으로 `padding(top = paddingValues.calculateTopPadding())` → 구분선 바로 아래부터, pager 밖이라 스크롤·보기 전환에 고정. 엽서 0장 빈 상태는 `matchParentSize`로 같은 composable 사용(두 곳은 서로 배타 분기라 동시에 한 레이어).
- 월별·기억 밀도·검색 빈 상태 페이지의 전체 배경 제거(카드·칩·월 헤더 자체 색은 유지).
- 양떼목장·쫑쫑컵(`SheepRanchStage`)은 사용자 결정으로 종이 없이 그대로. 퀵 셀렉트·흔들어서 한 장·drawer·popup·callback·navigation 변경 없음.
- Room·migration·serialization·dependency 변경 없음.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 893/893 (93 XML) | 신규 `GalleryPaperBackgroundStructureTest` 5건, `GalleryMonthlyGridStructureTest` 배경 개수 1→0 |
| assembleDebug / assembleDebugAndroidTest | 성공 | 최종 alpha 1f 코드로 재실행 |
| emulator instrumentation | 미실행 | androidTest 변경 없음, 이 셸에 adb 명령 없음, 기존 미실행 7건 유지 |
| 실기기 QA | 완료 | 질감 보임·대만족, 가로 이음매 선 없음, header 가림·검색·다중 선택·퀵 셀렉트·흔들어서 한 장·연못 정상 |
| TEST-COVERAGE-MAP | 갱신 완료 | 888→893, 파일 91→92, 구조 147→152 |
| git diff --check / LF | 깨끗 / LF 유지 | |
| commit / push / CI | 작업지시서 27절에 따라 진행 | 결과는 완료보고·`git log`·GitHub Actions |

## 남은 위험

- 종이 타일 상주 메모리 약 6.3MB(1254²×4). 퀵 셀렉트 손 5장 약 31MB와 합쳐 저사양 기기 부담 가능 — 문제 시 축소 디코드가 후보.
- 위아래 이음매 1px 밝기 단차: 시험한 기기에서는 안 보였지만 다른 밀도·밝기 설정 기기에서는 미확인.
- 가로 화면·태블릿·아주 작은 화면의 종이 모습 미확인(타일 반복이라 늘림은 없음).
- 연못 파문이 종이 위에서 그려지는 모습은 QA에서 정상이라고 했지만 세부 미감은 따로 보지 않았어.

## 다음 후보(승인된 작업 아님)

- 90일차: 사이드바 방문 달력 종이 질감 + 이전/다음 달 이동 시 달력 한 장이 넘어가는 페이지 물성(월별↔연도별 전환 slide는 유지 예정)
- `PhotoStickerEdgeStyleInstrumentedTest` 7건 검증 전용 emulator 실행, 퀵 셀렉트 PNG 메모리 최적화

---

# 이전 기록 — 88일차: 갤러리 퀵 셀렉트 미감 정비(신문 오림 손 부채)

확인일: 2026-09-29. 수동 표준 모드(88일차 작업지시서). 기능은 더하거나 빼지 않고, 갤러리 우측 하단 + 클러스터의 표현만 사용자가 준비한 신문 오림 손 5장이 부채처럼 펼쳐지며 기능을 내미는 모습으로 바꿨어. 로컬 자동검증과 사용자 실기기 QA 2회(1차 보정 요청 → 2차 통과)를 마쳤고, 작업지시서 34절에 따라 commit·push·CI 확인까지 진행해(실제 commit hash·CI는 완료보고·`git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, 시작 HEAD `6621e12` "Let cutout stickers reach the postcard edge by their visible part"(CI run `36400626851` 성공), origin `0/0`
- 커밋 대상: `ui/gallery/GalleryScreen.kt` 수정 / 새 파일 `ui/gallery/GalleryQuickSelectFan.kt`, `GalleryQuickSelectFanTest.kt`, 손 PNG 5장 / `GalleryViewSelectionStructureTest.kt`, `AppIntroVisitPostmarkStructureTest.kt` 수정 / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 기존 퀵 셀렉트 구조(바꾸기 전)

- `GalleryScreen.kt`의 `GalleryFabCluster`: 오른쪽 아래 + 버튼 → 큰 원 3개(카메라·미래 우체통·"특별한 갤러리" 묶음 토글) + 작은 원 3개(연못·양떼목장·쫑쫑컵), 실행 버튼 5 + 묶음 토글 1 = 6개. 68일차 롱프레스 드래그 바로 실행, 탭/드래그 공용 `dispatchDragTarget`.
- 실행: 카메라 → `navigate("camera")`, 미래 우체통 → `navigate("futureMailbox")`, 놀이 3종 → `onPlayModeSelected`(같은 모드 재선택 시 NONE, 선택 해제·메뉴 닫힘).
- 노출: 다중 선택 중에만 숨김(`visible = !selectionMode`). 검색·놀이 모드 중에는 보임(놀이 모드 출구). 방문 달력 drawer 아래, 흔들어서 한 장 overlay 아래. 뒤로가기(`BackHandler(enabled = fabMenuExpanded)`)·20% 딤 바깥 탭으로 닫힘. 흔들기 인식 시 강제로 닫힘.

## 앱에 달라진 점

- 닫힌 상태: 오른쪽 아래 끝에 손끝·블록 일부만 삐져나온 작은 손 묶음(0.8배, 손끼리 1.2°씩 어긋남). 그 자리 60×64dp가 손잡이(탭 = 열기/닫기, 접근성 "바로가기 열기/닫기").
- 열기: 묶음째 쑤욱(130ms) → 아래로 5° 젖힘(60ms) → 촤라락 부채(170ms, 손별 약 14ms 어긋남). 닫기는 역방향(모임 120ms → 빠짐 110ms). 도중 반대 요청은 그 자리에서 방향만 바꿔.
- 손 매핑(아래→위): 카메라 손 = 카메라, 편지 손 = 미래 우체통, 강물 손 = 엽서의 연못, 양 손 = 양떼목장, 체커 손 = 엽서 쫑쫑컵. 기능 callback·navigation·노출 조건·뒤로가기·바깥 탭(딤)은 그대로.
- 켜져 있는 놀이 모드의 손은 블록 테두리에 옅은 금색(SunsetGold) 선. 누르면 0.96배 짧은 눌림 + 확정 진동(22ms/160), 손잡이 탭은 가벼운 진동(10ms/90).
- **사용자 승인으로 빠진 것:** "특별한 갤러리" 묶음 단계(다섯 손이 한 단계), 68일차 롱프레스 드래그 바로 실행과 그 진동, 옛 원형 버튼·선택 링·물방울 pulse. 닫힌 위치는 오른쪽 아래 유지(권장안 승인). 롱프레스 드래그는 사용자가 불필요하다고 봐서 애초에 지시하지 않은 것으로, 완료 뒤 사용자 요청으로 [DECISIONS](DECISIONS.md) 2026-09-29 항목에 제품 결정으로 기록함(복원 대상 아님).
- 저장 데이터·Room·migration·serialization·dependency 변경 없음.

## 자산

- `app/src/main/res/drawable-nodpi/quick_select_{camera,letter,sheep,checker,river}_hand.png` — 사용자 제공 원본 그대로(crop·보정 없음). 모두 1254×1254 32bpp ARGB 투명 PNG, 손목은 오른쪽 가장자리에서 들어옴(우측 가장자리 불투명 행: camera 631~909, letter 696~992, sheep 619~920, checker 617~933, river 670~929).
- 블록 경계(원본 px, 눈으로 실측): camera 243,405–692,718 / letter 113,432–590,750 / river 243,403–692,713 / sheep 280,377–723,722 / checker 162,413–653,725. `GalleryQuickSelectItem`에 상수로 둠.
- 기존 손 오버레이와 같은 `ImageBitmap.imageResource` + `FilterQuality.Medium`(원본 해상도).

## 구조와 결정

- `GalleryQuickSelectFan.kt`(순수): `GalleryQuickSelectPhase`(CLOSED/OPENING/OPEN/CLOSING, `onRequest`·`onAnimationFinished`·`onSelect`), `GalleryQuickSelectItem`(슬롯·블록 좌표), 부채 geometry.
- 부채: 화면 오른쪽 밖 가상 pivot 하나. 각 손은 블록 중심이 pivot에서 반지름 440dp 위에 오고 손 가로축이 반지름과 나란하게 같은 각도로 돈다(`graphicsLayer` translation + 블록 중심 transformOrigin + rotationZ). 이웃 각도 9°(작은 화면은 7.6°까지 clamp) → 슬롯 각도 −18°/−9°/0°/+9°/+18°, 이웃 블록 중심 간격 약 69dp. 가운데 손 블록 중심 = 오른쪽 끝에서 92dp, 맨 아래 블록 중심 = 바닥에서 64dp. 손 표시 크기 180dp.
- 닫힌 묶음: 전부 맨 아래 슬롯 각도 근처, 반지름 −72dp(오른쪽으로 물러남), 0.8배, 닫힌 자리만 24dp 더 아래(`quickSelectAnimatedBlockCenter`).
- 겹침 순서: 위 손부터 그려 아래 손이 위 손의 손바닥을 덮음(블록은 안 가림). 터치 영역은 블록 + 좌우 6dp, 높이 48dp(이웃 손과 겹치지 않는 한계 `r_in·tan(Δ/2)`로 제한). 회전된 layer 안이라 터치 판정도 회전을 따라감. 손잡이는 손들보다 먼저 둬 겹치는 곳은 손이 입력을 받음.
- 입력 보호: 기능은 OPEN에서만 실행, 탭 즉시 CLOSING으로 넘어가 연타해도 callback 1회. CLOSED에서는 손 터치 영역·접근성 노드 자체가 없음. 열기/닫기 연타는 같은 방향이면 재시작하지 않음.
- 연출 값(`Animatable` 3개: slide·tilt·spread)은 `graphicsLayer` 안에서만 읽음.

## 실기기 QA 결과

- 1차: 기능·닫기·연타·속도 정상. 요청 = "닫힌 손잡이 조금 더 아래", "손 사이 간격이 빡빡하고 겹치는 부분이 어색" → 반지름 400→440dp, 이웃 각도 8→9°(블록 간격 약 56→69dp, 기울기 ±16→±18°), 닫힌 자리만 24dp 하강.
- 2차: 손잡이 위치·간격·기능 전부 "좋아/전부 정상".

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 888/888 (92 XML) | 신규 `GalleryQuickSelectFanTest` 14건, 구조 테스트 12→11 |
| assembleDebug / assembleDebugAndroidTest | 성공 | |
| emulator instrumentation | 미실행 | androidTest 변경 없음, 연결된 emulator 없음(`adb devices -l` 빈 목록), 기존 미실행 7건 유지 |
| 실기기 QA | 완료(2차 통과) | 위 결과 |
| TEST-COVERAGE-MAP | 갱신 완료 | 875→888, 파일 90→91, 구조 148→147 |
| Room / migration / serialization / dependency | 변경 없음 | |
| commit / push / CI | 작업지시서 34절에 따라 진행 | 결과는 완료보고·`git log`·GitHub Actions |

## 남은 위험

- 손 PNG 5장을 원본 해상도로 상주 로드(장당 약 6.3MB, 합 약 31MB). 저사양 기기 메모리 압박 가능성 — 문제 시 표시 크기에 맞춘 축소 디코드가 후보(질감 손실 확인 필요).
- 블록 좌표는 눈 실측이라 수 px 오차 가능(터치 영역·금색 테두리 위치). QA에서 문제는 없었어.
- 롱프레스 드래그 바로 실행을 쓰던 사용 습관은 사라짐(승인된 제거).
- 가로 화면·아주 작은 화면(높이 약 400dp 미만)에서는 각도가 7.6°까지만 줄어 맨 위 손이 잘릴 수 있어(실기기 미확인).

## 다음 후보(승인된 작업 아님)

- 퀵 셀렉트 미감 추가 보정(사용자 요청 시)
- `PhotoStickerEdgeStyleInstrumentedTest` 7건 검증 전용 emulator 실행
- 사진 복사 중 고아 파일, 기본 모양 사진 스티커 회전 문제(88일차 범위 밖으로 유지)

---

# 이전 기록 — 87일차 후속: 누끼 스티커 이동 경계 수정

확인일: 2026-09-28. 수동 표준 모드. 마스킹테이프 쓸어 붙이기(`b9cd53b`, CI run `36398477330` 성공)를 마감한 직후 사용자가 제보한 버그를 고쳤어: "스티커 배경 제거 후 크기를 조절하니 원본 사진 크기가 테두리처럼 작용해서 원하는 위치까지 이동 못 함". 사용자 선택은 "보이는 부분 기준". 로컬 자동검증과 사용자 실기기 QA를 마쳤고, 사용자 요청("커밋하고 푸시해줘")으로 commit·push해(실제 commit hash·CI는 `git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, 시작 HEAD `b9cd53b` "Place masking tapes with a pressing hand sweep", origin `0/0`
- 커밋 대상: `DetailScreen.kt`, `DetailViewModel.kt`, `PhotoStickerPlaceInteraction.kt`, `utils/PostcardImageExporter.kt` 수정 / 새 파일 `PhotoStickerCutoutBounds.kt`, `PhotoStickerCutoutBoundsTest.kt` / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 원인

사진 스티커는 늘 정사각형 칸(`STICKER_BASE_SIZE × scale`) 안에 그려지고, 누끼는 그 칸 안에 Fit으로 놓인 투명 PNG야. 끌기·크기·조준·저장된 자리 재보정·저장본(exporter)이 모두 `clampStickerOffset`(칸 전체가 엽서 안)을 써서, 투명 여백이 먼저 엽서 가장자리에 걸려 보이는 부분을 가장자리까지 옮길 수 없었어. 키울수록 칸이 커져 더 심해져.

## 앱에 달라진 점

- 누끼 스티커는 보이는 부분(사람·물건)의 테두리가 엽서 가장자리에 닿을 때까지 옮길 수 있어. 투명 여백은 엽서 밖으로 나가고(엽서 경계에서 잘려 안 보임), 보이는 부분은 잘리지 않아. 끌기·두 손가락 확대/회전·크기 핸들·회전 모드/핸들·조준 미리보기 모두 같은 규칙이야.
- 기본 모양(누끼 아닌) 스티커는 예전과 똑같이 칸 전체가 엽서 안.
- 저장·공유 이미지도 화면과 같은 자리에 그려(누끼 칸이 캔버스 밖으로 나가도 당기지 않음). 저장 형식·Room은 그대로 — offset이 음수이거나 엽서보다 클 수 있게 됐을 뿐 같은 필드야.

## 구조와 결정

- 새 파일 `PhotoStickerCutoutBounds.kt`: `cutoutStickerVisibleExtent`(불투명 표본점을 뒤집기→회전해 중심 기준 보이는 범위, 표본 칸 반 대각선만큼 여유), `clampPhotoStickerOffset`(기본 = `clampStickerOffset`, 누끼+표본점 = 보이는 부분만 엽서 안, 누끼+표본점 없음 = 느슨한 규칙: 칸 중심이 엽서에서 한 변의 0.75배 이상 벗어나지 않게).
- 두 규칙의 역할: **정확한 규칙은 사용자 제스처(끌기·크기·회전·조준)에서만**, **느슨한 규칙은 저장된 자리 재보정 LaunchedEffect와 exporter에서** 써. 보이는 부분은 늘 회전한 칸 안(중심에서 한 변의 약 0.72배 이내)이라 정확한 규칙으로 놓은 자리는 느슨한 규칙에서 절대 당겨지지 않아(테스트로 고정) → 표본점을 줄여 읽은 결과가 조금 달라도 다시 열 때 스티커가 움직이지 않고, 화면 = 저장본이야.
- 표본점: 조준 중 배경제거 결과는 이미 계산한 값을 붙일 때 `rememberStickerCutoutSilhouette`로 넘겨. 그 밖의 누끼(기존 `배경제거` 버튼, 다시 연 엽서)는 `loadStickerCutoutSilhouette`가 그림을 긴 변 512px 이하로 줄여 읽어 계산해(uri당 한 번, 읽기만, 실패 = 빈 목록 → 느슨한 규칙). 화면 전용 메모리 값이라 저장하지 않아.
- 조준: `clampPhotoStickerAimCenter`에 누끼 표본점·각도를 넘기면 같은 규칙. `원본복원` 토글 때 칸 기준으로 다시 가둬(`toggleAimBackground(postcardSize, base)`, 기본값이면 예전 동작).
- 두 손가락 확대·회전 중(`Move` 모드 멀티터치)에는 예전처럼 offset을 그대로 두되, 누끼만 새 크기·각도로 정확한 규칙을 적용해.
- exporter: `createStickerOverlayForExport`는 누끼일 때 느슨한 규칙과 정규화 범위 -1~2, `PostcardImageExporter.drawStickerOverlay`는 누끼일 때 칸을 캔버스 안으로 당기지 않아.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 875/875 (91 XML) | 신규 `PhotoStickerCutoutBoundsTest` 9건 |
| assembleDebug | 성공 | |
| emulator instrumentation | 미실행 | androidTest 변경 없음, 기존 미실행 7건 유지 |
| 실기기 QA | 완료 — 사용자가 "QA 완료" 보고, 보정 요청 없음(항목별 세부 결과는 받지 않음) | 아래 체크리스트 |
| TEST-COVERAGE-MAP | 갱신 완료 | 866→875, 파일 89→90 |
| Room / migration / serialization / dependency | 변경 없음 | |
| commit / push / CI | 사용자 요청으로 진행 | 결과는 완료보고·`git log`·GitHub Actions에서 확인 |

## 실기기 QA 체크리스트 (누끼 스티커 경계 — 사용자 QA 완료, 기록용)

1. 스티커를 붙이고 `배경제거` → 크게 키운 뒤 끌어서, 보이는 부분이 엽서 네 가장자리에 딱 닿을 때까지 가는지(투명 여백 때문에 멈추지 않는지), 보이는 부분이 잘리지 않는지
2. 누끼 스티커를 돌리거나(회전 모드·핸들·두 손가락) 뒤집은 뒤에도 가장자리까지 가고 잘리지 않는지
3. 조준 중 `배경제거` 후 가장자리로 조준 → 붙이기 → 조준한 자리 그대로인지, `원본복원`하면 칸이 엽서 안으로 들어오는지
4. 엽서를 나갔다 다시 열어도 가장자리에 붙인 누끼 스티커가 그 자리 그대로인지
5. 저장·공유 이미지에서 가장자리 누끼 스티커가 화면과 같은 자리에 있는지
6. 기본 모양 스티커(폴라로이드 등 포함)는 예전처럼 칸 전체가 엽서 안에서 멈추는지

## 남은 위험

- 누끼 스티커를 가장자리에 붙인 뒤 `원본복원`하면 칸 전체가 보이므로 재보정 규칙이 칸을 엽서 안으로 당겨(자리 이동). 의도된 동작이지만 QA에서 어색한지 확인 필요.
- 표본점은 48×48 격자라 보이는 테두리와 엽서 가장자리 사이에 최대 한 변의 약 1.5% 틈이 남을 수 있어(잘리지 않는 쪽을 택함).
- 표본점을 읽기 전 아주 짧은 순간 끌면 느슨한 규칙이 적용돼 보이는 부분이 엽서 밖으로 나갈 수 있어(읽기가 끝난 뒤 다시 끌면 맞음).
- exporter의 캔버스 밖 그리기는 JVM 테스트로 검사하지 못했어(`Uri` 필요) — QA 5번.

---

# 이전 기록 — 87일차 후속: 마스킹테이프 쓸어 붙이기

확인일: 2026-09-28. 수동 표준 모드. 87일차 작업지시서의 선택 목표였던 마스킹테이프 밀착 연출을 사용자 선택("조준 후 붙이기", "밀착 효과 살짝 넣기")으로 구현했어. 첫 QA 전 사용자 요청으로 "추가하면 세부 편집창이 바로 뜨고, 조준 중 `취소 | 편집 | 붙이기`" 흐름을 더했어(사용자 선택: 팝업 + 하단 편집 버튼). 로컬 자동검증과 사용자 실기기 QA를 마쳤고, 사용자 요청("커밋하고 푸시해줘")으로 commit·push해(실제 commit hash·CI는 `git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, HEAD `ec66fce` "Place label and text stickers with the tweezer hand"(CI run `36394271108` 성공), origin `0/0`
- 커밋 대상: `DetailScreen.kt`, `DetailViewModel.kt`, `MaskingTapeDetailScreen.kt` 수정 / 새 파일 `MaskingTapePlaceInteraction.kt`, `MaskingTapePlaceSessionTest.kt`, 이제 쓰는 자산 `tape_press_hand.png` / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존

## 앱에 달라진 점

- 테이프 `+ 추가`(기본 디자인·커스텀·사진 세 방식 모두)에서 고르면 바로 붙지 않고, 붙인 테이프 `편집`과 같은 세부 편집창(가장자리·길이·굵기·회전, 창 안 미리보기가 즉시 바뀜)이 곧바로 떠. `저장`하면 엽서 위 +와 반투명 미리보기에 그 모양·길이·각도가 반영되고, `취소`는 값만 버려(추가는 계속). 탭·드래그로 자리, 두 손가락으로도 각도를 바꿀 수 있어. 패널은 안내 한 줄과 `취소 | 편집 | 붙이기`만 보여 — `편집`은 창을 다시 열고(현재 조준 각도에서 시작), `취소`는 추가 자체를 취소해.
- `붙이기` → 신문 오림 손이 화면 아래에서 올라와 테이프 한쪽 끝에 손가락을 대고(이 순간 실제 테이프 생성·"톡" 진동), 테이프 긴 축을 따라 반대쪽 끝까지 한 번 쓸고 빠져. 왕복·문지르기·흔들림·반동 없음.
- 밀착 연출: 손가락이 닿은 뒤 손 앞쪽(아직 안 눌린 부분)만 종이색 막(32%)으로 살짝 옅게 덮이고, 손이 지나간 자리부터 제 색이 돼. 쓸기가 끝나면 60ms에 걸쳐 사라져. 연출 overlay 안에서만 그리므로 저장·공유 이미지와 무관해.
- 추가 1번 = undo 1건, 붙인 뒤 선택 상태. 저장 형식·Room·serialization은 그대로(기존 offset·rotationDegrees만 채움). 복제·편집·삭제·끌기는 그대로.

## 구조와 결정

- 새 파일 `MaskingTapePlaceInteraction.kt`: `MaskingTapePlaceSession`(Idle→Aiming→Placing(placed)→Idle, 초안을 돌려주는 `startAiming`/`cancelAiming`/`abandon`, 편집창 값을 받는 `editAim` — 범위 제한 후 새 크기로 자리 재보정), 순수 계산 `maskingTapeSweep`·`maskingTapeUnpressedRange`·`maskingTapeSizePx`, `MaskingTapeAimLayer`, `MaskingTapePressHandOverlay`. 조준 +·진동은 스티커 것(`StickerPlaceCrosshair`·`vibratePhotoStickerContact`)을 그대로 써.
- 좌표: 조준 중심 = 엽서 미리보기 좌상단 기준 px, 붙인 테이프 끌기와 같은 `clampStickerOffset` 규칙(회전 전 사각형이 엽서 안)으로 가둬서 조준 = 착지. 크기는 붙인 테이프 Box와 같은 `132dp×scale×lengthScale`, `40dp×scale×thicknessScale`을 `roundToPx`. 이를 위해 `MASKING_TAPE_BASE_WIDTH/HEIGHT`를 private → internal로만 바꿨어(값 그대로).
- 쓸기 경로: 긴 축 방향 = 테이프 각도의 `(cos, sin)`(화면 가로 아님). 손이 뒤집히지 않도록 각도를 (-90°, 90°]로 접어서 늘 대체로 왼쪽→오른쪽(세로면 위→아래)으로 쓸고, 손 이미지도 그 각도만큼 돌려. 양 끝에서 테이프 굵기의 절반만큼 안쪽이 시작·끝점(짧으면 중심으로 모임).
- 손 anchor: `tape_press_hand.png`(1254×1254) 손가락 부분(y 90~500) 불투명 영역 가운데 약 (500, 325) → `MASKING_TAPE_HAND_ANCHOR_X/Y = 0.399/0.259`, 손 크기 260dp. 진입 320 → 누름 60 → 쓸기(길이 dp×2.6ms, 360~760ms, FastOutSlowIn) → 뗌 60 → 퇴장 220ms. 진입·퇴장 모양(아래에서 원근 +12%, 기울기 4°)은 핀셋 손과 같아.
- 사진 테이프: `addPhotoMaskingTape` → `importMaskingTapePhoto`(복사만, 콜백) + `addPlacedMaskingTape`. 예전에는 복사 전에 undo를 기록해 실패해도 빈 undo가 남았는데 이제 실제 추가 때만 생겨. 조준하다 버린 사진(취소·교체·화면 이탈)은 기존 `deleteMaskingTapePhotoIfUnreferenced`(현재 테이프·undo/redo가 참조하면 남김)로 정리해. 복사를 기다리는 사이 조준할 수 없게 되면(탭 이동·크게보기·뒷면·엽서 크기 미측정) 예전처럼 가운데에 바로 붙여.
- 조준 편집창: 기존 private `MaskingTapeEditDialog`를 그대로 재사용(패널 안에서 새 조준 id가 생기면 자동으로 열림). 새 dialog·새 slider는 만들지 않았어.
- 입력 잠금·취소: overlay가 모든 pointer 소비, 패널 `enabled && !isPlacing`, 조준 중 undo/redo 비활성. 테이프 탭 이탈·크게보기·뒷면이면 조준 취소, 뒤로가기는 조준만 취소하고 손 연출 중엔 흘려보냄.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 | |
| 로컬 JVM | 통과 866/866 (90 XML) | 신규 `MaskingTapePlaceSessionTest` 16건 |
| assembleDebug / assembleDebugAndroidTest | 성공 | |
| emulator instrumentation | 미실행 | androidTest 변경 없음, `PhotoStickerEdgeStyleInstrumentedTest` 7건은 여전히 미실행 |
| 실기기 QA | 완료 — 사용자가 "QA 완료" 보고, 보정 요청 없음(항목별 세부 결과는 받지 않음) | 아래 체크리스트 |
| TEST-COVERAGE-MAP | 갱신 완료 | 850→866, 파일 88→89 |
| Room / migration / serialization / dependency | 변경 없음 | |
| commit / push / CI | 사용자 요청으로 진행 | 결과는 완료보고·`git log`·GitHub Actions에서 확인 |

## 실기기 QA 체크리스트 (마스킹테이프 — 사용자 QA 완료, 기록용)

1. 기본 디자인·커스텀·사진 테이프 각각 `+ 추가` → 세부 편집창이 바로 뜨는지, `저장` 후 엽서 위 미리보기에 길이·굵기·각도·가장자리가 반영되는지(`취소`면 기본값 그대로 조준), 탭·드래그·두 손가락 회전, 하단 `취소 | 편집 | 붙이기`와 `편집`으로 다시 열기(돌린 각도에서 시작)가 되는지
2. 붙이기: 손가락이 테이프 끝에 닿는 순간 테이프가 생기고 "톡" 진동, 손이 긴 축을 따라 한 번만 쓸고 빠지는지(돌린 테이프도 축을 따라가는지), 조준한 자리·각도에 어긋남 없이 붙는지
3. 밀착 막이 "살짝"으로 보이는지(너무 진하거나 안 보이지 않는지), 손 크기·손가락 닿는 위치·쓸기 속도가 어색하지 않은지
4. undo 한 번에 방금 테이프만 사라지고 redo로 돌아오는지, 조준 중 취소·뒤로가기·탭 이동 시 생성 안 되는지
5. 사진 테이프 조준을 취소한 뒤에도 다른 사진 테이프·복제·저장이 정상인지
6. 스티커 붙이기·도장 회귀 없음

## 남은 위험

- 밀착 막은 화면 최상단 overlay에 그려서, 테이프 위에 겹친 스티커·도장이 있으면 쓸기 동안(최대 약 0.8초) 그 부분도 살짝 옅게 덮여 보일 수 있어.
- 조준 미리보기는 스티커 조준처럼 맨 위에 보이지만, 붙은 테이프는 원래 순서대로 스티커 아래에 그려져.
- 사진 테이프 복사가 끝나기 전에 화면 자체를 떠나면, 늦게 온 콜백이 사라진 화면의 조준을 시작해 그 복사본이 고아 파일로 남을 수 있어(사진 스티커와 같은 위험, 읽기 전용 `OrphanFileDiagnostics` 대상).
- 손 anchor·크기·쓸기 속도는 이미지 측정값이라 실기기 QA로 보정할 수 있어.

---

# 이전 기록 — 87일차 후속: 라벨·텍스트 스티커 핀셋 붙이기

확인일: 2026-09-28. 수동 표준 모드. 사진 스티커 핀셋 붙이기(`c8a2cf5`, CI run `36392041882` 성공)에 이어, 사용자가 고른 순서대로 라벨 스티커와 텍스트 스티커에 같은 조준·핀셋 붙이기 문법을 옮겼어(텍스트는 처음에 조준만 넣었다가 사용자 요청으로 핀셋 손을 추가). 로컬 자동검증과 사용자 실기기 QA를 마쳤고, 사용자 요청으로 commit·push해(실제 commit hash·CI는 `git log`·GitHub Actions로 확인). **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`, HEAD `c8a2cf5` "Place photo stickers with a tweezer hand interaction", origin `0/0`
- 작업트리(미커밋): `DetailScreen.kt`, `LabelStickerDetailScreen.kt`, `TextStickerDetailScreen.kt`, `PhotoStickerPlaceInteraction.kt` 수정 / 새 파일 `LabelStickerPlaceInteraction.kt`, `TextStickerPlaceInteraction.kt`, `LabelStickerPlaceSessionTest.kt`, `TextStickerPlaceSessionTest.kt` / 이 HANDOFF·TEST-COVERAGE-MAP
- 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존. `tape_press_hand.png`는 미사용이라 여전히 untracked

## 앱에 달라진 점

- **라벨 스티커**: 문구·테이프를 고르고 확인하면 바로 붙지 않고 엽서 위 +와 반투명 미리보기로 자리·각도를 정해(붙인 라벨 편집처럼 크기 조절은 없음). `취소 | 붙이기` → 사진 스티커와 같은 핀셋 손이 라벨을 집어 와 톡 놓아. 핀셋은 회전한 라벨에서 화면 아래를 향한 변의 가운데를 물어.
- **텍스트 스티커**: 같은 조준(자리·크기 0.5~3배·각도)과 `취소 | 붙이기` → 핀셋 손이 외곽선 있는 글자 스티커를 집어 와 톡 놓아. 핀셋은 회전한 글자 영역(측정한 사각형)에서 화면 아래를 향한 변의 가운데를 물어 — 글자 모양이 아니라 사각형 기준이라 글자 사이 빈 곳을 무는 것처럼 보일 수 있어(QA로 판단).
- 둘 다 추가 1번 = undo 1건, 붙인 뒤 선택 상태. 저장 형식·Room은 그대로(기존 offset·scale·rotationDegrees만 채움).

## 구조와 결정

- 공용화한 최소 조각(`PhotoStickerPlaceInteraction.kt`): `clampStickerAimCenter`(직사각형판 조준 가두기), `rectStickerGripOffset`, `StickerPlaceCrosshair`, `StickerTweezerHandOverlay`(들고 갈 그림과 집는 자리만 받는 핀셋 손). 사진 스티커는 기존 동작 그대로 이 조각을 쓰도록 나눴어(상수·타이밍 변경 없음).
- 라벨: `LabelStickerPlaceSession`(Idle→Aiming→Placing(placed)→Idle). 크기는 `LabelStickerContent`와 같은 `LabelStickerRenderer`로 계산(`labelStickerSizePx`)해 조준 = 착지. 파일이 없어 정리할 것도 없어.
- 텍스트: `TextStickerPlaceSession`(Idle→Aiming→Placing(placed)→Idle). 크기는 글자 모양 측정이라 미리보기가 `onSizeChanged`로 잰 실제 크기를 쓰고, `붙이기` 때 마지막 크기로 자리를 한 번 더 가둬 고정해서 손 목표 = 착지야. 핀셋 손·집는 자리 계산은 라벨과 같은 `StickerTweezerHandOverlay`·`rectStickerGripOffset`.
- 조준 자동 취소: 각 하위 탭 이탈·크게보기·뒷면. 뒤로가기는 조준 취소, 손 연출 중엔 흘려보냄. 손 연출 중 각 패널 비활성. 엽서 크기를 아직 모르면 예전처럼 가운데에 바로 붙여.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 라벨·텍스트 완료 | |
| 로컬 JVM | 통과 850/850 (89 XML) | 신규 라벨 10건·텍스트 7건 |
| assembleDebug / assembleDebugAndroidTest | 성공 | |
| emulator instrumentation | 미실행 | 연결 기기·emulator 없음(앞 확인 기준) |
| 실기기 QA | 완료 — 사용자가 "QA 완료" 보고, 보정 요청 없음(항목별 세부 결과는 받지 않음) | 아래 체크리스트 |
| TEST-COVERAGE-MAP | 갱신 완료 | 833→850, 파일 86→88 |
| Room / migration / serialization / dependency | 변경 없음 | |
| commit / push / CI | 사용자 요청("커밋하고 푸시해줘")으로 진행 | 결과는 완료보고·`git log`·GitHub Actions에서 확인 |

## 실기기 QA 체크리스트 (라벨·텍스트 — 사용자 QA 완료, 기록용)

1. 라벨 추가 → 조준 미리보기가 실제 라벨과 같은 모양·크기로 가운데에 뜨고, 탭·드래그·두 손가락 회전이 되는지(크기는 안 바뀜)
2. 라벨 붙이기: 핀셋이 라벨 아래 변(많이 돌리면 옆 변)을 문 것처럼 보이는지, 도착 순간 어긋남 없이 조준한 자리·각도에 놓이는지, "톡" 진동
3. 텍스트 추가 → 조준 미리보기에서 자리·크기·각도 조절, 붙이기 → 핀셋이 글자 스티커를 자연스럽게 무는지(글자 사이 빈 곳을 무는 것처럼 보이지 않는지), 조준한 자리에 놓이는지(엽서 가장자리에서 키운 뒤 붙여도 밖으로 안 나가는지)
4. 라벨·텍스트 각각 undo 한 번에 방금 것만 사라지고 redo로 돌아오는지, 조준 중 취소·뒤로가기·탭 이동 시 생성 안 되는지
5. 사진 스티커 붙이기·도장·흔들기 회귀 없음

## 남은 위험

- 텍스트 미리보기는 첫 프레임에 크기를 아직 몰라 아주 잠깐 가운데 기준이 어긋나 보일 수 있어(측정 직후 바로 맞음).
- 라벨 크기 계산은 화면 `LabelStickerContent`와 같은 렌더러 함수지만 px→dp→px 반올림으로 1px 차이가 날 수 있어.
- 마스킹 테이프 밀착 연출은 여전히 미착수(별도 승인 필요).

---

# 이전 기록 — 87일차 꾸미기 손 인터랙션(사진 스티커 핀셋 붙이기)

확인일: 2026-09-28. 수동 표준 모드(공용 작업판 비활성). 87일차 작업지시서의 필수 목표인 사진 스티커 핀셋 붙이기(조준 중 배경제거 포함)를 구현하고 로컬 자동검증과 사용자 실기기 QA까지 마쳤어. commit·push는 작업지시서 40·41절의 마감 절차로 수행해(실제 commit hash는 `git log`로 확인). 선택 목표인 마스킹 테이프 밀착 연출은 미착수 — 사용자가 다음 순서로 "사진 스티커 QA → 라벨 스티커 핀셋 붙이기 → 텍스트 스티커(조준만)"를 골랐어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`
- 시작 HEAD: `dac9688` "Record final workflow closeout state", origin ahead·behind `0/0`, tracked clean
- 현재 작업트리: 아래 변경 파일이 **미커밋**. 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존
- 자산: 사용자가 넣은 `app/src/main/res/drawable-nodpi/sticker_tweezer_hand.png`, `tape_press_hand.png` (둘 다 1254×1254 RGBA PNG, 투명 배경). 첫 조사 때는 둘 다 없어서 자산 게이트 D로 조사만 하고, 사용자가 추가한 뒤 게이트 A로 진행했어.

## 앱에 달라진 점

- 사진 스티커를 갤러리·카메라·파일에서 고르면 바로 붙지 않고, 엽서 위 십자(+)와 반투명 미리보기로 자리·크기(0.5~2.5배)·각도를 먼저 정해. 패널은 안내 한 줄과 `취소 | 배경제거 | 붙이기`만 보여 줘(도장 찍기 조준과 같은 문법).
- 사용자 QA 첫 피드백("미리보기에 배경 제거가 안 돼서 불편")으로, 조준 중 `배경제거`/`원본복원` 토글을 추가했어(사용자가 선택지 중 "조준 중 누끼 토글"을 승인). 켜면 미리보기가 누끼 모양으로 바뀌고, 붙이면 처음부터 누끼 스티커로 생겨(undo 1건 그대로). 처리 중(`처리중...`)에는 토글·붙이기가 잠겨.
- `붙이기`를 누르면 신문 오림 핀셋 손이 스티커를 집은 채 화면 아래에서 올라와 조준한 자리에 놓고 빠져. 스티커는 손과 한 덩어리로 움직이고, 도착 순간 조준한 자리·크기·각도와 정확히 같아.
- 기존 엽서 데이터·저장 형식은 그대로야. 새 스티커는 기존 필드(offset·scale·rotationDegrees)만 채워 생성돼.

## 구조와 결정

- 새 파일 `ui/detail/PhotoStickerPlaceInteraction.kt`: `PhotoStickerPlaceSession`(Idle→Aiming→Placing(placed)→Idle), `PhotoStickerAimLayer`, `PhotoStickerTweezerHandOverlay`, `vibratePhotoStickerContact`. 도장(`SealStampInteraction.kt`)과 같은 모양이지만 코드는 공유하지 않아(공통 framework화 안 함).
- 좌표: 조준 중심 = 엽서 미리보기 좌상단 기준 px. 조준 중심을 붙인 스티커의 `clampStickerOffset`과 같은 규칙으로 가둬서 조준 = 착지. overlay 목표점은 도장과 같은 `postcardPositionInRoot - detailRootPositionInRoot + center`.
- 핀셋 anchor: 이미지에서 벌어진 집게 끝 약 (345,49)·(384,31) 바로 아래, 두 집게 사이 (372,56) → `PHOTO_STICKER_TWEEZER_ANCHOR_X/Y = 0.297/0.045`. 집는 점 = 스티커 중심에서 화면 아래로 `side/2 - 2dp`. 손·스티커는 이 점을 기준으로 함께 이동·기울기(4°)·원근 크기(+12%) 변환을 받아.
- 조준 중 배경제거: `DetailViewModel.removeAimStickerBackground` — 기존 `removeStickerBackground`와 같은 ML Kit·`saveStickerForegroundBitmap`(cache/photo_stickers/)을 쓰지만 undo·`stickerBackgroundRemovalState`는 건드리지 않아(아직 스티커가 아니라서). 결과는 기존 버튼이 만든 스티커와 같은 필드 모양(removedBgUri 보관, 켜졌을 때만 displayedUri)으로 채워. 조준이 취소·교체·출발한 뒤 늦게 온 결과는 세션이 돌려주고 바로 정리해.
- 누끼 핀셋 자리: 결과 그림을 48×48 격자로 훑어 alpha ≥128 표본점을 뽑고(Fit 배치 기준), 회전을 반영해 **화면에서 가장 아래 불투명 점**을 2dp 안쪽으로 물어. 기본 모양은 예전처럼 사각형 아래 변 가운데.
- 실제 스티커 생성 시점: 핀셋이 닿는 프레임 한 번(`takeContactPlacement` → `addPlacedPhotoSticker`). undo 1건 기록 + 추가 + 선택이 한 번에 일어나. 닿기 전·취소 후에는 생성 0건.
- 표현 중복 방지: 조준 미리보기는 닿는 순간 사라져. 들고 온 스티커는 실제 스티커가 확실히 그려지도록 같은 자리에 약 110ms(누름 40 + 멈춤 70) 겹친 뒤 손이 떼어질 때 사라져. 같은 자리·같은 그림이라 겹침은 보이지 않는 설계야(실기기 확인 필요).
- 타이밍: 진입 320 → 누름 40 → 멈춤 70 → 뗌 50 → 퇴장 220ms(도장 300/50/90/60/220 리듬). 안착 누름은 손만(1.2%), 스티커 크기는 변하지 않아(bounce 없음).
- 햅틱: 닿는 순간 한 번, 도장과 같은 Vibrator oneShot 방식에 14ms·세기 120("톡", 도장 28ms·200보다 약함).
- 입력 잠금: overlay가 모든 pointer를 소비, 스티커 패널 `enabled && !isPlacing`, 조준 중 undo/redo 비활성. 뒤로가기는 조준 중엔 조준만 취소, 손이 움직이는 동안은 흘려보냄(도장과 동일).
- 조준 자동 취소: 스티커 탭·사진 하위탭 이탈, 크게보기, 뒷면 전환.
- ViewModel: `addCameraPhotoSticker`/`addGalleryPhotoSticker` → `importCameraPhotoStickerOriginal`/`importGalleryPhotoStickerOriginal`(복사만 하고 콜백으로 원본 URI 전달) + `addPlacedPhotoSticker`. 예전에는 복사 전에 undo를 기록해서 복사가 실패해도 빈 undo가 남았는데, 이제 undo는 실제 추가 때만 생겨.
- 복사를 기다리는 사이 조준할 수 없는 상태(엽서 크기 미측정, 다른 탭, 크게보기·뒷면)가 되면 고른 사진을 잃지 않도록 예전처럼 가운데에 바로 붙여.
- 조준에 쓰다 버린 파일(원본·조준 중 배경제거 결과: 취소, 새 사진으로 교체, 화면 이탈, 늦은 결과)은 기존 `deleteStickerCacheUri` + `deleteStickerOriginalIfUnreferenced`로 정리해. 두 함수 모두 자기 폴더(`cache/photo_stickers/`·`sticker_bgs/` / `sticker_originals/`) 안의 `file://`이면서 현재 스티커·undo/redo 어디에서도 참조하지 않을 때만 지우고, 다른 폴더 파일과 SAF(파일에서 추가) URI는 건드리지 않아. 원본은 복사마다 새 UUID, 누끼 결과는 생성 시각 파일명이야.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 사진 스티커 완료 / 마스킹 테이프 미착수 | 테이프는 스티커 미감 QA 게이트 대기 |
| 로컬 JVM | 통과 833/833 (87 XML, 실패·오류·skip 0) | 신규 `PhotoStickerPlaceSessionTest` 20건 포함(조준 중 배경제거 8건 추가분 포함) |
| assembleDebug / assembleDebugAndroidTest | 성공 | androidTest 소스 변경 없음 |
| emulator instrumentation | 미실행 | `adb devices -l` 결과 연결 기기·emulator 0개 |
| `PhotoStickerEdgeStyleInstrumentedTest` 7건 | 미실행 유지 | 위와 같음. 검증 전용 emulator에서만 후속 실행 |
| 실기기 QA | 완료 — 1차 피드백(누끼 미리보기) 반영 후 사용자가 "QA완료" 보고, 추가 보정 요청 없음(항목별 세부 결과는 받지 않음) | 핀셋 위치·크기·속도 미감, 조준 중 배경제거, 누끼 스티커 집는 자리, 조준 gesture, 진동, undo/redo, 도장·흔들기 회귀 |
| TEST-COVERAGE-MAP | 갱신 완료 | 813→833, 파일 85→86, 87일차 보호 범위 추가 |
| Room / migration / serialization / dependency | 변경 없음 | Entity·DAO·Migration·`PhotoStickerItem.serialize`·Gradle 미수정 |
| commit / push / CI | 작업지시서 40·41절 승인 범위로 진행 | 결과는 완료보고·`git log`·GitHub Actions에서 확인 |

## 작업 중 사고와 처리

- Python으로 파일을 다시 쓰는 과정에서 `DetailScreen.kt`, `PhotoStickerPlaceInteraction.kt`의 줄바꿈이 LF→CRLF로 바뀌었어. 그 때문에 LF 빈 줄 두 개로 선언 끝을 찾는 기존 구조 테스트 `BackgroundColorPickerEnabledStructureTest` 1건이 실패했어(분류: test infrastructure / 작업자 실수, production 아님). 두 파일을 원래 LF로 되돌린 뒤 당시 전체 825/825 통과를 확인했어. 테스트 조건은 바꾸지 않았어.

## 범위 밖 발견(수정 안 함)

- 조준 중 배경제거 계산을 테스트하다 `photoStickerGripOffset`이 집는 점을 중심 아래로만 제한(`max(0, …)`)하던 문제를 찾아 제거했어 — 누끼 그림이 모두 중심 위에 있으면 허공을 집게 되기 때문이야(아직 커밋 전 코드라 사용자 영향 없음).
- 기본 모양 사진 스티커는 화면에서 `fillMaxSize → clip(16dp 둥근 사각) → graphicsLayer(rotationZ)` 순서로 그려져, 코드 순서상 회전해도 둥근 틀은 돌지 않고 안의 사진만 도는 것으로 보여. 실제로 그렇게 보이는지, exporter와 일치하는지는 **미확인**이야. 핀셋 미리보기·들고 온 스티커는 같은 순서로 그려서 실제 스티커와 같게 맞췄고, 집는 거리도 이 전제(틀은 회전하지 않음)로 계산했어. 이 순서를 나중에 바꾸면 `photoStickerGripDistance`도 다시 봐야 해.

## 남은 위험과 재개 조건

1. 다음 작업(사용자 선택): 라벨 스티커에 같은 조준·핀셋 붙이기를 옮기고, 텍스트 스티커는 조준만(손 연출 없음) 넣어. 다른 꾸미기 영역으로의 문법 이전이라 각 영역 실기기 QA가 필요해. 텍스트·라벨은 크기가 글자에 따라 달라 조준 미리보기가 자기 크기를 재야 해.
2. 화면 이탈 시 원본 정리는 `viewModelScope`에서 실행돼. ViewModel이 같은 순간 정리되면 원본 파일 1개가 남을 수 있어(고아 파일 — 사용자 데이터 손상 아님, 기존 `OrphanFileDiagnostics` 진단 대상).
3. 사진 복사가 끝나기 전에 화면을 떠나면 복사 완료 콜백이 사라진 화면의 세션에 도착해 원본 1개가 남을 수 있어(위와 같은 고아 파일 위험).
4. 마스킹 테이프 밀착 연출: 사용자가 라벨·텍스트를 먼저 골라 보류. 별도 승인으로 진행해. 손 asset `tape_press_hand.png`는 저장소 폴더에만 두고 아직 commit하지 않았어(미사용 resource). 설계 메모 — 테이프 rotation은 degree·y-down·시계방향이라 긴 축 방향 = `(cos θrad, sin θrad)`, 긴 축 길이 = `MASKING_TAPE_BASE_WIDTH(132dp) × scale × lengthScale`, 손 asset `tape_press_hand.png`.
5. `PhotoStickerEdgeStyleInstrumentedTest` 7건은 검증 전용 emulator에서만 실행해.

## 실기기 QA 체크리스트 (사용자 QA 완료 — 기록용)

1. 사진 스티커 → 추가 → 갤러리/카메라/파일 각각: 조준 미리보기가 엽서 가운데에 뜨고, 탭·드래그·두 손가락 확대·회전이 되는지
2. 조준 중 `배경제거` → `처리중...` 뒤 미리보기가 누끼로 바뀌는지, `원본복원`이 바로 되는지, 누끼로 붙인 스티커가 편집 줄에서 `원본복원`으로 보이는지
3. 붙이기: 핀셋 끝이 스티커 아래 변(누끼면 그림의 가장 아래 끝)을 문 것처럼 보이는지, 손 크기·속도가 과하지 않은지, 도착 순간 순간이동·어긋남이 없는지, "톡" 진동
4. 붙인 뒤 스티커가 조준한 자리·크기·각도 그대로인지, 바로 선택돼 편집(모양·누끼·복제)이 되는지
5. undo 한 번에 방금 스티커만 사라지고 redo로 돌아오는지
6. 붙이기 연타 → 1개만. 손이 움직이는 동안 다른 조작·뒤로가기 무반응
7. 조준 중 취소·뒤로가기·탭 이동 → 스티커 생성 안 됨
8. 도장 찍기·흔들어서 한 장 기존 동작 회귀 없음

---

# 이전 기록 — 86일차 workflow 마감 구조 정비

확인일: 2026-09-27. 수동 표준 모드에서도 저장소 상태가 바뀐 개발 작업은 최종 완료보고 전에 `docs/ai/HANDOFF.md`를 최신화하도록 workflow를 보강했어. 이번 작업은 운영 문서와 canonical workflow plugin만 다뤘고 앱 production·test·Room·Gradle·CI YAML은 수정하지 않았어. **이 문서의 다음 후보는 실행 승인이 아니야.**

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`
- 시작 HEAD: `1941308` "Recover 82-84 day handoff records from git and CI"
- 시작 origin 관계: ahead·behind `0/0`
- 시작 작업트리: tracked 변경 없음. 보호 untracked `.codex-config.candidate.toml`, `.kotlin/` 보존
- 현재 repository 변경: `AGENTS.md`, `CLAUDE.md`, `docs/ai/HANDOFF.md` 문서-only
- canonical workflow source: `C:/Users/estel/plugins/post-card-memory-workflow`
- 설치 캐시: `C:/Users/estel/.codex/plugins/cache/personal/post-card-memory-workflow/0.1.0+codex.20260927080957`
- source/cache: 전체 18개 파일 SHA-256 일치, 차이 0건
- plugin 내부 Markdown 링크: 14개 파일, 깨진 링크 0건
- plugin 재설치 명령: 성공 메시지와 새 캐시 생성을 확인했어. 직후 첫 조회에서는 일시적으로 `installed: false`, `enabled: false`가 보였지만 최종 재조회에서 새 버전 `installed: true`, `enabled: true`를 확인했어. 현재 세션은 이전 설치본을 이미 로드한 상태라 새 세션 실제 로딩만 아직 미확인이야.

## 이번에 고친 마감 구조

- `완료보고`: 작업자가 사용자에게 보내는 이번 실행 결과
- `다음 작업용 인수인계서`: ChatGPT·사용자가 다음 작업을 설계하거나 새 대화에 전달하는 별도 문서
- `repository HANDOFF`: 저장소의 `docs/ai/HANDOFF.md`. 실제 Git 상태·미검증·위험·재개 조건을 남기는 공용 교대 장부

수동 모드의 `CURRENT_TASK.md`·`WORK_CONTEXT.md`·`STATUS.md` 면제는 유지하지만 repository HANDOFF 마감까지 면제하지 않도록 분리했어. 기본 마감 순서는 아래와 같아.

```text
구현
→ 자동 검증
→ 필요한 사용자 QA
→ 테스트 변화가 있을 때 TEST-COVERAGE-MAP 갱신
→ repository HANDOFF 갱신
→ 전체 diff 확인
→ 명시적으로 승인된 commit / push
→ push했다면 CI 확인
→ 최종 완료보고
```

- 다음 작업 제안, 자동 후속 제안, `/clear` 권장은 repository HANDOFF 최신화 뒤에만 가능해.
- 날짜형 기록에 공백이 보이면 Git·CI·기존 기록으로 확인되는 사실만 복구하고, 나머지는 `미확인` 또는 `복구 필요`로 남겨.
- TEST-COVERAGE-MAP은 테스트 수·파일·의미·보호 범위가 실제로 바뀐 작업에서만 같은 작업일에 갱신해. 이번 작업은 production/test 변화가 없어 변경하지 않았어.
- commit·push는 기존 명시 승인 규칙을 그대로 따라. 이번 86일차 지시서에는 문서 commit·push·CI 확인이 명시적으로 포함돼 있어 현재 마감 단계에서 수행해.

## 검증과 남은 상태

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| workflow 정합성 | 확인 — 과거의 수동 모드 HANDOFF 면제 충돌 0건 | 저장소·canonical source 전체 검색 |
| source/cache | 확인 — 18/18 SHA-256 일치 | 새 설치 캐시와 canonical 비교 |
| plugin 설치 장부 | 확인 — installed / enabled 모두 true | `codex plugin list --available --json` 최종 재조회 |
| 내부 링크 | 확인 — 깨진 링크 0건 | Markdown 상대 링크 검사 |
| 공식 skill/plugin validator | 실행 불가 | 번들 Python에 `PyYAML`이 없어 import 단계에서 중단. 별도 dependency 설치는 범위 밖 |
| 앱 자동검증 | 불필요 | 운영 문서-only, 앱·테스트·Gradle 변경 없음 |
| 실기기 QA | 불필요 | 앱 동작 변경 없음 |
| TEST-COVERAGE-MAP | 변경 없음 | test 수·파일·의미·보호 범위 변화 없음 |
| 신규 instrumentation 7건 | 미실행 유지 | 85일차부터 compile만 확인. 이번 workflow 작업 범위 밖 |
| workflow commit / push / CI | 완료 — `257331e` push, CI 성공 | run `36305445591`: JVM unit tests·debug APK·instrumentation test compile 모두 success |

## 남은 항목과 재개 조건

1. 새 Codex 세션에서 `0.1.0+codex.20260927080957` 실제 로딩을 확인해. 현재 세션은 시작 시 이전 설치본을 로드했으므로 새 버전 로딩 근거가 될 수 없어.
2. `PhotoStickerEdgeStyleInstrumentedTest` 신규 7건은 검증 전용 emulator에서만 후속 실행해. 실사용 물리 기기에는 실행하지 않아.
3. 82~84일차 미확인 기록 3건은 아래 이전 기록의 상태를 유지해. 근거 없이 채우지 않아.

---

# 이전 기록 — 85일차 사진 스티커 오림 스타일 완료

확인일: 2026-09-26. 수동 표준 모드(공용 작업판 비활성). 85일차 작업지시서로 승인된 사진 스티커 오림 스타일 5종(기본·폴라로이드·가위 오림·찢은 종이·잡지 오림)은 구현·사용자 실기기 QA·commit·push·CI까지 끝났어. **이 문서의 다음 후보는 실행 승인이 아니야.**

이전 HANDOFF 원문(81일차 1~3단계 + 80일차 상세)은 [archive/HANDOFF-through-2026-09-22.md](archive/HANDOFF-through-2026-09-22.md)에 그대로 보존했어(git blob hash 일치 확인).

## 현재 상태 빠른 확인

- 브랜치: `feature/photo-sticker`
- 85일차 기능 commit: `c632887` "Add paper edge styles for photo stickers" (시작 HEAD `fdcabce`)
- origin: push 완료, 작업 종료 시 ahead·behind `0/0`
- 작업트리: tracked 변경 없음. 보호 untracked `.codex-config.candidate.toml`, `.kotlin/`만 존재하고 보존했어.
- 이 HANDOFF 갱신과 archive 추가는 별도 문서 commit으로 올라가. 실제 최신 HEAD는 `git log`로 확인해.

| 구분 | 현재 상태 | 근거 |
|---|---|---|
| 구현 | 완료 — 5개 스타일 | `c632887` |
| 로컬 자동검증 | 실행 — JVM 813/813 통과, `assembleDebug`·`assembleDebugAndroidTest` 통과 | 로컬 결과 XML 86개 합산 |
| emulator instrumentation | **미실행** — 신규 7건은 컴파일만 확인 | 작업 중 `adb devices -l`에 실사용 기기만 연결, 검증 전용 emulator 없음. 실기기 자동 계측 금지 규칙 |
| GitHub Actions CI | 실행 — 성공 | run `36229495520` (HEAD `c632887`): JVM unit tests·Assemble debug APK·Compile instrumentation tests 모두 success |
| 실기기 감각 QA | 완료 | 사용자가 스타일별 확인. 찢은 종이·잡지 오림은 피드백 반영 후 재확인 |
| commit | 완료 | `c632887`, 12개 파일 명시 stage |
| push | 완료 | `fdcabce..c632887` |

## 앱에 달라진 점

- 사진 스티커를 선택하면 오림 선택 줄 **기본 · 폴라로이드 · 가위 오림 · 찢은 종이 · 잡지 오림**이 생겨. 각 타일은 그 스티커 사진을 실제 렌더러로 작게 그려 보여줘.
- **기존 엽서·스티커는 모두 "기본"으로 읽혀서 모습이 그대로야.** 기본은 새 디자인이 아니라 기존 렌더링 경로 그 자체야.
- 누끼 상태에서는 선택 줄이 비활성화돼. 저장된 스타일은 지우지 않고 렌더링에서만 쉬게 해서, 원본복원하면 원래 스타일이 돌아와.
- 사진 필터(세피아·채도·밝기 등)는 만들지 않았어.

## 사용자 QA로 확정된 형태

- **폴라로이드:** 위·좌·우 4.5%, 아래 13%의 흰 여백. 옅은 가장자리 선만 있고 그림자·자동 문구는 없어.
- **가위 오림:** 흰 종이 여백에, 네 변을 각각 1~2번 꺾인 곧은 가위질로 자른 외곽. 지그재그는 없어.
- **찢은 종이:** **네 변 모두 찢김.** 종이 외곽은 완만하게 일렁이고, 사진 인쇄층은 그보다 안쪽에서 얕게 찢겨 흰 단면 띠가 드러나.
  - 처음 한두 변만 찢던 방식은 사용자가 "윗부분만 찢어진 느낌이라 어색하다"고 해서 폐기했어.
- **잡지 오림:** 사진 둘레가 흰 종이가 아니라 **인쇄색 띠**야. 옅은 인쇄색 바탕 위에 망점을 올리고, 잉크 4색 중 하나를 seed로 골라.
  - 칼 단면에만 얇은 흰 선이 있고, 외곽은 거의 곧아. 사진 위에는 6% 먹 망점만 얹었어.
  - 처음 "흰 여백 + 희미한 망점"은 사용자가 "가위 오림과 차이를 모르겠다"고 해서 폐기했어.

## 지켜야 할 구조 규칙

- **저장:** Room이 아니라 `filesDir/sticker_states/<id>.txt`(편집 중 draft 포함)에 저장돼. `PhotoStickerItem` 탭 구분 줄의 11·12번 자리가 `edgeStyle`, `edgeSeed`야.
  - Room·schema(19)·migration 변경은 0건이야.
  - 필드 없음·알 수 없는 style·깨진 seed는 예외 없이 DEFAULT / seed 0으로 읽혀. `valueOf()`를 쓰면 `runCatching` 전체가 실패해서 스티커가 사라지니까 쓰지 않았어.
- **seed:** 처음 스타일을 적용할 때 한 번만 정해(0 = 미지정).
  - 복제는 `copy()` 흐름이라 style·seed가 같이 복사돼.
  - 이동·회전·재구성·재실행 중에는 다시 만들지 않아.
- **모양 생성:** 고정 알고리즘 `StableEdgeRandom`(SplitMix64)을 써.
  - ⚠ **생성 코드를 바꾸면 이미 저장된 스티커 모양이 바뀌어.** 가위 오림·찢은 종이 출력은 golden 테스트로 고정돼 있어.
- **preview / export:** canonical source는 `PhotoStickerPaperSpec`이야.
  - Compose preview(`PhotoStickerPaperModifier`)와 Android Canvas export(`PostcardImageExporter.drawPaperStyledSticker`)가 각자 이 spec으로 도형을 만들어.
  - 망점은 공통 `PhotoStickerHalftoneRenderer` 하나를 둘 다 써.
  - 대칭 처리도 같아: 종이는 대칭을 되돌리고, 사진은 거울상 창 안에 그려.
- **undo/redo:** 기존 `recordStickerSnapshotForUndo()` 경로를 그대로 써. 전용 history는 없어.

## 변경 파일 (`c632887`)

- **신규 production:**
  - `ui/detail/PhotoStickerEdgeStyle.kt`
  - `ui/detail/PhotoStickerPaperModifier.kt`
  - `ui/detail/PhotoStickerEdgeStyleRow.kt`
  - `utils/PhotoStickerHalftoneRenderer.kt`
- **수정 production:**
  - `PhotoStickerItem.kt`
  - `DetailViewModel.kt`
  - `DetailScreen.kt`
  - `PhotoStickerDetailScreen.kt`
  - `PostcardImageExporter.kt`
- **테스트:**
  - `PhotoStickerEdgeStyleTest.kt` (JVM 29건)
  - `PhotoStickerEdgeStyleInstrumentedTest.kt` (7건, 미실행)
- **문서:** `docs/ai/TEST-COVERAGE-MAP.md` (85일차 숫자·보호 범위 갱신)
- **변경 없음:** asset, dependency, Gradle, workflow, CI YAML

## 검증 세부

- **JVM:** 784 → 813 (+29). 테스트 파일 85 + helper 1, 결과 XML 86.
- **instrumentation:** 13 → 20개 / 7파일. 기존 13건은 80~81일차 통과 기록 그대로고, **신규 7건은 미실행**이야.
  - 신규 7건이 다루는 것: `PhotoStickerItem` 직렬화 왕복, 옛 8·11필드 형식 fallback, 알 수 없는 style, 복제 seed 유지, 누끼 중 스타일 보존.
  - Uri 때문에 JVM에서는 만들 수 없어서 instrumentation으로 작성했어.
- **중간 실패 1건:** `StickerItemFlatBoxRemovalStructureTest`(타일 호출 수 2개 고정)와 충돌했어.
  - 테스트 조건은 바꾸지 않고, 선택 줄을 별도 파일로 분리해서 해결했어.
- **리팩터링 검증:** 잡지 오림 작업 중 가위 오림 생성 코드를 파라미터화했어. 그 전에 golden 값을 뽑아 두고, 리팩터링 후 출력이 동일한 걸 확인했어.
- **의도적 조건 변경:** 잡지 오림 재설계로 여백 망점 강도 상한 테스트를 0.3 → 0.6으로 바꿨어. 사진 위 망점 상한 0.08은 유지했어.

## 알려진 위험·미검증

1. **신규 instrumentation 7건 미실행.**
   - 재개 조건: 검증 전용 emulator만 연결된 상태(`adb devices -l`로 확인)에서 실행.
2. **기존 DEFAULT 스티커의 preview/export 차이**는 기록만 하고 범위 밖으로 뒀어. 고치면 기존 엽서 모습이 바뀌어서 손대지 않았어.
   - export에만 상시 검은 테두리가 있어.
   - preview는 clip이 회전보다 바깥이라 회전 시 자르는 틀이 안 돌 가능성이 있어.
3. **preview와 export의 그림 일치**는 공통 spec까지만 자동 검사해. 실제 그림 일치는 사용자 QA로만 확인했어.
4. **스타일 변경 undo/redo**는 전용 자동 테스트가 없어. 기존 경로 재사용과 사용자 QA로 확인했어.

## 기록 공백 (발견사항)

- 이 파일은 81일차 3단계(`000e7ce`) 이후 82~84일차 동안 갱신되지 않았어.
- 사용자 지시에 따라 그 기간 내용은 추정으로 역작성하지 않았어.
- 확인 가능한 사실은 git의 commit 목록뿐이야(`000e7ce..fdcabce`):
  - `3b7dfb3` Update test coverage map after emulator validation
  - `d833c2a` Make postcard stamps feel ink-pressed
  - `0a19187` Add newspaper-hand stamping interaction for seals
  - `af59a93` Update test coverage map after seal interaction tests
  - `a659f89` Keep test coverage map current with test changes
  - `d2a875f` Add shake-to-draw random postcard overlay
  - `fdcabce` Animate subtle steam above gallery clock
- 각 날짜의 QA·위험 상세가 필요하면 해당 일자의 완료보고·인수인계서를 근거로 따로 정리해야 해.
- 83~84일차 테스트 수 변화는 `docs/ai/TEST-COVERAGE-MAP.md`의 일자별 "추가 확인" 문단에 기록돼 있어.

## 범위 밖으로 남긴 항목 (85일차 지시서 기준, 수정 안 함)

- 랜덤 엽서 overlay의 왼쪽 끝 스와이프
- 삭제 중 랜덤 후보 포함 가능성
- 연못 모드 흔들기 판정
- 시스템 애니메이션 줄이기
- 도장·달력·흔들어서 한 장·커피 김 추가 수정
- 갤러리 구조 변경

## 다음 후보 (승인된 작업 아님)

1. 검증 전용 emulator에서 `PhotoStickerEdgeStyleInstrumentedTest` 7건 실행.
2. 82~84일차 기록 공백을 실제 완료보고 근거로 정리할지 결정.
3. 기존 DEFAULT 스티커의 preview/export 차이를 정리할지 제품 판단 (기존 엽서 모습이 바뀌므로 신중).

## 재개 시 확인

- `git status`, 최신 HEAD, origin 0/0
- 보호 untracked 2개가 그대로인지
- instrumentation을 실행한다면 먼저 `adb devices -l`로 실사용 기기가 없는지 확인

---

## 82~84일차 기록 복구 (2026-09-26 복구, 이력)

위 "기록 공백" 절의 82~84일차를 실제 근거로만 복구한 이력이야. 현재 상태나 실행 승인이 아니야. 근거 표기:
- **[git]** commit 메시지·변경 파일
- **[CI]** GitHub Actions run 조회 결과
- **[지도@커밋]** 그 커밋 시점의 `docs/ai/TEST-COVERAGE-MAP.md`
- **[세션 메모]** 당시 Claude Code 세션이 남긴 로컬 메모. 저장소 밖 기록이라 보조 근거로만 써

근거를 찾지 못한 항목은 **미확인**으로 남겼어.

### 82일차 — 2026-09-23 · TEST-COVERAGE-MAP 최신화 (docs-only)

- **commit:** `3b7dfb3` "Update test coverage map after emulator validation" [git]
  - 변경 파일: `docs/ai/TEST-COVERAGE-MAP.md` 1개(+37/−34)
  - 커밋 메시지에 "Docs-only, no production/test/dependency/CI changes"라고 명시돼 있어.
- **반영 내용** [git·지도@3b7dfb3]:
  - 80~81일차 emulator 결과를 지도에 반영했어. instrumentation 총수 10→13개/6파일(`PostcardDeletionOrchestrationTest` 3건 추가).
  - Espresso/API 37 수정 후 `PostcardBackRenderingTest` 3/3, 전체 13/13 통과.
  - emulator가 `mWakefulness=Asleep`이면 draw pass가 없어 저장 timeout이 날 수 있다는 환경 메모를 넣었어. 분류는 `emulator / OS environment`야.
  - 로컬 emulator 실제 실행과 CI의 Android test APK 컴파일(`assembleDebugAndroidTest`)을 구분해 기록했어.
- **JVM 750** [지도@3b7dfb3]: 지도에는 "81일차에 실제로 재실행해 750/750 통과를 확인했고, 82일차에는 재검증 없이 그 결과를 그대로 썼어"라고 적혀 있어. 82일차에 로컬 JVM을 다시 실행했다는 기록은 없어.
- **CI:** run `35805774124` success (head `3b7dfb3`) [CI]
  - JVM unit tests, Assemble debug APK, Compile instrumentation tests 모두 success.
- **당시 남은 항목** [지도@3b7dfb3]:
  - instrumentation은 여전히 CI에서 자동 실행되지 않아(emulator 없음). 실제 실행은 로컬 검증 전용 emulator에서만 확인됐어.
  - 그 밖의 경고·후속 항목은 기록에서 찾지 못했어 → 미확인.

### 83일차 — 2026-09-24 · 도장 잉크 질감 · 도장 찍기 · 지도 · 규칙

**1차 — 도장 잉크 질감** `d833c2a` "Make postcard stamps feel ink-pressed" [git]
- **기능:** 기존 seal id에서 결정론적 잉크 결손 지도를 만들어(FNV-1a seed, SplitMix64 value noise). 누름 성격은 세 가지야(WELL/MEDIUM/LIGHT).
- **렌더러:** 공용 `SealInkWearRenderer`를 화면(`SealPreviewContent`)과 `PostcardImageExporter`가 같이 써.
- **저장:** 새 저장 필드 없음, Room·직렬화 변경 없음.
- **예외:** UI 아이콘(패널 타일·다이얼로그·인트로 소인)은 질감 없이 유지해.
- **테스트:** `SealInkWearTest` 5건 + `PostcardOverlayExportLogicTest` seed 연결 1건 → JVM 750→756 [지도@af59a93].
- **CI:** run `35971142738` success — 세 단계 모두 success [CI].
- **QA** [세션 메모]:
  - 1차 실기기 QA에서 "너무 약함" 피드백 → 강도와 단계별 성격 차이를 반영했어.
  - 2차 QA에서 승인됐어.
  - "점 무리 + radial 얼룩" 접근은 먼지·빛 번짐처럼 보여서 폐기했어.

**2차 — 신문 오림 손 도장 찍기** `0a19187` "Add newspaper-hand stamping interaction for seals" [git]
- **흐름:** 도장 추가 → 조준 단계(같은 id라 잉크 결손이 같은 반투명 미리보기 + 작은 +, 탭·드래그·핀치·회전) → "도장 찍기" → 신문 오림 손이 아래에서 올라와 찍어.
- **생성:** 손이 닿는 프레임에 도장이 한 번 생성되고, undo 1건과 햅틱 1회가 함께 일어나.
- **범위:** 손은 일시 overlay라 저장·복원·export에 들어가지 않아.
- **asset:** `res/drawable-nodpi/seal_stamp_hand.png`(약 1.8MB) 추가.
- **테스트:** `SealStampSessionTest` 10건 → JVM 756→766 [git·지도@af59a93].
- **이 시점 검증** [지도@af59a93·CI]:
  - 로컬 JVM 766/766.
  - CI run `35977829337` success — JVM unit tests, Assemble debug APK, Compile instrumentation tests 모두 success.
- **QA** [세션 메모]: 손목 끝의 직선 잘림은 사용자가 "신문지 오린 것 같아 좋음"으로 승인했어.

**3차 — TEST-COVERAGE-MAP 갱신** `af59a93` "Update test coverage map after seal interaction tests" [git]
- **변경 파일:** 지도 1개(+26/−24). 지도 기준 HEAD는 `0a19187`.
- **수치:** JVM 766, 테스트 파일 82 + helper 1, XML 83. instrumentation 13개/6파일 그대로(83일차 androidTest 변경 없음) [지도@af59a93].
- **보호 범위 반영:**
  - 같은 id의 잉크 결정성
  - preview/export seed 연결
  - 조준→찍기 순수 상태 전이
- **구분해서 적은 것:** Compose pointer 입력, 신문지 손 animation, 진동(haptic), 실제 undo 연결은 실행 검증이 아니라고 따로 적었어.
- **CI:** run `35981166343` success — 세 단계 모두 success [CI].

**4차 — 공통 규칙 반영** `a659f89` "Keep test coverage map current with test changes" [git]
- **변경 파일:** `AGENTS.md` 1개(+9). 8절에 "TEST-COVERAGE-MAP 조건부 마감" 절을 추가했어(갱신 조건, 권장 마감 순서, 숫자 구분, 보호 범위 표기, 완료보고 기록).
- **성격:** docs/rules-only. production·test 변경 없음.
- **CI:** 생성됐어 — run `35984597452` success, 세 단계 모두 success [CI].
- **workflow source/cache 정합성: 미확인.**
  - 당시 SHA-256 일치 결과와 링크 검사 결과는 저장소, commit 메시지, 세션 메모 어디에서도 찾지 못했어.
  - 참고로 2026-09-26에 읽기 전용으로 관찰한 사실은 이것뿐이야: 저장소 밖 workflow source(`post-card-memory-workflow` plugin)의 version marker가 `0.1.0+codex.20260924094513`이고, `test-coverage-map-maintenance.md` 등에 TEST-COVERAGE-MAP 규칙이 들어 있어.
  - 이 관찰은 당시 검증 결과를 대신하지 않아.

**84일차로 넘긴 맥락** — "흔들어서 랜덤 엽서 + 커피잔 김 애니메이션"을 84일차로 넘겼다는 83일차 당시 기록은 저장소·commit·세션 메모에서 찾지 못했어 → **미확인**. 확인된 사실은 다음 날 두 작업이 실제로 수행됐다는 것(84일차 commit)뿐이야.

### 84일차 — 2026-09-25 · 흔들어서 한 장 · 커피잔 김

**1차 — 흔들어서 랜덤 엽서** `d2a875f` "Add shake-to-draw random postcard overlay" [git]
- **흔들기 감지:** 순수 `GalleryShakeDetector`(중력 제거, 창 안의 방향 전환 2회, cooldown). 센서 리스너는 갤러리 back stack entry lifecycle을 따라서, 상세 화면과 백그라운드에서는 멈춰.
- **흔들기가 꺼지는 경우:** play mode, 선택 모드, 삭제 확인, 방문 달력, overlay가 열려 있을 때.
- **랜덤 선택:** 균등 랜덤이고, 후보는 현재 검색 결과(`filterPostcardsForSearch(postcards, searchQuery)`) 범위 안이야 [git diff].
- **미래편지 제외:** 새 코드가 아니야. 갤러리 목록 자체가 `PostcardDao`의 `futureMailState = 'NONE'` 쿼리에서 오기 때문에 발송된 미래편지는 원래 후보에 없어 [현재 코드 대조].
- **overlay:** 흐려진 갤러리 위에 엽서를 띄우고, 신문 오림 손이 엽서 왼쪽 아래 모서리를 집어 올려.
  - 날짜 문구는 월·일 0 채움 없음, 자정 경계는 zone을 따라가(`storyLabel_*` 테스트).
  - 엽서를 탭하면 기존 상세 화면이 열리고, 바깥 탭이나 뒤로가기로 닫혀.
- **asset:** `res/drawable-nodpi/gallery_pick_hand.png`(약 1.4MB) 추가.
- **테스트:** `GalleryShakeDetectorTest` 11건 + `GalleryRandomPostcardOverlayTest` 7건 → JVM 766→784. 같은 commit에서 지도도 갱신했어 [git·지도@fdcabce].
- **QA** [세션 메모]:
  - 흔들면 상세로 바로 이동하던 방식은 "갑작스럽다"는 QA로 overlay 방식이 됐어.
  - 손 asset은 사용자가 직접 준비했어(지시서상 준비 전 STOP).
  - 손 위치는 사용자 요청으로 가운데가 아니라 왼쪽 아래 모서리를 집게 했어.
  - 연못 모드의 옛 흔들기 판정과 겹치지 않게, 새 판정은 playMode NONE에서만 켜.

**2차 — 커피잔 김 애니메이션** `fdcabce` "Animate subtle steam above gallery clock" [git]
- **동작:** 컵은 그대로 두고 김 두 줄만 좌우로 약 1dp 흔들려. 주기는 2.8초와 3.4초라 두 줄이 같이 움직이지 않아.
- **유지된 것:** 김의 모양·색·alpha는 기존 정적 아이콘과 같아.
- **변경 파일:** `GalleryRetroClock.kt` 1개. 테스트 변경은 없어(시각 전용) [git·지도@fdcabce].

**검증** [CI·지도@fdcabce]:
- `d2a875f`와 `fdcabce`는 커밋 시각이 같고(2026-09-25 21:16:13 +0900) 함께 push됐어. 그래서 CI run은 head `fdcabce` 하나야 — run `36133974480` success, 세 단계 모두 success. `d2a875f` 단독 CI run은 없어.
- 로컬 JVM 784/784.
- instrumentation은 13개/6파일 그대로(84일차 androidTest 변경 없음).
