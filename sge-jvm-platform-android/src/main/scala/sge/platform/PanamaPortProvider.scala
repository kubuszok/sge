// SGE — PanamaPort FFM provider (Android)
//
// Implements PanamaProvider using com.v7878.foreign (PanamaPort).
// Used on Android where java.lang.foreign is not available.
// On Desktop JVM this class is never loaded (PanamaPort is a Provided dependency).

package sge
package platform

import java.lang.invoke.MethodHandle
import java.nio.ByteBuffer

object PanamaPortProvider extends PanamaProvider {

  // ─── Core types (aliases to com.v7878.foreign) ──────────────────────

  type MemorySegment      = com.v7878.foreign.MemorySegment
  type Arena              = com.v7878.foreign.Arena
  type Linker             = com.v7878.foreign.Linker
  type SymbolLookup       = com.v7878.foreign.SymbolLookup
  type FunctionDescriptor = com.v7878.foreign.FunctionDescriptor
  type MemoryLayout       = com.v7878.foreign.MemoryLayout

  // ─── Concrete ValueLayout aliases (used inside this object only) ────

  private val vByte:   com.v7878.foreign.ValueLayout.OfByte   = com.v7878.foreign.ValueLayout.JAVA_BYTE
  private val vShort:  com.v7878.foreign.ValueLayout.OfShort  = com.v7878.foreign.ValueLayout.JAVA_SHORT
  private val vInt:    com.v7878.foreign.ValueLayout.OfInt    = com.v7878.foreign.ValueLayout.JAVA_INT
  private val vFloat:  com.v7878.foreign.ValueLayout.OfFloat  = com.v7878.foreign.ValueLayout.JAVA_FLOAT
  private val vLong:   com.v7878.foreign.ValueLayout.OfLong   = com.v7878.foreign.ValueLayout.JAVA_LONG
  private val vDouble: com.v7878.foreign.ValueLayout.OfDouble = com.v7878.foreign.ValueLayout.JAVA_DOUBLE
  private val vAddr:   com.v7878.foreign.AddressLayout        = com.v7878.foreign.ValueLayout.ADDRESS

  // ─── MemorySegment companion ────────────────────────────────────────

  object MemorySegment extends MemorySegmentModule {
    def NULL: MemorySegment = com.v7878.foreign.MemorySegment.NULL

    def ofAddress(address: Long): MemorySegment =
      com.v7878.foreign.MemorySegment.ofAddress(address)

    def ofBuffer(buffer: java.nio.Buffer): MemorySegment =
      com.v7878.foreign.MemorySegment.ofBuffer(buffer)

    def copyFromBytes(src: Array[Byte], srcOff: Int, dst: MemorySegment, dstOff: Long, count: Int): Unit =
      com.v7878.foreign.MemorySegment.copy(src, srcOff, dst, vByte, dstOff, count)

    def copyToBytes(src: MemorySegment, srcOff: Long, dst: Array[Byte], dstOff: Int, count: Int): Unit =
      com.v7878.foreign.MemorySegment.copy(src, vByte, srcOff, dst, dstOff, count)

    def copyFromFloats(src: Array[Float], srcOff: Int, dst: MemorySegment, dstOff: Long, count: Int): Unit =
      com.v7878.foreign.MemorySegment.copy(src, srcOff, dst, vFloat, dstOff, count)

    def copyToFloats(src: MemorySegment, srcOff: Long, dst: Array[Float], dstOff: Int, count: Int): Unit =
      com.v7878.foreign.MemorySegment.copy(src, vFloat, srcOff, dst, dstOff, count)

    def copyFromInts(src: Array[Int], srcOff: Int, dst: MemorySegment, dstOff: Long, count: Int): Unit =
      com.v7878.foreign.MemorySegment.copy(src, srcOff, dst, vInt, dstOff, count)

    def copyToInts(src: MemorySegment, srcOff: Long, dst: Array[Int], dstOff: Int, count: Int): Unit =
      com.v7878.foreign.MemorySegment.copy(src, vInt, srcOff, dst, dstOff, count)

    def copyFromShorts(src: Array[Short], srcOff: Int, dst: MemorySegment, dstOff: Long, count: Int): Unit =
      com.v7878.foreign.MemorySegment.copy(src, srcOff, dst, vShort, dstOff, count)

    def copyToShorts(src: MemorySegment, srcOff: Long, dst: Array[Short], dstOff: Int, count: Int): Unit =
      com.v7878.foreign.MemorySegment.copy(src, vShort, srcOff, dst, dstOff, count)
  }

  // ─── MemorySegment extensions ───────────────────────────────────────

  extension (seg: MemorySegment) {
    def segAddress:                             Long          = seg.address()
    def segReinterpret(size: Long):             MemorySegment = seg.reinterpret(size)
    def segSlice(offset:     Long):             MemorySegment = seg.asSlice(offset)
    def segSlice(offset:     Long, size: Long): MemorySegment = seg.asSlice(offset, size)
    def segAsByteBuffer:                        ByteBuffer    = seg.asByteBuffer()
    def segCopyFrom(src:     MemorySegment):    Unit          = seg.copyFrom(src)

    def getInt(offset:     Long):                       Int           = seg.get(vInt, offset)
    def getLong(offset:    Long):                       Long          = seg.get(vLong, offset)
    def getFloat(offset:   Long):                       Float         = seg.get(vFloat, offset)
    def getDouble(offset:  Long):                       Double        = seg.get(vDouble, offset)
    def getAddress(offset: Long):                       MemorySegment = seg.get(vAddr, offset)
    def setInt(offset:     Long, value: Int):           Unit          = seg.set(vInt, offset, value)
    def setLong(offset:    Long, value: Long):          Unit          = seg.set(vLong, offset, value)
    def setFloat(offset:   Long, value: Float):         Unit          = seg.set(vFloat, offset, value)
    def setAddress(offset: Long, value: MemorySegment): Unit          = seg.set(vAddr, offset, value)

    def getIntAtIndex(index:     Long):             Int           = seg.getAtIndex(vInt, index)
    def setIntAtIndex(index:     Long, value: Int): Unit          = seg.setAtIndex(vInt, index, value)
    def getAddressAtIndex(index: Long):             MemorySegment = seg.getAtIndex(vAddr, index)

    def getString(offset: Long): String = seg.getString(offset)

    def isNull: Boolean = seg.eq(com.v7878.foreign.MemorySegment.NULL) || seg.address() == 0L
  }

  // ─── Arena companion ────────────────────────────────────────────────

  object Arena extends ArenaModule {
    def ofConfined(): Arena = com.v7878.foreign.Arena.ofConfined()
    def ofAuto():     Arena = com.v7878.foreign.Arena.ofAuto()
    def global():     Arena = com.v7878.foreign.Arena.global()
  }

  extension (arena: Arena) {
    def allocate(byteSize:     Long):                      MemorySegment = arena.allocate(byteSize)
    def allocateElems(layout:  MemoryLayout, count: Long): MemorySegment = arena.allocate(layout, count)
    def allocateLayout(layout: MemoryLayout):              MemorySegment = arena.allocate(layout)
    def allocateString(s:      String):                    MemorySegment = arena.allocateFrom(s)
    def arenaClose():                                      Unit          = arena.close()
  }

  // ─── Linker companion ──────────────────────────────────────────────

  object Linker extends LinkerModule {
    def nativeLinker(): Linker = com.v7878.foreign.Linker.nativeLinker()
  }

  extension (linker: Linker) {
    def downcallHandle(symbol: MemorySegment, desc: FunctionDescriptor): MethodHandle =
      linker.downcallHandle(symbol, desc)

    def upcallStub(target: MethodHandle, desc: FunctionDescriptor, arena: Arena): MemorySegment =
      linker.upcallStub(target, desc, arena)
  }

  // ─── SymbolLookup companion ────────────────────────────────────────

  object SymbolLookup extends SymbolLookupModule {
    def libraryLookup(path: java.nio.file.Path, arena: Arena): SymbolLookup =
      com.v7878.foreign.SymbolLookup.libraryLookup(path, arena)

    def loaderLookup(): SymbolLookup =
      com.v7878.foreign.SymbolLookup.loaderLookup()
  }

  extension (lookup: SymbolLookup) {
    def findSymbol(name: String): Option[MemorySegment] = {
      val opt = lookup.find(name)
      if (opt.isPresent) Some(opt.get()) else None
    }

    def findOrThrow(name: String): MemorySegment =
      lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"Symbol not found: $name"))
  }

  // ─── FunctionDescriptor companion ──────────────────────────────────

  object FunctionDescriptor extends FunctionDescriptorModule {
    def of(retLayout: MemoryLayout, argLayouts: MemoryLayout*): FunctionDescriptor =
      com.v7878.foreign.FunctionDescriptor.of(retLayout, argLayouts*)

    def ofVoid(argLayouts: MemoryLayout*): FunctionDescriptor =
      com.v7878.foreign.FunctionDescriptor.ofVoid(argLayouts*)
  }

  // ─── Value layout constants (upcast to MemoryLayout for provider API) ─

  def JAVA_INT:    MemoryLayout = vInt
  def JAVA_FLOAT:  MemoryLayout = vFloat
  def JAVA_BYTE:   MemoryLayout = vByte
  def JAVA_LONG:   MemoryLayout = vLong
  def JAVA_DOUBLE: MemoryLayout = vDouble
  def ADDRESS:     MemoryLayout = vAddr

  // ─── Layout utilities ──────────────────────────────────────────────

  def structLayout(elements: MemoryLayout*): MemoryLayout =
    com.v7878.foreign.MemoryLayout.structLayout(elements*)

  extension (layout: MemoryLayout) {
    def layoutByteSize: Long = layout.byteSize()
  }

  // ─── Size constants ────────────────────────────────────────────────

  def addressSize: Int = vAddr.byteSize().toInt
}
