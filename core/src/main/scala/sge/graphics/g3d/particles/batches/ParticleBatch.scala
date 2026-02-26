/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/batches/ParticleBatch.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles
package batches

import sge.assets.AssetManager
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
