// SGE Native Ops — ETC1 shared test suite
//
// Tests run on all platforms (JVM, JS, Native) using the platform-specific
// ETC1Ops implementation provided by PlatformOps.

package sge
package platform

class ETC1OpsSuite extends munit.FunSuite {

  val ops: ETC1Ops = PlatformOps.etc1

  // ─── Compressed data size ──────────────────────────────────────────────

  test("getCompressedDataSize for 4x4 (single block)") {
    // 4x4 -> 1 block = 8 bytes
    assertEquals(ops.getCompressedDataSize(4, 4), 8)
  }

  test("getCompressedDataSize for 8x8 (4 blocks)") {
    // 8x8 -> 4 blocks = 32 bytes
    assertEquals(ops.getCompressedDataSize(8, 8), 32)
  }

  test("getCompressedDataSize for non-power-of-2 rounds up") {
    // 5x5 rounds up to 8x8 = 4 blocks = 32 bytes
    assertEquals(ops.getCompressedDataSize(5, 5), 32)
  }

  test("getCompressedDataSize for 16x16") {
    // 16x16 -> 16 blocks = 128 bytes
    assertEquals(ops.getCompressedDataSize(16, 16), 128)
  }

  test("getCompressedDataSize for 1x1") {
    // 1x1 rounds up to 4x4 = 1 block = 8 bytes
    assertEquals(ops.getCompressedDataSize(1, 1), 8)
  }

  test("getCompressedDataSize formula: ((w+3)&~3) * ((h+3)&~3) / 2") {
    // Verify the formula for various sizes
    val cases = List((1, 1), (2, 3), (4, 4), (5, 7), (8, 8), (13, 17), (64, 64), (100, 200))
    cases.foreach { case (w, h) =>
      val expected = (((w + 3) & ~3) * ((h + 3) & ~3)) >> 1
      assertEquals(ops.getCompressedDataSize(w, h), expected, s"Failed for ${w}x${h}")
    }
  }

  // ─── PKM header ───────────────────────────────────────────────────────

  test("PKM header round-trip") {
    val header = new Array[Byte](16)
    ops.formatHeader(header, 0, 128, 64)
    assertEquals(ops.getWidthPKM(header, 0), 128)
    assertEquals(ops.getHeightPKM(header, 0), 64)
    assert(ops.isValidPKM(header, 0))
  }

  test("PKM header with offset") {
    val header = new Array[Byte](32)
    ops.formatHeader(header, 8, 256, 512)
    assertEquals(ops.getWidthPKM(header, 8), 256)
    assertEquals(ops.getHeightPKM(header, 8), 512)
    assert(ops.isValidPKM(header, 8))
  }

  test("PKM header invalid magic (all zeros)") {
    val header = new Array[Byte](16)
    assert(!ops.isValidPKM(header, 0))
  }

  test("PKM header various dimensions") {
    val sizes = List((1, 1), (4, 4), (7, 5), (100, 200), (1024, 768))
    sizes.foreach { case (w, h) =>
      val header = new Array[Byte](16)
      ops.formatHeader(header, 0, w, h)
      assertEquals(ops.getWidthPKM(header, 0), w, s"Width mismatch for ${w}x${h}")
      assertEquals(ops.getHeightPKM(header, 0), h, s"Height mismatch for ${w}x${h}")
      assert(ops.isValidPKM(header, 0), s"Invalid PKM for ${w}x${h}")
    }
  }

  // ─── Encode/decode round-trip ──────────────────────────────────────────

  test("encode/decode round-trip RGB888 4x4") {
    val width     = 4
    val height    = 4
    val pixelSize = 3
    val imageData = new Array[Byte](width * height * pixelSize)
    // Fill with a gradient pattern
    var y = 0
    while (y < height) {
      var x = 0
      while (x < width) {
        val idx = (y * width + x) * pixelSize
        imageData(idx) = (x * 64).toByte // R
        imageData(idx + 1) = (y * 64).toByte // G
        imageData(idx + 2) = 128.toByte // B
        x += 1
      }
      y += 1
    }

    val compressed = ops.encodeImage(imageData, 0, width, height, pixelSize)
    assertEquals(compressed.length, ops.getCompressedDataSize(width, height))

    val decoded = new Array[Byte](width * height * pixelSize)
    ops.decodeImage(compressed, 0, decoded, 0, width, height, pixelSize)

    // ETC1 is lossy — verify decoded data is non-zero and average error is reasonable
    assert(decoded.exists(_ != 0), "Decoded data should not be all zeros")
    var totalError = 0L
    var i          = 0
    while (i < imageData.length) {
      val orig = imageData(i) & 0xff
      val dec  = decoded(i) & 0xff
      totalError += Math.abs(orig - dec)
      i += 1
    }
    val avgError = totalError.toDouble / imageData.length
    assert(avgError < 40.0, s"Average per-byte error $avgError exceeds threshold 40")
  }

  test("encode/decode round-trip RGB888 8x8") {
    val width     = 8
    val height    = 8
    val pixelSize = 3
    val imageData = new Array[Byte](width * height * pixelSize)
    var i         = 0
    while (i < imageData.length) {
      imageData(i) = (i % 256).toByte
      i += 1
    }

    val compressed = ops.encodeImage(imageData, 0, width, height, pixelSize)
    val decoded    = new Array[Byte](width * height * pixelSize)
    ops.decodeImage(compressed, 0, decoded, 0, width, height, pixelSize)

    // Verify not all zeros (sanity check)
    assert(decoded.exists(_ != 0), "Decoded data should not be all zeros")
  }

  test("encodeImagePKM includes header") {
    val width     = 4
    val height    = 4
    val pixelSize = 3
    val imageData = new Array[Byte](width * height * pixelSize)
    var i         = 0
    while (i < imageData.length) {
      imageData(i) = (i * 5).toByte
      i += 1
    }

    val pkmData = ops.encodeImagePKM(imageData, 0, width, height, pixelSize)
    assertEquals(pkmData.length, 16 + ops.getCompressedDataSize(width, height))

    // First 16 bytes should be a valid PKM header
    assert(ops.isValidPKM(pkmData, 0))
    assertEquals(ops.getWidthPKM(pkmData, 0), width)
    assertEquals(ops.getHeightPKM(pkmData, 0), height)
  }

  test("encode/decode non-power-of-2 dimensions") {
    val width     = 7
    val height    = 5
    val pixelSize = 3
    val imageData = new Array[Byte](width * height * pixelSize)
    var i         = 0
    while (i < imageData.length) {
      imageData(i) = ((i * 37) % 256).toByte
      i += 1
    }

    val compressed = ops.encodeImage(imageData, 0, width, height, pixelSize)
    val decoded    = new Array[Byte](width * height * pixelSize)
    ops.decodeImage(compressed, 0, decoded, 0, width, height, pixelSize)

    assert(decoded.exists(_ != 0), "Decoded data should not be all zeros")
  }

  test("encode/decode uniform color block") {
    // Uniform blocks should encode/decode with very small error
    val width     = 4
    val height    = 4
    val pixelSize = 3
    val imageData = new Array[Byte](width * height * pixelSize)
    // Fill with uniform color (128, 64, 200)
    var i = 0
    while (i < width * height) {
      imageData(i * 3) = 128.toByte
      imageData(i * 3 + 1) = 64.toByte
      imageData(i * 3 + 2) = 200.toByte
      i += 1
    }

    val compressed = ops.encodeImage(imageData, 0, width, height, pixelSize)
    val decoded    = new Array[Byte](width * height * pixelSize)
    ops.decodeImage(compressed, 0, decoded, 0, width, height, pixelSize)

    // Uniform blocks should have minimal error
    var j = 0
    while (j < imageData.length) {
      val orig = imageData(j) & 0xff
      val dec  = decoded(j) & 0xff
      assert(
        Math.abs(orig - dec) <= 10,
        s"Uniform block byte $j: original=$orig, decoded=$dec, diff=${Math.abs(orig - dec)}"
      )
      j += 1
    }
  }

  test("compressed data size matches expected for encoded output") {
    val width      = 8
    val height     = 4
    val pixelSize  = 3
    val imageData  = new Array[Byte](width * height * pixelSize)
    val compressed = ops.encodeImage(imageData, 0, width, height, pixelSize)
    assertEquals(compressed.length, ops.getCompressedDataSize(width, height))
  }
}
