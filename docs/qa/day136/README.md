# 136일차 C2 이식 검증

## 기준과 구현

사용자가 2026-11-18 C2를 단일 구현안으로 확정했어. [135일차 HTML](../../ai/mockups/day135-stamp-edge-study/index.html)의 `shallowAligned(C2)`가 기준이며 C1/C3와 목업은 수정하지 않았어.

- 16분할, 변당 일반 홈 15개. 입구48%·깊이15%·연결부52%는 모두 천공 간격 대비 비율이야.
- 반입구 `a=.24`, 깊이 `d=.15`, 원 반지름 `R=(a²+d²)/(2d)=.267`, 바깥 중심 거리 `o=R-d=.117`.
- 단위 정사각형의 간격 `p=1/(16−2o)`, 모서리 접점 `(a−o)p`. 모서리도 같은 원 격자를 사용해. 임의 둥근 코너나 별도 보정 홈은 없어.
- `traceStampPerforation` 한 곳에서 직선·원호를 방출하고 `PostcardRenderSpec.createPinkingPath`가 Android Path로 변환해. 기존 Compose wrapper는 그 Path를 그대로 감싸. 정사각형이 아닌 RectF에는 단위 윤곽을 각 축으로 확대하므로 원호가 타원이 될 수 있어. 현재 조사한 사진·가이드 호출은 정사각형이야.
- 사진과 흰 여백은 같은 외곽 Path를 써. 두 배 굵기 중심선을 외곽 안쪽으로 clip해서 기존 명목 흰 여백(갤러리3dp, exporter 논리18)을 남겨. 별도 축소 Path의 홈 위상 차이를 없앴어.
- 사진 비율·crop·zoom/offset·우표 배치는 변경하지 않았어. C2 이식 단계에서는 그림자 offset(논리22/26)·색/알파를 유지했어. 그림자 안쪽 여백(논리9)도 기존 값을 복원했어. 이후 사용자가 요청한 그림자 강도 조절은 아래 별도 비교에 기록해. 상세 미리보기와 앞면 공유/내보내기는 같은 공통 renderer를 써.

## 자동검증

- 로컬 전체 JVM **1295/1295**, 실패·오류·skip 0, XML138개. JVM source `@Test`1295 / test 파일137 / helper1. 신규 `StampPerforationTest`6건: HTML 독립 수치 fixture, 다섯 크기의 깊이, 홈·모서리 수/반지름, 원호 연결/폐합, 네 변 회전 대칭, 38/60dp·여러 밀도의 입구/연결부 비율, 유한 좌표와 범위.
- `testDebugUnitTest assembleDebug assembleDebugAndroidTest` 성공. 그림자 회귀 보정 뒤에는 영향에 맞게 `testDebugUnitTest assembleDebug`를 다시 실행해 성공했어. instrumentation21건/8파일은 컴파일만 확인했고 기기에서 실행하지 않았어.
- 첫 sandbox/offline 실행은 플러그인 캐시 해석 실패로 코드 검사 전에 중단됐어. production 실패가 아닌 실행환경 문제였고 설정·dependency 변경 없이 기존 Gradle/SDK 접근으로 해결했어.
- `TEST-COVERAGE-MAP.md`에 신규6건과 실제 보호 범위를 반영했어. 기존 Android golden을 갱신하지 않았어.

## PC 전후 비교의 근거와 한계

[전후 다섯 크기](c2-before-after.png), [60dp 네 모서리 6배](c2-corners-6x.png)는 [재현용 Java 프로그램](StampPerforationPreview.java)이 **빌드된 production Kotlin 함수**를 호출해서 그린 Java2D 비교야. 사진은 기존 HTML에 내장된 프로젝트 카메라 사진을 메모리에서 읽어. 개인 엽서·외부 자원은 넣지 않았어.

비교 조건: 밀도2.8125, 갤러리 흰 여백3dp, 그림자 없음, 같은 사진/crop. 이 조건은 PC 재현 조건이며 Android의 픽셀 rasterization·안티앨리어싱·Compose clip을 검증하지 않아. Android golden이나 실기기 스크린샷으로 취급하지 않아.

| 한 변(dp) | 전체 차이 픽셀 | 가장자리12dp를 제외한 내부 차이 |
|---|---:|---:|
| 93.333333 | 8303 | 0 |
| 106.666667 | 11127 | 0 |
| 146.666667 | 19241 | 0 |
| 60 | 3910 | 0 |
| 38 | 1632 | 0 |

바깥 톱니→얕은 원호와 흰 여백의 안쪽 생성 방식이 의도한 차이야. 이 PC 비교에서는 차이가 가장자리 영역에 한정됐고 사진 내부의 비의도적 차이는 검출되지 않았어. 다른 layout의 픽셀 불변까지 입증한 것은 아니야.

38dp 깊이는 약.362dp, 60dp는 약.571dp로 작아서 작은 화면에서 거의 사각형처럼 읽힐 수 있어. 승인된 목업의 한계를 그대로 유지했고 홈을 임의로 키우지 않았어. 6배 이미지는 원본 raster를 nearest-neighbour로 확대한 것으로 계단 현상이 두드러져 보여.

재현은 먼저 앱을 빌드한 뒤 Java17의 source-file mode로 실행해. 클래스 경로는 현재 AGP 산출물 기준이야.

```powershell
$classes = (Resolve-Path 'app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes').Path
$stdlib = Get-ChildItem "$env:USERPROFILE/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib" -Recurse -Filter '*.jar' | Select-Object -First 1 -ExpandProperty FullName
java '-Djava.awt.headless=true' --class-path "$classes;$stdlib" docs/qa/day136/StampPerforationPreview.java . docs/qa/day136
```

## 실기기 QA와 그림자 회귀

- 사용자 연결·설치 승인 후 SM-S936N 한 대를 확인했어. 실측1080×2340, 450dpi. 기존 APK와 새 APK의 signing certificate SHA-256 일치 확인 후 `adb install -r`만 실행했어. 최초 설치 시각 유지 확인은 설치 연속성 근거이며 DB·전체 파일의 byte 보존을 보증하는 검사는 아니야.
- 사용자 직접 실행으로 갤러리·카메라·상세/편집·공유·내보내기 동작은 모두 정상 확인했어. 자동으로 사용자 엽서를 수정·저장·공유·내보내기하지 않았어.
- 첫 구현에서 그림자 Path의 기존 inset9도 제거한 회귀가 있었어. 사용자가 상세 우표 뒤 그림자가 어색하다고 보고했고, 화면에서 진한 이중 윤곽을 관찰했어. **그림자 보정 1회차**로 기존 inset9만 복원했어. C2 수치·사진/테두리와 offset·색·알파를 재설계하지 않았어.
- 그림자 보정본: JVM1295 통과·앱 빌드 성공 후, 기존 승인 범위의 동일 기기·동일 서명·`install -r`로 설치했어. 최종 사용자 재확인 결과는 HANDOFF에 기록해.
- 개인 화면 캡처와 설치 전 APK는 PC 임시 폴더에만 두었고 Git에 포함하지 않아. 실제38/60dp Android pixel 검증, 다른 기기·밀도와 성능 측정은 미실행이야.
- emulator·connected task·uninstall·초기화·기기 instrumentation은 실행하지 않았어. CONDITIONAL/FORBIDDEN instrumentation은 격리 환경이 없어 실행 불가야.

## 사용자 후속 요청 — 상세/편집 우표 그림자 강도 비교

사용자는 처음에 갤러리라고 표현했지만, 코드에서 정지 월별 카드에는 그림자가 없고 집기 순간 그림자와 상세 공통 renderer의 그림자가 별개임을 확인한 뒤 **상세/편집의 우표 뒤 회색 그림자**를 대상으로 지정했어. 갤러리 집기/전환·연못·랜덤 overlay 그림자는 수정 대상이 아니야.

기존 공통 그림자는 별도 blur 없이 C2 Path를 회색으로 채워 옮겨 그려. `intensify`·`soften`이라는 속성은 없어. 새 필터·하이라이트·광범위한 blur 대신 진하기와 튀어나오는 범위를 줄인 [현재/약화/미세 엠보싱 비교](c2-shadow-comparison.png)를 만들었어.

| 비교안 | 알파(255 기준) | 논리 offset X/Y | 안쪽 여백 | blur |
|---|---:|---:|---:|---:|
| 현재 | 85 | 22/26 | 9 | 0 |
| 약화안 | 30 | 16/18 | 9 | 0 |
| 미세 엠보싱안 | 18 | 11/13 | 9 | 0 |

사진·흰 여백·C2 수치·그림자 색은 같아. 133.33dp(상세 크기 재현),60dp,38dp와 60dp 우하단6배를 같은 밀도2.8125로 비교했어. 작은 카드에서 그림자 범위가 줄어 외곽선과의 경쟁이 약해졌고, 천공 깊이/간격 자체는 동일해. 오른쪽은 별도 emboss filter가 아닌 매우 약하고 짧은 기존 그림자야. 퍼져 탁해지는 위험을 피하려고 blur를 늘리지 않았어.

사용자는 **미세 엠보싱안**을 선택했어. 실제 공통 우표 renderer의 알파를85→18, 논리 offset을22/26→11/13으로 바꾸고 inset9·blur0·색은 유지했어. 최종 `testDebugUnitTest assembleDebug assembleDebugAndroidTest` 성공, JVM1295/1295·XML138·실패/오류/skip0을 확인했어.

최종 APK SHA-256은 `facc2c8f397af8c10e2af47695e4125748265b9d97ed070bcde55aa4b37085e9`야. 같은 실기기·서명으로 승인된 `install -r` 설치 성공과 최초 설치 시각 유지를 확인했어. 사용자는 **상세와 공유/내보내기 모두 괜찮아**라고 최종 재확인했어. 앞서 갤러리·카메라·상세/편집·공유·내보내기 동작 정상 확인을 받았고, 마지막 변경은 이 공통 그림자 수치에 한정됐어.

세 안의 이미지는 끝까지 Java2D 비교로 구분해. Android 실화면의 최종 미감 근거는 위 사용자 실기기 확인이며, 38/60dp의 별도 Android pixel 계측과 성능 측정은 미실행이야.
