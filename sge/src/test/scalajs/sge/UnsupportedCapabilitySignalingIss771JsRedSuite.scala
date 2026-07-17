// SGE — RED reproducer for ISS-771 (ad-hoc unsupported-capability signaling), browser/Scala.js side.
//
// Companion to the JVM suite (UnsupportedCapabilitySignalingIss771RedSuite). Same uniform CONTRACT:
// an unsupported-capability call throws an sge.utils.SgeError (the project's own error hierarchy —
// NOT a raw JDK exception) whose variant is NOT InvalidInput ("unsupported capability" is a distinct
// semantic from "bad user input"; target: a single SgeError.Unsupported).
//
// This suite pins the two contrasting browser sites named in ISS-771:
//   * browser files   -> throws SgeError.InvalidInput  (BrowserFiles.scala:46) — wrong VARIANT
//   * browser sockets -> throws raw UnsupportedOperationException (BrowserNet.scala:38) — wrong TYPE
// Both FAIL the contract today. Introducing SgeError.Unsupported and flipping the sites flips green.

package sge

import munit.FunSuite
import sge.files.{ BrowserAssetLoader, BrowserFiles }
import sge.net.SocketHints
import sge.utils.SgeError

class UnsupportedCapabilitySignalingIss771JsRedSuite extends FunSuite {

  test("ISS-771 (RED): browser files unsupported capability is not reported as SgeError.InvalidInput") {
    val files  = new BrowserFiles(new BrowserAssetLoader)
    val thrown = intercept[Throwable](files.external("save.dat"))

    assert(
      thrown.isInstanceOf[SgeError],
      s"unsupported capability must be signaled via sge.utils.SgeError, got ${thrown.getClass.getName}"
    )
    assert(
      !thrown.isInstanceOf[SgeError.InvalidInput],
      "browser 'External files not supported' must not be SgeError.InvalidInput — that variant means " +
        "bad user input; unsupported capability is a distinct semantic (target: SgeError.Unsupported)"
    )
  }

  test("ISS-771 (RED): browser sockets signal unsupported via SgeError, not a raw JDK exception") {
    val net    = new BrowserNet(new BrowserApplicationConfig())
    val thrown = intercept[Throwable](net.newClientSocket(Net.Protocol.TCP, "localhost", 8080, new SocketHints))

    assert(
      thrown.isInstanceOf[SgeError],
      s"unsupported capability must be signaled via sge.utils.SgeError, not ${thrown.getClass.getName} " +
        "(browser sockets currently throw a raw java.lang.UnsupportedOperationException)"
    )
    assert(
      !thrown.isInstanceOf[SgeError.InvalidInput],
      "unsupported capability must not be reported as SgeError.InvalidInput (target: SgeError.Unsupported)"
    )
  }
}
