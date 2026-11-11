# 129일차 작업 A — 출력 스티커 메모리 최적화 중간 보고

2026-11-11, 패코(Codex), 수동 표준 모드. 사용자 정정에 따라 작업 A만 수행했어. 미래 우체통 UI·목업·피코 워크플로우 검증은 담당 범위에서 제외했어. 정정 전 우체통 코드는 읽기만 했고 해당 영역 파일 변경은 없어.

## 사용자 관점 결과

작게 붙인 고해상도 스티커를 공유·내보내기할 때, 필요 이상으로 큰 Bitmap을 읽지 않는 **최소 구현 후보**를 만들었어. 격리 검증 앱에서 스티커 한 장의 실제 Bitmap 할당량 감소를 확인했어. 원본 파일·저장 형식·Room 19는 바꾸지 않았어.

**최적화 최종 승인은 보류야. 로컬 체크포인트 commit만 별도로 승인받았어.** Bitmap 메모리 절감 실측·합성 이미지 21조건 비교는 완료했고, 촘촘한 선무늬 모아레는 확인됐어. 합성 이미지의 위치·회전·반전·크롭·종이 모양은 유지되지만 재표본화 차이가 있어. 실제 사진의 질감·머리카락 같은 누끼 가장자리와 사용자 앱의 공유·내보내기 전체 경로는 미검증이야. 이번 결과를 무손실 품질 또는 앱 전체 메모리 개선 완료로 확대하지 않아.

## 시작 상태와 구현

- 시작 `main`, HEAD 및 원격 main `e5d865cca98b1c1d37886383b44958107ab3fca7`, local/origin 0/0. 원격은 실제 `git ls-remote`로 확인했어.
- tracked 변경 0건에서 `perf/day129-export-sticker-decode`를 만들었어. 기존 `.claude/`, `.codex-config.candidate.toml`, `.kotlin/`, 보호 PNG 3개, 미추적 목업 4개는 작업 대상에 넣지 않았어.
- 실제 경로: `createPostcardBitmap` → `drawStickerOverlay` → `decodeStickerBitmap`. 2048×2048 출력, 스티커별 순차 디코딩·그리기·finally 해제는 유지해. 공유와 갤러리 내보내기가 같은 앞면 렌더러를 사용해.
- `sizeRatio × 2048`을 기존과 동일하게 1..2048px로 제한한 박스를 기준으로 계산해. Crop은 짧은 변, 누끼 Fit은 긴 변에 **출력 박스의 2배 이상** 해상도가 남는 가장 큰 2의 거듭제곱 sampleSize를 선택해. 작은 원본·큰 배치는 sampleSize=1로 유지해.
- API 28 이상은 `ImageDecoder.setTargetSampleSize`, API 26~27은 bounds 확인 후 `BitmapFactory.Options.inSampleSize`를 사용해. 전체 디코딩 뒤 별도 축소 Bitmap을 만드는 방식은 아니야. software allocator·색상 설정·알파 처리는 유지해.
- 누끼 파일 읽기 실패 뒤 원본으로 돌아가는 경우는 Fit이 아니라 **기존 Crop에 필요한 해상도**로 다시 계산해.
- EXIF 90°/270°는 축을 교환하지만 정사각 박스의 min/max 기준은 같아. 이후 EXIF 보정, 사용자 회전·반전, Crop/Fit, 종이 창·클립·테두리 렌더러는 바꾸지 않았어. 종이 창은 박스 안에 있으므로 박스 전체를 기준으로 하는 계산은 보수적이야.

공식 API 근거: [ImageDecoder](https://developer.android.com/reference/android/graphics/ImageDecoder#setTargetSampleSize(int)), [큰 Bitmap의 bounds·샘플링](https://developer.android.com/topic/performance/graphics/load-bitmap). 2배 여유는 이번 후보의 품질 여유값이고 공식 무손실 보장값은 아니야.

## Android 전후 비교 방법

연결된 단일 SM-S936N, API 37에서 실행했어. 처음에는 연결 기기가 없었고, 사용자가 연결한 뒤에 확인했어. serial은 기록하지 않아.

일반 instrumentation은 실제 `PostCardMemoryApp.onCreate()`의 `PostcardTempCleanup.cleanup(filesDir)`까지 고려해야 해서 실행하지 않았어. 기존 SAFE 정책·분류를 이 비교 실행의 승인 근거로 쓰지 않았어.

사용자가 별도로 승인한 **무권한·독립 package `com.postcardmemory.exportprobe.day129`**만 설치했어. 기존 앱을 실행·업데이트하지 않았어. 검증 앱은 앱 Activity·Application을 불러오지 않고, APK 사본을 별도 DexClassLoader로 읽어서 실제 `decodeStickerBitmap`·`drawStickerOverlay`만 호출해. 입력은 검증 앱이 생성한 PNG/JPEG뿐이고, 결과는 그 앱의 전용 `files/comparison-2`에만 저장해. DB·MediaStore·사용자 filesDir·공유 캐시에는 접근하지 않아. Gradle/dependency/runner 변경 없이 SDK 도구로 검증 APK만 별도 제작했어.

| 비교 대상 | SHA-256 |
|---|---|
| 기존 설치 APK 사본 | `de6a4068567f5638aef0739803c195eacf1843f7b235d9a1b44cf844b96edc5f` |
| 변경 후보 Debug APK | `94a36f1604dfffaf8e27b84b8e0acbacd31f057f49220b405cd2363f78cce97e` |
| 최종 검증 앱 v2 APK | `7e2f7b1302be50a239e30b77b54368ac6918caf09a5313a3c8b1a0781fcbf418` |

기존 설치 APK는 128일차 기록의 hash와 같고, `c731034..HEAD`의 exporter 소스 차이는 0건이었어. APK 사본은 동적 로딩 전에 read-only로 바꿨어. 검증 앱은 동일 결과 폴더가 있으면 재실행·덮어쓰기를 거부해.

## 메모리 실측 — 스티커 한 장

같은 검증 프로세스에서 변경 전·후 디코더가 돌려준 Bitmap의 `getAllocationByteCount()`를 각각 읽고 해제했어. **각 조건 1쌍의 결정적인 할당량 비교**이며 앱 전체 peak/PSS·GC·CPU·내보내기 처리시간 측정은 아니야. 디코더 내부 임시 버퍼와 다른 스티커·본사진·출력 Bitmap을 포함한 동시 최대값도 아니야.

| 조건 | 변경 전 → 후 해상도 | 할당량 전 → 후 (MiB) | 해당 Bitmap 감소 |
|---|---|---:|---:|
| 정사각 2992, sizeRatio=.25 | 2992² → 1496² | 34.15 → 8.54 | 75% |
| 작은 배치, sizeRatio=.10 | 2992² → 748² | 34.15 → 2.13 | 93.75% |
| 홀수 크기 투명 누끼 | 2993×1999 → 1496×999 | 22.82 → 5.70 | 약 75.02% |
| JPEG 회전·반전 / 종이 오림 / EXIF | 2992×1998 → 1496×999（EXIF에 따라 축 교환） | 22.80 → 5.70 | 75% |
| 큰 배치, sizeRatio=.75 | 2992² 유지 | 34.15 → 34.15 | 0% |
| 가로로 긴 Crop / 원본 fallback | 4000×1000 유지 | 15.26 → 15.26 | 0% |
| 작은 원본 | 120×80 유지 | 0.037 → 0.037 | 0% |

위 수치는 **실제 Android Bitmap 측정값**이야. 이론상 픽셀 산술을 실측으로 바꿔 부른 값이 아니야. [원시 측정 CSV](results.csv)에 바이트 수·해상도·이미지 차이 지표를 남겼어.

## 출력 품질 결과와 한계

21조건이 예외 없이 완료됐어: 정사각 사진형 PNG, 가로로 긴 Crop, 반투명 누끼, 작은 원본, 큰·작은 배치, 회전·양축 반전, 누끼 회전, 종이 오림 4종, 누끼 실패 fallback, JPEG EXIF 1~8.

- 축소하지 않은 4조건(가로로 긴 Crop·작은 원본·큰 배치·fallback)은 **전체 2048px 출력이 sameAs 일치**했어.
- 21조건의 alpha>8 영역 bounds는 20조건이 같고, 홀수 크기 투명 누끼 1조건은 최대 1px 차이였어. 이 검사는 전체 피사체·내용 일치를 증명하지 않아.
- 축소한 17조건은 흰 배경에 합성한 비어 있지 않은 영역의 RGB 채널 평균 절대차가 약 2.11~6.52/255야. 최대 단일 채널 차이는 최대 226/255라서 평균만으로 품질 합격을 선언하지 않아.
- 전후 비교 PNG 11쌍을 직접 열어 위치·크롭·회전·반전·종이 테두리·투명 외곽을 확인했어. 눈에 띄는 배치 이동이나 투명 테두리의 큰 손상은 보지 못했지만, 촘촘한 선무늬에서는 모아레·선 표현 차이가 보여. 이 차이에 대한 최종 품질 승인은 남아 있어.
- 비교판은 동일한 800×800 영역을 640×640으로 표시한 **검토용 축소판**이야. 원래 2048px PNG는 검증 앱 안에 남아 있어. 원본 해상도·확대 검토나 실제 사진 전수 QA를 대신하지 않아.
- 입력은 일반 JPEG 형식을 포함한 **합성 이미지**야. 실제 일반 사진·머리카락/털의 누끼 품질, content:// provider, API 26~27 BitmapFactory 실제 실행은 미검증이야. 로컬 emulator는 사용하지 않았어.

비교판(왼쪽 변경 전 / 오른쪽 변경 후): [정사각](comparisons/square-pair.png), [투명 누끼](comparisons/cutout-alpha-pair.png), [작은 배치](comparisons/tiny-pair.png), [회전·반전](comparisons/rotate-flip-pair.png), [누끼 회전](comparisons/alpha-rotate-pair.png), [폴라로이드](comparisons/POLAROID-pair.png), [가위](comparisons/SCISSOR-pair.png), [찢은 종이](comparisons/TORN-pair.png), [잡지](comparisons/MAGAZINE-pair.png), [EXIF 1](comparisons/exif-1-pair.png), [EXIF 6](comparisons/exif-6-pair.png).

## 자동 검증과 실패 구분

| 검증 영역 | 상태 | 근거·범위 |
|---|---|---|
| 로컬 자동검증 | 실행·통과 | Gradle 9.8.2 / JDK 17, JVM **1204/1204**, XML 128, 실패·오류·skip 0. `assembleDebug`, `assembleDebugAndroidTest` 성공 |
| 실사용 기기 instrumentation | 미실행 | 기존 21건/8파일 및 SAFE 정책 무변경. 이번 native 비교는 별도 검증 앱이며 instrumentation 성공으로 세지 않아 |
| 별도 Android 렌더러 비교 | 실행 | API 37, 합성 21조건, Bitmap 실측·PNG 비교. 앱 전체 경로 검증과 구분 |
| GitHub Actions CI | 미실행 | commit·push·PR 없음 |
| 실기기 감각 QA | 미실행 | 사용자 원래 앱의 실행 결과·실제 사진·공유/내보내기 최종 확인 대기 |

새 순수 JVM 테스트 12개는 sampleSize와 해상도 여유·Crop/Fit·fallback 기준·홀수/분수 경계·EXIF 축 교환·작은 원본·가느다란 이미지·비정상 값·overflow 및 504조합을 확인해. 디코더 실제 픽셀·화면 호출 연결을 보장하는 테스트는 아니야. 소스 JVM 1204개/127파일 + helper 1개, instrumentation 21개/8파일. TEST-COVERAGE-MAP을 같은 작업일 갱신했어.

환경 실패는 production 실패와 구분해 보존해:

1. 브랜치 생성은 `.git` 권한 제한으로 첫 실패 후 승인된 환경에서 성공. 원격 조회도 최초 네트워크 제한 뒤 성공했어.
2. 저장소에 `gradlew.bat`가 없어 그 호출은 실패했고, 실제 설치된 Gradle 9.8.2로 전환했어. offline 빌드는 `org.gradle.toolchains.foojay-resolver-convention:0.10.0` resolve에서 멈춰 테스트 본문에 진입하지 않았어. 기존 캐시·의존성 접근을 허용한 빌드가 성공했어. 변경 전 task는 전부 UP-TO-DATE였고, 변경 후 테스트는 실제 실행됐어.
3. 최초 app_process 프로브는 `PROBE_START` 뒤 `Killed` / exit 137. Application 초기화를 제거한 프로브는 `RENDERERS_READY` 뒤 `Assertion failed: src == nullptr && gDefaultTypeface == nullptr` / exit 134였어. 이 경로는 폐기했고 설치나 설정 우회로 복구하지 않았어. 사용자 앱은 실행하지 않았어.
4. 검증 전용 Java 컴파일의 기본 Windows 인코딩 오류는 `-encoding UTF-8`로 해결했어. SDK 읽기 권한 실패도 컴파일 환경 실패였어.
5. 사용자 승인 후 설치한 독립 검증 앱 v1은 렌더링 전에 `IllegalArgumentException: Use the isolated QA package directory`로 중단했어. 하드코딩 절대경로 대신 앱이 제공한 filesDir과 새 자식 폴더의 canonical parent를 대조하도록 수정했어. 별도 v2 업데이트·재실행 승인을 받은 뒤 21조건이 완료됐어. v1 실패 폴더는 보존했어. 실제 canonical 경로 문자열은 당시 로그에 없어 특정 Android 경로 별칭이 원인이라고 단정하지 않아.

## 데이터·파일·Git 상태와 재개 조건

- 기존 앱의 APK hash와 설치·업데이트 시각이 전후 같고, 기존 앱 프로세스는 이번 검증으로 실행하지 않았어. 앱 사용자 저장소에 쓰는 경로를 실행하지 않았어. 사용자 파일 전수 해시를 비교한 것은 아니므로 전수 해시 일치라고 보고하지 않아.
- 사용자 갤러리에 테스트 파일을 만들지 않았어. Room/schema/원본 스티커/발송·도착·개봉 로직/워크플로우 패키지/의존성/Gradle 설정은 무변경이야.
- 기기의 검증 앱(v2), v1 실패 자료·v2 결과(전용 files 합계 약 226.5MiB), `/data/local/tmp/postcard-day129-export-20261111-01`(약 102.9MiB)을 남겨뒀어. 검증 APK 설치 용량은 별도이며 원본 앱과 다른 package야. 임의 삭제·uninstall·초기화는 하지 않았어. PC 임시 폴더에도 APK·검증 산출물을 보존했어.
- production 변경은 exporter 1개와 계산 helper 1개야. JVM 테스트 1개, 이 QA 자료·검증 앱 소스 3개·비교 PNG 11개·CSV, TEST-COVERAGE-MAP과 HANDOFF가 관련 산출물이야. 검증 앱 소스는 원래 앱 빌드에 참여하지 않아.
- 사용자 마감 지시로 현재 작업 브랜치에 후보 보존용 **로컬 commit만** 진행해. 이 문서는 commit 직전 기록이며 최종 SHA·성공 여부는 완료보고에서 확인해. push·PR은 보류, main 병합·사용자 원래 앱 업데이트·실기기 검증 앱/임시 자료 삭제는 금지야. 기존 main과 설치 앱은 유지하며 후보를 배포 가능한 완료로 판단하지 않아.
- 재개는 **실제 사진·누끼의 품질과 사용자 앱 QA 범위 결정**부터야. 기존 앱 업데이트 설치, 실제 공유 cache/갤러리 쓰기는 이번 검증 앱 승인에 포함되지 않아. 해당 대상·효과를 구체화하고 별도 승인을 받아야 해. 품질 저하가 확인되면 추가 production 수정 전에 원인·대안을 보고해. 현재 핵심 구현 후보는 1회이며 해결책을 되돌리는 추가 수정은 하지 않았어.

## 검증 앱 재현 원칙

검증 소스는 [StickerExportProbe.java](StickerExportProbe.java), [ProbeActivity.java](ProbeActivity.java), [AndroidManifest.xml](AndroidManifest.xml)이야. 기존 Gradle 프로젝트에는 연결하지 않았어. SDK의 `javac -encoding UTF-8 -source 8 -target 8` → `d8 --min-api 26` → `aapt2 link`(before.apk·after.apk를 assets에 포함) → dex 추가 → `zipalign` → 별도 임시 키로 `apksigner` 서명 순서로 제작했어. 캐시나 기존 빌드 폴더를 수동 편집하지 않았어.

최종 소스는 기존 `comparison-2` 폴더를 덮어쓰지 않아서 재실행 시 멈춰. 재실행을 위해 앱 데이터를 지우거나 기존 실패 자료를 삭제하지 않아. 다른 기기·새 폴더·새 APK로 검증하려면 당시 대상과 설치·실행 승인을 다시 확인해.
