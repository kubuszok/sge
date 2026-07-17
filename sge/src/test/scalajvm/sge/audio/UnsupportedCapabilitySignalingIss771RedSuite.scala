// SGE — RED reproducer for ISS-771 (ad-hoc unsupported-capability signaling), desktop/JVM side.
//
// Unsupported capabilities are signaled inconsistently across the codebase:
//   * desktop audio recorder  -> raw java.lang.UnsupportedOperationException
//       (MiniaudioEngine.scala:48/176, DesktopApplication.scala:57)
//   * browser sockets         -> raw java.lang.UnsupportedOperationException (BrowserNet.scala:32-38)
//   * browser audio device/recorder + browser files -> sge.utils.SgeError.InvalidInput
//       (DefaultBrowserAudio.scala:30/33, BrowserFiles.scala:36/46/49/52)
//
// The target convention (a single SgeError.Unsupported flavor + capability probes) is captured here
// as a uniform CONTRACT that every unsupported-capability site should satisfy: it throws an
// sge.utils.SgeError (the project's own error hierarchy — NOT a raw JDK exception) whose variant is
// NOT InvalidInput (an "unsupported capability" is a distinct semantic from "bad user input").
//
// This suite pins the DESKTOP RECORDER site. It FAILS today because that site throws a raw
// UnsupportedOperationException — which is neither an SgeError. The sibling scala.js suite
// (UnsupportedCapabilitySignalingIss771JsRedSuite) pins the browser files/sockets sites against the
// same contract. Introducing SgeError.Unsupported and flipping every site flips both suites green.

package sge
package audio

import munit.FunSuite
import sge.utils.SgeError

class UnsupportedCapabilitySignalingIss771RedSuite extends FunSuite {

  test("ISS-771 (RED): desktop newAudioRecorder signals unsupported via SgeError, not a raw JDK exception") {
    val engine = MiniaudioEngine(new WaveDFakeAudioOps)
    val thrown = intercept[Throwable](engine.newAudioRecorder(44100, isMono = true))

    assert(
      thrown.isInstanceOf[SgeError],
      s"unsupported capability must be signaled via sge.utils.SgeError, not ${thrown.getClass.getName} " +
        "(desktop recorder currently throws a raw java.lang.UnsupportedOperationException)"
    )
    assert(
      !thrown.isInstanceOf[SgeError.InvalidInput],
      "an unsupported capability must not be reported as SgeError.InvalidInput — that variant means " +
        "bad user input; unsupported capability is a distinct semantic (target: SgeError.Unsupported)"
    )
  }
}
