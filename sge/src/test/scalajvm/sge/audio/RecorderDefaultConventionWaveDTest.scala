// SGE — ISS-771/785 follow-up (wave-D-K bounce #1): pins the two recorderFactory DEFAULTS that the
// frozen red suites cannot see.
//
// The frozen suites exercise MiniaudioEngine.apply's default (companion, MiniaudioEngine.scala:~176).
// Two more throwing defaults exist and were migrated to SgeError.Unsupported in the same wave:
//   * the MiniaudioEngine CLASS-constructor default (MiniaudioEngine.scala:48)
//   * the DesktopApplication constructor default   (DesktopApplication.scala:57)
// Both were mutation-invisible; this suite invokes each directly so a regression back to a raw
// java.lang exception (or to InvalidInput) fails a test.

package sge
package audio

import munit.FunSuite
import sge.utils.SgeError

class RecorderDefaultConventionWaveDTest extends FunSuite {

  test("ISS-771: MiniaudioEngine class-constructor default recorderFactory signals SgeError.Unsupported") {
    // Bypasses the companion apply so the CLASS default parameter (MiniaudioEngine.scala:48) is used.
    val engine = new MiniaudioEngine(16, 512, 9, new WaveDFakeAudioOps)
    val thrown = intercept[Throwable](engine.newAudioRecorder(44100, isMono = true))
    assert(
      thrown.isInstanceOf[SgeError.Unsupported],
      s"class-constructor default must throw SgeError.Unsupported, got ${thrown.getClass.getName}"
    )
  }

  test("ISS-771/785: DesktopApplication constructor default recorderFactory signals SgeError.Unsupported") {
    // The default cannot be reached without booting a full desktop application (GLFW/ANGLE), so the
    // compiler-generated default-argument accessor on the synthetic companion is invoked directly.
    val moduleCls = Class.forName("sge.DesktopApplication$")
    // Field.get ignores the receiver for static fields; the Class object avoids a bare null.
    val module  = moduleCls.getField("MODULE$").get(moduleCls)
    val factory = moduleCls.getMethod("$lessinit$greater$default$7").invoke(module).asInstanceOf[(Int, Boolean) => AudioRecorder]
    val thrown  = intercept[Throwable](factory(44100, true))
    assert(
      thrown.isInstanceOf[SgeError.Unsupported],
      s"DesktopApplication default recorderFactory must throw SgeError.Unsupported, got ${thrown.getClass.getName}"
    )
  }
}
