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
#   .rescale/scripts/red-green.sh --args <module> <suite> <red-sha> <fix-sha> [platform]
#
# Platform selection (ISS-596):
#
#   An optional 5th argument selects the test platform: `jvm` (default) or
#   `native`. JVM runs `<module>/testOnly`; Native runs the projectMatrix
#   Native variant `<module>Native/testOnly`. The two backends print DIFFERENT
#   test-result dialects, and classify()/total_failed() understand both:
#
#     * JVM (munit sbt-event listener):
#         Test run <Suite> started
#         Test run <Suite> finished: <N> failed, … total
#       …plus the sbt overall summary line below.
#
#     * Scala Native (NO munit per-run lines — the test binary is a separate
#       process whose per-test output sbt --client does not relay): the ONLY
#       machine-readable signal is the sbt overall summary line, prefixed
#       `Passed:` on success and `Failed:` on failure:
#         [info]  Passed: Total <N>, Failed 0, Errors 0, Passed <N>
#         [error] Failed: Total <N>, Failed <M>, Errors <E>, Passed <P>
#       with a completion trailer of `[success]`/`elapsed time` (pass) or
#       `TestsFailedException`/`[error]` (fail).
#
#   Both backends emit the `… Total N, Failed M, Errors E, Passed P[, Skipped
#   S]` summary, so the Native classifier reuses that line for BOTH the "ran
#   to completion" signal (replacing JVM's "Test run … started") and the
#   failure count (replacing JVM's "<N> failed,"), with the same fail-closed
#   discipline: rc/summary coherence, completion trailer required, a count
#   cross-check of Failed+Errors against Total-Passed, and an executed-count
#   guard (Passed+Failed+Errors >= 1, so skipped/ignored tests cannot stand
#   in for executed ones).
#
# Executed-count guard (ISS-596 bounce 1 + bounce 2):
#
#   Two ways a phase can run ZERO real tests yet read as green:
#
#     * Vanished suite (bounce 1) — `testOnly no.such.Suite`, or a fix that
#       silently drops the suite from scope — prints
#       `Total 0, Failed 0, Errors 0, Passed 0` with sbt exit 0.
#     * Skipped-to-zero suite (bounce 2) — sbt's `Total` field INCLUDES
#       skipped/ignored/canceled/pending tests, so a suite whose only test
#       is skipped prints `Total 1, Failed 0, Errors 0, Passed 0, Skipped 1`
#       with sbt exit 0. `Total` is 1 — a `Total >= 1` guard is satisfied —
#       yet ZERO assertions executed (a fix that legally adds `assume(false)`
#       to a base-commit helper could earn this vacuous green).
#
#   In both cases "0 failures AND sbt exit 0" would otherwise read as a pass
#   (fail-open). BOTH classifiers therefore require the EXECUTED count —
#   summed `Passed + Failed + Errors` across all summary lines (skipped /
#   ignored / canceled / pending do NOT count) — to be >= 1; an executed-0
#   summary is no-summary → fail closed (exit 2). This holds in the red
#   phase as well as the green phase.
#
#   Provenance differs but the RULE is now identical on both backends:
#   bounce 1 used `Total >= 1`, which the JVM "Test run … started" marker
#   only incidentally reinforced and which sbt's skip-inclusive `Total`
#   silently defeated for the skip case; bounce 2 replaces it with the
#   executed-count guard so JVM and Native fail closed by the SAME explicit
#   rule for vanished AND skipped suites alike. The native cross-check
#   (Failed+Errors vs Total-Passed) ALSO happens to reject the skip case,
#   but that is incidental — the executed-count guard is the shared,
#   intentional gate.
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
# ISS-596 Scala Native canary / probe set (--platform native):
#
#   canary/iss596-native       base(helper=1)+red(test only)+fix(helper=2),
#                              run with --platform native
#                              → red classifies the assertion-fail from the
#                                NATIVE summary line, green passes
#                              → RED-GREEN: PASS, exit 0
#   canary/iss596-native-tamper  fix edits the red-test file (native)
#                              → FAIL tamper, exit 1
#
# ISS-596 bounce-1 ran-at-all-guard probe set (all driven through the
# `--classify` self-test entrypoint and/or the live gate; each probe branch
# ships its own runner under sge-extension/noise/src/test/resources/iss596/):
#
# ISS-596 bounce-2 also adds a SKIP fixture to each zerotest probe — sbt's
# `Total` includes skipped/ignored tests, so a skipped-to-zero phase prints
# `Total 1, Failed 0, Errors 0, Passed 0, Skipped 1` rc=0. The bounce-1
# `Total >= 1` guard was satisfied yet 0 tests executed → vacuous green. The
# executed-count guard (Passed+Failed+Errors >= 1) closes it; both zerotest
# runners assert the skip fixture classifies `no-summary` on its backend.
#
#   probe/iss596-native-zerotest  base(helper=1)+red(suite asserts helper==2)
#                              +fix(DELETES the suite, bumps helper). At
#                              fix-sha `…Native/testOnly <FQCN>` matches zero
#                              suites → `Total 0, …` rc=0. The green phase
#                              must fail closed (exit 2). The branch runner
#                              also proves the SAME no-summary verdict at the
#                              RED-phase position (rc=1, Total 0), AND at the
#                              bounce-2 SKIP fixture (native-skip.out,
#                              `Total 1 … Passed 0, Skipped 1`, rc 0 and 1).
#                              → no-summary at both phases AND on skip
#   probe/iss596-jvm-zerotest  the JVM mirror. Ships the adversarial
#                              `started-marker present AND finished 0 total
#                              with Total 0` fixture that ONLY the explicit
#                              executed-count guard catches (the started
#                              marker is satisfied) — without the guard it
#                              was a fail-open `pass`. Bounce 2 adds the SKIP
#                              fixture (jvm-skip.out, the auditor's exact
#                              capture shape `Total 1 … Passed 0, Skipped 1`,
#                              rc 0) and a finding-B fixture (failures>0 with
#                              rc 0 → no-summary, forged/stale red).
#                              → no-summary (fail closed) on every case
#   probe/iss596-native-trunc  truncated output (summary cut mid-line; output
#                              cut before any summary; live truncate-stub.sh)
#                              with rc=1 → no-summary, exit 2 tool-error
#                              (fail-closed parity with JVM)
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
#   * --args:      the four (or five) values are passed literally on the CLI.
# PLATFORM is optional and defaults to jvm (ISS-596): an empty `platform`
# key in the mode file (the wrapper always writes the key, possibly empty)
# also means jvm.
MODULE="" ; SUITE="" ; RED="" ; FIX="" ; PLATFORM=""

read_mode_file() {
  local f="$1"
  if [ ! -f "$f" ]; then
    echo "RED-GREEN: FAIL — mode file not found: $f (did you pass --mode run -- <module> <suite> <red> <fix> [platform]?)"
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
      module)   MODULE="$val" ;;
      suite)    SUITE="$val" ;;
      red)      RED="$val" ;;
      fix)      FIX="$val" ;;
      platform) PLATFORM="$val" ;;
    esac
  done < "$f"
  # The runner contract: read on entry, delete on entry.
  rm -f "$f"
}

# ── classifier self-test (ISS-596 bounce 1) ─────────────────────────
# `--classify <platform> <rc> <outfile>` runs ONLY the classifier against
# a captured/fabricated test-output file and prints its verdict word
# (pass | assertion-fail | compile-error | no-summary), exiting 0. This
# lets the permanent probes (e.g. probe/iss596-native-trunc) feed the
# classifier a truncated/all-zero summary fixture and assert the verdict
# WITHOUT a full sbt clone — the same code path the live gate uses. The
# actual dispatch happens after the classifier functions are defined,
# below; here we only stash the flag.
SELFTEST_MODE=""
if [ "${1:-}" = "--classify" ]; then
  SELFTEST_MODE="classify"
  PLATFORM="${2:-}"
  SELFTEST_RC="${3:-}"
  SELFTEST_OUT="${4:-}"
  # Minimal validation; the dispatcher below does the rest.
  case "$PLATFORM" in ''|'$5') PLATFORM="jvm" ;; esac
elif [ "${1:-}" = "--args" ]; then
  shift
  MODULE="${1:-}" ; SUITE="${2:-}" ; RED="${3:-}" ; FIX="${4:-}" ; PLATFORM="${5:-}"
elif [ -n "${1:-}" ]; then
  read_mode_file "$1"
else
  echo "RED-GREEN: FAIL — no mode file path and no --args given"
  exit 1
fi

if [ -n "$SELFTEST_MODE" ]; then
  # Defer to the post-function dispatch; skip the run-mode validation.
  :
elif [ -z "$MODULE" ] || [ -z "$SUITE" ] || [ -z "$RED" ] || [ -z "$FIX" ]; then
  echo "RED-GREEN: FAIL — missing argument (module='$MODULE' suite='$SUITE' red='$RED' fix='$FIX')"
  exit 1
fi

# ── platform selection (ISS-596) ────────────────────────────────────
# Default jvm when absent/empty. The runner's mode-file templating leaves
# an UNFILLED positional as the literal placeholder (e.g. "$5") when the
# caller passes only the four JVM args; treat that — and an empty value —
# as the jvm default so JVM invocations stay behavior-identical to
# pre-ISS-596 EXCEPT that a no-summary / vanished-suite / all-skipped run
# now exits 2 (the executed-count guard, ISS-596 bounce 1 + bounce 2)
# instead of being mis-read as a pass. The Native test project is the
# projectMatrix variant named
# `<module>Native`; the JVM variant keeps the bare module name. Reject any
# other explicit value rather than silently picking jvm — fail closed.
#
# Everything from here to the function definitions is the LIVE-GATE SETUP
# (sha resolution, clone, FQCN resolution). In classifier self-test mode
# (`--classify`) we skip it entirely and fall through to the dispatcher at
# the foot of the file once the classifier functions are defined.
if [ -z "$SELFTEST_MODE" ]; then
case "$PLATFORM" in
  ''|'$5') PLATFORM="jvm" ;;
esac
case "$PLATFORM" in
  jvm)
    RUN_MODULE="$MODULE"
    ;;
  native)
    RUN_MODULE="${MODULE}Native"
    ;;
  *)
    echo "RED-GREEN: FAIL — unknown platform '$PLATFORM' (expected jvm or native)"
    exit 1
    ;;
esac

echo "red-green: module=$MODULE platform=$PLATFORM run-module=$RUN_MODULE suite=$SUITE red=$RED fix=$FIX"

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

fi  # end live-gate setup (skipped in --classify self-test mode)

# ── run the resolved suite(s) at the currently-checked-out commit ────
# Echoes captured output to stdout and writes it to $2 for later
# inspection. Returns the sbt exit code (captured BEFORE `cat`).
run_suite() {
  local label="$1" outfile="$2"
  echo "── $label: running '$RUN_MODULE/testOnly $TESTONLY_ARGS' ──"
  ( cd "$WT" && sbt --client "$RUN_MODULE/testOnly $TESTONLY_ARGS" ) >"$outfile" 2>&1
  local rc=$?
  cat "$outfile"
  return $rc
}

# ── sum ALL failure counts in a run ──────────────────────────────────
# A run may match more than one declared FQCN (multiple red-test files),
# so we must SUM every failure summary, never trust the first. Prints the
# integer total (0 if none matched).
#
# JVM (munit per-suite "<N> failed," lines):       sum every <N>.
# Native (sbt overall "Total N, Failed M, Errors E, Passed P" line — no
#   per-suite munit lines exist): sum Failed M + Errors E from every such
#   summary. Both a test assertion failure (Failed) and an uncaught
#   error/exception (Errors) are non-passing outcomes the gate must catch.
total_failed() {
  local outfile="$1" total=0 n
  if [ "$PLATFORM" = "native" ]; then
    while IFS= read -r n; do
      [ -z "$n" ] && continue
      total=$(( total + n ))
    done < <(native_failed_errors "$outfile")
    printf '%s' "$total"
    return
  fi
  while IFS= read -r n; do
    [ -z "$n" ] && continue
    total=$(( total + n ))
  done < <(grep -aoE '[0-9]+ failed,' "$outfile" 2>/dev/null | grep -oE '^[0-9]+')
  printf '%s' "$total"
}

# ── Native summary helpers (ISS-596) ─────────────────────────────────
# The sbt overall result line for a Native (or JVM) run has the shape:
#   Passed: Total 4, Failed 0, Errors 0, Passed 4    (success)
#   Failed: Total 1, Failed 1, Errors 0, Passed 0    (failure)
# native_summary_lines    — every such summary line (verbatim, -a binary-safe)
# native_failed_errors    — for each summary line, prints "Failed + Errors"
# native_total_minus_pass — for each summary line, prints "Total - Passed"
# The classifier cross-checks the two (Failed+Errors must equal Total-Passed)
# to fail closed on a malformed/truncated summary.
native_summary_lines() {
  grep -aoE 'Total [0-9]+, Failed [0-9]+, Errors [0-9]+, Passed [0-9]+' "$1" 2>/dev/null
}
native_failed_errors() {
  local line f e
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    f="$(printf '%s' "$line" | grep -oE 'Failed [0-9]+' | grep -oE '[0-9]+')"
    e="$(printf '%s' "$line" | grep -oE 'Errors [0-9]+' | grep -oE '[0-9]+')"
    printf '%s\n' "$(( f + e ))"
  done < <(native_summary_lines "$1")
}
native_total_minus_pass() {
  local line t p
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    t="$(printf '%s' "$line" | grep -oE 'Total [0-9]+' | grep -oE '[0-9]+')"
    p="$(printf '%s' "$line" | grep -oE 'Passed [0-9]+' | grep -oE '[0-9]+')"
    printf '%s\n' "$(( t - p ))"
  done < <(native_summary_lines "$1")
}

# ── ran-at-all / executed-count guard (ISS-596 bounce 1 + bounce 2) ───
# Sum the EXECUTED-test count across EVERY sbt overall summary line, where
# executed = Passed + Failed + Errors. Both backends emit at least one
# `… Total N, Failed M, Errors E, Passed P[, Skipped S]` summary, so this
# works on jvm and native alike.
#
# bounce 1 closed the vanished-suite hole — `testOnly no.such.Suite` prints
# `Total 0, Failed 0, Errors 0, Passed 0` with sbt exit 0; "0 failures AND
# sbt exit 0" would otherwise read as a pass (fail-open).
#
# bounce 2 (this fix) closes a SUBTLER hole: sbt's `Total` field INCLUDES
# skipped/ignored/canceled/pending tests (unlike a per-suite munit
# "ignored" count). A phase whose only test is SKIPPED prints
# `Passed: Total 1, Failed 0, Errors 0, Passed 0, Skipped 1` with sbt exit
# 0 — `Total` is 1, so a `Total >= 1` guard would be satisfied, yet ZERO
# assertions actually ran. A fix that legally adds `assume(false)` to a
# base-commit helper could thereby earn a vacuous green. We therefore count
# only the EXECUTED outcomes (Passed + Failed + Errors); a skipped /
# ignored / canceled / pending test contributes to none of them, so a
# skipped-to-zero phase has executed == 0 and fails closed. Both
# classifiers require executed >= 1 in BOTH phases (red too); an executed-0
# summary (vanished OR skipped-to-zero) is no-summary → fail closed.
#
# Native survives the skip case even on the cross-check (Failed+Errors=0 vs
# Total-Passed=1 disagree), but that is an accident of the skip count; the
# executed-count guard makes BOTH classifiers reject it by the SAME rule.
executed_tests() {
  local outfile="$1" total=0 line p f e
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    p="$(printf '%s' "$line" | grep -oE 'Passed [0-9]+' | grep -oE '[0-9]+')"
    f="$(printf '%s' "$line" | grep -oE 'Failed [0-9]+' | grep -oE '[0-9]+')"
    e="$(printf '%s' "$line" | grep -oE 'Errors [0-9]+' | grep -oE '[0-9]+')"
    total=$(( total + p + f + e ))
  done < <(native_summary_lines "$outfile")
  printf '%s' "$total"
}

# ── reported-test count (skip-INCLUSIVE) — sum the `Total N` field across
# every summary line. This is NOT the gate (sbt's Total includes skipped /
# ignored tests, which is exactly why the executed-count guard above exists,
# ISS-596 bounce 2). It is used ONLY by the no-summary DIAGNOSTIC below to
# tell "vanished suite" (Total 0) apart from "ran but every test was
# skipped" (Total >= 1, executed 0) so the failure is debuggable.
total_tests() {
  local outfile="$1" total=0 t
  while IFS= read -r t; do
    [ -z "$t" ] && continue
    total=$(( total + t ))
  done < <(native_summary_lines "$outfile" | grep -oE 'Total [0-9]+' | grep -oE '[0-9]+')
  printf '%s' "$total"
}

# ── classify a suite run as: pass | assertion-fail | compile-error |
#    no-summary ─────────────────────────────────────────────────────
# Inputs: $1 = outfile, $2 = sbt exit code for that run.
#
# JVM (munit/sbt signals):
#   * "Test run <Suite> started"   → the suite compiled and ran
#   * "<N> failed, … total"        → munit per-suite summary
#   * compile errors print "[error]" WITHOUT any "Test run … started"
#
# Native (ISS-596) — NO munit per-run lines exist; the only completion
# signal is the sbt overall summary line:
#   * "… Total N, Failed M, Errors E, Passed P"  → the binary ran to
#     completion (this plays the role JVM's "started" plays)
#   * Failed M + Errors E > 0                     → assertion/error fail
#   * compile errors print "[error]"/"Compilation failed" WITHOUT any
#     summary line
#
# We require the parsed failure total AND the sbt exit code to be COHERENT
# (this matches the JVM path to the Native one, ISS-596 bounce 2 finding B):
#   * pass            → 0 failures summed AND sbt exit 0 AND >=1 executed
#                       test (Passed+Failed+Errors), so the suite really ran
#   * assertion-fail  → >0 failures summed AND sbt exit nonzero (both signals
#                       agree the suite ran red)
#   * compile-error   → never started/no summary + sbt signalled a compile
#                       failure
#   * no-summary      → fail closed (→ exit 2) for any incoherent/empty case:
#                       0 executed tests (vanished OR all-skipped/ignored/
#                       canceled/pending), >0 failures with sbt exit 0, 0
#                       failures with sbt exit nonzero, or unparseable /
#                       truncated output
classify() {
  local outfile="$1" rc="$2" failed
  if [ "$PLATFORM" = "native" ]; then
    classify_native "$outfile" "$rc"
    return
  fi
  if ! grep -aq 'Test run .* started' "$outfile" 2>/dev/null; then
    if grep -aqE 'Compilation failed|compileIncremental|\[E[0-9]+\]|error found' "$outfile" 2>/dev/null; then
      echo "compile-error"
    else
      echo "no-summary"
    fi
    return
  fi
  # executed-count guard (ISS-596 bounce 1 + bounce 2): the "Test run …
  # started" marker only proves sbt *attempted* a suite. A vanished suite
  # leaves an all-zero `Total 0` summary (bounce 1); a skipped-to-zero
  # suite leaves `Total 1, … Passed 0, Skipped 1` (bounce 2) — Total >= 1
  # yet ZERO tests executed. Require Passed+Failed+Errors >= 1 (skipped /
  # ignored / canceled / pending do NOT count), mirroring the native guard
  # exactly, so the JVM path fails closed by design — naming skips in the
  # message so a legitimately-skipped suite is debuggable.
  if [ "$(executed_tests "$outfile")" -lt 1 ]; then
    echo "no-summary"
    return
  fi
  failed="$(total_failed "$outfile")"
  if [ "$failed" -gt 0 ]; then
    # Failures parsed; rc/summary coherence (matched to native, ISS-596
    # bounce 2 finding B): an assertion-fail must coincide with sbt exiting
    # nonzero. A "<N> failed" line with sbt exit 0 is incoherent (forged or
    # stale output) — do NOT trust it as a valid red; fail closed.
    if [ "$rc" -eq 0 ]; then
      echo "no-summary"
    else
      echo "assertion-fail"
    fi
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

# ── classify a Scala Native suite run (ISS-596) ──────────────────────
# The Native test binary runs as a separate process and sbt --client does
# NOT relay its per-test munit lines; the ONLY machine-readable signal is
# the sbt overall summary line. Discipline mirrors the JVM path:
#   * completion trailer required — at least one "Total N, Failed M, …"
#     summary line must be present, else compile-error (if a compile
#     failure is signalled) or no-summary (truncation / unparseable).
#   * count cross-check — for every summary line, Failed+Errors must equal
#     Total-Passed; any mismatch is a malformed/truncated summary and we
#     fail closed as no-summary.
#   * executed-count guard (ISS-596 bounce 2) — Passed+Failed+Errors must be
#     >= 1; a vanished (Total 0) or all-skipped (Total>=1 but Passed=0)
#     suite executed nothing → no-summary.
#   * rc/summary coherence — sbt exit 0 with >0 failures, or sbt exit
#     nonzero with 0 failures, is incoherent → no-summary (never pass).
classify_native() {
  local outfile="$1" rc="$2" failed

  # No summary line at all → either a compile error or truncated output.
  if [ -z "$(native_summary_lines "$outfile")" ]; then
    if grep -aqE 'Compilation failed|compileIncremental|\[E[0-9]+\]|error found' "$outfile" 2>/dev/null; then
      echo "compile-error"
    else
      echo "no-summary"
    fi
    return
  fi

  # Cross-check every summary line: Failed+Errors must equal Total-Passed.
  # A mismatch means the line was truncated or malformed — fail closed.
  if [ "$(native_failed_errors "$outfile")" != "$(native_total_minus_pass "$outfile")" ]; then
    echo "no-summary"
    return
  fi

  # executed-count guard (ISS-596 bounce 1 + bounce 2): a vanished suite
  # (e.g. a fix that silently drops the suite from Native scope) prints
  # `Total 0, Failed 0, Errors 0, Passed 0` with sbt exit 0 — passes the
  # cross-check (0 == 0) and parses 0 failures, so it would be mis-classified
  # as `pass` (fail-open). A skipped-to-zero suite prints `Total 1, …,
  # Passed 0, Skipped 1`: Total >= 1 yet ZERO tests executed — it passes the
  # cross-check ONLY accidentally (0 vs Total-Passed=1 actually DISAGREE, so
  # native already fails it there), but bounce 2 makes BOTH classifiers
  # reject it by the SAME rule. Require Passed+Failed+Errors >= 1 (skipped /
  # ignored / canceled / pending do NOT count); an executed-0 summary is
  # no-summary → fail closed (exit 2).
  if [ "$(executed_tests "$outfile")" -lt 1 ]; then
    echo "no-summary"
    return
  fi

  failed="$(total_failed "$outfile")"

  if [ "$failed" -gt 0 ]; then
    # Failures summed; sbt must have exited nonzero. If it exited 0 the
    # signals disagree — fail closed rather than trust a stale/partial run.
    if [ "$rc" -eq 0 ]; then
      echo "no-summary"
    else
      echo "assertion-fail"
    fi
    return
  fi

  # Zero failures parsed. sbt exit code is the tie-breaker (same as JVM).
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
  grep -anE 'munit|assert|FAILED|failed|==> X|values are not|expected|Caused by|Exception' "$outfile" \
    | grep -avE 'Test run .* finished: 0 failed' \
    | head -n 20
}

# ── diagnose a no-summary verdict so it is debuggable (ISS-596 bounce 2)
# A no-summary verdict means the executed-count guard (or a coherence /
# truncation check) fired. The most actionable sub-case is "the suite ran
# but every test was skipped/ignored/canceled/pending" — Total >= 1 yet
# executed == 0. Name it explicitly (the acceptance asks for a message that
# names skips) so a legitimately-skipped suite is not mistaken for a tool
# glitch. Prints to stdout alongside the run tail the callers already show.
diagnose_no_summary() {
  local outfile="$1"
  local executed total
  executed="$(executed_tests "$outfile")"
  total="$(total_tests "$outfile")"
  if [ "$executed" -lt 1 ] && [ "$total" -ge 1 ]; then
    echo "── note: $total test(s) reported but 0 executed (Passed+Failed+Errors=0) ──"
    echo "   every test was skipped/ignored/canceled/pending — the gate fails"
    echo "   closed (a skipped suite cannot prove red OR green). Skip lines:"
    grep -anE 'Skipped|[Ii]gnored|[Cc]anceled|[Cc]ancelled|[Pp]ending|assume' "$outfile" \
      | head -n 10 | sed 's/^/   /'
  fi
}

# ── classifier self-test dispatch (ISS-596 bounce 1) ────────────────
# `--classify <platform> <rc> <outfile>` exercises ONLY the classifier —
# the SAME `classify`/`classify_native` the live gate calls — against a
# captured or fabricated test-output fixture, prints the verdict word and
# exits 0. Used by the permanent probe scripts (probe/iss596-native-trunc,
# probe/iss596-native-zerotest, probe/iss596-jvm-zerotest) so they can
# assert the fail-closed verdict on a truncated / all-zero summary WITHOUT
# spinning up a full sbt clone. No live-gate state is touched.
if [ -n "$SELFTEST_MODE" ]; then
  if [ -z "$SELFTEST_OUT" ] || [ ! -f "$SELFTEST_OUT" ]; then
    echo "SELFTEST: FAIL — output fixture not found: '$SELFTEST_OUT'"
    echo "usage: red-green.sh --classify <jvm|native> <sbt-rc> <outfile>"
    exit 1
  fi
  case "$SELFTEST_RC" in
    ''|*[!0-9]*)
      echo "SELFTEST: FAIL — sbt-rc must be a non-negative integer, got '$SELFTEST_RC'"
      exit 1
      ;;
  esac
  case "$PLATFORM" in
    jvm|native) : ;;
    *)
      echo "SELFTEST: FAIL — platform must be jvm or native, got '$PLATFORM'"
      exit 1
      ;;
  esac
  classify "$SELFTEST_OUT" "$SELFTEST_RC"
  exit 0
fi

# ══ PHASE 1 — RED at red-sha: must be an assertion failure ══════════
RED_OUT="$WT/.red-out.txt"
run_suite "RED phase @ $RED_FULL" "$RED_OUT"
RED_RC=$?
RED_CLASS="$(classify "$RED_OUT" "$RED_RC")"
echo "red phase classification: $RED_CLASS (sbt exit $RED_RC, failures summed: $(total_failed "$RED_OUT"))"

case "$RED_CLASS" in
  compile-error)
    echo "── compile errors at red sha ──"
    grep -anE '\[error\]' "$RED_OUT" | head -n 20
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
    diagnose_no_summary "$RED_OUT"
    # Unparseable / truncated test output — OR a suite that ran 0 executed
    # tests (vanished, or every test skipped/ignored, ISS-596 bounce 2) — is
    # a TOOL error, not a test verdict: we cannot tell red from green. Fail
    # closed with a distinct exit 2 so callers do not mistake it for a
    # normal RED-GREEN: FAIL.
    echo "RED-GREEN: FAIL — could not parse a test result for '$SUITE' at red sha $RED_FULL (tool error)"
    exit 2
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
    grep -anE '\[error\]' "$FIX_OUT" | head -n 20
    echo "RED-GREEN: FAIL — suite '$SUITE' did not compile at fix sha $FIX_FULL"
    exit 1
    ;;
  assertion-fail)
    print_failing_assertions "$FIX_OUT"
    echo "RED-GREEN: FAIL — suite '$SUITE' still FAILS at fix sha $FIX_FULL (the fix does not make the red test pass)"
    exit 1
    ;;
  no-summary|*)
    echo "── fix run tail ──"
    tail -n 20 "$FIX_OUT"
    diagnose_no_summary "$FIX_OUT"
    # Same fail-closed tool-error discipline as the red phase: an
    # unparseable / truncated green run — OR a suite that ran 0 executed
    # tests (vanished, or every test skipped/ignored, ISS-596 bounce 2) —
    # cannot be trusted as a pass.
    echo "RED-GREEN: FAIL — could not parse a test result for '$SUITE' at fix sha $FIX_FULL (tool error)"
    exit 2
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
