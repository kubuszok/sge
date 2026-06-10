#!/bin/bash
#
# red-green.sh — mandatory resolve gate for SGE bug issues (ISS-487).
#
# Given a module, a test suite, a RED sha and a FIX sha, this script
# proves the red-green contract that the remediation campaign depends on:
#
#   1. The named suite must FAIL at <red-sha> with a TEST ASSERTION
#      failure (sbt exits nonzero AND munit reports >0 failures). A red
#      phase that fails to compile is a FALSE RED — the runner reports it
#      distinctly (compile-error-red) and exits nonzero.
#   2. The named suite must PASS at <fix-sha> (sbt exits 0 AND munit
#      reports 0 failures across ALL suites the run matched).
#   3. ALL test-scope files added or modified by the red commit are
#      tamper-protected: none may change between <red-sha> and <fix-sha>.
#      Any such change is TAMPER — exit nonzero with the diff shown.
#   4. The red commit must NOT touch any main-source file. A red that
#      breaks main code could be "fixed" simply by reverting it, so this
#      is rejected up front.
#
# The suite is run by its EXACT fully-qualified class name(s), resolved
# from the red-test file(s) the red commit added — never by a `*glob*`.
# Glob matching let an unrelated or shadow suite stand in for the real
# red suite; FQCN matching closes that hole.
#
# The check runs in a throwaway git clone so the caller's tree is never
# touched, and the clone is removed on exit (even on failure).
#
# Invocation (via the runner wrapper):
#
#   re-scale runner red-green --mode run -- <module> <suite> <red-sha> <fix-sha>
#
# The runner writes the four arguments into a mode key-value file (the
# path is passed to this script as $1) which we read and then delete.
#
# Direct invocation (for debugging) is also supported:
#
#   .rescale/scripts/red-green.sh --args <module> <suite> <red-sha> <fix-sha>
#
# Permanent canary / probe set (re-run on every audit of this script):
#
#   canary/iss487-pass         base(helper=1)+red(test only)+fix(helper=2)
#                              → RED-GREEN: PASS, exit 0
#   canary/iss487-tamper       fix edits the red-test file
#                              → FAIL tamper, exit 1
#   canary/iss487-helpertamper red adds test AND helper; fix edits helper
#                              → FAIL tamper, exit 1 (full-scope protection)
#   canary/iss487-falsered     test does not compile at red
#                              → FAIL compile-error-red, exit 1
#   canary/iss487-maintouch    red commit changes a main-source file
#                              → FAIL red-touches-main, exit 1
#   probe/iss487-shadow        fix adds a passing shadow suite, red suite
#                              still fails → FAIL, exit 1 (no glob masking)
#   probe/iss487-rename        fix renames+weakens the red-test file
#                              → FAIL tamper, exit 1
#
# Final line is always exactly one of:
#   RED-GREEN: PASS
#   RED-GREEN: FAIL — <reason>
# and the exit code is 0 only on PASS.

set -uo pipefail

# ── locate project root (dir containing .rescale) ───────────────────
PROJECT_ROOT="$(cd "$(pwd)" && while [ ! -d .rescale ] && [ "$PWD" != "/" ]; do cd ..; done; pwd)"
if [ ! -d "$PROJECT_ROOT/.rescale" ]; then
  echo "RED-GREEN: FAIL — could not locate project root (no .rescale dir found)"
  exit 1
fi

# ── argument acquisition ────────────────────────────────────────────
# Two intake paths:
#   * mode-file:   $1 is a path to a `key=value` file the runner wrote.
#   * --args:      the four values are passed literally on the CLI.
MODULE="" ; SUITE="" ; RED="" ; FIX=""

read_mode_file() {
  local f="$1"
  if [ ! -f "$f" ]; then
    echo "RED-GREEN: FAIL — mode file not found: $f (did you pass --mode run -- <module> <suite> <red> <fix>?)"
    exit 1
  fi
  local line key val
  while IFS= read -r line; do
    case "$line" in
      '#'*|'') continue ;;
    esac
    key="${line%%=*}"
    val="${line#*=}"
    case "$key" in
      module) MODULE="$val" ;;
      suite)  SUITE="$val" ;;
      red)    RED="$val" ;;
      fix)    FIX="$val" ;;
    esac
  done < "$f"
  # The runner contract: read on entry, delete on entry.
  rm -f "$f"
}

if [ "${1:-}" = "--args" ]; then
  shift
  MODULE="${1:-}" ; SUITE="${2:-}" ; RED="${3:-}" ; FIX="${4:-}"
elif [ -n "${1:-}" ]; then
  read_mode_file "$1"
else
  echo "RED-GREEN: FAIL — no mode file path and no --args given"
  exit 1
fi

if [ -z "$MODULE" ] || [ -z "$SUITE" ] || [ -z "$RED" ] || [ -z "$FIX" ]; then
  echo "RED-GREEN: FAIL — missing argument (module='$MODULE' suite='$SUITE' red='$RED' fix='$FIX')"
  exit 1
fi

echo "red-green: module=$MODULE suite=$SUITE red=$RED fix=$FIX"

# ── resolve shas to full form, reject unknown refs ──────────────────
RED_FULL="$(git -C "$PROJECT_ROOT" rev-parse --verify "${RED}^{commit}" 2>/dev/null)"
if [ -z "$RED_FULL" ]; then
  echo "RED-GREEN: FAIL — red sha '$RED' is not a valid commit"
  exit 1
fi
FIX_FULL="$(git -C "$PROJECT_ROOT" rev-parse --verify "${FIX}^{commit}" 2>/dev/null)"
if [ -z "$FIX_FULL" ]; then
  echo "RED-GREEN: FAIL — fix sha '$FIX' is not a valid commit"
  exit 1
fi

# ══ GUARD — red commit must not touch main sources ══════════════════
# A red commit that breaks main code could be "fixed" by reverting that
# change, defeating the gate. Reject it before doing any work. We look
# at the files the red commit itself introduced (its diff against its
# first parent, or the empty tree for a root commit).
RED_PARENT="$(git -C "$PROJECT_ROOT" rev-parse --verify "${RED_FULL}^1^{commit}" 2>/dev/null || true)"
if [ -n "$RED_PARENT" ]; then
  RED_CHANGED="$(git -C "$PROJECT_ROOT" diff --name-only "$RED_PARENT" "$RED_FULL" 2>/dev/null)"
else
  RED_CHANGED="$(git -C "$PROJECT_ROOT" show --name-only --pretty=format: "$RED_FULL" 2>/dev/null)"
fi
RED_CHANGED="$(printf '%s\n' "$RED_CHANGED" | grep -v '^$' || true)"

# Main-source paths: anything under a src/main/ tree.
RED_MAIN_FILES="$(printf '%s\n' "$RED_CHANGED" | grep -E '/src/main/' || true)"
if [ -n "$RED_MAIN_FILES" ]; then
  echo "── main-source file(s) changed by red commit $RED_FULL ──"
  printf '%s\n' "$RED_MAIN_FILES" | sed 's/^/  /'
  echo "RED-GREEN: FAIL — red commit touches main sources (a red that breaks main code could be 'fixed' by reverting it)"
  exit 1
fi

# ── throwaway working tree, removed on exit even on failure ─────────
# We do NOT use `git worktree add`: the sbt-git versioning plugin runs
# jgit, which mishandles a freshly-added linked worktree's commondir and
# raises MissingObjectException on commits written by another worktree's
# index. A `--shared --local` clone produces a NORMAL repository (its own
# .git directory, objects reachable via an alternates pointer to the
# source store), so the git plugin loads cleanly and we can check out an
# arbitrary detached sha. The clone is removed on exit.
WT="$(mktemp -d "${TMPDIR:-/tmp}/red-green.XXXXXX")"
WT="$WT/checkout"
cleanup() {
  # Shut down the per-clone sbt server so it does not outlive the dir.
  ( cd "$WT" 2>/dev/null && sbt --client shutdown ) >/dev/null 2>&1
  rm -rf "$(dirname "$WT")" >/dev/null 2>&1
}
trap cleanup EXIT

GIT_COMMON_DIR="$(git -C "$PROJECT_ROOT" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)"
if [ -z "$GIT_COMMON_DIR" ]; then
  echo "RED-GREEN: FAIL — could not resolve git common dir for $PROJECT_ROOT"
  exit 1
fi

if ! git clone --shared --local --no-checkout "$GIT_COMMON_DIR" "$WT" >/dev/null 2>&1; then
  echo "RED-GREEN: FAIL — could not clone repo into $WT"
  exit 1
fi
if ! git -C "$WT" checkout --detach "$RED_FULL" >/dev/null 2>&1; then
  echo "RED-GREEN: FAIL — could not check out red sha $RED_FULL in clone"
  exit 1
fi

# ── identify red-test files: ALL test-scope files the red commit added
#    or modified ──────────────────────────────────────────────────────
# The red commit proves the bug; the fix must not touch ANY test-scope
# file the red commit introduced (plan §3). We do NOT narrow to the
# suite-declaring file — a helper/fixture added by the red commit is part
# of the red contract too, so it is tamper-protected as well. (The legal
# way for a fix to change a helper is for that helper to live in a BASE
# commit, not in the red commit.)
TOUCHED_TEST_FILES="$(printf '%s\n' "$RED_CHANGED" | grep -E '/src/test/|/src/it/' | sort -u || true)"

if [ -z "$TOUCHED_TEST_FILES" ]; then
  echo "RED-GREEN: FAIL — red commit $RED_FULL touches no test-scope file (/src/test/ or /src/it/); cannot anchor a red test"
  exit 1
fi

# ── resolve the EXACT FQCN(s) of the named suite from the red-test
#    file(s) ───────────────────────────────────────────────────────────
# We never run `testOnly *glob*`: a shadow/unrelated suite whose name
# merely contains the fragment would match. Instead, find the file(s)
# (among the red commit's test files) that declare a class/object/trait
# whose simple name matches SUITE, read each file's `package` lines AT
# the red commit, and build `pkg.SimpleName`. The run uses those exact
# names. (Reading content at the red commit keeps resolution independent
# of any rename the fix might attempt.)
SUITE_SIMPLE="${SUITE##*.}"

# package_of <fileref> — prints the dotted package for a Scala file given
# as a `git show`-able ref (e.g. "$RED_FULL:path"). Handles split package
# clauses (`package sge` / `package noise`) by joining them with dots.
package_of() {
  local ref="$1" content pkg
  content="$(git -C "$PROJECT_ROOT" show "$ref" 2>/dev/null)"
  pkg="$(printf '%s\n' "$content" \
    | grep -E '^[[:space:]]*package[[:space:]]+[A-Za-z_]' \
    | sed -E 's/^[[:space:]]*package[[:space:]]+//; s/[[:space:]]*$//' \
    | grep -vE '(\{|object|case)' \
    | tr '\n' '.' \
    | sed -E 's/\.+/./g; s/^\.//; s/\.$//')"
  printf '%s' "$pkg"
}

RED_TEST_FILES=""
FQCNS=""
while IFS= read -r f; do
  [ -z "$f" ] && continue
  RED_TEST_FILES="${RED_TEST_FILES}${f}"$'\n'
  if git -C "$PROJECT_ROOT" show "$RED_FULL:$f" 2>/dev/null \
       | grep -qE "(class|object|trait)[[:space:]]+${SUITE_SIMPLE}\b"; then
    pkg="$(package_of "$RED_FULL:$f")"
    if [ -n "$pkg" ]; then
      FQCNS="${FQCNS}${pkg}.${SUITE_SIMPLE}"$'\n'
    else
      FQCNS="${FQCNS}${SUITE_SIMPLE}"$'\n'
    fi
  fi
done <<EOF
$TOUCHED_TEST_FILES
EOF
RED_TEST_FILES="$(printf '%s' "$RED_TEST_FILES" | grep -v '^$' || true)"
FQCNS="$(printf '%s' "$FQCNS" | grep -v '^$' | sort -u || true)"

if [ -z "$FQCNS" ]; then
  echo "── test-scope files touched by red commit ──"
  printf '%s\n' "$TOUCHED_TEST_FILES" | sed 's/^/  /'
  echo "RED-GREEN: FAIL — red commit $RED_FULL adds no test-scope file declaring suite '$SUITE_SIMPLE'; cannot anchor the red test"
  exit 1
fi

echo "── tamper-protected red-test file(s) (all test-scope files in red commit) ──"
printf '%s\n' "$RED_TEST_FILES" | sed 's/^/  /'
echo "── exact suite FQCN(s) to run ──"
printf '%s\n' "$FQCNS" | sed 's/^/  /'

# Build the space-separated testOnly argument list of EXACT FQCNs.
TESTONLY_ARGS="$(printf '%s ' $FQCNS)"

# ── run the resolved suite(s) at the currently-checked-out commit ────
# Echoes captured output to stdout and writes it to $2 for later
# inspection. Returns the sbt exit code (captured BEFORE `cat`).
run_suite() {
  local label="$1" outfile="$2"
  echo "── $label: running '$MODULE/testOnly $TESTONLY_ARGS' ──"
  ( cd "$WT" && sbt --client "$MODULE/testOnly $TESTONLY_ARGS" ) >"$outfile" 2>&1
  local rc=$?
  cat "$outfile"
  return $rc
}

# ── sum ALL munit "<N> failed," summaries in a run ───────────────────
# A run may match more than one declared FQCN (multiple red-test files),
# so we must SUM every failure summary, never trust the first. Prints the
# integer total (0 if none matched).
total_failed() {
  local outfile="$1" total=0 n
  while IFS= read -r n; do
    [ -z "$n" ] && continue
    total=$(( total + n ))
  done < <(grep -oE '[0-9]+ failed,' "$outfile" 2>/dev/null | grep -oE '^[0-9]+')
  printf '%s' "$total"
}

# ── classify a suite run as: pass | assertion-fail | compile-error |
#    no-summary ─────────────────────────────────────────────────────
# Inputs: $1 = outfile, $2 = sbt exit code for that run.
# munit/sbt signals:
#   * "Test run <Suite> started"   → the suite compiled and ran
#   * "<N> failed, … total"        → munit per-suite summary
#   * compile errors print "[error]" WITHOUT any "Test run … started"
# We require BOTH the parsed failure total AND the sbt exit code to agree:
#   * pass            → 0 failures summed AND sbt exit 0
#   * assertion-fail  → >0 failures summed OR sbt exit nonzero (suite ran)
#   * compile-error   → never started + sbt signalled a compile failure
#   * no-summary      → started but unparseable, or nonzero with 0 parsed
classify() {
  local outfile="$1" rc="$2" failed
  if ! grep -q 'Test run .* started' "$outfile" 2>/dev/null; then
    if grep -qE 'Compilation failed|compileIncremental|\[E[0-9]+\]|error found' "$outfile" 2>/dev/null; then
      echo "compile-error"
    else
      echo "no-summary"
    fi
    return
  fi
  failed="$(total_failed "$outfile")"
  if [ "$failed" -gt 0 ]; then
    echo "assertion-fail"
    return
  fi
  # Zero failures parsed. The sbt exit code is the tie-breaker: if sbt
  # exited nonzero despite "0 failed" lines, something failed that the
  # per-suite summary did not capture — do NOT call it a pass.
  if [ "$rc" -ne 0 ]; then
    echo "no-summary"
  else
    echo "pass"
  fi
}

# ── print the failing assertion line(s) from a captured run ─────────
print_failing_assertions() {
  local outfile="$1"
  echo "── failing assertion line(s) ──"
  grep -nE 'munit|assert|FAILED|failed|==> X|values are not|expected|Caused by|Exception' "$outfile" \
    | grep -vE 'Test run .* finished: 0 failed' \
    | head -n 20
}

# ══ PHASE 1 — RED at red-sha: must be an assertion failure ══════════
RED_OUT="$WT/.red-out.txt"
run_suite "RED phase @ $RED_FULL" "$RED_OUT"
RED_RC=$?
RED_CLASS="$(classify "$RED_OUT" "$RED_RC")"
echo "red phase classification: $RED_CLASS (sbt exit $RED_RC, failures summed: $(total_failed "$RED_OUT"))"

case "$RED_CLASS" in
  compile-error)
    echo "── compile errors at red sha ──"
    grep -nE '\[error\]' "$RED_OUT" | head -n 20
    echo "RED-GREEN: FAIL — compile-error-red: suite '$SUITE' did not compile at red sha $RED_FULL (FALSE RED, not a valid red phase)"
    exit 1
    ;;
  pass)
    echo "RED-GREEN: FAIL — suite '$SUITE' PASSED at red sha $RED_FULL (no red phase: the test does not catch the bug)"
    exit 1
    ;;
  no-summary)
    echo "── red run tail ──"
    tail -n 20 "$RED_OUT"
    echo "RED-GREEN: FAIL — could not parse a munit result for '$SUITE' at red sha $RED_FULL"
    exit 1
    ;;
  assertion-fail)
    print_failing_assertions "$RED_OUT"
    echo "red phase OK: '$SUITE' fails by assertion at $RED_FULL"
    ;;
esac

# ══ PHASE 2 — GREEN at fix-sha: must pass ═══════════════════════════
# Move the clone to the fix commit.
if ! git -C "$WT" checkout --detach "$FIX_FULL" >/dev/null 2>&1; then
  echo "RED-GREEN: FAIL — could not checkout fix sha $FIX_FULL in clone"
  exit 1
fi

FIX_OUT="$WT/.fix-out.txt"
run_suite "GREEN phase @ $FIX_FULL" "$FIX_OUT"
FIX_RC=$?
FIX_CLASS="$(classify "$FIX_OUT" "$FIX_RC")"
echo "fix phase classification: $FIX_CLASS (sbt exit $FIX_RC, failures summed: $(total_failed "$FIX_OUT"))"

case "$FIX_CLASS" in
  pass)
    # Belt-and-braces: green requires BOTH 0 summed failures and sbt
    # exit 0. classify() already enforces this, but assert it loudly.
    if [ "$FIX_RC" -ne 0 ]; then
      echo "RED-GREEN: FAIL — suite '$SUITE' reported 0 failures but sbt exited $FIX_RC at fix sha $FIX_FULL"
      exit 1
    fi
    echo "green phase OK: '$SUITE' passes at $FIX_FULL"
    ;;
  compile-error)
    echo "── compile errors at fix sha ──"
    grep -nE '\[error\]' "$FIX_OUT" | head -n 20
    echo "RED-GREEN: FAIL — suite '$SUITE' did not compile at fix sha $FIX_FULL"
    exit 1
    ;;
  assertion-fail)
    print_failing_assertions "$FIX_OUT"
    echo "RED-GREEN: FAIL — suite '$SUITE' still FAILS at fix sha $FIX_FULL (the fix does not make the red test pass)"
    exit 1
    ;;
  *)
    echo "── fix run tail ──"
    tail -n 20 "$FIX_OUT"
    echo "RED-GREEN: FAIL — could not parse a munit result for '$SUITE' at fix sha $FIX_FULL"
    exit 1
    ;;
esac

# ══ PHASE 3 — TAMPER check: red-test files unchanged red..fix ═══════
# Protect ALL test-scope files the red commit introduced. We run git diff
# in a way that lets us distinguish "no changes" (exit 0, empty output)
# from "git error" (nonzero exit): --exit-code makes diff return 1 when
# there ARE changes and 0 when there are none; any OTHER code is an error
# and we fail closed.
git -C "$PROJECT_ROOT" diff --exit-code "$RED_FULL".."$FIX_FULL" -- $RED_TEST_FILES >"$WT/.tamper.txt" 2>&1
TAMPER_RC=$?
if [ "$TAMPER_RC" -eq 0 ]; then
  echo "tamper check OK: red-test files are byte-identical between $RED_FULL and $FIX_FULL"
elif [ "$TAMPER_RC" -eq 1 ]; then
  echo "── TAMPER: red-test file(s) changed between red and fix ──"
  cat "$WT/.tamper.txt"
  echo "RED-GREEN: FAIL — fix commit modifies a red-test file (the fix must not touch any test-scope file the red commit added)"
  exit 1
else
  echo "── git diff error output ──"
  cat "$WT/.tamper.txt"
  echo "RED-GREEN: FAIL — tamper-check git diff failed (exit $TAMPER_RC); failing closed"
  exit 1
fi

# ══ VERDICT ═════════════════════════════════════════════════════════
echo "RED-GREEN: PASS"
exit 0
