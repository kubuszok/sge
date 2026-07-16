/*
 * SGE Gauntlet — headless (no-GL) runner shared by the JVM and Native entry points.
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

/** Runs the gauntlet under [[HeadlessApplication]] (NoopGraphics/NoopAudio, no GL context): `requiresGpu` probes are skipped and reported as `skipped_gpu` — the honest partial verification CI can
  * perform without a display.
  */
object HeadlessRunner {

  /** Blocks until the run completes and returns the process exit code (the hard-failure count). */
  def run(cfg: GauntletConfig, probes: List[FeatureProbe]): Int = {
    val completion = new java.util.concurrent.CountDownLatch(1)
    @volatile var exitCode: Int                         = 255
    val app:                Sge ?=> ApplicationListener = new GauntletApp(
      probes,
      cfg,
      results => {
        exitCode = ProbeResult.exitCode(results)
        completion.countDown()
      }
    )
    val _ = new HeadlessApplication(app, HeadlessApplicationConfig())
    completion.await()
    exitCode
  }
}
