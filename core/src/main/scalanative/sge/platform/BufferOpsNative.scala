// SGE Native Ops — Buffer operations Scala Native bindings (delegates to Rust via C ABI)
//
// Binds to the sge_native_ops Rust library using @link/@extern annotations.
// Array data is passed to C functions via .at(offset) to obtain Ptr[Float]/Ptr[Byte].

package sge
package platform

import scala.scalanative.unsafe.*
import scala.scalanative.unsigned.*

@link("sge_native_ops")
@extern
private object BufferOpsC {
  def sge_copy_bytes(
    src:       Ptr[Byte],
    srcOffset: CInt,
    dst:       Ptr[Byte],
    dstOffset: CInt,
    numBytes:  CInt
  ): Unit = extern
  def sge_transform_v4m4(
    data:   Ptr[CFloat],
    stride: CInt,
    count:  CInt,
    matrix: Ptr[CFloat],
    offset: CInt
  ): Unit = extern
  def sge_transform_v3m4(
    data:   Ptr[CFloat],
    stride: CInt,
    count:  CInt,
    matrix: Ptr[CFloat],
    offset: CInt
  ): Unit = extern
  def sge_transform_v2m4(
    data:   Ptr[CFloat],
    stride: CInt,
    count:  CInt,
    matrix: Ptr[CFloat],
    offset: CInt
  ): Unit = extern
  def sge_transform_v3m3(
    data:   Ptr[CFloat],
    stride: CInt,
    count:  CInt,
    matrix: Ptr[CFloat],
    offset: CInt
  ): Unit = extern
  def sge_transform_v2m3(
    data:   Ptr[CFloat],
    stride: CInt,
    count:  CInt,
    matrix: Ptr[CFloat],
    offset: CInt
  ): Unit = extern
  def sge_find_vertex(
    vertex:   Ptr[CFloat],
    size:     CUnsignedInt,
    vertices: Ptr[CFloat],
    count:    CUnsignedInt
  ): CLong = extern
  def sge_find_vertex_epsilon(
    vertex:   Ptr[CFloat],
    size:     CUnsignedInt,
    vertices: Ptr[CFloat],
    count:    CUnsignedInt,
    epsilon:  CFloat
  ): CLong = extern
}

private[platform] object BufferOpsNative extends BufferOps {

  // ─── Memory management ──────────────────────────────────────────────────

  override def newDisposableByteBuffer(numBytes: Int): java.nio.ByteBuffer =
    java.nio.ByteBuffer.allocateDirect(numBytes)

  override def freeMemory(buffer: java.nio.ByteBuffer): Unit = () // GC-managed on Native

  override def getBufferAddress(buffer: java.nio.Buffer): Long =
    throw new UnsupportedOperationException("getBufferAddress is not available on Scala Native")

  // ─── Copy ──────────────────────────────────────────────────────────────

  override def copy(
    src:       Array[Byte],
    srcOffset: Int,
    dst:       Array[Byte],
    dstOffset: Int,
    numBytes:  Int
  ): Unit =
    BufferOpsC.sge_copy_bytes(src.at(0), srcOffset, dst.at(0), dstOffset, numBytes)

  override def copy(
    src:       Array[Float],
    srcOffset: Int,
    dst:       Array[Float],
    dstOffset: Int,
    numFloats: Int
  ): Unit =
    // Use System.arraycopy for float-to-float copy — the Rust library only exposes
    // byte-level copy, and System.arraycopy is already optimised on Scala Native.
    System.arraycopy(src, srcOffset, dst, dstOffset, numFloats)

  // ─── Transform (Vec × Matrix, applied to vertex arrays) ───────────────

  override def transformV4M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferOpsC.sge_transform_v4m4(data.at(0), strideInFloats, count, matrix.at(0), offsetInFloats)

  override def transformV3M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferOpsC.sge_transform_v3m4(data.at(0), strideInFloats, count, matrix.at(0), offsetInFloats)

  override def transformV2M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferOpsC.sge_transform_v2m4(data.at(0), strideInFloats, count, matrix.at(0), offsetInFloats)

  override def transformV3M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferOpsC.sge_transform_v3m3(data.at(0), strideInFloats, count, matrix.at(0), offsetInFloats)

  override def transformV2M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferOpsC.sge_transform_v2m3(data.at(0), strideInFloats, count, matrix.at(0), offsetInFloats)

  // ─── Find ──────────────────────────────────────────────────────────────

  override def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int
  ): Long =
    if (numVertices == 0) -1L
    else
      BufferOpsC
        .sge_find_vertex(
          vertex.at(vertexOffsetInFloats),
          strideInFloats.toUInt,
          vertices.at(verticesOffsetInFloats),
          numVertices.toUInt
        )
        .toLong

  override def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int,
    epsilon:                Float
  ): Long =
    if (numVertices == 0) -1L
    else
      BufferOpsC
        .sge_find_vertex_epsilon(
          vertex.at(vertexOffsetInFloats),
          strideInFloats.toUInt,
          vertices.at(verticesOffsetInFloats),
          numVertices.toUInt,
          epsilon
        )
        .toLong
}
