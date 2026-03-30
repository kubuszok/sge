/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package framebuffer

import sge.graphics.{ Pixmap, Texture }

import scala.collection.mutable.ArrayBuffer

/** Provides looped access to an array of [[VfxFrameBuffer]]. */
class VfxFrameBufferQueue(pixelFormat: Pixmap.Format, fboAmount: Int)(using Sge) extends AutoCloseable {

  require(fboAmount >= 1, "FBO amount should be a positive number.")

  private val buffers: ArrayBuffer[VfxFrameBuffer] = {
    val buf = ArrayBuffer.empty[VfxFrameBuffer]
    var i = 0
    while (i < fboAmount) {
      buf += VfxFrameBuffer(pixelFormat)
      i += 1
    }
    buf
  }

  private var currentIdx: Int = 0

  private var wrapU: Texture.TextureWrap = Texture.TextureWrap.ClampToEdge
  private var wrapV: Texture.TextureWrap = Texture.TextureWrap.ClampToEdge
  private var filterMin: Texture.TextureFilter = Texture.TextureFilter.Nearest
  private var filterMag: Texture.TextureFilter = Texture.TextureFilter.Nearest

  override def close(): Unit = {
    var i = 0
    while (i < buffers.size) {
      buffers(i).close()
      i += 1
    }
    buffers.clear()
  }

  def resize(width: Int, height: Int): Unit = {
    var i = 0
    while (i < buffers.size) {
      buffers(i).initialize(width, height)
      i += 1
    }
  }

  /** Restores buffer OpenGL parameters. Could be useful in case of OpenGL context loss. */
  def rebind(): Unit = {
    var i = 0
    while (i < buffers.size) {
      val wrapper = buffers(i)
      // FBOs might be null if the instance wasn't initialized with resize yet.
      if (wrapper.getFbo.isDefined) {
        val tex = wrapper.getFbo.get.colorBufferTexture
        tex.setWrap(wrapU, wrapV)
        tex.setFilter(filterMin, filterMag)
      }
      i += 1
    }
  }

  def current: VfxFrameBuffer = buffers(currentIdx)

  def changeToNext(): VfxFrameBuffer = {
    currentIdx = (currentIdx + 1) % buffers.size
    current
  }

  def setTextureParams(u: Texture.TextureWrap, v: Texture.TextureWrap, min: Texture.TextureFilter, mag: Texture.TextureFilter): Unit = {
    wrapU = u
    wrapV = v
    filterMin = min
    filterMag = mag
    rebind()
  }
}
