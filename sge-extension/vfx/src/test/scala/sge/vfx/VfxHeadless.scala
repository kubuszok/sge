/*
 * Copyright 2025-2026 Mateusz Kubuszok
 * Licensed under the Apache License, Version 2.0
 *
 * Shared headless test fixture for the ISS-561 (batch F) vfx coverage suites.
 *
 * The headless Sge itself is sge's own SgeTestFixture, reached via the
 * `test->test` dependency declared on the sge-vfx module — so the vfx suites
 * reuse that shared noop Application/Files/Net helper rather than re-deriving
 * it. CompleteGL20 is the only extra GL seam the vfx suites need: a
 * VfxFrameBuffer.initialize(...) builds a real glutils FrameBuffer, whose
 * GLFrameBuffer.build() throws unless glCheckFramebufferStatus reports
 * GL_FRAMEBUFFER_COMPLETE (NoopGL20 returns 0). Returning COMPLETE lets the FBO
 * construct headlessly; every other GL call delegates to the no-op impl.
 */
package sge
package vfx

import sge.graphics.GL20
import sge.noop.{ NoopGL20, NoopGraphics }

object VfxHeadless {

  /** Creates a headless [[Sge]] whose graphics returns the supplied [[GL20]] (defaulting to the pure no-op). */
  def headlessSge(glImpl: GL20 = NoopGL20): Sge =
    SgeTestFixture.testSge(graphics = new NoopGraphics() {
      override def gl20: GL20 = glImpl
    })

  /** A [[GL20]] that lets a glutils FrameBuffer construct: it reports the FBO as complete so GLFrameBuffer.build() does not throw. Everything else is delegated to the no-op implementation. */
  final class CompleteGL20 extends GL20 {
    private val underlying: GL20 = NoopGL20
    export underlying.{ glCheckFramebufferStatus as _, * }

    override def glCheckFramebufferStatus(target: Int): Int = GL20.GL_FRAMEBUFFER_COMPLETE
  }
}
