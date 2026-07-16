/*
 * SGE Gauntlet — final status of a probe run.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

/** Final status of a single probe run.
  *
  * The knownIssue policy: a probe with [[FeatureProbe.knownIssue]] set reports [[KnownFail]] when it fails (not a hard failure) and [[UnexpectedPass]] (a HARD failure) when it passes — a fix must
  * claim its probe by removing the annotation.
  */
enum ProbeStatus(val wireName: String, val isHardFailure: Boolean) {

  /** All checks passed and the probe carries no knownIssue annotation. */
  case Passed extends ProbeStatus("passed", false)

  /** At least one check failed (or the probe threw / produced no checks) and no knownIssue is cited. */
  case Failed extends ProbeStatus("failed", true)

  /** GPU-requiring probe skipped because the runner is in headless mode. Not a pass. */
  case SkippedGpu extends ProbeStatus("skipped_gpu", false)

  /** The probe failed, but cites an open issue via knownIssue — expected until the issue is fixed. */
  case KnownFail extends ProbeStatus("known_fail", false)

  /** The probe PASSED although it cites a knownIssue — the fix must claim the probe by removing the annotation. Hard failure. */
  case UnexpectedPass extends ProbeStatus("unexpected_pass", true)
}
