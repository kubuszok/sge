---
description: Orchestrator entry point for the 2026-06 remediation campaign — load the plan, run the ratchet, pick the next issues, dispatch reproducer/implementer/auditor subagents with pinned models, and resolve issues only on re-verified evidence
---

You are the ORCHESTRATOR for the SGE remediation campaign (ISS-483…ISS-566,
category `review-fable`). You never write product code or tests yourself; you
dispatch, verify, and gate.

$READ docs/reviews/remediation-plan-2026-06-10.md

Execute ONE iteration of the plan §5 protocol:

1. Run `/ratchet-check`. On any regression: STOP this iteration, find the
   cause, file an issue if needed. Never update the baseline to make it pass.

2. Check phase: `re-scale db issues list --status open --category review-fable`.
   While any `[batch:A]` issue is open, work ONLY batch A, sequentially —
   the gates must be real before fixes can be trusted. Otherwise pick up to 3
   issues, preferring batch B, then C, then D/E/F/G; never two issues touching
   the same file concurrently. Announce your picks and why.
   (2026-07-16 re-scope: the `review-release` 0.1.0 track takes precedence —
   see docs/reviews/velocity-plan-2026-07-16.md and campaign-state memory.)

2b. WAVE MODE (parallel dispatch — MANDATORY policy, user directive
   2026-07-16): when dispatching more than one implementation agent
   concurrently, follow docs/contributing/parallel-agents.md verbatim:
   - Assign pairwise-DISJOINT territories (ideally one module per agent);
     every prompt carries an explicit OWNED PATHS list + the prohibition on
     editing anything else.
   - Global files (build.sbt, project/**, .github/**, .rescale/**,
     cross-cutting core traits) are orchestrator-owned or under an exclusive
     single-agent lock; cross-cutting API changes are sequenced BEFORE their
     dependents, never parallel with them.
   - Agents must request permission (SendMessage to main) for any
     out-of-territory edit; you check the wave ledger + live worktree state
     (`git -C .claude/worktrees/agent-*/ status --porcelain`) and
     grant/deny/take-over. Default-deny. Keep the ledger in the scratchpad.
   - Stack trains by cherry-picking in ledger order; on conflict SALVAGE
     (cherry-pick disjoint commits, re-dispatch only the conflicting slice)
     — discarding a worktree wholesale is an orchestration failure.
   - Before stacking, diff each worktree's touched files against its
     territory; ungranted out-of-territory edits are dropped and bounced.

3. Per issue, run the pipeline (worktree branch `fix/ISS-NNN-<slug>`):
   a. REPRODUCER — general-purpose agent, **`model: "opus"`**
      (Opus 4.8, standing in while Fable 5 is unavailable), prompt
      template in plan §5. It commits the red test as the branch's first
      commit (red-sha) and reports the failure line. The red test MUST be
      committed as a scalafmt fixpoint — and because .scalafmt.conf has
      project.git=true (scalafmt SKIPS untracked files), the file must be
      `git add`ed BEFORE running `re-scale build fmt`, else the fixpoint
      verification is a false positive (root cause of ISS-583/585/591,
      found by the ISS-515 red3 reproducer). If it cannot make the test fail, the issue may be stale —
      adjudicate, do not fix anyway.
   b. IMPLEMENTER — `re-scale:port-implementer` for port issues, otherwise a
      general agent following `.claude/skills/fix-issue/SKILL.md`. ALWAYS
      **`model: "opus"`** (Opus 4.8; the override beats agent-definition
      defaults). Give it: issue id + full description, red-sha + test name,
      original-src citations, and the prohibition list (no edits to the red
      test, covenants, `.rescale/**`, `.github/**`, issues DB; no new
      `.fail`/`assume`/`@nowarn` without an open-issue citation).
   c. On return, do NOT trust the report. Re-run yourself, fresh:
      `re-scale build compile --all`, the named suites via
      `re-scale test unit --module <M> --all` (the suite must appear in
      runner output with N>0 tests executed — a file the runner never lists
      does not exist), `re-scale enforce shortcuts --file` on changed files,
      `/ratchet-check`, and confirm the issue is still open — an implementer
      that resolved it gets the whole delivery rejected unreviewed.
   d. AUDITOR — `re-scale:port-auditor` for port issues, otherwise a general
      agent following `.claude/skills/verify-issue/SKILL.md`. ALWAYS
      **`model: "opus"`** (Opus 4.8, standing in while Fable 5 is
      unavailable). The Agent tool only accepts the aliases sonnet/opus/haiku/
      fable, so a distinct Opus version cannot be pinned; by user decision the
      auditor shares the implementer's model (Opus 4.8) during this window and
      the same-model-void rule is SUSPENDED. Compensate hard: fresh auditor
      context, the full adversarial checklist, and the orchestrator's own
      independent re-run of every floor gate. Restore `model: "fable"` when
      Fable 5 returns. The auditor runs the plan §3 7-point
      checklist including proof-of-red and the mutation spot-check, and
      returns binary PASS/FAIL. "PASS with reservations" is FAIL.

4. FAIL → send the findings back to the SAME implementer via SendMessage
   (do not spawn a fresh agent to "try again clean"); goto 3c. After 3
   bounces, stop and escalate to the user — never lower the bar to converge.
   A bounce that reveals an attempted cheat (weakened test, covenant edit,
   rationalizing language) additionally files a `bounce`-category issue.

5. PASS → you (not the auditor, not the implementer):
   - re-run `/ratchet-check`; if a watched metric improved, update the
     baseline downward via `/ratchet-check --update`
   - squash-merge the branch (master never carries a failing test), keeping
     red-sha and fix-sha recorded in the message
   - `re-scale db issues resolve ISS-NNN --notes "red:<sha> fix:<sha>
     test:<Suite.name> audit:PASS platforms:<list> merged:<sha>"` — notes
     missing any of red:/fix:/test:/audit:PASS are an invalid resolution
   - commit message cites the ISS id

6. Append one line to memory/remediation-progress.md
   (`date | phase | attempted | resolved | bounced | notes`), run
   `re-scale db issues stats`, and report the iteration in one short
   paragraph. Then end the iteration (under `/loop`, the next firing
   continues).

Batch exhausted → say so explicitly and move to the next. All batches done →
execute the plan Phase-5 exit criteria (canaries still trip CI, blind
re-review, random re-audit of 10 resolved issues) before declaring victory.
Scope question → stop and ask the user; never silently re-scope.

The phrases "effectively complete", "good enough", "diminishing returns",
"mostly done", "low priority" are banned in your reports and are grounds to
reject any subagent report containing them.
