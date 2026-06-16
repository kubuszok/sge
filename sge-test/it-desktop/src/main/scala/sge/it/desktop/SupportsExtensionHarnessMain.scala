// SGE — ISS-537 supportsExtension harness entry point
//
// Standalone main() so GLFW's glfwInit() runs on the OS main thread (macOS
// Cocoa/AppKit requirement). The munit test launches this as a subprocess and
// reads the single-line result file.

package sge.it.desktop

import java.io.File

object SupportsExtensionHarnessMain {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.err.println("Usage: SupportsExtensionHarnessMain <results-file-path>")
      System.exit(1)
    }

    val resultsFile = new File(args(0))
    val config      = new sge.DesktopApplicationConfig()
    config.title = "SGE supportsExtension IT"
    config.windowWidth = 100
    config.windowHeight = 100
    config.disableAudio = true
    config.vSyncEnabled = false

    try {
      val app: sge.Sge ?=> sge.ApplicationListener = new SupportsExtensionHarness(resultsFile)
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
