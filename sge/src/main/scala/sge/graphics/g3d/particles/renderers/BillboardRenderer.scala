/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/BillboardRenderer.java
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
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 56
 * Covenant-baseline-methods: BillboardRenderer,allocateChannels,copy,isCompatible,this
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/renderers/BillboardRenderer.java
 * Covenant-verified: 2026-04-19
 *
 * upstream-commit: f087ba76cac7ff2cc4ded91efc009f52bf0987c2
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
      BillboardControllerRenderData()
    ) {

  def this(batch: BillboardParticleBatch) = {
    this()
    setBatch(batch)
  }

  override def allocateChannels(): Unit =
    renderData.foreach { rd =>
      rd.positionChannel = controller.particles.addChannel(ParticleChannels.Position)
      rd.regionChannel = controller.particles.addChannel(ParticleChannels.TextureRegion, TextureRegionInitializer)
      rd.colorChannel = controller.particles.addChannel(ParticleChannels.Color, ColorInitializer)
      rd.scaleChannel = controller.particles.addChannel(ParticleChannels.Scale, ScaleInitializer)
      rd.rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation2D, Rotation2dInitializer)
    }

  override def copy(): ParticleControllerComponent =
    BillboardRenderer(batch)

  override def isCompatible(batch: ParticleBatch[?]): Boolean =
    batch.isInstanceOf[BillboardParticleBatch]
}
