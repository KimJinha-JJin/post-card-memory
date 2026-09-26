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
