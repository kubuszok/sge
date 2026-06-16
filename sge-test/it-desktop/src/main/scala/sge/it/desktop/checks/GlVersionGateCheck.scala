// SGE — Desktop integration test: gl31/gl32 availability must reflect the real GL version (ISS-539)
//
// DesktopApplication (sge/src/main/scaladesktop/sge/DesktopApplication.scala:308-309)
// installs ONE AngleGL32 instance as gl20 AND gl30 AND gl31 AND gl32 (all
// Nullable-present), unconditionally:
//     val gl32 = glFactory()
//     window.graphics.initGL(gl32, Nullable(gl32), Nullable(gl32), Nullable(gl32))
// As a result DesktopGraphics.gl31Available (= _gl31.isDefined) and
// gl32Available (= _gl32.isDefined) are ALWAYS true, even though the desktop
// EGL context is requested as ES 3.0 (GlOpsJvm requests
// EGL_CONTEXT_MAJOR_VERSION=3, MINOR=0) and the live window reports GL=3.0.
// Code that checks `if (gl32Available) glDispatchCompute(...)` etc. then calls
// ES 3.1/3.2 entry points on a 3.0 context → GL errors. The instance advertises
// capabilities the context does not actually have.
//
// This check QUERIES the real GL version of the live ANGLE context (via
// glGetIntegerv(GL_MAJOR_VERSION/GL_MINOR_VERSION), with a glGetString(GL_VERSION)
// fallback for diagnostics) and asserts, policy-agnostically:
//   gl31Available == (major > 3 || (major == 3 && minor >= 1))
//   gl32Available == (major > 3 || (major == 3 && minor >= 2))
// On the real ES 3.0 ANGLE context (major=3, minor=0) BOTH expected values are
// false, but the current code returns true for both → the red. The assertion is
// robust even if ANGLE happened to report 3.1 (it would then expect
// gl31Available true / gl32Available false, still failing current code on gl32).

package sge.it.desktop.checks

import sge.Sge
import sge.graphics.GL30
import sge.it.desktop.CheckResult
import sge.utils.BufferUtils

/** Verifies that Graphics.gl31Available / gl32Available reflect the live GL context's actual version, not an unconditional truth. */
object GlVersionGateCheck {

  /** Query the live GL context's major.minor version. Primary path is glGetIntegerv(GL_MAJOR_VERSION / GL_MINOR_VERSION) — GL30-era integer queries available on any ES 3.0 core context. If those come
    * back as 0/0 (some drivers gate them), fall back to parsing glGetString(GL_VERSION), whose ES form is "OpenGL ES <major>.<minor> <vendor>".
    */
  private def queryVersion(gl30: GL30): Either[String, (Int, Int)] = {
    val majBuf = BufferUtils.newIntBuffer(1)
    val minBuf = BufferUtils.newIntBuffer(1)
    gl30.glGetIntegerv(GL30.GL_MAJOR_VERSION, majBuf)
    gl30.glGetIntegerv(GL30.GL_MINOR_VERSION, minBuf)
    val major = majBuf.get(0)
    val minor = minBuf.get(0)
    if (major > 0) {
      Right((major, minor))
    } else {
      // Fallback: parse the version string, e.g. "OpenGL ES 3.0 (ANGLE ...)".
      val versionString = gl30.glGetString(sge.graphics.GL20.GL_VERSION)
      if ((versionString eq null) || versionString.isEmpty) {
        Left("glGetIntegerv(GL_MAJOR_VERSION)==0 and glGetString(GL_VERSION) returned null/empty — cannot determine GL version")
      } else {
        parseVersionString(versionString) match {
          case Some(mm) => Right(mm)
          case None     => Left(s"could not parse GL version from glGetString(GL_VERSION)='$versionString'")
        }
      }
    }
  }

  /** Extract the first "<major>.<minor>" pair from a GL version string. */
  private def parseVersionString(s: String): Option[(Int, Int)] = {
    val m = """(\d+)\.(\d+)""".r.findFirstMatchIn(s)
    m.map(mm => (mm.group(1).toInt, mm.group(2).toInt))
  }

  def run()(using Sge): CheckResult =
    try {
      val graphics  = Sge().graphics
      val maybeGl30 = graphics.gl30
      if (maybeGl30.isEmpty) {
        CheckResult(
          "glversiongate",
          passed = false,
          "gl30 unavailable — cannot query the live GL version (no ES 3.0 context)"
        )
      } else {
        val gl30 = maybeGl30.get
        queryVersion(gl30) match {
          case Left(msg) =>
            CheckResult("glversiongate", passed = false, msg)
          case Right((major, minor)) =>
            // Capability the live context actually has, derived purely from the
            // queried version — policy-agnostic.
            val expectedGl31 = major > 3 || (major == 3 && minor >= 1)
            val expectedGl32 = major > 3 || (major == 3 && minor >= 2)

            val actualGl31 = graphics.gl31Available
            val actualGl32 = graphics.gl32Available

            if (actualGl31 != expectedGl31 || actualGl32 != expectedGl32) {
              CheckResult(
                "glversiongate",
                passed = false,
                s"gl31Available/gl32Available do not match the live GL version $major.$minor: " +
                  s"gl31Available=$actualGl31 (expected $expectedGl31), " +
                  s"gl32Available=$actualGl32 (expected $expectedGl32). " +
                  "DesktopApplication installs one AngleGL32 as gl30/gl31/gl32 unconditionally, " +
                  s"so both report available even though the context is ES $major.$minor (ISS-539)."
              )
            } else {
              CheckResult(
                "glversiongate",
                passed = true,
                s"gl31Available=$actualGl31 / gl32Available=$actualGl32 match the live GL version $major.$minor"
              )
            }
        }
      }
    } catch {
      case e: Exception =>
        CheckResult("glversiongate", passed = false, s"Exception: ${e.getClass.getSimpleName}: ${e.getMessage}")
    }
}
