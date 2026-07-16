/*
 * SGE Gauntlet — result of a single probe run.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

/** Everything the report records about one probe run. */
final case class ProbeResult(
  id:             String,
  area:           String,
  status:         ProbeStatus,
  checks:         List[Check],
  durationMs:     Long,
  screenshotPath: Option[String],
  logLines:       List[String]
)

object ProbeResult {

  /** Count of hard failures ([[ProbeStatus.Failed]] + [[ProbeStatus.UnexpectedPass]]) — the process exit code. */
  def hardFailures(results: List[ProbeResult]): Int =
    results.count(_.status.isHardFailure)
}
