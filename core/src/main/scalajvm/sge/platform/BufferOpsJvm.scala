// SGE Native Ops — JVM implementation of BufferOps via Panama FFM
//
// Calls Rust C ABI functions directly using java.lang.foreign (Project Panama).
// Replaces the previous JNI approach (BufferOpsBridge.java + jni_bridge.rs).
//
// The same C ABI functions are also used by Scala Native via @extern.
// JNI bridge is retained only for Android (ART doesn't support Panama).
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: Panama FFM delegation to Rust C ABI native lib
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-05

package sge
package platform

import java.lang.foreign.*
import java.lang.foreign.ValueLayout.*
import java.lang.invoke.MethodHandle
import java.nio.Buffer
import java.nio.ByteBuffer

private[platform] object BufferOpsJvm extends BufferOps {

  // ─── Native library + linker setup ─────────────────────────────────────

  private val linker: Linker = Linker.nativeLinker()

  private val lib: SymbolLookup = {
    val libName = System.mapLibraryName("sge_native_ops")
    val libPath = System.getProperty("java.library.path", "")
    val paths   = libPath.split(java.io.File.pathSeparator)
    val found   = paths.iterator
      .map(p => java.nio.file.Path.of(p, libName))
      .find(java.nio.file.Files.exists(_))
      .getOrElse(
        throw new UnsatisfiedLinkError(
          s"Cannot find $libName in java.library.path: $libPath"
        )
      )
    SymbolLookup.libraryLookup(found, Arena.global())
  }

  private def lookup(name: String): MemorySegment =
    lib.find(name).orElseThrow(() => new UnsatisfiedLinkError(s"Symbol not found: $name"))

  // ─── Method handle cache ──────────────────────────────────────────────

  // Memory management
  private val hAllocMemory: MethodHandle = linker.downcallHandle(
    lookup("sge_alloc_memory"),
    FunctionDescriptor.of(ADDRESS, JAVA_INT)
  )

  private val hFreeMemory: MethodHandle = linker.downcallHandle(
    lookup("sge_free_memory"),
    FunctionDescriptor.ofVoid(ADDRESS)
  )

  // Copy operations
  private val hCopyBytes: MethodHandle = linker.downcallHandle(
    lookup("sge_copy_bytes"),
    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT)
  )

  private val hCopyFloats: MethodHandle = linker.downcallHandle(
    lookup("sge_copy_floats"),
    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT)
  )

  // Transform operations
  private val hTransformV4M4: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v4m4"),
    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
  )

  private val hTransformV3M4: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v3m4"),
    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
  )

  private val hTransformV2M4: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v2m4"),
    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
  )

  private val hTransformV3M3: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v3m3"),
    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
  )

  private val hTransformV2M3: MethodHandle = linker.downcallHandle(
    lookup("sge_transform_v2m3"),
    FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
  )

  // Find operations
  private val hFindVertex: MethodHandle = linker.downcallHandle(
    lookup("sge_find_vertex"),
    FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT)
  )

  private val hFindVertexEpsilon: MethodHandle = linker.downcallHandle(
    lookup("sge_find_vertex_epsilon"),
    FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT, JAVA_FLOAT)
  )

  // ─── Memory management ──────────────────────────────────────────────────

  override def newDisposableByteBuffer(numBytes: Int): ByteBuffer = {
    val ptr = hAllocMemory.invoke(numBytes).asInstanceOf[MemorySegment]
    if (ptr == MemorySegment.NULL) throw new OutOfMemoryError(s"sge_alloc_memory($numBytes) returned null")
    // Reinterpret to the requested size so we can create a ByteBuffer
    val sized = ptr.reinterpret(numBytes.toLong)
    sized.asByteBuffer().order(java.nio.ByteOrder.nativeOrder())
  }

  override def freeMemory(buffer: ByteBuffer): Unit =
    if (buffer.isDirect) {
      val seg = MemorySegment.ofBuffer(buffer)
      hFreeMemory.invoke(seg)
    }

  override def getBufferAddress(buffer: Buffer): Long = {
    val seg = MemorySegment.ofBuffer(buffer)
    seg.address()
  }

  // ─── Copy ──────────────────────────────────────────────────────────────

  override def copy(src: Array[Byte], srcOffset: Int, dst: Array[Byte], dstOffset: Int, numBytes: Int): Unit = {
    val arena = Arena.ofConfined()
    try {
      val srcSeg = arena.allocate(JAVA_BYTE, src.length.toLong)
      MemorySegment.copy(src, 0, srcSeg, JAVA_BYTE, 0L, src.length)
      val dstSeg = arena.allocate(JAVA_BYTE, dst.length.toLong)
      MemorySegment.copy(dst, 0, dstSeg, JAVA_BYTE, 0L, dst.length)
      hCopyBytes.invoke(srcSeg, srcOffset, dstSeg, dstOffset, numBytes)
      MemorySegment.copy(dstSeg, JAVA_BYTE, 0L, dst, 0, dst.length)
    } finally arena.close()
  }

  override def copy(
    src:       Array[Float],
    srcOffset: Int,
    dst:       Array[Float],
    dstOffset: Int,
    numFloats: Int
  ): Unit = {
    val arena = Arena.ofConfined()
    try {
      val srcSeg = arena.allocate(JAVA_FLOAT, src.length.toLong)
      MemorySegment.copy(src, 0, srcSeg, JAVA_FLOAT, 0L, src.length)
      val dstSeg = arena.allocate(JAVA_FLOAT, dst.length.toLong)
      MemorySegment.copy(dst, 0, dstSeg, JAVA_FLOAT, 0L, dst.length)
      hCopyFloats.invoke(srcSeg, srcOffset, dstSeg, dstOffset, numFloats)
      MemorySegment.copy(dstSeg, JAVA_FLOAT, 0L, dst, 0, dst.length)
    } finally arena.close()
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
    val arena = Arena.ofConfined()
    try {
      val dataSeg = arena.allocate(JAVA_FLOAT, data.length.toLong)
      MemorySegment.copy(data, 0, dataSeg, JAVA_FLOAT, 0L, data.length)
      val matSeg = arena.allocate(JAVA_FLOAT, matrix.length.toLong)
      MemorySegment.copy(matrix, 0, matSeg, JAVA_FLOAT, 0L, matrix.length)
      handle.invoke(dataSeg, strideInFloats, count, matSeg, offsetInFloats)
      MemorySegment.copy(dataSeg, JAVA_FLOAT, 0L, data, 0, data.length)
    } finally arena.close()
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
    val arena = Arena.ofConfined()
    try {
      val vSeg = arena.allocate(JAVA_FLOAT, vertex.length.toLong)
      MemorySegment.copy(vertex, 0, vSeg, JAVA_FLOAT, 0L, vertex.length)
      val vsSeg = arena.allocate(JAVA_FLOAT, vertices.length.toLong)
      MemorySegment.copy(vertices, 0, vsSeg, JAVA_FLOAT, 0L, vertices.length)
      // sge_find_vertex expects: (vertex_ptr, size_u32, vertices_ptr, count_u32)
      // We pass vertex starting at offset, size = strideInFloats
      val vertexAtOffset   = vSeg.asSlice(vertexOffsetInFloats.toLong * JAVA_FLOAT.byteSize())
      val verticesAtOffset = vsSeg.asSlice(verticesOffsetInFloats.toLong * JAVA_FLOAT.byteSize())
      hFindVertex.invoke(vertexAtOffset, strideInFloats, verticesAtOffset, numVertices).asInstanceOf[Long]
    } finally arena.close()
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
    val arena = Arena.ofConfined()
    try {
      val vSeg = arena.allocate(JAVA_FLOAT, vertex.length.toLong)
      MemorySegment.copy(vertex, 0, vSeg, JAVA_FLOAT, 0L, vertex.length)
      val vsSeg = arena.allocate(JAVA_FLOAT, vertices.length.toLong)
      MemorySegment.copy(vertices, 0, vsSeg, JAVA_FLOAT, 0L, vertices.length)
      val vertexAtOffset   = vSeg.asSlice(vertexOffsetInFloats.toLong * JAVA_FLOAT.byteSize())
      val verticesAtOffset = vsSeg.asSlice(verticesOffsetInFloats.toLong * JAVA_FLOAT.byteSize())
      hFindVertexEpsilon.invoke(vertexAtOffset, strideInFloats, verticesAtOffset, numVertices, epsilon).asInstanceOf[Long]
    } finally arena.close()
  }
}
