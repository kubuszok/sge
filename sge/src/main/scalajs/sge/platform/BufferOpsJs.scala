// SGE Native Ops — Buffer operations pure Scala implementation for Scala.js
//
// Faithful port of the C++ JNI implementations from BufferUtils.java to pure Scala.
// Provides copy, transform (matrix x vertex array), and find operations.
// No native code — suitable for browser environments.
//
// Migration notes:
//   Origin: SGE-original (platform abstraction)
//   Convention: pure Scala fallback for JS — memory, copy, transform, find
//   Idiom: boundary/break (0 return), Nullable (0 null), split packages
//   Audited: 2026-03-03

package sge
package platform

import java.nio.Buffer
import java.nio.ByteBuffer

import scala.util.boundary
import scala.util.boundary.break

private[platform] object BufferOpsJs extends BufferOps {

  // ─── Memory management ──────────────────────────────────────────────────

  override def newDisposableByteBuffer(numBytes: Int): ByteBuffer =
    ByteBuffer.allocateDirect(numBytes)

  override def freeMemory(buffer: ByteBuffer): Unit = () // GC-managed on JS

  override def getBufferAddress(buffer: Buffer): Long =
    throw new UnsupportedOperationException("getBufferAddress is not available on Scala.js")

  // ─── Copy ──────────────────────────────────────────────────────────────

  override def copy(src: Array[Byte], srcOffset: Int, dst: Array[Byte], dstOffset: Int, numBytes: Int): Unit =
    System.arraycopy(src, srcOffset, dst, dstOffset, numBytes)

  override def copy(src: Array[Float], srcOffset: Int, dst: Array[Float], dstOffset: Int, numFloats: Int): Unit =
    System.arraycopy(src, srcOffset, dst, dstOffset, numFloats)

  // ─── Transform (Vec x Matrix, applied to vertex arrays) ───────────────
  //
  // All matrices are column-major (matching LibGDX convention):
  //   m[0], m[1], m[2], m[3]   = column 0
  //   m[4], m[5], m[6], m[7]   = column 1
  //   m[8], m[9], m[10], m[11] = column 2
  //   m[12], m[13], m[14], m[15] = column 3

  override def transformV4M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = {
    val m   = matrix
    var off = offsetInFloats
    var i   = 0
    while (i < count) {
      val x = data(off)
      val y = data(off + 1)
      val z = data(off + 2)
      val w = data(off + 3)
      data(off) = x * m(0) + y * m(4) + z * m(8) + w * m(12)
      data(off + 1) = x * m(1) + y * m(5) + z * m(9) + w * m(13)
      data(off + 2) = x * m(2) + y * m(6) + z * m(10) + w * m(14)
      data(off + 3) = x * m(3) + y * m(7) + z * m(11) + w * m(15)
      off += strideInFloats
      i += 1
    }
  }

  override def transformV3M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = {
    val m   = matrix
    var off = offsetInFloats
    var i   = 0
    while (i < count) {
      val x = data(off)
      val y = data(off + 1)
      val z = data(off + 2)
      data(off) = x * m(0) + y * m(4) + z * m(8) + m(12)
      data(off + 1) = x * m(1) + y * m(5) + z * m(9) + m(13)
      data(off + 2) = x * m(2) + y * m(6) + z * m(10) + m(14)
      off += strideInFloats
      i += 1
    }
  }

  override def transformV2M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = {
    val m   = matrix
    var off = offsetInFloats
    var i   = 0
    while (i < count) {
      val x = data(off)
      val y = data(off + 1)
      data(off) = x * m(0) + y * m(4) + m(12)
      data(off + 1) = x * m(1) + y * m(5) + m(13)
      off += strideInFloats
      i += 1
    }
  }

  override def transformV3M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = {
    val m   = matrix
    var off = offsetInFloats
    var i   = 0
    while (i < count) {
      val x = data(off)
      val y = data(off + 1)
      val z = data(off + 2)
      data(off) = x * m(0) + y * m(3) + z * m(6)
      data(off + 1) = x * m(1) + y * m(4) + z * m(7)
      data(off + 2) = x * m(2) + y * m(5) + z * m(8)
      off += strideInFloats
      i += 1
    }
  }

  override def transformV2M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit = {
    val m   = matrix
    var off = offsetInFloats
    var i   = 0
    while (i < count) {
      val x = data(off)
      val y = data(off + 1)
      data(off) = x * m(0) + y * m(3) + m(6)
      data(off + 1) = x * m(1) + y * m(4) + m(7)
      off += strideInFloats
      i += 1
    }
  }

  // ─── Find ──────────────────────────────────────────────────────────────

  /** Exact bitwise float comparison, matching the C++ implementation: Two floats are "different" only if both their raw bits AND their values differ. This means +0.0 == -0.0 (different bits but equal
    * values) and NaN == NaN (same bits).
    */
  private def compareExact(
    a:    Array[Float],
    aOff: Int,
    b:    Array[Float],
    bOff: Int,
    size: Int
  ): Boolean = boundary {
    var i = 0
    while (i < size) {
      if (
        java.lang.Float.floatToRawIntBits(a(aOff + i)) != java.lang.Float.floatToRawIntBits(b(bOff + i)) &&
        a(aOff + i) != b(bOff + i)
      ) {
        break(false)
      }
      i += 1
    }
    true
  }

  /** Epsilon-tolerance float comparison, matching the C++ implementation: Two floats are "different" only if both their raw bits differ AND their absolute difference exceeds epsilon.
    */
  private def compareEpsilon(
    a:       Array[Float],
    aOff:    Int,
    b:       Array[Float],
    bOff:    Int,
    size:    Int,
    epsilon: Float
  ): Boolean = boundary {
    var i = 0
    while (i < size) {
      val ai = a(aOff + i)
      val bi = b(bOff + i)
      if (
        java.lang.Float.floatToRawIntBits(ai) != java.lang.Float.floatToRawIntBits(bi) &&
        Math.abs(ai - bi) > epsilon
      ) {
        break(false)
      }
      i += 1
    }
    true
  }

  override def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int
  ): Long = boundary {
    val size = strideInFloats
    var i    = 0
    while (i < numVertices) {
      if (compareExact(vertex, vertexOffsetInFloats, vertices, verticesOffsetInFloats + i * size, size)) {
        break(i.toLong)
      }
      i += 1
    }
    -1L
  }

  override def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int,
    epsilon:                Float
  ): Long = boundary {
    val size = strideInFloats
    var i    = 0
    while (i < numVertices) {
      if (compareEpsilon(vertex, vertexOffsetInFloats, vertices, verticesOffsetInFloats + i * size, size, epsilon)) {
        break(i.toLong)
      }
      i += 1
    }
    -1L
  }
}
