// SGE Native Ops — Panama-based implementation of BufferOps
//
// Calls Rust C ABI functions using the PanamaProvider abstraction.
// Works on both Desktop JVM (JdkPanama) and Android (PanamaPortProvider).
//
// The same C ABI functions are also used by Scala Native via @extern.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: Panama FFM delegation to Rust C ABI native lib
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-05

package sge
package platform

import java.lang.invoke.MethodHandle
import java.nio.Buffer
import java.nio.ByteBuffer

private[platform] class BufferOpsPanama(val p: PanamaProvider) extends BufferOps {
  import p.*

  // ─── Native library + linker setup ─────────────────────────────────────

  private val linker: p.Linker = p.Linker.nativeLinker()

  private val lib: p.SymbolLookup = {
    val found = NativeLibLoader.load("sge_native_ops")
    p.SymbolLookup.libraryLookup(found, p.Arena.global())
  }

  private def lookup(name: String): p.MemorySegment =
    lib.findOrThrow(name)

  // ─── Method handle cache ──────────────────────────────────────────────

  // Memory management
  private val hAllocMemory: MethodHandle = linker.downcallHandle(
    lookup("sge_alloc_memory"),
    p.FunctionDescriptor.of(p.ADDRESS, p.JAVA_INT)
  )

  private val hFreeMemory: MethodHandle = linker.downcallHandle(
    lookup("sge_free_memory"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS)
  )

  // Copy operations
  private val hCopyBytes: MethodHandle = linker.downcallHandle(
    lookup("sge_copy_bytes"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS, p.JAVA_INT, p.ADDRESS, p.JAVA_INT, p.JAVA_INT)
  )

  private val hCopyFloats: MethodHandle = linker.downcallHandle(
    lookup("sge_copy_floats"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS, p.JAVA_INT, p.ADDRESS, p.JAVA_INT, p.JAVA_INT)
  )

  // Transform operations
  private val hTransformV4M4: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v4m4"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS, p.JAVA_INT, p.JAVA_INT, p.ADDRESS, p.JAVA_INT)
  )

  private val hTransformV3M4: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v3m4"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS, p.JAVA_INT, p.JAVA_INT, p.ADDRESS, p.JAVA_INT)
  )

  private val hTransformV2M4: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v2m4"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS, p.JAVA_INT, p.JAVA_INT, p.ADDRESS, p.JAVA_INT)
  )

  private val hTransformV3M3: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v3m3"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS, p.JAVA_INT, p.JAVA_INT, p.ADDRESS, p.JAVA_INT)
  )

  private val hTransformV2M3: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v2m3"),
    p.FunctionDescriptor.ofVoid(p.ADDRESS, p.JAVA_INT, p.JAVA_INT, p.ADDRESS, p.JAVA_INT)
  )

  // Find operations
  private val hFindVertex: MethodHandle = linker.downcallHandle(
    lookup("sge_find_vertex"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.ADDRESS, p.JAVA_INT, p.ADDRESS, p.JAVA_INT)
  )

  private val hFindVertexEpsilon: MethodHandle = linker.downcallHandle(
    lookup("sge_find_vertex_epsilon"),
    p.FunctionDescriptor.of(p.JAVA_LONG, p.ADDRESS, p.JAVA_INT, p.ADDRESS, p.JAVA_INT, p.JAVA_FLOAT)
  )

  // ─── Memory management ──────────────────────────────────────────────────

  override def newDisposableByteBuffer(numBytes: Int): ByteBuffer = {
    val ptr = hAllocMemory.invoke(numBytes).asInstanceOf[p.MemorySegment]
    if (ptr.isNull) throw new OutOfMemoryError(s"sge_alloc_memory($numBytes) returned null")
    // Reinterpret to the requested size so we can create a ByteBuffer
    val sized = ptr.segReinterpret(numBytes.toLong)
    sized.segAsByteBuffer.order(java.nio.ByteOrder.nativeOrder())
  }

  override def freeMemory(buffer: ByteBuffer): Unit =
    if (buffer.isDirect) {
      val seg = p.MemorySegment.ofBuffer(buffer)
      hFreeMemory.invoke(seg)
    }

  override def getBufferAddress(buffer: Buffer): Long = {
    val seg = p.MemorySegment.ofBuffer(buffer)
    seg.segAddress
  }

  // ─── Copy ──────────────────────────────────────────────────────────────

  override def copy(src: Array[Byte], srcOffset: Int, dst: Array[Byte], dstOffset: Int, numBytes: Int): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val srcSeg = arena.allocateElems(p.JAVA_BYTE, src.length.toLong)
      p.MemorySegment.copyFromBytes(src, 0, srcSeg, 0L, src.length)
      val dstSeg = arena.allocateElems(p.JAVA_BYTE, dst.length.toLong)
      p.MemorySegment.copyFromBytes(dst, 0, dstSeg, 0L, dst.length)
      hCopyBytes.invoke(srcSeg, srcOffset, dstSeg, dstOffset, numBytes)
      p.MemorySegment.copyToBytes(dstSeg, 0L, dst, 0, dst.length)
    } finally arena.arenaClose()
  }

  override def copy(
    src:       Array[Float],
    srcOffset: Int,
    dst:       Array[Float],
    dstOffset: Int,
    numFloats: Int
  ): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val srcSeg = arena.allocateElems(p.JAVA_FLOAT, src.length.toLong)
      p.MemorySegment.copyFromFloats(src, 0, srcSeg, 0L, src.length)
      val dstSeg = arena.allocateElems(p.JAVA_FLOAT, dst.length.toLong)
      p.MemorySegment.copyFromFloats(dst, 0, dstSeg, 0L, dst.length)
      hCopyFloats.invoke(srcSeg, srcOffset, dstSeg, dstOffset, numFloats)
      p.MemorySegment.copyToFloats(dstSeg, 0L, dst, 0, dst.length)
    } finally arena.arenaClose()
  }

  // ─── Transform (Vec x Matrix, applied to vertex arrays) ───────────────

  private def invokeTransform(
    handle:         MethodHandle,
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = {
    val arena = p.Arena.ofConfined()
    try {
      val dataSeg = arena.allocateElems(p.JAVA_FLOAT, data.length.toLong)
      p.MemorySegment.copyFromFloats(data, 0, dataSeg, 0L, data.length)
      val matSeg = arena.allocateElems(p.JAVA_FLOAT, matrix.length.toLong)
      p.MemorySegment.copyFromFloats(matrix, 0, matSeg, 0L, matrix.length)
      handle.invoke(dataSeg, strideInFloats, count, matSeg, offsetInFloats)
      p.MemorySegment.copyToFloats(dataSeg, 0L, data, 0, data.length)
    } finally arena.arenaClose()
  }

  override def transformV4M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = invokeTransform(hTransformV4M4, data, strideInFloats, count, matrix, offsetInFloats)

  override def transformV3M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = invokeTransform(hTransformV3M4, data, strideInFloats, count, matrix, offsetInFloats)

  override def transformV2M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = invokeTransform(hTransformV2M4, data, strideInFloats, count, matrix, offsetInFloats)

  override def transformV3M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = invokeTransform(hTransformV3M3, data, strideInFloats, count, matrix, offsetInFloats)

  override def transformV2M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = invokeTransform(hTransformV2M3, data, strideInFloats, count, matrix, offsetInFloats)

  // ─── Find ──────────────────────────────────────────────────────────────

  override def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int
  ): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val vSeg = arena.allocateElems(p.JAVA_FLOAT, vertex.length.toLong)
      p.MemorySegment.copyFromFloats(vertex, 0, vSeg, 0L, vertex.length)
      val vsSeg = arena.allocateElems(p.JAVA_FLOAT, vertices.length.toLong)
      p.MemorySegment.copyFromFloats(vertices, 0, vsSeg, 0L, vertices.length)
      // sge_find_vertex expects: (vertex_ptr, size_u32, vertices_ptr, count_u32)
      // We pass vertex starting at offset, size = strideInFloats
      val vertexAtOffset   = vSeg.segSlice(vertexOffsetInFloats.toLong * p.floatSize)
      val verticesAtOffset = vsSeg.segSlice(verticesOffsetInFloats.toLong * p.floatSize)
      hFindVertex.invoke(vertexAtOffset, strideInFloats, verticesAtOffset, numVertices).asInstanceOf[Long]
    } finally arena.arenaClose()
  }

  override def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int,
    epsilon:                Float
  ): Long = {
    val arena = p.Arena.ofConfined()
    try {
      val vSeg = arena.allocateElems(p.JAVA_FLOAT, vertex.length.toLong)
      p.MemorySegment.copyFromFloats(vertex, 0, vSeg, 0L, vertex.length)
      val vsSeg = arena.allocateElems(p.JAVA_FLOAT, vertices.length.toLong)
      p.MemorySegment.copyFromFloats(vertices, 0, vsSeg, 0L, vertices.length)
      val vertexAtOffset   = vSeg.segSlice(vertexOffsetInFloats.toLong * p.floatSize)
      val verticesAtOffset = vsSeg.segSlice(verticesOffsetInFloats.toLong * p.floatSize)
      hFindVertexEpsilon.invoke(vertexAtOffset, strideInFloats, verticesAtOffset, numVertices, epsilon).asInstanceOf[Long]
    } finally arena.arenaClose()
  }
}
