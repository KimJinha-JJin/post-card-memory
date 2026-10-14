# IMAGE-ASSET-MAP — 앱 이미지 자원 지도

조사일: 2026-10-14(103일차), 기준 commit `d9d7a0a`(`feature/photo-sticker`). 조사 + 문서만 했고 이미지·코드는 바꾸지 않았다.
이 문서는 "이 그림 파일은 무엇이고, 어디서 어떻게 쓰이며, 바꾸면 무엇이 같이 바뀌는가"를 찾는 지도다. 실제 상태는 항상 코드와 Git이 기준이며, 자산·코드가 바뀌면 해당 행을 다시 실측한다.

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
| home_clock_collage_body | nodpi | 1695×928 RGBA | 2.24 / 2.18MB | 갤러리 상단 레트로 시계 몸체 | `GalleryRetroClock.kt` `GalleryRetroClock` | 단일 | `painterResource`(원본 decode) | 큼 | 6.29MB(갤러리 상주, **실측 확인: 시계 3장 약 18MB**, 8절) | APK-OPT, MEM-OPT |
| home_clock_left_snooze_hand | nodpi | 1448×1086 RGBA | 1.72 / 1.67MB | 시계 위 왼손 | 같음 | 단일 | `painterResource` | 큼 | 6.29MB(상주) | APK-OPT, MEM-OPT |
| home_right_hand_coffee_cup | nodpi | 1086×1448 RGBA | 2.36 / 2.29MB | 시계 옆 커피잔 든 오른손(김 애니메이션 아래) | 같음 | 단일 | `painterResource`, `requiredSize` 후 위쪽 108dp만 보이게 clip | 큼 | 6.29MB(상주) | APK-OPT, MEM-OPT |
| quick_select_camera_hand | nodpi | 1254² RGBA | 1.13 / 1.09MB | 갤러리 바로가기 부채: 카메라 | `GalleryScreen.kt` `handResId()` → `GalleryQuickSelectHand` | 단일 | `ImageBitmap.imageResource` | 중 | 6.29MB(**닫힌 상태에서도 항상 그려져 상주**. 실측: 부채를 열어도 Bitmap 변화 0, 5장 약 30MB, 8절) | MEM-OPT |
| quick_select_letter_hand | nodpi | 1254² RGBA | 1.37 / 1.33MB | 부채: 미래 우체통 | 같음 | 단일 | 같음 | 중 | 6.29MB(상주) | MEM-OPT |
| quick_select_river_hand | nodpi | 1254² RGBA | 1.13 / 1.09MB | 부채: 연못 놀이 | 같음 | 단일 | 같음 | 중 | 6.29MB(상주) | MEM-OPT |
| quick_select_sheep_hand | nodpi | 1254² RGBA | 1.34 / 1.30MB | 부채: 양 목장 놀이 | 같음 | 단일 | 같음 | 중 | 6.29MB(상주) | MEM-OPT |
| quick_select_checker_hand | nodpi | 1254² RGBA | 1.32 / 1.28MB | 부채: 경주 놀이 | 같음 | 단일 | 같음 | 중 | 6.29MB(상주) | MEM-OPT |
| gallery_pick_hand | nodpi | 1254² RGBA | 1.42 / 1.38MB | 흔들기 → 랜덤 엽서 오버레이에서 엽서 귀퉁이를 집은 손 | `GalleryRandomPostcardOverlay.kt` `GalleryRandomPostcardOverlay` | 단일 | `imageResource` → Canvas `drawImage` | 중 | 6.29MB(오버레이가 떠 있는 동안) | KEEP |

### 2-2. 방문 달력(갤러리 왼쪽 drawer)

| 자산 | 경로 | 픽셀 | 파일 / APK | 사용 화면·기능 | 코드 참조 | 재사용 | 로딩 | APK 영향 | 메모리 영향 | 상태 |
|---|---|---|---|---|---|---|---|---|---|---|
| visit_calendar_paper | nodpi | 1122×1402 RGB 불투명 | 2.43 / 2.34MB | 달력 월 페이지 종이, 연·월 선택(picker) 종이 조각 2곳 | `VisitCalendarDrawer.kt` `VisitCalendarDrawer`(1회 load) → `VisitCalendarMonthPage`, `visitCalendarPickerPaperScrap` | 달력 안 3곳 | `imageResource`, cover-crop `ImageShader`, 조각은 seed로 무작위 crop | 큼 | 6.29MB, **갤러리에 상주**(실측 정정: drawer가 닫혀 있어도 이미 decode됨, 8절) | SHARED, KEEP |
| visit_calendar_newspaper_ribbon | nodpi | 1448×1086 RGBA | 0.86 / 0.74MB | 달력 아래 양끝 신문 오림 리본 | `VisitCalendarDrawer.kt` `VisitCalendarBottomOrnament`(한 번 decode해 2곳에 그림) | 같은 줄 2곳 | `imageResource`, 폭 24dp, 원본 비율 상수 `1448f/1086f` | 낮음 | 6.29MB, **갤러리에 상주**(실측 정정, 8절) | MEM-OPT |

> **실측 정정(2026-10-14):** 달력 종이·리본은 달력을 열 때 decode되는 것이 아니다. 갤러리 화면에 있는 동안 이미 상주한다. 달력을 열 때 Bitmap 변화는 0이고, 늘어나는 것은 GPU 쪽(Graphics 약 +26.6MB)이다. 닫은 뒤에도 Graphics 약 +19MB가 남는다. drawer 내용이 닫혀 있는 동안에도 구성되기 때문으로 보인다(코드 원인 추적은 미실시).

### 2-3. 편집 화면 — 상위 기능줄·하단 세부 선택줄

| 자산 | 경로 | 픽셀(그림 영역) | 파일 / APK | 사용 화면·기능 | 코드 참조 | 재사용 | 로딩 | APK 영향 | 메모리 영향 | 상태 |
|---|---|---|---|---|---|---|---|---|---|---|
| editor_cutout_photo | nodpi | 1254² RGBA(950×697) | 1.71 / 1.55MB | 상위 기능줄 "사진" | `DetailScreen.kt` `customizationPageIcons` → `EditorBottomTabBar.kt` `loadEditorCutout` | 단일 | `decodeResource` + `inSampleSize 4`(약 313²), 알파 bbox로 자동 자르기, **process 정적 캐시** | 큼 | 약 0.39MB(상주) | APK-OPT |
| editor_cutout_background | nodpi | 1254² RGBA(974×714) | 1.89 / 1.68MB | "배경" | 같음 | 단일 | 같음 | 큼 | 0.39MB | APK-OPT |
| editor_cutout_text | nodpi | 1254² RGBA(911×570) | 0.99 / 0.89MB | "글귀" | 같음 | 단일 | 같음 | 중 | 0.39MB | APK-OPT |
| editor_cutout_sticker | nodpi | 1254² RGBA(988×1035) | 1.81 / 1.67MB | "스티커" | 같음 | 단일 | 같음 | 큼 | 0.39MB | APK-OPT |
| editor_cutout_tape | nodpi | 1254² RGBA(1044×784) | 1.60 / 1.49MB | "테이프" | 같음 | 단일 | 같음 | 큼 | 0.39MB | APK-OPT |
| editor_cutout_stamp | nodpi | 1254² RGBA(728×1118) | 1.54 / 1.41MB | "도장" | 같음 | 단일 | 같음 | 큼 | 0.39MB | APK-OPT |
| editor_cutout_doodle | nodpi | 1254² RGBA(1011×815) | 0.87 / 0.81MB | "낙서" | 같음 | 단일 | 같음 | 중 | 0.39MB | APK-OPT |
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
| quick_select_* 5장 | `GalleryQuickSelectFan.kt` 블록 좌표 | 1 | 5 | **강한 결합**(원본 px 좌표) | 블록 사각형 좌표가 원본 1254px 기준 숫자(`QUICK_SELECT_HAND_SOURCE_PX = 1254f`)다. 해상도·구도를 바꾸면 블록 테두리(활성 표시)와 터치 영역 좌표를 다시 재야 한다 |
| editor_label_peek | `PaperLabelSubcategoryNav.kt` crop 상수 | 1 | 1 | **강한 결합**(원본 px 좌표) | crop `(376,352)/720×656`과 `inSampleSize 4`가 짝이다 |
| editor_cutout_* 7장 | `EditorBottomTabBar.kt` | 1 | 7 | 단일 사용, 약한 결합 | 알파 bbox를 자동으로 잘라 32×24dp 상자에 맞춘다(좌표 상수 없음). 단 `EDITOR_CUTOUT_SAMPLE = 4`가 원본 1254px을 전제로 한 값이다 |
| visit_calendar_paper | 1파일, 3곳 | 1 | 2(월 페이지, picker 조각) | 공용(달력 안) | 페이지는 cover-crop, 조각은 seed로 무작위 crop |
| visit_calendar_newspaper_ribbon | 1파일, 2곳 | 1 | 1 | 단일(한 번 decode해 공유) | 비율 상수 `1448f/1086f`가 원본 비율과 짝이다 |
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
| editor_cutout_*(7) | 1254²(그림 약 700~1050) | 최대 32×24dp 상자 | 96×72 | 약 313²(1/4) | 원본 기준 약 10배, decode 기준 약 3배 | **APK 과대**(RAM은 이미 줄여 decode 중) |
| editor_label_peek | 720×656 crop | 높이 20dp | 약 66×60 | 1/4 → crop 180×164 | decode 기준 약 2.7배 | APK 과대 |
| gallery_date_paper_strip | 2011×379 crop | 최소 136dp × 약 29dp(월 라벨) / 높이 25dp(선택줄) | 약 408×87 | 갤러리: **원본 2172×724** / 선택줄: 543×181 | 갤러리 약 4.9배 | **갤러리 RAM 과대** |
| home_clock_collage_body | 1695×928 | 168×92dp | 504×276 | 원본 | 3.4배 | 과대 |
| home_clock_left_snooze_hand | 1448×1086 | 112×84dp | 336×252 | 원본 | 4.3배 | 과대 |
| home_right_hand_coffee_cup | 1086×1448 | 105×140dp(위 108dp만 보임) | 316×421 | 원본 | 3.4배 | 과대 |
| quick_select_*(5) | 1254² | 180dp 정사각 | 540 | 원본 | 2.3배 | 과대(5장 상주) |
| visit_calendar_newspaper_ribbon | 1448×1086(리본 948×463) | 24×18dp(실제 리본 약 16×8dp) | 72×54 | 원본 | **약 20배** | 크게 과대 |
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
| **A** | editor_cutout_* 7장 | APK 9.5MB를 32×24dp에 쓴다. 알파 bbox 자동 자르기라 좌표 상수가 없다. 화면은 1곳 | APK 약 −8~9MB(1/4 축소 기준). RAM 변화 없음 | `EDITOR_CUTOUT_SAMPLE`을 새 해상도에 맞춰야 한다(그대로 두면 이중 축소로 흐려짐). 상위 기능줄 7개를 육안 QA |
| **A** | home_clock_* 3장 | APK 6.1MB, 3.4~4.3배 과대, **갤러리 첫 화면에 상주**(실측 Bitmap 약 18MB, 8절). 표시 크기가 dp 상수라 원본 px에 묶이지 않는다 | APK 약 −4~5MB, RAM 약 −14MB(표시 px의 약 1.3~1.5배로 축소 시) | 비율 유지 필수(숫자창 위치가 dp로 몸체 비율에 맞춰져 있음). 갤러리 상단 육안 QA(하프톤 결) |
| **A** | visit_calendar_newspaper_ribbon | 약 20배 과대. **갤러리에 상주하는 6.3MB**(실측 정정: drawer가 닫혀 있어도 decode됨) | RAM 약 −6MB, APK 약 −0.7MB | 비율 `1448f/1086f` 유지 또는 상수 갱신. 육안 QA는 짧게 |
| **B** | quick_select_* 5장 | 갤러리에 상주하는 RAM 31.5MB(가장 큼, 실측 Bitmap 약 30MB, 부채를 열어도 변화 0), 2.3배 과대 | RAM 약 −20MB 이상, APK 약 −3~4MB | 블록 좌표가 원본 px(1254) 기준이라 자산을 줄이면 `QUICK_SELECT_HAND_SOURCE_PX`와 블록 20개 숫자를 함께 바꾸거나, 자산은 두고 decode만 줄이는 코드 경로가 필요하다. 부채 펼침·활성 테두리·터치 영역 실기기 QA |
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
