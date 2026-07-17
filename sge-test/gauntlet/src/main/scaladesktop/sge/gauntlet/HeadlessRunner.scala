/*
 * SGE Gauntlet — headless (no-GL) runner shared by the JVM and Native entry points.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import lowlevel.Nullable

/** Runs the gauntlet under [[HeadlessApplication]] (NoopGraphics/NoopAudio, no GL context): `requiresGpu` probes are skipped and reported as `skipped_gpu` — the honest partial verification CI can
  * perform without a display.
  *
  * Crash guard (ISS-796 bounce): the probe state machine runs on [[HeadlessApplication]]'s own main-loop thread. [[GauntletApp]] converts any throwable inside a probe phase into a failing check, but
  * a throwable that escapes OUTSIDE the phases (report writing, runner bugs) would kill that thread and leave the completion latch waiting forever — a hang, not a failure. An uncaught-exception
  * handler releases the latch on any main-loop death and the run exits [[HeadlessRunner.CrashedExitCode]]: a crashed run is a RED run, never a hang.
  */
object HeadlessRunner {

  /** Exit code for a run whose main loop crashed before reporting results (EX_SOFTWARE). */
  val CrashedExitCode: Int = 70

  /** Blocks until the run completes and returns the process exit code (the hard-failure count, or [[CrashedExitCode]] if the main loop died). */
  def run(cfg: GauntletConfig, probes: List[FeatureProbe]): Int = {
    val completion = new java.util.concurrent.CountDownLatch(1)
    // Stays at CrashedExitCode unless the run completes and reports its results.
    @volatile var exitCode: Int                         = CrashedExitCode
    val app:                Sge ?=> ApplicationListener = new GauntletApp(
      probes,
      cfg,
      results => {
        exitCode = ProbeResult.exitCode(results)
        completion.countDown()
      }
    )
    // Java interop: the JDK getter may return null; the value is treated as an opaque token —
    // only ever delegated to / handed straight back to the setter, never dereferenced directly.
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler
    Thread.setDefaultUncaughtExceptionHandler { (thread, e) =>
      System.err.println(s"SGE-GAUNTLET: main loop crashed on thread '${thread.getName}': $e — failing the run with exit $CrashedExitCode")
      e.printStackTrace()
      completion.countDown()
      Nullable(previousHandler).foreach(_.uncaughtException(thread, e))
    }
    try {
      val _ = new HeadlessApplication(app, HeadlessApplicationConfig())
      completion.await()
    } finally Thread.setDefaultUncaughtExceptionHandler(previousHandler)
    exitCode
  }
}
