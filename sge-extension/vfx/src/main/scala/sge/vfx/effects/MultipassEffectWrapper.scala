/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 */
package sge
package vfx
package effects

import sge.vfx.VfxRenderContext
import sge.vfx.framebuffer.VfxPingPongWrapper

class MultipassEffectWrapper(private val effect: ChainVfxEffect) extends AbstractVfxEffect with ChainVfxEffect {

  var passes: Int = 1

  override def resize(width: Int, height: Int): Unit =
    effect.resize(width, height)

  override def update(delta: Float): Unit =
    effect.update(delta)

  override def rebind(): Unit =
    effect.rebind()

  override def close(): Unit =
    effect.close()

  override def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit = {
    // Simply swap buffers to simulate render skip.
    if (passes == 0) {
      buffers.swap()
    } else {
      val finalPasses = this.passes
      var i = 0
      while (i < finalPasses) {
        effect.render(context, buffers)
        if (i < finalPasses - 1) {
          buffers.swap()
        }
        i += 1
      }
    }
  }
}
