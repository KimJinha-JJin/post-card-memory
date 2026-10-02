> **문서 상태:** 앱 소개는 2026-10-02(91일차) 코드 기준으로 갱신했어. 작업 규칙·현재 상태·승인 범위의 기준은 이 README가 아니라 [AGENTS.md](AGENTS.md), [현재 HANDOFF](docs/ai/HANDOFF.md), [유효한 결정](docs/ai/DECISIONS.md)이야.

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

찍은 사진을 **엽서 한 장**으로 만들어 모으는 Android 앱입니다.

카메라로 찍은 순간이 엽서 앞면이 되고, 뒷면에는 편지를 쓸 수 있어요.
엽서마다 스티커·마스킹테이프·도장·낙서·글귀로 손으로 꾸민 스크랩북처럼 꾸미고,
모인 엽서는 종이 위에 놓인 갤러리에서 달별로 다시 꺼내 봅니다.

모든 데이터는 **기기 안(앱 내부 저장소)** 에만 저장되고, 앱 소스에는 서버 통신이 없습니다.

---

## ✦ 주요 기능

### 📸 촬영과 엽서 앞면
- 카메라 가이드에 맞춰 찍으면 엽서 사진으로 잘려 저장됩니다.
- 사진 레이아웃 우표 / 폴라로이드 / 테이프 필름, 사진 확대·이동·가장자리 블러.
- 배경은 사진에서 뽑은 색·직접 고른 색·패턴, 추천 템플릿과 내 템플릿 저장.

### ✉️ 엽서 뒷면 — 편지
- 카드를 뒤집어 뒷면에 "To." 받는 이 표현·본문·추신을 씁니다. "From."에는 엽서를 만든 날짜의 내가 적힙니다.
- 작성 시각을 남기는 작성 기록(끌 수 있음).

### 🎨 꾸미기 (편집 탭: 사진 · 배경 · 글귀 · 스티커 · 테이프 · 도장 · 낙서)
- **사진 스티커** — ML Kit 배경 제거(누끼), 오림 스타일 기본 / 폴라로이드 / 가위 오림 / 찢은 종이 / 잡지 오림.
- **텍스트·라벨 스티커** — 글자 스티커와 라벨프린터로 뽑은 듯한 짧은 라벨.
- **마스킹테이프** — 기본 디자인·커스텀 색·사진 테이프.
- **도장** — 우편 소인 모양과 잉크 색, 엽서마다 다른 잉크 번짐 질감, 손이 찍어 주는 찍기 인터랙션.
- **낙서** — 펜·형광펜·점선·지우개.
- 꾸미기 요소는 끌어서 배치하고, 이동·회전·크기·레이어 순서를 바꾸며, 되돌리기/다시하기가 됩니다.

### 💾 저장 · 공유 · 내보내기
- 편집 중에는 초안이 자동 저장돼 앱이 꺼져도 이어서 편집할 수 있고, 완료 버튼으로 확정 저장합니다.
  확정 저장은 꾸미기 여섯 종이 모두 성공했을 때만 초안을 지웁니다.
- 앞면·뒷면을 이미지로 기기 갤러리에 내보내거나 공유합니다. 화면 미리보기와 내보낸 이미지는 같은 계산식을 씁니다.

### 🗂 갤러리
- **월별 갤러리** — 판지 종이 위 3열 그리드로 달마다 묶어 봅니다(최신순/오래된순).
- **기억 밀도 보기** — 1월~12월에 엽서가 얼마나 쌓였는지 한눈에 봅니다.
- **흔들어서 한 장** — 기기를 흔들면 엽서 한 장을 무작위로 꺼내 보여줍니다.
- **퀵셀렉트** — 신문 오림 손 다섯 장이 펼쳐지며 촬영·미래 우체통·특별한 갤러리(연못·양떼목장·쫑쫑컵)를 바로 엽니다.

### 📅 방문 기록 · 미래 우체통
- **방문 기록과 방문 달력** — 하루 처음 앱을 연 날을 기록하고, 벽걸이 달력처럼 넘겨 봅니다.
- **미래 우체통** — 지정한 날짜가 되기 전까지 봉인되는 엽서를 보냅니다.

---

## ✦ 디자인

따뜻한 종이 톤 화면(`PaperCanvas` `#F4ECDE` 등)에 잉크색 글자(`InkPrimary` `#1A1324`)를 기본으로,
신문·잡지 오림, 판지, 벽걸이 달력처럼 손으로 만든 종이 물건의 질감을 씁니다.

---

## ✦ 기술 스택

| 영역 | 기술 |
|------|------|
| UI | Jetpack Compose, Material3 |
| 카메라 | CameraX 1.6 |
| 이미지 로딩 | Coil 2.6 |
| 배경제거(누끼) | ML Kit Subject Segmentation |
| 사진 색상 추출 | AndroidX Palette |
| 데이터베이스 | Room 2.8 (schema 19, migration 1→19 연속 유지) |
| 꾸미기·초안·방문 기록 | 앱 내부 저장소의 텍스트 파일, 원자적 교체 저장 |
| 의존성 주입 | Hilt 2.59 |
| 권한 처리 | Accompanist Permissions 0.34 |
| 네비게이션 | Navigation Compose 2.7 |
| 빌드 | Kotlin 2.3, AGP 9.2, KSP, JDK 17, minSdk 26 |

---

## ✦ 프로젝트 구조

```
app/src/main/java/com/postcardmemory/
├── data/        ← Room 엔티티·DAO·DB(migration)·Repository
├── di/          ← Hilt 모듈
├── ui/
│   ├── camera/      ← 촬영
│   ├── gallery/     ← 월별/기억 밀도 갤러리, 퀵셀렉트, 흔들기, 방문 달력, 특별한 갤러리
│   ├── detail/      ← 엽서 앞뒷면 편집과 꾸미기 요소
│   ├── futuremail/  ← 미래 우체통
│   ├── intro/       ← 시작 화면(방문 소인)
│   ├── components/  ← 공용 컴포넌트·모양
│   └── theme/
├── utils/       ← 저장(원자 저장·초안·확정 상태), 삭제, 고아 파일 진단, 이미지 내보내기·렌더러
└── MainActivity.kt
```

---

## ✦ 검증

| 구분 | 내용 |
|------|------|
| JVM 단위 테스트 | `testDebugUnitTest` — 저장·초안·직렬화·삭제·좌표 계산 등. 일부는 소스 구조를 고정하는 구조 테스트 |
| 계측(instrumentation) 테스트 | `app/src/androidTest` — Room migration 1→19 연결, 삭제 순서, 렌더링 등. **검증 전용 emulator에서 수동으로 실행**해 확인 |
| GitHub Actions CI | push마다 `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest` 실행. 마지막 단계는 계측 테스트 APK를 **컴파일만** 하며 emulator에서 실행하지 않음 |
| 실기기 | 사람이 직접 하는 감각 QA. 실사용 기기에서는 자동 계측 테스트를 돌리지 않음 |

테스트 수와 보호 범위의 현재 기록은 [TEST-COVERAGE-MAP](docs/ai/TEST-COVERAGE-MAP.md)에 있습니다.

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
| `CAMERA` | 엽서 사진·스티커 촬영 |
| `VIBRATE` | 꾸미기 인터랙션 햅틱 |

사진과 엽서 데이터는 앱 내부 저장소에만 저장됩니다.

---

<div align="center">

```
📮  모든 순간은 우표가 될 수 있어요  📮
```

</div>
