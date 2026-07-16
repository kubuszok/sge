/*
 * SGE Gauntlet — ApplicationListener that drives the probe state machine (ISS-766).
 * Copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gauntlet

import sge.graphics.PixmapIO

import java.util.zip.Deflater
import scala.collection.mutable.ListBuffer

/** Runs the selected probes one after another, a few frames each, then writes the report and exits (or, with `--interactive`, stays open on the results grid).
  *
  * Per app frame the state machine advances the current probe by exactly one phase step: frame 0 runs `init`, frames 1..N run `render(frame)`, frame N+1 runs `verify` plus the FBO screenshot capture.
  * GPU probes execute every phase inside the shared 1280x720 FBO so pixel readbacks and screenshots observe exactly what the probe drew; in windowed mode the default framebuffer then shows the live
  * results grid.
  */
class GauntletApp(
  probes: List[FeatureProbe],
  cfg:    GauntletConfig,
  onDone: List[ProbeResult] => Unit
)(using sge: Sge)
    extends ApplicationListener {

  private val queue = probes.toArray

  private var index      = 0
  private var probeFrame = 0
  private var startedMs  = 0L
  private var ctx:     Option[ProbeContext] = None
  private var aborted: Option[Check]        = None

  private val results = ListBuffer.empty[ProbeResult]

  private var gpu: Option[GpuHarness] = None
  private var ui:  Option[GauntletUi] = None

  private var runFinished = false

  override def create(): Unit =
    scribe.info(s"SGE Gauntlet: ${queue.length} probes, headless=${cfg.headless}, interactive=${cfg.interactive}")

  override def resize(width: Pixels, height: Pixels): Unit =
    ui.foreach(_.resize(width, height))

  override def render(): Unit = {
    if (!runFinished) {
      if (index >= queue.length) finishRun()
      else stepProbe()
    }
    if (!cfg.headless) {
      drawUi()
    }
  }

  override def pause(): Unit = ()

  override def resume(): Unit = ()

  override def dispose(): Unit = {
    ui.foreach(_.dispose())
    gpu.foreach(_.dispose())
  }

  // ── probe state machine ─────────────────────────────────────────────

  private def ensureGpu(): GpuHarness = gpu.getOrElse {
    val g = new GpuHarness()
    gpu = Some(g)
    g
  }

  private def currentContext(probe: FeatureProbe): ProbeContext = ctx.getOrElse {
    val c = new ProbeContext(
      probe.id,
      cfg.headless,
      cfg.reportDir,
      if (probe.requiresGpu) Some(ensureGpu()) else None
    )
    ctx = Some(c)
    c
  }

  /** Runs one phase body, inside the FBO for GPU probes, converting exceptions into an aborting check. */
  private def runPhase(probe: FeatureProbe, phase: String)(body: => Unit): Unit =
    try
      if (probe.requiresGpu) ensureGpu().fbo.use(body)
      else body
    catch {
      case scala.util.control.NonFatal(e) =>
        ctx.foreach(_.log(s"exception in $phase: $e"))
        e.getStackTrace.take(5).foreach(el => ctx.foreach(_.log(s"  at $el")))
        aborted = Some(Check(s"no-unhandled-exception-in-$phase", passed = false, "no exception", e.toString))
    }

  private def stepProbe(): Unit = {
    val probe = queue(index)
    if (probe.requiresGpu && cfg.headless) {
      results += ProbeResult(probe.id, probe.area, ProbeStatus.SkippedGpu, Nil, 0L, None, Nil)
      scribe.info(s"SGE-GAUNTLET:${probe.id}:SKIPPED_GPU")
      advance()
    } else {
      val c = currentContext(probe)
      if (probeFrame == 0) {
        startedMs = System.currentTimeMillis()
        runPhase(probe, "init")(probe.init(c))
      } else if (probeFrame <= probe.frames && aborted.isEmpty) {
        runPhase(probe, s"render($probeFrame)")(probe.render(c, probeFrame))
      }
      if (aborted.isDefined || probeFrame >= probe.frames + 1) {
        finalizeProbe(probe, c)
        advance()
      } else {
        probeFrame += 1
      }
    }
  }

  private def finalizeProbe(probe: FeatureProbe, c: ProbeContext): Unit = {
    var checks:     List[Check]    = Nil
    var screenshot: Option[String] = None
    if (aborted.isEmpty) {
      runPhase(probe, "verify") {
        checks = probe.verify(c)
        if (probe.requiresGpu) {
          screenshot = captureScreenshot(probe)
        }
      }
    }
    val effective =
      if (aborted.isEmpty && checks.isEmpty)
        List(Check("produced-checks", passed = false, "at least one check", "probe returned no checks"))
      else checks ++ aborted.toList
    val allPassed = effective.nonEmpty && effective.forall(_.passed)
    val status    = (probe.knownIssue, allPassed) match {
      case (Some(_), true)  => ProbeStatus.UnexpectedPass
      case (Some(_), false) => ProbeStatus.KnownFail
      case (None, true)     => ProbeStatus.Passed
      case (None, false)    => ProbeStatus.Failed
    }
    val duration = System.currentTimeMillis() - startedMs
    results += ProbeResult(probe.id, probe.area, status, effective, duration, screenshot, c.logLines)
    scribe.info(s"SGE-GAUNTLET:${probe.id}:${status.wireName.toUpperCase}:${effective.count(_.passed)}/${effective.size} checks")
  }

  /** Captures the FBO into `screenshots/<id>.png` (and a UI texture in interactive mode). Called inside the FBO binding. */
  private def captureScreenshot(probe: FeatureProbe): Option[String] =
    gpu.map { g =>
      val safe   = probe.id.replace('/', '-')
      val relDir = sge.files.absolute(s"${cfg.reportDir}/screenshots")
      relDir.mkdirs()
      val file   = relDir.child(s"$safe.png")
      val pixmap = g.capture()
      try {
        PixmapIO.writePNG(file, pixmap, Deflater.DEFAULT_COMPRESSION, true)
        ui.foreach(_.addScreenshot(probe.id, pixmap))
      } finally pixmap.close()
      file.path
    }

  private def advance(): Unit = {
    index += 1
    probeFrame = 0
    ctx = None
    aborted = None
    ui.foreach(_.setResults(results.toList))
  }

  private def finishRun(): Unit = {
    runFinished = true
    val all = results.toList
    GauntletReport.write(all, mode, cfg.reportDir)
    scribe.info(s"SGE-GAUNTLET: report written to ${cfg.reportDir} (hard failures: ${ProbeResult.hardFailures(all)})")
    // False-green guard: a run in which NO probe executed (all skipped_gpu, e.g.
    // a GPU-only selection under --headless) verified nothing and must not exit 0.
    // ProbeResult.exitCode turns this into ProbeResult.NothingExecutedExitCode.
    if (ProbeResult.executed(all) == 0) {
      scribe.error(
        s"SGE-GAUNTLET: NO probe executed (${all.size} selected, all skipped_gpu) — " +
          s"failing the run with exit ${ProbeResult.NothingExecutedExitCode}; " +
          "run without --headless (or widen --only/--area) to execute these probes"
      )
    }
    onDone(all)
    if (!cfg.interactive) {
      sge.application.exit()
    } else {
      ui.foreach(_.setResults(all))
    }
  }

  private def mode: String =
    if (cfg.headless) "headless" else if (cfg.interactive) "interactive" else "windowed"

  // ── live results UI (windowed modes) ───────────────────────────────

  private def drawUi(): Unit = {
    val u = ui.getOrElse {
      val created = new GauntletUi(onExit = () => sge.application.exit())
      created.setResults(results.toList)
      ui = Some(created)
      created
    }
    val running = if (runFinished || index >= queue.length) None else Some(queue(index).id)
    u.render(running)
  }
}
