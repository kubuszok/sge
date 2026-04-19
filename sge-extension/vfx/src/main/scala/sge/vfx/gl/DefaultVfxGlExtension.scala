/*
 * Ported from gdx-vfx - https://github.com/crashinvaders/gdx-vfx
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 23
 * Covenant-baseline-methods: DefaultVfxGlExtension,boundFboHandle,tmpIntBuf
 * Covenant-source-reference: SGE-original
 * Covenant-verified: 2026-04-19
 */
package sge
package vfx
package gl

import sge.graphics.GL20
import java.nio.{ ByteBuffer, ByteOrder, IntBuffer }

class DefaultVfxGlExtension(using Sge) extends VfxGlExtension {

  private val tmpIntBuf: IntBuffer =
    ByteBuffer.allocateDirect(16 * Integer.SIZE / 8).order(ByteOrder.nativeOrder()).asIntBuffer()

  override def boundFboHandle: Int = {
    Sge().graphics.gl20.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, tmpIntBuf)
    tmpIntBuf.get(0)
  }
}
