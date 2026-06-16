// SGE — Desktop integration test harness for ISS-540 (glGetActiveUniformBlockName overflow)
//
// A self-contained ApplicationListener that runs ONLY the uniform-block-name
// overflow check against a live ANGLE GL ES3 context, writes a single-line
// result, and exits. Kept separate from the monolithic DesktopHarness so this
// red can be proven in isolation: it never touches the monitor APIs
// (glfwGetMonitorName aborts on a headless / no-monitor host) and never builds
// an FBO (ISS-572 ClassCastException), so the only way it can fail is the
// glGetActiveUniformBlockName overflow contract itself.

package sge.it.desktop

import sge.{ ApplicationListener, Pixels, Sge }
import sge.it.desktop.checks.UniformBlockNameCheck

import java.io.{ File, PrintWriter }

/** Runs the ISS-540 overflow check on a real desktop application and writes its result to a file.
  *
  * @param resultsFile
  *   path where the single-line result ("PASS:msg" or "FAIL:msg") is written
  */
class UniformBlockNameHarness(resultsFile: File)(using sge: Sge) extends ApplicationListener {

  private var ready: Boolean = false
  private var frame: Int     = 0

  override def create(): Unit = {
    scribe.info("SGE UniformBlockNameHarness.create()")
    ready = true
  }

  override def resize(width: Pixels, height: Pixels): Unit = ()

  override def render(): Unit = {
    if (!ready) return
    frame += 1

    // Give the GL context a couple of frames to settle, then run the check.
    if (frame == 2) {
      val result = UniformBlockNameCheck.run()
      val status = if (result.passed) "PASS" else "FAIL"
      scribe.info(s"SGE-IT:UNIFORMBLOCKNAME:$status:${result.message}")
      val writer = new PrintWriter(resultsFile)
      try writer.write(s"$status:${result.message}")
      finally writer.close()
      sge.application.exit()
    }
  }

  override def pause(): Unit = ()

  override def resume(): Unit = ()

  override def dispose(): Unit =
    scribe.info("SGE UniformBlockNameHarness.dispose()")
}
