/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx

import sge.graphics.Pixmap
import sge.vfx.framebuffer.{ VfxFrameBufferPool, VfxFrameBufferRenderer }
import sge.vfx.utils.ViewportQuadMesh

class VfxRenderContext(val pixelFormat: Pixmap.Format, initialWidth: Int, initialHeight: Int)(using Sge) extends AutoCloseable {

  val bufferPool:     VfxFrameBufferPool     = VfxFrameBufferPool(pixelFormat, initialWidth, initialHeight, 8)
  val bufferRenderer: VfxFrameBufferRenderer = VfxFrameBufferRenderer()

  private var _bufferWidth:  Int = initialWidth
  private var _bufferHeight: Int = initialHeight

  override def close(): Unit = {
    bufferPool.close()
    bufferRenderer.close()
  }

  def resize(w: Int, h: Int): Unit = {
    _bufferWidth = w
    _bufferHeight = h
    bufferPool.resize(w, h)
  }

  def rebind(): Unit =
    bufferRenderer.rebind()

  def viewportMesh: ViewportQuadMesh = bufferRenderer.getMesh

  def bufferWidth:  Int = _bufferWidth
  def bufferHeight: Int = _bufferHeight
}
