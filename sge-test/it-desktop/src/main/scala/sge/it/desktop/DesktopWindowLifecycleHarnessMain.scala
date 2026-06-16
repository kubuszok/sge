// SGE — ISS-551 DesktopWindow lifecycle harness entry point
//
// Standalone main() so GLFW's glfwInit() runs on the OS main thread (macOS
// Cocoa/AppKit requirement). The munit test launches this as a subprocess and
// reads the two single-line result files (one per clause). It is its OWN
// subprocess (separate from DesktopHarnessMain) so the pre-existing
// DesktopHarness SIGABRT (glfwGetMonitorName, ISS-485-era) and the FBO
// ClassCastException (ISS-572) cannot mask the windowing-lifecycle contracts.
//
// It drives the SGE windowing seams directly (WindowingOpsJvm + GlOpsJvm and
// DesktopWindow.create()/close()) — the very objects DesktopApplication uses —
// so the two clauses are produced by exactly the production code paths under
// test (see sge.DesktopWindowLifecycleCheck).
//
// Two clauses, two result files (args(0) = clause-1 file, args(1) = clause-3):
//   Clause 1 (no GLFW error callback installed): after WindowingOpsJvm().init(),
//     glfwSetErrorCallback must return a non-NULL previous callback. Against the
//     bug it returns NULL → RED.
//   Clause 3 (NPE on close-before-first-update): create a DesktopWindow and
//     close() it without ever calling update(). Against the bug _listener.pause()
//     NPEs → RED.

package sge.it.desktop

import sge.DesktopWindowLifecycleCheck

import java.io.{ File, PrintWriter }

object DesktopWindowLifecycleHarnessMain {

  private def writeResult(file: File, result: CheckResult): Unit = {
    val status = if (result.passed) "PASS" else "FAIL"
    val writer = new PrintWriter(file)
    try writer.write(s"$status:${result.message}")
    finally writer.close()
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: DesktopWindowLifecycleHarnessMain <clause1-results-file> <clause3-results-file>")
      System.exit(1)
    }

    val clause1File = new File(args(0))
    val clause3File = new File(args(1))

    try {
      val (clause1, clause3) = DesktopWindowLifecycleCheck.run()
      System.err.println(s"SGE-IT:DESKTOPWINDOW-ERRORCALLBACK:${if (clause1.passed) "PASS" else "FAIL"}:${clause1.message}")
      System.err.println(s"SGE-IT:DESKTOPWINDOW-CLOSE-BEFORE-UPDATE:${if (clause3.passed) "PASS" else "FAIL"}:${clause3.message}")
      writeResult(clause1File, clause1)
      writeResult(clause3File, clause3)
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
