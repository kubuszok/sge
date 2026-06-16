// SGE — Desktop integration test: Graphics.supportsExtension check (ISS-537)
//
// Pins the contract that DesktopGraphics.supportsExtension(name) consults the
// actual (ANGLE) GL context, not the GLFW context. The GLFW window is created
// with GLFW_NO_API, so glfwExtensionSupported() always returns false — the
// desktop backend must instead enumerate the live GL ES 3.0 extension set via
// glGetStringi(GL_EXTENSIONS, i) for i in 0 until glGetInteger(GL_NUM_EXTENSIONS).
//
// This check independently enumerates the live extension set, picks one that is
// genuinely present, and asserts:
//   1. supportsExtension(presentName) == true   (FAILS against the buggy code)
//   2. supportsExtension(absentName)  == false
// Against the current buggy DesktopGraphics, assertion #1 fails because
// glfwExtensionSupported() returns false for every name.

package sge.it.desktop.checks

import sge.Sge
import sge.graphics.{ GL20, GL30 }
import sge.it.desktop.CheckResult
import sge.utils.BufferUtils

/** Verifies that Graphics.supportsExtension reflects the live GL context's extension set. */
object SupportsExtensionCheck {

  // A name that no GL implementation will ever expose.
  private val absentExtension: String = "GL_THIS_DOES_NOT_EXIST_xyz"

  /** Enumerate the live GL ES 3.0 extension set via glGetStringi. The desktop ANGLE context is ES 3.0 core, where glGetString(GL_EXTENSIONS) returns null, so the indexed form is the only way to read
    * it.
    */
  private def enumerateExtensions(gl30: GL30): Seq[String] = {
    val countBuf = BufferUtils.newIntBuffer(16)
    gl30.glGetIntegerv(GL30.GL_NUM_EXTENSIONS, countBuf)
    val count = countBuf.get(0)
    (0 until count).iterator
      .map(i => gl30.glGetStringi(GL20.GL_EXTENSIONS, i))
      .filter(s => (s ne null) && s.nonEmpty)
      .toSeq
  }

  def run()(using Sge): CheckResult =
    try {
      val graphics = Sge().graphics

      // The desktop ANGLE context is GL ES 3.0 core; gl30 must be available.
      val maybeGl30 = graphics.gl30
      if (maybeGl30.isEmpty) {
        CheckResult(
          "supportsextension",
          passed = false,
          "gl30 unavailable — cannot enumerate the live GL extension set (no ES 3.0 context)"
        )
      } else {
        val gl30    = maybeGl30.get
        val present = enumerateExtensions(gl30)
        if (present.isEmpty) {
          // No live GL context / no extensions — we cannot exercise the positive
          // case. Report this honestly rather than fabricating a pass or a fail.
          CheckResult(
            "supportsextension",
            passed = false,
            "GL context exposed zero extensions via glGetStringi(GL_EXTENSIONS) — cannot test a present extension"
          )
        } else {
          val presentExtension = present.head

          val presentResult = graphics.supportsExtension(presentExtension)
          val absentResult   = graphics.supportsExtension(absentExtension)

          if (!presentResult) {
            CheckResult(
              "supportsextension",
              passed = false,
              s"supportsExtension('$presentExtension') returned false, expected true — the GL context " +
                s"exposes ${present.size} extension(s) including this one, but supportsExtension does not see them " +
                s"(GLFW_NO_API context has no GL state, so glfwExtensionSupported always returns false)"
            )
          } else if (absentResult) {
            CheckResult(
              "supportsextension",
              passed = false,
              s"supportsExtension('$absentExtension') returned true, expected false"
            )
          } else {
            CheckResult(
              "supportsextension",
              passed = true,
              s"supportsExtension reflects live GL set: present '$presentExtension' -> true, absent -> false " +
                s"(${present.size} extensions enumerated)"
            )
          }
        }
      }
    } catch {
      case e: Exception =>
        CheckResult("supportsextension", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
