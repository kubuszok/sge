// SGE — RED regression test for ISS-536 (Scala.js / browser only)
//
// BrowserGraphics.density must satisfy the libGDX Graphics.getDensity() contract:
//   density == ppiX / 160
// where BrowserGraphics defines ppiX = 96 * devicePixelRatio. The buggy code
// computes density = devicePixelRatio / 160 (missing the * 96 factor), making it
// 96x too small. Any UI scaled by graphics.density breaks on the browser only.
//
// This suite pins the contract; it FAILS until the * 96 factor is restored.

package sge

import munit.FunSuite
import org.scalajs.dom.{ HTMLCanvasElement, document }
import sge.graphics.GL30
import sge.graphics.glutils.GLVersion
import sge.noop.NoopGL20
import lowlevel.Nullable

class BrowserGraphicsDensityRedSuite extends FunSuite {

  // Real BrowserGraphics: real canvas + no-op GL (the density/ppiX path never
  // touches GL). getNativeScreenDensity() reads window.devicePixelRatio, which
  // defaults to 1.0 in the node/jsdom test environment.
  private def newGraphics(): BrowserGraphics = {
    val canvas: HTMLCanvasElement =
      document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
    val config    = new BrowserApplicationConfig()
    val glVersion = new GLVersion(Application.ApplicationType.WebGL, "WebGL 1.0", "vendor", "renderer")
    new BrowserGraphics(canvas, config, NoopGL20, Nullable.empty[GL30], glVersion)
  }

  test("density satisfies the libGDX contract density == ppiX / 160") {
    val g = newGraphics()
    // libGDX Graphics.getDensity(): density = ppiX / 160. Independent of the
    // actual devicePixelRatio value, so it pins the formula directly.
    assertEqualsFloat(g.density, g.ppiX / 160f, 0.0001f)
  }

  test("density equals 96 * devicePixelRatio / 160 (concrete value at dpr=1.0)") {
    val g = newGraphics()
    // In the node/jsdom test env devicePixelRatio defaults to 1.0, so
    // ppiX = 96 * 1.0 = 96 and the correct density = 96 / 160 = 0.6.
    // The buggy code returns 1.0 / 160 = 0.00625 — a 96x gap.
    assertEqualsFloat(g.ppiX, 96f, 0.0001f) // sanity: dpr resolved to 1.0
    assertEqualsFloat(g.density, 0.6f, 0.0001f)
  }
}
