// SGE — Desktop integration test harness
//
// ApplicationListener that runs subsystem checks sequentially across
// frames, logs structured results, and exits.

package sge.it.desktop

import sge.{ ApplicationListener, Pixels, Sge }
import sge.it.desktop.checks._

import java.io.{ File, PrintWriter }
import scala.collection.mutable.ArrayBuffer

/** Integration test harness that exercises subsystems on a real desktop application (GLFW + ANGLE + miniaudio) and writes results to a file.
  *
  * @param resultsFile
  *   path where JSON results will be written after all checks complete
  */
class DesktopHarness(resultsFile: File)(using sge: Sge) extends ApplicationListener {

  private var ready:   Boolean                  = false
  private var frame:   Int                      = 0
  private val results: ArrayBuffer[CheckResult] = ArrayBuffer.empty

  // Check schedule: (frameToRun, checkName, checkFn)
  // We spread checks across frames to let GL state settle between them.
  private val schedule: Array[(Int, () => CheckResult)] = Array(
    (2, () => {BootstrapCheck.run() }),
    (4, () => {FileIOCheck.run() }),
    (6, () => {JsonXmlCheck.run() }),
    (8, () => {GL2DCheck.run() }),
    (10, () => {GL3DCheck.run() }),
    (12, () => {AudioCheck.run() }),
    (14, () => {InputCheck.run() }),
    (16, () => {PixmapCheck.run() }),
    (18, () => {TextureCheck.run() }),
    (20, () => {SpriteBatchCheck.run() }),
    (22, () => {FBOCheck.run() }),
    (24, () => {ClipboardCheck.run() }),
    (26, () => {WindowCheck.run() }),
    (28, () => {MusicCheck.run() }),
    (30, () => {MultiSoundCheck.run() }),
    (32, () => {TextureAtlasCheck.run() }),
    (34, () => {WindowResizeCheck.run() }),
    (36, () => {CursorCheck.run() }),
    (38, () => {InputDispatchCheck.run() }),
    (40, () => {FullscreenCheck.run() })
  )

  override def create(): Unit = {
    scribe.info("SGE DesktopHarness.create()")
    ready = true
  }

  override def resize(width: Pixels, height: Pixels): Unit = ()

  override def render(): Unit = {
    if (!ready) return
    frame += 1

    // Run any checks scheduled for this frame
    schedule.foreach { case (targetFrame, checkFn) =>
      if (frame == targetFrame) {
        val result = checkFn()
        results += result
        val status = if (result.passed) "PASS" else "FAIL"
        scribe.info(s"SGE-IT:${result.name.toUpperCase}:$status:${result.message}")
      }
    }

    // After all checks, write results and exit
    if (frame == schedule.last._1 + 2) {
      writeResults()
      sge.application.exit()
    }
  }

  override def pause(): Unit = ()

  override def resume(): Unit = ()

  override def dispose(): Unit =
    scribe.info("SGE DesktopHarness.dispose()")

  private def writeResults(): Unit = {
    val harness = HarnessResult("jvm", results.toSeq)
    val json    = resultsToJson(harness)
    val writer  = new PrintWriter(resultsFile)
    try writer.write(json)
    finally writer.close()
    scribe.info(s"SGE-IT: Results written to ${resultsFile.getAbsolutePath}")
  }

  private def resultsToJson(hr: HarnessResult): String = {
    val checksJson = hr.checks
      .map { c =>
        val escaped = c.message.replace("\\", "\\\\").replace("\"", "\\\"")
        s"""    {"name":"${c.name}","passed":${c.passed},"message":"$escaped"}"""
      }
      .mkString(",\n")
    s"""|{
        |  "platform": "${hr.platform}",
        |  "checks": [
        |$checksJson
        |  ]
        |}""".stripMargin
  }
}
