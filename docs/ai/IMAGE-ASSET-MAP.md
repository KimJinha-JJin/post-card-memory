# IMAGE-ASSET-MAP — 앱 이미지 자원 지도

조사일: 2026-10-14(103일차), 기준 commit `d9d7a0a`(`feature/photo-sticker`). 조사 + 문서만 했고 이미지·코드는 바꾸지 않았다.
**104일차(2026-10-15) 갱신:** 그림 11장(cutout 7, 시계 3, 달력 리본 1)의 원본 해상도를 줄였다. 103일차 값은 지우지 않고 Before로 남겼으며, 바뀐 행에는 `→ 104일차` 값을 덧붙였다. 결과와 실측 Before/After는 **9절**에 있다. 1절 요약 통계는 103일차 기준이고, 104일차 합계는 9절에 있다.
이 문서는 "이 그림 파일은 무엇이고, 어디서 어떻게 쓰이며, 바꾸면 무엇이 같이 바뀌는가"를 찾는 지도다. 실제 상태는 항상 코드와 Git이 기준이며, 자산·코드가 바뀌면 해당 행을 다시 실측한다.
**105일차 마지막(2026-10-16) 갱신:** 아래 첫 절의 21장이 "실사 인쇄물 오림 질감" 사용자 제작 자원으로 교체됐다. 이 절이 해당 21장의 **현재 값**이며, 아래 기존 절의 같은 파일 행(크기·좌표·bbox·앵커)은 역사 값이다. 질감 정의 자체는 이 문서가 아니라 IMAGE-ASSET-STANDARD/workflow 몫이다.
**107일차(2026-10-18) 갱신:** 전수 재감사 + 실기기 기준선 + `gallery_date_paper_strip` 1장 절반 축소. 바로 아래 절이 현재 값이며, 아래 기존 절의 띠 행(2172×724, crop 89,172/2011×379)은 역사 값이다.

## 107일차 — 전수 재감사 · 실기기 기준선 · 월 라벨 띠 축소 (2026-10-18, 사용자 화면 QA 완료)

### 재감사 (34장: tracked 29, 보호 untracked 5)

- 105일차 21장·기존 최적화 3장(donggeuli 480², tape_press 1024², editor_label_peek 1254² 보류)의 현재 규격은 위 105일차 절과 일치.
- 450dpi(2.8125x) 기준 원본/표시 배율: 바로가기 손 1.42x, 시계 3장 약 1.52x, 집는 손 약 1.2x(엽서 0.62W × 최대 1.6), 도장·핀셋 손 1.49x, 테이프 손 1.25x, 동글이 약 2.0x, 종이 타일 1.16x, 달력 종이 약 1.3x → 유지(A).
- B(오늘 제외, 사용자 결정): `postcard_app_icon_hand_envelope`(APK만 약 −1.5MB 예상), `seal_stamp_hand`·`sticker_tweezer_hand`(APK만 약 −1MB, 연출 중에만 RAM). D(보류): `editor_label_peek`(crop·sample 결합), 도장 실루엣 4장(exporter 결합).
- **메타데이터:** 5장(gallery_paper_tile, visit_calendar_paper, gallery_date_paper_strip(축소 전), gallery_pick_hand, app icon)에 C2PA `caBX` chunk 장당 약 22~24KB가 있으나, release APK 안에서는 aapt crunch가 이미 제거해 0B. 정리해도 APK 이득 0.

### 바꾼 것 — gallery_date_paper_strip

| 항목 | 이전 | 이후 |
|---|---|---|
| 자산 | 2172×724 RGBA, 1,208,965 B | **1086×362 RGBA, 300,671 B** |
| 종이 영역(alpha > 16, 새 자산에서 실측) | (89,172)/2011×379 | **(44,86)/1006×189**(정규화 오차 ≤0.0015) |
| 갤러리 월 라벨 | 원본 decode 6,142.7 KiB, `GALLERY_DATE_STRIP_SRC_*` | 원본 decode 1,535.7 KiB, 상수 위 값 |
| 편집 하단 선택줄 | sample 4 → 543×181, crop (22,43)/502×94 | **sample 2** → 543×181(같음), crop (22,43)/503×94 |

- 축소: System.Drawing PArgb + HighQualityBicubic + TileFlipXY, 캔버스 전체 정비례(crop·tint·리터칭 없음). alpha 0~255 유지, 불투명 평균 휘도 203.9→203.3, 반투명 가장자리 휘도 221→223(어두운 테두리 없음).
- 표시: 월 라벨 상자 최소 136dp × 약 30dp(약 383~450×85px) 대비 crop 1006×189 = 약 2.2x 여유. 갈색 colorMatrix 공유·뒤집기·위치 코드 무변경.
- 사용자 실기기 화면 QA: 갤러리 월 라벨, 편집 선택줄 라벨 "전이랑 똑같다" 확인.

### APK (로컬 release, 보호 untracked PNG 포함 같은 조건)

- 40,599,253 B(10-16 빌드) → **39,730,981 B(−868,272 B)**. APK 안 띠 항목 1,127,315 → 259,044 B(−868,271 B)와 1B 차이라 감소는 이 자산 몫. 비교 전 빌드는 106일차 Kotlin 변경 전(같은 HEAD 재빌드 아님).

### 실기기 메모리 Before / After (SM-S936N 450dpi, 같은 경로·방식)

- 방식: 3초 간격 `dumpsys meminfo`, 사용자 marker, marker 후 6초 버린 중앙값, 2바퀴. 경로 = 103·104일차 경로 + **동글이 쉼터** 구간. 읽기 전용 adb + 사용자 승인 `install -r` 1회(firstInstallTime 10-05 유지).
- Before: 106일차 debug(`a3bf24d`), PID 8539 유지, 샘플 180. After: 107일차 작업트리 debug, PID 26964 유지, 샘플 142(화면 체류가 약간 짧았음). 두 측정 모두 Activities 1, 재시작 없음. After 1바퀴 도장 패널 뒤 선택줄 QA를 잠깐 거침(편집 구간, Bitmap 영향 없음).

| 구간 (Bitmap malloced, KiB 정확값) | Before | After | 차이 |
|---|---|---|---|
| 갤러리 시작 | 37 / 31,516 | 39 / 26,909 | **−4,607 KiB(−4.50MB)** |
| 편집 화면 / 도장 패널 | 13,771 / 13,785 | 13,771 / 13,785 | 0 |
| 갤러리 복귀(정적 캐시 포함) | 55 / 34,563 | 55 / 29,956 | **−4,607 KiB** |
| 동글이 쉼터 | 57 / 35,463 | 57 / 30,856 | **−4,607 KiB** |

- 계산 예상(6,142.7 − 1,535.7 = 4,607.0 KiB)과 정확히 일치. 편집 Bitmap 무변화(선택줄 decode 크기 동일)도 의도대로.
- 동글이 쉼터: 갤러리 대비 +900 KiB = 480² 한 장 decode(10마리 공유). 추가 최적화 불필요.
- 보조 지표(중앙값, 1바퀴/2바퀴, MB): 갤러리 시작 Native Heap Alloc 55.0→52.3, 갤러리 복귀 62.4/66.4→59.4/60.3. Graphics(86.5→93.1 등)·TOTAL PSS(249.5→258.0 등)는 GPU 캐시 변동이 커서 개선/악화 판단에 쓰지 않음.
- 반복: 두 측정 모두 1·2바퀴 같은 구간의 Bitmap이 같아 누수성 증가 없음. 구간 최댓값(Bitmap 38.1 / 39.7MB, Graphics 182.2 / 183.6MB)은 화면 전환 순간 1~2 샘플이며 다음 샘플에서 회복.
- 남은 갤러리 상주 큰 그림: 종이 타일 6.0MB, 달력 종이 6.0MB(둘 다 적정 배율), 바로가기 손 5장 9.9MB, 시계 2.8MB.

## 105일차 마지막 — 실사 인쇄물 오림 질감 21장 교체 (2026-10-16, 사용자 실기기 QA 완료)

사용자가 생성한 이미지를 같은 파일명으로 교체했다. 작업자는 캔버스 정렬·비례 리사이즈·회전(스누즈 손 1장)만 했고 질감·형태는 리터칭하지 않았다. 모두 RGBA·실제 alpha·`drawable-nodpi`. 사용자 원본은 저장소에 보관하지 않는다.

| 파일 | 현재 크기 / B | 사용처 | 정렬·결합(현재) |
|---|---|---|---|
| editor_cutout_photo·sticker·stamp | 314² / 124,065·97,000·88,745 | 편집 상위 기능줄 | 비례 축소. 로더 alpha>16 bbox crop + 32×24dp fit(좌표 결합 없음) |
| editor_cutout_background·tape·doodle | 314×236 / 90,051·69,021·37,316 | 〃 | 〃 |
| editor_cutout_text | 314×209 / 41,428 | 〃 | 〃 |
| editor_label_peek | 1254² / 836,646 | 하단 세부 선택줄 빠끔이 | ×0.678로 그림을 기존 src crop (376,352)/720×656 안, 앞발 바닥 y1006에 맞춤. sample 4·crop 상수 유지 |
| quick_select_camera/letter/river/sheep/checker_hand | 720² / 393,675·362,300·407,805·400,446·336,896 | 갤러리 바로가기 부채 | 손바닥 위 사진 카드 구도. 카드 폭 ≈520 논리px로 정규화. 블록 좌표(1254 논리)는 `GalleryQuickSelectFan.kt` enum: CAMERA 189/402/703/759, FUTURE_MAILBOX 243/474/763/799, POND 212/430/733/803, SHEEP_RANCH(동글이 쉼터, 새집 사진) 240/454/763/810, RACE 232/432/752/801 |
| sticker_tweezer_hand | 1254² / 1,249,581 | 사진·글귀·라벨 핀셋 연출 | 이동만. 집게 사이를 기존 ANCHOR (0.297,0.045)에 |
| tape_press_hand | **1024² / 1,125,904** (최종 다이어트, 이전 1254² / 1,724,028) | 테이프 누르기 | 이동만. 손가락 띠 중심을 기존 ANCHOR (0.399,0.259)에. 다이어트는 캔버스 전체 정비례 축소, 정규화 중심 (0.4003,0.2616)→(0.4004,0.2607). 최대 표시 ≈819px(260dp×1.12) 대비 1.25x |
| seal_stamp_hand | 1254² / 1,905,647 | 도장 연출 + 앱 인트로 | ×1.4. 손잡이가 주먹에 들어가는 점을 기존 ANCHOR (0.459,0.431)에 |
| gallery_pick_hand | 1254² / 1,357,720 | 흔들기 랜덤 엽서 | 픽셀 무변경. 아래에서 올라와 검지·엄지로 집는 자세. `RANDOM_OVERLAY_HAND_ANCHOR` **0.355/0.152**(집는 점 445,190) |
| home_clock_collage_body | 720×394 / 377,197 | 갤러리 레트로 시계 몸체 | 비례 축소. 밝은 창 원본(1697×927) x104~1595, y267~730 → `GalleryRetroClock.kt` WINDOW START 10.3 / TOP 26.5 / 147.6×46dp. 스누즈 버튼 윗면 y≈10% |
| home_clock_left_snooze_hand | 480×360 / 97,809 | 시계 스누즈 손 | 손끝 기준 −15° 회전. 장면 상자(122dp, clipToBounds) 윗변에서 팔이 잘리지 않고 화면 왼쪽 끝으로 빠지게 함. 손끝 = 손 canvas (0.960,0.534) |
| home_right_hand_coffee_cup | 450×600 / 232,834 | 시계 옆 커피 손 | 비례 축소. 컵 입구가 김 시작점(y 16%) 근처, 아래 23%는 viewport가 자름 |
| donggeuli_arms_up | **480² / 190,972** (최종 다이어트, 이전 720² / 427,339) | 동글이 쉼터 카드 | 비례 축소. 주먹 중심 x 23.9%/76.2%, y 21%가 사진(0.50C) 아래 양 모서리를 덮음. 코드 미변경. 다이어트는 캔버스 전체 정비례 축소, 정규화 bbox (0.181,0.161)-(0.818,0.914)→(0.179,0.160)-(0.819,0.915). 최대 표시 ≈258px 대비 1.86x, decode 추정 2.07→0.92MB |

- **합계:** 21장 HEAD~ 대비 11,673,884 → 10,656,499 B(−1,017,385, 21장 적용 commit `e952184` 시점). 최종 다이어트 2장(−834,491) 후 **9,822,008 B**.
- **최종 다이어트 범위:** 위 2장만. editor_label_peek(sample·crop 상수 결합, RAM 이득 없음)와 나머지 19장은 표시 대비 1.17~1.53x 또는 STANDARD 고정값이라 유지. 로컬 release APK 41.39MB는 로컬 untracked 자원 포함 빌드라 기준 비교 아님.
- **변경 없음:** gallery_date_paper_strip, gallery_paper_tile, visit_calendar_paper, visit_calendar_newspaper_ribbon, 도장 실루엣 4종, postcard_app_icon_hand_envelope, 미사용 보호 자원.
- **QA:** 사용자 실기기 감각 QA 완료(시계·스누즈 손·편지 손은 재교체 후 재확인). 자동 테스트는 좌표 경계만 보호하며 시각 정렬은 보호하지 않는다.

## 105일차 수정 — 동글이 쉼터: 양팔 든 동글이 (2026-10-16, 사용자 화면 QA 완료)

- 자원: `drawable-nodpi/donggeuli_arms_up.png`. 사용자 제공 1254² RGBA → **720² RGBA**, 477,088 B. PArgb + HighQualityBicubic 비례 축소, canvas·alpha 유지, crop·tint·재구도 없음. 최종 alpha 0~255, 완전 투명 319,845px, alpha>16 bbox `(134,60)..(599,679)`(끝점 포함).
- 소비: `SheepRanchCard(characterAppearance = !raceEnabled)` → `DonggeuliCardContent`. 엽서마다 캐릭터 1개, 최신 최대 10장. `StampCardContent`(날짜 숨김, 톱니 사진·편지 배지 유지)를 먼저 그린 뒤 `painterResource`/`Image`로 캐릭터를 앞에 그림. 손끝이 사진 아래 모서리를 덮게 배치, 사용자 실기기 확인 완료.
- 조작 상자 W=92/84/76dp, H=W+36dp 유지. 캐릭터 정사각 C=0.90W, top=H−14dp−C. 사진 정사각 P=0.50C(41.4/37.8/34.2dp), 가로 중앙, top=캐릭터top+0.20C−P. 날짜는 별도 8sp Text로 상자 하단 중앙(yyyy-MM-dd, 한 줄 말줄임). 예전 좁은 중앙 슬롯은 사용하지 않음.
- **구도/animation 결합:** PNG의 팔·머리 위치(사진 하단 기준 20%)와 배치 결합. 부모의 기존 이동·회전·원근·점프·늘이기를 캐릭터/사진/날짜가 함께 받음. 직사각 hit 유지, 새 입력/state 없음. 최대 stretch·lift 시 화질은 실제 QA 필요, 현재 자원은 export에 쓰지 않음.
- 원본 decode RAM 추정: 720²×4=2,073,600 B(약 1.98MiB). 동일 리소스 반복 참조, 실제 bitmap 공유·동시 상주·GPU·APK 변화는 미측정. 이전 합계는 역사 값.
- 이전 `sheep_ranch_sheep.png`(720×480, 611,857 B)와 `round_head.png`(1254²)는 연결하지 않으며 파일은 사용자 자원으로 보존. `quick_select_sheep_hand.png`는 새 메뉴 전용 자원 미제공이라 기존 메뉴 연결을 유지, 접근성 이름만 동글이 쉼터. 내부 enum/파일명 대규모 rename 없음.
- 실기기 감각 QA: 사용자가 2026-10-16 확인 완료를 보고하고 commit/push 승인. 개별 시나리오·기기 설정·최대 확대 화질의 별도 측정 기록은 없음. Codex 기기 설치·instrumentation 미실행.

## 0. 측정 방법과 한계

- **발견:** `.git`·`build/`·`.gradle/`·`.kotlin/`·`.idea/`를 뺀 저장소 전체에서 `png/jpg/jpeg/webp/gif/svg/bmp/avif/heic`를 찾았다. 앱 이미지는 `app/src/main/res/drawable/`(5장)과 `drawable-nodpi/`(26장)에만 있다. `drawable-*dpi` 밀도 폴더, `mipmap-*` PNG, `assets/` 폴더는 **없다**(`mipmap-anydpi-v26`에는 adaptive icon XML 2개만 있다). `test`·`androidTest`에도 이미지가 없다. 앱 밖으로는 `docs/ai/mockups/visit-calendar-2dp.png` 1장이 있으며 APK에 들어가지 않는다.
- **픽셀·형식·알파:** PNG IHDR과 tRNS를 직접 읽었다. 투명 여백(그림 영역 bbox)은 PNG를 직접 풀어 알파 > 16인 픽셀로 쟀다.
- **APK:** 로컬 `app-release-unsigned.apk`(2026-10-13 18:37 빌드, 59,547,037 B)와 `app-debug.apk`(70,053,840 B)를 `aapt2 dump resources`로 열어 리소스 이름과 APK 안 파일을 맞췄다. release는 리소스 경로를 줄여 쓴다(예: `res/9S.png`).
  - 이 release는 **로컬 빌드여서 보호 untracked `postcard_paper_fiber_tile.png`가 들어 있다.** clean checkout이나 CI 빌드에는 들어가지 않는다.
  - 오늘 다시 빌드하지 않았다.
- **메모리:** 숫자는 모두 `너비 × 높이 × 4 B`(ARGB_8888)로 **계산한 추정**이다. 실기기 `meminfo`는 측정하지 않았고, 이번 조사 중 연결된 기기도 없었다.
- **표시 px:** 3.0x(xxhdpi) 밀도를 가정해 계산했다. 같은 날 실측한 사용 기기(SM-S936N) 밀도는 **450dpi(2.8125x)**여서, 이 문서의 "3x 예상 px"는 실제보다 약 6% 크다(3x 값 × 0.9375 = 실기기 px). 과대 배율 판정과 등급은 바뀌지 않는다.
- **실기기 메모리:** 같은 날 읽기 전용 `dumpsys meminfo`로 실측했다(8절). 2·5절의 "추정" 메모리 값은 8절과 함께 읽는다.
- **용량 단위:** MB = 1,000,000 B다(MiB를 쓴 곳은 따로 적었다).

## 1. 요약 통계

| 항목 | 값 |
|---|---|
| 앱 이미지 총 개수 | **31**(PNG 31, JPG/WebP/기타 0). 그중 tracked 30, untracked(보호) 1 |
| 총 원본 파일 용량 | **42,935,656 B(42.94MB)**. tracked 39,938,015 B + 보호 untracked 2,997,641 B |
| APK 안 앱 이미지 실제 총 용량(release) | **40,707,776 B(40.71MB)**. aapt crunch로 원본 대비 약 −5.2%, zip 안에서는 STORED(무압축) |
| release APK 전체 대비 이미지 비율 | **68.4%**. 보호 타일을 빼고 다시 계산하면 37.85MB / 56.69MB = 66.8% |
| 같은 release의 라이브러리 이미지 | 192개, 95,281 B(0.1MB). 무시해도 되는 수준 |
| debug APK 안 앱 이미지 | 42,935,656 B(원본 바이트 그대로) / 70.05MB = 61.3% |
| PNG 개수 / 총 용량 | 31 / 42.94MB |
| 1MB 이상 | **23장**(1MiB 이상도 23장) |
| 2MB 이상 | **6장**(2MiB 이상은 5장. 앱 아이콘 2,000,324 B가 경계에 걸린다) |
| 상위 10장 비율 | 22,181,249 B = **51.7%** |
| 상위 20장 비율 | 36,710,323 B = **85.5%** |
| 원본 크기로 풀었을 때 장당 RAM | 큰 PNG 26장은 모두 약 157만 화소라서 **장당 약 6.29MB**로 같다 |

### 용량 상위 10

| 순위 | 자산 | 원본 B | release APK B | 상태 |
|---|---|---|---|---|
| 1 | postcard_paper_fiber_tile.png | 2,997,641 | 2,856,100(로컬 빌드만) | PROTECTED |
| 2 | gallery_paper_tile.png | 2,668,364 | 2,568,818 | SHARED |
| 3 | visit_calendar_paper.png | 2,431,538 | 2,337,336 | SHARED(달력 안) |
| 4 | home_right_hand_coffee_cup.png | 2,361,108 | 2,293,040 | APK·MEMORY |
| 5 | home_clock_collage_body.png | 2,238,553 | 2,175,590 | APK·MEMORY |
| 6 | postcard_app_icon_hand_envelope.png | 2,000,324 | 1,887,431 | APK·REVIEW |
| 7 | editor_cutout_background.png | 1,893,715 | 1,684,409 | APK |
| 8 | tape_press_hand.png | 1,891,338 | 1,834,830 | KEEP |
| 9 | seal_stamp_hand.png | 1,884,772 | 1,866,025 | SHARED |
| 10 | editor_cutout_sticker.png | 1,813,896 | 1,669,051 | APK |

### 기능군별 파일 수·용량

파일 하나는 한 묶음에만 넣었다. 여러 기능에서 쓰는 파일은 "공용" 묶음에 한 번만 셌다.

| 기능군 | 파일 | 원본 | release APK | 원본 크기로 풀 때 RAM 합(추정) |
|---|---|---|---|---|
| 편집 상위 기능줄(잡지 오림 7) | 7 | 10.41MB | 9.50MB | 44.0MB. 실제로는 1/4 decode라 약 2.7MB |
| 갤러리 상단 레트로 시계 | 3 | 6.32MB | 6.14MB | 18.9MB |
| 갤러리 바로가기 손(부채) | 5 | 6.30MB | 6.09MB | 31.5MB |
| 편집 손 연출(도장·핀셋·테이프) | 3 | 5.07MB | 4.95MB | 18.9MB(연출하는 동안만) |
| 공용 종이(갤러리 타일·월 라벨 띠) | 2 | 3.88MB | 3.70MB | 12.6MB |
| 방문 달력(종이·리본) | 2 | 3.29MB | 3.08MB | 12.6MB |
| 미사용/보호(구 런처 전경·fiber) | 2 | 3.12MB | 2.97MB | 쓰이지 않음 |
| 앱 아이콘(런처) | 1 | 2.00MB | 1.89MB | 런처 프로세스가 decode |
| 갤러리 흔들기 랜덤 엽서 손 | 1 | 1.42MB | 1.38MB | 6.3MB(오버레이가 떠 있는 동안) |
| 하단 세부 선택줄 빠끔 얼굴 | 1 | 1.06MB | 0.96MB | 1/4 decode, 약 0.39MB |
| 도장 실루엣(강아지·비둘기·하트·별) | 4 | 0.07MB | 0.05MB | 밀도만큼 확대돼 3x 기준 약 24.7MB(4장 모두 풀릴 때) |

## 2. 기본 지도

**상태 표기:**

| 상태 | 뜻 |
|---|---|
| KEEP | 지금 그대로 |
| SHARED | 여러 화면·기능이 같이 씀 |
| APK-OPT | APK 용량 최적화 후보 |
| MEM-OPT | 메모리 최적화 후보 |
| DUP | 중복 후보 |
| UNUSED | 미사용 후보 |
| PROTECTED | 보호 대상 |
| REVIEW | 판단 보류 |

**공통값:** 31장 모두 PNG, 8bit다. 픽셀 칸의 "RGBA"는 알파 채널이 있고 실제로 투명 배경이라는 뜻이다. 31장 모두 production 코드나 XML이 참조하는지 확인했고(아래 "코드 참조" 칸), APK에는 31장이 모두 들어 있다.

### 2-1. 갤러리(메인)

| 자산 | 경로 | 픽셀 | 파일 / APK | 사용 화면·기능 | 코드 참조 | 재사용 | 로딩 | APK 영향 | 메모리 영향 | 상태 |
|---|---|---|---|---|---|---|---|---|---|---|
| gallery_paper_tile | nodpi | 1254² RGB 불투명 | 2.67 / 2.57MB | 갤러리 본문 종이(빈 화면 + 목록 화면), **편집 화면 엽서 바깥 배경 + 하단 편집 메뉴 바닥** | `GalleryScreen.kt` `GalleryPaperBackground`, `DetailScreen.kt` `editorPaperTile` → `Modifier.editorPaperBackground` | 2화면·3곳, 공용 | `ImageBitmap.imageResource`(원본 decode), `ImageShader` Repeated, 360dp 타일 | 큼 | 6.29MB(갤러리·편집 모두 상주) | SHARED, REVIEW |
| gallery_date_paper_strip | nodpi | 2172×724 RGBA | 1.21 / 1.13MB | 갤러리 월별 보기 월 라벨 띠, **편집 하단 세부 선택줄(사진·배경·스티커·테이프·낙서 5탭)의 라벨 띠** | `GalleryScreen.kt` `GalleryMonthHeader`(페이지당 1회 load), `PaperLabelSubcategoryNav.kt` `loadPaperLabelStrip` | 2화면, 의미가 다른 재사용, **강한 결합** | 갤러리: 원본 decode. 선택줄: `decodeResource` + `inSampleSize 4` + 정적 캐시 | 중 | 갤러리 6.29MB, 선택줄 0.39MB | SHARED, MEM-OPT |
| home_clock_collage_body | nodpi | 1695×928 RGBA → **720×394**(104일차) | 2.24 / 2.18MB | 갤러리 상단 레트로 시계 몸체 | `GalleryRetroClock.kt` `GalleryRetroClock` | 단일 | `painterResource`(원본 decode) | 큼 | 6.29MB(갤러리 상주, **실측 확인: 시계 3장 약 18MB**, 8절) | APK-OPT, MEM-OPT |
| home_clock_left_snooze_hand | nodpi | 1448×1086 RGBA → **480×360**(104일차) | 1.72 / 1.67MB | 시계 위 왼손 | 같음 | 단일 | `painterResource` | 큼 | 6.29MB(상주) | APK-OPT, MEM-OPT |
| home_right_hand_coffee_cup | nodpi | 1086×1448 RGBA → **450×600**(104일차) | 2.36 / 2.29MB | 시계 옆 커피잔 든 오른손(김 애니메이션 아래) | 같음 | 단일 | `painterResource`, `requiredSize` 후 위쪽 108dp만 보이게 clip | 큼 | 6.29MB(상주) | APK-OPT, MEM-OPT |
| quick_select_camera_hand | nodpi | 1254² → **720²**(104일차 추가) RGBA | 1.13 / 1.09MB | 갤러리 바로가기 부채: 카메라 | `GalleryScreen.kt` `handResId()` → `GalleryQuickSelectHand` | 단일 | `ImageBitmap.imageResource` | 중 | 6.29MB(**닫힌 상태에서도 항상 그려져 상주**. 실측: 부채를 열어도 Bitmap 변화 0, 5장 약 30MB, 8절) | MEM-OPT |
| quick_select_letter_hand | nodpi | 1254² → **720²**(104일차 추가) RGBA | 1.37 / 1.33MB | 부채: 미래 우체통 | 같음 | 단일 | 같음 | 중 | 6.29MB(상주) | MEM-OPT |
| quick_select_river_hand | nodpi | 1254² → **720²**(104일차 추가) RGBA | 1.13 / 1.09MB | 부채: 연못 놀이 | 같음 | 단일 | 같음 | 중 | 6.29MB(상주) | MEM-OPT |
| quick_select_sheep_hand | nodpi | 1254² → **720²**(104일차 추가) RGBA | 1.34 / 1.30MB | 부채: 양 목장 놀이 | 같음 | 단일 | 같음 | 중 | 6.29MB(상주) | MEM-OPT |
| quick_select_checker_hand | nodpi | 1254² → **720²**(104일차 추가) RGBA | 1.32 / 1.28MB | 부채: 경주 놀이 | 같음 | 단일 | 같음 | 중 | 6.29MB(상주) | MEM-OPT |
| gallery_pick_hand | nodpi | 1254² RGBA | 1.42 / 1.38MB | 흔들기 → 랜덤 엽서 오버레이에서 엽서 귀퉁이를 집은 손 | `GalleryRandomPostcardOverlay.kt` `GalleryRandomPostcardOverlay` | 단일 | `imageResource` → Canvas `drawImage` | 중 | 6.29MB(오버레이가 떠 있는 동안) | KEEP |

### 2-2. 방문 달력(갤러리 왼쪽 drawer)

| 자산 | 경로 | 픽셀 | 파일 / APK | 사용 화면·기능 | 코드 참조 | 재사용 | 로딩 | APK 영향 | 메모리 영향 | 상태 |
|---|---|---|---|---|---|---|---|---|---|---|
| visit_calendar_paper | nodpi | 1122×1402 RGB 불투명 | 2.43 / 2.34MB | 달력 월 페이지 종이, 연·월 선택(picker) 종이 조각 2곳 | `VisitCalendarDrawer.kt` `VisitCalendarDrawer`(1회 load) → `VisitCalendarMonthPage`, `visitCalendarPickerPaperScrap` | 달력 안 3곳 | `imageResource`, cover-crop `ImageShader`, 조각은 seed로 무작위 crop | 큼 | 6.29MB, **갤러리에 상주**(실측 정정: drawer가 닫혀 있어도 이미 decode됨, 8절) | SHARED, KEEP |
| visit_calendar_newspaper_ribbon | nodpi | 1448×1086 RGBA → **192×144**(104일차, 비율 상수는 같은 4:3이라 유지) | 0.86 / 0.74MB | 달력 아래 양끝 신문 오림 리본 | `VisitCalendarDrawer.kt` `VisitCalendarBottomOrnament`(한 번 decode해 2곳에 그림) | 같은 줄 2곳 | `imageResource`, 폭 24dp, 원본 비율 상수 `1448f/1086f` | 낮음 | 6.29MB, **갤러리에 상주**(실측 정정, 8절) | MEM-OPT |

> **실측 정정(2026-10-14):** 달력 종이·리본은 달력을 열 때 decode되는 것이 아니다. 갤러리 화면에 있는 동안 이미 상주한다. 달력을 열 때 Bitmap 변화는 0이고, 늘어나는 것은 GPU 쪽(Graphics 약 +26.6MB)이다. 닫은 뒤에도 Graphics 약 +19MB가 남는다. drawer 내용이 닫혀 있는 동안에도 구성되기 때문으로 보인다(코드 원인 추적은 미실시).

### 2-3. 편집 화면 — 상위 기능줄·하단 세부 선택줄

| 자산 | 경로 | 픽셀(그림 영역) | 파일 / APK | 사용 화면·기능 | 코드 참조 | 재사용 | 로딩 | APK 영향 | 메모리 영향 | 상태 |
|---|---|---|---|---|---|---|---|---|---|---|
| editor_cutout_photo | nodpi | 1254² → **314²**(104일차) RGBA(950×697) | 1.71 / 1.55MB | 상위 기능줄 "사진" | `DetailScreen.kt` `customizationPageIcons` → `EditorBottomTabBar.kt` `loadEditorCutout` | 단일 | `decodeResource` + `inSampleSize 4`(약 313²) → 104일차: 자산 314², `EDITOR_CUTOUT_SAMPLE = 1`. 알파 bbox로 자동 자르기, **process 정적 캐시** | 큼 | 약 0.39MB(상주) | APK-OPT |
| editor_cutout_background | nodpi | 1254² → **314²**(104일차) RGBA(974×714) | 1.89 / 1.68MB | "배경" | 같음 | 단일 | 같음 | 큼 | 0.39MB | APK-OPT |
| editor_cutout_text | nodpi | 1254² → **314²**(104일차) RGBA(911×570) | 0.99 / 0.89MB | "글귀" | 같음 | 단일 | 같음 | 중 | 0.39MB | APK-OPT |
| editor_cutout_sticker | nodpi | 1254² → **314²**(104일차) RGBA(988×1035) | 1.81 / 1.67MB | "스티커" | 같음 | 단일 | 같음 | 큼 | 0.39MB | APK-OPT |
| editor_cutout_tape | nodpi | 1254² → **314²**(104일차) RGBA(1044×784) | 1.60 / 1.49MB | "테이프" | 같음 | 단일 | 같음 | 큼 | 0.39MB | APK-OPT |
| editor_cutout_stamp | nodpi | 1254² → **314²**(104일차) RGBA(728×1118) | 1.54 / 1.41MB | "도장" | 같음 | 단일 | 같음 | 큼 | 0.39MB | APK-OPT |
| editor_cutout_doodle | nodpi | 1254² → **314²**(104일차) RGBA(1011×815) | 0.87 / 0.81MB | "낙서" | 같음 | 단일 | 같음 | 중 | 0.39MB | APK-OPT |
| editor_label_peek | nodpi | 1254² RGBA(716×653 @379,353) | 1.06 / 0.96MB | 하단 세부 선택줄의 선택 칸 위 빠끔 얼굴(5탭 공통) | `PaperLabelSubcategoryNav.kt` `loadPaperLabelPeek` → `Modifier.paperLabelPeek` | 단일 컴포넌트(5탭) | `decodeResource` + `inSampleSize 4` + 정적 캐시, **고정 crop 좌표 `(376,352)/720×656`** | 중 | 0.39MB(상주) | APK-OPT, REVIEW |

### 2-4. 편집 화면 — 손 연출·도장·첫 방문 인트로

| 자산 | 경로 | 픽셀(그림 영역) | 파일 / APK | 사용 화면·기능 | 코드 참조 | 재사용 | 로딩 | APK 영향 | 메모리 영향 | 상태 |
|---|---|---|---|---|---|---|---|---|---|---|
| seal_stamp_hand | nodpi | 1254² RGBA(763×1194) | 1.88 / 1.87MB | 도장 찍기 손 연출, **앱 첫 방문 인트로에서 방문 소인을 찍는 손** | `SealStampInteraction.kt` `SealStampHandOverlay` ← `DetailScreen.kt`, `AppIntroScreen.kt` | 2화면, 의미가 다른 재사용 | `imageResource` → `drawImage`, 300dp, 정규화 anchor `(0.459, 0.431)` | 큼 | 6.29MB(연출하는 동안) | SHARED, KEEP |
| sticker_tweezer_hand | nodpi | 1254² RGBA(656×1228) | 1.29 / 1.25MB | 사진·글귀·라벨 스티커를 놓는 핀셋 손 | `PhotoStickerPlaceInteraction.kt` `StickerTweezerHandOverlay` ← Photo/Text/LabelStickerTweezerHandOverlay | 3기능 공용 | 같음, 300dp, anchor `(0.297, 0.045)` | 중 | 6.29MB(연출하는 동안) | SHARED, KEEP |
| tape_press_hand | nodpi | 1254² RGBA(943×1188) | 1.89 / 1.83MB | 마스킹테이프를 누르는 손 | `MaskingTapePlaceInteraction.kt` `MaskingTapePressHandOverlay` | 단일 | 같음, 260dp, anchor `(0.399, 0.259)` | 큼 | 6.29MB(연출하는 동안) | KEEP |
| seal_dog_paw | **drawable**(mdpi 취급) | 455×480 RGBA | 18 / 18KB | 도장 "강아지 발바닥" 모양: 엽서 위 도장, 도장 패널 52dp, 목록 56dp, 편집 다이얼로그 72dp, 찍기 미리보기, **저장·공유 이미지** | `SealShapes.kt` `sealImageRes` → `SealShapeContent`, `PostcardImageExporter.kt` `drawImageSealOverlay` | 화면 + exporter, **강한 결합** | 화면: `painterResource` + `ColorFilter.tint`. export: `decodeResource`(옵션 없음) + `PorterDuff SRC_IN`, 사용 후 recycle. **밀도만큼 확대해서 decode** | 없음 | 3x 기준 약 7.9MB(계산 추정, **실측 미발생**: 8절) | MEM-OPT, REVIEW |
| seal_heart | drawable | 480×416 RGBA | 21 / 14KB | 도장 "하트" | 같음 | 같음 | 같음 | 없음 | 3x 기준 약 7.2MB | MEM-OPT, REVIEW |
| seal_pigeon_track | drawable | 393×480 RGBA | 25 / 15KB | 도장 "비둘기 발자국" | 같음 | 같음 | 같음 | 없음 | 3x 기준 약 6.8MB | MEM-OPT, REVIEW |
| seal_star | drawable | 288×273 RGBA | 3 / 2KB | 도장 "별" | 같음 | 같음 | 같음 | 없음 | 3x 기준 약 2.8MB | MEM-OPT, REVIEW |

### 2-5. 앱 아이콘·미사용·보호

| 자산 | 경로 | 픽셀 | 파일 / APK | 사용 화면·기능 | 코드 참조 | 재사용 | 로딩 | APK 영향 | 메모리 영향 | 상태 |
|---|---|---|---|---|---|---|---|---|---|---|
| postcard_app_icon_hand_envelope | nodpi | 1254² RGB 불투명 | 2.00 / 1.89MB | 런처 아이콘(일반·둥근) 전경 | XML `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml` → `AndroidManifest` `@mipmap/ic_launcher*` | XML 2곳 | 시스템 런처가 108dp(xxxhdpi 432px)로 그림 | 큼 | 앱 프로세스 밖(런처) | APK-OPT, REVIEW |
| ic_launcher_foreground | drawable | 432² RGBA | 124 / 109KB | 없음. 7547e2c(2026-10-05)에서 런처 전경이 바뀐 뒤 참조 0 | 없음(HANDOFF에 "미참조, 삭제 안 함"으로 기록됨) | — | — | 낮음 | 없음 | UNUSED |
| postcard_paper_fiber_tile | nodpi | 1254² RGB 불투명 | 3.00MB / 로컬 release 2.86MB | 없음. 94일차 종이 질감 실험이 되돌려진 뒤 남은 사용자 제작 자산 | 없음 | — | — | 로컬 빌드만 | 없음 | **PROTECTED**: Git 비추적, 로컬 APK에는 포함, 사용 여부는 사용자 결정 대기. 수정·삭제·이동·stage 금지 |

참고로, 이미지가 아닌 drawable XML은 `drawable/ic_launcher_background.xml`(단색 `#F9EEDF` shape, 런처 배경) 하나뿐이다.

## 3. 재사용·강한 결합

**이 표의 자산은 파일 하나만 바꿔도 여러 화면이 동시에 바뀐다.**

| 자산 | 참조 위치 | 화면 | 기능 | 분류 | 같은 그림을 다르게 쓰는 방식 |
|---|---|---|---|---|---|
| gallery_date_paper_strip | 2파일(`GalleryScreen`, `PaperLabelSubcategoryNav`) | 2(갤러리, 편집) | 2(월 라벨, 세부 선택줄 5탭) | 의미가 다른 재사용 + **강한 결합** | 갤러리는 원본 decode에 바랜 갈색 colorMatrix를 쓴다. 선택줄은 1/4 decode에 미선택 연회색·선택 갈색 필터를 쓰고, 칸 번호에 따라 좌우·상하로 뒤집는다. **자르기 좌표 `(89,172)/2011×379`가 두 파일에 따로 있다**(선택줄은 /4). 갈색 필터 `GALLERY_DATE_STRIP_BROWN_FILTER`를 공유해서 두 곳 색이 항상 같이 바뀐다 |
| gallery_paper_tile | 2파일, 4곳(갤러리 2, 편집 2) | 2 | 3(갤러리 바닥, 편집 배경, 편집 메뉴 바닥) | 공용 재사용 | 갤러리와 편집 모두 360dp로 축소해 반복한다. 편집 메뉴 바닥은 `patternOrigin`만큼 무늬를 옮겨 위 종이와 섬유를 잇는다. 크기 계산이 `tile.width` 기준이라 해상도가 바뀌어도 화면 크기는 유지된다 |
| seal_stamp_hand | 1파일(오버레이) ← 2호출(`DetailScreen`, `AppIntroScreen`) | 2 | 2(도장 찍기, 첫 방문 인트로 방문 소인) | 의미가 다른 재사용 | 같은 손, 같은 anchor, 같은 300dp |
| sticker_tweezer_hand | 1파일 ← 3래퍼(사진·글귀·라벨 스티커) | 1 | 3 | 공용 재사용 | 같은 손 |
| 도장 실루엣 4장 | 2파일(`SealShapes`, `PostcardImageExporter`) | 편집 여러 곳 + 저장본 | 도장 모양, 저장·공유 | **강한 결합**(화면과 exporter가 같은 자산) | 화면은 Compose `tint`를 쓰고, export는 `PorterDuff SRC_IN`과 잉크 결손 erase mask를 쓴다. 자산을 바꾸면 이미 저장된 엽서를 **다시 내보낼 때** 결과가 바뀐다 |
| quick_select_* 5장 | `GalleryQuickSelectFan.kt` 블록 좌표 | 1 | 5 | **강한 결합**(원본 px 좌표) → **104일차 정정: 논리 좌표 결합** | 블록 사각형 좌표가 원본 1254px 기준 숫자(`QUICK_SELECT_HAND_SOURCE_PX = 1254f`)다. 해상도·구도를 바꾸면 블록 테두리(활성 표시)와 터치 영역 좌표를 다시 재야 한다 → 정정: 1254는 Bitmap px를 읽지 않는 **논리 좌표 단위**다. 모든 사용처가 `180dp/1254` 비례나 분율로 바뀐다. 정사각 canvas와 그림 구도만 지키면 PNG 해상도는 자유롭다(9-2절). **구도(crop·trim·위치)를 바꾸면** 여전히 블록 좌표를 다시 재야 한다 |
| editor_label_peek | `PaperLabelSubcategoryNav.kt` crop 상수 | 1 | 1 | **강한 결합**(원본 px 좌표) | crop `(376,352)/720×656`과 `inSampleSize 4`가 짝이다 |
| editor_cutout_* 7장 | `EditorBottomTabBar.kt` | 1 | 7 | 단일 사용, 약한 결합 | 알파 bbox를 자동으로 잘라 32×24dp 상자에 맞춘다(좌표 상수 없음). 단 `EDITOR_CUTOUT_SAMPLE = 4`가 원본 1254px을 전제로 한 값이다 → 104일차: 자산 314²와 짝인 `1`로 변경 |
| visit_calendar_paper | 1파일, 3곳 | 1 | 2(월 페이지, picker 조각) | 공용(달력 안) | 페이지는 cover-crop, 조각은 seed로 무작위 crop |
| visit_calendar_newspaper_ribbon | 1파일, 2곳 | 1 | 1 | 단일(한 번 decode해 공유) | 비율 상수 `1448f/1086f`가 원본 비율과 짝이다 → 104일차 192×144도 같은 4:3이라 상수 유지 |
| postcard_app_icon_hand_envelope | XML 2 | 런처 | 1 | 단일 | 일반·둥근 아이콘이 같은 전경을 쓴다 |
| 손 연출 anchor(seal_stamp/tweezer/tape/pick) | 각 파일 상수 | — | — | 약한 결합 | anchor가 0~1로 **정규화**돼 있어 같은 구도로 균일하게 축소하면 그대로 맞다. 다만 crop하거나 구도를 바꾸면 다시 재야 한다 |

**테스트 참조**(JVM 구조 테스트가 소스 텍스트를 읽음, production 아님). 이름을 바꾸거나 파일을 옮기면 이 테스트들도 깨진다:

- `EditorBottomTabBarStructureTest`: `editor_cutout_*`
- `GalleryPaperBackgroundStructureTest`: `gallery_paper_tile`
- `GalleryViewSelectionStructureTest`: `quick_select_*` 5장 + 손 3종이 갤러리에 없음을 확인
- `VisitCalendarPaperPageStructureTest`: `visit_calendar_paper`

`androidTest` 참조는 0이다.

## 4. 중복·미사용

- **바이트 단위 완전 중복(sha256):** 0건
- **이름은 다른데 내용 동일:** 0건
- **크기만 다른 동일 이미지:** 발견 못함
- **거의 같은 이미지:** `quick_select_*` 5장은 같은 손 + 같은 종이 블록 구도에 인쇄 그림만 다르다(카메라·강은 16×16 썸네일 차이가 가장 작음, 육안으로 확인). 합치려면 손과 블록 그림을 따로 합성하는 코드 변경이 필요하다 → DUP 아님, 현재 구조 유지
- **같은 목적의 여러 자산:** 종이 질감 3종(`gallery_paper_tile` 평균 밝기 222, `postcard_paper_fiber_tile` 197, `visit_calendar_paper` 209)은 서로 다른 그림이다. fiber는 미사용 + 보호
- **제거된 기능의 잔존:** `ic_launcher_foreground.png`(구 런처 전경), `postcard_paper_fiber_tile.png`(되돌린 94일차 종이 질감 실험)
- **동적 lookup**(`getIdentifier` 등): 0건
- **XML 간접 참조:** 런처 adaptive icon 1건뿐
- **판정**("검색 결과 없음 = 삭제 가능"으로 보지 않음):

| 판정 | 자산 |
|---|---|
| 사용 확인 | 29장 |
| 간접 사용 | `postcard_app_icon_hand_envelope`(XML), `seal_stamp_hand`(인트로, 컴포저블 경유) |
| test/preview 전용 | 0 |
| 미사용 의심 | `ic_launcher_foreground` |
| 보호 대상 | `postcard_paper_fiber_tile` |
| 조사 불확실 | 0 |

- 참고: release는 `isMinifyEnabled = false`이고 `shrinkResources`가 없어서, 쓰지 않는 리소스도 APK에 그대로 들어간다(빌드 설정은 이번 범위 밖).

## 5. 원본 크기 대비 실제 사용 크기

표시 px는 3.0x 가정이다. 실측 기기는 2.8125x(450dpi)이므로, 실기기 px는 아래 값 × 0.9375이고 과대 배율은 약 1.07배 커진다. 등급 판단은 그대로다. "과대 배율"은 decode한 쪽(또는 crop 영역)의 픽셀을 표시 px로 나눈 값이다.

| 자산 | 원본(사용 영역) | 화면 표시 | 3x 예상 px | decode 크기 | 과대 배율 | 판정 |
|---|---|---|---|---|---|---|
| editor_cutout_*(7) | 1254²(그림 약 700~1050) | 최대 32×24dp 상자 | 96×72 | 약 313²(1/4) | 원본 기준 약 10배, decode 기준 약 3배 | **APK 과대**(RAM은 이미 줄여 decode 중) → 104일차: 원본 314², decode 동일(약 3배) |
| editor_label_peek | 720×656 crop | 높이 20dp | 약 66×60 | 1/4 → crop 180×164 | decode 기준 약 2.7배 | APK 과대 |
| gallery_date_paper_strip | 2011×379 crop | 최소 136dp × 약 29dp(월 라벨) / 높이 25dp(선택줄) | 약 408×87 | 갤러리: **원본 2172×724** / 선택줄: 543×181 | 갤러리 약 4.9배 | **갤러리 RAM 과대** |
| home_clock_collage_body | 1695×928 | 168×92dp | 504×276 | 원본 | 3.4배 | 과대 → 104일차 720×394: 1.43배(4x 기기에서도 1배 이상) |
| home_clock_left_snooze_hand | 1448×1086 | 112×84dp | 336×252 | 원본 | 4.3배 | 과대 → 104일차 480×360: 1.43배 |
| home_right_hand_coffee_cup | 1086×1448 | 105×140dp(위 108dp만 보임) | 316×421 | 원본 | 3.4배 | 과대 → 104일차 450×600: 1.42배 |
| quick_select_*(5) | 1254² | 180dp 정사각 | 540 | 원본 | 2.3배 | 과대(5장 상주) → 104일차 720²: 450dpi 506px의 1.42배 |
| visit_calendar_newspaper_ribbon | 1448×1086(리본 948×463) | 24×18dp(실제 리본 약 16×8dp) | 72×54 | 원본 | **약 20배** | 크게 과대 → 104일차 192×144: 약 2.7배 |
| gallery_paper_tile | 1254² | 360dp 타일 | 1080 | 원본 | 1.16배 | 적정 |
| visit_calendar_paper | 1122×1402 | drawer 폭 304dp 안 월 페이지(cover-crop) | 약 840~900 폭 | 원본 | 약 1.3배 | 적정 |
| gallery_pick_hand | 1254² | 엽서 한 변의 1.0~1.6배(약 220~360dp) | 약 670~1070 | 원본 | 약 1.2~1.9배 | 대체로 적정 |
| seal_stamp_hand / sticker_tweezer_hand | 1254² | 300dp | 900 | 원본 | 1.4배 | 적정 |
| tape_press_hand | 1254² | 260dp | 780 | 원본 | 1.6배 | 적정에 가까움 |
| 도장 실루엣(4) | 288~480px | 52~72dp(패널·다이얼로그), 엽서 위는 도장 크기에 따름 | 156~216(아이콘) | **원본 × 밀도 배율**(3x면 약 1365×1440) | 아이콘 기준 약 6~9배 | RAM 과대(`drawable/` 위치 때문). export 해상도와 연결 |
| postcard_app_icon_hand_envelope | 1254² | 런처 108dp | 432(xxxhdpi) | 런처 | 2.9배 | APK 과대 |

### 로딩 방식 요약

| 방식 | 자산 | 특징 |
|---|---|---|
| `painterResource` | 시계 3장, 도장 실루엣(화면) | composition 동안 원본 decode. 실루엣은 밀도 확대 |
| `ImageBitmap.imageResource` | 종이 타일, 월 라벨 띠(갤러리), 바로가기 손 5, 흔들기 손, 손 연출 3, 달력 종이·리본 | 원본 decode. `remember` 범위 동안 유지, 내부적으로 Resources drawable 캐시(약참조)를 거친다(화면 간 공유 여부는 실측 안 함) |
| `BitmapFactory.decodeResource` + `inSampleSize` + 정적 캐시 | cutout 7장, 월 라벨 띠(선택줄), 빠끔 얼굴 | 1/4 decode, **process가 살아 있는 동안 상주**(합계 약 3.5MB) |
| `decodeResource`(옵션 없음) + recycle | 도장 실루엣(export) | 저장할 때마다 밀도 확대된 크기로 decode했다가 바로 해제 |
| shader 반복 | 종이 타일(Repeated), 달력 종이(cover-crop, 조각 crop) | 큰 원본 하나를 반복·자르기 |
| 시스템 | 앱 아이콘 | 런처 프로세스 |

**화면별 동시 상주(조사 당시 계산 추정).** 실측 결과는 8절에 있다. 갤러리 쪽은 거의 맞았고, 달력 항목은 정정됐다.

- **갤러리 첫 화면(월별 보기):** 아래 합계 **약 63MB**
  - 종이 타일 6.3MB
  - 시계 3장 18.9MB
  - 바로가기 손 5장 31.5MB(닫힌 묶음도 항상 그려짐)
  - 월 라벨 띠 6.3MB
- **편집 화면:** 아래 합계 약 10MB. 손 연출이 돌 때마다 6.3MB, 도장 패널을 열면 실루엣 4장(3x 기준 최대 약 24.7MB)이 더해진다
  - 종이 타일 6.3MB(갤러리와 공유되는지는 실측 안 함)
  - 정적 캐시 약 3.5MB
- **달력 drawer:** 열린 동안 12.6MB 추가 → **실측으로 정정:** Bitmap은 갤러리에 이미 상주(추가 0). 달력을 열 때는 Graphics만 약 +26.6MB 늘어난다

## 6. 최적화 후보 평가

기준: 파일 무게 × 실제로 얼마나 작게 쓰이는가 × 변경 위험. 절감량은 추정이고, 실행은 별도 승인 작업이다.

| 등급 | 자산 | 근거 | 예상 효과(추정) | 같이 바뀌어야 하는 것 / QA |
|---|---|---|---|---|
| **A → 완료(104일차, 9절)** | editor_cutout_* 7장 | APK 9.5MB를 32×24dp에 쓴다. 알파 bbox 자동 자르기라 좌표 상수가 없다. 화면은 1곳 | APK 약 −8~9MB(1/4 축소 기준). RAM 변화 없음 | `EDITOR_CUTOUT_SAMPLE`을 새 해상도에 맞춰야 한다(그대로 두면 이중 축소로 흐려짐). 상위 기능줄 7개를 육안 QA |
| **A → 완료(104일차, 9절)** | home_clock_* 3장 | APK 6.1MB, 3.4~4.3배 과대, **갤러리 첫 화면에 상주**(실측 Bitmap 약 18MB, 8절). 표시 크기가 dp 상수라 원본 px에 묶이지 않는다 | APK 약 −4~5MB, RAM 약 −14MB(표시 px의 약 1.3~1.5배로 축소 시) | 비율 유지 필수(숫자창 위치가 dp로 몸체 비율에 맞춰져 있음). 갤러리 상단 육안 QA(하프톤 결) |
| **A → 완료(104일차, 9절)** | visit_calendar_newspaper_ribbon | 약 20배 과대. **갤러리에 상주하는 6.3MB**(실측 정정: drawer가 닫혀 있어도 decode됨) | RAM 약 −6MB, APK 약 −0.7MB | 비율 `1448f/1086f` 유지 또는 상수 갱신. 육안 QA는 짧게 |
| **B → 완료(104일차 추가, 9-2절)** | quick_select_* 5장 | 갤러리에 상주하는 RAM 31.5MB(가장 큼, 실측 Bitmap 약 30MB, 부채를 열어도 변화 0), 2.3배 과대 | RAM 약 −20MB 이상, APK 약 −3~4MB | 블록 좌표가 원본 px(1254) 기준이라 자산을 줄이면 `QUICK_SELECT_HAND_SOURCE_PX`와 블록 20개 숫자를 함께 바꾸거나, 자산은 두고 decode만 줄이는 코드 경로가 필요하다. 부채 펼침·활성 테두리·터치 영역 실기기 QA |
| **B** | gallery_date_paper_strip | 갤러리에서 원본 decode 6.3MB, 약 4.9배 과대 | RAM 약 −6MB(갤러리 decode를 선택줄과 같은 1/4로 할 때) | **두 화면 강한 결합**: 자산 자체를 바꾸면 두 파일의 자르기 좌표와 선택줄 sample을 같이 바꿔야 한다. 메모리만 노리면 갤러리 decode 경로만 바꾸는 코드 변경(오늘 금지 항목). 갤러리 월 라벨 + 하단 선택줄 5탭 QA |
| **B** | 도장 실루엣 4장 | APK 영향은 없음. `drawable/`(mdpi) 위치 때문에 밀도 확대 decode(3x 기준 장당 약 3~8MB, 계산 추정). 10-14 실측 경로(도장 패널만 열기)에서는 이 decode가 발생하지 않아 **미측정** | RAM 수~20MB대(패널을 열 때) | **화면 + 저장·공유 exporter 공용**: 위치나 decode를 바꾸면 export 결과 해상도가 달라질 수 있다. 실측(기기 밀도, meminfo)부터 하고, 저장본 비교 QA |
| **B** | postcard_app_icon_hand_envelope | APK 1.9MB, 런처 432px 대비 2.9배 | APK 약 −1.5MB | 런처 아이콘은 시스템 전체에 보인다. 런처 캐시·둥근 마스크 QA |
| **B** | editor_label_peek | APK 0.96MB | APK 약 −0.85MB | 고정 crop 좌표 `(376,352)/720×656`과 sample 4를 같이 바꿔야 한다. 빠끔 위치 QA |
| **C** | tape_press_hand / seal_stamp_hand / sticker_tweezer_hand | 1.4~1.6배라 과대가 아니다. RAM은 연출하는 동안만 쓴다 | APK 약 −2MB(소폭 축소 시) | 큰 화면에 보여서 품질 손실이 눈에 띌 수 있다. 지금 할 필요 낮음 |
| **C** | gallery_pick_hand | 거의 적정 | 작음 | — |
| **C** | visit_calendar_paper | 약 1.3배, drawer가 열린 동안만 | 작음 | 해상도보다 형식 변환(손실 압축)으로 줄일 여지는 있으나 종이 결 품질 판단이 필요하다 |
| **C** | ic_launcher_foreground | 미사용, APK 0.1MB | APK −0.1MB | 삭제는 별도 승인(오늘 안 함) |
| **D** | gallery_paper_tile | 360dp ≈ 1080px라 해상도가 거의 적정이다. 갤러리·편집 공용의 핵심 질감 | 해상도 축소 이득 적음 | 품질 위험이 크다. 형식 변환은 별도 실험 대상 |
| **D** | postcard_paper_fiber_tile | **PROTECTED**: 사용자 결정 대기 | — | 건드리지 않음 |

### 순서 제안(실행 승인 아님)

1. **가장 적은 위험으로 가장 큰 APK 절감:** cutout 7장(+ sample 상수 1개). 그다음 시계 3장(갤러리 RAM 동시 절감)
2. **가장 큰 RAM 절감:** 바로가기 손 5장. 좌표 결합 때문에 "자산을 축소할지, decode만 줄일지" 방식을 먼저 정해야 한다
3. 월 라벨 띠·도장 실루엣은 실기기 `meminfo` 실측으로 실제 이득을 확인한 뒤 판단
4. PNG → WebP(손실/무손실) 변환은 해상도 축소와 섞지 말고, 같은 자산 1~2장으로 별도 비교한 뒤 확대
5. 앞서 알려진 후보에 대한 결론: cutout 7장은 A(전체 감사 뒤에도 유지), `gallery_date_paper_strip`은 두 화면 강한 결합 때문에 B로 조정

## 7. 이 지도를 갱신해야 하는 경우

- 이미지 추가·삭제·교체·해상도 변경·형식 변환
- 같은 이미지를 새 화면에서 재사용하게 될 때
- 로딩 방식(decode, `inSampleSize`, 캐시)이나 표시 크기 상수가 바뀔 때
- `postcard_paper_fiber_tile.png`의 사용·삭제를 사용자가 결정했을 때
- 이미지 관련 코드·자산이 바뀐 뒤 실기기 메모리를 다시 쟀을 때(8절 갱신)

## 8. 실기기 메모리 실측 (2026-10-14)

### 측정 조건

| 항목 | 값 |
|---|---|
| 기기 | 실사용 기기 SM-S936N 1대(`adb devices -l`로 1대만 연결 확인) |
| 밀도 | 450dpi / 2.8125x(`wm density`, 인자 없이 읽기) |
| 빌드 | 설치된 debug 빌드(`lastUpdateTime 2026-10-13 18:00`, `firstInstallTime 2026-10-05` 유지). 102일차 마지막 QA 빌드로 추정되며, HEAD와 완전히 같은지는 미확인. 이미지 자산은 동일 |
| 방식 | PC에서 3초 간격으로 `dumpsys meminfo com.postcardmemory`를 연속 기록했다. 사용자가 화면을 바꿀 때마다 알려 주면 그 시각을 marker로 붙였다. 화면마다 약 20초 머물렀다 |
| 경로 | 갤러리(월별) → 바로가기 부채 열기만 → 갤러리 → 편집 화면 → 도장 탭(패널만) → 갤러리 → 방문 달력 열기 → 닫기. 이 경로를 **2바퀴** 돌았다 |
| 샘플 | **139개**. 구간 값은 marker 후 6초를 버린 안정 구간의 중앙값이다 |
| 프로세스 | PID **13993**이 시작·매 marker·끝에서 모두 유지됐다. 재시작·오류·ANR 없음 |
| 안전 조건 | 실행한 명령은 승인된 읽기 전용 범위뿐이다(`devices -l`, `pidof`, `dumpsys meminfo`, `wm density`, `dumpsys package`). install·uninstall·`pm clear`·force-stop·kill·trim-memory·dumpheap·run-as·설정 변경·Profiler·gfxinfo는 쓰지 않았다. 기기 쪽 쓰기는 편집 화면을 나갈 때 앱이 하는 정상 초안 저장뿐이다(사용자 허용). 측정용 데이터 생성·수정은 없었다. 원본 로그는 PC 임시 폴더에만 있고 저장소에 넣지 않았다 |
| 해석 기준 | **`Native Allocations`의 Bitmap(malloced) 총량**과 Native Heap Alloc, Graphics를 중심으로 판단했다. TOTAL PSS·RSS는 측정 도중 swap된 페이지가 돌아오며(TOTAL SWAP PSS 약 104MB → 약 52MB) 흔들려서 보조값으로만 쓴다 |

### 구간별 결과 (MB = 1024 KiB, 2바퀴 패턴 동일)

| 구간 | Bitmap 개수 / 총량 | Native Heap Alloc | Graphics | TOTAL PSS(보조) |
|---|---|---|---|---|
| 갤러리 기준(1바퀴 시작) | 37 / **72.0MB** | 90.4MB | 119.2MB | 316.9MB |
| 바로가기 부채 열기 | 37 / 72.0MB(**변화 0**) | 90.5MB | 119.2MB | 317.3MB |
| 편집 화면 | 37 / **13.8MB** | 35.6MB | 163.3MB | 329.4MB |
| 도장 패널 | 47 / 13.9MB(+14KB) | 35.8MB | 155.4MB | 314.4MB |
| 갤러리 복귀 | 55 / **75.4MB** | 97.2MB | 126.2MB | 336.7MB |
| 방문 달력 열기 | 55 / 75.4MB(**변화 0**) | 98.0MB | 152.8MB(**약 +26.6MB**) | 365.4MB |
| 달력 닫음(갤러리) | 55 / 75.4MB | 97.5MB | 145.3MB(**약 +19MB 잔존**) | 357.0MB |
| 2바퀴: 편집 / 갤러리 복귀 | 37 / 13.8MB · 55 / 75.4MB | 36.3 / 97.5MB | 163.8 / 128.6MB | — |

### 결론

- **갤러리 Bitmap 상주량은 실측 72.0MB(73,730 KiB)다.** 이 값은 원본 decode 6,142 KiB(약 6.0MB)짜리 큰 그림 **12장**의 합(73,704 KiB)과 거의 같다. 12장 구성은 아래와 같다.
  - 종이 타일 1
  - 시계 3(**약 18MB**)
  - 바로가기 손 5(**약 30MB**)
  - 월 라벨 띠 1
  - **달력 종이 1, 달력 리본 1**
- **조사 때의 갤러리 추정(약 63MB, 10장)은 맞았다.** 다만 달력 2장이 이미 상주하고 있어서 실제로는 12장이었다.
- **바로가기 손:** 부채를 열기 전후 Bitmap 변화가 0이다. "닫혀 있어도 항상 상주"라는 판단이 실측으로 확인됐다. 갤러리 상주량의 약 42%(약 30MB)를 차지한다.
- **시계 3장:** 약 18MB로 갤러리 상주량의 약 25%를 차지한다. 메모리 최적화 우선순위 근거가 실측으로 강화됐다.
- **달력(정정):** 달력 종이·리본은 달력을 열 때 decode되는 것이 아니라 갤러리에서 이미 상주한다. 달력을 열 때 Bitmap 변화는 0이고, Graphics만 약 +26.6MB 늘어난다. 닫은 뒤에도 Graphics 약 +19MB가 남는다(GPU 텍스처 캐시로 보이며 원인은 미추적).
- **편집 화면:** Bitmap 약 13.8MB로, 갤러리보다 약 58MB 적다.
  - 구성(계산 대조): 종이 타일 약 6.0MB + 정적 캐시 약 3.4MB + 기타 약 4.5MB
  - 편집 화면에 들어가면 갤러리 그림이 해제되고, **갤러리로 돌아올 때마다 큰 PNG 10장 이상을 다시 decode한다.** 화면을 왕복할 때마다 decode 비용(CPU, 전환 순간의 버벅임)이 반복될 가능성이 있다. 체감 측정은 하지 않았다
- **정적 캐시:** 갤러리로 돌아온 뒤 기준보다 정확히 +3,447 KiB(약 3.4MB)가 남는다. cutout 7장 + 띠 1/4 + 빠끔 얼굴 1/4의 계산 합과 같다. 2바퀴째에는 더 늘지 않았다. **이번 측정에서는 누수처럼 계속 늘어나는 징후가 없다.**
- **도장 실루엣(미측정 유지):** 도장 패널만 열면 Bitmap이 +14KB뿐이라, 밀도 확대된 큰 decode가 이 경로에서는 일어나지 않았다. 종류를 고르는 다이얼로그나 실제 엽서 위 도장 경로는 측정하지 않았다. 그래서 MEM-OPT / REVIEW 판단은 **계산 추정 그대로** 둔다.
- **이번에 측정하지 못한 것:**
  - 1초 안에 지나가는 손 연출(3초 간격이라 잡히지 않음)
  - 도장 종류 다이얼로그
  - 흔들기 랜덤 엽서 오버레이
  - release 빌드
  - GPU 내역(`gfxinfo`는 1차에서 제외)

### 최적화 우선순위에 미치는 영향

1. **메모리:** 바로가기 손 5장(약 30MB) → 시계 3장(약 18MB) → 달력 리본(약 6MB, 20배 과대) 순으로 근거가 실측으로 확인됐다. 손 5장은 블록 좌표 결합 때문에 방식(자산 축소 vs decode 축소) 결정이 먼저다.
2. **APK:** cutout 7장(A)은 메모리와 무관해서 실측 결과로 바뀌지 않는다.
3. 달력 종이·리본이 갤러리에 상주하는 이유(drawer 구성 시점)는 이미지 문제가 아니라 구조 문제일 수 있다. 바꿀지는 별도 조사·승인 대상이다.
4. 갤러리 ↔ 편집 왕복 때 반복 decode는 이미지를 줄이면 함께 줄어든다. 체감 영향은 필요하면 따로 관찰한다.

## 9. 104일차 1차 다이어트 결과 (2026-10-15)

8절(103일차 Before)은 그대로 두고 After만 여기 적는다.

### 바꾼 것

- 그림 11장의 원본 해상도만 줄였다. 화면 dp 크기·위치·순서·터치 영역·접근성 코드는 그대로다.
- 축소 방법: Windows System.Drawing, premultiplied alpha, HighQualityBicubic. 축소 전후 알파 bbox(알파 > 16) 차이는 장마다 0.5% 미만(1px 미만)이고, 반투명 가장자리가 어두워지지 않았다(검은 테두리 없음).
- 코드:
  - `EditorBottomTabBar.kt` `EDITOR_CUTOUT_SAMPLE` 4 → 1. 자산이 예전 1/4 decode와 같은 314²라 다시 줄이면 이중 축소로 흐려지기 때문이다.
  - `VisitCalendarDrawer.kt`는 주석만 고쳤다(비율 상수 `1448f/1086f`는 새 192×144와 같은 4:3이라 유지).
- 제외(손대지 않음): quick select 손 5장, `gallery_date_paper_strip`, `editor_label_peek`, 도장 실루엣, 종이 texture, 보호 fiber 타일.

| 자산 | 이전 px | 이후 px | 이전 파일 | 이후 파일 | Bitmap(원본 decode 기준) | 품질 QA |
|---|---|---|---|---|---|---|
| editor_cutout_photo | 1254² | 314² | 1,710,104 B | 104,109 B | 변화 없음(전에도 1/4 decode 약 0.39MB) | 이전과 같음 |
| editor_cutout_background | 1254² | 314² | 1,893,715 B | 110,767 B | 같음 | 이전과 같음 |
| editor_cutout_text | 1254² | 314² | 992,104 B | 61,557 B | 같음 | 이전과 같음 |
| editor_cutout_sticker | 1254² | 314² | 1,813,896 B | 107,720 B | 같음 | 이전과 같음 |
| editor_cutout_tape | 1254² | 314² | 1,601,032 B | 108,102 B | 같음 | 이전과 같음 |
| editor_cutout_stamp | 1254² | 314² | 1,538,768 B | 96,600 B | 같음 | 이전과 같음 |
| editor_cutout_doodle | 1254² | 314² | 865,022 B | 61,085 B | 같음 | 이전과 같음 |
| home_clock_collage_body | 1695×928 | 720×394 | 2,238,553 B | 413,934 B | 6.29 → 1.13MB | 이전과 같음 |
| home_clock_left_snooze_hand | 1448×1086 | 480×360 | 1,721,991 B | 148,640 B | 6.29 → 0.69MB | 이전과 같음 |
| home_right_hand_coffee_cup | 1086×1448 | 450×600 | 2,361,108 B | 341,300 B | 6.29 → 1.08MB | 이전과 같음 |
| visit_calendar_newspaper_ribbon | 1448×1086 | 192×144 | 857,288 B | 15,709 B | 6.29 → 0.11MB | 이전과 같음 |
| **합계** | | | **17,593,581 B** | **1,569,523 B** | 갤러리 약 −22.6MB(계산) | 실기기 육안 QA 통과(사용자) |

- 크기 기준: 시계 3장과 리본은 450dpi 표시 px의 약 1.4배 이상이고, 4x 기기에서도 1배 이상이다. cutout은 원래 화면에 그려지던 해상도와 같다.
- 104일차 tracked 앱 이미지 원본 합계: 39,938,015 → **23,913,957 B**(보호 untracked 포함 26,911,598 B).

### APK Before → After

같은 HEAD(`a8603ca`) 작업트리, 같은 로컬 조건(보호 fiber 타일 포함)에서 빌드했다.

| | Before | After | 감소 |
|---|---|---|---|
| release unsigned 파일 | 59,547,037 B | 44,610,113 B | **−14,936,924 B(−14.94MB, −25.1%)** |
| release 안 PNG 항목 합계 | 40,803,057 B | 25,866,133 B | −14.94MB |
| debug 실제 항목 합계 | 69,891,485 B | 53,867,425 B | **−16.02MB** |
| debug 파일 | 70,053,840 B | 70,053,840 B | 0(아래 참고) |

- debug는 증분 패키징이라 지운 자리가 빈 공간(약 16.2MB)으로 남아 파일 크기가 그대로다. 새로 패키징하면 내용물만큼 줄어든다(실제 항목 합계 기준).
- release에서 앱 이미지 비중: 약 68.4% → 약 57.8%(추정: 103일차 40.71MB − 14.94MB ≈ 25.77MB / 44.61MB).

### 실기기 메모리 After (같은 조건 재측정)

| 항목 | 값 |
|---|---|
| 기기·밀도 | SM-S936N 1대(`adb devices -l`), 450dpi |
| 빌드 | 104일차 작업트리 debug. 사용자가 Android Studio로 덮어쓰기 설치(`lastUpdateTime 2026-10-15 12:40`, `firstInstallTime 2026-10-05` 유지 = 앱 삭제 없음) |
| 방식·경로 | 8절과 같다. 3초 간격 `dumpsys meminfo`, 사용자 marker, 화면마다 약 20초, 같은 경로 2바퀴 |
| 샘플 | 203개(측정 구간 marker 1~16) |
| 프로세스 | 측정 구간 내내 PID **11268** 유지. 측정기를 켠 직후, marker 1 전(12:43:52~55)에 PID가 6333에서 11268로 바뀌었다. 측정 시작 전이라 기준 PID를 11268로 잡았다. 오류·ANR 표시는 보고되지 않았다 |
| 안전 조건 | 8절과 같은 읽기 전용 명령만 썼다. install은 사용자가 Android Studio로 직접 했다. 측정용 데이터 생성 없음. 로그는 PC scratchpad에만 있다 |
| 참고 | 이번에는 TOTAL SWAP PSS가 약 30KB로 작아서, 8절보다 PSS 비교가 덜 흔들린다 |

| 구간 | Before(8절) Bitmap | **After Bitmap** | Before Native Alloc | **After Native Alloc** | Before Graphics | **After Graphics** |
|---|---|---|---|---|---|---|
| 갤러리 기준 | 37 / 72.0MB | **37 / 50.9MB(52,104 KiB)** | 90.4MB | **69.3MB** | 119.2MB | **103.7MB** |
| 바로가기 부채 | 72.0MB(변화 0) | 50.9MB(변화 0) | 90.5MB | 69.4MB | 119.2MB | 103.7MB |
| 편집 화면 | 37 / 13.8MB | **37 / 13.9MB(14,187 KiB)** | 35.6MB | 35.5MB | 163.3MB | 148.7MB |
| 도장 패널 | 47 / 13.9MB | 47 / 13.9MB(14,201 KiB) | 35.8MB | 35.7MB | 155.4MB | 140.2MB |
| 갤러리 복귀 | 55 / 75.4MB | **55 / 54.3MB(55,567 KiB)** | 97.2MB | **75.8MB** | 126.2MB | 114.1MB |
| 방문 달력 열기 | 75.4MB(변화 0) | 54.3MB(변화 0) | 98.0MB | 76.4MB | 152.8MB(+26.6) | 139.3MB(**+25.2**) |
| 달력 닫음 | 75.4MB | 54.3MB | 97.5MB | 76.2MB | 145.3MB(+19 잔존) | 125.4MB(**+11.3 잔존**) |
| 2바퀴 편집 / 갤러리 복귀 | 13.8 / 75.4MB | 13.9 / 54.3MB | 36.3 / 97.5MB | 36.3 / 76.2MB | 163.8 / 128.6MB | 162.1 / 131.4MB |
| 2바퀴 달력 열기 / 닫음 | — | 54.3 / 54.3MB | — | 76.5 / 76.3MB | — | 143.9 / 134.1MB |

### 결론

- **갤러리 Bitmap 72.0MB → 50.9MB(−21.1MB, −29%).**
  - 계산 예상(시계 −15.3MiB + 리본 −5.9MiB = −21.2MiB)과 맞는다.
  - Native Heap Alloc도 같은 만큼(−21.1MB) 줄었다.
- **편집 화면 Bitmap 13.8 → 13.9MB**로 사실상 같다. cutout은 decode 해상도를 일부러 똑같이 맞췄기 때문이다. 정적 캐시도 +3,463 KiB로 103일차(+3,447 KiB)와 거의 같다(313² → 314² 차이).
- **달력:** 열 때 Bitmap 변화는 0(그대로). Graphics 증가는 +26.6 → +25.2MB로 비슷하다. 닫은 뒤 잔존은 +19 → +11.3MB로 줄었다. GPU 캐시 쪽이라 원인은 계속 미추적이다.
- **2바퀴 반복:** Bitmap(54.3MB)과 Native Alloc(약 76MB)이 1·2바퀴 같은 구간에서 같다. **반복 증가(누수성) 징후 없음.**
  - Graphics는 2바퀴째 갤러리 복귀 때 114.1 → 131.4MB로 높게 남았다. 8절 Before에서도 126.2 → 128.6MB로 비슷한 GPU 캐시 잔존이 있었다. 달력을 연 뒤 남는 GPU 캐시로 보이며, 계속 늘어나는 모습은 아니다(2바퀴 달력 닫음 134.1MB).
- **화면 전환 순간:**
  - 편집 → 갤러리 전환 중 샘플 1개에서 Bitmap 66,305 KiB가 잡혔다. 편집 그림과 갤러리 그림이 잠깐 함께 있는 순간이고, 다음 샘플에서 55,567 KiB로 돌아왔다.
  - 갤러리 → 편집 전환 중에도 44,915 KiB 같은 중간값이 잡혔다. 두 경우 모두 일시적이다.
- 남은 갤러리 상주 큰 그림(제외 자산): 바로가기 손 5장 약 30MB, 종이 타일 약 6MB, 월 라벨 띠 약 6MB, 달력 종이 약 6MB.

## 9-2. 104일차 추가: quick select 손 5장 (D안) 결과 (2026-10-15)

### 바꾼 것

- 손 5장의 1254² canvas 전체를 그대로 **720²**로 비례 축소했다. 방법은 9절과 같다.
  - trim·crop·위치·색·필터는 바꾸지 않았다.
  - 알파 bbox 차이는 0.1% 미만이고, 손목 잘린 면은 그대로 오른쪽 끝에 있다.
- **`QUICK_SELECT_HAND_SOURCE_PX = 1254f`는 그대로 두고, 의미를 "논리 좌표 단위"로 주석에 적었다.**
  - 블록 좌표 20개, 터치 영역, 회전축, 금색 테두리, 부채 geometry·애니메이션 코드는 변경이 없다.
  - Bitmap의 실제 px를 읽는 코드가 없어서 그림만 180dp 상자에 맞춰 그려진다.

| 손 | 이전 px | 이후 px | 이전 파일 | 이후 파일 | Bitmap(계산) |
|---|---|---|---|---|---|
| camera | 1254² | 720² | 1,129,528 B | 374,099 B | 6,142.6 → 2,025 KiB |
| letter | 1254² | 720² | 1,374,450 B | 458,394 B | 같음 |
| river | 1254² | 720² | 1,131,731 B | 371,354 B | 같음 |
| sheep | 1254² | 720² | 1,342,899 B | 444,818 B | 같음 |
| checker | 1254² | 720² | 1,317,522 B | 447,014 B | 같음 |
| **합계** | | | **6,296,130 B** | **2,095,679 B** | 30,713 → 10,125 KiB(−20,588 KiB) |

### 실기기 QA (사용자, 통과)

- 닫힌 손잡이 묶음, 펼침 애니메이션, 5장 화질(종이 블록·손가락·오림 경계·인쇄 디테일), 손 5개 각각 터치·기능 실행, 선택 금색 테두리 위치를 확인했다.
- 모두 **이전과 같았다.**
- 이 항목들은 자동 테스트 공백(TEST GAP)이라 육안 QA로 확인했다.

### APK

| | 1차 다이어트 후 | 손 축소 후 | 감소 |
|---|---|---|---|
| release unsigned | 44,610,113 B | **40,557,565 B** | **−4,052,548 B(−4.05MB)** |
| debug 실제 항목 합계 | 53,867,425 B | 49,667,010 B | −4.20MB |

- 103일차부터 합치면 release는 59,547,037 → 40,557,565 B(**−18.99MB, −31.9%**)다.

### 실기기 메모리 (같은 방식, 3번째 측정)

- 조건: SM-S936N, 450dpi, 사용자가 Android Studio로 덮어쓰기 설치(`lastUpdateTime 13:21`, `firstInstallTime 10-05` 유지).
- 3초 간격 측정, 같은 경로 2바퀴, 샘플 130개. **PID 3260이 처음부터 끝까지 유지됐다.** 승인된 읽기 전용 adb만 썼다.

| 구간 | 103일차 | 104일차 1차 | **손 축소 후** |
|---|---|---|---|
| 갤러리 Bitmap | 72.0MB | 50.9MB | **30.8MB(31,516 KiB, 계산 예상과 정확히 일치)** |
| 갤러리 Native Alloc | 90.4MB | 69.3MB | **50.5MB** |
| 갤러리 Graphics | 119.2MB | 103.7MB | 83.4MB |
| 편집 Bitmap | 13.8MB | 13.9MB | 13.9MB |
| 갤러리 복귀 Bitmap | 75.4MB | 54.3MB | **34.2MB**(정적 캐시 +3,463 KiB 그대로) |
| 갤러리 복귀 Native Alloc | 97.2MB | 75.8MB | 58.2MB |

- **부채 닫힘 / 펼침:**
  - 1바퀴: Bitmap 30.8 / 30.8MB(변화 0), Native 50.5 / 50.6MB.
  - 2바퀴: Bitmap 34.2 / 34.2MB, Native 58.3 / 58.3MB.
  - "닫힌 상태에서 이미 5장 decode, 펼쳐도 추가 decode 없음" 구조가 그대로다.
- **2바퀴 반복:** 같은 구간의 Bitmap(34.2MB)과 Native Alloc(약 58.3~59.7MB)이 1·2바퀴에서 같다. **누수성 증가 없음.**
  - Graphics(GPU 캐시)는 달력을 연 뒤 높게 남는 기존 패턴과 같다. 갤러리 복귀 값은 1바퀴 92.7MB, 2바퀴 112.6MB였다.
  - 달력 열기 증가폭은 1바퀴 +32.5MB, 2바퀴 +10.4MB로 측정마다 흔들린다. 원인은 계속 미추적이다.
- 화면 전환 순간 샘플에 24,326~45,703 KiB 같은 중간값이 잡혔지만, 다음 샘플에서 안정값으로 돌아왔다(일시적).
- 남은 갤러리 상주 큰 그림(약 6MB씩): 종이 타일, 월 라벨 띠, 달력 종이.
