#!/usr/bin/env bash
# 공개 저장소에 새로 들어가는 줄과 Git Author/Committer 이메일을 검사한다.
#
# 사용:
#   sensitive-info-scan.sh --staged                 commit 직전 stage된 변경(pre-commit 훅)
#   sensitive-info-scan.sh --unpushed <sha>         원격 어디에도 없는 <sha>까지의 commit(pre-push 훅)
#   sensitive-info-scan.sh --range <base> <head>    <base>에 없는 <head>까지의 commit(CI의 PR 검사)
#
# 원칙
#   - 추가된 줄(+)만 본다. 삭제·마스킹 변경은 신규 유출로 보지 않는다.
#   - commit 단위로 본다. 중간 commit에 들어갔다 지워진 값도 공개 이력에 남기 때문이다.
#     파일 검사는 병합 commit을 건너뛴다(main에서 들어온 내용은 이미 공개 상태).
#     Author/Committer 검사는 빈 commit·병합 commit을 포함한다.
#   - 감지한 실제 값은 어디에도 출력하지 않는다. 종류·파일·줄 번호만 알린다.
#   - 실제 식별값을 이 스크립트에 하드코딩하지 않는다. 형식과 문맥으로만 판정한다.
#
# 종료 코드: 0 = 통과, 1 = 민감정보 감지(차단), 2 = 검사 실패(fail-closed, 차단으로 취급)
set -u
export LC_ALL=C

die() { echo "민감정보 검사 실패: $*" >&2; exit 2; }

mode=${1:-}
case "$mode" in
  --staged) ;;
  --unpushed) [ $# -eq 2 ] || die "--unpushed <sha> 형식이 필요함" ;;
  --range) [ $# -eq 3 ] || die "--range <base> <head> 형식이 필요함" ;;
  *) die "알 수 없는 모드(--staged | --unpushed <sha> | --range <base> <head>)" ;;
esac

git rev-parse --git-dir >/dev/null 2>&1 || die "Git 저장소가 아님"

DIFF_OPTS=(-c core.quotePath=false)
SHOW_ARGS=(--no-color --no-ext-diff --no-textconv -U0 --format=)

# 줄 형식: <위치표시>\t<파일>\t<줄번호>\t<내용>
extract_added() {
  awk -v where="$1" '
    /^diff --git / { inhdr = 1; file = ""; next }
    inhdr && /^\+\+\+ / {
      f = substr($0, 5)
      if (f == "/dev/null") { file = "" } else { sub(/^b\//, "", f); file = f }
      next
    }
    /^@@/ {
      inhdr = 0
      s = $0; sub(/^@@ -[0-9,]+ \+/, "", s); sub(/[ ,].*$/, "", s); n = s + 0
      next
    }
    inhdr { next }
    /^\+/ { if (file != "") printf "%s\t%s\t%d\t%s\n", where, file, n, substr($0, 2); n++; next }
    /^ / { n++ }
  '
}

collect() {
  case "$mode" in
    --staged)
      git "${DIFF_OPTS[@]}" diff --cached "${SHOW_ARGS[@]}" --diff-filter=ACMR \
        | extract_added "staged" || return 1
      ;;
    --unpushed|--range)
      local commits c
      if [ "$mode" = --unpushed ]; then
        commits=$(git rev-list --no-merges "$2" --not --remotes) || return 1
      else
        commits=$(git rev-list --no-merges "$3" --not "$2") || return 1
      fi
      for c in $commits; do
        git "${DIFF_OPTS[@]}" show "${SHOW_ARGS[@]}" --diff-filter=ACMR "$c" \
          | extract_added "commit ${c:0:7}" || return 1
      done
      ;;
  esac
}

added=$(collect "$@") || die "변경 내용을 읽지 못함"

hits=""
report() { # $1=종류 $2=줄(위치\t파일\t줄번호\t내용) — 내용은 버린다.
  local where file line
  IFS=$'\t' read -r where file line _ <<<"$2"
  hits+="민감정보 감지: $1 / $file:$line ($where)"$'\n'
}
content_of() { printf '%s' "${1#*$'\t'*$'\t'*$'\t'}"; }
# 내용 칸(파일 경로 제외)에서 grep 조건에 맞는 줄만 원래 형식으로 돌려준다.
match_rows() {
  local idx
  idx=$(printf '%s\n' "$added" | cut -f4- | grep -n "$@" | cut -d: -f1)
  [ -n "$idx" ] || return 0
  awk 'NR == FNR { want[$1] = 1; next } want[FNR]' <(printf '%s\n' "$idx") <(printf '%s\n' "$added")
}

# 1) 기기 식별정보 — serial·adb -s·ANDROID_SERIAL 문맥 뒤의 값, adb devices 출력 형식, IMEI 문맥의 숫자.
#    모델명(SM-S936N 등, '-' 포함)과 마스킹 표기(<serial>, [비공개], XXXX, ****)는 형식상 걸리지 않는다.
SERIAL_CTX='(serial(no|number)?|adb[[:space:]]+-s|android_serial)[^A-Za-z0-9<[]{0,12}[A-Za-z0-9]{8,20}([^A-Za-z0-9]|$)'
ADB_LIST='^[[:space:]]*[A-Za-z0-9]{8,20}[[:space:]]+(device|unauthorized|offline|recovery|sideload)[[:space:]]+(usb:|product:|model:|transport_id:)'
IMEI_CTX='(imei|meid)[^0-9]{0,12}[0-9]{14,16}([^0-9]|$)'

# 실제 serial처럼 보이는지: 숫자 1개 이상 + (영문 2개 이상 또는 12자 이상 16진수). 순수 숫자·영문만은 제외.
looks_like_serial() {
  local t=$1 d l
  d=${t//[^0-9]/}; l=${t//[^A-Za-z]/}
  [ ${#d} -ge 1 ] || return 1
  [ ${#l} -ge 2 ] && return 0
  [ ${#t} -ge 12 ] && [[ "$t" =~ ^[0-9A-Fa-f]+$ ]] && return 0
  return 1
}

while IFS= read -r row; do
  [ -n "$row" ] || continue
  tokens=$(content_of "$row" | grep -oiE "$SERIAL_CTX" | grep -oE '[A-Za-z0-9]{8,20}([^A-Za-z0-9]|$)' | tr -cd 'A-Za-z0-9\n')
  for t in $tokens; do
    case "$(printf '%s' "$t" | tr 'A-Z' 'a-z')" in serial*|android*) continue ;; esac
    if looks_like_serial "$t"; then report "기기 식별정보(serial)" "$row"; break; fi
  done
done < <(match_rows -iE "$SERIAL_CTX")
while IFS= read -r row; do [ -n "$row" ] && report "기기 식별정보(adb 목록)" "$row"; done < <(match_rows -E "$ADB_LIST")
while IFS= read -r row; do [ -n "$row" ] && report "기기 식별정보(IMEI)" "$row"; done < <(match_rows -iE "$IMEI_CTX")

# 2) 사용자 이름이 들어간 개인 경로. %USERPROFILE%·~·<user> 같은 치환 표기와 공용 폴더는 허용한다.
USER_PATH='([A-Za-z]:[\/]+|/mnt/[a-z]/|(^|[^A-Za-z0-9_.])/([a-z]/)?)(users|home)[\/]+[^\/%<>$*{}()[:space:]"'"'"'`]+'
ALLOWED_USERS='^(public|default|default user|all users|runner|user|username|you|me|name|someone|example|ubuntu|vscode|root)$'
while IFS= read -r row; do
  [ -n "$row" ] || continue
  while IFS= read -r m; do
    [ -n "$m" ] || continue
    name=$(printf '%s' "${m##*[\/]}" | tr 'A-Z' 'a-z')
    printf '%s' "$name" | grep -qE "$ALLOWED_USERS" || { report "개인 경로(사용자 이름 포함)" "$row"; break; }
  done < <(content_of "$row" | grep -oiE "$USER_PATH")
done < <(match_rows -iE "$USER_PATH")

# 3) 이메일 주소. GitHub noreply·Anthropic noreply·example 도메인·파일명(@2x.png 등)과
#    Kotlin 라벨 참조(this@drawWithContent.drawContent 등)는 허용한다.
EMAIL='[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}'
EMAIL_OK='(^(this|super|return|break|continue)@|@users\.noreply\.github\.com$|^noreply@github\.com$|^noreply@anthropic\.com$|@example\.(com|org|net)$|^git@github\.com$|\.(png|jpe?g|webp|gif|svg|xml|kt|kts|md|json|html|css|js)$)'
while IFS= read -r row; do
  [ -n "$row" ] || continue
  while IFS= read -r m; do
    [ -n "$m" ] || continue
    printf '%s' "$m" | tr 'A-Z' 'a-z' | grep -qE "$EMAIL_OK" || { report "이메일 주소" "$row"; break; }
  done < <(content_of "$row" | grep -oE "$EMAIL")
done < <(match_rows -E "$EMAIL")

# 4) 인증정보·비밀키. 알려진 토큰 접두 형식, 개인키 헤더, 비밀 값 대입(16자 이상, ${{ }}·$VAR·<...> 제외).
TOKEN_FMT='(^|[^A-Za-z0-9_])(gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,}|sk-(ant-|proj-)?[A-Za-z0-9_-]{32,}|AKIA[0-9A-Z]{16}|AIza[0-9A-Za-z_-]{35}|xox[abprs]-[A-Za-z0-9-]{10,}|glpat-[A-Za-z0-9_-]{20,})'
PRIVATE_KEY='-----BEGIN[A-Z ]*PRIVATE KEY-----'
SECRET_ASSIGN='(api[_-]?key|secret|access[_-]?token|auth[_-]?token|token|password|passwd)["'"'"']?[[:space:]]*[:=][[:space:]]*["'"'"']?[A-Za-z0-9/+_.=-]{16,}'
while IFS= read -r row; do [ -n "$row" ] && report "인증정보(토큰 형식)" "$row"; done < <(match_rows -E "$TOKEN_FMT")
while IFS= read -r row; do [ -n "$row" ] && report "비밀키(개인키 블록)" "$row"; done < <(match_rows -E -e "$PRIVATE_KEY")
# 대입 값은 영문·숫자로 시작하고 영문과 숫자를 함께 가진 16자 이상일 때만 비밀 값으로 본다
# (Kotlin의 `val token = ++hitFlightToken` 같은 코드 식별자는 제외).
while IFS= read -r row; do
  [ -n "$row" ] || continue
  while IFS= read -r m; do
    v=$(printf '%s' "$m" | sed -E 's/^[^:=]*[:=][[:space:]]*["'"'"']?//')
    [[ "$v" =~ ^[A-Za-z0-9] ]] || continue
    [ ${#v} -ge 16 ] && [[ "$v" =~ [0-9] ]] && [[ "$v" =~ [A-Za-z] ]] || continue
    report "인증정보(비밀 값 대입)" "$row"; break
  done < <(content_of "$row" | grep -oiE "$SECRET_ASSIGN")
done < <(match_rows -iE "$SECRET_ASSIGN")

# 파일 변경이 없는 commit과 merge commit도 Author/Committer는 공개된다.
# pre-commit에서는 Git이 정한 실제 identity(--author·환경 변수 포함)를 확인하고,
# push/CI에서는 파일 검사와 달리 merge를 포함한 범위 전체를 확인한다.
check_identity_email() {
  local mail=$1 role=$2 where=$3
  [ -n "$mail" ] || die "Git $role 이메일을 읽지 못함"
  # 파일명·Kotlin 라벨 예외는 identity에 적용하지 않는다.
  if ! printf '%s' "$mail" | tr 'A-Z' 'a-z' | grep -qE '^[a-z0-9._%+-]+@(users\.noreply\.github\.com|example\.(com|org|net))$|^noreply@(github|anthropic)\.com$'; then
    hits+="민감정보 감지: 커밋 메타데이터 이메일($role) / $where"$'\n'
  fi
}
case "$mode" in
  --staged)
    for role in AUTHOR COMMITTER; do
      ident=$(git var "GIT_${role}_IDENT" 2>/dev/null) || die "Git $role identity 확인 실패"
      mail=$(printf '%s' "$ident" | sed -n 's/.*<\([^<>]*\)> [0-9][0-9]* [+-][0-9][0-9][0-9][0-9]$/\1/p')
      check_identity_email "$mail" "$role" "새 commit"
    done
    ;;
  --unpushed|--range)
    if [ "$mode" = --unpushed ]; then
      metadata_commits=$(git rev-list "$2" --not --remotes) || die "메타데이터 범위 확인 실패"
    else
      metadata_commits=$(git rev-list "$3" --not "$2") || die "메타데이터 범위 확인 실패"
    fi
    for c in $metadata_commits; do
      mails=$(git show -s --format='%ae%n%ce' "$c" 2>/dev/null) || die "커밋 메타데이터 읽기 실패"
      check_identity_email "${mails%%$'\n'*}" AUTHOR "commit ${c:0:7}"
      check_identity_email "${mails#*$'\n'}" COMMITTER "commit ${c:0:7}"
    done
    ;;
esac

if [ -n "$hits" ]; then
  printf '%s' "$hits" | sort -u >&2
  echo "→ 위 위치의 값을 마스킹하거나 지운 뒤 다시 진행해. 실제 값은 출력·복사하지 않았어." >&2
  echo "→ 이미 commit된 값이면 이력 재작성이 필요할 수 있으니 force push 없이 멈추고 사용자 승인을 받아." >&2
  exit 1
fi
[ -n "$added" ] || { echo "민감정보 검사: 추가 줄 없음·메타데이터 확인 — 통과"; exit 0; }
echo "민감정보 검사: 추가 줄 $(printf '%s\n' "$added" | wc -l | tr -d ' ')개 — 통과"
exit 0
