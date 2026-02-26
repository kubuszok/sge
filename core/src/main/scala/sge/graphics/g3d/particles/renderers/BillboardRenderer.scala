/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/BillboardRenderer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
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
import sge.graphics.g3d.particles.batches.{ BillboardParticleBatch, ParticleBatch }

/** A {@link ParticleControllerRenderer} which will render particles as billboards to a {@link BillboardParticleBatch} .
  * @author
  *   Inferno
  */
class BillboardRenderer
    extends ParticleControllerRenderer[BillboardControllerRenderData, BillboardParticleBatch](
      new BillboardControllerRenderData()
    ) {

  def this(batch: BillboardParticleBatch) = {
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

  override def copy(): ParticleControllerComponent =
    new BillboardRenderer(batch)

  override def isCompatible(batch: ParticleBatch[?]): Boolean =
    batch.isInstanceOf[BillboardParticleBatch]
}
