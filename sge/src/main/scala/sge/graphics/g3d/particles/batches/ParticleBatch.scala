/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/batches/ParticleBatch.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java interface → Scala 3 trait
 * - save/load not declared directly; inherited via ResourceData.Configurable mixin (equivalent API)
 * - All public methods (begin, draw, end) faithfully ported
 * - Audit: pass (2026-03-03)
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 38
 * Covenant-baseline-methods: ParticleBatch,begin,draw,end
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/batches/ParticleBatch.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: f087ba76cac7ff2cc4ded91efc009f52bf0987c2
 */
package sge
package graphics
package g3d
package particles
package batches

import sge.graphics.g3d.RenderableProvider
import sge.graphics.g3d.particles.ResourceData
import sge.graphics.g3d.particles.renderers.ParticleControllerRenderData

/** Common interface to all the batches that render particles.
  * @author
  *   Inferno
  */
trait ParticleBatch[T <: ParticleControllerRenderData] extends RenderableProvider with ResourceData.Configurable {

  /** Must be called once before any drawing operation */
  def begin(): Unit

  def draw(controller: T): Unit

  /** Must be called after all the drawing operations */
  def end(): Unit
}
