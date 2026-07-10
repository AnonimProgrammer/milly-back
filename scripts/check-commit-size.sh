#!/usr/bin/env bash
#
# Check if commits exceed a maximum line-change threshold.
#
# Usage:
#   ./scripts/check-commit-size.sh                    # branch commits vs main
#   ./scripts/check-commit-size.sh --base develop     # compare against develop
#   ./scripts/check-commit-size.sh --limit 500        # custom threshold
#   ./scripts/check-commit-size.sh --range HEAD~10..HEAD
#   ./scripts/check-commit-size.sh --include-merges   # include merge commits
#
# Exit codes:
#   0 — all commits within limit
#   1 — one or more commits exceed the limit
#   2 — usage / runtime error

set -euo pipefail

LIMIT=300
BASE_BRANCH="main"
RANGE=""
INCLUDE_MERGES=false
REPO_ROOT=""

usage() {
  cat <<'EOF'
Usage: check-commit-size.sh [options]

Options:
  --limit N           Max lines changed per commit (default: 300)
  --base BRANCH       Base branch for comparison (default: main)
  --range REV_RANGE   Git revision range, e.g. main..HEAD or HEAD~5..HEAD
  --include-merges    Count merge commits (skipped by default)
  -h, --help          Show this help

Lines changed = insertions + deletions (from git diff --shortstat).
EOF
}

die() {
  echo "error: $*" >&2
  exit 2
}

parse_shortstat() {
  local stat="$1"
  local insertions=0
  local deletions=0

  if [[ "$stat" =~ ([0-9]+)\ insertion ]]; then
    insertions="${BASH_REMATCH[1]}"
  fi
  if [[ "$stat" =~ ([0-9]+)\ deletion ]]; then
    deletions="${BASH_REMATCH[1]}"
  fi

  echo $((insertions + deletions))
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --limit)
      [[ $# -ge 2 ]] || die "--limit requires a number"
      LIMIT="$2"
      shift 2
      ;;
    --base)
      [[ $# -ge 2 ]] || die "--base requires a branch name"
      BASE_BRANCH="$2"
      shift 2
      ;;
    --range)
      [[ $# -ge 2 ]] || die "--range requires a revision range"
      RANGE="$2"
      shift 2
      ;;
    --include-merges)
      INCLUDE_MERGES=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "unknown argument: $1 (use --help)"
      ;;
  esac
done

if ! [[ "$LIMIT" =~ ^[0-9]+$ ]]; then
  die "--limit must be a non-negative integer"
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null)" || die "not inside a git repository"
cd "$REPO_ROOT"

if [[ -z "$RANGE" ]]; then
  git rev-parse --verify "$BASE_BRANCH" >/dev/null 2>&1 || die "base branch '$BASE_BRANCH' not found"
  RANGE="${BASE_BRANCH}..HEAD"
fi

MERGE_FILTER=(--no-merges)
if [[ "$INCLUDE_MERGES" == true ]]; then
  MERGE_FILTER=()
fi

echo "Range: $RANGE"
echo "Limit: $LIMIT lines changed (insertions + deletions)"
echo

violations=0
checked=0
found_commits=false

while IFS= read -r commit; do
  [[ -n "$commit" ]] || continue
  found_commits=true
  shortstat="$(git show --shortstat --format=format: "$commit" | tail -n 1 | sed 's/^[[:space:]]*//')"
  lines=0

  if [[ -n "$shortstat" && "$shortstat" != "" ]]; then
    lines="$(parse_shortstat "$shortstat")"
  fi

  subject="$(git log -1 --format='%s' "$commit")"
  short_hash="$(git rev-parse --short "$commit")"
  checked=$((checked + 1))

  if (( lines > LIMIT )); then
    violations=$((violations + 1))
    printf 'FAIL  %s  %4d lines  %s\n' "$short_hash" "$lines" "$subject"
    if [[ -n "$shortstat" ]]; then
      echo "      $shortstat"
    fi
  else
    printf 'ok    %s  %4d lines  %s\n' "$short_hash" "$lines" "$subject"
  fi
done < <(git rev-list "${MERGE_FILTER[@]}" "$RANGE")

if [[ "$found_commits" == false ]]; then
  echo "No commits to check in range: $RANGE"
  exit 0
fi

echo
echo "Checked $checked commit(s) in range: $RANGE (limit: $LIMIT lines)"
echo
if (( violations > 0 )); then
  echo "$violations of $checked commit(s) exceed ${LIMIT} lines changed."
  exit 1
fi

echo "All $checked commit(s) are within the ${LIMIT}-line limit."
exit 0
