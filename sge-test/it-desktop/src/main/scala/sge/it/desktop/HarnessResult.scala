// SGE — Desktop integration test: result data model

package sge.it.desktop

/** Result of a single subsystem check.
  *
  * `skipped` marks a check that did not run because a *required capability is absent from the environment* — e.g. a GPU-reliant check on a GPU-less, software-rasterizer CI runner. A skip is distinct
  * from a fail: it neither passes nor fails the suite. It is granted narrowly — only when the missing capability is detected at runtime (the GL renderer string), never merely assumed — so it can
  * never become a vacuous green (ISS-485). See MultiWindowEglCheck clause 2 (cross-context EGL sharing) on software rasterizers (ISS-691).
  */
final case class CheckResult(name: String, passed: Boolean, message: String, skipped: Boolean = false)

/** Aggregated results from all subsystem checks. */
final case class HarnessResult(platform: String, checks: Seq[CheckResult]) {

  /** A skipped check neither passes nor fails the suite (it did not run). */
  def allPassed: Boolean = checks.forall(c => c.passed || c.skipped)

  /** Format as lines for stdout logging. */
  def toLogLines: Seq[String] =
    checks.map { c =>
      val status = if (c.skipped) "SKIP" else if (c.passed) "PASS" else "FAIL"
      s"SGE-IT:${c.name.toUpperCase}:$status:${c.message}"
    }
}
