# 133일차 보안 잔여 정리 및 Clone 점검 결과

2026-11-15, 패코(Codex), 수동 표준 모드. 조사 기준은 PR #27 병합 commit `fd2b4a4d8dca8764c29d6e1c8a9242ba48050659`야. 개인 이메일·기기 식별값·개인 경로 원문은 기록하지 않아.

## 1. 시작 Git 및 HANDOFF

- 시작 local main·origin/main·GitHub 실제 main은 위 SHA와 같았어. 추적 파일 변경은 없었고 기존 미추적 보호 항목은 보존했어.
- 기존 HANDOFF 앞부분은 PR #27 병합 전 기록이었어. GitHub API로 병합 완료를 확인했고 main CI run `43709744497`의 필수 job 성공을 확인했어. JVM test·debug APK·instrumentation APK compile이 모두 실제 실행됐어. 기기 instrumentation 실행을 뜻하지 않아.
- 이번 브랜치는 `chore/day133-security-followup`이야. 전역 Git·앱 코드·Room·사용자 데이터·기존 보호 파일은 변경하지 않았어.

## 2. 옛 Codex Clone 두 개

| 구분 | 위치 | 실제 상태 | 논리 파일 크기 |
|---|---|---|---|
| A | `%USERPROFILE%/.codex/.chatgpt-projects/g-p-6a3f956c87a881919ae6c4718ec37a7f/post-card-memory-workflow` | `feature/photo-sticker`, HEAD `4a0f184`, 2026-08-15. 작업트리·미추적·ignored 파일 없음. local main과 origin/main ref는 없음. 캐시된 origin 작업 브랜치보다 독립 commit 1개 있음 | 3,697,964 bytes, 약 3.53 MiB |
| B | `%USERPROFILE%/Documents/Codex/2026-08-15/referenced-chatgpt-conversation-this-is-an/work/post-card-memory-source` | `main`, HEAD `4e3cc86`, 2026-07-02. 당시 origin/main과만 일치하며 최신 원격과 동기화된 상태가 아님. 작업트리·미추적·ignored 파일 없음 | 1,876,743 bytes, 약 1.79 MiB |

- 둘 다 같은 프로젝트 GitHub origin을 갖고 있고 shallow clone·공유 object alternates는 없어. 각 저장소에 별도 worktree는 없어.
- A의 도달 가능한 commit 191개 중 1개가 현재 저장소의 도달 가능한 이력에 없어. B의 190개는 모두 현재 저장소 이력에 포함돼.
- A의 독립 commit은 규칙·공용 작업판 문서 7개 변경(727줄 추가·135줄 삭제)이야. 검사한 diff에서는 알려진 serial·개인 경로·Gmail을 찾지 못했어. 동일한 patch가 다른 형태로 반영됐는지까지 확정하지 않았어.
- 두 폴더 내부에서 별도 백업 파일은 발견하지 못했어. PC 전체 백업 존재를 조사한 것은 아니야.

## 3. 보존·재사용·정리 권고

- A: **보존 필요**. 독립 commit을 검토·보존하기 전 삭제하지 않아.
- B: **조건부 정리 후보**. 고유 commit·파일은 발견되지 않았지만 프로젝트 등록·프로세스 작업 디렉터리·모든 열린 핸들까지 완전히 배제하지 못했으므로 사용자 판단이 필요해.
- 개발 관련 프로세스의 읽을 수 있는 명령줄에는 두 경로 참조가 없었어. 이것을 사용 중이 아니라는 완전한 증명으로 보지 않아.
- 두 폴더를 합쳐도 약 5.32 MiB야. 공간 확보보다 오사용 방지가 주된 의미야. 즉시 삭제·이동·업데이트·push는 하지 않았어.

## 4. 훅 활성 및 보호 누락

- 공용 작업 폴더는 local `core.hooksPath=.githooks`. `git-hooks-check.sh --fix`로 Git의 pre-commit·pre-push 실제 실행을 확인했어. 이미 활성이라 시작 점검은 설정을 바꾸지 않았어.
- A·B는 훅 파일과 hooksPath가 없어서 보호가 빠져 있어. 재사용 대상으로 정하지 않았으므로 임의로 최신화하거나 훅을 설치하지 않았어.
- 소유자가 달라 처음 Git 조회가 막혔지만 확인된 두 경로에만 명령 단위 `safe.directory`를 적용해 읽었어. 전역 예외는 저장하지 않았어.
- 새 clone·옛 checkout·훅 우회·웹 작업의 한계는 남아. PR CI만으로 브랜치 push 시점의 공개를 되돌릴 수 없어.

## 5. Gmail 메타데이터 노출

- local refs 전체 도달 가능 commit **458개** 중 개인 Gmail을 가진 commit **42개**. Author 42개, Committer 16개(중복 포함), 둘 중 하나가 개인 메일인 merge commit 26개야. 실제 개인 이메일 종류는 1개야.
- PR #27 merge commit은 Author에 개인 메일, Committer에는 GitHub noreply가 있었어.
- GitHub가 광고하는 PR head 참조 27개 중 26개는 로컬 조사 이력에 포함돼. 별도 PR #1 head commit `9ccb269`는 로컬에 없고, API로 개인 Author·Committer를 확인했어. 따라서 확인된 공개 commit의 합집합에서는 **최소 43개**야.
- A에는 같은 과거 Gmail 이력을 가진 commit 16개가 있어. 별개의 새 공개 노출 수로 중복 합산하지 않아.

## 6. 향후 이메일 보호

- 사용자가 GitHub의 `Keep my email addresses private`와 `Block command line pushes that expose my email`을 모두 ON으로 변경했다고 확인했어. 설정 화면을 작업자가 직접 읽은 것은 아니야. API에는 해당 설정을 읽을 권한이 없었고 브라우저 연결도 사용할 수 없었어.
- 실제 GitHub commit `e87798a`의 noreply Author 주소를 읽어 현재 인증 계정의 id·login 및 GitHub의 Author 계정 연결과 대조했어. 주소를 추측·생성하지 않았어.
- 승인 후 **프로젝트 로컬** `user.email`에 그 주소를 설정했어. 기존 로컬 값도 같았어. 실제 `git var GIT_AUTHOR_IDENT`·`GIT_COMMITTER_IDENT`가 그 주소와 일치했고 이메일 환경 변수 override도 없었어. 전역 설정은 변경하지 않았어. 공용 폴더를 쓰는 피코·패코에 함께 적용돼.
- [GitHub 공식 설명](https://docs.github.com/en/account-and-profile/how-tos/email-preferences/setting-your-commit-email-address)에 따르면 이메일 비공개를 켜면 웹 Git 작업의 Author는 기본 noreply를 써. 새 웹 병합으로 직접 시험하지는 않았어.
- [명령줄 push 차단](https://docs.github.com/en/account-and-profile/how-tos/email-preferences/blocking-command-line-pushes-that-expose-your-personal-email-address)은 최근 commit의 Author 중심이므로 중간 commit·Committer까지 다루는 로컬 검사를 함께 유지해. 설정 변경은 과거 commit을 고치지 않아.

## 7. 과거 serial 재확인

- 알려진 serial 1개가 포함된 commit 트리 **136개**, 최초 유입 `244009b`(2026-09-07)를 재확인했어.
- 과거 해당 파일 경로는 HANDOFF·111일차 달력 감사·보존 HANDOFF 총 3개야. PR #25·#26 제공 patch에도 남아 있어. 현재 main 추적 파일에는 검출되지 않았어.

## 8. 개인 경로 및 추가 인증정보

- 개인 경로가 포함된 commit 트리 145개, 파일 경로 4개, 서로 다른 파일·줄 내용 및 추가 이력 기준 **11곳**을 확인했어. 이전 10곳 보고와 기준·범위가 달라 이번 수치로 구분해 남겨.
- 도달 가능한 object 7,093개에서 텍스트 이메일·알려진 토큰 형식·개인키 헤더·기존 검사와 같은 장문 비밀 대입 후보를 검사했어. 파일 본문의 개인 이메일·인증정보 후보는 검출되지 않았어.
- 바이너리·이미지 내 개인정보, 알려지지 않은 비밀 형식, 도달 불가능 object·외부 복제본은 미검증이야. 자격증명 유출이 전혀 없다는 보장은 아니야.

## 9. 격리 재작성 시뮬레이션

- 원본 object는 읽기만 했어. 메모리에서 알려진 serial·개인 경로·개인 이메일을 치환한 뒤 원격·alternates가 없는 새 TEMP bare 저장소에 익명화된 object만 기록했어. 이메일은 시뮬레이션용 example 주소로 바꿨어.
- 로컬 도달 가능 commit 458개, 부모 연결 비교 458개, 파일 snapshot 비교 84,416개를 확인했어. 이 중 앱 파일 비교 73,369개는 모두 원본과 byte 단위로 같았어. 변경된 파일 내용은 과거 문서 경로 4개에 한정됐어.
- 치환으로 blob 94개·tree 418개·commit 458개·annotated tag 1개의 object id가 바뀌었고 commit 서명 28개를 제거해야 했어. `git fsck --full --strict` 통과, 저장된 object에서 대상 serial·개인 이메일 재검출 0건이야.
- 익명화 저장소를 별도 mirror로 복제해 refs 23개 일치와 fsck를 확인했어. clone 2개와 PR #1 고유 이력은 이 시뮬레이션에 포함하지 않았어.
- 첫 복원 실험은 mirror에서 `remote remove`가 원격 추적 refs도 제거해 refs 비교에 실패했어. 원본에는 영향 없었어. 새 익명화 복제본에서 remote 설정만 제거하는 방식으로 고쳐 refs 일치까지 통과했어.
- `git-filter-repo`는 설치되지 않았어. 이번 검증은 Git object 재구성 시뮬레이션이며 실제 배포용 filter-repo 명령의 실행 검증이나 민감 원본 백업 복원 시험을 대신하지 않아.

## 10. 실제 변경 필요 범위

- 개인 이메일이 최초 commit `8ab6674`부터 있어. 전체 제거 시 조사한 458개 SHA가 모두 바뀌어. serial만 제거하는 경우보다 범위가 넓어.
- 조사 당시 원격 일반 브랜치 10개와 annotated tag 1개가 있어. 현재 tip 파일이 깨끗해도 조상에 개인정보가 남아 있으므로 전체 참조를 함께 검토해야 해. 릴리스는 없었어.
- 로컬 후속 작업 브랜치와 이후 신규 commit도 실제 실행 시 재집계해야 해. PR #1 전용 이력과 A 독립 commit의 보존 방법도 별도로 확정해야 해.

## 11. GitHub PR·캐시·지원

- 공개 PR 27개(당시 열린 PR 없음)의 본문과 저장소 issue 댓글 1개·review 댓글 0개에서 대상 값의 재인용을 찾지 못했어.
- 제공된 파일 patch에서는 PR #25·#26 serial을 확인했어. PR #4·#18에는 patch가 없는 파일이 있어 전체 diff 완전 검사를 주장하지 않아. 과거 Actions 로그·artifact 전체도 조사하지 않았어.
- [GitHub 공식 제거 절차](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository)에 따르면 force push 뒤에도 PR 내부 참조·캐시·fork·외부 clone이 남을 수 있어. 지원팀의 정리 가능 여부는 심사 대상이고 지원을 보장할 수 없어. PR diff 접근·검토 문맥이 사라질 수도 있어.
- 지원 요청 초안: “이 저장소의 과거 commit metadata에 개인 이메일, 문서 이력에 기기 식별값과 개인 경로가 공개됐어. 승인된 이력 정리 이후 PR 내부 참조와 cached views 정리 가능 여부를 검토해 줘. 실제 실행 후 확정한 first changed commits·영향 PR 수·fork 상태를 첨부할게. PR 본문·대화 보존이 가능한 dereferencing도 검토해 줘.” 실제 식별값·백업은 첨부하지 않아. 아직 요청을 보내지 않았어.

## 12. 위험·복구 계획

- `Protect main`은 active이고 삭제·non-fast-forward 금지·PR·필수 status check 규칙이 있어. 우회 actor는 없어. 이번에 변경하지 않았어.
- 실제 재작성 전에 개발·병합을 잠시 멈추고 정확한 refs 및 모든 clone/worktree를 동결해 확인해야 해. 민감한 원본 백업은 동기화·공개 업로드가 없는 사용자 승인 위치와 접근 제한·암호화 방법을 먼저 정한 뒤, 별도 환경 복원과 refs/object 무결성을 확인해야 해. 아직 그런 백업은 만들지 않았어.
- 원격 갱신 대상·허용 방식·실패 시 중단 기준·보호 규칙 복원·기존 clone 재사용 금지/재구성·독립 작업 보존까지 구체적으로 승인받아야 해. 오래된 이력을 merge/push하면 개인정보가 되살아날 수 있어.
- 백업으로 Git 상태를 복원할 수 있어도 GitHub PR 연결·서명·캐시·외부 복제본까지 원상복구되거나 완전 삭제되는 것은 아니야.

## 13. 수정 파일 및 검증

- `sensitive-info-scan.sh`: 기존 파일 검사에 Author·Committer 검사만 더했어. staged 검사는 실제 identity, push·PR 범위 검사는 빈 commit·merge를 포함해 확인해. identity에는 파일명·Kotlin 라벨 예외를 적용하지 않아. 값 대신 역할·위치만 출력해.
- `sensitive-info-scan-test.sh`: 기존 41개를 보존하고 9개를 추가했어. 같은 작업 세션에서 **50/50 통과, exit 0**. `--author`·환경 변수·빈 변경·중간 commit·merge·실제 pre-push 차단·원격 ref 미생성·가상 이메일 미출력을 확인했어. 재개 후 로직 변경 없이 이 결과를 재사용했어.
- 로컬 자동검증: 실행(셸 50개·문법·diff·시뮬레이션). 앱 JVM·APK 로컬 재빌드: 불필요(앱 변경 없음). 실사용 기기 instrumentation·감각 QA: 불필요, 실행 안 함. TEST-COVERAGE-MAP의 앱 테스트 수·보호 범위는 바뀌지 않아 수정하지 않아.
- CI는 기존 workflow를 유지해. PR은 기준 main의 검사기를 쓰므로 이 PR 자체 CI에는 이전 파일 검사기가 적용되고, 새 메타데이터 검사는 로컬 검증·훅에서 먼저 적용돼. 병합 후 후속 PR부터 기준 main에서도 적용돼. 이번 PR은 `.github` 변경으로 전체 빌드 대상이야.

## 14. 원본·캐시 정합성

- 작업 루틴 설치 원본 `%USERPROFILE%/plugins/post-card-memory-workflow`와 설치 캐시는 각각 19파일, 상대경로·SHA-256 전체 일치였어. 둘 다 수정하지 않았어. 공통 실행 규칙은 AGENTS와 기존 Codex 실행 규칙을 따랐어.

## 15. repository HANDOFF

- 이번 결과·미검증·승인 경계와 commit 전 상태를 HANDOFF 앞에 추가했어. 기존 사고·과거 기록은 보존해. 실제 PR 번호·SHA·CI 마감은 대화 완료보고에 남겨.

## 16. Git 마감

- 이 문서는 commit 전 작성본이야. 승인된 두 검사 파일과 보고서·HANDOFF만 명시적으로 stage해 commit·작업 브랜치 push·PR·필수 CI를 진행해. 실제 main 병합은 사용자 최종 승인 전 금지야.

## 17. 별도 승인 필요

- main 병합은 최종 PR head·base·CI를 검토한 뒤 별도 승인.
- 옛 Clone 삭제·파일 이동·독립 commit 처리, 민감 원본 백업 위치와 보호 방법, 실제 이력 재작성·원격 강제 갱신·보호 규칙 변경은 별도 승인.
- GitHub 지원 요청 발송·저장소 공개 범위 변경도 별도 승인. 이번 지시는 어느 항목도 승인하지 않았어.

## 18. 남은 미검증 및 의미

- 계정 설정 ON은 사용자 확인 근거야. 새 웹 병합 이메일은 아직 생성·검증하지 않았어.
- PR #1 전용 트리 전체·외부 fork/clone·GitHub 내부 캐시·과거 Actions 로그/산출물 전체·모든 프로세스 열린 핸들·민감 원본 백업 복원은 미검증이야.
- **완료:** 조사·로컬 이메일 보호 설정·보안 검사 보완 및 50개 검증. **안전하게 준비:** 익명화 시뮬레이션·복구 및 지원 요청 계획. **승인 대기:** main 병합과 실제 과거 이력 정리·Clone 삭제.
- 앱 동작과 사용자 엽서 데이터는 그대로야. 이번 보완은 앞으로 새 commit에서 개인 이메일이 다시 공개될 위험을 줄이며, 과거 노출 43개를 삭제한 작업은 아니야.
