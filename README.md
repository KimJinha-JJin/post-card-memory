<div align="center">

```
┌─────────────────────────────────────────┐
│                                         │
│   📮  포스트카드 메모리                    │
│       PostCard Memory                   │
│                                         │
│   추억을 우표처럼 수집하고 꾸며보세요        │
│                                         │
└─────────────────────────────────────────┘
```

[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose%20BOM-2026.04-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

</div>

---

## ✦ 앱 소개

포켓몬처럼 추억을 **수집**하는 갤러리 앱입니다.

카메라로 찍은 순간이 **우표 프레임**에 딱 맞게 잘려 저장되고,  
모인 카드들은 스크랩북처럼 제각각 살짝 기울어진 채로 진열돼요.

카드를 열면 **사진 · 배경 · 텍스트 · 스티커 · Seal** 5가지 페이지로 나뉜 편집 화면에서  
엽서 하나하나를 직접 꾸밀 수 있어요.

---

## ✦ 주요 기능

### 📸 우표 촬영
카메라 뷰파인더 가이드에 맞춰 사진을 찍으면, 우표 레이아웃에 맞게 잘려 저장됩니다.

### 🗂 스크랩북 갤러리
수집된 카드들이 **2열 그리드**로 펼쳐집니다.  
각 카드는 살짝 랜덤하게 기울어져 마치 손으로 붙여둔 것 같은 느낌을 줘요.

### 🎨 엽서 편집 (5페이지)
- **사진** — 우표 / 폴라로이드 / 테이프 필름 3가지 레이아웃, 사진 교체·가장자리 블러
- **배경** — 사진에서 색상 자동 추출 또는 커스텀 색상 지정, 패턴 8종(땡땡이·체크·사선무늬 등)
- **텍스트** — 글꼴과 날짜 표기 형식 선택
- **스티커** — 사진 스티커 추가, ML Kit 기반 배경제거(누끼), 이동·회전·크기·대칭·레이어 순서 편집
- **Seal** — 우편 소인 스타일 도장(원형·파도·에어메일·별) 색상과 배치 편집

### ↩️ Undo/Redo
스티커·Seal·사진 배치 편집을 각각 되돌리기/다시하기 할 수 있어요.

### 🗑 카드 상세 & 삭제
카드를 탭하면 큰 화면으로 감상할 수 있고, 필요 없는 추억은 삭제할 수 있어요.

---

## ✦ 디자인 시스템

**Neo-Brutalism** 감성의 굵은 검정 테두리와 오프셋 그림자에, 무채색 톤을 더한 **소프트 UI** 배경을 함께 씁니다.

| 용도 | 색상 |
|------|------|
| 기본 글자·외곽선 | `#1A1324` Brutal Black / `#FFFBFF` Brutal White |
| 포스트카드 파스텔 | `#8FFFDA` Mint · `#FF7F8E` Coral · `#FFE566` Yellow · `#FFB3E1` Pink · `#AAD4FF` Blue |
| 화면 배경·회색조 | `#EDEBF2` Screen BG · `#E0DFE5` Surface · `#F5F4F7` Soft Gray · `#CFCED6` Neutral Light |
| Seal 잉크 | 검정 · 빨강 · 네이비 · 세피아 · 그린 · 흰색 |

- **굵은 검정 테두리**와 **오프셋 그림자** — 네오 브루탈리즘의 핵심
- **랜덤 회전** (-3.5° ~ +3.5°) — 손으로 붙인 듯한 스크랩북 효과

---

## ✦ 화면 구성

```
갤러리 (2열 그리드)
   │
   ├─ 카메라 촬영 ──────────────┐
   │                           │
   └─ 기존 카드 탭 ─────────────┤
                                ▼
                     상세 편집 화면
              ┌───────────────────────────┐
              │ 사진 │ 배경 │ 텍스트 │ 스티커 │ Seal │
              └───────────────────────────┘
                                │
                                ▼
                          저장 → 갤러리
```

---

## ✦ 기술 스택

| 영역 | 기술 |
|------|------|
| UI | Jetpack Compose, Material3 |
| 카메라 | CameraX 1.6 |
| 이미지 로딩 | Coil 2.6 |
| 배경제거(누끼) | ML Kit Subject Segmentation |
| 사진 색상 추출 | AndroidX Palette |
| 데이터베이스 | Room 2.8 |
| 의존성 주입 | Hilt 2.59 |
| 권한 처리 | Accompanist Permissions 0.34 |
| 네비게이션 | Navigation Compose 2.7 |
| 빌드 | Kotlin 2.3, AGP 9.2, KSP |

---

## ✦ 프로젝트 구조

```
app/src/main/java/com/postcardmemory/
│
├── 📁 data/
│   ├── Postcard.kt          ← Room 엔티티
│   ├── PostcardDao.kt       ← DB 쿼리
│   ├── PostcardDatabase.kt
│   └── PostcardRepository.kt
│
├── 📁 di/
│   └── DatabaseModule.kt    ← Hilt 모듈
│
├── 📁 ui/
│   ├── 📁 theme/            ← Color.kt, Theme.kt, Type.kt
│   │
│   ├── 📁 components/
│   │   ├── StampCard.kt              ← 갤러리 카드 컴포넌트
│   │   ├── StampPhoto.kt             ← 핑킹 가위 사진 프레임
│   │   ├── PostcardLayoutPicker.kt   ← 사진 레이아웃(우표/폴라로이드/테이프 필름) 선택
│   │   ├── PostcardBackgroundPicker.kt ← 배경 색상·패턴 선택
│   │   ├── PostcardDateFormat.kt     ← 날짜 표기 형식 enum
│   │   ├── PostcardTextFont.kt       ← 글꼴 enum
│   │   ├── SealShapes.kt             ← Seal 도장 모양 드로잉
│   │   └── PhotoSourceMenu.kt        ← 사진 소스(카메라/갤러리) 메뉴
│   │
│   ├── 📁 gallery/          ← 홈 갤러리 화면
│   ├── 📁 camera/           ← 촬영 화면
│   └── 📁 detail/           ← 카드 상세·편집 화면
│       ├── DetailScreen.kt / DetailViewModel.kt
│       ├── PhotoStickerDetailScreen.kt / PhotoStickerItem.kt
│       └── PostcardSealDetailScreen.kt / PostcardSealItem.kt
│
├── 📁 utils/
│   ├── ImageUtils.kt            ← EXIF 보정 + 크롭
│   ├── PhotoColorExtractor.kt   ← 사진 색상 추출
│   ├── PostcardRenderSpec.kt    ← 엽서 미리보기 렌더링
│   ├── PostcardImageExporter.kt ← 엽서 이미지 내보내기
│   ├── PostcardImageStorage.kt
│   ├── BackgroundImageStorage.kt
│   └── PhotoStickerImageStorage.kt
│
└── MainActivity.kt
```

---

## ✦ 빌드 & 실행

**요구 사항**
- Android Studio (최신 버전 권장)
- JDK 17
- Android 8.0 (API 26) 이상 기기 또는 에뮬레이터

```bash
git clone https://github.com/KimJinha-JJin/post-card-memory.git
cd post-card-memory
```

Android Studio에서 프로젝트를 열고 Gradle Sync 후 실행하세요.  
(이 저장소에는 Gradle Wrapper가 포함되어 있지 않아 `./gradlew` 커맨드라인 빌드는 지원되지 않습니다.)

---

## ✦ 필요 권한

| 권한 | 용도 |
|------|------|
| `CAMERA` | 우표 사진 촬영 |

사진과 엽서 데이터는 앱 내부 저장소에만 저장되며 외부로 유출되지 않습니다.

---

<div align="center">

```
📮  모든 순간은 우표가 될 수 있어요  📮
```

</div>
