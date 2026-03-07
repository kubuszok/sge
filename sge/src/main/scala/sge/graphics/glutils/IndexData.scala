/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/glutils/IndexData.java
 * Original authors: mzechner
 * Licensed under the Apache License, Version 2.0
 *
 * Migration notes:
 *   Convention: Java interface -> Scala trait; Disposable -> AutoCloseable
 *   Idiom: split packages
 *   Audited: 2026-03-03
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package graphics
package glutils

import java.nio.ShortBuffer

/** An IndexData instance holds index data. Can be either a plain short buffer or an OpenGL buffer object.
  * @author
  *   mzechner
  */
trait IndexData extends AutoCloseable {

  /** @return the number of indices currently stored in this buffer */
  def getNumIndices(): Int

  /** @return the maximum number of indices this IndexBufferObject can store. */
  def getNumMaxIndices(): Int

  /** <p> Sets the indices of this IndexBufferObject, discarding the old indices. The count must equal the number of indices to be copied to this IndexBufferObject. </p>
    *
    * <p> This can be called in between calls to {@link #bind()} and {@link #unbind()} . The index data will be updated instantly. </p>
    *
    * @param indices
    *   the index data
    * @param offset
    *   the offset to start copying the data from
    * @param count
    *   the number of shorts to copy
    */
  def setIndices(indices: Array[Short], offset: Int, count: Int): Unit

  /** Copies the specified indices to the indices of this IndexBufferObject, discarding the old indices. Copying start at the current {@link ShortBuffer#position()} of the specified buffer and copied
    * the {@link ShortBuffer#remaining()} amount of indices. This can be called in between calls to {@link #bind()} and {@link #unbind()} . The index data will be updated instantly.
    * @param indices
    *   the index data to copy
    */
  def setIndices(indices: ShortBuffer): Unit

  /** Update (a portion of) the indices.
    * @param targetOffset
    *   offset in indices buffer
    * @param indices
    *   the index data
    * @param offset
    *   the offset to start copying the data from
    * @param count
    *   the number of shorts to copy
    */
  def updateIndices(targetOffset: Int, indices: Array[Short], offset: Int, count: Int): Unit

  /** <p> Returns the underlying ShortBuffer. If you modify the buffer contents they will be uploaded on the next call to {@link #bind()} . If you need immediate uploading use
    * {@link #setIndices(short[], int, int)} . </p>
    *
    * @return
    *   the underlying short buffer.
    * @deprecated
    *   use {@link #getBuffer(boolean)} instead
    */
  @deprecated("use getBuffer(boolean) instead", "")
  def getBuffer(): ShortBuffer

  /** Returns the underlying ShortBuffer for reading or writing.
    * @param forWriting
    *   when true, the underlying buffer will be uploaded on the next call to {@link #bind()} . If you need immediate uploading use {@link #setIndices(short[], int, int)} .
    * @return
    *   the underlying short buffer.
    */
  def getBuffer(forWriting: Boolean): ShortBuffer

  /** Binds this IndexBufferObject for rendering with glDrawElements. */
  def bind(): Unit

  /** Unbinds this IndexBufferObject. */
  def unbind(): Unit

  /** Invalidates the IndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss. */
  def invalidate(): Unit

  /** Disposes this IndexDatat and all its associated OpenGL resources. */
  def close(): Unit
}
