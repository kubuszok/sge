// SGE — RED regression test for ISS-784 (Scala.js / browser only)
//
// The synchronous Scala.js decoders reject two encodings that JVM and Native
// (gdx2d / stb_image) load fine, so the SAME asset works on desktop and crashes
// in the browser:
//
//   1. Interlaced (Adam7) PNG: PngDecoderJs.scala:108 throws PngError
//      ("interlaced PNG not supported"), which decode() maps to None.
//   2. Progressive JPEG (SOF2): JpegDecoderJs bails at decodeMarkers
//      (JpegDecoderJs.scala:167-169) and returns None.
//
// A None from the decoder makes Gdx2DPixmap raise "Error loading pixmap", i.e. a
// hard load failure on the browser only. This suite pins cross-platform parity:
// both encodings must decode to the correct pixels. It FAILS while the decoders
// return None.
//
// Fixtures (base64) were produced ON THE JVM and independently validated:
//   - The interlaced PNG is a real Adam7 PNG (interlace byte = 1) round-tripped
//     through javax.imageio.ImageIO to confirm every pixel; image model is
//     pixel (x,y) -> RGB(x*32, y*32, 128) on an 8x8 truecolor grid, so every
//     pixel is unique and a de-interlace ordering bug is detectable.
//   - The progressive JPEG is a real SOF2 JPEG; ground-truth quadrant colors are
//     the SAME bytes decoded by ImageIO. See scratchpad genfix.scala.

package sge
package platform

import java.util.Base64

class PngInterlacedProgressiveRedSuite extends munit.FunSuite {

  private def bytes(b64: String): Array[Byte] = Base64.getDecoder.decode(b64)

  private def px(res: Gdx2dOps.DecodeResult, x: Int, y: Int): (Int, Int, Int, Int) = {
    val buf = res.pixels
    val i   = (y * res.width + x) * 4
    (buf.get(i) & 0xff, buf.get(i + 1) & 0xff, buf.get(i + 2) & 0xff, buf.get(i + 3) & 0xff)
  }

  // 8x8 Adam7-interlaced truecolor PNG; pixel (x,y) = RGB(x*32, y*32, 128).
  private val InterlacedPng =
    "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAAE8ahlKAAAAcElEQVR4nB2NWQEAQQhCjWIUoxDlRSGKUYiyzuqHIodVxd/Fq5piDw3soZtz67BzYJl9912qCxUucuoeNHjIyRoEhlxAL1q85DxHdTONTtO42SZ9hGgxQnpmixXREabNGPlF2qyJjwgdJijvkcOGhA/3oFgB+o5lEgAAAABJRU5ErkJggg=="

  // 16x16 progressive (SOF2) JPEG; four solid quadrants (red/green/blue/white).
  private val ProgressiveJpeg =
    "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAMCAgMCAgMDAwMEAwMEBQgFBQQEBQoHBwYIDAoMDAsKCwsNDhIQDQ4RDgsLEBYQERMUFRUVDA8XGBYUGBIUFRT/2wBDAQMEBAUEBQkFBQkUDQsNFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBT/wgARCAAQABADASIAAhEBAxEB/8QAFgABAQEAAAAAAAAAAAAAAAAABwgJ/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAH/2gAMAwEAAhADEAAAATljl7S1P//EABQQAQAAAAAAAAAAAAAAAAAAACD/2gAIAQEAAQUCH//EAB0RAAECBwAAAAAAAAAAAAAAABIREwAEBhYhYnL/2gAIAQMBAT8BrCZvRjDTRbKQ8ogx/8QAHREAAQIHAAAAAAAAAAAAAAAAEgYRABMVJDFCYv/aAAgBAgEBPwFTIanSbkiLVsN1H//EABQQAQAAAAAAAAAAAAAAAAAAACD/2gAIAQEABj8CH//EABQQAQAAAAAAAAAAAAAAAAAAACD/2gAIAQEAAT8hH//aAAwDAQACAAMAAAAQ1//EABkRAAEFAAAAAAAAAAAAAAAAABEhQZGhsf/aAAgBAwEBPxDf+ukCWC//xAAXEQEAAwAAAAAAAAAAAAAAAAARAIHR/9oACAECAQE/EMYK2rP/xAAUEAEAAAAAAAAAAAAAAAAAAAAg/9oACAEBAAE/EB//2Q=="

  test("interlaced (Adam7) PNG decodes to the correct pixels (ISS-784)") {
    val data = bytes(InterlacedPng)
    val res  = PngDecoderJs
      .decode(data, 0, data.length)
      .getOrElse(
        fail("interlaced PNG must decode on JS as it does on JVM/Native (ISS-784); got None")
      )
    assertEquals(res.width, 8)
    assertEquals(res.height, 8)
    assertEquals(res.format, 4)
    // Sample every Adam7 pass so a de-interlace ordering bug cannot slip through.
    val samples = List((0, 0), (7, 0), (0, 7), (7, 7), (4, 0), (0, 4), (2, 0), (0, 2), (1, 0), (0, 1), (3, 5), (5, 3))
    samples.foreach { case (x, y) =>
      assertEquals(px(res, x, y), (x * 32, y * 32, 128, 255), s"interlaced pixel @($x,$y)")
    }
  }

  test("progressive JPEG (SOF2) decodes to the correct pixels (ISS-784)") {
    val data = bytes(ProgressiveJpeg)
    val res  = JpegDecoderJs
      .decode(data, 0, data.length)
      .getOrElse(
        fail("progressive JPEG must decode on JS as it does on JVM/Native (ISS-784); got None")
      )
    assertEquals(res.width, 16)
    assertEquals(res.height, 16)
    // Ground truth: the SAME JPEG bytes decoded by javax.imageio.ImageIO at the
    // quadrant centers. Lossy, so a tolerance is used.
    val Tol       = 12
    val Reference = List(
      (4, 4, 253, 2, 0, "top-left red"),
      (12, 4, 1, 255, 1, "top-right green"),
      (4, 12, 0, 1, 252, "bottom-left blue"),
      (12, 12, 252, 254, 255, "bottom-right white")
    )
    Reference.foreach { case (x, y, r, g, b, name) =>
      val (ar, ag, ab, aa) = px(res, x, y)
      assert(scala.math.abs(ar - r) <= Tol, s"$name R: got $ar expected ~$r")
      assert(scala.math.abs(ag - g) <= Tol, s"$name G: got $ag expected ~$g")
      assert(scala.math.abs(ab - b) <= Tol, s"$name B: got $ab expected ~$b")
      assertEquals(aa, 255, s"$name A should be opaque")
    }
  }
}
