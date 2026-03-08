// SGE — PanamaPort FFM provider (Android)
//
// Implements PanamaProvider using com.v7878.foreign (PanamaPort).
// Used on Android where java.lang.foreign is not available.
// On Desktop JVM this class is never loaded (PanamaPort is a Provided dependency).
//
// This file is a compile-time stub. The real implementation will be filled in
// when PanamaPort is added as a dependency. For now it throws at construction
// so we can build and test the abstraction layer on Desktop.

package sge
package platform

import java.lang.invoke.MethodHandle
import java.nio.ByteBuffer

// TODO: Replace stub implementations with com.v7878.foreign delegation
//       once PanamaPort is added as a Provided dependency.
object PanamaPortProvider extends PanamaProvider {

  // On Android, these would alias to com.v7878.foreign types.
  // For now we use opaque wrappers so the file compiles without PanamaPort on classpath.
  type MemorySegment      = AnyRef
  type Arena              = AnyRef
  type Linker             = AnyRef
  type SymbolLookup       = AnyRef
  type FunctionDescriptor = AnyRef
  type MemoryLayout       = AnyRef

  private def stub: Nothing =
    throw new UnsupportedOperationException(
      "PanamaPortProvider is a stub — add com.v7878:panama-port-core dependency for Android"
    )

  // ─── MemorySegment companion ──────────────────────────────────────────

  object MemorySegment extends MemorySegmentModule {
    def NULL:                                                                                           MemorySegment = stub
    def ofAddress(address:  Long):                                                                      MemorySegment = stub
    def ofBuffer(buffer:    java.nio.Buffer):                                                           MemorySegment = stub
    def copyFromBytes(src:  Array[Byte], srcOff:   Int, dst:  MemorySegment, dstOff: Long, count: Int): Unit          = stub
    def copyToBytes(src:    MemorySegment, srcOff: Long, dst: Array[Byte], dstOff:   Int, count:  Int): Unit          = stub
    def copyFromFloats(src: Array[Float], srcOff:  Int, dst:  MemorySegment, dstOff: Long, count: Int): Unit          = stub
    def copyToFloats(src:   MemorySegment, srcOff: Long, dst: Array[Float], dstOff:  Int, count:  Int): Unit          = stub
    def copyFromInts(src:   Array[Int], srcOff:    Int, dst:  MemorySegment, dstOff: Long, count: Int): Unit          = stub
    def copyToInts(src:     MemorySegment, srcOff: Long, dst: Array[Int], dstOff:    Int, count:  Int): Unit          = stub
    def copyFromShorts(src: Array[Short], srcOff:  Int, dst:  MemorySegment, dstOff: Long, count: Int): Unit          = stub
    def copyToShorts(src:   MemorySegment, srcOff: Long, dst: Array[Short], dstOff:  Int, count:  Int): Unit          = stub
  }

  extension (seg: MemorySegment) {
    def segAddress:                                           Long          = stub
    def segReinterpret(size:     Long):                       MemorySegment = stub
    def segSlice(offset:         Long):                       MemorySegment = stub
    def segSlice(offset:         Long, size:  Long):          MemorySegment = stub
    def segAsByteBuffer:                                      ByteBuffer    = stub
    def segCopyFrom(src:         MemorySegment):              Unit          = stub
    def getInt(offset:           Long):                       Int           = stub
    def getLong(offset:          Long):                       Long          = stub
    def getFloat(offset:         Long):                       Float         = stub
    def getDouble(offset:        Long):                       Double        = stub
    def getAddress(offset:       Long):                       MemorySegment = stub
    def setInt(offset:           Long, value: Int):           Unit          = stub
    def setLong(offset:          Long, value: Long):          Unit          = stub
    def setFloat(offset:         Long, value: Float):         Unit          = stub
    def setAddress(offset:       Long, value: MemorySegment): Unit          = stub
    def getIntAtIndex(index:     Long):                       Int           = stub
    def setIntAtIndex(index:     Long, value: Int):           Unit          = stub
    def getAddressAtIndex(index: Long):                       MemorySegment = stub
    def getString(offset:        Long):                       String        = stub
    def isNull:                                               Boolean       = stub
  }

  // ─── Arena companion ──────────────────────────────────────────────────

  object Arena extends ArenaModule {
    def ofConfined(): Arena = stub
    def ofAuto():     Arena = stub
    def global():     Arena = stub
  }

  extension (arena: Arena) {
    def allocate(byteSize:     Long):                      MemorySegment = stub
    def allocateElems(layout:  MemoryLayout, count: Long): MemorySegment = stub
    def allocateLayout(layout: MemoryLayout):              MemorySegment = stub
    def allocateString(s:      String):                    MemorySegment = stub
    def arenaClose():                                      Unit          = stub
  }

  // ─── Linker companion ────────────────────────────────────────────────

  object Linker extends LinkerModule {
    def nativeLinker(): Linker = stub
  }

  extension (linker: Linker) {
    def downcallHandle(symbol: MemorySegment, desc: FunctionDescriptor):               MethodHandle  = stub
    def upcallStub(target:     MethodHandle, desc:  FunctionDescriptor, arena: Arena): MemorySegment = stub
  }

  // ─── SymbolLookup companion ──────────────────────────────────────────

  object SymbolLookup extends SymbolLookupModule {
    def libraryLookup(path: java.nio.file.Path, arena: Arena): SymbolLookup = stub
    def loaderLookup():                                        SymbolLookup = stub
  }

  extension (lookup: SymbolLookup) {
    def findSymbol(name:  String): Option[MemorySegment] = stub
    def findOrThrow(name: String): MemorySegment         = stub
  }

  // ─── FunctionDescriptor companion ────────────────────────────────────

  object FunctionDescriptor extends FunctionDescriptorModule {
    def of(retLayout:      MemoryLayout, argLayouts: MemoryLayout*): FunctionDescriptor = stub
    def ofVoid(argLayouts: MemoryLayout*):                           FunctionDescriptor = stub
  }

  // ─── Value layout constants ──────────────────────────────────────────

  def JAVA_INT:    MemoryLayout = stub
  def JAVA_FLOAT:  MemoryLayout = stub
  def JAVA_BYTE:   MemoryLayout = stub
  def JAVA_LONG:   MemoryLayout = stub
  def JAVA_DOUBLE: MemoryLayout = stub
  def ADDRESS:     MemoryLayout = stub

  // ─── Layout utilities ────────────────────────────────────────────────

  def structLayout(elements: MemoryLayout*): MemoryLayout = stub

  extension (layout: MemoryLayout) {
    def layoutByteSize: Long = stub
  }

  def addressSize: Int = 8 // Android is 64-bit
}
