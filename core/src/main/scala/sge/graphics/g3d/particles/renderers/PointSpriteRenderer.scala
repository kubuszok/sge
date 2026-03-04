/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/PointSpriteRenderer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All 4 methods ported: allocateChannels, copy, isCompatible, secondary constructor
 * - allocateChannels() wrapped in renderData.foreach (null safety; Java accesses directly)
 * - import scala.language.implicitConversions is present but may be unused (minor)
 * - Audited 2026-03-03: pass
 */
package sge
package graphics
package g3d
package particles
package renderers

import scala.language.implicitConversions

import sge.graphics.g3d.particles.ParticleChannels
import sge.graphics.g3d.particles.ParticleChannels.{ ColorInitializer, Rotation2dInitializer, ScaleInitializer, TextureRegionInitializer }
import sge.graphics.g3d.particles.ParticleControllerComponent
import sge.graphics.g3d.particles.batches.{ ParticleBatch, PointSpriteParticleBatch }

/** A {@link ParticleControllerRenderer} which will render particles as point sprites to a {@link PointSpriteParticleBatch} .
  * @author
  *   Inferno
  */
class PointSpriteRenderer
    extends ParticleControllerRenderer[PointSpriteControllerRenderData, PointSpriteParticleBatch](
      new PointSpriteControllerRenderData()
    ) {

  def this(batch: PointSpriteParticleBatch) = {
    this()
    setBatch(batch)
  }

  override def allocateChannels(): Unit =
    renderData.foreach { rd =>
      rd.positionChannel = controller.particles.addChannel(ParticleChannels.Position)
      rd.regionChannel = controller.particles.addChannel(ParticleChannels.TextureRegion, TextureRegionInitializer.get())
      rd.colorChannel = controller.particles.addChannel(ParticleChannels.Color, ColorInitializer.get())
      rd.scaleChannel = controller.particles.addChannel(ParticleChannels.Scale, ScaleInitializer.get())
      rd.rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation2D, Rotation2dInitializer.get())
    }

  override def isCompatible(batch: ParticleBatch[?]): Boolean =
    batch.isInstanceOf[PointSpriteParticleBatch]

  override def copy(): ParticleControllerComponent =
    new PointSpriteRenderer(batch)
}
