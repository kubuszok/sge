// SGE — Desktop integration test: result data model

package sge.it.desktop

/** Result of a single subsystem check. */
final case class CheckResult(name: String, passed: Boolean, message: String)

/** Aggregated results from all subsystem checks. */
final case class HarnessResult(platform: String, checks: Seq[CheckResult]) {

  def allPassed: Boolean = checks.forall(_.passed)

  /** Format as lines for stdout logging. */
  def toLogLines: Seq[String] =
    checks.map { c =>
      val status = if (c.passed) "PASS" else "FAIL"
      s"SGE-IT:${c.name.toUpperCase}:$status:${c.message}"
    }
}
