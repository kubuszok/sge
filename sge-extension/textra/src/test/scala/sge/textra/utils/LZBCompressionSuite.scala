package sge
package textra
package utils

class LZBCompressionSuite extends munit.FunSuite {

  test("compress then decompress roundtrip equals original") {
    val original     = "Hello, World! This is a test of LZB compression."
    val compressed   = LZBCompression.compressToBytes(original)
    val decompressed = LZBDecompression.decompressFromBytes(compressed)
    assertEquals(decompressed, original)
  }

  test("empty string roundtrip") {
    val compressed = LZBCompression.compressToBytes("")
    assertEquals(compressed.length, 0)
  }

  test("repeated text compresses smaller than original") {
    val original   = "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc"
    val compressed = LZBCompression.compressToBytes(original)
    assert(
      compressed.length < original.length,
      s"compressed size (${compressed.length}) should be less than original (${original.length})"
    )
    val decompressed = LZBDecompression.decompressFromBytes(compressed)
    assertEquals(decompressed, original)
  }

  test("unicode text roundtrip") {
    val original     = "Scala 3 is great! \u00e9\u00e8\u00ea\u00eb \u00fc\u00f6\u00e4"
    val compressed   = LZBCompression.compressToBytes(original)
    val decompressed = LZBDecompression.decompressFromBytes(compressed)
    assertEquals(decompressed, original)
  }

  test("single character string roundtrip") {
    val original     = "a"
    val compressed   = LZBCompression.compressToBytes(original)
    val decompressed = LZBDecompression.decompressFromBytes(compressed)
    assertEquals(decompressed, original)
  }
}
