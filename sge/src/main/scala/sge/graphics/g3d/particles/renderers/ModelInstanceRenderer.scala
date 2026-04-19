/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/ModelInstanceRenderer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All 6 methods ported: allocateChannels, init, update, copy, isCompatible, secondary ctor
 * - init(): Java null assignments + boolean checks -> Scala Nullable getChannel + isDefined/foreach
 * - init(): modelInstanceChannel null -> getOrElse + SgeError.InvalidInput (matches Java exception)
 * - update(): wrapped in renderData.foreach; Java data[] -> Scala floatData()/objectData()
 * - update(): Java cast of BlendingAttribute + null check -> Scala Nullable().map().foreach
 * - update() calls super.update() at end (matches Java)
 * - Private boolean flags (hasColor/hasScale/hasRotation) faithfully preserved
 * - Audited 2026-03-03: pass
 *
 * Covenant: full-port
 * Covenant-baseline-spec-pass: 0
 * Covenant-baseline-loc: 126
 * Covenant-baseline-methods: ModelInstanceRenderer,allocateChannels,copy,hasColor,hasRotation,hasScale,init,isCompatible,this,update
 * Covenant-source-reference: com/badlogic/gdx/graphics/g3d/particles/renderers/ModelInstanceRenderer.java
 * Covenant-verified: 2026-04-19
 */
package sge
package graphics
package g3d
package particles
package renderers

import sge.graphics.g3d.ModelInstance
import sge.graphics.g3d.attributes.{ BlendingAttribute, ColorAttribute }
import sge.graphics.g3d.particles.ParallelArray.{ FloatChannel, ObjectChannel }
import sge.graphics.g3d.particles.{ ParticleChannels, ParticleControllerComponent }
import sge.graphics.g3d.particles.batches.{ ModelInstanceParticleBatch, ParticleBatch }
import sge.utils.{ Nullable, SgeError }

/** A {@link ParticleControllerRenderer} which will render particles as {@link ModelInstance} to a {@link ModelInstanceParticleBatch}.
  * @author
  *   Inferno
  */
class ModelInstanceRenderer
    extends ParticleControllerRenderer[ModelInstanceControllerRenderData, ModelInstanceParticleBatch](
      ModelInstanceControllerRenderData()
    ) {

  private var hasColor:    Boolean = false
  private var hasScale:    Boolean = false
  private var hasRotation: Boolean = false

  def this(batch: ModelInstanceParticleBatch) = {
    this()
    setBatch(batch)
  }

  override def allocateChannels(): Unit =
    renderData.foreach { rd =>
      rd.positionChannel = controller.particles.addChannel(ParticleChannels.Position)
    }

  override def init(): Unit =
    renderData.foreach { rd =>
      rd.modelInstanceChannel = controller.particles
        .getChannel[ObjectChannel[ModelInstance]](ParticleChannels.ModelInstance)
        .getOrElse(
          throw SgeError.InvalidInput("ModelInstance channel not allocated")
        )
      val colorOpt = controller.particles.getChannel[FloatChannel](ParticleChannels.Color)
      hasColor = colorOpt.isDefined
      colorOpt.foreach { ch => rd.colorChannel = ch }
      val scaleOpt = controller.particles.getChannel[FloatChannel](ParticleChannels.Scale)
      hasScale = scaleOpt.isDefined
      scaleOpt.foreach { ch => rd.scaleChannel = ch }
      val rotationOpt = controller.particles.getChannel[FloatChannel](ParticleChannels.Rotation3D)
      hasRotation = rotationOpt.isDefined
      rotationOpt.foreach { ch => rd.rotationChannel = ch }
    }

  override def update(): Unit = {
    renderData.foreach { rd =>
      var i              = 0
      var positionOffset = 0
      val c              = controller.particles.size
      while (i < c) {
        val instance = rd.modelInstanceChannel.objectData(i)
        val scale    = if (hasScale) rd.scaleChannel.floatData(i) else 1f
        var qx       = 0f; var qy = 0f; var qz = 0f; var qw = 1f
        if (hasRotation) {
          val rotationOffset = i * rd.rotationChannel.strideSize
          qx = rd.rotationChannel.floatData(rotationOffset + ParticleChannels.XOffset)
          qy = rd.rotationChannel.floatData(rotationOffset + ParticleChannels.YOffset)
          qz = rd.rotationChannel.floatData(rotationOffset + ParticleChannels.ZOffset)
          qw = rd.rotationChannel.floatData(rotationOffset + ParticleChannels.WOffset)
        }

        instance.transform.set(
          rd.positionChannel.floatData(positionOffset + ParticleChannels.XOffset),
          rd.positionChannel.floatData(positionOffset + ParticleChannels.YOffset),
          rd.positionChannel.floatData(positionOffset + ParticleChannels.ZOffset),
          qx,
          qy,
          qz,
          qw,
          scale,
          scale,
          scale
        )
        if (hasColor) {
          val colorOffset    = i * rd.colorChannel.strideSize
          val colorAttribute = instance.materials(0).get(ColorAttribute.Diffuse).asInstanceOf[ColorAttribute]
          val blendingAttribute: Nullable[BlendingAttribute] =
            Nullable(instance.materials(0).get(BlendingAttribute.Type)).map(_.asInstanceOf[BlendingAttribute])
          colorAttribute.color.r = rd.colorChannel.floatData(colorOffset + ParticleChannels.RedOffset)
          colorAttribute.color.g = rd.colorChannel.floatData(colorOffset + ParticleChannels.GreenOffset)
          colorAttribute.color.b = rd.colorChannel.floatData(colorOffset + ParticleChannels.BlueOffset)
          blendingAttribute.foreach { ba =>
            ba.opacity = rd.colorChannel.floatData(colorOffset + ParticleChannels.AlphaOffset)
          }
        }
        i += 1
        positionOffset += rd.positionChannel.strideSize
      }
    }
    super.update()
  }

  override def copy(): ParticleControllerComponent =
    ModelInstanceRenderer(batch)

  override def isCompatible(batch: ParticleBatch[?]): Boolean =
    batch.isInstanceOf[ModelInstanceParticleBatch]
}
