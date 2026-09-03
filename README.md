<div align="center">

# 📮 PostCard Memory

**오늘의 순간을 엽서로 남기고, 꾸미고, 미래의 나에게 보내는 Android 앱**

`사진 한 장 → 나만의 엽서 → 시간이 지나 다시 만나는 추억`

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Compose%20BOM-2026.04-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room-2.8.4-FF6F00?style=flat-square)](https://developer.android.com/training/data-storage/room)

</div>

---

> [!IMPORTANT]
> **현재 적극적으로 개발 중인 브랜치는 `feature/photo-sticker`입니다.**  
> 이 README는 최신 작업 브랜치의 앱 상태를 기준으로 작성되어 있으며, `main`의 소스 코드는 일부 기능이 아직 합쳐지지 않아 차이가 있을 수 있습니다.

## ✦ 어떤 앱인가요?

PostCard Memory는 사진을 단순히 보관하는 대신 **한 장의 엽서처럼 완성해 추억을 수집하는 로컬 기반 갤러리 앱**입니다.

처음에는 카메라로 촬영한 순간을 우표 프레임에 담아 모으는 작은 프로젝트로 시작했습니다.  
지금은 사진 레이아웃과 배경을 고르고, 스티커·마스킹테이프·도장·낙서로 앞면을 꾸미고, 뒷면에는 그날의 나에게 편지를 적을 수 있는 **디지털 스크랩북형 엽서 편집기**로 확장되고 있습니다.

완성한 엽서는 바로 간직할 수도 있고, 날짜를 정해 **미래 우체통**으로 보낼 수도 있습니다.

---

## ✦ 핵심 경험

### 📸 순간을 엽서로 수집
카메라로 직접 촬영하거나 사진을 이용해 추억 한 장을 엽서로 저장합니다.  
갤러리에서는 카드들이 손으로 붙여둔 스크랩북처럼 모여 보여요.

### 🖼 사진 레이아웃
현재 네 가지 레이아웃을 지원합니다.

- **우표**
- **폴라로이드**
- **테이프 필름**
- **편지지**

레이아웃에 맞춰 사진의 위치·확대 정도와 가장자리 블러를 조절할 수 있습니다.

### 🎨 배경 꾸미기
엽서의 바탕도 콘텐츠의 일부로 다룹니다.

- 파스텔 프리셋 색상
- 사용자 지정 색상
- 사진에서 색상 추출
- 다양한 반복 패턴
- 패턴 밀도 조절

### ✂️ 스티커
스티커는 현재 세 가지 종류로 나뉩니다.

**사진 스티커**
- 갤러리 / 파일 / 카메라에서 추가
- 이동·확대·회전·대칭
- 앞/뒤 레이어 순서 조절
- ML Kit Subject Segmentation 기반 배경 제거

**텍스트 스티커**
- 짧은 문구를 장식 요소처럼 배치
- 글자색과 외곽선 색상 선택
- 이동·확대·회전 편집

**라벨 스티커**
- 라벨 프린터/다이모 테이프 같은 제한된 디자인 문법
- 문구와 테이프 스타일 중심으로 빠르게 생성·편집

### 🩹 마스킹테이프
실제 다꾸에서 쓰는 마스킹테이프처럼 엽서 위에 붙일 수 있습니다.

- 기본 디자인
- 커스텀 색상·패턴
- 사진으로 만든 테이프
- 가장자리 모양·길이·굵기·회전 조절
- 복제·삭제·Undo/Redo

### ✍️ 낙서
엽서 위에 직접 그릴 수 있는 도구형 꾸미기 기능입니다.

- 펜
- 형광펜
- 점선
- 지우개
- 색상·굵기 조절
- Undo/Redo 및 전체 지우기

### ✉️ 우편 도장
우편 소인에서 가져온 도장을 추가해 엽서의 분위기를 마무리할 수 있습니다.

도장의 색상·크기·회전·위치를 편집할 수 있으며, 한 장의 엽서에는 최대 2개의 도장을 배치할 수 있습니다.

### ↩️ Undo / Redo
사진 스티커, 텍스트·라벨 스티커, 마스킹테이프, 도장, 낙서 등 주요 꾸미기 도구는 편집 내역을 되돌리고 다시 실행할 수 있습니다.

---

## ✦ 엽서의 앞면과 뒷면

PostCard Memory의 엽서는 **꾸미는 앞면**과 **편지를 쓰는 뒷면**을 구분합니다.

앞면에서는 사진과 장식 요소를 자유롭게 편집하고, 뒷면에서는 보다 실제 엽서에 가까운 형태로 메시지를 남깁니다.

```text
To. [수식언] 나

오늘의 나에게 하고 싶은 말을 적어봐.

From. yyyy-MM-dd의 나
```

뒷면은 장식 기능을 얹기보다 **글을 읽고 남기는 공간**에 집중하도록 설계되어 있습니다.

---

## ✦ 📮 미래 우체통

완성한 엽서는 미래의 날짜를 정해 보낼 수 있습니다.

- 발송된 엽서는 일반 갤러리에서 숨겨집니다.
- 미래 우체통에는 **도착 날짜·건수·진행도**만 표시됩니다.
- 배송 중에는 사진이나 편지 내용이 노출되지 않습니다.
- 지정한 날짜가 되면 엽서를 다시 열어볼 수 있습니다.

단순한 알림 기능보다, **한동안 보지 못했던 기억을 실제 편지처럼 다시 만나는 경험**을 목표로 합니다.

---

## ✦ 저장 · 공유

꾸민 엽서는 화면의 미리보기 상태와 최대한 동일하게 이미지로 렌더링해 저장하거나 공유할 수 있습니다.

사진과 엽서 데이터는 기본적으로 앱 내부 저장소와 Room 데이터베이스에 보관됩니다.  
사진 선택에는 Android Photo Picker를 활용하며, 앱 자체적으로 서버에 추억 데이터를 업로드하는 기능은 두고 있지 않습니다.

---

## ✦ 디자인 방향

현재 UI의 중심은 초기의 강한 네오 브루탈리즘보다 **따뜻한 종이와 실제 다꾸 도구의 문법**에 가깝습니다.

> 해질녘 노을빛이 밴 오래된 종이 위에 사진과 테이프, 잉크를 하나씩 올리는 느낌.

| 역할 | 색상 |
|---|---|
| Paper Canvas | `#F4ECDE` |
| Paper Tray | `#E8DCC6` |
| Paper Surface | `#FFFBF3` |
| Paper Field | `#FAF4E9` |
| Ink Primary | `#1A1324` |
| Ink Secondary | `#5B5046` |
| Sunset Gold | `#8C5F00` |
| Sunset Coral | `#FF7F8E` |

엽서 콘텐츠 자체에는 민트·옐로·핑크·블루 등의 파스텔 색을 별도로 사용합니다.

편집 UI에서는 불필요하게 모든 요소를 둥근 사각형 상자 안에 넣기보다, **실제 역할이 다른 곳에만 경계와 컨테이너를 쓰는 방향**으로 계속 다듬고 있습니다.

---

## ✦ 화면 흐름

```text
                         ┌──────────────────┐
                         │   📮 갤러리      │
                         │  추억 카드 모음   │
                         └────────┬─────────┘
                                  │
                    촬영 / 카드 열기
                                  │
                                  ▼
                  ┌──────────────────────────┐
                  │       엽서 상세 화면      │
                  │                          │
                  │   앞면 ⇄ 뒷면            │
                  │                          │
                  │ 사진 · 배경 · 텍스트      │
                  │ 스티커 · 테이프 · 낙서     │
                  │ 도장                     │
                  └───────┬─────────┬────────┘
                          │         │
                    저장·공유     미래로 보내기
                          │         │
                          ▼         ▼
                       갤러리   📮 미래 우체통
```

---

## ✦ 기술 스택

| 영역 | 기술 |
|---|---|
| 언어 | Kotlin 2.3.10 |
| UI | Jetpack Compose, Material 3 |
| 카메라 | CameraX 1.6.1 |
| 이미지 로딩 | Coil 2.6.0 |
| 배경제거 | ML Kit Subject Segmentation |
| 사진 색상 추출 | AndroidX Palette |
| 데이터베이스 | Room 2.8.4 |
| 의존성 주입 | Hilt 2.59.2 |
| 네비게이션 | Navigation Compose 2.7.7 |
| 권한 처리 | Accompanist Permissions 0.34.0 |
| 빌드 | AGP 9.2.1, KSP 2.3.2, JDK 17 |
| 최소 Android | API 26 (Android 8.0) |

---

## ✦ 프로젝트 구조

```text
app/src/main/java/com/postcardmemory/
│
├── data/
│   ├── Postcard.kt
│   ├── PostcardDao.kt
│   ├── PostcardDatabase.kt
│   └── PostcardRepository.kt
│
├── di/
│   └── DatabaseModule.kt
│
├── ui/
│   ├── camera/          # 촬영
│   ├── gallery/         # 갤러리
│   ├── detail/          # 엽서 상세·편집
│   ├── futuremail/      # 미래 우체통
│   ├── components/      # 엽서·편집 공용 컴포넌트
│   └── theme/           # 웜 페이퍼 디자인 토큰
│
├── utils/
│   ├── ImageUtils.kt
│   ├── PhotoColorExtractor.kt
│   ├── PostcardImageExporter.kt
│   ├── PostcardRenderSpec.kt
│   └── ...
│
└── MainActivity.kt
```

Room 스키마는 버전별 마이그레이션 안전성을 확인할 수 있도록 별도 스키마 파일로 관리합니다.

---

## ✦ 빌드 & 실행

### 요구 사항

- 최신 Android Studio 권장
- JDK 17
- Android 8.0 (API 26) 이상 기기 또는 에뮬레이터

현재 최신 작업 상태를 확인하려면 개발 브랜치를 사용하세요.

```bash
git clone https://github.com/KimJinha-JJin/post-card-memory.git
cd post-card-memory
git checkout feature/photo-sticker
```

Android Studio에서 프로젝트를 열고 Gradle Sync 후 실행하면 됩니다.

> 현재 저장소에는 Gradle Wrapper 실행 파일이 포함되어 있지 않아, 별도 환경 설정 없이 `./gradlew` 명령을 바로 사용하는 방식보다 Android Studio 실행을 권장합니다.

---

## ✦ 필요 권한

| 권한 | 용도 |
|---|---|
| `CAMERA` | 엽서 사진 및 사진 스티커 촬영 |

갤러리 사진 선택은 시스템 Photo Picker를 사용하므로 일반적인 전체 저장소 접근 권한을 요구하지 않습니다.

---

## ✦ 개발 상태

이 프로젝트는 **계속 디자인과 기능을 다듬고 있는 개인 Android 프로젝트**입니다.

기능을 무작정 늘리기보다,

- 실제 엽서와 다꾸 도구의 사용 문법이 자연스러운지
- 편집 결과와 저장 이미지가 일치하는지
- Undo/Redo와 저장 과정에서 데이터가 안전한지
- 오래된 앱 데이터가 마이그레이션 뒤에도 유지되는지
- 작은 화면에서도 편집 흐름이 복잡해지지 않는지

를 반복해서 확인하며 발전시키고 있습니다.

<div align="center">

---

### 📮 모든 순간은 엽서가 될 수 있어요.

**PostCard Memory**

</div>
