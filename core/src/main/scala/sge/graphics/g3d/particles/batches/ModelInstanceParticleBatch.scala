/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/batches/ModelInstanceParticleBatch.java
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

import sge.graphics.g3d.Renderable
import sge.graphics.g3d.particles.ResourceData
import sge.graphics.g3d.particles.renderers.ModelInstanceControllerRenderData
import sge.utils.DynamicArray
import sge.utils.Pool

/** * This class is used to render particles having a model instance channel.
  * @author
  *   Inferno
  */
class ModelInstanceParticleBatch extends ParticleBatch[ModelInstanceControllerRenderData] {
  var controllersRenderData: DynamicArray[ModelInstanceControllerRenderData] =
    DynamicArray[ModelInstanceControllerRenderData](5)
  var bufferedParticlesCount: Int = 0

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    for (data <- controllersRenderData) {
      var i     = 0
      val count = data.controller.particles.size
      while (i < count) {
        data.modelInstanceChannel.objectData(i).getRenderables(renderables, pool)
        i += 1
      }
    }

  def getBufferedCount(): Int =
    bufferedParticlesCount

  override def begin(): Unit = {
    controllersRenderData.clear()
    bufferedParticlesCount = 0
  }

  override def end(): Unit = {}

  override def draw(data: ModelInstanceControllerRenderData): Unit = {
    controllersRenderData.add(data)
    bufferedParticlesCount += data.controller.particles.size
  }

  override def save(manager: sge.assets.AssetManager, resources: ResourceData[?]): Unit = {}

  override def load(manager: sge.assets.AssetManager, resources: ResourceData[?]): Unit = {}
}
