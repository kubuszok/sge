/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

import sge.graphics.Pixmap
import sge.vfx.VfxRenderContext
import sge.vfx.effects.util.{ CopyEffect, MixEffect }
import sge.vfx.framebuffer.{ VfxFrameBufferQueue, VfxPingPongWrapper }

/** A motion blur effect which draws the last frame with a lower opacity. The result is then stored as the next last frame to create
  * the trail effect.
  */
class MotionBlurEffect(pixelFormat: Pixmap.Format, mixMethod: MixEffect.Method, blurFactor: Float)(using Sge)
    extends CompositeVfxEffect
    with ChainVfxEffect {

  private val mixFilter: MixEffect = register(MixEffect(mixMethod))
  private val copyFilter: CopyEffect = register(CopyEffect())

  // On WebGL we cannot render from/into the same texture simultaneously.
  // Will use ping-pong approach to avoid "writing into itself".
  // SGE doesn't have ApplicationType.WebGL check the same way, just use 2 for safety.
  private val localBuffer: VfxFrameBufferQueue = VfxFrameBufferQueue(pixelFormat, 2)

  private var firstFrameRendered: Boolean = false

  mixFilter.mixFactor = blurFactor

  override def close(): Unit = {
    super.close()
    localBuffer.close()
  }

  override def resize(width: Int, height: Int): Unit = {
    super.resize(width, height)
    localBuffer.resize(width, height)
    firstFrameRendered = false
  }

  override def rebind(): Unit = {
    super.rebind()
    localBuffer.rebind()
  }

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit = {
    val prevFrame = localBuffer.changeToNext()
    if (!firstFrameRendered) {
      // Mix filter requires two frames to render, so we gonna skip the first call.
      copyFilter.render(context, buffers.srcBuffer, prevFrame)
      buffers.swap()
      firstFrameRendered = true
    } else {
      mixFilter.render(context, buffers.srcBuffer, prevFrame, buffers.dstBuffer)
      copyFilter.render(context, buffers.dstBuffer, prevFrame)
    }
  }
}
