// SGE — Integration test: Panama FFI with real Rust native library
//
// Tests the full FFI roundtrip: JVM -> Panama downcall -> Rust C ABI -> return.
// Requires libsge_native_ops on java.library.path (skipped otherwise).
//
// Symbol names match native-components/src/buffer_ops.rs C ABI:
//   sge_alloc_memory, sge_free_memory, sge_copy_bytes, sge_copy_floats,
//   sge_transform_v4m4, sge_transform_v3m4, sge_transform_v2m4,
//   sge_transform_v3m3, sge_transform_v2m3,
//   sge_find_vertex, sge_find_vertex_epsilon

package sge
package platform

import munit.FunSuite

class PanamaBufferOpsIntegrationTest extends FunSuite {
  import JdkPanama.*

  private def nativeLibAvailable: Boolean =
    try {
      val libName = System.mapLibraryName("sge_native_ops")
      val libPath = System.getProperty("java.library.path", "")
      val paths   = libPath.split(java.io.File.pathSeparator)
      paths.exists { dir =>
        java.nio.file.Files.exists(java.nio.file.Path.of(dir, libName))
      }
    } catch {
      case _: Exception => false
    }

  private lazy val panama = JdkPanama
  private lazy val lib: panama.SymbolLookup = {
    import panama.*
    val libName = System.mapLibraryName("sge_native_ops")
    val libPath = System.getProperty("java.library.path", "")
    val paths   = libPath.split(java.io.File.pathSeparator)
    val found   = paths.iterator.map(dir => java.nio.file.Path.of(dir, libName)).find(java.nio.file.Files.exists(_)).getOrElse(throw new UnsatisfiedLinkError(s"Cannot find $libName"))
    SymbolLookup.libraryLookup(found, Arena.global())
  }

  override def beforeAll(): Unit =
    assume(nativeLibAvailable, "sge_native_ops not on java.library.path — skipping")

  // ─── Symbol availability ──────────────────────────────────────────────

  test("native library loads and sge_alloc_memory symbol exists") {
    assert(lib.findSymbol("sge_alloc_memory").isDefined, "sge_alloc_memory")
  }

  test("sge_free_memory symbol exists") {
    assert(lib.findSymbol("sge_free_memory").isDefined, "sge_free_memory")
  }

  test("sge_copy_bytes symbol exists") {
    assert(lib.findSymbol("sge_copy_bytes").isDefined, "sge_copy_bytes")
  }

  test("sge_copy_floats symbol exists") {
    assert(lib.findSymbol("sge_copy_floats").isDefined, "sge_copy_floats")
  }

  test("sge_transform_v2m3 symbol exists") {
    assert(lib.findSymbol("sge_transform_v2m3").isDefined, "sge_transform_v2m3")
  }

  test("sge_transform_v3m3 symbol exists") {
    assert(lib.findSymbol("sge_transform_v3m3").isDefined, "sge_transform_v3m3")
  }

  test("sge_transform_v2m4 symbol exists") {
    assert(lib.findSymbol("sge_transform_v2m4").isDefined, "sge_transform_v2m4")
  }

  test("sge_transform_v3m4 symbol exists") {
    assert(lib.findSymbol("sge_transform_v3m4").isDefined, "sge_transform_v3m4")
  }

  test("sge_transform_v4m4 symbol exists") {
    assert(lib.findSymbol("sge_transform_v4m4").isDefined, "sge_transform_v4m4")
  }

  test("sge_find_vertex symbol exists") {
    assert(lib.findSymbol("sge_find_vertex").isDefined, "sge_find_vertex")
  }

  test("sge_find_vertex_epsilon symbol exists") {
    assert(lib.findSymbol("sge_find_vertex_epsilon").isDefined, "sge_find_vertex_epsilon")
  }

  // ─── Alloc/free roundtrip via downcall ────────────────────────────────

  test("alloc and free native memory via downcall") {
    import panama.*
    val linker = Linker.nativeLinker()

    // sge_alloc_memory(num_bytes: i32) -> *mut u8 (pointer)
    val allocSym = lib.findOrThrow("sge_alloc_memory")
    val allocFd  = FunctionDescriptor.of(ADDRESS, JAVA_INT)
    val allocMh  = linker.downcallHandle(allocSym, allocFd)

    // sge_free_memory(ptr: *mut u8) -> void
    val freeSym = lib.findOrThrow("sge_free_memory")
    val freeFd  = FunctionDescriptor.ofVoid(ADDRESS)
    val freeMh  = linker.downcallHandle(freeSym, freeFd)

    val size = 1024
    val ptr  = allocMh.invoke(size).asInstanceOf[java.lang.foreign.MemorySegment]
    assert(!ptr.equals(java.lang.foreign.MemorySegment.NULL), s"Expected non-null pointer")

    // Free should not throw
    freeMh.invoke(ptr)
  }

  // ─── Data correctness: sge_copy_bytes ─────────────────────────────────

  test("sge_copy_bytes copies known data correctly") {
    import panama.*
    val linker = Linker.nativeLinker()
    val arena  = Arena.ofConfined()

    try {
      val copySym = lib.findOrThrow("sge_copy_bytes")
      // sge_copy_bytes(src: *const u8, src_offset: i32, dst: *mut u8, dst_offset: i32, num_bytes: i32)
      val copyFd = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT)
      val copyMh = linker.downcallHandle(copySym, copyFd)

      // Allocate source and destination buffers
      val src = arena.allocate(8)
      val dst = arena.allocate(8)

      // Write known pattern to source: [0xDE, 0xAD, 0xBE, 0xEF, 0x01, 0x02, 0x03, 0x04]
      src.set(JAVA_BYTE, 0, 0xde.toByte)
      src.set(JAVA_BYTE, 1, 0xad.toByte)
      src.set(JAVA_BYTE, 2, 0xbe.toByte)
      src.set(JAVA_BYTE, 3, 0xef.toByte)
      src.set(JAVA_BYTE, 4, 0x01.toByte)
      src.set(JAVA_BYTE, 5, 0x02.toByte)
      src.set(JAVA_BYTE, 6, 0x03.toByte)
      src.set(JAVA_BYTE, 7, 0x04.toByte)

      // Copy 4 bytes from offset 2 in src to offset 1 in dst
      copyMh.invoke(src, 2: java.lang.Integer, dst, 1: java.lang.Integer, 4: java.lang.Integer)

      // Verify: dst[1..5] should contain [0xBE, 0xEF, 0x01, 0x02]
      assertEquals(dst.get(JAVA_BYTE, 1), 0xbe.toByte)
      assertEquals(dst.get(JAVA_BYTE, 2), 0xef.toByte)
      assertEquals(dst.get(JAVA_BYTE, 3), 0x01.toByte)
      assertEquals(dst.get(JAVA_BYTE, 4), 0x02.toByte)
    } finally
      arena.close()
  }

  // ─── Data correctness: sge_transform_v4m4 with identity matrix ────────

  test("sge_transform_v4m4 with identity matrix leaves vector unchanged") {
    import panama.*
    val linker = Linker.nativeLinker()
    val arena  = Arena.ofConfined()

    try {
      val sym = lib.findOrThrow("sge_transform_v4m4")
      // sge_transform_v4m4(data: *mut f32, stride: i32, count: i32, matrix: *const f32, offset: i32)
      val fd = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
      val mh = linker.downcallHandle(sym, fd)

      // Allocate data (1 vertex: 4 floats) and identity matrix (16 floats)
      val data   = arena.allocate(4 * 4L) // 4 floats
      val matrix = arena.allocate(16 * 4L) // 16 floats

      // Set vector: (1.0, 2.0, 3.0, 1.0)
      data.set(JAVA_FLOAT, 0, 1.0f)
      data.set(JAVA_FLOAT, 4, 2.0f)
      data.set(JAVA_FLOAT, 8, 3.0f)
      data.set(JAVA_FLOAT, 12, 1.0f)

      // Set identity matrix (column-major)
      for (i <- 0 until 16) matrix.set(JAVA_FLOAT, i * 4L, 0.0f)
      matrix.set(JAVA_FLOAT, 0 * 4L, 1.0f) // m[0]
      matrix.set(JAVA_FLOAT, 5 * 4L, 1.0f) // m[5]
      matrix.set(JAVA_FLOAT, 10 * 4L, 1.0f) // m[10]
      matrix.set(JAVA_FLOAT, 15 * 4L, 1.0f) // m[15]

      // Transform: stride=4, count=1, offset=0
      mh.invoke(data, 4: java.lang.Integer, 1: java.lang.Integer, matrix, 0: java.lang.Integer)

      // Verify vector unchanged
      assertEqualsFloat(data.get(JAVA_FLOAT, 0), 1.0f, 1e-6f)
      assertEqualsFloat(data.get(JAVA_FLOAT, 4), 2.0f, 1e-6f)
      assertEqualsFloat(data.get(JAVA_FLOAT, 8), 3.0f, 1e-6f)
      assertEqualsFloat(data.get(JAVA_FLOAT, 12), 1.0f, 1e-6f)
    } finally
      arena.close()
  }

  test("sge_transform_v4m4 with translation matrix translates correctly") {
    import panama.*
    val linker = Linker.nativeLinker()
    val arena  = Arena.ofConfined()

    try {
      val sym = lib.findOrThrow("sge_transform_v4m4")
      val fd  = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
      val mh  = linker.downcallHandle(sym, fd)

      val data   = arena.allocate(4 * 4L)
      val matrix = arena.allocate(16 * 4L)

      // Vector: (1.0, 2.0, 3.0, 1.0)
      data.set(JAVA_FLOAT, 0, 1.0f)
      data.set(JAVA_FLOAT, 4, 2.0f)
      data.set(JAVA_FLOAT, 8, 3.0f)
      data.set(JAVA_FLOAT, 12, 1.0f)

      // Translation matrix: translate by (10, 20, 30)
      // Column-major identity + translation in last column
      for (i <- 0 until 16) matrix.set(JAVA_FLOAT, i * 4L, 0.0f)
      matrix.set(JAVA_FLOAT, 0 * 4L, 1.0f)
      matrix.set(JAVA_FLOAT, 5 * 4L, 1.0f)
      matrix.set(JAVA_FLOAT, 10 * 4L, 1.0f)
      matrix.set(JAVA_FLOAT, 15 * 4L, 1.0f)
      matrix.set(JAVA_FLOAT, 12 * 4L, 10.0f) // tx
      matrix.set(JAVA_FLOAT, 13 * 4L, 20.0f) // ty
      matrix.set(JAVA_FLOAT, 14 * 4L, 30.0f) // tz

      mh.invoke(data, 4: java.lang.Integer, 1: java.lang.Integer, matrix, 0: java.lang.Integer)

      // Result: (1+10, 2+20, 3+30, 1) = (11, 22, 33, 1)
      assertEqualsFloat(data.get(JAVA_FLOAT, 0), 11.0f, 1e-6f)
      assertEqualsFloat(data.get(JAVA_FLOAT, 4), 22.0f, 1e-6f)
      assertEqualsFloat(data.get(JAVA_FLOAT, 8), 33.0f, 1e-6f)
      assertEqualsFloat(data.get(JAVA_FLOAT, 12), 1.0f, 1e-6f)
    } finally
      arena.close()
  }

  // ─── ETC1 encode/decode roundtrip ──────────────────────────────────────

  test("etc1_encode_block and etc1_decode_block symbols exist") {
    assert(lib.findSymbol("etc1_encode_block").isDefined, "etc1_encode_block")
    assert(lib.findSymbol("etc1_decode_block").isDefined, "etc1_decode_block")
  }

  test("ETC1 encode/decode block roundtrip within tolerance") {
    import panama.*
    val linker = Linker.nativeLinker()
    val arena  = Arena.ofConfined()

    try {
      // etc1_encode_block(p_in: *const u8, valid_pixel_mask: u32, p_out: *mut u8)
      val encodeSym = lib.findOrThrow("etc1_encode_block")
      val encodeFd  = FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS)
      val encodeMh  = linker.downcallHandle(encodeSym, encodeFd)

      // etc1_decode_block(p_in: *const u8, p_out: *mut u8)
      val decodeSym = lib.findOrThrow("etc1_decode_block")
      val decodeFd  = FunctionDescriptor.ofVoid(ADDRESS, ADDRESS)
      val decodeMh  = linker.downcallHandle(decodeSym, decodeFd)

      // ETC1 block: 4x4 pixels × 3 bytes (RGB) = 48 bytes decoded, 8 bytes encoded
      val decodedSize = 48
      val encodedSize = 8

      val input   = arena.allocate(decodedSize)
      val encoded = arena.allocate(encodedSize)
      val decoded = arena.allocate(decodedSize)

      // Fill input with a known RGB pattern: alternating red/blue rows
      for {
        row <- 0 until 4
        col <- 0 until 4
      } {
        val offset = (row * 4 + col) * 3
        if (row < 2) {
          // Red pixel
          input.set(JAVA_BYTE, offset.toLong, 200.toByte)
          input.set(JAVA_BYTE, (offset + 1).toLong, 50.toByte)
          input.set(JAVA_BYTE, (offset + 2).toLong, 50.toByte)
        } else {
          // Blue pixel
          input.set(JAVA_BYTE, offset.toLong, 50.toByte)
          input.set(JAVA_BYTE, (offset + 1).toLong, 50.toByte)
          input.set(JAVA_BYTE, (offset + 2).toLong, 200.toByte)
        }
      }

      // Encode: all 16 pixels valid (0xFFFF)
      encodeMh.invoke(input, 0xffff: java.lang.Integer, encoded)

      // Decode back
      decodeMh.invoke(encoded, decoded)

      // Verify: each decoded byte should be within ETC1 lossy tolerance (±16) of original
      val maxDiff = 16
      for (i <- 0 until decodedSize) {
        val orig = input.get(JAVA_BYTE, i.toLong) & 0xff
        val dec  = decoded.get(JAVA_BYTE, i.toLong) & 0xff
        val diff = Math.abs(orig - dec)
        assert(diff <= maxDiff, s"Byte $i: original=$orig, decoded=$dec, diff=$diff (max $maxDiff)")
      }
    } finally
      arena.close()
  }

  private def assertEqualsFloat(actual: Float, expected: Float, tolerance: Float)(implicit loc: munit.Location): Unit =
    assert(Math.abs(actual - expected) <= tolerance, s"expected $expected ± $tolerance, got $actual")
}
