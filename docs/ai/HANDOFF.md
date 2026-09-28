# HANDOFF — 87일차 꾸미기 손 인터랙션(사진 스티커 핀셋 붙이기)

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
