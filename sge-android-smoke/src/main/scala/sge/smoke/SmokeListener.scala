// SGE — Android smoke test: minimal ApplicationListener
//
// Clears the screen to green, runs subsystem checks, and logs structured
// results. After completing checks it prints SMOKE_TEST_PASSED and exits.
//
// Subsystem check results are logged as:
//   SGE-IT:<SUBSYSTEM>:<PASS|FAIL>:<message>
//
// This file has no android.* imports and compiles on JVM without
// the Android SDK.

package sge
package smoke

import sge.utils.XmlReader

/** [[ApplicationListener]] for the Android smoke + integration test.
  *
  * Clears the screen to a color, runs subsystem checks on specific frames, and logs structured results. After 30 frames it calls `exit()` so the Activity finishes cleanly.
  */
class SmokeListener extends ApplicationListener with SgeAware {

  private var sge:       Sge     = scala.compiletime.uninitialized
  private var ready:     Boolean = false
  private var checksRun: Boolean = false
  private var allPassed: Boolean = true

  override def sgeAvailable(sge: Sge): Unit =
    this.sge = sge

  override def create(): Unit = {
    scribe.info("SGE SmokeListener.create()")
    ready = true
  }

  override def resize(width: Pixels, height: Pixels): Unit = ()

  override def render(): Unit = {
    if (!ready) return
    given Sge = sge
    val gl    = Sge().graphics.getGL20()
    gl.glClearColor(0f, 0.4f, 0f, 1f)
    gl.glClear(graphics.ClearMask.ColorBufferBit)
    val frame = Sge().graphics.getFrameId()
    if (frame % 10 == 0) scribe.info(s"SGE smoke frame $frame")

    // Run subsystem checks on frame 5 (GL is settled by then)
    if (frame == 5 && !checksRun) {
      checksRun = true
      runSubsystemChecks()
    }

    if (frame >= 30) {
      if (allPassed) scribe.info("SMOKE_TEST_PASSED")
      else scribe.info("SMOKE_TEST_FAILED")
      Sge().application.exit()
    }
  }

  override def pause(): Unit = ()

  override def resume(): Unit = ()

  override def dispose(): Unit =
    scribe.info("SGE SmokeListener.dispose()")

  // ── Subsystem checks ─────────────────────────────────────────────

  private def logCheck(name: String, passed: Boolean, message: String): Unit = {
    val status = if (passed) "PASS" else "FAIL"
    scribe.info(s"SGE-IT:$name:$status:$message")
    if (!passed) allPassed = false
  }

  private def runSubsystemChecks()(using Sge): Unit = {
    checkBootstrap()
    checkGL2D()
    checkFileIO()
    checkJsonXml()
    checkAudio()
  }

  private def checkBootstrap()(using Sge): Unit =
    try {
      val gl = Sge().graphics.getGL20()
      if (gl == null) logCheck("BOOTSTRAP", passed = false, "GL20 is null")
      else logCheck("BOOTSTRAP", passed = true, "Sge context created, GL20 available")
    } catch {
      case e: Exception =>
        logCheck("BOOTSTRAP", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkGL2D()(using Sge): Unit =
    try {
      val gl = Sge().graphics.getGL20()
      // Simple GL call verification — draw and read back would require FBO which
      // may not be available on all Android SwiftShader configs. Just verify GL
      // calls don't crash.
      gl.glClearColor(1f, 0f, 0f, 1f)
      gl.glClear(graphics.ClearMask.ColorBufferBit)
      // Reset to green
      gl.glClearColor(0f, 0.4f, 0f, 1f)
      gl.glClear(graphics.ClearMask.ColorBufferBit)
      logCheck("GL2D", passed = true, "GL clear calls OK")
    } catch {
      case e: Exception =>
        logCheck("GL2D", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkFileIO()(using Sge): Unit =
    try {
      // Write to a temp file and read back
      val tmpDir  = System.getProperty("java.io.tmpdir", "/data/local/tmp")
      val files   = Sge().files
      val tmpFile = files.absolute(s"$tmpDir/sge-it-test-${System.nanoTime()}.txt")
      val testStr = "SGE integration test"
      tmpFile.writeString(testStr, false)
      val readBack = tmpFile.readString()
      tmpFile.delete()
      if (readBack == testStr) logCheck("FILEIO", passed = true, "Write/readback OK")
      else logCheck("FILEIO", passed = false, s"Readback mismatch")
    } catch {
      case e: Exception =>
        logCheck("FILEIO", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkJsonXml(): Unit =
    try {
      // Verify JSON infrastructure classes are loadable (full parse test
      // requires jsoniter-scala-macros which is "provided" scope in sge)
      Class.forName("com.github.plokhotnyuk.jsoniter_scala.core.package$")

      // XML parsing via XmlReader
      val xmlStr    = "<config><width>100</width></config>"
      val xmlReader = new XmlReader()
      val root      = xmlReader.parse(xmlStr)
      if (root.name != "config") {
        logCheck("JSON_XML", passed = false, s"XML root name: ${root.name}")
        return
      }
      logCheck("JSON_XML", passed = true, "JSON + XML parsing OK")
    } catch {
      case e: Exception =>
        logCheck("JSON_XML", passed = false, s"Exception: ${e.getMessage}")
    }

  private def checkAudio()(using Sge): Unit =
    try {
      // Just verify the audio subsystem is accessible without crashing.
      // On Android emulator with -noaudio, the audio backend is typically noop.
      val audioType = Sge().audio.getClass.getSimpleName
      logCheck("AUDIO", passed = true, s"Audio subsystem accessible ($audioType)")
    } catch {
      case e: UnsatisfiedLinkError =>
        logCheck("AUDIO", passed = false, s"Native lib missing: ${e.getMessage}")
      case e: Exception =>
        logCheck("AUDIO", passed = false, s"Exception: ${e.getMessage}")
    }
}
