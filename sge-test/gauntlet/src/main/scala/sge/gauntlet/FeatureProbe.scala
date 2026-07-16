/*
 * SGE Gauntlet — feature probe abstraction (ISS-766).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

/** A single feature-verification probe.
  *
  * A probe exercises one engine feature end-to-end (render calls, asset I/O, input dispatch, …) and verifies it with named [[Check]]s — pixel asserts, state asserts, counters. Probes must be
  * deterministic: fixed 1280x720 target, no wall clock, no unseeded randomness (use [[ProbeContext.rng]]).
  *
  * Lifecycle, driven by the runner: `init(ctx)` once, then `render(ctx, frame)` for `frames` frames (frame is 1-based), then `verify(ctx)`. GPU probes run with the shared 1280x720 FBO bound, so
  * [[ProbeContext.pixel]] readbacks and the captured screenshot both observe what `render` drew. Probes dispose the resources they created at the end of `verify`.
  */
trait FeatureProbe {

  /** Stable probe id, `area/short-name` (e.g. "g2d/spritebatch-blend", "files/roundtrip"). */
  def id: String

  /** Feature area: "g2d" | "g3d" | "input" | "assets" | "audio" | "files" | "net" | "utils" | "scene2d" | "ext/&lt;name&gt;". */
  def area: String

  /** Whether this probe needs the real desktop backend (GL context, miniaudio device, windowed input). Headless mode skips these probes, reporting them as `skipped_gpu` — never as passed. */
  def requiresGpu: Boolean

  /** Open issue id (e.g. "ISS-584") this probe is expected to fail on. See [[ProbeStatus]] for the policy. */
  def knownIssue: Option[String] = None

  /** Number of frames to run `render` before `verify` is called. */
  def frames: Int

  /** One-time setup. Runs with the FBO bound for GPU probes. */
  def init(ctx: ProbeContext): Unit

  /** Per-frame work. `frame` counts 1..frames. */
  def render(ctx: ProbeContext, frame: Int): Unit

  /** Final assertions. Must return at least one [[Check]] — an empty list is reported as a failure (a probe that checks nothing proves nothing). */
  def verify(ctx: ProbeContext): List[Check]
}
