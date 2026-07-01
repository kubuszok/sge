/*
 * SGE Regression Test — structured result accumulator.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package regression

/** Accumulates pass/fail results for regression smoke checks.
  *
  * Logs each check using structured `SGE-IT:<SUBSYSTEM>:<PASS|FAIL>:<message>` format (matching `sge-android-smoke`'s protocol) and prints a final summary.
  */
object SmokeResult {

  private var passed: Int = 0
  private var failed: Int = 0

  /** Optional hook fired once [[summary]] has run. The headless desktop launcher installs this to release its own (JVM/Native) latch and derive the process exit code from [[allPassed]]. Kept as a
    * plain callback — not a `java.util.concurrent` primitive — so this shared source still links on Scala.js (browser), where the headless launcher and its threading do not exist.
    */
  private var onComplete: () => Unit = () => ()

  /** Installs the completion hook (see [[onComplete]]). Called by the headless desktop launcher before the app loop starts. */
  def setOnComplete(hook: () => Unit): Unit = onComplete = hook

  /** Log a single check result. */
  def logCheck(name: String, ok: Boolean, message: String): Unit = {
    val status = if (ok) "PASS" else "FAIL"
    val line   = s"SGE-IT:$name:$status:$message"
    // Print directly — scribe may not be configured on all platforms
    System.out.println(line)
    if (ok) passed += 1 else failed += 1
  }

  def allPassed: Boolean = failed == 0

  /** Print final summary and return overall result. */
  def summary(): Boolean = {
    val total = passed + failed
    if (failed == 0) {
      System.out.println(s"SMOKE_TEST_PASSED ($passed/$total checks)")
    } else {
      System.out.println(s"SMOKE_TEST_FAILED ($failed/$total failed)")
    }
    onComplete()
    failed == 0
  }
}
