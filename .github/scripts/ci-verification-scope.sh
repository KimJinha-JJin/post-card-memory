#!/usr/bin/env bash
# Android CI 검증 범위 판정.
#
# 출력(GITHUB_OUTPUT): full=true|false, mode=full|docs-only|verified-merge|manual
# 원칙(fail-closed): 아래 조건을 끝까지 "확실히" 만족할 때만 full=false를 낸다. 명령 실패,
# API 오류, 빈 결과, 예상 밖 형태는 모두 전체 검증(full=true)으로 떨어진다.
#
# 입력 환경변수
#   EVENT_NAME      github.event_name (pull_request | push | workflow_dispatch …)
#   REPO            owner/name
#   DEFAULT_BRANCH  기본 브랜치 이름(main)
#   REF_NAME        github.ref_name
#   BEFORE_SHA      push 이벤트의 github.event.before
#   WORKFLOW_PATH   이 워크플로 파일 경로(.github/workflows/android-ci.yml)
#   TARGET_SHA      판정할 commit(기본 HEAD, 로컬 시나리오 검증용)
#   GH_TOKEN        gh api 인증(CI에서는 github.token, 읽기 권한만 필요)
set -uo pipefail

target="${TARGET_SHA:-HEAD}"
full=true
mode=full
notes=()

note() { notes+=("$1"); echo "[scope] $1"; }

emit() {
  if [ -n "${GITHUB_OUTPUT:-}" ]; then
    { echo "full=$full"; echo "mode=$mode"; } >> "$GITHUB_OUTPUT"
  fi
  if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
      echo "### 검증 범위: \`$mode\` (full=$full)"
      for n in "${notes[@]}"; do echo "- $n"; done
    } >> "$GITHUB_STEP_SUMMARY"
  fi
  echo "full=$full"
  echo "mode=$mode"
  exit 0
}

# 빌드에 참여하지 않는 Markdown만 문서로 본다. docs/ 아래 HTML·이미지 등은 문서로 보지 않는다.
DOCS_ONLY_PATTERN='^(README\.md|AGENTS\.md|CLAUDE\.md|docs/.+\.md)$'
# Gradle 캐시 키·빌드 환경에 영향을 주는 파일. 바뀌면 main에서 전체 빌드로 캐시를 갱신한다.
BUILD_INPUT_PATTERN='(^|/)[^/]*\.gradle(\.kts)?$|(^|/)gradle\.properties$|^gradle/|^\.github/'

# $1..$target 사이 변경 파일(이름 변경은 옛 경로·새 경로 모두). 실패하거나 비어 있으면 1.
changed_files() {
  local files
  files=$(git diff --name-only --no-renames "$1" "$target") || return 1
  [ -n "$files" ] || return 1
  printf '%s\n' "$files"
}

is_docs_only() {
  local files
  files=$(changed_files "$1") || { note "변경 파일 목록을 확정하지 못함"; return 1; }
  note "변경 파일 $(printf '%s\n' "$files" | wc -l | tr -d ' ')개"
  if printf '%s\n' "$files" | grep -qvE "$DOCS_ONLY_PATTERN"; then
    note "문서 외 파일 포함: $(printf '%s\n' "$files" | grep -vE "$DOCS_ONLY_PATTERN" | head -5 | tr '\n' ' ')"
    return 1
  fi
  return 0
}

# main에 들어온 병합 commit이 이미 성공한 PR 전체 검증과 같은 결과임을 하나씩 확인한다.
is_verified_merge() {
  local parents p1 p2 files pr check_id job run
  parents=$(git rev-list --parents -n 1 "$target") || { note "부모 commit 조회 실패"; return 1; }
  set -- $parents
  [ "$#" -eq 3 ] || { note "병합 commit이 아님(부모 $(( $# - 1 ))개)"; return 1; }
  local head=$1; p1=$2; p2=$3

  [ "$p1" = "${BEFORE_SHA:-}" ] || { note "push 직전 main(${BEFORE_SHA:-없음})과 첫 부모가 다름"; return 1; }

  [ "$(git rev-parse "$head^{tree}")" = "$(git rev-parse "$p2^{tree}")" ] \
    || { note "병합 결과 파일 상태가 PR head와 다름"; return 1; }

  files=$(changed_files "$p1") || { note "변경 파일 목록을 확정하지 못함"; return 1; }
  if printf '%s\n' "$files" | grep -qE "$BUILD_INPUT_PATTERN"; then
    note "빌드·CI 설정 파일 변경 포함 — 전체 빌드로 캐시 갱신"
    return 1
  fi

  # PR head가 병합 직전 main을 포함해야 PR CI의 검증 대상(main+head)과 병합 결과가 같다.
  [ "$(gh api "repos/$REPO/compare/$p1...$p2" --jq '.behind_by' 2>/dev/null)" = "0" ] \
    || { note "PR head가 병합 직전 main을 포함하는지 확인 실패"; return 1; }

  pr=$(gh api "repos/$REPO/commits/$head/pulls" --jq "[.[] | select(.merged_at != null
        and .merge_commit_sha == \"$head\" and .head.sha == \"$p2\"
        and .base.ref == \"$DEFAULT_BRANCH\") | .number] | if length == 1 then .[0] else empty end" 2>/dev/null)
  [ -n "$pr" ] || { note "이 병합 commit을 만든 PR을 하나로 특정하지 못함"; return 1; }
  note "병합 PR #$pr, head ${p2:0:7}"

  check_id=$(gh api "repos/$REPO/commits/$p2/check-runs?check_name=build-and-test&filter=latest" \
    --jq '[.check_runs[] | select(.app.slug == "github-actions")] |
          if length == 1 and .[0].conclusion == "success" then .[0].id else empty end' 2>/dev/null)
  [ -n "$check_id" ] || { note "PR head의 최신 build-and-test 성공을 확인하지 못함"; return 1; }

  job=$(gh api "repos/$REPO/actions/jobs/$check_id" --jq "select(.conclusion == \"success\"
        and .head_sha == \"$p2\"
        and ([.steps[] | select(.conclusion == \"success\") | .name] as \$ok
             | [\"Run JVM unit tests\", \"Assemble debug APK\",
                \"Compile instrumentation tests (assembleDebugAndroidTest)\"]
             | all(. as \$s | \$ok | index(\$s)))) | .run_id" 2>/dev/null)
  [ -n "$job" ] || { note "PR CI가 전체 빌드 3단계를 실제로 성공했는지 확인하지 못함"; return 1; }

  run=$(gh api "repos/$REPO/actions/runs/$job" --jq "select(.event == \"pull_request\"
        and .head_sha == \"$p2\" and .path == \"$WORKFLOW_PATH\"
        and .conclusion == \"success\") | .id" 2>/dev/null)
  [ -n "$run" ] || { note "PR CI 실행(run $job)의 이벤트·워크플로·head가 맞지 않음"; return 1; }
  note "검증된 PR CI run $run (job $check_id)"

  # PR CI는 캐시를 읽기만 한다. main 캐시가 없으면 전체 빌드로 다시 채운다.
  [ "$(gh api "repos/$REPO/actions/caches?ref=refs/heads/$DEFAULT_BRANCH&key=setup-java-Linux-x64-gradle-" \
        --jq '.total_count' 2>/dev/null)" -ge 1 ] 2>/dev/null \
    || { note "main Gradle 캐시가 없거나 확인 실패 — 전체 빌드로 캐시 저장"; return 1; }

  return 0
}

case "${EVENT_NAME:-}" in
  pull_request)
    # PR CI는 refs/pull/N/merge를 검증한다: 첫 부모 = 기준 main, 두 번째 부모 = PR head.
    if [ "$(git rev-list --parents -n 1 "$target" 2>/dev/null | wc -w)" -eq 3 ] \
       && is_docs_only "$target^1"; then
      full=false; mode=docs-only; note "Markdown 문서만 변경 — 빌드 단계 생략"
    fi
    ;;
  push)
    if [ "${REF_NAME:-}" != "${DEFAULT_BRANCH:-}" ]; then
      note "기본 브랜치가 아닌 push"
    elif [ "$(git rev-parse "$target^1" 2>/dev/null)" != "${BEFORE_SHA:-}" ]; then
      note "한 번에 여러 commit이 들어온 push로 보임 — 전체 검증"
    elif is_docs_only "$target^1"; then
      full=false; mode=docs-only; note "Markdown 문서만 변경 — 빌드 단계 생략"
    elif is_verified_merge; then
      full=false; mode=verified-merge; note "이미 성공한 PR 전체 검증과 같은 결과 — 빌드 단계 생략"
    fi
    ;;
  workflow_dispatch)
    mode=manual; note "수동 실행 — 항상 전체 검증"
    ;;
  *)
    note "알 수 없는 이벤트(${EVENT_NAME:-없음})"
    ;;
esac

[ "$full" = true ] && note "전체 검증 실행"
emit
