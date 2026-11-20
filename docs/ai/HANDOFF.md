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
