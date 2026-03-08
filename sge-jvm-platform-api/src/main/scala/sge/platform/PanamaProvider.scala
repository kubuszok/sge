// SGE — Panama FFM abstraction layer
//
// Abstracts over java.lang.foreign (JDK 22+) and com.v7878.foreign (PanamaPort for Android).
// Desktop JVM uses JdkPanama; Android uses PanamaPort. The unused provider is never class-loaded.
//
// Pattern: path-dependent types — each provider defines concrete types for MemorySegment, Arena, etc.
// Code parameterized by PanamaProvider works identically on both platforms.

package sge
package platform

import java.lang.invoke.MethodHandle
import java.nio.ByteBuffer

/** Abstraction over Panama Foreign Function & Memory API.
  *
  * Two implementations exist:
  *   - `JdkPanama` — delegates to `java.lang.foreign` (JDK 22+, used on Desktop JVM)
  *   - `PanamaPortProvider` — delegates to `com.v7878.foreign` (PanamaPort, used on Android)
  *
  * Code that needs FFI should accept a `PanamaProvider` and use its path-dependent types.
  */
trait PanamaProvider {

  // ─── Core types ───────────────────────────────────────────────────────

  type MemorySegment
  type Arena
  type Linker
  type SymbolLookup
  type FunctionDescriptor
  type MemoryLayout

  // ─── MemorySegment companion ──────────────────────────────────────────

  val MemorySegment: MemorySegmentModule
  trait MemorySegmentModule { this: MemorySegment.type =>
    def NULL:                                MemorySegment
    def ofAddress(address: Long):            MemorySegment
    def ofBuffer(buffer:   java.nio.Buffer): MemorySegment

    // Array ↔ Segment copy (byte)
    def copyFromBytes(src: Array[Byte], srcOff:   Int, dst:  MemorySegment, dstOff: Long, count: Int): Unit
    def copyToBytes(src:   MemorySegment, srcOff: Long, dst: Array[Byte], dstOff:   Int, count:  Int): Unit

    // Array ↔ Segment copy (float)
    def copyFromFloats(src: Array[Float], srcOff:  Int, dst:  MemorySegment, dstOff: Long, count: Int): Unit
    def copyToFloats(src:   MemorySegment, srcOff: Long, dst: Array[Float], dstOff:  Int, count:  Int): Unit

    // Array ↔ Segment copy (int)
    def copyFromInts(src: Array[Int], srcOff:    Int, dst:  MemorySegment, dstOff: Long, count: Int): Unit
    def copyToInts(src:   MemorySegment, srcOff: Long, dst: Array[Int], dstOff:    Int, count:  Int): Unit

    // Array ↔ Segment copy (short)
    def copyFromShorts(src: Array[Short], srcOff:  Int, dst:  MemorySegment, dstOff: Long, count: Int): Unit
    def copyToShorts(src:   MemorySegment, srcOff: Long, dst: Array[Short], dstOff:  Int, count:  Int): Unit
  }

  // ─── MemorySegment extensions ─────────────────────────────────────────

  extension (seg: MemorySegment) {
    def segAddress:                             Long
    def segReinterpret(size: Long):             MemorySegment
    def segSlice(offset:     Long):             MemorySegment
    def segSlice(offset:     Long, size: Long): MemorySegment
    def segAsByteBuffer:                        ByteBuffer
    def segCopyFrom(src:     MemorySegment):    Unit

    // Typed get/set (by byte offset)
    def getInt(offset:     Long):                       Int
    def getLong(offset:    Long):                       Long
    def getFloat(offset:   Long):                       Float
    def getDouble(offset:  Long):                       Double
    def getAddress(offset: Long):                       MemorySegment
    def setInt(offset:     Long, value: Int):           Unit
    def setLong(offset:    Long, value: Long):          Unit
    def setFloat(offset:   Long, value: Float):         Unit
    def setAddress(offset: Long, value: MemorySegment): Unit

    // Indexed get/set (by element index, stride = element size)
    def getIntAtIndex(index:     Long):             Int
    def setIntAtIndex(index:     Long, value: Int): Unit
    def getAddressAtIndex(index: Long):             MemorySegment

    // C string
    def getString(offset: Long): String

    // Null check
    def isNull: Boolean
  }

  // ─── Arena companion ──────────────────────────────────────────────────

  val Arena: ArenaModule
  trait ArenaModule { this: Arena.type =>
    def ofConfined(): Arena
    def ofAuto():     Arena
    def global():     Arena
  }

  extension (arena: Arena) {
    def allocate(byteSize:     Long):                      MemorySegment
    def allocateElems(layout:  MemoryLayout, count: Long): MemorySegment
    def allocateLayout(layout: MemoryLayout):              MemorySegment
    def allocateString(s:      String):                    MemorySegment
    def arenaClose():                                      Unit
  }

  // ─── Linker companion ────────────────────────────────────────────────

  val Linker: LinkerModule
  trait LinkerModule { this: Linker.type =>
    def nativeLinker(): Linker
  }

  extension (linker: Linker) {
    def downcallHandle(symbol: MemorySegment, desc: FunctionDescriptor):               MethodHandle
    def upcallStub(target:     MethodHandle, desc:  FunctionDescriptor, arena: Arena): MemorySegment
  }

  // ─── SymbolLookup companion ──────────────────────────────────────────

  val SymbolLookup: SymbolLookupModule
  trait SymbolLookupModule { this: SymbolLookup.type =>
    def libraryLookup(path: java.nio.file.Path, arena: Arena): SymbolLookup
    def loaderLookup():                                        SymbolLookup
  }

  extension (lookup: SymbolLookup) {
    def findSymbol(name:  String): Option[MemorySegment]
    def findOrThrow(name: String): MemorySegment
  }

  // ─── FunctionDescriptor companion ────────────────────────────────────

  val FunctionDescriptor: FunctionDescriptorModule
  trait FunctionDescriptorModule { this: FunctionDescriptor.type =>
    def of(retLayout:      MemoryLayout, argLayouts: MemoryLayout*): FunctionDescriptor
    def ofVoid(argLayouts: MemoryLayout*):                           FunctionDescriptor
  }

  // ─── Value layout constants ──────────────────────────────────────────

  def JAVA_INT:    MemoryLayout
  def JAVA_FLOAT:  MemoryLayout
  def JAVA_BYTE:   MemoryLayout
  def JAVA_LONG:   MemoryLayout
  def JAVA_DOUBLE: MemoryLayout
  def ADDRESS:     MemoryLayout

  // ─── Layout utilities ────────────────────────────────────────────────

  def structLayout(elements: MemoryLayout*): MemoryLayout

  extension (layout: MemoryLayout) {
    def layoutByteSize: Long
  }

  // ─── Size constants (avoid depending on layout instances) ────────────

  def intSize:    Int = 4
  def floatSize:  Int = 4
  def longSize:   Int = 8
  def doubleSize: Int = 8

  /** Pointer size in bytes (8 on 64-bit). */
  def addressSize: Int
}
