// SGE — RED regression test for ISS-806 (Scala.js / browser only)
//
// BrowserGraphics.newCursor (BrowserGraphics.scala:245-247) silently returns
// Nullable.empty ("deferred"). The libGDX GWT backend implements custom pixmap
// cursors: GwtGraphics.newCursor delegates to `new GwtCursor(pixmap, x, y)`
// (GwtGraphics.java:607-609), and GwtCursor builds a CSS data-URL cursor from
// the pixmap (GwtCursor.java:55-61):
//
//   cssCursorProperty = "url('" + pixmap.getCanvasElement().toDataUrl("image/png")
//                       + "')" + xHotspot + " " + yHotspot + ",auto";
//
// So a valid (RGBA8888, power-of-two) pixmap must yield a non-empty cursor whose
// CSS value is a `url(<data-url>) <x> <y>,auto` custom cursor. This suite pins
// that contract; it FAILS while newCursor returns Nullable.empty.

package sge

import munit.FunSuite
import org.scalajs.dom.{ HTMLCanvasElement, document }
import sge.graphics.{ GL30, Pixmap }
import sge.graphics.glutils.GLVersion
import sge.noop.NoopGL20
import lowlevel.Nullable

class BrowserGraphicsNewCursorRedSuite extends FunSuite {

  private def newGraphics(): BrowserGraphics = {
    val canvas: HTMLCanvasElement =
      document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
    val config    = new BrowserApplicationConfig()
    val glVersion = new GLVersion(Application.ApplicationType.WebGL, "WebGL 1.0", "vendor", "renderer")
    new BrowserGraphics(canvas, config, NoopGL20, Nullable.empty[GL30], glVersion)
  }

  test("newCursor with a valid RGBA8888 power-of-two Pixmap returns a non-empty cursor (ISS-806)") {
    val g = newGraphics()
    // 4x4 (power of two) RGBA8888 pixmap — the format/size GwtCursor accepts.
    val pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888)
    try {
      val res = g.newCursor(pixmap, Pixels(1), Pixels(2))
      assert(
        res.isDefined,
        "newCursor with a valid RGBA8888 power-of-two Pixmap must return a cursor (ISS-806); got Nullable.empty"
      )
    } finally pixmap.close()
  }

  test("the custom cursor is a CSS url(data-URL) cursor with hotspot (ISS-806)") {
    val g      = newGraphics()
    val pixmap = new Pixmap(4, 4, Pixmap.Format.RGBA8888)
    try {
      val res = g.newCursor(pixmap, Pixels(1), Pixels(2))
      assert(res.isDefined, "newCursor returned empty (ISS-806)")
      val css = res.get.asInstanceOf[BrowserCursor].cssCursorProperty
      assert(css.startsWith("url("), s"expected a url(...) cursor; got: $css")
      assert(css.contains("data:"), s"expected an embedded data URL; got: $css")
      assert(css.contains("1 2"), s"expected hotspot '1 2' in the CSS value; got: $css")
      assert(css.contains(",auto"), s"expected a ',auto' fallback in the CSS value; got: $css")
    } finally pixmap.close()
  }
}
