/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 17
 * Covenant-baseline-methods: ChainVfxEffect,render
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
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
