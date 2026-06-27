<div align="center">

```
┌─────────────────────────────────────────┐
│                                         │
│   📮  포스트카드 메모리                    │
│       PostCard Memory                   │
│                                         │
│   추억을 우표처럼 수집하세요                │
│                                         │
└─────────────────────────────────────────┘
```

[![Android](https://img.shields.io/badge/Android-26%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.02-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-FFE566?style=flat-square)](LICENSE)

</div>

---

## ✦ 앱 소개

포켓몬처럼 추억을 **수집**하는 갤러리 앱입니다.

카메라로 찍은 순간이 **우표 프레임**에 딱 맞게 잘려 저장되고,  
모인 카드들은 스크랩북처럼 제각각 살짝 기울어진 채로 진열돼요.  
네오 브루탈리즘 감성의 굵은 테두리와 파스텔 색상이 어우러진,  
아기자기하고 손에 잡힐 듯한 추억 컬렉션을 만들어보세요.

---

## ✦ 주요 기능

### 📸 우표 촬영
카메라 뷰파인더에 표시된 **우표 모양 가이드**에 맞춰 사진을 찍으면,  
자동으로 **3:4 비율**로 크롭되어 저장됩니다.

### 🗂 스크랩북 갤러리
수집된 우표들이 **2열 그리드**로 펼쳐집니다.  
각 카드는 살짝 랜덤하게 기울어져 마치 손으로 붙여둔 것 같은 느낌을 줘요.

### 🎨 퍼포레이션 테두리
실제 우표처럼 가장자리에 **반원형 구멍 톱니** 테두리가 그려집니다.  
Canvas로 직접 드로잉한 커스텀 컴포넌트예요.

### 🗑 카드 상세 & 삭제
카드를 탭하면 큰 화면으로 감상할 수 있고,  
필요 없는 추억은 삭제할 수 있어요.

---

## ✦ 디자인 시스템

**Neo-Brutalism × Kawaii** 두 가지 감성을 섞었습니다.

```
┌──────────────────────────────────────┐
│  COLOR PALETTE                       │
│                                      │
│  ██ #1A1A1A  Brutal Black            │
│  ██ #FFFDF5  Brutal White (warm)     │
│  ██ #8FFFDA  Brutal Mint       ✦     │
│  ██ #FF7F7F  Brutal Coral      ✦     │
│  ██ #D4AAFF  Brutal Lavender   ✦     │
│  ██ #FFE566  Brutal Yellow     ✦     │
│  ██ #FFB3D9  Brutal Pink       ✦     │
│  ██ #AAD4FF  Brutal Blue       ✦     │
│                                      │
│  ✦ = 카드 배경에 순환 적용             │
└──────────────────────────────────────┘
```

- **굵은 검정 테두리** (3dp stroke) — 네오 브루탈리즘의 핵심
- **오프셋 그림자** (4dp solid black) — 입체감을 주는 브루탈 그림자
- **랜덤 회전** (-3.5° ~ +3.5°) — 손으로 붙인 듯한 스크랩북 효과

---

## ✦ 화면 구성

```
┌─────────────────┐   tap camera   ┌─────────────────┐
│ 📮 포스트카드    │ ─────────────► │   카메라 화면    │
│                 │                │                 │
│ ┌───┐  ┌───┐   │                │  [우표 프레임   │
│ │🖼️ │  │🖼️ │   │                │   가이드 오버레이│
│ └───┘  └───┘   │                │                 │
│                 │                │    ⚪ 셔터      │
│ ┌───┐  ┌───┐   │                └─────────────────┘
│ │🖼️ │  │🖼️ │   │
│ └───┘  └───┘   │   tap card     ┌─────────────────┐
│                 │ ─────────────► │   카드 상세     │
│          [📷]   │                │                 │
└─────────────────┘                │  ┌───────────┐  │
                                   │  │           │  │
      갤러리                        │  │   우표    │  │
                                   │  │           │  │
                                   │  └───────────┘  │
                                   │  MM.DD.YYYY     │
                                   │         [🗑 삭제]│
                                   └─────────────────┘
```

---

## ✦ 기술 스택

| 영역 | 기술 |
|------|------|
| UI | Jetpack Compose, Material3 |
| 카메라 | CameraX 1.3.1 |
| 이미지 로딩 | Coil 2.6 |
| 데이터베이스 | Room 2.6 |
| 의존성 주입 | Hilt 2.51 |
| 권한 처리 | Accompanist Permissions 0.34 |
| 네비게이션 | Navigation Compose 2.7 |
| 빌드 | Kotlin 1.9, AGP 8.3, KSP |

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
│   ├── 📁 theme/
│   │   ├── Color.kt         ← 네오 브루탈 팔레트
│   │   ├── Type.kt
│   │   └── Theme.kt
│   │
│   ├── 📁 components/
│   │   ├── StampBorder.kt   ← 퍼포레이션 Canvas 드로잉
│   │   ├── StampCard.kt     ← 갤러리 카드 컴포넌트
│   │   └── StampOverlay.kt  ← 카메라 뷰파인더 가이드
│   │
│   ├── 📁 gallery/          ← 홈 갤러리 화면
│   ├── 📁 camera/           ← 촬영 화면
│   └── 📁 detail/           ← 카드 상세 화면
│
├── 📁 utils/
│   └── ImageUtils.kt        ← EXIF 보정 + 3:4 크롭
│
└── MainActivity.kt
```

---

## ✦ 빌드 & 실행

**요구 사항**
- Android Studio Hedgehog 이상
- JDK 17
- Android 8.0 (API 26) 이상 기기 또는 에뮬레이터

```bash
# 레포 클론
git clone https://github.com/KimJinha-JJin/post-card-memory.git
cd post-card-memory
git checkout claude/postcard-gallery-android-rg0uha

# Android Studio에서 열기
# 또는 커맨드라인 빌드
./gradlew assembleDebug

# APK 설치
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## ✦ 필요 권한

| 권한 | 용도 |
|------|------|
| `CAMERA` | 우표 사진 촬영 |

사진은 앱 내부 저장소(`filesDir/stamps/`)에만 저장되며  
외부로 유출되지 않습니다.

---

<div align="center">

```
📮  모든 순간은 우표가 될 수 있어요  📮
```

</div>
