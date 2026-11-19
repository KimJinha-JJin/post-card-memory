# 137일차 개발 환경·의존성 보안 점검 결과

2026-11-19, 피코(Claude Code), 수동 표준 모드. 기준 main `5621236f2e6169500fb2f53fd89c3f7d7d2a8e9e`, 작업 브랜치 `security/day137-dependency-audit`. 개인 경로·기기 식별값·인증정보 원문은 기록하지 않아.

## 1. 결론 요약

| 항목 | 식별번호 | 우리 영향 | 판정 | 처리 |
|---|---|---|---|---|
| Gradle version catalog 코드 실행 | GHSA-3cfg-jpcr-gfp3 (CVSS4 9.3) | 9.8.2가 영향 범위(9.0.0~9.9.0). 공격 조건(외부 catalog import·악성 catalog 값)은 현재 없음 | 높음(취약 버전 사용) | **9.9.1로 수정** |
| Gradle 종료 데몬 포트 탈취 | GHSA-pcp8-98xm-c6pr (CVSS4 7.0) | 영향 범위. 공격자가 같은 PC의 다른 로컬 사용자여야 함. 개인 PC·일회용 CI runner | 낮음 | 9.9.1로 함께 해소 |
| Gradle 데몬 registry 신뢰 | GHSA-gqrr-4j89-5267 (CVSS4 8.6) | 영향 범위. 공격은 `org.gradle.daemon.registry.base`를 설정한 악성 저장소가 필요. 우리 `gradle.properties`·이력 전체에 없음, 추적 `registry.bin` 0개 | 낮음 | 9.9.1로 함께 해소 |
| JDK Java4Shell | CVE-2026-114938 | 로컬 Zulu 17.0.21(+9)과 AS JBR 21.0.10은 버전상 영향. 단 **x64 + AVX-512 VBMI CPU**가 조건이고 이 PC CPU는 AVX-512 자체가 없음(JVM 실측) | 낮음(로컬) | 로컬 JDK 변경 안 함(사용자 판단) |
| 〃 (CI) | 〃 | `setup-java` temurin 17 `check-latest: true` → 공개된 수정판 17.0.21.1+2를 받음(이번 PR CI 로그로 확인) | 영향 없음(수정판) | 설정 변경 불필요 |
| Kotlin 빌드 캐시 역직렬화 | CVE-2026-53914 / GHSA-r937-wjx7-w2jp (6.7) | KGP 2.3.10이 버전상 영향(<2.4.20-Beta1). 실제 수정 commit은 **KAPT 증분 캐시** 한정. 우리 앱은 KAPT 미사용(KSP만), Gradle build cache 비활성 | 낮음 | 보류(2.3→2.4 언어 버전 변경) |
| 그 외 의존성 | 아래 4절 | 앱 런타임은 Guava 1건, 나머지는 AGP 빌드 도구 classpath | 낮음 | 기록만 |

## 2. 실제 실행 환경(설정값과 실측 구분)

- Wrapper 설정: 9.8.2 → **9.9.1** + `distributionSha256Sum`. 저장소에 `gradlew`·wrapper jar가 없어 Android Studio는 이 properties로 배포판을 받고, CLI는 캐시된 배포판을 직접 실행해.
- CI: `setup-gradle` `gradle-version` 9.8.2 → **9.9.1**.
- 실측 실행 JVM: `JAVA_HOME`·`GRADLE_JAVA_HOME` 미설정. PATH의 java = Zulu 17.0.21+9-LTS(Zulu17.70+111-CA). 프로젝트 IDE Gradle JDK도 `zulu-17`. Gradle `--version`의 Launcher·Daemon JVM이 같은 Zulu 17이었어.
- Android Studio 내장 JBR 21.0.10은 이 프로젝트 Gradle 실행에 쓰이지 않아(IDE 자체 실행용). `~/.jdks/jbr-17.0.14`도 미사용·보존.
- AGP 9.2.1, Kotlin(KGP·Compose 플러그인) 2.3.10, KSP 2.3.2, Compose BOM 2026.04.01, Room 2.8.4, Hilt 2.59.2: 파일 값과 resolved 값 일치, **변경 안 함**.
- Gradle 9.8.2 자체에 내장된 Kotlin 2.4.10은 Gradle 스크립트용이야. 앱 컴파일용 KGP 2.3.10과 다르며 CVE-2026-53914 대상인 KGP 판정과 섞지 않아.

## 3. 항목별 근거

### 3.1 Gradle (공식 권고 3건, 2026-11-10 공개)

- 세 권고 모두 영향 `>= 9.0.0, <= 9.9.0`, OSS 수정판 **9.9.1**(9.8.3은 유료 구독판). GitHub API로 원문을 직접 읽었어.
- catalog: `gradle/libs.versions.toml` 하나뿐, 외부 published catalog import 없음. 권고의 탐지식(`*/`, `\u`)으로 검사 0건. `~/.gradle/caches/*/dependencies-accessors/` 포렌식 검사 0건.
- 데몬: 개인 단일 사용자 PC, CI는 GitHub 호스티드 일회용 runner. 원격 네트워크로는 성립하지 않는 취약점이야.
- 공격 흔적이 없다는 것은 공격이 없었다는 증명이 아니야(권고 원문 문구와 같음).
- 9.9.1 무결성: services.gradle.org 버전 API 체크섬, 공식 `.sha256` 파일, 내려받은 zip 직접 계산 SHA-256 세 값이 `d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228`로 일치.
- 근거: [GHSA-3cfg-jpcr-gfp3](https://github.com/gradle/gradle/security/advisories/GHSA-3cfg-jpcr-gfp3), [GHSA-pcp8-98xm-c6pr](https://github.com/gradle/gradle/security/advisories/GHSA-pcp8-98xm-c6pr), [GHSA-gqrr-4j89-5267](https://github.com/gradle/gradle/security/advisories/GHSA-gqrr-4j89-5267)

### 3.2 JDK — Java4Shell

- Adoptium 공지(2026-11-18): OpenJDK 17 이상의 메모리 손상, x64에서 원격 코드 실행 가능, **CPU가 AVX-512 VBMI를 지원해야 영향**. 임시 완화 `-XX:-UseBASE64Intrinsics`. Temurin 수정판 17.0.21.1+2 / 21.0.13.1+2 / 25.0.5.1+2. JDK 8·11 영향 없음.
- 이 PC: Intel Core Ultra 5 125H. JVM `-Xlog:os+cpu` 기능 목록에 avx2까지 있고 avx512 계열 0개. 그래서 취약 코드 경로의 CPU 조건이 성립하지 않아.
- 참고 실측: Zulu 17.0.21의 `UseBASE64Intrinsics` 기본값 false, JBR 21.0.10은 true. 어느 쪽이든 AVX-512 VBMI가 없으므로 판정은 같아.
- Azul 수정판 존재 확인: Zulu CA `17.70.201`(= 17.0.21.1, build 2). 시스템 전체 JDK 교체는 사용자 승인 사항이라 설치하지 않았어.
- Android 앱 자체는 기기의 ART에서 돌아가며 HotSpot JDK가 아니므로 앱 사용자에게 이 취약점은 해당하지 않아.
- CI: `actions/setup-java@v6` temurin 17 + `check-latest: true`. Adoptium API에서 최신 17이 17.0.21.1+2(2026-11-18)임을 확인했고, 이번 PR CI의 실제 JDK는 6절에 기록해.
- 근거: [Adoptium 공지](https://adoptium.net/news/2026/11/temurin-november-2026-cspu-java4shell/), [CERT-EU 2026-021](https://www.cert.europa.eu/publications/security-advisories/2026-021/pdf)(작업지시서 제공 링크, 본문은 이번에 직접 읽지 않음)

### 3.3 Kotlin — CVE-2026-53914

- GHSA: `org.jetbrains.kotlin:kotlin-gradle-plugin` `< 2.4.20-Beta1`, 설명 "Kotlin before 2.4.20". 안정 수정판 2.4.20·2.4.21이 Maven Central에 있어.
- 수정 commit `bf51df6`의 실제 변경은 `plugins/kapt/.../incremental/cache.kt` 하나(KAPT 증분 캐시에서 역직렬화 클래스 제한).
- 우리 앱: `kotlin-kapt` 미적용, 소스·빌드 파일에 kapt 0건. Room·Hilt 모두 KSP. `org.gradle.caching` 미설정이고 로컬 build-cache 폴더도 없어. 공격에는 우리 build 디렉터리·캐시에 쓰기 권한이 필요해.
- 따라서 실제 노출 경로는 없다고 판단해 **낮음**. 2.4.x 업그레이드는 언어 minor 변경이라 Compose 컴파일러 플러그인·KSP·Hilt·Room의 Kotlin metadata 호환 확인과 앱 컴파일 영향이 커서 이번 최소 수정에서 제외하고 후속 후보로 둬.
- 근거: [GHSA-r937-wjx7-w2jp](https://github.com/advisories/GHSA-r937-wjx7-w2jp), [수정 commit](https://github.com/JetBrains/kotlin/commit/bf51df665b458fda7c3eaf436c4d88dc119d7ec6)

## 4. 의존성 전체 스캔(OSV)

- 방법: Gradle `:app:dependencies`(debugRuntime·debugCompile·debugUnitTestRuntime·debugAndroidTestRuntime·kspDebugKotlinProcessor)와 `buildEnvironment`의 resolved 좌표를 추출해 OSV `querybatch`에 조회. 고유 좌표 **358개**, 탐지 **13건**. 새 도구 설치나 CI 추가 없이 일회성 조사로 실행했어.
- 탐지만으로 취약점을 확정하지 않고 실제 위치와 경로로 판정했어.

| 좌표 | 권고 | 위치 | 판정 |
|---|---|---|---|
| guava 33.3.1-android | GHSA-xxph-c9ww-hj94 (CVE-2026-102554, 역직렬화 메모리 고갈 DoS, 수정 33.7.2) | **앱 런타임**, CameraX → media3-common 전이 | 낮음: 앱에 Java 직렬화(ObjectInputStream 등)·Guava 직접 사용 0건 |
| guava 33.2.1-jre | 〃 | KSP 프로세서 classpath(빌드 시) | 낮음 |
| guava 33.3.1-jre, commons-lang3 3.16.0(CVE-2025-48924), jose4j 0.9.5(CVE-2024-29371), bcprov/bcpkix 1.79(CVE-2025-14813, CVE-2026-8763, CVE-2026-0636, CVE-2026-5598, CVE-2026-13506, CVE-2026-5588), jdom2 2.0.6(CVE-2021-33813) | 각 GHSA | AGP 9.2.1 빌드 도구 classpath(`buildEnvironment`) | 낮음: 빌드는 우리 저장소의 신뢰 입력만 처리, APK에 포함되지 않음. 해소하려면 AGP 업그레이드 또는 buildscript 강제 버전 필요 → 범위 밖 |
| kotlin-gradle-plugin 2.3.10 | GHSA-r937-wjx7-w2jp | 빌드 도구 | 3.3절 |

- `kotlin-stdlib` 2.3.10(앱 런타임)은 OSV 탐지 없음. KGP 권고와 다른 산출물이야.

## 5. 앱 자체 설정 점검(수정 없음)

- 권한: CAMERA·VIBRATE만 선언. INTERNET 등 네트워크 권한 선언 없음(병합 manifest는 아래 6절 확인).
- exported: 런처 `MainActivity`만 true(필수). FileProvider `exported=false`, `grantUriPermissions=true`, 공유 시 intent 단위 `FLAG_GRANT_READ_URI_PERMISSION`. 경로는 `files/postcards/`, `cache/camera_capture/`, `cache/shared_postcards/` 하위로 한정 → **양호**.
- 외부 사진 입력: 시스템 PhotoPicker·SAF(OpenDocument)로 사용자가 고른 URI만 읽고, 디코딩에 bounds 확인·`inSampleSize`·최대 크기(예: 마스킹 테이프 512)를 사용 → **양호**.
- 로그: `Log.w/e` 13곳. 엽서 ID·앱 내부 파일 경로·예외 메시지만 있고 글귀·편지 본문은 없어. Android 4.1+ 다른 앱은 logcat을 못 읽고 adb 접근이 필요 → **낮음**.
- 백업: `allowBackup="true"`, 백업 규칙 파일 없음 → 기기 백업/기기 간 이전에 엽서 DB·사진이 포함될 수 있어. 개인정보 측면의 노출면이지만 데이터 보존(과거 삭제 사고 이력)에 도움이 되는 정책이라 **사용자 판단 항목**으로만 남기고 바꾸지 않았어.
- release: `isMinifyEnabled=false`, 디버그 플래그 강제 없음. 하드코딩된 키·토큰·비밀번호 검색 0건(변수명 오탐만) → **양호**.
- Room: 앱 내부 저장소 DB, ContentProvider 노출 없음 → **양호**.

## 6. GitHub·공급망

- 민감정보 훅: pre-commit·pre-push 실제 실행 확인(`git-hooks-check.sh --fix`, 설정 변경 없음).
- CI 권한: 최상위 `contents/pull-requests/checks/actions: read`만, 저장소 기본 workflow 권한 read, PR 승인 권한 false. 트리거는 `pull_request`(fork PR에 secret 없음)이고 `pull_request_target` 미사용 → **양호**.
- 외부 액션: `actions/checkout@v7.0.1`, `actions/setup-java@v6`, `gradle/actions/setup-gradle@v6.4.0` 모두 OSV 탐지 없음. setup-gradle 6.4.1의 보안 수정(GHSA-chpw-2mqm-95cc)은 `dependency-submission` 액션 대상이라 우리 사용 경로와 무관 → 변경 안 함.
- 태그 고정이며 commit SHA 고정은 아니야. SHA 고정은 업데이트 관리 비용이 늘어 효과 대비 보류(후속 후보).
- Dependabot alerts·security updates 비활성, secret scanning·push protection 활성. Dependabot·dependency review·의존성 그래프 제출 도입은 CI·알림 관리 비용이 있어 이번에 도입하지 않고 사용자 판단 후보로 남겨.

## 7. 검증

- **로컬 자동검증 — 실행/통과:** Gradle **9.9.1**(Launcher·Daemon JVM Zulu 17.0.21+9) `testDebugUnitTest assembleDebug assembleDebugAndroidTest --warning-mode all` BUILD SUCCESSFUL. JVM **1295/1295**, 결과 XML 138개, 실패·오류·skip 0. source `@Test` 1295 / 테스트 파일 137. debug APK·테스트 APK 생성. instrumentation 21건/8파일은 컴파일만.
- **회귀 비교:** 같은 작업 대상으로 9.8.2와 9.9.1의 `--dry-run --warning-mode all` 경고를 비교했어. deprecation 경고 7종이 완전히 같아(기존 경고, 신규 0). 앱 의존성 5개 구성(debugRuntime·debugCompile·debugUnitTestRuntime·debugAndroidTestRuntime·kspDebugKotlinProcessor)의 resolved tree는 전후 **완전 동일**. `buildEnvironment` 차이는 Gradle 내장 Kotlin stdlib/reflect 2.4.10→2.4.21뿐이고 KGP 2.3.10·AGP 9.2.1은 그대로야. 변경 후 OSV 재조회도 358개·13건으로 같아.
- **병합 manifest 참고:** 직접 선언은 CAMERA·VIBRATE뿐이지만 라이브러리 병합으로 INTERNET·ACCESS_NETWORK_STATE가 들어가(media3-common, ML Kit 계열 datatransport). 기존부터 있던 상태이고 이번에 바뀌지 않았어. 제거는 ML Kit 동작 영향이 있어 범위 밖.
- **체크섬:** `distributionSha256Sum`이 공식 값과 내려받은 zip 계산값에 일치.
- **SAFE 보호 자료:** 원본 폴더의 `protected-baseline.json` 기준 **38개 모두 SHA-256 일치**, 불일치0·누락0. 원본 폴더 파일에는 쓰지 않았어.
- **실사용 기기 instrumentation·감각 QA — 불필요:** 앱 소스·리소스·의존성 resolve가 바뀌지 않았고 빌드 도구 버전만 바뀌어 자동 검증으로 충분해. 기기 연결·설치·adb 조작 0회.
- **GitHub Actions CI:** 이 문서는 commit 전 작성본이라 미실행. 실제 PR CI 결과(JDK 버전 포함)는 대화 완료보고와 PR에서 확인해.
- TEST-COVERAGE-MAP: 테스트 추가·삭제·의미 변화가 없어 **변경 없음**.

## 8. 남은 위험·후속 후보(승인 필요)

1. Kotlin 2.3.10 → 2.4.2x 업그레이드(별도 작업, 호환성 조사 포함). 현재 노출 경로는 없음.
2. 로컬 Zulu 17.0.21.1(17.70.201) 설치 여부(시스템 JDK 변경, 사용자 승인). CPU 조건상 현재 영향 없음.
3. AGP 빌드 도구 전이 의존성(Bouncy Castle 등)은 다음 AGP 업그레이드 때 재스캔.
4. 앱 백업 범위 정책(`allowBackup`) 결정.
5. Dependabot alerts 활성화·Actions SHA 고정 여부.
