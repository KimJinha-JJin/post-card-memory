> **문서 상태:** 앱 소개·구조·검증 설명은 2026-10-18(107일차) 코드·운영 규칙 기준으로 갱신했어. 작업 규칙·현재 상태·승인 범위의 기준은 이 README가 아니라 [AGENTS.md](AGENTS.md), [현재 HANDOFF](docs/ai/HANDOFF.md), [유효한 결정](docs/ai/DECISIONS.md)이야.

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
- 사진 레이아웃 우표 / 폴라로이드 / 테이프 필름 / 편지지, 사진 확대·이동·가장자리 블러.
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
- **퀵셀렉트** — 신문 오림 손 다섯 장이 부채처럼 펼쳐지며 카메라·미래 우체통과 놀이 갤러리 세 가지를 바로 엽니다.

### 🎠 놀이 갤러리 (퀵셀렉트에서 진입)
- **엽서의 연못** — 갤러리 엽서들이 물 위에 뜬 것처럼 흔들리고, 기기를 흔들면 반응합니다. 엽서를 새총처럼 당겼다 놓으면 날아가 주변 엽서를 밀어내고 파문이 남습니다.
- **동글이 쉼터** — 최신 엽서 최대 10장을 동글이 캐릭터가 들고 종이 바닥 위를 돌아다닙니다. 탭하면 엽서를 열고, 길게 눌러 끌 수 있어요.
- **엽서 쫑쫑컵** — 같은 무대가 잔디 경기장으로 바뀌고, 엽서 3장이 출전해 레이스를 벌입니다(엽서가 3장 미만이면 안내만 나옴). 나머지는 관중석에서 구경합니다.
- 세 모드 모두 화면 안에서만 노는 기능이라 위치·레이스 결과는 저장하지 않고, 엽서 데이터도 바꾸지 않습니다.

### 📅 방문 기록 · 미래 우체통
- **방문 기록과 방문 달력** — 하루 처음 앱을 연 날을 기록하고, 벽걸이 달력처럼 넘겨 봅니다.
  시작 화면에는 방문 소인이 찍히고, 그날 처음 연 실행에서는 도장 손이 들어와 직접 찍어 줍니다.
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
│   ├── gallery/     ← 월별/기억 밀도 갤러리, 퀵셀렉트, 흔들기, 방문 달력, 놀이 갤러리(아래 표)
│   ├── detail/      ← 엽서 앞뒷면 편집과 꾸미기 요소
│   ├── futuremail/  ← 미래 우체통
│   ├── intro/       ← 시작 화면(방문 소인)
│   ├── components/  ← 공용 컴포넌트·모양
│   └── theme/
├── utils/       ← 저장(원자 저장·초안·확정 상태), 삭제, 고아 파일 진단, 이미지 내보내기·렌더러
└── MainActivity.kt
```

**놀이 갤러리 코드 구조** (`ui/gallery/`)

| 이름 | 역할 |
|------|------|
| `GalleryPlayMode` | 갤러리 놀이 상태(`NONE` / `POND` / `SHEEP_RANCH` / `RACE`) |
| `PondController`, `PondRippleOverlay` | 엽서의 연못 — 발사 충격 전달과 파문. 카드의 부유·새총 물리는 `ui/components/StampCard.kt` |
| `GalleryPlayStage*` | 동글이 쉼터와 쫑쫑컵이 **함께 쓰는 무대**(카드 이동·점프·늘이기·구름). `raceEnabled`로 두 모드를 가른다 |
| `PostcardRace*`, `PixelPostcardRaceCar` | 쫑쫑컵 전용 — 레이스 단계·경기장·레이스 연출 |
| `Shelter*` (`GalleryPlayStage.kt` 안의 private 요소) | 동글이 쉼터 전용 종이 바닥·색 |

- `GalleryPlayMode.SHEEP_RANCH`는 동글이 쉼터의 예전("양떼목장") 이름입니다. 갤러리가 이 이름 문자열을 화면 복원 상태에 저장하고 `valueOf`로 되읽기 때문에, 화면 이름과 달라도 일부러 바꾸지 않는 **호환성 식별자**입니다(`GalleryPlayModeTest`가 고정).

---

## ✦ 검증

| 구분 | 내용 |
|------|------|
| JVM 단위 테스트 | `testDebugUnitTest` — 911건(107일차 기준). 저장·초안·직렬화·삭제·좌표 계산·놀이 무대 구역 계산 등. 일부는 소스 구조를 고정하는 구조 테스트 |
| 계측(instrumentation) 테스트 | `app/src/androidTest` 20건 — Room migration 1→19 연결, 삭제 순서, 렌더링, 사진 스티커 직렬화 등. 실제 앱 저장공간에서 돌기 때문에 SAFE 7 / CONDITIONAL 6 / FORBIDDEN 7 등급으로 관리하며, 실사용 기기에서는 **SAFE 7건만 사용자 승인 후 클래스 단위로** 실행할 수 있음 |
| GitHub Actions CI | push마다 `testDebugUnitTest`, `assembleDebug`, `assembleDebugAndroidTest` 실행. 마지막 단계는 계측 테스트 APK를 **컴파일만** 하며 실행하지 않음 |
| 실기기 | 로컬 기본 검증 환경. 실제 엽서가 있는 기기라 읽기·관찰 위주 확인과 사람이 직접 하는 감각 QA가 중심 |

- 로컬 Android Emulator는 이 프로젝트의 검증 수단에서 **폐기**됐어요(2026-10-03). 80~91일차의 emulator 계측 실행 결과는 당시 기록으로만 남아 있습니다.
- 실사용 기기에서는 Gradle `connected*` task, uninstall, `pm clear`, 앱 데이터 초기화를 쓰지 않습니다.
- 격리된 계측 실행 환경(원격 CI emulator)은 아직 없고 후속 후보예요.
- 등급별 테스트 목록과 테스트 수·보호 범위는 [TEST-COVERAGE-MAP](docs/ai/TEST-COVERAGE-MAP.md)에, 상세 안전 규칙은 [AGENTS.md](AGENTS.md) 5절에 있습니다.

---

## ✦ 빌드 & 실행

**요구 사항**
- Android Studio (최신 버전 권장)
- JDK 17
- Android 8.0 (API 26) 이상 기기 (앱은 에뮬레이터에서도 동작하지만, 이 프로젝트의 검증은 로컬 에뮬레이터를 쓰지 않습니다)

```bash
git clone https://github.com/KimJinha-JJin/post-card-memory.git
cd post-card-memory
```

Android Studio에서 프로젝트를 열고 Gradle Sync 후 실행하세요.
(이 저장소에는 Gradle Wrapper가 포함되어 있지 않아 `./gradlew` 커맨드라인 빌드는 지원되지 않습니다.
커맨드라인에서는 따로 설치한 Gradle 9.4.1과 JDK 17로 `gradle testDebugUnitTest assembleDebug`를 실행할 수 있고, CI도 같은 버전을 씁니다.)

**주의:** 실제 엽서가 들어 있는 기기에서는 `connectedAndroidTest` 같은 `connected*` task를 실행하지 마세요. 실행 후 앱이 제거되면서 엽서 데이터가 지워질 수 있습니다. 업데이트 설치는 데이터를 보존하는 `adb install -r`을 씁니다.

---

## ✦ 필요 권한

| 권한 | 용도 |
|------|------|
| `CAMERA` | 엽서 사진·스티커 촬영 |
| `VIBRATE` | 꾸미기 인터랙션 햅틱 |

사진과 엽서 데이터는 앱 내부 저장소에만 저장됩니다.

---

## ✦ 더 읽을 문서

| 문서 | 내용 |
|------|------|
| [AGENTS.md](AGENTS.md) | 작업 규칙과 사용자 데이터·실기기 안전선 |
| [docs/ai/HANDOFF.md](docs/ai/HANDOFF.md) | 현재 상태·미검증·남은 위험과 다음 출발점 |
| [docs/ai/DECISIONS.md](docs/ai/DECISIONS.md) | 계속 유효한 제품·구조 결정 |
| [docs/ai/TEST-COVERAGE-MAP.md](docs/ai/TEST-COVERAGE-MAP.md) | 테스트 수·보호 범위·계측 테스트 등급 |
| [docs/ai/IMAGE-ASSET-STANDARD.md](docs/ai/IMAGE-ASSET-STANDARD.md), [IMAGE-ASSET-MAP.md](docs/ai/IMAGE-ASSET-MAP.md) | 이미지 자산 규격과 화면별 사용처·메모리 측정 |

---

<div align="center">

```
📮  모든 순간은 우표가 될 수 있어요  📮
```

</div>
