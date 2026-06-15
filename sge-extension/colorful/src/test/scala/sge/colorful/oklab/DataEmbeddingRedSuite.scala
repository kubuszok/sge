package sge
package colorful
package oklab

/** Red test for ISS-527: colorful's Oklab gamut table ([[Gamut.GAMUT_DATA]], 65536 bytes) was externalized into a Base64 classpath resource (`/sge/colorful/oklab/gamut.b64`) loaded with
  * `getClass.getResourceAsStream`. On Scala.js (and plain Scala Native) that resource is absent, so the stream is null and the code SILENTLY falls back to an all-zeros `new Array[Byte](65536)` —
  * meaning `inGamut`/`limitToGamut` produce WRONG results on those platforms with no error. The data must instead be embedded in source (as upstream `com.github.tommyettinger.colorful.oklab.Gamut`
  * does) so it is correct on every platform.
  *
  * Upstream reference: original-src/colorful-gdx/.../com/github/tommyettinger/colorful/oklab/Gamut.java (commit e4a5fd960eef746ca5aa826063432fb79666d74f) — GAMUT_DATA embedded as a string literal.
  *
  * Expected byte values were captured from the JVM resource so the assertions pin the ACTUAL gamut data, not merely its length. The non-zero assertion specifically catches the silent all-zeros JS
  * fallback. This suite PASSES on JVM (real data present) and FAILS on JS/Native (all-zeros fallback) — that asymmetry IS the bug.
  */
class DataEmbeddingRedSuite extends munit.FunSuite {

  test("GAMUT_DATA has the correct length") {
    assertEquals(Gamut.GAMUT_DATA.length, 65536, "GAMUT_DATA must be 65536 bytes (256 lightness x 256 hue)")
  }

  test("GAMUT_DATA is not the all-zeros fallback") {
    val data = Gamut.GAMUT_DATA
    assert(data.exists(_ != 0), "GAMUT_DATA must contain non-zero bytes (an all-zeros array is the silent JS/Native fallback bug)")
  }

  test("GAMUT_DATA has the expected known bytes") {
    val data = Gamut.GAMUT_DATA
    assertEquals(data(0).toInt, 2, "GAMUT_DATA[0]")
    assertEquals(data(256).toInt, 4, "GAMUT_DATA[256]")
    assertEquals(data(32768).toInt, 67, "GAMUT_DATA[32768]")
    assertEquals(data(65535).toInt, 2, "GAMUT_DATA[65535]")
  }
}
