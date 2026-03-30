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

/** Any effect that is compatible with [[sge.vfx.VfxManager]]'s render chain, should implement this interface. */
trait ChainVfxEffect extends VfxEffect {
  def render(context: VfxRenderContext, buffers: VfxPingPongWrapper): Unit
}
