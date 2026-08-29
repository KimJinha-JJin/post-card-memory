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

**Git 상태**: `feature/photo-sticker`, 이번 HANDOFF 갱신을 포함해 commit `78d336f`로 push 완료. local == origin(ahead/behind 0/0), working tree clean(`.kotlin/` 기존 untracked만).

**다음 작업**: HANDOFF 운영 규칙을 AGENTS.md에 보강한 뒤, 사진(Photo) UI/UX 전수조사(코드 조사만, production 수정 금지)로 진행.

## 2026-08-28 — 56일차: HANDOFF 갱신 시점 규칙을 AGENTS.md에 보강

**목표**: "독립 작업 단위 종료 시 HANDOFF.md 갱신"이 `/clear` 전용 규칙이 아니라 일반 작업 루틴임을 `AGENTS.md`에 명문화(기존 STOP 규칙·자율 진행 조건·선례 사용 원칙·commit/push 승인 경계·데이터 안전 규칙·`/compact`·`/clear`는 재작성하지 않음).

**변경 파일**

- `AGENTS.md` — 11장 제목을 "Android Studio와 작업 종료"에서 "독립 작업 단위 종료와 Android Studio 작업 환경"으로 바꾸고, 기존 Android Studio 파일 잠금 규정 2줄은 그대로 유지한 채 다음을 추가: HANDOFF 갱신 트리거 6가지(조사 완료/구현+자동검증 완료/실기기 검증 완료/commit·push 완료/중요한 제품 판단 확정/다음 독립 작업 진입 직전), 갱신하지 않는 중간 상태 4가지, Git/AGENTS·CLAUDE/HANDOFF/세션 기억 각각의 역할 구분, `/compact`·`/clear`와의 관계는 `CLAUDE.md`를 따른다는 교차 참조 한 줄. 기존 "세션이나 터미널을 끝내기 전에…" 이하 4개 불릿은 문구만 "작업 단위 종료"로 일반화하고 내용은 그대로 둠.

**검증 방법과 결과**

- 문서 변경만 있고 앱 코드/Room/Gradle 변경 없음 → 빌드·테스트 실행 대상 없음.
- 갱신 후 자체 점검: 기존 규칙과 충돌 없음(세션 종료 케이스는 그대로 하위 항목으로 유지), 같은 내용을 `CLAUDE.md`와 중복 기재하지 않고 교차 참조만 추가, "파일 하나 읽음"류의 사소한 행동마다 갱신하라는 과잉 규칙이 되지 않도록 반대 목록을 명시, `/clear`에만 종속되지 않음을 첫 문장에서 직접 명시, Claude Code/Codex 어느 쪽에도 적용 가능한 일반 규칙(도구별 세부사항은 `CLAUDE.md`로 위임)임을 확인.

**남은 위험 또는 미검증 항목**: 없음(문서 전용 변경).

**Git 상태**: `feature/photo-sticker`, `AGENTS.md`만 unstaged 수정 상태. 지시서 11장 "필요한 commit/push 경계는 기존 프로젝트 규칙을 따른다"에 따라 자동으로 commit/push하지 않고 사용자 승인 대기로 남김(task 1의 cleanup commit과 달리 이 문서 변경은 명시적 commit/push 지시가 없었음).

**다음 작업**: 사진(Photo) UI/UX 전수조사(코드 조사만, production 수정 금지) 진행 후 결과를 별도 HANDOFF 항목으로 남기고 STOP.

## 2026-08-28 — 56일차: 사진(Photo) UI/UX 전수조사 (코드 수정 없음, STOP)

**목표**: "사진" 탭(엽서 기본 사진) 사용자 흐름·코드 구조·저장/복원·Undo/Redo·export를 조사하고, 사진 스티커 등 기존 선례와 비교해 문제·위험·수정 후보를 보고한다. Production 코드는 수정하지 않았다.

**핵심 발견**

- `Postcard.kt`의 `imagePath`(필수, non-null 파일 경로)가 "사진"이고, `layoutStyle`(STAMP/POLAROID/TAPED_FILM/LETTER)별로 독립된 scale/offset/zoom 컬럼 + 공통 `photoEdgeBlur`를 가진다. 스티커·테이프·도장·라벨처럼 "여러 개 중 하나를 선택해 리스트로 관리"하는 객체가 아니라 postcard당 정확히 1개, 필수, 교체만 가능(삭제 개념 없음).
- 위치/확대는 미리보기 캔버스 전체에 대한 pan(드래그)+pinch(확대, 1~3배) 제스처로 조작한다(`DetailScreen.kt` ~2065-2231) — 개별 오브젝트 드래그가 아니라 "캔버스 안 사진을 크롭/줌"하는 역할. `awaitEachGesture`로 제스처당 Undo 스냅샷 1회.
- 사진 교체(`updatePostcardImage`, `DetailViewModel.kt:4358`)는 `PostcardImageStorage.copyToAppStorage`로 **즉시 앱 filesDir/postcards/에 실제 파일 복사** 후 Room 갱신, 성공 확정 후에만 이전 파일을 소유권 확인(`deleteIfOwnedByApp`) 후 삭제. 스티커/테이프처럼 draft 2단계가 아니라 스타일 값들과 함께 즉시 Room에 쓴다.
- 화면 미리보기(`PostcardPreviewContent`)·최종 저장/공유(`PostcardImageExporter`)·템플릿 썸네일이 모두 `PostcardRenderSpec.drawBaseContent()/drawStampPhoto()/drawPolaroidPhoto()/drawTapedFilmPhoto()` 하나만 호출 — 계산이 갈라질 여지 없음(AGENTS.md 13장 불변값 재확인).
- **사진 스티커(갤러리/파일로 추가한 것)는 원본 URI를 앱 저장소로 복사하지 않고 Photo Picker/Document URI를 그대로 보관한다**(`PhotoStickerDetailScreen.kt:81-111`, `PhotoStickerItem(originalUri = uri, displayedUri = uri)`) — `takePersistableUriPermission`을 부르지만 `runCatching`으로 실패를 삼킨다. 54일차에 마스킹테이프 사진에서 발견한 것과 **같은 근본 원인·같은 위험**이 사진 스티커에도 그대로 있다. 반면 "사진"(`imagePath`) 자체는 항상 앱 저장소 복사본이라 이 위험이 없다 — 두 기능의 저장 안전성이 다르다.

**Undo/Redo 상세**

- pan/pinch 이동·확대, 사진 크기 슬라이더 → `PhotoTransformSnapshot` 스택으로 Undo/Redo 가능(제스처/슬라이더 조작 1회 = 1단계).
- **가장자리 흐림 슬라이더는 `PhotoTransformSnapshot`에 필드 자체가 없어 Undo 불가능** — 옆에 있는 실행취소 버튼이 이 값은 되돌리지 못한다.
- 레이아웃(우표/폴라로이드/…) 전환은 Undo 스택에 없다(다만 각 레이아웃 값이 독립 컬럼이라 전환 자체가 파괴적이지는 않음).
- "사진 바꾸기"는 Undo가 전혀 없고, 성공 즉시 이전 파일이 삭제되어 재선택 외에는 되돌릴 방법이 없다.

**현재 UI 문법 판단**: 레이아웃 선택 Box(우표/폴라로이드/테이프필름/편지지)는 상호 배타적 선택 상태를 보여주는 실제 정보라 box-removal 대상이 아님. 사진 탭 나머지(슬라이더·버튼)는 이미 공용 컴포넌트(`EditorPercentSlider`, `EditorSecondaryButton`, `EditorUndoRedoButtons`) 사용 중이라 문법 부채 없음.

**수정 후보(구현 안 함, 우선순위순)**

1. (작고 안전) `PhotoTransformSnapshot`에 `photoEdgeBlur` 필드 추가해 Undo 대상에 포함.
2. (저장 구조 변경, 별도 지시서 필요) 사진 스티커 원본을 마스킹테이프처럼 앱 저장소로 복사하는 구조로 바꿀지 결정.
3. (선택, 급하지 않음) 레이아웃 전환이 Undo 버튼 옆에 있어 "이것도 되돌려주겠지"라는 오해를 줄 수 있음 — UX 문구/배치만 조정할지 판단.
4. (판단 필요, 데이터 삭제 정책 변경) "사진 바꾸기" 실행취소 지원 여부 — 이전 파일 즉시 삭제 정책을 바꿔야 해서 범위가 큼, 권장하지 않되 사용자 판단에 맡김.

**위험**: 데이터 손상 위험은 없음(사진 자체 저장 경로는 안전). 위 1은 신뢰도(기대한 실행취소가 안 먹힘) 문제, 2는 54일차에 이미 열어둔 것과 같은 계열의 잠재적 사진 유실 위험, 4는 사용자 실수 복구 불가 문제.

**변경 파일**: 없음(코드 조사만, `docs/ai/HANDOFF.md`만 갱신).

**검증**: 코드 읽기·grep 기반 조사만 수행, 빌드/테스트 대상 없음.

**Git 상태**: `feature/photo-sticker`, 이번 조사로 코드 변경 없음. `AGENTS.md`(56일차 HANDOFF 규칙 보강)만 아직 commit 승인 대기.

**다음 작업**: 위 수정 후보 4가지 중 어느 것을 어떤 순서로 진행할지 사용자 확정 필요 — STOP.

## 2026-08-28 — 56일차: 사진 탭 2단 구조 개편 (레이아웃 | 사진 편집) + 템플릿·사진 변경 UI 제거

**목표**: 전수조사에서 확인한 사진 탭의 역할(레이아웃 상태 vs 사진 표현 상태)에 맞춰 하단 subcategory를 `레이아웃 | 사진 편집` 두 화면으로 분리하고, 추천/내 템플릿 UI와 "사진 바꾸기" 기능을 사진 탭에서 제거한다. 저장 구조·Room·RenderSpec·pan/pinch 동작은 변경하지 않는다.

**변경 파일**

- `app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt` (핵심 변경)
- `app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt`
- `app/src/main/java/com/postcardmemory/ui/components/PostcardLayoutPicker.kt`
- `app/src/main/java/com/postcardmemory/data/PostcardRepository.kt`
- `app/src/main/java/com/postcardmemory/data/PostcardDao.kt`
- `app/src/test/java/com/postcardmemory/ui/detail/SaveErrorDialogStructureTest.kt`
- `app/src/test/java/com/postcardmemory/ui/detail/EditorSubcategoryNavBarStructureTest.kt`

**핵심 변경**

- 하단 고정 subcategory nav에 `PHOTO_TAB_PAGE_INDEX`(=0) 분기를 추가해 `레이아웃 | 사진 편집`을 표시(기존 스티커/마스킹테이프/낙서와 같은 `EditorSubcategoryNavBar` 재사용, 화면 로컬 상태 `photoSubTabIndex`).
- **레이아웃 화면**: `PostcardLayoutPicker`에서 Undo/Redo 버튼과 "사진 위치·크기" 텍스트를 제거. 이후 사용자 지시로 4개 선택지를 가로 나열(탭/세그먼트형)에서 세로 4행 목록으로 다시 바꿈 — 각 행 왼쪽에 사각(둥근 모서리 4dp) 체크 표시, 선택된 항목만 SunsetGold로 채워진 체크 아이콘 표시, 원형 라디오버튼 형태는 사용하지 않음, 항목 전체를 감싸는 큰 Box 없이 행별 클릭 영역과 체크 상태·텍스트만으로 선택 관계를 전달(선택 상태를 보여주는 표시는 실제 정보이므로 유지 — box-removal 대상 아님).
- **사진 편집 화면**: Undo/Redo 버튼(`EditorUndoRedoButtons`)을 레이아웃 화면에서 이쪽으로 옮겨 "레이아웃 전환이 Undo 버튼 옆에 있어 history처럼 보이는" 문제를 해소. 슬라이더 라벨을 "사진 크기"→"크기", "가장자리 흐림"→"블러"로 자연스럽게 다듬음(표시 문구만 변경, `photoEdgeBlur` 필드명·`savePhotoEdgeBlur` 등 내부 이름은 그대로).
- **템플릿 UI 제거**: "템플릿" 접기/펼치기 헤더, "추천 템플릿"/"내 템플릿" `PostcardTemplateSection` 호출 2곳, "현재 꾸밈 저장" 진입점, 그리고 이들만 쓰던 저장/이름변경/덮어쓰기/삭제 다이얼로그 4개 + 관련 `LaunchedEffect` 2개를 DetailScreen.kt에서 제거. `templatesExpanded`/`showSaveTemplateDialog`/`templatePendingRename`/`templatePendingOverwrite`/`templatePendingDelete`/`canUndoTemplateStyle`/`canRedoTemplateStyle`/`userTemplates`/`templateSaveState`/`templateManageState`/`lastAppliedTemplateId`/`effectiveSelectedTemplateId` 등 DetailScreen.kt 전용 로컬 state도 함께 정리.
- **사진 변경 기능 제거**: "사진 바꾸기" 버튼, `PhotoSourceMenu` 호출, 3개 launcher(`postcardPhotoPicker`/`postcardFilePicker`/`postcardCameraCapture`), `launchPostcardCameraCapture()`, `showPhotoSourceMenu`/`pendingCameraCapturePath`/`pendingCameraCaptureCleanupPath` 상태, `LaunchedEffect(imageUpdateState)` 정리 로직을 DetailScreen.kt에서 제거.
- **연쇄 dead code 삭제(호출부 0 확인 후)**: `DetailViewModel.updatePostcardImage()`, `resetImageUpdateState()`, `ImageUpdateState` sealed interface, `_imageUpdateState`/`imageUpdateState` StateFlow, `imageUpdateJob`(awaitPendingStyleSaves 목록에서도 제거), `PostcardRepository.updatePostcardImagePath()`, `PostcardDao.updatePostcardImagePath()`(단순 UPDATE 쿼리 메서드, 컬럼/스키마/Migration 변경 아님). `imagePath` 컬럼 자체와 초기 엽서 생성 경로(`CameraViewModel`)는 완전히 별개라 영향 없음을 확인.
- **의도적으로 남긴 것(범위 확대 방지)**: `PostcardTemplateSection`/`PostcardTemplateRow.kt`/`rememberTemplatePreviewBitmap`/`BuiltInTemplates`/`resolveEffectiveSelectedTemplateId`와 `DetailViewModel`의 `applyTemplate`/`saveCurrentStyleAsNewTemplate`/`renameUserTemplate`/`overwriteUserTemplateWithCurrentStyle`/`deleteUserTemplate`/`undoTemplateStyleChange`/`redoTemplateStyleChange`/`TemplateSaveState`/`TemplateManageState`/`userTemplates` 흐름은 전부 그대로 둠 — 사진 탭에서 호출부는 사라졌지만 템플릿 시스템 자체(데이터·DB·썸네일·export)는 이번 작업 범위 밖. `PostcardImageStorage`(사진 파일 복사/삭제 유틸)도 `PostcardImageStorageTest.kt`가 직접 검증하는 대상이라 그대로 둠.
- 가장자리 흐림 Undo 스냅샷(직전 작업에서 추가한 `photoEdgeBlur` 필드 포함 `PhotoTransformSnapshot`), pan/pinch 제스처, `PostcardRenderSpec`/`PostcardImageExporter`는 전혀 손대지 않음.

**정적 확인 결과**

- "사진 바꾸기" 관련 심볼(`사진 바꾸기`, `showPhotoSourceMenu`, `ImageUpdateState`, `postcardPhotoPicker` 등) grep 재확인 — DetailScreen.kt에 잔여 호출부 0.
- "추천 템플릿"/"내 템플릿"/`PostcardTemplateSection`/`BuiltInTemplates`/`rememberTemplatePreviewBitmap` grep 재확인 — DetailScreen.kt에 잔여 참조 0(단, 컴포넌트 파일 자체는 의도적으로 보존).
- `PostcardRenderSpec.kt`/`PostcardImageExporter.kt`/`PhotoSourceMenu.kt`/`PostcardTemplateRow.kt` — `git status`에 등장하지 않음(완전히 미변경 확인).
- `PostcardDao.kt`/`PostcardRepository.kt` diff — `updatePostcardImagePath` 메서드 삭제만 있고 컬럼·스키마·Migration 변경 없음.

**검증 방법과 결과**

- `gradle compileDebugKotlin` — BUILD SUCCESSFUL(무관한 기존 경고만 남음).
- `gradle testDebugUnitTest` — 494 tests / failures 0 / errors 0. `SaveErrorDialogStructureTest`(7종→6종 다이얼로그로 앵커·개수 갱신, `imageError` 테스트 삭제)와 `EditorSubcategoryNavBarStructureTest`(3곳→4곳 호출, 사진 탭이 첫 번째 분기가 되도록 순서 갱신)를 실제 구조 변경에 맞춰 함께 수정.
- **실기기 검증 완료** — 사용자가 확인함(레이아웃 세로 체크 목록 재설계 포함).

**Git 상태**: `feature/photo-sticker`, HEAD `7b3edd9`, local == origin(ahead/behind 0/0), working tree clean(`.kotlin/` 기존 untracked만). `PostcardLayoutPicker.kt`(Undo/Redo 제거 + 세로 체크 목록 재설계), `DetailScreen.kt`, `DetailViewModel.kt`, `PostcardRepository.kt`, `PostcardDao.kt`, `SaveErrorDialogStructureTest.kt`, `EditorSubcategoryNavBarStructureTest.kt`, 이 문서 갱신까지 함께 commit·push 완료.

**다음 작업**: 사진 UI 작업은 완전히 닫혔다. 다음 후보는 "배경 UI/UX 전수조사"(사용자 확정 필요) 등 이전 조사에서 남긴 항목들.

## 2026-08-28 — 56일차: 배경 UI/UX 전수조사 (코드 수정 없음, STOP)

**목표**: 배경 탭에 섞여 있는 여러 메커니즘(색상 프리셋/커스텀 색상/사진 색 추출/패턴/패턴 세기/이미지)을 선택형·생성형·이미지형·속성형으로 분류하고, 저장·복원·Undo·Preview·Export·이미지 URI 안전성을 전수조사한다. Production 코드는 수정하지 않았다.

**핵심 발견**

- **배경 색과 배경 패턴은 상호 배타 "타입"이 아니라 항상 공존하는 두 독립 레이어다.** 색(`backgroundColorArgb`)은 항상 있고, 패턴(`backgroundPattern`)은 그 위에 얹히는 선택적 오버레이(`NONE` 포함 9종)라 "타입 전환 시 이전 값 유지 여부"라는 질문 자체가 성립하지 않는다 — 둘 다 각자 컬럼에 항상 남는다.
- **패턴 색은 사용자가 고르는 값이 아니라 배경색 밝기에서 자동 계산된다**(`PostcardRenderSpec.getPatternColor()` — 밝으면 어두운 반투명, 어두우면 밝은 반투명). "패턴 색상"이라는 별도 속성은 실제로 존재하지 않는다.
- **배경 탭은 앱 전체에서 유일하게 Undo/Redo가 전혀 없는 주요 탭이다.** 색상 프리셋·기타 색상(HSV)·사진에서 색 추출·패턴·패턴 세기 슬라이더 전부 Undo 스냅샷 없음(사진/스티커/텍스트/라벨/테이프/도장/낙서는 전부 각자 Undo 스택 보유). 조작은 전부 즉시(또는 슬라이더 확정 시) Room에 직접 저장되는 구조라 draft 복원 개념도 없다(사진 탭의 레이아웃/스케일/오프셋/줌/블러와 같은 부류).
- **이미지 배경 기능은 존재하지 않는다.** `backgroundImagePath` 컬럼과 삭제 방어(`PostcardDeletionManager`, `OrphanFileDiagnostics`)는 남아 있지만 UI/코드 경로가 전혀 없다. `BackgroundColorSaveRaceTest.kt`의 코드 주석이 직접 확인해준다 — "현재 앱에서 backgroundImagePath를 non-null로 만드는 UI 경로는 없다(호출자가 없던 updateBackgroundImage/removeBackgroundImage는 dead code 정리로 이미 제거됐다)." 새로 발견한 위험이 아니라 이미 알려져 있고 한 차례 정리까지 된 완전 비활성 스키마 잔재.
- **배경만 유일하게 자유 색상 생성(HSV) 도구를 가진다.** `PostcardCustomColorPicker`(2D 채도·명도 캔버스 + 색상환 바)는 다른 어떤 꾸미기 요소에도 없는 배경 고유 문법 — 프리셋과 억지로 통일할 대상이 아니다. 커스텀 색은 "저장해서 다시 고르는 팔레트"가 아니라 마지막 색이 즉시 배경색 자체가 되는 구조.
- **패턴 타일의 카드 배경(Box)은 장식이 아니라 기능이다** — 선택 시 실제 배경색으로 채워져 색+패턴 조합을 미리 보여준다. 53일차 box-removal 파일럿 당시 이미 이 이유로 의도적으로 유지 결정된 사례(`EditorSharedControls.kt`의 `EditorFlatPresetTile` 주석: "배경 패턴처럼 카드 배경이 실제로 필요한 화면은 계속 [DecorationPresetTile을] 쓴다"). 패턴 프리셋 타일은 도장·마스킹테이프와 `DecorationPresetTile` 공유.
- 화면 미리보기·저장/공유·템플릿 썸네일이 전부 `PostcardRenderSpec.drawBackground()/drawBackgroundPattern()` 하나만 사용 — 계산 갈라질 여지 없음(AGENTS.md 13장 불변값 재확인). `backgroundImagePath`는 애초에 어디에도 전달되지 않는다.
- 사소한 기술 부채: 사진에서 추출한 색 스와치가 프리셋 색상 스와치와 똑같은 "원+점" 시각 언어를 인라인 코드로 중복 구현(공유 컴포저블 없음).
- 배경 탭 전용 UI 구조 고정 테스트(`*StructureTest.kt`류)가 하나도 없음 — 다른 탭 대비 리팩터링 회귀 안전망이 약함.

**분류(선택형/생성형/이미지형/속성형)**: 프리셋 색상=선택형, 기타 색상(HSV)=생성형+즉시선택형, 사진에서 색 추출=생성형에서 파생된 선택형, 패턴=선택형, 패턴 세기=속성형, 이미지=없음.

**수정 후보(구현 안 함, 범위별)**

1. (최소 UI 정돈) 추출색 스와치를 프리셋 색상과 같은 공유 컴포저블로 통합해 중복 코드 제거.
2. (중간 범위) 배경 조작에 Undo/Redo를 추가할지 결정 — "즉시 저장" 구조는 그대로 두고 스냅샷 스택만 얹으면 되는 사진 탭 사례와 유사한 범위가 될 수 있음.
3. (구조 변경) `backgroundImagePath` 컬럼·삭제방어 코드를 완전히 제거할지, 혹은 실제 이미지 배경 기능으로 살릴지 — 어느 쪽이든 Migration 또는 새 기능 설계 필요.
4. (구조 변경) 커스텀 색상을 "즉시 반영"이 아니라 "내 팔레트에 저장" 구조로 바꿀지.

**위험**: 데이터 손상 위험 없음(`backgroundImagePath`가 완전 비활성이라 유실될 파일도 없음). Undo 부재는 데이터 손상이 아니라 사용자 실수 복구 불가라는 UX 위험. 구조 고정 테스트 부재는 향후 리팩터링 회귀 위험.

**변경 파일**: 없음(코드 조사만, `docs/ai/HANDOFF.md`만 갱신).

**검증**: 코드 읽기·grep 기반 조사만 수행, 빌드/테스트 대상 없음.

**Git 상태**: `feature/photo-sticker`, 이번 조사로 코드 변경 없음.

**다음 작업**: 위 수정 후보 4가지 중 어느 것을 어떤 순서로 진행할지, 혹은 보류할지 사용자 확정 필요 — STOP.

## 2026-08-28 — 56일차: 배경 탭 2단 구조 개편(색상 | 패턴) + 서랍장형 UI 제거

**목표**: 전수조사에서 확인한 구조에 맞춰 배경 탭 하단을 `색상 | 패턴` 두 화면으로 분리하고, "기타 색상" 카드의 큰 둥근 외곽 container를 제거해 제목·여백 중심의 평면 구조로 재구성한다. 패턴 타일의 Box(색+패턴 조합 미리보기)는 기능적 의미가 있어 유지. Undo/Redo, `backgroundImagePath` 정리, DB Migration, 커스텀 색 팔레트 저장 기능은 이번 범위에서 제외.

**변경 파일**

- `app/src/main/java/com/postcardmemory/ui/components/PostcardBackgroundPicker.kt`
- `app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt`
- `app/src/test/java/com/postcardmemory/ui/detail/EditorSubcategoryNavBarStructureTest.kt`

**핵심 변경**

- 하단 고정 subcategory nav에 `BACKGROUND_TAB_PAGE_INDEX`(=1) 분기를 추가해 `색상 | 패턴`을 표시(기존 사진/스티커/마스킹테이프/낙서와 같은 `EditorSubcategoryNavBar` 재사용, 화면 로컬 상태 `backgroundSubTabIndex`).
- **색상 화면**: 배경 색상 프리셋 12개, "직접 고르기"(HSV 커스텀 색상), "사진에서 색 가져오기" + 추출색 스와치를 배치.
- **패턴 화면**: 배경 패턴 9종 + "패턴 세기" 슬라이더를 배치.
- **서랍장형 UI 제거**: `PostcardCustomColorPicker`("기타 색상")를 감싸던 `.background(BrutalWhite, RoundedCornerShape(16.dp)).padding(16.dp)` 카드 배경을 제거하고 평면 Column으로 변경. DetailScreen.kt에서 이를 감싸던 불필요한 `Box(fillMaxWidth().padding(top=8.dp))` 래퍼도 제거하고 padding을 `PostcardCustomColorPicker` 자신의 modifier로 옮김.
- **패턴 타일 Box는 그대로 유지**: `PostcardBackgroundPatternPicker`/`DecorationPresetTile` 자체는 손대지 않음 — 선택 시 실제 배경색으로 채워지는 기능적 미리보기이기 때문.
- **추출색 스와치 중복 제거**: 신규 공용 컴포저블 `BackgroundColorSwatch`(원형 스와치 30dp + 선택 점 5dp)를 만들어 프리셋 색상(`PostcardBackgroundColorPicker`)과 사진 추출색(DetailScreen.kt) 양쪽에서 재사용 — 기존에 인라인으로 중복 구현되던 코드 제거(시각적으로는 32dp→30dp로 거의 차이 없음).
- Undo/Redo는 추가하지 않음(DetailViewModel.kt 무변경), `backgroundImagePath`/DB/Migration/커스텀 팔레트 저장 기능도 손대지 않음.

**정적 확인 결과**

- `git status` — 위 3개 파일만 변경, `DetailViewModel.kt`/`Postcard.kt`/`PostcardDao.kt`/`PostcardRepository.kt`/`PostcardDatabase.kt` 전부 미변경 확인(저장 구조·Migration·Undo 영향 없음).
- `PostcardBackgroundPatternPicker` 함수 본문 diff 없음 — 패턴 타일 Box 보존 확인.
- `PostcardCustomColorPickerTest.kt`(순수 로직 `shouldResyncCustomColorHsv`/`shouldEmitCustomColor` 테스트)는 이번 변경(카드 배경 제거)과 무관해 영향 없음.

**검증 방법과 결과**

- `gradle compileDebugKotlin` — BUILD SUCCESSFUL(무관한 기존 경고만 남음).
- `gradle testDebugUnitTest` — 전체 통과. `EditorSubcategoryNavBarStructureTest`를 4곳→5곳 호출(사진→배경→스티커→마스킹테이프→낙서 순서)로 갱신.
- **미실행**: 실기기 검증 필요 — ①배경 탭 하단 `색상 | 패턴` 표시 ②색상 화면에 프리셋/직접 고르기/사진에서 색 가져오기 정상 동작 ③"기타 색상"이 카드 배경 없이 평면적으로 보이는지 ④패턴 화면에 패턴 9종(카드 배경 유지) + 패턴 세기 슬라이더 정상 동작 ⑤저장/재진입 복원 ⑥export/공유 결과 일치.

**Git 상태**: `feature/photo-sticker`. 위 3개 파일 unstaged 수정 상태, 아직 commit 안 함(실기기 확인 전).

**다음 작업**: 실기기 검증 → 문제 없으면 commit/push 승인 요청. 이후 별도 작업 단위로 배경 Undo/Redo 추가 여부를 진행할 수 있다.

## 2026-08-28 — 56일차: 배경 UI 2차 정돈 — 패턴 카드 제거 + 직접 고르기 Dialog 전환

**목표**: 1차 개편(색상 | 패턴 2단 구조) 이후 남아 있던 두 UI 문제만 최소 범위로 정리한다 — ①패턴 선택 타일을 감싸던 네모 카드 제거 ②"직접 고르기"의 인라인 펼침형 HSV 팔레트를 별도 Dialog로 분리. 저장 구조·DB·RenderSpec·Undo/Redo는 변경하지 않는다.

**변경 파일**

- `app/src/main/java/com/postcardmemory/ui/components/PostcardBackgroundPicker.kt`
- `app/src/main/java/com/postcardmemory/ui/components/EditorSharedControls.kt`
- `app/src/main/java/com/postcardmemory/ui/detail/DetailScreen.kt`
- `app/src/test/java/com/postcardmemory/ui/detail/StickerItemFlatBoxRemovalStructureTest.kt` (주석 갱신)
- `app/src/test/java/com/postcardmemory/ui/detail/BackgroundPatternFlatBoxRemovalStructureTest.kt` (신규)

**핵심 변경**

- **패턴 카드 제거**: `PostcardBackgroundPatternPicker`가 카드형 `DecorationPresetTile` 대신 스티커/텍스트/라벨과 같은 평면형 `EditorFlatPresetTile`을 쓰도록 교체 — 기호(symbol)+이름(label)+선택 밑줄만 남고 카드 배경·둥근 Box 없음. `EditorFlatPresetTile`에 선택적 `label: String? = null` 파라미터를 새로 추가해 재사용했다(기존 5개 호출부는 기본값 `null`이라 동작 변화 없음). 선택 상태는 기존 앱 문법(선택 밑줄, SunsetGold)에 더해 기호 색 강조(선택 시 SunsetGold)로 표현.
- 카드 배경이 하던 "선택 시 실제 배경색으로 채워 색+패턴 조합을 미리 보여주는" 기능은 이번 정돈으로 의도적으로 빠졌다(지시서 목표 UI에 해당 기능이 없음) — `PostcardBackgroundPatternPicker`의 `selectedColorArgb` 파라미터가 완전히 불필요해져 함께 제거(단일 호출부인 DetailScreen.kt도 갱신).
- **직접 고르기 Dialog 전환**: 색상 탭 본문에서 `AnimatedVisibility` + 인라인 `PostcardCustomColorPicker`를 제거하고, "직접 고르기" 버튼이 `showCustomColorDialog`(구 `customColorDrawerExpanded`) 상태로 별도 `AlertDialog`를 열도록 변경. Dialog 안에는 `PostcardCustomColorPicker`를 그대로(계산 로직·HSV UI 전혀 수정 없이) 배치, "닫기" 버튼 하나만 추가. 별도 헤더 title 없이 picker 자신의 "기타 색상" 텍스트가 헤더 역할을 하도록 둬 중복 헤더를 피함.
- **저장 정책 유지(STOP 회피)**: Dialog로 옮기면서도 HSV 드래그 → `onColorSelected` → `viewModel.updateBackgroundColor()` 즉시 반영/저장 흐름을 그대로 유지 — local draft/적용/취소 개념을 새로 만들지 않았다. 따라서 12번 STOP 조건(적용/취소 의미 재정의 필요)에 해당하지 않아 STOP 없이 진행했다.
- **구조 테스트 추가**: `BackgroundPatternFlatBoxRemovalStructureTest`(신규) — 배경 패턴이 더 이상 `DecorationPresetTile`을 호출하지 않는지, `EditorFlatPresetTile`에 `label`을 전달하는지, 색상 탭 본문에 `PostcardCustomColorPicker`/`AnimatedVisibility`가 남아있지 않은지, `showCustomColorDialog` 게이트 안에 `AlertDialog`가 있는지를 소스 텍스트 기준으로 고정. 기존 `StickerItemFlatBoxRemovalStructureTest`의 "배경 패턴은 계속 DecorationPresetTile을 쓴다"는 전제 주석도 이번 변경에 맞춰 갱신.

**정적 확인 결과**

- `git status` — 위 파일들만 변경, `Postcard.kt`/`PostcardDao.kt`/`PostcardRepository.kt`/`PostcardDatabase.kt`/`PostcardRenderSpec.kt`/`DetailViewModel.kt` 전부 미변경 확인(데이터·렌더·Undo 영향 없음).
- `DecorationPresetTile`은 도장(`PostcardSealDetailScreen.kt`)·마스킹테이프(`MaskingTapeDetailScreen.kt`)에서 계속 사용 — 공용 컴포넌트 자체는 건드리지 않음, 배경 사용처만 분리했다.

**검증 방법과 결과**

- `gradle compileDebugKotlin` — BUILD SUCCESSFUL(무관한 기존 경고만 남음).
- `gradle testDebugUnitTest` — 498 tests / failures 0 / errors 0(신규 테스트 4건 포함, 기존 회귀 없음).
- **실기기 검증 완료** — 사용자가 확인함: 패턴 탭에서 카드 없이 기호+이름+밑줄만 보이는 것, 9종 패턴 식별, 선택 상태 명확성, "직접 고르기" Dialog 전환, Dialog 안 HSV 조작 즉시 반영, Dialog 닫기 후 화면 정상, 기존 색상/패턴/패턴 세기 동작, 저장/재진입 복원 모두 정상 확인.

**남은 Undo/Redo 과제**: 이번 작업에서 추가하지 않음(지시대로 보류) — 배경 색상/패턴/세기/HSV Undo는 UI 개편이 완전히 닫힌 뒤 별도 독립 작업으로 진행.

**Git 상태**: `feature/photo-sticker`, HEAD `8258503`("Remove background pattern card and move custom color to a dialog")로 commit·push 완료. local == origin(ahead/behind 0/0), working tree clean(`.kotlin/` 기존 untracked만).

**다음 작업**: 배경 UI 작업은 완전히 닫혔다. 다음 후보는 배경 Undo/Redo 추가 여부 등 이전 조사에서 남긴 항목들.

## 2026-08-29 — 57일차 선행 작업: IDE inspection dead template runtime 정리

**사용자 관점 요약**: 56일차에 화면에서 제거된 템플릿 기능이 상세 화면 뒤에서 계속 상태와 저장 Job을 만들고 사용자 템플릿 파일을 읽던 경로를 제거했다. 앱 화면과 기존 엽서 동작은 바꾸지 않았고, 기존 사용자 템플릿 파일·저장 형식·Room 데이터도 삭제하거나 변환하지 않았다.

**제거한 dead runtime**

- `DetailViewModel`의 template 적용·Undo/Redo 상태와 공개 StateFlow: `canUndoTemplateStyle`, `canRedoTemplateStyle`, `lastAppliedTemplateId` 및 대응 private state·history stack·snapshot.
- template 적용·복원 runtime: `applyTemplate`, `undoTemplateStyleChange`, `redoTemplateStyleChange`, `persistTemplateStyle`와 직접 고아가 된 private helper·상수·`templateStyleSaveJob`.
- 사용자 template 관리 runtime: `userTemplates`, `templateSaveState`, `templateManageState`, `loadUserTemplates`, 이름 추천·중복 확인·상태 reset·신규 저장·이름 변경·덮어쓰기·삭제 함수와 직접 고아가 된 미리보기 helper·Job·import.
- `loadPostcard()`에서 template history 초기화와 `loadUserTemplates()` 호출을 제거해, 상세 화면 진입 때 더 이상 사용되지 않는 사용자 template 파일을 읽지 않게 했다.
- `awaitPendingStyleSaves()`에서 template 적용·저장·관리 Job을 제거하고 현재 살아 있는 저장 Job만 기다리도록 주석과 목록을 정리했다.
- 삭제된 production runtime을 그대로 복제하던 `TemplateStyleSaveRollbackTest.kt`를 삭제하고, `StyleSaveRaceTest`·`DetailScreenExitSaveGuaranteeTest`·`DetailScreenExitSaveLossTest` 안의 template 전용 fake state·case를 제거했다. 현재 살아 있는 개별 저장 경합·화면 이탈 검증은 유지했다. 이 cleanup으로 dead runtime만 검증하던 테스트 22개가 전체 테스트 수에서 빠졌다.

**유지한 persistence와 테스트**

- `PostcardTemplateStorage`, `PostcardTemplate`, `PostcardTemplateStyle`, `BuiltInTemplates`, template UI helper 파일, 사용자 template 저장 파일과 serialization 형식은 그대로 보존했다.
- Room Entity·DAO·schema·Migration, 기존 엽서와 저장 파일 구조는 전혀 수정하지 않았다.
- `PostcardTemplateStorageTest`와 template 모델·legacy 호환 테스트는 유지했다. 혼합 저장 경합·화면 이탈 테스트에서는 template 전용 부분만 제거하고 현재 살아 있는 개별 저장 계약을 검증하는 case는 그대로 보존했다.

**typo inspection 판정**

- 실제 rename 없음.
- `removedBgUri`: `PhotoStickerItem`의 저장·복원 필드와 여러 production 경로에서 일관되게 쓰는 `background` 약어다. 전역 rename은 코드 의미나 가독성 개선보다 범위만 키우므로 spell checker false positive로 판정했다.
- `Snackbar`: Android/Compose의 정상 API·용어라 false positive로 판정했다.
- `uACBD`, `uACFC`: 한글 오류 문장의 `\\uBC30\\uACBD`, `\\uC81C\\uACFC` Unicode escape 내부 조각이다. 문자열 의미를 바꾸지 않고 false positive로 판정했다.
- IDE dictionary·suppression은 저장소에 추가하지 않았다.

**검증**

- 후보 production 심볼과 template Job·`loadUserTemplates()` 재검색 — `DetailViewModel` 잔여 0. `PostcardTemplate.kt`의 `lastAppliedTemplateId` 파라미터 helper는 별도 top-level 모델 로직이라 이번 범위에서 유지.
- 관련 테스트 6개 클래스(`StyleSaveRaceTest`, `DetailScreenExitSaveGuaranteeTest`, `DetailScreenExitSaveLossTest`, `PostcardTemplateTest`, `BuiltInTemplatesTest`, `PostcardTemplateStorageTest`) — BUILD SUCCESSFUL.
- `:app:compileDebugKotlin` — BUILD SUCCESSFUL. 이번 변경 관련 신규 경고 없음. 기존 Migration 파라미터명·deprecated API 경고는 범위 밖이라 유지.
- `:app:testDebugUnitTest` — 최종 상태에서 51 suites / 476 tests / failures 0 / errors 0 / skipped 0.
- `git diff --check` — 이상 없음.
- 첫 sandbox 실행은 Foojay plugin을 해석하지 못해 코드 검증 전에 실패했다. 네트워크 권한을 허용한 재실행에서는 plugin 해석 후 compile·test까지 정상 완료했으므로 코드 실패가 아닌 실행환경 실패로 분류했다.
- 실기기 검증 미실행 — UI와 사용자 동작을 바꾸지 않는 dead runtime cleanup이며 자동 compile·test까지만 확인했다.

**변경 파일**

- `app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt`
- `app/src/test/java/com/postcardmemory/ui/detail/StyleSaveRaceTest.kt`
- `app/src/test/java/com/postcardmemory/ui/detail/DetailScreenExitSaveGuaranteeTest.kt`
- `app/src/test/java/com/postcardmemory/ui/detail/DetailScreenExitSaveLossTest.kt`
- `app/src/test/java/com/postcardmemory/ui/detail/TemplateStyleSaveRollbackTest.kt` (삭제)
- `docs/ai/HANDOFF.md` (기존 사용자 실기기 확인 기록 보존 + 이번 항목 추가)

**Git 상태**: `feature/photo-sticker`, HEAD `8258503`, local == origin(ahead/behind 0/0). 위 코드·테스트·HANDOFF 변경은 unstaged이며 commit/push하지 않았다. 기존 untracked `.claude/`, `.kotlin/`은 건드리지 않았다.

**다음 작업**: `updateMessage()` race 수정과 재발 방지 테스트를 별도 독립 작업으로 진행한다.

## 2026-08-29 — 57일차: `updateMessage()` 저장 race 수정 + production StructureTest 추가

**시작 HEAD**: `8258503`(직전 57일차 선행 작업인 IDE inspection cleanup·template dead runtime 정리가 이미 unstaged로 반영된 상태에서 시작). 이번 작업 시작 전 `git status`로 확인한 결과 unstaged 변경은 그 선행 작업분(`DetailViewModel.kt`, 관련 테스트 3개, `TemplateStyleSaveRollbackTest.kt` 삭제, `docs/ai/HANDOFF.md`)뿐이었고 충돌 없음.

**race 원인**: `updateMessage()`가 다른 style 저장 함수(`updateBackMessage`, `updateBackRecipientModifier` 등)와 달리 `styleWriteMutex`를 쓰지 않았다. 함수 호출 시점의 `currentPostcard`(전체 Postcard snapshot)를 잡아둔 채 `withContext(Dispatchers.IO)`로 Room에 저장한 뒤, 저장이 끝나고 나서 `_postcard.value = currentPostcard.copy(message = normalizedMessage)`로 **그 오래된 snapshot 전체를 되썼다**. 글귀 저장이 진행되는 동안 사용자가 다른 style(배경색·패턴·사진 크기·blur 등)을 바꾸면, 그 최신 값이 메모리에서 과거로 되돌아갈 수 있었고, 이어서 다른 저장 Job이 오염된 `_postcard.value`를 최신으로 읽으면 Room에도 과거 값이 저장될 위험이 있었다.

**적용한 기존 안전 패턴**: `updateBackMessage()`(`DetailViewModel.kt:2342` 부근)를 선례로 그대로 재사용.

- 호출 시점에 `previous`(되돌릴 이전 값)만 별도로 잡아두고, `_postcard.value`는 `currentPostcard.copy(message = normalizedMessage)`로 **즉시(낙관적) 갱신**한다 — 이건 저장 시작 전 동기 구간이라 stale 문제가 없다.
- 실제 저장은 `viewModelScope.launch { withContext(Dispatchers.IO) { styleWriteMutex.withLock { ... } } }` 안에서, mutex 획득 후 `_postcard.value`를 다시 읽은 `latest`의 `message` 필드만 `repository.updatePostcardMessage()`에 넘긴다 — 저장 시점의 실제 최신 state를 기준으로 하므로 그 사이 바뀐 다른 style 값을 덮어쓰지 않는다.
- 실패 시 `_postcard.value?.message == normalizedMessage`(즉 그 사이 아무도 message를 다시 바꾸지 않았을 때만) `previous`로 message 필드만 롤백 — 다른 필드는 건드리지 않는다.
- `messageUpdateJob`은 그대로 유지, `awaitPendingStyleSaves()`의 대기 목록(`DetailViewModel.kt:3530` 부근)도 이미 포함돼 있어 수정 불필요.
- 성공/실패 의미, 함수 시그니처, 호출부는 전혀 바꾸지 않음.

**production StructureTest**: 신규 `app/src/test/java/com/postcardmemory/ui/detail/UpdateMessageSaveMutexStructureTest.kt` — `BackgroundPatternFlatBoxRemovalStructureTest`와 같은 소스 텍스트 기준 방식(candidates 경로로 `DetailViewModel.kt` 원문을 읽어 `updateMessage()` 함수 body만 잘라 검사). 4개 테스트:

1. `updateMessage_usesStyleWriteMutex` — `styleWriteMutex.withLock` 사용 확인.
2. `updateMessage_reRedsLatestStateInsideMutex` — mutex 블록 안에서 `val latest = _postcard.value`로 재조회한 뒤 `repository.updatePostcardMessage(...)`에 `latest.message`를 넘기는지 확인(호출 시점 인자를 그대로 쓰면 실패).
3. `updateMessage_doesNotRewriteStaleFullSnapshotAfterAsyncSave` — `viewModelScope.launch` 블록(비동기 구간) 안에 `_postcard.value = currentPostcard.copy(` 형태(오래된 전체 snapshot 되쓰기)가 없는지 정규식으로 확인.
4. `updateMessage_optimisticSyncUpdateOnlyTouchesMessageField` — launch 이전 동기 구간에 `_postcard.value = currentPostcard.copy(message = normalizedMessage)` 낙관적 갱신이 있는지 확인.

세부 문체(공백·변수명)가 아니라 저장 안전 계약(mutex 사용, 최신 state 재읽기, stale snapshot 되쓰기 금지)만 검사하도록 설계했다. 기존 Fake 기반 race test(`StyleSaveRaceTest` 등)는 수정하지 않았다 — 그쪽은 안전 패턴 자체의 정합성을, 이 신규 테스트는 production 코드가 실제로 그 패턴을 쓰는지를 검증하는 보완 관계다.

**검증 방법과 결과**

- 신규 `UpdateMessageSaveMutexStructureTest` 단독 실행 — BUILD SUCCESSFUL(4개 전부 통과).
- 관련 기존 테스트(`StyleSaveRaceTest`, `DetailScreenExitSaveGuaranteeTest`, `DetailScreenExitSaveLossTest`) — BUILD SUCCESSFUL, regression 없음.
- `:app:compileDebugKotlin` — BUILD SUCCESSFUL, 신규 경고 없음.
- `:app:testDebugUnitTest`(전체) — BUILD SUCCESSFUL. JUnit XML 52개 파일 집계 기준 **480 tests / failures 0 / errors 0 / skipped 0**(직전 476 + 신규 4).
- `git diff --check` — 이상 없음(기존 LF/CRLF 경고만, 실제 whitespace 오류 없음).
- `DetailViewModel.kt` diff 전수 재검토 — `updateMessage()` 함수 하나(HANDOFF 기준 `@@ -3005,6 +2237,7` ~ `@@ -3015,26 +2248,37` 구간)만 이번 변경이고, 나머지 hunk는 전부 직전 57일차 선행 template cleanup 작업분임을 확인(새로 섞인 변경 없음).
- 실기기 검증 **미실행** — 아래 시나리오로 사용자 확인 대기.

**변경 파일**

- `app/src/main/java/com/postcardmemory/ui/detail/DetailViewModel.kt` — `updateMessage()` 최소 수정.
- `app/src/test/java/com/postcardmemory/ui/detail/UpdateMessageSaveMutexStructureTest.kt` (신규).
- `docs/ai/HANDOFF.md` (이 항목 추가).

**실기기 검증 시나리오(사용자 확인 대기)**

1. 엽서 글귀를 수정.
2. 저장 직후 빠르게 다른 style(배경색/사진 크기/blur 등) 변경.
3. 글귀 저장 완료 후 방금 바꾼 최신 style 값이 이전으로 되돌아가지 않는지 확인.
4. 화면에서 나갔다가 다시 진입해 글귀와 마지막 style 변경값이 모두 최신 상태로 복원되는지 확인.

**남은 위험**: 낮음 — 기존 검증된 mutex 패턴을 그대로 재사용한 최소 patch. 자동 테스트로는 실제 Room/코루틴 타이밍 경합까지는 재현하지 못하므로 위 실기기 시나리오 확인 전까지는 완전히 닫힌 것으로 보지 않는다.

**Git 상태**: `feature/photo-sticker`, HEAD `8258503`(변경 전과 동일, 이번 작업은 아직 commit 안 함). `DetailViewModel.kt`/테스트 3개/`TemplateStyleSaveRollbackTest.kt` 삭제(선행 작업분) + 이번 `updateMessage()` 수정 + 신규 StructureTest + 이 HANDOFF 갱신까지 전부 unstaged. commit/push **미실행**(사용자 실기기 확인 후 승인 대기).

**다음 작업**: 실기기 검증 → 문제 없으면 사용자 승인 받아 commit/push. 승인·검증까지 완전히 닫힌 뒤의 다음 독립 작업은 "사진 스티커 / 사진 마스킹테이프 URI 영속성 전수조사".
