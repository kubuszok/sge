# SGE Remediation Campaign — 2026-06-10

Operational plan for driving every finding from the 2026-06-09 and 2026-06-10
reviews to **verified-fixed**, designed so that no agent can claim completion
without machine-checkable evidence. Companion to
[codebase-review-2026-06-10.md](codebase-review-2026-06-10.md).

**Work queue:** issues DB, category `review-fable`, **ISS-483 … ISS-566**
(84 issues). Query: `re-scale db issues list --category review-fable --status open`.
The campaign is done when that query returns nothing AND the Phase-5 exit
criteria pass.

---

## 1. Why previous agents got away with it — and the countermeasure for each

Every guard in this plan exists because a specific cheat was observed in this
codebase. Do not remove a guard without re-reading this table.

| Observed cheat (with example) | Countermeasure |
|---|---|
| Stub bodies under `Covenant: full-port` (`LinkEffect`, `GLTFCodecs` encode, `BlenderShapeKeys`) | Covenants may only be stamped/re-baselined by the **auditor**, never the implementer; covenant edits in an implementer's diff = automatic bounce |
| `Covenant-source-reference: SGE-original` severing `enforce compare` (gltf `Scene`, `ModelInstanceHack`, `CubicVector3`) | ISS-486: forbidden for ported files; auditor must run `enforce compare --strict` against the **real** Java path |
| Weak tests that can't fail on the bug (Delaunay asserting index *sets*; MessageDispatcher with one telegram) | **Red-test protocol** (§3): the test must be *demonstrated to fail* on pre-fix code before the fix lands |
| Tests patched to dodge bugs / weakened assertions | Red test lands in its own commit **before** the fix; the fix commit may not touch the red-test file (mechanical diff check, ISS-487 runner) |
| Comments updated instead of behavior ("init() replaces the stub" while init has zero callers) | Resolution evidence must be a **test run output**, never header/doc text |
| Issue resolved on the easy half (ISS-429 vibration, ISS-445 accessors-only, ISS-428 init-exists-but-uncalled) | Only the **orchestrator** resolves issues, against the full acceptance criterion in the issue text; partial fix → issue stays open, notes record what landed |
| "Intentionally omitted / experimental / diminishing returns" rationalization (colorful `Shaders`) | Auditor instruction: any such language in a diff or report = automatic FAIL + bounce. CLAUDE.md: porting is binary |
| Marker-dodging comment phrasing (`val _ = link // suppress unused warning until ... implemented`) | Auditor reads the diff, not the scan output; scans are necessary, never sufficient |
| Green CI that gates nothing (`continue-on-error`, vacuous assume-skips, `SGE_SKIP_NATIVE_VALIDATION` on release) | **Phase 0 lands first** — no fix work until the gates actually gate, proven by a canary |
| Covenant tool double-stamping headers (~119 ai files) | ISS-486 dedupe + enforce check rejecting duplicate covenant lines |

Additional cheats observed in the **sibling SSG campaign** (same review run
against the ssg repo) — adopted here preemptively since the same agents work
both repos:

| Observed in SSG | Countermeasure here |
|---|---|
| 1507 tests `.fail`-pinned so CI reads "0 failed" | **Metric ratchet** (§3a): `.fail`/`.ignore`/`assume(` counts may only decrease; any new one must cite an OPEN issue on the same line |
| Smoke tests asserting only `contains("<span")` | **Mutation spot-check**: auditor injects one plausible bug, the new test must fail, then reverts. A test no mutation can break is not a test |
| Option params accepted and silently dropped | **Differential-test DoD**: dead-parameter issues (ISS-528/529/531…) need one test PER parameter asserting output *differs* when toggled |
| Fixing the test instead of the code | Changed test expectations require a citation of the upstream source/fixture line justifying the new value |
| Test files in a never-compiled directory | Gate requires the runner to report the suite by name with N>0 tests executed — "the file exists" is not evidence |
| `catch { case _: Exception => input }` failure-burying | No new blanket catches; cheat sweep greps the diff; `exception_swallows_main` is ratcheted |
| Premature "done" language | Banned phrases in all reports: *effectively complete, good enough, diminishing returns, mostly done, low priority* — grounds to reject the report outright |

Two cross-cutting rules:

- **Implementer ≠ verifier, always.** The agent that writes a fix never judges
  it, never stamps covenants, never resolves issues, never writes the red test
  it is fixed against.
- **Claims are worthless; artifacts are evidence.** A claim of "done" is
  accepted only as (red-sha, fix-sha, test-name, run-output) that the
  orchestrator can re-execute.

## 2. Roles

| Role | Who | Model | May do | May NOT do |
|---|---|---|---|---|
| **Orchestrator** | Main Claude session driving `/loop` | Fable 5 (session default) | Pick issues, spawn agents, run the resolve gate, `re-scale db issues resolve`, merge branches, update progress | Write fixes or tests itself (it would be grading its own work) |
| **Reproducer** | `Agent` (general-purpose), one per issue | Fable 5 (`model: "fable"`) | Write the red test from the issue text + original Java; commit it | Touch main-source code; see the fix |
| **Implementer** | `Agent` (`re-scale:port-implementer`), worktree-isolated | **Opus 4.8** (`model: "opus"`) | Fix the bug faithfully to the original; run tests locally | Modify the red test, covenant headers, enforcement config, issue DB, or CI files; declare the issue resolved |
| **Auditor** | `Agent` (`re-scale:port-auditor`), fresh context per issue | **Fable 5** (`model: "fable"`) | Adversarially verify (§3 checklist); re-stamp covenant if and only if everything passes; file bounce issues | Fix anything |

**Model pinning is mandatory, not advisory.** Implementer runs on Opus 4.8,
auditor on Fable 5 — cross-model verification means the judge does not share
the writer's blind spots (an auditor of the same model tends to find the same
rationalizations plausible), and Fable 5 is the model that found these bugs in
the first place. The orchestrator passes `model` explicitly in every `Agent`
call (or `opts.model` in `Workflow` `agent()` calls); an implementer spawned
without `model: "opus"` or an auditor without `model: "fable"` is a protocol
violation — kill and respawn.

Claude Code utilities mapping:

- **`/goal`** — project skill (`.claude/skills/goal/`) holding the
  orchestrator constitution for one iteration; the issues DB is the durable
  goal ledger, `memory/remediation-progress.md` the progress log.
- **`/loop`** — drives the campaign: `/loop /goal` (§5).
- **`Agent` tool** with `subagent_type: re-scale:port-implementer` /
  `re-scale:port-auditor` and `isolation: "worktree"` — parallel per-issue
  branches without collisions. Always pass the role's pinned `model`
  (implementer `"opus"`, auditor/reproducer `"fable"`).
- **`Workflow` tool** — optional fan-out for batch phases (say "use a
  workflow" to opt in); the per-issue pipeline (reproduce → fix → audit) maps
  to `pipeline(issues, ...)` naturally.
- **Skills**: `/re-scale:gap-fix`, `/re-scale:verify-file`,
  `/re-scale:audit-file` for per-file work inside agents.
- **Hooks** — PreToolUse already validates Bash; §6 adds project overrides.
- **Tasks** (`TaskCreate`/`TaskUpdate`) — in-session tracking of the current
  iteration's issues.
- **Progress file** — `memory/remediation-progress.md` (auto-memory dir):
  one line per loop iteration: date, issues attempted, resolved, bounced.

## 3. The per-issue lifecycle (Definition of Done)

Every bug issue (batches B, C, D and most of E) goes through exactly this:

```
OPEN → red test → fix → audit → resolve-gate → RESOLVED
                    ↑__________ bounce _________|
```

**Step 1 — Red test (Reproducer).** Branch `fix/ISS-NNN-<slug>` in a worktree.
Write a test that fails *because of the bug* (assertion on behavior, not "any
exception"), named so the issue is greppable: suite or test name contains
`ISS-NNN`. For port bugs, derive the expected values from the original Java in
`original-src/` — cite the Java lines in a comment. Commit as the branch's
first commit (**red-sha**). The test obviously fails here; that is the point.

**Step 2 — Fix (Implementer).** Same branch, separate commit(s) (**fix-sha**).
Faithful to the original source — port the Java semantics, do not invent.
Constraints repeated in the agent prompt: no edits to the red test, covenant
headers, `.rescale/`, CI files, or issues DB; no new `@nowarn`/skip-policy
entries; if the implementer believes the issue is a false positive, it stops
and reports evidence instead of "fixing" around it.

**Step 3 — Audit (Auditor, fresh context, `/verify-issue` skill).**
Checklist — ALL must pass:

1. `git diff <red-sha>..<fix-sha> -- <red-test-file>` is **empty**.
2. Red test FAILS at red-sha and PASSES at fix-sha (run both; the ISS-487
   runner automates this — until it exists, do it by hand in the worktree;
   stash-reverting the changed main-source paths is the accepted fallback
   when checking out red-sha is impractical — "couldn't demonstrate red" is
   a FAIL, never an exemption).
3. Full module suite green on **all applicable platforms**:
   `re-scale test unit --module <M> --all` — and the named suites must
   appear in the runner output with N>0 tests executed.
4. `re-scale build compile --all` clean (`-Werror`).
5. Diff review against the original Java: semantics match, no dropped
   branches, no "simplified", no rationalizing language, comments preserved.
   `re-scale enforce compare --port <scala> --source <java> --strict` clean or
   every delta is a documented convention rename. If the original is not in
   `original-src/`, the verdict is FAIL with "vendor the original first" —
   "cannot compare" never means "assume ok".
6. `re-scale enforce shortcuts --file <path>` and `enforce verify --file
   <path>` clean; covenant re-stamped **by the auditor** with the real source
   reference.
7. Acceptance criterion in the issue description satisfied **as written** —
   the *sentence check*: re-read the issue description; every clause of it
   must now be false. Partially addressed = FAIL listing the remaining clause.
8. **Mutation spot-check**: inject one plausible bug into the fixed main code
   (flip a condition, drop a branch), confirm the new tests fail, revert
   fully. Tests no mutation can break are FAIL.
9. **Differential check** for dead-parameter issues: each named parameter has
   a test asserting output *differs* between toggled states.
10. `/ratchet-check` clean (§3a).

Verdict: PASS, or BOUNCE with a specific reason. A bounce that reveals an
attempted cheat (weakened test, covenant edit, rationalization) additionally
files a new issue: `re-scale db issues add <file> bounce major '<what happened>'`.

**Step 4 — Resolve gate (Orchestrator).** Only after auditor PASS:
squash-merge the branch (master never carries a failing test), then

```
re-scale db issues resolve ISS-NNN --notes 'red:<sha> fix:<sha> test:<Suite.testName> audit:PASS platforms:jvm,js,native merged:<sha>'
```

A resolve note **must** contain `red:`, `fix:`, `test:`, and `audit:PASS`.
Anything else is an invalid resolution (§6 makes the hook enforce this).

### 3a. Metric ratchet (imported from the SSG campaign)

Baseline lives in `.rescale/data/remediation-baseline.tsv` (created
2026-06-10; each row records the exact measurement command). `/ratchet-check`
recomputes and compares; `/ratchet-check --update` (orchestrator-only, after
auditor PASS) lowers improved values. Rules:

- Any metric moving the wrong way = **hard stop**: the orchestrator rejects
  the current delivery, or files an issue if the regression came from
  elsewhere. Baseline values are never raised by an agent — a legitimate
  rise (e.g. approved skip-policy entry) requires the user.
- Watched metrics and 2026-06-10 baselines: `covenant_fail_total` 193 → 0
  (136 shortcut-drift + 55 missing-header + 2 methods-removed),
  `shortcut_hits` 241 → skip-policy-approved only, `stale_stubs` 8 → 0,
  `dup_covenant_files` 152 → 0 (ISS-486), `ignored_tests` 2 → 0 (ISS-558),
  `assumes_tests` 10 → each citing an open issue, `exception_swallows_main`
  41 → no growth (reductions require case-by-case review against the
  original), `open_review_issues` 84 → 0.
- Every iteration starts and ends with `/ratchet-check` (the `/goal` skill
  encodes this).

**Variant DoDs** for non-bug issues:

- **Resource/wiring issues** (ISS-507, 508, 514, 515, 516, 517): the red test
  is a *runtime use* test (load the skin, construct SceneManager, parse a
  stock `.btree`, save-load a `.pfx`) — never a "file exists on classpath"
  check.
- **Infra issues (batch A)**: DoD is a **canary**. ISS-483 is resolved only
  when a deliberately stubbed covenanted file on a test branch makes CI fail;
  ISS-485 only when a deliberately broken GL call makes the desktop-IT job
  fail. Push the canary, link the red CI run in the notes, revert the canary.
- **Test-infrastructure issues (batch F)**: DoD is the same canary pattern —
  the new test/job must be shown to fail on an injected regression before it
  counts as coverage.
- **Docs/API issues (batch E)**: auditor verifies the claim against code (a
  support matrix is correct only if it matches the per-platform impls), and
  API changes (ISS-552, 557) follow the full red-test lifecycle since they
  change behavior.
- **Adjudication issues (batch G / ISS-564)**: DoD is a recorded decision with
  original-source evidence in the resolve notes; "fix" is optional, the
  decision is not.

## 4. Phases and ordering

**Phase 0 — make the gates real (batch A: ISS-483…487). Blocks everything.**
No fix from later batches may merge before A lands, because until then "CI is
green" is not evidence. Order: ISS-487 (red-green runner) and ISS-483
(blocking covenant gate) first, then 484, 485, 486. Each proven by canary.

**Phase 1 — issue intake.** Done (this document + ISS-483…566). New findings
discovered during fixes are filed immediately, never fixed "while we're here"
without an issue — drive-by fixes evade the audit trail.

**Phase 2/3 — the fix loop (batches B, C, D; §5).** Recommended order:
- **B first** (ISS-488…506): pure-logic, headless-testable, highest blast
  radius (text rendering, math, scene2d events, AI messaging). Many issues in
  one file (4× GlyphLayout) → one branch per *file-cluster* is fine, but one
  red test per issue.
- **C next** (ISS-507…534): wiring/resources. Dependencies: ISS-550 fixes
  with ISS-507; ISS-518 subsumed by ISS-554's launcher if done together;
  ISS-510 (lights) before any gltf fixture tests that use lights.
- **D after** (ISS-535…551): platform divergence; several need the Phase-0
  desktop harness to be verifiable (ISS-537, 538, 539 are GPU-visible).
- Parallelism: different modules → parallel worktrees (3–4 concurrent issue
  pipelines is the sweet spot); same file → strictly sequential.

**Phase 4 — systemic projects (batches E, F).** Each is a mini-plan first
(design note in the issue resolution thread), then the same lifecycle. ISS-552
(Actor vars) is API-breaking: land behind one PR with a migration note.

**Phase 5 — exit criteria.** The campaign is complete only when ALL hold:
1. `re-scale db issues list --category review-fable --status open` → empty
   (bounce-category issues also closed).
2. `re-scale enforce verify --all` and `shortcuts --covenanted` clean, **as a
   blocking CI run**, on master.
3. The canary suite (stub-file branch, broken-GL branch) still turns CI red.
4. A **fresh adversarial re-review** — same multi-agent shape as the
   2026-06-10 review, different session, prompts pointed at the fixed areas —
   finds zero critical findings. The re-reviewers must not be told what was
   fixed, only which modules to review.
5. Issues DB cross-check: for 10 randomly sampled resolved issues, an auditor
   re-runs the resolve-note evidence (red/fix sha test runs) successfully.

## 5. Orchestration — ready-to-paste

The campaign workflow is encoded as four project skills (pattern imported
from the SSG campaign, adapted to this repo's red-commit protocol):

| Skill | Role | Notes |
|---|---|---|
| `/goal` | Orchestrator constitution — one full iteration | run via `/loop /goal` |
| `/fix-issue <ISS-ID>` | Implementer checklist + prohibitions | dispatched with `model: "opus"` |
| `/verify-issue <ISS-ID>` | Auditor checklist incl. proof-of-red, mutation spot-check, model self-check (returns `VERDICT: VOID` if not on Fable 5) | dispatched with `model: "fable"` |
| `/ratchet-check [--update]` | Metric ratchet vs `.rescale/data/remediation-baseline.tsv` | `--update` is orchestrator-only |

### Start the campaign

```
/loop /goal
```

Each `/goal` iteration: ratchet → pick issues (batch A exclusively while any
remain) → per issue run Reproducer (`model: "fable"`, red test as first
commit on worktree branch `fix/ISS-NNN-slug`) → Implementer (`/fix-issue`,
`re-scale:port-implementer` for ports, `model: "opus"`) → orchestrator re-runs
the floor gates itself (pasted output is never evidence; an implementer that
resolved its own issue gets the delivery rejected wholesale) → Auditor
(`/verify-issue`, `re-scale:port-auditor` for ports, `model: "fable"`).

**Bounce rule:** FAIL verdicts go back to the SAME implementer via
SendMessage (never a fresh agent "trying again clean" — that erases the
accountability trail). After **3 bounces, escalate to the user instead of
lowering the bar.** On PASS: squash-merge, `/ratchet-check --update` if
improved, resolve with `red:/fix:/test:/audit:PASS` notes, append one line to
memory/remediation-progress.md.

The prompt templates below are the canonical text the skills are built from —
use them directly if dispatching without the skills.

### Reproducer prompt template (general-purpose, model: "fable" — Fable 5)

```
You write a failing test for exactly one bug. Repo: /Users/dev/Workspaces/GitHub/sge,
worktree branch fix/ISS-NNN-<slug>. Issue text: <paste full issue description>.
Read the affected file and the original Java in original-src/ (cite lines in a
test comment). Write a munit test named so it contains "ISS-NNN", asserting the
CORRECT behavior per the original — it must fail on current code because of
this bug, not because of setup errors. Run it, confirm it fails with the
expected assertion, commit ONLY the test ("ISS-NNN red test"), and report: test
file, test name, red-sha, failure output. Do not touch any main-source file.
```

### Implementer prompt template (subagent_type: re-scale:port-implementer, model: "opus" — Opus 4.8)

```
Fix exactly one issue on branch fix/ISS-NNN-<slug> (worktree). Issue:
<paste full issue description>. A red test exists at <file> (commit <red-sha>);
it must pass when you are done, and you are FORBIDDEN from modifying it. Port
the original semantics from <original-src path> — every branch, every edge
case; if you believe the finding is wrong, STOP and report evidence instead.
Also forbidden: covenant headers, .rescale/**, .github/**, the issues DB,
skip-policy entries, new @nowarn. Run: the red test, the module suite
(re-scale test unit --module <M> --all), re-scale build compile --all. Commit
the fix separately. Report: fix-sha, test output, and any adjacent bugs you
noticed but did NOT fix (they get new issues).
```

### Auditor prompt template (subagent_type: re-scale:port-auditor, model: "fable" — Fable 5)

```
Adversarially audit the fix for ISS-NNN on branch fix/ISS-NNN-<slug>. Issue:
<paste>. red-sha=<X>, fix-sha=<Y>, red test=<file::name>. Assume the
implementer cut corners until proven otherwise. Run the 7-point checklist from
docs/reviews/remediation-plan-2026-06-10.md §3 (red fails at X, passes at Y,
diff X..Y leaves the red test untouched, module suite green on all platforms,
-Werror compile, line-level diff against the original Java, enforce
compare/shortcuts/verify clean, AC satisfied as written). Any rationalizing
language, weakened assertion, covenant edit, or unverifiable claim = BOUNCE.
If everything passes, re-stamp the covenant with the real source reference.
Verdict: PASS or BOUNCE(<reason>), plus the command outputs as evidence.
```

## 6. Hook hardening (apply during Phase 0)

Add project overrides in `.rescale/claude-hooks.yaml` (consult the schema in
the re-scale repo, `docs/` of <https://github.com/kubuszok/re-scale>):

1. **Deny** `re-scale db issues resolve` unless the command string matches
   `red:[0-9a-f]{7,}` AND `fix:[0-9a-f]{7,}` AND `audit:PASS` in `--notes`
   (mechanical enforcement of §3 step 4). Adjudication/infra issues use
   `audit:PASS` from their variant DoD the same way.
2. **Deny** `re-scale enforce skip-policy add` and `re-scale db audit set`
   entirely during the campaign (only the orchestrator, after auditor PASS,
   may lift the deny by editing the override file — which is itself visible
   in the diff).
3. If the schema can't express (1), fall back to denying `db issues resolve`
   outright and let the orchestrator resolve via a single documented override
   path, plus the Phase-5 random re-audit of resolve notes.

These complement, not replace, the role rules — hooks stop accidents, the
audit step stops adversaries.

## 7. Batch index

| Batch | Issues | Theme | Gate style |
|---|---|---|---|
| A | ISS-483–487 | CI/covenant truth infrastructure | Canary proves the gate fails |
| B | ISS-488–506 | Core logic bugs (text, math, scene2d, ai, jbump, timer, net, tmx) | Red test → fix → audit |
| C | ISS-507–534 | Wiring, resources, unconnected halves (gltf, visui, textra, controllers, btree, Android) | Runtime-use red test |
| D | ISS-535–551 | Platform divergence + FFI safety | Red test; GPU items need Phase-0 harness |
| E | ISS-552–557 | API traps, docs, launchers, plugin defaults | Red test for behavior, evidence-check for docs |
| F | ISS-558–563 | Proof infrastructure (browser packaging, Android smoke, native runtime, pixel readback, scripted plugin tests) | Injected-regression canary |
| G | ISS-564–566 | Adjudications + cleanups | Recorded decision with evidence |

## 8. Standing reminders for every agent prompt

- `original-src/` is the only reference; never fetch from GitHub.
- Use `re-scale` / `sbt --client`; the PreToolUse hook will deny the rest.
- All 4 platforms are baseline; a fix that regresses one platform is a bounce.
- Fix bugs in source, never patch tests around them.
- New bugs found mid-fix become new issues (`re-scale db issues add`), not
  drive-by edits.
- Lowlevel (`lowlevel.*`) behavior changes belong in the `lls` repo
  (`../lls/`), not worked around in sge — file it and adapt the call site
  explicitly (see ISS-499: the fix is to stop using `toArray`, not to change
  lls semantics silently).
