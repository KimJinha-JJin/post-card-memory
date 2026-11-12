# HANDOFF — 129일차 작업 A: 스티커 메모리 최적화 후보 · Android 실측 완료 / 최종 품질·사용자 앱 QA 대기

2026-11-11, 담당 패코(Codex), 수동 표준 모드. 사용자 정정으로 **공유·내보내기 스티커 디코딩 작업 A만** 수행했어. 우체통 UI·목업·피코 ADB 워크플로우 검증은 담당하지 않아. 정정 전 해당 코드는 읽기만 했고 관련 파일 수정은 없어.

## 현재 사실과 Git

- 시작 `main = origin/main = 실제 원격 main = e5d865cca98b1c1d37886383b44958107ab3fca7`, local/origin 0/0, tracked 변경 0건. 현재 작업 브랜치 `perf/day129-export-sticker-decode`, HEAD는 같은 SHA야.
- 출력용 스티커만 수정한 **검증 대기 구현 후보**야. exporter 1개 + 순수 계산 helper 1개, JVM 테스트 1개, TEST-COVERAGE-MAP·이 HANDOFF·관련 QA 자료를 변경했어. 기존 보호 PNG 3개·미추적 목업 4개·로컬 설정·캐시는 작업 대상에 넣지 않았어.
- 사용자가 129일차 A 마감 지시에서 **품질 QA 전 현재 브랜치의 로컬 체크포인트 commit만 명시 승인**했어. 이 절은 해당 commit 직전 기록이며 최종 SHA·성공 여부는 완료보고에서 확인해. 최적화 최종 승인은 계속 보류야. push·PR은 보류, main 병합·원래 앱 업데이트·실기기 검증 앱/임시 자료 삭제는 금지야. 추가 production 변경 없이 기존 성과만 보존해.

## 구현과 측정 결과

- `drawStickerOverlay`에서 기존 `sizeRatio × 2048` 박스 크기를 decode 전에 계산해. Crop은 짧은 변, 누끼 Fit은 긴 변에 출력 박스의 2배 이상이 남는 2의 거듭제곱 sampling만 사용해. 큰 배치·작은 원본은 그대로 읽어. 누끼 실패 원본 fallback은 Crop 기준으로 계산해. API 28+ ImageDecoder와 API 26~27 BitmapFactory에 같은 순수 계산을 연결했어.
- EXIF 보정·회전·반전·Crop/Fit·종이 창·클립·테두리·해제 순서는 유지했어. 원본 파일/Room 19/저장 형식/발송·도착·개봉/워크플로우·Gradle·dependency 변경은 없어.
- 사용자 연결 SM-S936N(API 37)에서 **별도 무권한 검증 앱만** 설치했어. 기존 앱은 실행·업데이트하지 않았어. APK 전후 사본에서 실제 스티커 decoder·renderer를 로드해 합성 PNG/JPEG 21조건을 비교했어. 2992² 스티커의 `allocationByteCount`는 35,808,256→8,952,064바이트(34.15→8.54MiB), 작은 배치는 2,238,016바이트(2.13MiB). **스티커 한 장의 Bitmap 실측이며 앱 전체 peak/PSS·속도 개선 실측은 아니야.**
- 비축소 4조건은 전체 출력 sameAs 일치. alpha>8 영역 bounds는 20조건 동일·홀수 누끼 1조건 최대 1px 차이. 비교판 11쌍을 열어 배치·크롭·회전·반전·종이 모양을 확인했어. **촘촘한 선무늬의 모아레·선 표현 차이를 확인했으므로 품질 무손실·최종 적용 완료로 판단하지 않았어.** 실제 사진·누끼 질감·content://·API 26~27·원래 앱 공유/내보내기 전체 경로는 미검증이야.
- 자세한 측정값·비교 PNG·입력/출력 한계·검증 앱 소스·실패 이력은 [129일차 A 보고](../qa/day129/README.md), [CSV](../qa/day129/results.csv)에 있어.

## 검증 상태

- 로컬 자동검증: **실행·통과**, JVM 1204/1204(XML 128, 실패·오류·skip 0), Debug APK·androidTest APK assemble 성공. 새 순수 테스트 12건/1파일. TEST-COVERAGE-MAP 갱신 완료(JVM 1204개/127파일 + helper 1, 구조 247 유지, instrumentation 21개/8파일 유지).
- 실사용 기기 instrumentation: **미실행**. Application 시작 시 임시파일 정리까지 고려해야 해. 기존 SAFE 정책을 우회하지 않았고 별도 검증 앱의 21조건을 instrumentation 성공으로 세지 않아. CONDITIONAL/FORBIDDEN은 격리 instrumentation 환경 부재로 계속 실행 불가야.
- Android 별도 렌더러 비교: **실행**, 사용자 승인받은 독립 검증 앱 v2에서 21조건 완료. 사용자 원래 앱 감각 QA: **미실행/사용자 확인 대기**. GitHub Actions CI: **미실행**.
- 최초 권한·네트워크·Gradle offline resolve·Java 인코딩 실패는 환경 실패였고 production 테스트 실패는 없었어. app_process의 exit 137·글꼴 assertion exit 134 경로는 폐기했어. 독립 검증 앱 v1은 경로 안전 검사에서 멈췄고 실패 자료를 보존했어. 별도 승인받은 v2 업데이트 후 완료됐어. 상세 실패 근거는 보고서에 남겼어.

## 데이터 보호와 남은 기기 자료

- 기존 설치 APK SHA-256 `de6a4068567f5638aef0739803c195eacf1843f7b235d9a1b44cf844b96edc5f`와 설치/업데이트 시각이 전후 같아. 사용자 앱 프로세스를 실행하지 않았고 사용자 저장소·DB·갤러리에 쓰는 경로도 실행하지 않았어. 사용자 파일 전수 hash 비교를 한 것은 아니야.
- 변경 후보 APK SHA-256 `94a36f1604dfffaf8e27b84b8e0acbacd31f057f49220b405cd2363f78cce97e`. 검증 앱 v2 SHA-256 `7e2f7b1302be50a239e30b77b54368ac6918caf09a5313a3c8b1a0781fcbf418`.
- 별도 package `com.postcardmemory.exportprobe.day129`와 그 전용 files(실패 자료·결과 합계 약 226.5MiB), 직접 만든 `/data/local/tmp/postcard-day129-export-20261111-01`(약 102.9MiB), PC 임시 검증 자료를 보존했어. 검증 앱 APK 설치 공간은 별도야. 임의 삭제·uninstall·초기화는 하지 않았어. 검증 앱은 기존 결과 폴더가 있으면 덮어쓰지 않고 멈춰.

## 재개 조건과 다음 판단 하나

**실제 사진·누끼의 출력 품질과 사용자 앱 QA 범위를 결정하는 것**부터야. Bitmap 메모리 절감 실측·합성 21조건 비교는 완료, 촘촘한 선무늬 모아레는 확인됨, 실제 사진·누끼 품질과 사용자 앱 공유·내보내기는 미검증, 최적화 최종 승인은 보류야. 이번 로컬 commit은 후보 보존의 예외 승인일 뿐 품질 승인·push 승인이 아니야. 원래 앱 업데이트·공유 cache 생성/정리·갤러리 쓰기는 현재 금지이며 재개하려면 대상·효과를 구체화해 별도 승인을 받아야 해. 현재 핵심 production 구현 시도는 1회이며, QA 도구 환경 보정과 구분해. 추가 production 변경·기존 해결책 되돌리기는 원인·계획을 먼저 보고해.

## 앞선 기록

[128일차 QA·메모리 조사·Git 마감 당시 기록 원문](archive/HANDOFF-day128-before-day129-export-2026-11-11.md)을 보존했어. 아래 127일차 이하 기록도 당시 이력이며 현재 승인·현재 Git 상태는 위 절과 실제 저장소를 기준으로 해.

