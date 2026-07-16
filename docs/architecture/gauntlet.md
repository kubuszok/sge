# The Gauntlet — feature-verification app

`sge-test/gauntlet` (ISS-766, velocity-plan Track 2) is a runnable application
that is simultaneously a demo reel and an integration-test battery. It
exercises WHOLE engine functionality — 2D rendering, 3D rendering, input
dispatch, asset loading, audio, file I/O, networking, extensions — with
assertion-first verification (pixel and state asserts; **no golden images**),
and reports the results as JSON + markdown designed to be read by agents and
humans alike.

It exists to close the blind spots identified in
`docs/reviews/release-review-2026-07-16.md`: headless CI cannot see rendered
pixels, real audio playback, or windowed input behavior — the gauntlet runs
those paths on a GPU machine and proves them with pixel readbacks, while its
non-GPU probes still run (and gate) in CI.

Long-term it replaces the demos that are not really games; listing the
candidates is below, **deleting any demo is a maintainer decision** and no
demo is removed by this work.

## Running

```
sbt --client 'sge-gauntlet/run --headless --report target/gauntlet'   # CI mode
sbt --client 'sge-gauntlet/run'                                       # windowed, GPU probes run
sbt --client 'sge-gauntlet/run --interactive'                         # windowed + stays open on the grid
sbt --client 'sge-gauntlet/run --only g2d/'                           # id-prefix filter
sbt --client 'sge-gauntlet/run --area net'                            # area filter
```

Pass `--report` an **absolute** path when you care where the report lands —
the forked JVM's cwd is the sbt matrix project dir, not the repo root.

Modes:

| Mode | Application | GPU probes | Exit |
|------|-------------|-----------|------|
| `--headless` | `HeadlessApplication` (NoopGraphics/NoopAudio) | reported `skipped_gpu`, never executed | after run |
| windowed (default) | GLFW + ANGLE window, 1280x720 | render into a shared offscreen FBO | after run |
| `--interactive` | as windowed | as windowed | stays open on the results grid (arrow keys / click to inspect, each GPU probe's FBO capture shown in the panel; ESC exits) |

Process exit code = number of **hard failures** = `failed` + `unexpected_pass`.
False-green guard: if **no probe executed at all** (every selected probe was
`skipped_gpu`, e.g. a GPU-only `--only` selection under `--headless`), the run
exits **65** — a run that verified nothing must not look green. (Exit 64 =
CLI usage error / empty filter selection.)

## Probe API

```scala
trait FeatureProbe {
  def id: String            // "g2d/spritebatch-blend", "files/roundtrip", ...
  def area: String          // "g2d" | "g3d" | "input" | "assets" | "audio" | "files" | "net" | "utils" | "scene2d" | "ext/<name>"
  def requiresGpu: Boolean  // headless mode skips these (reported SKIPPED_GPU, not passed)
  def knownIssue: Option[String] = None // open ISS id; see policy below
  def frames: Int           // frames to run before verify
  def init(ctx: ProbeContext): Unit
  def render(ctx: ProbeContext, frame: Int): Unit   // frame is 1-based
  def verify(ctx: ProbeContext): List[Check]        // Check(name, passed, expected, actual)
}
```

The runner (`GauntletApp`) advances one probe phase per application frame:
frame 0 `init`, frames 1..N `render(frame)`, then `verify` + screenshot
capture. For GPU probes every phase runs with the shared 1280x720 depth-backed
FBO bound, so `ctx.pixel(x, y)` readbacks and the captured PNG observe exactly
what the probe drew. Any exception in any phase becomes a failing
`no-unhandled-exception-in-<phase>` check. A probe that returns **zero**
checks is reported failed (`produced-checks`) — a probe that verifies nothing
proves nothing.

`requiresGpu` semantically means "needs the real desktop backend": a GL
context, a miniaudio device, real windowed input. Audio *playback* probes are
`requiresGpu` because headless mode wires `NoopAudio` — there is nothing
honest to assert there.

### ProbeContext

- `batch` / `shapes` / `camera` — shared SpriteBatch, ShapeRenderer and a
  pixel-perfect y-up `OrthographicCamera` over the fixed 1280x720 target.
- `pixel(x, y): Color` / `pixelRgba(x, y)` — single-pixel readback from the
  probe's render target (GL coordinates, y up, matching the camera).
- `clear(r, g, b, a)` — clears color+depth of the render target.
- `setInputTarget(processor)` + `injectTouchDown/injectTouchUp/injectMouseMoved/injectKeyDown`
  — synthetic input dispatched through the `InputProcessor` interface (screen
  coordinates, y down), the same seam the real backends dispatch through.
- `fixedDelta` (1/60s), `rng` (seeded per probe id), `tempDir` (per-probe
  scratch dir under the report dir), `log(line)` (lines land in the report).

Determinism rules: fixed 1280x720 target, use `fixedDelta` and `rng`, no
unseeded randomness. One caveat learned the hard way: the windowed frame loop
is **unthrottled** (no vsync guarantee), so frame counts are not wall time —
probes asserting wall-time-driven behavior (music track end, scene2d's 0.1s
visual-pressed window) must wait for the *condition* with a bounded deadline
instead of counting frames.

## Report schema (the agent interface)

`<reportDir>/report.json`:

```json
{
  "mode": "headless" | "windowed" | "interactive",
  "hardFailures": 0,
  "probes": [
    {
      "id": "g2d/spritebatch-blend",
      "area": "g2d",
      "status": "passed" | "failed" | "skipped_gpu" | "known_fail" | "unexpected_pass",
      "checks": [ {"name": "...", "passed": true, "expected": "...", "actual": "..."} ],
      "durationMs": 12,
      "screenshotPath": "…/screenshots/g2d-spritebatch-blend.png",   // GPU probes only
      "logLines": ["..."]
    }
  ]
}
```

Plus `report.md` (summary table) and `screenshots/<id>.png` (per GPU probe,
captured from the FBO). "Run the gauntlet and read report.json" is a complete
subagent instruction.

## knownIssue policy

Project rule: every expected failure cites an open issue.

- `knownIssue = Some("ISS-xxx")` and the probe **fails** → status
  `known_fail`, not a hard failure.
- `knownIssue` set and the probe **passes** → status `unexpected_pass`, a
  **hard failure**: the fix that made it pass must claim the probe by
  removing the annotation (and resolving the issue through the normal flow).
- Verify the cited issue is open before annotating:
  `re-scale db issues list --status open | grep ISS-xxx`.

Current annotations (all verified open on 2026-07-16):

| Probe | Issue | Failing check(s) |
|-------|-------|------------------|
| `g2d/font-default-draw` | ISS-584 | `left-aligned-run-x` (-34 instead of 0), ink lands left of the anchor — plain `font.draw` renders right-aligned |
| `scene2d/button-states` | ISS-758 | `checked-drawable-hover-fallback` — checked+hover without `checkedOver` renders `up` instead of `checked` |
| `audio/music-oncomplete` | ISS-760 | `oncomplete-fired` — `Music.onComplete` never fires on desktop |

`g2d/fbo-roundtrip` was written as the ISS-572 re-verification and **passes**
on lls 0.3.0 — the FrameBuffer CCE no longer reproduces; it is a normal probe
(the issue itself is left to the normal verify-and-close flow, cf. ISS-789).

## Phase 1 probe set

Non-GPU (execute in headless CI): `files/roundtrip`, `assets/manager-async`,
`utils/json-xml`, `ext/jbump-collision`, `ext/ai-statemachine`,
`audio/lifecycle-headless`, `net/http-loopback`, `net/sockets-loopback`
(the net probes are JVM-only, appended by the JVM launcher).

GPU: `g2d/spritebatch-blend` (blend modes + flush-count assert),
`g2d/shaperenderer`, `g2d/font-default-draw`, `g2d/fbo-roundtrip`,
`scene2d/button-states` (synthetic hover/click through the Stage input
pipeline, programmatic Skin), `g3d/modelbatch-cube` (ModelBuilder cube through
DefaultShader, pixel asserts), `audio/sound-playback`,
`audio/music-oncomplete`.

The engine ships no default font, so the gauntlet bakes its own
(`GauntletFont`): a 5x7 pixel font generated into a texture at runtime — used
by the font probes and the interactive UI.

## Adding a probe

1. Create an object extending `FeatureProbe` under
   `sge-test/gauntlet/src/main/scala/sge/gauntlet/probes/` (platform-agnostic)
   or `src/main/scalajvm/.../probes/` (JVM-only APIs).
2. Do the work in `render`, collect `Check`s, return them from `verify`;
   dispose everything you created at the end of `verify`.
3. Keep it deterministic (see rules above); pixel-assert with tolerances
   (±12 has been robust) at coordinates you control.
4. Register it in `ProbeRegistry.shared` (or the launcher's platform list).
5. If it is expected to fail on a known bug, verify the issue is open and set
   `knownIssue`.
6. Run `sbt --client 'sge-gauntlet/run --only <your-id>'` and check the report
   shows real check counts.

## CI

The `gauntlet-headless` job (`.github/workflows/ci.yml`, needs
`compile-gate`, ubuntu, 30 min timeout) runs `--headless`, prints `report.md`
into the job log, uploads the report dir as the `gauntlet-report` artifact and
fails on a non-zero exit code. GPU probes appear there as `skipped_gpu` —
honest partial verification; the full run needs a GPU machine (mac/windows/
linux desktop).

## Platform roadmap

- **Phase 1 (this)**: JVM row only; probe core + shared probes are
  platform-agnostic (`src/main/scala`), the launcher split follows the
  regression-app pattern (`scaladesktop` headless runner, `scalajvm` main).
- **Phase 2 — browser + native**: add JS and Native rows to the matrix. The
  Native row reuses `HeadlessRunner`/`GauntletMain` patterns directly
  (mirror `regressionTest`'s `NativeMain` + resource embedding); the browser
  row needs a JS launcher and a report sink (postMessage/console protocol)
  and rides the existing Playwright harness in `sge-test/it-browser`.
  Candidate additions: TMX/TMJ fixture probes, NinePatch stretch, 2D
  particles, viewport letterboxing, FreeType glyph probe (JVM/Native only),
  Label.setFontScale metrics.
- **Phase 3 — Android**: drive the same registry from an instrumentation
  activity following the `sge-it-android` smoke-APK pattern; report over
  logcat with the `SGE-GAUNTLET:` prefix.
- **Demo-replacement candidates** (maintainer decision, nothing deleted):
  the non-game demos whose purpose is feature exercise — `asset-showcase`,
  `tile-world` (programmatic map), `viewer-3d`, `particle-show` — overlap
  with gauntlet areas; real games (Pong etc.) stay.
