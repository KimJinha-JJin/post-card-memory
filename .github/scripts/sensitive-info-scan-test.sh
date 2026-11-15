#!/usr/bin/env bash
# sensitive-info-scan.sh와 .githooks의 차단 동작을 임시 저장소에서 가상 값으로 검증한다.
# 실제 기기·계정 값은 쓰지 않는다. 가상 값도 이 파일에 그대로 적지 않고 조각을 이어 만든다
# (이 파일 자체가 검사에 걸리지 않게 하기 위함).
#
# 사용: bash .github/scripts/sensitive-info-scan-test.sh
# 작업 저장소·원격·Git 전역 설정은 건드리지 않는다. 임시 폴더는 끝나면 지운다.
set -u
export LC_ALL=C
src_root=$(cd "$(dirname "$0")/../.." && pwd)
scan="$src_root/.github/scripts/sensitive-info-scan.sh"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

pass=0; fail=0
ok()  { pass=$((pass + 1)); echo "  ok   $1"; }
bad() { fail=$((fail + 1)); echo "  FAIL $1"; }

# 가상 값(조각 연결)
FAKE_SERIAL="ZX9""QW81PLM2"            # 영문+숫자 11자, 실제 기기 아님
FAKE_IMEI="35""0000""11112222""3"       # 15자리 가상 숫자
FAKE_USER="kim""tester"
FAKE_TOKEN="gh""p_""$(printf 'a1B2c3D4%.0s' 1 2 3 4 5)"
FAKE_KEYVAL="Q7w""E8r""T9y""U0i""P1o""A2s""D3"
FAKE_MAIL="kim.tester""@""example-mail.kr"
PK_HEADER="-----BEGIN ""RSA PRIVATE"" KEY-----"

new_repo() {
  rm -rf "$work/r" "$work/remote.git"
  git init -q "$work/r" && cd "$work/r" || exit 2
  git config user.name t; git config user.email t@example.com; git config commit.gpgsign false; git config core.autocrlf false
  mkdir -p docs .github/scripts
  echo "# base" > docs/base.md
  git add docs/base.md && git commit -q -m base
}

# $1=설명 $2=기대(block|pass) $3=파일 내용 — stage만 하고 --staged로 검사
staged_case() {
  local desc=$1 want=$2 out rc
  printf '%s\n' "$3" > docs/case.md
  git add docs/case.md
  out=$(bash "$scan" --staged 2>&1); rc=$?
  git reset -q docs/case.md; rm -f docs/case.md
  leak=0
  for v in "$FAKE_SERIAL" "$FAKE_IMEI" "$FAKE_USER" "$FAKE_TOKEN" "$FAKE_KEYVAL" "$FAKE_MAIL"; do
    printf '%s' "$out" | grep -qF "$v" && leak=1
  done
  [ $leak -eq 0 ] || { bad "$desc — 출력에 가상 값이 드러남"; return; }
  if [ "$want" = block ]; then [ $rc -eq 1 ] && ok "$desc" || bad "$desc (rc=$rc)"
  else [ $rc -eq 0 ] && ok "$desc" || bad "$desc (rc=$rc: $(printf '%s' "$out" | head -1))"; fi
}

echo "[1-6] 단일 파일 stage 검사"
new_repo
staged_case "1 가상 serial 추가 → 탐지" block "- 대상 SM-S936N, serial \`$FAKE_SERIAL\` 한 대."
staged_case "1b adb -s 명령의 serial → 탐지" block "adb -s $FAKE_SERIAL install -r app.apk"
staged_case "1c adb devices 목록 형식 → 탐지" block "$FAKE_SERIAL       device product:pa3q model:SM_S936N transport_id:3"
staged_case "1d IMEI → 탐지" block "IMEI: $FAKE_IMEI"
staged_case "2 모델명만 → 허용" pass "- 대상 SM-S936N, 1080×2340, 450dpi 한 대."
staged_case "2b serial 확인 절차 문장·해상도 → 허용" pass "adb devices -l로 대상 serial 확인 후 1080x2340 화면에서 serial/model 일치"
staged_case "2c serialization 단어 → 허용" pass "kotlinx serializationTest 와 Serializable 2026 확인"
staged_case "3 완전 마스킹 → 허용" pass "- serial \`[승인된 실사용 기기 식별자 비공개]\`, adb -s <serial> install -r <APK>, serial R5CX****"
staged_case "4 사용자 이름 포함 Windows 경로 → 탐지" block "경로 C:\\Users\\$FAKE_USER\\StudioProjects\\app"
staged_case "4b Git Bash 경로 → 탐지" block "cd /c/Users/$FAKE_USER/StudioProjects"
staged_case "4c macOS/Linux 홈 경로 → 탐지" block "open /Users/$FAKE_USER/work and /home/$FAKE_USER/x"
staged_case "5 %USERPROFILE%·~ 치환 → 허용" pass "%USERPROFILE%\\StudioProjects\\app, ~/StudioProjects, C:\\Users\\<user>\\x, /home/runner/work"
staged_case "6 가상 GitHub 토큰 → 탐지" block "GH_TOKEN=$FAKE_TOKEN"
staged_case "6b 비밀 값 대입 → 탐지" block "storePassword = \"$FAKE_KEYVAL\""
staged_case "6b2 Kotlin 코드 대입 → 허용" pass "val token = ++hitFlightToken; val password = passwordFieldState"
staged_case "6c 개인키 블록 → 탐지" block "$PK_HEADER"
staged_case "6d CI 토큰 참조 표현 → 허용" pass "GH_TOKEN: \${{ github.token }}"
staged_case "6e 개인 이메일 → 탐지" block "연락처 $FAKE_MAIL"
staged_case "6f noreply·example 메일, @2x 파일명 → 허용" pass "Co-Authored-By: Claude <noreply@anthropic.com> t@example.com icon@2x.png"
staged_case "6g Kotlin 라벨 참조 → 허용" pass "this@drawWithContent.drawContent() super@Outer.call()"

echo "[7] 기존 노출을 지우거나 마스킹하는 변경"
new_repo
printf 'serial `%s` 한 대\n' "$FAKE_SERIAL" > docs/old.md
git add docs/old.md && git commit -q -m "old leak (setup)"
leak_commit=$(git rev-parse HEAD)
printf 'serial `[비공개]` 한 대\n' > docs/old.md
git add docs/old.md
bash "$scan" --staged >/dev/null 2>&1 && ok "7 마스킹 변경 stage → 통과" || bad "7 마스킹 변경이 신규 유출로 오판됨"
git commit -q -m mask
bash "$scan" --range "$leak_commit" HEAD >/dev/null 2>&1 && ok "7b 마스킹 commit만 범위 검사 → 통과" || bad "7b 범위 검사 오판"
git rm -q docs/old.md
bash "$scan" --staged >/dev/null 2>&1 && ok "7c 파일 삭제 stage → 통과" || bad "7c 삭제가 오판됨"
git reset -q --hard HEAD

echo "[8] 여러 파일 동시 stage"
new_repo
echo "모델 SM-S936N" > docs/a.md
echo "일반 문장" > app.txt
printf 'adb -s %s shell\n' "$FAKE_SERIAL" > docs/c.md
git add docs/a.md app.txt docs/c.md
out=$(bash "$scan" --staged 2>&1); rc=$?
if [ $rc -eq 1 ] && printf '%s' "$out" | grep -q 'docs/c.md:1' && ! printf '%s' "$out" | grep -q 'docs/a.md'; then
  ok "8 세 파일 중 세 번째만 정확히 탐지"
else bad "8 다중 파일 검사(rc=$rc)"; fi

echo "[9] 훅 실제 차단과 훅 우회 시 원격(CI) 검사"
new_repo
mkdir -p .githooks
cp "$src_root/.githooks/pre-commit" "$src_root/.githooks/pre-push" .githooks/
cp "$scan" .github/scripts/
git add .githooks .github/scripts && git commit -q --no-verify -m "hooks"
git config core.hooksPath .githooks
base=$(git rev-parse HEAD)
printf 'serial `%s`\n' "$FAKE_SERIAL" > docs/leak.md
git add docs/leak.md
if git commit -q -m leak >/dev/null 2>&1; then bad "9 pre-commit 훅이 commit을 막지 못함"; else ok "9 pre-commit 훅이 commit 차단"; fi
git commit -q --no-verify -m "leak via --no-verify"
git init -q --bare "$work/remote.git"; git remote add origin "$work/remote.git"
if git push -q origin HEAD:refs/heads/work >/dev/null 2>&1; then bad "9b pre-push 훅이 push를 막지 못함"
else ok "9b pre-push 훅이 push 차단"; fi
git push -q --no-verify origin HEAD:refs/heads/work >/dev/null 2>&1
bash "$scan" --range "$base" HEAD >/dev/null 2>&1; rc=$?
[ $rc -eq 1 ] && ok "9c 두 훅을 모두 우회해도 CI 범위 검사가 탐지" || bad "9c CI 범위 검사(rc=$rc)"
printf 'serial `[비공개]`\n' > docs/leak.md; git add docs/leak.md; git commit -q --no-verify -m fix
bash "$scan" --range "$base" HEAD >/dev/null 2>&1; rc=$?
[ $rc -eq 1 ] && ok "9d 뒤 commit에서 고쳐도 중간 commit 노출은 계속 탐지" || bad "9d 중간 commit(rc=$rc)"

echo "[10] 검사 실패 시 fail-closed"
bash "$scan" --range does-not-exist HEAD >/dev/null 2>&1; rc=$?
[ $rc -eq 2 ] && ok "10 잘못된 기준 → 종료 코드 2(차단)" || bad "10 fail-closed(rc=$rc)"

echo "[11-14] 새 clone·worktree에서의 훅 적용과 git-hooks-check.sh"
check="$src_root/.github/scripts/git-hooks-check.sh"
GH_URL="https://github.com/KimJinha-JJin/post-card-memory.git"
bare="$work/remote.git"
# 씨앗 저장소: 훅 도입 전 commit(old) → 훅 도입 commit. 원격은 로컬 bare 저장소뿐이다.
new_repo
old_base=$(git rev-parse HEAD)
mkdir -p .githooks
cp "$src_root/.githooks/pre-commit" "$src_root/.githooks/pre-push" .githooks/
cp "$scan" "$check" .github/scripts/
git add .githooks .github/scripts && git commit -q -m hooks
git init -q --bare "$bare"; git push -q "$bare" HEAD:refs/heads/main
cd "$work" && rm -rf c1 && git clone -q -b main "$bare" c1 && cd c1 || exit 2
git config user.name t; git config user.email t@example.com; git config core.autocrlf false
# 판별용 fetch 주소는 이 프로젝트, push 주소는 로컬 bare. GitHub로는 절대 보내지 않는다.
git remote set-url origin "$GH_URL"; git remote set-url --push origin "$bare"
# (Windows Git Bash는 경로 표기를 바꿔 저장하므로) 네트워크 주소가 아니고 임시 bare 저장소인지로 확인한다.
case "$(git remote get-url --push origin)" in *://*|*@*:*|*github*) echo "push 주소가 원격 서버 — 중단"; exit 2 ;; */remote.git) ;; *) echo "push 주소 확인 실패 — 중단"; exit 2 ;; esac

printf 'serial `%s`\n' "$FAKE_SERIAL" > docs/leak.md; git add docs/leak.md
if git commit -q -m leak >/dev/null 2>&1; then ok "11 새 clone은 훅이 꺼져 있어 commit이 통과함(누락 재현)"; else bad "11 새 clone 기본 동작 예상과 다름"; fi
git reset -q --soft HEAD~1
out=$(bash "$check" 2>&1); rc=$?
[ $rc -eq 1 ] && printf '%s' "$out" | grep -q '로컬 core.hooksPath가 비어' && ok "11b 점검 스크립트가 미설정 원인을 보고" || bad "11b 미설정 진단(rc=$rc)"
out=$(bash "$check" --fix 2>&1); rc=$?
[ $rc -eq 0 ] && [ "$(git config --local --get core.hooksPath)" = .githooks ] && ok "11c --fix가 이 clone 로컬에만 설정 후 실행 확인" || bad "11c --fix(rc=$rc)"
printf '%s' "$out" | grep -qF "$FAKE_SERIAL" && bad "11c 점검 출력에 가상 값 노출" || ok "11d 점검 출력에 stage된 가상 값 미노출"
if git commit -q -m leak >/dev/null 2>&1; then bad "11e 활성화 후에도 commit 통과"; else ok "11e 활성화 후 commit 차단"; fi
git commit -q --no-verify -m "leak via --no-verify"
out=$(git push origin HEAD:refs/heads/w1 2>&1); rc=$?
if [ $rc -ne 0 ] && ! git --git-dir="$bare" rev-parse -q --verify refs/heads/w1 >/dev/null; then ok "11f push 차단, 원격에 브랜치 미생성"; else bad "11f push 차단(rc=$rc)"; fi
printf '%s' "$out" | grep -qF "$FAKE_SERIAL" && bad "11g push 차단 로그에 가상 값 노출" || ok "11g push 차단 로그에 가상 값 미노출"

cfg_before=$(git config --list --local | sort)
bash "$check" --fix >/dev/null 2>&1
[ "$cfg_before" = "$(git config --list --local | sort)" ] && ok "12 이미 활성이면 설정을 다시 쓰지 않음" || bad "12 활성 상태에서 설정 변경됨"

git reset -q --hard HEAD~1
git worktree add -q -b wt-new "$work/wt-new" HEAD >/dev/null 2>&1
cd "$work/wt-new" || exit 2
bash "$check" >/dev/null 2>&1 && ok "13 새 worktree는 공용 설정을 이어받아 활성" || bad "13 새 worktree 활성 확인"
printf 'adb -s %s shell\n' "$FAKE_SERIAL" > docs/leak.md; git add docs/leak.md
if git commit -q -m leak >/dev/null 2>&1; then bad "13b worktree commit 통과"; else ok "13b worktree에서 commit 차단"; fi
git reset -q docs/leak.md; rm -f docs/leak.md
cd "$work/c1" && git worktree add -q --detach "$work/wt-old" "$old_base" >/dev/null 2>&1 && cd "$work/wt-old" || exit 2
out=$(bash "$check" --fix 2>&1); rc=$?
[ $rc -eq 1 ] && printf '%s' "$out" | grep -q '검사 도입 이전' && ok "13c 훅 도입 전 commit의 worktree는 비활성 원인 보고" || bad "13c 옛 base worktree(rc=$rc)"

cd "$work/c1" && git remote set-url origin "https://github.com/someone/other-repo.git"
cfg_before=$(git config --list --local | sort)
git config --local --unset core.hooksPath
bash "$check" --fix >/dev/null 2>&1; rc=$?
[ $rc -eq 1 ] && [ -z "$(git config --local --get core.hooksPath)" ] && ok "14 다른 저장소(origin 불일치)는 설정을 바꾸지 않음" || bad "14 다른 저장소 보호(rc=$rc)"

echo "[15] 메타데이터 보호(가상 이메일·로컬 원격만 사용)"
new_repo
mkdir -p .githooks
cp "$src_root/.githooks/pre-commit" "$src_root/.githooks/pre-push" .githooks/
cp "$scan" .github/scripts/
git config core.hooksPath .githooks
base=$(git rev-parse HEAD)
metadata_case() {
  local desc=$1 want=$2; shift 2
  out=$("$@" 2>&1); rc=$?
  if printf '%s' "$out" | grep -qF "$FAKE_MAIL"; then bad "$desc — 가상 이메일 출력"; return; fi
  if [ "$want" = block ]; then
    [ $rc -ne 0 ] && printf '%s' "$out" | grep -q '커밋 메타데이터 이메일' && ok "$desc" || bad "$desc (rc=$rc)"
  else [ $rc -eq 0 ] && ok "$desc" || bad "$desc (rc=$rc)"; fi
}
metadata_case "15 빈 변경에서도 Author 차단" block env GIT_AUTHOR_EMAIL="$FAKE_MAIL" bash "$scan" --staged
metadata_case "15b 환경 변수 Committer 차단" block env GIT_COMMITTER_EMAIL="$FAKE_MAIL" bash "$scan" --staged
metadata_case "15c 실제 commit --author 차단" block git -c user.email=t@example.com commit --allow-empty -m metadata --author="t <$FAKE_MAIL>"
metadata_case "15d noreply·가상 example identity 허용" pass env GIT_AUTHOR_EMAIL=t@users.noreply.github.com bash "$scan" --staged
# 파일명 예외로 개인 이메일을 우회하지 못한다.
metadata_case "15e identity에 파일 확장자 예외 미적용" block env GIT_AUTHOR_EMAIL="$FAKE_MAIL.kt" bash "$scan" --staged
env GIT_AUTHOR_EMAIL="$FAKE_MAIL" git commit -q --no-verify --allow-empty -m metadata
git commit -q --no-verify --allow-empty -m safe-tip
metadata_case "15f 안전한 tip 뒤 중간 Author를 CI 범위에서 차단" block bash "$scan" --range "$base" HEAD
git init -q --bare "$work/remote.git"; git remote add origin "$work/remote.git"
metadata_case "15g 실제 pre-push가 중간 metadata 차단" block git push origin HEAD:refs/heads/metadata
if git --git-dir="$work/remote.git" rev-parse -q --verify refs/heads/metadata >/dev/null; then bad "15h 차단 후 원격 ref가 생김"; else ok "15h 차단 후 원격 ref 없음"; fi
# 정상 부모 둘을 합친 merge에만 개인 Committer를 넣어 검사한다.
new_repo
base=$(git rev-parse HEAD)
git checkout -q -b side; git commit -q --allow-empty -m side
git checkout -q -b trunk "$base"; git commit -q --allow-empty -m trunk
env GIT_COMMITTER_EMAIL="$FAKE_MAIL" git merge -q --no-ff side -m merge
metadata_case "15i merge Committer도 범위 검사에서 차단" block bash "$scan" --range "$base" HEAD

echo "결과: 통과 $pass / 실패 $fail"
[ $fail -eq 0 ]
