/*
 * Ported from gdx-gltf - https://github.com/mgsx-dev/gdx-gltf
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package gltf
package loaders
package shared
package data

import java.nio.{ ByteBuffer, ByteOrder }
import sge.gltf.data.data.{ GLTFAccessor, GLTFBufferView }
import sge.gltf.loaders.shared.GLTFTypes

class AccessorBuffer private (
  private val accessor: GLTFAccessor,
  /** BufferView that represents this buffer. Is null when the accessor has no buffer (is initialized with zeros).
    */
  private val bufferView:      GLTFBufferView | Null, // @nowarn — nullable for zero-initialized case
  private var data:            ByteBuffer,
  private var byteOffsetValue: Int
) {

  def getByteStride: Int =
    if (bufferView != null && bufferView.byteStride.isDefined) {
      bufferView.byteStride.get
    } else {
      GLTFTypes.accessorStrideSize(accessor)
    }

  def getByteOffset: Int = byteOffsetValue

  /** If the accessor is backed by a buffer potentially used by others, create a copy so this buffer can be safely manipulated
    */
  def prepareForWriting(): Unit =
    if (bufferView != null) {
      // replace the buffer view with a copy as we will have to modify it
      prepareForReading()
      val clone = ByteBuffer.allocate(data.remaining())
      clone.put(data)
      clone.order(data.order())
      clone.flip()
      data = clone
      byteOffsetValue = 0
    }

  /** The buffer will be positioned on the first element belonging to the accessor.
    *
    * @see
    *   [[getData]]
    */
  def prepareForReading(): ByteBuffer = {
    data.position(getByteOffset)
    data
  }

  /** Buffer containing the accessors' data. Make sure to consider [[getByteStride]] and [[getByteOffset]] when reading from this!
    */
  def getData: ByteBuffer = data
}

object AccessorBuffer {

  def fromBufferView(
    glAccessor:   GLTFAccessor,
    glBufferView: GLTFBufferView,
    resolver:     DataFileResolver
  ): AccessorBuffer = {
    val byteOffset = glBufferView.byteOffset + glAccessor.byteOffset
    val buf        = new AccessorBuffer(
      glAccessor,
      glBufferView,
      resolver.getBuffer(glBufferView.buffer.get),
      byteOffset
    )
    buf.data.position(buf.getByteOffset)
    buf
  }

  def fromZeros(glAccessor: GLTFAccessor): AccessorBuffer = {
    // spec for undefined bufferView:
    // "When undefined, the accessor **MUST** be initialized with zeros"
    val zeros = ByteBuffer.allocate(GLTFTypes.accessorSize(glAccessor)).order(ByteOrder.LITTLE_ENDIAN)
    new AccessorBuffer(glAccessor, null, zeros, 0) // @nowarn — no bufferView for zero-initialized
  }
}
