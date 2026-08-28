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

## 2026-08-26 — 54일차: `/compact`·`/clear` 경계 규칙 + 마스킹테이프 UI/생성 문법 정돈

> 2026-08-15 이후 이 문서는 갱신되지 않았다. 그 사이 작업은 수동 표준 모드로
> 진행돼 완료보고서로 인수인계됐고, 이 문서가 비어 있다고 해서 작업이 없었던
> 것은 아니다. 이번 항목은 54일차 결과만 다룬다.

**오늘 올라간 커밋 4개** (브랜치 `feature/photo-sticker`)

1. `5f0aaa2` — `CLAUDE.md`에 `/compact`·`/clear` 작업 경계 규칙 명문화
2. `a0a431f` — 마스킹테이프 복제/삭제를 평면 텍스트 액션으로 전환
3. `7e40e72` — 생성 navigation을 고정 하단으로, 상세 Property를 편집창으로 이동
4. `fe8435a` — 세 생성 방식을 `+ 추가` 하나로 통일 + 사진 미반영 버그 수정

**변경 파일**

- `CLAUDE.md` — "세션 관리"에 `COMPACT/CLEAR 권장 지점` 명시 출력 규칙, `/clear` 금지 조건 8가지, "대화가 길다"만으로는 권장하지 않는다는 문장 추가. "Compact Instructions"를 `/clear` 사전 조건으로도 겸용.
- `app/src/main/java/com/postcardmemory/ui/detail/MaskingTapeDetailScreen.kt` — 패널 전면 재구성.
- `app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt` — 마스킹테이프 생성 탭 상태(`maskingTapeCreationTabIndex`) 추가, Property 콜백 5개를 `onEditMaskingTapeProperties` 하나로 통합, 슬라이더 드래그 스냅샷 플래그 3개 제거.
- `app/src/main/java/com/postcardmemory/ui/detail/EditorBottomTabBar.kt` — `StickerSubcategoryNavBar` → `EditorSubcategoryNavBar`로 일반화(스티커 전용 로직은 원래 없었음).
- `app/src/main/java/com/postcardmemory/utils/MaskingTapePhotoDecoder.kt` — 사진 버그 수정 1줄.
- `app/src/test/.../EditorSubcategoryNavBarStructureTest.kt` — 구 `StickerSubcategoryNavBarStructureTest.kt`에서 rename + 마스킹테이프 호출부 검사 추가.
- `app/src/test/.../MaskingTapeCreationGrammarStructureTest.kt` — 신규.

**확인된 사실 / 결정**

- **사진 마스킹테이프는 지금까지 한 번도 렌더된 적이 없었다.** 원인은 권한이나 URI가 아니라 `MaskingTapePhotoDecoder`의 로직 버그였다 — bounds 측정 단계에서 `inJustDecodeBounds = true`로 부른 `decodeStream`은 **성공해도 설계상 항상 null**을 돌려주는데, 그 결과를 `?: return@runCatching null`로 실패 판정에 써서 모든 사진이 예외 없이 null로 빠졌다. 실제 실패는 바로 아래 `outWidth/outHeight <= 0` 검사가 이미 걸러내고 있어, 잘못된 가드만 제거하는 1줄 수정으로 복구했다. 미리보기(`MaskingTapeShapes.kt`)와 저장/공유 export(`PostcardImageExporter.kt`)가 같은 디코더를 공유하므로 양쪽이 함께 복구됐다.
- 마스킹테이프 Undo는 ViewModel이 자동 기록하는 구조가 아니라 **호출부가 `recordMaskingTapeSnapshotForUndo()`를 명시적으로 부르는 구조**다. 덕분에 ViewModel·Undo 구조를 건드리지 않고 "저장 1회 = Undo 1단계"를 호출부 정리만으로 달성할 수 있었다.
- 생성(`+ 추가`)과 기존 객체 편집(`편집|복제|삭제`)을 역할로 분리했다. 세 생성 방식(기본 디자인/커스텀/사진)은 모두 "새 테이프 추가"라는 같은 역할이므로 진입점을 하나로 통일하고, 탭은 목적지만 결정한다. 세 목적지가 전부 modal이라 탭을 바꿔도 패널 높이와 `+ 추가` 위치가 고정된다.
- 프리셋 선택·커스텀 편집은 생성창 안의 local draft이며 `저장`에서만 실제 테이프가 생긴다. 취소/Back/바깥 dismiss는 아무것도 만들지 않는다.
- 빈 상태 안내 상자(`EditorEmptyHint`)와 "붙인 마스킹테이프" 제목은 제거했다. `+ 추가`가 목록 줄 안에 항상 있어 같은 말을 반복하게 되고, 제목 유무에 따라 `+ 추가` 위치가 흔들리기 때문이다.

**검증**

- `compileDebugKotlin` — 성공(마스킹테이프 관련 신규 경고 없음).
- `testDebugUnitTest` — **51 suites / 495 tests / failures 0 / errors 0**. 신규·rename된 구조 테스트가 실제로 실행됐음을 `test-results` XML에서 확인(skipped 0).
- `git diff --check` 이상 없음. 각 커밋마다 의도한 파일만 stage(`.kotlin/`은 매번 제외).
- **실기기 검증 완료** — 사용자가 4개 커밋 각각에 대해 확인함.
- **미실행**: `MaskingTapePhotoDecoder` 전용 자동 테스트는 없다. `BitmapFactory`·`ContentResolver` 의존이라 이 프로젝트의 순수 JUnit 환경에서는 작성할 수 없어, 사진 렌더 회귀는 현재 실기기 확인으로만 잡힌다.

**Git 상태**: `feature/photo-sticker`, HEAD `fe8435a`, local == origin (ahead/behind 0/0), working tree clean(`.kotlin/` 기존 untracked만).

**남은 위험 / 미확인**

- **`PickVisualMedia` URI를 persistable로 가정하는 전제가 코드에 남아 있다 (후속 작업 후보).**
  - 근거 1: `DetailViewModel.kt`의 `duplicateMaskingTape` 주석 — *"persistable 권한을 받은 갤러리 Uri라 복사가 필요 없다"*.
  - 근거 2: `MaskingTapeItem.photoUri` 선언부 주석 — *"영구 저장소에 복사된 사용자 사진"*.
  - 실제로는 안드로이드 시스템 포토피커(`ActivityResultContracts.PickVisualMedia`)가 돌려주는 `content://media/picker/...` URI는 persistable이 아니다. `takePersistableUriPermission` 호출은 `SecurityException`을 던지고 `runCatching`에 조용히 삼켜진다. **즉 두 주석 모두 사실과 다르고, 사진을 앱 저장소로 복사하는 코드는 어디에도 없다.**
  - 54일차 사진 수정은 "즉시 렌더" 회귀 복구만을 범위로 했고, 저장 구조 재설계는 사용자가 명시적으로 범위에서 제외했다.
  - 결과적으로 **장기 보관 시 사진이 유실될 가능성이 남아 있다.** 오래된 엽서를 다시 열었을 때 사진 테이프가 폴백색으로 보이는 신고가 들어오면 이 항목을 먼저 의심할 것. 해결하려면 사진 스티커처럼 앱 저장소로 복사하는 구조가 필요하고, 그때는 삭제 방어(`PostcardDeletionManager`, `OrphanFileDiagnostics`)까지 함께 검토해야 하므로 **별도 작업으로 분리한다.**
- 마스킹테이프 편집창의 미리보기는 회전 시 Dialog 영역 밖으로 잘릴 수 있다(clip하지 않음). 실기기에서 문제로 보고되지 않아 그대로 뒀다.
- 53일차 조사에서 확인된 UI token / 콘텐츠 색 / legacy alias의 hex 중복 정리는 계속 미착수(의도적 보류).

**다음 작업 하나**: 위 `PickVisualMedia` 영속성 전제 문제를 실제로 다룰지 결정하기. 다루기로 하면 "사진 마스킹테이프를 앱 저장소로 복사 + 삭제 방어 연결"을 독립 작업으로 시작한다.

## 2026-08-28 — 56일차: IDE inspection warning cleanup 마감

**목표**: Android Studio가 표시하던 IDE warning 8개 카테고리를 production 동작·공개 계약 변경 없이 정리(독립 작업 단위).

**변경 파일**

- `app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt`
- `app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt`
- `app/src/main/java/com/postcardmemory/ui/futuremail/FutureMailboxViewModel.kt`
- `app/src/main/java/com/postcardmemory/ui/gallery/SheepRanchStage.kt`
- `app/src/main/java/com/postcardmemory/utils/PostcardImageExporter.kt`

**핵심 변경**

- **미사용 함수 삭제 (DetailViewModel.kt)**: `updateMessageFont`, `updateDateFormat`, `setDateTextScalePreview`, `saveDateTextScale` — 전체 코드베이스 grep으로 호출부 0개 확인. 폰트/날짜형식은 템플릿 일괄 적용 경로로만 바뀌고, 날짜 크기 조절 UI는 54일차 이전 "조절 대상 토글" 제거로 이미 화면에서 사라졌음을 확인. 이 4개만 쓰던 헬퍼 `normalizeMessageFont`, `normalizeDateFormat`도 함께 삭제(연쇄 고아화).
- **불필요한 `suspend` 제거 12곳 (DetailViewModel.kt)**: 스티커/도장/텍스트 스티커/마스킹테이프/라벨 스티커/낙서 각각의 `persist*EditState()`/`readConfirmed*State()` — 내부에 suspend 호출 없는 순수 블로킹 파일 I/O이고 전부 private, 호출부는 이미 `viewModelScope.launch(Dispatchers.IO)` 내부라 시그니처 변경이 안전함을 호출부까지 확인. `awaitStickerCleanupSweep`/`awaitPendingStyleSaves`/`persistDraftNow`/`awaitResult`는 실제 suspend 호출이 있어 그대로 둠.
- **Legacy Long → Duration 3곳**: `PENDING_STYLE_SAVE_TIMEOUT_MS`, `DRAFT_AUTOSAVE_DEBOUNCE_MS`(DetailViewModel.kt), `RACE_NOT_ENOUGH_HINT_MILLIS`(SheepRanchStage.kt)에 `.milliseconds` 적용. DetailScreen.kt의 `withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis)`는 Compose `AwaitPointerEventScope`가 제공하는 별도 Long 전용 멤버 함수(Duration 오버로드 없음)라 변환 시도 시 컴파일 에러 발생 → 원본 그대로 원복.
- **KTX `createBitmap` 전환 2곳**: 빈 Bitmap 생성 패턴만(`DetailViewModel.kt`, `PostcardImageExporter.kt`) `androidx.core.graphics.createBitmap`으로 교체. source crop/matrix 오버로드 4곳(`ImageUtils.kt`, `PostcardImageExporter.kt`, `PostcardRenderSpec.kt`)은 core-ktx 1.16.0 소스 확인 결과 대응 wrapper가 없어 유지.
- **operator-assignment 19곳**: `x = x + y`/`x = x - y` → `x += y`/`x -= y` 기계적 치환(DetailScreen.kt 13, DetailViewModel.kt 4, FutureMailboxViewModel.kt 2). `MaskingTapeShapes.kt`/`SealShapes.kt`의 `strokeWidth = strokeWidth * 0.7f` 등 3곳은 `drawLine(...)` 내부의 named argument라 대상 아님으로 확인해 제외.
- **의도적으로 손대지 않음**: Typo 경고(`removedBgUri`, `Snackbar`, `uACBD`, `uACFC`)는 IDE spellcheck 오탐 — 식별자 변경도 suppression 추가도 하지 않음. Android Studio/Clangd 플러그인 내부 오류는 production 코드 경고가 아니므로 이번 범위에서 제외.

**검증 방법과 결과**

- `gradle compileDebugKotlin` — BUILD SUCCESSFUL (무관한 기존 경고만 남음: Migration 파라미터명, LocalLifecycleOwner deprecation 등).
- `gradle testDebugUnitTest` — 전체 통과.
- 삭제한 함수 6개(`updateMessageFont`/`updateDateFormat`/`setDateTextScalePreview`/`saveDateTextScale`/`normalizeMessageFont`/`normalizeDateFormat`) 전체 코드베이스 grep 재확인 — 실제 호출부 0, `DetailScreenExitSaveLossTest.kt` 주석 2곳에서만 이름 언급(코드 아님, 컴파일 영향 없음).
- 최종 `git diff` 5개 파일 전수 재검토 — 위 항목 외 예상 밖 production 변경 없음, operator-assignment 개수(13+4+2=19)와 createBitmap 개수(1+1=2)가 지시서 기대치와 일치.

**남은 위험 또는 미검증 항목**

- `DetailScreenExitSaveLossTest.kt` 주석 2곳이 삭제된 함수 이름을 그대로 언급 — 컴파일/동작에는 영향 없는 stale 주석이며 이번 cleanup 범위 밖으로 남겨둠.
- `FontUpdateState`/`DateFormatUpdateState` UI 상태 plumbing은 이제 항상 Idle로만 남는 죽은 경로가 됐으나, 이번 함수 삭제와 별개로 상태 자체를 지우는 것은 범위 확대라 손대지 않음.
- 실기기 검증 없음(경고성 리팩터링이라 자동 컴파일/테스트로 충분하다고 판단, 화면 동작 변화 없음).

**Git 상태**: `feature/photo-sticker`, HEAD `d4e95b9`, local == origin(ahead/behind 0/0). 위 5개 파일 unstaged 수정 상태였고, 이번 HANDOFF 갱신 직후 이 문서 갱신 작업까지 함께 commit/push 예정(사용자 승인 후).

**다음 작업**: HANDOFF 운영 규칙을 AGENTS.md에 보강한 뒤, 사진(Photo) UI/UX 전수조사(코드 조사만, production 수정 금지)로 진행.
