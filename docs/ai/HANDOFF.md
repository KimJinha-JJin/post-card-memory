# HANDOFF — 85일차 사진 스티커 오림 스타일 완료

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
