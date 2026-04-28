/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: partial-port
 * Covenant-source-reference: com/crashinvaders/vfx/framebuffer/VfxFrameBufferPool.java
 * Covenant: partial-port
 * Covenant-verified: 2026-04-08
 * Covenant-verified: 2026-04-08
 *
 * Partial-port debt:
 *   - Inherited TODO: missing scaladoc on the public API. Functional but not documented.
 *
 * upstream-commit: ece6757aa75974d6396325d9b8e0d0b8c8b5c28e
 */
package sge
package vfx
package framebuffer

import sge.graphics.{ Pixmap, Texture }

import scala.collection.mutable.ArrayBuffer

//TODO Add scaladoc.
class VfxFrameBufferPool(
  var pixelFormat:     Pixmap.Format,
  private var _width:  Int,
  private var _height: Int,
  initialCapacity:     Int
)(using Sge)
    extends AutoCloseable {

  /** The highest number of free buffer instances. Can be reset any time. */
  var freePeak: Int = 0

  /** A collection of all the buffers created and managed by the pool. */
  protected val managedBuffers: ArrayBuffer[VfxFrameBuffer] = ArrayBuffer.empty

  /** A pool of spare buffers that are ready to be obtained. */
  protected val freeBuffers: ArrayBuffer[VfxFrameBuffer] = ArrayBuffer.empty

  private var textureWrapU:     Texture.TextureWrap   = Texture.TextureWrap.ClampToEdge
  private var textureWrapV:     Texture.TextureWrap   = Texture.TextureWrap.ClampToEdge
  private var textureFilterMin: Texture.TextureFilter = Texture.TextureFilter.Nearest
  private var textureFilterMag: Texture.TextureFilter = Texture.TextureFilter.Nearest

  private var disposed: Boolean = false

  override def close(): Unit = {
    if (managedBuffers.size != freeBuffers.size) {
      val unfreedBufferAmount = managedBuffers.size - freeBuffers.size
      System.err.println(
        "[VfxFrameBufferPool] At the moment of disposal, the pool still has some managed buffers unfreed (" + unfreedBufferAmount + "). " +
          "Someone's using them and hasn't freed?"
      )
    }

    disposed = true

    var i = 0
    while (i < managedBuffers.size) {
      managedBuffers(i).close()
      i += 1
    }
    managedBuffers.clear()
    freeBuffers.clear()
  }

  def resize(width: Int, height: Int): Unit = {
    this._width = width
    this._height = height
    cleanupInvalid()
  }

  /** Returns a buffer from this pool. The buffer may be new (from [[createBuffer]]) or reused (previously [[free freed]]). */
  def obtain(): VfxFrameBuffer = {
    if (disposed) throw IllegalStateException("Instance is already disposed")

    if (freeBuffers.isEmpty) createBuffer() else freeBuffers.remove(freeBuffers.size - 1)
  }

  /** Returns the buffer in the free pool, making it eligible for [[obtain]].
    *
    * For performance sake, the pool does not check if the buffer is already freed, so the same buffer must not be freed multiple times.
    */
  def free(buffer: VfxFrameBuffer): Unit = {
    if (disposed) throw IllegalStateException("Instance is already disposed")
    require(buffer != null, "buffer cannot be null.") // @nowarn — Java interop boundary

    if (!validateBuffer(buffer)) {
      managedBuffers -= buffer
      buffer.close()
    } else {
      freeBuffers += buffer
      freePeak = Math.max(freePeak, freeBuffers.size)
      resetBuffer(buffer)
    }
  }

  /** Removes all the free buffers from the pool. */
  def clearFree(): Unit = {
    var i = 0
    while (i < freeBuffers.size) {
      val buffer = freeBuffers(i)
      managedBuffers -= buffer
      buffer.close()
      i += 1
    }
    freeBuffers.clear()
  }

  /** @return the number of the free buffers available. */
  def freeCount: Int = freeBuffers.size

  protected def createBuffer(): VfxFrameBuffer = {
    val buffer = VfxFrameBuffer(pixelFormat)
    buffer.initialize(_width, _height)
    managedBuffers += buffer
    buffer
  }

  /** Called when a buffer is freed to clear the state of the buffer for possible later reuse. */
  protected def resetBuffer(buffer: VfxFrameBuffer): Unit = {
    buffer.clearRenderers()

    // Reset texture params to the default ones.
    val tex = buffer.texture.get
    tex.setWrap(textureWrapU, textureWrapV)
    tex.setFilter(textureFilterMin, textureFilterMag)
  }

  protected def validateBuffer(buffer: VfxFrameBuffer): Boolean = {
    val f = buffer.getFbo
    buffer.initialized &&
    f.isDefined &&
    this._width == f.get.width.toInt &&
    this._height == f.get.height.toInt &&
    this.pixelFormat == buffer.pixelFormat
  }

  /** Checks if the buffers are valid. Those which are not will be reconstructed or deleted if they are free. */
  protected def cleanupInvalid(): Unit = {
    var i = 0
    while (i < managedBuffers.size) {
      val buffer = managedBuffers(i)
      if (!validateBuffer(buffer)) {
        // Buffer is invalid - means we have to reinitialize it according to the current configuration.
        // FBO reinitialization is an expensive operation, no reason doing it for the buffers that are currently not in use.
        // So in case a buffer is free, we just dispose and delete it.
        val wasFree = freeBuffers.contains(buffer)
        if (wasFree) {
          freeBuffers -= buffer
          managedBuffers.remove(i)
          buffer.close()
          // Don't increment i since we removed the element
        } else {
          buffer.initialize(_width, _height)
          i += 1
        }
      } else {
        i += 1
      }
    }
  }

  def setTextureParams(
    wrapU:     Texture.TextureWrap,
    wrapV:     Texture.TextureWrap,
    filterMin: Texture.TextureFilter,
    filterMag: Texture.TextureFilter
  ): Unit = {
    this.textureWrapU = wrapU
    this.textureWrapV = wrapV
    this.textureFilterMin = filterMin
    this.textureFilterMag = filterMag

    // Update the free textures'.
    var i = 0
    while (i < freeBuffers.size) {
      val tex = freeBuffers(i).texture.get
      tex.setWrap(wrapU, wrapV)
      tex.setFilter(filterMin, filterMag)
      i += 1
    }
  }
}
