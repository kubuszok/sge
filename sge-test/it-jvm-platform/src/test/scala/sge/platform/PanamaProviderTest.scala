// SGE — Integration test: JVM platform provider discovery
//
// Verifies that JdkPanama is discovered on JDK 22+ and that the
// PanamaProvider interface works correctly with real java.lang.foreign.

package sge
package platform

import munit.FunSuite

class PanamaProviderTest extends FunSuite {

  test("JdkPanama is discoverable via reflection") {
    val cls = Class.forName("sge.platform.JdkPanama$")
    val obj = cls.getField("MODULE$").get(null).asInstanceOf[PanamaProvider]
    assert(obj != null)
  }

  test("JdkPanama provides Arena, Linker, SymbolLookup modules") {
    val panama = JdkPanama
    // These are companion objects (always non-null), verify they exist
    assert(panama.Arena != null)
    assert(panama.Linker != null)
    assert(panama.SymbolLookup != null)
    assert(panama.FunctionDescriptor != null)
    assert(panama.MemorySegment != null)
  }

  test("JdkPanama Arena can open and close a confined scope") {
    val panama = JdkPanama
    import panama.*
    val arena = Arena.ofConfined()
    assert(arena != null)
    arena.arenaClose()
  }

  test("JdkPanama Arena can allocate memory") {
    val panama = JdkPanama
    import panama.*
    val arena = Arena.ofConfined()
    val seg   = arena.allocate(64L)
    assert(!seg.isNull)
    assertEquals(seg.segAddress != 0L, true)
    arena.arenaClose()
  }

  test("JdkPanama MemorySegment read/write int") {
    val panama = JdkPanama
    import panama.*
    val arena = Arena.ofConfined()
    val seg   = arena.allocate(16L)
    seg.setInt(0L, 42)
    assertEquals(seg.getInt(0L), 42)
    seg.setInt(4L, -1)
    assertEquals(seg.getInt(4L), -1)
    arena.arenaClose()
  }

  test("JdkPanama MemorySegment read/write float") {
    val panama = JdkPanama
    import panama.*
    val arena = Arena.ofConfined()
    val seg   = arena.allocate(16L)
    seg.setFloat(0L, 3.14f)
    assertEqualsFloat(seg.getFloat(0L), 3.14f, 0.001f)
    arena.arenaClose()
  }

  test("JdkPanama Linker nativeLinker is available") {
    val panama = JdkPanama
    import panama.*
    val linker = Linker.nativeLinker()
    assert(linker != null)
  }

  test("JdkPanama layout constants have correct sizes") {
    val panama = JdkPanama
    import panama.*
    assertEquals(JAVA_INT.layoutByteSize, 4L)
    assertEquals(JAVA_FLOAT.layoutByteSize, 4L)
    assertEquals(JAVA_BYTE.layoutByteSize, 1L)
    assertEquals(JAVA_LONG.layoutByteSize, 8L)
    assertEquals(JAVA_DOUBLE.layoutByteSize, 8L)
    assert(ADDRESS.layoutByteSize == 4L || ADDRESS.layoutByteSize == 8L)
  }

  test("JdkPanama MemorySegment.NULL is null address") {
    val panama = JdkPanama
    import panama.*
    val nullSeg = MemorySegment.NULL
    assert(nullSeg.isNull)
  }
}
