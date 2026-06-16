// SGE — ISS-539 gl31/gl32 version-gate harness entry point
//
// Standalone main() so GLFW's glfwInit() runs on the OS main thread (macOS
// Cocoa/AppKit requirement). The munit test launches this as a subprocess and
// reads the single-line result file.

package sge.it.desktop

import java.io.File

object GlVersionGateHarnessMain {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.err.println("Usage: GlVersionGateHarnessMain <results-file-path>")
      System.exit(1)
    }

    val resultsFile = new File(args(0))
    val config      = new sge.DesktopApplicationConfig()
    config.title = "SGE glVersionGate IT"
    config.windowWidth = 100
    config.windowHeight = 100
    config.disableAudio = true
    config.vSyncEnabled = false

    try {
      val app: sge.Sge ?=> sge.ApplicationListener = new GlVersionGateHarness(resultsFile)
      sge.DesktopApplicationFactory(app, config)
    } catch {
      case e: UnsatisfiedLinkError =>
        System.err.println(s"Native library not found: ${e.getMessage}")
        System.exit(2)
      case e: Throwable =>
        System.err.println(s"Harness crashed: ${e.getClass.getName}: ${e.getMessage}")
        e.printStackTrace(System.err)
        System.exit(3)
    }
  }
}
