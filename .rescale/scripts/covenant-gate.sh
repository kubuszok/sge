#!/bin/bash
#
# covenant-gate.sh — set-based ratchet gate for the covenant/shortcut checks
# (ISS-483).
#
# Background
# ----------
# `re-scale enforce verify --all` and `re-scale enforce shortcuts --covenanted`
# currently report a large backlog of failing covenanted files (the open work
# of the 2026-06 remediation campaign). The CI steps that ran them were marked
# `continue-on-error: true`, so CI could never actually reject a regression — a
# brand-new stubbed covenanted file would sail through green.
#
# Naively deleting `continue-on-error` would make CI permanently red against the
# existing backlog. Instead this script implements a SET-BASED RATCHET:
#
#   * A committed baseline (.rescale/data/covenant-gate-baseline.tsv) records the
#     exact set of currently-failing (file, kind) pairs.
#   * On every run the gate recomputes the live failing set and compares it to
#     the baseline AS A SET.
#       - Any live (file, kind) NOT in the baseline is a NEW failure → the gate
#         FAILS (exit 1) and names it.
#       - Any baseline (file, kind) no longer failing is a SHRINK → reported as
#         informational ("update the baseline"); it does NOT fail the gate, and
#         the gate does NOT auto-edit the committed baseline (baseline shrink is
#         manual/orchestrator work).
#
# A SET comparison (not a count comparison) is deliberate: a count gate would let
# one freshly-introduced failure hide behind one independently-fixed file. The
# ratchet can only move down.
#
# Kinds
# -----
# Each failing entry is normalized to a stable `kind` so the (file, kind) pair is
# reproducible:
#
#   verify --all line forms:
#     "<path>: shortcuts introduced: N hit(s), e.g. <x> at line M" -> shortcut-drift
#     "<path>: no covenant header"                                 -> missing-header
#     "<path>: methods removed since baseline: n"                  -> methods-removed
#     "<path>: <anything else>"                                    -> verify-other
#
#   shortcuts --covenanted hits (a file with N hits) -> covenanted-shortcut
#
#   filesystem scan (ISS-486):
#     a .scala file under the covenanted source roots whose canonical covenant
#     marker line "* Covenant: " appears more than once -> dup-covenant-header
#
# Paths are stored repo-RELATIVE (re-scale emits absolute paths rooted at the
# repo; CI checks out to a different absolute directory, so we strip the repo
# root to keep the baseline portable).
#
# Usage
# -----
#   .rescale/scripts/covenant-gate.sh            # gate mode (CI): exit 1 on new failures
#   .rescale/scripts/covenant-gate.sh --generate # (re)write the baseline TSV from the live set
#   .rescale/scripts/covenant-gate.sh --check    # explicit gate mode (default)
#
# `re-scale` must be on PATH (CI installs it by cloning its repo earlier in the
# job; see .github/workflows/ci.yml).
#
set -uo pipefail

# REPO_ROOT must be the *physical* root of the tree that `re-scale` will scan,
# because `re-scale enforce verify --all` emits ABSOLUTE paths (its own
# realpath of each file) and this script strips that prefix to make entries
# repo-relative. The previous implementation derived REPO_ROOT from
# ${BASH_SOURCE[0]} via `cd ... && pwd`, which preserves the logical (symlinked)
# path the script was invoked through. On macOS `/tmp` is a symlink to
# `/private/tmp`, so a worktree at `/tmp/x` yielded REPO_ROOT=`/tmp/x` while
# re-scale emitted `/private/tmp/x/...`. The `case "  $REPO_ROOT"/*` prefix
# match then matched NOTHING — the live failing set came back empty, every
# baseline entry looked like a "shrink", and the gate PASSED with a stub
# present (ISS-483 bounce 1).
#
# Fix: derive REPO_ROOT from `git rev-parse --show-toplevel` of the CURRENT
# directory. git returns the physical (symlink-resolved) path, which is exactly
# what re-scale emits, so the prefix match is robust regardless of checkout
# layout (main repo, linked worktree, CI clone, /tmp symlink).
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [ -z "$REPO_ROOT" ]; then
  echo "covenant-gate: FAIL — not inside a git working tree (git rev-parse --show-toplevel failed)." >&2
  echo "covenant-gate: run this script from within the checked-out repository." >&2
  exit 2
fi
# Resolve to a physical path explicitly (defensive: git already returns the
# physical path, but a future git/config quirk must not silently re-introduce a
# logical prefix mismatch).
REPO_ROOT="$(cd "$REPO_ROOT" && pwd -P)"

# Sanity-check that the tree we are about to scan is the checked-out one: this
# script lives at <root>/.rescale/scripts/covenant-gate.sh, so its physical
# parent-of-parent-of-parent must equal REPO_ROOT. A mismatch means the script
# was copied out of the tree or REPO_ROOT was resolved against a different
# checkout — refuse rather than scan the wrong tree.
SCRIPT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd -P)"
if [ "$SCRIPT_ROOT" != "$REPO_ROOT" ]; then
  echo "covenant-gate: FAIL — script root ($SCRIPT_ROOT) != git toplevel ($REPO_ROOT)." >&2
  echo "covenant-gate: run the gate from inside the checkout that contains it." >&2
  exit 2
fi

BASELINE="$REPO_ROOT/.rescale/data/covenant-gate-baseline.tsv"

MODE="check"
case "${1:-}" in
  --generate) MODE="generate" ;;
  --check|"") MODE="check" ;;
  *)
    echo "covenant-gate.sh: unknown argument '$1' (expected --generate or --check)" >&2
    exit 2
    ;;
esac

# FAIL-OPEN GUARD — distinguishing "ran to completion" from "died mid-stream"
# -----------------------------------------------------------------------------
# This gate computes the live failing set purely from the TEXT of two re-scale
# invocations. If either invocation does NOT actually run (re-scale absent from
# PATH, a subcommand error, a crash) or — the ISS-483 bounce-3 hole — runs but
# is KILLED PART-WAY THROUGH after already emitting its opening lines, the text
# this script parses is a TRUNCATED prefix. The live set then comes back EMPTY
# or PARTIAL, baseline entries look like "shrinks", and the gate PASSES exit 0
# on a tree that may carry a brand-new regression.
#
# Why the previous (bounce-2) guard was insufficient
# --------------------------------------------------
# Bounce 2 anchored verify's validity on its FIRST output line
# ("Verified: <N> of <M> files"). But that line is emitted BEFORE the per-file
# failing lines. A run that dies after line 1 with rc=1 (the legitimate
# failures-reported code) still carries that opening line, so it landed in the
# trusted bucket and produced a partial/empty live set => phantom shrinks =>
# PASS. The auditor's b3 probe proved this hides a real methods-removed
# regression: a wrapper replaying the opening lines + one failing line then
# exiting 1 sailed through. Anchoring on a line the tool prints EARLY can never
# prove the tool RAN TO COMPLETION.
#
# Bounce-3 fix: anchor on the COMPLETION TRAILER — the LAST signal each
# subcommand prints — and CROSS-CHECK the self-reported failure count against
# the number of per-file lines we actually parsed. A truncated stream has no
# trailer (its last line is some mid-list per-file line), so it fails closed.
#
# Trailer / terminal-signal formats (verified from real runs of THIS re-scale,
# 0.1.5 — see the bounce-3 report; they differ between the two subcommands):
#
#   verify --all:
#     * Emits, as its FINAL line, the literal completion trailer
#         "re-scale: exit=<rc> command=re-scale enforce verify --all"
#       printed only after the whole verify pass finishes. We require:
#         (1) the LAST non-empty line equals that trailer (mid-stream death =>
#             absent => exit 2), AND
#         (2) the <rc> embedded in the trailer equals the rc we captured
#             (defends against a replayed/forged trailer detached from the run),
#             AND
#         (3) the run's own "Failed: <N>" header equals the number of per-file
#             failing lines we parsed (truncation drops trailing failing lines
#             so the count under-shoots N => mismatch => exit 2; auditor item b).
#       rc must be 0 (all pass) or 1 (failures reported); rc>1 => tool error.
#       The opening "Verified: <N> of <M> files" line is still required as a
#       secondary shape check, but it is NO LONGER the trust anchor.
#
#   shortcuts --covenanted:
#     * PATH-DEPENDENT terminal signal (verified on re-scale 0.1.5 — the two
#       paths differ and rc differs too):
#         - NO covenanted markers (rc=0): the ENTIRE output is the single line
#             "No shortcut markers found"
#           with NO "re-scale: exit=..." trailer. Final line must equal it.
#         - HITS found (rc=1): output prints per-file "<path>  (<k> hits)" headers
#           and hit-detail lines, then a "Total: <N> hits in <M> files" summary,
#           then — as the genuine FINAL line — the completion trailer
#             "re-scale: exit=1 command=re-scale enforce shortcuts --covenanted"
#       So, like verify, the hits path DOES carry the completion trailer and we
#       anchor on it; the no-markers path has its own sentinel. rc and signal
#       must be COHERENT (rc=0<=>sentinel, rc=1<=>trailer) — a mismatch is a
#       replayed/forged stream => exit 2. On the hits path the "<M> files" count
#       in the summary must equal the number of hit-header lines parsed (auditor
#       item b applied to shortcuts: a truncated hit list under-counts headers vs
#       the summary => mismatch => exit 2). rc>1 => tool error.
#
# Capturing rc under `set -o pipefail`: each re-scale call below is a plain
# command substitution (NOT part of a pipeline), and we read `$?` on the very
# next statement before any other command runs. pipefail only affects pipelines,
# so it does not perturb these single-command substitutions; and because the
# script does NOT use `set -e`, a nonzero rc does not abort before we can emit
# our own diagnostic and chosen exit code.
#
# ISS-569 (SIGPIPE) — auditor-sanctioned, fixed in this same commit:
# The bounce-2 shape checks were `printf '%s' "$OUT" | grep -Eq PATTERN`.
# `grep -Eq` exits as soon as it matches and closes the read end of the pipe;
# once $OUT exceeds the OS pipe buffer (verify output is ~196 lines today and
# grows), the still-writing `printf` then takes SIGPIPE. Under `set -o pipefail`
# the pipeline's status becomes 141, which the surrounding logic would read as a
# spurious tool error (exit 2). Every shape/trailer test below is therefore done
# with NO pipeline — `[[ "$STR" == ... ]]`, `[[ "$STR" =~ RE ]]`, or
# `grep -Eq RE <<<"$STR"` (a here-string feeds grep from a temp file / internal
# buffer, not a pipe, so an early-exiting grep cannot SIGPIPE a writer). The
# last-non-empty-line extraction uses pure bash parameter expansion, no pipe.

# last_nonempty_line VAR_NAME OUT_STRING
# --------------------------------------
# Sets the named variable to the LAST non-empty line of OUT_STRING, using only
# bash parameter expansion and a read loop fed by a here-string — NO pipeline,
# so this can never SIGPIPE (ISS-569). A trailing newline / trailing blank lines
# are ignored, so "Total: ...\n" and "...trailer\n" both resolve to the real
# final content line. Used to anchor each subcommand's COMPLETION-TRAILER check
# on the genuinely last thing the tool printed (a truncated stream's last line
# is a mid-list per-file line, which then fails the trailer match => exit 2).
last_nonempty_line() {
  local __out_var="$1" __s="$2" __line __last=""
  while IFS= read -r __line || [ -n "$__line" ]; do
    if [ -n "$__line" ]; then __last="$__line"; fi
  done <<<"$__s"
  printf -v "$__out_var" '%s' "$__last"
}

# count_lines_matching OUT_STRING EXTENDED_REGEX
# ----------------------------------------------
# Echoes the number of lines in OUT_STRING matching the egrep pattern. Fed via a
# here-string (`grep <<<"$s"`), NOT a pipe — `grep -c` reads the whole input so
# there is no early close, but using a here-string keeps every match in this
# script pipe-free as a uniform ISS-569 discipline. `|| true` swallows grep's
# rc=1 on zero matches so the count (0) is still produced under set -uo pipefail.
count_lines_matching() {
  local __n
  __n="$(grep -Ec -- "$2" <<<"$1" || true)"
  printf '%s' "$__n"
}

# compute_live_set
# ----------------
# Emits the live failing set as TAB-separated "<relative-path>\t<kind>" rows on
# stdout, one per line, unsorted. Diagnostics go to stderr. On any tool-error
# (an invocation that did not run, was truncated mid-stream, or whose output
# shape is unrecognized) it prints a diagnostic to stderr and exits the WHOLE
# SCRIPT with status 2 — it deliberately does not "return empty", because an
# empty/partial live set is exactly the fail-open symptom this guard exists to
# prevent.
compute_live_set() {
  local verify_out verify_rc shortcuts_out shortcuts_rc

  verify_out="$(cd "$REPO_ROOT" && re-scale enforce verify --all 2>&1)"
  verify_rc=$?
  shortcuts_out="$(cd "$REPO_ROOT" && re-scale enforce shortcuts --covenanted 2>&1)"
  shortcuts_rc=$?

  local verify_last shortcuts_last

  # ============================ validate verify --all ============================
  # rc guard first: documented codes are 0 (all pass) and 1 (failures reported);
  # any other code (127 not-found, 2 usage, 139 segv, etc.) is a tool error.
  if [ "$verify_rc" -gt 1 ]; then
    echo "covenant-gate: FAIL (exit 2) — 're-scale enforce verify --all' exited with unexpected status $verify_rc (expected 0=all-pass or 1=failures-reported)." >&2
    exit 2
  fi

  # (a) COMPLETION TRAILER as the FINAL signal. re-scale prints, only after the
  # whole verify pass finishes, the literal line:
  #   re-scale: exit=<rc> command=re-scale enforce verify --all
  # A run truncated mid-stream never reaches it, so its last non-empty line is
  # some mid-list per-file failing line instead. Require the last non-empty line
  # to be EXACTLY that trailer AND the <rc> it carries to equal our captured rc
  # (a forged/replayed trailer for a different rc is rejected). No pipeline used.
  last_nonempty_line verify_last "$verify_out"
  local verify_expected_trailer="re-scale: exit=${verify_rc} command=re-scale enforce verify --all"
  if [ "$verify_last" != "$verify_expected_trailer" ]; then
    echo "covenant-gate: FAIL (exit 2) — 're-scale enforce verify --all' did not end with its completion trailer." >&2
    echo "covenant-gate: expected final line: '$verify_expected_trailer'" >&2
    echo "covenant-gate: actual final line:   '$verify_last'" >&2
    echo "covenant-gate: the run was truncated / did not complete (or rc was forged) — refusing to treat a partial result as the failing set. verify rc=$verify_rc." >&2
    exit 2
  fi

  # Secondary shape check: the opening summary line must also be present. It is
  # no longer the trust anchor (the trailer is), but its absence would mean the
  # output shape changed entirely. `grep <<<` here-string, NOT a pipe (ISS-569).
  if ! grep -Eq '^Verified: [0-9]+ of [0-9]+ files' <<<"$verify_out"; then
    echo "covenant-gate: FAIL (exit 2) — 're-scale enforce verify --all' carried its trailer but not its 'Verified: <N> of <M> files' summary; output shape changed." >&2
    exit 2
  fi

  # (b) CROSS-CHECK (only meaningful when failures were reported, i.e. rc==1).
  # The self-reported "Failed: <N>" header must equal the number of per-file
  # failing lines we will parse below. A mid-stream truncation that somehow still
  # showed a trailer (or a future format drift) would drop trailing failing lines,
  # making the parsed count under-shoot N — mismatch => exit 2. On rc==0 (all
  # pass) re-scale emits no per-file lines and need not print a 'Failed:' header,
  # so the cross-check is skipped; the trailer (verified above with rc embedded
  # ==0) is sufficient proof of a complete clean run. All extraction is via
  # here-string / parameter expansion, no pipeline (ISS-569).
  local verify_failed_hdr verify_failed_n verify_parsed_n
  if [ "$verify_rc" -eq 1 ]; then
    verify_failed_hdr="$(grep -E '^Failed: [0-9]+$' <<<"$verify_out" || true)"
    if [ -z "$verify_failed_hdr" ]; then
      echo "covenant-gate: FAIL (exit 2) — 're-scale enforce verify --all' reported failures (rc=1) but did not print its 'Failed: <N>' header; cannot cross-check the parsed failing-line count." >&2
      exit 2
    fi
    verify_failed_n="${verify_failed_hdr##Failed: }"
    # Count per-file failing lines: indented "  <REPO_ROOT>/...: ..." lines. Use a
    # fixed-string-prefixed ERE anchored at line start; REPO_ROOT is interpolated
    # but only ever an absolute path (no regex metacharacters in practice). This is
    # the SAME population the parse loop below consumes.
    verify_parsed_n="$(count_lines_matching "$verify_out" "^  ${REPO_ROOT}/.+: ")"
    if [ "$verify_failed_n" != "$verify_parsed_n" ]; then
      echo "covenant-gate: FAIL (exit 2) — verify count mismatch: header says 'Failed: $verify_failed_n' but $verify_parsed_n per-file failing line(s) were parsed." >&2
      echo "covenant-gate: a complete run lists exactly one line per failure; a discrepancy means the stream was truncated or its format drifted — refusing partial result." >&2
      exit 2
    fi
  fi

  # ===================== validate shortcuts --covenanted ======================
  # rc guard: documented codes are 0 (no covenanted markers) and 1 (hits found);
  # rc>1 (127 not-found, 2 usage, crash) is a tool error.
  if [ "$shortcuts_rc" -gt 1 ]; then
    echo "covenant-gate: FAIL (exit 2) — 're-scale enforce shortcuts --covenanted' exited with unexpected status $shortcuts_rc (expected 0=no-markers or 1=hits-reported)." >&2
    exit 2
  fi

  # (a) TERMINAL SIGNAL as the FINAL line — PATH-DEPENDENT (verified on re-scale
  # 0.1.5; the two paths print DIFFERENT terminal signals):
  #   * no covenanted markers (rc=0): output is the SINGLE line
  #       "No shortcut markers found"
  #     and NO completion trailer is printed. Final line must equal it.
  #   * hits found (rc=1): output ends with the hit-summary line
  #       "Total: <N> hits in <M> files"
  #     IMMEDIATELY FOLLOWED BY the completion trailer (the genuine final line)
  #       "re-scale: exit=1 command=re-scale enforce shortcuts --covenanted"
  #     A run truncated mid-hit-list reaches neither, so its final line is some
  #     hit-header/detail line => caught below.
  # rc and terminal signal must be COHERENT: rc=0 <=> no-markers sentinel,
  # rc=1 <=> trailer. A mismatch (e.g. rc=0 with a trailer, or rc=1 with the
  # no-markers sentinel) means a replayed/forged stream => exit 2.
  last_nonempty_line shortcuts_last "$shortcuts_out"
  local shortcuts_trailer="re-scale: exit=${shortcuts_rc} command=re-scale enforce shortcuts --covenanted"
  if [ "$shortcuts_rc" -eq 0 ]; then
    if [ "$shortcuts_last" != "No shortcut markers found" ]; then
      echo "covenant-gate: FAIL (exit 2) — 're-scale enforce shortcuts --covenanted' exited 0 but did not end with 'No shortcut markers found'." >&2
      echo "covenant-gate: actual final line: '$shortcuts_last' — truncated/forged stream, refusing partial result." >&2
      exit 2
    fi
    # Clean path: no hit lines to parse, nothing to cross-check.
  else
    # rc == 1 (hits): final line must be the completion trailer.
    if [ "$shortcuts_last" != "$shortcuts_trailer" ]; then
      echo "covenant-gate: FAIL (exit 2) — 're-scale enforce shortcuts --covenanted' exited 1 but did not end with its completion trailer." >&2
      echo "covenant-gate: expected final line: '$shortcuts_trailer'" >&2
      echo "covenant-gate: actual final line:   '$shortcuts_last'" >&2
      echo "covenant-gate: the run was truncated / did not complete (or rc was forged) — refusing partial result. shortcuts rc=$shortcuts_rc." >&2
      exit 2
    fi
    # (b) CROSS-CHECK: the "Total: <N> hits in <M> files" summary must be present
    # and its "<M> files" must equal the number of "<path>  (<k> hits)" hit-header
    # lines parsed below. A truncated hit list under-counts the headers vs the
    # summary => mismatch => exit 2. All matching via here-string, no pipeline.
    local shortcuts_total_line shortcuts_files_n shortcuts_parsed_n
    shortcuts_total_line="$(grep -E '^Total: [0-9]+ hits in [0-9]+ files$' <<<"$shortcuts_out" || true)"
    if [ -z "$shortcuts_total_line" ]; then
      echo "covenant-gate: FAIL (exit 2) — 're-scale enforce shortcuts --covenanted' reported hits (rc=1) but printed no 'Total: <N> hits in <M> files' summary; cannot cross-check hit-header count." >&2
      exit 2
    fi
    [[ "$shortcuts_total_line" =~ ^Total:\ [0-9]+\ hits\ in\ ([0-9]+)\ files$ ]]
    shortcuts_files_n="${BASH_REMATCH[1]}"
    shortcuts_parsed_n="$(count_lines_matching "$shortcuts_out" "^${REPO_ROOT}/.+  \([0-9]+ hits?\)$")"
    if [ "$shortcuts_files_n" != "$shortcuts_parsed_n" ]; then
      echo "covenant-gate: FAIL (exit 2) — shortcuts count mismatch: summary says '$shortcuts_files_n files' but $shortcuts_parsed_n hit-header line(s) were parsed." >&2
      echo "covenant-gate: the hit list was truncated or its format drifted — refusing partial result." >&2
      exit 2
    fi
  fi

  # --- verify --all ---
  # Failing entries are indented lines of the form "  <abs-path>: <reason>".
  printf '%s\n' "$verify_out" | while IFS= read -r line; do
    case "$line" in
      "  $REPO_ROOT"/*": "*)
        local rest path reason kind
        rest="${line#  }"                 # strip the 2-space indent
        path="${rest%%: *}"               # path is up to the first ": "
        reason="${rest#*: }"              # reason is the remainder
        path="${path#"$REPO_ROOT"/}"      # make repo-relative
        case "$reason" in
          "shortcuts introduced:"*)        kind="shortcut-drift" ;;
          "no covenant header")            kind="missing-header" ;;
          "methods removed since baseline:"*) kind="methods-removed" ;;
          *)                               kind="verify-other" ;;
        esac
        printf '%s\t%s\n' "$path" "$kind"
        ;;
    esac
  done

  # --- shortcuts --covenanted ---
  # Hit headers are non-indented lines of the form "<abs-path>  (N hits)".
  # "No shortcut markers found" produces nothing.
  printf '%s\n' "$shortcuts_out" | while IFS= read -r line; do
    case "$line" in
      "$REPO_ROOT"/*"  ("*"hits)")
        local path
        path="${line%%  (*}"
        path="${path#"$REPO_ROOT"/}"
        printf '%s\t%s\n' "$path" "covenanted-shortcut"
        ;;
    esac
  done

  # --- dup-covenant-header (ISS-486) ---
  # A double-stamping tool once applied the covenant / migration-notes block a
  # second time inside file header comments, leaving the canonical covenant
  # marker line ("* Covenant: ") duplicated (the dup_covenant_files ratchet
  # metric in remediation-baseline.tsv). After the ISS-486 dedupe NO file carries
  # more than one such marker, so this source contributes ZERO baseline rows; any
  # future file whose header re-doubles the marker becomes a NEW (file, kind) pair
  # not in the baseline and turns the gate red.
  #
  # Detection mirrors the committed ratchet command exactly: count occurrences of
  # the canonical marker line "* Covenant: " per .scala file under the covenanted
  # source roots; a count > 1 is a duplicate header. We scan the filesystem
  # directly (no re-scale dependency) so the check is deterministic. The roots
  # MUST exist — a missing root would silently yield zero dup rows (fail-open), so
  # we exit 2 if any expected root is absent.
  local dup_root
  local dup_roots=("sge/src" "sge-extension" "sge-jvm-platform")
  for dup_root in "${dup_roots[@]}"; do
    if [ ! -d "$REPO_ROOT/$dup_root" ]; then
      echo "covenant-gate: FAIL (exit 2) — dup-covenant-header scan root missing: $dup_root (cannot prove absence of duplicate covenant headers)." >&2
      exit 2
    fi
  done
  # `grep -rc <pat>` prints "<path>:<count>"; awk keeps files whose count > 1 and
  # strips the trailing ":<count>" so only the repo-relative path remains. Run
  # from REPO_ROOT so the emitted paths are already repo-relative.
  local dup_path
  while IFS= read -r dup_path; do
    [ -n "$dup_path" ] || continue
    printf '%s\t%s\n' "$dup_path" "dup-covenant-header"
  done < <(
    cd "$REPO_ROOT" &&
      grep -rc -- '\* Covenant: ' --include='*.scala' "${dup_roots[@]}" 2>/dev/null |
      awk -F: '$NF > 1 { sub(/:[0-9]+$/, "", $0); print }'
  )
}

# Canonicalize a set: drop blank lines, then sort+dedup under the C locale so
# the byte ordering is deterministic and identical for every input. Every set
# operation below consumes a stream produced by this function, which is the
# invariant `comm` relies on (both inputs sorted under the SAME collation).
canonicalize_set() {
  grep -v '^[[:space:]]*$' | LC_ALL=C sort -u
}

# Run compute_live_set as a PLAIN command substitution (no pipeline) so that the
# tool-error exit(2) it raises propagates as the substitution's exit status. If
# it were the left side of `compute_live_set | canonicalize_set`, its exit would
# be swallowed by the pipeline and we would silently fall through with an empty
# LIVE — re-introducing the very fail-open hole this guard closes. We capture the
# raw output, check the status, and only THEN canonicalize.
LIVE_RAW="$(compute_live_set)"
LIVE_RC=$?
if [ "$LIVE_RC" -ne 0 ]; then
  # compute_live_set already printed a specific tool-error diagnostic to stderr
  # before exiting; surface its status verbatim (2 = tool error) and stop.
  echo "covenant-gate: aborting — live-set computation reported a tool error (status $LIVE_RC); see message above." >&2
  exit "$LIVE_RC"
fi
LIVE="$(printf '%s\n' "$LIVE_RAW" | canonicalize_set)"
LIVE_COUNT="$(printf '%s' "$LIVE" | grep -c . || true)"

if [ "$MODE" = "generate" ]; then
  {
    echo "# covenant-gate baseline (ISS-483) — set of (file, kind) pairs currently failing"
    echo "# columns: file<TAB>kind   (file is repo-relative)"
    echo "# kinds: shortcut-drift | missing-header | methods-removed | verify-other | covenanted-shortcut | dup-covenant-header"
    echo "# regenerate: .rescale/scripts/covenant-gate.sh --generate"
    printf '%s\n' "$LIVE"
  } > "$BASELINE"
  echo "covenant-gate: wrote baseline with $LIVE_COUNT (file, kind) rows to $BASELINE"
  exit 0
fi

# ---- check mode ----
if [ ! -f "$BASELINE" ]; then
  echo "covenant-gate: FAIL — baseline file missing: $BASELINE" >&2
  echo "covenant-gate: run '.rescale/scripts/covenant-gate.sh --generate' to create it." >&2
  exit 1
fi

# Read committed baseline rows (skip comment lines), then canonicalize so it is
# sorted+deduped under the SAME collation as LIVE. Order in the committed file
# is irrelevant — the set comparison below is provably order-independent because
# both operands pass through canonicalize_set.
BASE="$(grep -v '^#' "$BASELINE" | canonicalize_set || true)"
BASE_COUNT="$(printf '%s' "$BASE" | grep -c . || true)"

# Defense-in-depth (ISS-483 bounce 2, acceptance item ii): the per-invocation
# output-shape guards above are the primary protection, but as a final backstop
# an empty live set while the baseline records failures is implausible. The
# baseline is a snapshot of failures that were live moments ago in this same
# tree; for ALL of them to vanish at once almost certainly means the verify
# tooling silently produced nothing rather than the tree genuinely going clean.
# Refuse (exit 2) rather than report 193 "shrinks" and PASS.
if [ "$LIVE_COUNT" -eq 0 ] && [ "$BASE_COUNT" -gt 0 ]; then
  echo "covenant-gate: FAIL (exit 2) — implausible empty live set — verify tooling: live failing set is empty while the baseline records $BASE_COUNT failure(s)." >&2
  echo "covenant-gate: a genuine tree would not clear every baselined failure at once; this signals the enforce tooling did not run or its output was not parsed." >&2
  exit 2
fi

# NEW = live \ baseline ; SHRINK = baseline \ live.
# Both `comm` operands are re-canonicalized at the point of comparison: this
# guarantees they are sorted under C collation (matching `comm`'s requirement)
# and that an empty operand yields zero records rather than one blank record
# (printf on an empty string would otherwise emit a spurious blank line that
# desyncs comm). comm itself runs under LC_ALL=C so its merge order matches.
NEW="$(LC_ALL=C comm -23 <(printf '%s\n' "$LIVE" | canonicalize_set) <(printf '%s\n' "$BASE" | canonicalize_set) || true)"
SHRINK="$(LC_ALL=C comm -13 <(printf '%s\n' "$LIVE" | canonicalize_set) <(printf '%s\n' "$BASE" | canonicalize_set) || true)"

NEW_COUNT="$(printf '%s' "$NEW" | grep -c . || true)"
SHRINK_COUNT="$(printf '%s' "$SHRINK" | grep -c . || true)"

echo "covenant-gate: live failing (file,kind) pairs: $LIVE_COUNT ; baselined: $BASE_COUNT"

if [ "$SHRINK_COUNT" -gt 0 ]; then
  echo "covenant-gate: $SHRINK_COUNT baselined pair(s) no longer failing (shrink — update the baseline):"
  printf '%s\n' "$SHRINK" | sed 's/^/  - /'
fi

if [ "$NEW_COUNT" -gt 0 ]; then
  echo "covenant-gate: FAIL — $NEW_COUNT NEW covenant/shortcut failure(s) not in baseline:" >&2
  printf '%s\n' "$NEW" | sed 's/^/  + /' >&2
  echo "covenant-gate: a covenanted file regressed (new stub/shortcut, dropped method, or lost header)." >&2
  echo "covenant-gate: fix the file, or — if intentional — re-baseline via the orchestrator." >&2
  exit 1
fi

echo "covenant-gate: PASS — 0 new failures, $BASE_COUNT baselined."
exit 0
