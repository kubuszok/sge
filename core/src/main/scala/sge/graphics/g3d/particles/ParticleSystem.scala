/*
 * Ported from libGDX - https://github.com/libgdx/libgdx
 * Original source: com/badlogic/gdx/graphics/g3d/particles/ParticleSystem.java
 * Original authors: inferno
 * Licensed under the Apache License, Version 2.0
 *
 * Scala port Copyright 2024-2026 Mateusz Kubuszok
 */
package sge
package graphics
package g3d
package particles

import sge.graphics.g3d.{ Renderable, RenderableProvider }
import sge.graphics.g3d.particles.batches.ParticleBatch
import sge.utils.DynamicArray
import sge.utils.Pool

/** Singleton class which manages the particle effects. It's a utility class to ease particle batches management and particle effects update.
  * @author
  *   inferno
  */
final class ParticleSystem extends RenderableProvider {

  private val batches: DynamicArray[ParticleBatch[?]] = DynamicArray[ParticleBatch[?]]()
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
  def update()(using sge: Sge): Unit =
    for (effect <- effects)
      effect.update()

  def updateAndDraw()(using sge: Sge): Unit =
    for (effect <- effects) {
      effect.update()
      effect.draw()
    }

  def update(deltaTime: Float): Unit =
    for (effect <- effects)
      effect.update(deltaTime)

  def updateAndDraw(deltaTime: Float): Unit =
    for (effect <- effects) {
      effect.update(deltaTime)
      effect.draw()
    }

  /** Must be called one time per frame before any particle effect drawing operation will occur. */
  def begin(): Unit =
    for (batch <- batches)
      batch.begin()

  /** Draws all the particle effects. Call {@link #begin()} before this method and {@link #end()} after. */
  def draw(): Unit =
    for (effect <- effects)
      effect.draw()

  /** Must be called one time per frame at the end of all drawing operations. */
  def end(): Unit =
    for (batch <- batches)
      batch.end()

  override def getRenderables(renderables: DynamicArray[Renderable], pool: Pool[Renderable]): Unit =
    for (batch <- batches)
      batch.getRenderables(renderables, pool)

  def getBatches(): DynamicArray[ParticleBatch[?]] =
    batches
}
