// SGE — Integration test: Panama FFI with real Rust native library
//
// Tests the full FFI roundtrip: JVM -> Panama downcall -> Rust C ABI -> return.
// Requires libsge_native_ops on java.library.path (skipped otherwise).

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

  test("native library loads and sge_alloc_native_buffer symbol exists") {
    assert(lib.findSymbol("sge_alloc_native_buffer").isDefined, "sge_alloc_native_buffer")
  }

  test("sge_free_native_buffer symbol exists") {
    assert(lib.findSymbol("sge_free_native_buffer").isDefined, "sge_free_native_buffer")
  }

  test("sge_copy_jni_byte symbol exists") {
    assert(lib.findSymbol("sge_copy_jni_byte").isDefined, "sge_copy_jni_byte")
  }

  test("sge_copy_jni_float symbol exists") {
    assert(lib.findSymbol("sge_copy_jni_float").isDefined, "sge_copy_jni_float")
  }

  test("sge_transform_v2_m3 symbol exists") {
    assert(lib.findSymbol("sge_transform_v2_m3").isDefined, "sge_transform_v2_m3")
  }

  test("sge_transform_v3_m3 symbol exists") {
    assert(lib.findSymbol("sge_transform_v3_m3").isDefined, "sge_transform_v3_m3")
  }

  test("sge_transform_v2_m4 symbol exists") {
    assert(lib.findSymbol("sge_transform_v2_m4").isDefined, "sge_transform_v2_m4")
  }

  test("sge_transform_v3_m4 symbol exists") {
    assert(lib.findSymbol("sge_transform_v3_m4").isDefined, "sge_transform_v3_m4")
  }

  test("sge_transform_v4_m4 symbol exists") {
    assert(lib.findSymbol("sge_transform_v4_m4").isDefined, "sge_transform_v4_m4")
  }

  test("sge_find_exact symbol exists") {
    assert(lib.findSymbol("sge_find_exact").isDefined, "sge_find_exact")
  }

  test("sge_find_epsilon symbol exists") {
    assert(lib.findSymbol("sge_find_epsilon").isDefined, "sge_find_epsilon")
  }

  // ─── Alloc/free roundtrip via downcall ────────────────────────────────

  test("alloc and free native buffer via downcall") {
    import panama.*
    val linker = Linker.nativeLinker()

    // sge_alloc_native_buffer(size: long) -> long (pointer address)
    val allocSym = lib.findOrThrow("sge_alloc_native_buffer")
    val allocFd  = FunctionDescriptor.of(JAVA_LONG, JAVA_LONG)
    val allocMh  = linker.downcallHandle(allocSym, allocFd)

    // sge_free_native_buffer(ptr: long, size: long) -> void
    val freeSym = lib.findOrThrow("sge_free_native_buffer")
    val freeFd  = FunctionDescriptor.ofVoid(JAVA_LONG, JAVA_LONG)
    val freeMh  = linker.downcallHandle(freeSym, freeFd)

    val size = 1024L
    val ptr  = allocMh.invoke(size).asInstanceOf[Long]
    assert(ptr != 0L, s"Expected non-zero pointer, got $ptr")

    // Free should not throw
    freeMh.invoke(ptr, size)
  }
}
