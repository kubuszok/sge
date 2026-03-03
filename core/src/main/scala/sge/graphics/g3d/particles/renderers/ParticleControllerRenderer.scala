/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/ParticleControllerRenderer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - renderData typed as Nullable[D] instead of bare D (null safety improvement)
 * - update() wraps batch.draw(renderData) in renderData.foreach (skips if null, Java would NPE)
 * - set() uses renderData.foreach instead of if (renderData != null) — equivalent
 * - setBatch() uses isInstanceOf + asInstanceOf (matches Java unchecked cast)
 * - All 4 methods faithfully ported: update, setBatch, isCompatible (abstract), set
 * - Audited 2026-03-03: pass
 */
package sge
package graphics
package g3d
package particles
package renderers

import sge.graphics.g3d.particles.ParticleController
import sge.graphics.g3d.particles.ParticleControllerComponent
import sge.graphics.g3d.particles.batches.ParticleBatch
import sge.utils.Nullable

/** It's a {@link ParticleControllerComponent} which determines how the particles are rendered. It's the base class of every particle renderer.
  * @author
  *   Inferno
  */
abstract class ParticleControllerRenderer[D <: ParticleControllerRenderData, T <: ParticleBatch[D]] extends ParticleControllerComponent {

  protected var batch:      T           = scala.compiletime.uninitialized
  protected var renderData: Nullable[D] = Nullable.empty

  protected def this(renderData: D) = {
    this()
    this.renderData = Nullable(renderData)
  }

  override def update(): Unit =
    renderData.foreach { rd =>
      batch.draw(rd)
    }

  @SuppressWarnings(Array("unchecked"))
  def setBatch(batch: ParticleBatch[?]): Boolean =
    if (isCompatible(batch)) {
      this.batch = batch.asInstanceOf[T]
      true
    } else false

  def isCompatible(batch: ParticleBatch[?]): Boolean

  override def set(particleController: ParticleController): Unit = {
    super.set(particleController)
    renderData.foreach { rd =>
      rd.controller = controller
    }
  }
}
