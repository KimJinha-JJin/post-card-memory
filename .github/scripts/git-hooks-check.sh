#!/usr/bin/env bash
# 이 작업 폴더(clone·worktree)에서 민감정보 훅(pre-commit·pre-push)이 Git에 의해 실제로 실행되는지 확인한다.
# 작업 시작 때 한 번 실행한다(AGENTS.md 3절). 이미 활성이면 아무것도 바꾸지 않는다.
#
# 사용: bash .github/scripts/git-hooks-check.sh [--fix]
#   --fix  이 저장소의 로컬 설정에 core.hooksPath가 "비어 있을 때만" .githooks로 설정한다.
#          전역·시스템 설정, 다른 저장소, 사용자가 다른 값으로 정한 로컬 설정은 건드리지 않는다.
#
# 종료 코드: 0 = 활성 확인(또는 --fix로 활성화 후 확인), 1 = 비활성(원인 출력)
# 비활성이면 원인을 고치기 전까지 commit·push 직전에 검사를 직접 실행해야 한다(아래 안내 참고).
set -u

fix=0
[ "${1:-}" = "--fix" ] && fix=1

inactive() {
  echo "훅 비활성: $*"
  echo "→ 고치기 전까지 commit 직전 'bash .github/scripts/sensitive-info-scan.sh --staged',"
  echo "  push 직전 'bash .github/scripts/sensitive-info-scan.sh --unpushed HEAD'를 직접 실행하고 통과를 확인해."
  exit 1
}

top=$(git rev-parse --show-toplevel 2>/dev/null) || inactive "Git 작업 폴더가 아님"
cd "$top" || inactive "작업 폴더로 이동 실패"

# 이 프로젝트의 clone에서만 동작한다(다른 저장소 설정 보호).
url=$(git remote get-url origin 2>/dev/null || true)
case "$url" in
  *[/:]KimJinha-JJin/post-card-memory|*[/:]KimJinha-JJin/post-card-memory.git) ;;
  *) inactive "origin이 post-card-memory 저장소가 아님 — 설정을 바꾸지 않았어" ;;
esac

# 훅 파일은 core.hooksPath(상대 경로)에 따라 "이 worktree의 체크아웃"에서 찾는다.
for f in .githooks/pre-commit .githooks/pre-push .github/scripts/sensitive-info-scan.sh; do
  [ -f "$f" ] || inactive "이 체크아웃에 $f 가 없음 — 검사 도입 이전 commit 기준 브랜치야. 최신 main에서 브랜치를 만들어"
done

local_val=$(git config --local --get core.hooksPath 2>/dev/null || true)
if [ -z "$local_val" ]; then
  if [ $fix -eq 1 ]; then
    git config --local core.hooksPath .githooks || inactive "로컬 설정 쓰기 실패"
    echo "설정: 이 저장소 로컬 core.hooksPath=.githooks (새 clone 등 미설정 상태였음)"
  else
    inactive "이 저장소 로컬 core.hooksPath가 비어 있음(새 clone 등) — --fix로 이 저장소에만 설정 가능"
  fi
elif [ "$local_val" != ".githooks" ]; then
  inactive "로컬 core.hooksPath가 다른 값으로 정해져 있음 — 사용자 설정일 수 있어 바꾸지 않았어"
fi

# worktree 전용 설정 등 더 우선하는 값이 있으면 실제 적용 값이 달라질 수 있다.
effective=$(git config --get core.hooksPath 2>/dev/null || true)
eff_scope=$(git config --show-scope --get core.hooksPath 2>/dev/null | cut -f1)
[ "$effective" = ".githooks" ] || inactive "실제 적용 core.hooksPath가 .githooks가 아님(우선 적용 범위: ${eff_scope:-없음})"

# 파일이 있다는 것과 Git이 실행한다는 것은 다르다. Git이 직접 훅을 찾아 실행하게 해 본다.
# pre-push는 표준 입력이 비면 검사할 commit이 없어 바로 통과한다(부작용 없음).
git hook run pre-push </dev/null >/dev/null 2>&1 \
  || inactive "Git이 pre-push 훅을 찾거나 실행하지 못함(git hook run 실패)"
# pre-commit은 현재 stage를 검사한다. stage에 민감정보가 있으면 실패하지만 그건 훅이 실행된다는 뜻이다.
out=$(git hook run pre-commit 2>&1); rc=$?
if [ $rc -ne 0 ]; then
  if [ $rc -eq 1 ] && printf '%s\n' "$out" | grep -q '^민감정보 감지:'; then
    echo "참고: pre-commit 훅은 실행되며, 지금 stage된 내용에서 민감정보가 감지됐어(값 비출력)."
    printf '%s\n' "$out" | grep '^민감정보 감지:'
  else
    inactive "Git이 pre-commit 훅을 찾거나 실행하지 못함(종료 코드 $rc)"
  fi
fi

echo "훅 활성: pre-commit·pre-push를 Git이 실행함 (core.hooksPath=.githooks, 범위 ${eff_scope:-local})"
exit 0
