/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/renderers/ParticleControllerControllerRenderer.java
 * Original authors: Inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - Java uses raw type ParticleControllerRenderer; Scala provides explicit type params
 * - controllerChannel: Java package-private, Scala public var (visibility widened)
 * - init(): Java null check + GdxRuntimeException -> Scala getOrElse + SgeError.InvalidInput
 * - update(): Java data[i] -> Scala objectData(i) (ParallelArray rename)
 * - All 4 methods ported: init, update, copy, isCompatible
 * - Audited 2026-03-03: pass
 */
package sge
package graphics
package g3d
package particles
package renderers

import sge.graphics.g3d.particles.ParallelArray.ObjectChannel
import sge.graphics.g3d.particles.{ ParticleChannels, ParticleController, ParticleControllerComponent }
import sge.graphics.g3d.particles.batches.ParticleBatch
import sge.utils.SgeError

/** A {@link ParticleControllerRenderer} which will render the {@link ParticleController} of each particle.
  * @author
  *   Inferno
  */
class ParticleControllerControllerRenderer extends ParticleControllerRenderer[ParticleControllerRenderData, ParticleBatch[ParticleControllerRenderData]] {

  var controllerChannel: ObjectChannel[ParticleController] = scala.compiletime.uninitialized

  override def init(): Unit =
    controllerChannel = controller.particles
      .getChannel[ObjectChannel[ParticleController]](ParticleChannels.ParticleController)
      .getOrElse(
        throw SgeError.InvalidInput("ParticleController channel not found, specify an influencer which will allocate it please.")
      )

  override def update(): Unit = {
    var i = 0
    val c = controller.particles.size
    while (i < c) {
      controllerChannel.objectData(i).draw()
      i += 1
    }
  }

  override def copy(): ParticleControllerComponent =
    new ParticleControllerControllerRenderer()

  override def isCompatible(batch: ParticleBatch[?]): Boolean =
    false
}
