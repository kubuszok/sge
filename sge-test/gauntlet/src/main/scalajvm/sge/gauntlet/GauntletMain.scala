/*
 * SGE Gauntlet — JVM desktop entry point (ISS-766).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

/** JVM entry point.
  *
  * `--headless` runs under [[HeadlessApplication]] (GPU probes reported as `skipped_gpu`); otherwise a real GLFW+ANGLE window opens at the fixed 1280x720 probe size and GPU probes render into the
  * shared FBO while the window shows the live results grid. The process exit code is the hard-failure count (failed + unexpected_pass).
  */
object GauntletMain {

  /** JVM-only probes appended to the shared registry (JDK loopback HTTP server, JDK stream plumbing). */
  private def jvmProbes: List[FeatureProbe] = List(
    probes.NetHttpLoopbackProbe,
    probes.NetSocketsLoopbackProbe
  )

  def main(args: Array[String]): Unit =
    GauntletConfig.parse(args) match {
      case Left(message) =>
        System.err.println(message)
        System.exit(64)
      case Right(parsed) =>
        // Resolve the report dir against the process cwd so all downstream file access is absolute.
        val cfg      = parsed.copy(reportDir = new java.io.File(parsed.reportDir).getAbsolutePath)
        val selected = cfg.select(ProbeRegistry.shared ++ jvmProbes)
        if (selected.isEmpty) {
          System.err.println(s"no probes match the given --only/--area filter")
          System.exit(64)
        } else if (cfg.headless) {
          System.exit(HeadlessRunner.run(cfg, selected))
        } else {
          @volatile var exitCode: Int                         = 255
          val app:                Sge ?=> ApplicationListener =
            new GauntletApp(selected, cfg, results => exitCode = ProbeResult.hardFailures(results))
          val config = DesktopApplicationConfig()
          config.title = "SGE Gauntlet"
          config.windowWidth = GpuHarness.Width
          config.windowHeight = GpuHarness.Height
          // DesktopApplicationFactory blocks until the application exits.
          DesktopApplicationFactory(app, config)
          System.exit(exitCode)
        }
    }
}
