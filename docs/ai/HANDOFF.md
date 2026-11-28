# HANDOFF — 2026-11-28 146일차: 인트로 필름 B안(아날로그 사진 필름) 재질

- **현재 승인:** 사용자가 [비교 목업](mockups/intro-film-texture-day146.html)에서 B안을 고르고, 실기기 정지 화면과 날짜 잉크 `#46392E`를 승인했어. 작업 브랜치 commit·push·PR·필수 CI까지 승인, **main 병합은 147일차 동적 모션 QA와 별도 병합 승인 전 금지**.
- **시작 Git:** `main` = `origin/main` = `68260cfc78661ba418e6a4c72bf885b761a50539`(PR #41 병합), 추적 변경 0. `feature/day146-intro-film-texture` 생성. 새 worktree 없음, 과거 `7b4c2bf` 보조 worktree는 건드리지 않음. 민감정보 훅 활성 확인.
- **조사(앱 실제 구조):** 인트로 필름은 사진 필름이 아니라 아이보리 종이 기록지였어(칸 `PaperSurface`, 기록 칸 `PaperField`+1dp `PaperDivider`, 천공은 `PaperTray` 원을 그린 점). Row drawBehind의 위아래 1dp 선은 칸 배경에 가려 실제로 보이지 않았어. 모션·시간표는 `IntroFilmPlan.kt`(280ms 대기·한 칸 720ms·+45%/칸·최대 2.6배·cubic(.40,0,.12,1)·140ms·손 720ms·260ms·Crossfade 180ms), 같은 날 650ms·모션0 800ms 정지. 시스템 SplashScreen API 없음(창 배경색만). 실제 준비 뒤 인트로가 불필요하게 기다리는 구조는 찾지 못했어(인트로는 고정 시간표로만 끝남).
- **구현:** 신규 `ui/intro/IntroFilmMaterial.kt`가 칸 한 개의 B안 재질만 그려(목업 색·투명도·두께 그대로): 반투명 호박색 세로 농도, 짝홀 Path로 실제로 뚫은 천공과 위 그늘/아래 밝은 단면, 얇은 가장자리 짙은선·밝은선, 칸 노출 면, 칸 번호 고정 시드(목업 mulberry32 이식) 노출 흔적·비네트·먼지, 그레인 타일 2장. `AppIntroScreen.kt`는 칸의 종이 배경·그려진 천공·회색 테두리·가려진 선을 이 재질로 바꾸고, 필름 날짜 잉크만 `#5B5046`→`#46392E`(사용자 선택). 소인·문구·손 도장·모션·배치·전환 코드는 그대로야. 새 이미지·dependency·Gradle·Room 변경 없음.
- **목업과의 차이(사용자 확인):** 칸 가장자리 blur(0.6px)는 반투명 1.2dp 테로 대체. 그레인은 타일 1픽셀=1dp(목업 PC 실제 크기 보기와 비슷하게; 기기 픽셀 그대로면 거의 안 보임). 정지 화면으로 승인받았어.
- **가독성:** 기기 캡처 날짜 주변 실측과 칸 16개 노출 계산 기준, 기존 잉크 대비 3.43~3.88:1 → `#46392E` 4.88~5.52:1. 승인 후 폰에서 진해진 날짜를 확인했어.
- **로컬 자동검증 — 실행/통과:** `testDebugUnitTest assembleDebug --no-daemon --max-workers=1`(Gradle 9.9.1/Zulu17) 성공. XML **1354/1354·148 suites·실패/오류/skip 0**, Debug APK 성공. 신규 `IntroFilmMaterialTest` 7건(TEST-COVERAGE-MAP 146일차 절).
- **실기기 — 실행(SM-S936N, 사용자 승인 `install -r --no-streaming` 2회, 같은 package·서명·schema20):** 두 번 모두 직전/직후 내부 166개 파일 해시 변화 0, 설치 APK와 로컬 빌드 SHA-256 일치. 정지 인트로(같은 날 재실행)·천공 투과·가장자리·갤러리 진입·홈 복귀 정상, 크래시 로그 0. 기기 설정(animator 1.0·font 0.9)은 바꾸지 않았어.
- **성능(같은 기기, 백그라운드 `am kill` 후 `am start -W`, 각 5회):** Cold 중앙 484ms(465~498)→486ms(483~512), Hot 48→41ms. 시작 후 2.5초(인트로+갤러리 첫 화면) 끊긴 프레임 평균 3.3%→3.9%, p99 93~113→105~150ms. PSS 1회 355→368MB. 인트로만 분리한 프레임과 인트로 실제 표시 시간은 미측정. 지시대로 추가 최적화는 안 했어.
- **데이터/보호:** 앱 데이터 삭제·초기화·force-stop·uninstall 없음. QA 뒤 바뀐 내부 파일은 실행 표식(`profileInstalled`·ActivityThread prefs)뿐, DB 해시 동일. 날짜 조작·방문 기록 수정 없음. SAFE 38/38은 `docs/ai/privacy-rewrite/protected-baseline.json` 기준 일치, 기존 미추적 48/48은 145일차 세션 시작 때 저장한 로컬 기준 JSON과 일치(그 이후 145일차 안의 변화는 이 기준으로 알 수 없음). 새 미추적은 이번 작업 파일뿐이고 함께 commit해.
- **147일차로 이월(미검증):** 11/28은 이미 방문 기록이 있어 손 도장 이송을 못 봤어. 11/29 첫 실행에서 실제 필름 이송, 필름·질감 이동 일체감, 손 도장 진입·접촉, 소인 정착, 갤러리 전환, 시스템 애니메이션 끄기, 큰 글자, 가능한 범위의 인트로 구간 성능을 확인해. 날짜 조작·방문 기록 삭제·DB 수정 금지. TalkBack 음성·저사양 기기 미검증. instrumentation 미실행(해당 없음), CONDITIONAL/FORBIDDEN 실행 불가, emulator 미사용.
- **Git 마감:** 이 절은 commit 전 작성이야. PR 번호·head SHA·CI 결과는 대화 완료보고에서 확인해. main 병합은 147일차 QA 뒤 사용자 승인 대기.

---

# HANDOFF — 2026-11-27 145일차: 기억 저울 B안 질감(오래된 우체국) 적용

- **현재 승인:** 사용자가 질감 비교 [목업](mockups/memory-scale-texture-day145.html)의 B안을 고르고, 실기기 확인 뒤 하단 영어 문구 `MEMORY POST OFFICE` 제거를 조건으로 최종 승인했어. 추가 디자인 탐색은 하지 않아. 작업 브랜치 commit·push·PR #41 갱신까지 승인, **main 병합은 별도 승인 전 금지**.
- **시작 Git:** `feature/day144-memory-scale` = origin = `b227ec9514ae4ae7078303778ef219c0f3069bc1`, origin/main `6c2aa0d`. 추적 변경 0, PR #41 OPEN·CI 성공·CLEAN. 새 worktree 없음.
- **조사 결과(투박함 원인):** ① 모든 부품이 단색이라 빛 방향이 없었어. ② 2~3px 진갈색 외곽선이 만화 같은 인상을 주고, 엽서 층마다 테두리가 있어 줄무늬 막대처럼 보였어. ③ 종이 타일이 그림 좌표 배율 탓에 약 2.6배 확대된 채 6~8%로 깔려 사실상 보이지 않았어.
- **구현(`MemoryScaleDrawing.kt`만, 기하·모션 동일):**
  - 본체: 세로 명암·좌우 빛·윗곡선 반사, 무광 도장 알갱이, 외곽선 1px/70%.
  - 받침대·받침판: 윗면/앞면 경계, 반사선, 피스톤에 지는 그림자(받침대와 같이 이동).
  - 피스톤: 가로 원통 명암만 써서 길이가 바뀌어도 무늬가 늘어나지 않아. 본체 입구에 고정 칼라.
  - 눈금판: 종이 타일을 갤러리 배경과 같은 360dp 결로 보정해 42%, 금속 링·안쪽 그늘. 눈금 굵기(큰 눈금 1.4·작은 눈금 0.8)만 정리하고 위치는 그대로.
  - 바늘: 같은 각도로 도는 화면 기준 오프셋 그림자, 반사선, 볼록한 축 머리.
  - 엽서 더미: 층별 미세 색 차이, 아래 가장자리만 옅은 선, 윗면 종이 결, 접촉 그림자.
  - 하단 영어 문구와 그 전용 테두리 Paint를 제거하고 대체 장식은 넣지 않았어.
  - 그림자는 흐림 없이 오프셋 도형·그라디언트로만 그려. Brush·Path는 drawWithCache에서 한 번 만들고, 64×64 알갱이 타일은 `remember`로 composition당 한 번만 만들어.
- **유지:** `MemoryScaleModel.kt`(400ms 접촉·2400ms 정착·감쇠·눈금 확장)와 화면 배치, 기간 선택, 미래 우체통은 그대로야. `GalleryScreen.kt`는 종이 타일 크기 상수를 `private`→`internal`로 바꾼 한 줄뿐이야. 새 이미지·dependency·Gradle·Room 변경 없음. 보호 자료 `postcard_paper_fiber_tile.png`는 사용하지 않았어.
- **로컬 자동검증 — 실행/통과:** 최종 `testDebugUnitTest assembleDebug --no-daemon --max-workers=1`(Gradle9.9.1/Zulu17) 성공. XML **1347/1347·147 suites·실패/오류/skip0**, Debug APK 성공. `MemoryScaleWiringStructureTest` 1건의 assertion을 새 이름·알갱이 1회 생성·영어 문구 없음으로 갱신했어(TEST-COVERAGE-MAP 145일차 절).
- **실기기 — 실행(SM-S936N, 사용자 승인 `install -r`, schema20·같은 서명):**
  - 첫 질감판 설치는 기기가 스트리밍 전송본을 파싱하지 못해 세션이 버려졌어. 서명 문제가 아니었고 기존 앱은 그대로였어. 같은 설치를 `--no-streaming`으로 한 번 다시 해서 성공했어.
  - 두 번 설치 모두 직전/직후 내부 149개 파일 해시 변화 0, 설치 APK와 로컬 빌드 SHA-256 일치.
  - 질감판 QA: 기존 화면과 같은 10장 자리에서 정착. 11월 5장·9월 0장·연간 10장·측정 중 연속 기간 전환·페이지 이탈/재진입·우체통 진입/Back·갤러리/책갈피 정상, 앱 종료 오류 0건.
  - 애니메이션 끄기(0으로 바꿨다 1.0 복구)는 즉시 정착. 큰 글자(1.3으로 바꿨다 0.9 복구)에서도 스크롤 없이 우체통까지 표시. 접근성 정보는 "기억 저울, 엽서 N장, 최대 눈금 60장" 유지.
  - gfxinfo는 위쪽 시계 포함 99% 11ms 이내, 지연 0.19%.
  - 문구 제거판: 문구가 사라졌고 외형·전체 10장·월간 5장 모션·semantics가 유지됐어. 오류 0건.
- **데이터/보호:** QA 중 내부 파일 변화는 실행 표식(`profileInstalled`·ActivityThread prefs)과 앱 실행 시 생기는 당일 방문 기록뿐이었고, DB 파일 해시는 설치·QA 전후 같았어. 마지막 읽기 전용 DB 사본에서 schema20·integrity ok·전체 11장/일반 보관 10장을 확인했어. 사본·화면은 저장소 밖 로컬 임시 경로에만 있어. schema19/20 사용자 백업 폴더는 열거나 바꾸지 않았어. SAFE 38/38·시작 미추적 48/48 해시 유지(새 미추적은 이번 목업 1개뿐이고 이번 commit에 함께 넣어).
- **미검증/경계:** TalkBack을 실제로 켜서 듣지는 않았어(접근성 트리만 확인). 기존 버전과 나란히 잰 프레임 비교값이 없어. 저사양 기기 성능은 미검증이야. 실사용 instrumentation 미실행(해당 없음), CONDITIONAL/FORBIDDEN은 실행 불가, emulator 미사용.
- **Git 마감:** 이 절은 commit 전 작성이야. 최종 PR #41 head SHA·CI 결과는 대화 완료보고에서 확인해. 다음은 사용자 main 병합 승인 대기야.

---

# HANDOFF — 2026-11-26 144일차 추가: 기억 저울 B안 실제 앱 적용·화면 간소화

- **현재 승인:** 사용자가 HTML의 B안(2.4초)을 채택하고 실제 Android 적용을 승인했어. 이후 실기기 B안 형태·접촉·반동·정착을 확인하고 남은 QA/Git 마감 진행을 승인했어. 마지막 추가 지시로 **12개월 그래프 완전 제거와 세로 간소화**를 승인했으므로, 아래 조사 당시의 그래프 보존·앱 적용 미승인 상태는 역사 기록이야. main 병합은 여전히 별도 승인 전 금지야.
- **시작/작업 Git:** 기존 `feature/day144-memory-scale`, 시작 HEAD와 origin 작업 브랜치 `df2956d4f980b8e7a075e8a29a125b0454306700`. 새 worktree/프로젝트 없음. 이번 절은 구현 commit 전 작업트리 상태야. commit/push/PR/CI의 최종 결과는 대화 완료보고에서 확인해.
- **앱 구성:** 제목 → 전체/연간/월간 기간 선택 → B안 Canvas 저울 → 실제 장수/최대 눈금 → 기존 미래 우체통. `GalleryMemoryScalePage`·`MemoryScaleDrawing`·`MemoryScaleModel`로 화면/그리기/집계를 분리했어. 새 이미지·dependency·Gradle·Room 변경 없음.
- **B안:** HTML과 같은 400ms 접촉·2400ms 종료, 받침대 w21/d20·바늘 w16/d15, 이동량16·각도−120~120·최대12겹. 같은 하중 시각으로 받침대/피스톤/정착 더미/바늘을 계산하고 종료값을 정확히 맞춰. 최대 눈금 이탈 방지를 위해 응답을 clamp하는 점은 HTML과 달라. 프레임 상태를 draw 단계에서 읽고 Path/눈금/문자 Paint를 캐시해. 대상 id 집합·기간이 바뀌거나 화면 재진입할 때 재생하고, 일반 재구성/정렬/꾸미기 변경은 재생 키를 바꾸지 않아. page/lifecycle 비활성·composition 제거 시 effect가 취소돼. 시스템 animator scale 변경을 관찰해 0이면 즉시 최종 자세야.
- **간소화:** 그래프/펜/20장 막대/초과표시/질감 행렬과 전용 상수를 제거했어. `GalleryPostcardMonth`·`postcardMonthsForYear`는 역할 중심 이름으로 보존하고 실제 연간/월간 집계에서 재사용해. 이미지 참조 조사 결과 기존 gallery 종이 타일은 배경·편집·저울에서 공용이라 제거하지 않았어. 저울은 같은 440 단위 기하를 유지하며 y65~390(440×325)만 표시해 위/아래 빈 공간을 줄였어. 낙하 더미는 화면 위 경계에서 들어오고 접촉 좌표·하중식은 같아. 제목은 페이지 점과 같은 상단 높이를 쓰고, 결과·우체통 간격을 줄였어. 큰 글자/작은 화면은 세로 스크롤로 보호해.
- **실제 집계:** DAO→Repository→GalleryViewModel 기존 흐름 유지. NONE 엽서만 읽고 기본 기간 ALL, 연간/월간은 capturedAt·기기 시간대·선택 연월 기준이야. 60/100/200/500/1000 이후 2·5·10 계열 Long 눈금으로 확장하므로 HTML 10000 입력 제한을 가져오지 않았어. DB/파일에 통계를 쓰지 않아.
- **명칭/호환:** 사용자 기능명과 진입 Composable은 기억 저울/`GalleryMemoryScalePage`. 구 그래프 식별자는 production에서 제거했고 월별 집계는 위 역할 이름으로 정비했어. `GalleryPageFormat.DENSITY`와 `.name` Saver는 그대로 유지해 기존 복원 문자열을 깨지 않아. 픽셀/배경 패턴 Density와 과거 archive는 변경하지 않았어.
- **우체통:** 기존 봉투·종이·도착/D-day·semantics·`onOpenFutureMailbox`/`futureMailbox` 경로를 재사용해. Slot의 `keepVisible` 경로로 글자 크기에 맞는 높이를 확보하며 topGap만8dp로 조정했어. 기존 날짜 포매터 `monthlyGridDayLabelFormatter`와 일반 갤러리/책갈피/방문 달력의 공용 동작을 보존했어.
- **로컬 자동검증 — 실행/통과:** 간소화 전 전체 JVM1362/1362·148 suites·Debug 빌드 성공. 간소화 후 첫 컴파일에서 그래프 사이에 있던 공용 날짜 포매터 누락을 발견해 원문 복원했어. 다음 전체 JVM1347 중 자정 소비자 구조 검사1건이 옛 파일 위치를 가리켜 실패했고, 실제 저울 화면과 GalleryViewModel을 검사하도록 목록을 갱신했어(방문 기록 생성 금지 assertion 유지). 최종 `testDebugUnitTest assembleDebug --no-daemon --max-workers=1` 성공(Gradle9.9.1/Zulu17), XML **1347/1347·147 suites·실패/오류/skip0**, Debug APK 성공이야. 초기 기존 플러그인 접근 실패는 네트워크/캐시 환경 문제였고 설정 변경 없이 해결했어.
- **테스트 범위:** 신규 모션/집계 순수11건·연결 구조5건. 기존 월별 집계8건은 `GalleryPostcardMonthTest`로 유지하고 제거된 그래프 전용 계산8건·구조7건은 제거했어. 전체 source @Test1347/test파일146/helper1, instrumentation21건/8파일 무변경. 새 UI의 실제 프레임·TalkBack·lifecycle 동작은 순수/구조 검사 성공으로 확대하지 않아.
- **실기기 — 실행:** 사용자 설치 승인 후 SM-S936N에 같은 package/서명·schema20 APK를 `adb install -r`로 설치했어. 최초 설치 직전/직후 내부147개 파일 해시 변화0. 첫 B안 화면의 0장·11월5장 표시와 사용자 모션 만족을 확인했어. 같은 승인 범위의 간소화판도 같은 package/서명·schema20으로 `install -r` 설치했고 직전/직후 내부148개 파일 해시 변화0, 설치 APK와 로컬 빌드 SHA-256 일치를 확인했어. 간소화판은 제목/기간/저울/결과/우체통을 스크롤 없이 함께 표시했어. 실제 전체10장·2026년 연간10장·11월5장·9월0장·다시5/10장 전환이 읽기 전용 DB 집계와 같았어. 우체통 진입/Back 복귀와 갤러리 페이지 왕복 후 저울·선택 기간·수량을 확인했고 AndroidRuntime fatal 표식0이야. 사용자가 간소화 화면을 확인한 뒤 기간 선택창을 갤러리 정렬 메뉴와 통일하도록 추가 요청했어. 기간/연도/월 모두 RectangleShape·PaperSurface·InkPrimary·선택 SunsetGold16% 배경/Bold로 맞췄고 기기에서 종이색 직각 메뉴와 선택 강조를 확인했어. 메뉴 수정본도 전체 JVM1347/1347·147 suites·실패/오류/skip0 및 Debug 빌드를 통과했고 같은 서명/schema20 재설치 직전/직후 내부148개 파일 해시 변화0이야. 현재 기기1080×2340/450dpi/fontScale0.9/animator1.0을 읽었고 전역 설정은 바꾸지 않았어.
- **데이터/보호:** 기기 DB의 로컬 읽기 전용 사본에서 schema20·integrity ok·전체11장/일반보관10장(미래발송 제외)/11월5장을 확인했어. 기존 schema19/20 백업은 열거나 덮어쓰거나 복원하지 않았어. QA 명령으로 원본 기기 DB·엽서·사진을 직접 생성/수정/삭제하지 않았어. 메뉴 최종 수정본 QA 뒤에도 기기 DB schema20·integrity ok와 엽서 id/capturedAt/futureMailState가 읽기 전용 기준과 같았어. SAFE38/38·시작 미추적48/48 해시 유지. DB 확인 사본·설치 APK 비교본·화면/XML 증거는 저장소 밖 로컬 임시 경로에만 있어.
- **미검증/경계:** 실사용 instrumentation 미실행(이번 UI 확인 수단 아님), CONDITIONAL/FORBIDDEN은 격리 환경 없어 실행 불가. 큰 글자·TalkBack 음성·실제 시스템 모션0 전환·저사양 성능은 별도 감각 QA 미검증. 로컬 emulator 사용 없음. 신규 Markdown 없음. TEST-COVERAGE-MAP을 최종 실행 수·제거/유지 보호 범위로 갱신했어.
- **Git 마감:** 사용자에게 승인된 작업 브랜치 commit/push/PR/CI는 최종 검증·필요 QA 후 수행해. main 병합 금지. 최종 PR head/base SHA와 CI 결과를 보고하고 별도 병합 승인 대기에서 멈춰.

---

# HANDOFF — 2026-11-26 144일차: 기억 저울 구조 조사·HTML 비교 목업

- **사용자 관점:** 앱은 바뀌지 않았어. 기존 기억 밀도·미래 우체통·엽서·책갈피는 그대로야. [단일 HTML 비교 목업](mockups/memory-scale-day144.html)을 브라우저에서 열어 A/B/C 모션·수량·눈금·기본 도형/색상/질감을 비교할 수 있어. 디자인과 실제 Android 적용은 사용자 승인 대기야.
- **승인 범위/운영:** 수동 표준 모드. 144일차 지시서의 조사·HTML 제작·검증·HANDOFF·작업 브랜치 Git 마감만 수행해. production/test/리소스/Gradle/Room 수정과 이미지 생성 없음. 신규 Markdown 없음. 마지막 안전 기준은 아래 시작 SHA이며, 목업 변경은 앱 동작과 분리돼 있어.
- **시작 Git:** `main` = `origin/main` = `6c2aa0d2441c6f7b37634079fa4ab7ea68d150ed`, 추적 파일 변경 없음·기존 미추적 파일 48개. 원격 main도 실제 조회로 같았어. `feature/day144-memory-scale` 생성, 오래된 잔여 브랜치와 worktree는 정리하지 않았어. 민감정보 pre-commit/pre-push 훅 활성 확인. 아래 143일차의 오래된 main 병합 전·schema 19 설명만 현재 사실로 최소 정정했어.

## 실제 구현 조사

- **진입:** `MainActivity.kt`의 갤러리 → `GalleryScreen.kt`의 `HorizontalPager`. `GalleryPageFormat.MONTHLY` 다음 `DENSITY` 페이지가 고정돼 있어. 기억 밀도 전용 nav route·Screen 파일·ViewModel은 없어.
- **원천:** `PostcardDao.getAllPostcards()`는 `futureMailState='NONE'`만 `capturedAt DESC`로 조회해. `PostcardRepository.getAllPostcards()` → `GalleryViewModel.postcards` StateFlow → `displayedPostcards` → `GalleryDensityPage`. 검색은 월별 페이지 이탈 때 비워지고, 정렬은 개수에 영향 없어. 책갈피 필터도 월별 전용이야.
- **실제 계산:** `memoryDensityMonthsForYear`가 `capturedAt`을 시스템 시간대의 `YearMonth`로 묶고 올해 1~12월을 0 포함해서 반환해. `GalleryMemoryDensityMonth.count=postcards.size`. 방문 일수·기억 점수·책갈피 가중치가 아니야. `rememberTodayDate()`로 연도 경계를 갱신해. 현재 기간 선택 UI는 없어.
- **현재 표현:** `GalleryMemoryDensityBar`는 `count/20`(0~1), 20장 초과는 `+`, `GalleryMemoryDensityFoot`는 월 숫자만 표시해. `MemoryDensityPenStroke` 계열의 중성펜 획과 갤러리 공용 종이 타일을 Canvas에 그려. 별도 저울용 이미지·하중 모션은 없어. semantics는 월과 기억 개수를 막대에 설명하고 하단 중복 발화를 지워.
- **미래 우체통:** `GalleryViewModel.futureMailGroups`는 `getFutureMailPostcards()`(SENT) + `dayBoundaryTicks()` → `buildFutureMailGroups`. `GalleryFutureMailDiorama.kt`의 도착 우선 상태·봉투·D-day·종이 전체 클릭을 유지할 수 있어. `onOpenFutureMailbox` → `onNavigateToFutureMailbox` → 기존 `futureMailbox` route. 퀵셀렉트의 별도 진입도 유지해야 해. 현재 디오라마는 남는 높이가 부족하면 숨겨지는 구조라 실제 교체 시 짧은 화면 배치를 다시 검증해야 해. 목업 봉투/D−7은 가상 배치이며 실제 우체통 화면은 구현하지 않았어.
- **테스트:** `GalleryMemoryDensityTest`는 월별 계산·20장 비율·초과·펜/질감, `GalleryMemoryDensityStructureTest`는 현재 12개월 그래프 구조·접근성·상호작용 제한을 보호해. `GalleryPagerTargetIndexTest`, `GalleryViewSelectionStructureTest`, `GalleryPondPageLockStructureTest`, `GalleryPaperBackgroundStructureTest`, `GalleryFutureMailDioramaTest/StructureTest`에도 연결돼 있어. 승인된 UI 전환 때 새 의미에 맞게 관련 검증을 바꾸되 기존 보호를 단순 삭제하면 안 돼.

## 명칭 전수 조사와 후속 변경 후보 (이번에는 변경 안 함)

| 실제 명칭 | 위치 | 역할 | 변경 필요성 | 호환성 영향 |
|---|---|---|---|---|
| `기억 밀도` / `기억밀도` | gallery Kotlin 주석, DAO 주석, 관련 테스트, HANDOFF·TEST-COVERAGE-MAP | 기능 설명 | 향후 현행 설명 정비. 현재 main에서 이 문자열을 렌더링하는 사용자 제목은 없고 밀도 페이지 제목은 연도야 | 사용자 새 표시명은 기억 저울. archive·과거 기록은 소급 변경 안 함 |
| `GalleryDensityPage` | `GalleryScreen.kt` | private Composable | `GalleryMemoryScalePage` 후보 | 내부 호출·구조 테스트 함께 수정 |
| `GalleryMemoryDensityMonth`, `memoryDensityMonthsForYear` | 같은 파일 | 월별 집계 모델·함수 | 재사용하면 `GalleryPostcardMonth`, `postcardMonthsForYear`처럼 역할 이름 권장 | 기존 날짜/시간대·0월 보장 테스트 유지 |
| `memoryDensityHeightFraction`, `memoryDensityHasOverflow` | 같은 파일 | 20장 막대 계산 | 저울 눈금·각도 계산으로 교체 검토 | 종전 함수명만 치환하면 의미가 틀려져 |
| `MEMORY_DENSITY_FULL_POSTCARDS`, `PLOT_HEIGHT`, `OVERFLOW_MARK_HEIGHT`, `BAR_WIDTH_RATIO`, `PEN_OPACITY`, `PEN_SEED` (뒤 5개도 `MEMORY_DENSITY_` 접두) | 같은 파일 | 기존 막대/펜 상수 | 막대 제거 확정 뒤 reference 조사하고 제거/대체 | 20장 기준을 저울 기준으로 자동 전파 금지 |
| `MemoryDensityPenStroke`, `memoryDensityRulerStroke`, `memoryDensityGelPenBarStrokes`, `memoryDensityGelPenPlusStrokes`, `memoryDensityPaperColorMatrices` | 같은 파일 | 기존 그림 전용 획·색 행렬 | 새 저울에 필요하지 않으면 사용처 검증 후 제거. 재사용이면 역할 기반 명명 | Canvas·구조 테스트와 연결 |
| `GalleryMemoryDensityBar`, `GalleryMemoryDensityFoot` | 같은 파일 | 막대·월 숫자 | 승인 시 저울 component로 교체 | 12개월 비교 기능이 사라지는 제품 판단 필요 |
| `GalleryPageFormat.DENSITY` | `GalleryPageFormat.kt`, pager·복원·관련 테스트 | 페이지 enum | `SCALE` 후보이나 기존 이름 유지도 가능 | **`PageFormatSaver`가 `.name` 문자열 저장·valueOf 복원. `DENSITY`→새 값 alias 복원 필요**. Room 영구 키가 아니라 saveable 상태야 |
| `GalleryMemoryDensityTest`, `GalleryMemoryDensityStructureTest` | `app/src/test/.../ui/gallery` | 현재 기능 검증·파일명 | 기능 전환 시 새 역할로 명명·검증 재설계 | 다른 구조 테스트 내 문자열/reference 동시 점검 |
| `GalleryViewModel`, `PostcardRepository`, `PostcardDao`, gallery 패키지·파일 | 공용 코드 | 월별·우체통과 공유 | 기억 저울 이름으로 바꾸지 않아 | 전용 `MemoryDensityViewModel/Screen`은 실제로 없어 |
| `futureMailbox` | `MainActivity.kt` | 우체통 nav route | 유지 | 밀도 전용 route는 없어. Room/설정/직렬화에서 별도 밀도 영구 식별자는 발견되지 않았어 |
| `LocalDensity`, DrawScope `density`, `backgroundPatternDensity`, `randomDensity` | 화면·꾸미기/저장 | 픽셀 배율·무늬 밀도 | 변경 대상 아님 | 단순 Density 치환 금지 |

## 설계·목업 결과

- **UI 문법 사전 판단:** 조회형 콘텐츠의 신규 인터랙션 실험이며 144일차 지시서가 HTML A/B/C·기간/눈금/표현 비교를 명시 승인했어. 기존 종이·봉투 역할을 참고하되 새 저울과 기간 선택은 기존 앱에서 확정된 문법으로 주장하지 않아. 실제 Compose 교체는 STOP/승인 대기야.
- **추천 후보:** 일반 보관 엽서(NONE) 개수·단위 `장`, 단계 확장 눈금 + 상시 범위 표시, B 손맛 모션. 전체/연간/월간 중 기본 기간, 미래 발송분 포함 여부, 기존 12개월 비교를 대체할지 사용자가 결정해야 해. 월간이면 기존 월별 함수 재사용, 전체면 기존 목록 size로 충분해. 새 저장 통계/DB 변경 불필요, schema 20 유지 가능해.
- **눈금:** 고정 60·100·200·500·1000 비교 가능. 초과는 바늘을 끝에 두고 정확한 실제 장수+범위 초과를 별도 표시해. 확장은 60→100→200→500→1000→2000→5000→10000. 60→61 때 바늘이 뒤로 가는 단점이 있어. 재생 중 눈금 범위는 고정해. 기간 선택은 A/B와 독립이며 목업에서는 동일 시험값을 사용해(기간별 실제 집계 아님).
- **구조:** 고정 본체/눈금판, 길이가 변하는 지지 피스톤, 같은 좌표계의 받침대+정착 엽서, 별도 낙하 엽서, 고정 축 바늘. 접촉 전 하중 0, 접촉 후 같은 step history가 받침대·바늘 spring에 전달돼. 최종 각도 `-120 + 240 * min(개수/범위,1)`, 받침대 `16 * min(개수/범위,1)`. 마지막 프레임을 목표에 정확히 맞춰. 더미는 최대 12겹 대표 표현이라 두께가 장수의 선형 그래프는 아니야.
- **A:** 1.2초·임계 감쇠·신속 측정. **B:** 2.4초·작은 감쇠 반동·손맛. **C:** 400ms 첫 접촉 이후 230ms 간격 적재, 180ms 낙하, 최대 4.2초·대표 더미. A/B/C 모두 같은 구조·입력·눈금·크기야. 모션/수량 변경은 이전 RAF를 취소하고 빈 저울에서 다시 시작해.
- **색·질감:** 기본 무채색→아이보리/브라운→절차적 섬유·금속 결·눈금 인쇄/접촉·받침대 그림자 순서. 모두 같은 기하 구조야. 실제 앱 종이 타일과 픽셀 동일성은 주장하지 않아.
- **이미지 판단:** 본체·피스톤·받침대·엽서·눈금·바늘은 Compose Canvas/Path와 graphicsLayer로 충분해. 새 이미지 필수 아님. 기존 종이 tile 재사용이 유리할 수 있고 새 금속 bitmap은 사용자 승인 후 성능/미감 이점이 있을 때만 검토해. 사용하더라도 본체·받침대·엽서·눈금판·바늘은 독립 레이어, 바늘/눈금 일체 이미지는 금지해.

## 검증·현재 마감 상태

- **로컬 자동검증 — 실행:** JS syntax, Edge headless 실제 브라우저에서 162개 수량/모션/범위 계산 조합(지정 0/1/11/30/60/100/500/1000 + 10000, 고정 5범위+확장)의 정확한 정착 확인. 각 조합의 17ms 표본에서 유한 값·음수 하강 없음·더미 상한 확인. 기본/색상/질감 × A/B/C 9회 실제 RAF 종료(3장)와 결과/각도 일치, 각 모드 질감 표시 여부 확인. 모션 중 전환·빠른 재생/초기화·30회 연속 수량/모션/초기화·감소·입력 clamp·범위 초과·체크박스와 OS prefers-reduced-motion 즉시 정착 확인. 최종 목업 런타임 예외 0. 외부 라이브러리·네트워크 자원·앱 데이터 접근 없음.
- **브라우저 시각 검증 — 실행:** 기본 도형 검사 뒤 접촉 좌표의 3단위 틈과 C안 겹친 낙하 구간을 보정하고 재검증. 색상/질감 적용 뒤 1280px·390px 캡처를 직접 확인, 320/360/390/740px 가로 overflow 0. 작은 눈금은 작으므로 정확한 결과 텍스트가 판독 기준이야. 임시 브라우저 profile·검증 script·화면 캡처는 저장소 밖 임시 경로에만 두고 공개 Git에는 넣지 않아.
- **보호:** SAFE 기준 **38/38**, 시작 미추적 **48/48** SHA-256이 시작과 최종 검사에서 모두 일치했어. schema 19/20 사용자 백업은 열거나 변경하거나 외부 전송하지 않았고 복원도 실행하지 않았어. 기기·DB·갤러리·설치 접근 없음. 백업의 현시점 복구 가능성을 재검증한 것은 아니야.
- **Android JVM/Debug 빌드 — 불필요:** 앱/test/빌드 코드 변경 없음. 1346개 통과는 143일차 이력이지 이번 실행 결과가 아니야. TEST-COVERAGE-MAP 변경 없음.
- **실사용 기기 instrumentation·실기기 감각 QA — 불필요:** 앱 무변경. Android 적용 후 실제 저울·작은 화면·큰 글자·TalkBack·OS 모션 설정은 미검증이야. 로컬 emulator 사용 없음.
- **GitHub Actions CI:** 143일차 main push success는 실제 조회했어. 144일차 새 CI는 아직 미실행이야. 작업 브랜치 push만으로 CI가 실행된다고 가정하지 않아.
- **사용자 QA/디자인 — 대기:** HTML A/B/C와 눈금/기본 기간 선택을 검토해야 해. 이는 실제 Android 구현의 승인 관문이야. 앱 실행 QA는 이번 산출물에 불필요하고 목업을 검토 가능한 작업물로 commit/push하는 범위는 144일차 9절에 명시돼 있어.
- **Git 작성 시점:** 아직 commit 전이야. 변경은 이 HANDOFF와 HTML 1개뿐이며, 작업 지시대로 명시적 stage·작업 브랜치 commit/push 후 SHA·동기화 여부를 최종 보고에 남겨. PR은 디자인 검토 후 생성할 후보로 보류해. main 병합은 별도 승인 전 금지야.
- **다음 출발점:** 사용자 모션·눈금·측정 기간 선택 → 실제 앱 교체 범위와 기존 연간 비교 보존 여부 확정 → 최소 Compose 정적 구조·우체통 배치 검토. 이번 목업을 최종 디자인 확정이나 Android 적용 승인으로 해석하지 않아.

---

# HANDOFF — 2026-11-25 143일차: 기억 책갈피 구현·실기기 QA 완료, main 병합 완료

- **구현:** 승인 A로 색 6개 추가(9색·81조합, schema 20 그대로)와 가는 가장자리 선. 편집 화면 더보기에서 여는 제작 화면(종이·색·작은 흔적, 저장하지 않는 미리보기, 저장 성공 뒤에만 나오는 930ms 끼우기 모션, 다시 꾸미기, 책갈피만 빼기), 월별 갤러리 표시, 정렬 메뉴의 책갈피 있는 엽서만 보기. DetailViewModel·초안·확정 저장·공유 exporter 무변경. 연못·쉼터·쫑쫑컵·흔들어서 한 장·기억 밀도에는 책갈피를 그리지 않아.
- **자동 검증:** 최종 코드 전체 JVM **1346/1346**(146 suites, 실패·오류·skip 0), `assembleDebug`·`assembleDebugAndroidTest` 성공. 계측 테스트는 컴파일만 했어.
- **실기기 QA(사용자):** 만들기·모션, 재시작 유지·다시 꾸미기·빼기, 접근성(애니메이션 끄기·TalkBack·큰 글자) 정상 보고. 갤러리 노출은 "조금 더 잘 보이게" 요청으로 5.5%→9.5%로 늘려 재설치했고 "딱 좋아" 확정(작업지시서의 5~6%보다 사용자 확정값이 우선).
- **미검증:** 실제 Android 자동 백업·복원, 복사본 복원, 실제 APK 교체 다운그레이드. 프로세스 강제 종료 뒤 제작 화면 복원은 JVM 세션 테스트까지만 했어. 원격 CI 결과와 PR은 대화상 최종 보고에서 확인해.
- **Git(144일차 현재 상태 정정):** PR #40은 Merge commit `6c2aa0d2441c6f7b37634079fa4ab7ea68d150ed`으로 병합됐고 local main=origin/main이야. 해당 push의 main CI [45609619884](https://github.com/KimJinha-JJin/post-card-memory/actions/runs/45609619884)는 success로 확인했어. 아래 설치 실험의 main schema 19는 당시 기준이며 현재 main은 schema 20이야.

## schema 20 설치 이후 주의사항

- **현재 사실:** 사용자 B-1 승인으로 SM-S936N에 `feature/day142-memory-bookmark` schema 20 APK를 `adb install -r`로 설치·실행했어. 이후 받은 다운그레이드 확인 지시(설치 전 조건)보다 설치가 먼저였어. 실기기 DB는 19→20이고, 첫 실행 직후 엽서 11장·37컬럼이 설치 전과 같았어. 이후 사용자 추가 승인으로 길이 수정본을 같은 방식으로 한 번 더 설치했고, 설치 직후 DB·files·shared_prefs 해시 변화는 0이었어. 그 밖의 APK 설치는 매번 새 승인이 필요해.
- **현재 설치 보호(144일차 정정):** 현재 main은 schema 20이야. schema 19인 과거 main·다른 브랜치·CI APK의 설치는 금지해. Android Studio Run도 설치 효과를 먼저 확인하고, schema 20 이상이어도 실기기 설치에는 별도 승인이 필요해. 20→19 migration은 없어.
- **브랜치 전환:** 코드 조회·PC 테스트를 위한 전환은 괜찮지만 기기 설치는 schema 20 이상인 이 브랜치 APK로만 해. 이후 파생 브랜치도 schema 20을 포함해야 해.
- **앱이 안 열리면:** 앱 제거·데이터 삭제·초기화·재설치 우회 금지. 그대로 멈추고 사용자에게 알린 뒤 schema 20용 수정판을 검토해. 역방향 migration이나 DB 초기화·destructive fallback을 임의로 추가하지 않아(main·이 브랜치 소스 모두 해당 설정 없음).
- **백업 한계:** 설치 전 앱을 닫고 DB·files·shared_prefs 146개를 PC 저장소 밖으로 복사해 해시 일치·DB 무결성·사진 11개를 확인했어. 두 번째 설치 전에도 schema 20 상태 147개를 따로 복사했어. 복원은 검증하지 않았어. Android 자동 백업도 미검증이야.
- **격리 시험(PC Robolectric/API 34, 가상 엽서 11장, origin/main `0e30c10`과 이 브랜치의 실제 `DatabaseModule`):** main으로 schema 20을 5회 열면 매번 `A migration from 20 to 19 was required`로 실패하고, 엽서·책갈피·version 20·integrity는 그대로였어(정상 종료본은 파일 바이트까지 동일, WAL이 남은 본은 WAL이 본 파일에 합쳐져 바이트만 달라짐). 이어 브랜치로 다시 열면 전부 동일하고 저장·추가·삭제도 정상이야. DB 계층만의 확인이고 실제 앱 화면의 종료 모습·사진/꾸미기 파일·실기기 다운그레이드는 미검증(실기기 실험 금지).

---

# HANDOFF — 2026-11-24 142일차 사용량 한도에 따른 안전 중단

- 사용자 긴급 지시로 새 구현을 중단했어. 책갈피의 전체 기능 완료가 아니라 **데이터 기반 작업과 미완성 종이 렌더러를 보존하는 체크포인트**야. 아래 조사 당시의 승인 대기·Git 상태는 역사 기록이고 이 절이 현재 상태야.
- **PR #39 완료:** 접근성 보정 head `6fa514d882f02e9a70f8d96696b3061cbe62da7e`의 JVM·APK·instrumentation 컴파일 CI 성공, 사용자 TalkBack 선택 상태 구분 확인과 갱신된 병합 승인 후 Merge commit `0e30c10be8828e1ac31c52ba687fb211bf1f5b2b`으로 병합. main CI [45390163795](https://github.com/KimJinha-JJin/post-card-memory/actions/runs/45390163795)는 `verified-merge` 빌드 생략 성공이야. local main=origin/main도 같은 SHA야.
- 현재 브랜치 `feature/day142-memory-bookmark`, 체크포인트 commit 전 HEAD는 위 main SHA야. 기존 갤러리 브랜치와 책갈피 production을 분리했어. 최종 commit·push SHA는 대화상 마감 보고에서 확인해. 신규 책갈피 main 병합은 금지야.
- **작성한 데이터 기반:** `MemoryBookmark` Entity·형태/색/작은 흔적 27조합·DAO/Repository, 엽서 id PK+FK로 0..1 관계, CASCADE 정리, 미래 형식 덮어쓰기 transaction 방어. `PostcardDao.insertPostcard` 한 메서드만 REPLACE→Room Upsert로 바꿔 같은 id 재저장 시 자식 삭제를 피하게 했어. DAO 전체 재작성 없음.
- **Room:** schema 20 export JSON 생성, `MIGRATION_19_20`은 빈 책갈피 테이블만 만들고 기존 엽서 행을 쓰지 않아. DatabaseModule 등록 완료. 기존 두 migration 계측 테스트도 새 현재 버전까지 연결했지만 계측 실행은 하지 않았어.
- **로컬 자동검증 — 실행:** 관련 신규 JVM **10/10**, XML 2 suites, failures/errors/skipped 0. `assembleDebug`도 성공(50 tasks: 24 executed·26 up-to-date). `testDebugUnitTest --tests com.postcardmemory.data.MemoryBookmarkDesignTest --tests com.postcardmemory.data.MemoryBookmarkDatabaseTest assembleDebug --no-daemon --max-workers=1`로 실행했어. 전체 JVM 1317건은 재실행하지 않았어. schema 20 APK는 실제 사용자 기기에 설치하지 않았고 중단 마감에서도 설치하지 않아.
- 사용자 별도 승인으로 테스트 전용 Robolectric 4.18.1을 추가했어. API 34·기존 JDK 17, PC 임시 Application/DB만 사용하며 실제 앱 DB·실기기·emulator는 사용하지 않아. DB 사본 검사는 **닫힌 PC 테스트 DB snapshot**의 id/관계 복원 검사이지 Android 시스템 백업·사진/꾸미기 파일·기기 간 복원 검증이 아니야.
- **미완성 별도 보존:** `ui/components/MemoryBookmarkPaper.kt`는 A/B/C Path·섬유·작은 흔적·뒤쪽 그리기 초안이야. 컴파일은 통과했지만 아직 호출부가 없고 실제 화면/27조합 렌더링 검증은 안 했어. 데이터 체크포인트와 별도 WIP commit으로 구분해. 사용자 검토한 HTML 목업과 앞선 조사 기록도 보존해. 책갈피 PR·CI·실기기 QA는 미실행이며 사용량 중단 지시에 따라 이번 마감에서 확대하지 않아.
- **미착수:** 제작 overlay/ViewModel, 더보기 진입, 미리보기/삽입 모션, 갤러리 표시·필터, 편집/제거/취소 UI, 책갈피 실기기 QA. 앱에서 책갈피를 사용할 수 있는 상태가 아니야.
- 환경 이력: `classes.jar` 잠금으로 테스트 도달 전 실패가 반복됐어. 점유 주체를 읽기 전용 조회하고 이번 작업의 유휴 Gradle daemon을 정상 종료했어(강제 종료·산출물/캐시 삭제 없음). 단일 실행 daemon·작업자 1개(`--no-daemon --max-workers=1`)에서는 JAR 단계를 통과했어. 이후 신규 테스트의 API 사용 오류 두 가지를 최소 보정했어. 환경 실패와 실제 테스트 실패를 구분해.
- 보호 확인: 기존 보호 JSON **38/38**, 시작 미추적·`.claude` 자료 **48/48** 해시 유지. 보호 이미지·목업·보안 자료·설정·기존 worktree 정리 없음. 실사용 엽서 삭제 없음.
- **정확한 재개점:** 이 브랜치의 최신 Git·이 절·마감 보고와 테스트 결과부터 확인해. migration/Room 테스트 미통과면 그 첫 오류부터 해결하고 실기기 설치는 보류해. 통과했다면 기존 상세 화면 composition/VM을 유지하는 제작 overlay부터 구현해. `draftSaveStatus=Saved/Idle`을 확정 저장 완료로 오인하지 말고, 진입에서 기존 꾸미기 저장·초안 삭제·되돌리기를 자동 호출하지 않아. 일반 갤러리의 기존 NONE 상태 엽서 목록에 책갈피 Map/필터를 결합하고 선택·삭제 원천은 유지해. 전체 회귀·실기기·OS 백업 검증은 후속 범위야.

---

# HANDOFF — 2026-11-24 142일차 추가: PR #39 카드 선택 접근성 보정

- 추가 지시의 선행 검증에서 일반 월별 갤러리 카드가 선택 상태를 Compose semantics로 전달하지 않는 것을 확인했어. `GalleryScreen.kt`의 일반 카드 modifier에 `selected = isSelected`만 연결했어. 시각 디자인·선택/삭제 로직·Room schema 19·다른 화면은 무변경이야.
- **로컬 자동검증 — 실행:** `testDebugUnitTest assembleDebug` 성공, XML 1,307 tests / 140 suites, failures/errors/skipped 0. JVM source `@Test` 1,307·테스트 파일 139로 수·테스트 구현은 무변경이야. 이 JVM 검사가 실제 TalkBack 발화를 보호하는 것은 아니야.
- **실기기 감각 QA — 실행:** 같은 승인 기기 SM-S936N·package·서명·schema에서 기존 승인 범위의 `adb install -r`로 보정 APK 설치 성공. 최초 설치 시각 유지, 설치 APK와 로컬 APK hash 일치. 설치 직후 내부 files 139개 경로·hash가 설치 전과 전부 같았어.
- 시스템 animator scale 1.0→0에서 앱에 새로 진입해 단일 선택·도장 최종 표시를 확인하고 1.0으로 복구했어. 140ms 정밀 프레임 측정은 사용자 추가 지시로 병합 필수 조건에서 제외됐어.
- 사용자가 TalkBack을 직접 켜서 선택한 첫 엽서와 선택하지 않은 옆 엽서를 확인한 뒤 **“선택됨/선택 안 됨이 구분돼”**라고 보고했어. 실제 음성은 사용자 확인 근거야. 기존 비파괴 QA의 1~5장 선택·해제·삭제 확인창 취소 결과와 함께 사용해. 최종 삭제·편집 저장·실기기 instrumentation은 실행하지 않았어.
- UIAutomator XML의 selected=false만으로 보정 실패를 판정하지 않아. 일반 Compose 선택 항목은 stateDescription으로 전달될 수 있고 XML dump는 그 속성을 충분히 보여주지 않아. 초기 코드의 semantics 누락과 XML 관찰의 한계를 구분해. [AndroidX 접근성 구현](https://android.googlesource.com/platform/frameworks/support/+/b8f12c706099a47e1e44d9772d03e14a4ae37146/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt)도 대조했어.
- TEST-COVERAGE-MAP에 자동 보호 범위와 실제 기기 확인을 구분해 기록했어. `Protect main` 활성·Merge commit만 허용·strict 최신 base·필수 `build-and-test`를 원격 조회로 확인했어.
- 작성 시점 PR #39 원격 head는 `08b2f03a491b6d41380ea3d7d3db9118480553b2`, base는 `7609a67f59ce7e16f804b4ea90a61f41ba9dd5b1`. 보정 commit/push 후 새 head와 CI를 보고하고 AGENTS.md 9절에 따라 최종 병합 승인을 갱신해야 해. **기존 head에서 접근성 누락이 확인됐으므로 자동 병합하지 않아.**
- 사용자 추가 지시로 기억 책갈피 A/B/C·27조합·별도 Entity·schema 20 구현은 승인됐지만, PR #39 선행 마감 전 구현 기준 브랜치를 임의로 바꾸지 않아. 책갈피 production 미착수. 목업·앞선 조사 기록은 별도 보존하고 갤러리 보정 commit에 혼합하지 않아.

---

# HANDOFF — 2026-11-24 142일차: 갤러리 B안 비파괴 QA · 기억 책갈피 조사와 비교 목업

패코(Codex), 수동 표준 모드. 앱 production·test·리소스·Room·Gradle 설정은 수정하지 않았어. 사용자 승인으로 PR #39 Debug APK를 실사용 기기에 데이터 유지 설치하고 선택·삭제 확인창 취소를 검증했어. 기억 책갈피는 **조사·설계 제안·독립 HTML 목업까지만** 진행했고 디자인·정책·migration·구현 승인은 아직 없어. 아래 141일차 기록은 당시 상태로 보존해.

## 현재 상태와 검증

- 로컬 브랜치 `feature/day141-gallery-selection-ui`, HEAD `08b2f03a491b6d41380ea3d7d3db9118480553b2`, local main/origin/main `7609a67f59ce7e16f804b4ea90a61f41ba9dd5b1`. 시작 tracked/staged 변경 0개. 기존 보조 worktree(detached HEAD `7b4c2bf`, prunable 표시)를 정리하지 않았어. Git CLI 원격 조회는 네트워크 제한으로 실패했지만 GitHub connector로 PR #39 open·merged=false·head/base 일치를 확인했어.
- [PR #39](https://github.com/KimJinha-JJin/post-card-memory/pull/39) [Android CI run 184](https://github.com/KimJinha-JJin/post-card-memory/actions/runs/45178052479): `build-and-test` 성공. JVM·Debug APK·instrumentation 컴파일 단계 실제 실행 성공(빌드 생략 성공이 아님). CI 재실행이나 PR 수정은 하지 않았어.
- **로컬 자동검증 — 실행:** 현재 head에서 Gradle 9.9.1·Zulu 17 `assembleDebug` 성공(41 tasks, 1 executed·40 up-to-date). 첫 offline 시도는 foojay 플러그인 해석 실패로 코드 검사 전에 중단됐고, 정상 저장소 접근으로 재시도해 성공했어. JVM은 오늘 재실행하지 않았어(앱 코드 무변경, 위 CI와 141일차 로컬 1,307건 기록을 구분해 사용).
- **실사용 기기 instrumentation — 미실행:** 이번 QA는 ADB 관찰·안전한 입력만 사용했어. `connected*`, instrumentation, emulator 사용 없음.
- **설치 — 사용자 명시 승인 후 실행:** SM-S936N, 실측 1080×2340·450dpi. 기존 설치본과 새 APK 서명 일치 확인 후 `adb install -r` 성공. 최초 설치 시각 유지, 설치된 APK 해시와 로컬 APK 해시 일치. 설치 직후 `files` 내부 139개 경로·SHA-256가 설치 전과 모두 같았어.
- **실기기 감각 QA — 실행·일부 미검증:** 아래 표. 중간에 사용자의 기기 조작으로 다른 화면에 진입해 입력을 멈췄고, 사용자가 직접 조작했다고 확인했어. 이후 폰을 잠시 비워두겠다는 응답 후 재개했어. 최종 삭제·편집 저장·공유·내보내기는 누르지 않았어.
- QA 후 `files`는 139개 유지. 실행 프로필 표식 `files/profileInstalled` 1개만 해시 변경, 나머지 138개는 같았어. 현재 앱 프로세스 AndroidRuntime 로그의 FATAL 표시 0개. **DB·SharedPreferences 전체 동일성이나 과거 모든 로그의 무오류를 보장하는 근거는 아니야.** 화면·로그 원본은 공개 저장소에 추가하지 않았어.
- 기존 보호 JSON 기준선은 **38개 모두 일치**, 이번 시작 시 따로 확보한 기존 미추적·`.claude` 파일 **48개 모두 해시 유지**. 지시서의 과거 SAFE 43개와 집계 기준이 달라 43개 검증으로 바꿔 쓰지 않았어. 기존 미추적 목업·보안 자료·이미지·설정·로컬 산출물 이동/정리 없음. 민감정보 훅 활성 확인, 설정 변경 없음.
- `TEST-COVERAGE-MAP` 변경 없음: production/test 및 보호 범위 변경 없음. 신규 Markdown 파일 없음. 신규 파일은 `docs/ai/mockups/memory-bookmark-day142.html` 하나야. Git stage·commit·push·main 병합 없음. 이 HANDOFF와 목업은 미커밋 상태로 검토 대기야.

| 갤러리 QA 항목 | 결과와 한계 |
|---|---|
| 길게 누르기·단일 선택 | 실행: 선택 바·종이 덧댐·체크 도장 표시 |
| 복수 선택·밝은/어두운 사진 | 실행: 1~5장 선택 수와 도장 수 일치, 이웃 카드 겹침·잘림 미발견 |
| 140ms 모션·눌림 충돌 | 자동 프레임·시간 측정 미실행. 사용자는 깜빡임·충돌 문제 없다고 확인 |
| 개별 해제·전체 선택 종료 | 실행: 5→4장, 선택 종료 후 기존 사진·날짜 표시로 복귀 |
| 삭제 확인창 | 실행: 제목·경고 문구·적갈색 버튼·썸네일 4개·5장 선택의 `+1`, 잘림 미발견 |
| 삭제 취소 | 실행: 확인창 닫힌 뒤 5장 선택 그대로 유지 |
| 임시 종이와 엽서 자체 구분 | 사용자 확인: “선택 표시로 잘 구분되고 문제 없었어” |
| 접근성·애니메이션 비활성화 | 미실행: 현재 animator scale 1.0·accessibility_enabled 0, 설정 변경 없음. 화면 낭독·배율 0·스크롤 재진입 모션은 미검증 |

일반 사용 조건에서 수정할 결함을 발견하지 못했고 사용자 시각 확인도 받았어. **접근성·움직임 끄기 미검증을 남긴 조건부 병합 후보**이며 최종 병합 승인은 아직 없어. 전체 QA 통과로 표현하지 않아.

## 기억 책갈피 — 실제 구조와 반례

- `app/src/main/java/com/postcardmemory/data/Postcard.kt:7`: `postcards`, 자동 생성 `Long id` PK. 책갈피 속성·범용 metadata 컬럼은 없어. `PostcardDatabase.kt:8`은 Postcard 단일 Entity·schema 19. `DatabaseModule.kt:26`에서 1→19 migration을 전부 등록하며 destructive fallback 없음.
- `PostcardDao.kt` → `PostcardRepository.kt` → `GalleryViewModel.kt:49`의 Flow로 갤러리 공급. 일반 목록은 `futureMailState='NONE'`; 책갈피 필터도 이 조건 안에서 적용해야 봉인된 편지가 노출되지 않아. 검색·정렬·월별 개수는 같은 필터 결과를 보도록 설계해야 해.
- `CameraViewModel.kt:264`는 이미지 임시 생성 → `insertPostcard` 성공 → 파일 소유권 확정 흐름이야. 실패·취소는 임시 파일만 정리해. 현재 production insert 호출은 카메라 신규 생성(id=0)이야. `PostcardDao.kt:83`은 `OnConflictStrategy.REPLACE`이므로 **향후 같은 id 재삽입과 CASCADE 외래키가 만나는 반례**를 보호해야 해. 지금 책갈피가 지워진다는 현재 결함은 아니야. SQLite는 PK 충돌 REPLACE에서 기존 행을 먼저 삭제한다고 명시해([공식 REPLACE 문서](https://www.sqlite.org/lang_conflict.html), [외래키 동작](https://www.sqlite.org/foreignkeys.html)).
- 엽서 컬럼 수정은 개별 DAO UPDATE와 `DetailViewModel.kt:467`의 `styleWriteMutex`로 직렬화돼. 꾸미기 6종은 `PostcardOwnedFileLayout.kt`의 엽서 id별 확정 파일, `PostcardEditDraft.kt`·`PostcardDraftStorage.kt`의 별도 초안, `DecorationEditHistory.kt`와 종류별 이력으로 관리돼.
- `DetailViewModel.kt:945` 완료 저장은 `ConfirmedEditTransaction`으로 6종+이미지를 준비 후 교체해. DB transaction과 같은 것이 아니며 rename 중 부분 실패·프로세스 종료 한계도 명시돼 있어. 책갈피를 여기에 단순 일곱 번째 파일로 붙이면 저장·초안·확정 기준선·Undo·삭제·진단 전체를 확장해야 해.
- `PostcardDeletionManager.kt`는 DB 삭제 성공 후에만 소유 파일 정리, 취소는 재전파해. B안 Entity는 DB 안에서 엽서와 관계 정리 후 기존 파일 정리 순서를 유지할 수 있어. 삭제 중 책갈피 재저장 경합도 별도 검증이 필요해.
- `AndroidManifest.xml:9`의 `allowBackup=true`를 확인했어. 앱 자체 데이터 백업·가져오기/복원 workflow는 production 검색에서 찾지 못했어. 꾸미기 초안 복원은 시스템 백업과 다르고, 이미지 내보내기도 데이터 백업이 아니야. OS 백업·기기 간 복원 성공은 미검증이며 allowBackup만으로 보장하지 않아.
- 앞면 공유·이미지 저장은 `DetailViewModel.kt:4649/4698` → `PostcardImageExporter.kt`, 뒷면은 `exportBackPostcard`와 `PostcardBackExportCapture.kt`의 캡처 경로야. 책갈피 포함을 고르면 두 출력과 화면의 여백·겹침·최종 범위를 함께 설계해야 해.

## 저장 방식 비교 — 전부 승인 전 제안

| 방식 | 무결성·수정·삭제 | 난이도·확장 | 백업/복원·schema |
|---|---|---|---|
| A Postcard 컬럼 추가 | 엽서와 같은 행에서 관리·삭제. 전용 UPDATE로 다른 속성 보존 가능 | 초기 단순, 선택 속성과 null 조합·모델 확장 부담 | 같은 DB에 포함, migration 필요 → 20 |
| B 별도 Bookmark Entity **권장** | `postcardId` PK+FK로 0..1 관계, 설정 한 번 저장·조회·필터, 엽서 삭제와 관계 정리를 DB에서 처리 | 중간, DAO/Flow/경합·REPLACE 반례 테스트 필요. 확장과 책임 분리 유리 | 같은 DB에 포함, 빈 새 테이블 19→20 추가 후보. 실제 복원 검증은 별도 |
| C 엽서별 확정 파일 | 기존 파일 저장 선례 있음. DB와 분리돼 고아·삭제 경합·원자성 직접 관리 | schema는 유지하나 필터 인덱스·Flow 갱신·초안·진단·소유권 처리 비용 큼 | schema 19 유지 가능. 파일·DB를 함께 복원하는 일관성 위험 |

권장 데이터 초안은 `postcardId`, 안정된 `shapeId/colorId/decorationId`, 설정 format version이야. 자산 파일 없는 작은 종이 설정으로 시작하고, schema 19 유지를 위해 기존 스티커·도장 필드에 숨기거나 SharedPreferences에 흩어 저장하는 우회는 권장하지 않아. schema 20은 **필요성 제안일 뿐 변경·실행하지 않았어**. 승인 후 migration 계획은 기존 엽서 무변경·기존 엽서 책갈피 없음·새 빈 테이블·외래키/인덱스·19→20 스키마 검증·삭제/재저장/실패/재시작/복원 테스트를 포함해야 해. 실제 migration instrumentation은 격리 환경 부재와 실기기 보호 경계를 따라 실행 가능성을 따로 보고해야 해.

## 진입 위치와 UI/UX 문법 사전 보고

| 후보 | 발견·흐름·복잡도 | 제안 |
|---|---|---|
| 엽서 상세 | 한 장에 집중, 의미 명확 | 현재 독립 읽기 상세와 편집은 별도 화면이 아님. `MainActivity.kt:213`의 `detail/{postcardId}`가 `DetailScreen` 편집으로 이어져 |
| 편집 화면 더보기 | `DetailScreen.kt:6420` 기존 메뉴 활용 가능. 도구 7종에 새 탭을 늘리지 않음 | **주 진입 권장:** 더보기 → 기억 책갈피 만들기/다시 꾸미기. 미저장 꾸미기/진행 중 저장이 있으면 선행 완료 저장 또는 취소를 사용자에게 선택하게 하고 몰래 저장하지 않는 안 |
| 갤러리 엽서별 메뉴 | 찾아가기 빠르지만 현행 메뉴 없는 카드에 새 조작 필요 | 초기 보류. 길게 누르기의 선택·삭제 의미를 바꾸지 않음 |
| 검색·필터 | 완성품 다시 찾기에 적합, 제작 대상은 아직 불명확 | 제작 진입 없이 ‘책갈피 있는 엽서’ 필터만 후보. 다중 선택 제작·복제 없음 |

- 사용자 행동: 한 엽서에 독립 종이 소품 제작·끼우기. UI 역할: Preset Selection(종이 끝·무늬), Color Selection, Property Row, Completion/Save, 제거는 Destructive Action. 자유 이동하는 Canvas Object·기존 Sticker Add Action과 다른 역할이야.
- 토큰/속성 행/기본 견본은 기존 Warm Paper 문법 보존 후보야. 전용 제작대와 끼우기 완료 흐름은 기존 variant가 아니라 **신규 문법 제안**으로 판단했어. 기존 꾸미기 고급 편집기 전체를 승인된 패밀리로 간주해 복사하지 않았어. 최신 전체 실기기 UX 감사·책갈피 선례는 없어 미확인 부분을 최종 승인으로 쓰지 않아.
- 새 문법 이유: 스티커처럼 표면에 붙이고 이동하는 문법으로는 ‘뒤에 끼운 별도 종이’의 제작 과정과 겹침이 흐려져. 한 장을 제작하는 작업대와 전후 깊이 미리보기로 해결하는 제안이야. 다른 종이 소품으로의 전파·공용화는 현재 범위에 없어.
- 진입·완료·제거·미저장 꾸미기 처리·연민트 콘텐츠 색 등은 목업 제안이며 앱 구현 STOP. 사용자 지시가 승인한 독립 HTML 비교에만 적용했어. 외부 앱 레퍼런스 복제 없음.

## 디자인 자산과 신규 범위

- 종이 질감: 추적 자산 `app/src/main/res/drawable-nodpi/gallery_paper_tile.png`, `GalleryScreen.kt:1685`·`DetailScreen.kt:2542` 재사용 후보. **미추적 보호 자료 `postcard_paper_fiber_tile.png`는 사용·변경하지 않았어.** HTML은 외부 파일 없이 CSS 결을 근사해.
- 색: `ui/theme/Color.kt`의 PaperCanvas/Surface/Field/Tray/Divider, InkPrimary/Secondary, SealInkSepia. 연민트는 기존 공식 토큰으로 주장하지 않는 새 콘텐츠 색 후보야.
- 종이 윤곽/C2: `utils/PostcardRenderSpec.kt:776`의 공용 `createPinkingPath`, `ui/components/StampCard.kt` 표시. 책갈피는 작은 별도 윤곽을 새로 설계하되 엽서의 C2 경계는 바꾸지 않아. HTML 천공은 근사야.
- 잉크: `ui/components/SealShapes.kt`, `utils/SealInkWearRenderer.kt`; 테이프: `ui/components/MaskingTapeShapes.kt`의 섬유·끝선. 종이 질감·잉크 표현의 계산 선례만 참고하고 사용자의 도장/테이프 객체나 저장 생애주기를 책갈피로 재사용하지 않아.
- 그림자: `PostcardRenderSpec.kt` 접촉 그림자와 `ui/detail/DecorationLiftFeel.kt` 참고 후보. 새 책갈피/엽서의 앞뒤 레이어와 가림·작은 그리드 노출 범위는 신규 구현 필요.
- 움직임 줄이기: `ui/gallery/StickerStashPouch.kt:123`의 `rememberSystemReducedMotion`, `GalleryCardPressFeel.kt`, `StampCard.kt:844` 선례. 현재 helper는 remember 시 설정을 읽어, 화면 살아 있는 중 설정 변경 반영은 별도 확인 필요야. 이번에 수정하지 않았어.

## HTML 목업과 기능 정책 제안

파일: [memory-bookmark-day142.html](mockups/memory-bookmark-day142.html). 파일을 Chrome/Edge로 열면 돼. 외부 라이브러리·네트워크·실제 사진·앱 데이터·localStorage 없이 탭 메모리만 사용해. A 클래식 V자 / B 손오림 / C 미니 태그를 동일 가상 풍경·색·장식으로 비교해. 일반 엽서 → 제작 → 종이 끝/색/장식 → 미리보기 → 뒤에 끼우기 → 갤러리 → 책갈피 필터를 조작할 수 있어. 엽서마다 별도 설정이고 복제 없음. 미리보기·취소는 저장하지 않고, 끼우기 모션 중 재진입은 막아. 시스템 모션 감소와 목업 체크박스는 즉시 완료 상태로 처리해.

- 추천은 **B의 작은 비대칭**: 기존 손오림 종이와 조화하며 선택 배경의 사방 덧댐/체크와 구분돼. A는 의미가 명확하지만 긴 노출, C는 작은 카드에 안정적이나 개성이 약해질 가능성이 있어. 이 추천은 목업 실화면 승인 전 제안이야.
- 초기 범위 후보: 엽서당 1개·A/B/C 종이 끝·아이보리/브라운/연민트·장식 없음/작은 소인/세 줄. 길이 조절·사용자 글귀·위치 이동·복수 책갈피·완성품 복제는 후속 후보. 길이는 형태에 고정해.
- 편집: 기존 것을 다시 꾸미고 명시 완료 때만 교체. 취소는 이전 것 유지. 제거: 책갈피만 제거하고 엽서는 유지, 확인 절차 후보. 기존 6종 꾸미기 Undo/Redo와 분리하고 제작 세션의 취소/원래대로를 우선 제안해. 책갈피가 기존 꾸미기 완료 저장을 대신하지 않아.
- 필터: 일반 갤러리 내 책갈피 있는 엽서만, 검색/정렬과 함께 적용. 저장 성공 후 표시·재시작 유지. 엽서 삭제 시 관계 정리, 미래 편지 발송 시 데이터 유지하되 기존 비노출 정책 준수.
- 공유/이미지 내보내기: 초기 제외 권장(앱 안 보관 소품), 제외 안내 필요. 포함을 원하면 앞뒤 공통 renderer·출력 크기·클리핑까지 별도 범위로 승인받아. OS 백업·복원 호환 목표는 유지하되 실제 검증되지 않은 복원 보장은 하지 않아.
- 접근성: ‘책갈피 있음’ 설명·옵션 이름/선택 상태·색 외 표시·충분한 터치 범위·모션 감소 시 최종 모습. 갤러리 카드 크기·3열·간격 유지, 작은 노출이 이웃 행/선택 도장과 충돌하지 않는지 Compose·실기기 검증 필요.
- **목업 검증:** Node VM의 DOM 대역으로 JS 구문·A/B/C 갱신·미리보기 비저장·엽서별 독립 적용·취소 보존·필터 토글·모션 재진입 방지·외부 요청 없음 통과. 첫 대역 검사는 PowerShell pipe 인코딩으로 한글 기대값이 깨져 실패했고 ASCII escape 기대값으로 바로잡았어. 앱/목업 결함으로 처리하지 않아.
- **목업 실제 브라우저 표시·포인터 조작·모바일 반응형 시각 검증은 미실행:** browser access host unavailable, native Chrome 접근도 coordinate geometry unavailable / foreground process id 오류로 중단. 코드 대역 검증을 실제 브라우저 QA로 표현하지 않아. 사용자 화면 검토 또는 도구 복구 후 시각 검증이 필요해.

## 승인 대기와 재개 조건

1. PR #39: 위 head/base·CI와 일반 조건 QA 결과, 접근성/모션 끄기 미검증을 검토하고 병합 여부를 별도로 결정해야 해. 사용자 시각 OK는 병합 승인이 아니야.
2. 책갈피: A/B/C 디자인, 초기 옵션·진입/미저장 처리·독립 완료·수정/제거·필터·공유 제외/포함 정책을 승인받아야 해. 목업 표시 확인도 남아 있어.
3. 저장: B Entity와 schema 20/migration은 별도 명시 승인 필요. 승인 전 앱 코드·schema·DB 수정 없음. 새 dependency/빌드 변경도 하지 않아.
4. PR #39 미병합 상태이므로 책갈피 Compose를 현재 브랜치에 섞지 않아. 병합 승인 후 최신 main·별도 `feature/day142-memory-bookmark`에서 시작하거나, 미병합이면 구현 기준 브랜치를 먼저 확인해야 해.

---

# HANDOFF — 2026-11-23 141일차 후속: 갤러리 선택·삭제 B안(종이 도장형) 구현

피코(Claude Code), 수동 표준 모드. 반말 통일 PR #38은 main에 Merge commit `7609a67`로 병합됐고(main CI verified-merge 성공, 작업 브랜치 로컬·원격 정리 완료), 그 위에서 기존 빈 브랜치 `feature/day141-gallery-selection-ui`를 최신 main으로 맞춰 사용자가 확정한 B안을 Compose로 구현했어. 브랜치 commit/push·PR·CI까지 승인, **main 병합은 사용자 최종 승인 전 금지**.

## 현재 상태와 변경 범위

- **선택 카드(`ui/components/StampCard.kt` `StampCardContent`):** 선택되면 카드 뒤에 `PaperTray` 종이를 사방 4dp 덧대 그려(2dp 모서리, `drawBehind`라 그리드 배치·카드 크기 불변). 그리드 간격 가로 8·세로 10dp라 이웃 선택 카드와 겹치지 않아(가로는 맞닿음, 세로 2dp 틈). 사진은 가위 윤곽 안쪽만 `PaperTray` 22%로 바래고, 날짜는 `InkPrimary`·SemiBold로 강조. 기존 핑크(`BrutalCoral`) 오버레이와 14dp 점은 없앴어. 오른쪽 위 24dp 잉크 체크 도장(`InkSecondary` 원, 종이색 테두리·안쪽 얇은 링, 흰 체크, 1dp 그림자)은 선택 순간에만 140ms 0.86배·투명→1로 찍히고 회전·번짐·튕김 없음. 이미 선택된 채 스크롤로 들어온 카드와 시스템 "애니메이션 끄기"는 바로 최종 모습. 130일차 눌림·기울기, 가위 윤곽, 흰 테두리, 편지 배지는 무변경. 연못·경주 화면은 `isSelected=false`만 넘겨 영향 없음.
- **상단 선택 바(`GalleryScreen.kt`):** `N장 선택` 글자색 핑크→`InkPrimary`. 휴지통 아이콘만 있던 버튼을 아이콘+`삭제` 글자의 적갈색(`GalleryDangerRed`) 테두리·종이색 바탕·2dp 모서리 상자로(보이는 높이 36dp, 터치 48dp, 알약형 아님).
- **삭제 확인창:** 4dp 모서리 종이 창, 제목 `엽서 N장을 삭제할까?`, 지울 엽서 썸네일 44dp(갤러리 순서 앞 4장, 초과는 `+N`), 안내 `삭제하면 되돌릴 수 없어. 엽서에 담긴 사진과 꾸미기도 함께 지워져.`(실제 삭제 범위 = 앱 안 사진·꾸미기 상태·초안 파일과 DB 기록, 되돌리기 없음과 일치), 테두리 `취소` + 적갈색 채움 `N장 삭제`(2dp 모서리). 썸네일은 Coil `AsyncImage`가 44dp 표시 크기로 decode하고 4장까지만 만들어 대량 선택에도 원본 decode가 없어. 사진을 못 읽으면 종이색 빈 자리로 남아.
- 선택 상태 구조, ViewModel 선택·삭제 로직, Repository, Room schema 19, 저장 무결성, 미래 우체통, C2 천공, 140일차 펀칭, 갤러리 전체 배치는 무변경. 삭제 확인을 생략하는 경로 없음.
- 변경: `StampCard.kt`, `GalleryScreen.kt`, 신규 `GalleryDeleteDialogThumbnailsTest.kt`, `TEST-COVERAGE-MAP.md`, 이 HANDOFF. 목업 HTML은 미추적 그대로.

## 검증

- **로컬 자동검증 — 실행:** 전체 `testDebugUnitTest` 1,307건·140 suite 실패 0(신규 4건), `assembleDebug` 성공(Gradle 9.9.1, Zulu 17). 선택 개수 문구 구조 검사 통과.
- **TEST-COVERAGE-MAP — 갱신 완료:** 141일차 절 추가.
- **실사용 기기 instrumentation — 불필요**(변경 없음).
- **실기기 감각 QA — 필요·대기(기기 미연결, `adb devices` 0대):** 길게 누르기 직후, 1장·여러 장 선택, 해제, 도장 찍힘, 종이 덧댐(밝은·어두운 사진, 이웃 카드), 삭제 버튼, 확인창 썸네일·긴 문구, 취소 후 복귀. PR #38에서 미검증으로 남은 삭제 문구 표시도 함께. 실제 삭제 QA는 사용자 사전 승인 없이 하지 않아.

## 남은 위험·재개 조건

1. 실기기 QA 전에는 병합하지 않는 게 안전해(시각 결과는 자동 검사로 보호 안 됨).
2. 화면 낭독기: 카드의 선택 상태 전달은 이번에 바꾸지 않았어(별도 범위). 상단 삭제 버튼은 `Role.Button`+보이는 `삭제` 글자로 읽혀.
3. 위 141일차 반말 절의 용어·알약형 후보는 그대로 대기.

---

# HANDOFF — 2026-11-23 141일차: 안내 문구 반말 통일

피코(Claude Code), 수동 표준 모드. 사용자 작업지시서로 앱 공식 문체를 반말(해체)로 확정하고, 화면 표시 문장 중 해요체·합니다체만 반말로 바꿨어. 작업 브랜치 commit/push·PR·필수 CI까지 승인됐고 **main 병합은 사용자 최종 승인 전 금지**야. 140일차 PR #36·#37은 이미 병합돼 있어(시작 main = `bde0420`).

## 현재 상태와 변경 범위

- 브랜치 `feature/day141-voice-banmal`(origin/main `bde0420`에서 분기, 원본 폴더). 갤러리 B안(종이 도장형) 구현은 별도 후속 작업이고, 그 비교 목업 `docs/ai/mockups/gallery-selection-delete-day141.html`은 미추적으로 보존(이번 커밋 제외). B안 탐색용 빈 브랜치 `feature/day141-gallery-selection-ui`(커밋 0, 로컬 전용)가 남아 있어.
- 조사 기준(화면 표시 458곳 중 해요체 34 + 합니다체 41 = 75곳)을 다시 확인하고 74곳을 반말로 바꿨어. 문자열만 바꿨고 기능·저장·삭제·Undo·Room schema 19·UI 색/크기/배치·알약형 모양은 무변경.
  - 실패창 제목은 반말인데 본문(`exception.message`)이 합니다체로 뜨던 문제: 카메라·사진 저장·스티커/테이프 사진·이미지 내보내기 예외 문구 41곳을 `~하지 못했어.`/`~할 수 없어.`로 맞췄어.
  - 삭제: 엽서 편집 `이 엽서를 삭제할까?` / `삭제하면 되돌릴 수 없어.`, 갤러리 `엽서 N장을 삭제할까?` / `삭제하면 되돌릴 수 없어.`. 갤러리 선택·삭제 흐름은 승인된 B안 방향대로 단위를 `장`으로 바꿨어(`N장 선택`, `엽서 N장을 삭제했어.`, 부분 실패 `N장 삭제했고, N장은 삭제하지 못했어.`). 갤러리 밖 단위는 무변경.
  - 미래 우체통 보내기 창·봉인 화면, 스티커 통 빼기 창, 카메라 안내, 빈 화면, 공유 시트, 쫑쫑컵 안내도 반말로.
  - 감성 문구는 말끝만 바꾸고 유지: `수정구가 추억의 좌표를 맞추고 있어`, `M월 d일의 이야기야!`, `오늘까지 N번 만났어!`(물결표만 뺌), `N에 다시 만나자.`
- **의도적으로 남긴 존댓말 1곳:** 인트로 비밀 문구 `비개발자가 만들었어요`(`AppIntroScreen.kt`, 테스트로 고정된 제작자 서명). 사용자 판단 대기.
- 접근성 전용 문구, 명사형 버튼·라벨, 기존 반말 문구는 손대지 않았어.

## 검증

- **로컬 자동검증 — 실행:** 전체 `testDebugUnitTest` 1,303건·139 suite 실패 0, `assembleDebug` 성공(Gradle 9.9.1, Zulu 17). 테스트 기대값만 갱신(6파일: 삭제 결과 문구, 선택 개수 문구 구조 검사, 내보내기 실패 문구, 랜덤 엽서·방문 횟수 라벨, 정사각형 저장 실패 메시지).
- 문구 재스캔: 화면 표시 문장 중 존댓말은 위 1곳만 남음.
- **TEST-COVERAGE-MAP — 변경 없음:** 테스트 수·보호 범위는 같고 기대 문구만 바뀜.
- **실사용 기기 instrumentation — 불필요**(변경 없음).
- **실기기 감각 QA — 미실행(기기 미연결, `adb devices` 0대):** 바뀐 문구는 대부분 같거나 짧아. 길어진 것은 `이 엽서를 삭제할까?`(창 제목), `엽서 N장을 삭제할까?`, `엽서 N장을 삭제했어.`(토스트) 정도인데, 잘림 여부는 코드상 줄바꿈 가능한 자리라 문제없을 것으로 보지만 기기에서 확인하지 않았어.

## 남은 위험·재개 조건

1. 사용자 판단 대기 용어: 패턴/무늬, 편집/수정, 개/장(갤러리 밖), 글귀/텍스트/문구, 색/색상, 잠시 뒤/잠시 후, 엽서/포스트카드, 엽서/편지, 꾸미기/편집 내용. 이번에 바꾸지 않았어.
2. `수정구가 추억의 좌표를 맞추고 있어`(저장 중 문구)는 의미가 흐려 대안 검토 후보.
3. 알약형 UI 5곳(공유하기 버튼, 갤러리 빈 화면, 검색 결과 없음, 카메라 촬영 안내, 쫑쫑컵 안내)은 후속 디자인 후보로 유지, 이번 변경 0건.
4. 갤러리 B안 Compose 구현은 별도 승인 단계.

---

# HANDOFF — 2026-11-22 140일차 마지막: 펀칭 책상 공간감 · 펀칭 뒤 편집 화면 진입

피코(Claude Code), 수동 표준 모드. PR #36(아래 절)은 main에 Merge commit `3d56253`으로 병합됐고 main CI는 verified-merge로 성공, 작업 브랜치는 정리됐어. 사용자 추가 지시로 최신 main에서 `feature/day140-punch-desk-and-editor`를 만들어 두 가지만 바꿨어. **main 병합은 사용자 승인 전 보류.**

- **공간감(`StampPunchScreen.kt`):** 배경을 기존 종이색 중심에서 가장자리로 아주 조금 어두워지는 책상 면(radial gradient)으로 바꿨어. 매트는 평면 안쪽으로 들여 그려(둥근 모서리 1.6) 책상 위에 놓인 판으로 표현했어. 책상 높이에 앞쪽 두께 면(MAT_THICK 2.2)과 넓은·짧은 접지 그림자 두 겹을 두고, 종이가 매트에 얹힌 짧은 접촉 그림자를 더했어. 장면 전체에는 먼 쪽 가장자리가 한쪽당 3.5% 좁아지는 원근(`setPolyToPoly`)을 걸었어. 펀칭기 그림자는 가까울수록 짙고 또렷해지게 조정했고, 날이 닿을 때 C2 윤곽을 따라 눌린 그늘이 생기게 했어. 예전 무대 색 띠와 둥근 잘림은 없앴어(펀칭기가 무대 밖 책상 위로 빠져나감). C2 천공선·2.8초 시간표·장면 구성·사진 표시는 무변경.
- **펀칭 뒤 편집 화면:** 기존 `insertPostcard`가 돌려주는 Room 자동 id를 `CaptureState.Success.postcardId`로 넘겨, 펀칭이 끝나면 기존 `detail/{id}` 경로로 바로 들어가. `popUpTo("camera") { inclusive = true }`라 갤러리를 거치는 이중 전환이 없고, 편집에서 뒤로 가면 갤러리야. id가 0 이하(비정상)면 예전처럼 갤러리 복귀(`editablePostcardIdOrNull`). 복귀 게이트(저장당 1회), 저장 중·실패·취소 분기, 움직임 줄이기 600ms는 그대로라 실패·취소 때 편집 화면으로 가지 않아. 엽서는 저장 때 한 번만 insert되고 편집 화면은 그 행을 열기만 해. DetailScreen·Undo/Redo·저장 로직·Room schema 19 무변경.
- 변경: `StampPunchScreen.kt`, `StampPunchTimeline.kt`(id 판정 함수), `CameraScreen.kt`(`onOpenSavedPostcard`), `CameraViewModel.kt`(insert id 보존), `MainActivity.kt`(카메라 → 편집 이동), `StampPunchTimelineTest.kt`(+1건), 이 HANDOFF, `TEST-COVERAGE-MAP.md`.
- **로컬 자동검증 — 실행:** `StampPunchTimelineTest` 8/8, 전체 `testDebugUnitTest` 1,303건·139 suite 실패 0, `assembleDebug` 성공.
- **실기기 QA — 실행(승인된 실사용 기기, 사용자 승인 `adb install -r`, firstInstallTime 유지, 설치 전후 사진 15개 동일):** 저장 1회 → 펀칭(책상·매트 두께·접지 그림자·원근 프레임 확인) → 갤러리 없이 방금 저장한 엽서 편집 화면(같은 사진·날짜) → 뒤로가기로 갤러리, 사진 파일 +1(중복 없음), 임시 0, FATAL 없음. 사용자 육안 "둘 다 좋아".
- 미검증: 움직임 줄이기 상태에서 편집 화면 진입은 기기에서 다시 확인하지 않았어(코드상 같은 `onFinished` 경로). 회전·저장 실패 실기기 재현은 이전과 같이 미검증.

---

# HANDOFF — 2026-11-22 140일차: C안 우표 펀칭 모션 앱 구현

피코(Claude Code), 수동 표준 모드. 사용자 140일차 작업지시서로 138일차 채택 C안을 앱에 구현하고, 작업 브랜치 commit/push·PR·필수 CI 확인까지 승인됐어. **main 병합은 사용자 최종 승인 전 금지**야. 아래 139일차 이하 원문은 당시 상태로 보존했어.

## 현재 상태와 변경 범위

- 시작 local main = origin/main = `3d8fc55`(PR #35 병합). 작업 브랜치 `feature/day140-stamp-punch-motion`(원본 폴더, 새 worktree 없음). 시작 tracked/staged 변경 0개, SAFE 38개 일치, 기존 미추적 47개(무시되는 `.claude/settings.local.json` 포함) 해시 기준선 기록.
- 저장 성공(`CaptureState.Success` = 자르기 + DB insert 완료) 화면을 수정구 대기(1,100ms)에서 **C안 펀칭 → 갤러리 복귀**로 바꿨어. 첫 구현은 목업 그대로 1,900ms였고, 실기기 확인 뒤 사용자 후속 요청으로 **2,800ms**로 늘렸어(아래 속도 조정). 저장 중(`Saving`) 화면, 실패(`Error`) 문구, 취소(coroutine 취소 → 임시 파일 정리) 경로는 그대로야. 펀칭은 성공 상태에서만 그려져.
- 새 파일: `ui/camera/StampPunchTimeline.kt`(목업 `CANDS` C안·`stateAt` 직역 순수 함수 + `SuccessExitGate`), `ui/camera/StampPunchScreen.kt`(Canvas 한 장). 수정: `CameraScreen.kt`(Success 분기 교체, 1,100ms `LaunchedEffect` 제거), `CameraViewModel.kt`(`claimSuccessExit()` 추가). 테스트: `test/.../ui/camera/StampPunchTimelineTest.kt` 7건. 문서: 이 HANDOFF, `TEST-COVERAGE-MAP.md`.
- 장면: 커팅 매트 위 고정된 종이에 방금 저장한 사진이 C2 우표로 인쇄 → 펀칭기(C2 날 + C2 창 판) 하강·누름 → 1,050ms에 종이가 `시트 − C2`(Path Difference) 구멍 + 같은 C2 조각으로 바뀜 → 펀칭기 상승 → 조각만 높이 5로 들려 그림자 생김 → 정착 후 종료 정지 → 복귀. 윤곽은 `PostcardRenderSpec.createPinkingPath` 재사용(C2 재구현 없음).
- 목업과의 차이: CSS 3D(rotateX 40°·perspective)를 정사영 2D 근사(평면 y×cos40°, 높이 z는 위로 sin40°, 높이에 비례한 작은 확대)로 그렸어. SVG 흐림 대신 겹친 반투명 층 그림자(minSdk 26 하드웨어 캔버스 호환), 종이 노이즈 결 생략, 우표 그림은 실제 저장 사진(우표 크기로만 decode, 사진 로딩은 최대 400ms 대기 후 진행).
- 중복·생명주기: 복귀는 `SuccessExitGate`로 같은 저장에 한 번만 `popBackStack`. 진행 시각은 `rememberSaveable`이라 재생성 시 이어서 재생. 뒤로가기는 기존 BackHandler(Saving/Success 차단) 유지. 저장 연타는 기존 `CropReady` 검사로 무시. 복귀 직전 `resetState()`는 더 부르지 않아(카메라 미리보기가 나가는 화면에 잠깐 보이지 않게). VM은 pop 시 정리돼.
- 움직임 줄이기(`rememberSystemReducedMotion`, 애니메이터 배율 0): 보간 없이 잘려 들린 끝 장면만 600ms 보여 주고 복귀. 배율 0.5 등은 Compose 기본 배율을 따라.
- Room schema 19·Entity·DAO·저장 순서·파일 소유권 로직 무변경.

## 검증

- **속도 조정(사용자 후속 지시, 같은 브랜치·PR #36):** 장면·높이·그림자·디자인은 그대로 두고 `StampPunchTimeline`의 시간과 곡선만 바꿨어. 하강 120–950(종이에 가까울수록 감속하는 inOutCubic, 기존 inQuad), 누름 950–1,180·절단 1,050(누름 100ms 뒤 즉시 전환), 상승 1,180–1,800(천천히 떨어져 빠져나가는 inOutCubic, 기존 outQuart), 분리 확인 1,800–1,900, 들림 1,900–2,350(높이 5, outCubic 유지), 정착·종료 2,350–2,800 후 복귀. 움직임 줄이기 600ms·복귀 게이트·저장 분기 무변경.
- **속도 조정 실기기 재검증 — 실행:** 사용자 승인 `adb install -r`(firstInstallTime 유지, 설치 전후 사진 14개 동일). 저장 1회에서 PC 연속 스크린샷으로 하강(펀칭기 위), 접촉·누름(펀칭기가 종이 위), 절단 후 분리(절단선·펀칭기 빠짐), 들림(조각 그림자·구멍 속 매트)을 약 2.8초 동안 확인. 사용자 육안 "이 속도 좋아". 앱 FATAL 없음, 임시 파일 0. 확인 중 사용자가 엽서 1장을 직접 삭제했어(파일 수 변화 원인).
- **로컬 자동검증 — 실행:** 속도 조정 후 새 테스트 7/7(시간표 순서, 하강 감속·상승 출발 완만 검사 추가), 전체 `testDebugUnitTest` 1,302건·139 suite 실패 0, `assembleDebug` 성공(Gradle 9.9.1, Zulu 17).
- **실사용 기기 instrumentation — 불필요:** 추가·변경 없음.
- **실기기 감각 QA — 실행(승인된 실사용 기기 SM-S936N, 사용자 승인 `adb install -r`, firstInstallTime 유지·기존 사진 14개 보존 확인):** 정상 저장 → 펀칭 1회 → 갤러리 복귀·새 엽서 표시(사용자 "잘 됐어"). PC로 받은 연속 스크린샷에서 펀칭기 하강, 절단 후 구멍 속 매트·조각 그림자·들림 확인. 연타 + 재생 중 뒤로가기: 엽서 1장만 생성, 복귀 1회, 앱 종료 없음. 애니메이션 끔: 끝 장면 후 바로 복귀(사용자 확인), 배율 1.0 복원 확인. 5회 저장 동안 이 앱 FATAL 없음, `postcards_temp` 0개.
- **미검증·한계:** 저장 실패 화면은 안전한 재현 수단이 없어 코드 경로 확인만(기존 Error 분기 무변경). 회전 중 재생성 이어 재생은 실기기 미확인. 반복 저장 메모리는 측정 화면 상태가 달라 PSS 265MB(3회 후)·324MB(5회 후, Graphics 44→121MB)를 엄밀히 비교하지 못했어 — 누수 단정도 배제도 하지 않아. 녹화 영상(h264)은 PC 디코더가 없어 프레임 분석 미실행(세션 scratchpad, 저장소 밖).
- 테스트 중 생긴 사용자 확인 사항: 19:16 저장 엽서는 사용자가 앱에서 직접 삭제했어(파일 소실 아님). 오늘 테스트 엽서는 기기에 남아 있고 에이전트는 지우지 않았어.
- **TEST-COVERAGE-MAP — 갱신 완료:** 기존 문서 맨 위에 140일차 절 추가(JVM 테스트 파일 +1·@Test +6, 보호·공백 범위). 새 Markdown 파일은 만들지 않았어.

## 남은 위험·재개 조건

1. 사용자가 실제 모션을 다시 보고 2D 근사(우표가 가로로 눌려 보이는 원근, 매트 위아래 띠, 펀칭기 크기·그림자 진하기)를 승인해야 최종 확정이야. 조정 요청은 같은 브랜치에서 수치만 바꿔.
2. main 병합은 PR head/base·CI를 사용자가 검토·승인한 뒤에만.

---

# HANDOFF — 2026-11-21 139일차 후속: Markdown 운영 규칙 최소 정비

패코(Codex), 수동 표준 모드. 사용자 후속 작업지시서로 기존 규칙 최소 수정·기존 HANDOFF 갱신·작업 브랜치 commit/push·PR·필수 CI 확인까지 승인됐어. **main 병합은 별도 최종 승인 전 금지**야. 아래 선행 작업 원문은 당시 상태로 그대로 보존했어.

## 현재 상태와 변경 범위

- 시작 local main = origin/main = GitHub 실제 main = `ceea1b488a84d02b64f659c04f21f809524b0ffe`. PR #34는 이미 병합됐고, 그때 작업 브랜치 정리도 끝났어. 아래 원문의 미병합·commit 전 상태는 현재 상태가 아니야.
- 작업 브랜치 `docs/day139-markdown-rules`, 작성 시점 HEAD는 위 시작 SHA야. 시작 tracked/staged 변경 0개, 기존 미추적 47개와 SAFE 38개 해시 일치. 새 worktree 없음.
- 저장소 수정은 `AGENTS.md` 문서·목업 저장 위치 절의 기존 두 항목과 이 HANDOFF뿐이야. 채팅 인수인계·기존 HANDOFF 갱신·명시적 독립 파일 생성을 구분하고, 기존 문서 우선·승인된 독립 증거 보존·재승인 방지 원칙을 유지했어. 과거 문서의 사실관계는 코드·설정·Git·CI와 대조하되 확정된 사용자 결정·안전 규칙은 임의로 무효화하지 않아.
- `CLAUDE.md`와 에이전트별 실행 지침은 기존 공통 규칙 참조로 충분해 무변경. README의 CI 버전 오기는 사용자 지시대로 별도 정정 사항으로 남겼어. 37개 Markdown 재감사·새 보고서/관리 대장·파일 이동/삭제 없음.

## 외부 사용자 플러그인 — 저장소 commit 밖의 변경

- 원본 `%USERPROFILE%/plugins/post-card-memory-workflow/skills/`의 `post-card-memory-workflow/SKILL.md`, `write-project-handoff/SKILL.md`만 수정했어. 작업 루틴은 공통 구분을 참조하고 과거 사실 대조를 명확히 했어. 인수인계 스킬은 일반 요청의 채팅 제공을 기본으로 하고 별도 파일 생성은 명시적 요청/승인 범위로 한정했어.
- 설치본은 `%USERPROFILE%/.codex/plugins/cache/personal/post-card-memory-workflow/0.1.0+codex.20260927080957/skills/`야. 시작 시 위 원본과 같았고 실제 스킬 목록은 이 캐시를 가리켜. 공식 재배포·재로딩 방식은 이번 확인 범위에서 확정하지 못했으므로 **캐시·플러그인 설정·버전은 임의 수정하지 않았어. 원본 수정 완료, 설치본 적용 대기**야. 현재 세션/새 세션에 자동 반영됐다고 주장하지 않아.

## 검증·한계·재개 조건

- 여섯 요청을 문구로 대조했어: 채팅 인수인계 → 채팅, HANDOFF 갱신 → 기존 문서, 명시적 독립 보고서 → 승인 범위 내 파일, 단순 조사 → 자동 파일 없음, 과거 보고서 → 최신 사실과 구분하되 확정 결정 존중, Codex/Claude → AGENTS 공통 기준. 실제 새 세션의 자동 선택·응답 동작을 실행 검증한 것은 아니야.
- **로컬 자동검증 — 실행:** 저장소 diff/공백 검사, 외부 원본의 의도한 치환 외 변경 없음·frontmatter 보존 확인, 스킬 기본 형식·상대 참조 파일 검사. 공식 `quick_validate.py`는 Python의 PyYAML 미설치로 import 단계에서 중단됐어(test infrastructure). 검증 성공으로 표시하지 않으며 패키지 설치·환경 변경은 하지 않았어.
- **Android 빌드·실사용 기기 instrumentation·실기기 감각 QA — 불필요:** 앱·테스트·Gradle·DB·자산 무변경, 기기 조작 없음. **GitHub Actions CI — 이 문서 작성 시점 미실행:** PR 생성 뒤 실제 범위와 필수 체크 결과를 최종 완료보고에서 확인해.
- 기존 HANDOFF/archive 원문·SAFE 38개·미추적 47개를 보존해. commit 전 최종 diff·민감정보·보호 해시를 재확인하고 승인된 두 저장소 파일만 명시 stage해. 최종 SHA·PR·CI 결과는 대화 완료보고에 남기며, 정상 Git 성공만의 추가 HANDOFF commit은 만들지 않아.
- 외부 설치본 적용은 원본 수정과 구분해 보고하고, 공식 적용 방법 확인 전 캐시 직접 수정으로 우회하지 않아. main 병합은 최종 PR head/base와 CI를 사용자가 검토·승인한 뒤에만 가능해. 우표 펀칭 Compose 구현은 별도 작업이며 이번 범위가 아니야.

---

# HANDOFF — 2026-11-21 139일차 선행: 저장소 구조 점검·목업 통합

패코(Codex), 수동 표준 모드. 이번 사용자 작업지시서에 따라 추적 목업만 통합하고 문서 구조를 조사했어. 우표 펀칭 Compose 구현은 보류 상태야. **사용자가 139일차 구조 정리의 stage·commit·push·PR·필수 CI 확인을 1단계 승인했어. main 병합은 미승인**이야. 이 문서는 최종 commit 전 기록이며 실제 SHA·PR·CI 결과는 최종 완료보고에서 확인해.

## 현재 결과와 Git 출발점

- 추적 목업 17개를 `docs/ai/mockups/`로 모았어. 기존 미추적 목업 30개는 `docs/mockups/`에 그대로 있어. 로컬의 두 폴더가 모두 없어졌다는 뜻은 아니야.
- 앱 Kotlin·Compose·리소스·Room·DB·Gradle 무변경. 실기기와 사용자 엽서를 조작하지 않았어. 기존 Markdown 37개는 전부 위치를 유지했어.
- 시작 local main = origin/main = GitHub 실제 main = `53edc4fbc6a5e2e29ae339d59912755ffcd6b066`. [PR #33](https://github.com/KimJinha-JJin/post-card-memory/pull/33)은 API 재조회에서도 merged=true, merge SHA도 같았어. 아래 138일차 병합 미승인 표기는 당시 기록이야.
- 현재 브랜치 `chore/day139-repository-structure-cleanup`, HEAD는 위 SHA 그대로야. 새 worktree 없음. 추적 파일 변경은 모두 이번 작업의 unstaged 변경이야.
- 시작 tracked 393개, tracked/staged 변경 0개, 미추적 47개. 미추적 구성은 루트 설정 후보 1·.claude/ 1·.kotlin/ 3·app/ 이미지 4·docs/ai/ 보안 준비 자료 8·docs/mockups/ 30개야. 보호 기준 JSON의 38개와 기존 미추적 47개 전체의 시작/종료 SHA를 대조해 보존을 확인했어.
- ignore 열거 결과 9,589개(.gradle 26·.idea 20·app 빌드 9,540·루트 build 1·heap dump 1·local.properties 1)를 정리하지 않았어. 전역 ignore 파일은 읽기 권한 경고가 있어 이 집계를 전역 규칙까지 완전한 판정으로 확대하지 않아. 저장소 .gitignore는 확인했어.
- `git-hooks-check.sh --fix` 결과 기존 pre-commit·pre-push 활성, 설정 무변경. Git ref 쓰기 sandbox 제한으로 브랜치 생성이 한 번 실패한 뒤 승인된 권한 실행으로 생성했어. 실제 파일 이동은 브랜치 전환 뒤에 했어.

## 폴더 집계와 역할

시작 시점 추적/미추적 수이며 하위 폴더를 포함해 상위 행과 중복돼. 날짜는 해당 영역의 마지막 Git 변경일이야. 로컬 파일 복사 시각을 이력으로 취급하지 않아.

| 경로 | 역할·종류 | 추적 / 미추적 | 마지막 Git 변경 | 문제점·권고 |
|---|---|---:|---|---|
| 루트 직접 파일 | MD 3·설정 등 6 | 9 / 1 | 파일별 상이 | 운영 진입점 유지, 설정 후보 보호 |
| .githooks/ | 검사 연결 훅 2 | 2 / 0 | 2026-11-15 | 유지 |
| .github/ | workflow 1·검사 SH 4 | 5 / 0 | 2026-11-19 | 목업 실행 참조 없음, 변경 없음 |
| app/ | KT 276·PNG 33·XML 7 등 | 320 / 4 | 2026-11-18 | 구조·참조만 조사, 변경 없음 |
| gradle/ | catalog·wrapper 설정 | 2 / 0 | 2026-11-19 | 변경 없음 |
| docs/ 전체 | MD 34·HTML 14·PNG 6·Java 1 | 55 / 38 | 2026-11-20 | 역할별 안내가 유용해 |
| docs/ 직접 파일 | 8월 6일 기술 보고서 MD 4 | 4 / 0 | 2026-08-06 | 보고서 이동 후보, 현재 유지 |
| docs/ai/ 전체 | 운영·관리·역사·목업 | 42 / 8 | 2026-11-20 | 현행과 날짜별 조사 혼재 |
| docs/ai/ 직접 파일 | MD 18 | 18 / 1 | 2026-11-20 | 아래 전체 분류 참조 |
| docs/ai/archive/ | 과거 HANDOFF MD 11 | 11 / 0 | 2026-11-20 | 원문 보존, 기존 상대 링크 문제 있음 |
| docs/ai/mockups/ | HTML 12·PNG 1 → HTML 14·PNG 3 | 13 / 0 → 17 / 0 | 2026-11-20 | 신규 목업 단일 경로 |
| docs/mockups/ | 추적 시안과 로컬 실험 | 4 / 30 → 0 / 30 | 2026-11-17 | 기존 로컬 자료 보호·폴더 유지 |
| docs/qa/day136/ | QA 설명·Java2D 도구·증거 PNG 3 | 5 / 0 | 2026-11-18 | 디자인 목업과 검증 증거 구분 적절 |
| docs/ai/privacy-rewrite/ | 보안 준비 JSON·CJS 7 | 0 / 7 | 미추적 | SAFE 보호, 실행·이동·수정 없음 |
| .claude/·.kotlin/·IDE/빌드 산출물 | 로컬 설정·로그·캐시 | 별도 보호/ignore | Git 이력 해당 없음 | 수동 정리 대상 아님 |

## 목업 경로 선택과 이동

`docs/ai/mockups/`는 2026-09-15 `49654c2`부터 축적됐고 최신 138일차 `89503c4`까지 이어졌어. `docs/mockups/` 추적 자료는 126일차 `5dff634`(2026-11-08)와 135일차 `860bc72`(2026-11-17)에 추가됐어. 기능별 시안 작성 과정에서 경로 선택이 갈라진 것으로 보이며, 기존 AGENTS에는 목업 단일 저장 규칙이 없었어. 작성자 의도까지 이력만으로 단정하지 않아.

경로 전체 문자열을 포함하는 추적 파일은 AI 경로 쪽 6개(문서 4·앱 주석 2), 다른 경로 쪽 6개(문서 1·앱 주석 4·QA 도구 1)였고 상대 링크도 별도로 조사했어. **13개와 최신 펀칭 목업을 유지하고 4개만 이동**하는 AI 경로가 변경 범위와 운영 문서의 기존 `mockups/` 상대 링크 측면에서 유리해. 반대 방향은 13개와 더 많은 기존 참조를 옮겨야 해. 목업은 앱 리소스·QA 증거와 계속 구분해.

아래 파일의 접두 경로를 `docs/mockups/` → `docs/ai/mockups/`로 바꿨어. 파일명 충돌은 없었어. 이동 직후 4개 모두 SHA-256이 원본과 같았어.

| 파일 | 최종 SHA-256 | 원본 대비 |
|---|---|---|
| memory-density-paper-day126.html | `8348f242101c015c7be4ae150cbe48f6d7d73debbb4df7a355f479a7bc6b00e8` | 바이트 동일 |
| day135-stamp-edge-study/c-edge-corners.png | `55011def9b4900a646dd8269b7c088da21253d499518e2d1b6d6affd70cc3178` | 바이트 동일 |
| day135-stamp-edge-study/c-small-cards.png | `20ecfeda96a6cf18bf348c5748de399043964b70f611fd16c8934c1fc31b4f7a` | 바이트 동일 |
| day135-stamp-edge-study/index.html | `3bdc16537a8c3d5bd169ebfb422b4b26d288fd2c19e249bbfa73e3f6dc029b34` | 보고서 href 1곳만 변경 |

index.html 원본 SHA는 `db9a4386b76569426bc33c18f5107be8d795d16750f2265b0134dbb212eb3ced`야. 이동 후 보고서 링크 `../../ai/DAY-135-STAMP-EDGE-STUDY.md` → `../../DAY-135-STAMP-EDGE-STUDY.md`만 바꿨어. 역치환한 바이트 전체가 원본과 일치하는지 확인해 내용·동작 변경과 구분했어.

연결 수정: DAY-135-STAMP-EDGE-STUDY.md의 링크 3개, docs/qa/day136/README.md의 링크 1개, StampPerforationPreview.java의 입력 경로 1개, 이동 HTML의 보고서 href 1개야. Java는 PC QA 재현 도구이며 앱 코드가 아니야. 기존 QA PNG는 재생성하거나 덮어쓰지 않았어.

미추적 목업 30개는 이동하지 않았어: 단독 HTML 6개(detail-topbar-height-day125, detail-topbar-variants-day125, menu-structure-day125, future-mailbox-diorama-day127, future-mailbox-inside-day129, paper-touch-feel-day130)와 day132-gallery-shake/ 10·day132-memory-box/ 5·day132-tweezer-box/ 4·day133-film-intro/ 5개야. 앱 주석의 옛 경로 4곳은 이 미추적 자료를 가리키므로 유지했어. 빈 원래 day135 디렉터리도 강제 삭제하지 않았어.

archive의 당시 경로 문자열은 원문 그대로 남겼어. 옛 126일차 경로는 [현재 HTML](mockups/memory-density-paper-day126.html), 135일차는 [현재 HTML](mockups/day135-stamp-edge-study/index.html)·[작은 카드](mockups/day135-stamp-edge-study/c-small-cards.png)·[모서리](mockups/day135-stamp-edge-study/c-edge-corners.png)로 찾아가면 돼.

## Markdown 37개 전체 진단

**유지 28개·이동 후보 8개·추가 검토 1개**야. 이동 후보도 이번에는 위치를 유지했어. 새 문서나 docs/README.md는 만들지 않았어. 문서 수를 줄이는 것이 목적이 아니야.

| 문서 또는 그룹(개수) | 현재 위치 | 역할 | 판단 | 제안 |
|---|---|---|---|---|
| AGENTS.md, CLAUDE.md (2) | 루트 | 공통·에이전트 운영 규칙 | 유지 | 진입점 유지, AGENTS 저장 규칙만 추가 |
| README.md (1) | 루트 | 프로젝트 소개·운영 문서 진입 | 유지 | 소개 기준일과 현행 상태 구분이 이미 있어 |
| HANDOFF.md (1) | docs/ai/ | 현행 교대 장부 | 유지 | 이번 결과와 이전 원문을 분리해 보존 |
| CURRENT_TASK.md, WORK_CONTEXT.md, STATUS.md (3) | docs/ai/ | 비활성 작업판·역사 맥락 | 유지 | 비활성 표기가 명확해, 수동 모드에서 재활성화하지 않아 |
| DECISIONS.md (1) | docs/ai/ | 유효·대체된 결정 근거 | 유지 | 현행 결정 기준 역할 유지 |
| IMAGE-ASSET-STANDARD.md (1) | docs/ai/ | 이미지 제작 기술 기준 | 유지 | 적용 지침과 과거 조사 기준일이 구분돼 있어 |
| IMAGE-ASSET-MAP.md, TEST-COVERAGE-MAP.md (2) | docs/ai/ | 지속 갱신 관리 목록 | 유지 | 자산·테스트 보호 범위 무변경이라 수정 불필요 |
| CI-AUDIT-79.md, CI-AUDIT-80.md (2) | docs/ai/ | CI 도입 전후 조사·실패·성공 근거 | 이동 후보 | docs/reports/ 후보. 현재 CI 규칙으로 읽지 않아 |
| DAY-111-CALENDAR-AUDIT.md, DAY-111-STICKER-POUCH-ASSET-PLAN.md (2) | docs/ai/ | 구조 감사·디자인·QA·조건부 계획 | 유지 | 지도·목업의 직접 참조가 있어 이동 이점이 작아 |
| DAY-133-SECURITY-FOLLOWUP.md, DAY-137-SECURITY-AUDIT.md (2) | docs/ai/ | 날짜별 보안 후속·의존성 조사 | 이동 후보 | docs/reports/ 후보. 잔여 위험의 현행 HANDOFF 연결은 유지해야 해 |
| DAY-135-STAMP-EDGE-STUDY.md (1) | docs/ai/ | 테두리 구조·C1/C2/C3 설계 근거 | 유지 | 펀칭 후속 직접 근거, 이번엔 링크만 수정 |
| LETTER-DAY-100.md, LETTER-DAY-100-GABI.md, LETTER-DAY-100-PACO.md (3) | docs/ai/ | 작성자별 기념 편지 | 유지 | 기술 문서 중복이 아니며 3개만 위한 새 폴더 실익은 작아 |
| atomic-storage-overwrite-stabilization-2026-08-06.md, detail-structure-audit-2026-08-06.md, detail-structure-audit-phase2-2026-08-06.md, storage-overwrite-test-failure-investigation-2026-08-06.md (4) | docs/ | 구조 감사·분리 준비·실패 원인·안정화 연속 기록 | 이동 후보 | docs/reports/에 함께 두면 찾기 쉬워져. 상호 경로 문자열도 함께 검증해야 해 |
| HANDOFF-2026-09-13-harness.md, HANDOFF-2026-09-14-history-investigation.md, HANDOFF-2026-09-14-visit-calendar-final.md (3) | docs/ai/archive/ | 하네스·방문 정책 STOP·최종 구현 | 유지 | 당시 사고·판단·승인 증거 보존 |
| HANDOFF-through-2026-09-12.md, HANDOFF-through-2026-09-20-before-close.md, HANDOFF-through-2026-09-20.md, HANDOFF-through-2026-09-22.md (4) | docs/ai/archive/ | 누적 역사와 마감 시점 | 유지 | 날짜가 겹쳐도 기록 범위가 달라, 원문 보존 |
| HANDOFF-before-day135-2026-11-17.md (1) | docs/ai/archive/ | 84~134일차 중심 누적 기록 | 추가 검토 | 큰 파일 탐색·기존 링크 문제만 후속 검토. 축약·삭제 금지 |
| HANDOFF-before-day136-2026-11-18.md, HANDOFF-before-day137-2026-11-19.md, HANDOFF-before-day138-2026-11-20.md (3) | docs/ai/archive/ | 135~137일차 원문 | 유지 | 짧은 개별 기록과 당시 승인·미검증 보존 |
| README.md (1) | docs/qa/day136/ | C2·그림자·실기기 QA 근거 | 유지 | 시안과 실제 QA 구분 적절 |

미추적 PRIVACY-REWRITE-FINAL-PLAN.md는 추적 37개 밖의 별도 보안 준비 문서야. 관리·승인 상태 추가 검토 대상으로 식별만 했고 이동·수정하지 않았어.

### archive 크기와 중복

11개 합계 1,269,359 bytes야. 개행을 정규화한 30줄 이상 연속 동일 구간을 모든 파일 쌍에서 비교했지만 없었어. 30자 이상 비어 있지 않은 동일한 줄이 3개 이상 겹치는 쌍도 없었어. 전체 파일 해시 중복도 없어. 짧은 재서술이나 같은 사건을 참조하는 의미상 중복까지 없다는 주장은 아니야.

| archive 파일명 | bytes | 범위·보존 가치 |
|---|---:|---|
| HANDOFF-2026-09-13-harness.md | 5,469 | 하네스·사고 원문 안내 |
| HANDOFF-2026-09-14-history-investigation.md | 9,254 | 방문 이력 조사·정책 STOP |
| HANDOFF-2026-09-14-visit-calendar-final.md | 28,755 | 방문 달력 최종 구현·후속 |
| HANDOFF-through-2026-09-12.md | 421,229 | 71일차까지, 앱 제거 사고·복구 범위 포함 |
| HANDOFF-through-2026-09-20-before-close.md | 187,305 | 73~79일차 중심, 마감 전 이력 |
| HANDOFF-through-2026-09-20.md | 4,074 | 79일차 보호지도·CI 조사 마감 |
| HANDOFF-through-2026-09-22.md | 15,925 | 80~81일차 CI·실패·migration·규칙 후속 |
| HANDOFF-before-day135-2026-11-17.md | 576,415 | 84~134일차 중심 누적 원문 |
| HANDOFF-before-day136-2026-11-18.md | 5,001 | 135일차 시안·미검증·승인 경계 |
| HANDOFF-before-day137-2026-11-19.md | 11,555 | 136일차 구현·시행착오·병합 후속 |
| HANDOFF-before-day138-2026-11-20.md | 4,377 | 137일차 보안·잔여 위험 |

정리안은 원문 그대로 유지야. 큰 파일만을 이유로 삭제·요약 대체·병합·재작성할 근거가 없어. archive의 상대 링크는 당시 HANDOFF 위치 기준인 곳이 있어 현재 위치에서 깨져 있어. 이동 전 파일 링크 누락 51건 → 이동 후 50건(모두 archive), 신규 누락은 없었어. 135일차 보존본 목업 링크 1건은 이동으로 자연스럽게 정상화됐어. 나머지는 원문을 바꾸기보다 별도 길잡이에서 실제 목적지를 안내하는 후속안이 적절해.

## 검증·diff·남은 한계

- Git index 추적 수는 stage 전이라 원래 경로 393개 그대로야. 이동 대응을 적용한 작업트리도 393개로 1:1 대응해. 삭제 4개+목적지 미추적 4개는 unstaged 이동 상태이며 기존 미추적 47개와 구분해야 해.
- 기존 파일 수정 5개: AGENTS.md, DAY-135-STAMP-EDGE-STUDY.md, docs/qa/day136/README.md, docs/qa/day136/StampPerforationPreview.java, 이 HANDOFF.md. 이동 4개 중 index.html의 href 1곳만 의도적으로 변경했어.
- AGENTS에는 신규 HTML·PNG 저장 경로, 복수 파일 하위 폴더, 목업/앱 자원/QA 구분, 현행/과거 기록 구분, 새 문서 전 기존 문서 재사용 확인만 추가했어.
- **로컬 자동검증 — 실행:** 이동 직후 4개 SHA 일치, 최종 3개 동일·HTML 링크만 변경, 목업 HTML 14개 inline JavaScript 구문 검사, docs PNG 6개 헤더·청크 경계·IDAT 압축 해제, Markdown 상대 파일 링크 전후 비교, 내부 경로 검색, 보호 자료 해시, diff·민감정보 검사. 브라우저 동작·시각 재생은 미실행이라 기존 렌더링 QA와 이번 정적 검사를 구분해야 해.
- **Gradle/JVM/APK 전체 빌드 — 불필요:** 앱·빌드 입력 무변경. Java2D 도구는 입력 경로만 검증했고 재실행하지 않아 QA PNG를 보존했어.
- **실사용 기기 instrumentation — 불필요.** CONDITIONAL/FORBIDDEN은 격리 환경이 없어 실행 불가이며 이번 대상이 아니야. 기기 연결·설치·DB·파일 조작 없음.
- **GitHub Actions CI — 미실행:** 아직 commit·push·PR 없음. HTML·PNG·Java도 포함하므로 향후 docs-only 생략을 가정하지 말고 실제 CI 범위 판정과 결과를 확인해야 해.
- **실기기 감각 QA — 불필요:** 앱 결과 무변경. TEST-COVERAGE-MAP 변경 없음(앱 테스트·보호 범위 무변경).

## 후속 제안과 펀칭 재개 조건

1. 현재 위치를 유지하는 것이 기본 권고야. 실제 이동 이점이 있는 후보는 docs/ 기술 보고 4개 + CI-AUDIT 2개 + 보안 보고 2개, 총 8개를 `docs/reports/`로 모으는 안이야. 사용자 승인 전 이동하지 않아. 상호 링크·원문 경로·현행 기록 연결을 먼저 검증해야 해.
2. `docs/README.md`는 운영·보고서·목업·QA·archive 역할과 큰 파일의 날짜 범위를 안내하면 유용해. 새 목차 자체를 늘리지 않도록 보고서 이동과 함께 승인받을 후속 후보로만 남겼어. 현행 상태·규칙 본문을 복제하지 않는 방향을 권장해.
3. 미추적 목업 통합·Git 추적 추가는 별도 사용자 판단 대상이야. 새 저장 규칙이 기존 보호 자료 이동 승인을 뜻하지 않아.
4. 다음 행동은 승인된 이 구조 정리만 명시적으로 stage·commit·push하고 main 대상 별도 PR·필수 CI를 확인하는 것이야. PR·head/base SHA·실제 CI 검증 범위를 보고한 뒤 main 병합 승인 대기에서 멈춰. main 병합은 별도 2단계 승인이고 펀칭 작업과 섞지 않아.
5. 펀칭은 별도 구현 작업지시서로 재개해. 기준은 [138일차 C안](mockups/stamp-punch-motion-mockup.html), [C2 설계](DAY-135-STAMP-EDGE-STUDY.md), [136일차 QA](../qa/day136/README.md)야. C안 1,900ms·저장 성공 뒤 대기 화면 대체·갤러리 복귀 결정을 유지하고 저장 중/실패/취소, 성공 시점 동기화, 뒤로가기·연타, reduced motion, 사진 decode·메모리를 검토해. 이번에는 구현하지 않았어.

아래 138일차 원문은 그대로 보존한 역사 기록이야. 현재 Git·승인·미검증 상태는 위 139일차 절을 먼저 읽어.

---

# HANDOFF — 2026-11-20 138일차: 우표 펀칭 모션 목업 · C안 채택

피코(Claude Code), 수동 표준 모드. 사용자 138일차 작업지시서(C2 구조 조사 → 독립 HTML 비교 목업 → 브라우저 검증 → 사용자 채택)와 최종 승인(C안·(가) 적용 결정 기록, 목업 종료 표현 정리, 목업·HANDOFF·archive 한정 commit/push/PR, **main 병합 미승인**)을 수행했어. 직전 137일차 HANDOFF 원문은 [archive/HANDOFF-before-day138-2026-11-20.md](archive/HANDOFF-before-day138-2026-11-20.md)에 바이트 그대로 보존했어.

## 현재 상태

- 시작 실측: 원본 폴더 `main` = `origin/main` = `10afcf7`(PR #32 병합). 137일차 HANDOFF의 "main 병합 미승인"은 작성 시점 기록이고, 실제로는 이후 병합됐어. 미추적 파일은 기존 보호 자료 목록 그대로였어.
- 작업 브랜치 `feature/day138-stamp-punch-mockup`(원본 폴더, `10afcf7`에서 생성). 새 worktree는 만들지 않았어. 이전 세션 scratchpad의 `head-baseline-worktree`(prunable 표시)는 건드리지 않았어.
- 산출물: [stamp-punch-motion-mockup.html](mockups/stamp-punch-motion-mockup.html) 한 파일(외부 자원·CDN 없음). 판정 **138일차 완료 / C안 채택 / 실제 앱 구현 대기**.
- 앱 소스·리소스·Room·Gradle·C2 테두리는 **무변경**.

## 확정된 제품 결정(2026-11-20, 사용자)

- **C안(경쾌한 손맛형) 채택.** 전체 1,900ms 유지: 접촉 540, 절단 560, 펀칭기 상승 600–860, 분리 확인 860–1060, 우표 들림 1,060–1,360(높이 5), 정착 1,360–1,660 후 종료 정지.
- **적용 위치 (가):** 저장 후 기존 대기 화면(`TeleportWaitingScreen`, `CaptureState.Success` 뒤 1,100ms)을 펀칭 애니메이션으로 대체하고, 끝나면 **지금처럼 갤러리로 복귀**. 꾸미기 화면 진입 흐름은 바꾸지 않아.
- **길이 차이:** 기존 성공 대기 1,100ms → C안 1,900ms(+800ms). 실제 저장 완료 시점·화면 이동과의 동기화는 139일차 구현 단계에서 검토해.
- 목업 정리: 종료의 어두워짐(조명 변화)과 "꾸미기로 넘어가는 암시" 문구를 제거했어. 종료는 우표가 들린 채 멈추는 장면이고 특정 화면 이동을 암시하지 않아. 펀칭·절단·상승·들림 타이밍과 움직임은 그대로야. A/B는 비교 기록으로 남겼어.

## C2 구조 조사 요약(읽기 전용)

- 윤곽 원본: `utils/StampPerforation.kt traceStampPerforation`(단위 정사각형, 16구간·입구 48%·깊이 15%). Android 어댑터는 `utils/PostcardRenderSpec.kt createPinkingPath`(RectF 스케일 + `Path.arcTo`), Compose는 `ui/components/StampPhoto.kt PinkingPhotoShape/StampPhoto`가 같은 Path를 감싸. 흰 테두리는 같은 윤곽 2배 굵기 선을 윤곽 안으로 clip(exporter `STAMP_BORDER_WIDTH`=18 / 우표 1000).
- 목업은 `traceStampPerforation`을 같은 연산 순서로 JS로 옮긴 경로 문자열 하나(`STAMP_D`)로 구멍(시트+STAMP_D, evenodd)·조각·펀칭날·펀칭기 창·그림자·절단선을 모두 그려. 윤곽 닫힘 오차 5.5e-17.
- 진입 흐름: 갤러리 → `camera` → `CropEditorScreen`(`PinkingPhotoGuide`) → `saveCroppedPhoto` → `TeleportWaitingScreen`("우표를 정리하는 중") → `CaptureState.Success` 1100ms → `popBackStack()`으로 **갤러리 복귀**. 꾸미기(`detail/{id}`)로 바로 가는 경로는 없어. NavHost에 커스텀 전환 없음(기본값).
- 움직임 줄이기: `ui/gallery/StickerStashPouch.kt rememberSystemReducedMotion()`(ANIMATOR_DURATION_SCALE==0), `DecorationLiftFeel.kt` 등이 사용.

## A/B/C 시간표(ms, HTML `CANDS` 값과 동일)

| 시안 | 전체 | 하강 | 접촉 | 절단 | 펀칭기 상승 | 분리 확인 | 우표 들림 | 들림 높이 |
|---|---:|---|---:|---:|---|---|---|---:|
| A 정석형 | 2600 | 300–900 | 900 | 900 | 980–1480 | 1480–1780 | 1780–2280 | 8 |
| B 분리 강조형 | 3000 | 300–1000 | 1000 | 1220 | 1260–1860 | 1860–2200 | 2200–2700 | 14 |
| C 경쾌한 손맛형 | 1900 | 250–540 | 540 | 560 | 600–860 | 860–1060 | 1060–1360 | 5 |

## 검증

- **브라우저 시각 검증 — 실행:** Chrome 확장 미연결이라 로컬 Edge headless + DevTools 프로토콜로 조작·캡처(임시 프로필·캡처는 세션 scratchpad, 저장소 밖). 5개 장면 정지, 재생 중 시간 진행, 일시정지 후 정지 유지·이어서 재생, 반복/반복 해제 시 종료 정지, 후보별 단독 재생, 움직임 줄이기 3상태 전환, 390px(가로 스크롤 없음)·1280·1700px, 스크립트 오류 0건 확인.
- **구멍·조각 일치 픽셀 비교 — 실행:** 펀칭기를 숨긴 같은 조건에서 "절단 전 한 장" vs "절단 후 조각 제자리(절단선 숨김)"를 DPR2로 비교. 차이는 우표 윤곽·그림 경계선의 안티에일리어싱 띠뿐이고 면 영역 차이는 없었어(변경 픽셀 1.6%, 모두 선 위).
- **미검증:** 실제 사람 눈의 60fps 체감, 저사양 기기 성능, `prefers-reduced-motion` OS 설정 자체(체크박스 시뮬레이션만 확인), Android Compose 픽셀 일치.
- 로컬 Gradle/JVM — 불필요(앱 무변경). 실사용 기기 instrumentation — 불필요. 실기기 감각 QA — 불필요(HTML 목업). TEST-COVERAGE-MAP — 변경 없음.
- **종료 표현 수정 후 재검증 — 실행:** 같은 방식으로 윤곽 자기 점검(STAMP_D 81곳 일치·닫힘 오차 5.5e-17), C안 시간표·단계 표시, 종료 장면(어두워짐 없음), 일시정지 유지·이어서 재생·반복, 움직임 줄이기 3상태, 390px 가로 스크롤 없음, 구멍·조각 픽셀 비교(면 영역 차이 없음), 스크립트 오류 0건을 다시 확인했어.
- SAFE 보호 기준 `protected-baseline.json` 38개 SHA-256 시작·수정 후 모두 일치.

## 남은 위험·후속 후보(승인 전 미실행)

1. 139일차 실제 구현은 별도 작업지시서 대기. 검토할 것: 저장 중(`CaptureState.Saving`) 화면과 펀칭 시작 시점, 저장 실패·취소 때 펀칭을 보여주지 않기, 1,900ms 동안 뒤로가기·연타 처리, 움직임 줄이기에서의 짧은 대체 표시, 실제 촬영 사진을 우표 면으로 쓸 때의 decode·메모리.
2. Compose 구현 예상: 기존 `createPinkingPath` 재사용(구멍은 `Path.op(DIFFERENCE)` 또는 evenodd), `graphicsLayer` 기반 들림, 움직임 줄이기 시 즉시 종료 상태. 3D 원근은 목업 연출이라 앱에서는 2D 근사가 필요할 수 있어.

## Git 출발점

- 변경 파일: `docs/ai/mockups/stamp-punch-motion-mockup.html`(신규), `docs/ai/HANDOFF.md`, `docs/ai/archive/HANDOFF-before-day138-2026-11-20.md`(신규, 137일차 원문 — 사용자 승인으로 포함).
- 1단계 승인 범위: 위 세 파일 명시적 stage → commit → push → PR → 필수 CI 확인. 이 HANDOFF는 commit 전 작성이라 SHA·PR 번호·CI 결과는 대화 완료보고와 PR 기준으로 확인해.
- **main 병합은 미승인.** 성공 기록만을 위한 추가 HANDOFF commit은 만들지 않아.
