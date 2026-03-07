// SGE Native Ops — Buffer operations API (memory, copy, transform, find)
//
// Platform implementations:
//   JVM:    BufferOpsJvm    (delegates to Rust via JNI)
//   JS:     BufferOpsJs     (pure Scala fallback)
//   Native: BufferOpsNative (delegates to Rust via C ABI)

/*
 * Migration notes:
 *   SGE-original platform abstraction trait, no LibGDX counterpart
 *   Idiom: split packages
 *   Audited: 2026-03-03
 */
package sge
package platform

import java.nio.Buffer
import java.nio.ByteBuffer

/** Buffer operations for vertex data manipulation.
  *
  * Provides memory management, bulk copy, batch vertex transforms (matrix × vertex array), and vertex search operations. These are the performance-critical operations from LibGDX's BufferUtils that
  * benefit from native code.
  */
private[sge] trait BufferOps {

  // ─── Memory management ──────────────────────────────────────────────────

  /** Allocates a new direct ByteBuffer backed by native memory (malloc on JVM/Native, GC-managed on JS). */
  def newDisposableByteBuffer(numBytes: Int): ByteBuffer

  /** Frees native memory backing a disposable ByteBuffer. No-op on platforms with GC-managed buffers. */
  def freeMemory(buffer: ByteBuffer): Unit

  /** Returns the native memory address of a direct buffer. Unsupported on JS. */
  def getBufferAddress(buffer: Buffer): Long

  // ─── Copy ──────────────────────────────────────────────────────────────

  /** Copies bytes from a source array to a destination array.
    *
    * @param src
    *   source byte array
    * @param srcOffset
    *   byte offset into source
    * @param dst
    *   destination byte array
    * @param dstOffset
    *   byte offset into destination
    * @param numBytes
    *   number of bytes to copy
    */
  def copy(src: Array[Byte], srcOffset: Int, dst: Array[Byte], dstOffset: Int, numBytes: Int): Unit

  /** Copies floats from a source array to a destination array.
    *
    * @param src
    *   source float array
    * @param srcOffset
    *   element offset into source
    * @param dst
    *   destination float array
    * @param dstOffset
    *   element offset into destination
    * @param numFloats
    *   number of floats to copy
    */
  def copy(
    src:       Array[Float],
    srcOffset: Int,
    dst:       Array[Float],
    dstOffset: Int,
    numFloats: Int
  ): Unit

  // ─── Transform (Vec × Matrix, applied to vertex arrays) ───────────────

  /** Transforms Vec4 vertices by a 4×4 matrix (column-major). Modifies data in-place.
    *
    * @param data
    *   float array containing interleaved vertex data
    * @param strideInFloats
    *   stride between consecutive vertices, in floats
    * @param count
    *   number of vertices to transform
    * @param matrix
    *   4×4 matrix in column-major order (16 floats)
    * @param offsetInFloats
    *   offset to the first vertex component, in floats
    */
  def transformV4M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit

  /** Transforms Vec3 vertices by a 4×4 matrix (column-major, implicit w=1). Modifies data in-place.
    */
  def transformV3M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit

  /** Transforms Vec2 vertices by a 4×4 matrix (column-major, implicit z=0, w=1). Modifies data in-place.
    */
  def transformV2M4(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit

  /** Transforms Vec3 vertices by a 3×3 matrix (column-major). Modifies data in-place. */
  def transformV3M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit

  /** Transforms Vec2 vertices by a 3×3 matrix (column-major). Modifies data in-place. */
  def transformV2M3(
    data:           Array[Float],
    strideInFloats: Int,
    count:          Int,
    matrix:         Array[Float],
    offsetInFloats: Int
  ): Unit

  // ─── Find ──────────────────────────────────────────────────────────────

  /** Finds a vertex in a vertex array by exact bitwise float comparison.
    *
    * @param vertex
    *   the vertex to search for
    * @param vertexOffsetInFloats
    *   offset into the vertex array
    * @param strideInFloats
    *   stride between vertices in the search array, in floats
    * @param vertices
    *   the vertex array to search in
    * @param verticesOffsetInFloats
    *   offset into the vertices array
    * @param numVertices
    *   number of vertices to search
    * @return
    *   index of the first matching vertex, or -1 if not found
    */
  def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int
  ): Long

  /** Finds a vertex in a vertex array with epsilon tolerance.
    *
    * @param vertex
    *   the vertex to search for
    * @param vertexOffsetInFloats
    *   offset into the vertex array
    * @param strideInFloats
    *   stride between vertices in the search array, in floats
    * @param vertices
    *   the vertex array to search in
    * @param verticesOffsetInFloats
    *   offset into the vertices array
    * @param numVertices
    *   number of vertices to search
    * @param epsilon
    *   maximum allowed difference per component
    * @return
    *   index of the first matching vertex, or -1 if not found
    */
  def find(
    vertex:                 Array[Float],
    vertexOffsetInFloats:   Int,
    strideInFloats:         Int,
    vertices:               Array[Float],
    verticesOffsetInFloats: Int,
    numVertices:            Int,
    epsilon:                Float
  ): Long
}
