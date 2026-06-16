// SGE — ISS-538 multi-window EGL harness entry point
//
// Standalone main() so GLFW's glfwInit() runs on the OS main thread (macOS
// Cocoa/AppKit requirement). The munit test launches this as a subprocess and
// reads the two single-line result files (one per clause). It is its OWN
// subprocess (separate from DesktopHarnessMain) so the pre-existing
// DesktopHarness SIGABRT (glfwGetMonitorName, ISS-485-era) and the FBO
// ClassCastException (ISS-572) cannot mask the multi-window EGL contract.
//
// This harness deliberately does NOT go through DesktopApplicationFactory /
// ApplicationListener: the bug is in how a SECOND window's EGL context is
// created and torn down on the shared ANGLE EGL display, and the factory
// creates only one window before rendering. Instead it drives the SGE
// platform seams (WindowingOpsJvm + GlOpsJvm) directly — the very same
// objects DesktopApplication uses (DesktopApplicationFactory wires
// WindowingOpsJvm() + GlOpsJvm()) — so the contexts created here are produced
// by exactly the production code path under test (GlOpsJvm.createContext /
// destroyContext / makeCurrent).
//
// Two clauses, two result files (args(0) = clause-1 file, args(1) = clause-2):
//   Clause 1 (eglTerminate kills all contexts): create TWO GLFW windows +
//     TWO EGL contexts on the shared display, destroyContext(ctx1),
//     makeCurrent(ctx2), do a GL call → ctx2 must still be valid. Against the
//     bug, destroyContext calls eglTerminate(display) which tears down the
//     ENTIRE shared display, invalidating ctx2 → GL call fails → RED.
//   Clause 2 (no cross-window GL sharing): create a texture in window 1's
//     context, make window 2 current, query the texture id → it must be
//     recognized (shared namespace). Against the bug, createContext passes
//     EGL_NO_CONTEXT as the share context, so the id is unknown in window 2 →
//     RED. See MultiWindowEglCheck for how the fix flips this green.

package sge.it.desktop

import sge.it.desktop.checks.MultiWindowEglCheck

import java.io.{ File, PrintWriter }

object MultiWindowEglHarnessMain {

  private def writeResult(file: File, result: CheckResult): Unit = {
    val status = if (result.passed) "PASS" else "FAIL"
    val writer = new PrintWriter(file)
    try writer.write(s"$status:${result.message}")
    finally writer.close()
  }

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: MultiWindowEglHarnessMain <clause1-results-file> <clause2-results-file>")
      System.exit(1)
    }

    val clause1File = new File(args(0))
    val clause2File = new File(args(1))

    try {
      val (clause1, clause2) = MultiWindowEglCheck.run()
      System.err.println(s"SGE-IT:MULTIWINDOWEGL-TERMINATE:${if (clause1.passed) "PASS" else "FAIL"}:${clause1.message}")
      System.err.println(s"SGE-IT:MULTIWINDOWEGL-SHARING:${if (clause2.passed) "PASS" else "FAIL"}:${clause2.message}")
      writeResult(clause1File, clause1)
      writeResult(clause2File, clause2)
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
