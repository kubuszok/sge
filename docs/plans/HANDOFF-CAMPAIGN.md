# SGE remediation campaign — live handoff state

Purpose: an Opus-tier orchestrator must be able to resume from this file +
`/goal` alone. Constitution: `docs/reviews/remediation-plan-2026-06-10.md`.
Ledger: `memory/remediation-progress.md`. Sibling SSG handoff:
`../ssg/docs/plans/HANDOFF-CAMPAIGN.md`.

## Iteration 2 result (2026-07-01): ALL 4 bounce records CLOSED

All four verification audits returned PASS with mutation-kill evidence; resolve
notes carry the full chains (commit 6d0633fe). `review-fable` = 0 AND `bounce`
= 0 → **Phase-5 exit criteria are the remaining campaign work**:
- Item 5 (random re-audit of 10 resolved issues): IN FLIGHT since 2026-07-01
  (two Opus 4.8 auditors, 5 issues each, worktree-isolated, re-executing
  resolve-note evidence: red-sha fails / fix-sha passes / red..fix diff empty).
  Any failure ⇒ reopen the issue + sweep that implementer's other resolutions.
- Item 4 (fresh blind adversarial re-review): NEXT SGE iteration — multi-agent,
  reviewers get module lists only (NOT what was fixed), fresh contexts; during
  the Fable window run reviewers on `model: "fable"`. Zero critical findings
  required.
- Items 2+3 (blocking enforce CI on master; canary branches trip CI red):
  require the `more-improvements-2` → master merge first, which is sanctioned
  by ../SGE_SSG_SBT2_HANDOFF.md (PR → CI green → merge per repo process — the
  dedicated agents' merge authority transferred to this takeover). Treat as its
  own iteration; CI roundtrips are slow.
- Old worktrees under .claude/worktrees/ (fix/ISS-558/559/560/561/562,
  canary/iss587, fix/phase5-cleanup) predate the takeover — verify branches
  merged before removing; canary branches are NEEDED for exit item 3.

## Original state assessment (2026-07-01, iteration 2 start)

- Branch `more-improvements-2`. **review-fable queue: ZERO open.** Campaign is
  at the Phase-5 exit-criteria stage, blocked by criterion 1's parenthetical:
  4 open `bounce`-category issues (ISS-620, ISS-638, ISS-646, ISS-650).
- Ratchet at iteration start: CLEAN. Improvements pending fold-in after next
  PASS: `ignored_tests` 2→1, `exception_swallows_main` 40→37. `covenant_*`
  metrics carried (recompute via `re-scale enforce verify --all` only when
  covenanted files are touched — takes minutes).
- **In flight: 4 parallel verification audits** (Opus 4.8, isolated worktrees,
  mutation probes) — one per bounce issue, each verifying the recorded defect
  is genuinely gone at current HEAD:
  - ISS-620 → SaveDataCodecSuite: load-only decode + genuine inline-field
    LibGDX wire shape must be covered; mutations severing registration/decode
    must turn tests red.
  - ISS-638 → BlenderShapeKeys number semantics vs JsonReader.java:220-263 +
    JsonValue.java:387-415, pinned tests for 1e2/-0.0/10.50.
  - ISS-646 → LinkEffect covenant integrity + TypingLabel.sgeContext writers;
    mutation severing markup wiring must fail tests.
  - ISS-650 → PathFinderQueue covenant legitimacy + the never-performed
    content audit of the ISS-530 timepiece change.
- On PASS verdicts: orchestrator resolves the bounce issue with
  `red:n/a(bounce-record) fix:<parent-final-sha> test:<Suite> audit:PASS` style
  notes citing the verifying evidence. On FAIL: file a fix issue, dispatch
  implementer next iteration (red-commit protocol applies).
- **After all 4 close → Phase-5 exit criteria** (plan §Phase 5):
  1. review-fable + bounce queues empty; 2. `enforce verify --all` +
  `shortcuts --covenanted` clean as BLOCKING CI on master; 3. canary branches
  (stub-file, broken-GL) still turn CI red; 4. fresh adversarial re-review
  (multi-agent, reviewers told modules only, NOT what was fixed) with zero
  critical findings; 5. random re-audit of 10 resolved issues re-executing
  resolve-note evidence. Items 3+4+5 are multi-agent work — during the Fable
  window (through 2026-07-06) reviewers may run on `model: "fable"`; after it,
  Opus 4.8 with fresh contexts.
- CI note: the 99 open non-campaign issues (fidelity/infra/port-gap/hardening/
  test_gap/bug/…) are BACKLOG, not campaign scope. Do not pull them into /goal
  iterations without a user scope decision.
- Untracked `build.sbt.semanticdb` at repo root: pre-existing stray artifact,
  not campaign output; left untouched pending a db-hygiene decision.

## Model routing (this takeover window)

- SGE plan already runs all roles on `model: "opus"` (4.8) with same-model-void
  SUSPENDED (user decision, compensations: fresh contexts, full adversarial
  checklist, orchestrator re-runs gates). Restore `model: "fable"` for
  reproducer/auditor only if a durable Fable becomes available — the current
  window ends 2026-07-06 and mid-campaign flip-flops are not worth it.
- Verification auditors this iteration run Opus 4.8 in worktree isolation.
- Never dispatch versioned model IDs via the Agent tool `model` param (enum
  only); frontmatter is the only pin. Probe availability with
  `claude --model <id> -p "Reply OK" --output-format json` → `modelUsage`.

## Cleanup checklist per iteration (memory hygiene)

- Worktree auditors spawn their own sbt servers: after retiring each agent,
  `re-scale proc kill --kind sbt --dir <worktree>` then `git worktree remove`
  + `git worktree prune`. NEVER touch sbt servers under ~/Workspaces/ChiliPiper
  (other projects).
- `re-scale build kill-sbt` in the repo root at iteration end.
