// SGE — Integration test: Panama FFI with real Rust native library
//
// Tests the full FFI roundtrip: JVM -> Panama downcall -> Rust C ABI -> return.
//
// The native library ships in the published provider JAR (pnm-provider-sge-desktop,
// resolved on the test classpath) and is loaded at runtime by
// multiarch.core.NativeLibLoader — the same classpath-extraction path the engine
// uses (see sge.platform.*Jvm). No java.library.path wiring (a local Rust build
// dir CI never creates, which made this suite assume-skip wholesale, ISS-697).
//
// CI hard-fail switch:
//   When SGE_CI_REQUIRE_PANAMA=1 every skip in this suite becomes a hard
//   assertion (FAIL, not skip), so the job can never silently degrade to a
//   vacuous green where every test assume-skips. See requireOrAssume.
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
  import multiarch.panama.JdkPanama.*

  // ── CI hard-fail switch ─────────────────────────────────────────────
  // SGE_CI_REQUIRE_PANAMA=1 (set in the CI job env) flips every skip in this
  // suite into a hard failure. A precondition the job is supposed to guarantee
  // (native libs resolvable from the provider JAR) must never silently skip on
  // CI — that is exactly the vacuous-green failure mode ISS-697 documents.
  // Locally the var is unset, so developers without the provider JAR resolved
  // still get the usual skip.
  private val requirePanama: Boolean =
    System.getenv("SGE_CI_REQUIRE_PANAMA") == "1"

  /** Like munit's `assume`, but hard-fails instead of skipping when SGE_CI_REQUIRE_PANAMA=1. Use everywhere a precondition would otherwise skip the test, so CI cannot return to zero executed
    * assertions.
    */
  private def requireOrAssume(cond: Boolean, clue: => String): Unit =
    if (requirePanama) assert(cond, s"SGE_CI_REQUIRE_PANAMA=1 but precondition failed: $clue")
    else assume(cond, clue)

  /** Resolve a native library via the multiarch-core loader (provider-JAR classpath extraction, the engine's real loading path). Returns Right with the extracted library Path, or Left with the
    * loader's diagnostic message when the library cannot be resolved on this platform.
    */
  private def loadNative(name: String): Either[String, java.nio.file.Path] =
    try Right(multiarch.core.NativeLibLoader.load(name))
    catch {
      case e: UnsatisfiedLinkError => Left(e.getMessage)
      case e: LinkageError         => Left(s"${e.getClass.getSimpleName}: ${e.getMessage}")
    }

  private lazy val panama = multiarch.panama.JdkPanama
  private lazy val lib: panama.SymbolLookup = {
    import panama.*
    // Resolve libsge_native_ops from the provider JAR on the classpath via
    // NativeLibLoader (host detection + native/<classifier>/<lib> extraction),
    // then open it with Panama's SymbolLookup. beforeEach has already gated
    // every test on this succeeding, so `.toOption.get` here is safe.
    val found = loadNative("sge_native_ops").toOption.get
    SymbolLookup.libraryLookup(found, Arena.global())
  }

  private def requireNative(): Unit = {
    val loaded = loadNative("sge_native_ops")
    requireOrAssume(loaded.isRight, s"libsge_native_ops not resolvable via provider JARs: ${loaded.left.toOption.getOrElse("")}")
  }

  override def beforeEach(context: BeforeEach): Unit =
    requireNative()

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
      src.setByte(0, 0xde.toByte)
      src.setByte(1, 0xad.toByte)
      src.setByte(2, 0xbe.toByte)
      src.setByte(3, 0xef.toByte)
      src.setByte(4, 0x01.toByte)
      src.setByte(5, 0x02.toByte)
      src.setByte(6, 0x03.toByte)
      src.setByte(7, 0x04.toByte)

      // Copy 4 bytes from offset 2 in src to offset 1 in dst
      copyMh.invoke(src, 2: java.lang.Integer, dst, 1: java.lang.Integer, 4: java.lang.Integer)

      // Verify: dst[1..5] should contain [0xBE, 0xEF, 0x01, 0x02]
      assertEquals(dst.getByte(1), 0xbe.toByte)
      assertEquals(dst.getByte(2), 0xef.toByte)
      assertEquals(dst.getByte(3), 0x01.toByte)
      assertEquals(dst.getByte(4), 0x02.toByte)
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
      data.setFloat(0, 1.0f)
      data.setFloat(4, 2.0f)
      data.setFloat(8, 3.0f)
      data.setFloat(12, 1.0f)

      // Set identity matrix (column-major)
      for (i <- 0 until 16) matrix.setFloat(i * 4L, 0.0f)
      matrix.setFloat(0 * 4L, 1.0f) // m[0]
      matrix.setFloat(5 * 4L, 1.0f) // m[5]
      matrix.setFloat(10 * 4L, 1.0f) // m[10]
      matrix.setFloat(15 * 4L, 1.0f) // m[15]

      // Transform: stride=4, count=1, offset=0
      mh.invoke(data, 4: java.lang.Integer, 1: java.lang.Integer, matrix, 0: java.lang.Integer)

      // Verify vector unchanged
      assertEqualsFloat(data.getFloat(0), 1.0f, 1e-6f)
      assertEqualsFloat(data.getFloat(4), 2.0f, 1e-6f)
      assertEqualsFloat(data.getFloat(8), 3.0f, 1e-6f)
      assertEqualsFloat(data.getFloat(12), 1.0f, 1e-6f)
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
      data.setFloat(0, 1.0f)
      data.setFloat(4, 2.0f)
      data.setFloat(8, 3.0f)
      data.setFloat(12, 1.0f)

      // Translation matrix: translate by (10, 20, 30)
      // Column-major identity + translation in last column
      for (i <- 0 until 16) matrix.setFloat(i * 4L, 0.0f)
      matrix.setFloat(0 * 4L, 1.0f)
      matrix.setFloat(5 * 4L, 1.0f)
      matrix.setFloat(10 * 4L, 1.0f)
      matrix.setFloat(15 * 4L, 1.0f)
      matrix.setFloat(12 * 4L, 10.0f) // tx
      matrix.setFloat(13 * 4L, 20.0f) // ty
      matrix.setFloat(14 * 4L, 30.0f) // tz

      mh.invoke(data, 4: java.lang.Integer, 1: java.lang.Integer, matrix, 0: java.lang.Integer)

      // Result: (1+10, 2+20, 3+30, 1) = (11, 22, 33, 1)
      assertEqualsFloat(data.getFloat(0), 11.0f, 1e-6f)
      assertEqualsFloat(data.getFloat(4), 22.0f, 1e-6f)
      assertEqualsFloat(data.getFloat(8), 33.0f, 1e-6f)
      assertEqualsFloat(data.getFloat(12), 1.0f, 1e-6f)
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
          input.setByte(offset.toLong, 200.toByte)
          input.setByte((offset + 1).toLong, 50.toByte)
          input.setByte((offset + 2).toLong, 50.toByte)
        } else {
          // Blue pixel
          input.setByte(offset.toLong, 50.toByte)
          input.setByte((offset + 1).toLong, 50.toByte)
          input.setByte((offset + 2).toLong, 200.toByte)
        }
      }

      // Encode: all 16 pixels valid (0xFFFF)
      encodeMh.invoke(input, 0xffff: java.lang.Integer, encoded)

      // Decode back
      decodeMh.invoke(encoded, decoded)

      // Verify: each decoded byte should be within ETC1 lossy tolerance (±16) of original
      val maxDiff = 16
      for (i <- 0 until decodedSize) {
        val orig = input.getByte(i.toLong) & 0xff
        val dec  = decoded.getByte(i.toLong) & 0xff
        val diff = Math.abs(orig - dec)
        assert(diff <= maxDiff, s"Byte $i: original=$orig, decoded=$dec, diff=$diff (max $maxDiff)")
      }
    } finally
      arena.close()
  }

  private def assertEqualsFloat(actual: Float, expected: Float, tolerance: Float)(implicit loc: munit.Location): Unit =
    assert(Math.abs(actual - expected) <= tolerance, s"expected $expected ± $tolerance, got $actual")
}
