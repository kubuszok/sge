#!/bin/bash
#
# covenant-gate.sh — set-based ratchet gate for the covenant/shortcut checks
# (ISS-483; baseline-growth / count-ceiling / deletion guards: ISS-700).
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
# Baseline growth, per-pair count ceilings, deletions (ISS-700)
# -------------------------------------------------------------
# The set ratchet alone had three growth holes, each closed by a guard that
# runs in check mode BEFORE the expensive live-set computation (a padded or
# gutted baseline must be rejected regardless of what the enforce tooling
# would report):
#
# (a) BASELINE GROWTH needs EXPLICIT APPROVAL. Previously a commit could
#     append its own brand-new failing (file, kind) pair to the committed
#     baseline in the same change that introduces the failure — the pair then
#     looked "already baselined" and the gate PASSED. Now the gate diffs the
#     baseline it is about to trust against the baseline at the MERGE-BASE
#     with master (origin/master, falling back to a local master ref). Any
#     GROWTH — a new (file, kind) pair, a raised per-pair count ceiling, a
#     removed count ceiling, or the removal of a row whose file was deleted
#     too — FAILS (exit 1) unless BOTH hold:
#       * the working-tree baseline is identical to HEAD's (an uncommitted
#         baseline edit has no commit message to audit, so growth in it can
#         never be approved), AND
#       * EVERY commit in <merge-base>..HEAD that touches the baseline file
#         carries the approval marker
#             covenant-baseline-approved: <reason>
#         in its commit message.
#     Why a commit-message marker: it lives in immutable commit METADATA, not
#     in the diff, so it cannot ride along silently inside a file change — the
#     author must state the approval out loud where `git log`, the PR review
#     page, and this gate's CI log all surface it, and
#     `git log --grep covenant-baseline-approved:` enumerates every approval
#     ever granted. Baseline SHRINK (a pair removed while its file still
#     exists, a ceiling lowered, or a ceiling added to a legacy row) is a
#     tightening and needs no approval.
#     Merge-base resolution: on CI push builds origin/master is the checked-out
#     ref itself; on CI pull_request builds (shallow, merge-ref-only checkout)
#     the guard fetches origin master — and unshallows if needed — to resolve
#     it, and FAILS CLOSED (exit 2) if it still cannot: a gate that cannot see
#     the approved baseline must not guess. OUTSIDE CI a missing merge-base
#     only prints a WARN and skips THIS guard (local scratch trees may lack a
#     master ref; CI remains authoritative).
#
# (b) PER-PAIR COUNT CEILING. The (file, kind) set alone let an
#     already-baselined file regress FURTHER silently (e.g. a file baselined
#     with 3 covenanted-shortcut hits growing to 4 — same pair, so the set
#     comparison stayed green). Baseline rows now carry an optional third
#     column: the pair's failure COUNT (hits for shortcut kinds, method count
#     for methods-removed, 1 for presence-only kinds). A live count HIGHER
#     than the baselined ceiling FAILS (exit 1) listing the pairs. Legacy
#     2-column rows carry no ceiling and stay pair-ratcheted only;
#     `--generate` writes the count column, so the count ratchet arms as the
#     baseline is regenerated (adding a ceiling is a tightening — no approval
#     needed; RAISING one is growth under guard (a)).
#
# (c) DELETION IS NOT A SHRINK. Deleting a covenanted, baselined file made
#     its pairs vanish from the live set, which read as a shrink → PASS. Now
#     every file named in the baseline must still exist in the working tree;
#     a missing file FAILS (exit 1). Legitimately deleting such a file
#     requires removing its baseline rows, and removing rows together with
#     their file is GROWTH under guard (a) — i.e. it needs the commit-message
#     approval marker, so a covenant can never disappear silently.
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

# Repo-relative baseline path: guard (a) needs it for `git show <sha>:<path>`
# and `git log -- <path>` lookups, which take repo-relative pathspecs.
BASELINE_REL=".rescale/data/covenant-gate-baseline.tsv"
BASELINE="$REPO_ROOT/$BASELINE_REL"

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
# Emits the live failing set as TAB-separated "<relative-path>\t<kind>\t<count>"
# rows on stdout, one per line, unsorted. The count is the pair's failure count
# (hits for shortcut kinds, method count for methods-removed, 1 for
# presence-only kinds) and feeds the ISS-700 per-pair count-ceiling guard.
# Diagnostics go to stderr. On any tool-error
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
        local rest path reason kind count
        rest="${line#  }"                 # strip the 2-space indent
        path="${rest%%: *}"               # path is up to the first ": "
        reason="${rest#*: }"              # reason is the remainder
        path="${path#"$REPO_ROOT"/}"      # make repo-relative
        # count defaults to 1 (presence-only kinds); numeric kinds extract it
        # from the reason text via pure parameter expansion (no pipeline).
        count=1
        case "$reason" in
          "shortcuts introduced: "*)
            kind="shortcut-drift"
            count="${reason#shortcuts introduced: }"  # "N hit(s), e.g. ..."
            count="${count%% *}"                      # leading "N"
            ;;
          "no covenant header")            kind="missing-header" ;;
          "methods removed since baseline: "*)
            kind="methods-removed"
            count="${reason#methods removed since baseline: }"
            count="${count%% *}"
            ;;
          *)                               kind="verify-other" ;;
        esac
        # A count we cannot parse means the reason format drifted; fall back to
        # the presence marker 1 rather than emitting a malformed row (the pair
        # ratchet still applies; the ceiling just is not raised/checked).
        if ! [[ "$count" =~ ^[0-9]+$ ]]; then count=1; fi
        printf '%s\t%s\t%s\n' "$path" "$kind" "$count"
        ;;
    esac
  done

  # --- shortcuts --covenanted ---
  # Hit headers are non-indented lines of the form "<abs-path>  (N hits)".
  # "No shortcut markers found" produces nothing.
  printf '%s\n' "$shortcuts_out" | while IFS= read -r line; do
    case "$line" in
      "$REPO_ROOT"/*"  ("*"hits)")
        local path count
        path="${line%%  (*}"
        path="${path#"$REPO_ROOT"/}"
        count="${line##*  (}"             # "N hits)"
        count="${count%% *}"              # "N"
        if ! [[ "$count" =~ ^[0-9]+$ ]]; then count=1; fi
        printf '%s\t%s\t%s\n' "$path" "covenanted-shortcut" "$count"
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
    printf '%s\t%s\t1\n' "$dup_path" "dup-covenant-header"
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

# in_ci — true on GitHub Actions (or any CI that exports CI=...). Guard (a)
# fails CLOSED on an unresolvable merge-base in CI but only WARNs locally.
in_ci() {
  [ "${GITHUB_ACTIONS:-}" = "true" ] || [ -n "${CI:-}" ]
}

# validate_baseline_rows  (ISS-700)
# ---------------------------------
# Structural check of the committed baseline BEFORE anything trusts it. Rows
# must be "file<TAB>kind" (legacy) or "file<TAB>kind<TAB>count" with a numeric
# count. A malformed row (missing kind, extra columns, non-numeric count)
# would silently fall out of the set/ceiling comparisons — fail-open — so it
# is a hard tool error (exit 2), not a policy failure. Blank lines were
# already dropped by canonicalize_set; the awk blank-guard only covers the
# one synthetic blank a here-fed EMPTY set produces.
validate_baseline_rows() {
  local bad
  bad="$(awk -F'\t' '
    $0 ~ /^[[:space:]]*$/ { next }
    NF < 2 || NF > 3 || (NF == 3 && $3 !~ /^[0-9]+$/) { printf "  ? %s\n", $0 }
  ' <<<"$BASE" || true)"
  [ -z "$bad" ] && return 0
  echo "covenant-gate: FAIL (exit 2) — malformed row(s) in $BASELINE_REL (expected 'file<TAB>kind' or 'file<TAB>kind<TAB>count' with numeric count):" >&2
  printf '%s\n' "$bad" >&2
  echo "covenant-gate: a row the comparisons cannot parse would silently escape the ratchet — refusing to run against a malformed baseline." >&2
  exit 2
}

# guard_baseline_growth  (ISS-700 guard (a))
# ------------------------------------------
# Rejects UNAPPROVED baseline growth: the baseline this run is about to trust
# is diffed against the baseline at the merge-base with master. Growth =
#   * a (file, kind) pair not present at the merge-base, or
#   * a count ceiling raised or removed on a pair present at the merge-base, or
#   * a row removed together with its file (covenant deletion — see guard (c)).
# Growth passes only when the working-tree baseline equals HEAD's AND every
# commit in <merge-base>..HEAD touching the baseline carries the
# 'covenant-baseline-approved: <reason>' marker in its message (see header).
# Shrinks and added ceilings are tightenings and pass without approval.
guard_baseline_growth() {
  local base_sha="" ref
  for ref in origin/master master; do
    git -C "$REPO_ROOT" rev-parse --verify --quiet "$ref^{commit}" >/dev/null 2>&1 || continue
    base_sha="$(git -C "$REPO_ROOT" merge-base "$ref" HEAD 2>/dev/null || true)"
    [ -n "$base_sha" ] && break
  done

  if [ -z "$base_sha" ] && in_ci; then
    # CI pull_request checkouts are shallow and carry only the synthetic
    # refs/pull/N/merge ref — no origin/master, no parent history. Fetch the
    # master ref (and, on a shallow clone, the full history of HEAD by sha —
    # GitHub serves reachable sha wants) so the ancestry walk can reach the
    # fork point. On push builds origin/master is the checked-out ref itself
    # and this branch is never taken. `|| true`: a failed fetch falls through
    # to the fail-closed exit below with its own diagnostic.
    echo "covenant-gate: baseline-growth guard: no master merge-base in this checkout; fetching origin master to resolve it (CI)." >&2
    if [ "$(git -C "$REPO_ROOT" rev-parse --is-shallow-repository 2>/dev/null)" = "true" ]; then
      git -C "$REPO_ROOT" fetch --quiet --no-tags --unshallow origin \
        "+refs/heads/master:refs/remotes/origin/master" "$(git -C "$REPO_ROOT" rev-parse HEAD)" >&2 || true
    else
      git -C "$REPO_ROOT" fetch --quiet --no-tags origin \
        "+refs/heads/master:refs/remotes/origin/master" >&2 || true
    fi
    base_sha="$(git -C "$REPO_ROOT" merge-base origin/master HEAD 2>/dev/null || true)"
  fi

  if [ -z "$base_sha" ]; then
    if in_ci; then
      echo "covenant-gate: FAIL (exit 2) — baseline-growth guard could not resolve a merge-base with master in CI (even after fetching origin master)." >&2
      echo "covenant-gate: without the merge-base the gate cannot distinguish approved baseline rows from rows the change under test granted itself — failing closed." >&2
      echo "covenant-gate: fix the checkout (e.g. actions/checkout with fetch-depth: 0) or the network and re-run." >&2
      exit 2
    fi
    # LOCAL fallback (documented in the header): scratch trees may genuinely
    # lack any master ref. Skip ONLY this guard — CI stays authoritative and
    # fail-closed, so the growth policy cannot be dodged by where you run.
    echo "covenant-gate: WARN — no merge-base with origin/master or master; SKIPPING the baseline-growth guard for this LOCAL run." >&2
    echo "covenant-gate: baseline growth is still enforced authoritatively in CI, where an unresolvable merge-base fails closed." >&2
    return 0
  fi

  # Baseline as approved at the merge-base. Absent there (brand-new baseline)
  # => empty set => every current row is growth and needs approval.
  local mb_rows
  mb_rows="$(git -C "$REPO_ROOT" show "$base_sha:$BASELINE_REL" 2>/dev/null | grep -v '^#' | canonicalize_set || true)"

  local cur_pairs mb_pairs added_pairs removed_pairs
  cur_pairs="$(printf '%s\n' "$BASE" | cut -f1,2 | canonicalize_set || true)"
  mb_pairs="$(printf '%s\n' "$mb_rows" | cut -f1,2 | canonicalize_set || true)"
  added_pairs="$(LC_ALL=C comm -13 <(printf '%s\n' "$mb_pairs" | canonicalize_set) <(printf '%s\n' "$cur_pairs" | canonicalize_set) || true)"
  removed_pairs="$(LC_ALL=C comm -23 <(printf '%s\n' "$mb_pairs" | canonicalize_set) <(printf '%s\n' "$cur_pairs" | canonicalize_set) || true)"

  local growth="" row f k
  while IFS= read -r row; do
    [ -n "$row" ] || continue
    growth+="  + $row — new (file, kind) pair"$'\n'
  done <<<"$added_pairs"

  # Raised/removed count ceilings on pairs present on both sides. awk keys on
  # "file FS kind"; a merge-base row without a numeric count carries no
  # ceiling, so nothing on it can be "raised". Current-side rows were already
  # structurally validated (validate_baseline_rows).
  local ceiling_growth
  ceiling_growth="$(awk -F'\t' '
    $0 ~ /^[[:space:]]*$/ { next }
    NR==FNR { if (NF >= 3 && $3 ~ /^[0-9]+$/) mb[$1 FS $2] = $3; next }
    {
      key = $1 FS $2
      if (!(key in mb)) next
      if (NF < 3)                printf "  + %s\t%s — count ceiling removed (was %s)\n", $1, $2, mb[key]
      else if ($3+0 > mb[key]+0) printf "  + %s\t%s — count ceiling raised %s -> %s\n", $1, $2, mb[key], $3
    }' <(printf '%s\n' "$mb_rows") <(printf '%s\n' "$BASE") || true)"
  if [ -n "$ceiling_growth" ]; then growth+="$ceiling_growth"$'\n'; fi

  # Rows removed together with their file: covenant deletion, growth-class.
  # (Rows removed while the file still exists are a genuine shrink — pass.)
  while IFS=$'\t' read -r f k; do
    [ -n "$f" ] || continue
    if [ ! -e "$REPO_ROOT/$f" ]; then
      growth+="  + $f"$'\t'"$k — row removed together with its file (covenant deletion)"$'\n'
    fi
  done <<<"$removed_pairs"

  growth="$(grep -v '^[[:space:]]*$' <<<"$growth" | LC_ALL=C sort || true)"
  if [ -z "$growth" ]; then
    echo "covenant-gate: baseline unchanged or tightened vs merge-base ${base_sha:0:9} — no growth to approve."
    return 0
  fi

  # Growth exists — audit the approval. An uncommitted baseline edit can never
  # be approved: there is no commit message to carry the marker.
  local dirty=0
  git -C "$REPO_ROOT" diff --quiet HEAD -- "$BASELINE_REL" 2>/dev/null || dirty=1

  local touching unapproved="" approving="" c msg subj
  touching="$(git -C "$REPO_ROOT" log --format=%H "$base_sha..HEAD" -- "$BASELINE_REL" 2>/dev/null || true)"
  while IFS= read -r c; do
    [ -n "$c" ] || continue
    msg="$(git -C "$REPO_ROOT" log -1 --format=%B "$c" 2>/dev/null || true)"
    subj="$(git -C "$REPO_ROOT" log -1 --format='%h %s' "$c" 2>/dev/null || true)"
    # Substring test via `case` — pure bash, pipe-free (ISS-569 discipline).
    case "$msg" in
      *"covenant-baseline-approved:"*) approving+="  * $subj"$'\n' ;;
      *)                               unapproved+="  ! $subj"$'\n' ;;
    esac
  done <<<"$touching"

  if [ "$dirty" -eq 0 ] && [ -n "$approving" ] && [ -z "$unapproved" ]; then
    echo "covenant-gate: baseline GROWTH vs merge-base ${base_sha:0:9} is APPROVED ('covenant-baseline-approved:' marker present):"
    printf '%s\n' "$growth"
    echo "covenant-gate: approving commit(s):"
    printf '%s' "$approving"
    return 0
  fi

  echo "covenant-gate: FAIL — baseline GROWTH vs merge-base ${base_sha:0:9} without explicit approval:" >&2
  printf '%s\n' "$growth" >&2
  if [ "$dirty" -ne 0 ]; then
    echo "covenant-gate: the working-tree baseline differs from HEAD — uncommitted baseline growth can never be approved (no commit message to audit)." >&2
  fi
  if [ -n "$unapproved" ]; then
    echo "covenant-gate: baseline-touching commit(s) WITHOUT the approval marker:" >&2
    printf '%s' "$unapproved" >&2
  fi
  if [ "$dirty" -eq 0 ] && [ -z "$touching" ]; then
    echo "covenant-gate: no commit in ${base_sha:0:9}..HEAD touches $BASELINE_REL yet its content grew — rename/rewrite trickery; refusing to trust it." >&2
  fi
  echo "covenant-gate: growing the baseline (new pair, raised/removed count ceiling, or rows removed with their file) requires the marker" >&2
  echo "covenant-gate:   covenant-baseline-approved: <reason>" >&2
  echo "covenant-gate: in the message of EVERY commit that edits $BASELINE_REL since the merge-base." >&2
  exit 1
}

# guard_missing_files  (ISS-700 guard (c))
# ----------------------------------------
# Every file the baseline names must still exist in the working tree. Without
# this, deleting a covenanted file removed its pairs from the LIVE set only —
# which read as a shrink and PASSED. Deletion must instead be made explicit:
# remove the baseline row(s) in a commit carrying the approval marker, which
# guard (a) audits (row removed together with its file = growth-class).
guard_missing_files() {
  local f missing="" missing_n=0
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    if [ ! -e "$REPO_ROOT/$f" ]; then
      missing+="  - $f"$'\n'
      missing_n=$((missing_n + 1))
    fi
  done < <(printf '%s\n' "$BASE" | cut -f1 | canonicalize_set || true)
  [ -z "$missing" ] && return 0
  echo "covenant-gate: FAIL — $missing_n baselined covenanted file(s) missing from the working tree (deleting a covenanted file is NOT a shrink):" >&2
  printf '%s' "$missing" >&2
  echo "covenant-gate: restore the file(s), or make the deletion explicit: remove their baseline row(s) in a commit whose message carries 'covenant-baseline-approved: <reason>' (guard (a) audits that removal)." >&2
  exit 1
}

# ---- check-mode structural guards (ISS-700) ----
# These need only git and the filesystem, so they run BEFORE the expensive
# live-set computation: a padded, loosened, or gutted baseline must be
# rejected no matter what the enforce tooling would go on to report.
BASE=""
BASE_PAIRS=""
BASE_COUNT=0
if [ "$MODE" = "check" ]; then
  if [ ! -f "$BASELINE" ]; then
    echo "covenant-gate: FAIL — baseline file missing: $BASELINE" >&2
    echo "covenant-gate: run '.rescale/scripts/covenant-gate.sh --generate' to create it." >&2
    exit 1
  fi
  # Read committed baseline rows (skip comment lines), then canonicalize so it
  # is sorted+deduped under the SAME collation as LIVE. Order in the committed
  # file is irrelevant — every set comparison below is order-independent
  # because both operands pass through canonicalize_set.
  BASE="$(grep -v '^#' "$BASELINE" | canonicalize_set || true)"
  BASE_PAIRS="$(printf '%s\n' "$BASE" | cut -f1,2 | canonicalize_set || true)"
  BASE_COUNT="$(printf '%s' "$BASE_PAIRS" | grep -c . || true)"
  validate_baseline_rows # malformed row => tool error (exit 2)
  guard_baseline_growth  # ISS-700 (a): unapproved growth vs master merge-base => exit 1
  guard_missing_files    # ISS-700 (c): baselined file deleted from the tree  => exit 1
fi

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
# LIVE rows are "file<TAB>kind<TAB>count"; LIVE_PAIRS strips the count column
# for the set comparisons (the committed baseline may hold legacy 2-column
# rows, so sets are always compared at (file, kind) granularity and counts
# are compared separately by the ceiling guard).
LIVE="$(printf '%s\n' "$LIVE_RAW" | canonicalize_set)"
LIVE_PAIRS="$(printf '%s\n' "$LIVE" | cut -f1,2 | canonicalize_set || true)"
LIVE_COUNT="$(printf '%s' "$LIVE_PAIRS" | grep -c . || true)"

if [ "$MODE" = "generate" ]; then
  {
    echo "# covenant-gate baseline (ISS-483, ISS-700) — set of (file, kind, count) rows currently failing"
    echo "# columns: file<TAB>kind<TAB>count   (file is repo-relative; count = per-pair failure-count ceiling)"
    echo "# legacy 2-column rows (no count) are accepted: pair-ratcheted only, no count ceiling"
    echo "# kinds: shortcut-drift | missing-header | methods-removed | verify-other | covenanted-shortcut | dup-covenant-header"
    echo "# growth vs the master merge-base (new pair, raised/removed ceiling, rows removed with their file)"
    echo "# requires 'covenant-baseline-approved: <reason>' in every baseline-editing commit message (ISS-700)"
    echo "# regenerate: .rescale/scripts/covenant-gate.sh --generate"
    printf '%s\n' "$LIVE"
  } > "$BASELINE"
  echo "covenant-gate: wrote baseline with $LIVE_COUNT (file, kind, count) rows to $BASELINE"
  exit 0
fi

# ---- check mode ----
# ($BASE / $BASE_PAIRS / $BASE_COUNT were read — and the ISS-700 structural
# guards enforced — before the live-set computation above.)

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
NEW="$(LC_ALL=C comm -23 <(printf '%s\n' "$LIVE_PAIRS" | canonicalize_set) <(printf '%s\n' "$BASE_PAIRS" | canonicalize_set) || true)"
SHRINK="$(LC_ALL=C comm -13 <(printf '%s\n' "$LIVE_PAIRS" | canonicalize_set) <(printf '%s\n' "$BASE_PAIRS" | canonicalize_set) || true)"

NEW_COUNT="$(printf '%s' "$NEW" | grep -c . || true)"
SHRINK_COUNT="$(printf '%s' "$SHRINK" | grep -c . || true)"

# ISS-700 guard (b): per-pair count ceilings. For every baseline row that
# carries a count, the live count for the same (file, kind) pair must not
# exceed it — an already-baselined failure getting WORSE (more hits, more
# methods removed) is a regression the pair-set comparison cannot see. Legacy
# 2-column rows carry no ceiling and are skipped (pair-ratchet only). awk keys
# on "file FS kind"; both inputs are here-fed process substitutions, no
# SIGPIPE-able pipeline (ISS-569 discipline).
REGRESSED="$(awk -F'\t' '
  $0 ~ /^[[:space:]]*$/ { next }
  NR==FNR { if (NF >= 3 && $3 ~ /^[0-9]+$/) ceil[$1 FS $2] = $3; next }
  {
    key = $1 FS $2
    if (key in ceil && NF >= 3 && $3+0 > ceil[key]+0)
      printf "  + %s\t%s — live count %s exceeds baselined ceiling %s\n", $1, $2, $3, ceil[key]
  }' <(printf '%s\n' "$BASE") <(printf '%s\n' "$LIVE") || true)"
REGRESSED_COUNT="$(printf '%s' "$REGRESSED" | grep -c . || true)"

echo "covenant-gate: live failing (file,kind) pairs: $LIVE_COUNT ; baselined: $BASE_COUNT"

if [ "$SHRINK_COUNT" -gt 0 ]; then
  echo "covenant-gate: $SHRINK_COUNT baselined pair(s) no longer failing (shrink — update the baseline):"
  printf '%s\n' "$SHRINK" | sed 's/^/  - /'
fi

# Report BOTH failure classes before exiting so one cannot mask the other.
GATE_FAILED=0

if [ "$NEW_COUNT" -gt 0 ]; then
  echo "covenant-gate: FAIL — $NEW_COUNT NEW covenant/shortcut failure(s) not in baseline:" >&2
  printf '%s\n' "$NEW" | sed 's/^/  + /' >&2
  echo "covenant-gate: a covenanted file regressed (new stub/shortcut, dropped method, or lost header)." >&2
  echo "covenant-gate: fix the file, or — if intentional — re-baseline via the orchestrator." >&2
  GATE_FAILED=1
fi

if [ "$REGRESSED_COUNT" -gt 0 ]; then
  echo "covenant-gate: FAIL — $REGRESSED_COUNT baselined pair(s) regressed past their recorded count ceiling:" >&2
  printf '%s\n' "$REGRESSED" >&2
  echo "covenant-gate: an already-baselined failure got worse — fix the file back to (or below) its ceiling;" >&2
  echo "covenant-gate: raising a ceiling on purpose is baseline GROWTH and needs the guard (a) approval marker." >&2
  GATE_FAILED=1
fi

if [ "$GATE_FAILED" -ne 0 ]; then
  exit 1
fi

echo "covenant-gate: PASS — 0 new failures, 0 count-ceiling regressions, $BASE_COUNT baselined."
exit 0
