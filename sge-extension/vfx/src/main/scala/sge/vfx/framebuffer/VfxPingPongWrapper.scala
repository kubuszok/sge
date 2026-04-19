/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 148
 * Covenant-baseline-methods: VfxPingPongWrapper,_capturing,begin,bufDst,bufSrc,bufferPool,capturing,cleanUpBuffers,dst,dstBuffer,dstTexture,end,gl,initialize,isInitialized,reset,src,srcBuffer,srcTexture,swap,this,tmp,wasCapturing
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package framebuffer

import sge.graphics.{ ClearMask, Color, Texture }
import sge.utils.Nullable

/** Encapsulates a pair of [[VfxFrameBuffer]]s with the ability to swap between them.
  *
  * Upon [[begin]] the buffer is reset to a known initial state, this is usually done just before the first usage of the buffer. Subsequent [[swap]] calls will initiate writing to the next available
  * buffer, effectively ping-ponging between the two. Chained rendering will be possible by retrieving the necessary buffers via [[srcBuffer]], [[dstBuffer]], [[srcTexture]] or [[dstTexture]].
  *
  * When rendering is finished, [[end]] should be called to stop capturing.
  */
class VfxPingPongWrapper(using Sge) {

  protected var bufDst: Nullable[VfxFrameBuffer] = Nullable.empty
  protected var bufSrc: Nullable[VfxFrameBuffer] = Nullable.empty

  /** Where capturing is started. Should be true between [[begin]] and [[end]]. */
  protected var _capturing: Boolean = false

  protected var bufferPool: Nullable[VfxFrameBufferPool] = Nullable.empty

  def this(pool: VfxFrameBufferPool)(using Sge) = {
    this()
    initialize(pool)
  }

  def this(src: VfxFrameBuffer, dst: VfxFrameBuffer)(using Sge) = {
    this()
    initialize(src, dst)
  }

  def initialize(pool: VfxFrameBufferPool): VfxPingPongWrapper = {
    this.bufferPool = Nullable(pool)
    val dst = pool.obtain()
    val src = pool.obtain()
    initialize(src, dst)
  }

  def initialize(src: VfxFrameBuffer, dst: VfxFrameBuffer): VfxPingPongWrapper = {
    if (_capturing) {
      throw IllegalStateException(
        "Ping pong buffer cannot be initialized during capturing stage. It seems the instance is already initialized."
      )
    }
    if (isInitialized) {
      reset()
    }
    this.bufSrc = Nullable(src)
    this.bufDst = Nullable(dst)
    this
  }

  def reset(): Unit = {
    if (_capturing) {
      throw IllegalStateException("Ping pong buffer cannot be reset during capturing stage. Forgot to call end()?")
    }

    // If the buffers were created using VfxBufferPool, we shall free them properly.
    if (bufferPool.isDefined) {
      bufferPool.get.free(bufSrc.get)
      bufferPool.get.free(bufDst.get)
      bufferPool = Nullable.empty
    }

    bufSrc = Nullable.empty
    bufDst = Nullable.empty
  }

  def isInitialized: Boolean = bufDst.isDefined && bufSrc.isDefined

  /** Start capturing into the destination buffer. To swap buffers during capturing, call [[swap]]. [[end]] shall be called after rendering to ping-pong buffer is done.
    */
  def begin(): Unit = {
    if (_capturing) {
      throw IllegalStateException("Ping pong buffer is already in capturing state.")
    }

    _capturing = true
    bufDst.get.begin()
  }

  /** Finishes ping-ponging. Must be called after [[begin]]. */
  def end(): Unit = {
    if (!_capturing) {
      throw IllegalStateException("Ping pong is not in capturing state. You should call begin() before calling end().")
    }
    bufDst.get.end()
    _capturing = false
  }

  /** Swaps source/target buffers. May be called outside of capturing state. */
  def swap(): Unit = {
    if (_capturing) {
      bufDst.get.end()
    }

    // Swap buffers
    val tmp = this.bufDst
    bufDst = bufSrc
    bufSrc = tmp

    if (_capturing) {
      bufDst.get.begin()
    }
  }

  def capturing: Boolean = _capturing

  /** @return the source texture of the current ping-pong chain. */
  def srcTexture: Texture = bufSrc.get.getFbo.get.colorBufferTexture

  /** @return the source buffer of the current ping-pong chain. */
  def srcBuffer: VfxFrameBuffer = bufSrc.get

  /** @return the result's texture of the latest [[swap]]. */
  def dstTexture: Texture = bufDst.get.getFbo.get.colorBufferTexture

  /** @return Returns the result's buffer of the latest [[swap]]. */
  def dstBuffer: VfxFrameBuffer = bufDst.get

  /** Cleans up managed [[VfxFrameBuffer]]s' with the color specified. */
  def cleanUpBuffers(clearColor: Color): Unit =
    cleanUpBuffers(clearColor.r, clearColor.g, clearColor.b, clearColor.a)

  /** Cleans up managed [[VfxFrameBuffer]]s' with the color specified. */
  def cleanUpBuffers(r: Float, g: Float, b: Float, a: Float): Unit = {
    val gl           = Sge().graphics.gl20
    val wasCapturing = this._capturing

    if (!wasCapturing) { begin() }

    gl.glClearColor(r, g, b, a)
    gl.glClear(ClearMask.ColorBufferBit)
    swap()
    gl.glClear(ClearMask.ColorBufferBit)

    if (!wasCapturing) { end() }
  }
}
