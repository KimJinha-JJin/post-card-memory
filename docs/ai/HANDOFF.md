# HANDOFF — 2026-11-24 142일차 추가: PR #39 카드 선택 접근성 보정

- 추가 지시의 선행 검증에서 일반 월별 갤러리 카드가 선택 상태를 Compose semantics로 전달하지 않는 것을 확인했어. `GalleryScreen.kt`의 일반 카드 modifier에 `selected = isSelected`만 연결했어. 시각 디자인·선택/삭제 로직·Room schema 19·다른 화면은 무변경이야.
- **로컬 자동검증 — 실행:** `testDebugUnitTest assembleDebug` 성공, XML 1,307 tests / 140 suites, failures/errors/skipped 0. JVM source `@Test` 1,307·테스트 파일 139로 수·테스트 구현은 무변경이야. 이 JVM 검사가 실제 TalkBack 발화를 보호하는 것은 아니야.
- **실기기 감각 QA — 실행:** 같은 승인 기기 SM-S936N·package·서명·schema에서 기존 승인 범위의 `adb install -r`로 보정 APK 설치 성공. 최초 설치 시각 유지, 설치 APK와 로컬 APK hash 일치. 설치 직후 내부 files 139개 경로·hash가 설치 전과 전부 같았어.
- 시스템 animator scale 1.0→0에서 앱에 새로 진입해 단일 선택·도장 최종 표시를 확인하고 1.0으로 복구했어. 140ms 정밀 프레임 측정은 사용자 추가 지시로 병합 필수 조건에서 제외됐어.
- 사용자가 TalkBack을 직접 켜서 선택한 첫 엽서와 선택하지 않은 옆 엽서를 확인한 뒤 **“선택됨/선택 안 됨이 구분돼”**라고 보고했어. 실제 음성은 사용자 확인 근거야. 기존 비파괴 QA의 1~5장 선택·해제·삭제 확인창 취소 결과와 함께 사용해. 최종 삭제·편집 저장·실기기 instrumentation은 실행하지 않았어.
- UIAutomator XML의 selected=false만으로 보정 실패를 판정하지 않아. 일반 Compose 선택 항목은 stateDescription으로 전달될 수 있고 XML dump는 그 속성을 충분히 보여주지 않아. 초기 코드의 semantics 누락과 XML 관찰의 한계를 구분해. [AndroidX 접근성 구현](https://android.googlesource.com/platform/frameworks/support/+/b8f12c706099a47e1e44d9772d03e14a4ae37146/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt)도 대조했어.
- TEST-COVERAGE-MAP에 자동 보호 범위와 실제 기기 확인을 구분해 기록했어. `Protect main` 활성·Merge commit만 허용·strict 최신 base·필수 `build-and-test`를 원격 조회로 확인했어.
- 작성 시점 PR #39 원격 head는 `08b2f03a491b6d41380ea3d7d3db9118480553b2`, base는 `7609a67f59ce7e16f804b4ea90a61f41ba9dd5b1`. 보정 commit/push 후 새 head와 CI를 보고하고 AGENTS.md 9절에 따라 최종 병합 승인을 갱신해야 해. **기존 head에서 접근성 누락이 확인됐으므로 자동 병합하지 않아.**
- 사용자 추가 지시로 기억 책갈피 A/B/C·27조합·별도 Entity·schema 20 구현은 승인됐지만, PR #39 선행 마감 전 구현 기준 브랜치를 임의로 바꾸지 않아. 책갈피 production 미착수. 목업·앞선 조사 기록은 별도 보존하고 갤러리 보정 commit에 혼합하지 않아.

---

# HANDOFF — 2026-11-23 141일차 후속: 갤러리 선택·삭제 B안(종이 도장형) 구현

피코(Claude Code), 수동 표준 모드. 반말 통일 PR #38은 main에 Merge commit `7609a67`로 병합됐고(main CI verified-merge 성공, 작업 브랜치 로컬·원격 정리 완료), 그 위에서 기존 빈 브랜치 `feature/day141-gallery-selection-ui`를 최신 main으로 맞춰 사용자가 확정한 B안을 Compose로 구현했어. 브랜치 commit/push·PR·CI까지 승인, **main 병합은 사용자 최종 승인 전 금지**.

## 현재 상태와 변경 범위

- **선택 카드(`ui/components/StampCard.kt` `StampCardContent`):** 선택되면 카드 뒤에 `PaperTray` 종이를 사방 4dp 덧대 그려(2dp 모서리, `drawBehind`라 그리드 배치·카드 크기 불변). 그리드 간격 가로 8·세로 10dp라 이웃 선택 카드와 겹치지 않아(가로는 맞닿음, 세로 2dp 틈). 사진은 가위 윤곽 안쪽만 `PaperTray` 22%로 바래고, 날짜는 `InkPrimary`·SemiBold로 강조. 기존 핑크(`BrutalCoral`) 오버레이와 14dp 점은 없앴어. 오른쪽 위 24dp 잉크 체크 도장(`InkSecondary` 원, 종이색 테두리·안쪽 얇은 링, 흰 체크, 1dp 그림자)은 선택 순간에만 140ms 0.86배·투명→1로 찍히고 회전·번짐·튕김 없음. 이미 선택된 채 스크롤로 들어온 카드와 시스템 "애니메이션 끄기"는 바로 최종 모습. 130일차 눌림·기울기, 가위 윤곽, 흰 테두리, 편지 배지는 무변경. 연못·경주 화면은 `isSelected=false`만 넘겨 영향 없음.
- **상단 선택 바(`GalleryScreen.kt`):** `N장 선택` 글자색 핑크→`InkPrimary`. 휴지통 아이콘만 있던 버튼을 아이콘+`삭제` 글자의 적갈색(`GalleryDangerRed`) 테두리·종이색 바탕·2dp 모서리 상자로(보이는 높이 36dp, 터치 48dp, 알약형 아님).
- **삭제 확인창:** 4dp 모서리 종이 창, 제목 `엽서 N장을 삭제할까?`, 지울 엽서 썸네일 44dp(갤러리 순서 앞 4장, 초과는 `+N`), 안내 `삭제하면 되돌릴 수 없어. 엽서에 담긴 사진과 꾸미기도 함께 지워져.`(실제 삭제 범위 = 앱 안 사진·꾸미기 상태·초안 파일과 DB 기록, 되돌리기 없음과 일치), 테두리 `취소` + 적갈색 채움 `N장 삭제`(2dp 모서리). 썸네일은 Coil `AsyncImage`가 44dp 표시 크기로 decode하고 4장까지만 만들어 대량 선택에도 원본 decode가 없어. 사진을 못 읽으면 종이색 빈 자리로 남아.
- 선택 상태 구조, ViewModel 선택·삭제 로직, Repository, Room schema 19, 저장 무결성, 미래 우체통, C2 천공, 140일차 펀칭, 갤러리 전체 배치는 무변경. 삭제 확인을 생략하는 경로 없음.
- 변경: `StampCard.kt`, `GalleryScreen.kt`, 신규 `GalleryDeleteDialogThumbnailsTest.kt`, `TEST-COVERAGE-MAP.md`, 이 HANDOFF. 목업 HTML은 미추적 그대로.

## 검증

- **로컬 자동검증 — 실행:** 전체 `testDebugUnitTest` 1,307건·140 suite 실패 0(신규 4건), `assembleDebug` 성공(Gradle 9.9.1, Zulu 17). 선택 개수 문구 구조 검사 통과.
- **TEST-COVERAGE-MAP — 갱신 완료:** 141일차 절 추가.
- **실사용 기기 instrumentation — 불필요**(변경 없음).
- **실기기 감각 QA — 필요·대기(기기 미연결, `adb devices` 0대):** 길게 누르기 직후, 1장·여러 장 선택, 해제, 도장 찍힘, 종이 덧댐(밝은·어두운 사진, 이웃 카드), 삭제 버튼, 확인창 썸네일·긴 문구, 취소 후 복귀. PR #38에서 미검증으로 남은 삭제 문구 표시도 함께. 실제 삭제 QA는 사용자 사전 승인 없이 하지 않아.

## 남은 위험·재개 조건

1. 실기기 QA 전에는 병합하지 않는 게 안전해(시각 결과는 자동 검사로 보호 안 됨).
2. 화면 낭독기: 카드의 선택 상태 전달은 이번에 바꾸지 않았어(별도 범위). 상단 삭제 버튼은 `Role.Button`+보이는 `삭제` 글자로 읽혀.
3. 위 141일차 반말 절의 용어·알약형 후보는 그대로 대기.

---

# HANDOFF — 2026-11-23 141일차: 안내 문구 반말 통일

피코(Claude Code), 수동 표준 모드. 사용자 작업지시서로 앱 공식 문체를 반말(해체)로 확정하고, 화면 표시 문장 중 해요체·합니다체만 반말로 바꿨어. 작업 브랜치 commit/push·PR·필수 CI까지 승인됐고 **main 병합은 사용자 최종 승인 전 금지**야. 140일차 PR #36·#37은 이미 병합돼 있어(시작 main = `bde0420`).

## 현재 상태와 변경 범위

- 브랜치 `feature/day141-voice-banmal`(origin/main `bde0420`에서 분기, 원본 폴더). 갤러리 B안(종이 도장형) 구현은 별도 후속 작업이고, 그 비교 목업 `docs/ai/mockups/gallery-selection-delete-day141.html`은 미추적으로 보존(이번 커밋 제외). B안 탐색용 빈 브랜치 `feature/day141-gallery-selection-ui`(커밋 0, 로컬 전용)가 남아 있어.
- 조사 기준(화면 표시 458곳 중 해요체 34 + 합니다체 41 = 75곳)을 다시 확인하고 74곳을 반말로 바꿨어. 문자열만 바꿨고 기능·저장·삭제·Undo·Room schema 19·UI 색/크기/배치·알약형 모양은 무변경.
  - 실패창 제목은 반말인데 본문(`exception.message`)이 합니다체로 뜨던 문제: 카메라·사진 저장·스티커/테이프 사진·이미지 내보내기 예외 문구 41곳을 `~하지 못했어.`/`~할 수 없어.`로 맞췄어.
  - 삭제: 엽서 편집 `이 엽서를 삭제할까?` / `삭제하면 되돌릴 수 없어.`, 갤러리 `엽서 N장을 삭제할까?` / `삭제하면 되돌릴 수 없어.`. 갤러리 선택·삭제 흐름은 승인된 B안 방향대로 단위를 `장`으로 바꿨어(`N장 선택`, `엽서 N장을 삭제했어.`, 부분 실패 `N장 삭제했고, N장은 삭제하지 못했어.`). 갤러리 밖 단위는 무변경.
  - 미래 우체통 보내기 창·봉인 화면, 스티커 통 빼기 창, 카메라 안내, 빈 화면, 공유 시트, 쫑쫑컵 안내도 반말로.
  - 감성 문구는 말끝만 바꾸고 유지: `수정구가 추억의 좌표를 맞추고 있어`, `M월 d일의 이야기야!`, `오늘까지 N번 만났어!`(물결표만 뺌), `N에 다시 만나자.`
- **의도적으로 남긴 존댓말 1곳:** 인트로 비밀 문구 `비개발자가 만들었어요`(`AppIntroScreen.kt`, 테스트로 고정된 제작자 서명). 사용자 판단 대기.
- 접근성 전용 문구, 명사형 버튼·라벨, 기존 반말 문구는 손대지 않았어.

## 검증

- **로컬 자동검증 — 실행:** 전체 `testDebugUnitTest` 1,303건·139 suite 실패 0, `assembleDebug` 성공(Gradle 9.9.1, Zulu 17). 테스트 기대값만 갱신(6파일: 삭제 결과 문구, 선택 개수 문구 구조 검사, 내보내기 실패 문구, 랜덤 엽서·방문 횟수 라벨, 정사각형 저장 실패 메시지).
- 문구 재스캔: 화면 표시 문장 중 존댓말은 위 1곳만 남음.
- **TEST-COVERAGE-MAP — 변경 없음:** 테스트 수·보호 범위는 같고 기대 문구만 바뀜.
- **실사용 기기 instrumentation — 불필요**(변경 없음).
- **실기기 감각 QA — 미실행(기기 미연결, `adb devices` 0대):** 바뀐 문구는 대부분 같거나 짧아. 길어진 것은 `이 엽서를 삭제할까?`(창 제목), `엽서 N장을 삭제할까?`, `엽서 N장을 삭제했어.`(토스트) 정도인데, 잘림 여부는 코드상 줄바꿈 가능한 자리라 문제없을 것으로 보지만 기기에서 확인하지 않았어.

## 남은 위험·재개 조건

1. 사용자 판단 대기 용어: 패턴/무늬, 편집/수정, 개/장(갤러리 밖), 글귀/텍스트/문구, 색/색상, 잠시 뒤/잠시 후, 엽서/포스트카드, 엽서/편지, 꾸미기/편집 내용. 이번에 바꾸지 않았어.
2. `수정구가 추억의 좌표를 맞추고 있어`(저장 중 문구)는 의미가 흐려 대안 검토 후보.
3. 알약형 UI 5곳(공유하기 버튼, 갤러리 빈 화면, 검색 결과 없음, 카메라 촬영 안내, 쫑쫑컵 안내)은 후속 디자인 후보로 유지, 이번 변경 0건.
4. 갤러리 B안 Compose 구현은 별도 승인 단계.

---

# HANDOFF — 2026-11-22 140일차 마지막: 펀칭 책상 공간감 · 펀칭 뒤 편집 화면 진입

피코(Claude Code), 수동 표준 모드. PR #36(아래 절)은 main에 Merge commit `3d56253`으로 병합됐고 main CI는 verified-merge로 성공, 작업 브랜치는 정리됐어. 사용자 추가 지시로 최신 main에서 `feature/day140-punch-desk-and-editor`를 만들어 두 가지만 바꿨어. **main 병합은 사용자 승인 전 보류.**

- **공간감(`StampPunchScreen.kt`):** 배경을 기존 종이색 중심에서 가장자리로 아주 조금 어두워지는 책상 면(radial gradient)으로 바꿨어. 매트는 평면 안쪽으로 들여 그려(둥근 모서리 1.6) 책상 위에 놓인 판으로 표현했어. 책상 높이에 앞쪽 두께 면(MAT_THICK 2.2)과 넓은·짧은 접지 그림자 두 겹을 두고, 종이가 매트에 얹힌 짧은 접촉 그림자를 더했어. 장면 전체에는 먼 쪽 가장자리가 한쪽당 3.5% 좁아지는 원근(`setPolyToPoly`)을 걸었어. 펀칭기 그림자는 가까울수록 짙고 또렷해지게 조정했고, 날이 닿을 때 C2 윤곽을 따라 눌린 그늘이 생기게 했어. 예전 무대 색 띠와 둥근 잘림은 없앴어(펀칭기가 무대 밖 책상 위로 빠져나감). C2 천공선·2.8초 시간표·장면 구성·사진 표시는 무변경.
- **펀칭 뒤 편집 화면:** 기존 `insertPostcard`가 돌려주는 Room 자동 id를 `CaptureState.Success.postcardId`로 넘겨, 펀칭이 끝나면 기존 `detail/{id}` 경로로 바로 들어가. `popUpTo("camera") { inclusive = true }`라 갤러리를 거치는 이중 전환이 없고, 편집에서 뒤로 가면 갤러리야. id가 0 이하(비정상)면 예전처럼 갤러리 복귀(`editablePostcardIdOrNull`). 복귀 게이트(저장당 1회), 저장 중·실패·취소 분기, 움직임 줄이기 600ms는 그대로라 실패·취소 때 편집 화면으로 가지 않아. 엽서는 저장 때 한 번만 insert되고 편집 화면은 그 행을 열기만 해. DetailScreen·Undo/Redo·저장 로직·Room schema 19 무변경.
- 변경: `StampPunchScreen.kt`, `StampPunchTimeline.kt`(id 판정 함수), `CameraScreen.kt`(`onOpenSavedPostcard`), `CameraViewModel.kt`(insert id 보존), `MainActivity.kt`(카메라 → 편집 이동), `StampPunchTimelineTest.kt`(+1건), 이 HANDOFF, `TEST-COVERAGE-MAP.md`.
- **로컬 자동검증 — 실행:** `StampPunchTimelineTest` 8/8, 전체 `testDebugUnitTest` 1,303건·139 suite 실패 0, `assembleDebug` 성공.
- **실기기 QA — 실행(승인된 실사용 기기, 사용자 승인 `adb install -r`, firstInstallTime 유지, 설치 전후 사진 15개 동일):** 저장 1회 → 펀칭(책상·매트 두께·접지 그림자·원근 프레임 확인) → 갤러리 없이 방금 저장한 엽서 편집 화면(같은 사진·날짜) → 뒤로가기로 갤러리, 사진 파일 +1(중복 없음), 임시 0, FATAL 없음. 사용자 육안 "둘 다 좋아".
- 미검증: 움직임 줄이기 상태에서 편집 화면 진입은 기기에서 다시 확인하지 않았어(코드상 같은 `onFinished` 경로). 회전·저장 실패 실기기 재현은 이전과 같이 미검증.

---

# HANDOFF — 2026-11-22 140일차: C안 우표 펀칭 모션 앱 구현

피코(Claude Code), 수동 표준 모드. 사용자 140일차 작업지시서로 138일차 채택 C안을 앱에 구현하고, 작업 브랜치 commit/push·PR·필수 CI 확인까지 승인됐어. **main 병합은 사용자 최종 승인 전 금지**야. 아래 139일차 이하 원문은 당시 상태로 보존했어.

## 현재 상태와 변경 범위

- 시작 local main = origin/main = `3d8fc55`(PR #35 병합). 작업 브랜치 `feature/day140-stamp-punch-motion`(원본 폴더, 새 worktree 없음). 시작 tracked/staged 변경 0개, SAFE 38개 일치, 기존 미추적 47개(무시되는 `.claude/settings.local.json` 포함) 해시 기준선 기록.
- 저장 성공(`CaptureState.Success` = 자르기 + DB insert 완료) 화면을 수정구 대기(1,100ms)에서 **C안 펀칭 → 갤러리 복귀**로 바꿨어. 첫 구현은 목업 그대로 1,900ms였고, 실기기 확인 뒤 사용자 후속 요청으로 **2,800ms**로 늘렸어(아래 속도 조정). 저장 중(`Saving`) 화면, 실패(`Error`) 문구, 취소(coroutine 취소 → 임시 파일 정리) 경로는 그대로야. 펀칭은 성공 상태에서만 그려져.
- 새 파일: `ui/camera/StampPunchTimeline.kt`(목업 `CANDS` C안·`stateAt` 직역 순수 함수 + `SuccessExitGate`), `ui/camera/StampPunchScreen.kt`(Canvas 한 장). 수정: `CameraScreen.kt`(Success 분기 교체, 1,100ms `LaunchedEffect` 제거), `CameraViewModel.kt`(`claimSuccessExit()` 추가). 테스트: `test/.../ui/camera/StampPunchTimelineTest.kt` 7건. 문서: 이 HANDOFF, `TEST-COVERAGE-MAP.md`.
- 장면: 커팅 매트 위 고정된 종이에 방금 저장한 사진이 C2 우표로 인쇄 → 펀칭기(C2 날 + C2 창 판) 하강·누름 → 1,050ms에 종이가 `시트 − C2`(Path Difference) 구멍 + 같은 C2 조각으로 바뀜 → 펀칭기 상승 → 조각만 높이 5로 들려 그림자 생김 → 정착 후 종료 정지 → 복귀. 윤곽은 `PostcardRenderSpec.createPinkingPath` 재사용(C2 재구현 없음).
- 목업과의 차이: CSS 3D(rotateX 40°·perspective)를 정사영 2D 근사(평면 y×cos40°, 높이 z는 위로 sin40°, 높이에 비례한 작은 확대)로 그렸어. SVG 흐림 대신 겹친 반투명 층 그림자(minSdk 26 하드웨어 캔버스 호환), 종이 노이즈 결 생략, 우표 그림은 실제 저장 사진(우표 크기로만 decode, 사진 로딩은 최대 400ms 대기 후 진행).
- 중복·생명주기: 복귀는 `SuccessExitGate`로 같은 저장에 한 번만 `popBackStack`. 진행 시각은 `rememberSaveable`이라 재생성 시 이어서 재생. 뒤로가기는 기존 BackHandler(Saving/Success 차단) 유지. 저장 연타는 기존 `CropReady` 검사로 무시. 복귀 직전 `resetState()`는 더 부르지 않아(카메라 미리보기가 나가는 화면에 잠깐 보이지 않게). VM은 pop 시 정리돼.
- 움직임 줄이기(`rememberSystemReducedMotion`, 애니메이터 배율 0): 보간 없이 잘려 들린 끝 장면만 600ms 보여 주고 복귀. 배율 0.5 등은 Compose 기본 배율을 따라.
- Room schema 19·Entity·DAO·저장 순서·파일 소유권 로직 무변경.

## 검증

- **속도 조정(사용자 후속 지시, 같은 브랜치·PR #36):** 장면·높이·그림자·디자인은 그대로 두고 `StampPunchTimeline`의 시간과 곡선만 바꿨어. 하강 120–950(종이에 가까울수록 감속하는 inOutCubic, 기존 inQuad), 누름 950–1,180·절단 1,050(누름 100ms 뒤 즉시 전환), 상승 1,180–1,800(천천히 떨어져 빠져나가는 inOutCubic, 기존 outQuart), 분리 확인 1,800–1,900, 들림 1,900–2,350(높이 5, outCubic 유지), 정착·종료 2,350–2,800 후 복귀. 움직임 줄이기 600ms·복귀 게이트·저장 분기 무변경.
- **속도 조정 실기기 재검증 — 실행:** 사용자 승인 `adb install -r`(firstInstallTime 유지, 설치 전후 사진 14개 동일). 저장 1회에서 PC 연속 스크린샷으로 하강(펀칭기 위), 접촉·누름(펀칭기가 종이 위), 절단 후 분리(절단선·펀칭기 빠짐), 들림(조각 그림자·구멍 속 매트)을 약 2.8초 동안 확인. 사용자 육안 "이 속도 좋아". 앱 FATAL 없음, 임시 파일 0. 확인 중 사용자가 엽서 1장을 직접 삭제했어(파일 수 변화 원인).
- **로컬 자동검증 — 실행:** 속도 조정 후 새 테스트 7/7(시간표 순서, 하강 감속·상승 출발 완만 검사 추가), 전체 `testDebugUnitTest` 1,302건·139 suite 실패 0, `assembleDebug` 성공(Gradle 9.9.1, Zulu 17).
- **실사용 기기 instrumentation — 불필요:** 추가·변경 없음.
- **실기기 감각 QA — 실행(승인된 실사용 기기 SM-S936N, 사용자 승인 `adb install -r`, firstInstallTime 유지·기존 사진 14개 보존 확인):** 정상 저장 → 펀칭 1회 → 갤러리 복귀·새 엽서 표시(사용자 "잘 됐어"). PC로 받은 연속 스크린샷에서 펀칭기 하강, 절단 후 구멍 속 매트·조각 그림자·들림 확인. 연타 + 재생 중 뒤로가기: 엽서 1장만 생성, 복귀 1회, 앱 종료 없음. 애니메이션 끔: 끝 장면 후 바로 복귀(사용자 확인), 배율 1.0 복원 확인. 5회 저장 동안 이 앱 FATAL 없음, `postcards_temp` 0개.
- **미검증·한계:** 저장 실패 화면은 안전한 재현 수단이 없어 코드 경로 확인만(기존 Error 분기 무변경). 회전 중 재생성 이어 재생은 실기기 미확인. 반복 저장 메모리는 측정 화면 상태가 달라 PSS 265MB(3회 후)·324MB(5회 후, Graphics 44→121MB)를 엄밀히 비교하지 못했어 — 누수 단정도 배제도 하지 않아. 녹화 영상(h264)은 PC 디코더가 없어 프레임 분석 미실행(세션 scratchpad, 저장소 밖).
- 테스트 중 생긴 사용자 확인 사항: 19:16 저장 엽서는 사용자가 앱에서 직접 삭제했어(파일 소실 아님). 오늘 테스트 엽서는 기기에 남아 있고 에이전트는 지우지 않았어.
- **TEST-COVERAGE-MAP — 갱신 완료:** 기존 문서 맨 위에 140일차 절 추가(JVM 테스트 파일 +1·@Test +6, 보호·공백 범위). 새 Markdown 파일은 만들지 않았어.

## 남은 위험·재개 조건

1. 사용자가 실제 모션을 다시 보고 2D 근사(우표가 가로로 눌려 보이는 원근, 매트 위아래 띠, 펀칭기 크기·그림자 진하기)를 승인해야 최종 확정이야. 조정 요청은 같은 브랜치에서 수치만 바꿔.
2. main 병합은 PR head/base·CI를 사용자가 검토·승인한 뒤에만.

---

# HANDOFF — 2026-11-21 139일차 후속: Markdown 운영 규칙 최소 정비

패코(Codex), 수동 표준 모드. 사용자 후속 작업지시서로 기존 규칙 최소 수정·기존 HANDOFF 갱신·작업 브랜치 commit/push·PR·필수 CI 확인까지 승인됐어. **main 병합은 별도 최종 승인 전 금지**야. 아래 선행 작업 원문은 당시 상태로 그대로 보존했어.

## 현재 상태와 변경 범위

- 시작 local main = origin/main = GitHub 실제 main = `ceea1b488a84d02b64f659c04f21f809524b0ffe`. PR #34는 이미 병합됐고, 그때 작업 브랜치 정리도 끝났어. 아래 원문의 미병합·commit 전 상태는 현재 상태가 아니야.
- 작업 브랜치 `docs/day139-markdown-rules`, 작성 시점 HEAD는 위 시작 SHA야. 시작 tracked/staged 변경 0개, 기존 미추적 47개와 SAFE 38개 해시 일치. 새 worktree 없음.
- 저장소 수정은 `AGENTS.md` 문서·목업 저장 위치 절의 기존 두 항목과 이 HANDOFF뿐이야. 채팅 인수인계·기존 HANDOFF 갱신·명시적 독립 파일 생성을 구분하고, 기존 문서 우선·승인된 독립 증거 보존·재승인 방지 원칙을 유지했어. 과거 문서의 사실관계는 코드·설정·Git·CI와 대조하되 확정된 사용자 결정·안전 규칙은 임의로 무효화하지 않아.
- `CLAUDE.md`와 에이전트별 실행 지침은 기존 공통 규칙 참조로 충분해 무변경. README의 CI 버전 오기는 사용자 지시대로 별도 정정 사항으로 남겼어. 37개 Markdown 재감사·새 보고서/관리 대장·파일 이동/삭제 없음.

## 외부 사용자 플러그인 — 저장소 commit 밖의 변경

- 원본 `%USERPROFILE%/plugins/post-card-memory-workflow/skills/`의 `post-card-memory-workflow/SKILL.md`, `write-project-handoff/SKILL.md`만 수정했어. 작업 루틴은 공통 구분을 참조하고 과거 사실 대조를 명확히 했어. 인수인계 스킬은 일반 요청의 채팅 제공을 기본으로 하고 별도 파일 생성은 명시적 요청/승인 범위로 한정했어.
- 설치본은 `%USERPROFILE%/.codex/plugins/cache/personal/post-card-memory-workflow/0.1.0+codex.20260927080957/skills/`야. 시작 시 위 원본과 같았고 실제 스킬 목록은 이 캐시를 가리켜. 공식 재배포·재로딩 방식은 이번 확인 범위에서 확정하지 못했으므로 **캐시·플러그인 설정·버전은 임의 수정하지 않았어. 원본 수정 완료, 설치본 적용 대기**야. 현재 세션/새 세션에 자동 반영됐다고 주장하지 않아.

## 검증·한계·재개 조건

- 여섯 요청을 문구로 대조했어: 채팅 인수인계 → 채팅, HANDOFF 갱신 → 기존 문서, 명시적 독립 보고서 → 승인 범위 내 파일, 단순 조사 → 자동 파일 없음, 과거 보고서 → 최신 사실과 구분하되 확정 결정 존중, Codex/Claude → AGENTS 공통 기준. 실제 새 세션의 자동 선택·응답 동작을 실행 검증한 것은 아니야.
- **로컬 자동검증 — 실행:** 저장소 diff/공백 검사, 외부 원본의 의도한 치환 외 변경 없음·frontmatter 보존 확인, 스킬 기본 형식·상대 참조 파일 검사. 공식 `quick_validate.py`는 Python의 PyYAML 미설치로 import 단계에서 중단됐어(test infrastructure). 검증 성공으로 표시하지 않으며 패키지 설치·환경 변경은 하지 않았어.
- **Android 빌드·실사용 기기 instrumentation·실기기 감각 QA — 불필요:** 앱·테스트·Gradle·DB·자산 무변경, 기기 조작 없음. **GitHub Actions CI — 이 문서 작성 시점 미실행:** PR 생성 뒤 실제 범위와 필수 체크 결과를 최종 완료보고에서 확인해.
- 기존 HANDOFF/archive 원문·SAFE 38개·미추적 47개를 보존해. commit 전 최종 diff·민감정보·보호 해시를 재확인하고 승인된 두 저장소 파일만 명시 stage해. 최종 SHA·PR·CI 결과는 대화 완료보고에 남기며, 정상 Git 성공만의 추가 HANDOFF commit은 만들지 않아.
- 외부 설치본 적용은 원본 수정과 구분해 보고하고, 공식 적용 방법 확인 전 캐시 직접 수정으로 우회하지 않아. main 병합은 최종 PR head/base와 CI를 사용자가 검토·승인한 뒤에만 가능해. 우표 펀칭 Compose 구현은 별도 작업이며 이번 범위가 아니야.

---

# HANDOFF — 2026-11-21 139일차 선행: 저장소 구조 점검·목업 통합

패코(Codex), 수동 표준 모드. 이번 사용자 작업지시서에 따라 추적 목업만 통합하고 문서 구조를 조사했어. 우표 펀칭 Compose 구현은 보류 상태야. **사용자가 139일차 구조 정리의 stage·commit·push·PR·필수 CI 확인을 1단계 승인했어. main 병합은 미승인**이야. 이 문서는 최종 commit 전 기록이며 실제 SHA·PR·CI 결과는 최종 완료보고에서 확인해.

## 현재 결과와 Git 출발점

- 추적 목업 17개를 `docs/ai/mockups/`로 모았어. 기존 미추적 목업 30개는 `docs/mockups/`에 그대로 있어. 로컬의 두 폴더가 모두 없어졌다는 뜻은 아니야.
- 앱 Kotlin·Compose·리소스·Room·DB·Gradle 무변경. 실기기와 사용자 엽서를 조작하지 않았어. 기존 Markdown 37개는 전부 위치를 유지했어.
- 시작 local main = origin/main = GitHub 실제 main = `53edc4fbc6a5e2e29ae339d59912755ffcd6b066`. [PR #33](https://github.com/KimJinha-JJin/post-card-memory/pull/33)은 API 재조회에서도 merged=true, merge SHA도 같았어. 아래 138일차 병합 미승인 표기는 당시 기록이야.
- 현재 브랜치 `chore/day139-repository-structure-cleanup`, HEAD는 위 SHA 그대로야. 새 worktree 없음. 추적 파일 변경은 모두 이번 작업의 unstaged 변경이야.
- 시작 tracked 393개, tracked/staged 변경 0개, 미추적 47개. 미추적 구성은 루트 설정 후보 1·.claude/ 1·.kotlin/ 3·app/ 이미지 4·docs/ai/ 보안 준비 자료 8·docs/mockups/ 30개야. 보호 기준 JSON의 38개와 기존 미추적 47개 전체의 시작/종료 SHA를 대조해 보존을 확인했어.
- ignore 열거 결과 9,589개(.gradle 26·.idea 20·app 빌드 9,540·루트 build 1·heap dump 1·local.properties 1)를 정리하지 않았어. 전역 ignore 파일은 읽기 권한 경고가 있어 이 집계를 전역 규칙까지 완전한 판정으로 확대하지 않아. 저장소 .gitignore는 확인했어.
- `git-hooks-check.sh --fix` 결과 기존 pre-commit·pre-push 활성, 설정 무변경. Git ref 쓰기 sandbox 제한으로 브랜치 생성이 한 번 실패한 뒤 승인된 권한 실행으로 생성했어. 실제 파일 이동은 브랜치 전환 뒤에 했어.

## 폴더 집계와 역할

시작 시점 추적/미추적 수이며 하위 폴더를 포함해 상위 행과 중복돼. 날짜는 해당 영역의 마지막 Git 변경일이야. 로컬 파일 복사 시각을 이력으로 취급하지 않아.

| 경로 | 역할·종류 | 추적 / 미추적 | 마지막 Git 변경 | 문제점·권고 |
|---|---|---:|---|---|
| 루트 직접 파일 | MD 3·설정 등 6 | 9 / 1 | 파일별 상이 | 운영 진입점 유지, 설정 후보 보호 |
| .githooks/ | 검사 연결 훅 2 | 2 / 0 | 2026-11-15 | 유지 |
| .github/ | workflow 1·검사 SH 4 | 5 / 0 | 2026-11-19 | 목업 실행 참조 없음, 변경 없음 |
| app/ | KT 276·PNG 33·XML 7 등 | 320 / 4 | 2026-11-18 | 구조·참조만 조사, 변경 없음 |
| gradle/ | catalog·wrapper 설정 | 2 / 0 | 2026-11-19 | 변경 없음 |
| docs/ 전체 | MD 34·HTML 14·PNG 6·Java 1 | 55 / 38 | 2026-11-20 | 역할별 안내가 유용해 |
| docs/ 직접 파일 | 8월 6일 기술 보고서 MD 4 | 4 / 0 | 2026-08-06 | 보고서 이동 후보, 현재 유지 |
| docs/ai/ 전체 | 운영·관리·역사·목업 | 42 / 8 | 2026-11-20 | 현행과 날짜별 조사 혼재 |
| docs/ai/ 직접 파일 | MD 18 | 18 / 1 | 2026-11-20 | 아래 전체 분류 참조 |
| docs/ai/archive/ | 과거 HANDOFF MD 11 | 11 / 0 | 2026-11-20 | 원문 보존, 기존 상대 링크 문제 있음 |
| docs/ai/mockups/ | HTML 12·PNG 1 → HTML 14·PNG 3 | 13 / 0 → 17 / 0 | 2026-11-20 | 신규 목업 단일 경로 |
| docs/mockups/ | 추적 시안과 로컬 실험 | 4 / 30 → 0 / 30 | 2026-11-17 | 기존 로컬 자료 보호·폴더 유지 |
| docs/qa/day136/ | QA 설명·Java2D 도구·증거 PNG 3 | 5 / 0 | 2026-11-18 | 디자인 목업과 검증 증거 구분 적절 |
| docs/ai/privacy-rewrite/ | 보안 준비 JSON·CJS 7 | 0 / 7 | 미추적 | SAFE 보호, 실행·이동·수정 없음 |
| .claude/·.kotlin/·IDE/빌드 산출물 | 로컬 설정·로그·캐시 | 별도 보호/ignore | Git 이력 해당 없음 | 수동 정리 대상 아님 |

## 목업 경로 선택과 이동

`docs/ai/mockups/`는 2026-09-15 `49654c2`부터 축적됐고 최신 138일차 `89503c4`까지 이어졌어. `docs/mockups/` 추적 자료는 126일차 `5dff634`(2026-11-08)와 135일차 `860bc72`(2026-11-17)에 추가됐어. 기능별 시안 작성 과정에서 경로 선택이 갈라진 것으로 보이며, 기존 AGENTS에는 목업 단일 저장 규칙이 없었어. 작성자 의도까지 이력만으로 단정하지 않아.

경로 전체 문자열을 포함하는 추적 파일은 AI 경로 쪽 6개(문서 4·앱 주석 2), 다른 경로 쪽 6개(문서 1·앱 주석 4·QA 도구 1)였고 상대 링크도 별도로 조사했어. **13개와 최신 펀칭 목업을 유지하고 4개만 이동**하는 AI 경로가 변경 범위와 운영 문서의 기존 `mockups/` 상대 링크 측면에서 유리해. 반대 방향은 13개와 더 많은 기존 참조를 옮겨야 해. 목업은 앱 리소스·QA 증거와 계속 구분해.

아래 파일의 접두 경로를 `docs/mockups/` → `docs/ai/mockups/`로 바꿨어. 파일명 충돌은 없었어. 이동 직후 4개 모두 SHA-256이 원본과 같았어.

| 파일 | 최종 SHA-256 | 원본 대비 |
|---|---|---|
| memory-density-paper-day126.html | `8348f242101c015c7be4ae150cbe48f6d7d73debbb4df7a355f479a7bc6b00e8` | 바이트 동일 |
| day135-stamp-edge-study/c-edge-corners.png | `55011def9b4900a646dd8269b7c088da21253d499518e2d1b6d6affd70cc3178` | 바이트 동일 |
| day135-stamp-edge-study/c-small-cards.png | `20ecfeda96a6cf18bf348c5748de399043964b70f611fd16c8934c1fc31b4f7a` | 바이트 동일 |
| day135-stamp-edge-study/index.html | `3bdc16537a8c3d5bd169ebfb422b4b26d288fd2c19e249bbfa73e3f6dc029b34` | 보고서 href 1곳만 변경 |

index.html 원본 SHA는 `db9a4386b76569426bc33c18f5107be8d795d16750f2265b0134dbb212eb3ced`야. 이동 후 보고서 링크 `../../ai/DAY-135-STAMP-EDGE-STUDY.md` → `../../DAY-135-STAMP-EDGE-STUDY.md`만 바꿨어. 역치환한 바이트 전체가 원본과 일치하는지 확인해 내용·동작 변경과 구분했어.

연결 수정: DAY-135-STAMP-EDGE-STUDY.md의 링크 3개, docs/qa/day136/README.md의 링크 1개, StampPerforationPreview.java의 입력 경로 1개, 이동 HTML의 보고서 href 1개야. Java는 PC QA 재현 도구이며 앱 코드가 아니야. 기존 QA PNG는 재생성하거나 덮어쓰지 않았어.

미추적 목업 30개는 이동하지 않았어: 단독 HTML 6개(detail-topbar-height-day125, detail-topbar-variants-day125, menu-structure-day125, future-mailbox-diorama-day127, future-mailbox-inside-day129, paper-touch-feel-day130)와 day132-gallery-shake/ 10·day132-memory-box/ 5·day132-tweezer-box/ 4·day133-film-intro/ 5개야. 앱 주석의 옛 경로 4곳은 이 미추적 자료를 가리키므로 유지했어. 빈 원래 day135 디렉터리도 강제 삭제하지 않았어.

archive의 당시 경로 문자열은 원문 그대로 남겼어. 옛 126일차 경로는 [현재 HTML](mockups/memory-density-paper-day126.html), 135일차는 [현재 HTML](mockups/day135-stamp-edge-study/index.html)·[작은 카드](mockups/day135-stamp-edge-study/c-small-cards.png)·[모서리](mockups/day135-stamp-edge-study/c-edge-corners.png)로 찾아가면 돼.

## Markdown 37개 전체 진단

**유지 28개·이동 후보 8개·추가 검토 1개**야. 이동 후보도 이번에는 위치를 유지했어. 새 문서나 docs/README.md는 만들지 않았어. 문서 수를 줄이는 것이 목적이 아니야.

| 문서 또는 그룹(개수) | 현재 위치 | 역할 | 판단 | 제안 |
|---|---|---|---|---|
| AGENTS.md, CLAUDE.md (2) | 루트 | 공통·에이전트 운영 규칙 | 유지 | 진입점 유지, AGENTS 저장 규칙만 추가 |
| README.md (1) | 루트 | 프로젝트 소개·운영 문서 진입 | 유지 | 소개 기준일과 현행 상태 구분이 이미 있어 |
| HANDOFF.md (1) | docs/ai/ | 현행 교대 장부 | 유지 | 이번 결과와 이전 원문을 분리해 보존 |
| CURRENT_TASK.md, WORK_CONTEXT.md, STATUS.md (3) | docs/ai/ | 비활성 작업판·역사 맥락 | 유지 | 비활성 표기가 명확해, 수동 모드에서 재활성화하지 않아 |
| DECISIONS.md (1) | docs/ai/ | 유효·대체된 결정 근거 | 유지 | 현행 결정 기준 역할 유지 |
| IMAGE-ASSET-STANDARD.md (1) | docs/ai/ | 이미지 제작 기술 기준 | 유지 | 적용 지침과 과거 조사 기준일이 구분돼 있어 |
| IMAGE-ASSET-MAP.md, TEST-COVERAGE-MAP.md (2) | docs/ai/ | 지속 갱신 관리 목록 | 유지 | 자산·테스트 보호 범위 무변경이라 수정 불필요 |
| CI-AUDIT-79.md, CI-AUDIT-80.md (2) | docs/ai/ | CI 도입 전후 조사·실패·성공 근거 | 이동 후보 | docs/reports/ 후보. 현재 CI 규칙으로 읽지 않아 |
| DAY-111-CALENDAR-AUDIT.md, DAY-111-STICKER-POUCH-ASSET-PLAN.md (2) | docs/ai/ | 구조 감사·디자인·QA·조건부 계획 | 유지 | 지도·목업의 직접 참조가 있어 이동 이점이 작아 |
| DAY-133-SECURITY-FOLLOWUP.md, DAY-137-SECURITY-AUDIT.md (2) | docs/ai/ | 날짜별 보안 후속·의존성 조사 | 이동 후보 | docs/reports/ 후보. 잔여 위험의 현행 HANDOFF 연결은 유지해야 해 |
| DAY-135-STAMP-EDGE-STUDY.md (1) | docs/ai/ | 테두리 구조·C1/C2/C3 설계 근거 | 유지 | 펀칭 후속 직접 근거, 이번엔 링크만 수정 |
| LETTER-DAY-100.md, LETTER-DAY-100-GABI.md, LETTER-DAY-100-PACO.md (3) | docs/ai/ | 작성자별 기념 편지 | 유지 | 기술 문서 중복이 아니며 3개만 위한 새 폴더 실익은 작아 |
| atomic-storage-overwrite-stabilization-2026-08-06.md, detail-structure-audit-2026-08-06.md, detail-structure-audit-phase2-2026-08-06.md, storage-overwrite-test-failure-investigation-2026-08-06.md (4) | docs/ | 구조 감사·분리 준비·실패 원인·안정화 연속 기록 | 이동 후보 | docs/reports/에 함께 두면 찾기 쉬워져. 상호 경로 문자열도 함께 검증해야 해 |
| HANDOFF-2026-09-13-harness.md, HANDOFF-2026-09-14-history-investigation.md, HANDOFF-2026-09-14-visit-calendar-final.md (3) | docs/ai/archive/ | 하네스·방문 정책 STOP·최종 구현 | 유지 | 당시 사고·판단·승인 증거 보존 |
| HANDOFF-through-2026-09-12.md, HANDOFF-through-2026-09-20-before-close.md, HANDOFF-through-2026-09-20.md, HANDOFF-through-2026-09-22.md (4) | docs/ai/archive/ | 누적 역사와 마감 시점 | 유지 | 날짜가 겹쳐도 기록 범위가 달라, 원문 보존 |
| HANDOFF-before-day135-2026-11-17.md (1) | docs/ai/archive/ | 84~134일차 중심 누적 기록 | 추가 검토 | 큰 파일 탐색·기존 링크 문제만 후속 검토. 축약·삭제 금지 |
| HANDOFF-before-day136-2026-11-18.md, HANDOFF-before-day137-2026-11-19.md, HANDOFF-before-day138-2026-11-20.md (3) | docs/ai/archive/ | 135~137일차 원문 | 유지 | 짧은 개별 기록과 당시 승인·미검증 보존 |
| README.md (1) | docs/qa/day136/ | C2·그림자·실기기 QA 근거 | 유지 | 시안과 실제 QA 구분 적절 |

미추적 PRIVACY-REWRITE-FINAL-PLAN.md는 추적 37개 밖의 별도 보안 준비 문서야. 관리·승인 상태 추가 검토 대상으로 식별만 했고 이동·수정하지 않았어.

### archive 크기와 중복

11개 합계 1,269,359 bytes야. 개행을 정규화한 30줄 이상 연속 동일 구간을 모든 파일 쌍에서 비교했지만 없었어. 30자 이상 비어 있지 않은 동일한 줄이 3개 이상 겹치는 쌍도 없었어. 전체 파일 해시 중복도 없어. 짧은 재서술이나 같은 사건을 참조하는 의미상 중복까지 없다는 주장은 아니야.

| archive 파일명 | bytes | 범위·보존 가치 |
|---|---:|---|
| HANDOFF-2026-09-13-harness.md | 5,469 | 하네스·사고 원문 안내 |
| HANDOFF-2026-09-14-history-investigation.md | 9,254 | 방문 이력 조사·정책 STOP |
| HANDOFF-2026-09-14-visit-calendar-final.md | 28,755 | 방문 달력 최종 구현·후속 |
| HANDOFF-through-2026-09-12.md | 421,229 | 71일차까지, 앱 제거 사고·복구 범위 포함 |
| HANDOFF-through-2026-09-20-before-close.md | 187,305 | 73~79일차 중심, 마감 전 이력 |
| HANDOFF-through-2026-09-20.md | 4,074 | 79일차 보호지도·CI 조사 마감 |
| HANDOFF-through-2026-09-22.md | 15,925 | 80~81일차 CI·실패·migration·규칙 후속 |
| HANDOFF-before-day135-2026-11-17.md | 576,415 | 84~134일차 중심 누적 원문 |
| HANDOFF-before-day136-2026-11-18.md | 5,001 | 135일차 시안·미검증·승인 경계 |
| HANDOFF-before-day137-2026-11-19.md | 11,555 | 136일차 구현·시행착오·병합 후속 |
| HANDOFF-before-day138-2026-11-20.md | 4,377 | 137일차 보안·잔여 위험 |

정리안은 원문 그대로 유지야. 큰 파일만을 이유로 삭제·요약 대체·병합·재작성할 근거가 없어. archive의 상대 링크는 당시 HANDOFF 위치 기준인 곳이 있어 현재 위치에서 깨져 있어. 이동 전 파일 링크 누락 51건 → 이동 후 50건(모두 archive), 신규 누락은 없었어. 135일차 보존본 목업 링크 1건은 이동으로 자연스럽게 정상화됐어. 나머지는 원문을 바꾸기보다 별도 길잡이에서 실제 목적지를 안내하는 후속안이 적절해.

## 검증·diff·남은 한계

- Git index 추적 수는 stage 전이라 원래 경로 393개 그대로야. 이동 대응을 적용한 작업트리도 393개로 1:1 대응해. 삭제 4개+목적지 미추적 4개는 unstaged 이동 상태이며 기존 미추적 47개와 구분해야 해.
- 기존 파일 수정 5개: AGENTS.md, DAY-135-STAMP-EDGE-STUDY.md, docs/qa/day136/README.md, docs/qa/day136/StampPerforationPreview.java, 이 HANDOFF.md. 이동 4개 중 index.html의 href 1곳만 의도적으로 변경했어.
- AGENTS에는 신규 HTML·PNG 저장 경로, 복수 파일 하위 폴더, 목업/앱 자원/QA 구분, 현행/과거 기록 구분, 새 문서 전 기존 문서 재사용 확인만 추가했어.
- **로컬 자동검증 — 실행:** 이동 직후 4개 SHA 일치, 최종 3개 동일·HTML 링크만 변경, 목업 HTML 14개 inline JavaScript 구문 검사, docs PNG 6개 헤더·청크 경계·IDAT 압축 해제, Markdown 상대 파일 링크 전후 비교, 내부 경로 검색, 보호 자료 해시, diff·민감정보 검사. 브라우저 동작·시각 재생은 미실행이라 기존 렌더링 QA와 이번 정적 검사를 구분해야 해.
- **Gradle/JVM/APK 전체 빌드 — 불필요:** 앱·빌드 입력 무변경. Java2D 도구는 입력 경로만 검증했고 재실행하지 않아 QA PNG를 보존했어.
- **실사용 기기 instrumentation — 불필요.** CONDITIONAL/FORBIDDEN은 격리 환경이 없어 실행 불가이며 이번 대상이 아니야. 기기 연결·설치·DB·파일 조작 없음.
- **GitHub Actions CI — 미실행:** 아직 commit·push·PR 없음. HTML·PNG·Java도 포함하므로 향후 docs-only 생략을 가정하지 말고 실제 CI 범위 판정과 결과를 확인해야 해.
- **실기기 감각 QA — 불필요:** 앱 결과 무변경. TEST-COVERAGE-MAP 변경 없음(앱 테스트·보호 범위 무변경).

## 후속 제안과 펀칭 재개 조건

1. 현재 위치를 유지하는 것이 기본 권고야. 실제 이동 이점이 있는 후보는 docs/ 기술 보고 4개 + CI-AUDIT 2개 + 보안 보고 2개, 총 8개를 `docs/reports/`로 모으는 안이야. 사용자 승인 전 이동하지 않아. 상호 링크·원문 경로·현행 기록 연결을 먼저 검증해야 해.
2. `docs/README.md`는 운영·보고서·목업·QA·archive 역할과 큰 파일의 날짜 범위를 안내하면 유용해. 새 목차 자체를 늘리지 않도록 보고서 이동과 함께 승인받을 후속 후보로만 남겼어. 현행 상태·규칙 본문을 복제하지 않는 방향을 권장해.
3. 미추적 목업 통합·Git 추적 추가는 별도 사용자 판단 대상이야. 새 저장 규칙이 기존 보호 자료 이동 승인을 뜻하지 않아.
4. 다음 행동은 승인된 이 구조 정리만 명시적으로 stage·commit·push하고 main 대상 별도 PR·필수 CI를 확인하는 것이야. PR·head/base SHA·실제 CI 검증 범위를 보고한 뒤 main 병합 승인 대기에서 멈춰. main 병합은 별도 2단계 승인이고 펀칭 작업과 섞지 않아.
5. 펀칭은 별도 구현 작업지시서로 재개해. 기준은 [138일차 C안](mockups/stamp-punch-motion-mockup.html), [C2 설계](DAY-135-STAMP-EDGE-STUDY.md), [136일차 QA](../qa/day136/README.md)야. C안 1,900ms·저장 성공 뒤 대기 화면 대체·갤러리 복귀 결정을 유지하고 저장 중/실패/취소, 성공 시점 동기화, 뒤로가기·연타, reduced motion, 사진 decode·메모리를 검토해. 이번에는 구현하지 않았어.

아래 138일차 원문은 그대로 보존한 역사 기록이야. 현재 Git·승인·미검증 상태는 위 139일차 절을 먼저 읽어.

---

# HANDOFF — 2026-11-20 138일차: 우표 펀칭 모션 목업 · C안 채택

피코(Claude Code), 수동 표준 모드. 사용자 138일차 작업지시서(C2 구조 조사 → 독립 HTML 비교 목업 → 브라우저 검증 → 사용자 채택)와 최종 승인(C안·(가) 적용 결정 기록, 목업 종료 표현 정리, 목업·HANDOFF·archive 한정 commit/push/PR, **main 병합 미승인**)을 수행했어. 직전 137일차 HANDOFF 원문은 [archive/HANDOFF-before-day138-2026-11-20.md](archive/HANDOFF-before-day138-2026-11-20.md)에 바이트 그대로 보존했어.

## 현재 상태

- 시작 실측: 원본 폴더 `main` = `origin/main` = `10afcf7`(PR #32 병합). 137일차 HANDOFF의 "main 병합 미승인"은 작성 시점 기록이고, 실제로는 이후 병합됐어. 미추적 파일은 기존 보호 자료 목록 그대로였어.
- 작업 브랜치 `feature/day138-stamp-punch-mockup`(원본 폴더, `10afcf7`에서 생성). 새 worktree는 만들지 않았어. 이전 세션 scratchpad의 `head-baseline-worktree`(prunable 표시)는 건드리지 않았어.
- 산출물: [stamp-punch-motion-mockup.html](mockups/stamp-punch-motion-mockup.html) 한 파일(외부 자원·CDN 없음). 판정 **138일차 완료 / C안 채택 / 실제 앱 구현 대기**.
- 앱 소스·리소스·Room·Gradle·C2 테두리는 **무변경**.

## 확정된 제품 결정(2026-11-20, 사용자)

- **C안(경쾌한 손맛형) 채택.** 전체 1,900ms 유지: 접촉 540, 절단 560, 펀칭기 상승 600–860, 분리 확인 860–1060, 우표 들림 1,060–1,360(높이 5), 정착 1,360–1,660 후 종료 정지.
- **적용 위치 (가):** 저장 후 기존 대기 화면(`TeleportWaitingScreen`, `CaptureState.Success` 뒤 1,100ms)을 펀칭 애니메이션으로 대체하고, 끝나면 **지금처럼 갤러리로 복귀**. 꾸미기 화면 진입 흐름은 바꾸지 않아.
- **길이 차이:** 기존 성공 대기 1,100ms → C안 1,900ms(+800ms). 실제 저장 완료 시점·화면 이동과의 동기화는 139일차 구현 단계에서 검토해.
- 목업 정리: 종료의 어두워짐(조명 변화)과 "꾸미기로 넘어가는 암시" 문구를 제거했어. 종료는 우표가 들린 채 멈추는 장면이고 특정 화면 이동을 암시하지 않아. 펀칭·절단·상승·들림 타이밍과 움직임은 그대로야. A/B는 비교 기록으로 남겼어.

## C2 구조 조사 요약(읽기 전용)

- 윤곽 원본: `utils/StampPerforation.kt traceStampPerforation`(단위 정사각형, 16구간·입구 48%·깊이 15%). Android 어댑터는 `utils/PostcardRenderSpec.kt createPinkingPath`(RectF 스케일 + `Path.arcTo`), Compose는 `ui/components/StampPhoto.kt PinkingPhotoShape/StampPhoto`가 같은 Path를 감싸. 흰 테두리는 같은 윤곽 2배 굵기 선을 윤곽 안으로 clip(exporter `STAMP_BORDER_WIDTH`=18 / 우표 1000).
- 목업은 `traceStampPerforation`을 같은 연산 순서로 JS로 옮긴 경로 문자열 하나(`STAMP_D`)로 구멍(시트+STAMP_D, evenodd)·조각·펀칭날·펀칭기 창·그림자·절단선을 모두 그려. 윤곽 닫힘 오차 5.5e-17.
- 진입 흐름: 갤러리 → `camera` → `CropEditorScreen`(`PinkingPhotoGuide`) → `saveCroppedPhoto` → `TeleportWaitingScreen`("우표를 정리하는 중") → `CaptureState.Success` 1100ms → `popBackStack()`으로 **갤러리 복귀**. 꾸미기(`detail/{id}`)로 바로 가는 경로는 없어. NavHost에 커스텀 전환 없음(기본값).
- 움직임 줄이기: `ui/gallery/StickerStashPouch.kt rememberSystemReducedMotion()`(ANIMATOR_DURATION_SCALE==0), `DecorationLiftFeel.kt` 등이 사용.

## A/B/C 시간표(ms, HTML `CANDS` 값과 동일)

| 시안 | 전체 | 하강 | 접촉 | 절단 | 펀칭기 상승 | 분리 확인 | 우표 들림 | 들림 높이 |
|---|---:|---|---:|---:|---|---|---|---:|
| A 정석형 | 2600 | 300–900 | 900 | 900 | 980–1480 | 1480–1780 | 1780–2280 | 8 |
| B 분리 강조형 | 3000 | 300–1000 | 1000 | 1220 | 1260–1860 | 1860–2200 | 2200–2700 | 14 |
| C 경쾌한 손맛형 | 1900 | 250–540 | 540 | 560 | 600–860 | 860–1060 | 1060–1360 | 5 |

## 검증

- **브라우저 시각 검증 — 실행:** Chrome 확장 미연결이라 로컬 Edge headless + DevTools 프로토콜로 조작·캡처(임시 프로필·캡처는 세션 scratchpad, 저장소 밖). 5개 장면 정지, 재생 중 시간 진행, 일시정지 후 정지 유지·이어서 재생, 반복/반복 해제 시 종료 정지, 후보별 단독 재생, 움직임 줄이기 3상태 전환, 390px(가로 스크롤 없음)·1280·1700px, 스크립트 오류 0건 확인.
- **구멍·조각 일치 픽셀 비교 — 실행:** 펀칭기를 숨긴 같은 조건에서 "절단 전 한 장" vs "절단 후 조각 제자리(절단선 숨김)"를 DPR2로 비교. 차이는 우표 윤곽·그림 경계선의 안티에일리어싱 띠뿐이고 면 영역 차이는 없었어(변경 픽셀 1.6%, 모두 선 위).
- **미검증:** 실제 사람 눈의 60fps 체감, 저사양 기기 성능, `prefers-reduced-motion` OS 설정 자체(체크박스 시뮬레이션만 확인), Android Compose 픽셀 일치.
- 로컬 Gradle/JVM — 불필요(앱 무변경). 실사용 기기 instrumentation — 불필요. 실기기 감각 QA — 불필요(HTML 목업). TEST-COVERAGE-MAP — 변경 없음.
- **종료 표현 수정 후 재검증 — 실행:** 같은 방식으로 윤곽 자기 점검(STAMP_D 81곳 일치·닫힘 오차 5.5e-17), C안 시간표·단계 표시, 종료 장면(어두워짐 없음), 일시정지 유지·이어서 재생·반복, 움직임 줄이기 3상태, 390px 가로 스크롤 없음, 구멍·조각 픽셀 비교(면 영역 차이 없음), 스크립트 오류 0건을 다시 확인했어.
- SAFE 보호 기준 `protected-baseline.json` 38개 SHA-256 시작·수정 후 모두 일치.

## 남은 위험·후속 후보(승인 전 미실행)

1. 139일차 실제 구현은 별도 작업지시서 대기. 검토할 것: 저장 중(`CaptureState.Saving`) 화면과 펀칭 시작 시점, 저장 실패·취소 때 펀칭을 보여주지 않기, 1,900ms 동안 뒤로가기·연타 처리, 움직임 줄이기에서의 짧은 대체 표시, 실제 촬영 사진을 우표 면으로 쓸 때의 decode·메모리.
2. Compose 구현 예상: 기존 `createPinkingPath` 재사용(구멍은 `Path.op(DIFFERENCE)` 또는 evenodd), `graphicsLayer` 기반 들림, 움직임 줄이기 시 즉시 종료 상태. 3D 원근은 목업 연출이라 앱에서는 2D 근사가 필요할 수 있어.

## Git 출발점

- 변경 파일: `docs/ai/mockups/stamp-punch-motion-mockup.html`(신규), `docs/ai/HANDOFF.md`, `docs/ai/archive/HANDOFF-before-day138-2026-11-20.md`(신규, 137일차 원문 — 사용자 승인으로 포함).
- 1단계 승인 범위: 위 세 파일 명시적 stage → commit → push → PR → 필수 CI 확인. 이 HANDOFF는 commit 전 작성이라 SHA·PR 번호·CI 결과는 대화 완료보고와 PR 기준으로 확인해.
- **main 병합은 미승인.** 성공 기록만을 위한 추가 HANDOFF commit은 만들지 않아.
