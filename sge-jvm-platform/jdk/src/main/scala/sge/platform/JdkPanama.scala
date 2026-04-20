// SGE — JDK Panama FFM provider
//
// Implements PanamaProvider using java.lang.foreign (JDK 22+).
// Used on Desktop JVM (JDK 25). Never loaded on Android.

package sge
package platform

import java.lang.invoke.MethodHandle
import java.nio.ByteBuffer

object JdkPanama extends PanamaProvider {

  // ─── Core types (aliases to java.lang.foreign) ────────────────────────

  type MemorySegment      = java.lang.foreign.MemorySegment
  type Arena              = java.lang.foreign.Arena
  type Linker             = java.lang.foreign.Linker
  type SymbolLookup       = java.lang.foreign.SymbolLookup
  type FunctionDescriptor = java.lang.foreign.FunctionDescriptor
  type MemoryLayout       = java.lang.foreign.MemoryLayout

  // ─── Concrete ValueLayout aliases (used inside this object only) ──────

  private val vByte:   java.lang.foreign.ValueLayout.OfByte   = java.lang.foreign.ValueLayout.JAVA_BYTE
  private val vShort:  java.lang.foreign.ValueLayout.OfShort  = java.lang.foreign.ValueLayout.JAVA_SHORT
  private val vInt:    java.lang.foreign.ValueLayout.OfInt    = java.lang.foreign.ValueLayout.JAVA_INT
  private val vFloat:  java.lang.foreign.ValueLayout.OfFloat  = java.lang.foreign.ValueLayout.JAVA_FLOAT
  private val vLong:   java.lang.foreign.ValueLayout.OfLong   = java.lang.foreign.ValueLayout.JAVA_LONG
  private val vDouble: java.lang.foreign.ValueLayout.OfDouble = java.lang.foreign.ValueLayout.JAVA_DOUBLE
  private val vAddr:   java.lang.foreign.AddressLayout        = java.lang.foreign.ValueLayout.ADDRESS

  // ─── MemorySegment companion ──────────────────────────────────────────

  object MemorySegment extends MemorySegmentModule {
    def NULL: MemorySegment = java.lang.foreign.MemorySegment.NULL

    def ofAddress(address: Long): MemorySegment =
      java.lang.foreign.MemorySegment.ofAddress(address)

    def ofBuffer(buffer: java.nio.Buffer): MemorySegment =
      java.lang.foreign.MemorySegment.ofBuffer(buffer)

    def copyFromBytes(src: Array[Byte], srcOff: Int, dst: MemorySegment, dstOff: Long, count: Int): Unit =
      java.lang.foreign.MemorySegment.copy(src, srcOff, dst, vByte, dstOff, count)

    def copyToBytes(src: MemorySegment, srcOff: Long, dst: Array[Byte], dstOff: Int, count: Int): Unit =
      java.lang.foreign.MemorySegment.copy(src, vByte, srcOff, dst, dstOff, count)

    def copyFromFloats(src: Array[Float], srcOff: Int, dst: MemorySegment, dstOff: Long, count: Int): Unit =
      java.lang.foreign.MemorySegment.copy(src, srcOff, dst, vFloat, dstOff, count)

    def copyToFloats(src: MemorySegment, srcOff: Long, dst: Array[Float], dstOff: Int, count: Int): Unit =
      java.lang.foreign.MemorySegment.copy(src, vFloat, srcOff, dst, dstOff, count)

    def copyFromInts(src: Array[Int], srcOff: Int, dst: MemorySegment, dstOff: Long, count: Int): Unit =
      java.lang.foreign.MemorySegment.copy(src, srcOff, dst, vInt, dstOff, count)

    def copyToInts(src: MemorySegment, srcOff: Long, dst: Array[Int], dstOff: Int, count: Int): Unit =
      java.lang.foreign.MemorySegment.copy(src, vInt, srcOff, dst, dstOff, count)

    def copyFromShorts(src: Array[Short], srcOff: Int, dst: MemorySegment, dstOff: Long, count: Int): Unit =
      java.lang.foreign.MemorySegment.copy(src, srcOff, dst, vShort, dstOff, count)

    def copyToShorts(src: MemorySegment, srcOff: Long, dst: Array[Short], dstOff: Int, count: Int): Unit =
      java.lang.foreign.MemorySegment.copy(src, vShort, srcOff, dst, dstOff, count)
  }

  // ─── MemorySegment extensions ─────────────────────────────────────────

  extension (seg: MemorySegment) {
    def segAddress:                             Long          = seg.address()
    def segReinterpret(size: Long):             MemorySegment = seg.reinterpret(size)
    def segSlice(offset:     Long):             MemorySegment = seg.asSlice(offset)
    def segSlice(offset:     Long, size: Long): MemorySegment = seg.asSlice(offset, size)
    def segAsByteBuffer:                        ByteBuffer    = seg.asByteBuffer()
    def segCopyFrom(src:     MemorySegment):    Unit          = seg.copyFrom(src)

    def getByte(offset:    Long):                       Byte          = seg.get(vByte, offset)
    def getInt(offset:     Long):                       Int           = seg.get(vInt, offset)
    def getLong(offset:    Long):                       Long          = seg.get(vLong, offset)
    def getFloat(offset:   Long):                       Float         = seg.get(vFloat, offset)
    def getDouble(offset:  Long):                       Double        = seg.get(vDouble, offset)
    def getAddress(offset: Long):                       MemorySegment = seg.get(vAddr, offset)
    def setByte(offset:    Long, value: Byte):          Unit          = seg.set(vByte, offset, value)
    def setInt(offset:     Long, value: Int):           Unit          = seg.set(vInt, offset, value)
    def setLong(offset:    Long, value: Long):          Unit          = seg.set(vLong, offset, value)
    def setFloat(offset:   Long, value: Float):         Unit          = seg.set(vFloat, offset, value)
    def setAddress(offset: Long, value: MemorySegment): Unit          = seg.set(vAddr, offset, value)

    def getIntAtIndex(index:     Long):             Int           = seg.getAtIndex(vInt, index)
    def setIntAtIndex(index:     Long, value: Int): Unit          = seg.setAtIndex(vInt, index, value)
    def getAddressAtIndex(index: Long):             MemorySegment = seg.getAtIndex(vAddr, index)

    def getString(offset: Long): String = seg.getString(offset)

    def isNull: Boolean = (seg eq java.lang.foreign.MemorySegment.NULL) || seg.address() == 0L
  }

  // ─── Arena companion ──────────────────────────────────────────────────

  object Arena extends ArenaModule {
    def ofConfined(): Arena = java.lang.foreign.Arena.ofConfined()
    def ofAuto():     Arena = java.lang.foreign.Arena.ofAuto()
    def global():     Arena = java.lang.foreign.Arena.global()
  }

  extension (arena: Arena) {
    def allocate(byteSize:     Long):                      MemorySegment = arena.allocate(byteSize)
    def allocateElems(layout:  MemoryLayout, count: Long): MemorySegment = arena.allocate(layout, count)
    def allocateLayout(layout: MemoryLayout):              MemorySegment = arena.allocate(layout)
    def allocateString(s:      String):                    MemorySegment = arena.allocateFrom(s)
    def arenaClose():                                      Unit          = arena.close()
  }

  // ─── Linker companion ────────────────────────────────────────────────

  object Linker extends LinkerModule {
    def nativeLinker(): Linker = java.lang.foreign.Linker.nativeLinker()
  }

  extension (linker: Linker) {
    def downcallHandle(symbol: MemorySegment, desc: FunctionDescriptor): MethodHandle =
      linker.downcallHandle(symbol, desc)

    def upcallStub(target: MethodHandle, desc: FunctionDescriptor, arena: Arena): MemorySegment =
      linker.upcallStub(target, desc, arena)
  }

  // ─── SymbolLookup companion ──────────────────────────────────────────

  object SymbolLookup extends SymbolLookupModule {
    def libraryLookup(path: java.nio.file.Path, arena: Arena): SymbolLookup =
      java.lang.foreign.SymbolLookup.libraryLookup(path, arena)

    def loaderLookup(): SymbolLookup =
      java.lang.foreign.SymbolLookup.loaderLookup()
  }

  extension (lookup: SymbolLookup) {
    def findSymbol(name: String): Option[MemorySegment] = {
      val opt = lookup.find(name)
      if (opt.isPresent) Some(opt.get()) else None
    }

    def findOrThrow(name: String): MemorySegment =
      lookup.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"Symbol not found: $name"))
  }

  // ─── FunctionDescriptor companion ────────────────────────────────────

  object FunctionDescriptor extends FunctionDescriptorModule {
    def of(retLayout: MemoryLayout, argLayouts: MemoryLayout*): FunctionDescriptor =
      java.lang.foreign.FunctionDescriptor.of(retLayout, argLayouts*)

    def ofVoid(argLayouts: MemoryLayout*): FunctionDescriptor =
      java.lang.foreign.FunctionDescriptor.ofVoid(argLayouts*)
  }

  // ─── Value layout constants (upcast to MemoryLayout for provider API) ─

  def JAVA_INT:    MemoryLayout = vInt
  def JAVA_FLOAT:  MemoryLayout = vFloat
  def JAVA_BYTE:   MemoryLayout = vByte
  def JAVA_LONG:   MemoryLayout = vLong
  def JAVA_DOUBLE: MemoryLayout = vDouble
  def ADDRESS:     MemoryLayout = vAddr

  // ─── Layout utilities ────────────────────────────────────────────────

  def structLayout(elements: MemoryLayout*): MemoryLayout =
    java.lang.foreign.MemoryLayout.structLayout(elements*)

  extension (layout: MemoryLayout) {
    def layoutByteSize: Long = layout.byteSize()
  }

  // ─── Size constants ──────────────────────────────────────────────────

  def addressSize: Int = vAddr.byteSize().toInt
}
