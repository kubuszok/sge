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

  /** Count of hard failures ([[ProbeStatus.Failed]] + [[ProbeStatus.UnexpectedPass]]). */
  def hardFailures(results: List[ProbeResult]): Int =
    results.count(_.status.isHardFailure)

  /** Count of probes that actually EXECUTED (any status other than skipped_gpu). */
  def executed(results: List[ProbeResult]): Int =
    results.count(_.status != ProbeStatus.SkippedGpu)

  /** Exit code when every selected probe was skipped: nothing executed, so a green exit would be a false green. */
  val NothingExecutedExitCode: Int = 65

  /** The process exit code: the hard-failure count — except that a run in which NO probe executed (e.g. a GPU-only selection in headless mode) is itself a failure, [[NothingExecutedExitCode]]. A run
    * that verifies nothing must not look green.
    */
  def exitCode(results: List[ProbeResult]): Int =
    if (executed(results) == 0) NothingExecutedExitCode
    else hardFailures(results)
}
