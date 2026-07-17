/*
 * SGE Gauntlet — runner crash semantics regression tests (ISS-796 bounce).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/** Regression tests for the ISS-796 bounce: a probe-phase `Error` (e.g. `UnsatisfiedLinkError` from a missing native provider) used to escape [[GauntletApp]]'s `NonFatal`-only phase capture, kill the
  * [[HeadlessApplication]] main-loop thread, and leave [[HeadlessRunner]]'s completion latch hanging forever. A crashed probe must be a RED run, never a hang.
  *
  * Both tests return Futures so a regression surfaces as a bounded munit timeout instead of a stuck JVM.
  */
class RunnerCrashSuite extends munit.FunSuite {

  /** A probe whose render phase throws an Error — the linkage-error failure mode. */
  private object LinkageCrashProbe extends FeatureProbe {

    override def id: String = "test/linkage-crash"

    override def area: String = "test"

    override def requiresGpu: Boolean = false

    override def frames: Int = 1

    override def init(ctx: ProbeContext): Unit = ()

    override def render(ctx: ProbeContext, frame: Int): Unit =
      throw new UnsatisfiedLinkError("Cannot find native library 'libsge_test.dylib' (synthetic)")

    override def verify(ctx: ProbeContext): List[Check] =
      List(Check.cond("reached-verify", passed = true, "unreachable after render crash", "reached"))
  }

  /** A well-behaved probe used to drive the run into the report-writing phase. */
  private object PassingProbe extends FeatureProbe {

    override def id: String = "test/passing"

    override def area: String = "test"

    override def requiresGpu: Boolean = false

    override def frames: Int = 1

    override def init(ctx: ProbeContext): Unit = ()

    override def render(ctx: ProbeContext, frame: Int): Unit = ()

    override def verify(ctx: ProbeContext): List[Check] =
      List(Check.cond("always-passes", passed = true, "pass", "pass"))
  }

  test("an Error thrown in a probe phase is reported as a failing check and exits nonzero — no hang") {
    Future {
      val dir  = java.nio.file.Files.createTempDirectory("gauntlet-crash-test").toAbsolutePath.toString
      val exit = HeadlessRunner.run(GauntletConfig(headless = true, reportDir = dir), List(LinkageCrashProbe))
      // The graceful path: the Error becomes the probe's failing no-unhandled-exception check,
      // the run completes and reports 1 hard failure (NOT the crashed-run exit code).
      assertEquals(exit, 1)
      val json = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(dir, "report.json")), "UTF-8")
      assert(json.contains("no-unhandled-exception-in-render(1)"), s"report must record the phase crash check, got:\n$json")
      assert(json.contains("UnsatisfiedLinkError"), "report must record the linkage error detail")
    }
  }

  test("a crash outside probe phases releases the latch with CrashedExitCode — no hang") {
    Future {
      // An unwritable report dir makes GauntletReport.write throw AFTER the probes ran —
      // outside any probe phase, on the application's main-loop thread. The runner's
      // uncaught-exception guard must turn that into a red exit instead of a hang.
      val exit = HeadlessRunner.run(GauntletConfig(headless = true, reportDir = "/dev/null/not-a-directory"), List(PassingProbe))
      assertEquals(exit, HeadlessRunner.CrashedExitCode)
    }
  }
}
