/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleSystem.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port copyright 2025-2026 Mateusz Kubuszok
 *
 * Migration notes:
 * - All public methods ported faithfully
 * - Fixes (2026-03-04): getBatches() removed → public val batches
 * - Deprecated static get() singleton omitted (Java @Deprecated)
 * - update()/updateAndDraw() overloads use (using Sge) context parameter for no-arg variants
 * - remove uses removeValue without identity flag (DynamicArray uses == equality)
 * - Class is final (matches Java)
 * - Convention: opaque Seconds for update(deltaTime), updateAndDraw(deltaTime) params
 */
package sge
package graphics
package g3d
package particles

import sge.Sge
import sge.graphics.g3d.{ Renderable, RenderableProvider }
import sge.graphics.g3d.particles.batches.ParticleBatch
import sge.utils.DynamicArray
import sge.utils.Pool

import scala.annotation.publicInBinary

/** Singleton class which manages the particle effects. It's a utility class to ease particle batches management and particle effects update.
  * @author
  *   inferno
  */
final class ParticleSystem()(using Sge) extends RenderableProvider {

  val batches:         DynamicArray[ParticleBatch[?]] = DynamicArray[ParticleBatch[?]]()
  private val effects: DynamicArray[ParticleEffect]   = DynamicArray[ParticleEffect]()

  def add(batch: ParticleBatch[?]): Unit =
    batches.add(batch)

  def add(effect: ParticleEffect): Unit =
    effects.add(effect)

  def remove(effect: ParticleEffect): Unit =
    effects.removeValue(effect)

  /** Removes all the effects added to the system */
  def removeAll(): Unit =
    effects.clear()

  /** Updates the simulation of all effects */
  def update(): Unit =
    for (effect <- effects)
      effect.update()

  def updateAndDraw(): Unit =
    for (effect <- effects) {
      effect.update()
      effect.draw()
    }

  def update(deltaTime: sge.utils.Seconds): Unit =
    for (effect <- effects)
      effect.update(deltaTime)

  def updateAndDraw(deltaTime: sge.utils.Seconds): Unit =
    for (effect <- effects) {
      effect.update(deltaTime)
      effect.draw()
    }

  /** Must be called one time per frame before any particle effect drawing operation will occur. */
  @publicInBinary private[sge] def begin(): Unit =
    for (batch <- batches)
      batch.begin()

  /** Draws all the particle effects. Call {@link #begin()} before this method and {@link #end()} after. */
  def draw(): Unit =
    for (effect <- effects)
      effect.draw()

  /** Must be called one time per frame at the end of all drawing operations. */
  @publicInBinary private[sge] def end(): Unit =
    for (batch <- batches)
      batch.end()

  /** Executes `body` between [[begin]] and [[end]], ensuring [[end]] is called even if `body` throws. */
  inline def rendering[A](inline body: => A): A = {
    begin()
    try body
    finally end()
  }

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    for (batch <- batches)
      batch.getRenderables(renderables, pool)

}
