// SGE — pixel-correctness tests for the Scala.js BMP/GIF/JPEG decoders.
//
// Fixtures are real encoded images produced ON THE JVM by javax.imageio.ImageIO
// (and, for the 32-bit BMP, a small hand-written BI_RGB encoder) over a KNOWN
// 16x16 image with four solid colored quadrants — top-left red, top-right
// green, bottom-left blue, bottom-right white — plus a 16x16 horizontal
// grayscale gradient for JPEG. The encoded bytes are embedded here as base64 so
// the decode result is checked against independently-known ground truth, never
// against the decoder's own output. See genfixtures.sc at the repository root
// for the (one-shot, JVM-run) fixture generator.
//
//   - BMP / GIF are lossless: assert exact RGBA at sampled pixels.
//   - JPEG is lossy: assert each channel within a tolerance of the expected
//     value at interior sample points (edges between quadrants ring under the
//     DCT, so quadrant centers are sampled).

package sge
package platform

import java.util.Base64

class ImageDecoderJsSuite extends munit.FunSuite {

  private def bytes(b64: String): Array[Byte] = Base64.getDecoder.decode(b64)

  // -- Fixtures (base64 of JVM-encoded images; see genfixtures.sc) ------------

  private val Bmp24 =
    "Qk02AwAAAAAAADYAAAAoAAAAEAAAABAAAAABABgAAAAAAAADAAAAAAAAAAAAAAAAAAAAAAAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA/////////////////////////////////wAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA/////////////////////////////////wAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA/////////////////////////////////wAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA/////////////////////////////////wAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA/////////////////////////////////wAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA/////////////////////////////////wAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA/////////////////////////////////wAA/wAA/wAA/wAA/wAA/wAA/wAA/wAA////////////////////////////////AAD/AAD/AAD/AAD/AAD/AAD/AAD/AAD/AP8AAP8AAP8AAP8AAP8AAP8AAP8AAP8AAAD/AAD/AAD/AAD/AAD/AAD/AAD/AAD/AP8AAP8AAP8AAP8AAP8AAP8AAP8AAP8AAAD/AAD/AAD/AAD/AAD/AAD/AAD/AAD/AP8AAP8AAP8AAP8AAP8AAP8AAP8AAP8AAAD/AAD/AAD/AAD/AAD/AAD/AAD/AAD/AP8AAP8AAP8AAP8AAP8AAP8AAP8AAP8AAAD/AAD/AAD/AAD/AAD/AAD/AAD/AAD/AP8AAP8AAP8AAP8AAP8AAP8AAP8AAP8AAAD/AAD/AAD/AAD/AAD/AAD/AAD/AAD/AP8AAP8AAP8AAP8AAP8AAP8AAP8AAP8AAAD/AAD/AAD/AAD/AAD/AAD/AAD/AAD/AP8AAP8AAP8AAP8AAP8AAP8AAP8AAP8AAAD/AAD/AAD/AAD/AAD/AAD/AAD/AAD/AP8AAP8AAP8AAP8AAP8AAP8AAP8AAP8A"

  private val Bmp32 =
    "Qk02BAAAAAAAADYAAAAoAAAAEAAAABAAAAABACAAAAAAAAAEAAATCwAAEwsAAAAAAAAAAAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD///8A////AP///wD///8A////AP///wD///8A////AP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA////AP///wD///8A////AP///wD///8A////AP///wD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP///wD///8A////AP///wD///8A////AP///wD///8A/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD///8A////AP///wD///8A////AP///wD///8A////AP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA////AP///wD///8A////AP///wD///8A////AP///wD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP///wD///8A////AP///wD///8A////AP///wD///8A/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD///8A////AP///wD///8A////AP///wD///8A////AP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA////AP///wD///8A////AP///wD///8A////AP///wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAA/wAAAP8AAAD/AAAA/wAAAP8AAAD/AAAA/wAAAP8AAA=="

  private val Gif =
    "R0lGODlhEAAQAPEAAAAA/wD/AP8AAP///ywAAAAAEAAQAEEIRgAFCBwoMIDBgwYBKFyocIDDhw4JEkSIkCFDiBAlDqR4UGNBjgEsLsT4UGRDkgM8CgAZQCVLlyBhcjQJAOUAmjZxotRJMiAAOw=="

  private val JpegQuad =
    "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAQABADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDi69KrwavuKjiLgz6l7P8Af83Nf7Ntrf3n3DivEf6zex09n7Pm/vX5uX/Da3L57n//2Q=="

  private val JpegGrad =
    "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAAQABADASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDzrwX/AMs6+ifBf/LOvnbwX/yzr6J8F/8ALOgD/9k="

  // -- Helpers ----------------------------------------------------------------

  /** (r,g,b,a) of pixel (x,y) from a DecodeResult. */
  private def px(res: Gdx2dOps.DecodeResult, x: Int, y: Int): (Int, Int, Int, Int) = {
    val buf = res.pixels
    val i   = (y * res.width + x) * 4
    (buf.get(i) & 0xff, buf.get(i + 1) & 0xff, buf.get(i + 2) & 0xff, buf.get(i + 3) & 0xff)
  }

  private def assertRGBA(actual: (Int, Int, Int, Int), r: Int, g: Int, b: Int, a: Int, clue: String): Unit =
    assertEquals(actual, (r, g, b, a), clue)

  private def assertNear(actual: (Int, Int, Int, Int), r: Int, g: Int, b: Int, tol: Int, clue: String): Unit = {
    val (ar, ag, ab, aa) = actual
    assert(scala.math.abs(ar - r) <= tol, s"$clue R: got $ar expected ~$r (tol $tol)")
    assert(scala.math.abs(ag - g) <= tol, s"$clue G: got $ag expected ~$g (tol $tol)")
    assert(scala.math.abs(ab - b) <= tol, s"$clue B: got $ab expected ~$b (tol $tol)")
    assertEquals(aa, 255, s"$clue A should be opaque")
  }

  // Quadrant centers and expected colors for the 16x16 quad image.
  private val Quadrants = List(
    (4, 4, 255, 0, 0, "top-left red"),
    (12, 4, 0, 255, 0, "top-right green"),
    (4, 12, 0, 0, 255, "bottom-left blue"),
    (12, 12, 255, 255, 255, "bottom-right white")
  )

  // ==========================================================================
  // BMP — 24-bit
  // ==========================================================================

  test("BMP 24-bit BI_RGB decodes to exact RGBA quadrants") {
    val res = BmpDecoderJs.decode(bytes(Bmp24), 0, bytes(Bmp24).length).get
    assertEquals(res.width, 16)
    assertEquals(res.height, 16)
    assertEquals(res.format, 4)
    Quadrants.foreach { case (x, y, r, g, b, name) =>
      assertRGBA(px(res, x, y), r, g, b, 255, s"BMP24 $name @ ($x,$y)")
    }
  }

  // ==========================================================================
  // BMP — 32-bit
  // ==========================================================================

  test("BMP 32-bit BI_RGB decodes to exact opaque RGBA quadrants") {
    val data = bytes(Bmp32)
    val res  = BmpDecoderJs.decode(data, 0, data.length).get
    assertEquals(res.width, 16)
    assertEquals(res.height, 16)
    Quadrants.foreach { case (x, y, r, g, b, name) =>
      assertRGBA(px(res, x, y), r, g, b, 255, s"BMP32 $name @ ($x,$y)")
    }
  }

  // ==========================================================================
  // GIF — palettized, first frame
  // ==========================================================================

  test("GIF decodes to exact RGBA quadrants") {
    val data = bytes(Gif)
    val res  = GifDecoderJs.decode(data, 0, data.length).get
    assertEquals(res.width, 16)
    assertEquals(res.height, 16)
    Quadrants.foreach { case (x, y, r, g, b, name) =>
      assertRGBA(px(res, x, y), r, g, b, 255, s"GIF $name @ ($x,$y)")
    }
  }

  // ==========================================================================
  // JPEG — baseline, 4:2:0 (lossy: tolerance check)
  // ==========================================================================

  test("JPEG baseline (4:2:0) decodes quadrants within tolerance") {
    val data = bytes(JpegQuad)
    val res  = JpegDecoderJs.decode(data, 0, data.length).get
    assertEquals(res.width, 16)
    assertEquals(res.height, 16)
    // Ground truth is the SAME JPEG bytes decoded by the JVM reference decoder
    // (javax.imageio.ImageIO), captured at the quadrant centers — NOT the
    // pre-compression colors, since lossy 4:2:0 on saturated primaries shifts
    // them (e.g. red 255 -> ~237 even in the reference decoder). A tight
    // tolerance of 10 then proves this decoder agrees with a known-good decoder
    // rather than merely "is in the right ballpark".
    val Tol                = 10
    val ReferenceQuadrants = List(
      (4, 4, 237, 9, 0, "top-left red"),
      (12, 4, 14, 249, 0, "top-right green"),
      (4, 12, 9, 0, 252, "bottom-left blue"),
      (12, 12, 248, 255, 255, "bottom-right white")
    )
    ReferenceQuadrants.foreach { case (x, y, r, g, b, name) =>
      assertNear(px(res, x, y), r, g, b, Tol, s"JPEG $name @ ($x,$y)")
    }
  }

  test("JPEG baseline grayscale gradient is monotonic and bracketed") {
    val data = bytes(JpegGrad)
    val res  = JpegDecoderJs.decode(data, 0, data.length).get
    assertEquals(res.width, 16)
    assertEquals(res.height, 16)
    // Ground truth: the same JPEG bytes decoded by the JVM reference decoder
    // (javax.imageio.ImageIO) on the middle row — a left(dark)->right(bright)
    // ramp. Tight tolerance 8 proves agreement with the reference decoder.
    val Tol          = 8
    val ReferenceRow = List((0, 0), (4, 67), (8, 137), (12, 204), (15, 255))
    ReferenceRow.foreach { case (x, expected) =>
      val (r, g, b, a) = px(res, x, 8)
      assert(scala.math.abs(r - expected) <= Tol, s"grad x=$x R got $r expected ~$expected")
      // Gray: channels should be close to each other.
      assert(scala.math.abs(r - g) <= Tol && scala.math.abs(g - b) <= Tol, s"grad x=$x not gray: ($r,$g,$b)")
      assertEquals(a, 255)
    }
    // Monotonic non-decreasing brightness left->right (allow tiny lossy dips).
    var prev = -100
    var x    = 0
    while (x < 16) {
      val (r, _, _, _) = px(res, x, 8)
      assert(r >= prev - 12, s"grad not monotonic at x=$x: $r < $prev")
      prev = r
      x += 1
    }
  }

  // ==========================================================================
  // Dispatch: non-matching magic bytes -> None
  // ==========================================================================

  test("each decoder returns None for foreign / empty input") {
    val notImage = Array[Byte](1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    assertEquals(BmpDecoderJs.decode(notImage, 0, notImage.length), None)
    assertEquals(GifDecoderJs.decode(notImage, 0, notImage.length), None)
    assertEquals(JpegDecoderJs.decode(notImage, 0, notImage.length), None)
    // A PNG must not be claimed by the others.
    val pngSig = Array[Byte](-119, 80, 78, 71, 13, 10, 26, 10, 0, 0)
    assertEquals(BmpDecoderJs.decode(pngSig, 0, pngSig.length), None)
    assertEquals(GifDecoderJs.decode(pngSig, 0, pngSig.length), None)
    assertEquals(JpegDecoderJs.decode(pngSig, 0, pngSig.length), None)
  }
}
