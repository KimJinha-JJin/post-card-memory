# DetailScreen·DetailViewModel 구조 감사 보고서

- 대작업: DetailScreen·DetailViewModel 구조 감사 및 단계적 안정화
- 차수: 제1차 — 기준 상태 확인 및 구조 감사
- 실제 작업일: 2026-08-06
- 조사 방식: 코드 정독 + Grep/측정. 프로덕션 Kotlin 코드는 변경하지 않음.

---

## 1. 최종 판정

**제1차 완료**

제1차 완료 조건(작업지시서 §13) 17개 항목을 모두 충족했다. 조사 중 치명적 결함(데이터 손실, 삭제 가능성, Migration 실패, 크래시 등)은 발견되지 않았으며, 발견한 결함은 모두 §12에 기록하고 수정하지 않았다.

---

## 2. Git 기준 상태

| 항목 | 값 |
|---|---|
| 브랜치 | `feature/photo-sticker` |
| 시작 HEAD | `7fe0047` |
| 종료 HEAD | `7fe0047` (조사 중 프로덕션 커밋 없음) |
| origin/feature/photo-sticker | `7fe0047` (fetch 후 일치) |
| 작업트리 상태 | `?? .kotlin/` 만 존재 (빌드 산출물, gitignore 대상 아님이나 무시 대상 — 수정하지 않음) |
| 봉투 철회 커밋 | `7fe0047 Discard envelope feature, restore postmark seal to postcard face` — 커밋됨, origin에 push됨 (HEAD == origin HEAD) |
| 예상하지 못한 변경 | 없음 |

### 봉투 철회 상태 확인 (코드 기준)

| 확인 항목 | 결과 |
|---|---|
| 봉투 관련 UI(진입 메뉴/선택 시트/렌더링/애니메이션/전용 소인 흐름) | `DetailScreen.kt` 전체에서 `envelope`/`Envelope`/`봉투` 문자열 0건 — UI에서 완전히 제거됨 |
| 기존 도장 복구 | `PostcardSealItem.kt`의 `SealType` enum에 8종 존재: `CIRCLE_POSTMARK`, `WAVE_CANCEL`, `AIR_MAIL`, `STAR`, `DOG_PAW`, `PIGEON_TRACK`, `HEART`, `STAR_STAMP`. **작업지시서 원문(§2)은 "도장 6종 복구"라 명시하나 실제 코드는 8종(주요 4종 + 미니스탬프 4종)이다 — 문서와 코드가 다르므로 임의로 맞추지 않고 사실만 보고한다.** |
| `CIRCLE_POSTMARK` 복구 | 확인됨 (`PostcardSealItem.kt:11`) |
| Room version 17 / `MIGRATION_16_17` | 유지됨 (`PostcardDatabase.kt:10` `version = 17`, `PostcardDatabase.kt:403` `MIGRATION_16_17`) |
| `envelopeStyle`/`envelopePostmarked` UI 미참조 | 확인됨. 두 필드는 `data/Postcard.kt:60,62`(Entity), `data/PostcardRepository.kt:304,308`, `data/PostcardDao.kt:391,397,403,416,417`(Migration 내 컬럼 추가 SQL 포함)에만 존재하고 `ui/` 패키지 전체에서 0건 참조 — 휴면 필드로 정상 격리됨 |

결론: 봉투 철회는 코드 기준으로 완료 및 push된 상태다. 도장 종수 표현 차이만 문서-코드 불일치로 남아 있어 §12에 별도 기록한다.

---

## 3. 실제 파일 규모

| 항목 | DetailScreen.kt | DetailViewModel.kt |
|---|---|---|
| 줄 수 | 6,357 | 4,812 |
| 파일 크기 | 289,451 bytes (~283 KB) | 171,713 bytes (~168 KB) |
| import 수 | 198 | 52 |
| 함수/컴포저블 수 | 총 23개 top-level 선언 (`@Composable` 9개 + 순수 함수 14개). **`DetailScreen` 컴포저블 1개가 1316~6194줄, 약 4,880줄(파일의 77%)을 단독으로 차지** | 약 137개 함수 (Grep 정규식 기준 정밀 측정) |
| 상태 변수 수 | 로컬 `remember`/`rememberSaveable` 약 35개(§4-2 표), `collectAsState()`로 구독하는 ViewModel StateFlow 약 26개는 별도 | `StateFlow<...>` 선언 26개, `MutableStateFlow(` 생성 13개(내부 backing + 로컬 변수 포함) |
| callback/이벤트 처리 함수 수 | 별도 집계 불가(콜백 대부분이 인라인 람다) — 대신 ViewModel 직접 호출 지점으로 대체 계측 | 해당 없음(호출되는 쪽) |
| 직접 참조하는 ViewModel 함수 수 | `viewModel.` 호출 198회, 고유 함수 111종 | 해당 없음 |
| 직접 참조하는 외부 컴포넌트 수 | `Postcard*Picker`류 6개(Layout/Template/BackgroundColor/CustomColor/BackgroundPattern), `PhotoStickerPickerPanel`, `SealPickerPanel`, `DoodlePanel` 등 최소 9개 이상(정밀 전수조사는 이번 범위 밖) | Repository 1개(`PostcardRepository`) + utils 8개(`PostcardDraftStorage`, `ConfirmedEditStateStorage`, `PostcardImageStorage`, `PhotoStickerImageStorage`, `PostcardTemplateStorage`, `PostcardImageExporter`, `PostcardDeletionManager`, `PhotoColorExtractor`) |
| Mutex 수 | 0 | 2 (`styleWriteMutex`, `draftSaveMutex`) |
| 관련 테스트 파일 수 | 없음(직접 인스턴스화 테스트 0개, §8 참고) | 없음(직접 인스턴스화 테스트 0개) — 단 순수 로직/Fake 기반 테스트 27개 파일이 두 파일의 로직을 간접 커버 |
| 합계 | **11,169줄**, **461,164 bytes (~450 KB)** | |

측정 명령(재현 가능):
```bash
wc -l app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt
wc -l app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt
grep -cE "^\s*(private |internal |protected )?(suspend )?fun " DetailViewModel.kt
grep -c ": StateFlow<" DetailViewModel.kt
grep -oE "viewModel\.[a-zA-Z]+" DetailScreen.kt | sort | uniq -c | sort -rn
```

---

## 4. DetailScreen.kt 책임 지도

### 4-1. 전체 블록 개요 (파일 내 실제 선언 순서)

| 블록 | 줄 범위 | 설명 |
|---|---|---|
| 상태 수집/선언부 | 1316~1702 | `collectAsState`, 로컬 `remember` 상태, ActivityResult 런처, Pager 설정 |
| 진입 `LaunchedEffect`들 | 1703~1990 | 로드, 삭제/발송/저장 상태 반응, 스티커 배경제거 반응 |
| lifecycle/공유 이펙트 | 1991~2018 | `ON_STOP` 시 draft flush, 공유 에러 토스트 |
| 메인 엽서 캔버스 Box | 2019~3623 | 사진/스티커/도장/낙서 렌더링과 제스처 전부 (약 1,600줄) |
| 탭 콘텐츠 (`HorizontalPager` 6페이지) | 3625~4886 | 사진·배경·텍스트·스티커·도장·낙서 탭 |
| 상단 헤더 바(뒤로/완료/더보기) + 더보기 메뉴 | 4888~5150 | z-order상 캔버스보다 나중에 그려져 위에 뜸(코드 위치는 파일 뒤쪽) |
| 집중 미리보기 종료 버튼, 사진 소스 메뉴 | 5153~5206 | |
| AlertDialog 묶음 (글귀/템플릿/삭제/미래엽서/저장 에러 7종) | 5208~6151 | 총 15개 Dialog |
| 하단 `EditorBottomTabBar` + SnackbarHost | 6153~6185 | |
| `SharePreviewBottomSheet` 트리거 | 6187~6193 | 정의 자체는 함수 밖(6198~6357) |

### 4-2. 책임별 위치·상태·결합도

| 책임 | 줄 범위 | 관련 state | ViewModel 직접 호출 | 저장/파일 직접 처리 | 결합도 |
|---|---|---|---|---|---|
| 화면 진입/초기 상태 | 1321~1391, 1703~1707 | 15개 StateFlow 구독 | `loadPostcard`, `loadStickerSealStateAndAutoRestoreDraft`, `resetShareState` | 없음 | 높음(전체 화면 전제조건) |
| 상단 헤더 바(대체 TopAppBar) | 4890~4938 | `moreMenuExpanded` | `saveEditsAndClearDraft` | 없음 | 낮음 |
| 더보기 메뉴 | 4940~5145 | `moreMenuExpanded` | 공유/내보내기/미리보기/미래엽서/삭제 콜백 | `createStickerOverlaysForExport`/`createSealOverlaysForExport` 직접 호출 | 중간(export 오버레이 계산 포함) |
| 완료 저장 | 4925~4938 | 없음 | `saveEditsAndClearDraft(postcardId)` | 없음 | 낮음 |
| 뒤로가기 | 1721~1727 | `isFocusPreviewMode` | `navigateBackAfterPendingStyleSaves`(내부적으로 `awaitPendingStyleSaves()`) | 없음 | 높음(저장 보장 경로) |
| 삭제 | 5701~5743(다이얼로그), 1743~1760(결과 반응) | `showDeleteDialog` | `deletePostcard()` | 없음(ViewModel 위임) | 중간 |
| 메인 캔버스 렌더링 | 2019~3623 | `postcardPreviewSize`, `stickerSizes`, `sealSizes` 등 다수 | 제스처 확정 시점마다 `set*`/`record*` 호출 | 없음(파일은 ViewModel 경유) | **매우 높음** |
| 사진 탭 | 3764~4056 | `templatesExpanded` 등 | `updateLayoutStyle`, `applyTemplate`, `undoTemplateStyleChange` | 없음 | 중간 |
| 배경 탭 | 4057~4304 | `customColorDrawerExpanded` | `updateBackgroundColor`, `updateBackgroundPattern`, `extractBackgroundColorsFromPhoto` | 없음 | 낮음~중간 |
| 텍스트 탭 | 4305~4479 | `textScaleTarget` | `setMessageTextScalePreview`/`saveMessageTextScale` 등 | 없음 | 낮음 |
| 스티커 탭 | 4480~4563(패널) + 2276~3025(캔버스 내) | `stickerEditMode`, `stickerEditModeOwnerId`(로컬), `selectedStickerId`(VM) | `setPhotoStickers`, `setSelectedStickerId`, `recordStickerSnapshotForUndo`, `removeStickerBackground`, `duplicateSticker`, `moveStickerForward/Backward` | 없음 | **높음**(파일 삭제 결과와 연결, 가장 코드량 많음) |
| 도장 탭 | 4564~4680(패널) + 3027~3301(캔버스 내) | `selectedSealId`(VM), `sealSizes` | `setPhotoSeals`, `setSelectedSealId`, `recordSealSnapshotForUndo` | 없음 | 중간 |
| 낙서 탭 | 4682~4713(패널) + 3303~3620(캔버스 내) | `doodleTool`, `doodleColorArgb`, `doodleWidth`, `currentDoodleStrokePoints`, `isDrawingDoodle`(전부 로컬) | `setDoodleStrokes`, `recordDoodleSnapshotForUndo`, `undoDoodleChange`, `redoDoodleChange` | 없음 | 중간~높음(제스처 회귀 이력 있음, 3303~3309 주석) |
| 선택 객체 상태 | `selectedStickerId`/`selectedSealId`는 VM 소유, `stickerEditMode`류만 로컬 | — | — | — | 중간(로컬·VM 혼재) |
| 제스처 처리 전반 | 2070~2230(사진), 2373~2596·2610~3023(스티커), 3163~3258(도장), 3320~3596(낙서) | 각 섹션 로컬 클로저 변수 다수 | 확정 시점에 `set*`/`record*` | 없음 | **매우 높음** |
| Undo/Redo 버튼 | 3782~3789, 3854~3857, 4550~4557, 4668~4675, 4701~4708 | `canUndo*`/`canRedo*`(전부 VM) | 각 `undo*Change()`/`redo*Change()` | 없음 | 낮음(버튼 자체는) |
| Dialog 15종 | 5208~6151 | 각 `show*` bool 또는 파생 상태 | 각 `reset*State()` | 없음 | 낮음(개별), 패턴 반복 |
| BottomSheet | 6187~6357 | `shareState` | `resetShareState()` | 없음 | 낮음 |
| 색상 선택 UI | 4067~4274 | — | `updateBackgroundColor` | 없음 | 낮음 |
| 도구 선택 패널 | `StickerEditModeToolbar`(3661), `DoodlePanel`(4687) | `stickerEditMode`, `doodleTool/Color/Width` | — | 없음 | 낮음 |
| 미리보기 | `isFocusPreviewMode`(1357), 2048~2054, 5153~5178 | `isFocusPreviewMode` | — | 없음 | 낮음 |
| 공유 | 4960~5023, 2007~2018 | `shareState` | `sharePostcard` | 오버레이 계산 직접 수행 | 중간 |
| 파일 내보내기 | 5024~5087, 5878~5949 | `exportState` | `exportPostcardToGallery`/`resetExportState` | 오버레이 계산 직접 수행 | 중간 |
| 미래엽서 UI | 5107~5123, 5745~5876, 1734~1741(SENT 조기 return) | `showFutureMailDatePicker` 등 | `sendToFuture` | 없음 | 중간 |
| 오류/안내 메시지 | Toast 다수, `detailSnackbarHostState` | — | `textScaleSaveErrors`/`draftAutoRestoredEvents` Flow 수집 | 없음 | 낮음 |
| 애니메이션 | 4116~4141, 4855~4877 | `showSavedBriefly` | — | 없음 | 낮음(단순 `AnimatedVisibility`만, `Animatable` 직접 사용 없음) |

### 4-3. 로컬 `remember` 상태 중 ViewModel과 개념적 중복 의심 항목

| 변수 | 줄 | 의심 사유 |
|---|---|---|
| `messageDraft` | 1365 | `postcard.message`와 별도 편집 버퍼. 다이얼로그 열릴 때마다 재초기화하는 지점이 여러 곳이면 동기화 버그 위험 |
| `stickerEditMode`/`stickerEditModeOwnerId` | 1413, 1416 | `selectedStickerId`는 VM 소유인데 "편집 모드(이동/크기/회전)"만 로컬 — 스티커 변경 시 수동 리셋 코드(1419~1427) 필요 |
| `backgroundRemovalError` | 1459 | VM의 `stickerBackgroundRemovalState` Error 케이스를 로컬 String으로 복사 보관 — 에러 정보가 두 곳에 흩어짐(의도적으로 보이나 주의 필요) |

나머지 약 30개 로컬 상태는 순수 UI 상호작용 보조 상태(다이얼로그 표시 여부, 드래그 임시 버퍼, 레이아웃 픽셀 캐시 등)로 VM과 중복 없음.

---

## 5. DetailViewModel.kt 책임 지도

### 5-1. 저장 인프라 개요

- **Mutex 2개**: `styleWriteMutex`(491, 슬라이더·배경·폰트·레이아웃·날짜형식·템플릿 저장 17개 함수 공용), `draftSaveMutex`(523, 초안 저장/삭제 전용)
- **재읽기+직렬화 패턴**: 스타일 계열 저장 함수 전부 "Mutex 획득 시점에 `_postcard.value`를 다시 읽어 커밋" — 오래된 조작이 최신 조작을 덮어쓰지 않도록 설계(주석 472~490에 근거 명시)
- **Job 취소 정책 2계열**: 슬라이더/오프셋/줌/템플릿 등 12개는 새 저장 전 이전 Job을 `cancel()`. 배경색/배경패턴/폰트/레이아웃/날짜형식 5개는 취소하지 않음(재읽기+직렬화만으로 충분하다는 설계, 주석 484~489)
- **화면 종료 저장 보장**: `awaitPendingStyleSaves()`(3897~3934)가 이 파일 안에 존재. 25개 안팎의 저장 Job + `awaitStickerCleanupSweep()`을 `withTimeoutOrNull(2000L)`로 대기

### 5-2. 책임 그룹별 요약

| 그룹 | 대표 함수(줄) | 상태 | 파일/Repo 접근 | 동기화 | 실패 복구 |
|---|---|---|---|---|---|
| 엽서 로드 | `loadPostcard`(2320) | `_postcard`, undo 스택 초기화 | `repository.getPostcardById` | 없음 | **없음 — 예외 시 그대로 전파**(§12 기록) |
| 정규화 함수 | `normalizeBackgroundPattern/MessageFont/LayoutStyle/DateFormat`(4637~4692) | 없음(순수) | 없음 | — | 해당 없음. **`loadPostcard`가 아니라 사용자가 해당 필드를 능동적으로 바꿀 때만 호출됨** — 작업지시서가 전제한 "오래된 데이터 로드 시 정규화"와 실제 동작이 다름(§12 기록) |
| 편집 초안(Draft) | `scheduleDraftAutosave`(695), `flushDraftNow`(710), `persistDraftNow`(762), `loadStickerSealStateAndAutoRestoreDraft`(541), `revertToConfirmedState`(664) | `_photoStickers/_photoSeals/_doodleStrokes`, `currentDraftPostcardId`, `draftRevisionCounter` | `PostcardDraftStorage`(외부 utils), `sticker_states`/`seal_states`/`doodle_states` 확정 파일 | `draftSaveMutex` | revision 비교로 오래된 저장이 최신을 덮지 않음. 부분 실패 시 기존 초안 보존 |
| 사진 교체·정리 | `updatePostcardImage`(3945), `persistStickerBackground`(2357), `saveStickerForegroundBitmap`(4718) 등 10개 | `_postcard`, `_imageUpdateState` | `PostcardImageStorage`, 캐시/영구 디렉터리 File I/O | 없음(단일 흐름) | 이전 파일은 **새 파일 저장·DB 갱신 성공 확정 후에만** 삭제(3936~3944 주석). 스티커/도장 상태와 결합 없음(독립 트랜잭션) |
| Undo/Redo (스티커/도장/낙서/사진변형/템플릿, 5계열) | `undo*Change`/`redo*Change` (1177~1717 구간) | 각 `Undo/RedoStack`(ArrayDeque) | 없음(순수 인메모리), 실제 저장은 그룹2·5로 위임 | 없음 | 템플릿만 실패 롤백 있음(1699~1757). 스티커는 `stickerCleanupCandidates`로 파일 삭제를 지연 |
| 저장 흐름(자동/확정/개별) | `saveEditsAndClearDraft`(848), `persistStickerEditState`(932), `persistSealEditState`(1049), `persistDoodleEditState`(1097), `awaitPendingStyleSaves`(3897) | `_draftSaveStatus`, `_confirmSaveState` | 그룹2·3 위임 | `draftSaveMutex`(확정 시), `styleWriteMutex`(스타일 저장 시) | `shouldConfirmSaveSucceed`(117, 순수)로 3개 모두 성공해야 초안 삭제 |
| 삭제 | `deletePostcard`(4561), `deleteStickerCacheUri`(4182), `sweepStickerCleanupCandidates`(1332) | `_deleteState` | `PostcardDeletionManager` | 없음 | DB 삭제 성공 시 파일 정리 부분 실패는 **로그만 남기고 성공 처리**(§12 기록) |
| 공유/내보내기/미래엽서 | `exportPostcardToGallery`(4450), `sharePostcard`(4502), `sendToFuture`(4605) | `_exportState`, `_shareState`, `_futureMailSendState` | `PostcardImageExporter`, `repository.sendToFutureMailbox` | 재진입 차단(상태 체크) | 편집 상태는 읽기 전용 참조만, 삭제 안 함(미래엽서는 삭제 로직 재사용 안 함, 4599~4604 주석 명시) |
| 템플릿 | `applyTemplate`(1769), `saveCurrentStyleAsNewTemplate`(1998), `deleteUserTemplate`(2285) 등 | `_userTemplates`, `_templateSaveState` | `PostcardTemplateStorage` | `styleWriteMutex` 경유(persistTemplateStyle) | 원자적 저장(temp+rename)으로 실패해도 기존 파일 보존 |

### 5-3. 상태의 단일 기준점

| 개념 | 기준점 | 비고 |
|---|---|---|
| 편집 중 엽서 스타일 | `_postcard`(308) | 유일한 기준점 — 낙관적 갱신 후 IO 재확인 |
| 스티커/도장/낙서 현재 편집 상태 | `_photoStickers`/`_photoSeals`/`_doodleStrokes` | 이것이 최종 기준점이나, `confirmedStickersBaseline` 등 되돌리기용 스냅샷과 파일시스템상의 확정 파일이 각각 다른 시점을 들고 있어 **세 지점이 명시적으로 동기화되지 않음**(§9 고위험) |
| 저장 진행 상태 | `_draftSaveStatus`(자동저장) vs `_confirmSaveState`(확정저장) | 개념적으로 잘 분리됨. 단 `draftAutosaveJob`은 `awaitPendingStyleSaves`의 대기 목록에 **포함되지 않음**(§12 기록) |

### 5-4. 무상태 순수 함수 후보 (제7차·제9차 참고용, 이번 차수에서는 이동하지 않음)

| 함수 | 위치 |
|---|---|
| `shouldConfirmSaveSucceed` | 117~121 |
| `canStartConfirmSave` | 124~126 |
| `isReadableNonEmptyFile` | 129~130 |
| `shouldClearBackgroundRemoval` | 137~140 |
| `normalizeBackgroundPattern`/`MessageFont`/`LayoutStyle`/`DateFormat` | 4637~4692 |
| `uriToLocalStickerFile` | 4142~4152 |
| `isFileUriReadable` | 4353~4359 |
| `renderTemplatePreviewBitmap` | 2081~2131 |

DetailScreen.kt 쪽 순수 함수(213~770줄, `clampStickerOffset` 등 14개)는 이미 파일 상단에 분리되어 있고 `PostcardOverlayExportLogicTest`/`DoodleLineSnapTest`로 테스트됨 — 제7차의 유력 후보군이나 이미 부분적으로 잘 분리된 상태.

---

## 6. 두 파일의 연결 관계

| 연결 | 방식 | 근거 |
|---|---|---|
| 화면 선택 상태 ↔ VM 편집 상태 | `selectedStickerId`/`selectedSealId`는 VM StateFlow가 단일 기준점. 로컬 `remember`는 "어떤 모드로 편집 중인지" 같은 보조 상태에만 사용 | DetailScreen.kt 1369~1391 |
| Undo/Redo ↔ 스티커·도장·낙서 | 편집 직전 `record*SnapshotForUndo()` 선행 호출, 버튼은 `undo*Change()`/`redo*Change()` 직접 호출 | DetailScreen.kt 2416, 2799, 3187, 3349 등 / DetailViewModel.kt 1177~1717 |
| 미리보기 ↔ 저장 결과 | `set*Preview()`(낙관적 즉시 반영) → `save*()`(debounce/즉시 DAO). 저장 실패 시 StateFlow 롤백으로 미리보기도 함께 되돌아감 | DetailScreen.kt 2188/2192 등 |
| 저장 결과 ↔ 공유·내보내기 | Room에 저장된 값이 아니라 **그 시점 화면 상태**(`photoStickers`, `photoSeals`, 크기 캐시)를 오버레이로 재계산해 전달 — 저장 미완료 상태에서도 공유 가능(`controlsEnabled`가 Saving 중만 차단) | DetailScreen.kt 4992~5019, 5056~5070 |
| 사진 교체 ↔ 임시 파일 | `updatePostcardImage` 성공/실패 확정(`imageUpdateState`) 이후에만 화면이 카메라 임시 파일 정리 — 양방향 계약 | DetailScreen.kt 1787~1810 |
| 자동저장 ↔ 완료 저장 | `ON_STOP` 시 `flushDraftNow()`(자동), 완료 버튼이 `saveEditsAndClearDraft()`(확정). revision 카운터로 오래된 자동저장이 확정 이후 값을 덮지 않음 | DetailScreen.kt 1993~2005, 4926~4930 |
| 저장 중 화면 종료 ↔ 저장 보장 | 아이콘 뒤로가기·시스템 back 모두 `navigateBackAfterPendingStyleSaves` 경유 → `awaitPendingStyleSaves()` 대기 후 이동 | DetailScreen.kt 1709~1727 / DetailViewModel.kt 3897~3934 |
| 탭 전환 ↔ 선택 객체 해제 | **탭이 바뀌어도 `selectedStickerId`/`selectedSealId`는 지워지지 않는다.** 대신 탭별로 제스처의 `pointerInput` 자체를 조건부로 붙여 입력만 차단(예: 낙서 Canvas는 낙서 탭일 때만 pointerInput 연결) | DetailScreen.kt 2367~2373, 3310~3320 — 명시적 초기화 `LaunchedEffect` 없음 확인 |
| 오래된 데이터 ↔ 현재 편집 상태 | `loadPostcard`는 Room 결과를 그대로 대입, 정규화는 필드를 능동 변경할 때만 적용 | §5-2 |
| 오류 발생 ↔ 상태 롤백 | 대부분 조건부 롤백("그 사이 더 최신 값으로 안 바뀌었을 때만 되돌림")으로 일관, `updateMessage`만 예외적으로 롤백 없음 | §12 기록 |
| 취소 ↔ 임시 파일 정리 | Draft 삭제 시 draft 전용 스티커 배경만 지우고 확정본은 보존(`PostcardDraftStorageTest`로 검증됨) | 그룹2 |

**중복 상태**: `_photoStickers`(현재) / `confirmedStickersBaseline`(되돌리기 대상) / 확정 파일(`sticker_states/*.txt`)이 서로 다른 시점의 스냅샷을 독립적으로 보관하며 명시적 동기화 로직이 없음 — §9 고위험 참고.

---

## 7. 기능별 의존 관계

| 기능 | 진입 UI | 화면 상태 | ViewModel 함수 | 편집 상태 변경 | 저장/파일 처리 | 미리보기 | 공유·내보내기 | 테스트 |
|---|---|---|---|---|---|---|---|---|
| 사진 | 사진 소스 메뉴(1504~) | `pendingCameraCapturePath` | `updatePostcardImage` | `_postcard`(imagePath) | `PostcardImageStorage` | 즉시 반영 | 오버레이 재계산 포함 | `PostcardImageStorageTest`(경계만), ViewModel 자체 미검증 |
| 배경 | 배경 탭 | `customColorDrawerExpanded` | `updateBackgroundColor`/`updateBackgroundPattern` | `_postcard` | `styleWriteMutex` 경유 DAO | 즉시 반영 | 포함 | `BackgroundColorSaveRaceTest`(Fake) |
| 텍스트 | 텍스트 탭 | `textScaleTarget` | `updateMessage`, `setMessageTextScalePreview`/`save*` | `_postcard` | `styleWriteMutex` | 즉시 반영 | 포함 | 직접 테스트 없음 |
| 스티커 | 스티커 탭 + 캔버스 제스처 | `stickerEditMode` | `set/save/undo/redoStickerChange`, `removeStickerBackground`, `duplicateSticker` | `_photoStickers` | 캐시/영구 파일 + draft/확정 상태 파일 | 즉시 반영 | 포함 | `PostcardOverlayExportLogicTest`(export 계산만), undo/redo 자체 미검증 |
| 도장 | 도장 탭 + 캔버스 제스처 | `selectedSealId`(VM) | `set/save/undo/redoSealChange` | `_photoSeals` | draft/확정 상태 파일(파일 참조 없음) | 즉시 반영 | 포함 | `PostcardOverlayExportLogicTest` 일부, undo/redo 미검증 |
| 낙서 | 낙서 탭 + 캔버스 Canvas | `doodleTool` 등 | `set/save/undo/redoDoodleChange` | `_doodleStrokes` | draft/확정 상태 파일 | 즉시 반영 | 포함 | `DoodleLineSnapTest`, `DoodleStrokeTest`(순수 로직) |
| 템플릿 | 사진 탭 내 템플릿 섹션 | `templatesExpanded` 등 | `applyTemplate`, `save/rename/overwrite/deleteUserTemplate` | `_postcard` 다수 필드 + 도장 | `PostcardTemplateStorage` | 즉시 반영 | 해당 없음 | `TemplateStyleSaveRollbackTest`, `PostcardTemplateTest`, `BuiltInTemplatesTest` |
| 미리보기 | 집중 미리보기 버튼 | `isFocusPreviewMode` | 없음(읽기 전용) | 없음 | 없음 | 자기 자신 | 해당 없음 | 해당 없음 |
| 공유 | 더보기 메뉴 | `shareState` | `sharePostcard`/`resetShareState` | 없음(읽기) | `PostcardImageExporter` | 캡처 소스 | 자기 자신 | `PostcardImageExporterShareTest`(파일명만) |
| 파일 내보내기 | 더보기 메뉴 | `exportState` | `exportPostcardToGallery`/`resetExportState` | 없음 | `PostcardImageExporter` | 캡처 소스 | 자기 자신 | 위와 동일 파일, 갤러리 저장 자체 미검증 |
| 미래엽서 | 더보기 메뉴 | `showFutureMailDatePicker` 등 | `sendToFuture` | `_futureMailSendState` | `repository.sendToFutureMailbox` | 해당 없음 | 해당 없음 | `FutureMailLogicTest`(순수 로직) |
| 저장(자동/확정/종료) | 전역 | `draftSaveStatus`, `confirmSaveState` | `flushDraftNow`, `saveEditsAndClearDraft`, `awaitPendingStyleSaves` | 스티커/도장/낙서 확정 | draft/확정 파일 | 해당 없음 | 해당 없음 | `DetailScreenExitSaveLossTest`+`GuaranteeTest`(Fake), `ConfirmSaveLogicTest` |
| Undo·Redo | 각 탭 툴바 | `canUndo*`/`canRedo*`(VM) | 5계열 `undo*/redo*Change` | 각 대상 상태 | 없음(저장은 위임) | 즉시 반영 | 해당 없음 | 템플릿만 `TemplateStyleSaveRollbackTest`, 나머지 4계열 미검증 |

---

## 8. 테스트 안전망 지도

**공통 특징**: `DetailScreen.kt`/`DetailViewModel.kt`를 직접 인스턴스화하는 테스트는 0개. 전부 (a) Context/Room/Hilt/Uri 의존 없는 순수 함수·데이터 클래스 직접 테스트, 또는 (b) 동일 제어흐름을 재현한 Fake 기반 테스트다(Robolectric 등 신규 프레임워크 미도입 방침이 각 테스트 파일 주석에 명시됨).

| 영역 | 판정 | 근거 |
|---|---|---|
| 뒤로가기와 완료 저장 | 일부 | `DetailScreenExitSaveGuaranteeTest`(Fake)로 로직 검증. 실제 저장→네비게이션 순서(4927줄)는 실기기 의존 |
| 저장 중 화면 종료 | 충분(로직 수준) | `DetailScreenExitSaveLossTest`(결함 재현) + `GuaranteeTest`(수정 검증) 촘촘. 단 `viewModelScope`/`NavBackStackEntry` 실제 생명주기는 Fake라 실기기 검증 별개 |
| 저장 실패 후 롤백 | 충분 | `BackgroundColorSaveRaceTest`, `StyleSaveRaceTest`, `TemplateStyleSaveRollbackTest` |
| 자동저장과 수동 저장 경합 | 일부 | 스타일 저장 간 경합은 커버, draft-flush와 confirm-save 간 실제 통합 경합은 `shouldPersistDraftRevision` 순수 함수만 커버 |
| Undo·Redo(5계열) | 일부 | 템플릿만 실패 롤백까지 검증. 스티커/도장/낙서/사진변형 undo·redo 자체는 Uri/Context 의존이라 **직접 테스트 없음** |
| 선택된 스티커/도장/낙서 상태 | 부족 | `setSelectedStickerId` 등 자체나 탭 전환 시 선택 유지 로직 검증 테스트 없음 |
| 사진 교체 실패 시 기존 파일 유지 | 부족~일부 | `PostcardImageStorageTest`는 `deleteIfOwnedByApp` 경계만 검증. `updatePostcardImage()` 자체 순서·롤백은 미검증 |
| 미리보기·저장·공유·내보내기 결과 일치 | 일부 | `PostcardOverlayExportLogicTest`가 export 계산은 잘 커버하나 실제 비트맵 결과 일치는 미검증 |
| 취소 시 임시 파일 처리 | 일부 | `PostcardDraftStorageTest`로 draft 파일은 검증, 카메라 임시 파일 정리(1801~1809)는 미검증 |
| 오래된 엽서 데이터 로드 | 일부 | 손상 파일/레거시 필드 파싱은 커버, Room 자체 마이그레이션 경로는 미검증 |
| 삭제 실패 처리 | 일부 | 파일 정리 단계는 `PostcardDeletionManagerTest`로 검증. `deletePostcard()`의 Room DAO 삭제 자체는 "코드 리뷰로만 확인"이라고 테스트 주석에 명시 — 자동 테스트 없음 |
| 날짜 yyyy-MM-dd | 충분 | `PostcardDateFormatTest` |

제2차에서 우선 보강이 필요한 영역: **선택 객체 상태**, **스티커/도장/낙서/사진변형 Undo·Redo**, **사진 교체 실패 롤백**, **삭제 실패 처리**.

---

## 9. 위험도별 분리 후보

### 낮은 위험
- 저장 에러 AlertDialog 7종(DetailScreen.kt 5878~6151) — 완전히 동일한 패턴 반복
- 글귀/템플릿 관련 AlertDialog 5종(5208~5699)
- 미래엽서 날짜선택/확인 다이얼로그(5745~5876)
- 배경/텍스트 탭 콘텐츠(4057~4479) — 기존 `Postcard*Picker` 컴포저블을 호출하는 얇은 레이아웃
- 상단 헤더 바 Row(4890~4938, 더보기 메뉴 제외)
- `EditorPercentSlider`, `EditorSecondaryButton` 등 이미 분리된 소형 컴포저블(773~1315) — 참고용, 이미 완료
- DetailViewModel의 무상태 순수 함수 8개(§5-4) — 제7차 대상

### 중간 위험
- 스티커/도장/낙서 탭 콘텐츠 전체(패널 부분) — 로컬 상태와 VM 상태가 혼재
- 더보기 메뉴(내부에 export 오버레이 계산 직접 포함)
- 공유 미리보기 바텀시트(`SharePreviewBottomSheet`)
- `stickerEditMode`/`stickerEditModeOwnerId`와 `selectedStickerId`의 동기화 로직

### 높은 위험
- 메인 엽서 캔버스 Box 전체(2019~3623, 약 1,600줄) — 4개 오브젝트(사진/스티커/도장/낙서)의 제스처가 한 트리 안에서 다수의 `rememberUpdatedState`를 클로저로 공유, 과거 회귀 이력 있음
- Undo/Redo 5계열의 스택·이력 관리(DetailViewModel 1152~1780)
- 자동저장/확정저장/화면종료저장(`scheduleDraftAutosave`, `persistDraftNow`, `saveEditsAndClearDraft`, `awaitPendingStyleSaves`)
- 저장 경합 제어(`styleWriteMutex`, `draftSaveMutex`)
- 파일 교체·삭제(`updatePostcardImage`, `deleteStickerCacheUri`, `deleteStickerOriginalIfUnreferenced`, `deletePostcard`)
- 실패 롤백 전반
- 공유·내보내기 렌더링(오버레이 계산 + 실제 비트맵 생성)
- 최종 편집 상태 StateFlow 자체(`_postcard`, `_photoStickers`, `_photoSeals`, `_doodleStrokes`)

---

## 10. 첫 분리 후보

**후보명: 저장 에러 AlertDialog 7종 통합/분리**

| 항목 | 내용 |
|---|---|
| 코드 위치 | `DetailScreen.kt` 5878~6151 (약 270줄, 저장/내보내기/공유 등 실패 시 뜨는 Error 다이얼로그 7개) |
| 선정 이유 | 7개 모두 "제목 + 메시지 + 확인 버튼 → `viewModel.reset*State()` 호출" 패턴이 완전히 동일. state는 각 `*State is *.Error`로 이미 파생돼 전달 가능, ViewModel을 다이얼로그 내부에서 직접 찾지 않고 `onDismiss` 콜백만 받으면 됨. 최종 편집 상태(스티커/도장/낙서/postcard)를 전혀 소유하지 않음. 다른 기능과의 결합도 최소(각자 자신의 Error 상태만 참조) |
| 필요한 테스트 | 자동 테스트는 UI 텍스트/네비게이션이 아니므로 우선순위 낮음. 제2차에서 "각 Error 상태 진입 시 대응 다이얼로그가 뜨고, 확인 클릭 시 대응 reset 함수가 정확히 1번 호출된다"는 점을 실기기 또는 Compose UI 테스트로 고정 필요 |
| 예상 변경 파일 | `DetailScreen.kt`(호출부 축소) + 새 파일 1개(예: `DetailScreenSaveErrorDialogs.kt`) |
| 예상 검증 항목 | 7종 각 에러 상태 재현(배경/폰트/레이아웃/날짜형식/이미지/내보내기/공유 등) → 다이얼로그 문구·버튼 동일, 확인 클릭 시 상태 정상 리셋, 다른 저장 흐름에 영향 없음 |

우선순위 2·3위(제4차 이후 고려): (2) 글귀/템플릿/미래엽서 Dialog 그룹(5208~5876), (3) 배경/텍스트 탭 콘텐츠(4057~4479).

---

## 11. 먼저 건드리면 안 되는 영역

| 영역 | 이유 | 필요한 선행 조건 |
|---|---|---|
| 메인 엽서 캔버스 Box(2019~3623) | 4개 오브젝트 제스처가 다수의 `rememberUpdatedState` 클로저로 얽혀 있고, 낙서 탭 pointerInput 조건부 연결 관련 회귀 이력이 코드 주석에 남아 있음(3303~3309) | 오브젝트별 제스처를 먼저 상태 홀더로 승격, Compose UI 테스트 또는 실기기 회귀 시나리오 확보 |
| Undo/Redo 5계열 | 파일 삭제 지연(`stickerCleanupCandidates`)과 얽혀 있어 스택 구조를 잘못 건드리면 고아 파일 또는 되돌리기 시 깨진 참조 위험(§12 발견사항 1) | 스티커 undo/redo에 대한 자동 테스트 우선 확보(현재 0) |
| 자동저장/확정저장/화면종료저장 | `draftSaveMutex`/`styleWriteMutex` 두 Mutex와 revision 카운터로 정교하게 조율된 경합 방지 로직. `awaitPendingStyleSaves`의 Job 목록에서 하나라도 누락되면 조용한 데이터 유실 재발 가능(과거 `DetailScreenExitSaveLossTest`가 재현한 문제와 동일 유형) | 저장 관련 자동 테스트를 실제 코드 경로에 최대한 가깝게 강화, Job 목록 변경 시 반드시 회귀 테스트 동반 |
| 파일 교체·삭제 | 사용자 원본 파일 삭제·손상 가능성이 가장 큰 영역. `File.delete()` 반환값 미확인 패턴이 일관되어 있어 구조를 바꾸다 실수하면 자각 없이 파일이 남거나 사라질 수 있음 | 삭제 실패 처리에 대한 테스트 보강 우선 |
| Room Migration(`MIGRATION_16_17`) 및 Entity | 스키마 변경은 이번 대작업 범위 밖이며 명시적 요청 없이 변경 금지 | 별도 작업지시서 필요 |
| 공유·내보내기 렌더링 | 오버레이 계산(`createStickerOverlaysForExport` 등)이 캔버스 미리보기와 동일한 좌표계를 공유 — 잘못 분리하면 미리보기와 실제 공유 결과가 어긋날 수 있음 | `PostcardOverlayExportLogicTest`가 이미 존재하므로 이를 기준선으로 삼아 분리 전후 비교 |

---

## 12. 발견했지만 수정하지 않은 사항

| # | 결함/개선 후보 | 위치 | 영향도 | 데이터 손상 가능성 | 긴급도 | 별도 작업 필요 여부 |
|---|---|---|---|---|---|---|
| 1 | `revertToConfirmedState`가 스티커 파일 존재를 검증하지 않고 `confirmedStickersBaseline`을 그대로 대입 | `DetailViewModel.kt` 664~693 (참고: `isStickerFileStillReferenced` 1319) | 되돌리기 후 깨진 스티커 이미지 표시 가능 | 낮음(표시 오류, 파일 자체 손상 아님) | 중 | 예 — 재현 조건(스티커 30개 이상 연속 편집 후 되돌리기)이 특정적이라 별도 재현/검증 필요 |
| 2 | `awaitPendingStyleSaves`의 2초 타임아웃 만료 시 저장 미완료 상태로 네비게이션 진행 가능 | `DetailViewModel.kt` 3897~3934 | 느린 디스크/DB IO에서 마지막 조작 유실 가능 | 낮음~중(의도적 트레이드오프로 주석에 명시돼 있으나 실패 감지/사용자 알림 없음) | 중 | 예 — 제2차 테스트 보강 대상으로 우선 검토 권고 |
| 3 | 스티커 초안/확정 파일 승격의 부분 실패 시 고아 파일 발생 가능 | `DetailViewModel.kt` 730~760, 932~982, 2357~2458 | 디스크 누적(반복 실패 시) | 낮음 | 낮 | 아니오(모니터링만 필요, 별도 정리 로직은 이미 `OrphanFileDiagnostics` 존재) |
| 4 | `deletePostcard`에서 `failedAssets`(파일 정리 부분 실패)가 사용자에게 노출되지 않고 로그만 남음 | `DetailViewModel.kt` 4561~4597 | 고아 파일 누적을 사용자/운영이 인지할 방법이 로그뿐 | 낮음(Room 참조는 이미 끊김) | 낮 | 아니오 |
| 5 | 삭제 계열 함수들이 `File.delete()` 반환값을 확인하지 않음(일관된 패턴) | `DetailViewModel.kt` 4154~4180, 4182~4200, 1332~1346, 1357~1378 | 삭제 실패가 조용히 무시됨 | 낮음 | 낮 | 아니오(패턴 통일 시 함께 검토) |
| 6 | `updateMessage`만 저장 실패 시 롤백을 하지 않음(다른 update 함수들과 다른 처리) | `DetailViewModel.kt` 2510~2546 (주석 2536~2539: "이 값을 관찰하는 UI 상태가 없어") | 실패해도 화면상 값과 실제 Room 값이 어긋날 가능성(주석상 의도적) | 낮음 | 낮 | 아니오, 다만 제8차 재감사 시 의도적 설계인지 재확인 권고 |
| 7 | `draftAutosaveJob`이 `awaitPendingStyleSaves`의 대기 Job 목록에 포함되지 않음 | `DetailViewModel.kt` 3897~3934 | `flushDraftNow()` 호출이 누락되는 경로가 생기면 초안 자동저장이 대기 목록에서 빠질 수 있음(현재는 `ON_STOP`에서 별도 호출되어 실질 위험 낮음) | 낮음(현재 경로상으로는 커버됨) | 낮 | 아니오, 제8차 재감사 시 함께 재확인 권고 |
| 8 | 탭을 전환해도 `selectedStickerId`/`selectedSealId`가 초기화되지 않음(명시적 초기화 로직 없음) | `DetailScreen.kt` (전역), `LaunchedEffect(customizationPagerState...)` 패턴 부재 확인 | 버그 단정 불가 — 의도된 "선택 유지"일 수 있음 | 없음 | 낮 | 아니오, 사용자 확인 후 의도 여부만 기록 권고 |
| 9 | 작업지시서 원문의 "도장 6종 복구"와 실제 코드의 8종(SealType)이 불일치 | 작업지시서 §2 vs `PostcardSealItem.kt:11~18` | 문서-코드 불일치, 기능적 영향 없음 | 없음 | 낮 | 아니오, 사실관계 보고로 충분 |

치명적 결함(데이터 손실/삭제/Migration 실패/크래시/원본 손상)에 해당하는 항목은 없어 감사를 중단하지 않았다.

---

## 13. 제2차 작업 권고안

- **제2차 목표**: 제10단계에서 선정한 첫 분리 후보(저장 에러 AlertDialog 7종)의 현재 동작을 고정하는 회귀 테스트 확보, 그리고 §8에서 "부족"으로 판정된 영역(선택 객체 상태, 4계열 Undo/Redo, 사진 교체 실패 롤백, 삭제 실패 처리) 중 첫 분리 후보와 직접 관련된 범위의 안전망 보강
- **필요한 테스트**:
  - 7종 저장 에러 다이얼로그 각각에 대해 "Error 상태 진입 → 다이얼로그 노출 → 확인 클릭 → 대응 reset 함수 1회 호출 → 상태 Idle 복귀"를 고정하는 테스트(Compose UI 테스트 또는 기존 프로젝트 방식인 Fake 기반 로직 테스트)
  - 가능하다면 실기기에서 7종 에러를 순서대로 유발해 다이얼로그 문구·동작 스크린샷/메모로 기준 동작 기록
- **작업 범위**: 프로덕션 코드 변경 없음(제2차는 테스트 안전망 구축 단계). §9 부족 판정 영역 전체를 이번 차수에서 다 메우지 않고, 첫 분리 후보와 직접 관련된 범위로 한정
- **완료 조건**: 첫 분리 후보의 기존 동작이 자동 테스트 또는 문서화된 실기기 절차로 고정됨. 제3차에서 실제 분리 시 "동작 변경 없음"을 비교할 기준선이 확보됨

---

## 14. 대작업 진행 위치

```text
실제 작업일: 2026-08-06
완료 위치: 제1차 완료
다음 시작 위치: 제2차 1단계
전체 대작업: 제1차~제10차 중 제1차 완료
```

---

## 부록: 사용자 검증 대기

- 제1차는 조사 전용 작업으로 코드 변경이 없어 실기기 검증 대상 자체가 없음
- §12 발견사항 1(되돌리기 후 깨진 스티커 이미지 가능성)은 코드 근거는 명확하나, 실제 재현에는 특정 조작 순서(스티커 30개 이상 연속 편집 후 되돌리기)가 필요해 이번 조사만으로는 실기기 발생 여부를 확인하지 못함 — 제2차 이전에 필요 시 별도 재현 시도 권고
- §12 발견사항 8(탭 전환 시 선택 유지)은 의도된 동작인지 결함인지 코드만으로는 판정 불가 — 사용자 확인 필요
