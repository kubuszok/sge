// SGE — Desktop integration test harness entry point
//
// Standalone main() so GLFW's glfwInit() runs on the OS main thread.
// On macOS, -XstartOnFirstThread ensures main() is thread 0 (required by Cocoa/AppKit).
// The munit test launches this as a subprocess and reads the results file.

package sge.it.desktop

import java.io.File

object DesktopHarnessMain {

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      System.err.println("Usage: DesktopHarnessMain <results-file-path>")
      System.exit(1)
    }

    val resultsFile = new File(args(0))
    val config      = new sge.DesktopApplicationConfig()
    config.title = "SGE Desktop IT"
    config.windowWidth = 100
    config.windowHeight = 100
    config.disableAudio = false
    config.vSyncEnabled = false

    try
      sge.DesktopApplicationFactory(new DesktopHarness(resultsFile), config)
    catch {
      case e: UnsatisfiedLinkError =>
        System.err.println(s"Native library not found: ${e.getMessage}")
        System.err.println(s"java.library.path: ${System.getProperty("java.library.path", "(not set)")}")
        System.exit(2)
      case e: Throwable =>
        System.err.println(s"Harness crashed: ${e.getClass.getName}: ${e.getMessage}")
        e.printStackTrace(System.err)
        System.exit(3)
    }
  }
}
