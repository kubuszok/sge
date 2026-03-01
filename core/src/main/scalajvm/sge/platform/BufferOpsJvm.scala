// SGE Native Ops — JVM implementation of BufferOps via Rust JNI
//
// Delegates all methods to BufferOpsBridge, which loads libsge_native_ops via JNI.

package sge
package platform

import java.nio.Buffer
import java.nio.ByteBuffer

import BufferOpsBridge as BufferJni

private[platform] object BufferOpsJvm extends BufferOps {

  // ─── Memory management ──────────────────────────────────────────────────

  override def newDisposableByteBuffer(numBytes: Int): ByteBuffer =
    BufferJni.newDisposableByteBuffer(numBytes)

  override def freeMemory(buffer: ByteBuffer): Unit =
    BufferJni.freeMemory(buffer)

  override def getBufferAddress(buffer: Buffer): Long =
    BufferJni.getBufferAddress(buffer)

  // ─── Copy ──────────────────────────────────────────────────────────────

  override def copy(src: Array[Byte], srcOffset: Int, dst: Array[Byte], dstOffset: Int, numBytes: Int): Unit =
    BufferJni.copyBytes(src, srcOffset, dst, dstOffset, numBytes)

  override def copy(
    src:       Array[Float],
    srcOffset: Int,
    dst:       Array[Float],
    dstOffset: Int,
    numFloats: Int
  ): Unit =
    BufferJni.copyFloats(src, srcOffset, dst, dstOffset, numFloats)

  // ─── Transform (Vec x Matrix, applied to vertex arrays) ───────────────

  override def transformV4M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferJni.transformV4M4(data, strideInFloats, count, matrix, offsetInFloats)

  override def transformV3M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferJni.transformV3M4(data, strideInFloats, count, matrix, offsetInFloats)

  override def transformV2M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferJni.transformV2M4(data, strideInFloats, count, matrix, offsetInFloats)

  override def transformV3M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferJni.transformV3M3(data, strideInFloats, count, matrix, offsetInFloats)

  override def transformV2M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit =
    BufferJni.transformV2M3(data, strideInFloats, count, matrix, offsetInFloats)

  // ─── Find ──────────────────────────────────────────────────────────────

  override def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int
  ): Long =
    BufferJni.find(vertex, vertexOffsetInFloats, strideInFloats, vertices, verticesOffsetInFloats, numVertices)

  override def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int,
    epsilon:                Float
  ): Long =
    BufferJni.findEpsilon(
      vertex,
      vertexOffsetInFloats,
      strideInFloats,
      vertices,
      verticesOffsetInFloats,
      numVertices,
      epsilon
    )
}
