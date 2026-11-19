# HANDOFF — 2026-11-19 137일차: 개발 환경·의존성 보안 점검 / Gradle 9.9.1

피코(Claude Code), 수동 표준 모드. 사용자 137일차 작업지시서(보안 조사 → 최소 수정 → 자동 검증 → 문서화 → commit/push/PR, **main 병합 금지**)를 수행했어. 상세 근거는 [137일차 보안 점검 보고서](DAY-137-SECURITY-AUDIT.md), 직전 136일차 HANDOFF 원문은 [archive/HANDOFF-before-day137-2026-11-19.md](archive/HANDOFF-before-day137-2026-11-19.md)에 바이트 그대로 보존했어. 우표 펀칭기 HTML 모션 목업은 138일차 이후로 이월됐어.

## 현재 상태

- 직전 상태: 136일차 PR #30·#31 병합 완료, 시작 시 local main = origin/main = GitHub main `5621236f2e6169500fb2f53fd89c3f7d7d2a8e9e`.
- 구현 **완료(최소 수정)**: Gradle Wrapper·CI `gradle-version` 9.8.2 → **9.9.1**, wrapper에 `distributionSha256Sum` 추가. Gradle 공식 보안 권고 3건(GHSA-3cfg-jpcr-gfp3, GHSA-pcp8-98xm-c6pr, GHSA-gqrr-4j89-5267)의 OSS 수정판이야.
- 앱 소스·리소스·Room·UI·C2 우표 구조·AGP·Kotlin·KSP·라이브러리 버전은 **무변경**. 앱 의존성 resolved tree도 전후 동일해.
- 판정 요약: Gradle 높음→수정 / Java4Shell(CVE-2026-114938) 로컬 낮음(이 PC CPU에 AVX-512 없음), CI는 수정판 JDK 자동 사용 / Kotlin CVE-2026-53914 낮음(수정은 KAPT 캐시 한정, 우리는 KAPT 미사용·build cache 비활성) → 보류 / OSV 13건 모두 낮음(앱 런타임은 Guava 1건, 역직렬화 경로 없음).

## 검증

- **로컬 자동검증 — 실행/통과:** Gradle 9.9.1 + Zulu 17.0.21 `testDebugUnitTest assembleDebug assembleDebugAndroidTest` 성공. JVM **1295/1295**, XML 138, 실패·오류·skip 0. 9.8.2와 deprecation 경고 동일(신규 0). instrumentation 21건/8파일 컴파일만.
- **실사용 기기 instrumentation — 불필요/미실행**, CONDITIONAL·FORBIDDEN은 격리 환경 없어 실행 불가. **실기기 감각 QA — 불필요:** 앱 코드·의존성 resolve 무변경, 빌드 도구만 변경. 기기 조작 0회.
- **GitHub Actions CI — 작성 시점 미실행:** commit 전 작성이야. PR 번호·head SHA·CI 실제 step·CI JDK 버전은 대화 완료보고와 PR 기준으로 확인해.
- SAFE 보호 기준 `protected-baseline.json` **38개 모두 SHA-256 일치**(변경0·누락0). 원본 폴더 파일과 미추적 보호 항목은 건드리지 않았어.
- TEST-COVERAGE-MAP: **변경 없음**(테스트 변화 없음).

## 남은 위험·사용자 판단(승인 전 미실행)

1. Kotlin 2.3.10 → 2.4.2x: 노출 경로는 없지만 버전상 영향. 언어 minor 변경이라 Compose 컴파일러·KSP·Hilt·Room 호환 조사를 포함한 별도 작업 후보.
2. 로컬 Zulu 17.0.21.1(Azul 17.70.201) 설치: 시스템 JDK 변경이라 사용자 승인 필요. 현재 CPU 조건상 영향 없음. 다른 PC(AVX-512 VBMI 지원 CPU)에서 빌드한다면 우선순위가 올라가.
3. AGP 빌드 도구 전이 의존성(Bouncy Castle·jose4j·jdom2·commons-lang3·Guava)의 알려진 취약점은 다음 AGP 업그레이드 때 재스캔.
4. 앱 `allowBackup="true"` 백업 범위 정책, Dependabot alerts 활성화, Actions commit SHA 고정은 사용자 판단 후보. 이번에 바꾸지 않았어.
5. Android Studio는 다음 sync 때 wrapper 설정에 따라 9.9.1 배포판을 내려받고 체크섬을 검증해. 저장소에 `gradlew`가 없는 기존 구조는 그대로야.

## Git 출발점

- 작업 worktree는 개발용 `post-card-memory-next`, 브랜치 `security/day137-dependency-audit`(main `5621236`에서 생성). 원래 작업 폴더(다른 브랜치·미추적 보호 자료)는 읽기 전용으로만 SAFE 해시를 확인했어.
- 훅 실제 실행 점검: pre-commit·pre-push 이미 활성, 설정 변경 없음.
- 변경 파일: `gradle/wrapper/gradle-wrapper.properties`, `.github/workflows/android-ci.yml`, `docs/ai/DAY-137-SECURITY-AUDIT.md`, `docs/ai/HANDOFF.md`, `docs/ai/archive/HANDOFF-before-day137-2026-11-19.md`. 명시적 stage → commit → push → PR → 필수 CI 확인까지가 1단계 승인 범위야.
- **main 병합은 미승인.** 사용자가 PR·head/base SHA·CI를 검토하고 명시 승인하기 전에는 병합·브랜치 정리를 하지 않아. 성공 기록만을 위한 추가 HANDOFF commit은 만들지 않아.
