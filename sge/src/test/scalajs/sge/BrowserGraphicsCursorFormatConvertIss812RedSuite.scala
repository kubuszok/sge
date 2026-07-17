// SGE — RED regression test for ISS-812 (Scala.js / browser only)
//
// Cursor pixmap FORMAT POLICY diverges across platforms:
//
//   - Desktop (DesktopCursor.create, DesktopCursor.scala:74-78) copies the cursor
//     image into an RGBA8888 pixmap with blending disabled, which CONVERTS
//     non-RGBA8888 input instead of rejecting it (wave-C adjudicated, mirroring
//     Lwjgl3Cursor.java:68-70's implicit convert).
//   - Browser (BrowserGraphics.newCursor, BrowserGraphics.scala:252-254) THROWS
//     SgeError.GraphicsError("Cursor image pixmap is not in RGBA8888 format.")
//     (faithful to GwtCursor.java:32-34).
//
// So the SAME non-RGBA8888 pixmap yields a working cursor on desktop but a hard
// throw in the browser — an SGE cross-platform parity violation. The orchestrator
// adjudication (ISS-812) unifies the policy on CONVERT for JS too: newCursor must
// convert a non-RGBA8888 pixmap to RGBA8888 (pre-PNG-encoding) and succeed, just
// like DesktopCursor.create does.
//
// This suite pins that contract. It FAILS while newCursor throws on a non-RGBA8888
// pixmap.

package sge

import munit.FunSuite
import org.scalajs.dom.{ HTMLCanvasElement, document }
import sge.graphics.{ GL30, Pixmap }
import sge.graphics.glutils.GLVersion
import sge.noop.NoopGL20
import lowlevel.Nullable

class BrowserGraphicsCursorFormatConvertIss812RedSuite extends FunSuite {

  private def newGraphics(): BrowserGraphics = {
    val canvas: HTMLCanvasElement =
      document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
    val config    = new BrowserApplicationConfig()
    val glVersion = new GLVersion(Application.ApplicationType.WebGL, "WebGL 1.0", "vendor", "renderer")
    new BrowserGraphics(canvas, config, NoopGL20, Nullable.empty[GL30], glVersion)
  }

  test("newCursor converts a non-RGBA8888 (RGB888) power-of-two Pixmap instead of throwing (ISS-812)") {
    val g = newGraphics()
    // 4x4 (power of two) RGB888 pixmap: valid dimensions/hotspot, only the format
    // differs from RGBA8888. Desktop accepts this by converting; JS must too.
    val pixmap = new Pixmap(4, 4, Pixmap.Format.RGB888)
    try {
      val res = g.newCursor(pixmap, Pixels(1), Pixels(2))
      assert(
        res.isDefined,
        "newCursor with a non-RGBA8888 pixmap must convert-and-succeed (ISS-812), matching DesktopCursor.create; got Nullable.empty"
      )
      val css = res.get.asInstanceOf[BrowserCursor].cssCursorProperty
      assert(css.startsWith("url("), s"expected a url(...) cursor; got: $css")
      assert(css.contains("data:"), s"expected an embedded data URL; got: $css")
      assert(css.contains("1 2"), s"expected hotspot '1 2' in the CSS value; got: $css")
      assert(css.contains(",auto"), s"expected a ',auto' fallback in the CSS value; got: $css")
    } finally pixmap.close()
  }
}
