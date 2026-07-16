# Velocity plan: parallel fixing, GPU gauntlet, scaladoc (2026-07-16)

Companion to `release-review-2026-07-16.md`. Three tracks: (1) raise issue-fix
throughput from ~1.5/day to 6-10/day, (2) a local GPU test app ("gauntlet")
covering what CI cannot see, (3) scaladoc for the public API.

Measured facts this plan builds on (checked 2026-07-16):

- Last master CI run: 1h13m wall. Critical path is the four Rosetta
  macos-x86_64 jobs — Scala Native tests **49m36s**, verify-release 19m31s,
  JVM tests 16m25s, FFI IT 12m34s. Every other job is 1m40s–17m.
- `master` has **no branch protection** — required checks/merge policy are
  self-imposed, so trains and two-tier CI need no GitHub-settings work.
- The non-blocking scaladoc probe (`sbt sge/doc`, ci.yml:131) **passes** on
  current master — the upstream compiler bug that motivated
  `packageDoc / publishArtifact := false` (build.sbt:181) appears fixed.

---

## Track 1 — Parallelize issue fixing

Three levers, in ROI order. A is one PR of CI work; B is pure protocol; C is
a /goal skill rewrite.

### Lever A: cut the per-run CI cost (73m → ~25m)

- **A1. Demote the Rosetta macos-x86_64 rows** (all four jobs) from the
  per-PR set to master-post-merge + nightly. Rosetta emulation is a
  compatibility check, not fast feedback; it is 3-5× slower than every
  sibling row and singlehandedly defines the wall time. PR wall drops to
  ~25-30m (next-longest: Scala Native windows 17m).
- **A2. Path-filtered job gating.** A change-detection first job (git diff
  against merge-base) sets outputs like `core`, `extension-only`, `build-only`,
  `docs-only`; Android emulator, demo compile, browser packaging, and
  verify-release jobs skip when untouched trees make them irrelevant. Most
  campaign fixes are single-module — those PRs drop to ~10-15m.
- **A3. Hang armor.** Add `timeout-minutes` to every job (hangs currently
  burn the 6h default until someone cancels) and a scheduled babysitter
  (gh CLI cron or workflow_run) that re-runs failed-with-timeout jobs once.
  Removes the manual cancel+rerun toil that ate whole evenings this week.
- **A4 (policy, needs sign-off).** Two-tier discipline: PR-required =
  linux JVM + JS + Native linux + affected suites + enforce/ratchet +
  scalafmt; the full exotic matrix runs post-merge on master + nightly, and
  a red full-tier **locks merges** (auto-filed issue, fix-first rule) so
  green-before-merge survives at one-remove. Alternative that keeps the
  letter of the discipline: full matrix runs once per *train* (Lever B), not
  per PR.

### Lever B: merge trains (amortize one CI run over N fixes)

1. Wave planning: pick 3-6 issues whose `file_path`s don't intersect
   (query the DB up front; group by module). Conflict-free by construction.
2. Each fix runs the existing pipeline (reproducer → implementer → orchestrator
   re-verify → auditor PASS) in its own worktree, with **local** gates only:
   `re-scale build compile --all`, targeted `--only <Suite>` runs, shortcuts,
   ratchet. No per-fix CI run.
3. Stack audited fixes onto `train/YYYY-MM-DD-N` (cherry-pick, red-sha +
   fix-sha preserved in messages). One full CI run on the train.
4. Green → fast-forward master, resolve all ISS ids. Red → identify culprit
   from the failing job (per-module mapping makes this near-mechanical),
   eject that commit back to its branch, rerun failed jobs only. Never debug
   on the train.
5. Effective throughput: 4-6 issues per CI-hour instead of 1. Doc-only waves
   (Track 3) can be much wider (10+ packages) since they can't break tests.

### Lever C: wave-parallel /goal (skill rewrite)

Rework one `/goal` iteration from "up to 3 issues, sequential" to a wave:

- Dispatch all reproducers of the wave concurrently (background agents,
  worktree isolation — infra already exists). As each red lands, dispatch its
  implementer; auditors likewise as fixes land. Agent time is free; the host
  bottleneck is sbt, so the orchestrator serializes only compile/test steps
  (stagger, or one `sbt --client` per worktree with a concurrency cap of 2).
- Keep unchanged: bounce-to-same-agent, max 3 bounces, banned phrases,
  adversarial audit, orchestrator's independent re-verify.
- Add: implementers receive the auditor's 7-point checklist + mutation-check
  expectations **upfront** (cuts the bounce rate — ISS-729's bounce was a
  predictable mutation-coverage gap).
- Encode the train protocol (Lever B) as the merge step of the skill.

Expected combined effect: 6-10 issues/day for waves of independent
minors/majors; the 0.1.0 blocker list (release-review §G) is mostly
single-file fixes — ideal train cargo.

Sequencing: A1+A3 first (one PR), then B as protocol (no code), then C
(edit `.claude/skills/goal/SKILL.md`), then A2 opportunistically.

---

## Track 2 — The gauntlet: a local GPU feature-verification app

A runnable app that is simultaneously a demo reel and an integration test
battery — run locally on a GPU machine to see what headless CI cannot:
actual rendered output, real window/input/audio behavior.

**Location:** `sge-test/gauntlet`, a module in the root build (dependsOn sge +
extensions). Root-build placement gives agents the fast master-HEAD feedback
loop; the demos sub-build stays the "consume published artifacts" proof.

**Core abstraction:**

```scala
trait FeatureProbe {
  def id: String        // "g2d/font-align-left"
  def module: String    // "sge-core" | "textra" | ...
  def frames: Int       // how many frames to run
  def init(ctx: ProbeContext): Unit
  def render(ctx: ProbeContext, frame: Int): Unit
  def verify(ctx: ProbeContext): List[Check]   // named pass/fail assertions
}
```

`ProbeContext` provides: FBO capture + pixel readback helpers, a fixed
timestep clock, seeded RNG, synthetic input injection, temp-dir, and a
logger whose lines land in the report. Determinism rules: fixed 1280×720,
no wall clock, no unseeded randomness; audio probes assert callbacks/timing,
not waveforms.

**Verification tiers:**
1. *Programmatic asserts* (default): pixel probes at known coordinates
   ("pixel(10,10) is red", "text bounding box starts at x=20"), state
   asserts, flush/bind counters. Robust across GPUs/drivers.
2. *Golden images* (opt-in per probe): PNG + perceptual diff with tolerance,
   `--update-goldens` to re-bake, goldens per-platform if drivers force it.
3. *Human eyeball*: `--interactive` mode cycles probes with keys and a live
   pass/fail overlay — this is the "demo" face.

**Runner modes:** `--all`, `--only <id-prefix>`, `--tag <module>`,
`--interactive`, `--report <dir>`. Output: `gauntlet-report.json` (per probe:
id, status, failed checks with expected/actual, screenshot path, diff path,
duration, log excerpt), `report.md` summary, per-probe PNGs. JSON schema is
the agent interface: "run `re-scale runner gauntlet -- --only g2d/font` and
read the report" is a complete subagent instruction. Exit code = #failures.
Wire as a `gauntlet` runner in `.rescale/runners.yaml`.

**Phase 1 probe set (~15, deliberately the review's blind spots):**
font draw alignment (ISS-584), Label.setFontScale metrics, Button
checked/over drawable cascade, scene2d menu click-through via synthetic
input, Skin load from JSON, FBO render-to-texture (ISS-572), TMX **and** TMJ
file load + render (first real fixtures in the repo), SpriteBatch
blend-mode grid + flush-count assert, ShapeRenderer primitives, NinePatch
stretch, 2D particles, viewport letterboxing, music onComplete (1s clip),
sound playback state, FreeType glyph generation + render.

**Phase 2:** one probe per feature area from the migration DB; all 5 tiled
renderers; physics visual step; ai steering visual; controllers (manual
tag — needs a pad plugged in). **Phase 3:** compile the same probe registry
for Native and browser (probe code is already platform-agnostic if it avoids
JVM-only APIs); browser rides the existing Playwright harness. Later: a
self-hosted GPU runner label for nightly gauntlet in CI.

**Where it sits in the loop:** local gauntlet run becomes a required
pre-merge gate for graphics/UI/audio-touching fixes (minutes, not an hour),
and its Phase-1 probes double as the regression proof for the whole 0.1.0
text-rendering cluster.

---

## Track 3 — Scaladoc for the public API

**Step 0 (unblocks release blocker A.3):** verify `doc` locally across all
modules and platforms, then delete `packageDoc / publishArtifact := false`
from `mimaSettings` (build.sbt:181) and promote the CI probe from
non-blocking to blocking. The probe already passes on master.

**Content tiers:**
- **T1 (0.1.0-gating):** the getting-started path — `Sge`, Application/
  Game/Screen/ApplicationListener, desktop config classes, the five platform
  traits (Graphics/Input/Audio/Files/Net), SpriteBatch, Texture,
  TextureAtlas, BitmapFont/GlyphLayout, Camera/Viewport, AssetManager +
  loaders, scene2d core (Stage/Actor/Group/Table, main widgets, Skin), math
  essentials. **Docs must be honest about the gaps the review found** —
  where behavior is deferred (cursors, startup fullscreen) the scaladoc says
  so instead of promising LibGDX behavior; several current doc comments
  promise features the code ignores, which is worse than no doc.
- **T2:** per-extension package overview + top ~5 entry-point classes each.
- **T3:** remaining public surface. Much already carries ported LibGDX
  comments (the no-comment-removal rule preserved them); the work is
  javadoc→scaladoc syntax conversion and gap-filling, not greenfield.

**Doc-style rules (add a section to docs/contributing/code-style.md):**
document Nullable semantics ("returns `Nullable.empty` when …"), the
`(using Sge)` requirement, renames from LibGDX (`@note LibGDX: rect()`),
platform capability differences (this is where the ad-hoc
unsupported-capability signaling finally gets written down), and no stale
LibGDX code examples (the ShapeRenderer `rect(...)` doc-rot case).

**Tooling — doc-coverage ratchet:** a scalameta script (scala-cli, or an
`re-scale enforce doc-coverage` extension) counting public
classes/defs lacking scaladoc per module; add `doc_coverage` per-module
metrics to the remediation baseline, allowed to move in one direction only.
Package-level doc required for every public package.

**Execution:** doc-writer agents per package — pure-additive comment edits
are conflict-free by package, so doc waves ride the Track-1 train machinery
at high width (10+ packages/train, one CI run each). Auditor spot-checks a
sample per wave for honesty (no invented behavior claims) and syntax.
Post-0.1.0: publish the scaladoc site (GitHub Pages); for 0.1.0 itself only
the javadoc JARs on Central matter.

---

## Decisions (user, 2026-07-16)

1. **CI tiering (A4):** full matrix once per merge *train* (fixes verify
   locally; green-before-merge preserved at train granularity). A1/A3
   (Rosetta demotion for PR runs, job timeouts + rerun babysitter) still
   apply to whatever runs per-PR.
2. **Gauntlet placement:** `sge-test/gauntlet` in the root build.
3. **Golden images:** assertion-first; goldens opt-in per probe.
4. **Review findings:** filed as DB issues under category `review-release`;
   ratchet re-baseline user-authorized same day.

## Suggested first week

1. PR: A1 (Rosetta demotion) + A3 (timeouts/babysitter) + Track-3 Step 0
   (doc JARs on). One CI-infra train.
2. File review findings as issues (pending decision 4); plan wave 1 =
   release-engineering blockers (providers 0.1.3, sge-build publishing,
   kindlings check, doc pins).
3. Gauntlet skeleton + first 5 probes (font alignment, font scale, button
   states, FBO, TMX load) — lands *before* the text-cluster fixes so each
   fix ships with its probe.
4. Wave 2 = text-rendering cluster + fullscreen cluster + Wayland hint +
   Music.onComplete, validated by gauntlet locally, merged as one train.
5. Rewrite `/goal` to wave+train mode (Lever C); resume `/loop /goal`.
