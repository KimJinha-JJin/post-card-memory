# HANDOFF — 74일차 방문 달력 폴리싱

확인일: 2026-09-15. 수동 표준 모드. 사용자 제공 74일차 지시서에 따라 Codex가 직접 구현했어. 공용 작업판은 활성화하지 않았어.

## 현재 상태

방문 라벨 모서리는 **6dp → 2dp**로 바꿨고 실기기 QA와 이전 commit·push까지 끝났어. 사용자가 승인한 전체 폭 장식선을 실제 방문 달력에도 최소 반영했어. 청록색·라벨 크기·3dp 여백·날짜·카오모지 위치·대비와 방문 저장 구조는 그대로야. 공휴일은 지원 연도 결정 지점에서 STOP했어.

| 구분 | 상태 |
|---|---|
| 구현 | 2dp 완료. 장식선 목업 승인 및 production 최소 반영 완료. 공휴일 미구현·정책 판단 대기 |
| 자동 검증 | 관련 테스트 16건·Kotlin compile·diff check 통과 |
| 사용자 QA | 2dp 실기기 완료. 장식선 목업 승인 완료, production 실기기 QA 대기 |
| commit | 2dp 작업 완료 — `49654c2`, HANDOFF `a113dd7`. 현재 장식선 변경은 미커밋 |
| push | 2dp 작업 완료 — 원격 `a113dd7`. 현재 장식선 변경은 미푸시 |
| 전체 완료 | 2dp 폴리싱 완료. 장식선은 production 반영 후 실기기 QA 대기. 공휴일 구현은 사용자 정책 판단 대기 |

73일차 원문은 [방문 달력 최종 인수인계](archive/HANDOFF-2026-09-14-visit-calendar-final.md)에 byte 동일하게 보존했어(SHA256 일치 확인). 과거 사고·실패·폐기 접근도 원문 그대로야. archive 안 상대 링크는 원래 HANDOFF 위치 기준이야. 원문에 QA 완료 표와 과거 QA 대기 문장이 혼재해 있어. 73일차 QA 완료는 현재 사용자 지시서와 원문 최종 표를 기준으로 봐. 이번 QA와 혼동하지 않아.

## Git 시작·종료 상태

- 브랜치: `feature/photo-sticker`.
- 시작 HEAD: `141d6cba7a25db88ed80678efbc8b1c896a87814`. 2dp·목업 commit: `49654c2` (`Polish visit calendar label corners`).
- `49654c2` push 후 local과 origin의 `feature/photo-sticker`가 동일한 것을 확인했어. 이 HANDOFF 결과 갱신은 별도 기록 commit으로 이어서 반영해.
- 시작 tracked 변경 없음. 기존 untracked `.claude/`, `.codex-config.candidate.toml`, `.kotlin/` 모두 보존. 지시서보다 `.claude/`가 추가로 있었어.
- `49654c2` 포함 파일: `VisitCalendarDrawer.kt`, `docs/ai/archive/HANDOFF-2026-09-14-visit-calendar-final.md`, `docs/ai/mockups/visit-calendar-2dp.html`, `docs/ai/mockups/visit-calendar-2dp.png`. 현재 HANDOFF 갱신은 별도 기록 commit 대상이야.
- 최근 관련 커밋: `141d6cb` 인수인계, `02574f1` 방문 달력 본체.

## UI/UX 문법·수정 범위

- 역할: 행동 없는 방문 기록 표시. 선택·속성 조절·객체 행동·위험 행동·완료/저장 UI는 해당 없어.
- 근거: `VisitCalendarDrawer.kt`의 현재 월 종이 달력, 73일차 QA와 현재 지시서에서 승인·유지 확인.
- 진입: Gallery 좌측 drawer 그대로. 토큰 `PaperSurface`, `InkSecondary`, `SealInkNavy`, `SealInkRed` 재사용 유지.
- 기존 variant 범위: 역할·정보 계층·상호작용·container·위치는 같고 명시 승인된 반경만 변경. 신규 문법·외부 디자인 레퍼런스·전역 shape 변경은 없어.
- 실제 production diff는 `.background(VisitFillColor, RoundedCornerShape(6.dp))`의 `6`을 `2`로 바꾼 한 줄이야.
- `#16A7A1`, 자동 대비, 카오모지 4종과 날짜 기반 selector, 현재 월만 표시, 크기·spacing은 그대로야.
- 경미한 조정 1회. 핵심 해결 실패·해결책 역전·범위 확대 없음.
- 이번 세션에 최신 실기기 스크린샷은 제공되지 않았어. 새 미감을 실제로 봤다고 주장하지 않아.

## 공휴일 조사와 STOP

`app/src`와 `gradle`의 holiday/공휴일/대체공휴일/KoreanCalendar/lunar 검색에서 기존 STOP 주석만 발견했어. 날짜 helper·방문 기록은 날짜 포맷·요일·epochDay를 다루며 국내 공휴일 데이터가 없어. build 파일과 version catalog에도 공휴일 제공 의존성이 없어.

| 방식 | 정확도·지원 범위 | 대체·임시공휴일 | 새 dependency·네트워크 | 유지보수·복잡도 |
|---|---|---|---|---|
| 공식 자료 기반 정적 연도별 목록 | 대조한 연도와 발표 시점까지. 2026 월력요항 확인 | 공식 대체공휴일 포함 가능. 이후 임시 지정은 데이터 업데이트 필요 | 둘 다 불필요 | 구현 단순, 매년 및 변경 발표 때 목록 대조 필요 |
| 기존 dependency / 플랫폼 API | 한국 공휴일 판정 지원 확인 못함. 날짜/역법 계산과 휴일 정책은 별개 | 자체 계산으로 임시 지정까지 보장 불가 | 현재 구성만으로 정확한 정책 데이터 확보 불가 | 자체 음력·대체 규칙 계산은 복잡하고 유지 비용 큼 |
| 한국천문연구원 특일 정보 API | 공식 날짜·공공기관 휴일 여부·명칭 제공. 전체 지원 연도는 페이지에 미명시 | 실제 응답의 수록·최신성 검증 필요 | 인증키·앱 네트워크 필요, 새 라이브러리 필요 여부와 별개로 API 연결은 STOP 대상 | 장애·캐시·키·갱신 관리 추가 |

출처(2026-09-15 조회):

- [우주항공청 2026년 월력요항](https://www.kasa.go.kr/prog/bbsArticle/BBSMSTR_000000000010/view.do?bbsId=BBSMSTR_000000000010&nttId=B000000001860Pe2zT3): 매년 달력 제작 기준 발표, 대체공휴일 포함. 2025-06-30 발표라 이후 정책 변경까지 검증됐다는 뜻은 아니야.
- [한국천문연구원 특일 정보](https://www.data.go.kr/data/15012690/openapi.do): REST/XML, 인증키 필요. 공공기관 휴일 여부·명칭 제공, 무료·이용허락범위 제한 없음 표시. 실제 API 호출이나 키 발급은 하지 않았어.
- [Android ICU Calendar](https://developer.android.com/reference/android/icu/util/Calendar): 날짜 필드와 역법 계산 문서. 한국 공휴일 정책을 정확히 반환하는 내장 기능은 확인하지 못했어.

**권장:** 공식 자료를 대조한 정적 연도별 목록. 단 몇 년까지 지원할지와 연도별 갱신 부담은 제품 선택이므로, 사용자 지시서 13절의 지원 연도 STOP 경계에 따라 공휴일 구현만 멈췄어. 2026 우선 지원 또는 다음 해 포함 등은 미승인 후보야. 지원 연도 미확정, 현재 앱의 공휴일·대체공휴일·임시공휴일 지원 없음. 임시공휴일 추정, 새 dependency·API·권한·Room 변경 없음.

재개 조건: 지원 연도와 유지 방침 결정 → 해당 연도 최신 공식 목록 및 이후 변경 발표 대조 → 최소 날짜 판정과 색상 분리 구현. 정책을 자체 계산하거나 미래 정확성을 약속하지 않아.

현재 색상은 방문 채움+자동 대비가 요일색보다 우선하고 토요일 navy·일요일 red·평일 기본색이야. 추후 구현은 `방문 채움+자동 대비 > 공휴일/대체공휴일 red > 토요일 navy > 평일 기본색`과 일요일 red를 지켜. 토요일 공휴일·방문 공휴일·12월→1월 테스트가 필요해. 이번 공휴일 테스트는 로직 미구현으로 미실행이야.

## 귀여운 요소 검토

- 방문이 드문 달은 기존 문구와 카오모지로 충분한 여백을 유지해.
- 5~7일 연속은 라벨·얼굴이 이미 반복되므로 추가 반복 장식 필요성이 낮아.
- 방문이 많은 달은 청록 면적이 커져 여백 보존이 중요해.
- 공휴일 red가 늘어나는 상황은 navy/red/teal 정보가 늘어 작은 낙서도 시선을 분산할 수 있어.
- 현재 판단은 **추가 없음 권장**. 작은 소인·월별 낙서도 해결할 문제가 뚜렷하지 않아 후보로 채택하지 않았어.
- 최초에는 목업을 만들지 않았고, 이후 사용자 추가 지시로 아래 비교 목업을 제작했어. production 장식·애니메이션 추가는 없어.

### 74일차 추가 지시 — 비교 목업 완료

- 사용자 확인(2026-09-15): 목업과 실기기에서 2dp 디자인이 마음에 든다고 확인했고, 이어서 commit·push를 명시적으로 요청했어. **2dp 실기기 QA 통과와 Git 반영 승인 완료**로 기록해. 공휴일 방식·지원 연도나 추가 장식 승인으로 확대하지 않아.

- 방식: 앱 밖 독립 [HTML 목업](mockups/visit-calendar-2dp.html), [브라우저 캡처](mockups/visit-calendar-2dp.png). 로컬 HTML을 브라우저로 열면 돼. 앱 route·debug screen·dependency는 추가하지 않았어.
- 2026년 10월 한 달 전체를 3개 나란히 비교해. A는 10/1~7 연속 7일(주말 포함), B는 2·9·21일 3회, C는 16일 방문 샘플이야. 하단 횟수도 샘플 수치야.
- 기본 2dp, 이전 6dp로 비교 가능. 공휴일 가정을 끄면 현재 앱처럼 주말색만 보여. 목업에서만 10/3 개천절(토), 10/5 대체공휴일(월), 10/9 한글날(금)을 사용해. 위 공식 월력요항에서 확인한 샘플이며 연간 공휴일 production 구현은 아니야.
- B에서 토요일+공휴일 red와 대체공휴일 red, 방문한 공휴일 teal을 동시에 확인할 수 있어. A에서는 주말·공휴일에 방문 라벨이 우선해.
- 앱 토큰 색상, 304px 패널/256px 달력, 34px 셀, 3px 안쪽 여백, 글자 크기를 dp/sp 수치에 대응했어. 날짜 기반 epochDay mod 4와 4종 카오모지를 재현했어. Android 폰트·실제 dp 밀도와 브라우저 CSS 렌더링 차이는 남아 있어.
- 자동 점검: Node에서 실제 HTML script 실행해 3개 달력·총 26개 라벨·공휴일 전환 시 방문 라벨 수 유지·2/6px 전환 통과. 최초 점검 명령의 PowerShell 인용 오류는 stdin 전달로 수정했어(앱 코드 오류 아님).
- Chrome headless 캡처를 직접 열어 3개 월 전체, 라벨·색상·텍스트 배치를 확인했어. 첫 제한 환경 실행의 GPU 실패 후 독립 임시 프로필과 허용된 권한으로 캡처 성공. 사용자 브라우저 프로필은 사용하지 않았어.
- 내 시각 판단: A 연속 7일이 작은 종이 흔적이라는 의도를 가장 잘 보여줘. 라벨 사이 여백이 유지돼 한 덩어리로 붙지 않고, C의 16일도 뭉치지 않아 보여. 이는 브라우저 목업 의견이며 사용자 QA 승인은 아니야.
- 실제 filesDir·visit_record·방문 횟수·streak 접근 없음. 이번 추가 작업에서 production 수정 없음. 기존 2dp 한 줄 변경만 그대로 남아 있어.
- 추가 산출물은 `docs/ai/mockups/`의 HTML·PNG와 이 기록이야. HTML 정적 점검 및 `git diff --check` 통과. 앱 코드 변경이 없어 Gradle 재실행은 불필요, 직전 관련 16건·compile 통과 기록 유지. 실기기 2dp QA와 `49654c2` commit·push까지 완료했어.

### 74일차 추가 장식선 목업 승인 및 production 최소 구현

- 사용자 지정 조합을 같은 HTML 목업에 추가했어: 상단 `────── ✦ ──────`, 하단 `⋆｡────────｡⋆`.
- 첫 목업 확인 후 사용자가 양옆 선이 중간에서 끊겨 보인다고 피드백했어. 상·하단 선을 주차 구분선과 같은 달력 전체 폭으로 늘리고, 중앙 `✦`와 양끝 `⋆｡`·`｡⋆`만 작은 문자로 남겼어.
- 두 장식선은 기존 상·하단 구분선 자리를 대신해 달력 높이를 거의 늘리지 않아. 선 두께는 주차 구분선과 같은 0.5px이고, 본문 잉크보다 훨씬 낮은 대비(`InkSecondary` 계열을 약 29%로 표현)와 9px 장식 문자 크기를 사용했어.
- `장식선 보기`를 켜고 끌 수 있어 같은 3종 전체 달력에서 기존 가로선과 직접 비교 가능해. 2dp·공휴일 가정 비교 기능도 유지했어.
- Chrome headless로 선 길이 수정 후 PNG를 다시 만들고 직접 확인했어. 양쪽 여백까지 선이 이어져 기존보다 덜 끊겨 보이며, 상단 장식은 요일·날짜보다 뒤로 물러나고 하단 장식은 방문 횟수 문구보다도 조용하게 보여.
- HTML script 실행 점검: 상단 장식 3개, 하단 장식 3개, 전체 폭 선 조각 9개, 끄면 기존 구분선 6개, 방문 라벨 26개, 2px 반경 유지 모두 통과. `git diff --check` 통과.
- 사용자 확인(2026-09-15): 늘린 선 길이가 정확히 원한 지점이고 완벽하다고 명시했어. 이 승인으로 정적 프로토타입 단계를 통과했어.
- production은 `VisitCalendarDrawer.kt` 안에서 기존 상·하단 구분선 자리를 전용 장식 composable로 교체했어. 상단은 전체 폭 선 사이 중앙 `✦`, 하단은 양끝 `⋆｡`·`｡⋆` 사이 전체 폭 선이야. 선은 0.5dp, `InkSecondary` 29% 색, 문자는 9sp로 목업과 맞췄어.
- 장식 Row를 기존 구분선과 여백의 총높이에 맞추고 footer 위 여백을 조정해 달력 전체 밀도와 위치를 유지했어. 장식은 `clearAndSetSemantics`로 접근성 읽기에서 제외했어.
- 실제 방문 데이터·filesDir·visit_record·streak 접근 없음. route·dependency·저장 구조 변경 없음. 현재 변경은 `VisitCalendarDrawer.kt`, `docs/ai/mockups/visit-calendar-2dp.html`, 갱신된 PNG와 이 HANDOFF야.
- 상태: 목업 승인과 production 최소 구현 완료, production 실기기 QA 대기. 현재 변경은 commit·push하지 않았어.

### 74일차 하단 장식선 대비·문자 후보 목업

- 사용자 추가 지시(2026-09-15): 현재 `⋆｡────────｡⋆` 문자열은 유지하되 색 대비를 소폭 높인 안을 먼저 확인하고, 존재감이 부족하면 `୨୧────────୨୧`와 `୨୧ ──────── ୨୧` 후보를 비교하기로 했어. 대비를 높여도 별무늬 가독성이 약해 사용자가 `୨୧ ──────── ୨୧`를 선택했어.
- 같은 HTML 목업에 `하단 장식` 선택을 추가했어. 기본 선택은 기존 문자열 + 대비 상향(약 39%)이고, 기존 대비(약 29%)와 두 `୨୧` 후보를 같은 2026년 10월 A/B/C 전체 달력에서 비교할 수 있어.
- 상단 `✦`와 선 길이, 달력 크기, 2dp 라벨, 공휴일 가정은 유지했어. 실제 앱 코드·route·dependency·filesDir·visit_record·streak는 건드리지 않았어.
- Chrome headless로 기본 대비 상향 상태의 PNG를 갱신하고 3개 달력 전체를 직접 확인했어. 문자열은 양끝에 남고 선은 기존 승인 길이를 유지하며, 대비 상향안도 방문 라벨보다 먼저 시선을 끌 정도는 아니었어. 후보 선택별 최종 채택은 아직 사용자 확인 대기야.
- 사용자가 선택한 `୨୧ ──────── ୨୧`를 production 하단 장식에 반영했고, 하단만 대비를 39%로 올렸어. 상단 `✦`는 기존 29% 대비를 유지해 시선 우선순위를 보존했어. 목업 기본 선택도 선택안으로 맞췄어.
- 자동 검증: 단일 Gradle 프로세스로 `VisitCalendarTest`·`VisitHistoryStorageTest` 관련 테스트 통과, `:app:compileDebugKotlin` 성공. 병렬 실행 시 Kotlin incremental cache 잠금 환경 오류가 있었지만 중단 후 단일 실행으로 재검증했어.
- 상태: 선택안 production 반영 및 자동 검증 완료, 실기기 QA 대기. 현재 변경은 commit·push하지 않았어.

## 자동 검증·실행 환경

- 기존 로컬 Gradle 9.4.1 및 Android Studio JBR 사용. 저장소에 실행 wrapper가 없어 기존 로컬 `gradle.bat` 사용.
- 첫 offline 실행은 `settings.gradle.kts:8`의 기존 `foojay-resolver-convention:0.10.0`을 찾지 못해 설정 단계에서 실패. 앱 코드 오류가 아니야. build 설정을 수정하지 않았어.
- 기존 캐시·네트워크 접근 권한으로 재실행. `:app:testDebugUnitTest --tests com.postcardmemory.ui.gallery.VisitCalendarTest --tests com.postcardmemory.utils.VisitHistoryStorageTest :app:compileDebugKotlin --console=plain`.
- 장식선 production 반영 후 재검증 결과: `BUILD SUCCESSFUL in 2m 32s`. `VisitCalendarTest` 8건, `VisitHistoryStorageTest` 8건, 실패·에러 0을 XML 결과로 확인했고 `compileDebugKotlin`도 성공했어. 기존 코드의 deprecated API 등 warning만 있었어.
- 최종 `git diff --check` 통과. production diff는 장식선 전용 composable과 기존 구분선 교체 및 밀도 보정으로 한정했어. 기존 LF→CRLF 경고는 있어.
- 전체 unit test는 국소 시각 변경에 비례해 실행하지 않았어. 새 장식을 그대로 되풀이하는 테스트도 추가하지 않았어.
- 실기기 설치·실행·삭제·초기화·계측 테스트·사용자 데이터 조작은 실행하지 않았어.

## 실기기 QA·남은 위험·다음 행동

**2dp 실기기 QA는 완료했고, 새 장식선 production 실기기 QA는 대기 중이야.** 사용자가 목업의 전체 폭 선 길이와 인상을 승인한 뒤 실제 코드에 반영했어.

- 확인 대상은 `141d6cb` 위 2dp 작업트리였고, 확인된 결과를 `49654c2`에 commit했어.
- Gallery 좌측 메뉴 → 방문 달력. 여러 방문 라벨을 함께 보고 종이 조각 느낌인지, 너무 직각이거나 답답한지 확인해. 닫고 다시 열어 날짜·카오모지·횟수 유지도 확인해.
- 5~7일 연속 기록이 없다면 기록·기기 날짜를 조작하지 않고 자연적으로 쌓인 뒤 확인해. 그 조건은 미검증으로 남겨.
- 장식선 QA는 Gallery 좌측 메뉴에서 방문 달력을 열어 상·하단 선이 목업처럼 양옆까지 충분히 이어지는지, 본문·방문 라벨보다 먼저 보이지 않는지, 전체 높이와 footer 간격이 자연스러운지 확인하면 돼. 공휴일 QA는 미구현이라 해당 없어.
- history marker와 `visit_record.txt`, total/streak·하루 중복 방지, Intro·Gallery 기타 기능, Room·Migration·저장·공유 경로는 변경하지 않았어.
- 기존 위험 유지: history와 요약 저장은 단일 트랜잭션이 아니며 history 실패 시 그날 라벨이 없을 수 있어. 자정 너머 프로세스를 유지할 때 방문 집계 범위도 기존 그대로야. 이번에 이를 해결하거나 실기기에서 검증했다고 주장하지 않아.
- 과거 데이터 사고·복구 범위와 Intro 자연 도달 미검증은 archive 기록을 유지해. 코드 커밋을 사용자 데이터 백업으로 해석하지 않아.
- 다음 행동: 사용자가 장식선 production 실기기 화면을 확인해. 통과 후 별도 명시 요청이 있으면 현재 4개 tracked 파일만 commit·push할 수 있어. 공휴일은 지원 연도·유지 방침 결정 후 별도 재개 가능해.
- 전용 task checklist 도구가 없어 진행 메시지와 이 문서로 단계를 기록했어. Goal 대체나 하위 agent 위임 없음.
